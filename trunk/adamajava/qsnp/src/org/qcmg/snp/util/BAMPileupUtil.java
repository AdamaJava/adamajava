/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.util;

import java.util.List;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.QSnpGATKRecord;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.snp.util.QJumperWorker.Mode;

public class BAMPileupUtil {
	
	public static int SM_CUTOFF = 14;
	public static int MD_CUTOFF = 3;
	public static int CIGAR_CUTOFF = 34;
	
	private static final QLogger logger = QLoggerFactory.getLogger(BAMPileupUtil.class);
	
	public static void examinePileupVCF(List<SAMRecord> sams, QSnpGATKRecord record) {
		String pileup = "";
		String qualities = "";
//		boolean debug = null != record && "chr1".equals(record.getChromosome()) && record.getPosition() == 4480470;
		
//		if (debug) logger.info("in examinePileupVCF with no of sam records: " + sams.size());
		for (SAMRecord sam : sams ) {
			
			if ( eligibleSamRecordNonDupOnly(sam)) {
//				if ( eligibleSamRecord(sam)) {
				int offset = getReadPosition(sam, record.getPosition());
				
				if (offset < 0)  {
					logger.info("invalid offset position - position falls within deletion?? position: "+ record.getPosition() + ", alignment start: " + sam.getAlignmentStart() + ", alignment end: " + sam.getAlignmentEnd() + ", read length: " + sam.getReadLength() + " cigar: "+ sam.getCigarString());
					continue;
				} else if (offset >= sam.getReadLength()) {
					logger.info("offset: " + offset + ", position: " + record.getPosition() + ", alignment start: " + sam.getAlignmentStart() + ", unclipped alignment start: " + sam.getUnclippedStart() + ", alignment end: " + sam.getAlignmentEnd());
					logger.info( sam.format());
					continue;
				}
				
				char c = sam.getReadString().charAt(offset);
				pileup += sam.getReadNegativeStrandFlag() ? Character.toLowerCase(c) : c;
				qualities += sam.getBaseQualityString().charAt(offset);
			}
		}
		
//		if (debug) logger.info("in examinePileupVCF with pileup: " + pileup);
		if (pileup.length() > 0) {
			record.setPileup(PileupElementUtil.getPileupCounts(pileup, qualities));
		}
	}
	
	public static void examinePileupSNP(final List<SAMRecord> sams, final QSnpRecord record, final Mode mode) {
		String pileup = "", qualities = "";
		
		final char mutation = Mode.QSNP_MUTATION_IN_NORMAL == mode ? 
				record.getMutation().charAt(record.getMutation().length()-1) : '\u0000';
		
		for (final SAMRecord sam : sams ) {
			
			if ( eligibleSamRecordNonDupOnly(sam)) {
				int offset = getReadPosition(sam, record.getPosition());
				
				if (offset < 0 || offset >= sam.getReadLength())  {
					if (offset >= sam.getReadLength()) {
						logger.info("offset: " + offset + ", position: " + record.getPosition() + ", alignment start: " + sam.getAlignmentStart() + ", unclipped alignment start: " + sam.getUnclippedStart() + ", alignment end: " + sam.getAlignmentEnd());
						logger.info( sam.format());
					}
					continue;
				}
				
				final char c = sam.getReadString().charAt(offset);
				if (Mode.QSNP_MUTATION_IN_NORMAL == mode) {
					// both chars should always be upper case at this point
					assert ! Character.isLowerCase(c);
					if (c == mutation) {
						VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.MUTATION_IN_UNFILTERED_NORMAL);
						return;
					}
				}
				
				pileup += sam.getReadNegativeStrandFlag() ? Character.toLowerCase(c) : c;
				qualities += sam.getBaseQualityString().charAt(offset);
			}
		}
		
