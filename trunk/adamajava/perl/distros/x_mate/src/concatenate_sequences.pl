#! /usr/bin/perl

=head1 NAME

concatenate_sequences.pl

=head1 USAGE

 perl concatenate_sequences.pl -f <fasta_file> -o <outputfile> -h <header>

=head1 DESCRIPTION

This script takes a multi-entry fasta file and generates one sequence 
string with a '.' separating the sequences

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

use strict;
use warnings;

my $i;
my ($next_arg, $fasta_file, $outputfile, $header, $spacer_size);

sub usage {
    print "\nUsage: $0 -f <fasta_file> -o <outputfile> -h <header> -s <spacerSize>\n\n";
    exit(1);
}
if (scalar(@ARGV) == 0) {
    usage();
}

#  Parse the command line
while (scalar @ARGV > 0) {
    $next_arg = shift(@ARGV);
    if    ($next_arg eq "-f") { $fasta_file  = shift(@ARGV); }
    elsif ($next_arg eq "-o") { $outputfile  = shift(@ARGV); }
    elsif ($next_arg eq "-h") { $header      = shift(@ARGV); }
    elsif ($next_arg eq "-s") { $spacer_size = shift(@ARGV); }
    else                      { print "Invalid argument: $next_arg"; usage(); }
}

#  Output
unless (open(CONCATENATED, ">$outputfile")) {
    print "Cannot open file \"$outputfile\" to write to!!\n\n";
    exit;
}
print CONCATENATED ">$header\n";

#  Fasta File
open(FILE, "< $fasta_file") or die "Can't open $fasta_file : $!";
my $sequence      = '';
my $count         = 0;
my @array         = ();
my $space_counter = 0;
my $line_width    = 70;

while (<FILE>) {
    chomp;
    $_ =~ s/^\s+//;
    $_ =~ s/\s+$//;
    if ($_ =~ /^>/) {
        $count++;
        if ($count > 1) {
            for (my $i = 0; $i < $spacer_size; $i++) {
                print CONCATENATED ".";
                $space_counter++;
                if ($space_counter == $line_width) {
                    print CONCATENATED "\n";
                    $space_counter = 0;
                }
            }
        }
    }

    #  Check for a blank line
    elsif (/^(\s)*$/) {
        $sequence = '.';
    }
    else {
        @array = ();
        @array = split(//, $_);
        for ($i = 0 ; $i < length($_) ; $i++) {
            print CONCATENATED $array[$i];
            $space_counter++;
            if ($space_counter == $line_width) {
                print CONCATENATED "\n";
                $space_counter = 0;
            }
        }
    }
}
print CONCATENATED "\n";
close CONCATENATED;

exit;
