package QCMG::QBamMaker::MapsetCollection;


##############################################################################
#
#  Module:   QCMG::QBamMaker::MapsetCollection
#  Author:   John V Pearson
#  Created:  2013-02-15
#
#  This module is derived from the original qBamMaker.pl script whcih
#  got too complicated and needed to be broken up.
#
#  $Id: MapsetCollection.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use UUID::Tiny;

#use QCMG::DB::Metadata;
use QCMG::DB::QcmgReader;
use QCMG::FileDir::Finder;
use QCMG::QBamMaker::Mapset;
use QCMG::QBamMaker::SeqFinalBam;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %CONSTRAINTS $FORCE_PBS );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: MapsetCollection.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures

BEGIN {
    # This is the list of *permissible* constraints.  It can be extended
    # by assigning a new character, mapping it to the correct field name
    # in the mapset hash and adding a new CLI option to qBamMaker.pl so
    # users can specify a pattern for the field to be matched against.
    %CONSTRAINTS = ( p => 'parent_project',
                     d => 'project',
                     r => 'material',
                     s => 'sample_code',
                     e => 'sample',
                     b => 'primary_library',
                     a => 'aligner',
                     t => 'mapset',
                     c => 'capture_kit',
                     f => 'sequencing_platform',
                     q => 'failed_qc');

    # If we set this to 1 then PBS files will be written regardless of
    # the status of the target BAM.  This is DANGEROUS!  Don't use it
    # unless you really have to. 
    $FORCE_PBS = 0;
}



###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { mapsets              => [],
                 applied_constraints  => [],
                 constraint_order     => undef,
                 grouping_attributes  => [],
                 cmdline              => undef,
                 ordered_constraints  => [ values %CONSTRAINTS ],
                 verbose              => ($params{verbose} ?
                                          $params{verbose} : 0),
               };

    # attributes: an array of [key,value] pairs that describe the
    # mapset.  In the original use-case this was the list of fields that
    # were used to select the mapsets that went into this collection.

    bless $self, $class;

    # Set default constraint order
    $self->set_constraint_order( 'pdrsebatcfq' );

    return $self;
}


