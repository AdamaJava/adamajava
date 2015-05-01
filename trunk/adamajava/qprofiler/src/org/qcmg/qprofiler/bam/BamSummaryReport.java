/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.bam;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import net.sf.samtools.AlignmentBlock;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMTagUtil;
import net.sf.samtools.SAMUtils;

import org.qcmg.common.model.CigarStringComparator;
import org.qcmg.common.model.MAPQMatrix;
import org.qcmg.common.model.MAPQMatrix.MatrixType;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.model.SummaryByCycleNew2;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.util.FlagUtil;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.w3c.dom.Element;

public class BamSummaryReport extends SummaryReport {

	private final AtomicLong duplicateCount = new AtomicLong();
	private final AtomicLong failedVendorQualityCheckCount = new AtomicLong();
	private final static int mateRefNameMinusOne = 255;

	private static final Character c = Character.MAX_VALUE;
	private static final Integer i = Integer.MAX_VALUE;
	//SEQ
	private final SummaryByCycleNew2<Character> seqByCycle = new SummaryByCycleNew2<Character>(c, 512);
	private Map<Integer, AtomicLong> seqLineLengths = null;
	private final QCMGAtomicLongArray seqBadReadLineLengths = new QCMGAtomicLongArray(128);

	//QUAL
	private final SummaryByCycleNew2<Integer> qualByCycleInteger = new SummaryByCycleNew2<Integer>(i, 512);
	private Map<Integer, AtomicLong> qualLineLengths = null;
	private final QCMGAtomicLongArray qualBadReadLineLengths = new QCMGAtomicLongArray(128);

	// TAGS
	private final SummaryByCycleNew2<Character> tagCSByCycle = new SummaryByCycleNew2<Character>(c, 512);
	private Map<Integer, AtomicLong> tagCSLineLengths = null;
	private final QCMGAtomicLongArray csBadReadLineLengths = new QCMGAtomicLongArray(128);

	private final SummaryByCycleNew2<Integer> tagCQByCycle = new SummaryByCycleNew2<Integer>(i, 512);
	private Map<Integer, AtomicLong> tagCQLineLengths = null;
	private final QCMGAtomicLongArray cqBadReadLineLengths = new QCMGAtomicLongArray(128);

	private final ConcurrentMap<String, AtomicLong> tagRGLineLengths = new ConcurrentHashMap<String, AtomicLong>();
	private final QCMGAtomicLongArray tagZMLineLengths = new QCMGAtomicLongArray(2048);
	private final QCMGAtomicLongArray tagCMLineLengths = new QCMGAtomicLongArray(128);
	private final QCMGAtomicLongArray tagSMLineLengths = new QCMGAtomicLongArray(256);
	private final QCMGAtomicLongArray tagNHLineLengths = new QCMGAtomicLongArray(1024);
	private final QCMGAtomicLongArray tagIHLineLengths = new QCMGAtomicLongArray(1024);
	private final SummaryByCycleNew2<Character> tagMDMismatchByCycle = new SummaryByCycleNew2<Character>(c, 512);
	private final QCMGAtomicLongArray mdRefAltLengthsForward = new QCMGAtomicLongArray(32);
	private final QCMGAtomicLongArray mdRefAltLengthsReverse = new QCMGAtomicLongArray(32);
	//	private final SummaryByCycle<Character> tagMDMismatchByCycle = new SummaryByCycle<Character>();
	private final QCMGAtomicLongArray allReadsLineLengths = new QCMGAtomicLongArray(1024);

	// new tags for JP
	private final ConcurrentMap<String, AtomicLong> tagZPLineLengths = new ConcurrentHashMap<String, AtomicLong>();

	private final QCMGAtomicLongArray tagZFLineLengths = new QCMGAtomicLongArray(128);
	// mapqMatrix
	private final ConcurrentMap<Integer, MAPQMatrix> mapQMatrix = new ConcurrentSkipListMap<Integer, MAPQMatrix>();

	private final SummaryByCycle<Integer> zmSmMatrix = new SummaryByCycle<Integer>(128);

	//	private final ConcurrentMap<Integer, AtomicLong> iSizeLengths = new ConcurrentHashMap<Integer, AtomicLong>();
	//	private final QCMGAtomicLongArray iSizeLengths10 = new QCMGAtomicLongArray(1024 * 16);
	//	private final QCMGAtomicLongArray iSizeLengths1M = new QCMGAtomicLongArray(1024);

	private final ConcurrentMap<String, AtomicLongArray> iSizeByReadGroupMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, QCMGAtomicLongArray> iSizeByReadGroupMapBinned = new ConcurrentHashMap<>();


	private final QCMGAtomicLongArray MRNMLengths = new QCMGAtomicLongArray(mateRefNameMinusOne + 1);
	//	private final ConcurrentMap<String, AtomicLong> MRNMLengths = new ConcurrentHashMap<String, AtomicLong>(100);

	private final ConcurrentMap<String, AtomicLong> cigarValuesCount = new ConcurrentHashMap<String, AtomicLong>();

	private final QCMGAtomicLongArray mapQualityLengths = new QCMGAtomicLongArray(256);

	private final ConcurrentMap<String, PositionSummary> rNamePosition = new ConcurrentHashMap<String, PositionSummary>(85);

	// FLAGS
	private final Map<String, AtomicLong> flagBinaryCount = new ConcurrentSkipListMap<String, AtomicLong>();
	private final QCMGAtomicLongArray flagIntegerCount = new QCMGAtomicLongArray(2048);

