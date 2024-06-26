/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 * <p>
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 * <p>
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 * <p>
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 * <p>
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.bam;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import htsjdk.samtools.*;


import org.qcmg.common.model.CigarStringComparator;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.KmersSummary;
import org.qcmg.qprofiler.summarise.PositionSummary;
import org.qcmg.qprofiler.summarise.ReadGroupSummary;
import org.qcmg.qprofiler.util.FlagUtil;
import org.qcmg.qprofiler.util.MAPQMatrix;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.qcmg.qvisualise.util.SummaryByCycle;
import org.qcmg.qvisualise.util.SummaryByCycleNew2;
import org.qcmg.qvisualise.util.SummaryByCycleUtils;
import org.qcmg.qprofiler.util.MAPQMatrix.MatrixType;
import org.w3c.dom.Element;

public class BamSummaryReport extends SummaryReport {

    private long failedVendorQualityCheckCount = 0;
    private long secondaryCount = 0;
    private long supplementaryCount = 0;

    private final static int mateRefNameMinusOne = 255;
    public final static int errMDReadLimit = 100;
    private long errMDReadNo = 0;

    private static final Character c = Character.MAX_VALUE;
    private static final Integer i = Integer.MAX_VALUE;
    //SEQ
    private final SummaryByCycleNew2<Character> seqByCycle = new SummaryByCycleNew2<>(c, 512);
    private Map<Integer, AtomicLong> seqLineLengths = null;
    private final QCMGAtomicLongArray seqBadReadLineLengths = new QCMGAtomicLongArray(128);

    //QUAL
    private final SummaryByCycleNew2<Integer> qualByCycleInteger = new SummaryByCycleNew2<>(i, 512);
    private Map<Integer, AtomicLong> qualLineLengths = null;
    private final QCMGAtomicLongArray qualBadReadLineLengths = new QCMGAtomicLongArray(128);

    // TAGS
    private final SummaryByCycleNew2<Character> tagCSByCycle = new SummaryByCycleNew2<>(c, 512);
    private Map<Integer, AtomicLong> tagCSLineLengths = null;
    private final QCMGAtomicLongArray csBadReadLineLengths = new QCMGAtomicLongArray(128);

    private final SummaryByCycleNew2<Integer> tagCQByCycle = new SummaryByCycleNew2<>(i, 512);
    private Map<Integer, AtomicLong> tagCQLineLengths = null;
    private final QCMGAtomicLongArray cqBadReadLineLengths = new QCMGAtomicLongArray(128);

    private final ConcurrentMap<String, AtomicLong> tagRGLineLengths = new ConcurrentHashMap<>();
    private final QCMGAtomicLongArray tagZMLineLengths = new QCMGAtomicLongArray(2048);
    private final QCMGAtomicLongArray tagCMLineLengths = new QCMGAtomicLongArray(128);
    private final QCMGAtomicLongArray tagSMLineLengths = new QCMGAtomicLongArray(256);
    private final QCMGAtomicLongArray tagNHLineLengths = new QCMGAtomicLongArray(1024);
    private final QCMGAtomicLongArray tagIHLineLengths = new QCMGAtomicLongArray(1024);
    private final SummaryByCycleNew2<Character> tagMDMismatchByCycle = new SummaryByCycleNew2<>(c, 512);
    private final QCMGAtomicLongArray mdRefAltLengthsForward = new QCMGAtomicLongArray(32);
    private final QCMGAtomicLongArray mdRefAltLengthsReverse = new QCMGAtomicLongArray(32);
    private final QCMGAtomicLongArray allReadsLineLengths = new QCMGAtomicLongArray(1024);

    // new tags for JP
    private final ConcurrentMap<String, AtomicLong> tagZPLineLengths = new ConcurrentHashMap<>();
    private final QCMGAtomicLongArray tagZFLineLengths = new QCMGAtomicLongArray(128);

    // mapqMatrix
    private final ConcurrentMap<Integer, MAPQMatrix> mapQMatrix = new ConcurrentSkipListMap<>();
    private final SummaryByCycle<Integer> zmSmMatrix = new SummaryByCycle<>(128);
    private final QCMGAtomicLongArray MRNMLengths = new QCMGAtomicLongArray(mateRefNameMinusOne + 1);
    private final ConcurrentMap<String, AtomicLong> cigarValuesCount = new ConcurrentHashMap<>();
    private final QCMGAtomicLongArray mapQualityLengths = new QCMGAtomicLongArray(256);

    // FLAGS
    private final Map<String, AtomicLong> flagBinaryCount = new ConcurrentSkipListMap<>();
    private final QCMGAtomicLongArray flagIntegerCount = new QCMGAtomicLongArray(2048);

