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
	
	private final float gapOpen;
	private final float gapExtend;
	private final int matchScore;
	private final int mismatchScore;
	private final String sequenceA;
	private final String sequenceB;
	private final int rows;
	private final int columns;
	private int[][] pointerMatrix;
	private short[][] verticalGaps;
	private short[][] horizontalGaps;
	private int bestRow;
	private int bestColumn;
	private float bestScore;
	private static final int STOP = 0;
	private static final int LEFT = 1;	
	private static final int DIAGONAL = 2;
	private static final int UP = 3;
	private static final char GAP = '-';
	private static final char EMPTY = ' ';
	private static final char MISMATCH = '.';
	private static final char MATCH = '|';
//	private static final String GAP = "-";
//	private static final String EMPTY = " ";
//	private static final String MISMATCH = ".";
//	private static final String MATCH = "|";
//	private static final String TAB = "";
	
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
		
		initialize();
		
		//storage for current calculations
		float[] bestScores = new float[columns];//score if xi aligns to gap after yi		
		float[] queryGapScores = new float[columns];//best score of alignment x1..xi to y1..yi
		Arrays.fill(queryGapScores, Float.NEGATIVE_INFINITY);
//		for (int i = 0; i < columns; i++) {
//			queryGapScores[i] = Float.NEGATIVE_INFINITY;
//			bestScores[i] = 0;
//		}
		
		float currentAnchorGapScore;// score if yi aligns to a gap after xi
		float totalSimilarityScore;		
		float bestScoreDiagonal;		
		
		//keep track of highset score for traceback
		bestRow = 0;
		bestColumn = 0;
		bestScore = Float.NEGATIVE_INFINITY;
		
		float simScore, queryGapExtendScore, queryGapOpenScore, referenceGapExtendScore, referenceGapOpenScore;
		
		for (int row = 1; row < rows; row++) {		
			currentAnchorGapScore = Float.NEGATIVE_INFINITY;
			bestScoreDiagonal = bestScores[0];
			for (int column = 1; column < columns; column++) {
				simScore = findSimilarity(row, column);
				totalSimilarityScore = bestScoreDiagonal + simScore;								
				
				//calculate vertical/sequenceA gaps
				queryGapExtendScore = queryGapScores[column] - gapExtend;
				queryGapOpenScore = bestScores[column] - gapOpen;
				
				if (queryGapExtendScore > queryGapOpenScore) {
					//add extend score
					queryGapScores[column] = queryGapExtendScore;
					//increase size of gap
					verticalGaps[row][column] = (short) (verticalGaps[row - 1][column] + 1);  
				} else {
					//add open score
					queryGapScores[column] = queryGapOpenScore; 
				}
				
				//calculate horizontal gaps
				referenceGapExtendScore = currentAnchorGapScore - gapExtend;
				referenceGapOpenScore = bestScores[column-1] - gapOpen;
				
				if (referenceGapExtendScore > referenceGapOpenScore) {
					//add extend score
					currentAnchorGapScore = referenceGapExtendScore;
					//increase size of gap					
					horizontalGaps[row][column] = (short) (horizontalGaps[row][column - 1] + 1);	
				} else {
					//add open score
					currentAnchorGapScore = referenceGapOpenScore; 
				}
				
				//test scores
				bestScoreDiagonal = bestScores[column];
				bestScores[column] = findMaximum(totalSimilarityScore, queryGapScores[column], currentAnchorGapScore);
				
				//determine trackback direction
				float score = bestScores[column];
				if (score == 0) {
					/*
					 * don't think we need to set this as it should already be zero
					 */
//					pointerMatrix[row][column] = STOP;
				} else if (score == totalSimilarityScore) {
					pointerMatrix[row][column] = DIAGONAL;
				} else if (score == queryGapScores[column]) {
					pointerMatrix[row][column] = UP;
				} else {
					pointerMatrix[row][column] = LEFT;
				}
				
				//set current cell if this is the best score
				if (score > bestScore) {
					bestRow = row;
					bestColumn = column;
					bestScore = score;
				}				
			}
		}
	}
	
//	public void afterInitialiseState() {
//		System.out.println("pointerMatrix: " + Arrays.deepToString(pointerMatrix));
//		System.out.println("verticalGaps: " + Arrays.deepToString(verticalGaps));
//		System.out.println("horizontalGaps: " + Arrays.deepToString(horizontalGaps));
//	}

	private void initialize() {
		pointerMatrix = new int[rows][columns];
		verticalGaps = new short[rows][columns];
		horizontalGaps = new short[rows][columns];
		
		for (int row = 0; row < rows; row++) {
			Arrays.fill(verticalGaps[row], (short) 1);
			Arrays.fill(horizontalGaps[row], (short) 1);
		}		
	}
