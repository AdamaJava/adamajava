package org.qcmg.snp.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.Rule;
import org.qcmg.snp.SnpException;

public class IniFileUtilTest {
	
	private File iniFile;
	
	 @org.junit.Rule
	 public TemporaryFolder folder = new TemporaryFolder();
	 
	@Before
	public void createIniFile() throws IOException {
		iniFile = folder.newFile("IniFlieUtilTest.ini");
		BufferedWriter out = new BufferedWriter(new FileWriter(iniFile));
        	out.write("[rules]\nnormal1= 0,20,3\n");
        	out.write("normal2=21,50,4\n");
        	out.write("normal3=51,,10\n");
        	out.write("tumour1=0,20,3\n");
        	out.write("tumour2=21,50,4\n");
        	out.write("tumour3=51,,5\n");
        	out.write("[patient]\nid = APGI_1992\n");
        	out.write("[outputFiles]\ndccSomatic = ${patient/id}__ssm__au__01__018__p__4__20110419.txt\n");
        	out.write("dccGermline = ${patient/id}__sgv__au__01__018__p__4__20110419.txt\n");
        	out.write("[inputFiles]\nnormalBam = ${patient/id}__ssm__au__01__018__p__4__20110419_1.txt\n");
        	out.write("normalBam = ${patient/id}__ssm__au__01__018__p__4__20110419_2.txt\n");
        	out.write("normalBam = ${patient/id}__ssm__au__01__018__p__4__20110419_3.txt\n");
        	out.write("[parameters]\n");
        	out.write("pileupOrder = NNTT\n");
        	out.close();
	}
	
	@Test
	public void testGetInpoutFiles() throws InvalidFileFormatException, IOException {
		Ini ini = new Ini(iniFile);
		Assert.assertEquals(3, IniFileUtil.getInputFiles(ini, "normalBam").length);
	}
	
