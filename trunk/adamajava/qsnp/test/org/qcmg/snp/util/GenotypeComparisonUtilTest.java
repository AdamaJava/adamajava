package org.qcmg.snp.util;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;

public class GenotypeComparisonUtilTest {

	@Test
	public void testCompareGenotypesSame() {
		QSnpRecord record = new QSnpRecord("1",36568, "G");
		record.setNormalGenotype(GenotypeEnum.AA);
		record.setTumourGenotype(GenotypeEnum.AA);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("G>A", record.getMutation());
		
		// reset annotation
		record.getVcfRecord().setFilter(null);
		// set the normal count - still less than 8 so will annotate
		record.setNormalCount(6);
		record.setNormalGenotype(GenotypeEnum.CC);
		record.setTumourGenotype(GenotypeEnum.CC);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("G>C", record.getMutation());
		
		// reset annotation
		record.getVcfRecord().setFilter(null);
		// set the normal count to 8, will now complain about the tumour count
		record.setNormalCount(8);
		record.setNormalGenotype(GenotypeEnum.GT);
		record.setTumourGenotype(GenotypeEnum.GT);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("G>T", record.getMutation());
		
		// reset annotation
		record.getVcfRecord().setFilter(null);
		// set the tumour count - still less than 8 so will annotate
		record.setTumourCount(6);
		record.setNormalGenotype(GenotypeEnum.TT);
		record.setTumourGenotype(GenotypeEnum.TT);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("G>T", record.getMutation());
		
		// reset annotation
		record.getVcfRecord().setFilter(null);
		// set the tumour count to 8, should now be class A
		record.setTumourCount(8);
		record.setNormalGenotype(GenotypeEnum.AC);
		record.setTumourGenotype(GenotypeEnum.AC);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertEquals("G>A/C", record.getMutation());
		Assert.assertNull(record.getAnnotation());
	}
	
	@Test
	public void testCompareGenotypesNotSameButBothHomozygous() {
		QSnpRecord record = new QSnpRecord("1", 1, "A");
		record.setNormalGenotype(GenotypeEnum.AA);
		record.setTumourGenotype(GenotypeEnum.GG);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("A>G", record.getMutation());
		
		// reset annotation
		record = new QSnpRecord("1", 1, "T");
		// set the ref to be the tumour - should be annotated as such
		record.setNormalGenotype(GenotypeEnum.CC);
		record.setTumourGenotype(GenotypeEnum.TT);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("C>T", record.getMutation());
	}
	
	@Test
	public void testCompareGenotypesNotSameButBothHeterozygous() {
		QSnpRecord record = new QSnpRecord("1", 1, "A");
		record.setNormalGenotype(GenotypeEnum.AC);
		record.setTumourGenotype(GenotypeEnum.GT);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("A/C>G/T", record.getMutation());
		
		// reset annotation
		record = new QSnpRecord("1", 1, "G");
		record.setNormalGenotype(GenotypeEnum.CT);
		record.setTumourGenotype(GenotypeEnum.AG);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("C/T>A/G", record.getMutation());
		
		// reset annotation
		record.getVcfRecord().setFilter(null);
		record.setNormalGenotype(GenotypeEnum.CT);
		record.setTumourGenotype(GenotypeEnum.CG);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("C>G", record.getMutation());

		// reset annotation
		record.getVcfRecord().setFilter(null);
		record.setNormalGenotype(GenotypeEnum.CT);
		record.setTumourGenotype(GenotypeEnum.AC);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("T>A", record.getMutation());
	}
	
	
	@Test
	public void testCompareGenotypesHomHet() {
		QSnpRecord record = new QSnpRecord("1", 1, "G");
		record.setNormalGenotype(GenotypeEnum.CC);
		record.setTumourGenotype(GenotypeEnum.CG);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("G>C", record.getMutation());
		//TODO check this!!!
//		Assert.assertEquals("C>G", record.getMutation());
		
		// reset annotation
		record.getVcfRecord().setFilter(null);
		record.setNormalGenotype(GenotypeEnum.AA);
		record.setTumourGenotype(GenotypeEnum.CT);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("A/A>C/T", record.getMutation());
	}
	
