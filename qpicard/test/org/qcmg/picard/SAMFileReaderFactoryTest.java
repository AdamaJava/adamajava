package org.qcmg.picard;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SAMFileReaderFactoryTest {
	
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	private static final int validBamRecordCount = 5;
	private static final int inValidBamRecordCount = 6;
	
	@Test
	public void testInvalidHeaderValidBody() throws IOException {
		
		File bamFile = testFolder.newFile("testInvalidHeaderValidBody.bam");
		getBamFile(bamFile, false, true);
		// no validation set - should pick up bwa in header and set validation to silent
		int recordCount = 0;
		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
		try {
			for (SAMRecord s : reader) {
				recordCount++;
			}
			assertEquals(validBamRecordCount, recordCount);
		} finally {
			reader.close();
		}
		
		// this time set the validation stringency to strict - should fail
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "strict");
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
		} finally {
			reader.close();
		}
		
		// this time set the validation stringency to silent - should work
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "silent");
			for (SAMRecord s : reader) {
				recordCount++;
			}
		} finally {
			reader.close();
		}
		assertEquals(validBamRecordCount, recordCount);
		
		// this time set the validation stringency to lenient - should work
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "lenient");
			for (SAMRecord s : reader) {
				recordCount++;
			}
		} finally {
			reader.close();
		}
		assertEquals(validBamRecordCount, recordCount);
		
	}
	
	@Test
	public void testValidHeaderInvalidBody() throws IOException {
		File bamFile = testFolder.newFile("testValidHeaderInvalidBody.bam");
		getBamFile(bamFile, true, false);
		// no validation set - should pick up bwa in header and set validation to silent
		int recordCount = 0;
		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
		try {
			for (SAMRecord s : reader) {
				recordCount++;
			}
			assertEquals(inValidBamRecordCount, recordCount);
		} finally {
			reader.close();
		}
		
		// this time set the validation stringency to strict - should fail
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "strict");
			for (SAMRecord s : reader) {
				// do something
			}
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
		} finally {
			reader.close();
		}
		
		// this time set the validation stringency to silent - should work
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "silent");
			for (SAMRecord s : reader) recordCount++;
		} finally {
			reader.close();
		}
		assertEquals(inValidBamRecordCount, recordCount);
		
		// this time set the validation stringency to lenient - should work
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "lenient");
			for (SAMRecord s : reader) recordCount++;
		} finally {
			reader.close();
		}
		assertEquals(inValidBamRecordCount, recordCount);
		
	}
	
	@Test
	public void testInvalidHeaderInvalidBody() throws IOException {
		File bamFile = testFolder.newFile("testInvalidHeaderInvalidBody.bam");
		getBamFile(bamFile, false, false);
		// no validation set - should pick up bwa in header and set validation to silent
		int recordCount = 0;
		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
		try {
			for (SAMRecord s : reader) recordCount++;
			assertEquals(inValidBamRecordCount, recordCount);
		} finally {
			reader.close();
		}
		
		// this time set the validation stringency to strict - should fail
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "strict");
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
		} finally {
			reader.close();
		}
		
		// this time set the validation stringency to silent - should work
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "silent");
			for (SAMRecord s : reader) recordCount++;
		} finally {
			reader.close();
		}
		assertEquals(inValidBamRecordCount, recordCount);
		
		// this time set the validation stringency to lenient - should work
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "lenient");
			for (SAMRecord s : reader) {
				recordCount++;
			}
		} finally {
			reader.close();
		}
		assertEquals(inValidBamRecordCount, recordCount);
		
	}
	
	@Test
	public void testValidHeaderValidBody() throws IOException {
		File bamFile = testFolder.newFile("testValidHeaderValidBody.bam");
		getBamFile(bamFile, true, true);
		// no validation set - should pick up bwa in header and set validation to silent
		int recordCount = 0;
		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
		try {
			for (SAMRecord s : reader) {
				recordCount++;
			}
			assertEquals(validBamRecordCount, recordCount);
		} finally {
			reader.close();
		}
		
		// this time set the validation stringency to strict - should pass
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "strict");
			for (SAMRecord s : reader) {
				recordCount++;
			}
		} finally {
			reader.close();
		}
		assertEquals(validBamRecordCount, recordCount);
		
		// this time set the validation stringency to silent - should work
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "silent");
			for (SAMRecord s : reader) {
				recordCount++;
			}
		} finally {
			reader.close();
		}
		assertEquals(validBamRecordCount, recordCount);
		
		// this time set the validation stringency to lenient - should work
		recordCount = 0;
		try {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "lenient");
			for (SAMRecord s : reader) {
				recordCount++;
			}
		} finally {
			reader.close();
		}
		assertEquals(validBamRecordCount, recordCount);
	}
	
	
	private static void getBamFile(File bamFile, boolean validHeader, boolean validRecords) {
		SAMFileHeader header = getHeader(validHeader);
		SAMOrBAMWriterFactory factory = new SAMOrBAMWriterFactory(header, false, bamFile, false);
		try {
			SAMFileWriter writer = factory.getWriter();
			for (SAMRecord s : getRecords(validRecords,header)) {
				writer.addAlignment(s);
			}
		} finally {
			factory.closeWriter();
		}
	}
	
	private static List<SAMRecord> getRecords(boolean valid, SAMFileHeader header) {
		List<SAMRecord> records = new ArrayList<SAMRecord>();
//		records.add("HS2000-152_756:1:1316:11602:65138	89	chr1	9993	25	100M	=	9993	0	TCTTCCGATCTCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	B@??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?=7D9@@8.C;B8=.HGDBBBCCD::*GGD:?*FDGFCA>EIHEEBEAEFDFFC=+?DD@@@	X0:i:1	X1:i:0	ZC:i:9	MD:Z:0C0T0G6A0A89	PG:Z:MarkDuplicates	RG:Z:20130325103517169	XG:i:0	AM:i:0	NM:i:5	SM:i:25	XM:i:5	XN:i:8	XO:i:0	XT:A:U");
		SAMRecord sam = new SAMRecord(header);
		sam.setAlignmentStart(9993);
		sam.setReferenceName("chr1");
		sam.setReadName("HS2000-152_756:1:1316:11602:65138");
		sam.setReadString("TCTTCCGATCTCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA");
		sam.setBaseQualityString("B@??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?=7D9@@8.C;B8=.HGDBBBCCD::*GGD:?*FDGFCA>EIHEEBEAEFDFFC=+?DD@@@");
		sam.setCigarString("100M");
		records.add(sam);
		
//		records.add("HS2000-152_756:2:1212:5424:43221	99	chr1	10001	29	45M1I14M4D9M2D21M10S	=	10101	199	TAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCCCACCCCCTACCCCACACTCACCCACCCCCTAACCTCAGCACCCC	CCCFFFFFFHHHHGGIJJIIJJJJJJJJJJGEHIJJ9)?)?D))?(?BFB;CD@C#############################################	ZC:i:7	MD:Z:54T0A3^CTAA5A3^TA2C2A4T0A0A8	PG:Z:MarkDuplicates	RG:Z:20130325112045146	XG:i:7	AM:i:29	NM:i:15	SM:i:29	XM:i:8	XO:i:3	XT:A:M");
		sam = new SAMRecord(header);
		sam.setAlignmentStart(10001);
		sam.setReferenceName("chr1");
		sam.setReadName("HS2000-152_756:2:1212:5424:43221");
		sam.setReadString("TAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCCCACCCCCTACCCCACACTCACCCACCCCCTAACCTCAGCACCCC");
		sam.setBaseQualityString("CCCFFFFFFHHHHGGIJJIIJJJJJJJJJJGEHIJJ9)?)?D))?(?BFB;CD@C#############################################");
		sam.setCigarString("45M1I14M4D9M2D21M10S");
		records.add(sam);
		
//		records.add("HS2000-152_757:7:1311:15321:98529	163	chr1	10002	0	100M	=	10020	118	AACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACACTAACCCTAACCCTAACCCTAACCCTAACCCTAACC	@@@FFDFFB;DFHIHIIIIJGICGGGGGGF?9CF;@?DD;BDGG2DEFGC9EDHHI@CCEEFE)=?33;6;6;@AA;=A?2<?((59(9<<((28?<?B8	X0:i:362	ZC:i:8	MD:Z:63C36	PG:Z:MarkDuplicates	RG:Z:20130325084856212	XG:i:0	AM:i:0	NM:i:1	SM:i:0	XM:i:1	XO:i:0	XT:A:R");
		sam = new SAMRecord(header);
		sam.setAlignmentStart(10002);
		sam.setReferenceName("chr1");
		sam.setReadName("HS2000-152_757:7:1311:15321:98529");
		sam.setReadString("AACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACACTAACCCTAACCCTAACCCTAACCCTAACCCTAACC");
		sam.setBaseQualityString("@@@FFDFFB;DFHIHIIIIJGICGGGGGGF?9CF;@?DD;BDGG2DEFGC9EDHHI@CCEEFE)=?33;6;6;@AA;=A?2<?((59(9<<((28?<?B8");
		sam.setCigarString("100M");
		records.add(sam);
		
//		records.add("HS2000-152_756:2:2306:7001:4421	99	chr1	10003	29	2S53M1I44M	=	10330	426	TGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA	CCCFFFFFHHHHHIJJJJEHGIIJIHGIIJIFGIJJJGGHHIIGCDGIHI>GIIEAFGJJI@EGFDFCE@DDDE@CA=A;3;?BDB?CD@DB9ADDBA9?	ZC:i:7	MD:Z:5A5A5A5A5A5A5A5A49	PG:Z:MarkDuplicates	RG:Z:20130325112045146	XG:i:1	AM:i:29	NM:i:9	SM:i:29	XM:i:8	XO:i:1	XT:A:M");
		sam = new SAMRecord(header);
		sam.setAlignmentStart(10003);
		sam.setReferenceName("chr1");
		sam.setReadName("HS2000-152_756:2:2306:7001:4421");
		sam.setReadString("TGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA");
		sam.setBaseQualityString("CCCFFFFFHHHHHIJJJJEHGIIJIHGIIJIFGIJJJGGHHIIGCDGIHI>GIIEAFGJJI@EGFDFCE@DDDE@CA=A;3;?BDB?CD@DB9ADDBA9?");
		sam.setCigarString("2S53M1I44M");
		records.add(sam);
		
//		records.add("HS2000-152_756:1:1215:14830:88102	99	chr1	10004	29	24M4D76M	=	10441	537	CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC	CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB	ZC:i:9	MD:Z:5A5A5A6^CCCT54A11A9	PG:Z:MarkDuplicates	RG:Z:20130325103517169	XG:i:4	AM:i:29	NM:i:9	SM:i:29	XM:i:5	XO:i:1	XT:A:M");
		sam = new SAMRecord(header);
		sam.setAlignmentStart(10004);
		sam.setReferenceName("chr1");
		sam.setReadName("HS2000-152_756:1:1215:14830:88102");
		sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
		sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
		sam.setCigarString("24M4D76M");
		records.add(sam);
		
		if ( ! valid) {
			sam = new SAMRecord(header);
			sam.setAlignmentStart(10005);
			sam.setReferenceName("chr1");
			sam.setReadName("HS2000-152_756:1:1215:14830:88103");
			sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
			sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
			sam.setCigarString("24M4D76M");
			sam.setReadUnmappedFlag(true);
			sam.setMappingQuality(1);
			records.add(sam);
		}
		
		return records;
	}
	
	private static SAMFileHeader getHeader(boolean valid) {
		SAMFileHeader header = new SAMFileHeader();
		header.setTextHeader(VALID_HEADER);
		
		SAMProgramRecord bwaPG = new SAMProgramRecord("bwa");
		bwaPG.setProgramName("bwa");
		bwaPG.setProgramVersion("0.6.1-r104");
		header.addProgramRecord(bwaPG);
//		"@PG	ID:bwa	PN:bwa	zc:6	VN:0.6.1-r104"+
		
		if ( ! valid) {
			SAMProgramRecord invalidPG = new SAMProgramRecord("blah");
			invalidPG.setAttribute("CL", "");
			header.addProgramRecord(invalidPG);
			
			SAMReadGroupRecord rgRec = new SAMReadGroupRecord("ID");
			rgRec.setAttribute("PG", "tmap");
			header.addReadGroup(rgRec);
		}
		
		// looks like we need this to be specifically defined
		SAMSequenceDictionary seqDict = new SAMSequenceDictionary();
		SAMSequenceRecord seqRec1 = new SAMSequenceRecord("chr1", 249250621);
		SAMSequenceRecord seqRec2 = new SAMSequenceRecord("chr2", 243199373);
		SAMSequenceRecord seqRec3 = new SAMSequenceRecord("chr3", 198022430);
		SAMSequenceRecord seqRec4 = new SAMSequenceRecord("chr4", 191154276);
		SAMSequenceRecord seqRec5 = new SAMSequenceRecord("chr5", 180915260);
		seqDict.addSequence(seqRec1);
		seqDict.addSequence(seqRec2);
		seqDict.addSequence(seqRec3);
		seqDict.addSequence(seqRec4);
		seqDict.addSequence(seqRec5);
		header.setSequenceDictionary(seqDict);
		
		return header;
	}
	
	private static final String VALID_HEADER = "@HD	VN:1.4	GO:none	SO:coordinate"+
