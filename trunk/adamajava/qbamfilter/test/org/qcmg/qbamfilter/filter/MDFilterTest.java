/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qcmg.qbamfilter.filter;

import static org.junit.Assert.assertTrue;

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
    public void ValidTest() throws Exception{
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
                Pattern p = Pattern.compile("\\d[ATGCN]");
                Matcher m = p.matcher(re.getAttribute("MD").toString());
                int mis = 0;
                while(m.find()){mis ++ ;}
                assertTrue( mis == mis_assum[i]);
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
        assertTrue(NumTrueRead == Less3Mis);
        Inreader.close();


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
                assertTrue( mis == mis_assum[i]);
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
        assertTrue(NumTrueRead == Less3Mis);
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
             assertTrue( 0 == mis);
    	}
    	System.out.println("time taken '2' OLD: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		int mis = tallyMDMismatches(mdString);
             assertTrue( 0 == mis);
    	}
    	System.out.println("time taken '2' NEW: " + (System.currentTimeMillis() - start));
    	
//    	start = System.currentTimeMillis();
//    	for (int i = 0 ; i < 1000000 ; i++) {
//    		int mis = tallyMDMismatchesNEW(mdString);
//    		assertTrue( 0 == mis);
//    	}
//    	System.out.println("time taken '2' NEW NEW: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
             assertTrue( 0 == mis);
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
             assertTrue( 2 == mis);
    	}
    	System.out.println("time taken '500' OLD: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		int mis = tallyMDMismatches(mdString);
             assertTrue( 2 == mis);
    	}
    	System.out.println("time taken '500' NEW: " + (System.currentTimeMillis() - start));
//    	start = System.currentTimeMillis();
//    	for (int i = 0 ; i < 1000000 ; i++) {
//    		int mis = tallyMDMismatchesNEW(mdString);
//    		assertTrue( 2 == mis);
//    	}
//    	System.out.println("time taken '500' NEW NEW: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
             assertTrue( 2 == mis);
    	}
    	System.out.println("time taken '500' OLD: " + (System.currentTimeMillis() - start));
    	
    	mdString = "17C2^CCACTTAATTTCATGTGATAATTTTCCCCAATGACTAACCCACTATTATATAAATCAATTCTTTCTTAATGCC" +
		 "30A";
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
             assertTrue( 2 == mis);
    	}
    	System.out.println("time taken '84' OLD: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		int mis = tallyMDMismatches(mdString);
             assertTrue( 2 == mis);
    	}
    	System.out.println("time taken '84' NEW: " + (System.currentTimeMillis() - start));
//    	start = System.currentTimeMillis();
//    	for (int i = 0 ; i < 1000000 ; i++) {
//    		int mis = tallyMDMismatchesNEW(mdString);
//    		assertTrue( 2 == mis);
//    	}
//    	System.out.println("time taken '500' NEW NEW: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
             assertTrue( 2 == mis);
    	}
    	System.out.println("time taken '84' OLD: " + (System.currentTimeMillis() - start));
    	
    	mdString = "39A6^TGCTGTGGCC4T0";
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
             assertTrue( 2 == mis);
    	}
    	System.out.println("time taken '18' OLD: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		int mis = tallyMDMismatches(mdString);
             assertTrue( 2 == mis);
    	}
    	System.out.println("time taken '18' NEW: " + (System.currentTimeMillis() - start));
//    	start = System.currentTimeMillis();
//    	for (int i = 0 ; i < 1000000 ; i++) {
//    		int mis = tallyMDMismatchesNEW(mdString);
//    		assertTrue( 2 == mis);
//    	}
//    	System.out.println("time taken '18' NEW NEW: " + (System.currentTimeMillis() - start));
    	
    	start = System.currentTimeMillis();
    	for (int i = 0 ; i < 1000000 ; i++) {
    		Matcher m = p.matcher(mdString);
    		 int mis = 0;
             while(m.find()){mis ++ ;}
             assertTrue( 2 == mis);
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
    
    public static int tallyMDMismatchesNEW(String mdData) {
    	int count = 0;
    	boolean validPrecurser = false;
    	for (char c : mdData.toCharArray()) {
    		if (Character.isDigit(c)) {
    				validPrecurser = true;
    			} else {
    			if (validPrecurser) count++;
    				validPrecurser = false;
    			}
    		}
//		if (null != mdData) {
//			for (int i = 0, size = mdData.length() ; i < size ; ) {
//				
//				if (isValidMismatch(mdData.charAt(i))) {
//					count++;
//					i++;
//				} else if ('^' == mdData.charAt(i)) {
//					while (++i < size && Character.isLetter(mdData.charAt(i))) {}
//				} else i++;	// need to increment this or could end up with infinite loop...
//			}
//		}
		return count;
	}

//	private static boolean isInValidExtendedInDelete(char c) {
//		if (! isInValidExtended(c))
//			return c == 'M' || c =='R';
//		else return true;
//	}


    /**
    *Test the exception case for invalid cigar operator "o"
    */
//    @Test(expected=Exception.class)
//    public void InvalidTest() throws Exception{
//       //  SamRecordFilter filter = new CigarFilter("o", Comparator.Equal, "0");
//     }
}
