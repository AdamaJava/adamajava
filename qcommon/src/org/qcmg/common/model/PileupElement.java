/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.ArrayList;
import java.util.Collection;


public class PileupElement implements Comparable<PileupElement> {

    private char base;
    private int forwardCount;
    private int reverseCount;
    private final Collection<Byte> forwardQualities = new ArrayList<>();
    private final Collection<Byte> reverseQualities = new ArrayList<>();

    public PileupElement(char base) {
        this.base = base;
    }

    public PileupElement(byte a) {
        this((char) a);
    }

    public char getBase() {
        return base;
    }

    public void setBase(char base) {
        this.base = base;
    }

    public int getForwardCount() {
        return forwardCount;
    }

    /**
     * If b is set to Byte.MIN_VALUE, then don't add to the quality
     * this is used when we are dealing with bases that don't have quality info
     */
    public void incrementForwardCount(byte b) {
        forwardCount++;
        if (b != Byte.MIN_VALUE) {
            addForwardQuality(b);
        }
    }

    public void incrementForwardCount() {
        forwardCount++;
    }

    public int getReverseCount() {
        return reverseCount;
    }

    /**
     * If b is set to Byte.MIN_VALUE, then don't add to the quality
     * this is used when we are dealing with bases that don't have quality info
     */
    public void incrementReverseCount(byte b) {
        reverseCount++;
        if (b != Byte.MIN_VALUE)
            addReverseQuality(b);
    }

    public void incrementReverseCount() {
        reverseCount++;
    }

    public int getTotalCount() {
        return forwardCount + reverseCount;
    }

    public boolean isFoundOnBothStrands() {
        return forwardCount > 0 && reverseCount > 0;
    }

    @Override
    public int compareTo(PileupElement o) {
        // only interested in the total count for the purposes or ordering
        return o.getTotalCount() - getTotalCount();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PileupElement other = (PileupElement) obj;
        if (base != other.base)
            return false;
        if (forwardCount != other.forwardCount)
            return false;
        if (!forwardQualities.equals(other.forwardQualities))
            return false;
        if (reverseCount != other.reverseCount)
            return false;
        return reverseQualities.equals(other.reverseQualities);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + base + forwardCount + reverseCount;
        result += forwardQualities.hashCode();
        result += reverseQualities.hashCode();
        return result;
    }

    public void addForwardQuality(byte b) {
        forwardQualities.add(b);
    }

    public void addReverseQuality(byte b) {
        reverseQualities.add(b);
    }

    public int getTotalForwardQualityScore() {
        int total = 0;
        for (Byte b : forwardQualities) {
            total += b;
        }
        return total;
    }

    public int getTotalReverseQualityScore() {
        int total = 0;
        for (Byte b : reverseQualities) {
            total += b;
        }
        return total;
    }

    public int getTotalQualityScore() {
        return getTotalForwardQualityScore() + getTotalReverseQualityScore();
    }

    public Collection<Byte> getForwardQualities() {
        return forwardQualities;
    }

    public Collection<Byte> getReverseQualities() {
        return reverseQualities;
    }

    public Collection<Byte> getQualities() {
        Collection<Byte> allQuals = new ArrayList<>();
        allQuals.addAll(forwardQualities);
        allQuals.addAll(reverseQualities);
        return allQuals;
    }

}
