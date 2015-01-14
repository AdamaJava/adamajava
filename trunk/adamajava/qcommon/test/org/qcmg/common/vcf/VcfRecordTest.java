package org.qcmg.common.vcf;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VcfRecordTest {
	
	@Test
	public void qSignatureInfoField() {
		VcfRecord vcf =VcfUtils.createVcfRecord("chr1", 47851, "C");
		
		assertEquals(".", vcf.getInfo());
		vcf.setInfo("FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0");
		assertEquals("FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0", vcf.getInfo());
		assertEquals(true, vcf.toString().contains("FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0"));
	}

}
