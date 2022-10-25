package org.qcmg.sig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.sig.util.SignatureUtil;
import org.qcmg.sig.util.SignatureUtilTest;

public class FindPatientDistanceTest {
	
	public static final String COVERAGE_10A_10G = "FULLCOV=A:10,C:0,G:10,T:0,N:0,TOTAL:20;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
	public static final String COVERAGE_20A = "FULLCOV=A:20,C:0,G:0,T:0,N:0,TOTAL:20;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	public  File searchFolder;
	
	@Before
	public void setup() throws IOException {
		searchFolder = testFolder.newFolder();
	}
	
	@Test
	public void testNoArgs() throws Exception {
		Executor exec = execute(null);
		assertTrue(1 == exec.getErrCode());
		
		exec = execute("");
		assertTrue(1 == exec.getErrCode());
	}
	
	@Ignore
	public void testNoSnpChipFiles() throws Exception {
		File logFile = testFolder.newFile("testNoSnpChipFiles.log");
		File bamVcfFile = testFolder.newFile("testNoSnpChipFiles.qsig.vcf`");
		
		Executor exec = execute("--log " + logFile.getAbsolutePath() + " -i blah");
		assertTrue(2 == exec.getErrCode());		// doesn't like default path name (/aa/bb/cc)
		
		exec = execute("--log " + logFile.getAbsolutePath() + " -i " + bamVcfFile.getAbsolutePath() + " -d " + searchFolder.getAbsolutePath());
		assertEquals(1, exec.getErrCode());
	}
	
	@Ignore
	public void testNoMainFiles() throws Exception {
		File logFile = testFolder.newFile("testNoMainFiles.log");
		File snpChipSigFile = testFolder.newFile(searchFolder.getName() + "/snpChip.txt.qsig.vcf");
		File bamVcfFile = testFolder.newFile("testNoSnpChipFiles.qsig.vcf`");
		
		SignatureUtilTest.writeSignatureFile(snpChipSigFile, "20	14370	rs6054257	G	A	29	PASS\t" + SignatureUtil.EMPTY_COVERAGE + "\n");
		
		Executor exec = execute("--log " + logFile.getAbsolutePath() +  " -i " + bamVcfFile.getAbsolutePath() + " -d " + searchFolder.getAbsolutePath());
		assertEquals(1 , exec.getErrCode());
	}
	
	@Ignore
	public void test1Main1Comp() throws Exception {
		String patient = "ABCD_123";
		File logFile = testFolder.newFile("test1Main1Comp.log");
		File snpChipSigFile = testFolder.newFile(searchFolder.getName() + "/snpChip.txt.qsig.vcf");
		File bamSigFile = testFolder.newFile(searchFolder.getName() + "/" + patient + "_bam.qsig.vcf");
		
		SignatureUtilTest.writeSignatureFile(snpChipSigFile, "20	14370	rs6054257	G	A	29	PASS\t" + COVERAGE_10A_10G + "\n");
		SignatureUtilTest.writeSignatureFile(bamSigFile, "20	14370	rs6054257	G	A	29	PASS\t" + COVERAGE_20A + "\n");
		
		Executor exec = execute("--log " + logFile.getAbsolutePath() + " -i " + bamSigFile.getAbsolutePath() +" -d " + searchFolder.getAbsolutePath() + " -additionalSearch snpChip ");
		assertEquals(0 , exec.getErrCode());
	}
	
	@Ignore
	public void test1Main2Comp() throws Exception {
		String patient = "ABCD_123";
		File patientFolder = testFolder.newFolder(patient);
		File logFile = testFolder.newFile(patientFolder.getName() + "/test1Main2Comp.log");
		File snpChipSigFile = testFolder.newFile(patientFolder.getName() + "/snpChip" + patient + ".txt.qsig.vcf");
		File otherPatientSnpChipSigFile = testFolder.newFile(patientFolder.getName() + "/snpChip.txt.qsig.vcf");
		File bamSigFile = testFolder.newFile(patientFolder.getName() + "/" + patient + "_bam.qsig.vcf");
		
		SignatureUtilTest.writeSignatureFile(otherPatientSnpChipSigFile, "20	14370	rs6054257	G	A	29	PASS\t" + COVERAGE_10A_10G + "\n");
		SignatureUtilTest.writeSignatureFile(snpChipSigFile, "20	14370	rs6054257	G	A	29	PASS\t" + COVERAGE_20A + "\n");
		SignatureUtilTest.writeSignatureFile(bamSigFile, "20	14370	rs6054257	G	A	29	PASS\t" + COVERAGE_20A + "\n");
		
		Executor exec = execute("--log " + logFile.getAbsolutePath() + " -i " + bamSigFile.getAbsolutePath() +" -d " + patientFolder.getAbsolutePath() + " -additionalSearch snpChip ");
		assertEquals(0 , exec.getErrCode());
	}
	
	private Executor execute(final String command) throws Exception {
		return new Executor(command, "org.qcmg.sig.QSigFindPatientDistance");
	}
	
}
