#!/usr/bin/perl

eval 'exec /usr/bin/perl  -S $0 ${1+"$@"}'
    if 0; # not running under some shell

# Find the base changes in a dibase encoded tag compared to a reference genome
# Outputs only tags with unique placement and with a color space number in the first position

use strict;
use warnings;

use FindBin;
use lib ("${FindBin::Bin}/../lib", "${FindBin::Bin}/../lib/perl");
use Cwd;

use QCMG::Editing::dna_subroutines;
use QCMG::Editing::working_subroutines;
use QCMG::Editing::tags2genome_dibase_all_color_subroutines;

my $parameters = {};

sub usage {

    print "\nUsage: $0 \n\n\t ";
    print "-t <type: paired or single> \n\t ";
    print "-i <input_file: mates file for paired tag data, match file for single tag data> \n\t ";
    print "-g <genome_file> \n\t ";
    print "-f3 <F3_outputfile> \n\t ";
    print "-r3 <R3_outputfile> \n\n";

    exit(1);
}
if(scalar(@ARGV) == 0){
    usage();
}

my ($date,$time) = &time_stamp();
$parameters->{info} = "# $date $time $0";

# Parse the Command Line
&parse_command_line($parameters, @ARGV);

# Verify Input
&verify_input($parameters);

# Color Code
my(%color) = &define_color_code;

# Reverse Color
my(%reverse_color) = &define_reverse_color;

# Genome
my($header, $genome, $genome_length) = &process_fasta_file($parameters->{genome_file});

# Output
unless ( open(CHANGES_F3, ">$parameters->{F3_outputfile}") ) {
    print "Cannot open file \"$parameters->{F3_outputfile}\" to write to!!\n\n";
    exit;
}
print CHANGES_F3 $parameters->{info}, "\n";
print CHANGES_F3 "# Cwd: " . getcwd() . "\n";
#unless ( open(CHANGES_R3, ">$parameters->{R3_outputfile}") ) {
#    print "Cannot open file \"$parameters->{R3_outputfile}\" to write to!!\n\n";
#    exit;
#}
#print CHANGES_R3 $parameters->{info}, "\n";
#print CHANGES_R3 "# Cwd: " . getcwd() . "\n";

# Tags
my($reference_sequence, $reference, $sequence);
my(@reference_array, @sequence_array) = ();
my @base_changes = ();
my $numberOfBaseChanges;
my $count = 0;
my $continue;

