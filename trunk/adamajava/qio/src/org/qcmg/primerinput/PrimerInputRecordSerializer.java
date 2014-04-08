/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.primerinput;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.qcmg.gff3.GFF3Record;
import org.qcmg.record.Serializer;

public final class PrimerInputRecordSerializer extends
		Serializer<PrimerInputRecord> {
	private final static String SEQUENCE_ID = "SEQUENCE_ID";
	private final static String SEQUENCE_TEMPLATE = "SEQUENCE_TEMPLATE";
	private final static String SEQUENCE_TARGET = "SEQUENCE_TARGET";
	private final static String PRIMER_PRODUCT_MIN_TM = "PRIMER_PRODUCT_MIN_TM";
	private final static String PRIMER_PRODUCT_MAX_TM = "PRIMER_PRODUCT_MAX_TM";
	private final static String PRIMER_DNA_CONC = "PRIMER_DNA_CONC";
	private final static String PRIMER_SALT_CONC = "PRIMER_SALT_CONC";
	private final static String PRIMER_MIN_TM = "PRIMER_MIN_TM";
	private final static String PRIMER_OPT_TM = "PRIMER_OPT_TM";
	private final static String PRIMER_MAX_TM = "PRIMER_MAX_TM";
	private final static String PRIMER_MIN_SIZE = "PRIMER_MIN_SIZE";
	private final static String PRIMER_OPT_SIZE = "PRIMER_OPT_SIZE";
	private final static String PRIMER_MAX_SIZE = "PRIMER_MAX_SIZE";
	private final static String PRIMER_PRODUCT_SIZE_RANGE = "PRIMER_PRODUCT_SIZE_RANGE";
	private final static String PRIMER_EXPLAIN_FLAG = "PRIMER_EXPLAIN_FLAG";
	private final static String PRIMER_NUM_RETURN = "PRIMER_NUM_RETURN";
	private final static String PRIMER_NUM_NS_ACCEPTED = "PRIMER_NUM_NS_ACCEPTED";
	private final static String EQUALS = "=";

	public PrimerInputRecord parseRecord(final BufferedReader reader)
			throws Exception {
		String nextLine = nextStringValue(reader);
		if (nextLine.equals("=")) {
			return null;
		}
		PrimerInputRecord result = new PrimerInputRecord();
		result.setSequenceId(nextLine);
		result.setSequenceTemplate(nextStringValue(reader));
		result.setSequenceTarget(nextTargetValue(reader));
		result.setPrimerProductMinTm(nextIntegerValue(reader));
		result.setPrimerProductMaxTm(nextIntegerValue(reader));
		result.setPrimerDnaConc(nextDoubleValue(reader));
		result.setPrimerSaltConc(nextDoubleValue(reader));
		result.setPrimerMinTm(nextIntegerValue(reader));
		result.setPrimerOptTm(nextIntegerValue(reader));
		result.setPrimerMaxTm(nextIntegerValue(reader));
		result.setPrimerMinSize(nextIntegerValue(reader));
		result.setPrimerOptSize(nextIntegerValue(reader));
		result.setPrimerMaxSize(nextIntegerValue(reader));
		result.setPrimerProductSizeRange(nextPrimerSizeRangeValue(reader));
		result.setPrimerExplainFlag(nextBooleanValue(reader));
		result.setPrimerNumReturn(nextIntegerValue(reader));
		result.setPrimerNumNsAccepted(nextBooleanValue(reader));
		return result;
	}

	private double nextDoubleValue(BufferedReader reader) throws NumberFormatException, Exception {
		return Double.parseDouble(nextStringValue(reader));
	}

	private PrimerSizeRange nextPrimerSizeRangeValue(BufferedReader reader)
			throws Exception {
		String targetValue = nextStringValue(reader);
		final String[] params = hyphenPattern.split(targetValue, -1);
		if (2 != params.length) {
			throw new Exception("Bad format. Insufficient numbered values: '"
					+ targetValue + "'");
		}
		PrimerSizeRange range = new PrimerSizeRange();
		range.setLowerLimit(Integer.parseInt(params[0]));
		range.setUpperLimit(Integer.parseInt(params[1]));
		return range;
	}

	private boolean nextBooleanValue(BufferedReader reader) throws Exception {
		return Boolean.parseBoolean(nextStringValue(reader));
	}

	private String nextStringValue(final BufferedReader reader)
			throws Exception {
		final String line = reader.readLine();
		final String[] params = equalsPattern.split(line, -1);
		if (2 != params.length) {
			throw new Exception("Bad format. Insufficient columns: '" + line
					+ "'");
		}
		return params[1].trim();
	}

	private int nextIntegerValue(final BufferedReader reader) throws Exception {
		return Integer.parseInt(nextStringValue(reader));
	}

	private PrimerSequenceTarget nextTargetValue(final BufferedReader reader)
			throws Exception {
		String targetValue = nextStringValue(reader);
		final String[] params = commaPattern.split(targetValue, -1);
		if (2 != params.length) {
			throw new Exception("Bad format. Insufficient numbered values: '"
					+ targetValue + "'");
		}
		PrimerSequenceTarget target = new PrimerSequenceTarget();
		target.setLeftValue(Integer.parseInt(params[0]));
		target.setRightValue(Integer.parseInt(params[1]));
		return target;
	}

	public String serialise(final PrimerInputRecord record) throws Exception {
		String result = addLine("", SEQUENCE_ID, record.getSequenceId());
		result = addLine(result, SEQUENCE_TEMPLATE, record
				.getSequenceTemplate());
		result = addLine(result, SEQUENCE_TARGET, record.getSequenceTarget());
		result = addLine(result, PRIMER_PRODUCT_MIN_TM, record
				.getPrimerProductMinTm());
		result = addLine(result, PRIMER_PRODUCT_MAX_TM, record
				.getPrimerProductMaxTm());
		result = addLine(result, PRIMER_DNA_CONC, record.getPrimerDnaConc());
		result = addLine(result, PRIMER_SALT_CONC, record.getPrimerSaltConc());
		result = addLine(result, PRIMER_MIN_TM, record.getPrimerMinTm());
		result = addLine(result, PRIMER_OPT_TM, record.getPrimerOptTm());
		result = addLine(result, PRIMER_MAX_TM, record.getPrimerMaxTm());
		result = addLine(result, PRIMER_MIN_SIZE, record.getPrimerMinSize());
		result = addLine(result, PRIMER_OPT_SIZE, record.getPrimerOptSize());
		result = addLine(result, PRIMER_MAX_SIZE, record.getPrimerMaxSize());
		result = addLine(result, PRIMER_PRODUCT_SIZE_RANGE, record
				.getPrimerProductSizeRange());
		result = addLine(result, PRIMER_EXPLAIN_FLAG, record
				.isPrimerExplainFlag());
		result = addLine(result, PRIMER_NUM_RETURN, record.getPrimerNumReturn());
		result = addLine(result, PRIMER_NUM_NS_ACCEPTED, record
				.isPrimerNumNsAccepted());
		return result;
	}

	public static void initialise(PrimerInputRecord record) {
		record.setPrimerDnaConc(120);
		record.setPrimerSaltConc(50);
		record.setPrimerExplainFlag(false);
		record.setPrimerMaxSize(25);
		record.setPrimerExplainFlag(true);
		record.setPrimerMaxSize(25);
		record.setPrimerMaxTm(75);
		record.setPrimerMinSize(18);
		record.setPrimerMinTm(55);
		record.setPrimerNumNsAccepted(true);
		record.setPrimerNumReturn(10000);
		record.setPrimerOptSize(20);
		record.setPrimerOptTm(65);
		record.setPrimerProductMaxTm(85);
		record.setPrimerProductMinTm(65);
		PrimerSizeRange range = new PrimerSizeRange();
		range.setLowerLimit(50);
		range.setUpperLimit(120);
		record.setPrimerProductSizeRange(range);
		record.setSequenceId("");
		record.setSequenceTemplate("");
		PrimerSequenceTarget target = new PrimerSequenceTarget();
		target.setLeftValue(249);
		target.setRightValue(3);
		record.setSequenceTarget(target);
	}

	private static String addLine(final String result, final String lhs,
			final String rhs) {
		return result + lhs + EQUALS + rhs + NEWLINE;
	}

	private static String addLine(String result, final String lhs,
			final double rhs) {
		return result + lhs + EQUALS + Double.toString(rhs) + NEWLINE;
	}

	private static String addLine(String result, final String lhs, final int rhs) {
		return result + lhs + EQUALS + Integer.toString(rhs) + NEWLINE;
	}

	private static String addLine(String result, String lhs,
			final PrimerSequenceTarget rhs) {
		return result + lhs + EQUALS + rhs.getLeftValue() + ","
				+ rhs.getRightValue() + NEWLINE;
	}

	private static String addLine(String result, String lhs, final boolean rhs) {
		if (rhs) {
			return result + lhs + EQUALS + "1" + NEWLINE;
		} else {
			return result + lhs + EQUALS + "0" + NEWLINE;
		}
	}

	private static String addLine(String result, String lhs,
			final PrimerSizeRange rhs) {
		return result + lhs + EQUALS + rhs.getLowerLimit() + "-"
				+ rhs.getUpperLimit() + NEWLINE;
	}

}
