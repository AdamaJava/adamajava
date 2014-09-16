#!/usr/bin/perl -w

##############################################################################
#
#  Program:  dcc_tools.pl
#  Author:   John V Pearson
#  Created:  2012-11-01
#
#  Operate on ICGC DCC files.
#
#  $Id: dcc_tools.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX qw( floor );
use Storable qw(dclone);
use Statistics::Descriptive;

use QCMG::FileDir::Finder;
use QCMG::IO::DccSnpReader;
use QCMG::IO::GffReader;
use QCMG::IO::MafReader;
use QCMG::IO::VcfReader;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: dcc_tools.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures (if any)

BEGIN {
}


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $filea      = '';
    my $fileb      = '';
    my $outfile    = '';
    my $logfile    = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    my %modes = ( compare => 1 );

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'a|filea=s'            => \$filea,         # -a
           'b|fileb=s'            => \$fileb,         # -b
           'o|outfile=s'          => \$outfile,       # -o
           'l|logfile=s'          => \$logfile,       # -l
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );
    
    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    qlogfile($logfile) if $logfile;
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");
    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    my $mode = shift @ARGV;
    die "Unknown mode: $mode\n" unless exists $modes{$mode};

    if ($mode =~ /compare/i) {
       my $ra_recs1 = read_dcc_file( $filea );
       my $ra_recs2 = read_dcc_file( $fileb );
       my ($ra_both, $ra_a_only, $ra_b_only) =
           compare_dccs( $ra_recs1, $ra_recs2 );
    }
    else {
        die "Mode $mode not yet implemented\n";
    }

    qlogend;
}


sub read_dcc_file {
    my $dccfile = shift;

    my @records = ();
    my $dcc = QCMG::IO::DccSnpReader->new( filename => $dccfile );
    while (my $rec = $dcc->next_record()) {
       push @records, $rec;
    }
    return \@records;
}


sub sort_records {
    my $ra_records = shift;

    my %records = ();
    foreach my $rec (@{ $ra_records }) {
       push @{ $records{ $rec->chromosome } }, $rec;
    }

    # Sort records by start position within each sequence
    foreach my $seq (keys %records) {
        my @sorted_records = map  { $_->[1] }
                             sort { $a->[0] <=> $b->[0] }
                             map  { [$_->chromosome_start, $_ ] }
                             @{ $records{$seq} };
        $records{ $seq } = \@sorted_records;
    }

    return \%records;
}


sub compare_dccs {
    my $ra_records1 = shift;
    my $ra_records2 = shift;

    my $rh_records1 = sort_records( $ra_records1 );
    my $rh_records2 = sort_records( $ra_records2 );

    # Outputs
    my @both        = ();
    my @first_only  = ();
    my @second_only = ();

    my %seqs1 = ();
    my %seqs2 = ();

    $seqs1{ $_ } = 1 foreach (keys %{$rh_records1});
    $seqs2{ $_ } = 1 foreach (keys %{$rh_records2});

    foreach my $seq (sort keys %{$rh_records1}) {
        # If this seq is not present in second list them immediately
        # push these records onto @first_only and skip to next seq
        if (! exists $rh_records2->{ $seq }) {
            push @first_only, @{ $rh_records1->{ $seq } };
            next;
        }

        my @recs1 = @{ $rh_records1->{ $seq } };
        my @recs2 = @{ $rh_records2->{ $seq } };

        while (scalar(@recs1) or scalar(@recs2)) {
            if (scalar(@recs2) == 0) {
                push @first_only, @recs1;
                last;
            }
            elsif (scalar(@recs1) == 0) {
                push @second_only, @recs2;
                last;
            }
            elsif ($recs1[0]->chromosome_start ==
                   $recs2[0]->chromosome_start) {
                push @both, shift(@recs1);
                my $discard = shift @recs2;
            }    
            elsif ($recs1[0]->chromosome_start < 
                   $recs2[0]->chromosome_start) {
                push @first_only, shift(@recs1);
            }
            elsif ($recs1[0]->chromosome_start > 
                   $recs2[0]->chromosome_start) {
                push @second_only, shift(@recs2);
            }
        }

        # Remove completed stuff from both lists
        delete $rh_records1->{ $seq };
        delete $rh_records2->{ $seq };
    }

    # Anything left in list2 must be unique to 2
    foreach my $seq (sort keys %{$rh_records2}) {
        push @second_only, @{ $rh_records2->{ $seq } };
    }

    print 'Count 1 in:       ',scalar(@{$ra_records1}),"\n",
          'Count 2 in:       ',scalar(@{$ra_records2}),"\n",
          'Count both out:   ',scalar(@both),"\n",
          'Count 1-only out: ',scalar(@first_only),"\n",
          'Count 2-only out: ',scalar(@second_only),"\n";

    return \@both, \@first_only, \@second_only;
}


__END__

=head1 NAME

dcc_tools.pl - Tools for operating on ICGC DCC files


=head1 SYNOPSIS

 dcc_tools.pl mode [options]


=head1 ABSTRACT

This script operates on ICGC DCC files.


=head1 OPTIONS

 compare

 -a | --filea         First DCC file
 -b | --fileb         Second DCC file
 -o | --outfile       MAF output file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

=head2 Modes

=head3 compare

=over 2

=item B<-a | --filea>

First DCC file containing records to be compared.

=item B<-b | --fileb>

Second DCC file containing records to be compared.

=item B<-o | --outfile>

MAF output file.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<--version>

Print the script version and exit immediately.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: dcc_tools.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012

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
