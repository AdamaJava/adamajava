package QCMG::QBamMaker::SeqFinalBam;

###########################################################################
#
#  Module:   QCMG::QBamMaker::SeqFinalBam
#  Creator:  John V Pearson
#  Created:  2013-05-23
#
#  Data container for information about a QCMG seq_final BAM.
#
#  $Id: SeqFinalBam.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Digest::CRC;
use Memoize;

use QCMG::IO::SamReader;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $QLIMS_META $VALID_AUTOLOAD_METHODS
             $T_OLD2NEW $T_NEW2OLD );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: SeqFinalBam.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

our $AUTOLOAD;  # it's a package global

BEGIN {

    # This is a list of the fields that a qlimsmeta line should return.
    # We had to use an array of arrays because the order is critical so
    # a simple hash is not safe because "keys" does not guarantee to
    # return keys in the order they appear in the hash definition.  We
    # need the two strings because the strings used in qlimsmeta (e.g.
    # Project) no longer match the values that are used in the LIMS
    # (parent_project) so we need to be able to translate between the
    # two sets of identifiers.

    my @ql_v1 = ( [ 1,  'Donor',                    'project' ],
                  [ 2,  'Project',                  'parent_project' ],
                  [ 3,  'Material',                 'material' ],
                  [ 4,  'Sample Code',              'sample_code' ],
                  [ 5,  'Sequencing Platform',      'sequencing_platform' ],
                  [ 6,  'Aligner',                  'aligner' ],
                  [ 7,  'Capture Kit',              'capture_kit' ],
                  [ 8,  'Library Protocol',         'library_protocol' ],
                  [ 9,  'Sample',                   'sample' ],
                  [ 10, 'Species Reference Genome', 'species_reference_genome' ],
                  [ 11, 'Reference Genome File',    'reference_genome_file' ],
                  [ 12, 'Failed QC',                'failed_qc' ],
                );

    $QLIMS_META = {
       '1.0' => \@ql_v1,
    };

    # The version-specific headers are doubly important to use because
    # we are going to use them to create a hashref ($VALID_AUTOLOAD_METHODS)
    # that will hold the names of all the methods that we will try to handle
    # via AUTOLOAD.  We are using AUTOLOAD because with a little planning,
    # it lets us avoid defining and maintaining a lot of basically identical
    # accessor methods.

    $VALID_AUTOLOAD_METHODS = {};
    $T_OLD2NEW = {};
    $T_NEW2OLD = {};
    foreach my $version (keys %{$QLIMS_META}) {
        $VALID_AUTOLOAD_METHODS->{$version} = {};
        $T_OLD2NEW->{$version} = {};
        $T_NEW2OLD->{$version} = {};
        foreach my $method (@{$QLIMS_META->{$version}}) {
            $VALID_AUTOLOAD_METHODS->{$version}->{ $method->[2] } = 1;
            $T_OLD2NEW->{$version}->{ $method->[1] } = $method->[2];
            $T_NEW2OLD->{$version}->{ $method->[2] } = $method->[1];
        }
        # In each version we must allow for the extra fields that we'd
        # like to handle via AUTOLOAD
        my @extras = qw( BamName BamHeaderCRC32 BamMtimeEpoch FromQLimsMeta );
        foreach my $method (@extras) {
            $VALID_AUTOLOAD_METHODS->{$version}->{$method} = 1;
            $T_NEW2OLD->{$version}->{$method} = $method;
            $T_OLD2NEW->{$version}->{$method} = $method;
        }
    }
}

###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    croak "no filename parameter to new()" unless
        (exists $params{filename} and defined $params{filename});

    my $self = { BamName => $params{filename},
                 version => ($params{version} ? $params{version} : '1.0'),
                 verbose => ($params{verbose} ? $params{verbose} : 0) };
    bless $self, $class;

    $self->_parse_header;
#qlogprint "after _parse_header() so should be exiting new() now\n";

    return $self;
}


