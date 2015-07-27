package QCMG::SeqResults::Mapsets;

###########################################################################
#
#  Module:   QCMG::SeqResults::Mapsets.pm
#  Creator:  John V Pearson
#  Created:  2011-06-29
#
#  Data container for mapset info retrieved from TrackLite.  Each mapset
#  is represented as an array with the following 10 elements:
#
#  0  run_id
#  1  mapset_name
#  2  sample_set
#  3  project_id
#  4  library_type
#  5  pooled_library_id
#  6  primary_library_id
#  7  run_type
#  8  input_type
#  9  experiment_type
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use File::Find;
use Getopt::Long;
use Pod::Usage;
use Data::Dumper;

use QCMG::DB::TrackLite;
use QCMG::SeqResults::Util qw( qmail is_valid_mapset_name
                               bams_that_were_merged_into_this_bam
                               seqmapped_bams seqlib_bams );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { };

    # Set values from input %params or from defaults.
    my %defaults = ( panfs     => '/mnt/seq_results', #$ENV{'QCMG_PANFS'} . '/seq_results',
                     verbose   => 0 );

    # This block will set values from parameters, then from defaults.
    # Note that only keys from %defaults are
    # even checked so this is how we control what can be specified in
    # the config file - if it's not in %defaults, it can't be set!

    foreach my $key (keys %defaults) {
        if (exists $params{$key}) {
            $self->{ $key } = $params{ $key };
        }
        else {
            $self->{ $key } = $defaults{ $key };
        }
    }

    # FYI
    $self->{_params}   = \%params;
    $self->{_defaults} = \%defaults;
    bless $self, $class;

    $self->_query_lims_for_mapsets;
    $self->_mapset_integrity_checks;
    return $self;
}


sub panfs {
    my $self = shift;
    return $self->{panfs} = shift if @_;
    return $self->{panfs};
}


sub donors {
    my $self = shift;
    return keys %{ $self->{donor_index} };
}


sub libraries {
    my $self = shift;
    return keys %{ $self->{library_index} };
}


sub libraries_for_donor {
    my $self  = shift;
    my $donor = shift;

    return undef unless (exists $self->{donor_index}->{$donor});

    my @runids = @{ $self->{donor_index}->{$donor} };
    my @tmplibs = map { $self->{records_by_runid}->{$_}->[6] } @runids;

    # We need to make these unique so we'll push them through a hash
    my %tmplibs = ();
    foreach my $lib (@tmplibs) {
        $tmplibs{ $lib }++;
    }
    my @libraries = sort keys %tmplibs;
    
    return @libraries;
}


sub libraries_for_donor_no_mirna {
    my $self  = shift;
    my $donor = shift;

    return undef unless (exists $self->{donor_index}->{$donor});

    my @runids = @{ $self->{donor_index}->{$donor} };

    my @tmplibs = ();
    foreach my $runid (@runids) {
        next if $self->{records_by_runid}->{$runid}->[9] =~ /miRNA/i;
        push @tmplibs, $self->{records_by_runid}->{$runid}->[6];
    }

    # We need to make these unique so we'll push them through a hash
    my %tmplibs = ();
    foreach my $lib (@tmplibs) {
        $tmplibs{ $lib }++;
    }
    my @libraries = sort keys %tmplibs;
    
    return @libraries;
}


sub runids_for_library {
    my $self    = shift;
    my $library = shift;

    return undef unless (exists $self->{library_index}->{$library});

    my @runids = @{ $self->{library_index}->{$library} };
    return \@runids;
}
    

sub seqmapped_bams_for_library {
    my $self    = shift;
    my $library = shift;

    my @bams = map { $self->_seqmapped_bam_name( $_ ) }
                  @{ $self->runids_for_library( $library ) };
    return @bams;
}
 

sub seqlib_bam_for_library {
    my $self    = shift;
    my $library = shift;

    # A library can only exist if there are BAM records so take
    # the first seq_mapped BAM and predict the library BAM name
    # based on it.

    my $ra_runids = $self->runids_for_library( $library );
    return undef unless defined $ra_runids;

    my $record    = $self->record_from_runid( $ra_runids->[0] );
    my $pathname  = $self->_rootpath_for_donor( $record );

    # Add seq_lib, library name and .bam extension
    $pathname .= '/seq_lib/' . $record->[6] . '.bam';

    return $pathname;
}
    

sub record_from_runid {
    my $self  = shift;
    my $runid = shift;

    return undef unless (exists $self->{records_by_runid}->{$runid});

    # For safety, we'll clone the record
    my @clone = @{ $self->{records_by_runid}->{$runid} };
    return \@clone;
}


