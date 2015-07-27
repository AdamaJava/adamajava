package PGTools::Util::Translate;

use strict;

=head1 NAME

  PGTools::Util::Translate - A Translation tool to convert DNA condons to amino acids

=head1 SYNOPSIS


  # The sequence
  $translate = PGTools::Util::Translate->new( sequence => $sequence );

  $translate->set_sequence( $sequence );

  # Clear the existing sequence
  $translate->clear;

  # Actual translation
  PGTools::Util::Translate->codon_to_amino_acid( $codon )

  # or the object method
  # returns F -> Phenylalanine
  $translate->codon_to_amino_acid( 'TTC' )


  # Frame could be in [ 1, 2, 3, -1 -2 -3  ] 
  # Returns the modified sequence or copied one
  $translate->translate( frame => 1 )

=head1 DESCRIPTION

Is heavily inspired from 

http://etutorials.org/Programming/perl+bioinformatics/Part+I+Object-Oriented+Programming+in+Perl/Chapter+1.+Modular+Programming+with+Perl/1.7+Writing+Your+First+Perl+Module/

=cut

my $invalid_codon = qr/N/o;

my %genetic_code  = (
    'TCA' => 'S',    # Serine
    'TCC' => 'S',    # Serine
    'TCG' => 'S',    # Serine
    'TCT' => 'S',    # Serine
    'TTC' => 'F',    # Phenylalanine
    'TTT' => 'F',    # Phenylalanine
    'TTA' => 'L',    # Leucine
    'TTG' => 'L',    # Leucine
    'TAC' => 'Y',    # Tyrosine
    'TAT' => 'Y',    # Tyrosine
    'TAA' => '_',    # Stop
    'TAG' => '_',    # Stop
    'TGC' => 'C',    # Cysteine
    'TGT' => 'C',    # Cysteine
    'TGA' => '_',    # Stop
    'TGG' => 'W',    # Tryptophan
    'CTA' => 'L',    # Leucine
    'CTC' => 'L',    # Leucine
    'CTG' => 'L',    # Leucine
    'CTT' => 'L',    # Leucine
    'CCA' => 'P',    # Proline
    'CCC' => 'P',    # Proline
    'CCG' => 'P',    # Proline
    'CCT' => 'P',    # Proline
    'CAC' => 'H',    # Histidine
    'CAT' => 'H',    # Histidine
    'CAA' => 'Q',    # Glutamine
    'CAG' => 'Q',    # Glutamine
    'CGA' => 'R',    # Arginine
    'CGC' => 'R',    # Arginine
    'CGG' => 'R',    # Arginine
    'CGT' => 'R',    # Arginine
    'ATA' => 'I',    # Isoleucine
    'ATC' => 'I',    # Isoleucine
    'ATT' => 'I',    # Isoleucine
    'ATG' => 'M',    # Methionine
    'ACA' => 'T',    # Threonine
    'ACC' => 'T',    # Threonine
    'ACG' => 'T',    # Threonine
    'ACT' => 'T',    # Threonine
    'AAC' => 'N',    # Asparagine
    'AAT' => 'N',    # Asparagine
    'AAA' => 'K',    # Lysine
    'AAG' => 'K',    # Lysine
    'AGC' => 'S',    # Serine
    'AGT' => 'S',    # Serine
    'AGA' => 'R',    # Arginine
    'AGG' => 'R',    # Arginine
    'GTA' => 'V',    # Valine
    'GTC' => 'V',    # Valine
    'GTG' => 'V',    # Valine
    'GTT' => 'V',    # Valine
    'GCA' => 'A',    # Alanine
    'GCC' => 'A',    # Alanine
    'GCG' => 'A',    # Alanine
    'GCT' => 'A',    # Alanine
    'GAC' => 'D',    # Aspartic Acid
    'GAT' => 'D',    # Aspartic Acid
    'GAA' => 'E',    # Glutamic Acid
    'GAG' => 'E',    # Glutamic Acid
    'GGA' => 'G',    # Glycine
    'GGC' => 'G',    # Glycine
    'GGG' => 'G',    # Glycine
    'GGT' => 'G',    # Glycine
);


sub new {
  my ( $class, $sequence ) = @_;

  my $self = bless { }, $class;

  $self->set_sequence( $sequence )
    if $sequence;

  $self;

}

sub set_sequence {
  my ( $self, $sequence ) = @_;

  $self->{sequence} = $sequence; 

  $self;
}


sub codon_to_amino_acid {
  my ( $self, $codon ) = @_;

  return ''
    unless 
      $codon                    and 
      ( length( $codon ) == 3 ) and  
      exists( $genetic_code{ uc $codon } );

  $genetic_code{ uc $codon };

}

sub clear {
  my $self = shift;

  $self->{sequence} = '';

  $self;
}


sub translate {
  my ( $self, %options ) = @_;

  my $translated  = '';
  my $sequence    = $self->{sequence}; 
  my $frame       = $options{frame};

  return '' unless $frame;

  if( $frame < 0 ) {
    $sequence = reverse $sequence;
    $sequence =~ tr/ATGC/TACG/;
  }

  # Absolute value
  $frame      = abs $frame;
  $sequence   = substr( $sequence, $frame - 1 );

  CODON_LOOP: while( $sequence =~ /(.{3})/gc ) {

    my $codon = $1;

    ( $translated .= 'X' ) && next CODON_LOOP 
      if $codon =~ $invalid_codon;

    $translated .= $self->codon_to_amino_acid( $codon );
  }

  $translated;

}


sub frame_label {
  my ( $self, $frame ) = @_;

  my $label = ( $frame > 0 )
    ? "5'3' "
    : "3'5' ";


  return sprintf( 
    ' %s - Frame %d', 
    $label, 
    abs( $frame )
  );

}


1;
__END__