sub version {
    my $self = shift;
    return $self->{version};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub AUTOLOAD {
    my $self  = shift;
    my $value = undef;
    $value = shift if @_;

    my $type       = ref($self) or confess "$self is not an object";
    my $invocation = $AUTOLOAD;
    my $method     = undef;
    my $version    = $self->version;

    if ($invocation =~ m/^.*::([^:]*)$/) {
        $method = $1;
    }
    else {
        croak "QCMG::QBamMaker::SeqFinalBam AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    croak "QCMG::QBamMaker::SeqFinalBam can't access method [$method] via AUTOLOAD"
        unless (exists $VALID_AUTOLOAD_METHODS->{$version}->{$method});

    # If this is a setter call then do the set
    if (defined $value) {
        $self->{$method} = $value;
    }
    # Return current value
    return $self->{$method};
}


# This private method is useful if we have a list of the names of the
# values we want so we'd rather look them up than have to work out how
# to invoke the appropriate methods.

sub _attribute {
    my $self = shift;
    my $attr = shift;
    return ( exists $self->{$attr} ? $self->{$attr} : undef );
}


sub mapsets {
    my $self = shift;
    return $self->{mapsets};
}


# Return a list of all optional columns
sub optional_columns {
    my $self = shift;
   return @{ $self->{extra_fields} };
}


sub _parse_header {
    my $self = shift;

    my $bamfile = $self->BamName;

    # BAM is locked for creation
    if (-r $bamfile.'.qbmlock') {
        warn "bamfile is locked for creation: $bamfile\n";
        return undef;
    }
    # BAM file does not exist
    if (! -r $bamfile) {
        warn "bamfile does not exist: $bamfile\n";
        return undef;
    }

    qlogprint( "reading from file: $bamfile\n" ) if $self->verbose;

    # Read the BAM headers
    my $bam = QCMG::IO::SamReader->new( filename => $bamfile,
                                        verbose  => $self->verbose );
    my $headers_text = $bam->headers_text;

    # Initialise our 3 extra fields
    my $ctx = Digest::CRC->new( type => 'crc32' );
    $ctx->add( $headers_text );
    $self->{BamHeaderCRC32} = $ctx->hexdigest;
    my @stats = stat( $bamfile );
    $self->{BamMtimeEpoch} = $stats[9];
    $self->{BamName} = $bamfile;

#qlogprint( "before _parse_header_qlimsmeta\n" ) if ($self->verbose > 1);
    $self->_parse_header_qlimsmeta( $headers_text );
#qlogprint( "before _parse_header_qmapset\n" ) if ($self->verbose > 1);
    $self->_parse_header_qmapset( $headers_text );
#qlogprint( "after _parse_header_qmapset\n" ) if ($self->verbose > 1);

#qlogprint( "should be falling back to new() now\n" ) if ($self->verbose > 1);
}


sub _translate_old2new {
    my $self = shift;
    my $field = shift;
    
    return undef unless exists $T_OLD2NEW->{$self->version}->{$field};
    return $T_OLD2NEW->{$self->version}->{$field};
}


sub _translate_new2old {
    my $self = shift;
    my $field = shift;
    
    return undef unless exists $T_NEW2OLD->{$self->version}->{$field};
    return $T_NEW2OLD->{$self->version}->{$field};
}


sub qlimsmeta_fields {
    my $self = shift;
    
    # Lookup conversion hash and return NEW names, i.e. project not Donor
    my @headers = map { $_->[2] } @{ $QLIMS_META->{$self->version} };
    return @headers;
}


# Non-OO
sub qlimsmeta_old2new_hash {
    my $version = shift;
    my %translator = map { $_->[1] => $_->[2] }
                     @{ $QLIMS_META->{$version} };
    return \%translator;
}

# Non-OO
sub qlimsmeta_new2old_hash {
    my $version = shift;
    my %translator = map { $_->[2] => $_->[1] }
                     @{ $QLIMS_META->{$version} };
    return \%translator;
}


sub _parse_header_qlimsmeta {
    my $self = shift;
    my $text = shift;

    my $infile    = $self->BamName;
    my @lines     = split (/\n/, $text);
    my @qlimsmeta = grep { /^\@CO\tCN:QCMG\tQN:qlimsmeta/ } @lines;

    # Older seq_final BAMs may not have a qlimsmeta line so we'll need
    # to cope with that by "guessing" as many attributes as possible.
    # We better only see 1 qlimsmeta line
    if (scalar(@qlimsmeta) > 1) {
        die 'There should only be 1 qlimsmeta line but we see ',
            scalar(@qlimsmeta), " in $infile\n";
    }
    elsif (scalar(@qlimsmeta) == 1) {
        # We have a single qlimsmeta line so parse it
        $self->FromQLimsMeta( 1 );

        # Put the qlimsmeta fields into a hash
        my %fields = map { /(.*)=(.*)/; $1 => $2 }
                     grep { /\=/ }
                     split /\t/, $qlimsmeta[0];
 
        # Check that all of the expected fields were in the qlimsmeta line
        # - they can be blank but they should exist.  Note also that
        # they could be in old or new form so we'll need to check for
        # either.  N.B. qlimsmeta_fields() returns NEW names!!!
        my @headers = $self->qlimsmeta_fields();
        foreach my $header (@headers) {
            my $new_format = $header;
            my $old_format = $self->_translate_new2old($header);
            if (exists $fields{ $new_format }) {
                # New format so just write it straight in
                $self->{ $new_format } = $fields{ $new_format };
            }
            elsif (exists $fields{ $old_format }) {
                # Old format so change to new format in $self
                $self->{ $new_format } = $fields{ $old_format };
            }
            else {
                warn "field [$header] is missing from qlimsmeta line: $infile\n";
            }
        }
        #print Dumper \@headers, \%fields, $self;
        #die;
    }
    elsif (scalar(@qlimsmeta) == 0) {
        # We do not have a qlimsmeta line so start the guessing

        qlogprint "guessing properties from BAM name for file $infile\n"
            if $self->verbose;

        $self->FromQLimsMeta( 0 );

        $self->capture_kit( '' );
        $self->parent_project( 'qcmg_unspecified' );
        
        if ($infile =~ /\/([^\/]+)\/seq_final\//i) {
            $self->project( $1 );
        }

        if ($infile =~ /HiSeq/i) {
            $self->aligner( 'bwa' );
            $self->sequencing_platform( 'Hiseq' );
        }

        if ($infile =~ /novoalign/i) {
            $self->aligner( 'novoalign' );
        }

        if ($infile =~ /\.[ATNXSC]D\.bam$/i) {
            $self->material( '1:DNA' );
        }
        elsif ($infile =~ /\.[ATNXSC]R\.bam$/i) {
            $self->material( '2:RNA' );
        }

        if ($infile =~ /\.mRNA\./i) {
            $self->material( '2:RNA' );
        }

        $self->sample_code( '1:Normal blood' )
            if ($infile =~ /_1NormalBlood/i);
        $self->sample_code( '3:Normal control (adjacent)' )
            if ($infile =~ /_3NormalAdjacent/i or
                $infile =~ /\.AD\./ or
                $infile =~ /\.AR\./);
        $self->sample_code( '4:Normal control (other site)' )
            if ($infile =~ /_4NormalOther/i or
                $infile =~ /\.ND\./ or
                $infile =~ /\.NR\./);
        $self->sample_code( '7:Primary tumour' )
            if ($infile =~ /_7PrimaryTumour/i or
                $infile =~ /\.TD\./ or
                $infile =~ /\.TR\./);
        $self->sample_code( '10:Cell line derived from xenograft' )
            if ($infile =~ /_10XenograftCellLine/i or
                $infile =~ /\.XD\./ or
                $infile =~ /\.XR\./);
        $self->sample_code( '16:Tumour metastasis to distant location' )
            if ($infile =~ /_16DistantMet/i or
                $infile =~ /\.MD\./ or
                $infile =~ /\.MR\./);
        $self->sample_code( '9:Cell line derived from tumour' )
            if ($infile =~ /_9TumourCellLine/i or
                $infile =~ /\.CD\./ or
                $infile =~ /\.CR\./);

        if ($text =~ /PL:SOLiD/i) {
            $self->sequencing_platform( 'SOLiD4' );
            $self->capture_kit( 'Human All Exon 50Mb (SureSelect)' )
                if ($infile =~ /exome/i);
            $self->library_protocol( 'SOLiD4Library' );
            $self->species_reference_genome( 'Homo sapiens (GRCh37_ICGC_standard_v2)' );
            $self->reference_genome_file( '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa' );
            $self->failed_qc( 0 );
        }
        if ($text =~ /PU:bioscope-pairing/i) {
            $self->aligner( 'bioscope' );
        }
    }
    else {
        die "logic error in processing qlimsmeta line for $infile\n";
    }
}


sub _parse_header_qmapset {
    my $self = shift;
    my $text = shift;

    my @lines     = split (/\n/, $text);
    my %mapsets   = map  { /\tMapset=([^\s]*)\t/; $1 => $_ }
                    grep { /^\@CO\tCN:QCMG\tQN:qmapset/ } @lines;

    # Turn each mapset line into a hash
    foreach my $key (keys %mapsets) {
        # Don't forget to lc() and replace any spaces in field names with
        # underscores prior to pushing them into the hash.
        my %fields = map { /(.*)=(.*)/; my $x = lc($1); my $y = $2; $x =~ s/ /_/g; $x => $y }
                     grep { /\=/ }
                     split /\t/, $mapsets{$key};
        $mapsets{ $key } = \%fields;
    }

    $self->{mapsets} = \%mapsets;
}


1;
__END__


=head1 NAME

QCMG::QBamMaker::SeqFinalBam - seq_final BAM data container


=head1 SYNOPSIS

 use QCMG::QBamMaker::SeqFinalBam;


=head1 DESCRIPTION

This module provides a data container for a seq_final BAM Record.  It
needs to parse BAM headers so the 'samtools' binary must be in $PATH.
At QCMG that means you need to do a 'module load samtools' prior to
running using any code that uses this module.


=head1 PUBLIC METHODS

=over 2

=item B<new()>

 my $bam = QCMG::QBamMaker::SeqFinalBam->new( filename => 'myseq.bam',
                                              verbose  => 1 );

The B<new()> method takes a compulsory parameter (filename) and an 
optional parameter (verbose).

=item B<mapsets()>

 $bam->mapsets;

Returns information parsed out of qmapset @CO comment lines in the BAM
header.  Also includes 3 extra fields:

 BamName - the name of the BAM file
 BamHeaderCRC32 - a CRC32 taken across the text content of the BAM
     header.  It's not an ideal situation but this CRC plus the 
     B<BamMtimeEpoch> I<should> be sufficient for a unique
     identifier of a seq_final BAM.
 BamMtimeEpoch - the last change time of the BAM file in seconds
     since the Unix epoch (00:00:00 UTC on 1 January 1970).

=item B<verbose()>

 $bam->verbose( 2 );
 my $verbose = $bam->verbose;

Accessor for verbosity level of progress reporting.

=item B<project()>

 [ 2,  'Project',                  'parent_project' ],
 [ 3,  'Material',                 'material' ],
 [ 4,  'Sample Code',              'sample_code' ],
 [ 5,  'Sequencing Platform',      'sequencing_platform' ],
 [ 6,  'Aligner',                  'aligner' ],
 [ 7,  'Capture Kit',              'capture_kit' ],
 [ 8,  'Library Protocol',         'library_protocol' ],
 [ 9,  'Sample',                   'sample' ],
 [ 10, 'Species Reference Genome', 'species_reference_genome' ],
 [ 11, 'Reference Genome File',    'reference_genome_file' ],
 [ 12, 'Failed QC',                'failed_qc' ],

This is the list of accessors that can be called on a SeqFinalBam.  The
information is all extracted from the 'qlimsmeta' comment line (@CO) in
the BAM header.

=back

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: SeqFinalBam.pm 4665 2014-07-24 08:54:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013,2014

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
