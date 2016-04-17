package org.qcmg.qbamfilter.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.filter.TestFile;

public class QueryExecutorTest {
    private static final String INPUT_FILE_NAME = "input.sam";

    /**
     * Create an testing input sam file
     */
    @BeforeClass
    public static void before()throws Exception{
        CreateSamFile();
    }
    
    @AfterClass
    public static void after1(){
          new File(TestFile.INPUT_FILE_NAME).delete();
     }
	
    @Test
    public void testValid() throws Exception{
        //there are two reads satisfied below query
        String query = "or (option_ZM == 1 , OPTION_ZM >= 100)";
        int r = checkRecord(query);
        assertTrue(r == 2);

        //there are no reads satisfied below query
        query = "and (mapq > 16 ,cigar_M >= 40 , or (option_ZM == 0 , option_ZM > 200))";
        r = checkRecord(query);
        assertTrue( r == 0);

        //there are two reads satisfy below query
        query = "opTIOn_ZM < 200";
        r = checkRecord(query);
        assertTrue( r == 1 );
        
        query = "Flag_DuplicateRead == false";
        r = checkRecord(query);
        assertEquals(3, r );
       
    }

    /**
     * picard won't treat a read with cigar "42M8S" but it is 8 base hardclip, as invalid read
     * So here we only test invalid flag and Mate information
     */
    @Test
    public void InvalidFlag() throws Exception{
            String  query = "and (mapq > 16,cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
            QueryExecutor myExecutor = new QueryExecutor(query);
            SamReader inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(INPUT_FILE_NAME));

