/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.VcfRecord;

public class ChrPositionComparator implements Comparator<ChrPosition>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
    public static final List<String> contigs = List.of("chr1", "chr2", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9", "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16", "chr17", "chr18", "chr19", "chr20", "chr21", "chr22", "chrX", "chrY", "GL000191.1", "GL000192.1", "GL000193.1", "GL000194.1", "GL000195.1", "GL000196.1", "GL000197.1", "GL000198.1", "GL000199.1", "GL000200.1", "GL000201.1", "GL000202.1", "GL000203.1", "GL000204.1", "GL000205.1", "GL000206.1", "GL000207.1", "GL000208.1", "GL000209.1", "GL000210.1", "GL000211.1", "GL000212.1", "GL000213.1", "GL000214.1", "GL000215.1", "GL000216.1", "GL000217.1", "GL000218.1", "GL000219.1", "GL000220.1", "GL000221.1", "GL000222.1", "GL000223.1", "GL000224.1", "GL000225.1", "GL000226.1", "GL000227.1", "GL000228.1", "GL000229.1", "GL000230.1", "GL000231.1", "GL000232.1", "GL000233.1", "GL000234.1", "GL000235.1", "GL000236.1", "GL000237.1", "GL000238.1", "GL000239.1", "GL000240.1", "GL000241.1", "GL000242.1", "GL000243.1", "GL000244.1", "GL000245.1", "GL000246.1", "GL000247.1", "GL000248.1", "GL000249.1", "chrMT");
    public static final Map<String, Integer> HG37_CONTIGS_MAP;

    public static final Map<String, Integer> HG38_CONTIGS_MAP = Map.ofEntries(Map.entry("1", 1), Map.entry("2", 2), Map.entry("3", 3), Map.entry("4", 4), Map.entry("5", 5), Map.entry("6", 6), Map.entry("7", 7), Map.entry("8", 8), Map.entry("9", 9), Map.entry("10", 10), Map.entry("11", 11), Map.entry("12", 12), Map.entry("13", 13), Map.entry("14", 14), Map.entry("15", 15), Map.entry("16", 16), Map.entry("17", 17), Map.entry("18", 18), Map.entry("19", 19), Map.entry("20", 20), Map.entry("21", 21), Map.entry("22", 22), Map.entry("X", 23), Map.entry("Y", 24), Map.entry("M", 25));

    static {
        Map<String, Integer> temp = new HashMap<>();
        for (int i = 1; i <= 22; i++) {
            temp.put("chr" + i, i);
        }
        temp.put("chrX", 23);
        temp.put("chrY", 24);
        for (int i = 191, j = 25; i <= 249; i++, j++) {
            temp.put("GL000" + i + ".1", j);
        }
        HG37_CONTIGS_MAP = Collections.unmodifiableMap(temp);
    }
    @Override
    public int compare(ChrPosition o1, ChrPosition o2) {
        int chromosomeDiff = COMPARATOR.compare(o1.getChromosome(), o2.getChromosome());
        if (chromosomeDiff != 0) return chromosomeDiff;

        int positionDiff = o1.getStartPosition() - o2.getStartPosition();
        if (positionDiff != 0) return positionDiff;

        return o1.getEndPosition() - o2.getEndPosition();
    }

    /**
     * Creates a ChrPosition comparator that is based on the chromosome name comparator supplied as an argument.
     * Allows the user to be flexible as to how ChrPosition objects are compared
     *
     */
    public static Comparator<ChrPosition> getComparator(Comparator<String> chrNameComp) {

        return Comparator.comparing(ChrPosition::getChromosome, chrNameComp).thenComparingInt(ChrPosition::getStartPosition).thenComparingInt(ChrPosition::getEndPosition);
    }


    /**
     * This method is useful if you have a list of contigs whose order you want to preserve.
     * eg. a sorted bam will in its  header have a list of contigs, and it is possible that you would like to sort chromosome (Strings) based on this order
     * <p>
     * If the list is empty of null, then @link ReferenceNameComparator comparator will be returned.
     *
     */
    public static Comparator<String> getChrNameComparator(List<String> list) {

        return (null == list || list.isEmpty()) ? COMPARATOR : (o1, o2) -> {
            int i1 = list.indexOf(o1);
            int i2 = list.indexOf(o2);
            if (i1 >= 0 && i2 >= 0) {
                return i1 - i2;
            } else if (i1 >= 0) {
                // o1.chr in list but not o2.chr => o1 < o2
                return -1;
            } else if (i2 >= 0) {
                // o2.chr in list but not o1.chr => o2 < o1
                return 1;
            } else {
                // neither o1 nor o2 chr in list => "natural" ordering
                return o1.compareTo(o2);
            }
        };

    }

    /**
     * Returns a comparator for sorting chromosome names based on a map of chromosome names and their corresponding positions.
     * The comparator sorts the chromosome names based on the positions in the map. If a chromosome name is not present in the map,
     * it is treated as having a position of -1 and is sorted after the chromosome names present in the map.
     *
     * @param map a map of chromosome names and their corresponding positions
     * @return a comparator for sorting chromosome names based on the positions in the map
     */
    public static Comparator<String> getChrNameComparator(Map<String, Integer> map) {

        if (null == map || map.isEmpty()) {
            return COMPARATOR;
        }

        return (o1, o2) -> {
            int i1 = map.getOrDefault(o1, -1);
            int i2 = map.getOrDefault(o2, -1);

            if (i1 == -1){
                return (i2 == -1) ? o1.compareTo(o2) : 1;
            }
            if (i2 == -1){
                return -1;
            }
            return i1 - i2;
        };

    }

    /**
     * Return a comparator for VCF records, preserving the order according to the supplied
     * list of contigs. If the CHR value of record A is in the list but that of record B isn't
     * then record A sorts earlier than the record B. If the CHR value of neither A nor B is in
     * the list then the records are sorted according to the "natural" order given by
     * `ChrPositionComparator.compare(o1, o2)`
     */
    public static Comparator<VcfRecord> getVcfRecordComparator(List<String> list) {

        return (null == list || list.isEmpty()) ? null : new Comparator<>() {
            private final ChrPositionComparator chrPosComp = new ChrPositionComparator();

            @Override
            public int compare(VcfRecord o1, VcfRecord o2) {
                ChrPosition o1Pos = o1.getChrPosition();
                ChrPosition o2Pos = o2.getChrPosition();
                int i1 = list.indexOf(o1Pos.getChromosome());
                int i2 = list.indexOf(o2Pos.getChromosome());
                if (i1 >= 0 && i2 >= 0) {
                    // o1 & o2 chr in list => order by chr in list then pos
                    int diff = i1 - i2;
                    if (diff == 0) {
                        diff = o1Pos.getStartPosition() - o2Pos.getStartPosition();
                    }
                    return diff;
                } else if (i1 >= 0) {
                    // o1.chr in list but not o2.chr => o1 < o2
                    return -1;
                } else if (i2 >= 0) {
                    // o2.chr in list but not o1.chr => o2 < o1
                    return 1;
                } else {
                    // neither o1 nor o2 chr in list => "natural" ordering
                    return chrPosComp.compare(o1Pos, o2Pos);
                }

            }
        };
    }

    /**
     * Convenience method to return a VCFRecord comparator based on the GRCh37_ICGC_standard_v2.fa reference file used at QIMRB
     *
     */
    public static Comparator<VcfRecord> getVcfRecordComparatorForGRCh37() {
        return getVcfRecordComparator(contigs);
    }

    /**
     * Convenience method to return a ChrPosition comparator based on the GRCh37_ICGC_standard_v2.fa reference file used at QIMRB
     *
     */
    public static Comparator<ChrPosition> getCPComparatorForGRCh37() {
        return getComparator(getChrNameComparator(HG37_CONTIGS_MAP));
    }

    /**
     * Convenience method to return a ChrPosition comparator based on the GRCh38 reference file used at QIMRB
     *
     */
    public static Comparator<String> getStringComparatorForHG38() {
        return getChrNameComparator(HG38_CONTIGS_MAP);
    }

    /**
     * Returns a comparator for sorting chromosome names, from "1" to "M" (inclusive).
     *
     * The comparator compares chromosome names based on the following rules:
     * - If both names are numeric (e.g., "2", "10"), they are sorted numerically.
     * - If one name is numeric and the other is not, the numeric name is sorted first.
     * - If both names are non-numeric, they are sorted lexicographically.
     *
     * @return the chromosome name comparator
     */
    public static Comparator<String> getChrNameComparatorNoChrsOneToM() {

        return (o1, o2) -> {

            int i1 = Character.isDigit(o1.charAt(0)) ? Integer.parseInt(o1) : -1;
            int i2 = Character.isDigit(o2.charAt(0)) ? Integer.parseInt(o2) : -1;
            if (i1 > -1 && i2 > -1) {
                return i1 - i2;
            }

            if (i1 == -1){
                i1 = o1.equals("X") ? 23 : o1.equals("Y") ? 24 : o1.equals("M") ? 25 : -1;
            }
            if (i2 == -1){
                i2 = o2.equals("X") ? 23 : o2.equals("Y") ? 24 : o2.equals("M") ? 25 : -1;
            }

            if (i1 > -1) {
                if (i2 > -1) {
                    return i1 - i2;
                } else {
                    return -1;
                }
            } else if (i2 > -1) {
                return 1;
            }
            return o1.compareTo(o2);
        };

    }

}
