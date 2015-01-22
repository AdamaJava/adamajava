package org.qcmg.common.vcf.header;

//import static org.qcmg.common.vcf.header.VcfHeaderRecord.getStringFromArray;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.header.VcfHeaderRecord.MetaType;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoType;

public class VcfHeaderQPG extends VcfHeaderRecord implements Comparable<VcfHeaderQPG> {
	
	
//	public static final String ORDER = "ORDER";
	public static final String TOOL = "Tool";
	public static final String DATE = "Date";
	public static final String COMMAND_LINE = "CL";
//	public static final String QPG_PREFIX = "##qPG";
	
	
	public static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static final Pattern pattern_CL = Pattern.compile(COMMAND_LINE + "=\\\"(.+)\\\"");
	
	private final int order;
 	private final String tool;
//	private final String version;
	private String commandLine;
 	
	private final String date;

	
	public VcfHeaderQPG(String line) {
		super(line);
		
		if (StringUtils.isNullOrEmpty(line)) {
			throw new IllegalArgumentException("null or empty string passed to QPG ctor");
		}
		
		// if line doesn't start with "##qPG" bomb
		if ( ! line.startsWith(VcfHeaderUtils.HEADER_LINE_QPG)) {
			throw new IllegalArgumentException("string passed to QPG ctor doesn't start with " + VcfHeaderUtils.HEADER_LINE_QPG + " : " + line);
		}
		
		
		//parse line		
 		final int start = line.indexOf('<');
		final int end = line.lastIndexOf('>');
		
		if (start == -1 || end == -1)  
			throw new IllegalArgumentException("string passed to QPG ctor doesn't contain < and >  : " + line);	
		
		final String params = line.substring(start + 1, end);
		
		// Find description	 
		final Matcher matcher = pattern_CL.matcher(params);
		if (matcher.find()) commandLine = matcher.group(1);

		
		//do rest string without \"
		final String[] elements = params.replace(COMMAND_LINE + "=\"" + commandLine +  "\"", "").split(Constants.COMMA_STRING); 
		
		id = getStringValueFromArray(elements, ID, Constants.EQ_STRING);
		order =  Integer.parseInt(id);		
		tool  = getStringValueFromArray(elements, TOOL, Constants.EQ_STRING);
		version = getStringValueFromArray(elements, VERSION, Constants.EQ_STRING);
		date =  getStringValueFromArray(elements, DATE, Constants.EQ_STRING);
		
		line = toString();
		this.record = this;
		
	}
	

	public VcfHeaderQPG(int order, String tool, String version, String cl) {
		super(null);
		
		this.order = order;
		this.id = Integer.toString(order);
		this.tool = tool;
		this.version = version;
		this.date = DF.format(new Date());
		this.commandLine = cl;
		
		this.type = MetaType.QPG; //type should bf line otherwise exception
		this.line = toString();		
		this.record = this;
		
	}
	
	public int getOrder() {
		return order;
	}
	
	public String getTool() {
		return tool;
	}
	
	@Override
	public String getVersion() {
		return version;
	}
	
	public String getCommandLine() {
		return commandLine;
	}
	
	public String getISODate() {
		return date;
	}
	

	@Override
	public String toString() {		
		return this.line = type.toString() + "<ID=" + order + 
				Constants.COMMA + TOOL + Constants.EQ + tool +				
				Constants.COMMA + VERSION + Constants.EQ + version +
				Constants.COMMA + DATE + Constants.EQ + date +
				Constants.COMMA + COMMAND_LINE + Constants.EQ + "\"" + commandLine + "\"" +
				">";  				
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + order;
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		final VcfHeaderQPG other = (VcfHeaderQPG) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (order != other.order)
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public int compareTo(VcfHeaderQPG arg0) {
		return Integer.compare(arg0.order, this.order);
	}

}
