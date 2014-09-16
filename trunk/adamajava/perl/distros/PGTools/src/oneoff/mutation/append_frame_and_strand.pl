use strict;
use FindBin;
use lib "/home/venkat/bio/lib";
use PGTools::Util;
use PGTools::Util::Fasta;
use Data::Dumper;
use IO::File;
use DBI;

# database file
my $db = 'feature.sqlite';

# original fasta file
my $fa = 'Homo_sapiens.GRCh37.74.cdna.all.fa';

# to be generated
my $ou = 'Homo_sapiens.GRCh37.74.cdna.all.frame_strand.fa';

my $fasta = PGTools::Util::Fasta->new_with_file( $fa );
my $dbh   = DBI->connect( "dbi:SQLite:dbname=$db" );
my $fh    = IO::File->new( $ou, 'w' );
my $st    = $dbh->prepare( 'SELECT * FROM grch37 WHERE feature="cds" AND attributes LIKE ?');

$fasta->reset;


while( $fasta->next ) {
  my ( $id ) = $fasta->title =~ /(ENST\d+)/;
  $st->execute( "%\L$id\E%" );

  my $row = $st->fetchrow_hashref;

  print Dumper( $row );

  print $fh '>' .
    $fasta->title . ' ' . "frame=" . $row->{frame} . ';strand=' . $row->{strand} .
    $fasta->eol .
    normalize( $fasta->sequence );

}

