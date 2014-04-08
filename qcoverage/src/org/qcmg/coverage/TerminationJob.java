/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

class TerminationJob implements Job {
	public static final Job INSTANCE = new TerminationJob();
	
	private TerminationJob() {
	}

	public HashMap<String, HashMap<Integer, AtomicLong>> getResults() {
		throw new IllegalStateException("Termination job never returns a result.");
	}

	public void run() {
		throw new IllegalStateException("Termination job should never be run.");
	}

	@Override
	public String toString() {
		return "Termination job";
	}
}
