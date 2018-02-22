/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.summarise;


import java.io.File;

import org.qcmg.qprofiler.report.SummaryReport;

public interface Summarizer {
	public static final int FEEDBACK_LINES_COUNT = 1000000;

	/**
	 * Summarises the data held in the submitted file, returning a SummaryReport object
	 * 
	 * @param input String input containing data to be summarised
	 * @param index String index relating to the input to be summarised
	 * @param regions String[] containing regions in the input file to be summarised
	 * @return SummaryReport object containing the summations
	 * @throws Exception
	 */
	public SummaryReport summarize(String input, String index, String[] regions) throws Exception;
	public default SummaryReport summarize(File input) throws Exception {
		return summarize(input.getAbsolutePath(), null, null);
	}
}
