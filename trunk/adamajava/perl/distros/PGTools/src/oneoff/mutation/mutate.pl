use strict;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use PGTools::Util;
use PGTools::Util::Fasta;
use PGTools::Util::Translate;
use IO::File;
use Data::Dumper;
use autodie;

my $input = 'Homo_sapiens.GRCh37.74.cds.cancer.translated.all.fa';
my $output = 'Homo_sapiens.GRCh37.74.cds.cancer.translated.mutated.all.fa';
my $fa = PGTools::Util::Fasta->new_with_file( $input );
my $fh = IO::File->new( $output, 'w' );
my $el = IO::File->new( 'mutate.error.log', 'w' );

sub split_by {
  my ( $seq, $pos ) = @_;
  ( substr( $seq, 0, $pos - 2), substr( $seq, $pos - 1, 1), substr( $seq, $pos ) );
}

sub mutation {
  my ( $seq, $pos ) = @_;

  my ( $pre, $curr, $post ) = split_by $seq, $pos;

  $el->print( "POS: $pos, PRE: $pre, POST: $post, CURR: $curr \n" );

  my $post_seq = ( $post =~ /(.+?[RK])(?!P)/g )[ 0 ];
  my $final = substr( $pre, -5 ) . lc( $curr ) . $post_seq;

  unless( $post_seq ) {
    $el->print( "ERROR, NO POST SEQ FOUND \n" );
  } else {
    $el->print( "FINAL: $final \n" );
  }

  return undef unless $post_seq;

  return $final;

}


$fa->reset;

while( $fa->next ) {
  my $title = $fa->title;
  my $sequence = $fa->sequence_trimmed;


  # get mutation data
  my ( $mutation_data_raw ) = $title =~ /(mutation_id=.*)$/;

  # collect data
  my %mutation_data = (
    map {
      my ( $key, $val ) = split /=/;

      $key => $val;

    } split /;/, $mutation_data_raw

  );

  my $mutation;

  # substitution
  if( $mutation_data{ mutation } =~ /c\.(\d+)([A-Z]+)\-([A-Z]+)/ ) {

    $el->print( "SUBSTITUTION \n" );
    $el->print( "PEPTIDE_MUTATION: " . $mutation_data{ mutation_peptide } . "\n" );

    # extract the mutation of the peptide
    my ( $from, $pos, $to ) = $mutation_data{ mutation_peptide } =~ /p\.([A-Z]+)(\d+)([A-Z]+)/i;

    $mutation = mutation( $sequence, $pos );
  }

  # range deletes
  elsif( $mutation_data{ mutation } =~ /((\d+)_(\d+)del(\d+))|(\d+)del([A-Z]+)/ ) {
    my ( $pos ) = $mutation_data{ mutation_peptide } =~ /p\.[A-Z](\d+)/;

    $mutation = mutation( $sequence, $pos );

  }

  elsif( $mutation_data{ mutation } =~ /(\d+)_(\d+)ins([A-Z]+)/ ) {
    my ( $pos ) = $mutation_data{ mutation_peptide } =~ /c\.[A-Z]+(\d+)/;

    $mutation = mutation( $sequence, $pos );
  }

  else {
    next;
  }


  next unless $mutation;

  print $fh '>'
    . $fa->title . $fa->eol . $mutation . $fa->eol;


}
