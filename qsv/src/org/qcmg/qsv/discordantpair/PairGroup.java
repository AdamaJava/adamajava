/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.discordantpair;

public enum PairGroup {
    
	AAB("AAB"),
	ABB("ABB"),
    AAC("AAC"), 
    ABA("ABA"),      
    ABC("ABC"), 
    BAA_BBA("BAA_BBA"), 
    BAB_BBB("BAB_BBB"), 
    BAC_BBC("BAC_BBC"),  
    Cxx("Cxx");
    
    private String zpType;

    private PairGroup(final String zpType) {
        this.zpType = zpType;
    }

    public String getPairGroup() {
        return zpType;
    } 
    

}
