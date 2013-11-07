/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.ma;

public final class MAMappingParameters {
    private final int length;
    private final int possibleMismatches;
    private final int seedStart;

    public MAMappingParameters(int mappingLength,
            int mappingPossibleMismatches, int mappingSeedStart) {
        length = mappingLength;
        possibleMismatches = mappingPossibleMismatches;
        seedStart = mappingSeedStart;
    }

    public int getLength() {
        return length;
    }

    public int getPossibleMismatches() {
        return possibleMismatches;
    }

    public int getSeedStart() {
        return seedStart;
    }

}
