/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf;

import static org.qcmg.common.util.Constants.COLON;
import static org.qcmg.common.util.Constants.COLON_STRING;
import static org.qcmg.common.util.Constants.MISSING_DATA_STRING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;


public class VcfUtils {
	
	private static final QLogger logger = QLoggerFactory.getLogger(VcfUtils.class);
	
	public static final Pattern pattern_AC = Pattern.compile("[ACGT][0-9]+\\[[0-9]+.?[0-9]*\\],[0-9]+\\[[0-9]+.?[0-9]*\\]");
	public static final Pattern pattern_ACCS = Pattern.compile("[ACGT_]+,[0-9]+,[0-9]+");

 
 /**
  * 
  * @param re: String record from sample format column. eg. 0/1:A/C:A2[17.5],34[25.79],C2[28.5],3[27.67]
  * @param base: allel base, eg. [A,T,G,C] for SNP; [AAT, GC...] for compound SNP; null for all base
  * @return the counts for specified base or return total allels counts if base is null;
  */
	public static int getAltFrequency( VcfFormatFieldRecord re, String base){
		
		int count = 0;
 		 
		final Matcher m;
		String countString = null;
		if( (countString = re.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT) )  != null ){
			// eg. 0/1:A/C:A2[17.5],34[25.79],C2[28.5],3[27.67]
			m = pattern_AC.matcher(countString);			
			while (m.find()) {
				final String pileup = m.group();
				if(base == null){
					count += Integer.parseInt(pileup.substring(1, pileup.indexOf('['))) +
								Integer.parseInt(pileup.substring(pileup.indexOf(',')+1, pileup.indexOf('[', pileup.indexOf(','))));
				}else if(base.equalsIgnoreCase(pileup.substring(0,1))){
					count = Integer.parseInt(pileup.substring(1, pileup.indexOf('['))) +
							Integer.parseInt(pileup.substring(pileup.indexOf(',')+1, pileup.indexOf('[', pileup.indexOf(','))));
					break;
				}	 
			}					
		}else if((countString = re.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP)) != null){
			// eg. AA,1,1,CA,4,1,CT,3,1,TA,11,76,TT,2,2,_A,0,3,TG,0,1
			m = pattern_ACCS.matcher(countString);
			while (m.find()) {
				final String[] pileup = m.group().split(Constants.COMMA_STRING);
				if(base == null){
					count += Integer.parseInt(pileup[1]) + Integer.parseInt(pileup[2]);
					
				}else if(base.equalsIgnoreCase(pileup[0])){	 
					count = Integer.parseInt(pileup[1]) + Integer.parseInt(pileup[2]);
					break;
				}
			}	
		} 
		 
		 return count;
	 }   
	 
	
	public static String getPileupElementAsString(List<PileupElement> pileups, boolean novelStart) {
		
		int a = 0, c = 0, g = 0, t = 0, n = 0;
		if (null != pileups) {
			for (final PileupElement pe : pileups) {
				switch (pe.getBase()) {
				case 'A': a = pe.getTotalCount(); break;
				case 'C': c = pe.getTotalCount(); break;
				case 'G': g = pe.getTotalCount(); break;
				case 'T': t = pe.getTotalCount(); break;
				case 'N': n = pe.getTotalCount(); break;
				}
			}
		}
		return (novelStart ? "NOVELCOV" : "FULLCOV") + "=A:" + a + ",C:" + c + ",G:" + g + ",T:" + t + ",N:" + n + ",TOTAL:" + (a+c+g+t+n);
	}
	
	/**
	 * Returns the AD value from the genotype field in the GATK vcf output
	 * This corresponds to: 
	 * <code>GT:AD:DP:GQ:PL	0/1:<b>173,141</b>:282:99:255,0,255</code>
	 * where the AD field is the total unfiltered coverage for the ref and alt alleles. 
	 * @param genotype
	 * @return int corresponding to the AD field in the GATK genotype field
	 */
	public static int getADFromGenotypeField(String genotype) {
		if (null == genotype || genotype.length() == 0) return 0;
		int tally = 0;
		// looking for the number between the first and second colons
		
//		int firstIndex = genotype.indexOf(":");
		final int firstIndex = 4;	// string should always start with 0/0
		final int secondIndex = genotype.indexOf(":", firstIndex);
		final String adNumbers = genotype.substring(firstIndex, secondIndex);
		
		for (int i = 0 , size = adNumbers.length() ; i < size ; ) {
			
			if (Character.isDigit(adNumbers.charAt(i))) {
				int numberLength = 1;
				while (++i < size && Character.isDigit(adNumbers.charAt(i))) {
					numberLength++;
				}
				tally += Integer.parseInt(adNumbers.substring(i-numberLength, i));
			} else i++;
		}
		return tally;
	}
	
	/**
	 * Returns the DP value from the genotype field in the GATK vcf output
	 * This corresponds to: 
	 * <code>GT:AD:DP:GQ:PL	0/1:173,141:<b>282</b>:99:255,0,255</code>
	 * where the DP field is the total filtered coverage for all bases seen at this position. 
	 * @param format
	 * @return int corresponding to the DP field in the GATK genotype field
	 */
	public static int getDPFromFormatField(String format) {
		if (null == format || format.length() == 0) return 0;
		
		// looking for the number between the second and third colons
//		int firstIndex = genotype.indexOf(":");
		final int firstIndex = 4;	// string should always start with 0/0
		final int secondIndex = format.indexOf(":", firstIndex) +1;
		if (secondIndex == -1) { 
//			System.err.println("incorrent index for format field: " + format);
			return -1;
		}
		
		final int thirdIndex = format.indexOf(":", secondIndex);
		
		if (thirdIndex == -1) {
//			System.err.println("incorrent index for format field: " + format);
			return -1;
		}
		
		final String dpString = format.substring(secondIndex, thirdIndex);
		return Integer.parseInt(dpString);
	}
	
	public static String getGenotypeFromGATKVCFRecord(VcfRecord rec) {
		if (null == rec || rec.getFormatFields().size() < 2)
			throw new IllegalArgumentException("VCFRecord null, or does not contain the appropriate fields");
		
		final String extraField = rec.getFormatFields().get(1);	// second item in list should have pertinent info
		if (StringUtils.isNullOrEmpty(extraField)) return null;
		return extraField.substring(0,3);
	}
	
	public static GenotypeEnum getGEFromGATKVCFRec(VcfRecord rec) {
		final String genotypeString = getGenotypeFromGATKVCFRecord(rec);
		return calculateGenotypeEnum(genotypeString, rec.getRefChar(), rec.getAlt().charAt(0));
	}
	
	/**
	 * Calculates the GenotypeEnum object associated with the supplied genotype string, ref and alt chars
	 * <p>
	 * If the genotype string is 0/0, then ref/ref is returned
	 * If the genotype string is 0/1, then ref/alt is returned
	 * If the genotype string is 1/1, then alt/alt is returned
	 * 
	 * 
	 * @param genotypeString
	 * @param ref
	 * @param alt
	 * @return
	 */
	public static GenotypeEnum calculateGenotypeEnum(String genotypeString, char ref, char alt) {
		
		if ("0/1".equals(genotypeString)) {
			return GenotypeEnum.getGenotypeEnum(ref, alt);
		} else if ("1/1".equals(genotypeString)) {
			return GenotypeEnum.getGenotypeEnum(alt, alt);
		} else if ("0/0".equals(genotypeString)) {
			return GenotypeEnum.getGenotypeEnum(ref, ref);
		} else {
			System.out.println("unhandled genotype string: " + genotypeString);
			return null;
		}
	}
	
	/**
	 * Returns a string representation of the vcf GT field, which is generally "0/0", "0/1", or "1/1"
	 * 
	 * In this case it is just the "0/1" and "1/1" values that are returned as it is assumed that were are only dealing with instances where we have a mutation.
	 * 
	 * If the supplied genotype is homozygous, returns "1/1", otherwise returns "0/1"
	 * 
	 * @param genotype GenotypeEnum 
	 * @return String correspinding to the GT value relating to the supplied GenotypeEnum
	 */
	public static String calculateGTField(GenotypeEnum genotype) {
		if (null == genotype) return null;
		if (genotype.isHomozygous()) return "1/1";
		else return "0/1";
	}
	

	
	public static String[] getMutationAndGTs(String refString, GenotypeEnum control, GenotypeEnum test) {
		final String [] results = new String[3];
		final Set<Character> alts= new TreeSet<Character>();
		char ref = '\u0000';
		
		if ( ! StringUtils.isNullOrEmpty(refString)) {
			if (refString.length() > 1) {
				logger.warn("getting the first char from ref: " + refString + "  in VcfUtils.getMutationAndGTs");
			}
			ref = refString.charAt(0);
			
			
			if (null != control) {
				if (ref != control.getFirstAllele()) {
					alts.add(control.getFirstAllele());
				}
				if (ref != control.getSecondAllele()) {
					alts.add(control.getSecondAllele());
				}
			}
			if (null != test) {
				if (ref != test.getFirstAllele()) {
					alts.add(test.getFirstAllele());
				}
				if (ref != test.getSecondAllele()) {
					alts.add(test.getSecondAllele());
				}
			}
		}
		
		final int size = alts.size();
		
		final String altsString = getStringFromCharSet(alts);
		if (size == 0) {
//			assert false : "empty list of alts from control and test: " + control + ", " + test;
			Arrays.fill(results, MISSING_DATA_STRING);
		
		} else if (size == 1) {
			results[0] = alts.iterator().next().toString();
			results[1] = null != control ? control.getGTString(ref) : MISSING_DATA_STRING;
			results[2] = null != test ? test.getGTString(ref) : MISSING_DATA_STRING;
		} else {
			String alt = "";
			for (final char c : alts) {
				if (alt.length() == 0) {
					alt = "" + c;
				} else { 
					alt += "," + c;
				}
			}
			results[0] = alt;
			results[1] = getGTString(altsString, ref, control);
			results[2] = getGTString(altsString, ref, test);
		}
		
		return results;
	}
	
	public static String getGTString(String altsString, char ref, GenotypeEnum ge) {
		String result = MISSING_DATA_STRING;
		if (ge != null && ! StringUtils.isNullOrEmpty(altsString)) {
			if (ge.containsAllele(ref)) {
				if (ge.isHeterozygous()) {
					result = "0/" + (altsString.indexOf(ref == ge.getFirstAllele() ? ge.getSecondAllele() : ge.getFirstAllele()) + 1);
				} else {
					result = "0/0";
				}
			} else {
				result = (altsString.indexOf(ge.getFirstAllele()) + 1) + "/" + (altsString.indexOf(ge.getSecondAllele()) + 1);
			}
		}
		
		return result;
	}
	 
	public static String getStringFromCharSet(Set<Character> set) {
		final StringBuilder sb = new StringBuilder();
		if (null != set) {
			for (final Character c : set) sb.append(c);
		}
		return sb.toString();
	}
	
	public static VcfRecord createVcfRecord(ChrPosition cp, String id, String ref, String alt) {
		final VcfRecord rec = new VcfRecord(cp, id, ref, alt);
		return rec;
	}
	public static VcfRecord createVcfRecord(ChrPosition cp, String ref) {
		return createVcfRecord(cp, null, ref, null);
	}
