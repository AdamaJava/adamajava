package au.edu.qimr.indel.pileup;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.indel.Support;
import au.edu.qimr.indel.IniFileTest;


public class IndelPositionTest {
	static final String inputIndel = "indel.vcf"; 
	static final String emptyVcf = "empty.vcf";
	static final  String normalBAM = "ND.bam";
	static final  String tumourBAM = "TD.bam";
	static final String inputBam = "tumor.bam"; 
	QLogger logger = QLoggerFactory.getLogger(IndelPileupTest.class);
	
	@BeforeClass
	public static void createInput() {	
//		IndelPileupTest.createVcf();
		Support.createGatkVcf(inputIndel);
		File vcf = new File(inputIndel);
		
		//dodgy fake reference  and index
	 	ContigPileupTest.createSam(vcf + ".fai");	
		 
		List<String> data = new ArrayList<String>();
		data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE);
		Support.createVcf(data,data, emptyVcf); //empty vcf; 
	}
	
	@AfterClass
	public static void clear() throws IOException {
		 Support.clear();		
	}
	
	@Test
	//test HCOVN and HCOVT can't have other flag or SOMATIC
	public void HCOVTest() throws Exception{
	
		 List<String> data = new ArrayList<String>();
		 for(int i = 1; i <= 1000; i ++) 
			 data.add("ST-" + i + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*");		  
		 Support.createBam(data, tumourBAM);
		 		
		 int i = 1001;
		 data.add("ST-" + i + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*");
		 Support.createBam(data, normalBAM);
		 
		 IniFileTest.createIniFile(IndelMTTest.ini_noquery,  tumourBAM, normalBAM, inputIndel, inputIndel, null);		
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
 					assertTrue(re.getFilter().equals(IndelUtils.FILTER_HCOVN)); 
 					assertTrue(re.getSampleFormatRecord(2).getField("ACINDEL") == null );
				}
		}	
		new File(IniFileTest.output).delete();
		 
		 //swap tumour and normal bam order
		 IniFileTest.createIniFile(IndelMTTest.ini_noquery, normalBAM,tumourBAM, inputIndel, inputIndel, null);		
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 
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
 
	@Test	 
	//ACINDEL==1,7,7,7[7,0],7[1],0,0,7 novel start of support read in normal is 7
	public void germlineTest1() throws IOException{				
		//normal BAM with one novel start	
		List<String> data = new ArrayList<String>();
		 for(int i = 1; i <= 7; i ++) 
			 data.add("ST-" + i + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*\tMD:Z:10^TT123^TT10");	 
		 Support.createBam(data,normalBAM);

		//tumour BAM with assertTrue(record.getSampleFormatRecord(1).getField("ACINDEL").equals("3,12,11,4[2,2],2,4,4"));
		IndelMTTest.createDelBam(tumourBAM);
		
		 IniFileTest.createIniFile(IndelMTTest.ini_noquery,  tumourBAM, normalBAM,inputIndel, emptyVcf, null);		
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);

		 //not somatic since supporting/informative=100% on control BAM 
		 //NBIAS 100 support reads >=3 and one of strand is 0; 
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){	
					assertFalse(re.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC));
					assertTrue(re.getFilter().contains(IndelUtils.FILTER_COVN8));
 					assertTrue(re.getFilter().contains(IndelUtils.FILTER_NBIAS)); //germline, strong support 7>=3 and all in one strand
 					assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals("1,7,7,7[7,0],7[1],0,0,7"));
 					assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_NIOC).equals("0"));  // 0/7 == 0
 					assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_SSOI).equals("1.000"));  // 7/7 == 0
				}
		}	
		new File(IniFileTest.output).delete();		
				
		//swap normal and tumour make it to be somatic
		 IniFileTest.createIniFile(IndelMTTest.ini_noquery,   normalBAM, tumourBAM,inputIndel, emptyVcf, null);		
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		//not somatic since support noverlStart==3, none of strand reads <%%
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
					//System.out.println(re.toString());
					assertTrue(re.getFilter().contains(IndelUtils.FILTER_COVT )); //germline, tumour with coverge 7
					assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals("2,12,11,3[1,2],4[3],2,4,4"));
 					assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_NIOC).equals("0.333"));  // 4/12 == 0.333
 					assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_SSOI).equals("0.273"));  // 3/11 == 0.273 why not 0.272
				}
		}	
		new File(IniFileTest.output).delete();		
	}
	
	@Test	
	//tumour("ACINDEL").equals("1,8,8,8[8,0],0,0,8") ; germline indel but passed all filter
	public void germlineTest2() throws IOException{		
		//normal BAM with one novel start	
		List<String> data = new ArrayList<String>();
		 for(int i = 1; i <= 8; i ++) 
			 data.add("ST-" + i + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*\tMD:Z:10^TT123^TT10");	 
		 Support.createBam(data,tumourBAM);
		 IndelMTTest.createDelBam(normalBAM);
		 
		 IniFileTest.createIniFile(IndelMTTest.ini_noquery,  tumourBAM, normalBAM, inputIndel, inputIndel, null);		
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){
					assertFalse( re.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC)); 
					assertTrue(re.getFilter().equals(IndelUtils.FILTER_NNS)); 
					assertTrue(re.getSampleFormatRecord(2).getField("ACINDEL").equals("1,8,8,8[8,0],8[1],0,0,8"));
					assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals("2,12,11,3[1,2],4[3],2,4,4"));
				}
		}
		new File(IniFileTest.output).delete();			 
	}	
	
	//SOMATIC, TPART, NPART
	@Test 
	public void partialTest() throws IOException{
		
		// somatic: normal BAM with zero novel start three partial reads
		//normal BAM don't have indel evident
		 List<String> data = new ArrayList<String>();
		 for(int i = 1; i <= 3; i ++) 
			data.add("ST-" + i + ":c:104\t99\tchrY\t2672696\t60\t40M3D111M\t=\t2672957\t412\tATCTACCTAGCAGTCTATCTTATTGGGTGTGTGTGTGTGATTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTT\t*");
		 Support.createBam(data,normalBAM);
		 IniFileTest.createIniFile(IndelMTTest.ini_noquery,  normalBAM, normalBAM,inputIndel, inputIndel, null);		
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){ 
					assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_COVERAGE_TUMOUR )); //germline and coverage only 3
					assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_8 )); //
					assertTrue(re.getFilter().contains(IndelUtils.FILTER_NPART)); //partial reads 3>=3 and partial/total=100% >5% on normal	
					assertTrue(re.getFilter().contains(IndelUtils.FILTER_TPART));//partial reads 3>= and partial/total=100% >10% on tumour						
				}
		}
		 new File(IniFileTest.output).delete();
		 
		 
		// somatic 4 partial 46 supporting  partial/total=8%
		 //normal BAM have strong indel evident
		data.add("ST-" + 4 + ":c:104\t99\tchrY\t2672696\t60\t40M3D111M\t=\t2672957\t412\tATCTACCTAGCAGTCTATCTTATTGGGTGTGTGTGTGTGATTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTCCCCTT\t*");
		 for(int i = 5; i <= 50; i ++) 
			 data.add("ST-" + i + ":a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*");	 
		 Support.createBam(data,normalBAM);
	
		 //with same ini file
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){ 
					
					assertTrue(!re.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC)); //NOT somatic: support 46 reads of 50 informative reads
					assertTrue(re.getFilter().contains(VcfHeaderUtils.FILTER_NOVEL_STARTS)); //Not Somatic, so don't care nns 
					assertTrue(!re.getFilter().contains(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_12)); //total coverage 50
					assertTrue(!re.getFilter().contains(IndelUtils.FILTER_TPART));	//partial reads 4>=3 but partial/total=8% < 10%	on tumour								
					assertTrue(!re.getFilter().contains(IndelUtils.FILTER_TBIAS)); //not somtic, don't care TBIAS
					assertTrue(re.getFilter().contains(IndelUtils.FILTER_NBIAS)); //not somtic, support read 46>3 and all in one strand
					assertTrue(re.getFilter().contains(IndelUtils.FILTER_NPART)); //partial reads 4>= and partial/total=8% >5%	on normal	
				}
		}
		 new File(IniFileTest.output).delete();
	}
	 
	@Test //SOMTIC, COVN12, MIN, NNS
	public void somaticTest() throws IOException{
		//normal BAM with one novel start, gematic.soi = 3% < 0.05
		 List<String> data = new ArrayList<String>();
        data.add("ST-E00139:1112:a:102\t99\tchrY\t2672601\t60\t10M2D123M2D10M8S\t=\t2673085\t631\tGTAGTTTATATTTCTGTGGGGTCAGTGGTGATATCCCTTTTATTATTTTTTATTGTGTCTTTTTGATTCTTCTCTCTTTTCTTTTTTATTAATCTACCTAGCAGTCTATCTTATTGGGTGTG\t*\tMD:Z:10^TT12^TT10M");
		
		 for(int i = 1; i <= 10; i ++) 
			data.add("ST-" + i + ":f:108\t147\tchrY\t2672723\t60\t28S13M1I109M\t=\t2672317\t-527\tTTTTTTTTTTTTTGTTGTTTATTTTTTTGTGTGTGTGTGTGTTTTTTTTTTTTTTTTCCAAAAAACCAGTTCCTGAATTCATTGATTTTTTGAAGGGTTTTTTGTGTCACTGTC\t*");        
		 Support.createBam(data,normalBAM);

		//tumour BAM with assertTrue(record.getSampleFormatRecord(1).getField("ACINDEL").equals("3,12,11,4[2,2],2,4,4"));
		 IndelMTTest.createDelBam(tumourBAM);
		 
        data.clear();;
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1");
        data.add("chr11	2672739	.	ATT	A	123.86	.	.	GT	0/1"); 
        Support.createVcf(data, data, "control.vcf");
	
	     IniFileTest.createIniFile(IndelMTTest.ini_noquery,tumourBAM, normalBAM, inputIndel,"control.vcf", null);		
		 Support.runQ3IndelNoHom(IndelMTTest.ini_noquery);
		 		 
		 //not somatic since supporting/informative=100% on control BAM 
		 //NBIAS 100 support reads >=3 and one of strand is 0; 
		 try (VCFFileReader reader = new VCFFileReader(IniFileTest.output)) {				 
			 for (VcfRecord re : reader)  											
				if(re.getChromosome().equals("chrY")){	
					assertTrue(re.getInfo().contains(VcfHeaderUtils.INFO_SOMATIC));
					assertTrue(re.getInfo().contains("GATKINFO")); //make sure GATK INFO column still exists
					assertTrue(re.getFilter().contains(IndelUtils.FILTER_COVN12)); 
					assertTrue(re.getFilter().contains(IndelUtils.FILTER_MIN)); 
					assertTrue(re.getFilter().contains(IndelUtils.FILTER_NNS)); 					
					assertTrue(re.getSampleFormatRecord(1).getField("ACINDEL").equals("1,11,11,1[1,0],1[1],0,10,1"));
					assertTrue(re.getSampleFormatRecord(2).getField("ACINDEL").equals("2,12,11,3[1,2],4[3],2,4,4"));
 					assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_NIOC).equals("0.333"));  // 4/12 == 0.333
 					assertTrue(re.getInfoRecord().getField(IndelUtils.INFO_SSOI).equals("0.273"));  // 3/11 == 0.273 why not 0.272
					
				}
		}	
		new File(IniFileTest.output).delete();	
	}

}
