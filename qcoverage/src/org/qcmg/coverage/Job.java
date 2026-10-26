/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

interface Job {
	/**
	 * @return the coverage results, keyed by coverage type
	 */
	Map<Object, HashMap<String, HashMap<Integer, AtomicLong>>> getResults();
	/**
	 * @return the low read depth results, keyed by coverage type
	 */
	Map<Object, HashMap<String, List<LowReadDepthRegion>>> getLowReadDepthResults();
	void run() throws Exception;
	String toString();
}
