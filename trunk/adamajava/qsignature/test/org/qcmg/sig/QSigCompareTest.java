package org.qcmg.sig;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;

public class QSigCompareTest {
	
	private final static AtomicInteger ai = new AtomicInteger();
	
	@Test
	public void testCompareRatios() {
//		String s1 = "file1";
//		String s2 = "file2";
		Map<ChrPosition, int[]> file1Ratios = new HashMap<ChrPosition, int[]>();
		Map<ChrPosition, int[]> file2Ratios = new HashMap<ChrPosition, int[]>();
		
		Assert.assertEquals(0.0f, QSigCompare.compareRatios(file1Ratios, file2Ratios, 10, ai));
//		Assert.assertEquals(0.0f, QSigCompare.compareRatios(s1, s2, file1Ratios, file2Ratios, 10));
		
		ChrPosition cp = new ChrPosition("chr1", 100);
		
		file1Ratios.put(cp, new int[] {1,10});
		file2Ratios.put(cp, new int[] {1,10});
		Assert.assertEquals(0.0f, QSigCompare.compareRatios(file1Ratios, file2Ratios, 10, ai));
//		Assert.assertEquals(0.0f, QSigCompare.compareRatios(s1, s2, file1Ratios, file2Ratios, 10));
		
		file1Ratios.put(cp, new int[] {1,100});
		file2Ratios.put(cp, new int[] {11,100});
		Assert.assertEquals(0.1f, QSigCompare.compareRatios(file1Ratios, file2Ratios, 10, ai));
//		Assert.assertEquals(0.1f, QSigCompare.compareRatios(s1, s2, file1Ratios, file2Ratios, 10));
		
		file1Ratios.put(cp, new int[] {21,100});
		file2Ratios.put(cp, new int[] {1,100});
		Assert.assertEquals(0.2f, QSigCompare.compareRatios(file1Ratios, file2Ratios, 10, ai), 0.0000001);
//		Assert.assertEquals(0.2f, QSigCompare.compareRatios(s1, s2, file1Ratios, file2Ratios, 10), 0.0000001);
	}
	
	@Test
	public void testCompareRatiosManyValues() {
//		String s1 = "file1";
//		String s2 = "file2";
		Map<ChrPosition, int[]> file1Ratios = new HashMap<ChrPosition, int[]>();
		Map<ChrPosition, int[]> file2Ratios = new HashMap<ChrPosition, int[]>();
		
		int counter = 100000;
		for (int i = 0 ; i < counter ; i++) {
			ChrPosition cp = new ChrPosition("chr1", i);
			file1Ratios.put(cp, new int[] {1,100});
			file2Ratios.put(cp, new int[] {1,100});
		}
		Assert.assertEquals(0.0f, QSigCompare.compareRatios(file1Ratios, file2Ratios, 10, ai));
//		Assert.assertEquals(0.0f, QSigCompare.compareRatios(s1, s2, file1Ratios, file2Ratios, 10));
		
		//reset
		file1Ratios.clear();
		file2Ratios.clear();
		
		for (int i = 0 ; i < counter ; i++) {
			ChrPosition cp = new ChrPosition("chr1", i);
			file1Ratios.put(cp, new int[] {1,100});
			file2Ratios.put(cp, new int[] {1,50});
		}
		
		float expectedAnswer = 0.0f;
		for (int i = 0 ; i < counter ; i++) expectedAnswer += Math.abs(-0.01f);
		Float expectedAnswerF = Float.valueOf(expectedAnswer);
		Assert.assertTrue(expectedAnswerF.equals(Float.valueOf(QSigCompare.compareRatios(file1Ratios, file2Ratios, 10, ai))));
		Assert.assertEquals(expectedAnswerF, Float.valueOf(QSigCompare.compareRatios(file1Ratios, file2Ratios, 10, ai)));
	}
	
