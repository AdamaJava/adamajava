/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public class BAMPileupUtil {
	
	public static int SM_CUTOFF = 14;
	public static int MD_CUTOFF = 3;
	public static int CIGAR_CUTOFF = 34;

	private static final QLogger logger = QLoggerFactory.getLogger(BAMPileupUtil.class);
	
	/**
	 * Determines whether a sam record is eligible by applying some filtering criteria.
	 * Currently filters on the SM tag value, some of the flags, and the Cigar string
	 * 
	 * <p><b>NOTE</b> that we should also be filtering on MD tag, but GATK removes this 
	 * tag when it does its local realignment, so there is no need to include this check for the time being
	 * 
	 * @param record SAMRecord that is being put through the filter check
	 * @return boolean indicating if the record has passed the filter
	 */
	public static boolean eligibleSamRecord(SAMRecord record) {
		if (null == record) return false;
		Integer sm = record.getIntegerAttribute("SM");
		return ! record.getDuplicateReadFlag() 
			&& (null != sm && sm > SM_CUTOFF)
//			&& tallyMDMismatches(record.getStringAttribute("MD")) < MD_CUTOFF	// 
			&& ((record.getReadPairedFlag() && record.getSecondOfPairFlag() && record.getProperPairFlag()) 
					|| tallyCigarMatchMismatches(record.getCigar()) > CIGAR_CUTOFF);
		
	}
	
	/**
	 * Calculates the tally of match operations in a cigar string.
	 *
	 * @param cigar The Cigar object representing the cigar string.
	 * @return The tally of match operations in the cigar string.
	 */
	public static int tallyCigarMatchMismatches(Cigar cigar) {

		int tally = 0;
		if (null != cigar) {
			for (CigarElement element : cigar.getCigarElements()) {
				if (CigarOperator.M == element.getOperator()) {
					tally += element.getLength();
				}
			}
		}
		return tally;
	}
	
	public static int tallyMDMismatches(String mdData) {
		int count = 0;
		if (null != mdData) {
			for (int i = 0, size = mdData.length() ; i < size ; ) {
					
				if (isValidMismatch(mdData.charAt(i))) {
					count++;
					i++;
				} else if ('^' == mdData.charAt(i)) {
					while (++i < size && Character.isLetter(mdData.charAt(i))) {}
				} else i++;	// need to increment this or could end up with infinite loop...
			}
		}
		return count;
	}
	
	private static boolean isValidMismatch(char c) {
		return c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N';
	}

}
