#! /usr/bin/perl -w

=head1 NAME

check_matching_stats_XMATE.pl

=head1 USAGE

 perl check_matching_stats_XMATE.pl
	-p full path of the directory to be checked

=head1 DESCRIPTION

This program generates matching information for completed X-MATE runs.

=head1 AUTHORS

=over 3

=item Nicole Cloonan (n.cloonan@imb.uq.edu.au)

=item David Wood (d.wood@imb.uq.edu.au)

=back

=head1 COPYRIGHT

This software is copyright 2010 by the Queensland Centre for Medical
Genomics. All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.  For commercial or governmental 
use, please contact licensing\@qcmg.org.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://bioinformatics.qcmg.org/software/

=cut

use Getopt::Std;

# get command line options
getopt("p");
getopts("p:");

# if insufficient options, print usage
if (!($opt_p)) {
    print "\n\nUsage: $0\n\n";
    print "\tREQUIRED:\n";
    print "\t-p full path of the directory to be checked\n";
    die "\n\n";
}

# set paramaters
$path = $opt_p;
%counts = ();

print STDOUT "\n\n\nChecking directory:\t$path\n";

# count genome matches
print STDOUT "Checking genome and junction matches...\n";
while ($filename = glob("$path/*.collated")) {
    print STDOUT "File: $filename\n";

    # get length of tags
    $basename = `basename $filename`;
    @fileparts = split(/\./, $basename);
    $fileparts[2] =~ s/mers//g;

    # count tags at that length
    open(IN, $filename);
    while ($line = <IN>) {
        next if $line =~ /#/;
        if ($line =~ /^\>\S+\t(\d+)/) {
            if ($1 > 1) {
                $counts{$fileparts[2]}{'multi'}++;
            }
            else {
                $counts{$fileparts[2]}{'unique'}++;
            }
        }
    }
    close(IN);
}

$uniqueSequences = 0;
$multiSequences = 0;

$uniqueSequence = 0;
$multiSequence = 0;

print STDOUT "\n\n";
print STDOUT "Recursive Run\tUnique\tMulti\tTotal\n";
foreach $length (sort { $b <=> $a } keys %counts) {
    print STDOUT "$length mers:\t$counts{$length}{'unique'}\t$counts{$length}{'multi'}\t".($counts{$length}{'unique'} + $counts{$length}{'multi'})."\n";
    $uniqueSequence = $uniqueSequence + ($length * $counts{$length}{'unique'});
    $multiSequence = $multiSequence + ($length * $counts{$length}{'multi'});
    $uniqueSequences += $counts{$length}{'unique'};
    $multiSequences += $counts{$length}{'multi'};
}

print STDOUT "total:\t$uniqueSequences\t$multiSequences\t".($uniqueSequences+$multiSequences)."\n";

$totalSequence = ($multiSequence + $uniqueSequence) / 1000000000;
$uniqueSequence = $uniqueSequence / 1000000000;
$multiSequence = $multiSequence / 1000000000;

print STDOUT "\n\nTotal Unique GB matched:\t$uniqueSequence\n";
print STDOUT "Total Multi GB matched:\t$multiSequence\n";
print STDOUT "Total GB matched:\t$totalSequence\n\n\n";

exit(0);
