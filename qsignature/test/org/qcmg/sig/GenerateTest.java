package org.qcmg.sig;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.qio.vcf.VcfFileReader;

import gnu.trove.map.TObjectIntMap;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

public class GenerateTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	public Generate qss;
	
	@Before
	public void setup() {
		qss = new Generate();
	}
	
	@Test
	public void getDist() {
		int[][] readGroupBasesArray = new int[4][4];
		int[] rg1Bases = new int[]{0,0,0,0};
		int[] rg2Bases = new int[]{10,9,8,7};
		readGroupBasesArray[0] = rg1Bases;
		readGroupBasesArray[1] = rg2Bases;
		
		assertEquals("", Generate.getDist(readGroupBasesArray, 0));
		assertEquals("10-9-8-7", Generate.getDist(readGroupBasesArray, 1));
		assertEquals("", Generate.getDist(readGroupBasesArray, 2));
		assertEquals("", Generate.getDist(readGroupBasesArray, 3));
		
		readGroupBasesArray[2] = new int[]{100,90,80,70};
		assertEquals("", Generate.getDist(readGroupBasesArray, 0));
		assertEquals("10-9-8-7", Generate.getDist(readGroupBasesArray, 1));
		assertEquals("100-90-80-70", Generate.getDist(readGroupBasesArray, 2));
		assertEquals("", Generate.getDist(readGroupBasesArray, 3));
	}
	
	@Test
	public void getTotalDist() {
		int[][] readGroupBasesArray = new int[4][4];
		int[] rg1Bases = new int[]{0,0,0,0};
		int[] rg2Bases = new int[]{10,9,8,7};
		readGroupBasesArray[0] = rg1Bases;
		readGroupBasesArray[1] = rg2Bases;
		
		assertEquals("10-9-8-7", Generate.getTotalDist(readGroupBasesArray));
		
		readGroupBasesArray[0] = new int[]{1,1,1,1};
		assertEquals("11-10-9-8", Generate.getTotalDist(readGroupBasesArray));
		
		readGroupBasesArray[3] = new int[]{1,2,3,4};
		assertEquals("12-12-12-12", Generate.getTotalDist(readGroupBasesArray));
		
		readGroupBasesArray[2] = new int[]{10,20,30,40};
		assertEquals("22-32-42-52", Generate.getTotalDist(readGroupBasesArray));
	}
	
	@Test
	public void recordWithNoRG() {
		SAMFileHeader header = SignatureGeneratorTest.getHeader(true, true, true);
		/*
		 * setup map of rgids
		 */
		TObjectIntMap<String> rgIds = Generate.getReadGroupsAsMap(header);
		
		/*
		 * setup SAMRecord
		 */
		SAMRecord sam = new SAMRecord(header);
		sam.setAlignmentStart(126890960);
		sam.setReferenceName( "chr12" );
		sam.setFlags(67);
		sam.setReadName("HS2000-152_756:1:1215:14830:88102");
		sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
		sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
		sam.setMappingQuality(60);
		sam.setCigarString("24M4D76M");
		String rgId = null != sam.getReadGroup() ? sam.getReadGroup().getId() : null;
        assertNull(rgId);
		assertEquals(0, rgIds.get(rgId));
	}
	
	@Test
	public void recordWithRGInList() {
		SAMFileHeader header = SignatureGeneratorTest.getHeader(true, true, true);
		/*
		 * setup map of rgids
		 */
		TObjectIntMap<String> rgIds = Generate.getReadGroupsAsMap(header);
		
		/*
		 * setup SAMRecord
		 */
		SAMRecord sam = new SAMRecord(header);
		sam.setAlignmentStart(126890960);
		sam.setReferenceName( "chr12" );
		sam.setFlags(67);
		sam.setReadName("HS2000-152_756:1:1215:14830:88102");
		sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
		sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
		sam.setMappingQuality(60);
		sam.setCigarString("24M4D76M");
		sam.setAttribute("RG", "20130325103517169");
		String rgId = null != sam.getReadGroup() ? sam.getReadGroup().getId() : null;
		assertEquals("20130325103517169", rgId);
		assertEquals(1, rgIds.get(rgId));
	}
	
	@Test
	public void recordWithRGNotInList() {
		SAMFileHeader header = SignatureGeneratorTest.getHeader(true, true, true);
		/*
		 * setup map of rgids
		 */
		TObjectIntMap<String> rgIds = Generate.getReadGroupsAsMap(header);
		
		/*
		 * setup SAMRecord
		 */
		SAMRecord sam = new SAMRecord(header);
		sam.setAlignmentStart(126890960);
		sam.setReferenceName( "chr12" );
		sam.setFlags(67);
		sam.setReadName("HS2000-152_756:1:1215:14830:88102");
		sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
		sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
		sam.setMappingQuality(60);
		sam.setCigarString("24M4D76M");
		sam.setAttribute("RG", "This_is_no_in_the_list");
		String rgId = null != sam.getReadGroup() ? sam.getReadGroup().getId() : null;
		/*
		 * will be null as the RG needs to be set on the record and also exist in the header
		 */
        assertNull(rgId);
		assertEquals(0, rgIds.get(rgId));
	}
	
	@Test
	public void rgMapReturnValueWithRGs() {
		SAMFileHeader header = SignatureGeneratorTest.getHeader(true, true, true);
		/*
		 * setup map of rgids
		 */
		TObjectIntMap<String> rgIds = Generate.getReadGroupsAsMap(header);
		
		assertEquals(4, rgIds.size());
		assertEquals(0, rgIds.get(null));
		assertEquals(1, rgIds.get("20130325103517169"));
		assertEquals(2, rgIds.get("20130325112045146"));
		assertEquals(3, rgIds.get("20130325084856212"));
	}
	@Test
	public void rgMapReturnValueNoGs() {
		SAMFileHeader header = SignatureGeneratorTest.getHeader(true, true, false);
		/*
		 * setup map of rgids
		 */
		TObjectIntMap<String> rgIds = Generate.getReadGroupsAsMap(header);
		
		assertEquals(1, rgIds.size());
		assertEquals(0, rgIds.get(null));
		assertEquals(0, rgIds.get("20130325103517169"));
		assertEquals(0, rgIds.get("20130325112045146"));
		assertEquals(0, rgIds.get("20130325084856212"));
		assertEquals(0, rgIds.get("Any_value_not_in_map_will_return_0"));
	}
	
	@Test
    public void runProcessWithHG19BamFile() throws Exception {
    	final File positionsOfInterestFile = testFolder.newFile("runProcessWithHG19BamFile.snps.txt");
    	final File bamFile = testFolder.newFile("runProcessWithHG19BamFile.bam");
    	final File logFile = testFolder.newFile("runProcessWithHG19BamFile.log");
    	final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
    	final File outputFile = new File(outputFIleName);
	    	
	    SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
	    SignatureGeneratorTest.getBamFile(bamFile);
	    	
    	final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath()} );
    	assertEquals(0, exitStatus);
    	
    	assertTrue(outputFile.exists());
   	
    	final List<VcfRecord> recs = new ArrayList<>();
    	try (VcfFileReader reader = new VcfFileReader(outputFile)) {
    		/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=44c26173967735d324a818769fcf3ae1", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
	    	for (final VcfRecord rec : reader) {
	    		recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
	    		System.out.print("rec: " + rec);
	    	}
	    	VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
    	}
       	
    	assertEquals(7, recs.size());
    	assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(0).getInfo());
    	assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(1).getInfo());
    	assertEquals("QAF=t:20-0-0-0,rg0:20-0-0-0", recs.get(2).getInfo());
    	assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(3).getInfo());
    	assertEquals("QAF=t:0-0-0-20,rg0:0-0-0-20", recs.get(4).getInfo());
    	assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(5).getInfo());
    	assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
    }
	
	@Test
	public void runProcessWithHG19BamFileStream() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithHG19BamFileStream.snps.txt");
		final File bamFile = testFolder.newFile("runProcessWithHG19BamFileStream.bam");
		final File logFile = testFolder.newFile("runProcessWithHG19BamFileStream.log");
		final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFIleName);
		
		SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
		SignatureGeneratorTest.getBamFile(bamFile);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath(), "--stream"} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=44c26173967735d324a818769fcf3ae1", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
				System.out.print("rec: " + rec);
			}
			VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
		}
		
		assertEquals(7, recs.size());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(1).getInfo());
		assertEquals("QAF=t:20-0-0-0,rg0:20-0-0-0", recs.get(2).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(3).getInfo());
		assertEquals("QAF=t:0-0-0-20,rg0:0-0-0-20", recs.get(4).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(5).getInfo());
		assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
	}
	
	@Test
	public void runProcessWithHG19BamFileVcf() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithHG19BamFileVcf.snps.vcf");
		final File bamFile = testFolder.newFile("runProcessWithHG19BamFileVcf.bam");
		final File logFile = testFolder.newFile("runProcessWithHG19BamFileVcf.log");
		final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFIleName);
		
		SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
		SignatureGeneratorTest.getBamFile(bamFile);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath()} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=e8f0f76a06ba1f2763167e37c254747f", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
		}
		
		assertEquals(7, recs.size());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(1).getInfo());
		assertEquals("QAF=t:20-0-0-0,rg0:20-0-0-0", recs.get(2).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(3).getInfo());
		assertEquals("QAF=t:0-0-0-20,rg0:0-0-0-20", recs.get(4).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(5).getInfo());
		assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
	}
	
	@Test
	public void runProcessWithHG19BamFileVcfStream() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithHG19BamFileVcfStream.snps.vcf");
		final File bamFile = testFolder.newFile("runProcessWithHG19BamFileVcfStream.bam");
		final File logFile = testFolder.newFile("runProcessWithHG19BamFileVcfStream.log");
		final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFIleName);
		
		SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
		SignatureGeneratorTest.getBamFile(bamFile);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath(), "--stream"} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=e8f0f76a06ba1f2763167e37c254747f", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
		}
		
		assertEquals(7, recs.size());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(1).getInfo());
		assertEquals("QAF=t:20-0-0-0,rg0:20-0-0-0", recs.get(2).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(3).getInfo());
		assertEquals("QAF=t:0-0-0-20,rg0:0-0-0-20", recs.get(4).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(5).getInfo());
		assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
	}
	
	@Test
	public void runProcessWithSnpChipFile() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithSnpChipFile.snps.vcf");
		final File snpChipFile = testFolder.newFile("runProcessWithSnpChipFile.txt");
		final File illuminaArraysDesignFile = testFolder.newFile("runProcessWithSnpChipFile.illumina.txt");
		final File logFile = testFolder.newFile("runProcessWithSnpChipFile.log");
		final String outputFileName = snpChipFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFileName);
		
		SignatureGeneratorTest.writeSnpChipFile(snpChipFile);
		SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
		SignatureGeneratorTest.writeIlluminaArraysDesignFile(illuminaArraysDesignFile);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , snpChipFile.getAbsolutePath(),"-illuminaArraysDesign", illuminaArraysDesignFile.getAbsolutePath()} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=e8f0f76a06ba1f2763167e37c254747f", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertFalse(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));	// no rgs in snp chips
		}	
		
		assertEquals(3, recs.size());
		assertEquals("QAF=t:0-0-0-30", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-24-0", recs.get(1).getInfo());
		assertEquals("QAF=t:10-0-10-0", recs.get(2).getInfo());
	}
	
	@Test
	public void runProcessWithSnpChipFileStream() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithSnpChipFileStream.snps.vcf");
		final File snpChipFile = testFolder.newFile("runProcessWithSnpChipFileStream.txt");
		final File illuminaArraysDesignFile = testFolder.newFile("runProcessWithSnpChipFileStream.illumina.txt");
		final File logFile = testFolder.newFile("runProcessWithSnpChipFileStream.log");
		final String outputFileName = snpChipFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFileName);
		
		SignatureGeneratorTest.writeSnpChipFile(snpChipFile);
		SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
		SignatureGeneratorTest.writeIlluminaArraysDesignFile(illuminaArraysDesignFile);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , snpChipFile.getAbsolutePath(),"-illuminaArraysDesign", illuminaArraysDesignFile.getAbsolutePath(), "--stream"} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=e8f0f76a06ba1f2763167e37c254747f", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertFalse(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));	// no rgs in snp chips
		}	
		
		assertEquals(3, recs.size());
		assertEquals("QAF=t:0-0-0-30", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-24-0", recs.get(1).getInfo());
		assertEquals("QAF=t:10-0-10-0", recs.get(2).getInfo());
	}
	
	@Test
	public void runProcessWithSnpPositionsHeader() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithSnpPositionsHeader.snps.vcf");
		final File bamFile = testFolder.newFile("runProcessWithSnpPositionsHeader.bam");
		final File logFile = testFolder.newFile("runProcessWithSnpPositionsHeader.log");
		final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFIleName);
		
		//    	writeSnpChipFile(snpChipFile);
		SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
		SignatureGeneratorTest.getBamFile(bamFile);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath()} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=e8f0f76a06ba1f2763167e37c254747f", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
				System.out.print("rec: " + rec.toString());
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
		}
		
		assertEquals(7, recs.size());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(1).getInfo());
		assertEquals("QAF=t:20-0-0-0,rg0:20-0-0-0", recs.get(2).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(3).getInfo());
		assertEquals("QAF=t:0-0-0-20,rg0:0-0-0-20", recs.get(4).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(5).getInfo());
		assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
	}
	
	@Test
	public void runProcessWithSnpPositionsHeaderStream() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithSnpPositionsHeaderStream.snps.vcf");
		final File bamFile = testFolder.newFile("runProcessWithSnpPositionsHeaderStream.bam");
		final File logFile = testFolder.newFile("runProcessWithSnpPositionsHeaderStream.log");
		final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFIleName);
		
		//    	writeSnpChipFile(snpChipFile);
		SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
		SignatureGeneratorTest.getBamFile(bamFile);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath(), "--stream"} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=e8f0f76a06ba1f2763167e37c254747f", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
				System.out.print("rec: " + rec.toString());
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
		}
		
		assertEquals(7, recs.size());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-0-10,rg0:0-0-0-10", recs.get(1).getInfo());
		assertEquals("QAF=t:20-0-0-0,rg0:20-0-0-0", recs.get(2).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(3).getInfo());
		assertEquals("QAF=t:0-0-0-20,rg0:0-0-0-20", recs.get(4).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg0:0-10-0-0", recs.get(5).getInfo());
		assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
	}
	
	@Test
	public void runProcessWithReadGroupsSetInHeader() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithReadGroupsSetInHeader.snps.txt");
		final File bamFile = testFolder.newFile("runProcessWithReadGroupsSetInHeader.bam");
		final File logFile = testFolder.newFile("runProcessWithReadGroupsSetInHeader.log");
		final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFIleName);
		
		SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
		SignatureGeneratorTest.getBamFile(bamFile, true);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath()} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=44c26173967735d324a818769fcf3ae1", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg1=20130325103517169")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg2=20130325112045146")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg3=20130325084856212")));
		}
		
		assertEquals(7, recs.size());
		assertEquals("QAF=t:0-0-0-10,rg1:0-0-0-10", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-0-10,rg1:0-0-0-10", recs.get(1).getInfo());
		assertEquals("QAF=t:20-0-0-0,rg2:20-0-0-0", recs.get(2).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg3:0-10-0-0", recs.get(3).getInfo());
		assertEquals("QAF=t:0-0-0-20,rg2:0-0-0-20", recs.get(4).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg1:0-10-0-0", recs.get(5).getInfo());
		assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
	}
	
	@Test
	public void runProcessWithReadGroupsSetInHeaderStream() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithReadGroupsSetInHeaderStream.snps.txt");
		final File bamFile = testFolder.newFile("runProcessWithReadGroupsSetInHeaderStream.bam");
		final File logFile = testFolder.newFile("runProcessWithReadGroupsSetInHeaderStream.log");
		final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFIleName);
		
		SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
		SignatureGeneratorTest.getBamFile(bamFile, true);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath(), "--stream"} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=44c26173967735d324a818769fcf3ae1", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg1=20130325103517169")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg2=20130325112045146")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg3=20130325084856212")));
		}
		
		assertEquals(7, recs.size());
		assertEquals("QAF=t:0-0-0-10,rg1:0-0-0-10", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-0-10,rg1:0-0-0-10", recs.get(1).getInfo());
		assertEquals("QAF=t:20-0-0-0,rg2:20-0-0-0", recs.get(2).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg3:0-10-0-0", recs.get(3).getInfo());
		assertEquals("QAF=t:0-0-0-20,rg2:0-0-0-20", recs.get(4).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg1:0-10-0-0", recs.get(5).getInfo());
		assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
	}
	
	@Test
	public void runProcessWithReadGroupsSetInHeaderVcf() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithReadGroupsSetInHeaderVcf.snps.vcf");
		final File bamFile = testFolder.newFile("runProcessWithReadGroupsSetInHeaderVcf.bam");
		final File logFile = testFolder.newFile("runProcessWithReadGroupsSetInHeaderVcf.log");
		final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFIleName);
		
		SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
		SignatureGeneratorTest.getBamFile(bamFile, true);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath()} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=e8f0f76a06ba1f2763167e37c254747f", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg1=20130325103517169")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg2=20130325112045146")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg3=20130325084856212")));
		}
		
		assertEquals(7, recs.size());
		assertEquals("QAF=t:0-0-0-10,rg1:0-0-0-10", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-0-10,rg1:0-0-0-10", recs.get(1).getInfo());
		assertEquals("QAF=t:20-0-0-0,rg2:20-0-0-0", recs.get(2).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg3:0-10-0-0", recs.get(3).getInfo());
		assertEquals("QAF=t:0-0-0-20,rg2:0-0-0-20", recs.get(4).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg1:0-10-0-0", recs.get(5).getInfo());
		assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
	}
	
	@Test
	public void runProcessWithReadGroupsSetInHeaderVcfStream() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithReadGroupsSetInHeaderVcfStream.snps.vcf");
		final File bamFile = testFolder.newFile("runProcessWithReadGroupsSetInHeaderVcfStream.bam");
		final File logFile = testFolder.newFile("runProcessWithReadGroupsSetInHeaderVcfStream.log");
		final String outputFIleName = bamFile.getAbsolutePath() + ".qsig.vcf.gz";
		final File outputFile = new File(outputFIleName);
		
		SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
		SignatureGeneratorTest.getBamFile(bamFile, true);
		
		final int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-snpPositions" , positionsOfInterestFile.getAbsolutePath(), "-i" , bamFile.getAbsolutePath(), "--stream"} );
		assertEquals(0, exitStatus);
		
		assertTrue(outputFile.exists());
		
		final List<VcfRecord> recs = new ArrayList<>();
		try (VcfFileReader reader = new VcfFileReader(outputFile)) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=e8f0f76a06ba1f2763167e37c254747f", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			for (final VcfRecord rec : reader) {
				recs.add(rec);
                assertTrue(rec.getRef().equals("A") || rec.getRef().equals("C") || rec.getRef().equals("G") || rec.getRef().equals("T"));
			}
			VcfHeader header = reader.getVcfHeader();
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg0=null")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg1=20130325103517169")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg2=20130325112045146")));
            assertTrue(header.getAllMetaRecords().contains(new VcfHeaderRecord("##rg3=20130325084856212")));
		}
		
		assertEquals(7, recs.size());
		assertEquals("QAF=t:0-0-0-10,rg1:0-0-0-10", recs.get(0).getInfo());
		assertEquals("QAF=t:0-0-0-10,rg1:0-0-0-10", recs.get(1).getInfo());
		assertEquals("QAF=t:20-0-0-0,rg2:20-0-0-0", recs.get(2).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg3:0-10-0-0", recs.get(3).getInfo());
		assertEquals("QAF=t:0-0-0-20,rg2:0-0-0-20", recs.get(4).getInfo());
		assertEquals("QAF=t:0-10-0-0,rg1:0-10-0-0", recs.get(5).getInfo());
		assertEquals("QAF=t:0-20-0-0,rg0:0-20-0-0", recs.get(6).getInfo());
	}
	
	@Test
	public void runProcessWithNoOutputOption() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithNoOutputOption.snps.txt");
		final File illuminaArraysDesignFile = testFolder.newFile("runProcessWithNoOutputOption.illuminaarray.txt");
		final File bamFile = testFolder.newFile("runProcessWithNoOutputOption.bam");
		final File logFile = testFolder.newFile("runProcessWithNoOutputOption.log");
		
		SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
		
		/*
		 * no output or dir option specified
		 * output will live next to input
		 */
		int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), 
				"-snpPositions" , positionsOfInterestFile.getAbsolutePath(), 
				"-i" , bamFile.getAbsolutePath(),  
				"-illuminaArraysDesign" , illuminaArraysDesignFile.getAbsolutePath()} );
		assertEquals(0, exitStatus);
		assertTrue(new File(bamFile.getAbsolutePath() + ".qsig.vcf.gz").exists());
		assertTrue(new File(bamFile.getAbsolutePath() + ".qsig.vcf.gz").length() > 0);
	}
		
		
	@Test
	public void runProcessWithOutputOptionFile() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithOutputOptionFile.snps.txt");
		final File illuminaArraysDesignFile = testFolder.newFile("runProcessWithOutputOptionFile.illuminaarray.txt");
		final File bamFile = testFolder.newFile("runProcessWithOutputOptionFile.bam");
		final File logFile = testFolder.newFile("runProcessWithOutputOptionFile.log");
		
		SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
		/*
		 * and now with the output option pointing to a file
		 */
		String outputFile = testFolder.newFile("output_file").getAbsolutePath();
		int exitStatus = qss.setup(new String[] {"--log", logFile.getAbsolutePath(), 
				"-snpPositions", positionsOfInterestFile.getAbsolutePath(), 
				"-i", bamFile.getAbsolutePath(),  
				"-output", outputFile,  
				"-illuminaArraysDesign", illuminaArraysDesignFile.getAbsolutePath()} );
		assertEquals(0, exitStatus);
		assertTrue(new File(outputFile).exists());
		assertTrue(new File(outputFile).length() > 0);
	}
		
	
	@Test
	public void runProcessWithOutputOptionDir() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithOutputOptionDir.snps.txt");
		final File illuminaArraysDesignFile = testFolder.newFile("runProcessWithOutputOptionDir.illuminaarray.txt");
		final File bamFile = testFolder.newFile("runProcessWithOutputOptionDir.bam");
		final File logFile = testFolder.newFile("runProcessWithOutputOptionDir.log");
		
		SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
		/*
		 * and now with the output option pointing to a folder
		 * this means the output will be in the form : folder/input_name.qsig.vcf.gz
		 */
		String outputFolder = testFolder.newFolder("output_folder").getAbsolutePath();
		int exitStatus = qss.setup(new String[] {"--log", logFile.getAbsolutePath(), 
				"-snpPositions", positionsOfInterestFile.getAbsolutePath(), 
				"-i", bamFile.getAbsolutePath(),  
				"-output", outputFolder,  
				"-illuminaArraysDesign", illuminaArraysDesignFile.getAbsolutePath()} );
		assertEquals(0, exitStatus);
		assertTrue(new File(outputFolder + File.separator + bamFile.getName() + ".qsig.vcf.gz").exists());
		assertTrue(new File(outputFolder + File.separator + bamFile.getName() + ".qsig.vcf.gz").length() > 0);
	}

	@Test
	public void runProcessWithDirOption() throws Exception {
		final File positionsOfInterestFile = testFolder.newFile("runProcessWithDirOption.snps.txt");
		final File bamFile = testFolder.newFile("runProcessWithDirOption.bam");
		final File logFile = testFolder.newFile("runProcessWithDirOption.log");
		
		SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
		/*
		 * and now with the dir option pointing to a folder
		 * this means the output will be in the form : dir/input_name.qsig.vcf.gz
		 */
		String dir = testFolder.newFolder("output_dir").getAbsolutePath();
		int exitStatus = qss.setup(new String[] {"--log", logFile.getAbsolutePath(), 
				"-snpPositions", positionsOfInterestFile.getAbsolutePath(), 
				"-i", bamFile.getAbsolutePath(),  
				"-d", dir});
		assertEquals(0, exitStatus);
        assertTrue(new File(dir + File.separator + bamFile.getName() + ".qsig.vcf.gz").exists());
		assertTrue(new File(dir + File.separator + bamFile.getName() + ".qsig.vcf.gz").length() > 0);
	}
	
	@Test
	public void runProcessWithGenePositionsOption() throws Exception {
		final File genesOfInterestFile = testFolder.newFile("runProcessWithGenePositionsOption.genes.gff3");
		final File bamFile = testFolder.newFile("runProcessWithGenePositionsOption.bam");
		final File logFile = testFolder.newFile("runProcessWithGenePositionsOption.log");
		final File refFile = testFolder.newFile("runProcessWithGenePositionsOption.fa");
		final File refIndexFile = testFolder.newFile("runProcessWithGenePositionsOption.fa.fai");
		
		SignatureGeneratorTest.writeGenePositionsFile(genesOfInterestFile, 10, 1000);
		String dir = testFolder.newFolder("output_dir").getAbsolutePath();
		try {
			/*
			 * this should fail as we are not supplying the reference file
			 */
			qss.setup(new String[] {"--log", logFile.getAbsolutePath(), 
					"-genePositions", genesOfInterestFile.getAbsolutePath(), 
					"-i", bamFile.getAbsolutePath(),  
					"-d", dir} );
			fail("Should have thrown an IllegalArgumentException");
		} catch (QSignatureException e) {}
		
		setupReferenceFile(refFile, refIndexFile);
		SignatureGeneratorTest.getBamFile(bamFile, true);
		
		int exitStatus = qss.setup(new String[] {"--log", logFile.getAbsolutePath(), 
				"-genePositions", genesOfInterestFile.getAbsolutePath(), 
				"-i", bamFile.getAbsolutePath(),  
				"-reference", refFile.getAbsolutePath(),  
				"-d", dir} );
		assertEquals(0, exitStatus);
		String name = dir + File.separator + bamFile.getName() + ".qsig.vcf.gz";
        assertTrue(new File(name).exists());
		assertTrue(new File(name).length() > 0);
		try (VcfFileReader reader = new VcfFileReader(new File(name))) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=3c608c8bc46b55b08b14dec84efd179e", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			int vcfRecordCounter = 0;
			for (VcfRecord rec : reader) {
				vcfRecordCounter++;
			}
			assertEquals(97, vcfRecordCounter);
		}
	}
	
	@Test
	public void runProcessWithGenePositionsOptionNoOverlap() throws Exception {
		final File genesOfInterestFile = testFolder.newFile("runProcessWithGenePositionsOptioNoOverlap.genes.gff3");
		final File bamFile = testFolder.newFile("runProcessWithGenePositionsOptioNoOverlap.bam");
		final File logFile = testFolder.newFile("runProcessWithGenePositionsOptioNoOverlap.log");
		final File refFile = testFolder.newFile("runProcessWithGenePositionsOptioNoOverlap.fa");
		final File refIndexFile = testFolder.newFile("runProcessWithGenePositionsOptioNoOverlap.fa.fai");
		
		SignatureGeneratorTest.writeGenePositionsFile(genesOfInterestFile, 10000, 12000);
		String dir = testFolder.newFolder("output_dir").getAbsolutePath();
		
		setupReferenceFile(refFile, refIndexFile);
		SignatureGeneratorTest.getBamFile(bamFile, true);
		
		int exitStatus = qss.setup(new String[] {"--log", logFile.getAbsolutePath(), 
				"-genePositions", genesOfInterestFile.getAbsolutePath(), 
				"-i", bamFile.getAbsolutePath(),  
				"-reference", refFile.getAbsolutePath(),  
				"-d", dir} );
		assertEquals(0, exitStatus);
		String name = dir + File.separator + bamFile.getName() + ".qsig.vcf.gz";
        assertTrue(new File(name).exists());
		assertTrue(new File(name).length() > 0);
		try (VcfFileReader reader = new VcfFileReader(new File(name))) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=7abe1a11cdda9e86b9651b2bec3ff04b", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			int vcfRecordCounter = 0;
			for (VcfRecord rec : reader) {
				vcfRecordCounter++;
			}
			assertEquals(0, vcfRecordCounter);
		}
	}
	
	@Test
	public void runProcessWithGenePositionsOptionPartialOverlap() throws Exception {
		final File genesOfInterestFile = testFolder.newFile("runProcessWithGenePositionsOptioPartialOverlap.genes.gff3");
		final File bamFile = testFolder.newFile("runProcessWithGenePositionsOptioPartialOverlap.bam");
		final File logFile = testFolder.newFile("runProcessWithGenePositionsOptioPartialOverlap.log");
		final File refFile = testFolder.newFile("runProcessWithGenePositionsOptioPartialOverlap.fa");
		final File refIndexFile = testFolder.newFile("runProcessWithGenePositionsOptioPartialOverlap.fa.fai");
		
		SignatureGeneratorTest.writeGenePositionsFile(genesOfInterestFile, 10, 200);
		String dir = testFolder.newFolder("output_dir").getAbsolutePath();
		
		setupReferenceFile(refFile, refIndexFile);
		SignatureGeneratorTest.getBamFile(bamFile, true);
		
		int exitStatus = qss.setup(new String[] {"--log", logFile.getAbsolutePath(), 
				"-genePositions", genesOfInterestFile.getAbsolutePath(), 
				"-i", bamFile.getAbsolutePath(),  
				"-reference", refFile.getAbsolutePath(),  
				"-d", dir} );
		assertEquals(0, exitStatus);
		String name = dir + File.separator + bamFile.getName() + ".qsig.vcf.gz";
        assertTrue(new File(name).exists());
		assertTrue(new File(name).length() > 0);
		try (VcfFileReader reader = new VcfFileReader(new File(name))) {
			/*
			 * get md5sum
			 */
			assertEquals("##positions_md5sum=d18b180d0ef7a72df689471007a8ae99", reader.getHeader().stream().filter(s -> s.startsWith("##positions_md5sum=")).collect(Collectors.joining()));
			
			int vcfRecordCounter = 0;
			for (VcfRecord rec : reader) {
				vcfRecordCounter++;
			}
			assertEquals(50, vcfRecordCounter);		// records start at position 150, gene ends at 200
		}
	}
	
	private void setupReferenceFile(File file, File indexFile) throws IOException {
			
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(">chr1\n");
			writer.write("GATCACAGGTCTATCACCCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGG");
			writer.write("GTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTC");
			writer.write("CTGCCTCATCCTATTATTTATCGCACCTACGTTCAATATTACAGGCGAACATACTTACTAAAGTGTGTTA");
			writer.write("ATTAATTAATGCTTGTAGGACATAATAATAACAATTGAATGTCTGCACAGCCACTTTCCACACAGACATC");
			writer.write("ATAACAAAAAATTTCCACCAAACCCCCCCTCCCCCGCTTCTGGCCACAGCACTTAAACACATCTCTGCCA");
			writer.write("AACCCCAAAAACAAAGAACCCTAACACCAGCCTAACCAGATTTCAAATTTTATCTTTTGGCGGTATGCAC");
			writer.write("TTTTAACAGTCACCCCCCAACTAACACATTATTTTCCCCTCCCACTCCCATACTACTAATCTCATCAATA");
			writer.write("CAACCCCCGCCCATCCTACCCAGCACACACACACCGCTGCTAACCCCATACCCCGAACCAACCAAACCCC");
			writer.write("AAAGACACCCCCCACAGTTTATGTAGCTTACCTCCTCAAAGCAATACACTGAAAATGTTTAGACGGGCTC");
			writer.write("ACATCACCCCATAAACAAATAGGTTTGGTCCTAGCCTTTCTATTAGCTCTTAGTAAGATTACACATGCAA");
			writer.write("GCATCCCCGTTCCAGTGAGTTCACCCTCTAAATCACCACGATCAAAAGGAACAAGCATCAAGCACGCAGC");
			writer.write("AATGCAGCTCAAAACGCTTAGCCTAGCCACACCCCCACGGGAAACAGCAGTGATTAACCTTTAGCAATAA");
			writer.write("ACGAAAGTTTAACTAAGCTATACTAACCCCAGGGTTGGTCAATTTCGTGCCAGCCACCGCGGTCACACGA");
			writer.write("TTAACCCAAGTCAATAGAAGCCGGCGTAAAGAGTGTTTTAGATCACCCCCTCCCCAATAAAGCTAAAACT");
			writer.write("CACCTGAGTTGTAAAAAACTCCAGTTGACACAAAATAGACTACGAAAGTGGCTTTAACATATCTGAACAC");
			writer.write("ACAATAGCTAAGACCCAAACTGGGATTAGATACCCCACTATGCTTAGCCCTAAACCTCAACAGTTAAATC");
			writer.write("AACAAAACTGCTCGCCAGAACACTACGAGCCACAGCTTAAAACTCAAAGGACCTGGCGGTGCTTCATATC");
			writer.write("CCTCTAGAGGAGCCTGTTCTGTAATCGATAAACCCCGATCAACCTCACCACCTCTTGCTCAGCCTATATA");
			writer.write("CCGCCATCTTCAGCAAACCCTGATGAAGGCTACAAAGTAAGCGCAAGTACCCACGTAAAGACGTTAGGTC");
			writer.write("AAGGTGTAGCCCATGAGGTGGCAAGAAATGGGCTACATTTTCTACCCCAGAAAACTACGATAGCCCTTAT");
			writer.write("GAAACTTAAGGGTCGAAGGTGGATTTAGCAGTAAACTAAGAGTAGAGTGCTTAGTTGAACAGGGCCCTGA");
			writer.write("AGCGCGTACACACCGCCCGTCACCCTCCTCAAGTATACTTCAAAGGACATTTAACTAAAACCCCTACGCA");
			writer.write("TTTATATAGAGGAGACAAGTCGTAACATGGTAAGTGTACTGGAAAGTGCACTTGGACGAACCAGAGTGTA");
			writer.write("GCTTAACACAAAGCACCCAACTTACACTTAGGAGATTTCAACTTAACTTGACCGCTCTGAGCTAAACCTA");
			writer.write("GCCCCAAACCCACTCCACCTTACTACCAGACAACCTTAGCCAAACCATTTACCCAAATAAAGTATAGGCG");
			writer.write("ATAGAAATTGAAACCTGGCGCAATAGATATAGTACCGCAAGGGAAAGATGAAAAATTATAACCAAGCATA");
			writer.write("ATATAGCAAGGACTAACCCCTATACCTTCTGCATAATGAATTAACTAGAAATAACTTTGCAAGGAGAGCC");
			writer.write("AAAGCTAAGACCCCCGAAACCAGACGAGCTACCTAAGAACAGCTAAAAGAGCACACCCGTCTATGTAGCA");
			writer.write("AAATAGTGGGAAGATTTATAGGTAGAGGCGACAAACCTACCGAGCCTGGTGATAGCTGGTTGTCCAAGAT");
			writer.write("AGAATCTTAGTTCAACTTTAAATTTGCCCACAGAACCCTCTAAATCCCCTTGTAAATTTAACTGTTAGTC");
			writer.write("CAAAGAGGAACAGCTCTTTGGACACTAGGAAAAAACCTTGTAGAGAGAGTAAAAAATTTAACACCCATAG");
			writer.write("TAGGCCTAAAAGCAGCCACCAATTAAGAAAGCGTTCAAGCTCAACACCCACTACCTAAAAAATCCCAAAC");
			writer.write("ATATAACTGAACTCCTCACACCCAATTGGACCAATCTATCACCCTATAGAAGAACTAATGTTAGTATAAG");
			writer.write("TAACATGAAAACATTCTCCTCCGCATAAGCCTGCGTCAGATTAAAACACTGAACTGACAATTAACAGCCC");
			writer.write("AATATCTACAATCAACCAACAAGTCATTATTACCCTCACTGTCAACCCAACACAGGCATGCTCATAAGGA");
			writer.write("AAGGTTAAAAAAAGTAAAAGGAACTCGGCAAATCTTACCCCGCCTGTTTACCAAAAACATCACCTCTAGC");
			writer.write("ATCACCAGTATTAGAGGCACCGCCTGCCCAGTGACACATGTTTAACGGCCGCGGTACCCTAACCGTGCAA");
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile))) {
			writer.write("chr1\t16571\t7\t50\t51\n");
		}
    }

}
