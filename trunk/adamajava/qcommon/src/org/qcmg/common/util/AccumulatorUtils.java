package org.qcmg.common.util;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.PileupElementLite;

public class AccumulatorUtils {
	
	public static boolean bothStrands(Accumulator acc) {
		if (acc.getPELs().stream() 
				.filter(pel -> pel.isFoundOnBothStrands()).findAny().isPresent()) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Here we are returning true if there is coverage on both strands, 
	 * AND that the number of reads on the least well represented strand 
	 * accounts for more than the supplied percentage of the total number of reads
	 * 
	 * @param acc
	 * @param percentage
	 */
	public static boolean bothStrandsByPercentage(Accumulator acc, int percentage) {
		AtomicInteger fs = new AtomicInteger();
		AtomicInteger rs = new AtomicInteger();
		if (null != acc) {
			acc.getPELs().stream() 
					.forEach(pel -> {fs.addAndGet(pel.getForwardCount()); rs.addAndGet(pel.getReverseCount());});
		}
		return areBothStrandsRepresented(fs, rs, percentage);
	}
	
	public static boolean bothStrandsByPercentageCS(Map<String, short[]> basesCountsNNS, int percentage) {
		AtomicInteger fs = new AtomicInteger();
		AtomicInteger rs = new AtomicInteger();
		basesCountsNNS.values().stream().forEach(sa -> {fs.addAndGet(sa[0]);	rs.addAndGet(sa[2]);});
		
		return  areBothStrandsRepresented(fs, rs, percentage);
	}
	
	public static boolean areBothStrandsRepresented(AtomicInteger fs, AtomicInteger rs, int percentage) {
		return areBothStrandsRepresented(fs.get(), rs.get(), percentage);
	}
	public static boolean areBothStrandsRepresented(int fs, int rs, int percentage) {
		if (fs == 0 || rs == 0) {
			return false;
		}
		int min = Math.min(fs, rs);
		return ((double) min / (fs + rs)) * 100 > percentage;
	}
	
	/**
	 * Returns the read count for this accumulator, minus the reads for the supplied PEL (if supplied)
	 * 
	 * @param acc
	 * @param exclude
	 * @return
	 */
	public static int getReadCount(Accumulator acc, PileupElementLite exclude) {
		if (null == acc) {
			throw new IllegalArgumentException("null Accumulator object passed to AccumulatorUtils.getReadCount()");
		}
		if (null == exclude) {
			return acc.getCoverage();
		}
		
		return acc.getPELs().stream() 
				.filter(o -> ! o.equals(exclude))
				.mapToInt(pel -> pel.getTotalCount()).sum();
		
	}
	public static int getReadCount(Accumulator acc) {
		return getReadCount(acc, null);
	}
	
	public static TIntIntMap getReadIdStartPosMap(Accumulator acc) {
		if (null != acc) {
			List<PileupElementLite> pels = acc.getPELs();
			TIntIntMap combinedMap = new TIntIntHashMap();
			for (PileupElementLite p : pels) {
				combinedMap.putAll(PileupElementLiteUtil.getDetailsFromCombinedListInMap(p.getReadIdStartPositionsQualityList(true), 3, 0, 1));
				
				TIntIntMap rsM = PileupElementLiteUtil.getDetailsFromCombinedListInMap(p.getReadIdStartPositionsQualityList(false), 3, 0, 1);
				/*
				 * convert keys in reverse strand map to -ve
				 */
				rsM.forEachEntry((k,v) -> {combinedMap.put( - k, v); return true;});
			}
			return combinedMap;
		}
		return null;
	}
	
	public static TIntIntMap getReadIdStartPosMap(List<Accumulator> accs) {
		if (null != accs) {
			
			TIntIntMap combinedMap = new TIntIntHashMap();
			
			for (Accumulator acc : accs) {
				
				List<PileupElementLite> pels = acc.getPELs();
				
				for (PileupElementLite p : pels) {
					combinedMap.putAll(PileupElementLiteUtil.getDetailsFromCombinedListInMap(p.getReadIdStartPositionsQualityList(true), 3, 0, 1));
					
					TIntIntMap rsM = PileupElementLiteUtil.getDetailsFromCombinedListInMap(p.getReadIdStartPositionsQualityList(false), 3, 0, 1);
					/*
					 * convert keys in reverse strand map to -ve
					 */
					rsM.forEachEntry((k,v) -> {combinedMap.put( - k, v); return true;});
				}
			}
			return combinedMap;
		}
		return null;
	}
	
	
	
	/**
	 * Attempts to create an Accumulator object in all its glory from a mere Observed Allele By Strand string (no doubt from the format field of a vcf record)
	 * format is:
	 * A10[40]0[0]
	 * 
	 * NOTE that this Accumulator object will not contain unfiltered records, nor will it have meaningful information for ends of reads or read ids or novel starts.
	 * 
	 * SHOULD BE USED FOR TESTING PURPOSES ONLY!!!
	 * 
	 * @param oabs
	 * @return
	 */
	public static Accumulator createFromOABS(String oabs, int position) {
		
		if (null != oabs) {
			Accumulator acc = new Accumulator(position);
			String [] alleles = oabs.split(Constants.SEMI_COLON_STRING);
			for (String a : alleles) {
				char base = a.charAt(0);
				int openBracketIndex = a.indexOf(Constants.OPEN_SQUARE_BRACKET);
				int closeBracketIndex = a.indexOf(Constants.CLOSE_SQUARE_BRACKET);
				int fsCount = Integer.parseInt(a.substring(1, openBracketIndex));
				float fsQual = Float.parseFloat(a.substring(openBracketIndex + 1, closeBracketIndex));
				/*
				 * reverse strand
				 */
				int openBracketIndexRS = a.indexOf(Constants.OPEN_SQUARE_BRACKET, openBracketIndex + 1);
				int closeBracketIndexRS = a.indexOf(Constants.CLOSE_SQUARE_BRACKET, closeBracketIndex + 1);
				int rsCount = Integer.parseInt(a.substring(closeBracketIndex + 1, openBracketIndexRS));
				float rsQual = Float.parseFloat(a.substring(openBracketIndexRS + 1, closeBracketIndexRS));
				
				for (int i = 0 ; i < fsCount ; i++) {
					acc.addBase((byte)base, (byte)fsQual, true, position - 10, position, position + 10, i);
				}
				for (int i = 0 ; i < rsCount ; i++) {
					acc.addBase((byte)base, (byte)rsQual, false, position - 10, position, position + 10, i);
				}
			}
			return acc;
		}
		return null;
		
	}

}
