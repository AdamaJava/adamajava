/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.SnpUtils;

public class VcfUtils {
	
	private final static DateFormat df = new SimpleDateFormat("yyyyMMdd");
	
	//FILTER FIELDS
	public static final String FILTER_PASS = "PASS";
	public static final String FILTER_COVERAGE_NORMAL_12 = SnpUtils.LESS_THAN_12_READS_NORMAL;
	public static final String FILTER_COVERAGE_NORMAL_8 = SnpUtils.LESS_THAN_8_READS_NORMAL;
	public static final String FILTER_COVERAGE_TUMOUR = SnpUtils.LESS_THAN_8_READS_TUMOUR;
	public static final String FILTER_GERMLINE = SnpUtils.MUTATION_GERMLINE_IN_ANOTHER_PATIENT;
	public static final String FILTER_MUTATION_IN_NORMAL = SnpUtils.MUTATION_IN_NORMAL;
	public static final String FILTER_MUTATION_IN_UNFILTERED_NORMAL = SnpUtils.MUTATION_IN_UNFILTERED_NORMAL;
	public static final String FILTER_SAME_ALLELE_NORMAL = SnpUtils.LESS_THAN_3_READS_NORMAL;
	public static final String FILTER_SAME_ALLELE_TUMOUR = SnpUtils.LESS_THAN_3_READS_TUMOUR;
	public static final String FILTER_NOVEL_STARTS = SnpUtils.NOVEL_STARTS;
	public static final String FILTER_MUTANT_READS = SnpUtils.MUTANT_READS;
	public static final String FILTER_MUTATION_EQUALS_REF = SnpUtils.MUTATION_EQUALS_REF;
	
	//INFO FIELDS
	public static final String INFO_MUTANT_READS = FILTER_MUTANT_READS;
	public static final String INFO_NOVEL_STARTS = FILTER_NOVEL_STARTS;
	public static final String INFO_MUTATION = "MU";
	public static final String INFO_FLANKING_SEQUENCE = "FS";
	public static final String INFO_DONOR = "DON";
	//FORMAT FIELDS
	public static final String FORMAT_GENOTYPE = "GT";
	public static final String FORMAT_GENOTYPE_DETAILS = "GD";
	public static final String FORMAT_ALLELE_COUNT = "AC";
	
	// standard final header line
	public static final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n";

	public static final String getHeaderForQSig(final String patientId,  final String slide, final String library,
			final String inputType, final String barcode, final String physicalDivision, 
			final String bamName, final String snpFile) {
		
		return "##fileformat=VCFv4.0\n" +
		"##patient_id=" + patientId + "\n" + 
		"##input_type=" + inputType  + "\n" +
		"##library=" + library  + "\n" +
		"##slide=" + slide + "\n" +
		"##barcode=" + barcode + "\n" + 
		"##physical_division=" + physicalDivision + "\n" + 
		"##bam=" + bamName + "\n" +
		"##snp_file=" + snpFile + "\n" + 
		"##filter_q_score=10\n" + 
		"##filter_match_qual=10\n" + 
		"##genome=GRCh37_ICGC_standard_v2.fa\n" + 
		"##FILTER=<ID=LowQual,Description=\"REQUIRED: QUAL < 50.0\">\n" + 
		"##INFO=<ID=FULLCOV,Number=.,Type=String,Description=\"all bases at position\">\n" + 
		"##INFO=<ID=NOVELCOV,Number=.,Type=String,Description=\"bases at position from reads with novel starts\">\n" + 
		STANDARD_FINAL_HEADER_LINE;
	}
	public static final String getReducedHeaderForQSig(final String patientId, final String inputType,  final String library,
			final String bamName, final String snpFile) {
		
		return "##fileformat=VCFv4.0\n" +
		"##patient_id=" + patientId + "\n" +
		(inputType != null ? ("##input_type=" + inputType  + "\n") : "") + 
		"##library=" + library  + "\n" +
		"##bam=" + bamName + "\n" +
		"##snp_file=" + snpFile + "\n" + 
		"##filter_q_score=10\n" + 
		"##filter_match_qual=10\n" + 
		"##genome=GRCh37_ICGC_standard_v2.fa\n" + 
		"##FILTER=<ID=LowQual,Description=\"REQUIRED: QUAL < 50.0\">\n" + 
		"##INFO=<ID=FULLCOV,Number=.,Type=String,Description=\"all bases at position\">\n" + 
		"##INFO=<ID=NOVELCOV,Number=.,Type=String,Description=\"bases at position from reads with novel starts\">\n" + 
		STANDARD_FINAL_HEADER_LINE;
	}
	public static final String getReducedHeaderForQSig(final String patientId,  final String library,
			final String bamName, final String snpFile) {
		return getReducedHeaderForQSig(patientId, library, (String) null, bamName, snpFile);
	}
	
