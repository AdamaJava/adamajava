#!/usr/bin/perl -w

##############################################################################
#
#  Program:  torrent_stats.pl
#  Author:   John V Pearson
#  Created:  2012-02-01
#
#  $Id: torrent_stats.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );
use QCMG::DB::Torrent;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: torrent_stats.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $outfile        = '';
    my $user           = '';
    my $passwd         = '';
       $VERBOSE        = 0;
       $VERSION        = 0;
    my $help           = 0;
    my $man            = 0;

    # Print usage message if no arguments supplied
    #pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'o|outfile=s'          => \$outfile,       # -o
           'u|user=s'             => \$user,          # -u
           'p|passwd=s'           => \$passwd,        # -p
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    print( "\ntorrent_stats.pl  v$REVISION  [" . localtime() . "]\n",
           "   outfile       $outfile\n",
           "   user          $user\n",
           "   passwd        $passwd\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;


    my $ts = QCMG::DB::Torrent->new();
    $ts->connect;
    my $ra_expts     = experiments( $ts );
    my $ra_analmetrs = analysismetrics( $ts );
    my $ra_qualmetrs = qualitymetrics( $ts );
    my $ra_results   = results( $ts );

    #results_dump( $ra_results );

    my $ra_flat_expts = resolve_this_mess( $ts, $ra_expts, $ra_analmetrs,
                                           $ra_qualmetrs, $ra_results );
    big_flat_expt_dump( $ra_flat_expts );

}


sub big_flat_expt_dump {
    my $ra_expts = shift;

    # Stop the whining about undefined values in print()
    no warnings;
    my @keys = qw( geneus_id 
                     pgm_name chip_type chip_barcode serial_num expt_name
                     project sample library
                     date flows start_time end_time  
                   results_id
                     results_name timestamp analysis_ver bam_location
                   analysismetrics_id
                     bead empty ignored pinned dud live lib
                   qualitymetrics_id
                     q0_bases q17_bases q20_bases q0_mean_length 
                     q17_mean_length q20_mean_length
                   libmetrics_id
                     genome q7_mapped_bases q20_mapped_bases
                   );
    print join("\t", @keys),"\n";
    foreach my $expt (@{ $ra_expts }) {
        my @values = map { $expt->{$_} } @keys;
        print join("\t", @values),"\n";
    }
}


