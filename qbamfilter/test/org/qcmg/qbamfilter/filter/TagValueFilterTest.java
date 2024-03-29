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

}
