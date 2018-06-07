/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;


import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.ContentType;
import org.qcmg.common.vcf.VcfFileMeta;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils.VcfInfoType;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Main;
import au.edu.qimr.qannotate.Options;

/**
 * 
 * This class takes an existing in-valid vcf file, and makes it valid.
 * It does this by splitting the joined format columns into seperate columns, thereby removing the '&' operator.
 * 
 * 
 * @author Oliver Holmes
 *
 */

public class MakeValidMode extends AbstractMode {
	
	private static final QLogger logger = QLoggerFactory.getLogger(MakeValidMode.class);
	public static final String VCF_DELIM_STRING = Constants.VCF_MERGE_DELIM+"";
	
	private VcfFileMeta meta;
	
	//filters 
	
	public static final String DESCRIPTION_INFO_CONFIDENCE ="set to HIGH if the variants passed all filter, "
			+ "appeared on more than 4 novel stars reads and more than 5 reads contains variants, is adjacent to reference sequence with less than 6 homopolymer base; "
			+ "Or set to LOW if the variants passed MIUN/MIN/GERM filter, appeared on more than 4 novel stars reads and more than4 reads contains variants;"
			+ "Otherwise set to Zero if the variants didn't matched one of above conditions.";


	
	public MakeValidMode( Options options) throws IOException {
		logger.tool("input: " + options.getInputFileName());
		String refFile = options.getDatabaseFileName();
		if ( ! StringUtils.isNullOrEmpty(refFile)) {
			logger.tool("reference file: " + refFile);
		}
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
        
        processVcfFile(options.getInputFileName(), options.getOutputFileName(), options.getCommandLine(), refFile);
	}
	
	private void processVcfFile(String input, String output, String cmd, String ref) throws FileNotFoundException, IOException {
		File inputFile = new File(input);
		
		try (VCFFileReader reader = VCFFileReader.createStream(inputFile);
				VCFFileWriter writer = new VCFFileWriter(new File(output));) {
			
			VcfHeader inputHeader = reader.getHeader();
			
			/*
			 * check that input file is in need of a valid makeover
			 */
			if (doesMakeValidNeedToBeRun(inputHeader)) {
			
				VcfHeader outputHeader = reheader(inputHeader, cmd, input,  ref);
				meta = new VcfFileMeta(outputHeader);
				boolean singleSample = ! ContentType.multipleSamples(meta.getType());
				logger.info("new vcf file meta: " + meta.getType());
				logger.info("singleSample: " + singleSample);
				
				for(final VcfHeaderRecord record: outputHeader)  {
					writer.addHeader(record.toString());
				}
				Map<String, short[]> callerPositionsMap = meta.getCallerSamplePositions();
				for (VcfRecord vcf : reader) {
					if ( ! invalidRefAndAlt(vcf)) {
						processVcfRecord(vcf, callerPositionsMap, singleSample);
						writer.add(vcf);
					}
				}
			} else {
				logger.info("Input vcf file already has multiple callers");
			}
		}
	}
	
	public static boolean doesMakeValidNeedToBeRun(VcfHeader header) {
		VcfFileMeta m = new VcfFileMeta(header);
		return ! ContentType.multipleCallers(m.getType());
	}
	
	public static void processVcfRecord(VcfRecord v, Map<String, short[]> callerPositionsMap) {
		processVcfRecord(v, callerPositionsMap, false);
	}
	public static void processVcfRecord(VcfRecord v, Map<String, short[]> callerPositionsMap, boolean singleSample) {
		makeValid(v);
		/*
		 * only run CCM mode if we have multiple samples
		 */
		if  ( ! singleSample) {
			addCCM(v, callerPositionsMap);
		}
		addFormatDetails(v, callerPositionsMap, singleSample);
	}
	
