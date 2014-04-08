/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import java.io.File;

public class BAMFileUtils {
	
	public static boolean bamFileIsLocked(File bamFile) {
		if (bamFile != null) {
			File bamlock = new File (bamFile.getAbsolutePath() + ".lck");
			
			if (bamlock.exists()) {
				return true;
			}
			
			File indexLock = new File (bamFile.getAbsolutePath() + ".bai.lck");
			
			if (indexLock.exists()) {
				return true;
			}
		}
		return false;
	}

}
