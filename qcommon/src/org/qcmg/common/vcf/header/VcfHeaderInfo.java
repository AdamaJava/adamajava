package org.qcmg.common.vcf.header;

import org.qcmg.common.util.Constants;

public final class VcfHeaderInfo extends VcfHeaderRecord{

	public VcfHeaderInfo(String line) {
		super(line);	
		 
		// Is this an Info line?
		if (line.startsWith(MetaType.INFO.toString())) {			
			 parseLine(line);
		} else throw new IllegalArgumentException("Can't create VcfHeaderInfo - line provided is not an INFO definition: '" + line + "'");
	
		record = this;

	}
 
 /**
  * it create an INFO vcf header record, eg. ##INFO=<ID=id,NUMBER=number/infoNumber,Type=infoType,Description=description,Source=source,Version=version>
  * @param id 
  * @param infoNumbe
  * @param number: this number (>=0) will show on vcf header record if infoNumber is MetaType.NUMBER
  * @param infoType
  * @param description
  * @param source: it will show on vcf header if source != null
  * @param version: it will show on vcf header if source != null
  * @throws Exception
  */
	public VcfHeaderInfo(String id, VcfInfoNumber infoNumber, int number, VcfInfoType infoType, String description, String source, String version) {

		super(null);
		this.id = id;
		vcfInfoNumber = infoNumber;
		this.number = number;
		vcfInfoType = infoType;
		this.description = description;
		this.version = version;
		this.source = source;
		 
		this.type = MetaType.INFO; //type should bf line otherwise exception
		this.line = type.toString() + "<ID=" + id//
				+ ",Number=" + (number >= 0 ? number : vcfInfoNumber.toString()) //
				+ ",Type=" + vcfInfoType.toString() //
				+ ",Description=\"" + description + "\"" //
				+ (source == null ? "" : ",Source=\"" + source + "\"" )//
				+ (version == null ? "" : ",Version=\"" + version + "\"" ) + ">" ;
		record = this;
	}
	
	@Override
	public String toString() {
		if (line != Constants.NULL_STRING) 
			return (line.endsWith(Constants.NL_STRING))? line : line + Constants.NL  ;

		return type.toString() + "<ID=" + id//
				+ ",Number=" + (number >= 0 ? number : vcfInfoNumber) //
				+ ",Type=" + vcfInfoType //
				+ ",Description=\"" + description + "\"" //
				+ (source == null ? "" : ",Source=\"" + source + "\"" )//
				+ (version == null ? "" : ",Version=\"" + version + "\"" )//
				+ ">" + Constants.NL;  
	}

	
}
