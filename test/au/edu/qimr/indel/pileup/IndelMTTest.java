package au.edu.qimr.indel.pileup;

import static org.junit.Assert.assertTrue;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.indel.IniFileTest;
import au.edu.qimr.indel.Options;
import au.edu.qimr.indel.Q3IndelException;



public class IndelMTTest {
	public static final String inputVcf = "input.vcf"; 
 
	public static final String TEST_BAM_NAME = "test.bam";
	public static final String ini_noquery = "test1.ini";
	public static final String ini_query = "test2.ini";
	public static final String query = "and (Flag_DuplicateRead==false, CIGAR_M>150, MD_mismatch <= 3)";
	
//	static final String query = "and (Flag_DuplicateRead == false, CIGAR_M > 34, MD_mismatch <= 3)";
	
	@Before
	public void before() {
		 
		createBam( TEST_BAM_NAME);
 		File bam = new File(TEST_BAM_NAME);
 		
		createGatkVcf(inputVcf);
		File vcf = new File(inputVcf);
		
		//dodgy fake reference  and index
		File ref = vcf; 
	 	contigPileupTest.createSam(inputVcf + ".fai");
	 			
		//fake ref and make test and control point to same bam
		File ini = new File(ini_noquery);	
		IniFileTest.createIniFile(ini, ref, bam,bam,vcf,vcf,null);	
				
		ini = new File(ini_query);	
		IniFileTest.createIniFile(ini, ref, bam,bam,vcf,vcf,query);			

	}
	
	@After
	public void clear() throws IOException {
		File dir = new java.io.File( "." ).getCanonicalFile();		
		for(File f: dir.listFiles())
		    if(f.getName().endsWith(".fai")  ||  f.getName().endsWith(".ini")  || f.getName().endsWith(".bai")  ||
		    		f.getName().endsWith(".vcf") || f.getName().endsWith(".bam") || f.getName().endsWith(".sam")     )
		        f.delete();
		
	}
	
	@Test
	//without apply query that is only discard duplicats and unmapped 
	public void noQueryTest(){
		String[] args = {"-i", ini_noquery};
		 
		try {
			Options options = new Options(args);	
			assertTrue(options.getFilterQuery() == null);
			IndelMT mt = new IndelMT(options, options.getLogger());
			mt.process(2,false);
		//check output	
			int line = 0;
			VcfRecord record = null;
			VcfHeader header = null; 
			try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {
				header = reader.getHeader();
				for (VcfRecord re : reader) {	
					line ++;
					record = re; 						
					if(record.getChromosome().equals("chrY"))
						//input 12 reads including one duplicate so coverage is 11
						assertTrue(record.getSampleFormatRecord(1).getField("ACINDEL").equals("3,12,11,4[2,2],2,4,4"));
					}
			}
			
			assertTrue(line == 4);			
			
			
		} catch (Exception e) {
			Assert.fail("Should not threw a Exception");
		}
		
	}
	
