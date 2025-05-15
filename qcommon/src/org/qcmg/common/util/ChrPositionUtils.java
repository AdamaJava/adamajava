/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import org.qcmg.common.model.*;
import org.qcmg.common.string.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.qcmg.common.util.Constants.TAB;

public class ChrPositionUtils {

    /**
     * Checks if two ChrPosition objects overlap.
     *
     * @param a the first ChrPosition
     * @param b the second ChrPosition
     * @return true if the ChrPositions overlap, false otherwise
     */
    public static boolean doChrPositionsOverlap(ChrPosition a, ChrPosition b) {
        return doChrPositionsOverlap(a, b, 0);
    }

    /**
     * convert ChrPosition to a long. Will only examine the contig and start position
     */
    public static long convertContigAndPositionToLong(String contig, int position) {
        long l = (long) convertContigNameToInt(contig) << 32;
        return l + position;
    }

    public static ChrPosition convertLongToChrPosition(long l) {
        int contig = (int) (l >> 32);
        int position = (int) l;

        if (contig == 23) {
            return ChrPointPosition.valueOf("X", position);
        } else if (contig == 24) {
            return ChrPointPosition.valueOf("Y", position);
        } else if (contig == 25) {
            return ChrPointPosition.valueOf("M", position);
        } else if (contig > 25) {
            return ChrPointPosition.valueOf("-1", position);
        }
        return ChrPointPosition.valueOf(Integer.toString(contig), position);
    }

    /**
     * Converts a contig name to an integer representing the contig.
     * Assumes that the contig name is either a number or a string that does not start with "chr".
     *
     * @param contigName the name of the contig
     * @return an integer representation of the contig
     */
    public static int convertContigNameToInt(String contigName) {
        if (null == contigName || contigName.isEmpty()) {
            throw new IllegalArgumentException("null or empty contig name supplied to convertContigNameToInt");
        }
        // check if the contig name is a number
        // if so, return it as an int
        // otherwise, convert it to a hash code
        if (isDigits(contigName)) {
            return Integer.parseInt(contigName);
        }


        if (contigName.length() > 3 && contigName.startsWith("chr")) {
            return convertContigNameToInt(contigName.substring(3));
        }

        return switch (contigName) {
            case "X" -> 23;
            case "Y" -> 24;
            case "M", "MT" -> 25;
            default -> contigName.hashCode();
        };
    }

