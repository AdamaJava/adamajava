/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.ma;

public final class MaMapping {
    private final String chromosome;
    private final String location;
    private final int mismatchCount;
    private final String quality;
    
    private final int length;
    private final int possibleMismatches;
    private final int seedStart;
    
    

    MaMapping(final String mappingChromosome, final String mappingLocation,
        final int mappingMismatchCount,
        int mappingLength,
        int mappingPossibleMismatches, int mappingSeedStart,
            
        final String mappingQuality) {
        chromosome = mappingChromosome;
        location = mappingLocation;
        mismatchCount = mappingMismatchCount;
        
        length = mappingLength;
        possibleMismatches = mappingPossibleMismatches;
        seedStart = mappingSeedStart;
        quality = mappingQuality;
    }

    public String getChromosome() {
        return chromosome;
    }

    public String getLocation() {
        return location;
    }

    public int getMismatchCount() {
        return mismatchCount;
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
    
    public String getQuality() {
        return quality;
    }
}
