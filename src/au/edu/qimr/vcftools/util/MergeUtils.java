package au.edu.qimr.vcftools.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.*;

import au.edu.qimr.vcftools.Rule;

public class MergeUtils {
	
	private static final String GENOTYPE = "##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">";
	private static final String GENOTYPEDescription = "Genotype";
	private static final QLogger logger = QLoggerFactory.getLogger(MergeUtils.class);

	public static List<String> mergeOtherHeaderRecords(List<VcfHeaderRecord> ...  loloRecs) {
		if (null == loloRecs || loloRecs.length == 0) {
			return Collections.emptyList();
		}
		
		AtomicInteger prefix = new AtomicInteger(1);
		List<String> mergedRecs = new ArrayList<>();
		Arrays.stream(loloRecs)
			.filter(list -> list != null && ! list.isEmpty())
			.forEach(list -> {
				mergedRecs.addAll(list.stream()
						.filter(r -> r != null).map(r -> r.toString().replaceAll(Constants.DOUBLE_HASH, Constants.DOUBLE_HASH + prefix.get() + Constants.COLON))
						.collect(Collectors.toList()));
				prefix.incrementAndGet();
			});
			
		return mergedRecs;
	}

	
	public static Pair<VcfHeader, Rule> getMergedHeaderAndRules(VcfHeader ... headers) {
		
		if(!canMergeBePerformed(headers)){			
			logger.warn("Unable to perform merge - please check that the vcf headers contain the same samples in the same order");			
			return null;
		}
		
		Map<String, String> infoRule = new HashMap<>();
		Map<String, String> formatRule = new HashMap<>(); 
		Map<String, String> filterRule = new HashMap<>(); 
		
		//get Filter, INFO and FORMAT from first header
		VcfHeader mergedHeader = new VcfHeader();
		String[] keys = new String[]{VcfHeader.HEADER_LINE_FILTER, VcfHeader.HEADER_LINE_FORMAT, VcfHeader.HEADER_LINE_INFO};
		for(String key : keys)
			for(VcfHeaderRecord re : headers[0].getRecords(key)) 
				mergedHeader.addOrReplace(re);	
		
		for(int i = 1; i < headers.length; i++){
			for(String key : keys){ 			 
				for(VcfHeaderRecord re : headers[i].getRecords(key)){
					VcfHeaderRecord re0 = mergedHeader.getIDRecord(key, re.getId());
					if(re0 == null ){
						mergedHeader.addOrReplace(re);
					}else if(re0.getSubFieldValue("Description").equals(re.getSubFieldValue("Description"))  
							||  re0.toString().equals(GENOTYPE ) || re.toString() .equals(GENOTYPE )  ){						 
						 // Just match on description - ignore type and number for now (ever??)...						 
						logger.info("Found identical header entry (apart from type, and number): " + re0.toString());					 
							 
					}else {		
						String newId =  re.getId() + i;
						String newRec = ""; //key+"=<";
						for(Pair p: re.getSubFields())
							if(p.getLeft().equals("ID"))
								newRec += "," + p.getLeft() + "=" + newId; 
							else
								newRec += "," + p.getLeft() + "=" + p.getRight(); 
							
						if(key.equals(VcfHeader.HEADER_LINE_FILTER)) filterRule.put(re.getId(), newId);
						else if(key.equals(VcfHeader.HEADER_LINE_FORMAT)) formatRule.put(re.getId(), newId);
							else if(key.equals(VcfHeader.HEADER_LINE_INFO)) formatRule.put(re.getId(),  newId);
						
						//remove leading "," and parse to structured meta info line
						mergedHeader.addOrReplace(key+"=<" + newRec.substring(1) + ">");;					 				
						logger.info("bumping id from " + re.getId() + " to " + newId + " and adding to new header!");						 
					}				
				}
			}
		}
		
			
		/*
		 * add IN= to 
		 */
		logger.info("checking that no existing info lines start with "+Constants.VCF_MERGE_INFO+"= ");
		if ( mergedHeader.getInfoRecord(Constants.VCF_MERGE_INFO) != null) {
			logger.warn("Can't use "+Constants.VCF_MERGE_INFO+"= to mark records as having come from a particular input file - "+Constants.VCF_MERGE_INFO+"= is already in use!");
		}
		
		mergedHeader.addInfo(Constants.VCF_MERGE_INFO, ".","Integer", VcfHeaderUtils.DESCRITPION_MERGE_IN);
		//		 "Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file");
		
		/*
		 * make sure SOMATIC has been added, and add the _n entry
		 */
		if ( mergedHeader.getInfoRecord(VcfHeaderUtils.INFO_SOMATIC) == null) {
			mergedHeader.addInfo(VcfHeaderUtils.INFO_SOMATIC, "0", "Flag", VcfHeaderUtils.INFO_SOMATIC_DESC);
		}
		mergedHeader.addInfo(VcfHeaderUtils.INFO_SOMATIC + "_n", "0", "Flag", "Indicates that the nth input file considered this record to be somatic. Multiple values are allowed which indicate that more than 1 input file consider this record to be somatic");
			
		mergedHeader.addOrReplace(headers[0].getChrom().toString());
		
		Rule r = new Rule(headers.length);
		for (int i = 0 ; i < headers.length ; i++) {
			r.setRules(i, filterRule, infoRule, formatRule);
		}
		
		return Pair.of(mergedHeader,  r);
					
	}

