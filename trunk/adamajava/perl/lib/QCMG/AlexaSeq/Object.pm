package QCMG::AlexaSeq::Object;

use strict;
use vars qw(
		@ISA @EXPORT @EXPORT_OK %EXPORT_TAGS 
	);
use Exporter;
require AutoLoader;

=pod

=head1 VARIABLES AND TAGS

 Module version
  $VERSION
 
=cut

@ISA = qw(Exporter);
@EXPORT = qw( 
				%EXPORT_TAGS
		);
@EXPORT_OK = qw();
%EXPORT_TAGS =  (
				);
our $AUTOLOAD;

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

my $t = QCMG::AlexaSeq::Object->new();

Create a new instance of this class.

Parameters:
 None.

Returns:
 a new instance of this class.

=cut
sub new {
	my $class = shift @_;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $self	= {};

	bless ($self, $class);

	return $self;
}

sub AUTOLOAD {
	my $self = shift;
	my $type = ref($self) or warn "$self is not an object";

	my $name = $AUTOLOAD;
	$name =~ s/.*://;	# strip fully-qualified portion

	if (@_) {
		#print STDERR "AUTOLOAD $name\n";
		return $self->{$name} = shift;
	}
	else {
		return $self->{$name};
	}
}  


1;

__END__

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
