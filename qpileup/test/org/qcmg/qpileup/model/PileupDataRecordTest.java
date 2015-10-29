package org.qcmg.qpileup.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
import org.qcmg.pileup.model.StrandEnum;
import org.qcmg.pileup.util.TestUtil;

public class PileupDataRecordTest {
	
	
	private File testFile;
	private ArrayList<SAMRecord> samRecords;	
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws IOException {
		testFile = testFolder.newFile("test.bam");
		TestUtil.createBam(testFile.getAbsolutePath());
		
		samRecords = new ArrayList<SAMRecord>();
		
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(testFile);//new SAMFileReader(testFile);
		
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
	public void testCheckBase() throws QPileupException {
		PileupDataRecord d1 = new PileupDataRecord(123);
		d1.checkBase('A', 20, 'C', samRecords.get(0));
		assertEquals(1, d1.getNonReferenceNo());
		assertEquals(1, d1.getBaseA());
		assertEquals(20, d1.getaQual());		
		
		PileupDataRecord d2 = new PileupDataRecord(123);
		d2.checkBase('T', 20, 'T',  samRecords.get(0));
		assertEquals(0, d2.getNonReferenceNo());
		assertEquals(1, d2.getBaseT());
		assertEquals(20, d2.gettQual());
		
		PileupDataRecord d3 = new PileupDataRecord(123);
		d3.checkBase('C', 20, 'C',  samRecords.get(0));
		assertEquals(0, d3.getNonReferenceNo());
		assertEquals(1, d3.getBaseC());
		assertEquals(20, d3.getcQual());
		
		PileupDataRecord d5 = new PileupDataRecord(123);
		d5.checkBase('N', 20, 'N',  samRecords.get(0));
		assertEquals(0, d5.getNonReferenceNo());
		assertEquals(1, d5.getBaseN());
		assertEquals(20, d5.getnQual());		
	}
	
	@Test
	public void testToString() {
		PileupDataRecord d1 = new PileupDataRecord(123);
		String recordString = d1.toString();
		assertFalse(recordString.endsWith("\t"));
		assertEquals(StrandEnum.values().length, recordString.split("\t").length);
	}

	


}
