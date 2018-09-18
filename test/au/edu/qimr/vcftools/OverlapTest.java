package au.edu.qimr.vcftools;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.vcf.VcfRecord;

public class OverlapTest {
	
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void areRecordsTheSame() {
		/*
		 * chr1	729700	.	C	T	.	.	FLANK=CAGTTTCAGCA;BaseQRankSum=-2.743;ClippingRankSum=0.000;DP=62;ExcessHet=3.0103;FS=5.197;MQ=48.06;MQRankSum=-0.304;QD=1.56;ReadPosRankSum=-0.151;SOR=1.580;IN=1,2;HOM=3,CTTCCCAGTTtCA	GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL	0/0:43,0:43:C4[]1[]:C4:PASS:.:.:.:C18[34.33]25[34.2]:.	0/1:50,10:60:C2[]2[]:C2;G1;T1:PASS:.:SOMATIC:10:C23[35.7]27[34.33];T7[31.86]3[29.67]:.	./.:.:.:.:.:PASS:.:NCIG:.:.:.	0/1:50,10:60:.:.:PASS:99:SOMATIC:.:.:93.77
		 */
		VcfRecord newVcf = new VcfRecord(new String[]{"chr1","729700",".","C","T",".",".","FLANK=CAGTTTCAGCA;BaseQRankSum=-2.743;ClippingRankSum=0.000;DP=62;ExcessHet=3.0103;FS=5.197;MQ=48.06;MQRankSum=-0.304;QD=1.56;ReadPosRankSum=-0.151;SOR=1.580;IN=1,2;HOM=3,CTTCCCAGTTtCA","GT:AD:DP:EOR:FF:FT:GQ:INF:NNS:OABS:QL","0/0:43,0:43:C4[]1[]:C4:PASS:.:.:.:C18[34.33]25[34.2]:.","0/1:50,10:60:C2[]2[]:C2;G1;T1:PASS:.:SOMATIC:10:C23[35.7]27[34.33];T7[31.86]3[29.67]:.","./.:.:.:.:.:PASS:.:NCIG:.:.:.","0/1:50,10:60:.:.:PASS:99:SOMATIC:.:.:93.77"});
		/*
		 * chr1    729700  .       C       T       .       PASS_1;PASS_2   SOMATIC_1;FLANK=CAGTTTCAGCA;SOMATIC_2;IN=1,2;HOM=2,CTTCCCAGTTtCAGCACTCTC;CONF=HIGH_1,HIGH_2    GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL        0/0&.:C/C&C/C:C18[34.33],25[34.2]&C18[34.33],25[34.2]:43&.:C18[34.33]25[34.2]&C18[34.33]25[34.2]:0&0:0&0:.:.:.  0/1&0/1:C/T&C/T:C23[35.7],27[34.33],T7[31.86],3[29.67]&C23[35.7],27[34.33],T7[31.86],3[29.67]:60&60:C23[35.7]27[34.33];T7[31.86]3[29.67]&C23[35.7]27[34.33];T7[31.86]3[29.67]:10&10:10&10:50,10:99:122,0,1667
		 */
		VcfRecord oldVcf = new VcfRecord(new String[]{"chr1","729700",".","C","T",".","PASS_1;PASS_2","SOMATIC_1;FLANK=CAGTTTCAGCA;SOMATIC_2;IN=1,2;HOM=2,CTTCCCAGTTtCAGCACTCTC;CONF=HIGH_1,HIGH_2","GT:GD:AC:DP:OABS:MR:NNS:AD:GQ:PL","0/0&.:C/C&C/C:C18[34.33],25[34.2]&C18[34.33],25[34.2]:43&.:C18[34.33]25[34.2]&C18[34.33]25[34.2]:0&0:0&0:.:.:.","0/1&0/1:C/T&C/T:C23[35.7],27[34.33],T7[31.86],3[29.67]&C23[35.7],27[34.33],T7[31.86],3[29.67]:60&60:C23[35.7]27[34.33];T7[31.86]3[29.67]&C23[35.7]27[34.33];T7[31.86]3[29.67]:10&10:10&10:50,10:99:122,0,1667"});
		
		Map<ChrPositionRefAlt, Pair<float[], int[]>> map = new HashMap<>();
		Overlap.processVcfRecord(newVcf, true, true, map, 0,2);
		assertEquals(1, map.size());
		Overlap.processVcfRecord(oldVcf, true, true, map, 1,2);
		assertEquals(1, map.size());
		float[] alleleDist = map.get(new ChrPositionRefAlt("chr1", 729700,729700, "C","T")).getLeft();
		assertEquals(2, alleleDist.length);
		assertEquals(true, alleleDist[0] > 0);
		assertEquals(true, alleleDist[0] < Float.MAX_VALUE);
		assertEquals(true, alleleDist[1] > 0);
		assertEquals(true, alleleDist[1] < Float.MAX_VALUE);
		
//		assertEquals(true, map.get(new ChrPositionRefAlt("chr1", 729700,729700, "C","T")).contains("input2"));
	}
}
