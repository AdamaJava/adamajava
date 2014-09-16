#! /usr/bin/perl -w


=head1 NAME

assess_junctions_for_directionality.pl

=head1 USAGE

 perl assess_junctions_for_directionality.pl
	-s name of BED file containing sense matches
	-a name of BED file containing antisense matches
	-o name of outfile

=head1 DESCRIPTION

This programs takes the junction BED files and outputs the directionality of the library
and an individual list of the proportions of antisense to sense junctions. Junctions are 
only included if there is more than one tag supporting it. Additionally, antisense junctions
are only included if there is a corresponding sense junction. 

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

#get command line options

getopt("sao");
getopts("sao:");

#if insufficient options, print usage

if(! ($opt_s && $opt_a && $opt_o))
        {
        print "\n\nUsage: $0\n\n";
        print "\tREQUIRED:\n";
        print "\t-s name of BED file containing sense matches\n";
        print "\t-a name of BED file containing antisense matches\n";
        print "\t-o name of outfile\n";
        die "\n\n";
        }

#set paramaters

$sensefile=$opt_s;
$antisensefile=$opt_a;
$outfile=$opt_o;


open(SENSE, $sensefile) or die "Can't open $sensefile because $!\n";
open(ANTISENSE, $antisensefile) or die "Can't open $antisensefile because $!\n";
open(OUT, ">$outfile") or die "Can't open $outfile because $!\n";

#read sense
%sense=();
while($line=<SENSE>) {
	next if $line=~/track/;
	chomp($line);
	@data=split(/\t/, $line);	
	$sense{$data[3]}=$data[4];
}

close(SENSE);

%antisense=();
while($line=<ANTISENSE>) {
	next if $line=~/track/;
    chomp($line);
	@data=split(/\t/, $line);
	$antisense{$data[3]}=$data[4];
}

close(ANTISENSE);

$s_total=0;
$as_total=0;
print OUT "Junction\tSense\tAntisense\tProportion\n";

foreach $key (keys %sense) {
	$as=0;
	if ($sense{$key}>1) {$s_total=$s_total+$sense{$key}; $s=$sense{$key}} else {$s=0};
	if ((exists($antisense{$key}))&&($antisense{$key}>1)&&($sense{$key}>1)) {$as_total=$as_total+$sense{$key}; $as=$sense{$key}}
	if (($s+$as)>0) {$p=$as/($s+$as);  print OUT "$key\t$s\t$as\t$p\n"};
}
close(OUT);
	
$directionality=1-($as_total/($as_total+$s_total));
$directionality=$directionality*100;

print STDOUT "Directionality: $directionality %\n\n\n";

exit(0);