    // Coverage
    private final ConcurrentMap<Integer, AtomicLong> coverage = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<Integer, AtomicLong> coverageQueue = new ConcurrentSkipListMap<>();
    private final ConcurrentMap<String, PositionSummary> rNamePosition = new ConcurrentHashMap<>(85);

    private final QCMGAtomicLongArray cigarLengths = new QCMGAtomicLongArray(1024);
    private final QCMGAtomicLongArray p1Lengths = new QCMGAtomicLongArray(1024);
    private final QCMGAtomicLongArray p2Lengths = new QCMGAtomicLongArray(1024);
    private final ConcurrentMap<String, ConcurrentSkipListMap<String, AtomicLong>> additionalTags = new ConcurrentSkipListMap<>();
    private final ConcurrentMap<String, QCMGAtomicLongArray> additionalIntegerTags = new ConcurrentSkipListMap<>();
    private final ConcurrentMap<String, ConcurrentSkipListMap<Character, AtomicLong>> additionalCharacterTags = new ConcurrentSkipListMap<>();

    //Xu Code: each RG softclips, hardclips, read length;
    private final ConcurrentMap<String, ReadGroupSummary> rgSummaries = new ConcurrentHashMap<>();

    {
        rgSummaries.put(SummaryReportUtils.All_READGROUP, new ReadGroupSummary(SummaryReportUtils.All_READGROUP));
    }

    private final KmersSummary kmersSummary = new KmersSummary(KmersSummary.MAX_KMERS); //default use biggest mers length

    // custom tags
    private final short ZM = SAMTag.makeBinaryTag("ZM");
    private final short ZP = SAMTag.makeBinaryTag("ZP");
    private final short ZF = SAMTag.makeBinaryTag("ZF");

    private int zeroCoverageCount;
    private Long maxRecords;
    private final String[] includes;
    private final String[] tags;
    private final String[] tagsInt;
    private final String[] tagsChar;
    private boolean includeMatrices;
    private boolean includeCoverage;
    private boolean includeMDTag;
    private boolean torrentBam;
    private String bamHeader;
    private SAMSequenceDictionary samSeqDictionary;
    private List<String> readGroupIds = List.of(SummaryReportUtils.UNKNOWN_READGROUP); //init

    public BamSummaryReport(String[] includes, int maxRecs, String[] tags, String[] tagsInt, String[] tagsChar) {
        super();

        this.includes = includes;
        this.tags = tags;
        this.tagsInt = tagsInt;
        this.tagsChar = tagsChar;
        if (maxRecs > 0)
            maxRecords = (long) maxRecs;
        if (null != includes) {
            for (String include : includes) {
                if ("matrices".equalsIgnoreCase(include) || "matricies".equalsIgnoreCase(include)) {
                    includeMatrices = true;
                } else if ("coverage".equalsIgnoreCase(include)) {
                    includeCoverage = true;
                } else if ("md".equalsIgnoreCase(include)) {
                    includeMDTag = true;
                } else if ("html".equalsIgnoreCase(include)) {
                } else {
                    logger.warn("Unknown include type: " + include);
                }
            }
        }

        setupAdditionalTagMaps();
        logger.debug("Running with includeMatrices: " + includeMatrices + ", includeCoverage: " + includeCoverage
                + ", includeMDTag: " + includeMDTag + ", tags: " + Arrays.deepToString(tags));

    }

    private void setupAdditionalTagMaps() {
        if (null != tags) {
            for (String tag : tags) {
                additionalTags.put(tag, new ConcurrentSkipListMap<>());
            }
        }
        if (null != tagsInt) {
            for (String tagInt : tagsInt) {
                additionalIntegerTags.put(tagInt, new QCMGAtomicLongArray((100)));
            }
        }
        if (null != tagsChar) {
            for (String tagChar : tagsChar) {
                additionalCharacterTags.put(tagChar, new ConcurrentSkipListMap<>());
            }
        }
    }

