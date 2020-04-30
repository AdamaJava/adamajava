package au.edu.qimr.panel.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.vcf.VcfRecord;

import au.edu.qimr.panel.model.Contig;
import au.edu.qimr.panel.model.Fragment2;
import au.edu.qimr.panel.util.PanelUtil;
import gnu.trove.map.hash.THashMap;

public class PanelUtilTest {
	
	@Test
	public void createVcfRecordFromVariantMap() {
		Map<ChrPosition, Map<String, List<Fragment2>>> variants = new THashMap<>();
		Map<Contig, List<Fragment2>> contigFragmentMap = new THashMap<>();
		List<VcfRecord> vcfs = new ArrayList<>();
		
		ChrPosition variantCP = new ChrPositionName("chr1", 100, 101, "A");
		 Map<String, List<Fragment2>> alts = new THashMap<>();
		 Fragment2 altFragment = new Fragment2(1, "CCCCCCCCCCCCCCCCCCCC");
		 altFragment.setPosition(new ChrPointPosition("chr1", 100), true);
		 altFragment.addPosition(1);
		 alts.put("C", Arrays.asList(altFragment));
		 variants.put(variantCP, alts);
		 
		 ChrPosition contigCP = new ChrPositionName("chr1", 50, 250);
		 contigFragmentMap.put(new Contig(0, contigCP), Arrays.asList(altFragment));
		
		 /*
		  * OK..
		  */
		 
		 PanelUtil.createVcfRecordFromVariantMap(variants, contigFragmentMap, vcfs, 10);
		 assertEquals(1, vcfs.size());
		 VcfRecord v = vcfs.get(0);
		 assertEquals("GT:AD:DP:OABS\t1/1:0,1:1:C1[]0[]", v.getFormatFieldStrings());
		 vcfs.clear();
		 
		 Fragment2 refFragment = new Fragment2(2, "AAAAAAAAAAAAAAAA");
		 refFragment.setPosition(new ChrPointPosition("chr1", 100), true);
		 refFragment.addPosition(1);
		 contigFragmentMap.put(new Contig(0, contigCP), Arrays.asList(altFragment, refFragment));
		 
		 PanelUtil.createVcfRecordFromVariantMap(variants, contigFragmentMap, vcfs, 10);
		 assertEquals(1, vcfs.size());
		 v = vcfs.get(0);
		 assertEquals("GT:AD:DP:OABS\t0/1:1,1:2:A1[]0[];C1[]0[]", v.getFormatFieldStrings());
		 vcfs.clear();
		 
		 Fragment2 refFragment2 = new Fragment2(2, "AAAAAAAAAAAAAAAA");
		 refFragment2.setPosition(new ChrPointPosition("chr1", 100), true);
		 refFragment2.addPosition(1);
		 refFragment2.addPosition(1);
		 refFragment2.addPosition(1);
		 refFragment2.addPosition(1);
		 refFragment2.setForwardStrand(false);
		 contigFragmentMap.put(new Contig(0, contigCP), Arrays.asList(altFragment, refFragment, refFragment2));
		 
		 PanelUtil.createVcfRecordFromVariantMap(variants, contigFragmentMap, vcfs, 10);
		 assertEquals(1, vcfs.size());
		 v = vcfs.get(0);
		 assertEquals("GT:AD:DP:OABS\t0/1:5,1:6:A1[]4[];C1[]0[]", v.getFormatFieldStrings());
	}
	
	@Test
	public void getOABS() {
		Map<String, int[]> map = new THashMap<>();
		assertEquals("", PanelUtil.getOABS(map));
		map.put("A", new int[]{});
		assertEquals("", PanelUtil.getOABS(map));
		map.put("A", new int[]{1,0});
		assertEquals("A1[]0[]", PanelUtil.getOABS(map));
		map.put("A", new int[]{1,1});
		assertEquals("A1[]1[]", PanelUtil.getOABS(map));
		map.put("A", new int[]{5,10});
		assertEquals("A5[]10[]", PanelUtil.getOABS(map));
		map.put("B", new int[]{50,1});
		assertEquals("A5[]10[];B50[]1[]", PanelUtil.getOABS(map));
		map.put("ABC", new int[]{20,21});
		assertEquals("A5[]10[];ABC20[]21[];B50[]1[]", PanelUtil.getOABS(map));
	}
	
