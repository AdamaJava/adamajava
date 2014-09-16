#! /usr/bin/perl -w

=head1 NAME

chrom_sizes.pl

=head1 USAGE

 perl chrom_sizes.pl
	-p path name containing the *.fa files for sizing
	-o name of the output file

=head1 DESCRIPTION

Calculate the size of each sequence in the fasta file and
output to the specified file.

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
use, please contact licensing\@qcmg.org.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://bioinformatics.qcmg.org/software/

=cut


use Getopt::Std;

# get command line options
getopt("op");
getopts("op:");

# if insufficient options, print usage
if (!(($opt_o) && ($opt_p))) {
    print "\n\nUsage: $0\n\n";
    print "\tREQUIRED:\n";
    print "\t-p path name containing the *.fa files for sizing\n";
    print "\t-o name of the output file\n";
    die "\n\n";
}

# set paramaters
$path   = $opt_p;
$output = $opt_o;

open(OUT, ">$output") or die "Can't open $output because $!\n";
while ($filename = glob("$path/*.fa")) {
    $size = 0;
    open(IN, $filename) or die "Can't open $filename because $!\n";
    while ($line = <IN>) {
        next if $line =~ />/;
        chomp($line);
        $size = $size + length($line);
    }
    close(IN);
    $filename =~ s/$path//g;
    $filename =~ s/\///g;
    $filename =~ s/\.fa//g;
    print OUT "$filename\t$size\n";
}
close(OUT);

exit(0);
