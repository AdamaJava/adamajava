package org.qcmg.snp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.illumina.IlluminaRecord;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;

public class PipelineTest {
	
	private static final String C = ":";
	private static final String SC = ";";
	private static final String T = "\t";
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testLoadIlluminaData() throws Exception {
		File illuminaFile = testFolder.newFile("illumina.file");
		Map<ChrPosition, IlluminaRecord> illuminaMap = new HashMap<ChrPosition, IlluminaRecord>();
		
		// populate
		IlluminaFileGenerator.createIlluminaFile(illuminaFile);
		Pipeline.loadIlluminaData(illuminaFile.getAbsolutePath(), illuminaMap);
		
		assertEquals(false, illuminaMap.isEmpty());
		assertEquals(39, illuminaMap.size());
		
	}
	
	@Test
	public void testUpdateAnnotation() {
		QSnpRecord rec = new QSnpRecord();
		assertEquals(null, rec.getAnnotation());
		Pipeline.updateAnnotation(rec, null);
		assertEquals(null, rec.getAnnotation());
		Pipeline.updateAnnotation(rec, "");
		assertEquals("", rec.getAnnotation());
		Pipeline.updateAnnotation(rec, "MIN");
		assertEquals("MIN", rec.getAnnotation());
		Pipeline.updateAnnotation(rec, "COV");
		assertEquals("MIN;COV", rec.getAnnotation());
		
		// reset
		rec.setAnnotation(null);
		Pipeline.updateAnnotation(rec, "PASS");
		assertEquals("PASS", rec.getAnnotation());
		Pipeline.updateAnnotation(rec, "MIN");
		assertEquals("MIN", rec.getAnnotation());
		Pipeline.updateAnnotation(rec, "PASS");
		assertEquals("PASS", rec.getAnnotation());
	}
	
	@Test
	public void testCheckForEndsOfReadSomatic() {
		Pipeline pipeline = new TestPipeline();
		QSnpRecord snp = new QSnpRecord();
		snp.setChromosome("chr1");
		snp.setPosition(100);
		snp.setRef('A');
		snp.setDbSnpId("rs12345");
		snp.setMutation("A>C");
		snp.setClassification(Classification.SOMATIC);
		assertEquals(null, snp.getAnnotation());
		pipeline.checkForEndsOfReads(snp, null, null, '\u0000');
		assertEquals(null, snp.getAnnotation());
		
		
		Accumulator tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef());
		assertEquals(SnpUtils.END_OF_READ + 1, snp.getAnnotation());
		
