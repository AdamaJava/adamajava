package au.edu.qimr.vcf.header;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.edu.qimr.qannotate.Utils;
import ca.mcgill.mcb.pcingola.util.Gpr;

/**
 * Represents a info elements in a VCF file
 *
 * References: http://www.1000genomes.org/wiki/Analysis/Variant%20Call%20Format/vcf-variant-call-format-version-42
 *
 * INFO fields should be described as follows (all keys are required):
 * 		##FORMAT=<ID=ID,Number=number,Type=type,Description="description">
 *
 * 		Possible Types for INFO fields are: Integer, Float, Flag, Character, and String.
 *
 * 		The Number entry is an Integer that describes the number of values that
 * 		can be included with the INFO field. For example, if the INFO field contains
 * 		a single number, then this value should be 1; if the INFO field describes a
 * 		pair of numbers, then this value should be 2 and so on. If the field has one
 * 		value per alternate allele then this value should be 'A'; if the field has
 * 		one value for each possible genotype (more relevant to the FORMAT tags) then
 * 		this value should be 'G'.  If the number of possible values varies, is unknown,
 * 		or is unbounded, then this value should be '.'. The 'Flag' type indicates that
 * 		the INFO field does not contain a Value entry, and hence the Number should be 0 in
 * 		this case. The Description value must be surrounded by double-quotes. Double-quote
 * 		character can be escaped with backslash (\") and backslash as \\.
 *
 * @author Christina Xu
 */
public class VcfHeaderFormat extends VcfHeaderRecord{

	/**
	 * Number of values in an FORMAT field.
	 * Reference
	 * 		http://samtools.github.io/hts-specs/VCFv4.2.pdf
	 *
	 * Number of items in an FORMAT field. The Number entry is an Integer that describes the number of values that can be
	 * included with the INFO field. For example, if the INFO field contains
	 * a single number, then this value should be 1; if the INFO field describes a pair of numbers, then this value should
	 * be 2 and so on. There are also certain special characters used to define special cases:
	 * 		- If the field has one value per alternate allele then this value should be `A'.
	 * 		- If the field has one value for each possible allele (including the reference), then this value should be `R'.
	 * 		- If the field has one value for each possible genotype (more relevant to the FORMAT tags) then this value should be `G'.
	 * 		- If the number of possible values varies, is unknown, or is unbounded, then this value should be `.'.
	 * The `Flag' type indicates that the INFO field does not contain a Value entry, and hence the Number should be 0 in this case.
	 */
	public enum VcfInfoNumber {
		NUMBER, UNLIMITED, ALLELE, ALL_ALLELES, GENOTYPE;

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
	

 	VcfInfoNumber vcfInfoNumber;
	VcfInfoType vcfInfoType;
	int number;


	/**
	 * Constructor using a "##INFO" line from a VCF file
	 * @param line
	 */
	public VcfHeaderFormat(String line) {
		super(line);
		// Is this an Info line?
		if (line.startsWith(MetaType.FORMAT.toString())) {
			
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

		} else throw new RuntimeException("Line provided is not an FORMAT definition: '" + line + "'");
	}


	public VcfInfoType getVcfInfoType() {
		return vcfInfoType;
	}
	public String getNumber() {
		return "" + (number >= 0 ? number : vcfInfoNumber);
	}

	public boolean isNumberAllAlleles() {
		return vcfInfoNumber == VcfInfoNumber.ALL_ALLELES;
	}

	public boolean isNumberNumber() {
		return vcfInfoNumber == VcfInfoNumber.NUMBER;
	}

	public boolean isNumberOnePerAllele() {
		return vcfInfoNumber == VcfInfoNumber.ALLELE;
	}

	public boolean isNumberOnePerGenotype() {
		return vcfInfoNumber == VcfInfoNumber.GENOTYPE;
	}

	public boolean isNumberPerAllele() {
		return vcfInfoNumber == VcfInfoNumber.ALLELE || vcfInfoNumber == VcfInfoNumber.ALL_ALLELES;
	}

	void parseNumber(String number) {
		// Parse number field
		if (number.equals("A")) vcfInfoNumber = VcfInfoNumber.ALLELE;
		else if (number.equals("R")) vcfInfoNumber = VcfInfoNumber.ALL_ALLELES;
		else if (number.equals("G")) vcfInfoNumber = VcfInfoNumber.GENOTYPE;
		else if (number.equals(".")) vcfInfoNumber = VcfInfoNumber.UNLIMITED;
		else {
			vcfInfoNumber = VcfInfoNumber.NUMBER;
			this.number = Utils.parseIntSafe(number);
		}
	}

	@Override
	public String toString() {
		if (line != null) return line;

		return type.toString() + "=<ID=" + id//
				+ ",Number=" + (number >= 0 ? number : vcfInfoNumber) //
				+ ",Type=" + vcfInfoType //
				+ ",Description=\"" + description + "\"" //
				+ ">" //
		;
	}
	

	
	
}
