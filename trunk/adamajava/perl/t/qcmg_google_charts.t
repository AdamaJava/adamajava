###########################################################################
#
#  File:     qcmg_google_charts.t
#  Creator:  John Pearson
#  Created:  2011-09-24
#
#  $Id: qcmg_google_charts.t 2777 2012-08-24 03:12:28Z j.pearson $
#
###########################################################################

use Test::More tests => 50;
BEGIN { use_ok('QCMG::Google::Charts') };

# Test defaults (31)

my $gc = QCMG::Google::Charts->_inherit( name => 'jp1' );
isa_ok( $gc, 'QCMG::Google::Charts' );

ok( $gc->name eq 'jp1', 'name getter' );
ok( $gc->verbose == 0, 'verbose default' );
ok( defined $gc->table, 'datatable exists' );
isa_ok( $gc->table, 'QCMG::Google::DataTable' );
ok( $gc->myparams->{width} == 1000, 'width default' );
ok( $gc->myparams->{height} == 1000, 'height default' );
ok( $gc->myparams->{title} eq "'QCMG Chart'", 'title default' );
ok( ! defined $gc->chart_type, 'chart_type default is undef' );
ok( $gc->myparams->{chartArea}->{left}  == 60, 'ChartArea->left default' );
ok( $gc->myparams->{chartArea}->{top} == 40, 'ChartArea->top default' );
ok( $gc->myparams->{chartArea}->{width} eq "'80%'", 'chart_width default' );
ok( $gc->myparams->{chartArea}->{height} eq "'85%'", 'chart_height default' );
ok( $gc->myparams->{isStacked} eq "'false'", 'is_stacked default' );
ok( $gc->myparams->{isVertical} eq "'true'", 'is_vertical default' );
my $cols = $gc->myparams->{colors};
ok( ref( $cols ) eq 'ARRAY', 'colors is an array' );
ok( scalar(@$cols) == 5 , 'colors has 5 defaults' );
ok( $cols->[0] eq "'green'", 'colors[0] is green' );
ok( $cols->[1] eq "'red'", 'colors[1] is red' );
ok( $cols->[2] eq "'black'", 'colors[2] is black' );
ok( $cols->[3] eq "'blue'", 'colors[3] is blue' );
ok( $cols->[4] eq "'aqua'", 'colors[4] is aqua' );
ok( $gc->myparams->{fontSize} == 12, 'font_size default' );
ok( $gc->myparams->{titleFontSize} == 15, 'title_font_size default' );
ok( $gc->myparams->{hAxis}->{title} eq "'x axis'", 'horiz_title default' );
ok( $gc->myparams->{hAxis}->{titleColor} eq "'blue'", 'horiz_title_color default' );
ok( $gc->myparams->{hAxis}->{logScale} eq "'false'", 'horiz_logscale default' );
ok( $gc->myparams->{vAxis}->{title} eq "'y axis'", 'vert_title default' );
ok( $gc->myparams->{vAxis}->{titleColor} eq "'blue'", 'vert_title_color default' );
ok( $gc->myparams->{vAxis}->{logScale} eq "'false'", 'vert_logscale default' );
ok( defined $gc->svn_version, 'svn_version default' );

# Test accessors (18)
 
$gc = QCMG::Google::Charts->_inherit( name => 'jp2', verbose => 1 );
isa_ok( $gc, 'QCMG::Google::Charts' );
ok( $gc->verbose == 1, 'verbose non-default' );

$gc->add_col( 1, 'ID', 'number' );
$gc->add_col( 2, 'Score', 'number' );
ok( $gc->col_count == 2, 'col_count gives correct results after add_col' );
my @cols = $gc->cols;
ok( scalar(@cols) == 2, 'cols returns a correctly sized array' );
ok( $cols[0]->[0] == 1, 'cols data structure is intact (1)' );
ok( $cols[0]->[1] eq 'ID', 'cols data structure is intact (2)' );
ok( $cols[0]->[2] eq 'number', 'cols data structure is intact (3)' );
ok( $cols[1]->[0] == 2, 'cols data structure is intact (4)' );
ok( $cols[1]->[1] eq 'Score', 'cols data structure is intact (5)' );
ok( $cols[1]->[2] eq 'number', 'cols data structure is intact (6)' );

$gc->add_row( 1, 19 );
$gc->add_row( 2, 24 );
$gc->add_row( 3, 27 );
ok( $gc->row_count == 3, 'row_count gives correct results after add_row' );
my @rows = $gc->rows;
ok( scalar(@rows) == 3, 'rows returns a correctly sized array' );
ok( $rows[0]->[0] == 1, 'rows data structure is intact (1)' );
ok( $rows[0]->[1] == 19, 'rows data structure is intact (2)' );
ok( $rows[1]->[0] == 2, 'rows data structure is intact (3)' );
ok( $rows[1]->[1] == 24, 'rows data structure is intact (4)' );
ok( $rows[2]->[0] == 3, 'rows data structure is intact (5)' );
ok( $rows[2]->[1] == 27, 'rows data structure is intact (6)' );
