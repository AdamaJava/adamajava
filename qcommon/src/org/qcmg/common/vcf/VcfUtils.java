/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf;

import static org.qcmg.common.util.Constants.COLON;
import static org.qcmg.common.util.Constants.COLON_STRING;
import static org.qcmg.common.util.Constants.MISSING_DATA_STRING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class VcfUtils {
	
	private static final QLogger logger = QLoggerFactory.getLogger(VcfUtils.class);
	public static final Pattern pattern_AC = Pattern.compile("[ACGT][0-9]+\\[[0-9]+.?[0-9]*\\],[0-9]+\\[[0-9]+.?[0-9]*\\]");
	public static final Pattern pattern_ACCS = Pattern.compile("[ACGT_]+,[0-9]+,[0-9]+");
	public static final int CONF_LENGTH = (VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ).length();
	
	/**
	 * Attempts to merge a number of vcf records that have the same start position
	 * NOTE that only the first 8 columns are merged, info, format fields will be empty
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
						startPos = vcf.getChrPosition().getStartPosition();
					}
					if (startPos == vcf.getChrPosition().getStartPosition()) {
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
						startPos = vcf.getChrPosition().getStartPosition();
					}
					if (startPos == vcf.getChrPosition().getStartPosition()) {
						
						
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
			mergeRecord = new VcfRecord.Builder(chr, startPos, ref).allele(sb.toString()).build();
					
		}
		return mergeRecord;
	}
	
	public static List<String> convertFFMapToList(Map<String, String[]> ffm) {
		return convertFFMapToList(ffm, null);
	}
	
	public static List<String> convertFFMapToList(Map<String, String[]> ffm, String [] orderedHeaders) {
		/*
		 * needs to be  a list of string, ordered correctly
		 */
		List<String> header = null;
		if (orderedHeaders == null) {
			 header = new ArrayList<>(ffm.keySet());
			header.sort((s1, s2) -> {
				if (s1.equals(VcfHeaderUtils.FORMAT_GENOTYPE)) return -1; 
				else if (s2.equals(VcfHeaderUtils.FORMAT_GENOTYPE)) return 1; 
				else return s1.compareTo(s2);});
     	} else {
     		header = Arrays.asList(orderedHeaders);
     	}
		
		List<StringBuilder> ffl = new ArrayList<>();
		ffl.add(new StringBuilder(header.stream().collect(Collectors.joining(Constants.COLON_STRING))));
		
		for (String h : header) {
			String[] sa = ffm.get(h);
			for (int i = 1 ; i <= sa.length ; i++) {
				StringBuilder sb;
				if (ffl.size() <= i) {
					sb = new StringBuilder();
					ffl.add(sb);
				} else {
					sb = ffl.get(i);
				}
				StringUtils.updateStringBuilder(sb, sa[i - 1], Constants.COLON);
			}
		}
		return ffl.stream().map(StringBuilder::toString).collect(Collectors.toList());
	}
	
	/**
	 * calculates the AD for a sample based on the ref, alts, and OABS
	 * Should be safe to use for snps and compound snps alike - yay
	 * 
	 * Will return the missing data string should OABS empty
	 */
	public static String getAD(String ref, String alts, String oabs) {
		if (StringUtils.isNullOrEmptyOrMissingData(oabs)) {
			return Constants.MISSING_DATA_STRING;
		}
		Map<String, Integer> alleleCounts = getAllelicCoverage(oabs);
		if (alleleCounts.isEmpty()) {
			return Constants.MISSING_DATA_STRING;
		}
		
		StringBuilder sb = new StringBuilder();
		StringUtils.updateStringBuilder(sb, alleleCounts.getOrDefault(ref, 0).toString(), Constants.COMMA);
		for (String alt : alts.split(Constants.COMMA_STRING)) {
			StringUtils.updateStringBuilder(sb, alleleCounts.getOrDefault(alt, 0).toString(), Constants.COMMA);
		}
		
		return sb.length() > 0 ? sb.toString() : Constants.MISSING_DATA_STRING;
	}
	
	public static Map<String, Integer> getAllelicCoverage(String oabs) {
		
		/*
		 * need to decompose the OABs string into a map of string keys and corresponding counts
		 */
		if ( ! StringUtils.isNullOrEmptyOrMissingData(oabs)) {
			
			String [] a = oabs.split(Constants.SEMI_COLON_STRING);
			Map<String, Integer> m = new HashMap<>(a.length * 2);
			
			for (String pileup : a) {
				int openBracketIndex = pileup.indexOf(Constants.OPEN_SQUARE_BRACKET);
				/*
				 * only populate map if we have a valid OABS
				 */
				if (openBracketIndex > -1) {
					int startOfNumberIndex = 1;
					for (int i = 1 ; i < openBracketIndex ; i++) {
						char c = pileup.charAt(i);
						if (Character.isDigit(c)) {
							/*
							 * end of the line
							 */
							startOfNumberIndex = i;
							break;
						}
					}
					
					/*
					 * get fs + rs count
					 */
					int fsCount = Integer.parseInt(pileup.substring(startOfNumberIndex, openBracketIndex));
					int rsCount = Integer.parseInt(pileup.substring(pileup.indexOf(Constants.CLOSE_SQUARE_BRACKET) + 1, pileup.indexOf(Constants.OPEN_SQUARE_BRACKET, openBracketIndex + 1)));
					m.put(pileup.substring(0, startOfNumberIndex),  fsCount + rsCount);
				}
			}
			return m;
		}
		
		return Collections.emptyMap();
	}
	
	public static Map<String, Integer> getAllelicCoverageFromAC(String ac) {
		
		/*
		 * need to decompose the OABs string into a map of string keys and corresponding counts
		 * A7[41.29],9[38.56],C14[38.43],12[37.83]
		 */
		if ( ! StringUtils.isNullOrEmptyOrMissingData(ac)) {
			
			String [] a = ac.split(Constants.COMMA_STRING);
			Map<String, Integer> m = new HashMap<>(a.length * 2);
			
			for (int j = 0 ; j < a.length ; j=j+2) {
				String pileup = a[j];
				int openBracketIndex = pileup.indexOf(Constants.OPEN_SQUARE_BRACKET);
				int startOfNumberIndex = 1;
				for (int i = 1 ; i < openBracketIndex ; i++) {
					char c = pileup.charAt(i);
					if (Character.isDigit(c)) {
						/*
						 * end of the line
						 */
						startOfNumberIndex = i;
						break;
					}
				}
				
				/*
				 * get fs + rs count
				 */
				int fsCount = Integer.parseInt(pileup.substring(startOfNumberIndex, openBracketIndex));
				String rs = a[j+1];
				openBracketIndex = rs.indexOf(Constants.OPEN_SQUARE_BRACKET);
				int rsCount = Integer.parseInt(rs.substring(0, openBracketIndex));
				m.put(pileup.substring(0, startOfNumberIndex),  fsCount + rsCount);
			}
			return m;
		}
		
		return Collections.emptyMap();
	}
	
	/**
	 * Returns true if the vcf record has been merged by the q3vcftools merge program, which inserts into the info field the inputs from which the vcf record originated.
	 * 
	 *  eg. if "IN=1" is present in the info field, it would indicate that the vcf record originated from the first input vcf file (as specified in the vcf header)
	 *  if "IN=1,2" is present in the info field, it would indicate that the vcf record originated from the first and second input vcf files (as specified in the vcf header)
	 *  
	 * @param vcf
	 * @return true if the info field contains "IN=" followed by a comma separated list of more than 1 number
	 */
	public static boolean isMergedRecord(VcfRecord vcf) {
		VcfInfoFieldRecord info =vcf.getInfoRecord();
		if (null != info) {
			String mergeInfo = info.getField(Constants.VCF_MERGE_INFO);
			
			if ( ! StringUtils.isNullOrEmpty(mergeInfo)) {
				return mergeInfo.contains(Constants.COMMA_STRING);
			}
		}
		return false;
	}
	
	public static boolean isCompoundSnp(VcfRecord vcf) {
		int refLength = vcf.getRef().length();
		if (refLength > 1) {
			String alt = vcf.getAlt();
			if (alt.indexOf(Constants.COMMA) > -1) {
				/*
				 * all alts need to be of the same length, and also equal to refLength
				 */
				String [] alts = alt.split(Constants.COMMA_STRING);
				for (String a : alts) {
					if (a.length() != refLength) {
						return false;
					}
				}
				return true;
			} else {
				/*
				 * single alt, just check the length
				 */
				return alt.length() == refLength;
			}
		}
		return false;
	}
	
	public static String getGenotypeFromGATKVCFRecord(VcfRecord rec) {
		if (null == rec)
			throw new IllegalArgumentException("VCFRecord passed to VcfUtils.getGenotypeFromGATKVCFRecord is null");
		
		List<String> formats = rec.getFormatFields();
		if (null == formats || formats.size() < 2) {
			throw new IllegalArgumentException("VCFRecord passed to VcfUtils.getGenotypeFromGATKVCFRecord does not contain the appropriate number of format fields. rec: [" + (null != rec ? rec.toString() : null) + "]");
		}
		
		/*
		 * If format contains GT then it must be at the start of the format field 
		 */
		if ( ! formats.get(0).startsWith("GT")) {
			throw new IllegalArgumentException("VCFRecord passed to VcfUtils.getGenotypeFromGATKVCFRecord contains a format field that is malformed: rec: [" + (null != rec ? rec.toString() : null) + "]");
		}
		final String extraField = formats.get(1);	// second item in list should have pertinent info
		if (null == extraField || extraField.length() < 3) {
			throw new IllegalArgumentException("VCFRecord passed to VcfUtils.getGenotypeFromGATKVCFRecord contains a format field that is malformed: rec: [" + (null != rec ? rec.toString() : null) + "]");
		}
		return extraField.substring(0,3);
	}
	
	public static GenotypeEnum getGEFromGATKVCFRec(VcfRecord rec) {
		if (rec.hasFormatFields()) {
			final String genotypeString = getGenotypeFromGATKVCFRecord(rec);
			return calculateGenotypeEnum(genotypeString, rec.getRefChar(), rec.getAlt().charAt(0));
		} 
		return null;
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
	 * Returns the alleles that the genotype corresponds to based on the supplied genotype, reference and alt alleles string
	 * 
	 * @param genotype
	 * @param ref
	 * @param alts
	 * @return
	 */
	public static String[] getAlleles(String genotype, String ref, String alts) {
		if (StringUtils.isNullOrEmptyOrMissingData(genotype) || Constants.MISSING_GT.equals(genotype)
				|| StringUtils.isNullOrEmptyOrMissingData(ref)) {
			return new String[]{};
		}
		int gt1 = Integer.parseInt(genotype.charAt(0) + "");
		int gt2 = Integer.parseInt(genotype.charAt(2) + "");
		
		String[] array = new String[2];
		String [] altsArr = alts.split(Constants.COMMA_STRING);
		array[0] = gt1 == 0 ? ref : altsArr[gt1 - 1];
		array[1] = gt2 == 0 ? ref : altsArr[gt2 - 1];
		return array;
	}
	
	/**
	 * Returns a map of string arrays that represent the format field records
	 * @param ff
	 * @return
	 */
	public static Map<String, String[]> getFormatFieldsAsMap(List<String> ff) {
		if (null != ff &&  ! ff.isEmpty()) {
			int ffSize = ff.size();
			String [] headerFields = TabTokenizer.tokenize(ff.get(0), Constants.COLON);
			
			Map<String, String[]> ffm = new HashMap<>((int)(headerFields.length * 1.5));
			
			for (String s : headerFields) {
				ffm.computeIfAbsent(s, (z) -> new String[ffSize-1]);
			}
			
			for (int i = 1 ; i < ffSize ; i++) {
				String [] sa = TabTokenizer.tokenize(ff.get(i), Constants.COLON);
				
				for (int j = 0, len = sa.length ; j < len ; j++) {
					ffm.get(headerFields[j])[i - 1] = sa[j];
				}
			}
			return ffm;
		}
		return Collections.emptyMap();
	}
	
	public static Map<String, String[]> getFormatFieldsAsMap(String ff) {
		return getFormatFieldsAsMap(Arrays.asList(ff.split(Constants.TAB_STRING)));
	}
	
	/**
	 * generic method to retrieve a field from the format fields.
	 * User needs to specify which field to retrieve, and from which sample (position)
	 * position must be zero-based (ie. first column is 0).
	 * @param ffs
	 * @param key
	 * @param position
	 */
	public static String getFormatField(List<String> ffs, String key, int position) {
		return getFormatField(getFormatFieldsAsMap(ffs), key, position);
	}
	public static String getFormatField(Map<String, String[]> ffMap, String key, int position) {
		if ( ! StringUtils.isNullOrEmptyOrMissingData(key) && null != ffMap && ! ffMap.isEmpty()) {
			
			String[] keyArray = ffMap.get(key);
			if (null != keyArray && keyArray.length > position) {
				return keyArray[position];
			}
		}
		return null;
	}
	
	/**
	 * Get GT from genotype, ref, alt
	 * @param altsString
	 * @param ref
	 * @param ge
	 * @return
	 */
	public static String getGTStringWhenAltHasCommas(String altsString, char ref, GenotypeEnum ge) {
		String result = Constants.MISSING_GT;
		if (ge != null && ! StringUtils.isNullOrEmpty(altsString)) {
			if (ge.containsAllele(ref)) {
				if (ge.isHeterozygous()) {
					int index = altsString.indexOf(ref == ge.getFirstAllele() ? ge.getSecondAllele() : ge.getFirstAllele());
					result = "0/" + ((index / 2) + 1);
				} else {
					result = "0/0";
				}
			} else {
				int index1 = altsString.indexOf(ge.getFirstAllele());
				int index2 = altsString.indexOf(ge.getSecondAllele());
				result = ((index1 / 2) + 1) + "/" + ((index2 / 2) + 1);
			}
		}
		return result;
	}
	 
	/**
	 * it adjust the endposition of cp if it is not corrected
	 * @param cp: ChrPosition of variant position
	 * @param id: String of variant id can be null
	 * @param ref: String of reference base, it will turn to "." if null
	 * @param alt: String of allel base can be null
	 * @return a vcfRecord
	 */
	public static VcfRecord createVcfRecord(ChrPosition cp, String id, String ref, String alt) {
 			
		if (ref != null   && (cp.getEndPosition() - cp.getStartPosition() + 1 ) != ref.length()) {
			return(new VcfRecord.Builder(cp.getChromosome(),cp.getStartPosition(), ref)).id(id).allele(alt).build();
		}
		if (cp instanceof ChrPointPosition) {
			
			return new VcfRecord.Builder(cp, ref).id(id).allele(alt).build();
		} else {
			return new VcfRecord.Builder(cp.getChromosome(),cp.getStartPosition(), ref).id(id).allele(alt).build();
			
		}
	}
	
	public static VcfRecord createVcfRecord(String chr, int position) {
		return new VcfRecord.Builder(chr, position).build();
	}
	
	public static VcfRecord resetAllel(VcfRecord re, String alt) {
		VcfRecord re1  =  new VcfRecord.Builder(re.getChrPosition(), re.getRef(), alt)
				.id(re.getId()).qualString(re.getQualString()).filter(re.getFilter()).build();
		
		re1.setInfo(re.getInfo());
		re1.setFormatFields(re.getFormatFields());
		
		return re1; 
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
	
	public static boolean isRecordASnpOrMnp(VcfRecord vcf) {
		if (null != vcf) {
			final String ref = vcf.getRef();
			final String alt = vcf.getAlt();
			
			if ( ! StringUtils.isNullOrEmpty(ref) && ! StringUtils.isNullOrEmpty(alt)) {
				final int refLength = ref.length();
				
				if ( ! alt.contains(Constants.COMMA_STRING)) {
					
					/*
					 * just check lengths match
					 */
					return refLength == alt.length();
				} else {
					String [] alts = alt.split(Constants.COMMA_STRING);
					/*
					 * need all of these alts to be the same length as the ref
					 */
					return Arrays.stream(alts).allMatch(s -> s.length() == refLength);
				}
			}
		}
		return false;
	}
	
	/**
	 * Takes a String representing the observed allele by strand (OABS) and returns a corresponding map where the keys are the bases seen, and the values are an int array containing counts on each strand
	 * 
	 * eg. A12[40]20[30];T5[34]9[18]
	 * would be represented by the following map:
	 * <A: int[]{12,20}>
	 * <T: int[]{5,9}>
	 * 
	 * @param oabs
	 * @return
	 */
	public static Map<String, int[]> getAllelicCoverageWithStrand(String oabs) {
		
		/*
		 * need to decompose the OABs string into a map of string keys and corresponding counts
		 */
		if ( ! StringUtils.isNullOrEmptyOrMissingData(oabs)) {
			
			String [] a = oabs.split(Constants.SEMI_COLON_STRING);
			Map<String, int[]> m = new HashMap<>(a.length * 2);
			
			for (String pileup : a) {
				int openBracketIndex = pileup.indexOf(Constants.OPEN_SQUARE_BRACKET);
				/*
				 * only populate map if we have a valid OABS
				 */
				if (openBracketIndex > -1) {
					int startOfNumberIndex = 1;
					for (int i = 1 ; i < openBracketIndex ; i++) {
						char c = pileup.charAt(i);
						if (Character.isDigit(c)) {
							/*
							 * end of the line
							 */
							startOfNumberIndex = i;
							break;
						}
					}
					/*
					 * get fs + rs count
					 */
					int fsCount = Integer.parseInt(pileup.substring(startOfNumberIndex, openBracketIndex));
					int rsCount = Integer.parseInt(pileup.substring(pileup.indexOf(Constants.CLOSE_SQUARE_BRACKET) + 1, pileup.indexOf(Constants.OPEN_SQUARE_BRACKET, openBracketIndex + 1)));
					m.put(pileup.substring(0, startOfNumberIndex), new int[] {fsCount,rsCount});
				}
			}
			return m;
		}
		return Collections.emptyMap();
	}
	
	/**
	 * Adds missing data '.' to the format field for the specified vcf record.
	 * The position parameter indicates which column should have the missing data added to it.
	 * If the position is less than 1 (the format header should always be at position 0) or greater than the 
	 * number of entries already in the format field, and IllegalArgumentException will be thrown.
	 * An insert into the list is performed ( {@link java.util.List#add(int, Object)})
	 * which shifts existing elements if required.
	 * 
	 * 
	 * @param vcf
	 * @param position
	 */
	public static void addMissingDataToFormatFields(VcfRecord vcf, int position) {
		addMissingDataToFormatFields(vcf, position, 1);
	}
	
	public static void addMissingDataToFormatFields(VcfRecord vcf, int position, int count) {
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
			int noOfColumns = formatColumns.split(Constants.COLON_STRING).length;
			for (int j = 0 ; j < count ; j++) {
				StringBuilder missingData = new StringBuilder(formatColumns.startsWith(VcfHeaderUtils.FORMAT_GENOTYPE) ? Constants.MISSING_GT : Constants.MISSING_DATA_STRING);
				for (int i = 1 ; i < noOfColumns ; i++) {
					missingData.append(":.");
				}
				formatFields.add(position, missingData.toString());
			}
			vcf.setFormatFields(formatFields);
		}
	}
	
	public static void addAdditionalSamplesToFormatField(VcfRecord vcf, List<String> additionalSampleFormatFields) {
		
		if (null == additionalSampleFormatFields || null == vcf || additionalSampleFormatFields.size() < 2) {
			throw new IllegalArgumentException("Invalid arguments passed to VcfUtils.addAdditionalSamplesToFormatField");
		}
		
		/*
		 * split list up and add 1 at a time...
		 */
		if (additionalSampleFormatFields.size() == 2) {
			addAdditionalSampleToFormatField(vcf, additionalSampleFormatFields);
		} else {
			for (int i = 1; i < additionalSampleFormatFields.size() ; i++) {
				addAdditionalSampleToFormatField(vcf, Arrays.asList(additionalSampleFormatFields.get(0), additionalSampleFormatFields.get(i)));
			}
		}
	}
	
	/**
	 * Attempts to add an additional samples format field to the existing vcf records format fields.
	 * 
	 */
	public static void addAdditionalSampleToFormatField(VcfRecord vcf, List<String> additionalSampleFormatFields) {
		if ( null != additionalSampleFormatFields && additionalSampleFormatFields.size() > 1) {
			
			List<String[]> additionalSampleFormatFieldsParams = additionalSampleFormatFields.stream().map(s -> s.split(Constants.COLON_STRING)).collect(Collectors.toList());
			List<String> existingFF = vcf.getFormatFields();
			
			if( existingFF.isEmpty()) {
				throw new IllegalArgumentException("can't append additionalSampleFormatField to vcf with null on format column: " + vcf.toString());
			}
			
			int noOfExistingSamples = existingFF.size() -1;
			Map<String, String[]> ffMap = getFormatFieldsAsMap(existingFF);
			
			int noOfNewSamples = additionalSampleFormatFields.size() - 1;
			int updatedNumberOfSamples = noOfExistingSamples + noOfNewSamples;
			/*
			 * loop through existing fields, if new sample has data for it, add, otherwise using missing data
			 * if new sample has additional fields, add to map, inserting missing data for any existing samples
			 * easy huh?
			 */
			String [] emptyFF = new String[noOfExistingSamples];
			Arrays.fill(emptyFF, Constants.MISSING_DATA_STRING);
			
			String [] newSampleHeaders = additionalSampleFormatFieldsParams.get(0);
			for (int i = 0 ; i < newSampleHeaders.length ; i++) {
				String key = newSampleHeaders[i];
				String [] ff = ffMap.computeIfAbsent(key, v -> emptyFF);
				
				for (int j = 1 ; j <= noOfNewSamples ; j++) {
					String [] newSampleData = additionalSampleFormatFieldsParams.get(j);
					ff = Arrays.copyOf(ff, ff.length + 1);
					ff[noOfExistingSamples + j -1 ] = newSampleData[i];
					ffMap.put(key,  ff);
				}
			}
			
			/*
			 * loop through map, and append missing data to any values that aren't of the correct length
			 */
			List<String> keysToBeefUp = ffMap.entrySet().stream().filter(e -> e.getValue().length < updatedNumberOfSamples).map(e -> e.getKey()).collect(Collectors.toList());
			for (String s : keysToBeefUp) {
				String[] ff = ffMap.get(s);
				int origLen = ff.length;
				ff =  Arrays.copyOf(ff, updatedNumberOfSamples);
				for (int d = origLen ; d < updatedNumberOfSamples ; d++) {
					ff[d] = VcfHeaderUtils.FORMAT_GENOTYPE.equals(s) ? Constants.MISSING_GT : Constants.MISSING_DATA_STRING;
				}
				ffMap.put(s, ff);
			}
			
			/*
			 * finally, update the vcf record with the updated map
			 */
			List<String> updatedFFs = convertFFMapToList(ffMap);
			vcf.setFormatFields(updatedFFs);
		}
	}
	
	public static void addFormatFieldsToVcf(VcfRecord vcf, List<String> additionalFormatFields) {
		 addFormatFieldsToVcf(vcf,  additionalFormatFields, false, '\u0000');
	}
	
	public static void addFormatFieldsToVcf(VcfRecord vcf, List<String> additionalFormatFields, boolean appendInfo, char separator) {
		if ( null != additionalFormatFields && ! additionalFormatFields.isEmpty()) {
			// if there are no existing format fields, set field to be additional..
			
			List<String> existingFF = vcf.getFormatFields();
			
			if (null == existingFF || existingFF.isEmpty()) {
				vcf.setFormatFields(additionalFormatFields);
			} else {
				
				// check that the 2 lists of the same size
				if (existingFF.size() != additionalFormatFields.size()) {
					logger.warn("format field size mismatch. Existing record has " 
				+ existingFF.size() + " entries, whereas additionalFormatFields has " 
							+ additionalFormatFields.size() + " entries - skipping addition");
					logger.info("vcf: " + vcf.toString() + ", additional fields: " + Arrays.deepToString(additionalFormatFields.toArray(new String[]{})));
					
				} else {
					// need to check each element to see if it already exists...
					final String [] formatFieldAttributes = additionalFormatFields.get(0).split(COLON_STRING);
					final String existingFieldAttributes = existingFF.get(0);
					
					for (int i = 0 ; i < formatFieldAttributes.length ; i++) {
						
						final String s = formatFieldAttributes[i];
						
						if (existingFieldAttributes.contains(s)) {
							if (appendInfo) {
								/*
								 * Don't update header field, append value to existing value (if different)
								 */
								
								/*
								 * Need position in existing array of this attribute
								 */
								String [] existingFieldAttributesArray = existingFieldAttributes.split(COLON_STRING);
								int index = 0;
								for (int x = 0 ; x < existingFieldAttributesArray.length ; x++) {
									if (existingFieldAttributesArray[x].equals(s)) {
										index = x;
									}
								}
								
								for (int j = 1 ; j < additionalFormatFields.size() ; j++) {
									String additionalValue = additionalFormatFields.get(j).split(COLON_STRING)[i];
									// get existing entry 
									String [] existingArray = existingFF.get(j).split(COLON_STRING);
									String existingValue = existingArray[index];
									
									if (StringUtils.isNullOrEmptyOrMissingData(existingValue)) {
									/*
									 * always want to put delimited into place if we are in appendInfo mode
									 */
										existingArray[index] = additionalValue;
									} else {
										existingArray[index] = existingValue + separator + additionalValue;
									}
									// re-insert into vcf
									existingFF.set(j, Arrays.stream(existingArray).collect(Collectors.joining(Constants.COLON_STRING)));
								}
							}
						} else {
							// add this one
								
							for (int j = 0 ; j < additionalFormatFields.size() ; j++) {
								// get existing entry 
								final String existing = existingFF.get(j);
								// create new
								final String newEntry = existing + COLON + additionalFormatFields.get(j).split(COLON_STRING)[i];
								
								// re-insert into vcf
								existingFF.set(j, newEntry);
							}
						}
					}
					
					if ( ! existingFF.isEmpty()) {
						vcf.setFormatFields(existingFF);
					}
				}
			}
		}
	}
	
	public static String calculateAD(String oldAD, String newGt, String oldGt) {
		if (newGt.equals(oldGt)) {
			return oldAD;
		}
		String [] adArray = TabTokenizer.tokenize(oldAD, Constants.COMMA);
		int newgt1 = Integer.parseInt("" + newGt.charAt(0));
		int newgt2 = Integer.parseInt("" + newGt.charAt(2));
		int oldgt1 = Integer.parseInt("" + oldGt.charAt(0));
		int oldgt2 = Integer.parseInt("" + oldGt.charAt(2));
		
		if (Math.max(oldgt1, oldgt2) >= Math.max(newgt1, newgt2)) {
			return oldAD;
		}
		
		String [] newADArray = new String[Math.max(newgt1,  newgt2) + 1];
		
		int i = 0;
		for (String s : adArray) {
				
			if (i == oldgt1) {
				newADArray[oldgt1 != newgt1 ? newgt1 : i] = s;
			} else if ( i == oldgt2) {
				newADArray[oldgt2 != newgt2 ? newgt2 : i] = s;
			} else {
				newADArray[i] = s;	
			}
				
			i++;
		}
		
		/*
		 * loop through and set any null elements to .
		 * ACTUALLY, set this to 0 rather than missing data
		 */
		for (int j = 0 ; j < newADArray.length ; j++) {
			if (null == newADArray[j]) {
				newADArray[j] = "0";
			}
		}
		
		return Arrays.stream(newADArray).collect(Collectors.joining(Constants.COMMA_STRING));
	}
	
	
	/**
	 * Will return a Optional<VcfRecord> that represents the smooshing together of the supplied control and test VCfRecords that are assumed to be GATK generated.
	 * If control OR test is null, will return the non-null one, with an additional entry in the format field containing the missing data char.
	 * If control AND test are null, returns an empty Optional
	 * 
	 * If both control and test exist, will merge the records into a single record with (hopefully) all required data populated.
	 * 
	 * @param control
	 * @param test
	 * @return
	 */
	public static Optional<VcfRecord> mergeGATKVcfRecs(VcfRecord control, VcfRecord test) {
		if (null == control && null == test) {
			return Optional.empty();
		}
		if (null != test) {
			prepareGATKVcfForMerge(test);
		}
		if (null != control) {
			prepareGATKVcfForMerge(control);
		}
		
		if (null == test) {
			addMissingDataToFormatFields(control, 2);
			return  Optional.ofNullable(control);
		}
		
		if (null == control) {
			addMissingDataToFormatFields(test, 1);
			return Optional.ofNullable(test) ;
		}
		
		/*
		 * Have both control and test
		 * Strip out the quality and info fields as they will be sample specific.
		 * Check that ref is the same, and that alt is the same
		 */
		if (control.getAlt().equals(test.getAlt())) {
			List<String> ff = test.getFormatFields();
			addAdditionalSampleToFormatField(control, ff);
			return Optional.ofNullable(control);
		} else {
			
			/*
			 * alts are not the same and so need to update the GT format field to reflect the new alt layout
			 */
			String combinedAlts = control.getAlt() + Constants.COMMA + test.getAlt();
			/*
			 * Should only have to update the test gt field as the control one went first...
			 */
			VcfRecord m = createVcfRecord(control.getChrPosition(), null, control.getRef(), combinedAlts);
			m.setQualString(MISSING_DATA_STRING);
			/*
			 * need to update PL entry in format field if it exists to cater for triallelic values - just put in 0s for the additional allele
			 */
			List<String> cFF = control.getFormatFields();
			m.setFormatFields(cFF);
			
			List<String> tFFs = test.getFormatFields(); 
			String tFF = tFFs.get(1);
			String tGT = tFF.substring(0, tFF.indexOf(Constants.COLON));
			
			if (tGT.equals("0/0")) {
				// do nowt
			} else {
				/*
				 * increment each side of genotype with control alt count as long as it is non-zero
				 */
				int noOfControlAlts = control.getAlt().split(Constants.COMMA_STRING).length;
				
				int firstGT = Integer.parseInt(tGT.charAt(0) + "");
				int secondGT = Integer.parseInt(tGT.charAt(2) + "");
				if (firstGT > 0) {
					firstGT += noOfControlAlts;
				}
				if (secondGT > 0) {
					secondGT += noOfControlAlts;
				}
				String newGT = firstGT + Constants.SLASH_STRING + secondGT;
				tFF = tFF.replace(tGT, newGT);
				tFFs.remove(1);
				tFFs.add(tFF);
				Map<String, String [] > ffMap = getFormatFieldsAsMap(tFFs);
				String [] adArray = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
				if (null != adArray && adArray.length > 0) {
					String newAD = calculateAD(adArray[0] , newGT, tGT);
					adArray[0] = newAD;
					tFFs = convertFFMapToList(ffMap);
				}
				
				addAdditionalSampleToFormatField(m, tFFs);
			}
			return Optional.ofNullable(m);
		}
	}
	
	public static final void prepareGATKVcfForMerge(VcfRecord v) {
		/*
		 * need to remove AC and AN from info field
		 */
		removeElementsFromInfoField(v);
		Map<String, String[]> ffMap = getFormatFieldsAsMap(v.getFormatFields());
		ffMap.remove("PL");
		
		/*
		 * make sure record has DP,AD,QL field - if not add with missing data
		 */
		ffMap.computeIfAbsent(VcfHeaderUtils.FORMAT_READ_DEPTH, f -> new String[]{Constants.MISSING_DATA_STRING});
		ffMap.computeIfAbsent(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS, f -> new String[]{Constants.MISSING_DATA_STRING});
		ffMap.computeIfAbsent(VcfHeaderUtils.FORMAT_QL, f -> new String[]{StringUtils.isNullOrEmptyOrMissingData(v.getQualString()) ? Constants.MISSING_DATA_STRING : v.getQualString()});
		/*
		 * reset qual field as it won't be applicable once merged
		 */
		v.setQualString(MISSING_DATA_STRING);
		v.setFormatFields(convertFFMapToList(ffMap));
	}
	
	public static void removeElementsFromInfoField(VcfRecord vcf) {
		removeElementsFromInfoField(vcf, "AN", "AC", "AF","MLEAF","MLEAC");
	}
	
	public static void removeElementsFromInfoField(VcfRecord vcf, String ... elements) {
		VcfInfoFieldRecord i = vcf.getInfoRecord();
		if (null != i) {
			for (String s : elements) {
				i.removeField(s);
			}
		}
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
	
	/**
	 * Checks to see if the existing annotation is a PASS (or missing data symbol (".")).
	 * If it is, then the annotation is replaced with the supplied annotation.
	 * If its not, the supplied annotation is appended to the existing annotation(s)
	 * 
	 * Also, if the supplied annotation is a PASS, then all previous annotations are removed.
	 * 
	 * @param rec qsnp record
	 * @param an String representation of the annotation
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
	
	/**
	 * For SOMATIC records, if all format columns have a PASS in them, then we are a PASS.
	 * For Germline, only care about the control column(s) being a PASS
	 * @param v
	 * @return
	 */
	public static boolean isRecordAPass(VcfRecord v) {
		Map<String, String[]> ffMap = getFormatFieldsAsMap(v.getFormatFields());
		return isRecordAPass(ffMap);
	}
	
	public static boolean isRecordAPass(Map<String, String[]> ffMap) {
		String [] filterArray = ffMap.get(VcfHeaderUtils.FORMAT_FILTER);
		Optional<Boolean> shortCut = getShortCutPassFail(filterArray);
		if (shortCut.isPresent()) {
			return shortCut.get().booleanValue();
		}
		
		String [] infoArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		long somCount =   null != infoArr ? Arrays.asList(infoArr).stream().filter(f -> f.contains(VcfHeaderUtils.INFO_SOMATIC)).count() : 0;
		/*
		 * germline, just look at pass in control fields
		 */
		if (somCount == 0) {
			int i = 0;
			boolean allPasses = true;
			for (String s : filterArray) {
				if (i++ %2 == 0) {
					if ( ! s.equals(VcfHeaderUtils.FILTER_PASS)) {
						allPasses = false;
						break;
					}
				}
			}
			return allPasses;
		}
		return false;	
	}
	
	/**
	 * Returns true if the VcfRecord is deemed to have passed any applied filters, and false otherwise
	 * 
	 * @param v
	 * @param meta
	 * @return
	 */
	public static boolean isRecordAPass(VcfRecord v, VcfFileMeta meta) {
		if (null == meta) {
			return isRecordAPass(v);
		}
		return isRecordAPass(v.getFormatFieldsAsMap(), meta, v.getInfo(), isCompoundSnp(v));
	}
	
	public static boolean isRecordAPass(Map<String, String[]> ffMap, VcfFileMeta meta, String infoField, boolean compoundSnp) {
		String [] filterArray = ffMap.get(VcfHeaderUtils.FORMAT_FILTER);
		
		Optional<Boolean> shortCut = getShortCutPassFail(filterArray);
		if (shortCut.isPresent()) {
			return shortCut.get().booleanValue();
		}
		
		/*
		 * if record is a regular snp, check to see if it is somatic
		 * If somatic, need all samples to carry the PASS
		 * If germline, need all control samples to carry the PASS
		 * 
		 * If its a compound snp and 
		 */
		String [] infoArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		long somaticCount = null != infoArr ? Arrays.asList(infoArr).stream().filter(f -> f.contains(VcfHeaderUtils.INFO_SOMATIC)).count() : 0;
		if (compoundSnp) {
			
			/*
			 * If its a compound snp, there is an assumption that this will have only been called by 
			 * 1 caller (qsnp) and also that qsnp was the first caller.
			 */
			
			short [] positions = somaticCount > 0 ?  new short[]{(short) meta.getFirstControlSamplePos(), (short)meta.getFirstTestSamplePos()}
													: new short[]{(short) meta.getFirstControlSamplePos()};
			return areTheseFilterFieldsAPass(filterArray, positions);
			
		} else {
			
			/*
			 * Single Nucleotide Polymorphism
			 */
			if (somaticCount == 0) {
				return areTheseFilterFieldsAPass(filterArray, meta.getAllControlPositions().toArray());
			} else {
				/*
				 * somatic, we need all fields to be PASS, but that is not the case (getShortCutPassFail didn't give us a pass)
				 * and so will fall through to returning false
				 */
			}
		}
		
		return false;
	}
	
	/**
	 * If the filter fields String array contains "PASS" at the positions (1-based) in the positions short array, return true.
	 * False otherwise
	 * 
	 * @param filterFields
	 * @param positions
	 * @return
	 */
	public static boolean areTheseFilterFieldsAPass(String [] filterFields, short[] positions) {
		if (null != filterFields && filterFields.length > 0 
				&& null != positions && positions.length > 0) {
			
			boolean allGood = true;
			for (int i : positions) {
				if (i > 0 && i <= filterFields.length) {
					if ( ! VcfHeaderUtils.FILTER_PASS.equals(filterFields[i - 1])) {
						allGood =  false;
					}
				} else {
					allGood = false;
				}
			}
			return allGood;
		}
		return false;
	}
	
	public static Optional<Boolean> getShortCutPassFail(String [] filterArr) {
		if (null == filterArr || filterArr.length == 0) {
			return Optional.of(false);
		}
		
		int passCount = 0;
		for (String s : filterArr) {
			if (VcfHeaderUtils.FILTER_PASS.equals(s)) {
				passCount++;
			}
		}
		
		/*
		 * If we have no PASS, return optional false
		 */
		if (passCount == 0) {
			return Optional.of(false);
		}
		
		/*
		 * if all columns have a pass, return optional true
		 */
		if (passCount == filterArr.length) {
			return Optional.of(true);
		}
		
		/*
		 * otherwise return empty optional
		 */
		return Optional.empty();
	}
	
	/**
	 * If germline, only examine control sample filter field. If this is a PASS, check out the corresponding test sample to make sure the coverage is decent.
	 * If somatic, only examine test sample filter field. If this is a PASS, check out the corresponding control sample to make sure the coverage is decent.
	 * 
	 * @param v
	 * @return
	 */
	public static boolean isRecordAPassOldSkool(VcfRecord v) {
		
		Map<String, String[]> ffMap = getFormatFieldsAsMap(v.getFormatFields());
		String [] filterArr = ffMap.get(VcfHeaderUtils.FORMAT_FILTER);
		if (null == filterArr) {
			return false;
		}
		
		long passCount = Arrays.stream(filterArr).filter(f ->VcfHeaderUtils.FILTER_PASS.equals(f)).count();
		
		if (passCount == filterArr.length) {
			/*
			 * PASS in all filter fields - thumbs up!
			 */
			return true;
		}
		
		/*
		 * if we are in single sample mode and we don't have a PASS, then FAIL
		 */
		if (filterArr.length == 1) {
			return false;
		}
		
		String [] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		String [] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
		boolean pass = true;
		for (int i = 0 ; i < filterArr.length ; i++) {
			if (pass && i % 2 ==0) {
				if ( ! samplesPass(filterArr[i], (null != dpArr ? dpArr[i] : null), filterArr[i + 1], (null != infArr ? infArr[i+1] : null), (null != dpArr ? dpArr[i+1] : null))) {
					pass = false;
				}
			}
		}
		return pass;
	}
	
	public static boolean samplesPass(String controlFilter, String controlCov, String testFilter, String testInfo, String testCov) {
		/*
		 * If both filters are pass, great
		 */
		if (VcfHeaderUtils.FILTER_PASS.equals(controlFilter) && VcfHeaderUtils.FILTER_PASS.equals(testFilter)) {
			return true;
		}
		
		/*
		 * if neither filter are pass, not so great
		 */
		if ( ! VcfHeaderUtils.FILTER_PASS.equals(controlFilter) && ! VcfHeaderUtils.FILTER_PASS.equals(testFilter)) {
			return false;
		}
		
		/*
		 * if testInfo contains SOMATIC, then testFilter must equal PASS and control coverage must be up to scratch
		 */
		if (null != testInfo && testInfo.contains(VcfHeaderUtils.INFO_SOMATIC)) {
			return VcfHeaderUtils.FILTER_PASS.equals(testFilter) && null != controlCov && Integer.parseInt(controlCov) > 8;
		} else {
			return VcfHeaderUtils.FILTER_PASS.equals(controlFilter) && null != testCov &&Integer.parseInt(testCov) > 8;
		}
	}

	/**
	 * returns true if the info field contains SOMATIC, or if more than half of format fields contain SOMATIC (this should mean all callers called this somatic, as we are only populating the test info field with this tag)
	 */
	public static boolean isRecordSomatic(VcfRecord rec) {
		return isRecordSomatic(rec.getInfo(), rec.getFormatFieldsAsMap());
	}
	
	public static boolean isRecordSomatic(String info, Map<String, String[]> formatFields) {
		if ( ! StringUtils.isNullOrEmpty(info) && info.contains(VcfHeaderUtils.INFO_SOMATIC)) {
			return true;
		}
		
		if (null == formatFields || formatFields.isEmpty()) {
			return false;
		}
		String [] infos = formatFields.get(VcfHeaderUtils.FORMAT_INFO);
		if (null == infos || infos.length == 0) {
			return false;
		}
		int somCount =  getSomaticCountFromFormatInfoFields(infos);
		if (0 == somCount) {
			return false;
		}
		
		/*
		 * need to see how many callers contributed to this call
		 * If 1, then need a single SOMATIC
		 * If 2, need 2 SOMATICs etc.
		 */
		OptionalInt callerCount = getCallerCount(info);
		if (callerCount.isPresent()) {
			return somCount >= callerCount.getAsInt();
		}
		
		/*
		 * default to pre-existing behaviour should caller count not be present
		 */
		return somCount * 2 >= infos.length;
	}
	
	/**
	 * Will return the number of callers used to make this call as depicted in the "IN=.." entry in the INFO field.
	 * If the INFO field contains IN=1,2 then OptionalInt.of(2) will be returned
	 * If the INFO field contains IN=1 or IN=2, then OptionalInt(1) will be returned
	 * If the INFO field doesn't contain IN then OptionalInt.empty() will be returned
	 * @param info
	 * @return
	 */
	public static OptionalInt getCallerCount(String info) {
		if ( ! StringUtils.isNullOrEmptyOrMissingData(info) && info.contains("IN=")) {
			int commaCount = (int) new VcfInfoFieldRecord(info).getField(Constants.VCF_MERGE_INFO).chars().filter(c -> c == Constants.COMMA).count();
			return OptionalInt.of(commaCount + 1);
		}
		return OptionalInt.empty();
	}

	/**
	 * Returns the number of times the string SOMATIC appears in the format info (INF) fields
	 * @param infos
	 * @return
	 */
	public static int getSomaticCountFromFormatInfoFields(String [] infos) {
		if (null != infos && infos.length > 0) {
			return (int) Arrays.asList(infos).stream().filter(f -> null != f && f.contains(VcfHeaderUtils.INFO_SOMATIC)).count();
		}
		return 0;
	}
	
	/**
	 * Returns the confidence of a VcfRecord, which is found in the info field.
	 * Returns null should the record be null, or the CONF value not be set in the info field.
	 * 
	 * For merged records
	 * 
	 * @param rec
	 * @return
	 */
	public static String getConfidence(VcfRecord rec) {
		String info = rec.getInfo();
		
		if (StringUtils.isNullOrEmpty(info)) {
			return null;
		}
		int index = info.indexOf(VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ);
		if (index == -1) {
			return null;
		}
		int scIndex = info.indexOf(Constants.SEMI_COLON_STRING, index);
		String conf = info.substring(index + CONF_LENGTH, scIndex > -1 ? scIndex : info.length());
		return conf;
	}



	/**
	 * will return true if:
	 * altCount is >= max(minValue, percentage * (totalReadCount / 100))
	 * 
	 * @param altCount
	 * @param totalReadCount
	 * @param percentage
	 * @param minCoverage
	 * @return
	 */
	public static boolean mutationInNormal(int altCount, int totalReadCount, float percentage, int minValue) {
		if (altCount == 0 || altCount < minValue) {
			return false;
		}
		/*
		 * Need to get the max(minValue, percentage * totalReadCount) and see if the alt count is larger than that
		 */
		return altCount >= Math.max(minValue, ((float) totalReadCount / 100) * percentage);
	}
	
	public static boolean mutationInNormal(int altCount, int totalReadCount, int percentage, int maxCoverage) {
		return mutationInNormal(altCount, totalReadCount, (float) percentage, maxCoverage);
	}
}
