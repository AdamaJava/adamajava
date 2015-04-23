package au.edu.qimr.clinvar.util;

import java.util.Set;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

import au.edu.qimr.clinvar.model.FastqProbeMatch;

public class FastqProbeMatchUtil {
	private static QLogger logger =  QLoggerFactory.getLogger(FastqProbeMatchUtil.class);
	
	public static boolean isMultiMatched(FastqProbeMatch fpm) {
		
		return bothReadsHaveAMatch(fpm)
				&& ! fpm.getRead1Probe().equals(fpm.getRead2Probe());
	}
	
	/**
	 * both reads match to the same probe
	 * @param fpm
	 * @return
	 */
	public static boolean isProperlyMatched(FastqProbeMatch fpm) {
		
		return bothReadsHaveAMatch(fpm)
				&& fpm.getRead1Probe().equals(fpm.getRead2Probe());
	}
	
	/**
	 * Both reads have a match, although they may not be the same...
	 */
	public static boolean bothReadsHaveAMatch(FastqProbeMatch fpm) {
		
		return null != fpm.getRead1Probe() 
				&& null != fpm.getRead2Probe();
	}
	
	/**
	 * Both reads have a match, although they may not be the same...
	 */
	public static boolean neitherReadsHaveAMatch(FastqProbeMatch fpm) {
		
		return null == fpm.getRead1Probe() 
				&& null == fpm.getRead2Probe();
	}
	
	
	
	public static void getStats(Set<FastqProbeMatch> set) {
		
		int bothReadsHaveAMatch = 0;
		int sameMatch = 0;
		int differentMatches = 0;
		int neitherHaveAMatch = 0;
		int justOneMatch = 0;
		int count = set.size();
		
		for (FastqProbeMatch fpm : set) {
			
			if (neitherReadsHaveAMatch(fpm)) {
				neitherHaveAMatch++;
			} else {
				// at least 1 match
				if (bothReadsHaveAMatch(fpm)) {
					bothReadsHaveAMatch++;
					if (isProperlyMatched(fpm)) {
						sameMatch++;
					}
					if (isMultiMatched(fpm)) {
						differentMatches++;
					}
					
				} else {
					justOneMatch++;
				}
			}
		}
		
		logger.info("Total count: " + count);
		logger.info("Neither read matches: " + neitherHaveAMatch + " (" + ((100 * neitherHaveAMatch) / count) + "%)");
		logger.info("One read has a match: " + justOneMatch + " (" + ((100 * justOneMatch) / count) + "%)");
		logger.info("Both reads have a match: " + bothReadsHaveAMatch + " (" + ((100 * bothReadsHaveAMatch) / count) + "%)");
		logger.info("Both reads have the same match: " + sameMatch + " (" + ((100 * sameMatch) / count) + "%)");
		logger.info("Reads have a different match: " + differentMatches + " (" + ((100 * differentMatches) / count) + "%)");
		
		
	}
	
	

}
