package au.edu.qimr.qannotate.modes;

import org.junit.Test;
import org.qcmg.common.vcf.VCFRecord;

public class AbstractModeTest {
	
	
	
	//test data
	@Test
	public void inputRecordCompoundSnp(){
		
		//final String[] params =  {"chr1","10180",".","T","C","."," MIN;MIUN","SOMATIC","ACCS","TA,5,37,CA,0,2", "AA,1,1,CA,4,1,CT,3,1,TA,11,76,TT,2,2,_A,0,3,TG,0,1"};

		final String[] params =  {"chr1","10180",".","TA","CT","."," MIN;MIUN","SOMATIC;END=10181","ACCS","TA,5,37,CA,0,2", "AA,1,1,CA,4,1,CT,3,1,TA,11,76,TT,2,2,_A,0,3,TG,0,1"};
		final VCFRecord record = new VCFRecord(params);
		
		System.out.println(record.getChrPosition().toString());
		 
	}
}
