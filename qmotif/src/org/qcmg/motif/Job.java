/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.util.Map;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.motif.util.RegionCounter;

interface Job {
	public Map<ChrPosition, RegionCounter> getResults();
	public void run() throws Exception;
	@Override
	public String toString();
}