sub resolve_this_mess {
    my $ts = shift;
    my $ra_expts = shift;
    my $ra_analmetrs = shift;
    my $ra_qualmetrs = shift;
    my $ra_results = shift;
    #my $ra_libmetrs = shift;

    # Experiments is the primary "reporting" unit because it represents
    # a chip run on the machine so we will want to finally output a
    # single line per experiment.  Results is the glue because it links
    # experiments to quality and analysis metrics.  The trick is going
    # to be to pick which is the best Result for each Experiment.

    # Sort the Experiment records by ID
    my @sorted_expts = map  { $_->[0] }
                       sort { $a->[1] <=> $b->[1] }
                       map  { [ $_, $_->{geneus_id} ] }
                       @{ $ra_expts };

    # Hash the Results records based on their experiment_id
    my %res_by_expt = ();
    foreach my $record (@{ $ra_results }) {
        push @{ $res_by_expt{ $record->{experiment_id} } }, $record;
    }
    
    # Hash the Results metrics based on ID
    my %results = ();
    foreach my $record (@{ $ra_results }) {
        $results{ $record->{geneus_id} } = $record;
    }

    # Hash the Analysis metrics based on ID
    my %anal_metrics = ();
    foreach my $record (@{ $ra_analmetrs }) {
        $anal_metrics{ $record->{geneus_id} } = $record;
    }

    # Hash the Quality metrics based on ID
    my %qual_metrics = ();
    foreach my $record (@{ $ra_qualmetrs }) {
        $qual_metrics{ $record->{geneus_id} } = $record;
    }

    ## Hash the library metrics based on ID
    #my %lib_metrics = ();
    #foreach my $record (@{ $ra_libmetrs }) {
    #    $lib_metrics{ $record->{geneus_id} } = $record;
    #}

    # Loop through Experiments
    foreach my $expt (@sorted_expts) {

        # Loop through all Results that match this Experiment and pick
        # out values that will be used to select a single "best" Result
        my @choices = ();
        foreach my $result (@{ $res_by_expt{ $expt->{geneus_id} } }) {

            # Skip results without Analysis, Quality and Library metrics
            next unless (exists $result->{analmtr_ids}->[0] and
                         exists $result->{qualmtr_ids}->[0]);

            my $analmtr_rec = $anal_metrics{ $result->{analmtr_ids}->[0] };
            my $qualmtr_rec = $qual_metrics{ $result->{qualmtr_ids}->[0] };

            my $new_rec = { results_id => $result->{geneus_id},
                            bead       => $analmtr_rec->{bead},
                            lib        => $analmtr_rec->{lib},
                            q0_bases   => $qualmtr_rec->{q0_bases},
                            q17_bases  => $qualmtr_rec->{q17_bases},
                            q20_bases  => $qualmtr_rec->{q20_bases} };

            # Populate extra fields if lib metrics available
            if (exists $result->{libmtr_ids}->[0]) {
                my $libmtr_rec = $ts->query( 'libmetrics/' .
                                             $result->{libmtr_ids}->[0] );
                if (defined $libmtr_rec) {
                    $new_rec->{genome} = $libmtr_rec->{genome};
                    $new_rec->{q7_mapped_bases} = $libmtr_rec->{q7_mapped_bases};
                    $new_rec->{q20_mapped_bases} = $libmtr_rec->{q20_mapped_bases};
                }
            }

            push @choices, $new_rec;

        }

        # Make choice based on highest q20_mapped_bases
        my $choice = 0;
        my $max    = 0;
        foreach my $ctr (0..$#choices) {
            if (defined $choices[$ctr]->{q20_mapped_bases}
                and $choices[$ctr]->{q20_mapped_bases} > $max) {
                $choice = $choices[$ctr]->{results_id};
                $max    = $choices[$ctr]->{q20_mapped_bases};
            }
        }

        # If there were no q20_mapped_bases then try again with
        # raw base count - q20_bases
        if ($max == 0) {
            foreach my $ctr (0..$#choices) {
                if (defined $choices[$ctr]->{q20_bases}
                    and $choices[$ctr]->{q20_bases} > $max) {
                    $choice = $choices[$ctr]->{results_id};
                    $max    = $choices[$ctr]->{q20_bases};
                }
            }
        }

        # If there are multiple Results, print em out
        if ($VERBOSE and scalar(@choices) > 1) {
            # In this scope, silence whining about undef values in print()
            no warnings;

            print "\n",
                  join("  ", $expt->{geneus_id},
                             $expt->{pgm_name},
                             $expt->{sample},
                             $expt->{project},
                             $expt->{chip_type},
                             $expt->{chip_barcode},
                             $expt->{date} ), "\n";
            print "Chose item $choice from:\n";
            print join("\t", qw(id bead lib q0_bases q17_bases q20_bases
                                q7_mapped_bases q20_mapped_bases genome)), "\n";
            foreach my $rec (@choices) {
                print join("\t", $rec->{results_id},
                                 $rec->{bead},
                                 $rec->{lib},
                                 $rec->{q0_bases},
                                 $rec->{q17_bases},
                                 $rec->{q20_bases},
                                 $rec->{q7_mapped_bases},
                                 $rec->{q20_mapped_bases},
                                 $rec->{genome} ), "\n";
            }
        }

        # Now that we've chosen our Result, it's time to build the MEGA
        # record.  We'll start with Experiment and load it up.

        # Transfer values from Results
        my $result = $results{ $choice };
        my @keys = qw( results_name timestamp analysis_ver 
                       bam_location );
        foreach my $key (@keys) {
            $expt->{$key} = $result->{$key};
        }
        $expt->{results_id} = $result->{ geneus_id };

        # Transfer values from AnalysisMetrics
        if (exists $result->{analmtr_ids}->[0]) {
            my $analmtr_rec = $anal_metrics{ $result->{analmtr_ids}->[0] };
            my @keys = qw( bead empty ignored pinned dud live lib );
            foreach my $key (@keys) {
                $expt->{$key} = $analmtr_rec->{$key};
            }
            $expt->{analysismetrics_id} = $analmtr_rec->{ geneus_id };
        }

        # Transfer values from QualityMetrics
        if (exists $result->{qualmtr_ids}->[0]) {
            my $qualmtr_rec = $qual_metrics{ $result->{qualmtr_ids}->[0] };
            my @keys = qw( q0_bases q17_bases q20_bases q0_mean_length 
                           q17_mean_length q20_mean_length );
            foreach my $key (@keys) {
                $expt->{$key} = $qualmtr_rec->{$key};
            }
            $expt->{qualitymetrics_id} = $qualmtr_rec->{ geneus_id };
        }

        # Transfer values from LibraryMetrics
        if (exists $result->{libmtr_ids}->[0]) {
            my $libmtr_rec = $ts->query( 'libmetrics/' .
                                         $result->{libmtr_ids}->[0] );
            if (defined $libmtr_rec) {
                my @keys = qw( genome q7_mapped_bases q20_mapped_bases );
                foreach my $key (@keys) {
                    $expt->{$key} = $libmtr_rec->{$key};
                }
            }
            $expt->{libmetrics_id} = $result->{libmtr_ids}->[0];
        }

    }

    return \@sorted_expts;
}


