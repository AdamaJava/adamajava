/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

import java.util.Arrays;

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

    /**
     * default initial capacity (in longs) of the data list - each base uses 2 longs
     */
    public static final int DEFAULT_INITIAL_DATA_CAPACITY = 80;

    /**
     * default initial capacity of each failed filter list (1 long per base) - the same as the TLongArrayList default
     */
    public static final int DEFAULT_INITIAL_FAILED_FILTER_CAPACITY = 10;

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

    /*
     * read name hashes of the bases that failed the filter (or base quality check), by base
     * raw arrays and sizes for the same reasons as data (below). Arrays are null until the first base is added
     */
    private long[] failedFilterA;
    private long[] failedFilterC;
    private long[] failedFilterG;
    private long[] failedFilterT;
    private int failedFilterASize;
    private int failedFilterCSize;
    private int failedFilterGSize;
    private int failedFilterTSize;

    /*
     * pairs of longs - read name hash, followed by the encoded strand, end of read, base, quality and read start position
     * (see AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong)
     *
     * held as a raw array and size, rather than a TLongArrayList, to save an object and a pointer dereference per base -
     * adding bases is the hottest part of qsnp
     * data.length >= dataSize, and data is null until the first base is added
     */
    private long[] data;
    private int dataSize;

    public Accumulator(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void addFailedFilterBase(final byte base, long readNameHash) {
        addFailedFilterBase(base, readNameHash, DEFAULT_INITIAL_FAILED_FILTER_CAPACITY);
    }

    /**
     * Records the read name hash of a base that failed the filter
     *
     * @param base only A, C, G and T are recorded
     * @param readNameHash
     * @param initialCapacity capacity of the list for this base if this is the first failed filter base of this type
     * @return true if this created a new list (ie. this is the first failed filter base of this type at this position)
     */
    public boolean addFailedFilterBase(final byte base, long readNameHash, int initialCapacity) {
        switch (base) {
            case A_BYTE:
                failedFilterA = append(failedFilterA, failedFilterASize, readNameHash, initialCapacity);
                return ++failedFilterASize == 1;
            case C_BYTE:
                failedFilterC = append(failedFilterC, failedFilterCSize, readNameHash, initialCapacity);
                return ++failedFilterCSize == 1;
            case G_BYTE:
                failedFilterG = append(failedFilterG, failedFilterGSize, readNameHash, initialCapacity);
                return ++failedFilterGSize == 1;
            case T_BYTE:
                failedFilterT = append(failedFilterT, failedFilterTSize, readNameHash, initialCapacity);
                return ++failedFilterTSize == 1;
            default: /* do nothing */
                return false;
        }
    }

    /*
     * sets array[size] to value, creating or growing (doubling, as TLongArrayList does) the array if required
     * returns the array, which will be a new instance if it was created or grown
     */
    private static long[] append(long[] array, int size, long value, int initialCapacity) {
        if (null == array) {
            array = new long[Math.max(1, initialCapacity)];
        } else if (size == array.length) {
            array = Arrays.copyOf(array, Math.max(array.length << 1, size + 1));
        }
        array[size] = value;
        return array;
    }

    /**
     * Returns a {@link TLongList} view of the data (read name hash and encoded base pairs).
     * <p>
     * The view shares the underlying array, so is cheap to create, but changes to it are not reliably reflected in this Accumulator
     * (eg. if it grows) - use {@link #setData(TLongList)} to replace the data.
     * <p>
     * Performance critical code should use {@link #getDataArray()} and {@link #getDataSize()} instead.
     */
    public TLongList getData() {
        return null != data ? new DataView(data, dataSize) : new TLongArrayList(0);
    }

    /**
     * Replaces the data with a copy of the supplied list
     */
    public void setData(TLongList list) {
        if (null == list) {
            data = null;
            dataSize = 0;
        } else {
            data = list.toArray();
            dataSize = data.length;
        }
    }

    /**
     * Returns the underlying data array (read name hash and encoded base pairs), which may be null if no bases have been added.
     * <p>
     * Only the first {@link #getDataSize()} entries are valid. The array must not be modified.
     */
    public long[] getDataArray() {
        return data;
    }

    /**
     * Returns the number of valid entries in {@link #getDataArray()} - 2 per base
     */
    public int getDataSize() {
        return dataSize;
    }

    /*
     * read-only-by-convention TLongList view of an array - wraps rather than copies
     */
    private static final class DataView extends TLongArrayList {
        DataView(long[] data, int size) {
            super(data, 0L, true);
            _pos = size;
        }
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


        addEncodedBase(readNameHash, AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(forwardStrand, endOfRead, base, qual, startPositionToUse), DEFAULT_INITIAL_DATA_CAPACITY);
    }

    /**
     * Adds a base that has already been encoded by {@link AccumulatorUtils#encodeBase(long, boolean, byte, byte)}.
     * <p>
     * Unlike {@link #addBase(byte, byte, boolean, int, int, int, long)} this does not check that the base belongs to this position,
     * so callers must ensure that it does.
     *
     * @param readNameHash
     * @param encodedBase
     * @param initialCapacity capacity (in longs - each base uses 2) of the data array if it has not yet been allocated
     * @return true if this was the first base added to this Accumulator (ie. it held no bases before this call, including after {@link #setData(TLongList)} with an empty list)
     */
    public boolean addEncodedBase(long readNameHash, long encodedBase, int initialCapacity) {
        long[] d = data;
        final int size = dataSize;
        final boolean first = size == 0;
        if (null == d) {
            d = new long[Math.max(2, initialCapacity)];
            data = d;
        } else if (size + 2 > d.length) {
            // same growth policy as TLongArrayList - double
            d = Arrays.copyOf(d, Math.max(d.length << 1, size + 2));
            data = d;
        }
        d[size] = readNameHash;
        d[size + 1] = encodedBase;
        dataSize = size + 2;
        return first;
    }

    @Override
    public String toString() {
        return position + ":" + AccumulatorUtils.getOABS(this);
    }

    public String getFailedFilterPileup() {
        StringBuilder sb = new StringBuilder();
        if (failedFilterASize > 0) {
            StringUtils.updateStringBuilder(sb, A_STRING + failedFilterASize, Constants.SEMI_COLON);
        }
        if (failedFilterCSize > 0) {
            StringUtils.updateStringBuilder(sb, C_STRING + failedFilterCSize, Constants.SEMI_COLON);
        }
        if (failedFilterGSize > 0) {
            StringUtils.updateStringBuilder(sb, G_STRING + failedFilterGSize, Constants.SEMI_COLON);
        }
        if (failedFilterTSize > 0) {
            StringUtils.updateStringBuilder(sb, T_STRING + failedFilterTSize, Constants.SEMI_COLON);
        }
        return !sb.isEmpty() ? sb.toString() : Constants.MISSING_DATA_STRING;
    }


    public int getCoverage() {
        return dataSize / 2;
    }

    public boolean isEmpty() {
        return null == data && null == failedFilterA && null == failedFilterC && null == failedFilterG && null == failedFilterT;
    }

    /*
     * The failed filter getters return a read-only-by-convention view of the read name hashes, or null if there are none for that base
     */
    public TLongList getFailedFilterACount() {
        return null == failedFilterA ? null : new DataView(failedFilterA, failedFilterASize);
    }

    public TLongList getFailedFilterCCount() {
        return null == failedFilterC ? null : new DataView(failedFilterC, failedFilterCSize);
    }

    public TLongList getFailedFilterGCount() {
        return null == failedFilterG ? null : new DataView(failedFilterG, failedFilterGSize);
    }

    public TLongList getFailedFilterTCount() {
        return null == failedFilterT ? null : new DataView(failedFilterT, failedFilterTSize);
    }
}
