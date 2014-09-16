package QCMG::Google::Chart::Table;

###########################################################################
#
#  Module:   QCMG::Google::Chart::Table.pm
#  Creator:  John V Pearson
#  Created:  2011-09-23
#
#  Convenience class for creating javascript to define and display a
#  Google Chart Table.
#
#  $Id: Table.pm 4662 2014-07-23 12:39:59Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use vars qw( $VERSION @ISA );

@ISA = qw( QCMG::Google::Charts );  # inherit Common methods
( $VERSION ) = '$Revision: 4662 $ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = bless $class->SUPER::_inherit( %params ), $class;

    $self->title( 'Google TableChart' );
    $self->chart_type( 'Table' );
    $self->show_row_number( 'false' );
    $self->svn_version( $VERSION );

    return $self;
}


sub show_row_number {
    my $self = shift;
    return $self->{show_row_number} = shift if @_;
    return $self->{show_row_number};
}


# The draw method for a table is quite different from that of other
# visualisations so we are going to override some of the inherited
# javscript generation methods

sub _draw_params_javascript {
    my $self = shift;

    my $text = '{width:' . $self->width .
               ', height:' . $self->height .
               ', showRowNumber:' . $self->show_row_number . '}';
    return $text;
}


sub _local_javascript_params {
    my $self = shift;
    return '';
}



# Example javascript as created by Ollie
# var head11Chart = new google.visualization.Table(
#     document.getElementById('head11Chart_div'));
#     head11Chart.draw(head11,
#         {width: 1000, 
#          height: head11.getNumberOfRows() > 50 ? 400 : 0,
#          showRowNumber: head11.getNumberOfRows() > 1});


1;
__END__


=head1 NAME

QCMG::Google::Chart::Table - Google charts Table


=head1 SYNOPSIS

 use QCMG::Google::Charts;

=head1 DESCRIPTION


=head1 PUBLIC METHODS

The majority of methods in this class are inherited from the
QCMG::Google::Charts superclass.

=over

=item B<new()>
 
 $tc = QCMG::Google::Chart::Table->new( name => 'data1', verbose => 0 );

Takes compulsory name and optional verbose parameters.

=back


=head1 SEE ALSO

=over

=item QCMG::Google::Charts

=item QCMG::Google::Chart::*

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Table.pm 4662 2014-07-23 12:39:59Z j.pearson $


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