	@Test
	// check whether query work, check output vcf header and variant order
	public void withQueryTest(){
		
		String[] args = {"-i", ini_query}; 
		try {
			Options options = new Options(args);	
			assertTrue(options.getFilterQuery().equals(query));
			IndelMT mt = new IndelMT(options, options.getLogger());
			mt.process(2,false);
						
			//check output
			int passNo = 0;
			VcfRecord record = null;
			VcfHeader header = null; 
			try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {
				header = reader.getHeader();
				for (VcfRecord re : reader) {	
					passNo ++;
					record = re; 
					//test the output variants order
					if(passNo == 1)
						assertTrue(record.getChromosome().equals("chr11") && record.getPosition() == 2672734 && record.getChrPosition().getEndPosition() == 2672736);
					else if(passNo == 2)
						assertTrue(record.getChromosome().equals("chr11") && record.getPosition() == 2672739 && record.getChrPosition().getEndPosition() == 2672741);
					else if(passNo == 3)
						assertTrue(record.getChromosome().equals("chr11") && record.getPosition() == 2672739 && record.getChrPosition().getEndPosition() == 2672742);
					else if(passNo == 2)
						assertTrue(record.getChromosome().equals("chrY") && record.getPosition() == 2672735 && record.getChrPosition().getEndPosition() == 2672737);

				}
			}
			//there is no record pass the query so no indel counts
			assertTrue(passNo == 4);
			if(record.getChromosome().equals("chrY")){
				assertTrue(record.getSampleFormatRecord(1).getField(IndelUtils.INFO_ACINDEL).equals("."));
				assertTrue(record.getSampleFormatRecord(2).getField(IndelUtils.INFO_ACINDEL).equals("."));
			}
			
			//check sample column name
			assertTrue(header.getSampleId()[0].equals(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE.replaceAll("#", "")));
			assertTrue(header.getSampleId()[1].equals(VcfHeaderUtils.STANDARD_TEST_SAMPLE.replaceAll("#", "")));
			
			//check header 
			HashMap<String, String> headerlist = new HashMap<String, String>();
			for(Record re: header.getMetaRecords()){
				String str[] = VcfHeaderUtils.splitMetaRecord(re);
				headerlist.put(str[0], str[1]);
			}
			
 			assertTrue( headerlist.get(VcfHeaderUtils.STANDARD_DONOR_ID).equals(options.getDonorId()) );
 			assertTrue( headerlist.get(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE).equals(options.getControlSample()) );
 			assertTrue( headerlist.get(VcfHeaderUtils.STANDARD_TEST_SAMPLE).equals(options.getTestSample()) ); 			
 			assertTrue( headerlist.get(VcfHeaderUtils.STANDARD_INPUT_LINE + "_GATK_TEST").equals(options.getTestInputVcf().getAbsolutePath()) );
 			assertTrue( headerlist.get(VcfHeaderUtils.STANDARD_INPUT_LINE + "_GATK_CONTROL").equals(options.getControlInputVcf().getAbsolutePath()) ); 			
 			assertTrue( headerlist.get( VcfHeaderUtils.STANDARD_CONTROL_BAM ).equals(options.getControlBam().getAbsolutePath()) );
 			assertTrue( headerlist.get(VcfHeaderUtils.STANDARD_TEST_BAM ).equals(options.getTestBam().getAbsolutePath()) );
  			assertTrue( headerlist.get(VcfHeaderUtils.STANDARD_ANALYSIS_ID).equals(options.getAnalysisId()) );
		
		} catch (Exception e) {
			System.err.println(Q3IndelException.getStrackTrace(e));
			Assert.fail("Should not threw a Exception");
		}
		
	}
	
