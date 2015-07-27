sub parse_command_line {

    my($parameters, @ARGV) = @_;
    my $next_arg;

    while(scalar @ARGV > 0){
	$next_arg = shift(@ARGV);
	$parameters->{info} .= " $next_arg $ARGV[0]";
	if($next_arg eq "-t"){ $parameters->{type} = shift(@ARGV); }
	elsif($next_arg eq "-i"){ $parameters->{input_file} = shift(@ARGV); }
	elsif($next_arg eq "-g"){ $parameters->{genome_file} = shift(@ARGV); }
	elsif($next_arg eq "-f3"){ $parameters->{F3_outputfile} = shift(@ARGV); }
	elsif($next_arg eq "-r3"){ $parameters->{R3_outputfile} = shift(@ARGV); }
	else { print "Invalid argument: $next_arg"; usage(); }
    }
}


sub verify_input {

    my($parameters) = @_;

    # type
    if($parameters->{type} ne 'paired' && $parameters->{type} ne 'single'){
	print "\n  ERROR: type of input $parameters->{type} is invalid \n";
	usage();
    }

    # input_file
    if(-e $parameters->{input_file}){ 
#	print "  input_file = $parameters->{input_file} \n"; 
    } 
    else{ 
	print "\n  ERROR: input file $parameters->{input_file} does not exist \n"; 
	usage(); 
    }

    # genome_file
    if(-e $parameters->{genome_file}){ 
#	print "  genome_file = $parameters->{genome_file} \n"; 
    } 
    else{ 
	print "\n  ERROR: genome file $parameters->{genome_file} does not exist \n"; 
	usage(); 
    }
}


sub extract_tag_information {

    my($line, $parameters, $genome_length) = @_;

    my $location_info;
    my @values = ();
    my @tabs = ();

    if ($$line =~ /^>/){
	
	# Remove > from beginning of header
	my $header = substr($$line, 1, length($$line)-1);
	
	# Split tag information and locations of hits
	@tabs = ();
	@tabs = split(/,/, $header);
	$parameters->{id} = $tabs[0];

	if($parameters->{id} =~ 'F3'){
	    $parameters->{tag} = 'F3';
	}
	elsif($parameters->{id} =~ 'R3'){
	    $parameters->{tag} = 'R3';
	}   

	@{$parameters->{locations}} = ();
	@{$parameters->{errors}} = ();

	# Extract locations of hits and the number of errors
	for($i = 1; $i < scalar @tabs; $i++){
	    @values = ();
	    @values = split(/:/, $tabs[$i]);
	    $location_info = $values[0];
	    @values = ();
	    @values = split(/\./, $location_info);
	    $parameters->{locations}->[$i-1] = $values[0];
	    $parameters->{errors}->[$i-1] = $values[1];
	    $current_counter = 0;
	}

	# Get the original sequence from the next line
	$parameters->{sequence_original} = <FILE>;
	chomp $parameters->{sequence_original};
	$parameters->{tag_length} = length($parameters->{sequence_original}) - 1;

	for($i = 0; $i < scalar @{$parameters->{locations}}; $i++){
	    if($parameters->{locations}->[$i] < 0){
		$temp = $parameters->{locations}->[$i] * -1;
	    }
	    else{
		$temp = $parameters->{locations}->[$i];
	    }
	    
	    # top strand
	    if($parameters->{locations}->[$i] >= 0){
		if($temp > ($$genome_length - $parameters->{tag_length}) ){
		    splice(@{$parameters->{locations}}, $i, 1);
		    splice(@{$parameters->{errors}}, $i, 1);
		}
	    }
	    # reverse strand
	    else{
		if($temp < $parameters->{tag_length}){
		    splice(@{$parameters->{locations}}, $i, 1);
		    splice(@{$parameters->{errors}}, $i, 1);
		}
	    }	
	}
	$parameters->{number_of_locations} = scalar @{$parameters->{locations}};

	$parameters->{location} = undef;
	$parameters->{placement} = undef;
	# Determine which match to use
	if($parameters->{number_of_locations} == 1){
	    $parameters->{location} = 0;
	    $parameters->{placement} = 'unique';
	}
	elsif($parameters->{number_of_locations} > 1){
	    # Randomly select a match to the reference sequence
	    $parameters->{location} = int(rand($parameters->{number_of_locations}));
	    $parameters->{placement} = 'random';
	}

    }
}


