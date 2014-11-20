package org.qcmg.common.vcf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.common.model.VCFRecord;

public class VcfPositionComparatorTest {
	
	@Test
	public void testComparator() {
		VCFRecord vcf1 = VcfUtils.createVcfRecord("chr1", 12346);
		VCFRecord vcf2 = VcfUtils.createVcfRecord("chr1", 12345);
		
		List<VCFRecord> array = new ArrayList<VCFRecord>();
		array.add(vcf1);
		array.add(vcf2);
		
		Assert.assertEquals(vcf1, array.get(0));
		Assert.assertEquals(vcf2, array.get(1));
		
		Collections.sort(array, new VcfPositionComparator());
		
		Assert.assertEquals(vcf1, array.get(1));
		Assert.assertEquals(vcf2, array.get(0));
	}

}