	@Test
	public void testCompareRatiosManyValuesWithMismatches() {
//		String s1 = "file1";
//		String s2 = "file2";
		Map<ChrPosition, int[]> file1Ratios = new HashMap<ChrPosition, int[]>();
		Map<ChrPosition, int[]> file2Ratios = new HashMap<ChrPosition, int[]>();
		
		int counter = 100000;
		for (int i = 0 ; i < counter ; i++) {
			ChrPosition cp = new ChrPosition("chr1", i);
			file1Ratios.put(cp, new int[] {1,10});
			file2Ratios.put(cp, (i % 2 == 0) ? new int[] {0,0} : new int[] {2,10});
		}
		float expectedAnswer = 0.0f;
		for (int i = 0 ; i < counter/2 ; i++) expectedAnswer += Math.abs(0.1f);
		Float expectedAnswerF = Float.valueOf(expectedAnswer);
		Assert.assertEquals(expectedAnswerF, QSigCompare.compareRatios(file1Ratios, file2Ratios, 10, ai));
		Assert.assertEquals(0.0f, QSigCompare.compareRatios(file1Ratios, file2Ratios, 11, ai));
//		Assert.assertEquals(expectedAnswerF, QSigCompare.compareRatios(s1, s2, file1Ratios, file2Ratios, 10));
//		Assert.assertEquals(0.0f, QSigCompare.compareRatios(s1, s2, file1Ratios, file2Ratios, 11));
		
		//reset
		file1Ratios.clear();
		file2Ratios.clear();
		
		for (int i = 0 ; i < counter ; i++) {
			ChrPosition cp = new ChrPosition("chr1", i);
			file1Ratios.put(cp, (i % 2 == 0) ? new int[] {9,9} : new int[] {3,300});
			file2Ratios.put(cp, new int[] {2,100});
		}
		expectedAnswer = 0.0f;
		for (int i = 0 ; i < counter ; i++) {
			expectedAnswer += Math.abs((i % 2 == 0) ? 0.00f : 0.01f);
		}
		expectedAnswerF = Float.valueOf(expectedAnswer);
		Assert.assertEquals(expectedAnswerF, QSigCompare.compareRatios(file1Ratios, file2Ratios, 10, ai));
//		Assert.assertEquals(expectedAnswerF, QSigCompare.compareRatios(s1, s2, file1Ratios, file2Ratios, 10));
	}
	
	@Test
	public void testGetRatioFromCov() {
		String test = "FULLCOV=A:0,C:0,G:0,T:3,N:0,TOTAL:3;NOVELCOV=A:0,C:0,G:0,T:3,N:0,TOTAL:3;";
		
		Assert.assertEquals(0, QSigCompare.getRatioFromCoverageString(test, 'T')[0]);
		Assert.assertEquals(3, QSigCompare.getRatioFromCoverageString(test, 'T')[1]);
		
		test = "FULLCOV=A:0,C:0,G:0,T:3,N:0,TOTAL:3;NOVELCOV=A:1,C:1,G:1,T:13,N:0,TOTAL:16;";
		Assert.assertEquals(0, QSigCompare.getRatioFromCoverageString(test, 'T')[0]);
		Assert.assertEquals(3, QSigCompare.getRatioFromCoverageString(test, 'T')[1]);
		
		test = "FULLCOV=A:0,C:0,G:2,T:47,N:0,TOTAL:49;NOVELCOV=A:0,C:0,G:2,T:36,N:0,TOTAL:38;";
		Assert.assertEquals(2, QSigCompare.getRatioFromCoverageString(test, 'T')[0]);
		Assert.assertEquals(49, QSigCompare.getRatioFromCoverageString(test, 'T')[1]);
		
		test = "FULLCOV=A:0,C:19,G:9,T:2,N:0,TOTAL:30;NOVELCOV=A:0,C:17,G:8,T:2,N:0,TOTAL:27;";
		Assert.assertEquals(11, QSigCompare.getRatioFromCoverageString(test, 'C')[0]);
		Assert.assertEquals(30, QSigCompare.getRatioFromCoverageString(test, 'C')[1]);
		
//		// with N ref
//		Assert.assertEquals(11, QSigCompare.getRatioFromCoverageString(test, 'N')[0]);
//		Assert.assertEquals(30, QSigCompare.getRatioFromCoverageString(test, 'N')[1]);
	}
	
