/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.string.StringUtils;

public class VCFRecord {
	
	private static final char TAB = '\t';
	private static final char NL = '\n';
	public static final char MISSING_DATA = '.';
	private static final char NULL_CHAR = '\u0000';
	private static Pattern effPattern = Pattern.compile("\\w.\\([A-Za-z].(\\|\\S.).\\)");
	
	
	private String chromosome;
	private int position;
	private String id;
	private char ref;
	private String alt;
	private String qualString;
	private String filter;
	private String info;
	private List<String> extraFields;
	
	public char getRef() {
		return ref;
	}
	public void setRef(char ref) {
		this.ref = ref;
	}
	public String getAlt() {
		return alt;
	}
	public void setAlt(String alt) {
		this.alt = alt;
	}

	public void setQualString(String qualString) {
		this.qualString = qualString;
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
	public String getInfo() {
		return info;
	}
	
	public void addExtraField(String field) {
		if (null == extraFields) extraFields = new ArrayList<String>();
		extraFields.add(field);
	}
	
	public List<String> getExtraFields() {
		return extraFields;
	}
	
	public String getChromosome() {
		return chromosome;
	}
	public void setChromosome(String chromosome) {
		this.chromosome = chromosome;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
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
		builder.append(chromosome).append(TAB);
		builder.append(position).append(TAB);
		builder.append(null == id ? MISSING_DATA : id).append(TAB);
		builder.append(NULL_CHAR == ref ? MISSING_DATA : ref).append(TAB);
		builder.append(null == alt ? MISSING_DATA : alt).append(TAB);
		builder.append(null == qualString ? MISSING_DATA : qualString).append(TAB);
		builder.append(null == filter ? MISSING_DATA : filter).append(TAB);
		builder.append(info);
		if (null != extraFields)
			for (String s : extraFields) builder.append(TAB).append(s);
		builder.append(NL);
		return builder.toString();
	}

	
}
