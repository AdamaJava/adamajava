package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;

import au.edu.qimr.qannotate.modes.ConfidenceMode.Confidence;

public class IndelConfidenceModeTest {
	
	
	@Test
	public void myTest(){
		String str = "chr1	11303744	.	C	CA	37.73	PASS	SOMATIC;HOMCNTXT=10,AGCCTGTCTCaAAAAAAAAAA;NIOC=0.087;SVTYPE=INS;END=11303745	GT:AD:DP:GQ:PL:ACINDEL	.:.:.:.:.:0,39,36,0[0,0],0,4,4	0/1:30,10:40:75:75,0,541:7,80,66,8[4,4],1,7,8";
		
		VcfRecord vcf = new	VcfRecord(str.split("\\t"));
		IndelConfidenceMode mode = new IndelConfidenceMode();
		
		Confidence conf = mode.getConfidence(vcf);
		assertTrue(conf == Confidence.LOW);
		 
	}
	

}
