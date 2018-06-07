package org.qcmg.qbamfilter.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.picard.SAMFileReaderFactory;

public class SeqFilterTest {
	@BeforeClass
    public static void before(){
        //create testing data regardless whether it exists or not, Since old testing data maybe damaged.
        TestFile.CreateBAM(TestFile.INPUT_FILE_NAME);
    }

    @AfterClass
    public static void after(){
        new File(TestFile.INPUT_FILE_NAME).delete();
    }
    
    @Test
    public void filterTest() throws Exception{
    	
    	//only read the first read
        SamReader Inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        SAMRecord record = null ;
        for( SAMRecord re : Inreader){
        		record = re;
        }
        Inreader.close();
        assertEquals(false, null == record);
        
        //create a base string
        String str = "ACCCTNNNNCTAACCCTAACCNTAACCCTAACCCAAC";
        record.setReadString(str);
        SamRecordFilter filter = new SeqFilter("numberN", Comparator.Small, "4");
        assertFalse(filter.filterOut(record));	 
        
        filter = new SeqFilter("numberN", Comparator.Equal, "5");
        assertTrue(filter.filterOut(record));	
        
        filter = new SeqFilter("numberN", Comparator.Great, "5");
        assertFalse(filter.filterOut(record)); 

    }
    @Test
    public void speedTest() {
    		String str = "CACTGCAGCCTTGTCCTTNNGGGCTCGAGCAATCCTCCCNCCTCAGCCTCACAAACAGTTGGGACTATAGGANNNN";
      	int count = str.length() - str.replace("N", "").replace("n","").length();
      	
      	//test replace method
	    	long start = System.currentTimeMillis();
	    	for (int i = 0 ; i < 10000 ; i++)  {
	    		assertTrue( str.length() - str.replace("N", "").replace("n","").length() == count);
	    	}
	    	System.out.println("time taken by replacement method: " + (System.currentTimeMillis() - start));
	    	
	    	start = System.currentTimeMillis();
	    	for (int i = 0 ; i < 10000 ; i++) {      		 
	    		assertTrue( count == countN(str));
	    	}
	    	System.out.println("time taken by counting method: " + (System.currentTimeMillis() - start));    
    }
    
	private int countN(String str){
	    	int count = 0;
	    	for (int i = 0; i < str.length(); i ++) {
	    		if( str.charAt(i) == 'N' ) {
	    			count ++;
	    		}
	    	}
	    	return count;
    }
    
    @Test
    public void testFilterOnEmptySequence() throws Exception {
	    	SAMRecord rec = new SAMRecord(null);
	    	rec.setReadString("");
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Small, "4").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Small, "1").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Small, "100000").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.SmallEqual, "0").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Small, "0").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.GreatEqual, "100000").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.GreatEqual, "0").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.GreatEqual, "1").filterOut(rec));
    }
    
    @Test
    public void testFilterOnSequenceNoNs() throws Exception {
	    	SAMRecord rec = new SAMRecord(null);
	    	rec.setReadString("ACGT");
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Small, "4").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Small, "1").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Small, "100000").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.SmallEqual, "0").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Small, "0").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.GreatEqual, "100000").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.GreatEqual, "0").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.GreatEqual, "1").filterOut(rec));
    }
    @Test
    public void testFilterOnSequenceNs() throws Exception {
	    	SAMRecord rec = new SAMRecord(null);
	    	rec.setReadString("N");
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Small, "4").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Small, "1").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.SmallEqual, "1").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.SmallEqual, "0").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Equal, "1").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.GreatEqual, "1").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Great, "0").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Great, "1").filterOut(rec));
	    	
	    	rec.setReadString("NNNN");
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Small, "4").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Small, "1").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.SmallEqual, "1").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.SmallEqual, "0").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Equal, "1").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.GreatEqual, "1").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Great, "0").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Great, "1").filterOut(rec));
	    	
	    	rec.setReadString("NNNNNNNNNN");
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Small, "4").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Small, "10").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.SmallEqual, "10").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.SmallEqual, "0").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Equal, "1").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.GreatEqual, "1").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Great, "0").filterOut(rec));
	    	assertEquals(true, new SeqFilter("numberN", Comparator.Great, "1").filterOut(rec));
	    	assertEquals(false, new SeqFilter("numberN", Comparator.Great, "1000").filterOut(rec));
    	
    }
}
