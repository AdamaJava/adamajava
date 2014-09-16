package QCMG::QBamMaker::AutoMode;

##############################################################################
#
#  Module:   QCMG::QBamMaker::AutoMode
#  Author:   John V Pearson
#  Created:  2013-02-20
#
#  This module creates PBS submittable jobs that build all possible
#  seq_final BAMs for a given project and Donor.
#
#  It has to be able to apply multiple different filtering and fixing
#  profiles so it can be used at multiple levels of granularity, e.g.
#  genome and exome separate, genome and exome collapsed, SOLiD
#  lifescope and bioscope exome collapsed but not HiSeq, etc, etc.  We
#  will call these different systems "profiles".
#
#  $Id: AutoMode.pm 4665 2014-07-24 08:54:04Z j.pearson $
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

#use QCMG::DB::Metadata;
#use QCMG::DB::QcmgReader;
use QCMG::FileDir::Finder;
use QCMG::QBamMaker::MapsetCollection;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %CONSTRAINTS %PROFILES );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: AutoMode.pm 4665 2014-07-24 08:54:04Z j.pearson $'
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
                  9   => { naming_attributes => 
                               [ 'parent_project', 'project', 'material',
                                 'sample_code',
                                 'library_protocol', 'capture_kit',
                                 'aligner', 'sequencing_platform'
                               ],
                           checking_attributes => 
                               [ 'species_reference_genome',
                                 'reference_genome_file',
                                 'failed_qc',
                                 'sample' ]
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
                                         $params{rootdir} : '/mnt/seq_results'),
                 verbose             => ($params{verbose} ?
                                         $params{verbose} : 0),
               };

    bless $self, $class;


    # If a mapset collection was passed in then we go with it, otherwise
    # we initialise from the LIMS.  This mechanism can be used to 
    # run qBamMaker against lists of mapsets not in the LIMS - we read
    # in an external file of mapsets, create a MapsetCollection manually
    # for each donor and pass it to this constructor.

    my $msc = undef;
    if (exists $params{mapset_collection} and defined $params{mapset_collection}) {
        # Use a passed-in MapsetCollection object
        my $obj_type = ref( $params{mapset_collection} );
        die 'parameter mapset_collection must be an object of type '.
            "QCMG::QBamMaker::MapsetCollection not $obj_type \n"
            unless ( $obj_type eq 'QCMG::QBamMaker::MapsetCollection' );
        $msc = $params{mapset_collection};
    }
    else {
        # Create and initialise a MapsetCollection object from the LIMS
        $msc = QCMG::QBamMaker::MapsetCollection->new( verbose => $self->verbose );
        $msc->initialise_from_lims; 
    }
                                  
    # project and parent_project are complusory constraints so apply them now 
    $msc->apply_constraint( 'parent_project', $self->parent_project );
    $msc->apply_constraint( 'project',   $self->project );

    # We'll need a full pathname later so set it for each mapset now
    foreach my $mapset ($msc->mapsets) {
        $mapset->attribute( '_pathName', $self->rootdir .'/'.
                                         $self->parent_project .'/'.
                                         $self->project .'/'.
                                         'seq_mapped/'.
                                         $mapset->attribute('mapset').'.bam' );
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


sub apply_profile {
    my $self    = shift;
    my $profile = shift;

    if ($profile == 1) {
        return $self->apply_profile_1;
    }
    elsif ($profile == 9) {
        return $self->apply_profile_9;
    }
    elsif ($profile == 666) {
        # Special "do nothing" debugging mode
    }
    else {
       die "profile [$profile] is not implemented";
    }
}


sub apply_profile_1 {
    my $self = shift;

    my $json = JSON->new->allow_nonref;
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

            # Collapse Hiseq2000 and HiSeq2500 in all circumstances and
            # if it's a SOLiD 5500 run that has been remapped with
            # Bioscope then change platform to SOLiD4
            if ($attribute eq 'sequencing_platform') {
                if (defined $value and ( $value =~ /HiSeq2000/i or
                                         $value =~ /HiSeq2500/i ) ) {
                    $value = 'HiSeq';
                }
                if (defined $value and $value =~ /SOLiD5500xL/i and
                    $mapset->attribute('aligner') =~ /Bioscope/i ) {
                    $value = 'SOLiD4';
                }
            }

            # Insert NoCapture for mapsets with an empty Capture_Kit field
            elsif ($attribute eq 'capture_kit') {
                if (! defined $value or $value eq '') {
                    $value = 'NoCapture';
                }
            }

            ## Test case for futzing with JSON-encoded fields
            #my $study_group = $mapset->attribute('Study Group');
            #if (defined $study_group and $study_group) {
            #    my $rh_json = $json->decode( $study_group );
            #    print Dumper $mapset->{'Study Group'}, $rh_json;
            #    if (defined $rh_json->{'Pancreas_StudyID_12'} and
            #        $rh_json->{'Pancreas_StudyID_12'} eq JSON::true ) {
            #        print $mapset->{mapset}." is in study group 12\n";
            #    }
            #}

            # If the Researcher_Annotation field indicates that we want
            # the sample to behave as though it were another sample then
            # do that substitution now.
            elsif ($attribute eq 'sample') {
                my $researcher_annot = $mapset->attribute('researcher_annotation');
                if (defined $researcher_annot and $researcher_annot) {
                    my $rh_json = $json->decode( $researcher_annot );
                    if (defined $rh_json->{'TreatAsThoughSampleIs'}) {
                        qlogprint ' replacing sample ID '. $mapset->attribute('sample') .
                                  ' with sample ID ' . $rh_json->{'TreatAsThoughSampleIs'}.
                                  ' for mapset: '.
                                  $mapset->attribute('mapset'). "\n"
                            if $self->verbose;
                        $value = $rh_json->{'TreatAsThoughSampleIs'};
                    }
                }
            }
            
            # For SOLiD Agilent exomes, collapse 4 library protocols and
            # for genomes (no capture) collapse frags and LMPs.
            elsif ($attribute eq 'library_protocol') {
                if (defined $value and
                    ( $value =~ /SOLiD v4 Library Builder/i or
                      $value =~ /SOLiD v4 SpriTE/i or
                      $value =~ /SureSelect PreCapture Manual/i or
                      $value =~ /SOLiD v4 Multiplexed SpriTE/i ) and
                    ( defined $mapset->attribute('capture_kit') and 
                      $mapset->attribute('capture_kit') =~
                          /Human All Exon 50Mb \(SureSelect\)/i ) ) {
                    $value = 'SOLiD4Library';
                }
                elsif (defined $value and
                    ( $value =~ /SOLiD 4 LMP Protocol/i or
                      $value =~ /5500 LMP Protocol/i or
                      $value =~ /SOLiD v4 Manual/i ) and
                    ( $mapset->attribute('aligner') eq 'bioscope' ) and
                    ( ! defined $mapset->attribute('capture_kit') or
                      $mapset->attribute('capture_kit') eq '' ) ) {
                    $value = 'SOLiD4Library';
                }
            }

            # Regardless of which attribute we are processing we need to
            # remember it for naming and grouping.  The LIMS does contain
            # records that are not yet mapped in which case there will be
            # empty fields, particularly Aligner.  We need some serious
            # complaining if any value is empty.

            my $new_value = _transform_string($value);
            if (! $new_value) {
                my $mapset_bam = $self->rootdir .'/'.
                                 $mapset->attribute('parent_project') .'/'.
                                 $mapset->attribute('project') .'/seq_mapped/'.
                                 $mapset->attribute('mapset') .'.bam';
                if (! -e $mapset_bam) {
                    qlogprint "empty field [$attribute] but mapset is not in seq_mapped so it's probably OK: ", 
                              $mapset->attribute('mapset')."\n" if $self->verbose;
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

    #print Dumper \%mapset_groups;

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


sub apply_profile_9 {
    my $self = shift;

    # This profile is for looking for cases where there are mapsets that
    # would be merged together BUT they have different Sample values.

    my $json = JSON->new->allow_nonref;
    my $profile = $PROFILES{ 9 };
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

            # Collapse Hiseq2000 and HiSeq2500
            if ($attribute eq 'sequencing_platform') {
                if (defined $value and ( $value =~ /HiSeq2000/i or
                                         $value =~ /HiSeq2500/i ) ) {
                    $value = 'HiSeq';
                }
                if (defined $value and $value =~ /SOLiD5500xL/i and
                    $mapset->attribute('aligner') =~ /Bioscope/i ) {
                    $value = 'SOLiD4';
                }
            }

            # Insert NoCapture for mapsets with an empty Capture_Kit field
            elsif ($attribute eq 'capture_kit') {
                if (! defined $value or $value eq '') {
                    $value = 'NoCapture';
                }
            }

            # Regardless of which attribute we are processing we
            # need to remember it for naming and grouping.  We need some
            # serious complaining if any value is empty.

            my $new_value = _transform_string($value);
            if (! $new_value) {
                my $mapset_bam = $self->rootdir .'/' .
                                 $mapset->attribute('parent_project') .'/'.
                                 $mapset->attribute('project') .'/seq_mapped/'.
                                 $mapset->attribute('mapset') .'.bam';
                if (! -e $mapset_bam) {
                    qlogprint "empty field [$attribute] but mapset is not in seq_mapped so it's probably OK: ", 
                              $mapset->attribute('mapset') if $self->verbose;
                }
                else {
                    warn "empty field [$attribute] in mapset: ",
                         $mapset->attribute('mapset'), "\n";
                }
                $found_empty = 1;
            }
            push @names, $new_value;
            $grouping_attributes{ $attribute } = $new_value;
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
                     warn "group $group attribute $attribute mismatch:\n";
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
            warn "abandoning processing of group $group\n";
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
                                      $mapset->attribute('primary_library')
                          ."\n";
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

QCMG::QBamMaker::AutoMode - Run the "auto" mode for qBamMaker


=head1 SYNOPSIS

 use QCMG::QBamMaker::AutoMode;

 my $auto = QCMG::QBamMaker::AutoMode->new( parent_project => 'icgc_ovarian',
                                            project => 'AOCS_002',
                                            verbose => 1 );
 $auto->apply_profile( 1 );


=head1 DESCRIPTION

This module creates PBS submittable jobs that build all possible
seq_final BAMs for a given project and Donor.

It has to be able to apply multiple different filtering and fixing
profiles so it can be used at multiple levels of granularity, e.g.
genome and exome separate, genome and exome collapsed, SOLiD
lifescope and bioscope exome collapsed but not HiSeq, etc, etc.  We
will call these different systems "profiles".


=head2 Public Methods

=over 2

=item B<new()>

 my $auto = QCMG::QBamMaker::AutoMode->new( parent_project => 'icgc_ovarian',
                                            project => 'AOCS_002',
                                            verbose => 1 );

 my $auto = QCMG::QBamMaker::AutoMode->new( parent_project => 'smgres_ghq',
                                            rootdir => '/mnt/seq_results/smg_core',
                                            project => 'GQ_0002',
                                            verbose => 1 );

The B<new()> method takes 2 mandatory and 2 optional parameters.  The
mandatory params are I<parent_project> and I<project> which should be self
explanatory from the example.  The first optional param is I<verbose> which 
defaults to 0 and indicates the level of verbosity in reporting.

The second optional parameter is I<rootdir> which needs some
explanation.  In most cases, parent_project directories sit under
B</mnt/seq_results> but for some projects, there is another directory
layer, for example, for smg_core projects, the parent_project directories
sit under B</mnt/seq_results/smg_core>.  In these special cases, you
need to explicitly tell the module what is the rootdir under which the
parent_project directory can be located.

 my $auto = QCMG::QBamMaker::AutoMode->new( parent_project => 'smgres_gemm',
                                            project => 'GEMM_9901',
                                            mapset_collection => $msc,
                                            verbose => 1 );

You can optionally pass a QCMG::QBamMaker::MapsetCollection object to
new() in which case it is used as the basis of all subsequent processing
rather than initialising a MapsetCollection from the QCMG LIMS.

This feature is useful for the case where we need to run 
qBamMaker against lists of mapsets not in the LIMS - we read
in an external file of mapsets, create a MapsetCollection manually
for each donor and pass it to this constructor.

=item B<apply_profile()>

 $auto->apply_profile( 1 );

A profile is a list of selection and grouping criteria which are applied
as a set to modify the contents of mapsets and prepare them to be
written out as groups.  For details on the available profiles, see the
qbammaker.pl wiki page under the B<--auto> and B<--automode> sections.

=item B<rootdir()>

 my $dir = $auto->rootdir;

Get-only accessor for directory under which the parent_project directory
can be found.  Defaults to B</mnt/seq_results> unless a value is
supplied to B<new()>.

=item B<parent_project()>

 my $study = $auto->parent_project;

Get-only accessor for parent_project (study).

=item B<project()>

 my $donor = $auto->project;

Get-only accessor for project (donor).

=item B<verbose()>

 $auto->verbose( 2 );
 my $verb = $auto->verbose;

Accessor for verbosity level of progress reporting.

=back

=head2 Profiles

A profile is made up of two sets of attributes - naming and checking.
Attributes are properties of mapsets and are used to sort and group
mapsets into MapsetCollection objects which can be used to write PBS
files for final BAM creation.

Naming attributes will be used in the naming of the BAM file that will
be written out for a mapset collection.
The checking attributes are like naming attributes in that they must 
be identical for all mapsets within a mapset group BUT they are not
part of the naming or classifying.  Rather, groups are constructred
using the naming attributes and then each group is checked to make
sure that all mapsets in the group have the same values for the
checking attributes.  All naming AND checking attributes must be
output in the BAM @CO qlimsmeta line.

There are currently 2 profiles available:

 1:  naming_attributes    parent_project
                          project
                          material
                          sample_code
                          sample
                          library_protocol
                          capture_kit
                          aligner
                          sequencing_platform
     checking_attributes  species_reference_genome
                          reference_genome_file
                          failed_qc

 9:  naming_attributes    parent_project
                          project
                          material
                          sample_code
                          library_protocol
                          capture_kit
                          aligner
                          sequencing_platform
     checking_attributes  species_reference_genome
                          reference_genome_file
                          failed_qc
                          sample

Practically, profiles 1 and 9 end up with the same BAM files in each
group but the BAM names will be different because profile 1 has sample
as a naming attribute whereas it is only a checking attribute in profile
9.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: AutoMode.pm 4665 2014-07-24 08:54:04Z j.pearson $


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
