package org.qcmg.coverage;

import htsjdk.samtools.SAMRecord;
import org.junit.Assert;
import org.junit.Test;

public class LowReadDepthAlgorithmTest {
	
	
	@Test
	public void testApplyToSingle() {
		LowReadDepthAlgorithm algorithm = new LowReadDepthAlgorithm(12);
		int [] arrayNew = new int[1024];

		SAMRecord rec = new SAMRecord(null);
		
		rec.setAlignmentStart(1);
		rec.setReadString("AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTT");
		rec.setCigarString("40M");
		
		algorithm.applyTo(rec, arrayNew);
		
		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
			Assert.assertEquals((i < 40 ) ? 1 : 0, arrayNew[i]);
		}
		
		// reset arrays
		arrayNew = new int[1024];
		rec.setAlignmentStart(1000);
		
		algorithm.applyTo(rec, arrayNew);
		
		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
			Assert.assertEquals((i > 998 ) ? 1 : 0, arrayNew[i]);
		}

	}
	

}