	@Test
	public void testCompareGenotypesHetHom() {
		QSnpRecord record = new QSnpRecord("1", 1, "A");
		record.setNormalGenotype(GenotypeEnum.AG);
		record.setTumourGenotype(GenotypeEnum.TT);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("A>T", record.getMutation());
		
		// reset annotation
		record.getVcfRecord().setFilter(null);
		record.setMutation(null);
		record.setNormalGenotype(GenotypeEnum.AG);
		record.setTumourGenotype(GenotypeEnum.GG);
		
		GenotypeComparisonUtil.compareGenotypes(record);
		
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertNotNull(record.getAnnotation());
		Assert.assertEquals("A>G", record.getMutation());
//		Assert.assertNull(record.getMutation());
	}
	
	@Test
	public void testWikiPageDetails() {
		QSnpRecord record = new QSnpRecord("1", 1, "A");
		
		record.setNormalGenotype(GenotypeEnum.GG);
		record.setTumourGenotype(GenotypeEnum.AG);
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		
		record.setNormalGenotype(GenotypeEnum.GG);
		record.setTumourGenotype(GenotypeEnum.GT);
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		
		record.setNormalGenotype(GenotypeEnum.AG);
		record.setTumourGenotype(GenotypeEnum.GG);
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		
		record.setNormalGenotype(GenotypeEnum.AG);
		record.setTumourGenotype(GenotypeEnum.TT);
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		
		record.setNormalGenotype(GenotypeEnum.GG);
		record.setTumourGenotype(GenotypeEnum.GG);
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		
		record.setNormalGenotype(GenotypeEnum.GG);
		record.setTumourGenotype(GenotypeEnum.TT);
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		
		record.setNormalGenotype(GenotypeEnum.AG);
		record.setTumourGenotype(GenotypeEnum.AG);
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		
		record.setNormalGenotype(GenotypeEnum.AG);
		record.setTumourGenotype(GenotypeEnum.AT);
		GenotypeComparisonUtil.compareGenotypes(record);
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		
	}
	
	@Test
	public void testCompareGenotypesNullRecord() {
		try {
			GenotypeComparisonUtil.compareGenotypes(null);
			Assert.fail("Should have thrown an illegal args exception");
		} catch(IllegalArgumentException iae) {}
	}
	
	@Test
	public void testCompareGenotypesEmptyRecord() {
		QSnpRecord record = new QSnpRecord("1", 1, null);
		try {
			GenotypeComparisonUtil.compareGenotypes(record);
			Assert.fail("Should have thrown an illegal args exception");
		} catch(IllegalArgumentException iae) {}
	}

	@Test
	public void testCompareSingleGenotypeSameAsRef() {
		QSnpRecord record = new QSnpRecord("1", 1, "A");
		record.setTumourGenotype(GenotypeEnum.AA);
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertNull(record.getClassification());
		
		record = new QSnpRecord("1", 1, "A");
		record.setNormalGenotype(GenotypeEnum.AA);
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertNull(record.getClassification());
	}
	
	@Test
	public void testCompareSingleGenotypeLowNormalCoverage() {
		QSnpRecord record = new QSnpRecord("1", 1, "A");
		record.setTumourGenotype(GenotypeEnum.AG);
		record.setNormalCount(2);
		record.setNormalPileup("Ag");
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertEquals("A>G", record.getMutation());
		
		record = new QSnpRecord("1", 1, "A");
		record.setTumourGenotype(GenotypeEnum.AG);
		record.setNormalCount(3);
		record.setNormalPileup("AGG");
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertEquals("A>G", record.getMutation());
		
		record = new QSnpRecord("1", 1, "A");
		record.setTumourGenotype(GenotypeEnum.AG);
		record.setNormalCount(2);
		record.setNormalPileup("AA");	// no evidence
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertEquals("A>G", record.getMutation());
		
		record = new QSnpRecord("1", 1, "A");
		record.setTumourGenotype(GenotypeEnum.GT);
		record.setNormalCount(3);
		record.setNormalPileup("CAc");	// no evidence
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertEquals(Classification.SOMATIC, record.getClassification());
		Assert.assertEquals("A>G/T", record.getMutation());
		
		record = new QSnpRecord("1", 1, "A");
		record.setTumourGenotype(GenotypeEnum.TT);
		record.setNormalCount(4);
		record.setNormalPileup("AtGg");	// no evidence
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertEquals("A>T", record.getMutation());
		
	}
	
