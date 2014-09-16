#! /usr/bin/perl -w

=head1 NAME

filter_bedGraphs.pl

=head1 USAGE

 perl filter_bedGraphs.pl
	-f name of file to be filtered
	-m minimum number of tags to report

=head1 DESCRIPTION

This program filters wiggle plots/bedGraphs by a user defined amount. This is used to compress
the data so that it can be easily uploaded into the UCSC genome browser. 

=head1 AUTHORS

=over 3

=item Nicole Cloonan (n.cloonan@imb.uq.edu.au)

=back

=head1 COPYRIGHT

This software is copyright 2010 by the Queensland Centre for Medical
Genomics. All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.  For commercial or governmental 
use, please contact licensing@qcmg.org.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://bioinformatics.qcmg.org/software/

=cut


use Getopt::Std;

getopt("fm");
getopts("fm:");

#if insufficient options, print usage

if(! ($opt_f && $opt_m)) {
    print "\n\nUsage: $0\n\n";
    print "\tREQUIRED:\n";
    print "\t-f name of file to be filtered\n";
    print "\t-m minimum number of tags to report\n";
    die "\n\n";
}

#set paramaters

$filename=$opt_f;
$min=$opt_m;

open (WIGGLE, "$filename") or die "Can't open $filename because $!\n";
open (OUT, ">$filename.$min.filtered") or die "Can't open $filename.$min.filtered because $!\n";

#read and output header
$header=<WIGGLE>;
print OUT "$header";
	
while (<WIGGLE>) {
    chomp();
    ($chr, $start, $stop, $count)=split();
    if ($count >=$min) {print OUT "$chr\t$start\t$stop\t$count\n"}
}

exit(0);
