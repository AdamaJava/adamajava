package org.qcmg.qbamfilter.filter;

 
import static org.junit.Assert.assertTrue;

import java.io.File;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.picard.SAMFileReaderFactory;

public class ISIZETest {
    @BeforeClass
    public static void before(){
        TestFile.CreateBAM(TestFile.INPUT_FILE_NAME);
    }

    @AfterClass
    public static void after(){
        new File(TestFile.INPUT_FILE_NAME).delete();
    }
    
    /**
     * In this testing case, we check total number of reads with mapping quality
     * higher and equal than 20 
     */
    @Test
    public void testFilterOut() throws Exception{
        Comparator op = Comparator.GreatEqual;
        String value = "20";

        SamRecordFilter filter = new IsizeFilter(op, value);
        SamReader Inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        int i = 0;
        int NumRealRecord = 0;

        for(SAMRecord re : Inreader){
           if(re.getInferredInsertSize() >= 20){ 
               NumRealRecord ++;
           }

           if(filter.filterOut(re)){
                i ++;
           }
        }

        //check there is only one record will be filter
        assertTrue(i == NumRealRecord);
        Inreader.close();
    }

    /**
     * In this testing case, we set an invalid mapq 256, but our filter still return true;
     * so before run this filter, we must check whether this read isValid or not.
     * see htsjdk.samtools.SAMRecord::isValid()
     */
    @Test
    public void testNegativeValue() throws Exception{
        try (SamReader Inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));){    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));) {
	        SamRecordFilter filter = new IsizeFilter(Comparator.Great, "-101");
	        for(SAMRecord re : Inreader){
	        		re.setInferredInsertSize(-100);
	            assertTrue(filter.filterOut(re));
	       }
        }

    }
}
