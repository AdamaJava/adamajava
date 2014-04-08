package org.qcmg.snp;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.pileup.QSnpRecord;

public class TorrentPipelineTest {

	
	@Test
	public void testCheckForMutationInNormal() throws SnpException, IOException, Exception {
		String chr = "chr1";
		int position = 240519264;
			
		ChrPosition cp = new ChrPosition(chr, position);
		//qcmg_ssm_20120502_torrent_v1    XXXXX   APGI_2340_SNP_20        1       1       240519264       240519264       1       -888    -888    A       A/A     A/T     A>T     -999    -999    34      2       2       -888    -999    -999    --
		//A:19[13.54],27[25.87],T:0[0],2[25.02],C:0[0],1[13],G:0[0],1[28] A:13[12.94],15[20.01],C:0[0],2[30.07],G:0[0],2[27.54]
		QSnpRecord snp = new QSnpRecord();
		snp.setChromosome(chr);
		snp.setPosition(position);
		snp.setRef('A');
		snp.setMutation("A>T");
		snp.setNormalNucleotides("A:19[13.54],27[25.87],T:0[0],1[25.02],C:0[0],1[13],G:0[0],1[28]");
		snp.setTumourNucleotides("A:13[12.94],15[20.01],C:0[0],2[30.07],G:0[0],2[27.54]");
		snp.setNormalGenotype(GenotypeEnum.AA);
		snp.setTumourGenotype(GenotypeEnum.AT);
		snp.setNormalPileup("ACGT");
		snp.setNormalCount(49);
		
		TestPipeline tp = new TestPipeline();
		tp.positionRecordMap.put(cp, snp);
		tp.classifyPileup();
		Assert.assertEquals(true, snp.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		
		tp.checkForMutationInNormal();
		Assert.assertEquals(false, snp.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		
		
		// AND one where the annotation will reamain as it is > 3%
		/*
		 * qcmg_ssm_20120502_torrent_v1    XXXXX   APGI_2340_SNP_2124      1       3       172117182       172117182       1       -888    -888    C       C/C     A/C     C>A     -999    -999    88      2       2       -888    -999    -999    --
      C:93[25.42],112[15.85],A:13[19.2],0[0],G:1[15],0[0]     C:34[25.33],49[14.69],A:18[18.13],1[11],G:0[0],2[11],T:0[0],2[10.51]
		 */
			
		snp = new QSnpRecord();
		snp.setChromosome("chr3");
		snp.setPosition(172117182);
		snp.setRef('C');
		snp.setMutation("C>A");
		snp.setNormalNucleotides("C:93[25.42],112[15.85],A:13[19.2],0[0],G:1[15],0[0]");
		snp.setTumourNucleotides("C:34[25.33],49[14.69],A:18[18.13],1[11],G:0[0],2[11],T:0[0],2[10.51]");
		snp.setNormalGenotype(GenotypeEnum.CC);
		snp.setTumourGenotype(GenotypeEnum.AC);
		snp.setNormalPileup("ACG");
		snp.setNormalCount(219);
		
		tp.positionRecordMap.put(cp, snp);		//overwrite the old snp
		tp.classifyPileup();
		Assert.assertEquals(true, snp.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		
		tp.checkForMutationInNormal();
		Assert.assertEquals(true, snp.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		
		// and finally another keeper
		/*
		 * qcmg_ssm_20120502_torrent_v1    XXXXX   APGI_2340_SNP_33        1       2       1642738 1642738 1       -888    -888    C       C/C     C/T     C>T     -999    -999    292     2       2       -888    -999    -999    MIN     C:290[14.34],280[25.23],T:12[14.34],11[19.97],G:7[14.88],1[9],A:2[10.51],0[0]   C:168[15.03],130[24.14],T:10[12.48],12[15.2],G:1[12],0[0]
		 */
		snp = new QSnpRecord();
		snp.setChromosome("chr2");
		snp.setPosition(1642738);
		snp.setRef('C');
		snp.setMutation("C>T");
		snp.setNormalNucleotides("C:290[14.34],280[25.23],T:12[14.34],11[19.97],G:7[14.88],1[9],A:2[10.51],0[0]");
		snp.setTumourNucleotides("C:168[15.03],130[24.14],T:10[12.48],12[15.2],G:1[12],0[0]");
		snp.setNormalGenotype(GenotypeEnum.CC);
		snp.setTumourGenotype(GenotypeEnum.CT);
		snp.setNormalPileup("ACGT");
		snp.setNormalCount(603);
		
		tp.positionRecordMap.put(cp, snp);		//overwrite the old snp
		tp.classifyPileup();
		Assert.assertEquals(true, snp.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
		
		tp.checkForMutationInNormal();
		Assert.assertEquals(true, snp.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL));
	}
}