sub mapsets_report {
    my $self = shift;

    # Create list of mapsets sorted by runid
    my @runids = sort { $a <=> $b } keys %{ $self->{records_by_runid} };
    my $text = 'ICGC Mapsets in LIMS ('. scalar(@runids). ") :\n\n";
    foreach my $runid (@runids) {
        $text .= join("\t", @{ $self->record_from_runid($runid) } ). "\n";
    }
    return $text;
}


sub _rootpath_for_donor {
    my $self   = shift;
    my $record = shift;

    die "FATAL: must supply record hash" unless defined $record;

    my $pathname = $self->{panfs};

    # Add project subdir
    if ($record->[3] =~ /ICGC-Pancreas/) {
        $pathname .= '/icgc_pancreatic/';
    }
    elsif ($record->[3] =~ /ICGC-Ovarian/) {
        $pathname .= '/icgc_ovarian/';
    }
    else {
        die 'FATAL: Unrecognised project: [', $record->[3] , ']';
    }

    # Add donor subdir
    $pathname .= $record->[2];

    return $pathname;
}


sub _seqmapped_bam_name {
    my $self  = shift;
    my $runid = shift;

    my $record   = $self->record_from_runid( $runid );
    my $pathname = $self->_rootpath_for_donor( $record );

    # Add seq_mapped, mapset name and .bam extension
    $pathname .= '/seq_mapped/' . $record->[1] . '.bam';

    return $pathname;
}


sub _query_lims_for_mapsets {
    my $self = shift;

    my $lims = QCMG::DB::TrackLite->new();
    $lims->connect();
    # We're only going to target ICGC libraries for now
    my $ra_rows = $lims->libraries_icgc;

    # Each row returned must have values for these fields
    my %required_fields = ( 1 => 'mapset_name',
                            2 => 'sample_set',
                            3 => 'project_id',
                            6 => 'primary_library_id',
                            7 => 'run_type',
                            8 => 'input_type',
                            9 => 'experiment_type' );

    # Put each row into a hash to enable easy lookup;
    my $ctr = 0;
    my %records_by_runid  = ();
    my %records_by_mapset = ();
    my %library_index     = ();
    my %donor_index       = ();

    foreach my $row (@{ $ra_rows }) {

        # Check that this row has all of the information required
        my @missing = ();
        foreach my $key (sort keys %required_fields) {
            push( @missing, $required_fields{$key} ) 
                unless defined $row->[$key];
        }

        # Report (and skip) incomplete rows
        if (scalar(@missing) > 0) {
            no warnings;
            warn 'Record ', $row->[0],
                  ' has the following required fields undefined: ',
                  join(',',@missing), "\n    ", join(',',@{$row}), "\n";
            next;   # skip further processing on this row
        }

        # Save data by run_id and mapset both of which should be unique
        $records_by_runid{ $row->[0] } = $row;
        $records_by_mapset{ $row->[1] } = $row;

        # Convert hyphens and periods to underscores in donor ID
        $row->[2] =~ s/\-/_/g;
        $row->[2] =~ s/\./_/g;
        push @{ $donor_index{ $row->[2] } }, $row->[0];

        # Convert hyphens to underscores in primary library name
        $row->[6] =~ s/\-/_/g;
        push @{ $library_index{ $row->[6] } }, $row->[0];
    }

    $self->{ records_by_runid }  = \%records_by_runid;
    $self->{ records_by_mapset } = \%records_by_mapset;
    $self->{ donor_index }       = \%donor_index;
    $self->{ library_index }     = \%library_index;
}


sub _mapset_integrity_checks {
    my $self = shift;

    # Check by library - do all mapsets have the same donor,
    # input_type and experiment_type
    my @libs = sort $self->libraries;
    foreach my $lib (@libs) {
        my @runids = @{ $self->runids_for_library( $lib ) };
        my $first_rec = '';
        foreach my $runid (@runids) {
            my $record = $self->record_from_runid( $runid );
            # Initialise the comparison values
            $first_rec = $record if (! $first_rec);

            if ($first_rec->[2] ne $record->[2] or
                $first_rec->[8] ne $record->[8] or
                $first_rec->[9] ne $record->[9]) {
                warn "WARNING: library $lib has critical mismatches:\n",
                      '  ', join("\t", @{$first_rec}), "\n",
                      '  ', join("\t", @{$record}), "\n";
            }
        }
    }
}

1;

=head1 COPYRIGHT

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
