#!/usr/bin/perl -w

##############################################################################
#
#  Program:  visualise_qcoverage.pl
#  Author:   John V Pearson
#  Created:  2011-05-15
#
#  Read a qcoverage report and create a HTML file that uses the Google chart
#  API to display graphs and summary tables describing the coverage.
#
#  $Id: tally_feature_coverage.pl 2860 2011-05-05 16:25:41Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use XML::Simple;
use Data::Dumper;
use Pod::Usage;
use POSIX qw(floor);

use QCMG::Google::ChartCollection;
use QCMG::Google::DataTable;
use QCMG::Google::Chart::Bar;
use QCMG::Google::Chart::Line;

use Carp qw( carp croak );

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 2860 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: tally_feature_coverage.pl 2860 2011-05-05 16:25:41Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $infile     = '';
    my $outfile    = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \$infile,        # -i
           'o|outfile=s'          => \$outfile,       # -o
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    print( "\nvisualise_qcoverage.pl  v$REVISION  [" . localtime() . "]\n",
           "   infile        $infile\n",
           "   outfile       $outfile\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my %features = ();
    my $charts = process_file( $infile, \%features );
    #print Dumper $charts;
    #print Dumper \%features;
    print_report( $outfile, \%features, $charts );
    print '['.localtime()."]  Finished.\n" if $VERBOSE;
}



sub process_file {
    my $file        = shift;
    my $rh_features = shift;

    print '['.localtime()."]  Opening file $file\n" if $VERBOSE;
    my $fh = IO::File->new( $file, 'r' );
    die("Cannot open input file $file for reading: $!\n")
        unless defined $fh;

    my $file_total_bases  = 0;
    my $file_features     = 0;
    my $file_new_features = 0;

    # Slurp file
    print '['.localtime()."]    reading contents\n" if $VERBOSE;
    my $contents = '';
    while (my $line = $fh->getline) {
        $contents .= $line;
    }

    # Create new ChartCollection object
    my $charts = QCMG::Google::ChartCollection->new();

    # Pull out all the <coverageReport/> elements using regexes
    print '['.localtime()."]    processing coverageReports\n" if $VERBOSE;
    my $rpt_pattern = qr/<coverageReport.+?<\/coverageReport>/s;  # y
    my $cov_pattern = qr/<coverage at="(\d+)" bases="(\d+)"\/>/s;  # y
    my @matches = $contents =~ m/$rpt_pattern/g;

    # Process each coverageReport
    foreach my $xml (@matches) {
        $file_features++;
        if ($xml =~ /feature="(.*)"/) {
            my $feature = $1;
            my %covs = ();
            my @bins = ();

            # Tally coverage for this feature
            my @bases = $xml =~ m/$cov_pattern/g;
            my $total_base_coverage = 0;
            my $total_base_span     = 0;
            while (@bases) {
               my $x = shift @bases;
               my $y = shift @bases;
               $total_base_coverage += ($x * $y);
               $total_base_span     += $y;
               $covs{ $x } = $y;
            }

            # Decide on "binning factor" for this feature based on
            # average coverage.
            my $average_coverage = $total_base_coverage / $total_base_span;
            my $bin_factor = floor( $average_coverage / 100 ) + 1;

            # Initialise coverages (always have 101 bins)
            for my $ctr (0..100) {
                my $bin_start = $ctr * $bin_factor;
                my $bin_end   = $bin_start + $bin_factor - 1;
                my $label = $bin_start == $bin_end ? $bin_start :
                                                     $bin_start.'-'.$bin_end;
                $bins[ $ctr ] = [ $label, 0 ];
            }
            # Initialise "the rest" bin
            $bins[ 100 ] = [ ($bin_factor * 100) .'+', 0 ];

            # Tally coverages.  Any coverage greater than 100 *
            # $bin_factor gets put into a single bin

            foreach my $key (sort keys %covs) {
                if ($key < (100 * $bin_factor)) { 
                    $bins[ floor($key / $bin_factor) ]->[1] += $covs{ $key };
                }
                else {
                    $bins[ 100 ]->[1] += $covs{ $key };
                }
            }

            # Now that we have all of the coverages, we need to push
            # them into our Google Chart for plotting.
            my $chart1 = QCMG::Google::Chart::Bar->new( name => $feature.'_cov' );
            $chart1->title( 'Count of bases at a given coverage level' );
            $chart1->add_col( 'bin', 'bin', 'string' );
            $chart1->add_col( 'coverage', 'coverage', 'number' );
            foreach my $i (0..100) {
                $chart1->add_row( $bins[$i]->[0], $bins[$i]->[1] );
            }
            $chart1->horiz_logscale('true');
            $chart1->vert_title('coverage bins');
            $chart1->horiz_title('bases at each coverage level');
            $chart1->chart_left(100);
            $chart1->width(700);
            $chart1->height(1000);

            my $chart2 = QCMG::Google::Chart::Line->new( name => $feature.'_cumul' );
            $chart2->title( 'Percentage of bases at a given coverage level or higher' );
            $chart2->add_col( 'bin', 'bin', 'string' );
            $chart2->add_col( 'coverage', 'coverage', 'number' );
            $chart2->add_col( '80%', '80%', 'number' );
            my $cumulative_cov = 0;
            foreach my $i (0..100) {
                my $cov_at_this_or_more = 1 - ($cumulative_cov / $total_base_span);
                $chart2->add_row( $bins[$i]->[0], $cov_at_this_or_more, 0.8 );
                $cumulative_cov += $bins[$i]->[1];
            }
            $chart2->vert_title('% bases at given or higher coverage level');
            $chart2->horiz_title('coverage bins');
            $chart2->chart_left(100);
            $chart2->width(1200);
            $chart2->height(600);

            # Add new charts to collection
            $charts->add_chart( $chart1 );
            $charts->add_chart( $chart2 );

            $rh_features->{$feature} = { feature          => $feature,
                                         base_coverage    => $total_base_coverage,
                                         base_span        => $total_base_span,
                                         avg_coverage     => $average_coverage,
                                         bin_factor       => $bin_factor,
                                         binned_coverages => \@bins };

        }
    }

    print '['.localtime()."]    processed $file_features coverageReports\n"
        if $VERBOSE;

    return $charts;
}


