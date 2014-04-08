package org.qcmg.sig.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.illumina.IlluminaRecord;

public class SignatureUtilTest {
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testGetPatientFromFile() {
		assertEquals("AAAA_1234", SignatureUtil.getPatientFromFile(new File("/path/project/AAAA_1234/SNP_array/1234.txt")));
		assertEquals("AAAA_9999", SignatureUtil.getPatientFromFile(new File("/path/project/AAAA_9999/SNP_array/5555.txt")));
		assertEquals("AAAA_333", SignatureUtil.getPatientFromFile(new File("/path/project/AAAA_333/SNP_array/5555.txt")));
		assertEquals("UNKNOWN", SignatureUtil.getPatientFromFile(new File("/path/project/AAAA_33/SNP_array/5555.txt")));
	}
	
	@Test
	public void testGetPatternFromString() {
		try {
			SignatureUtil.getPatternFromString(null, null);
			Assert.fail("SHOULD have thrown an iae");
		} catch (IllegalArgumentException iae) {}
		try {
			SignatureUtil.getPatternFromString("blah", null);
			Assert.fail("SHOULD have thrown an iae");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(SignatureUtil.UNKNOWN, SignatureUtil.getPatternFromString("foo", "bar"));
		
		assertEquals("-ND_", SignatureUtil.getPatternFromString("-[A-Z]{2}_", "GGGG-ABMJ-26-ND_5760640025_R02C01.txt"));
		assertEquals("GGGG-ABMJ-20110329-26-ND", SignatureUtil.getPatternFromString("[A-Z]{4}-[A-Z]{4}-[0-9]{8}-[0-9]{2}-[A-Z]{2}", "GGGG-ABMJ-20110329-26-ND_5760640025_R02C01.txt"));
	}
	
	@Test
	public void testGetCoverageStringFromCharsAndInts() {
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('\u0000', '\u0000', 0, 0));
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 0, 0));
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'A', 0, 0));
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('X', 'Y', 0, 0));
		assertEquals(SignatureUtil.EMPTY_COVERAGE, SignatureUtil.getCoverageStringFromCharsAndInts('Z', 'Z', 0, 0));
		
		assertEquals(true, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 1, 0).contains("A:1,C:0,G:0,T:0,N:0,TOTAL:1"));
		assertEquals(true, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 1, 1).contains("A:1,C:1,G:0,T:0,N:0,TOTAL:2"));
		assertEquals(true, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 2, 1).contains("A:2,C:1,G:0,T:0,N:0,TOTAL:3"));
		assertEquals(true, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 2, 2).contains("A:2,C:2,G:0,T:0,N:0,TOTAL:4"));
		assertEquals(true, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 299, 2).contains("A:299,C:2,G:0,T:0,N:0,TOTAL:301"));
		assertEquals(true, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'C', 1, 201).contains("A:1,C:201,G:0,T:0,N:0,TOTAL:202"));
		assertEquals(true, SignatureUtil.getCoverageStringFromCharsAndInts('A', 'A', 1, 200).contains("A:201,C:0,G:0,T:0,N:0,TOTAL:201"));
	}
	
	@Test
	public void testLoadSignatureRatiosEmptyCoverage() throws Exception {
		File sigFile = testFolder.newFile("testLoadSignatureRatiosEmptyCoverage");
		// write no data
		writeSignatureFile(sigFile, "");
		assertEquals(true, SignatureUtil.loadSignatureRatios(sigFile).isEmpty());
		
		sigFile.delete();
		
		sigFile = testFolder.newFile("testLoadSignatureRatiosEmptyCoverage");
		// write no data
		writeSignatureFile(sigFile, "20	14370	rs6054257	G	A	29	PASS\t" + SignatureUtil.EMPTY_COVERAGE + "\n");
		assertEquals(true, SignatureUtil.loadSignatureRatios(sigFile).isEmpty());
	}
	
	@Test
	public void testLoadSignatureRatios() throws Exception {
		File sigFile = testFolder.newFile("testLoadSignatureRatios");
		
		// write some data - still empty map as need minimum of 10 coverage
		writeSignatureFile(sigFile, "20\t14370\trs6054257\tG\tA\t29\tPASS\tFULLCOV=A:1,C:1,G:1,T:1,N:1,TOTAL:5;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0\n");
		assertEquals(true, SignatureUtil.loadSignatureRatios(sigFile).isEmpty());
		
		sigFile.delete();
		
		sigFile = testFolder.newFile("testLoadSignatureRatios");
		
		// write some data - still empty map as need minimum of 10 coverage
		writeSignatureFile(sigFile, "20\t14370\trs6054257\tG\tA\t29\tPASS\tFULLCOV=A:2,C:2,G:2,T:2,N:2,TOTAL:10;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0\n");
		assertEquals(false, SignatureUtil.loadSignatureRatios(sigFile).isEmpty());
		assertEquals(1, SignatureUtil.loadSignatureRatios(sigFile).size());
	}
	
	@Test
	public void testGetDonorSnpChipData() throws Exception {
		String donor = "ABCD_1234";
		File donor1 = testFolder.newFile("testLoadSignatureRatios" + donor + "qsig.vcf");
		File donor2 = testFolder.newFile("testLoadSignatureRatiosblahqsig.vcf");
		
		// write some data - still empty map as need minimum of 10 coverage
		writeSignatureFile(donor1, "20\t14370\trs6054257\tG\tA\t29\tPASS\tFULLCOV=A:1,C:1,G:1,T:1,N:1,TOTAL:5;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0\n");
		assertEquals(true, SignatureUtil.loadSignatureRatios(donor1).isEmpty());
		writeSignatureFile(donor2, "20\t14370\trs6054257\tG\tA\t29\tPASS\tFULLCOV=A:1,C:1,G:1,T:1,N:1,TOTAL:5;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0\n");
		assertEquals(true, SignatureUtil.loadSignatureRatios(donor2).isEmpty());
		
		List<File> files = new ArrayList<File>();
		files.add(donor1);
		files.add(donor2);
		
		assertEquals(false, SignatureUtil.getDonorSnpChipData(donor, files).isEmpty());
		assertEquals(true, SignatureUtil.getDonorSnpChipData(donor, files).containsKey(donor1));
		assertEquals(false, SignatureUtil.getDonorSnpChipData(donor, files).containsKey(donor2));
		
	}
	
	@Test
	public void testGetExcludesFromFile() throws IOException {
		assertEquals(0, SignatureUtil.getEntriesFromExcludesFile(null).size());
		assertEquals(0, SignatureUtil.getEntriesFromExcludesFile("").size());
		
		File excludesFile = testFolder.newFile("excludesFile");
		assertEquals(0, SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).size());
		try (FileWriter writer = new FileWriter(excludesFile);){
			writer.write("excludesFile1");
		}
		
		assertEquals(1, SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).size());
		assertEquals("excludesFile1", SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).get(0));
		
		try (FileWriter writer = new FileWriter(excludesFile, true);){
			writer.write("excludesFile2");
		}
		assertEquals(1, SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).size());
		assertEquals("excludesFile1excludesFile2", SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).get(0));
		
		try (FileWriter writer = new FileWriter(excludesFile, true);){
			for (int i = 3 ; i <= 10 ; i++)
				writer.append("\nexcludesFile" + i);
		}
		
		assertEquals(9, SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).size());
		assertEquals("excludesFile10", SignatureUtil.getEntriesFromExcludesFile(excludesFile.getAbsolutePath()).get(8));
	}
	
	@Test
	public void testRemoveExcludedFilesFromList() {
		assertEquals(null, SignatureUtil.removeExcludedFilesFromList(null, null));
		assertEquals(0, SignatureUtil.removeExcludedFilesFromList(new ArrayList<File>(), null).size());
		assertEquals(0, SignatureUtil.removeExcludedFilesFromList(new ArrayList<File>(), new ArrayList<String>()).size());
		List<File> files = new ArrayList<>();
		files.add(new File("file1"));
		assertEquals(1, SignatureUtil.removeExcludedFilesFromList(files, null).size());
		files.add(new File("file2"));
		List<String> excludes = new ArrayList<>();
		assertEquals(2, SignatureUtil.removeExcludedFilesFromList(files, excludes).size());
		files.add(new File("file2"));
		excludes.add("file3");
		assertEquals(3, SignatureUtil.removeExcludedFilesFromList(files, excludes).size());
		files.add(new File("file3"));
		excludes.add("file4");
		assertEquals(3, SignatureUtil.removeExcludedFilesFromList(files, excludes).size());
		files.add(new File("file4"));
		assertEquals(3, SignatureUtil.removeExcludedFilesFromList(files, excludes).size());
	}
	
	@Test
	public void testRemoveClosedProjectFilesFromList() {
		assertEquals(null, SignatureUtil.removeClosedProjectFilesFromList(null, null));
		assertEquals(0, SignatureUtil.removeClosedProjectFilesFromList(new ArrayList<File>(), null).size());
		assertEquals(0, SignatureUtil.removeClosedProjectFilesFromList(new ArrayList<File>(), new ArrayList<String>()).size());
		
		List<File> files = new ArrayList<>();
		files.add(new File("/ABCD_1234/file1"));
		assertEquals(1, SignatureUtil.removeClosedProjectFilesFromList(files, null).size());
		files.add(new File("/ABCD_1234/file2"));
		List<String> excludes = new ArrayList<>();
		assertEquals(2, SignatureUtil.removeClosedProjectFilesFromList(files, excludes).size());
		files.add(new File("/ABCD_5678/file1"));
		excludes.add("ABCD_9999");
		assertEquals(3, SignatureUtil.removeClosedProjectFilesFromList(files, excludes).size());
		files.add(new File("/ABCD_9999/file1"));
		assertEquals(3, SignatureUtil.removeClosedProjectFilesFromList(files, excludes).size());
		files.add(new File("/ABCD_9999/file2"));
		assertEquals(3, SignatureUtil.removeClosedProjectFilesFromList(files, excludes).size());
		excludes.add("ABCD_1234");
		assertEquals(1, SignatureUtil.removeClosedProjectFilesFromList(files, excludes).size());
	}
	
	
	@Test
	public void testAddFileToCollection() {
		SignatureUtil.addFileToCollection(null, null, null);
		List<File> collection = new ArrayList<>();
		SignatureUtil.addFileToCollection(collection, null, null);
		assertEquals(0, collection.size());
		SignatureUtil.addFileToCollection(collection, null, new File("file1"));
		assertEquals(1, collection.size());
		SignatureUtil.addFileToCollection(collection, null, new File("file2"));
		assertEquals(2, collection.size());
		
		List<String> excludes = new ArrayList<>();
		SignatureUtil.addFileToCollection(collection, excludes, new File("file2"));
		assertEquals(3, collection.size());
		
		excludes.add("file2");
		SignatureUtil.addFileToCollection(collection, excludes, new File("file2"));
		assertEquals(3, collection.size());
	}
	
	
	@Test
	public void testPopulateSnpChipFilesList() throws Exception {
		try {
			SignatureUtil.populateSnpChipFilesList(null, null, null, (String[])null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			SignatureUtil.populateSnpChipFilesList(testFolder.getRoot().getAbsolutePath(), "", null, (String[])null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}

		File snpChipFile1 = testFolder.newFile("snpCHip1");
		assertEquals(1, SignatureUtil.populateSnpChipFilesList(snpChipFile1.getAbsolutePath(), "snpCHip1", null,  (String[])null).size());
		assertEquals(0, SignatureUtil.populateSnpChipFilesList(snpChipFile1.getAbsolutePath(), "snp", null,  (String[])null).size());
		assertEquals(0, SignatureUtil.populateSnpChipFilesList(snpChipFile1.getAbsolutePath(), "CH", null,  (String[])null).size());
		assertEquals(1, SignatureUtil.populateSnpChipFilesList(snpChipFile1.getAbsolutePath(), "ip1", null, (String[])null).size());
		assertEquals(0, SignatureUtil.populateSnpChipFilesList(snpChipFile1.getAbsolutePath(), "ip1", null, "snap").size());
		assertEquals(0, SignatureUtil.populateSnpChipFilesList(snpChipFile1.getAbsolutePath(), "ip1", null, "sn1p").size());
		assertEquals(1, SignatureUtil.populateSnpChipFilesList(snpChipFile1.getAbsolutePath(), "ip1", null, "snp").size());
		
	}
	
	@Test
	public void populateSnpChipWithGemms() throws Exception {
		File gemmFolder = testFolder.newFolder();
		// add some qsig files
		File.createTempFile("GEMM_0001", ".qsig.vcf", gemmFolder);
		File.createTempFile("GEMM_0002", ".qsig.vcf", gemmFolder);
		File.createTempFile("GEMM_0003", ".qsig.vcf", gemmFolder);
		
		List<File> snpChipFiles = SignatureUtil.populateSnpChipFilesList(gemmFolder.getAbsolutePath(), ".qsig.vcf", null, null);
		assertEquals(3, snpChipFiles.size());
		
	}
	
	public static void writeSignatureFile(File signatureFile, String data) throws IOException {
		try (FileWriter writer = new FileWriter(signatureFile);){
			// add header
			writer.write("##test vcf file for use in SignatureUtilTest\n#CHROM POS     ID        REF ALT    QUAL FILTER INFO\n");
			// add data
			writer.write(data);
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void invalidCoverageString() {
		SignatureUtil.decipherCoverageString(null);
	}
	@Test (expected=IllegalArgumentException.class)
	public void invalidCoverageString2() {
		SignatureUtil.decipherCoverageString("");
	}
	@Test (expected=IllegalArgumentException.class)
	public void invalidCoverageString3() {
		SignatureUtil.decipherCoverageString("blah-dee-blah");
	}
	
	@Test
	public void validCoverageString() {
		String coverage = "FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[0]);
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[1]);
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[2]);
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[3]);
		assertEquals(0, SignatureUtil.decipherCoverageString(coverage)[4]);
		coverage = "FULLCOV=A:1,C:2,G:3,T:4,N:0,TOTAL:10;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
		assertEquals(1, SignatureUtil.decipherCoverageString(coverage)[0]);
		assertEquals(2, SignatureUtil.decipherCoverageString(coverage)[1]);
		assertEquals(3, SignatureUtil.decipherCoverageString(coverage)[2]);
		assertEquals(4, SignatureUtil.decipherCoverageString(coverage)[3]);
		assertEquals(10, SignatureUtil.decipherCoverageString(coverage)[4]);
	}
	
	@Test
	public void unambiguousIlluminaRecord() {
		String [] illRecParams = new String[32];
		Arrays.fill(illRecParams, "0");
		 
		illRecParams[20] = "[A/C]";	// snp
		illRecParams[14] = "A";	// first allele call
		illRecParams[15] = "B";	// second allele call
		illRecParams[22] = "TOP";	// strand
		illRecParams[30] = "0.5";	// b-allele freq
		
		testResults(illRecParams, 0,1);
		
		illRecParams[20] = "[A/G]";	// snp
		testResults(illRecParams, 0,2);
		
		illRecParams[20] = "[T/C]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1,3);
		illRecParams[20] = "[T/G]";	// snp
		testResults(illRecParams, 2,3);
	}
	
	@Test
	public void ambiguousIlluminaRecordAB() {
		String [] illRecParams = new String[32];
		Arrays.fill(illRecParams, "0");
		
		illRecParams[14] = "A";	// first allele call
		illRecParams[15] = "B";	// second allele call
		illRecParams[30] = "0.5";	// b-allele freq
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 0,3);
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 0,3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 0,3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 0,3);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 1,2);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1,2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 1,2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1,2);
	}
	
	@Test
	public void ambiguousIlluminaRecordAA() {
		String [] illRecParams = new String[32];
		Arrays.fill(illRecParams, "0");
		
		illRecParams[14] = "A";	// first allele call
		illRecParams[15] = "A";	// second allele call
		illRecParams[30] = "0.5";	// b-allele freq
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 0);
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 0);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 1);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1);
	}
	
	@Test
	public void ambiguousIlluminaRecordBB() {
		String [] illRecParams = new String[32];
		Arrays.fill(illRecParams, "0");
		
		illRecParams[14] = "B";	// first allele call
		illRecParams[15] = "B";	// second allele call
		illRecParams[30] = "0.5";	// b-allele freq
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 3);
		
		illRecParams[20] = "[A/T]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 3);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 0);
		
		illRecParams[20] = "[T/A]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 0);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 2);
		
		illRecParams[20] = "[C/G]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 2);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "TOP";	// strand
		testResults(illRecParams, 1);
		
		illRecParams[20] = "[G/C]";	// snp
		illRecParams[22] = "BOT";	// strand
		testResults(illRecParams, 1);
	}
	
	private void testResults(String [] illRecParams, int ... positionsWithCoverage) {
		int coverageValue = 20 / positionsWithCoverage.length;
		
		IlluminaRecord illRec = new IlluminaRecord(illRecParams);
		String infoField = SignatureUtil.getCoverageStringForIlluminaRecord(illRec, new String[8], 20);
		int [] coverages = SignatureUtil.decipherCoverageString(infoField);
		
		for (int i = 0 ; i < coverages.length -1 ; i++) {
			if (Arrays.binarySearch(positionsWithCoverage, i) >= 0) {
				assertEquals(coverageValue, coverages[i]);
			} else {
				assertEquals(0, coverages[i]);
			}
		}
		assertEquals(20, coverages[4]);
		
	}

}
