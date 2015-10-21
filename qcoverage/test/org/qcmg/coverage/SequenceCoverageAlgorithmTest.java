package org.qcmg.coverage;

import java.util.Arrays;

import net.sf.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class SequenceCoverageAlgorithmTest {
	
	
	@Test
	public void testApplyToSingle() {
		SequenceCoverageAlgorithm algorithm = new SequenceCoverageAlgorithm();
		int [] arrayNew = new int[1024];
		int [] arrayOld = new int[1024];
		SAMRecord rec = new SAMRecord(null);
		
		rec.setAlignmentStart(1);
		rec.setReadString("AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTT");
		rec.setCigarString("40M");
		
		algorithm.applyTo(rec, arrayNew);
		applyToOld(rec, arrayOld);
		
		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
			Assert.assertEquals((i < 40 ) ? 1 : 0, arrayNew[i]);
		}
		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
			Assert.assertEquals((i < 40 ) ? 1 : 0, arrayOld[i]);
		}
		Assert.assertArrayEquals(arrayOld, arrayNew);
		
		// reset arrays
		arrayNew = new int[1024];
		arrayOld = new int[1024];
		rec.setAlignmentStart(1000);
		
		algorithm.applyTo(rec, arrayNew);
		applyToOld(rec, arrayOld);
		
		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
			Assert.assertEquals((i > 998 ) ? 1 : 0, arrayNew[i]);
		}
		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
			Assert.assertEquals((i > 998 ) ? 1 : 0, arrayOld[i]);
		}
		Assert.assertArrayEquals(arrayOld, arrayNew);
		
		// reset arrays and set all values to -1
		// this should mean that no coverage values are stored
		Arrays.fill(arrayOld, -1);
		Arrays.fill(arrayNew, -1);
		rec.setAlignmentStart(1);
		
		algorithm.applyTo(rec, arrayNew);
		applyToOld(rec, arrayOld);
		
		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
			Assert.assertEquals( -1, arrayNew[i]);
		}
		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
			Assert.assertEquals(-1, arrayOld[i]);
		}
		Assert.assertArrayEquals(arrayOld, arrayNew);
	}
	
	
	
	@Test
	public void testApplyToOverlapBait() {
		SequenceCoverageAlgorithm algorithm = new SequenceCoverageAlgorithm();
		int [] arrayNew = new int[1024];
		int [] arrayOld = new int[1024];
		
		// setup the bait from 20-50
		Arrays.fill(arrayNew, -1);
		Arrays.fill(arrayOld, -1);
		Arrays.fill(arrayNew, 19, 49, 0);
		Arrays.fill(arrayOld, 19, 49, 0);
		
		SAMRecord rec = new SAMRecord(null);
		
		rec.setAlignmentStart(1);
		rec.setReadString("AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTT");
		rec.setCigarString("40M");
		
		algorithm.applyTo(rec, arrayNew);
		applyToOld(rec, arrayOld);
		
		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
			if (i < 19 || i >= 49) {
				Assert.assertEquals( -1, arrayNew[i]);
			} else {
				Assert.assertEquals((i < 40 ) ? 1 : 0, arrayNew[i]);
			}
		}
		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
			if (i < 19 || i >= 49) {
				Assert.assertEquals( -1, arrayOld[i]);
			} else {
				Assert.assertEquals((i < 40 ) ? 1 : 0, arrayOld[i]);
			}
		}
		Assert.assertArrayEquals(arrayOld, arrayNew);
	}
	
	@Ignore
	public void testApplyToPerformance() {
		SequenceCoverageAlgorithm algorithm = new SequenceCoverageAlgorithm();
		int [] array = new int[1024 * 1024];
		SAMRecord rec = new SAMRecord(null);
		
		rec.setAlignmentStart(1);
		rec.setReadString("AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTT");
		rec.setCigarString("40M");
		
		int noOfLoops = 1000000;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			applyToOld(rec, array);
		}
		System.out.println("OLD :" + (System.currentTimeMillis() - start));
		
		for (int i = 0 , length = array.length  ; i < length ; i++) {
			Assert.assertEquals((i < 40 ) ? noOfLoops : 0, array[i]);
		}
		
		// reset array
		array = new int[1024 * 1024];
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			algorithm.applyTo(rec, array);
		}
		System.out.println("NEW :" + (System.currentTimeMillis() - start));
		
		for (int i = 0 , length = array.length  ; i < length ; i++) {
			Assert.assertEquals((i < 40 ) ? noOfLoops : 0, array[i]);
		}
		
		// reset array
		array = new int[1024 * 1024];
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			applyToOld(rec, array);
		}
		System.out.println("OLD :" + (System.currentTimeMillis() - start));
		
		for (int i = 0 , length = array.length  ; i < length ; i++) {
			Assert.assertEquals((i < 40 ) ? noOfLoops : 0, array[i]);
		}
		
		// reset array
		array = new int[1024 * 1024];
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			algorithm.applyTo(rec, array);
		}
		System.out.println("NEW :" + (System.currentTimeMillis() - start));
		
		for (int i = 0 , length = array.length  ; i < length ; i++) {
			Assert.assertEquals((i < 40 ) ? noOfLoops : 0, array[i]);
		}
		
	}
	
	/**
	 * OLD version of the algorithm that performed a number of checks within the loops
	 * Used in tests to ensure no loss of functionality  
	 * @param read
	 * @param perBaseCoverages
	 */
	public void applyToOld(final SAMRecord read, final int[] perBaseCoverages) {
		for (int pos = read.getAlignmentStart(), end = read.getAlignmentEnd() ; pos <= end ; pos++) {
			if (pos > 0 && (pos - 1) < perBaseCoverages.length) {
				// Adjust from 1-based to 0-based indexing
				int cov = perBaseCoverages[pos - 1];
				if (-1 < cov) {
					cov++;
					perBaseCoverages[pos - 1] = cov;
				}
			}
		}
	}

}
