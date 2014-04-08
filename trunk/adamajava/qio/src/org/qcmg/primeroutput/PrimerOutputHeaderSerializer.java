/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.primeroutput;

import java.io.BufferedReader;

import org.qcmg.record.Serializer;

public class PrimerOutputHeaderSerializer extends
		Serializer<PrimerOutputHeader> {
	private final static String[] FIELDS = {
	    "SEQUENCE_ID",
	    "SEQUENCE_TEMPLATE",
	    "SEQUENCE_TARGET",
	    "PRIMER_PRODUCT_MIN_TM",
	    "PRIMER_PRODUCT_MAX_TM",
	    "PRIMER_DNA_CONC",
	    "PRIMER_SALT_CONC",
	    "PRIMER_MIN_TM",
	    "PRIMER_OPT_TM",
	    "PRIMER_MAX_TM",
	    "PRIMER_MIN_SIZE",
	    "PRIMER_OPT_SIZE",
	    "PRIMER_MAX_SIZE",
	    "PRIMER_PRODUCT_SIZE_RANGE",
	    "PRIMER_EXPLAIN_FLAG",
	    "PRIMER_NUM_RETURN",
	    "PRIMER_NUM_NS_ACCEPTED",
	    "PRIMER_LEFT_EXPLAIN",
	    "PRIMER_RIGHT_EXPLAIN",
	    "PRIMER_PAIR_EXPLAIN",
	    "PRIMER_LEFT_NUM_RETURNED",
	    "PRIMER_RIGHT_NUM_RETURNED",
	    "PRIMER_INTERNAL_NUM_RETURNED",
	    "PRIMER_PAIR_NUM_RETURNED"};

	static {
		assert(24 == FIELDS.length);
	}
	
	@Override
	public PrimerOutputHeader parseRecord(final BufferedReader reader)
			throws Exception {
		PrimerOutputHeader result = new PrimerOutputHeader();
		result.setSequenceId(nextStringValue(reader));
		result.setSequenceTemplate(nextStringValue(reader));
		result.setSequenceTarget(nextStringValue(reader));
		result.setProductMinTm(nextIntegerValue(reader));
		result.setProductMaxTm(nextIntegerValue(reader));
		result.setDnaConc(nextDoubleValue(reader));
		result.setSaltConc(nextDoubleValue(reader));
		result.setMinTm(nextIntegerValue(reader));
		result.setOptTm(nextIntegerValue(reader));
		result.setMaxTm(nextIntegerValue(reader));
		result.setMinSize(nextIntegerValue(reader));
		result.setOptSize(nextIntegerValue(reader));
		result.setMaxSize(nextIntegerValue(reader));
		result.setProductSizeRange(nextStringValue(reader));
		result.setExplainFlag(nextBooleanValue(reader));
		result.setNumReturn(nextIntegerValue(reader));
		result.setNumNsAccepted(nextIntegerValue(reader));
		result.setLeftExplain(nextStringValue(reader));
		result.setRightExplain(nextStringValue(reader));
		result.setPairExplain(nextStringValue(reader));
		result.setLeftNumReturned(nextIntegerValue(reader));
		result.setRightNumReturned(nextIntegerValue(reader));
		result.setInternalNumReturned(nextIntegerValue(reader));
		result.setPairNumReturned(nextIntegerValue(reader));
		return result;
	}

	@Override
	public String serialise(PrimerOutputHeader record) throws Exception {
		String result = createLine(FIELDS[0], record.getSequenceId());
		result += FIELDS[1] + EQUALS + record.getSequenceTemplate() + NEWLINE;
		result += FIELDS[2] + EQUALS + record.getSequenceTarget() + NEWLINE;
		result += FIELDS[3] + EQUALS + record.getProductMinTm() + NEWLINE;
		result += FIELDS[4] + EQUALS + record.getProductMaxTm() + NEWLINE;
		result += FIELDS[5] + EQUALS + record.getDnaConc() + NEWLINE;
		result += FIELDS[6] + EQUALS + record.getSaltConc() + NEWLINE;
		result += FIELDS[7] + EQUALS + record.getMinTm() + NEWLINE;
		result += FIELDS[8] + EQUALS + record.getOptTm() + NEWLINE;
		result += FIELDS[9] + EQUALS + record.getMaxTm() + NEWLINE;
		result += FIELDS[10] + EQUALS + record.getMinSize() + NEWLINE;
		result += FIELDS[11] + EQUALS + record.getOptSize() + NEWLINE;
		result += FIELDS[12] + EQUALS + record.getMaxSize() + NEWLINE;
		result += FIELDS[13] + EQUALS + record.getProductSizeRange() + NEWLINE;
		result += FIELDS[14] + EQUALS + record.isExplainFlag() + NEWLINE;
		result += FIELDS[15] + EQUALS + record.getNumReturn() + NEWLINE;
		result += FIELDS[16] + EQUALS + record.getNumNsAccepted() + NEWLINE;
		result += FIELDS[17] + EQUALS + record.getLeftExplain() + NEWLINE;
		result += FIELDS[18] + EQUALS + record.getRightExplain() + NEWLINE;
		result += FIELDS[19] + EQUALS + record.getPairExplain() + NEWLINE;
		result += FIELDS[20] + EQUALS + record.getLeftNumReturned() + NEWLINE;
		result += FIELDS[21] + EQUALS + record.getRightNumReturned() + NEWLINE;
		result += FIELDS[22] + EQUALS + record.getInternalNumReturned() + NEWLINE;
		result += FIELDS[23] + EQUALS + record.getPairNumReturned() + NEWLINE;
		return result;
	}

	private String createLine(final String fieldName, final String fieldValue) {
		return fieldName + EQUALS + fieldValue + NEWLINE;
	}
	
	private double nextDoubleValue(BufferedReader reader) throws NumberFormatException, Exception {
		return Double.parseDouble(nextStringValue(reader));
	}

	private boolean nextBooleanValue(BufferedReader reader) throws Exception {
		return Boolean.parseBoolean(nextStringValue(reader));
	}

	private String nextStringValue(final BufferedReader reader)
			throws Exception {
		final String line = reader.readLine();
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