	/**
	 * Merge vcf records that have same position, ref, alt and number of samples into a single record
	 */
	public static VcfRecord mergeRecords(Map<Integer, Map<String, String>> rules, VcfRecord ... records) {
		if (null == records || records.length == 0) {
			throw new IllegalArgumentException("MergeUtils.mergeRecords called will no records!!!");
		}
		
		/*
		 * Get common values from 1st record
		 */ 
//		VcfRecord mergedRecord =  VcfUtils.createVcfRecord(records[0].getChrPosition(),   records[0].getId(), records[0].getRef(), records[0].getAlt());
		VcfRecord mergedRecord =  new VcfRecord.Builder(records[0].getChrPosition(), records[0].getRef())
									.id(records[0].getId()).allele(records[0].getAlt()).build();
				
		
		/*
		 * Update id, info, filter, and format fields
		 */
		
		for (int i = 0 ; i < records.length ; i++) {
			VcfRecord r = records[i];
			Map<String, String> thisRecordsRules = null != rules ? rules.get(i) : null;
			String suffix = "_" + (i+1);
			
			mergedRecord.appendId(r.getId());
			
			if (r.getInfo().contains(SnpUtils.SOMATIC)) {
				// replace with suffix
				r.setInfo(r.getInfo().replace(SnpUtils.SOMATIC, SnpUtils.SOMATIC + suffix));
			}
			
			if (null != thisRecordsRules && ! thisRecordsRules.isEmpty()) {
				
				/*
				 * INFO
				 */
				for (String s : r.getInfo().split(Constants.SEMI_COLON_STRING)) {
					int equalsIndex = s.indexOf(Constants.EQ);
					String key = equalsIndex > -1 ? s.substring(0, equalsIndex) : s;
					String replacementKey = thisRecordsRules.get(key);
					if (null == replacementKey) {
						mergedRecord.getInfoRecord().appendField(key, (equalsIndex > -1) ? s.substring(equalsIndex+1) : s);
					} else {
						mergedRecord.getInfoRecord().addField(replacementKey, (equalsIndex > -1) ? s.substring(equalsIndex+1) : s);
					}
				}
				
			} else {
				mergedRecord.appendInfo(r.getInfo(), false);
			}
			
			/*
			 * FORMAT
			 */
			List<String> rFF =  r.getFormatFields() ;
			if (null != rFF &&  ! rFF.isEmpty()) {
				if (null == thisRecordsRules) {
					VcfUtils.addFormatFieldsToVcf(mergedRecord, r.getFormatFields(), true);
				} else {
					/*
					 * create new header string, substituting in the new values should any be present in the rules map
					 */
					String [] formatHeadersArray = rFF.get(0).split(Constants.COLON_STRING);
					StringBuilder newHeader = new StringBuilder();
					for (String s : formatHeadersArray) {
						if (newHeader.length() > 0) {
							newHeader.append(Constants.COLON);
						}
						String replacementHeaderAttribute = thisRecordsRules.get(s);
						newHeader.append(replacementHeaderAttribute == null ? s : replacementHeaderAttribute);
					}
					if ( ! newHeader.toString().equals(rFF.get(0))) {
						rFF.set(0, newHeader.toString());
					}
					VcfUtils.addFormatFieldsToVcf(mergedRecord, rFF, true);
					
				}
			}
			
			/*
			 * FILTER
			 */
			if (null != r.getFilter()) {
				for (String s : r.getFilter().split(Constants.SEMI_COLON_STRING)) {
					String replacementKey = (null == thisRecordsRules) ? null : thisRecordsRules.get(s);
					if (null == replacementKey) {
						mergedRecord.addFilter(s + suffix);
					} else {
						mergedRecord.addFilter(replacementKey + suffix);
					}
				}
			}
		}
		return mergedRecord;
	}
	
	/*
	 * Need to be same ChrPosition, ref and alt
	 */
//	public static boolean areRecordsEligibleForMerge(VcfRecord ... records) {
//		if (null == records || records.length == 0) {
//			return false;
//		}
//	
//		VcfRecord r1 = records[0];
//		return Arrays.stream(records)
//			.allMatch(r -> r.equals(r1));
//		
//	}
	
	/**
	 * Same sample merge test
	 * Need headers to contain the same samples in the same order
	 * Samples can't be null
	 * 
	 * Returns true if all headers contain the same samples in the same order, false otherwise
	 * 
	 * @param headers
	 * @return
	 */
	public static boolean canMergeBePerformed(VcfHeader ... headers) {
		
		if (null == headers || headers.length == 0) {
			return false;
		}
		String [] firstIds = headers[0].getSampleId();
		if (null == firstIds || firstIds.length == 0) {
			return false;
		}
		
		/*
		 * Get sample ids for each header and check that they are the same for each (number and order)
		 */
//		boolean doSampleIdsMatch = 
		return Arrays.stream(headers)
			.map(header -> header.getSampleId())
			.allMatch(array -> {
				return Arrays.deepEquals(array, firstIds);
			});
	}

}
