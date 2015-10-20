/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
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
	
	public static int readLengthMatchCounter = 0;
	public static int posiitonInDeletionCounter = 0;
	
	private static final QLogger logger = QLoggerFactory.getLogger(BAMPileupUtil.class);
	
	
//	public static void examinePileup(List<SAMRecord> sams, VCFRecord record) {
////		int normalCoverage = 0;
//		String pileup = "";
//		String qualities = "";
//		for (SAMRecord sam : sams ) {
//			
//			if ( eligibleSamRecord(sam)) {
////				++normalCoverage;
//				
//				int offset = getReadPosition(sam, record.getPosition());
//				
//				if (offset < 0)  {
//					logger.info("invalid offset position - position falls within deletion?? position: "+ record.getPosition() + ", alignment start: " + sam.getAlignmentStart() + ", alignment end: " + sam.getAlignmentEnd() + ", read length: " + sam.getReadLength() + " cigar: "+ sam.getCigarString());
//					continue;
//				}
//				
//				if (offset >= sam.getReadLength()) {
////					throw new Exception("offset [position: " + record.getPosition() + ", read start pos(unclipped): " + sam.getUnclippedStart() + ", read end pos(unclipped): " + sam.getUnclippedEnd()+ "] is larger than read length!!!: " + sam.format());
//					// set to last entry in sequence
////					logger.info("adjusting offset to read length -1");
////					String read = sam.getReadString();
////					int refPosition = sam.getReferencePositionAtReadPosition(offset);
//					logger.info("offset: " + offset + ", position: " + record.getPosition() + ", alignment start: " + sam.getAlignmentStart() + ", unclipped alignment start: " + sam.getUnclippedStart() + ", alignment end: " + sam.getAlignmentEnd());
//					logger.info( sam.format());
////					offset = sam.getReadLength() -1;
////					logger.info("char at adjusted offset: " + read.charAt(offset));
////					logger.info("md tag: " + sam.getStringAttribute("MD"));
//					continue;
//				}
//				
//				char c = sam.getReadString().charAt(offset);
//				pileup += sam.getReadNegativeStrandFlag() ? Character.toLowerCase(c) : c;
//				qualities += sam.getBaseQualityString().charAt(offset);
//			}
//		}
//		
//		
//		if (pileup.length() > 0)
//			record.setPileup(PileupUtil.getPileupCounts(pileup, qualities));
//		
//	}
	
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
			&& (null == sm ? false : sm.intValue() > SM_CUTOFF)
//			&& tallyMDMismatches(record.getStringAttribute("MD")) < MD_CUTOFF	// 
			&& ((record.getReadPairedFlag() && record.getSecondOfPairFlag() && record.getProperPairFlag()) 
					|| tallyCigarMatchMismatches(record.getCigar()) > CIGAR_CUTOFF);
		
	}
	
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
