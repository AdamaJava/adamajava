/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.string.StringUtils;
import org.qcmg.maf.MAFRecord;

public class MafStatsUtils {
	
	public static final String SEPARATOR = " : ";
	public static final String VARIANT_CLASSIFICATION = "Variant_classification";
	public static final String SILENT_RATIO = "Silent/non-silent ratio";
	public static final String BASE_CHANGE = "Base change";
	public static final String TITV_RATIO = "Ti/Tv ratio";
	public static final String RS_RATIO = "dbSNP annotation percentage";
	
	public static final NumberFormat nf = new DecimalFormat("0.##");
	
	public static List<String> getVariantClassifications(List<MAFRecord> mafs) {
		List<String> variantClassifications = Collections.emptyList();
		
		if (null != mafs && ! mafs.isEmpty()) {
			Map<String, AtomicInteger> classificationCounts = new HashMap<>();
			variantClassifications = new ArrayList<>();
			
			for (MAFRecord maf : mafs) {
				String classification = maf.getVariantClassification();
				
				if (classificationCounts.containsKey(classification)) {
					classificationCounts.get(classification).incrementAndGet();
				} else {
					classificationCounts.put(classification, new AtomicInteger(1));
				}
			}
			
			// convert to list of strings
			int silent = 0, nonSilent = 0;
			for (Entry<String, AtomicInteger> entry : classificationCounts.entrySet()) {
				if (null == entry.getKey()) continue;
				
				switch (entry.getKey()) {
				case "Frame_Shift_Del" :
				case "Frame_Shift_Ins" :
				case "In_Frame_Del" :
				case "In_Frame_Ins" :
				case "Missense_Mutation" :
				case "Nonsense_Mutation" :
				case "De_novo_Start_InFrame" :
				case "Nonstop_Mutation" :
				case "De_novo_Start_OutOfFrame" : nonSilent += entry.getValue().intValue() ; break;
				case "Silent" : silent += entry.getValue().intValue(); break;
				}
				variantClassifications.add(VARIANT_CLASSIFICATION + SEPARATOR + entry.getKey() + SEPARATOR + entry.getValue().intValue());
			}
			variantClassifications.add(SILENT_RATIO + SEPARATOR+ "1:" + nf.format(((double)nonSilent / silent)));
			
		}
		
		return variantClassifications;
	}
	
	public static List<String> getMutationsAndTiTv(List<MAFRecord> mafs) {
		List<String> variantClassifications = Collections.emptyList();
		
		if (null != mafs && ! mafs.isEmpty()) {
			int ac = 0, ag = 0, at = 0;
			int ca = 0, cg = 0, ct = 0;
			int ga = 0, gc = 0, gt = 0;
			int ta = 0, tc = 0, tg = 0;
			
			for (MAFRecord maf : mafs) {
				String refString = maf.getRef();
				if (null == refString) {
					continue;
				}
				String altString = MafUtils.getVariant(maf);
				if (null == altString) {
					continue;
				}
				
				char ref = refString.charAt(0);
				char alt = altString.charAt(0);
				
				switch (ref) {
				case 'A' : 
					switch (alt) {
					case 'C' : ac++ ; break;
					case 'G' : ag++ ; break;
					case 'T' : at++ ; break;
					}
				case 'C' : 
					switch (alt) {
					case 'A' : ca++ ; break;
					case 'G' : cg++ ; break;
					case 'T' : ct++ ; break;
					}
				case 'G' : 
					switch (alt) {
					case 'A' : ga++ ; break;
					case 'C' : gc++ ; break;
					case 'T' : gt++ ; break;
					}
				case 'T' : 
					switch (alt) {
					case 'A' : ta++ ; break;
					case 'C' : tc++ ; break;
					case 'G' : tg++ ; break;
					}
				}
			}
			variantClassifications = new ArrayList<>();
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "A->C" + SEPARATOR + ac);
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "A->G" + SEPARATOR + ag);
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "A->T" + SEPARATOR + at);
			
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "C->A" + SEPARATOR + ca);
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "C->G" + SEPARATOR + cg);
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "C->T" + SEPARATOR + ct);
			
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "G->A" + SEPARATOR + ga);
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "G->C" + SEPARATOR + gc);
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "G->T" + SEPARATOR + gt);
			
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "T->A" + SEPARATOR + ta);
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "T->C" + SEPARATOR + tc);
			variantClassifications.add(BASE_CHANGE + SEPARATOR + "T->G" + SEPARATOR + tg);
			
			int ti = ac + at + ca + cg + gc + gt + ta + tg;
			int tv = ag + ga + tc + ct;
					
			variantClassifications.add(TITV_RATIO + SEPARATOR + nf.format(((double) ti / tv)) + ":1");
		}
		
		return variantClassifications;
	}
	
	public static double getRsRatioDouble(List<MAFRecord> mafs) {
		double result = 0.0;
		if (null != mafs && ! mafs.isEmpty()) {
			int totalCount = mafs.size();
			int rsIdCount = 0;
			
			for (MAFRecord maf : mafs) {
				if ( ! StringUtils.isNullOrEmpty(maf.getDbSnpId()) && maf.getDbSnpId().startsWith("rs")) {
					rsIdCount++;
				}
			}
			result = 100 * ((double)rsIdCount / totalCount);
		}
		return result;
	}
	
	public static String getRsRatio(List<MAFRecord> mafs) {
		String ratio = null;
		
		if (null != mafs && ! mafs.isEmpty()) {
			double rsRatio = getRsRatioDouble(mafs);
			ratio = RS_RATIO + SEPARATOR + nf.format(rsRatio);
		}
		
		return ratio;
	}

}