sub initialise_from_lims {
    my $self = shift;

    # Get metadata for all mapsets in LIMS
    #my $geneus = QCMG::DB::GeneusReader->new();
    my $geneus = QCMG::DB::QcmgReader->new();
    if ( $geneus->all_resources_metadata() ) {
        #$self->{mapsets} = $geneus->fetch_metadata();
        my $ra_recs = $geneus->fetch_metadata();
        foreach my $rec (@{ $ra_recs }) {

            # There are a selection of LMP runs done by LifeTech
            # that are special cases because they don't have a
            # "Library" process in the LIMS so there is no way to
            # assign them a Library Protocol.  To make the downstream
            # processing work, these mapsets need to get a faux Library
            # Protocol assigned upfront.
            if ( (! exists  $rec->{library_protocol} or
                  ! defined $rec->{library_protocol} or
                  !         $rec->{library_protocol} ) and
                 ( $rec->{mapset} =~ /anita.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /barb.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /clara.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /dot.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /joan.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /liz.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /nancy.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /rosalind.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /selma.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /teresa.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /tomoe.*_LMP.nopd.nobc/i or
                   $rec->{mapset} =~ /val.*_LMP.nopd.nobc/i ) ) {
                $rec->{library_protocol} = 'SOLiD 4 Library';
            }

            my $mapset = QCMG::QBamMaker::Mapset->new(
                              %{ $rec },
                              verbose => $self->verbose );
            $self->add_mapset( $mapset );
        }
    }

    qlogprint( 'Found ', $self->mapset_count, " mapsets in Geneus\n" )
        if $self->verbose;
    return $self->mapset_count;
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub grouping_attributes {
    my $self = shift;
    return $self->{grouping_attributes} = shift if @_;
    return $self->{grouping_attributes};
}


sub cmdline {
    my $self = shift;
    return $self->{cmdline} = shift if @_;
    return $self->{cmdline};
}


sub mapsets {
    my $self = shift;
    return @{ $self->{mapsets} };
}

sub mapset_count {
    my $self = shift;
    return scalar( $self->mapsets );
}


sub add_mapset {
    my $self   = shift;
    my $mapset = shift;
    die 'item to be added must be an object of type QCMG::QBamMaker::Mapset'.
        ' not '. ref($mapset). "\n"
        unless ref($mapset) eq 'QCMG::QBamMaker::Mapset';
    push @{ $self->{mapsets} }, $mapset;
}

# Apply a constraint (regex) to the mapsets and only return those that match

sub apply_constraint {
    my $self    = shift;
    my $field   = shift;
    my $pattern = shift;

    # If we have no mapsets then we can warn and exit imediately
    my @mapsets = $self->mapsets;
    if (! scalar(@mapsets)) {
        qlogprint "No mapsets left so did not apply constraint [$pattern] on field [$field]\n"
            if $self->verbose;
        return;
    }

    # We assume that the mapsets are homogenous in terms of their hash keys
    # so we use the first as exemplar for presence/absence of fields.

    die "Field [$field] does not exist in mapsets so could not apply constraint\n"
        unless (defined $mapsets[0]->attribute($field));

    my @passed_recs = ();
    foreach my $rec ( @mapsets ) {
        # 'NULL' is a special case
        if ($pattern =~ /^NULL$/i) {
            next unless (! defined $rec->attribute( $field ) or
                                   $rec->attribute( $field ) eq '');
        }
        else { 
            next unless (defined $rec->attribute( $field ) and
                                 $rec->attribute( $field ) =~ /$pattern/i);
        }
        push @passed_recs, $rec;
    }

    # Save the mapsets that passed
    $self->{mapsets} = \@passed_recs;

    # Save a record of the constraint being applied
    push @{ $self->{applied_constraints} },
         [ $field, $pattern, $self->mapset_count ];

    qlogprint( $self->mapset_count,
               " mapsets passed constraint [$pattern] on field [$field]\n" )
        if $self->verbose;
}


sub set_constraint_order {
    my $self     = shift;
    my $conorder = shift;

    # Check that all of the available constraints have been specified
    # and each specified only once.

    my @cons = split(//,$conorder);

    die "-z string [$conorder] did not contain all constraints once\n" unless
        ( join('',sort keys %CONSTRAINTS) eq join('',sort @cons) );
    my @ordered_constraints = ();
    foreach my $con (@cons) {
        push @ordered_constraints, $CONSTRAINTS{$con};
    }
    
    $self->{constraint_order} = $conorder;
    $self->{ordered_constraints} = \@ordered_constraints;
}


sub get_constraint_order {
    my $self = shift;
    return @{ $self->{ordered_constraints} };
}


sub mapsets_to_string {
    my $self = shift;

    # Call private method specifying ALL mapsets
    my @mapsets = $self->mapsets;
    return $self->_selected_mapsets_to_string( \@mapsets );
}


sub _selected_mapsets_to_string {
    my $self       = shift;
    my $ra_mapsets = shift;

    # We need to get the full strings to use as headers
    my @headers = map { $CONSTRAINTS{$_} }
                  split( //, $self->{constraint_order} );

    my $string = '';
    $string .= join(',',@headers)."\n";
    no warnings;
    foreach my $rec ( @{ $ra_mapsets } ) {
        my @values = map { $rec->{attributes}->{$_} } @headers;
        $string .= join(',',@values)."\n";
    }
    use warnings;

    return $string;
}


# This is a little tricky.  We want to use a set of constraints to sort
# the mapsets into groups but we don't know ahead of time what the
# constraints might be so we'll loop though them getting deeper and
# deeper into the hash.  Once we are at our end point, we treat the
# final reference as an array and push on the current mapset.  Rinse
# and repeat.

sub mapsets_as_hash {
    my $self    = shift;
    my $ra_keys = shift; # list of fields to be used to classify BAMs

    my @keys = @{ $ra_keys };
    my $bighash = {};
    foreach my $rec ( $self->mapsets ) {
        my $pos_in_hash = $bighash;
        foreach my $key (@keys) {

            # Mapset is a special case - it is globally unique and the
            # point of this whole routine is to group Mapsets by their
            # other attributes so if we see mapset, we just skip over it
            # when building the hash.  There may be other attributes
            # that need to be treated this way also (e.g. primary_library).
            next if ($key eq 'mapset' or
                     $key eq 'primary_library');

            my $value = (exists $rec->{$key} and defined $rec->{$key}) ?
                        $rec->{$key} : 'NULL';

            # If we are at the last key then we need to initialise an
            # array, otherwise we need a hash so we can continue to move
            # down the tree of values
            if (not exists $pos_in_hash->{ $value }) {
                if ($key eq $keys[$#keys]) {
                    $pos_in_hash->{ $value } = [];
                }
                else {
                    $pos_in_hash->{ $value } = {};
                }
            }
            $pos_in_hash = $pos_in_hash->{ $value };
        }
        push @{ $pos_in_hash }, $rec;
    }

    return $bighash;
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
            # that need to be treated this way also (e.g. primary_library).

            next if ($key eq 'mapset' or
                     $key eq 'primary_library');

            my $value = (exists $rec->{$key} and defined $rec->{$key}) ?
                        $rec->{$key} : 'NULL';

            push @attributes, [ $key, $value ];
        }

        # Work out if we have seen this group before.  If so add the
        # mapset, otherwise create a new MapsetCollection

        my $group_name = _string_from_attributes( \@attributes );
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


sub _string_from_attributes {
    my $ra_attributes = shift;

    my @values = ();

    foreach my $ra_pair (@{ $ra_attributes }) {
        my $new_field = $ra_pair->[0];
        my $new_value = $ra_pair->[1];
        $new_value =~ s/[^a-zA-Z0-9]/ /g;      # ditch any non-alphanum chars
        $new_value =~ s/(\b)([a-z])/$1\u$2/g;  # uc() first letters of strings
        $new_value =~ s/\s//g;                 # ditch any remaining spaces
        # If we end up with an empty string, we will use 'NULL'
        $new_value = $new_value ? $new_value : 'NULL';
        push @values, $new_value;
    }

    return join('_',@values);
}


sub _recursive_stringify {
    my $data_in  = shift;
    my $string   = shift;
    my $data_out = shift;

    if (ref($data_in) eq 'HASH') {
        foreach my $key (sort keys %{ $data_in }) {
            my $new_key = $key;
            $new_key =~ s/[^a-zA-Z0-9]/ /g;      # ditch any non-alphanum chars
            $new_key =~ s/(\b)([a-z])/$1\u$2/g;  # uc() first letters of strings
            $new_key =~ s/\s//g;                 # ditch any remaining spaces
            #print ref($data_in)." $key $new_key -> $string\n";

            # Build new string, begin careful to cope with empty case
            my $new_string = $string ? $string.'_'.$new_key : $new_key;

            _recursive_stringify( $data_in->{$key}, $new_string, $data_out);
        }
    }
    elsif (ref($data_in) eq 'ARRAY') {
        $data_out->{ $string } = $data_in;
    }
}


sub mapsets_as_groups_2 {
    my $self    = shift;
    my $ra_keys = shift; # list of fields to be used to group BAMs

    my $rh_mapsets       = $self->mapsets_as_hash( $ra_keys );
    my $ra_attributes    = [];
    my $ra_mapset_groups = [];

    _recursively_build_groups( $rh_mapsets, $ra_attributes, $ra_mapset_groups );

    return $ra_mapset_groups;
}


sub uptodate {
    my $self    = shift;
    my $bamfile = shift;

    # Need to compare mapsets from our target set to those listed in the
    # BAM header.
    # Return values:
    # 0 - BAM does not exist
    # 1 - BAM exists and is uptodate
    # 2 - BAM exists and is not uptodate
    # 3 - BAM is locked for creation
    # 4 - more than one BAM exists

    # Unfortunately the BAM file might have been made by a different
    # user so the names may not match exactly.  So we are going to have
    # to do some pattern matching.  Sigh.

    # Pull $bamfile into a path and a filename
    my $path = '';
    my $file = '';
    if ($bamfile =~ /(.*)\/([^\/]+)$/) {
        $path = $1;
        $file = $2;
    }

    # Now we transform the filename to replace the creator name with a
    # '*' and make sure the .bam is the end of the filename (add a '$')
    # We will also make a .bam.qbmlock$ equivalent.
    my $mybam = $file;
    my $mybamlock = $file;
    $mybam     =~ s/\.[^\.]+\.bam/.*.bam\$/;
    $mybamlock =~ s/\.[^\.]+\.bam/.*.bam.qbmlock\$/;

    # Now we see if we got matches for bam or lock files
    my $find  = QCMG::FileDir::Finder->new;
    my @bams  = $find->find_file( $path, $mybam );
    my @locks = $find->find_file( $path, $mybamlock );

    # BAM is locked for creation
    #return 3 if (-r $bamfile.'.qbmlock');
    return 3 if scalar(@locks);

    # BAM file does not exist
    #return 0 unless (-r $bamfile);
    return 0 unless scalar(@bams);

    # More than one BAM matched pattern
    return 4 if (scalar(@bams) > 1);

    # If we got this far then we use whatever bam we found
    $bamfile = $bams[0];

    my $bam = QCMG::QBamMaker::SeqFinalBam->new( filename => $bamfile,
                                                 verbose  => $self->verbose );
    my %qcmg_mapsets = map { $_ => 1 }
                       keys %{ $bam->mapsets };
    my @target_mapsets = map { $_->attribute('mapset') }
                         $self->mapsets;

    #print Dumper $bam, \%qcmg_mapsets, \@target_mapsets;

    my $missing_count = 0;
    foreach my $mapset ( @target_mapsets ) {
        if (! exists $qcmg_mapsets{ $mapset }) {
            warn 'mapset missing from final BAM: ', $bam->project,
                 " - $mapset - $bamfile\n";
            $missing_count++;
        }
    }
    # BAM exists and is not uptodate
    return 2 if $missing_count;

    # BAM exists and is uptodate
    return 1;
}

sub _recursively_build_groups {
    my $data_in  = shift;
    my $string   = shift;
    my $data_out = shift;

    if (ref($data_in) eq 'HASH') {
        foreach my $key (sort keys %{ $data_in }) {
            my $new_key = $key;
            $new_key =~ s/[^a-zA-Z0-9]/ /g;      # ditch any non-alphanum chars
            $new_key =~ s/(\b)([a-z])/$1\u$2/g;  # uc() first letters of strings
            $new_key =~ s/\s//g;                 # ditch any remaining spaces
            #print ref($data_in)." $key $new_key -> $string\n";

            # Build new string, begin careful to cope with empty case
            my $new_string = $string ? $string.'_'.$new_key : $new_key;

            _recursive_stringify( $data_in->{$key}, $new_string, $data_out);
        }
    }
    elsif (ref($data_in) eq 'ARRAY') {
        $data_out->{ $string } = $data_in;
    }
}


sub write_pbs_script {
    my $self   = shift;
    my %params = @_;

    my $final_bam_pathname  = $params{'final_bam_pathname'};
    my $final_bam_name_stem = $params{'final_bam_name_stem'};
    my $final_outdir        = $params{'bam_dir'};
    my $pbs_script_pathname = $params{'pbs_script_pathname'};
    my $final_bam_lockfile  = $final_bam_pathname . '.qbmlock';

    qlogprint "processing for $pbs_script_pathname\n" if $self->verbose;

    # We will go through the mapsets and look for possible but unlikely
    # events, e.g. more than one donor, sample, aligner, sample code,
    # material etc.  There are scenarios where all of these unlikely
    # events are desired by the user so we will print a warning for each
    # but continue processing - no die's.

    my %tallys = ();
    my @checks = ('parent_roject','project','sample','sample_code','material','capture_kit');
    foreach my $mapset ( $self->mapsets ) {
        foreach my $check (@checks) {
            next unless $mapset->attribute($check);
            $tallys{ $check }->{ $mapset->attribute($check) }++;
        }
    }
    foreach my $check (@checks) {
        next unless exists $tallys{$check};
        my $count = scalar keys %{ $tallys{ $check } };
        warn "Multiple [$count] $check\'s found in mapset collection: ",
            $final_bam_pathname, ".bam\n"
            if ($count > 1);
    }

    # If any of the mapsets are not on disk then we are wasting our time
    # so we should do that check now.  We don't want to call a die
    # because the program could be processing multiple MapsetCollection
    # objects so we'll warn and return immediately

    foreach my $mapset ( $self->mapsets ) {
        if (! -r $mapset->attribute('_pathName')) {
            warn 'mapset not found on disk: ',
                 $mapset->attribute('project'), ' - ',
                 $mapset->attribute('mapset'), ' - ',
                 $final_bam_name_stem, ".bam\n";
            return undef;
        }
    }

    # If the final BAM already exists then we want to exit with an
    # explanation because we never write over existing BAMs.  We also want
    # to know whether the BAM is uptodate so we have to check to see
    # whether the mapsets it contains matches the list of mapsets that
    # we would write into the final BAM.  If the lists do not match then
    # the BAM is out of date and we need to print a warning for the user
    # so they can delete the obsolete BAM.

    my $status = $self->uptodate( $final_bam_pathname );
    if ($FORCE_PBS or ($status == 0)) {
        # Fall straight through, i.e. always create PBS file
    }
    elsif ($status == 1) {
        qlogprint 'BAM already exists so not creating PBS job for: ',
            $final_bam_name_stem, ".bam\n" if $self->verbose;
        return undef;
    }
    elsif ($status == 2) {
        # This should probably be a WARN not an INFO
        warn 'BAM is not uptodate: ', $final_bam_name_stem, ".bam\n";
        return undef;
    }
    elsif ($status == 3) {
        qlogprint 'BAM is locked for qbammaker: ', $final_bam_name_stem,
            ".bam\n" if $self->verbose;
        return undef;
    }
    elsif ($status == 4) {
        qlogprint 'more than one BAM matches (ignoring user name): ', $final_bam_name_stem,
            ".bam\n" if $self->verbose;
        return undef;
    }
    else {
        die 'should not be possible to get here - '.
            "status $status on $final_bam_pathname\n";
    }

    # If we got this far then we are going to create a PBS file
    qlogprint "creating PBS file $pbs_script_pathname\n" if $self->verbose;

    # No matter what else happens, we need to merge at the library level
    # so we can mark duplicates.  Sort mapsets into groups by library.
    my %mapset_sets = ();
    foreach my $mapset ( $self->mapsets ) {
        push @{ $mapset_sets{ $mapset->attribute('primary_library') } }, $mapset;
    }
    my @mapset_sets = values %mapset_sets;

    # Note use of single quotes - 'EOHEADER' - which prevent
    # interpolation within the heredoc otherwise the ${PBS_*} strings
    # cause perl to bitch
    my $script = <<'EOHEADER';
#!/bin/bash

##########################################################################
#
#  Generator: qBamMaker.pl
#  Creator:   ~~USER~~
#  Created:   ~~DATETIME~~
#
#  This is a PBS submission job for merging and deduping BAM files
#
#  Cmdline: ~~CMDLINE~~
#
##########################################################################
#
#PBS -N ~~PBSJOBNAME~~
#PBS -S /bin/bash
#PBS -q qbammaker
#PBS -r n
#PBS -l walltime=80:00:00,ncpus=1,mem=20gb
#PBS -m ae
#PBS -M ~~EMAIL~~

# Cause shell to exit immediately if any command exits with non-zero status
set -e

module load samtools
module load java/1.7.13
module load picard/1.110
module load adama/nightly

MARK_DUPS="java -Xmx20g -jar $PICARD_HOME/MarkDuplicates.jar"
QBAMMERGE="java -Dsamjdk.compression_level=1 -jar $ADAMA_HOME/lib/qbammerge-0.6pre.jar"

## Identify the job-specific local scratch dir on the minion
SCRATCH_DIR=/scratch/${PBS_JOBID}

EOHEADER

    # For safety, we never want to write over an existing final BAM so
    # we need to start the script with a sanity check that will exit
    # immediately if the final bam already exists.
    $script .= "#\n# Exit if BAM with final name already exists or is locked\n#\n" .
               "if [ -r $final_bam_pathname ] ; then\n" .
               "    echo FATAL : BAM already exists with name $final_bam_pathname\n" .
               "    exit 123\n" .
               "fi\n" .
               "if [ -r $final_bam_lockfile ] ; then\n" .
               "    echo FATAL : BAM is locked $final_bam_lockfile\n" .
               "    exit 124\n" .
               "fi\n\n";


    # For ease of tracking down the matching PBS .o and .e files, push the
    # PBS JOBID and JOBNAME into the lockfile.
    $script .= "# Create lockfile\n" .
               "touch $final_bam_lockfile\n" .
               "echo PBS_JOBID: \$PBS_JOBID >> $final_bam_lockfile\n" .
               "echo PBS_JOBNAME: \$PBS_JOBNAME >> $final_bam_lockfile\n\n";

    my @constraints = map{ '# ' . join(', ', @{ $_ }) .' passed' }
                      @{ $self->{applied_constraints} };
    if (@constraints) {
        $script .= "#\n# Constraints applied to select mapsets:\n#\n" .
                   join( "\n", @constraints) . "\n#\n\n";
    }

    # A mapset set by definition should be for a single library so we
    # must merge if there is more than one mapset, and dedup regardless
    # of number of mapsets.  This work should all be done on the node in
    # a directory with a nam based with the PBS jobid for trackability.
    # Once we have deduped library-level BAMs, they all need to be 
    # merged and the final BAM output to the --outfile BAM name.

    my @dedup_lib_bams = ();
    foreach my $ctr (0..$#mapset_sets) {
        my $mapset_set  = $mapset_sets[$ctr];

        # Assuming a single library, we can set the libname now
        my $libname = $mapset_set->[0]->attribute('primary_library');
        $libname =~ s/\s//g;          # strip out spaces
        $libname =~ s/[^\w_\,]+/_/g;  # strip out troublesome chars
        my @mapsets = @{ $mapset_set };

        # Prepare comment block that lists details of all the mapsets
        # that will be included in this BAM
        my $mapset_list = '# '. $self->_selected_mapsets_to_string( \@mapsets );
        $mapset_list =~ s/\n/\n# /g;

        $script .= "\n#\n# Commands for library $libname:\n#\n" .
                   $mapset_list . "\n";

        my $dest_bam = '${SCRATCH_DIR}/'. $final_bam_name_stem .'.'. $libname;

        # We always do a merge here, even if there is no merging
        # required.  We do this because if we use a cp it breaks our
        # ability to track back through the qbammerge steps to see the
        # names of all of the source mapsets that are in the final BAM.
        # So using cp means the original mapset name is lost.

        $script .=
            '$QBAMMERGE --output ' . $dest_bam .".bam \\\n".
            '          --log    ' . $dest_bam .".bam.qmrg.log \\\n";
        my @inputs = ();
        foreach my $mapset (@mapsets) {
            push @inputs, 
                 '          --input  '. $mapset->attribute('_pathName');
        }
        $script .= join(" \\\n",@inputs)."\n";

        # RNA MarkDuplicates.
        # After discussion with NW, we decided that RNA library BAMs
        # shoudl be deduped.  The seq_final BAMs are primarily used for
        # qVerify so the most conservative approach is to discard dups
        # when verifying so that PCR dups don't drive verification.

        # Rename merged BAM so that the dedup can recreate a BAM that
        # matches $dest_bam.  This is important because otherwise the
        # "chain" of qbammerge is broken because the first round of
        # merges create $dest_bam but the final round of merging is on
        # BAMs that now include some string ('.dedup') to indicate that
        # they have been deduped.  Instead we need to rename the
        # library BAM to a temp name so the dedup can create BAMs with
        # the original library name ($dest_bam).
        my $withdups_bam = $dest_bam . '.withdups';
        $script .= "mv $dest_bam.bam $withdups_bam.bam\n";

        # Index new library BAM
        $script .= 'samtools index ' . $withdups_bam .".bam\n";

        # Mark duplicates - always run on the library-level BAM
        # 2013-07-10.  On advice from CL the optical_dup distance
        # was changed from 10 to 100 and the regex was added so that
        # reads could be correctly picked up byMarkDups.

        $script .=
        '$MARK_DUPS OPTICAL_DUPLICATE_PIXEL_DISTANCE=100 '."\\\n".
        '  READ_NAME_REGEX=\'^[a-zA-Z0-9\-]+:[0-9]+:[a-zA-Z0-9]+:[0-9]:([0-9]+):([0-9]+):([0-9]+)\' ' ."\\\n".
        '  VALIDATION_STRINGENCY=SILENT ' ."\\\n".
	'  COMPRESSION_LEVEL=1 ' ."\\\n".
        '  INPUT=' . $withdups_bam .".bam \\\n".
        '  OUTPUT=' . $dest_bam .".bam \\\n".
        '  METRICS_FILE=' .$dest_bam .".bam.dedup_metrics\n";

        # We need to remember the deduped (or not) BAM names so we 
        # can include them in the final mega-merge
        push @dedup_lib_bams, $dest_bam .'.bam';

	$script .= "#\n#list internal BAM files size here\n";
	$script .= "ls -alh " . $withdups_bam  . ".bam \n";
	$script .= "ls -alh " . $dest_bam . ".bam \n";

    }

    # Copy back logs, metrics files etc
    $script .= "#\n# Copy log and dedup_metrics files to final directory\n#\n" .
               'cp ${SCRATCH_DIR}/*.log '. $final_outdir ."\n".
               'cp ${SCRATCH_DIR}/*.dedup_metrics '. $final_outdir ."\n\n";

    # Regardless of how many libraries there are, we always do a final
    # qbammerge back to outfile so we can add our custom @CO comment
    # lines with the attributes and the commandline.

    $script .= "#\n# And now the mega-merge ...\n#\n";
    $script .= 'qbammerge --output '. $final_bam_pathname ." \\\n".
               '          --log    '. $final_bam_pathname .".log \\\n".
               "          --co     \'CN:QCMG\tQN:qbammaker\tCL:~~CMDLINE~~\'\\\n";

    # Create an identifier for the final BAM
    my $uuid = create_UUID_as_string(UUID_V4);
    my $now  = time();
    my $id = "uuid=$uuid\tepoch=$now\tbamname=$final_bam_name_stem.bam";
    $script .= "          --co     \'CN:QCMG\tQN:qbamid\t$id\'\\\n";

    # Use version '1.0' of the new-to-old naming hash so we can map 
    # from parent_project to Project, etc
    my %name_map = %{ QCMG::QBamMaker::SeqFinalBam::qlimsmeta_new2old_hash('1.0') };

    # Add mappings that are not part of the standard set
    $name_map{ 'mapset' } = 'Mapset';

    # Add metadata for the grouping attributes
    if (defined $self->grouping_attributes) {
        my @formatted_attribs = ();
        foreach my $attrib (sort keys %{ $self->grouping_attributes }) {
            my $value = $self->grouping_attributes->{ $attrib };
            $value = '' unless defined $value;
            push @formatted_attribs, $name_map{$attrib} .'='. $value;
        }
        my $attrib_string = join("\t",@formatted_attribs);
        $script .= "          --co     \'CN:QCMG\tQN:qlimsmeta\t$attrib_string\'\\\n";
    }

    # Add metadata for each mapset in this Collection
    foreach my $mapset ($self->mapsets) {
        my @formatted_attribs = ();
        foreach my $attrib (sort ('mapset', keys %{ $self->grouping_attributes })) {
            my $value = $mapset->attribute( $attrib );
            $value = '' unless defined $value;
            # Don't forget to map capture_kit bask to 'Capture Kit', etc
            push @formatted_attribs, $name_map{$attrib} .'='. $value;
        }
        my $attrib_string = join("\t",@formatted_attribs);
        $script .= "          --co     \'CN:QCMG\tQN:qmapset\t$attrib_string\' \\\n";
    }

    # Add the BAMs output by the library-level merges
    my @inputs = ();
    foreach my $bamname (@dedup_lib_bams) {
            push @inputs, "          --input  $bamname";
    }
    $script .= join(" \\\n",@inputs)."\n\n";


    # Do final substitutions
    my $user     = $ENV{USER};
    my $email    = $ENV{QCMG_EMAIL};
    my $datetime = localtime() .'';
    my $pbsjob   = $params{'pbs_job_name'} ?
                   $params{'pbs_job_name'} : 'qBamMaker';
    $script =~ s/~~USER~~/$user/smg;
    $script =~ s/~~EMAIL~~/$email/smg;
    $script =~ s/~~DATETIME~~/$datetime/smg;
    $script =~ s/~~PBSJOBNAME~~/$pbsjob/smg;

    my $cmdline = defined $self->cmdline ? $self->cmdline : 'No_cmdline_specified';
    $script =~ s/~~CMDLINE~~/$cmdline/smg;

    # Add final cleanup command for node
    $script .= "#\n# Cleanup after ourselves\n#\n" .
               "rm -f $final_bam_lockfile\n";
               #'rm -rf $SCRATCH_DIR'."\n";

    # Write out script file
    my $outfh = IO::File->new( $pbs_script_pathname, 'w' );
    die "Unable to open file ",$pbs_script_pathname," for writing: $!" 
        unless defined $outfh;
    $outfh->print( $script );
    $outfh->close;
}


#sub bam_name_from_mapset {
#    my $rh_mapset = shift;
#    
#    my $bam_name = '/mnt/seq_results/' .
#                   $rh_mapset->attribute('parent_project') . '/' .
#                   $rh_mapset->attribute('project') . '/seq_mapped/' .
#                   $rh_mapset->attribute('mapset') . '.bam';
#    return $bam_name;
#}


# Not sure if this would be useful here or not?
#sub write_bamlist_file {
#    my $self = shift;
#    my $file = shift;
#
#    my @fields = @QCMG::QBamMaker::SeqFinalBamProperties::QLIMSMETA;
#    unshift @fields, 'FromQLimsMeta';
#    my @headers = map { my $xx = $_; $xx =~ s/\s//g; $xx }  @fields;
#
#    my $outfh = IO::File->new( $file, 'w' ) or
#                die "Can't open file [$file] for writing :$!\n";
#    print $outfh join("\t",'ID',@headers),"\n";
#
#    my $ctr = 1;
#    foreach my $rec ($self->mapsets) {
#        my @outputs = map { (exists $rec->{$_}) ? $rec->{$_} : '' }
#                      @fields;
#        print $outfh join("\t",$ctr++,@outputs),"\n";
#    }
#
#    $outfh->close;
#
#}


1;
__END__


=head1 NAME

QCMG::QBamMaker::MapsetCollection - This module does stuff


=head1 SYNOPSIS

 use QCMG::QBamMaker::MapsetCollection;

 my $obj = QCMG::QBamMaker::MapsetCollection->new( filename => $infile );


=head1 DESCRIPTION

This module provides ...


=head1 PUBLIC METHODS

=over 2

=item B<new()>

 my $obj = QCMG::QBamMaker::MapsetCollection->new(
                                 filename => 'myfile.txt',
                                 verbose  => 1 );

The B<new()> method takes 1 mandatory and 2 optional parameters.  The
mandatory param is I<filename> which is ...
and the optional params are I<verbose> which default to 0 and
indicates the level of verbosity in reporting, and ...

=item B<filename()>

 $obj->filename( 'hohoho.bam' );

Accessor for filename.  No point using the setter because the only
way to trigger processing of a new file is via B<new()>.

item B<initialise_from_lims>

item B<grouping_attributes>

=item B<cmdline()>

=item B<mapsets()>

=item B<mapset_count()>

=item B<add_mapset()>

=item B<apply_constraint()>

=item B<set_constraint_order()>

=item B<get_constraint_order()>

=item B<mapsets_to_string()>

=item B<_selected_mapsets_to_string()>

=item B<mapsets_as_hash()>

=item B<mapsets_grouped_by_mapset_fields()>

=item B<mapsets_as_groups_2()>

=item B<uptodate()>

This one is tricky - it takes a BAM name and then it checks whether a
file with that name exists BUT it ignores the part of the bamname that
is set to the creator's name, i.e. if stuff.production.bam is supplied and
it can find stuff.jpearson.bam then it would say that the BAM exists.
This is what we want because if the only difference is the creator name
then the BAMs are equivalent.  If then checks the 
up-to-date-ness of whatever file is has found, not the file that was
supplied.  Tricky but this is what we want it to do.

=item B<write_pbs_script()>

=item B<verbose()>

 $bam->verbose( 2 );
 my $verb = $bam->verbose;

Accessor for verbosity level of progress reporting.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: MapsetCollection.pm 4665 2014-07-24 08:54:04Z j.pearson $


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
