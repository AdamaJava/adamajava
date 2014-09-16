package QCMG::SeqResults::ReportInspectBam;

###########################################################################
#
#  Module:   QCMG::SeqResults::ReportInspectBam.pm
#  Creator:  John V Pearson
#  Created:  2011-05-16
#
#  Logic for command inspectbam that looks inside BAMs for key processing
#  information - were the records aligned against the correct reference,
#  have MD tags been calculated, has markDuplicates been run, etc?
#
#  $Id: ReportInspectBam.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use File::Find;
use Getopt::Long;
use Pod::Usage;
use Data::Dumper;

use QCMG::IO::SamReader;
use QCMG::Util::QLog;

use QCMG::SeqResults::Util qw( qmail bams_in_directory
                               is_valid_mapset_name );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: ReportInspectBam.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    qlogprint( "Starting new()\n" );

    # Defaults (if any) for commandline options
    my %opts       = ();
    $opts{dir}     = $params{dir}     ||  '/mnt/seq_results';
    $opts{emails}  = $params{emails}  || [ ];
    $opts{outfile} = $params{outfile} || '';
    $opts{verbose} = $params{verbose} || 0;
    $opts{help}    = 0;

    # Use GetOptions module to parse commandline options
    my $results = GetOptions (
           'd|dir=s'         => \$opts{dir},           # -d
           'e|email=s'       =>  $opts{emails},        # -e
           'o|outfile=s'     => \$opts{outfile},       # -o
           'v|verbose+'      => \$opts{verbose},       # -v
           'h|help|?'        => \$opts{help},          # -h
           );

    # If no email recipient supplied, add invoker as default
    push @{$opts{emails}}, $ENV{QCMG_EMAIL} unless scalar @{$opts{emails}};

    # Print help if help requested or no options supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND checkbam' ) if $opts{help};

    # Create the object
    my $self = { bams_not_aligned_to_v2 => [],
                 bams_without_MD_tags   => [],
                 bams_without_HD_header => [],
                 bams_without_RG_header => [],
                 bams_with_no_records   => [],
                 files_passed => [],
                 files_failed => [],
                 %opts };

    qlogprint( "Completed new()\n" );

    bless $self, $class;
}


sub dir {
    my $self = shift;
    return $self->{dir};
}


sub email {
    my $self = shift;
    return $self->{emails};
}


sub emails {
    my $self = shift;
    return @{ $self->{emails} };
}


sub outfile {
    my $self = shift;
    return $self->{outfile};
}


sub bams_not_aligned_to_v2 {
    my $self = shift;
    return @{ $self->{bams_not_aligned_to_v2} };
}


sub bams_without_MD_tags {
    my $self = shift;
    return @{ $self->{bams_without_MD_tags} };
}


sub bams_without_HD_header {
    my $self = shift;
    return @{ $self->{bams_without_HD_header} };
}


sub bams_without_RG_header {
    my $self = shift;
    return @{ $self->{bams_without_RG_header} };
}