	/*
	 * adding INF and FT format fields
	 * will also add AD and DP, should they not be present
	 */
	public static void addFormatDetails(VcfRecord v, Map<String, short[]> callerPositionsMap) {
		addFormatDetails(v, callerPositionsMap, false);
	}
	public static void addFormatDetails(VcfRecord v, Map<String, short[]> callerPositionsMap, boolean singleSample) {
		/*
		 * need to do this for all callers
		 */
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String[] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		
		
		if (null != gtArr) {
			
			String [] infArray = new String[gtArr.length];
			String [] ftArray = new String[gtArr.length];
			Arrays.fill(infArray, Constants.MISSING_DATA_STRING);
			Arrays.fill(ftArray, Constants.MISSING_DATA_STRING);
			
			/*
			 * if single sample, skip
			 */
			if (gtArr.length > 1 &&  ! callerPositionsMap.isEmpty()) {
				
				/*
				 * do this for each caller
				 */
				String info = v.getInfo();
				VcfInfoFieldRecord infoRec = v.getInfoRecord();
				String conf = infoRec.getField("CONF");
				boolean som1 = info.contains("SOMATIC_1") || (info.contains("SOMATIC") && info.contains("IN=1;"));
				boolean som2 = info.contains("SOMATIC_2") || (info.contains("SOMATIC") && info.contains("IN=2;"));
				
				boolean pass1 = conf.contains("HIGH_1") || (conf.contains("HIGH") && info.contains("IN=1;"));
				boolean pass2 = conf.contains("HIGH_2") || (conf.contains("HIGH") && info.contains("IN=2;"));
				
				
				short[] firstCaller = callerPositionsMap.get("1");
				short[] secondCaller = callerPositionsMap.get("2");
				
				if (som1) {
					infArray[firstCaller[1] - 1] = "SOMATIC";
				}
				if (som2) {
					infArray[secondCaller[1] - 1] = "SOMATIC";
				}
				
				if (pass1) {
					if ( ! singleSample) ftArray[firstCaller[0] - 1] = "PASS";
					ftArray[firstCaller[1] - 1] = "PASS";
				}
				if (pass2) {
					if ( ! singleSample) ftArray[secondCaller[0] - 1] = "PASS";
					ftArray[secondCaller[1] - 1] = "PASS";
				}
			}
			
			/*
			 * add next
			 */
			String [][] adAndDpArrays = getFFValues(ffMap, v.getRef(), v.getAlt());
			
			/*
			 * update the map with the new entries, and update the vcf record
			 */
			ffMap.put(VcfHeaderUtils.FORMAT_INFO, infArray);
			ffMap.put(VcfHeaderUtils.FORMAT_FILTER, ftArray);
			ffMap.put(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS, adAndDpArrays[0]);
			ffMap.put(VcfHeaderUtils.FORMAT_READ_DEPTH, adAndDpArrays[1]);
			v.setFormatFields(VcfUtils.convertFFMapToList(ffMap));
		}
	}
	
	public static String [][] getFFValues(Map<String, String[]> ffMap, String ref, String alts) {
		
		String [] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		String [] acArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		String [] adArr = ffMap.getOrDefault(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS, createMissingDataArray(gtArr.length));
		String [] dpArr = ffMap.getOrDefault(VcfHeaderUtils.FORMAT_READ_DEPTH, createMissingDataArray(gtArr.length));
		
		List<String> list = getRefAndAltsAsList(ref, alts);
		
		for (int i = 0 ; i < gtArr.length ; i++) {
			String gt = gtArr[i];
			String ac = null != acArr ? acArr[i] : null;
			if ( ! StringUtils.isNullOrEmptyOrMissingData(gt) && ! Constants.MISSING_GT.equals(gt)
					&& ! StringUtils.isNullOrEmptyOrMissingData(ac) && ! Constants.MISSING_GT.equals(ac)) {
				
				/*
				 * if existing value is missing data, do something
				 */
				String ad = adArr[i];
				Map<String, Integer> alleleicCounts = null;
				if (StringUtils.isNullOrEmptyOrMissingData(ad)) {
					alleleicCounts = VcfUtils.getAllelicCoverageFromAC(ac);
					StringBuilder sb = new StringBuilder();
					for (String s : list) {
						StringUtils.updateStringBuilder(sb, alleleicCounts.getOrDefault(s, 0).toString(), Constants.COMMA);
					}
					adArr[i] = sb.toString();
				}
				
				String dp = dpArr[i];
				if (StringUtils.isNullOrEmptyOrMissingData(dp)) {
					if (null == alleleicCounts) {
						alleleicCounts = VcfUtils.getAllelicCoverageFromAC(ac);
					}
					dpArr[i] = alleleicCounts.values().stream().mapToInt(z -> z.intValue()).sum() + "";
				}
			}
		}
		return new String[][]{adArr, dpArr};
	}
	
