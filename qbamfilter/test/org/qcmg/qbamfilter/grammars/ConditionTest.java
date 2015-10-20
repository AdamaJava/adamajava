package org.qcmg.qbamfilter.grammars;


import java.io.File;



import org.junit.*;

import static org.junit.Assert.*;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.filter.SamRecordFilter;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.filter.TestFile;

public class ConditionTest {
	private final String para = "option_ZM";
    private final String tag = "ZM";
    private final String value = "1";
	
	@BeforeClass
    public static void before(){
        //create testing data regardless whether it exists or not, Since old testing data maybe damaged.
        TestFile.CreateBAM(TestFile.INPUT_FILE_NAME);
    }
	
	@AfterClass
    public static void after1(){
          new File(TestFile.INPUT_FILE_NAME).delete();
     }
	/**
     * Here we used an invalid comparator for testing
     * expected happened an exception
     */
    @Test(expected=Exception.class)
    public void testConstrutor() throws Exception{
        //test on wrong operator
        String op = "=";
        new Condition(para, op, value).getFilter();
    }
    
    
    ///??????????debuging
    @Test
    public void testWildCase() throws Exception{ 
    	String[] Operators = {"=~", "!~" };
    	String[] Values = {"chr*", "*11", "chr1"};
    	
    	final String RNAME = "RNAME";
    	final String MRNM = "MRNM";
    	
    	//5 reads "RNAME =~ chr*"; 1 read "RNAME =~ chr11"; 4 reads "RNAME !~ chr11"    	
    	 SamRecordFilter filter1 = new Condition(RNAME, Operators[0], Values[0]).getFilter();  
    	 SamRecordFilter filter2 = new Condition(RNAME, Operators[0], Values[2]).getFilter();
    	 SamRecordFilter filter3 = new Condition(RNAME, Operators[1], Values[2]).getFilter(); 
    	 assertTrue( check(filter1) == check(filter2) + check(filter3));   
    	 
    	 filter1 = new Condition(MRNM, Operators[1], Values[0]).getFilter();  
    	 filter2 = new Condition(MRNM, Operators[1], Values[2]).getFilter();
    	 assertTrue( check(filter1) == check(filter2)  );   

    	 
    
    }
    
    @Test
    public void testCheckOut() throws Exception{
        String[] Operators = {"==", ">=", "<=" , ">", "<","!="};
        int[] result = new int[6];

        for(int i = 0; i < 6; i ++){
            String op = Operators[i];
            SamRecordFilter filter = new Condition(para, op, value).getFilter();
             
            result[i] = check(filter);
        }

        assertTrue(result[0] == 1);   //==
        assertTrue(result[1] == 3);   //>=
        assertTrue(result[2] == 1);   //<=
        assertTrue(result[3] == 2);   //>
        assertTrue(result[4] == 0);   //<
        assertTrue(result[5] == 2);   //!= it skip the read without this field
    }
    private int check(SamRecordFilter filter ){
        int r = 0;
        try{

            SamReader inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));
            for(SAMRecord record : inreader){
                if(record.getAttribute(tag) == null){continue;}
                if(filter.filterOut(record)){
                    r ++;
                }
            }
            inreader.close();
          }catch(Exception ex){
            System.out.println(ex.getClass()+ "\nwith message: " + ex.getMessage() );
          }
        
          return r;

    }
    
    @Ignore
    public void testStringSplitter() {
    	String testString = "MD_mismatch";
    	String[] params;
    	long start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		params = testString.split("_");
    		assertTrue(params[0].equals("MD"));
    		assertTrue(params[1].equals("mismatch"));
    	}
    	System.out.println("time taken using String split: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	String firstElement, secondElement;
    	for (int i = 0 ; i < 1000000 ; i++) {
    		int underscorePosition = testString.indexOf("_");
    		firstElement = testString.substring(0, underscorePosition);
    		secondElement = testString.substring(underscorePosition+1);
    		assertTrue(firstElement.equals("MD"));
    		assertTrue(secondElement.equals("mismatch"));
    	}
    	System.out.println("time taken using String substring: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		params = testString.split("_");
    		assertTrue(params[0].equals("MD"));
    		assertTrue(params[1].equals("mismatch"));
    	}
    	System.out.println("time taken using String split: " + (System.currentTimeMillis() - start));
    	
    }

    @AfterClass
    public static void after(){
          new File(TestFile.INPUT_FILE_NAME).delete();
     }
    
    @Ignore
    public void testConditionTemplate() {
    	int count = 1000000;
    	String key = "CIGAR";
    	String comp = ">=";
    	String value = "15";
    	
    	long start = System.currentTimeMillis();
    	for (int i = 0 ; i < count ; i++) {
    		
    		try {
				Condition ct = new Condition(key, comp, value);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	System.out.println("ConditionTemplate time: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < count ; i++) {
    		
    		String compKey = key + comp+ value;
    	}
    	System.out.println("String time: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < count ; i++) {
    		
    		try {
				Condition ct = new Condition(key, comp, value);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	System.out.println("ConditionTemplate time: " + (System.currentTimeMillis() - start));
    	
    	
    }
   
}
