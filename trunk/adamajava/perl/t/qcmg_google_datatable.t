###########################################################################
#
#  File:     qcmg_google_datatable.t
#  Creator:  John Pearson
#  Created:  2011-09-24
#
#  $Id: qcmg_google_datatable.t 1274 2011-10-18 04:44:57Z j.pearson $
#
###########################################################################

use Data::Dumper;
use Test::More tests => 69;
BEGIN { use_ok('QCMG::Google::Charts') };  # remember to count this test!

# Test defaults (9)

my $dt = QCMG::Google::DataTable->new( name => 'jp1' );
isa_ok( $dt, 'QCMG::Google::DataTable' );

ok( $dt->name eq 'jp1', 'name getter' );
ok( $dt->verbose == 0, 'verbose default' );
my @cols = $dt->cols;
ok( scalar(@cols) == 0 , 'cols has 0 elements as default' );
ok( $dt->col_count == 0 , 'col_count default' );
my @rows = $dt->rows;
ok( scalar(@rows) == 0 , 'rows has 0 elements as default' );
ok( $dt->row_count == 0 , 'row_count default' );
ok( $dt->version eq '0.6', 'version default' );
ok( defined $dt->svn_version, 'svn_version is set' );

# Test accessors (16)

$dt->add_col( 1, 'ID', 'number' );
$dt->add_col( 2, 'Score', 'number' );
ok( $dt->col_count == 2, 'col_count gives correct results after add_col' );
@cols = $dt->cols;
ok( scalar(@cols) == 2, 'cols returns a correctly sized array' );
ok( $cols[0]->[0] == 1, 'cols data structure is intact (1)' );
ok( $cols[0]->[1] eq 'ID', 'cols data structure is intact (2)' );
ok( $cols[0]->[2] eq 'number', 'cols data structure is intact (3)' );
ok( $cols[1]->[0] == 2, 'cols data structure is intact (4)' );
ok( $cols[1]->[1] eq 'Score', 'cols data structure is intact (5)' );
ok( $cols[1]->[2] eq 'number', 'cols data structure is intact (6)' );

$dt->add_row( 1, 19 );
$dt->add_row( 2, 24 );
$dt->add_row( 3, 27 );
ok( $dt->row_count == 3, 'row_count gives correct results after add_row' );
@rows = $dt->rows;
ok( scalar(@rows) == 3, 'rows returns a correctly sized array' );
ok( $rows[0]->[0] == 1, 'rows data structure is intact (1)' );
ok( $rows[0]->[1] == 19, 'rows data structure is intact (2)' );
ok( $rows[1]->[0] == 2, 'rows data structure is intact (3)' );
ok( $rows[1]->[1] == 24, 'rows data structure is intact (4)' );
ok( $rows[2]->[0] == 3, 'rows data structure is intact (5)' );
ok( $rows[2]->[1] == 27, 'rows data structure is intact (6)' );

# Test trim (12)

# Read data that we'll use for subsequent tests
my @data = ();
while (my $line = <DATA>) {
    chomp $line;
    my @fields = split(',',$line);
    push @data, \@fields;
}
ok( scalar(@data) == 26, 'read correct row count from __DATA__' );

my $d2 = QCMG::Google::DataTable->new( name => 'jp2' );
isa_ok( $d2, 'QCMG::Google::DataTable' );
_populate_table( $d2, \@data );

ok( $d2->col_count == 3 , 'col_count setup for trim' );
ok( $d2->row_count == 26, 'row_count setup for trim' );

$d2->trim( 22000 );
ok( $d2->row_count == 20, 'trim results in correct row_count' );
@rows = $d2->rows;
ok( $rows[19]->[0] eq '>18', 'trim results in correct final record field 0' );
ok( $rows[19]->[1] == 1000, 'trim results in correct final record field 1' );
ok( $rows[19]->[2] == 560, 'trim results in correct final record field 2' );
$d2->trim( 22000 );
ok( $d2->row_count == 20, 'repeat trim does nothing (1)' );
ok( $rows[19]->[0] eq '>18', 'repeat trim does nothing (2)' );
ok( $rows[19]->[1] == 1000, 'repeat trim does nothing (3)' );
ok( $rows[19]->[2] == 560, 'repeat trim does nothing (4)' );

# Test trim with column actions (12)

my $d3 = QCMG::Google::DataTable->new( name => 'jp3' );
isa_ok( $d3, 'QCMG::Google::DataTable' );
_populate_table( $d3, \@data );

