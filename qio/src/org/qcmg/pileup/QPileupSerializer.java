/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.qcmg.common.util.BaseUtils;
import org.qcmg.pileup.QSnpRecord.Classification;

public final class QPileupSerializer {
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

	public static QSnpRecord nextRecord(final BufferedReader reader)
			throws IOException , Exception {
		QSnpRecord result = null;
		String line = nextNonheaderLine(reader);
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static QSnpRecord parseRecord(final String line) throws Exception {
		String[] params = tabbedPattern.split(line, -1);
//		if (4 > params.length) {
//			throw new Exception("Bad Pileup format. Insufficient columns: '" + line + "'");
//		}
		QSnpRecord result = new QSnpRecord();
		
		result.setChromosome(params[0]);
		result.setPosition(Integer.parseInt(params[1]));
		//FIXME - just want the actual pileup ideally...
		result.setPileup(line);
		result.setNormalGenotype(BaseUtils.getGenotypeEnum(params[15]));
		result.setTumourGenotype(BaseUtils.getGenotypeEnum(params[16]));
		result.setClassification(getClassification(params[17]));
		result.setMutation(params[18]);
		result.setAnnotation(params[19]);
		
		return result;
	}
	
	private static Classification getClassification(String classification) {
		if ("SOMATIC".equals(classification)) return Classification.SOMATIC;
		if ("GERMLINE".equals(classification)) return Classification.GERMLINE;
		return null;
	}
}
