package au.edu.qimr.vcftools;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;

public class AmalgamatorGSTest {
	
	@Test
	public void updateGTsAndACsForLAtestVcfRecord() {
		/*
		 * chr1	572085	.	C	T	.	.	FLANK=AGTTTTCTTTA;BaseQRankSum=0.680;ClippingRankSum=0.000;DP=25;ExcessHet=3.0103;FS=20.607;MQ=37.92;MQRankSum=-2.443;QD=4.71;ReadPosRankSum=-0.340;SOR=0.002;IN=1,2;HOM=3,TCAAGAGTTTcCTTTATTTTT	GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/0:38,0:38:C1[]0[]:A1;C20:PASS:.:.:.:C4[31.75]34[36.79]:.	0/1:25,6:31:C0[]1[]:C26;T2:PASS:.:SOMATIC:6:C0[0]25[39.16];T3[39.67]3[41]:.	./.:.:.:.:.:PASS:.:NCIG:.:.:.	0/1:19,6:25:.:.:PASS:99:SOMATIC:.:.:117.77
		 */
		VcfRecord r = new VcfRecord(new String[]{"chr1","572085",".","C","T",".",".","FLANK=AGTTTTCTTTA;BaseQRankSum=0.680;ClippingRankSum=0.000;DP=25;ExcessHet=3.0103;FS=20.607;MQ=37.92;MQRankSum=-2.443;QD=4.71;ReadPosRankSum=-0.340;SOR=0.002;IN=1,2;HOM=3,TCAAGAGTTTcCTTTATTTTT","GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL","0/0:38,0:38:C1[]0[]:A1;C20:PASS:.:.:.:C4[31.75]34[36.79]:.","0/1:25,6:31:C0[]1[]:C26;T2:PASS:.:SOMATIC:6:C0[0]25[39.16];T3[39.67]3[41]:.","./.:.:.:.:.:PASS:.:NCIG:.:.:.","0/1:19,6:25:.:.:PASS:99:SOMATIC:.:.:117.77"});
		Pair<String[], String[]> pair = Pair.of(new String[1], new String[1]);
		AmalgamatorGS.updateGTsAndACs(0, r, true, "C", "T", pair);
		
		assertEquals("0/1", pair.getLeft()[0]);
		assertEquals("25,6", pair.getRight()[0]);
		
	}
	
	@Test
	public void getScoreZero() {
		assertEquals(0, AmalgamatorGS.getScore(null));
		assertEquals(0, AmalgamatorGS.getScore(new String[]{}));
		assertEquals(0, AmalgamatorGS.getScore(new String[]{""}));
		assertEquals(0, AmalgamatorGS.getScore(new String[]{"./."}));
		assertEquals(0, AmalgamatorGS.getScore(new String[]{"./.","./."}));
		assertEquals(0, AmalgamatorGS.getScore(new String[]{"./.","./."}));
		assertEquals(0, AmalgamatorGS.getScore(new String[]{"./.","./.","","./."}));
	}
	
	@Test
	public void getScore() {
		assertEquals(1, AmalgamatorGS.getScore(new String[]{"0/1"}));
		assertEquals(2, AmalgamatorGS.getScore(new String[]{"0/1","0/1"}));
		assertEquals(2, AmalgamatorGS.getScore(new String[]{"0/1","0/1","1/1"}));
		assertEquals(2, AmalgamatorGS.getScore(new String[]{"0/1","0/1","1/1","1/1"}));
		assertEquals(2, AmalgamatorGS.getScore(new String[]{"0/1","0/1","1/1","1/1","1/2"}));
		assertEquals(2, AmalgamatorGS.getScore(new String[]{"0/1","0/1","1/1","1/1","1/2","1/2"}));
		assertEquals(2, AmalgamatorGS.getScore(new String[]{"0/1","0/1","1/1","1/1","1/2","1/2","2/2"}));
		assertEquals(2, AmalgamatorGS.getScore(new String[]{"0/1","0/1","1/1","1/1","1/2","1/2","2/2","2/2"}));
		assertEquals(3, AmalgamatorGS.getScore(new String[]{"0/1","0/1","1/1","1/1","1/2","1/2","2/2","2/2","2/2"}));
		assertEquals(9, AmalgamatorGS.getScore(new String[]{"1/1","1/1","1/1","1/1","1/1","1/1","1/1","1/1","1/1"}));
		assertEquals(9, AmalgamatorGS.getScore(new String[]{"1/1","1/1","1/1","1/1","1/1","1/1","1/1","1/1","1/1",""}));
		assertEquals(9, AmalgamatorGS.getScore(new String[]{"1/1","1/1","1/1","1/1","1/1","1/1","1/1","1/1","1/1","","./."}));
		assertEquals(9, AmalgamatorGS.getScore(new String[]{"1/1","1/1","1/1","1/1","1/1","1/1","1/1","1/1","1/1","","./.","./."}));
		assertEquals(6, AmalgamatorGS.getScore(new String[]{"./.","1/1","1/1","./.","1/1","./.","1/1","1/1","1/1","","./.","./."}));
		assertEquals(4, AmalgamatorGS.getScore(new String[]{"./.","1/1","./.","./.","1/1","./.","1/1","./.","1/1","","./.","./."}));
	}
	
	@Test
	public void getGTAndAC() {
		/*
		 * chr1	143144058	rs61815815	G	A	.	PASS	FLANK=ACCAGATAAAT;IN=1;DB;VLD;HOM=2,CTGGAACCAGaTAAATGTTGA;CONF=LOW	GT:GD:AC:DP:OABS:MR:NNS	0/1:A/G:A3[39.67],1[41],G35[39.26],19[40.37]:58:A3[39.67]1[41];G35[39.26]19[40.37]:4:4	0/1:A/G:A4[40],1[32],G47[39.74],44[38.95]:96:A4[40]1[32];G47[39.74]44[38.95]:5:5
		 */
		VcfRecord r = new VcfRecord(new String[]{"chr1","143144058","rs61815815","G","A",".","PASS","FLANK=ACCAGATAAAT;IN=1;DB;VLD;HOM=2,CTGGAACCAGaTAAATGTTGA;CONF=LOW","GT:GD:AC:DP:OABS:MR:NNS","0/1:A/G:A3[39.67],1[41],G35[39.26],19[40.37]:58:A3[39.67]1[41];G35[39.26]19[40.37]:4:4","0/1:A/G:A4[40],1[32],G47[39.74],44[38.95]:96:A4[40]1[32];G47[39.74]44[38.95]:5:5"});
		Pair<String[], String[]> pair = Pair.of(new String[1], new String[1]);
		AmalgamatorGS.updateGTsAndACs(0, r, false, r.getRef(), r.getAlt(),pair);
		assertEquals("54,4,0", pair.getRight()[0]);
	}
	

}