	// Coverage
	private final ConcurrentMap<Integer, AtomicLong> coverage = new ConcurrentSkipListMap<Integer, AtomicLong>();
	private final ConcurrentNavigableMap<Integer, AtomicLong> coverageQueue = new ConcurrentSkipListMap<Integer, AtomicLong>();


	private final QCMGAtomicLongArray cigarLengths = new QCMGAtomicLongArray(1024);
	
	private final QCMGAtomicLongArray p1Lengths = new QCMGAtomicLongArray(1024);
	private final QCMGAtomicLongArray p2Lengths = new QCMGAtomicLongArray(1024);

	private final ConcurrentMap<String, ConcurrentSkipListMap<String, AtomicLong>> additionalTags = 
			new ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, AtomicLong>>();
	private final ConcurrentMap<String, QCMGAtomicLongArray> additionalIntegerTags = 
			new ConcurrentSkipListMap<String, QCMGAtomicLongArray>();
	private final ConcurrentMap<String, ConcurrentSkipListMap<Character, AtomicLong>> additionalCharacterTags = 
			new ConcurrentSkipListMap<String, ConcurrentSkipListMap<Character, AtomicLong>>();
	
	//Xu Code: each RG softclips, hardclips, read length; 
	private final ConcurrentMap<String, QCMGAtomicLongArray> rgSoftClip = 
			new ConcurrentSkipListMap<String, QCMGAtomicLongArray>();
	private final ConcurrentMap<String, QCMGAtomicLongArray> rgHardClip = 
			new ConcurrentSkipListMap<String, QCMGAtomicLongArray>();
	private final ConcurrentMap<String, QCMGAtomicLongArray> rgReadLength = 
			new ConcurrentSkipListMap<String, QCMGAtomicLongArray>();
	
	private final static SAMTagUtil STU = SAMTagUtil.getSingleton();
	private final short CS = STU.CS;
	private final short CQ = STU.CQ;
	private final short RG = STU.RG;
	private final short CM = STU.CM;
	private final short SM = STU.SM;
	private final short NH = STU.NH;
	private final short MD = STU.MD;
	private final short IH = STU.IH;
	// custom tags
	//	private final short ZB = STU.makeBinaryTag("ZB");
	private final short ZM = STU.makeBinaryTag("ZM");
	private final short ZP = STU.makeBinaryTag("ZP");
	private final short ZF = STU.makeBinaryTag("ZF");

	private int zeroCoverageCount;

	private Long maxRecords;
	private String [] includes;
	private String [] tags;
	private String [] tagsInt;
	private String [] tagsChar;
	//	private boolean excludeAll;
	private boolean includeMatrices;
	private boolean includeCoverage;
	private boolean includeMDTag;
	private boolean torrentBam;
	private String bamHeader;
	private SAMSequenceDictionary samSeqDictionary;

	public BamSummaryReport() {
		super();
	}

	public BamSummaryReport(String [] includes, int maxRecs, String [] tags, String [] tagsInt, String [] tagsChar) {
		super();

		this.includes = includes;
		this.tags = tags;
		this.tagsInt = tagsInt;
		this.tagsChar = tagsChar;
		if (maxRecs > 0)
			maxRecords = Long.valueOf(maxRecs);
		if (null != includes) {
			for (String include : includes) {
				if ("matrices".equalsIgnoreCase(include) || "matricies".equalsIgnoreCase(include)) {
					includeMatrices = true;
				} else if ("coverage".equalsIgnoreCase(include)) {
					includeCoverage = true;
				} else if ("md".equalsIgnoreCase(include)) {
					includeMDTag = true;
				} else if ("html".equalsIgnoreCase(include)){
				} else {
					logger.warn("Unknown include type: " + include);
				}
			}
		}

		setupAdditionalTagMaps();

		logger.debug("Running with includeMatrices: " + includeMatrices + ", includeCoverage: " + includeCoverage 
				+  ", includeMDTag: " +  includeMDTag + ", tags: " + Arrays.deepToString(tags));
	}

	private void setupAdditionalTagMaps() {
		if (null != tags) {
			for (String tag : tags)
				additionalTags.put(tag, new ConcurrentSkipListMap<String, AtomicLong>());
		}
		if (null != tagsInt) {
			for (String tagInt : tagsInt)
				additionalIntegerTags.put(tagInt, new QCMGAtomicLongArray((100)));
		}
		if (null != tagsChar) {
			for (String tagChar : tagsChar)
				additionalCharacterTags.put(tagChar,  new ConcurrentSkipListMap<Character, AtomicLong>());
		}
	}

