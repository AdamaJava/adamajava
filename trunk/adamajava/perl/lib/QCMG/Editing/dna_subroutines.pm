# revcomp
# complement
# convert_to_dibase
# convert_to_uppercase
# process_fasta_file
# length_of_sequence
# define_color_code
# define_reverse_color
# reference_sequence
# reference_sequence_all_color
# reference_to_dibase
# reference_to_dibase_all_color
# tag_sequence
# tag_sequence_all_color
# print_sequence
# define_snp_changes

# Reverse complement a given sequence
sub revcomp {
    my($sequence) = @_;
    my($return_value) = "";

    for (my $i = length($sequence) - 1; $i >= 0; $i--) {
	my $char = substr($sequence, $i, 1);

	if($char eq "A") {
	    $return_value .= "T";
	} elsif ($char eq "C") {
	    $return_value .= "G";
	} elsif ($char eq "G") {
	    $return_value .= "C";
	} elsif ($char eq "T") {
	    $return_value .= "A";
	} elsif ($char eq "a") {
	    $return_value .= "t";
	} elsif ($char eq "c") {
	    $return_value .= "g";
	} elsif ($char eq "g") {
	    $return_value .= "c";
	} elsif ($char eq "t") {
	    $return_value .= "a";
	} elsif ($char eq "N") {
	    $return_value .= "N";
	} elsif ($char eq "n") {
	    $return_value .= "n";
	} elsif ($char eq ".") {
	    $return_value .= ".";
	} else {
	    print(STDERR "Unknown DNA character ($char).");
	    exit(1);
	}
    }

    return($return_value);
}


# Complement a given sequence
sub complement {
    my($sequence) = @_;
    my($return_value) = "";

    for (my $i = 0; $i < length($sequence); $i++) {
	my $char = substr($sequence, $i, 1);

	if($char eq "A") {
	    $return_value .= "T";
	} elsif ($char eq "C") {
	    $return_value .= "G";
	} elsif ($char eq "G") {
	    $return_value .= "C";
	} elsif ($char eq "T") {
	    $return_value .= "A";
	} elsif ($char eq "a") {
	    $return_value .= "t";
	} elsif ($char eq "c") {
	    $return_value .= "g";
	} elsif ($char eq "g") {
	    $return_value .= "c";
	} elsif ($char eq "t") {
	    $return_value .= "a";
	} elsif ($char eq "N") {
	    $return_value .= "N";
	} elsif ($char eq "n") {
	    $return_value .= "n";
	} elsif ($char eq "n") {
	    $return_value .= "n";
	} elsif ($char eq ".") {
	    $return_value .= ".";
	} else {
	    print(STDERR "Unknown DNA character ($char).");
	    exit(1);
	}
    }

    return($return_value);
}


# Convert a sequence to dibase encoding
sub convert_to_dibase{

    my($sequence) = @_;
    my ($i, $di);
    my $dibase_sequence = '';

    # Color Code
    my(%color) = &define_color_code;

    for($i = 0; $i < (length($sequence) - 1); $i++){
	$di = substr($sequence, $i, 2);
	$di =~ tr/acgtnx/ACGTNX/;
	$dibase_sequence .= $color{$di};
    }
    
    return $dibase_sequence;
}

# Convert a string to uppercase
sub convert_to_uppercase{

    my($string) = @_;

    $string =~ tr/[a-z]/[A-Z]/;
#    $string =~ tr/acgtnx/ACGTNX/;

    return $string;
}


# Get the header, sequence and length_of_sequence of a fasta file
sub process_fasta_file{

    my($fasta_file) = @_;
    my $header = '';
    my $sequence = '';

    open( FILE, "< $fasta_file" ) or die "Can't open $fasta_file : $!";
    while( <FILE> ) {
	chomp;
	$_ =~ s/^\s+//;
	$_ =~ s/\s+$//;
	if (/^\#/){
	}
	elsif (/^>/){
	    $header .= $_;
	}
	else{
	    $sequence .= $_;
	}
    }
    my $length_of_sequence = length($sequence);

    return ($header, $sequence, $length_of_sequence);
}

