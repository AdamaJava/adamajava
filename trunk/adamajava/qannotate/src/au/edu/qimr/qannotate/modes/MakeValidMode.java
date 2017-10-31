/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import static org.qcmg.common.util.Constants.SEMI_COLON;
import static org.qcmg.common.util.SnpUtils.LESS_THAN_12_READS_NORMAL;
import static org.qcmg.common.util.SnpUtils.LESS_THAN_3_READS_NORMAL;
import static org.qcmg.common.util.SnpUtils.MUTATION_IN_UNFILTERED_NORMAL;
import static org.qcmg.common.util.SnpUtils.PASS;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils.VcfInfoType;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.Main;
import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.SampleColumn;

/**
 * 
 * This class takes an existing in-valid vcf file, and makes it valid.
 * It does this by splitting the joined format columns into seperate columns, thereby removing the '&' operator.
 * 
 * 
 * @author Oliver Holmes
 *
 */

public class MakeValidMode extends AbstractMode{
	private static final QLogger logger = QLoggerFactory.getLogger(MakeValidMode.class);
	private static final DateFormat df = new SimpleDateFormat("yyyyMMdd");
	public static final String VCF_DELIM_STRING = Constants.VCF_MERGE_DELIM+"";
	
	
//	static final String[] CLASS_B_FILTERS= new String[] {PASS, MUTATION_IN_UNFILTERED_NORMAL, LESS_THAN_12_READS_NORMAL, LESS_THAN_3_READS_NORMAL};
	
	static final int HIGH_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	static final int LOW_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	
	static final int HIGH_CONF_ALT_FREQ_PASSING_SCORE = 5;	
	static final int LOW_CONF_ALT_FREQ_PASSING_SCORE = 4;	
	
	//filters 
	
	public static final String DESCRITPION_INFO_CONFIDENCE = String.format( "set to HIGH if the variants passed all filter, "
			+ "appeared on more than %d novel stars reads and more than %d reads contains variants, is adjacent to reference sequence with less than %d homopolymer base; "
			+ "Or set to LOW if the variants passed MIUN/MIN/GERM filter, appeared on more than %d novel stars reads and more than %d reads contains variants;"
			+ "Otherwise set to Zero if the variants didn't matched one of above conditions.",
			HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, HIGH_CONF_ALT_FREQ_PASSING_SCORE,IndelConfidenceMode.DEFAULT_HOMN,  LOW_CONF_NOVEL_STARTS_PASSING_SCORE, LOW_CONF_ALT_FREQ_PASSING_SCORE);


	
	//for unit testing
	MakeValidMode(String patient){}

	
	public MakeValidMode( Options options) throws IOException {		 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
        
		inputRecord(new File( options.getInputFileName()));
		
		makeVcfRecordsValid();
		
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()) );	
	}
	
	
	
	private void makeVcfRecordsValid() {
		for (List<VcfRecord> vcfs : positionRecordMap.values()) {
			for (VcfRecord vcf : vcfs){
				makeValid(vcf);
			}
		}
	}
	
	/**
	 * THIS METHOD HAS SIDE EFFECTS
	 */
	public static VcfRecord makeValid(VcfRecord vcf) {
		
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
//				updatedFF.add((i * 2) -1 , splitString[0]);
//				updatedFF.add(i *2, splitString[1]);
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
//			VcfUtils.addFormatFieldsToVcf(vcf, gts);
			
		}
		
		
		if (vcf.getAlt().contains(Constants.COMMA_STRING)) {
			updateVCfRecordGTField(vcf);
		}
		
		
		return vcf;
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
		String [] gts = ffMap.get("GT");
		String [] replacementGTs = new String [gds.length]; 
		for (int i = 0 ; i < gds.length ; i++) {
			String gd = gds[i];
			if (Constants.MISSING_DATA_STRING.equals(gd)) {
				replacementGTs[i] = gts[i];
			} else {
				replacementGTs[i] = getUpdatedGT(ref, alts, gd);
			}
		}
		
		ffMap.put("GT", replacementGTs);
		vcf.setFormatFields(VcfUtils.convertFFMapToList(ffMap, existingHeaders.split(Constants.COLON_STRING)));
	}
	
	public static String getUpdatedGT(String ref, String alts, String gd) {
		List<String> list = new ArrayList<>();
		list.add(ref);
		String [] altsArray = alts.split(Constants.COMMA_STRING);
		for (String a : altsArray) {
			list.add(a);
		}
		
		int firstNumber =  list.indexOf(gd.charAt(0)+"");
		int secondNumber =  list.indexOf(gd.charAt(2)+"");
		
		
		return Math.min(firstNumber, secondNumber) + "/" + Math.max(firstNumber, secondNumber);
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
		
		return refSeen && altSeen ? "0/1" : refSeen ? "0/0" : altSeen ? "1/1" : "./.";
	}
	
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

	static VcfHeader reheader(VcfHeader header, String cmd, String inputVcfName) {	
		 
		VcfHeader myHeader = header;  	
 		
		String version = Main.class.getPackage().getImplementationVersion();
		String pg = Main.class.getPackage().getImplementationTitle();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();
		
		myHeader.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version);
		
		/*
		 * The following header lines need to be adjusted so as to be valid:
		 * HOM, CONF, GERM, AC, ACCS, SOMATIC_1, 5BP
		 */
		myHeader.addInfo(VcfHeaderUtils.INFO_HOM,  ".", "String",VcfHeaderUtils.DESCRITPION_INFO_HOM);
		myHeader.addInfo(VcfHeaderUtils.INFO_CONFIDENCE, ".", "String", DESCRITPION_INFO_CONFIDENCE);
		myHeader.addInfo(VcfHeaderUtils.INFO_GERMLINE, ".", "String", VcfHeaderUtils.DESCRITPION_INFO_GERMLINE);
		myHeader.addInfo(VcfHeaderUtils.INFO_DB,  "0", VcfInfoType.Flag.name(),VcfHeaderUtils.DESCRITPION_INFO_DB);
		myHeader.addFilter(VcfHeaderUtils.FILTER_END_OF_READ,VcfHeaderUtils.FILTER_END_OF_READ_DESC); 
		myHeader.addFormat(VcfHeaderUtils.FORMAT_ALLELE_COUNT, ".", "String",VcfHeaderUtils.FORMAT_ALLELE_COUNT_DESC);
		myHeader.addFormat(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP, ".", "String",VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP_DESC);
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
		
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + exsitIds[0] + "_1\t" + exsitIds[1] + "_1\t" + exsitIds[0] + "_2\t" + exsitIds[1] + "_2");
		
		return myHeader;			
	}
	

	/**
	 * 
	 * @param cmd: add this cmd string into vcf header
	 * @param inputVcfName: add input file name into vcf header
	 * @throws IOException
	 */
	@Override
	void reheader(String cmd, String inputVcfName) throws IOException {	

		if(header == null)
	        try(VCFFileReader reader = new VCFFileReader(inputVcfName)) {
	        	header = reader.getHeader();	
	        	if(header == null)
	        		throw new IOException("can't receive header from vcf file, maybe wrong format: " + inputVcfName);
	        } 	
 
		header = reheader(header, cmd, inputVcfName);			
	}


	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub		
	}  
}	
	
