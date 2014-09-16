use strict;
use IO::File;
use PGTools::Util::Fasta;
use PGTools::Util qw/
  normalize
/;

my $digested = shift @ARGV;
my $unique   = shift( @ARGV );
my $other    = shift( @ARGV );

die "Digested fa can't be found "
  unless -e $digested;

my $ufh = IO::File->new( $unique, 'w' );
my $ofh = IO::File->new( $other, 'w' );
my $dfa = PGTools::Util::Fasta->new_with_file( $digested );

my %sequences = ( );
my %other = ( );

while(  $dfa->next ) {
  unless( $sequences{ $dfa->sequence } ) {
    $sequences{ $dfa->sequence } = $dfa->title;
  } else {
    $sequences{ $dfa->sequence } = 0;
    $other{ $dfa->sequence } = $dfa->title;
  }
}

while( my ( $key, $value ) = each( %sequences ) ) {
  if( $value ) {
    print $ufh '>' . $value . $dfa->eol;
    print $ufh $key . $dfa->eol;
  }
}

while( my ( $key, $value ) = each( %other ) ) {
  print $ofh '>' . $value . $dfa->eol;
  print $ofh $key . $dfa->eol;
}

$ufh->close;
$ofh->close;
