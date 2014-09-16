package QCMG::Util::XML;

##############################################################################
#
#  Module:   QCMG::Util:XML.pm
#  Author:   John V Pearson
#  Created:  2011-09-26
#
#  This module does not contain a class but it does contain a collection
#  of methods (some OO and some not) that relate to operating on XML
#  files and objects.
#
#  $Id: XML.pm 4665 2014-07-24 08:54:04Z j.pearson $
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

    $REVISION = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
    $SVNID = '$Id: XML.pm 4665 2014-07-24 08:54:04Z j.pearson $'
              =~ /\$Id:\s+(.*)\s+/;

    @ISA = qw(Exporter);
    %EXPORT_TAGS = ( );     # eg: TAG => [ qw!name1 name2! ],
    
    # Optionally exported functions
    @EXPORT_OK = qw( get_attr_by_name get_nodes_by_names 
                     get_node_by_name );
}


# Get the value of a named attribute of an XML::LibXML::Node
sub get_attr_by_name {
    my $this = shift;  # XML::LibXML::Node object to be parsed
    my $name = shift;

    if ($this->hasAttributes) {
        my @attributes = $this->attributes;
        foreach my $attr (@attributes) {
            #print $attr->nodeName, ' -> ', $attr->getValue, "\n";
            return $attr->getValue if ($attr->nodeName eq $name);
        }
    }
    return '';
}


sub get_nodes_by_names {
    my $this  = shift;  # XML::LibXML::Node object to be parsed
    my @names = @_;

    my @found = ();
    if ($this->hasChildNodes) {
        my @nodes = $this->childNodes;
        foreach my $node (@nodes) {
            foreach my $name (@names) {
                push @found, $node if ($node->nodeName eq $name);
            }
        }
    }

    return @found;
}


# Useful for cases where you know there is a single element instance
sub get_node_by_name {
    my $this = shift;  # XML::LibXML::Node object to be parsed
    my $name = shift;

    my @nodes = get_nodes_by_names( $this, $name );

    return $nodes[0];
}


1;

__END__


=head1 NAME

QCMG::Util::XML - Perl module containing imethods for operating on XML


=head1 SYNOPSIS

 use QCMG::Util::XML qw( get attr_by_name );


=head1 DESCRIPTION

This module is not a class, rather it contains a collection of
methods associated with processing XML.  To use any of
the functions described below, you will need to use the 'import'
notations shown above to make the functions visible in your programs
MAIN namespace.


=head1 FUNCTIONS

=over

=item B<get_attr_by_name()>

    my $node2 = get_attr_by_name( $node1, 'id' );

Takes 2 parameters - an object of type XML::LibXML::Node and a string
containing the name of the attribute you want to retrieve.

=item B<get_nodes_by_names()>

=item B<get_node_by_name()>

=back 


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: XML.pm 4665 2014-07-24 08:54:04Z j.pearson $


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
