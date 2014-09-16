package PGTools::Util::AccessionHelper;

use strict;
use warnings;
use PGTools::Util;


sub id_for_accession {
  my ( $class, $accession ) = @_;

  # ENSG ids get the highest priority
  # if we find them, we'll use them
  # else we'll switch to normal lookup
  my ( $id ) = $accession =~ /(ENSG\d+)/;

  return $id if $id;

  my $rna_prefix      = qr/(?:(?:NM)|(?:NR)|(?:XM)|(?:XR))_/;

  my $protein_prefix  = qr/(?:[NXZAY]P_)/;

  my $genomic_prefix  = qr/
    (?:
      (?:NC) | (?:NG) | (?:NT) | (?:NW) | (?:NZ) | (?:NS) | (?:AC)
    )
  /x;

  my $nids = qr/
    n\d+
  /x;

  my $alpha_numeric   = qr/[A-Za-z0-9]+/;

  ( $id ) = grep { $_ } $accession =~ /
    (?:IPI:([A-Z0-9\.]+))   |
    (?:gene:(ENSG\d+))      |
    (?:tr\|([A-Z0-9]+))     |
    (?:(PGOHUM\S+))         |
    (?:
      \b
      ( 
        (?: $rna_prefix | $protein_prefix | $genomic_prefix )
        [0-9\.]+
      ) 
      \b
    )                       |
    (?i:gene:($alpha_numeric)) |
    (?i:($nids)) |
    (?i:(ENSP\d+))
  /x;

  $id;

}

"World's full of trivial complexities";

__END__
