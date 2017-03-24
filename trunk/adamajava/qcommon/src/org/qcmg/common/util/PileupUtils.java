/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import org.qcmg.common.string.StringUtils;

public class PileupUtils {
	
	/**  Three columns per sample: count, bases, qualities */
	private static final int NO_OF_PILEUP_COLUMNS_PER_SAMPLE = 3;
	/** first entries in pileup file are chromosome, position, and reference*/
	private static final int PILEUP_OFFSET = 3;
	
	/**
	 * Calculates the start positions of interest within a pileup file.
	 * <p>
	 * eg. for a pileup file containing 2 normal files, and 2 tumour files, the returned int array for the normal data would contain
	 * [3.6] and for tumour would be [9,12]
	 * <p>
	 *  This assumes that the pileup file contains normal data first, then tumour
	 *  Note that if no normal or tumour data exists, use 0 for the respective file count
	 *  
	 * @param noOfNormalFiles int count of normal files in pileup
	 * @param noOfTumourFiles int count of normal files in pileup
	 * @param forNormal boolean indicating if its the normal start positions that are being requested
	 * @return int array corresponding to positions within the pileup file that are of interest
	 */
	public static int[] getStartPositions(int noOfNormalFiles, int noOfTumourFiles, boolean forNormal) {
		int[] returnArray = null;
		
		if (forNormal) {
			returnArray = new int[noOfNormalFiles];
			for (int i = PILEUP_OFFSET, j = 0; j < noOfNormalFiles; i += NO_OF_PILEUP_COLUMNS_PER_SAMPLE, j++)
				returnArray[j] = i;
		} else {
			returnArray = new int[noOfTumourFiles];
			for (int i = PILEUP_OFFSET + (noOfNormalFiles * NO_OF_PILEUP_COLUMNS_PER_SAMPLE), j = 0 ; 
					j < noOfTumourFiles ; i += NO_OF_PILEUP_COLUMNS_PER_SAMPLE, j++)
				returnArray[j] = i;
		}
		return returnArray;
	}
	
	/**
	 * Returns the relevant counts from the pileup
	 * 
	 * @param params String array containing the pileup
	 * @param startPositions int array containing the positions in the pileup array that are of interest
	 * @return int containing the count from the pileup data 
	 */
	public static int getCoverageCount(String[] params, int[] startPositions) {
		if (null == params) throw new IllegalArgumentException("null string array passed to getCoverageCount");
		if (null == startPositions) throw new IllegalArgumentException("null int array passed to getCoverageCount");
		
		int coverage = 0;
		for (int i : startPositions)
			coverage += Integer.parseInt(params[i]);
		return coverage;
	}
	
	/**
	 * Returns the relevant bases from the pileup
	 * 
	 * @param params String array containing the pileup
	 * @param startPositions int array containing the positions in the pileup array that are of interest
	 * @return String containing the bases from the pileup data
	 */
	public static String getBases(String[] params, int[] startPositions) {
		if (null == params) throw new IllegalArgumentException("null string array passed to getBases");
		if (null == startPositions) throw new IllegalArgumentException("null int array passed to getBases");
		
		String bases = "";
		for (int i : startPositions)
			bases += params[i + 1];
		
		return bases;
	}
	
	/**
	 * Returns the relevant qualities from the pileup  
	 * 
	 * @param params String array containing the pileup
	 * @param startPositions int array containing the positions in the pileup array that are of interest
	 * @return String containing the qualities from the pileup data
	 */
	public static String getQualities(String[] params, int[] startPositions) {
		if (null == params) throw new IllegalArgumentException("null string array passed to getQualities");
		if (null == startPositions) throw new IllegalArgumentException("null int array passed to getQualities");
		
		String qualities = "";
		for (int i : startPositions)
			qualities += params[i + 2];

		return qualities;
	}
	
	/**
	 * Returns the number of times a particular char is seen in a string.
	 * <p>
	 * Used to determine how many tumour and normal files are present in the pileupFormat string
	 * eg. NNNT will return 3 when type is N, and 1 when type is T
	 *  
	 * @param pileupFormat String indicating the number of normal and tumour samples in pileup file eg. NNTT
	 * @param type char representing the type of interest
	 * @return int count of char type in string pileupFormat
	 */
	public static int getNoOfFilesFromPileupFormat(String pileupFormat, char type) {
		int count = 0;
		if (null != pileupFormat) {
			for (int i  = 0, len = pileupFormat.length() ; i < len ; i++) {
				if (type == pileupFormat.charAt(i)) count++;
			}
		}
		return count;
	}
	
	/**
	 *  Indels are identified by a + (insertion) or a - (deletion)<br>
	*   one needs to check that these symbols do not occur immediately after the new read symbol (^) as in that
	*   scenario, the symbol corresponds to the mapping quality of the new read...
	*   
	 * @param pileup
	 * @return
	 */
	public static boolean doesPileupContainIndel(String pileup) {
			
		return (containsCharacterNotPrecededByNewRead(pileup,  '+') ||
				containsCharacterNotPrecededByNewRead(pileup, '-'));
	}
	
	/**
	 * Examines the supplied string and returns true if the supplied character 
	 * appears in the string AND is not preceded by the start of read character (^)
	 * 
	 * 
	 * @param pileup
	 * @param indel
	 * @return
	 */
	static boolean containsCharacterNotPrecededByNewRead(String pileup, char indel) {
		if (StringUtils.isNullOrEmpty(pileup))
			throw new IllegalArgumentException("null or empty pileup string passed to containsCharacterNotPrecededByNewRead");
		
		int index = pileup.indexOf(indel);
		while (index > -1) {
			
			// if indel is first position in string - return true
			if (index == 0) return true;
			
			// if previous char is NOT start of read - return true
			if ('^' != pileup.charAt(index-1)) return true;
			
			// otherwise, get the next index value
			index = pileup.indexOf(indel, index+1);
		}
		return false;
	}
}
