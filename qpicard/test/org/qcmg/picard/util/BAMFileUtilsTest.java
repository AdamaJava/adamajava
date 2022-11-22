package org.qcmg.picard.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMWriterFactory;

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
		getBamFile(bam, null, true);
		List<String> contigs = BAMFileUtils.getContigsFromBamFile(bam);
		assertEquals(5, contigs.size());
		assertEquals("chr1", contigs.get(0));
		assertEquals("chr5", contigs.get(4));
	}
	
	@Test
	public void getContigNamesFromHeaderNoChr() throws IOException {
		File bam = testFolder.newFile("getContigNamesFromHeaderNoChr.bam");
		getBamFile(bam, null, false);
		List<String> contigs = BAMFileUtils.getContigsFromBamFile(bam);
		assertEquals(5, contigs.size());
		assertEquals("1", contigs.get(0));
		assertEquals("5", contigs.get(4));
	}
	
	 @Test(expected = IOException.class)
	public void renameAlreadyCanonicalIndex() throws IOException {
		Path dir = Files.createTempDirectory("java-test");
		File bam = new File(dir.resolve("test.bam").toString());
		File bai = new File(dir.resolve("test.bam.bai").toString());
		// create the bam and an already-canonically named index
		bam.createNewFile();
		bai.createNewFile();
		// expect IOException
		BAMFileUtils.renameIndex(bam);
	}

	@Test
	public void renameBamIndexTest() throws IOException {
		Path dir = Files.createTempDirectory("java-test");
		File bam = new File(dir.resolve("test.bam").toString());
		File bai = new File(dir.resolve("test.bai").toString());
		// create the bam and a picard-style ${stem/bam/bai} index 
		bam.createNewFile();
		bai.createNewFile();
		File baiRenamed = new File(dir.resolve("test.bam.bai").toString());
	
		Assert.assertTrue(bai.exists());
		Assert.assertFalse(baiRenamed.exists());
		BAMFileUtils.renameIndex(bam);
		Assert.assertTrue(baiRenamed.exists());
		Assert.assertFalse(bai.exists());		
	}
	
	@Test
	public void renameCramIndexTest() throws IOException {
		Path dir = Files.createTempDirectory("java-test");
		File bam = new File(dir.resolve("test.cram").toString());
		File bai = new File(dir.resolve("test.cram.bai").toString());
		// create the bam and a picard-style ${stem/bam/bai} index 
		bam.createNewFile();
		bai.createNewFile();
		File baiRenamed = new File(dir.resolve("test.cram.crai").toString());
	
		Assert.assertTrue(bai.exists());
		Assert.assertFalse(baiRenamed.exists());
		BAMFileUtils.renameIndex(bam);
		Assert.assertTrue(baiRenamed.exists());
		Assert.assertFalse(bai.exists());		
	}

	@Test(expected = IOException.class)
	public void renameNonExistentIndex() throws IOException {		
		Path dir = Files.createTempDirectory("java-test");
		File bam = new File(dir.resolve("test.bam").toString());
		// create the bam but no index
		bam.createNewFile();
		// expect IOException
		BAMFileUtils.renameIndex(bam);
		
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void renameNullIndex() throws IOException {		
 		BAMFileUtils.renameIndex(null);		
	}

	private static void getBamFile(File bamFile, List<SAMRecord> data, boolean useChrs) {
    	final SAMFileHeader header = getHeader(useChrs);
    	final SAMWriterFactory factory = new SAMWriterFactory(header, false, bamFile, false);
    	try(SAMFileWriter writer = factory.getWriter();) {
     		if (null != data)
    			for (final SAMRecord s : data) writer.addAlignment(s);
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