sub bams_with_no_records {
    my $self = shift;
    return @{ $self->{bams_with_no_records} };
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub execute {
    my $self = shift;

    qlogprint( "Starting execute()\n" );
 
    my @bams = sort( bams_in_directory( $self->dir ) );

    foreach my $bam (@bams) {
        qlogprint( "processing file $bam\n" ) if $self->verbose;
        my $bfile = QCMG::IO::SamReader->new( filename => $bam );
        my $header = $bfile->headers_text;

        # Does the BAM have a @HD line?
        if ($header !~ /^\@HD\s/m) {
            push @{ $self->{bams_without_HD_header} }, $bam;
            qlogprint( "  no \@HD line found in BAM header\n" )
                if ($self->verbose > 1);
        }

        # Does the BAM have a @RG line?
        if ($header !~ /^\@RG\s/m) {
            push @{ $self->{bams_without_RG_header} }, $bam;
            qlogprint( "  no \@RG line found in BAM header\n" )
                if ($self->verbose > 1);
        }

        # Try to work out what copy of the genome was used
        my $genome_version = '';
        if ($header =~ /^(\@SQ\s+SN:chr1.*)$/m) {
            my $chr1_seq = $1;
            if ($chr1_seq =~ /UR:file.*\/([^\/]+.fa)/) {
                $genome_version = $1;
            }
            else {
                $genome_version = '!No UR: found for chr1!';
            }
        }
        else {
            $genome_version = '!No @SQ entry found for chr1!';
        }

        # Spot any BAMs that don't match our v2 genome
        if ($genome_version ne 'GRCh37_ICGC_standard_v2.fa') {
            push @{ $self->{bams_not_aligned_to_v2} }, $bam;
            qlogprint( "  not aligned to GRCh37_ICGC_standard_v2.fa\n" )
                if ($self->verbose > 1);
        }

        # Make sure there is a first record
        my $read = $bfile->next_record_as_record;
        if (! $read) {
            push @{ $self->{bams_with_no_records} }, $bam;
            qlogprint( "  no records found\n" )
                if ($self->verbose > 1);
        }
        else {
            # Look for MD tag in first *mapped* record
            while (my $read = $bfile->next_record_as_record) {
                # Skip if CIGAR is a '*'
                next if ($read->cigar() =~ /\*/);
                if (! $read->tag('MD')) {
                    push @{ $self->{bams_without_MD_tags} }, $bam;
                    qlogprint( "  no MD tag found in first record\n" )
                        if ($self->verbose > 1);
                }
                last; # We only need to check a single mapped read
            }
        }
    }

    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{bams_not_aligned_to_v2}} ) .
              " BAMs not aligned against GRCh37_ICGC_standard_v2.fa\n";
    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{bams_without_MD_tags}} ) .
              " BAMs without MD tags\n";
    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{bams_without_HD_header}} ) .
              " BAMs without \@HD line in header\n";
    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{bams_without_RG_header}} ) .
              " BAMs without \@RG line in header\n";
    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{bams_with_no_records}} ) .
              " BAMs with no records\n";

    # Tally problem BAMs - a BAM should only be counted once even if it
    # has multiple problems so we'll need to hash all bad BAMs.
    my %bad_bams;
    foreach my $bam ( $self->bams_not_aligned_to_v2, 
                      $self->bams_without_MD_tags,
                      $self->bams_without_HD_header,
                      $self->bams_without_RG_header,
                      $self->bams_with_no_records ) {
        $bad_bams{ $bam }++;
    }
    my @bad_bams = keys %bad_bams;
    $self->{bad_bam_count} = scalar( @bad_bams );

    qlogprint {l=>'TOOL'}, "found " . $self->{bad_bam_count} .
              " BAMs with one or more problems\n";
    qlogprint( "Completed execute()\n" );
}


sub output_report {
    my $self = shift;

    # Report is emailed unless -o is specified to direct output to a file

    if ($self->outfile) {
        open OUT, '>'.$self->outfile ||
            die 'Unable to open ',$self->outfile," for writing $!";
        print OUT $self->report_text;
        close OUT;
    }
    else {
        qmail( To      => $self->email,
               From    => $ENV{QCMG_EMAIL},
               Subject => $self->{bad_bam_count} .
                          ' problematic BAMs found in '. $self->dir ."\n\n",
               Message => $self->report_text );
    }
}


sub report_text {
    my $self = shift;

    my $text = 
        "\ntimelord.pl inspectbam  v$REVISION  [" . localtime() . "]\n" .
        '   dir           '. $self->dir . "\n" .
        '   email(s)      '. join("\n".' 'x17, $self->emails). "\n" .
        '   outfile       '. $self->outfile . "\n" .
        '   verbose       '. $self->verbose ."\n\n";

    $text .= $self->report_bams_not_aligned_to_reference_v2 ."\n".
             $self->report_bams_without_MD_tags ."\n".
             $self->report_bams_without_HD_header ."\n".
             $self->report_bams_without_RG_header ."\n".
             $self->report_bams_with_no_records;

    return $text;
}


sub report_bams_not_aligned_to_reference_v2 {
    my $self = shift;
    return "BAMs not aligned against GRCh37_ICGC_standard_v2.fa\n\n" .
           join("\n", $self->bams_not_aligned_to_v2) . "\n";
}


