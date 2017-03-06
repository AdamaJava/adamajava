package org.qcmg.cnv;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MtCountsTest {
	
	public static final String[] INPUTs = { "input.normal.bam","input.tumor.bam"};
	public static final String output = "output.counts";
 	
	String dir = new java.io.File( "." ).getAbsolutePath() + "/";

	@After
	public void deleteIO() throws IOException{
		File dir = new java.io.File( "." ).getCanonicalFile();
		File[] files =dir.listFiles();
		if (null != files) {
			for(File f: files) {
			    if(  f.getName().startsWith("input") || f.getName().startsWith("output") ) {
			    		f.delete();
			    }
			}
		}
	}
	
	@Before
	public void createIO(){
		TestFiles.CreateBAM(INPUTs[0], INPUTs[1]);
	}
	
	@Test
	public void testNoQuery() throws Exception{
		
		String[] ids = new String[] {"tumor" ,"normal"};
		String[] args = new String[] {"-i",  INPUTs[0], "-i", INPUTs[1], "--id", ids[0] , "--id", ids[1], "-o" , output};
		 		
        Options option = new Options(args);        	
		MtCounts cnvThread = new MtCounts(option.getInputNames(), option.getSampleIds(), 
				option.getOutputName(), option.getThreadNumber(), option.getWindowSize(), option.getQuery(),  option.getLogger(args));			
		cnvThread.callCounts();	
		
		String content;	
		int lineNo = 0; 
	    try (
	    		Scanner scanner = new Scanner(new FileReader(new File(output))) ) {
	    	
	    	scanner.useDelimiter("\\n");
	    	
	    	while(scanner.hasNext()){
	    		lineNo ++;	    		
	    		content = scanner.next();		    		
	    		if(content.startsWith("GL000195.1")){
	    			System.out.println("GL000195.1 " + lineNo + " " + content);
	    			assertTrue(content.endsWith("\t0\t0"));	    		
	    		}else if(content.startsWith("GL000196.1")){
	    			 String[] eles = content.split("\t");
	    			 if(lineNo == 21){
	    				 assertTrue(eles[5].equals("8"));
	    				 assertTrue(eles[6].equals("4"));
	    			 }else if(lineNo == 24){
	    				 assertTrue(eles[5].equals("1"));
	    				 assertTrue(eles[6].equals("1"));
	    			 }else{
	    				 assertTrue(eles[5].equals("0"));
	    				 assertTrue(eles[6].equals("0"));	    				 
	    			 }
	    		} 
	    	} //end while	    	
	    } //end try		
	}
	
	@Test
	public void testQuery() throws Exception{
						 
		String[] ids = {"tumor" ,"normal"};
		String[] args = {"-i", INPUTs[0],"-i", INPUTs[1], "--id", ids[0] , "--id", ids[1], "-o" , output, "-q", "and(RNAME =~ GL00019*, cigar_M >= 50)"};
		
        Options option = new Options(args);        	
		MtCounts cnvThread = new MtCounts(option.getInputNames(), option.getSampleIds(), 
				option.getOutputName(), option.getThreadNumber(), option.getWindowSize(), option.getQuery(),  option.getLogger(args));			
		cnvThread.callCounts();	
		
		String content;	
		int lineNo = 0;
	    try (
	    		Scanner scanner = new Scanner(new FileReader(new File(output))) ) {
	    	
	    	scanner.useDelimiter("\\n");
	    	while(scanner.hasNext()){	 
	    		lineNo ++;
	    		content = scanner.next();
	    		if(content.startsWith("GL000195.1"))
	    			assertTrue(content.endsWith("\t0\t0"));	    			    		
	    		else if(content.startsWith("GL000196.1")){
	    			 String[] eles = content.split("\t");
	    			 if(lineNo == 21){
	    				 assertTrue(eles[5].equals("3"));
	    				 assertTrue(eles[6].equals("3"));
	    			 }else{
	    				 assertTrue(eles[5].equals("0"));
	    				 assertTrue(eles[6].equals("0"));	    				 
	    			 }
	    		} 
	    	} //end while	    	
	    }//end try
		
	}	
	

}