	@Test
	public void getGTAndAD() {
		Map<String, int[]> map = new THashMap<>();
		assertArrayEquals(new String[]{".",".","."}, PanelUtil.getGTADAlts(null, null, 10));
		assertArrayEquals(new String[]{".",".","."}, PanelUtil.getGTADAlts(map, null, 10));
		assertArrayEquals(new String[]{".",".","."}, PanelUtil.getGTADAlts(map, "", 10));
		map.put("A", new int[]{1,0});
		assertArrayEquals(new String[]{".",".","."}, PanelUtil.getGTADAlts(map, "", 10));
		assertArrayEquals(new String[]{"0/0","1","."}, PanelUtil.getGTADAlts(map, "A", 10));
		assertArrayEquals(new String[]{"1/1","0,1","A"}, PanelUtil.getGTADAlts(map, "C", 10));
		assertArrayEquals(new String[]{"1/1","0,1","A"}, PanelUtil.getGTADAlts(map, "G", 10));
		assertArrayEquals(new String[]{"1/1","0,1","A"}, PanelUtil.getGTADAlts(map, "T", 10));
		map.put("C", new int[]{1,0});
		assertArrayEquals(new String[]{"0/1","1,1","C"}, PanelUtil.getGTADAlts(map, "A", 10));
		assertArrayEquals(new String[]{"0/1","1,1","A"}, PanelUtil.getGTADAlts(map, "C", 10));
		assertArrayEquals(new String[]{"1/2","0,1,1","A,C"}, PanelUtil.getGTADAlts(map, "G", 10));
		assertArrayEquals(new String[]{"1/2","0,1,1","A,C"}, PanelUtil.getGTADAlts(map, "T", 10));
		
		
		map.put("A", new int[]{5,6});
		map.put("C", new int[]{1,0});
		assertArrayEquals(new String[]{"0/0","11","."}, PanelUtil.getGTADAlts(map, "A", 10));
		assertArrayEquals(new String[]{"1/1","1,11","A"}, PanelUtil.getGTADAlts(map, "C", 10));
		assertArrayEquals(new String[]{"1/1","0,11","A"}, PanelUtil.getGTADAlts(map, "G", 10));
		assertArrayEquals(new String[]{"1/1","0,11","A"}, PanelUtil.getGTADAlts(map, "T", 10));
		map.put("C", new int[]{1,1});
		assertArrayEquals(new String[]{"0/1","11,2","C"}, PanelUtil.getGTADAlts(map, "A", 10));
		assertArrayEquals(new String[]{"0/1","2,11","A"}, PanelUtil.getGTADAlts(map, "C", 10));
		assertArrayEquals(new String[]{"1/2","0,2,11","C,A"}, PanelUtil.getGTADAlts(map, "G", 10));
		assertArrayEquals(new String[]{"1/2","0,2,11","C,A"}, PanelUtil.getGTADAlts(map, "T", 10));
		
		
		
//		assertEquals(".:.", PanelUtil.getGTAndAD(map, null));
//		assertEquals(".:.", PanelUtil.getGTAndAD(map, ""));
//		map.put("A", new int[]{1,0});
//		assertEquals("0/0:1", PanelUtil.getGTAndAD(map, "A"));
//		map.put("C", new int[]{1,0});
//		assertEquals("0/1:1,1", PanelUtil.getGTAndAD(map, "A"));
//		map.put("C", new int[]{1,10});
//		assertEquals("1/1:1,11", PanelUtil.getGTAndAD(map, "A"));
//		assertEquals(".:.", PanelUtil.getGTAndAD(map, "C"));
//		map.put("G", new int[]{5,5});
//		assertEquals("1/2:1,11,10", PanelUtil.getGTAndAD(map, "A"));
	}

}
