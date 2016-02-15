package org.qcmg.motif;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import htsjdk.samtools.SAMRecord;

import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.motif.util.MotifsAndRegexes;
import org.qcmg.motif.util.RegionCounter;
import org.qcmg.motif.util.RegionType;

public class MotifCoverageAlgorithmTest {
	
	Motifs stageOneMotifs;
	Motifs stageTwoMotifs;
	String stageOneRegex;
	String stageTwoRegex;
	
	SAMRecord samPass;
	SAMRecord samFail;
	
	MotifsAndRegexes mAndR;
	MotifsAndRegexes mAndRStage1RegexStage2String;
	MotifsAndRegexes mAndRStage1StringStage2Regex;
	MotifCoverageAlgorithm mca;
	MotifCoverageAlgorithm mcaStage1RegexStage2String;
	MotifCoverageAlgorithm mcaStage1StringStage2Regex;
	
	@Before
	public void setup() {
		stageOneMotifs = new Motifs(true, "TTAGGGTTAGGG");
		stageTwoMotifs = null;
		stageOneRegex = null;
		stageTwoRegex = "((TTA|TCA|TTC|GTA|TGA|TTG|TAA|ATA|CTA|TTT|TTAA)GGG){2,}|(CCC(TAA|TGA|GAA|TAC|TCA|CAA|TTA|TAT|TAG|AAA|TTAA)){2,}";
		mAndR = new MotifsAndRegexes(stageOneMotifs, stageOneRegex, stageTwoMotifs, stageTwoRegex, 10000);
		mAndRStage1RegexStage2String = new MotifsAndRegexes(null, "...GGG{2,}|CCC...{2,}", new Motifs(true, "TTAGGGTTAGGG"), null, 10000);
		mca = new MotifCoverageAlgorithm(mAndR);
		mcaStage1RegexStage2String = new MotifCoverageAlgorithm(mAndRStage1RegexStage2String);
		
		mAndRStage1StringStage2Regex = new MotifsAndRegexes(new Motifs(true, "TTAGGGTTAGGGTTAGGG"), null , null, "(...GGG){2,}|(CCC...){2,}", 10000);
		mcaStage1StringStage2Regex = new MotifCoverageAlgorithm(mAndRStage1StringStage2Regex);
		
		samPass = new SAMRecord(null);
		samPass.setReferenceName("chr1");
		samPass.setAlignmentStart(100);
		samPass.setReadString("TTAGGGTTAGGGTTAGGGTTAGGG");
		samFail = new SAMRecord(null);
		samFail.setReferenceName("chr1");
		samFail.setAlignmentStart(100);
		samFail.setReadString("ACGTACGTACGTACGT");
	}
	
	@Test
	public void stageOneSearchNullEmptyAndNoMatch() {
		assertEquals(false, mca.stageOneSearch(null));
		assertEquals(false, mca.stageOneSearch(""));
		assertEquals(false, mca.stageOneSearch("ACGTACGTACGTACGTACGT"));
		assertEquals(false, mca.stageOneSearch("ABCDEFGHIJKLMNOP"));
		assertEquals(false, mca.stageOneSearch("TTAGGGTTAGGTTAGGGTTAGG"));
	}
	
	@Test
	public void stageOneSearchMatchAndComplement() {
		assertEquals(true, mca.stageOneSearch("TTAGGGTTAGGGTTAGGTTAGG"));
		assertEquals(true, mca.stageOneSearch("CCCTAACCCTAAACGT"));
	}
	