	@Test
	public void testGetTumourRulesFromIni() {
		
		try {
			IniFileUtil.getRules(null, "normal");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		// add a rule
		Wini ini = new Wini();
		ini.add("rules", "normal1", "0,20,3");
		List<Rule> normalRules;
		try {
			normalRules = IniFileUtil.getRules(ini, null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		normalRules = IniFileUtil.getRules(ini, "normal");
		List<Rule> tumourRules = IniFileUtil.getRules(ini, "tumour");
		Assert.assertEquals(1, normalRules.size());
		Assert.assertEquals(0, tumourRules.size());
		Assert.assertEquals(0, normalRules.get(0).getMinCoverage());
		Assert.assertEquals(20, normalRules.get(0).getMaxCoverage());
		Assert.assertEquals(3, normalRules.get(0).getNoOfVariants());
		
		ini.add("rules", "normal2", "21,50,4");
		ini.add("rules", "tumour1", "1,150,8");
		
		normalRules = IniFileUtil.getRules(ini, "normal");
		tumourRules = IniFileUtil.getRules(ini, "tumour");
		Assert.assertEquals(2, normalRules.size());
		Assert.assertEquals(true, normalRules.contains(new Rule(0,20,3)));
		Assert.assertEquals(true, normalRules.contains(new Rule(21,50,4)));
		
		Assert.assertEquals(1, tumourRules.size());
		Assert.assertEquals(1, tumourRules.get(0).getMinCoverage());
		Assert.assertEquals(150, tumourRules.get(0).getMaxCoverage());
		Assert.assertEquals(8, tumourRules.get(0).getNoOfVariants());
		
		
		ini.add("rules", "tumour2", "2,300,16");
		tumourRules = IniFileUtil.getRules(ini, "tumour");
		Assert.assertEquals(2, tumourRules.size());
		Assert.assertEquals(true, tumourRules.contains(new Rule(1,150,8)));
		Assert.assertEquals(true, tumourRules.contains(new Rule(2,300,16)));		
		Assert.assertEquals(0, IniFileUtil.getRules(ini, "anything").size());
		Assert.assertEquals(0, IniFileUtil.getRules(ini, "blah").size());
	}
	
	@Test
	public void testGetLowestRuleValueFromIni() throws SnpException {
		try {
			IniFileUtil.getLowestRuleValue(null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		Wini ini = new Wini();
		
		try {
			IniFileUtil.getLowestRuleValue(ini);
			Assert.fail("Should have thrown a SnpException");
		} catch (SnpException e) {}
		
		
		ini.add("rules", "normal1", "0,20,3");		
		
		int lowestValue = IniFileUtil.getLowestRuleValue(ini);
		Assert.assertEquals(3, lowestValue);
		
		ini.add("rules", "tumour1", "0,1,2");
		
		lowestValue = IniFileUtil.getLowestRuleValue(ini);
		Assert.assertEquals(2, lowestValue);
		
		ini.add("rules", "normal2", "1111111110,1111111111,1");
		
		lowestValue = IniFileUtil.getLowestRuleValue(ini);
		Assert.assertEquals(1, lowestValue);
	}
	
	@Test
	public void testGetNumberOfFilesFromIni() {
		try {
			IniFileUtil.getNumberOfFiles(null, '\u0000');
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		Wini ini = new Wini();
		int number = IniFileUtil.getNumberOfFiles(ini, '\u0000');
		Assert.assertEquals(0, number);
		
		ini.add("parameters", "pileupOrder", "N");
		number = IniFileUtil.getNumberOfFiles(ini, '\u0000');
		int numberT = IniFileUtil.getNumberOfFiles(ini, 'T');
		int numberN = IniFileUtil.getNumberOfFiles(ini, 'N');
		
		Assert.assertEquals(0, number);
		Assert.assertEquals(0, numberT);
		Assert.assertEquals(1, numberN);

		ini.add("parameters", "pileupOrder", "NNNNNNNNNN");
		number = IniFileUtil.getNumberOfFiles(ini, '\u0000');
		numberT = IniFileUtil.getNumberOfFiles(ini, 'T');
		numberN = IniFileUtil.getNumberOfFiles(ini, 'N');
		
		Assert.assertEquals(0, number);
		Assert.assertEquals(0, numberT);
		Assert.assertEquals(10, numberN);
		
		ini.add("parameters", "pileupOrder", "NNNNNNNNNNTTT");
		number = IniFileUtil.getNumberOfFiles(ini, '\u0000');
		numberT = IniFileUtil.getNumberOfFiles(ini, 'T');
		numberN = IniFileUtil.getNumberOfFiles(ini, 'N');
		
		Assert.assertEquals(0, number);
		Assert.assertEquals(3, numberT);
		Assert.assertEquals(10, numberN);
		
		ini.add("parameters", "pileupOrder", "XXX");
		number = IniFileUtil.getNumberOfFiles(ini, '\u0000');
		numberT = IniFileUtil.getNumberOfFiles(ini, 'T');
		numberN = IniFileUtil.getNumberOfFiles(ini, 'N');
		int numberX = IniFileUtil.getNumberOfFiles(ini, 'X');
		
		Assert.assertEquals(0, number);
		Assert.assertEquals(0, numberT);
		Assert.assertEquals(0, numberN);
		Assert.assertEquals(3, numberX);
		
		ini.add("parameters", "pileupOrder", "");
		number = IniFileUtil.getNumberOfFiles(ini, '\u0000');
		numberT = IniFileUtil.getNumberOfFiles(ini, 'T');
		numberN = IniFileUtil.getNumberOfFiles(ini, 'N');
		
		Assert.assertEquals(0, number);
		Assert.assertEquals(0, numberT);
		Assert.assertEquals(0, numberN);
	}
	
	@Test
	public void testActualIniFile() throws InvalidFileFormatException, IOException, SnpException {
		Assert.assertNotNull(iniFile);
		Wini ini = new Wini(iniFile);
		
		List<Rule> normalRules = IniFileUtil.getRules(ini, "normal");
		List<Rule> tumourRules = IniFileUtil.getRules(ini, "tumour");
		int lowestValue = IniFileUtil.getLowestRuleValue(ini);
		int numberT = IniFileUtil.getNumberOfFiles(ini, 'T');
		int numberN = IniFileUtil.getNumberOfFiles(ini, 'N');
		String patientId = IniFileUtil.getPatientId(ini);
		String dccSomaticOutputFileName = IniFileUtil.getOutputFile(ini,  "dccSomatic");
		String dccGermlineOutputFileName = IniFileUtil.getOutputFile(ini, "dccGermline");
		
		Assert.assertEquals("APGI_1992", patientId);
		Assert.assertEquals("APGI_1992__ssm__au__01__018__p__4__20110419.txt", dccSomaticOutputFileName);
		Assert.assertEquals("APGI_1992__sgv__au__01__018__p__4__20110419.txt", dccGermlineOutputFileName);
		
		Assert.assertEquals(3, normalRules.size());
		Assert.assertEquals(true, normalRules.contains(new Rule(0,20,3)));
		Assert.assertEquals(true, normalRules.contains(new Rule(21,50,4)));
		Assert.assertEquals(true, normalRules.contains(new Rule(51,Integer.MAX_VALUE,10)));
		
		Assert.assertEquals(3, tumourRules.size());
		Assert.assertEquals(true, tumourRules.contains(new Rule(0,20,3)));
		Assert.assertEquals(true, tumourRules.contains(new Rule(21,50,4)));
		Assert.assertEquals(true, tumourRules.contains(new Rule(51,Integer.MAX_VALUE,5)));
		
		Assert.assertEquals(3, lowestValue);
		Assert.assertEquals(2, numberT);
		Assert.assertEquals(2, numberN);
	}
	
}
