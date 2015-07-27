package PGTools::Util::FDR;

use strict;
use warnings;
use parent 'PGTools::Util::FDRBase';
use Text::CSV;
use IO::File;
use PGTools::Util;

use constant SCORE => 0;


=head1 NAME

PGTools::Util::FDR

=head1 SYNOPSIS

  my $fdr = PGTools::Util::FDR->new(
    database              => '/path/to/database.fa',
    decoy_database        => '/path/to/decoy-database.fa',
    cutoff                => 0.05,

    target                => 'target.csv',
    decoy                 => 'decoy.csv',
    is_decoy_concatenated => 1,

    scan_id               => sub { ... },
    score                 => sub { ... },
    peptide               => sub { ... },
    accession             => sub { ... },

  );

  $fdr->run;

=cut
sub run {

  my $self = shift;

  my ( @target, @decoy );
  my ( $target_psm, $decoy_psm ) = $self->_get_psms;

  while( my ( $key, $value ) = each( %{ $target_psm } ) ) {

    
    push @target, $value->[ SCORE ];

    # if scan id exists in decoy psm
    # and scores are the same
    if( defined( $decoy_psm->{$key} ) && $value->[ SCORE ] == $decoy_psm->{$key}[ SCORE ] ) {
      unless( $self->_decoy_has_peptides_from_target( $value, $decoy_psm->{$key} ) ) {
        push @decoy, $decoy_psm->{$key}[ SCORE ];
      }
    }

    elsif( defined( $decoy_psm->{ $key } ) ) {
      push @decoy, $decoy_psm->{$key}[ SCORE ];
    }

  }

  while( my ( $key, $value ) = each( %{ $decoy_psm } ) ) {
    push @decoy, $value->[SCORE] unless defined( $target_psm->{ $key } );
  }

  @target = sort { $a <=> $b } @target;
  @decoy  = sort { $a <=> $b } @decoy;

  my $fdr   = $self->fdr( \@target, \@decoy );

  print "FDR: $fdr \n";

  $self->_publish_filtered( $fdr );
}


sub _publish_filtered {
  my ( $self, $fdr ) = @_;

  my $csv = $self->get_csv_handler;
  my $fh  = IO::File->new( $self->{target}, 'r' );
  my $ofh = IO::File->new( $self->{output}, 'w' );

  my @columns = map { trim $_ } @{ $csv->getline( $fh ) };

  # Set the columns
  $csv->column_names( @columns );

  # Publish headers in the output file
  $csv->print( $ofh, \@columns );

  $ofh->write( "\n" );

  while( my $row = $csv->getline_hr( $fh ) ) {
    if( $self->{score}->( $row ) < $fdr ) {
      $csv->print( 
        $ofh,
        [
          map {
            $row->{$_}
          } @columns
        ]
      );

      $ofh->write( "\n" );
    }
  }

  $fh->close;
  $ofh->close;
}


sub fdr {
  my ( $self, $target, $decoy ) = @_;

  my $fdr = 0;

  # $target and $decoy - contains all target and decoy hits respectively 
  # irrespective of wether the search was performed on decoy-concatenated or
  # seperate databases, the hits are separated out in former case
  #
  # The data structure is array of scores sorted by e-value 
  my ( $target_count, $decoy_count ) = ( scalar( @$target ), scalar( @$decoy ) );

  if( $target_count > 0 && $decoy_count <= 0 ) { 
    $fdr = ( sort { $a <=> $b } @$target )[ 0 ];
  }

  elsif ( $target_count <= 0 && $decoy_count > 0 ) {
    $fdr = ( sort { ( $a <=> $b ) * -1 } @$decoy )[ 0 ];
  }

  else {

    # XXX: No need to make a copy here
    my @target = @$target;
    my @decoy  = @$decoy;

    # Worst target score better than the best decoy score?
    if( $target[ $#target ] <  $decoy[ 0 ] ) {
      $fdr = $decoy[ 0 ];
    }

    else {

      my $fdr_cutoff = 0;

      foreach my $decoy_score ( @decoy ) {
        my $target_count = 0; 
        my $decoy_count  = 0;

        for ( @target ) {
          $target_count++ if $_ < $decoy_score;
        }

        for ( @decoy ) {
          $decoy_count++ if $_ < $decoy_score;
        }

        my $score = $self->get_score( $target_count, $decoy_count );

        print( "EVALUATING DECOY: $decoy_score, TCOUNT: $target_count, DCOUNT: $decoy_count, FDR: $score, CUTOFF: " . $self->{cutoff} . " \n" );


        if( $score <= $self->{cutoff} && $score >= $fdr_cutoff ) {
          $fdr        = $decoy_score;
          $fdr_cutoff = $score;
        }
      }
    }
  }


  $fdr;

}




sub _decoy_has_peptides_from_target {
  my ( $self, $target, $decoy ) = @_;

  my $decoy_peptides = join( ',', @{ $decoy }[ 1 .. scalar( @$decoy ) - 1 ] );

  for my $target_peptide ( @{ $target }[ 1 .. ( scalar( @$target ) - 1 ) ] ) {
    return 1 if $decoy_peptides =~ /$target_peptide/;
  }

  return 0;

}


1;
__END__
