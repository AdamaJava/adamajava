package org.qcmg.qsv.softclip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.util.QSVUtil;
import org.qcmg.qsv.util.TestUtil;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class SoftClipStaticMethodsTest {
	
	Breakpoint breakpoint;

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testCreateSoftClipRecordIsLeft() throws IOException {
		
        SAMRecord record = getSAMRecords(11); 
        
        //left clip
		Clip leftClip = SoftClipStaticMethods.createSoftClipRecord(record, record.getReadGroup().getId(), 89712340, 89712350);
		assertTrue(leftClip.isLeft());
		assertEquals(59, leftClip.getLength());
		assertEquals("AAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA", leftClip.getClipSequence());
		assertEquals(record.getAlignmentStart(), leftClip.getBpPos());
	}
	
	@Test
	public void testCreateSoftClipRecordIsRight() throws IOException {
		
		SAMRecord record = getSAMRecords(5); 
		//right clip		
		Clip rightClip = SoftClipStaticMethods.createSoftClipRecord(record, record.getReadGroup().getId(), 89699960, 89700400);
		assertEquals(14, rightClip.getLength());
		assertEquals("GAGATTATACTTTG", rightClip.getClipSequence());
		assertEquals(record.getAlignmentEnd(), rightClip.getBpPos());
	}
	
	@Test
	public void testCreateSoftClipRecordIsNull() throws IOException {
		
		SAMRecord record = getSAMRecords(5); 
		//right clip
		SoftClipStaticMethods.createSoftClipRecord(record, record.getReadGroup().getId(), 89700213, 89700213);
		Clip rightClip = SoftClipStaticMethods.createSoftClipRecord(record, record.getReadGroup().getId(), 89712341, 89712341);
		assertNull(rightClip);
	}
	
	private SAMRecord getSAMRecords(int index) throws IOException {
		File tumourBam = testFolder.newFile("test.bam");
        TestUtil.createHiseqBamFile(tumourBam.getCanonicalPath(), null, SortOrder.coordinate);
        SamReader read = SAMFileReaderFactory.createSAMFileReader(tumourBam);
        List<SAMRecord> list = new ArrayList<SAMRecord>();
        for (SAMRecord r : read) {
        	list.add(r);        	
        }
        
        read.close();
        return list.get(index);
	}
	
	@Test
	public void testGetSoftClipFile() {		
		String expected = testFolder.getRoot().toString() + QSVUtil.getFileSeparator() + "tumour.chr1.clip";
		assertEquals(expected, SoftClipStaticMethods.getSoftClipFile("chr1", "tumour",testFolder.getRoot().toString()));
	}

}
