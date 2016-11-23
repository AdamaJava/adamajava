package org.qcmg.bammerge;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.SamReader.AssertingIterator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordCoordinateComparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.split.SamSplitType;
import org.qcmg.split.Split;
import org.qcmg.testing.SamTestData;

public class FileMergerTest {
	public static final String OUTPUT_FILE_NAME = "output.sam";
	private static final String FILE_NAME_A = SamTestData.FILE_NAME_A;
	private static final String FILE_NAME_B = SamTestData.FILE_NAME_B;
	private static final String FILE_NAME_C = SamTestData.FILE_NAME_C;

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public final void before() {
		try {
			SamTestData.createAllTestFiles();
		} catch (Exception e) {
			System.err.println("File creation error in test harness: "
					+ e.getMessage());
		}
	}

	@After
	public final void after() {
		try {
			SamTestData.deleteAllTestFiles();
			File outputFile = new File(OUTPUT_FILE_NAME);
			outputFile.delete();
		} catch (Exception e) {
			System.err.println("File creation error in test harness: "
					+ e.getMessage());
		}
	}
	
	@Test
	public void largeData() throws Exception {
		File f1 = testFolder.newFile();
		File f2 = testFolder.newFile();
		File f3 = testFolder.newFile();
		File f = testFolder.newFolder();
		File fOut = new File(f.getAbsolutePath() + "output.sam");
		
		SamTestData.createFirstSam(f1, true);
		SamTestData.createSecondSam(f2, true);
		SamTestData.createThirdSam(f3, true);
		
		
		String[] args = { f1.getAbsolutePath(), f2.getAbsolutePath(), f3.getAbsolutePath() };
		long now = System.currentTimeMillis();
		new FileMerger(fOut.getAbsolutePath(), args, "commandLine", true);
		System.out.println("Time taken: " + (System.currentTimeMillis() - now));
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(f1);
		int countA = 0;
		for (SAMRecord record : reader) {
			countA++;
		}

		reader = SAMFileReaderFactory.createSAMFileReader(f2);
		int countB = 0;
		for (SAMRecord record : reader) {
			countB++;
		}
		reader = SAMFileReaderFactory.createSAMFileReader(f3);
		int countC = 0;
		for (SAMRecord record : reader) {
			countC++;
		}

		reader = SAMFileReaderFactory.createSAMFileReader(fOut);
		int countD = 0;
		for (SAMRecord record : reader) {
			countD++;
			Integer zc = record.getIntegerAttribute("ZC");
			assert (null != zc);
		}

		assertTrue(countD == countA + countB + countC);
	}
	
	@Test
	public void singleInput() throws Exception {
		File f1 = testFolder.newFile();
		SamTestData.createFirstSam(f1, false);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(f1);
		int countA = 0;
		for (SAMRecord record : reader) {
			countA++;
		}
		File f = testFolder.newFolder();
		File fOut = new File(f.getAbsolutePath() + "output.sam");
		String[] args = { f1.getAbsolutePath() };
		long now = System.currentTimeMillis();
		new FileMerger(fOut.getAbsolutePath(), args, null,"commandLine",-1, false,false, true, null,null, null, ",my_uuid");
		
		reader = SAMFileReaderFactory.createSAMFileReader(fOut);
		SAMFileHeader header = reader.getFileHeader();
		int countB = 0;
		for (SAMRecord record : reader) {
			countB++;
		}
		assertEquals(1, header.getComments().size());
		assertEquals(true, header.getComments().get(0).contains("my_uuid"));
		assertEquals(countB, countA);
		
		/*
		 * update uuid
		 */
		File fOut2 = new File(f.getAbsolutePath() + "output2.sam");
		new FileMerger(fOut2.getAbsolutePath(), new String[]{fOut.getAbsolutePath()}, null,"commandLine",-1, false,false, true, null,null, null, ",my_uuid_take_II");
		reader = SAMFileReaderFactory.createSAMFileReader(fOut2);
		header = reader.getFileHeader();
		int countC = 0;
		for (SAMRecord record : reader) {
			countC++;
		}
		assertEquals(1, header.getComments().size());
		assertEquals(true, header.getComments().get(0).contains("my_uuid_take_II"));
		assertEquals(countC, countA);
		
	}

