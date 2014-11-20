package au.edu.qimr.vcf.header;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.mcgill.mcb.pcingola.util.Gpr;

/**
 * Represents a info elements in a VCF file
 *
 * References: http://www.1000genomes.org/wiki/Analysis/Variant%20Call%20Format/vcf-variant-call-format-version-42
 *
 * FILTER fields should be described as follows (all keys are required):
 * 		##FILTER=<ID=ID,Description="description">
 *
 * @author Christina Xu
 */
public class VcfHeaderFilter extends VcfHeaderRecord{
 
	/**
	 * Constructor using a "##FILTER" line from a VCF file
	 * @param line
	 */

	public VcfHeaderFilter(String line) {
		super(line);
		// Is this an Info line?
		if (line.startsWith(MetaType.FILTER.toString())  ) {
			int start = line.indexOf('<');
			int end = line.lastIndexOf('>');
			String params = line.substring(start + 1, end);

			// Find ID
			Pattern pattern = Pattern.compile("ID=([^,]+),");
			Matcher matcher = pattern.matcher(params);
			if (matcher.find()) id = matcher.group(1);
			else throw new RuntimeException("Cannot find 'ID' in info line: '" + line + "'");

			// Find description
			pattern = Pattern.compile("Description=\\\"(.+)\\\"");
			matcher = pattern.matcher(params);
			if (matcher.find()) description = matcher.group(1);
			else throw new RuntimeException("Cannot find 'Description' in info line: '" + line + "'");

		} else throw new RuntimeException("Line provided is not an FILTER definition: '" + line + "'");
	}

	
	public VcfHeaderFilter(String id, String description){ 
		 super();
		 this.id = id;
		 this.description = description;
	}
	
	@Override
	public String toString() {
		if (line != null) return line;

		return  type.toString() +  "=<ID=" + id//
 				+ ",Description=\"" + description + "\"" //
				+ ">" //
		;
	}
}
