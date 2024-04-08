/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.common.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;

public class SnpUtils {

    //NORMAL
    public static final String LESS_THAN_12_READS_NORMAL = "COVN12";
    public static final String LESS_THAN_8_READS_NORMAL = "COVN8";
    public static final String LESS_THAN_3_READS_NORMAL = "SAN3";

    //TUMOUR
    public static final String LESS_THAN_8_READS_TUMOUR = "COVT";
    public static final String LESS_THAN_3_READS_TUMOUR = "SAT3";
    public static final String NO_CALL_IN_TEST = "NCIT";

    //COMPOUND_SNP
    public static final String COMPOUND_SNP = "COMPOUNDMUTATION";

    public static final String SOMATIC = "SOMATIC";


    //MUTATION
    public static final String MUTATION_IN_UNFILTERED_NORMAL = "MIUN";
    public static final String MUTATION_IN_NORMAL = "MIN";
    public static final String MUTATION_GERMLINE_IN_ANOTHER_PATIENT = "GERM";    //mutation is a germline variant in another patient

    //	public static final String STRAND_BIAS = "SBIAS";	// mutation only found on one strand
    public static final String STRAND_BIAS_ALT = "SBIASALT";    // mutation only found on one strand
    public static final String STRAND_BIAS_COVERAGE = "SBIASCOV";    // mutation only found on 1 strand, and no (or v. little) coverage at all found on other strand
    public static final String ALLELIC_FRACTION = "AF";    // allelic fraction of mutation is less than appended number eg. AF20
    public static final String END_OF_READ = "5BP";    // number of reads carrying the mutation that are within 5 base pairs of the end of the read

    // a couple of extra class B's
    public static final String LESS_THAN_12_READS_NORMAL_AND_UNFILTERED = LESS_THAN_12_READS_NORMAL + "; " + MUTATION_IN_UNFILTERED_NORMAL;
    public static final String LESS_THAN_3_READS_NORMAL_AND_UNFILTERED = LESS_THAN_3_READS_NORMAL + "; " + MUTATION_IN_UNFILTERED_NORMAL;

    //EXTRAs
    public static final String NOVEL_STARTS = "NNS";
    public static final String MUTANT_READS = "MR";
    public static final String PASS = "PASS";
    public static final String MUTATION_EQUALS_REF = "MER";    // mutation same as reference


    public static final String INDEL_SATELLITE = "Satellite";
    public static final String INDEL_SIMPLE_REPEAT = "Simple_repeat::";
    public static final String INDEL_LOW_COMPLEXITY = "Low_complexity::";
    public static final String INDEL_HOM_ADJ = "HOMADJ_";
    public static final String INDEL_HOM_CON = "HOMCON_";
    public static final String INDEL_HOM_EMB = "HOMEMB_";
    public static final String INDEL_HCOVT = "HCOVT";
    public static final String INDEL_HCOVN = "HCOVN";
    public static final String INDEL_STRAND_BIAS = "TBIAS";
    public static final String INDEL_NPART = "NPART";

    public static final int MUT_DELIM_LENGTH = Constants.MUT_DELIM.length();

    private static final QLogger logger = QLoggerFactory.getLogger(SnpUtils.class);

    /**
     * Utility method to determine if an annotation contains text that classifies a snp as class A or B
     * Class B is currently defined as one of the following:
     *
     * <code>LESS_THAN_12_READS_NORMAL, LESS_THAN_3_READS_NORMAL, MUTATION_IN_UNFILTERED_NORMAL</code>
     *
     * @param annotation
     * @return
     * @see #isClassA(String)
     * @see #LESS_THAN_12_READS_NORMAL
     * @see #LESS_THAN_3_READS_NORMAL
     * @see #MUTATION_IN_UNFILTERED_NORMAL
     */
    public static boolean isClassAorB(String annotation) {
        annotation = removeCompoundSnpAnnotationFromString(annotation);
        return isClassA(annotation)
                || LESS_THAN_12_READS_NORMAL.equals(annotation)
                || LESS_THAN_3_READS_NORMAL.equals(annotation)
                || MUTATION_IN_UNFILTERED_NORMAL.equals(annotation)
                || LESS_THAN_12_READS_NORMAL_AND_UNFILTERED.equals(annotation)
                || LESS_THAN_3_READS_NORMAL_AND_UNFILTERED.equals(annotation);
    }