"@SQ	SN:chr1	LN:249250621"+
"@SQ	SN:chr2	LN:243199373"+
"@SQ	SN:chr3	LN:198022430"+
"@SQ	SN:chr4	LN:191154276"+
"@SQ	SN:chr5	LN:180915260"+
"@SQ	SN:chr6	LN:171115067"+
"@SQ	SN:chr7	LN:159138663"+
"@SQ	SN:chr8	LN:146364022"+
"@SQ	SN:chr9	LN:141213431"+
"@SQ	SN:chr10	LN:135534747"+
"@SQ	SN:chr11	LN:135006516"+
"@SQ	SN:chr12	LN:133851895"+
"@SQ	SN:chr13	LN:115169878"+
"@SQ	SN:chr14	LN:107349540"+
"@SQ	SN:chr15	LN:102531392"+
"@SQ	SN:chr16	LN:90354753"+
"@SQ	SN:chr17	LN:81195210"+
"@SQ	SN:chr18	LN:78077248"+
"@SQ	SN:chr19	LN:59128983"+
"@SQ	SN:chr20	LN:63025520"+
"@SQ	SN:chr21	LN:48129895"+
"@SQ	SN:chr22	LN:51304566"+
"@RG	ID:20130325090048212	PL:ILLUMINA	PU:lane_8.nobc	LB:LP6005273-DNA_G04	zc:6:/path/project2/SSSS_076/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.bam	SM:SSSS_076	PG:qbamfix	CN:QCMG"+
"@RG	ID:20130325112045146	PL:ILLUMINA	PU:lane_2.nobc	LB:LP6005273-DNA_G04	zc:7:/path/project2/SSSS_076/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.bam	SM:SSSS_076	PG:qbamfix	CN:QCMG"+
"@RG	ID:20130325084856212	PL:ILLUMINA	PU:lane_7.nobc	LB:LP6005273-DNA_G04	zc:8:/path/project2/SSSS_076/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.bam	SM:SSSS_076	PG:qbamfix	CN:QCMG"+
"@RG	ID:20130325103517169	PL:ILLUMINA	PU:lane_1.nobc	LB:LP6005273-DNA_G04	zc:9:/path/project2/SSSS_076/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.bam	SM:SSSS_076	PG:qbamfix	CN:QCMG"+
"@PG	ID:0ee948df-cf28-4456-be94-15b3aa9ed7b4	PN:qbammerge	zc:10	VN:0.6pre (6216)	CL:qbammerge --output /scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam --log /scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam.qmrg.log --input /path/project2/SSSS_076/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.bam --input /path/project2/SSSS_076/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.bam --input /path/project2/SSSS_076/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.bam --input /path/project2/SSSS_076/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.bam"+
"@PG	ID:7022afd6-d0fb-47b9-90d0-c6f3ef0f98e6	PN:qbamfix	zc:9	VN:qbamfix, version 0.2pre	CL:qbamfix --input /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.sam --output /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.fixrg.bam --log /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.fix.log --RGLB LP6005273-DNA_G04 --RGSM SSSS_076 --tmpdir /scratch/329416.machine"+
"@PG	ID:9b400d65-3b8d-4f98-b246-ba4280b916ae	PN:qbamfix	zc:7	VN:qbamfix, version 0.2pre	CL:qbamfix --input /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.sam --output /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.fixrg.bam --log /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.fix.log --RGLB LP6005273-DNA_G04 --RGSM SSSS_076 --tmpdir /scratch/329421.machine"+
"@PG	ID:9c662b98-b27f-4a94-bd76-d1937d33ab9c	PN:qbamfix	zc:8	VN:qbamfix, version 0.2pre	CL:qbamfix --input /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.sam --output /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.fixrg.bam --log /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.fix.log --RGLB LP6005273-DNA_G04 --RGSM SSSS_076 --tmpdir /scratch/329449.machine"+
"@PG	ID:MarkDuplicates	PN:MarkDuplicates	zc:5	VN:1.88(1394)	QN:picard_markdups	CL:net.sf.picard.sam.MarkDuplicates INPUT=[/scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.withdups.bam] OUTPUT=/scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam METRICS_FILE=/scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam.dedup_metrics COMMENT=[CN:QCMG"+
"@PG	ID:ad74545e-b36f-4587-aa46-812baa87f467	PN:qbamfix	zc:6	VN:qbamfix, version 0.2pre	CL:qbamfix --input /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.sam --output /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.fixrg.bam --log /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.fix.log --RGLB LP6005273-DNA_G04 --RGSM SSSS_076 --tmpdir /scratch/329452.machine"+
"@PG	ID:bwa	PN:bwa	zc:6	VN:0.6.1-r104"+
"@PG	ID:bwa.1	PN:bwa	zc:7	VN:0.6.1-r104"+
"@PG	ID:bwa.2	PN:bwa	zc:8	VN:0.6.1-r104"+
"@PG	ID:bwa.3	PN:bwa	zc:9	VN:0.6.1-r104"+
"@PG	ID:d0cac0d3-3318-42bc-91d2-1d5abc92ab80	PN:qbammerge	zc:11	VN:0.6pre (6239)	CL:qbammerge --output /path/project2/SSSS_076/folder/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.bam --log /path/project2/SSSS_076/folder/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.bam.log --co CN:QCMG QN:qbammaker CL:-v --auto /path/project2/SSSS_076 --logfile SSSS_076.auto.log --co CN:QCMG QN:qlimsmeta Aligner=bwa Capture Kit=kit1 Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=HiSeq Species Reference Genome=Homo sapiens (reference) --co CN:QCMG QN:qmapset Aligner=bwa Capture Kit= Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Mapset=130207_SN152_0756_AC1PJYACXX.lane_1.nobc Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=Plat1 Species Reference Genome=Homo sapiens (reference) --co CN:QCMG QN:qmapset Aligner=bwa Capture Kit= Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Mapset=130207_SN152_0757_BD1U57ACXX.lane_8.nobc Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=Plat1 Species Reference Genome=Homo sapiens (reference) --co CN:QCMG QN:qmapset Aligner=bwa Capture Kit= Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Mapset=130207_SN152_0757_BD1U57ACXX.lane_7.nobc Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=Plat1 Species Reference Genome=Homo sapiens (reference) --co CN:QCMG QN:qmapset Aligner=bwa Capture Kit= Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Mapset=130207_SN152_0756_AC1PJYACXX.lane_2.nobc Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=Plat1 Species Reference Genome=Homo sapiens (reference) --input /scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam"+
"@CO	CN:QCMG	QN:picard_markdups	CL:OPTICAL_DUPLICATE_PIXEL_DISTANCE=10 VALIDATION_STRINGENCY=SILENT INPUT=${SCRATCH_DIR}/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.withdups.bam OUTPUT=${SCRATCH_DIR}/SSSS076_1DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam METRICS_FILE=${SCRATCH_DIR}/SSSS076_1DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam.dedup_metrics"+
"@CO	CN:QCMG	QN:qbammaker	CL:-v --auto /path/project2/SSSS_076 --logfile SSSS_076.auto.log"+
"@CO	CN:QCMG	QN:qlimsmeta	Aligner=bwa	Capture Kit=kit1	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=HiSeq	Species Reference Genome=Homo sapiens (reference)"+
"@CO	CN:QCMG	QN:qmapset	Aligner=bwa	Capture Kit=	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Mapset=130207_SN152_0756_AC1PJYACXX.lane_1.nobc	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=Plat1	Species Reference Genome=Homo sapiens (reference)"+
"@CO	CN:QCMG	QN:qmapset	Aligner=bwa	Capture Kit=	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Mapset=130207_SN152_0757_BD1U57ACXX.lane_8.nobc	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=Plat1	Species Reference Genome=Homo sapiens (reference)"+
"@CO	CN:QCMG	QN:qmapset	Aligner=bwa	Capture Kit=	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Mapset=130207_SN152_0757_BD1U57ACXX.lane_7.nobc	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=Plat1	Species Reference Genome=Homo sapiens (reference)"+
"@CO	CN:QCMG	QN:qmapset	Aligner=bwa	Capture Kit=	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Mapset=130207_SN152_0756_AC1PJYACXX.lane_2.nobc	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=Plat1	Species Reference Genome=Homo sapiens (reference)";
	
}
