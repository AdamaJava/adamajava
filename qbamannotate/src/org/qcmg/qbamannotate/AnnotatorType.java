/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamannotate;

import java.util.regex.Pattern;

import org.qcmg.ma.MARecord;

import htsjdk.samtools.SAMRecord;

public abstract class AnnotatorType {
	private static final Pattern delimiterPattern = Pattern.compile("[_]+");
	private int unmatchedRecordCount = 0;

	public abstract boolean annotate(final SAMRecord record) throws Exception;

	public abstract boolean annotate(final SAMRecord record,
			final MARecord maRecord) throws Exception;

	public void markRecordUnmatched(final SAMRecord record) {
		Object zmObj = record.getAttribute("ZM");
		if (null == zmObj) {
			setZMAttribute(record, -1);
			unmatchedRecordCount++;
		} else {
			String zm = (String) zmObj;
			Integer zmInteger = Integer.valueOf(zm);
			if (0 > zmInteger.intValue()) {
				unmatchedRecordCount++;
			}
		}
	}

	final public int getUnmatchedRecordCount() {
		return unmatchedRecordCount;
	}

	public abstract String generateReport() throws Exception;

	public abstract void resetCount();
	
	static void setZMAttribute(final SAMRecord record, int n) {
		record.setAttribute("ZM", Integer.toString(n));
	}

	static int compareTriplet(final SAMRecord record, final MARecord maRecord) {
		return compareTriplet(record.getReadName(), maRecord.getDefLine()
				.getReadName());
	}

	static int compareTriplet(final String readName1, final String readName2) {
		String[] indices1 = delimiterPattern.split(readName1);
		String[] indices2 = delimiterPattern.split(readName2);
		int panel1 = Integer.parseInt(indices1[0].trim());
		int x1 = Integer.parseInt(indices1[1].trim());
		int y1 = Integer.parseInt(indices1[2].trim());
		int panel2 = Integer.parseInt(indices2[0].trim());
		int x2 = Integer.parseInt(indices2[1].trim());
		int y2 = Integer.parseInt(indices2[2].trim());
		int result = panel1 - panel2;
		if (0 == result) {
			result = x1 - x2;
		}
		if (0 == result) {
			result = y1 - y2;
		}
		return result;
	}

}
