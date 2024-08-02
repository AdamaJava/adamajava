package org.qcmg.qprofiler2.bam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.CycleSummaryUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecord.SAMTagAndValue;
import htsjdk.samtools.SAMTagUtil;

// for sam record tag information
public class TagSummaryReport {

	public static final int ADDI_TAG_MAP_LIMIT = 100;
	public static final int errReadLimit  = 10;	
	public static final String separator = Constants.COLON_STRING;

	private static final SAMTagUtil STU = SAMTagUtil.getSingleton();

    // TAGS
	@SuppressWarnings("unchecked")
	final CycleSummary<Character>[] tagMDMismatchByCycle = new CycleSummary[] {new CycleSummary<>(BamSummaryReport.cc, 512), new CycleSummary<>(BamSummaryReport.cc, 512), new CycleSummary<>(BamSummaryReport.cc, 512)};
	private final QCMGAtomicLongArray[] mdRefAltLengthsForward = new QCMGAtomicLongArray[] {new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32)};	
	private final QCMGAtomicLongArray[] mdRefAltLengthsReverse = new QCMGAtomicLongArray[] {new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32)};	
    final QCMGAtomicLongArray[] allReadsLineLengths = new QCMGAtomicLongArray[] {new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024)};

	private final ConcurrentMap<String, ConcurrentMap<String, AtomicLong>> additionalTags = new ConcurrentHashMap<>();
	protected QLogger logger = QLoggerFactory.getLogger(getClass());	
	private long errMdReadNo = 0 ;	
	private final AtomicLong mdTagCounts = new AtomicLong();
	
	public void parseTAGs(final SAMRecord record, boolean isLongReadBam)  {
		String mdValue = null;
		for ( SAMTagAndValue tag : record.getAttributes()) {
			if (tag.tag.equals("MD")) {
				mdValue = (String) tag.value;
				continue;
			}
			
			// the type may be one of A (character), B (general array), f (real number), H (hexadecimal array), i (integer), or Z (string).
			// Note that H tag type is never written anymore, because B style is more compact.	         
			String type = ":B";			
			if (tag.value instanceof Integer) {
				type = ":i";
			} else if (tag.value instanceof Number ) {
				type = ":f";
			} else if (tag.value instanceof String) {
				type = ":Z";
			} else if (tag.value instanceof Character) {
				type = ":A";
			}

			Map<String, AtomicLong> map = additionalTags.computeIfAbsent(tag.tag + type, k -> new ConcurrentHashMap<>());
			XmlUtils.updateMapWithLimit(map, tag.value + "", ADDI_TAG_MAP_LIMIT);				 
		}
						
		// MD	 
		if (null != mdValue) {
			mdTagCounts.incrementAndGet();
			byte[] readBases = record.getReadBases();
			boolean reverseStrand = record.getReadNegativeStrandFlag();		

			// 0: unpaired , 1: firstOfPair , 2: secondOfPair				
			int order = (!record.getReadPairedFlag()) ? 0 : (record.getFirstOfPairFlag()) ? 1 : 2;					
			String err = CycleSummaryUtils.tallyMDMismatches( mdValue, record.getCigar(), tagMDMismatchByCycle[order],
					readBases, reverseStrand, mdRefAltLengthsForward[order], mdRefAltLengthsReverse[order], isLongReadBam);
			// limit err message on log file
			if ( err != null && (( errMdReadNo ++) < errReadLimit)) {
				logger.warn(record.getReadName() + ": " + err);
			}
			
			// this counts will be used to calculate % for MD
			for ( int i = 1, len = readBases.length; i <= len; i ++ ) {
				allReadsLineLengths[order].increment(i);		
			}		
		}		
	}
	
	public void toXml(Element parent) {
				
		// "tags:MD:Z" mismatchbycycle
		if ( mdTagCounts.get() > 0) {
			Element ele = XmlUtils.createMetricsNode(parent, "tags:MD:Z",
                    new Pair<>(ReadGroupSummary.READ_COUNT, mdTagCounts.get()));
			for (int order = 0; order < 3; order ++) {
				// so choose 1st cycle base counts as read count, since all read at least have one base. 
				tagMDMismatchByCycle[order].toXml( ele, BamSummaryReport.sourceName.get(order), allReadsLineLengths[order].get(1) );
				
			}
					
			for (String strand : new String[] {"ForwardStrand", "ReverseStrand" }) {				
				for (int order = 0; order < 3; order ++) {				
					Map<String, AtomicLong> mdRefAltLengthsString = new HashMap<>();
					QCMGAtomicLongArray mdRefAltLengths = (strand.contains("Forward")) ? mdRefAltLengthsForward[order] : mdRefAltLengthsReverse[order];				
					for (int m = 0 ; m < mdRefAltLengths.length() ; m++) {
						long l = mdRefAltLengths.get(m);
						if (l <= 0)  {
							continue;
						}
						mdRefAltLengthsString.put(CycleSummaryUtils.getStringFromInt(m), new AtomicLong(l));					 
					}
					String name = BamSummaryReport.sourceName.get(order) + strand; 				
					XmlUtils.outputTallyGroup(ele,  name, mdRefAltLengthsString, true, true);				
				}		
			}			
		}
		
		// additional tags includes RG
		// sort as we are no longer using SkipListMap
		List<String> sortedKeys = additionalTags.keySet().stream().sorted().toList();
		for (String key : sortedKeys) {
			outputTag(parent, key, additionalTags.get(key));
		}
	}
	
	
	private <T> void outputTag(Element ele, String tag,  Map<T, AtomicLong> tallys) {

		/*
		 *  create sorted map, and pass that to outputTallyGroupWithSize
		 */
		Map<T, AtomicLong> sortedMap = new TreeMap<>(tallys);
		long counts = sortedMap.values().stream().mapToLong(AtomicLong::get).sum();
		
		ele = XmlUtils.createMetricsNode(ele, "tags:" + tag, new Pair<>(ReadGroupSummary.READ_COUNT, counts));
			
		String name = tag.substring(0, tag.indexOf(separator));
		XmlUtils.outputTallyGroupWithSize(ele, name, sortedMap, ADDI_TAG_MAP_LIMIT, false);
					
	}
	
}