sub experiments {
    my $ts = shift;
    my $results = $ts->query( 'experiment/?format=json&limit=0' );
    my @records = ();
    foreach my $record (@{ $results->{objects} }) {
        push @records, { geneus_id    => $record->{id},
                         pgm_name     => $record->{pgmName},
                         sample       => $record->{sample}, 
                         project      => $record->{project}, 
                         expt_name    => $record->{expName},
                         library      => $record->{library}, 
                         chip_type    => $record->{chipType}, 
                         chip_barcode => $record->{chipBarcode}, 
                         date         => $record->{date}, 
                         flows        => $record->{flows},
                         serial_num   => $record->{log}->{serial_number}, 
                         start_time   => $record->{log}->{start_time}, 
                         end_time     => $record->{log}->{end_time}, 
                       };
    }
    return \@records;
}


# analysismetrics:
# bead + empty + ignored + pinned = (approx) total wells on chip
# bead - dud = live 
# live - tf = lib

sub analysismetrics {
    my $ts = shift;
    my $results = $ts->query( 'analysismetrics/?format=json&limit=0' );
    my @records = ();
    foreach my $record (@{ $results->{objects} }) {
        push @records, { geneus_id    => $record->{id},
                         bead         => $record->{bead},
                         empty        => $record->{empty},
                         ignored      => $record->{ignored},
                         pinned       => $record->{pinned},
                         dud          => $record->{dud},
                         live         => $record->{live},
                         lib          => $record->{lib},
                       };
#        print $record->{id} , "\t" ,
#              $record->{bead} + $record->{empty} +
#              $record->{ignored} + $record->{pinned} , "\n";
    }

    return \@records;
}


sub qualitymetrics {
    my $ts = shift;
    my $results = $ts->query( 'qualitymetrics/?format=json&limit=0' );
    my @records = ();
    foreach my $record (@{ $results->{objects} }) {
        push @records, { geneus_id       => $record->{id},
                         q0_bases        => $record->{q0_bases},
                         q17_bases       => $record->{q17_bases},
                         q20_bases       => $record->{q20_bases},
                         q0_mean_length  => $record->{q0_mean_read_length},
                         q17_mean_length => $record->{q17_mean_read_length},
                         q20_mean_length => $record->{q20_mean_read_length},
                       };
#        print join("\t", $record->{id},
#                         $record->{q0_bases},
#                         $record->{q17_bases},
#                         $record->{q20_bases},
#                         $record->{q0_mean_read_length},
#                         $record->{q17_mean_read_length},
#                         $record->{q20_mean_read_length} ), "\n";
    }

    return \@records;
}


sub results {
    my $ts = shift;
    my $results = $ts->query( 'results/?format=json&limit=0' );
    my @records = ();
    foreach my $record (@{ $results->{objects} }) {
        my $output = { geneus_id       => $record->{id},
                       results_name    => $record->{resultsName},
                       timestamp       => $record->{timeStamp},
                       analysis_ver    => $record->{analysisVersion},
                       bam_location    => $record->{bamLink},
                       experiment_id   => undef,
                       qualmtr_ids     => [],
                       analmtr_ids     => [],
                       libmtr_ids      => [],
                       };

        if ($record->{experiment} =~ /experiment\/(\d+)\/$/) {
           $output->{experiment_id} = $1;
        }

        foreach my $id (@{ $record->{qualitymetrics} }) {
            if ($id =~ /qualitymetrics\/(\d+)\/$/) {
               push @{$output->{qualmtr_ids}}, $1;
            }
        }
        foreach my $id (@{ $record->{analysismetrics} }) {
            if ($id =~ /analysismetrics\/(\d+)\/$/) {
               push @{$output->{analmtr_ids}}, $1;
            }
        }
        foreach my $id (@{ $record->{libmetrics} }) {
            if ($id =~ /libmetrics\/(\d+)\/$/) {
               push @{$output->{libmtr_ids}}, $1;
            }
        }

        push @records, $output
    }

    return \@records;
}


sub results_dump {
    my $ra_records = shift;

    # sort by experiment id
    my @sorted_recs = map  { $_->[0] }
                      sort { $a->[1] <=> $b->[1] }
                      map  { [ $_, $_->{experiment_id} ] }
                      @{ $ra_records };

    foreach my $record (@sorted_recs) {
        my $output = join( "\t", $record->{geneus_id},
                                 $record->{experiment_id},
                                 $record->{results_name},
                                 join(',',@{ $record->{analmtr_ids} }),
                                 join(',',@{ $record->{qualmtr_ids} }),
                                 join(',',@{ $record->{libmtr_ids} }),
                                 $record->{timestamp},
                                 $record->{analysis_ver} ) . "\n";
        print $output;
    }
}


__END__

=head1 NAME

torrent_stats.pl - Perl script for extracting data from Torrent Server REST interface


=head1 SYNOPSIS

 torrent_stats.pl [options]


=head1 ABSTRACT



=head1 OPTIONS

 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION


=head2 Commandline Options

=over

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: torrent_stats.pl 4669 2014-07-24 10:48:22Z j.pearson $


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
