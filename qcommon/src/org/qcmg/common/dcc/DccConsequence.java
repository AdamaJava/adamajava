/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
/**
 *
 */
package org.qcmg.common.dcc;

import static java.lang.Integer.MAX_VALUE;

public enum DccConsequence {
    FRAMESHIFT_CODING("frameshift_variant", "Frame_Shift_", true, true, MAX_VALUE, 2, 11, MutationType.INS, MutationType.DEL),

    NON_SYNONYMOUS_CODING_INS("inframe_insertion", "In_Frame_", true, true, MAX_VALUE, 4, 15, MutationType.INS),
    NON_SYNONYMOUS_CODING_DEL("inframe_deletion", "In_Frame_", true, true, MAX_VALUE, 4, 15, MutationType.DEL),
    NON_SYNONYMOUS_CODING_SNP("missense_variant", "Missense_Mutation", false, true, 3, MAX_VALUE, 15, MutationType.SNP),
    NON_SYNONYMOUS_CODING_SNP_2("initiator_codon_variant", "Missense_Mutation", false, true, 3, MAX_VALUE, 15, MutationType.SNP),

    STOP_GAINED("stop_gained", "Nonsense_Mutation", false, true, 1, 1, 21, MutationType.SNP, MutationType.INS, MutationType.DEL),

    SYNONYMOUS_CODING("synonymous_variant", "Silent", false, true, 100, MAX_VALUE, 25, MutationType.SNP),
    SYNONYMOUS_CODING_2("stop_retained_variant", "Silent", false, true, 100, MAX_VALUE, 25, MutationType.SNP),

    THREE_PRIME_UTR_ESSENTIAL_SPLICE_SITE("splice_acceptor_variant", "Splice_Site", false, true, 2, 3, 5, MutationType.SNP, MutationType.INS, MutationType.DEL),
    FIVE_PRIME_UTR_ESSENTIAL_SPLICE_SITE("splice_donor_variant", "Splice_Site", false, true, 2, 3, 6, MutationType.SNP, MutationType.INS, MutationType.DEL),
    SPLICE_SITE("splice_region_variant", "Splice_Site", false, true, 2, 3, 20, MutationType.SNP, MutationType.INS, MutationType.DEL),

    STOP_LOST("stop_lost", "Nonstop_Mutation", false, true, 1, 1, 23, MutationType.SNP, MutationType.INS, MutationType.DEL),

    THREE_PRIME_UTR("3_prime_UTR_variant", "3'UTR", false, false, MAX_VALUE, MAX_VALUE, 1, MutationType.SNP, MutationType.INS, MutationType.DEL),

    DOWNSTREAM("downstream_gene_variant", "3'Flank", false, false, MAX_VALUE, MAX_VALUE, 4, MutationType.SNP, MutationType.INS, MutationType.DEL),

    FIVE_PRIME_UTR("5_prime_UTR_variant", "5'UTR", false, false, MAX_VALUE, MAX_VALUE, 2, MutationType.SNP, MutationType.INS, MutationType.DEL),

    UPSTREAM("upstream_gene_variant", "5'Flank", false, false, MAX_VALUE, MAX_VALUE, 3, MutationType.SNP, MutationType.INS, MutationType.DEL),

    INTERGENIC("intergenic_variant", "IGR", false, false, MAX_VALUE, MAX_VALUE, 13, MutationType.SNP, MutationType.INS, MutationType.DEL),

    INTRONIC("intron_variant", "Intron", false, false, MAX_VALUE - 100, MAX_VALUE - 100, 14, MutationType.SNP, MutationType.INS, MutationType.DEL),
    INTRONIC_NMD_TRANSCRIPT("NMD_transcript_variant", "Intron", false, false, MAX_VALUE, MAX_VALUE, 19, MutationType.SNP, MutationType.INS, MutationType.DEL),

    WITHIN_NON_CODING_GENE("non_coding_exon_variant", "RNA", false, false, MAX_VALUE - 1, MAX_VALUE, -1, MutationType.SNP, MutationType.INS, MutationType.DEL),
    WITHIN_NON_CODING_GENE_2("nc_transcript_variant", "RNA", false, false, MAX_VALUE - 1, MAX_VALUE, -1, MutationType.SNP, MutationType.INS, MutationType.DEL),
    WITHIN_MATURE_miRNA("mature_miRNA_variant", "RNA", false, false, MAX_VALUE, MAX_VALUE, -1, MutationType.SNP, MutationType.INS, MutationType.DEL),


    /*
     * Ensemble v66 version of consequences follows - used by mouse
     */

