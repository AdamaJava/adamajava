package au.edu.qimr.panel.util;

import au.edu.qimr.panel.model.Fragment2;
import java.util.List;
import java.util.Optional;
import org.qcmg.common.model.ChrPosition;

public class FragmentUtil {	
	public static Optional<String> getFragmentString(String r1, String r2RevComp, String overlap) {
		String fragment = null;
		if (r1.indexOf(overlap) < 2 || r2RevComp.endsWith(overlap)) {
			fragment = r2RevComp.substring(0, r2RevComp.lastIndexOf(overlap)) + overlap + r1.substring(r1.indexOf(overlap) + overlap.length());
		} else if (r2RevComp.indexOf(overlap) < 2 || r1.endsWith(overlap)) {
			fragment = r1.substring(0, r1.lastIndexOf(overlap)) + overlap + r2RevComp.substring(r2RevComp.indexOf(overlap) + overlap.length());
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
			return Optional.empty();
		} else if (offset >= 0 && offset + len <= f.getSequence().length()) {
			return Optional.ofNullable(f.getSequence().substring(offset, offset + len));
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * given a list of Fragment2 object, this method will return an int array which will have the 
	 * forward strand record count as the first array element, 
	 * and the reverse strand count as the second array element
	 * @param fragments
	 * @return
	 */
	public static int[] getCountsFromFragments(List<Fragment2> fragments) {
		int [] counts = new int[2];
		if (null != fragments) {
			for (Fragment2 f : fragments) {
				counts[f.isForwardStrand() ? 0 : 1] += f.getRecordCount();
			}
		}
		return counts;
	}

}
