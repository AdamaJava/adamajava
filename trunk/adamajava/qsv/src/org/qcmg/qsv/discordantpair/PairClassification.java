/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.discordantpair;

public enum PairClassification {
    AAB("AAB"), AAC("AAC"), ABA("ABA"), ABB("ABB"), ABC("ABC"), BAA("BAA"), BAB(
            "BAB"), BAC("BAC"), BBA("BBA"), BBB("BBB"), BBC("BBC"), Cxx("Cxx");

    private String zpType;

    private PairClassification(final String zpType) {
        this.zpType = zpType;
    }

    public String getPairingClassification() {
        return zpType;
    }

}
