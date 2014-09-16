package QCMG::QBamMaker::Mapset;


##############################################################################
#
#  Module:   QCMG::QBamMaker::Mapset
#  Author:   John V Pearson
#  Created:  2013-02-15
#
#  This module is derived from the original qBamMaker.pl script whcih
#  got too complicated and needed to be broken up.
#
#  $Id: Mapset.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::FileDir::Finder;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Mapset.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { attributes   => {},
                 group        => '',
                 verbose      => ($params{verbose} ?
                                  $params{verbose} : 0),
               };

    # attributes: a hash of key,value pairs that describe the mapset.
    # These are often set by a LIMS query.
    delete $params{verbose};
    $self->{attributes} = \%params;

    bless $self, $class;
}


# As of 2013-06-04, this hash shows what a mapset record, as returned
# by QCMG::DB::QcmgReader->fetch_metatdata() actually looks like.
# Note that this data structure is somewhat brittle because Conrad/Matt
# will vary it as users request mods including new fields so be careful!
# Also note that spaces and mixed case have been removed from all keys
# in the hash.
# 
# {
#   'parent_project' => 'icgc_pancreatic',
#   'reference_genome_file' => '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
#   'project' => 'APGI_2156',
#   'parent_project_label' => 'ICGC pancreatic cancer project',
#   'failed_qc' => 0,
#   'primary_library' => 'Library_20110404_D',
#   'capture_kit' => 'Human All Exon 50Mb (SureSelect)',
#   'mapset' => 'S0411_20110418_2_FragPEBC.nopd.bcB16_02',
#   'researcher_annotation' => '',
#   'project_open' => '1',
#   'sequencing_platform' => 'SOLiD4',
#   'isize_min' => '81',
#   'sample' => 'ICGC-ABMJ-20100903-05-TD',
#   'alignment_required' => '1',
#   'project_limsid' => 'BIA4903',
#   'isize_manually_reviewed' => 0,
#   'aligner' => 'bioscope',
#   'library_protocol' => 'SOLiD v4 Multiplexed SpriTE',
#   'parent_project_path' => '/mnt/seq_results/icgc_pancreatic',
#   'project_prefix' => 'APGI',
#   'mapset_limsid' => '92-66527',
#   'gff_file' => '/panfs/share/qsegments/SureSelect_All_Exon_50mb_filtered_baits_1-200_20110524_shoulders.gff3',
#   'species_reference_genome' => 'Homo sapiens (GRCh37_ICGC_standard_v2)',
#   'study_group' => '{"Pancreas_StudyID_12":true,"Pancreas_StudyID_16":true,"Pancreas_StudyID_22":true,"Pancreas_StudyID_30":true}',
#   'sample_limsid' => 'BIA4903A2',
#   'sample_code' => '7:Primary tumour',
#   'isize_max' => '263',
#   'material' => '1:DNA'
# };
#
#
# This is the format for an older record (from 2013-02-20). Note that
# many of the key values have spaces and mixed case.
# 
# {
#   'Expression Array' => '',
#   'isize Manually Reviewed' => 0,
#   'Species Reference Genome' => 'Homo sapiens (GRCh37_ICGC_standard_v2)',
#   'Mapset' => '130206_7001240_0145_AD1JLWACXX.lane_3.TAGGCATG-CTCTCTAT',
#   'Methylation Array' => '',
#   'Reference Genome File' => '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
#   'Library Protocol' => 'Illumina Nextera Rapid Capture Exome Manual',
#   'Aligner' => 'bwa',
#   'Sample Code' => '10:Cell line derived from xenograft',
#   'Donor' => 'APGI_1992',
#   'Sample' => 'ICGC-ABMJ-20120706-01',
#   'Researcher Annotation' => undef,
#   'Primary Library' => 'Library_NRET_2',
#   'Failed QC' => 0,
#   'Library Type' => 'Fragment',
#   'isize Min' => '60',
#   'Sequencing Platform' => 'HiSeq2000',
#   'Alignment Required' => '1',
#   'SNP Array' => '8803730164R05C01',
#   'Sample LIMS ID' => 'BIA4857A11',
#   'Capture Kit' => 'Human Rapid Exome (Nextera)',
#   'Material' => '1:DNA',
#   'Donor LIMS ID' => 'BIA4857',
#   'Study Group' => '{"Pancreas_StudyID_12":true}',
#   'GFF File' => '/panfs/share/qsegments/CEX_Manifest_01242013_shoulders.gff3',
#   'Project' => 'icgc_pancreatic',
#   'Sequencing Type' => 'Paired End Sequencing',
#   'isize Max' => '510',
#   'Mapset LIMS ID' => '92-106009'
# };