sub report_bams_without_MD_tags {
    my $self = shift;
    return "BAMs without MD tags\n\n" .
           join("\n", $self->bams_without_MD_tags) . "\n";
}


sub report_bams_without_HD_header {
    my $self = shift;
    return "BAMs without \@HD line in header\n\n" .
           join("\n", $self->bams_without_HD_header) . "\n";
}


sub report_bams_without_RG_header {
    my $self = shift;
    return "BAMs without \@RG line in header\n\n" .
           join("\n", $self->bams_without_RG_header) . "\n";
}


sub report_bams_with_no_records {
    my $self = shift;
    return "BAMs with no records\n\n" .
           join("\n", $self->bams_with_no_records) . "\n";
}


sub report_xml_string {
    my $self = shift;
    my $rcb = $self->report_xml_object;
    return $rcb->toString();
}


sub report_xml_object {
    my $self = shift;

    my $rib = XML::LibXML::Element->new( 'ReportInspectBam' );
    $rib->setAttribute( 'svn_revision', $REVISION );
    $rib->setAttribute( 'start_time', localtime().'' );
    my $rps = XML::LibXML::Element->new( 'CliParameters' );
    $rib->appendChild( $rps );
    if ($self->dir) {
        my $rp = XML::LibXML::Element->new( 'CliParameter' );
        $rp->setAttribute( 'dir', $self->dir );
        $rps->appendChild( $rp );
    }
    if ($self->outfile) {
        my $rp = XML::LibXML::Element->new( 'CliParameter' );
        $rp->setAttribute( 'outfile', $self->outfile );
        $rps->appendChild( $rp );
    }
    if ($self->emails) {
        my $rp = XML::LibXML::Element->new( 'CliParameter' );
        $rp->setAttribute( 'emails', join(',',$self->emails) );
        $rps->appendChild( $rp );
    }
    if ($self->verbose) {
        my $rp = XML::LibXML::Element->new( 'CliParameter' );
        $rp->setAttribute( 'verbose', $self->verbose );
        $rps->appendChild( $rp );
    }

    my $bs1 = XML::LibXML::Element->new( 'BamsNotAlignedAgainstGRCh37_ICGC_standard_v2.fa' );
    foreach my $bam ($self->bams_not_aligned_to_v2) {
        my $bx = XML::LibXML::Element->new( 'BamNotAlignedAgainstGRCh37_ICGC_standard_v2.fa' );
        $bx->appendText( $bam  );
        $bs1->appendChild( $bx );
    }
    $rib->appendChild( $bs1 );

    my $bs2 = XML::LibXML::Element->new( 'BamsWithoutMdTag' );
    foreach my $bam ($self->bams_without_MD_tags) {
        my $bx = XML::LibXML::Element->new( 'BamWithoutMdTag' );
        $bx->appendText( $bam  );
        $bs2->appendChild( $bx );
    }
    $rib->appendChild( $bs2 );

    my $bs3 = XML::LibXML::Element->new( 'BamsWithoutHdHeader' );
    foreach my $bam ($self->bams_without_HD_header) {
        my $bx = XML::LibXML::Element->new( 'BamWithoutHdHeader' );
        $bx->appendText( $bam  );
        $bs3->appendChild( $bx );
    }
    $rib->appendChild( $bs3 );

    my $bs4 = XML::LibXML::Element->new( 'BamsWithoutRgHeader' );
    foreach my $bam ($self->bams_without_RG_header) {
        my $bx = XML::LibXML::Element->new( 'BamWithoutRgHeader' );
        $bx->appendText( $bam  );
        $bs4->appendChild( $bx );
    }
    $rib->appendChild( $bs4 );

    my $bs5 = XML::LibXML::Element->new( 'BamsWithNoRecords' );
    foreach my $bam ($self->bams_with_no_records) {
        my $bx = XML::LibXML::Element->new( 'BamWithNoRecords' );
        $bx->appendText( $bam  );
        $bs5->appendChild( $bx );
    }
    $rib->appendChild( $bs5 );

    return $rib;
}



1;

-head1 COPYRIGHT

Copyright (c) The University of Queensland 2011,2012

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
