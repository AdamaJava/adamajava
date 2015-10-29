package org.qcmg.qpileup.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.model.PileupDataRecord;
import org.qcmg.pileup.model.PileupSAMRecord;
import org.qcmg.pileup.util.TestUtil;

public class PileupSAMRecordTest {

	
	private File testFile;
	private ArrayList<SAMRecord> samRecords;	
	private static String CIGAR_H = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t1\t0\t0";
	private static String CIGAR_HStart = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0";
	private static String CIGAR_S = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t1\t0\t0\t0\t0";
	private static String CIGAR_N = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0";
	private static String CIGAR_I = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t0\t0\t0";
	private static String CIGAR_D = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t0\t0\t0\t0\t0\t0\t0";
	private static String CIGAR_DStart = "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t1\t1\t0\t0\t0\t0\t0\t0";


	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws IOException {
		testFile = testFolder.newFile("test.bam");
		TestUtil.createBam(testFile.getAbsolutePath());
		
		samRecords = new ArrayList<SAMRecord>();
		
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(testFile); //new SAMFileReader(testFile);
		
		for (SAMRecord r: reader) {
			samRecords.add(r);
		}
		reader.close();
	}
	
	@After
	public void tearDown() {
		samRecords = null;
	}
	
	@Test
	public void testPileupWithCigarSandH() throws QPileupException {
		SAMRecord record = samRecords.get(0);
		record.setFlags(1033);
		PileupSAMRecord r = new PileupSAMRecord(record);
		r.pileup();
		
		assertEquals(getLength(record), r.getPileupDataRecords().size());
		
		//read record
		PileupDataRecord dataRecord = r.getPileupDataRecords().get(0);
		assertEquals(record.getUnclippedStart(), dataRecord.getPosition().intValue());
		assertEquals(record.getAlignmentStart(), dataRecord.getPosition().intValue());
		assertEquals(record.getMappingQuality(), dataRecord.getMapQual());
		assertEquals(1, dataRecord.getBaseG());
		assertEquals(record.getBaseQualities()[0], dataRecord.getgQual());
		assertEquals(1, dataRecord.getMateUnmapped());
		assertEquals(1, dataRecord.getStartAll());
		assertEquals(0, dataRecord.getStartNondup());
		assertEquals(1, dataRecord.getDupCount());
		assertEquals(0, dataRecord.getStopAll());
		
		PileupDataRecord endRecord = r.getPileupDataRecords().get(120);
		assertEquals(record.getAlignmentEnd(), endRecord.getPosition().intValue());
		assertEquals(0, endRecord.getStartAll());
		assertEquals(1, endRecord.getStopAll());

		//cigar S and H
		assertEquals(CIGAR_S, r.getPileupDataRecords().get(121).toString());
		assertEquals(CIGAR_H, r.getPileupDataRecords().get(122).toString());
		assertEquals(CIGAR_HStart, r.getPileupDataRecords().get(146).toString());
		assertEquals(record.getUnclippedEnd(), r.getPileupDataRecords().get(146).getPosition().intValue());
	}
	
	@Test
	public void testPileupWithCigarN() throws QPileupException {
		SAMRecord record = samRecords.get(1);
		record.setFlags(1033);
		PileupSAMRecord r = new PileupSAMRecord(record);
		
		r.pileup();
		assertEquals(getLength(record), r.getPileupDataRecords().size());
		//read record
		PileupDataRecord dataRecord = r.getPileupDataRecords().get(0);
		assertEquals(record.getUnclippedStart(), dataRecord.getPosition().intValue());
		assertEquals(record.getAlignmentStart(), dataRecord.getPosition().intValue());
		assertEquals(record.getMappingQuality(), dataRecord.getMapQual());
		assertEquals(1, dataRecord.getBaseT());
		assertEquals(1, dataRecord.getMateUnmapped());
		assertEquals(record.getBaseQualities()[0], dataRecord.gettQual());
		assertEquals(1, dataRecord.getStartAll());
		assertEquals(0, dataRecord.getStartNondup());
		assertEquals(1, dataRecord.getDupCount());
		assertEquals(0, dataRecord.getStopAll());
		
		PileupDataRecord endRecord = r.getPileupDataRecords().get(getLength(record) -1);
		assertEquals(record.getAlignmentEnd(), endRecord.getPosition().intValue());
		assertEquals(record.getUnclippedEnd(), endRecord.getPosition().intValue());
		assertEquals(0, endRecord.getStartAll());
		assertEquals(1, endRecord.getStopAll());
		
		//cigar S and H
		assertEquals(CIGAR_N, r.getPileupDataRecords().get(12).toString());

	}
	
	@Test
	public void testPileupWithCigarI() throws QPileupException {
		SAMRecord record = samRecords.get(2);
		record.setFlags(9);
		PileupSAMRecord r = new PileupSAMRecord(record);
		r.pileup();

		assertEquals(142, r.getPileupDataRecords().size());
		
		//read record
		PileupDataRecord dataRecord = r.getPileupDataRecords().get(0);
		assertEquals(record.getUnclippedStart(), dataRecord.getPosition().intValue());
		assertEquals(record.getAlignmentStart(), dataRecord.getPosition().intValue());
		assertEquals(record.getMappingQuality(), dataRecord.getMapQual());
		assertEquals(1, dataRecord.getBaseT());
		assertEquals(1, dataRecord.getMateUnmapped());
		assertEquals(record.getBaseQualities()[0], dataRecord.gettQual());
		assertEquals(1, dataRecord.getStartAll());
		assertEquals(1, dataRecord.getStartNondup());
		assertEquals(0, dataRecord.getDupCount());
		assertEquals(0, dataRecord.getStopAll());
		
		//cigar I - cigar of 2, but only one read marked
		assertEquals(CIGAR_I, r.getPileupDataRecords().get(1).toString());	
		assertFalse(CIGAR_I.equals(r.getPileupDataRecords().get(2).toString()));	
	}
	
