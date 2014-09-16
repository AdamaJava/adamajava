use strict;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use PGTools::Util;
use PGTools::Util::Fasta;
use PGTools::Util::Translate;
use IO::File;
use Data::Dumper;
use autodie;

my $fasta = PGTools::Util::Fasta->new_with_file( 'Homo_sapiens.GRCh37.74.cdna.all.fa' );
my $db = IO::File->new( 'CosmicFusionExport_v67_241013.tsv', 'r' );
my $oh = IO::File->new( 'Homo_sapiens.GRCh37.74.cdna.fusion.all.fa', 'w' );
my $th = IO::File->new( 'Homo_sapiens.GRCh37.74.cdna.fusion.translated.all.fa', 'w' );
my $el = IO::File->new( 'fusion.error.log', 'w');
my $gff = IO::File->new( 'ref_GRCh37.p13_top_level.gff3', 'r' );
my $gtf = IO::File->new( '../Homo_sapiens.GRCh37.71.gtf', 'r' );
my $tr  = PGTools::Util::Translate->new;


my $id_re = qr/(ENST\d+|NM_\d+)/;

my %frame_data = ();


# collect frame data
while( <$gff> ) {
  my @fields = split /\t/, $_;
  my ( $id ) = $fields[ 8 ] =~ /$id_re/;

  next unless $id;
  next if $frame_data{ $id };

  $frame_data{ $id } = $fields[ 6 ];
}


while( <$gtf> ) {
  my @fields = split( /\s+/, $_, 10 );

  my ( $id ) = $fields[ 9 ] =~ /(ENST\d+)/;

  next unless $id;
  next if $frame_data{ $id };

  $frame_data{ $id } = $fields[ 6 ];

}


$fasta->reset;

my %all_sequences = ();
# Read all the sequences
while( $fasta->next ) {
  my ( $id ) = $fasta->title =~ $id_re; 

  next unless $id;

  next if $all_sequences{ $id };

  $all_sequences{ $id } = [
    $fasta->title,
    $fasta->sequence_trimmed
  ];

}


while( <$db> ) {
  my @fields = split /\t/;

  next unless /$id_re/;

  my ( $cancer, $description ) = @fields[ 2, 7 ];


  $el->print( "DESCRIPTION: $description \n" );

  my ( $seq_a, $start_a, $end_a, $seq_b, $start_b, $end_b ) = $description =~ /
    \w+\{$id_re[^\}]*\}
    :r\.
    (\d+)_(\d+)
    _
    \w+\{$id_re[^\}]*\}
    :r\.
    (\d+)_(\d+)
  /x;

  my ( $_fa, $_fb ) = @frame_data{ $seq_a, $seq_b };

  $el->print( 
    "A: $seq_a, ST-A: $start_a, EN-A: $end_a \n B: $seq_b, ST-B: $start_b, EN-B: $end_b \n" 
  );

  print( "SEQ: $seq_a Seqence length: " .  length( $all_sequences{ $seq_a }[ 1 ] ) . " START_A: $start_a END: $end_a \n" );
  print( "SEQ: $seq_b Seqence length: " .  length( $all_sequences{ $seq_b }[ 1 ] ) . " START_B: $start_b END: $end_b \n" );


  next unless $all_sequences{ $seq_a };
  next unless $all_sequences{ $seq_b };
  next unless $frame_data{ $seq_a };
  next unless $frame_data{ $seq_b };

  my $l = sub {
    return length( $all_sequences{ shift }[1] );
  };



  my $final = substr( $all_sequences{ $seq_a }[1], $start_a - 1, $end_a - $start_a ) .
    substr( $all_sequences{ $seq_b }[1], $start_b - 1, $end_b - $start_b );


  my $accession = "$seq_a:$seq_b $cancer $description strand:$seq_a=$_fa;$seq_b=$_fb;";  

  # main output
  print $oh '>' . $accession . $fasta->eol .
      normalize( 
        $final
      ) .
    $fasta->eol;

  # publish translated output too
  for my $i ( 1 .. 3 ) {
    my $j = $_fa =~ /\-/ ? ( $i * -1 ) : $i;

    $tr->set_sequence( $final );

    # translate next
    my $translated = $tr->translate( frame => $j );
    my $mid = int( ( ( $end_a - 1 ) / 3 ) + 0.5 );
    my @post = ( substr( $translated, $mid + 1 ) =~ /(.+?[RK])(?!P)/g );

    print Dumper \@post;
    

    my ( $pre, $junc, $post ) = ( 
      substr( $translated, ( $mid - 1 ) - 30, 30 ),
      substr( $translated, $mid, 1 ),
      $post[ 0 ]
    );

    print $th '>' . "$accession frame:$j " . $fasta->eol .
      normalize( $pre . $junc . $post ) .
      $fasta->eol;

  }

}


