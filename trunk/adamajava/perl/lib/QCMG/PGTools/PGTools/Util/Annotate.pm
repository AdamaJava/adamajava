package PGTools::Util::Annotate;

use strict;
use warnings;
use PGTools::Util::Annotate::DB;
use PGTools::Util::AccessionHelper;
use PGTools::Util;
use PGTools::Util::Path;
use FindBin;

use IO::File;
use Text::CSV;
use Data::Dumper;

sub new {
  my $class = shift;
  my %options = @_;

  bless { 
    %options
  }, $class;

}

sub run {
  no warnings 'uninitialized';

  my $self = shift;

  my $db = PGTools::Util::Annotate::DB->new;
  my $allowed_ensgs = $self->allowed_ensgs;

  my $create_atlas_link = sub {
    my $ensg_id = shift;

    $allowed_ensgs->{ $ensg_id } 
      ? sprintf( 'http://proteinatlas.org/%s', $ensg_id ) 
      : 'NA';

  };

  # setup the database
  unless( $db->is_database_setup ) {
    $db->download_data;
  }

  my $input   = IO::File->new( $self->{input}, 'r' )    || die( "Error Opening file: " . $! ); 
  my $output  = IO::File->new( $self->{output}, 'w' )  || die( "Error Opening file: " . $! ); 

  my $error   = $self->{error_log} 
    ? ( IO::File->new( $self->{error_log}, 'w' )  or die( "Error opening file: " . $! ) )
    : undef;

  my $csv_in = Text::CSV->new( {
    sep_char  => ( ( $self->{type} eq 'tsv' ) ? "\t" : "," )
  } );

  my $csv_out = Text::CSV->new( { binary => 1 } );

  my $count = 0;
  my %done = ();

  # Ignore header 
  if( ! $self->{no_header} ) {
    $csv_in->getline( $input, $input );
    $count++;
  }

  $csv_out->print( $output, [ 'Merge ID', @{ $db->header_data->{ 'MASTER.csv' } }, 'HPRD Link', 'Protein Atlas', 'Error' ] );
  $output->write( "\n" );
  while( my $row = $csv_in->getline( $input ) ) {


    $count++;

    my $old_protein_id = $row->[ $self->{column} ];
    my $protein_id;

    # trim leading and trailing whitespace
    $old_protein_id =~ s/^\s*|\s*$//g;

    # must be an accession string, extract id
    my $original_protein_id = $old_protein_id;
    if( $old_protein_id =~ /\s/ ) {
      $protein_id = $original_protein_id = PGTools::Util::AccessionHelper->id_for_accession( $old_protein_id );
    }


    # move to next item if we have already processed this protein id
    exists( $done{ $protein_id } ) && next; 

    $done{ $protein_id } = 1;

    my $data;

    # No verbose output
    # print "protein_id: $protein_id \n";
    if( $protein_id && ( $data = $db->get_data_for( $protein_id ) ) ) {

      my $hprd_link   = sprintf( 'http://hprd.org/protein/%05d', $data->{hprd_id} );
      my $atlas_link  = $create_atlas_link->( $data->{ensg_id} );


      my $rows = [ $protein_id, @{ $data }{ @{ $db->column_data->{ 'MASTER.csv' } } }, $hprd_link, $atlas_link, '' ];
      $csv_out->print( $output, $rows ); 
      $output->write( "\n" );
    }

    elsif( $original_protein_id =~ /ENSG/ ) {

      my $atlas_link  = $create_atlas_link->( $original_protein_id );

      # don't publish if we don't even have atlas link
      next if $atlas_link eq 'NA';

      my $rows = [ 
        $protein_id,
        ( map { '' } @{ $db->column_data->{ 'MASTER.csv'} } ), '',
        $atlas_link,
        " Unable to annotate: $original_protein_id "
      ];

      $csv_out->print( $output, $rows ); 
      $output->write( "\n" );
    }


    else {
      $error->print( "Protein ID: $protein_id not found in row: $count \n" )
        if $error;
    }

  }

  $input->close;
  $output->close;

}

sub allowed_ensgs {
  my $class = shift;
  my %ensgs = (); 

  my $file = catfile( "$FindBin::Bin", "normal_tissue.csv" );

  return \%ensgs unless -e $file;

  foreach_csv_row $file => sub {

    my $gene = shift->{Gene};

    $ensgs{ $gene } = 1 
      unless exists( $ensgs{ $gene } );

  };


  \%ensgs;

}


1;
__END__
