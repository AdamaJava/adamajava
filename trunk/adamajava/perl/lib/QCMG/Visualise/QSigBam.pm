package QCMG::Visualise::QSigBam;

##############################################################################
#
#  Module:   QCMG::Visualise:QSigBam.pm
#  Author:   John V Pearson
#  Created:  2012-02-19
#
#  Parse and visualise the BAM file XML element from qsignature XML files.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use XML::LibXML;

use QCMG::Google::Charts;
use QCMG::HTML::TabbedPage;
use QCMG::HTML::Tab;
use QCMG::Util::QLog;
use QCMG::Util::XML qw( get_attr_by_name get_node_by_name );
use QCMG::Visualise::Util qw( parse_xml_table );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    my $class  = shift;
    my %params = @_;

    # Must pass xmlnode : XML::LibXML::Element
    # Must pass charts : QCMG::Google::ChartCollection

    die 'You must supply an XML::LibXML::Element object as '.
         ' xmlnode parameter to new()'
        unless (exists $params{xmlnode});

    my $type = ref($params{xmlnode});
    die "xmlnode must refer to a XML::LibXML::Element object not [$type]"
        unless ($type eq 'XML::LibXML::Element');

    my $name = $params{xmlnode}->nodeName;
    die "xmlnode must be a BAMFiles Element not [$name]"
        unless ($name eq 'BAMFiles');

    my $self = { xmlnode => $params{xmlnode},
                 charts  => $params{charts},  # QCMG::Google::ChartCollection
                 tab     => undef,  # QCMG::HTML::Tab
                 dir     => '',
                 file    => '',
                 verbose => $params{verbose} || 0 };
    bless $self, $class;
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub charts {
    my $self = shift;
    return $self->{charts};
}


sub tab {
    my $self = shift;
    return $self->{tab};
}


sub xmlnode {
    my $self = shift;
    return $self->{xmlnode};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub html {
    my $self = shift;
    return $self->tab->as_html;
}   
 
 
sub process {
    my $self = shift;

    qlogprint( "Processing qsignature file\n" ) if $self->verbose;

    # Create tab for this QSigBam and output info
    my $tab = QCMG::HTML::Tab->new( id => 'Files' );
    $self->{tab} = $tab;

    $self->_parse_summary();
    $self->_parse_bams();
}


sub _parse_summary {
    my $self = shift;

    my $tab = QCMG::HTML::Tab->new( id => 'Summary' );

    my @nodes = $self->xmlnode->findnodes( 'BAM' );
    my %subtabs = ();
    foreach my $node (@nodes) {
        my @fields = ();
        my $id = get_attr_by_name( $node, 'id' );
        push @fields, get_attr_by_name( $node, 'id' );
        push @fields, get_attr_by_name( $node, 'patient' );
        push @fields, get_attr_by_name( $node, 'inputType' );
        push @fields, get_attr_by_name( $node, 'bam' );
        $subtabs{ $id } = \@fields;
    }

    $tab->add_content( "<br><table class=\"qcmgtable\">\n" );
    $tab->add_content(
    "<tr><th>ID</th><th>Patient</th><th>Type</th><th>File</th></tr>\n" );
    foreach my $id (sort {$a<=>$b} keys %subtabs) {
        $tab->add_content( '<tr><td>', $subtabs{$id}->[0], 
                           '</td><td>', $subtabs{$id}->[1],
                           '</td><td>', $subtabs{$id}->[2],
                           '</td><td>', $subtabs{$id}->[3], "</td></tr>\n" );
    }
    $tab->add_content( "</table><br>\n" );

    $self->tab->add_subtab( $tab );
}


sub _parse_bams {
    my $self = shift;

    my @nodes = $self->xmlnode->findnodes( 'BAM' );
    foreach my $node (@nodes) {

        my @fields = ();
        my $id = get_attr_by_name( $node, 'id' );
        my $tab = QCMG::HTML::Tab->new( id => $id );

        my $chart = QCMG::Google::Chart::Column->new( name => "bam_cov_$id" );
        $chart->add_col( 1, 'Coverage cutoff', 'string' );
        $chart->add_col( 2, 'SNP Count', 'number' );

        my @covs = split ',', get_attr_by_name( $node, 'coverage' );
        foreach my $cov (@covs) {
            $chart->add_row( split( ':', $cov ) );
        }

        $chart->title( 'Counts of SNPs at coverage levels' );
        $chart->width( 800 );
        $chart->height( 800 );

        $tab->add_content( '<p class="header">' .
                           '<i>Patient: </i><b>' . 
                           get_attr_by_name( $node, 'patient' ) .'</b>' .
                           ' &nbsp;&nbsp;&nbsp; '.
                           '<i>Type: </i><b>' . 
                           get_attr_by_name( $node, 'inputType' ) .'</b>' .
                           ' &nbsp;&nbsp;&nbsp; '.
                           '<i>Library: </i><b>' . 
                           get_attr_by_name( $node, 'library' ) ."</b><br>\n".
                           '<i>BAM: </i>' .
                           get_attr_by_name( $node, 'bam' ) . "<br>\n" .
                           '<i>SNP File: </i>' .
                           get_attr_by_name( $node, 'snpFile' ) . "<p>\n" );

        $tab->add_content( $chart->javascript_div_html );

        $self->charts->add_chart( $chart );
        $self->tab->add_subtab( $tab );

    }
}


1;

__END__


=head1 NAME

QCMG::Visualise::QSigBam - Perl module for parsing BAM data
from qsignature XML reports


=head1 SYNOPSIS

 use QCMG::Visualise::QSigCompare;


=head1 DESCRIPTION


=head1 PUBLIC METHODS

=over

=item B<new()>

=item B<process()>

=item B<html()>

=item B<verbose()>

=back 


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


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
