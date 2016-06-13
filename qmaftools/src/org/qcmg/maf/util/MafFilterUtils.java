/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf.util;

import org.qcmg.common.dcc.DccConsequence;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.maf.MAFRecord;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.model.MafType;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.maf.MafPipelineNew;

public class MafFilterUtils {
	
	private static final QLogger logger = QLoggerFactory.getLogger(MafFilterUtils.class);
	
	public static final int MIN_PASSING_PERCENTAGE = 3;
	
	public static final int HIGH_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int HIGH_CONF_ALT_FREQ_PASSING_SCORE = 5;
	
	public static final int LOW_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int LOW_CONF_ALT_FREQ_PASSING_SCORE = 4;
	
	public static final int INDEL_NNS_PERCENTAGE = 5;
	public static final FilterOptions F_O;
	
	static {
		F_O = new FilterOptions();
		F_O.setHomopolymerCutoff(MafPipelineNew.HOMOPOLYMER_CUTOFF);
	}
	
	public static void classifyMAFRecord(MAFRecord maf) {
		classifyMAFRecord(maf, F_O);
	}
	
	public static void classifyMAFRecord(MAFRecord maf, FilterOptions options) {
		if (null == maf) throw new IllegalArgumentException("Null maf record passed to classifyMAFRecord");
		if (null == maf.getVariantType()) 
			throw new IllegalArgumentException("Maf record with null/empty variantType field passed to classifyMAFRecord");
		
		MafConfidence confidence = MafConfidence.ZERO;
		// if validation status is set to valid - high confidence
		if (checkValidationStatus(maf)) {
			confidence = MafConfidence.HIGH;
		} else {
			switch (maf.getVariantType()) {
			case DNP : 
			case TNP : 
			case ONP : break;	// only want SNPs to make it io HC or LC status
			case SNP : // should all fall through to the ONP case
				if (checkNovelStarts(HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, maf) 
						&& checkAltFrequency(HIGH_CONF_ALT_FREQ_PASSING_SCORE, maf)
						&& SnpUtils.isClassA(maf.getFlag())) {
					
					confidence = MafConfidence.HIGH;
					
				} else if (checkNovelStarts(LOW_CONF_NOVEL_STARTS_PASSING_SCORE, maf) 
						&& checkAltFrequency(LOW_CONF_ALT_FREQ_PASSING_SCORE, maf)
						&& SnpUtils.isClassAorB(maf.getFlag())) {
					
					confidence = MafConfidence.LOW;
				}
				
				break;
			case INS :	// should fall through to the DEL case here
			case DEL :
//				if (checkDbSnpIsNovel(maf)) {
					if (SnpUtils.isClassAIndel(maf.getFlag(), options.getHomopolymerCutoff())) {
						confidence = MafConfidence.HIGH;
					} else if (SnpUtils.isClassAorBIndel(maf.getFlag()))	confidence = MafConfidence.LOW;
//				}
				break;
				
				/****** insert additional code here when required ******/
			}
		}
		
		// and now check for gene
		if (confidence.isHighOrLowConfidence() && checkNonEmptyGene(maf) && checkConsequence(maf)) {
			// set to consequence confidence
			if (MafConfidence.HIGH == confidence) {
				confidence = MafConfidence.HIGH_CONSEQUENCE;
			} else {
				confidence = MafConfidence.LOW_CONSEQUENCE;
			}
		}
		maf.setConfidence(confidence);
	}
	
	/**
	 * returns true if maf record passes this check, false otherwise
	 * 
	 * @param indelNnsPercentage
	 * @param maf
	 * @param type
	 * @return
	 */
	public static boolean checkNovelStartsIndel(int indelNnsPercentage, MAFRecord maf) {
		if (null == maf || maf.getFlag() == null) 
			throw new IllegalArgumentException("null maf or null maf flag passed to checkNovelStartsIndel");
		
		if (maf.getFlag().contains(SnpUtils.NOVEL_STARTS)) {
			if (maf.getMafType().isSomatic()) {
				
				int tSeventh =  SnpUtils.getCountFromIndelNucleotideString(maf.getTd(), 6);
				if (tSeventh > 0) {
				
					int nSeventh = SnpUtils.getCountFromIndelNucleotideString(maf.getNd(), 6);
					int nSecond = SnpUtils.getCountFromIndelNucleotideString(maf.getNd(), 1);
					
					return (((double) nSeventh / nSecond) * 100)  <  indelNnsPercentage;
				}
				
			} else {
				int tSeventh =  SnpUtils.getCountFromIndelNucleotideString(maf.getTd(), 6);
				int tSecond = SnpUtils.getCountFromIndelNucleotideString(maf.getTd(), 1);
				
				int nSeventh = SnpUtils.getCountFromIndelNucleotideString(maf.getNd(), 6);
				int nSecond = SnpUtils.getCountFromIndelNucleotideString(maf.getNd(), 1);
				return (((double) nSeventh / nSecond) * 100)  >=  indelNnsPercentage
						|| (((double) tSeventh / tSecond) * 100)  <  indelNnsPercentage;
			}
			
		} else {
			// no suspicion of NNS - true 
			return true;
		}
		return false;
	}

