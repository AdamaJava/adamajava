package org.qcmg.snp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;

public class PipelineTest {
	
	private static final String C = ":";
	private static final String SC = ";";
	private static final String T = "\t";
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testUpdateAnnotation() {
		QSnpRecord rec = new QSnpRecord("chr1", 100, null);
		assertEquals(null, rec.getAnnotation());
		VcfUtils.updateFilter(rec.getVcfRecord(), null);
		assertEquals(null, rec.getAnnotation());
		VcfUtils.updateFilter(rec.getVcfRecord(), "");
		assertEquals("", rec.getAnnotation());
		VcfUtils.updateFilter(rec.getVcfRecord(), "MIN");
		assertEquals("MIN", rec.getAnnotation());
		VcfUtils.updateFilter(rec.getVcfRecord(), "COV");
		assertEquals("MIN;COV", rec.getAnnotation());
		
		// reset
		rec = new QSnpRecord("chr1", 100, null);
		VcfUtils.updateFilter(rec.getVcfRecord(), "PASS");
		assertEquals("PASS", rec.getAnnotation());
		VcfUtils.updateFilter(rec.getVcfRecord(), "MIN");
		assertEquals("MIN", rec.getAnnotation());
		VcfUtils.updateFilter(rec.getVcfRecord(), "PASS");
		assertEquals("PASS", rec.getAnnotation());
	}
	
	@Test
	public void compoundSnp() {
		Pipeline pipeline = new TestPipeline();
		QSnpRecord snp = new QSnpRecord("chr1", 100, "A");
//		snp.setDbSnpId("rs12345");
		snp.setMutation("A>C");
		snp.setClassification(Classification.SOMATIC);
		assertEquals(null, snp.getAnnotation());
		
		QSnpRecord snp2 = new QSnpRecord("chr1", 101, "C");
		snp2.setMutation("C>G");
		snp2.setClassification(Classification.SOMATIC);
		assertEquals(null, snp.getAnnotation());
		
		pipeline.positionRecordMap.put(new ChrPosition("chr1", 100), snp);
		pipeline.positionRecordMap.put(new ChrPosition("chr1", 101), snp2);
		
		Accumulator tumour100 = new Accumulator(100);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		Accumulator tumour101 = new Accumulator(101);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 1);
		
		pipeline.accumulators.put(new ChrPosition("chr1", 100), new Pair<Accumulator, Accumulator>(null, tumour100));
		pipeline.accumulators.put(new ChrPosition("chr1", 101), new Pair<Accumulator, Accumulator>(null, tumour101));
		pipeline.compoundSnps();
		
		assertEquals(0, pipeline.compoundSnps.size());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		pipeline.compoundSnps();
		