	public static String[] createMissingDataArray(int i) {
		if (i <0 ) throw new IllegalArgumentException("Negative value passed to createMissingDataArray method!");
		
		String [] arr = new String[i];
		Arrays.fill(arr, Constants.MISSING_DATA_STRING);
		return arr;
	}
	
	public static void addCCM(VcfRecord vcf, Map<String, short[]> callerPositionsMap) {
		/*
		 * don't do this if each caller only has a single sample
		 */
		 CCMMode.updateVcfRecordWithCCM(vcf, callerPositionsMap, null);
	}
	
	
	/**
	 * Return false if the ref is equal to M, RR or the alt
	 * @param vcf
	 * @return
	 */
	public static boolean invalidRefAndAlt(VcfRecord vcf) {
		return vcf.getRef().equals("M") || vcf.getRef().equals("RR") || vcf.getRef().equals(vcf.getAlt());
	}
	
	/**
	 * THIS METHOD HAS SIDE EFFECTS
	 */
	public static void makeValid(VcfRecord vcf) {
		
		/*
		 * filter
		 */
		fixFilters(vcf);
		
		/*
		 * need to remove AC and AN from info field
		 */
		removeACandAN(vcf);
		
		
		String info = vcf.getInfo();
		List<String> existingFF = vcf.getFormatFields(); 
		int numberOfSamples = existingFF.size() - 1;
		
		if (info.contains("IN=1,2")) {
			
			List<String> updatedFF = new ArrayList<>();
			// add header
			updatedFF.add(existingFF.get(0));

			List<String[]> newFormatFieldsBySample = new ArrayList<>();
			
			for (int i = 1; i <= numberOfSamples ; i++) {
				String s = existingFF.get(i);
				/*
				 * need to split this out into 2
				 */
				String [] splitString = splitFormatField(s);
				newFormatFieldsBySample.add(splitString);
			}
			
			for (String [] array : newFormatFieldsBySample) {
				updatedFF.add(array[0]);
			}
			for (String [] array : newFormatFieldsBySample) {
				updatedFF.add(array[1]);
			}
			
			vcf.setFormatFields(updatedFF);
			
		} else if (info.contains("IN=2")) {
			/*
			 * need to add empty format field values for caller 1
			 */
			for (int i = 0 ; i < numberOfSamples ; i++) {
				VcfUtils.addMissingDataToFormatFields(vcf, 1);
			}
		} else if (info.contains("IN=1")) {
			/*
			 * need to add empty format field values for caller 2
			 */
			for (int i = 0 ; i < numberOfSamples ; i++) {
				VcfUtils.addMissingDataToFormatFields(vcf, numberOfSamples+1);
			}
			
		} else {
			logger.warn("Could not determine which caller provided vcf record: " + vcf);
		}
		
		/*
		 * format
		 * get refreshed format fields
		 */
		existingFF = vcf.getFormatFields();
		String existingHeaders = existingFF.get(0);
		if (existingHeaders.contains("PL")) {
			String newHeader = existingHeaders.replace(":PL", "");
			Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(existingFF);
			vcf.setFormatFields(VcfUtils.convertFFMapToList(ffMap, newHeader.split(Constants.COLON_STRING)));
		}
		
		/*
		 * need to add GT for compound snps
		 */
		if (existingHeaders.contains("ACCS")) {
			existingFF = vcf.getFormatFields();
			String ref = vcf.getRef();
			String alts = vcf.getAlt();
			List<String> gts = new ArrayList<>();
			gts.add("GT:ACCS");
			
			for (int j = 1 ; j < existingFF.size() ; j++) {
				String accsGT = getGTForCS(ref, alts, existingFF.get(j));
				gts.add(accsGT + Constants.COLON + existingFF.get(j));
			}
			
			vcf.setFormatFields(gts);
		}
		
		if (vcf.getAlt().contains(Constants.COMMA_STRING)) {
			updateVCfRecordGTField(vcf);
		}
	}
	
