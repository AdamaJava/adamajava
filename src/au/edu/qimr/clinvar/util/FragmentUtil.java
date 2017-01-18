package au.edu.qimr.clinvar.util;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public class FragmentUtil {
	private static final QLogger logger = QLoggerFactory.getLogger(FragmentUtil.class);
	
	public static String getFragmentString(String r1, String r2RevComp, String overlap) {
		String fragment = null;
		if (r1.indexOf(overlap) < 2 || r2RevComp.endsWith(overlap)) {
			fragment = r2RevComp.substring(0, r2RevComp.lastIndexOf(overlap)) + overlap + r1.substring(r1.indexOf(overlap) + overlap.length());
		} else if (r2RevComp.indexOf(overlap) < 2 || r1.endsWith(overlap)) {
			fragment = r1.substring(0, r1.lastIndexOf(overlap)) + overlap + r2RevComp.substring(r2RevComp.indexOf(overlap) + overlap.length());
		} else {
			logger.warn("neither r1 nor r2RevComp start with the overlap!!!");
			logger.warn("r1: " + r1);
			logger.warn("r2RevComp: " + r2RevComp);
			logger.warn("overlap: " + overlap);
//			continue;
		}
		return fragment;
	}

}
