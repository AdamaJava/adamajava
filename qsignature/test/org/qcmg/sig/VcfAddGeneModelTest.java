package org.qcmg.sig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qio.vcf.VcfFileReader;

public class VcfAddGeneModelTest {
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	public VcfAddGeneModel vp;
	
	@Before
	public void setup() {
		vp = new VcfAddGeneModel();
	}
	
	@Test
    public void runProcess() throws Exception {
    	final File logFile = testFolder.newFile("runProcess.log");
    	final File inputVcfFile = testFolder.newFile("runProcess.snps.vcf");
    	final File geneModelFile = testFolder.newFile("runProcess.gene.model.gtf");
    	final String outputFileName = inputVcfFile.getAbsolutePath() + ".with.gene.model.vcf";
		final File outputFile = new File(outputFileName);
	    	
	    writeSnpPositionsVcf(inputVcfFile);
	    writeGeneModelFile(geneModelFile);
	    	
    	final int exitStatus = vp.setup(new String[] {"--log" , logFile.getAbsolutePath(), "--geneModel" , geneModelFile.getAbsolutePath(), "--input" , inputVcfFile.getAbsolutePath(), "--output", outputFileName} );
    	assertEquals(0, exitStatus);
    	assertTrue(outputFile.exists());
    	
    	
    	/*
    	 * load in VcfRecords and check to see if gene model info has been added
    	 */
    	List<VcfRecord> vcfRecs = new ArrayList<>();
    	try(VcfFileReader reader = new VcfFileReader(outputFile)) {
    		for (VcfRecord rec : reader) {
    			vcfRecs.add(rec);
    		}
    	}
    	
    	assertEquals(6, vcfRecs.size());
    	assertEquals(3, vcfRecs.stream().filter(vcf -> ".".equals(vcf.getInfo())).count());
    	
    	
    	for (VcfRecord rec : vcfRecs) {
    		if (rec.getPosition() == 52651) {
                assertTrue(rec.getInfo().startsWith("gene_id=123_456;gene_version=15;transcript_id=ENST00000480186;transcript_version=7;"));
    		} else if (rec.getPosition() == 108826383) {
    			assertEquals("gene_id=ENSG00000131686;gene_version=15;transcript_id=ENST00000480186;transcript_version=7;gene_name=CA6;gene_source=ensembl_havana;gene_biotype=protein_coding;transcript_name=CA6-205;transcript_source=ensembl_havana;transcript_biotype=protein_coding;tag=basic;transcript_support_level=2;", rec.getInfo());
    		} else if (rec.getPosition() == 159441457) {
                assertTrue(rec.getInfo().startsWith("gene_id=987_654;gene_version=15;transcript_id=ENST00000480186;transcript_version=7;"));
    		} else {
                assertEquals(".", rec.getInfo());
    		}
    	}
    }
	
	static void writeSnpPositionsVcf(File output) throws IOException {
    	try (Writer writer = new FileWriter(output)) {
    		writer.write("##fileformat=VCFv4.2\n");
    		writer.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n");
    		writer.write("chr1	52651	.	T	.	.	.	.\n");
    		writer.write("chr4	75406448	.	T	.	.	.	.\n");
    		writer.write("chr4	95733906	.	T	.	.	.	.\n");
    		writer.write("chr4	108826383	.	T	.	.	.	.\n");
    		writer.write("chr4	159441457	.	T	.	.	.	.\n");
    		writer.write("chr12	126890980	.	T	.	.	.	.\n");
    	}
    }
	
	 static void writeGeneModelFile(File output) throws IOException {
    	try (Writer writer = new FileWriter(output)) {
    		writer.write("chr1	ensembl_havana	exon	51651	53651	.	+	.	gene_id \"123_456\"; gene_version \"15\"; transcript_id \"ENST00000480186\"; transcript_version \"7\"; exon_number \"3\"; gene_name \"CA6\"; gene_source \"ensembl_havana\"; gene_biotype \"protein_coding\"; transcript_name \"CA6-205\"; transcript_source \"ensembl_havana\"; transcript_biotype \"protein_coding\"; exon_id \"ENSE00001913776\"; exon_version \"2\"; tag \"basic\"; transcript_support_level \"2\";\n");
			writer.write("chr4	ensembl_havana	CDS	75405448	75407448	.	+	2	gene_id \"ENSG00000131686\"; gene_version \"15\"; transcript_id \"ENST00000480186\"; transcript_version \"7\"; exon_number \"3\"; gene_name \"CA6\"; gene_source \"ensembl_havana\"; gene_biotype \"protein_coding\"; transcript_name \"CA6-205\"; transcript_source \"ensembl_havana\"; transcript_biotype \"protein_coding\"; protein_id \"ENSP00000435280\"; protein_version \"1\"; tag \"basic\"; transcript_support_level \"2\";\n");
			writer.write("chr4	ensembl_havana	stop_codon	95733806	95733956	.	+	0	gene_id \"ENSG00000131686\"; gene_version \"15\"; transcript_id \"ENST00000480186\"; transcript_version \"7\"; exon_number \"3\"; gene_name \"CA6\"; gene_source \"ensembl_havana\"; gene_biotype \"protein_coding\"; transcript_name \"CA6-205\"; transcript_source \"ensembl_havana\"; transcript_biotype \"protein_coding\"; tag \"basic\"; transcript_support_level \"2\";\n");
			writer.write("chr4	ensembl_havana	five_prime_utr	108826283	108826483	.	+	.	gene_id \"ENSG00000131686\"; gene_version \"15\"; transcript_id \"ENST00000480186\"; transcript_version \"7\"; gene_name \"CA6\"; gene_source \"ensembl_havana\"; gene_biotype \"protein_coding\"; transcript_name \"CA6-205\"; transcript_source \"ensembl_havana\"; transcript_biotype \"protein_coding\"; tag \"basic\"; transcript_support_level \"2\";\n");
			writer.write("chr4	ensembl_havana	three_prime_utr	159441357	159441557	.	+	.	gene_id \"987_654\"; gene_version \"15\"; transcript_id \"ENST00000480186\"; transcript_version \"7\"; gene_name \"CA6\"; gene_source \"ensembl_havana\"; gene_biotype \"protein_coding\"; transcript_name \"CA6-205\"; transcript_source \"ensembl_havana\"; transcript_biotype \"protein_coding\"; tag \"basic\"; transcript_support_level \"2\";\n");
			writer.write("chr12	ensembl_havana	transcript	126890970	126890990	.	+	.	gene_id \"ENSG00000131686\"; gene_version \"15\"; transcript_id \"ENST00000377436\"; transcript_version \"6\"; gene_name \"CA6\"; gene_source \"ensembl_havana\"; gene_biotype \"protein_coding\"; transcript_name \"CA6-201\"; transcript_source \"ensembl_havana\"; transcript_biotype \"protein_coding\"; tag \"CCDS\"; ccds_id \"CCDS57970\"; tag \"basic\"; transcript_support_level \"1\";\n");
    		
    	}
    }
}
