/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.util;

import net.sf.samtools.SAMRecord;

import org.qcmg.common.string.StringUtils;

public class FlagUtil {
	
	private static SAMRecord record = new SAMRecord(null);
	
	
	public static String getFlagString(int flags) {
		record.setFlags(flags);
		
		boolean readPaired = record.getReadPairedFlag();
		
		StringBuilder additionalData = new StringBuilder();
		if (readPaired)
			additionalData.append("p");
		if (readPaired && record.getProperPairFlag())
			additionalData.append("P");
		if (record.getReadUnmappedFlag())
			additionalData.append("u");
		if (readPaired && record.getMateUnmappedFlag())
			additionalData.append("U");
		if (record.getReadNegativeStrandFlag())
			additionalData.append("r");
		if (readPaired && record.getMateNegativeStrandFlag())
			additionalData.append("R");
		if (readPaired && record.getFirstOfPairFlag())
			additionalData.append("1");
		if (readPaired && record.getSecondOfPairFlag())
			additionalData.append("2");
		if (record.getNotPrimaryAlignmentFlag())
			additionalData.append("s");
		if (record.getReadFailsVendorQualityCheckFlag())
			additionalData.append("f");
		if (record.getDuplicateReadFlag())
			additionalData.append("d");
		
//		SummaryByCycleUtils.incrementCount(flagBinaryCount, StringUtils
//				.padString(Integer.toBinaryString(record.getFlags()), 12, '0', true) 
//				+ ((additionalData.length() > 0) ? ", " + additionalData : ""));
		
		return  StringUtils
		.padString(Integer.toBinaryString(flags), 12, '0', true) 
		+ ((additionalData.length() > 0) ? ", " + additionalData : "");
	}

}
