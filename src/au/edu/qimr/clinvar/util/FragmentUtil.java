package au.edu.qimr.clinvar.util;

import java.util.Optional;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;

import au.edu.qimr.clinvar.model.Fragment;
import au.edu.qimr.clinvar.model.Fragment2;

public class FragmentUtil {
	private static final QLogger logger = QLoggerFactory.getLogger(FragmentUtil.class);
	
	public static Optional<String> getFragmentString(String r1, String r2RevComp, String overlap) {
		String fragment = null;
		if (r1.indexOf(overlap) < 2 || r2RevComp.endsWith(overlap)) {
			fragment = r2RevComp.substring(0, r2RevComp.lastIndexOf(overlap)) + overlap + r1.substring(r1.indexOf(overlap) + overlap.length());
		} else if (r2RevComp.indexOf(overlap) < 2 || r1.endsWith(overlap)) {
			fragment = r1.substring(0, r1.lastIndexOf(overlap)) + overlap + r2RevComp.substring(r2RevComp.indexOf(overlap) + overlap.length());
		} else {
//			logger.warn("neither r1 nor r2RevComp start with the overlap!!!");
//			logger.warn("r1: " + r1);
//			logger.warn("r2RevComp: " + r2RevComp);
//			logger.warn("overlap: " + overlap);
		}
		return Optional.ofNullable(fragment);
	}
	
	/**
	 * attempts to return the bases in the Fragment sequence that represent the supplied ChrPosition obj (and length offset)
	 * 
	 * @param cp
	 * @param f
	 * @param len
	 * @return
	 */
	public static Optional<String> getBasesAtPosition(ChrPosition cp, Fragment2 f, int len) {
		ChrPosition fCp = f.getPosition();
		int offset = cp.getStartPosition() - fCp.getStartPosition();
		if (offset >  f.getSequence().length()) {
			//hmmm
//			logger.warn("cp : " + cp.toIGVString() + ", fCP: " + fCp.toIGVString() + ", fragment length: " + f.getSequence().length() + ", len: " + len + ", fragment: " + f.getSequence());
			return Optional.empty();
		} else if (offset >= 0 && offset + len <= f.getSequence().length()){
			return Optional.ofNullable(f.getSequence().substring(offset, offset + len));
		} else {
//			logger.warn("cp : " + cp.toIGVString() + ", fCP: " + fCp.toIGVString() + ", fragment length: " + f.getSequence().length() + ", len: " + len + ",offset: " + offset + ", fragment: " + f.getSequence());
			
			return Optional.empty();
		}
	}

}
