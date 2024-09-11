package org.qcmg.coverage;

import java.util.*;

public class LowCoverageRegionComparator implements Comparator<LowCoverageRegion> {
    private final Map<String, Integer> refNameOrder;

    public LowCoverageRegionComparator(LinkedHashSet<String> refNames) {
        refNameOrder = new HashMap<>();
        List<String> refNameList = new ArrayList<>(refNames);
        for (int i = 0; i < refNameList.size(); i++) {
            refNameOrder.put(refNameList.get(i), i);
        }
    }

    @Override
    public int compare(LowCoverageRegion r1, LowCoverageRegion r2) {
        int refNameComparison = Integer.compare(refNameOrder.getOrDefault(r1.getRefName(), Integer.MAX_VALUE),
                refNameOrder.getOrDefault(r2.getRefName(), Integer.MAX_VALUE));

        if (refNameComparison != 0) {
            return refNameComparison;
        }

        int startComparison = Integer.compare(r1.getStart(), r2.getStart());
        if (startComparison != 0) {
            return startComparison;
        }

        return Integer.compare(r1.getEnd(), r2.getEnd());
    }
}