    /**
     * Called once all records have been parsed.
     * Allows some cleanup to take place - eg. move remaining entries from coverageQueue and add to coverage map
     */
    public void cleanUp() {

        ReadGroupSummary summary = rgSummaries.get("overall");
        long duplicateCount = summary.getNumDuplicate();
        long unmappedCount = summary.getNumUnMapped();
        long nonCanonicalPairCount = summary.getNumSecondary();
        secondaryCount = summary.getNumSecondary();
        supplementaryCount = summary.getNumSupplementary();
        failedVendorQualityCheckCount = summary.getNumFailedVendorQuality();

        long countedNo = getRecordsParsed() - supplementaryCount - failedVendorQualityCheckCount - secondaryCount
                - unmappedCount - duplicateCount - nonCanonicalPairCount;

        if (includeCoverage) {
            // add the zero coverage count to the collection
            if (zeroCoverageCount > 0)
                coverage.put(0, new AtomicLong(zeroCoverageCount));
            // if there are any entries left in the queue, add them to the map
            if (!coverageQueue.isEmpty()) {
                int lastEntry = coverageQueue.lastKey();
                //					int lastEntry = ((TreeMap<Integer, AtomicLong>)coverageQueue).lastKey().intValue();
                lastEntry++;    // increment as headMap returns values less than the passed in key
                removeCoverageFromQueueAndAddToMap(lastEntry, coverageQueue, coverage);
                assert coverageQueue.isEmpty() : "There are still entries in the coverageQueue!!";
            }
        }

        // create the length maps here from the cycles objects
        // only get seq and qual data for good alignments
        seqLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(seqByCycle, countedNo);
        qualLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(qualByCycleInteger, countedNo);
        // always summarise tags so can use getRecordsParsed()
        tagCSLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(tagCSByCycle, getRecordsParsed());
        tagCQLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(tagCQByCycle, getRecordsParsed());

        long length = flagIntegerCount.length();
        for (int i = 0; i < length; i++) {
            if (flagIntegerCount.get(i) > 0) {
                String flagString = FlagUtil.getFlagString(i);
                flagBinaryCount.put(flagString, new AtomicLong(flagIntegerCount.get(i)));
            }
        }
    }

