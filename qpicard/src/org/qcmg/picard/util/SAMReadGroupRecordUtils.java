/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import java.text.SimpleDateFormat;

 
import htsjdk.samtools.SAMReadGroupRecord;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class SAMReadGroupRecordUtils {
	
	public static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	
	public static String getRGString(SAMReadGroupRecord rec) {
		if (null == rec) throw new IllegalArgumentException("null SAMReadGroupRecord passed to getRGString");
		
		StringBuilder sb = new StringBuilder(Constants.READ_GROUP_PREFIX);
		if ( ! StringUtils.isNullOrEmpty(rec.getId()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.READ_GROUP_ID_TAG)).append(Constants.COLON).append(rec.getId());
		if ( ! StringUtils.isNullOrEmpty(rec.getSequencingCenter()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.SEQUENCING_CENTER_TAG)).append(Constants.COLON).append(rec.getSequencingCenter());
		if ( ! StringUtils.isNullOrEmpty(rec.getDescription()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.DESCRIPTION_TAG)).append(Constants.COLON).append(rec.getDescription());
		if (null != (rec.getRunDate()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.DATE_RUN_PRODUCED_TAG)).append(Constants.COLON).append(sdf.format(rec.getRunDate()));
		if ( ! StringUtils.isNullOrEmpty(rec.getFlowOrder()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.FLOW_ORDER_TAG)).append(Constants.COLON).append(rec.getFlowOrder());
		if ( ! StringUtils.isNullOrEmpty(rec.getKeySequence()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.KEY_SEQUENCE_TAG)).append(Constants.COLON).append(rec.getKeySequence());
		if ( ! StringUtils.isNullOrEmpty(rec.getLibrary()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.LIBRARY_TAG)).append(Constants.COLON).append(rec.getLibrary());
		
		// PG??
		
		if (null != rec.getPredictedMedianInsertSize())
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.PREDICTED_MEDIAN_INSERT_SIZE_TAG)).append(Constants.COLON).append(rec.getPredictedMedianInsertSize().intValue());
		if ( ! StringUtils.isNullOrEmpty(rec.getPlatform()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.PLATFORM_TAG)).append(Constants.COLON).append(rec.getPlatform());
		if ( ! StringUtils.isNullOrEmpty(rec.getPlatformUnit()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.PLATFORM_UNIT_TAG)).append(Constants.COLON).append(rec.getPlatformUnit());
		if ( ! StringUtils.isNullOrEmpty(rec.getSample()))
			sb.append(StringUtils.getTabAndString(SAMReadGroupRecord.READ_GROUP_SAMPLE_TAG)).append(Constants.COLON).append(rec.getSample());
		
		return sb.toString();
	}

}
