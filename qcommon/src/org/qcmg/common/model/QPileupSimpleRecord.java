/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.concurrent.atomic.AtomicInteger;

public class QPileupSimpleRecord {

    private char ref;
    private AtomicInteger countA;
    private AtomicInteger countT;
    private AtomicInteger countC;
    private AtomicInteger countG;
    private AtomicInteger countN;
    private AtomicInteger countDot;


    public char getRef() {
        return ref;
    }

    public void setRef(char ref) {
        this.ref = ref;
    }

    public AtomicInteger getCountA() {
        if (null == countA)
            countA = new AtomicInteger();
        return countA;
    }

    public AtomicInteger getCountT() {
        if (null == countT)
            countT = new AtomicInteger();
        return countT;
    }

    public AtomicInteger getCountC() {
        if (null == countC)
            countC = new AtomicInteger();
        return countC;
    }

    public AtomicInteger getCountG() {
        if (null == countG)
            countG = new AtomicInteger();
        return countG;
    }

    public AtomicInteger getCountN() {
        if (null == countN)
            countN = new AtomicInteger();
        return countN;
    }

    public AtomicInteger getCountDot() {
        if (null == countDot)
            countDot = new AtomicInteger();
        return countDot;
    }

    public void incrementBase(byte b) {
        switch ((char) b) {
            case 'A':
                getCountA().incrementAndGet();
                break;
            case 'T':
                getCountT().incrementAndGet();
                break;
            case 'C':
                getCountC().incrementAndGet();
                break;
            case 'G':
                getCountG().incrementAndGet();
                break;
            case 'N':
                getCountN().incrementAndGet();
                break;
            case '.':
                getCountDot().incrementAndGet();
                break;
            default:
                throw new IllegalArgumentException("Invalid base supplied: " + b);
        }
    }

    public String getFormattedString() {

        return getBaseCount(countA, 'A') +
                getBaseCount(countT, 'T') +
                getBaseCount(countC, 'C') +
                getBaseCount(countG, 'G') +
                getBaseCount(countN, 'N') +
                getBaseCount(countDot, '.') +
                "\n";
    }

    private String getBaseCount(AtomicInteger ai, char c) {
        if (null == ai) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = ai.get(); i < len; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

}
