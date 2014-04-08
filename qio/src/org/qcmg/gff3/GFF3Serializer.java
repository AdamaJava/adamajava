/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.gff3;

import java.io.BufferedReader;
import java.io.IOException;

import org.qcmg.common.util.TabTokenizer;

public final class GFF3Serializer {
	private static final String DEFAULT_HEADER_PREFIX = "#";

	private static String nextNonheaderLine(final BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		while (null != line && line.startsWith(DEFAULT_HEADER_PREFIX)) {
			line = reader.readLine();
		}
		return line;
	}

	public static GFF3Record nextRecord(final BufferedReader reader)
			throws Exception, IOException {
		GFF3Record result = null;
		try {
			
			String line = nextNonheaderLine(reader);
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

	static GFF3Record parseRecord(final String line) throws Exception {
		String[] params = TabTokenizer.tokenize(line);
		if (8 > params.length) {
			throw new Exception("Bad GFF3 format. Insufficient columns: '" + line + "'");
		}
		GFF3Record result = new GFF3Record();
		result.setRawData(line);
		result.setSeqId(params[0]);
		result.setSource(params[1]);
		result.setType(params[2]);
		result.setStart(Integer.parseInt(params[3]));
		result.setEnd(Integer.parseInt(params[4]));
		result.setScore(params[5]);
		result.setStrand(params[6]);
		result.setPhase(params[7]);
		if (8 < params.length) {
			result.setAttributes(params[8]);
		}
		return result;
	}

	public static GFF3Record duplicate(final GFF3Record record) {
		GFF3Record result = new GFF3Record();
		result.setSeqId(record.getSeqId());
		result.setSource(record.getSource());
		result.setType(record.getType());
		result.setStart(record.getStart());
		result.setEnd(record.getEnd());
		result.setScore(record.getScore());
		result.setStrand(record.getStrand());
		result.setPhase(record.getPhase());
		result.setAttributes(record.getAttributes());
		return result;
	}

	public static String serialise(final GFF3Record record) throws Exception {
		String result = record.getSeqId() + "\t";
		result += record.getSource() + "\t";
		result += record.getType() + "\t";
		result += record.getStart() + "\t";
		result += record.getEnd() + "\t";
		result += record.getScore() + "\t";
		result += record.getStrand() + "\t";
		result += record.getPhase() + "\t";
		if (null != record.getAttributes()) {
			result += record.getAttributes();
		}
		return result;
	}

}
