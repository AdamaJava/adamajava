package QCMG::IO::JsonReader;

##############################################################################
#
#  Script:   JsonReader.pm
#  Creator:  Matthew J Anderson
#  Created:  2013-02-26
# 
#  This is a script remove comments from a JSON file and decodes the information
#  into a Perl data struture. Which may be a mixture of hashes and arrays.
#
#  Requires:
#    JSON		- From CPAN
#
#  $Id: JsonReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
##############################################################################

use strict;                     # Good practice
use warnings;                   # Good practice
use JSON qw( decode_json );     # From CPAN

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: JsonReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 scalar: debug (optional)	Set as 1 for debugging.

Returns:
 a new instance of this class.

=cut

sub new {
    my $class   	= shift;
    my $debug   	= shift;

	my $self = {
		debug           => ( $debug ?  1 :  0  )
	};
	
	bless $self, $class;
	return $self;
}


################################################################################

=pod

=head1 METHODS

B<decode_json_file()>

Decodes the contents of JSON file format into a Perl data structure. 

Parameters:
 scalar: json_file 	File path to the JSON file.

Returns:
 a Perl data structure. Its up to the user to know what to do with it.

=cut

sub decode_json_file {
	my $self		= shift;
	my $json_file	= shift;
	
	if ( $self->{debug} ) {
		print "JSON file is $json_file\n";
	}
	
	my $json = '';
	
	open JSON_FILE, "<", $json_file or die $!;
	# Read JSON file removing empty and single line comments.
	while ( my $line = <JSON_FILE>) {
		$line =~ s/^(\s*)*\/\/.+\n//g;   	# Remove single line comments
		$line =~ s/^\s*\n//g; 				# Remove empty lines	
		$json .= $line;
	}
	close JSON_FILE;
	
	# Finaly remove any muliti line comments
	$json =~ s!/\*.*(\n.*)*\*/(\s*\n)!!g;   	# Remove multi line comments  /*  */
	
	# Decode the entire JSON sting into a perl data strucutre.
	my $decoded_json = decode_json( $json );
	
	# Return data structure.
	return $decoded_json;
}


1;

__END__


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013-2014

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