	public static void fixFilters(VcfRecord vcf) {
		String filter = vcf.getFilter();
		int index = filter.indexOf(VcfHeaderUtils.FILTER_END_OF_READ);
		if (index > -1) {
		/*
		 * split by semi-colon, and reconstruct without number
		 */
			String [] filterArray = filter.split(Constants.SEMI_COLON_STRING);
			if (filterArray.length == 1) {
				vcf.setFilter(VcfHeaderUtils.FILTER_END_OF_READ);
			} else {
				StringBuilder sb = new StringBuilder();
				for (String f : filterArray) {
					if (f.startsWith(VcfHeaderUtils.FILTER_END_OF_READ)) {
						StringUtils.updateStringBuilder(sb, VcfHeaderUtils.FILTER_END_OF_READ, Constants.SEMI_COLON);
					} else {
						StringUtils.updateStringBuilder(sb, f, Constants.SEMI_COLON);
					}
				}
				vcf.setFilter(sb.toString());
			}
		}
	}
	
	public static void removeACandAN(VcfRecord v) {
		VcfInfoFieldRecord infoRec = v.getInfoRecord();
		if (null != infoRec) {
			infoRec.removeField("AN");
			infoRec.removeField("AC");
			infoRec.removeField("AF");
			infoRec.removeField("MLEAF");
			infoRec.removeField("MLEAC");
		}
	}
	
	public static void updateVCfRecordGTField(VcfRecord vcf) {
		List<String> existingFF = vcf.getFormatFields();
		String existingHeaders = existingFF.get(0);
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(existingFF);
		
		String ref = vcf.getRef();
		String alts = vcf.getAlt();
		
		String [] gds = ffMap.get("GD");
		String [] gts = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		String [] replacementGTs = new String [gds.length]; 
		for (int i = 0 ; i < gds.length ; i++) {
			String gd = gds[i];
			if (Constants.MISSING_DATA_STRING.equals(gd)) {
				replacementGTs[i] = gts[i];
			} else {
				replacementGTs[i] = getUpdatedGT(ref, alts, gd);
			}
		}
		
		ffMap.put(VcfHeaderUtils.FORMAT_GENOTYPE, replacementGTs);
		vcf.setFormatFields(VcfUtils.convertFFMapToList(ffMap, existingHeaders.split(Constants.COLON_STRING)));
	}
	
	public static String getUpdatedGT(String ref, String alts, String gd) {
		List<String> list = getRefAndAltsAsList(ref, alts);
		
		int firstNumber =  list.indexOf(gd.charAt(0)+"");
		int secondNumber =  list.indexOf(gd.charAt(2)+"");
		
		return Math.min(firstNumber, secondNumber) + "/" + Math.max(firstNumber, secondNumber);
	}

	/**
	 * Creates a list based on the ref and alts of a VcfRecord
	 * alts will be split by comma so that each alt will have its own entry in the list.
	 * 
	 * @param ref
	 * @param alts
	 * @return
	 */
	public static List<String> getRefAndAltsAsList(String ref, String alts) {
		List<String> list = new ArrayList<>();
		list.add(ref);
		list.addAll(Arrays.asList(alts.split(Constants.COMMA_STRING)));
		return list;
	}
	
	/**
	 * Very simplisticly, if we see the ref and alt in ACCS, its 0/1, just alt, 1/1, just ref 0/0
	 * This is just a stopgap so that the file will validate. The newer qsnp will handle compound snps more appropriately
	 * @param ref
	 * @param alts
	 * @param accs
	 * @return
	 */
	public static String getGTForCS(String ref, String alts, String accs) {
		boolean refSeen = accs.contains(ref);
		boolean altSeen = false;
		if (alts.contains(Constants.COMMA_STRING)) {	
		} else {
			altSeen = accs.contains(alts);
		}
		
		return refSeen && altSeen ? "0/1" : refSeen ? "0/0" : altSeen ? "1/1" : Constants.MISSING_GT;
	}
	
