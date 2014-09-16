use strict;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use PGTools::Util;
use PGTools::Util::Fasta;
use IO::File;
use Data::Dumper;
use autodie;

my $input = 'Homo_sapiens.GRCh37.74.cds.all.fa';
my $output = 'Homo_sapiens.GRCh37.74.cds.cancer.all.fa';
my $cancer_data = 'CosmicMutantExportCensus_v67_241013.tsv';
my $error_log = 'error.log';

my $ch = IO::File->new( $cancer_data, 'r' ); 
my $oh = IO::File->new( $output, 'w' );
my $eh  = IO::File->new( $error_log, 'w' );
my $fasta = PGTools::Util::Fasta->new_with_file( $input );
my $data = { };

sub mutation_details {

  my $val = shift;

  join ';', map {
    # copy 
    my $item = $val->{$_};

    # remove weird ones
    $item =~ s/>/-/g;

    # return 
    "$_=$item";

  } qw/
    mutation_id mutation cancer mutation_peptide
  /;

}

sub mutate {
  my ( $sequence, $value ) = @_;

  my $info = $value->{mutation};

  $info =~ s/^c\.//;

  if( $value->{mutation_description} eq 'Substitution - Missense' ) {

    my ( $pos, $from, $to ) = $info =~ /^\s*(\d+)([A-Z]+)>([A-Z]+)/;

    unless( $from ) {
      ( $pos, $from, $to ) = $info =~ /^\s*(\d+)_\d+(\w+)>(\w+)/;
    }

    if( substr( $sequence, $pos - 1, length( $from ) ) ne $from ) {
      $eh->print( "ERROR: " . $value->{id} . " Mutation: " . $info . " Expected: " . $value->{mutation} . '@' . ( $pos - 1 ) . " But found: " .  substr( $sequence, $pos - 1, length( $from ) )  . "\n" );
      return undef;
    }

    elsif( length( $sequence ) <= ( $pos - 1 ) + length( $from ) ) {
      $eh->print( "ERROR: " . $value->{id} . " Sequence length: " . length( $sequence ) . ' but position required is ' . ( ( $pos - 1 ) + length( $from ) ) . " Cant proceed \n" ); 
      return undef;
    }



    # substitute
    substr( $sequence, $pos - 1, length( $from ) ) = $to;

    # return the mutated sequence
    return $sequence;
  }


  elsif( $value->{mutation_description} eq 'Insertion - Frameshift' ) {

    my ( $from, $to, $what) = $info =~ /(\d+)_(\d+)ins(\w+)/;

    if( $to > length( $sequence ) ) {
      $eh->print( "ERROR: " . $value->{id} . " Sequence length: " . length( $sequence ) . ' but position required is ' . $to . " Cant proceed \n" ); 
      return undef;
    }

    if( $what =~ /\d/ ) {
      $eh->print( "ERROR: " . $value->{id} . " Matched number instead of string: $what \n" );
      return undef;
    }

    # insert
    substr( $sequence, $from - 1, ( $to - $from ) ) = $what;

    # return the sequence
    return $sequence;
  }

  elsif( $value->{mutation_description} eq 'Deletion - Frameshift' ) {

    my $how_many;
    my ( $pos, $what ) = $info =~ /(\d+)del([A-Z]+)/;

    unless( $what ) {
      ( $pos, $how_many ) = $info =~ /(\d+)_\d+del(\d+)/;

      print "INFO: $info \n";
      print "SEQLEN: " . length( $sequence ) . "\n";

      if( ( $pos - 1 ) + $how_many  > length( $sequence ) ) {
        $eh->print( "ERROR: " . $value->{id} . " Sequence length: " . length( $sequence ) . ' but position required is ' . ( ( $pos - 1 ) + $how_many ) . " Cant proceed \n" ); 
        return undef;
      }

      substr( $sequence, $pos - 1, $how_many ) = '';
      return $sequence;
    }

    if( ( $pos - 1 ) + length( $what ) > length( $sequence ) ) {
      $eh->print( "ERROR: " . $value->{id} . " Sequence length: " . length( $sequence ) . ' but position required is ' . ( ( $pos - 1 ) + length( $what ) ) . " Cant proceed \n" ); 
      return undef;
    }

    substr( $sequence, $pos - 1, length( $what ) ) = '';

    return $sequence;
  }


  return undef;



}

my %headers = (
  id => 1,
  cancer => 6,
  site => 7,
  mutation_id => 11,
  mutation => 12,
  mutation_peptide => 13,
  mutation_description => 14 
);


# ignore first line
$ch->getline;

# collect data
while( my $line = <$ch> ) {

  my @fields = split /\t/, $line;
  my $id = $fields[ $headers{ id } ];

  next unless $id =~ /^ENST/;

  $data->{ $id } ||= [ ];

  my %d = ( );
  while( my ( $key, $value) = each %headers ) {
    $d{ $key } = $fields[ $value ];
  }

  push @{ $data->{ $id } }, \%d; 

}
# print Dumper( $data );

$fasta->reset;
while( $fasta->next ) {

  # get the transcript
  my ( $id ) = $fasta->title =~ /(ENST\d+)/;

  # no mutation exists, skip
  next unless $data->{$id};

  # apply mutation
  foreach my $value ( @{ $data->{$id} } ) {

    my $mutated = mutate( $fasta->sequence_trimmed, $value );

    next unless $mutated;


    print $oh '>' .
      $fasta->title . ' ' . mutation_details( $value ) . $fasta->eol . $mutated .  $fasta->eol;

  }

}

close( $oh );






