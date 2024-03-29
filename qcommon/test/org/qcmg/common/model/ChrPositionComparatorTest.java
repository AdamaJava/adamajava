package org.qcmg.common.model;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;

public class ChrPositionComparatorTest {
	
	@Test
	public void vcfComp() {
		
		List<String> contigs = Arrays.asList("chr1","chr2", "chr3", "chr4");
		Comparator<VcfRecord> c = ChrPositionComparator.getVcfRecordComparator(contigs);
		
		VcfRecord v1 = VcfUtils.createVcfRecord("chr1", 100);
		VcfRecord v2 = VcfUtils.createVcfRecord("chr1", 101);
		VcfRecord v3 = VcfUtils.createVcfRecord("chr2", 99);
		VcfRecord v4 = VcfUtils.createVcfRecord("chr2", 100);
		
		List<VcfRecord> vcfs = Arrays.asList(v2, v4, v1, v3);
		
		vcfs.sort(c);
		assertEquals(v1, vcfs.get(0));
		assertEquals(v2, vcfs.get(1));
		assertEquals(v3, vcfs.get(2));
		assertEquals(v4, vcfs.get(3));
	}

	/**
	 * Test case when reference list of contigs is a subset of contigs in the records to be sorted
	 */
	@Test
	public void vcfComp2() {
		
		List<String> contigs = List.of("chr1");
		Comparator<VcfRecord> c = ChrPositionComparator.getVcfRecordComparator(contigs);
		
		VcfRecord v1 = VcfUtils.createVcfRecord("chr1", 100);
		VcfRecord v2 = VcfUtils.createVcfRecord("chr1", 101);
		VcfRecord v3 = VcfUtils.createVcfRecord("chr2", 400);
		VcfRecord v4 = VcfUtils.createVcfRecord("chr3", 300);
		
		List<VcfRecord> vcfs = Arrays.asList(v2, v4, v1, v3);
		
		vcfs.sort(c);
		assertEquals(v1, vcfs.get(0));
		assertEquals(v2, vcfs.get(1));
		assertEquals(v3, vcfs.get(2));
		assertEquals(v4, vcfs.get(3));
	}
	
	/**
	 * Test case when reference list of contigs is a subset of contigs in the records to be sorted
	 */
	@Test
	public void vcfSortingReferenceAgnostic() {
		VcfRecord v1 = VcfUtils.createVcfRecord("chr1", 300);
		VcfRecord v2 = VcfUtils.createVcfRecord("chr2", 300);
		VcfRecord v3 = VcfUtils.createVcfRecord("chr2_KI270729v1_random", 300);
		VcfRecord v4 = VcfUtils.createVcfRecord("chr3", 300);
		
		List<VcfRecord> vcfs = Arrays.asList(v1, v2, v3, v4);
		
		vcfs.sort(ChrPositionComparator.getVcfRecordComparatorForGRCh37());
		assertEquals(v1, vcfs.get(0));
		assertEquals(v2, vcfs.get(1));
		assertEquals(v3, vcfs.get(3));
		assertEquals(v4, vcfs.get(2));
		
		VcfRecord v5 = VcfUtils.createVcfRecord("chrUn_GL000216v2", 300);
		vcfs = Arrays.asList(v5, v1, v2, v3, v4);
		vcfs.sort(ChrPositionComparator.getVcfRecordComparatorForGRCh37());
		assertEquals(v1, vcfs.get(0));
		assertEquals(v2, vcfs.get(1));
		assertEquals(v3, vcfs.get(3));
		assertEquals(v4, vcfs.get(2));
		assertEquals(v5, vcfs.get(4));
	}
	/**
	 * Test case when reference list of contigs is a subset of contigs in the records to be sorted
	 */
	@Test
	public void vcfSortingReferenceAgnostic2() {
		VcfRecord v1 = VcfUtils.createVcfRecord("chrUn_GL000216v2", 100);
		VcfRecord v2 = VcfUtils.createVcfRecord("chr17_KI270729v1_random", 100);
		VcfRecord v3 = VcfUtils.createVcfRecord("chrUn_KI270516v1", 100);
		VcfRecord v4 = VcfUtils.createVcfRecord("chrUn_KI270438v1", 100);
		VcfRecord v5 = VcfUtils.createVcfRecord("chrUn_KI270742v1", 100);
		VcfRecord v6 = VcfUtils.createVcfRecord("chrUn_GL000216v2", 100);
		VcfRecord v7 = VcfUtils.createVcfRecord("chrY", 300);
		VcfRecord v8 = VcfUtils.createVcfRecord("chr1", 300);
		
		List<VcfRecord> vcfs = Arrays.asList(v1, v2, v3, v4, v5, v6, v7, v8);
		
		vcfs.sort(ChrPositionComparator.getVcfRecordComparatorForGRCh37());
		assertEquals(v8, vcfs.get(0));
		assertEquals(v7, vcfs.get(1));
		assertEquals(v2, vcfs.get(2));
		assertEquals(v1, vcfs.get(3));
	}
	
	@Test
	public void cpSortingReferenceAgnostic2() {
		ChrPosition cp1 = new ChrPointPosition("chrUn_GL000216v2", 100);
		ChrPosition cp2 = new ChrPointPosition("chr17_KI270729v1_random", 100);
		ChrPosition cp3 = new ChrPointPosition("chrY", 100);
		ChrPosition cp4 = new ChrPointPosition("chr1", 100);
		
		List<ChrPosition> cps = Arrays.asList(cp1, cp2, cp3, cp4);
		
		cps.sort(ChrPositionComparator.getCPComparatorForGRCh37());
		assertEquals(cp4, cps.get(0));
		assertEquals(cp3, cps.get(1));
		assertEquals(cp2, cps.get(2));
		assertEquals(cp1, cps.get(3));
	}
	
	@Test
	public void qsigComparatorTesting() {
		List<String> contigOrder = Arrays.asList("chr1", "chr2", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9", "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16", "chr17", "chr18", "chrX", "chrY", "GL000199.1", "GL000216.1", "chrMT");
		Map<String, Integer> contigOrderMap = new HashMap<>();
		for (int i = 0; i < contigOrder.size(); i++) {
			contigOrderMap.put(contigOrder.get(i), i);
		}
		Comparator<String> cpc =  ChrPositionComparator.getChrNameComparator(contigOrderMap);
		
		assertEquals(-1, cpc.compare("chr1", "chr2"));
		assertEquals(1, cpc.compare("chr2", "chr1"));
		assertEquals(0, cpc.compare("chr2", "chr2"));
		assertEquals(2, cpc.compare("chrMT", "GL000199.1"));
		assertEquals(1, cpc.compare("chrMT", "GL000216.1"));
		assertEquals(-1, cpc.compare("GL000216.1", "chrMT"));
		assertEquals(1, cpc.compare("chrGL000216.1", "chrMT"));
		assertEquals(-22, cpc.compare("chr1", "chrMT"));
	}

	@Test
	public void testShortcutComparator() {
		Comparator<String> cpc =  ChrPositionComparator.getChrNameComparatorNoChrsOneToM();

		assertEquals(-1, cpc.compare("1", "2"));
		assertEquals(1, cpc.compare("2", "1"));
		assertEquals(0, cpc.compare("2", "2"));
		assertEquals(-1, cpc.compare("M", "GL000199.1"));
		assertEquals(1, cpc.compare("GL000216.1", "M"));
		assertEquals(-24, cpc.compare("1", "M"));
		assertEquals(24, cpc.compare("M", "1"));
		assertEquals(0, cpc.compare("M", "M"));
	}
	
}
