use strict;
use IO::File;
use PGTools::Util::Fasta;
use PGTools::Util qw/
  normalize
/;

my $digested = shift @ARGV;
my $known    = shift @ARGV;
my $common   = shift( @ARGV) || 'COMMON.fa';
my $unique   = shift( @ARGV ) || 'UNIQUE.fa';

die "Digested fa can't be found "
  unless -e $digested;

die "Known fa can't be found "
  unless -e $known;

my $cfh = IO::File->new( $common, 'w' );
my $ufh = IO::File->new( $unique, 'w' );
my $dfa = PGTools::Util::Fasta->new_with_file( $digested );
my $kfa = PGTools::Util::Fasta->new_with_file( $known );

my %sequences = ( );

while(  $kfa->next ) {
  $sequences{ $kfa->sequence } = 1;
}

while( $dfa->next ) {

  if( exists( $sequences{ $dfa->sequence } ) ) {
    print $cfh '>'
      . $dfa->title 
      . $dfa->eol;

    print $cfh normalize( $dfa->sequence ); 

  }
  else {
    print $ufh '>'
      . $dfa->title 
      . $dfa->eol;

    print $ufh normalize( $dfa->sequence ); 
  }

}

$cfh->close;
$ufh->close;