		assertEquals(1, pipeline.compoundSnps.size());
	}
	
	@Test
	public void compoundSnpWithNormalSnps() {
		Pipeline pipeline = new TestPipeline();
		QSnpRecord snp98 = new QSnpRecord("chr1", 98, "A");
//		snp98.setDbSnpId("rs12345678");
		snp98.setMutation("A>C");
		snp98.setClassification(Classification.SOMATIC);
		
		QSnpRecord snp100 = new QSnpRecord("chr1", 100, "A");
//		snp100.setDbSnpId("rs12345");
		snp100.setMutation("A>C");
		snp100.setClassification(Classification.SOMATIC);
		
		QSnpRecord snp101 = new QSnpRecord("chr1", 101, "C");
		snp101.setMutation("C>G");
		snp101.setClassification(Classification.SOMATIC);
		
		pipeline.positionRecordMap.put(snp98.getChrPos(), snp98);
		pipeline.positionRecordMap.put(snp100.getChrPos(), snp100);
		pipeline.positionRecordMap.put(snp101.getChrPos(), snp101);
		
		Accumulator tumour98 = new Accumulator(98);
		tumour98.addBase((byte)'C', (byte)30, true, 98, 98, 200, 1);
		tumour98.addBase((byte)'C', (byte)30, true, 98, 98, 200, 2);
		tumour98.addBase((byte)'C', (byte)30, true, 98, 98, 200, 4);
		Accumulator tumour100 = new Accumulator(100);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		tumour100.addBase((byte)'A', (byte)30, true, 100, 100, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		Accumulator tumour101 = new Accumulator(101);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 1);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 3);
		
		pipeline.accumulators.put(new ChrPosition("chr1", 98), new Pair<Accumulator, Accumulator>(null, tumour98));
		pipeline.accumulators.put(new ChrPosition("chr1", 100), new Pair<Accumulator, Accumulator>(null, tumour100));
		pipeline.accumulators.put(new ChrPosition("chr1", 101), new Pair<Accumulator, Accumulator>(null, tumour101));
		pipeline.compoundSnps();
		
		assertEquals(0, pipeline.compoundSnps.size());
		
		// need 4 records supporting the cs
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 5);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 6);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 5);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 6);
		pipeline.compoundSnps();
		assertEquals(0, pipeline.compoundSnps.size());
		
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 7);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 7);
		pipeline.compoundSnps();
		assertEquals(1, pipeline.compoundSnps.size());
	}
	
	@Test
	public void testCheckForEndsOfReadSomatic() {
		Pipeline pipeline = new TestPipeline();
		QSnpRecord snp = new QSnpRecord("chr1", 100, "A");
//		snp.setDbSnpId("rs12345");
		snp.setMutation("A>C");
		snp.setClassification(Classification.SOMATIC);
		assertEquals(null, snp.getAnnotation());
		pipeline.checkForEndsOfReads(snp, null, null, '\u0000');
		assertEquals(null, snp.getAnnotation());
		
		
		Accumulator tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef().charAt(0));
		assertEquals(SnpUtils.END_OF_READ + 1, snp.getAnnotation());
		
		// add another read - this time at the end
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104, 3);
		snp.getVcfRecord().setFilter(null);		// reset annotation
		
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef().charAt(0));
		assertEquals(SnpUtils.END_OF_READ + 2, snp.getAnnotation());
		
		// add some reads where the POI will be in the middle
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		snp.getVcfRecord().setFilter(null);		// reset annotation
		
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef().charAt(0));
		assertEquals(SnpUtils.END_OF_READ + 2, snp.getAnnotation());
		
		// another 4 of these and we will have the 5 good reads - but all on the same strand
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		snp.getVcfRecord().setFilter(null);		// reset annotation
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef().charAt(0));
		assertEquals(SnpUtils.END_OF_READ + 2, snp.getAnnotation());
		
		// add another read - this time on the reverse strand - but within 5bp of read end
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, false, 50, 100, 104, 1);
		snp.getVcfRecord().setFilter(null);		// reset annotation
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef().charAt(0));
		assertEquals(SnpUtils.END_OF_READ + 3, snp.getAnnotation());
		
		// add another read - again on the reverse strand - but outwith 5bp of read end
		tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 104, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, true, 50, 100, 105, 1);
		tumour.addBase((byte)'C', (byte)30, false, 50, 100, 104, 1);
		tumour.addBase((byte)'C', (byte)30, false, 50, 100, 105, 1);
		snp.getVcfRecord().setFilter(null);		// reset annotation
		pipeline.checkForEndsOfReads(snp, null, tumour, snp.getRef().charAt(0));
		assertEquals(null, snp.getAnnotation());
	}
	
	@Test
	public void testCheckForStrandBiasGermline() {
		Pipeline pipeline = new TestPipeline();
		QSnpRecord snp = new QSnpRecord("chr1", 100, "A");
//		snp.setDbSnpId("rs12345");
		snp.setMutation("A>C");
		snp.setClassification(Classification.GERMLINE);
		
		assertEquals(null, snp.getAnnotation());
		pipeline.checkForStrandBias(snp, null, null, '\u0000');
		assertEquals(null, snp.getAnnotation());
		Accumulator normal = new Accumulator(100);
		normal.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		pipeline.checkForStrandBias(snp, normal, null, 'A');
		assertEquals(SnpUtils.STRAND_BIAS, snp.getAnnotation());
		// reset
		snp.getVcfRecord().setFilter(null);
		
		// now add another base to the normal acc - on the other strand - should no longer get the annotation
		normal.addBase((byte)'C', (byte)30, false, 100, 100, 200, 1);
		pipeline.checkForStrandBias(snp, normal, null, 'A');
		assertEquals(null, snp.getAnnotation());
		
		// setup a tumour acc 
		Accumulator tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
		
		// switch to germline and try again
		snp.setClassification(Classification.SOMATIC);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(SnpUtils.STRAND_BIAS, snp.getAnnotation());
		
		// reset
		snp.getVcfRecord().setFilter(null);
		tumour.addBase((byte)'C', (byte)30, false, 100, 100, 200, 1);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
	}
	
	@Test
	public void testCheckForStrandBiasSomatic() {
		Pipeline pipeline = new TestPipeline();
		QSnpRecord snp = new QSnpRecord("chr1", 100, "A");
//		snp.setDbSnpId("rs12345");
		snp.setMutation("A>C");
		snp.setClassification(Classification.SOMATIC);
		
		assertEquals(null, snp.getAnnotation());
		pipeline.checkForStrandBias(snp, null, null, '\u0000');
		assertEquals(null, snp.getAnnotation());
		Accumulator tumour = new Accumulator(100);
		tumour.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		pipeline.checkForStrandBias(snp, null, tumour, 'A');
		assertEquals(SnpUtils.STRAND_BIAS, snp.getAnnotation());
		// reset
		snp.getVcfRecord().setFilter(null);
		
		// now add another base to the tumour acc - on the other strand - should no longer get the annotation
		tumour.addBase((byte)'C', (byte)30, false, 100, 100, 200, 1);
		pipeline.checkForStrandBias(snp, null, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
		
		// setup a normal acc 
		Accumulator normal = new Accumulator(100);
		normal.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		normal.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
		
		// switch to germline and try again
		snp.setClassification(Classification.GERMLINE);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(SnpUtils.STRAND_BIAS, snp.getAnnotation());
		
		// reset
		snp.getVcfRecord().setFilter(null);
		normal.addBase((byte)'C', (byte)30, false, 100, 100, 200, 1);
		pipeline.checkForStrandBias(snp, normal, tumour, 'A');
		assertEquals(null, snp.getAnnotation());
	}
	
	@Test
	public void testConvertQSnpToVCFSomatic() throws SnpException, IOException, Exception {
		// create qsnp record
		QSnpRecord snp = new QSnpRecord("chr1", 100, "A");
//		snp.setDbSnpId("rs12345");
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
		vcf.setFormatField(null);
		
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(Classification.SOMATIC.toString(), vcf.getInfo());

		// add in tumour nucleotides
		String tumourNucleotides = "C:15[18.95],19[19.35],A:2[27.02],3[29.03]"; 
		snp.setTumourNucleotides(tumourNucleotides);
		vcf.setFormatField(null);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(Classification.SOMATIC + SC + VcfUtils.INFO_MUTANT_READS + "=34" , vcf.getInfo());
		
		// add in Novel starts
		snp.setTumourNovelStartCount(5);
		vcf.setFormatField(null);
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
		vcf.setFormatField(null);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT,
				vcf.getFormatFields().get(0));
		assertEquals("0/0:A/A:" + normalNucleotides.replace(":", ""), vcf.getFormatFields().get(1));
		assertEquals("0/1:A/C:" + tumourNucleotides.replace(":", ""), vcf.getFormatFields().get(2));
		
//		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT + T
//				+ "0/0:A/A:" + normalNucleotides.replace(":", "") + T + "0/1:A/C:" + tumourNucleotides.replace(":", "")
//				, vcf.getFormatFields().get(0));
		
		System.out.println(vcf.toString());
	}
	@Test
	public void testConvertQSnpToVCFGermline() throws SnpException, IOException, Exception {
		// create qsnp record
		QSnpRecord snp = new QSnpRecord("chr1", 100, "G");
//		snp.setDbSnpId("rs12345");
		snp.setMutation("G>A");
		snp.setNormalGenotype(GenotypeEnum.AG);
		snp.setTumourGenotype(GenotypeEnum.AG);
		
		Pipeline pipeline = new TestPipeline();
		VCFRecord vcf = pipeline.convertQSnpToVCF(snp);
		
		assertEquals(snp.getChromosome(), vcf.getChromosome());
		assertEquals(snp.getPosition(), vcf.getPosition());
		assertEquals(snp.getRef() + "", vcf.getRef());
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
		
		vcf.setFormatField(null);	// reset format fields
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(VcfUtils.INFO_MUTANT_READS + "=15;" 
				+ VcfUtils.INFO_NOVEL_STARTS + "=7" , vcf.getInfo());
		
		// format field
//		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT + T
//				+ ".:.:" + normalNucleotides.replace(":", "") + T + ".:.:" + tumourNucleotides.replace(":", "") , vcf.getExtraFields().get(0));
		
//		snp.setNormalGenotype(GenotypeEnum.AG);
//		snp.setTumourGenotype(GenotypeEnum.AG);
		vcf = pipeline.convertQSnpToVCF(snp);
//		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT + T
//				+ "0/1:A/G:" + normalNucleotides.replace(":", "") + T + "0/1:A/G:" + tumourNucleotides.replace(":", "")
//				, vcf.getFormatFields().get(0));
		
		assertEquals(VcfUtils.FORMAT_GENOTYPE + C + VcfUtils.FORMAT_GENOTYPE_DETAILS + C + VcfUtils.FORMAT_ALLELE_COUNT,
				vcf.getFormatFields().get(0));
		assertEquals("0/1:A/G:" + normalNucleotides.replace(":", ""), vcf.getFormatFields().get(1));
		assertEquals("0/1:A/G:" + tumourNucleotides.replace(":", ""), vcf.getFormatFields().get(2));
		
		System.out.println(vcf.toString());
	}
	@Test
	public void testConvertQSnpToVCFFilter() throws SnpException, IOException, Exception {
		// create qsnp record
		QSnpRecord snp = new QSnpRecord("chr1", 100, "G");
//		snp.setDbSnpId("rs12345");
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
		snp.getVcfRecord().setFilter(null);
		snp.setTumourNovelStartCount(4);
		pipeline.classifyPileupRecord(snp);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(SnpUtils.LESS_THAN_12_READS_NORMAL + SC + SnpUtils.MUTANT_READS , vcf.getFilter());
		
		snp.getVcfRecord().setFilter(null);
		tumourNucleotides = "A:1[26.65],4[28.2],G:1[20],8[24.33]"; 
		snp.setTumourNucleotides(tumourNucleotides);
		snp.setNormalCount(12);
		pipeline.classifyPileupRecord(snp);
		vcf = pipeline.convertQSnpToVCF(snp);
		assertEquals(SnpUtils.PASS , vcf.getFilter());
		
		System.out.println(vcf.toString());
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
