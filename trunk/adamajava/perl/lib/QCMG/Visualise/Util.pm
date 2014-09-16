package QCMG::Visualise::Util;

##############################################################################
#
#  Module:   QCMG::Visualise::Util.pm
#  Author:   John V Pearson
#  Created:  2011-10-13
#
#  This module does not contain a class but it does contain a collection
#  of methods (some OO and some not) that relate to operating on XML
#  files and objects.
#
#  $Id: Util.pm 4666 2014-07-24 09:03:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use XML::LibXML;
use vars qw( $SVNID $REVISION @ISA @EXPORT @EXPORT_OK %EXPORT_TAGS );

BEGIN {
    use Exporter ();

    $REVISION = '$Revision: 4666 $ ' =~ /\$Revision:\s+([^\s]+)/;
    $SVNID = '$Id: Util.pm 4666 2014-07-24 09:03:04Z j.pearson $'
              =~ /\$Id:\s+(.*)\s+/;

    @ISA = qw(Exporter);
    %EXPORT_TAGS = ( );   # eg: TAG => [ qw!name1 name2! ],
    
    # Optionally exported functions
    @EXPORT_OK = qw( parse_xml_table );
}


# Get the value of a named attribute of an XML::LibXML::Node
sub parse_xml_table {
    my $this = shift;  # XML::LibXML::Node object of <Table/>

    my $name         = $this->getAttribute( 'name' );
    my @header_nodes = $this->findnodes( 'Headers/Header' );
    my @row_nodes    = $this->findnodes( 'Data/Row' );

    my @headers = ();
    push @headers, $_->textContent foreach @header_nodes;

    my @data = ();
    foreach my $row_node (@row_nodes) {
        my @cell_nodes = $row_node->findnodes( 'Cell' );
        my @this_row = ();
        push @this_row, $_->textContent foreach @cell_nodes;
        push @data, \@this_row;
    }

    my %table = ( name    => $name,
                  headers => \@headers,
                  data    => \@data );
    return \%table;
}


1;

__END__


=head1 NAME

QCMG::Visualise::Util - Perl module containing utility methods


=head1 SYNOPSIS

 use QCMG::Visualise::Util qw( parse_xml_table );


=head1 DESCRIPTION

This module is not a class, rather it contains a collection of
methods associated with processing Visualise XML.  To use any of
the functions described below, you will need to use the 'import'
notations shown above to make the functions visible in your programs
MAIN namespace.


=head1 FUNCTIONS

=over

=item B<parse_xml_table()>

    my $rh_table = parse_xml_table( $node );

Takes an XML::LibXML::Node element that must represent an XML Table
element of the form written by solidstatsreport, for example
BarcodeCounts, MappedReads, PairingClassifications.  Returns a hash with
3 elements:  'name' of the table (scalar) , 'headers' (arrayref) and
'data' (arrayref).

=back 


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Util.pm 4666 2014-07-24 09:03:04Z j.pearson $


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
