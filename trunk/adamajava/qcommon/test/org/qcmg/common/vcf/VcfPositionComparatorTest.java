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
		VCFRecord vcf1 = new VCFRecord();
		VCFRecord vcf2 = new VCFRecord();
		
		// same chr
		vcf1.setChromosome("chr1");
		vcf2.setChromosome("chr1");
		
		// diff position
		vcf1.setPosition(12346);
		vcf2.setPosition(12345);
		
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
