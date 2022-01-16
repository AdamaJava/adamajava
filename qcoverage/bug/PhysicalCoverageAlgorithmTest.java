package org.qcmg.coverage;

import java.util.Arrays;

import htsjdk.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PhysicalCoverageAlgorithmTest {
	
	
	@Test
	public void testApplyToSingle() {
		PhysicalCoverageAlgorithm algorithm = new PhysicalCoverageAlgorithm();
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
	public void testApplyToPair() {
		PhysicalCoverageAlgorithm algorithm = new PhysicalCoverageAlgorithm();
		int [] arrayNew = new int[1024];
		int [] arrayOld = new int[1024];
		SAMRecord rec1 = new SAMRecord(null);
		
		rec1.setAlignmentStart(1);
		rec1.setReadString("AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTT");
		rec1.setCigarString("40M");
		rec1.setMateAlignmentStart(101);
		rec1.setMateReferenceName("=");
		rec1.setReadPairedFlag(true);
		rec1.setProperPairFlag(true);
		rec1.setFirstOfPairFlag(true);
		rec1.setInferredInsertSize(140);
		
		algorithm.applyTo(rec1, arrayNew);
		applyToOld(rec1, arrayOld);
		
		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
			Assert.assertEquals((i <= 140 ) ? 1 : 0, arrayNew[i]);
		}
		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
			Assert.assertEquals((i <= 140 ) ? 1 : 0, arrayOld[i]);
		}
		Assert.assertArrayEquals(arrayOld, arrayNew);
		
		// reset arrays
		arrayNew = new int[1024];
		arrayOld = new int[1024];
		rec1.setAlignmentStart(901);
		rec1.setMateAlignmentStart(1001);
		
		
		algorithm.applyTo(rec1, arrayNew);
		applyToOld(rec1, arrayOld);
		
		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
			Assert.assertEquals((i >= 900 ) ? 1 : 0, arrayNew[i]);
		}
		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
			Assert.assertEquals((i >= 900 ) ? 1 : 0, arrayOld[i]);
		}
		Assert.assertArrayEquals(arrayOld, arrayNew);
		
		// reset arrays and set all values to -1
		// this should mean that no coverage values are stored
		Arrays.fill(arrayOld, -1);
		Arrays.fill(arrayNew, -1);
		rec1.setAlignmentStart(1);
		
		algorithm.applyTo(rec1, arrayNew);
		applyToOld(rec1, arrayOld);
		
		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
			Assert.assertEquals( -1, arrayNew[i]);
		}
		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
			Assert.assertEquals(-1, arrayOld[i]);
		}
		Assert.assertArrayEquals(arrayOld, arrayNew);
	}
	
	@Ignore
	public void testApplyToPair2() {
		PhysicalCoverageAlgorithm algorithm = new PhysicalCoverageAlgorithm();
		int [] arrayNew = new int[1024];
		int [] arrayOld = new int[1024];
		SAMRecord rec1 = new SAMRecord(null);
		
		rec1.setAlignmentStart(1);
		rec1.setReadString("AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTT");
		rec1.setCigarString("40M");
		rec1.setMateAlignmentStart(101);
		rec1.setMateReferenceName("=");
		rec1.setReadPairedFlag(true);
		rec1.setProperPairFlag(true);
		rec1.setFirstOfPairFlag(true);
		rec1.setInferredInsertSize(1000000);
		
		int counter = 1000;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			algorithm.applyTo(rec1, arrayNew, true);
		}
		System.out.println("new method (true): " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			applyToOld(rec1, arrayOld);
		}
		System.out.println("old method: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			algorithm.applyTo(rec1, arrayNew, false);
		}
		System.out.println("new method (false)" + (System.currentTimeMillis() - start));
		
//		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
//			Assert.assertEquals((i <= 140 ) ? 1 : 0, arrayNew[i]);
//		}
//		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
//			Assert.assertEquals((i <= 140 ) ? 1 : 0, arrayOld[i]);
//		}
//		Assert.assertArrayEquals(arrayOld, arrayNew);
//		
//		// reset arrays
//		arrayNew = new int[1024];
//		arrayOld = new int[1024];
//		rec1.setAlignmentStart(901);
//		rec1.setMateAlignmentStart(1001);
//		
//		
//		algorithm.applyTo(rec1, arrayNew);
//		applyToOld(rec1, arrayOld);
//		
//		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
//			Assert.assertEquals((i >= 900 ) ? 1 : 0, arrayNew[i]);
//		}
//		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
//			Assert.assertEquals((i >= 900 ) ? 1 : 0, arrayOld[i]);
//		}
//		Assert.assertArrayEquals(arrayOld, arrayNew);
//		
//		// reset arrays and set all values to -1
//		// this should mean that no coverage values are stored
//		Arrays.fill(arrayOld, -1);
//		Arrays.fill(arrayNew, -1);
//		rec1.setAlignmentStart(1);
//		
//		algorithm.applyTo(rec1, arrayNew);
//		applyToOld(rec1, arrayOld);
//		
//		for (int i = 0 , length = arrayNew.length  ; i < length ; i++) {
//			Assert.assertEquals( -1, arrayNew[i]);
//		}
//		for (int i = 0 , length = arrayOld.length  ; i < length ; i++) {
//			Assert.assertEquals(-1, arrayOld[i]);
//		}
//		Assert.assertArrayEquals(arrayOld, arrayNew);
	}
	
	@Test
	public void testApplyToOverlapBait() {
		PhysicalCoverageAlgorithm algorithm = new PhysicalCoverageAlgorithm();
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
		PhysicalCoverageAlgorithm algorithm = new PhysicalCoverageAlgorithm();
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
	private static void applyToOld(final SAMRecord read, final int[] perBaseCoverages) {
		if (read.getReadPairedFlag()) {
			if (0 <= read.getInferredInsertSize()) {
				final int end = read.getAlignmentStart() + read.getInferredInsertSize();
				for (int pos = read.getAlignmentStart(); pos <= end; pos++) {
					// Avoid bad isizes beyond end-of-reference
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
		} else {
			for (int pos = read.getAlignmentStart(), end = read.getAlignmentEnd() ; pos <= end ; pos++) {
				// Avoid malformed reads with ends beyond end-of-reference
				if (pos > 0 && (pos - 1) < perBaseCoverages.length) {
					// Adjust coordinate from 1-based to 0-based indexing
					int cov = perBaseCoverages[pos - 1];
					if (-1 < cov) {
						cov++;
						perBaseCoverages[pos - 1] = cov;
					}
				}
			}
		}
	}

}