    FRAMESHIFT_CODING_V66("FRAMESHIFT_CODING", "Frame_Shift_", true, true, MAX_VALUE, 2, 11, MutationType.INS, MutationType.DEL),
    FRAMESHIFT_CODING_SPLICE_SITE_V66("FRAMESHIFT_CODING--SPLICE_SITE", "Frame_Shift_", true, true, MAX_VALUE, 2, 12, MutationType.INS, MutationType.DEL),
    COMPLEX_INDEL_V66("COMPLEX_INDEL", "Frame_Shift_", true, true, MAX_VALUE, 2, 29, MutationType.INS, MutationType.DEL),

    NON_SYNONYMOUS_CODING_INDEL_V66("NON_SYNONYMOUS_CODING", "In_Frame_", true, true, MAX_VALUE, 4, 15, MutationType.INS, MutationType.DEL),
    NON_SYNONYMOUS_CODING_SNP_V66("NON_SYNONYMOUS_CODING", "Missense_Mutation", false, true, 3, MAX_VALUE, 15, MutationType.SNP),

    STOP_GAINED_V66("STOP_GAINED", "Nonsense_Mutation", false, true, 1, 1, 21, MutationType.SNP, MutationType.INS, MutationType.DEL),
    SPLICE_SITE_STOP_GAINED_V66("SPLICE_SITE--STOP_GAINED", "Nonsense_Mutation", false, true, 1, 1, 22, MutationType.SNP, MutationType.INS, MutationType.DEL),

    SYNONYMOUS_CODING_V66("SYNONYMOUS_CODING", "Silent", false, true, 100, MAX_VALUE, 25, MutationType.SNP),

    ESSENTIAL_SPLICE_SITE_INTRONIC_V66("ESSENTIAL_SPLICE_SITE--INTRONIC", "Splice_Site", false, true, 2, 3, 7, MutationType.SNP, MutationType.INS, MutationType.DEL),
    THREE_PRIME_UTR_ESSENTIAL_SPLICE_SITE_V66("3PRIME_UTR--ESSENTIAL_SPLICE_SITE", "Splice_Site", false, true, 2, 3, 5, MutationType.SNP, MutationType.INS, MutationType.DEL),
    FIVE_PRIME_UTR_ESSENTIAL_SPLICE_SITE_V66("5PRIME_UTR--ESSENTIAL_SPLICE_SITE", "Splice_Site", false, true, 2, 3, 6, MutationType.SNP, MutationType.INS, MutationType.DEL),
    NON_SYNONYMOUS_CODING_SPLICE_SITE_V66("NON_SYNONYMOUS_CODING--SPLICE_SITE", "Splice_Site", false, true, 2, 3, 16, MutationType.SNP, MutationType.INS, MutationType.DEL),
    SPLICE_SITE_SYNONYMOUS_CODING_V66("SPLICE_SITE--SYNONYMOUS_CODING", "Splice_Site", false, true, 2, 3, 20, MutationType.SNP, MutationType.INS, MutationType.DEL),
    SPLICE_SITE_V66("SPLICE_SITE", "Splice_Site", false, true, 2, 3, 20, MutationType.SNP, MutationType.INS, MutationType.DEL),

    STOP_LOST_V66("STOP_LOST", "Nonstop_Mutation", false, true, 1, 1, 23, MutationType.SNP, MutationType.INS, MutationType.DEL),
    SPLICE_SITE_STOP_LOST_V66("SPLICE_SITE--STOP_LOST", "Nonstop_Mutation", false, true, 1, 1, 24, MutationType.SNP, MutationType.INS, MutationType.DEL),

    THREE_PRIME_UTR_V66("3PRIME_UTR", "3'UTR", false, false, MAX_VALUE, MAX_VALUE, 1, MutationType.SNP, MutationType.INS, MutationType.DEL),
    THREE_PRIME_UTR_SPLICE_SITE_V66("3PRIME_UTR--SPLICE_SITE", "3'UTR", false, false, MAX_VALUE, MAX_VALUE, 17, MutationType.SNP, MutationType.INS, MutationType.DEL),

    DOWNSTREAM_V66("DOWNSTREAM", "3'Flank", false, false, MAX_VALUE, MAX_VALUE, 4, MutationType.SNP, MutationType.INS, MutationType.DEL),

    FIVE_PRIME_UTR_V66("5PRIME_UTR", "5'UTR", false, false, MAX_VALUE, MAX_VALUE, 2, MutationType.SNP, MutationType.INS, MutationType.DEL),
    FIVE_PRIME_UTR_SPLICE_SITE_V66("5PRIME_UTR--SPLICE_SITE", "5'UTR", false, false, MAX_VALUE, MAX_VALUE, 18, MutationType.SNP, MutationType.INS, MutationType.DEL),

    UPSTREAM_V66("UPSTREAM", "5'Flank", false, false, MAX_VALUE, MAX_VALUE, 3, MutationType.SNP, MutationType.INS, MutationType.DEL),

