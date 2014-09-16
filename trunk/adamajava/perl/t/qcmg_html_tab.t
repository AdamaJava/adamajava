###########################################################################
#
#  File:     qcmg_html_tab.t
#  Creator:  John Pearson
#  Created:  2011-09-25
#
#  $Id: qcmg_html_tab.t 1151 2011-09-26 00:03:57Z j.pearson $
#
###########################################################################

use Test::More tests => 17;
BEGIN { use_ok('QCMG::HTML::Tab') };

# Test defaults (7)

my $tb = QCMG::HTML::Tab->new( );
isa_ok( $tb, 'QCMG::HTML::Tab' );

ok( $tb->id eq '', 'id getter' );
ok( $tb->content eq '', 'content default' );
ok( $tb->verbose == 0, 'verbose default' );
ok( defined $tb->subtabs, 'subtabs exists' );
my $st = $tb->subtabs;
ok( ref( $st ) eq 'ARRAY', 'subtabs is an array' );
ok( scalar(@{ $st }) == 0, 'subtabs has no content' );

# Test accessors (7)
 
$tb = QCMG::HTML::Tab->new( id => 'jp1', content => 'QCMG', verbose => 1 );
isa_ok( $tb, 'QCMG::HTML::Tab' );

ok( $tb->id eq 'jp1', 'id getter' );
ok( $tb->id('jp2') eq 'jp2', 'id setter' );
ok( $tb->content eq 'QCMG', 'content getter' );
ok( $tb->content('IMB') eq 'IMB', 'content setter' );
ok( $tb->verbose == 1, 'verbose getter' );
ok( $tb->verbose(2) == 2, 'verbose setter' );

# Test subtabs (2)

my $st1 = QCMG::HTML::Tab->new( id => 'subtab1' );
$tb->add_subtab( $st1 );
my @tabs = @{ $tb->subtabs };
ok( scalar( @tabs ) == 1, 'added a subtab' );
ok( $tabs[0]->id eq 'subtab1', 'subtab is intact on retrieval' );

