package org.qcmg.common.model;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
	
	@Test
	public void specialString() {
		List<String> contigs = Arrays.asList("chr1","chr2", "chr3", "chr4");
		Comparator<String> c = ChrPositionComparator.getChrNameComparatorForSingleString("chr2");
		contigs.sort(c);
		assertEquals("chr2", contigs.get(0));
		assertEquals("chr1", contigs.get(1));
		assertEquals("chr3", contigs.get(2));
		assertEquals("chr4", contigs.get(3));
		
		contigs = Arrays.asList("chr1","chr2", "chr3", "chr4", "chr2");
		contigs.sort(c);
		assertEquals("chr2", contigs.get(0));
		assertEquals("chr2", contigs.get(1));
		assertEquals("chr1", contigs.get(2));
		assertEquals("chr3", contigs.get(3));
		assertEquals("chr4", contigs.get(4));
	}

	/**
	 * Test case when reference list of contigs is a subset of contigs in the records to be sorted
	 */
	@Test
	public void vcfComp2() {
		
		List<String> contigs = Arrays.asList("chr1");
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
}
