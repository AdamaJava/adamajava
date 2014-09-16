package QCMG::Lifescope::Cht;

###########################################################################
#
#  Module:   QCMG::Lifescope::Cht.pm
#  Creator:  John V Pearson
#  Created:  2011-08-30
#
#  Data container for LifeScope .cht files.
#  This module uses the Moose OO framework.
#
#  $Id: Cht.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################


use Moose;  # implicitly sets strict and warnings

use Carp qw( carp croak cluck confess );
use Data::Dumper;
use IO::File;
use QCMG::Util::QLog;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Cht.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

has 'pathname' => ( is => 'rw', isa => 'Str', default => '' );
has 'name'     => ( is => 'rw', isa => 'Str', default => '' );
has 'type'     => ( is => 'rw', isa => 'Str', default => '' );
has 'title'    => ( is => 'rw', isa => 'Str', default => '' );
has 'axis_num' => ( is => 'rw', isa => 'Int', default => 0 );
has 'axes'     => ( is => 'rw', isa => 'HashRef', default => sub{{}} );
has 'headers'  => ( is => 'rw', isa => 'ArrayRef[Str]', default => sub{[]} );
has 'data'     => ( is => 'rw', isa => 'ArrayRef[Num]',
                    default => sub{[]}, clearer => 'clear_data' );
has 'verbose'  => ( is => 'rw', isa => 'Int', default => 0 );
has 'attribs'  => ( is => 'rw', isa => 'HashRef', default => sub{{}} );


sub trim_data_table {
    my $self = shift;
    my $max  = shift;

    # Trimming only makes sense for 2-column tables of the form [name,%].
    return unless ($self->axis_num == 2);

    warn 'cant trim ' . $self->file . " because it has no data\n"
        if ($self->row_count == 0);
    qlogprint( 'Trimming ' . $self->file . " to $max\%\n" ) if $self->verbose;

    my @new_data = ();
    my $category = '';
    my $cumulative_total = 0;

    my $keepctr = 0;
    while ($cumulative_total < $max) {
       my $row = shift @{ $self->data };
       $category = $row->[0];
       $cumulative_total += $row->[1];
       push @new_data, $row;
       $keepctr++;
    }

    my $dropctr;
    while (scalar( @{ $self->data } )) {
         shift @{ $self->data };
         $dropctr++;
    }
    qlogprint( "  keeping $keepctr lines and trimming $dropctr\n" )
        if $self->verbose;

    # Add on "the rest" category
    push @new_data, [ '>'.$category, sprintf("%.4f",100-$cumulative_total) ]
        unless $cumulative_total >= 100;

    #$self->clear_data;
    push @{ $self->data }, @new_data;
}


sub bin_data_table {
    my $self = shift;
    my $bin  = shift;

    # Binning only makes sense for 2-column tables of the form [name,%].
    return unless ($self->axis_num == 2);

    my @new_data = ();
    my $first    = '';
    my $current  = '';
    my $ctr      = 0;
    my $total    = 0;

    while (my $row = shift @{ $self->data }) {
       $current = $row->[0];
       $ctr++;
       if ($ctr == 1) {
           $first = $current;
           $total = $row->[1];
       }
       elsif ($ctr % $bin == 0) {
           $total += $row->[1];
           push @new_data, [ $first .'-'. $current, sprintf("%.4f",$total) ];
           $ctr = 0;
       }
       else {
           $total += $row->[1];
       }
    }

    # Add on final category
    if ($ctr != 0) {
        push @new_data, [ '>'.$first, sprintf("%.4f",$total) ];
    }

    # Put all new data back into data structure
    push @{ $self->data }, @new_data;
}


sub row_count {
    my $self = shift;
    return scalar( @{ $self->data } );
}