//	public static VCFRecord createVcfRecord(ChrPosition cp) {
//		return createVcfRecord(cp, null, null);
//	}
	public static VcfRecord createVcfRecord(String chr, int position, String ref) {
		return createVcfRecord(new ChrPosition(chr, position), ref);
	}
	public static VcfRecord createVcfRecord(String chr, int position) {
		return createVcfRecord(new ChrPosition(chr, position), null);
	}
	
	/**
	 * A vcf record is considered to be a snp if the length of the ref and alt fields are the same (and not null/0), the fields don't contain commas, and are not equal 
	 * This caters for standard snps and compound snps (where the polymorphism covers more than 1 base)
	 * 
	 * @param vcf VCFRecord
	 * @return boolean indicating if this vcf record is a snp (or compound snp)
	 */
	public static boolean isRecordAMnp(VcfRecord vcf) {
		boolean isSnp = false;
		if (null != vcf) {
			final String ref = vcf.getRef();
			final String alt = vcf.getAlt();
			
			if ( ! StringUtils.isNullOrEmpty(ref) && ! StringUtils.isNullOrEmpty(alt)) {
				
				// if lengths are both 1, and they are not equal
				final int refLength = ref.length();
				final int altLength = alt.length();
				
				if (refLength == altLength
						&& ! ref.contains(Constants.COMMA_STRING)
						&& ! alt.contains(Constants.COMMA_STRING)
						&& ! ref.equals(alt)) {
					isSnp = true;
				}
			}
		}
		return isSnp;
	}
	
	/**
	 * Adds missing data '.' to the format field for the specified vcf record.
	 * The position parameter indicates which column should have the missing data added to it.
	 * If the position is less than 1 (the format header should always be at position 0) or greater than the 
	 * number of entries already in the format field, and IllegalArgumentException will be thrown.
	 * An insert into the list is performed ( {@link java.util.List#add(int, Object)})
	 * which shifts existing elements if required.
	 * 
	 * @param vcf
	 * @param position
	 */
	public static void addMissingDataToFormatFields(VcfRecord vcf, int position) {
		if (null == vcf) {
			throw new IllegalArgumentException("null vcf parameter passed to addMissingDataToFormatFields");
		}
		if (position < 1) {
			throw new IllegalArgumentException("position parameter passed to addMissingDataToFormatFields should not be less than 1: " + position);
		}
		// need to get the number of missing data elements to be added
		List<String> formatFields = vcf.getFormatFields();
		if (null != formatFields && ! formatFields.isEmpty()) {
			
			if (position > formatFields.size()) {
				throw new IllegalArgumentException("position parameter passed to addMissingDataToFormatFields should not be > " + formatFields.size() + " : "+ position);
			}
			
			String formatColumns = formatFields.get(0);
			//split by ":"
			int noOfColumns = formatColumns.split(":").length;
			String missingData = ".";
			for (int i = 1 ; i < noOfColumns ; i++) {
				missingData +=":.";
			}
			formatFields.add(position, missingData);
			vcf.setFormatFields(formatFields);
		}
		
	}
	
	public static void addFormatFieldsToVcf(VcfRecord vcf, List<String> additionalFormatFields) {
		if ( null != additionalFormatFields && ! additionalFormatFields.isEmpty()) {
			// if there are no existing format fields, set field to be additional..
			if (null == vcf.getFormatFields() || vcf.getFormatFields().isEmpty()) {
				//vcf.setFormatField(additionalFormatFields);
				vcf.setFormatFields(additionalFormatFields);
			} else {
				
				// check that the 2 lists of the same size
				if (vcf.getFormatFields().size() != additionalFormatFields.size()) {
					logger.warn("format field size mismatch. Exiting record has " 
				+ vcf.getFormatFields().size() + " entries, whereas additionalFormatFields has " 
							+ additionalFormatFields.size() + " entries - skipping addition");
					logger.info("vcf: " + vcf.toString() + ", additional fields: " + Arrays.deepToString(additionalFormatFields.toArray(new String[]{})));
					
				} else {
					final List<String> newFF =  vcf.getFormatFields();
					
					// need to check each element to see if it already exists...
					final String [] formatFieldAttributes = additionalFormatFields.get(0).split(COLON_STRING);
					
					for (int i = 0 ; i < formatFieldAttributes.length ; i++) {
						
						final String existingFieldAttributes = newFF.get(0);
						final String s = formatFieldAttributes[i];
						
						if (existingFieldAttributes.contains(s)) {
							// skip this one
						} else {
							// add this one
							for (int j = 0 ; j < additionalFormatFields.size() ; j++) {
								
								// get existing entry 
								final String existing = newFF.get(j);
								// create new
								final String newEntry = existing + COLON + additionalFormatFields.get(j).split(COLON_STRING)[i];
								
								// re-insert into vcf
								newFF.set(j, newEntry);
							}
						}
					}
					
					if ( ! newFF.isEmpty()) {
						vcf.setFormatFields(newFF);
					}
				}
			}
		}
	}
	
	/**
	 * Attempts to merge a number of vcf records that have the same start position
	 * NOTE that only the first 8 columns are merged, info, format fields will be empty
	 * 
	 * 
	 * 
	 * @param records
	 * @return
	 */
	public static VcfRecord mergeVcfRecords(Set<VcfRecord> records) {
		
		VcfRecord mergeRecord = null;
		
		if ( ! records.isEmpty()) {
			String chr = null;
			int startPos = -1;
			String ref = "";
			
			/*
			 * get largest ref string first
			 */
			for (VcfRecord vcf : records) {
				if (null == chr) {
					chr = vcf.getChrPosition().getChromosome();
				}
				if (chr.equals(vcf.getChrPosition().getChromosome())) {
					if (-1 == startPos) {
						startPos = vcf.getChrPosition().getPosition();
					}
					if (startPos == vcf.getChrPosition().getPosition()) {
						if (ref.length() < vcf.getRef().length()) {
							ref = vcf.getRef();
						}
					}
				}
			}
			
			List<String> altStrings = new ArrayList<>();
			
			for (VcfRecord vcf : records) {
				if (null == chr) {
					chr = vcf.getChrPosition().getChromosome();
				}
				if (chr.equals(vcf.getChrPosition().getChromosome())) {
					
					if (-1 == startPos) {
						startPos = vcf.getChrPosition().getPosition();
					}
					if (startPos == vcf.getChrPosition().getPosition()) {
						
						
						// get alt based on this ref and the largest ref
						altStrings.add(getUpdateAltString(ref, vcf.getRef(), vcf.getAlt()));
						
					}
				}
			}
			
			StringBuilder sb = new StringBuilder();
			Collections.sort(altStrings);
			for (String s : altStrings) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(s);
			}
			
			if (ref.length() == 0) {
				logger.warn("Got zero length ref!!! ref: " + ref + ", entries in set: " + records.size());
			}
			mergeRecord = VcfUtils.createVcfRecord(new ChrPosition(chr, startPos, startPos + ref.length() -1 ), ".", ref, sb.toString());
		}
		return mergeRecord;
	}
	
	
	public static String getUpdateAltString(String newRef, String origRef, String origAlt) {
		/*
		 * Perform some checks on input strings
		 */
		if (StringUtils.isNullOrEmpty(origRef) || StringUtils.isNullOrEmpty(newRef) || StringUtils.isNullOrEmpty(origAlt)) {
			throw new IllegalArgumentException("Null or empty string(s) passed to getUpdateAltString, newRef: " + newRef + ", origRef: " + origRef + ", origAlt: " + origAlt);
		}
		// newRef should be longer than origRef
		if (origRef.length() > newRef.length()) {
			throw new IllegalArgumentException("Orig ref is longer than new ref! newRef: " + newRef + ", origRef: " + origRef + ", origAlt: " + origAlt);
		}
		
		if (newRef.equals(origRef)) {
			return origAlt;
		} else {
			// get extra chars to add onto origAlt
			String additionalAlt = newRef.substring(origRef.length());
			return origAlt + additionalAlt;
		}
	}
	
