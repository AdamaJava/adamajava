package org.qcmg.coverage;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.util.Pair;
import org.qcmg.qio.gff3.Gff3Record;

public class CoverageJobTest {
	
//	static final QLogger logger = QLoggerFactory.getLogger(CoverageJobTest.class);
	private final HashSet<Pair<File, File>> pairs = new HashSet<>();
	
	@Test
	public void testConstructCoverageMap() throws Exception {
		HashSet<Gff3Record> features = new HashSet<>();
		HashMap<String, HashSet<Gff3Record>> refToFeaturesMap = new HashMap<>();
		
		Gff3Record gff = new Gff3Record();
		gff.setStart(100);
		gff.setEnd(200);
		
		features.add(gff);
		refToFeaturesMap.put("test", features);
		
		int arraySize = 1000;
		
//		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs, null, null, false, null);
		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs,  null, false, null, null,null);
		
		
		 
		
		cj.constructCoverageMap();
		int [] arrayNew = cj.getPerBaseCoverages(null);
		int [] arrayOld = new int[arraySize];
		constructCoverageMapOld(arrayOld, features);
		
		Assert.assertEquals(arraySize, arrayNew.length);
		// the coverage is calculated using a difference array, which starts out empty
		for (int i = 0 ; i < arraySize ; i++) {
			Assert.assertEquals(0, arrayNew[i]);
		}
		// the positions covered by the features must be the same as those of the old implementation
		assertRangesEqual(CoverageArrayUtils.toRanges(arrayOld), cj.getFeatureRanges());
		
	}
	@Test
	public void testConstructCoverageMapManyFeatures() throws Exception {
		HashSet<Gff3Record> features = new HashSet<>();
		HashMap<String, HashSet<Gff3Record>> refToFeaturesMap = new HashMap<>();
		
		int gffRange = 100;
		
		for (int i = 1 ; i < 1000 ; i++) {
			int startPosition = i *  1000;
			Gff3Record gff = new Gff3Record();
			gff.setStart(startPosition);
			gff.setEnd(startPosition + gffRange);
			features.add(gff);
		}
		refToFeaturesMap.put("test", features);
		
		int arraySize = 1000000;
		
//		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs, null, null, false, null);
		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs,  null, false, null, null, null);
		cj.constructCoverageMap();
		int [] arrayNew = cj.getPerBaseCoverages(null);
		int [] arrayOld = new int[arraySize];
		constructCoverageMapOld(arrayOld, features);
		
		Assert.assertEquals(arraySize, arrayNew.length);
		// the coverage is calculated using a difference array, which starts out empty
		for (int i = 0 ; i < arraySize ; i++) {
			Assert.assertEquals(0, arrayNew[i]);
		}
		// the positions covered by the features must be the same as those of the old implementation
		assertRangesEqual(CoverageArrayUtils.toRanges(arrayOld), cj.getFeatureRanges());
		
	}
	
	@Ignore
	public void testConstructCoverageMapPerformance() throws Exception {
		HashSet<Gff3Record> features = new HashSet<>();
		HashMap<String, HashSet<Gff3Record>> refToFeaturesMap = new HashMap<String, HashSet<Gff3Record>>();
		
		int gffRange = 100;
		
		for (int i = 1 ; i < 10000 ; i++) {
			int startPosition = i *  1000;
			Gff3Record gff = new Gff3Record();
			gff.setStart(startPosition);
			gff.setEnd(startPosition + gffRange);
			features.add(gff);
		}
		refToFeaturesMap.put("test", features);
		
		int arraySize = 10000000;
		
//		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs, null, null, false, null);
		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs,  null, false, null, null, null);
		long startTime = System.nanoTime();
		cj.constructCoverageMap();
		System.out.println("New: " + (System.nanoTime() - startTime));
		int [] arrayNew = cj.getPerBaseCoverages(null);
		int [] arrayOld = new int[arraySize];
		startTime = System.nanoTime();
		constructCoverageMapOld(arrayOld, features);
		System.out.println("Old: " + (System.nanoTime() - startTime));
		
		Assert.assertEquals(arraySize, arrayNew.length);
		assertRangesEqual(CoverageArrayUtils.toRanges(arrayOld), cj.getFeatureRanges());
		
	}
	
	
	private void constructCoverageMapOld(int [] perBaseCoverages, HashSet<Gff3Record> features) {
		// Initially set all values to -1 for no coverage at that coordinate
        Arrays.fill(perBaseCoverages, -1);
		// For all coordinates where a feature exists, set to zero coverage
		for (Gff3Record feature : features) {
			for (int coord = feature.getStart(); coord <= feature.getEnd(); coord++) {
				// GFF3 format uses 1-based feature coordinates; avoid problem
				// of GFF3 accidentally containing 0 coordinate
				if (coord > 0) {
					// Convert 1-based feature coordinates to 0-based indices
					perBaseCoverages[coord - 1] = 0;
				}
			}
		}
	}

	private static void assertRangesEqual(final int[][] expected, final int[][] actual) {
		Assert.assertEquals(expected.length, actual.length);
		for (int i = 0 ; i < expected.length ; i++) {
			Assert.assertArrayEquals(expected[i], actual[i]);
		}
	}

}
