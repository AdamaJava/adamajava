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
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.ListUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;



public class VcfUtils {
	
	private static final QLogger logger = QLoggerFactory.getLogger(VcfUtils.class);
	
	public static final Pattern pattern_AC = Pattern.compile("[ACGT][0-9]+\\[[0-9]+.?[0-9]*\\],[0-9]+\\[[0-9]+.?[0-9]*\\]");
	public static final Pattern pattern_ACCS = Pattern.compile("[ACGT_]+,[0-9]+,[0-9]+");
	public static final int CONF_LENGTH = (VcfHeaderUtils.INFO_CONFIDENCE + Constants.EQ).length();

	
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
	
	public static Map<String, Integer> getAllelicCoverageFromOABS(String oabs) {
		
		/*
		 * need to decompose the OABs string into a map of string keys and corresponding counts
		 */
		if ( ! StringUtils.isNullOrEmptyOrMissingData(oabs)) {
			
			String [] a = oabs.split(Constants.SEMI_COLON_STRING);
			Map<String, Integer> m = new HashMap<>(a.length * 2);
			
			for (String pileup : a) {
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
				int rsCount = Integer.parseInt(pileup.substring(pileup.indexOf(Constants.CLOSE_SQUARE_BRACKET) + 1, pileup.indexOf(Constants.OPEN_SQUARE_BRACKET, openBracketIndex + 1)));
				m.put(pileup.substring(0, startOfNumberIndex),  fsCount + rsCount);
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
	 * Gets just the filters that end in the suppled suffix
	 * USed for merged vcf records where the input file position is appended to each filter value
	 * eg. PASS_1 indicates that input file 1 deemed this position a PASS
	 * 
	 * @param rec
	 * @param suffix
	 * @return
	 */
	public static String getFiltersEndingInSuffix(VcfRecord rec, String suffix) {
		if (rec == null || suffix == null) {
			throw new IllegalArgumentException("Null VcfRecord or suffix passed to getFiltersEndingInSuffix");
		}
		String filter = rec.getFilter();
		if (null == filter) {
			throw new IllegalArgumentException("Null VcfRecord or suffix passed to getFiltersEndingInSuffix");
		}
		String [] params = filter.split(Constants.SEMI_COLON_STRING);
		return Arrays.stream(params).filter(s -> s.endsWith(suffix)).collect(Collectors.joining(Constants.SEMI_COLON_STRING));
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
		String ff = vcf.getFormatFieldStrings();
		return ff.contains(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP);
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
//			System.err.println("incorrect index for format field: " + format);
			return -1;
		}
		
		final int thirdIndex = format.indexOf(":", secondIndex);
		
		if (thirdIndex == -1) {
//			System.err.println("incorrect index for format field: " + format);
			return -1;
		}
		
		final String dpString = format.substring(secondIndex, thirdIndex);
		return Integer.parseInt(dpString);
	}
	
	public static String getGenotypeFromGATKVCFRecord(VcfRecord rec) {
		if (null == rec)
			throw new IllegalArgumentException("VCFRecord passed to VcfUtils.getGenotypeFromGATKVCFRecord is null");
		
		List<String> formats = rec.getFormatFields();
		if (formats.size() < 2) {
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
	
	/**
	 * Returns a map of string arrays that represent the format field records
	 * @param ff
	 * @return
	 */
	public static Map<String, String[]> getFormatFieldsAsMap(List<String> ff) {
		Map<String, String[]> ffm = new HashMap<>();
		if (null != ff &&  ! ff.isEmpty()) {
			
			String [] headerFields = ff.get(0).split(Constants.COLON_STRING);
			
			for (String s : headerFields) {
				ffm.computeIfAbsent(s, (z) -> new String[ff.size()-1]);
			}
			
			for (int i = 1 ; i < ff.size() ; i++) {
				String [] sa = ff.get(i).split(Constants.COLON_STRING);
				
				for (int j = 0 ; j < sa.length ; j++) {
					String header = headerFields[j];
					
					ffm.get(header)[i-1] = sa[j];
				}
			}
		}
		return ffm;
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
	
	/**
	 * it adjust the endposition of cp if it is not corrected
	 * @param cp: ChrPosition of variant position
	 * @param id: String of variant id can be null
	 * @param ref: String of reference base, it will turn to "." if null
	 * @param alt: String of allel base can be null
	 * @return a vcfRecord
	 */
	public static VcfRecord createVcfRecord(ChrPosition cp, String id, String ref, String alt) {
 			
		if(ref != null   && (cp.getEndPosition() - cp.getStartPosition() +1 )!= ref.length()) {
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
	
	public static VcfRecord resetAllel(VcfRecord re, String alt){
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
	
	/**
	 * Attempts to add an additional samples format field to the existing vcf records format fields.
	 * 
	 */
	public static void addAdditionalSampleToFormatField(VcfRecord vcf, List<String> additionalSampleFormatFields) {
		if ( null != additionalSampleFormatFields && additionalSampleFormatFields.size() == 2) {
			
			List<String> existingFF = vcf.getFormatFields();
			
			if(existingFF == null)
				throw new IllegalArgumentException("can't append additionalSampleFormatField to vcf with null on format column: " + vcf.toString());
		 
			
			String newSampleFormatFields = additionalSampleFormatFields.get(0);
			String existingFormatFields =existingFF.get(0);
			
			if ( newSampleFormatFields.equals(existingFormatFields)) {
				existingFF.add(additionalSampleFormatFields.get(1));
				vcf.setFormatFields(existingFF);
			} else {
				
				Map<String, StringBuilder> ff = new HashMap<>();
				
				List<String [] > existingFFParams = new ArrayList<>();
				for (String s : existingFF) {
					existingFFParams.add(s.split(":"));
				}
				
				/*
				 * insert original data first, with missingData value at end for additional sample
				 */
				int i = 0;
				int noOfExistingSamples = existingFF.size() -1;
				for (String s : existingFFParams.get(0)) {
					StringBuilder sb = new StringBuilder();
					
					for (int j = 1 ; j <= noOfExistingSamples ; j++) {
						if (sb.length() > 0) {
							sb.append(':');
						}
						sb.append(existingFFParams.get(j)[i]);
					}
					
					sb.append(":.");	// for the additional sample
					
					ff.put(s,  sb);
					i++;
				}
				
				String [] newSampleFormatParamsHeaders = additionalSampleFormatFields.get(0).split(":");
				String [] newSampleFormatParamsData = additionalSampleFormatFields.get(1).split(":");
				
				int z = 0;
				for (String s : newSampleFormatParamsHeaders) {
					StringBuilder sb = ff.get(s);
					if (null == sb) {
						sb = new StringBuilder();
						for (int k = 0 ; k < noOfExistingSamples ; k++) {
							if (k > 0) {
								sb.append(':');
							}
							sb.append('.');
						}
						sb.append(':').append(newSampleFormatParamsData[z]);
						ff.put(s,sb);
					} else {
						sb.deleteCharAt(sb.length() -1);
						sb.append(newSampleFormatParamsData[z]);
					}
					z++;
				}
				
				/*
				 * re-populate the vcf format field
				 */
				StringBuilder header = new StringBuilder();
				List<StringBuilder> values = new ArrayList<>();
				
				/*
				 * try and maintain ordering
				 */
				String hugeHeader = existingFormatFields + ':' + newSampleFormatFields;
				for (String s : hugeHeader.split(":")) {
					
					StringBuilder sb = ff.remove(s);
					if (null != sb) {
						if (header.length() > 0) {
							header.append(':');
						}
						header.append(s);
						
						String [] params = sb.toString().split(":");
						int m = 0;
						for (String s1 : params) {
							StringBuilder sb1 =  values.size() > m ? values.get(m) : null;
							if (null == sb1) {
								sb1 = new StringBuilder(s1);
								values.add(sb1);
							} else {
								sb1.append(':').append(s1);
							}
							m++;
						}
					}
				}
				
				List<String> finalList = new ArrayList<>();
				finalList.add(header.toString());
				for (StringBuilder sb : values) {
					finalList.add(sb.toString());
				}
				vcf.setFormatFields(finalList);
				
			}
		}
	}
	public static void addFormatFieldsToVcf(VcfRecord vcf, List<String> additionalFormatFields) {
		 addFormatFieldsToVcf(vcf,  additionalFormatFields, false);
	}
	
	public static void addFormatFieldsToVcf(VcfRecord vcf, List<String> additionalFormatFields, boolean appendInfo) {
		if ( null != additionalFormatFields && ! additionalFormatFields.isEmpty()) {
			// if there are no existing format fields, set field to be additional..
			
			List<String> existingFF = vcf.getFormatFields();
			
			if (null == existingFF || existingFF.isEmpty()) {
				vcf.setFormatFields(additionalFormatFields);
			} else {
				
				// check that the 2 lists of the same size
				if (existingFF.size() != additionalFormatFields.size()) {
					logger.warn("format field size mismatch. Exiting record has " 
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
									/*
									 * always want to put delimited into place if we are in appendInfo mode
									 */
										existingArray[index] = existingValue + Constants.VCF_MERGE_DELIM + additionalValue;
//									}
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
	
	public static void removeFilter(VcfRecord rec, String filter) {
		// perform some null guarding
		if (null == rec) throw new IllegalArgumentException("Null vcf record passed to removeAnnotation");
		if (null == filter) {
			return;
		}
		
		rec.setFilter(StringUtils.removeFromString(rec.getFilter(), filter, Constants.SC));
	}

	public static boolean isRecordSomatic(VcfRecord rec) {
		String info = rec.getInfo();
		if (StringUtils.isNullOrEmpty(info) || ! info.contains(VcfHeaderUtils.INFO_SOMATIC)) {
			return false;
		}
		
		if (isMergedRecord(rec)) {
			/*
			 *SOMATIC_1 and SOMATIC_2 to return true
			 */
			return info.contains(VcfHeaderUtils.INFO_SOMATIC + "_1") && info.contains(VcfHeaderUtils.INFO_SOMATIC + "_2");  
		} else {
			return true;
		}
	}
	
	/**
	 * merges the 2 supplied alt strings.
	 * Returns a comma separated merged string containing the contents of the supplied alts
	 * This will merge alts of different lengths, so if this is not desired, please
	 * 
	 * 
	 * @param existingAlt
	 * @param newAlt
	 * @return
	 */
	public static String mergeAlts(String existingAlt, String newAlt) {
		
		if (existingAlt.equals(newAlt)) {
			return existingAlt;
		} else {
			/*
			 * possible scenarios:
			 * - both existing and new alts contain a single alt, and are different (most likely)
			 * -  existing contains multiple alts, one of which is the new alt
			 * -  existing contains single alt, new alt contains multiple one of which is the existing
			 * -  existing contains multiple alt, new alt contains multiple and they are all different
			 */
			int existingIndex = existingAlt.indexOf(Constants.COMMA_STRING);
			int newIndex = newAlt.indexOf(Constants.COMMA_STRING);
			
			if (existingIndex == newIndex && existingIndex == -1) {
				/*
				 * append and return 
				 */
				return existingAlt + Constants.COMMA + newAlt;
			} else {
				String [] existingAltsArray = existingAlt.split(Constants.COMMA_STRING);
				String [] newAltsArray = newAlt.split(Constants.COMMA_STRING);
				
				for (String ns : newAltsArray) {
					/*
					 * does this appear in the existing array?
					 */
					boolean match = false;
					for (String es : existingAltsArray) {
						if (ns.equals(es)) {
							match = true;
							break;
						}
					}
					if ( ! match) {
						existingAlt += Constants.COMMA_STRING + ns;
					}
				}
				return existingAlt;
			}
		}
	}
	
	public static String getUpdatedGT(String alts, String oldAlt, String oldGT) {
		
		if (alts.equals(oldAlt) || alts.startsWith(oldAlt + Constants.COMMA) || "0/0".equals(oldGT)) {
			/*
			 * no change
			 */
			return oldGT;
		}
		
		int index = oldGT.indexOf("/");
		int first = Integer.parseInt(oldGT.substring(0, index));
		int second = Integer.parseInt(oldGT.substring(index + 1));
		String [] oldAltArray = oldAlt.split(Constants.COMMA_STRING);
		String [] altsArray = alts.split(Constants.COMMA_STRING);
		
		int newFirst = first > 0 ?  ListUtils.positionOfStringInArray(altsArray, oldAltArray[first -1]) + 1 : first;
		int newSecond = second > 0 ?  ListUtils.positionOfStringInArray(altsArray, oldAltArray[second -1]) + 1: second;
		
		return newFirst + "/" + newSecond;
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
 
}
