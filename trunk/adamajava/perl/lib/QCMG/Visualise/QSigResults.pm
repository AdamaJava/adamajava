package QCMG::Visualise::QSigResults;

##############################################################################
#
#  Module:   QCMG::Visualise:QSigResults.pm
#  Author:   John V Pearson
#  Created:  2012-02-19
#
#  Parse and visualise the Results XML element from qsignature XML files.
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
    die "xmlnode must be a Results Element not [$name]"
        unless ($name eq 'Results');

    my $self = { xmlnode   => $params{xmlnode},
                 charts    => $params{charts},  # QCMG::Google::ChartCollection
                 tab       => undef,  # QCMG::HTML::Tab
                 comps     => undef,
                 files     => undef,
                 mincov    => 8,
                 minsnps   => 1000,
                 greenmax  => $params{greenmax}  || 0.025,
                 yellowmax => $params{yellowmax} || 0.04,
                 mode      => $params{mode} || 0,
                 verbose   => $params{verbose} || 0 };
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


sub greenmax {
    my $self = shift;
    return $self->{greenmax};
}


sub yellowmax {
    my $self = shift;
    return $self->{yellowmax};
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

    qlogprint( "Processing qsignature Results\n" ) if $self->verbose;

    # Create tab for this QSigResults and output info
    my $tab = QCMG::HTML::Tab->new( id => 'Comparisons' );
    $self->{tab} = $tab;

    $self->_parse_results();
    $self->_create_matrix_tab();
    if ($self->{mode} == 0) {
        # Plot minimum coverage on X axis
        $self->_create_plot_tabs_mode0();
    }
    elsif ($self->{mode} == 0) {
        # Plot SNPs used on X axis
        $self->_create_plot_tabs_mode1();
    }
    else {
        die 'mode ['. $self->{mode} .
            "] is not supported in QCMG::Visualise::QSigResults\n";
    }
}


sub _parse_results {
    my $self = shift;

    # Parse out the data for the matrix of comparisons

    my @nodes = $self->xmlnode->findnodes( 'result' );
    my %results = ();
    my %files   = ();
    foreach my $node (@nodes) {
        my $files  = get_attr_by_name( $node, 'files' );
        my $flag   = get_attr_by_name( $node, 'flag' );
        my $minCov = get_attr_by_name( $node, 'minCoverage' );
        my $rating = get_attr_by_name( $node, 'rating' );
        my $score  = get_attr_by_name( $node, 'score' );
        my $snps   = get_attr_by_name( $node, 'snpsUsed' );
        if ($files =~ /(\d+) vs (\d+)/) {
            my $file1 = $1;
            my $file2 = $2;
            # Keep track of which files IDs we have seen so far
            $files{$file1}++;
            $files{$file2}++;
            my %comp = ( file1  => $file1,
                         file2  => $file2,
                         flag   => $flag,
                         minCov => $minCov,
                         rating => $rating,
                         score  => $score,
                         snps   => $snps );

            # For each file pair, there will be many different
            # comparisons at different coverage levels

            $results{$file1}->{$file2}->{$minCov} = \%comp;
            $results{$file2}->{$file1}->{$minCov} = \%comp;
        }
        else {
            die "Cannot parse files being compared from string: $files\n"
        }
    }

    $self->{comps} = \%results;
    $self->{files} = \%files;
}


sub _create_matrix_tab {
    my $self = shift;

    my %comps = %{ $self->{comps} };
    my @files = sort { $a <=> $b } keys %{ $self->{files} };

    # For each pairwise file comparison, there are scores at multiple
    # coverage levels and we want to pick a single "best" value to 
    # display in the matrix so we need to do some processing.

    my %results = ();
    foreach my $file1 (@files) {
        foreach my $file2 (@files) {
            $results{$file1}->{$file2} = 0;

            # We want to start testing our comparison scores starting
            # from the least sensitive, i.e. the lowest coverage level.

            my $rh_comps = $comps{$file1}->{$file2};
            my @counts = sort { $a <=> $b } keys %{ $rh_comps };

            # Default score is unknown
            $results{$file1}->{$file2} = 0;

            foreach my $count (@counts) {
                # Pick the highest coverage level above the pre-defined
                # minimum coverage that still has the pre-defined minimum
                # number of SNPs.
                next unless ($rh_comps->{$count}->{snps} >= $self->{minsnps});
                next unless ($rh_comps->{$count}->{minCov} >= $self->{mincov});
                $results{$file1}->{$file2} = $rh_comps->{$count}->{score};
            }
        }
    }

    my $tab = QCMG::HTML::Tab->new( id => 'Matrix' );

    $tab->add_content( "<br><br><table>\n<tr><th></th>" );
    # Add column headings
    foreach my $file1 (@files) {
        $tab->add_content( "<th>$file1</th>" );
    }
    $tab->add_content( "</tr>\n" );

    foreach my $file1 (@files) {
        $tab->add_content( "<tr><th>$file1</th>\n" );
        foreach my $file2 (@files) {
            # There could be no content so cope with !exists
            my $score = 0;
            if (exists $results{$file1}->{$file2}) {
                $score = $results{$file1}->{$file2};
            }
            # Pick a color for this cell:
            my $color = ($score < 0.0001)              ? 'gray' :
                        ($score < $self->greenmax() )  ? 'green' :
                        ($score < $self->yellowmax() ) ? 'yellow' : 'red';

            $tab->add_content( "<td style=\"background-color:$color\">",
                               sprintf("%.4f",$score), '</td>' );
        }
        $tab->add_content( "</tr>\n" );
    }

    $tab->add_content( "</table><br>\n" );

    $self->tab->add_subtab( $tab );
}


