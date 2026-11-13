/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.common.sw;

import java.io.IOException;
import java.util.Arrays;

public class SmithWatermanGotoh {

    private final float gapOpen;
    private final float gapExtend;
    private final int matchScore;
    private final int mismatchScore;
    private final char[] sequenceA;
    private final char[] sequenceB;
    private final int rows;
    private final int columns;
    /*
     * Single flat (row * columns + column) matrix of one byte per cell, holding:
     *  - bits 0-1: the traceback direction (STOP, LEFT, DIAGONAL, UP)
     *  - bit 2: set if the vertical gap was extended (rather than opened) at this cell
     *  - bit 3: set if the horizontal gap was extended (rather than opened) at this cell
     *
     * The original implementation kept two additional short[rows][columns] matrices of gap lengths.
     * A gap length at a cell is 1 if the gap was opened there, or 1 + the gap length of the previous cell if it was extended,
     * so it can be recovered at traceback time by counting consecutive extended flags.
     * This cuts the memory used per cell from 5 bytes to 1, which keeps the matrix in cache.
     */
    private byte[] pointerMatrix;
    private int bestRow;
    private int bestColumn;
    private static final byte STOP = 0;
    private static final byte LEFT = 1;
    private static final byte DIAGONAL = 2;
    private static final byte UP = 3;
    private static final byte DIRECTION_MASK = 3;
    private static final byte VERTICAL_GAP_EXTENDED = 4;
    private static final byte HORIZONTAL_GAP_EXTENDED = 8;
    private static final char GAP = '-';
    private static final char EMPTY = ' ';
    private static final char MISMATCH = '.';
    private static final char MATCH = '|';

    public SmithWatermanGotoh(String a, String b, int matchScore, int mismatchScore, float gapOpen, float gapExtend) {

        this.sequenceA = a.toCharArray();
        this.sequenceB = b.toCharArray();
        this.gapOpen = gapOpen;
        this.gapExtend = gapExtend;
        this.matchScore = matchScore;
        this.mismatchScore = mismatchScore;
        this.rows = sequenceA.length + 1;//i
        this.columns = sequenceB.length + 1;//j
        if (gapOpen == (int) gapOpen && gapExtend == (int) gapExtend) {
            fillMatrixInt();
        } else {
            fillMatrix();
        }
    }

    /*
     * Integer version of fillMatrix, used when the gap penalties are whole numbers (as they are for all current callers).
     * All scores are then whole numbers, which float represents exactly (they are far below 2^24),
     * so the comparisons, and therefore the alignment, are identical to the float version, but run faster.
     * Float.NEGATIVE_INFINITY is replaced with a large negative number that can never win a comparison.
     *
     * The inner loop is written to be free of data dependent branches, as the outcomes of its comparisons (match or mismatch, gap opened or extended,
     * traceback direction) are close to random from one cell to the next, and so are mispredicted by the CPU when written as branches:
     *  - match/mismatch scores come from a per-base profile of sequence B (profile[column] = score of the row's base against seqB[column - 1])
     *  - the gap extended flags are taken from the sign bit of (open score - extend score), ie. set if extend > open, as before.
     *    Neither score can be less than (Integer.MIN_VALUE / 2 - gap extend penalty), and the open score is at least -gapOpen, so the subtraction cannot overflow
     *  - the direction is chosen with the same priority as before (STOP if the score is 0, then DIAGONAL, then UP, then LEFT) using selects
     */
    private void fillMatrixInt() {
        final int cols = columns;
        final byte[] pointers = new byte[rows * cols];
        final char[] seqA = sequenceA;
        final char[] seqB = sequenceB;
        final int open = (int) gapOpen;
        final int extend = (int) gapExtend;
        final int minusInfinity = Integer.MIN_VALUE / 2;

        int[] bestScores = new int[cols];
        int[] queryGapScores = new int[cols];
        Arrays.fill(queryGapScores, minusInfinity);

        /*
         * per base profiles of sequence B, created when first needed
         */
        int[][] profiles = new int[128][];
        int[] scratchProfile = null;

        int bRow = 0;
        int bColumn = 0;
        int bestScore = Integer.MIN_VALUE;

        for (int row = 1; row < rows; row++) {
            int currentAnchorGapScore = minusInfinity;
            int bestScoreDiagonal = bestScores[0];
            final char a = seqA[row - 1];
            final int rowOffset = row * cols;
            int[] profile;
            if (a < profiles.length) {
                profile = profiles[a];
                if (null == profile) {
                    profile = profile(a, seqB, cols, null);
                    profiles[a] = profile;
                }
            } else {
                scratchProfile = profile(a, seqB, cols, scratchProfile);
                profile = scratchProfile;
            }

            for (int column = 1; column < cols; column++) {
                final int totalSimilarityScore = bestScoreDiagonal + profile[column];

                //vertical gaps
                final int queryGapExtendScore = queryGapScores[column] - extend;
                final int queryGapOpenScore = bestScores[column] - open;
                final int queryGapScore = Math.max(queryGapExtendScore, queryGapOpenScore);
                final int verticalExtendedFlag = ((queryGapOpenScore - queryGapExtendScore) >>> 31) * VERTICAL_GAP_EXTENDED;
                queryGapScores[column] = queryGapScore;

                //horizontal gaps
                final int referenceGapExtendScore = currentAnchorGapScore - extend;
                final int referenceGapOpenScore = bestScores[column - 1] - open;
                currentAnchorGapScore = Math.max(referenceGapExtendScore, referenceGapOpenScore);
                final int horizontalExtendedFlag = ((referenceGapOpenScore - referenceGapExtendScore) >>> 31) * HORIZONTAL_GAP_EXTENDED;

                bestScoreDiagonal = bestScores[column];
                // same as findMaximum: 0 if all are <= 0, otherwise the max
                final int score = Math.max(0, Math.max(totalSimilarityScore, Math.max(queryGapScore, currentAnchorGapScore)));
                bestScores[column] = score;

                int direction = score == queryGapScore ? UP : LEFT;
                direction = score == totalSimilarityScore ? DIAGONAL : direction;
                direction = score == 0 ? STOP : direction;
                pointers[rowOffset + column] = (byte) (direction | verticalExtendedFlag | horizontalExtendedFlag);

                if (score > bestScore) {
                    bRow = row;
                    bColumn = column;
                    bestScore = score;
                }
            }
        }
        this.pointerMatrix = pointers;
        this.bestRow = bRow;
        this.bestColumn = bColumn;
    }

