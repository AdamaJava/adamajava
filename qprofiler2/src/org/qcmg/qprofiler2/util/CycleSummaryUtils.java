package org.qcmg.qprofiler2.util;

import java.nio.charset.StandardCharsets;
import java.util.TreeSet;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import htsjdk.samtools.Cigar;

public class CycleSummaryUtils {

	private static final int[][] lookupTable = new int[256][256];
	static {
		lookupTable['A']['A'] = 1;
		lookupTable['A']['C'] = 2;
		lookupTable['A']['G'] = 3;
		lookupTable['A']['T'] = 4;
		lookupTable['A']['N'] = 5;

		lookupTable['C']['A'] = 6;
		lookupTable['C']['C'] = 7;
		lookupTable['C']['G'] = 8;
		lookupTable['C']['T'] = 9;
		lookupTable['C']['N'] = 10;

		lookupTable['G']['A'] = 11;
		lookupTable['G']['C'] = 12;
		lookupTable['G']['G'] = 13;
		lookupTable['G']['T'] = 14;
		lookupTable['G']['N'] = 15;

		lookupTable['T']['A'] = 16;
		lookupTable['T']['C'] = 17;
		lookupTable['T']['G'] = 18;
		lookupTable['T']['T'] = 19;
		lookupTable['T']['N'] = 20;

		lookupTable['N']['A'] = 21;
		lookupTable['N']['C'] = 22;
		lookupTable['N']['G'] = 23;
		lookupTable['N']['T'] = 24;
		lookupTable['N']['N'] = 25;
	}

	public static String tallyMDMismatches(
			final String mdData, Cigar cigar, final CycleSummary<Character> tagMDMismatchByCycle,
			final byte[] readBases, final boolean reverse,
			QCMGAtomicLongArray mdRefAltLengthsForward, QCMGAtomicLongArray mdRefAltLengthsReverse,
			boolean isLongReadBam) {

		if (mdData == null) return null;

		int readLength = readBases.length;
		if (cigar.getReadLength() != readLength) {
			return String.format("%s is invalid cigar, readlength from cigar is not same to length of readBases!", cigar);
		}

		if (readLength == 0) return "can't process mismatch for an empty readBase";

		boolean deletion = false;
		int position = 1;
		int dataLength = mdData.length();

		for (int i = 0; i < dataLength; ) {
			char currentChar = mdData.charAt(i);
			if (Character.isDigit(currentChar)) {
				int start = i;
				while (++i < mdData.length() && Character.isDigit(mdData.charAt(i)));
				position +=  Integer.parseInt(mdData, start, i, 10);
			} else if (currentChar == '^') {
				deletion = true;
				i++;
			} else if (isInValidExtended(currentChar)) {
				if (deletion) {
					i = skipDeletions(mdData, i, dataLength);
					deletion = false;
					continue;
				}
				int additionalOffset = SAMUtils.getAdjustedReadOffset(cigar, position);
				if (position + additionalOffset > readLength) {
					return "invalid MD string, position outside read Base!";
				}

				char readBase = adjustAndGetReadBase(readBases, position, additionalOffset, reverse);
				char refBase = reverse ? BaseUtils.getComplement(currentChar) : currentChar;

				if (refBase == readBase) {
					return String.format("Found refBase == altBase, md: %s , cigar: %s, seq: %s, reverse strand: %b",
							mdData, cigar, new String(readBases, StandardCharsets.UTF_8), reverse);
				}

				int adjustedPosition = reverse ? readLength - position - additionalOffset + 1 : position + additionalOffset;
				if (!isLongReadBam) {
					tagMDMismatchByCycle.increment(adjustedPosition, readBase);
				}

				incrementMDRefAltLengths(mdRefAltLengthsForward, mdRefAltLengthsReverse, reverse, refBase, readBase);
				i++;
				position++;
			} else {
				i++;
			}
		}
		
		return null; 				
	}

	private static int skipDeletions(String mdData, int i, int size) {
		while (++i < size && isInValidExtendedInDelete(mdData.charAt(i)));
		return i;
	}

	private static char adjustAndGetReadBase(byte[] readBases, int position, int offset, boolean reverse) {
		char readBase = (char) readBases[position - 1 + offset];
		return reverse ? BaseUtils.getComplement(readBase) : readBase;
	}

	private static void incrementMDRefAltLengths(
			QCMGAtomicLongArray forward, QCMGAtomicLongArray reverse,
			boolean isReverse, char refBase, char readBase) {
		int intFromChar = getIntFromChars(refBase, readBase);
		if (isReverse && reverse != null) {
			reverse.increment(intFromChar);
		} else if (!isReverse && forward != null) {
			forward.increment(intFromChar);
		}
	}
	
	/**
	 * 
	 * @param mdCycles
	 * @param percent
	 * @param allReadsLineLengths
	 * @return the number of cycle with big mismatch rate after combining all strand reads mismatch information
	 */
	public static int getBigMDCycleNo(CycleSummary<Character>[] mdCycles, float percent, QCMGAtomicLongArray[] allReadsLineLengths ) {
		if (mdCycles.length == 0) {
			return 0; 
		}
		
		// get all cycle number
		TreeSet<Integer> cycles = (TreeSet<Integer>) mdCycles[0].cycles();
		for (int i = 1; i < mdCycles.length; i++) {
			cycles.addAll(mdCycles[i].cycles());
		}
		
		int mismatchingCycles = 0;
		for (Integer cycle : cycles) {
			long count = 0, allReadsCount = 0;
			for (int i = 0, len = mdCycles.length; i < len; i++) {
				count += SummaryReportUtils.getCountOfMapValues(mdCycles[i].getValue(cycle));	
				allReadsCount += allReadsLineLengths[i].get(cycle);
			}					
			if ( (( (double) count / allReadsCount )) > percent) {
				mismatchingCycles++;
			}	
		}
		
		return mismatchingCycles; 
	}
  	
	private static boolean isInValidExtended(char c) {
		return c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N';
	}
	
	private static boolean isInValidExtendedInDelete(char c) {
		if (! isInValidExtended(c)) {
			return c == 'M' || c == 'R';
		} else {
			return true;
		}
	}
		
	public static int getIntFromChars(final char ref, final char alt) {
		return lookupTable[ref][alt] == 0 ? -1 : lookupTable[ref][alt];
	}
	
	public static String getStringFromInt(final int i) {
        return switch (i) {
            // A's
            case 1 -> "A>A";
            case 2 -> "A>C";
            case 3 -> "A>G";
            case 4 -> "A>T";
            case 5 -> "A>N";

            // C's
            case 6 -> "C>A";
            case 7 -> "C>C";
            case 8 -> "C>G";
            case 9 -> "C>T";
            case 10 -> "C>N";

            // G's
            case 11 -> "G>A";
            case 12 -> "G>C";
            case 13 -> "G>G";
            case 14 -> "G>T";
            case 15 -> "G>N";

            // T's
            case 16 -> "T>A";
            case 17 -> "T>C";
            case 18 -> "T>G";
            case 19 -> "T>T";
            case 20 -> "T>N";

            // N's
            case 21 -> "N>A";
            case 22 -> "N>C";
            case 23 -> "N>G";
            case 24 -> "N>T";
            case 25 -> "N>N";

            // hmmmm
            case -1 -> "???";
            default -> null;
        };
    }
}