    @Override
    public void toXml(Element parent) {

        Element bamReportElement = init(parent, ProfileType.BAM, null, includes, maxRecords);

        // bam file HEADER
        // xml transformer can't handle large entries in the CDATA section so leave out bam header if its large (I'm looking at you here Platypus)
        if (!StringUtils.isNullOrEmpty(bamHeader)) {
            int cutoff = 100000;
            if (StringUtils.passesOccurrenceCountCheck(bamHeader, "@SQ", cutoff)) {
                Element headerElement = createSubElement(bamReportElement, "HEADER");
                SummaryReportUtils.bamHeaderToXml(headerElement, bamHeader);
            } else {
                logger.warn("Omitting bam header information from report as the number of chromosomes/contigs is greater than: " + cutoff);
            }
        }


        //Xu code : Summary for all read group
        summaryToXml(bamReportElement);

        // SEQ
        Element seqElement = createSubElement(bamReportElement, "SEQ");
        seqByCycle.toXml(seqElement, "BaseByCycle");
        SummaryReportUtils.lengthMapToXmlTallyItem(seqElement, "LengthTally", seqLineLengths);
        SummaryReportUtils.lengthMapToXml(seqElement, "BadBasesInReads", seqBadReadLineLengths);
        kmersSummary.toXml(seqElement, KmersSummary.MAX_KMERS);
        kmersSummary.toXml(seqElement, 1); //add 1-mers
        kmersSummary.toXml(seqElement, 2); //add 2-mers
        kmersSummary.toXml(seqElement, 3); //add 3-mers

        // QUAL
        Element qualElement = createSubElement(bamReportElement, "QUAL");
        qualByCycleInteger.toXml(qualElement, "QualityByCycle");
        SummaryReportUtils.lengthMapToXmlTallyItem(qualElement, "LengthTally", qualLineLengths);
        SummaryReportUtils.lengthMapToXml(qualElement, "BadQualsInReads", qualBadReadLineLengths);

        //TAG
        Element tagElement = createSubElement(bamReportElement, "TAG");
        //TAG-CS
        Element tagCSElement = createSubElement(tagElement, "CS");
        tagCSByCycle.toXml(tagCSElement, "ColourByCycle");
        SummaryReportUtils.lengthMapToXmlTallyItem(tagCSElement, "LengthTally",
                tagCSLineLengths);
        SummaryReportUtils.lengthMapToXml(tagCSElement, "BadColoursInReads",
                csBadReadLineLengths);

        //TAG-CQ
        Element tagCQElement = createSubElement(tagElement, "CQ");
        tagCQByCycle.toXml(tagCQElement, "QualityByCycle");
        SummaryReportUtils.lengthMapToXmlTallyItem(tagCQElement, "LengthTally",
                tagCQLineLengths);
        SummaryReportUtils.lengthMapToXml(tagCQElement, "BadQualsInReads",
                cqBadReadLineLengths);

        //TAG-RG
        SummaryReportUtils.lengthMapToXml(tagElement, "RG", getTagRGLineLengths());

        //TAG-ZM
        SummaryReportUtils.lengthMapToXml(tagElement, "ZM", tagZMLineLengths);
        // TAG-ZP
        SummaryReportUtils.lengthMapToXml(tagElement, "ZP", tagZPLineLengths);
        //			// TAG-ZB
        //			SummaryReportUtils.lengthMapToXml(tagElement, "ZB", tagZBLineLengths);
        // TAG-ZF
        SummaryReportUtils.lengthMapToXml(tagElement, "ZF", tagZFLineLengths);
        // TAG-CM
        SummaryReportUtils.lengthMapToXml(tagElement, "CM", tagCMLineLengths);
        // TAG-SM
        SummaryReportUtils.lengthMapToXml(tagElement, "SM", tagSMLineLengths);
        // TAG-IH
        SummaryReportUtils.lengthMapToXml(tagElement, "IH", tagIHLineLengths);
        // TAG-NH
        SummaryReportUtils.lengthMapToXml(tagElement, "NH", tagNHLineLengths);
        // TAG-MD-Mismatch
        Element tagMDElement = createSubElement(tagElement, "MD");
        if (!tagMDMismatchByCycle.cycles().isEmpty()) {
            SummaryReportUtils.toXmlWithPercentage(tagMDMismatchByCycle, tagMDElement, "MismatchByCycle", allReadsLineLengths, getRecordsParsed());
            SummaryReportUtils.lengthMapToXml(tagElement, "AllReads", allReadsLineLengths);
        }
        // TAG-MD ref>alt
        // switch the ints back to Strings
        Map<String, AtomicLong> mdRefAltLengthsString = new HashMap<>();
        for (int i = 0; i < mdRefAltLengthsForward.length(); i++) {
            long l = mdRefAltLengthsForward.get(i);
            if (l > 0) {
                mdRefAltLengthsString.put(SummaryReportUtils.getStringFromInt(i), new AtomicLong(l));
            }
        }
        SummaryReportUtils.lengthMapToXml(tagElement, "MD_mutation_forward", mdRefAltLengthsString);
        mdRefAltLengthsString = new HashMap<>();
        for (int i = 0; i < mdRefAltLengthsReverse.length(); i++) {
            long l = mdRefAltLengthsReverse.get(i);
            if (l > 0) {
                mdRefAltLengthsString.put(SummaryReportUtils.getStringFromInt(i), new AtomicLong(l));
            }
        }
        SummaryReportUtils.lengthMapToXml(tagElement, "MD_mutation_reverse", mdRefAltLengthsString);

        // additional tags
        for (Entry<String, ConcurrentSkipListMap<String, AtomicLong>> entry : additionalTags.entrySet()) {
            SummaryReportUtils.lengthMapToXml(tagElement, entry.getKey(), entry.getValue());
        }
        // additional tagsInt
        for (Entry<String, QCMGAtomicLongArray> entry : additionalIntegerTags.entrySet()) {
            SummaryReportUtils.lengthMapToXml(tagElement, entry.getKey(), entry.getValue());
        }
        // additional tagsChar
        for (Entry<String, ConcurrentSkipListMap<Character, AtomicLong>> entry : additionalCharacterTags.entrySet()) {
            SummaryReportUtils.lengthMapToXml(tagElement, entry.getKey(), entry.getValue());
        }

        // ISIZE
        Element tagISizeElement = createSubElement(bamReportElement, "ISIZE");

        rgSummaries.entrySet().stream().sorted(Entry.comparingByKey()).forEach(e -> e.getValue().iSize2Xml(tagISizeElement));


        // MRNM
        if (null != samSeqDictionary) {
            // convert to strings using SAMSequenceDictionary
            Map<String, AtomicLong> MRNMLengthsString = new HashMap<>();

            for (int i = 0; i < MRNMLengths.length(); i++) {
                long l = MRNMLengths.get(i);
                if (l > 0) {
                    if (i == mateRefNameMinusOne) {
                        MRNMLengthsString.put("*", new AtomicLong(l));
                    } else {
                        MRNMLengthsString.put(samSeqDictionary.getSequence(i).getSequenceName(), new AtomicLong(l));
                    }
                }
            }
            SummaryReportUtils.lengthMapToXml(bamReportElement, "RNEXT", MRNMLengthsString, new ReferenceNameComparator());
        } else {
            SummaryReportUtils.lengthMapToXml(bamReportElement, "RNEXT", MRNMLengths);
        }

        // CIGAR
        Element cigarElement = createSubElement(bamReportElement, "CIGAR");
        SummaryReportUtils.lengthMapToXml(cigarElement, "ObservedOperations",
                cigarValuesCount, new CigarStringComparator());
        SummaryReportUtils.lengthMapToXml(cigarElement, "Lengths", cigarLengths);

        // MAPQ
        SummaryReportUtils.lengthMapToXml(bamReportElement, "MAPQ", mapQualityLengths);

        // RNAME_POS
        SummaryReportUtils.postionSummaryMapToXml(bamReportElement, "RNAME_POS", rNamePosition, readGroupIds);

        // FLAG
        SummaryReportUtils.lengthMapToXml(bamReportElement, "FLAG", flagBinaryCount);

        if (includeCoverage)
            SummaryReportUtils.lengthMapToXml(bamReportElement, "Coverage", coverage);

        if (includeMatrices) {
            Map<MAPQMiniMatrix, AtomicLong> cmMatrix = new TreeMap<>();
            Map<MAPQMiniMatrix, AtomicLong> smMatrix = new TreeMap<>();
            Map<MAPQMiniMatrix, AtomicLong> lengthMatrix = new TreeMap<>();
            Map<MAPQMiniMatrix, AtomicLong> nhMatrix = new TreeMap<>();
            Map<MAPQMiniMatrix, AtomicLong> zmMatrix = new TreeMap<>();

            generateMAPQSubMaps(cmMatrix, smMatrix, lengthMatrix, nhMatrix, zmMatrix);

            logger.debug("cmMatrix(): " + cmMatrix.size());
            logger.debug("smMatrix(): " + smMatrix.size());
            logger.debug("lengthMatrix(): " + lengthMatrix.size());
            logger.debug("nhMatrix(): " + nhMatrix.size());
            logger.debug("zmMatrix(): " + zmMatrix.size());

            SummaryReportUtils.lengthMapToXml(bamReportElement, "MAPQMatrixCM", cmMatrix);
            SummaryReportUtils.lengthMapToXml(bamReportElement, "MAPQMatrixSM", smMatrix);
            SummaryReportUtils.lengthMapToXml(bamReportElement, "MAPQMatrixLength", lengthMatrix);
            SummaryReportUtils.lengthMapToXml(bamReportElement, "MAPQMatrixNH", nhMatrix);
            SummaryReportUtils.lengthMapToXml(bamReportElement, "MAPQMatrixZM", zmMatrix);

            zmSmMatrix.toXml(bamReportElement, "ZmSmMatrix");
        }
    }

