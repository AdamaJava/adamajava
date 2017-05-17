/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTagUtil;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;

public class MultipleAdjacentSnpsFilter implements SamRecordFilter {
	private final static SAMTagUtil STU = SAMTagUtil.getSingleton();
	private static final short MD_TAG = STU.MD;
	
	public static final String TRIPLE_SNP_STRING = "[ACGT]0[ACGT]0[ACGT]";
	public static final Pattern TRIPLE_SNP_PATTERN_MATCH_ALL = Pattern.compile(".*" +  TRIPLE_SNP_STRING + ".*");
	public static final Pattern TRIPLE_SNP_PATTERN = Pattern.compile(TRIPLE_SNP_STRING);
	private final int position;
	
	public MultipleAdjacentSnpsFilter(int position) {
		this.position = position;
	}

	@Override
	public boolean filterOut(SAMRecord sam) {
		
		String mdTag = (String) sam.getAttribute(MD_TAG);
		
		if (StringUtils.isNullOrEmpty(mdTag)) {
			return false;
		}
		
		// check to see if pattern appears in the string
		Matcher m = TRIPLE_SNP_PATTERN_MATCH_ALL.matcher(mdTag);
		if (m.matches()) {
			return tallyMDMismatches(mdTag, position - sam.getAlignmentStart() + 1);
		} else {
			return false;
		}
	}
	
	public static boolean tallyMDMismatches(String mdData, int readPosition) {
		
		Matcher m = TRIPLE_SNP_PATTERN.matcher(mdData);
		int baseCount = 0;
		int lastPosition = 0;
		while (m.find(lastPosition)) {
			
			int start = m.start();
			if (start == 0) {
				// snps are at beginning of string
				if (readPosition <=3) return true;
			}
			String nonSnp = mdData.substring(lastPosition, start);
			
			// check that snp is valid by checking the last char of nonSnp - should not be a letter....
			// if it is - continue
			if (Character.isLetter(nonSnp.charAt(nonSnp.length()-1))) {
				// check to see if there are any further matches
				if (m.find(start+1)) {
					start = m.start();
					if (start == 0) {
						// snps are at beginning of string
						if (readPosition <=3) return true;
					}
					nonSnp = mdData.substring(lastPosition, start);
				} else {
					break;
				}
			}
			
			boolean deletion = false;
			for (int i = 0, len = nonSnp.length() ; i < len ; ) {
				if (Character.isDigit(nonSnp.charAt(i))) {
					int numberLength = 1;
					while (++i < len && Character.isDigit(nonSnp.charAt(i))) {
						numberLength++;
					}
					baseCount += Integer.parseInt(nonSnp.substring(i-numberLength, i));
					
				} else if ('^' == nonSnp.charAt(i)) {
					deletion = true;
					i++;
					
				} else if (BaseUtils.isACGTNDot(nonSnp.charAt(i))) {
					if (! deletion) {
						i++;
						baseCount++;
					} else {
						baseCount++;
						while (++i < len && BaseUtils.isACGTNDotMR(nonSnp.charAt(i))) {
							baseCount++;
						}
					}
					deletion = false;
				} else i++;	// need to increment this or could end up with infinite loop...
			}
			
			if (baseCount >= readPosition) {
				return false;
			} else if ((baseCount <= readPosition) && (baseCount + 3 >= readPosition)) {
				return ! deletion;
			}
			
			lastPosition = m.end();
			baseCount += 3;
		}
		
		return false;
	}

	@Override
	public boolean filterOut(SAMRecord sam1, SAMRecord sam2) {
		return filterOut(sam1) && filterOut(sam2);
	}

}
