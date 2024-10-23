/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

interface Job {
	HashMap<String, HashMap<Integer, AtomicLong>> getResults();
	HashMap<String, List<LowReadDepthRegion>> getLowReadDepthResults();
	void run() throws Exception;
	String toString();
}
