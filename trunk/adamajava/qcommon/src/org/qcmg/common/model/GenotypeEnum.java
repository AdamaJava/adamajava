package org.qcmg.common.model;

/**
 * Alphabetic Genotype Enum
 * 
 * @author holmes
 *
 */
public enum GenotypeEnum {

	AA('A','A'), AC('A','C'), AG('A','G'), AT('A','T'),
	CC('C','C'), CG('C','G'), CT('C','T'),
	GG('G','G'), GT('G','T'),
	TT('T','T');
	
	private char firstAllele;
	private char secondAllele;
	
	private GenotypeEnum(char c1, char c2) {
		this.firstAllele = c1;
		this.secondAllele = c2;
	}
	
	// Accessor methods
	
	public static GenotypeEnum getGenotypeEnum(char c1, char c2) {
		char firstA = c1 <= c2 ? c1 : c2;
		char secondA = c1 <= c2 ? c2 : c1;
		for (GenotypeEnum g : GenotypeEnum.values()){
			if (g.firstAllele == firstA && g.secondAllele == secondA)
				return g;
		}
		return null;
	}
	
	public char getFirstAllele() {
		return firstAllele;
	}
	
	public char getSecondAllele() {
		return secondAllele;
	}
	
	public boolean containsAllele(char reference) {
		return firstAllele == reference || secondAllele == reference;
	}
	
	public String getGTString(char reference) {
		if (isHeterozygous()) {
			if (containsAllele(reference)) {
				return "0/1";
			} else return "1/2";
		} else {
			if (containsAllele(reference)) {
				return "0/0";
			} else return "1/1";
		}
	}
	
	public boolean isHomozygous() {
		return firstAllele == secondAllele;
	}
	
	public boolean isHeterozygous() {
		return ! isHomozygous();
	}
	
	public String getDisplayString(){
		return firstAllele + "/" + secondAllele;
	}
	
	public GenotypeEnum getComplement() {
		switch (this) {
		case AA: return TT;
		case AC: return GT;
		case AG: return CT;
		case AT: return AT;
		case CC: return GG;
		case CG: return CG;
		case CT: return AG;
		case GG: return CC;
		case GT: return AC;
		case TT: return AA;
		}
		return null;
	}
	
}
