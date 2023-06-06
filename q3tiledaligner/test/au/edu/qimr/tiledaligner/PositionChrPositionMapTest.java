package au.edu.qimr.tiledaligner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.util.ChrPositionUtils;

import au.edu.qimr.tiledaligner.PositionChrPositionMap.LongRange;

public class PositionChrPositionMapTest {
	
	File grch38FullIndexFile = new File(this.getClass().getResource("/resources/GRCh38_full_analysis_set_plus_decoy_hla.fa.fai").getFile());
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void loadMap() {
		
		List<String> data = getMapData();
		Map<ChrPosition, LongRange> map = PositionChrPositionMap.loadGRCh37Map();
		
		ChrPosition cp = PositionChrPositionMap.getChrPositionFromLongPosition(4611686018697298984l, map); 
		assertEquals("chr2:20660458-20660458", cp.toIGVString());
		assertEquals("R", ((ChrPositionName)cp).getName());
		
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(1211992123, map);
		assertEquals("chr6:149450162-149450162", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(1211992123, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(2563614047l, map);
		assertEquals("chr17:63442182-63442182", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(2563614047l, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(2774027828l, map);
		assertEquals("chr20:55454522-55454522", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(2774027828l, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(2859915179l, map);
		assertEquals("chr22:30186458-30186458", cp.toIGVString());
		assertEquals("F",((ChrPositionName)cp).getName());
		assertEquals(2859915179l, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(1773033468l, map);
		assertEquals("chr10:92660324-92660324", cp.toIGVString());
		assertEquals("F",((ChrPositionName)cp).getName());
		assertEquals(1773033468l, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(1773029213l, map);
		assertEquals("chr10:92656069-92656069", cp.toIGVString());
		assertEquals("F",((ChrPositionName)cp).getName());
		assertEquals(1773029213l, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(1773028779l, map);
		assertEquals("chr10:92655635-92655635", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(1773028779l, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(1773028352l, map);
		assertEquals("chr10:92655208-92655208", cp.toIGVString());
		assertEquals("F", ((ChrPositionName)cp).getName());
		assertEquals(1773028352l, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		System.out.println("2341317558l: " +  PositionChrPositionMap.getChrPositionFromLongPosition(2341317558l, map).toIGVString());
		System.out.println("1681646586: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1681646586, map).toIGVString());
		System.out.println("2387069827: " +  PositionChrPositionMap.getChrPositionFromLongPosition(2387069827l, map).toIGVString());
		System.out.println("1686790720: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1686790720, map).toIGVString());
		System.out.println("1698698695: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1698698695, map).toIGVString());
		System.out.println("1787516226: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1787516226, map).toIGVString());
		System.out.println("1813682072: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1813682072, map).toIGVString());
		System.out.println("1701419713: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1701419713, map).toIGVString());
		System.out.println("1714178896: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1714178896, map).toIGVString());
		System.out.println("1744741606: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1744741606, map).toIGVString());
		System.out.println("1786667227: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1786667227, map).toIGVString());
		System.out.println("1788983461: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1788983461, map).toIGVString());
		System.out.println("1692152558: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1692152558, map).toIGVString());
		System.out.println("1711610060: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1711610060, map).toIGVString());
		System.out.println("1762861394: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1762861394, map).toIGVString());
		System.out.println("1780921819: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1780921819, map).toIGVString());
		System.out.println("1791951306: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1791951306, map).toIGVString());
		System.out.println("1723400752: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1723400752, map).toIGVString());
		System.out.println("1703071172: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1703071172, map).toIGVString());
		System.out.println("1811909929: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1811909929, map).toIGVString());
		System.out.println("1696336850: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1696336850, map).toIGVString());
		System.out.println("1701193865: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1701193865, map).toIGVString());
		System.out.println("4611686019268629681: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611686019268629681l, map).toIGVString());
		System.out.println("4611686021388081931: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611686021388081931l, map).toIGVString());
		System.out.println("4611686019841866826: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611686019841866826l, map).toIGVString());
		System.out.println("4611686019957071700: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611686019957071700l, map).toIGVString());
		System.out.println("4611686020235394854: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611686020235394854l, map).toIGVString());
		System.out.println("4611817961329122949: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611817961329122949l, map).toIGVString());
		System.out.println("4611819059994527615: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611819059994527615l, map).toIGVString());
		System.out.println("4611814661781716933: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611814661781716933l, map).toIGVString());
		System.out.println("4611819061188469983: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611819061188469983l, map).toIGVString());
		System.out.println("4611820159401548518: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611820159401548518l, map).toIGVString());
		System.out.println("4611820159960278991: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611820159960278991l, map).toIGVString());
		System.out.println("4611820160008170697: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611820160008170697l, map).toIGVString());
		System.out.println("4611820160024014928: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611820160024014928l, map).toIGVString());
		System.out.println("4611820160366126097: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611820160366126097l, map).toIGVString());
		System.out.println("4611816861840777044: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611816861840777044l, map).toIGVString());
		System.out.println("4611816862119100198: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611816862119100198l, map).toIGVString());
		System.out.println("1290259894: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1290259894l, map).toIGVString());
		System.out.println("2136488151: " +  PositionChrPositionMap.getChrPositionFromLongPosition(2136488151, map).toIGVString());
		System.out.println("2412006194: " +  PositionChrPositionMap.getChrPositionFromLongPosition(2412006194l, map).toIGVString());
		System.out.println("4611687118316934048: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611687118316934048l, map).toIGVString());
		System.out.println("80267450632041: " +  PositionChrPositionMap.getChrPositionFromLongPosition(80267450632041l, map).toIGVString());
		System.out.println("4611823460482664201: " +  PositionChrPositionMap.getChrPositionFromLongPosition(4611823460482664201l, map).toIGVString());
		System.out.println("1529683796: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1529683796, map).toIGVString());
		System.out.println("1808006950: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1808006950, map).toIGVString());
		System.out.println("2719061437: " +  PositionChrPositionMap.getChrPositionFromLongPosition(2719061437l, map).toIGVString());
		System.out.println("2792721623: " +  PositionChrPositionMap.getChrPositionFromLongPosition(2792721623l, map).toIGVString());
		System.out.println("chr15:79474963: " +  PositionChrPositionMap.getLongStartPositionFromChrPosition(new ChrPointPosition("chr15", 79474963), map));
		System.out.println("chr13:51701287: " +  PositionChrPositionMap.getLongStartPositionFromChrPosition(new ChrPointPosition("chr13", 51701287), map));
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
		Map<ChrPosition, LongRange> map = PositionChrPositionMap.loadMap(getMapData());
		
		ChrPosition cp = new ChrPointPosition("chr10", 1);
		System.out.println("chr10: " + PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		cp = new ChrPointPosition("chr15", 1);
		System.out.println("chr15: " + PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
	}
	
	@Test
	public void getCPFromPositions() {
		Map<ChrPosition, LongRange> map = PositionChrPositionMap.loadGRCh37Map();
		ChrPosition cp = PositionChrPositionMap.getChrPositionFromLongPosition(1154875906, map);
		
		assertEquals("chr6", cp.getChromosome());
		assertEquals(92333945, cp.getStartPosition());
		assertEquals(1154875906, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map ));
		 
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(368673907, map);
		assertEquals("chr2", cp.getChromosome());
		assertEquals(119423285, cp.getStartPosition());
		assertEquals(368673907, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(1670562876, map);
		assertEquals("chr9", cp.getChromosome());
		assertEquals(131403163, cp.getStartPosition());
		assertEquals(1670562876, PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map));
		
		System.out.println("1537429159: " +  PositionChrPositionMap.getChrPositionFromLongPosition(1537429159, map).toIGVString());
	}
	
	@Test
	public void testLongRangeOverlaps() {
		assertEquals(false, LongRange.doRangesOverlap(new LongRange(1, 100), new LongRange(101, 200)));
		assertEquals(true, LongRange.doRangesOverlap(new LongRange(1, 101), new LongRange(101, 200)));
		assertEquals(false, LongRange.doRangesOverlap(new LongRange(1, 101), new LongRange(102, 200)));
		assertEquals(false, LongRange.doRangesOverlap(new LongRange(201, 301), new LongRange(102, 200)));
		assertEquals(true, LongRange.doRangesOverlap(new LongRange(201, 301), new LongRange(102, 300)));
		assertEquals(true, LongRange.doRangesOverlap(new LongRange(201, 202), new LongRange(10, 300)));
		assertEquals(true, LongRange.doRangesOverlap(new LongRange(201, 2002), new LongRange(300, 303)));
	}
	@Test
	public void testLongRangeOverlapsInMap() {
		Map<ChrPosition, LongRange> map = new HashMap<>();
		map.put(ChrPositionUtils.getChrPosition("chr1", 1, 100), new LongRange(1, 100));
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		map.put(ChrPositionUtils.getChrPosition("chr1", 101, 200), new LongRange(101, 200));
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		
		map.put(ChrPositionUtils.getChrPosition("chr1", 200, 210), new LongRange(200, 210));
		assertEquals(true, PositionChrPositionMap.doesMapContainOverlaps(map));
		
		map.clear();
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		map.put(ChrPositionUtils.getChrPosition("chr1", 1, 100), new LongRange(1, 100));
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		map.put(ChrPositionUtils.getChrPosition("chr1", 101, 200), new LongRange(101, 200));
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		
		map.put(ChrPositionUtils.getChrPosition("chr1", 201, 210), new LongRange(201, 210));
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		
		map.put(ChrPositionUtils.getChrPosition("chr1", 51, 61), new LongRange(51, 61));
		assertEquals(true, PositionChrPositionMap.doesMapContainOverlaps(map));
		
		map.clear();
		map.put(ChrPositionUtils.getChrPosition("chr1", 1, 100), new LongRange(1, 100));
		map.put(ChrPositionUtils.getChrPosition("chr1", 101, 200), new LongRange(101, 200));
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		
		map.put(ChrPositionUtils.getChrPosition("chr1", 201, 210), new LongRange(201, 210));
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		
		map.put(ChrPositionUtils.getChrPosition("chr1", 51, 61), new LongRange(451, 561));
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
	}
	
	@Test
	public void getLongFromCP() {
		Map<ChrPosition, LongRange> map = PositionChrPositionMap.loadMap(getMapData());
		ChrPosition cp = new ChrPointPosition("chr20", 55454523);
		long pos = PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map);
		assertEquals(2774027829l, pos);
		
		ChrPosition cp1 = PositionChrPositionMap.getChrPositionFromLongPosition(2774027829l, map);
		assertEquals(cp.getStartPosition(), cp1.getStartPosition());
		
		cp = new ChrPointPosition("chr2", 56405541);
		pos = PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map);
		assertEquals(305656163l, pos);
		cp = new ChrPointPosition("chr12", 2627611);
		pos = PositionChrPositionMap.getLongStartPositionFromChrPosition(cp, map);
		assertEquals(1953542018l, pos);
	}
	
	@Test
	public void testNPE() throws IOException {
		Map<ChrPosition, LongRange> map = PositionChrPositionMap.loadGRCh37Map();
		assertEquals(84, map.size());
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		ChrPosition cp = PositionChrPositionMap.getChrPositionFromLongPosition(2921554763l, map);
		assertNotNull(cp);
		assertEquals("chrX", cp.getChromosome());
		assertEquals(40521476, cp.getStartPosition());
		assertEquals(40521476, cp.getEndPosition());
		
		File f = testFolder.newFile("index.file");
		getFastaIndexDataAsFile(f, getFastaIndexData37Data());
		map = PositionChrPositionMap.loadMap(f.getAbsolutePath());
		assertEquals(84, map.size());
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		cp = PositionChrPositionMap.getChrPositionFromLongPosition(2921554763l, map);
		assertNotNull(cp);
		assertEquals("chrX", cp.getChromosome());
		assertEquals(40521476, cp.getStartPosition());
		assertEquals(40521476, cp.getEndPosition());
	}
	
	@Test
	public void checkMaps() throws IOException {
		Map<ChrPosition, LongRange> staticMmap = PositionChrPositionMap.loadGRCh37Map();
		assertEquals(84, staticMmap.size());
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(staticMmap));
		
		File f = testFolder.newFile("index.file");
		getFastaIndexDataAsFile(f, getFastaIndexData37Data());
		Map<ChrPosition, LongRange> indexMap = PositionChrPositionMap.loadMap(f.getAbsolutePath());
		assertEquals(84, indexMap.size());
		
		for (Entry<ChrPosition, LongRange> entry : staticMmap.entrySet()) {
			LongRange indexLR = indexMap.get(entry.getKey());
			assertNotNull(indexLR);
			assertEquals(entry.getValue(), indexLR);
		}
	}
	
	
	@Test
	public void loadRangesFromIndex() throws IOException {
		File f = testFolder.newFile("index.file");
		getFastaIndexDataAsFile(f, getFastaIndexData37Data());
		Map<ChrPosition, LongRange> map = PositionChrPositionMap.loadMap(f.getAbsolutePath());
		assertEquals(84, map.size());
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		
		File f38 = testFolder.newFile("index.38.file");
		getFastaIndexDataAsFile(f38, getFastaIndexData38NoAltData());
		map = PositionChrPositionMap.loadMap(f38.getAbsolutePath());
		assertEquals(195, map.size());
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
		
		/*
		 * and now for the GRCh38 with alts
		 */
		map = PositionChrPositionMap.loadMap(grch38FullIndexFile.getAbsolutePath());
		assertEquals(3366, map.size());
		assertEquals(false, PositionChrPositionMap.doesMapContainOverlaps(map));
	}
	
	private void getFastaIndexDataAsFile(File f, List<String> data) throws IOException {
		try (FileWriter fw = new FileWriter(f);) {
			for (String s : data) {
				fw.write(s + "\n");
			}
		}
	}
	
	private List<String> getFastaIndexData38NoAltData() {
		List<String> data = Arrays.asList(
				"chr1\t248956422\t112\t70\t71",
				"chr2\t242193529\t252513167\t70\t71",
				"chr3\t198295559\t498166716\t70\t71",
				"chr4\t190214555\t699295181\t70\t71",
				"chr5\t181538259\t892227221\t70\t71",
				"chr6\t170805979\t1076358996\t70\t71",
				"chr7\t159345973\t1249605173\t70\t71",
				"chr8\t145138636\t1411227630\t70\t71",
				"chr9\t138394717\t1558439788\t70\t71",
				"chr10\t133797422\t1698811686\t70\t71",
				"chr11\t135086622\t1834520613\t70\t71",
				"chr12\t133275309\t1971537157\t70\t71",
				"chr13\t114364328\t2106716512\t70\t71",
				"chr14\t107043718\t2222714743\t70\t71",
				"chr15\t101991189\t2331287770\t70\t71",
				"chr16\t90338345\t2434736088\t70\t71",
				"chr17\t83257441\t2526365093\t70\t71",
				"chr18\t80373285\t2610812039\t70\t71",
				"chr19\t58617616\t2692333639\t70\t71",
				"chr20\t64444167\t2751788762\t70\t71",
				"chr21\t46709983\t2817153685\t70\t71",
				"chr22\t50818468\t2864531079\t70\t71",
				"chrX\t156040895\t2916075638\t70\t71",
				"chrY\t57227415\t3074345836\t70\t71",
				"chrM\t16569\t3132390908\t70\t71",
				"chr1_KI270706v1_random\t175055\t3132407851\t70\t71",
				"chr1_KI270707v1_random\t32032\t3132585543\t70\t71",
				"chr1_KI270708v1_random\t127682\t3132618170\t70\t71",
				"chr1_KI270709v1_random\t66860\t3132747813\t70\t71",
				"chr1_KI270710v1_random\t40176\t3132815765\t70\t71",
				"chr1_KI270711v1_random\t42210\t3132856651\t70\t71",
				"chr1_KI270712v1_random\t176043\t3132899601\t70\t71",
				"chr1_KI270713v1_random\t40745\t3133078295\t70\t71",
				"chr1_KI270714v1_random\t41717\t3133119759\t70\t71",
				"chr2_KI270715v1_random\t161471\t3133162209\t70\t71",
				"chr2_KI270716v1_random\t153799\t3133326124\t70\t71",
				"chr3_GL000221v1_random\t155397\t3133482258\t70\t71",
				"chr4_GL000008v2_random\t209709\t3133640012\t70\t71",
				"chr5_GL000208v1_random\t92689\t3133852853\t70\t71",
				"chr9_KI270717v1_random\t40062\t3133947003\t70\t71",
				"chr9_KI270718v1_random\t38054\t3133987774\t70\t71",
				"chr9_KI270719v1_random\t176845\t3134026509\t70\t71",
				"chr9_KI270720v1_random\t39050\t3134206017\t70\t71",
				"chr11_KI270721v1_random\t100316\t3134245764\t70\t71",
				"chr14_GL000009v2_random\t201709\t3134347653\t70\t71",
				"chr14_GL000225v1_random\t211173\t3134552383\t70\t71",
				"chr14_KI270722v1_random\t194050\t3134766712\t70\t71",
				"chr14_GL000194v1_random\t191469\t3134963674\t70\t71",
				"chr14_KI270723v1_random\t38115\t3135158017\t70\t71",
				"chr14_KI270724v1_random\t39555\t3135196815\t70\t71",
				"chr14_KI270725v1_random\t172810\t3135237075\t70\t71",
				"chr14_KI270726v1_random\t43739\t3135412492\t70\t71",
				"chr15_KI270727v1_random\t448248\t3135456995\t70\t71",
				"chr16_KI270728v1_random\t1872759\t3135911787\t70\t71",
				"chr17_GL000205v2_random\t185591\t3137811439\t70\t71",
				"chr17_KI270729v1_random\t280839\t3137999821\t70\t71",
				"chr17_KI270730v1_random\t112551\t3138284811\t70\t71",
				"chr22_KI270731v1_random\t150754\t3138399109\t70\t71",
				"chr22_KI270732v1_random\t41543\t3138552155\t70\t71",
				"chr22_KI270733v1_random\t179772\t3138594431\t70\t71",
				"chr22_KI270734v1_random\t165050\t3138776911\t70\t71",
				"chr22_KI270735v1_random\t42811\t3138944457\t70\t71",
				"chr22_KI270736v1_random\t181920\t3138988019\t70\t71",
				"chr22_KI270737v1_random\t103838\t3139172677\t70\t71",
				"chr22_KI270738v1_random\t99375\t3139278137\t70\t71",
				"chr22_KI270739v1_random\t73985\t3139379070\t70\t71",
				"chrY_KI270740v1_random\t37240\t3139454248\t70\t71",
				"chrUn_KI270302v1\t2274\t3139492137\t70\t71",
				"chrUn_KI270304v1\t2165\t3139494561\t70\t71",
				"chrUn_KI270303v1\t1942\t3139496874\t70\t71",
				"chrUn_KI270305v1\t1472\t3139498961\t70\t71",
				"chrUn_KI270322v1\t21476\t3139500573\t70\t71",
				"chrUn_KI270320v1\t4416\t3139522473\t70\t71",
				"chrUn_KI270310v1\t1201\t3139527070\t70\t71",
				"chrUn_KI270316v1\t1444\t3139528406\t70\t71",
				"chrUn_KI270315v1\t2276\t3139529988\t70\t71",
				"chrUn_KI270312v1\t998\t3139532413\t70\t71",
				"chrUn_KI270311v1\t12399\t3139533544\t70\t71",
				"chrUn_KI270317v1\t37690\t3139546239\t70\t71",
				"chrUn_KI270412v1\t1179\t3139584585\t70\t71",
				"chrUn_KI270411v1\t2646\t3139585898\t70\t71",
				"chrUn_KI270414v1\t2489\t3139588699\t70\t71",
				"chrUn_KI270419v1\t1029\t3139591341\t70\t71",
				"chrUn_KI270418v1\t2145\t3139592502\t70\t71",
				"chrUn_KI270420v1\t2321\t3139594795\t70\t71",
				"chrUn_KI270424v1\t2140\t3139597267\t70\t71",
				"chrUn_KI270417v1\t2043\t3139599555\t70\t71",
				"chrUn_KI270422v1\t1445\t3139601745\t70\t71",
				"chrUn_KI270423v1\t981\t3139603327\t70\t71",
				"chrUn_KI270425v1\t1884\t3139604440\t70\t71",
				"chrUn_KI270429v1\t1361\t3139606468\t70\t71",
				"chrUn_KI270442v1\t392061\t3139607968\t70\t71",
				"chrUn_KI270466v1\t1233\t3140005747\t70\t71",
				"chrUn_KI270465v1\t1774\t3140007115\t70\t71",
				"chrUn_KI270467v1\t3920\t3140009032\t70\t71",
				"chrUn_KI270435v1\t92983\t3140013126\t70\t71",
				"chrUn_KI270438v1\t112505\t3140107557\t70\t71",
				"chrUn_KI270468v1\t4055\t3140221787\t70\t71",
				"chrUn_KI270510v1\t2415\t3140226017\t70\t71",
				"chrUn_KI270509v1\t2318\t3140228584\t70\t71",
				"chrUn_KI270518v1\t2186\t3140231053\t70\t71",
				"chrUn_KI270508v1\t1951\t3140233388\t70\t71",
				"chrUn_KI270516v1\t1300\t3140235484\t70\t71",
				"chrUn_KI270512v1\t22689\t3140236921\t70\t71",
				"chrUn_KI270519v1\t138126\t3140260054\t70\t71",
				"chrUn_KI270522v1\t5674\t3140400271\t70\t71",
				"chrUn_KI270511v1\t8127\t3140406144\t70\t71",
				"chrUn_KI270515v1\t6361\t3140414505\t70\t71",
				"chrUn_KI270507v1\t5353\t3140421074\t70\t71",
				"chrUn_KI270517v1\t3253\t3140426621\t70\t71",
				"chrUn_KI270529v1\t1899\t3140430038\t70\t71",
				"chrUn_KI270528v1\t2983\t3140432082\t70\t71",
				"chrUn_KI270530v1\t2168\t3140435225\t70\t71",
				"chrUn_KI270539v1\t993\t3140437540\t70\t71",
				"chrUn_KI270538v1\t91309\t3140438666\t70\t71",
				"chrUn_KI270544v1\t1202\t3140531397\t70\t71",
				"chrUn_KI270548v1\t1599\t3140532734\t70\t71",
				"chrUn_KI270583v1\t1400\t3140534473\t70\t71",
				"chrUn_KI270587v1\t2969\t3140536010\t70\t71",
				"chrUn_KI270580v1\t1553\t3140539139\t70\t71",
				"chrUn_KI270581v1\t7046\t3140540832\t70\t71",
				"chrUn_KI270579v1\t31033\t3140548097\t70\t71",
				"chrUn_KI270589v1\t44474\t3140579692\t70\t71",
				"chrUn_KI270590v1\t4685\t3140624919\t70\t71",
				"chrUn_KI270584v1\t4513\t3140629788\t70\t71",
				"chrUn_KI270582v1\t6504\t3140634483\t70\t71",
				"chrUn_KI270588v1\t6158\t3140641197\t70\t71",
				"chrUn_KI270593v1\t3041\t3140647560\t70\t71",
				"chrUn_KI270591v1\t5796\t3140650762\t70\t71",
				"chrUn_KI270330v1\t1652\t3140656758\t70\t71",
				"chrUn_KI270329v1\t1040\t3140658551\t70\t71",
				"chrUn_KI270334v1\t1368\t3140659723\t70\t71",
				"chrUn_KI270333v1\t2699\t3140661228\t70\t71",
				"chrUn_KI270335v1\t1048\t3140664083\t70\t71",
				"chrUn_KI270338v1\t1428\t3140665263\t70\t71",
				"chrUn_KI270340v1\t1428\t3140666829\t70\t71",
				"chrUn_KI270336v1\t1026\t3140668395\t70\t71",
				"chrUn_KI270337v1\t1121\t3140669553\t70\t71",
				"chrUn_KI270363v1\t1803\t3140670808\t70\t71",
				"chrUn_KI270364v1\t2855\t3140672754\t70\t71",
				"chrUn_KI270362v1\t3530\t3140675767\t70\t71",
				"chrUn_KI270366v1\t8320\t3140679465\t70\t71",
				"chrUn_KI270378v1\t1048\t3140688021\t70\t71",
				"chrUn_KI270379v1\t1045\t3140689201\t70\t71",
				"chrUn_KI270389v1\t1298\t3140690378\t70\t71",
				"chrUn_KI270390v1\t2387\t3140691812\t70\t71",
				"chrUn_KI270387v1\t1537\t3140694351\t70\t71",
				"chrUn_KI270395v1\t1143\t3140696027\t70\t71",
				"chrUn_KI270396v1\t1880\t3140697304\t70\t71",
				"chrUn_KI270388v1\t1216\t3140699328\t70\t71",
				"chrUn_KI270394v1\t970\t3140700678\t70\t71",
				"chrUn_KI270386v1\t1788\t3140701779\t70\t71",
				"chrUn_KI270391v1\t1484\t3140703710\t70\t71",
				"chrUn_KI270383v1\t1750\t3140705333\t70\t71",
				"chrUn_KI270393v1\t1308\t3140707225\t70\t71",
				"chrUn_KI270384v1\t1658\t3140708669\t70\t71",
				"chrUn_KI270392v1\t971\t3140710467\t70\t71",
				"chrUn_KI270381v1\t1930\t3140711569\t70\t71",
				"chrUn_KI270385v1\t990\t3140713643\t70\t71",
				"chrUn_KI270382v1\t4215\t3140714765\t70\t71",
				"chrUn_KI270376v1\t1136\t3140719158\t70\t71",
				"chrUn_KI270374v1\t2656\t3140720428\t70\t71",
				"chrUn_KI270372v1\t1650\t3140723239\t70\t71",
				"chrUn_KI270373v1\t1451\t3140725030\t70\t71",
				"chrUn_KI270375v1\t2378\t3140726619\t70\t71",
				"chrUn_KI270371v1\t2805\t3140729148\t70\t71",
				"chrUn_KI270448v1\t7992\t3140732111\t70\t71",
				"chrUn_KI270521v1\t7642\t3140740335\t70\t71",
				"chrUn_GL000195v1\t182896\t3140748206\t70\t71",
				"chrUn_GL000219v1\t179198\t3140933834\t70\t71",
				"chrUn_GL000220v1\t161802\t3141115711\t70\t71",
				"chrUn_GL000224v1\t179693\t3141279944\t70\t71",
				"chrUn_KI270741v1\t157432\t3141462324\t70\t71",
				"chrUn_GL000226v1\t15008\t3141622124\t70\t71",
				"chrUn_GL000213v1\t164239\t3141637466\t70\t71",
				"chrUn_KI270743v1\t210658\t3141804171\t70\t71",
				"chrUn_KI270744v1\t168472\t3142017958\t70\t71",
				"chrUn_KI270745v1\t41891\t3142188955\t70\t71",
				"chrUn_KI270746v1\t66486\t3142231563\t70\t71",
				"chrUn_KI270747v1\t198735\t3142299118\t70\t71",
				"chrUn_KI270748v1\t93321\t3142500811\t70\t71",
				"chrUn_KI270749v1\t158759\t3142595585\t70\t71",
				"chrUn_KI270750v1\t148850\t3142756731\t70\t71",
				"chrUn_KI270751v1\t150742\t3142907827\t70\t71",
				"chrUn_KI270752v1\t27745\t3143060841\t70\t71",
				"chrUn_KI270753v1\t62944\t3143089101\t70\t71",
				"chrUn_KI270754v1\t40191\t3143153063\t70\t71",
				"chrUn_KI270755v1\t36723\t3143193947\t70\t71",
				"chrUn_KI270756v1\t79590\t3143231313\t70\t71",
				"chrUn_KI270757v1\t71251\t3143312158\t70\t71",
				"chrUn_GL000214v1\t137718\t3143384546\t70\t71",
				"chrUn_KI270742v1\t186739\t3143524351\t70\t71",
				"chrUn_GL000216v2\t176608\t3143713877\t70\t71",
				"chrUn_GL000218v1\t161147\t3143893127\t70\t71",
				"chrEBV\t171823\t3144056708\t70\t71");
		return data;
	}
	
	private List<String> getFastaIndexData37Data() {
		List<String> data = Arrays.asList(
				"chr1\t249250621\t6\t70\t71",
				"chr2\t243199373\t252811358\t70\t71",
				"chr3\t198022430\t499485015\t70\t71",
				"chr4\t191154276\t700336344\t70\t71",
				"chr5\t180915260\t894221403\t70\t71",
				"chr6\t171115067\t1077721174\t70\t71",
				"chr7\t159138663\t1251280749\t70\t71",
				"chr8\t146364022\t1412692829\t70\t71",
				"chr9\t141213431\t1561147773\t70\t71",
				"chr10\t135534747\t1704378547\t70\t71",
				"chr11\t135006516\t1841849513\t70\t71",
				"chr12\t133851895\t1978784702\t70\t71",
				"chr13\t115169878\t2114548775\t70\t71",
				"chr14\t107349540\t2231363945\t70\t71",
				"chr15\t102531392\t2340247058\t70\t71",
				"chr16\t90354753\t2444243193\t70\t71",
				"chr17\t81195210\t2535888737\t70\t71",
				"chr18\t78077248\t2618243887\t70\t71",
				"chr19\t59128983\t2697436533\t70\t71",
				"chr20\t63025520\t2757410224\t70\t71",
				"chr21\t48129895\t2821336117\t70\t71",
				"chr22\t51304566\t2870153590\t70\t71",
				"chrX\t155270560\t2922191086\t70\t71",
				"chrY\t59373566\t3079679804\t70\t71",
				"GL000191.1\t106433\t3139901577\t70\t71",
				"GL000192.1\t547496\t3140009544\t70\t71",
				"GL000193.1\t189789\t3140564875\t70\t71",
				"GL000194.1\t191469\t3140757389\t70\t71",
				"GL000195.1\t182896\t3140951607\t70\t71",
				"GL000196.1\t38914\t3141137129\t70\t71",
				"GL000197.1\t37175\t3141176612\t70\t71",
				"GL000198.1\t90085\t3141214332\t70\t71",
				"GL000199.1\t169874\t3141305717\t70\t71",
				"GL000200.1\t187035\t3141478031\t70\t71",
				"GL000201.1\t36148\t3141667751\t70\t71",
				"GL000202.1\t40103\t3141704429\t70\t71",
				"GL000203.1\t37498\t3141745118\t70\t71",
				"GL000204.1\t81310\t3141783165\t70\t71",
				"GL000205.1\t174588\t3141865650\t70\t71",
				"GL000206.1\t41001\t3142042746\t70\t71",
				"GL000207.1\t4262\t3142084346\t70\t71",
				"GL000208.1\t92689\t3142088682\t70\t71",
				"GL000209.1\t159169\t3142182709\t70\t71",
				"GL000210.1\t27682\t3142344165\t70\t71",
				"GL000211.1\t166566\t3142372256\t70\t71",
				"GL000212.1\t186858\t3142541215\t70\t71",
				"GL000213.1\t164239\t3142730756\t70\t71",
				"GL000214.1\t137718\t3142897355\t70\t71",
				"GL000215.1\t172545\t3143037054\t70\t71",
				"GL000216.1\t172294\t3143212077\t70\t71",
				"GL000217.1\t172149\t3143386846\t70\t71",
				"GL000218.1\t161147\t3143561468\t70\t71",
				"GL000219.1\t179198\t3143724931\t70\t71",
				"GL000220.1\t161802\t3143906702\t70\t71",
				"GL000221.1\t155397\t3144070829\t70\t71",
				"GL000222.1\t186861\t3144228459\t70\t71",
				"GL000223.1\t180455\t3144418003\t70\t71",
				"GL000224.1\t179693\t3144601049\t70\t71",
				"GL000225.1\t211173\t3144783323\t70\t71",
				"GL000226.1\t15008\t3144997526\t70\t71",
				"GL000227.1\t128374\t3145012762\t70\t71",
				"GL000228.1\t129120\t3145142983\t70\t71",
				"GL000229.1\t19913\t3145273961\t70\t71",
				"GL000230.1\t43691\t3145294172\t70\t71",
				"GL000231.1\t27386\t3145338501\t70\t71",
				"GL000232.1\t40652\t3145366292\t70\t71",
				"GL000233.1\t45941\t3145407538\t70\t71",
				"GL000234.1\t40531\t3145454149\t70\t71",
				"GL000235.1\t34474\t3145495273\t70\t71",
				"GL000236.1\t41934\t3145530253\t70\t71",
				"GL000237.1\t45867\t3145572800\t70\t71",
				"GL000238.1\t39939\t3145619336\t70\t71",
				"GL000239.1\t33824\t3145659859\t70\t71",
				"GL000240.1\t41933\t3145694180\t70\t71",
				"GL000241.1\t42152\t3145736726\t70\t71",
				"GL000242.1\t43523\t3145779494\t70\t71",
				"GL000243.1\t43341\t3145823652\t70\t71",
				"GL000244.1\t39929\t3145867626\t70\t71",
				"GL000245.1\t36651\t3145908139\t70\t71",
				"GL000246.1\t38154\t3145945327\t70\t71",
				"GL000247.1\t36422\t3145984040\t70\t71",
				"GL000248.1\t39786\t3146020996\t70\t71",
				"GL000249.1\t38502\t3146061364\t70\t71",
				"chrMT\t16569\t3146100425\t70\t71");
		return data;
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
