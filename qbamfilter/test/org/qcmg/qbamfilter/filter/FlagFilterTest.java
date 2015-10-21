/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qcmg.qbamfilter.filter;


import org.junit.*;
import static org.junit.Assert.*;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.picard.filter.SamRecordFilter;
import java.io.File;

public class FlagFilterTest {    
    @BeforeClass
    public static void before(){
        //create testing data regardless whether it exists or not, Since old testing data maybe damaged.
        TestFile.CreateBAM(TestFile.INPUT_FILE_NAME);
    }

    @AfterClass
    public static void after(){
        new File(TestFile.INPUT_FILE_NAME).delete();
    }

    /**
     * In this testing data we compare the binary base of the flag with the FlagFilter by using picard
     * Here we assume all read with valid Flag, so some unproper query can be executed as well.
     * eg. Flag_ProperPaire != true; // it return false if it isn't a Paired read rather exception
     */
    @Test
    public void ValidTest(){
        String[] FlagName = {
            "ReadPaired", "ProperPair","ReadUnmapped", "Mateunmapped",
            "ReadNegativeStrand",  "MateNegativeStrand",  "FirstOfpair", "SecondOfpair",
            "NotprimaryAlignment",  "ReadFailsVendorQuality", "DuplicateRead"};

        Comparator[] comps = {
            Comparator.Equal, Comparator.Equal, Comparator.NotEqual,Comparator.NotEqual,
            Comparator.Equal, Comparator.Equal, Comparator.NotEqual,Comparator.NotEqual,
            Comparator.Equal, Comparator.NotEqual,Comparator.Equal};

        String[] values = { "true", "1","false","0","false","true","false","true","true","0","1"};

        //The number of satisfied reads for each 11 query is:
        //int[] vs = {4,3,0,0,1,3,2,3,0,0,1};

        for(int i = 0; i < 11; i++){
           try{
               int Fnum = CountFilterReads(FlagName[i], comps[i], values[i]);
               int Vnum = CountValidReads( i, comps[i], values[i]);
               assertTrue( Fnum == Vnum);
           }catch(Exception e){
                System.out.println(e.getMessage());
                assertTrue(false);
            }
        }

    }
    private int CountValidReads(int FlagPos, Comparator comp, String value) throws Exception{
        String FlagValue;
        if(value.equalsIgnoreCase("true") || value.equals("1")){
            FlagValue = "1";
        }
        else if(value.equalsIgnoreCase("false") || value.equals("0")){
            FlagValue = "0";
        }
        else{
            throw new Exception("invalid value used in query!");
        }

        SAMFileReader Inreader = new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        int num = 0;
        for(SAMRecord re : Inreader){
            if( re.isValid() != null){
                continue;
            }
            String binFlag = Integer.toBinaryString(re.getFlags());
            String reverse = new StringBuffer(binFlag).reverse().toString();
            while(reverse.length() <= 11){
                reverse = reverse.concat("0");
            }
            String BaseValue = reverse.substring(FlagPos, FlagPos + 1);

            if( comp.eval(BaseValue,FlagValue) ){
                num ++;
            }
        }
        Inreader.close();

        return num;
    }
    private int CountFilterReads(String FlagName, Comparator comp, String value) throws Exception{

        SamRecordFilter filter = new FlagFilter(FlagName, comp, value);
        SAMFileReader Inreader = new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        int num = 0;

        for(SAMRecord re : Inreader){
            if( re.isValid() != null){
                continue;
            }
           if(filter.filterOut(re)){
               num ++;
           }
        }
        Inreader.close();

        return num;
    }

    /**
     * test invalid flag value: "ok"
     */
    @Test(expected = Exception.class)
    public void InvalidValueTest()throws Exception{
       SamRecordFilter filter = new FlagFilter("ReadPaired", Comparator.Equal, "ok");
    }

    /**
     * test on invalid comparator: Great
     */
    @Test(expected = Exception.class)
    public void InvalidCompTest() throws Exception{
       SamRecordFilter filter = new FlagFilter("ReadPaired", Comparator.Great, "1");

    }
    
    /**
     * test invalid flag name: ReadNotPair
     */
    @Test(expected = Exception.class)
    public void InvalidFlagTest() throws Exception{
       SamRecordFilter filter = new FlagFilter("ReadNotPair", Comparator.NotEqual, "1");

    }
}
