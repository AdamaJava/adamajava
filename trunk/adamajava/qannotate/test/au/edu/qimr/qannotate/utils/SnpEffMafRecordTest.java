package au.edu.qimr.qannotate.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class SnpEffMafRecordTest {
	
	
	@Test
	public void getHeaderLine() {
		assertEquals("Hugo_Symbol	Entrez_Gene_Id	Center	NCBI_Build	Chromosome	Start_Position	End_Position	Strand	Variant_Classification	Variant_Type	Reference_Allele	Tumor_Seq_Allele1	Tumor_Seq_Allele2	DbSNP_RS	DbSNP_Val_Status	Tumor_Sample_Barcode	Matched_Norm_Sample_Barcode	Match_Norm_Seq_Allele1	Match_Norm_Seq_Allele2	Tumor_Validation_Allele1	Tumor_Validation_Allele2	Match_Norm_Validation_Allele1	Match_Norm_Validation_Allele2	Verification_Status	Validation_Status	Mutation_Status	Sequencing_Phase	Sequence_Source	Validation_Method	Score	BAM_File	Sequencer	Tumor_Sample_UUID	Matched_Norm_Sample_UUID	QFlag	ND	TD	Confidence	Eff_Impact	Consequence_rank	Novel_Starts	Var_Plus_Flank	dbSNP_AF	Germ_Counts	T_Depth	T_Ref_Count	T_Alt_Count	N_Depth	N_Ref_Count	N_Alt_Count	Transcript_ID	Amino_Acid_Change	CDS_Change	Codon_Change	Transcript_BioType	Gene_Coding	Exon_Intron_Rank	Genotype_Number	Effect_Ontology	Effect_Class	Input	Notes", SnpEffMafRecord.getSnpEffMafHeaderline());
	}
	
	@Test
	public void mafLine() {
		SnpEffMafRecord r = new SnpEffMafRecord();
		assertEquals("Unknown	0	QIMR_Berghofer	37	null	null	null	+	Unknown	UNKOWN	null	null	null	novel	null	null	null	null	null	null	null	null	null	Unknown	Unknown	Unknown	null	Unknown	none	null	null	Unknown	none	none	null	null	null	Unknown	Unknown	0	Unknown	Unknown	Unknown	null	0	0	0	0	0	0	null	null	null	null	null	null	null	null	null	null	null	null", r.getMafLine());
	}

	
	@Test
	public void setAndGet() {
		SnpEffMafRecord r = new SnpEffMafRecord();
		r.setColumnValue(MafElement.Hugo_Symbol, "HUGO");
		assertEquals("HUGO", r.getColumnValue(MafElement.Hugo_Symbol));
		assertEquals("HUGO	0	QIMR_Berghofer	37	null	null	null	+	Unknown	UNKOWN	null	null	null	novel	null	null	null	null	null	null	null	null	null	Unknown	Unknown	Unknown	null	Unknown	none	null	null	Unknown	none	none	null	null	null	Unknown	Unknown	0	Unknown	Unknown	Unknown	null	0	0	0	0	0	0	null	null	null	null	null	null	null	null	null	null	null	null", r.getMafLine());
		r.setColumnValue(MafElement.NCBI_Build, "38");
		assertEquals("38", r.getColumnValue(MafElement.NCBI_Build));
		assertEquals("HUGO	0	QIMR_Berghofer	38	null	null	null	+	Unknown	UNKOWN	null	null	null	novel	null	null	null	null	null	null	null	null	null	Unknown	Unknown	Unknown	null	Unknown	none	null	null	Unknown	none	none	null	null	null	Unknown	Unknown	0	Unknown	Unknown	Unknown	null	0	0	0	0	0	0	null	null	null	null	null	null	null	null	null	null	null	null", r.getMafLine());
	}
}
