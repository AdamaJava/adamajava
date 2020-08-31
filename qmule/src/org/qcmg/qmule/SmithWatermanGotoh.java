/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class SmithWatermanGotoh {
	
	private static final byte STOP = 0;
	private static final byte LEFT = 1;	
	private static final byte DIAGONAL = 2;
	private static final byte UP = 3;
	private static final char GAP = '-';
	private static final char EMPTY = ' ';
	private static final char MISMATCH = '.';
	private static final char MATCH = '|';
	private static final short ONE = 1;
	
	private static final short [] SHORT_ARRAY_1 = new short[10000];
	static {
		arrayFill(SHORT_ARRAY_1);
	}
	
	private final float gapOpen;
	private final float gapExtend;
	private final int matchScore;
	private final int mismatchScore;
	private final String sequenceA;
	private final String sequenceB;
	private final int rows;
	private final int columns;
//	private POINTER[][] pointerMatrix;
	private byte[][] pointerMatrix;
//	private int[][] pointerMatrix;
	private short[][] verticalGaps;
	private short[][] horizontalGaps;
	private int bestRow;
	private int bestColumn;
	private float bestScore;
//	private short[] intialShortArraySetTo1;
	
	public SmithWatermanGotoh(File fileA, File fileB, int matchScore, int mismatchScore, float gapOpen, float gapExtend) throws IOException  {
		
		this.sequenceA = readFastaFile(fileA);
		this.sequenceB = readFastaFile(fileB);
		this.gapOpen = gapOpen;
		this.gapExtend = gapExtend;
		this.matchScore = matchScore;
		this.mismatchScore =  mismatchScore;
		this.rows = sequenceA.length() + 1;//i
		this.columns = sequenceB.length() + 1;//j
		align();
	}
	
	public SmithWatermanGotoh(String a, String b, int matchScore, int mismatchScore, float gapOpen, float gapExtend) {
		
		this.sequenceA = a;
		this.sequenceB = b;
		this.gapOpen = gapOpen;
		this.gapExtend = gapExtend;
		this.matchScore = matchScore;
		this.mismatchScore =  mismatchScore;
		this.rows = sequenceA.length() + 1;//i
		this.columns = sequenceB.length() + 1;//j
//		intialShortArraySetTo1 = Arrays.copyOf(SHORT_ARRAY_1, columns);
		fillMatrix();
	}
	
	private String readFastaFile(File file) throws IOException {
		
		try (FileInputStream inputStream = new FileInputStream(file)) {
	        StringBuilder buffer = new StringBuilder();
	        int ch;
	        while ((ch = inputStream.read()) != -1) {
	            buffer.append((char)ch);
	        }
	        inputStream.close();
	        
	        String seq = buffer.toString();
	        
	        if (seq.startsWith(">")) {
		        	int index = seq.indexOf("\n");
		        	return seq.substring(index, seq.length()).replace("\n", "").toUpperCase();
	        } else {
	        		return seq.replace("\n", "").toUpperCase();
	        }
		}
	}

	private void align() {		
		fillMatrix();
//		traceback();
//		System.out.println(getDiffs());
	}
	
	private void fillMatrix() {
		//etutorials.org/Misc/blast/Part+II+Theory/Chapter+3.+Sequence+Alignment/3.2+Local+Alignment+Smith-Waterman/
		//Gotoh: http://www.cse.msu.edu/~cse891/Sect001/notes_alignment.pdf
		//https://github.com/ekg/smithwaterman/blob/master/SmithWatermanGotoh.cpp
		//http://cci.lbl.gov/cctbx_sources/mmtbx/alignment.py
		//
		//The dynamic programming algorithm was improved in performance by Gotoh (1982) by using the linear 
//		relationship for a gap weight wx = g + rx, where the weight for a gap of length x is the sum of a gap 
//				opening penalty (g) and a gap extension penalty (r) times the gap length (x), and by simplifying 
//				the dynamic programming algorithm. He reasoned that two of the terms that are maximized in the 
//				dynamic programming algorithm and designated here Pij and Qij depend only on the values in the 
//				current and previous row and column, as indicated below.
		
//		initializeNew();
		initialize();
//		afterInitialiseState();
		
		//storage for current calculations
		float[] bestScores = new float[columns];//score if xi aligns to gap after yi		
		float[] queryGapScores = new float[columns];//best score of alignment x1..xi to y1..yi
		Arrays.fill(queryGapScores, Float.NEGATIVE_INFINITY);
		
		float currentAnchorGapScore;// score if yi aligns to a gap after xi
		float totalSimilarityScore;		
		float bestScoreDiagonal;	
		
		//keep track of highset score for traceback
		bestRow = 0;
		bestColumn = 0;
		bestScore = Float.NEGATIVE_INFINITY;
		
		for (int row = 1; row < rows; row++) {
			currentAnchorGapScore = Float.NEGATIVE_INFINITY;
			bestScoreDiagonal = bestScores[0];
			for (int column = 1; column < columns; column++) {
				totalSimilarityScore = bestScoreDiagonal + findSimilarity(row, column);								
				
				if (column > 1 && (queryGapScores[column] - gapExtend) > ( bestScores[column] - gapOpen)) {
					//add extend score
					queryGapScores[column] = queryGapScores[column] - gapExtend;
					//increase size of gap
					verticalGaps[row][column] = (short) (verticalGaps[row - 1][column] + 1);  
				} else {
					//add open score
					queryGapScores[column] =  bestScores[column] - gapOpen; 
				}
				
				if (column > 1 && currentAnchorGapScore - gapExtend > bestScores[column - 1] - gapOpen) {
					//add extend score
					currentAnchorGapScore = currentAnchorGapScore - gapExtend;
					//increase size of gap					
					horizontalGaps[row][column] = (short) (horizontalGaps[row][column - 1] + 1);	
				} else {
					//add open score
					currentAnchorGapScore = bestScores[column - 1] - gapOpen; 
				}
				
				//test scores
				bestScoreDiagonal = bestScores[column];
				bestScores[column] = findMaximum(totalSimilarityScore, queryGapScores[column], currentAnchorGapScore);
				
				//determine trackback direction
				if (bestScores[column] == 0) {
					/*
					 * don't think we need to set this as it should already be zero
					 */
//					pointerMatrix[row][column] = STOP;
				} else if (bestScores[column] == totalSimilarityScore) {
					pointerMatrix[row][column] = DIAGONAL;
				} else if (bestScores[column] == queryGapScores[column]) {
					pointerMatrix[row][column] = UP;
				} else {
					pointerMatrix[row][column] = LEFT;
				}
				
				//set current cell if this is the best score
				if (bestScores[column] > bestScore) {
					bestRow = row;
					bestColumn = column;
					bestScore = bestScores[column];
				}				
			}
		}
	}
	
	public void afterInitialiseState() {
//		System.out.println("pointerMatrix: " + pointerMatrix.length);
//		System.out.println("verticalGaps: " + verticalGaps.length);
//		System.out.println("horizontalGaps: " + horizontalGaps.length);
//		System.out.println("pointerMatrix: " + Arrays.deepToString(pointerMatrix));
//		System.out.println("verticalGaps: " + Arrays.deepToString(verticalGaps));
//		System.out.println("horizontalGaps: " + Arrays.deepToString(horizontalGaps));
	}
	
	private static final int SMALL = 32;

	public static void arrayFill(short[] array) {
	  int len = array.length;
	  int lenB = len < SMALL ? len : SMALL;
	  for (int i = 0; i < lenB; i++) {
	    array[i] = ONE;
	  }
	  for (int i = lenB; i < len; i += i) {
	    System.arraycopy(array, 0, array, i, len < i + i ? len - i : i);
	  }
	}
	public static void arrayFill(short[] array1, short[] array2) {
		int len = array1.length;
		int lenB = len < SMALL ? len : SMALL;
		for (int i = 0; i < lenB; i++) {
			array1[i] = ONE;
			array2[i] = ONE;
		}
		for (int i = lenB; i < len; i += i) {
			System.arraycopy(array1, 0, array1, i, len < i + i ? len - i : i);
			System.arraycopy(array1, 0, array2, i, len < i + i ? len - i : i);
		}
	}
	public static void arrayFill(float[] array) {
		int len = array.length;
		int lenB = len < SMALL ? len : SMALL;
		for (int i = 0; i < lenB; i++) {
			array[i] = Float.NEGATIVE_INFINITY;
		}
		for (int i = lenB; i < len; i += i) {
			System.arraycopy(array, 0, array, i, len < i + i ? len - i : i);
		}
	}
	
	public static void arrayFill2D(short[][] array, int length) {
		int len = array.length;
		int lenB = len < SMALL ? len : SMALL;
		array[0] = Arrays.copyOf(SHORT_ARRAY_1, length);
//		for (int i = 0; i < lenB; i++) {
//			arrayFill(array[i]);
////			Arrays.fill(array[i], (short)1);
//		}
//		for (int i = 1; i < len; i += i) {
			System.arraycopy(array, 0, array, 1, len - 1);
//		}
	}

	private void initialize() {
		pointerMatrix = new byte[rows][columns];
		verticalGaps = new short[rows][columns];
		horizontalGaps = new short[rows][columns];
		for (int row = 0; row < rows; row++) {
			Arrays.fill(verticalGaps[row], ONE);
			Arrays.fill(horizontalGaps[row], ONE);
		}
	}
	
	public static void fillArrays(short[][] array1, short[][] array2, short[] source, int length) {
		for (int i = 0, len = array1.length; i < len ; i++) {
//			System.arraycopy(source, 0, array1[i], 0, length);
			array1[i] = source.clone();
			array2[i] = source.clone();
//			System.arraycopy(source, 0, array2[i], 0, length);
		}
	}
	
	private void initializeNew() {
		pointerMatrix = new byte[rows][columns];
		verticalGaps = new short[rows][columns];
		horizontalGaps = new short[rows][columns];
		fillArrays(verticalGaps, horizontalGaps, Arrays.copyOf(SHORT_ARRAY_1, columns), columns);
	}
	
	public static short[][] cloneArray(short[][] src) {
	    int length = src.length;
	    short[][] target = new short[length][src[0].length];
	    for (int i = 0; i < length; i++) {
	        System.arraycopy(src[i], 0, target[i], 0, src[i].length);
	    }
	    return target;
	}
	
	private void initializeOld() {
		pointerMatrix = new byte[rows][columns];
		verticalGaps = new short[rows][columns];
		horizontalGaps = new short[rows][columns];
		for (int i = 0; i < rows; i++) {
			pointerMatrix[i][0] = STOP;
			if (i == 0) {
				for (int j = 0; j < columns; j++) {
					pointerMatrix[0][j] = STOP;
				}
			}
		}		
		
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				verticalGaps[row][column] = 1;
				horizontalGaps[row][column] = 1;
			}
		}		
	}
	
	public String[] traceback() {
		StringBuilder alignmentA = new StringBuilder();
		StringBuilder gapString = new StringBuilder();
		StringBuilder alignmentB = new StringBuilder();
		
		int rs = bestRow;
		int cs = bestColumn;
		
		boolean run = true;
		while (run) {
			switch(pointerMatrix[rs][cs]) {
				case LEFT:		
					
				    //horizontal gap
					int hEnd = horizontalGaps[rs][cs];
					for (int i = 0; i < hEnd; i++) {
						alignmentA.append(GAP);
						gapString.append(EMPTY);
						alignmentB.append(sequenceB.charAt(--cs));
					}				
					break;
			
				case DIAGONAL:
					
					char a = sequenceA.charAt(--rs);
					char b = sequenceB.charAt(--cs);
					alignmentA.append(a);
					alignmentB.append(b);
					if (a == b) {
						gapString.append(MATCH);
					} else {
						gapString.append(MISMATCH);
					}

					break;
			
				case UP:
					//vertical gap					
					int vEnd = verticalGaps[rs][cs];
					for (int i = 0; i < vEnd; i++) {
						alignmentB.append(GAP);				
						gapString.append(EMPTY);
						alignmentA.append(sequenceA.charAt(--rs));
					}					
					break;
					
				case STOP:
					run = false;
					break;		
			}
		}
		
		return new String[] {alignmentA.reverse().toString(), gapString.reverse().toString(), alignmentB.reverse().toString()};  
	}
//	public String[] traceback() {
//		StringBuilder alignmentA = new StringBuilder();
//		StringBuilder gapString = new StringBuilder();
//		StringBuilder alignmentB = new StringBuilder();
//		
//		int rs = bestRow;
//		int cs = bestColumn;
//		
//		boolean run = true;
//		while (run) {
//			switch(pointerMatrix[rs][cs]) {
//			case LEFT:		
//				
//				//horizontal gap
//				int hEnd = horizontalGaps[rs][cs];
//				for (int i = 0; i < hEnd; i++) {
//					alignmentA.append(GAP);
//					gapString.append(EMPTY);
//					alignmentB.append(sequenceB.charAt(--cs)).append(TAB);
//				}					
//				break;
//				
//			case DIAGONAL:
//				
//				char a = sequenceA.charAt(--rs);
//				char b = sequenceB.charAt(--cs);
//				alignmentA.append(a).append(TAB);
//				alignmentB.append(b).append(TAB);
//				if (a == b) {
//					gapString.append(MATCH);
//				} else {
//					gapString.append(MISMATCH);
//				}
//				
//				break;
//				
//			case UP:
//				//vertical gap					
//				int vEnd = verticalGaps[rs][cs];
//				for (int i = 0; i < vEnd; i++) {
//					alignmentB.append(GAP);						
//					gapString.append(EMPTY);
//					alignmentA.append(sequenceA.charAt(--rs)).append(TAB);						
//				}					
//				break;
//				
//			case STOP:
//				run = false;
//				break;		
//			}
//		}
//		
//		return new String[] {alignmentA.reverse().toString(), gapString.reverse().toString(), alignmentB.reverse().toString()};  
//	}
	
//	public String getDiffs() {
//		StringBuilder alignmentA = new StringBuilder();
//		StringBuilder gapString = new StringBuilder();
//		StringBuilder alignmentB = new StringBuilder();
//		StringBuilder diffs = new StringBuilder();
//		
//		int rs = bestRow;
//		int cs = bestColumn;
//		
//		boolean run = true;
//		int pos = 0;
//		while (run) {
//			pos++;
//			switch(pointerMatrix[rs][cs]) {
//			case LEFT:		
//				
//				//horizontal gap
//				int hEnd = horizontalGaps[rs][cs];
//				for (int i=0; i<hEnd; i++) {
//					alignmentA.append(GAP);
//					gapString.append(EMPTY);
//					alignmentB.append(sequenceB.charAt(--cs)).append(TAB);
//					diffs.append(pos++).append(":").append(GAP).append("/").append(sequenceB.charAt(cs)).append(",");
//				}					
//				break;
//				
//			case DIAGONAL:
//				
//				char a = sequenceA.charAt(--rs);
//				char b = sequenceB.charAt(--cs);
//				alignmentA.append(a).append(TAB);
//				alignmentB.append(b).append(TAB);
//				if (a == b) {
//					gapString.append(MATCH);
//				} else {
//					gapString.append(MISMATCH);
//					diffs.append(pos).append(":").append(a).append("/").append(b).append(",");
//				}
//				
//				break;
//				
//			case UP:
//				//vertical gap					
//				int vEnd = verticalGaps[rs][cs];
//				for (int i=0; i<vEnd; i++) {
//					alignmentB.append(GAP);						
//					gapString.append(EMPTY);
//					alignmentA.append(sequenceA.charAt(--rs)).append(TAB);						
//					diffs.append(pos++).append(":").append(sequenceA.charAt(rs)).append("/").append(GAP).append(",");
//				}					
//				break;
//				
//			case STOP:
//				run = false;
//				break;		
//			}
//		}
		
