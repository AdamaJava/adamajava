/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.util;

import static org.qcmg.common.util.Constants.MUT_DELIM;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.common.model.Classification;
public class GenotypeComparisonUtil {
	
	
	private final static QLogger logger = QLoggerFactory.getLogger(GenotypeComparisonUtil.class);
	
	
	public static void singleSampleGenotype(QSnpRecord record) {
		if (null == record) 
			throw new IllegalArgumentException("Null pileup record passed to GenotypeComparisonUtil.compareSingleGenotype");
		
		GenotypeEnum genotype = record.getTumourGenotype();
		
		if (null == genotype) {
			logger.error("Error predicting genotype for record: " + record.getFormattedString() + ", vcf: " + record.getVcfRecord().toString());
			throw new IllegalArgumentException("Pileup record passed to GenotypeComparisonUtil.compareGenotypes has missing genotype");
		}
		
		record.setClassification(Classification.UNKNOWN);
		
		String refString = record.getRef();
		if (refString.length() > 1) {
			logger.warn("ref string: " + refString + "  has more than 1 char in it! in GenotypeComparisonUtil.singleSampleGenotype");
		}
		char reference = refString.charAt(0);
		
		if (genotype.containsAllele(reference)) {
		
			if (genotype.isHeterozygous()) {
				record.setMutation(reference + MUT_DELIM + (genotype.getFirstAllele() == reference ? genotype.getSecondAllele() : genotype.getFirstAllele()));
			} else {
				// Ignore (not expecting this to happen...)
				logger.warn("Ignoring due to homozygous and equal to ref: " + record.getFormattedString());
			}
		} else {
			if (genotype.isHeterozygous()) {
				record.setMutation(reference + MUT_DELIM + genotype.getFirstAllele() + Constants.COMMA +  genotype.getSecondAllele());
			} else {
				record.setMutation(reference + MUT_DELIM + genotype.getFirstAllele());
			}
			
		}
	}
	
	public static void compareSingleGenotype(QSnpRecord record) {
		if (null == record) 
			throw new IllegalArgumentException("Null pileup record passed to GenotypeComparisonUtil.compareSingleGenotype");
		
		GenotypeEnum normal = record.getNormalGenotype();
		GenotypeEnum tumour = record.getTumourGenotype();
		
		if (null == normal && null == tumour) {
			logger.error("Error predicting genotype for record: " + record.getFormattedString());
			throw new IllegalArgumentException("Pileup record passed to GenotypeComparisonUtil.compareGenotypes has missing genotype");
		}
		
		String refString = record.getRef();
		if (refString.length() > 1) {
			logger.warn("ref string: " + refString + "  has more than 1 char in it! in GenotypeComparisonUtil.compareSingleGenotype");
		}
		char reference = refString.charAt(0);
		
		if (null == tumour) {
			
			VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.LESS_THAN_3_READS_TUMOUR);
			
			if (normal.containsAllele(reference)) {
				if (normal.isHeterozygous()) {
					record.setClassification(Classification.GERMLINE);
					record.setMutation(reference + MUT_DELIM + (normal.getFirstAllele() == reference ? normal.getSecondAllele() : normal.getFirstAllele()));
				} else {
					// Ignore (not expecting this to happen...)
					logger.info("Ignoring (ND) due to homozygous and equal to ref: " + record.getFormattedString());
				}
			} else {
				record.setClassification(Classification.GERMLINE);
				if (normal.isHeterozygous()) {
					// het but ref is not one of the allele's - hmmmmm...
					logger.info("Het call in normal, but doesn't contain reference: " + record.getFormattedString());
				} else {
					record.setMutation(reference + MUT_DELIM + normal.getFirstAllele());
				}
			}
			
		} else {
			VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.LESS_THAN_3_READS_NORMAL);
			