sub group {
    my $self = shift;
    return $self->{group} = shift if @_;
    return $self->{group};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub attributes {
    my $self = shift;
    return $self->{attributes} = shift if @_;
    return $self->{attributes};
}


sub attribute {
    my $self = shift;
    my $attr = shift;

    if (@_) {
        return $self->{attributes}->{$attr} = shift;
    }
    else {
        return defined $self->{attributes}->{$attr} ?
                       $self->{attributes}->{$attr} : undef;
    }
}


sub mapsets_grouped_by_mapset_fields {
    my $self    = shift;
    my $ra_keys = shift; # list of fields to be used to group BAMs

    my @keys = @{ $ra_keys };
    my %mapset_groups = ();

    foreach my $rec ( $self->mapsets ) {
        my @attributes = ();
        foreach my $key (@keys) {

            # Mapset is a special case - it is globally unique and the
            # point of this whole routine is to group Mapsets by their
            # other attributes so if we see mapset, we just skip over it
            # when building the hash.  There may be other attributes
            # that need to be treated this way also (e.g. Primary Library).

            next if ($key eq 'mapset' or
                     $key eq 'primary_library');

            my $value = (exists $rec->{$key} and defined $rec->{$key}) ?
                        $rec->{$key} : 'NULL';

            push @attributes, [ $key, $value ];
        }

        # Work out if we have seen this group before.  If so add the
        # mapset, otherwise create a new MapsetCollection

        #print Dumper \@attributes;
        my $group_name = _string_from_attributes( \@attributes );
        print $group_name ,' ', $rec->{mapset},"\n";

        if (exists $mapset_groups{ $group_name }) {
            $mapset_groups{ $group_name }->add_mapset( $rec );
        }
        else {
            my $msc = QCMG::QBamMaker::MapsetCollection->new(
                          verbose => $self->verbose );
            $msc->add_mapset( $rec );
            $msc->attributes( \@attributes );
            $mapset_groups{ $group_name } = $msc;
        }

    }

    return \%mapset_groups;
}


sub string_from_attributes {
    my $self    = shift;
    my $ra_keys = shift;

    my @values = ();

    foreach my $key (@{ $ra_keys }) {
        my $new_value = $self->attribute( $key );
        # Cope with the case that the requested attribute is not present
        $new_value = 'NoValue' unless defined $new_value;

        $new_value =~ s/[^a-zA-Z0-9]/ /g;      # ditch any non-alphanum chars
        $new_value =~ s/(\b)([a-z])/$1\u$2/g;  # uc() first letters of strings
        $new_value =~ s/\s//g;                 # ditch any remaining spaces

        # If we end up with an empty string, we will use 'NULL'
        $new_value = $new_value ? $new_value : 'NULL';
        push @values, $new_value;
    }

    return join('_',@values);
}


sub co_line_from_attributes {
    my $self    = shift;
    my $ra_keys = shift;

    my @formatted_attribs = ();

    foreach my $key (@{ $ra_keys }) {
        my $new_value = $self->attribute( $key );
        # Cope with the case that the requested attribute is not present
        $new_value = 'NULL' unless defined $new_value;

        push @formatted_attribs, $key .'='. $new_value;
    }

    return join("\t",@formatted_attribs);
}


#sub bam_name {
#    my $self = shift;
#
#    # This logic will not cope with smg_core projects which live under
#    # /mnt/seq_results/smg_core, not /mnt/seq_results
#    my $bam_name = '/mnt/seq_results/' .
#                   $self->attribute('parent_project') . '/' .
#                   $self->attribute('project') . '/seq_mapped/' .
#                   $self->attribute('mapset') . '.bam';
#    return $bam_name;
#}



1;
__END__


=head1 NAME

QCMG::QBamMaker::Mapset - A data structure for LIMS mapsets


=head1 SYNOPSIS

 use QCMG::QBamMaker::Mapset;


=head1 DESCRIPTION

This module provides a very basic structure to hold information about
QCMG mapsets.
In most cases, users should not be using this class directly but rather
should be creating a QCMG::QBamMaker::MapsetCollection object and using
the initialise_from_lims() method from that class to create Mapset 
objects for all of the mapsets in the LIMS.

As of 2013-06-04, this hash shows what a mapset record, as returned
by QCMG::DB::QcmgReader->fetch_metatdata() actually looks like.
Note that this data structure is somewhat brittle because Conrad/Matt
will vary it as users request mods including new fields so be careful!
Also note that spaces and mixed case have been removed from all keys
in the hash.

 {
   'parent_project' => 'icgc_pancreatic',
   'reference_genome_file' => '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
   'project' => 'APGI_2156',
   'parent_project_label' => 'ICGC pancreatic cancer project',
   'failed_qc' => 0,
   'primary_library' => 'Library_20110404_D',
   'capture_kit' => 'Human All Exon 50Mb (SureSelect)',
   'mapset' => 'S0411_20110418_2_FragPEBC.nopd.bcB16_02',
   'researcher_annotation' => '',
   'project_open' => '1',
   'sequencing_platform' => 'SOLiD4',
   'isize_min' => '81',
   'sample' => 'ICGC-ABMJ-20100903-05-TD',
   'alignment_required' => '1',
   'project_limsid' => 'BIA4903',
   'isize_manually_reviewed' => 0,
   'aligner' => 'bioscope',
   'library_protocol' => 'SOLiD v4 Multiplexed SpriTE',
   'parent_project_path' => '/mnt/seq_results/icgc_pancreatic',
   'project_prefix' => 'APGI',
   'mapset_limsid' => '92-66527',
   'gff_file' => '/panfs/share/qsegments/SureSelect_All_Exon_50mb_filtered_baits_1-200_20110524_shoulders.gff3',
   'species_reference_genome' => 'Homo sapiens (GRCh37_ICGC_standard_v2)',
   'study_group' => '{"Pancreas_StudyID_12":true,"Pancreas_StudyID_16":true,"Pancreas_StudyID_22":true,"Pancreas_StudyID_30":true}',
   'sample_limsid' => 'BIA4903A2',
   'sample_code' => '7:Primary tumour',
   'isize_max' => '263',
   'material' => '1:DNA'
 };


=head1 PUBLIC METHODS

=over 2

=item B<new()>

 my $obj = QCMG::QBamMaker::Mapset->new(
                                 parent_project => 'smgres_meso',
                                 verbose => 1 );

You should almost certainly not be using this method directly.  It's
more likely that you want to create a QCMG::QBamMaker::MapsetCollection
object and use the
initialise_from_lims() method to create Mapset objects for all of the
mapsets in the LIMS.

If you do want to manually create a Mapset object, you should pass all
of the values for the mapset attributes in the hash elements to new(),
for example:

 my $obj = QCMG::QBamMaker::Mapset->new(
               parent_project => 'icgc_pancreatic',
               project => 'APGI_2156',
               parent_project_label => 'ICGC pancreatic cancer project',
               failed_qc => 0,
               primary_library => 'Library_20110404_D',
               capture_kit => 'Human All Exon 50Mb (SureSelect)',
               ...
               );

=item B<attribute()>

 my $project = $obj->attribute( parent_project );
 my $project = $obj->attribute( project );
 my $project = $obj->attribute( primary_library );

Get accessor for attribute values.  There is no set accessor for
attributes.

=item B<verbose()>

 $obj->verbose( 2 );
 my $verb = $bam->verbose;

Accessor for verbosity level of progress reporting.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: Mapset.pm 4665 2014-07-24 08:54:04Z j.pearson $


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
