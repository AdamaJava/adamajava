package org.qcmg.qprofiler.bam;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.qprofiler.summarise.PositionSummary;
import org.qcmg.qprofiler.util.MAPQMatrix;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.qcmg.qvisualise.util.SummaryByCycle;
import org.qcmg.qvisualise.util.SummaryByCycleUtils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;


public class BamSummaryReportTest {

    @Test
    public void testParseRNameAndPos() {
        BamSummaryReport bsr = new BamSummaryReport(new String[]{"matrices", "coverage"}, -1, null, null, null);
        final String rg = "rg1";
        bsr.setReadGroups(List.of(rg));

        String rName = "test";
        int position = 999;
        bsr.parseRNameAndPos(rName, position, rg);
        PositionSummary returnedSummary = bsr.getRNamePosition().get(rName);
        assertEquals(position, returnedSummary.getMax());
        assertEquals(position, returnedSummary.getMin());
        assertEquals(1, returnedSummary.getCoverage().get(0).get());

        // and again - min and max should stay the same, count should increase
        bsr.parseRNameAndPos(rName, position, rg);
        returnedSummary = bsr.getRNamePosition().get(rName);
        assertEquals(position, returnedSummary.getMax());
        assertEquals(position, returnedSummary.getMin());
        assertEquals(2, returnedSummary.getCoverage().get(0).get());

        // add another position to this rName
        position = 1000000;
        bsr.parseRNameAndPos(rName, position, rg);
        returnedSummary = bsr.getRNamePosition().get(rName);
        assertEquals(position, returnedSummary.getMax());
        assertEquals(999, returnedSummary.getMin());
        assertEquals(1, returnedSummary.getRgCoverage().get(1).get(0));

        // add another position to this rName
        position = 0;
        bsr.parseRNameAndPos(rName, position, rg);
        returnedSummary = bsr.getRNamePosition().get(rName);
        assertEquals(1000000, returnedSummary.getMax());
        assertEquals(position, returnedSummary.getMin());
        assertEquals(3, returnedSummary.getRgCoverage().get(0).get(0));
        assertEquals(1, returnedSummary.getRgCoverage().get(1).get(0));

        // add a new rname
        rName = "new rname";
        bsr.parseRNameAndPos(rName, 0, rg);
        returnedSummary = bsr.getRNamePosition().get(rName);
        assertEquals(0, returnedSummary.getMax());
        assertEquals(0, returnedSummary.getMin());
        assertEquals(1, returnedSummary.getRgCoverage().get(0).get(0));
        assertEquals(0, returnedSummary.getRgCoverage().get(0).get(1));
    }

    @Test
    public void testParseCoverageSeparateComponents() {
        BamSummaryReport bsr = new BamSummaryReport(new String[]{"matrices", "coverage"}, -1, null, null, null);
        ConcurrentMap<Integer, AtomicLong> queue = bsr.getCoverageQueue();
        ConcurrentMap<Integer, AtomicLong> map = bsr.getCoverage();

        SummaryReportUtils.addPositionAndLengthToMap(queue, 100, 25);
        SummaryReportUtils.addPositionAndLengthToMap(queue, 100, 50);
        SummaryReportUtils.addPositionAndLengthToMap(queue, 100, 75);
        SummaryReportUtils.addPositionAndLengthToMap(queue, 125, 10);

        assertFalse(queue.isEmpty());
        assertTrue(map.isEmpty());

        bsr.removeCoverageFromQueueAndAddToMap(125, queue, map);

        // should now have an entry in the map
        assertFalse(queue.isEmpty());
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        assertEquals(25, map.get(3).get());

        bsr.cleanUp();
        assertTrue(queue.isEmpty());
        assertEquals(3, map.size());
        // 100 - 135
        assertEquals(35, map.get(3).get());
        // 135 - 150
        assertEquals(15, map.get(2).get());
        // 150 - 175
        assertEquals(25, map.get(1).get());
    }