    INTERGENIC_V66("INTERGENIC", "IGR", false, false, MAX_VALUE, MAX_VALUE, 13, MutationType.SNP, MutationType.INS, MutationType.DEL),

    INTRONIC_V66("INTRONIC", "Intron", false, false, MAX_VALUE - 100, MAX_VALUE - 100, 14, MutationType.SNP, MutationType.INS, MutationType.DEL),
    INTRONIC_SPLICE_SITE_V66("INTRONIC--SPLICE_SITE", "Intron", false, false, MAX_VALUE, MAX_VALUE, 19, MutationType.SNP, MutationType.INS, MutationType.DEL),
    INTRONIC_NMD_TRANSCRIPT_V66("INTRONIC--NMD_TRANSCRIPT", "Intron", false, false, MAX_VALUE, MAX_VALUE, 19, MutationType.SNP, MutationType.INS, MutationType.DEL),

    WITHIN_NON_CODING_GENE_V66("WITHIN_NON_CODING_GENE", "RNA", false, false, MAX_VALUE - 1, MAX_VALUE, -1, MutationType.SNP, MutationType.INS, MutationType.DEL),
    WITHIN_MATURE_miRNA_V66("WITHIN_MATURE_miRNA", "RNA", false, false, MAX_VALUE, MAX_VALUE, -1, MutationType.SNP, MutationType.INS, MutationType.DEL);

    private final String name;
    private final String mafName;
    private final boolean mutationTypeSpecific;
    private final boolean passesFilter;
    private final MutationType[] types;
    private final int snpRank;
    private final int indelRank;


    DccConsequence(String name, String mafName, boolean mutationTypeSpecific, boolean passesFilter, int snpRank, int indelRank, int dccId, MutationType... type) {
        this.name = name;
        this.mafName = mafName;
        this.mutationTypeSpecific = mutationTypeSpecific;
        this.passesFilter = passesFilter;
        this.types = type;
        this.snpRank = snpRank;
        this.indelRank = indelRank;
    }

    public static String getMafName(String name, MutationType type, int mutationType) {
        if (null == name) {
            return null;
        }

        // we haven't defined all the snp types here - so if its a type snp, set to SNP
        MutationType type1 = MutationType.isSubstitution(type) ? MutationType.SNP : type;

        //String returnString = "";
        StringBuilder returnString = new StringBuilder();
        for (DccConsequence dcEnum : values()) {
            String str = (!returnString.isEmpty() ? "," : "");
            if (dcEnum.name.equals(name) && dcEnum.containsType(type1)) {
                if (!dcEnum.mutationTypeSpecific) {
                    //add: (returnString.length() > 0 ? "," : "") + dcEnum.mafName;
                    returnString.append(str).append(dcEnum.mafName);
                } else if (mutationType == 2) {
                    //add: (returnString.length() > 0 ? "," : "") + dcEnum.mafName + "Ins";
                    returnString.append(str).append(dcEnum.mafName).append("Ins");
                } else if (mutationType == 3) {
                    //add: (returnString.length() > 0 ? "," : "") + dcEnum.mafName + "Del";
                    returnString.append(str).append(dcEnum.mafName).append("Del");
                } //others do nothing
            }
        }

        return !returnString.isEmpty() ? returnString.toString() : null;
    }

    public static boolean passesMafNameFilter(String mafName) {
        boolean result;
        if (null != mafName) {
            // mafName could be comprised of multiple names, colon delimited
            String[] mafNames = mafName.split(";");
            for (String mname : mafNames) {

                for (DccConsequence dcEnum : values()) {
                    if (mname.startsWith(dcEnum.mafName)) {
                        result = dcEnum.passesFilter;
                        // return true if one of the mafNames passes the filter
                        if (result) return true;
                    }
                }
            }
        }
        return false;
    }

    public static String getWorstCaseConsequence(MutationType type, String... strings) {

        // we haven't defined all the snp types here - so if its a type snp, set to SNP
        if (MutationType.isSubstitution(type)) type = MutationType.SNP;

        DccConsequence worstConsequence = null;
        if (null == strings || strings.length == 0) return null;

        for (String consequence : strings) {
            for (DccConsequence dcEnum : values()) {
                if (dcEnum.name.equals(consequence) && dcEnum.containsType(type)) {
                    if (null == worstConsequence) {
                        worstConsequence = dcEnum;
                        continue;
                    }

                    if (MutationType.isIndel(type)) {
                        if (dcEnum.indelRank < worstConsequence.indelRank)
                            worstConsequence = dcEnum;

                    } else if (type == MutationType.SNP) {
                        if (dcEnum.snpRank < worstConsequence.snpRank)
                            worstConsequence = dcEnum;
                    }
                }
            }
        }

        return null != worstConsequence ? worstConsequence.name : null;
    }

    private boolean containsType(MutationType type) {
        for (MutationType t : this.types) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

}

