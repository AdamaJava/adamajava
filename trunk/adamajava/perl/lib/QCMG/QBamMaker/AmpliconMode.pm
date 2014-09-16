package QCMG::QBamMaker::AmpliconMode;

##############################################################################
#
#  Module:   QCMG::QBamMaker::AmpliconMode
#  Author:   John V Pearson
#  Created:  2013-03-20
#
#  This module creates PDB submittable jobs that build BAMs for
#  Ion Torrent amplicon sequencing.  It is loosely based on AutoMode.pm
#  but with some differences:
#
#  1. Not all amplicon BAMs are in the LIMS so as well as a LIMS query,
#     we have to look in the seq_amplicon directory and create/add to
#     MapsetCollections based on the BAMs we find.
#  2. We will be guessing many attributes for the disk-only BAMs.
#
#  $Id: AmpliconMode.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use JSON;
use Pod::Usage;

use QCMG::DB::Metadata;
use QCMG::FileDir::Finder;
use QCMG::QBamMaker::Mapset;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %CONSTRAINTS %PROFILES );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: AmpliconMode.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures

# Naming attributes are those attributes that will be used in the
# sorting/grouping of mapsets into MapsetCollections.  They will also be
# used in the naming of the BAM file that will be written out for a
# mapset collection.  The checking attributes are like naming attributes
# in that they must be uniform within a mapset group BUT they are not
# part of the naming or classifying.  Rather, groups are constructred
# using the naming attributes and then each group is checked to make
# sure that all mapsets in the group have the same values for the
# checking attributes.  All naming AND checking attributes must be
# output in the BAM @CO qlimsmeta line.

BEGIN {
    %PROFILES = ( 1   => { naming_attributes => 
                               [ 'parent_project', 'project', 'material',
                                 'sample_code', 'sample',
                                 'library_protocol', 'capture_kit',
                                 'aligner', 'sequencing_platform'
                               ],
                           checking_attributes => 
                               [ 'species_reference_genome',
                                 'reference_genome_file',
                                 'failed_qc'
                               ]
                         },
                  666 => { naming_attributes => 
                               [ 'parent_project', 'project' ],
                         }
                );
}


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

sub new {
    my $class  = shift;
    my %params = @_;

    croak "new() requires a parent_project name"
        unless (exists $params{parent_project} and defined $params{parent_project});
    croak "new() requires a project name"
        unless (exists $params{project} and defined $params{project});

    my $self = { parent_project      => $params{parent_project},
                 project             => $params{project},
                 mapset_collection   => undef,
                 rootdir             => ($params{rootdir} ?
                                         $params{rootdir} : '/mnt/seq_results' ),
                 verbose             => ($params{verbose} ?
                                         $params{verbose} : 0),
               };

    bless $self, $class;

    my $msc = QCMG::QBamMaker::MapsetCollection->new( verbose => $self->verbose );
    $msc->initialise_from_lims; 
                                  
    $msc->apply_constraint( 'parent_project', $self->parent_project );
    $msc->apply_constraint( 'project',   $self->project );
    $msc->apply_constraint( 'aligner', 'Tmap' );
    $msc->apply_constraint( 'sequencing_platform', 'IonPGM' );
    $msc->apply_constraint( 'library_protocol',
                            'Amplicon Fragment Library \(manual\)' );

    # We'll need a full pathname later so set it now
    foreach my $mapset ($msc->mapsets) {
        $mapset->attribute( '_pathName', $self->rootdir .'/'.
                                         $self->parent_project .'/'.
                                         $self->project .'/'.
                                         'seq_mapped/'.
                                         $mapset->attribute( 'mapset' ));
    }


    $self->{mapset_collection} = $msc;

    return $self;
}


sub mapset_collection {
    my $self = shift;
    return $self->{mapset_collection};
}

sub mapset_count {
    my $self = shift;
    return $self->mapset_collection->mapset_count;
}

sub rootdir {
    my $self = shift;
    return $self->{rootdir};
}

sub project {
    my $self = shift;
    return $self->{project};
}

sub parent_project {
    my $self = shift;
    return $self->{parent_project};
}

sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub add_BAMs_from_directory {
    my $self = shift;
    my $dir  = shift;

    my $finder = QCMG::FileDir::Finder->new( verbose => $self->verbose );
    my @bams = $finder->find_file( $dir, '.bam$' );

    foreach my $bam (@bams) {
        my $map = QCMG::QBamMaker::Mapset->new( verbose => $self->verbose );

        # Some of the mapset attributes are easy to set because they are
        # fixed and so require no thought.

        $map->attribute( '_pathName' => $bam );
        $map->attribute( 'parent_project' => $self->parent_project );
        $map->attribute( 'project' => $self->project );
        $map->attribute( 'material' => '1:DNA' );
        $map->attribute( 'sequencing_platform' => 'IonPGM' );
        $map->attribute( 'aligner' => 'tmap' );
        $map->attribute( 'library_protocol' => 'Amplicon Fragment Library (manual)' );
        $map->attribute( 'primary_library' => 'MadeUpByQBamMakerForAmplicons' );
        $map->attribute( 'failed_qc' => 0 );
        $map->attribute( 'species_reference_genome' => 'Homo sapiens (GRCh37_ICGC_standard_v2)' );
        $map->attribute( 'reference_genome_file' =>
                         '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa' );

        # Some of the mapset attributes are not so easy to set because they 
        # are variable and so require some calculation or pattern matching 

        my $mapset = $bam;
        $mapset =~ s/.*\///g;
        $map->attribute( 'mapset' => $mapset );

        my $sample_code = '';
        my $sample = '';
        if ($mapset =~ /a(4NormalOther)/i) {
            $sample = 'QBM-'. $1;
            $sample_code = '4:Normal control (other site)';
        }
        elsif ($mapset =~ /a(7PrimaryTumour)/i) {
            $sample = 'QBM-'. $1;
            $sample_code = '7:Primary tumour';
        }
        else {
            die "Unable to parse SampleCode out of BAM: $mapset\n";
        }
        $map->attribute( 'sample_code' => $sample_code );
        $map->attribute( 'sample' => $sample );

        $self->mapset_collection->add_mapset( $map );
    }

    qlogprint 'Added ',scalar(@bams)," amplicon mapsets from seq_amplicon\n";
}


