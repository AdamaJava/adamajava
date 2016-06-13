/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.util.Map;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.motif.util.RegionCounter;

class TerminationJob implements Job {
	public static final Job INSTANCE = new TerminationJob();
	
	private TerminationJob() {
	}

	@Override
	public Map<ChrPosition, RegionCounter> getResults() {
		throw new IllegalStateException("Termination job never returns a result.");
	}

	@Override
	public void run() {
		throw new IllegalStateException("Termination job should never be run.");
	}

	@Override
	public String toString() {
		return "Termination job";
	}
}