	@Test
	public final void constructWithSameInputFileAsOutputFile() throws Exception {
		// Ensure we detect duplicate input files before detecting same input
		// and output files
		thrown.expect(Exception.class);
		thrown.expectMessage("Input file first.sam supplied more than once");

		String[] args = { FILE_NAME_A, FILE_NAME_A };
		new FileMerger(FILE_NAME_A, args, "commandLine", true);

		thrown.expect(Exception.class);
		thrown.expectMessage("File first.sam used both as input and output");

		String[] args2 = { FILE_NAME_A };
		new FileMerger(FILE_NAME_A, args2, "commandLine", true);
	}

	@Test
	public final void constructDuplicateInputFiles() throws Exception {
		thrown.expect(Exception.class);
		thrown.expectMessage("Input file first.sam supplied more than once");

		String[] args = { FILE_NAME_A, FILE_NAME_A };
		new FileMerger(OUTPUT_FILE_NAME, args, "commandLine", true);
	}

	@Test
	public final void constructWithValidArguments() throws Exception {
		ExpectedException.none();
		String[] args = { FILE_NAME_A, FILE_NAME_B };
		new FileMerger(OUTPUT_FILE_NAME, args, "commandLine", true);

		File fileA = new File(FILE_NAME_A);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(fileA);
		int countA = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countA++;
		}

