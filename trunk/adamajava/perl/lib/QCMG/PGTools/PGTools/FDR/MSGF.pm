package PGTools::FDR::MSGF;

use strict;
use parent 'PGTools::MSearch::Base';

use Data::Dumper;
use PGTools::Util qw/
  number
/;

use PGTools::Util::FDR;
use PGTools::Util::FDRScore;
use Scalar::Util; 


sub get_runnable {
  my ( $self ) = @_;

  my $concat = $self->is_decoy_concatenated;
  my $fdr = $self->get_fdr_class->new(
    is_decoy_concatenated => $concat, 
    database              => ( $concat ? undef : $self->database ),
    decoy_database        => $self->decoy_database,
    target                => ( $concat ? undef : $self->ofile( '.csv' ) ), 
    cutoff                => $self->msearch_config->{cutoff}, 
    decoy                 => $self->ofile( '-d.csv' ),
    output                => $self->ofile( '-filtered.csv' ),
    instance              => $self,  
    tab_separated         => 1, 

    scan_id               => sub { 
      my $row = shift;
      # print Dumper $row;
      $row->{ 'SpecID' };
    },

    score                 => sub { 
      my $row = shift;
      my $evalue = $row->{ 'EValue' };
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
      shift->{ 'Protein' };
    }

  );


  sub { 
    print "About to process FDR for MSGF...";
    $fdr->run; 
    print "Done processing FDR for MSGF ";
  }

}

sub peptide {
  my ( $self, $row ) = @_;
  my $peptide = $row->{Peptide};

  $peptide =~ tr/[A-Za-z]//cd;

  $peptide;
}

sub protein {
  shift && shift->{Protein};
}




1;
__END__
