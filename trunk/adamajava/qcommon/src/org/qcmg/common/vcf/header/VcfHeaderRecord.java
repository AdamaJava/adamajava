package org.qcmg.common.vcf.header;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfHeaderRecord {
	public static final String ID = "ID";

	public static final String NUMBER = "Number";
	public static final String TYPE = "Type";	
	public static final String DESCRIPTION = "Description";	
	public static final String SOURCE = "Source";
	public static final String VERSION = "Version";	


	static final Pattern pattern_description = Pattern.compile(DESCRIPTION + "=\\\"(.+)\\\"");
//	static final Pattern pattern_number = Pattern.compile(NUMBER + "=([^,]+),");
//	static final Pattern pattern_type = Pattern.compile(TYPE + "=([^,]+),");
//	static final Pattern pattern_source = Pattern.compile(SOURCE + "=\\\"(.+)\\\"");
//	static final Pattern pattern_version = Pattern.compile(VERSION + "=\\\"(.+)\\\"");	
//	static final Pattern pattern_id = Pattern.compile(ID + "=([^,]+),");		
	
	
	
	
	protected String line = null;
	public MetaType type = null;
	
	VcfHeaderRecord record = null;		
	String id = null ;
	String description  = null;		
 	VcfInfoNumber vcfInfoNumber;
	VcfInfoType vcfInfoType;
	int number = -1;
	String source = null;
	String version = null;
	
	public enum VcfInfoNumber {
		NUMBER, UNLIMITED, ALLELE, ALL_ALLELES, GENOTYPE,UNKNOWN;

		@Override
		public String toString() {
			switch (this) {
			case NUMBER:
				return "";
			case ALLELE:
				return "A";
			case ALL_ALLELES:
				return "R";
			case GENOTYPE:
				return "G";
			case UNKNOWN:
			case UNLIMITED:
				return ".";	
			default:
				throw new IllegalArgumentException("Unimplemented method for type " + this);
			}
		}
	}
	
	public enum VcfInfoType {

		UNKNOWN, String, Integer, Float, Flag, Character;

		public static VcfInfoType parse(String str) {
			if(StringUtils.isNullOrEmpty(str))
				return null;
			
			str = str.toUpperCase();
			if (str.equals("STRING")) return VcfInfoType.String;
			if (str.equals("INTEGER")) return VcfInfoType.Integer;
			if (str.equals("FLOAT")) return VcfInfoType.Float;
			if (str.equals("FLAG")) return VcfInfoType.Flag;
			if (str.equals("CHARACTER")) return VcfInfoType.Character;
			if (str.equals("UNKNOWN")) return VcfInfoType.UNKNOWN;
			throw new RuntimeException("Unknown VcfInfoType '" + str + "'");
		}
	} 	
	
	public enum MetaType {
		FORMAT, FILTER, INFO,QPG, CHROM, META, OTHER;
		@Override
		public String toString() {
			switch (this) {
			case FORMAT:
				return VcfHeaderUtils.HEADER_LINE_FORMAT + "=";
			case FILTER:
				return VcfHeaderUtils.HEADER_LINE_FILTER + "=";
			case INFO:
				return VcfHeaderUtils.HEADER_LINE_INFO + "=";
			case QPG:
				return VcfHeaderUtils.HEADER_LINE_QPG + "=";
			case CHROM:
				return VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE;
			case META:
			case OTHER:
			default:
				return "##";
			}
		}
	}
		

	/**
	 * Constructor using a "##INFO" line from a VCF file
	 * @param line
	 */
	public VcfHeaderRecord(String line)  {
	
		if(line == null) return;
		
		String Hline = line.trim();
		while (Hline.endsWith("\n"))
			Hline = Hline.substring(0, line.length() - 1);
		this.line = Hline;

		// Is this an Info line?
		if (line.toUpperCase().startsWith(MetaType.FORMAT.toString()) )  
			type = MetaType.FORMAT;	 
		else if (line.toUpperCase().startsWith(MetaType.FILTER.toString()) )  
			type = MetaType.FILTER;		 
		else if (line.toUpperCase().startsWith(MetaType.INFO.toString()) )  
			type = MetaType.INFO;
		else if (line.toUpperCase().startsWith(MetaType.QPG.toString().toUpperCase()) )  
			type = MetaType.QPG;		
		else if (line.toUpperCase().startsWith(MetaType.CHROM.toString())  ) 
			type = MetaType.CHROM;
		else{  
			if( ! line.startsWith(MetaType.OTHER.toString())) {
				throw new IllegalArgumentException("can't convert String into VcfHeaderRecord since missing \"##\" at the begin of line: " + line);
			}
			
			final int index = line.indexOf('=');
			if(index >= 0){
				type = MetaType.META; 
				id = line.substring(0, index); 
				description = line.substring(index + 1);
			}else
				type = MetaType.OTHER;				
		}
		
	}
	public String getDescription() { 
		return parseRecord().description;
	}
	public String getId() {
		return parseRecord().id;
	}
	public String getSource()  {
		return source; 
	}
	public String getVersion()  {
		return version; 
	}
	
	public VcfHeaderRecord parseRecord() {			
		if(record != null)	return record;

		switch (type) {
			case FORMAT:
				record = new VcfHeaderFormat(line);
				break;
			case FILTER:
				record = new VcfHeaderFilter(line);
				break;
			case INFO:
				record =  new VcfHeaderInfo(line);
				break;
			case QPG:
				record =  new VcfHeaderQPG(line);
				break;
		
			default:
				record = this;
				break;
		}
	
		return record;
	}
  
	
	public MetaType getMetaType() {
		return parseRecord().type;	
	}
	
	public VcfInfoType getVcfInfoType()  { 
		return parseRecord().vcfInfoType; 
	}
	
	public String getNumber() {
		return (number >= 0 ? number + "": vcfInfoNumber.toString());
	}
	

	public void parseLine(String line){
		
		if(!type.equals(MetaType.FILTER) && !type.equals(MetaType.FORMAT) && ! type.equals(MetaType.INFO) && ! type.equals(MetaType.QPG))
			return ;
		
		final int start = line.indexOf('<');
		final int end = line.lastIndexOf('>');
		
		if (start == -1 || end == -1) {
			throw new IllegalArgumentException("string passed to QPG ctor doesn't contain < and >  : " + line);
		}
		
		
		final String params = line.substring(start + 1, end);
		
		// Find description	 
		final Matcher matcher = pattern_description.matcher(params);
		if (matcher.find()) description = matcher.group(1);
		
//		start = params.indexOf(DESCRIPTION);
//		end = params.lastIndexOf("\"");
		
		//do rest string without \"
//		params = params.replace(DESCRIPTION + "=\\\"(.+)\\\"", "");
		final String[] elements = params.replace(DESCRIPTION + "=\"" + description +  "\"", "").split(Constants.COMMA_STRING); 
		
		id = getStringValueFromArray(elements, ID, Constants.EQ_STRING);
		parseNumber(getStringValueFromArray(elements, NUMBER, Constants.EQ_STRING));
		if( getStringValueFromArray(elements, TYPE, Constants.EQ_STRING) != null)
			vcfInfoType = VcfInfoType.parse(getStringValueFromArray(elements, TYPE, Constants.EQ_STRING).toUpperCase());
		source  = getStringValueFromArray(elements, SOURCE, Constants.EQ_STRING);
		version = getStringValueFromArray(elements, VERSION, Constants.EQ_STRING);

		// Find ID
/*		 matcher = pattern_id.matcher(params);
		if (matcher.find()) id = matcher.group(1);
		else throw new RuntimeException("Cannot find 'ID' in info line: '" + line + "'");

		// Find and parse 'Number'
		number = -1;
		vcfInfoNumber = VcfInfoNumber.UNLIMITED;
		matcher = pattern_number.matcher(params);
		if (matcher.find()) parseNumber(matcher.group(1));

		// Find type
		matcher = pattern_type.matcher(params);
		if (matcher.find()) vcfInfoType = VcfInfoType.parse(matcher.group(1).toUpperCase());

		
		// Find description
		matcher = pattern_source.matcher(params);
		if (matcher.find()) source = matcher.group(1);
		
		// Find description
		matcher = pattern_version.matcher(params);
		if (matcher.find()) version = matcher.group(1);*/
	}
	
	void parseNumber(String number) {
		if(StringUtils.isNullOrEmpty(number))
			return;
		
		// Parse number field
		if (number.equals("A")) vcfInfoNumber = VcfInfoNumber.ALLELE;
		else if (number.equals("R")) vcfInfoNumber = VcfInfoNumber.ALL_ALLELES;
		else if (number.equals("G")) vcfInfoNumber = VcfInfoNumber.GENOTYPE;
		else if (number.equals(".")) vcfInfoNumber = VcfInfoNumber.UNLIMITED;
		else {			
			this.number = VcfHeaderUtils.parseIntSafe(number);
			vcfInfoNumber = VcfInfoNumber.NUMBER;
		}
	}	
	@Override
	public String toString() {	
		if (line != Constants.NULL_STRING) 
			return (line.endsWith(Constants.NL_STRING))? line : line + Constants.NL  ;
		return Constants.NULL_STRING;
		
	}
	
	/**
	 * 
	 * @param array
	 * @param string: 
	 * @return 
	 */
	
	
	/**
	 * 
	 * @param array
	 * @param string: seek element contain this string 
	 * @param sep: a  separator mark, eg "="
	 * @return substring (value) of matched element after the sep
	 */
	public static String getStringValueFromArray(String [] array, String string, String sep) {
		//ignor case
		String value = null;
		for (final String arr : array) {
			if (arr.toUpperCase().startsWith(string.toUpperCase())) value = arr;
		}
		
		if(value != null)
			value = value.substring(value.indexOf(sep)+1).trim();
		
		return value;
	}
}