ok( $d3->row_count == 26, 'row_count reset for trim column actions' );
ok( $d3->_total(1) == 23160, 'column 1 total is correct before trim' );
$d3->trim( 22000, 1, 'cumul_max', '.+.' );
ok( $d3->_total(1) == 23160, 'column 1 total is correct after trim' );
ok( $d3->row_count == 20, 'trim col acts: correct row_count' );
@rows = $d3->rows;
ok( $rows[19]->[0] eq '>18', 'trim col act .: correct final record field 0' );
ok( $rows[19]->[1] == 1000, 'trim col act +: correct final record field 1' );
ok( $rows[19]->[2] == 80, 'trim col act .: correct final record field 2' );

$d3->trim( 21000, 1, 'cumul_max', '.+_' );
ok( $d3->row_count == 19, 'retrim col acts: correct row_count' );
@rows = $d3->rows;
ok( $rows[18]->[0] eq '>17', 'retrim col act .: correct final record field 0' );
ok( $rows[18]->[1] == 1530, 'retrim col act +: correct final record field 1' );
ok( $rows[18]->[2] eq '', 'retrim col act -: correct final record field 2' );

# Test add_percentage and add_cumulative percentage (17)

my $d4 = QCMG::Google::DataTable->new( name => 'jp4' );
isa_ok( $d4, 'QCMG::Google::DataTable' );
_populate_table( $d4, \@data );

ok( $d4->row_count == 26, 'row_count setup for percentage methods' );
$d4->add_percentage(1);
ok( ($d4->{rows}->[0]->[3] > 0.086 and $d4->{rows}->[0]->[3] < 0.087),
    'percentage correct for first row' );
ok( ($d4->{rows}->[3]->[3] > 0.077 and $d4->{rows}->[3]->[3] < 0.078),
    'percentage correct for row 3' );
ok( ($d4->{rows}->[24]->[3] > 0.0004 and $d4->{rows}->[24]->[3] < 0.0005),
    'percentage correct for row 24' );
ok( $d4->{rows}->[25]->[3] == 0, 'percentage correct for last row' );

$d4->add_cumulative_percentage(1);
ok( ($d4->{rows}->[0]->[4] > 0.086 and $d4->{rows}->[0]->[4] < 0.087),
    'percentage correct for first row' );
ok( ($d4->{rows}->[3]->[4] > 0.246 and $d4->{rows}->[3]->[4] < 0.247),
    'percentage correct for row 3' );
ok( ($d4->{rows}->[23]->[4] > 0.9995 and $d4->{rows}->[23]->[4] < 0.9996),
    'percentage correct for row 23' );
ok( $d4->{rows}->[24]->[4] == 1, 'percentage correct for row 24' );
ok( $d4->{rows}->[25]->[4] == 1, 'percentage correct for last row' );

$d4->trim( 0.95, 3, 'cumul_max', '.+_+1' );
ok( $d4->row_count == 20, 'correct number of rows trimmed on cumulative %' );
ok( ($d4->{rows}->[0]->[4] > 0.086 and $d4->{rows}->[0]->[4] < 0.087),
    'percentage correct for first row' );
ok( ($d4->{rows}->[3]->[4] > 0.246 and $d4->{rows}->[3]->[4] < 0.247),
    'percentage correct for row 3' );
ok( ($d4->{rows}->[18]->[4] > 0.95 and $d4->{rows}->[18]->[4] < 1),
    'percentage correct for row 18' );
ok( $d4->{rows}->[19]->[4] == 1, 'percentage correct for last row' );
ok( $d4->{rows}->[19]->[0] eq '>18', 'label correct for last row' );

# Test binning (2)

my $d5 = QCMG::Google::DataTable->new( name => 'jp5' );
isa_ok( $d5, 'QCMG::Google::DataTable' );
_populate_table( $d5, \@data );

ok( $d5->row_count == 26, 'row_count setup for bin' );
$d5->bin(6);  # 6 bins


# Private helper functions

sub _populate_table {
    my $dt      = shift;
    my $ra_data = shift;

    my @data = ();
    foreach my $row (@{ $ra_data }) {
       my @fields = @{ $row };
       push @data, \@fields;
    }

    $dt->add_col( 1, 'ID', 'string' );
    $dt->add_col( 2, 'Coverage', 'number' );
    $dt->add_col( 3, 'Threshold', 'number' );
    foreach my $row (@data) {
        $dt->add_row( @{ $row } );
    }
}


# This data will be used to test the trim and bin methods.
# Rows: 26
# Total col1: 23160
__DATA__
0,2000,80
1,1900,80
2,0,80
3,1800,80
4,1700,80
5,1600,80
6,1500,80
7,1400,80
8,1300,80
9,1200,80
10,1100,80
11,920,80
12,960,80
13,880,80
14,890,80
15,900,80
16,780,80
17,800,80
18,530,80
19,200,80
20,280,80
21,240,80
22,180,80
23,90,80
24,10,80
25,0,80
