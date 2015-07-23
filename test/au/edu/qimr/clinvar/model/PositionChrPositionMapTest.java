package au.edu.qimr.clinvar.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class PositionChrPositionMapTest {
	
	
	private final static List<String> standardHeader = new ArrayList<>();
	
	
	@Test
	public void positionOne() {
		
		PositionChrPositionMap map = new PositionChrPositionMap();
		map.loadMap(standardHeader);

		/*
		 * 0 results in an error
		 */
		assertEquals(1, map.getChrPositionFromLongPosition(1).getPosition());
		assertEquals("chr1", map.getChrPositionFromLongPosition(1).getChromosome());
		assertEquals(2, map.getChrPositionFromLongPosition(2).getPosition());
		assertEquals("chr1", map.getChrPositionFromLongPosition(1).getChromosome());
		assertEquals(249250621, map.getChrPositionFromLongPosition(249250621).getPosition());
		assertEquals("chr1", map.getChrPositionFromLongPosition(1).getChromosome());
		assertEquals(1, map.getChrPositionFromLongPosition(249250622).getPosition());
		assertEquals("chr2", map.getChrPositionFromLongPosition(249250622).getChromosome());
		
		System.out.println("pos: " + map.getChrPositionFromLongPosition(2792625622l).toIGVString());
		
	}
	
	@BeforeClass
	public static void before() {
		standardHeader.add("##q3TiledAligner version: 0.1pre (1988)");
		standardHeader.add("##RunBy: oliverH");
		standardHeader.add("##RunOn: 2015-07-08 10:27:35");
		standardHeader.add("##List of positions/Count cutoff: 500");
		standardHeader.add("##Tile length: 13");
		standardHeader.add("##Number of tiles: 62308054");
		standardHeader.add("##contig:contigLength:longPosition");
		standardHeader.add("##chr1:249250621:1");
		standardHeader.add("##chr2:243199373:249250622");
		standardHeader.add("##chr3:198022430:492449995");
		standardHeader.add("##chr4:191154276:690472425");
		standardHeader.add("##chr5:180915260:881626701");
		standardHeader.add("##chr6:171115067:1062541961");
		standardHeader.add("##chr7:159138663:1233657028");
		standardHeader.add("##chr8:146364022:1392795691");
		standardHeader.add("##chr9:141213431:1539159713");
		standardHeader.add("##chr10:135534747:1680373144");
		standardHeader.add("##chr11:135006516:1815907891");
		standardHeader.add("##chr12:133851895:1950914407");
		standardHeader.add("##chr13:115169878:2084766302");
		standardHeader.add("##chr14:107349540:2199936180");
		standardHeader.add("##chr15:102531392:2307285720");
		standardHeader.add("##chr16:90354753:2409817112");
		standardHeader.add("##chr17:81195210:2500171865");
		standardHeader.add("##chr18:78077248:2581367075");
		standardHeader.add("##chr19:59128983:2659444323");
		standardHeader.add("##chr20:63025520:2718573306");
		standardHeader.add("##chr21:48129895:2781598826");
		standardHeader.add("##chr22:51304566:2829728721");
		standardHeader.add("##chrX:155270560:2881033287");
		standardHeader.add("##chrY:59373566:3036303847");
		standardHeader.add("##GL000191.1:106433:3095677413");
		standardHeader.add("##GL000192.1:547496:3095783846");
		standardHeader.add("##GL000193.1:189789:3096331342");
		standardHeader.add("##GL000194.1:191469:3096521131");
		standardHeader.add("##GL000195.1:182896:3096712600");
		standardHeader.add("##GL000196.1:38914:3096895496");
		standardHeader.add("##GL000197.1:37175:3096934410");
		standardHeader.add("##GL000198.1:90085:3096971585");
		standardHeader.add("##GL000199.1:169874:3097061670");
		standardHeader.add("##GL000200.1:187035:3097231544");
		standardHeader.add("##GL000201.1:36148:3097418579");
		standardHeader.add("##GL000202.1:40103:3097454727");
		standardHeader.add("##GL000203.1:37498:3097494830");
		standardHeader.add("##GL000204.1:81310:3097532328");
		standardHeader.add("##GL000205.1:174588:3097613638");
		standardHeader.add("##GL000206.1:41001:3097788226");
		standardHeader.add("##GL000207.1:4262:3097829227");
		standardHeader.add("##GL000208.1:92689:3097833489");
		standardHeader.add("##GL000209.1:159169:3097926178");
		standardHeader.add("##GL000210.1:27682:3098085347");
		standardHeader.add("##GL000211.1:166566:3098113029");
		standardHeader.add("##GL000212.1:186858:3098279595");
		standardHeader.add("##GL000213.1:164239:3098466453");
		standardHeader.add("##GL000214.1:137718:3098630692");
		standardHeader.add("##GL000215.1:172545:3098768410");
		standardHeader.add("##GL000216.1:172294:3098940955");
		standardHeader.add("##GL000217.1:172149:3099113249");
		standardHeader.add("##GL000218.1:161147:3099285398");
		standardHeader.add("##GL000219.1:179198:3099446545");
		standardHeader.add("##GL000220.1:161802:3099625743");
		standardHeader.add("##GL000221.1:155397:3099787545");
		standardHeader.add("##GL000222.1:186861:3099942942");
		standardHeader.add("##GL000223.1:180455:3100129803");
		standardHeader.add("##GL000224.1:179693:3100310258");
		standardHeader.add("##GL000225.1:211173:3100489951");
		standardHeader.add("##GL000226.1:15008:3100701124");
		standardHeader.add("##GL000227.1:128374:3100716132");
		standardHeader.add("##GL000228.1:129120:3100844506");
		standardHeader.add("##GL000229.1:19913:3100973626");
		standardHeader.add("##GL000230.1:43691:3100993539");
		standardHeader.add("##GL000231.1:27386:3101037230");
		standardHeader.add("##GL000232.1:40652:3101064616");
		standardHeader.add("##GL000233.1:45941:3101105268");
		standardHeader.add("##GL000234.1:40531:3101151209");
		standardHeader.add("##GL000235.1:34474:3101191740");
		standardHeader.add("##GL000236.1:41934:3101226214");
		standardHeader.add("##GL000237.1:45867:3101268148");
		standardHeader.add("##GL000238.1:39939:3101314015");
		standardHeader.add("##GL000239.1:33824:3101353954");
		standardHeader.add("##GL000240.1:41933:3101387778");
		standardHeader.add("##GL000241.1:42152:3101429711");
		standardHeader.add("##GL000242.1:43523:3101471863");
		standardHeader.add("##GL000243.1:43341:3101515386");
		standardHeader.add("##GL000244.1:39929:3101558727");
		standardHeader.add("##GL000245.1:36651:3101598656");
		standardHeader.add("##GL000246.1:38154:3101635307");
		standardHeader.add("##GL000247.1:36422:3101673461");
		standardHeader.add("##GL000248.1:39786:3101709883");
		standardHeader.add("##GL000249.1:38502:3101749669");
		standardHeader.add("##chrMT:16569:3101788171");
		standardHeader.add("#Tile   list of positions OR count (C12345)");
	}
	

}
