#! /usr/bin/perl -w

=head1 NAME

create_non_redundant_intron_coordinates.tidy.pl

=head1 USAGE

 perl create_non_redundant_intron_coordinates.pl
    -p Common filename text as input files. (eg. BED will be processed as *BED*)
	-s the size of the exon-junction library you want to make. Integer. (eg. 40)
	-o the name of the prefix for the BED file outputs (a donor BED file and an acceptor BED file)
	-f name of the chromosome sizes file (output of chrom_sizes.pl)
	-m minimum size of introns
	[-t chromosome name translation file]

=head1 DESCRIPTION

Create a BED file containing non-redundant coordinates of exon-exon junctions from a set of
gene models (input using one or more BED files).  Optionally use the tab delimited file (-t) to
translate chromosome names from the galaxy nomenclature to the reference genome name.  For example,
you may want genes on the unplaced contig chrUn_gl000242 as listed in galaxy gene output to appear in the
non redundant intron coordinate bed file with the name GL000242.1 as it is described in 
GRCh37_ICGC_standard_v2 fasta files.

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
use strict;

# get command line options
getopt("psofmt");
getopts("psoftm:");

our ($opt_p, $opt_s, $opt_o, $opt_f, $opt_m, $opt_t);

# set paramaters
my $prefix  = $opt_p;
my $size    = $opt_s;
my $output  = $opt_o;
my $chrom   = $opt_f;
my $minimum = $opt_m;
my $transFile = $opt_t;
my %sizes = ();
my %introns = ();
my %unknownChrs = ();


# if insufficient options, print usage
if (!(($prefix) && ($size) && ($output) && ($chrom) && ($minimum))) {
    print "\n\nUsage: $0\n\n";
    print "\tREQUIRED:\n";
    print "\t-p Common filename text as input files. No wildcards necessary. (eg. BED will be processed as *BED*)\n";
    print "\t-s the size of the exon-junction library you want to make. Integer. (eg. 40)\n";
    print "\t-o the name of the prefix for the BED file outputs (a donor BED file and an acceptor BED file)\n";
    print "\t-f name of the chromosome sizes file (output of chrom_sizes.pl)\n";
    print "\t-m minimum size of introns\n";
    print "\tOPTIONAL:\n";
    print "\t-t name of tabbed file to translate chromosome names from the bed file to reference fasta file names.\n";
    die "\n\n";
}



# read in the chomosome sizes into a hash to enable error checking down the track 
# (don't want to get junctions that fall off the end of chromosomes/scaffolds)
print STDOUT "Reading the file $chrom...\n";
open(IN, $chrom) or die "Can't open $chrom because $!\n";
while (my $line = <IN>) {
    chomp($line);
    my ($chr, $size_of_chr) = split(/\t/, $line);
    $sizes{$chr} = $size_of_chr;
}
close(IN);

# if required, read in the chromosome names to be translated:
my %chr_lookup = ();
if (defined($transFile)) {
    open(TRANSLATION, $transFile) or die "Failed to read from $transFile: $!\n";
    while (<TRANSLATION>) {
        chomp;
        my ($ref_chr_name, $bed_chr_name) = split(/\t/, $_);
        $chr_lookup{$bed_chr_name} = $ref_chr_name;
    }
    close TRANSLATION;
}
else {
    foreach my $chr (keys %sizes) {
        $chr_lookup{$chr} = $chr;
    }
}


# read in all the BED files and create a non-redundant set
while (my $filename = glob("*$prefix*")) {
    print STDOUT "Reading file $filename...\n";
    open(IN, $filename) or die "Can't open $filename because $!\n";
    while (my $line = <IN>) {
        chomp($line);
        my ($chr, $start, $end, $name, $score, $strand) = split(/\t/, $line);
        $end--;
        if (($end - $start) > ($minimum - 1)) {
            unless (defined($chr_lookup{$chr})) { $unknownChrs{$chr}++; next; }
            $name = $chr_lookup{$chr} . '_' . $start . '_' . $end . '_' . $strand;
            $introns{$name}++;
        }
    }
    close(IN);
}

print STDERR "Got ".(keys %introns)." introns\n";

if (keys %unknownChrs > 0) {
    foreach my $chr (keys %unknownChrs) {
        print STDERR "WARNING: chromosome $chr ($unknownChrs{$chr} introns) not known in reference files - all introns skipped.\n";
    }
}

# write out the donor and acceptor files including track lines
print STDOUT "Writing donor and acceptor files...\n";
open(DONOR, ">$output.donor.$size.BED") or die "Can't open $output.donor.$size.BED because $!\n";
print DONOR "track name=donor description=donor\n";
open(ACCEPTOR, ">$output.acceptor.$size.BED")
  or die "Can't open $output.acceptor.$size.BED because $!\n";
print ACCEPTOR "track name=acceptor description=acceptor\n";
my $score = 0;

foreach my $intron (sort keys %introns) {

    # account for the unassembled fractions
    my @name         = split(/\_/, $intron);
    my $strand       = pop(@name);
    my $end          = pop(@name);
    my $start        = pop(@name);
    my $chr          = join("_", @name);
    
    my $donor_start  = $start - $size;
    my $acceptor_end = $end + $size;

    # error checking to make sure coords don't fall off the ends
    if (($donor_start < 0) || ($acceptor_end > $sizes{$chr})) {
        print "skipped [$intron], it must have extended past extremities of the chromosome.\n"; 
    }
    else {
        print DONOR "$chr\t$donor_start\t$start\t$intron\t$score\t$strand\n";
        print ACCEPTOR "$chr\t$end\t$acceptor_end\t$intron\t$score\t$strand\n";
    }
}

close(DONOR);
close(ACCEPTOR);

exit(0);
