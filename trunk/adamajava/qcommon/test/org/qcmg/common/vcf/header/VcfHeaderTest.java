package org.qcmg.common.vcf.header;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VcfHeaderTest {

	@Test
	public void sampleIdTest() throws Exception{
		final VcfHeader header = new VcfHeader();
		header.add(new VcfHeaderRecord("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	Control	Test"));
		
		final String[] sample = header.getSampleId();
		assertTrue(sample[0].equals("Control"));
		assertTrue(sample[1].equals("Test"));
	}
}