# Get the length of a sequence in a fasta file
sub length_of_sequence{

    my($fasta_file) = @_;
    my $header = '';
    my $sequence = '';

    open( FILE, "< $fasta_file" ) or die "Can't open $fasta_file : $!";
    while( <FILE> ) {
	chomp;
	if (/^\#/){
	}
	elsif (/^>/){
	    $header .= $_;
	}
	else{
	    $sequence .= $_;
	}
    }
    my $length_of_sequence = length($sequence);

    return ($length_of_sequence);
}


sub define_color_code {

    my %color = ();

    $color{AA} = 0;
    $color{CC} = 0;
    $color{GG} = 0;
    $color{TT} = 0;
    $color{AC} = 1;
    $color{CA} = 1;
    $color{GT} = 1;
    $color{TG} = 1;
    $color{AG} = 2;
    $color{CT} = 2;
    $color{GA} = 2;
    $color{TC} = 2;
    $color{AT} = 3;
    $color{CG} = 3;
    $color{GC} = 3;
    $color{TA} = 3;
    $color{AN} = 4;
    $color{CN} = 4;
    $color{GN} = 4;
    $color{TN} = 4;
    $color{NA} = 5;
    $color{NC} = 5;
    $color{NG} = 5;
    $color{NT} = 5;
    $color{NN} = 6;

    $color{aa} = 0;
    $color{cc} = 0;
    $color{gg} = 0;
    $color{tt} = 0;
    $color{ac} = 1;
    $color{ca} = 1;
    $color{gt} = 1;
    $color{tg} = 1;
    $color{ag} = 2;
    $color{ct} = 2;
    $color{ga} = 2;
    $color{tc} = 2;
    $color{at} = 3;
    $color{cg} = 3;
    $color{gc} = 3;
    $color{ta} = 3;
    $color{an} = 4;
    $color{cn} = 4;
    $color{gn} = 4;
    $color{tn} = 4;
    $color{na} = 5;
    $color{nc} = 5;
    $color{ng} = 5;
    $color{nt} = 5;
    $color{nn} = 6;

    $color{aA} = 0;
    $color{cC} = 0;
    $color{gG} = 0;
    $color{tT} = 0;
    $color{aC} = 1;
    $color{cA} = 1;
    $color{gT} = 1;
    $color{tG} = 1;
    $color{aG} = 2;
    $color{cT} = 2;
    $color{gA} = 2;
    $color{tC} = 2;
    $color{aT} = 3;
    $color{cG} = 3;
    $color{gC} = 3;
    $color{tA} = 3;
    $color{aN} = 4;
    $color{cN} = 4;
    $color{gN} = 4;
    $color{tN} = 4;
    $color{nA} = 5;
    $color{nC} = 5;
    $color{nG} = 5;
    $color{nT} = 5;
    $color{nN} = 6;

    $color{Aa} = 0;
    $color{Cc} = 0;
    $color{Gg} = 0;
    $color{Tt} = 0;
    $color{Ac} = 1;
    $color{Ca} = 1;
    $color{Gt} = 1;
    $color{Tg} = 1;
    $color{Ag} = 2;
    $color{Ct} = 2;
    $color{Ga} = 2;
    $color{Tc} = 2;
    $color{At} = 3;
    $color{Cg} = 3;
    $color{Gc} = 3;
    $color{Ta} = 3;
    $color{An} = 4;
    $color{Cn} = 4;
    $color{Gn} = 4;
    $color{Tn} = 4;
    $color{Na} = 5;
    $color{Nc} = 5;
    $color{Ng} = 5;
    $color{Nt} = 5;
    $color{Nn} = 6;

    $color{'A.'} = '.';
    $color{'C.'} = '.';
    $color{'G.'} = '.';
    $color{'T.'} = '.';
    $color{'N.'} = '.';

    $color{'a.'} = '.';
    $color{'c.'} = '.';
    $color{'g.'} = '.';
    $color{'t.'} = '.';
    $color{'n.'} = '.';

    $color{'.A'} = '.';
    $color{'.C'} = '.';
    $color{'.G'} = '.';
    $color{'.T'} = '.';
    $color{'.N'} = '.';

    $color{'.a'} = '.';
    $color{'.c'} = '.';
    $color{'.g'} = '.';
    $color{'.t'} = '.';
    $color{'.n'} = '.';

    $color{'..'} = '.';

    return(%color);
}