	@Test
	public void stageTwoMotifsNullEmptyAndNoMatch() {
		String s = mca.getStageTwoMotifs(null);
		assertEquals(null, s);
		assertNull(mca.getStageTwoMotifs(""));
		s = mca.getStageTwoMotifs("ACGTACGTACGTACGTACGTACGT");
		assertEquals(null, s);
		s = mca.getStageTwoMotifs("ACGTTTAGGTTAGGACGT");
		assertEquals(null, s);
	}
//	@Test
//	public void stageTwoMotifsNullEmptyAndNoMatch() {
//		RegionCounter rc = new RegionCounter(RegionType.INCLUDES);
//		mca.getStageTwoMotifs(null, false, false, rc);
//		assertNull(null, rc.getMotifsReverseStrand());
//		assertNull(null, rc.getMotifsForwardStrand());
//		mca.getStageTwoMotifs("", false, false, rc);
////		assertNull(mca.getStageTwoMotifs(""));
//		assertNull(null, rc.getMotifsReverseStrand());
//		assertNull(null, rc.getMotifsForwardStrand());
//		mca.getStageTwoMotifs("ACGTACGTACGTACGTACGTACGT", false, false, rc);
//		assertNull(null, rc.getMotifsReverseStrand());
//		assertNull(null, rc.getMotifsForwardStrand());
//		mca.getStageTwoMotifs("ACGTTTAGGTTAGGACGT", false, false, rc);
//		assertNull(null, rc.getMotifsReverseStrand());
//		assertNull(null, rc.getMotifsForwardStrand());
//	}
	
