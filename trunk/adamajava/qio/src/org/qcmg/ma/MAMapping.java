/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.ma;

public final class MAMapping {
    private final String chromosome;
    private final String location;
    private final int mismatchCount;
    private final MAMappingParameters parameters;
    private final String quality;

    MAMapping(final String mappingChromosome, final String mappingLocation,
            final int mappingMismatchCount,
            final MAMappingParameters mappingParameters,
            final String mappingQuality) {
        chromosome = mappingChromosome;
        location = mappingLocation;
        mismatchCount = mappingMismatchCount;
        parameters = mappingParameters;
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

    public MAMappingParameters getParameters() {
        return parameters;
    }

    public String getQuality() {
        return quality;
    }

}
