package org.qcmg.qbamfilter.filter;

import java.io.File;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.picard.SAMFileReaderFactory;

import static org.junit.Assert.*;


public class QualFilterTest {
	
 @BeforeClass
    public static void before(){
        //create testing data regardless whether it exists or not, Since old testing data maybe damaged.
        TestFile.createBAM(TestFile.INPUT_FILE_NAME);
    }

    @AfterClass
    public static void after(){
        new File(TestFile.INPUT_FILE_NAME).delete();
    }

    @Test
    public void GetTest() throws Exception{
        Comparator op = Comparator.GreatEqual;
        String value = "20";
        SamRecordFilter filter = new QualFilter("average",op, value);
        SamReader Inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));

        for(SAMRecord re : Inreader){
        	char[] quals =   re.getBaseQualityString().toCharArray();
        	byte[] bQuals = re.getBaseQualities();
        	
        	byte[] sQuals = new byte[quals.length];
        	for (int i = 0; i < quals.length; i ++)
        		sQuals[i] = (byte) quals[i];

            assertEquals(getAverage(sQuals) - 33, getAverage(bQuals));
        	if(getAverage(sQuals)  >= 53)
        		assertTrue(filter.filterOut(re));	 
        }
        
        Inreader.close();
    }
    private int getAverage(byte[] qualities){
    	
    	int total = 0;
    	for (byte q : qualities) total += q;
    	return total / qualities.length;
    }

    
    @Test
    public void setTest() throws Exception{
    	
    	//only read the first read
        SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        SAMRecord record = null ;
        for( SAMRecord re : reader){
             record = re;
             break;
        }
        reader.close();

        assertNotNull(record);
        //create a base quality array
        byte[] qualities = new byte[record.getReadLength()];
        for(int i = 0; i < record.getReadLength(); i ++)
        	qualities[i] = 20;
        record.setBaseQualities(qualities);
        
       // test smaller case
        SamRecordFilter filter = new QualFilter("average", Comparator.Small, "20");
        assertFalse(filter.filterOut(record));	 
        
      //  test equal case
        filter = new QualFilter("average", Comparator.Equal, "20");
        assertTrue(filter.filterOut(record));	
        
       //change a base quality value and test again
        qualities[0] = 19;
        record.setBaseQualities(qualities);
        filter = new QualFilter("average", Comparator.Small, "20");
        assertTrue(filter.filterOut(record));
        filter = new QualFilter("average", Comparator.Equal, "20");
        assertFalse(filter.filterOut(record));	
    }
    
    @Test
    public void testFilterLowQual() throws Exception {
    	SAMRecord rec = new SAMRecord(null);
    	rec.setBaseQualityString("###");		// # has an ascii score of 35, phred score of 2

        assertTrue(new QualFilter("average", Comparator.Small, "20").filterOut(rec));
        assertTrue(new QualFilter("average", Comparator.SmallEqual, "2").filterOut(rec));
        assertFalse(new QualFilter("average", Comparator.Great, "20").filterOut(rec));
    }
    
    @Test
    public void testFilterHighQual() throws Exception {
    	SAMRecord rec = new SAMRecord(null);
    	String quality = "BBBFFFFFFFFFFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIFFFFIIIIIIIIFFFFFFFFFFFBBBBBB<BBBBBBBBBBB7BBB<'0BBBBB";
    	rec.setBaseQualityString(quality);			// average is 69 ascii -> 36 fasta

        assertTrue(new QualFilter("average", Comparator.Great, "30").filterOut(rec));
        assertTrue(new QualFilter("average", Comparator.Great, "35").filterOut(rec));
        assertTrue(new QualFilter("average", Comparator.GreatEqual, "36").filterOut(rec));
        assertFalse(new QualFilter("average", Comparator.GreatEqual, "37").filterOut(rec));
        assertTrue(new QualFilter("average", Comparator.Small, "100").filterOut(rec));
        assertTrue(new QualFilter("average", Comparator.Small, "37").filterOut(rec));
        assertFalse(new QualFilter("average", Comparator.Small, "36").filterOut(rec));
        assertFalse(new QualFilter("average", Comparator.Small, "0").filterOut(rec));
    }
}
