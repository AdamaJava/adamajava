/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2026.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.string;

import java.nio.charset.StandardCharsets;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Positions in a reference of the tiles (fixed length substrings) of a sequence, so that repeated
 * <code>reference.indexOf(tile, from)</code> calls can be answered with a binary search rather than a scan of the reference.
 * <p>
 * Built with a single pass over the reference. Only tiles made up entirely of the (upper case) bases A, C, G and T are indexed -
 * {@link #tileKey(String, int)} returns -1 for any other tile, and callers should fall back to <code>String.indexOf</code> for those.
 * As a tile of ACGT can only match a reference window of ACGT, windows containing any other character (N, lower case etc.) can never match,
 * and so are skipped.
 */
final class SubStringTileIndex {

	static final int TILE_LENGTH = 13;
	private static final int KEY_MASK = (1 << (2 * TILE_LENGTH)) - 1;

	/*
	 * cheap bit filter on the reference windows, so that the hash map is only consulted for windows that could be one of the tiles
	 */
	private static final int FILTER_BITS = 1 << 16;

	private final TIntObjectHashMap<TIntArrayList> positionsByKey = new TIntObjectHashMap<>();

	/**
	 * @param reference the reference
	 * @param referenceFrom positions in the reference before this are not indexed
	 * @param sequence the sequence whose tiles will be looked up
	 * @param sequenceFrom first tile start position in the sequence to index (inclusive)
	 * @param sequenceTo last tile start position in the sequence to index (exclusive)
	 */
	SubStringTileIndex(String reference, int referenceFrom, String sequence, int sequenceFrom, int sequenceTo) {
		long[] filter = new long[FILTER_BITS / 64];
		for (int j = sequenceFrom; j < sequenceTo; j++) {
			int key = tileKey(sequence, j);
			if (key >= 0 && ! positionsByKey.containsKey(key)) {
				positionsByKey.put(key, new TIntArrayList(4));
				int bit = filterBit(key);
				filter[bit >>> 6] |= 1L << bit;
			}
		}
		if (positionsByKey.isEmpty()) {
			return;
		}

		/*
		 * Latin-1 bytes of the remaining reference (fast array copies), as it is quicker to walk a byte array with a lookup table than to call charAt
		 * Any char that is not Latin-1 becomes '?' which, like every other char that is not A, C, G or T, is not part of any indexed window
		 */
		byte[] bytes = reference.substring(referenceFrom).getBytes(StandardCharsets.ISO_8859_1);
		int key = 0;
		int run = 0;
		for (int p = 0, len = bytes.length; p < len; p++) {
			int code = CODES[bytes[p] & 0xFF];
			if (code < 0) {
				run = 0;
				continue;
			}
			key = ((key << 2) | code) & KEY_MASK;
			if (++run >= TILE_LENGTH) {
				int bit = filterBit(key);
				if ((filter[bit >>> 6] & (1L << bit)) != 0) {
					TIntArrayList positions = positionsByKey.get(key);
					if (positions != null) {
						positions.add(referenceFrom + p - TILE_LENGTH + 1);		// positions are added in ascending order
					}
				}
			}
		}
	}

	private static final byte[] CODES = new byte[256];
	static {
		java.util.Arrays.fill(CODES, (byte) -1);
		CODES['A'] = 0;
		CODES['C'] = 1;
		CODES['G'] = 2;
		CODES['T'] = 3;
	}

	private static int filterBit(int key) {
		return (key ^ (key >>> 13)) & (FILTER_BITS - 1);
	}

	static int baseCode(char c) {
		return switch (c) {
			case 'A' -> 0;
			case 'C' -> 1;
			case 'G' -> 2;
			case 'T' -> 3;
			default -> -1;
		};
	}

	/**
	 * @return the key of the tile starting at <code>from</code> in <code>s</code>, or -1 if the tile contains anything other than A, C, G or T
	 */
	static int tileKey(String s, int from) {
		int key = 0;
		for (int i = from, end = from + TILE_LENGTH; i < end; i++) {
			int code = baseCode(s.charAt(i));
			if (code < 0) {
				return -1;
			}
			key = (key << 2) | code;
		}
		return key;
	}

	/**
	 * Same as <code>reference.indexOf(tile, from)</code> for an indexed tile, provided that <code>from</code> is not less than the referenceFrom used to build this index.
	 */
	int indexOf(int key, int from) {
		TIntArrayList positions = positionsByKey.get(key);
		if (positions == null || positions.isEmpty()) {
			return -1;
		}
		int lo = 0;
		int hi = positions.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (positions.getQuick(mid) < from) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo < positions.size() ? positions.getQuick(lo) : -1;
	}
}
