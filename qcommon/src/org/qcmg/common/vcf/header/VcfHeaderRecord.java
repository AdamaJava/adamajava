package org.qcmg.common.vcf.header;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.util.Constants;

public class VcfHeaderRecord {
	static final Pattern pattern_id = Pattern.compile("ID=([^,]+),");	
	static final Pattern pattern_description = Pattern.compile("Description=\\\"(.+)\\\"");
	static final Pattern pattern_number = Pattern.compile("Number=([^,]+),");
	static final Pattern pattern_type = Pattern.compile("Type=([^,]+),");
	static final Pattern pattern_source = Pattern.compile("Source=\\\"(.+)\\\"");
	static final Pattern pattern_version = Pattern.compile("Version=\\\"(.+)\\\"");	
	
	
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
		//ZERO(0),ONE(1), TWO(2),THREE(3),FOUR(4),FIVE(5);

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
					return ".";	
				default:
					throw new RuntimeException("Unimplemented method for type " + this);
			}
		}
		

	}
	
	public enum VcfInfoType {

		UNKNOWN, String, Integer, Float, Flag, Character;

		public static VcfInfoType parse(String str) {
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
		FORMAT, FILTER, INFO, CHROM, META, OTHER;
		@Override
		public String toString() {
			switch (this) {
			case FORMAT:
				return VcfHeaderUtils.HEADER_LINE_FORMAT + "=";
			case FILTER:
				return VcfHeaderUtils.HEADER_LINE_FILTER + "=";
			case INFO:
				return VcfHeaderUtils.HEADER_LINE_INFO + "=";
			case CHROM:
				return VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE;
			}
			return "##";
		}
	}
		

	/**
	 * Constructor using a "##INFO" line from a VCF file
	 * @param line
	 * @throws Exception 
	 */
	public VcfHeaderRecord(String line) throws Exception {
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
		else if (line.toUpperCase().startsWith(MetaType.CHROM.toString())  ) 
			type = MetaType.CHROM;
		else{  
			if(!line.startsWith(MetaType.OTHER.toString())){
				throw new Exception("can't convert String into VcfHeaderRecord since missing \"##\" at the begin of line: " + line);
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
	public String getDescription() throws Exception { return parseRecord().description;	}
	public String getId() throws Exception {	return parseRecord().id; }
	public String getSource() throws Exception {	return source; }
	public String getVersion() throws Exception {	return version; }
	
	
	public VcfHeaderRecord parseRecord() throws Exception{			
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
			default:
				record = this;
				break;
		}
	
		return record;
	}
  
	
	public MetaType getMetaType() throws Exception{		return parseRecord().type;	}
	
	public VcfInfoType getVcfInfoType() throws Exception { return parseRecord().vcfInfoType; }
	
	public String getNumber() {
		return "" + (number >= 0 ? number : vcfInfoNumber.toString());
	}
	

	public void parseLine(String line){
		
		if(!type.equals(MetaType.FILTER) && !type.equals(MetaType.FORMAT) && ! type.equals(MetaType.INFO))
			return ;
		
		final int start = line.indexOf('<');
		final int end = line.lastIndexOf('>');
		final String params = line.substring(start + 1, end);

		// Find ID
		Matcher matcher = pattern_id.matcher(params);
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
		matcher = pattern_description.matcher(params);
		if (matcher.find()) description = matcher.group(1);
		
		// Find description
		matcher = pattern_source.matcher(params);
		if (matcher.find()) source = matcher.group(1);
		
		// Find description
		matcher = pattern_version.matcher(params);
		if (matcher.find()) version = matcher.group(1);
	}
	
	void parseNumber(String number) {
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
}
