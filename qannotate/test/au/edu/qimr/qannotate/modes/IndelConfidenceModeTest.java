package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.Main;

public class IndelConfidenceModeTest {
	public static String dbMask = "repeat.mask";
	public static String input = "input.vcf";
	public static String output = "output.vcf";
	public static String log = "output.log";
	
	@After
	public void clear(){
		
        new File(dbMask).delete();
        new File(input).delete();
        new File(output).delete();
        new File(log).delete();

	}
	
	@Test
	public void InfoTest(){
		
		IndelConfidenceMode mode = new IndelConfidenceMode();	
		
		String str = "chr1	11303744	.	C	CA	37.73	PASS	SOMATIC;HOM=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087;SVTYPE=INS;END=11303745	GT:AD:DP:GQ:PL:ACINDEL	.:.:.:.:.:0,39,36,0[0,0],0,4,4	0/1:30,10:40:75:75,0,541:7,80,66,8[4,4],1,7,8";
//		String str = "chr1	11303744	.	C	CA	37.73	HOM5	SOMATIC;HOMTXT=AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087;SVTYPE=INS;END=11303745	GT:AD:DP:GQ:PL:ACINDEL	.:.:.:.:.:0,39,36,0[0,0],0,4,4	0/1:30,10:40:75:75,0,541:7,80,66,8[4,4],1,7,8";

		VcfRecord vcf = new	VcfRecord(str.split("\\t"));
		assertTrue(mode.getConfidence(vcf) == MafConfidence.HIGH);
 				
		//HOMCNTXT is no longer checked
		vcf.setInfo("SOMATIC;HOMCNTXT=9,AGCCTGTCTCaAAAAAAAAAA;SVTYPE=INS;END=11303745");
		assertTrue(mode.getConfidence(vcf) == MafConfidence.HIGH);
		vcf.setInfo("SOMATIC;HOM=9,AGCCTGTCTCaAAAAAAAAAA;SVTYPE=INS;END=11303745");		
		assertTrue(mode.getConfidence(vcf) == MafConfidence.LOW);
		
		
		//no homopolymers (repeat)
		vcf = new	VcfRecord(str.split("\\t"));
		vcf.setInfo("SOMATIC;NIOC=0.087;SVTYPE=INS;END=11303745");
		assertTrue(mode.getConfidence(vcf) == MafConfidence.HIGH);
		
		//somatic SSOI
		vcf.setInfo("SOMATIC;NIOC=0.087;SSOI=0.1;SVTYPE=INS;END=11303745");
		assertTrue(mode.getConfidence(vcf) == MafConfidence.HIGH);
		
		//germline SSOI high
		vcf.setInfo("NIOC=0.087;SSOI=0.2;SVTYPE=INS;END=11303745");
		assertTrue(mode.getConfidence(vcf) == MafConfidence.HIGH);
		
		//germline SSOI low
		vcf.setInfo("NIOC=0.087;SSOI=0.1;SVTYPE=INS;END=11303745");
		assertTrue(mode.getConfidence(vcf) == MafConfidence.LOW);
				
		//9 base repeat
		//vcf.setFilter(IndelUtils.FILTER_HOM + "9");
		vcf.setInfo("SOMATIC;HOM=9,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087;SVTYPE=INS;END=11303745");
		assertTrue(mode.getConfidence(vcf) == MafConfidence.LOW);

		//no repeat but too many nearby indel
		vcf.setInfo("SOMATIC;HOM=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.187;SVTYPE=INS;END=11303745");
		assertTrue(mode.getConfidence(vcf) == MafConfidence.LOW);	 
		
		
		//vcf.setFilter(IndelUtils.FILTER_HOM + "3" );
		vcf.setInfo("SOMATIC;HOM=3,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087;SVTYPE=INS;END=11303745");
		assertTrue(mode.getConfidence(vcf) == MafConfidence.HIGH);	 
		
		vcf.setFilter(IndelUtils.FILTER_MIN );
		assertTrue(mode.getConfidence(vcf) == MafConfidence.LOW);	
		
	}
	
	
	@Test
	 public void confidenceRealLifeIndel() throws Exception {
		 //chr2    29432822        .       C       CGCGAATT        .       HCOVT   AC=2;AF=1.00;AN=2;DP=1174;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=59.98;QD=7.19;SOR=11.114;HOM=0,GGATTTTATCgcgaattTCCAAGCTGA;CONF=ZERO   GT:GD:AD:DP:GQ:PL       .:.:.:.:.:.     1/1:CGCGAATT/CGCGAATT:0,258:258:99:8481,606,0
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr2","29432822",".","C","CGCGAATT",".","HCOVT","AC=2;AF=1.00;AN=2;DP=1174;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=59.98;QD=7.19;SOR=11.114;HOM=0,GGATTTTATCgcgaattTCCAAGCTGA","GT:GD:AD:DP:GQ:PL"," .:.:.:.:.:.","1/1:CGCGAATT/CGCGAATT:0,258:258:99:8481,606,0"});
		 
		 IndelConfidenceMode cm =new IndelConfidenceMode();
		 assertTrue(cm.getConfidence(vcf1) == MafConfidence.ZERO);
	 }
	
