###########################################################################
#
#  File:     qcmg_google_chart_column.t
#  Creator:  John Pearson
#  Created:  2011-09-24
#
#  $Id: qcmg_google_chart_column.t 2778 2012-08-24 03:18:16Z j.pearson $
#
###########################################################################

use Test::More tests => 5;
BEGIN { use_ok('QCMG::Google::Charts') };

# Test defaults (4)

my $gc = QCMG::Google::Chart::Column->new( name => 'jp1' );
isa_ok( $gc, 'QCMG::Google::Chart::Column' );
isa_ok( $gc, 'QCMG::Google::Charts' );

ok( $gc->title eq "'Google ColumnChart'", 'title getter' );
ok( $gc->chart_type eq 'ColumnChart', 'chart_type getter' );