sub process {
    my $self = shift;

    my $profile = $PROFILES{ 1 };
    my @naming_attributes   = @{ $profile->{naming_attributes} };
    my @checking_attributes = @{ $profile->{checking_attributes} };

    # Assign a group string to each mapset based on the naming_attributes
    # for the given profile.  When we assemble MapsetCollections later 
    # for PBS job creation, we will use this group string to determine
    # which group they are in.
 
    my %mapset_groups = ();
    foreach my $mapset ($self->mapset_collection->mapsets) {
        my $found_empty = 0;

        # Skip any records that fail QC
        next if $mapset->attribute( 'failed_qc' ) != 0;

        my @names = ();
        my %grouping_attributes = ();

        foreach my $attribute (@naming_attributes) {
            my $value = $mapset->attribute( $attribute );

            # Insert NoCapture for mapsets with an empty Capture_Kit field
            if ($attribute eq 'capture_kit') {
                if (! defined $value or $value eq '') {
                    $value = 'NoCapture';
                }
            }

            # Regardless of which attribute we are processing we need to
            # remember it for naming and grouping.  The LIMS does contain
            # records that are not yet mapped in which case there will be
            # empty fields, particularly Aligner.  We need some serious
            # complaining if any value is empty.

            my $new_value = _transform_string($value);
            if (! $new_value) {
                if (! -e $mapset->attribute( '_pathName' )) {
                    qlogprint "empty field [$attribute] but mapset is not in seq_mapped so it's probably OK: ",
                         $mapset->attribute('mapset');
                }
                else {
                    warn "empty field [$attribute] in mapset: ",
                         $mapset->attribute('mapset'), "\n";
                }
                $found_empty = 1;
            }

            # We will use the transformed value for naming BUT we will
            # use the original value for grouping.
            push @names, $new_value;
            $grouping_attributes{ $attribute } = $value;
        }

        # Skip any mapsets with empty attributes
        next if $found_empty;

        $mapset->group( join('_',@names) );

        # Now we look to put this mapset into a group!
        if (exists $mapset_groups{ $mapset->group }) {
            $mapset_groups{ $mapset->group }->add_mapset( $mapset );
        }
        else {
            my $msc = QCMG::QBamMaker::MapsetCollection->new(
                          verbose => $self->verbose );
            $msc->add_mapset( $mapset );
            $mapset_groups{ $mapset->group } = $msc;
            $msc->{grouping_attributes} = \%grouping_attributes;
        }
    }

    # If there were any empty fields then we should die immediately
    # because it is not possible to build sensible groups;

    #die "Cannot continue until all empty fields have been resolved\n"
    #    if $found_empty;

    # Now that we have the groups, we need to use the profile
    # checking_attributes to make sure that all mapsets in the group
    # have the same values for these attributes.  If we have any
    # problems, we need to whinge and ditch the mapset group so no
    # further processing happens on it.

    my %problems = ();
    foreach my $group (sort keys %mapset_groups) {
        my $msc = $mapset_groups{ $group };
        my @mapsets = $msc->mapsets;
        my $exemplar = $mapsets[0];
        foreach my $mapset (@mapsets) {
            foreach my $attribute (@checking_attributes) {
                if ( $mapset->attribute( $attribute ) ne
                     $exemplar->attribute( $attribute ) ) {
                     # Tally the problems and complain
                     $problems{ $group }++;
                     warn "group [$group] attribute [$attribute] mismatch:\n";
                     warn '  ', $exemplar->attribute( $attribute ),
                          ' ',  $exemplar->attribute( 'mapset' ), "\n";
                     warn '  ', $mapset->attribute( $attribute ), 
                          ' ',  $mapset->attribute( 'mapset' ), "\n";
                }
            }
        }
    }

    # If a MapsetCollection is marked as having problems we drop it
    # completely.  Otherwise we need to add the checking_attributes as
    # grouping attributes so they can be reported out in the PBS file.

    foreach my $group (sort keys %mapset_groups) {
        if (exists $problems{ $group }) {
            warn "problems found so abandoning processing of group: $group\n";
            delete $mapset_groups{ $group };
        }
        else {
            my $msc = $mapset_groups{ $group };
            my @mapsets = $msc->mapsets;
            my $exemplar = $mapsets[0];
            foreach my $check (@checking_attributes) {
                $msc->{grouping_attributes}->{$check} =
                    $exemplar->attribute( $check );
            }
        }
    }

    # We are going to treat the Ion Torrent amplicon as a special case.
    # We have put these bams in seq_amplicon rather than seq_mapped
    # while we try to figure out how to deal with them.

    return \%mapset_groups;
}


sub _transform_string {
    my $string = shift;
    return '' unless defined $string;   # return empty if undef
    $string =~ s/[^a-zA-Z0-9]/ /g;      # ditch any non-alphanum chars
    $string =~ s/(\b)([a-z])/$1\u$2/g;  # uc() first letters of strings
    $string =~ s/\s//g;                 # ditch any remaining spaces
    # If we end up with an empty string, we will use 'NULL'
    $string = $string ? $string : 'NULL';
    return $string;
}


