package org.qcmg.qbamannotate;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.qcmg.common.commandline.Executor;
import org.qcmg.testing.TestDataSolid0039_20091125_2_TD04_LMP_20100531;

public class MainTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public final void before() {
		try {
			TestDataSolid0039_20091125_2_TD04_LMP_20100531
					.createNaturalReadNameSortedMappedSamFile();
			TestDataSolid0039_20091125_2_TD04_LMP_20100531.createF3MaFile();
			TestDataSolid0039_20091125_2_TD04_LMP_20100531.createR3MaFile();
		} catch (Exception e) {
			System.err.println("File creation error in test harness: "
					+ e.getMessage());
		}
	}

	@After
	public final void after() {
		try {
			File inputFile = new File("test.sam");
			inputFile.delete();
			File f3File = new File("f3.ma");
			f3File.delete();
			File r3File = new File("r3.ma");
			r3File.delete();
			File outputFile = new File("output.sam");
			outputFile.delete();
		} catch (Exception e) {
			System.err.println("File deletion error in test harness: "
					+ e.getMessage());
		}
	}

	@Test
	public final void callWithOneMaMissingLogfileOption() throws Exception {
		ExpectedException.none();
		String command = "--type frag output.sam test.sam f3.ma";
		Executor exec = new Executor(command, "org.qcmg.qbamannotate.Main");
		assertTrue(1 == exec.getErrCode());
	}

	@Test
	public final void callWithTwoMaMissingLogfileOption() throws Exception {
		ExpectedException.none();
		String command = "--type lmp -l 0 -u 10000 output.sam test.sam f3.ma r3.ma";
		Executor exec = new Executor(command, "org.qcmg.qbamannotate.Main");
		assertTrue(1 == exec.getErrCode());
	}
	
	@Test
	public final void callWithValidArgumentsOneMa() throws Exception {
		ExpectedException.none();
		String command = "--log ./logfile --type frag output.sam test.sam f3.ma";
		Executor exec = new Executor(command, "org.qcmg.qbamannotate.Main");
		assertTrue(0 == exec.getErrCode());
		//assertTrue(0 == exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithValidArgumentsTwoMa() throws Exception {
		ExpectedException.none();
		String command = "--log ./logfile --type lmp -l 0 -u 10000 output.sam test.sam f3.ma r3.ma";
		Executor exec = new Executor(command, "org.qcmg.qbamannotate.Main");
		assertTrue(0 == exec.getErrCode());
		//assertTrue(0 == exec.getErrorStreamConsumer().getLines().length);
	}

	@Test
	public final void callWithNoArgs() throws Exception {
		ExpectedException.none();
		String command = "";
		Executor exec = new Executor(command, "org.qcmg.qbamannotate.Main");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}
}