    @Test
    public void testGenerateMAPQSubMaps() throws Exception {
        BamSummaryReport bsr = new BamSummaryReport(new String[]{"matrices", "coverage"}, -1, null, null, null);

        ConcurrentMap<MAPQMiniMatrix, AtomicLong> mapQCMMatrix = new ConcurrentSkipListMap<>();
        ConcurrentMap<MAPQMiniMatrix, AtomicLong> mapQSMMatrix = new ConcurrentSkipListMap<>();
        ConcurrentMap<MAPQMiniMatrix, AtomicLong> mapQNHMatrix = new ConcurrentSkipListMap<>();
        ConcurrentMap<MAPQMiniMatrix, AtomicLong> mapQZMMatrix = new ConcurrentSkipListMap<>();
        Random r = new Random(); //so it will pass parseRecord

        for (int i = 0; i < 10000; i++) {
            Integer mapQ = r.nextInt(100);

            SAMRecord record = new SAMRecord(new SAMFileHeader());
            record.setMappingQuality(mapQ);
            record.setFlags(83);

            int cm = r.nextInt(12);
            int sm = r.nextInt(102);
//			int length = r.nextInt(50);
            int nh = r.nextInt(100);
            int zm = r.nextInt(100);

            record.setAttribute("CM", cm);
            record.setAttribute("SM", sm);
            record.setAttribute("NH", nh);
            record.setAttribute("ZM", zm);

            MAPQMiniMatrix cmM = new MAPQMiniMatrix(mapQ, cm);
            MAPQMiniMatrix smM = new MAPQMiniMatrix(mapQ, sm);
            MAPQMiniMatrix nhM = new MAPQMiniMatrix(mapQ, nh);
            MAPQMiniMatrix zmM = new MAPQMiniMatrix(mapQ, zm);

            SummaryByCycleUtils.incrementCount(mapQCMMatrix, cmM);
            SummaryByCycleUtils.incrementCount(mapQSMMatrix, smM);
            SummaryByCycleUtils.incrementCount(mapQNHMatrix, nhM);
            SummaryByCycleUtils.incrementCount(mapQZMMatrix, zmM);

            bsr.parseRecord(record);
        }

        Map<MAPQMiniMatrix, AtomicLong> cmMatrix = new TreeMap<>();
        Map<MAPQMiniMatrix, AtomicLong> smMatrix = new TreeMap<>();
        Map<MAPQMiniMatrix, AtomicLong> lengthMatrix = new TreeMap<>();
        Map<MAPQMiniMatrix, AtomicLong> nhMatrix = new TreeMap<>();
        Map<MAPQMiniMatrix, AtomicLong> zmMatrix = new TreeMap<>();

        bsr.generateMAPQSubMaps(cmMatrix, smMatrix, lengthMatrix, nhMatrix, zmMatrix);

        assertEquals(mapQCMMatrix.size(), cmMatrix.size());
        assertEquals(mapQSMMatrix.size(), smMatrix.size());
        assertEquals(100, lengthMatrix.size());    // for length 0 for each mapq value
        assertEquals(mapQNHMatrix.size(), nhMatrix.size());
        assertEquals(mapQZMMatrix.size(), zmMatrix.size());

        assertEquals(mapQCMMatrix.size(), cmMatrix.size());
        for (Entry<MAPQMiniMatrix, AtomicLong> entry : mapQCMMatrix.entrySet()) {
            assertEquals(entry.getValue().get(), cmMatrix.get(entry.getKey()).get());
        }

        assertEquals(mapQSMMatrix.size(), smMatrix.size());
        for (Entry<MAPQMiniMatrix, AtomicLong> entry : mapQSMMatrix.entrySet()) {
            assertEquals(entry.getValue().get(), smMatrix.get(entry.getKey()).get());
        }
        //nh
        assertEquals(mapQNHMatrix.size(), nhMatrix.size());
        for (Entry<MAPQMiniMatrix, AtomicLong> entry : mapQNHMatrix.entrySet()) {
            assertEquals(entry.getValue().get(), nhMatrix.get(entry.getKey()).get());
        }
        //zm
        assertEquals(mapQZMMatrix.size(), zmMatrix.size());
        for (Entry<MAPQMiniMatrix, AtomicLong> entry : mapQZMMatrix.entrySet()) {
            assertEquals(entry.getValue().get(), zmMatrix.get(entry.getKey()).get());
        }
    }


    @Test
    public void parseIntegerAttribute() {
        QCMGAtomicLongArray array = new QCMGAtomicLongArray(100);
        MAPQMatrix matrix = new MAPQMatrix();
        BamSummaryReport.parseIntegerAttribute(null, array, true, matrix, MAPQMatrix.MatrixType.ZM);
        assertTrue(array.isEmpty());
        assertEquals(0, matrix.getMatrixByType(MAPQMatrix.MatrixType.ZM).size());
        BamSummaryReport.parseIntegerAttribute("hello", array, true, matrix, MAPQMatrix.MatrixType.ZM);
        assertTrue(array.isEmpty());
        assertEquals(0, matrix.getMatrixByType(MAPQMatrix.MatrixType.ZM).size());
        BamSummaryReport.parseIntegerAttribute(1, array, true, matrix, MAPQMatrix.MatrixType.ZM);
        assertEquals(1, array.get(1));
        BamSummaryReport.parseIntegerAttribute(10, array, true, matrix, MAPQMatrix.MatrixType.ZM);
        assertEquals(1, array.get(1));
        assertEquals(1, array.get(10));
        assertEquals(2, matrix.getMatrixByType(MAPQMatrix.MatrixType.ZM).size());
    }

