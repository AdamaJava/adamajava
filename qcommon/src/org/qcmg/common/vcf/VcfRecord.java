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

public class VcfRecord implements Comparable<VcfRecord> {
	
	private static final QLogger logger = QLoggerFactory.getLogger(VcfRecord.class);
	
	private final ChrPosition chrPos;
	private final String ref;
	private String id;
	private String alt;
	private String qualString;
	private String filter;
	private VcfInfoFieldRecord infoRecord;
	private final List<String> formatRecords = new ArrayList<String>(4);
	
//	public VcfRecord(ChrPosition cp, String id, String ref, String alt) {
//		this.chrPos = cp;
//		this.id = id;
//		this.ref = ref;
//		this.alt = alt;
//		// check to see if the length of the reference is equal to the length of the ChrPosition
//		if ( ! StringUtils.isNullOrEmpty(ref)) {
//			if (ref.length() != chrPos.getLength()) {
//				logger.warn("In VCFRecord constructor, ref length != chrPos length! ref: " + ref + ", chrPos: " + chrPos);
//			}
//		}
//	}
	
//	public VcfRecord(String chr, int position) {
//		this(new ChrPosition(chr, position), null, null, null);
//	}
//	public VcfRecord(String chr, int position, String id, String ref, String alt) {
//		this(new ChrPosition(chr, position), id, ref, alt);
//	}
//	
//	public VcfRecord(String chr, int position, int end, String id, String ref, String alt) {
//		this(new ChrPosition(chr, position, end), id, ref, alt);
//	}
	
