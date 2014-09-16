#!/usr/bin/perl -w

eval 'exec /usr/bin/perl  -S $0 ${1+"$@"}'
    if 0; # not running under some shell
# Wrapper for mapreads to match tags to a genome

use strict;
use warnings;

BEGIN {
    # This forces FindBin to find the script in the current working directory
    $ENV{'PATH'} = ".:" . $ENV{'PATH'};
}


#hack for compatibility, should not be necessary for recent mapreads installs
use lib '/data/matching/perl';
use lib '/data/matching/perl/Path-Class-0.16/lib';

use FindBin;
use File::Basename;
use File::Temp qw/tempfile tempdir/;
use Path::Class qw/dir file/;
use Cwd;

my $schema_dir = "${FindBin::Bin}/schemas";
$ENV{'PATH'} = ${FindBin::Bin} . ":" . $ENV{'PATH'};

my $parameters = {};

sub usage {
    print "\nUsage: $0 \n\n\t ";

    print "REQUIRED \n\t ";
    print "-g <genome_file> \n\t ";
    print "-program <mapreads_program> \n\t ";
    print "-r <reads_file> \n\t ";
    print "-d <output_directory> \n\t ";
    print "-t <tag_length> \n\t ";
    print "-e <number_of_errors> \n\n\t ";
    
    print "OPTIONAL \n\t ";
    print "-s <schema_file> \n\t ";
    print "-p <pattern: defaults to all 1's> \n\t ";
    print "-start <start: defaults to 0> \n\t ";
    print "-a <count adjacent errors as 1: 0 = no : 1 = valid adjacent errors : 2 = all adjacent errors : defaults to 0> \n\t ";
    print "-z <maximum number of hits per tag: defaults to 1000> \n\t ";
    print "-ref <output the reference sequence of hits: 0 = no : 1 = yes : defaults to 0> \n\t ";
    print "-multi <output results in multi-entry format> \n\n";

    exit(1);
}
if(scalar(@ARGV) == 0){
    usage();
}

# Parse the Command Line
&parse_command_line($parameters, @ARGV);

# Verify Input
&verify_input($parameters);

# Create Name of Output File
my($match_file) = &output_file_name($parameters);
#print "\t output file name: $match_file \t";

# Get Schema File
if( (not defined $parameters->{schema_file}) || (defined $parameters->{pattern}) ) {
    &get_schema($parameters);
}
#print "schema file: $parameters->{schema_file}";
# Set Matrix Parameter
my($matrix_parameter) = &set_matrix_parameter($parameters);
#print "matric parameter: $matrix_parameter";

# Run Mapreads
&mapreads($parameters, $matrix_parameter);

# Remove the Masked Schema
if(defined $parameters->{pattern}){
    unlink($parameters->{schema_file});
}

exit;


sub parse_command_line {

    my($parameters, @ARGV) = @_;

    my $next_arg;
    $parameters->{schema_file} = undef;
    $parameters->{pattern} = undef;
    $parameters->{start} = 0;
    $parameters->{adj_errors} = 0;
    $parameters->{maximum_hits} = 1000;
    $parameters->{reference} = 0;
    $parameters->{multi_entry} = 'no';

    while(scalar @ARGV > 0){
	$next_arg = shift(@ARGV);
	if($next_arg eq "-g"){ $parameters->{genome_file} = shift(@ARGV); }
	elsif($next_arg eq "-r"){ $parameters->{reads_file} = shift(@ARGV); }
	elsif($next_arg eq "-program"){ $parameters->{mapreads_program} = shift(@ARGV); }
	elsif($next_arg eq "-d"){ $parameters->{output_directory} = shift(@ARGV); }
	elsif($next_arg eq "-t"){ $parameters->{tag_length} = shift(@ARGV); }
	elsif($next_arg eq "-e"){ $parameters->{number_of_errors} = shift(@ARGV); }
	elsif($next_arg eq "-s"){ $parameters->{schema_file} = shift(@ARGV); }
	elsif($next_arg eq "-p"){ $parameters->{pattern} = shift(@ARGV); }
	elsif($next_arg eq "-start"){ $parameters->{start} = shift(@ARGV); }
	elsif($next_arg eq "-a"){ $parameters->{adj_errors} = shift(@ARGV); }
	elsif($next_arg eq "-z"){ $parameters->{maximum_hits} = shift(@ARGV); }
	elsif($next_arg eq "-ref"){ $parameters->{reference} = shift(@ARGV); }
	elsif($next_arg eq "-multi"){ $parameters->{multi_entry} = 'yes'; }
	else { print "Invalid argument: $next_arg"; usage(); }
    }
}


