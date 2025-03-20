package org.qcmg.qbamfilter.filter;

import htsjdk.samtools.*;
import org.junit.*;
import org.qcmg.picard.SAMFileReaderFactory;

import static org.junit.Assert.*;

import htsjdk.samtools.filter.SamRecordFilter;

import java.io.File;

public class CigarFilterTest {
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
    public void validTest(){
        String[] CigarOPs = {"M","I", "D", "N", "S","H","P"};
        
        Comparator[] comps = {Comparator.Great, Comparator.SmallEqual, Comparator.GreatEqual,
            Comparator.Equal,Comparator.Small, Comparator.NotEqual,Comparator.Equal};
        
        String[] values = {"25", "8", "2","200","8","0","0" };
        int[] vs = {4,5,1,1,4,4,5};

        for(int i = 0; i < 7; i++){
           try{ 
                SamRecordFilter filter = new CigarFilter(CigarOPs[i], comps[i], values[i]);                
				SamReader Inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    
                int num = 0;
            
                for(SAMRecord re : Inreader){
                   if(filter.filterOut(re)){
                       num ++;
                   }
                }
               assertEquals(num, vs[i]);
                Inreader.close();           
           }catch(Exception e){
                System.out.println(e.getMessage());
               fail();
            }
        }
    }

    @Test
    public void someCIGARValues() throws Exception {
        Cigar c = new Cigar();
        c.add(new CigarElement(150, CigarOperator.M));
        SAMRecord sam = new SAMRecord(new SAMFileHeader());
        sam.setCigar(c);
        SamRecordFilter filter = new CigarFilter("M", Comparator.GreatEqual, "34");
        assertTrue(filter.filterOut(sam));

        c = new Cigar();
        c.add(new CigarElement(33, CigarOperator.M));
        sam.setCigar(c);
        assertFalse(filter.filterOut(sam));

        c = new Cigar();
        c.add(new CigarElement(34, CigarOperator.M));
        sam.setCigar(c);
        assertTrue(filter.filterOut(sam));

        c = new Cigar();
        c.add(new CigarElement(10, CigarOperator.M));
        sam.setCigar(c);
        assertFalse(filter.filterOut(sam));
        c.add(new CigarElement(10, CigarOperator.M));
        sam.setCigar(c);
        assertFalse(filter.filterOut(sam));
        c.add(new CigarElement(10, CigarOperator.M));
        sam.setCigar(c);
        assertFalse(filter.filterOut(sam));
        c.add(new CigarElement(10, CigarOperator.M));
        sam.setCigar(c);
        assertTrue(filter.filterOut(sam));
    }
    
    /**
    *Test the exception case for invalid cigar operator "o"
    */
    @Test(expected=Exception.class)
    public void invalidTest() throws Exception {
        new CigarFilter("o", Comparator.Equal, "0");
     }
     
    
}