	public static final String getBasicHeaderForQSig(final String bamName, final String snpFile, String ... bamHeaderInfo) {
		
		String patient = null;
		String library = null;
		if (null != bamHeaderInfo && bamHeaderInfo.length > 0) {
			patient = bamHeaderInfo[0];
			library = bamHeaderInfo.length > 1 ? bamHeaderInfo[1] : null; 
		}
		
		return "##fileformat=VCFv4.0\n" +
		(patient != null ? 	("##patient_id=" + patient + "\n")  : "") +
		(library != null ? 	("##library=" + library  + "\n")  : "") +
		"##bam=" + bamName + "\n" +
		"##snp_file=" + snpFile + "\n" + 
		"##filter_q_score=10\n" + 
		"##filter_match_qual=10\n" + 
		"##FILTER=<ID=LowQual,Description=\"REQUIRED: QUAL < 50.0\">\n" + 
		"##INFO=<ID=FULLCOV,Number=.,Type=String,Description=\"all bases at position\">\n" + 
		"##INFO=<ID=NOVELCOV,Number=.,Type=String,Description=\"bases at position from reads with novel starts\">\n" + 
		STANDARD_FINAL_HEADER_LINE;
	}
	
	public static final String getHeaderForQSigIlluminaFile(final String patientId,  final String sample,
			final String inputType, final String illuminaFileName, final String snpFile) {
		
		return "##fileformat=VCFv4.0\n" +
		"##patient_id=" + patientId + "\n" + 
		"##input_type=" + inputType  + "\n" +
		"##sample=" + sample  + "\n" +
		"##bam=" + illuminaFileName + "\n" +
		"##snp_file=" + snpFile + "\n" + 
		"##filter_q_score=10\n" + 
		"##filter_match_qual=10\n" + 
		"##genome=GRCh37_ICGC_standard_v2.fa\n" + 
		"##FILTER=<ID=LowQual,Description=\"REQUIRED: QUAL < 50.0\">\n" + 
		"##INFO=<ID=FULLCOV,Number=.,Type=String,Description=\"all bases at position\">\n" + 
		"##INFO=<ID=NOVELCOV,Number=.,Type=String,Description=\"bases at position from reads with novel starts\">\n" + 
		STANDARD_FINAL_HEADER_LINE;
	}
	