	@Test
	public void testPileupWithCigarD() throws QPileupException {
		SAMRecord record = samRecords.get(3);
		record.setFlags(9);
		PileupSAMRecord r = new PileupSAMRecord(record);
		r.pileup();
		
		assertEquals(getLength(record), r.getPileupDataRecords().size());
		
		//read record
		PileupDataRecord dataRecord = r.getPileupDataRecords().get(0);
		assertEquals(record.getUnclippedStart(), dataRecord.getPosition().intValue());
		assertEquals(record.getAlignmentStart(), dataRecord.getPosition().intValue());
		assertEquals(record.getMappingQuality(), dataRecord.getMapQual());
		assertEquals(1, dataRecord.getBaseT());
		assertEquals(1, dataRecord.getMateUnmapped());
		assertEquals(record.getBaseQualities()[0], dataRecord.gettQual());
		assertEquals(1, dataRecord.getStartAll());
		assertEquals(1, dataRecord.getStartNondup());
		assertEquals(0, dataRecord.getDupCount());
		assertEquals(0, dataRecord.getStopAll());
		
		assertEquals(CIGAR_DStart, r.getPileupDataRecords().get(74).toString());	
		assertEquals(CIGAR_D, r.getPileupDataRecords().get(75).toString());	
	}
	
	
//	@Test(expected=QPileupException.class)
//	public void testPileupWithIllegalMD() throws QPileupException {
//		SAMRecord record = samRecords.get(0);
//		record.setAttribute("MD", "s");
//		PileupSAMRecord r = new PileupSAMRecord(record);
//		r.pileup();
//	}
	
	@Test
	public void testSetCigarI() {
		PileupSAMRecord r = new PileupSAMRecord(samRecords.get(0));
		r.setCigarI(1);
		
		assertEquals(1, r.getPileupDataRecords().size());
		assertEquals(1, r.getPileupDataRecords().get(0).getCigarI());
	}
	
	@Test
	public void testSetCigarRecordsWithCigarH() {
		SAMRecord record = samRecords.get(0);
		CigarElement element = record.getCigar().getCigarElement(2);
		assertEquals("H", element.getOperator().name());
		PileupSAMRecord r = new PileupSAMRecord(record);
		
		r.setCigarRecords(1, 3, element.getOperator(), 0, 0);
		assertEquals(3, r.getPileupDataRecords().size());
		assertEquals(1, r.getPileupDataRecords().get(0).getCigarH());	
	}
	
	@Test
	public void testSetCigarRecordsWithCigarS() {
		SAMRecord record = samRecords.get(0);
		CigarElement element = record.getCigar().getCigarElement(1);
		assertEquals("S", element.getOperator().name());
		PileupSAMRecord r = new PileupSAMRecord(record);
		
		r.setCigarRecords(1, 3, element.getOperator(), 0,0);
		assertEquals(3, r.getPileupDataRecords().size());
		assertEquals(1, r.getPileupDataRecords().get(0).getCigarS());	
	}
	
	@Test
	public void testSetCigarRecordsWithCigarN() {
		SAMRecord record = samRecords.get(1);
		CigarElement element = record.getCigar().getCigarElement(1);
		assertEquals("N", element.getOperator().name());
		PileupSAMRecord r = new PileupSAMRecord(record);
		
		r.setCigarRecords(1, 3, element.getOperator(), 0,0);
		assertEquals(3, r.getPileupDataRecords().size());
		assertEquals(1, r.getPileupDataRecords().get(0).getCigarN());	
	}
	
	@Test
	public void testAddReadDataRecord() throws QPileupException {
		SAMRecord record = samRecords.get(0);
		//readstart, is duplicate, mate unmapped
		record.setFlags(1033);		
		PileupSAMRecord r = new PileupSAMRecord(record);
		r.addReadDataRecord(11190680, 'A', 'C', 20);
		assertReadDataRecord(r, 11190680, 84, 1, 1, 1, 0, 1);
		
		//readstart, nonduplicate, mate unmapped
		record.setFlags(25);		
		r = new PileupSAMRecord(record);
		r.addReadDataRecord(11190680, 'A', 'C', 20);
		assertReadDataRecord(r, 11190680, 84, 0, 1, 1, 0, 1);
		
		record.setFlags(17);		
		r = new PileupSAMRecord(record);
		r.addReadDataRecord(11190800, 'A', 'C', 20);
		assertReadDataRecord(r, 11190800, 84, 0, 0, 0, 1, 0);		
	}

	private void assertReadDataRecord(PileupSAMRecord r, int pos, int mapQual, int dupCount, int readStart, int readNonDup, int readEnd, int mateUnmapped) {
		assertEquals(1, r.getPileupDataRecords().size());
		PileupDataRecord dataRecord = r.getPileupDataRecords().get(0);
		assertEquals(new Integer(pos), dataRecord.getPosition());
		assertEquals(mapQual, dataRecord.getMapQual());
		assertEquals(dupCount, dataRecord.getDupCount());
		assertEquals(readNonDup, readStart, dataRecord.getStartAll());
		assertEquals(readEnd, dataRecord.getStopAll());	
		assertEquals(mateUnmapped, dataRecord.getMateUnmapped());
	}
	
	private int getLength(SAMRecord record) {
		return record.getUnclippedEnd() - record.getUnclippedStart() + 1;
	}
}
