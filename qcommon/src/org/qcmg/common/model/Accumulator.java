/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.AccumulatorUtils;
import org.qcmg.common.util.Constants;

/**
 * This class aims to track all the necessary information at a loci required by qSNP to make a call on its eligibility as being a position of interest
 * with regard to snp calling
 *
 * NOT THREAD SAFE
 *
 * @author oholmes
 *
 */
public class Accumulator {

    public static final int END_OF_READ_DISTANCE = 5;

    public static final char A_CHAR = 'A';
    public static final char C_CHAR = 'C';
    public static final char G_CHAR = 'G';
    public static final char T_CHAR = 'T';

    public static final byte A_BYTE = 'A';
    public static final byte C_BYTE = 'C';
    public static final byte G_BYTE = 'G';
    public static final byte T_BYTE = 'T';

    public static final String A_STRING = "A";
    public static final String C_STRING = "C";
    public static final String G_STRING = "G";
    public static final String T_STRING = "T";

    private final int position;

    private short failedFilterACount = 0;
    private short failedFilterCCount = 0;
    private short failedFilterGCount = 0;
    private short failedFilterTCount = 0;

    private TLongList readNameHashStrandBasePositionQualities;

    public Accumulator(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void addFailedFilterBase(final byte base) {
        switch (base) {
            case A_BYTE:
                failedFilterACount++;
                break;
            case C_BYTE:
                failedFilterCCount++;
                break;
            case G_BYTE:
                failedFilterGCount++;
                break;
            case T_BYTE:
                failedFilterTCount++;
                break;
            default: /* do nothing */
                break;
        }
    }

    public TLongList getData() {
        return null != readNameHashStrandBasePositionQualities
                ? readNameHashStrandBasePositionQualities
                : new TLongArrayList(0);
    }

    public void setData(TLongList list) {
        readNameHashStrandBasePositionQualities = list;
    }

    public void addBase(final byte base, final byte qual, final boolean forwardStrand, final int startPosition, final int position, final int endPosition, int readId) {
        addBase(base, qual, forwardStrand, startPosition, position, endPosition, (long) readId);
    }


    public void addBase(final byte base, final byte qual, final boolean forwardStrand, final int startPosition, final int position, final int endPosition, long readNameHash) {

        if (this.position != position) {
            throw new IllegalArgumentException("Attempt to add data for wrong position. " +
                    "This position: " + this.position + ", position: " + position);
        }

        boolean endOfRead = startPosition > (position - END_OF_READ_DISTANCE)
                || endPosition < (position + END_OF_READ_DISTANCE);

        // if on the reverse strand, start position is actually endPosition
        int startPositionToUse = forwardStrand ? startPosition : endPosition;


        if (null == readNameHashStrandBasePositionQualities) {
            readNameHashStrandBasePositionQualities = new TLongArrayList(80);
        }
        readNameHashStrandBasePositionQualities.add(readNameHash);
        readNameHashStrandBasePositionQualities.add(AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(forwardStrand, endOfRead, base, qual, startPositionToUse));
    }

    @Override
    public String toString() {
        return position + ":" + AccumulatorUtils.getOABS(this);
    }

    public String getFailedFilterPileup() {
        StringBuilder sb = new StringBuilder();
        if (failedFilterACount > 0) {
            StringUtils.updateStringBuilder(sb, A_STRING + failedFilterACount, Constants.SEMI_COLON);
        }
        if (failedFilterCCount > 0) {
            StringUtils.updateStringBuilder(sb, C_STRING + failedFilterCCount, Constants.SEMI_COLON);
        }
        if (failedFilterGCount > 0) {
            StringUtils.updateStringBuilder(sb, G_STRING + failedFilterGCount, Constants.SEMI_COLON);
        }
        if (failedFilterTCount > 0) {
            StringUtils.updateStringBuilder(sb, T_STRING + failedFilterTCount, Constants.SEMI_COLON);
        }
        return !sb.isEmpty() ? sb.toString() : Constants.MISSING_DATA_STRING;
    }


    public int getCoverage() {
        return null == readNameHashStrandBasePositionQualities ? 0 : readNameHashStrandBasePositionQualities.size() / 2;
    }

}
