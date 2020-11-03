package org.qcmg.common.vcf;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.vcf.ContentType;
import org.qcmg.common.vcf.VcfFileMeta;
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
	public void callerSamplePositions() {
		VcfHeader header = new VcfHeader();
		
		header.addOrReplace("##1:qControlBamUUID=control-bam-uuid");
		header.addOrReplace("##1:qTestBamUUID=test-bam-uuid");
		header.addOrReplace("##2:qControlBamUUID=control-bam-uuid");
		header.addOrReplace("##2:qTestBamUUID=test-bam-uuid");
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	control-bam-uuid_1	test-bam-uuid_1	control-bam-uuid_2	test-bam-uuid_2");
		
		VcfFileMeta meta = new VcfFileMeta(header);
//		Map<String, short[]> callerSamplePositions = meta.getCallerSamplePositions();
//		for (Entry<String, short[]> entry : callerSamplePositions.entrySet()) {
//			System.out.println("key: " + entry.getKey() + ", value: " + Arrays.toString(entry.getValue()));
//		}
		
		
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, meta.getType());
		assertEquals(2, meta.getCallerSamplePositions().size());
		assertEquals(true, meta.getCallerSamplePositions().containsKey("1"));
		assertEquals(true, meta.getCallerSamplePositions().containsKey("2"));
		assertArrayEquals(new short[]{1,2}, meta.getCallerSamplePositions().get("1"));
		assertArrayEquals(new short[]{3,4}, meta.getCallerSamplePositions().get("2"));
		
		/*
		 * and now with underscores replacing dashes
		 */
		header = new VcfHeader();
		header.addOrReplace("##1:qControlBamUUID=control_bam_uuid");
		header.addOrReplace("##1:qTestBamUUID=test_bam_uuid");
		header.addOrReplace("##2:qControlBamUUID=control_bam_uuid");
		header.addOrReplace("##2:qTestBamUUID=test_bam_uuid");
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	control_bam_uuid_1	test_bam_uuid_1	control_bam_uuid_2	test_bam_uuid_2");
		meta = new VcfFileMeta(header);
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, meta.getType());
		assertEquals(2, meta.getCallerSamplePositions().size());
		assertEquals(true, meta.getCallerSamplePositions().containsKey("1"));
		assertEquals(true, meta.getCallerSamplePositions().containsKey("2"));
		assertArrayEquals(new short[]{1,2}, meta.getCallerSamplePositions().get("1"));
		assertArrayEquals(new short[]{3,4}, meta.getCallerSamplePositions().get("2"));
		
		header = new VcfHeader();
		header.addOrReplace("##1:qControlBamUUID=bfb11d61_bbf2_4d49_8be5_01f129750cb8");
		header.addOrReplace("##1:qTestBamUUID=4473aebc_7cbc_43f8_8bbc_299265214cf3");
		header.addOrReplace("##2:qControlBamUUID=bfb11d61_bbf2_4d49_8be5_01f129750cb8");
		header.addOrReplace("##2:qTestBamUUID=4473aebc_7cbc_43f8_8bbc_299265214cf3");
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	bfb11d61_bbf2_4d49_8be5_01f129750cb8_1	4473aebc_7cbc_43f8_8bbc_299265214cf3_1	bfb11d61_bbf2_4d49_8be5_01f129750cb8_2	4473aebc_7cbc_43f8_8bbc_299265214cf3_2");
		meta = new VcfFileMeta(header);
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, meta.getType());
		