		File fileB = new File(FILE_NAME_B);
		reader = SAMFileReaderFactory.createSAMFileReader(fileB);
		int countB = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countB++;
		}

		File fileC = new File(OUTPUT_FILE_NAME);
		reader = SAMFileReaderFactory.createSAMFileReader(fileC);
		int countC = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countC++;
			Integer zc = record.getIntegerAttribute("ZC");
			assert (null != zc);
		}

		assertTrue(countC == countA + countB);
	}

	@Test
	public final void constructWithValidArgumentsThenSplit() throws Exception {
		ExpectedException.none();
		String[] args = { FILE_NAME_A, FILE_NAME_B };
		new FileMerger(OUTPUT_FILE_NAME, args, "commandLine", true);

		File fileA = new File(FILE_NAME_A);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(fileA);
		int countA = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countA++;
		}

		File fileB = new File(FILE_NAME_B);
		reader = SAMFileReaderFactory.createSAMFileReader(fileB);
		int countB = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countB++;
		}

		Split splitPass = new Split(OUTPUT_FILE_NAME, ".", false, new SamSplitType());
		Integer zcA = splitPass.getZcFromOriginalFileName(fileA
				.getAbsolutePath());
		Integer zcB = splitPass.getZcFromOriginalFileName(fileB
				.getAbsolutePath());

		File splitFileA = new File(zcA.toString() + ".sam");
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileA);
		int splitCountA = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountA++;
		}

		File splitFileB = new File(zcB.toString() + ".sam");
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileB);
		int splitCountB = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountB++;
		}

		assert (countA == splitCountA);
		assert (countB == splitCountB);

		SamReader readerA = SAMFileReaderFactory.createSAMFileReader(fileA);
		SamReader splitReaderA = SAMFileReaderFactory.createSAMFileReader(splitFileA);
		Iterator<SAMRecord> iterA = splitReaderA.iterator();
		for (SAMRecord record : readerA) {
			SAMRecord splitRecord = iterA.next();
			assert (record.getReadName().equals(splitRecord.getReadName()));
			assert (record.getFlags() == splitRecord.getFlags());
		}

		SamReader readerB = SAMFileReaderFactory.createSAMFileReader(fileB);
		SamReader splitReaderB = SAMFileReaderFactory.createSAMFileReader(splitFileB);
		Iterator<SAMRecord> iterB = splitReaderB.iterator();
		for (SAMRecord record : readerB) {
			SAMRecord splitRecord = iterB.next();
			assert (record.getReadName().equals(splitRecord.getReadName()));
			assert (record.getFlags() == splitRecord.getFlags());
		}

		splitFileA.delete();
		splitFileB.delete();
	}

	@Test
	public final void recursiveMergeWithValidArguments() throws Exception {
		ExpectedException.none();

		String[] replacements = { FILE_NAME_B + ":ES:XXX" };
		
		File output = new File("recursiveMergeWithValidArguments.sam");
		if (output.exists()) {
			output.delete();
		}

		String[] args = { FILE_NAME_A, FILE_NAME_B };
		new FileMerger(output.getAbsolutePath(), args, replacements, "commandLine", true);

		String[] args2 = {output.getAbsolutePath(), FILE_NAME_B };
		new FileMerger(OUTPUT_FILE_NAME, args2, "commandLine", true);

		File fileA = new File(FILE_NAME_A);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(fileA);
		int countA = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countA++;
		}

		File fileB = new File(FILE_NAME_B);
		reader = SAMFileReaderFactory.createSAMFileReader(fileB);
		int countB = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countB++;
		}

		File fileC = new File(FILE_NAME_B);
		reader = SAMFileReaderFactory.createSAMFileReader(fileC);
		int countC = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countC++;
		}

		File outputFile = new File(OUTPUT_FILE_NAME);
		reader = SAMFileReaderFactory.createSAMFileReader(outputFile);
		int outputCount = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			outputCount++;
			Integer zc = record.getIntegerAttribute("ZC");
			assert (null != zc);
		}

		assertTrue(outputCount == countA + 2 * countB);

		output.delete();
	}

	@Test
	public final void recursiveMergeWithValidArgumentsThenSplit()
			throws Exception {
		ExpectedException.none();
		
		String[] replacements = { FILE_NAME_B + ":ES:XXX" };
		
		File output = new File("recursiveMergeWithValidArgumentsThenSplit.sam");
		if (output.exists()) {
			output.delete();
		}
		String[] args = { FILE_NAME_A, FILE_NAME_B };
		new FileMerger(output.getAbsolutePath(), args, replacements, "commandLine", true);

		String[] args2 = { output.getAbsolutePath(), FILE_NAME_C };
		new FileMerger(OUTPUT_FILE_NAME, args2, "commandLine", true);

		File fileA = new File(FILE_NAME_A);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(fileA);
		int countA = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countA++;
		}

		File fileB = new File(FILE_NAME_B);
		reader = SAMFileReaderFactory.createSAMFileReader(fileB);
		int countB = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countB++;
		}

		File fileC = new File(FILE_NAME_C);
		reader = SAMFileReaderFactory.createSAMFileReader(fileC);
		int countC = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			countC++;
		}

		File outputFile = new File(OUTPUT_FILE_NAME);
		reader = SAMFileReaderFactory.createSAMFileReader(outputFile);
		int outputCount = 0;
		for (SAMRecord record : reader) {
			outputCount++;
			Integer zc = record.getIntegerAttribute("ZC");
			assert (null != zc);
		}

		assertTrue(outputCount == countA + countB + countC);

		output.delete();

		Split splitPass = new Split(OUTPUT_FILE_NAME, ".", false, new SamSplitType());
		Integer zcA = splitPass.getZcFromOriginalFileName(fileA
				.getAbsolutePath());
		Integer zcB = splitPass.getZcFromOriginalFileName(fileB
				.getAbsolutePath());
		Integer zcC = splitPass.getZcFromOriginalFileName(fileC
				.getAbsolutePath());

		File splitFileA = new File(zcA.toString() + ".sam");
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileA);
		int splitCountA = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountA++;
		}

		File splitFileB = new File(zcB.toString() + ".sam");
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileB);
		int splitCountB = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountB++;
		}

		File splitFileC = new File(zcC.toString() + ".sam");
		reader = SAMFileReaderFactory.createSAMFileReader(splitFileC);
		int splitCountC = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			splitCountC++;
		}

		assert (countA == splitCountA);
		assert (countB == splitCountB);
		assert (countC == splitCountC);

		SamReader readerA = SAMFileReaderFactory.createSAMFileReader(fileA);
		SamReader splitReaderA = SAMFileReaderFactory.createSAMFileReader(splitFileA);
		Iterator<SAMRecord> iterA = splitReaderA.iterator();
		for (SAMRecord record : readerA) {
			SAMRecord splitRecord = iterA.next();
			assert (record.getReadName().equals(splitRecord.getReadName()));
			assert (record.getFlags() == splitRecord.getFlags());
		}

		SamReader readerB = SAMFileReaderFactory.createSAMFileReader(fileB);
		SamReader splitReaderB = SAMFileReaderFactory.createSAMFileReader(splitFileB);
		Iterator<SAMRecord> iterB = splitReaderB.iterator();
		for (SAMRecord record : readerB) {
			SAMRecord splitRecord = iterB.next();
			assert (record.getReadName().equals(splitRecord.getReadName()));
			assert (record.getFlags() == splitRecord.getFlags());
		}

		SamReader readerC = SAMFileReaderFactory.createSAMFileReader(fileC);
		SamReader splitReaderC = SAMFileReaderFactory.createSAMFileReader(splitFileC);
		Iterator<SAMRecord> iterC = splitReaderC.iterator();
		for (SAMRecord record : readerC) {
			SAMRecord splitRecord = iterC.next();
			assert (record.getReadName().equals(splitRecord.getReadName()));
			assert (record.getFlags() == splitRecord.getFlags());
		}

		splitFileA.delete();
		splitFileB.delete();
		splitFileC.delete();
	}

	@Test
	public final void constructWithNoInputFiles() throws Exception {
		thrown.expect(Exception.class);
		thrown.expectMessage("Insufficient input files");

		String[] args = {};
		new FileMerger(OUTPUT_FILE_NAME, args, "", true);
	}

	@Test
	public final void constructWithNonexistentInputFiles() throws Exception {
		thrown.expect(Exception.class);
		thrown.expectMessage("Nonexistent input file somefilename");

		String[] args = { "somefilename" };
		new FileMerger(OUTPUT_FILE_NAME, args, "", true);
	}

	@Test
	public final void constructWithNullInputFiles() throws Exception {
		thrown.expect(Exception.class);
		thrown.expectMessage("Insufficient input files");

		String[] args = null;
		new FileMerger(OUTPUT_FILE_NAME, args, "commandLine", true);
	}

	@Test
	public final void constructWithExistingOutputFile() throws Exception {
		ExpectedException.none();
		String[] args = { FILE_NAME_A, FILE_NAME_B };
		new FileMerger(OUTPUT_FILE_NAME, args, "commandLine", true);

		thrown.expect(Exception.class);
		thrown.expectMessage("Cannot overwrite existing output file");
		new FileMerger(OUTPUT_FILE_NAME, args, "commandLine", true);
	}

	@Test
	public final void constructWithOutputFileIsDirectory() throws Exception {
		ExpectedException.none();
		String[] args = { FILE_NAME_A, FILE_NAME_B };
		new FileMerger(OUTPUT_FILE_NAME, args, "", true);

		thrown.expect(Exception.class);
		thrown.expectMessage("Cannot overwrite existing output file");
		new FileMerger(OUTPUT_FILE_NAME, args, "commandLine", true);
	}

	@Test
	public final void constructWithReadGroupClash() throws Exception {
		thrown.expect(Exception.class);
		thrown.expectMessage("Read group overlap RG lines: @RG	ID:ES	DS:rl=50	SM:ES and @RG	ID:ES	DS:rl=50	SM:ES");

		File file = new File(FILE_NAME_A);
		File copiedFile = new File("copied_file.sam");
		FileReader in = new FileReader(file);
		FileWriter out = new FileWriter(copiedFile);
		int c;
		while ((c = in.read()) != -1) {
			out.write(c);
		}
		in.close();
		out.close();

		String[] args = { FILE_NAME_A, "copied_file.sam" };
		new FileMerger(OUTPUT_FILE_NAME, args, "commandLine", false);

		copiedFile.delete();
	}

	@Test
	public final void constructWithReadGroupSubstition() throws Exception {
		ExpectedException.none();

		String[] replacements = { "copied_file.sam:ES:ESX" };

		copyFile(FILE_NAME_A, "copied_file.sam");

		String[] args = { FILE_NAME_A, "copied_file.sam" };
		new FileMerger(OUTPUT_FILE_NAME, args, replacements, "commandLine", false);

		deleteFile("copied_file.sam");

		File outputFile = new File(OUTPUT_FILE_NAME);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader( outputFile) ;//new SAMFileReader(outputFile);
		Iterator<SAMRecord> iter = reader.iterator();

		assertTrue(iter.hasNext());
		SAMRecord nextRecord = iter.next();

		SAMRecordCoordinateComparator comparator = new SAMRecordCoordinateComparator();

		while (null != nextRecord) {
			int esCount = 0;
			int esxCount = 0;

			boolean flag = true;
			Vector<SAMRecord> nextRecords = new Vector<SAMRecord>();
			while (flag) {
				nextRecords.add(nextRecord);
				SAMRecord previousRecord = nextRecord;
				if (iter.hasNext()) {
					nextRecord = iter.next();
					if (0 != comparator.fileOrderCompare(nextRecord,
							previousRecord)) {
						flag = false;
					}
				} else {
					nextRecord = null;
					flag = false;
				}
			}

			assertTrue(0 != nextRecords.size());
			assertTrue(0 == nextRecords.size() % 2);

			for (SAMRecord record : nextRecords) {
				String readGroup = record.getAttribute("RG").toString();
				if (0 == readGroup.compareTo("ES")) {
					esCount++;
				} else if (0 == readGroup.compareTo("ESX")) {
					esxCount++;
				} else {
					// Should never reach here as either ES or ESX.
					assertFalse(false);
				}
			}

			assertTrue(esCount == esxCount);
		}
	}

	public final void deleteFile(final String fileName) throws IOException {
		File file = new File(fileName);
		file.delete();
	}

	public final void copyFile(final String fromFileName,
			final String toFileName) throws IOException {
		File file = new File(fromFileName);
		File copiedFile = new File(toFileName);
		FileReader in = new FileReader(file);
		FileWriter out = new FileWriter(copiedFile);
		int c;
		while ((c = in.read()) != -1) {
			out.write(c);
		}
		in.close();
		out.close();
	}

	@Test
	public final void constructWithClashingOldReplacementGroups1()
			throws Exception {
		thrown.expect(Exception.class);
		thrown
				.expectMessage("Identical old group ES for replacements copied_file.sam:ES:ESX and copied_file.sam:ES:ESZ");

		String[] replacements = {"copied_file.sam:ES:ESX", "copied_file.sam:ES:ESZ"};

		copyFile(FILE_NAME_A, "copied_file.sam");

		String[] args = { FILE_NAME_A, "copied_file.sam" };
		new FileMerger(OUTPUT_FILE_NAME, args, replacements, "commandLine", false);

		deleteFile("copied_file.sam");
	}

	@Test
	public final void constructWithClashingOldReplacementGroups2()
			throws Exception {
		// We detect that old group clashes with precedence to new group
		// clashes.
		thrown.expect(Exception.class);
		thrown
				.expectMessage("Identical old group ES for replacements copied_file.sam:ES:ESX and copied_file.sam:ES:ESX");

		String[] replacements = {"copied_file.sam:ES:ESX", "copied_file.sam:ES:ESX"};
		copyFile(FILE_NAME_A, "copied_file.sam");

		String[] args = { FILE_NAME_A, "copied_file.sam" };
		new FileMerger(OUTPUT_FILE_NAME, args, replacements, "commandLine", false);

		deleteFile("copied_file.sam");
	}

	@Test
	public final void constructWithClashingNewReplacementGroups1()
			throws Exception {
		thrown.expect(Exception.class);
		thrown
				.expectMessage("Identical new group ESZ for replacements copied_file.sam:EST:ESZ and copied_file.sam:ES:ESZ");

		String[] replacements = {"copied_file.sam:EST:ESZ", "copied_file.sam:ES:ESZ"};
		copyFile(FILE_NAME_A, "copied_file.sam");

		String[] args = { FILE_NAME_A, "copied_file.sam" };
		new FileMerger(OUTPUT_FILE_NAME, args, replacements, "commandLine", false);

		deleteFile("copied_file.sam");
	}

	@Test
	public final void constructWithClashingNewReplacementGroups2()
			throws Exception {
		thrown.expect(Exception.class);
		thrown
				.expectMessage("Identical new group ESZ for replacements first.sam:EST:ESZ and copied_file.sam:ES:ESZ");

		String[] replacements = {FILE_NAME_A
				+ ":EST:ESZ", "copied_file.sam:ES:ESZ"};
		copyFile(FILE_NAME_A, "copied_file.sam");

		String[] args = { FILE_NAME_A, "copied_file.sam" };
		new FileMerger(OUTPUT_FILE_NAME, args, replacements, "commandLine", false);

		deleteFile("copied_file.sam");
	}

	@Test
	public final void constructWithBadReplacementGroupFileName1()
			throws Exception {
		thrown.expect(Exception.class);
		thrown
				.expectMessage("Group replacement XXXXX.sam:ES:ESZ specifies unknown input file XXXXX.sam");
		String[] replacements = {FILE_NAME_A
				+ ":EST:ESZ", "XXXXX.sam:ES:ESZ"};

		copyFile(FILE_NAME_A, "copied_file.sam");

		String[] args = { FILE_NAME_A, "copied_file.sam" };
		new FileMerger(OUTPUT_FILE_NAME, args, replacements, "commandLine", false);

		deleteFile("copied_file.sam");
	}

	@Test
	public final void constructWithBadReplacementGroupFileName2()
			throws Exception {
		// We detect unknown replacement group filename with precedence over bad
		// old/new groups
		thrown.expect(Exception.class);
		thrown
				.expectMessage("Group replacement XXXXX.sam:ES:ESZ specifies unknown input file XXXXX.sam");

		String[] replacements = {"XXXXX.sam:ES:ESZ","XXXXX.sam:ES:ESZ"};
		copyFile(FILE_NAME_A, "copied_file.sam");

		String[] args = { FILE_NAME_A, "copied_file.sam" };
		new FileMerger(OUTPUT_FILE_NAME, args, replacements, "commandLine", false);

		deleteFile("copied_file.sam");
	}
}
