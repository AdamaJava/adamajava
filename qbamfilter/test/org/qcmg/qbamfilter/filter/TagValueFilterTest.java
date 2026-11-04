/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * @author q.xu
 */

package org.qcmg.qbamfilter.filter;

//import org.qcmg.qbamfilter.filter.TagValueFilter;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.qcmg.picard.SAMFileReaderFactory;

import static org.junit.Assert.*;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import java.io.File;



public class TagValueFilterTest {

    @Before
    public void before(){
        TestFile.createBAM(TestFile.INPUT_FILE_NAME);
    }

    /**
     * count the number of reads with optional field ZM:i:1
     */
    @Test
    public void testFilterOut() throws Exception{
        String tag = "ZM";
        String value = "1";

        Comparator op = Comparator.Equal;
        TagValueFilter filter = new TagValueFilter(tag, op, value);
        SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        int NunRealRecord = 1;
        int i = 0;
        for(SAMRecord re : reader){
           if(filter.filterOut(re)){
               //the filter out record must contain this tag value pair
               assertEquals(re.getAttribute(tag).toString(), value);
                i ++;
           }
        }

        //check there is only one record will be filter
        assertEquals(i, NunRealRecord);
        reader.close();
    }
    
    @Test
    public void testAsterisk() throws Exception{
    	String tag = "ZP";
        String value = "Z**";

        Comparator op = Comparator.Equal;
        TagValueFilter filter = new TagValueFilter(tag, op, value);
        SamReader Inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        int i = 0;
        for(SAMRecord re : Inreader){
        	i ++;
        	if(i % 2 == 0 ){
        		re.setAttribute(tag, "Z**");
        		assertTrue(filter.filterOut(re));
        	}else if(i % 3 == 0 ){
        		re.setAttribute(tag, "D**");
        		assertFalse(filter.filterOut(re));
        	}else 
        		assertFalse(filter.filterOut(re));
        }  
        Inreader.close();
    	
    }

    @After
    public void after(){
        new File(TestFile.INPUT_FILE_NAME).delete();
    }

    @Test
    public void integerTagMatchesStringComparison() throws Exception {
        SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        SAMRecord re = reader.iterator().next();
        reader.close();

        String[] values = {"5", "05", "+5", "-5", "abc", "4", "6"};
        String[] comps = {"==", "!=", ">", ">=", "<", "<="};
        for (int tagValue : new int[] {-5, 4, 5, 6}) {
            re.setAttribute("ZZ", tagValue);
            for (String comp : comps) {
                for (String value : values) {
                    Comparator op = Comparator.getComparator(comp, value);
                    TagValueFilter filter = new TagValueFilter("ZZ", op, value);
                    Boolean expected;
                    try {
                        expected = op.eval(Integer.toString(tagValue), value);
                    } catch (NumberFormatException e) {
                        expected = null;
                    }
                    if (expected == null) {
                        // non-integer value with a numeric comparator - must still fail as it did before
                        try {
                            filter.filterOut(re);
                            fail("expected NumberFormatException for ZZ " + comp + " " + value);
                        } catch (NumberFormatException e) {
                            // expected
                        }
                    } else {
                        assertEquals("ZZ:" + tagValue + " " + comp + " " + value, expected, filter.filterOut(re));
                    }
                }
            }
        }
    }

    @Test
    public void wildcardIgnoresCase() {
        SamReader reader;
        SAMRecord re;
        try {
            reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));
            re = reader.iterator().next();
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        re.setAttribute("ZP", "AbCdE");
        assertTrue(new TagValueFilter("ZP", Comparator.getComparator("=~", "aBc*"), "aBc").filterOut(re));
        assertFalse(new TagValueFilter("ZP", Comparator.getComparator("=~", "bcd*"), "bcd").filterOut(re));
        assertTrue(new TagValueFilter("ZP", Comparator.getComparator("=~", "*cDe"), "cDe").filterOut(re));
        assertFalse(new TagValueFilter("ZP", Comparator.getComparator("=~", "*xAbCdE"), "xAbCdE").filterOut(re));
        assertTrue(new TagValueFilter("ZP", Comparator.getComparator("=~", "BcD"), "BcD").filterOut(re));
        assertTrue(new TagValueFilter("ZP", Comparator.getComparator("=~", "abcde"), "abcde").filterOut(re));
        assertFalse(new TagValueFilter("ZP", Comparator.getComparator("=~", "bcdef"), "bcdef").filterOut(re));
        assertTrue(new TagValueFilter("ZP", Comparator.getComparator("!~", "xyz"), "xyz").filterOut(re));
        assertFalse(new TagValueFilter("ZP", Comparator.getComparator("!~", "ab*"), "ab").filterOut(re));
    }
}
