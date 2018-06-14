package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

public class GermlineModeTest {
	
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
  	
	 
	 @Test
	 public void getDataForInfoField() {
		 assertEquals(null, GermlineMode.getDataForInfoField(null, null));
		 assertEquals(null, GermlineMode.getDataForInfoField("", null));
		 assertEquals(null, GermlineMode.getDataForInfoField("A", null));
		 assertEquals(null, GermlineMode.getDataForInfoField(null, new String[]{}));
		 assertEquals("A:1:2:1:2", GermlineMode.getDataForInfoField("A", new String[]{"1","2","1","2"}));
		 assertEquals("T:11:2:1:12", GermlineMode.getDataForInfoField("T", new String[]{"11","2","1","12"}));
		 assertEquals("ABC:11:2:1:12", GermlineMode.getDataForInfoField("ABC", new String[]{"11","2","1","12"}));
	 }
	 
	 @Test
	 public void annotate() {
		 
		VcfRecord r = new VcfRecord(new String[]{"chr1","16534","rs201459529","C","T",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
		assertEquals(true, GermlineMode.annotateGermlineSnp(r, "C", "T", new String[]{"1","1","1","1"}));
		assertEquals(true, r.getInfo().contains("GERM=T:1:1:1:1"));
		assertEquals(false, GermlineMode.annotateGermlineSnp(r, "C", "A", new String[]{"1","1","1","1"}));
		assertEquals(false, r.getInfo().contains("GERM=A:1:1:1:1"));
		
		r = new VcfRecord(new String[]{"chr1","16534","rs201459529","C","T,A",".",".","IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
		assertEquals(true, GermlineMode.annotateGermlineSnp(r, "C", "T", new String[]{"1","1","1","1"}));
		assertEquals(true, r.getInfo().contains("GERM=T:1:1:1:1"));
		assertEquals(true, GermlineMode.annotateGermlineSnp(r, "C", "A", new String[]{"1","1","1","1"}));
		assertEquals(true, r.getInfo().contains("GERM=T:1:1:1:1,A:1:1:1:1"));
	 }
	 
	@Test
	public void germlineModeTest() throws Exception {
		File f = testFolder.newFile();
		File db = testFolder.newFile();
		File out = testFolder.newFile();
		createVcf(f);
		createGermlineFile(db);
		final GermlineMode mode = new GermlineMode();		
		mode.loadVcfRecordsFromFile(f);
		mode.addAnnotation(db.getAbsolutePath());						
		mode.reheader("testing run",   f.getAbsolutePath());
		mode.writeVCF(out);
		
		try(VCFFileReader reader = new VCFFileReader(out)){
			 
			 //check header
			VcfHeader header = reader.getHeader();	
			assertEquals(false, header.getFilterRecord(VcfHeaderUtils.FILTER_GERMLINE) != null);

			//check records
			int inputs = 0;
			int germNo = 0;
			
			for (final VcfRecord re : reader) {	
 				inputs ++;
 				
 				if(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE) != null)
 					germNo ++;
 				
 				if (re.getPosition() == 95813205) {
 					assertEquals("T:10:283:293:0", re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE));
 				}
 				if (re.getPosition() == 	60781981) {
 					assertEquals("G:10:302:312:0", re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE));
 				}
			}
			assertTrue(inputs == 2);
			assertTrue(germNo == 2);
		 }
	}	 
	 
	@Test
	public void checkNonSomatic() throws Exception {
		File db = testFolder.newFile();
		File out = testFolder.newFile();
		File f = testFolder.newFile();
		createGermlineFile(db);
		
		
	    final List<String> data = new ArrayList<String>(4);
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1\ts2");
       
        //germ position but not somatic
        data.add("chrY\t14923588\t.\tG\tA\t.\t.\tFS=GTGATATTCCC\tGT:GD:AC:MR:NNS\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"); 
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(f));) {          
            for (final String line : data)   bw.write(line + "\n");                  
         }
        
		final GermlineMode mode = new GermlineMode();		
		mode.loadVcfRecordsFromFile(f);
		mode.addAnnotation(db.getAbsolutePath());
		mode.writeVCF(out);
        
