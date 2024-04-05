/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import org.qcmg.common.util.Constants;

/**
 * Alphabetic Genotype Enum
 *
 * @author holmes
 *
 */
public enum GenotypeEnum {

    AA('A', 'A'), AC('A', 'C'), AG('A', 'G'), AT('A', 'T'),
    CC('C', 'C'), CG('C', 'G'), CT('C', 'T'),
    GG('G', 'G'), GT('G', 'T'),
    TT('T', 'T');

    private final char firstAllele;
    private final char secondAllele;

    GenotypeEnum(char c1, char c2) {
        this.firstAllele = c1;
        this.secondAllele = c2;
    }

    // Accessor methods

    public static GenotypeEnum getGenotypeEnum(char c1, char c2) {
        char firstA = c1 <= c2 ? c1 : c2;
        char secondA = c1 <= c2 ? c2 : c1;
        for (GenotypeEnum g : GenotypeEnum.values()) {
            if (g.firstAllele == firstA && g.secondAllele == secondA) {
                return g;
            }
        }
        return null;
    }

    public char getFirstAllele() {
        return firstAllele;
    }

    public char getSecondAllele() {
        return secondAllele;
    }

    public boolean containsAllele(char reference) {
        return firstAllele == reference || secondAllele == reference;
    }

    public boolean isHomozygous() {
        return firstAllele == secondAllele;
    }

    public boolean isHeterozygous() {
        return !isHomozygous();
    }

    public String getDisplayString() {
        return firstAllele + "/" + secondAllele;
    }

    public String getAltAlleleString() {
        return firstAllele + Constants.COMMA_STRING + secondAllele;
    }

    public String getQualifyingAltAlleles(char ref) {

        if (containsAllele(ref)) {

            if (isHomozygous()) {
                return null;
            } else {
                return ((secondAllele == ref) ? firstAllele : secondAllele) + Constants.EMPTY_STRING;
            }

        } else {
            if (isHomozygous()) {
                return firstAllele + Constants.EMPTY_STRING;
            } else {
                return getAltAlleleString();
            }
        }
    }

    public GenotypeEnum getComplement() {
        return switch (this) {
            case AA -> TT;
            case AC -> GT;
            case AG -> CT;
            case AT -> AT;
            case CC -> GG;
            case CG -> CG;
            case CT -> AG;
            case GG -> CC;
            case GT -> AC;
            case TT -> AA;
        };
    }

}
