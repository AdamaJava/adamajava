/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import htsjdk.samtools.SAMUtils;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.PileupElementComparator;
import org.qcmg.common.model.Rule;
import org.qcmg.common.string.StringUtils;

public class PileupElementUtil {

	/** Byte typed variables for all normal bases. */
    public static final byte a='a', c='c', g='g', t='t', n='n', A='A', C='C', G='G', T='T', N='N', DOT='.',COMMA=',';
    public static final char SEPARATOR = '/';
    private static final Comparator<PileupElement> countRefQualComparator = new PileupElementComparator();
    private static final QLogger logger = QLoggerFactory.getLogger(PileupElementUtil.class);
    private static final NumberFormat nf = new DecimalFormat("0.##");
    
    private static final Pattern pattern = Pattern.compile("[ACGT]:[0-9]+\\[[0-9]+.?[0-9]*\\],[0-9]+\\[[0-9]+.?[0-9]*\\]");
    
    public static List<PileupElement> getPileupCounts(String bases) {
    	return getPileupCounts(bases, (String) null);
    }
    
    public static List<PileupElement> getPileupCounts(String bases, String baseQualities) {
    		// return null if the bases string is null, or zero length, or if it just contains '*'
    	if (null == bases || bases.length() == 0 || (bases.length() == 1 && bases.charAt(0) == '*')) return null;
    	
    	
	    	PileupElement peA = new PileupElement(A);
	    	PileupElement peC = new PileupElement(C);
	    	PileupElement peG = new PileupElement(G);
	    	PileupElement peT = new PileupElement(T);
	    	PileupElement peDot = new PileupElement(DOT);
    	
	    byte [] baseQuals = StringUtils.isNullOrEmpty(baseQualities) ? null : SAMUtils.fastqToPhred(baseQualities);
	    
    	int pointer = 0;
    	for (int i = 0 , end = bases.length() ; i < end ; i++) {
    		char b = bases.charAt(i);
    		if ('$' == b) continue;
    		if ('^' == b) {	// skip this byte and the next one too
    				i++;
    			continue;
    			}
    		if ('+' == b || '-' == b) {
	    			b = bases.charAt(++i);
	    			String indelSizeString = ""+b;
	    			
	    			b = bases.charAt(++i);
    			while( Character.isDigit(b) ) {
	    				indelSizeString += b;
	    				b = bases.charAt(++i);
	    			}
    			
    				i += (Integer.valueOf(indelSizeString) - 1);
    			
    			continue;
    			}
    		
    			// INDELs....
    		
    			// extra code to take base quality into account
    		byte quality = baseQuals == null ? Byte.MIN_VALUE : baseQuals[pointer++];
    			// end of extra code
    		
    		switch(b) {
	    		case COMMA: peDot.incrementReverseCount(quality);break;
	    		case DOT: peDot.incrementForwardCount(quality);break;
	    		case a: peA.incrementReverseCount(quality); 	break;
	    		case A: peA.incrementForwardCount(quality); 	break;
	    		case c: peC.incrementReverseCount(quality);	break;
	    		case C: peC.incrementForwardCount(quality);	break;
	    		case g: peG.incrementReverseCount(quality);	break;
	    		case G: peG.incrementForwardCount(quality);	break;
	    		case t: peT.incrementReverseCount(quality);	break;
	    		case T: peT.incrementForwardCount(quality); 	break;
    			}
    		}
    	
    	int validBases = 0;
    		// should never have more than 4 elements in this list
    		List<PileupElement> baseCounts = new ArrayList<PileupElement>(4);
    	if (peA.getTotalCount() > 0) {
    			baseCounts.add(peA);
    			validBases++;
    		}
    	if (peC.getTotalCount() > 0) {
	    		baseCounts.add(peC);
	    		validBases++;
	    	}
    	if (peG.getTotalCount() > 0) {
	    		baseCounts.add(peG);
	    		validBases++;
	    	}
    	if (peT.getTotalCount() > 0) {
	    		baseCounts.add(peT);
	    		validBases++;
	    	}
    	if (peDot.getTotalCount() > 0) {
	    		baseCounts.add(peDot);
	    		validBases++;
	    	}
    	
	    	// sort the collection to ensure that the 1st entry is the one with the largest total count
	    	// only need to do this if we have more than 1 entry in the list
	    	//TODO should we be using the PileupelementComparator for this??
    	if (validBases > 1)
    			Collections.sort(baseCounts);
    	
    	return baseCounts;
    	}
    public static List<PileupElement> getPileupCounts(String bases, byte [] baseQuals) {
    	// return null if the bases string is null, or zero length, or if it just contains '*'
    	if (null == bases || bases.length() == 0 || (bases.length() == 1 && bases.charAt(0) == '*')) return null;
    	
    	
    	PileupElement peA = new PileupElement(A);
    	PileupElement peC = new PileupElement(C);
    	PileupElement peG = new PileupElement(G);
    	PileupElement peT = new PileupElement(T);
    	PileupElement peDot = new PileupElement(DOT);
    	
    	int pointer = 0;
    	for (int i = 0 , end = bases.length() ; i < end ; i++) {
    		char b = bases.charAt(i);
    		if ('$' == b) continue;
    		if ('^' == b) {	// skip this byte and the next one too
    			i++;
    			continue;
    		}
    		if ('+' == b || '-' == b) {
    			b = bases.charAt(++i);
    			String indelSizeString = ""+b;
    			
    			b = bases.charAt(++i);
    			while( Character.isDigit(b) ) {
    				indelSizeString += b;
    				b = bases.charAt(++i);
    			}
    			
    			i += (Integer.valueOf(indelSizeString) - 1);
    			
    			continue;
    		}
    		
    		// INDELs....
    		
    		// extra code to take base quality into account
    		byte quality = baseQuals == null ? Byte.MIN_VALUE : baseQuals[pointer++];
    		// end of extra code
    		
    		switch(b) {
    		case COMMA: peDot.incrementReverseCount(quality);break;
    		case DOT: peDot.incrementForwardCount(quality);break;
    		case a: peA.incrementReverseCount(quality); 	break;
    		case A: peA.incrementForwardCount(quality); 	break;
    		case c: peC.incrementReverseCount(quality);	break;
    		case C: peC.incrementForwardCount(quality);	break;
    		case g: peG.incrementReverseCount(quality);	break;
    		case G: peG.incrementForwardCount(quality);	break;
    		case t: peT.incrementReverseCount(quality);	break;
    		case T: peT.incrementForwardCount(quality); 	break;
    		}
    	}
    	
    	int validBases = 0;
    	// should never have more than 4 elements in this list
    	List<PileupElement> baseCounts = new ArrayList<PileupElement>(4);
    	if (peA.getTotalCount() > 0) {
    		baseCounts.add(peA);
    		validBases++;
    	}
    	if (peC.getTotalCount() > 0) {
    		baseCounts.add(peC);
    		validBases++;
    	}
    	if (peG.getTotalCount() > 0) {
    		baseCounts.add(peG);
    		validBases++;
    	}
    	if (peT.getTotalCount() > 0) {
    		baseCounts.add(peT);
    		validBases++;
    	}
    	if (peDot.getTotalCount() > 0) {
    		baseCounts.add(peDot);
    		validBases++;
    	}
    	
    	// sort the collection to ensure that the 1st entry is the one with the largest total count
    	// only need to do this if we have more than 1 entry in the list
    	//TODO should we be using the PileupelementComparator for this??
    	if (validBases > 1)
    		Collections.sort(baseCounts);
    	
    	return baseCounts;
    }
    
