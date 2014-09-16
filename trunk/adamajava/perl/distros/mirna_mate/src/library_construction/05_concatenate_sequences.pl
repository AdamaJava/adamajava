#!/usr/bin/perl

use Getopt::Std;

#get command line options

getopt("foh");
getopts("foh:");

#if insufficient options, print usage

if(! (($opt_f)&&($opt_o)&&($opt_h)))
        {
        print "\n\nUsage: $0\n\n";
        print "\tREQUIRED:\n";
        print "\t-f This program takes a fasta file as input - download from miRBase as appropriate\n";
        print "\t-o name of the output file\n";
	print "\t-h header for the concatenated file\n";
        die "\n\n";
        }

#set paramaters

$fasta_file=$opt_f;
$outputfile=$opt_o;
$header=$opt_h;

open(FILE, $fasta_file) or die "Can't open $fasta_file because $!\n";
open(CONCATENATED, ">$outputfile") or die "Can't open $outputfile because $!\n";

print CONCATENATED ">$header\n";

my $sequence = '';
my $count = 0;
my @array = ();
my $space_counter = 0;
my $line_width = 70;
while( <FILE> ) {
    chomp;
    $_ =~ s/^\s+//;
    $_ =~ s/\s+$//;
    if( $_ =~ /^>/ ){
	$count++;
	if($count > 1){
	    print CONCATENATED ".";
	    $space_counter++;
	    if($space_counter == $line_width){
		print CONCATENATED "\n";
		$space_counter = 0;
	    }
	}
    }
    # Check for a blank line
    elsif (/^(\s)*$/){
	$sequence = '.';
    }
    else{
	@array = ();
	@array = split(//, $_);
	for($i = 0; $i < length($_); $i++){
	    print CONCATENATED $array[$i];
	    $space_counter++;
	    if($space_counter == $line_width){
		print CONCATENATED "\n";
		$space_counter = 0;
	    }
	}
    }
}
print CONCATENATED "\n";
close(CONCATENATED);
close(FILE);
exit(0);

