/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf;

import static org.qcmg.common.util.Constants.MISSING_DATA_STRING;
import static org.qcmg.common.util.Constants.NL;
import static org.qcmg.common.util.Constants.NULL_CHAR;
import static org.qcmg.common.util.Constants.SEMI_COLON;
import static org.qcmg.common.util.Constants.TAB;

import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfRecord {
	
	private static final QLogger logger = QLoggerFactory.getLogger(VcfRecord.class);
	
	private final ChrPosition chrPos;
	private final String ref;
	private String id;
	private String alt;
	private String qualString;
	private String filter;
	private VcfInfoFieldRecord infoRecord;
	private final List<String> formatRecords = new ArrayList<String>(4);
//	private final List<VcfFormatFieldRecord> formatRecords = new ArrayList<VcfFormatFieldRecord>();
	
	public VcfRecord(ChrPosition cp, String id, String ref, String alt) {
		this.chrPos = cp;
		this.id = id;
		this.ref = ref;
		this.alt = alt;
		// check to see if the length of the reference is equal to the length of the ChrPosition
		if ( ! StringUtils.isNullOrEmpty(ref)) {
			final int refLength = ref.length();
			final int chrPosLength = chrPos.getLength();
			if (refLength != chrPosLength) {
				logger.warn("In VCFRecord constructor, ref length != chrPos length! ref: " + ref + ", chrPos: " + chrPos);
			}
		}
	}
	
	public VcfRecord(String chr, int position) {
		this(new ChrPosition(chr, position), null, null, null);
	}
	public VcfRecord(String chr, int position, String id, String ref, String alt) {
		this(new ChrPosition(chr, position), id, ref, alt);
	}
	
	public VcfRecord(String chr, int position, int end, String id, String ref, String alt) {
		this(new ChrPosition(chr, position, end), id, ref, alt);
	}
	
	public VcfRecord(String [] params) {
		this(params[0], Integer.parseInt(params[1]), Integer.parseInt(params[1]) + params[3].length() - 1, params[2], params[3], params[4]);			
		
		qualString = (params.length >= 6) ?  params[5] : null ;
		filter = (params.length >= 7) ? params[6] : null;
		infoRecord = (params.length >= 8) ?  new VcfInfoFieldRecord(params[7]): null;
		
		for (int i = 8; i < params.length; i ++) {
			formatRecords.add( params[i]);
		}
//		for (int i = 9; i < params.length; i ++) {
//			formatRecords.add(new VcfFormatFieldRecord(params[8], params[i]));
//		}
	}
	
	public ChrPosition getChrPosition() {		
		return this.chrPos;	
	}

	public String getRef() {	
		return ref;	
	}
	
	public char getRefChar() {
		final int len = null != ref ? ref.length() : 0;
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
 
	public String getAlt() { return alt; }
	public void setAlt(String alt) { this.alt = alt; }

	public void setQualString(String qualString) { this.qualString = qualString; }
	
	public void addFilter(String additionalFilter) { 
		this.filter = StringUtils.addToString(this.filter, additionalFilter, SEMI_COLON);
	}
	
	public void setFilter(String filter) { this.filter = filter; }
	public String getFilter() { return filter; }
	
	/**
	 * the existing INFO column value will be replaced by this new info string
	 * @param info INFO column value. eg. SOMATIC:RSPOS=100:END=102
	 * @throws Exception If the info String didn't follow pattern : <key>=<data> joined by ';'
	 */
	public void setInfo(String info) {
		if ( ! StringUtils.isNullOrEmpty(info)) {
			this.infoRecord = new VcfInfoFieldRecord(info);
		}
	}
	
	/**
	 * append additional info record into info column, however existing sub string data will be replace for same key.
	 * @param additionalInfo: eg. RSPOS=99;END=100;
	 * @throws Exception if sub string of additionalInfo split by ';',  didn't follow pattern: <key>=<data>
	 */
	public void appendInfo(String additionalInfo) {
		if( StringUtils.isNullOrEmpty( additionalInfo ))
			return;
		
		if (infoRecord == null) {
			infoRecord = new VcfInfoFieldRecord(additionalInfo);
		} else {
			// need to check that we are not duplicating info
			final String [] infoParam = additionalInfo.split(Constants.SEMI_COLON_STRING);
			for (final String s : infoParam) {
				if(  ! s.contains(Constants.EQ_STRING)) {
						infoRecord.setField(s,null);
				} else {
					final String key = s.substring(0, s.indexOf(Constants.EQ));
					final String data = s.substring(s.indexOf(Constants.EQ) +1 );
				    if (key.isEmpty() || data.isEmpty()) {
				    		throw new IllegalArgumentException("Sub INFO string didn't follow format <key>=<data>:" + s);
				    }
				    infoRecord.setField(key, data);					 
				}
			}
		}
	}
	public String getInfo() { 	return (infoRecord == null)? Constants.MISSING_DATA_STRING: infoRecord.toString(); }	
	public VcfInfoFieldRecord getInfoRecord() { return infoRecord; }
	
//	public void addFormatField(int position, String field) {
//		if (null == format) format = new ArrayList<String>(4);		// 1 for header, 1 for control and 1 for test
//		format.set(position, field);
//	}
	
	/**
	 * add/replace new format fields, it will wipe off old format column data if exits
	 * @param field: a list of format string start with FORMAT string. Empty FORMAT and Sample columns if field is null.
	 * @throws Exception if list size smaller than two 
	 */
	public void setFormatFields(List<String> field) {
		if(field == null) {
			formatRecords.clear();
			return;
		}
		
		if (field.size() == 1) {
			throw new IllegalArgumentException("missing sample column information");
		}
		
		formatRecords.clear();
		formatRecords.addAll(field);
//		for(int i = 1; i < field.size(); i ++) {
//			formatRecords.add(new VcfFormatFieldRecord(field.get(0), field.get(i)));
//		}
	}
	
	/**
	 * 
	 * @param index: the column number of sample. eg. 1 means the first column after "FORMAT" column
	 * @return a VcfFormatFieldRecord for specified sample 
	 */
	public VcfFormatFieldRecord getSampleFormatRecord(int index){
		String s = (index > formatRecords.size())? null: formatRecords.get(index);
		return new VcfFormatFieldRecord(formatRecords.get(0), s);
//		return (index > formatRecords.size())? null: formatRecords.get(index-1);		
	}
	
	
	/**
	 * 
	 * @return the first element is value of FORMAT column: eg. GT:GD:AC
	 * the second  and third element are values of normal and tumor column: eg. 0/1:A/G:A15[38.93],15[38.67],G1[39],1[39]
	 */
	public List<String> getFormatFields() {
		// return a copy of this
		if( formatRecords.size() == 0 ) return null;		
		return new ArrayList<String>(formatRecords);
//		return formatRecords;
		
//		
//		final List<String> list = new ArrayList<String>();
//		list.add( formatRecords.get(0) );
////		list.add( formatRecords.get(0).getFormatColumnString() );
//				 
//		for(int i = 0; i < formatRecords.size(); i ++) {
//			list.add( formatRecords.get(i).toString());
//		}
////		for(int i = 0; i < formatRecords.size(); i ++) {
////			list.add( formatRecords.get(i).toString());
////		}
//		
//		return list;
	}
	
	public String getFormatFieldStrings(){ 
		if(formatRecords.size() == 0 ) return Constants.EMPTY_STRING;		
		
//		String str =  formatRecords.get(0);
//		String str =  formatRecords.get(0).getFormatColumnString();
		
		StringBuilder sb = new StringBuilder();
		for (String s : formatRecords) {
			if (sb.length() > 0) {
				sb.append(Constants.TAB);
			}
			sb.append(s);
		}
//		for(int i = 0; i < formatRecords.size(); i ++) {
//			str += Constants.TAB +  formatRecords.get(i).toString();
//		}
		
		return sb.toString();	
	}
	
	public String getChromosome() { 	return chrPos.getChromosome(); }
	public int getPosition() { 	return chrPos.getPosition(); }
	
	public void setId(String id) { this.id = id; }
	public String getId() { 	return id; }
	 
	@Override
	public String toString(){
		
		//add END position into info column for compound SNP
		if (! chrPos.isSinglePoint())
			try {
				appendInfo("END=" + chrPos.getEndPosition()  );
			} catch (final Exception e) {
				// This exception shouldn't happen
				e.printStackTrace();
			}
		
		final StringBuilder builder = new StringBuilder();
		builder.append(chrPos.getChromosome()).append(TAB);
		builder.append(chrPos.getPosition()).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(id) ? MISSING_DATA_STRING : id).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(ref) ? MISSING_DATA_STRING : ref).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(alt) ? MISSING_DATA_STRING : alt).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(qualString) ? MISSING_DATA_STRING : qualString).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(filter) ? MISSING_DATA_STRING : filter).append(TAB);
		// add END to info field if this record spans more than 1 base
		builder.append( (infoRecord == null) ? MISSING_DATA_STRING : getInfo()).append(TAB);
		builder.append( getFormatFieldStrings() );
		builder.append(NL);
		return builder.toString();
	}

	
}
