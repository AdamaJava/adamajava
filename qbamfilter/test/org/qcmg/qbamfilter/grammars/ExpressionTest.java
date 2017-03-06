package org.qcmg.qbamfilter.grammars;

import static org.junit.Assert.*;

import java.io.File;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.filter.TestFile;

public class ExpressionTest {
	private final String para = "option_ZM";
    private final String tag = "ZM";
    private final String value = "1";
    private final String op = "==";
    
	@BeforeClass
    public static void before(){
        //create testing data regardless whether it exists or not, Since old testing data maybe damaged.
        TestFile.CreateBAM(TestFile.INPUT_FILE_NAME);
    }
	
	@AfterClass
    public static void after1(){
          new File(TestFile.INPUT_FILE_NAME).delete();
     }
	
	
	/**
     * Test the process,at the end it cause an exception
     * @throws Exception
     */
    @Test
    public void testSingleCondition() throws Exception{

        //test true/false case with only one condition
    	SamRecordFilter exp1 = new Condition("option_ZM", "==", "1").getFilter();
    	SamRecordFilter exp2 = new Condition("option_ZM", "!=", "1").getFilter();
        //only check the first record
    	SamReader inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        for(SAMRecord record : inreader){
        	assertTrue( exp1.filterOut(record));
        	assertFalse( exp2.filterOut(record)); 
        	break;
	    }
        inreader.close();
         
    }
    
    @Test
    public void testmultiCondition() throws Exception{
        //test true case with two condition
        Expression exp1 = new Expression();
        exp1.addCondition(new Condition("option_ZM", "==", "1").getFilter());
        exp1.addCondition(new Condition("option_ZM", "!=", "1").getFilter()); 
        exp1.addOperator(queryTree.Operator.OR);
        
        Expression exp2 = new Expression();
        exp2.addCondition(new Condition("option_ZM", "==", "1").getFilter());
        exp2.addCondition(new Condition("option_ZM", "!=", "1").getFilter()); 
        exp2.addOperator(queryTree.Operator.AND);
      //only check the first record
    	SamReader inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        for(SAMRecord record : inreader){
        	assertTrue( exp1.filterOut(record));
        	assertFalse( exp2.filterOut(record)); 
        	break;
	    }
        inreader.close();
        
    }
    @Test
    public void testsubTree() throws Exception{
        Expression exp1 = new Expression();
        exp1.addCondition(new Condition("option_ZM", "==", "1").getFilter());
        exp1.addCondition(new Condition("option_ZM", "!=", "1").getFilter()); 
        exp1.addOperator(queryTree.Operator.OR);
        
        //add exp1 as sub tree into exp2
        Expression exp2 = new Expression();
        exp2.addOperator(queryTree.Operator.AND);
        exp2.addCondition(new Condition("option_ZM", "!=", "1").getFilter());
        exp2.addCondition(exp1);
        
      //add exp2 as sub tree into exp3
        Expression exp3 = new Expression();
        exp3.addOperator(queryTree.Operator.OR);
        exp3.addCondition(exp1);
        exp3.addCondition(exp2);
                      
       //only check the first record
    	SamReader inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(TestFile.INPUT_FILE_NAME));
        for(SAMRecord record : inreader){
        	assertTrue( exp1.filterOut(record) );
        	assertFalse( exp2.filterOut(record)); 
        	assertTrue( exp3.filterOut(record) ); 
        	break;
	    }
        inreader.close();       
    }
    
    
}