    /*
     * profile[column] = the score of base a against seqB[column - 1]
     */
    private int[] profile(char a, char[] seqB, int cols, int[] reuse) {
        int[] profile = null != reuse ? reuse : new int[cols];
        for (int column = 1; column < cols; column++) {
            profile[column] = a == seqB[column - 1] ? matchScore : mismatchScore;
        }
        return profile;
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

        final int cols = columns;
        final byte[] pointers = new byte[rows * cols];
        final char[] seqA = sequenceA;
        final char[] seqB = sequenceB;
        final float open = gapOpen;
        final float extend = gapExtend;

        //storage for current calculations
        float[] bestScores = new float[cols];//score if xi aligns to gap after yi
        float[] queryGapScores = new float[cols];//best score of alignment x1..xi to y1..yi
        Arrays.fill(queryGapScores, Float.NEGATIVE_INFINITY);

        //keep track of highest score for traceback
        int bRow = 0;
        int bColumn = 0;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (int row = 1; row < rows; row++) {
            float currentAnchorGapScore = Float.NEGATIVE_INFINITY;// score if yi aligns to a gap after xi
            float bestScoreDiagonal = bestScores[0];
            final char a = seqA[row - 1];
            final int rowOffset = row * cols;

            for (int column = 1; column < cols; column++) {
                final int idx = rowOffset + column;
                float totalSimilarityScore = bestScoreDiagonal + (a == seqB[column - 1] ? matchScore : mismatchScore);

                //calculate vertical/sequenceA gaps
                float queryGapExtendScore = queryGapScores[column] - extend;
                float queryGapOpenScore = bestScores[column] - open;

                byte flags = 0;
                if (queryGapExtendScore > queryGapOpenScore) {
                    //add extend score, and increase size of gap
                    queryGapScores[column] = queryGapExtendScore;
                    flags = VERTICAL_GAP_EXTENDED;
                } else {
                    //add open score
                    queryGapScores[column] = queryGapOpenScore;
                }

                //calculate horizontal gaps
                float referenceGapExtendScore = currentAnchorGapScore - extend;
                float referenceGapOpenScore = bestScores[column - 1] - open;

                if (referenceGapExtendScore > referenceGapOpenScore) {
                    //add extend score, and increase size of gap
                    currentAnchorGapScore = referenceGapExtendScore;
                    flags |= HORIZONTAL_GAP_EXTENDED;
                } else {
                    //add open score
                    currentAnchorGapScore = referenceGapOpenScore;
                }

                //test scores
                bestScoreDiagonal = bestScores[column];
                float queryGapScore = queryGapScores[column];
                float score = findMaximum(totalSimilarityScore, queryGapScore, currentAnchorGapScore);
                bestScores[column] = score;

                //determine trackback direction
                byte direction;
                if (score == 0) {
                    direction = STOP;
                } else if (score == totalSimilarityScore) {
                    direction = DIAGONAL;
                } else if (score == queryGapScore) {
                    direction = UP;
                } else {
                    direction = LEFT;
                }
                pointers[idx] = (byte) (direction | flags);

                //set current cell if this is the best score
                if (score > bestScore) {
                    bRow = row;
                    bColumn = column;
                    bestScore = score;
                }
            }
        }
        this.pointerMatrix = pointers;
        this.bestRow = bRow;
        this.bestColumn = bColumn;
    }

