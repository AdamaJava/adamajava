package org.qcmg.qbamfilter.filter;

import org.junit.*;
import static org.junit.Assert.*;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.picard.filter.SamRecordFilter;
import java.io.File;

public class RNameFilterTest {
    @BeforeClass
    public static void before(){
        TestFile.CreateBAM(TestFile.INPUT_FILE_NAME);
    }

    @AfterClass
    public static void after(){
        new File(TestFile.INPUT_FILE_NAME).delete();
    }
    
    /**
     * In this testing case, we check total number of reads which mapped on chr1
     * we also set reference to '*' and "chr11", check for query: RNAME != chr1
     */
    @Test
    public void testFilterOut() throws Exception{
        int NumCheck = 0;
        int NumRealRecord = 0;
        SamRecordFilter filter1 = new RNameFilter(Comparator.Equal, "chr1");
        SamRecordFilter filter2 = new RNameFilter(Comparator.NotEqual, "chr1");
        SAMFileReader Inreader = new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        for(SAMRecord re : Inreader){
            String chr = re.getReferenceName();
           if(chr.equalsIgnoreCase("chr1") ){
               NumRealRecord ++;
           }
           if(filter1.filterOut(re)){
                NumCheck ++;
           }
            
           //set reference to *, may cause this read invalid but it still work for filter      
           re.setReferenceName("*"); 
           assertTrue(filter2.filterOut(re));
           assertFalse(filter1.filterOut(re));         
           re.setReferenceName("chr11");
           assertTrue(filter2.filterOut(re));
           assertFalse(filter1.filterOut(re));
        }
        Inreader.close();

        //check there is only one record will be filter
        assertTrue(NumCheck == NumRealRecord);
    }
    @Test
    public void testMRNMOut() throws Exception{
        int EqualNum = 0;
        int UnequalNum = 0;
        SamRecordFilter filter1 = new RNameFilter(Comparator.Equal, "mrnm");
        SamRecordFilter filter2 = new RNameFilter(Comparator.NotEqual, "MRNM");

        SAMFileReader Inreader = new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        for(SAMRecord re : Inreader){
            String ref = re.getReferenceName();
            //picard will convert "=" to string from reference name
            String mref = re.getMateReferenceName();
            if( ref.equals(mref) ){
                assertTrue(filter1.filterOut(re));
                assertFalse(filter2.filterOut(re));
                EqualNum ++;
            }
            else{
                System.out.println(ref + " compare with " + mref);
                assertTrue(filter2.filterOut(re));
                assertFalse(filter1.filterOut(re));
                UnequalNum ++;
            }
        }
        Inreader.close();
        assertTrue(EqualNum == 3);
        assertTrue(UnequalNum == 2);

    }
    /**
     * In this testing case, we set an invalid comparator: Greater
     */
    @Test(expected=Exception.class)
    public void testFalseRef() throws Exception{
        new RNameFilter(Comparator.Great, "chr1");
 
    }
}
