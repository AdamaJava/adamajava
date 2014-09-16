package QCMG::SeqResults::ActionLibraryMerge;

###########################################################################
#
#  Module:   QCMG::SeqResults::ActionLibraryMerge.pm
#  Creator:  John V Pearson
#  Created:  2011-06-02
#
#  Logic for command librarymerge that populates seq_lib/ for each donor
#  with library-level merged BAM files.
#
#  THIS MODULE IS INCOMPLETE!
#
#  $Id: ActionLibraryMerge.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use File::Find;
use Getopt::Long;
use Pod::Usage;
use Data::Dumper;
use XML::LibXML;

use QCMG::DB::TrackLite;
use QCMG::FileDir::Finder;
use QCMG::SeqResults::Mapsets;
use QCMG::SeqResults::Pbs;
use QCMG::SeqResults::Util qw( qmail is_valid_mapset_name
                               bams_that_were_merged_into_this_bam
                               seqmapped_bams seqlib_bams );
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: ActionLibraryMerge.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;

    # Defaults (if any) for commandline options
    my %opts       = ();
    #$opts{dir}     = $ENV{'QCMG_PANFS'} . '/seq_results';
    $opts{dir}     = '/mnt/seq_results';
    $opts{tmpdir}  = $ENV{'QCMG_HOME'} . '/tmp';
    $opts{email}   = [ ];
    $opts{outfile} = '';
    $opts{verbose} = 0;
    $opts{help}    = 0;

    # Use GetOptions module to parse commandline options
    my $results = GetOptions (
           'd|dir=s'         => \$opts{dir},           # -d
           'e|email=s'       =>  $opts{email},         # -e
           'o|outfile=s'     => \$opts{outfile},       # -o
           't|tmpdir=s'      => \$opts{tmpdir},        # -t
           'v|verbose+'      => \$opts{verbose},       # -v
           'h|help|?'        => \$opts{help},          # -h
           );

    # If no email recipient supplied, add invoker as default
    push @{$opts{email}}, $ENV{'QCMG_EMAIL'} unless scalar @{$opts{email}};

    # Print help if help requested or no options supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND librarymerge' ) if $opts{help};

    # Create the object
    my $self = { seqmapped_bams => {},
                 seqlib_bams    => {},
                 mapsetobj      => '',
                 %opts };
    bless $self, $class;
}


sub dir {
    my $self = shift;
    return $self->{dir};
}


sub email {
    my $self = shift;
    return $self->{email};
}


sub emails {
    my $self = shift;
    return @{ $self->{email} };
}


sub outfile {
    my $self = shift;
    return $self->{verbose};
}


