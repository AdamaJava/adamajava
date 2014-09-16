package QCMG::Visualise::LifeScopeDirectory;

##############################################################################
#
#  Module:   QCMG::Visualise:LifeScopeDirectory.pm
#  Author:   John V Pearson
#  Created:  2011-10-11
#
#  Read a solidstatsreport XML file and create a HTML file that uses the
#  Google chart API to display appropriate graphs and summary tables.
#
#  $Id: LifeScopeDirectory.pm 4666 2014-07-24 09:03:04Z j.pearson $
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

use vars qw( $SVNID $REVISION %LFSCHT_PATTERNS $CHT_CTR );

( $REVISION ) = '$Revision: 4666 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: LifeScopeDirectory.pm 4666 2014-07-24 09:03:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


BEGIN {
    $CHT_CTR=0;  # We will use this to assign unique IDs to charts

    # Patterns for matching dfferent .cht types based on filenames
    my $group  = '^Group_(\d+)';
    my $strand = '((?:Positive)|(?:Negative))';
    my $chrom  = '([\w\d]+(?:\.\d+)?)';
    my $tagext = '([FR][35P2BC-]{1,4})\.cht$'; # F3, R3, F5, F5-P2, F5-BC

    %LFSCHT_PATTERNS = (
        alignment_length => 
            qr/$group\.Alignment\.Length\.Distribution\.$tagext/,
        alignment_length_unique => 
            qr/$group\.Alignment\.Length\.Distribution\.Unique\.$tagext/,
        basemismatch => 
            qr/$group\.BaseMismatch\.Distribution\.$tagext$/,
        basemismatch_unique => 
            qr/$group\.BaseMismatch\.Distribution\.Unique\.$tagext/,
        base_qv =>
            qr/$group\.BaseQV\.$tagext/,
        mapping_qv =>
            qr/$group\.MappingQV\.$tagext/,
        coverage =>
            qr/$group\.Coverage\.cht$/,
        coverage_by_strand =>
            qr/$group\.Coverage\.by\.Strand\.$strand\.cht$/,
        coverage_by_chrom =>
            qr/$group\.Coverage\.by\.Chromosome\.$chrom\.cht$/,
        mismatches_by_base_qv => 
            qr/$group\.Mismatches\.by\.BaseQV\.$tagext/,
        mismatches_by_pos_base => 
            qr/$group\.Mismatches\.by\.Position\.BaseSpace\.$tagext/,
        mismatches_by_pos_color => 
            qr/$group\.Mismatches\.by\.Position\.ColorSpace\.$tagext/,
        insert_range => 
            qr/$group\.Insert\.Range\.Distribution\.cht$/,
        pairing_qv => 
            qr/$group\.PairingQV\.cht$/,
        pairing_stats => 
            qr/$group\.Pairing\.Stats\.cht$/,
        read_pair_type => 
            qr/$group\.ReadPair\.Type\.cht$/ );
}


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
    die "xmlnode must be a LifeScopeDirectory Element not [$name]"
        unless ($name eq 'LifeScopeDirectory');

    my $self = { file    => '',
                 xmlnode => $params{xmlnode},
                 charts  => $params{charts},  # QCMG::Google::ChartCollection
                 tab     => undef,  # QCMG::HTML::Tab
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

    qlogprint( "Processing LifeScopeDirectory\n" ) if $self->verbose;

    # Create tab for this LifeScopeDirectory and output info
    my $tab = QCMG::HTML::Tab->new( id => 'LifeScopeDir' );
    $self->{tab} = $tab;

    my @alntabs = ();
    my @covtabs = ();
    my @mimtabs = ();
    my @mmctabs = ();
    my @qvstabs = ();
    my @badtabs = ();

    # This is the grand Cht parser - it reads each Cht and based on a
    # pattern match on the filename, it assigns the Cht to one of a number
    # of type-specific parsers that generate HTML::Tab and Google::Chart
    # objects.

    my @chtnodes = $self->xmlnode->findnodes( "LifeScopeChts/LifeScopeCht" );
    foreach my $node (@chtnodes) {
        my $cht = $self->parse_lfschtnode( $node );
        if ($cht->file =~ /$LFSCHT_PATTERNS{alignment_length}/) {
            my $group = $1;
            my $tag   = $2;
            my $name  = "$tag.overall";
            my $title = 'Alignment lengths of reads for '.
                        "tag $tag in group $group";
            push @alntabs, $self->alignment_length_cht_to_tab(
                                  $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{alignment_length_unique}/) {
            my $group = $1;
            my $tag   = $2;
            my $name  = "$tag.unique";
            my $title = 'Unique alignment lengths of reads for '.
                        "tag $tag in group $group";
            push @alntabs, $self->alignment_length_cht_to_tab(
                                  $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{coverage}/) {
            my $group = $1;
            my $name  = "Summary";
            my $title = "Coverage summary";
            push @covtabs, $self->coverage_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{coverage_by_strand}/) {
            my $group  = $1;
            my $strand = $2;
            my $name   = "$strand.strand";
            my $title  = "Coverage for $strand strand";
            push @covtabs, $self->coverage_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{coverage_by_chrom}/) {
            # Do nothing - too many of these for them to be useful
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{mismatches_by_base_qv}/) {
            my $group = $1;
            my $tag   = $2;
            my $name   = "$tag.BaseQV";
            my $title  = "$tag Mismatches by Base QV";
            push @mimtabs, $self->mismatch_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{mismatches_by_pos_base}/) {
            my $group = $1;
            my $tag   = $2;
            my $name   = "$tag.ByBasespacePos";
            my $title  = "$tag Mismatches by Basespace Position";
            push @mimtabs, $self->mismatch_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{mismatches_by_pos_color}/) {
            my $group = $1;
            my $tag   = $2;
            my $name   = "$tag.ByColorspacePos";
            my $title  = "$tag Mismatches by Colorspace Position";
            push @mimtabs, $self->mismatch_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{basemismatch}/) {
            my $group = $1;
            my $tag   = $2;
            my $name   = "$tag.MismatchCount";
            my $title  = "$tag Distribution of mismatch count per read";
            push @mmctabs, $self->mmcount_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{basemismatch_unique}/) {
            my $group = $1;
            my $tag   = $2;
            my $name   = "$tag.MismatchCountUnique";
            my $title  = "$tag Distribution of mismatch count per unique read";
            push @mmctabs, $self->mmcount_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{base_qv}/) {
            my $group = $1;
            my $tag   = $2;
            my $name   = "$tag.BaseQV";
            my $title  = "$tag Distribution of base qualities";
            push @qvstabs, $self->qvb_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{mapping_qv}/) {
            my $group = $1;
            my $tag   = $2;
            my $name   = "$tag.MappingQV";
            my $title  = "$tag Distribution of mapping qualities";
            push @qvstabs, $self->qvl_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{insert_range}/) {
            my $group = $1;
            my $name   = "InsertSize";
            my $title  = "Distribution of Insert Sizes";
            push @qvstabs, $self->insert_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{pairing_qv}/) {
            my $group = $1;
            my $name   = "PairQV";
            my $title  = "Distribution of pairing qualities";
            push @qvstabs, $self->qvl_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{pairing_stats}/) {
            my $group = $1;
            my $name   = "PairMapping";
            my $title  = "Pair Mapping Distribution";
            push @qvstabs, $self->pairs_cht_to_tab( $cht, $name, $title );
        }
        elsif ($cht->file =~ /$LFSCHT_PATTERNS{read_pair_type}/) {
            my $group = $1;
            my $name   = "PairCategories";
            my $title  = "Pair Categories";
            push @qvstabs, $self->pairs_cht_to_tab( $cht, $name, $title );
        }
        else {
            push @badtabs, $self->alignment_length_cht_to_tab(
                                  $cht, $cht->name, $cht->name );
        }
    }

    #    pairing_qv => 
    #        qr/$group\.PairingQV\.cht$/,

    my $alntab = QCMG::HTML::Tab->new( id => 'Alignment Lengths' );
    foreach my $tab ($self->sort_tabs_by_name( @alntabs )) {
        $alntab->add_subtab( $tab );
    }
    my $covtab = QCMG::HTML::Tab->new( id => 'Coverage' );
    foreach my $tab ($self->sort_tabs_by_name( @covtabs )) {
        $covtab->add_subtab( $tab );
    }
    my $mimtab = QCMG::HTML::Tab->new( id => 'Mismatches' );
    foreach my $tab ($self->sort_tabs_by_name( @mimtabs )) {
        $mimtab->add_subtab( $tab );
    }
    my $mmctab = QCMG::HTML::Tab->new( id => 'Mismatch Counts' );
    foreach my $tab ($self->sort_tabs_by_name( @mmctabs )) {
        $mmctab->add_subtab( $tab );
    }
    my $qvstab = QCMG::HTML::Tab->new( id => 'QVs, Pairs and Inserts' );
    foreach my $tab ($self->sort_tabs_by_name( @qvstabs )) {
        $qvstab->add_subtab( $tab );
    }
    my $badtab = QCMG::HTML::Tab->new( id => 'Charts not handled yet!' );
    foreach my $tab ($self->sort_tabs_by_name( @badtabs )) {
        $badtab->add_subtab( $tab );
    }

    $self->tab->add_subtab( $alntab );
    $self->tab->add_subtab( $covtab );
    $self->tab->add_subtab( $mimtab );
    $self->tab->add_subtab( $mmctab );
    $self->tab->add_subtab( $qvstab );
    $self->tab->add_subtab( $badtab );
}


sub pairs_cht_to_tab {
    my $self  = shift;
    my $cht   = shift;  # QCMG::Lifescope::Cht object
    my $name  = shift;
    my $title = shift;

    return $self->cht_to_tab( QCMG::Google::Chart::PieChart->new(name => $name),
                              $title, 'Frequency', 'Category',
                              '4digit,fixpc',
                              $cht->data, $cht->pathname );
}

sub insert_cht_to_tab {
    my $self  = shift;
    my $cht   = shift;  # QCMG::Lifescope::Cht object
    my $name  = shift;
    my $title = shift;

    return $self->cht_to_tab( QCMG::Google::Chart::Bar->new( name => $name ),
                              $title, 'Reads %', 'Insert Size',
                              'trail0,trim99,4digit,fixpc',
                              $cht->data, $cht->pathname );
}

sub qvb_cht_to_tab {
    my $self  = shift;
    my $cht   = shift;  # QCMG::Lifescope::Cht object
    my $name  = shift;
    my $title = shift;

    return $self->cht_to_tab( QCMG::Google::Chart::Bar->new( name => $name ),
                              $title, 'Frequency', 'Base QV',
                              'trail0,4digit,fixpc',
                              $cht->data, $cht->pathname );
}

sub qvl_cht_to_tab {
    my $self  = shift;
    my $cht   = shift;  # QCMG::Lifescope::Cht object
    my $name  = shift;
    my $title = shift;

    return $self->cht_to_tab( QCMG::Google::Chart::Line->new( name => $name ),
                              $title, 'Mapping QV', 'Frequency',
                              'trail0,4digit,fixpc,fliptitles',
                              $cht->data, $cht->pathname );
}

sub mmcount_cht_to_tab {
    my $self  = shift;
    my $cht   = shift;  # QCMG::Lifescope::Cht object
    my $name  = shift;
    my $title = shift;

    return $self->cht_to_tab( QCMG::Google::Chart::Bar->new( name => $name ),
                              $title, 'Reads %', 'Mismatch count per read',
                              'trail0,4digit,fixpc',
                              $cht->data, $cht->pathname );
}


sub mismatch_cht_to_tab {
    my $self  = shift;
    my $cht   = shift;  # QCMG::Lifescope::Cht object
    my $name  = shift;
    my $title = shift;

    return $self->cht_to_tab( QCMG::Google::Chart::Bar->new( name => $name ),
                              $title, 'Error rate %', 'Base QV',
                              'trail0,4digit,fixpc',
                              $cht->data, $cht->pathname );
}


sub coverage_cht_to_tab {
    my $self  = shift;
    my $cht   = shift;  # QCMG::Lifescope::Cht object
    my $name  = shift;
    my $title = shift;

    return $self->cht_to_tab( QCMG::Google::Chart::Bar->new( name => $name ),
                              $title, 'Frequency', 'Coverage Depth',
                              'trim99,4digit,fixpc',
                              $cht->data, $cht->pathname );
}


sub alignment_length_cht_to_tab {
    my $self  = shift;
    my $cht   = shift;  # QCMG::Lifescope::Cht object
    my $name  = shift;
    my $title = shift;

    return $self->cht_to_tab( QCMG::Google::Chart::Bar->new( name => $name ),
                              $title, 'Reads %', 'Read Length',
                              '4digit,fixpc,revdat',
                              $cht->data, $cht->pathname );
}


# This is the generic tab builder.  The majority of this code is the
# same across large number of the charts so rather than many wasteful
# duplicates, we'll use a handful of if-then-elses to capture the
# differences.  Looks a bit messy but is probably more maintainable.

sub cht_to_tab {
    my $self     = shift;
    my $chart    = shift;
    my $title    = shift;
    my $htitle   = shift;
    my $vtitle   = shift;
    my $mode     = shift;
    my $ra_data  = shift;
    my $filename = shift;

    # Mode is a combination of strings that drives the switches in this
    # method: fliptitles,fixpc,trail0,4digit

    if ($mode =~ /\b fliptitles \b/x) {
        $chart->add_col( 1, $htitle, 'string' );
        $chart->add_col( 2, $vtitle, 'number');
    }
    else {
        $chart->add_col( 1, $vtitle, 'string' );
        $chart->add_col( 2, $htitle, 'number');
    }

    # Transforms that need to be applied to data prior to chart loading
    my @data = @{ $ra_data };
    # Convert percentages in 0-100 range into 0-1 range
    if ($mode =~ /\b fixpc \b/x) {
        my @newdata = ();
        foreach my $row (@data) {
            push @newdata, [ $row->[0], $row->[1]/100 ];
        }
        @data = @newdata;
    }
    # Reverse the order of the rows
    if ($mode =~ /\b revdat \b/x) {
        my @newdata = reverse @data;
        @data = @newdata;
    }


    # All of the pre-load transforms have been completed so load the data
    foreach my $row (@data) {
        $chart->add_row( @{ $row } );
    }

    # Some routines rely on the order of these ops so DO NOT reorder
    if ($mode =~ /\b trail0 \b/x) {
        $chart->table->trim( 0, 1, 'trailing_zeros' );
    }
    if ($mode =~ /\b trim99 \b/x) {
        $chart->table->trim( 0.99, 1 );
    }
    if ($mode =~ /\b 4digit \b/x) {
        $chart->table->add_format( 1, '%.4f' );
    }

    $chart->title( $title );
    $chart->vert_title( $vtitle );
    $chart->horiz_title( $htitle );
    $chart->width( 700 );
    $chart->height( 700 );

#    # Usually don't want the legend
#    if ($mode !~ /\b legend \b/x) {
#        $chart->legend( 'none' );
#    }

    # Create a Chart::Table doppelganger for $chart
    my $table = QCMG::Google::Chart::Table->doppelganger( $chart,
                     name => 'doppelganger_of_' . $chart->name );
    $table->height( 650 );
    $table->width( 300 );

    $self->charts->add_chart( $chart );
    $self->charts->add_chart( $table );

    return $self->tab_with_chart_and_table( $chart, $table, $filename );
}


sub tab_with_chart_and_table {
    my $self  = shift;
    my $chart = shift;
    my $table = shift;
    my $file  = shift;

    # If file is a pathname, split it into directory and filename
    my $dir = '';
    if ($file =~ /^(.*)\/([^\/]+)$/) {
        $dir  = $1;
        $file = $2;
    }

    # Add the subtab and content as 2-cell table
    my $tab = QCMG::HTML::Tab->new( id => $chart->name );

    $tab->add_content( "<p class=\"header\">\n" );
    $tab->add_content( "<i>File:</i> $file\n" );
    $tab->add_content( "<br><i>Dir:</i> $dir</p>\n" ) if $dir;
    $tab->add_content( "</p>\n" );

    $tab->add_content( "<table><tr><td style=\"width:800\">\n" );
    # This is where the Google Chart will go
    $tab->add_content( '<div id="' . $chart->javascript_div_name .
                       "\"></div>\n" );
    $tab->add_content( "</td><td style=\"vertical-align:top\">\n" );
    # This is where the Google Table will go
    $tab->add_content( '<div id="' . $table->javascript_div_name .
                       "\"></div>\n" );
    $tab->add_content( "</td></tr></table>\n" );

    return $tab;
}


# Sort a list of QCMG::HTML::Tab object by name.  Useful when you've
# been creating Tab objects from XML elements in the order they occur
# in source XML file but you'd like to sort the Tabs prior to adding
# them to the page.

sub sort_tabs_by_name {
    my $self = shift;
    my @tabs = @_;

    # Now we sort the tabs alphabetically
    my @sorted_tabs = map  { $_->[0] }
                      sort { $a->[1] cmp $b->[1] }
                      map  { [ $_, $_->id ] }
                      @tabs;
    return @sorted_tabs;
}


# Parse LifeScopeCht XML::LibXML::Node into a data structure
sub parse_lfschtnode {
    my $self = shift;
    my $this = shift;  # XML::LibXML::Node object to be parsed

    # Extract filename
    my $cht = QCMG::Lifescope::Cht->new( verbose => $self->verbose );
    $cht->pathname( get_attr_by_name( $this, 'file' ) );

    my @childnodes = $this->nonBlankChildNodes;
        
    $cht->name( get_node_by_name( $this, 'name' )->textContent );
    $cht->type( get_node_by_name( $this, 'type' )->textContent );
    $cht->title( get_node_by_name( $this, 'title' )->textContent );
    
    qlogprint( 'Parsing XML for cht: '. $cht->name ."\n" ) if $self->verbose;

    my @axes = $this->findnodes( 'axes/axis' );
    foreach my $axis (@axes) {
        my $x = get_attr_by_name($axis,'name');
        $cht->axes->{ $x }->{title} = get_attr_by_name($axis,'title');
        $cht->axes->{ $x }->{range} = get_attr_by_name($axis,'range');
    }

    my @headers = $this->findnodes( 'table/headers/header' );
    foreach my $header (@headers) {
        push @{ $cht->headers }, get_attr_by_name($header,'name');
    }
    my @rows = $this->findnodes( 'table/data/tr' );
    foreach my $row (@rows) {
        my @vals = map { $_->textContent }
                   $row->findnodes( 'td' );
        push @{ $cht->data }, \@vals;
    }

    # Check that axis and header counts agree
    my $axis_count   = scalar ( keys %{ $cht->axes } );
    my $header_count = scalar @{ $cht->headers };
    warn sprintf( "Column counts from axes (%s) and headers (%s) differ for file (%s)",
                  $axis_count, $header_count, $cht->file)
        unless ($axis_count == $header_count and $axis_count > 0);
    $cht->axis_num( $axis_count );

    qlogprint( 'Cht: '. $cht->name .';  rows:'. $cht->row_count ."\n" ) 
        if ($self->verbose > 1);

    return $cht;  # QCMG::Lifescope::Cht object
}


1;

__END__


=head1 NAME

QCMG::Visualise::LifeScopeDirectory - Perl module for creating HTML pages
from LifeScope SolidStatsReport XML reports


=head1 SYNOPSIS

 use QCMG::Visualise::LifeScopeDirectory;

 my $report = QCMG::Visualise::LifeScopeDirectory->new( file => 'report.xml' );
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

$Id: LifeScopeDirectory.pm 4666 2014-07-24 09:03:04Z j.pearson $


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
