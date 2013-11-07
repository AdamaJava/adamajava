/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.consensuscalls;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ConsensusCallsSerializer {
	private static final Pattern tabbedPattern = Pattern.compile("[\\t]+");
	private static final Pattern commaPattern = Pattern.compile("[,]+");

	public static ConsensusCallsRecord nextRecord(final BufferedReader reader)
			throws Exception, IOException {
		ConsensusCallsRecord result = null;
		try {
			String line = reader.readLine();
			if (null != line) {
				result = parseRecord(line);
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw e;
		}
		return result;
	}

	static ConsensusCallsRecord parseRecord(final String line) throws Exception {
		String[] params = tabbedPattern.split(line);
		if (17 != params.length) {
			throw new Exception("Bad Consensus Calls format");
		}
		ConsensusCallsRecord result = new ConsensusCallsRecord();
		result.setChr(params[0]);
		result.setPosition(Integer.parseInt(params[1]));
		result.setAlleleDiColor1(params[2]);
		result.setAlleleDiColor2(params[3]);
		result.setReference(params[4]);
		result.setGenotype(params[5]);
		result.setPValue(Double.parseDouble(params[6]));
		parseFlags(result.getFlag(), params[7]);
		result.setCoverage(Integer.parseInt(params[8]));
		result.setNCountsNonReferenceAllele(Integer.parseInt(params[9]));
		result.setNCountsReferenceAllele(Integer.parseInt(params[10]));
		result.setNCountsNonReferenceAllele(Integer.parseInt(params[11]));
		result.setRefAvgQV(Integer.parseInt(params[12]));
		result.setNovelAvgQV(Integer.parseInt(params[13]));
		result.setHeterozygous(Integer.parseInt(params[14]));
		result.setAlgorithm(params[15]);
		result.setAlgorithmName(params[16]);
		return result;
	}

	public static void parseFlags(final List<ConsensusCallsFlag> list, final String value) throws Exception {
		String[] params = commaPattern.split(value);
		if (1 > params.length) {
			throw new Exception("Bad Consensus Calls Flag format");
		}
		for (String param : params) {
			list.add(ConsensusCallsFlag.fromValue(param));
		}
	}
}
