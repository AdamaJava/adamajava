package PGTools::FDR::OMSSA;

use strict;
use parent 'PGTools::MSearch::Base';
use PGTools::Util::FDR;
use PGTools::Util::FDRScore;

use Data::Dumper;
use PGTools::Util qw/
  number
/;

use Scalar::Util; 


sub get_runnable {
  my ( $self ) = @_;

  my $concat = $self->is_decoy_concatenated;


  # cutoff not configurable
  my $fdr = $self->get_fdr_class->new(
    is_decoy_concatenated => $concat, 
    database              => ( $concat ? undef : $self->database ),
    decoy_database        => $self->decoy_database,
    target                => ( $concat ? undef : $self->ofile( '.csv' ) ), 
    cutoff                => $self->msearch_config->{cutoff}, 
    decoy                 => $self->ofile( '-d.csv' ),
    output                => $self->ofile( '-filtered.csv' ),
    instance              => $self,  

    scan_id               => sub { 
      shift->{ 'Filename/id' };
    },

    score                 => sub { 
      my $row = shift;
      my $evalue = $row->{ 'E-value' };
      my $value = 0;

      if( $evalue =~ /e/i ) {
        my ( $score, $exponent ) = split /e/i, $evalue;
        $value = ( ( $score * 1 ) * ( 10 ** ( $exponent * 1 ) ) ) + 0 ;
      } else {
        $value = ( $evalue * 1 ) + 0 ;
      }


      $value;
    },

    peptide               => sub { 
      $self->peptide( shift );
    },

    accession             => sub {
      shift->{Defline};
    }


  );

  sub { 
    print "About to process FDR for OMSSA ... \n";
    $fdr->run; 
    print "Done processing FDR for OMSSA \n";

  };

}


sub peptide {
  my $self = shift;
  shift->{Peptide};
}

sub protein {
  my $self = shift;
  shift->{Defline};
}


1;
__END__