sub write_pbs_scripts {
    my $self   = shift;
    my %params = @_;

    $params{pbs_dir} .= '/' unless ($params{pbs_dir} =~ /.*\/$/);
    $params{bam_dir} .= '/' unless ($params{bam_dir} =~ /.*\/$/);

    # This routine has to be a one-stop-shop for printing out all of the
    # PBS files so it has to look at the BAM groupings, make a new 
    # MapsetCollection object for each one and then write out a PBS file
    # to the appropriate directory so we end up with an army of PBS
    # scripts for each donor.

    my $rh_mapsets_as_groups = $params{mapset_groups};

    my $ctr = 1;
    foreach my $group (sort keys %{$rh_mapsets_as_groups}) {
        qlogprint $ctr++,":  $group\n" if ($self->verbose > 1);
        my $msc = $rh_mapsets_as_groups->{$group};

        # In automode we will need to manually set a cmdline param for
        # each of our synthetic MapsetCollection's.
        $msc->cmdline( $params{cmdline} ) if defined $params{cmdline};
        
        if ($self->verbose > 1) {
            foreach my $mapset ($msc->mapsets) {
                qlogprint '        '. $mapset->attribute('mapset') ."\t". 
                                      $mapset->attribute('sample') ."\t". 
                                      $mapset->attribute('primary_library') ."\n";
            }
        }

        my $final_bam_name_stem = $group .'.'. $ENV{USER};
        my $final_bam_pathname  = $params{bam_dir} . $final_bam_name_stem .'.bam';
        my $pbs_script_pathname = $params{pbs_dir} . $final_bam_name_stem .'.pbs';

        # Create name for PBS job based on donor ID
        my @mapsets = $msc->mapsets;
        my $pbs_job_name = 'qbm' . $mapsets[0]->attribute('project');
        $pbs_job_name =~ s/[^A-Za-z0-9]//g;

        # At this point we know the final BAM we are going to make plus
        # we have the list of mapsets we would write into it so this is
        # the point where we need to insert QX's uptodate logic and if
        # the final BAM is uptodate then we skip creating the PBS file.
        # We should also log a list of final BAMs that would be
        # recreated because by definition if these exist and are not
        # uptodate THEN the PBS file will not run unless someone deletes
        # the existing BAM file because we very sepcifically designed 
        # the PBS jobs so they will NEVER write over the top of an
        # existing BAM.  You have to get rid of the BAM by deleting or
        # renaming and then the new BAM can be created.


        $msc->write_pbs_script( final_bam_pathname  => $final_bam_pathname,
                                final_bam_name_stem => $final_bam_name_stem,
                                bam_dir             => $params{'bam_dir'},
                                pbs_job_name        => $pbs_job_name,
                                pbs_script_pathname => $pbs_script_pathname,
                               );

    }
}


1;
__END__


=head1 NAME

QCMG::QBamMaker::AmpliconMode - Run the "auto" mode for qBamMaker


=head1 SYNOPSIS

 use QCMG::QBamMaker::AmpliconMode;

 my $auto = QCMG::QBamMaker::AmpliconMode->new(
                parent_project => 'icgc_ovarian',
                project => 'AOCS_002',
                verbose => 1 );
 $auto->apply_profile( 1 );


=head1 DESCRIPTION

This module provides ...


=head1 PUBLIC METHODS

=over 2

=item B<new()>

 my $auto = QCMG::QBamMaker::AmpliconMode->new(
                parent_project => 'icgc_ovarian',
                project => 'AOCS_002',
                verbose => 1 );

 my $auto = QCMG::QBamMaker::AmpliconMode->new(
                parent_project => 'smgcore_ghq',
                project => 'GQ_0002',
                rootdir => '/mnt/seq_results/smg_core',
                verbose => 1 );

The B<new()> method takes 2 mandatory and 2 optional parameters.  The
mandatory params are I<parent_project> and I<project> which should be
self-explanatory from the example.  The first optional param is I<verbose>
which 
defaults to 0 and indicates the level of verbosity in reporting.
The second optional parameter is I<rootdir> which needs some
explanation.  In most cases, QCMG parent_project directories sit under
B</mnt/seq_results> but for some projects, there is another directory
layer, for example, for smg_core projects, the parent_project
directories
sit under B</mnt/seq_results/smg_core>.  In these special cases, you
need to explicitly tell the module the rootdir under which the
parent_project directory can be located.

=item apply_profile()>

 $auto->apply_profile( 1 );

A profile is a list of selection and grouping criteria whcih are applied
as a set to modify the contents of mapsets and prepare them to be
written out as groups.  For details on the available profiles, see the
qbammaker.pl wiki page under the B<--auto> and B<--automode> sections.

=item B<verbose()>

 $auto->verbose( 2 );
 my $verb = $auto->verbose;

Accessor for verbosity level of progress reporting.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: AmpliconMode.pm 4665 2014-07-24 08:54:04Z j.pearson $


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