    public String[] traceback() {
        /*
         * the alignment can be at most (sequenceA length + sequenceB length) long
         * build it backwards into char arrays and then create the strings
         */
        int capacity = sequenceA.length + sequenceB.length;
        char[] alignmentA = new char[capacity];
        char[] gapString = new char[capacity];
        char[] alignmentB = new char[capacity];
        int pos = capacity;

        int rs = bestRow;
        int cs = bestColumn;

        boolean run = true;
        while (run) {
            int idx = rs * columns + cs;
            switch (pointerMatrix[idx] & DIRECTION_MASK) {
                case LEFT:

                    //horizontal gap
                    int hEnd = gapLength(idx, 1, HORIZONTAL_GAP_EXTENDED);
                    for (int i = 0; i < hEnd; i++) {
                        pos--;
                        alignmentA[pos] = GAP;
                        gapString[pos] = EMPTY;
                        alignmentB[pos] = sequenceB[--cs];
                    }
                    break;

                case DIAGONAL:

                    char a = sequenceA[--rs];
                    char b = sequenceB[--cs];
                    pos--;
                    alignmentA[pos] = a;
                    alignmentB[pos] = b;
                    gapString[pos] = a == b ? MATCH : MISMATCH;
                    break;

                case UP:
                    //vertical gap
                    int vEnd = gapLength(idx, columns, VERTICAL_GAP_EXTENDED);
                    for (int i = 0; i < vEnd; i++) {
                        pos--;
                        alignmentB[pos] = GAP;
                        gapString[pos] = EMPTY;
                        alignmentA[pos] = sequenceA[--rs];
                    }
                    break;

                case STOP:
                    run = false;
                    break;
                default: /* do nothing */
                    break;
            }
        }
        int length = capacity - pos;
        return new String[]{new String(alignmentA, pos, length), new String(gapString, pos, length), new String(alignmentB, pos, length)};
    }

    /*
     * Length of the gap ending at idx: 1 + the number of consecutive cells (stepping back by step) that have the extended flag set
     */
    private int gapLength(int idx, int step, byte extendedFlag) {
        int length = 1;
        while ((pointerMatrix[idx] & extendedFlag) != 0) {
            length++;
            idx -= step;
        }
        return length;
    }

    private static float findMaximum(float valueA, float valueB, float valueC) {
        if (valueA <= 0 && valueB <= 0 && valueC <= 0) {
            return 0;
        }
        return Math.max(valueA, Math.max(valueB, valueC));
    }

