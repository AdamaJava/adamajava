# time_stamp
# factorial
# median_of_sorted_array

sub time_stamp {
  my ($d,$t);
  my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);
        $year += 1900;
        $mon++;
        $d = sprintf("%4d-%2.2d-%2.2d",$year,$mon,$mday);
        $t = sprintf("%2.2d:%2.2d:%2.2d",$hour,$min,$sec);
        return($d,$t);
}


sub factorial {
    my ($n) = @_;

    if($n == 0){
	return 1;
    }
    elsif($n == 1){
	return $n;
    }
    else {
	return $n * factorial($n-1);
    }
}


sub median_of_sorted_array {

    my(@array) = @_;

    my($length_of_array, $median);

    # Length of Array
    $length_of_array = scalar @array;
    
    #If the number of values is even, the median is the average of the two middle values
    if( ($length_of_array % 2) == 0 ) {
	# Looking for the average of the values at positions n/2 and n/2 + 1
	$median = ( $array[($length_of_array/2) - 1] + $array[(($length_of_array/2)+1) - 1] ) / 2;
    }
    #If the number of values is odd, the median is the middle value 
    else{
	# Looking for the value at position (n+1)/2
	$median = $array[( ($length_of_array+1)/2 ) - 1];
    }
    
    return $median;
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
