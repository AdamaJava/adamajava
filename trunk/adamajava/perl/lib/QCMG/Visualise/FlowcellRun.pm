package QCMG::Visualise::FlowcellRun;

##############################################################################
#
#  Module:   QCMG::Visualise:FlowcellRun.pm
#  Author:   John V Pearson
#  Created:  2011-10-12
#
#  Parse and visualise the FlowcellRun XML file that can appear in 
#  solidstatsreport XML files.
#
#  $Id: FlowcellRun.pm 4666 2014-07-24 09:03:04Z j.pearson $
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

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4666 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: FlowcellRun.pm 4666 2014-07-24 09:03:04Z j.pearson $'
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
    die "xmlnode must be a FlowCellRun Element not [$name]"
        unless ($name eq 'FlowCellRun');

    my $self = { xmlnode => $params{xmlnode},
                 charts  => $params{charts},  # QCMG::Google::ChartCollection
                 xmlstem => 'com.apldbio.aga.common.model.objects.FlowcellRun',
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

    qlogprint( "Processing FlowCellRun\n" ) if $self->verbose;

    # Create tab for this LifeScopeDirectory and output info
    my $tab = QCMG::HTML::Tab->new( id => 'FlowcellRun' );
    $self->{tab} = $tab;

    $self->_parse_details();
    $self->_parse_protocol_xml();
    $self->_parse_sequencing_runs();
}


sub _parse_details {
    my $self = shift;

    # If file is a pathname, split it into directory and filename
    my $file = $self->xmlnode->getAttribute('file');
    if ($file =~ /^(.*)\/([^\/]+)$/) {
       $self->{dir}  = $1;
       $self->{file} = $2;
    }

    my @flowcellnums = $self->xmlnode->findnodes(
                           $self->{xmlstem}.'/flowcellNum' );
    my @protnames    = $self->xmlnode->findnodes(
                           $self->{xmlstem}.'/protocolName' );

    $self->{flowcellnum}  = $flowcellnums[0]->textContent;
    $self->{protocolname} = $protnames[0]->textContent;
}


sub _parse_protocol_xml {
    my $self = shift;

    my @xmls = $self->xmlnode->findnodes( $self->{xmlstem}.'/protocolXML' );

    foreach my $xml (@xmls) {
        my $tab = QCMG::HTML::Tab->new( id => 'ProtocolXML' );
        $self->_add_header( $tab );
        # Process XML text into viewable HTML <pre></pre>
        my $xmltext = $xml->textContent;
        $xmltext =~ s/\r//g;
        $xmltext =~ s/\</&lt;/g;
        $xmltext =~ s/\>/&gt;/g;
        $tab->add_content( "<pre>\n$xmltext</pre>\n" );
        $self->tab->add_subtab( $tab );
    }
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

QCMG::Visualise::FlowcellRun - Perl module for parsing Flowcell data
from SolidStatsReport XML reports


=head1 SYNOPSIS

 use QCMG::Visualise::SolidStatsReport;

 my $report = QCMG::Visualise::SolidStatsReport->new( file => 'report.xml' );
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

$Id: FlowcellRun.pm 4666 2014-07-24 09:03:04Z j.pearson $


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