	public static boolean checkValidationStatus(MAFRecord maf) {
		if (null == maf) return false;
		return "valid".equalsIgnoreCase(maf.getValidationStatus());
	}
	
	/**
	 * Returns true if "novel" is found in the db snp id field
	 * 
	 * @param maf
	 * @return
	 */
	public static boolean checkDbSnpIsNovel(MAFRecord maf) {
		if (null == maf) return false;
		return "novel".equalsIgnoreCase(maf.getDbSnpId());
	}
	
	/**
	 * Returns true if the number of novel starts are greater than or equal to the passingScore parameter
	 * 
	 * @param passingScore int
	 * @param maf MAFRecord
	 * @return boolean indicating if the supplied maf record has the supplied number of novel starts or higher
	 */
	public static boolean checkNovelStarts(int passingScore, MAFRecord maf) {
		if (null == maf) return false;
		return maf.getNovelStartCount() >= passingScore;
	}
	
	/**
	 * Returns true if consequence enum is one we care about
	 * @param maf
	 * @return
	 */
	public static boolean checkConsequence(MAFRecord maf) {
		if (null == maf) return false;
		return DccConsequence.passesMafNameFilter(maf.getVariantClassification());
	}

	/**
	 * Returns false if the gene is null, empty, equal to "Unknown", "--",or "---"
	 * true otherwise
	 * 
	 * @param maf
	 * @return
	 */
	public static boolean checkNonEmptyGene(MAFRecord maf) {
		if (null == maf) return false;
		String gene = maf.getHugoSymbol();
		if (StringUtils.isNullOrEmpty(gene)) return false;
		if ("Unknown".equalsIgnoreCase(gene)) return false;
		if ("-".equals(gene)) return false;
		if ("--".equals(gene)) return false;
		if ("---".equals(gene)) return false;
		return true; 	
	}
	
	/**
	 * Need to determine the type of maf record (Som or germ) as to what fields to use in the calculation
	 * @param passingScore
	 * @param maf
	 * @return
	 */
	public static boolean checkAltFrequency(int passingScore, MAFRecord maf) {
		if (null == maf) return false;
		
		// if novel starts is greater than or equals to passingScore - return true, otherwise get total numbers and check
		if (maf.getNovelStartCount() >= passingScore && passingScore > 0) return true;
		
		String ndTd = null;
		if (maf.getMafType().isSomatic()) {
			if (null == maf.getTd()
					|| null == maf.getRef()
					|| null == maf.getTumourAllele1()
					|| null == maf.getTumourAllele2()) return false;
			ndTd = maf.getTd();
		} else {
			if (null == maf.getNd()
					|| null == maf.getRef()
					|| null == maf.getNormalAllele1()
					|| null == maf.getNormalAllele2()) return false;
			ndTd = maf.getNd();
		}
		
		String alt = MafUtils.getVariant(maf);
		if (alt.length() > 1) {
			logger.warn("alt: " + alt + " in MafFilterUtils.checkAltFrequency");
		}
		return MafUtils.passesCountCheck(ndTd, passingScore, alt.charAt(0));
	}
	