sub verify_input {

    my($parameters) = @_;

    my($i, $temp);
    my @values = ();

    print "\n";

    my $cwd = getcwd();

    # genome_file
#    $parameters->{genome_file} = file($parameters->{genome_file})->absolute($cwd);
    if(-e $parameters->{genome_file}){ 
	print "  genome_file = $parameters->{genome_file} \n"; 
    } 
    else{ 
	print "\n  ERROR: genome file $parameters->{genome_file} does not exist \n"; 
	usage(); 
    }

    # reads_file
#    $parameters->{reads_file} = file($parameters->{reads_file})->absolute($cwd);
    if(-e $parameters->{reads_file}){ 
	print "  reads_file = $parameters->{reads_file} \n"; 
    } 
    else{
	print "\n  ERROR: reads file $parameters->{reads_file} does not exist \n"; 
	usage(); 
    }

    # output_directory
    # $parameters->{output_directory} = dir($parameters->{output_directory})->absolute($cwd);

    if(-e $parameters->{output_directory} && -d $parameters->{output_directory}){ 
	print "  output_directory = $parameters->{output_directory} \n"; 
    } 
    else{
	$parameters->{output_directory}->mkpath(1,02775);
	print "  output_directory $parameters->{output_directory} did not exist so it was created \n"; 
    }

    # tag_length
    if(defined $parameters->{tag_length}){
	if ($parameters->{tag_length} =~ /^-?\d/){
	    if($parameters->{tag_length} > 0){ 
		print "  tag_length = $parameters->{tag_length} \n"; 
	    } 
	    else{
		print "\n  ERROR: invalid tag_length \n"; 
		usage(); 
	    }
	} 
	else{ 
	    print "\n  ERROR: invalid tag_length \n"; 
	    usage(); 
	}
    }
    else{ 
	print "\n  ERROR: tag length not defined \n"; 
	usage(); 
    }

    # number_of_errors
    if(defined $parameters->{number_of_errors}){
	if ($parameters->{number_of_errors} =~ /^-?\d/){
	    if($parameters->{number_of_errors} >= 0){ 
		print "  number_of_errors = $parameters->{number_of_errors} \n"; 
	    } 
	    else{
		print "\n  ERROR: invalid number_of_errors \n"; 
		usage(); 
	    }
	} 
	else{ 
	    print "\n  ERROR: invalid number_of_errors \n"; 
	    usage(); 
	}
    }
    else{ 
	print "\n  ERROR: number of errors not defined \n"; 
	usage(); 
    }

    # schema_file
    if(defined $parameters->{schema_file}){
	if(-e $parameters->{schema_file}){ 
	    print "  schema_file = $parameters->{schema_file} \n"; 
	} 
	else{ 
	    print "\n  ERROR: schema file $parameters->{schema_file} does not exist \n"; 
	    usage(); 
	}
    }

    # pattern
    if(defined $parameters->{pattern}){
	if(length($parameters->{pattern}) == $parameters->{tag_length}){
	    print "  pattern = $parameters->{pattern} \n";
	}
	else{
	    print "\n  ERROR: pattern length does not equal tag length \n";
	}
    }

    # start
    if ($parameters->{start} =~ /^-?\d/){
	if($parameters->{start} >= 0){ 
	    print "  start = $parameters->{start} \n"; 
	} 
	else{
	    print "  \nERROR: invalid start \n"; 
	    usage(); 
	}
    } 
    else{ 
	print "  \nERROR: invalid start \n"; 
	usage(); 
    }

    # adj_errors
    if ($parameters->{adj_errors} =~ /^-?\d/){
	if($parameters->{adj_errors} >= 0 && $parameters->{adj_errors} <= 2){ 
	    print "  adj_errors = $parameters->{adj_errors} \n"; 
	} 
	else{
	    print "\n  ERROR: invalid adj_errors \n"; 
	    usage(); 
	}
    } 
    else{ 
	print "\n  ERROR: invalid adj_errors \n"; 
	usage(); 
    }

    # maximum_hits
    if ($parameters->{maximum_hits} =~ /^-?\d/){
	if($parameters->{maximum_hits} >= 0){ 
	    print "  maximum_hits = $parameters->{maximum_hits} \n"; 
	} 
	else{
	    print "  \nERROR: invalid maximum_hits \n"; 
	    usage(); 
	}
    } 
    else{ 
	print "  \nERROR: invalid maximum_hits \n"; 
	usage(); 
    }

    # reference
    if ($parameters->{reference} =~ /^-?\d/){
	if($parameters->{reference} == 0 || $parameters->{reference} == 1){ 
	    print "  reference option = $parameters->{reference} \n"; 
	} 
	else{
	    print "\n  ERROR: invalid reference option \n"; 
	    usage(); 
	}
    } 
    else{ 
	print "\n  ERROR: invalid reference option \n"; 
	usage(); 
    }

    print "\n";

    if(defined $parameters->{schema_file} && defined $parameters->{pattern}){
	print "WARNING: You've given a schema file and a pattern\n";
	print "         Your pattern will be used but your schema will be ignored\n";
	print "         This will be improved in a later version\n";
    }
}


