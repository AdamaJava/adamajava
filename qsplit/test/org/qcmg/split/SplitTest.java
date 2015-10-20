package org.qcmg.split;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.testing.SamTestData;

public class SplitTest {
	
	private static final Pattern colonDelimitedPattern = Pattern.compile("[:]+");
	private static final String SEP = System.getProperty("file.separator");
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	private File mergedFile;
	private final  String ORIG1 = "orig1.sam";
	private final  String ORIG2 = "orig2.sam";
	private final  String ORIG3 = "orig3.sam";
	

	@Before
	public final void before() {
		try {
			mergedFile = tempFolder.newFile("merged.sam");
			String root = tempFolder.getRoot().getPath();
			SamTestData.createMergedSam(mergedFile, root + SEP + ORIG1, root + SEP + ORIG2, root + SEP + ORIG3);
			assertTrue(mergedFile.exists());
		} catch (Exception e) {
			System.err.println("File creation error in test harness: " + e.getMessage());
		}
	}
	
	@Test
	public void testSingleSplit() throws Exception {
		
		File samFile = tempFolder.newFile("testSingleSplit.sam");
		String splitFile = tempFolder.getRoot().getAbsolutePath() + SEP + "testSingleSplit_split.sam";
		
		Map<Integer, String> splitZCFile = new HashMap<Integer, String>();
		splitZCFile.put(1, splitFile);
		
		createSamFile(samFile, splitZCFile);
		Split splitPass = new Split(samFile.getAbsolutePath(), tempFolder.getRoot().getAbsolutePath(), true, new SamSplitType());
		
		// check that only 1 output file has been created
		// check that output file contains all the details
		int origFileCount = 0, splitFileCount = 0;
		
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(samFile);//new SAMFileReader(samFile);
		for (@SuppressWarnings("unused") SAMRecord record : reader) origFileCount++;
		reader =SAMFileReaderFactory.createSAMFileReader(new File(splitFile)); //new SAMFileReader(new File(splitFile));
		for (@SuppressWarnings("unused") SAMRecord record : reader) splitFileCount++;
	
		Assert.assertEquals(origFileCount, splitFileCount);
	}
	
	@Test
	public void testMultipleSplit() throws Exception {
		
		File samFile = tempFolder.newFile("testMultipleSplit.sam");
		String splitFile1 = tempFolder.getRoot().getAbsolutePath() + SEP + "testSingleSplit_split1.sam";
		String splitFile2 = tempFolder.getRoot().getAbsolutePath() + SEP + "testSingleSplit_split2.sam";
		String splitFile3 = tempFolder.getRoot().getAbsolutePath() + SEP + "testSingleSplit_split3.sam";
		String splitFile4 = tempFolder.getRoot().getAbsolutePath() + SEP + "testSingleSplit_split4.sam";
		String splitFile5 = tempFolder.getRoot().getAbsolutePath() + SEP + "testSingleSplit_split5.sam";
		
		Map<Integer, String> splitZCFile = new HashMap<Integer, String>();
		splitZCFile.put(1, splitFile1);
		splitZCFile.put(2, splitFile2);
		splitZCFile.put(3, splitFile3);
		splitZCFile.put(4, splitFile4);
		splitZCFile.put(5, splitFile5);
		
		createSamFile(samFile, splitZCFile);
		
		Split splitPass = new Split(samFile.getAbsolutePath(), tempFolder.getRoot().getAbsolutePath(), true, new SamSplitType());
		
		// check that only 1 output file has been created
		// check that output file contains all the details
		int origFileCount = 0, splitFile1Count = 0, splitFile2Count = 0, splitFile3Count = 0, splitFile4Count = 0, splitFile5Count = 0;
		
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(samFile);
		for (@SuppressWarnings("unused") SAMRecord record : reader) origFileCount++;
		reader = SAMFileReaderFactory.createSAMFileReader(new File(splitFile1));
		for (@SuppressWarnings("unused") SAMRecord record : reader) splitFile1Count++;
		reader = SAMFileReaderFactory.createSAMFileReader(new File(splitFile2));
		for (@SuppressWarnings("unused") SAMRecord record : reader) splitFile2Count++;
		reader = SAMFileReaderFactory.createSAMFileReader(new File(splitFile3));
		for (@SuppressWarnings("unused") SAMRecord record : reader) splitFile3Count++;
		reader = SAMFileReaderFactory.createSAMFileReader(new File(splitFile4));
		for (@SuppressWarnings("unused") SAMRecord record : reader) splitFile4Count++;
		reader = SAMFileReaderFactory.createSAMFileReader(new File(splitFile5));
		for (@SuppressWarnings("unused") SAMRecord record : reader) splitFile5Count++;
		
		Assert.assertEquals(origFileCount, (splitFile1Count + splitFile2Count + splitFile3Count + splitFile4Count + splitFile5Count));
	}
	
