package au.edu.qimr.vcftools.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeader.FormattedRecord;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.vcftools.Rule;

public class MergeUtils {
	
	private static final String GENOTYPE = "##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">";
	private static final QLogger logger = QLoggerFactory.getLogger(MergeUtils.class);

	public static List<String> mergeOtherHeaderRecords(List<Record> ...  loloRecs) {
		if (null == loloRecs || loloRecs.length == 0) {
			return Collections.emptyList();
		}
		
		AtomicInteger prefix = new AtomicInteger(1);
		List<String> mergedRecs = new ArrayList<>();
		Arrays.stream(loloRecs)
			.filter(list -> list != null && ! list.isEmpty())
			.forEach(list -> {
				mergedRecs.addAll(list.stream()
						.filter(r -> r != null && r.getData() != null && ! r.getData().equals(VcfHeaderUtils.BLANK_HEADER_LINE))
						.map(r -> r.getData().replaceAll("##", "##" + prefix.get() + ":"))
						.collect(Collectors.toList()));
				prefix.incrementAndGet();
			});
			
		return mergedRecs;
	}
	
	public static Map<Integer, Map<String, String>> getRules(Map<String, FormattedRecord> ...  loMaRecs) {
		return getHeaderAndRules(Arrays.asList(loMaRecs)).getRight();
	}
	
	public static Pair<VcfHeader, Rule> getMergedHeaderAndRules(List<String> vcfFileNames, VcfHeader ... headers) {
		
		if (canMergeBePerformed(headers)) {
			VcfHeader mergedHeader = new VcfHeader();
			List<Map<String, FormattedRecord>> infoHeaders = new ArrayList<>(headers.length);
			List<Map<String, FormattedRecord>> filterHeaders = new ArrayList<>(headers.length);
			List<Map<String, FormattedRecord>> formatHeaders = new ArrayList<>(headers.length);
			List<List<Record>> otherHeaders = new ArrayList<>(headers.length);
			for (VcfHeader h : headers) {
				infoHeaders.add(h.getInfoRecords());
				filterHeaders.add(h.getFilterRecords());
				formatHeaders.add(h.getFormatRecords());
				otherHeaders.add(h.getNonStandardRecords());
			}
			
			Pair<List<FormattedRecord>, Map<Integer, Map<String, String>>> infoPair = getHeaderAndRules(infoHeaders);
			for (FormattedRecord fr : infoPair.getLeft()) {
				mergedHeader.addInfo(fr);
			}
			Pair<List<FormattedRecord>, Map<Integer, Map<String, String>>> filterPair = getHeaderAndRules( filterHeaders);
			for (FormattedRecord fr : filterPair.getLeft()) {
				mergedHeader.addFilter(fr);
			}
			Pair<List<FormattedRecord>, Map<Integer, Map<String, String>>> formatPair = getHeaderAndRules(formatHeaders);
			for (FormattedRecord fr : formatPair.getLeft()) {
				mergedHeader.addFormat(fr);
			}
			List<String> mergedOtherRecords = mergeOtherHeaderRecords(otherHeaders.toArray(new List[]{}));
			for (String s : mergedOtherRecords) {
				mergedHeader.parseHeaderLine(s);
			}
			
			/*
			 * add IN= to 
			 */
			logger.info("checking that no existing info lines start with IN= ");
			if (infoPair.getLeft().contains("IN")) {
				logger.warn("Can't use IN= to mark records as having come from a particular input file - IN= is already in use!");
			}
			
			mergedHeader.addInfoLine("IN", ".", "Integer", "Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file");
			mergedHeader.parseHeaderLine(headers[0].getChrom().toString());
			
			Rule r = new Rule(headers.length);
			for (int i = 0 ; i < headers.length ; i++) {
				r.setRules(i, filterPair.getRight().get(i), infoPair.getRight().get(i), formatPair.getRight().get(i));
			}
			
			return Pair.of(mergedHeader,  r);
			
		} else {
			logger.warn("Unable to perform merge - please check that the vcf headers contain the same samples in the same order");
		}
		return null;
		
		
	}
	
