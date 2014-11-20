package org.qcmg.snp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MainTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	

	
	@Test
	public final void executeWithNoArgs() throws Exception {
		String[] args = {};
		try {
			// no longer throws SNPException with null args - shows usage message instead
			new Main().setup(args);
//			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {}
	}
	
	@Test
	public final void executeWithInvalidArgs() throws Exception {
		String[] args = {"-input", "blah.ini"};
		try {
			new Main().setup(args);
			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {}
		
		// this time
		File inputFile = folder.newFile("input.file");
		try {
			new Main().setup(new String[]{"-input", inputFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {}
		
	}
	
	@Test
	public final void executeWithArgsForDoitall() throws Exception {
		String[] args = {"-input", "blah.ini"};
		try {
			new Main().setup(args);
			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {}
		
		// this time
		File inputFile = folder.newFile("input.ini");
		try {
			new Main().setup(new String[]{"-input", inputFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {}
		
		// now add the runType
		IniFileGenerator.addStringToIniFile(inputFile, "[runType]\nmode = doitall", true);	// append to file
		try {
			new Main().setup(new String[]{"-input", inputFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {
			Assert.assertEquals(true, e.getMessage().startsWith(Messages.getMessage("MISSING_ENTRIES_IN_INI_FILE")));
		}
		
	}
	
	
	
	@Test
	public final void executeWithInvalidArgs2() throws Exception {
		File iniFile = folder.newFile("test.ini");
		File logFile = folder.newFile("test.log");
		int exitStatus; 
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown an exception");
		} catch (SnpException se) {}
//		Assert.assertEquals(1, exitStatus);
		
		// this time populate ini file with pileup input file
		File pileupFile = folder.newFile("test.pileup");
		FileWriter writer = new FileWriter(iniFile);
		try {
			writer.write("[inputFiles]\npileup = " + pileupFile.getAbsolutePath());
		} finally {
			writer.close();
		}
		
		// fails due to missing vcf output file
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown an exception");
		} catch (SnpException se) {}
//		Assert.assertEquals(1, exitStatus);
		
		// ADD IN OUTPUT FILE
		// create vcfOutputFile and add to ini
		File vcfOutputFile = folder.newFile("test.vcf.output");
		writer = new FileWriter(iniFile, true);
		try {
			writer.write("\n[outputFiles]\nvcf = " + vcfOutputFile.getAbsolutePath());
		} finally {
			writer.close();
		}
		
		// fails due to missing entries in ini file
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {
			Assert.assertEquals(true, e.getMessage().startsWith(Messages.getMessage("MISSING_ENTRIES_IN_INI_FILE")));
		}
		
		// add in the runType
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = pileup", true);	// append to file
		// fails due to missing entries in ini file
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {
			Assert.assertEquals(true, e.getMessage().startsWith("Ini file did not contain any valid rules"));
		}
		
		// ADD IN RULES
		addRulesToIniFile(iniFile);
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {
			Assert.assertEquals(true, e.getMessage().startsWith(Messages.getMessage("EMPTY_PILEUP_FILE")));
		}
		
	}
	
	@Test
	public void testGermlineDBUpdateOptions() throws Exception {
		
		// if updateGermlineDB value is set to true in the ini file, the GermlineDB file must also be specified
		
		File iniFile = folder.newFile("testGermlineDBUpdateOptions.ini");
		File logFile = folder.newFile("testGermlineDBUpdateOptions.log");
		File vcfOutputFile = folder.newFile("testGermlineDBUpdateOptions.vcf.output");
		File germlineDBFile = folder.newFile("testGermlineDBUpdateOptions.germlineDB");
		int exitStatus ;
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown an excpetion");
		} catch (SnpException se) {}
//		Assert.assertEquals(1, exitStatus);
		
		// add in the updateGermlineDB value
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nupdateGermlineDB = true", false);	// new file
		IniFileGenerator.addStringToIniFile(iniFile, "[outputFiles]\nvcf = " + vcfOutputFile.getAbsolutePath(), true);	// append to file
		
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SNPException");
		} catch (SnpException e) {
			Assert.assertEquals(true, e.getMessage().startsWith(Messages.getMessage("MISSING_ENTRIES_IN_INI_FILE")));
		}
		
		// add in the germlinedb file
		IniFileGenerator.addStringToIniFile(iniFile, "[inputFiles]\npileup = " + vcfOutputFile.getAbsolutePath()
				+ "\ngermlineDB = " + germlineDBFile.getAbsolutePath(), true);	// append to file
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SNPException");
		} catch (SnpException e) {
			Assert.assertEquals(true, e.getMessage().startsWith(Messages.getMessage("MISSING_ENTRIES_IN_INI_FILE")));
		}
		
		// add in the runType
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = pileup", true);	// append to file
		
		// different exception
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SNPException");
		} catch (SnpException e) {
			Assert.assertEquals(true, e.getMessage().startsWith(Messages.getMessage("NO_VALID_RULES_IN_INI")));
		}
		
		// create new ini file, setting updateGermlineDB to false
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nupdateGermlineDB = false", false);	// new file
		IniFileGenerator.addStringToIniFile(iniFile, "\nrunMode = pileup", true);	// append to file
		IniFileGenerator.addStringToIniFile(iniFile, "[outputFiles]\nvcf = " + vcfOutputFile.getAbsolutePath(), true);	// append to file
		IniFileGenerator.addStringToIniFile(iniFile, "[inputFiles]\npileup = " + vcfOutputFile.getAbsolutePath(), true);	// append to file
		
		// different exception
		try {
			exitStatus = new Main().setup(new String[]{"-input", iniFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()});
			Assert.fail("Should have thrown a SNPException");
		} catch (SnpException e) {
			Assert.assertEquals(true, e.getMessage().startsWith(Messages.getMessage("NO_VALID_RULES_IN_INI")));
		}
		
	}
	
	private void addRulesToIniFile(File iniFile) throws IOException {
		FileWriter writer = new FileWriter(iniFile, true);
		try {
			writer.write("\n[rules]\nnormal1=0,20,3");
			writer.write("\ntumour1=0,20,3");
		} finally {
			writer.close();
		}
	}
}