    void generateMAPQSubMaps(Map<MAPQMiniMatrix, AtomicLong> cmMatrix,
                             Map<MAPQMiniMatrix, AtomicLong> smMatrix,
                             Map<MAPQMiniMatrix, AtomicLong> lengthMatrix,
                             Map<MAPQMiniMatrix, AtomicLong> nhMatrix,
                             Map<MAPQMiniMatrix, AtomicLong> zmMatrix) {

        logger.debug("mapQMatrix.size(): " + mapQMatrix.size());

        Map<Integer, AtomicLong> map;

        for (Entry<Integer, MAPQMatrix> entry : mapQMatrix.entrySet()) {
            logger.debug(entry.getKey() + ", entry.getValue().toString(): " + entry.getValue().toString());

            for (MatrixType type : MatrixType.values()) {
                map = entry.getValue().getMatrixByType(type);

                for (Entry<Integer, AtomicLong> mapEntry : map.entrySet()) {

                    switch (type) {
                        case CM:
                            cmMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
                            break;
                        case SM:
                            smMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
                            break;
                        case LENGTH:
                            lengthMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
                            break;
                        case NH:
                            nhMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
                            break;
                        case ZM:
                            zmMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
                            break;
                    }
                }
            }
            // removing due to memory limitations
            mapQMatrix.remove(entry.getKey());
        }
    }

