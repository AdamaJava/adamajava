use strict;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use PGTools::Util;
use PGTools::Util::Fasta;
use PGTools::Util::Translate;
use IO::File;
use Data::Dumper;
use autodie;

# open all essential files
my $fa = PGTools::Util::Fasta->new_with_file( 'Homo_sapiens.GRCh37.74.cdna.fusion.all.fa' ); 
my $oh = IO::File->new( 'Homo_sapiens.GRCh37.74.cdna.fusion.frame.all.fa', 'w' );
my $gff = IO::File->new( 'ref_GRCh37.p13_top_level.gff3', 'r' );
my $gtf = IO::File->new( '../Homo_sapiens.GRCh37.71.gtf', 'r' );
my $el = IO::File->new( 'fusion.frame.error.log', 'w' );

$fa->reset;

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


while( $fa->next ) {
  my ( $id_a, $id_b ) = $fa->title =~ /$id_re:$id_re/;
  my ( $_fa, $_fb ) = @frame_data{ $id_a, $id_b };

  next unless $_fa;
  next unless $_fb;

  print $oh '>' .
    $fa->title . ' ' . "strand:$id_a=$_fa;$id_b=$_fb;" . $fa->eol . normalize( $fa->sequence_trimmed ) . $fa->eol;

}




