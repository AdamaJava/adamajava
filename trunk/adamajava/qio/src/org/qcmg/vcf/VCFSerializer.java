/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.vcf;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Vector;

import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.util.TabTokenizer;

public final class VCFSerializer {
//	private static final Pattern tabbedPattern = Pattern.compile("[\\t]+");
	private static final String DEFAULT_HEADER_PREFIX = "#";

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}
	
	public static VCFHeader readHeader(final BufferedReader reader) throws Exception {
		Vector<String> headerLines = new Vector<String>();
		String line = reader.readLine();
		while (null != line && line.startsWith("#")) {
			headerLines.add(line);
			line = reader.readLine();
		}
		return new VCFHeader(headerLines);
	}

	public static VCFRecord nextRecord(final BufferedReader reader)
			throws IOException , Exception {
		VCFRecord result = null;
		String line = nextNonheaderLine(reader);
		if (null != line) {
			result = parseRecord(line);
		}
		return result;
	}

	static VCFRecord parseRecord(final String line) throws Exception {
		String[] params = TabTokenizer.tokenize(line);
		int arrayLength = params.length; 
		if (8 > arrayLength) {
			throw new Exception("Bad VCF format. Insufficient columns: '" + line + "'");
		}
		VCFRecord result = new VCFRecord();
		result.setChromosome(params[0]);
		result.setPosition(Integer.parseInt(params[1]));
		result.setId(params[2]);
		result.setRef(params[3].charAt(0));
		result.setAlt(params[4]);
		result.setQualString(params[5]);
		result.setFilter(params[6]);
		result.setInfo(params[7]);
		for (int i = 8 ; i < arrayLength ; i++) {
			result.addExtraField(params[i]);
		}
//		result.setGenotype(params[9]);
//		calculateGenotypeEnum(result);
		return result;
	}
	
//	private static void calculateGenotypeEnum(VCFRecord record) {
//		String genotypeString = record.getGenotype().substring(0, 3);
//		
//		if ("0/1".equals(genotypeString)) {
//			record.setGenotypeEnum(GenotypeEnum.getGenotypeEnum(record.getRef(), record.getAlt().charAt(0)));
//		} else if ("1/1".equals(genotypeString)) {
//			record.setGenotypeEnum(GenotypeEnum.getGenotypeEnum(record.getAlt().charAt(0), record.getAlt().charAt(0)));
//		} else if ("0/0".equals(genotypeString)) {
//			record.setGenotypeEnum(GenotypeEnum.getGenotypeEnum(record.getRef(), record.getRef()));
//		} else {
//			System.out.println("unhandled genotype string: " + genotypeString);
//		}
//	}
}