    public static boolean isDigits(String str) {
        if (str == null || str.isEmpty()) return false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    /**
     * Checks if two ChrPosition objects overlap with a buffer.
     *
     * @param a      the first ChrPosition
     * @param b      the second ChrPosition
     * @param buffer the buffer to consider for overlap
     * @return true if the ChrPositions overlap considering the buffer, false otherwise
     */
    public static boolean doChrPositionsOverlap(ChrPosition a, ChrPosition b, int buffer) {

        // check chromosome first
        if (!a.getChromosome().equals(b.getChromosome())) return false;

        // now positions
        if (a.getEndPosition() < b.getStartPosition() - buffer) return false;

        return !(a.getStartPosition() > b.getEndPosition() + buffer);
    }

    /**
     * Checks if a ChrPosition is contained within another ChrPosition.
     *
     * @param a the first ChrPosition
     * @param b the second ChrPosition
     * @return true if b is contained within a, false otherwise
     */
    public static boolean isChrPositionContained(ChrPosition a, ChrPosition b) {

        if (doChrPositionsOverlap(a, b)) {
            return a.getStartPosition() <= b.getStartPosition() && a.getEndPosition() >= b.getEndPosition();
        }
        return false;

    }

    /**
     * Returns a map of ChrRangePosition objects grouped by their start and end positions.
     *
     * @param fragments the list of ChrRangePosition objects
     * @return a map of ChrRangePosition objects grouped by their start and end positions
     */
    public static Map<ChrRangePosition, Set<ChrRangePosition>> getAmpliconsFromFragments(List<ChrRangePosition> fragments) {

        return fragments.stream()
                .collect(Collectors.groupingBy(cp -> new ChrRangePosition(cp.getChromosome(), cp.getStartPosition(), cp.getEndPosition()), Collectors.toSet()));
    }

    /**
     * Returns a ChrPosition object - will be ChrPointPosition if start == end, and ChrRangePosition otherwise
     */
    public static ChrPosition getChrPosition(String chr, int start, int end) {
        if (start == end) {
            return ChrPointPosition.valueOf(chr, start);
        } else {
            return new ChrRangePosition(chr, start, end);
        }
    }

    /**
     * Converts a string in the COSMIC format to a ChrPosition object.
     * The COSMIC format is "9:5073770-5073770", where the chromosome number is followed by the start and end positions separated by a colon and a dash.
     * The resulting ChrPosition object will have "chr" prepended to the chromosome number.
     *
     * @param cosmicCoords the string in COSMIC format
     * @return the corresponding ChrPosition object
     * @throws IllegalArgumentException if the string is null or empty
     */
    public static ChrPosition createCPFromCosmic(String cosmicCoords) {
        if (StringUtils.isNullOrEmpty(cosmicCoords)) {
            return null;
        } else {
            int colonIndex = cosmicCoords.indexOf(':');
            int minusIndex = cosmicCoords.indexOf('-');
            int start = Integer.parseInt(cosmicCoords, colonIndex + 1, minusIndex, 10);
            int end = Integer.parseInt(cosmicCoords, minusIndex + 1, cosmicCoords.length(), 10);
            return getChrPosition("chr" + cosmicCoords.substring(0, colonIndex), start, end);
        }
    }

    /**
     * Checks if the start and end positions of two ChrPosition objects are within a specified delta.
     * The method first checks if the two ChrPosition objects are on the same chromosome.
     * Then, it calculates the absolute difference between the start positions and the end positions of the two ChrPosition objects.
     * If the total difference is less than or equal to the specified delta, the method returns true.
     *
     * @param a     the first ChrPosition
     * @param b     the second ChrPosition
     * @param delta the maximum allowed difference between the start and end positions of the two ChrPosition objects
     * @return true if the start and end positions of the two ChrPosition objects are within the specified delta, false otherwise
     */
    public static boolean arePositionsWithinDelta(ChrPosition a, ChrPosition b, int delta) {
        // check chromosome first
        if (!a.getChromosome().equals(b.getChromosome())) return false;

        int diff = Math.abs(a.getStartPosition() - b.getStartPosition());
        if (diff > delta) {
            return false;
        }

        diff += Math.abs(a.getEndPosition() - b.getEndPosition());
        return diff <= delta;
    }

    /**
     * Checks if two ChrPosition objects overlap considering only their positions.
     *
     * @param a the first ChrPosition
     * @param b the second ChrPosition
     * @return true if the positions of the ChrPositions overlap, false otherwise
     */
    public static boolean doChrPositionsOverlapPositionOnly(ChrPosition a, ChrPosition b) {
        // positions
        if (a.getStartPosition() > b.getEndPosition()) return false;

        return !(a.getEndPosition() < b.getStartPosition());
    }


    /**
     * Returns a new ChrPosition object with a new chromosome name.
     *
     * @param cp     the ChrPosition to clone
     * @param newChr the new chromosome name
     * @return a new ChrPosition object with the new chromosome name
     */
    public static ChrPosition cloneWithNewChromosomeName(ChrPosition cp, String newChr) {
        if (cp instanceof ChrPointPosition) {
            return ChrPointPosition.valueOf(newChr, cp.getStartPosition());
        } else if (cp instanceof ChrRangePosition) {
            return new ChrRangePosition(newChr, cp.getStartPosition(), cp.getEndPosition());
        } else {
            throw new UnsupportedOperationException("cloneWithNewName not yet implemented for any types other than ChrPointPosition and ChrRangePosition!!!");
        }
    }

    /**
     * Converts a string in the format "chr1:12345-12345" to a ChrRangePosition object.
     *
     * @param position the string to convert
     * @return the corresponding ChrRangePosition object
     * @throws IllegalArgumentException if the string is null, empty, not in the correct format, or represents a single point rather than a range
     */
    public static ChrRangePosition getChrPositionFromString(String position) {
        if (StringUtils.isNullOrEmpty(position))
            throw new IllegalArgumentException("Null or empty string passed to getChrPositionFromString()");

        int colonPos = position.indexOf(':');
        int minusPos = position.indexOf('-');

        if (colonPos == -1 || minusPos == -1) {
            throw new IllegalArgumentException("invalid string passed to getChrPositionFromString() - must be in chr1:12345-23456 format: " + position);
        }

        String chr = position.substring(0, colonPos);
        int start = Integer.parseInt(position, colonPos + 1, minusPos, 10);
        int end = Integer.parseInt(position, minusPos + 1, position.length(), 10);

        return new ChrRangePosition(chr, start, end);
    }

    /**
     * Converts a string in the format "chr1:12345-12345" and a name to a ChrPositionName object.
     * The string must represent a single point on the chromosome (start position equals end position).
     *
     * @param position the string to convert
     * @param name     the name to assign to the ChrPositionName object
     * @return the corresponding ChrPositionName object
     * @throws IllegalArgumentException if the string is null, empty, not in the correct format, or represents a range rather than a single point
     */
    public static ChrPositionName getChrPositionNameFromString(String position, String name) {
        if (StringUtils.isNullOrEmpty(position))
            throw new IllegalArgumentException("Null or empty string passed to getChrPositionNameFromString()");

        int colonPos = position.indexOf(':');
        int minusPos = position.indexOf('-');

        if (colonPos == -1 || minusPos == -1) {
            throw new IllegalArgumentException("invalid string passed to getChrPositionNameFromString() - must be in chr1:12345-23456 format: " + position);
        }

        String chr = position.substring(0, colonPos);
        int start = Integer.parseInt(position, colonPos + 1, minusPos, 10);
        int end = Integer.parseInt(position, minusPos + 1, position.length(), 10);

        return new ChrPositionName(chr, start, end, name);
    }

    /**
     * Returns a new ChrPosition object that precedes the given ChrPosition.
     * The start and end positions of the new ChrPosition are each one less than the corresponding positions of the given ChrPosition.
     *
     * @param cp the ChrPosition to get the preceding position of
     * @return a new ChrPosition that precedes the given ChrPosition
     */
    public static ChrPosition getPrecedingChrPosition(ChrPosition cp) {
        return new ChrRangePosition(cp.getChromosome(), cp.getStartPosition() - 1, cp.getEndPosition() - 1);
    }

    /**
     * Converts a ChrPosition and additional data to a VCF string.
     *
     * @param cp     the ChrPosition
     * @param id     the ID
     * @param ref    the reference
     * @param alt    the alternative
     * @param qual   the quality
     * @param filter the filter
     * @param info   the info
     * @return the VCF string
     */
    public static String toVcfString(ChrPosition cp, String id, String ref, String alt, String qual, String filter, String info) {
        switch (cp) {
            case ChrPositionRefAlt cpra -> {
                return cpra.getChromosome() + TAB + cpra.getStartPosition() + TAB + id + TAB + cpra.getRef() + TAB + cpra.getAlt() + TAB + qual + TAB + filter + TAB + info;
            }
            case ChrPosition cpp -> {
                return cpp.getChromosome() + TAB + cpp.getStartPosition() + TAB + id + TAB + ref + TAB + alt + TAB + qual + TAB + filter + TAB + info;
            }
        }
    }

    /**
     * Returns true if the two supplied ChrPosition objects are on the same chromosome and are adjacent
     * that is, the end position of 1 is next to the start position of the other
     * <p>
     * if they overlap, return false
     *
     * @param cp1 the first ChrPosition
     * @param cp2 the second ChrPosition
     * @return true if the ChrPositions are adjacent, false otherwise
     */
    public static boolean areAdjacent(ChrPosition cp1, ChrPosition cp2) {
        // need to be on the same chromosome
        if (cp1.getChromosome().equals(cp2.getChromosome())) {

            if (cp1.getStartPosition() == cp2.getEndPosition() + 1) {
                return true;
            }
            return cp1.getEndPosition() == cp2.getStartPosition() - 1;
        }
        return false;
    }

}
