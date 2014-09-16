#! /usr/bin/perl -w

=head1 NAME

combine_donor_and_acceptor_sequences.pl

=head1 USAGE

 perl combine_donor_and_acceptor_sequences.pl
	-d filename of the donor sequences
	-a filename of the acceptor sequences
	-e prefix to remove from the donor header
	-b prefix to remove from the acceptor header
	-s comma separated exon part sizes (eg. 40,35,30,25,20)
	-m minimum size of intron (integer)
	-o prefix of the output fasta files

=head1 DESCRIPTION

Using a fasta file of donor exon sequences, and a fasta file of
acceptor exon sequences, create a fasta file containing the joined sequences
with N bases from each exon (where N is specified using the -s parameter).  If
multiple values for N are specified, one file will be created for each value.

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

getopt("dasobem");
getopts("dasobem:");

our ($opt_d, $opt_a, $opt_s, $opt_o, $opt_m, $opt_e, $opt_b);

if (!(($opt_d) && ($opt_a) && ($opt_s) && ($opt_o) && ($opt_m))) {
    print "\n\nUsage: $0\n\n";
    print "\tREQUIRED:\n";
    print "\t-d filename of the donor sequences\n";
    print "\t-a filename of the acceptor sequences\n";
    print "\t-s comma separated sizes of the exon junction libraries to make (eg. 40,35,30,25,20)\n";
    print "\t-m minimum size of intron (integer)\n";
    print "\t-o prefix of the output fasta files\n";
    print "\tOPTIONAL:\n";
    print "\t-e prefix to remove from the donor header\n";
    print "\t-b prefix to remove from the acceptor header\n";
    die "\n\n";
}

# set paramaters
$donor    = $opt_d;
$acceptor = $opt_a;
$d_prefix = $opt_e;
$a_prefix = $opt_b;
@sizes    = split(/\,/, $opt_s);
$output   = $opt_o;
$minimum  = $opt_m;

# read in donor and acceptor sequences into memory.
print STDOUT "Reading file: $donor ...\n";
open(DONOR, $donor) or die "Can't open $donor because $!\n";
while ($header = <DONOR>) {
    $sequence = <DONOR>;
    chomp($header);
    chomp($sequence);
    @name = split(/\s/, $header);
    $junctionID = $name[0];
    $junctionID =~ s/$d_prefix//;
    $junctionID =~ s/>//g;

    # add size filter in case this wasn't done before
    @name   = split(/\_/, $junctionID);
    $strand = pop(@name);
    $end    = pop(@name);
    $start  = pop(@name);
    if (($end - $start) > ($minimum - 1)) { $donors{$junctionID} = $sequence }
}
close(DONOR);

print STDOUT "Reading file: $acceptor ...\n";
open(ACCEPTOR, $acceptor) or die "Can't open $acceptor because $!\n";
while ($header = <ACCEPTOR>) {
    $sequence = <ACCEPTOR>;
    chomp($header);
    chomp($sequence);
    @name = split(/\s/, $header);
    $junctionID = $name[0];
    $junctionID =~ s/$a_prefix//;
    $junctionID =~ s/>//g;

    # add size filter in case this wasn't done before
    @name   = split(/\_/, $junctionID);
    $strand = pop(@name);
    $end    = pop(@name);
    $start  = pop(@name);
    if (($end - $start) > ($minimum - 1)) { $acceptors{$junctionID} = $sequence }
}
close(ACCEPTOR);

# open all the output files
foreach $size (@sizes) {
    $filename = $output . $size . '.fa';
    open($size, ">$filename") or die "Can't open $filename because $!\n";
}

foreach $junctionID (sort keys %donors) {

    # get the strand
    @name   = split(/\_/, $junctionID);
    $strand = pop(@name);

    # only output if there is both a donor and acceptor
    if (exists($acceptors{$junctionID})) {
	if ($strand eq '+') {$junction = $donors{$junctionID} . $acceptors{$junctionID}}
	    else {$junction = $acceptors{$junctionID} . $donors{$junctionID}};
        $length   = length($junction);
        $length   = $length / 2;
        foreach $size (@sizes) {
            $offset   = ($length - $size) / 2;
            $size     = $size * 2;
            $sequence = substr($junction, $offset, $size);
            $size     = $size / 2;
            print $size ">$junctionID\n$sequence\n";
        }
    }
}

# close all the output files
foreach $size (@sizes) {
    close($size);
}
exit(0);
