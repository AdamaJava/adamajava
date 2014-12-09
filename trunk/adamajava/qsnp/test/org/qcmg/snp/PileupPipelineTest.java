package org.qcmg.snp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.Rule;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileReader;

public class PileupPipelineTest {
 
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@org.junit.Rule
    public ExpectedException thrown= ExpectedException.none();
	
	@Test
	public void testPileupPipelineEmptyPileupFile() throws Exception {
	 	final File logFile = testFolder.newFile("qsnp.log");
		final File iniFile = testFolder.newFile("qsnp.ini");
		final File pileupInput = testFolder.newFile("input.pileup");
		final File vcfOutput = testFolder.newFile("output.vcf");
		PileupFileGenerator.createPileupFile(pileupInput);
	
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "pileup = " + pileupInput.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// that should be it
		final String command = "-log " + logFile.getAbsolutePath() + " -i " + iniFile.getAbsolutePath();
		final Executor exec = new Executor(command, "org.qcmg.snp.Main");
		assertEquals(1, exec.getErrCode());
	}
	
	@Test
	public void testPileupPipelineGenerateVCFOnly() throws Exception {
 		final File logFile = testFolder.newFile("qsnp.log");
		final File iniFile = testFolder.newFile("qsnp.ini");
		
		
		final File pileupInput = testFolder.newFile("input.pileup");
		final File vcfOutput = testFolder.newFile("output.vcf");
 		
		PileupFileGenerator.createPileupFile(pileupInput);
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "pileup = " + pileupInput.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = pileup", true);	// append to file
		
		// that should be it
		ExpectedException.none();
		final String command = "-log " + logFile.getAbsolutePath() + " -i " + iniFile.getAbsolutePath();
		final Executor exec = new Executor(command, "org.qcmg.snp.Main");
//		String [] lines = exec.getErrorStreamConsumer().getLines();
//		for (String s : lines) {
//			System.out.println("s: " + s);
//		}
		assertEquals(0, exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		
		//debug		  
		 final BufferedReader bufferReader = new BufferedReader(new FileReader(vcfOutput));
		 String line;
		while ((line = bufferReader.readLine()) != null)   {
	            System.out.println(line);
	    }
		
		// check the vcf output file
		assertEquals(1, noOfLinesInVCFOutputFile(vcfOutput));
	}
	
	@Test
	public void testPileupPipelineGenerateVCFOnlyIncludeIndels() throws Exception {
		final File logFile = testFolder.newFile("qsnp.log");
		final File iniFile = testFolder.newFile("qsnp.ini");
		IniFileGenerator.createRulesOnlyIni(iniFile);
		
		final File pileupInput = testFolder.newFile("input.pileup");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
		PileupFileGenerator.createPileupFile(pileupInput);
		
		IniFileGenerator.addInputFiles(iniFile, false, "pileup = " + pileupInput.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nincludeIndels = true", true);
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "\nrunMode = pileup", true);	// append to file
		
		// that should be it
		ExpectedException.none();
		final String command = "-log " + logFile.getAbsolutePath() + " -i " + iniFile.getAbsolutePath();
		final Executor exec = new Executor(command, "org.qcmg.snp.Main");
		assertEquals(0, exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		
		// check the vcf output file
		assertEquals(2, noOfLinesInVCFOutputFile(vcfOutput));
	}
	
//	@Test
//	public void testPileupPipelineDCCMode() throws Exception{
//		File logFile = testFolder.newFile("qsnp.log");
//		File iniFile = testFolder.newFile("qsnp.ini");
//		IniFileGenerator.createRulesOnlyIni(iniFile);
//		
//		File pileupInput = testFolder.newFile("input.pileup");
//		File vcfOutput = testFolder.newFile("output.vcf");
//		
//		PileupFileGenerator.createPileupFile(pileupInput);
//		
//		IniFileGenerator.addInputFiles(iniFile, false, "pileup = " + pileupInput.getAbsolutePath());
//		
//		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath()); 
//		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nincludeIndels = true", true);
//		
//		// add the annotate mode=dcc to the ini file
//		IniFileGenerator.addStringToIniFile(iniFile, "\nannotateMode = dcc", true);
//		// add runType to ini file
//		IniFileGenerator.addStringToIniFile(iniFile, "\nrunMode = pileup", true);	// append to file
//		
//		// that should be it
//		ExpectedException.none();
//		String command = "-log " + logFile.getAbsolutePath() + " -i " + iniFile.getAbsolutePath();
//		Executor exec = new Executor(command, "org.qcmg.snp.Main");
//		assertEquals(0, exec.getErrCode());
//		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
//		
//		// check the vcf output file
//		assertEquals(2, noOfLinesInVCFOutputFile(vcfOutput));
//		
//	}
	
	private int noOfLinesInVCFOutputFile(File vcfOutput) throws Exception {
		int noOfLines = 0;
		try (VCFFileReader reader = new VCFFileReader(vcfOutput);) {
			for (final VcfRecord vcf : reader) noOfLines++;
		}
		return noOfLines;
	}
	
	public static String getFileHeader(File file) throws Exception {
		try (TabbedFileReader reader = new TabbedFileReader(file);) {
			for (final TabbedRecord vcf : reader) {
				if (vcf.getData().startsWith("analysis")) return vcf.getData();
			}
		}
		return "no header line found";
	}
	
	private int noOfLinesInDCCOutputFile(File dccFile) throws Exception {
		int noOfLines = 0;
		try (TabbedFileReader reader = new TabbedFileReader(dccFile);) {
			for (final TabbedRecord vcf : reader) {
				if (vcf.getData().startsWith("analysis")) continue;	// header line
				noOfLines++;
			}
		}
		return noOfLines;
	}
	
	@Test
	public void testIsRecordAKeeper() {
		// arguments are.....
		// int variantCount, int coverage, Rule rule, List<PileupElement> baseCounts, double percentage
		
		final Rule r = new Rule(0, 20, 1);
		final List<PileupElement> pes = new ArrayList<PileupElement>();
		
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(0, 0, r, null, 0));
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(0, 0, r, pes, 0));
		
		try {	// variant count is greater than total count
			assertEquals(false, PileupPipeline.isPileupRecordAKeeper(1, 0, r, pes, 0));
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (final IllegalArgumentException e) {}
		
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(1, 1, r, pes, 0));
		final PileupElement pe = new PileupElement('A');
		pe.incrementForwardCount((byte)'I');
		pes.add(pe);
		assertEquals(true, PileupPipeline.isPileupRecordAKeeper(1, 1, r, pes, 0));
		
		//. change rule
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(1, 1, new Rule(0,20,2), pes, 0));
		pe.incrementReverseCount((byte)'I');
		assertEquals(true, PileupPipeline.isPileupRecordAKeeper(2, 2, new Rule(0,20,2), pes, 0));
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(2, 2, new Rule(0,20,4), pes, 0));
		
		// only use percentage if we are dealing with the upper bounded rule
		assertEquals(true, PileupPipeline.isPileupRecordAKeeper(2, 100, new Rule(0,Integer.MAX_VALUE,4), pes, 0));
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(1, 100, new Rule(0,Integer.MAX_VALUE,4), pes, 0));
		
		final PileupElement pe2 = new PileupElement('.');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pes.add(pe2);
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(2, 9, new Rule(0,20,2), pes, 50));
		assertEquals(true, PileupPipeline.isPileupRecordAKeeper(2, 9, new Rule(0,20,2), pes, 10));
	}
	
	@Test
	public void testIsVariantOnBothStrands() {
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(null));
		final List<PileupElement> pes = new ArrayList<PileupElement>();
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(pes));
		final PileupElement pe = new PileupElement('.');
		pe.incrementForwardCount((byte)'I');
		pes.add(pe);
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(pes));
		final PileupElement pe2 = new PileupElement('C');
		pe2.incrementForwardCount((byte)'I');
		pes.add(pe2);
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(pes));
		pe2.incrementReverseCount((byte)'?');
		assertEquals(true, PileupPipeline.isVariantOnBothStrands(pes));
		
		pes.clear();
		final PileupElement pe3 = new PileupElement('T');
		pe3.incrementReverseCount((byte)'I');
		pes.add(pe3);
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(pes));
		pe3.incrementForwardCount((byte)'I');
		assertEquals(true, PileupPipeline.isVariantOnBothStrands(pes));
	}
}
