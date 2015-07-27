package QCMG::Google::ChartCollection;

###########################################################################
#
#  Module:   QCMG::Google::ChartCollection.pm
#  Creator:  John V Pearson
#  Created:  2011-05-29
#
#  Helper class for organising tricky Javascript associated with a
#  single (tabbed) document containing multiple Google Chart
#  visualisations of various types.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;
use Carp qw( croak );

use QCMG::Google::DataTable;
use QCMG::Google::Chart::Bar;

use vars qw( $VERSION @ISA );

( $VERSION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    # For convenience, we will store refs to all input objects in both
    # an array $self->{in_order} and a hash $self->{charts}.

    my $self = { charts     => {},
                 in_order   => [],
                 types      => {},
                 verbose    => (exists $params{verbose} and $params{verbose}
                                ? $params{verbose} : 0),
                 version    => $VERSION };

    bless $self, $class;
}



sub _charts {
    my $self = shift;
    return $self->{charts};
}

sub _charts_in_order {
    my $self = shift;
    return $self->{in_order};
}

sub _types {
    my $self = shift;
    return $self->{types};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub version {
    my $self = shift;
    return $self->{version};
}


sub _scripts {
    my $self = shift;
    my $text = '<!-- Google Chart -->
<script type="text/javascript" src="http://www.google.com/jsapi"></script>
<script type="text/javascript">
google.load("visualization", "1", {packages:["corechart","table"]});
google.load("prototype", "1.6");
';

    my %include_text = ( corechart => '',
                         bioheatmap =>
'<script type="text/javascript" src="http://systemsbiology-visualizations.googlecode.com/svn/trunk/src/main/js/load.js"></script>
<script type="text/javascript">
systemsbiology.load("visualization", "1.0", {packages:["bioheatmap"]});
</script>
',
                         magictable => 
'<script type="text/javascript" src="http://magic-table.googlecode.com/svn/trunk/magic-table/javascript/magic_table.js"></script>
' );

    foreach my $type (keys %{$self->_types}) {
        $text .= $include_text{ $type };
    }

    return $text;
}


sub javascript {
    my $self   = shift;

    my $text = $self->_scripts .
               "\nfunction drawChart() {\n";

    # Add in javascript for each Chart in Collection
    foreach my $obj (@{ $self->{in_order} }) {
        $text .= $obj->javascript();
    }

    $text .= '
}
google.setOnLoadCallback( function() {
    <!-- This JavaScript snippet activates the tabs -->
    jQuery("ul.tabs").tabs("> .pane");
    <!-- And this JavaScript snippet triggers drawing the charts -->
    drawChart();
});
</script>' ."\n";

    return $text;
}


sub add_parameter {
    my $self   = shift;
    my %params = @_;

    my $p = QCMG::Bioscope::Ini::Parameter->new( %params );
    $self->add_object( $p );
}


sub add_chart {
    my $self = shift;
    my $obj  = shift;

    # Check that this object is something we understand
    if (ref($obj) =~ /QCMG::Google::Chart::/) {
        $self->_charts->{ $obj->name } = $obj;
        push @{ $self->_charts_in_order }, $obj;
    }
    else {
        croak 'Parameter to add_chart() must be a ' .
              'QCMG::Google::Chart, not a ['. ref($obj) . ']';
    }
}


sub get_chart {
    my $self = shift;
    my $name = shift;

    if  (exists $self->_charts->{$name} and defined $self->_charts->{$name}) {
        return $self->_charts->{ $name };
    }
    else {
        return undef;
    }
}



1;
__END__


=head1 NAME

QCMG::Google::ChartCollection - A Collection of Google Charts


=head1 SYNOPSIS

 use QCMG::Google::ChartCollection;
 my $cc = QCMG::Google::ChartCollection->new( );
 $cc->add_chart( QCMG::Google::Chart::Pie->new( name = 'chart1',
                                                values = [ 10,20,30,40 ] );
 print $cc->javascript;


=head1 DESCRIPTION

This module represents a collection of Google Chart objects.  It is
intended to simplify and centralise the task of building the JavaScript
that is required for each chart.


=head1 PUBLIC METHODS

=over

=item B<new()>
 
 my $cc = QCMG::Google::ChartCollection->new( verbose => 0 );

Takes (optional) verbose parameter.

=item B<add_chart()>
 
 my $c1 = QCMG::Google::Chart::Pie->new( name   => 'chart1',
                                         values => [ 60,40 ] );
 $cc->add_chart( $c );

This method is used to add Google::Chart objects to the collection.

=item B<get_chart()>

 my $c2 = $cc->get_chart( 'chart2' );

This method retuns a Google::Chart object if one exists with the specified
name.  If a matching object does not exist then undef is returned.  If 
it's important to know what sort of object has been returned, you'll have
to use Perl's ref() statement.

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
