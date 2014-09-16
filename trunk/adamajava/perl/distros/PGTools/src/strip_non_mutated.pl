use strict;
use FindBin;
use lib "$FindBin::Bin/../lib";
use Data::Dumper;
use PGTools::Util;
use PGTools::Util::Fasta;
use Getopt::Long;

my $options = { };
GetOptions( $options, 'fasta=s', 'output=s' );

must_have( "Fasta File", $options->{fasta},  ); 
must_be_defined( "Output", $options->{output} );

my $fh = IO::File->new( $options->{output}, 'w' ) or die( "Can't open file for writing" );
my $fa = PGTools::Util::Fasta->new_with_file( $options->{fasta} );
$fa->reset;


while( $fa->next ) {

  next unless $fa->title =~ /ENSP\d+\s+\w+/;
  print $fh '>' .
    $fa->title .
    $fa->eol;

  print $fh normalize( $fa->sequence_trimmed );

}

$fh->close;
