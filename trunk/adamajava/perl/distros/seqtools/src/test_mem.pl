#!/usr/bin/perl -w

##############################################################################
#
#  Program:  test_mem.pl
#  Author:   John V Pearson
#  Created:  2011-02-23
#
#  Test memory cost of various array options.
#
#  $Id: test_mem.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Getopt::Long;
use IO::File;
use Data::Dumper;
use Time::HiRes;
use Pod::Usage;

use QCMG::IO::SamReader;

use vars qw( $CVSID $REVISION $VERBOSE $VERSION %PARAMS );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $CVSID ) = '$Id: test_mem.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    $PARAMS{'mode'}         = 1;
    $VERBOSE                = 0;
    $VERSION                = 0;

    my $help                = 0;
    my $man                 = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'd|mode=i'             => \$PARAMS{'mode'},         # -d
           'v|verbose+'           => \$VERBOSE,                # -v
           'version!'             => \$VERSION,                #
           'h|help|?'             => \$help,                   # -h
           'm|man'                => \$man                     # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;
    if ($VERSION) { print "$CVSID\n"; exit }

    print "\ntest_mem.pl  v$REVISION  [" . localtime() ."]\n",
          "    mode           $PARAMS{'mode'}\n",
          "    verbose        $VERBOSE\n\n" if $VERBOSE;

    if ($PARAMS{mode} == 1) {
        mode1();
    }
    elsif ($PARAMS{mode} == 2) {
        mode2();
    }
    elsif ($PARAMS{mode} == 3) {
        mode3();
    }
    elsif ($PARAMS{mode} == 4) {
        mode4();
    }
    else {
       die 'Unrecognised mode [',$PARAMS{mode},"]\n";
    }

    logprint( 'Finished.' );

}


sub mode1 {
   my $count = 200000000;
   logprint( "Allocating $count ints in a single array ..." );
   my @vals = ();
   foreach my $i (0..($count-1)) {
      $vals[$i] += 1;
   }

   logprint( "Incrementing $count ints in a single array ..." );
   foreach my $i (0..($count-1)) {
      $vals[$i] += 1;
   }
   logprint( 'sleeping for 10 secs ...' );
   sleep(10);

   # Results:
   # 100M ints cost 3.3 GB RAM
   # 200M ints cost 6.8 GB RAM
   # 300M ints cost 12.4GB RAM
}


sub mode2 {
   my $count = 300000000;
   logprint( "Allocating $count ints in a single array ..." );
   my @vals = ();
   foreach my $i (0..($count-1)) {
      $vals[$i] += 1;
   }

   logprint( "Deleting $count ints in a single array ..." );
   foreach my $i (0..($count-1)) {
      delete $vals[$i];
   }
   logprint( 'sleeping for 10 secs ...' );
   sleep(10);

   # Results:
   # 100M ints cost 18 secs to allocate and 12 secs to delete and cost
   # the same RAM as mode1.  300M records takes a total of 1:34 minutes
   # so it appears to be pretty close to linear.
}


sub mode3 {
   my $count = 100000000;

   logprint( "Allocating and deleting $count ints in a single array ..." );
   my @vals = ();
   foreach my $i (0..($count-1)) {
      $vals[$i] += 1;
      delete $vals[$i];
   }

   logprint( 'sleeping for 10 secs ...' );
   sleep(10);
   # Results:
   # This uses no memory but never actually finishes - the tight
   # allocation and deletion loop introduces some sort of problem
}


sub mode4 {
   my $count = 300000000;
   my $block = 10000000;

   logprint( "Allocating $count ints in a single array ..." );
   my @vals = ();
   foreach my $i (0..($count-1)) {
      if ($i % $block == 0) {
          my $start = $i-$block;
          my $end   = $i-1;
          logprint( "Deleting ints $start..$end" );
          foreach my $j (($i-$block)..$i) {
              delete $vals[$j];
          }
          logprint( '  reallocating ...' );
      }
      $vals[$i] += 1;
   }

   logprint( 'sleeping for 10 secs ...' );
   sleep(10);
   # Results:
   # This uses 6.5GB RAM for 300M records and takes 2:22 mins.  So
   # deleting definitely saves about half the memory but deleting
   # blocks rather than whole arrays definitely takes a LOT longer.
}



sub logprint {
    print( "[" . localtime() ."]  $_\n") foreach (@_);
}

__END__

=head1 NAME

test_mem.pl - test memory cost of list allocation


=head1 SYNOPSIS

 test_mem.pl [options]


=head1 OPTIONS

 -d | --mode          memory test mode
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -h | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<test_mem.pl> is very basic.


=head2 Command-line options

=over 

=item B<-i | --infile>

SAM or BAM alignment file.  The B<-i> option may be specified multiple 
times, and each B<-i> may be followed by a comma-separated list (no 
spaces!) of filenames.  You may also mix the 2 modes. 

=item B<-o | --outfile>

Name of the text-format file output.

=item B<--version>

Print the script version number and exit immediately.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT unless the B<-l> option is used to
specify a logfile.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a 
perldoc on the script.

=back

=head2 Example invocation

 qcoverage.pl -v \
     -i library1_exome_1.bam \
     -i library1_exome_2.bam \
     -o library1_coverage


=head1 AUTHOR

=over

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: test_mem.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

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
