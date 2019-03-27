/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.model.Accumulator;
import org.qcmg.common.util.BaseUtils;

public class SAMUtils {
	
	static final Pattern pa = Pattern.compile("\\d[ATGCN]");
	
	/**
	 * Returns the position within the read string that the position int value corresponds to
	 * <p>Alas this is not as simple as position-readStartPosition as deletions, insertions, etc all have an effect on this value
	 * 
	 * @param sam SAMRecord for which the offset is being sought
	 * @param position int describing the reference position that is of interest
	 * @return int indicating the position within this records sequence that refers to the supplied position. -1 if this falls within a deletion region
	 */
	public static int getIndexInReadFromPosition(final SAMRecord sam, final int position) {
		int offset = -1;
		final int readStart = sam.getAlignmentStart();
		final int readEnd = sam.getAlignmentEnd();
		
//		// positon is before start of read, or greater than end of read -> return -1
		if (position > readEnd || position < readStart) return -1;
			
		Cigar cigar = sam.getCigar();
		if (null == cigar || cigar.isEmpty()) return -1;
		
		int referenceOffset = readStart-1;
		
		for (CigarElement ce : cigar.getCigarElements()) {
			CigarOperator co = ce.getOperator();
			int length = ce.getLength();
			

			if (co.consumesReferenceBases() && co.consumesReadBases()) {
				
				if ((referenceOffset ) == position) {
					break;
				} else if ((referenceOffset  + length) == position) {
					offset += length;
					break;
				} else if ((referenceOffset  + length) > position) {
					offset += (position - referenceOffset);
					break;
				} else {
					// move both offset and referenceOffset forward by length
					referenceOffset += length;
					offset += length;
				}
				
			} else if (co.consumesReferenceBases()) {
				// DELETION
				// position could fall within a deletion - return -1 in this instance
				if ((referenceOffset + length) >= position) {
					offset = -1;
					break;
				} else {
					referenceOffset += length;
				}
			} else if (co.consumesReadBases()){
				// INSERTION, SOFT CLIPPING
				offset += length;
			}
		}
		
		if (offset != -1) {
			// check value of offset using getReferencePositionAtReadPosition
			int referencePosition = sam.getReferencePositionAtReadPosition(offset+1);
			
			if (position == referencePosition) {
				// 0-based for array positions
				return offset;
			} else {
				System.out.println("offsets dont match");
				return -999;
			}
		} else {
			return -1;
		}
	}
	
	
	/**
	 * 
	 * Returns a count of the number of unique start positions from the list of 
	 * sam records that have the supplied base (ACGT) and the supplied position
	 * <p>
	 * Strand is taken into account so that a read is unique if it has a unique alignment start position amongst fellow reads on the same strand 
	 * <p>
	 * Assume the read is filtered - do no further filtering here<br>
	 * Don't take base quality, or mapping quality into account here
	 * 
	 * @param sams List of SAMRecords
	 * @param position int relating to the locus of interest 
	 * (it is assumed that the list of records are in the same chromosome and overlap this position)
	 * @param base char of the base that we are intersted in
	 * @return int corresponding to the count of novel starts
	 */
	public static int getNovelStartsForBase(final List<SAMRecord> sams, final int position, final char base) {
		Set<Integer> forwardStrand = new HashSet<Integer>();
		Set<Integer> reverseStrand = new HashSet<Integer>();
		int count = 0;
		
		for (SAMRecord sam : sams) {
			final int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, position);
			if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
				final char c = sam.getReadString().charAt(indexInRead);
				
				// only care about variant novel starts
				if (base == c) {
			
					if (sam.getReadNegativeStrandFlag()) {
						if (reverseStrand.add(sam.getAlignmentStart())) {
							count ++;
						}
					} else {
						if (forwardStrand.add(sam.getAlignmentStart())) {
							count ++;
						}
					}
				}
			}
		}
	
		return count;
	}
	
	
