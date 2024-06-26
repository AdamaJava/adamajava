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
    private final String sequenceA;
    private final String sequenceB;
    private final int rows;
    private final int columns;
    private byte[][] pointerMatrix;
    private short[][] verticalGaps;
    private short[][] horizontalGaps;
    private int bestRow;
    private int bestColumn;
    private static final byte ONE = 1;
    private static final byte STOP = 0;
    private static final byte LEFT = ONE;
    private static final byte DIAGONAL = 2;
    private static final byte UP = 3;
    private static final String GAP = "-";
    private static final String EMPTY = " ";
    private static final String MISMATCH = ".";
    private static final String MATCH = "|";
    private static final String TAB = "";

    public SmithWatermanGotoh(String a, String b, int matchScore, int mismatchScore, float gapOpen, float gapExtend) {

        this.sequenceA = a;
        this.sequenceB = b;
        this.gapOpen = gapOpen;
        this.gapExtend = gapExtend;
        this.matchScore = matchScore;
        this.mismatchScore = mismatchScore;
        this.rows = sequenceA.length() + 1;//i
        this.columns = sequenceB.length() + 1;//j
        align();
    }

    private void align() {
        fillMatrix();
//		traceback();
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

        for (int i = 0; i < columns; i++) {
            queryGapScores[i] = Float.NEGATIVE_INFINITY;
            bestScores[i] = 0;
        }

        float currentAnchorGapScore;// score if yi aligns to a gap after xi
        float totalSimilarityScore;
        float bestScoreDiagonal;

        //keep track of highest score for traceback
        bestRow = 0;
        bestColumn = 0;
        float bestScore = Float.NEGATIVE_INFINITY;

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
                    int gapLength = verticalGaps[row - 1][column] + 1;
                    verticalGaps[row][column] = (short) gapLength;
                } else {
                    //add open score
                    queryGapScores[column] = queryGapOpenScore;
                }

                //calculate horizontal gaps
                referenceGapExtendScore = currentAnchorGapScore - gapExtend;
                referenceGapOpenScore = bestScores[column - 1] - gapOpen;

                if (referenceGapExtendScore > referenceGapOpenScore) {
                    //add extend score
                    currentAnchorGapScore = referenceGapExtendScore;
                    //increase size of gap
                    short gapLength = (short) (horizontalGaps[row][column - 1] + 1);
                    horizontalGaps[row][column] = gapLength;
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
                    pointerMatrix[row][column] = STOP;
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

    private void initialize() {
        pointerMatrix = new byte[rows][columns];
        verticalGaps = new short[rows][columns];
        horizontalGaps = new short[rows][columns];
        for (int row = 0; row < rows; row++) {
            Arrays.fill(verticalGaps[row], ONE);
            Arrays.fill(horizontalGaps[row], ONE);
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
            switch (pointerMatrix[rs][cs]) {
                case LEFT:

                    //horizontal gap
                    int hEnd = horizontalGaps[rs][cs];
                    for (int i = 0; i < hEnd; i++) {
                        alignmentA.append(GAP);
                        gapString.append(EMPTY);
                        alignmentB.append(sequenceB.charAt(--cs)).append(TAB);
                    }
                    break;

                case DIAGONAL:

                    char a = sequenceA.charAt(--rs);
                    char b = sequenceB.charAt(--cs);
                    alignmentA.append(a).append(TAB);
                    alignmentB.append(b).append(TAB);
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
                        alignmentA.append(sequenceA.charAt(--rs)).append(TAB);
                    }
                    break;

                case STOP:
                    run = false;
                    break;
                default: /* do nothing */
                    break;
            }
        }
        return new String[]{alignmentA.reverse().toString(), gapString.reverse().toString(), alignmentB.reverse().toString()};
    }

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
