package QCMG::Visualise::qProfiler;

##############################################################################
#
#  Module:   QCMG::Visualise::qProfiler.pm
#  Author:   John V Pearson
#  Created:  2013-03-07
#
#  Read a qprofiler XML report and create a HTML file that uses the Google
#  chart API to display graphs and summary tables describing the coverage.
#
#  $Id: qProfiler.pm 4666 2014-07-24 09:03:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;

#use Carp qw( carp croak confess );
use Data::Dumper;
use POSIX qw(floor);
use XML::LibXML;

use QCMG::Google::Charts;
use QCMG::HTML::TabbedPage;
use QCMG::HTML::Tab;
use QCMG::Util::QLog;
use QCMG::Util::XML qw( get_attr_by_name get_node_by_name );
use QCMG::Visualise::Coverage;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4666 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qProfiler.pm 4666 2014-07-24 09:03:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

sub new {
    my $class  = shift;
    my %params = @_;

    die "You must supply one of file/xmltext/xmlnode to new()"
        unless (exists $params{file} or
                exists $params{xmltext} or
                exists $params{xmlnode});

    my $self = { file     => $params{file} || '',
                 xmltext  => '',
                 xmlnode  => undef,
                 charts   => QCMG::Google::ChartCollection->new(),
                 page     => QCMG::HTML::TabbedPage->new(),
                 features => {},
                 verbose  => $params{verbose} || 0 };
    bless $self, $class;

    # Setup page including enabling Google charts API
    $self->page->use_google_charts(1);
    $self->page->title( 'QCMG qProfiler' );

    # Ultimately we need a XML::LibXML::Element but we could have been
    # passed an Element object, a filename or a text blob.  In the latter
    # two cases, we need to create an XML node from the file or text.

    if (exists $params{xmlnode}) {
        my $type = ref($params{xmlnode});
        die "xmlnode parameter must refer to a XML::LibXML::Element object not [$type]"
            unless ($type eq 'XML::LibXML::Element');
        my $name = $params{xmlnode}->nodeName;
        die "xmlnode parameter must be a qProfiler Element not [$name]"
            unless ($name =~ /qProfiler/);
        $self->{xmlnode} = $params{xmlnode};
    }
    elsif (exists $params{xmltext}) {
        my $xmlnode = undef;
        eval { $xmlnode = XML::LibXML->load_xml( string => $params{xmltext} ); };
        die $@ if $@;
        $self->{xmlnode} = $xmlnode;
    }
    elsif (exists $params{file}) {
        my $xmlnode = undef;
        eval { $xmlnode = XML::LibXML->load_xml( location => $params{file} ); };
        die $@ if $@;
        $self->{xmlnode} = $xmlnode;
    }
    else {
        die "Uh oh - should not be able to get here!";
    }

    return $self;
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub charts {
    my $self = shift;
    return $self->{charts};
}


sub page {
    my $self = shift;
    return $self->{page};
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
    return $self->page->as_html;
}


sub process {
    my $self = shift;

    my $charts = $self->charts;  # QCMG::Google::ChartCollection object
    my $page   = $self->page;    # QCMG::HTML::TabbedPage object
    my $qprof  = $self->xmlnode;
    my @nodes  = $qprof->childNodes;

    my $root = undef;
    foreach my $node (@nodes) {
        if ( $node->nodeName =~ /BamReport/i ) {
            $root = $node;
            last;
        }
    }
    die "No BAMReport element found under qProfiler\n"
        unless defined $root;

    # Extract all of the reportable units
    my @headers   = $root->findnodes( 'HEADER' );
    my @seqs      = $root->findnodes( 'SEQ' );
    my @quals     = $root->findnodes( 'QUALS' );
    my @tags      = $root->findnodes( 'TAG' );
    my @isizes    = $root->findnodes( 'ISIZE' );
    my @rnexts    = ( $root->findnodes( 'RNEXT' ),
                      $root->findnodes( 'MRNM' ) );
    my @cigar     = $root->findnodes( 'CIGAR' );
    my @rname_pos = $root->findnodes( 'RNAME_POS' );
    my @flags     = $root->findnodes( 'FLAG' );

    print Dumper \@headers;
    die;

    # Problematically we can't create the Summary tab (which should go
    # first) until we have done all of the other tabs so we can get the
    # totals.  This means we will create all of the other tabs but put
    # them in a temporary array and then we'll create the summary tab
    # and add_subtab() all of the tabs in the temporary list.

    # Start parsing all of the reports into Google::Charts
    my @tabs = ();
    foreach my $header (@headers) {
        push @tabs, $self->parse_cov( $header );
    }
    # Now we sort the tabs alphabetically
    my @sorted_tabs = map  { $_->[0] }
                      sort { $a->[1] cmp $b->[1] }
                      map  { [ $_, $_->id ] }
                      @tabs;

    # Add summary tab and all of the stored tabs
    $page->add_subtab( $self->summary_tab );
    foreach my $tab (@sorted_tabs) {
        $page->add_subtab( $tab );
    }

    # If we've parsed all of the reports then all our charts should be
    # ready so we can add the javascript block
    $page->add_content( $charts->javascript );

    # If we know the file name then include it
    if ($self->file) {
        $page->add_content( '<div class="header">File: '. $self->file.
                            "<br><br></div>\n" );
    }
}


sub parse_cov {
    my $self = shift;
    my $node = shift;

    my $cov = QCMG::Visualise::Coverage->new( node => $node,
                                              verbose => $self->verbose );
    my $tab = QCMG::HTML::Tab->new( id => $cov->feature );

    qlogprint( 'Processing coverage for '. $cov->feature ."\n" )
        if $self->verbose; 

    # Create our basic coverage chart

    my $chart1 = QCMG::Google::Chart::Bar->new( name => $cov->feature.'_cov' );
    $chart1->title( 'Count of bases at a given coverage level' );
    $chart1->add_col( 'bin', 'bin', 'string' );
    $chart1->add_col( 'coverage', 'coverage', 'number' );
    foreach my $row (@{$cov->data}) {
        $chart1->add_row( @{ $row } );
        #debugging: qlogprint( join("\t",@{$row}), "\n" );
    }
    $chart1->table->add_percentage(1);
    $chart1->table->trim(0.99,2);
    $chart1->table->bin;  # default 50 bins is what we want
    $chart1->table->add_format(2,'%.4f');  # 4 decimal places
    $chart1->myparams->{hAxis}->{logscale} = "'true'";
    $chart1->myparams->{hAxis}->{title} = "'bases at each coverage level'";
    $chart1->myparams->{vAxis}->{title} = "'coverage bins'";
    $chart1->myparams->{chartArea}->{left} = 100;
    $chart1->width(700);
    $chart1->height(1000);

    # Add a Chart::Table so we can have the Table visualisation if we choose

    my $table1 = QCMG::Google::Chart::Table->doppelganger( $chart1,
                      name => 'doppelganger_of_' . $chart1->name );
    $table1->height( 800 );
    $table1->width( 300 );

    # Now add the cumulative coverage plot
    
    my $chart2 = QCMG::Google::Chart::Line->new( name => $cov->feature.'_cumul' );
    $chart2->title( 'Percentage of bases at a given coverage level or higher' );
    $chart2->add_col( 'bin', 'bin', 'string' );
    $chart2->add_col( 'cumul coverage', 'cumul coverage', 'number' );
    $chart2->add_col( '80%', '80%', 'number' );
    $chart2->add_col( 'coverage', 'coverage', 'number' );
    my $cumulative_cov = 0;
    my @cov_rows = ();  # We'll parse these again later
    foreach my $row (@{$cov->data}) {
        my $cov_at_this_or_more = 1 - ($cumulative_cov / $cov->region_length);
        $chart2->add_row( $row->[0], $cov_at_this_or_more, 0.8, $row->[1] );
        push @cov_rows, [ $row->[0], $cov_at_this_or_more, $row->[1] ];
        $cumulative_cov += $row->[1];
    }
    # We're going to add percentage but only for trimming - we'll pull it
    # off for the final plotting
    $chart2->table->add_percentage(3);
    $chart2->table->trim(0.95, 4, 'cumul_max', '_..++');
    $chart2->table->drop_col(4);
    $chart2->table->drop_col(3);

    # Parse @cov_rows looking for key coverage criteria and bury them in
    # the HTML document as <meta> tags in the page <head>.
    my @key_covs = ();
    foreach my $ctr (0..($#cov_rows-1)) {
        if ($cov_rows[$ctr]->[1] >= 0.9 and $cov_rows[$ctr+1]->[1] < 0.9) {
            push @key_covs, '0.90@'. $cov_rows[$ctr]->[0];
        }
        if ($cov_rows[$ctr]->[1] >= 0.8 and $cov_rows[$ctr+1]->[1] < 0.8) {
            push @key_covs, '0.80@'. $cov_rows[$ctr]->[0];
        }
        if (($cov_rows[$ctr]->[0] eq '10') or ($cov_rows[$ctr]->[0] eq '20')) {
            push @key_covs, sprintf('%.2f',$cov_rows[$ctr]->[1]).
                            '@'. $cov_rows[$ctr]->[0];
        }
    }

    # Add average coverage
    push @key_covs, 'avg@'.sprintf('%.2f',$cov->average_coverage);

    # Push key coverages into a META element in page header
    $self->page->add_meta( 'coverage_'. $cov->feature, join(',',@key_covs) );

    $chart2->myparams->{hAxis}->{title} = "'coverage bins'";
    $chart2->myparams->{vAxis}->{title} = "'% bases at given or higher coverage level'";
    $chart2->myparams->{chartArea}->{left} = 100;
    $chart2->width(1200);
    $chart2->height(600);

    # Add new charts to collection and to page
    $self->charts->add_chart( $chart1 );
    $self->charts->add_chart( $table1 );
    $self->charts->add_chart( $chart2 );
    $tab->add_subtab( chart_with_table_to_tab( $chart1, $table1, 'Coverage' ) );
    $tab->add_subtab( chart_to_tab( $chart2, 'Cumulative Coverage' ) );

    # If we are to build the summary table for the front page then we
    # are going to need to keep summary stats for each feature ...
    $self->add_feature( $cov );

    return $tab;
}


sub add_feature {
    my $self = shift;
    my $cov  = shift;  # QCMG::Visualise::Coverage object

    my $type = ref($cov);
    die 'add_feature() must be passed an object of type '.
          "QCMG::Visualise::Coverage, not [$type]"
        unless ($type eq 'QCMG::Visualise::Coverage');
    if (exists $self->{features}->{$cov->feature}) {
        warn "Cannot add a second coverage object with name".  $cov->feature;
        return undef;
    }
    else {
        $self->{features}->{$cov->feature} = $cov;
    }
}


# Non-OO
sub chart_to_tab {
    my $chart = shift;  # QCMG::Google::Chart::* object
    my $title = shift;  # string

    my $tab = QCMG::HTML::Tab->new( id => $title );

    $tab->add_content( '<div id="'.$chart->javascript_div_name ."\"></div>\n" );

    return $tab;
}


# Non-OO.  Assumes that the Table is a doppelganger of the Chart.  Must do
# this so that we can predict the name of the single javascript datatable

sub chart_with_table_to_tab {
    my $chart = shift;  # QCMG::Google::Chart::* object
    my $table = shift;  # QCMG::Google::Chart::Table object
    my $title = shift;  # string

    my $tab = QCMG::HTML::Tab->new( id => $title );

    # Setup div contents as 2-cell table with Google Chart in the left
    # (first) cell and the text table in the second (right) cell
    
    $tab->add_content( "<table><tr><td style=\"width:800\">\n" );

    $tab->add_content( '<div id="'. $chart->javascript_div_name .
                       "\"></div>\n" );

    $tab->add_content( "</td><td style=\"vertical-align:top\">\n" );

    $tab->add_content( '<div id="'. $table->javascript_div_name .
                       "\"></div>\n" );

    $tab->add_content( "</td></tr></table>\n" );

    return $tab;
}


sub summary_tab {
    my $self = shift;

    my $tab = QCMG::HTML::Tab->new( id => 'Summary' );
    
    $tab->add_content( "<div style=\"font-family:arial,san-serif\"><br>\n" );

    my @summary_columns = ( 'Feature type',
                            'Basepairs in feature type',
                            '% of total basepairs',
                            'Sequenced base in feature type',
                            '% of total sequenced bases',
                            'Average per-basepair coverage' );

    $tab->add_content( '<table class="qcmgtable"><tr>', "\n" );
    foreach my $text (@summary_columns) {
        $tab->add_content( "<th>$text<\/th>\n" );
    }
    $tab->add_content( "</tr>\n" );

    # Do totals calcs first so they can be used as denominators
    my $total_bases_in_bam  = 0;
    my $total_bases_in_span = 0;
    foreach my $id (sort keys %{ $self->{features} }) {
        my $feature = $self->{features}->{ $id };
        $total_bases_in_bam  += $feature->bases_of_sequence;
        $total_bases_in_span += $feature->region_length;
    }

    # Do per-feature calcs using totals calcs
    foreach my $id (sort keys %{ $self->{features} }) {
        my $feature = $self->{features}->{ $id };

        my $span_as_pc_of_total = $feature->region_length * 100 /
                                  $total_bases_in_span;
        my $coverage_as_pc_of_total = $feature->bases_of_sequence * 100 /
                                      $total_bases_in_bam;
        $tab->add_content( '<tr><td>',
                  join("<\/td><td>",
                       $feature->feature,
                       $feature->region_length,
                       sprintf( "%.2f", $span_as_pc_of_total ),
                       $feature->bases_of_sequence,
                       sprintf( "%.2f", $coverage_as_pc_of_total ),
                       sprintf( "%.2f", $feature->average_coverage ) ),
                  "<\/td><\/tr>\n" );
    }

    # Add in totals row
    $tab->add_content( "<tr><td>",
                       join("<\/td>\n<td>",
                            'Totals',
                            $total_bases_in_span,
                            '100.00',
                            $total_bases_in_bam,
                            '100.00',
                            sprintf( "%.2f", $total_bases_in_bam /
                                             $total_bases_in_span) ),
                       "<\/td><\/tr>\n",
                       "</table>\n" );
    $tab->add_content( "</div>\n" );

    return $tab;
}


1;
__END__


=head1 NAME

QCMG::Visualise::qCoverage - Perl module for creating HTML pages
from qCoverage XML reports


=head1 SYNOPSIS

 use QCMG::Visualise::qCoverage;

 my $report = QCMG::Visualise::qCoverage->new( file => 'report.xml' );
 print $report->as_html( 'report.html' );


=head1 DESCRIPTION


=head1 PUBLIC METHODS

=over

=item B<new()>

=item B<file()>

=item B<as_html()>

=item B<verbose()>

=back 


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qProfiler.pm 4666 2014-07-24 09:03:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

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
