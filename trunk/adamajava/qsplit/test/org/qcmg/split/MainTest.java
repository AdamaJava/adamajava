package org.qcmg.split;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.testing.SamTestData;

public class MainTest {
	public static final String SEP = System.getProperty("file.separator");
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	private File mergedFile;
	
	private final  String ORIG1 = "orig1.sam";
	private final  String ORIG2 = "orig2.sam";
	private final  String ORIG3 = "orig3.sam";
	private final  String ORIG1BAM = "orig1.bam";
	private final  String ORIG2BAM = "orig2.bam";
	private final  String ORIG3BAM = "orig3.bam";

	@Before
	public final void before() {
		try {
			mergedFile = tempFolder.newFile("merged.sam");
			String root = tempFolder.getRoot().getPath();
			SamTestData.createMergedSam(mergedFile, root + SEP + ORIG1, root + SEP + ORIG2, root + SEP + ORIG3);
			assertTrue(mergedFile.exists());
		} catch (Exception e) {
			System.err.println("File creation error in test harness: "
					+ e.getMessage());
		}
	}
	
	public void filesShouldNotExist() {
		assertFalse(new File( tempFolder.getRoot(), ORIG1).exists());
		assertFalse(new File( tempFolder.getRoot(), ORIG2).exists());
		assertFalse(new File( tempFolder.getRoot(), ORIG3).exists());
	}
	public void filesShouldExist() {
		assertTrue(new File( tempFolder.getRoot(), ORIG1).exists());
		assertTrue(new File( tempFolder.getRoot(), ORIG2).exists());
		assertTrue(new File( tempFolder.getRoot(), ORIG3).exists());
	}

	@Test
	public final void callWithValidArguments() throws Exception {
		ExpectedException.none();
		filesShouldNotExist();
		
		String command = "--log ./logfile -i " + mergedFile.getAbsolutePath() + " -d" + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(0 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		
		filesShouldExist();
	}

	@Test
	public final void callWithValidArgumentsButNoLogfile() throws Exception {
		ExpectedException.none();
		filesShouldNotExist();
		String command = " -i " + mergedFile.getAbsolutePath() + " -d " + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getOutputStreamConsumer().getLines().length);
		filesShouldNotExist();
	}

	@Test
	public final void callWithNoArgs() throws Exception {
		ExpectedException.none();
		String command = "";
		filesShouldNotExist();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getOutputStreamConsumer().getLines().length);
		filesShouldNotExist();
	}

	@Test
	public final void callWithNoOutputDirectorySpecified() throws Exception {
		ExpectedException.none();
		filesShouldNotExist();
		String command = "-log ./logfile -i " + mergedFile.getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
		filesShouldNotExist();
	}

	@Test
	public final void callWithNoInputFile() throws Exception {
		ExpectedException.none();
		filesShouldNotExist();
		String command = "-log ./logfile -d " + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
		filesShouldNotExist();
	}

	@Test
	public final void callWithDuplicateInputFile() throws Exception {
		ExpectedException.none();
		filesShouldNotExist();
		String command = "-log ./logfile -i " + mergedFile.getAbsolutePath() + " -i " + mergedFile.getAbsolutePath() + " -d " + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
		filesShouldNotExist();
	}

	@Test
	public final void callWithBamOptionButNotNOption() throws Exception {
		ExpectedException.none();
		filesShouldNotExist();
		String command = "--log ./logfile -b -i " + mergedFile.getAbsolutePath() + " -d " + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(0 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		filesShouldExist();
		assertFalse(new File( tempFolder.getRoot(), ORIG1BAM).exists());
		assertFalse(new File( tempFolder.getRoot(), ORIG2BAM).exists());
		assertFalse(new File( tempFolder.getRoot(), ORIG3BAM).exists());
	}
	
	@Test
	public final void callWithBamAndNOption() throws Exception {
		ExpectedException.none();
		filesShouldNotExist();
		String command = "--log ./logfile -b -n -i " + mergedFile.getAbsolutePath() + " -d " + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(0 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		filesShouldNotExist();
		assertTrue(new File( tempFolder.getRoot(), "3.bam").exists());
		assertTrue(new File( tempFolder.getRoot(), "5.bam").exists());
		assertTrue(new File( tempFolder.getRoot(), "6.bam").exists());
	}
	
	@Test
	public final void callWithNOption() throws Exception {
		ExpectedException.none();
		filesShouldNotExist();
		String command = "--log ./logfile -n -i " + mergedFile.getAbsolutePath() + " -d " + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(0 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		filesShouldNotExist();
		assertTrue(new File( tempFolder.getRoot(), "3.sam").exists());
		assertTrue(new File( tempFolder.getRoot(), "5.sam").exists());
		assertTrue(new File( tempFolder.getRoot(), "6.sam").exists());
	}
	
	@Test
	public final void callWithNAndSOption() throws Exception {
		ExpectedException.none();
		filesShouldNotExist();
		String command = "--log ./logfile -n -s -i " + mergedFile.getAbsolutePath() + " -d " + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(0 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		filesShouldNotExist();
		assertTrue(new File( tempFolder.getRoot(), "3.sam").exists());
		assertTrue(new File( tempFolder.getRoot(), "5.sam").exists());
		assertTrue(new File( tempFolder.getRoot(), "6.sam").exists());
	}

	@Test
	public final void callWithVersionOption() throws Exception {
		ExpectedException.none();
		String command = "-v";
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithHelpOption() throws Exception {
		ExpectedException.none();
		String command = "-h";
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getOutputStreamConsumer().getLines().length);
		//FIXME the following line fails in TeamCity build!!!
//		assertTrue(0 == exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithBothSAndBOptions() throws Exception {
		ExpectedException.none();
		String command = "--log ./logfile -n -s -b -i " + mergedFile.getAbsolutePath() + " -d " + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		//TODO this does not currently throw an exception
		assertTrue(0 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithNonOptions() throws Exception {
		ExpectedException.none();
		String command = "-log ./logfile -o output.sam first.sam second.sam";
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithMultipleOutputOptions() throws Exception {
		ExpectedException.none();
		String command = "-log ./logfile -d output.sam -d output2.sam -i first.sam -i second.sam";
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}
	
	@Ignore // not yet implemented
	public final void callWithValidArgumentsIncludingIndex() throws Exception {
		ExpectedException.none();
		String command = "--log ./logfile -o output.sam -i first.sam -i second.sam -bai";
		Executor exec = new Executor(command, "org.qcmg.split.Main");
		assertTrue(0 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
	}
}
