/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf;

import static org.qcmg.common.util.Constants.COLON;
import static org.qcmg.common.util.Constants.COLON_STRING;
import static org.qcmg.common.util.Constants.MISSING_DATA_STRING;

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

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QBamId;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class CopyOfVcfUtils {
	
	private static final QLogger logger = QLoggerFactory.getLogger(CopyOfVcfUtils.class);
	
	private final static DateFormat df = new SimpleDateFormat("yyyyMMdd");

	public static final String FORMAT_ALLELE_COUNT_COMPOUND_SNP = "ACCS";
	
	// standard final header line
//	public static final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n";

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
		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\n";
 
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
		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE+ "\n";
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
		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE+ "\n";
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
		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE+ "\n";
	}
	
	public static final String getHeaderForQSnp(final String patientId,  final String normalSampleId, final String tumourSampleId, final String source, QBamId[] normalBamIds, QBamId[] tumourBamIds, String uuid, boolean singleSampleMode) {
		
		String controlBamString = "";
		if (null != normalBamIds) {
			for (final QBamId s : normalBamIds) {
				controlBamString += "##controlBam=" + s.getBamName()  + "\n";
				controlBamString += "##controlBamUUID=" + s.getUUID();
			}
		}
		String testBamString = "";
		if (null != tumourBamIds) {
			for (final QBamId s : tumourBamIds) {
				testBamString += "##testBam=" + s.getBamName()  + "\n";
				testBamString += "##testBamUUID=" + s.getUUID()  + "\n";
			}
		}
		
		return "##fileformat=VCFv4.0\n" +
		"##fileDate=" + df.format(Calendar.getInstance().getTime()) + "\n" + 
		"##source=" + source + "\n" + 
		"##patient_id=" + patientId + "\n" + 
//		"##input_type=" + inputType  + "\n" +
		"##controlSample=" + normalSampleId  + "\n" +
		"##testSample=" + tumourSampleId  + "\n" +
		controlBamString +
		testBamString+
		"##analysisId=" + uuid  + "\n" +
		"##\n##\n" +
		
		// INFO field options
		"##INFO=<ID=" + VcfHeaderUtils.FORMAT_MUTANT_READS + ",Number=1,Type=Integer,Description=\"Number of mutant/variant reads\">\n" +
		"##INFO=<ID=" + VcfHeaderUtils.FORMAT_NOVEL_STARTS + ",Number=1,Type=Integer,Description=\"Number of novel starts not considering read pair\">\n" +
		"##INFO=<ID=" + VcfHeaderUtils.INFO_FLANKING_SEQUENCE + ",Number=1,Type=String,Description=\"Reference bases either side of mutation\">\n" +
//		"##INFO=<ID=" + INFO_MUTATION + ",Number=1,Type=String,Description=\"mutation/variant\">\n" +
		
		
		// FILTER field options
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_COVERAGE_NORMAL_12 + ",Description=\"Less than 12 reads coverage in normal\">\n" + 
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_COVERAGE_NORMAL_8 + ",Description=\"Less than 8 reads coverage in normal\">\n" + 
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_COVERAGE_TUMOUR + ",Description=\"Less than 8 reads coverage in tumour\">\n" + 
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_SAME_ALLELE_NORMAL + ",Description=\"Less than 3 reads of same allele in normal\">\n" + 
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_SAME_ALLELE_TUMOUR + ",Description=\"Less than 3 reads of same allele in tumour\">\n" + 
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_MUTATION_IN_NORMAL + ",Description=\"Mutation also found in pileup of normal\">\n" + 
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_MUTATION_IN_UNFILTERED_NORMAL + ",Description=\"Mutation also found in pileup of (unfiltered) normal\">\n" + 
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_GERMLINE + ",Description=\"Mutation is a germline variant in another patient\">\n" + 
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_NOVEL_STARTS + ",Description=\"Less than 4 novel starts not considering read pair\">\n" + 
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_MUTANT_READS + ",Description=\"Less than 5 mutant reads\">\n" +
		"##FILTER=<ID=" + VcfHeaderUtils.FILTER_MUTATION_EQUALS_REF + ",Description=\"Mutation equals reference\">\n" +
		
		// FORMAT field options
		"##FORMAT=<ID=" + VcfHeaderUtils.FORMAT_GENOTYPE + ",Number=1,Type=String,Description=\"Genotype: 0/0 homozygous reference; 0/1 heterozygous for alternate allele; 1/1 homozygous for alternate allele\">\n" + 
		"##FORMAT=<ID=" + VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS + ",Number=1,Type=String,Description=\"Genotype details: specific alleles (A,G,T or C)\">\n" + 
		"##FORMAT=<ID=" + VcfHeaderUtils.FORMAT_ALLELE_COUNT + ",Number=1,Type=String,Description=\"Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]\">\n" + 
		"##FORMAT=<ID=" + FORMAT_ALLELE_COUNT_COMPOUND_SNP + ",Number=1,Type=String,Description=\"Allele Count Compound Snp: lists read sequence and count (forward strand, reverse strand) \">\n" + 
		
		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\t" + ( ! singleSampleMode ? "Control\t" : "" ) + "Test\n";
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
		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE+ "\n";
	}
	
	public static final String getHeaderForCommonSnps(final String searchString, final String searchDirectory, String[] additionalSearchStrings, Map<File, Integer> mapOfFilesAndIds) {
		final StringBuilder filesMapSB = new StringBuilder();
		if (null != mapOfFilesAndIds && mapOfFilesAndIds.size() > 0) {
			
			final List<File> files = new ArrayList<File>(mapOfFilesAndIds.keySet());
			Collections.sort(files);
			
			for (final File f : files) {
				filesMapSB .append("##INFO=<ID=" + mapOfFilesAndIds.get(f) + ",Number=0,Type=Flag,Description=\"" + f.getAbsolutePath() + "\">\n");
			}
		}
		
		return "##fileformat=VCFv4.0\n" +
		"##search_string=" + searchString + "\n" +
		"##search_directory=" + searchDirectory + "\n" + 
		"##additional_search_directory=" + Arrays.deepToString(additionalSearchStrings) + "\n" + 
		filesMapSB.toString() + VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE+ "\n";
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
	
	public static void addFormatFieldsToVcf(VcfRecord vcf, List<String> additionalFormatFields) throws Exception {
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
						//vcf.setFormatField(newFF);
						vcf.setFormatFields(newFF);
					}
					
				}
			}
		}
	}
	/**
	 * Checks to see if the existing annotation is a PASS.
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
		
		if (SnpUtils.PASS.equals(rec.getFilter()) || SnpUtils.PASS.equals(ann)) {
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
