package PGTools::Util::FDRScore;

use strict;
use warnings;
use parent 'PGTools::Util::FDRBase';
use Data::Dumper;
use PGTools::Util;

sub run {
  my $self = shift;

  # target_psm and decoy_psm contains target and decoy hits
  # irrespective of decoy-concatenated search or separate search
  #
  # the data-structure is hash of arrays, with each key of the hash
  # being the scan_id and value array containing the first value as
  # score, and the rest of the values being peptides
  #
  # This data-structure is first transformed into arrays, so sorting
  # can be possible
  @{ $self }{ qw/target_psm decoy_psm/ } = map {
    $self->_rearrange( $_ ) 
  } $self->_get_psms;


  $self->compute_simple_fdr;
  $self->compute_qvalues;
  $self->compute_fdr_score;
  $self->restructure;
  $self->_publish_filtered;

}

sub _normalize_scan_id {
  my ( $self, $scan_id ) = @_;

  $scan_id =~ tr/a-zA-Z0-9_//cd;

  $scan_id;
}

sub restructure {
  my $self = shift;
  my $target_psm = $self->{target_psm};
  my %output = ( );

  for my $hit ( @{ $target_psm } ) {
    $output{ $self->_normalize_scan_id( $hit->{scan_id} ) } = $hit;
  }

  $self->{restructured} = \%output;

}


sub _publish_filtered {
  my $self = shift;

  my ( $csv_in, $csv_out ) = ( $self->get_csv_handler, $self->get_csv_handler );

  my $fh  = IO::File->new( $self->{target}, 'r' );
  my $ofh = IO::File->new( $self->{output}, 'w' );
  my $cutoff = $self->{cutoff};

  # current set
  my @columns = ( map { trim $_ } @{ $csv_in->getline( $fh ) } );
  my @columns_new = ( @columns, qw/fdr_est qvalue fdr_score/ );
    
  $csv_in->column_names( @columns );
  $csv_out->column_names( @columns_new );

  # write columns to the output file
  $csv_out->print( $ofh, \@columns_new );
  $ofh->write( "\n" );

  # restructured output
  my $rs = $self->{restructured};

  # process
  while( my $row = $csv_in->getline_hr( $fh ) ) {
    my $scan_id = $self->_normalize_scan_id( $self->{scan_id}->( $row ) );
    my $hit = $rs->{$scan_id};

    # publish this
    if( $hit->{ fdr_score } < $cutoff ) {
      $csv_out->print( 
        $ofh,
        [
          ( 
            map {
              $row->{$_}
            } @columns
          ),
          (
            map {
              $hit->{$_}
            } qw/fdr_est qvalue fdr_score/
          )
        ]
      );

      $ofh->write( "\n" );

    }


  }
  $fh->close;
  $ofh->close;
}


sub compute_simple_fdr {
  my $self = shift;

  my $target_count = 0;

  for my $target ( @{ $self->{target_psm} } ) {

    my $score = $target->{score};

    $target_count++;

    my $decoy_count  = 0;
    for my $decoy ( @{ $self->{decoy_psm} } ) {
      if( $decoy->{score} < $target->{score} ) {
        $decoy_count++;
      }

      else {
        last;
      }
    }

    # print "DECOY_COUNT: $decoy_count \n";
    # print "TARGET_COUNT: $target_count \n";

    $target->{fdr_est} = $self->get_score( $target_count, $decoy_count );

    # print "SCORE: ", $target->{fdr_est}, "\n";


  }

}

sub compute_qvalues {
  my $self = shift;

  my $fdr_min = undef;
  my $target_psm = $self->{target_psm};

  for( my $i=scalar( @{ $target_psm } ) - 1; $i >= 0; $i-- ) {

    my $fdr = $target_psm->[$i]{fdr_est};

    # first iteration
    unless( defined( $fdr_min ) ) {
      $fdr_min = $target_psm->[$i]{qvalue} = $fdr;
    }

    else {
      $fdr_min = $fdr if $fdr < $fdr_min; 
      $target_psm->[$i]{qvalue} = $fdr_min;
    }
  }
}

