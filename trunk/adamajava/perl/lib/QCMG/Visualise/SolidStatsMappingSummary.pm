package QCMG::Visualise::SolidStatsMappingSummary;

##############################################################################
#
#  Module:   QCMG::Visualise::SolidStatsMappingSummary.pm
#  Author:   John V Pearson
#  Created:  2011-11-29
#
#  Read an XML report file from summarise_solid_stats.pl and create a HTML
#  file that uses the Google chart API to display graphs and summary tables
#  describing the mapping.
#
#  $Id: SolidStatsMappingSummary.pm 4666 2014-07-24 09:03:04Z j.pearson $
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
use QCMG::Util::Data qw( sequencers );
use QCMG::Util::QLog;
use QCMG::Util::XML qw( get_attr_by_name get_node_by_name );
use QCMG::Visualise::Coverage;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4666 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: SolidStatsMappingSummary.pm 4666 2014-07-24 09:03:04Z j.pearson $'
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

    my $self = { file      => $params{file} || '',
                 xmltext   => '',
                 xmlnode   => undef,
                 charts    => QCMG::Google::ChartCollection->new(),
                 page      => QCMG::HTML::TabbedPage->new(),
                 features  => {},
                 summary   => {},
                 date_mode => 0,
                 verbose   => $params{verbose} || 0 };
    bless $self, $class;

    # Setup page including enabling Google charts API
    $self->page->use_google_charts(1);
    $self->page->title( 'QCMG Mapping Statistics' );

    # Ultimately we need a XML::LibXML::Element but we could have been
    # passed an Element object, a filename or a text blob.  In the latter
    # two cases, we need to create an XML node from the file or text.

    if (exists $params{xmlnode}) {
        my $type = ref($params{xmlnode});
        die "xmlnode parameter must refer to a XML::LibXML::Element object not [$type]"
            unless ($type eq 'XML::LibXML::Element');
        my $name = $params{xmlnode}->nodeName;
        die "xmlnode parameter must be a SolidStatsMappingSummary Element not [$name]"
            unless ($name =~ /SolidStatsMappingSummary/);
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


sub date_mode {
    my $self = shift;
    return $self->{date_mode} = shift if @_;
    return $self->{date_mode};
}


sub html {
    my $self = shift;
    return $self->page->as_html;
}


sub process {
    my $self = shift;

    my $charts = $self->charts;  # QCMG::Google::ChartCollection object
    my $page   = $self->page;    # QCMG::HTML::TabbedPage object
    my $root   = $self->xmlnode;

    $self->{summary}->{start_time} = get_attr_by_name( $root, 'start_time' );

    # In order to create the Summary tab (which should go first) we need
    # to parse all of the data.  We have two sets of data elements:
    $self->parse_files( $root );
    $self->parse_mapsets( $root );

    # Add summary tab and all of the stored tabs
    $page->add_subtab( $self->summary_tab );
    $page->add_subtab( $self->machines_tab );
    $page->add_subtab( $self->runtypes_tab );

    # If we've parsed all of the reports then all our charts should be
    # ready so we can add the javascript block
    $page->add_content( $charts->javascript );

    # If we know the file name then include it
    if ($self->file) {
        $page->add_content( '<div class="header">'.
                            'Data from: <i><b>'. $self->file .'</b></i>' .
                            '&nbsp;&nbsp; Created: <i><b>'.
                            $self->{summary}->{start_time} .'</b></i>' .
                            "<br><br></div>\n" );
    }
}


sub parse_files {
    my $self = shift;
    my $node = shift;

    my $match = 'ParsedSolidStatsReportFiles/SolidStatsReportFile';
    my @files = $node->findnodes( $match );

    foreach my $file (@files) {
        my $fileid   = get_attr_by_name( $file, 'fileid' );
        my $filename = get_attr_by_name( $file, 'filename' );
        my $project  = '';
        my $donor    = '';
        # Pull project and donor out of file path
        if ($filename =~ /\/seq_results\/([^\/]+)\/([^\/]+)/) {
           $project = $1;
           $donor   = $2;
        }
        else {
           warn "unable to parse project and donor from file $filename\n";
        }
        $self->{files}->{ $fileid } = { fileid   => $fileid,
                                        filename => $filename,
                                        project  => $project,
                                        donor    => $donor };
    }
}


sub parse_mapsets {
    my $self = shift;
    my $node = shift;

    my $match   = 'MapsetMappings/MapsetMapping';
    my @mapsets = $node->findnodes( $match );

    my @tags = qw( BioscopeVersion FileID MappedFulllengthNoerrorPercent
                   MappedReads MappedReadsGigabase MappedReadsPercent
                   MappingStartDate RunID StartPointsMapped StartPointsUnique
                   Tag TotalTags UniqueReadsGigabase Filename URL );
    my @details = qw( MapsetName PhysicalDivision Barcode Slide
                      Sequencer RunDate SlideNumber RunType );

    foreach my $mapset (@mapsets) {
        my %record = ();
        foreach my $tag (@tags) {
            my $lnode = get_node_by_name( $mapset, $tag );
            if (defined $lnode) {
                $record{ $tag } = $lnode->textContent;
            }
            else {
                $record{ $tag } = '';
            }
        }
        my $dnode = get_node_by_name( $mapset, 'MapsetDetails' );
        foreach my $detail (@details) {
            my $lnode = get_node_by_name( $dnode, $detail );
            if (defined $lnode) {
                $record{ $detail } = $lnode->textContent;
            }
            else {
                $record{ $detail } = '';
            }
        }

        push @{ $self->{mapsets} }, \%record;
    }
}


sub summary_tab {
    my $self = shift;

    qlogprint( "creating summary tab\n" ) if $self->verbose; 

    # Create a by-month data structure
    my %by_month = ();
    my $total_mapped_reads = 0;
    my $total_unique_reads = 0;
    foreach my $rh_mapset (@{ $self->{mapsets} }) {
        my $date = '2099-01';

        if ($self->date_mode == 0) {
            # Sort data by slide run date
            if ($rh_mapset->{RunDate} =~ /^(20\d{2})(\d{2}).*$/) {
                $date = $1 . '-' . $2;
            }
        }
        elsif ($self->date_mode == 1) {
            # Sort data by mapping date
            if ($rh_mapset->{MappingStartDate} =~ /^(\d{4}\-\d{2}).*$/) {
                $date = $1;
            }
        }
        else {
            warn 'date_mode '. $self->{date_mode} ." is not supported\n";
        }

        $by_month{ $date }->{mapped_reads} +=
            $rh_mapset->{MappedReadsGigabase};
        $by_month{ $date }->{unique_reads} +=
            $rh_mapset->{UniqueReadsGigabase};
        $total_mapped_reads += $rh_mapset->{MappedReadsGigabase};
        $total_unique_reads += $rh_mapset->{UniqueReadsGigabase};
    }

    # Save summary data for later use
    $self->{summary}->{total_mapped_reads} = $total_mapped_reads;
    $self->{summary}->{total_unique_reads} = $total_unique_reads;

    my $chart1 = QCMG::Google::Chart::Bar->new( name => 'Summary' );
    $chart1->title( 'Breakdown of bases mapped by month' );
    $chart1->add_col( 'Month', 'Month', 'string' );
    $chart1->add_col( 'MappedReadsGB', 'MappedReadsGB', 'number' );
    $chart1->add_col( 'UniqueReadsGB', 'UniqueReadsGB', 'number' );
    foreach my $date (sort keys %by_month) {
        $chart1->add_row( $date,
                          $by_month{ $date }->{mapped_reads},
                          $by_month{ $date }->{unique_reads} );
    }
    $chart1->table->add_format(2,'%.2f');  # 2 decimal places
    $chart1->table->add_format(3,'%.2f');  # 2 decimal places
    $chart1->myparams->{chartArea}->{left} = 100;
    $chart1->myparams->{vAxis}->{title} = "'Months'";
    $chart1->myparams->{hAxis}->{title} = "'Gigabases'";
    $chart1->width(700);
    $chart1->height(1000);

    # Add a Chart::Table so we can have the Table visualisation if we choose

    my $table1 = QCMG::Google::Chart::Table->doppelganger( $chart1,
                      name => 'doppelganger_of_' . $chart1->name );
    $table1->height( 800 );
    $table1->width( 400 );

    # Add new charts to collection and to page
    $self->charts->add_chart( $chart1 );
    $self->charts->add_chart( $table1 );

    my $tab = QCMG::HTML::Tab->new( id => 'Summary' );

    # Setup div contents as 2-cell table with Google Chart in the left
    # (first) cell and the text table in the second (right) cell
    
    $tab->add_content( "<table><tr><td style=\"width:800\">\n" );

    $tab->add_content( '<div id="'. $chart1->javascript_div_name .
                       "\"></div>\n" );

    $tab->add_content( "</td><td style=\"vertical-align:top\">\n" );

    $tab->add_content( "<br>\n<table class='qcmgtable'>\n".
                       "<tr><td>Total Mapped Gigabases</td><td>" .
                       sprintf("%.2f",$self->{summary}->{total_mapped_reads}).
                       "</td></tr>\n" .
                       "<tr><td>Total Unique Mapped Gigabases</td><td>" .
                       sprintf("%.2f",$self->{summary}->{total_unique_reads}).
                       "</td></tr>\n</table>\n<br><br>\n" );

    $tab->add_content( '<div id="'. $table1->javascript_div_name .
                       "\"></div>\n" );

    $tab->add_content( "</td></tr></table>\n" );

    return $tab;
}


sub machines_tab {
    my $self = shift;

    qlogprint( "creating by-machine stats\n" ) if $self->verbose; 

    # Create a by-month data structure.  Note that we are going to
    # collapse all throughput from non-QCMG sequencers.

    my $rh_qcmg_seqs = sequencers();  # hash of all QCMG sequencers
    my %by_machine   = ();
    my %my_months    = ();  # list of all months when sequencing was done
    my %total_mapped_reads = ();
    my %total_unique_reads = ();
    foreach my $rh_mapset (@{ $self->{mapsets} }) {
        my $date = '';
        if ($rh_mapset->{MappingStartDate} =~ /^(\d{4}\-\d{2}).*$/) {
            $date = $1;
        }
        else {
            $date = '2099-01';
        }

        my $sequencer = $rh_mapset->{Sequencer};
        $sequencer = 'Non-QCMG' if (! exists $rh_qcmg_seqs->{ $sequencer });

        $my_months{ $date }++;
        $by_machine{ $sequencer }->{ $date }->{mapped_reads} +=
            $rh_mapset->{MappedReadsGigabase};
        $by_machine{ $sequencer }->{ $date }->{unique_reads} +=
            $rh_mapset->{UniqueReadsGigabase};
        $total_mapped_reads{ $sequencer } += $rh_mapset->{MappedReadsGigabase};
        $total_unique_reads{ $sequencer } += $rh_mapset->{UniqueReadsGigabase};
    }

    # Now we need to create lots of tabs
    # 1. Machine tab to contain all of the other tabs
    # 2. by-machine summary tab
    # 3. tab for each machine showing output

    my $tab = QCMG::HTML::Tab->new( id => 'By Machine' );
    $tab->add_subtab( $self->machines_summary( \%total_mapped_reads ) );

    foreach my $sequencer (sort keys %by_machine) {
        my $chart1 = QCMG::Google::Chart::Bar->new( name => $sequencer );
        $chart1->title( 'Breakdown of bases mapped by month' );
        $chart1->add_col( 'Month', 'Month', 'string' );
        $chart1->add_col( 'MappedReadsGB', 'MappedReadsGB', 'number' );
        $chart1->add_col( 'UniqueReadsGB', 'UniqueReadsGB', 'number' );
        foreach my $date (sort keys %my_months) {
            my $mapped_reads = 0;
            my $unique_reads = 0;
            if (exists $by_machine{ $sequencer }->{ $date }) {
                $mapped_reads = $by_machine{ $sequencer }->{ $date }->{mapped_reads};
                $unique_reads = $by_machine{ $sequencer }->{ $date }->{unique_reads};
            }
            $chart1->add_row( $date, $mapped_reads, $unique_reads );
        }
        $chart1->table->add_format(2,'%.2f');  # 2 decimal places
        $chart1->table->add_format(3,'%.2f');  # 2 decimal places
        $chart1->myparams->{chartArea}->{left} = 100;
        $chart1->myparams->{vAxis}->{title} = "'Months'";
        $chart1->myparams->{hAxis}->{title} = "'Gigabases'";
        $chart1->width(700);
        $chart1->height(1000);

        # Add a Chart::Table so we can have the Table visualisation if we choose

        my $table1 = QCMG::Google::Chart::Table->doppelganger( $chart1,
                          name => 'doppelganger_of_' . $chart1->name );
        $table1->height( 800 );
        $table1->width( 400 );

        # Add new charts to collection and to page
        $self->charts->add_chart( $chart1 );
        $self->charts->add_chart( $table1 );

        my $stab = QCMG::HTML::Tab->new( id => $sequencer );

        # Setup div contents as 2-cell table with Google Chart in the left
        # (first) cell and the text table in the second (right) cell
    
        $stab->add_content( "<br><table><tr><td style=\"width:800\">\n" );

        $stab->add_content( '<div id="'. $chart1->javascript_div_name .
                            "\"></div>\n" );

        $stab->add_content( "</td><td style=\"vertical-align:top\">\n" );

        $stab->add_content( "<br>\n<table class='qcmgtable'>\n".
                           "<tr><td>Total Mapped Gigabases</td><td>" .
                           sprintf("%.2f",$total_mapped_reads{$sequencer}).
                           "</td></tr>\n" .
                           "<tr><td>Total Unique Mapped Gigabases</td><td>" .
                           sprintf("%.2f",$total_unique_reads{$sequencer}).
                           "</td></tr>\n</table>\n<br><br>\n" );

        $stab->add_content( '<div id="'. $table1->javascript_div_name .
                           "\"></div>\n" );

        $stab->add_content( "</td></tr></table>\n" );

        $tab->add_subtab( $stab );
    }

    return $tab;
}


sub machines_summary {
    my $self = shift;
    my $rh_total_mapped_reads = shift;

    my $tab = QCMG::HTML::Tab->new( id => 'Summary' );

    my $chart1 = QCMG::Google::Chart::PieChart->new( name => 'MachineSummary' );
    $chart1->title( 'Breakdown of bases mapped by machine' );
    $chart1->add_col( 'ID', 'ID', 'string' );
    $chart1->add_col( 'MappedReadsGB', 'MappedReadsGB', 'number' );
    foreach my $sequencer (sort keys %{ $rh_total_mapped_reads }) {
        $chart1->add_row( $sequencer,
                          $rh_total_mapped_reads->{ $sequencer } );
    }
    $chart1->table->add_format(2,'%.2f');  # 2 decimal places
    $chart1->myparams->{chartArea}->{left} = 100;
    $chart1->width(700);
    $chart1->height(700);

    # Add a Chart::Table so we can have the Table visualisation if we choose

    my $table1 = QCMG::Google::Chart::Table->doppelganger( $chart1,
                      name => 'doppelganger_of_' . $chart1->name );
    $table1->height( 800 );
    $table1->width( 400 );

    # Add new charts to collection and to page
    $self->charts->add_chart( $chart1 );
    $self->charts->add_chart( $table1 );

    # Setup div contents as 2-cell table with Google Chart in the left
    # (first) cell and the text table in the second (right) cell
    
    $tab->add_content( "<table><tr><td style=\"width:800\">\n" );

    $tab->add_content( '<div id="'. $chart1->javascript_div_name .
                        "\"></div>\n" );

    $tab->add_content( "</td><td style=\"vertical-align:top\">\n" );

    $tab->add_content( '<div id="'. $table1->javascript_div_name .
                       "\"></div>\n" );

    $tab->add_content( "</td></tr></table>\n" );

    return $tab;
}


sub runtypes_tab {
    my $self = shift;

    qlogprint( "creating by-runtype stats\n" ) if $self->verbose; 

    # Create a by-machinetype/by-slide data structure.

    my %data = ();
    foreach my $rh_mapset (@{ $self->{mapsets} }) {
        my $runtype = $rh_mapset->{RunType};
        my $slide   = $rh_mapset->{Slide};
        my $date    = '2099-01-01';
        my $mtype   = 'SOLiD';
        if ($rh_mapset->{Sequencer} =~ /^S\d{5}$/ or
            $rh_mapset->{Sequencer} =~ /^S8006$/) {
            $mtype = '5500XL';
        }
        if ($rh_mapset->{Slide} =~ /_(\d{8})_/) {
            $date = $1;
        }
        
        $data{ $mtype }->{ $runtype }->{ $slide }->{mapped_reads} +=
            $rh_mapset->{MappedReadsGigabase};
        $data{ $mtype }->{ $runtype }->{ $slide }->{unique_reads} +=
            $rh_mapset->{UniqueReadsGigabase};
        $data{ $mtype }->{ $runtype }->{ $slide }->{date} = $date;
    }

    # Now we need to create lots of tabs
    # 1. RunType tab to contain all of the other tabs
    # 2. Tab for each machine-type / runtype combination

    my $tab = QCMG::HTML::Tab->new( id => 'By Runtype' );

    foreach my $machtype (sort keys %data) {
        foreach my $runtype (sort keys %{$data{$machtype}}) {

            my $tabname = $machtype .'_'. $runtype;

            my $chart1 = QCMG::Google::Chart::Bar->new( name => $tabname );
            $chart1->title( 'Bases mapped by slide' );
            $chart1->add_col( 'Slide', 'Slide', 'string' );
            $chart1->add_col( 'MappedReadsGB', 'MappedReadsGB', 'number' );
            $chart1->add_col( 'UniqueReadsGB', 'UniqueReadsGB', 'number' );

            # Sort slides by date
            my @slides = map  { $_->[0] }
                         sort { $a->[1] <=> $b->[1] }
                         map  { [ $_, $data{$machtype}->{$runtype}->{$_}->{date} ] }
                         keys %{$data{$machtype}->{$runtype}};

            foreach my $slide (@slides) {
                my $rh_data = $data{$machtype}->{$runtype}->{$slide};
                $chart1->add_row( $slide,
                                  $rh_data->{mapped_reads},
                                  $rh_data->{unique_reads} );
            }
            $chart1->table->add_format(2,'%.2f');  # 2 decimal places
            $chart1->table->add_format(3,'%.2f');  # 2 decimal places
            $chart1->myparams->{chartArea}->{left} = 100;
            $chart1->myparams->{vAxis}->{title} = "'Slides'";
            $chart1->myparams->{hAxis}->{title} = "'Gigabases'";
            $chart1->width(800);
            $chart1->height(1000);

            # Add a Chart::Table so we can have the Table visualisation if we choose

            my $table1 = QCMG::Google::Chart::Table->doppelganger( $chart1,
                              name => 'doppelganger_of_' . $chart1->name );
            $table1->height( 800 );
            $table1->width( 500 );

            # Add new charts to collection and to page
            $self->charts->add_chart( $chart1 );
            $self->charts->add_chart( $table1 );

            my $stab = QCMG::HTML::Tab->new( id => $tabname );

            # Setup div contents as 2-cell table with Google Chart in the left
            # (first) cell and the text table in the second (right) cell
    
            $stab->add_content( "<br><table><tr><td style=\"width:900\">\n" );

            $stab->add_content( '<div id="'. $chart1->javascript_div_name .
                                "\"></div>\n" );

            $stab->add_content( "</td><td style=\"vertical-align:top\">\n" );

            $stab->add_content( '<div id="'. $table1->javascript_div_name .
                               "\"></div>\n" );

            $stab->add_content( "</td></tr></table>\n" );

            $tab->add_subtab( $stab );
        }
    }

    return $tab;
}



1;
__END__


=head1 NAME

QCMG::Visualise::SolidStatsMappingSummary - Perl module for creating HTML
pages from a mapping summary XML report


=head1 SYNOPSIS

 use QCMG::Visualise::SolidStatsMappingSummary;

 my $report = QCMG::Visualise::SolidStatsMappingSummary->new( file => 'report.xml' );
 print $report->as_html( 'report.html' );


=head1 DESCRIPTION

Here's a list of reports we probably need to see:

 Summary
  - table by month of mapped and uniquely mapped reads in GBases
  - table of summary stats - slides, barcodes, files, etc
  - line chart showing overall capacity
 By machine
  - by month line chart showing how each sequencer has been used
  - pie chart showing overall relative volumes
  - cumulative line chart showing per-sequencer totals
  - per-sequencer barchart showing per-slide totals for F3/R3/F5
 By Run type
  - by month line chart showing throughputs for LMP/Frag/FragPEBC etc
  - pie chart showing overall breakdown
  - showing per-slide totals for F3/R3/F5
 By Project - probably impossible without LIMS lookup unless we just
    take it from the filepath



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

$Id: SolidStatsMappingSummary.pm 4666 2014-07-24 09:03:04Z j.pearson $


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
