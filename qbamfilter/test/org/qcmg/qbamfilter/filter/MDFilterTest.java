/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qcmg.qbamfilter.filter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.picard.SAMFileReaderFactory;

public class MDFilterTest {
 @BeforeClass
    public static void before(){
        //create testing data regardless whether it exists or not, Since old testing data maybe damaged.
        TestFile.CreateBAM_MD(TestFile.INPUT_FILE_NAME);
    }

    @AfterClass
    public static void after(){
        new File(TestFile.INPUT_FILE_NAME).delete();
    }

	@Test
	public void testTallyMDMismatchesRepeatedCalls() {
		// Define a sample MD data string
//		String mdData = "150";
		String mdData = "10A5^ACGT8G1N2T6N";
//		int expectedResult = 0; // 3 mismatches (A,G,T), plus 2 N's
		int expectedResult = 5; // 3 mismatches (A,G,T), plus 2 N's

		// Define how many times to call the method
		final int repeatCount = 1_000_000;

		// Measure performance across repeated calls
		long startTime = System.nanoTime();
		for (int i = 0; i < repeatCount; i++) {
			int result = MDFilter.tallyMDMismatches(mdData);
			assertEquals(expectedResult, result); // Ensure correctness
		}
		long endTime = System.nanoTime();

		// Print out the time taken
		System.out.printf("tallyMDMismatches executed %d times in %d ms%n",
				repeatCount, (endTime - startTime) / 1_000_000);
	}

	 @Test
	 public void commonMDValues() {
	 	 String md = "150";
		 assertEquals(0, MDFilter.tallyMDMismatches(md));
		 md = "10C100";
		 assertEquals(1, MDFilter.tallyMDMismatches(md));
		 md = "10C100A";
		 assertEquals(2, MDFilter.tallyMDMismatches(md));
		 md = "G10C100A";
		 assertEquals(3, MDFilter.tallyMDMismatches(md));
		 md = "G10C0C100A";
		 assertEquals(4, MDFilter.tallyMDMismatches(md));
		 md = "G10^CC100A";
		 assertEquals(2, MDFilter.tallyMDMismatches(md));
		 md = "G10^CC100A";
		 assertEquals(2, MDFilter.tallyMDMismatches(md));

	}


	@Test
    public void validTest() throws Exception{
	    Comparator op = Comparator.Small;
        String value = "3";
        SamRecordFilter filter = new MDFilter("mismatch",op, value);
        SamReader inReader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        int i = 0;
        int NumTrueRead = 0;
        int Less3Mis = 0;

        int[] mis_assum = {0, 100, 1,2,0,3};
        //int[] mis_results = new int[5];
        for(SAMRecord re : inReader){
           if( re.getAttribute("MD") != null){
                Pattern p = Pattern.compile("\\d[ATGCN]");
                Matcher m = p.matcher(re.getAttribute("MD").toString());
                int mis = 0;
                while(m.find()){mis ++ ;}
			   assertEquals(mis, mis_assum[i]);
                if(mis < 3){
                    Less3Mis ++;
                }
           }

           if(filter.filterOut(re)){
               NumTrueRead ++;
           }
           i++;
        }

        //check there is only one record will be filter
		assertEquals(NumTrueRead, Less3Mis);
        inReader.close();
    }
    
    
    @Test
    public void validTestNewMethod() throws Exception{
	    Comparator op = Comparator.Small;
        String value = "3";
        SamRecordFilter filter = new MDFilter("mismatch",op, value);
        SamReader Inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        int i = 0;
        int NumTrueRead = 0;
        int Less3Mis = 0;

        int[] mis_assum = {0, 100, 1,2,0,3};
        //int[] mis_results = new int[5];
        for(SAMRecord re : Inreader){
           if( re.getAttribute("MD") != null){
        	   int mis = tallyMDMismatches(re.getAttribute("MD").toString());
			   assertEquals(mis, mis_assum[i]);
                if(mis < 3){
                    Less3Mis ++;
                }
           }

           if(filter.filterOut(re)){
               NumTrueRead ++;
           }
           i++;
        }

        //check there is only one record will be filter
		assertEquals(NumTrueRead, Less3Mis);
        Inreader.close();


    }
    
