package PGTools::Util::FDRBase;

use strict;

use warnings;
use PGTools::Util;


sub new {
  my ( $class, %options ) = @_;

  bless { %options }, $class;
}


sub _split_files_and_resetup {

  my ( $self ) = @_;

  my $fh  = IO::File->new( $self->{decoy}, 'r' );

  my $tfh = IO::File->new( 
    (
      $self->{target} = $self->{instance}->ofile( '-target.csv' )
    ), 'w'
  );

  my $dfh = IO::File->new( 
    (
      $self->{decoy} = $self->{instance}->ofile( '-decoy.csv' )
    ), 'w'
  );

  my $csv = $self->get_csv_handler;

  my @columns = map { trim $_ } @{ $csv->getline( $fh ) }; 


  $csv->column_names( @columns ); 

  # Write column names
  map {
    $csv->print( $_, [ @columns ] );
    $_->write( "\n" );
  } ( $dfh, $tfh );

  while( my $row = $csv->getline_hr( $fh ) ) {

    my @fields = map { $row->{$_} } @columns;

    if( $self->{accession}->( $row ) =~ /^(rev|rand)_/ ) {
      $csv->print( $dfh, [ @fields ] );
      $dfh->write( "\n" );
    } else {
      $csv->print( $tfh, [ @fields ] );
      $tfh->write( "\n" );
    }
  }

  map { $_->close } ( $fh, $tfh, $dfh );

}



sub get_score {
  my ( $self, $target_count, $decoy_count ) = @_;

  return 0 if $target_count <= 0;

  
  if( $self->{is_decoy_concatenated} ) {
    ( $decoy_count / ( $target_count + $decoy_count ) ) * 2 * 100;
  }

  else {
    ( $decoy_count / $target_count ) * 100;
  }

}


sub get_csv_handler {
  my $self = shift;

  my $csv =  Text::CSV->new( {
    sep_char => ( $self->{tab_separated} ? "\t" : "," )
  } );

  $csv;
}



sub _handle_file {
  my ( $self, $filename ) = @_;

  my $csv = $self->get_csv_handler;
  my $fh  = IO::File->new( $filename, 'r' );

  my $heading = $csv->getline( $fh );



  # FIXME::
  return unless $heading;

  my @columns = map { trim $_ } @{ $heading };

  # set column names
  $csv->column_names(  \@columns );

  my %psm = ();

  # loop through each of the data line
  while( ( my $row = $csv->getline_hr( $fh ) ) || ( print( $csv->error_diag ) && 0 ) ) {

    my ( $scan_id, $score, $peptide ) = map { $_->( $row ) } ( @{ $self }{ qw/ scan_id score peptide / } );

    # define if it doesn't exist
    unless( $psm{ $scan_id } ) {
      $psm{ $scan_id } = [ $score, $peptide ];
    } 

    else {
      push @{ $psm{ $scan_id } }, $peptide;
    }
  }

  $fh->close;

  \%psm;

}


sub _get_psms {
  my $self = shift;

  $self->_split_files_and_resetup
    if $self->{is_decoy_concatenated};

  return ( 
    $self->_handle_file( $self->{target} ),
    $self->_handle_file( $self->{decoy} )
  );

}


1;
__END__