    /**
     * Parse a SAMRecord Collate various pieces of info from the SAMRecord ready for the summariser to retrieve
     *
     * @param record SAMRecord next row in file
     */
    public void parseRecord(final SAMRecord record) throws Exception {
        updateRecordsParsed();
        MAPQMatrix matrix = null;

        SAMReadGroupRecord srgr = record.getReadGroup();
        String readGroup = srgr == null ? SummaryReportUtils.UNKNOWN_READGROUP : srgr.getReadGroupId();

        // Xu code: check if record has its fail or duplicate flag set. if so, miss out some of the summaries
        //anyway, add to summary and then add to it's readgroup
        rgSummaries.get(SummaryReportUtils.All_READGROUP).parseRecord(record);

        ReadGroupSummary rgSumm = rgSummaries.computeIfAbsent(readGroup, ReadGroupSummary::new);
        boolean parsedRecord = rgSumm.parseRecord(record);

        if (parsedRecord) {
            // SEQ
            SummaryByCycleUtils.parseCharacterSummary(seqByCycle, record.getReadBases(), record.getReadNegativeStrandFlag());
            SummaryReportUtils.tallyBadReadsAsString(record.getReadBases(), seqBadReadLineLengths);
            kmersSummary.parseKmers(record.getReadBases(), record.getReadNegativeStrandFlag());

            // QUAL
            SummaryByCycleUtils.parseIntegerSummary(qualByCycleInteger, record.getBaseQualities(), record.getReadNegativeStrandFlag());
            SummaryReportUtils.tallyQualScores(record.getBaseQualities(), qualBadReadLineLengths);

            // ISIZE  is done inside readGroupSummary.pParseRecord

            // MRNM
            final int mateRefNameIndex = record.getMateReferenceIndex();
            if (mateRefNameIndex == -1) MRNMLengths.increment(mateRefNameMinusOne);
            else MRNMLengths.increment(mateRefNameIndex);

            // RNAME & POS
            parseRNameAndPos(record.getReferenceName(), record.getAlignmentStart(), readGroup);    // Position value

            if (record.getReadPairedFlag()) {
                if (record.getFirstOfPairFlag()) {
                    p1Lengths.increment(record.getReadBases().length);
                } else if (record.getSecondOfPairFlag()) {
                    p2Lengths.increment(record.getReadBases().length);
                }
            }
            // coverage
            if (includeCoverage) {
                parseCoverage(record);
            }
        }

        // MAPQ (Mapping Quality)
        final int mapQ = record.getMappingQuality();
        mapQualityLengths.increment(mapQ);

        if (includeMatrices) {
            matrix = mapQMatrix.get(mapQ);
            if (null == matrix) {
                MAPQMatrix newMatrix = new MAPQMatrix();
                matrix = mapQMatrix.putIfAbsent(mapQ, newMatrix);
                if (null == matrix)
                    matrix = newMatrix;
            }
            matrix.addToMatrix(record.getReadLength(), MatrixType.LENGTH);
        }

        // only TAGS, FLAGS, and CIGARS are always summarised
        // TAGS
        parseTAGs(record, matrix, record.getReadBases(), record.getReadNegativeStrandFlag());

        // CIGAR
        parseCigar(record.getCigar());

        // Flags
        flagIntegerCount.increment(record.getFlags());
    }

    private void parseTAGs(final SAMRecord record, final MAPQMatrix matrix, final byte[] readBases,
                           final boolean reverseStrand) {

        // if the record has a CQ tag, then it will likely have a CS tag
        // if a record does not have a CQ tag, then it will not have a CS tag
        String value = (String) record.getAttribute(SAMTag.CQ);
        if (null != value) {
            // CQ
            byte[] valueB = SAMUtils.fastqToPhred(value);
            if (null != valueB) {
                SummaryByCycleUtils.parseIntegerSummary(tagCQByCycle, valueB);
                SummaryReportUtils.tallyQualScores(valueB, cqBadReadLineLengths);
            }
            // CS
            value = (String) record.getAttribute(SAMTag.CS);
            if (null != value) {
                SummaryByCycleUtils.parseCharacterSummary(tagCSByCycle, value, null, 1);
                SummaryReportUtils.tallyBadReadsAsString(value, csBadReadLineLengths);
            }
        }

        aggregateStringAttribute(record.getAttribute("RG"), tagRGLineLengths);
        if (!torrentBam) {
            aggregateIntegerAttribute(record.getAttribute("ZM"), tagZMLineLengths, includeMatrices, matrix, MatrixType.ZM);
            aggregateIntegerAttribute(record.getAttribute("ZF"), tagZFLineLengths, false, null, null);
            aggregateStringAttribute(record.getAttribute("ZP"), tagZPLineLengths);
        }
        aggregateIntegerAttribute(record.getAttribute("CM"), tagCMLineLengths, includeMatrices, matrix, MatrixType.CM);
        aggregateIntegerAttribute(record.getAttribute("SM"), tagSMLineLengths, includeMatrices, matrix, MatrixType.SM);
        aggregateIntegerAttribute(record.getAttribute("IH"), tagIHLineLengths, false, null, null);
        aggregateIntegerAttribute(record.getAttribute("NH"), tagNHLineLengths, includeMatrices, matrix, MatrixType.NH);
        //MD
        if (!includeMDTag) {
            value = (String) record.getAttribute(SAMTag.MD);
            if (null != value) {
                String err = SummaryReportUtils.tallyMDMismatches(value, record.getCigar(), tagMDMismatchByCycle, readBases, reverseStrand, mdRefAltLengthsForward, mdRefAltLengthsReverse);
                //limit err message on log file
                if (err != null && ((errMDReadNo++) < errMDReadLimit)) {
                    logger.warn(err);
                }
                allReadsLineLengths.increment(record.getReadLength());
            }
        }

        // additionalTags
        if (null != tags)
            for (String s : tags) {
                Object o = record.getAttribute(s);
                if (o instanceof String) {
                    SummaryByCycleUtils.incrementCount(additionalTags.get(s), (String) o);
                }
            }
        // additionalTagsInt
        if (null != tagsInt)
            for (String s : tagsInt) {
                Object o = record.getAttribute(s);
                if (o instanceof Integer) {
                    additionalIntegerTags.get(s).increment((Integer) o);
                }
            }
        // additionalTagsChar
        if (null != tagsChar) {
            for (String s : tagsChar) {
                Object o = record.getAttribute(s);
                if (o instanceof Character) {
                    SummaryByCycleUtils.incrementCount(additionalCharacterTags.get(s), (Character) o);
                }
            }
        }
    }