            int i = 0;
            for(SAMRecord record : inreader){
               if(i == 2){
                    record.setFlags(7);
                    assertFalse(myExecutor.Execute(record));
                    break;
               }
               i ++;
            }
            inreader.close();
    }
    
    @Test
    public void nullOrEmptyQuery() throws Exception {
    		try {
    			new QueryExecutor(null);
    			Assert.fail("Should have thrown an exception");
    		} catch (IllegalArgumentException aie) {}
    		try {
    			new QueryExecutor("");
    			Assert.fail("Should have thrown an exception");
    		} catch (IllegalArgumentException aie) {}
    }
    
    @Test
    public void queryContainsQuotes() throws Exception {
    		// if the query passed to QueryExecutor contains quotes, then antlr with eventually give an OOM error
    	
	    	String  query = "\"and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))\"";
  		try {
  			QueryExecutor myExecutor = new QueryExecutor(query);
    			Assert.fail("Should have thrown an exception");
    		} catch (IllegalArgumentException aie) {}
  		 query = "and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))\"";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
     			Assert.fail("Should have thrown an exception");
     		} catch (IllegalArgumentException aie) {}
   		query = "\"and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		query = "and (mapq > 16 , cigar_M >= \"40\" , or (option_ZM == 0, option_ZM > 200))";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		
   		
   		query = "\'and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))\'";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		query = "and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))\'";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		query = "\'and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		query = "and (mapq > 16 , cigar_M >= \'40\' , or (option_ZM == 0, option_ZM > 200))";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
    	
    }
    @Test
    public void queryContainsDodgyCharacters() throws Exception {
    	// if the query passed to QueryExecutor contains quotes, then antlr with eventually give an OOM error
    	
	    	String  query = "\\ and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
	    	try {
	    		QueryExecutor myExecutor = new QueryExecutor(query);
	    		Assert.fail("Should have thrown an exception");
	    	} catch (IllegalArgumentException aie) {}
	    	
	    	query = "and mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
	    	try {
	    		QueryExecutor myExecutor = new QueryExecutor(query);
	    		Assert.fail("Should have thrown an exception");
	    	} catch (RuntimeException aie) {}
    	
    }

    /**
     * test invalid read: Flag is 0 but it's mate mapping positon is 100
     */
    @Test
    public void InvalidMate() throws Exception{
            String  query = "and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
            QueryExecutor myExecutor = new QueryExecutor(query);
            SamReader inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(INPUT_FILE_NAME));

            int i = 0;
            for(SAMRecord record : inreader){
                if(i == 1){
                   record.setFlags(0);
                   record.setMateAlignmentStart(100);
                   assertFalse(myExecutor.Execute(record));
                   break;
               }
               i ++;
            }
            inreader.close();
    }

    @Test
    public void mrnmRNAME() throws Exception{
            String  query = " and (rname != mrnm, mrnm != *)";
            QueryExecutor myExecutor = new QueryExecutor(query);
            SamReader inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(INPUT_FILE_NAME));

            int i = 0;
            for(SAMRecord record : inreader){
               if(i == 1){
                   //origial return False since chr1 equal '='
                  assertFalse(myExecutor.Execute(record));

                  //return True since chr1 unequal chr11
                   record.setMateReferenceName("chr11");
                   assertTrue(myExecutor.Execute(record));
                   
                   //create a valid read with unmaped Mate; return False since mrnm equal '*'
                   record.setMateReferenceName("*");
                   record.setMateAlignmentStart(0);
                   record.setFlags(123);
                   assertFalse(myExecutor.Execute(record));
               }
               i ++;
            }
            inreader.close();
    }

    private int checkRecord(String query) throws Exception{
        int r = 0;
        QueryExecutor myExecutor = new QueryExecutor(query);
        SamReader inreader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME));    //new SAMFileReader(new File(INPUT_FILE_NAME));

        for(SAMRecord record : inreader){
           if(myExecutor.Execute(record)){
                r ++;
           }
        }
        inreader.close();
        return r;
    }
    
    private static void CreateSamFile() throws Exception{
          List<String> data = new ArrayList<String>();
        //create sam header and records
          data.addAll(CreateSamHeader());
          data.add("@CO	create by TagValueFilterTest::CreateSamHeader");
          data.addAll(CreateSamBody());

        //add above contend into a new sam file
          String fileName = INPUT_FILE_NAME;

         BufferedWriter out;
        out = new BufferedWriter(new FileWriter(fileName));
        for (String line : data) {
                out.write(line + "\n");
        }
        out.close();
   }

    private static List<String> CreateSamHeader(){
        List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        return data;
    }

    private static List<String> CreateSamBody(){
    		List<String> data = new ArrayList<String>();
		data.add("970_1290_1068	163	chr1	10176	3	42M8H	=	10167	-59	" +
				"AACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	I&&HII%%IIII4CII=4?IIF0B((!!7F@+129G))I>.6	RG:Z:1959T	" +
				"CS:Z:G202023020023010023010023000.2301002302002330000000	CQ:Z:@A&*?=9%;?:A-(<?8&/1@?():(9!,,;&&,'35)69&)./?11)&=	ZM:i:1");
		data.add("681_1482_392	115	chr1	10236	20	10H40M	=	10242	56	" +
				"AACCCTAACCCTAAACCCTAAACCCTAACCCTAACCCTAA	IIIIIIIIEBIIIIFFIIIIIIIIIIIIIIIIIIIIIIII	RG:Z:1959T	" +
				"CS:Z:T00320010320010320010032001003200103200100320000320	CQ:Z::<=>:<==8;<;<==9=9?;5>8:<+<;795.89>2;;8<:.78<)1=5;	ZM:i:200");
		data.add("1997_1173_1256	177	chr1	10242	100	22H28M	chr1	10236	0	" +
				"AACCCTAAACCCTAAACCCTAACCCTAA	IIII27IICHIIIIHIIIHIIIII$$II	RG:Z:1959T	" +
				"CS:Z:G10300010320010032001003000100320000032000020001220	CQ:Z:5?8$2;>;:458=27597:/5;7:2973:3/9;18;6/:5+4,/85-,'(");
		return data;
    }
}
