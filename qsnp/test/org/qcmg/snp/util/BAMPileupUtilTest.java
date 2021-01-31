package org.qcmg.snp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import htsjdk.samtools.SAMRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.model.QSnpRecord;
import org.qcmg.common.model.QSnpGATKRecord;

public class BAMPileupUtilTest {

	@Test
	public void testGetReadPositionStart() {
//	 position: 38240010, alignment end: 38240052, read length: 41 record: 2344_1201_1194        83      chr15   38240010        49      15M2D26M        chr15   38239842        -210    CACACACACACACACGTCAGCAAACTAATGTCTTTGACTGT       6:A@A@A@A@A@A@8>AAA@ABB@A@B@A?B@BB@A@AA??       OC:Z:9H41M      ZC:i:1  RG:Z:20100905231122897  NH:i:1  CM:i:2  NM:i:3  SM:i:65 OP:i:38240012   CQ:Z:BBAA@AB@=;BA?=:BA@A;@@A;<A??>8?9:@=?<><4(07=%%80<1 MQ:i:39 OQ:Z:7;IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII  CS:Z:T31121210022113032100132121311111111111111311111311
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(38240010);
		record.setReadBases(new byte[41]);
		record.setCigarString("15M2D26M");
		
		int offset = BAMPileupUtil.getReadPosition(record, 38240010);
		assertTrue(0 == offset);
	}
	
	@Test
	public void testGetReadPositionEnd() {
//	 position: 38240010, alignment end: 38240052, read length: 41 record: 2344_1201_1194        83      chr15   38240010        49      15M2D26M        chr15   38239842        -210    CACACACACACACACGTCAGCAAACTAATGTCTTTGACTGT       6:A@A@A@A@A@A@8>AAA@ABB@A@B@A?B@BB@A@AA??       OC:Z:9H41M      ZC:i:1  RG:Z:20100905231122897  NH:i:1  CM:i:2  NM:i:3  SM:i:65 OP:i:38240012   CQ:Z:BBAA@AB@=;BA?=:BA@A;@@A;<A??>8?9:@=?<><4(07=%%80<1 MQ:i:39 OQ:Z:7;IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII  CS:Z:T31121210022113032100132121311111111111111311111311
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(38240010);
		record.setReadBases(new byte[41]);
		record.setCigarString("15M2D26M");
		
		int offset = BAMPileupUtil.getReadPosition(record, 38240052);
		assertTrue(40 == offset);
	}
	
	@Test
	public void testGetReadPositionDeletion() {
//		offset: 45, position: 1334, alignment start: 1289, unclipped alignment start: 1289, alignment end: 1338
//		13:08:32.335 [main] INFO org.qcmg.snp.util.BAMPileupUtil - 28_1615_1541 99      GL000216.1      1289    29      25M5D20M        GL000216.1      1512    257     GTTCCATTCCATTCTATTCTGTACCATTCCATTCCATTCCATTCC   :=>;<>=><<>=><><=><>=<;;354599<;<::<;<;:=<><P   OC:Z:45M5H      ZC:i:2  RG:Z:20110414154541403  NH:i:3  CM:i:7  NM:i:7  SM:i:25 CQ:Z:A9BA@B:ABBB>A@BBB>AB</6B5@A:@@@B@BB?B<=A?B64<=A=+> MQ:i:19 OQ:Z:IIIIIIIIIIIIIIIIIIIIIDII5:::IIIIIIIIIIIIIIIII      CS:Z:T11020130201302233022113101302013020130201302013020
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(1289);
		record.setReadBases(new byte[45]);
		record.setCigarString("25M5D20M");
		
		assertEquals(40, BAMPileupUtil.getReadPosition(record, 1334));
		assertEquals(44, BAMPileupUtil.getReadPosition(record, 1338));
		
		record = new SAMRecord(null);
		record.setAlignmentStart(1289);
		record.setReadBases(new byte[45]);
		record.setCigarString("25M5D20M");
		
		assertEquals(39, BAMPileupUtil.getReadPosition(record, 1333));
		
		//position: 203012906, offset: 1, movingOffset: 2, record: 1909_1945_1498	99	chr1	203012896	52	2M9D48M	chr1	203013070	205	CAGGAGCTCTGTGCCCATTTGGAAGTATTTCAGTGTTTTCACAGTGGGGT	:><;><<=<>===<<<>=>>=<>>;:9:;;:<::;;<<=;=;>9Q;)CGS	OC:Z:50M	ZC:i:2	RG:Z:20110414154541403	NH:i:1	CM:i:3	NM:i:9	SM:i:86	OP:i:203012905	CQ:Z:B@?>AAA@>AA<8??B:@<A>3??=@@@=:@8<8>A8;=?@9:)5=()*1	MQ:i:42	OQ:Z:IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIB=I((2:1	CS:Z:T21202232221113001300102021330021211100021112111001

		record = new SAMRecord(null);
		record.setAlignmentStart(203012896);
		record.setReadBases(new byte[50]);
		record.setCigarString("2M9D48M");
		System.out.println("end: " + record.getAlignmentEnd());
		assertEquals(-1, BAMPileupUtil.getReadPosition(record, 203012906));
		
		record = new SAMRecord(null);
		record.setAlignmentStart(100);
		record.setReadBases(new byte[50]);
		record.setCigarString("50M");
		assertEquals(0, BAMPileupUtil.getReadPosition(record, 100));
		assertEquals(1, BAMPileupUtil.getReadPosition(record, 101));
		assertEquals(25, BAMPileupUtil.getReadPosition(record, 125));
		
		record = new SAMRecord(null);
		record.setAlignmentStart(100);
		record.setReadBases(new byte[50]);
		record.setCigarString("10M20D40M");
		assertEquals(0, BAMPileupUtil.getReadPosition(record, 100));
		assertEquals(1, BAMPileupUtil.getReadPosition(record, 101));
		assertEquals(9, BAMPileupUtil.getReadPosition(record, 109));
		assertEquals(-1, BAMPileupUtil.getReadPosition(record, 110));
		assertEquals(-1, BAMPileupUtil.getReadPosition(record, 125));
		assertEquals(-1, BAMPileupUtil.getReadPosition(record, 129));
		assertEquals(10, BAMPileupUtil.getReadPosition(record, 130));
		
	}
	