//		System.out.println(alignmentA.reverse().toString());
//		System.out.println(gapString.reverse().toString());
//		System.out.println(alignmentB.reverse().toString());
//		return (diffs.toString());
//		
//	}
	
	private int findSimilarity(int row, int column) {
		if (sequenceA.charAt(row - 1) == sequenceB.charAt(column - 1)) {
			return matchScore;
		} 
		return mismatchScore;
	}

	private float findMaximum(float valueA, float valueB, float valueC) {
		if (valueA <= 0 && valueB <= 0 && valueC <= 0) {
			return 0;
		}
		return Math.max(valueA, Math.max(valueB, valueC));
	}
	
	public static void main(String[] args) throws IOException {
		String sequence1 = "GAATTCAG";
		String sequence2 = "GGATCGA";
		
		SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		String [] results = nm.traceback();
		for (String s : results) {
			System.out.println(s);
		}
//		System.exit(1);
		
//		String sequence1 = "TTGGGCTAAA";
//		String sequence2 = "TTGGGAACTAAA";
//		String sequence1 = "ATCGCTTTATGTTTTTGGTTATTGGTTTTTTTGTATAGACCAAAGCAAAGAAAATAACAATAACACAGATGCGTTGGAGGCTGTTTAGGGGAGTGGGGTGGGAAAGTTGAGGGGCTTCCCTAGCGGCCTGGCGCCCTCTTTGCTGGGTCCTGCGGGTCCTCAGGGCTGCCTTGCATGTGGGAAGGACTAGAAGAGGCAAGCTGGGGAGCCAGGAGTGTTGGGGGA";
//		String sequence2 = "ATCGCTTTATGTTTTTGGTTATTGGTTTTTTTGTATAGACCAAAGCAAAGAAAATAAAAATAACACAGATGCGTTGGAGGCTGTTTAGGGGAGTGGGGTGGGAAAGTTGAGGGGCTTCCCTAGCGGCCTGGCGCCCTCTTTGCTGGGTCCTGCTGGTCCTCAGGGCTGCCTTGCATGTGGGAAGGACTAGAAGAGGCAAGCTGGGGAGCCAGGAGTGTTGGGGGA";
		sequence1 = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGGTTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCCTAC";
		sequence2 = "CAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGGTTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCCTAC";
//		String sequence1 = "CGATTTCTTGATCACATAGACTTCCATTTTCTACTTTTTCTGAGGTTTCCTCTGGTCCTGGTATGAAGAATGTATTTACCCAAAAGTGAAACATTTTGTCCTAAAAAAAAAAAAAAAGAAAAGAAAAAGAAATGAAATGACATATTTAATTAATGATGTTTTATTTTTTTAAAAAAGAAAATCTGTCACCTATGTTAAACATTTGCAAAAAGTCAACAAAATAAAC";
//		String sequence2 = "CGATTTCTTGATCACATAGACTTCCATTTTCTACTTTTTCTGAGGTTTCCTCTGGTCCTGGTATGAAGAATGTATTTACCCAAAAGTGAAACATTTTGTGCGAAAAAAAAAAAAGAAAAGAAAAAGAAATGAAATGACATATTTAATTAATGATGTTTTATTTTTTTAAAAAAGAAAATCTGTCACCTATGTTAAACATTTGCAAAAAGTCAACAAAATAAAC";
//		String sequence1 = "AAATATGCTTCACTTCAGAAGACATTTTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
//		String sequence2 = "GAATATGCTTCACTTCAGAAGACATTTTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
		nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
		for (String s : results) {
			System.out.println(s);
		}
		if ( ! results[0].equals("AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGGTTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCCTAC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[1].equals("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[2].equals("AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGGTTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCCTAC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}

		String ref=("aaaagaaccaggagggcacatgggcatggggagtgatgaaccagagaaag"+
				"ctgctgtctttctgggcaagtgccaagcaacggatcacccttgaccccta"+
				"GGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCG"+
				"AAAAAACTTTCAGGCCCTGTTGGAGGAGCAGgtgagaggagggtcggcct"+
				"gggaggaccccacagggaaggggtgagcctggcccgggcaggtgttcgct"+
				"gcgtgggtgggcggaggagttctagagccggccccttgtctctgcagAAC"+
				"TTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCC"+
				"ATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCT"+
				"ACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGG"+
				"ACCCACCAGGAGACCAGgtgagcatgagacctgctgtccactcccactcc"+
				"ctccttcccacagcctccccagacctctctcccctcatcctggcttcccc"+
				"tctgtctgcagGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGA"+
				"GAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCAT"+
				"CACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATT"+
				"CACCCaacaaaactgtgtcttatctgccaggaaagaccagcctcactcct"+
				"gggaactgtctggcaggtaggctgggccccccagtgctgttagaataaaa"+
				"agcct").toUpperCase();
		String ref2 = "AAAAAGAACCAGGAGGGCACATGGGCATGGGGAGTGATGAACCAGAGAAAGCTGCTGTCTTTCTGGGCAAGTGCCAAGCAACGGATCACCCTTGACCCCTAGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGGTGAGAGGAGGGTCGGCCTGGGAGGACCCCACAGGGAAGGGGTGAGCCTGGCCCGGGCAGGTGTTCGCTGCGTGGGTGGGCGGAGGAGTTCTAGAGCCGGCCCCTTGTCTCTGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAGCATGAGACCTGCTGTCCACTCCCACTCCCTCCTTCCCACAGCCTCCCCAGACCTCTCTCCCCTCATCCTGGCTTCCCCTCTGTCTGCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCAACAAAACTGTGTCTTATCTGCCAGGAAAGACCAGCCTCACTCCTGGGAACTGTCTGGCAGGTAGGCTGGGCCCCCCAGTGCTGTTAGAATAAAAAGCCTCGTGCCGGAAGCCTTCCTGTTTGGTCGTGGTGTGTTTGAGGTGATGGTAATGGGTCACCCGTCTCTCCTGCTCACGGCTCTGTCTCTCTTCCTCCTGCCTCCCACTCACCCCTGCCACCGTCCGCCCCTCTGTGTCCCTGATCGCGAGAGATTCTGTCCCATTTTCCTGCCACCCCCGAGCCCCTGCCCTCCTTGGCTGCTTCTTTAAGTCTTTTTGGTTATTGATTTAGTTGTTTAAACTATTTTATTTATTTATTAGAGACAGGGTCTCGCACTGTAACCCAGGCTGG";
//		String ref2 = "CAAGACCAGCCTGGCCAACATGGTGAAACCCCATCTCTACTAAAAATACAAAAACAAAATTAGCCAGGCATGGTGGTGGACACCTGTAATCCCAGCTACTCAGGAGGCCAAGGCAAGAAAATCACTTGAACCCAGGAGATGGAGGTTGCAGTGAGTCAAGATCGCACCACTGCACTCCAGCCTGGGTGACAGAGTGAGACTGTCTCAAAAAGAACCAGGAGGGCACATGGGCATGGGGAGTGATGAACCAGAGAAAGCTGCTGTCTTTCTGGGCAAGTGCCAAGCAACGGATCACCCTTGACCCCTAGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGGTGAGAGGAGGGTCGGCCTGGGAGGACCCCACAGGGAAGGGGTGAGCCTGGCCCGGGCAGGTGTTCGCTGCGTGGGTGGGCGGAGGAGTTCTAGAGCCGGCCCCTTGTCTCTGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAGCATGAGACCTGCTGTCCACTCCCACTCCCTCCTTCCCACAGCCTCCCCAGACCTCTCTCCCCTCATCCTGGCTTCCCCTCTGTCTGCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCAACAAAACTGTGTCTTATCTGCCAGGAAAGACCAGCCTCACTCCTGGGAACTGTCTGGCAGGTAGGCTGGGCCCCCCAGTGCTGTTAGAATAAAAAGCCTCGTGCCGGAAGCCTTCCTGTTTGGTCGTGGTGTGTTTGAGGTGATGGTAATGGGTCACCCGTCTCTCCTGCTCACGGCTCTGTCTCTCTTCCTCCTGCCTCCCACTCACCCCTGCCACCGTCCGCCCCTCTGTGTCCCTGATCGCGAGAGATTCTGTCCCATTTTCCTGCCACCCCCGAGCCCCTGCCCTCCTTGGCTGCTTCTTTAAGTCTTTTTGGTTATTGATTTAGTTGTTTAAACTATTTTATTTATTTATTAGAGACAGGGTCTCGCACTGTAACCCAGGCTGGAGTGCAGTGGTGCGGTCTCAGCTCACTGCAACCTCTGCCTCCCAGGTTCAAGGGATTCTCCTGCCTCAGCCTCCCAAGTAACTGGGATTACAGGTG";
		String seq = "TGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCG";
		
		nm = new SmithWatermanGotoh(ref2, seq, 4, -4, 4, 1);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
		if ( ! results[0].equals("GGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCG-AAAAAACTTTCAGGCCCTGTTGGAGGAGCAGGTGAGAGGAGGGTCGGCCTGGGAGGACCCCACAGGGAAGGGGTGAGCCTGGCCCGGGCAGGTGTTCGCTGCGTGGGTGGGCGGAGGAGTTCTAGAGCCGGCCCCTTGTCTCTGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAGCATGAGACCTGCTGTCCACTCCCACTCCCTCCTTCCCACAGCCTCCCCAGACCTCTCTCCCCTCATCCTGGCTTCCCCTCTGTCTGCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[1].equals("|||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||                                                                                                                    |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||                                                                                              |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[2].equals("GGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAAACTTTCAGGCCCTGTTGGAGGA--------------------------------------------------------------------------------------------------------------------GCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGAC----------------------------------------------------------------------------------------------CAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
//		for (String s : results) {
//			System.out.println(s);
//		}
		nm = new SmithWatermanGotoh(ref2, seq, 5, -1, 1, 1);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
		if ( ! results[0].equals("TAGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGGTGAGAGGAGGGTCGGCCTGGGAGGACCCCACAGGGAAGGGGTGAGCCTGGCCCGGGCAGGTGTTCGCTGCGTGGGTGGGCGGAGGAGTTCTAGAGCCGGCCCCTTGTCTCTGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAGCATGAGACCTGCTGTCCACTCCCACTCCCTCCTTCCCACAGCCTCCCCAGACCTCTCTCCCCTCATCCTGGCTTCCCCTCTGTCTGCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[1].equals("| ||||||||||||||||||||||||||||||||||||||||||||||||||  ||||     |           |  | |   |          |     |            |            |          |||       | ||  ||   |       ||||     ||                 ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||  | ||     | |       |     |      |                          |       ||              |                  |  |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[2].equals("T-GGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCG--AAAA-----A-----------A--A-C---T----------T-----T------------C------------A----------GGC-------C-CT--GT---T-------GGAG-----GA-----------------GCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGA--C-CC-----A-C-------C-----A------G--------------------------G-------AG--------------A------------------C--CAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
//		for (String s : results) {
//			System.out.println(s);
//		}
		nm = new SmithWatermanGotoh(ref, seq, 5, -1, 1, 2);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
		if ( ! results[0].equals("TAGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGGTGAGAGGAGGGTCGGCCTGGGAGGACCCCACAGGGAAGGGGTGAGCCTGGCCCGGGCAGGTGTTCGCTGCGTGGGTGGGCGGAGGAGTTCTAGAGCCGGCCCCTTGTCTCTGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAGCATGAGACCTGCTGTCCACTCCCACTCCCTCCTTCCCACAGCCTCCCCAGACCTCTCTCCCCTCATCCTGGCTTCCCCTCTGTCTGCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[1].equals("| ||||||||||||||||||||||||||||||||||||||||||||||||||  ||||     |           |  | |   |          |     |            |            |          |||       | ||  ||   |       ||||     ||                 ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||  | ||     | |       |     |      |                          |       ||              |                  |  |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[2].equals("T-GGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCG--AAAA-----A-----------A--A-C---T----------T-----T------------C------------A----------GGC-------C-CT--GT---T-------GGAG-----GA-----------------GCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGA--C-CC-----A-C-------C-----A------G--------------------------G-------AG--------------A------------------C--CAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
//		for (String s : results) {
//			System.out.println(s);
//		}
		String seq5 = "GCCTATTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGTCGGCAGCCGGCGGCCGTGGCCGGGCCGCGAGGTCTGGGACGGGGGGCG";
		String ref5 = "ATCCTCTGGGTCAGTTTCTGTGGAATAAACTGTAAAGGCAAACGGTGGGACGCCCGAACCCGCCCGGCAGCCACGGTGGGACGCCCGAACCCGCCCGGCAGCCACGGTGGGACGCCCGAACCCGCCCGGCAGCCGCGCTGCGACCCCGCCTTCCCGCCCGCGTCGCTCCGCGCGGGGCCCGCGCCCTCACCGTCTCCCAGCGGATGCCCTGGACGGCCCTCCACTGCGAGGGCACGGACGCCACACCCAGGGTCTCGTCCTGCAGCGGCCCTGGCCGCCCCGGCCCCGGGCCGCTCGCAACCCCGACTGTCGCCAGCGCCGCAACACCAGCGCTTCCCGCCTGGCTGAGTGGCCCCGGCTGCGCGCGGGTTGCCATGGAGACGGTTCCGCCCTCTCGTGCGGACGCACTCAGGCGCGACCTCCGCCCCTACGCCGCCATGAGCGGAAAACGGGGAATGTGAGGCTGACGGCGCCATGTTTGAATTGGTCGCAGCGCCTCCTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGGCGGCAGCCTGCGGCCGTGGCCGGCCCGCGAGGTCTGGGCCTGGGAGCGCAGATCTGGCTGAGCAGCTGATTCTTCCAGCAGATCCGAGAACCGCGCCACTCGAACAGGCTGTCACCTCGAAGCCAAGTGATCTCCCTTTAATCCTCAGTCTCTGTCTCAATAGAATGATGATGAAAAATATTCCTACGGAGATCCTAAATGTGGTTAAAAAGCTGGGAGGCCCAAATGAGGTCTTCTACAGAAACATGCTTATTAAATCGAAATTAAAAGCAAATAACTAAATAATGTGCCTACTGTGTGTCAGGCAAAGAGCCCAGAACGTCTCTTTCTTATTAAGTTCTCACCCTGAGGAAAGAATTGCTTTTTATACTATATTATGTCGTTCTGCCATTCCGCGCGCCCCACCGGAATTAAAAAAACTGGAGATTCTGACCTGAGGACTAAAAAATAAAAATAAAGAATAAACAAATAGGCCAGGTGCGGTGGCTCACACCTGTAATCCCGGTACTTTGAGAGGCCAAGGCAGGAGGATCGCTTGAAGCCAGGAGTTCGAA";
		nm = new SmithWatermanGotoh(ref5, seq5, 2, -5, 5, 1);
//			SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
//		for (String s : results) {
//			System.out.println(s);
//		}
		if ( ! results[0].equals("TGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGGCGGCAGCCTGCGGCCGTGGCCGGCCCGCGAGGTCTGGG")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[1].equals("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||.||||||||||||||.||||||||||||||")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[2].equals("TGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGTCGGCAGCCGGCGGCCGTGGCCGGGCCGCGAGGTCTGGG")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		nm = new SmithWatermanGotoh(ref5, seq5, 5, -4, 16, 4);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
//		for (String s : results) {
//			System.out.println(s);
//		}
		if ( ! results[0].equals("GCCTCCTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGGCGGCAGCCTGCGGCCGTGGCCGGCCCGCGAGGTCTGGGCCTGGGAGCG")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[1].equals("||||..||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||.||||||||||||||.||||||||||||||.|.|||.|||")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[2].equals("GCCTATTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGTCGGCAGCCGGCGGCCGTGGCCGGGCCGCGAGGTCTGGGACGGGGGGCG")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		
		String r = "ACCATGGTATTTGTGTATCTAAACATACCTAAACATAGAAAAGGTACAGTAAAAATACAGTATTATAATCTTATGGGACCACCATCATATATGTGGTCGTCATTGACTGAGACATCATTAATGTGGTGCATGACTACTCCAATCAGTCCAGGAACAAATTAAAAAGATAAGGAGAAAAGTCAGTGCTTTGAGGCTCCACAACACCTTGCTGTGTCCATTTAGAGCAATTTACAGCTGTTGCTGTAATTAATGAAGCTATCTCCTCCCAGGCAAAGCCTTTGGTTGTGTTGGAGGGTACATCGCCAGCACGAGTTCTCTGATTGACACCGTACGGTCCTATGCTGCTGGCTTCATCTTCACCACCTCTCTGCCACCCATGCTGCTGGCTGGAGCCCTGGAGTCTGTGCGGATCCTGAAGAGCGCTGAGGGACGGGTGCTTCGCCGCCAGCACCAGCGCAACGTCAAACTCATGAGACAGATGCTAATGGATGCCGGCCTCCCTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGGGTAATGGCCTGTCTCTGATTGGACTTGCCGTGGGGTGTGCCTCTACACATGATGTACGGATGTTCTGCTTCATACCTTCCTGAAGTTGGGCTTGAGCGGGGTGACTGCCAGGGCAGGGGTTGTAGCCAGCCACCCTCTGTCATGTTTCCGCCATTGGCTGACTTCACCAAGAGAAGAAAGCCTTTGAACCCAGCAGGCTGGGGCAGAAGTTCCCTCTCCGGAGCACTGACCTTAACAGGGTAAACACAGAGCTTGTATCTAGAAAGCTCCAGAAGCCTGAGCTTGGCCAGCTTTGAAGTATGGCTTTCTACTTAGTAAATTTCAAAATAGGTTTTGCCCTTCCCACTACAAATGGTAGCACTGTTGATGTCACAGTTGAATTAGTGTAATGAATACAGCTAGTATAACTGAATCTAGATTATACATCGTGGGTATGAGAGTCTGCTGGTACGAACAGAACCAGTGTTTTCTGATTAAAAATGTATTTCTTTTTAATAAGGTTTTGGTTCCCTGGTGTTCACGAAACAACACTGGCTTCTTTTAAATGACAGGTGTTTGGGCAGCGCTTTCCCTCTGCCCCAAGCTTGCATGTGTTGCTACAGTCTGGTCTTGAGCCTGAGCGTTGTGGGGACTGCGTTCGTTAGGATCTCTGCTAAGAGGTAGTCCTTCCTGTTGTGACCTTACCTTCTGCTCTCATTGAACTTAGGTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACAC";
		String s = "CTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGGGTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACACCCCAGATGATGAACTACTTCCTTGGTGAGTACCTGGGGAGCTGCTGGTGCCTCACTGAGGAGTTGCATAAAGCTGTCTTTGCAGTGTTTATAATTGAAGCCCTTCGGAGGGCTTCAGATTTGTTTCTTCTTCTTTTTTTATTTTTTTTTTTTTTTCCATTATTTTCGTTCTTTTTTCCCTTCCTTGGTTTTTTTTGCCCAATCCCT";
		nm = new SmithWatermanGotoh(r, s, 4, -4, 4, 1);
		results = nm.traceback();
//		for (String s1 : results) {
//			System.out.println(s1);
//		}
		if ( ! results[0].equals("CTGTTGTGACCTTA----CCTTCTGCTC---TCAT-----TGAACTTAGGTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACAC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[1].equals("|||||||  ||  |    ||  |.|| |   ||||     ||  |  .||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		if ( ! results[2].equals("CTGTTGT--CC--ACTGCCC--CAGC-CACATCATCCCTGTG--C--GGGTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACAC")) {
			throw new IllegalArgumentException("FAIL!!!");
		}
		
		r = "TTATACATTCCTAAATAATATAATTTGTCTATTTTTGGCTTTATATAGATGGAATCAGATGATACGTGCTTTTTGCTCTTATCCTTATGTTTGTTAGATTCATATGGCCATATGTATGTTCTCTCTCACTCTTTCTAAGACATACCCACACACTCACTCACAAAACAGAAGAAAATGAAAGGTTACATTTTTCCAAACAAGAAATCCAAAACATGTTAGCAATATTACTTCAAAGACTCAAGTAATCCAAGTTCAGAGACTGAGACAGTCCATTCAGTGAGCCAAC";
		s = "CATATGGCCATATGTAGAGATGGGGTTTCACCTTGTTAGCCAGGATGGTCTCGATCTCCTGACCTCATGATCCACCCGCCTCGGCC";
		nm = new SmithWatermanGotoh(r, s, 5, -4, 16, 4);	
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		
		r = "CCCACACACACACACCCCCACACACACCCCCACACCCACACACCCACACACCACACACACACACCCCCACACCCACCCCACACACCCACACACCCACACACCACACACACACCCACACACACCCATACCCACACCCACACACACCCACACACCCACACCCACACCCCACACACACTCACACCCCCCACACACCCACACACCCACACACACACCCACACACCCCCACACACATAACCACACACCCAGACACACCTCCCCACACA";
		s = "CCCACACACACACCCACACACCCNCACANNNANAANAAGAAGAGATAGAGAGAAAAGTCAAAA";
		nm = new SmithWatermanGotoh(r, s,  4, -4, 4, 1);	
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		/*
		 * about to sw ref: TGATTCCCAGTATTCTGATGATCACCACAATTTCACTTTGGCCAAACTCTTTCTTCTTTCCCTATCACCACTCTCTCTCTTCCTCTCTCTCTTTCTCAATCTCTCTCTCTCACACACACACACACACCCACACCCACACACAGACACACCACACACACACACACACACACACACACTCTGACTGGTTGTTTGCATGTTTGCCTTATAAAAAGGTTAATCAACAGAATGGCAGGGAGTCTCCTCTG, fragString: TCTCTCTCTCTCTCTCTCTCACACACACACACACACCCACACCCC for cp: chr3:143908281
		 */
		r = "TGATTCCCAGTATTCTGATGATCACCACAATTTCACTTTGGCCAAACTCTTTCTTCTTTCCCTATCACCACTCTCTCTCTTCCTCTCTCTCTTTCTCAATCTCTCTCTCTCACACACACACACACACCCACACCCACACACAGACACACCACACACACACACACACACACACACACTCTGACTGGTTGTTTGCATGTTTGCCTTATAAAAAGGTTAATCAACAGAATGGCAGGGAGTCTCCTCTG,,";
		s = "TCTCTCTCTCTCTCTCTCTCACACACACACACACACCCACACCCC";
		nm = new SmithWatermanGotoh(r, s,  4, -4, 4, 0);	
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		nm = new SmithWatermanGotoh(r, s, 5, -4, 16, 4);	
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		
		/*
		 * about to sw ref: AGCTGCTATTTATTCACTCAAAATATAAATATATTATACACCTTTTATGTAAAAGGCAGTCTAGCTCTCAAGAAGTGTGCAAGCTTGGCAGAGCCAATGAGATACACGTTCAACTAGGACAGAAATACTATCTTAACAAATGTCATTCCCAGAAATAATCAACACAGGTGAAAGAATTAGAGATGGGGAAAAGTCAAAAAATGAGAGAGGGAAAGGTTGCAGTGTGGAAAATAGCATTTAAATTCTAACCAAACTAGAATCAGACATATAGGAAAATCTAAAATAAAATCTGGGAGCCTTGAAGCCGGAGGAAGAATTAAGGAAATCTGTGTTCGGGAGGGAAAAGCAGAAGGGGCCTTCAAGTACAACTGAATTAAATCAAGATGGACTGCCAGTTCTAGAAAAAGACAAGTTTCTCCATTCCCCGTAAATGCTCAGGAGTAAACCCCAGTAGTCACAGCTGGGCCAGTCCCAACTTATACTCTGGGCAATCGAAACTCATTTGCCAAGCAGAGACTTGGACCATACTGCCTAGAACATGCCTACCATTCTTTCTTTATTCTTTTGAAAGAGTACTGCCACTCAAGTGACTTTTGCAATTGAGAGTCTGATTATCATCTCTATGCTGAAAATCCTTCAGATGTTTTCTCCATTAGGAATAATCCTCATCACCAGCTCTAAAGGCCTGCACAGTTGGCCCTTCCCATCCTCTTTAGCCTGTCTTCCTCATTATTTTTCTCCAGCATTTGACCTTCCCTACGTGCTGCCATGCTGCAACCTTGAAGACTTTTGCATGCTCTGACCTCCCTCTGAAAGTGCACCTTCCTCTACTATTTAACTGCTTAATTTCTTCCTCAACCCTAAACTTCTCCTTGATAGCACTTAACACAGTTGAAGTTTTGTATTTTTGTGTGGTTATTGATCATCTCCCCCACTAAAATGTAAGCTTTTAGAAGGGAGAAACCATCTCAGCTTTTGCACGTAGTGGGCTTAAAACATCTTTTGTGAATGGATGCAGACATTTAGGTGCTCATTAGGAAAGCTGAACAGTATGGTGGTTAATTTCAATATCATTTCCACTCATCTACTTACCTGTGGATCCCTCTGTAGTAAGAGTACATTAAATAAAGAAGTTTCTGTTTCTCATGAAAAAAGCCAAGCTTGTATGGTGAGCTGGGCAAAATCTTCAAGGGCACTTCCATCTTTTTTGCTGTGCGGCTATAAGCCCAGTGAAAAATCAGGGGCTTTATTACTAAAGAAAAAGGAGAGGACAGATATCTGGGCAATTTGCAGGCTTTGCAGTAATGCTTTATAAAGGTATTGTAGTAAGGATTAAATATTATATATAAGACTCTCTTAGCATATAGTCAGCACTCAATTAACTATGGCTGTTATTCTCTCTTCAACATATAGAGAATAAATTACTTACCTGAAAGACCTTCCTGCCCTTGGTTCTTTGGGCTTTTTTCAGCTCTTCATTTCTTTGTCTCTTTGTGCATTCATAGGTCAGATTACTTGGTTTACTATGTTTGTATACAGTTTACTGTAAATGTAGGGAGTACATTTCTGTTTTCCTTTTGATAGTTCATGGAGTAGTAGCCCCTCTCTCACGCCATTGGGACTTTGTTCTTCTTAGCCTCTTCATAATTCCTGTTTATGTAAAGTCAGCTTCCCCACAGCCAAGTTAAACTCTCTTAATCAAGTAGAGCCACGGATTTATTTTCTTCTCCTTGTGGGTGGTGCCATTTGAGTGTTGATCTGTCTTTCATAAAGCTGTACCTTGACATGTAAGGGGTCAGTAGTAGGGAGTGAGGAGGGATGTGAAGAGAGAATCAGTGAAAATGAAGTAGGCCAGTGCATGCTTATCTCTGACAGTTCTCTTTATTTCCTGTGAAGGCGATTGACCGAGGCCTGGCTTTTGAACTTGTCTATAGCCCTGCTATCAAAGACTCCACAATGAGAAGGTATACAATTTCCAGTGCCCTCAATTTGATGCAAATCTGCAAAGGAAAGGTATGGTTCTATAATTTTAGGATGTTTTTCGCATCTGGCAGTTTATTATTAGTAGTACACTGGAATTGTCAAACTGACTCCTTTTCCTTAAAGCAGAATGTTCTCATAGCATAGGAACAAATTCTGCCATTTCAGTGAGCTCTGCAAATTTTAAGGGAAAGAAAAATACCAAGGGAAGAATAATGTAAGGGAGAAAAAGATGTGTTGGAAGCCATATCTTTTGATAAAAATTTGCAATTTACATACTGTGAAAGAAAAGTCTAGAATAACAATGGTCCTCACTGAGTTGAATTAAGATATGTGACCTTTTCATCTTGTTTTTTAAACTTAATTCACCTTTACTAAGATCTGTTAGGAAAACATATTTTGTTTTTTATTACAGATTGCCTTTAGTTTGACATGAAGTCTTTTCATTTTTTTCACCTTAATTATTTACCTTATCTTTAAAAGAAGTTGGTATTTGAGCCTTAAATGTTAAAATATGATTACTGGCTATTTATATTATGAAACTCCTTTTTTTACATTTATCTAAGATTAATATTAGTCACTTTCTATTTTGTAGAATGTAATTATATCTAGTGCTGCAGAAAGGGTAAGTCTAGCATGAAGTACTTTGTTTTCATCTTTCAGGTTATAAAACTTTTCAGTTTCTAGGGGATAAATTAAAATAGTAAATGAATATTTGTTGCATTTTTTTCTATTTTAACTGCATTTGCTCATTGTAATTGCTAAATAGTAAAATTTATCCCCACTTTCTTTAGGCTTCAACTTTTGTTTATATAATCAGTTTTTCTTTTATACTGTTACAAAATGCCTTTCAAGCTTCTCTTATATTTTCAAAAGCTATGAAACCCATCAAAATTATAAGGACTTAAATTTATGTAATTTTTTTATTTTTGGATACTTAAATTTTCTATTAATTTGCTATAAGTCATTGAAATGTTATAAATAATTTTCTCATATCTACTCTTTGTTCATTTTATTTCAGCCTTTAGAAATAAGAGGGCCATATGACGTGGCAAATCTGTATCCTTTTCTGAGTAAAAAGTTGTTGACATTGTCTTTCAGGAAATTTTGAATTTGAAGTCGTATTATAGTTGAACATGTATTTTTCTCAACCTTTTACTGTAAGTTTACCAATTAATAACTTCAAAAATAAATTATATAACATTATAGAGAGCTTTATGCTTCTGTTTTTTAATTAATCATGAGATAGAAAAGAGGGAATGAGGGACTTCATATGATAAAAGCTTAGAGTTAAATGAGTCTTTTCTCAGATGCATGTTCCAGTTGGACTAAGAACTGCTGTTGAATTAATTAGCCTCTCAAGATGACCAGGTTAGCTGGCACTTTCTGTTATGTTTGTGTTAGTCTCGTGTCCTTGGGTGACTCTGGGTTGAAATCTTTAATGCTCCCCCAAGAGGCTTGCTGTTTGGGCTCTCTGAAAGTGACGCCAAGGCTGCGGTGTCCACCAACTGCCGAGCAGCGCTTCTCCATGGAGGTAAGCAAGTTCTTCCAGTACAAACTGAATGGTCATGTTAAAAGCAAGTCATTTTTACAGCCATACCTATTTTTATTGCCCTACCCTACAGAGTTCCTTTGGGATCATCTTTTCTCAACCTGTTCTAAACACAGTCATCAGTGTTTTCTCACCCTCACTTCAGGCTTTACTTTCTAAAGTTTGTAGAATGAATATAGTTTTCATTACAGAAATAAGTATTGAATAGTTCTTCCAGCTATACATGTGTGCTCCTGCCGACATTTTGATAGGCTTCTAATCTCCCTTCCTTCTTCTCCCTCACCAGAAGGCCATGCTTTCCCTGTTGAAAATCTCTGTTTTAGAGAACGGATATGTTAAAAATATGTGTTAAACTATTTATTAGGAAAACAAAACCAGCAAATACATTTTTAAAAATCCAACTAACAAAACATAGACTGTAACTTTTACATTTATTATAACTCAGAATGTTTCTTTTCTCATGTTTGCTTACATGGTCAAGTTGTGTCCAGACTGTACCTTTTTTATTTTTTTTTCTTTTTCTAAATAGAGACAGAGTCTCACTATGTTGCCCAGGCTGGTCTTGAACTCCTGGGCTCAAGCAATCCTCCTACCTCAGCCTCCCAAAGTGCTAGGATTACAGACATGAGCCACCATGCCCGGCCAGACTGTACATTTTTTACTCACATTTTGTATATGTACCTTTTCACTGACTGAGTTTCCTGCTCATCAGATGTTAAAAATTGAAAGTATGCTGAAACTTTGGAAAATGAAAGTGAAATTTTATTAAAACAATTTAAGCTTTTGATCACTGTTTTGTCTTCGTTATCTTTATAATTCTTAGGACTAGTTGTAGTGGTTAAGAAGCAATTTCATTGATATCACCCAAGTGAATTTTAAACTTCAAAGTAGATTGCTACATATAGACTGAGTTATTTGTCATATGAATAGCTAGGTTATTGAGGGCTTGTGTGTTGGGCTTTATTGATCTGATTTCCACTTGAGAGAACTGCTTATGTATTGGCTTGCCGTAAATTAGATGATTTTGCTTTTAGTTTCAACATTTTTAGTTTGAGATTTTTGTATTATGTAGCCTAGAACTCATCACATAAGCTTTGGGAGTGGTCAAATTAATGGAGATTTTGAATAATGATTTTCTTGAAATTAGGGAACAGTTGGTTCACACTTTGATAGTTCTGTCATTATTTTGATCTATTTTAAATAAACATTCATCAGGATTTTGAGTAACAGTTGGGGTTCCAAAATGGACTAAACAAGAAACAGAAACTAAAATATAAATACATACATAATTTTATATATAGGAAAATAGGAAGTCCTGCATTTGACTTTAAGAACTCAGCTATTCAGCATATAACACATAATATGTAAAGTATTTTTTTTTAGAATTTCTTTTTGTGGGTACATAGTATGTGTATATATTTATGGGGTACTGAGATATTTTGATATAGGCATGCAATGCATAATAATTACATCATGGAAAATGGGGTGTCCATCCCCTCAAGCATATGTCTTGTGTTATAAACAATCCAATTATGCTTTTTTAGTTATTTTTAAATATACAATTAAATTGTATTGACTATGGTCACCCTCTTGTGCTATCATATACTAGACCTTGTTCATTCTATTTTTTTTGTACCCATTAACCATCTTCACCTCCCTCTCAGCCCCCCACTACCCTTCCCAGCCTCTGGTAACCATCCTTCTACTTTCTGTCTCCAGGAGTTCAGTTGCTTTGATTTTTAGATCCCACAAACAAGTGAGAACATGTGATGTTTGTCTTTCTGTGTCTAAAATCAGGCTTTTATAGAAGCTAAACTTAAAGAAGACTTGTGAACTTTGGGTGGCATTACCATTCACCCCAGTGTACCCAGGACAGTTCTAGTTTATGCCTGCTGTTTCTGTGACCTTATTAGCACTTCTATTCCCTGAAGAGAGTCCTAAACTAGATGATAAAATATGGTTACCCTGGTTTTCGTTGCCATTGGGATCAGTATGAGACGGTAGTATGATGTGTTCTTAAGATTGCTAATTCAGCCTAGGTAATATTAGAAGAAATAAAGGATTCTGAGACAAGGAAATTCAAAGCCTCGTTATTATACTCTCATTGATGAAATCTCAGCCGGCATCTGTATCATTCTGAGTGCCACATTTAAGAAGGACAGCAGTGCACTGAAACACTATTGGAAAATGTCTACTCAGTCCTTAAAGGGTCTGGAAGCCTCATGATGAGAGGAATCACGAGGGTGGTGCAGACATGGATACCTGGCAAGCAGAAGAGTTGGGACACATGGTAGTTAGCTTTCAACTTGGAAGGCTTGCCACAGGAAATCAGAATTAGGTTCTTTGGGGGTCCAGAAATCTAGAAACCAATGAATAGCAATTCTGAGGAGGTTGTGGGGAAAGGACAGAGGAAAGAGGCCCAAGGTCAGGGTCTTTGGCCTGTGATTGATAAAGTTCATGCCCTCAATCAGCCCCTGGACCAAAATGAACCTGTTGGGGACACCTGCCTTAGCCCCTTGTGAACAAAATTATTCACTCGTAGATAATCCTTTAGATTTTCCTTGGCAAGATGATTAAAGGAAATACTATATGTGAAAACATTTTATAAACTAAAGCATACACAAATGTAACGTCTTGCTGTTACCAAGAAATGCATTTATTTTTAAAAGACACTAAAGAAATTAATGCATTTTGTTAAAAAGTGCTGATTTTCATAATGTTTATATTTCAGTTGATTTTTAGACTATGGGTACAGAAT, fragString: AAAGAATGTAATTATATCTAGTGCTGCAGAAAGGCCTTTAGAAATAAGAGGGCCATATGACGTGGCAAATCTAGGCTTGCTGTTTGGGCTCTCTGAAAGTGACGCCAAGGCTGCGGTGTCCACCAACTGCCGAGCAGCGCTTCTCCATGGAGAAACTAGAAAAACTGCTTTTGGAATTATCTCTACAGTGAAGAAACCTCGGCCATCAGAAGGAGATGAAGATTGTCTTCCAGCTTCCAAGAAAGCCAAGTGTGAGGGCTGAAAAGAATGCCCCAGTCTCTGTCAGCACC
		 */
		r = "AGCTGCTATTTATTCACTCAAAATATAAATATATTATACACCTTTTATGTAAAAGGCAGTCTAGCTCTCAAGAAGTGTGCAAGCTTGGCAGAGCCAATGAGATACACGTTCAACTAGGACAGAAATACTATCTTAACAAATGTCATTCCCAGAAATAATCAACACAGGTGAAAGAATTAGAGATGGGGAAAAGTCAAAAAATGAGAGAGGGAAAGGTTGCAGTGTGGAAAATAGCATTTAAATTCTAACCAAACTAGAATCAGACATATAGGAAAATCTAAAATAAAATCTGGGAGCCTTGAAGCCGGAGGAAGAATTAAGGAAATCTGTGTTCGGGAGGGAAAAGCAGAAGGGGCCTTCAAGTACAACTGAATTAAATCAAGATGGACTGCCAGTTCTAGAAAAAGACAAGTTTCTCCATTCCCCGTAAATGCTCAGGAGTAAACCCCAGTAGTCACAGCTGGGCCAGTCCCAACTTATACTCTGGGCAATCGAAACTCATTTGCCAAGCAGAGACTTGGACCATACTGCCTAGAACATGCCTACCATTCTTTCTTTATTCTTTTGAAAGAGTACTGCCACTCAAGTGACTTTTGCAATTGAGAGTCTGATTATCATCTCTATGCTGAAAATCCTTCAGATGTTTTCTCCATTAGGAATAATCCTCATCACCAGCTCTAAAGGCCTGCACAGTTGGCCCTTCCCATCCTCTTTAGCCTGTCTTCCTCATTATTTTTCTCCAGCATTTGACCTTCCCTACGTGCTGCCATGCTGCAACCTTGAAGACTTTTGCATGCTCTGACCTCCCTCTGAAAGTGCACCTTCCTCTACTATTTAACTGCTTAATTTCTTCCTCAACCCTAAACTTCTCCTTGATAGCACTTAACACAGTTGAAGTTTTGTATTTTTGTGTGGTTATTGATCATCTCCCCCACTAAAATGTAAGCTTTTAGAAGGGAGAAACCATCTCAGCTTTTGCACGTAGTGGGCTTAAAACATCTTTTGTGAATGGATGCAGACATTTAGGTGCTCATTAGGAAAGCTGAACAGTATGGTGGTTAATTTCAATATCATTTCCACTCATCTACTTACCTGTGGATCCCTCTGTAGTAAGAGTACATTAAATAAAGAAGTTTCTGTTTCTCATGAAAAAAGCCAAGCTTGTATGGTGAGCTGGGCAAAATCTTCAAGGGCACTTCCATCTTTTTTGCTGTGCGGCTATAAGCCCAGTGAAAAATCAGGGGCTTTATTACTAAAGAAAAAGGAGAGGACAGATATCTGGGCAATTTGCAGGCTTTGCAGTAATGCTTTATAAAGGTATTGTAGTAAGGATTAAATATTATATATAAGACTCTCTTAGCATATAGTCAGCACTCAATTAACTATGGCTGTTATTCTCTCTTCAACATATAGAGAATAAATTACTTACCTGAAAGACCTTCCTGCCCTTGGTTCTTTGGGCTTTTTTCAGCTCTTCATTTCTTTGTCTCTTTGTGCATTCATAGGTCAGATTACTTGGTTTACTATGTTTGTATACAGTTTACTGTAAATGTAGGGAGTACATTTCTGTTTTCCTTTTGATAGTTCATGGAGTAGTAGCCCCTCTCTCACGCCATTGGGACTTTGTTCTTCTTAGCCTCTTCATAATTCCTGTTTATGTAAAGTCAGCTTCCCCACAGCCAAGTTAAACTCTCTTAATCAAGTAGAGCCACGGATTTATTTTCTTCTCCTTGTGGGTGGTGCCATTTGAGTGTTGATCTGTCTTTCATAAAGCTGTACCTTGACATGTAAGGGGTCAGTAGTAGGGAGTGAGGAGGGATGTGAAGAGAGAATCAGTGAAAATGAAGTAGGCCAGTGCATGCTTATCTCTGACAGTTCTCTTTATTTCCTGTGAAGGCGATTGACCGAGGCCTGGCTTTTGAACTTGTCTATAGCCCTGCTATCAAAGACTCCACAATGAGAAGGTATACAATTTCCAGTGCCCTCAATTTGATGCAAATCTGCAAAGGAAAGGTATGGTTCTATAATTTTAGGATGTTTTTCGCATCTGGCAGTTTATTATTAGTAGTACACTGGAATTGTCAAACTGACTCCTTTTCCTTAAAGCAGAATGTTCTCATAGCATAGGAACAAATTCTGCCATTTCAGTGAGCTCTGCAAATTTTAAGGGAAAGAAAAATACCAAGGGAAGAATAATGTAAGGGAGAAAAAGATGTGTTGGAAGCCATATCTTTTGATAAAAATTTGCAATTTACATACTGTGAAAGAAAAGTCTAGAATAACAATGGTCCTCACTGAGTTGAATTAAGATATGTGACCTTTTCATCTTGTTTTTTAAACTTAATTCACCTTTACTAAGATCTGTTAGGAAAACATATTTTGTTTTTTATTACAGATTGCCTTTAGTTTGACATGAAGTCTTTTCATTTTTTTCACCTTAATTATTTACCTTATCTTTAAAAGAAGTTGGTATTTGAGCCTTAAATGTTAAAATATGATTACTGGCTATTTATATTATGAAACTCCTTTTTTTACATTTATCTAAGATTAATATTAGTCACTTTCTATTTTGTAGAATGTAATTATATCTAGTGCTGCAGAAAGGGTAAGTCTAGCATGAAGTACTTTGTTTTCATCTTTCAGGTTATAAAACTTTTCAGTTTCTAGGGGATAAATTAAAATAGTAAATGAATATTTGTTGCATTTTTTTCTATTTTAACTGCATTTGCTCATTGTAATTGCTAAATAGTAAAATTTATCCCCACTTTCTTTAGGCTTCAACTTTTGTTTATATAATCAGTTTTTCTTTTATACTGTTACAAAATGCCTTTCAAGCTTCTCTTATATTTTCAAAAGCTATGAAACCCATCAAAATTATAAGGACTTAAATTTATGTAATTTTTTTATTTTTGGATACTTAAATTTTCTATTAATTTGCTATAAGTCATTGAAATGTTATAAATAATTTTCTCATATCTACTCTTTGTTCATTTTATTTCAGCCTTTAGAAATAAGAGGGCCATATGACGTGGCAAATCTGTATCCTTTTCTGAGTAAAAAGTTGTTGACATTGTCTTTCAGGAAATTTTGAATTTGAAGTCGTATTATAGTTGAACATGTATTTTTCTCAACCTTTTACTGTAAGTTTACCAATTAATAACTTCAAAAATAAATTATATAACATTATAGAGAGCTTTATGCTTCTGTTTTTTAATTAATCATGAGATAGAAAAGAGGGAATGAGGGACTTCATATGATAAAAGCTTAGAGTTAAATGAGTCTTTTCTCAGATGCATGTTCCAGTTGGACTAAGAACTGCTGTTGAATTAATTAGCCTCTCAAGATGACCAGGTTAGCTGGCACTTTCTGTTATGTTTGTGTTAGTCTCGTGTCCTTGGGTGACTCTGGGTTGAAATCTTTAATGCTCCCCCAAGAGGCTTGCTGTTTGGGCTCTCTGAAAGTGACGCCAAGGCTGCGGTGTCCACCAACTGCCGAGCAGCGCTTCTCCATGGAGGTAAGCAAGTTCTTCCAGTACAAACTGAATGGTCATGTTAAAAGCAAGTCATTTTTACAGCCATACCTATTTTTATTGCCCTACCCTACAGAGTTCCTTTGGGATCATCTTTTCTCAACCTGTTCTAAACACAGTCATCAGTGTTTTCTCACCCTCACTTCAGGCTTTACTTTCTAAAGTTTGTAGAATGAATATAGTTTTCATTACAGAAATAAGTATTGAATAGTTCTTCCAGCTATACATGTGTGCTCCTGCCGACATTTTGATAGGCTTCTAATCTCCCTTCCTTCTTCTCCCTCACCAGAAGGCCATGCTTTCCCTGTTGAAAATCTCTGTTTTAGAGAACGGATATGTTAAAAATATGTGTTAAACTATTTATTAGGAAAACAAAACCAGCAAATACATTTTTAAAAATCCAACTAACAAAACATAGACTGTAACTTTTACATTTATTATAACTCAGAATGTTTCTTTTCTCATGTTTGCTTACATGGTCAAGTTGTGTCCAGACTGTACCTTTTTTATTTTTTTTTCTTTTTCTAAATAGAGACAGAGTCTCACTATGTTGCCCAGGCTGGTCTTGAACTCCTGGGCTCAAGCAATCCTCCTACCTCAGCCTCCCAAAGTGCTAGGATTACAGACATGAGCCACCATGCCCGGCCAGACTGTACATTTTTTACTCACATTTTGTATATGTACCTTTTCACTGACTGAGTTTCCTGCTCATCAGATGTTAAAAATTGAAAGTATGCTGAAACTTTGGAAAATGAAAGTGAAATTTTATTAAAACAATTTAAGCTTTTGATCACTGTTTTGTCTTCGTTATCTTTATAATTCTTAGGACTAGTTGTAGTGGTTAAGAAGCAATTTCATTGATATCACCCAAGTGAATTTTAAACTTCAAAGTAGATTGCTACATATAGACTGAGTTATTTGTCATATGAATAGCTAGGTTATTGAGGGCTTGTGTGTTGGGCTTTATTGATCTGATTTCCACTTGAGAGAACTGCTTATGTATTGGCTTGCCGTAAATTAGATGATTTTGCTTTTAGTTTCAACATTTTTAGTTTGAGATTTTTGTATTATGTAGCCTAGAACTCATCACATAAGCTTTGGGAGTGGTCAAATTAATGGAGATTTTGAATAATGATTTTCTTGAAATTAGGGAACAGTTGGTTCACACTTTGATAGTTCTGTCATTATTTTGATCTATTTTAAATAAACATTCATCAGGATTTTGAGTAACAGTTGGGGTTCCAAAATGGACTAAACAAGAAACAGAAACTAAAATATAAATACATACATAATTTTATATATAGGAAAATAGGAAGTCCTGCATTTGACTTTAAGAACTCAGCTATTCAGCATATAACACATAATATGTAAAGTATTTTTTTTTAGAATTTCTTTTTGTGGGTACATAGTATGTGTATATATTTATGGGGTACTGAGATATTTTGATATAGGCATGCAATGCATAATAATTACATCATGGAAAATGGGGTGTCCATCCCCTCAAGCATATGTCTTGTGTTATAAACAATCCAATTATGCTTTTTTAGTTATTTTTAAATATACAATTAAATTGTATTGACTATGGTCACCCTCTTGTGCTATCATATACTAGACCTTGTTCATTCTATTTTTTTTGTACCCATTAACCATCTTCACCTCCCTCTCAGCCCCCCACTACCCTTCCCAGCCTCTGGTAACCATCCTTCTACTTTCTGTCTCCAGGAGTTCAGTTGCTTTGATTTTTAGATCCCACAAACAAGTGAGAACATGTGATGTTTGTCTTTCTGTGTCTAAAATCAGGCTTTTATAGAAGCTAAACTTAAAGAAGACTTGTGAACTTTGGGTGGCATTACCATTCACCCCAGTGTACCCAGGACAGTTCTAGTTTATGCCTGCTGTTTCTGTGACCTTATTAGCACTTCTATTCCCTGAAGAGAGTCCTAAACTAGATGATAAAATATGGTTACCCTGGTTTTCGTTGCCATTGGGATCAGTATGAGACGGTAGTATGATGTGTTCTTAAGATTGCTAATTCAGCCTAGGTAATATTAGAAGAAATAAAGGATTCTGAGACAAGGAAATTCAAAGCCTCGTTATTATACTCTCATTGATGAAATCTCAGCCGGCATCTGTATCATTCTGAGTGCCACATTTAAGAAGGACAGCAGTGCACTGAAACACTATTGGAAAATGTCTACTCAGTCCTTAAAGGGTCTGGAAGCCTCATGATGAGAGGAATCACGAGGGTGGTGCAGACATGGATACCTGGCAAGCAGAAGAGTTGGGACACATGGTAGTTAGCTTTCAACTTGGAAGGCTTGCCACAGGAAATCAGAATTAGGTTCTTTGGGGGTCCAGAAATCTAGAAACCAATGAATAGCAATTCTGAGGAGGTTGTGGGGAAAGGACAGAGGAAAGAGGCCCAAGGTCAGGGTCTTTGGCCTGTGATTGATAAAGTTCATGCCCTCAATCAGCCCCTGGACCAAAATGAACCTGTTGGGGACACCTGCCTTAGCCCCTTGTGAACAAAATTATTCACTCGTAGATAATCCTTTAGATTTTCCTTGGCAAGATGATTAAAGGAAATACTATATGTGAAAACATTTTATAAACTAAAGCATACACAAATGTAACGTCTTGCTGTTACCAAGAAATGCATTTATTTTTAAAAGACACTAAAGAAATTAATGCATTTTGTTAAAAAGTGCTGATTTTCATAATGTTTATATTTCAGTTGATTTTTAGACTATGGGTACAGAAT";
		s = "AAAGAATGTAATTATATCTAGTGCTGCAGAAAGGCCTTTAGAAATAAGAGGGCCATATGACGTGGCAAATCTAGGCTTGCTGTTTGGGCTCTCTGAAAGTGACGCCAAGGCTGCGGTGTCCACCAACTGCCGAGCAGCGCTTCTCCATGGAGAAACTAGAAAAACTGCTTTTGGAATTATCTCTACAGTGAAGAAACCTCGGCCATCAGAAGGAGATGAAGATTGTCTTCCAGCTTCCAAGAAAGCCAAGTGTGAGGGCTGAAAAGAATGCCCCAGTCTCTGTCAGCACC";
		nm = new SmithWatermanGotoh(r, s,  4, -4, -2, 4);	
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		nm = new SmithWatermanGotoh(r, s, 5, -4, 16, 4);	
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		
		r = "AGCCACAAAGGAAACTTGAACAAAAAGTCACAATTTGTTCTTAGAAAAATTTATTCTCGATGGATTGATAGGTGCAGCAAACCACATGGGACATGTTTACCTACGTAACAAACCTGCACGTCCTGCACGTATATTCCAGAATTTAAAAGAACAATTTATTCTTATAAAGATGTCAATTCTCCCTGACTTCATAGGCTTAACATTTTATAACAATTTTAGAGTTTTTTCCCCAATCTGGACAAGCTGAGAACTCTCAGCAAATCTTAAACGATTTGAGGAGGAGGACATTTTTTAACGAAAAGAATACAAAATTAAGAAAACGAAATTAGGTAGAAAAGCTAACATTAACTAAAAACAAGAAAATAAATTACAAATATTTAAAAGCTGACAAATATAAAACATTACCAACTTAAATTCAGAAAAATATTTTTATTAATCGACTGCTTGATACACCTGTTATATTTTTCATTTTACTAACTGAATTATCAAATCTACAAGCTTATTTGCTACTTCTATTTGAAGTTTGTGTCTTTAATTACTGCTTTTCATTAGATCTGAAATTTTTTTATAATTTGCTTTTTTTTTTTTTTTTTTGAAATGGAGTCTTGTTCTGTCACCCAGGCTAGAGTGCAATGGCATGATCTCAGCTCACTGCAACATCTGCCTCCCGGGTTCCAGCAATTCTCCTGCCTCAGCCTCCTGAGTAGCTGGGATTACAAGCGTGTGCCACCACACCGGGCTAATTTTTGTATTTTTAATAGAGATGGGGTTTCACCATATTGGCCAGGCTGGTCTCGAACTTCTGACCTCGTGATCTGCCCACTGCGGCTTCCCAAAGTGCTAGGATTACAGGCGTGAGCCACTGTGCCCAGACTATAATTTGCTGCTTGATGTCAGAATAATTTCTATTAATTTCATTGTTCATCTTTAATCACCATTGTTTCATAGTCCAGTAATTTTCTGTATTTTCTGTTAAACTCTTCATATGTCTAAATCAGGAATCAGACATTTTTCATTTTTTTTTCTAAACTAGCCTAAAATTGTTTTTCTAAATTGCAGTTAAAATGGCATATTTGACCTTTGGTAACTTTTAAAATATTGACATATAATTAATATACCACAAAACTCACCCTTTAAAAATATGCATGATTCGGTGGCTTTTTATTATATTCACAAAGTTGTACAATAATCACCATTATCTATCTTCAACATTTTCATCACCCCCAGGAGAAATCTCATAACCATTAGAAACCATTCTCCCTTTTCCTCTTCTTCCAGCCCTTTGCAACCACTAATCTACTTTGTCTGATTTGCCTATTCTGGACATTTTGTATACATAGAATTATACAATATGTGACCGTTTGTGCAATGTTTATTAAATTTTGAATGAACTCTCTATTCATGCTCAGGTGTTCCTTCTGAGTCAGAAAATATATTTTAGAGTCTTTAAGATGTTGTGTACTTTGTGCTACAAGATCTCAATGCTTTATAATGGGCAAACCACCTCACTATCCTTAGACCCTTGAATACTAAGTTGGCATGGGTGATTAAAATACTACATGTAATGTAAAAGATAAATTCAAGAAGAACGTGTAACTTGGAAAATCTGAAGTAGTCAGCAACTTTAGGGACTGGGAAGGCAGCTTTCCCAGGGATAACTAAGTGATAAAGCGAACTGCGTCCAGAATATATTTTAAGTACTTACTAATGTGAAAGCATTGTGGAATCTTCACGTCTTCTAAAAGTCACGTTTTTACGTGACTGATTGCAAATGCAACATAAGGTTAAAGTTGCTCAGAACTTATTTCATTCTGTTGAAAAGACAAAACTGCGTTTGAATTATTCTTGTGTATACTTTAATTTCAGATGAATTTTGACAGTTAAAAGTTGGTTTGCTGTAGAACATGATTTTTCTGTTTTAGAGTAGCACACGATAATGGCATACATAAAGCAATTTGCTACTGGAGAATTTGTAGCAATATTTGATGTTTGGTTTATTTACTTATTTGAGTTCCCACTTTATTTTGTACATATTTTCCCTTATTTTGATTAATTCTCCATGAGCTATTTTGGATAAATAAAAGTCATATTTGTCCTTCTACCTTTATAGTTTTCCAGGCACTCATGTAAAAATTGATCAAATCTAGAAATCAATTGGGTGATTCCTAGAAATTTCTGTTACTTGAAGAACACAACTTTTTCATTGTGATCTCTGATGAATAAACCTTTTTCACTTAACAATTCAATACCTTCCTGTTGGGAGCAGGCCCCCCAAAATCTGGCGATAAACCAGCCCCAAAACTGGCCATAAACAATCTCTGCAGCACTGTAACATGTTCATAATGGCCCTAACGCCCAAGCTGGAAGGTTGTGGGTTTACAGGAATGAGGGCAAGGAACACCTGGCCTGCCCAGGGTGGAAAACCGCTTAAAGGCATTCTTAAGCTACAAACAATAGCATGAGCGATCTGTGCCTTAAGGACATGCTCCTGCTGCAGTTAACTAGCCTAACCTATTCCTTTAATTCGGCCCATCCCTTCCTTTCCCATAAGGGATACTTTCAGTTAATTTAACATCTATAGAAACAATGCTAATGACTGGTTTGCTGTCAGTAAATACGTGGGTAAATCTCTGTTGGGGGCTGTCAGTTCTGAAGGCTGCGAGACCCCTGATTTCCCACTTCACACCTCTGTATTTCTGTGTGTGTGTCTTTAATTCCTCTAGTGCTGCTAGGTTAGGGTCTCCCCAACCGAGCTTGTCTCCACACCTTCCACTGTTCTTCTTAGTACTTCAGCATAATACTGTATGTTCCTTCTTTGTCCTGCAAGACAACGGTAAAGTGCATTATAATTAATTTATGTGTAATCCAAGTAAACAAGCCTCATTGTGTTGTATGTGTCTTATGATTAAGAGCTCAATACATTTAATCTAGTCTGACAGTTTGCCTGGTGTAAGTCATGTGTGTCTTGTTAAAAAAAATTTAATAAGAACAAAACAACTGGGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCGACGCAGAAGACGGGTGATTTCTGCATTTCCATCTGAGGTACCGGGTTTGTCTCACTAGGGAGTGCCAGACAGTGGGCGCAGGCCAGTGGGTGCGCGCACCGTGCGCAAGCCGAAGCAGGGCGAGGCATTGCCTCACTTGGGAAGCGCAAGGGGTCAGGGAGTTCCCTTTCCGAGTCAAAGAAAGGGGTGACAGACGCACCTGGAAAATCGGGTCACTCCCACCCGAATATTGTGCTTTTCAGACCGGCTTAAAAAACGGCGAACCACGAGATTATATCCCACACCTGGCTGGGAGGGTCCTACGCCCACGGAATCTCGCTGACTGCTAGCACAGCAGTCTGAGATCAAACTGCAAGGCGGCAGCGAGGCTGGGGGAGGGGTGCCTGCTATTGCCCAGGCTTGCTTAGGTAAACAAAGCAGCCAGGAGGCTCGAACTGGGTGGAGCCCACCACAACTCAAGGAGGCCTGCCTGCCTCTGTAGGCTCCACCTCTGGGGGCAGGGCACAGACAAACAAAAAGACAGCAGTAACCTCTGCAGGCTTAAGTGTCCCTGTCTGACAGCTTTGAAGAGAGCAGTGGTTCTCCCAGCACGCAGCTGGAGATCTGAGAACCGGCAGACTGCCTCCTCAAGTGGGTCCCTGACCACTGACCCCTGACCCCCGAGCAGCCTAACTGGGAGGCACCCCGCAGCAGGGGCACACTGACACCTCACACGGCAGGGTATTCCAACAGACCTGCAGCTGAGGGTCCTGTCTGTTAGAAGGAAAACTAACAAACAGAAAGGACATCCACATCGAAAACCCATCTGTACATCACCATCATCAAAGACCAAAAGTAGATAAAACCACAAAGATGGGAAAAAAACAGAACAGAAAAACTGGAAACTCTAAAACGCAGAGCACCTCTCCTCCTCCAAAGGAACGCAGTTCCTCACCAGCAACGGAACAAAGCTGGATGGAGAATGACTTTGACGAGCTGAGAGAAGAAGGCTTCAGACGATCAAATTACTCTGAGCTACGGGAGGACATTCAAACCAAAGGCAAAGAAGTTGAAAACTTTGAAAAAAATTTAGAAGAATGTATAACTAGAATAACCAATACAGAGAAGTGCTTAAAGGAGCTGATGGAGCTGAAAACCAAGGCTCGAGAACTATGTGAAGAATGCAGAAGCCTCAGGAGCTGATGTGATCAACTGGAAGAAAGGGTATCAGCAATGGAAGATGAAATGAATGAAATGAAGTGAGAAGGGAAGTTTAGAGAAAAAAGAATAAAAATAAATGAGCAAAGCCTCCAAGAAATATGGGACTATGTGAAAAGACCAAATCTACGTCTGATTGGTGTACCTGAAAGTGATGGGGAGAATGGAACCAAGTTGGAAAACACTCTGCAGGATATTATCCAGGAGAACTTCCCAAATCTAGCAAGGCAGGCCAACGTTCAGATTCAGGAAATACAGAGAACGCCACAAAGATACTCCTCGAGAAGAGCAACTCCAAGACACATATTTCCTCATAATTGTCAGATTCACCAAAGTTGAAGGAAAAAATGTTAAGGGCAGCCAGAGAGAAAGGTCGGGTTACCCTCAAAGGGAAGCCCATCAGACTAACAGCGGATCTCTGGGCAGAAACCCTACAAGCCAGAAGAGAGTGGGGGCCAATATTCAACATTCTTAAAGAAAAGAATTTTCAACCCAGAATTTCATATCCAGCCAAACTAAGCTTCATAAGTGAAGGAGAAATAAAATACTTTACAGACAAGCAAATGCTGAGAGATTTTGTCACCACCAGGCCTGCCCTAAAAGAGCTCCTGAAGGAAGCGCTAAACATGGAAAGGAACAACCGGTACCAGCTGCTGCAAAATCATGCCAAAATGTAAAGACCATCAAGACTAGGAAGAAACTGCATCAACTAACAAGCAAAATAACCAGCTAACATCATAATGACAGGATCAAATTCACACATAACAATATTAACTTTAAATGTAAATGGACTAAATTCTCCAATTAAAAGACACAGACTGGCAAATTGGATAAAGAGTCAAGACCCATCAGTGTGCTGTATTCAGGAAACCCATCTCATGTGCAGAGACACACATAGGCTCAAAATAAAAGGATGGAGGAAGATCTACCAAGCAAATGGAAAACAAAAAAAGGCAGGGGTTGCAATCCTAGTCTCTGATAAAACAGACTTCAAACCAACAAAGATCAAAAGAGACAAAGAAGGCCATTACATAATGGTAAAGGGATCAATTCAACAAGAAGAGCTAACTATCCTAAATATATATGCACCCAATACAAGAGCACCCAGATTCATAAAGCAAGTCCTGAGTGACCTACAAAGAGACTTAGACACCCACACATTAATAATGGGAGACTTTAACACCCCACTGTCAACATTAGACAGATCAACGAGACAGAAAGTCAACAAGGATACCCAGGAATTGAACTCAGCTCTGCACCAAGTGGACCTAATAGACATCTACAGAACTCTCCACCCCAAATCAACAGAATATACATTTTTTTCAGCAGCACACCACACCTATTCCAAAATTGACCACATACTTGGAAGTAAAGCTCTCCTCAGCAAATGTAAAAGAACAGAAATTATAACAAACTATCTCTCAGACCACAGTGCAATCAAACTAGAACTCAGGATTAAGAATCTCACTCAAAGCCACTCAACTACCTGGAAACTGAACAACCTGCTCCTGAATGACTACTGGGTACATAACGAAATGAAGGCAGAAATAAAGATGTTCTTTGAAACCAACGAGAACAAAGACACAACATACCAGAATCTCTGGGATGCATTCAAAGCAGTGTGTAGAGGGAAATTTATAGCACTAAATGCCCACAAGAGAAAGCAGGAAAGATCCAAAATTGACACCCTAACATCACAATTAAAAGAACTAGAAAAGCAAGAGCAAACACATTCAAAAGCTAGCAGAAGGCAAGAAATAACTAAAATCAGAGCAGAACTGAAGGAAATAGAGACACAAAAACCCCTTCAAAAAAATCAATGAATCCAGGAGCTGGTTTTTTTGAAAGGATCAACAAAATTGATAGACCACTAGCAAGACTAATAAAGAAAAAGAGAAGAATCAAATAGACACAATAAAAAATA";
		s = "CTGAAAACAGAAATAAAATACGCGCATTTATCCAGGTTAACCAAAACGGGAAATGTGTAACCCCTTAAATATTTTCTGGTGGAAATGCAGGTATCCTTGGAAAATGAAATAACACTATAGGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCAACG";
		nm = new SmithWatermanGotoh(r, s,  4, -4, -2, 4);	
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
//		nm = new SmithWatermanGotoh(r, s,  4, -14, 20, 15);
		nm = new SmithWatermanGotoh(r, s, 5, -6, 16, 10);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		r = "AGTGCAGTGGTGCAATCTCGGCTCACTGCAACCTCCACTTCCCAGGTTCAAGCAGTTCTCCTGCCCCAGCCTCCCAAGTAGCTGGGACTACAGGCGTAAGCCACCATGCCCGGCTAATTTCTTTTGTATTTTAGTAGAGACCGGGTTTCACCGTTTTGCCCAGGCTGGTCTTGAACTCCTGAGCTCCGTCAACCCACCCACCTCGGCCTCCCAAAGTGCTAGGGTTACAGACATGAGCCACCCTGCCTGGCCTTTTTTTTTTTTTTTTTGAAACGGGGTTTCATTCTTGTTGCCCAGGCTGGAGTGCAATGGCATGATCTTGGCGTCGGCTTACTGCAACCTCCACCTCCCGGGTTCAAGCGATTATCCTGCCTCAGTCTCCTGAGTAGCTGGGATTACAGGCACACACCACCATGCCTGACTAATTTTTGTATTTTTAGTAGAGACGAGATTTCATCATGTTGGCCAGGCTGGTCTTGAACTCCTGACCTCAGGTGATCCACCTGCCTCGGCCTCCCGAAGTGCTGGGATTACAGGCATGAGCCACCATGCCTGACCTTACAATTCTTAAAAGAGTAGTGTACACATACTACAACCACTTCACCTCTCCTTCACAACTCAACTCTCTGCAATCTGGATTCAATCCTGACATTTCCTCATGTCTTATGGGCACGAATTTCACCTCTATGCAGCATTTAGCAATAAGTAACAACAGCTGGTAATTATTGAGTGTGCACATTCTGCCAGTCACTGTGTTAAGCACGTCACGTGCATTTTGGTTGATGCTTCCCAAGCTACAAATCAAACCCAATCTTTTTTCCAAGCTCTAGACTAGTATAGTGACAGGCACCACAAATCCATACAGATGAAAACAGAGCTCATTAGCAAAATCCCACTGCTGGATGTCTCCTGCTTGCCCTTCTAGTTCCACTTGCAACCCTTCCCCAACCTGCTCTGCACTCCGAGAGGCTGACCTTTGTGAATGGCCTCCCTGGACTCCCATGCCTCTAGCTTATGTTTGAAGAACCCAGTGGAGAGTCCCAGCAGAAAGCTGGAAGAAGACAGGAGAGTGAGGTAAGTGTATTGATTCCTCAGTCACCATCTGCTGTGGCCGTGTGGGCTGGCTGGGTCCACTTGGCTGTGTCCCTCTAGTGAAAGGCACAGCCGCTGTCATGCAGCCCTCATCGCATAGAGCCCCCCGGATTCCAGCAACTGCTCCCTTCTCTTTTCTCCTTCAGGAGTCACAGCCCCACTGCCAAAGCCACAGGGCCATGCACTGTCTCTGGGACATCACCCTACACCTGCCCGCACCTTTCAAGCAGCCTTTTTATTAAATGCTCCTCCTGTTACTTAATTGGAGTATGTCATCTTTTCTGACCAGAATGACGACTGACACATGCCCTCCCCATTCTCTATCTCTTTTTTTTAATTATTATTCTACTTTAAGTTCTAGGGTACATGTGCACAACGTGCAGGCTTGTTACATATGTATACATGTGCCATGTTGGTGTGCTGCACCCATTAACTTGTTATTTACATTAGGTGTATCTCCTAATGCTATCCCTCCCCCCTCCCCCCACCCTACAACAGGCCCAAGACGGTGTGTGATGTTCCCCACACACAGTGTGGCAATTCCTCAAGGATCTAGAAGTAGAAATACGATTTGACCCAGCGATCCCATTACTGGGTATATACCCGAAGGATTATAAATCATGCTGCTATAAAGACGCATGCACACATATGTTTATTGTGGCACTATTCTCTATTTCATTTAACGGAGTTGCTCCAACGTCATATTCCTAAAATGGAAACCAAGATGACCCTAGACTTCTTCACCCTCCCCTTTATACCCAGTAAGTCCTGCCCATTTTCCTTCAGGAACGCATTCCCCAAATCCACTCTCTCTTCTGAACCCCTGTTCTCATTTCGCTCACTGCGTCAGATTTCAGTTCTCCCTTCTCACTCCAGAAAGTTACAGTTCTAAAATGACAATCTGGGGAGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCGACGCAGAAGACGGGTGATTTCTGCATTTCCATCTGAGGTACCGGGTTCATCTCACTAGGGAGTGCCAGACAGTGGGCGCAGGTCAGTGGGTGCGCGCACCGTGCGCGAGCCGAAGCAGGGCGAGGCATTGCCTCACTCGGGAAGCGCAAGGGGTCAGGGAGTTCCCATTCCGAGTCAAAGAAAGGGGTGACGGACTCACCTGGAAAATCGGGTCACTCCCACCCGAATATTGCGCTTTTCGGACCGGCGTAAAAAACGGCGCACCACGAGATTATGTCCCGCACCTGGCTCGGAGGGTCCTACGCCCACGGAGTCTCGCTGATTGCTAGCACAGCAGTCTGAGATCAAACTGCAAGGCGGCAGCGAGGCTGGGGGAGGGGCGCCCGCCATTGCCCAGGCTTGCTTAGGTAAACAAAGCAGCCTGGAAGCTCGAACTGGGTGGAGCCCACCACAGCTCAAGGAGGCCTGCCTGCCTCTGTAGGCTCCACCTCTGGGGGCAGGGCACAGACAAACAAAAAGACAGCAGTAACCTCTACAGACTTAAATGTCCCTGTCCGACAGCTTTGAAGAGAGCAGTGGTTCTCCCAGCACGCAGCTGGAGATCTGAGAACGGGCAGACTGCCTCCTCAAGTGGGTCCGTGACCCCTGACCCCCGAGCAGCCTAACTGGGAGGCACCACCCAGCAGGGGCACACTGACACCTCACACGGCAGGGTATTCCAACAGACCTGCAGCTGAGGGTCCTGTCTGTTAGAAGGAAAACTAACAAACAGAAAGGACATCCACACCAAAAACCCATCTGTACATCACCATCATCAAAGACCAAAAGTAGATAAAACCACAAAGATGGGGAAAAAACAGAACAGAAAAACGGGAAACTCTAAAACCCAGAGCGACTCTCCTCCTCCAAAGGAACGCAGTTCCTCACCAGCAACAGAACAAAGCTGGATGGAGAATGATTTTTACGAGCTGAGAGAAGAAGGCTTCAGACGATCAAATTACTCTGAGCTACGGGAGGACATTCAAACCAAAGGCAAAGAAGTTGAAAACTTTGAAAAAAATTTAGAAGAATGTATAACTAGAATAACCAATACAGAGAAGTGCTTAAAGGAGCTGATGGAGCTGAAAACCAAGGCTCGAGAACTACGTGAAGAATGCAGAAGCCTCAGGAGCCGATGCAATCAACTGGAAGAAAGGGTATCAGCGATGGAAGACGAAATGAATGAAATGAAGCGAGAAGGGAAGTTTAGAGAAAAAAGAATAAAAAGAAATGAGCAAAGCCTCCAAGAAATATGGGACCATGTGAAAAGACCAAATCTACGTCTGATTGGTGTACCTGAAAGTGATGGGGAGAATGGAACCAAGTTGGAAAACACTCTGCAGGATATTATCCAGGAGAACTTCCCCAATCTAGCAAGGCAGGCCAACGTTCACATTCAGGAAATACAGAGAACGCCACAGAGATACTCCTCGAGAAGAGCAACTCCAAGACACATAATTGTCAGATTCACCAAAGTTGAAATGAAGGAAAAAATGTTAAGGGCAGCCAGAGAGAAAGGTCGGGTTACCCTCAAAGGGAAGCCCATCAGACTAACAGCGGATCTCTCGGCAGAAACCCTACAAGCCAGAAGAGAGTGGGGGCCAATATTCAACATTCTTAAAGAAAAGAATTTTCAACCCAGAATTTCATATCCAGCCAAACTAAGCTTCATAAGTGAAGGAGAACTAAAATACTTTACAGACAAGCAAATGCTGCGAGATTTCGTCACGACCAGGCCTGCCCTAAAAGAGCCCCTGAAGGAAGCGCTAACCATGGAAAGGAACAACTGGTACCAGCCGCTGCAAAATCATGCCAAAATGTAAAGACCATCGAGACTAGGAAGAAACTGCATCAACTAACGAGCAAAATAACCAGCTAACATCATAATGACAGGATCAAATTCACACATAACAATATTAACTTTAAATGTAAATGGATTAAATGCTCCAATTAAAAGACACAGACTGGCAAATTGGATAAAGAGTCAAGACCCATCAGTGTGCTGTATTCAGGAAACCCATCTCACTTGCAGAGACACACATAGGCTCAAAATAAAAGGATGGAGGAAGATCTACCAAGCCAATGGAAAACA";
		s = "CTGAAAACAGAAATAAAATACGCGCATTTATCCAGGTTAACCAAAACGGGAAATGTGTAACCCCTTAAATATTTTCTGGTGGAAATGCAGGTATCCTTGGAAAATGAAATAACACTATAGGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCAACGCCGAA";
		nm = new SmithWatermanGotoh(r, s,  4, -4, -2, 4);	
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		nm = new SmithWatermanGotoh(r, s, 5, -6, 16, 10);
//		nm = new SmithWatermanGotoh(r, s, 4, -14, 20, 15);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		r = "aaaagaaccaggagggcacatgggcatggggagtgatgaaccagagaaagctgctgtctttctgggcaagtgccaagcaacggatcacccttgacccctaGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGgtgagaggagggtcggcctgggaggaccccacagggaaggggtgagcctggcccgggcaggtgttcgctgcgtgggtgggcggaggagttctagagccggccccttgtctctgcagAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGgtgagcatgagacctgctgtccactcccactccctccttcccacagcctccccagacctctctcccctcatcctggcttcccctctgtctgcagGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCaacaaaactgtgtcttatctgccaggaaagaccagcctcactcctgggaactgtctggcaggtaggctgggccccccagtgctgttagaataaaaagcct".toUpperCase().replaceAll(" ", "").replaceAll("\t", "");
		s = "TGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCG";
		nm = new SmithWatermanGotoh(r, s,  4, -4, -2, 4);	
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
//		nm = new SmithWatermanGotoh(r, s, 5, -6, 16, 10);
//		nm = new SmithWatermanGotoh(r, s, 4, -14, 20, 15);
//		nm = new SmithWatermanGotoh(r, s,  5, -4, 16, 4);
		nm = new SmithWatermanGotoh(r, s,   4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		r = "GCCTGCTTTCTTACTTTTCTGCCCTCATATGACAAACTCCTCTGAGCCCTTTTCACCCTTAGGGGCCCAACTCAAGTGCCCGTCACTTCCATACCTCTATCAGCCAGAAAGGATGTGTCTGTGTCTGTCTGAGCTCTCTCACATTTACACTCTGCCTGATATCAGCTAGCCCTGGACACGCTGGGTGATAAATCCCCTCCTGTGTTCACCAAGTAATGGTTATGAGTGGGGACCCTGGAGCTGAGCCATCTGGGCTCCATCTTTGCTACATCACCTACTAGCTGTGTGACTTCTGGCTGGTTACTTAACGTCTCTGGGCCTGAGTTTTTTCATCTGTCAAATAAGGATAGTGATAATGGTAGCTACTTCACTGGGTTGTCGTGAGAACTAAATACACGTGGAGCACTTAGGGTGTACGTGTGGGTTTCCTGGGTGGCTGGCTAGGCCCTCCCCAGATGGTGGGAGGGGTCCCCCCAGAAGCAGCCCCTTGAATGCTCACCCTTTCTTGGCGAGAAAGGCCATGCGCCTGGCTTCTGCCTTCTCTCTCAGCACTGCAGGGTCCTGAACAAAATGGTCGGGCTGTGGAAAGGAGAGGAGACCAAATCTGGAGTCAGGAGCTGCCACATTTTCCTAAGCGGCGATGAGGCCCTGCTCAAGTGAAGCTTGTCGATGCCTCCATGCCAGGTGTGGGGGGAAGCTGCAGCCTGGCAGAGCGACGCCCAGGAATGGAGTGGGATTACTGTGGTCTGAGGTTTCCATTAGGAGCCCCCTGCCCCACCTCTTCCCTGAGCGGGTGGCAGCTGAAGGGACTGAGGCTTCCTGGACAGGCTGGTAGAGTGGATAGACCATAGTTATGGCATAACAATGACAAATGGGAGTGGTGGGGAAGCCAGGATGGCCACTGTGAGAGGCTGTGCAGGGCCCTGAAGACTGCGGGAGCCACCGTGGGAGCTAAGTGGTCCCGGGAGCTGCAGGCCTAGGCCTCACTGTGGCACCACTA";
		s = "CTTTCTTGGCGAGAAAGGCCATGCGCCTGGCTTCTGCCNTCTCTCTCAGCNCTGCNGGGNCCTGAACAAAATNGNNNGGGACAGCACA";
		nm = new SmithWatermanGotoh(r, s,  4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
//		nm = new SmithWatermanGotoh(r, s, 5, -6, 16, 10);
//		nm = new SmithWatermanGotoh(r, s, 4, -14, 20, 15);
		nm = new SmithWatermanGotoh(r, s,  5, -4, 16, 4);
//		nm = new SmithWatermanGotoh(r, s,   4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		r = "GCACAACTTGGTTTTATACATTTTAGGGAGACATGAGACATCAATCAATATATGTAAGATGAGCATTGGTTTGATCTGGAAAGGCAGAACAACTTGAAGCAAAGGACTCACAGTGGGGAGGGGGCTTCCAGGTCTTTGGTAGATAAGAGACAAATGGTTGCATTCTTTTCAGTTTCTGATTAGCTCTTCCAAAGGAGGCAATCAAATACACATTTATCTCAGTGAGCAGAGGGATAACTTTGAATAGAATGGGAGGTGGGTTTGCCCTAAGCAGTTCCCAGCTTGACTTTTCCCTTTAGCTTAGTGATTTGGGAAACCTGAGACTTATTTTCCTTTCACATTGCCCAGAGCTCTGGGTTTCTCTATCTCAGTGTCTGTCTGGCCTGCACCCATCACCAAACCAGGTTCCTCAACCTGCAGTGGCCCGCTTTGATCCCCTGGTCAGTGTCAAAGCTCACAGTACTTCCCTAGGCTTCTATCAACCTCAGGATTGCCTACCCTTGCTGGAAGCAGTAAAACAGCTGCAGCTGCAAAAGGTGGGGGCAGGCCAGGGGAGAGGTGGCATCCCAGGGAAGTCCCAGGTACTGACCATGTGTCCCCCTCCCAGGTACTCTGCTGGGGTGTGGATGCTGCACCATTTCCTGTTCTGCATCTGCATAGCATCTACACATTGCTCTCAGACCTTGGACTAGGTGTGGTGGGGACCCCCTTTCTTATCTCCTACACAGGAGAACAGTCTCTGTGAGAGTCAGTCCTGCTATAGGACAAATCCCGCTTTGGTGCCTGTGGCCTAGCTCCCTCCGTGCCTGTGAGAGAAGGGGCAGGGGCCTGGGTCCCAGTCATGGCTCTGCTGACCATGGGACAGTGGCTGCTGTGATCCTCCAGGGCTCTGTGGACTCATTTTAAACGCAAGGGGTGAGGCCAGGTATGGTGGCTCATGCCTGTAATCCCAGTACTTTGGGAGGCCGAGGTGGGTGGATCACCTGAGGTCAGGAGTTCGAGACGAGCCTGGCCAACATAGTGGAACCCCATCTCTACAAAAACACAAAAATTAGCCGGGCATGATGGCGGGTGCCTGTAATCCCAGCTACTCGGGAGGCTGAGGCTGGAGAATCGCTTGAACCCGGGAGGCAGAGGTTGCAGTGAGCTGATATCGCACCATTGCACTCCAGCCTGGGCAACAGAGCAAGACTGTGTCTCAAAAAAAAACAACGAAGGGGTGGATTTGGTCCAGGTCTTCAACCCTAGTTGCACATTAGAATCAATCACCTGAGAGATGTTTAAAAATACCCATGCCTGATCCTCCTCACAGACCAATTAAATCAGAACCTCCAGCAGGGAGGAACGGGTCTGTGGCTAATAAAACGGCTCAGTTGATTCTCACATATGGAGAGGGCTTGAGAAGCCCCGGATTAGGGGATCTGCAAGGGCCTCCTATGTCTGACGTTTGCCACTCTGTATATGACAGAATCATAGGGTCCCTGGGCTCTGGGTGCTGGTGGACCCTCCCCCAACACATGGAACCAAGCAAATGGAAATGAAACAGCACTGGATGACGCTTTGCATCTGCATAGCACTTTGACCCATGTGATTTCCGTAAAATCCCATGGCAAATTGGACCCCTATGATTACTTCTAGTGGAGAGGAAGGAAAAGAAACAAGGGCCAGCCAGGGAGAGGTCTCCCCAGGCCTTGCCAGCAGCCTGCCTCCCAGTCCTTGGGCTCAGCCAAGCCTCTTCATCTCTTTACAGAGACAAGGGAGCGCCTGCCCTGTGCCAGGCACTGCAGCAGGAAGGACTACGTCAGCAACATAGCACACACGCACGGCCCCTGCCCTCAGCAGGGGAGGAGAGCGACGCACAGGAAAACATCTTAAATGGTGTGATAGGCATCATGAAGGACGCCCACAGGGTGCCATGCAGAGAACAATGGTGGTGGCAGGAAGCCCCACAGGGAGGTGACATTTCAGTGAAGAGCTGTAAGGTAAAAGCCGGCAGCCCCCTGCAGAGCCAGGAGGCTCACGAAGGAAGAGCACAGCTCATGCAATCCCCTGAGGTCCCCCTGGAGGGGGGTCCAATGACACCTGGGGATGAGAAGGAAAGTCATCCTTTCCCTCCACCACATGGTGGGGCTTCTGGCTTATGGACAGGAACCCACTCCCACTGGGCAGCGACGTGCCTTAGGAAAAGGAAGAACAAACCCCCTAACTGGCTGAGGGCCCAAATGCCCCGCTGGCCTCAGGAGCCAGGGCTCTTTGAGGGTACCCTCCGCCGCCAGGCCCCAGCTGGGCCCGTATTGACCGTTTGGGCTTGCTGCTGCCTCCACGCCCTGGCTGCCCACCTCAAAGGCTCCCAGGACTCCACGGCTCAGGCTCCCTCTTGCAGGCAGCTGCTCCTTCCTGGGGCCAGGCTTCCCTTTACTCTGCAGAGGGGAATGGGTGCTGTTTTCCTTCCCCTCACAAAGCATCTTTCTGACTGCCCACCTCCTGCCCCACACAGTTTGAAAGGCTGATAGTTTCGAGATTCTGAATGAGGCTTCTGCAGAGGCCTTCTTGTGCTCCACAAGAAGGGCTGGGCAAGAATTTCAACCACGCTGCTAATTTCAGTCCGTCACTGCGGTGGCAGCTTCATATTTTCCCACTAGATGGGGTTCTTGATCTCTGCAGCTAAAGATCAGCTAGCTAGAGAGAATTCCAAATCATTTAATTTGGCTCTGAAGGCTGCTTAGAAATTTTATTCAAAGCAGAGGGAAATCTTTTATAGGAAGTGGCCAAAGGCTGAAATTGAAAAAAAAAAAAAAAAAACCCTAAAAATCTGGTTTCTGTATCTTCCTTAATTAGCAAAGGTGATAAAGGGTAATAAGTGAAAATGTTTGCAAAGGAAACCTAGCAAATCTGAAATACAGATGCAAATGGAGTGAGCTAGAAATGATTTCTCTAAAGAGCCATTTTGAACAATGCTGTAACTGCTTCTTCCCTTTGAGACAGCCAGAAGCATTGACCCTGGACATTCAAGCTGAAAAAAATATCCTTTGGTCTGGTCATTATCACCATTCTGCTTGATCAATGGTAGCCTGGTTCTCATTGATCCACTCCAGCCCCCAGACACACAAGAGAGGTAGCACAAGGTAGACGCATAGTGAAGCCACTATGAGCCAAAGGCAATTATGTTTTTCCAGTGCAAACTCATTTTGCTCAGTTCGGGAAACAGTTATATCTTTGAAGTCATTTAGCATGAATATTTCTCTTCATCATCAATAATAAGCACCTCTCTTGTCCCTTCCTCTGTAGGAAGGCTTTCTCTGACGTTTTTAGGCAGAGTTGAGATGGTGCCAGAAACAGCAAGAACAAAAAGATACTAACACTAACCCTGTCTTCAAGAGTCTATATCTTAGCCCAAGAAATAGCACTCTTCCCAGAAAACAGTAGAAGATACACATTATTTACATCATCTTAGAACTTTGCATGGATAAATGAAATATTTGACGAATAAACTCAGACCTTTGTATTTAGAAGTTAGAATTTTCCGTAGATATAAAACCAGTTTTAAAGAAGCTTCCCTTTTGAAGTGCCATAACAGAATTGCACAAGAATTATGGACAAAATTATGTAGACAAAAACACATGATCAGCTTTTACTGCTGACATTTTCATTTACTGTGGTGTCGTTTGTGGGACCCCCACATTTCTCCAGAATTGGGGATATTTTT";
		s = "GAGGGGGGGAAGGCAAGAGGGGCGAGGCAGGGAGAGGGATCCCCAGGCCCTGCCGGCAGCCTGCCTACCAGTCCTTGGGCTCAGCCAAGCCTCTTCATTTCTTTACAGAGACAAGGGAGCGCCTGCCCTGTGCCAGGCACTGCAGCAGGAAGGACTACGTCAGCAACATCGCACACACGCACGGCCCCTGCCCTCAGCAGGGGAGGAGAGCGACGCACAGGAAAACATCTTAAATGGTGTGATAGGCATCATGAAGGACGCCCACAGGATCCCGCGCA";
		nm = new SmithWatermanGotoh(r, s,  4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
//		nm = new SmithWatermanGotoh(r, s, 5, -6, 16, 10);
//		nm = new SmithWatermanGotoh(r, s, 4, -14, 20, 15);
		nm = new SmithWatermanGotoh(r, s,  5, -4, 16, 4);
//		nm = new SmithWatermanGotoh(r, s,   4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		/*
		 * This example is taken from http://genome.ucsc.edu/FAQ/FAQformat#format2
		 */
		r = "TGACTATGTCTTCTAGAATATTTCTAAGAAATTGCCTAAGTCACCACTGCTCACCAAGAGGCCTTTGTTTTTTCCTCTTCTTAACCATGGGAAAGGAATGTAGGAGGGTAGGGAGTGGATATTTTCTAACCTGGAAAAAACTCATTTTACCCTATATAATTTTTTTTAGCAAATTCCTTCTTTGCACTTACTCCACAATCTTTCCAAATTCTCCCAAATGCTCAAGCTTTTAAAAAACAAAAGACAGAAAGA";
		s = "CTTGCTAGGAGGGTAGGGAGTGGATAAAAAACTCATTTTACCCGGGG";
//		s = "CTTGCTAGGAGGGTAGGGAGTGGATTAAAAGATTGGACCAAAAAACTCATTTTACCCGGGG";
		nm = new SmithWatermanGotoh(r, s,  4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
//		nm = new SmithWatermanGotoh(r, s, 5, -6, 16, 10);
//		nm = new SmithWatermanGotoh(r, s, 4, -14, 20, 15);
		nm = new SmithWatermanGotoh(r, s,  5, -4, 16, 4);
//		nm = new SmithWatermanGotoh(r, s,   4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		r = "AGCTAATTTTTGTATTAGTAGAGTTGGGGTTTTGCCATGTTGGCCAGGCTGCTCTCGAACTCCTGACCTCAGGTGATCCGCTCACCTCAGCCTCCCAAAGTGCTGGGATTACAGATACGAGCCACCGCACCTGGCCATGATTGTTGTTTTTATAAAATCAGAATATAAAATTTGAAAATGTTCTTTTTTGGTGAGAAATGAGAACTACCAAAGGAGTAAAATGCTCTAAAAGCCATCAGGTCCTTCCCACAGTATTGAGGCTCTGCTCTTACCATTTTGTTAAATAAGTTTTGTTCCTAACACTCTTTGTGATATGAAGTATTCTTGCTAATCAAGCTGCCCGGAGACTGGCGTTTCCCAGAGCAGTTTCGAAAGTGGAAATGATGCTGCATTTCCTACCCACAGTCTGGAGGGTGGCTGTGCTCTCTGCACGGTGGGTCTGTGCTGCTCAGTTACGGGATGGTAAGGGAAGGCTCTAACTCGGTGTCTTACACCATGCACTAGGGAGTCGCCCGCTTTATTTACGAGCGAGTTGATGCTTTCAGTGGGTTTTGGCTTTGCCTGGTTCCAGACTCAAGCCCTTCCTCTAATGTAGGGCGGGTACAAGAGAGACTTGAGCAAGGATATTGGGATTTAAGGGAAGGGCAGTGCAGGCCAAGGTCAGCAAGAGGACATCTCTAAGATGAACAGCACTGCATTTGTGGGAAGCAACCCTGCCATAAACAACTCAGTTAACGTTTACATAGTAAATAATTGCATTGCCTTGGGCTGGAATAGGGACGCCAGAAGGTATAGGACCATGTGGAATTCAGAAAAGGGAGCCCTGAAGAATGAGATTTTATTGTTCTTACCAGGAGAGTCCAGGTGCTAGAGTTCTCTCCTTGTAAGTGACTAACCTTCTCCCTTTTGGTACACAATCCTCAGATGAACGGAGATGATTCATTAGGCCATTCTAGCTTAATGGATGCATCACTGTGCAACCACGCAAATGCCTCATTTCTGATTCACTCATTTAATATTTATTGAGGAGTGCTGGGTACTAGGCCATTAGAAGAACAAACAACAACAACATAGGCCTGGACCTTGCACTTGCAGAGTTCACAGGCTAGAGAGGCACACAAGTTAAATATAGAGACAGCTGAAAGAGCTGACGTCTGGGACAGAGGGAGCCCCCTGCTCCAGGAGCCAGGGAGCAGATGCCATGGGGGGCTGGCAGGGATAGGGAAGTCAGGCCAGGAATGGGGTTACCTAAGCCGGATCATGAAGGACGAGAACAGACTCACCAGGGGTCGAGGGACAAGGAGGCAGCAAGTCCGCAGCATGAAGGCAGGAGAGAACATTGCTGTAAGCCTTTACCATGGCCACTGCATGGGGAGGAAACAGGAGATTTGGAATGAGCTGTGACACTGGGCAGGAAAGCAAGACCCAGGTCTTGTGAGGCCTTGAGCGTCAAGCTTAGGAGTCAGGACCTGGGGATTTTAAGTACAGCAATATCATAATTACATTCATGTTTCAGAGAGATCACTCAGGCAGCAGTCTGGAAGGTGGAGTGGAAAACGGAGAAATTAGTCATGAAGACCTATTTAGAAAGTAGTAGGCAAGAAATACCCAGGGCCTAACCTGAGGTGTAGGCAGGGGATGGGAATGAGAGCAATTACCGAACTAGAATGAACTAAACTCAATGAACAGGAGCGGGGAAGAGGGAATGCAGAAATGGCTCTCAGATTTCTGACTTGTTCGCTTAATGACTGGGGTTGTCATTTGTTTAGATAAGCAATAAAGAACCTAGAAACAATTTTAGGCGGAAGATAGTTGTGTTGGGAGCCCCTAAGACCATGATTTATTACAGCACAAGGATACAAAGCAAAATTAGCAAAAGGAAACAGCACGTGGGGTCAAGTCCAGGGGAGCTCAGGCGCAAGCTTCCAGAGTACTCTCCCCATAGAGTCACACAAGATACACTTAGTTCCTCCAGCATTGAACTATGACGACACCTGAAATATTGTCTGCAGAAGAGGCTCATTAGAGACTCAGTCCCCAAGGTTTTTATTGGGTAATGGTCACGTACCAAATTTCAGTCTCTCAGAACAAAAGCAAATGTTCAGCATAGACCATTTTGTTTGTGAAATTTAGGCACAGTGAGCCCCTCTTGTCAGTTAGAGGATAGTTGAAACC";
		s = "ATGATTCACTCATTTAATATTTATTGAGGAGTGCTGGGTACTAGGCCATTAGAAGAACAAACAACAACAACATAGGCCTGGACCTTGCACTTGCAGAGTTCACAGGCTAGAGAGGCACACAAGTTAAATATAATGATTCACTCATTTAATATTTATTGAGGAGTGCTGGGTACTA";
//		s = "CTTGCTAGGAGGGTAGGGAGTGGATTAAAAGATTGGACCAAAAAACTCATTTTACCCGGGG";
		nm = new SmithWatermanGotoh(r, s,  4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
//		nm = new SmithWatermanGotoh(r, s, 5, -6, 16, 10);
//		nm = new SmithWatermanGotoh(r, s, 4, -14, 20, 15);
		nm = new SmithWatermanGotoh(r, s,  5, -4, 16, 4);
//		nm = new SmithWatermanGotoh(r, s,   4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		r = "ATAAGAATAAAATTGTTTTAGTCAATGAAAAATTAAGGCTTTGTCTCACTCCTGGACCCAATCTTATTCCTGAGAGCATTACCAGCGCTCTGCTCTCCTTCCACAGGAAATCTTTGCTTATAGAAGCATATAGATCTACAATTTCATTTGTTTTTAAGAAAATCCTAGTATACATTGCTCCACACGTTACACCTTAAAAACTGAGTTGATTTGTTGTGAAAGAATATATCTTGAAGATACCGTCTATTAATGGTGCTTCCTCAGTTTTTAAAGAATGCATAATATTGCATTATAAGGATTACCTAAAATTATTAATCTTGACTCTTTTTAAGGGCCTTGAGGTTATTTCCAATATTTTGCTCTCACTAATGGTGCTGTATTAAGACTTCTAGTCAATTCTTTTGTTTTATCCACCTCCCTTACTTATATTGAAAGATAATCATAAAGAAATGAAAAACTGGTTCAAAAATGTGTGCATTTTAAATTTAACAAATTGACACCTAGAAGGTTACTTTATAGACTCAGTAATGAAGAATATATCCAAATGTTCTGGCCCTTTCTTATGAGTAGAGCAAAAGATTCACATTTTATTCAGATTCAAGTGAAGGATCTCTTTCTATGCTTAGAAACCGTTCAAAATGATAGCCTATGTTTTGCAACACTTGTCCAATTCTACCTCTCCAACAACACTTGAATTTTATTCTTGGCCACCATCAGCCCAGATCTAGGCAACTCCAAAGAGAAACTGGGCCATCAGTAGGGAATGGCAGTCAGAGTTCATGGAACAGATATATATGCATTCAATGAACATAATCCAGTGTTAATCCTGTTTGAACCCCTGTACAAGATGCAGGGATTCAAGTTCATGAAATACAGTCACTAGCCATGGGGATTTTCCAAGCTAGACGGGAGGCTGAGCAAACATAGACCATGTGACACAGAGTGGTACAGCCTTGCTGGAGGAAACAGAGGAAACACTAGAAATAATGTGAGCACTCTCTCAAATAGTTAATCTTTAACAGGATTGTGAGTCTTGGGTTCAGTTATATAACTAGCAGTTTTCTCAGGGGATGTTGTAGGTATAGGAGAGAGTAGAGAGAAGGCTGAAAGTTTGTTTGGTGGCATACTGTCTTTCTGAGGCACTTTTCCTCTTGTAGGTAATAATAAATGGCCAACACCATACATCTTTGCCCATCCACTTCCACCATGTTCTATGAAGATTTTGCAAGTGTGGGAAGATGTCTCCCTGACCTCAATGTCTGTCTGCAATTTATGGAGATGACCACACCCCTCATTGTTGAGGAATCCCTCTTTCCATGTGACCATAGGATCCCCAGAGCCTGCTAACAGCATGATCTCTCCTCACCCTTTTCCCTACACTTGGTCATTAAAACATCACCAAACTTCCCAGAATTTGGTTCTTGCTTTAAGGGGGAAAGAGAAAGTGGTAATCCCCAAGAGGCCCCGAGGAACTTTATGGGAAGCATCCAGAAATCAAATAGGATAAACCTTCTTGTGACTCCAGGAGTGAATGACAAGGTCCCTGCAATATCTTAGGAGACATTAGGTACAGCATCCTTTGACAGCATATTGTTGACATCAGAGACTCTGAGACAAAATCTTTACCCTATCTTAGATGACTCACTGCACTGAAATGTTAGGTAGATGTTTACAGCCACACAGATGTACCTCGTGGCTTTGGGGAGAAATTTTGTAACTTAAGAACAATCTAGTGGAATTTCCTAATACTTTCCATTATTTTCAGGATATTTATTGTATGTTAACACGGTGCCTGGCTCTGTATAGGCTGCCATACAGGAGCTTTACAAAAAGACACGTTTCTCACTTGAAAATTTCAATCCAAGCAACAGTTAGAGAATCACAGGAGTCCAAAGATTTCTCTTAAAGAACCCCAATTGTGGAAGATAGTGTGGCAATTCCTCACGGATCTAGAACTAGAAATACCATTTGACCCAGCAATCCCATTACTGGGCATATACCCAAAGGATTATAAATCATTCTGCGATAAAGACACATGCACACATGTGTTTCTTGCGGCACTATTCACAATAGCAAAGACTTGGAACCAACCCAAAAGTCCATCAATGATAGACTGGATTATGAAAATGTGGTACATACACACCATGGAATACTATGCAGCCATAAAAAGGATGAGTTTATGTCCTTTGCAGGGACATGGATGAAGCTGGAAACCATCATTCTCAGCAAACTATCAGAAGATCAGAAAACAAAACACCACATGTTCTCACTCATAAGTGGGAGCTGAACAATGAGAACACATGGACACAGGGAGGGGAACGTCGCACACTGGGGCCTGTTTGGGGGTAGGGGGCTAGGGGAGGGATAACATTAGGAGAAATCCCTAATGTAGGTGACAGGTTGATGGGTGCAGCAAACCACCATGGCACATGTATATCTATGTAACAAAACTGCACGTTCTGCACATGTAACCCAGAACTAAAGTATAATTTTTCTTAAAACTAAAAAATAATAATAATAATTGTGAGATTTTTCCAAAGTATGTGGCAGAATCCTGGGATTTCTATTTAAAAACAAGTGGTCATGGTTTTAGATCAGAAAATATGTAGTTTATTTTTGGGATGGACACAATTCATACACATGGACACAGTGAGCTGACCTTGCTTTGCTGCTCTTTCATTAGGCATTGCAGCACAGCAAAGCTCCCAACACACGTTGGCTTACAGAAATCATTTACTCTGATCTTTCTTGGTTAACTGGACTCATGTGGGTAGTAGATCATGTGGTGAGAGTCATGTTTTCTGAAGCGTCTTTTAGTTTGGGATTCCAGAAGATGCTTCACTCATGTCTGACTCAACACGAGGGCTGGGTGAGCACCTACGGGCTGGCTGATTATCTCCTTCCCTACGTCACTTCCCTATTCATGCCTTGAATTCCCTCAAATCATAAGAGTCTCATGGTTCTTGGACTTAAGGAATGATGATTGGCTGCCACCAAAGAAAGCCTTCCAAAAGGCTCAAGGAGAAGGTGAAAGGCATCATAGGACCTGGCTTTGAAAGGCAGACAGTGTCACTTCTATGACATTATATTGATCAAAGTGAGCCATACACAAGCCCAAATCAAAGGGAGGGGGGTTTCATAAGATGTGGTTATTTTGACATGCAGTCATGAGGGAGCATCCTTGGCAACTATTTCCACTCAGTCCTCACTGGCTTACATTTTTCCCATATATCAAAAGCACTAATCCTGTAAGGAACCACTGAAGGCAATACACTTATAGGAACAGGCTCAGATTGTAGTCCAGAATGTTATCATCCAATCAGGATCTGGTGTCCATGAAGTGACTAGAAATTATCATGTGCACTTTCTTTCAACATCTGCACTAAAGAAATACATTATCTGCCCTATACATATTAAACCTACAAGACTAAAACAGATATAGGAAAATGCAGTGAACACACTTATTTTAGAGGGACAAAAGATAATAAACAGCACCCTGAAAGCAAGATGGGTCACCAACAACTCTACTGGAGCCACCCACAAAAAAATCAGCCAGGGTTCTCACCCTTGTTGCATCTGCTCAAACCAGCATCTCCTTATCTGAAAATATTGTCTTGATATGTGCCGCTTCTGTTTCCACAAGTACTTAAAGGATATAGGTCTCATTAAGTTGGACTAAGTGATGCTCCTTGAATTAATAAGCAAGACATCCACTCAGTGAAAGAAACCATGCTAACTCTTTGTACATGAATAAAAATTAAAATAAAGAAACCAATACAAACAAACAAGAACAAAAAAAAAATAGGCATCCATCACTGGTCCACATCTTGCTGAAGCCAACTATACCCGTATTTTCATCTTCTCTGAAAACGTTTCTCTGCAACAGTTGGCTCCACCTTCTACTGTCAGCTTCTTAGAGCTCCTACCAAGTAGGCACAATGACAGCCCTTTTCCCTCTATAAGAGCTGTTGTCATCTGGTTCATAGCACAAATAGAGAACCATTTAGGAGAAGATAAACAACATATTACCTAAAAGCCAAACAATGCAGACTGAAGAGGGTCTCATCAAATAGCCTCTTTGTCTGCAGGGCCTATCACAGGTGTCAATTTGATCATCTTCATCCTCTCTTTCCATGTTCATTGCCACAACTTGTTCAGCTCCAACCTTACTCATGGGTATGCCTCTCTCCTTGCCCTCCTTTAACCTAATATTCATATACCTTGAGGTGTCAATGCACAGTAAATGACACCTTCATGCCTCTCTTTCTTGTTGCTTTCCTGTATGAACTGAAATTCCCTCCGCTGTCACTGGAAAGTCTCAGGTTCTTGTTAGGTACACTTTCCCCCTTATATCATCCCTCTCACAGTTTTGGTCACCGCCCTATTTTGATGATGGATCATAGGCCCCCTCATCAGCATGGAAATCATTTCTTGGGGAATTAGGCAATACTTGCTCTGAGTGGATAAAAACTATCTTGGACCCCATGAGTTTCTGAGCAGAACATGAGATTGATTTCATATCTACATGTCGCTGGACGATGCCCATTGTATTTGCATGACCTACAGTGTCCTACATGCTGCCCCTGCTATACAAACACACAGTGACTGCCTTTTCTTGACTGATATTCACCAGTTCAGTTCAGCTCATATAGGCCATTTGTTACTCTCATCTTTATACCAACTACTACCCTATGCTCATGGTGGCCAGGAGTGATGTCTCAGATAAAAAGAGAAGACATAGAGCCAAGGAACCAATGTTGAACAAACGATTTTTGTTTGCCTCTGGAGACTTACTCAACATCTCTTTCATGCTTGTCAACCCAAGTTCATGGCCTTCTCCCAGCAAATACATCATTGCTGAAAACACAGTAGGTTATTTCCGCAGTCTAAAGCCTTACCTTTTGAAAGGTCTGTAGGGCTTTCCAGTTTTTGCTGAAAGCCCCCAAAGAATAAATCTACTCTTTGCTAACGTTGTCCTTGATGTCCAAGTCTCCCAAAGCTGTTTCTTCTGTGGTGCTGTCCGTGGTGCTGAGCTCTGGTGCGTCTGGGCCTGTATTTGCAAGTGAGGGGCCAATAAGACAGAATTAACAGAGAAATTTTTTGTTTTGTATTTGATAAATCACAGCTCAAAATATGTATGATGAACGGAGCAGGAATTGAATTTGGACTTTCAGATCACAATTTCTGCAGTGCCCTCTCTCACCGTCCTATTCAACATAGTGTTGGAAGTCCTGGCCAGAGCAATCAGGCAAGAGAAAGAAAGAAAGGGCATCCAAACAGAAAGAGAGGTCAAACCATCCCTGTTTGCAGATGTCATGATTCTGTATCTAGAAAACATCATAGTCTCTGCCCAAATAATTCTTAAGCTGGTAAACAACTTGAGCAAAATTTCAGGATACAAAATCAAAGTGCAAAAATTACTAGAACTCCTATATACCAACAACAGCAAAGCATAGGGCCAAATCAGGACTGCAATTCCATTCACAATTGCCATAAAAAGAATAAAATACCTAGGAATACAGCTAACCAGGAGGTGTAAGATTTTTACCATGAAAATTACAAAACACTGCTTAAAATATCAGAGATGACCCAAACAAATGGAAAAACATCCCAGGCTCATAGATAGCAAGAGTCATTATCATTAAAACAGCCATATTGCCCAAAGAAATGTACAGATTCAATGCTATTTCTGTTGAACTACCTATGACATCCTTCATAGAATTAGAAAAAAACTATTTTAAAATACTCATGGAACCAAGAACGAGTTGAATAACCAAGACAGTTCTTAGCAAAAAGAATCAAGCTGGAAACATCATATTACCCAATGTCAAACTGTGTTACAGGGCCACAGTAACAAAACAGTATGGTACTGGTACAAAAACAGACACAGAGACCAGTGGGACAGAAGAGAGAGCCCAGAAATAAGGTCACACACCTACAACCATCATCAGACCCTTCCACTAAGCTGACCAAACAAGCAATGTAAAAAGGATTTCTTATTCAAGAAATGGTTCTGGGTAACTGGCTAACCATATGCAGATGATTGAAACTGAACCCCTTCATCACATTATATAGAAAAATCAACACAAGATGGGTTAAAGACTTAAATGTAAATGCAAAACTATAAAAACCTTGGAAGACTACATAGGCAATACCATTCTGAATGTAGAAATGGACAAAAGTTTTATGACAAAGATGCGAAAAGCAACCTCAAGAAAAGCAAAAATTGACAGTGGATTTATTTAAGCTAAAGAGCTTCTGCACA";
		s = "AAGCCTTCCAAAAGGCTCAAGGAGAAGGTGAAAGGCATCATAGGACCTGGCTTTGAAAGGCAGACAGTGTCACTTCTATGACATTATATTGATCAAAGTGAGCCATACACAAGCCCAAATCAAAGGGAGGGGGGTTTCATAAGATGTGACCACAGTGGAGTGCCCACTGCACTCCAGCCTGGGCAACAATAGTGAGACCTGTCTCTGTTAGAAAAAAATAAAAAAGAAAAAGAAAACTAGGTCCAATGAACACTGAGGAAACAAG";
		nm = new SmithWatermanGotoh(r, s,  4, -14, 14, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		nm = new SmithWatermanGotoh(r, s,  5, -4, 16, 4);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
		r = "TCCCTCCCTTCCTCTTTCTGTCTTTTCTTCCTTCCTTCCTTTCTTTCTCTTTCTTTCTTTCTTTCTTCCTTCCTTCCTTTCTTTCTCTTTCTTTCTTTCTTTCTTCCTTCCTTCCTTTCTTTCTCTTTCTTTCTTTCTTTCTTTCTTTCTTTCTTTCTTTCTTTTCCTCCCTCCCTCCCCTTCCTTCCTTTCTCTTTCTTTCTTCCCTTTCTTTCTCTCTTTCTTTTCTTTCTTTCTTTCTTTTTCTCCCTCCCTCCCTTCCTCCCTCTTCCTTTCTTTCCTTCCTTCCTTCCTTTCTCTCTCTCTCTTTCTTTCTTTTCTTTCTTTCCTTCCTTCTTTTTCAGAGACAAGGTCTTGCTCTGTTGTCCAGGCTGGAGTGCAGTAGCATGATTATAGCTCACTGCTACCTCAAATTCCTGGGCTCAGGTGATCCACCCACTTCAGCTTCCCGAGTGGCTGAGACTACAGGTGTAGGCCACCATGCCAGGATATTCTTCAAAAATTTTTTTGTAGAGATGGGGTCTCACTATGTTGCCCAGGCT";
		s = "TCCCTCCCTTCCTCTTTCTGTCTTTTCTTCCTTCCTTCATTTCTTTCTCTTTCTCTTTCCTTCTTTCTTTCTTTTCCTCCCTCCCTCCCCTTCCTTCCTTTCTCTTTCTTTCTTCCCTTTCTTTCTCTCTTTCTTTTGTCCCTTTCTTTCTCACTTTCTAGTCTTTCATTCTTTATTTTTCTCACTCCCTACCTTCCTCCCTCAACCTTAATTTACATCCATCCATCCATTCACTCTCTATCTTTCTTTATTTTCATAATTTCATTCCTTATTTTTCAAAAAAAATGT";
		nm = new SmithWatermanGotoh(r, s,  4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		nm = new SmithWatermanGotoh(r, s,   4, -14, 14, 1);
//		nm = new SmithWatermanGotoh(r, s,   5, -4, 16, 4);
//		nm = new SmithWatermanGotoh(r, s,   4, -4, 3, 1);
//		nm = new SmithWatermanGotoh(r, s,  15, -14, 9, 4);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		r = "GGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCGACGCAGAAGACGGGTGATTTCTGCATT";
		s = "GGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCAACG";
		nm = new SmithWatermanGotoh(r, s,  5, -4, 16, 4);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		nm = new SmithWatermanGotoh(r, s,   4, -14, 14, 1);
//		nm = new SmithWatermanGotoh(r, s,   5, -4, 16, 4);
//		nm = new SmithWatermanGotoh(r, s,   4, -4, 3, 1);
//		nm = new SmithWatermanGotoh(r, s,  15, -14, 9, 4);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
		
//		r = "TGACTATGTCTTCTAGAATATTTCTAAGAAATTGCCTAAGTCACCACTGCTCACCAAGAGGCCTTTGTTTTTTCCTCTTCTTAACCATGGGAAAGGAATGTAGGAGGGTAGGGAGTGGATATTTTCTAACCTGGAAAAAACTCATTTTACCCTATATAATTTTTTTTAGCAAATTCCTTCTTTGCACTTACTCCACAATCTTTCCAAATTCTCCCAAATGCTCAAGCTTTTAAAAAACAAAAGACAGAAAGA";
//		s = "CTTGCTAGGAGGGTAGGGAGTGGATTAAAAGATTGGACCAAAAAACTCATTTTACCCGGGG";
//		nm = new SmithWatermanGotoh(r, s,  4, -4, -2, 4);	
//		results = nm.traceback();
//		for (String s1 : results) {
//			System.out.println(s1);
//		}
//		nm = new SmithWatermanGotoh(r, s, 5, -4, 16, 14);	
//		results = nm.traceback();
//		for (String s1 : results) {
//			System.out.println(s1);
//		}
//		String seq6 = "AACTTTTCGTCTCCCCGCGGATGCCCTGGACGGCCCTCCACTGCGAGGGCACGGACGCCACACCCAGGGTCTCGTCCTGCAGCGGCCCTGGCCGCCCCGGCCCCGGGCCGCTCGCAACCCCG";
//		String ref6=("ccgcccggcagccacggtgggacgcccgaacccgcccggcagccgcgctg"+
//				"cgaccccgccttcccgcccgcgtcgctccgcgcggggcccgcgccctcac"+
//				"CGTCTCCCaGCGGATGCCCTGGACGGCCCTCCACTGCGAGGGCACGGACG"+
//				"CCACACCCAGGGTCTCGTCCTGCAGCGGCCCTGGCCGCCCCGGCCCCGGG"+
//				"CCGCTCGCAACCCCGactgtcgccagcgccgcaacaccagcgcttcccgc"+
//				"ctggctgagtggccccggctgcgcgcgggttgccatggagacggttccgc"+
//				"cctctcgtgcggacg").toUpperCase();
//		nm = new SmithWatermanGotoh(ref6, seq6, 4, -4, 4, 1);
//		results = nm.traceback();
//		for (String s : results) {
//			System.out.println(s);
//		}
//		nm = new SmithWatermanGotoh(ref6, seq6, 5, -4, 16, 4);
//		results = nm.traceback();
//		for (String s : results) {
//			System.out.println(s);
//		}
//		nm = new SmithWatermanGotoh(ref6, seq6, 5, -4, 1, 1);
//		results = nm.traceback();
//		for (String s : results) {
//			System.out.println(s);
//		}
		
		nm = new SmithWatermanGotoh(r, s, 4, -4, 4, 1);
		System.out.println("rows: " + nm.rows + ", columns: " + nm.columns);
		int counter = 200;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++ ) {
			nm.initializeOld();
		}
		System.out.println("time take to initialize old: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++ ) {
			nm.initialize();
		}
		System.out.println("time take to initialize: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++ ) {
			nm.initializeOld();
		}
		System.out.println("time take to initialize old: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++ ) {
			nm.initializeNew();
		}
		System.out.println("time take to initialize new: " + (System.currentTimeMillis() - start));
		
		
		
		
		
		System.out.println("TESTS PASSED!");
	}

}
