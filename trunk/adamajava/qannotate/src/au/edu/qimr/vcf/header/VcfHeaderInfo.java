package au.edu.qimr.vcf.header;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VcfHeaderInfo extends VcfHeaderFormat{
	String source = null;
	String version = null;

	public VcfHeaderInfo(String line) {
		super(line);	
		// Is this an Info line?
		if (line.startsWith(MetaType.INFO.toString())) {
			
			int start = line.indexOf('<');
			int end = line.lastIndexOf('>');
			String params = line.substring(start + 1, end);

			// Find ID
			Pattern pattern = Pattern.compile("ID=([^,]+),");
			Matcher matcher = pattern.matcher(params);
			if (matcher.find()) id = matcher.group(1);
			else throw new RuntimeException("Cannot find 'ID' in info line: '" + line + "'");

			// Find and parse 'Number'
			number = -1;
			vcfInfoNumber = VcfInfoNumber.UNLIMITED;
			pattern = Pattern.compile("Number=([^,]+),");
			matcher = pattern.matcher(params);
			if (matcher.find()) parseNumber(matcher.group(1));
			else throw new RuntimeException("Cannot find 'Number' in info line: '" + line + "'");

			// Find type
			pattern = Pattern.compile("Type=([^,]+),");
			matcher = pattern.matcher(params);
			if (matcher.find()) vcfInfoType = VcfInfoType.parse(matcher.group(1).toUpperCase());
			else throw new RuntimeException("Cannot find 'Type' in info line: '" + line + "'");

			// Find description
			pattern = Pattern.compile("Description=\\\"(.+)\\\"");
			matcher = pattern.matcher(params);
			if (matcher.find()) description = matcher.group(1);
			else throw new RuntimeException("Cannot find 'Description' in format line: '" + line + "'");
			
			// Find description
			pattern = Pattern.compile("Source=\\\"(.+)\\\"");
			matcher = pattern.matcher(params);
			if (matcher.find()) source = matcher.group(1);
			
			// Find description
			pattern = Pattern.compile("Version=\\\"(.+)\\\"");
			matcher = pattern.matcher(params);
			if (matcher.find()) version = matcher.group(1);

		} else throw new RuntimeException("Line provided is not an FORMAT definition: '" + line + "'");

		
	}

	
	@Override
	public String toString() {
		if (line != null) return line;

		return type + "<ID=" + id//
				+ ",Number=" + (number >= 0 ? number : vcfInfoNumber) //
				+ ",Type=" + vcfInfoType //
				+ ",Description=\"" + description + "\"" //
				+ (source == null ? "" : ",Source=\"" + source + "\"" )//
				+ (version == null ? "" : ",Version=\"" + version + "\"" )//
				+ ">" //
		;
	}
	

	
	
}
