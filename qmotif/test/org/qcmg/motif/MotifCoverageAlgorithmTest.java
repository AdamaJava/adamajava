package org.qcmg.motif;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import net.sf.samtools.SAMRecord;

import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.model.ChrPosition;
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
	MotifCoverageAlgorithm mca;
	MotifCoverageAlgorithm mcaStage1RegexStage2String;
	
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
		assertNull(mca.getStageTwoMotifs(null));
		assertNull(mca.getStageTwoMotifs(""));
		assertEquals("", mca.getStageTwoMotifs("ACGTACGTACGTACGTACGTACGT"));
		assertEquals("", mca.getStageTwoMotifs("ACGTTTAGGTTAGGACGT"));
	}
	
	@Test
	public void stageTwoMotifsSameMatch() {
		assertEquals("TTAGGGTTAGGG", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGACGT"));
		assertEquals("TTAGGGTTAGGGTTAGGG", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGTTAGGGACGT"));
		assertEquals("TTAGGGTTAGGGTTAGGGTTAGGG", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGTTAGGGTTAGGGACGT"));
		assertEquals("TTAGGGTTAGGGTTAGGGTTAGGGTTAGGG", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGACGT"));
	}
	@Test
	public void stageTwoMotifsDiffMatch() {
		assertEquals("TTAGGGTTAGGG:CCCTGACCCTGA", mca.getStageTwoMotifs("ACGTTTAGGGTTAGGGCCCTGACCCTGAACGT"));
	}
	
	@Test
	public void noValidCountersInMap() {
		Map<ChrPosition, RegionCounter> map = new HashMap<>();
		assertEquals(null, mca.getCounterFromMap(map, null));
		
		map.put(new ChrPosition("1", 100, 200), new RegionCounter(RegionType.INCLUDES));
		assertEquals(null, mca.getCounterFromMap(map, new ChrPosition("1", 300, 400)));
		RegionCounter rc300To400 = new RegionCounter(RegionType.INCLUDES);
		map.put(new ChrPosition("1", 300, 400), rc300To400);
		assertEquals(rc300To400, mca.getCounterFromMap(map, new ChrPosition("1", 299, 400)));
		
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
		map.put(new ChrPosition("chr1", 1, 1000), rc);
		for (int i = 0 ; i < 100 ; i++) {
			assertEquals(true, mca.applyTo(samPass, map));
		}
		assertEquals(true, rc.hasMotifs());
		assertEquals(100, rc.getStage1Coverage());
		assertEquals(100, rc.getStage2Coverage());
		assertEquals(0, rc.getTotalCoverage());		// not yet implemented
		assertEquals(4 * 6 * 100 + 99, rc.getMotifsForwardStrand().length());
	}
	
	@Test
	public void applyToStageOneFail() {
		Map<ChrPosition, RegionCounter> map = new HashMap<>();
		RegionCounter rc = new RegionCounter(RegionType.INCLUDES);
		map.put(new ChrPosition("chr1", 1, 1000), rc);
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
		map.put(new ChrPosition("chr2", 182140000, 182149999), rc);
		
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
		
		assertEquals(false, mca.applyTo(samFail, map));
		
	}

}
