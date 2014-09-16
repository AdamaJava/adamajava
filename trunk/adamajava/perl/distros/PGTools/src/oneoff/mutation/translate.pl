use strict;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use PGTools::Util;
use PGTools::Util::Fasta;
use PGTools::Util::Translate;
use IO::File;
use Data::Dumper;
use autodie;

my $input  = 'Homo_sapiens.GRCh37.74.cds.cancer.all.fa';
my $output = 'Homo_sapiens.GRCh37.74.cds.cancer.translated.all.fa';
my $fasta = PGTools::Util::Fasta->new_with_file( $input );
my $fh = IO::File->new( $output, 'w' );
my $translate = PGTools::Util::Translate->new;

$fasta->reset;
while( $fasta->next ) {
  my ( $frame, $strand ) = $fasta->title =~ /\s+frame=(\d);strand=([-+])\s+/;

  # start translation
  $translate->set_sequence( $fasta->sequence_trimmed );

  my $seq = $translate->translate( frame => 1 );

  print $fh '>'
    . $fasta->title . $fasta->eol . $seq . $fasta->eol;

}

close( $fh );