sub define_reverse_color {

    my %reverse_color = ();

    $reverse_color{A0} = 'A';
    $reverse_color{A1} = 'C';
    $reverse_color{A2} = 'G';
    $reverse_color{A3} = 'T';
    $reverse_color{T0} = 'T';
    $reverse_color{T1} = 'G';
    $reverse_color{T2} = 'C';
    $reverse_color{T3} = 'A';
    $reverse_color{C0} = 'C';
    $reverse_color{C1} = 'A';
    $reverse_color{C2} = 'T';
    $reverse_color{C3} = 'G';
    $reverse_color{G0} = 'G';
    $reverse_color{G1} = 'T';
    $reverse_color{G2} = 'A';
    $reverse_color{G3} = 'C';
    $reverse_color{'A.'} = '.';
    $reverse_color{'T.'} = '.';
    $reverse_color{'C.'} = '.';
    $reverse_color{'G.'} = '.';

    $reverse_color{a0} = 'A';
    $reverse_color{a1} = 'C';
    $reverse_color{a2} = 'G';
    $reverse_color{a3} = 'T';
    $reverse_color{t0} = 'T';
    $reverse_color{t1} = 'G';
    $reverse_color{t2} = 'C';
    $reverse_color{t3} = 'A';
    $reverse_color{c0} = 'C';
    $reverse_color{c1} = 'A';
    $reverse_color{c2} = 'T';
    $reverse_color{c3} = 'G';
    $reverse_color{g0} = 'G';
    $reverse_color{g1} = 'T';
    $reverse_color{g2} = 'A';
    $reverse_color{g3} = 'C';
    $reverse_color{'a.'} = '.';
    $reverse_color{'t.'} = '.';
    $reverse_color{'c.'} = '.';
    $reverse_color{'g.'} = '.';

    return(%reverse_color);
}


sub reference_sequence {

    my($strand, $genome, $location, $tag_length) = @_;

    my $reference_sequence;

    if($strand eq 'top'){
	$reference_sequence = substr($$genome, $location, $tag_length);
    }
    else{
	$reference_sequence = substr($$genome, $location - $tag_length + 1, $tag_length);
    }

    return $reference_sequence;
}


sub reference_sequence_all_color {

    my($strand, $genome, $location, $tag_length) = @_;

    my $reference_sequence;

    if($strand eq 'top'){
	$reference_sequence = substr($$genome, $location - 1, $tag_length + 1);
    }
    else{
	$reference_sequence = substr($$genome, $location - $tag_length + 1, $tag_length + 1);
    }

    return $reference_sequence;
}


sub reference_to_dibase {

    my($reference_sequence, $color, $strand, $tag_length) = @_;

    my($i, $di, $reference);

    $reference = '';
    for($i = 0; $i < length($reference_sequence) - 1; $i++){
	$di = substr($reference_sequence, $i, 2);
	if($di =~ '\.'){
	    print $di, "\t", $i, "\t", $reference_sequence, "\n";
	}
	$reference .= $$color{$di};
    }
    if($strand eq 'top'){
	$reference = substr($reference_sequence, 0, 1) . $reference;
    }
    else{
	if( length($reference_sequence) >= $tag_length ){
	    $reference = $reference . substr($reference_sequence, $tag_length - 1, 1);
	}
    }

    return $reference;
}