//		callerSamplePositions = meta.getCallerSamplePositions();
//		for (Entry<String, short[]> entry : callerSamplePositions.entrySet()) {
//			System.out.println("key: " + entry.getKey() + ", value: " + Arrays.toString(entry.getValue()));
//		}
		
		assertEquals(2, meta.getCallerSamplePositions().size());
		assertEquals(true, meta.getCallerSamplePositions().containsKey("1"));
		assertEquals(true, meta.getCallerSamplePositions().containsKey("2"));
		assertArrayEquals(new short[]{1,2}, meta.getCallerSamplePositions().get("1"));
		assertArrayEquals(new short[]{3,4}, meta.getCallerSamplePositions().get("2"));
	}
	
	@Test
	public void callerSamplePositionsSampleNames() {
		VcfHeader header = new VcfHeader();
		
		/*
		 * ##1:qControlSample=edba8df8-2e83-46c2-9216-d737f42a3ab3
		 * ##1:qTestSample=23d282c5-5996-4903-8576-515139830265
		 */
		
		
		header.addOrReplace("##1:qControlSample=edba8df8-2e83-46c2-9216-d737f42a3ab3");
		header.addOrReplace("##1:qTestSample=23d282c5-5996-4903-8576-515139830265");
		header.addOrReplace("##2:qControlSample=edba8df8-2e83-46c2-9216-d737f42a3ab3");
		header.addOrReplace("##2:qTestSample=23d282c5-5996-4903-8576-515139830265");
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	edba8df8-2e83-46c2-9216-d737f42a3ab3_1	23d282c5-5996-4903-8576-515139830265_1	edba8df8-2e83-46c2-9216-d737f42a3ab3_2	23d282c5-5996-4903-8576-515139830265_2");
		
		VcfFileMeta meta = new VcfFileMeta(header);
//		Map<String, short[]> callerSamplePositions = meta.getCallerSamplePositions();
//		for (Entry<String, short[]> entry : callerSamplePositions.entrySet()) {
//			System.out.println("key: " + entry.getKey() + ", value: " + Arrays.toString(entry.getValue()));
//		}
		
		
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, meta.getType());
		assertEquals(2, meta.getCallerSamplePositions().size());
		assertEquals(true, meta.getCallerSamplePositions().containsKey("1"));
		assertEquals(true, meta.getCallerSamplePositions().containsKey("2"));
		assertArrayEquals(new short[]{1,2}, meta.getCallerSamplePositions().get("1"));
		assertArrayEquals(new short[]{3,4}, meta.getCallerSamplePositions().get("2"));
		
		/*
		 * and now with underscores replacing dashes
		 */
		header = new VcfHeader();
		header.addOrReplace("##1:qControlSample=edba8df8_2e83_46c2_9216_d737f42a3ab3");
		header.addOrReplace("##1:qTestSample=23d282c5_5996_4903_8576_515139830265");
		header.addOrReplace("##2:qControlSample=edba8df8_2e83_46c2_9216_d737f42a3ab3");
		header.addOrReplace("##2:qTestSample=23d282c5_5996_4903_8576_515139830265");
		header.addOrReplace("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	edba8df8_2e83_46c2_9216_d737f42a3ab3_1	23d282c5_5996_4903_8576_515139830265_1	edba8df8_2e83_46c2_9216_d737f42a3ab3_2	23d282c5_5996_4903_8576_515139830265_2");
		meta = new VcfFileMeta(header);
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, meta.getType());
		assertEquals(2, meta.getCallerSamplePositions().size());
		assertEquals(true, meta.getCallerSamplePositions().containsKey("1"));
		assertEquals(true, meta.getCallerSamplePositions().containsKey("2"));
		assertArrayEquals(new short[]{1,2}, meta.getCallerSamplePositions().get("1"));
		assertArrayEquals(new short[]{3,4}, meta.getCallerSamplePositions().get("2"));
	}
	
	@Test
	public void callerSamplePositionsCombined() {
		VcfHeader header = new VcfHeader();
		
		header.addOrReplace("##qControlSample=qasim_chr1_70p_T");
		header.addOrReplace("##qTestSample=qasim_chr1_70p_B");
		header.addOrReplace("##qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");
		header.addOrReplace("##qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");
		header.addOrReplace("##1:qControlSample=edba8df8-2e83-46c2-9216-d737f42a3ab3");
		header.addOrReplace("##1:qTestSample=23d282c5-5996-4903-8576-515139830265");
		header.addOrReplace("##1:qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");
		header.addOrReplace("##1:qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");
		header.addOrReplace("##2:qControlSample=edba8df8-2e83-46c2-9216-d737f42a3ab3");
		header.addOrReplace("##2:qTestSample=23d282c5-5996-4903-8576-515139830265");
		header.addOrReplace("##2:qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");
		header.addOrReplace("##2:qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tqasim_chr1_70p_T_1\tqasim_chr1_70p_B_1\tqasim_chr1_70p_T_2\tqasim_chr1_70p_B_2");
		
		VcfFileMeta meta = new VcfFileMeta(header);
		Map<String, short[]> callerSamplePositions = meta.getCallerSamplePositions();
		for (Entry<String, short[]> entry : callerSamplePositions.entrySet()) {
			System.out.println("key: " + entry.getKey() + ", value: " + Arrays.toString(entry.getValue()));
		}
		
		assertEquals(ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES, meta.getType());
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
	public void ctorRealLifeFail2() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace("##fileformat=VCFv4.2");
		header.addOrReplace("##fileDate=20181008");
		header.addOrReplace("##reference=file:///mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa");
		header.addOrReplace("##qDonorId=02.006.0452");
		header.addOrReplace("##qControlSample=020060452BP");
		header.addOrReplace("##qTestSample=020060452FFPE");
		header.addOrReplace("##qINPUT_GATK_TEST=/mnt/lustre/working/genomeinfo/analysis/7/2/72dde0d0-b795-44d2-956f-3b948389c677/72dde0d0-b795-44d2-956f-3b948389c677.vcf");
		header.addOrReplace("##qINPUT_GATK_CONTROL=/mnt/lustre/working/genomeinfo/analysis/6/7/67db1323-1761-46e2-9f2a-6ec66c0cb229/67db1323-1761-46e2-9f2a-6ec66c0cb229.vcf");
		header.addOrReplace("##qControlBam=/mnt/lustre/working/genomeinfo/sample/a/f/af146d8c-0a17-4a24-8953-38fb82b06987/aligned_read_group_set/a264340d-07bf-433b-b871-5a1051b040ae.bam");
		header.addOrReplace("##qControlBamUUID=a264340d-07bf-433b-b871-5a1051b040ae");
		header.addOrReplace("##qTestBam=/mnt/lustre/working/genomeinfo/sample/8/0/807cf4b0-4f64-4c75-8367-8edf5887b470/aligned_read_group_set/e3125365-7708-4e97-bee8-ba93337daedd.bam");
		header.addOrReplace("##qTestBamUUID=e3125365-7708-4e97-bee8-ba93337daedd");
		header.addOrReplace("##qAnalysisId=cf1c66f2-f2cd-4e90-8df0-34719d230c6e");
		header.addOrReplace("##qUUID=405ee63d-a2ae-4ad5-a302-e5d523f3fb77");
		header.addOrReplace("##qSource=qannotate-2.0.1 (2566)");
		header.addOrReplace("##qINPUT=405ee63d-a2ae-4ad5-a302-e5d523f3fb77:/mnt/lustre/working/genomeinfo/analysis/c/f/cf1c66f2-f2cd-4e90-8df0-34719d230c6e/cf1c66f2-f2cd-4e90-8df0-34719d230c6e.vcf.conf");
		header.addOrReplace("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t020060452BP\t020060452FFPE");
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.SINGLE_CALLER_MULTIPLE_SAMPLES, meta.getType());
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
	
	@Test
	public void mergedVcfHeader() {
		
		
		List<String> data = new ArrayList<>();
		data.add("##fileformat=VCFv4.2");
		data.add("##fileDate=20190726");;
		data.add("##reference=file:///mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa");;
		data.add("##qDonorId=qasim_70p");;
		data.add("##qControlSample=qasim_chr1_70p_T");;
		data.add("##qTestSample=qasim_chr1_70p_B");;
		data.add("##qINPUT_GATK_TEST=/mnt/lustre/working/genomeinfo/analysis/b/f/bf77f9bf-f597-4d00-b0f2-7b669016cf84/bf77f9bf-f597-4d00-b0f2-7b669016cf84.vcf");;
		data.add("##qINPUT_GATK_CONTROL=/mnt/lustre/working/genomeinfo/analysis/7/5/75fd3e43-ef28-4968-8ed5-08a979e3f873/75fd3e43-ef28-4968-8ed5-08a979e3f873.vcf");;
		data.add("##qControlBam=/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam");;
		data.add("##qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");;
		data.add("##qTestBam=/mnt/lustre/working/genomeinfo/sample/2/3/23d282c5-5996-4903-8576-515139830265/aligned_read_group_set/dae3dc96-245b-48ad-8a55-00ebb9c5d2f2.bam");;
		data.add("##qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");;
		data.add("##qAnalysisId=1bca1f69-26db-40be-90da-77149f308a21");
		data.add("##SnpEffVersion=\"4.0e (build 2014-09-13), by Pablo Cingolani\"");
		data.add("##SnpEffCmd=\"SnpEff  -o VCF -stats /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.snpEff_summary.html GRCh37.75 /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf \"");
		data.add("##qUUID=609e8bd3-a7c0-4934-8c21-e2fdee9e5352");
		data.add("##qSource=qannotate-2.0.1 (2566)");
		data.add("##qINPUT=609e8bd3-a7c0-4934-8c21-e2fdee9e5352:/mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf");
		data.add("##FILTER=<ID=TRF,Description=\"at least one of the repeat is with repeat sequence length less than six; and the repeat frequence is more than 10 (or more than six for homoplymers repeat), , or less than 20% of informative reads are strong supporting in case of indel variant\">");
		data.add("##FILTER=<ID=TPART,Description=\"The number in the tumour partials column is >=3 and is >10% of the total reads at that position\">");
		data.add("##FILTER=<ID=TBIAS,Description=\"For somatic calls: the supporting tumour reads value is >=3 and the count on one strand is =0 or >0 and is either <10% of supporting reads or >90% of supporting reads\">");
		data.add("##FILTER=<ID=REPEAT,Description=\"this variants is fallen into the repeat region\">");
		data.add("##FILTER=<ID=NPART,Description=\"The number in the normal partials column is >=3 and is >5% of the total reads at that position\">");
		data.add("##FILTER=<ID=NNS,Description=\"For somatic calls: less than 4 novel starts not considering read pair in tumour BAM\">");
		data.add("##FILTER=<ID=NBIAS,Description=\"For germline calls: the supporting normal reads value is >=3 and the count on one strand is =0 or >0 and is either <5% of supporting reads or >95% of supporting reads\">");
		data.add("##FILTER=<ID=MIN,Description=\"For somatic calls: mutation also found in pileup of normal BAM\">");
		data.add("##FILTER=<ID=LowQual,Description=\"Low quality\">");
		data.add("##FILTER=<ID=HCOVT,Description=\"more than 1000 reads in tumour BAM\">");
		data.add("##FILTER=<ID=HCOVN,Description=\"more than 1000 reads in normal BAM\">");
		data.add("##FILTER=<ID=COVT,Description=\"For germline calls: less than 8 reads coverage in tumour\">");
		data.add("##FILTER=<ID=COVN8,Description=\"For germline calls: less than 8 reads coverage in normal\">");
		data.add("##FILTER=<ID=COVN12,Description=\"For somatic calls: less than 12 reads coverage in normal BAM\">");
		data.add("##INFO=<ID=VLD,Number=0,Type=Flag,Description=\"Is Validated.  This bit is set if the variant has 2+ minor allele count based on frequency or genotype data.\">");
		data.add("##INFO=<ID=VAF,Number=.,Type=String,Description=\"Variant allele frequencies based on 1000Genomes from dbSNP as the CAF. CAF starting with the reference allele followed by alternate alleles as ordered in the ALT column.   Here we only take the related allel frequency.\">");
		data.add("##INFO=<ID=TRF,Number=1,Type=String,Description=\"List all repeat reported by TRFFinder,  crossing over the variant position.all repeat follow <repeat sequence Length>_<repeat frequency>, separated by ';'\">");
		data.add("##INFO=<ID=SSOI,Number=1,Type=String,Description=\"counts of strong support indels compare with total informative reads coverage\">");
		data.add("##INFO=<ID=SOR,Number=1,Type=Float,Description=\"Symmetric Odds Ratio of 2x2 contingency table to detect strand bias\">");
		data.add("##INFO=<ID=SOMATIC,Number=1,Type=String,Description=\"There are more than 2 novel starts  or more than 0.05 soi (number of supporting informative reads /number of informative reads) on control BAM\">");
		data.add("##INFO=<ID=ReadPosRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt vs. Ref read position bias\">");
		data.add("##INFO=<ID=QD,Number=1,Type=Float,Description=\"Variant Confidence/Quality by Depth\">");
		data.add("##INFO=<ID=NMD,Number=.,Type=String,Description=\"Predicted nonsense mediated decay effects for this variant. Format: 'Gene_Name | Gene_ID | Number_of_transcripts_in_gene | Percent_of_transcripts_affected'\">");
		data.add("##INFO=<ID=NIOC,Number=1,Type=String,Description=\"counts of nearby indels compare with total coverage\">");
		data.add("##INFO=<ID=MQRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref read mapping qualities\">");
		data.add("##INFO=<ID=MQ0,Number=1,Type=Integer,Description=\"Total Mapping Quality Zero Reads\">");
		data.add("##INFO=<ID=MQ,Number=1,Type=Float,Description=\"RMS Mapping Quality\">");
		data.add("##INFO=<ID=MLEAF,Number=A,Type=Float,Description=\"Maximum likelihood expectation (MLE) for the allele frequency (not necessarily the same as the AF), for each ALT allele, in the same order as listed\">");
		data.add("##INFO=<ID=MLEAC,Number=A,Type=Integer,Description=\"Maximum likelihood expectation (MLE) for the allele counts (not necessarily the same as the AC), for each ALT allele, in the same order as listed\">");
		data.add("##INFO=<ID=LOF,Number=.,Type=String,Description=\"Predicted loss of function effects for this variant. Format: 'Gene_Name | Gene_ID | Number_of_transcripts_in_gene | Percent_of_transcripts_affected'\">");
		data.add("##INFO=<ID=InbreedingCoeff,Number=1,Type=Float,Description=\"Inbreeding coefficient as estimated from the genotype likelihoods per-sample when compared against the Hardy-Weinberg expectation\">");
		data.add("##INFO=<ID=IN,Number=1,Type=String,Description=\"Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file\">");
		data.add("##INFO=<ID=HaplotypeScore,Number=1,Type=Float,Description=\"Consistency of the site with at most two segregating haplotypes\">");
		data.add("##INFO=<ID=HOM,Number=.,Type=String,Description=\"nearby reference sequence fallen in a specified widow size,  leading by the number of homopolymers base.\">");
		data.add("##INFO=<ID=FS,Number=1,Type=Float,Description=\"Phred-scaled p-value using Fisher's exact test to detect strand bias\">");
		data.add("##INFO=<ID=EFF,Number=.,Type=String,Description=\"Predicted effects for this variant.Format: 'Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_B	ioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )'\">");
		data.add("##INFO=<ID=DS,Number=0,Type=Flag,Description=\"Were any of the samples downsampled?\">");
		data.add("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">");
		data.add("##INFO=<ID=DB,Number=0,Type=Flag,Description=\"dbSNP Membership\",Source=/mnt/lustre/reference/dbsnp/141/00-All.vcf,Version=141>");
		data.add("##INFO=<ID=ClippingRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref number of hard clipped bases\">");
		data.add("##INFO=<ID=CONF,Number=1,Type=String,Description=\"set to HIGH if the variants passed all filter, nearby homopolymer sequence base less than six and less than 10% reads contains nearby indel; set to Zero if coverage more than 1000, or fallen in repeat region; set to LOW for reminding variants\">");
		data.add("##INFO=<ID=BaseQRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt Vs. Ref base qualities\">");
		data.add("##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">");
		data.add("##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency, for each ALT allele, in the same order as listed\">");
		data.add("##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes, for each ALT allele, in the same order as listed\">");
		data.add("##FORMAT=<ID=PL,Number=G,Type=Integer,Description=\"Normalized, Phred-scaled likelihoods for genotypes as defined in the VCF specification\">");
		data.add("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");
		data.add("##FORMAT=<ID=GQ,Number=1,Type=Integer,Description=\"Genotype Quality\">");
		data.add("##FORMAT=<ID=GD,Number=1,Type=String,Description=\"Genotype details: specific alleles\">");
		data.add("##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth (reads with MQ=255 or with bad mates are filtered)\">");
		data.add("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		data.add("##FORMAT=<ID=ACINDEL,Number=.,Type=String,Description=\"counts of indels, follow formart:novelStarts,totalCoverage,informativeReadCount,strongSuportReadCount[forwardsuportReadCount,backwardsuportReadCount],suportReadCount[novelStarts],partialReadCount,nearbyIndelCount,nearybySoftclipCount\">");
		data.add("##GATKCommandLine=<ID=HaplotypeCaller,Version=3.3-0-g37228af,Date=\"Thu Jul 25 14:13:49 AEST 2019\",Epoch=1564028029841,CommandLineOptions=\"analysis_type=HaplotypeCaller input_file=[/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam] showFullBamList=false read_buffer_size=null phone_home=AWS gatk_key=null tag=NA read_filter=[] intervals=[chr1] excludeIntervals=null interval_set_rule=UNION interval_merging=ALL interval_padding=0 reference_sequence=/mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa nonDeterministicRandomSeed=false disableDithering=false maxRuntime=-1 maxRuntimeUnits=MINUTES downsampling_type=BY_SAMPLE downsample_to_fraction=null downsample_to_coverage=250 baq=OFF baqGapOpenPenalty=40.0 refactor_NDN_cigar_string=false fix_misencoded_quality_scores=false allow_potentially_misencoded_quality_scores=false useOriginalQualities=false defaultBaseQualities=-1 performanceLog=null BQSR=null quantize_quals=0 disable_indel_quals=false emit_original_quals=false preserve_qscores_less_than=6 globalQScorePrior=-1.0 validation_strictness=SILENT remove_program_records=false keep_program_records=false sample_rename_mapping_file=null unsafe=null disable_auto_index_creation_and_locking_when_reading_rods=false no_cmdline_in_header=false sites_only=false never_trim_vcf_format_field=false bcf=false bam_compression=null simplifyBAM=false disable_bam_indexing=false generate_md5=false num_threads=1 num_cpu_threads_per_data_thread=1 num_io_threads=0 monitorThreadEfficiency=false num_bam_file_handles=null read_group_black_list=null pedigree=[] pedigreeString=[] pedigreeValidationType=STRICT allow_intervals_with_unindexed_bam=false generateShadowBCF=false variant_index_type=DYNAMIC_SEEK variant_index_parameter=-1 logging_level=INFO log_to_file=/mnt/lustre/working/genomeinfo/analysis/7/5/75fd3e43-ef28-4968-8ed5-08a979e3f873/tmp_75fd3e43-ef28-4968-8ed5-08a979e3f873_0.vcf.log help=false version=false out=org.broadinstitute.gatk.engine.io.stubs.VariantContextWriterStub likelihoodCalculationEngine=PairHMM heterogeneousKmerSizeResolution=COMBO_MIN graphOutput=null bamOutput=null bamWriterType=CALLED_HAPLOTYPES disableOptimizations=false dbsnp=(RodBinding name=dbsnp source=/mnt/lustre/reference/dbsnp/135/00-All_chr.vcf) dontTrimActiveRegions=false maxDiscARExtension=25 maxGGAARExtension=300 paddingAroundIndels=150 paddingAroundSNPs=20 comp=[] annotation=[ClippingRankSumTest, DepthPerSampleHC] excludeAnnotation=[SpanningDeletions, TandemRepeatAnnotator] debug=false useFilteredReadsForAnnotations=false emitRefConfidence=NONE annotateNDA=false heterozygosity=0.001 indel_heterozygosity=1.25E-4 standard_min_confidence_threshold_for_calling=30.0 standard_min_confidence_threshold_for_emitting=30.0 max_alternate_alleles=6 input_prior=[] sample_ploidy=2 genotyping_mode=DISCOVERY alleles=(RodBinding name= source=UNBOUND) contamination_fraction_to_filter=0.0 contamination_fraction_per_sample_file=null p_nonref_model=null exactcallslog=null output_mode=EMIT_VARIANTS_ONLY allSitePLs=false sample_name=null kmerSize=[10, 25] dontIncreaseKmerSizesForCycles=false allowNonUniqueKmersInRef=false numPruningSamples=1 recoverDanglingHeads=false doNotRecoverDanglingBranches=false minDanglingBranchLength=4 consensus=false GVCFGQBands=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 70, 80, 90, 99] indelSizeToEliminateInRefModel=10 min_base_quality_score=10 minPruning=2 gcpHMM=10 includeUmappedReads=false useAllelesTrigger=false phredScaledGlobalReadMismappingRate=45 maxNumHaplotypesInPopulation=128 mergeVariantsViaLD=false doNotRunPhysicalPhasing=true pair_hmm_implementation=VECTOR_LOGLESS_CACHING keepRG=null justDetermineActiveRegions=false dontGenotype=false errorCorrectKmers=false debugGraphTransformations=false dontUseSoftClippedBases=false captureAssemblyFailureBAM=false allowCyclesInKmerGraphToGeneratePaths=false noFpga=false errorCorrectReads=false kmerLengthForReadErrorCorrection=25 minObservationsForKmerToBeSolid=20 pcr_indel_model=CONSERVATIVE maxReadsInRegionPerSample=1000 minReadsPerAlignmentStart=5 activityProfileOut=null activeRegionOut=null activeRegionIn=null activeRegionExtension=null forceActive=false activeRegionMaxSize=null bandPassSigma=null maxProbPropagationDistance=50 activeProbabilityThreshold=0.002 min_mapping_quality_score=20 filter_reads_with_N_cigar=false filter_mismatching_base_and_quals=false filter_bases_not_stored=false\">");
		data.add("##contig=<ID=chrY,length=59373566>");
		data.add("##contig=<ID=chrX,length=155270560>");
		data.add("##contig=<ID=chrMT,length=16569>");
		data.add("##contig=<ID=chr9,length=141213431>");
		data.add("##contig=<ID=chr8,length=146364022>");
		data.add("##contig=<ID=chr7,length=159138663>");
		data.add("##contig=<ID=chr6,length=171115067>");
		data.add("##contig=<ID=chr5,length=180915260>");
		data.add("##contig=<ID=chr4,length=191154276>");
		data.add("##contig=<ID=chr3,length=198022430>");
		data.add("##contig=<ID=chr22,length=51304566>");
		data.add("##contig=<ID=chr21,length=48129895>");
		data.add("##contig=<ID=chr20,length=63025520>");
		data.add("##contig=<ID=chr2,length=243199373>");
		data.add("##contig=<ID=chr19,length=59128983>");
		data.add("##contig=<ID=chr18,length=78077248>");
		data.add("##contig=<ID=chr17,length=81195210>");
		data.add("##contig=<ID=chr16,length=90354753>");
		data.add("##contig=<ID=chr15,length=102531392>");
		data.add("##contig=<ID=chr14,length=107349540>");
		data.add("##contig=<ID=chr13,length=115169878>");
		data.add("##contig=<ID=chr12,length=133851895>");
		data.add("##contig=<ID=chr11,length=135006516>");
		data.add("##contig=<ID=chr10,length=135534747>");
		data.add("##contig=<ID=chr1,length=249250621>");
		data.add("##contig=<ID=GL000249.1,length=38502>");
		data.add("##contig=<ID=GL000248.1,length=39786>");
		data.add("##contig=<ID=GL000247.1,length=36422>");
		data.add("##contig=<ID=GL000246.1,length=38154>");
		data.add("##contig=<ID=GL000245.1,length=36651>");
		data.add("##contig=<ID=GL000244.1,length=39929>");
		data.add("##contig=<ID=GL000243.1,length=43341>");
		data.add("##contig=<ID=GL000242.1,length=43523>");
		data.add("##contig=<ID=GL000241.1,length=42152>");
		data.add("##contig=<ID=GL000240.1,length=41933>");
		data.add("##contig=<ID=GL000239.1,length=33824>");
		data.add("##contig=<ID=GL000238.1,length=39939>");
		data.add("##contig=<ID=GL000237.1,length=45867>");
		data.add("##contig=<ID=GL000236.1,length=41934>");
		data.add("##contig=<ID=GL000235.1,length=34474>");
		data.add("##contig=<ID=GL000234.1,length=40531>");
		data.add("##contig=<ID=GL000233.1,length=45941>");
		data.add("##contig=<ID=GL000232.1,length=40652>");
		data.add("##contig=<ID=GL000231.1,length=27386>");
		data.add("##contig=<ID=GL000230.1,length=43691>");
		data.add("##contig=<ID=GL000229.1,length=19913>");
		data.add("##contig=<ID=GL000228.1,length=129120>");
		data.add("##contig=<ID=GL000227.1,length=128374>");
		data.add("##contig=<ID=GL000226.1,length=15008>");
		data.add("##contig=<ID=GL000225.1,length=211173>");
		data.add("##contig=<ID=GL000224.1,length=179693>");
		data.add("##contig=<ID=GL000223.1,length=180455>");
		data.add("##contig=<ID=GL000222.1,length=186861>");
		data.add("##contig=<ID=GL000221.1,length=155397>");
		data.add("##contig=<ID=GL000220.1,length=161802>");
		data.add("##contig=<ID=GL000219.1,length=179198>");
		data.add("##contig=<ID=GL000218.1,length=161147>");
		data.add("##contig=<ID=GL000217.1,length=172149>");
		data.add("##contig=<ID=GL000216.1,length=172294>");
		data.add("##contig=<ID=GL000215.1,length=172545>");
		data.add("##contig=<ID=GL000214.1,length=137718>");
		data.add("##contig=<ID=GL000213.1,length=164239>");
		data.add("##contig=<ID=GL000212.1,length=186858>");
		data.add("##contig=<ID=GL000211.1,length=166566>");
		data.add("##contig=<ID=GL000210.1,length=27682>");
		data.add("##contig=<ID=GL000209.1,length=159169>");
		data.add("##contig=<ID=GL000208.1,length=92689>");
		data.add("##contig=<ID=GL000207.1,length=4262>");
		data.add("##contig=<ID=GL000206.1,length=41001>");
		data.add("##contig=<ID=GL000205.1,length=174588>");
		data.add("##contig=<ID=GL000204.1,length=81310>");
		data.add("##contig=<ID=GL000203.1,length=37498>");
		data.add("##contig=<ID=GL000202.1,length=40103>");
		data.add("##contig=<ID=GL000201.1,length=36148>");
		data.add("##contig=<ID=GL000200.1,length=187035>");
		data.add("##contig=<ID=GL000199.1,length=169874>");
		data.add("##contig=<ID=GL000198.1,length=90085>");
		data.add("##contig=<ID=GL000197.1,length=37175>");
		data.add("##contig=<ID=GL000196.1,length=38914>");
		data.add("##contig=<ID=GL000195.1,length=182896>");
		data.add("##contig=<ID=GL000194.1,length=191469>");
		data.add("##contig=<ID=GL000193.1,length=189789>");
		data.add("##contig=<ID=GL000192.1,length=547496>");
		data.add("##contig=<ID=GL000191.1,length=106433>");
		data.add("##qPG=<ID=6,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:25:33,CL=\"qannotate --mode snpeff -d /mnt/lustre/reference/software/snpEff/GRCh37.75 -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.snpeff.log\">");
		data.add("##qPG=<ID=5,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:23:51,CL=\"qannotate --mode indelConfidence -d /mnt/lustre/reference/genomeinfo/qannotate/indel.repeat.mask -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.hom -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf --buffer 5 --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.conf.log\">");
		data.add("##qPG=<ID=4,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:23:35,CL=\"qannotate --mode hom -d /mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.trf -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.hom --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.hom.log\">");
		data.add("##qPG=<ID=3,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:23:33,CL=\"qannotate --mode trf -d /mnt/lustre/reference/genomeinfo/qannotate/GRCh37_ICGC_standard_v2.fa.2.7.7.80.10.20.2000_simple.txt -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.trf --buffer 5 --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.trf.log\">");
		data.add("##qPG=<ID=2,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:20:59,CL=\"qannotate --mode dbsnp -d /mnt/lustre/reference/dbsnp/141/00-All.vcf -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.indel -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp.log\">");
		data.add("##qPG=<ID=2,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 11:20:59,CL=\"qannotate --mode dbsnp -d /mnt/lustre/reference/dbsnp/141/00-All.vcf -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.indel -o /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp --log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.vcf.dbsnp.log\">");
		data.add("##qPG=<ID=1,Tool=q3indel,Version=1.0 (9971),Date=2019-07-26 11:19:21,CL=\"q3indel -i /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.ini -log /mnt/lustre/working/genomeinfo/analysis/1/b/1bca1f69-26db-40be-90da-77149f308a21/1bca1f69-26db-40be-90da-77149f308a21.log [runMode: gatk]\">");
		data.add("##1:qUUID=8a12dd2e-a856-4247-9a96-cec09f3fd784");
		data.add("##1:qSource=qSNP v2.0 (2566)");
		data.add("##1:qDonorId=http://purl.org/net/grafli/donor#01b9ca75-2aec-4ac6-929f-8f127a51556e");
		data.add("##1:qControlSample=edba8df8-2e83-46c2-9216-d737f42a3ab3");
		data.add("##1:qTestSample=23d282c5-5996-4903-8576-515139830265");
		data.add("##1:qControlBam=/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam");
		data.add("##1:qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");
		data.add("##1:qTestBam=/mnt/lustre/working/genomeinfo/sample/2/3/23d282c5-5996-4903-8576-515139830265/aligned_read_group_set/dae3dc96-245b-48ad-8a55-00ebb9c5d2f2.bam");
		data.add("##1:qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");
		data.add("##1:qAnalysisId=4019018e-4a54-4fc7-834b-d858e9e8981a");
		data.add("##2:qUUID=7d6847f0-edba-45d5-81fb-c03b3cd0fa28");
		data.add("##2:qSource=qSNP v2.0 (2566)");
		data.add("##2:qDonorId=http://purl.org/net/grafli/donor#01b9ca75-2aec-4ac6-929f-8f127a51556e");
		data.add("##2:qControlSample=edba8df8-2e83-46c2-9216-d737f42a3ab3");
		data.add("##2:qTestSample=23d282c5-5996-4903-8576-515139830265");
		data.add("##2:qControlBam=/mnt/lustre/working/genomeinfo/sample/e/d/edba8df8-2e83-46c2-9216-d737f42a3ab3/aligned_read_group_set/f2ec7646-6b51-4b53-ae00-a123324c438b.bam");
		data.add("##2:qControlBamUUID=f2ec7646-6b51-4b53-ae00-a123324c438b");
		data.add("##2:qTestBam=/mnt/lustre/working/genomeinfo/sample/2/3/23d282c5-5996-4903-8576-515139830265/aligned_read_group_set/dae3dc96-245b-48ad-8a55-00ebb9c5d2f2.bam");
		data.add("##2:qTestBamUUID=dae3dc96-245b-48ad-8a55-00ebb9c5d2f2");
		data.add("##2:qAnalysisId=e90fd4e3-a6de-40be-8458-60dca990c0d7");
		data.add("##2:qControlVcf=/mnt/lustre/working/genomeinfo/analysis/7/5/75fd3e43-ef28-4968-8ed5-08a979e3f873/75fd3e43-ef28-4968-8ed5-08a979e3f873.vcf");
		data.add("##2:qControlVcfUUID=null");
		data.add("##2:qControlVcfGATKVersion=3.3-0-g37228af");
		data.add("##2:qTestVcf=/mnt/lustre/working/genomeinfo/analysis/b/f/bf77f9bf-f597-4d00-b0f2-7b669016cf84/bf77f9bf-f597-4d00-b0f2-7b669016cf84.vcf");
		data.add("##2:qTestVcfUUID=null");
		data.add("##2:qTestVcfGATKVersion=3.3-0-g37228af");
		data.add("##INPUT=1,FILE=/mnt/lustre/working/genomeinfo/analysis/4/0/4019018e-4a54-4fc7-834b-d858e9e8981a/4019018e-4a54-4fc7-834b-d858e9e8981a.vcf");
		data.add("##INPUT=2,FILE=/mnt/lustre/working/genomeinfo/analysis/e/9/e90fd4e3-a6de-40be-8458-60dca990c0d7/e90fd4e3-a6de-40be-8458-60dca990c0d7.vcf");
		data.add("##SnpEffCmd=\"SnpEff  -o VCF -stats /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.snpEff_summary.html GRCh37.75 /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf\"");
		data.add("##qUUID=e8eadd70-0f21-4592-8186-76851c99f4b0");
		data.add("##qINPUT=e8eadd70-0f21-4592-8186-76851c99f4b0:/mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf");
		data.add("##FILTER=<ID=SBIASCOV,Description=\"Sequence coverage on only one strand (or percentage coverage on other strand is less than 5%)\">");
		data.add("##FILTER=<ID=SBIASALT,Description=\"Alternate allele on only one strand (or percentage alternate allele on other strand is less than 5%)\">");
		data.add("##FILTER=<ID=SAT3,Description=\"Less than 3 reads of same allele in tumour\">");
		data.add("##FILTER=<ID=SAN3,Description=\"Less than 3 reads of same allele in normal\">");
		data.add("##FILTER=<ID=NNS,Description=\"Less than 4 novel starts not considering read pair\">");
		data.add("##FILTER=<ID=NCIT,Description=\"No call in test\">");
		data.add("##FILTER=<ID=MR,Description=\"Less than 5 mutant reads\">");
		data.add("##FILTER=<ID=MIUN,Description=\"Mutation also found in pileup of (unfiltered) normal\">");
		data.add("##FILTER=<ID=MIN,Description=\"Mutation also found in pileup of normal\">");
		data.add("##FILTER=<ID=MER,Description=\"Mutation equals reference\">");
		data.add("##FILTER=<ID=GERM,Description=\"Mutation is a germline variant in another patient\">");
		data.add("##FILTER=<ID=COVT,Description=\"Less than 8 reads coverage in tumour\">");
		data.add("##FILTER=<ID=COVN8,Description=\"Less than 8 reads coverage in normal\">");
		data.add("##FILTER=<ID=COVN12,Description=\"Less than 12 reads coverage in normal\">");
		data.add("##INFO=<ID=SOMATIC_n,Number=0,Type=Flag,Description=\"Indicates that the nth input file considered this record to be somatic. Multiple values are allowed which indicate that more than 1 input file consider this record to be somatic\">");
		data.add("##INFO=<ID=SOMATIC,Number=0,Type=Flag,Description=\"Indicates that the record is a somatic mutation\">");
		data.add("##INFO=<ID=IN,Number=.,Type=Integer,Description=\"Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file\">");
		data.add("##INFO=<ID=GERM,Number=2,Type=Integer,Description=\"Counts of donor occurs this mutation, total recorded donor number\",Source=/mnt/lustre/reference/genomeinfo/qannotate/icgc_germline_qsnp_PUBLIC.vcf,FileDate=null>");
		data.add("##INFO=<ID=FLANK,Number=1,Type=String,Description=\"Flanking sequence either side of variant\">");
		data.add("##INFO=<ID=CONF,Number=.,Type=String,Description=\"set to HIGH if the variants passed all filter, appeared on more than 4 novel stars reads and more than 5 reads contains variants, is adjacent to reference sequence with less than 6 homopolymer base; Or set to LOW if the variants passed MIUN/MIN/GERM filter, appeared on more than 4 novel stars reads and more than 4 reads contains variants;Otherwise set to Zero if the variants didn't matched one of above conditions.\">");
		data.add("##FORMAT=<ID=OABS,Number=1,Type=String,Description=\"Observed Alleles By Strand: semi-colon separated list of observed alleles with each one in this format: forward_strand_count[avg_base_quality]reverse_strand_count[avg_base_quality], e.g. A18[39]12[42]\">");
		data.add("##FORMAT=<ID=NNS,Number=1,Type=Integer,Description=\"Number of novel starts not considering read pair\">");
		data.add("##FORMAT=<ID=MR,Number=1,Type=Integer,Description=\"Number of mutant/variant reads\">");
		data.add("##FORMAT=<ID=GD,Number=1,Type=String,Description=\"Genotype details: specific alleles (A,G,T or C)\">");
		data.add("##FORMAT=<ID=ACCS,Number=.,Type=String,Description=\"Allele Count Compound Snp: lists read sequence and count (forward strand, reverse strand)\">");
		data.add("##FORMAT=<ID=AC,Number=.,Type=String,Description=\"Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]\">");
		data.add("##qPG=<ID=6,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:24:03,CL=\"qannotate --mode snpeff -d /mnt/lustre/reference/software/snpEff/GRCh37.75 -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.snpeff.log\">");
		data.add("##qPG=<ID=5,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:21:35,CL=\"qannotate --mode Confidence -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.hom -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.conf.log\">");
		data.add("##qPG=<ID=4,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:21:08,CL=\"qannotate --mode hom -d /mnt/lustre/reference/genomes/GRCh37_ICGC_standard_v2/indexes/GATK_3.3-0/GRCh37_ICGC_standard_v2.fa -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.germ -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.hom --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.hom.log\">");
		data.add("##qPG=<ID=3,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:21:02,CL=\"qannotate --mode germline -d /mnt/lustre/reference/genomeinfo/qannotate/icgc_germline_qsnp_PUBLIC.vcf -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.dbsnp -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.germ --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.germ.log\">");
		data.add("##qPG=<ID=2,Tool=qannotate,Version=2.0.1 (2566),Date=2019-07-26 15:20:36,CL=\"qannotate --mode dbsnp -d /mnt/lustre/reference/dbsnp/141/00-All.vcf -i /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.merged -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.dbsnp --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.dbsnp.log\">");
		data.add("##qPG=<ID=1,Tool=q3vcftools MergeSameSample,Version=0.1 (9971),Date=2019-07-26 15:18:18,CL=\"q3vcftools MergeSameSample -vcf /mnt/lustre/working/genomeinfo/analysis/4/0/4019018e-4a54-4fc7-834b-d858e9e8981a/4019018e-4a54-4fc7-834b-d858e9e8981a.vcf -vcf /mnt/lustre/working/genomeinfo/analysis/e/9/e90fd4e3-a6de-40be-8458-60dca990c0d7/e90fd4e3-a6de-40be-8458-60dca990c0d7.vcf -o /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.merged --log /mnt/lustre/working/genomeinfo/analysis/e/2/e2822842-5679-46be-a778-55ec65be6e07/e2822842-5679-46be-a778-55ec65be6e07.vcf.merged.log\">");
		data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tqasim_chr1_70p_T\tqasim_chr1_70p_B");



		VcfHeader header = new VcfHeader();
		for (String s : data) {
			header.addOrReplace(s);
		}
		
		VcfFileMeta meta = new VcfFileMeta(header);
		assertEquals(ContentType.SINGLE_CALLER_MULTIPLE_SAMPLES, meta.getType());
		assertEquals(1, meta.getFirstControlSamplePos());
		assertEquals(2, meta.getFirstTestSamplePos());
		assertEquals(1, meta.getAllControlPositions().size());
		assertEquals(1, meta.getAllTestPositions().size());
		
		
	}

}
