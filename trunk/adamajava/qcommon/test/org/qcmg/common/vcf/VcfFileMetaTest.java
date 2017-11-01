package org.qcmg.common.vcf;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.vcf.header.VcfHeader;

public class VcfFileMetaTest {
	
	@Test
	public void ctor() {
		VcfHeader header = new VcfHeader();
		try {
			new VcfFileMeta(header);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		
		header.addOrReplace("##1:qControlBamUUID=bfb11d61-bbf2-4d49-8be5-01f129750cb8");
		header.addOrReplace("##1:qTestBamUUID=4473aebc-7cbc-43f8-8bbc-299265214cf3");
		header.addOrReplace("##2:qControlBamUUID=bfb11d61-bbf2-4d49-8be5-01f129750cb8");
		header.addOrReplace("##2:qTestBamUUID=4473aebc-7cbc-43f8-8bbc-299265214cf3");
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	bfb11d61-bbf2-4d49-8be5-01f129750cb8_1	4473aebc-7cbc-43f8-8bbc-299265214cf3_1	bfb11d61-bbf2-4d49-8be5-01f129750cb8_2	4473aebc-7cbc-43f8-8bbc-299265214cf3_2");
		
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, meta.getType());
		assertEquals(1, meta.getFirstControlSamplePos());
		assertEquals(2, meta.getFirstTestSamplePos());
		assertEquals(2, meta.getAllControlPositions().size());
		assertEquals(2, meta.getAllTestPositions().size());
		assertEquals(2, meta.getCallerSamplePositions().size());
		assertEquals(true, meta.getCallerSamplePositions().containsKey("1"));
		assertEquals(true, meta.getCallerSamplePositions().containsKey("2"));
		assertArrayEquals(new short[]{1,2}, meta.getCallerSamplePositions().get("1"));
		assertArrayEquals(new short[]{3,4}, meta.getCallerSamplePositions().get("2"));
	}
	
	@Test
	public void ctorSingleSampleMultipleCallers() {
		VcfHeader header = new VcfHeader();
		try {
			new VcfFileMeta(header);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		
		header.addOrReplace("##1:qTestBamUUID=4473aebc-7cbc-43f8-8bbc-299265214cf3");
		header.addOrReplace("##2:qTestBamUUID=4473aebc-7cbc-43f8-8bbc-299265214cf3");
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	4473aebc-7cbc-43f8-8bbc-299265214cf3_1	4473aebc-7cbc-43f8-8bbc-299265214cf3_2");
		
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.MULTIPLE_CALLERS_SINGLE_SAMPLE, meta.getType());
		assertEquals(-1, meta.getFirstControlSamplePos());
		assertEquals(1, meta.getFirstTestSamplePos());
		assertEquals(0, meta.getAllControlPositions().size());
		assertEquals(2, meta.getAllTestPositions().size());
		assertEquals(2, meta.getCallerSamplePositions().size());
		assertEquals(true, meta.getCallerSamplePositions().containsKey("1"));
		assertEquals(true, meta.getCallerSamplePositions().containsKey("2"));
		assertArrayEquals(new short[]{0,1}, meta.getCallerSamplePositions().get("1"));
		assertArrayEquals(new short[]{0,2}, meta.getCallerSamplePositions().get("2"));
	}
	
	@Test
	public void ctorSingleSampleSingleCaller() {
		VcfHeader header = new VcfHeader();
		try {
			new VcfFileMeta(header);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		
		header.addOrReplace("##qTestBamUUID=4473aebc-7cbc-43f8-8bbc-299265214cf3");
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	4473aebc-7cbc-43f8-8bbc-299265214cf3");
		
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.SINGLE_CALLER_SINGLE_SAMPLE, meta.getType());
		assertEquals(-1, meta.getFirstControlSamplePos());
		assertEquals(1, meta.getFirstTestSamplePos());
		assertEquals(0, meta.getAllControlPositions().size());
		assertEquals(1, meta.getAllTestPositions().size());
		assertEquals(1, meta.getCallerSamplePositions().size());
		assertEquals(true, meta.getCallerSamplePositions().containsKey("1"));
		assertEquals(false, meta.getCallerSamplePositions().containsKey("2"));
		assertArrayEquals(new short[]{0,1}, meta.getCallerSamplePositions().get("1"));
	}
	
	@Test
	public void ctorMultipleSamplesSingleCaller() {
		VcfHeader header = new VcfHeader();
		try {
			new VcfFileMeta(header);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		
		header.addOrReplace("##qControlBamUUID=bfb11d61-bbf2-4d49-8be5-01f129750cb8");
		header.addOrReplace("##qTestBamUUID=4473aebc-7cbc-43f8-8bbc-299265214cf3");
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	bfb11d61-bbf2-4d49-8be5-01f129750cb8	4473aebc-7cbc-43f8-8bbc-299265214cf3");
		
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.SINGLE_CALLER_MULTIPLE_SAMPLES, meta.getType());
		assertEquals(1, meta.getFirstControlSamplePos());
		assertEquals(2, meta.getFirstTestSamplePos());
		assertEquals(1, meta.getAllControlPositions().size());
		assertEquals(1, meta.getAllTestPositions().size());
		assertEquals(1, meta.getCallerSamplePositions().size());
		assertEquals(true, meta.getCallerSamplePositions().containsKey("1"));
		assertEquals(false, meta.getCallerSamplePositions().containsKey("2"));
		assertArrayEquals(new short[]{1,2}, meta.getCallerSamplePositions().get("1"));
	}

}
