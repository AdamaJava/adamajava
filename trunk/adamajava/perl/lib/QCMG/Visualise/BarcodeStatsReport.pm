package QCMG::Visualise::BarcodeStatsReport;

##############################################################################
#
#  Module:   QCMG::Visualise:BarcodeStatsReport.pm
#  Author:   John V Pearson
#  Created:  2011-10-13
#
#  Parse and visualise the BarcodeStatsReport XML element from
#  solidstatsreport XML files.
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
use QCMG::Lifescope::Cht;
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
    die "xmlnode must be a BarcodeStatsReport Element not [$name]"
        unless ($name eq 'BarcodeStatsReport');

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

    qlogprint( "Processing BarcodeStatsReport\n" ) if $self->verbose;

    # Create tab for this LifeScopeDirectory and output info
    my $tab = QCMG::HTML::Tab->new( id => 'Barcodes' );
    $self->{tab} = $tab;

    $self->_parse_details();
    $self->_parse_barcode_tables();
}


sub _parse_details {
    my $self = shift;

    # If file is a pathname, split it into directory and filename
    my $file = $self->xmlnode->getAttribute('file');
    if ($file =~ /^(.*)\/([^\/]+)$/) {
       $self->{dir}  = $1;
       $self->{file} = $2;
    }

    my @nodes = $self->xmlnode->findnodes( 'BarcodeInfo/MissingBarcodeReads' );
    $self->{missingbarcodereads} = $nodes[0]->textContent;
    @nodes = $self->xmlnode->findnodes( 'BarcodeInfo/MissingF3Reads' );
    $self->{missingf3reads} = $nodes[0]->textContent;
}


sub _parse_barcode_tables {
    my $self = shift;

    my @xmls = $self->xmlnode->findnodes( 'Table' );

    foreach my $xml (@xmls) {
        my $rh_table = parse_xml_table( $xml );
        my ($chart,$table) = $self->_piechart_and_table( $rh_table );

        my $tab = QCMG::HTML::Tab->new( id => $rh_table->{name} );

        $tab->add_content( "<p class=\"header\">\n" );
        $tab->add_content( '<i>File:</i> '. $self->{file} ."\n" );
        $tab->add_content( '<br><i>Dir:</i> '. $self->{dir} ."\n" );
        $tab->add_content( "</p>\n" );

        #$tab->add_content( "<table><tr><td style=\"width:800\">\n" );
        #$tab->add_content( "<table><tr><td>\n" );
        $tab->add_content( "<table><tr><td style=\"vertical-align:top\">\n" );
        # This is where the Google Chart will go
        $tab->add_content( '<div id="' . $chart->javascript_div_name .
                           "\"></div>\n" );
        $tab->add_content( "</td><td style=\"vertical-align:top\">\n" );
        # This is where the Google Table will go
        $tab->add_content( '<div id="' . $table->javascript_div_name .
                           "\"></div>\n" );
        $tab->add_content( "</td></tr></table>\n" );

        $self->tab->add_subtab( $tab );
    }
}


sub _parse_barcode_tables_old {
    my $self = shift;

    my @xmls = $self->xmlnode->findnodes( 'Table' );

    foreach my $xml (@xmls) {
        my $rh_table = parse_xml_table( $xml );
        my $tab = QCMG::HTML::Tab->new( id => $rh_table->{name} );

        $tab->add_content( "<table class=\"qcmgtable\">\n");
        $tab->add_content( "<tr>");
        $tab->add_content( "<th>$_</th>" ) foreach @{$rh_table->{headers}};
        $tab->add_content( "</tr>\n");
        foreach my $row (@{$rh_table->{data}}) {
            $tab->add_content( "<tr>");
            $tab->add_content( "<td>$_</td>" ) foreach @{$row};
            $tab->add_content( "</tr>\n");
        }
        $tab->add_content( "</table>\n");

        $self->tab->add_subtab( $tab );
    }
}