sub reference_to_dibase_all_color {

    my($reference_sequence, $color, $strand, $tag_length) = @_;

    my($i, $di, $reference);

    $reference = '';
    for($i = 0; $i < length($reference_sequence) - 1; $i++){
	$di = substr($reference_sequence, $i, 2);
	$reference .= $$color{$di};
    }

    return $reference;
}


sub tag_sequence {

    my($strand, $sequence_original, $reverse_color, $tag_length) = @_;

    my($first_base, $sequence);

    if($strand eq 'top'){
	if( substr($sequence_original, 1, 1) ne '.'){
	    $first_base = $$reverse_color{substr($sequence_original, 0, 2)};
	}
	else{
	    $first_base = '.';
	}
	$sequence = $first_base . substr($sequence_original, 2, length($sequence_original) - 2);
	if( substr($sequence_original, 1, 1) ne '.'){
	    $sequence = substr($sequence, 0, $tag_length);
	}
	else{
	    $sequence = substr($sequence, 0, $tag_length + 1);
	}
    }
    else{
	$sequence_original = substr($sequence_original, 0, $tag_length + 1);
	if( substr($sequence_original, 1, 1) ne '.'){
	    $first_base = $$reverse_color{substr($sequence_original, 0, 2)};
	    $first_base = revcomp($first_base);
	}
	else{
	    $first_base = '.';
	}
	$sequence = substr($sequence_original, 2, $tag_length - 1);
	$sequence = reverse($sequence);
	$sequence .= $first_base;
    }

    return $sequence;
}


sub tag_sequence_all_color {

    my($strand, $sequence_original, $color, $reverse_color, $genome, $location, $tag_length) = @_;

    my($first_base, $reference_primer, $first_dimer, $first_color_call, $sequence);

    if($strand eq 'top'){
	if( substr($sequence_original, 1, 1) ne '.'){
	    $first_base = $$reverse_color{substr($sequence_original, 0, 2)};
	}
	else{
	    $first_base = '.';
	}
	$reference_primer = substr($$genome, $location - 1, 1);
	$first_dimer = $reference_primer . $first_base;
	$first_color_call = $$color{$first_dimer};
	$sequence = $first_color_call . substr($sequence_original, 2, length($sequence_original) - 2);
    }
    else{
	$sequence_original = substr($sequence_original, 0, $tag_length + 1);
	if( substr($sequence_original, 1, 1) ne '.'){
	    $first_base = $$reverse_color{substr($sequence_original, 0, 2)};
	    $first_base = revcomp($first_base);
	}
	else{
	    $first_base = '.';
	}
	$reference_primer = substr($$genome, $location + 1, 1);
	$first_dimer = $first_base . $reference_primer;
	$first_color_call = $$color{$first_dimer};
	$sequence  = reverse(substr($sequence_original, 2, $tag_length - 1)) . $first_color_call;
    }

    return $sequence;
}


sub print_sequence {

    my($sequence, $length, $FILE) = @_;

    use strict;
    use warnings;

    # Print sequence in lines of $length
    for ( my $pos = 0; $pos < length($sequence); $pos += $length ) {
	print $FILE substr($sequence, $pos, $length), "\n";
    }
}


