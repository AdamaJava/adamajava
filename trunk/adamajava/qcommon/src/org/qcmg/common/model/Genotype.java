/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;


/**
 * Immutable object representing a genotype (ordered)
 * Can handle tri-allelic genotypes, but homozygosity &  heterozygosity do not currently take the third allele into account 
 * 
 * @author oholmes
 *
 */
public class Genotype {
	
	private static final String SEPARATOR = "/";
	private static final char DEFAULT_CHAR = '\u0000';

	private char firstAllele;
	private char secondAllele;
	private char thirdAllele;
	private char fourthAllele;
	
	public Genotype() {}	// default constructor
	
	/**
	 * Create ordered Genotype based on the supplied char parameters
	 * <p>
	 * @param c1 will be the first or second allele (depending on order)
	 * @param c2 will be the first or second allele (depending on order)
	 */
	public Genotype(char c1, char c2) {
		this.firstAllele = c1 <= c2 ? c1 : c2;
		this.secondAllele = c1 <= c2 ? c2 : c1;
	}
	
	/**
	 * Tri-alleleic genotype constructor
	 * Does not currently provide an ordered genotype (unless the supplied parameters are themselves ordered)
	 * <p>
	 * @param c1
	 * @param c2
	 * @param c3
	 */
	public Genotype(char c1, char c2, char c3) {
		this(c1, c2);
		this.thirdAllele = c3;
	}
	public Genotype(char c1, char c2, char c3, char c4) {
		this(c1, c2,c3);
		this.fourthAllele = c4;
	}
	
	public char getFirstAllele() {
		return firstAllele;
	}
	public char getSecondAllele() {
		return secondAllele;
	}
	public char getThirdAllele() {
		return thirdAllele;
	}
	public char getFourthAllele() {
		return fourthAllele;
	}
	
	/**
	 * Returns true if the first allele is not equal to the second allele
	 * <p>
	 * NOTE that this does not consider the third (or fourth) allele should one be present
	 * 
	 * @return boolean indicating of the genotype is heterozygous
	 */
	public boolean isHeterozygous() {
		return ! isHomozygous();
	}
	public boolean isHomozygous() {
		return firstAllele == secondAllele;
	}
	
	public String getFormattedGenotype() {
		return firstAllele + SEPARATOR + secondAllele + (DEFAULT_CHAR != thirdAllele ? SEPARATOR + thirdAllele : "")
		+ (DEFAULT_CHAR != fourthAllele ? SEPARATOR + fourthAllele : "");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + firstAllele;
		result = prime * result + fourthAllele;
		result = prime * result + secondAllele;
		result = prime * result + thirdAllele;
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
		Genotype other = (Genotype) obj;
		if (firstAllele != other.firstAllele)
			return false;
		if (fourthAllele != other.fourthAllele)
			return false;
		if (secondAllele != other.secondAllele)
			return false;
		if (thirdAllele != other.thirdAllele)
			return false;
		return true;
	}
	
}
