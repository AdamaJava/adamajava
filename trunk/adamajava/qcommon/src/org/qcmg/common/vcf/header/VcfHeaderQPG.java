package org.qcmg.common.vcf.header;

//import static org.qcmg.common.vcf.header.VcfHeaderRecord.getStringFromArray;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class VcfHeaderQPG extends VcfHeaderRecord implements Comparable<VcfHeaderQPG> {
	
	public static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final String ORDER = "ORDER";
//	public static final String TOOL = "TOOL";
	public static final String DATE = "DATE";
//	public static final String COMMAND_LINE = "CL";
//	public static final String QPG_PREFIX = "##qPG";
	
	private final int order;
/*	private final String tool;
	private final String version;
	private final String commandLine;
*/	
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
		
		
		parseLine(line);
		
		final String [] params = line.split(Constants.COMMA_STRING);
		order =  Integer.parseInt(id);
		date =  getStringValueFromArray(params, DATE, Constants.EQ_STRING);

		//get date
		
/*		
		final int start = line.indexOf('<');
		final int end = line.lastIndexOf('>');
		
		if (start == -1 || end == -1) {
			throw new IllegalArgumentException("string passed to QPG ctor doesn't contain < and >  : " + line);
		}
		
		final String [] params = line.substring(start + 1, end).split(Constants.COMMA_STRING);
		if (null == params || params.length == 0) {
			throw new IllegalArgumentException("string passed to QPG ctor doesn't contain < and >  : " + line);
		}
		order =  Integer.parseInt(getStringFromArray(params, ORDER).substring(ORDER.length() + 1));
		tool =  getStringFromArray(params, TOOL).substring(TOOL.length() + 1);
		version =  getStringFromArray(params, VERSION).substring(VERSION.length() + 1);
		date =  getStringFromArray(params, DATE).substring(DATE.length() + 1);
		commandLine =  getStringFromArray(params, COMMAND_LINE).substring(COMMAND_LINE.length() + 1);
*/		
	}
	

	public VcfHeaderQPG(int order, String tool, String version, String cl) {
		super(null);
		
		this.order = order;
		this.id = Integer.toString(order);
		this.source = tool;
	//	this.tool = tool;
		this.version = version;
		this.date = DF.format(new Date());
		this.description = cl;
	//	this.commandLine = cl;
		
		this.type = MetaType.QPG; //type should bf line otherwise exception
		this.line = type.toString() + "<ID=" + order + 
				Constants.COMMA + DESCRIPTION + Constants.EQ + "\"" + description + "\"" +
				Constants.COMMA + VERSION + Constants.EQ + version +
				Constants.COMMA + DATE + Constants.EQ + date +
				">";
		

		record = this;
		
	}
	
	public int getOrder() {
		return order;
	}
/*	
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
	
*/	
	public String getISODate() {
		return date;
	}
	

	@Override
	public String toString() {

		
		return type.toString() + "<ID=" + order + 
				Constants.COMMA + DESCRIPTION + Constants.EQ + "\"" + description + "\"" +
				Constants.COMMA + SOURCE + Constants.EQ + source +
				Constants.COMMA + VERSION + Constants.EQ + version +
				Constants.COMMA + DATE + Constants.EQ + date +
				">" + Constants.NL;  
				
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
