/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import gnu.trove.list.TShortList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.AccumulatorUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfFileMeta;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.Options;

/**
 * @author christix
 *
 */

public class ConfidenceMode extends AbstractMode{
	private final QLogger logger = QLoggerFactory.getLogger(ConfidenceMode.class);
	
	
//	public static final String[] CLASS_B_FILTERS= new String[] {PASS, MUTATION_IN_UNFILTERED_NORMAL, LESS_THAN_12_READS_NORMAL, LESS_THAN_3_READS_NORMAL};
//	public static final String[] CONTROL_FILTERS= new String[] {MUTATION_IN_UNFILTERED_NORMAL, MUTATION_IN_NORMAL, LESS_THAN_12_READS_NORMAL, LESS_THAN_8_READS_NORMAL, LESS_THAN_3_READS_NORMAL};
	
	public static final int HIGH_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int LOW_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	
	public static final int HIGH_CONF_ALT_FREQ_PASSING_SCORE = 5;	
	public static final int LOW_CONF_ALT_FREQ_PASSING_SCORE = 4;	
	
	public static final int CONTROL_COVERAGE_MIN_VALUE = 8;
	public static final int TEST_COVERAGE_MIN_VALUE = 8;
	
	public static final int MUTATION_IN_NORMAL_MIN_PERCENTAGE = 5;
	public static final int MUTATION_IN_NORMAL_MIN_COVERAGE = 3;
	
	public static final int sBiasAltPercentage = 5;
	public static final int sBiasCovPercentage = 5;
	
	//filters 
	
	public static final String DESCRIPTION_INFO_CONFIDENCE = String.format( "set to HIGH if the variants passed all filter, "
			+ "appeared on more than %d novel stars reads and more than %d reads contains variants, is adjacent to reference sequence with less than %d homopolymer base; "
			+ "Or set to LOW if the variants passed MIUN/MIN/GERM filter, appeared on more than %d novel stars reads and more than %d reads contains variants;"
			+ "Otherwise set to Zero if the variants didn't matched one of above conditions.",
			HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, HIGH_CONF_ALT_FREQ_PASSING_SCORE,IndelConfidenceMode.DEFAULT_HOMN,  LOW_CONF_NOVEL_STARTS_PASSING_SCORE, LOW_CONF_ALT_FREQ_PASSING_SCORE);


	private TShortList testCols;
	private TShortList controlCols;
	private VcfFileMeta meta;
	
	private int nnsCount = HIGH_CONF_NOVEL_STARTS_PASSING_SCORE;
	private int mrCount = HIGH_CONF_ALT_FREQ_PASSING_SCORE;
	
	private int controlCovCutoff = CONTROL_COVERAGE_MIN_VALUE;
	private int testCovCutoff = TEST_COVERAGE_MIN_VALUE;
	
	private int minCov = 0;
	
	private List<String> filtersToIgnore = new ArrayList<>();
	private double mrPercentage = 0.0f;
	