sub print_report {
    my $file        = shift;
    my $rh_features = shift;
    my $charts      = shift;

    print '['.localtime()."]  Opening file $file\n" if $VERBOSE;
    my $fh = IO::File->new( $file, 'w' );
    die("Cannot open output file $file for writing: $!\n")
        unless defined $fh;

    my $header = html_header();
    my $footer = html_footer();

    # Put in filename and Google Charts javascript
    $header =~ s/~~~TITLE_GOES_HERE~~~/$file/;

    print $fh $header,"\n\n";

    # Build the UL that lists the available tabs
    print $fh '<ul class="tabs">',"\n",
              '<li><a href="#">Summary</a></li>', "\n";
    foreach my $id (sort keys %{ $rh_features }) {
        my $feature = $rh_features->{ $id };
        print $fh '<li><a href="#">', $feature->{feature}, '</a></li>', "\n";
    }
    print $fh '</ul>', "\n\n";

    # First pane is always summary so lets get that out of the road
    my @summary_columns = ( 'Feature type',
                            'Basepairs in feature type',
                            '% of total basepairs',
                            'Sequenced base in feature type',
                            '% of total sequenced bases',
                            'Average per-basepair coverage' );
    print $fh '<div class="pane"><br>', "\n",
              '<table class="qcmgtable"><tr>', "\n";
    foreach my $text (@summary_columns) {
        print $fh "<th>$text<\/th>\n";
    }
    print $fh "</tr>\n";
    
    # Do calculations
    my $total_bases_in_bam  = 0;
    my $total_bases_in_span = 0;
    foreach my $id (sort keys %{ $rh_features }) {
        my $feature = $rh_features->{ $id };
        $total_bases_in_bam  += $feature->{base_coverage};
        $total_bases_in_span += $feature->{base_span};
    }
    #print Dumper $rh_features, $total_bases_in_bam, $total_bases_in_span;

    foreach my $id (sort keys %{ $rh_features }) {
        my $feature = $rh_features->{ $id };
        my $span_as_pc_of_total = $feature->{base_span} * 100 /
                                  $total_bases_in_span;
        my $coverage_as_pc_of_total = $feature->{base_coverage} * 100 /
                                      $total_bases_in_bam;
        print $fh '<tr><td>',
                  join("<\/td>\n<td>", $feature->{feature},
                                       $feature->{base_span},
                                       sprintf( "%.2f", $span_as_pc_of_total ),
                                       $feature->{base_coverage},
                                       sprintf( "%.2f", $coverage_as_pc_of_total ),
                                       sprintf( "%.2f", $feature->{avg_coverage} ) ),
                  "<\/td><\/tr>\n";
    }

    print $fh "<tr><td>";
    print $fh join("<\/td>\n<td>", 'Totals',
                                   $total_bases_in_span,
                                   '100.00',
                                   $total_bases_in_bam,
                                   '100.00',
                                   sprintf( "%.2f", $total_bases_in_bam /
                                                    $total_bases_in_span) );
    print $fh "<\/td><\/tr>\n";

    print $fh "</table>\n",
              "</div>\n";

    # Put in panes for the charts
    foreach my $id (sort keys %{ $rh_features }) {
        print $fh '<div class="pane">', "\n",
                  '<ul class="tabs">',"\n",
                  '<li><a href="#">Coverage</a></li>', "\n",
                  '<li><a href="#">Cumulative Coverage</a></li>', "\n",
                  '</ul>', "\n";
        my $chart1 = $charts->get_chart( $id .'_cov' );
        print $fh '<div class="pane" id="' . $chart1->name .
                  'Chart_div"></div>' ."\n";
        my $chart2 = $charts->get_chart( $id .'_cumul' );
        print $fh '<div class="pane" id="' . $chart2->name .
                  'Chart_div"></div>' ."\n";
        print $fh "</div>\n\n";
    }

    print $fh $charts->as_javascript();
    print $fh $footer;
}