sub compute_fdr_score {
  my $self = shift;

  my $ALMOST_ZERO = 0; 
  my $previous_evalue = $ALMOST_ZERO;
  my $previous_qvalue = $ALMOST_ZERO;
  my $previous_previous_evalue = $ALMOST_ZERO;
  my $counter_backward_step = 0;



  my $i = 0;
  for my $target ( @{ $self->{target_psm} } ) {

    my $current_evalue = $target->{evalue}; 
    my $current_qvalue = $target->{qvalue};

    # set default
    $target->{fdr_score} = 0;

=pod
    print "\nCUR EVAL: $current_evalue\n";
    print "PREV EVAL: $previous_evalue\n";
    print "CUR QVAL: $current_qvalue\n";
    print "PREV QVAL: $previous_qvalue\n";
    print "CURRENT FDR: " . $target->{fdr_est} . "\n"; 
=cut

    # if the qvalue changes at this step, compute intercept and slope
    if( $current_qvalue > $previous_qvalue ) {

      my ( $slope, $intercept );


      # if evalues last evalue was different
      if( $current_evalue != $previous_evalue ) {
        # print "NOT EQUAL \n";
        $slope      = ( $current_qvalue - $previous_qvalue ) / ( $current_evalue - $previous_evalue );
        $intercept  = $previous_qvalue - $slope * $previous_evalue;
      }

      # if last evalue was the same, try to use one before the last
      else {
        $slope      = ( $current_qvalue - $previous_qvalue ) / ( $current_evalue - $previous_previous_evalue );
        $intercept  = $previous_qvalue - $slope * $previous_previous_evalue;
      }


      # now if the qvalue din't change for a few 
      # iterations, then we need to compute scores for all
      # hits since the last computations, they all have the same slope and 
      # intercept and fall in teh same line
      if( $counter_backward_step > 0 ) {

        # print "$i : BACKWARD STEP SET UP RUNNING THROUGH IT: $counter_backward_step \n";
        for my $j ( ( 0 .. $counter_backward_step  ) ) {
          my $index = $i - $j;
          $self->{target_psm}[$index]{fdr_score} = $slope * $self->{target_psm}[ $index ]{ evalue } + $intercept;
        }

      }

      else {
        # print "ASSIGNING FDR SCORE\n";
        $target->{fdr_score} = $slope * $current_evalue + $intercept;
      }

      $counter_backward_step = 0;
      
      ( $previous_previous_evalue, $previous_evalue ) = ( $previous_evalue, $current_evalue ) 
        if $current_evalue > $previous_evalue ;


      $previous_qvalue = $current_qvalue;

    }

    else {
      # print "SETTING BACKWARD STEP\n";
      $counter_backward_step++;
    }


    $i++;

  }

  my ( $j, $k );
  my $target_psm = $self->{target_psm};
  $j = $k = scalar( @{ $target_psm } ) - 1;

  # likely that some of the fdr scores haven't been setup
  if( $target_psm->[ $k ] == 0 ) { 

    # find the index from where to fix
    $k-- while $target_psm->[ $k ] == 0;

    # save the last know score
    my $last_known_fdr_score = $target_psm->[ $k - 1 ]->{fdr_score};

    # set the rest of the indexes to this value
    $target_psm->[ $_ ]{fdr_score} = $last_known_fdr_score for $k .. $j;

  }



}




sub _rearrange {
  my ( $self, $psm ) = @_;
  my @list = ();

  while( my ( $key, $value ) = each( %$psm ) ) {
    push @list, {
      scan_id   => $key,
      score     => $value->[0],
      evalue    => $value->[0],
      peptides  => [ @{ $value }[ 1 .. ( scalar( @$value ) - 1 ) ] ]
    }
  }

  [ sort { $a->{score} <=> $b->{score} } @list ];

}




1;
__END__