    public static boolean isClassAorBIndel(String annotation) throws IllegalArgumentException {
        if (StringUtils.isNullOrEmpty(annotation))
            throw new IllegalArgumentException("null or empty annotation passed to SnpUtils.isClassAorBIndel");

        return
                isAnnotationAlone(annotation, INDEL_SATELLITE)
                        || isAnnotationAlone(annotation, INDEL_SIMPLE_REPEAT)
                        || isAnnotationAlone(annotation, INDEL_LOW_COMPLEXITY)
                        || isAnnotationAlone(annotation, MUTATION_IN_NORMAL)
                        || isAnnotationAlone(annotation, INDEL_HCOVN)
                        || isAnnotationAlone(annotation, INDEL_HCOVT)
                        || isAnnotationAlone(annotation, LESS_THAN_12_READS_NORMAL)
                        || isAnnotationAlone(annotation, LESS_THAN_8_READS_NORMAL)
                        || isAnnotationAlone(annotation, INDEL_STRAND_BIAS)
                        || isAnnotationAlone(annotation, INDEL_NPART);
    }

    public static String removeCompoundSnpAnnotationFromString(String annotation) {
        if (!StringUtils.isNullOrEmpty(annotation) && annotation.contains(COMPOUND_SNP)) {
            return annotation.replace(";" + COMPOUND_SNP, "");
        }
        return annotation;
    }

    /**
     * Utility method to determine if an annotation contains text that classifies a snp as class A
     * Class A is currently defined as "PASS"
     *
     * @param annotation String
     * @return true if string matches "--", false otherwise
     */
    public static boolean isClassA(String annotation) {
        return PASS.equals(removeCompoundSnpAnnotationFromString(annotation));
    }

    /**
     * Determines if a given annotation classifies a snp as class A or B in the isClassAIndel method.
     *
     * @param annotation        the annotation to check
     * @param homopolymerCutoff the cutoff value for homopolymer
     * @return true if the annotation classifies the snp as class A or B, false otherwise
     * @throws IllegalArgumentException if the annotation is null or empty
     */
    public static boolean isClassAIndel(String annotation, int homopolymerCutoff) throws IllegalArgumentException {
        if (StringUtils.isNullOrEmpty(annotation))
            throw new IllegalArgumentException("null or empty annotation passed to SnpUtils.isClassAIndel");

        return
                isClassA(annotation) || (!containsIndelClassBAnnotation(annotation)
                        && ((annotation.contains(INDEL_HOM_ADJ)
                        && getNNumberFromAnnotation(annotation, INDEL_HOM_ADJ) <= homopolymerCutoff)
                        || (annotation.contains(INDEL_HOM_CON)
                        && getNNumberFromAnnotation(annotation, INDEL_HOM_CON) <= homopolymerCutoff)
                        || (annotation.contains(INDEL_HOM_EMB)
                        && getNNumberFromAnnotation(annotation, INDEL_HOM_EMB) <= homopolymerCutoff)));
    }

    public static boolean containsIndelClassBAnnotation(String annotation) {
        return
                annotation.contains(INDEL_SATELLITE)
                        || annotation.contains(INDEL_SIMPLE_REPEAT)
                        || annotation.contains(INDEL_LOW_COMPLEXITY)
                        || annotation.contains(MUTATION_IN_NORMAL)
                        || annotation.contains(INDEL_HCOVN)
                        || annotation.contains(INDEL_HCOVT)
                        || annotation.contains(LESS_THAN_12_READS_NORMAL)
                        || annotation.contains(MUTATION_GERMLINE_IN_ANOTHER_PATIENT)
                        || annotation.contains(LESS_THAN_8_READS_NORMAL)
                        || annotation.contains(INDEL_STRAND_BIAS)
                        || annotation.contains(INDEL_NPART);

    }

    /**
     * Checks to see if the supplied subAnnotation is the only annotation in the supplied string
     * With the exclusion of certain allowed annotations. eg. PASS
     *
     * @param annotation
     * @param subAnnotation
     * @return
     */
    public static boolean isAnnotationAlone(String annotation, String subAnnotation) {
        if (StringUtils.isNullOrEmpty(annotation))
            throw new IllegalArgumentException("null or empty annotation passed to isAnnotationAlone");

        if (annotation.equals(subAnnotation)) return true;

        // check to see if there are more than 1 annotation - they are delimited by ';'
        int colonIndex = annotation.indexOf(';');
        if (colonIndex > -1) {

            String[] params = TabTokenizer.tokenize(annotation, ';');
            if (params.length > 2) return false;

            for (String s : params) {
                if (!SnpUtils.PASS.equals(s) && !s.contains(subAnnotation)) {
                    return false;
                }
            }
            return true;
        }

        // only 1 annotation and it doesn't match the subAnnotation
        return false;

    }