			if (tumour.containsAllele(reference)) {
				
				if (tumour.isHomozygous()) {
					// Ignore (not expecting this to happen...)
					logger.info("Ignoring (TD) due to homozygous and equal to ref: " + record.getFormattedString());
				} else {
					char M = reference == tumour.getFirstAllele() ? tumour.getSecondAllele() : tumour.getFirstAllele();
					// if there is evidence of the variant in the normal - > GERMLINE
					if (record.getNormalCount() > 0 && StringUtils.isCharPresentInString(record.getNormalOABS(), M)) {
						
						record.setClassification(Classification.GERMLINE);
						record.setMutation(reference + MUT_DELIM + M);
//					} else if (record.getNormalCount() > 0) {
//						// no evidence in the normal -> SOMATIC
//						record.setClassification(Classification.SOMATIC);
//						record.setMutation(reference + "/" + M);
					} else {
						// no coverage at all in normal - ?????
						record.setClassification(Classification.SOMATIC);
						record.setMutation(reference + MUT_DELIM + M);
					}
				}
				
			} else {	// reference not present in tumour genotype
				
				if (tumour.isHomozygous()) {
					char M = tumour.getFirstAllele();
					if (record.getNormalCount() > 0 && (StringUtils.isCharPresentInString(record.getNormalOABS(), M))) {
//						if (record.getNormalCount() > 0 && (record.getNormalPileup().contains(""+M) 
//								|| record.getNormalPileup().contains(""+Character.toLowerCase(M)))) {
						
						record.setClassification(Classification.GERMLINE);
						record.setMutation(reference + MUT_DELIM + M);
//						record.addAnnotation("mutation also found in pileup of normal");
//					} else if (record.getNormalCount() > 0) {
//						// no evidence in the normal -> SOMATIC
//						record.setClassification(Classification.SOMATIC);
//						record.setMutation(reference + "/" + M);
					} else {
						// no coverage at all in normal - ?????
						record.setClassification(Classification.SOMATIC);
						record.setMutation(reference + MUT_DELIM + M);
					}
				} else {
					logger.debug("het tumour genotype, with ref not in alleles: " + record.getFormattedString());
					
					char A = tumour.getFirstAllele();
					char B = tumour.getSecondAllele();
					
					if (record.getNormalCount() > 0 
							&& StringUtils.isCharPresentInString(record.getNormalOABS(), A) 
							&& StringUtils.isCharPresentInString(record.getNormalOABS(), B)) {
						record.setClassification(Classification.GERMLINE);
						record.setMutation(reference + MUT_DELIM + tumour.getDisplayString());
//						record.addAnnotation("mutation also found in pileup of normal");
					} else {
						// no coverage at all in normal - ?????
						record.setClassification(Classification.SOMATIC);
						record.setMutation(reference + MUT_DELIM + tumour.getDisplayString());
					}
				}
			}
		}
	}
	
	
	public static void compareGenotypes(QSnpRecord record) {
		if (null == record) 
			throw new IllegalArgumentException("Null pileup record passed to GenotypeComparisonUtil.compareGenotypes");
		
		// record has a hom normal and het tumour
		GenotypeEnum normal = record.getNormalGenotype();
		GenotypeEnum tumour = record.getTumourGenotype();
		
		if (null == normal || null == tumour) {
			logger.error("Error predicting genotype for record: " + record.getFormattedString());
			throw new IllegalArgumentException("Pileup record passed to GenotypeComparisonUtil.compareGenotypes has missing genotype");
		}
		
		String mutation = null;
		String note = null;
		
		String refString = record.getRef();
		if (refString.length() > 1) {
			logger.warn("ref string: " + refString + "  has more than 1 char in it! in GenotypeComparisonUtil.compareGenotypes");
		}
		char reference = refString.charAt(0);
		
		
		////////////////////////////////////////////////////////////////
		// this deals with HOM/HOM and HET/HET
		//////////////////////////////////////////////////////////////
		if (normal == tumour) {
			record.setClassification(Classification.GERMLINE);
			
		} else if (normal.isHomozygous() && tumour.isHomozygous()) {
			// not equal but both are homozygous
			record.setClassification(Classification.SOMATIC);
			mutation = normal.getFirstAllele() + MUT_DELIM + tumour.getFirstAllele();
			
			if (tumour.getFirstAllele() == reference) {
				VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.MUTATION_EQUALS_REF);
			}
		} else if (normal.isHeterozygous() && tumour.isHeterozygous()) {
			// not equal but both are heterozygous
			record.setClassification(Classification.SOMATIC);
			
			if ((tumour.getFirstAllele() == reference || normal.containsAllele(tumour.getFirstAllele())) 
					&& (tumour.getSecondAllele() == reference || normal.containsAllele(tumour.getSecondAllele()))) {
				VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.MUTATION_EQUALS_REF);
				char firstAllele = normal.getFirstAllele() == reference ? normal.getSecondAllele() : normal.getFirstAllele();
				mutation = firstAllele + MUT_DELIM + record.getRef();
			} else {
				
//				if ( ! tumour.containsAllele(record.getRef()))
//					record.addAnnotation("tumour heterozygous for two non-reference alleles");
				
				if (tumour.getFirstAllele() == normal.getFirstAllele())
					mutation = normal.getSecondAllele() + MUT_DELIM + tumour.getSecondAllele();
				else if (tumour.getFirstAllele() == normal.getSecondAllele())
					mutation = normal.getFirstAllele() + MUT_DELIM + tumour.getSecondAllele();
				else if (tumour.getSecondAllele() == normal.getSecondAllele())
					mutation = normal.getFirstAllele() + MUT_DELIM + tumour.getFirstAllele();
				else if (tumour.getSecondAllele() == normal.getFirstAllele())
					mutation = normal.getSecondAllele() + MUT_DELIM + tumour.getFirstAllele();
				else
					mutation = normal.getDisplayString() + MUT_DELIM + tumour.getDisplayString();
				
			}
		}
		
		///////////////////////////////////////////////////////
		// normal is HOM and tumour is HET
		///////////////////////////////////////////////////////
		else if (normal.isHomozygous() && tumour.isHeterozygous()) {
			
			GenotypeEnum refAndNormalGenotype = GenotypeEnum.getGenotypeEnum(reference, normal.getFirstAllele());
			
			if (tumour == refAndNormalGenotype) {
				record.setClassification(Classification.GERMLINE);
			} else {
				record.setClassification(Classification.SOMATIC);
				if (tumour.getFirstAllele() == normal.getFirstAllele()) {
					mutation = normal.getFirstAllele() + MUT_DELIM + tumour.getSecondAllele();
				} else if (tumour.getSecondAllele() == normal.getFirstAllele()) {
					mutation = normal.getFirstAllele() + MUT_DELIM + tumour.getFirstAllele();
				} else {
					mutation = normal.getDisplayString() + MUT_DELIM + tumour.getDisplayString();
				}
			}
		}
		
		///////////////////////////////////////////////////////
		// normal is HET and tumour is HOM
		//////////////////////////////////////////////////////
		else if (normal.isHeterozygous() && tumour.isHomozygous()){
			
			if (normal.containsAllele(tumour.getFirstAllele())) {
				record.setClassification(Classification.GERMLINE);
				if (record.getTumourCount() >= 8) note = "potential LOH";
			} else {
				record.setClassification(Classification.SOMATIC);
				mutation = normal.getFirstAllele() + MUT_DELIM + tumour.getFirstAllele();
			}
		}
		
		if (Classification.GERMLINE.equals(record.getClassification())) {
			
			// add standard annotation
			// ref -> normal
			if (normal.isHomozygous() && ! normal.containsAllele(reference)) {
				mutation = record.getRef() + MUT_DELIM + normal.getFirstAllele();
			} else if (normal.isHeterozygous() && normal.containsAllele(reference)) {
				mutation = record.getRef() + MUT_DELIM + (reference == normal.getFirstAllele() ? normal.getSecondAllele() : normal.getFirstAllele());
			} else if (normal.isHeterozygous()) {
				mutation = record.getRef() + MUT_DELIM + normal.getDisplayString();
			}
			
			if (record.getNormalCount() < 8) 
				VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.LESS_THAN_8_READS_NORMAL);
			if (record.getTumourCount() < 8)
				VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.LESS_THAN_8_READS_TUMOUR);
		} else if (Classification.SOMATIC.equals(record.getClassification())) {
			
			if (record.getNormalCount() < 12) 
				VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.LESS_THAN_12_READS_NORMAL);
			
			if (null != mutation) {
				char M = mutation.charAt(mutation.length()-1);
				
				if (StringUtils.isCharPresentInString(record.getNormalOABS(), M)) {
					final String ND = record.getNormalNucleotides();
					final int normalCount = record.getNormalCount();
//					final String alt = SnpUtils.getAltFromMutationString(mutation);
					final int altCount = SnpUtils.getCountFromNucleotideString(ND, ""+M);
					
					// only add MIN annotation if more than 3% of reads in normal support the alt
					if (( (double) altCount / normalCount) * 100 >= 3.0 ) {
						VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.MUTATION_IN_NORMAL);
					}
				}
			}
		}
		
//		if (null != note)
//			record.setNote(note);
		record.setMutation(mutation);
	}

}
