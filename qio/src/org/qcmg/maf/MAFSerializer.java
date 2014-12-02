/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.qcmg.common.maf.MAFRecord;

public final class MAFSerializer {
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

	public static MAFRecord nextRecord(final BufferedReader reader)
			throws IOException , Exception {
		MAFRecord result = null;
		String line = nextNonheaderLine(reader);
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static MAFRecord parseRecord(final String line) throws Exception {
		String[] params = tabbedPattern.split(line, -1);
		if (8 > params.length) {
			throw new Exception("Bad VCF format. Insufficient columns: '" + line + "'");
		}
		MAFRecord result = new MAFRecord();
//		result.setChromosome(params[0]);
//		result.setPosition(Integer.parseInt(params[1]));
//		result.setRef(params[3].charAt(0));
//		result.setAlt(params[4].charAt(0));
//		result.setGenotype(params[9]);
//		calculateGenotypeEnum(result);
		return result;
	}
	
	private static void calculateGenotypeEnum(MAFRecord record) {
		
//		String genotypeString = record.getGenotype().substring(0, 3);
//		
//		if ("0/1".equals(genotypeString)) {
//			record.setGenotypeEnum(GenotypeEnum.getGenotypeEnum(record.getRef(), record.getAlt()));
//		} else if ("1/1".equals(genotypeString)) {
//			record.setGenotypeEnum(GenotypeEnum.getGenotypeEnum(record.getAlt(), record.getAlt()));
//		} else if ("0/0".equals(genotypeString)) {
//			record.setGenotypeEnum(GenotypeEnum.getGenotypeEnum(record.getRef(), record.getRef()));
//		} else {
//			System.out.println("unhandled genotype string: " + genotypeString);
//		}
		
	}
}
