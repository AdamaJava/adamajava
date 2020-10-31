/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.qcmg.common.model.Classification;


public final class VerifiedSnpSerializer {
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	private static final String DEFAULT_HEADER_PREFIX = "#";

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}

	public static VerifiedSnpRecord nextRecord(final BufferedReader reader)
			throws IOException , Exception {
		VerifiedSnpRecord result = null;
		String line = nextNonheaderLine(reader);
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static VerifiedSnpRecord parseRecord(final String line) throws Exception {
		String[] params = tabbedPattern.split(line, -1);
		VerifiedSnpRecord result = new VerifiedSnpRecord();
		
		result.setId(params[0]);
		result.setChromosome(params[3]);
		result.setPosition(Integer.parseInt(params[4]));
		result.setAnalysis(params[6]);
		result.setMutation(params[7]);
		result.setClassification(getClassification(params[8]));
		if (params[8].contains(" "))
			result.setClazz(params[8].substring(params[8].indexOf(' ')+1));
		
		if (params.length > 21)
			result.setStatus(params[22]);
		return result;
	}
//	static VerifiedSnpRecord parseRecord(final String line) throws Exception {
//		String[] params = tabbedPattern.split(line, -1);
//		VerifiedSnpRecord result = new VerifiedSnpRecord();
//		
//		result.setId(params[0]);
//		result.setChromosome((String) params[1].subSequence(0, params[1].indexOf(':')));
//		result.setPosition(Integer.parseInt((String) params[1].subSequence(params[1].indexOf(':') + 1, params[1].indexOf('-'))));
//		result.setMutation(params[2]);
//		result.setClassification(getClassification(params[3]));
//		if (params[3].contains(" "))
//			result.setClazz(params[3].substring(params[3].indexOf(' ')+1));
//		
//		result.setStatus(params[4]);
//		return result;
//	}
	
	private static Classification getClassification(String classification) {
		if (classification.startsWith("somatic")) return Classification.SOMATIC;
		if (classification.startsWith("germline")) return Classification.GERMLINE;
		return null;
	}
}
