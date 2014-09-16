package QCMG::Visualise::DirectorySizeReport;

##############################################################################
#
#  Module:   QCMG::Visualise:DirectorySizeReport.pm
#  Author:   John V Pearson
#  Created:  2011-10-12
#
#  Parse and visualise the DirectorySizeReport XML element from
#  solidstatsreport XML files.
#
#  $Id: DirectorySizeReport.pm 4666 2014-07-24 09:03:04Z j.pearson $
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
( $SVNID ) = '$Id: DirectorySizeReport.pm 4666 2014-07-24 09:03:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    my $class  = shift;
    my %params = @_;

    # Must pass xmlnode : XML::LibXML::Element
    # Must pass charts : QCMG::Google::ChartCollection

    croak 'You must supply an XML::LibXML::Element object as '.
          ' xmlnode parameter to new()'
        unless (exists $params{xmlnode} and
                ref($params{xmlnode}) eq 'XML::LibXML::Element');

    my $name = $params{xmlnode}->nodeName;
    die "xmlnode must be a DirectorySizeReport Element not [$name]"
        unless ($name eq 'DirectorySizeReport');

    my $self = { xmlnode => $params{xmlnode},
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

    qlogprint( "Processing DirectorySizeReport\n" ) if $self->verbose;

    $self->_parse_details();
    # Look for seq_raw or seq_mapped in directory name
    my $name = ($self->{dir} =~ /\/seq_raw\//) ? 'seq_raw Dir' :
               ($self->{dir} =~ /\/seq_mapped\//) ? 'seq_mapped Dir' :
               'Directory';

    # Create tab for this DirectorySizeReport and output info
    my $tab = QCMG::HTML::Tab->new( id => $name );
    $self->{tab} = $tab;

    my @dirs = $self->xmlnode->findnodes('DirSizeDirectory');
    $self->_parse_dir( $dirs[0] );
    $self->_add_header;
    #$self->_create_google_table;
    $self->_create_google_table2;
}


sub _parse_details {
    my $self = shift;

    $self->{dir}   = $self->xmlnode->getAttribute('directory');
    $self->{exts}  = $self->xmlnode->getAttribute('extensions');
    $self->{start} = $self->xmlnode->getAttribute('start_time');
}


sub _add_header {
    my $self = shift;

    $self->tab->add_content( "<p class=\"header\">\n" );
    $self->tab->add_content( '<i>Dir: </i>'. $self->{dir} ."\n");
    $self->tab->add_content( '<br><i>Extensions: </i>'. $self->{exts} ."\n" );
    $self->tab->add_content( '<br><i>Start: </i>'. $self->{start}. "</p>\n" );
}


sub _parse_dir {
    my $self = shift;
    my $node = shift;

    # DirSizeDirectory: name size size_in_bytes file_count subdir_count level
    # DirSizFile: name size size_in_bytes

    my $name  = $node->getAttribute('name');
    my $size  = $node->getAttribute('size');
    my $bytes = $node->getAttribute('size_in_bytes');
    my $files = $node->getAttribute('file_count');
    my $subds = $node->getAttribute('subdir_count');
    my $level = $node->getAttribute('level');

    # Push self onto the stack
    push @{$self->{records}}, [ $name, $size, $bytes, $files, $subds, $level ];

    # Push files onto the stack
    my @files = $node->findnodes( 'DirSizeFile' );
    foreach my $file (@files) {
        my $name  = $file->getAttribute('name');
        my $size  = $file->getAttribute('size');
        my $bytes = $file->getAttribute('size_in_bytes');
        push @{$self->{records}}, [ $name, $size, $bytes, '', '', $level+1 ];
    }

    # Recursively parse any subdirs
    my @dirs  = $node->findnodes( 'DirSizeDirectory' );
    foreach my $dir (@dirs) {
        $self->_parse_dir( $dir );
    }
}


sub _create_google_table {
    my $self = shift;

    # Create table with columns and rows
    my $table = QCMG::Google::Chart::Table->new( name => 'DirectoryReport' );
    $table->add_col( 1, 'Size', 'string' );
    $table->add_col( 2, 'Name', 'string' );
    $table->add_col( 3, 'Files', 'string' );
    $table->add_col( 4, 'SubDirectories', 'string' );
    $table->add_col( 5, 'SizeInBytes', 'string' );

    foreach my $ra_vals (@{ $self->{records} }) {
        $table->add_row( $ra_vals->[1],
                         ('  'x $ra_vals->[5]) . $ra_vals->[0],
                         $ra_vals->[3],
                         $ra_vals->[4],
                         $ra_vals->[2] );
    }
    $self->charts->add_chart( $table );

    # This is where the Google Table will go
    $self->tab->add_content( '<div id="' . $table->javascript_div_name .
                             "\"></div>\n" );
}


sub _create_google_table2 {
    my $self = shift;

    my $text = '<table class="qcmgtable">' ."\n".
               '<tr><th>Size</th><th>Name</th><th>Files</th>' .
                   "<th>SubDirectories</th><th>Size in bytes</th></tr>\n";

    foreach my $ra_vals (@{ $self->{records} }) {
        $text .= '<tr>'.
                 '<td>'. $ra_vals->[1] .'</td>'.
                 '<td><img src="/spacer.gif" height="1" width="' .
                            15 * $ra_vals->[5] .'"><code>'.
                            $ra_vals->[0] .'</code></td>'.
                 '<td>'. $ra_vals->[3] .'</td>'.
                 '<td>'. $ra_vals->[4] .'</td>'.
                 '<td>'. $ra_vals->[2] ."</td></tr>\n";
    }
    $text .= "</table>\n";

    # Push the finished table into the tab HTML
    $self->tab->add_content( $text );
}

1;

__END__


=head1 NAME

QCMG::Visualise::DirectorySizeReport - Perl module for parsing Directory
reports from SolidStatsReport XML reports


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

$Id: DirectorySizeReport.pm 4666 2014-07-24 09:03:04Z j.pearson $


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