	public static void createBam( String output) {
        List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:20140717025441134	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:qtest::Test	VN:0.2pre");
        data.add("@SQ	SN:chrY	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        data.add("@CO	create by qcmg.qbamfilter.filter::TestFile");
       
        data.add("ST-E00139:1112:a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*");
        data.add("ST-E00139:2210:b:103\t99\tchrY\t2672680\t60\t56M2D50M45S\t=\t2672878\t349\tCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTGTGTGTGTGATTTTTTTTTTTTTTTCAAAAAACCAGTTCCTGAATTTATTTATTTTTTGATGTGTTTTTTTTTTCA\t*");
		data.add("ST-E00139:2121:c:104\t99\tchrY\t2672696\t60\t40M3D111M\t=\t2672957\t412\tATCTACCTAGCAGTCTATCTTATTGGGTGTGTGTGTGTGATTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTT\t*");
		data.add("ST-E00139:2223:d:105\t83\tchrY\t2672708\t60\t19S26M1D2M2D104M\t=\t2672595\t-246\tTTTTTTTTTCTTCTTTGCTGTCTATTTTATTGGGTTTGTGTGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACT\t*");
		data.add("ST-E00139:1112:e:106\t83\tchrY\t2672713\t60\t16S21M2D114M\t=\t2672335\t-514\tTTTTTTTTGTTTTCTTTCTTATTGGGTGTGTGTGTGTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTT\t*");
		data.add("ST-E00139:2114:f:108\t147\tchrY\t2672723\t60\t28S13M1I109M\t=\t2672317\t-527\tTTTTTTTTTTTTTGTTGTTTATTTTTTTGTGTGTGTGTGTGTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTC\t*");        
		data.add("ST-E00139:2114:g:108\t147\tchrY\t2672723\t60\t28S14M1I108M\t=\t2672317\t-527\tTTTTTTTTTTTTTGTTGTTTATTTTTTTGTGTGTGTGTGTGTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTC\t*");        

		data.add("ST-E00139:2212:h:101\t83\tchrY\t2672728\t60\t24S8M1D119M\t=\t2672357\t-499\tTGTATTTTCTCTTTTTGGGTGTTTGTGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGAT\t*");
		data.add("ST-E00139:2212:i:107\t83\tchrY\t2672730\t60\t24S6M2D121M\t=\t2672357\t-499\tTGTATTTTCTCTTTTTGGGTGTTTGTGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGAT\t*");
		data.add("ST-E00139:2101:k:111\t83\tchrY\t2672731\t60\t5M2D121M25S\t=\t2672990\t407\tGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGATCTTAGTTATTTCTTATCT\t*");
		data.add("ST-E00139:2101:j:109\t163\tchrY\t2672730\t60\t151M\t=\t2672990\t407\tGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGATCTTAGTTATTTCTTATCT\t*");
		data.add("ST-E00139:1219:l:110\t83\tchrY\t2672736\t60\t41S110M\t=\t2672368\t-478\tTTTTTTTTCTTGTTGTCTTTTTTTTTTTGTTTTTTTTTTTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTT\t*");	
		//duplicate
		data.add("ST-E00139:1219:m:112\t1107\tchrY\t2672736\t60\t41S110M\t=\t2672368\t-478\tTTTTTTTTCTTGTTGTCTTTTTTTTTTTGTTTTTTTTTTTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTT\t*");	
		
        try( BufferedWriter out = new BufferedWriter(new FileWriter(output + ".sam"))) {	           
           for (String line : data)  
                   out.write(line + "\n");	           	            
        }catch(IOException e){
        	System.err.println( Q3IndelException.getStrackTrace(e));	 	        	 
        	assertTrue(false);
        }
		 	
		try(SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(output + ".sam"));){		
			SAMOrBAMWriterFactory factory = new  SAMOrBAMWriterFactory(reader.getFileHeader() ,false, new File(output)) ;
			SAMFileWriter writer = factory.getWriter();
			for( SAMRecord record : reader) 
				writer.addAlignment(record);
			 
			factory.closeWriter();
		} catch (IOException e) {
			System.err.println(Q3IndelException.getStrackTrace(e));
			Assert.fail("Should not threw a Exception");
		}		
	}
 
	public static void createGatkVcf(String vcf){						
        List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.1");
        data.add("##contig=<ID=chrY,length=59373566>");
        data.add("#CHROM	POS	ID      REF     ALT     QUAL	FILTER	INFO	FORMAT	S1"); 
        data.add("chr11	2672739	.	ATT	A	123.86	.	.	GT	0/1"); 
        data.add("chrY	2672735	.	ATT	A	123.86	.	.	GT	0/1"); 
        data.add("chr11	2672739	.	ATTC	A	123.86	.	.	GT	0/1"); 
        data.add("chr11	2672734	.	ATT	A	123.86	.	.	GT	0/1"); 
        
        //input1 with 7 lines
        try( BufferedWriter out = new BufferedWriter(new FileWriter(vcf ))) {	           
           for (String line : data)  
                   out.write(line + "\n");	           	            
        }catch(IOException e){
        	System.err.println( Q3IndelException.getStrackTrace(e));	 	        	 
        	assertTrue(false);
        }   
	}
		
}
