package QCMG::Lifescope::Tbl;

###########################################################################
#
#  Module:   QCMG::Lifescope::Tbl.pm
#  Creator:  John V Pearson
#  Created:  2011-12-05
#
#  Data container for LifeScope .tbl files.
#  This module uses the Moose OO framework.
#
#  $Id$
#
###########################################################################


use Moose;  # implicitly sets strict and warnings

use Carp qw( carp croak cluck confess );
use Data::Dumper;
use IO::File;
use QCMG::Util::QLog;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

has 'pathname' => ( is => 'rw', isa => 'Str', default => '' );
has 'title'    => ( is => 'rw', isa => 'Str', default => '' );
has 'headers'  => ( is => 'rw', isa => 'ArrayRef[Str]', default => sub{[]} );
has 'data'     => ( is => 'rw', isa => 'ArrayRef[Str]',
                    default => sub{[]}, clearer => 'clear_data' );
has 'verbose'  => ( is => 'rw', isa => 'Int', default => 0 );
has 'attribs'  => ( is => 'rw', isa => 'HashRef', default => sub{{}} );


sub row_count {
    my $self = shift;
    return scalar( @{ $self->data } );
}


sub as_xml {
    my $self = shift;
    my $xml = '';

    $xml .= '<LifeScopeTbl file="' . $self->file . '"' . ">\n";
    $xml .= '  <title>' . $self->title . "</title>\n";

    # Data including headers
    $xml .= "  <table>\n";
    $xml .= "    <headers>\n";
    foreach my $ctr (0..($self->header_count-1)) {
        $xml .= '     <header ctr="' . $ctr . '" name="' .
                $self->headers->[$ctr] . "\"/>\n";
    }
    $xml .= "    </headers>\n";
    $xml .= "    <data>\n";
    foreach my $data_ref (@{ $self->data }) {
        $xml .= '     <tr>';
        foreach my $ctr (0..($self->header_count-1)) {
            $xml .= '<td>' . $data_ref->[$ctr] . '</td>';
        }
        $xml .= "</tr>\n";
    }
    $xml .= "    </data>\n";
    $xml .= "  </table>\n";

    $xml .= "</LifeScopeTbl>\n";

    return $xml;
}


sub file {
    my $self = shift;

    my $file = $self->pathname;
    $file =~ s/.*\///g;
    return $file;
}


sub header_count {
    my $self = shift;
    return scalar( @{ $self->headers } );
}


no Moose;

1;
__END__


=head1 NAME

QCMG::Lifescope::Tbl - Perl data container for Lifescope .tbl files


=head1 SYNOPSIS

 use QCMG::Lifescope::Tbl;

 my $tbl = QCMG::Lifescope::TblReader->new( filename => 'Group_1-summary.tnl' );
 print $tbl->as_xml();

=head1 DESCRIPTION

This module provides a data container for Lifescope .tbl files.  This 
module is not designed with a constructor that shoudl be called by an
end user.


=head1 PUBLIC METHODS

=over

=item B<filename()>

Filename of the .tbl.

=item B<title()>

Title of the chart - taken from the file headers.

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

$Id$


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