	@Test
	public void testGetDiscretisedValuesFromCov() {
		String test = "FULLCOV=A:0,C:0,G:0,T:3,N:0,TOTAL:3;NOVELCOV=A:0,C:0,G:0,T:3,N:0,TOTAL:3;";
		Assert.assertEquals(null, QSigCompare.getDiscretisedValuesFromCoverageString(test));
		
		test = "FULLCOV=A:0,C:0,G:0,T:13,N:0,TOTAL:13;NOVELCOV=A:0,C:0,G:0,T:13,N:0,TOTAL:13;";
		
		Assert.assertEquals(0.0, QSigCompare.getDiscretisedValuesFromCoverageString(test)[0]);
		Assert.assertEquals(0.0, QSigCompare.getDiscretisedValuesFromCoverageString(test)[1]);
		Assert.assertEquals(0.0, QSigCompare.getDiscretisedValuesFromCoverageString(test)[2]);
		Assert.assertEquals(1.0, QSigCompare.getDiscretisedValuesFromCoverageString(test)[3]);
		
		
		test = "FULLCOV=A:0,C:0,G:2,T:47,N:0,TOTAL:49;NOVELCOV=A:0,C:0,G:2,T:36,N:0,TOTAL:38;";
		
		Assert.assertEquals(0.0, QSigCompare.getDiscretisedValuesFromCoverageString(test)[0]);
		Assert.assertEquals(0.0, QSigCompare.getDiscretisedValuesFromCoverageString(test)[1]);
		Assert.assertEquals(0.0, QSigCompare.getDiscretisedValuesFromCoverageString(test)[2]);
		Assert.assertEquals(1.0, QSigCompare.getDiscretisedValuesFromCoverageString(test)[3]);
		
		test = "FULLCOV=A:0,C:19,G:9,T:2,N:0,TOTAL:30;NOVELCOV=A:0,C:17,G:8,T:2,N:0,TOTAL:27;";
		Assert.assertEquals(0.0, QSigCompare.getDiscretisedValuesFromCoverageString(test)[0]);
		Assert.assertEquals(0.5, QSigCompare.getDiscretisedValuesFromCoverageString(test)[1]);
		Assert.assertEquals(Double.NaN, QSigCompare.getDiscretisedValuesFromCoverageString(test)[2]);
		Assert.assertEquals(Double.NaN, QSigCompare.getDiscretisedValuesFromCoverageString(test)[3]);
		
	}
	
	@Test
	public void testGetRatioFromCovRealLifeData() {
		String test = "FULLCOV=A:0,C:0,G:0,T:100,TOTAL:100;NOVELCOV=A:0,C:0,G:0,T:100,TOTAL:100";
		
		Assert.assertEquals(0, QSigCompare.getRatioFromCoverageString(test, 'T')[0]);
		Assert.assertEquals(100, QSigCompare.getRatioFromCoverageString(test, 'T')[1]);
	}
	
