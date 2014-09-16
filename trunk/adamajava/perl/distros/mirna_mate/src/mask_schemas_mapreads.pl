#!/usr/bin/perl

eval 'exec /usr/bin/perl  -S $0 ${1+"$@"}'
    if 0; # not running under some shell
# Generate masked schema files

use strict;
use warnings;

my($i,$j);
my($next_arg, $schema_file, $tag_length, $number_of_errors, $pattern, $outputfile);

sub usage {
    print "\nUsage: $0 \n\n\t ";
    print "-s <schema_file> \n\t ";
    print "-t <tag_length> \n\t ";
    print "-e <number_of_errors> \n\t ";
    print "-p <pattern> \n\t ";
    print "-o <outputfile> \n\n";
    exit(1);
}
if(scalar(@ARGV) == 0){
    usage();
}

# Parse the command line
while(scalar @ARGV > 0){
    $next_arg = shift(@ARGV);
    if($next_arg eq "-s"){ $schema_file = shift(@ARGV); }
    elsif($next_arg eq "-t"){ $tag_length = shift(@ARGV); }
    elsif($next_arg eq "-e"){ $number_of_errors = shift(@ARGV); }
    elsif($next_arg eq "-p"){ $pattern = shift(@ARGV); }
    elsif($next_arg eq "-o"){ $outputfile = shift(@ARGV); }
    else { print "Invalid argument: $next_arg"; usage(); }
}

# Pattern
$pattern =~ s/^\s+//;
$pattern =~ s/\s+$//;
my $pattern_length = length($pattern);
if($pattern_length != $tag_length){
    print "  ERROR: pattern length does not equal tag length \n";
    exit(1);
}
$pattern = substr($pattern, 1, length($pattern) - 1);
my @positions = ();
@positions = split(//, $pattern);

# Schema
open( FILE, "< $schema_file" ) or die "  ERROR: schema $schema_file needed for masking does not exist or cannot be opened\n";
my @indicies = ();
my @index_positions = ();
my $count = 0;
my @values = ();
my $number_of_bases_in_index = 0;
while( <FILE> ) {
    chomp;
    if ($_ !~ /\#/){
	$indicies[$count] = $_;
	$count++;
    }
}

# Output
unless ( open(SCHEMA, ">$outputfile") ) {
    print "Cannot open file \"$outputfile\" to write to!!\n\n";
    exit;
}
for($i = 0; $i < (scalar @indicies); $i++){
    @index_positions = ();
    @index_positions = split(//, $indicies[$i]);
    $count = 0;
    for($j = 0; $j < (scalar @positions); $j++){
	if($positions[$j] == 1){
	    print SCHEMA $index_positions[$count];
	    $count++;
	}
	else{
	    print SCHEMA $positions[$j];
	}
    }
    print SCHEMA "\n";
}
close(SCHEMA);

exit;
