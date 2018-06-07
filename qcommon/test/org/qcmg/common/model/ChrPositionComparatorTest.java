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
}
