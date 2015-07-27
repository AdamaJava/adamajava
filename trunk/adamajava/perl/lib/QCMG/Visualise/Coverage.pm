package QCMG::Visualise::Coverage;

##############################################################################
#
#  Module:   QCMG::Visualise::Coverage.pm
#  Author:   John V Pearson
#  Created:  2011-09-26
#
#  Parse a XML::LibXML::Element that contains a <coverage> element take
#  from a qcoverage XML Report.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use POSIX qw(floor);
use XML::LibXML;

use QCMG::Util::QLog;
use QCMG::Util::XML qw( get_attr_by_name get_node_by_name );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

sub new {
    my $class  = shift;
    my %params = @_;

    my $type = ref($params{node});
    die "node parameter must refer to a XML::LibXML::Element object not [$type]"
        unless ($type eq 'XML::LibXML::Element');

    my $self = { xmlnode => $params{node},
                 feature => '',
                 type    => '',
                 data    => [],
                 verbose => $params{verbose} || 0 };
    bless $self, $class;

    $self->{feature} = get_attr_by_name( $self->node, 'feature' );
    $self->{type}    = get_attr_by_name( $self->node, 'type' );
    my @bcovs        = $self->node->findnodes( 'coverage' );

    # Process each coverage in the coverageReport
    my @data = ();
    my $bases_of_sequence = 0;
    my $region_length = 0;
    foreach my $bcov (@bcovs) {
        my $x = get_attr_by_name( $bcov, 'at' );
        my $y = get_attr_by_name( $bcov, 'bases' );
        $bases_of_sequence += ($x * $y);
        $region_length += $y;
        push @data, [ $x, $y ];
    }

    # Now make sure the coverage rows are sorted
    my @sorted_data = map  { $_->[1] }
                      sort { $a->[0] <=> $b->[0] }
                      map  { [ $_->[0], $_ ] }
                      @data;
    $self->{data} = \@sorted_data;

    $self->{bases_of_sequence} = $bases_of_sequence;
    $self->{region_length}     = $region_length;
    $self->{average_coverage}  = $bases_of_sequence / $region_length;

    return $self;
}


sub node {
    my $self = shift;
    return $self->{xmlnode};
}


sub feature {
    my $self = shift;
    return $self->{feature};
}


sub type {
    my $self = shift;
    return $self->{type};
}


sub data {
    my $self = shift;
    return $self->{data};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub bases_of_sequence {
    my $self = shift;
    return $self->{bases_of_sequence};
}


sub region_length {
    my $self = shift;
    return $self->{region_length};
}


sub average_coverage {
    my $self = shift;
    return $self->{average_coverage};
}



1;
__END__


=head1 NAME

QCMG::Visualise::Coverage - Perl module for holding a coverage record


=head1 SYNOPSIS

 use QCMG::Visualise::Coverage;

 my $cov = QCMG::Visualise::Coverage->new( node => $xmlnode,
                                           verbose => 1 );


=head1 DESCRIPTION

Class to contain data parsed from an XML::LibXML::Element that contains
a <coverage> element taken from a qcoverage XML Report.


=head1 PUBLIC METHODS

=over

=item B<new()>

This constructor takes a hash of parameters and returns an object.
There are only 2 valid parameters - B<node> and B<verbose>.  B<node> is
required and must be an object of type XML::LibXML::Element that relates
to a <coverage> element from a qcoverage XML report.  B<verbose> is 
optional (default=0) and if supplied, should be an integer.

=item B<node()>

Returns a reference to the XML::LibXML::Element object that was parsed
to create this object.

=item B<feature()>

Returns the value of the feature attribute of the <coverage> element.

=item B<type()>

Returns the value of the feature attribute of the <coverage> element.

=item B<data()>

Returns a reference to the internal array that holds the data.

=item B<bases_of_sequence()>

The number of sequenced bases that fall within the region of coverage.

=item B<region_length()>

The length of this region of coverage (in bases).

=item B<average_coverage()>

B<bases_of_sequence()> / B<region_length()>.

=item B<verbose()>

Returns the verbosity level.

=back 


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

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
