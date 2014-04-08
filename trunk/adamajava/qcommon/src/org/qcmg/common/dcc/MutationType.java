/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.dcc;

public enum MutationType {
	SNP,
	DNP,		// di-nucleotide polymorphism - analogous to SNP but for 2 consecutive nucleotides
	TNP,		// tri-nucleotide polymorphism - analogous to DNP but for 3 consecutive nucleotides
	ONP,		// oligo-nucleotide polymorphism - analogous to TNP but for consecutive runs of 4 or more.
	INS,
	DEL;
	
	
	public static boolean isIndel(MutationType type) {
		return type == INS || type == DEL;
	}
	
	public static boolean isSubstitution(MutationType type) {
		return type == SNP || type == DNP || type == TNP || type == ONP;
	}
	
	public static boolean isMultiBaseSubstitution(MutationType type) {
		return type == DNP || type == TNP || type == ONP;
	}
	
	public static MutationType getMutationType(String type) {
		if (null == type) throw new IllegalArgumentException("null type passed to getMutationType");
		
		switch (type.toUpperCase()) {
		case "SNP": return SNP;
		case "DNP": return DNP;
		case "TNP": return TNP;
		case "ONP": return ONP;
		case "INS": return INS;
		case "DEL": return DEL;
		}
		throw new IllegalArgumentException("invalid type passed to getMutationType: " + type);
	}
}

