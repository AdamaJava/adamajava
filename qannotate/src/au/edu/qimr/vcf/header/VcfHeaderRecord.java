package au.edu.qimr.vcf.header;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.vcf.VCFHeaderUtils;

import ca.mcgill.mcb.pcingola.genBank.Feature.Type;

public class VcfHeaderRecord {
	String line = null;
	MetaType type = null;
	
	VcfHeaderRecord record = null;	
	String id = null;
	String description = null;	
	public enum MetaType {
		FORMAT, FILTER, INFO, CHROM, META, OTHER;
		@Override
		public String toString() {
			switch (this) {
			case FORMAT:
				return VCFHeaderUtils.HEADER_LINE_FORMAT;
			case FILTER:
				return VCFHeaderUtils.HEADER_LINE_FILTER;
			case INFO:
				return VCFHeaderUtils.HEADER_LINE_INFO;
			case CHROM:
				return VCFHeaderUtils.STANDARD_FINAL_HEADER_LINE;
			}
			return "##";
		}
	}
	public VcfHeaderRecord(){}
	/**
	 * Constructor using a "##INFO" line from a VCF file
	 * @param line
	 */
	public VcfHeaderRecord(String line) {
		String Hline = line.trim();
		while (Hline.endsWith("\n"))
			Hline = Hline.substring(0, line.length() - 1);
		this.line = Hline;

		// Is this an Info line?
		if (line.toUpperCase().startsWith(MetaType.FORMAT.toString())  )  
			type = MetaType.FORMAT;
		else if (line.toUpperCase().startsWith(MetaType.FILTER.toString())  )  
			type = MetaType.FILTER;
		else if (line.toUpperCase().startsWith(MetaType.INFO.toString())  )  
			type = MetaType.INFO;
		else if (line.toUpperCase().startsWith(MetaType.CHROM.toString())  )  
			type = MetaType.CHROM;
		else{  
			int index = line.indexOf('=');
			if(index >= 0){
				type = MetaType.META; 
				id = line.substring(0, index); 
				description = line.substring(index + 1);
			}else{
				type = MetaType.OTHER;
				id = null;
				description = null;
			}
		
			getRecord();
		}
				
	}
/* 
	public VcfHeaderRecord(String id,  String description, MetaType type) {
		this.id = id;
 		this.description = description;		
		this.type = type;		
		//line = type.toString() + id + "=" + description;
		record = this;
	}
*/ 
	public String getDescription() { 	return record.description;	}

	public String getId() {		return record.id;	}
	
	public VcfHeaderRecord getRecord(){	
		if(record != null)	return record;
		
		if(type.equals(MetaType.FILTER))
			record = new VcfHeaderFilter(this.toString());
		else if(type.equals(MetaType.FORMAT))
			record = new VcfHeaderFormat(this.toString());
		else if(type.equals(MetaType.INFO))
			record =  new VcfHeaderInfo(this.toString());
		else
			record = this;
		
		return record;
	}
  
	@Override
	public String toString() {	return line;	}
}
