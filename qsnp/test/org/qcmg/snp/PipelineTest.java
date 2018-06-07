package org.qcmg.snp;

import static org.junit.Assert.assertEquals;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class PipelineTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	
//	@Test
//	public void getCountFromString() {
//		assertEquals(1, Pipeline.getCountFromString("A1", 'A'));
//		assertEquals(1, Pipeline.getCountFromString("A1;B2", 'A'));
//		assertEquals(2, Pipeline.getCountFromString("A1;B2", 'B'));
//		assertEquals(-1, Pipeline.getCountFromString("A1;B2", 'C'));
//		assertEquals(10, Pipeline.getCountFromString("C10;A1;B2", 'C'));
//		assertEquals(-1, Pipeline.getCountFromString("C10;A1;B2", 'X'));
//	}
	
	@Test
	public void accumulateReadBases() {
		final Pipeline pipeline = new TestPipeline();
		
		Map<Long, StringBuilder>readSeqMap = new HashMap<>();
		Accumulator acc = new Accumulator(100);
		acc.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		
		pipeline.accumulateReadBases(acc, readSeqMap, 100);
		
		assertEquals(1, readSeqMap.size());
		
		Accumulator acc2 = new Accumulator(101);
		acc2.addBase((byte)'C', (byte)30, true, 101, 101, 200, 1);
		
		pipeline.accumulateReadBases(acc2, readSeqMap, 101);
//		assertEquals(2, readSeqMap.size());
	}
	
	@Test
	public void getHeader() {
		VcfHeader h = Pipeline.getHeaderForQSnp("abc", "123", "456", "qsnp", null, null, "xyz");
		assertEquals(true, null != h.getFormatRecord(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND));
		assertEquals(true, h.getFormatRecord(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND).toString().contains(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND_DESC));
	}
	
	
	
	
}
