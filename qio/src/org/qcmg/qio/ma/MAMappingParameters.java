/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
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