sub as_xml {
    my $self = shift;
    my $xml = '';

    $xml .= '<LifeScopeCht file="' . $self->file . '"' . ">\n";
    $xml .= '  <name>'  . $self->name  . "</name>\n";
    $xml .= '  <type>'  . $self->type  . "</type>\n";
    $xml .= '  <title>' . $self->title . "</title>\n";

    # Axes and ranges
    $xml .= "  <axes>\n";
    foreach my $axis (sort keys %{ $self->axes }) {
        $xml .= '    <axis name="' . $axis . '" title="' .
                (defined( $self->axes->{$axis}->{title} ) ?
                          $self->axes->{$axis}->{title} : '') . '" range="' .
                (defined( $self->axes->{$axis}->{range} ) ?
                          $self->axes->{$axis}->{range} : '') . "\"/>\n";
    }
    $xml .= "  </axes>\n";

    # Data including headers
    $xml .= "  <table>\n";
    $xml .= "    <headers>\n";
    foreach my $ctr (0..($self->axis_num-1)) {
        $xml .= '     <header ctr="' . $ctr . '" name="' .
                $self->headers->[$ctr] . "\"/>\n";
    }
    $xml .= "    </headers>\n";
    $xml .= "    <data>\n";
    foreach my $data_ref (@{ $self->data }) {
        $xml .= '     <tr>';
        foreach my $ctr (0..($self->axis_num-1)) {
            $xml .= '<td>' . $data_ref->[$ctr] . '</td>';
        }
        $xml .= "</tr>\n";
    }
    $xml .= "    </data>\n";
    $xml .= "  </table>\n";

    $xml .= "</LifeScopeCht>\n";

    return $xml;
}


sub file {
    my $self = shift;

    my $file = $self->pathname;
    $file =~ s/.*\///g;
    return $file;
}

no Moose;

1;
__END__


=head1 NAME

QCMG::Lifescope::Cht - Perl module for parsing Lifescope .cht files


=head1 SYNOPSIS

 use QCMG::Lifescope::Cht;

 my $cht = QCMG::Lifescope::Cht->new( filename => 'Group_1.BaseQv.F3.cht' );
 $cht->trim_data_table(99);
 print $cht->as_xml();

=head1 DESCRIPTION

This module provides an interface for parsing Lifescope .cht files.
This module will not usually be directly invoked by the user.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $cht = QCMG::Lifescope::Cht->new( filename => 'Group_1.BaseQv.F3.cht' );

Contructor has no mandatory argument but most atributes can be set in
the constructor.

=item B<filename()>

Filename of the .cht.

=item B<name()>

Name of the .cht (from headers in file).

=item B<type()>

Type of the chart, currently a value from the list line, vbar, pie.

=item B<title()>

Title of the chart - in practice this is almost always the same as
B<name()> or very similar.

=item B<axis_num()>

Number of axes on this plot - 2 for the majority (all?) of charts.

=item B<axes()>

Returns a hashref where the keys are the axis designations (x,y,z) and
th value is a list of attributes of the axis, typically one or both of
I<range> and I<name> where range is a string showing the predicted best
scale for the axis (e.g. '0:10:100') and name is a suggested label for
the axis.

=item B<headers()>

Returns a reference to an array of strings that are the column headers
for the chart data table.

=item B<data()>

Returns a reference to an array of arrays representing the data rows of
the chart.

=item B<as_xml()>

 $cht->as_xml();

Returns the contents of the Cht data table as XML.  Note that this
does not include a document type line.

=item B<trim_data_table()>
 
 $cht->trim_data_table(99.9);

Takes a floating point number that is used to trim the end of the data
table.
Data tables can get out of hand, for example the BaseQV table has a
natural maximum of 41 because that is the highest allocated base QV,
but the data table goes up to almost 10,000 so 99.5% of the lines in
the BaseQV data table are wasted space.  To combat this problem, this
method will roll through the data table keeping a tally of the cumulative
total and as soon as it exceeds the nominated percentage (expressed as a
number in the range 0-100), the remaining data rows are dropped
and replaced
with a single >X category with a value of (100 - cumulative_total).

=item B<bin_data_table()>

 $cht->bin_data_table(25);

Takes an integer that is used as count for the number of records from
the data table that should be compressed into a single bin.  This method
is useful for cases where a data table has a large number of records but
the records are not in a tail that can be trimmed with 
B<trim_data_table()>.  Coverage for chrMT is a good example - the
average coverage is likely to be in the thousands so the distribution of
coverage values will be very broad with tails on both sides of the
average.  Binning is the best way to compress this coverage data.

=item B<verbose()>

 $cht->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Cht.pm 4663 2014-07-24 06:39:00Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

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
