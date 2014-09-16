#!/usr/bin/perl -w

##############################################################################
#
#  Program:  bam2bed.pl
#  Author:   John V Pearson
#  Created:  2010-09-29
#
#  Quick-n-dirty script for making positive and negative strand BED
#  files from a single BAM file.
#
#  $Id: bam2bed.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );
use POSIX qw( floor );


use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: bam2bed.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $infile         = '';
    #my $tmpdir         = '/panfs/home/jpearson/tmp/';
    my $tmpdir         = '/panfs/imb/home/uqjpear1/tmp/';
    #my $samtoolsbin    = '/panfs/share/software/samtools-0.1.8/samtools';
    my $samtoolsbin    = '/sw/samtools/1.8/bin/samtools';
       $VERBOSE        = 0;
       $VERSION        = 0;
    my $help           = 0;
    my $man            = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \$infile,        # -i
           't|tmpdir=s'           => \$tmpdir,        # -t
           's|samtoolsbin=s'      => \$samtoolsbin,   # -s
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

    die 'You must supply a BAM file name' unless $infile;

    # Append '/' to tmpdir if not already present;
    $tmpdir .= '/' unless ($tmpdir =~ m/\/$/);

    print( "\nbam2bed.pl  v$REVISION  [" . localtime() . "]\n",
           "   infile        $infile\n",
           "   tmpdir        $tmpdir\n",
           "   samtoolsbin   $samtoolsbin\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    process( $infile, $tmpdir, $samtoolsbin );
}


sub process {
    my $bam         = shift;
    my $tmpdir      = shift;
    my $samtoolsbin = shift;

    # Open BAM file for reading - N.B. we do *NOT* want the header output
    open INSAM,  "$samtoolsbin view $bam |"
        or die "Can't open samtools view on BAM [$bam]: $!";

    # Take BAM filename, strip off any path and do extension
    # substitutions to give positive and negative BED filenames
    $bam =~ /\/([^\/]+)$/;
    my $filestem = $1;
    my $pos_filename = $tmpdir . '/' . $filestem;
    $pos_filename =~ s/\.bam$/\.positive\.galaxy/;
    my $neg_filename = $pos_filename;
    $neg_filename =~ s/\.positive\.galaxy$/\.negative\.galaxy/;

    open OUTPOS, ">$pos_filename"
        or die "Can't open positive BED [$pos_filename] for writing: $!";
    open OUTNEG, ">$neg_filename"
        or die "Can't open negative BED [$pos_filename] for writing: $!";

    my $header = join("\t",'#CHROM',qw( START END STRAND COUNT ));

    print OUTPOS $header,"\n";
    print OUTNEG $header,"\n";

    my $current_seq = '';
    my $current_pos = 0;
    my $pos_count   = 0;
    my $neg_count   = 0;
    my $ctr         = 0;

    # Read through BAM
    while (my $line = <INSAM>) {

        # Because of the way the bits are arranged in the FLAG we see:
        # substr($flag,0,1)  = unused
        # substr($flag,1,1)  = unused
        # substr($flag,2,1)  = unused
        # substr($flag,3,1)  = unused
        # substr($flag,4,1)  = unused
        # substr($flag,5,1)  = read is PCR or optical duplicate
        # substr($flag,6,1)  = read fails platform/vendor checks
        # substr($flag,7,1)  = alignment is not primary (???)
        # substr($flag,8,1)  = read is second in pair
        # substr($flag,9,1)  = read is first in pair
        # substr($flag,10,1) = mate strand
        # substr($flag,11,1) = read strand (0=forward)
        # substr($flag,12,1) = mate is unmapped
        # substr($flag,13,1) = read is unmapped
        # substr($flag,14,1) = read is in a proper pair
        # substr($flag,15,1) = paired in sequencing

        chomp $line;

        $ctr++;
        if (($ctr % 1000000 == 0) and $VERBOSE) {
            print '[' . localtime() . ']  ', $ctr, " Records read from BAM\n";
        }

        my @fields = split /\t/, $line;
        my ($zb,$zf) = int_to_bit_string( $fields[1] ); 
        
        next if (substr($zb,8,1) eq '1');  # skip read 2
        next if (substr($zb,6,1) eq '1');  # skip vendor fails

        # For any given position in a given sequence, we need to tally
        # all of the positive and negtive strand reads at that point so
        # we need to keep reading records until we see the sequence name
        # or start position change.

        my $strand = substr($zb,11,1);

        if ($current_pos == 0) {
            $current_seq = $fields[2];
            $current_pos = $fields[3];
        }
        elsif (($fields[2] ne $current_seq) or 
               ($fields[3] != $current_pos)) {
            if ($pos_count > 0) {
                print OUTPOS join("\t", $current_seq,
                                        $current_pos,
                                        $current_pos+1,
                                        '+',
                                        $pos_count), "\n";
                $pos_count = 0;
            }
            if ($neg_count > 0) {
                print OUTNEG join("\t", $current_seq,
                                        $current_pos,
                                        $current_pos+1,
                                        '-',
                                        $neg_count), "\n";
                $neg_count = 0;
            }
            $current_seq = $fields[2];
            $current_pos = $fields[3];
        }
        $pos_count++ if ($strand eq '0');
        $neg_count++ if ($strand eq '1');

#        print join("\t",$fields[2],$fields[3],$strand,$ctr,
#                        $current_seq,$current_pos,$pos_count,$neg_count),"\n";

    }
}


sub int_to_bit_string {
    my $int = shift;

    # Special case
    return ( '0000000000000000', '0' ) if ($int == 0);

    my $zb = '';
    foreach my $powerof2 (32768,16384,8192,4096,2048,1024,512,256,128,64,32,16,8,4,2,1) {
        my $this_bit = floor( $int / $powerof2 );
        if ($this_bit > 0) {
            $zb .= '1';
            $int -= $powerof2;
        }
        else {
            $zb .= '0';
        }
    }

    my $zf = '';
    my $zf_mask = '     dfs21RrUuPp';
    foreach (0..15) {
        if (substr($zb,15-$_,1) eq '1') {
            $zf .= substr($zf_mask,15-$_,1);
        }
    }
    return ($zb,$zf);
}


__END__

=head1 NAME

bam2bed.pl - Perl script for outputting BED from BAM


=head1 SYNOPSIS

 bam2bed.pl [options]


=head1 ABSTRACT

This is a very quick-n-dirty script for creating BED files of read
start points from BAM files for WTA analysis.


=head1 OPTIONS

 -i | --infile        name of BAM file
 -t | --tmpdir        temporary directory; default=/panfs/home/jpearson/tmp
 -s | --samtoolsbin   location of samtools binary
 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<bam2bed.pl> relies on samtools for the conversions between SAM and 
BAM so you'll need to have samtools installed to use this script.  This
script is very basic and makes the following assumptions:

 1. The BAM is sorted by sequence and position within each sequence.
 2. All reads flagged as second-in-pair are ignored.  This means we only
    count each LMP or PE pair once BUT it also means we tally LMP and PE
    read-pairs based on the leftmost base of their F3 read which might
    be ideal.

=head2 Commandline Options

=over

=item B<-i | --infile>

Full pathname to BAM to be processed.

=item B<-t | --tmpdir>

Directory where the 2 output BED files (one for positive and one for
negative) will be placed.
This script was designed to be run once by John
Pearson so the default value for this parameter is JP's ~/tmp/
directory (/panfs/home/jpearson/tmp/).

=item B<-s | --samtoolsbin>

Full pathname to the samtools executable.

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


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: bam2bed.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010

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