	public static final String getHeaderForQSnp(final String patientId,  final String normalSampleId, final String tumourSampleId, final String source) {
		
		return "##fileformat=VCFv4.0\n" +
		"##fileDate=" + df.format(Calendar.getInstance().getTime()) + "\n" + 
		"##source=" + source + "\n" + 
		"##patient_id=" + patientId + "\n" + 
//		"##input_type=" + inputType  + "\n" +
		"##normalSample=" + normalSampleId  + "\n" +
		"##tumourSample=" + tumourSampleId  + "\n" +
//		"##library=" + library  + "\n" +
//		"##slide=" + slide + "\n" +
//		"##barcode=" + barcode + "\n" + 
//		"##physical_division=" + physicalDivision + "\n" + 
//		"##pileupFile=" + pileupFile + "\n" +
//		"##bam=" + bamName + "\n" +
//		"##snp_file=" + snpFile + "\n" +
		
		// INFO field options
		"##INFO=<ID=" + INFO_MUTANT_READS + ",Number=1,Type=Integer,Description=\"Number of mutant/variant reads\">\n" +
		"##INFO=<ID=" + INFO_NOVEL_STARTS + ",Number=1,Type=Integer,Description=\"Number of novel starts not considering read pair\">\n" +
		"##INFO=<ID=" + INFO_FLANKING_SEQUENCE + ",Number=1,Type=String,Description=\"Reference bases either side of mutation\">\n" +
//		"##INFO=<ID=" + INFO_MUTATION + ",Number=1,Type=String,Description=\"mutation/variant\">\n" +
		
		
		// FILTER field options
		"##FILTER=<ID=" + FILTER_COVERAGE_NORMAL_12 + ",Description=\"Less than 12 reads coverage in normal\">\n" + 
		"##FILTER=<ID=" + FILTER_COVERAGE_NORMAL_8 + ",Description=\"Less than 8 reads coverage in normal\">\n" + 
		"##FILTER=<ID=" + FILTER_COVERAGE_TUMOUR + ",Description=\"Less than 8 reads coverage in tumour\">\n" + 
		"##FILTER=<ID=" + FILTER_SAME_ALLELE_NORMAL + ",Description=\"Less than 3 reads of same allele in normal\">\n" + 
		"##FILTER=<ID=" + FILTER_SAME_ALLELE_TUMOUR + ",Description=\"Less than 3 reads of same allele in tumour\">\n" + 
		"##FILTER=<ID=" + FILTER_MUTATION_IN_NORMAL + ",Description=\"Mutation also found in pileup of normal\">\n" + 
		"##FILTER=<ID=" + FILTER_MUTATION_IN_UNFILTERED_NORMAL + ",Description=\"Mutation also found in pileup of (unfiltered) normal\">\n" + 
		"##FILTER=<ID=" + FILTER_GERMLINE + ",Description=\"Mutation is a germline variant in another patient\">\n" + 
		"##FILTER=<ID=" + FILTER_NOVEL_STARTS + ",Description=\"Less than 4 novel starts not considering read pair\">\n" + 
		"##FILTER=<ID=" + FILTER_MUTANT_READS + ",Description=\"Less than 5 mutant reads\">\n" +
		"##FILTER=<ID=" + FILTER_MUTATION_EQUALS_REF + ",Description=\"Mutation equals reference\">\n" +
		
		// FORMAT field options
		"##FORMAT=<ID=" + FORMAT_GENOTYPE + ",Number=1,Type=String,Description=\"Genotype: 0/0 homozygous reference; 0/1 heterozygous for alternate allele; 1/1 homozygous for alternate allele\">\n" + 
		"##FORMAT=<ID=" + FORMAT_GENOTYPE_DETAILS + ",Number=1,Type=String,Description=\"Genotype details: specific alleles (A,G,T or C)\">\n" + 
		"##FORMAT=<ID=" + FORMAT_ALLELE_COUNT + ",Number=1,Type=String,Description=\"Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]\">\n" + 
		
		"#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tNormal\tTumour\n";
	}
	
	public static final String getHeaderForQCoverage(final String bamFileName, final String gffFile) {
		return "##fileformat=VCFv4.0\n" +
		"##bam_file=" + bamFileName + "\n" +
		"##gff_file=" + gffFile + "\n" + 
		"##FILTER=<ID=LowQual,Description=\"REQUIRED: QUAL < 50.0\">\n" + 
		"##INFO=<ID=B,Number=.,Type=String,Description=\"Bait name\">\n" + 
		"##INFO=<ID=BE,Number=.,Type=String,Description=\"Bait end position\">\n" + 
		"##INFO=<ID=ZC,Number=.,Type=String,Description=\"bases with Zero Coverage\">\n" + 
		"##INFO=<ID=NZC,Number=.,Type=String,Description=\"bases with Non Zero Coverage\">\n" + 
		"##INFO=<ID=TOT,Number=.,Type=String,Description=\"Total number of sequenced bases\">\n" +
		STANDARD_FINAL_HEADER_LINE;
	}
	
	public static final String getHeaderForCommonSnps(final String searchString, final String searchDirectory, String[] additionalSearchStrings, Map<File, Integer> mapOfFilesAndIds) {
		StringBuilder filesMapSB = new StringBuilder();
		if (null != mapOfFilesAndIds && mapOfFilesAndIds.size() > 0) {
			
			List<File> files = new ArrayList<File>(mapOfFilesAndIds.keySet());
			Collections.sort(files);
			
			for (File f : files) {
				filesMapSB .append("##INFO=<ID=" + mapOfFilesAndIds.get(f) + ",Number=0,Type=Flag,Description=\"" + f.getAbsolutePath() + "\">\n");
			}
		}
		
		return "##fileformat=VCFv4.0\n" +
		"##search_string=" + searchString + "\n" +
		"##search_directory=" + searchDirectory + "\n" + 
		"##additional_search_directory=" + Arrays.deepToString(additionalSearchStrings) + "\n" + 
		filesMapSB.toString() + 
		STANDARD_FINAL_HEADER_LINE;
	}
	