    public static int getLargestVariantCount(List<PileupElement> baseCounts) {
    	return getLargestVariantCount(baseCounts, (char)DOT);
    }
    
    public static int getLargestVariantCount(List<PileupElement> baseCounts, char ref) {
    	PileupElement largestVarient = getLargestVariant(baseCounts, ref);
    	if (null != largestVarient) return largestVarient.getTotalCount();
    	return 0;
    }
    
    public static int getBaseCount(List<PileupElement> elements, char base) {
	    	for (PileupElement pe : elements) {
	    		if (base == pe.getBase()) return pe.getTotalCount();
	    	}
	    	return 0;
    	}
    
    public static PileupElement getLargestVariant(List<PileupElement> baseCounts) {
    	
    	// use default DOT as the ref
    	return getLargestVariant(baseCounts, (char) DOT);
    	}
    
    public static PileupElement getLargestVariant(List<PileupElement> baseCounts, char ref) {
    	
    	if (null == baseCounts || baseCounts.isEmpty()) return null;
    	
    	// if this relates to the reference base '.', return the next largest count
    	if (baseCounts.get(0).getBase() != ref)
    		return baseCounts.get(0);
    	else if (baseCounts.size() > 1)
    		return baseCounts.get(1);
    	else return null;
    }
    
    public static List<PileupElement> getPileupCountsThatPassRule(List<PileupElement> originalCounts, final Rule rule, final boolean secondPass, final double percentage) {
    	if (null == originalCounts || null == rule) return null;
    	
    	int coverage = 0;
    	int totalQuality = 0;
    	for (PileupElement pe : originalCounts) {
    			coverage += pe.getTotalCount();
    			totalQuality += pe.getTotalQualityScore();
    		}
    	
    		List<PileupElement> newCounts = new ArrayList<PileupElement>(originalCounts.size());	// won't be bigger than the original list
    	
    	for (PileupElement pe : originalCounts) {
    		if (passesCountCheck(pe.getTotalCount(), coverage, rule, secondPass) 
    				&& qualityCheck(totalQuality, pe.getTotalQualityScore(), percentage, secondPass)) {
    			if (secondPass) {
    				// needs to be on both strands
    				if (pe.isFoundOnBothStrands()) newCounts.add(pe);
    				} else newCounts.add(pe);
    			}
    		}
    	
    	return newCounts;
    	}
    