	/**
	 * Splits the format field based on the '&' char, and return 2 strings, one for erach caller
	 */
	public static String[] splitFormatField(String ff) {
		StringBuilder s1 = new StringBuilder();
		StringBuilder s2 = new StringBuilder();
		String [] ffArray = ff.split(Constants.COLON_STRING);
		for (String s : ffArray) {
			if (s.contains(VCF_DELIM_STRING)) {
				String [] sArray = s.split(VCF_DELIM_STRING);
				StringUtils.updateStringBuilder(s1, sArray[0], Constants.COLON);
				StringUtils.updateStringBuilder(s2, sArray[1], Constants.COLON);
			} else {
				StringUtils.updateStringBuilder(s1, s, Constants.COLON);
				StringUtils.updateStringBuilder(s2, s, Constants.COLON);
			}
		}
		
		return new String[]{s1.toString(), s2.toString()};
	}

	static VcfHeader reheader(VcfHeader header, String cmd, String inputVcfName, String refFile) {	
		 
		VcfHeader myHeader = header; 
		VcfFileMeta inputMeta = new VcfFileMeta(myHeader);
 		
		String version = Main.class.getPackage().getImplementationVersion();
		String pg = Main.class.getPackage().getImplementationTitle();
		final String fileDate = new SimpleDateFormat(AbstractMode.DATE_FORMAT_STRING).format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();
		
		myHeader.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version);
		
		boolean contigsAlreadyExist = VcfHeaderUtils.containsContigs(myHeader);
		if ( ! contigsAlreadyExist && ! StringUtils.isNullOrEmpty(refFile)) {
			ReferenceSequenceFile ref = ReferenceSequenceFileFactory.getReferenceSequenceFile(new File(refFile));
			myHeader.addOrReplace(VcfHeaderUtils.HEADER_LINE_REF + "=file://" + refFile);
			for (SAMSequenceRecord ssr : ref.getSequenceDictionary().getSequences()) {
				myHeader.addOrReplace(VcfHeaderUtils.HEADER_LINE_CONTIG + "=<ID=" + ssr.getSequenceName()+",length="+ssr.getSequenceLength()+",URL="+refFile + ">");
			}
		}
		