//	private void initialize() {
//		pointerMatrix = new int[rows][columns];
//		verticalGaps = new short[rows][columns];
//		horizontalGaps = new short[rows][columns];
//		for (int i = 0; i < rows; i++) {
//			pointerMatrix[i][0] = STOP;
//			if (i == 0) {
//				for (int j = 0; j < columns; j++) {
//					pointerMatrix[0][j] = STOP;
//				}
//			}
//		}		
//		
//		for (int row = 0; row < rows; row++) {
//			for (int column = 0; column < columns; column++) {
//				verticalGaps[row][column] = 1;
//				horizontalGaps[row][column] = 1;
//			}
//		}		
//	}
	
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
//		String sequence1 = "GAATTCAG";
//		String sequence2 = "GGATCGA";
//		String sequence1 = "TTGGGCTAAA";
//		String sequence2 = "TTGGGAACTAAA";
//		String sequence1 = "ATCGCTTTATGTTTTTGGTTATTGGTTTTTTTGTATAGACCAAAGCAAAGAAAATAACAATAACACAGATGCGTTGGAGGCTGTTTAGGGGAGTGGGGTGGGAAAGTTGAGGGGCTTCCCTAGCGGCCTGGCGCCCTCTTTGCTGGGTCCTGCGGGTCCTCAGGGCTGCCTTGCATGTGGGAAGGACTAGAAGAGGCAAGCTGGGGAGCCAGGAGTGTTGGGGGA";
//		String sequence2 = "ATCGCTTTATGTTTTTGGTTATTGGTTTTTTTGTATAGACCAAAGCAAAGAAAATAAAAATAACACAGATGCGTTGGAGGCTGTTTAGGGGAGTGGGGTGGGAAAGTTGAGGGGCTTCCCTAGCGGCCTGGCGCCCTCTTTGCTGGGTCCTGCTGGTCCTCAGGGCTGCCTTGCATGTGGGAAGGACTAGAAGAGGCAAGCTGGGGAGCCAGGAGTGTTGGGGGA";
		String sequence1 = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGGTTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCCTAC";
		String sequence2 = "CAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGGTTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCCTAC";
//		String sequence1 = "CGATTTCTTGATCACATAGACTTCCATTTTCTACTTTTTCTGAGGTTTCCTCTGGTCCTGGTATGAAGAATGTATTTACCCAAAAGTGAAACATTTTGTCCTAAAAAAAAAAAAAAAGAAAAGAAAAAGAAATGAAATGACATATTTAATTAATGATGTTTTATTTTTTTAAAAAAGAAAATCTGTCACCTATGTTAAACATTTGCAAAAAGTCAACAAAATAAAC";
//		String sequence2 = "CGATTTCTTGATCACATAGACTTCCATTTTCTACTTTTTCTGAGGTTTCCTCTGGTCCTGGTATGAAGAATGTATTTACCCAAAAGTGAAACATTTTGTGCGAAAAAAAAAAAAGAAAAGAAAAAGAAATGAAATGACATATTTAATTAATGATGTTTTATTTTTTTAAAAAAGAAAATCTGTCACCTATGTTAAACATTTGCAAAAAGTCAACAAAATAAAC";
//		String sequence1 = "AAATATGCTTCACTTCAGAAGACATTTTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
//		String sequence2 = "GAATATGCTTCACTTCAGAAGACATTTTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";

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
		
		SmithWatermanGotoh nm = new SmithWatermanGotoh(ref2, seq, 4, -4, 4, 1);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		String [] results = nm.traceback();
		for (String s : results) {
			System.out.println(s);
		}
		nm = new SmithWatermanGotoh(ref2, seq, 5, -1, 1, 1);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
		for (String s : results) {
			System.out.println(s);
		}
		nm = new SmithWatermanGotoh(ref, seq, 5, -1, 1, 2);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
		for (String s : results) {
			System.out.println(s);
		}
		String seq5 = "GCCTATTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGTCGGCAGCCGGCGGCCGTGGCCGGGCCGCGAGGTCTGGGACGGGGGGCG";
		String ref5 = "ATCCTCTGGGTCAGTTTCTGTGGAATAAACTGTAAAGGCAAACGGTGGGACGCCCGAACCCGCCCGGCAGCCACGGTGGGACGCCCGAACCCGCCCGGCAGCCACGGTGGGACGCCCGAACCCGCCCGGCAGCCGCGCTGCGACCCCGCCTTCCCGCCCGCGTCGCTCCGCGCGGGGCCCGCGCCCTCACCGTCTCCCAGCGGATGCCCTGGACGGCCCTCCACTGCGAGGGCACGGACGCCACACCCAGGGTCTCGTCCTGCAGCGGCCCTGGCCGCCCCGGCCCCGGGCCGCTCGCAACCCCGACTGTCGCCAGCGCCGCAACACCAGCGCTTCCCGCCTGGCTGAGTGGCCCCGGCTGCGCGCGGGTTGCCATGGAGACGGTTCCGCCCTCTCGTGCGGACGCACTCAGGCGCGACCTCCGCCCCTACGCCGCCATGAGCGGAAAACGGGGAATGTGAGGCTGACGGCGCCATGTTTGAATTGGTCGCAGCGCCTCCTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGGCGGCAGCCTGCGGCCGTGGCCGGCCCGCGAGGTCTGGGCCTGGGAGCGCAGATCTGGCTGAGCAGCTGATTCTTCCAGCAGATCCGAGAACCGCGCCACTCGAACAGGCTGTCACCTCGAAGCCAAGTGATCTCCCTTTAATCCTCAGTCTCTGTCTCAATAGAATGATGATGAAAAATATTCCTACGGAGATCCTAAATGTGGTTAAAAAGCTGGGAGGCCCAAATGAGGTCTTCTACAGAAACATGCTTATTAAATCGAAATTAAAAGCAAATAACTAAATAATGTGCCTACTGTGTGTCAGGCAAAGAGCCCAGAACGTCTCTTTCTTATTAAGTTCTCACCCTGAGGAAAGAATTGCTTTTTATACTATATTATGTCGTTCTGCCATTCCGCGCGCCCCACCGGAATTAAAAAAACTGGAGATTCTGACCTGAGGACTAAAAAATAAAAATAAAGAATAAACAAATAGGCCAGGTGCGGTGGCTCACACCTGTAATCCCGGTACTTTGAGAGGCCAAGGCAGGAGGATCGCTTGAAGCCAGGAGTTCGAA";
		nm = new SmithWatermanGotoh(ref5, seq5, 2, -5, 5, 1);
