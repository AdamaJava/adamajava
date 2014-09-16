###########################################################################
#
#  File:     qcmg_google_chart_bar.t
#  Creator:  John Pearson
#  Created:  2011-09-24
#
#  $Id: qcmg_google_chart_bar.t 2778 2012-08-24 03:18:16Z j.pearson $
#
###########################################################################

use Test::More tests => 7;
BEGIN { use_ok('QCMG::Google::Charts') };

# Test defaults (6)

my $gc = QCMG::Google::Chart::Bar->new( name => 'jp1' );
isa_ok( $gc, 'QCMG::Google::Chart::Bar' );
isa_ok( $gc, 'QCMG::Google::Charts' );

ok( $gc->title eq "'Google BarChart'", 'title getter' );
ok( $gc->width == 1400, 'width getter' );
ok( $gc->height == 900, 'height getter' );
ok( $gc->chart_type eq 'BarChart', 'chart_type getter' );