		if (pileup.length() > 0) {
			if (Mode.QSNP == mode) {
				if (null == record.getNormalGenotype()) {
					record.setNormalPileup(pileup);
					record.setNormalCount(pileup.length());
					record.setNormalNucleotides(PileupElementUtil.getPileupElementString(PileupElementUtil.getPileupCounts(pileup, qualities), record.getRef().charAt(0)));
				} else if (null == record.getTumourGenotype()) {
					record.setTumourCount(pileup.length());
					record.setTumourNucleotides(PileupElementUtil.getPileupElementString(PileupElementUtil.getPileupCounts(pileup, qualities), record.getRef().charAt(0)));
				}
			} 
//			else if (Mode.QSNP_MUTATION_IN_NORMAL == mode) {
//				// just want to add an annotation if the pileup contatins the mutation
//				char mutationChar = record.getMutation().charAt(record.getMutation().length()-1);
//				if (GenotypeComparisonUtil.isCharPresentInString(pileup, mutationChar))
//					record.addAnnotation("mutation also found in pileup of (unfiltered) normal");
//			}
		}
	}
	
	/**
	 * Returns the position within the read string that the position int value corresponds to
	 * <p>Alas this is not as simple as position-readStartPosition as deletions, insertions, etc all have an effect on this value
	 * 
	 * @param sam SAMRecord for which the offset is being sought
	 * @param position int describing the reference position that is of interest
	 * @return int indicating the position within this records sequence that refers to the supplied position. -1 if this falls within a deletion region
	 */
	public static int getReadPosition(final SAMRecord sam, final int position) {
		return SAMUtils.getIndexInReadFromPosition(sam, position);
//		int offset = -1;
//		final int readStart = sam.getAlignmentStart();
//		final int readEnd = sam.getAlignmentEnd();
//		final int readLength = sam.getReadLength();
//		
//		if (position == readStart) offset = 0;
//		if (position == readEnd) offset = readLength-1;
//		
//		if (-1 == offset) {
//			if (CoordMath.getLength(readStart, readEnd) == readLength) {
//				//woohoo - no deletions
//				offset = position - readStart;
//				readLengthMatchCounter++;
//			} else {
////				// we have a deletion - use AlignmentBlocks to determine the position within the string
//				
//				int blockLengthTally = 0;
//				for (AlignmentBlock block : sam.getAlignmentBlocks()) {
//					if (block.getReferenceStart() <= position && (CoordMath.getEnd(block.getReferenceStart(),block.getLength()) >= position)) {
//						offset = blockLengthTally + ( position -  block.getReferenceStart() );
//						break;
//					} else blockLengthTally += block.getLength();
//				}
//				
//				if (-1 == offset) {
//					posiitonInDeletionCounter++;
//				}
//			}
//		}	
//		return offset;
	}
	
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
	public static boolean eligibleSamRecord(final SAMRecord record) {
		if (null == record) return false;
		Integer sm = record.getIntegerAttribute("SM");
		return ! record.getDuplicateReadFlag() 
			&& (null == sm ? false : sm.intValue() > SM_CUTOFF)
//			&& tallyMDMismatches(record.getStringAttribute("MD")) < MD_CUTOFF	// 
			&& ((record.getReadPairedFlag() && record.getSecondOfPairFlag() && record.getProperPairFlag()) 
					|| tallyCigarMatchMismatches(record.getCigar()) > CIGAR_CUTOFF);
		
	}
	
	public static boolean eligibleSamRecordNonDupOnly(SAMRecord record) {
		return null != record && ! record.getDuplicateReadFlag();
	}
	
	public static int tallyCigarMatchMismatches(Cigar cigar) {
		int tally = 0;
		if (null != cigar) {
			for (final CigarElement element : cigar.getCigarElements()) {
				if (CigarOperator.M == element.getOperator()) {
					tally += element.getLength();
				}
			}
		}
		return tally;
	}
	
//	public static int tallyMDMismatches(final String mdData) {
//		int count = 0;
//		if (null != mdData) {
//			for (int i = 0, size = mdData.length() ; i < size ; ) {
//					
//				if (isValidMismatch(mdData.charAt(i))) {
//					count++;
//					i++;
//				} else if ('^' == mdData.charAt(i)) {
//					while (++i < size && Character.isLetter(mdData.charAt(i))) {}
//				} else i++;	// need to increment this or could end up with infinite loop...
//			}
//		}
//		return count;
//	}
//	
//	private static boolean isValidMismatch(final char c) {
//		return c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N';
//	}

}
