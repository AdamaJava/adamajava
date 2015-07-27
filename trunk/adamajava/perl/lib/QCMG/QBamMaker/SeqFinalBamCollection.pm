package QCMG::QBamMaker::SeqFinalBamCollection;

###########################################################################
#
#  Module:   QCMG::QBamMaker::SeqFinalBamCollection
#  Creator:  John V Pearson
#  Created:  2013-05-31
#
#  Operations on a collection of QCMG::QBamMaker::SeqFinalBam objects.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX qw( floor );
use Storable qw(dclone);

use QCMG::QBamMaker::SeqFinalBam;
use QCMG::Util::QLog;
use QCMG::Verify::VoteCounter;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

our $AUTOLOAD;  # it's a package global


###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { records => [],
                 verbose => ($params{verbose} ? $params{verbose} : 0) };
    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub records {
    my $self = shift;
    return @{ $self->{records} };
}


sub _records {
    my $self = shift;
    return $self->{records};
}


sub record_count {
    my $self = shift;
    return scalar( $self->records );
}


sub add_record {
    my $self = shift;
    my $rec  = shift;

    if (ref($rec) ne 'QCMG::QBamMaker::SeqFinalBam') {
        die 'SeqFinalBamCollection::add_record() can only accept objects '.
            'of class QCMG::QBamMaker::SeqFinalBam and you passed an object of '.
            'type '. ref($rec);
    }

    push @{ $self->{records} }, $rec;
}


sub add_records {
    my $self    = shift;
    my $ra_recs = shift;

    foreach my $rec (@{ $ra_recs }) {
        $self->add_record( $rec );
    }
}


sub bams_paired_for_variant_calling {
    my $self = shift;

    my $record_count = $self->record_count;
    my @pairs = ();

    # Compare every BAM to every other BAM looking for
    # pairs that could be variant called.
    foreach my $i (0..($record_count-2)) {
        foreach my $j (($i+1)..($record_count-1)) {
            
            my $rec1 = $self->{records}->[$i];
            my $rec2 = $self->{records}->[$j];

            if (     ( $rec1->project eq $rec2->project )
                 and ( $rec1->parent_project eq $rec2->parent_project )
                 and ( $rec1->material eq $rec2->material )
                 and ( $rec1->library_protocol eq $rec2->library_protocol )
                 and ( $rec1->capture_kit eq $rec2->capture_kit )
                 and ( $rec1->sequencing_platform eq $rec2->sequencing_platform )
                 and ( $rec1->aligner eq $rec2->aligner )
                 and ( ( _is_normal( $rec1->sample_code ) and
                         _is_tumour( $rec2->sample_code ) ) or
                       ( _is_normal( $rec2->sample_code ) and
                         _is_tumour( $rec1->sample_code ) ) )
               ) {
                push @pairs, [ $rec1, $rec2 ];
                #print "$i:\t",$rec1->BamName, " vs\n",
                #      "$j:\t",$rec2->BamName, "\n\n";
                qlogprint( join('  ',"found callable BAM pair ($i vs $j):",
                                     $rec1->material,
                                     $rec1->sequencing_platform,
                                     $rec1->aligner,
                                     $rec1->capture_kit,
                                     $rec1->sample_code,
                                     $rec2->sample_code ),"\n" )
                   if $self->verbose;
            }
        }
    }

    return \@pairs;
}


sub new_collections_sorted_by_project_and_donor {
    my $self = shift;

    my %new_colls = ();

    # Create a hash of new SeqFinalBamCollection objects where the hash
    # is keyed on parent_project/project and each new Collection
    # contains all of the records from this collection that match the
    # parent_project/project.
    foreach my $rec (@{ $self->_records }) {

        # Create a new collection if one does not already exist
        if (! exists $new_colls{ $rec->parent_project }) {
            $new_colls{ $rec->parent_project } = {};
        }
        if (! exists $new_colls{ $rec->parent_project }->{ $rec->project } ) {
            my $coll = QCMG::QBamMaker::SeqFinalBamCollection->new(
                           verbose => $self->verbose );
            $new_colls{ $rec->parent_project }->{ $rec->project } = $coll;
        }

        # Add this record to the collection
        $new_colls{ $rec->parent_project }->{ $rec->project }->add_record( $rec );

    }

    return \%new_colls;
}
            