	@Test
	public void testCompareSingleGenotypeLowTumourCoverage() {
		QSnpRecord record = new QSnpRecord("1", 1, "A");
		record.setNormalGenotype(GenotypeEnum.AG);
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertEquals("A>G", record.getMutation());
		
		record = new QSnpRecord("1", 1, "A");
		record.setNormalGenotype(GenotypeEnum.CC);
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertEquals("A>C", record.getMutation());
		
		record = new QSnpRecord("1", 1, "C");
		record.setNormalGenotype(GenotypeEnum.CC);
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertNull(record.getClassification());
		Assert.assertNull(record.getMutation());
	}
	
	
	@Test
	public void testCompareSingleGenotypeNullRecord() {
		try {
			GenotypeComparisonUtil.compareSingleGenotype(null);
			Assert.fail("Should have thrown an illegal args exception");
		} catch(IllegalArgumentException iae) {}
	}
	
	@Test
	public void testCompareSingleGenotypeEmptyRecord() {
		QSnpRecord record = new QSnpRecord("1", 1, null);
		try {
			GenotypeComparisonUtil.compareSingleGenotype(record);
			Assert.fail("Should have thrown an illegal args exception");
		} catch(IllegalArgumentException iae) {}
	}

	@Test
	public void testRealLifeData() {
//		14:38:26.931 [main] INFO org.qcmg.snp.util.GenotypeComparisonUtil - het tumour genotype, with ref not in alleles: chr12 58638253        G       3       AaT     AII     6       TtAatA  IIHIII          A/T     null            less than 3 reads of same allele in normal              --      -888    -888    2                       2       -888
		// should this be GERMLINE???
		QSnpRecord record = new QSnpRecord("chr12", 58638253, "G");
		record.setTumourGenotype(GenotypeEnum.AT);
		record.setNormalCount(3);
		record.setNormalPileup("AaT");
		
		GenotypeComparisonUtil.compareSingleGenotype(record);
		Assert.assertEquals(Classification.GERMLINE, record.getClassification());
		Assert.assertEquals(SnpUtils.LESS_THAN_3_READS_NORMAL, record.getAnnotation());
		
		
//		14:38:26.939 [main] INFO org.qcmg.snp.util.GenotypeComparisonUtil - het tumour genotype, with ref not in alleles: chr14 94914340        G       4       TCCT    IIII    17      TCCCCTCTCCTCCCC^UC^UC   3EDIIII;DIIIIIIII               C/T     null            less than 3 reads of same allele in normal              --      -888    -888    2                       2       -888
		// should this be GERMLINE???
		QSnpRecord record2 = new QSnpRecord("chr14", 94914340, "G");
		record2.setTumourGenotype(GenotypeEnum.CT);
		record2.setNormalCount(4);
		record2.setNormalPileup("TCCT");
		
		GenotypeComparisonUtil.compareSingleGenotype(record2);
		Assert.assertEquals(Classification.GERMLINE, record2.getClassification());
		Assert.assertEquals(SnpUtils.LESS_THAN_3_READS_NORMAL, record2.getAnnotation());
		
		QSnpRecord record3 = new QSnpRecord("chr4", 36568, "A");
		record3.setTumourGenotype(GenotypeEnum.GG);
		record3.setTumourCount(4);
		record3.setNormalCount(1);
		record3.setNormalPileup("G");
		GenotypeComparisonUtil.compareSingleGenotype(record3);
		Assert.assertEquals(Classification.GERMLINE, record3.getClassification());
		Assert.assertEquals(SnpUtils.LESS_THAN_3_READS_NORMAL, record3.getAnnotation());
	}

}