    public static int getNNumberFromAnnotation(String annotation, String prefix) {

        int startIndex = annotation.indexOf(prefix) + prefix.length();
        //String numberString = "";
        StringBuilder numberString = new StringBuilder();
        for (int i = startIndex, len = annotation.length(); i < len; i++) {
            char c = annotation.charAt(i);
            if (Character.isDigit(c)) {
                numberString.append(c);
            } else {
                break;
            }
        }

        // deal with empty string case
        if (numberString.isEmpty())
            throw new IllegalArgumentException("Invalid annotation passed to getNNumberFromAnnotation");

        return Integer.parseInt(numberString.toString());
    }

    /**
     * returns the alt allele from the mutation string
     * assuming that the following format is adhered to:
     * A/C
     * will return C in this instance
     *
     * @param mutation
     * @return char representing the alt allele
     * @TODO must update to deal with X>X/Y and X/Y>X/Y
     */
    public static String getAltFromMutationString(String mutation) {
        if (StringUtils.isNullOrEmpty(mutation)) {
            throw new IllegalArgumentException("invalid mutation string supplied to getAltFromMutationString (null or empty)");
        }
        int index = mutation.indexOf(Constants.MUT_DELIM);
        return mutation.substring(index + MUT_DELIM_LENGTH);
    }


    /**
     * Get the count of the supplied base from the supplied Nucleotide string.
     * <p>
     * eg. for the Nucleotide string A1[41],3[19.33],G8[39.12],29[26.48]
     * this method will return 4 if 'A' is the supplied base, 37 if 'G' is the supplied base, and 0 for all other supplied bases
     * <p>
     * If the bases have come from a merged vcf record, then the first occurrence of the desired base will be returned.
     *
     */
    public static int getCountFromNucleotideString(final String bases, final String base) {
        return getCountFromNucleotideString(bases, base, false);
    }

    public static Map<String, Integer> getCompoundSnpDistribution(String dist, int minimumCoverage) {
        if (!StringUtils.isNullOrEmpty(dist) && dist.contains(Constants.COMMA_STRING)) {

            String[] ar = TabTokenizer.tokenize(dist, Constants.COMMA);

            int len = ar.length;

            if (len > 0) {
                Map<String, Integer> map = new HashMap<>(8);
                String bases = null;
                int tally = 0;

                for (String s : ar) {

                    if (Character.isDigit(s.charAt(0))) {
                        tally += Integer.parseInt(s);
                    } else {
                        /*
                         * Populate previous bases if not null and minimum coverage has been met
                         */
                        if (null != bases && tally >= minimumCoverage) {
                            map.put(bases, tally);
                        }
                        bases = s;
                        tally = 0;
                    }
                }
                /*
                 * populate last entry if we have coverage
                 */
                if (tally >= minimumCoverage) {
                    map.put(bases, tally);
                }

                return map;
            }
        } else {
            logger.warn("dist: " + dist);
        }
        return Collections.emptyMap();
    }



    /**
     * This method returns the count of the supplied base from the supplied Nucleotide string.
     * If the bases have come from a merged vcf record, then the first occurrence of the desired base will be returned.
     *
     * @param bases The Nucleotide string.
     * @param base The base to count.
     * @param compoundSnp Flag to indicate if the Nucleotide string is for a compound SNP.
     * @return The count of the supplied base. Returns 0 if the Nucleotide string or the base is null or empty.
     * @see SnpUtils#getTotalCountFromNucleotideString(String, boolean)
     */
    public static int getCountFromNucleotideString(final String bases, final String base, boolean compoundSnp) {
        if (StringUtils.isNullOrEmpty(bases) || StringUtils.isNullOrEmpty(base)) {
            return 0;
        }

        final int basePosition = bases.indexOf(base);
        if (basePosition == -1) return 0;

        if (compoundSnp) {
            //AC,2,2,AT,2,4,A_,3,3,CA,2,1,CC,16,17,CG,2,1,CT,1,2,C_,16,16,GC,1,0,_C,0,1,G_,0,1
            // need next 2 commas locations
            int comma1 = bases.indexOf(Constants.COMMA, basePosition);
            int comma2 = bases.indexOf(Constants.COMMA, comma1 + 1);
            int comma3 = bases.indexOf(Constants.COMMA, comma2 + 1);
            if (comma1 == -1 || comma2 == -1) {
                return 0;
            }
            if (comma3 == -1) {
                // end of string
                comma3 = bases.length();
            }
            final int forwardCount = Integer.parseInt(bases, comma1 + 1, comma2, 10);
            final int reverseCount = Integer.parseInt(bases, comma2 + 1, comma3, 10);

            return forwardCount + reverseCount;
        } else {

            final int bracketPosition = bases.indexOf('[', basePosition);

            final int forwardCount = Integer.parseInt(bases, basePosition + base.length(), bracketPosition, 10);

            final int commaPosition = bases.indexOf(',', bracketPosition);
            final int reverseCount = Integer.parseInt(bases, commaPosition + 1, bases.indexOf('[', commaPosition), 10);

            return forwardCount + reverseCount;
        }
    }

