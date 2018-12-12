package au.edu.qimr.vcftools.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.vcftools.Rule;

public class MergeUtils {
	
	private static final String GENOTYPE = "##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">";
	public static final String FORMAT = "FORMAT";
	private static final QLogger logger = QLoggerFactory.getLogger(MergeUtils.class);

	
	public static Pair<VcfHeader, Rule> getMergedHeaderAndRules(VcfHeader ... headers) {
		
		if(!canMergeBePerformed(headers)){			
			logger.warn("Unable to perform merge - please check that the vcf headers contain the same samples in the same order");			
			return null;
		}
		
		/*
		 * add in qPG lines
		 */
		VcfHeader mergedHeader = new VcfHeader();
		AtomicInteger prefix = new AtomicInteger(1);
		for (VcfHeader h : headers) {
			h.getAllMetaRecords().stream()
			.filter(r -> r.toString().startsWith("##q"))
			.map(r -> r.toString().replaceAll(Constants.DOUBLE_HASH, Constants.DOUBLE_HASH + prefix.get() + Constants.COLON))
			.forEach(r -> mergedHeader.addOrReplace(r));
			prefix.incrementAndGet();
		}
		
		Map<String, String> infoRule = new HashMap<>();
		Map<String, String> formatRule = new HashMap<>(); 
		Map<String, String> filterRule = new HashMap<>(); 
		
		//get Filter, INFO and FORMAT from first header
		
		
		String[] keys = new String[]{VcfHeaderUtils.HEADER_LINE_FILTER, VcfHeaderUtils.HEADER_LINE_FORMAT, VcfHeaderUtils.HEADER_LINE_INFO};
		for(String key : keys) {
			for(VcfHeaderRecord re : headers[0].getRecords(key)) { 
				mergedHeader.addOrReplace(re);	
			}
		}
		
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
						for(Pair<String, String> p: re.getSubFields()) {
							if(p.getLeft().equals("ID")) {
								newRec += "," + p.getLeft() + "=" + newId; 
							} else {
								newRec += "," + p.getLeft() + "=" + p.getRight(); 
							}
						}
						if (key.equals(VcfHeaderUtils.HEADER_LINE_FILTER)) {
							filterRule.put(re.getId(), newId);
						} else if(key.equals(VcfHeaderUtils.HEADER_LINE_FORMAT)) {
							formatRule.put(re.getId(), newId);
						} else if(key.equals(VcfHeaderUtils.HEADER_LINE_INFO)) {
							formatRule.put(re.getId(),  newId);
						}
						
						//remove leading "," and parse to structured meta info line
						mergedHeader.addOrReplace(key + "=<" + newRec.substring(1) + ">");;					 				
						logger.info("bumping id from " + re.getId() + " to " + newId + " and adding to new header!");						 
					}				
				}
			}
		}
		
			
		/*
		 * add IN= to 
		 */
		logger.info("checking that no existing info lines start with " + Constants.VCF_MERGE_INFO + "= ");
		if ( mergedHeader.getInfoRecord(Constants.VCF_MERGE_INFO) != null) {
			logger.warn("Can't use " + Constants.VCF_MERGE_INFO + "= to mark records as having come from a particular input file - " + Constants.VCF_MERGE_INFO + "= is already in use!");
		}
		
		mergedHeader.addInfo(Constants.VCF_MERGE_INFO, ".","Integer", VcfHeaderUtils.DESCRITPION_MERGE_IN);
		//		 "Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file");
		
			
		/*
		 * Build up new CHROM line with the samples updated with the input number appended to them
		 */
		StringBuilder sb = new StringBuilder();
		String [] firstChrLine = headers[0].getChrom().toString().split(Constants.TAB_STRING);
		/*
		 * add the first 9 columns to sb
		 */
		for (int i = 0 ; i < 9 ; i++) {
			StringUtils.updateStringBuilder(sb, firstChrLine[i], Constants.TAB);
		}
		for (int i = 0 ; i < headers.length ; i++) {
			String [] array = headers[i].getChrom().toString().split(Constants.TAB_STRING);
			/*
			 * add every column after FORMAT to sb, appending the numeric identifier
			 */
			boolean go = false;
			for (String s : array) {
				if (go) {
					StringUtils.updateStringBuilder(sb, s + "_" + (i + 1), Constants.TAB);
				}
				if (FORMAT.equals(s)) {
					go = true;
				}
			}
		}
		mergedHeader.addOrReplace(sb.toString());
		
		Rule r = new Rule(headers.length);
		for (int i = 0 ; i < headers.length ; i++) {
			r.setRules(i, filterRule, infoRule, formatRule);
		}
		
		return Pair.of(mergedHeader,  r);
					
	}

	public static Optional<String> getCombinedAlt(VcfRecord ... recs) {
		return Optional.ofNullable ( 
				Arrays.stream(
					Arrays.stream(recs)
					.map(VcfRecord::getAlt)
					.collect(Collectors.joining(Constants.COMMA_STRING))
				.split(Constants.COMMA_STRING))
				.distinct()
				.sorted()
				.collect(Collectors.joining(Constants.COMMA_STRING)));
	}
	
	public static String getGT(String combinedAlts, String myAlts, String currentGT) {
		if ("0/0".equals(currentGT) || Constants.MISSING_GT.equals(currentGT)) {
			return currentGT;
		}
		if (combinedAlts.equals(myAlts)) {
			return currentGT;
		}
		int index = currentGT.indexOf(Constants.SLASH_STRING);
		int gt1 = Integer.parseInt(currentGT.substring(0, index));
		int gt2 = Integer.parseInt(currentGT.substring(index + 1));
		
		String [] myAltsArray = myAlts.split(Constants.COMMA_STRING);
		String [] combinedAltsArray = combinedAlts.split(Constants.COMMA_STRING);
		
		int newGT1 = getNewPosition(combinedAltsArray, myAltsArray, gt1) ;
		int newGT2 = getNewPosition(combinedAltsArray, myAltsArray, gt2) ;
		
		return newGT1 + Constants.SLASH_STRING + newGT2;
	}
	
	public static int getNewPosition(String[] newAlts, String[] oldAlts, int oldPos) {
		if (oldPos == 0) {
			return oldPos;
		}
		
		String alt = oldAlts[oldPos - 1];
		int newPos = -1;
		
		for (int i = 1 ; i <= newAlts.length ; i++) {
			if (alt.equals(newAlts[i - 1])) {
				newPos = i;
				break;
			}
		}
		return newPos;
	}
	
	/**
	 * Moves any data that we don't like in the filter and info fields into specific fields in the format column.
	 * Modifies the VcfRecord supplied as an argument in place, and so is not referentially transparent
	 */
	public static void moveDataToFormat(VcfRecord r, String combinedAlt, boolean updateAD) {
		/*
		 * for info field, we are just looking for SOMATIC
		 * If this is present, move to the FORMAT-INF field for all samples (assuming that the format
		 */
		List<String> ff = r.getFormatFields();
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(ff);
		int sampleCount = ff.size() - 1;
		if (sampleCount > 0) {
			
			/*
			 * filter field
			 */
			String filter = r.getFilter();
			if ( ! StringUtils.isNullOrEmptyOrMissingData(filter)) {
				String [] infArr = ffMap.computeIfAbsent(VcfHeaderUtils.FORMAT_FILTER, k -> new String[sampleCount]);
				for (int i = 0 ; i < infArr.length ; i++) {
					String s = infArr[i];
					if (StringUtils.isNullOrEmptyOrMissingData(s)) {
						infArr[i] = filter;
					} else {
						infArr[i] += Constants.SEMI_COLON + filter;
					}
				}
			}
		}
		
		/*
		 * if the combinedAlts is different from the alt for this record, we may need to update the GT field and the AD field
		 */
		String aa = r.getAlt();
		if ( ! aa.equals(combinedAlt)) {
			
			String[] existingGTs = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
			String[] existingADs = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
			String[] oabss = ffMap.get(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND);
			/*
			 * we could have an updated gts
			 */
			for (int z = 0 ; z < existingGTs.length ; z++) {
				String gt = existingGTs[z];
				if ( ! gt.equals(Constants.MISSING_DATA_STRING)) {
					String newGT = getGT(combinedAlt, aa, gt);
					if ( ! newGT.equals(gt)) {
						existingGTs[z] = newGT;
					}
				}
				if (updateAD) {
					String ad = existingADs[z];
					if ( ! StringUtils.isNullOrEmptyOrMissingData(ad)) {
						String newAD = null != oabss ? VcfUtils.getAD(r.getRef(), combinedAlt, oabss[z]) : fillAD(ad, existingGTs[z], gt);
						if ( ! newAD.equals(ad)) {
							existingADs[z] = newAD;
						}
					}
				}
			}
		}
		
		/*
		 * update record
		 */
		if ( ! ffMap.isEmpty()) {
			r.setFormatFields(VcfUtils.convertFFMapToList(ffMap));
		}
	}
	
	/**
	 * returns true if the supplied string only contains the missing data symbol '.' and the colon delimiter ':'
	 * eg. .:.:.:.:.:.:.:. would return true, .:.:.:SOMATIC:.:.:. would return false.
	 * 
	 * @param f
	 * @return
	 */
	public static boolean isFormatSampleEmpty(String f) {
		if (StringUtils.isNullOrEmptyOrMissingData(f)) {
			return true;
		}
//		f.chars().forEach(System.out::println);
		return f.chars().allMatch(i -> i == 46 || i == 58);
	}
	
	public static String fillAD(String oldAD, String newGt, String oldGt) {
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
	 * Returns a new VcfRecord that takes the positional information from the suppliued VCfRecord, and uses the supplied alt for the alt...
	 * @param r
	 * @param combinedAlt
	 * @return
	 */
	public static VcfRecord getBaseVcfRecordDetails(VcfRecord r, String combinedAlt) {
		return new VcfRecord.Builder(r.getChrPosition(), r.getRef()).id(r.getId()).allele(combinedAlt).build();
	}
	
	public static VcfRecord mergeRecords(Map<Integer, Map<String, String>> rules, VcfRecord caller1, VcfRecord caller2) {
		VcfRecord mr = null;
		
		
		if (null == caller1 && null == caller2) {
			throw new IllegalArgumentException("MergeUtils.mergeRecords called will no records!!!");
		} else if (null == caller1) {
//			Map<String, String> thisRecordsRules = null != rules ? rules.get(1) : null;
			/*
			 * just got caller 2 data
			 */
			mr = getBaseVcfRecordDetails(caller2, caller2.getAlt());
			mr.setInfo(caller2.getInfo());
			moveDataToFormat(caller2, mr.getAlt(), false);
			
			/*
			 * add format fields from caller2 to mr
			 */
			List<String> ff = caller2.getFormatFields();
			mr.setFormatFields(ff);
			
			/*
			 * add format columns for missing caller 
			 */
			int noOfSamples = ff.size() - 1;
			for (int i = 0 ; i < noOfSamples ; i++) {
				VcfUtils.addMissingDataToFormatFields(mr, 1);
			}
			
		} else if (null == caller2) {
//			Map<String, String> thisRecordsRules = null != rules ? rules.get(0) : null;
			/*
			 * just got caller 1 data
			 */
			mr = getBaseVcfRecordDetails(caller1, caller1.getAlt());
			moveDataToFormat(caller1, mr.getAlt(), true);
			/*
			 * add format fields from caller1 to mr
			 */
			List<String> ff = caller1.getFormatFields();
			mr.setFormatFields(ff);
			
			/*
			 * add format columns for missing caller 
			 */
			int noOfSamples = ff.size() - 1;
			for (int i = 0 ; i < noOfSamples ; i++) {
				VcfUtils.addMissingDataToFormatFields(mr, noOfSamples);
			}
			
			
		} else {
			/*
			 * got data from both callers
			 */
			
			Optional<String> combinedAlts = getCombinedAlt(caller1, caller2);
			mr = getBaseVcfRecordDetails(caller1, combinedAlts.get());
			
			Map<String, String> caller1Rules = null != rules ? rules.get(0) : null;
			if (null != caller1Rules && ! caller1Rules.isEmpty()) {
				
				/*
				 * INFO
				 */
				for (String s : caller1.getInfo().split(Constants.SEMI_COLON_STRING)) {
					int equalsIndex = s.indexOf(Constants.EQ);
					String key = equalsIndex > -1 ? s.substring(0, equalsIndex) : s;
					String replacementKey = caller1Rules.get(key);
					if (null == replacementKey) {
						mr.getInfoRecord().appendField(key, (equalsIndex > -1) ? s.substring(equalsIndex + 1) : s);
					} else {
						mr.getInfoRecord().addField(replacementKey, (equalsIndex > -1) ? s.substring(equalsIndex + 1) : s);
					}
				}
				
			} else {
				mr.appendInfo(caller1.getInfo(), false);
			}
			Map<String, String> caller2Rules = null != rules ? rules.get(1) : null;
			if (null != caller2Rules && ! caller2Rules.isEmpty()) {
				
				/*
				 * INFO
				 */
				for (String s : caller2.getInfo().split(Constants.SEMI_COLON_STRING)) {
					int equalsIndex = s.indexOf(Constants.EQ);
					String key = equalsIndex > -1 ? s.substring(0, equalsIndex) : s;
					String replacementKey = caller2Rules.get(key);
					if (null == replacementKey) {
						mr.getInfoRecord().appendField(key, (equalsIndex > -1) ? s.substring(equalsIndex + 1) : s);
					} else {
						mr.getInfoRecord().addField(replacementKey, (equalsIndex > -1) ? s.substring(equalsIndex + 1) : s);
					}
				}
				
			} else {
				mr.appendInfo(caller2.getInfo(), false);
			}
			
			moveDataToFormat(caller1, mr.getAlt(), true);
			moveDataToFormat(caller2, mr.getAlt(), true);
			
			/*
			 * add format fields from caller1 to mr
			 */
			
			Map<String, String[]> caller1FFMap = caller1.getFormatFieldsAsMap();
			Map<String, String[]> caller2FFMap = caller2.getFormatFieldsAsMap();
			
			if (null != caller1Rules && ! caller1Rules.isEmpty()) {
				/*
				 * update any keys depending on rules
				 */
				for (Entry<String, String> entry : caller1Rules.entrySet()) {
					if (caller1FFMap.containsKey(entry.getKey())) {
						String [] values = caller1FFMap.remove(entry.getKey());
						caller1FFMap.put(entry.getValue(), values);
					}
				}
			}
			if (null != caller2Rules &&  ! caller2Rules.isEmpty()) {
				/*
				 * update any keys depending on rules
				 */
				for (Entry<String, String> entry : caller2Rules.entrySet()) {
					if (caller2FFMap.containsKey(entry.getKey())) {
						String [] values = caller2FFMap.remove(entry.getKey());
						caller2FFMap.put(entry.getValue(), values);
					}
				}
			}
			
			
			List<String> ff1 = VcfUtils.convertFFMapToList(caller1FFMap);
			if (ff1.size() > 1) {
				mr.setFormatFields(ff1);
			}
			List<String> ff2 = VcfUtils.convertFFMapToList(caller2FFMap);
			if (ff2.size() > 1) {
				VcfUtils.addAdditionalSamplesToFormatField(mr, ff2);
			}
			
			/*
			 * remove somatic from info field should it be there
			 */
			mr.getInfoRecord().removeField(SnpUtils.SOMATIC);
			
		}
		return mr;
	}
	
	/**
	 * Merge vcf records that have same position, ref, alt and number of samples into a single record
	 */
	public static VcfRecord mergeRecords(Map<Integer, Map<String, String>> rules, VcfRecord ... records) {
		if (null == records || records.length == 0) {
			throw new IllegalArgumentException("MergeUtils.mergeRecords called will no records!!!");
		}
		
		/*
		 * build up alt string
		 */
		
		Optional<String> combinedAlt = getCombinedAlt(records);
//		assert combinedAlt.isPresent() : "Null returned from getCombinedAlt";
		
		/*
		 * Get common values from 1st record
		 */ 
		VcfRecord mergedRecord =  new VcfRecord.Builder(records[0].getChrPosition(), records[0].getRef())
									.id(records[0].getId()).allele(combinedAlt.get()).build();
				
		
		/*
		 * Update id, info, filter, and format fields
		 */
		
		for (int i = 0 ; i < records.length ; i++) {
			VcfRecord r = records[i];
			Map<String, String> thisRecordsRules = null != rules ? rules.get(i) : null;
			String suffix = "_" + (i + 1);
			
			mergedRecord.appendId(r.getId());
			List<String> rFF =  r.getFormatFields() ;
			
			/*
			 * if the combinedAlts is different from the alt for this record, we may need to update the GT field
			 */
			String aa = r.getAlt();
			if ( ! aa.equals(combinedAlt.get())) {
				
				Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(rFF);
				String[] existingGTs = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
				/*
				 * we could have an updated gts
				 */
				boolean needToUpdate = false;
				for (int z = 0 ; z < existingGTs.length ; z++) {
					String gt = existingGTs[z];
					if ( ! gt.equals(Constants.MISSING_DATA_STRING)) {
						String newGT = getGT(combinedAlt.get(), aa, gt);
						if ( ! newGT.equals(gt)) {
							existingGTs[z] = newGT;
							needToUpdate = true;
						}
					}
				}
				
				
				/*
				 * update map
				 */
				if (needToUpdate) {
					ffMap.put(VcfHeaderUtils.FORMAT_GENOTYPE, existingGTs);
					rFF = VcfUtils.convertFFMapToList(ffMap);
					r.setFormatFields(rFF);
				}
				
			}
			/*
			 * remove SOMATIC from info field, and add to format INF subfield
			 */
			List<String> formatInfo = new ArrayList<>(4);
			formatInfo.add(VcfHeaderUtils.FORMAT_INFO);
			boolean isSomatic = VcfUtils.isRecordSomatic(r);
			
			if (null != rFF) {
				for (int j = 1 ; j < rFF.size(); j++) {
					formatInfo.add(isSomatic && ! rFF.get(j).startsWith(Constants.MISSING_DATA_STRING) ? SnpUtils.SOMATIC : Constants.MISSING_DATA_STRING);
				}
			}
			VcfUtils.addFormatFieldsToVcf(r,formatInfo);
			if (isSomatic) {
				r.getInfoRecord().removeField(SnpUtils.SOMATIC);
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
						mergedRecord.getInfoRecord().appendField(key, (equalsIndex > -1) ? s.substring(equalsIndex + 1) : s);
					} else {
						mergedRecord.getInfoRecord().addField(replacementKey, (equalsIndex > -1) ? s.substring(equalsIndex + 1) : s);
					}
				}
				
			} else {
				mergedRecord.appendInfo(r.getInfo(), false);
			}
			
			/*
			 * FORMAT
			 */
			if (null != rFF &&  ! rFF.isEmpty()) {
				if (null == thisRecordsRules) {
					if (i == 0) {
						VcfUtils.addFormatFieldsToVcf(mergedRecord, r.getFormatFields());
					} else {
						VcfUtils.addAdditionalSamplesToFormatField(mergedRecord, r.getFormatFields());
					}
//					VcfUtils.addFormatFieldsToVcf(mergedRecord, r.getFormatFields(), true);
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
					if (i == 0) {
						VcfUtils.addFormatFieldsToVcf(mergedRecord, rFF);
					} else {
						VcfUtils.addAdditionalSamplesToFormatField(mergedRecord, rFF);
					}
//					VcfUtils.addFormatFieldsToVcf(mergedRecord, rFF, true);
					
				}
			}
			
			
			/*
			 * FILTER
			 */
			if ( ! StringUtils.isNullOrEmptyOrMissingData(r.getFilter())) {
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
		return Arrays.stream(headers)
			.map(header -> header.getSampleId())
			.allMatch(array -> {
				return Arrays.deepEquals(array, firstIds);
			});
	}

}