sub output_file_name {

    my($parameters) = @_;
    
    my $str = $parameters->{genome_file};
    $str = substr( $str, rindex($str, '/')+1);
    my ($chr,$blash )= split(/\./, $str );
    $chr =~ s/Chr/chr/g;
    
    my $match_file = $chr. "." . basename($parameters->{reads_file}) . ".ma";
    $match_file = "$parameters->{output_directory}" . $match_file . ".$parameters->{tag_length}";
    $match_file .= ".$parameters->{number_of_errors}";
    if($parameters->{adj_errors} == 1){
	$match_file .= ".adj_valid"; 
    }
    elsif($parameters->{adj_errors} == 2){
	$match_file .= ".adj_all";
    }

    return $match_file;
}


sub get_schema {

    my($parameters) = @_;
    
    my @positions = ();
    my($i, $standard_length, $standard_schema_file, $fh, $filename, $rc);


    if(not defined $parameters->{pattern}){ # use standard schema
	
	$parameters->{schema_file} = "$schema_dir/schema_$parameters->{tag_length}" . "_$parameters->{number_of_errors}";
	if(-e $parameters->{schema_file}){ }
	else{
	    print "  ERROR: $parameters->{schema_file} does not exist\n\n";
	    exit(1);
	}
	
	
    }
    else{ # generate masked schema
	# Count number of 1's in pattern to determine which standard schema to use to make masked schema
	@positions = split(//, $parameters->{pattern});
	$standard_length = 0;
	for($i = 0; $i < (scalar @positions); $i++){
	    if($positions[$i] == 1){
		$standard_length++;
	    }
	}
	if($positions[0] == 0){
	    $standard_length++;
	}
	$standard_schema_file = "$schema_dir/schema_$standard_length" . "_$parameters->{number_of_errors}";
	

	# Create a temporary file name for the masked schema
	my $cwd = getcwd();
	($fh, $filename) = tempfile( "$cwd/schema_XXXXX" );
 	close $fh;
	$parameters->{schema_file} = $filename;

	# Generate the masked schema
	$rc = system("mask_schemas_mapreads.pl -s $standard_schema_file -t $parameters->{tag_length} -e $parameters->{number_of_errors} -p $parameters->{pattern} -o $parameters->{schema_file}");
	if($rc != 0){
	    print "\n  ERROR: Generation of masked schema failed\n\n";
	    exit(1);
	}
    }

}


# Set X option to empty if there is no mask otherwise set it to X=pattern
sub set_matrix_parameter {

    my($parameters) = @_;

    my $matrix_parameter = '';
    if(defined $parameters->{pattern}){
	$matrix_parameter = "X=" . $parameters->{pattern};
    }

    return $matrix_parameter;
}


sub mapreads {

    my($parameters, $matrix_parameter) = @_;

    my $format_parameter = "I=0";
    if($parameters->{multi_entry} eq 'yes'){
	$format_parameter = "I=1";
    }

    my $scratchdir = undef;
    my $cwd = getcwd();
    if(-d "/data/scratch" && -w _) {
	# Have a local scratch directory
	$scratchdir = tempdir( "matching.XXXXXXX", 
			       CLEANUP => 1,
			       DIR => "/data/scratch" );
    }
    if($scratchdir) {
	print "   using scratch directory '$scratchdir'\n";	
	chdir($scratchdir);
    } else {
	print "   [WARNING]: Unable to find scratch directory.\n";
#   	die "Refusing to match in a home directory\n" if ($cwd =~ m{^/home});
	print "      *** mapreads will run in current directory ('$cwd').\n";
	print "      *** It may run very slowly.";
    }
     
    
    my $comm = "$parameters->{mapreads_program} $parameters->{reads_file} $parameters->{genome_file} M=$parameters->{number_of_errors} S=0 L=$parameters->{tag_length} ${matrix_parameter} T=$parameters->{schema_file} A=$parameters->{adj_errors} O=$parameters->{start} Z=$parameters->{maximum_hits} R=$parameters->{reference} $format_parameter > $match_file";
    print "\t$comm\n";
    my $rc = system($comm );

    if(defined $scratchdir && defined $cwd) {
	chdir($cwd) || warn "Unable to restore CWD: '$cwd'\n";
    }

    if($rc != 0){
	print "\n  ERROR: mapreads failed\n\n";
	exit(1);
    }
    else{
	print "\n[SUCCESS]: map to $parameters->{genome_file} ( $parameters->{tag_length}mers)\n";
	my $f = "$match_file.success";
	open(FF,">$f");
	print FF "success on $match_file\n";
	close(FF);
    }
    print "\n";
}