sub _piechart_and_table {
    my $self     = shift;
    my $rh_table = shift;

    # First we'll do the Table
    my $table = QCMG::Google::Chart::Table->new( name => $rh_table->{name} );
    my $ctr = 1;
    foreach my $header (@{$rh_table->{headers}}) {
        $table->add_col( $ctr++, $header, 'string' );
    }
    foreach my $row (@{$rh_table->{data}}) {
        $table->add_row( @{$row} );
    }

    my $chart = QCMG::Google::Chart::PieChart->new( name => $rh_table->{name} );
    $chart->add_col( 1, 'Barcode', 'string' );
    $chart->add_col( 2, 'Reads', 'number' );

    # The way we form the category labels is a bit different for the 2
    # tables - BarcodeTotals and BarcodeCounts

    foreach my $row (@{$rh_table->{data}}) {
        my @fields = @{$row};

        if ($rh_table->{name} eq 'BarcodeTotals') {
            # Don't include 'All Beads'
            next if ($fields[0] =~ /All Beads/i);
            $chart->add_row( $fields[0], $fields[4] );
        }
        elsif ($rh_table->{name} eq 'BarcodeCounts') {
            $chart->add_row( $fields[0].'-'.$fields[1], $fields[4] );
        }
        else {
            croak "Can't parse a barcodes table of type [". 
                  $rh_table->{name} .']';
        }
    }
    $chart->title( 'Barcodes' );
    $chart->width( 600 );
    $chart->height( 600 );

    $self->charts->add_chart( $chart );
    $self->charts->add_chart( $table );

    return $chart, $table;
}


sub _parse_sequencing_runs {
    my $self = shift;

    my @runs = $self->xmlnode->findnodes( $self->{xmlstem}.
    '/sequencingRuns/list/com.apldbio.aga.common.model.objects.SequencingRun' );

    # Initialise output matrix 
    my @terms = qw{ primerSet/name 
                    dateStarted dateCompleted numBases numPrimers index id
                    primerSet/version primerSet/primerBase primerSet/id
                    probeSet/name probeSet/primerOrder };

    # Now do the parsing - assumes one of everything in the XML
    my %vals = ();
    foreach my $ctr (0..$#runs) {
        my $run = $runs[$ctr];
        foreach my $term (@terms) {
            my $node = $run->findnodes( $term )->[0];
            $vals{$term}->[$ctr] = defined $node ? $node->textContent : '';
        }
    }

    # Create table with columns and rows
    my $table = QCMG::Google::Chart::Table->new( name => 'SequencingRuns' );
    my $idctr = 1;
    $table->add_col( $idctr++, 'Fields', 'string' );
    foreach my $ctr (0..$#runs) {
        $table->add_col( $idctr++, $vals{'primerSet/name'}->[$ctr], 'string');
    }
    foreach my $term (@terms) {
        $table->add_row( $term, @{$vals{$term}} );
    }
    $self->charts->add_chart( $table );

    my $tab = QCMG::HTML::Tab->new( id => 'SequencingRuns' );
    $self->_add_header( $tab );

    # This is where the Google Table will go
    $tab->add_content( '<div id="' . $table->javascript_div_name .
                       "\"></div>\n" );

    $self->tab->add_subtab( $tab );
}


sub _add_header {
    my $self = shift;
    my $tab  = shift;

    $tab->add_content( "<p class=\"header\">\n" );
    $tab->add_content( '<i>File: </i>'. $self->{file} ."\n" ) if $self->{file};
    $tab->add_content( '<br><i>Dir: </i>'. $self->{dir} ."\n" ) if $self->{dir};
    $tab->add_content( '<br><i>Protocol Name: </i>'. $self->{protocolname}.
                       ' &nbsp;&nbsp;&nbsp; ' .
                       '<i>Flowcell Number: </i>'. $self->{flowcellnum}.
                       "</p>\n" );
}

1;

__END__


=head1 NAME

QCMG::Visualise::BarcodeStatsReport - Perl module for parsing Barcode
statistics data from SolidStatsReport XML reports


=head1 SYNOPSIS

 use QCMG::Visualise::BarcodeStatsReport;

 my $report = QCMG::Visualise::BarcodeStatsReport->new( file => 'report.xml' );
 print $report->as_html( 'report.html' );


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

Copyright (c) The University of Queensland 2011,2012

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