	@Test
	public void stageTwoMotifsSameMatch() {
		String s = mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGACGT");
		assertEquals("TTAGGGTTAGGG", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGACGT"));
		assertEquals("TTAGGGTTAGGGTTAGGG", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGTTAGGGACGT"));
		assertEquals("TTAGGGTTAGGGTTAGGGTTAGGG", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGTTAGGGTTAGGGACGT"));
		assertEquals("TTAGGGTTAGGGTTAGGGTTAGGGTTAGGG", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGACGT"));
	}
//	@Test
//	public void stageTwoMotifsSameMatch() {
//		RegionCounter rc = new RegionCounter(RegionType.INCLUDES);
//		mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGACGT", false, false, rc);
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGG").intValue());
//		mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGTTAGGGACGT", false, false, rc);
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGG").intValue());
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGGTTAGGG").intValue());
//		mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGTTAGGGTTAGGGACGT", false, false, rc);
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGG").intValue());
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGGTTAGGG").intValue());
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGGTTAGGGTTAGGG").intValue());
//		mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGACGT", false, false, rc);
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGG").intValue());
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGGTTAGGG").intValue());
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGGTTAGGGTTAGGG").intValue());
//		assertEquals(1, rc.getMotifsReverseStrand().get("TTAGGGTTAGGGTTAGGGTTAGGGTTAGGG").intValue());
//	}
	@Test
	public void stageTwoMotifsDiffMatch() {
		assertEquals("TTAGGGTTAGGG:CCCTGACCCTGA", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGCCCTGACCCTGAACGT"));
	}
//	@Test
//	public void stageTwoMotifsDiffMatch() {
//		RegionCounter rc = new RegionCounter(RegionType.INCLUDES);
//		mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGCCCTGACCCTGAACGT", true, false, rc);
//		assertEquals(1, rc.getMotifsForwardStrand().get("TTAGGGTTAGGG").intValue());
//		assertEquals(1, rc.getMotifsForwardStrand().get("CCCTGACCCTGA").intValue());
////		assertEquals("TTAGGGTTAGGG:CCCTGACCCTGA", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGCCCTGACCCTGAACGT"));
//	}
	
	@Test
	public void noValidCountersInMap() {
		Map<ChrPosition, RegionCounter> map = new HashMap<>();
		assertEquals(null, mca.getCounterFromMap(map, null));
		
		map.put(new ChrRangePosition("1", 100, 200), new RegionCounter(RegionType.INCLUDES));
		assertEquals(null, mca.getCounterFromMap(map, new ChrRangePosition("1", 300, 400)));
		RegionCounter rc300To400 = new RegionCounter(RegionType.INCLUDES);
		map.put(new ChrRangePosition("1", 300, 400), rc300To400);
		assertEquals(rc300To400, mca.getCounterFromMap(map, new ChrRangePosition("1", 299, 400)));
		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void applyToDoubleNull() {
		mca.applyTo(null, null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void applyToSingleNull() {
		mca.applyTo(null, new HashMap<ChrPosition, RegionCounter>());
	}
	@Test(expected=IllegalArgumentException.class)
	public void applyToSingleNull2() {
		mca.applyTo(samPass, null);
	}
	
	@Test
	public void applyToStageOnePass() {
		Map<ChrPosition, RegionCounter> map = new HashMap<>();
		RegionCounter rc = new RegionCounter(RegionType.INCLUDES);
		map.put(new ChrRangePosition("chr1", 1, 1000), rc);
		for (int i = 0 ; i < 100 ; i++) {
			assertEquals(true, mca.applyTo(samPass, map));
		}
		assertEquals(true, rc.hasMotifs());
		assertEquals(100, rc.getStage1Coverage());
		assertEquals(100, rc.getStage2Coverage());
		assertEquals(0, rc.getTotalCoverage());		// not yet implemented
//		assertEquals(1, rc.getMotifsForwardStrand().size());
//		assertEquals(100, rc.getMotifsForwardStrand().get(samPass.getReadString()).intValue());
		assertEquals(4 * 6 * 100 + 99, rc.getMotifsForwardStrand().length());
	}
//	@Test
//	public void applyToStageOnePass() {
//		Map<ChrPosition, RegionCounter> map = new HashMap<>();
//		RegionCounter rc = new RegionCounter(RegionType.INCLUDES);
//		map.put(new ChrPosition("chr1", 1, 1000), rc);
//		for (int i = 0 ; i < 100 ; i++) {
//			assertEquals(true, mca.applyTo(samPass, map));
//		}
//		assertEquals(true, rc.hasMotifs());
//		assertEquals(100, rc.getStage1Coverage());
//		assertEquals(100, rc.getStage2Coverage());
//		assertEquals(0, rc.getTotalCoverage());		// not yet implemented
//		assertEquals(1, rc.getMotifsForwardStrand().size());
//		assertEquals(100, rc.getMotifsForwardStrand().get(samPass.getReadString()).intValue());
////		assertEquals(4 * 6 * 100 + 99, rc.getMotifsForwardStrand().length());
//	}
	
	@Test
	public void applyToStageOneFail() {
		Map<ChrPosition, RegionCounter> map = new HashMap<>();
		RegionCounter rc = new RegionCounter(RegionType.INCLUDES);
		map.put(new ChrRangePosition("chr1", 1, 1000), rc);
		for (int i = 0 ; i < 100 ; i++) {
			assertEquals(false, mca.applyTo(samFail, map));
		}
		assertEquals(false, rc.hasMotifs());
		assertEquals(0, rc.getStage1Coverage());
		assertEquals(0, rc.getStage2Coverage());
		assertEquals(0, rc.getTotalCoverage());		// not yet implemented
	}
	
	@Test
	public void realLifeData() {
		Map<ChrPosition, RegionCounter> map = new HashMap<>();
		RegionCounter rc = new RegionCounter(RegionType.GENOMIC);
		map.put(new ChrRangePosition("chr2", 182140000, 182149999), rc);
		
		//HWI-ST1407:62:D1JPHACXX:6:1115:17220:37446	99	chr2	182140442	60	101M	=	182140519	178	
		//GGTATTTCCCTCTATTCTTTGCCTGCGTACTTCTGGGTTAATGTAAGGTGCAAGAGACACAGTGCATAAGATGTGCACTAACCCTAACCCTAACCCTAACC	
		//<<B0BBF<<FBBBFFBFBBF7FFFBFFFFFFIIBBF70BBFBB<BF0'0BFFFFF7BFFFI<'7<FFIIBB<B0<<7707<<7'77<B<0<BBBBBB<BBB	X0:i:1	
		//X1:i:0	ZC:i:7	MD:Z:101	RG:Z:20121225042225590	XG:i:0	AM:i:37	NM:i:0	SM:i:37	XM:i:0	XO:i:0	XT:A:U
		SAMRecord sam = new SAMRecord(null);;
		sam.setFlags(99);
		sam.setAlignmentStart(182140442);
		sam.setReadString("GGTATTTCCCTCTATTCTTTGCCTGCGTACTTCTGGGTTAATGTAAGGTGCAAGAGACACAGTGCATAAGATGTGCACTAACCCTAACCCTAACCCTAACC");
		sam.setCigarString("101M");
		sam.setMappingQuality(60);
		sam.setBaseQualityString("<<B0BBF<<FBBBFFBFBBF7FFFBFFFFFFIIBBF70BBFBB<BF0'0BFFFFF7BFFFI<'7<FFIIBB<B0<<7707<<7'77<B<0<BBBBBB<BBB");
		
		assertEquals(false, mca.applyTo(sam, map));
		
	}
	
	@Test
	public void realLifeData2() {
		Map<ChrPosition, RegionCounter> map = new HashMap<>();
		RegionCounter rc = new RegionCounter(RegionType.INCLUDES);
		map.put(new ChrRangePosition("chr", 10001,12464), rc);
		
		//HWI-ST526:240:C2928ACXX:7:2107:12069:7310	163	chr1	10003	9	101M	=	10041	139	
		//ACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCT	
		//CC@FFFFFHHHHHJJJJJJJJJJJJJJCEHIJJJGIBHIGEHDHIIDIGIBFFGHIJJJIHGGHFFDFFDECEBDBBCCDBDDD?BDDBDDDDB?CCDDD2	X0:i:361	
		//ZC:i:10	MD:Z:101	PG:Z:MarkDuplicates.3	RG:Z:20140411002647551	XG:i:0	AM:i:0	NM:i:0	SM:i:0	XM:i:0	XO:i:0	XT:A:R
		SAMRecord sam = new SAMRecord(null);;
		sam.setFlags(163);
		sam.setAlignmentStart(10003);
		sam.setReadString("ACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCT");
		sam.setCigarString("101M");
		sam.setMappingQuality(9);
		sam.setBaseQualityString("CC@FFFFFHHHHHJJJJJJJJJJJJJJCEHIJJJGIBHIGEHDHIIDIGIBFFGHIJJJIHGGHFFDFFDECEBDBBCCDBDDD?BDDBDDDDB?CCDDD2");
		
		assertEquals(true, mca.applyTo(sam, map));
		// same as initial sequence apart from the first character and the last 4 characters
		assertEquals("CCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA"
				, rc.getMotifsForwardStrand().toString());
		
	}
//	@Test
//	public void realLifeData2() {
//		Map<ChrPosition, RegionCounter> map = new HashMap<>();
//		RegionCounter rc = new RegionCounter(RegionType.INCLUDES);
//		map.put(new ChrPosition("chr", 10001,12464), rc);
//		
//		//HWI-ST526:240:C2928ACXX:7:2107:12069:7310	163	chr1	10003	9	101M	=	10041	139	
//		//ACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCT	
//		//CC@FFFFFHHHHHJJJJJJJJJJJJJJCEHIJJJGIBHIGEHDHIIDIGIBFFGHIJJJIHGGHFFDFFDECEBDBBCCDBDDD?BDDBDDDDB?CCDDD2	X0:i:361	
//		//ZC:i:10	MD:Z:101	PG:Z:MarkDuplicates.3	RG:Z:20140411002647551	XG:i:0	AM:i:0	NM:i:0	SM:i:0	XM:i:0	XO:i:0	XT:A:R
//		SAMRecord sam = new SAMRecord(null);;
//		sam.setFlags(163);
//		sam.setAlignmentStart(10003);
//		sam.setReadString("ACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCT");
//		sam.setCigarString("101M");
//		sam.setMappingQuality(9);
//		sam.setBaseQualityString("CC@FFFFFHHHHHJJJJJJJJJJJJJJCEHIJJJGIBHIGEHDHIIDIGIBFFGHIJJJIHGGHFFDFFDECEBDBBCCDBDDD?BDDBDDDDB?CCDDD2");
//		
//		assertEquals(true, mca.applyTo(sam, map));
//		// same as initial sequence apart from the first character and the last 4 characters
//		assertEquals(true
//				, rc.getMotifsForwardStrand().containsKey("CCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA"));
//		
//	}

}
