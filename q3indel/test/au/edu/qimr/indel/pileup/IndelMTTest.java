package au.edu.qimr.indel.pileup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.*;
import org.qcmg.qio.vcf.VcfFileReader;
import au.edu.qimr.indel.Support;
import au.edu.qimr.indel.IniFileTest;
import au.edu.qimr.indel.Options;


public class IndelMTTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	File inputVcf;  
	File test_bam; 
	File ini_noquery; 
	File ini_query; 
	File output;
	public static final String query = "and (Flag_DuplicateRead==false, CIGAR_M>150, MD_mismatch <= 3)";
		
	@Before
	public void before() throws IOException {
		test_bam = testFolder.newFile("test.bam");
		inputVcf = testFolder.newFile("input.vcf");
		ini_noquery = testFolder.newFile("test1.ini");
		ini_query =	testFolder.newFile("test2.ini");
		
		//option won't accept existing file, so can't call testFolder.newFile
		String str = testFolder.getRoot().getAbsolutePath() + "/test.output.vcf";
		output = new File(str);
				
		createDelBam( test_bam);		
 		Support.createGatkVcf(inputVcf);
	 			
		//fake ref and make test and control point to same bam
		File ini = new File(ini_noquery.getAbsolutePath());
		IniFileTest.createIniFile(ini, test_bam, test_bam, inputVcf, inputVcf, null,  output);	
				
		ini = new File(ini_query.getAbsolutePath());	
		IniFileTest.createIniFile(ini,test_bam, test_bam, inputVcf, inputVcf, query,  output);			
	}
	
	@Test
	//without apply query that is only discard duplicats and unmapped 
	public void noQueryTest() throws IOException{
				
		Support.runQ3IndelNoHom( ini_noquery.getAbsolutePath());
	 
		//check output	
		int line = 0;
		VcfRecord record = null;
		try (VcfFileReader reader = new VcfFileReader(output)) {
			for (VcfRecord re : reader) {	
				line ++;
				record = re; 	
				assertTrue(record.getFilter().contains("NNS"));
				if(record.getChromosome().equals("chrY")){									
					//input 12 reads including one duplicate so coverage is 11
                    assertEquals("2,12,11,3[1,2],4[3],2,4,4", record.getSampleFormatRecord(1).getField("ACINDEL"));
                    assertEquals("NNS", record.getFilter());
				}else{
					assertTrue(record.getFilter().contains("COVN8"));	
					assertTrue(record.getFilter().contains("COVT"));	
				}				
			}
		}
		
		assertEquals(4, line);					
	}
	

	@Test
	// check whether query work, check output vcf header and variant order
	public void withQueryTest() throws IOException{
		
		Options options = Support.runQ3IndelNoHom( ini_query.getAbsolutePath());
        Assert.assertNotNull(options);
        assertEquals(query, options.getFilterQuery());
		//check output 
		int passNo = 0;
		VcfRecord record = null;
		VcfHeader header = null; 
		try (VcfFileReader reader = new VcfFileReader(output)) {
			header = reader.getVcfHeader();
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
			}
		}
		//there is no record pass the query so no indel counts
        assertEquals(4, passNo);
        Assert.assertNotNull(record);
        if(record.getChromosome().equals("chrY")){
            assertEquals(".", record.getSampleFormatRecord(1).getField(IndelUtils.FORMAT_ACINDEL));
            assertEquals(".", record.getSampleFormatRecord(2).getField(IndelUtils.FORMAT_ACINDEL));
		}
				
		//check sample column name
        assertEquals(header.getSampleId()[0], test_bam.getName().replaceAll("(?i).bam", ""));
        assertEquals(header.getSampleId()[1], test_bam.getName().replaceAll("(?i).bam", ""));
        assertEquals(header.firstMatchedRecord(VcfHeaderUtils.STANDARD_DONOR_ID).getMetaValue(), options.getDonorId());
        assertEquals(header.firstMatchedRecord(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE).getMetaValue(), options.getControlSample());
        assertEquals(header.firstMatchedRecord(VcfHeaderUtils.STANDARD_TEST_SAMPLE).getMetaValue(), options.getTestSample());
        assertEquals(header.firstMatchedRecord(VcfHeaderUtils.STANDARD_INPUT_LINE + "_GATK_TEST").getMetaValue(), options.getTestInputVcf().getAbsolutePath());
        assertEquals(header.firstMatchedRecord(VcfHeaderUtils.STANDARD_INPUT_LINE + "_GATK_CONTROL").getMetaValue(), options.getControlInputVcf().getAbsolutePath());
        assertEquals(header.firstMatchedRecord(VcfHeaderUtils.STANDARD_CONTROL_BAM).getMetaValue(), options.getControlBam().getAbsolutePath());
        assertEquals(header.firstMatchedRecord(VcfHeaderUtils.STANDARD_TEST_BAM).getMetaValue(), options.getTestBam().getAbsolutePath());
        assertEquals(header.firstMatchedRecord(VcfHeaderUtils.STANDARD_ANALYSIS_ID).getMetaValue(), options.getAnalysisId());
	}
	
	public static void createDelBam( File output) throws IOException {
        List<String> data = new ArrayList<>();        
        data.add("ST-E00139:1112:a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*\tMD:Z:10^AT100G0G21^TT9T");
        data.add("ST-E00139:2210:b:103\t99\tchrY\t2672680\t60\t56M2D50M45S\t=\t2672878\t349\tCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTGTGTGTGTGATTTTTTTTTTTTTTTCAAAAAACCAGTTCCTGAATTTATTTATTTTTTGATGTGTTTTTTTTTTCA\t*\tMD:Z:56^TT50");
		data.add("ST-E00139:2121:c:104\t99\tchrY\t2672696\t60\t40M3D111M\t=\t2672957\t412\tATCTACCTAGCAGTCTATCTTATTGGGTGTGTGTGTGTGATTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTT\t*");
		data.add("ST-E00139:2223:d:105\t83\tchrY\t2672708\t60\t19S26M1D2M2D104M\t=\t2672595\t-246\tTTTTTTTTTCTTCTTTGCTGTCTATTTTATTGGGTTTGTGTGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACT\t*");
		data.add("ST-E00139:1112:e:106\t83\tchrY\t2672713\t60\t16S21M2D114M\t=\t2672335\t-514\tTTTTTTTTGTTTTCTTTCTTATTGGGTGTGTGTGTGTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTT\t*");
		data.add("ST-E00139:2114:f:108\t147\tchrY\t2672723\t60\t28S13M1I109M\t=\t2672317\t-527\tTTTTTTTTTTTTTGTTGTTTATTTTTTTGTGTGTGTGTGTGTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTC\t*");        
		data.add("ST-E00139:2114:g:108\t147\tchrY\t2672723\t60\t28S14M1I108M\t=\t2672317\t-527\tTTTTTTTTTTTTTGTTGTTTATTTTTTTGTGTGTGTGTGTGTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTC\t*");        
		data.add("ST-E00139:2212:h:101\t83\tchrY\t2672728\t60\t24S8M1D119M\t=\t2672357\t-499\tTGTATTTTCTCTTTTTGGGTGTTTGTGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGAT\t*");

		data.add("ST-E00139:2212:i:107\t83\tchrY\t2672730\t60\t24S6M2D121M\t=\t2672357\t-499\tTGTATTTTCTCTTTTTGGGTGTTTGTGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGAT\t*\tMD:Z:6^TT0C1C198");
		data.add("ST-E00139:2101:k:111\t83\tchrY\t2672731\t60\t5M2D121M25S\t=\t2672990\t407\tGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGATCTTAGTTATTTCTTATCT\t*\tMD:Z:5M^TT121");

		data.add("ST-E00139:2101:j:109\t163\tchrY\t2672730\t60\t151M\t=\t2672990\t407\tGTGTGATTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTTTCACTCTGATCTTAGTTATTTCTTATCT\t*");
		data.add("ST-E00139:1219:l:110\t83\tchrY\t2672736\t60\t41S110M\t=\t2672368\t-478\tTTTTTTTTCTTGTTGTCTTTTTTTTTTTGTTTTTTTTTTTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTT\t*");	
		//duplicate
		data.add("ST-E00139:1219:m:112\t1107\tchrY\t2672736\t60\t41S110M\t=\t2672368\t-478\tTTTTTTTTCTTGTTGTCTTTTTTTTTTTGTTTTTTTTTTTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTTCAGTT\t*");			
		Support.createBam(data, output);		 		
	}			
}
