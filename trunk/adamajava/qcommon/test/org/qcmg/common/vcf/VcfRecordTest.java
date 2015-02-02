package org.qcmg.common.vcf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VcfRecordTest {

	
	@Test
	public void infoFieldTest(){
		
		final String[] parms = {"chrY","2675826",".","TG","CA",".","COVN12;MIUN","SOMATIC;NNS=4;END=2675826","ACCS","TG,5,37,CA,0,2","AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1"};
	
		final VcfRecord re = new VcfRecord(parms);
		System.out.println(re.getInfo());
		
		
		assertTrue(re.getInfo().equals("SOMATIC;NNS=4;END=2675826"));
		re.appendInfo("NNS=5");
		assertTrue(re.getInfoRecord().getField("NNS").equals("5"));
		
		assertEquals(true, re.getInfo().contains("SOMATIC"));
		assertEquals(true, re.getInfo().contains("NNS=5"));
		assertEquals(true, re.getInfo().contains("END=2675826"));
		
		re.setInfo("NNS=6");
		assertTrue(re.getInfo().equals("NNS=6"));
		
	}
	
	@Test
	public void doesToStringWork(){
		
		String[] parms = {"chrY","2675826",".","TG","CA"};
		VcfRecord re = new VcfRecord(parms);
		
		// expecting end to be inserted by toString, and to be equal to start + 1 (compound snp)
		String reToString = re.toString();
		assertEquals(true, reToString.contains("END=2675827"));
		
		String[] parms2 = {"chrY","2675826",".","TGAA","CATT"};
		re = new VcfRecord(parms2);
		
		// expecting end to be inserted by toString, and to be equal to start + 1 (compound snp)
		reToString = re.toString();
		assertEquals(true, reToString.contains("END=2675829"));
		
	}
}
