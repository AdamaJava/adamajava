package org.qcmg.bammerge;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.qcmg.common.commandline.Executor;
import org.qcmg.testing.SamTestData;

public class MainTest {
	public static final String OUTPUT_FILE_NAME = "output.sam";
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();

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
	public final void callWithValidArguments() throws Exception {
		ExpectedException.none();
		String command = "--log ./logfile -f -o output.sam -i first.sam -i second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(0 == exec.getErrCode());
	}

	@Test
	public final void callWithValidArgumentsButNoLogfile() throws Exception {
		ExpectedException.none();
		String command = "-f -o output.sam -i first.sam -i second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
	}

	@Test
	public final void callWithNoArgs() throws Exception {
		ExpectedException.none();
		String command = "";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithNoOutputOptionOneInputFile() throws Exception {
		ExpectedException.none();
		String command = "-i second.sam";		
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithNoOutputOptionTwoInputFiles() throws Exception {
		ExpectedException.none();
		String command = "-i first.sam -i second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithNoOutputOptionDuplicateInputFile()
			throws Exception {
		ExpectedException.none();
		String command = "-i first.sam -i first.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithMergeOptionNoLogfile() throws Exception {
		ExpectedException.none();
		String command = "-f -m first.sam -i second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
	}

	@Test
	public final void callWithMergeOption() throws Exception {
		ExpectedException.none();
		String command = "--log ./logfile -f -m first.sam -i second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(0 == exec.getErrCode());
	}

	@Test
	public final void callWithReadGroupClash() throws Exception {
		ExpectedException.none();
		String command = "-o output.sam -i first.sam -i second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Ignore //TODO: Reinstate post reporting
	public final void callWithVersionOption() throws Exception {
		ExpectedException.none();
		String command = "-v";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(0 == exec.getErrCode());
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Ignore //TODO: Reinstate post reporting
	public final void callWithHelpOption() throws Exception {
		ExpectedException.none();
		String command = "-h";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(0 == exec.getErrCode());
		assertTrue(0 == exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithBothOutputAndMergeOptions() throws Exception {
		ExpectedException.none();
		String command = "-o output.sam -m first.sam -i second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithNonOptions() throws Exception {
		ExpectedException.none();
		String command = "-o output.sam first.sam second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithMultipleOutputOptions() throws Exception {
		ExpectedException.none();
		String command = "-o output.sam -o output2.sam -i first.sam -i second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());//		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithMultipleMergeOptions() throws Exception {
		ExpectedException.none();
		String command = "-m first.sam -m second.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithoutInputFiles() throws Exception {
		ExpectedException.none();
		String command = "-o output.sam";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(1 == exec.getErrCode());
	}
	
	@Test
	public final void callWithValidArgumentsIncludingIndex() throws Exception {
		ExpectedException.none();
		String command = "--log ./logfile -f -o output.sam -i first.sam -i second.sam -bai";
		Executor exec = new Executor(command, "org.qcmg.bammerge.Main");
		assertTrue(0 == exec.getErrCode());
	}
}