	public static boolean passesCountCheck(int count, int coverage, Rule rule) {
		return passesCountCheck(count, coverage, rule, false);
	}
	
	public static boolean passesCountCheck(int count, int coverage, Rule rule, boolean secondPass) {
		if (coverage < 0)
			throw new IllegalArgumentException("coverage cannot be less then zero");
		if (count > coverage)
			throw new IllegalArgumentException("count cannot be greater than the coverage");
		if (null == rule)
			throw new IllegalArgumentException("null Rule passed to method");
		
		boolean usePercentage = rule.getMaxCoverage() == Integer.MAX_VALUE;
		
		if (usePercentage) {
			double noOfVariants = rule.getNoOfVariants();
			
			if (secondPass) {
				return ((double)count / coverage * 100) >= (noOfVariants / 2);
			} else {
				return ((double)count / coverage * 100) >= noOfVariants;
			}
		} else {
			return count >= rule.getNoOfVariants();
		}
	}
	


	public static GenotypeEnum getGenotype(List<PileupElement> baseCounts, char reference) {
		if (null == baseCounts || baseCounts.isEmpty()) return null;
		
		if (baseCounts.size() == 1) {
			char c = baseCounts.get(0).getBase();
			if (DOT == c) c = reference;
			return GenotypeEnum.getGenotypeEnum(c, c);
//			return new Genotype(c,c);
		} else if (baseCounts.size() == 2) {
			char c1 = baseCounts.get(0).getBase();
			char c2 = baseCounts.get(1).getBase();
			if (DOT == c1) c1 = reference;
			if (DOT == c2) c2 = reference;
			return GenotypeEnum.getGenotypeEnum(c1,c2);
//			return new Genotype(c1,c2);
		} else  {
			//3 or more in collection
			// re-sort according to count, reference, and base qualities
			//TODO should this be the only sorting we do??
			Collections.sort(baseCounts, countRefQualComparator);
			char c1 = baseCounts.get(0).getBase();
			char c2 = baseCounts.get(1).getBase();
			if (DOT == c1) c1 = reference;
			if (DOT == c2) c2 = reference;
			return GenotypeEnum.getGenotypeEnum(c1,c2);
		}
	}
	
	/**
	 * Checks to see if the base qualities for the largest variant (see {@link #getLargestVariant(List)}) 
	 * as a percentage of the total base qualities for the pileupe elements in the collection,
	 * is equal to or larger than the supplied percentage value. Return true if yes, false otherwise. 
	 * 
	 * @param bases  List of PileupElements to be examined
	 * @param percentage double representing percentage pass mark
	 * @return boolean indicating if percentage score was reached
	 */
	public static boolean passesWeightedVotingCheck(List<PileupElement> bases, final double percentage, final boolean secondPass) {
		
		PileupElement largestVariant = getLargestVariant(bases);
		if (null == largestVariant) return false;
		
		int variantQualityScore = largestVariant.getTotalQualityScore();
		int totalQualityScore= 0;
		
		for (PileupElement pe : bases) {
			totalQualityScore += pe.getTotalQualityScore();
		}
		
		return qualityCheck(totalQualityScore, variantQualityScore, percentage, secondPass);
//		return (100 * (double)variantQualityScore / totalQualityScore) >= (secondPass ? percentage / 2 : percentage);
	}
	public static boolean passesWeightedVotingCheck(List<PileupElement> bases, final double percentage) {
		return passesWeightedVotingCheck(bases, percentage, false);
	}
	
	public static boolean qualityCheck(final int totalQualityScore, final int baseQualityScore, final double percentage, final boolean secondPass) {
		return (100 * (double)baseQualityScore / totalQualityScore) >= (secondPass ? percentage / 2 : percentage);
	}
	