sub tmpdir {
    my $self = shift;
    return $self->{tmpdir};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub execute {
    my $self = shift;

    # Get mapset details from LIMS
    my $ms = QCMG::SeqResults::Mapsets->new();
    $self->{mapsetobj} = $ms;

    # Process by donor, library, BAM
    my @donors = sort $ms->donors;
    foreach my $donor (@donors) {
        qlogprint( "$donor\n" ) if $self->verbose > 1;

        my @libraries = sort $ms->libraries_for_donor_no_mirna( $donor );
        foreach my $library (@libraries) {
            qlogprint( "   $library\n" ) if $self->verbose > 1;
        
            # Consult the LIMS to see what BAMs are supposed to be in
            # this library.
            my @lims_bams = sort $ms->seqmapped_bams_for_library( $library );
            foreach my $bam (@lims_bams) {
                qlogprint( "      mapbam: $bam\n" ) if $self->verbose > 1;
                # Check if mapset BAM exists
            }

            # A library can only exist if there are BAM records so take
            # the first seq_mapped BAM from the LIMS and predict the 
            # library BAM name based on it.
            my $libbam = $ms->seqlib_bam_for_library( $library );
            qlogprint( "      libbam: $libbam\n" ) if $self->verbose > 1;

            # Symlinks are a special case - if there is a single
            # seq_mapped BAM in the library then we do not need to merge
            # and in fact we just symlink the library to the seq_mapped
            # BAM.  So if the lib is a symlink then we check that there
            # is only one BAM in the LIMS and if so, we can skip the 
            # rest of the tests.

            if (-l $libbam) {
               qlogprint( "      library $library has a symlink BAM\n" )
                   if $self->verbose > 1;
               my $lims_bam_count = scalar(@lims_bams);
               if ($lims_bam_count != 1) {
                   warn "Library $library is a symlink but has" .
                        " $lims_bam_count mapsets listed in the LIMS\n";
               }
            }
            else {

                # Now go get the predicted library name and use the @RG
                # lines to work out what BAMs were merged into the library
                my $ra_rg_bams = bams_that_were_merged_into_this_bam( $libbam );

                # If we couldn't open the library BAM then the rest of
                # the testing is pointless so move on
                next unless defined $ra_rg_bams;

                foreach my $bam (@{$ra_rg_bams}) {
                    qlogprint( "         subbam: $bam\n" ) if $self->verbose > 1;
                }

                # Now we need to compare the LIMS and actual BAM lists and
                # complain if they are not EXACTLY the same;

                my %lims_bams = map { $_ => 1 }
                                map { m/^.*\/([^\/]+)$/ }
                                @lims_bams;
                my %rg_bams   = map { $_ => 1 }
                                map { m/^.*\/([^\/]+)$/ }
                                @{ $ra_rg_bams };

                #if (scalar(@lims_bams) != scalar(@rg_bams)) {
                #    warn 'LIMS and actual BAM counts differ for library' .
                #         " $library ($donor)\n";
                #    foreach my $bam (sort @lims_bams) {
                #        warn "   LIMS: $bam\n";
                #    }
                #    foreach my $bam (@rg_bams) {
                #        warn "   \@RG: $bam\n";
                #    }
                #}

                foreach my $key (sort keys %lims_bams) {
                    if (! exists $rg_bams{$key}) {
                        warn "Library problem ($donor:$library) -" .
                             " LIMS BAM $key is not in \@RG list\n";
                    }
                }
                foreach my $key (sort keys %rg_bams) {
                    if (! exists $lims_bams{$key}) {
                        warn "Library problem ($donor:$library) -" .
                             " \@RG BAM $key is not in LIMS list\n";
                    }
                }
            }

            # diagnostic death
            #die if ($donor =~ /1987/);
        }
    }
}

#sub identify_libraries_to_be_merged {
#    my $self = shift;
#
#    my @donors = $self->donors;
#
#    foreach my $lib (@libraries) {
#        # Which mapsets are part of the library and are they on disk?
#        my $lib_bam = $lib . '.bam';
#        my $thislib = $merges{ $lib };
#
#        my @mapsets = @{ $self->{mapsets_by_library}->{$lib} };
#        foreach my $mapset (@mapsets) {
#            my $bam = $mapset . '.bam';
#            if (exists $self->{seqmapped_bams}->{$bam}) {
#                push @{ $thislib->{bams_found} }, $bam;
#            }
#            else {
#                push @{ $thislib->{bams_not_found} }, $bam;
#            }
#        }
#
#        # Does the library already exist on disk?
#        if (exists $self->{seqlib_bams}->{$lib_bam}) {
#            my $seqlib_bam = $self->{seqlib_bams}->{$lib_bam};
#            $thislib->{lib_bam_found} = $seqlib_bam; 
#            # What seq_mapped BAMs have been merged to create this lib?
#            # Note that this method does work on symlinked BAMs but of 
#            # course they have not been through merging so they always 
#            # return no merged BAMs.  Just FYI.
#            $thislib->{lib_bam_contains} =
#                bams_that_were_merged_into_this_bam( $seqlib_bam );
#        }
#        else {
#            $thislib->{lib_bam_found} = undef;
#        }
#    }
#
#    $self->{merges} = \%merges;
#}


sub create_jobs_for_libraries_that_already_exist {
    my $self = shift;

    my @libraries = sort keys %{ $self->{merges} };

    # Libraries that already exist
    foreach my $lib (@libraries) {
        my $thislib = $self->{merges}->{$lib};
        if (defined $thislib->{lib_bam_found}) {
            my $libbam = $thislib->{lib_bam_found};

            # Loop through all BAMs available for merging and check
            # whether they are already in the library BAM
            my %inlib = ();
            foreach my $bam ($self->merge_lib_bam_contains($lib)) {
                #$text .= "  found:     $bam\n";
            }
            foreach my $bam ($self->merge_bams_found($lib)) {
                #$text .= "  found:     $bam\n";
            }

            #foreach my $bam ($self->merge_bams_not_found($lib)) {
            #    $text .= "  not found: $bam\n";
            #}
        }
    }

}


sub mapsets_by_library_report {
    my $self = shift;

    # Libraries
    my @libraries = sort keys %{ $self->{mapsets_by_library} };
    my $text = 'ICGC Libraries in LIMS ('. scalar(@libraries). ") :\n\n";
    foreach my $lib (@libraries) {
        $text .= $lib. "\n";
        foreach my $mapset (@{ $self->{mapsets_by_library}->{$lib} }) {
            $text .= "  $mapset\n";
        }
    }

    return $text;
}


sub output_report {
    my $self = shift;

    # Report is emailed unless -o is specified to direct output to a file
    if ($self->outfile) {
        open OUT, '>'.$self->outfile ||
            die 'Unable to open ',$self->outfile," for writing $!";
        print OUT $self->report_text();
        close OUT;
    }
    else {
        qmail( To      => $self->email,
               From    => $ENV{QCMG_EMAIL},
               Subject => 'need something to go here' .
                          ' Library-level merges from '. $self->dir ."\n\n",
               Message => $self->report_text() );
    }
}


sub get_bams {
    my $self = shift;

    # Find BAMs in seq_mapped/ and seq_lib/ directories
    my $finder = QCMG::FileDir::Finder->new( verbose => 0 );
    my %seqmapped_bams = seqmapped_bams( $self->dir );
    my %seqlib_bams    = seqlib_bams( $self->dir );

    $self->{ seqmapped_bams } = \%seqmapped_bams;
    $self->{ seqlib_bams }    = \%seqlib_bams;
}


sub report_text {
    my $self = shift;

    my $text = 
        "\ntimelords.pl librarymerge  v$REVISION  [" . localtime() . "]\n" .
        '   dir           '. $self->dir ."\n".
        '   outfile       '. $self->outfile ."\n".
        '   email(s)      '. join("\n".' 'x17, $self->emails) ."\n".
        '   verbose       '. $self->verbose ."\n\n";

    $text .= "\n" . $self->mapsets_report;
    $text .= "\n" . $self->mapsets_by_library_report;
    $text .= "\n" . $self->merges_report;

    return $text;
}

1;

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2012

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
