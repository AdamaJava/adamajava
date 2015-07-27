package QCMG::Google::Charts;

###########################################################################
#
#  Module:   QCMG::Google::Charts.pm
#  Creator:  John V Pearson
#  Created:  2011-05-29
#
#  Entry point for QCMG classes to implement Google charts API.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use QCMG::Google::ChartCollection;
use QCMG::Google::DataTable;
use QCMG::Google::Chart::Bar;
use QCMG::Google::Chart::Column;
use QCMG::Google::Chart::Line;
use QCMG::Google::Chart::Scatter;
use QCMG::Google::Chart::Table;
use QCMG::Google::Chart::PieChart;
use vars qw( $VERSION $GC_UNIQUE_CTR );

( $VERSION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
BEGIN { $GC_UNIQUE_CTR = 0; }


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub _inherit {
    my $class  = shift;
    my %params = @_;

    croak 'name parameter is compulsory' unless
        (exists $params{name} and defined $params{name} and $params{name}); 
    #croak 'name parameter supplied to new() ['. $params{name} .
    #      '] must not contain whitespace'
    #    if ($params{name} =~ /\s/);

    #             width              => 1000,
    #             height             => 1000,
    #             title              => 'Google Chart',
    #             chart_type         => undef,
    #             legend             => 'right',
    #             chart_left         => 60,
    #             chart_top          => 40,
    #             chart_width        => 80,  # %
    #             chart_height       => 85,  # %
    #             is_stacked         => 'false',
    #             is_vertical        => 'true',
    #             colors             => [ 'green','red','black','blue','aqua' ],
    #             font_size          => 12,
    #             title_font_size    => 15,
    #             horiz_title        => 'x axis',
    #             horiz_title_color  => 'blue',
    #             horiz_logscale     => 'false',
    #             vert_title         => 'y axis',
    #             vert_title_color   => 'blue',
    #             vert_logscale      => 'false',

    my $self = { name               => $params{name},
                 unique_name        => 'googleChart_'.$GC_UNIQUE_CTR++,
                 datatable          => undef,
                 is_doppelganger_of => undef,
                 type               => 'corechart',
                 verbose            => ($params{verbose} || 0),
                 svn_version        => $VERSION,
                 myparams => { width      => 1000,
                               height     => 1000,
                               title      => "'QCMG Chart'",
                               chartArea  => { left   => 60,
                                               top    => 40,
                                               width  => "'80%'",
                                               height => "'85%'" },
                               isStacked  => "'false'",
                               isVertical => "'true'",
                               colors     => [ "'green'","'red'","'black'","'blue'",
                                               "'aqua'" ],
                               legend     => "'right'",
                               fontSize   => 12,
                               titleFontSize => 15,
                               hAxis => { title      => "'x axis'",
                                          titleColor => "'blue'",
                                          logScale   => "'false'"},
                               vAxis => { title      => "'y axis'",
                                          titleColor => "'blue'",
                                          logScale   => "'false'"} } };

    bless $self, $class;
    
    my $dt = QCMG::Google::DataTable->new( name    => $self->unique_name,
                                           verbose => $self->verbose );
    $self->{datatable} = $dt;

    return $self;
}


# This is a special constructor that takes an existing Chart:: object
# and points the new Charts datatable to the existing Charts datatable

sub doppelganger {
    my $class  = shift;
    my $donor  = shift;
    my %params = @_;

    my $self = $class->new( %params );
    # Tie the datatables together;
    $self->{datatable} = $donor->{datatable};
    $self->{is_doppelganger_of} = $donor;
    return $self;
}


# Name and verbose level should not be reset unless you also reset them
# for the captive DataTable object.

sub name {
    my $self = shift;
    return $self->{name};
}

sub unique_name {
    my $self = shift;
    return $self->{unique_name};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub type {
    my $self = shift;
    return $self->{type};
}

sub myparams {
    my $self = shift;
    return $self->{myparams};
}

sub set_param {
    my $self = shift;
    my $name = shift;
    my $value = shift;

    return undef unless (defined $name and defined $value);
    return $self->{myparams}->{$name} = $value;
}

sub width {
    my $self = shift;
    return $self->myparams->{width} = shift if @_;
    return $self->myparams->{width};
}

sub height {
    my $self = shift;
    return $self->myparams->{height} = shift if @_;
    return $self->myparams->{height};
}

sub title {
    my $self = shift;
    return $self->myparams->{title} = ("'". shift() ."'") if @_;
    return $self->myparams->{title};
}

sub chart_type {
    my $self = shift;
    return $self->{chart_type} = shift if @_;
    return $self->{chart_type};
}

sub horiz_title {
    my $self = shift;
    #return $self->myparams->{horiz_title} = shift if @_;
    return $self->myparams->{horiz_title} = ("'". shift() ."'") if @_;
    return $self->myparams->{horiz_title};
}

sub vert_title {
    my $self = shift;
    #return $self->{vert_title} = shift if @_;
    return $self->myparams->{vert_title} = ("'". shift() ."'") if @_;
    return $self->myparams->{vert_title};
}

sub table {
    my $self = shift;
    return $self->{datatable};
}

sub cols {
    my $self = shift;
    return $self->table->cols();
}

sub rows {
    my $self = shift;
    return $self->table->rows();
}

sub col_count {
    my $self = shift;
    return $self->table->col_count();
}

sub row_count {
    my $self = shift;
    return $self->table->row_count();
}

sub version {
    my $self = shift;
    return $self->{version};
}

sub svn_version {
    my $self = shift;
    return $self->{svn_version};
}


sub javascript {
    my $self = shift;

    # If this chart is a doppelganger of another then we do not output
    # the "data structure" javascript, just the "draw" javascript.
    my $text = '';
    if (defined $self->{is_doppelganger_of}) {
        $text .= $self->_chart_javascript;
    }
    else {
        $text .= $self->table->as_javascript_literal(1) .
                 $self->_chart_javascript;
    }
    return $text;
}



sub javascript_name {
    my $self = shift;
    return $self->unique_name . '_Chart';
}

sub javascript_div_name {
    my $self = shift;
    return $self->javascript_name . 'Div';
}

sub javascript_div_html {
    my $self = shift;
    return '<div id="' . $self->javascript_div_name . "\"></div>\n";
}



sub _chart_javascript {
    my $self = shift;

    # Create new Chart javascript object
    my $text = 'var '. $self->javascript_name . 
               ' = new google.visualization.'. $self->chart_type .
               '( document.getElementById(\'' .
               $self->javascript_div_name . "'));\n" .
               $self->_chart_draw_javascript;
    return $text;
}


sub _chart_draw_javascript {
    my $self = shift;

    # Doppelgangers do not have their own datatable so point them at the
    # datatable written for their doppelganger.
    my $data_name = '';
    if (defined $self->{is_doppelganger_of}) {
        $data_name = $self->{is_doppelganger_of}->table->javascript_name;
    }
    else {
        $data_name = $self->table->javascript_name;
    }

    # Diagnotic
    #print $self->_chart_params_to_javascript;

    my $text = $self->javascript_name . ".draw( $data_name, " .
               $self->_chart_params_to_javascript . ");\n";
#               $self->_draw_params_javascript . ");\n";
    return $text;
}


# Parameter string for .draw(...) 

#sub _draw_params_javascript {
#    my $self = shift;
#
#    my $text = '{width:' . $self->width . 
#               ', height:' . $self->height .
#               ', title:\'' . $self->title . '\'' .
#               ', chartArea:{left:' . $self->chart_left .
#                            ',top:' . $self->chart_top .
#                            ',width:"' . $self->chart_width . '%"' .
#                            ',height:"' . $self->chart_height . '%"}' .
#               ', isStacked:\'' . $self->is_stacked . '\'' .
#               ', isVertical:\'' . $self->is_vertical . '\'';
#
#    if (defined $self->{colors}) {
#        $text .= ', colors:[' . join(',',$self->colors_as_strings) . ']';
#    }
#    if (defined $self->{legend}) {
#        $text .= ', legend:\'' . $self->legend . '\'';
#    }
#
#    # Include any chart type-specific options
#    $text .= $self->_local_javascript_params;
#     
#    $text .=   ', fontSize:' . $self->font_size .
#               ', titleFontSize:' . $self->title_font_size .
#               ', hAxis:{title:\'' . $self->horiz_title . '\'' .
#                        ', titleColor:\'' . $self->horiz_title_color . '\'' .
#                        ', logScale:' . $self->horiz_logscale . '}' .
#               ', vAxis:{title:\'' . $self->vert_title . '\'' .
#                        ', titleColor:\'' . $self->vert_title_color . '\'' .
#                        ', logScale:' . $self->vert_logscale . '}}';
#
#    return $text;
#}


sub add_col {
    my $self  = shift;
    my $id    = shift;
    my $label = shift;
    my $type  = shift;

    $self->table->add_col( $id, $label, $type );
}


sub add_row {
    my $self = shift;
    my @vals = @_;
    
    $self->table->add_row( @_ );
}


sub _chart_params_to_javascript {
    my $self = shift;
    return _hash2js($self->{myparams});
}


sub _array2js {
    my $thing = shift;
    my @elements =();
    foreach my $key (@{$thing}) {
        my $type = ref($key);
        #print "a_$type ";
        if ($type eq '') {
            push @elements, $key;
        }
        elsif ($type eq 'HASH') {
            push @elements, "$key:"._hash2js($thing->{$key});
        }
        elsif ($type eq 'ARRAY') {
            push @elements, "$key:"._array2js($thing->{$key});
        }
    }

    return '['. join(',',@elements) .']';
}


sub _hash2js {
    my $thing = shift;
    my @keys = sort keys %{ $thing };
    my @elements =();
    foreach my $key (@keys) {
        my $type = ref($thing->{$key});
        #print "h_$key($type) ";
        if ($type eq '') {
            # If value is undefined then skip it
            push @elements, "$key:".$thing->{$key} if defined($thing->{$key});
        }
        elsif ($type eq 'HASH') {
            push @elements, "$key:"._hash2js($thing->{$key});
        }
        elsif ($type eq 'ARRAY') {
            push @elements, "$key:"._array2js($thing->{$key});
        }
    }

    return '{'. join(',',@elements) .'}';
}


1;





=head1 NAME

QCMG::Google::Charts - QCMG classes to implement Google charts API


=head1 SYNOPSIS

 use QCMG::Google::Charts;

 my $gc = QCMG::Google::Chart:Table->new( name => 'Library Table' );

 $gc->add_col( 'col_A', 'Library', 'string');
 $gc->add_col( 'col_B', 'On-target %', 'number');
 $gc->add_col( 'col_C', 'Off-target %', 'number');
 $gc->add_row( 'APGI_1992_ND', 36.2, 63.8 );
 $gc->add_row( 'APGI_1992_TD', 37.4, 62.6 );
 $gc->add_row( 'APGI_2017_ND', 48.7, 51.3 );

 print $gc->javascript;


=head1 DESCRIPTION

This module is the entry point for the collection of QCMG classes that
implement the Google charts API. You should not directly instantiate
this class but should use one of the subclasses in the
QCMG::Google::Chart::* hierachy.

See also:

 http://code.google.com/apis/chart/interactive/docs/reference.html#DataTable
 http://code.google.com/apis/chart/interactive/docs/reference.html#dataparam


=head2 Javascript

The I<raison d'etre> of the entire QCMG::Google hierachy of modules is
to automate creation of the javascript that is required to create Google
Charts visualisations.  Each visualisation consists of 3 javascript
commands and an HTML div into which the visualisation is placed.
Assuming we had defined a QCMG::Google::Chart of type XXX (Bar, Line, 
Column etc)  with name B<blah>, the 3 commands would look something like:

 var blahTable = new google.visualization.DataTable( ...
 var blahChart = new google.visualization.XXX(
                     document.getElementById('blahChart_div'));
 blahChart.draw( blahTable, ...

The first step is to define a variable that contains a
B<google.visualization.DataTable>.  This is the common data container
used by all Google charts so no matter what sort of visualisation you
are designing, you need to create a DataTable to hold the values.

The second step is to define a variable that contains a
B<google.visualization.XXX> where XXX is the visualisation type you are
using.  You supply a single value to this call - the ID of the HTML div
element that you want the visualisation to be placed into.

The third and final step is to call the draw method of the visualisation
created in step 2 and give it the DataTable defined in step 1 along with
a collection of attributes that define draw parameters like chart width,
height, colours etc.

The javascript may look crazy but it's really just the same 3 elements
repeated for each of the visualisations on the page.  Easy peasy.

Having explained all of that, the only time you should even see the word
javascript is when you call the javascript method to get the text to be
inserted into your HTML page.  The B<add_col()> and B<add_row()> methods
are how you add values to the DataTable and most of the other accessors
in this Class and its subclasses are to change display parameters for
the javascript draw() method.


=head1 PUBLIC METHODS

All Google charts have 2 sets of accessors - one set to manipulate the
table of data that will be used to generate the plot, and a second set
that manipulate the display parameters that will determine how the plot
will be rendered.

=item B<doppelganger()>

 $tc = QCMG::Google::Chart::Table->doppelganger( $chart,
            name => 'data1', verbose => 0 );

This is a special constructor - you can get a new Chart:: object that 
shares the data from an existing Chart.  This method is invoked axactly
as you would for the B<new()> method in the Chart::* class you are
creating except that the first parmeter must be the Chart:: object that
you wish to share data from.

This method was created for a very specific use-case - you would like to
present the same data both graphically and as a table but you don't want
to have two copies of the data in the HTML page javascript.  Note that
there really is only one datatable and both Charts just hold references
to it so if you manipulate the data in either object, it will affect
both.  This is probably what you want if you are doing the dual 
presentation trick but just be forewarned.

Exercise caution when using this method.


=head2 DataTable Methods

These methods manipulate the underlying DataTable that is common to all
subclasses of type QCMG::Google::Charts.

=over

=item B<add_col()>

=item B<cols()>

=item B<rows()>
 
 $gc->add_col( 'col_A', 'Library', 'string');

This method takes 3 values that represent the id, label and type of the
column.  id is a string used to identify the column in any
columne-related operations, label is the string that will be output for
this column on a Google Chart, and type is the sort of values that are
contained in the column and should be on of I<string>, I<number> or
I<date>.

=item B<add_row()>
 
 $gc->add_row( 'APGI_1992_ND', 36.2, 63.8 );

This method takes as many values as there are columns in the data table.

=item B<col_count()>

=item B<row_count()>

=back

=head2 Miscellaneous Methods

=over 

=item B<javascript()>

Returns a block of javascript that defines the various elements required
to display a Google Chart.  The user must supply a HTML <div> element
with an id attribute that matches the chart name plus the '_div' suffix,
for example:

 <div id="MyChart_div"></div>

=item B<table()>

There may be cases where you would like to have a chart and a text table
of the same data.  You could do easily do this by defining a standard
chart and a Chart::Table both with the same data but this would be
wasteful as the javascript would contain 2 identical DataTables.  The
B<table()> method provides a better alternative - we define a new Table
visualisation but have it point to the same javascript DataTable as the
existing Chart.  To make this work, you must define a new 
QCMG::Google::Chart::Table object, setup the display options as you'd 
like but leave the DataTable empty and then supply the object as a
parameter the B<table()> method.  When the javascript is
written out, there will be 5 elements created - the standard 3 outlined 
above plus a B<google.visualization.Table> that refers to a new HTML
div plus a call to the draw method of the Table visualisation.
Assuming we had defined a QCMG::Google::Chart of type XXX (Bar, Line, 
Column etc)  with name B<blah>, the 5 commands would look something like:

 var blahTable = new google.visualization.DataTable( ...
 var blahChart = new google.visualization.XXX(
                     document.getElementById('blahChart_div'));
 blahChart.draw( blahTable, ...
 var blahChartTable = new google.visualization.Table(
                     document.getElementById('blahChartTable_div'));
 blahChartTable.draw( blahTable, ...

Note that there are not 6 javascript elements because both
visualisations share the same B<blahTable> DataTable.  Note that you now
need to supply 2 named HTML divs - one called B<blahChart_div> to hold
that chart and one called B<blahChartTable_div> to hold the matching
data table.

Also note that the name of the captive Chart::Table is not used as the
name of the primary Chart::* visualisation is used for both Chart and
Table.

=back

=head2 Display Methods

=over

=item B<name()>

=item B<verbose()>

=item B<type()>

=item B<width()>

=item B<height()>

=item B<title()>

=item B<chart_top()>

=item B<chart_width()>

=item B<chart_height()>

=item B<is_stacked()>

=item B<is_vertical()>

=item B<colors()>

=item B<colors_as_strings()>

=item B<font_size()>

=item B<title_font_size()>

=item B<horiz_title()>

=item B<horiz_title_color()>

=item B<horiz_logscale()>

=item B<vert_title()>

=item B<vert_title_color()>

=item B<vert_logscale()>

=item B<version()>

=item B<svn_version()>

=back

=head1 SEE ALSO

=over

=item QCMG::Google::ChartCollection

=item QCMG::Google::DataTable

=item QCMG::Google::Chart::Bar

=item QCMG::Google::Chart::Column

=item QCMG::Google::Chart::Line

=item QCMG::Google::Chart::Table

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