    public static void aggregateIntegerAttribute(Object attribute,
                                                 final QCMGAtomicLongArray array, boolean includeMatrices, final MAPQMatrix matrix, MatrixType matrixType) {
        if (attribute instanceof Integer) {
            if (null != array) {
                array.increment((Integer) attribute);
            }
            if (includeMatrices && matrix != null) {
                matrix.addToMatrix((Integer) attribute, matrixType);
            }
        }
    }

    public static void aggregateStringAttribute(Object attribute, final ConcurrentMap<String, AtomicLong> map) {
        if (attribute instanceof String && null != map) {
            SummaryByCycleUtils.incrementCount(map, (String) attribute);
        }
    }

    void parseCoverage(SAMRecord record) {
        int count = 0;
        for (AlignmentBlock ab : record.getAlignmentBlocks()) {
            // find out how many positions were skipped...
            int referenceStart = ab.getReferenceStart();
            synchronized (coverageQueue) {
                int latestFromMap = coverageQueue.isEmpty() ? 0 : coverageQueue.lastKey();
                if (latestFromMap < referenceStart) {
                    zeroCoverageCount += referenceStart - latestFromMap + 1;
                }

                SummaryReportUtils.addPositionAndLengthToMap(coverageQueue, referenceStart, ab.getLength());
                if (++count == 1)
                    removeCoverageFromQueueAndAddToMap(referenceStart, coverageQueue, coverage);

            }
        }
    }

    void removeCoverageFromQueueAndAddToMap(int referenceStart, Map<Integer, AtomicLong> queue,
                                            ConcurrentMap<Integer, AtomicLong> map) {
        for (Iterator<Integer> it = ((ConcurrentSkipListMap<Integer, AtomicLong>) queue)
                .headMap(referenceStart).keySet().iterator(); it.hasNext(); ) {

            SummaryByCycleUtils.incrementCount(map, (int) queue.get(it.next()).get());
            synchronized (queue) {
                it.remove();
            }
        }
    }

    void parseCigar(Cigar cigar) {
        if (null != cigar) {
            int length = 0;

            for (CigarElement ce : cigar.getCigarElements()) {
                CigarOperator operator = ce.getOperator();
                if (!CigarOperator.M.equals(operator)) {
                    SummaryByCycleUtils.incrementCount(cigarValuesCount, "" + ce.getLength() + operator);
                }
                length += getSizeFromInt(ce.getLength()) + 1;
            }
            cigarLengths.increment(length);
        }
    }

    private int getSizeFromInt(int value) {
        if (value == 0) return 0;
        return 1 + getSizeFromInt(value / 10);
    }

    public long getErrMDReadNumber() {
        return errMDReadNo;
    }


