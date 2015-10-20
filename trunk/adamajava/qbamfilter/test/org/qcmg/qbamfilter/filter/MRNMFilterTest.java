package org.qcmg.qbamfilter.filter;

import org.junit.*;
import org.qcmg.picard.SAMFileReaderFactory;

import static org.junit.Assert.*;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.filter.SamRecordFilter;

import java.io.File;

public class MRNMFilterTest {
    @BeforeClass
    public static void before(){
        TestFile.CreateBAM(TestFile.INPUT_FILE_NAME);
    }

    @AfterClass
    public static void after(){
        new File(TestFile.INPUT_FILE_NAME).delete();
    }

    @Test
    public void testRNMAE() throws Exception{
            int EqualNum = 0;
            int UnequalNum = 0;
            SamRecordFilter filter1 = new MRNMFilter(Comparator.Equal, "rname");
            SamRecordFilter filter2 = new MRNMFilter(Comparator.NotEqual, "RNAME");

            SamReader Inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
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

}
