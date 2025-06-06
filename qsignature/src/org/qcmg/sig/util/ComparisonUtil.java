/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.util;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.sig.model.Comparison;

import gnu.trove.map.hash.TIntByteHashMap;
import gnu.trove.map.hash.TIntShortHashMap;

public class ComparisonUtil {
	
	private static final QLogger logger = QLoggerFactory.getLogger(ComparisonUtil.class);
	
	public static String getComparisonsBody(List<Comparison> comparisons) {
		if (null == comparisons || comparisons.isEmpty()) {
			throw new IllegalArgumentException("null or empty list of comparisons to headerise");
		}
		
		StringBuilder sb = new StringBuilder();
		String mainFile = comparisons.getFirst().getMain();
		if (null != mainFile) {
			sb.append(mainFile).append('\t');
		}
		
		for (Comparison comp : comparisons) {
			sb.append(SignatureUtil.nf.format(comp.getScore())).append("[").append(comp.getOverlapCoverage())
			.append("],");
		}
		
		sb.deleteCharAt(sb.length() - 1);		// remove trailing comma
		
		return sb.toString();
	}
	
	public static boolean containsDodgyComparisons(List<Comparison> comparisons, double cutoff) {
		if (null == comparisons || comparisons.isEmpty()) {
			throw new IllegalArgumentException("null or empty list of comparisons to examine");
		}
		
		for (Comparison comp : comparisons) {
			if (comp.getScore() > cutoff) {
				return true;
			}
		}
		return false;
	}

	public static Comparison compareRatiosUsingSnps(final Map<ChrPosition, double[]> file1Ratios,
				final Map<ChrPosition, double[]> file2Ratios, File file1, File file2, Map<ChrPosition, ChrPosition> positionsOfInterest) {
			
		if (null == file1Ratios || null == file2Ratios) {
			throw new IllegalArgumentException("null maps passed to compareRatios");
		}
		if (null == file1 || null == file2) {
			throw new IllegalArgumentException("null files passed to compareRatios");
		}
		if (file1Ratios.isEmpty() || file2Ratios.isEmpty()) {
			return  new Comparison(file1.getAbsolutePath(), file1Ratios.size(), file2.getAbsolutePath(), file2Ratios.size(), 0, 0);
		}
		
		// if the files are the same, return a comparison object without doing the comparison
		if (file1.equals(file2)) {
			return  new Comparison(file1, file1Ratios.size(), file2, file2Ratios.size(), 0, file1Ratios.size());
		}
		
		boolean checkPositionsList = null != positionsOfInterest && ! positionsOfInterest.isEmpty();
		
		int match = 0;
		int totalCompared = 0;
		for (Entry<ChrPosition, double[]> file1RatiosEntry : file1Ratios.entrySet()) {
			
			// check to see if position is in the list of desired positions, if not continue
			if (checkPositionsList) {
				boolean overlap = positionsOfInterest.containsKey(file1RatiosEntry.getKey());
				if ( ! overlap)  {
					continue;
				}
			}
			
			// if coverage is zero, skip
			final double[] file1Ratio = file1RatiosEntry.getValue();
			
			/*
			 * See if we can get a clear genotype from file1Ratios before retrieving ratios from file2
			 */
			int g1 = 0;
			for (int i = 0 ; i < 4 ; i++) {
				double d = file1Ratio[i];
				if (d > 0.90000) {
					// this is all we care about
					g1 += i == 0 ? 1 : i == 1 ? 10 : i == 2 ? 100 : 1000;
					break;
				} else if (d >= 0.40000 && d <= 0.60000) {
					g1 += i == 0 ? 1 : i == 1 ? 10 : i == 2 ? 100 : 1000;
				}
			}
			
			if (g1 > 0) {
				
				final double[] file2Ratio = file2Ratios.get(file1RatiosEntry.getKey());
				if (null != file2Ratio) {
					int g2 = 0;
					for (int i = 0 ; i < 4 ; i++) {
						double d = file2Ratio[i];
						if (d > 0.90000) {
							// this is all we care about
							g2 += i == 0 ? 1 : i == 1 ? 10 : i == 2 ? 100 : 1000;
							break;
						} else if (d >= 0.40000 && d <= 0.60000) {
							g2 += i == 0 ? 1 : i == 1 ? 10 : i == 2 ? 100 : 1000;
						}
					}
					
					if ( g2 > 0) {
						if (g1 == g2) {
							match++;
						}
						totalCompared++;
					}
				}
			}
		}
		double concordance = totalCompared > 0 ? ((double)match / totalCompared) * 100 : 0;
		logger.info("match: " + match + ", totalCompared: " + totalCompared + " percentage concordance: " + concordance);

        return new Comparison(file1, file1Ratios.size(), file2, file2Ratios.size(), match, totalCompared);
	}
	