sub define_snp_changes {

    my($snp_changes) = @_;

    $$snp_changes{'00'} = ['11', '22', '33', '11', '22', '33'];
    $$snp_changes{'11'} = ['00', '22', '33', '00', '22', '33'];
    $$snp_changes{'22'} = ['00', '11', '33', '00', '11', '33'];
    $$snp_changes{'33'} = ['00', '11', '22', '00', '11', '22'];    
    $$snp_changes{'01'} = ['10', '23', '32', '10', '23', '32'];
    $$snp_changes{'10'} = ['01', '23', '32', '01', '23', '32'];
    $$snp_changes{'23'} = ['01', '10', '32', '01', '10', '32'];
    $$snp_changes{'32'} = ['01', '10', '23', '01', '10', '23'];
    $$snp_changes{'02'} = ['13', '20', '31', '13', '20', '31'];
    $$snp_changes{'13'} = ['02', '31', '20', '02', '31', '20'];
    $$snp_changes{'20'} = ['31', '02', '13', '31', '02', '13'];
    $$snp_changes{'31'} = ['20', '13', '02', '20', '13', '02'];
    $$snp_changes{'03'} = ['12', '21', '30', '12', '21', '30'];
    $$snp_changes{'12'} = ['03', '30', '21', '03', '30', '21'];
    $$snp_changes{'21'} = ['30', '03', '12', '30', '03', '12'];
    $$snp_changes{'30'} = ['21', '12', '03', '21', '12', '03'];

    $$snp_changes{'A0'} = ['T3', 'C1', 'G2', 't3', 'c1', 'g2'];
    $$snp_changes{'A1'} = ['T2', 'C0', 'G3', 't2', 'c0', 'g3'];
    $$snp_changes{'A2'} = ['T1', 'C3', 'G0', 't1', 'c3', 'g0'];
    $$snp_changes{'A3'} = ['T0', 'C2', 'G1', 't0', 'c2', 'g1'];
    $$snp_changes{'T0'} = ['A3', 'C2', 'G1', 'a3', 'c2', 'g1'];
    $$snp_changes{'T1'} = ['A2', 'C3', 'G0', 'a2', 'c3', 'g0'];
    $$snp_changes{'T2'} = ['A1', 'C0', 'G3', 'a1', 'c0', 'g3'];
    $$snp_changes{'T3'} = ['A0', 'C1', 'G2', 'a0', 'c1', 'g2'];
    $$snp_changes{'C0'} = ['A1', 'T2', 'G3', 'a1', 't2', 'g3'];
    $$snp_changes{'C1'} = ['A0', 'T3', 'G2', 'a0', 't3', 'g2'];
    $$snp_changes{'C2'} = ['A3', 'T0', 'G1', 'a3', 't0', 'g1'];
    $$snp_changes{'C3'} = ['A2', 'T1', 'G0', 'a2', 't1', 'g0'];
    $$snp_changes{'G0'} = ['A2', 'T1', 'C3', 'a2', 't1', 'c3'];
    $$snp_changes{'G1'} = ['A3', 'T0', 'C2', 'a3', 't0', 'c2'];
    $$snp_changes{'G2'} = ['A0', 'T3', 'C1', 'a0', 't3', 'c1'];
    $$snp_changes{'G3'} = ['A1', 'T2', 'C0', 'a1', 't2', 'c0'];

    $$snp_changes{'0A'} = ['3T', '1C', '2G', '3t', '1c', '2g'];
    $$snp_changes{'1A'} = ['2T', '0C', '3G', '2t', '0c', '3g'];
    $$snp_changes{'2A'} = ['1T', '3C', '0G', '1t', '3c', '0g'];
    $$snp_changes{'3A'} = ['0T', '2C', '1G', '0t', '2c', '1g'];
    $$snp_changes{'0T'} = ['3A', '2C', '1G', '3a', '2c', '1g'];
    $$snp_changes{'1T'} = ['2A', '3C', '0G', '2a', '3c', '0g'];
    $$snp_changes{'2T'} = ['1A', '0C', '3G', '1a', '0c', '3g'];
    $$snp_changes{'3T'} = ['0A', '1C', '2G', '0a', '1c', '2g'];
    $$snp_changes{'0C'} = ['1A', '2T', '3G', '1a', '2t', '3g'];
    $$snp_changes{'1C'} = ['0A', '3T', '2G', '0a', '3t', '2g'];
    $$snp_changes{'2C'} = ['3A', '0T', '1G', '3a', '0t', '1g'];
    $$snp_changes{'3C'} = ['2A', '1T', '0G', '2a', '1t', '0g'];
    $$snp_changes{'0G'} = ['2A', '1T', '3C', '2a', '1t', '3c'];
    $$snp_changes{'1G'} = ['3A', '0T', '2C', '3a', '0t', '2c'];
    $$snp_changes{'2G'} = ['0A', '3T', '1C', '0a', '3t', '1c'];
    $$snp_changes{'3G'} = ['1A', '2T', '0C', '1a', '2t', '0c'];

    $$snp_changes{'a0'} = ['T3', 'C1', 'G2', 't3', 'c1', 'g2'];
    $$snp_changes{'a1'} = ['T2', 'C0', 'G3', 't2', 'c0', 'g3'];
    $$snp_changes{'a2'} = ['T1', 'C3', 'G0', 't1', 'c3', 'g0'];
    $$snp_changes{'a3'} = ['T0', 'C2', 'G1', 't0', 'c2', 'g1'];
    $$snp_changes{'t0'} = ['A3', 'C2', 'G1', 'a3', 'c2', 'g1'];
    $$snp_changes{'t1'} = ['A2', 'C3', 'G0', 'a2', 'c3', 'g0'];
    $$snp_changes{'t2'} = ['A1', 'C0', 'G3', 'a1', 'c0', 'g3'];
    $$snp_changes{'t3'} = ['A0', 'C1', 'G2', 'a0', 'c1', 'g2'];
    $$snp_changes{'c0'} = ['A1', 'T2', 'G3', 'a1', 't2', 'g3'];
    $$snp_changes{'c1'} = ['A0', 'T3', 'G2', 'a0', 't3', 'g2'];
    $$snp_changes{'c2'} = ['A3', 'T0', 'G1', 'a3', 't0', 'g1'];
    $$snp_changes{'c3'} = ['A2', 'T1', 'G0', 'a2', 't1', 'g0'];
    $$snp_changes{'g0'} = ['A2', 'T1', 'C3', 'a2', 't1', 'c3'];
    $$snp_changes{'g1'} = ['A3', 'T0', 'C2', 'a3', 't0', 'c2'];
    $$snp_changes{'g2'} = ['A0', 'T3', 'C1', 'a0', 't3', 'c1'];
    $$snp_changes{'g3'} = ['A1', 'T2', 'C0', 'a1', 't2', 'c0'];

    $$snp_changes{'0a'} = ['3T', '1C', '2G', '3t', '1c', '2g'];
    $$snp_changes{'1a'} = ['2T', '0C', '3G', '2t', '0c', '3g'];
    $$snp_changes{'2a'} = ['1T', '3C', '0G', '1t', '3c', '0g'];
    $$snp_changes{'3a'} = ['0T', '2C', '1G', '0t', '2c', '1g'];
    $$snp_changes{'0t'} = ['3A', '2C', '1G', '3a', '2c', '1g'];
    $$snp_changes{'1t'} = ['2A', '3C', '0G', '2a', '3c', '0g'];
    $$snp_changes{'2t'} = ['1A', '0C', '3G', '1a', '0c', '3g'];
    $$snp_changes{'3t'} = ['0A', '1C', '2G', '0a', '1c', '2g'];
    $$snp_changes{'0c'} = ['1A', '2T', '3G', '1a', '2t', '3g'];
    $$snp_changes{'1c'} = ['0A', '3T', '2G', '0a', '3t', '2g'];
    $$snp_changes{'2c'} = ['3A', '0T', '1G', '3a', '0t', '1g'];
    $$snp_changes{'3c'} = ['2A', '1T', '0G', '2a', '1t', '0g'];
    $$snp_changes{'0g'} = ['2A', '1T', '3C', '2a', '1t', '3c'];
    $$snp_changes{'1g'} = ['3A', '0T', '2C', '3a', '0t', '2c'];
    $$snp_changes{'2g'} = ['0A', '3T', '1C', '0a', '3t', '1c'];
    $$snp_changes{'3g'} = ['1A', '2T', '0C', '1a', '2t', '0c'];
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