	//for unit testing
	ConfidenceMode(){
//		this.testCols = new TShortArrayList(testCol);
//		this.controlCols = new TShortArrayList(controlCol);
	}

	
	public ConfidenceMode( Options options) throws Exception{				 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
 		
		loadVcfRecordsFromFile(new File( options.getInputFileName())   );	
		
		options.getNNSCount().ifPresent(i -> nnsCount = i.intValue());
		options.getMRCount().ifPresent(i -> mrCount = i.intValue());
		options.getControlCutoff().ifPresent(i -> controlCovCutoff = i.intValue());
		options.getTestCutoff().ifPresent(i -> testCovCutoff = i.intValue());
		options.getMRPercentage().ifPresent(i -> mrPercentage = i.floatValue());
		filtersToIgnore = options.getFiltersToIgnore();
		logger.tool("Number of Novel Starts filter value: " + nnsCount);
		logger.tool("Number of Mutant Reads filter value: " + mrCount);
		logger.tool("Percentage of Mutant Reads filter value: " + mrPercentage);
		logger.tool("Control coverage minimum value: " + controlCovCutoff);
		logger.tool("Test coverage minimum value: " + testCovCutoff);
		logger.tool("Filters to ignore: " + filtersToIgnore.stream().collect(Collectors.joining(", ")));
		
		minCov = Math.min(controlCovCutoff, testCovCutoff);

		//get control and test sample column; here use the header from inputRecord(...)
		meta = new VcfFileMeta(header);
		testCols = meta.getAllTestPositions();
		controlCols = meta.getAllControlPositions();

		addAnnotation();
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()) );	
	}

	public void setMRPercentage(double perc) {
		mrPercentage = perc;
	}
	public void setNNSCount(int i) {
		nnsCount = i;
	}
	public void setFiltersToIgnore(List<String> l) {
		filtersToIgnore = l;
	}

	void addAnnotation() {
		
		int pass = 0;
		int fail = 0;
		
		final boolean percentageMode = mrPercentage > 0.0f;
		
		//check high, low nns...
		for (List<VcfRecord> vcfs : positionRecordMap.values()) {
			for(VcfRecord vcf : vcfs){
				
				
				boolean isSomatic = VcfUtils.isRecordSomatic(vcf);
				String [] alts = vcf.getAlt().split(Constants.COMMA_STRING);
				
				/*
				 * We will look at each sample in isolation
				 * if no genotype is present, skip the sample
				 * If we have a 0/0 genotype, and the sample FT is ., and the coverage is adequate, set it to pass
				 * if we have any other genotype (ie, the sample is showing a mutation), check coverage, MR and NNS and existing FT fields. If within our acceptable limits, set FT to PASS
				 */
				
				Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(vcf.getFormatFields());
				VcfInfoFieldRecord info = vcf.getInfoRecord();
				int lhomo = (info.getField(VcfHeaderUtils.INFO_HOM) == null)? 1 :
					StringUtils.string2Number(info.getField(VcfHeaderUtils.INFO_HOM).split(Constants.COMMA_STRING)[0], Integer.class);
				
				
				String [] gtArray = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
				String [] nnsArr = ffMap.get(VcfHeaderUtils.FILTER_NOVEL_STARTS);
//				String [] mrArr = ffMap.get(VcfHeaderUtils.FORMAT_MUTANT_READS);
				String [] filterArr = ffMap.get(VcfHeaderUtils.FORMAT_FILTER);
				String [] covArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
				String [] ccmArr = ffMap.get(VcfHeaderUtils.FORMAT_CCM);
				String [] oabsArr = ffMap.get(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND);
				String [] gqArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY);
				String [] adArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
				String [] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
				String [] eorArr = ffMap.get(VcfHeaderUtils.FORMAT_END_OF_READ);
				if (covArr == null || covArr.length == 0) {
					logger.warn("no coverage values for vcf record!!!: " + vcf);
					continue;
				}
				
				for (int i = 0 ; i < gtArray.length ; i++) {
					
					
					String inf = null != infArr && infArr.length > i ? infArr[i] : null;
					/*
					 * for No Call in GATK, set filter to PASS
					 */
					if (VcfHeaderUtils.FORMAT_NCIG.equals(inf)) {
						filterArr[i] = VcfHeaderUtils.FILTER_PASS;
						continue;
					}
					
					
					/*
					 * check filter field - only proceed if its null, empty or '.'
					 */
					if (StringUtils.isNullOrEmptyOrMissingData(filterArr[i])) {
						
						boolean isControl =  controlCols != null && controlCols.contains((short) (i+1));
						/*
						 * add all failed filters to FT field
						 */
						StringBuilder fSb = new StringBuilder();
						
						
						/*
						 * coverage next - needs to be >= the min coverage value
						 */
						String covS = covArr[i];
						boolean isGATKCall = null != gqArr && ! StringUtils.isNullOrEmptyOrMissingData(gqArr[i]);
						int cov = StringUtils.isNullOrEmptyOrMissingData(covS) ? 0 :Integer.parseInt(covS);
						
						if (cov < minCov || cov < (isControl ? controlCovCutoff : testCovCutoff)) {
							StringUtils.updateStringBuilder(fSb, "COV", Constants.SEMI_COLON);
						}
						
						Map<String, int[]> alleleDist = (null != oabsArr && oabsArr.length >= i) ? VcfUtils.getAllelicCoverageWithStrand(oabsArr[i]) : Collections.emptyMap();
						/*
						 * MIN next - only do this for control when we have a somatic call
						 * need OABS field to be able to do this.
						 */
						if (isControl && isSomatic && ! isGATKCall) {
							
							for (String alt : alts) {
								int altCov = Arrays.stream(alleleDist.getOrDefault(alt, new int[]{0,0})).sum();
								boolean min = VcfUtils.mutationInNorma(altCov, cov, MUTATION_IN_NORMAL_MIN_PERCENTAGE, MUTATION_IN_NORMAL_MIN_COVERAGE);
								if (min) {
									StringUtils.updateStringBuilder(fSb, "MIN", Constants.SEMI_COLON);
									break;
								}
							}
						}
						
						/*
						 * SBIASALT and SBIASCOV - do for all samples
						 */
						String gt = gtArray[i];
						if ( ! StringUtils.isNullOrEmptyOrMissingData(gt)) {
							if ( ! "0/0".equals(gt)) {
								
								
								if ( ! alleleDist.isEmpty() && ! isGATKCall) {
									int index = gt.indexOf(Constants.SLASH);
									int []  gts = new int[] {Integer.parseInt(gt.substring(0,index)), Integer.parseInt(gt.substring(index + 1))};
								
									AtomicInteger fsCount = new AtomicInteger();
									AtomicInteger rsCount = new AtomicInteger();
									alleleDist.values().stream().forEach(a -> {fsCount.addAndGet(a[0]); rsCount.addAndGet(a[1]);});
									boolean sbiasCov = ! AccumulatorUtils.areBothStrandsRepresented(fsCount, rsCount, sBiasCovPercentage);
									
									
									for (int gtI : gts) {
										if (gtI > 0) {
											int [] iArray = alleleDist.get(alts[gtI - 1]);
											int min = Math.min(iArray[0], iArray[1]);
											
											if ( ((double) min / (iArray[0] + iArray[1])) * 100 < sBiasAltPercentage) {
												StringUtils.updateStringBuilder(fSb, sbiasCov ? SnpUtils.STRAND_BIAS_COVERAGE : SnpUtils.STRAND_BIAS_ALT, Constants.SEMI_COLON);
												break;
											}
										}
									}
								}
								
								/*
								 * HOM
								 */
								if (lhomo >= IndelConfidenceMode.DEFAULT_HOMN) {
									StringUtils.updateStringBuilder(fSb, "HOM", Constants.SEMI_COLON);
								}
								
								/*
								 * check AD and NNS
								 */
								if ( ! isGATKCall) {
									int [] nns = getFieldOfInts(nnsArr[i]);
									if ( ! allValuesAboveThreshold(nns, nnsCount)) {
										StringUtils.updateStringBuilder(fSb, "NNS", Constants.SEMI_COLON);
									}
								}
								
								int [] altADs = getAltCoveragesFromADField(adArr[i]);
								if ( ! (percentageMode ?  allValuesAboveThreshold(altADs, cov, mrPercentage) : allValuesAboveThreshold(altADs, mrCount))) {
									StringUtils.updateStringBuilder(fSb, "MR", Constants.SEMI_COLON);
								}
								
								/*
								 * end of read check
								 */
								if ( ! isGATKCall && null != eorArr) {
									int eor = endsOfReads(alts, gt, alleleDist, eorArr[i]);
									if (eor > 0) {
										StringUtils.updateStringBuilder(fSb, "5BP=" + eor, Constants.SEMI_COLON);
									}
								}
							}
						}
						
						filterArr[i] = fSb.length() == 0 ? VcfHeaderUtils.FILTER_PASS : fSb.toString();
						/*
						 * deal with homozygous loss instances where we potentially have no coverage in test - still mark as a pass
						 */
						if (null != ccmArr) {
							int ccm = Integer.parseInt(ccmArr[i]);
							if (ccm == 11 || ccm == 21 || ccm == 31 || ccm == 41) {
								filterArr[i] = VcfHeaderUtils.FILTER_PASS;
							}
						}
					}
				}
				
				/*
				 * update vcf record with (possibly) updated ffs
				 */
				vcf.setFormatFields(VcfUtils.convertFFMapToList(ffMap));
				
				if (Arrays.stream(filterArr).distinct().count() == 1 && filterArr[0].equals(VcfHeaderUtils.FILTER_PASS)) {
					pass++;
				} else {
					fail++;
				}
			}
		}
		
		logger.info("Confidence breakdown, pass: " + pass + ", fail: " + fail );
 
		//add header line  set number to 1
		if (null != header ) {
			header.addInfo(VcfHeaderUtils.INFO_CONFIDENCE, "1", "String", DESCRIPTION_INFO_CONFIDENCE);
		}
	}
	
	public static int endsOfReads(String [] alts, String gt, Map<String, int[]> oabsMap, String eor) {
		if ((null == oabsMap || oabsMap.isEmpty()) 
				|| StringUtils.isNullOrEmptyOrMissingData(eor)
				|| (null == alts || alts.length == 0)
				|| "0/0".equals(gt)
				|| "./.".equals(gt)
				) {
			return 0;
		}
		
		int i = 1;
		int maxBP = 0;
//		Map<String, int[]> oabsMap = VcfUtils.getAllelicCoverageWithStrand(oabs);
		Map<String, int[]> eorMap = VcfUtils.getAllelicCoverageWithStrand(eor);
		for (String alt : alts) {
			if (gt.contains(""+i)) {
				int [] altCov = oabsMap.getOrDefault(alt, new int [] {0,0});
				int [] altCovEOR = eorMap.getOrDefault(alt, new int [] {0,0});
				int middleOfReadForwardStrand = altCov[0] - altCovEOR[0];
				int middleOfReadReverseStrand = altCov[1] - altCovEOR[1];
				int middleOfReadCount = middleOfReadForwardStrand + middleOfReadReverseStrand;
				int endOfReadCount = altCovEOR[0] +  altCovEOR[1];
				
				if (middleOfReadCount >= 5 && (middleOfReadReverseStrand > 0 && middleOfReadForwardStrand > 0)) {
					// all good
				} else {
					if ((endOfReadCount) > maxBP) {
						maxBP = endOfReadCount;
					}
				}
			}
			i++;
		}
		return maxBP;
	}
	
	public static boolean allValuesAboveThreshold(int[] values, int threshold) {
		return Arrays.stream(values).allMatch(i -> i >= threshold);
	}
	public static boolean allValuesAboveThreshold(int[] values, int coverage, double percentageCutoff) {
		return Arrays.stream(values).allMatch(i -> ((double)i / coverage) * 100 >= percentageCutoff);
	}

	 public static int [] getFieldOfInts(VcfFormatFieldRecord formatField ,String key) {
		 String value = formatField.getField(key);
		 return getFieldOfInts(value);
	 }
	 
	 public static int [] getFieldOfInts(String value) {
		 if (StringUtils.isNullOrEmptyOrMissingData(value)) {
			 return new int[]{0};
		 }
		 int cI = value.indexOf(Constants.COMMA);
		 if (cI == -1) return new int[] {Integer.parseInt(value)};
		 return new int[]{Integer.parseInt(value.substring(0,cI)), Integer.parseInt(value.substring(cI + 1))}; 
	 }
	 
	 public static int [] getAltCoveragesFromADField(String ad) {
		 if (StringUtils.isNullOrEmptyOrMissingData(ad)) {
			 return new int[]{0};
		 }
		 String [] adArray = ad.split(Constants.COMMA_STRING);
		 if (adArray.length < 2) {
			 return new int[]{0};
		 }
		 int [] adIntArray = new int[adArray.length -1];
		 for (int i = 1 ; i < adArray.length ; i++) {
			 adIntArray[i-1] = Integer.parseInt(adArray[i]);
		 }
		 return adIntArray;
	 }
	 
	@Override
	void addAnnotation(String dbfile) throws IOException {
		// TODO Auto-generated method stub		
	}  
}	
	