    public static void main(String[] args) throws IOException {
        String sequence1 = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGGTTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCCTAC";
        String sequence2 = "CAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGGTTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCCTAC";

        SmithWatermanGotoh nm = new SmithWatermanGotoh(sequence1, sequence2, 5, -4, 16, 4);
        String[] results = nm.traceback();
        for (String s : results) {
            System.out.println(s);
        }


        String r = "GGTCCACTCTACTCCAATATGATCTCATTTCAATCCTTACCTTAATTATACCTGCAAAGACTCTATTTCCAAATAAGGTCACATTCTGAGGTTCTCGACGAACATGAATTTTGGGTGGGCACTATTCAATCCACTATATAAATTTAACACACCATTTTAGTATTGACCATAACATGTTATAAAATTTCATATCATTGAATTATTTATACATATTATTTGTTAACACGAATCAGT";
        String s = "GGTCCACTCTACTCCAATATGATCTCATTTCAATCCTTACCTTAATTATACCTGCAAAGACTGGTCCACTCTACTCCAATATGATCTCATTTCAATCCTTACCTTAATTATACCTGCAAAGACTGGTCCACTCTACTCCAATATGATCTCATTTCAATCCTTACCTTAATTATA";
        nm = new SmithWatermanGotoh(r, s, 5, -4, 16, 4);
        results = nm.traceback();
        for (String s1 : results) {
            System.out.println(s1);
        }
        nm = new SmithWatermanGotoh(r, s, 4, -14, 14, 1);
        results = nm.traceback();
        for (String s1 : results) {
            System.out.println(s1);
        }
        r = "AAATATATAATCTCAGTTAATCATGTGTTGATACGATGGGTGCAGATAAGGATGTATTAAGGGATATATGATGTGTGCAGATAGGGATATTCTAGATCTATTTTAAATATGTAATAATAGTGGGGGGATTGGTTTATTTTTAGCCTGTTTACCTTAAGAACATATATAATCTCAGTTAATTATGTGTTGATACAATAGGTGAAGATAGGGATATATGAAGGAATCTATGCAAACTCAAGATAATCACACTTAATTTTTTAAAGTGCAAGTGAGAACACAAATCATTTCTAG";
        s = "GGATTGAAAATATTCATGGCATTTGGGAAGAAGAGAAAGGGCAGGTCTTTGCAGTATGCTTCTAGATCTATTTTAAATATGTAATAATAGTGGAGGGATCAGTTTATTTTTAGCCTGTTTACCTTAAGAACATATATAATCTCAGTTAATTATGTGTTGATACAATAGGTGAAGATAGGGATATATGAAGGAATCTATGCAAACTCAAGATAATCACACTTAATTTTTTAAAGTGCAAGTGAGAACACAAATCATTTCTAG";
        nm = new SmithWatermanGotoh(r, s, 5, -4, 16, 4);
        results = nm.traceback();
        for (String s1 : results) {
            System.out.println(s1);
        }
        nm = new SmithWatermanGotoh(r, s, 4, -14, 14, 1);
        results = nm.traceback();
        for (String s1 : results) {
            System.out.println(s1);
        }
        r = "ACTGGTCTCGAACCCCTAACCTCAAGTGATTCACCCACCTTGGCCTCCCAAATTGCTGGGATTACAGGCGTGAGCCAGTATGCCTGGCCACAAGTCATACTTTAAATCACATATGATATTACTTTTAATTACTTTTTTTT";
        s = "ATTACAGGCGGGAGCCACTACTCCTGGCCACAAGTCATACTTTAAATCACATATGATATTACTTTTAATTACTTTTTTTT";
        nm = new SmithWatermanGotoh(r, s, 5, -4, 16, 4);
        results = nm.traceback();
        for (String s1 : results) {
            System.out.println(s1);
        }
        nm = new SmithWatermanGotoh(r, s, 4, -14, 14, 1);
        results = nm.traceback();
        for (String s1 : results) {
            System.out.println(s1);
        }
        r = "TAATGTTGTTCTGTGTGAATTTGATCCTGTCATCATGATTCTAGCTGGTTATTTTGCAGACTTTTTTATGTGGTTGCTTCATAGTGTTACTGATCTGTGTACTTCAATGTGTTTTGGTAGTGGCTGATAATTGTTTTTCATTTCCATATTTAATGCTTCCTTCAGGAGCTCTTGTAAGGCAGGCCTGGTGGTGATGTATTCCCTCAGCATTTGCTTGTCTGAAAAGGATCTTATTTCTCTTTTGTTTATGAAG";
        s = "GAAGGGTGTTCTGTGTGAGGTTGAGGCTGTCGTCATGATGCTAGCTGGTTATTGTGCAGACTTGTTTATGTGGTTGCTTCATAGTGTGACTGATCTGTGTACTTACATGTGTTTTGGTAGTGGCTGATAATTGTTTTTCATTTCCATATTTAATGCTTCCTTCAGGAGCTCTTGTAAGGCAGGCCTGGTTGTGATGTATTCCCTCAGCATTTGCTTGTCTGAAAAGGATCTTATTTCTCTTTTGTTTATGAAG";
        nm = new SmithWatermanGotoh(r, s, 5, -4, 16, 4);
        results = nm.traceback();
        for (String s1 : results) {
            System.out.println(s1);
        }
        nm = new SmithWatermanGotoh(r, s, 4, -14, 14, 1);
        results = nm.traceback();
        for (String s1 : results) {
            System.out.println(s1);
        }
    }
}
