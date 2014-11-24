package org.qcmg.common.vcf.header;


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
public final class VcfHeaderFormat extends VcfHeaderRecord{

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

	public VcfHeaderFormat(String line) throws Exception {
		super(line);
		// Is this an Info line?
		if (line.startsWith(MetaType.FORMAT.toString())) {
			parseLine(line);
		} else throw new RuntimeException("Line provided is not an FORMAT definition: '" + line + "'");
		
		record = this;

	}
	
	public VcfHeaderFormat(String id, VcfInfoNumber infoNumber, VcfInfoType infoType, String description) throws Exception {
		super( null );
		this.id = id;
		vcfInfoNumber = infoNumber;
		vcfInfoType = infoType;
		this.description = description;
		type = MetaType.FORMAT;
		line = type.toString() + "=<ID=" + id//
				+ ",Number=" + (number >= 0 ? number : vcfInfoNumber) //
				+ ",Type=" + vcfInfoType //
				+ ",Description=\"" + description + "\"" + ">" ;
		
		record = this;

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
