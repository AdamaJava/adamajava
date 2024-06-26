package org.qcmg.common.vcf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;

import gnu.trove.list.TShortList;
import gnu.trove.list.array.TShortArrayList;

public class VcfFileMeta {

    private final ContentType type;
    private final TShortList controlSamplePositions = new TShortArrayList();
    private final TShortList testSamplePositions = new TShortArrayList();
    private final List<String> controlSamples = new ArrayList<>(3);
    private final List<String> testSamples = new ArrayList<>(3);
    private final List<String> controlBamUUIDs = new ArrayList<>(3);
    private final List<String> testBamUUIDs = new ArrayList<>(3);
    private final List<String> donorIDs = new ArrayList<>(3);
    private final Map<String, short[]> callerSamplePositions = new HashMap<>(4);


    public VcfFileMeta(VcfHeader h) {
        if (null == h) {
            throw new IllegalArgumentException("Null VcfHeader obj passed to VcfFileMeta ctor");
        }

        for (final VcfHeaderRecord hr : h.getAllMetaRecords()) {
            if (hr.toString().contains("qControlBamUUID")) {
                controlBamUUIDs.add(StringUtils.getValueFromKey(hr.toString(), "qControlBamUUID"));
            }
            if (hr.toString().contains("qControlSample")) {
                controlSamples.add(StringUtils.getValueFromKey(hr.toString(), "qControlSample"));
            }
            if (hr.toString().contains("qTestBamUUID")) {
                testBamUUIDs.add(StringUtils.getValueFromKey(hr.toString(), "qTestBamUUID"));
            }
            if (hr.toString().contains("qTestSample")) {
                testSamples.add(StringUtils.getValueFromKey(hr.toString(), "qTestSample"));
            }
            if (hr.toString().contains("qDonorId")) {
                donorIDs.add(StringUtils.getValueFromKey(hr.toString(), "qDonorId"));
            }
        }

        VcfHeaderRecord chrLine = h.getChrom();
        if (null == chrLine) {
            throw new IllegalArgumentException("Null CHROM line in VcfHeader obj passed to VCfFileMeta ctor");
        }
        String[] chrLineArray = chrLine.toString().split(Constants.TAB_STRING);
        if (chrLineArray.length <= 8) {
            throw new IllegalArgumentException("Insufficient fields in the CHROM line of the VcfHeader to be able to create VcfFileMeta object: " + chrLine);
        }
        /*
         * first 8 columns are #CHROM  POS     ID      REF     ALT     QUAL    FILTER  INFO
         * We want the next n columns: FORMAT  s1_1	s2_1	s3_1 s1_2 etc
         */
        String[] ffs = Arrays.copyOfRange(chrLineArray, 9, chrLineArray.length);

        int callerId = 1;
        for (short i = 1; i <= ffs.length; i++) {
            String s = ffs[i - 1];

            /*
             * choose caller string based on suffix of sample if there is one, or caller
             * Use lastIndexOf as sample names will contain underscores in some instances, and we are after the final part of the string
             */
            int index = s.lastIndexOf('_');
            String caller = index > -1 ? s.substring(index + 1) : callerId + "";

            if (controlBamUUIDs.stream().anyMatch(s::startsWith)) {
                controlSamplePositions.add(i);
                callerSamplePositions.computeIfAbsent(caller, v -> new short[2])[0] = i;
            }
            if (testBamUUIDs.stream().anyMatch(s::startsWith)) {
                testSamplePositions.add(i);
                callerSamplePositions.computeIfAbsent(caller, v -> new short[2])[1] = i;
                callerId++;
            }
        }

        if (controlSamplePositions.isEmpty() && testSamplePositions.isEmpty()) {
            /*
             * maybe the CHROM line is using the sample names rather than the BAM UUIDs
             */
            callerId = 1;    // reset this
            for (short i = 1; i <= ffs.length; i++) {
                String s = ffs[i - 1];

                /*
                 * choose caller string based on suffix of sample if there is one, or caller
                 *  * Use lastIndexOf as sample names will contain underscores in some instances, and we are after the final part of the string
                 */
                int index = s.lastIndexOf('_');
                String caller = index > -1 ? s.substring(index + 1) : callerId + "";

                if (controlSamples.stream().anyMatch(s::startsWith)) {
                    controlSamplePositions.add(i);
                    callerSamplePositions.computeIfAbsent(caller, v -> new short[2])[0] = i;
                }
                if (testSamples.stream().anyMatch(s::startsWith)) {
                    testSamplePositions.add(i);
                    callerSamplePositions.computeIfAbsent(caller, v -> new short[2])[1] = i;
                    callerId++;
                }
            }

            /*
             * If positions are still empty, throw an exception - we cant continue
             */
            if (controlSamplePositions.isEmpty() && testSamplePositions.isEmpty()) {
                throw new IllegalArgumentException("unable to determine control and test positions from VcfHeader to be able to create VcfFileMeta object: " + chrLine);
            }
        }

        /*
         * single sample
         */
        if (controlSamplePositions.isEmpty()) {
            type = testSamplePositions.size() > 1 ? ContentType.MULTIPLE_CALLERS_SINGLE_SAMPLE : ContentType.SINGLE_CALLER_SINGLE_SAMPLE;
        } else if (testSamplePositions.isEmpty()) {
            type = controlSamplePositions.size() > 1 ? ContentType.MULTIPLE_CALLERS_SINGLE_SAMPLE : ContentType.SINGLE_CALLER_SINGLE_SAMPLE;
        } else {
            if (controlSamplePositions.size() == 1 && testSamplePositions.size() == 1) {
                type = ContentType.SINGLE_CALLER_MULTIPLE_SAMPLES;
            } else {
                type = ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES;
            }
        }
    }

    /**
     * returns 1-based positions of control (array index 0)  and test (array index 1) for all callers.
     */
    public Map<String, short[]> getCallerSamplePositions() {
        return callerSamplePositions;
    }

    public ContentType getType() {
        return type;
    }

    public TShortList getAllControlPositions() {
        return controlSamplePositions;
    }

    public TShortList getAllTestPositions() {
        return testSamplePositions;
    }

    public int getFirstControlSamplePos() {
        return !controlSamplePositions.isEmpty() ? controlSamplePositions.get(0) : -1;
    }

    public int getFirstTestSamplePos() {
        return !testSamplePositions.isEmpty() ? testSamplePositions.get(0) : -1;
    }

    public Optional<String> getFirstTestSample() {
        return testSamples.isEmpty() ? Optional.empty() : Optional.ofNullable(testSamples.getFirst());
    }

    public Optional<String> getFirstControlSample() {
        return controlSamples.isEmpty() ? Optional.empty() : Optional.ofNullable(controlSamples.getFirst());
    }

    public Optional<String> getFirstTestBamUUID() {
        return testBamUUIDs.isEmpty() ? Optional.empty() : Optional.ofNullable(testBamUUIDs.getFirst());
    }

    public Optional<String> getFirstControlBamUUID() {
        return controlBamUUIDs.isEmpty() ? Optional.empty() : Optional.ofNullable(controlBamUUIDs.getFirst());
    }

    public Optional<String> getFirstDonorID() {
        return donorIDs.isEmpty() ? Optional.empty() : Optional.ofNullable(donorIDs.getFirst());
    }

}