sub new_collections_sorted_by_donor {
    my $self = shift;

    my %new_colls = ();

    foreach my $rec (@{ $self->_records }) {

        # Create a new collection if one does not already exist
        if (! exists $new_colls{ $rec->project }) {
            my $coll = QCMG::QBamMaker::SeqFinalBamCollection->new(
                           verbose => $self->verbose );
            $new_colls{ $rec->project } = $coll;
        }

        # Add this record to the collection
        $new_colls{ $rec->project }->add_record( $rec );
    }

    return \%new_colls;
}
            

# These 2 non-OO routines are copied from QCMG::Verify::VoteCounter
sub _is_normal {
    my $sample_code = shift;
    return 0 unless $sample_code;
    return 0 if (! exists $QCMG::Verify::VoteCounter::BAM_CATEGORY{ $sample_code });
    return 1 if ($QCMG::Verify::VoteCounter::BAM_CATEGORY{ $sample_code } == 1);
    return 0;
}
sub _is_tumour {
    my $sample_code = shift;
    return 0 unless $sample_code;
    return 0 if (! exists $QCMG::Verify::VoteCounter::BAM_CATEGORY{ $sample_code });
    return 1 if ($QCMG::Verify::VoteCounter::BAM_CATEGORY{ $sample_code } == 2);
    return 0;
}


sub write_bamlist_file {
    my $self    = shift;
    my $outfile = shift;

    # We are hard-coding this because we want this file format to be
    # hard-coded regardless of whether or not the actual BAMs contain
    # these fields.

    my @fields = (
        [ 'Donor',                    'project' ],
        [ 'BamName',                  'BamName' ],
        [ 'BamHeaderCRC32',           'BamHeaderCRC32' ],
        [ 'BamMtimeEpoch',            'BamMtimeEpoch' ],
        [ 'Project',                  'parent_project' ],
        [ 'Material',                 'material' ],
        [ 'Sample Code',              'sample_code' ],
        [ 'Sequencing Platform',      'sequencing_platform' ],
        [ 'Aligner',                  'aligner' ],
        [ 'Capture Kit',              'capture_kit' ],
        [ 'Library Protocol',         'library_protocol' ],
        [ 'Sample',                   'sample' ],
        [ 'Species Reference Genome', 'species_reference_genome' ],
        [ 'Reference Genome File',    'reference_genome_file' ],
        [ 'Failed QC',                'failed_qc' ],
        [ 'FromQLimsMeta',            'FromQLimsMeta' ],
    );

    my $outfh = IO::File->new( $outfile, 'w' )
    or die 'Can\'t open output file [', $outfile,"]: $!";

    my @headers    = map { $_->[0] } @fields;
    my @attributes = map { $_->[1] } @fields;

    #print $outfh join("\t",'ID',@headers),"\n";
    print $outfh join("\t",'ID',@attributes),"\n";

    my $ctr = 1;
    foreach my $bam (@{ $self->_records }) {
        my @outputs = map { (defined $_ ? $_ : '') }
                      map { $bam->_attribute( $_ ) }
                      @attributes;
        print $outfh join("\t",$ctr++,@outputs),"\n";
    }

    $outfh->close;
}




1;

__END__


=head1 NAME

QCMG::QBamMaker:SqeFinalBamCollection - Collection of BAM objects


=head1 SYNOPSIS

 use QCMG::QBamMaker::SeqFinalBamCollection;


=head1 DESCRIPTION

This module provides operations on a collection of 
QCMG::QBamMaker::SeqFinalBam objects.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $sfbc = QCMG::QBamMaker::SeqFinalBamCollection->new( verbose => 1 );

There are no compulsory options to new().  Verbose is set to 0 by
default and any non-zero integer will turn on additional logging.

=item B<add_record()>

Add an object of type QCMG::QBamMaker::SeqFinalBam to the collection.

=item B<add_records()>

Takes an arrayref of objects of type QCMG::QBamMaker::SeqFinalBam to be added to
the collection.

=item B<records()>

Returns an array containing the QCMG::QBamMaker::SeqFinalBam objects 
contained in the collection.

=item B<record_count()>

Number of SeqFinalBam objects in the collection.

=item B<new_collections_sorted_by_project_and_donor()>

 my $rh_sfbs = $sfbc->new_collections_sorted_by_project_and_donor;

This is useful where you have a collection of SeqFinaBam's and you would
like to break it down into new SeqFinalBamCollection objects where each
has BAMs from a single project/donor.

This method returns a 2-level hash of SeqFinalBamCollection objects 
where the hash is keyed on project/donor.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


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
