package org.qcmg.qbamfix;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NewOptionsTest {
	public static final String INPUT_FILE_NAME = "./input.sam";
	public static final String INPUT_NON_EXIST = "./input.bam";
	public static final String OUTPUT_SAM_NAME = "./output.sam";
	public static final String OUTPUT_FILE_NAME = "./output.bam";
	public static final String LOG_FILE_NAME = "./output.log";
	
	@Test 
	public void checkSameIOTest() throws Exception{
		final String[] args = {"-i",INPUT_FILE_NAME, "-o", INPUT_FILE_NAME, "--log", LOG_FILE_NAME};
	
		NewOptions myOpt = new NewOptions(args);
		
		try{
			myOpt.checkFiles();
		}catch(Exception e){
//			System.out.println("e.toString: " + e.toString());
//			System.out.println("Message: " + Messages.getMessage("INPUT_SAME_OUTPUT",INPUT_FILE_NAME,INPUT_FILE_NAME));
			Assert.assertTrue(e.getMessage().contains(
					Messages.getMessage("INPUT_SAME_OUTPUT",new File(INPUT_FILE_NAME).getAbsolutePath(), 
							new File(INPUT_FILE_NAME).getAbsolutePath() )));
		}
		
	}
	@Test  
	public void checkSamOutputTest() throws Exception{
		final String[] args = {"-i",INPUT_FILE_NAME, "-o", OUTPUT_SAM_NAME, "--log", LOG_FILE_NAME};
	
		NewOptions myOpt = new NewOptions(args);

		try{
			myOpt.checkFiles();
		}catch(Exception e){
			Assert.assertTrue(e.getMessage().contains(
				Messages.getMessage("NONSUPPORTED_SAM_OUTPUT", new File(OUTPUT_SAM_NAME).getName()) ));
		} 
	}
	
	@Test  
	public void checkNonExistInputTest() throws Exception{
		final String[] args = {"-i",INPUT_NON_EXIST, "-o", OUTPUT_SAM_NAME, "--log", LOG_FILE_NAME};
	
		NewOptions myOpt = new NewOptions(args);

		try{
			myOpt.checkFiles();
		}catch(Exception e){
//			System.out.println(e.getMessage());
//			System.out.println("Message: " + Messages.getMessage("NONEXIST_INPUT_FILE", new File(INPUT_NON_EXIST).getAbsolutePath()));
			Assert.assertTrue(e.getMessage().contains(
				Messages.getMessage("NONEXIST_INPUT_FILE", new File(INPUT_NON_EXIST).getAbsolutePath()) ));
		} 
	}
	@AfterClass
	public static void deleteInput() throws IOException{
		new File(INPUT_FILE_NAME).delete();
	}
	
	@BeforeClass 
	public static void createInput() throws IOException{
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
        
        
        //debug
        System.out.println("get parent: " + new File(INPUT_NON_EXIST).getParent());
        System.out.println("get name: " + new File(INPUT_NON_EXIST).getName());
        System.out.println("get absolute path: " + new File(INPUT_NON_EXIST).getAbsolutePath());
        System.out.println("get Canonical path: " + new File(INPUT_NON_EXIST).getCanonicalPath());
	}
	
  
	
}