		 try(VCFFileReader reader = new VCFFileReader(out)){ 
			//check records
			int i = 0;
			for (final VcfRecord re : reader) {	
 				i ++;
 				if (re.getPosition() == 14923588) {
					assertTrue(re.getFilter().equals(Constants.MISSING_DATA_STRING));
					//assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE).equals("86,185"));
					assertTrue(re.getInfoRecord().getField(VcfHeaderUtils.INFO_GERMLINE) == null);
 				}
			}
			assertTrue( i == 1 );
		 }	        
	}
	@Test
	public void checkSomatic()throws IOException{
		File db = testFolder.newFile();
		createGermlineFile(db);
		
		VcfRecord r = new VcfRecord(new String[]{"chr1","16534","rs201459529","C","T",".",".","AF=1.00;DP=2;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=34.79;MQ0=0;QD=20.87;SOR=2.303;IN=2;DB","GT:AD:DP:GQ:PL:FT:MR:NNS:OABS:INF",".:.:.:.:.:.:.:.:.:.","1/1:0,2:2:6:69,6,0:SBIASCOV;SAT3:2:2:T0[0]2[34.5]:SOMATIC"});
		
		final GermlineMode mode = new GermlineMode();		
		mode.positionRecordMap.put(r.getChrPosition(), Arrays.asList(r));
		mode.addAnnotation(db.getAbsolutePath());
		
		List<VcfRecord> recs = mode.positionRecordMap.get(r.getChrPosition());
		assertEquals(1, recs.size());
		assertEquals(true, VcfUtils.isRecordSomatic(recs.get(0)));
	}
	
	/**
	 * create input vcf file containing 2 dbSNP SNPs and one verified SNP
	 * @throws IOException
	 */
	static void createVcf(File f) throws IOException{
        List<String> data =Arrays.asList(
        		"##fileformat=VCFv4.2",
        		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT,
        		"chr1	95813205	rs11165387	C	T	.	.	FLANK=CTTCGTGTCTT;BaseQRankSum=1.846;ClippingRankSum=-1.363;DP=35;FS=3.217;MQ=60.00;MQRankSum=-1.052;QD=18.76;ReadPosRankSum=-1.432;SOR=0.287;IN=1,2;DB;VAF=0.6175;HOM=0,TCCTTCTTCGtGTCTTTCCTT;EFF=downstream_gene_variant(MODIFIER||3602|||RP11-14O19.1|lincRNA|NON_CODING|ENST00000438093||1),intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/1:17,15:Germline:22:32:T0[]1[]:C1;T8:PASS:.:.:15:C4[36.5]13[38.54];T5[41]10[39.7]:.	0/0:26,0:ReferenceNoVariant:22:26:C1[]1[]:C7:PASS:.:.:.:C8[36.5]18[39.56]:.	0/1:16,18:Germline:21:34:.:.:PASS:99:.:.:.:656.77	./.:.:HomozygousLoss:21:.:.:.:PASS:.:NCIG:.:.:.",
        		"chr1	60781981	rs5015226	T	G	.	.	FLANK=ACCAAGTGCCT;DP=53;FS=0.000;MQ=60.00;QD=31.16;SOR=0.730;IN=1,2;DB;HOM=0,CGAAAACCAAgTGCCTGCATT;EFF=intergenic_region(MODIFIER||||||||||1)	GT:AD:CCC:CCM:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	1/1:0,46:Germline:34:47:G0[]1[]:G5;T1:PASS:.:.:42:C0[0]1[12];G23[40.04]23[36.83]:.	1/1:0,57:Germline:34:57:G2[]0[]:G3:PASS:.:.:51:G24[39]33[39.18]:.	1/1:0,53:Germline:34:53:.:.:PASS:99:.:.:.:2418.77	1/1:0,60:Germline:34:60:.:.:PASS:99:.:.:.:2749.77"
        		);
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(f));) {          
            for (String line : data)   out.write(line + "\n");                  
         }  
	}	
	
	/**
	 * a mini dbSNP vcf file 
	 */
	static void createGermlineFile(File f) throws IOException{
        List<String> data = Arrays.asList("chr1_11894494_C_T:0:1:1:0:rs372880659",
"chr1_143540358_A_T:183:1:1:183:rs76379294",
"chr1_95813205_C_T:10:283:293:0:rs11165387",
"chr1_36332201_A_G:2:0:2:0:rs79659675",
"chr1_79040252_A_G:0:1:1:0:novel",
"chr1_147325053_G_A:1:0:1:0:novel",
"chr1_44499568_T_G:1:0:1:0:novel",
"chr1_60781981_T_G:10:302:312:0:rs5015226",
"chr1_173643835_T_C:0:2:2:0:rs1903411");

        try(BufferedWriter out = new BufferedWriter(new FileWriter(f));) {
           for (String line : data)  out.write(line + "\n");
        }  
	}	
}