	/**
	 * Called once all records have been parsed.
	 * Allows some cleanup to take place - eg. move remaining entries from coverageQueue and add to coverage map
	 */
	public void cleanUp() {

		final long nonDupCount = getRecordsParsed() - duplicateCount.longValue();

		if (includeCoverage ) {
			// add the zero coverage count to the collection
			if (zeroCoverageCount > 0)
				coverage.put(0, new AtomicLong(zeroCoverageCount));
			// if there are any entries left in the queue, add them to the map
			if ( ! coverageQueue.isEmpty()) {
				int lastEntry = ((ConcurrentSkipListMap<Integer, AtomicLong>)coverageQueue).lastKey().intValue();
				//					int lastEntry = ((TreeMap<Integer, AtomicLong>)coverageQueue).lastKey().intValue();
				lastEntry++;	// increment as headMap returns values less than the passed in key

				removeCoverageFromQueueAndAddToMap(lastEntry, coverageQueue, coverage);
				assert coverageQueue.isEmpty() : "There are still entries in the coverageQueue!!"; 
			}
		}

		// create the length maps here from the cycles objects
		// only get seq and qual data for non-duplicates
		seqLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(seqByCycle, nonDupCount);
		qualLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(qualByCycleInteger, nonDupCount);
		// always summarise tags so can use getRecordsParsed()
		tagCSLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(tagCSByCycle, getRecordsParsed());
		tagCQLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(tagCQByCycle, getRecordsParsed());

		long length = flagIntegerCount.length();
		for (int i = 0 ; i < length ; i++) {
			if (flagIntegerCount.get(i) > 0) {
				String flagString = FlagUtil.getFlagString(i);
				flagBinaryCount.put(flagString, new AtomicLong(flagIntegerCount.get(i)));
			}
		}

		//Always want a count of the duplicates
		// check no of records is equal to non duplicate count minus duplicates in flags collection
		long duplicates = 0;
		for (Entry<String, AtomicLong> entry : flagBinaryCount.entrySet()) {
			if (entry.getKey().contains("d"))
				duplicates +=entry.getValue().get();
		}
		if (getRecordsParsed() != duplicates + nonDupCount) {
			logger.warn("No of parsed records [" + getRecordsParsed() 
					+ "] does not add up to the count of duplicates [" 
					+ duplicates + "] and non-duplicates [" + nonDupCount + "]");
		}
	}

