package org.qcmg.qbamfix;

import java.io.BufferedWriter;
import java.util.Date;
import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import htsjdk.samtools.SAMReadGroupRecord;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class ReplaceRGTest {
	 public static final String INPUT_FILE_NAME = "input.sam";
	
	@Test
	public void test_preRGisNull() throws Exception{
		
		ReplaceRG myRG = new org.qcmg.qbamfix.ReplaceRG();
		long Did = new Date().getTime();		
		String Sid =new SimpleDateFormat("yyyyMMddhhmmss").format(Did);
		
		//use date long formate as input
		SAMReadGroupRecord longRG = myRG.createRG(Did ,null);
		//use string as input
		SAMReadGroupRecord stringRG = myRG.createRG(Sid ,null);
		
		Assert.assertTrue(longRG.getId().startsWith(String.valueOf(Sid)));
		Assert.assertTrue(stringRG.getId().equals(Sid));
		Assert.assertFalse(stringRG.getId().equals(longRG.getId()));
		Assert.assertTrue(stringRG.getLibrary() == null); 
	}
	
	@Test
	public void test_createRG() throws Exception{
		String CN = "deafultCenter ";
		String PL = "defaultPlatform";		
		String SM = "defaultSample";		
		String LB = "defaultLibrary";
		String PU = "defaultPlatformUnit";
		
		//create new RG
		HashMap<String, String> optRG = new  HashMap<String, String>();
		optRG.put("LB", LB);
		SAMReadGroupRecord newRG =   new SAMReadGroupRecord("defaultid");
		ReplaceRG myRG = new org.qcmg.qbamfix.ReplaceRG();
		myRG.replaceRG(newRG, optRG, new File(INPUT_FILE_NAME) );
		
		//check RG field value is equal to optRG
		Assert.assertTrue(newRG.getLibrary().equals(LB));		
		Assert.assertTrue(newRG.getPlatform().equals(myRG.default_PL));
		Assert.assertTrue(newRG.getSample().equals(myRG.default_SM));
		Assert.assertTrue(newRG.getSequencingCenter().equals(myRG.default_CN));
		
		//overwrite existing RG fiele
		optRG = new  HashMap<String, String>();
		optRG.put("CN", CN);
		optRG.put("PL", PL);
		optRG.put("SM", SM);
		optRG.put("PU", PU);
		myRG.replaceRG(newRG, optRG, new File(INPUT_FILE_NAME) );
		
		Assert.assertTrue(newRG.getLibrary().equals(LB));		
		Assert.assertTrue(newRG.getPlatform().equals(PL));
		Assert.assertTrue(newRG.getSample().equals(SM));
		Assert.assertTrue(newRG.getSequencingCenter().equals(CN));
		Assert.assertTrue(newRG.getPlatformUnit().equals(PU));		
	}
	
	@Test
	public void test_createPU() throws Exception{
		HashMap<String, String> optRG = new  HashMap<String, String>();
		optRG.put("LB", "library");
		SAMReadGroupRecord newRG =   new SAMReadGroupRecord("defaultid");
		ReplaceRG myRG = new org.qcmg.qbamfix.ReplaceRG();
		
		//existing PU value won't be replaced if didn't specify value on command line
		myRG.replaceRG(newRG, optRG, new File("input.lane.bar.sam") );	
		myRG.replaceRG(newRG, optRG, new File("input.lane.sam") );
		Assert.assertEquals("lane.bar", newRG.getPlatformUnit());
		
		//only retrive the middle string from input name
		newRG =   new SAMReadGroupRecord("defaultid");
		myRG.replaceRG(newRG, optRG, new File("input.lane.sam") );
		Assert.assertEquals("lane", newRG.getPlatformUnit());
		
		//the middle string is null
		newRG =   new SAMReadGroupRecord("defaultid");
		myRG.replaceRG(newRG, optRG, new File("input.sam") );
		Assert.assertEquals(null, newRG.getPlatformUnit());
	}
	
	@Test (expected = ParseException.class)  
	public void test_createDT() throws Exception{
		String DT1 = "2010-04-08 16:02:03";
		String DT2 = "2010-04-08";
		HashMap<String, String> optRG = new  HashMap<String, String>();
		optRG.put("LB", "library");
		
		//correct date format
		optRG.put("DT", DT1);
		SAMReadGroupRecord newRG =   new SAMReadGroupRecord("defaultid");
		ReplaceRG myRG = new org.qcmg.qbamfix.ReplaceRG();
		myRG.replaceRG(newRG, optRG, new File("input.sam") );

		//incorrect date format
		optRG.put("DT", DT2);
		myRG.replaceRG(newRG, optRG, new File("input.sam") );

	}
	@BeforeClass
	public static void createInput(){
		List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");       
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        data.add("@CO	create by org.qcmg.qbamfix.RepalceRGTest::createInput");
              
        BufferedWriter out;
        try {
           out = new BufferedWriter(new FileWriter(INPUT_FILE_NAME));
           for (String line : data) {
                   out.write(line + "\n");
           }
           out.close();
        } catch (IOException e) {
            System.err.println("IOException caught whilst attempting to write to SAM test file: "
                                               + INPUT_FILE_NAME + e);
        }
	}
	
	@AfterClass
	public static void deletInput(){
		new File(INPUT_FILE_NAME).delete();		
	}
}
