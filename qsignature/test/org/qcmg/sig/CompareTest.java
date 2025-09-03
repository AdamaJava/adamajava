package org.qcmg.sig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.sig.util.SignatureUtilTest;


public class CompareTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void singleEmptyInputFile() throws Exception {
		File logF = testFolder.newFile();
		File inputF = testFolder.newFile("blah.qsig.vcf.gz");
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + inputF.getParent());
		assertEquals(0, exec.getErrCode());		// all ok
	}
	
	@Test
	public void nonEmptyInputFiles() throws Exception {
		File logF = testFolder.newFile();
		File f1 = testFolder.newFile("blah.qsig.vcf");
		File f2 = testFolder.newFile("blah2.qsig.vcf");
		File o = testFolder.newFile();
		
		writeVcfFile(f1);
		writeVcfFile(f2);
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + f1.getParent() + " -o " + o.getAbsolutePath());
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		assertEquals(11, Files.readAllLines(Paths.get(o.getAbsolutePath())).size());		// 11 lines means 1 comparison
	}
	
	@Test
	public void nonEmptyAndEmptyInputFiles() throws Exception {
		File logF = testFolder.newFile();
		File f1 = testFolder.newFile("blah.qsig.vcf");
		File f2 = testFolder.newFile("blah2.qsig.vcf");
		File f3 = testFolder.newFile("blah3.qsig.vcf");
		File o = testFolder.newFile();
		
		writeVcfFile(f1);
		writeVcfFileHeader(f2);
		writeVcfFile(f3);
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + f1.getParent() + " -o " + o.getAbsolutePath());
        assertEquals(0, exec.getErrCode());		// all ok
		assertTrue(o.exists());
		List<String> outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(14, outputData.size());		// 13 lines means 3 comparison
		for (int i = 0 ; i < 14 ; i++) {
			System.out.println(outputData.get(i));
		}
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"0\" score=\"NaN\"/>", outputData.get(9).trim());		// file 2 is empty
		assertEquals("<comparison file1=\"2\" file2=\"3\" overlap=\"0\" score=\"NaN\"/>", outputData.get(11).trim());		// file 2 is empty
	}
	
	@Test
	public void diffMd5InputFiles() throws Exception {
		File logF = testFolder.newFile();
		File f1 = testFolder.newFile("blah.qsig.vcf");
		File f2 = testFolder.newFile("blah2.qsig.vcf");
		File o = testFolder.newFile();
		
		writeVcfFile(f1, "##positions_md5sum=d18c99f481afbe04294d11deeb418890\n");
		writeVcfFile(f2, "##positions_md5sum=d18c99f481afbe04294d11deeb418890XXX\n");
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + f1.getParent() + " -o " + o.getAbsolutePath());
		assertEquals(0, exec.getErrCode());		// all ok
		assertTrue(o.exists());
		assertEquals(9, Files.readAllLines(Paths.get(o.getAbsolutePath())).size());		// 9 lines means 0 comparison
	}
	
	@Test
	public void bespokeVsTraditionalBAM() throws IOException, InterruptedException {
		File logF = testFolder.newFile();
		File bespoke = testFolder.newFile("bespoke.qsig.vcf");
		File trad = testFolder.newFile("traditional.qsig.vcf");
		File o = testFolder.newFile();
		List<String> tradData = new ArrayList<>(SignatureUtilTest.BAM_HEADER_OLD_SKOOL);
		tradData.addAll(Arrays.asList("chr1\t99236\t cnvi0131297\t T\t.\t.\t.\tFULLCOV=A:0,C:0,G:0,T:101,N:0,TOTAL:101;NOVELCOV=A:0,C:0,G:0,T:71,N:0,TOTAL:71",
						"chr1\t100622\tcnvi0147523\tG\t.\t.\t.\tFULLCOV=A:0,C:0,G:55,T:0,N:0,TOTAL:55;NOVELCOV=A:0,C:0,G:49,T:0,N:0,TOTAL:49",
						"chr1\t101095\tcnvi0133071\tT\t.\t.\t.\tFULLCOV=A:0,C:0,G:0,T:49,N:0,TOTAL:49;NOVELCOV=A:0,C:0,G:0,T:42,N:0,TOTAL:42",
						"chr1\t102954\tcnvi0120648\tT\t.\t.\t.\tFULLCOV=A:0,C:0,G:0,T:337,N:0,TOTAL:337;NOVELCOV=A:0,C:0,G:0,T:167,N:0,TOTAL:167",
						"chr1\t104813\tcnvi0124605\tG\t.\t.\t.\tFULLCOV=A:0,C:0,G:86,T:1,N:0,TOTAL:87;NOVELCOV=A:0,C:0,G:60,T:1,N:0,TOTAL:61",
						"chr1\t106222\tcnvi0146646\tT\t.\t.\t.\tFULLCOV=A:0,C:0,G:1,T:32,N:0,TOTAL:33;NOVELCOV=A:0,C:0,G:1,T:29,N:0,TOTAL:30",
						"chr1\t113422\tcnvi0147530\tT\t.\t.\t.\tFULLCOV=A:0,C:1,G:0,T:158,N:0,TOTAL:159;NOVELCOV=A:0,C:1,G:0,T:112,N:0,TOTAL:113"));
		List<String> bespokeData = new ArrayList<>(SignatureUtilTest.BAM_HEADER);
		bespokeData.addAll(Arrays.asList("chr1\t99236\t.\tT\t.\t.\t.\tQAF=t:0-0-0-34,rg1:0-0-0-20,rg2:0-0-0-14",
						"chr1\t101095\t.\tT\t.\t.\t.\tQAF=t:0-0-0-20,rg1:0-0-0-14,rg2:0-0-0-6",
						"chr1\t102954\t.\tT\t.\t.\t.\tQAF=t:0-0-1-161,rg1:0-0-1-71,rg2:0-0-0-90",
						"chr1\t104813\t.\tG\t.\t.\t.\tQAF=t:0-0-9-0,rg1:0-0-4-0,rg2:0-0-5-0",
						"chr1\t113422\t.\tT\t.\t.\t.\tQAF=t:0-0-0-13,rg1:0-0-0-7,rg2:0-0-0-6"));
		
		writeDataToFile(bespokeData, bespoke);
		writeDataToFile(tradData, trad);
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + bespoke.getParent() + " -o " + o.getAbsolutePath());
		assertEquals(0, exec.getErrCode());		// all ok
		assertTrue(o.exists());
		List<String> outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison		
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"4\" score=\"1.0\"/>", outputData.get(8).trim());
	}
	
	@Test
	public void traditionalVsTraditionalBAM() throws IOException, InterruptedException {
		File logF = testFolder.newFile();
		File trad1 = testFolder.newFile("trad1.qsig.vcf");
		File trad2 = testFolder.newFile("trad2.qsig.vcf");
		File o = testFolder.newFile();
		List<String> trad1Data = new ArrayList<>(SignatureUtilTest.BAM_HEADER_OLD_SKOOL);
		trad1Data.addAll(Arrays.asList("chr1\t99236\t cnvi0131297\t T\t.\t.\t.\tFULLCOV=A:0,C:0,G:0,T:101,N:0,TOTAL:101;NOVELCOV=A:0,C:0,G:0,T:71,N:0,TOTAL:71",
				"chr1\t100622\tcnvi0147523\tG\t.\t.\t.\tFULLCOV=A:0,C:0,G:55,T:0,N:0,TOTAL:55;NOVELCOV=A:0,C:0,G:49,T:0,N:0,TOTAL:49",
				"chr1\t101095\tcnvi0133071\tT\t.\t.\t.\tFULLCOV=A:0,C:0,G:0,T:49,N:0,TOTAL:49;NOVELCOV=A:0,C:0,G:0,T:42,N:0,TOTAL:42",
				"chr1\t102954\tcnvi0120648\tT\t.\t.\t.\tFULLCOV=A:0,C:0,G:0,T:337,N:0,TOTAL:337;NOVELCOV=A:0,C:0,G:0,T:167,N:0,TOTAL:167",
				"chr1\t104813\tcnvi0124605\tG\t.\t.\t.\tFULLCOV=A:0,C:0,G:86,T:1,N:0,TOTAL:87;NOVELCOV=A:0,C:0,G:60,T:1,N:0,TOTAL:61",
				"chr1\t106222\tcnvi0146646\tT\t.\t.\t.\tFULLCOV=A:0,C:0,G:1,T:32,N:0,TOTAL:33;NOVELCOV=A:0,C:0,G:1,T:29,N:0,TOTAL:30",
				"chr1\t113422\tcnvi0147530\tT\t.\t.\t.\tFULLCOV=A:0,C:1,G:0,T:158,N:0,TOTAL:159;NOVELCOV=A:0,C:1,G:0,T:112,N:0,TOTAL:113"));
		List<String> trad2Data = new ArrayList<>(SignatureUtilTest.BAM_HEADER_OLD_SKOOL);
		trad2Data.addAll(Arrays.asList("chr1\t99236\t cnvi0131297\t T\t.\t.\t.\tFULLCOV=A:0,C:0,G:0,T:101,N:0,TOTAL:101;NOVELCOV=A:0,C:0,G:0,T:71,N:0,TOTAL:71",
				"chr1\t100622\tcnvi0147523\tG\t.\t.\t.\tFULLCOV=A:0,C:0,G:55,T:0,N:0,TOTAL:55;NOVELCOV=A:0,C:0,G:49,T:0,N:0,TOTAL:49",
				"chr1\t101095\tcnvi0133071\tT\t.\t.\t.\tFULLCOV=A:0,C:0,G:0,T:49,N:0,TOTAL:49;NOVELCOV=A:0,C:0,G:0,T:42,N:0,TOTAL:42",
				"chr1\t102954\tcnvi0120648\tT\t.\t.\t.\tFULLCOV=A:0,C:0,G:0,T:337,N:0,TOTAL:337;NOVELCOV=A:0,C:0,G:0,T:167,N:0,TOTAL:167",
				"chr1\t104813\tcnvi0124605\tG\t.\t.\t.\tFULLCOV=A:0,C:0,G:86,T:1,N:0,TOTAL:87;NOVELCOV=A:0,C:0,G:60,T:1,N:0,TOTAL:61",
				"chr1\t106222\tcnvi0146646\tT\t.\t.\t.\tFULLCOV=A:0,C:0,G:1,T:32,N:0,TOTAL:33;NOVELCOV=A:0,C:0,G:1,T:29,N:0,TOTAL:30",
				"chr1\t113422\tcnvi0147530\tT\t.\t.\t.\tFULLCOV=A:0,C:1,G:0,T:158,N:0,TOTAL:159;NOVELCOV=A:0,C:1,G:0,T:112,N:0,TOTAL:113"));
		
		writeDataToFile(trad1Data, trad1);
		writeDataToFile(trad2Data, trad2);
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + trad1.getParent() + " -o " + o.getAbsolutePath());
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		List<String> outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"7\" score=\"1.0\"/>", outputData.get(8).trim());
	}
	
	@Test
	public void bespokeVsBespokeBAM() throws IOException, InterruptedException {
		File logF = testFolder.newFile();
		File bespoke1 = testFolder.newFile("bespoke1.qsig.vcf");
		File bespoke2 = testFolder.newFile("bespoke2.qsig.vcf");
		File o = testFolder.newFile();
		List<String> bespoke1Data = new ArrayList<>(SignatureUtilTest.BAM_HEADER);
		bespoke1Data.addAll(Arrays.asList("chr1\t99236\t.\tT\t.\t.\t.\tQAF=t:0-0-0-34,rg1:0-0-0-20,rg2:0-0-0-14",
				"chr1\t101095\t.\tT\t.\t.\t.\tQAF=t:0-0-0-20,rg1:0-0-0-14,rg2:0-0-0-6",
				"chr1\t102954\t.\tT\t.\t.\t.\tQAF=t:0-0-1-161,rg1:0-0-1-71,rg2:0-0-0-90",
				"chr1\t104813\t.\tG\t.\t.\t.\tQAF=t:0-0-19-0,rg1:0-0-14-0,rg2:0-0-5-0",
				"chr1\t113422\t.\tT\t.\t.\t.\tQAF=t:0-0-0-23,rg1:0-0-0-17,rg2:0-0-0-6"));
		List<String> bespoke2Data = new ArrayList<>(SignatureUtilTest.BAM_HEADER);
		bespoke2Data.addAll(Arrays.asList("chr1\t99236\t.\tT\t.\t.\t.\tQAF=t:0-0-0-34,rg1:0-0-0-20,rg2:0-0-0-14",
				"chr1\t101095\t.\tT\t.\t.\t.\tQAF=t:0-0-0-20,rg1:0-0-0-14,rg2:0-0-0-6",
				"chr1\t102954\t.\tT\t.\t.\t.\tQAF=t:0-0-1-161,rg1:0-0-1-71,rg2:0-0-0-90",
				"chr1\t104813\t.\tG\t.\t.\t.\tQAF=t:0-0-19-0,rg1:0-0-14-0,rg2:0-0-5-0",
				"chr1\t113422\t.\tT\t.\t.\t.\tQAF=t:0-0-0-23,rg1:0-0-0-17,rg2:0-0-0-6"));
		
		writeDataToFile(bespoke1Data, bespoke1);
		writeDataToFile(bespoke2Data, bespoke2);
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + bespoke1.getParent() + " -o " + o.getAbsolutePath());
		assertEquals(0, exec.getErrCode());		// all ok
		assertTrue(o.exists());
		List<String> outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"5\" score=\"1.0\"/>", outputData.get(8).trim());
	}
	
	@Test
	public void testCutoffs() throws IOException, InterruptedException {
		File logF = testFolder.newFile();
		File cutoff1 = testFolder.newFile("testCutoff1.qsig.vcf");
		File cutoff2 = testFolder.newFile("testCutoff2.qsig.vcf");
		File o = testFolder.newFile();
		List<String> cutoff1Data = new ArrayList<>(SignatureUtilTest.BAM_HEADER);
		cutoff1Data.addAll(Arrays.asList("chr1\t99236\t.\tT\t.\t.\t.\tQAF=t:0-0-0-34,rg1:0-0-0-20,rg2:0-0-0-14",
				"chr1\t101095\t.\tT\t.\t.\t.\tQAF=t:0-0-0-20,rg1:0-0-0-14,rg2:0-0-0-6",
				"chr1\t102954\t.\tT\t.\t.\t.\tQAF=t:0-0-1-161,rg1:0-0-1-71,rg2:0-0-0-90",
				"chr1\t104813\t.\tG\t.\t.\t.\tQAF=t:0-0-19-0,rg1:0-0-14-0,rg2:0-0-5-0",
				"chr1\t113422\t.\tT\t.\t.\t.\tQAF=t:0-0-0-23,rg1:0-0-0-17,rg2:0-0-0-6"));
		List<String> cutoff2Data = new ArrayList<>(SignatureUtilTest.BAM_HEADER);
		cutoff2Data.addAll(Arrays.asList("chr1\t99236\t.\tT\t.\t.\t.\tQAF=t:0-0-0-34,rg1:0-0-0-20,rg2:0-0-0-14",
				"chr1\t101095\t.\tT\t.\t.\t.\tQAF=t:0-0-0-20,rg1:0-0-0-14,rg2:0-0-0-6",
				"chr1\t102954\t.\tT\t.\t.\t.\tQAF=t:0-0-21-141,rg1:0-0-21-51,rg2:0-0-0-90",
				"chr1\t104813\t.\tG\t.\t.\t.\tQAF=t:0-14-19-0,rg1:0-14-14-0,rg2:0-0-5-0",
				"chr1\t113422\t.\tT\t.\t.\t.\tQAF=t:0-0-0-23,rg1:0-0-0-17,rg2:0-0-0-6"));
		
		writeDataToFile(cutoff1Data, cutoff1);
		writeDataToFile(cutoff2Data, cutoff2);
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + cutoff1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 0");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		List<String> outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"4\" score=\"0.75\"/>", outputData.get(8).trim());
		
		exec = execute("--log " + logF.getAbsolutePath() + " -d " + cutoff1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 0 --homCutoff 0.99 --hetLowerCutoff 0.49 --hetUpperCutoff 0.51");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.99\" lower_het=\"0.49\" upper_het=\"0.51\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"3\" score=\"1.0\"/>", outputData.get(8).trim());
		
		exec = execute("--log " + logF.getAbsolutePath() + " -d " + cutoff1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 0 --homCutoff 0.85");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.85\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"5\" score=\"0.8\"/>", outputData.get(8).trim());
		
		exec = execute("--log " + logF.getAbsolutePath() + " -d " + cutoff1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 0 --hetLowerCutoff 0.49");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.49\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"3\" score=\"1.0\"/>", outputData.get(8).trim());
		
		exec = execute("--log " + logF.getAbsolutePath() + " -d " + cutoff1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 0 --hetUpperCutoff 0.50");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.5\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"3\" score=\"1.0\"/>", outputData.get(8).trim());
		
		exec = execute("--log " + logF.getAbsolutePath() + " -d " + cutoff1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 0 --hetUpperCutoff 0.7");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"4\" score=\"0.75\"/>", outputData.get(8).trim());
}
	
	@Test
	public void bespokeVsBespokeBAMMaxCacheSize() throws IOException, InterruptedException {
		File logF = testFolder.newFile();
		File bespoke1 = testFolder.newFile("bespoke1.qsig.vcf");
		File bespoke2 = testFolder.newFile("bespoke2.qsig.vcf");
		File o = testFolder.newFile();
		List<String> bespoke1Data = new ArrayList<>(SignatureUtilTest.BAM_HEADER);
		bespoke1Data.addAll(Arrays.asList("chr1\t99236\t.\tT\t.\t.\t.\tQAF=t:0-0-0-34,rg1:0-0-0-20,rg2:0-0-0-14",
				"chr1\t101095\t.\tT\t.\t.\t.\tQAF=t:0-0-0-20,rg1:0-0-0-14,rg2:0-0-0-6",
				"chr1\t102954\t.\tT\t.\t.\t.\tQAF=t:0-0-1-161,rg1:0-0-1-71,rg2:0-0-0-90",
				"chr1\t104813\t.\tG\t.\t.\t.\tQAF=t:0-0-19-0,rg1:0-0-14-0,rg2:0-0-5-0",
				"chr1\t113422\t.\tT\t.\t.\t.\tQAF=t:0-0-0-23,rg1:0-0-0-17,rg2:0-0-0-6"));
		List<String> bespoke2Data = new ArrayList<>(SignatureUtilTest.BAM_HEADER);
		bespoke2Data.addAll(Arrays.asList("chr1\t99236\t.\tT\t.\t.\t.\tQAF=t:0-0-0-34,rg1:0-0-0-20,rg2:0-0-0-14",
				"chr1\t101095\t.\tT\t.\t.\t.\tQAF=t:0-0-0-20,rg1:0-0-0-14,rg2:0-0-0-6",
				"chr1\t102954\t.\tT\t.\t.\t.\tQAF=t:0-0-1-161,rg1:0-0-1-71,rg2:0-0-0-90",
				"chr1\t104813\t.\tG\t.\t.\t.\tQAF=t:0-0-19-0,rg1:0-0-14-0,rg2:0-0-5-0",
				"chr1\t113422\t.\tT\t.\t.\t.\tQAF=t:0-0-0-23,rg1:0-0-0-17,rg2:0-0-0-6"));
		
		writeDataToFile(bespoke1Data, bespoke1);
		writeDataToFile(bespoke2Data, bespoke2);
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + bespoke1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 0");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		List<String> outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 11 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"5\" score=\"1.0\"/>", outputData.get(8).trim());
		
		exec = execute("--log " + logF.getAbsolutePath() + " -d " + bespoke1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 1");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"5\" score=\"1.0\"/>", outputData.get(8).trim());
		
		exec = execute("--log " + logF.getAbsolutePath() + " -d " + bespoke1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 2");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"5\" score=\"1.0\"/>", outputData.get(8).trim());
		
		exec = execute("--log " + logF.getAbsolutePath() + " -d " + bespoke1.getParent() + " -o " + o.getAbsolutePath() + " --max-cache-size 20");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"5\" score=\"1.0\"/>", outputData.get(8).trim());
		
		exec = execute("--log " + logF.getAbsolutePath() + " -d " + bespoke1.getParent() + " -o " + o.getAbsolutePath() + " --maxCacheSize 1");
		assertEquals(0, exec.getErrCode());		// all ok
        assertTrue(o.exists());
		outputData = Files.readAllLines(Paths.get(o.getAbsolutePath()));
		assertEquals(11, outputData.size());		// 10 lines means 1 comparison
		assertEquals("<cutoffs hom=\"0.9\" lower_het=\"0.3\" upper_het=\"0.7\"/>", outputData.get(2).trim());
		assertEquals("<comparison file1=\"1\" file2=\"2\" overlap=\"5\" score=\"1.0\"/>", outputData.get(8).trim());
	}
	
	private void writeVcfFileHeader(File f) throws IOException {
		writeVcfFileHeader(f, "##positions_md5sum=d18c99f481afbe04294d11deeb418890\n");
	}
	private void writeVcfFileHeader(File f, String md5) throws IOException {
		try (FileWriter w = new FileWriter(f)){
			w.write("##fileformat=VCFv4.2\n");
			w.write("##datetime=2016-08-17T14:44:30.088\n");
			w.write("##program=SignatureGeneratorBespoke\n");
			w.write("##version=1.0 (1230)\n");
			w.write("##java_version=1.8.0_71\n");
			w.write("##run_by_os=Linux\n");
			w.write("##run_by_user=oliverH\n");
			w.write("##positions=/software/genomeinfo/configs/qsignature/qsignature_positions.txt\n");
			w.write(md5);
			w.write("##positions_count=1456203\n");
			w.write("##filter_base_quality=10\n");
			w.write("##filter_mapping_quality=10\n");
			w.write("##illumina_array_design=/software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt\n");
			w.write("##cmd_line=SignatureGeneratorBespoke -i /software/genomeinfo/configs/qsignature/qsignature_positions.txt -illumina /software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt -i /mnt/lustre/working/genomeinfo/share/mapping/aws/argsBams/dd625894-d1e3-4938-8d92-3a9f57c23e08.bam -d /mnt/lustre/home/oliverH/qsignature/bespoke/ -log /mnt/lustre/home/oliverH/qsignature/bespoke/siggen.log\n");
			w.write("##INFO=<ID=QAF,Number=.,Type=String,Description=\"Lists the counts of As-Cs-Gs-Ts for each read group, along with the total\">\n");
			w.write("##input=/mnt/lustre/working/genomeinfo/share/mapping/aws/argsBams/dd625894-d1e3-4938-8d92-3a9f57c23e08.bam\n");
			w.write("##id:readgroup\n");
			w.write("##rg1:143b8c38-62cb-414a-aac3-ea3a940cc6bb\n");
			w.write("##rg2:65a79904-ee91-4f53-9a94-c02e23e071ef\n");
			w.write("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO\n");
		}
	}
	
	private void writeVcfFile(File f) throws IOException {
		writeVcfFile(f, "##positions_md5sum=d18c99f481afbe04294d11deeb418890\n");
	}
	
	private void writeVcfFile(File f, String md5) throws IOException {
		try (FileWriter w = new FileWriter(f)){
			w.write("##fileformat=VCFv4.2\n");
			w.write("##datetime=2016-08-17T14:44:30.088\n");
			w.write("##program=SignatureGeneratorBespoke\n");
			w.write("##version=1.0 (1230)\n");
			w.write("##java_version=1.8.0_71\n");
			w.write("##run_by_os=Linux\n");
			w.write("##run_by_user=oliverH\n");
			w.write("##positions=/software/genomeinfo/configs/qsignature/qsignature_positions.txt\n");
			w.write(md5);
			w.write("##positions_count=1456203\n");
			w.write("##filter_base_quality=10\n");
			w.write("##filter_mapping_quality=10\n");
			w.write("##illumina_array_design=/software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt\n");
			w.write("##cmd_line=SignatureGeneratorBespoke -i /software/genomeinfo/configs/qsignature/qsignature_positions.txt -illumina /software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt -i /mnt/lustre/working/genomeinfo/share/mapping/aws/argsBams/dd625894-d1e3-4938-8d92-3a9f57c23e08.bam -d /mnt/lustre/home/oliverH/qsignature/bespoke/ -log /mnt/lustre/home/oliverH/qsignature/bespoke/siggen.log\n");
			w.write("##INFO=<ID=QAF,Number=.,Type=String,Description=\"Lists the counts of As-Cs-Gs-Ts for each read group, along with the total\">\n");
			w.write("##input=/mnt/lustre/working/genomeinfo/share/mapping/aws/argsBams/dd625894-d1e3-4938-8d92-3a9f57c23e08.bam\n");
			w.write("##id:readgroup\n");
			w.write("##rg1:143b8c38-62cb-414a-aac3-ea3a940cc6bb\n");
			w.write("##rg2:65a79904-ee91-4f53-9a94-c02e23e071ef\n");
			w.write("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO\n");
			w.write("chr1	47851\t.\tC\t.\t.\t.\tQAF=t:0-120-0-0,rg1:0-90-0-0,rg2:0-30-0-0\n");
			w.write("chr1	50251\t.\tT\t.\t.\t.\tQAF=t:0-0-0-110,rg1:0-0-0-90,rg2:0-0-0-20\n");
			w.write("chr1\t51938	.\tT\t.\t.\t.\tQAF=t:0-0-0-90,rg1:0-0-0-50,rg2:0-0-0-40\n");
			w.write("chr1\t52651	.\tT\t.\t.\t.\tQAF=t:0-0-0-30,rg1:0-0-0-10,rg2:0-0-0-20\n");
			w.write("chr1\t64251	.\tA\t.\t.\t.\tQAF=t:90-0-0-0,rg1:50-0-0-0,rg2:40-0-0-0\n");
			w.write("chr1\t98222	.\tC\t.\t.\t.\tQAF=t:0-120-0-0,rg1:0-50-0-0,rg2:0-70-0-0\n");
			w.write("chr1\t99236	.\tT\t.\t.\t.\tQAF=t:0-0-0-220,rg1:0-0-0-120,rg2:0-0-0-100\n");
			w.write("chr1\t101095	.\tT\t.\t.\t.\tQAF=t:0-0-0-100,rg1:0-0-0-50,rg2:0-0-0-50\n");
			w.write("chr1\t102954	.\tT\t.\t.\t.\tQAF=t:0-0-10-640,rg1:0-0-0-360,rg2:0-0-10-280\n");
			w.write("chr1\t104813	.\tG\t.\t.\t.\tQAF=t:0-10-170-0,rg1:0-10-100-0,rg2:0-0-70-0\n");
			w.write("chr1\t106222	.\tT\t.\t.\t.\tQAF=t:0-0-0-40,rg1:0-0-0-10,rg2:0-0-0-30\n");
		}
	}
	
	public static void writeDataToFile(List<String> data, File f) throws IOException {
		try (FileWriter w = new FileWriter(f)){
			for (String d : data) {
				w.write(d + "\n");
			}
		}
	}
	
	private Executor execute(final String command) throws IOException, InterruptedException {
		return new Executor(command, "org.qcmg.sig.Compare");
	}

}