if($parameters->{type} eq 'single'){
    open( FILE, "< $parameters->{input_file}" ) or die "Can't open $parameters->{input_file} : $!";
    while( <FILE> ) {
	
	chomp;
	
	if (/^\#/){
	    if( exists $parameters->{F3_outputfile} ){
		print CHANGES_F3 $_, "\n";
	    }
	    if( exists $parameters->{R3_outputfile} ){
		print CHANGES_R3 $_, "\n";
	    }
	}
	else{
	    
	    # Get Tag Information
	    &extract_tag_information(\$_, $parameters, \$genome_length);
	    
	    if($parameters->{number_of_locations} == 1){
		
		# Get Strand
		&strand_type($parameters);
		
		# Remove tags not covering the genome
		$continue = &remove_tags_not_covering_the_genome($parameters, \$genome_length);
		
		if($continue == 1){
		    
		    # Determine Reference Sequence
		    ($reference_sequence) = &reference_sequence_all_color($parameters->{strand}, \$genome, $parameters->{locations}->[0], $parameters->{tag_length});
		    
		    # Convert $reference to di-base encoding
		    ($reference) = &convert_reference_to_dibase_encoding_all_color($reference_sequence, \%color, $parameters, \@reference_array);
		    
		    # Determine Tag Sequence
		    ($sequence) = &get_tag_sequence_all_color($parameters, \%color, \%reverse_color, \$genome, $parameters->{locations}->[0], \@sequence_array);
		    
		    # Print id, strand, sequence and reference
		    if( exists $parameters->{F3_outputfile} && $parameters->{tag} eq 'F3' ){
			print CHANGES_F3 $parameters->{id}, "\t", $parameters->{strand}, "\t", $sequence, "\t", $reference, "\t", $reference_sequence;
		    }
		    elsif( exists $parameters->{R3_outputfile} && $parameters->{tag} eq 'R3' ){
			print CHANGES_R3 $parameters->{id}, "\t", $parameters->{strand}, "\t", $sequence, "\t", $reference, "\t", $reference_sequence;
		    }
		    
		    # Get Base Changes
		    ($numberOfBaseChanges) = &base_changes(\@base_changes, $parameters, \@sequence_array, \@reference_array);
		    if( exists $parameters->{F3_outputfile} && $parameters->{tag} eq 'F3' ){
			&print_base_changes_all_color($sequence, $reference, $numberOfBaseChanges, \@base_changes, $parameters, \*CHANGES_F3);
		    }
		    elsif( exists $parameters->{R3_outputfile} && $parameters->{tag} eq 'R3' ){
			&print_base_changes_all_color($sequence, $reference, $numberOfBaseChanges, \@base_changes, $parameters, \*CHANGES_R3);
		    }
		}
		
	    }
	}
	
	$count++;
	if($count % 1000000 == 0){
	    print "  $count\n";
	}
    }
}
elsif($parameters->{type} eq 'paired'){

    open( FILE, "< $parameters->{input_file}" ) or die "Can't open $parameters->{input_file} : $!";
    while( <FILE> ) {
	
	chomp;
	
	if (/^\#/){
	    if( exists $parameters->{F3_outputfile} ){
		print CHANGES_F3 $_, "\n";
	    }
	    if( exists $parameters->{R3_outputfile} ){
		print CHANGES_R3 $_, "\n";
	    }
	}
	else{
	    
	    # Get Tag Information
	    &extract_tag_information_mates(\$_, $parameters);

	    if($parameters->{mates}->{category} eq 'AAA'){

		# F3
		$parameters->{id} = $parameters->{mates}->{id} . "_F3";
		$parameters->{tag} = 'F3';
		@{$parameters->{locations}} = ();
		@{$parameters->{errors}} = ();
		$parameters->{locations}->[0] = $parameters->{mates}->{F3}->{location};
		$parameters->{errors}->[0] = $parameters->{mates}->{F3}->{errors};
		$parameters->{sequence_original} = $parameters->{mates}->{F3}->{sequence_original};
		$parameters->{tag_length} = $parameters->{mates}->{F3}->{tag_length};
		$parameters->{number_of_locations} = 1;
		$parameters->{location} = 0;
		$parameters->{placement} = 'mates';

		# Get Strand
		&strand_type($parameters);

		# Determine Reference Sequence
		($reference_sequence) = &reference_sequence_all_color($parameters->{strand}, \$genome, $parameters->{locations}->[0], $parameters->{tag_length});
		    
		# Convert $reference to di-base encoding
		($reference) = &convert_reference_to_dibase_encoding_all_color($reference_sequence, \%color, $parameters, \@reference_array);
		
		# Determine Tag Sequence
		($sequence) = &get_tag_sequence_all_color($parameters, \%color, \%reverse_color, \$genome, $parameters->{locations}->[0], \@sequence_array);
		
		# Print id, strand, sequence and reference
		if( exists $parameters->{F3_outputfile} && $parameters->{tag} eq 'F3' ){
		    print CHANGES_F3 $parameters->{id}, "\t", $parameters->{strand}, "\t", $sequence, "\t", $reference, "\t", $reference_sequence;
		}
		elsif( exists $parameters->{R3_outputfile} && $parameters->{tag} eq 'R3' ){
		    print CHANGES_R3 $parameters->{id}, "\t", $parameters->{strand}, "\t", $sequence, "\t", $reference, "\t", $reference_sequence;
		}
		
		# Get Base Changes
		($numberOfBaseChanges) = &base_changes(\@base_changes, $parameters, \@sequence_array, \@reference_array);
		if( exists $parameters->{F3_outputfile} && $parameters->{tag} eq 'F3' ){
		    &print_base_changes_all_color($sequence, $reference, $numberOfBaseChanges, \@base_changes, $parameters, \*CHANGES_F3);
		}
		elsif( exists $parameters->{R3_outputfile} && $parameters->{tag} eq 'R3' ){
		    &print_base_changes_all_color($sequence, $reference, $numberOfBaseChanges, \@base_changes, $parameters, \*CHANGES_R3);
		}


		# R3
		$parameters->{id} = $parameters->{mates}->{id} . "_R3";
		$parameters->{tag} = 'R3';
		@{$parameters->{locations}} = ();
		@{$parameters->{errors}} = ();
		$parameters->{locations}->[0] = $parameters->{mates}->{R3}->{location};
		$parameters->{errors}->[0] = $parameters->{mates}->{R3}->{errors};
		$parameters->{sequence_original} = $parameters->{mates}->{R3}->{sequence_original};
		$parameters->{tag_length} = $parameters->{mates}->{R3}->{tag_length};
		$parameters->{number_of_locations} = 1;
		$parameters->{location} = 0;
		$parameters->{placement} = 'mates';

		# Get Strand
		&strand_type($parameters);

		# Determine Reference Sequence
		($reference_sequence) = &reference_sequence_all_color($parameters->{strand}, \$genome, $parameters->{locations}->[0], $parameters->{tag_length});
		    
		# Convert $reference to di-base encoding
		($reference) = &convert_reference_to_dibase_encoding_all_color($reference_sequence, \%color, $parameters, \@reference_array);
		
		# Determine Tag Sequence
		($sequence) = &get_tag_sequence_all_color($parameters, \%color, \%reverse_color, \$genome, $parameters->{locations}->[0], \@sequence_array);
		
		# Print id, strand, sequence and reference
		if( exists $parameters->{F3_outputfile} && $parameters->{tag} eq 'F3' ){
		    print CHANGES_F3 $parameters->{id}, "\t", $parameters->{strand}, "\t", $sequence, "\t", $reference, "\t", $reference_sequence;
		}
		elsif( exists $parameters->{R3_outputfile} && $parameters->{tag} eq 'R3' ){
		    print CHANGES_R3 $parameters->{id}, "\t", $parameters->{strand}, "\t", $sequence, "\t", $reference, "\t", $reference_sequence;
		}
		
		# Get Base Changes
		($numberOfBaseChanges) = &base_changes(\@base_changes, $parameters, \@sequence_array, \@reference_array);
		if( exists $parameters->{F3_outputfile} && $parameters->{tag} eq 'F3' ){
		    &print_base_changes_all_color($sequence, $reference, $numberOfBaseChanges, \@base_changes, $parameters, \*CHANGES_F3);
		}
		elsif( exists $parameters->{R3_outputfile} && $parameters->{tag} eq 'R3' ){
		    &print_base_changes_all_color($sequence, $reference, $numberOfBaseChanges, \@base_changes, $parameters, \*CHANGES_R3);
		}
	    }

	}
	
	$count++;
	if($count % 1000000 == 0){
	    print "  $count\n";
	}	
    }
}
close(CHANGES_F3);
close(CHANGES_R3);

exit;