	public static Comparison compareRatiosUsingSnpsFloat(TIntShortHashMap file1Ratios, TIntShortHashMap file2Ratios, File file1, File file2) {
		return compareRatiosUsingSnpsFloat(file1Ratios, file2Ratios, file1.getAbsolutePath(), file2.getAbsolutePath()); 
	}
	public static Comparison compareRatiosUsingSnpsFloat(TIntShortHashMap file1Ratios, TIntShortHashMap file2Ratios, String file1, String file2) {
		if (null == file1Ratios || null == file2Ratios) {
			throw new IllegalArgumentException("null maps passed to compareRatios");
		}
		if (null == file1 || null == file2) {
			throw new IllegalArgumentException("null files passed to compareRatios");
		}
		if (file1Ratios.isEmpty() || file2Ratios.isEmpty()) {
			return  new Comparison(file1, file1Ratios.size(), file2, file2Ratios.size(), 0, 0);
		}
		
		if (file1.equals(file2)) {
			return  new Comparison(file1, file1Ratios.size(), file2, file2Ratios.size(), 0, file1Ratios.size());
		}
		AtomicInteger match = new AtomicInteger();
		AtomicInteger totalCompared = new AtomicInteger();
		
		file1Ratios.forEachEntry((int a, short s) -> {
			if (s > 0) {
				short s2 = file2Ratios.get(a);
				if (s2 > 0) {
					if (s == s2) {
						match.incrementAndGet();
					}
					totalCompared.incrementAndGet();
				}
			}
			return true;
		});

        return new Comparison(file1, file1Ratios.size(), file2, file2Ratios.size(), match.get(), totalCompared.get());
	}
	public static Comparison compareRatiosUsingSnpsFloat(TIntByteHashMap file1Ratios, TIntByteHashMap file2Ratios, File file1, File file2) {
		return compareRatiosUsingSnpsFloat(file1Ratios, file2Ratios, file1.getAbsolutePath(), file2.getAbsolutePath()); 
	}
	public static Comparison compareRatiosUsingSnpsFloat(TIntByteHashMap file1Ratios, TIntByteHashMap file2Ratios, String file1, String file2) {
		if (null == file1Ratios || null == file2Ratios) {
			throw new IllegalArgumentException("null maps passed to compareRatios");
		}
		if (null == file1 || null == file2) {
			throw new IllegalArgumentException("null files passed to compareRatios");
		}
		if (file1Ratios.isEmpty() || file2Ratios.isEmpty()) {
			return  new Comparison(file1, file1Ratios.size(), file2, file2Ratios.size(), 0, 0);
		}
		
		if (file1.equals(file2)) {
			return  new Comparison(file1, file1Ratios.size(), file2, file2Ratios.size(), 0, file1Ratios.size());
		}
		AtomicInteger match = new AtomicInteger();
		AtomicInteger totalCompared = new AtomicInteger();
		/*
		 * find the map with the smaller number of entries and iterate over that one - save some cycles
		 */
		boolean useFirst = file1Ratios.size() < file2Ratios.size();
		TIntByteHashMap iter = useFirst ? file1Ratios : file2Ratios;
		TIntByteHashMap lookup = useFirst ? file2Ratios : file1Ratios;
		
		iter.forEachEntry((int a, byte b) -> {
			if (SignatureUtil.isCodedGenotypeValid(b)) {
				byte b2 = lookup.get(a);
				if (b == b2) {
					match.incrementAndGet();
				}
				if (SignatureUtil.isCodedGenotypeValid(b2)) {
					totalCompared.incrementAndGet();
				}
			}
			return true;
		});

        return new Comparison(file1, file1Ratios.size(), file2, file2Ratios.size(), match.get(), totalCompared.get());
	}
}