    @Test
    public void parseStringAttributeTest() {

        ConcurrentMap<String, AtomicLong> map = new ConcurrentHashMap<>();

        // Scenario 1: null attribute, null map
        BamSummaryReport.parseStringAttribute(null, null);
        assertTrue(map.isEmpty());
        // Scenario 2: null attribute, null map
        BamSummaryReport.parseStringAttribute(null, map);
        assertTrue(map.isEmpty());

        // Scenario 3: Non-String attribute
        BamSummaryReport.parseStringAttribute(123, map);
        // Since the input parameter was not a string, no modification should have been made to the map
        assertEquals(map.size(), 0);

        // Scenario 4: String attribute, null map
        BamSummaryReport.parseStringAttribute("testAttribute", null);
        // Now, the map should contain an entry with key 'testAttribute' with a count of 1
        assertEquals(map.size(), 0);

        // Scenario 5: String attribute, null map
        BamSummaryReport.parseStringAttribute("testAttribute", map);
        assertEquals(map.size(), 1);
    }

    @Test
    public void testCompareWithSAMUtils() {
        String inputString = "!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65";
        String expectedOutputString = "BHHKIIIIKKKLJJFFFLLJIFFFFJORKKKNLKHHJJKKVVddg______dddddddWV";
        int counter = 100000;
        String outputString = null;

        for (int i = 0; i < counter; i++) {
            outputString = StringUtils.addASCIIValueToChar(inputString, 33);
        }
        assertEquals(expectedOutputString, outputString);

        byte[] bytes = inputString.getBytes();
        for (int i = 0; i < counter; i++) {
            outputString = SAMUtils.phredToFastq(bytes);
        }
        assertEquals(expectedOutputString, outputString);

        for (int i = 0; i < counter; i++) {
            outputString = StringUtils.addASCIIValueToChar(inputString, 33);
        }
        assertEquals(expectedOutputString, outputString);
    }

    @Test
    public void testCompareWithSAMUtilsAgain() {
        String inputString = "!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65";
        int counter = 100000;
        ConcurrentMap<Integer, AtomicLong> map;
        ConcurrentMap<Integer, AtomicLong> mapBytes;

        map = new ConcurrentHashMap<>();
        for (int i = 0; i < counter; i++) {
            SummaryReportUtils.tallyQualScoresASCII(inputString, map, 0);
        }
        assertEquals(1, map.size());


        mapBytes = new ConcurrentHashMap<>();
        byte[] bytes = inputString.getBytes();
        for (int i = 0; i < counter; i++) {
            SummaryReportUtils.tallyQualScores(bytes, mapBytes);
        }
        assertEquals(1, mapBytes.size());

        map = new ConcurrentHashMap<>();
        for (int i = 0; i < counter; i++) {
            SummaryReportUtils.tallyQualScoresASCII(inputString, map, 0);
        }
        assertEquals(1, map.size());
        assertEquals(map.keySet(), mapBytes.keySet());
        assertEquals(map.get(0).get(), mapBytes.get(0).get());
    }

    @Ignore
    public void testQualityStringVsByteArray() {
        String inputString = "!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65";
        int counter = 10000;
        SummaryByCycle<Character> qualByCycleORIGINAL = new SummaryByCycle<>();
        SummaryByCycle<Integer> qualByCycleNEW = new SummaryByCycle<>();
        ConcurrentMap<Integer, AtomicLong> lineLengthsORIGINAL;
        ConcurrentMap<Integer, AtomicLong> lineLengthsNEW;

        SAMRecord record = new SAMRecord(null);
        record.setBaseQualityString(inputString);
        lineLengthsNEW = new ConcurrentHashMap<>();
        for (int i = 0; i < counter; i++) {
            byte[] bytes = record.getBaseQualities();
            SummaryByCycleUtils.parseIntegerSummary(qualByCycleNEW, bytes);
            SummaryReportUtils.tallyQualScores(bytes,
                    lineLengthsNEW);
        }

        lineLengthsORIGINAL = new ConcurrentHashMap<>();
        for (int i = 0; i < counter; i++) {
            String quals = record.getBaseQualityString();
            SummaryByCycleUtils.parseCharacterSummary(qualByCycleORIGINAL, StringUtils.addASCIIValueToChar(quals, 33));
            SummaryReportUtils.tallyQualScoresASCII(quals,
                    lineLengthsORIGINAL, 33);
        }

        lineLengthsORIGINAL = new ConcurrentHashMap<>();
        for (int i = 0; i < counter; i++) {
            String quals = record.getBaseQualityString();
            SummaryByCycleUtils.parseCharacterSummary(qualByCycleORIGINAL, StringUtils.addASCIIValueToChar(quals, 33));
            SummaryReportUtils.tallyQualScoresASCII(quals,
                    lineLengthsORIGINAL, 33);
        }
    }
}