//	public static String getUpdateAltString(String ref, String alt) {
//		if (alt.contains(",")) {
//			
//			StringBuilder sb = new StringBuilder();
//			List<String> altStrings = new ArrayList<>();
//			String [] alts = alt.split(",");
//			for (int i = 0 ; i < alts.length ; i++) {
//				String s = alts[i];
//				if (s.length() > 1) {
//					// add last base in ref to this alt string
//					alts[i] = s + ref.charAt(ref.length() -1);
//				}
//				altStrings.add(alts[i]);
//			}
//			Collections.sort(altStrings);
//			for (String s : altStrings) {
//				if (sb.length() > 0) {
//					sb.append(",");
//				}
//				sb.append(s);
//			}
//			return sb.toString();
//		}
//		return null;
//	}
	
	
	
	/**
	 * Checks to see if the existing annotation is a PASS (or missing data symbol (".")).
	 * If it is, then the annotation is replaced with the supplied annotation.
	 * If its not, the supplied annotation is appended to the existing annotation(s)
	 * 
	 * Also, if the supplied annotation is a PASS, then all previous annotations are removed.
	 * 
	 * @param rec qsnp record
	 * @param ann String representation of the annotation
	 */
	public static void updateFilter(VcfRecord rec, String ann) {
		// perform some null guarding
		if (null == rec) throw new IllegalArgumentException("Null vcf record passed to updateFilter");
		
		if (SnpUtils.PASS.equals(rec.getFilter()) || Constants.MISSING_DATA_STRING.equals(rec.getFilter()) || SnpUtils.PASS.equals(ann)) {
			rec.setFilter(ann);
		} else {
			rec.addFilter(ann);
		}
	}
	
	public static void removeFilter(VcfRecord rec, String filter) {
		// perform some null guarding
		if (null == rec) throw new IllegalArgumentException("Null vcf record passed to removeAnnotation");
		if (null == filter) {
			return;
		}
		
		rec.setFilter(StringUtils.removeFromString(rec.getFilter(), filter, Constants.SC));
	}
	
 
}
