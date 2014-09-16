use strict;
use Test::More;
use FindBin;
use Data::Dumper;

use_ok( 'PGTools::Util::GTF' );

my $gtf = PGTools::Util::GTF->new( "$FindBin::Bin/data/test.gtf" );

isa_ok( $gtf, 'PGTools::Util::GTF' );
ok( $gtf->length == 10, 'Length is 10' );
ok( scalar( @{ [ $gtf->first( 2 ) ] } ) == 2, 'First method returns right' );
ok( $gtf->first( 1 )->{seqname} eq 'chr10' );

$gtf->each( sub {
  ok( shift->{attribute}{gene_type} eq 'pseudogene' );
} );


done_testing;
