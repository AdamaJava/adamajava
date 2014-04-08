/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import java.text.SimpleDateFormat;

import net.sf.samtools.SAMReadGroupRecord;

import org.qcmg.common.string.StringUtils;

public class SAMReadGroupRecordUtils {
	
	public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	
	public static String getRGString(SAMReadGroupRecord rec) {
		if (null == rec) throw new IllegalArgumentException("null SAMReadGroupRecord passed to getRGString");
		
		StringBuilder sb = new StringBuilder("@RG");
		if ( ! StringUtils.isNullOrEmpty(rec.getId()))
			sb.append(StringUtils.getTabAndString("ID:")).append(rec.getId());
		if ( ! StringUtils.isNullOrEmpty(rec.getSequencingCenter()))
			sb.append(StringUtils.getTabAndString("CN:")).append(rec.getSequencingCenter());
		if ( ! StringUtils.isNullOrEmpty(rec.getDescription()))
			sb.append(StringUtils.getTabAndString("DS:")).append(rec.getDescription());
		if (null != (rec.getRunDate()))
			sb.append(StringUtils.getTabAndString("DT:")).append(sdf.format(rec.getRunDate()));
		if ( ! StringUtils.isNullOrEmpty(rec.getFlowOrder()))
			sb.append(StringUtils.getTabAndString("FO:")).append(rec.getFlowOrder());
		if ( ! StringUtils.isNullOrEmpty(rec.getKeySequence()))
			sb.append(StringUtils.getTabAndString("KS:")).append(rec.getKeySequence());
		if ( ! StringUtils.isNullOrEmpty(rec.getLibrary()))
			sb.append(StringUtils.getTabAndString("LB:")).append(rec.getLibrary());
		
		// PG??
		
		if (null != rec.getPredictedMedianInsertSize())
			sb.append(StringUtils.getTabAndString("PI:")).append(rec.getPredictedMedianInsertSize().intValue());
		if ( ! StringUtils.isNullOrEmpty(rec.getPlatform()))
			sb.append(StringUtils.getTabAndString("PL:")).append(rec.getPlatform());
		if ( ! StringUtils.isNullOrEmpty(rec.getPlatformUnit()))
			sb.append(StringUtils.getTabAndString("PU:")).append(rec.getPlatformUnit());
		if ( ! StringUtils.isNullOrEmpty(rec.getSample()))
			sb.append(StringUtils.getTabAndString("SM:")).append(rec.getSample());
		
		return sb.toString();
	}

}