sub _create_plot_tabs_mode1 {
    my $self = shift;

    my %comps = %{ $self->{comps} };
    my @files = sort { $a <=> $b } keys %{ $self->{files} };

    foreach my $file1 (@files) {

        # To draw a scatter diagram we need to know all of the possible
        # X values up front so we'll do a full pass through all of the
        # files creating a "super" list of all the SNP counts and
        # rearranging the scores data by SNP count.  Then we'll
        # do a second pass through the data building the plot data
        # structure, inserting nulls as needed.

        # Pass 1 - get SNP counts for this file vs all other files
        my %counts = ();
        my %scores = ();
        my @rows   = ();
        foreach my $file2 (@files) {
            my $rh_comps = $comps{$file1}->{$file2};
            my @covs = sort { $a <=> $b } keys %{ $rh_comps };
            foreach my $cov (@covs) {
                my $snps  = $rh_comps->{$cov}->{snps};
                my $score = $rh_comps->{$cov}->{score};
                $counts{ $snps }++;
                $scores{ $file2 }->{ $snps } = $score;
                push @rows, [ $file2,
                              $cov,
                              $rh_comps->{$cov}->{snps},
                              $rh_comps->{$cov}->{score},
                              $rh_comps->{$cov}->{rating},
                              $rh_comps->{$cov}->{flag} ];
            }
        }
        my @counts = sort { $a <=> $b } keys %counts;

        # Pass 2 - pad table with 'null'
        foreach my $file2 (@files) {
            foreach my $count (@counts) {
                if (! defined $scores{ $file2 }->{ $count }) {
                     $scores{ $file2 }->{ $count } = 'null';
                }
            }
        }

        my $tab = QCMG::HTML::Tab->new( id => "$file1" );

        my $chart = QCMG::Google::Chart::Scatter->new( name => "bam_cov_$file1" );
        $chart->title( "qsignature scores for file $file1 vs all other files at various minimum coverage levels" );
        $chart->width( 750 );
        $chart->height( 700 );
        $chart->myparams->{interpolateNulls} = "'true'";
        $chart->myparams->{lineWidth} = 3;
        $chart->myparams->{pointSize} = 5;
        $chart->myparams->{colors} = [ qw( 'red' 'green' 'yellow'
                                           'violet' 'orange' 'aqua'
                                           'blue' 'lightgreen'
                                           'brown' 'black' ) ];
        $chart->myparams->{hAxis}->{logScale} = "'true'";
        $chart->myparams->{hAxis}->{title} = "'Number of SNPs used'";
        $chart->myparams->{vAxis}->{title} = "'Similarity score'";
        $chart->add_col( 1, 'SNPcount', 'number' );

        # We can't add the rows until we have the columns

        my $col_ctr = 2;
        foreach my $file2 (@files) {
            $chart->add_col( $col_ctr++, $file2, 'number' );
        }

        # Do a pass building the chart table data structure

        foreach my $count (@counts) {
            my @row = ( $count );
            foreach my $file2 (@files) {
                push @row, $scores{ $file2 }->{ $count };
            }
            $chart->add_row( @row );
        }

        # Build the table of comparisons

        my $table = QCMG::Google::Chart::Table->new( name => "bam_cov_table_$file1" );
        $table->height( 600 );
        $table->width( 500 );
        $table->add_col( 1, 'File', 'number' );
        $table->add_col( 2, 'Coverage', 'number' );
        $table->add_col( 3, 'SNPs_Available', 'number' );
        $table->add_col( 4, 'Score', 'number' );
        $table->add_col( 5, 'Flag', 'string' );
        $table->add_col( 6, 'Rating', 'string' );

        foreach my $ra_row (@rows) {
            $table->add_row( @{ $ra_row } );
        }

        # Create the HTML table to hold both chart and table

        $tab->add_content( "<table><tr><td>\n" );
        $tab->add_content( '  '. $chart->javascript_div_html ); 
        $tab->add_content( "</td><td>\n" );
        $tab->add_content( '  '. $table->javascript_div_html );
        $tab->add_content( "</td></tr></table>\n" );

        $self->charts->add_chart( $chart );
        $self->charts->add_chart( $table );
        $self->tab->add_subtab( $tab );
    }
}