    private void summaryToXml(Element bamReportElement) {
        // Summary
        long noOfRecords = getRecordsParsed() - supplementaryCount - failedVendorQualityCheckCount - secondaryCount;

        Element summaryElement = createSubElement(bamReportElement, "SUMMARY");

        // md cycles
        int mismatchingCycles = 0;
        for (Integer cycle : tagMDMismatchByCycle.cycles()) {
            long mapTotal = SummaryReportUtils.getCountOfMapValues(tagMDMismatchByCycle.getValue(cycle));
            double percentage = (((double) mapTotal / noOfRecords));
            if (percentage > 0.01)
                mismatchingCycles++;
        }
        Element mdMismatchCyclesE = createSubElement(summaryElement, "MDMismatchCycles");
        mdMismatchCyclesE.setAttribute("value", mismatchingCycles + "");

        // Summary - 1st and 2nd in pair ave read lengths
        long runningTally = 0, total = 0;
        for (int i = 0; i < p1Lengths.length(); i++) {
            long value = p1Lengths.get(i);
            if (value > 0) {
                total += value;
                runningTally += (value * i);
            }
        }

        Element p1AveLenghtE = createSubElement(summaryElement, "FirstInPairAveLength");
        long averageLength = total > 0 ? (runningTally / total) : 0;
        p1AveLenghtE.setAttribute("value", averageLength + "");
        p1AveLenghtE.setAttribute("count", String.format("%,d", total));

        runningTally = 0;
        total = 0;
        for (int i = 0; i < p2Lengths.length(); i++) {
            long value = p2Lengths.get(i);
            if (value > 0) {
                total += value;
                runningTally += (value * i);
            }
        }

        Element p2AveLenghtE = createSubElement(summaryElement, "SecondInPairAveLength");
        averageLength = total > 0 ? (runningTally / total) : 0;
        p2AveLenghtE.setAttribute("value", averageLength + "");
        p2AveLenghtE.setAttribute("count", String.format("%,d", total));

        long badReadNum = rgSummaries.get(SummaryReportUtils.All_READGROUP).getNumFailedVendorQuality() +
                rgSummaries.get(SummaryReportUtils.All_READGROUP).getNumSecondary() +
                rgSummaries.get(SummaryReportUtils.All_READGROUP).getNumSupplementary();
        createSubElement(summaryElement, "Failed_Secondary_Supplementary").setAttribute("value", badReadNum + "");
        createSubElement(summaryElement, "inputedReads").setAttribute("value", getRecordsParsed() + "");

        //Xu code : for each read group
        Element rgClipElement = createSubElement(summaryElement, "Reads");
        Element pairElement = createSubElement(summaryElement, "Pairs");

        //for each real read group first
        long trimBases = 0;
        long maxBases = 0;
        List<String> sortedRGs = rgSummaries.keySet().stream().sorted().toList();
        for (String rg : sortedRGs) {
            ReadGroupSummary summary = rgSummaries.get(rg);
            if (!rg.equals(SummaryReportUtils.All_READGROUP)) {
                summary.readSummary2Xml(rgClipElement);
                summary.pairSummary2Xml(pairElement);
                trimBases += summary.getTrimedBases();
                logger.info("for " + rg + ", summary.getMaxReadLength(): " + summary.getMaxReadLength() + ", summary.getCountedReads(): " + summary.getCountedReads());
                maxBases += summary.getMaxReadLength() * summary.getCountedReads();
            }
        }

        if (maxBases == 0) {
            logger.info("maxBases = 0, number of rgs: " + rgSummaries.size());
        }

        //overall group at last
        ReadGroupSummary summary = rgSummaries.get(SummaryReportUtils.All_READGROUP);
        summary.setMaxBases(maxBases);
        summary.setTrimedBases(trimBases);
        summary.readSummary2Xml(rgClipElement);
        summary.pairSummary2Xml(pairElement);
    }

    void parseRNameAndPos(final String rName, final int position, String rgid) {

        PositionSummary ps = rNamePosition.get(rName);
        if (null == ps) {
            rNamePosition.putIfAbsent(rName, new PositionSummary(readGroupIds));
            ps = rNamePosition.get(rName);
        }

        ps.addPosition(position, rgid);
    }

    // ///////////////////////////////////////////////////////////////////////
    // The following methods are used by the test classes
    // ///////////////////////////////////////////////////////////////////////

    ConcurrentMap<String, AtomicLong> getCigarValuesCount() {
        return cigarValuesCount;
    }

    ConcurrentMap<String, PositionSummary> getRNamePosition() {
        return rNamePosition;
    }

    SummaryByCycleNew2<Character> getSeqByCycle() {
        return seqByCycle;
    }

    SummaryByCycleNew2<Character> getTagCSByCycle() {
        return tagCSByCycle;
    }

    ConcurrentMap<String, AtomicLong> getTagRGLineLengths() {
        return tagRGLineLengths;
    }

    QCMGAtomicLongArray getTagZMLineLengths() {
        return tagZMLineLengths;
    }

    Map<Integer, AtomicLong> getTagCSLineLengths() {
        return tagCSLineLengths;
    }

    Map<Integer, AtomicLong> getTagCQLineLengths() {
        return tagCQLineLengths;
    }

    ConcurrentMap<Integer, AtomicLong> getCoverage() {
        return coverage;
    }

    ConcurrentMap<Integer, AtomicLong> getCoverageQueue() {
        return coverageQueue;
    }

    public ConcurrentMap<Integer, MAPQMatrix> getMapQMatrix() {
        return mapQMatrix;
    }

    public void setBamHeader(String bamHeader) {
        this.bamHeader = bamHeader;
    }

    public void setTorrentBam(boolean isTorrentBam) {
        this.torrentBam = isTorrentBam;
    }

    public void setSamSequenceDictionary(SAMSequenceDictionary samSeqDictionary) {
        this.samSeqDictionary = samSeqDictionary;
    }

    public void setReadGroups(List<String> ids) {
        readGroupIds = Stream.concat(ids.stream(), readGroupIds.stream()).collect(Collectors.toList());
    }
}