	public static List<FormattedRecord> mergeHeaderRecords(Map<String, FormattedRecord> ...  loMaRecs) {
		return getHeaderAndRules(Arrays.asList(loMaRecs)).getLeft();
	}
	
	public static Pair<List<FormattedRecord>, Map<Integer, Map<String, String>>> getHeaderAndRules(List<Map<String, FormattedRecord> >  loMaRecs) {
		if (null == loMaRecs || loMaRecs.isEmpty()) {
			return Pair.of(Collections.emptyList(), Collections.emptyMap());
		}
		
		Map<Integer,Map<String, String>> replacementIds = new HashMap<>(4);
		
		Map<String, FormattedRecord> mergedRecsMap = loMaRecs.get(0);
		for (int i = 1 ; i < loMaRecs.size() ; i++) {
			Map<String, FormattedRecord> map = loMaRecs.get(i);
			for (Entry<String, FormattedRecord> entry : map.entrySet()) {
				FormattedRecord mergedRec = mergedRecsMap.get(entry.getKey());
				if (null == mergedRec) {
					// add
					mergedRecsMap.put(entry.getKey(), entry.getValue());
				} else {
					if (mergedRec.equals(entry.getValue())) {
						logger.info("Found identical header entry: " + mergedRec.toString());
					} else if (mergedRec.getData().substring(mergedRec.getData().indexOf("Description="))
							.equals(entry.getValue().getData().substring(entry.getValue().getData().indexOf("Description=")))) {
						/*
						 * Just match on description - ignore type and number for now (ever??)...
						 */
						logger.info("Found identical header entry (apart from type, and number): " + mergedRec.toString());
					} else if (mergedRec.getData().equals(GENOTYPE) || entry.getValue().getData().equals(GENOTYPE)) {
						/*
						 * Just match on description - ignore type and number for now (ever??)...
						 */
						logger.info("Found identical header entry (apart from type, and number): " + mergedRec.toString());
					} else {
						String newId = entry.getKey() + i;
						String existingRec = entry.getValue().getData();
						
						FormattedRecord updatedRec = existingRec.startsWith(VcfHeaderUtils.HEADER_LINE_INFO) ? new VcfHeader.InfoRecord(entry.getValue().getData().replace(entry.getKey(), newId))
						: existingRec.startsWith(VcfHeaderUtils.HEADER_LINE_FILTER) ? new VcfHeader.FilterRecord(entry.getValue().getData().replace(entry.getKey(), newId))
						: existingRec.startsWith(VcfHeaderUtils.HEADER_LINE_FORMAT) ? new VcfHeader.FormatRecord(entry.getValue().getData().replace(entry.getKey(), newId))
						: null;
						
						logger.info("bumping id from " + entry.getKey() + " to " + newId + " and adding to map. orig: " + mergedRec + ", updated: " + updatedRec);
						mergedRecsMap.put(newId, updatedRec);
						Map<String, String> replacement = replacementIds.get(i);
						if (null == replacement) {
							replacement = new HashMap<>();
							replacementIds.put(i, replacement);
						}
						replacement.put(entry.getKey(), newId);
					}
				}
			}
		}
		
		List<FormattedRecord> mergedRecs = new ArrayList<>(mergedRecsMap.values());
		return  Pair.of(mergedRecs, replacementIds);
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
		VcfRecord mergedRecord =  VcfUtils.createVcfRecord(records[0].getChrPosition(),   records[0].getId(), records[0].getRef(), records[0].getAlt());

		
		/*
		 * Update id, info, filter, and format fields
		 */
		
		for (int i = 0 ; i < records.length ; i++) {
			VcfRecord r = records[i];
			Map<String, String> thisRecordsRules = null != rules ? rules.get(i) : null;
			
			mergedRecord.appendId(r.getId());
			
			if (null != thisRecordsRules && ! thisRecordsRules.isEmpty()) {
				
				/*
				 * INFO
				 */
				for (String s : r.getInfo().split(";")) {
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
				String suffix = "_" + (i+1);
				for (String s : r.getFilter().split(";")) {
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
