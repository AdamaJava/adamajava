package QCMG::QInspect::PdfPath;

###########################################################################
#
#  Module:   QCMG::QInspect::PdfPath.pm
#  Creator:  John V Pearson
#  Created:  2012-06-10
#
#  Data container for a PDF Path which is a sequence of points defined
#  in 2-D space (x and y coordinates).
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { points    => [],
                 pre_text  => '',
                 post_text => '',
                 verbose   => $params{verbose} || 0 };
    if (exists $params{points}) {
        $self->{points} = $params{points};
    }
    bless $self, $class;
}


sub points {
    my $self = shift;
    return $self->{points};
}


sub pre_text {
    my $self = shift;
    return $self->{pre_text} = shift if @_;
    return $self->{pre_text};
}


sub post_text {
    my $self = shift;
    return $self->{post_text} = shift if @_;
    return $self->{post_text};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub add_points {
    my $self = shift;
    my @points = @_;
    push @{ $self->{points} }, @points;
}


sub apply_x_offset {
    my $self   = shift;
    my $offset = shift;
    foreach my $point (@{ $self->points() }) {
        $point->[0] += $offset;
    }
}


sub apply_y_offset {
    my $self   = shift;
    my $offset = shift;
    foreach my $point (@{ $self->points() }) {
        $point->[1] += $offset;
    }
}


sub offset_y_from {
    my $self   = shift;
    my $offset = shift;
    foreach my $point (@{ $self->points() }) {
        $point->[1] = $offset - $point->[1];
    }
}


sub scale {
    my $self  = shift;
    my $scale = shift;
    $self->scale_x($scale);
    $self->scale_y($scale);
}


sub scale_x {
    my $self  = shift;
    my $scale = shift;
    foreach my $point (@{ $self->points() }) {
        $point->[0] *= $scale;
    }
}


sub scale_y {
    my $self  = shift;
    my $scale = shift;
    foreach my $point (@{ $self->points() }) {
        $point->[1] *= $scale;
    }
}

sub pdf_text {
    my $self  = shift;

    my @points = @{ $self->points };
    my $point1 = shift @points;
    my $text = sprintf("%.3f", $point1->[0]) .' '.
               sprintf("%.3f", $point1->[1]) .' m ';
    foreach my $point (@points) {
        $text .= sprintf("%.3f", $point->[0]) .' '.
                 sprintf("%.3f", $point->[1]) .' l ';
    }
    return $self->pre_text() . $text . $self->post_text();
}


1;
 
__END__


=head1 NAME

QCMG::QInspect::PdfPath - PDF Path data container


=head1 SYNOPSIS

 use QCMG::QInspect::PdfPath;


=head1 DESCRIPTION

This module provides a data container for a PDF Path object.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $path = QCMG::QInspect::PdfPath->new();

This method creates a new SamRecord object from a text SAM record.

=item B<qname()>

 $path->add_points( [1,2], [2,2] [2,3], [1,3] );

Add one or more points to a path where each point is a 2-element array
representing the x and y coordinates of the point.

=item B<apply_x_offset()>

 $path->apply_x_offset( 7 );

Add the specified offset to the x-coordinate of every point in the path.
The offset can be a negative number.

=item B<apply_y_offset()>

 $path->apply_y_offset( 2.9 );

As per B<apply_x_offset()> but for the y-axis.

=item B<scale()>

 $path->scale( 0.8 );

Multiplies every x and y coordinate across the whole path by the scale
factor.

=item B<pre_text()>

 $path->pre_text( "0.8 0.8 0.8 RG\n" );

This text will be output by B<pdf_text()> before the PDF for the path
points.  This is useful if you have colors that you wish to apply to te
path.

=item B<post_text()>

 $path->post_text( " f\n" );

As per B<pre_text()> but output after the path point text.  This is
useful to append the stroke or fill or clip operators that you wish to
apply to the path.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012

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
