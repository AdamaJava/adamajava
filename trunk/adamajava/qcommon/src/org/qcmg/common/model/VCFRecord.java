/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import static org.qcmg.common.util.Constants.MISSING_DATA_STRING;
import static org.qcmg.common.util.Constants.NL;
import static org.qcmg.common.util.Constants.NULL_CHAR;
import static org.qcmg.common.util.Constants.SEMI_COLON;
import static org.qcmg.common.util.Constants.TAB;

import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VCFRecord {
	
	private static final QLogger logger = QLoggerFactory.getLogger(VCFRecord.class);
	
	private final ChrPosition chrPos;
	private final String ref;
	private String id;
	private String alt;
	private String qualString;
	private String filter;
	private String info;
//	private String format;
	private List<String> format = new ArrayList<>(4);
	
	public VCFRecord(ChrPosition cp, String id, String ref, String alt) {
		this.chrPos = cp;
		this.id = id;
		this.ref = ref;
		this.alt = alt;
		// check to see if the length of the reference is equal to the length of the ChrPosition
		if ( ! StringUtils.isNullOrEmpty(ref)) {
			int refLength = ref.length();
			int chrPosLength = chrPos.getLength();
			if (refLength != chrPosLength) {
				logger.warn("In VCFRecord constructor, ref length != chrPos length! ref: " + ref + ", chrPos: " + chrPos);
			}
		}
	}
	
	public VCFRecord(String chr, int position) {
		this(new ChrPosition(chr, position), null, null, null);
	}
	public VCFRecord(String chr, int position, String id, String ref, String alt) {
		this(new ChrPosition(chr, position), id, ref, alt);
	}
	
	public VCFRecord(String [] params) {
		this(params[0], Integer.parseInt(params[1]), params[2], params[3], params[4]);
		
		qualString = (params[5]);
		filter = (params[6]);
		addInfo(params[7]);
		int length = params.length;
		for (int i = 8 ; i < length ; i++) {
			format.add(params[i]);
		}
	}
	
	public ChrPosition getChrPosition() {
		return this.chrPos;
	}

	public String getRef() {
		return ref;
	}
	public char getRefChar() {
		int len = null != ref ? ref.length() : 0;
		if (0 == len) {
			logger.warn("Reference is empty at " + chrPos.toIGVString());
			return NULL_CHAR;
		} else if (1 == len) {
			return ref.charAt(0);
		} else {
			logger.warn("Retrieving first char from ref where ref is: " + ref + " at " + chrPos.toIGVString());
			return ref.charAt(0);
		}
	}
//	public void setRef(String ref) {
//		this.ref = ref;
//	}
	public String getAlt() {
		return alt;
	}
	public void setAlt(String alt) {
		this.alt = alt;
	}

	public void setQualString(String qualString) {
		this.qualString = qualString;
	}
	
	public void addFilter(String additionalFilter) {
		this.filter = StringUtils.addToString(this.filter, additionalFilter, SEMI_COLON);
	}
	
	public void setFilter(String filter) {
		this.filter = filter;
	}
	public String getFilter() {
		return filter;
	}
	
	public void setInfo(String info) {
		this.info = info;
	}
	public void addInfo(String additionalInfo) {
		if (StringUtils.isNullOrEmpty(info)) {
			this.info = additionalInfo;
		} else {
			// need to check that we are not duplicating info
			String [] infoParam = additionalInfo.split(Constants.SEMI_COLON_STRING);
			for (String s : infoParam) {
				info = StringUtils.addToString(info, s, SEMI_COLON);
			}
//			info += (Constants.SEMI_COLON + additionalInfo);
		}
	}
	public String getInfo() {
		return info;
	}
	
//	public void addFormatField(int position, String field) {
//		if (null == format) format = new ArrayList<String>(4);		// 1 for header, 1 for control and 1 for test
//		format.set(position, field);
//	}
	public void setFormatField(List<String> field) {
		format = field;		// 1 for header, 1 for control and 1 for test
	}
	
	public List<String> getFormatFields() {
		return format;
	}
	
	public String getChromosome() {
		return chrPos.getChromosome();
	}
	public int getPosition() {
		return chrPos.getPosition();
	}
	
	public void setId(String id) {
		this.id = id;
	}
	public String getId() {
		return id;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(chrPos.getChromosome()).append(TAB);
		builder.append(chrPos.getPosition()).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(id) ? MISSING_DATA_STRING : id).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(ref) ? MISSING_DATA_STRING : ref).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(alt) ? MISSING_DATA_STRING : alt).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(qualString) ? MISSING_DATA_STRING : qualString).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(filter) ? MISSING_DATA_STRING : filter).append(TAB);
		// add END to info field if this record spans more than 1 base
		if (chrPos.isSinglePoint()) {
			builder.append(StringUtils.isNullOrEmpty(info) ? MISSING_DATA_STRING : info);
		} else {
			builder.append(StringUtils.isNullOrEmpty(info) ? "END=" : info + ";END=").append(chrPos.getEndPosition());
		}
		if (null != format)
			for (String s : format) {
				builder.append(TAB).append(s);
			}
		builder.append(NL);
		return builder.toString();
	}

	
}