	@Override
	public void toXml(Element parent) {
		long noOfRecords = getRecordsParsed();
		Element bamReportElement = init(parent, ProfileType.BAM, 
				Long.valueOf(duplicateCount.longValue()), includes, maxRecords);

		//		if ( ! excludeAll) {

		// bam file HEADER
		// xml transformer can't handle large entries in the CDATA section so leave out bam header if its large (I'm looking at you here Platypus)
		if ( ! StringUtils.isNullOrEmpty(bamHeader)) {
			int cutoff = 100000;
			if (StringUtils.passesOccurenceCountCheck(bamHeader, "@SQ", cutoff)) {
				Element headerElement = createSubElement(bamReportElement, "HEADER");
				SummaryReportUtils.bamHeaderToXml(headerElement, bamHeader);
			} else {
				logger.warn("Ommitting bam header information from report as the number of chromosomes/contigs is greater than: " + cutoff);
			}
		}

		// Summary
		Element summaryElement = createSubElement(bamReportElement, "SUMMARY");
		Element noOfReadsE = createSubElement(summaryElement, "ReadCount");
		noOfReadsE.setAttribute("value", noOfRecords + "");
		Element noOfDupsE = createSubElement(summaryElement, "DuplicatePercentage");
		noOfDupsE.setAttribute("value", (duplicateCount.doubleValue() / noOfRecords) * 100 + "");
		
		// loop through flag, tallying unmapped
		long tally = 0;
		for (String s : flagBinaryCount.keySet()) {
			if (s.contains("U") || s.contains("u")) {
				tally += flagBinaryCount.get(s).longValue();
			}
		}
		Element noOfUnmappedE = createSubElement(summaryElement, "UnmappedPercentage");
		noOfUnmappedE.setAttribute("value", ((double)tally / noOfRecords) * 100 + "");
		
		for (String s : iSizeByReadGroupMap.keySet()) {
			Element modalISizeE = createSubElement(summaryElement, "ModalISize");
			long mode = 0, iSize = 0;
			AtomicLongArray rgISize = iSizeByReadGroupMap.get(s); 
			for (int i = 0 ; i < rgISize.length() ; i++) {
				if ( rgISize.get(i) > mode && i > 0) {
					mode =  rgISize.get(i);
//					logger.info("setting mode to be: " + mode + " at iSize (i): " + i);
					iSize = i;
				}
			}
			modalISizeE.setAttribute("rg", s + "");
			modalISizeE.setAttribute("value", iSize + "");
		}
		
		// md cycles
//		StringBuilder mismatchingCycles = new StringBuilder();
		int mismatchingCycles = 0;
		for (Integer cycle : tagMDMismatchByCycle.cycles()) {
			long mapTotal = SummaryReportUtils.getCountOfMapValues(tagMDMismatchByCycle.getValue(cycle));
			double percentage = (((double) mapTotal / noOfRecords));
			if (percentage > 0.01) {
				mismatchingCycles++;
				// we want to know about this
//				if (mismatchingCycles.length() > 0) {
//					mismatchingCycles.append(",");
//				}
//				mismatchingCycles.append(cycle.intValue());
			}
		}
		Element mdMismatchCyclesE = createSubElement(summaryElement, "MDMismatchCycles");
		mdMismatchCyclesE.setAttribute("value", mismatchingCycles + "");
		
		// Summary - 1st and 2nd in pair ave read lengths
		long runningTally = 0, total = 0;
		for (int i = 0 ; i < p1Lengths.length() ; i++) {
			long value = p1Lengths.get(i);
			if (value > 0) {
				total += value;
				runningTally += (value * i);
			}
		}
		
		Element p1AveLenghtE = createSubElement(summaryElement, "FirstInPairAveLength");
		long averageLength = total > 0 ? (runningTally / total) : 0;
		p1AveLenghtE.setAttribute("value", averageLength + "");
		p1AveLenghtE.setAttribute("count", total + "");
		
		runningTally = 0;
		total = 0;
		for (int i = 0 ; i < p2Lengths.length() ; i++) {
			long value = p2Lengths.get(i);
			if (value > 0) {
				total += value;
				runningTally += (value * i);
			}
		}
		Element p2AveLenghtE = createSubElement(summaryElement, "SecondInPairAveLength");
		averageLength = total > 0 ? (runningTally / total) : 0;
		p2AveLenghtE.setAttribute("value", averageLength + "");
		p2AveLenghtE.setAttribute("count", total + "");

		
		// SEQ
		Element seqElement = createSubElement(bamReportElement, "SEQ");
		seqByCycle.toXml(seqElement, "BaseByCycle");
		SummaryReportUtils.lengthMapToXmlTallyItem(seqElement, "LengthTally",
				seqLineLengths);
		SummaryReportUtils.lengthMapToXml(seqElement, "BadBasesInReads",
				seqBadReadLineLengths);

		// QUAL
		Element qualElement = createSubElement(bamReportElement, "QUAL");
		qualByCycleInteger.toXml(qualElement, "QualityByCycle");
		SummaryReportUtils.lengthMapToXmlTallyItem(qualElement, "LengthTally",
				qualLineLengths);
		SummaryReportUtils.lengthMapToXml(qualElement, "BadQualsInReads",
				qualBadReadLineLengths);

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
		if ( ! tagMDMismatchByCycle.cycles().isEmpty()) {
			SummaryReportUtils.toXmlWithPercentage(tagMDMismatchByCycle, 
					tagMDElement, "MismatchByCycle", allReadsLineLengths, getRecordsParsed());
			SummaryReportUtils.lengthMapToXml(tagElement, "AllReads", allReadsLineLengths);
		}
		// TAG-MD ref>alt
		// switch the ints back to Strings
		Map<String, AtomicLong> mdRefAltLengthsString = new HashMap<String, AtomicLong>();
		for (int i = 0 ; i < mdRefAltLengthsForward.length() ; i++) {
			long l = mdRefAltLengthsForward.get(i);
			if (l > 0) {
				mdRefAltLengthsString.put(SummaryReportUtils.getStringFromInt(i), new AtomicLong(l));
			}
		}
		SummaryReportUtils.lengthMapToXml(tagElement, "MD_mutation_forward", mdRefAltLengthsString);
		mdRefAltLengthsString = new HashMap<String, AtomicLong>();
		for (int i = 0 ; i < mdRefAltLengthsReverse.length() ; i++) {
			long l = mdRefAltLengthsReverse.get(i);
			if (l > 0) {
				mdRefAltLengthsString.put(SummaryReportUtils.getStringFromInt(i), new AtomicLong(l));
			}
		}
		SummaryReportUtils.lengthMapToXml(tagElement, "MD_mutation_reverse", mdRefAltLengthsString);


		// additional tags
		for (Entry<String,  ConcurrentSkipListMap<String, AtomicLong>> entry : additionalTags.entrySet()) {
			SummaryReportUtils.lengthMapToXml(tagElement, entry.getKey(), entry.getValue());
		}
		// additional tagsInt
		for (Entry<String,  QCMGAtomicLongArray> entry : additionalIntegerTags.entrySet()) {
			SummaryReportUtils.lengthMapToXml(tagElement, entry.getKey(), entry.getValue());
		}
		// additional tagsChar
		for (Entry<String,  ConcurrentSkipListMap<Character, AtomicLong>> entry : additionalCharacterTags.entrySet()) {
			SummaryReportUtils.lengthMapToXml(tagElement, entry.getKey(), entry.getValue());
		}

		// ISIZE
		Element tagISizeElement = createSubElement(bamReportElement, "ISIZE");
		ConcurrentMap<String, ConcurrentMap<Integer, AtomicLong>> iSizeByReadGroupCompleteMap = SummaryReportUtils.binIsize(1, iSizeByReadGroupMap, iSizeByReadGroupMapBinned);

		for (Entry<String, ConcurrentMap<Integer, AtomicLong>> entry : iSizeByReadGroupCompleteMap.entrySet()) {
			// create new tag for this readgroup
			Element rgElement = createSubElement(tagISizeElement, "RG");
			rgElement.setAttribute("value", entry.getKey());
			SummaryReportUtils
			.binnedLengthMapToRangeTallyXml(rgElement, entry.getValue());	
		}
		
		//Xu code : rgClipElement code clip 2 xml: there are three page unger RG
		//add below method to stat min, max, medium, mode of clips or length	
		Element rgClipElement = createSubElement(bamReportElement, "RG");		
		SummaryReportUtils.ToXmlWithoutPercentage(rgClipElement, "ReadCounts", getRGCounts());	
		SummaryReportUtils.ToXmlWithoutPercentage(rgClipElement, "ReadLength", getRGStats(rgReadLength) );	
		SummaryReportUtils.ToXmlWithoutPercentage(rgClipElement, "HardClips", getRGStats( rgHardClip) );	
		SummaryReportUtils.ToXmlWithoutPercentage(rgClipElement, "SoftClips", getRGStats( rgSoftClip) );	

		// MRNM
		if (null != samSeqDictionary) {
			// convert to strings using SAMSequenceDictionary
			Map<String, AtomicLong> MRNMLengthsString = new HashMap<String, AtomicLong>();

			for (int i = 0 ; i < MRNMLengths.length() ; i++) {
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
		SummaryReportUtils.postionSummaryMapToXml(bamReportElement, "RNAME_POS", rNamePosition);

		// FLAG
		SummaryReportUtils.lengthMapToXml(bamReportElement, "FLAG", flagBinaryCount);

		if (includeCoverage) {
			SummaryReportUtils.lengthMapToXml(bamReportElement, "Coverage", coverage);
		}

		if (includeMatrices) {
			Map<MAPQMiniMatrix, AtomicLong> cmMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
			Map<MAPQMiniMatrix, AtomicLong> smMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
			Map<MAPQMiniMatrix, AtomicLong> lengthMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
			Map<MAPQMiniMatrix, AtomicLong> nhMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
			Map<MAPQMiniMatrix, AtomicLong> zmMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();

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
		//		}
	}

	void generateMAPQSubMaps(Map<MAPQMiniMatrix, AtomicLong> cmMatrix,
			Map<MAPQMiniMatrix, AtomicLong> smMatrix,
			Map<MAPQMiniMatrix, AtomicLong> lengthMatrix,
			Map<MAPQMiniMatrix, AtomicLong> nhMatrix,
			Map<MAPQMiniMatrix, AtomicLong> zmMatrix) {

		logger.debug("mapQMatrix.size(): " + mapQMatrix.size());

		Map<Integer, AtomicLong> map = null;

		for (Entry<Integer, MAPQMatrix> entry : mapQMatrix.entrySet()) {
			logger.debug( entry.getKey() + ", entry.getValue().toString(): " + entry.getValue().toString());

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
	 * Parse a SAMRecord Collate various pieces of info from the SAMRecord ready
	 * for the summariser to retrieve
	 * 
	 * @param record
	 *            SAMRecord next row in file
	 */
	public void parseRecord(final SAMRecord record) throws Exception{
		updateRecordsParsed();
		MAPQMatrix matrix = null;

		final byte[] readString = record.getReadBases();
		final boolean reverseStrand = record.getReadNegativeStrandFlag();

		// check if record has its fail or duplicate flag set.
		// if so, miss out some of the summaries
		if (record.getDuplicateReadFlag()) {
			// increment duplicate count
			duplicateCount.incrementAndGet();
		} else if (record.getReadFailsVendorQualityCheckFlag()) {
			// 	increment duplicate count
			failedVendorQualityCheckCount.incrementAndGet();
		} else {

			byte[] qualBytes = record.getBaseQualities();	// faster than getBaseQualityString() 

			// SEQ
			SummaryByCycleUtils.parseCharacterSummary(seqByCycle, readString, reverseStrand);
			SummaryReportUtils.tallyBadReadsAsString(readString, seqBadReadLineLengths);

			// QUAL
			SummaryByCycleUtils.parseIntegerSummary(qualByCycleInteger, qualBytes, reverseStrand);
			SummaryReportUtils.tallyQualScores(qualBytes, qualBadReadLineLengths);

			// ISIZE
			String readGroup = (String) record.getAttribute(RG);
			if (null == readGroup) {
				SAMReadGroupRecord srgr = record.getReadGroup();
				if (null != srgr)
					readGroup = record.getReadGroup().getReadGroupId();
			}
			parseISize(record.getInferredInsertSize(), readGroup);
			
			//Xu code for soft, hard clips and readlenthg
			//private final ConcurrentMap<String, QCMGAtomicLongArray> 
			parseClipsByRG(record.getReadString(), record.getCigar(), readGroup);				
			
			// MRNM
			final int mateRefNameIndex = record.getMateReferenceIndex();
			if (mateRefNameIndex == -1) {
				MRNMLengths.increment(mateRefNameMinusOne);
			} else {
				MRNMLengths.increment(mateRefNameIndex);
			}

			// RNAME & POS
			parseRNameAndPos(record.getReferenceName(), record.getAlignmentStart());	// POSition value
			
			if (record.getReadPairedFlag()) {
				if (record.getFirstOfPairFlag()) {
					p1Lengths.increment(readString.length);
				} else if (record.getSecondOfPairFlag()) {
					p2Lengths.increment(readString.length);
				}
			}

			// coverage
			if (includeCoverage) {
				parseCoverage(record);
			}
		}

		//TODO mapping qual normally sits in the non-duplicate section - is this correct?
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
		parseTAGs(record, matrix, readString, reverseStrand);

		// CIGAR
		parseCigar(record.getCigar());

		// Flags
		flagIntegerCount.increment(record.getFlags());
	}
	
	
	//Xu code
	void parseClipsByRG(final String seq, final Cigar cigar, String readGroup) {
		
		if (null == readGroup) readGroup = "EMPTY"; 
				
		QCMGAtomicLongArray larray = rgReadLength.get(readGroup);
		 if( larray == null){
			 rgReadLength.putIfAbsent(readGroup, new QCMGAtomicLongArray(128));		
			 larray = rgReadLength.get(readGroup);
		 }
		 larray.increment(seq.length());
		 		 
		 QCMGAtomicLongArray harray = rgHardClip.get(readGroup);
		 if( harray == null){
			 rgHardClip.putIfAbsent(readGroup, new QCMGAtomicLongArray(128));	
			 harray = rgHardClip.get(readGroup);
		 }
		 QCMGAtomicLongArray sarray = rgSoftClip.get(readGroup);
		 if( sarray == null){
			rgSoftClip.putIfAbsent(readGroup, new QCMGAtomicLongArray(128));	
			sarray = rgSoftClip.get(readGroup);
		 }
		 if (null == cigar) return;
		 
		 int lhard = 0;
		 int lsoft = 0;
		 for(CigarElement ce : cigar.getCigarElements()) 
			 if(ce.getOperator().equals(CigarOperator.HARD_CLIP))
				 lhard += ce.getLength();
			 else if(ce.getOperator().equals(CigarOperator.SOFT_CLIP))
				 lsoft += ce.getLength();
		 
		 //debug
//		 if(lsoft > 0) System.out.println(cigar.toString()+  ", find soft clips: " + lsoft);
				 
		harray.increment(lhard);
		sarray.increment(lsoft);
			
	}

	private void parseTAGs(final SAMRecord record, final MAPQMatrix matrix, final byte[] readBases, final boolean reverseStrand) {
		Integer zm = null;
		Integer sm = null;

		// if the record has a CQ tag, then it will likely have a CS tag
		// if a record does not have a CQ tag, then it will not have a CS tag
		String value = (String) record.getAttribute(CQ);
		if (null != value) {
			// CQ
			byte[] valueB = SAMUtils.fastqToPhred(value);
			if (null != valueB) {
				SummaryByCycleUtils.parseIntegerSummary(tagCQByCycle, valueB);
				SummaryReportUtils.tallyQualScores(valueB, cqBadReadLineLengths);
			}
			// CS
			value = (String) record.getAttribute(CS);
			if (null != value) {
				SummaryByCycleUtils.parseCharacterSummary(tagCSByCycle, value, null, 1);
				SummaryReportUtils.tallyBadReadsAsString(value, csBadReadLineLengths);
			}
		}

		//TODO are there any tags we can exclude if the read is unmapped?

		//RG
		value = (String) record.getAttribute(RG);
		if (null != value)
			SummaryByCycleUtils.incrementCount(tagRGLineLengths, value);
		//ZM - not for torrent bams
		if ( ! torrentBam) {	
			value = (String) record.getAttribute(ZM);
			if (null != value) {
				tagZMLineLengths.increment(Integer.parseInt(value));
				if (includeMatrices) {
					zm = Integer.valueOf(Integer.parseInt(value));
					matrix.addToMatrix(zm, MatrixType.ZM);
				}
			}
		}
		//ZP - not for torrent bams
		if ( ! torrentBam) {	
			value = (String) record.getAttribute(ZP);
			if (null != value)
				SummaryByCycleUtils.incrementCount(tagZPLineLengths, value);
		}
		//ZB
		//		value = (String) record.getAttribute(ZB);
		//		if (null != value)
		//			SummaryByCycleUtils.incrementCount(tagZBLineLengths, value);
		//CM
		Integer iValue = (Integer) record.getAttribute(CM);
		if (null != iValue) {
			tagCMLineLengths.increment(iValue.intValue());
			//			SummaryByCycleUtils.incrementCount(tagCMLineLengths, iValue);
			if (includeMatrices)
				matrix.addToMatrix(iValue, MatrixType.CM);
		}

		//ZF - not for torrent bams
		if ( ! torrentBam) {	
			value = (String) record.getAttribute(ZF);
			if (StringUtils.isNumeric(value)) {				// only care about ints in this tag
				tagZFLineLengths.increment(Integer.parseInt(value));
				//					tagZFLineLengths.increment(ZF);
			}
		}
		//SM
		sm = (Integer) record.getAttribute(SM);
		if (null != sm) {
			tagSMLineLengths.increment(sm.intValue());
			if (includeMatrices)
				matrix.addToMatrix(sm, MatrixType.SM);
		}
		//IH
		iValue = (Integer) record.getAttribute(IH);
		if (null != iValue) {
			tagIHLineLengths.increment(iValue.intValue());
		}
		//NH
		iValue = (Integer) record.getAttribute(NH);
		if (null != iValue) {
			tagNHLineLengths.increment(iValue.intValue());
			if (includeMatrices)
				matrix.addToMatrix(iValue, MatrixType.NH);
		}
		//MD
		if ( ! includeMDTag) {
			value = (String) record.getAttribute(MD);
			if (null != value) {
				//				SummaryReportUtils.tallyMDMismatches(value, tagMDMismatchByCycle, readBases, mdRefAltLengthsForward);
				//				if (record.getCigarString().contains("I")) {
				//					logger.info("md: " + value + ", cigar: " + record.getCigarString() + ", readBases: " + new String(readBases));
				//				}
				SummaryReportUtils.tallyMDMismatches(value, record.getCigar(), tagMDMismatchByCycle, readBases, reverseStrand, mdRefAltLengthsForward, mdRefAltLengthsReverse);
				allReadsLineLengths.increment(record.getReadLength());
			}
		}

		// additionalTags
		if (null != tags)
			for (String s : tags) {
				value = (String) record.getAttribute(s);
				if (null != value)
					SummaryByCycleUtils.incrementCount(additionalTags.get(s), value);
			}
		// additionalTagsInt
		if (null != tagsInt)
			for (String s : tagsInt) {
				iValue = (Integer) record.getAttribute(s);
				if (null != iValue)
					additionalIntegerTags.get(s).increment(iValue.intValue());
			}
		// additionalTagsChar
		if (null != tagsChar) {
			Character c = null;
			for (String s : tagsChar) {
				c = (Character) record.getAttribute(s);
				if (null != c)
					SummaryByCycleUtils.incrementCount(additionalCharacterTags.get(s), c);
			}
		}

		if (includeMatrices && null != zm && null != sm)
			zmSmMatrix.increment(zm, sm);
	}



	void parseCoverage(SAMRecord record) {
		int count = 0;
		for (AlignmentBlock ab : record.getAlignmentBlocks()) {
			// find out how many positions were skipped...
			int referenceStart = ab.getReferenceStart();
			synchronized (coverageQueue) {
				int latestFromMap = coverageQueue.isEmpty() ? 0 :  coverageQueue.lastKey();
				if (latestFromMap < referenceStart) {
					zeroCoverageCount += referenceStart - latestFromMap + 1;
				}

				SummaryReportUtils.addPositionAndLengthToMap(coverageQueue, referenceStart, ab.getLength());

				// don't care about large coverage at the moment
				//				for (Entry<Integer,AtomicLong> entry : coverageQueue.entrySet()) {
				//					if (entry.getValue().get() >= 100000) {
				//						if ( ! largeCoverage.containsKey(entry.getKey()))
				//							largeCoverage.put(entry.getKey(), entry.getValue());
				//					}
				//				}

				// remove any items from map queue that have positions less than our current position
				// big assumption that we are dealing with coord sorted bams here..
				// only do this the first time round for each record as a record can have multiple AlignmentBlocks
				// and if we do this for each iteration we could stuff up the values if the next read is close by...
				if (++count == 1)
					removeCoverageFromQueueAndAddToMap(referenceStart, coverageQueue, coverage);

			}
		}
	}

	void removeCoverageFromQueueAndAddToMap(int referenceStart, Map<Integer, AtomicLong> queue, 
			ConcurrentMap<Integer, AtomicLong> map) {
		for (Iterator<Integer> it = ((ConcurrentSkipListMap<Integer,AtomicLong>) queue)
				.headMap(referenceStart).keySet().iterator() ; it.hasNext() ; ) {

			SummaryByCycleUtils.incrementCount(map, Integer.valueOf((int)queue.get(it.next()).get()));
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
				if ( ! CigarOperator.M.equals(operator)) {
					SummaryByCycleUtils.incrementCount(cigarValuesCount, "" + ce.getLength() + operator);
				}
				length += getSizeFromInt(ce.getLength()) + 1;
			}
			cigarLengths.increment(length);
		}
	}

	private int getSizeFromInt(int value) {
		if (value == 0) return 0;
		return 1 + getSizeFromInt(value/10);
	}


	void parseISize(final int iSize, String readGroup) {
		if (null == readGroup) readGroup = "EMPTY"; 
		// get absolute value
		final int absISize = Math.abs(iSize);

		if (absISize < SummaryReportUtils.MAX_I_SIZE) {

			AtomicLongArray readGroupArray = iSizeByReadGroupMap.get(readGroup);
			if (null == readGroupArray) {
				readGroupArray = new AtomicLongArray(SummaryReportUtils.MAX_I_SIZE);
				AtomicLongArray existingArray = iSizeByReadGroupMap.putIfAbsent(readGroup, readGroupArray);
				if (existingArray != null) {
					readGroupArray = existingArray;
				}
			}

			readGroupArray.incrementAndGet(absISize);

		} else {

			QCMGAtomicLongArray readGroupArray = iSizeByReadGroupMapBinned.get(readGroup);
			if (null == readGroupArray) {
				readGroupArray = new QCMGAtomicLongArray(1024);
				QCMGAtomicLongArray existingArray = iSizeByReadGroupMapBinned.putIfAbsent(readGroup, readGroupArray);
				if (existingArray != null) {
					readGroupArray = existingArray;
				}
			}

			readGroupArray.increment(absISize / SummaryReportUtils.FINAL_I_SIZE_BUCKET_SIZE);
		}

	}

	void parseRNameAndPos(final String rName, final int position) {
		PositionSummary ps = rNamePosition.get(rName);
		if (null == ps) {
			ps = rNamePosition.putIfAbsent(rName, new PositionSummary(position));
			if (null == ps)
				return;
		}
		ps.addPosition(position);
	}

	// ///////////////////////////////////////////////////////////////////////
	// The following methods are used by the test classes
	// ///////////////////////////////////////////////////////////////////////

	ConcurrentMap<String, AtomicLong> getCigarValuesCount() {
		return cigarValuesCount;
	}

	ConcurrentMap<Integer, AtomicLong> getISizeLengths() {
		ConcurrentMap<Integer, AtomicLong> iSizeLengths = new ConcurrentHashMap<Integer, AtomicLong>();


		for (Entry<String, AtomicLongArray> entry : iSizeByReadGroupMap.entrySet()) {
			// ignore read group for now - just checking that we get same results as previously
			// need to bin by 10 here
			AtomicLongArray array = entry.getValue();
			long longAdder = 0;
			for (int i = 0 ; i < array.length() ; i++) {
				longAdder +=array.get(i);

				if (i % 10 == 9 && longAdder > 0) {
					// update map and reset longAdder
					Integer binNumber = i  - 9;
					AtomicLong al = iSizeLengths.get(binNumber);
					if (null == al) {
						al = new AtomicLong();
						AtomicLong existingLong = iSizeLengths.putIfAbsent(binNumber, al);
						if (null != existingLong) al = existingLong;
					}
					al.addAndGet(longAdder);
					longAdder = 0;
				}
			}
			// add last entry to map
			if (longAdder > 0) {
				Integer binNumber = array.length() + 1 / 10;
				AtomicLong al = iSizeLengths.get(binNumber);
				if (null == al) {
					al = new AtomicLong();
					AtomicLong existingLong = iSizeLengths.putIfAbsent(binNumber, al);
					if (null != existingLong) al = existingLong;
				}
				al.addAndGet(longAdder);
			}
		}

		// now for the binned map
		for (Entry<String, QCMGAtomicLongArray> entry : iSizeByReadGroupMapBinned.entrySet()) {
			QCMGAtomicLongArray array = entry.getValue();
			for (int i = 0 ; i < array.length() ; i++) {
				long l = array.get(i);
				if (l > 0) {
					Integer binNumber = (i == 0 ? SummaryReportUtils.MAX_I_SIZE : i * 1000000);
					AtomicLong al = iSizeLengths.get(binNumber);
					if (null == al) {
						al = new AtomicLong();
						AtomicLong existingAL = iSizeLengths.putIfAbsent(binNumber, al);
						if (null != existingAL) {
							al = existingAL;
						}
					}
					al.addAndGet(l);

				}
			}
		}

		// first add in the 10 binned array, then the 1M
		//		for (int i = 0 ; i < iSizeLengths10.length() ; i++) {
		//			long l = iSizeLengths10.get(i);
		//			if (l > 0) {
		//				iSizeLengths.put(i, new AtomicLong(l));
		//			}
		//		}
		//		for (int i = 0 ; i < iSizeLengths1M.length() ; i++) {
		//			long l = iSizeLengths1M.get(i);
		//			if (l > 0) {
		//				if (i == 0)
		//					iSizeLengths.put(50000, new AtomicLong(l));
		//				else
		//					iSizeLengths.put(i * 1000000, new AtomicLong(l));
		//			}
		//		}

		return iSizeLengths;
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

	SummaryByCycleNew2 getTagCQByCycle() {
		return tagCQByCycle;
	}
	
	//xu code:
	
	//get counts of clip reads 
	ConcurrentMap<String, AtomicLong>  getRGCounts(){
		ConcurrentMap<String, AtomicLong> counts = new ConcurrentHashMap<String, AtomicLong>();
	
		for (Entry<String, QCMGAtomicLongArray> entry : rgSoftClip.entrySet()) {

			long Scount = 0, Hcount = 0;
			 
			QCMGAtomicLongArray array = entry.getValue();
			long noSoft = array.get(0);
			for (int i = 0 ; i < array.length() ; i++) 
				Scount += array.get(i);
			
			counts.put(entry.getKey() + ":total", new AtomicLong(Scount));
			counts.put(entry.getKey() + ":noSoftClip", new AtomicLong(array.get(0)));
			counts.put(entry.getKey() + ":noHardClip", new AtomicLong(rgHardClip.get(entry.getKey()).get(0)));
		}
		return counts;
		
	}
	//return the min, max, mode and median of read length, clips length for each group
	ConcurrentMap<String, AtomicLong> getRGStats( ConcurrentMap<String, QCMGAtomicLongArray> rgClips) {
		ConcurrentMap<String, AtomicLong> clips = new ConcurrentHashMap<String, AtomicLong>();

		// now for the binned map
		for (Entry<String, QCMGAtomicLongArray> entry : rgClips.entrySet()) {
			QCMGAtomicLongArray array = entry.getValue();
			
			//get the position of median
			long sum = 1,counts = 0;
			for (int i = 1 ; i < array.length() ; i++){
				counts += array.get(i);
				sum += i * array.get(i);
			}
			int mean = (int) (sum / counts);
			
			int min = 0, max = 0, mode = 0, median = 0; 
			long count = 0,highest = 0;
			for (int i = 1 ; i < array.length() ; i++) {
				if(array.get(i) > 0){
					//last non-zero position
					max = i;
					
					//first non-zero position
					min = ( min == 0 ) ? i : min; 
					
					//mode is the number of read which length is most popular
					if(array.get(i) > highest){
						highest = array.get(i);
						mode = i; 
					}  
				}
			}
			
			long highestBar = (array.get(mode) > array.get(0)) ? mode : 0;
			//add stats to Map				
			clips.put(entry.getKey() +":min", new AtomicLong(min) );
			clips.put(entry.getKey() +":max", new AtomicLong(max) );
			clips.put(entry.getKey() +":mode", new AtomicLong(mode)) ;
			clips.put(entry.getKey() +":mean", new AtomicLong(mean) );
			clips.put(entry.getKey() +":highestBar", new AtomicLong(highestBar));
		}
		
		return clips;		 
	}
	
	ConcurrentMap<String, AtomicLong> getTagRGLineLengths() {

		return tagRGLineLengths;
		//		ConcurrentMap<String, AtomicLong> tagRGLL = new ConcurrentHashMap<>();
		//		
		//		for (Entry<String, AtomicLongArray> entry : iSizeByReadGroupMap.entrySet()) {
		//			long rgTally = 0;
		//			AtomicLongArray array = entry.getValue();
		//			for (int i = 0, len = array.length() ; i < len ; i++) {
		//				rgTally += array.get(i);
		//			}
		//			
		//			tagRGLL.putIfAbsent(entry.getKey(), new AtomicLong(rgTally));
		//		}
		//		
		//		// and now for the binned collection
		//		for (Entry<String, QCMGAtomicLongArray> entry : iSizeByReadGroupMapBinned.entrySet()) {
		//			long rgTally = 0;
		//			QCMGAtomicLongArray array = entry.getValue();
		//			long len = array.length();
		//			for (int i = 0 ; i < len ; i++) {
		//				rgTally += array.get(i);
		//			}
		//			AtomicLong existingValue = tagRGLL.get(entry.getKey());
		//			// this should not be null
		//			if (null == existingValue) {
		//				existingValue = new AtomicLong();
		//				tagRGLL.putIfAbsent(entry.getKey(), existingValue);
		//			}
		//			existingValue.addAndGet(rgTally);
		//		}
		//		
		//		return tagRGLL;
	}

	QCMGAtomicLongArray  getTagZMLineLengths() {
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

	public String getBamHeader() {
		return bamHeader;
	}
	public void setSamSequenceDictionary(SAMSequenceDictionary samSeqDictionary) {
		this.samSeqDictionary = samSeqDictionary;
	}

	public SAMSequenceDictionary getSamSequenceDictionary() {
		return samSeqDictionary;
	}
}