//			SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
		for (String s : results) {
			System.out.println(s);
		}
		nm = new SmithWatermanGotoh(ref5, seq5, 5, -4, 16, 4);
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);	
		results = nm.traceback();
		for (String s : results) {
			System.out.println(s);
		}
		
		String r = "ACCATGGTATTTGTGTATCTAAACATACCTAAACATAGAAAAGGTACAGTAAAAATACAGTATTATAATCTTATGGGACCACCATCATATATGTGGTCGTCATTGACTGAGACATCATTAATGTGGTGCATGACTACTCCAATCAGTCCAGGAACAAATTAAAAAGATAAGGAGAAAAGTCAGTGCTTTGAGGCTCCACAACACCTTGCTGTGTCCATTTAGAGCAATTTACAGCTGTTGCTGTAATTAATGAAGCTATCTCCTCCCAGGCAAAGCCTTTGGTTGTGTTGGAGGGTACATCGCCAGCACGAGTTCTCTGATTGACACCGTACGGTCCTATGCTGCTGGCTTCATCTTCACCACCTCTCTGCCACCCATGCTGCTGGCTGGAGCCCTGGAGTCTGTGCGGATCCTGAAGAGCGCTGAGGGACGGGTGCTTCGCCGCCAGCACCAGCGCAACGTCAAACTCATGAGACAGATGCTAATGGATGCCGGCCTCCCTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGGGTAATGGCCTGTCTCTGATTGGACTTGCCGTGGGGTGTGCCTCTACACATGATGTACGGATGTTCTGCTTCATACCTTCCTGAAGTTGGGCTTGAGCGGGGTGACTGCCAGGGCAGGGGTTGTAGCCAGCCACCCTCTGTCATGTTTCCGCCATTGGCTGACTTCACCAAGAGAAGAAAGCCTTTGAACCCAGCAGGCTGGGGCAGAAGTTCCCTCTCCGGAGCACTGACCTTAACAGGGTAAACACAGAGCTTGTATCTAGAAAGCTCCAGAAGCCTGAGCTTGGCCAGCTTTGAAGTATGGCTTTCTACTTAGTAAATTTCAAAATAGGTTTTGCCCTTCCCACTACAAATGGTAGCACTGTTGATGTCACAGTTGAATTAGTGTAATGAATACAGCTAGTATAACTGAATCTAGATTATACATCGTGGGTATGAGAGTCTGCTGGTACGAACAGAACCAGTGTTTTCTGATTAAAAATGTATTTCTTTTTAATAAGGTTTTGGTTCCCTGGTGTTCACGAAACAACACTGGCTTCTTTTAAATGACAGGTGTTTGGGCAGCGCTTTCCCTCTGCCCCAAGCTTGCATGTGTTGCTACAGTCTGGTCTTGAGCCTGAGCGTTGTGGGGACTGCGTTCGTTAGGATCTCTGCTAAGAGGTAGTCCTTCCTGTTGTGACCTTACCTTCTGCTCTCATTGAACTTAGGTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACAC";
		String s = "CTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGGGTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACACCCCAGATGATGAACTACTTCCTTGGTGAGTACCTGGGGAGCTGCTGGTGCCTCACTGAGGAGTTGCATAAAGCTGTCTTTGCAGTGTTTATAATTGAAGCCCTTCGGAGGGCTTCAGATTTGTTTCTTCTTCTTTTTTTATTTTTTTTTTTTTTTCCATTATTTTCGTTCTTTTTTCCCTTCCTTGGTTTTTTTTGCCCAATCCCT";
		nm = new SmithWatermanGotoh(r, s, 4, -4, 4, 1);
		results = nm.traceback();
		for (String s1 : results) {
			System.out.println(s1);
		}
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
	}

}