	/**
	 * 
	 * @param params: a string array with mimumum five element, following order: 
	 * <CHROM>, <POS>, <ID> , <REF>, <ALT>, <QUAL>, <FILTER>, <INFO>, <FORMAT> ...
	 * Here the first five element is compulsory, The reminding field will be filled with null if not provided.
	 * here CHROM, POS and REF can't be null
	 * eg. new VcfRecord(new String[] {"1", "100", null, ".", null}) 
	 *     new VcfRecord("chr1,100,.,A,T,.,PASS".split(","))
	 *     new VcfRecord("chr1\t100\t.\tA\tT\t.\tPASS\t.\tGT\tS1\tS2\tS3".split("\t"))
	 */
	public VcfRecord(String [] params) {
//		this(params[0], Integer.parseInt(params[1]), Integer.parseInt(params[1]) + params[3].length() - 1, params[2], params[3], params[4]);
		this.chrPos = new ChrPosition(params[0], Integer.parseInt(params[1]), Integer.parseInt(params[1]) + params[3].length() - 1);
		
		this.id = params[2] ;
		this.ref = params[3];
		this.alt = params[4];
		// check to see if the length of the reference is equal to the length of the ChrPosition
		if ( ! StringUtils.isNullOrEmpty(ref) && ref.length() != chrPos.getLength()) {
				logger.warn("In VCFRecord constructor, ref length != chrPos length! ref: " + ref + ", chrPos: " + chrPos);
		}		
		
		qualString = (params.length >= 6) ?  params[5] : null ;
		filter = (params.length >= 7) ? params[6] : null;
		infoRecord = (params.length >= 8) ?  new VcfInfoFieldRecord(params[7]): null;
		
		for (int i = 8; i < params.length; i ++)  {
			if (StringUtils.isNullOrEmpty(params[i])) {
				formatRecords.add( Constants.MISSING_DATA_STRING);
			} else {
				formatRecords.add( params[i]);
			}
		}
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
 
	public String getAlt() {
		return alt ;
	}
	public void setAlt(String alt) { this.alt = alt;}
	
	/**
	 * 
	 * @param otherAlt, append this allels if not exists in exisiting allel. 
	 */
	public void appendAlt(String otherAlt){
		if( StringUtils.isNullOrEmpty( otherAlt )) {
			return;
		}
		
		if (StringUtils.isNullOrEmpty(alt)) {  
			this.alt = otherAlt;
		}
		
		final String[] altParam = this.alt.split(Constants.COMMA_STRING);
		final String[] otherParm = otherAlt.split(Constants.COMMA_STRING);
		
		for(final String in : otherParm) {
			boolean isExist = false; 
			for (final String s : altParam){ 
				if(s.equalsIgnoreCase(otherAlt)){
					isExist = true;
					break; 
				}
			}
			
			//append non existed alt
			if(! isExist) {
				this.alt.concat(Constants.COMMA + in);
			}
		}
	}

	public void setQualString(String qualString) { this.qualString = qualString; }
	
	public void addFilter(String additionalFilter) { 
		if(StringUtils.isNullOrEmpty(   this.filter)  || 
				this.filter .equals(Constants.MISSING_DATA_STRING) )
			setFilter(additionalFilter);
		else
			this.filter = StringUtils.addToString(this.filter, additionalFilter, SEMI_COLON);
	}
	
	public void setFilter(String filter) { this.filter = filter; }
	
	/**
	 * 
	 * @return filter column value
	 */
	public String getFilter() { 
		
		if(! StringUtils.isNullOrEmpty(this.filter)){
			filter = filter.replace(Constants.MISSING_DATA_STRING + Constants.SEMI_COLON, "");
			filter = filter.replace(Constants.SEMI_COLON + Constants.MISSING_DATA_STRING,"");
		} 		
		
		return filter; 
	}
	
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
	public void appendInfo(String additionalInfo, boolean overwrite) {
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
				    if (overwrite) {
				    		infoRecord.setField(key, data);
				    } else {
				    		infoRecord.appendField(key, data);
				    		
				    }
				}
			}
		}
	}
	public void appendInfo(String additionalInfo) {
		appendInfo(additionalInfo, true);
	}
	public String getInfo() { 	return (infoRecord == null)? Constants.MISSING_DATA_STRING: infoRecord.toString(); }	
	public VcfInfoFieldRecord getInfoRecord() { return infoRecord; }
	
	
	/**
	 * add/replace new format fields, it will wipe off old format column data if exits
	 * @param field: a list of format string start with FORMAT string. Empty FORMAT and Sample columns if field is null.
	 */
	public void setFormatFields(List<String> field) {
		formatRecords.clear();
		
		if(field == null)  return;
	 		
		if (field.size() == 1)  
			throw new IllegalArgumentException("missing sample column information");
		 
		for (int i = 0; i < field.size(); i ++)  
			if(StringUtils.isNullOrEmpty(field.get(i)))
				formatRecords.add( Constants.MISSING_DATA_STRING);
			else
				formatRecords.add( field.get(i));
		
	 
	}
	
	/**
	 * 
	 * @param index: the column number of sample. eg. 1 means the first column after "FORMAT" column
	 * @return a VcfFormatFieldRecord for specified sample 
	 */
	public VcfFormatFieldRecord getSampleFormatRecord(int index){
		String s = (index > formatRecords.size())? null: formatRecords.get(index);
		return new VcfFormatFieldRecord(formatRecords.get(0), s);
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
		
	}
	
	public String getFormatFieldStrings(){ 
		if(formatRecords.size() == 0 ) return Constants.EMPTY_STRING;		
		
		StringBuilder sb = new StringBuilder();
		for (String s : formatRecords) {
			if (sb.length() > 0) {
				sb.append(Constants.TAB);
			}			
			sb.append(s);			
			
		}
		return sb.toString();	
	}
	
	public String getChromosome() { 	return chrPos.getChromosome(); }
	public int getPosition() { 	return chrPos.getPosition(); }
	
	public void appendId(String additionalId) {
		this.id = (StringUtils.isNullOrEmpty(this.id) || this.id.equals(Constants.MISSING_DATA_STRING)) ? additionalId : this.id + ";" + additionalId;
	}
	public void setId(String id) { this.id = id; }
	public String getId() { 	return id; }
	
	 
	@Override
	/**
	 * Join all vcf record fields into string with tab seperated. 
	 * return a vcf string trailing with a newline 
	 */
	public String toString(){
		
		//add END position into info column for compound SNP
//		if (! chrPos.isSinglePoint()) {
//				appendInfo("END=" + chrPos.getEndPosition()  );
//		}
		
		final StringBuilder builder = new StringBuilder();
		builder.append(chrPos.getChromosome()).append(TAB);
		builder.append(chrPos.getPosition()).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(id) ? MISSING_DATA_STRING : id).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(ref) ? MISSING_DATA_STRING : ref).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(alt) ? MISSING_DATA_STRING : alt).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(qualString) ? MISSING_DATA_STRING : qualString).append(TAB);
		builder.append(StringUtils.isNullOrEmpty(filter) ? MISSING_DATA_STRING : filter).append(TAB);
		builder.append( (infoRecord == null) ? MISSING_DATA_STRING : getInfo()).append(TAB);
		builder.append( getFormatFieldStrings() );
		builder.append(NL);
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alt == null) ? 0 : alt.hashCode());
		result = prime * result + ((chrPos == null) ? 0 : chrPos.hashCode());
		result = prime * result + ((ref == null) ? 0 : ref.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VcfRecord other = (VcfRecord) obj;
		if (alt == null) {
			if (other.alt != null)
				return false;
		} else if (!alt.equals(other.alt))
			return false;
		if (chrPos == null) {
			if (other.chrPos != null)
				return false;
		} else if (!chrPos.equals(other.chrPos))
			return false;
		if (ref == null) {
			if (other.ref != null)
				return false;
		} else if (!ref.equals(other.ref))
			return false;
		return true;
	}

	@Override
	public int compareTo(VcfRecord arg0) {
		return chrPos.compareTo(arg0.chrPos);
	}

	
}