	/**
	 * Returns all the bases contained within a collection of PileupElements as a String
	 * Slightly different to {@link #getPileupFromPileupList(List)} in that it does not print the base for each PileupElement more than once.
	 * <p>
	 * If a PileupElement object within the collection has '.' as its base, the supplied char will be used in its stead.
	 * 
	 * @param pileupElements List of PileupElements to be decoded
	 * @param reference char referring to the reference
	 * @return
	 */
	public static String getBasesFromPileupElements(List<PileupElement> pileupElements, char reference) {
		if (null == pileupElements || pileupElements.isEmpty()) return null;
		String result = "";
		for (PileupElement pe : pileupElements)
			result += DOT == pe.getBase() ? reference : pe.getBase();
		return result;
	}

	/**
	 * Returns a string representation of a collection of PileupElements. Includes the count and RMS of quals for each strand.
	 *  eg. A:1[40],2[35] tells us that we the base is an A, with 1 count on the forward strand 
	 *  (RMS of base qual = 40), and 2 counts on the reverse strand(where the RMS of the base qualities is 35)
	 *   <p>
	 *   If a PileupElement object within the collection has '.' as its base, the supplied char will be used in its stead.
	 * 
	 * @param pileupElements List of PileupElements to be decoded
	 * @param reference char referring to the reference
	 * @return String representation of the collection of PileupElements
	 */
	public static String getPileupElementString(List<PileupElement> pileupElements, char reference) {
		if (null == pileupElements || pileupElements.isEmpty()) return null;
		String result = "";
		for (PileupElement pe : pileupElements) {
			if (result.length() > 0) result += ",";
			result += DOT == pe.getBase() ? reference : pe.getBase();
			result += (pe.getForwardCount() + "[" + nf.format(getRMSQualities(pe.getForwardQualities())) + "]," 
					+ pe.getReverseCount() + "[" + nf.format(getRMSQualities(pe.getReverseQualities())) + "]");
		}
		return result;
	}
	
	/**
	 * Recreate PileupElement collection from String
	 * <p>
	 * Strings are in the following format:
	 * T:7[40],0[0],G:5[32.68],0[0]
	 * <p>
	 * NOTE that the qualities will no longer be accurate as they were RMS'ed when converting into String format for brevity, and so only an 
	 * 
	 * @param pileupString String containing pileup elements
	 * @return List of PileupElement objects
	 */
	public static List<PileupElement> createPileupElementsFromString(String pileupString) {
		if (null == pileupString || pileupString.isEmpty()) return null;
		
		List<PileupElement> result = new ArrayList<PileupElement>();
		Matcher m = pattern.matcher(pileupString);
		while (m.find()) {
			String pileup = m.group();
			// first char is the base
			char base = pileup.charAt(0);
			PileupElement pe = new PileupElement(base);
			
			int forwardStrandCount = Integer.parseInt(pileup.substring(2, pileup.indexOf('[')));
			int reverseStrandCount = Integer.parseInt(pileup.substring(pileup.indexOf(',')+1, pileup.indexOf('[', pileup.indexOf(','))));
			
			for (int i = 0 ; i < forwardStrandCount ; i++) pe.incrementForwardCount();
			for (int i = 0 ; i < reverseStrandCount ; i++) pe.incrementReverseCount();
			
			result.add(pe);
		}
		return result;
	}
	
	/**
	 * Calculates the root mean square of the Byte values in a collection
	 * 
	 * @param qualities collection containing Byte onjects
	 * @return double representing the RMS of the collection of Bytes
	 */
	public static double getRMSQualities(Collection<Byte> qualities) {
		if (null == qualities || qualities.isEmpty()) return 0.0;
		double total = 0;
		for (Byte b : qualities) total += (b*b);
		total /= qualities.size();
		return Math.sqrt(total);
	}
	
	/**
	 * Returns the combined count of all PileupElements in the supplied collection<p>
	 * NOTE that this does not take strand into account, and so the total count (forward and reverse strand) is returned 
	 * 
	 * @param elements List of PileupElements to be decoded
	 * @return int coverage represented by the collection of PileupElements
	 */
	public static int getCoverageFromPileupList(List<PileupElement> elements) {
		if (null == elements || elements.isEmpty()) return 0;
		int tally = 0;
		for (PileupElement pe : elements) tally += pe.getTotalCount();
		return tally;
	}
	
	/**
	 * Returns a string representation of the pileup used to form the list of PileupElements<p>
	 * Note that strand information is not currently conserved. ie. the pileup string will be in upper case
	 * 
	 * @param elements List of PileupElements to be decoded
	 * @return Stirng representing pileup from collection
	 */
	public static String getPileupFromPileupList(List<PileupElement> elements) {
		if (null == elements || elements.isEmpty()) return null;
		String pileup = "";
		for (PileupElement element : elements) {
			int count = element.getTotalCount();
			for (int i = 0; i < count ; i++)
				pileup += element.getBase();
		}
		return pileup;
	}
}
