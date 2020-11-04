/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.vcf;

import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.Classification;

public class RefAndMultiGenotype {
	
	private final char ref;
	private final GenotypeEnum normal;
	private final GenotypeEnum tumour;
	
	public RefAndMultiGenotype(char ref, GenotypeEnum normal, GenotypeEnum tumour) {
		this.ref = ref;
		this.normal = normal;
		this.tumour = tumour;
	}
	
	@Override
	public String toString() {
		return ref + " : " + normal.getDisplayString() + " : " +  tumour.getDisplayString() + " : " + getClassification();
	}
	
	public String  getClassification() {
		if (normal == tumour) {
			return Classification.GERMLINE.name();
			
		} else if (normal.isHomozygous() && tumour.isHomozygous()) {
			// not equal but both are homozygous
			return Classification.SOMATIC.name();
		} else if (normal.isHeterozygous() && tumour.isHeterozygous()) {
			// not equal but both are heterozygous
			return Classification.SOMATIC.name();
		}
		
		///////////////////////////////////////////////////////
		// normal is HOM and tumour is HET
		///////////////////////////////////////////////////////
		if (normal.isHomozygous() && tumour.isHeterozygous()) {
			
			GenotypeEnum refAndNormalGenotype = GenotypeEnum.getGenotypeEnum(ref, normal.getFirstAllele());
			
			if (tumour == refAndNormalGenotype) {
				return Classification.GERMLINE.name();
//				mutation = normal.getFirstAllele() + MUT_DELIM + record.getRef();
			} else {
				return Classification.SOMATIC.name();
			}
		}
		
		///////////////////////////////////////////////////////
		// normal is HET and tumour is HOM
		//////////////////////////////////////////////////////
		else if (normal.isHeterozygous() && tumour.isHomozygous()){
			
			if (normal.containsAllele(tumour.getFirstAllele())) {
				return Classification.GERMLINE.name();
			} else {
				return Classification.SOMATIC.name();
			}
		}
		return null;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((normal == null) ? 0 : normal.hashCode());
		result = prime * result + ref;
		result = prime * result + ((tumour == null) ? 0 : tumour.hashCode());
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
		RefAndMultiGenotype other = (RefAndMultiGenotype) obj;
		if (normal == null) {
			if (other.normal != null)
				return false;
		} else if (!normal.equals(other.normal))
			return false;
		if (ref != other.ref)
			return false;
		if (tumour == null) {
			if (other.tumour != null)
				return false;
		} else if (!tumour.equals(other.tumour))
			return false;
		return true;
	}

}
