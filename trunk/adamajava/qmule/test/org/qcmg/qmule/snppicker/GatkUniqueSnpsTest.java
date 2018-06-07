package org.qcmg.qmule.snppicker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import htsjdk.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.pileup.QSnpRecord;

public class GatkUniqueSnpsTest {
	
	
	private static List<SAMRecord> samRecords = new ArrayList<SAMRecord>();
	
	@Before
	public void setup() throws IOException {
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(100);
		record.setReferenceName("chr1");
		record.setReadBases(new byte[] {'A', 'C', 'G', 'T', 'A','A','A','A','A','A','A','A','A'});
		samRecords.add(record);
		
		for (int i = 1 ; i < 12 ; i++) {
			record = new SAMRecord(null);
			record.setAlignmentStart(100+i);
			record.setReferenceName("chr1");
			record.setReadBases(new byte[] {'A', 'A', 'A', 'A', 'A','A','A','A','A','A','A','A','A'});
			samRecords.add(record);
		}
	}
	
	@Test
	public void testFailingRead() throws Exception {
		SAMRecord record = new SAMRecord(null);
		record.setReferenceName("chr1");
		record.setAlignmentStart(168512433);
//		record.setAlignmentEnd(168512486);
		record.setCigarString("7M4D43M");
		record.setReadString("AGCTGGTATTGCACATGGTGTGGACCCCATCAAGCTGGTTAACTTTCTGN");
		List<SAMRecord> records = new ArrayList<SAMRecord>();
		records.add(record);
		
		QSnpRecord qpr = new QSnpRecord("chr1", 168512486, "G");
		qpr.setAlt("C");
		
		GatkUniqueSnps.examinePileup(records, qpr);
		
		Assert.assertNotNull(qpr.getAnnotation());
		Assert.assertFalse(qpr.getAnnotation().contains("mutation also found in pileup of normal"));
		Assert.assertTrue(qpr.getAnnotation().contains(SnpUtils.LESS_THAN_12_READS_NORMAL));
	}
	
	@Test
	public void testFailingRead2() throws Exception{
		SAMRecord record = new SAMRecord(null);
		record.setReferenceName("chr1");
		record.setAlignmentStart(55524198);
		record.setCigarString("1H49M");
		record.setReadString("TGGTCAGCACACTGGGGGCCTACACGGATGGCCACAGCCATCGCCCGCT");
		List<SAMRecord> records = new ArrayList<SAMRecord>();
		records.add(record);
		
		record = new SAMRecord(null);
		record.setReferenceName("chr1");
		record.setAlignmentStart(55524210);
		record.setCigarString("13H37M");
		record.setReadString("TCGGGGCCTACACGGATGGCCACAGCCATCGCCCGCT");
		records.add(record);
		
		record = new SAMRecord(null);
		record.setReferenceName("chr1");
		record.setAlignmentStart(55524212);
		record.setCigarString("10H40M");
		record.setReadString("GGGGCCTACACGGATGGCCACAGCCATCGCCCGCTGCGCC");
		records.add(record);
		
		record = new SAMRecord(null);
		record.setReferenceName("chr1");
		record.setAlignmentStart(55524218);
		record.setCigarString("2H48M");
		record.setReadString("TACACGGATGGCCACAGCCGTCGCCCGCTGCGCCCCAGATGAGGAGCT");
		records.add(record);
		
		record = new SAMRecord(null);
		record.setReferenceName("chr1");
		record.setAlignmentStart(55524228);
		record.setCigarString("4M6D21M");
		record.setReadString("GCCATCGCCCGCTGCGCCCCAGATG");
		records.add(record);
		
		QSnpRecord qpr = new QSnpRecord("chr1", 55524237, "G");
		qpr.setAlt("A");
		
		GatkUniqueSnps.examinePileup(records, qpr);
		
		Assert.assertNotNull(qpr.getAnnotation());
		Assert.assertTrue(qpr.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		Assert.assertTrue(qpr.getAnnotation().contains(SnpUtils.LESS_THAN_12_READS_NORMAL));
	}
	
	
	@Test
	public void testExaminePileup() throws Exception {
		QSnpRecord qpr = new QSnpRecord("chr1", 101, "G");
		qpr.setAlt("C");
		
		GatkUniqueSnps.examinePileup(samRecords.subList(0,1), qpr);
		
		Assert.assertNotNull(qpr.getAnnotation());
		Assert.assertTrue(qpr.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		Assert.assertTrue(qpr.getAnnotation().contains(SnpUtils.LESS_THAN_12_READS_NORMAL));
		
		qpr = new QSnpRecord("chr1", 102, "G");
		qpr.setAlt("C");
		
		GatkUniqueSnps.examinePileup(samRecords.subList(0, 1), qpr);
		
		Assert.assertNotNull(qpr.getAnnotation());
		Assert.assertFalse(qpr.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		Assert.assertTrue(qpr.getAnnotation().contains(SnpUtils.LESS_THAN_12_READS_NORMAL));
		
		qpr = new QSnpRecord("chr1", 110, "A");
		qpr.setAlt("G");
		
		GatkUniqueSnps.examinePileup(samRecords.subList(0, 10), qpr);
		
		Assert.assertNotNull(qpr.getAnnotation());
		Assert.assertFalse(qpr.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		Assert.assertTrue(qpr.getAnnotation().contains(SnpUtils.LESS_THAN_12_READS_NORMAL));
		
		qpr = new QSnpRecord("chr1", 112, "A");
		qpr.setAlt("G");
		
		GatkUniqueSnps.examinePileup(samRecords, qpr);
		Assert.assertNull(qpr.getAnnotation());
		
		qpr = new QSnpRecord("chr1", 112, "G");
		qpr.setAlt("A");
		
		GatkUniqueSnps.examinePileup(samRecords, qpr);
		Assert.assertNotNull(qpr.getAnnotation());
		Assert.assertTrue(qpr.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		Assert.assertFalse(qpr.getAnnotation().contains(SnpUtils.LESS_THAN_12_READS_NORMAL));
	}
	
	

}