# For some reason which I haven't worked out yet, if the block of Google
# javascript script refs are after the block of tabbing scripts and
# styles then the tabbing doesn't work.  Go figure.

sub html_header {
    my $header = <<E_O_HEADER;
<HTML>
<HEAD>
<TITLE>qCoverage: ~~~TITLE_GOES_HERE~~~</TITLE>

<link rel="stylesheet" href="http://grimmond.imb.uq.edu.au/styles/qcmg.css" type="text/css" />

<!-- Tabbing system -->
<script src="http://cdn.jquerytools.org/1.2.5/full/jquery.tools.min.js"></script>
<style> 
ul.tabs             { margin:0 !important;  padding:0;  height:30px; border-bottom:1px solid #666;}
ul.tabs li          { float:left;  text-indent:0;  padding:0;  margin:0 !important;  list-style-image:none !important;}
ul.tabs a           { float:left;  font-family: Verdana, Helvetica, Arial; font-size:12px; display:block; padding:5px 30px;
                      text-decoration:none;  border:1px solid #666; border-bottom:0px; height:18px; background-color:rgb(234,242,255);
                      color:rgb(0,66,174); margin-right:2px; position:relative; top:1px; outline:0;  -moz-border-radius:4px 4px 0 0;}
ul.tabs a:active     { background-color:#ddd;  border-bottom:1px solid #ddd;  color:#CCCCCC;  cursor:default;}
ul.tabs a:hover      { background-position: -420px -31px; background-color:#CCFFFF;  color:#333; }
ul.tabs a.current, ul.tabs a.current:hover, ul.tabs li.current a { background-position: -420px -62px; cursor:default !important;
                       font-weight:bold;    color:#000066 !important;}
ul.tabs a.s          { background-position: -553px 0; width:81px; }
ul.tabs a.s:hover    { background-position: -553px -31px; }
ul.tabs a.s.current  { background-position: -553px -62px; }
ul.tabs a.l          { background-position: -248px -0px; width:174px; }
ul.tabs a.l:hover    { background-position: -248px -31px; }
ul.tabs a.l.current  { background-position: -248px -62px; }
ul.tabs a.xl         { background-position: 0 -0px; width:248px; }
ul.tabs a.xl:hover   { background-position: 0 -31px; }
ul.tabs a.xl.current { background-position: 0 -62px; }.panes .pane {
display:none;}
</style>
</HEAD>
<BODY>
E_O_HEADER
    return $header;
}


sub html_footer {
    my $footer = <<E_O_FOOTER;

</BODY>
</HTML>
E_O_FOOTER
    return $footer;
}

__END__

=head1 NAME

visualise_qcoverage.pl - Create HTML visualisation for qcoverage XML


=head1 SYNOPSIS

 visualise_qcoverage.pl -i infile -o outfile [options]


=head1 ABSTRACT

This script will take a qcoverage XML file and create a HTML file with
summary tables and Google chart API plots.


=head1 OPTIONS

 -i | --infile        qcoverage sequence XML file
 -o | --outfile       HTML file
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: tally_feature_coverage.pl 2860 2011-05-05 16:25:41Z j.pearson $


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
