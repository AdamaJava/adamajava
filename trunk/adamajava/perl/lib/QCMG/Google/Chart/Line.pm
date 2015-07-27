package QCMG::Google::Chart::Line;

###########################################################################
#
#  Module:   QCMG::Google::Chart::Line.pm
#  Creator:  John V Pearson
#  Created:  2011-05-29
#
#  Convenience class for creating javascript to define and display a
#  Google LineChart.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use vars qw( $VERSION @ISA );

@ISA = qw( QCMG::Google::Charts );  # inherit Common methods
( $VERSION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = bless $class->SUPER::_inherit( %params ), $class;
            
    $self->title( 'Google LineChart' );
    $self->chart_type( 'LineChart' );
    $self->svn_version( $VERSION );

    return $self;
}


sub _local_javascript_params {
    my $self = shift;
    return '';
}


1;
__END__


=head1 NAME

QCMG::Google::Chart::Line - Google charts Line


=head1 SYNOPSIS

 use QCMG::Google::Charts;

=head1 DESCRIPTION

=head1 PUBLIC METHODS

The majority of methods in this class are inherited from the
QCMG::Google::Charts superclass.

=over

=item B<new()>
 
  my $tc = QCMG::Google::Chart::Line->new( name => 'data1', verbose => 0 );

Takes compulsory name and optional verbose parameters.

=back

=head1 SEE ALSO

=over

=item QCMG::Google::Chart::*

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014

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