    public static int getTotalCountFromNucleotideString(final String bases, boolean cs) {
        if (StringUtils.isNullOrEmpty(bases)) {
            return 0;
        }
        int tally = 0;
        String[] params = bases.split(Constants.COMMA_STRING);
        if (cs) {
            for (String s : params) {
                if (!StringUtils.isNullOrEmpty(s) && Character.isDigit(s.charAt(0))) {
                    tally += Integer.parseInt(s);
                }
            }
        } else {
            for (String s : params) {
                if (!StringUtils.isNullOrEmpty(s)) {
                    int bracketPosition = s.indexOf('[');
                    if (bracketPosition > -1) {
                        tally += Integer.parseInt(s, (Character.isDigit(s.charAt(0)) ? 0 : 1), bracketPosition, 10);
                    }
                }
            }
        }

        return tally;
    }

    /**
     * Get the count of the supplied base from the supplied indel Nucleotide string.
     * <p>
     * eg. for the Nucleotide string 4;39;24;4;4;0;5
     * this method will return 4 if 0 is the supplied position , 39 if 1 is the supplied position, etc.
     *
     * @param countsString
     * @param position     0 based position of count of interest in string
     * @return
     */
    public static int getCountFromIndelNucleotideString(final String countsString, int position) {
        if (StringUtils.isNullOrEmpty(countsString)) return 0;

        String[] counts = TabTokenizer.tokenize(countsString, ';');
        if (position >= counts.length) {
            // hmmmm
            return 0;
        }
        String count = counts[position];
        // check that count is a number
        if (StringUtils.isNumeric(count)) {
            return Integer.parseInt(count);
        } else {
            throw new IllegalArgumentException
                    ("invalid parameters supplied to getCountFromIndelNucleotideString: " + countsString + ", and position: " + position);
        }
    }

    public static boolean doesNucleotideStringContainReadsOnBothStrands(String bases, int sBiasCovPercentage) {

        if (StringUtils.isNullOrEmpty(bases)) return false;


//		A0[0],848[37.74],C0[0],3[23],G0[0],393[38.01],T0[0],4[21.5]

        String[] basesArray = TabTokenizer.tokenize(bases, Constants.COMMA);
        // should always have an even number of bases (1 for each strand)
        if (basesArray.length % 2 != 0) {
            logger.warn("Incorrect number of basesArray elements: " + basesArray.length);
        }

        int fsCount = 0, rsCount = 0;
        for (String arrayValue : basesArray) {
            // strip leading character if it is a letter
            if (Character.isAlphabetic(arrayValue.charAt(0))) {
                // forward strand
                // strip colon if it exists.
                int startPos = 1;
                if (arrayValue.charAt(1) == Constants.COLON) {
                    startPos++;
                }
                int endPos = arrayValue.indexOf('[');
                fsCount += Integer.parseInt(arrayValue, startPos, endPos, 10);

            } else {
                // reverse strand
                int startPos = 0;
                if (arrayValue.charAt(1) == Constants.COLON) {
                    startPos++;
                }
                int endPos = arrayValue.indexOf('[');
                rsCount += Integer.parseInt(arrayValue, startPos, endPos, 10);
            }
        }

        int total = fsCount + rsCount;
        int min = Math.min(rsCount, fsCount);

        return ((double) min / total) * 100 > sBiasCovPercentage;
    }

}
