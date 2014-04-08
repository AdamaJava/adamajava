package org.qcmg.maf;

import java.io.File;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LiftoverTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();

	@Test
	public final void executeWithNoArgs() throws Exception {
		String[] args = {};
		try {
			int exitStatus = new Liftover().setup(args);
			Assert.assertEquals(1, exitStatus);
		} catch (Exception e) {
			Assert.fail("no exception should have been thrown from executeWithNoArgs()");
		}
	}
	
	@Test
	public final void executeWithMissingChainFile() throws Exception {
		File logFile = testFolder.newFile("executeWithMissingChainFile.log");
		File inputFile = testFolder.newFile("executeWithMissingChainFile.maf");
		
		String[] args = {"-log",  logFile.getAbsolutePath(), "-input", inputFile.getAbsolutePath()};
		try {
			new Liftover().setup(args);
			Assert.fail("Should have thrown a QMafException");
		} catch (QMafException e) {}
	}
	
}
