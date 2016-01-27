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
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.indel.IniFileTest;
import au.edu.qimr.indel.Options;
import au.edu.qimr.indel.Q3IndelException;


public class IndelPositionTest {
	static final String inputIndel = "indel.vcf"; 
	
	static final String inputBam = "tumor.bam"; 
	QLogger logger = QLoggerFactory.getLogger(IndelPileupTest.class);
	
	@BeforeClass
	public static void createInput() {	
		IndelPileupTest.createVcf();
		IndelPileupTest.CreateSam();
		
		
		IndelMTTest.createGatkVcf(inputIndel);
		File vcf = new File(inputIndel);
		
		//dodgy fake reference  and index
		File ref = vcf; 
	 	contigPileupTest.createSam(vcf + ".fai");		
		
	}
	
	@AfterClass
	public static void clear() throws IOException {
		File dir = new java.io.File( "." ).getCanonicalFile();		
		for(File f: dir.listFiles())
		    if(f.getName().endsWith(".fai")  ||  f.getName().endsWith(".ini")  || f.getName().endsWith(".bai")  ||
		    		f.getName().endsWith(".vcf") || f.getName().endsWith(".bam") || f.getName().endsWith(".sam")     )
		        f.delete();
		
	}
	
	@Test
	//test qFlag
	public void HCOVTest() throws Exception{
				
		 List<String> data = new ArrayList<String>();
		 for(int i = 1; i <= 1000; i ++) 
			 data.add("ST-" + i + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*");
 
		 String TD = "TD.bam";
		 createBam(data, TD);
		 		
		 int i = 1001;
		 data.add("ST-" + i + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*");
		 String ND = "ND.bam";
		 createBam(data, ND);
		 
		 IniFileTest.createIniFile(IndelMTTest.ini_noquery, inputIndel, TD, ND, inputIndel, inputIndel, null);		
		 IndelMTTest.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
 					assertTrue(re.getFilter().equals(IndelUtils.FILTER_HCOVN)); 
 					assertTrue(re.getSampleFormatRecord(2).getField("ACINDEL") == null );
				}
		}	
		new File(IniFileTest.output).delete();
		 
		 //swap tumour and normal bam order
		 IniFileTest.createIniFile(IndelMTTest.ini_noquery, inputIndel, ND,TD, inputIndel, inputIndel, null);		
		 IndelMTTest.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
 					assertTrue(!re.getFilter().equals(IndelUtils.FILTER_HCOVN)); 
					assertTrue(re.getFilter().equals(IndelUtils.FILTER_HCOVT));
					assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL") == null );
				}
		}		 		 
		new File(IniFileTest.output).delete();
	}
 
	
	public static void createBam( List<String> data1, String output) {
        List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:20140717025441134	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:qtest::Test	VN:0.2pre");
        data.add("@SQ	SN:chrY	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        data.add("@CO	create by qcmg.qbamfilter.filter::TestFile");
       
 		
        String tmp = "tmp.sam";
        try( BufferedWriter out = new BufferedWriter(new FileWriter(tmp))) {	           
           for (String line : data)   out.write(line + "\n");	
           for (String line : data1)  out.write(line + "\n");	
        }catch(IOException e){
        	System.err.println( Q3IndelException.getStrackTrace(e));	 	        	 
        	assertTrue(false);
        }
		 	
		try(SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(tmp));){		
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

}