	public static String getPileupElementAsString(List<PileupElement> pileups, boolean novelStart) {
		
		int a = 0, c = 0, g = 0, t = 0, n = 0;
		if (null != pileups) {
			for (PileupElement pe : pileups) {
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
		int firstIndex = 4;	// string should always start with 0/0
		int secondIndex = genotype.indexOf(":", firstIndex);
		String adNumbers = genotype.substring(firstIndex, secondIndex);
		
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
		int firstIndex = 4;	// string should always start with 0/0
		int secondIndex = format.indexOf(":", firstIndex) +1;
		if (secondIndex == -1) { 
//			System.err.println("incorrent index for format field: " + format);
			return -1;
		}
		
		int thirdIndex = format.indexOf(":", secondIndex);
		
		if (thirdIndex == -1) {
//			System.err.println("incorrent index for format field: " + format);
			return -1;
		}
		
		String dpString = format.substring(secondIndex, thirdIndex);
		return Integer.parseInt(dpString);
	}
	
	public static String getGenotypeFromGATKVCFRecord(VCFRecord rec) {
		if (null == rec || rec.getExtraFields().size() < 2)
			throw new IllegalArgumentException("VCFRecord null, or does not contain the appropriate fields");
		
		String extraField = rec.getExtraFields().get(1);	// second item in list should have pertinent info
		if (StringUtils.isNullOrEmpty(extraField)) return null;
		return extraField.substring(0,3);
	}
	
	public static GenotypeEnum getGEFromGATKVCFRec(VCFRecord rec) {
		String genotypeString = getGenotypeFromGATKVCFRecord(rec);
		return calculateGenotypeEnum(genotypeString, rec.getRef(), rec.getAlt().charAt(0));
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
	
	public static String[] getMutationAndGTs(char ref, GenotypeEnum control, GenotypeEnum test) {
		String NO_DATA = "" + VCFRecord.MISSING_DATA;
		String [] results = new String[3];
		Set<Character> alts= new TreeSet<Character>();
		
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
		
		int size = alts.size();
		
		String altsString = getStringFromCharSet(alts);
		if (size == 0) {
//			assert false : "empty list of alts from control and test: " + control + ", " + test;
			Arrays.fill(results, NO_DATA);
		
		} else if (size == 1) {
			results[0] = alts.iterator().next().toString();
			results[1] = null != control ? control.getGTString(ref) : NO_DATA;
			results[2] = null != test ? test.getGTString(ref) : NO_DATA;
		} else {
			String alt = "";
			for (char c : alts) {
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
		String result = "" + VCFRecord.MISSING_DATA;
		if (ge != null && ! StringUtils.isNullOrEmpty(altsString)) {
			if (ge.containsAllele(ref)) {
				if (ge.isHeterozygous()) {
					result = "0/" + (altsString.indexOf(ref == ge.getFirstAllele() ? ge.getSecondAllele() : ge.getFirstAllele()) + 1);
				} else {
					result = "0/0";
				}
			} else {
//				if (ge.isHeterozygous()) {
					result = (altsString.indexOf(ge.getFirstAllele()) + 1) + "/" + (altsString.indexOf(ge.getSecondAllele()) + 1);
//				} else {
//					result = (altsString.indexOf(ge.getFirstAllele()) + 1) + "/" + (altsString.indexOf(ge.getSecondAllele()) + 1);
//				}
			}
		}
		
		return result;
	}
	 
	public static String getStringFromCharSet(Set<Character> set) {
		StringBuilder sb = new StringBuilder();
		if (null != set) {
			for (Character c : set) sb.append(c);
		}
		return sb.toString();
	}
	
//	public static String getAltFromGenotypeEnum(char ref, GenotypeEnum ge) {
//		String result = null;
//		
//		if (null != ge) {
//			
//			if (ge.containsAllele(ref)) {
//				if (ge.isHeterozygous()) {
//					result = (ref == ge.getFirstAllele()) ? "" + ge.getSecondAllele() : "" + ge.getFirstAllele();
//				}
//			} else {
//				if (ge.isHeterozygous()) {
//					result = ge.getFirstAllele() + "," + ge.getSecondAllele();
//				} else {
//					result = "" + ge.getFirstAllele(); 
//				}
//			}
//		}
//		
//		return result;
//	}
}
