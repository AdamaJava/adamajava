use strict;
use DBI;
use IO::File;
use Getopt::Long;
use Data::Dumper;
use FindBin;
use lib "$FindBin::Bin/../lib";
use PGTools::Util;
use Text::CSV;

use constant {
  CHROMOSOME => 0, 
  FEATURE => 2, 
  START => 3,
  END => 4,
  STRAND => 6,
  ATTRIBUTES => 8,
  SCORE => 7,
  FRAME => 7,
};

use constant {
  GCHROMOSOME => 0,
  GSTART => 1,
  GEND => 2,
  GENE => 3,
  GSTRAND => 5,
  GFRAME => 20,
  GSCORE => 20,
};


my $options = { };
GetOptions( $options, 'gtf=s', 'db=s', 'gene=s' );

must_have( "GTF File", $options->{gtf} ); 
must_have( "Gene Database File", $options->{gene} ); 
must_be_defined "Database file", $options->{db};

unlink $options->{db} if -e $options->{db};

my $dbh = DBI->connect( 'dbi:SQLite:dbname=' . $options->{db} );

$dbh->do( qq'

  CREATE TABLE grch37 (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    sequence VARCHAR( 100 ),
    source VARCHAR( 50 ),
    feature VARCHAR( 50 ),
    start INT,
    end INT, 
    score VARCHAR( 100 ),
    strand VARCHAR( 5 ),
    frame VARCHAR( 100 ),
    chromosome VARCHAR( 10 ),
    attributes TEXT
  )

');

my $st = $dbh->prepare( "
  INSERT INTO grch37 ( sequence, source, feature, start, end, score, strand, frame, attributes, chromosome )
    VALUES ( @{[ join( ',', split( '', '?' x 10 ) ) ]} )
");

my $gtf = IO::File->new( $options->{gtf}, 'r' );
while( my $line = <$gtf> ) {
  my @data = split /\t/, $line, 9; 
  $st->execute( '', '', map { "\L$_\E" } @data[ FEATURE, START, END, SCORE, STRAND, FRAME, ATTRIBUTES, CHROMOSOME ] )
}
$gtf->close;

my $gene = IO::File->new( $options->{gene}, 'r' ) or die( "Can't open file for reading!");
while( my $line = <$gene> ) {
  my @data = split /\t/, $line, 12;
  $st->execute( '', '', 'gene', @data[ GSTART, GEND, GSCORE, GSTRAND, GFRAME, GENE, GCHROMOSOME] );
}
$gene->close;

$dbh->close;