	@Test
	public void testExaminePileup() {
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(100);
		record.setReadBases("ACGT".getBytes());
		record.setBaseQualities("IIII".getBytes());
		record.setCigarString("35M");
		record.setAttribute("SM", Integer.valueOf(15));
		List<SAMRecord> records = new ArrayList<SAMRecord>();
		records.add(record);
		
		QSnpGATKRecord vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 100));
		
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		
		assertNotNull(vcfRecord.getPileup());
		assertEquals(1, vcfRecord.getPileup().size());
		assertEquals('A', vcfRecord.getPileup().get(0).getBase());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 101));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNotNull(vcfRecord.getPileup());
		assertEquals(1, vcfRecord.getPileup().size());
		assertEquals('C', vcfRecord.getPileup().get(0).getBase());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 102));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNotNull(vcfRecord.getPileup());
		assertEquals(1, vcfRecord.getPileup().size());
		assertEquals('G', vcfRecord.getPileup().get(0).getBase());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 103));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNotNull(vcfRecord.getPileup());
		assertEquals(1, vcfRecord.getPileup().size());
		assertEquals('T', vcfRecord.getPileup().get(0).getBase());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 104));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNull(vcfRecord.getPileup());
	}
	
	@Test
	public void testExaminePileupWithDeletion() {
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(100);
		record.setReadBases("AAAAAGAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTTAAAAA".getBytes());
		record.setBaseQualityString("IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII");
		record.setCigarString("5M10D35M5D5M");
		record.setAttribute("SM", Integer.valueOf(15));
		List<SAMRecord> records = new ArrayList<SAMRecord>();
		records.add(record);
		
		QSnpGATKRecord vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 100));
		VcfUtils.createVcfRecord( "chr1", 100);
		
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		
		assertNotNull(vcfRecord.getPileup());
		assertEquals(1, vcfRecord.getPileup().size());
		assertEquals('A', vcfRecord.getPileup().get(0).getBase());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 105));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNull(vcfRecord.getPileup());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 114));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNull(vcfRecord.getPileup());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 115));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNotNull(vcfRecord.getPileup());
		assertEquals(1, vcfRecord.getPileup().size());
		assertEquals('G', vcfRecord.getPileup().get(0).getBase());
		assertEquals(40, vcfRecord.getPileup().get(0).getTotalQualityScore());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 149));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNotNull(vcfRecord.getPileup());
		assertEquals(1, vcfRecord.getPileup().size());
		assertEquals('T', vcfRecord.getPileup().get(0).getBase());
		assertEquals(40, vcfRecord.getPileup().get(0).getTotalQualityScore());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 150));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNull(vcfRecord.getPileup());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 154));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNull(vcfRecord.getPileup());
		
		vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 155));
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		assertNotNull(vcfRecord.getPileup());
		assertEquals(1, vcfRecord.getPileup().size());
		assertEquals('A', vcfRecord.getPileup().get(0).getBase());
		assertEquals(40, vcfRecord.getPileup().get(0).getTotalQualityScore());
		
	}
	
	@Test
	public void testExaminePileupWithRealLifeData() {
//		chr1	753224	1	48M2H	*	0	0	GTGCTGTAGTCACACTGACTGTGACTACTGCTCAGTCCCTGAGGACTG	:==;>><;<=<>;%%%2:79;=8534%%%%4545/21+,<916;LHGA	ZC:i:2	MD:Z:14A0G11A0G19	RG:Z:20110414154541403	NH:i:0	CM:i:5	NM:i:4	SM:i:22	CQ:Z:BBA@A=A9
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(753224);
		record.setReadBases("GTGCTGTAGTCACACTGACTGTGACTACTGCTCAGTCCCTGAGGACTG".getBytes());
		record.setBaseQualityString(":==;>><;<=<>;%%%2:79;=8534%%%%4545/21+,<916;LHGA");
		record.setCigarString("48M2H");
		record.setAttribute("SM", Integer.valueOf(22));
		List<SAMRecord> records = new ArrayList<SAMRecord>();
		records.add(record);
		
//		chr1	753242	3	43M7H	chrY	7987772	0	CTGTGACTAAGGCTCAGTCCCTGAGGAGTGCCTTGGCATGGGC	?AB@BB>A@BAA?AAAAAA@@A>B@@;%<92:<B=:=10AAB5	ZC:i:0	MD:Z:43	RG:Z:20100907043607557	NH:i:2	CM:i:2	NM:i:0	SM:i:22	CQ:Z:AA<3?<?>?=?:<8=29/:</:.7>,>;%:%+
		SAMRecord record2 = new SAMRecord(null);
		record2.setAlignmentStart(753242);
		record2.setReadBases("CTGTGACTAAGGCTCAGTCCCTGAGGAGTGCCTTGGCATGGGC".getBytes());
		record2.setBaseQualityString("?AB@BB>A@BAA?AAAAAA@@A>B@@;%<92:<B=:=10AAB5");
		record2.setCigarString("43M7H");
		record2.setAttribute("SM", Integer.valueOf(22));
		records.add(record2);
		
//		chr1	753250	0	2H48M	=	753120	-177	AAGGCTCAGTCCCTCAGGAGTGCCTTGGCATGGGCTGCTTTAGGCTGT	CB>=<>;</;;;:4-0431+299%%><<48><<==?<<8>;=<<;>;:	ZC:i:2	MD:Z:14G33	RG:Z:20110414154541403	NH:i:2	CM:i:4	NM:i:1	SM:i:22	
		SAMRecord record3 = new SAMRecord(null);
		record3.setAlignmentStart(753250);
		record3.setReadBases("AAGGCTCAGTCCCTCAGGAGTGCCTTGGCATGGGCTGCTTTAGGCTGT".getBytes());
		record3.setBaseQualityString("CB>=<>;</;;;:4-0431+299%%><<48><<==?<<8>;=<<;>;:");
		record3.setCigarString("2H48M");
		record3.setAttribute("SM", Integer.valueOf(22));
		records.add(record3);
		
		QSnpGATKRecord vcfRecord = new QSnpGATKRecord(VcfUtils.createVcfRecord( "chr1", 753269));
		
		BAMPileupUtil.examinePileupVCF(records, vcfRecord);
		
		assertNotNull(vcfRecord.getPileup());
		assertEquals(2, vcfRecord.getPileup().size());
		assertEquals('G', vcfRecord.getPileup().get(0).getBase());
		assertEquals(2, vcfRecord.getPileup().get(0).getTotalCount());
		assertEquals('C', vcfRecord.getPileup().get(1).getBase());
		assertEquals(1, vcfRecord.getPileup().get(1).getTotalCount());
		assertEquals(39, vcfRecord.getPileup().get(1).getTotalQualityScore());
	}
	
	@Test
	public void testEligibleSamRecord() {
		SAMRecord record = new SAMRecord(null);
		assertFalse(BAMPileupUtil.eligibleSamRecord(record));
		record.setAttribute("SM", Integer.valueOf(14));
		record.setCigarString("35M");
		assertFalse(BAMPileupUtil.eligibleSamRecord(record));
		record.setAttribute("SM", Integer.valueOf(15));
		assertTrue(BAMPileupUtil.eligibleSamRecord(record));
		record.setCigarString("33M");
		assertFalse(BAMPileupUtil.eligibleSamRecord(record));
		record.setReadPairedFlag(true);
		record.setProperPairFlag(true);
		record.setSecondOfPairFlag(true);
		assertTrue(BAMPileupUtil.eligibleSamRecord(record));
	}	
}
