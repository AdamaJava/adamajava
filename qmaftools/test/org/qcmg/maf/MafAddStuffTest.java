package org.qcmg.maf;

import java.io.FileNotFoundException;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class MafAddStuffTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public final void executeWithNoArgs() throws Exception {
		String[] args = {};
		try {
			int exitStatus = new MafAddStuff().setup(args);
			Assert.assertEquals(1, exitStatus);
		} catch (Exception e) {
			Assert.fail("no exception should have been thrown from executeWithNoArgs()");
		}
	}
	
	@Test
	public final void executeWithMissingFiles() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_FASTA_FILE_ERROR"));
		
		new MafAddStuff().setup(new String[] {"-log",  testFolder.newFile("log").getAbsolutePath(), 
				"-input", testFolder.newFile("maf").getAbsolutePath()});
	}
	@Test
	public final void executeWithMissingFastaFiles() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_GFF_FILE_ERROR"));
		
		new MafAddStuff().setup(new String[] {"-log",  testFolder.newFile("log").getAbsolutePath(), 
				"-input", testFolder.newFile("maf").getAbsolutePath(),
				"-fasta", testFolder.newFile("fasta").getAbsolutePath()});
	}
	
	@Test
	public final void executeWithMissingOutput() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("INPUT_OUTPUT_FILE_NUMBERS_ARE_NOT_EQUAL"));
		
		new MafAddStuff().setup(new String[] {"-log",  testFolder.newFile("log").getAbsolutePath(), 
				"-input", testFolder.newFile("maf").getAbsolutePath(),
				"-fasta", testFolder.newFile("fasta").getAbsolutePath(),
				"-gff", testFolder.newFile("gff").getAbsolutePath()});
	}
	
	@Test
	public final void executeWithMissingFastIndex() throws Exception {
		exception.expect(FileNotFoundException.class);
		
		new MafAddStuff().setup(new String[] {"-log",  testFolder.newFile("log").getAbsolutePath(), 
				"-input", testFolder.newFile("maf").getAbsolutePath(),
				"-output", testFolder.newFile("output").getAbsolutePath(),
				"-fasta", testFolder.newFile("fasta").getAbsolutePath(),
				"-gff", testFolder.newFile("gff").getAbsolutePath()});
	}
	
}
