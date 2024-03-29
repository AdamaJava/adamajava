package org.qcmg.qbamfilter.filter;

import org.junit.*;
import org.qcmg.picard.SAMFileReaderFactory;

import static org.junit.Assert.*;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.filter.SamRecordFilter;

import java.io.File;

public class PosFilterTest {
    final static int start1 = 10167;
    final static int start2 = 10176;

    @BeforeClass
    public static void before(){
        TestFile.createBAM(TestFile.INPUT_FILE_NAME);
    }

    @AfterClass
    public static void after(){
        new File(TestFile.INPUT_FILE_NAME).delete();
    }
   
    /**
     * In this testing case, we check total number of reads with alignment start
     * position in [10167, 10176];
     */ 
    @Test
    public void testFilterOut() throws Exception{
        Comparator op1 = Comparator.GreatEqual;
        String value1 = Integer.toString(start1);
        Comparator op2 = Comparator.SmallEqual;
        String value2 = Integer.toString(start2);
        
        int NumCheck = 0;
        int NumRealRecord = 0;
        SamRecordFilter filter1 = new PosFilter(op1, value1);
        SamRecordFilter filter2 = new PosFilter(op2, value2);   
        SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        for(SAMRecord re : reader){
            int start = re.getAlignmentStart();
           if((start >= start1) && (start <= start2) ){
               NumRealRecord ++;
           }

           if(filter1.filterOut(re) && filter2.filterOut(re)){
                NumCheck ++;
           }
        }
        reader.close();
        
        //check there is only one record will be filter
        assertEquals(NumCheck, NumRealRecord);
    }
    
    /**
     * In this testing case, we set an invalid read:
     * reference * and start position 100; query: pos == 100
     * but our filter still return true;
     * so before run this filter, we must check whether this read isValid or not.
     * see htsjdk.samtools.SAMRecord::isValid()
     */
    @Test
    public void testInvalidMapQ1() throws Exception{
        try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME))) {
            SamRecordFilter filter = new PosFilter(Comparator.Equal, "100");
            for (SAMRecord re : reader) {
                re.setReferenceName("*");
                re.setAlignmentStart(100);
                assertNotNull(re.isValid());
                assertTrue(filter.filterOut(re));
            }
        }
    }
        
    /**
     * In this testing case, we set an invalid read:
     * reference chr1 and start position 0; query: pos == 0
     * but our filter still return true;
     * so before run this filter, we must check whether this read isValid or not.
     * see htsjdk.samtools.SAMRecord::isValid()
     */
    @Test
    public void testInvalidMapQ2() throws Exception{
        try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME))) {
            SamRecordFilter filter = new PosFilter(Comparator.Equal, "0");
            for (SAMRecord re : reader) {
                re.setAlignmentStart(0);
                assertNotNull(re.isValid());
                assertTrue(filter.filterOut(re));
            }
        }
    }
}
