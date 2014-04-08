package org.qcmg.coverage;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.util.Pair;
import org.qcmg.gff3.GFF3Record;

public class CoverageJobTest {
	
//	static final QLogger logger = QLoggerFactory.getLogger(CoverageJobTest.class);
	private final HashSet<Pair<File, File>> pairs = new HashSet<Pair<File, File>>();
	
	@Test
	public void testConstructCoverageMap() throws Exception {
		HashSet<GFF3Record> features = new HashSet<GFF3Record>();
		HashMap<String, HashSet<GFF3Record>> refToFeaturesMap = new HashMap<String, HashSet<GFF3Record>>();
		
		GFF3Record gff = new GFF3Record();
		gff.setStart(100);
		gff.setEnd(200);
		
		features.add(gff);
		refToFeaturesMap.put("test", features);
		
		int arraySize = 1000;
		
//		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs, null, null, false, null);
		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs,  null, false, null, null,null);
		
		
		 
		
		cj.constructCoverageMap();
		int [] arrayNew = cj.getPerBaseCoverages();
		int [] arrayOld = new int[arraySize];
		constructCoverageMapOld(arrayOld, features);
		
		Assert.assertEquals(arraySize, arrayNew.length);
		for (int i = 0 ; i < arraySize ; i++) {
			if (i < gff.getStart()-1)
				Assert.assertEquals(-1, arrayNew[i]);
			else if (i < gff.getEnd())
				Assert.assertEquals(0, arrayNew[i]);
			else
				Assert.assertEquals(-1, arrayNew[i]);
		}
		Assert.assertArrayEquals(arrayNew, arrayOld);
		
	}
	@Test
	public void testConstructCoverageMapManyFeatures() throws Exception {
		HashSet<GFF3Record> features = new HashSet<GFF3Record>();
		HashMap<String, HashSet<GFF3Record>> refToFeaturesMap = new HashMap<String, HashSet<GFF3Record>>();
		
		int gffRange = 100;
		
		for (int i = 1 ; i < 1000 ; i++) {
			int startPosition = i *  1000;
			GFF3Record gff = new GFF3Record();
			gff.setStart(startPosition);
			gff.setEnd(startPosition + gffRange);
			features.add(gff);
		}
		refToFeaturesMap.put("test", features);
		
		int arraySize = 1000000;
		
//		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs, null, null, false, null);
		CoverageJob cj = new CoverageJob("test", arraySize, refToFeaturesMap, pairs,  null, false, null, null, null);
		cj.constructCoverageMap();
		int [] arrayNew = cj.getPerBaseCoverages();
		int [] arrayOld = new int[arraySize];
		constructCoverageMapOld(arrayOld, features);
		
		Assert.assertEquals(arraySize, arrayNew.length);
		for (GFF3Record gff : features) {
			int start = gff.getStart() -1;
			int end = gff.getEnd();
			for (int i = start ; i < end && i < arraySize; i++) {
				Assert.assertEquals(0, arrayNew[i]);
				Assert.assertEquals(0, arrayOld[i]);
			}
			for (int i = start -(gffRange-1) ;i > 0 && i < start && i < arraySize; i++) {
				Assert.assertEquals(-1, arrayNew[i]);
				Assert.assertEquals(-1, arrayOld[i]);
			}
		}
		Assert.assertArrayEquals(arrayNew, arrayOld);
		
	}
	
	@Ignore
	public void testConstructCoverageMapPerformance() throws Exception {
		HashSet<GFF3Record> features = new HashSet<GFF3Record>();
		HashMap<String, HashSet<GFF3Record>> refToFeaturesMap = new HashMap<String, HashSet<GFF3Record>>();
		
		int gffRange = 100;
		
		for (int i = 1 ; i < 10000 ; i++) {
			int startPosition = i *  1000;
			GFF3Record gff = new GFF3Record();
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
		int [] arrayNew = cj.getPerBaseCoverages();
		int [] arrayOld = new int[arraySize];
		startTime = System.nanoTime();
		constructCoverageMapOld(arrayOld, features);
		System.out.println("Old: " + (System.nanoTime() - startTime));
		
		Assert.assertEquals(arraySize, arrayNew.length);
		for (GFF3Record gff : features) {
			int start = gff.getStart() -1;
			int end = gff.getEnd();
			for (int i = start ; i < end && i < arraySize; i++) {
				Assert.assertEquals(0, arrayNew[i]);
				Assert.assertEquals(0, arrayOld[i]);
			}
			for (int i = start -(gffRange-1) ;i > 0 && i < start && i < arraySize; i++) {
				Assert.assertEquals(-1, arrayNew[i]);
				Assert.assertEquals(-1, arrayOld[i]);
			}
		}
		Assert.assertArrayEquals(arrayNew, arrayOld);
		
		
		// reset arrays
		startTime = System.nanoTime();
		cj.constructCoverageMap();
		System.out.println("New: " + (System.nanoTime() - startTime));
		
		arrayNew = cj.getPerBaseCoverages();
		arrayOld = new int[arraySize];
		
		startTime = System.nanoTime();
		constructCoverageMapOld(arrayOld, features);
		System.out.println("Old: " + (System.nanoTime() - startTime));
		
		Assert.assertArrayEquals(arrayNew, arrayOld);
	}
	
	
	private void constructCoverageMapOld(int [] perBaseCoverages, HashSet<GFF3Record> features) {
		// Initially set all values to -1 for no coverage at that coordinate
		for (int i = 0; i < perBaseCoverages.length; i++) {
			perBaseCoverages[i] = -1;
		}
		// For all coordinates where a feature exists, set to zero coverage
		for (GFF3Record feature : features) {
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

}