		/*
		 * The following header lines need to be adjusted so as to be valid:
		 * HOM, CONF, GERM, AC, ACCS, SOMATIC_1, 5BP
		 */
		myHeader.addInfo(VcfHeaderUtils.INFO_HOM,  ".", "String",VcfHeaderUtils.INFO_HOM_DESC);
		myHeader.addInfo(VcfHeaderUtils.INFO_CONFIDENCE, ".", "String", DESCRIPTION_INFO_CONFIDENCE);
		myHeader.addInfo(VcfHeaderUtils.INFO_GERMLINE, ".", "String", VcfHeaderUtils.INFO_GERMLINE_DESC);
		myHeader.addInfo(VcfHeaderUtils.INFO_DB,  "0", VcfInfoType.Flag.name(),VcfHeaderUtils.INFO_DB_DESC);
		myHeader.addFilter(VcfHeaderUtils.FILTER_END_OF_READ,VcfHeaderUtils.FILTER_END_OF_READ_DESC); 
		myHeader.addFormat(VcfHeaderUtils.FORMAT_ALLELE_COUNT, ".", "String",VcfHeaderUtils.FORMAT_ALLELE_COUNT_DESC);
		myHeader.addFormat(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP, ".", "String",VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP_DESC);
		myHeader.addFormat(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND, ".", "String",VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND_DESC);
		myHeader.addFormat(VcfHeaderUtils.FORMAT_FILTER, ".", "String",VcfHeaderUtils.FORMAT_FILTER_DESCRIPTION);
		myHeader.addFormat(VcfHeaderUtils.FORMAT_INFO, ".", "String",VcfHeaderUtils.FORMAT_INFO_DESCRIPTION);
		myHeader.addOrReplace(VcfHeaderUtils.FORMAT +"=<ID=" + VcfHeaderUtils.FORMAT_CCM + ",Number=.,Type=String,Description=\"" + VcfHeaderUtils.FORMAT_CCM_DESC + "\">" );
		myHeader.addOrReplace(VcfHeaderUtils.FORMAT +"=<ID=" + VcfHeaderUtils.FORMAT_CCC + ",Number=.,Type=String,Description=\"" + VcfHeaderUtils.FORMAT_CCC_DESC + "\">" );
		for (int i = 1 ; i <= 2 ; i++) {
			String subscript = i == 1 ? "st" : "nd";
			myHeader.addInfo(VcfHeaderUtils.INFO_SOMATIC + "_" + i, "0", "Flag", "Indicates that the " + i + subscript + " input file considered this record to be somatic.");
			myHeader.addFilter(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_12 + "_" + i, "Less than 12 reads coverage in normal");
			myHeader.addFilter(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_8 + "_" + i,"Less than 8 reads coverage in normal");  
			myHeader.addFilter(VcfHeaderUtils.FILTER_COVERAGE_TUMOUR + "_" + i,"Less than 8 reads coverage in tumour"); 
			myHeader.addFilter(VcfHeaderUtils.FILTER_SAME_ALLELE_NORMAL + "_" + i,"Less than 3 reads of same allele in normal");  
			myHeader.addFilter(VcfHeaderUtils.FILTER_SAME_ALLELE_TUMOUR + "_" + i,"Less than 3 reads of same allele in tumour");  
			myHeader.addFilter(VcfHeaderUtils.FILTER_MUTATION_IN_NORMAL + "_" + i,"Mutation also found in pileup of normal");  
			myHeader.addFilter(VcfHeaderUtils.FILTER_MUTATION_IN_UNFILTERED_NORMAL + "_" + i,"Mutation also found in pileup of (unfiltered) normal");  
			myHeader.addFilter(VcfHeaderUtils.FILTER_GERMLINE + "_" + i,"Mutation is a germline variant in another patient");  
			myHeader.addFilter(VcfHeaderUtils.FILTER_NOVEL_STARTS + "_" + i,"Less than 4 novel starts not considering read pair");  
			myHeader.addFilter(VcfHeaderUtils.FILTER_MUTANT_READS + "_" + i,"Less than 5 mutant reads"); 
			myHeader.addFilter(VcfHeaderUtils.FILTER_MUTATION_EQUALS_REF + "_" + i,"Mutation equals reference"); 
			myHeader.addFilter(VcfHeaderUtils.FILTER_NO_CALL_IN_TEST + "_" + i,"No call in test"); 
			myHeader.addFilter(VcfHeaderUtils.FILTER_STRAND_BIAS_ALT + "_" + i,"Alternate allele on only one strand (or percentage alternate allele on other strand is less than 5%)"); 
			myHeader.addFilter(VcfHeaderUtils.FILTER_STRAND_BIAS_COV + "_" + i,"Sequence coverage on only one strand (or percentage coverage on other strand is less than 5%)"); 
			myHeader.addFilter(VcfHeaderUtils.FILTER_STRAND_BIAS_COV + "_" + i,"Sequence coverage on only one strand (or percentage coverage on other strand is less than 5%)"); 
			myHeader.addFilter(VcfHeaderUtils.FILTER_PASS + "_" + i,"Record has passed all filters"); 
		}
		
		String inputUuid = (myHeader.getUUID() == null)? null:  myHeader.getUUID().getMetaValue(); //new VcfHeaderUtils.SplitMetaRecord(myHeader.getUUID()).getValue();   
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName);
		
		if(version == null) version = Constants.NULL_STRING_UPPER_CASE;
	    if(pg == null ) pg = Constants.NULL_STRING_UPPER_CASE;
	    if(cmd == null) cmd = Constants.NULL_STRING_UPPER_CASE;
		VcfHeaderUtils.addQPGLineToHeader(myHeader, pg, version, cmd);
		String[] exsitIds = myHeader.getSampleId();
		
		boolean multipleSamples = ContentType.multipleSamples(inputMeta.getType());
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + exsitIds[0] + "_1\t" + (multipleSamples ? exsitIds[1] + "_1\t" : "") +  exsitIds[0] + "_2\t" + (multipleSamples ? exsitIds[1] + "_2\t" : ""));
		
		return myHeader;			
	}

	@Override
	void addAnnotation(String dbfile) throws IOException {
		// TODO Auto-generated method stub		
	}  
}	
	