sub extract_tag_information_mates {

    my($line, $parameters, $genome_length) = @_;

    my @values = ();

    @values = split(/\t/, $$line);

    $parameters->{mates}->{id} = $values[0];

    $parameters->{mates}->{F3}->{sequence_original} = $values[1];
    $parameters->{mates}->{R3}->{sequence_original} = $values[2];

    $parameters->{mates}->{F3}->{errors} = $values[3];
    $parameters->{mates}->{R3}->{errors} = $values[4];

    $parameters->{mates}->{F3}->{location} = $values[6];
    $parameters->{mates}->{R3}->{location} = $values[7];

    $parameters->{mates}->{category} = $values[8];

    $parameters->{mates}->{F3}->{tag_length} = length($parameters->{mates}->{F3}->{sequence_original}) - 1;
    $parameters->{mates}->{R3}->{tag_length} = length($parameters->{mates}->{R3}->{sequence_original}) - 1;
}


sub strand_type {

    my($parameters) = @_;
	    
    $parameters->{strand} = 'top';

    if($parameters->{locations}->[$parameters->{location}] < 0){
	$parameters->{locations}->[$parameters->{location}] = $parameters->{locations}->[$parameters->{location}] * -1;
	$parameters->{strand} = 'reverse';
    }
}


sub remove_tags_not_covering_the_genome{

    my($parameters, $genome_length) = @_;

    my $continue = 1;

    if( ( ($parameters->{strand} eq 'top') && ( $parameters->{locations}->[$parameters->{location}] > $$genome_length ) ) || 
	( ($parameters->{strand} eq 'reverse') && ( $parameters->{locations}->[$parameters->{location}] >= ($$genome_length + $parameters->{tag_length}) ) ) ){
	$continue = 0;
    }

    return $continue;
}



sub get_reference_sequence  {

    my($parameters, $genome) = @_;

    my($reference_sequence) = &reference_sequence($parameters->{strand}, \$$genome, $parameters->{locations}->[$parameters->{location}], $parameters->{tag_length});
    
    return $reference_sequence;
}


sub convert_reference_to_dibase_encoding_all_color {

    my($reference_sequence, $color, $parameters, $reference_array) = @_;

    my($reference) = &reference_to_dibase_all_color($reference_sequence, \%{$color}, $parameters->{strand}, $parameters->{tag_length});

    @{$reference_array} = ();
    @{$reference_array} = split(//, $reference);
    
    return $reference;
}


sub get_tag_sequence_all_color {

    my($parameters, $color, $reverse_color, $genome, $location, $sequence_array) = @_;

    my($sequence) = &tag_sequence_all_color($parameters->{strand}, $parameters->{sequence_original}, \%{$color}, \%{$reverse_color}, \$$genome, $location, $parameters->{tag_length});
    
    @{$sequence_array} = ();
    @{$sequence_array} = split(//, $sequence);

    return $sequence;
}


sub base_changes {

    my($base_changes, $parameters, $sequence_array, $reference_array) = @_;

    my $i;
    my $base_change_counter = 0;
    my($temp_1, $temp_2);

    @{$base_changes} = ();
    for($i = 0; $i < $parameters->{tag_length}; $i++){
	if($$sequence_array[$i] ne $$reference_array[$i]){
	    $temp_1 = $$sequence_array[$i];
	    $temp_2 = $$reference_array[$i];
	    $temp_1 = &convert_to_uppercase($temp_1);
	    $temp_2 = &convert_to_uppercase($temp_2);
	    if($temp_1 ne $temp_2){
		if($$sequence_array[$i] ne '.'){
		    if($parameters->{strand} eq 'top'){
			$$base_changes[$base_change_counter]{position} = $i;
		    }
		    else{
			$$base_changes[$base_change_counter]{position_revcomp} = $parameters->{tag_length} - $i - 1;
			$$base_changes[$base_change_counter]{position} = ( ( $parameters->{tag_length} - $i ) * -1 ) + 1;
		    }
		    $$base_changes[$base_change_counter]{sequence} = $$sequence_array[$i];
		    $$base_changes[$base_change_counter]{reference} = $$reference_array[$i];
		    $$base_changes[$base_change_counter]{label} = $$base_changes[$base_change_counter]{position} . "_";
		    $$base_changes[$base_change_counter]{label} .= $$sequence_array[$i];
		    $$base_changes[$base_change_counter]{label} .= $$reference_array[$i];
		    $base_change_counter++;
		}
	    }
	}
    }
 
    return(scalar @{$base_changes});
}


sub print_base_changes_all_color {

    my($sequence, $reference, $numberOfBaseChanges, $base_changes, $parameters, $CHANGES) = @_;

    my $i;

    print $CHANGES "\t", $parameters->{locations}->[$parameters->{location}], "\t", $parameters->{errors}->[$parameters->{location}], "\t";
    if($numberOfBaseChanges > 0){
	for($i = 0; $i < $numberOfBaseChanges; $i++){
	    print $CHANGES $$base_changes[$i]{label};
	    if( $i < ($numberOfBaseChanges - 1) ){
		print $CHANGES ",";
	    }
	}
    }
    else{
	print $CHANGES "-";
    }
    print $CHANGES "\n";
}


1;

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
