package org.qcmg.picard.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMOrBAMWriterFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

public class BAMFileUtilsTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void getContigNamesFromHeader() throws IOException {
		File bam = testFolder.newFile("getContigNamesFromHeader.bam");
		BAMFileUtilsTest.getBamFile(bam, null, true);
		List<String> contigs = BAMFileUtils.getContigsFromBamFile(bam);
		assertEquals(5, contigs.size());
		assertEquals("chr1", contigs.get(0));
		assertEquals("chr5", contigs.get(4));
	}
	
	@Test
	public void getContigNamesFromHeaderNoChr() throws IOException {
		File bam = testFolder.newFile("getContigNamesFromHeaderNoChr.bam");
		BAMFileUtilsTest.getBamFile(bam, null, false);
		List<String> contigs = BAMFileUtils.getContigsFromBamFile(bam);
		assertEquals(5, contigs.size());
		assertEquals("1", contigs.get(0));
		assertEquals("5", contigs.get(4));
	}
	
	 private static void getBamFile(File bamFile, List<SAMRecord> data, boolean useChrs) {
    	final SAMFileHeader header = getHeader(useChrs);
    	final SAMOrBAMWriterFactory factory = new SAMOrBAMWriterFactory(header, false, bamFile, false);
    	try {
    		final SAMFileWriter writer = factory.getWriter();
    		if (null != data)
    			for (final SAMRecord s : data) writer.addAlignment(s);
    	} finally {
    		factory.closeWriter();
    	}
    }
	    
	private static SAMFileHeader getHeader(boolean useChrs) {
		final SAMFileHeader header = new SAMFileHeader();
		
		final SAMProgramRecord bwaPG = new SAMProgramRecord("bwa");
		bwaPG.setProgramName("bwa");
		bwaPG.setProgramVersion("0.6.1-r104");
		header.addProgramRecord(bwaPG);
		
		// looks like we need this to be specifically defined
		final SAMSequenceDictionary seqDict = new SAMSequenceDictionary();
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr1" : "1", 249250621));
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr2" : "2", 243199373));
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr3" : "3", 198022430));
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr4" : "4", 191154276));
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr5" : "5", 180915260));
		header.setSequenceDictionary(seqDict);
		return header;
	}

}