//	public static Accumulator getAccumulatorFromReads(List<SAMRecord> sams, int position) {
//		if (null == sams || sams.isEmpty()) throw new IllegalArgumentException("null or empty list of sam records passed to getAccumulatorFromReads");
//		
//		Accumulator acc = new Accumulator(position);
//		
//		int counter = 0;
//		for (SAMRecord sam : sams) {
//			counter++;
//			int positionInReadString = getIndexInReadFromPosition(sam, position);
//			if (positionInReadString > -1) {
//				byte base = sam.getReadBases()[positionInReadString];
//				byte qual = sam.getBaseQualities()[positionInReadString];
//				
//				// need to take strand into account when setting start position
//				boolean forwardStrand = ! sam.getReadNegativeStrandFlag();
//				int startPosition = sam.getAlignmentStart();
//				int endPosition = sam.getAlignmentEnd();
//				
//				acc.addBase(base, qual, forwardStrand, startPosition, position, endPosition, counter, sam.getReadName());
//			}
//		}
//		
//		return acc;
//	}
	
	/**
	 * Returns the sam records in the supplied list that have the specified base at the position of interest
	 * 
	 * @param records List<SAMRecord> initial list of sam records
	 * @param base char representing the base of interest
	 * @param position int representing the position of interest
	 * @return List<SAMRecord> list of sam records that have the base at the position
	 */
	public static List<SAMRecord> getRecordsWithBaseAtPosition(List<SAMRecord> records, char base, int position) {
		List<SAMRecord> results = new ArrayList<SAMRecord>();
		
		for (SAMRecord sam : records) {
			
			// get position in read
			int readPosition = getIndexInReadFromPosition(sam, position);
			if (readPosition > -1) {
				byte baseAtReadPosition = sam.getReadBases()[readPosition];
				
				if (base == baseAtReadPosition)
					results.add(sam);
			}
		}
		
		if (results.isEmpty()) return Collections.emptyList();
		return results;
	}
	
	public int getNumberOfMismatchesInMDString(String md) {
		
		// if a letter is prefixed with '^' it is a deletion rather than a mismatch
		int misMatchCount = 0;
		boolean deletion = false;
		for (int i = 0 , size = md.length() ; i < size ;) {
			
			char c = md.charAt(i);
			if ('^' == c) {
				deletion = true;
				i++;
			} else if (BaseUtils.isACGTN(c)) {
				if ( ! deletion)
					misMatchCount++;
				else {
					while (++i < size && BaseUtils.isACGTNDotMR(md.charAt(i))) {
					}
				}
				deletion = false;
			} else i++;
			
		}
		
		return misMatchCount;
	}
	
	 public static int CountMismatch(String MD){
	        
	        Matcher m = pa.matcher(MD);
	        int mis = 0;
	        while(m.find()){mis ++;}

	        return mis;
	    }
	 
	 /**
	  * Remove the trailing newline char at the end of the supplied getSAMString
	  * @param rec
	  * @return
	  */
	 public static String getSAMRecordAsSting(SAMRecord rec) {
		 if (null == rec) throw new IllegalArgumentException("Null SAMRecord passed to getSAMRecordAsSting");
		 
		 return rec.getSAMString().replace("\n", "");
	 }
	 
	 public static boolean isSAMRecordValid(SAMRecord rec) {
		 return 
				 null != rec
				 && ! rec.getReadFailsVendorQualityCheckFlag();
	 }
	 
	 /**
	  * Valid record for variant calling is one which is valid, not a dup, has mapped, and is a primary alignment
	  * @param rec
	  * @return
	  */
	 public static boolean isSAMRecordValidForVariantCalling(SAMRecord rec) {
		 return isSAMRecordValidForVariantCalling(rec, false);
	 }
	 
	 public static boolean isSAMRecordValidForVariantCalling(SAMRecord rec, boolean includeDuplicates) {
		 return
				 isSAMRecordValid(rec)
				 && (includeDuplicates || ! rec.getDuplicateReadFlag())
				 && ! rec.getReadUnmappedFlag()
				 && ! rec.isSecondaryOrSupplementary();
	 }
	 
	 
		/**
		 * eg. cigar: 10S12M1I10M , MD:Z:10A11 (MD tag don't store insertion, it just present the reference base according to ciagar.M ciagr.D)
		 * refer to: https://github.com/vsbuffalo/devnotes/wiki/The-MD-Tag-in-BAM-Files
		 * @param cigar
		 * @param i
		 * @return the number of softclip/insertion base in front of the offset ith position of the MD tag
		 */
		public static int getAdjustedReadOffset(Cigar cigar, int i) {
			int offset = 0, rollingLength = 0;
			for (CigarElement ce : cigar.getCigarElements()) {
				CigarOperator co = ce.getOperator();
				
				// Match/mismatch
				if (co.consumesReadBases() && co.consumesReferenceBases()) {
					rollingLength += ce.getLength();
				} else if (co.consumesReadBases()) {
					offset += ce.getLength();
				} else if (co.consumesReferenceBases()) {
//					rollingLength += ce.getLength();
				}
				if (rollingLength >= i) {
					break;
				}
				
			}
			return offset;
		}	

}
