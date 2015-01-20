package org.qcmg.common.vcf.header;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;

public class QPG extends VcfHeaderRecord implements Comparable<QPG> {
	
	public static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final String ORDER = "ORDER";
	public static final String TOOL = "TOOL";

	public static final String VERSION = "TVER";
	public static final String DATE = "DATE";
	public static final String COMMAND_LINE = "CL";
	public static final String QPG_PREFIX = "##qPG";
	
	private final int order;
	private final String tool;
	private final String version;
	private final String date;
	private final String commandLine;
	
	public QPG(String line) {
		super(null);
		if (StringUtils.isNullOrEmpty(line)) {
			throw new IllegalArgumentException("null or empty string passed to QPG ctor");
		}
		
		// if line doesn't start with "##qPG" bomb
		if ( ! line.startsWith(QPG_PREFIX)) {
			throw new IllegalArgumentException("string passed to QPG ctor doesn't start with " + QPG_PREFIX + " : " + line);
		}
		
		final int start = line.indexOf('<');
		final int end = line.lastIndexOf('>');
		
		if (start == -1 || end == -1) {
			throw new IllegalArgumentException("string passed to QPG ctor doesn't contain < and >  : " + line);
		}
		
		String [] params = line.substring(start + 1, end).split(Constants.COMMA_STRING);
		if (null == params || params.length == 0) {
			throw new IllegalArgumentException("string passed to QPG ctor doesn't contain < and >  : " + line);
		}
		order =  Integer.parseInt(getStringFromArray(params, ORDER).substring(ORDER.length() + 1));
		tool =  getStringFromArray(params, TOOL).substring(TOOL.length() + 1);
		version =  getStringFromArray(params, VERSION).substring(VERSION.length() + 1);
		date =  getStringFromArray(params, DATE).substring(DATE.length() + 1);
		commandLine =  getStringFromArray(params, COMMAND_LINE).substring(COMMAND_LINE.length() + 1);
		
	}
	
	private static String getStringFromArray(String [] array, String string) {
		for (String arr : array) {
			if (arr.startsWith(string)) return arr;
		}
		return null;
	}
	public QPG(int order, String tool, String version, String cl) {
		super(null);
		this.order = order;
		this.tool = tool;
		this.version = version;
		this.date = DF.format(new Date());
		this.commandLine = cl;
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
	
	public String getISODate() {
		return date;
	}
	
	public String getCommandLine() {
		return commandLine;
	}
	


	@Override
	public String toString() {
		return "##qPG=<" + ORDER + Constants.EQ + order + 
				Constants.COMMA + TOOL + Constants.EQ + tool +
				Constants.COMMA + VERSION + Constants.EQ + version +
				Constants.COMMA + DATE + Constants.EQ + date +
				Constants.COMMA + COMMAND_LINE + Constants.EQ + commandLine +
				">\n";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((commandLine == null) ? 0 : commandLine.hashCode());
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + order;
		result = prime * result + ((tool == null) ? 0 : tool.hashCode());
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
		QPG other = (QPG) obj;
		if (commandLine == null) {
			if (other.commandLine != null)
				return false;
		} else if (!commandLine.equals(other.commandLine))
			return false;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (order != other.order)
			return false;
		if (tool == null) {
			if (other.tool != null)
				return false;
		} else if (!tool.equals(other.tool))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public int compareTo(QPG arg0) {
		return Integer.compare(arg0.order, this.order);
	}

}