	/**
	 * 
	 * returns true if it looks good, false otherwise
	 * 
	 * @param maf
	 * @param mafType
	 * @return
	 */
	public static boolean checkIndelMotif(MAFRecord maf, MafType mafType) {
		if (null == maf)
			throw new IllegalArgumentException("null maf passed to checkIndelMotif");
		if (mafType.isGermline() && null == maf.getNd())
			throw new IllegalArgumentException("maf with null nd field passed to checkIndelMotif");
		else if (mafType.isSomatic() && null == maf.getTd())
			throw new IllegalArgumentException("maf with null td field passed to checkIndelMotif");
		
		int motifSize = maf.getRef().length();
		int first = SnpUtils.getCountFromIndelNucleotideString(mafType.isSomatic() ? maf.getTd() : maf.getNd(), 0);
		int third = SnpUtils.getCountFromIndelNucleotideString(mafType.isSomatic() ? maf.getTd() : maf.getNd(), 2);
		
		if (motifSize == 1 || motifSize == 2) {
			return (((double) first / third) * 100) > 10.0;
		} else if (motifSize >= 3) {
			return (((double) first / third) * 100) > 5.0;
		}
		
		return false;
	}
	
	/**
	 * return true if there is plenty of evidence, false otherwise
	 * 
	 */
	public static boolean checkIndelEvidence(MAFRecord maf) {
		if (null == maf)
			throw new IllegalArgumentException("null maf passed to checkIndelLowEvidence");
		if (maf.getMafType().isGermline() && null == maf.getNd())
			throw new IllegalArgumentException("maf with null nd field passed to checkIndelLowEvidence");
		else if (maf.getMafType().isSomatic() && null == maf.getTd())
			throw new IllegalArgumentException("maf with null td field passed to checkIndelLowEvidence");
		
		// ADD low evidence FILTER HERE
        // If the tumour novel starts (1st number) is <5% of the total informative reads(3rd number)
		
		int NNS = SnpUtils.getCountFromIndelNucleotideString(maf.getMafType().isSomatic() ? maf.getTd() : maf.getNd(), 0);
		int informative = SnpUtils.getCountFromIndelNucleotideString(maf.getMafType().isSomatic() ? maf.getTd() : maf.getNd(), 2);
		
		return (((double) NNS / informative) * 100 ) >= INDEL_NNS_PERCENTAGE;
	}
	
	// utility methods to check existence of fields used in checks
	public static boolean containsNovelStart(MAFRecord maf) {
		if (null == maf) return false;
		return maf.getNovelStartCount() > 0;
	}
	
	/**
	 * {@link #checkMAFForMIN(MAFRecord, int)}
	 */
	public static void checkMAFForMIN(MAFRecord maf) {
		checkMAFForMIN(maf, MIN_PASSING_PERCENTAGE);
	}

	/**
	 * Checks to see if the maf record contains MIN as the only value in the qcmg flag field.
	 * If this is the case, it will check to see if there is a single read supporting the alt.
	 * If yes, and this single read accounts for more than 3% of the total reads at this position, do nowt, otherwise replace the annotation with PASS
	 * as there are not enough of the alts in the normal to support this flag.
	 * 
	 * @param maf
	 * @param minPassingPercentage
	 */
	public static void checkMAFForMIN(MAFRecord maf, int minPassingPercentage) {
		if (null == maf) throw new IllegalArgumentException("Null maf record passed to checkMAFForMIN");
		
		// for snps - only proceed if MIN is the only annotation
		// for indels, PASS;MIN is the only allowed annotation
		
		if ( maf.getMafType().isIndel()) {
			if (maf.getMafType().isGermline() && SnpUtils.isAnnotationAlone(maf.getFlag(), SnpUtils.MUTATION_IN_NORMAL)) {
					MafUtils.updateFlag(maf, SnpUtils.PASS);
			}
		} else {
			if (SnpUtils.MUTATION_IN_NORMAL.equals(maf.getFlag())) {
				String alt = MafUtils.getVariant(maf);
				int normalAltCount = SnpUtils.getCountFromNucleotideString(maf.getNd(), alt);
				
				assert normalAltCount > 0 : 
					SnpUtils.MUTATION_IN_NORMAL + " annotation is set, but there are no reads supporting the alt in normal: " + maf.getNd() + ", alt: " + alt;
				
//				if (normalAltCount == 1) {
					String ref = maf.getRef();
					int normalRefCount = SnpUtils.getCountFromNucleotideString(maf.getNd(), ref);
					
					if (( 100.0d * normalAltCount / (normalRefCount + normalAltCount)) < minPassingPercentage) {
						// reset annotation
						MafUtils.updateFlag(maf, SnpUtils.PASS);
					}
//				}
			}
			
		}
	}
	
	
}