	@Test
	public void testCompareFloatToDouble() {
		// doubles may be quicker on some processors...
		
		Random random = new Random();
		
		 long startTime = System.nanoTime();
		   // ... the code being measured ...
		 double  totalDifferenceDouble = 0d;
			for (int i = 0 ; i < 1000000 ; i++) {
				int f1NonRefCount = random.nextInt();
				int f1TotalCount = random.nextInt();
				int f2NonRefCount = random.nextInt();
				int f2TotalCount = random.nextInt();
				
				totalDifferenceDouble += Math.abs(((double)f1NonRefCount / f1TotalCount)- ((double)f2NonRefCount / f2TotalCount));
			}
			long	estimatedTime = System.nanoTime() - startTime;
			System.out.println("Tame taken double: " + estimatedTime);

			startTime = System.nanoTime();
		float totalDifferenceFloat = 0f;
		for (int i = 0 ; i < 1000000 ; i++) {
			int f1NonRefCount = random.nextInt();
			int f1TotalCount = random.nextInt();
			int f2NonRefCount = random.nextInt();
			int f2TotalCount = random.nextInt();
			
			totalDifferenceFloat += Math.abs(((float)f1NonRefCount / f1TotalCount)- ((float)f2NonRefCount / f2TotalCount));
		}
		
		 estimatedTime = System.nanoTime() - startTime;
		System.out.println("Tame taken float: " + estimatedTime);
		
		startTime = System.nanoTime();
		totalDifferenceFloat = 0f;
		for (int i = 0 ; i < 1000000 ; i++) {
			int f1NonRefCount = random.nextInt();
			int f1TotalCount = random.nextInt();
			int f2NonRefCount = random.nextInt();
			int f2TotalCount = random.nextInt();
			
			totalDifferenceFloat += Math.abs(((float)f1NonRefCount / f1TotalCount)- ((float)f2NonRefCount / f2TotalCount));
		}
		
		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Tame taken float: " + estimatedTime);
	}
	
	@Test
	public void testBamFileNameParsing() {
		String fileName = "/path/AAAA_1992/seq_mapped/SSSS_20110603.nopd.bcB16_09.bam";
		
		String bamName = fileName.substring(fileName.lastIndexOf(FileUtils.FILE_SEPARATOR) + 1);
		// if we have a seq_mapped bam, use the bam name to drive the query
		String runName = bamName.substring(0, bamName.indexOf("."));
		int firstDotPos = bamName.indexOf(".") + 1;
		int secondDotPos = bamName.indexOf(".", firstDotPos);
		String physicalDivision = bamName.substring(firstDotPos, secondDotPos);
		String barcode = bamName.substring(secondDotPos +1, bamName.lastIndexOf("."));
		System.out.println("Extracted following from file name: ");
		System.out.println("runName: " + runName);
		System.out.println("physicalDivision: " + physicalDivision);
		System.out.println("barcode: " + barcode);
	}
	
	@Test
	public void testGetFlagCutoff() {
		Assert.assertEquals("OK", QSigCompare.getFlag("AAA", 0.015f, 1, 0.035f));
		Assert.assertEquals("???", QSigCompare.getFlag("BAA", 0.015f, 1, 0.035f));
		Assert.assertEquals("OK", QSigCompare.getFlag("AAA", 0.034f, 1, 0.035f));
		Assert.assertEquals("???", QSigCompare.getFlag("ABB", 0.03501f, 1, 0.035f));
	}
	
	@Test
	public void testGetRating() {
		
		try {
			QSigCompare.getRating(null, null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			QSigCompare.getRating(new String[] {}, new String[] {});
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		Assert.assertEquals("AAA", QSigCompare.getRating(new String[] {"","",""}, new String[] {"","",""}));
		Assert.assertEquals("BAA", QSigCompare.getRating(new String[] {"apples","",""}, new String[] {"","",""}));
		Assert.assertEquals("BAA", QSigCompare.getRating(new String[] {"apples","",""}, new String[] {"oranges","",""}));
		Assert.assertEquals("ABA", QSigCompare.getRating(new String[] {"apples","",""}, new String[] {"apples","oranges","",""}));
		Assert.assertEquals("ABB", QSigCompare.getRating(new String[] {"apples","","grapes"}, new String[] {"apples","oranges","",""}));
		Assert.assertEquals("ABB", QSigCompare.getRating(new String[] {"apples","","grapes"}, new String[] {"apples","oranges","pears",""}));
		Assert.assertEquals("BBB", QSigCompare.getRating(new String[] {"apples","","grapes"}, new String[] {"mangoes","apples","oranges","pears",""}));
	}
}
