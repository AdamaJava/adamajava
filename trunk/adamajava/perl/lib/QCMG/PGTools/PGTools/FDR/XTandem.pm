package PGTools::FDR::XTandem;

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
      shift->{ 'ScanID' };
    },

    score                 => sub { 
      my $row = shift;
      my $evalue = $row->{ 'Expect' };
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
      shift->{ 'Protein ID' };
    }

  );


  sub { 
    print "About to process FDR for XTandem ...";
    $fdr->run; 
    print "Done processing FDR for XTandem ";
  }

}

sub peptide {
  shift;
  shift->{Peptide};
}

sub protein {
  shift && shift->{'Protein ID'};
}



1;
__END__
