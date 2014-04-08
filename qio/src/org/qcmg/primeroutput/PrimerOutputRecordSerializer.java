/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.primeroutput;

import java.io.BufferedReader;
import java.io.IOException;

import org.qcmg.record.Serializer;

public final class PrimerOutputRecordSerializer extends
		Serializer<PrimerOutputRecord> {
	private final static String[] FIELD_PREFIXES = {		
	    "PRIMER_PAIR_",
	    "PRIMER_LEFT_",
	    "PRIMER_RIGHT_",
	    "PRIMER_LEFT_",
	    "PRIMER_RIGHT_",
	    "PRIMER_LEFT_",
	    "PRIMER_RIGHT_",
	    "PRIMER_LEFT_",
	    "PRIMER_RIGHT_",
	    "PRIMER_LEFT_",
	    "PRIMER_RIGHT_",
	    "PRIMER_LEFT_",
	    "PRIMER_RIGHT_",
	    "PRIMER_LEFT_",
	    "PRIMER_RIGHT_",
	    "PRIMER_LEFT_",
	    "PRIMER_RIGHT_",
	    "PRIMER_PAIR_",
	    "PRIMER_PAIR_",
	    "PRIMER_PAIR_",
	    "PRIMER_PAIR_",
	    "PRIMER_PAIR_",
	    "PRIMER_PAIR_"};

	private final static String[] FIELD_SUFFIXES = {
	    "_PENALTY",
	    "_PENALTY",
	    "_PENALTY",
	    "_SEQUENCE",
	    "_SEQUENCE",
	    "",
	    "",
	    "_TM",
	    "_TM",
	    "_GC_PERCENT",
	    "_GC_PERCENT",
	    "_SELF_ANY",
	    "_SELF_ANY",
	    "_SELF_END",
	    "_SELF_END",
	    "_END_STABILITY",
	    "_END_STABILITY",
	    "_COMPL_ANY",
	    "_COMPL_END",
	    "_PRODUCT_SIZE",
	    "_PRODUCT_TM",
	    "_PRODUCT_TM_OLIGO_TM_DIFF",
	    "_T_OPT_A"};

	static {
		assert(FIELD_PREFIXES.length == FIELD_SUFFIXES.length);
	}

	public PrimerOutputRecord parseRecord(final BufferedReader reader)
			throws Exception {
		String nextLine = reader.readLine();
		if (null == nextLine) {
			return null;
		}
		PrimerOutputRecord result = new PrimerOutputRecord();
		result.setPairPenalty(doubleValue(nextLine));
		result.setLeftPenalty(nextDoubleValue(reader));
		result.setRightPenalty(nextDoubleValue(reader));
		result.setLeftSequence(nextStringValue(reader));
		result.setRightSequence(nextStringValue(reader));
		result.setLeft(nextStringValue(reader));
		result.setRight(nextStringValue(reader));
		result.setLeftTm(nextDoubleValue(reader));
		result.setRightTm(nextDoubleValue(reader));
		result.setLeftGcPercent(nextDoubleValue(reader));
		result.setRightGcPercent(nextDoubleValue(reader));
		result.setLeftSelfAny(nextDoubleValue(reader));
		result.setRightSelfAny(nextDoubleValue(reader));
		result.setLeftSelfEnd(nextDoubleValue(reader));
		result.setRightSelfEnd(nextDoubleValue(reader));
		result.setLeftEndStability(nextDoubleValue(reader));
		result.setRightEndStability(nextDoubleValue(reader));
		result.setPairComplAny(nextDoubleValue(reader));
		result.setPairComplEnd(nextDoubleValue(reader));
		result.setPairProductSize(nextIntegerValue(reader));
		result.setPairProductTm(nextDoubleValue(reader));
		result.setPairProductTmOligoTmDiff(nextDoubleValue(reader));
		result.setPairTOptA(nextDoubleValue(reader));
		return result;		
	}

	public String serialise(final PrimerOutputRecord record) throws Exception {
		String result = null;
		return result;
	}
	
	private double nextDoubleValue(BufferedReader reader) throws NumberFormatException, Exception {
		return Double.parseDouble(nextStringValue(reader));
	}

	private double doubleValue(final String line) throws Exception {
		final String[] params = equalsPattern.split(line);
		if (2 != params.length) {
			throw new Exception("Bad format. Insufficient columns: '" + line
					+ "'");
		}
		return Double.parseDouble(params[1].trim());
	}

	private String nextStringValue(final BufferedReader reader)
			throws Exception {
		final String line = reader.readLine();
		return stringValue(line);
	}

	private String stringValue(final String line) throws Exception {
		final String[] params = equalsPattern.split(line);
		if (2 != params.length) {
			throw new Exception("Bad format. Insufficient columns: '" + line
					+ "'");
		}
		return params[1].trim();
	}

	private int nextIntegerValue(final BufferedReader reader) throws Exception {
		return Integer.parseInt(nextStringValue(reader));
	}
}
