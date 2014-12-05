package au.edu.qimr.qannotate.utils;

public class SnpEffFieldRecord {
	
	/*
	 *
Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_change| Amino_Acid_length | Gene_Name | Gene_BioType | Coding | Transcript | Exon [ | ERRORS | WARNINGS ] )

Effect	Effect of this variant. See details here.
Effect impact	Effect impact {High, Moderate, Low, Modifier}. See details here.
Functional Class	Functional class {NONE, SILENT, MISSENSE, NONSENSE}.
Codon_Change / Distance	Codon change: old_codon/new_codon OR distance to transcript (in case of upstream / downstream)
Amino_Acid_Change	Amino acid change: old_AA AA_position/new_AA (e.g. 'E30K')
Amino_Acid_Length	Length of protein in amino acids (actually, transcription length divided by 3).
Gene_Name	Gene name
Transcript_BioType	Transcript bioType, if available.
Gene_Coding	[CODING | NON_CODING]. This field is 'CODING' if any transcript of the gene is marked as protein coding.
Transcript_ID	Transcript ID (usually ENSEMBL IDs)
Exon/Intron Rank	Exon rank or Intron rank (e.g. '1' for the first exon, '2' for the second exon, etc.)
Genotype_Number	Genotype number corresponding to this effect (e.g. '2' if the effect corresponds to the second ALT)
Warnings / Errors	Any warnings or errors (not shown if empty).




Impact	Meaning	Example
HIGH	The variant is assumed to have high (disruptive) impact in the protein, probably causing protein truncation, loss of function or triggering nonsense mediated decay.	stop_gained, frameshift_variant
MODERATE	A non-disruptive variant that might change protein effectiveness.	missense_variant, inframe_deletion
LOW	Assumed to be mostly harmless or unlikely to change protein behavior.	synonymous_variant
MODIFIER	Usually non-coding variants or variants affecting non-coding genes, where predictions are difficult or there is no evidence of impact.	exon_variant, downstream_gene_variant





Effect
Seq. Ontology	Effect
Classic	Note & Example	Impact
coding_sequence_variant	CDS	The variant hits a CDS.	MODIFIER
chromosome	CHROMOSOME_LARGE DELETION	A large parte (over 1%) of the chromosome was deleted.	HIGH
coding_sequence_variant	CODON_CHANGE	One or many codons are changed 
e.g.: An MNP of size multiple of 3	MODERATE
inframe_insertion	CODON_INSERTION	One or many codons are inserted 
e.g.: An insert multiple of three in a codon boundary	MODERATE
disruptive_inframe_insertion	CODON_CHANGE_PLUS CODON_INSERTION	One codon is changed and one or many codons are inserted 
e.g.: An insert of size multiple of three, not at codon boundary	MODERATE
inframe_deletion	CODON_DELETION	One or many codons are deleted 
e.g.: A deletion multiple of three at codon boundary	MODERATE
disruptive_inframe_deletion	CODON_CHANGE_PLUS CODON_DELETION	One codon is changed and one or more codons are deleted 
e.g.: A deletion of size multiple of three, not at codon boundary	MODERATE
downstream_gene_variant	DOWNSTREAM	Downstream of a gene (default length: 5K bases)	MODIFIER
exon_variant	EXON	The vairant hits an exon.	MODIFIER
exon_loss_variant	EXON_DELETED	A deletion removes the whole exon.	HIGH
frameshift_variant	FRAME_SHIFT	Insertion or deletion causes a frame shift 
e.g.: An indel size is not multple of 3	HIGH
gene_variant	GENE	The variant hits a gene.	MODIFIER
intergenic_region	INTERGENIC	The variant is in an intergenic region	MODIFIER
conserved_intergenic_variant	INTERGENIC_CONSERVED	The variant is in a highly conserved intergenic region	MODIFIER
intragenic_variant	INTRAGENIC	The variant hits a gene, but no transcripts within the gene	MODIFIER
intron_variant	INTRON	Variant hits and intron. Technically, hits no exon in the transcript.	MODIFIER
conserved_intron_variant	INTRON_CONSERVED	The variant is in a highly conserved intronic region	MODIFIER
miRNA	MICRO_RNA	Variant affects an miRNA	MODIFIER
missense_variant	NON_SYNONYMOUS_CODING	Variant causes a codon that produces a different amino acid 
e.g.: Tgg/Cgg, W/R	MODERATE
initiator_codon_variant	NON_SYNONYMOUS_START	Variant causes start codon to be mutated into another start codon (the new codon produces a different AA). 
e.g.: Atg/Ctg, M/L (ATG and CTG can be START codons)	LOW
stop_retained_variant	NON_SYNONYMOUS_STOP	Variant causes stop codon to be mutated into another stop codon (the new codon produces a different AA). 
e.g.: Atg/Ctg, M/L (ATG and CTG can be START codons)	LOW
rare_amino_acid_variant	RARE_AMINO_ACID	The variant hits a rare amino acid thus is likely to produce protein loss of function	HIGH
splice_acceptor_variant	SPLICE_SITE_ACCEPTOR	The variant hits a splice acceptor site (defined as two bases before exon start, except for the first exon).	HIGH
splice_donor_variant	SPLICE_SITE_DONOR	The variant hits a Splice donor site (defined as two bases after coding exon end, except for the last exon).	HIGH
splice_region_variant	SPLICE_SITE_REGION	A sequence variant in which a change has occurred within the region of the splice site, either within 1-3 bases of the exon or 3-8 bases of the intron.	LOW
splice_region_variant	SPLICE_SITE_BRANCH	A varaint affective putative (Lariat) branch point, located in the intron.	LOW
splice_region_variant	SPLICE_SITE_BRANCH_U12	A varaint affective putative (Lariat) branch point from U12 splicing machinery, located in the intron.	MODERATE
stop_lost	STOP_LOST	Variant causes stop codon to be mutated into a non-stop codon 

	 */
	
	
	
	

}