sub _create_plot_tabs_mode0 {
    my $self = shift;

    my %comps = %{ $self->{comps} };
    my @files = sort { $a <=> $b } keys %{ $self->{files} };

    foreach my $file1 (@files) {

        # To draw a scatter diagram we need to know all of the possible
        # X values up front so we'll do a full pass through all of the
        # files creating a "super" list of all the SNP counts and
        # rearranging the scores data by SNP count.  Then we'll
        # do a second pass through the data building the plot data
        # structure, inserting nulls as needed.

        # Pass 1 - get coverage levels for this file vs all other files
        my %covs   = ();
        my %scores = ();
        my @rows   = ();
        foreach my $file2 (@files) {
            my $rh_comps = $comps{$file1}->{$file2};
            my @covs = sort { $a <=> $b } keys %{ $rh_comps };
            foreach my $cov (@covs) {
                my $score = $rh_comps->{$cov}->{score};
                $covs{ $cov }++;
                $scores{ $file2 }->{ $cov } = $score;
                push @rows, [ $file2,
                              $cov,
                              $rh_comps->{$cov}->{snps},
                              $rh_comps->{$cov}->{score},
                              $rh_comps->{$cov}->{rating},
                              $rh_comps->{$cov}->{flag} ];
            }
        }
        my @covs = sort { $a <=> $b } keys %covs;

        # Pass 2 - pad table with 'null'
        foreach my $file2 (@files) {
            foreach my $cov (@covs) {
                if (! defined $scores{ $file2 }->{ $cov }) {
                     $scores{ $file2 }->{ $cov } = 'null';
                }
            }
        }

        my $tab = QCMG::HTML::Tab->new( id => "$file1" );

        my $chart = QCMG::Google::Chart::Scatter->new( name => "bam_cov_$file1" );
        $chart->title( "qsignature scores for file $file1 vs all other files at various minimum coverage levels" );
        $chart->width( 750 );
        $chart->height( 700 );
        $chart->myparams->{interpolateNulls} = "'true'";
        $chart->myparams->{lineWidth} = 3;
        $chart->myparams->{pointSize} = 5;
        $chart->myparams->{colors} = [ qw( 'red' 'green' 'yellow'
                                           'violet' 'orange' 'aqua'
                                           'blue' 'lightgreen'
                                           'brown' 'black' ) ];
        $chart->myparams->{hAxis}->{logScale} = "'true'";
        $chart->myparams->{hAxis}->{title} = "'Minimum coverage level at SNP (bases)'";
        $chart->myparams->{vAxis}->{title} = "'Similarity score'";
        $chart->add_col( 1, 'SNPcount', 'number' );

        # We can't add the rows until we have the columns

        my $col_ctr = 2;
        foreach my $file2 (@files) {
            $chart->add_col( $col_ctr++, $file2, 'number' );
        }

        # Do a pass building the chart table data structure

        foreach my $cov (@covs) {
            my @row = ( $cov );
            foreach my $file2 (@files) {
                push @row, $scores{ $file2 }->{ $cov };
            }
            $chart->add_row( @row );
        }

        # Build the table of comparisons

        my $table = QCMG::Google::Chart::Table->new( name => "bam_cov_table_$file1" );
        $table->height( 600 );
        $table->width( 500 );
        $table->add_col( 1, 'File', 'number' );
        $table->add_col( 2, 'Coverage', 'number' );
        $table->add_col( 3, 'SNPs_Available', 'number' );
        $table->add_col( 4, 'Score', 'number' );
        $table->add_col( 5, 'Flag', 'string' );
        $table->add_col( 6, 'Rating', 'string' );

        foreach my $ra_row (@rows) {
            $table->add_row( @{ $ra_row } );
        }

        # Create the HTML table to hold both chart and table

        $tab->add_content( "<table><tr><td>\n" );
        $tab->add_content( '  '. $chart->javascript_div_html ); 
        $tab->add_content( "</td><td>\n" );
        $tab->add_content( '  '. $table->javascript_div_html );
        $tab->add_content( "</td></tr></table>\n" );

        $self->charts->add_chart( $chart );
        $self->charts->add_chart( $table );
        $self->tab->add_subtab( $tab );
    }
}


1;

__END__


=head1 NAME

QCMG::Visualise::QSigResults - Perl module for parsing comparison data
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
