use strict;
use FindBin;
use Getopt::Long;

BEGIN {

  my @paths = (
    "$FindBin::Bin/../lib",
    "/Perl/lib/pgtools"
  );
  # In the parent directory?
  for ( @paths ) {
    unshift @INC, $_ if -d $_; 
  }

}

use PGTools::Util::Fasta;
use PGTools::Util qw/
  normalize
/;

use IO::File;
# use Bio::Protease;
use Data::Dumper;

my $file = shift @ARGV;
my $output = shift @ARGV;

die "File doesn't exist: $file"
  unless -e $file;

my $fasta     = PGTools::Util::Fasta->new_with_file( $file );
my $ofh       = IO::File->new( $output, 'w' );

sub digest {

  my $string = shift;

  $string =~ s/\n//g;

  my @pieces;

  @pieces = $string =~ /(.+?[RK])(?!P)/g;

  @pieces;

}


while( $fasta->next ) {
  my $seq     = $fasta->sequence; 
  my @pieces  = grep { $_ } split /_/, $seq;

  for my $piece ( @pieces ) {
    for my $di ( digest( $piece ) ) {
      if( length( $di ) >= 7 && length( $di ) < 36 ) {
        print $ofh '>'
          . $fasta->title 
          . " #$di# "
          . $fasta->eol;

        print $ofh normalize( $di ); 
      }

    }
  }
}

$ofh->close;
