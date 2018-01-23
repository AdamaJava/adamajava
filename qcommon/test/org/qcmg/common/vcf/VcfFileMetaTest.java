package org.qcmg.common.vcf;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Ignore;
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
	public void mcss() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace("##fileDate=20171117");
		header.addOrReplace("##1:qUUID=1e7f8938-7965-4cfc-85c9-f3197fa150d3");
		header.addOrReplace("##1:qSource=qSNP v2.0 (2269)");
		header.addOrReplace("##1:qDonorId=http://purl.org/net/grafli/donor#87c39cab-1720-4af9-9fe2-714511c6a830");
		header.addOrReplace("##1:qControlSample=null");
		header.addOrReplace("##1:qTestSample=c9a6be94-bdb7-4c0d-a89d-4addbf76e486");
		header.addOrReplace("##1:qTestBam=/reference/genomeinfo/regression/data/COLO-829/0f443106-e17d-4200-87ec-bd66fe91195f_GS.bam");
		header.addOrReplace("##1:qTestBamUUID=0f443106-e17d-4200-87ec-bd66fe91195f");
		header.addOrReplace("##1:qAnalysisId=3d4ecf27-fc71-4853-ade6-4451a3771c7a");
		header.addOrReplace("##2:qUUID=36419b4c-8bd6-4383-9ac0-bb2d8e243ae0");
		header.addOrReplace("##2:qSource=qSNP v2.0 (2269)");
		header.addOrReplace("##2:qDonorId=http://purl.org/net/grafli/donor#87c39cab-1720-4af9-9fe2-714511c6a830");
		header.addOrReplace("##2:qControlSample=null");
		header.addOrReplace("##2:qTestSample=c9a6be94-bdb7-4c0d-a89d-4addbf76e486");
		header.addOrReplace("##2:qTestBam=/reference/genomeinfo/regression/data/COLO-829/0f443106-e17d-4200-87ec-bd66fe91195f_GS.bam");
		header.addOrReplace("##2:qTestBamUUID=0f443106-e17d-4200-87ec-bd66fe91195f");
		header.addOrReplace("##2:qAnalysisId=fa34f968-5fe3-4bff-a54f-c08813091e77");
		header.addOrReplace("##2:qTestVcf=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/2/0/20b6e966-9b36-4bf5-b598-0da638e6e6dd/testGatkHCCV.vcf.gz");
		header.addOrReplace("##2:qTestVcfUUID=null");
		header.addOrReplace("##2:qTestVcfGATKVersion=null");
		header.addOrReplace("##INPUT=1,FILE=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/3/d/3d4ecf27-fc71-4853-ade6-4451a3771c7a/3d4ecf27-fc71-4853-ade6-4451a3771c7a.vcf.gz");
		header.addOrReplace("##INPUT=2,FILE=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/f/a/fa34f968-5fe3-4bff-a54f-c08813091e77/fa34f968-5fe3-4bff-a54f-c08813091e77.vcf.gz");
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t0f443106-e17d-4200-87ec-bd66fe91195f_1\t0f443106-e17d-4200-87ec-bd66fe91195f_2");
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.MULTIPLE_CALLERS_SINGLE_SAMPLE, meta.getType());
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
	public void ctorRealLifeFail() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace("##1:qDonorId=http://purl.org/net/grafli/donor#87c39cab-1720-4af9-9fe2-714511c6a830");
//		header.addOrReplace("##1:qControlSample=null");
		header.addOrReplace("##1:qTestSample=c9a6be94-bdb7-4c0d-a89d-4addbf76e486");
		header.addOrReplace("##1:qTestBam=/reference/genomeinfo/regression/data/COLO-829/0f443106-e17d-4200-87ec-bd66fe91195f_GS.bam");
		header.addOrReplace("##1:qTestBamUUID=0f443106-e17d-4200-87ec-bd66fe91195f");
		header.addOrReplace("##1:qAnalysisId=a139f235-97e3-41c0-ad89-81ddd7d93337");
		header.addOrReplace("##2:qUUID=ed38b92a-3b77-44ac-b49a-329ea78af010");
		header.addOrReplace("##2:qSource=qSNP v2.0 (2256)");
		header.addOrReplace("##2:qDonorId=http://purl.org/net/grafli/donor#87c39cab-1720-4af9-9fe2-714511c6a830");
//		header.addOrReplace("##2:qControlSample=null");
		header.addOrReplace("##2:qTestSample=c9a6be94-bdb7-4c0d-a89d-4addbf76e486");
		header.addOrReplace("##2:qTestBam=/reference/genomeinfo/regression/data/COLO-829/0f443106-e17d-4200-87ec-bd66fe91195f_GS.bam");
		header.addOrReplace("##2:qTestBamUUID=0f443106-e17d-4200-87ec-bd66fe91195f");
		header.addOrReplace("##2:qAnalysisId=f8eefab2-7abd-4c5e-8f90-b33635b20b22");
		header.addOrReplace("##2:qTestVcf=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/2/2/22e3adc6-dd8d-432d-865d-d69895c2547b/testGatkHCCV.vcf");
		header.addOrReplace("##2:qTestVcfUUID=null");
		header.addOrReplace("##2:qTestVcfGATKVersion=null");
		header.addOrReplace("##INPUT=1,FILE=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/a/1/a139f235-97e3-41c0-ad89-81ddd7d93337/a139f235-97e3-41c0-ad89-81ddd7d93337.vcf.gz");
		header.addOrReplace("##INPUT=2,FILE=/mnt/lustre/working/genomeinfo/cromwell-test/analysis/f/8/f8eefab2-7abd-4c5e-8f90-b33635b20b22/f8eefab2-7abd-4c5e-8f90-b33635b20b22.vcf.gz");
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t0f443106-e17d-4200-87ec-bd66fe91195f");
		
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.SINGLE_CALLER_SINGLE_SAMPLE, meta.getType());
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
