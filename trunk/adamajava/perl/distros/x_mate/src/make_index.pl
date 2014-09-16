#! /usr/bin/perl -w

=head1 NAME

make_index.pl

=head1 USAGE

 perl make_index.pl
	-f file containing combined donor and acceptor sequences
	-o name of the output file

=head1 DESCRIPTION

Create an index file containing a row for each junction,
and the coordinates for that junction within the .cat (concatenated
junction file).  This index file is then used to decode the name of
the exon (eg it's genomic coordinates) after mapping.

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
getopt("fos");
getopts("fos:");

# if insufficient options, print usage
if (!(($opt_f) && ($opt_o) &&($opt_s))) {
    print "\n\nUsage: $0\n\n";
    print "\tREQUIRED:\n";
    print "\t-f file containing combined donor and acceptor sequences\n";
    print "\t-o name of the output file\n";
    print "\t-s spacer length (must be the same as that used in concatenate_sequences.pl)\n";
    die "\n\n";
}

# set paramaters
$filename = $opt_f;
$output   = $opt_o;
$spacer_size = $opt_s;

open(IN,  $filename)  or die "Can't open $filename because $!\n";
open(OUT, ">$output") or die "Can't open $output because $!\n";

$counter = 0;
while ($line = <IN>) {
    if ($line =~ />/) {
        $line =~ s/>//g;
        chomp($line);
        $sequence = <IN>;
        chomp($sequence);
        $length = length($sequence);
        print OUT "$line\t$counter\t";
        print OUT $counter + $length - 1;
        print OUT "\n";
        $counter = $counter + $length + $spacer_size;
    }
}

close(IN);
close(OUT);

exit(0);