		// add another read - this time at the end
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104);
		snp.setAnnotation(null);		// reset annotation
		
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef());
		assertEquals(SnpUtils.END_OF_READ + 2, snp.getAnnotation());
		
		// add some reads where the POI will be in the middle
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		snp.setAnnotation(null);		// reset annotation
		
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef());
		assertEquals(SnpUtils.END_OF_READ + 2, snp.getAnnotation());
		
		// another 4 of these and we will have the 5 good reads - but all on the same strand
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		snp.setAnnotation(null);		// reset annotation
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef());
		assertEquals(SnpUtils.END_OF_READ + 2, snp.getAnnotation());
		
		// add another read - this time on the reverse strand - but within 5bp of read end
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, false, 50, 100, 104);
		snp.setAnnotation(null);		// reset annotation
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef());
		assertEquals(SnpUtils.END_OF_READ + 3, snp.getAnnotation());
		
		// add another read - again on the reverse strand - but outwith 5bp of read end
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105);
		tumour.addBase((byte)'C', (byte)30, false, 50, 100, 104);
		tumour.addBase((byte)'C', (byte)30, false, 50, 100, 105);
		snp.setAnnotation(null);		// reset annotation
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef());
		assertEquals(null, snp.getAnnotation());
	}
	
	@Test
	public void testCheckForStrandBiasGermline() {
		Pipeline pipeline = new TestPipeline();
		QSnpRecord snp = new QSnpRecord();
		snp.setChromosome("chr1");
		snp.setPosition(100);
		snp.setRef('A');
		snp.setDbSnpId("rs12345");
		snp.setMutation("A>C");
		snp.setClassification(Classification.GERMLINE);
		
		assertEquals(null, snp.getAnnotation());
		pipeline.checkForStrandBias(snp, null, null, '\u0000');
		assertEquals(null, snp.getAnnotation());
		Accumulator normal = new Accumulator(100);
		normal.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		pipeline.checkForStrandBias(snp, normal, null, 'A');
		assertEquals(SnpUtils.STRAND_BIAS, snp.getAnnotation());
		// reset
		snp.setAnnotation(null);
		
		// now add another base to the normal acc - on the other strand - should no longer get the annotation
		normal.addBase((byte)'C', (byte)30, false, 100, 100, 200);
		pipeline.checkForStrandBias(snp, normal, null, 'A');
		assertEquals(null, snp.getAnnotation());
		
		// setup a tumour acc 
		Accumulator tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
		
		// switch to germline and try again
		snp.setClassification(Classification.SOMATIC);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(SnpUtils.STRAND_BIAS, snp.getAnnotation());
		
		// reset
		snp.setAnnotation(null);
		tumour.addBase((byte)'C', (byte)30, false, 100, 100, 200);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
	}
	
	@Test
	public void testCheckForStrandBiasSomatic() {
		Pipeline pipeline = new TestPipeline();
		QSnpRecord snp = new QSnpRecord();
		snp.setChromosome("chr1");
		snp.setPosition(100);
		snp.setRef('A');
		snp.setDbSnpId("rs12345");
		snp.setMutation("A>C");
		snp.setClassification(Classification.SOMATIC);
		
		assertEquals(null, snp.getAnnotation());
		pipeline.checkForStrandBias(snp, null, null, '\u0000');
		assertEquals(null, snp.getAnnotation());
		Accumulator tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		pipeline.checkForStrandBias(snp, null, tumour, 'A');
		assertEquals(SnpUtils.STRAND_BIAS, snp.getAnnotation());
		// reset
		snp.setAnnotation(null);
		
		// now add another base to the tumour acc - on the other strand - should no longer get the annotation
		tumour.addBase((byte)'C', (byte)30, false, 100, 100, 200);
		pipeline.checkForStrandBias(snp, null, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
		
		// setup a normal acc 
		Accumulator normal = new Accumulator(100);
		normal.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		normal.addBase((byte)'C', (byte)30, true, 100, 100, 200);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
		
		// switch to germline and try again
		snp.setClassification(Classification.GERMLINE);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(SnpUtils.STRAND_BIAS, snp.getAnnotation());
		
		// reset
		snp.setAnnotation(null);
		normal.addBase((byte)'C', (byte)30, false, 100, 100, 200);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
	}
	
	@Test
	public void testConvertQSnpToVCFSomatic() throws SnpException, IOException, Exception {
		// create qsnp record
		QSnpRecord snp = new QSnpRecord();
		snp.setChromosome("chr1");
		snp.setPosition(100);
		snp.setRef('A');
		snp.setDbSnpId("rs12345");
		snp.setMutation("A>C");
		snp.setNormalGenotype(GenotypeEnum.AA);
		snp.setTumourGenotype(GenotypeEnum.AC);
		
		Pipeline pipeline = new TestPipeline();
		VCFRecord vcf = pipeline.convertQSnpToVCF(snp);
		
		assertEquals(snp.getChromosome(), vcf.getChromosome());
		assertEquals(snp.getPosition(), vcf.getPosition());
		assertEquals(snp.getRef(), vcf.getRef());
		assertEquals("C", vcf.getAlt());
		assertEquals("", vcf.getInfo());
//		assertEquals(VcfUtils.INFO_MUTATION + "=A>C", vcf.getInfo());
		
		// add SOMATIC
		snp.setClassification(Classification.SOMATIC);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(Classification.SOMATIC.toString(), vcf.getInfo());

		// add in tumour nucleotides
		String tumourNucleotides = "C:15[18.95],19[19.35],A:2[27.02],3[29.03]"; 
		snp.setTumourNucleotides(tumourNucleotides);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(Classification.SOMATIC + SC + VcfUtils.INFO_MUTANT_READS + "=34" , vcf.getInfo());
		
		// add in Novel starts
		snp.setTumourNovelStartCount(5);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(Classification.SOMATIC + SC + VcfUtils.INFO_MUTANT_READS + "=34;" 
				+ VcfUtils.INFO_NOVEL_STARTS + "=5" , vcf.getInfo());
		
		// format field
//		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT + T
//				+ ".:.:." + T + ".:.:" + tumourNucleotides.replace(":", "")
//				, vcf.getExtraFields().get(0));
//		
		String normalNucleotides = "A:19[26.47],14[23.7],C:0[0],1[1]"; 
		snp.setNormalNucleotides(normalNucleotides);
//		vcf = pipeline.convertQSnpToVCF(snp);
//		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT + T
//				+ ".:.:" + normalNucleotides.replace(":", "") + T + ".:.:" + tumourNucleotides.replace(":", "")
//				, vcf.getExtraFields().get(0));
		
//		snp.setNormalGenotype(GenotypeEnum.AA);
//		snp.setTumourGenotype(GenotypeEnum.AC);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT + T
				+ "0/0:A/A:" + normalNucleotides.replace(":", "") + T + "0/1:A/C:" + tumourNucleotides.replace(":", "")
				, vcf.getExtraFields().get(0));
		
		System.out.println(vcf.toString());
	}
	@Test
	public void testConvertQSnpToVCFGermline() throws SnpException, IOException, Exception {
		// create qsnp record
		QSnpRecord snp = new QSnpRecord();
		snp.setChromosome("chr1");
		snp.setPosition(100);
		snp.setRef('G');
		snp.setDbSnpId("rs12345");
		snp.setMutation("G>A");
		snp.setNormalGenotype(GenotypeEnum.AG);
		snp.setTumourGenotype(GenotypeEnum.AG);
		
		Pipeline pipeline = new TestPipeline();
		VCFRecord vcf = pipeline.convertQSnpToVCF(snp);
		
		assertEquals(snp.getChromosome(), vcf.getChromosome());
		assertEquals(snp.getPosition(), vcf.getPosition());
		assertEquals(snp.getRef(), vcf.getRef());
		assertEquals("A", vcf.getAlt());
		assertEquals("", vcf.getInfo());
//		assertEquals(VcfUtils.INFO_MUTATION + "=G>A", vcf.getInfo());
		
		// add GERMLINE
		snp.setClassification(Classification.GERMLINE);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals("", vcf.getInfo());
		
		// add in tumour nucleotides
		String tumourNucleotides = "A:12[26.65],5[28.2],G:1[20],8[24.33]"; 
		snp.setTumourNucleotides(tumourNucleotides);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals("", vcf.getInfo());
		
		String normalNucleotides = "A:5[28.01],10[26.6],G:9[19.34],4[25.51]"; 
		snp.setNormalNucleotides(normalNucleotides);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(VcfUtils.INFO_MUTANT_READS + "=15", vcf.getInfo());
		
		// add in Novel starts
		snp.setNormalNovelStartCount(7);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(VcfUtils.INFO_MUTANT_READS + "=15;" 
				+ VcfUtils.INFO_NOVEL_STARTS + "=7" , vcf.getInfo());
		
		// format field
//		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT + T
//				+ ".:.:" + normalNucleotides.replace(":", "") + T + ".:.:" + tumourNucleotides.replace(":", "") , vcf.getExtraFields().get(0));
		
//		snp.setNormalGenotype(GenotypeEnum.AG);
//		snp.setTumourGenotype(GenotypeEnum.AG);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT + T
				+ "0/1:A/G:" + normalNucleotides.replace(":", "") + T + "0/1:A/G:" + tumourNucleotides.replace(":", "")
				, vcf.getExtraFields().get(0));
		
		System.out.println(vcf.toString());
	}
	@Test
	public void testConvertQSnpToVCFFilter() throws SnpException, IOException, Exception {
		// create qsnp record
		QSnpRecord snp = new QSnpRecord();
		snp.setChromosome("chr1");
		snp.setPosition(100);
		snp.setRef('G');
		snp.setDbSnpId("rs12345");
		snp.setMutation("G>A");
		snp.setClassification(Classification.SOMATIC);
		// add in tumour nucleotides
		String tumourNucleotides = "A:0[26.65],4[28.2],G:1[20],8[24.33]"; 
		snp.setTumourNucleotides(tumourNucleotides);
		String normalNucleotides = "A:5[28.01],10[26.6],G:9[19.34],4[25.51]"; 
		snp.setNormalNucleotides(normalNucleotides);
		snp.setNormalGenotype(GenotypeEnum.GG);
		snp.setTumourGenotype(GenotypeEnum.AG);
		// add in Novel starts
		snp.setTumourNovelStartCount(3);
		
		Pipeline pipeline = new TestPipeline();
		pipeline.classifyPileupRecord(snp);
		assertEquals(SnpUtils.LESS_THAN_12_READS_NORMAL + SC + SnpUtils.MUTANT_READS + SC + SnpUtils.NOVEL_STARTS , snp.getAnnotation());
		VCFRecord vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(SnpUtils.LESS_THAN_12_READS_NORMAL + SC + SnpUtils.MUTANT_READS + SC + SnpUtils.NOVEL_STARTS , vcf.getFilter());
		
		// reset annotation
		snp.setAnnotation(null);
		snp.setTumourNovelStartCount(4);
		pipeline.classifyPileupRecord(snp);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(SnpUtils.LESS_THAN_12_READS_NORMAL + SC + SnpUtils.MUTANT_READS , vcf.getFilter());
		
		snp.setAnnotation(null);
		tumourNucleotides = "A:1[26.65],4[28.2],G:1[20],8[24.33]"; 
		snp.setTumourNucleotides(tumourNucleotides);
		snp.setNormalCount(12);
		pipeline.classifyPileupRecord(snp);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(SnpUtils.PASS , vcf.getFilter());
		
		System.out.println(vcf.toString());
	}
	
	
	@Test
	public void testAddDbSnpData() throws Exception {
		String chr = "chr1";
		int position = 10234;
			
		ChrPosition cp = new ChrPosition(chr, position);
		QSnpRecord snp = new QSnpRecord();
		snp.setChromosome(chr);
		snp.setPosition(position);
		snp.setTumourGenotype(GenotypeEnum.CT);
		
		TestPipeline tp = new TestPipeline();
		tp.positionRecordMap.put(cp, snp);
		
		// create dbSNP vcf file
		File vcfFile = testFolder.newFile();
		createVcfFile(vcfFile, "1	10234	rs145599635	C	T	.	.	RSPOS=10234;dbSNPBuildID=134;SSR=0;SAO=0;VP=050000000004000000000100;WGT=0;VC=SNV;ASP");
		
		tp.addDbSnpData(vcfFile.getAbsolutePath());
		
		assertEquals(null, tp.positionRecordMap.get(cp).getDbSnpId());
		// set to SOMATIC
		snp.setClassification(Classification.SOMATIC);
		tp.addDbSnpData(vcfFile.getAbsolutePath());
		assertEquals("rs145599635", tp.positionRecordMap.get(cp).getDbSnpId());
		snp.setDbSnpId(null);		//reset
		// set to GERMLINE
		snp.setClassification(Classification.GERMLINE);
		tp.addDbSnpData(vcfFile.getAbsolutePath());
		assertEquals(null, tp.positionRecordMap.get(cp).getDbSnpId());
		// and now set the normal genotype
		snp.setNormalGenotype(GenotypeEnum.CT);
		tp.addDbSnpData(vcfFile.getAbsolutePath());
		assertEquals("rs145599635", tp.positionRecordMap.get(cp).getDbSnpId());
		
		position = 1026344;
			
		cp = new ChrPosition(chr, position);
		snp = new QSnpRecord();
		snp.setChromosome(chr);
		snp.setPosition(position);
		snp.setTumourGenotype(GenotypeEnum.CG);
		
		tp.positionRecordMap.put(cp, snp);
		
		// create dbSNP vcf file
		File vcfFile2 = testFolder.newFile();
		String data = "1	1026344	rs34528299	G	C	.	.	RSPOS=1026344;RV;dbSNPBuildID=126;SSR=0;SAO=0;VP=050000000000000000000100;GENEINFO=C1orf159:54991;WGT=0;VC=SNV\n" + 
			 		"1	1026344	rs34609238	G	GA	.	.	RSPOS=1026344;RV;dbSNPBuildID=126;SSR=0;SAO=0;VP=050000000000000000000200;GENEINFO=C1orf159:54991;WGT=0;VC=SNV";	
		createVcfFile(vcfFile2, data);
		
		tp.addDbSnpData(vcfFile2.getAbsolutePath());
		
		assertEquals(null, tp.positionRecordMap.get(cp).getDbSnpId());
		// set to SOMATIC
		snp.setClassification(Classification.SOMATIC);
		tp.addDbSnpData(vcfFile2.getAbsolutePath());
		assertEquals("rs34528299", tp.positionRecordMap.get(cp).getDbSnpId());
		snp.setDbSnpId(null);		//reset
		// set to germline - should be empty
		snp.setClassification(Classification.GERMLINE);
		tp.addDbSnpData(vcfFile2.getAbsolutePath());
		assertEquals(null, tp.positionRecordMap.get(cp).getDbSnpId());
		
		// set normal genotype - should now pick up dbsnp id
		snp.setNormalGenotype(GenotypeEnum.CG);
		tp.addDbSnpData(vcfFile2.getAbsolutePath());
		assertEquals("rs34528299", tp.positionRecordMap.get(cp).getDbSnpId());
	}
	
	@Test
	public void testAddGermlineDBData() throws Exception {
		String chr = "chr1";
		int position = 10234;
		
		ChrPosition cp = new ChrPosition(chr, position);
		QSnpRecord snp = new QSnpRecord();
		snp.setChromosome(chr);
		snp.setPosition(position);
		snp.setClassification(Classification.SOMATIC);
		snp.setTumourGenotype(GenotypeEnum.CT);
		snp.setMutation("C>T");
		
		TestPipeline tp = new TestPipeline();
		tp.ensembleToQCMG.put("1", "chr1");
		tp.positionRecordMap.put(cp, snp);
		tp.patientId = "APGI_1234";
		// create germlineDB vcf file
		File vcfFile = testFolder.newFile();
		createVcfFile(vcfFile, "##INFO=<ID=1,Number=0,Type=Flag,Description=\"/mnt/seq_results/icgc_pancreatic/APGI_1234/variants/qSNP/uuid/APGI_1234.GermlineSNV.dcc1\">\n" + 
				"1	10234	.	N	T	.	.	1");
		
		tp.addGermlineDBData(vcfFile.getAbsolutePath());
		assertEquals(null, tp.positionRecordMap.get(cp).getAnnotation());
		
		
		// now set patient to be someone else - should get GERM annotation
		tp.patientId = "APGI_5678";
		snp.setAnnotation(null);
		tp.addGermlineDBData(vcfFile.getAbsolutePath());
		assertEquals("GERM", tp.positionRecordMap.get(cp).getAnnotation());
		
		// now add in entry in germline db for the new patient - should keep GERM
		snp.setAnnotation(null);
		vcfFile = testFolder.newFile();
		createVcfFile(vcfFile, "##INFO=<ID=1,Number=0,Type=Flag,Description=\"/mnt/seq_results/icgc_pancreatic/APGI_1234/variants/qSNP/uuid/APGI_1234.GermlineSNV.dcc1\">\n" + 
				"##INFO=<ID=2,Number=0,Type=Flag,Description=\"/mnt/seq_results/icgc_pancreatic/APGI_5678/variants/qSNP/uuid2/APGI_5678.GermlineSNV.dcc1\">\n" + 
				"1	10234	.	N	T	.	.	1");
		
		tp.addGermlineDBData(vcfFile.getAbsolutePath());
		assertEquals("GERM", tp.positionRecordMap.get(cp).getAnnotation());
		
		// now set both germline entries to be same patient - back to null
		vcfFile = testFolder.newFile();
		createVcfFile(vcfFile, "##INFO=<ID=1,Number=0,Type=Flag,Description=\"/mnt/seq_results/icgc_pancreatic/APGI_5678/variants/qSNP/uuid/APGI_5678.GermlineSNV.dcc1\">\n" + 
				"##INFO=<ID=2,Number=0,Type=Flag,Description=\"/mnt/seq_results/icgc_pancreatic/APGI_5678/variants/qSNP/uuid2/APGI_5678.GermlineSNV.dcc1\">\n" + 
				"1	10234	.	N	T	.	.	1");
		
		snp.setAnnotation(null);
		tp.addGermlineDBData(vcfFile.getAbsolutePath());
		assertEquals(null, tp.positionRecordMap.get(cp).getAnnotation());
		
	}
	
	private void createVcfFile(File vcfFile, String data) throws IOException {
		FileWriter writer = new FileWriter(vcfFile);
		try {
			// add data
			writer.write(data);
		} finally {
			writer.close();
		}
	}

}
