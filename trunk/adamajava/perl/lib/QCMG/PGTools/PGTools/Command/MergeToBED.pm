package PGTools::Command::MergeToBED;

use strict;
use PGTools::Util::Fasta;
use Text::CSV;
use Data::Dumper;
use PGTools::Util;
use parent 'PGTools::Command';


sub run {

  my $class   = shift; 

  my $options = $class->get_options( [
    'merge|m=s', 'bed|b=s', 'database|d=s', 'color|c=s'
  ]);

  must_have "Input file", $options->{merge};
  must_be_defined "Ouput file: ", $options->{bed};

  my $csv   = Text::CSV->new;

  # open merge file for reading
  my $merge = IO::File->new( $options->{merge}, 'r' );

  # open BED file for writing
  my $bed   = IO::File->new( $options->{bed}, 'w' ) 
    or die( "Can't open @{[ $options->{bed} ]} for writing" );

  my $database = $options->{database} || 'Non-CodeDB';

  my $color   = $options->{color} || 'red';

  # set columns
  $csv->column_names( 
    @{ $csv->getline( $merge ) }
  );
  
  while( my $row = $csv->getline_hr( $merge ) ) {
    my $bed_data = $class->bed_entry_from_row( $row, $database, $class->to_rgb( $color ) );

    if( ref( $bed_data ) eq 'ARRAY' ) {
      $bed->print( join( "\t", @$bed_data ) . "\n" );
    }

    else {
      warn "can't extract BED data";
    }


  }

  $bed->close;
}

sub bed_entry_from_row {

  my $class = shift;
  my $row = shift;
  my $database = shift;
  my $color = shift;

  my $gssp = qr/
    (?<id>GSSP\d+)
    \s+
    (?<chromosome>chr\w+):
    POSITION\((?<start_pos>\d+)\-(?<end_pos>\d+)\)
    \s*
    Frame:\s*
    (?<strand>[-+])
  /x;


  my $pg = qr/
    (?<id>PGOHUM\d+)
    \s+
    loc:(?<chromosome>chr\w+)
    \|
    (?<start_pos>\d+)
    \-
    (?<end_pos>\d+)
    \|
    (?<strand>[-+])
  /x;

  my $splice = qr/
    (?<id>ENS[PG]\d+)
    ;
    (?<chromosome>\d+)
    :
    (?<start_pos>\d+)
    ,
    (?<end_pos>\d+)
    ,
    (?<strand>[-+])
  /x;

  my $utr = qr/
    (?<id>ENSG\d+)
    # ignore ENSP ids and the weird 2 digits
    \|[^\|]+\|(?<chromosome>\d+)\|\-?\d+\|
    (?<start_pos>\d+(?:;\d+)*)\|
    (?<end_pos>\d+(?:;\d+)*)\|
    .*
    frame:\s+(?<frame>\-?\d+)
  /x;


  my ( $start_pos, $end_pos, $strand, $chromosome );
  my ( $thick_start, $thick_end, $block_size ) = ( 0, 0, 0 );

  my $protein = $row->{Protein} || $row->{protein};

  if( 
      ( $protein  =~ /$gssp/)  || 
      ( $protein  =~ /$pg/ ) || 
      ( $protein  =~ /$splice/ ) || 
      ( $protein  =~ /$utr/ ) 
  ) {

    my %matches = %+;

    ( $start_pos, $end_pos, $strand, $chromosome ) = @matches{ qw/start_pos end_pos strand chromosome / };

    if( $strand !~ /^\+|-$/ && $matches{frame} ) {
      $strand = ( $matches{frame} < 0 ) ? '-' : '+';
    }

    if( index( $start_pos, ';' ) > 0 && index( $end_pos, ';' ) > 0 ) {
      ( $start_pos, $end_pos ) = ( 
        ( split( ';', $start_pos ) )[ 0 ],
        ( reverse( split( ';', $end_pos ) ) )[ 0 ]
      );
    }

    # Add chr if required
    $chromosome = 'chr' . $chromosome if $chromosome =~ /^\d+$/;


  }

  elsif( $protein  =~ /attributes:\(([^\)]+)\)/ ) {
    my $attributes = $1;

    my %properties = map {
      my ($key, $value ) = $_ =~ /([^=]+)=(.*)/;
      $key => $value
    } split /;/, $attributes;

    ( $start_pos, $end_pos, $strand, $chromosome, $thick_start, $thick_end ) = 
      @properties{ qw/chromStart chromEnd strand chrom thickStart thickEnd/ };

    $block_size = 2;

  }

  else {
    warn "Doesn't match";  
    warn $protein; 
    return undef;
  }

  # reverse for negetive strand
  ( $start_pos, $end_pos ) = ( $end_pos, $start_pos )
    if ( $strand =~ /\-/ ) && $start_pos > $end_pos;

  [ $chromosome, $start_pos, $end_pos, $database, 0, $strand, $thick_start, $thick_end, $color, $block_size ]; 

}

sub to_rgb {
  my $class = shift;
  my $color = shift;

  my %colors = (
    red     => '255,0,0',
    green   => '0,255,0',
    blue    => '0,0,255',
    black   => '0,0,0',
    orange  => '255,140,0',
    yellow  => '128,128,128',
  );

  $colors{ $color };

}



1;
__END__