    @Ignore
    public void testCompareOldAndNew() {
    	 Pattern p = Pattern.compile("\\d[ATGCN]");
    	String mdString = "50";
    	
    	long start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
			assertEquals(0, mis);
    	}
    	System.out.println("time taken '2' OLD: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		int mis = tallyMDMismatches(mdString);
			assertEquals(0, mis);
    	}
    	System.out.println("time taken '2' NEW: " + (System.currentTimeMillis() - start));

    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
			assertEquals(0, mis);
    	}
    	System.out.println("time taken '2' OLD: " + (System.currentTimeMillis() - start));
    	
    	mdString = "17C2^CCACTTAATTTCATGTGATAATTTTCCCCAATGACTAACCAAATATGCTTCACTATTATATAAATCAATTCTTTCTTAATGCC" +
		"ACAAGTGAAAGTGCAAAGGTAGCTAATGGTTTTCTTCTCATAAAAATCACACTTTGGCTTTTTCCTTTCATATGTAATTAATCATATT" +
		"TGTGACAATCTTCCAAACTTACTTGAAATTTTTCTGAATCCCTTTCAAATCAGGACAAGAACTAGAAATGTCTATACAGGTTTAATAT" +
		"GAAGTAAAGAAAATGTTTTTCATTTTCTTGATTTATTTCTGAATTCAGCTTGCTCTTCATTAGCGCTACATAGCTGMCTTATTATTCG" +
		"TGGTCCCCTATGACCCCCTGATCATTTTCCCTGAGGGTGCATATTTATTCACTAACTATGTTACAATCATGTGATCTGCTGGATTTT" +
		"TTCTGATAGTCTACTCTAGATTTGTTCTAAATTAATAAATCCCATTATTTTTG30A";
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
			assertEquals(2, mis);
    	}
    	System.out.println("time taken '500' OLD: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		int mis = tallyMDMismatches(mdString);
			assertEquals(2, mis);
    	}
    	System.out.println("time taken '500' NEW: " + (System.currentTimeMillis() - start));

    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
			assertEquals(2, mis);
    	}
    	System.out.println("time taken '500' OLD: " + (System.currentTimeMillis() - start));
    	
    	mdString = "17C2^CCACTTAATTTCATGTGATAATTTTCCCCAATGACTAACCCACTATTATATAAATCAATTCTTTCTTAATGCC" +
		 "30A";
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
			assertEquals(2, mis);
    	}
    	System.out.println("time taken '84' OLD: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		int mis = tallyMDMismatches(mdString);
			assertEquals(2, mis);
    	}
    	System.out.println("time taken '84' NEW: " + (System.currentTimeMillis() - start));

    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
			assertEquals(2, mis);
    	}
    	System.out.println("time taken '84' OLD: " + (System.currentTimeMillis() - start));
    	
    	mdString = "39A6^TGCTGTGGCC4T0";
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
			assertEquals(2, mis);
    	}
    	System.out.println("time taken '18' OLD: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		int mis = tallyMDMismatches(mdString);
			assertEquals(2, mis);
    	}
    	System.out.println("time taken '18' NEW: " + (System.currentTimeMillis() - start));

    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
			assertEquals(2, mis);
    	}
    	System.out.println("time taken '18' OLD: " + (System.currentTimeMillis() - start));
    }
    
    
    public static int tallyMDMismatches(String mdData) {
    	int count = 0;
		if (null != mdData) {
			for (int i = 0, size = mdData.length() ; i < size ; ) {
				char c = mdData.charAt(i);
				if (isValidMismatch(c)) {
					count++;
					i++;
				} else if ('^' == c) {
					while (++i < size && Character.isLetter(mdData.charAt(i))) {}
				} else i++;	// need to increment this or could end up with infinite loop...
			}
		}
		return count;
	}
    
    private static boolean isValidMismatch(char c) {
		return c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N';
	}

}
