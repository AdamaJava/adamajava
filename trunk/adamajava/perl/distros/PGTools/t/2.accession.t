use strict;
use Test::More; 

use_ok 'PGTools::Util::AccessionHelper';

my %accessions = (
  'IPI:IPI00000001.2|SWISS-PROT:O95793-1|TREMBL:A8K622;Q59F99|ENSEMBL:ENSP00000360922|REFSEQ:NP_059347|H-INV:HIT000329496|VEGA:OTTHUMP00000031233' 
    => 'IPI00000001.2',

  'tr|Q59F99|Q59F99_HUMAN Staufen isoform b variant (Fragment) OS=Homo sapiens PE=2 SV=1'
    => 'Q59F99',

  'ENST00000318149 cdna:known chromosome:GRCh37:6:43299513:43337181:-1 gene:ENSG00000171467 gene'
    => 'ENSG00000171467',

  'gi|306140489|ref|NM_001017388.2| Homo sapiens toll-like receptor 10 (TLR10), transcript variant 2, mRNA'
    => 'NM_001017388.2',

  'gi|306140489|ref|XM_001017388.2| Homo sapiens toll-like receptor 10 (TLR10), transcript variant 2, mRNA'
    => 'XM_001017388.2',

  'OTTHUMP00000163744 pep:all chromosome:VEGA47:18:71920531:71959251:-1 Gene:OTTHUMG00000132843 Transcript:OTTHUMT00000256316'
    => 'OTTHUMG00000132843',

  'PGOHUM00000242757 loc:chr11|62098420-62099288|+ exons:62098420-62099288 segs:1-869 frame: 2 details:(source=Yale_UCSC;frame=.;score=.;end=62099288;feature=transcript;seqname=chr11;strand=+;start=62098420) attributes:(gene_id=PGOHUM00000242757;transcript_type=pseudogene;ucsc_id=NM_002520.6-8;transcript_name=PGOHUM00000242757;gene_status=UNKNOWN;gene_type=pseudogene;parent_id=ENSG00000181163;level=3;tag=2way_pseudo_cons;transcript_status=UNKNOWN;yale_id=PGOHUM00000242757;transcript_id=PGOHUM00000242757;gene_name=PGOHUM00000242757) #VDNDEDEHQLSLR#
  VDNDEDEHQLSLR'
    => 'PGOHUM00000242757',

  'n37 | AB030733 | IGF2AS RNA | Homo sapiens | IGF2AS | NONCODE v2.0 | n37 | NULL | 0.6950430 | -0.1106332'
    => 'n37',

  'some random string'
    => undef

);

while( my ( $key, $value ) = each( %accessions ) ) {
  is( PGTools::Util::AccessionHelper->id_for_accession( $key ), $value );
}

done_testing;

