package org.qcmg.sig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.sig.model.SigVcfMeta;

import com.fasterxml.jackson.databind.ObjectMapper;

public class VcfProfilerTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	public VcfProfiler vp;
	
	@Before
	public void setup() {
		vp = new VcfProfiler();
	}
	
	@Test
    public void runProcess() throws Exception {
    	final File positionsOfInterestFile = testFolder.newFile("runProcess.snps.vcf");
    	final File logFile = testFolder.newFile("runProcess.log");
    	final File inputVcfFile = testFolder.newFile("runProcess.qsig.vcf");
    	final String outputFileName = inputVcfFile.getAbsolutePath() + ".json";
		final File outputFile = new File(outputFileName);
	    	
	    SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
	    writeQsigVcf(inputVcfFile);
	    	
    	final int exitStatus = vp.setup(new String[] {"--log" , logFile.getAbsolutePath(), "--snpPositions" , positionsOfInterestFile.getAbsolutePath(), "--input" , inputVcfFile.getAbsolutePath(), "--output", outputFileName} );
    	assertEquals(0, exitStatus);
    	assertTrue(outputFile.exists());
    	
    	//JSON file to Java object
    	ObjectMapper mapper = new ObjectMapper();
        SigVcfMeta obj = mapper.readValue(outputFile, SigVcfMeta.class);
        assertEquals(6, obj.getNumberOfPositions());
        assertEquals(6, obj.getNumberOfHomPositions());
        assertEquals(0, obj.getNumberOfHetPositions());
        assertEquals(0, obj.getGeneHitCount());
        assertEquals(0, obj.getGeneHitCountPassingCoverage());
        assertEquals(0, obj.getUniqueGeneHitCount());
        Map<String, int[]> geneDistribution = obj.getGeneDist();
		assertEquals(0, geneDistribution.size());
		Map<Integer, Integer> homLengthDistribution = obj.getHomLengthDistribution();
		assertEquals(0, homLengthDistribution.size());
    }
	
	@Test
	public void runProcessWithGeneInfo() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithGeneInfo.snps.vcf");
		final File logFile = testFolder.newFile("runProcessWithGeneInfo.log");
		final File inputVcfFile = testFolder.newFile("runProcessWithGeneInfo.qsig.vcf");
		final String outputFileName = inputVcfFile.getAbsolutePath() + ".json";
		final File outputFile = new File(outputFileName);
		
		writeSnpPositionsWithGeneInfoVcf(positionsOfInterestFile);
		writeQsigVcf(inputVcfFile);
		
		final int exitStatus = vp.setup(new String[] {"--log" , logFile.getAbsolutePath(), "--snpPositions" , positionsOfInterestFile.getAbsolutePath(), "--input" , inputVcfFile.getAbsolutePath(), "--output", outputFileName} );
		assertEquals(0, exitStatus);
		assertTrue(outputFile.exists());
		
		//JSON file to Java object
		ObjectMapper mapper = new ObjectMapper();
		SigVcfMeta obj = mapper.readValue(outputFile, SigVcfMeta.class);
		assertEquals(6, obj.getNumberOfPositions());
		assertEquals(6, obj.getNumberOfHomPositions());
		assertEquals(0, obj.getNumberOfHetPositions());
		assertEquals(5, obj.getGeneHitCount());
		assertEquals(4, obj.getGeneHitCountPassingCoverage());
		assertEquals(5, obj.getUniqueGeneHitCount());
		Map<String, int[]> geneDistribution = obj.getGeneDist();
		assertEquals(5, geneDistribution.size());
		Map<Integer, Integer> homLengthDistribution = obj.getHomLengthDistribution();
		assertEquals(0, homLengthDistribution.size());
		
	}
	
	static void writeQsigVcf(File qsigVcf) throws IOException {
    	try (Writer writer = new FileWriter(qsigVcf);) {
    		writer.write("##fileformat=VCFv4.2\n");
    		writer.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n");
    		writer.write("chr3	183635768	.	G	.	.	.	QAF=t:0-0-10-0,rg1:0-0-10-0\n");
    		writer.write("chr4	75406448	random_649440	A	.\t.\t.\tQAF=t:0-0-20-0,rg1:0-0-20-0\n");
    		writer.write("chr4	95733906	random_1053689	G	.\t.\t.\tQAF=t:0-0-5-0,rg1:0-0-5-0\n");
    		writer.write("chr4	108826383	random_1146989	T	.\t.\t.\tQAF=t:0-0-15-0,rg1:0-0-15-0\n");
    		writer.write("chr4	159441457	random_1053689	G	.\t.\t.\tQAF=t:0-21-0-0,rg1:0-21-0-0\n");
    		writer.write("chr12	126890980	random_169627	G	.\t.\t.\tQAF=t:31-0-0-0,rg1:31-0--0\n");
    	}
    }
	
	 static void writeSnpPositionsWithGeneInfoVcf(File snpPositions) throws IOException {
    	try (Writer writer = new FileWriter(snpPositions);) {
    		writer.write("##fileformat=VCFv4.2\n");
    		writer.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n");
    		writer.write("chr3	183635768	random_1016708	C	.\t.\t.\tgene_id=ENSG00000237683;gene_version=5;transcript_id=ENST00000423372;transcript_version=3;exon_number=1;gene_name=AL627309.1;gene_source=ensembl;gene_biotype=protein_coding;transcript_name=AL627309.1-201;transcript_source=ensembl;transcript_biotype=protein_coding;exon_id=ENSE00002221580;exon_version=1;tag=basic;\n");
    		writer.write("chr4	75406448	random_649440	A	.\t.\t.\tgene_id=ENSG00000177000;gene_version=6;transcript_id=ENST00000376486;transcript_version=2;exon_number=1;gene_name=MTHFR;gene_source=ensembl_havana;gene_biotype=protein_coding;transcript_name=MTHFR-003;transcript_source=havana;transcript_biotype=protein_coding;havana_transcript=OTTHUMT00000006540;havana_transcript_version=1;exon_id=ENSE00001470707;exon_version=2;tag=cds_end_NF;tag=mRNA_end_NF;\n");
    		writer.write("chr4	95733906	random_1053689	G	.\t.\t.\tene_id=ENSG00000001461;gene_version=12;transcript_id=ENST00000003912;transcript_version=3;exon_number=3;gene_name=NIPAL3;gene_source=ensembl_havana;gene_biotype=protein_coding;transcript_name=NIPAL3-001;transcript_source=havana;transcript_biotype=protein_coding;havana_transcript=OTTHUMT00000009178;havana_transcript_version=2;exon_id=ENSE00001463425;exon_version=1;tag=basic;\n");
    		writer.write("chr4	108826383	random_1146989	T	.\t.\t.\t.\n");
    		writer.write("chr4	159441457	random_1053689	G	.\t.\t.\tgene_id=ENSG00000237435;gene_version=4;transcript_id=ENST00000598129;transcript_version=1;exon_number=4;gene_name=RP11-147C23.1;gene_source=havana;gene_biotype=protein_coding;transcript_name=RP11-147C23.1-008;transcript_source=havana;transcript_biotype=processed_transcript;havana_transcript=OTTHUMT00000461111;havana_transcript_version=1;exon_id=ENSE00003009230;exon_version=1;\n");
    		writer.write("chr12	126890980	random_169627	G	.\t.\t.\tgene_id=ENSG00000143369;gene_version=10;transcript_id=ENST00000470432;transcript_version=1;exon_number=4;gene_name=ECM1;gene_source=ensembl_havana;gene_biotype=protein_coding;transcript_name=ECM1-014;transcript_source=havana;transcript_biotype=processed_transcript;havana_transcript=OTTHUMT00000035835;havana_transcript_version=1;exon_id=ENSE00003579731;exon_version=1;\n");
    	}
    }
}