	@Test
	public void testGetAllZCs() throws Exception {
		SamReader mergedReader =  SAMFileReaderFactory.createSAMFileReader(mergedFile);
		Set<Integer> zcsFromFile = new HashSet<Integer>();
		Collection<String> fileNamesFromFile = new LinkedHashSet<String>();
		for (SAMRecord record : mergedReader) {
			zcsFromFile.add(record.getIntegerAttribute("ZC"));
		}
		SAMFileHeader header = mergedReader.getFileHeader();
		for (SAMReadGroupRecord record : header.getReadGroups()) {
			String zc = record.getAttribute("ZC");
			String[] params = colonDelimitedPattern.split(zc);
			fileNamesFromFile.add(params[2]);
		}
		
		Split splitPass = new Split(mergedFile.getAbsolutePath(), tempFolder.getRoot().getAbsolutePath(), false, new SamSplitType());
		Assert.assertEquals(zcsFromFile, splitPass.getAllZcs());
		Assert.assertTrue(fileNamesFromFile.containsAll(splitPass.getOriginalFileNames()));
		Assert.assertTrue(splitPass.getOriginalFileNames().containsAll(fileNamesFromFile));
	}
	
	@Test
	public void testSplitUseZCNames() throws Exception {
		SamReader mergedReader;
		SamReader reader;
		
		// get some details from the merged file
		mergedReader = SAMFileReaderFactory.createSAMFileReader(mergedFile);
		int countA = 0, countB = 0, countC = 0;
		for (SAMRecord record : mergedReader) {
			switch (record.getIntegerAttribute("ZC")) {
			case 3:
				countC++; break;
			case 5:
				countA++; break;
			case 6:
				countB++; break;
			}
		}
		
		Split splitPass = new Split(mergedFile.getAbsolutePath(), tempFolder.getRoot().getAbsolutePath(), false, new SamSplitType());
		
		Integer zcA = splitPass.getZcFromOriginalFileName(tempFolder.getRoot().getAbsolutePath() + SEP + ORIG1);
		Integer zcB = splitPass.getZcFromOriginalFileName(tempFolder.getRoot().getAbsolutePath() + SEP + ORIG2);
		Integer zcC = splitPass.getZcFromOriginalFileName(tempFolder.getRoot().getAbsolutePath() + SEP + ORIG3);

		File splitFileA = new File(tempFolder.getRoot().getAbsolutePath() + SEP + zcA.toString() + ".sam");
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileA);
		int splitCountA = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountA++;
		}

		File splitFileB = new File(tempFolder.getRoot().getAbsolutePath() + SEP + zcB.toString() + ".sam");
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileB);
		int splitCountB = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountB++;
		}

		File splitFileC = new File(tempFolder.getRoot().getAbsolutePath() + SEP + zcC.toString() + ".sam");
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileC);
		int splitCountC = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountC++;
		}

		assert (countA == splitCountA);
		assert (countB == splitCountB);
		assert (countC == splitCountC);

		SamReader splitReaderA = SAMFileReaderFactory.createSAMFileReader(splitFileA);
		Iterator<SAMRecord> iterA = splitReaderA.iterator();
		mergedReader = SAMFileReaderFactory.createSAMFileReader(mergedFile);
		for (SAMRecord record : mergedReader) {
			if (5 == record.getIntegerAttribute("ZC")) {
				SAMRecord splitRecord = iterA.next();
				assert (record.getReadName().equals(splitRecord.getReadName()));
				assert (record.getFlags() == splitRecord.getFlags());
			}
		}

		SamReader splitReaderB = SAMFileReaderFactory.createSAMFileReader(splitFileB);
		Iterator<SAMRecord> iterB = splitReaderB.iterator();
		mergedReader = SAMFileReaderFactory.createSAMFileReader(mergedFile);
		for (SAMRecord record : mergedReader) {
			if (6 == record.getIntegerAttribute("ZC")) {
				SAMRecord splitRecord = iterB.next();
				assert (record.getReadName().equals(splitRecord.getReadName()));
				assert (record.getFlags() == splitRecord.getFlags());
			}
		}

		SamReader splitReaderC = SAMFileReaderFactory.createSAMFileReader(splitFileC);
		Iterator<SAMRecord> iterC = splitReaderC.iterator();
		mergedReader = SAMFileReaderFactory.createSAMFileReader(mergedFile);
		for (SAMRecord record : mergedReader) {
			if (3 == record.getIntegerAttribute("ZC")) {
				SAMRecord splitRecord = iterC.next();
				assert (record.getReadName().equals(splitRecord.getReadName()));
				assert (record.getFlags() == splitRecord.getFlags());
			}
		}
	}
	
	@Test
	public void testSplitUseFileNames() throws Exception {
		SamReader mergedReader;
		SamReader reader;
		
		// get some details from the merged file
		mergedReader = SAMFileReaderFactory.createSAMFileReader(mergedFile);
		int countA = 0, countB = 0, countC = 0;
		for (SAMRecord record : mergedReader) {
			switch (record.getIntegerAttribute("ZC")) {
			case 3:
				countC++; break;
			case 5:
				countA++; break;
			case 6:
				countB++; break;
			}
		}
		
		Split splitPass = new Split(mergedFile.getAbsolutePath(), tempFolder.getRoot().getAbsolutePath(), true, new SamSplitType());

		File splitFileA = new File(tempFolder.getRoot().getAbsolutePath() + SEP + ORIG1);
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileA);
		int splitCountA = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountA++;
		}

		File splitFileB = new File(tempFolder.getRoot().getAbsolutePath() + SEP + ORIG2);
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileB);
		int splitCountB = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountB++;
		}

		File splitFileC = new File(tempFolder.getRoot().getAbsolutePath() + SEP + ORIG3);
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileC);
		int splitCountC = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountC++;
		}

		assert (countA == splitCountA);
		assert (countB == splitCountB);
		assert (countC == splitCountC);

		SamReader splitReaderA = SAMFileReaderFactory.createSAMFileReader(splitFileA);
		Iterator<SAMRecord> iterA = splitReaderA.iterator();
		mergedReader = SAMFileReaderFactory.createSAMFileReader(mergedFile);
		for (SAMRecord record : mergedReader) {
			if (5 == record.getIntegerAttribute("ZC")) {
				SAMRecord splitRecord = iterA.next();
				assert (record.getReadName().equals(splitRecord.getReadName()));
				assert (record.getFlags() == splitRecord.getFlags());
			}
		}

		SamReader splitReaderB = SAMFileReaderFactory.createSAMFileReader(splitFileB);
		Iterator<SAMRecord> iterB = splitReaderB.iterator();
		mergedReader = SAMFileReaderFactory.createSAMFileReader(mergedFile);
		for (SAMRecord record : mergedReader) {
			if (6 == record.getIntegerAttribute("ZC")) {
				SAMRecord splitRecord = iterB.next();
				assert (record.getReadName().equals(splitRecord.getReadName()));
				assert (record.getFlags() == splitRecord.getFlags());
			}
		}

		SamReader splitReaderC = SAMFileReaderFactory.createSAMFileReader(splitFileC);
		Iterator<SAMRecord> iterC = splitReaderC.iterator();
		mergedReader = SAMFileReaderFactory.createSAMFileReader(mergedFile);
		for (SAMRecord record : mergedReader) {
			if (3 == record.getIntegerAttribute("ZC")) {
				SAMRecord splitRecord = iterC.next();
				assert (record.getReadName().equals(splitRecord.getReadName()));
				assert (record.getFlags() == splitRecord.getFlags());
			}
		}
	}
	
	private void createSamFile(File samFile, Map<Integer, String> zcFiles) throws IOException {
		OutputStream os = new FileOutputStream(samFile);
		PrintStream ps = new PrintStream(os);
		
		
		ps.println("@HD	VN:1.0	GO:none	SO:coordinate");
		ps.println("@SQ	SN:chr1	LN:249250621");
		for (Entry<Integer, String> entry : zcFiles.entrySet()) {
			ps.println("@RG	ID:ES	DS:rl=50	SM:ES	ZC:Z:" + entry.getKey() + ":" + entry.getValue());
		}

		ps.println("@PG	ID:SOLID-GffToSam	VN:1.4.3	ZC:Z:3");
		ps.println("@PG	ID:SOLID-GffToSam.1	VN:1.4.3	ZC:Z:5");
		ps.println("@PG	ID:SOLID-GffToSam.1.1	VN:1.4.3	ZC:Z:6");
		ps.println("@PG	ID:dc8c7858-fb5f-4cb5-8b68-b0ee27c3a9a5	ZC:Z:7	CL:blah");
		ps.println("@PG	ID:5d0b9a81-6ed6-4aee-9706-bb9d182535d4	ZC:Z:8	CL:blah");
		
		for (Entry<Integer, String> entry : zcFiles.entrySet()) {
			ps.println("1858_2026_1766	0	chr1	10148	255	40M10H	*	0	0	CCCTAACCCTAACCCTAACCCTAAAAATAACCTCCCCCTA	!DDDDD8DDDDCBDDDC2@DD0\"%%%%%6<C:'&&&D<)!	ZC:i:" + entry.getKey() + "	RG:Z:ES	CQ:Z:7733:>(180:9+8239+(9<)(&853%+,13()'&08%%	CS:Z:T2002301002301002301002320003301022000023");
			ps.println("1858_2026_1766	0	chr1	10148	255	40M10H	*	0	0	CCCTAACCCTAACCCTAACCCTAAAAATAACCTCCCCCTA	!DDDDD8DDDDCBDDDC2@DD0\"%%%%%6<C:'&&&D<)!	ZC:i:" + entry.getKey() + "	RG:Z:ES	CQ:Z:7733:>(180:9+8239+(9<)(&853%+,13()'&08%%	CS:Z:T2002301002301002301002320003301022000023");
			ps.println("229_1153_1818	16	chr1	10169	255	50M	*	0	0	NAACCCTAACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA	!1*>C\"\"\";><)6931*BD%%DA8C5>DDDDDDDDDDDDDDDDDDDDDD!	ZC:i:" + entry.getKey() + "	RG:Z:ES	CQ:Z:5?@><<9=B5:<:>;+<>>:-:99&04%=,%<>%&,(2%%8'5(5*:%&,	CS:Z:T00320010320010320010320010320000320010320100000100");
			ps.println("229_1153_1818	16	chr1	10169	255	50M	*	0	0	NAACCCTAACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA	!1*>C\"\"\";><)6931*BD%%DA8C5>DDDDDDDDDDDDDDDDDDDDDD!	ZC:i:" + entry.getKey() + "	RG:Z:ES	CQ:Z:5?@><<9=B5:<:>;+<>>:-:99&04%=,%<>%&,(2%%8'5(5*:%&,	CS:Z:T00320010320010320010320010320000320010320100000100");
			ps.println("2173_1119_350	0	chr1	10223	255	35M15H	*	0	0	AACCCTAACCCCTAACCCTAACCCTAAACCCTAAA	!DDDDDDDDDDDDDDDDA-D++D<9DD4?DA+;D!	ZC:i:" + entry.getKey() + "	RG:Z:ES	CQ:Z:;=@A;<>B+AB@;6=9?;''A+A8%5:4A?=%'55	CS:Z:T30100230100023010023000023000002300");
		}
		
		ps.close();
		os.close();
	}
}
