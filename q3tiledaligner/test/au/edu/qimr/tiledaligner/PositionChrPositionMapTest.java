package au.edu.qimr.tiledaligner;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

//import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.testng.annotations.Test;

public class PositionChrPositionMapTest {
	
	@Test
	public void loadMap() {
		List<String> data = getMapData();
		PositionChrPositionMap map = new PositionChrPositionMap();
		map.loadMap(data);
		
		ChrPosition cp = map.getChrPositionFromLongPosition(4611686018697298984l);
		assertEquals("chr2:20660458-20660458", cp.toIGVString());
//		assertEquals("chr2:20660459-20660459", cp.toIGVString());
		assertEquals("R", ((ChrPositionName)cp).getName());
		
		cp = map.getChrPositionFromLongPosition(1211992123);
		assertEquals("chr6:149450162-149450162", cp.toIGVString());
//		assertEquals("chr6:149450163-149450163", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(1211992123, map.getLongStartPositionFromChrPosition(cp));
		
		cp = map.getChrPositionFromLongPosition(2563614047l);
		assertEquals("chr17:63442182-63442182", cp.toIGVString());
//		assertEquals("chr17:63442183-63442183", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(2563614047l, map.getLongStartPositionFromChrPosition(cp));
		
		cp = map.getChrPositionFromLongPosition(2774027828l);
		assertEquals("chr20:55454522-55454522", cp.toIGVString());
//		assertEquals("chr20:55454523-55454523", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(2774027828l, map.getLongStartPositionFromChrPosition(cp));
		
		cp = map.getChrPositionFromLongPosition(2859915179l);
		assertEquals("chr22:30186458-30186458", cp.toIGVString());
		assertEquals("F",((ChrPositionName)cp).getName());
		assertEquals(2859915179l, map.getLongStartPositionFromChrPosition(cp));
		
		cp = map.getChrPositionFromLongPosition(1773033468l);
		assertEquals("chr10:92660324-92660324", cp.toIGVString());
		assertEquals("F",((ChrPositionName)cp).getName());
		assertEquals(1773033468l, map.getLongStartPositionFromChrPosition(cp));
		
		cp = map.getChrPositionFromLongPosition(1773029213l);
		assertEquals("chr10:92656069-92656069", cp.toIGVString());
		assertEquals("F",((ChrPositionName)cp).getName());
		assertEquals(1773029213l, map.getLongStartPositionFromChrPosition(cp));
		
		cp = map.getChrPositionFromLongPosition(1773028779l);
		assertEquals("chr10:92655635-92655635", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(1773028779l, map.getLongStartPositionFromChrPosition(cp));
		
		cp = map.getChrPositionFromLongPosition(1773028352l);
		assertEquals("chr10:92655208-92655208", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(1773028352l, map.getLongStartPositionFromChrPosition(cp));
		
		System.out.println("2341317558l: " +  map.getChrPositionFromLongPosition(2341317558l).toIGVString());
		System.out.println("1681646586: " +  map.getChrPositionFromLongPosition(1681646586).toIGVString());
		System.out.println("2387069827: " +  map.getChrPositionFromLongPosition(2387069827l).toIGVString());
		System.out.println("1686790720: " +  map.getChrPositionFromLongPosition(1686790720).toIGVString());
		System.out.println("1698698695: " +  map.getChrPositionFromLongPosition(1698698695).toIGVString());
		System.out.println("1787516226: " +  map.getChrPositionFromLongPosition(1787516226).toIGVString());
		System.out.println("1813682072: " +  map.getChrPositionFromLongPosition(1813682072).toIGVString());
		System.out.println("1701419713: " +  map.getChrPositionFromLongPosition(1701419713).toIGVString());
		System.out.println("1714178896: " +  map.getChrPositionFromLongPosition(1714178896).toIGVString());
		System.out.println("1744741606: " +  map.getChrPositionFromLongPosition(1744741606).toIGVString());
		System.out.println("1786667227: " +  map.getChrPositionFromLongPosition(1786667227).toIGVString());
		System.out.println("1788983461: " +  map.getChrPositionFromLongPosition(1788983461).toIGVString());
		System.out.println("1692152558: " +  map.getChrPositionFromLongPosition(1692152558).toIGVString());
		System.out.println("1711610060: " +  map.getChrPositionFromLongPosition(1711610060).toIGVString());
		System.out.println("1762861394: " +  map.getChrPositionFromLongPosition(1762861394).toIGVString());
		System.out.println("1780921819: " +  map.getChrPositionFromLongPosition(1780921819).toIGVString());
		System.out.println("1791951306: " +  map.getChrPositionFromLongPosition(1791951306).toIGVString());
		System.out.println("1723400752: " +  map.getChrPositionFromLongPosition(1723400752).toIGVString());
		System.out.println("1703071172: " +  map.getChrPositionFromLongPosition(1703071172).toIGVString());
		System.out.println("1811909929: " +  map.getChrPositionFromLongPosition(1811909929).toIGVString());
		System.out.println("1696336850: " +  map.getChrPositionFromLongPosition(1696336850).toIGVString());
		System.out.println("1701193865: " +  map.getChrPositionFromLongPosition(1701193865).toIGVString());
		System.out.println("4611686019268629681: " +  map.getChrPositionFromLongPosition(4611686019268629681l).toIGVString());
		System.out.println("4611686021388081931: " +  map.getChrPositionFromLongPosition(4611686021388081931l).toIGVString());
		System.out.println("4611686019841866826: " +  map.getChrPositionFromLongPosition(4611686019841866826l).toIGVString());
		System.out.println("4611686019957071700: " +  map.getChrPositionFromLongPosition(4611686019957071700l).toIGVString());
		System.out.println("4611686020235394854: " +  map.getChrPositionFromLongPosition(4611686020235394854l).toIGVString());
		System.out.println("4611817961329122949: " +  map.getChrPositionFromLongPosition(4611817961329122949l).toIGVString());
		System.out.println("4611819059994527615: " +  map.getChrPositionFromLongPosition(4611819059994527615l).toIGVString());
		System.out.println("4611814661781716933: " +  map.getChrPositionFromLongPosition(4611814661781716933l).toIGVString());
		System.out.println("4611819061188469983: " +  map.getChrPositionFromLongPosition(4611819061188469983l).toIGVString());
		System.out.println("4611820159401548518: " +  map.getChrPositionFromLongPosition(4611820159401548518l).toIGVString());
		System.out.println("4611820159960278991: " +  map.getChrPositionFromLongPosition(4611820159960278991l).toIGVString());
		System.out.println("4611820160008170697: " +  map.getChrPositionFromLongPosition(4611820160008170697l).toIGVString());
		System.out.println("4611820160024014928: " +  map.getChrPositionFromLongPosition(4611820160024014928l).toIGVString());
		System.out.println("4611820160366126097: " +  map.getChrPositionFromLongPosition(4611820160366126097l).toIGVString());
		System.out.println("4611816861840777044: " +  map.getChrPositionFromLongPosition(4611816861840777044l).toIGVString());
		System.out.println("4611816862119100198: " +  map.getChrPositionFromLongPosition(4611816862119100198l).toIGVString());
		System.out.println("1290259894: " +  map.getChrPositionFromLongPosition(1290259894l).toIGVString());
		System.out.println("2136488151: " +  map.getChrPositionFromLongPosition(2136488151).toIGVString());
		System.out.println("2412006194: " +  map.getChrPositionFromLongPosition(2412006194l).toIGVString());
		System.out.println("4611687118316934048: " +  map.getChrPositionFromLongPosition(4611687118316934048l).toIGVString());
		System.out.println("80267450632041: " +  map.getChrPositionFromLongPosition(80267450632041l).toIGVString());
		System.out.println("4611823460482664201: " +  map.getChrPositionFromLongPosition(4611823460482664201l).toIGVString());
		System.out.println("1529683796: " +  map.getChrPositionFromLongPosition(1529683796).toIGVString());
		System.out.println("1808006950: " +  map.getChrPositionFromLongPosition(1808006950).toIGVString());
		System.out.println("2719061437: " +  map.getChrPositionFromLongPosition(2719061437l).toIGVString());
		System.out.println("2792721623: " +  map.getChrPositionFromLongPosition(2792721623l).toIGVString());
		System.out.println("chr15:79474963: " +  map.getLongStartPositionFromChrPosition(new ChrPointPosition("chr15", 79474963)));
//		Found start position within range: 2341317558 that has tile count: 107
//		Found start position within range: 1681646586 that has tile count: 43
//		Found start position within range: 2387069827 that has tile count: 42
//		Found start position within range: 1686790720 that has tile count: 41
//		Found start position within range: 1698698695 that has tile count: 41
//		Found start position within range: 1787516226 that has tile count: 41
//		Found start position within range: 1813682072 that has tile count: 41
//		Found start position within range: 2360719339 that has tile count: 41
//		Found start position within range: 2381092478 that has tile count: 41
//		1701419713 that has tile count: 40
//		Found start position within range: 1714178896 that has tile count: 40
//		Found start position within range: 1744741606 that has tile count: 40
//		Found start position within range: 1786667227 that has tile count: 40
//		1788983461 that has tile count: 39
//		Found start position within range: 1692152558 that has tile count: 38
//		Found start position within range: 1711610060 that has tile count: 38
//		Found start position within range: 1762861394 that has tile count: 38
//		Found start position within range: 1780921819 that has tile count: 38
//		Found start position within range: 1791951306 that has tile count: 38
//		 1723400752 that has tile count: 36
//		 Found start position within range: 1703071172
//		1811909929 that has tile count: 34
//		Found start position within range: 1696336850 that has tile count: 34
//		Found start position within range: 1701193865
	}
	@Test
	public void getLongForPosition() {
		List<String> data = getMapData();
		PositionChrPositionMap map = new PositionChrPositionMap();
		map.loadMap(data);
		ChrPosition cp = new ChrPointPosition("chr10", 1);
		System.out.println("chr10: " + map.getLongStartPositionFromChrPosition(cp));
		cp = new ChrPointPosition("chr15", 1);
		System.out.println("chr15: " + map.getLongStartPositionFromChrPosition(cp));
	}
	
	@Test
	public void getCPFromPositions() {
		PositionChrPositionMap headerMap = new PositionChrPositionMap();
		headerMap.loadMap(PositionChrPositionMap.grch37Positions);
		ChrPosition cp = headerMap.getChrPositionFromLongPosition(1154875906);
		assertEquals("chr6", cp.getChromosome());
		assertEquals(92333945, cp.getStartPosition());
//		assertEquals(92333946, cp.getStartPosition());
		assertEquals(1154875906, headerMap.getLongStartPositionFromChrPosition(cp));
		
		cp = headerMap.getChrPositionFromLongPosition(368673907);
		assertEquals("chr2", cp.getChromosome());
		assertEquals(119423285, cp.getStartPosition());
//		assertEquals(119423286, cp.getStartPosition());
		assertEquals(368673907, headerMap.getLongStartPositionFromChrPosition(cp));
		
		cp = headerMap.getChrPositionFromLongPosition(1670562876);
		assertEquals("chr9", cp.getChromosome());
		assertEquals(131403163, cp.getStartPosition());
//		assertEquals(131403164, cp.getStartPosition());
		assertEquals(1670562876, headerMap.getLongStartPositionFromChrPosition(cp));
		
		System.out.println("1537429159: " +  headerMap.getChrPositionFromLongPosition(1537429159).toIGVString());
	}
	
	@Test
	public void getLongFromCP() {
		List<String> data = getMapData();
		PositionChrPositionMap map = new PositionChrPositionMap();
		map.loadMap(data);
		ChrPosition cp = new ChrPointPosition("chr20", 55454523);
		long pos = map.getLongStartPositionFromChrPosition(cp);
		assertEquals(2774027829l, pos);
//		assertEquals(2774027828l, pos);
		
		ChrPosition cp1 = map.getChrPositionFromLongPosition(2774027829l);
		assertEquals(cp.getStartPosition(), cp1.getStartPosition());
		
		cp = new ChrPointPosition("chr2", 56405541);
		pos = map.getLongStartPositionFromChrPosition(cp);
		assertEquals(305656163l, pos);
//		assertEquals(305656162l, pos);
		cp = new ChrPointPosition("chr12", 2627611);
		pos = map.getLongStartPositionFromChrPosition(cp);
		assertEquals(1953542018l, pos);
//		assertEquals(1953542017l, pos);
	}
	
	private List<String> getMapData() {
		return Arrays.asList("##q3TiledAligner version: 0.1pre (3428)",
				"##RunBy: oliverH",
				"##RunOn: 2015-11-24 20:57:59",
				"##List of positions/Count cutoff: 500",
				"##Tile length: 13",
				"##Number of tiles: 62308054",
				"##contig:contigLength:longPosition",
				"##chr1:249250621:1",
				"##chr2:243199373:249250622",
				"##chr3:198022430:492449995",
				"##chr4:191154276:690472425",
				"##chr5:180915260:881626701",
				"##chr6:171115067:1062541961",
				"##chr7:159138663:1233657028",
				"##chr8:146364022:1392795691",
				"##chr9:141213431:1539159713",
				"##chr10:135534747:1680373144",
				"##chr11:135006516:1815907891",
				"##chr12:133851895:1950914407",
				"##chr13:115169878:2084766302",
				"##chr14:107349540:2199936180",
				"##chr15:102531392:2307285720",
				"##chr16:90354753:2409817112",
				"##chr17:81195210:2500171865",
				"##chr18:78077248:2581367075",
				"##chr19:59128983:2659444323",
				"##chr20:63025520:2718573306",
				"##chr21:48129895:2781598826",
				"##chr22:51304566:2829728721",
				"##chrX:155270560:2881033287",
				"##chrY:59373566:3036303847",
				"##GL000191.1:106433:3095677413",
				"##GL000192.1:547496:3095783846",
				"##GL000193.1:189789:3096331342",
				"##GL000194.1:191469:3096521131",
				"##GL000195.1:182896:3096712600",
				"##GL000196.1:38914:3096895496",
				"##GL000197.1:37175:3096934410",
				"##GL000198.1:90085:3096971585",
				"##GL000199.1:169874:3097061670",
				"##GL000200.1:187035:3097231544",
				"##GL000201.1:36148:3097418579",
				"##GL000202.1:40103:3097454727",
				"##GL000203.1:37498:3097494830",
				"##GL000204.1:81310:3097532328",
				"##GL000205.1:174588:3097613638",
				"##GL000206.1:41001:3097788226",
				"##GL000207.1:4262:3097829227",
				"##GL000208.1:92689:3097833489",
				"##GL000209.1:159169:3097926178",
				"##GL000210.1:27682:3098085347",
				"##GL000211.1:166566:3098113029",
				"##GL000212.1:186858:3098279595",
				"##GL000213.1:164239:3098466453",
				"##GL000214.1:137718:3098630692",
				"##GL000215.1:172545:3098768410",
				"##GL000216.1:172294:3098940955",
				"##GL000217.1:172149:3099113249",
				"##GL000218.1:161147:3099285398",
				"##GL000219.1:179198:3099446545",
				"##GL000220.1:161802:3099625743",
				"##GL000221.1:155397:3099787545",
				"##GL000222.1:186861:3099942942",
				"##GL000223.1:180455:3100129803",
				"##GL000224.1:179693:3100310258",
				"##GL000225.1:211173:3100489951",
				"##GL000226.1:15008:3100701124",
				"##GL000227.1:128374:3100716132",
				"##GL000228.1:129120:3100844506",
				"##GL000229.1:19913:3100973626",
				"##GL000230.1:43691:3100993539",
				"##GL000231.1:27386:3101037230",
				"##GL000232.1:40652:3101064616",
				"##GL000233.1:45941:3101105268",
				"##GL000234.1:40531:3101151209",
				"##GL000235.1:34474:3101191740",
				"##GL000236.1:41934:3101226214",
				"##GL000237.1:45867:3101268148",
				"##GL000238.1:39939:3101314015",
				"##GL000239.1:33824:3101353954",
				"##GL000240.1:41933:3101387778",
				"##GL000241.1:42152:3101429711",
				"##GL000242.1:43523:3101471863",
				"##GL000243.1:43341:3101515386",
				"##GL000244.1:39929:3101558727",
				"##GL000245.1:36651:3101598656",
				"##GL000246.1:38154:3101635307",
				"##GL000247.1:36422:3101673461",
				"##GL000248.1:39786:3101709883",
				"##GL000249.1:38502:3101749669",
				"##chrMT:16569:3101788171",
				"#Tile   list of positions OR count (C12345)",
				"AAAAAAAAAAAAA   C1724809",
				"AAAAAAAAAAAAC   C51170",
				"AAAAAAAAAAAAG   C196190",
				"AAAAAAAAAAAAT   C89008");
	}

}