	@Test
	public void MaskTest() throws Exception{
		
		try{
			createMask();
			createVcf();
			
			String[] args ={"--mode","IndelConfidence", "-i", new File(input).getAbsolutePath(), "-o", new File(output).getAbsolutePath(),
					"-d", new File(dbMask).getAbsolutePath(), "--log", new File(log).getAbsolutePath()};									
			Main.main(args);
						
	        try (VCFFileReader reader = new VCFFileReader(new File(output))) {
	        	int i = 0;
	        	//mask 53744 53780 
	        	for ( VcfRecord re : reader) {		
	        		i ++;
	        		if(i == 1) 
	        			//chr1	53741	.	CTT	C
	        			assertTrue( re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE).equals( MafConfidence.HIGH.name()));	 
	        		else  if(i == 2)
	        			//chr1	53742	.	C	CA
	        			assertTrue( re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE).equals( MafConfidence.HIGH.name()));		        		
	        		else  if(i == 3)
	        			//chr1	53742	.	CTT	C
	        			assertTrue( re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE).equals( MafConfidence.ZERO.name()));	 	        		
	        		else  if(i == 4) 	        			
	        			//chr1	53743	.	C	CA
	        			//the insertion happened bwt 53743 and 53744, so it is before repeat region
	        			assertTrue( re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE).equals( MafConfidence.HIGH.name()));	      		
	        		 else  if(i == 5)
	        			//chr1	53744	.	CTT	C
	        			assertTrue( re.getInfoRecord().getField(VcfHeaderUtils.INFO_CONFIDENCE).equals( MafConfidence.ZERO.name()));	 	         	        		
	        	}
	        	assertEquals(5, i);	
	        }
			
		}catch(Exception e){
			System.out.println(e.getMessage());
			
			fail( "My method  throw expection" );
		}
	
	}
	
	public static void createMask() throws IOException{
		        final List<String> data = new ArrayList<String>();
        data.add("1 53744 53780 Low_complexity Low_complexity");
        data.add("chr1 54712 54820 Simple_repeat Simple_repeat");
        try(BufferedWriter out = new BufferedWriter(new FileWriter(dbMask));) {          
            for (final String line : data)   out.write(line +"\n");                  
         }  
	}
	
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_UUID_LINE + "=abcd_12345678_xzy_999666333");
        data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
        
        data.add("chr1	53741	.	CTT	C	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");
        data.add("chr1	53742	.	C	CA	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");      
        data.add("chr1	53742	.	CTT	C	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");
        data.add("chr1	53743	.	C	CA	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");
        data.add("chr1	53744	.	CTT	C	37.73	PASS	SOMATIC;HOMCNTXT=5,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087");
       
        try(BufferedWriter out = new BufferedWriter(new FileWriter(input));) {          
            for (final String line : data)   out.write(line +"\n");                  
         }  

	}
}
