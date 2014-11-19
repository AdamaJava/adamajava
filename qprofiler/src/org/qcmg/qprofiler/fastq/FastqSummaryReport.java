/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 * © Copyright QIMR Berghofer Medical Research Institute 2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.fastq;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.picard.fastq.FastqRecord;
import net.sf.samtools.SAMUtils;

import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.SummaryByCycleNew2;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.w3c.dom.Element;

public class FastqSummaryReport extends SummaryReport {
	
	private static final Character c = Character.MAX_VALUE;
	private static final Integer i = Integer.MAX_VALUE;
	
	//SEQ
	private final SummaryByCycleNew2<Character> seqByCycle = new SummaryByCycleNew2<Character>(c, 512);
	private Map<Integer, AtomicLong> seqLineLengths = null;
	private final QCMGAtomicLongArray seqBadReadLineLengths = new QCMGAtomicLongArray(128);
	
	
	ConcurrentMap<String, AtomicLong> kmers = new ConcurrentHashMap<>();

	//QUAL
	private final SummaryByCycleNew2<Integer> qualByCycleInteger = new SummaryByCycleNew2<Integer>(i, 512);
	private Map<Integer, AtomicLong> qualLineLengths = null;
	private final QCMGAtomicLongArray qualBadReadLineLengths = new QCMGAtomicLongArray(128);
	
	// Header info
	ConcurrentMap<String, AtomicLong> instruments = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> runIds = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> flowCellIds = new ConcurrentHashMap<>();
	ConcurrentMap<String, AtomicLong> flowCellLanes = new ConcurrentHashMap<>();
	ConcurrentMap<Integer, AtomicLong> tileNumbers = new ConcurrentHashMap<>();
	AtomicLong firstInPair = new AtomicLong();
	AtomicLong secondInPair = new AtomicLong();
	AtomicLong filteredY = new AtomicLong();
	AtomicLong filteredN = new AtomicLong();
	ConcurrentMap<String, AtomicLong> indexes = new ConcurrentHashMap<>();
	
	AtomicLong qualHeaderNotEqualToPlus = new AtomicLong();
	
	private  String[] excludes;
	private boolean excludeAll;
	private boolean reverseStrand;
	
	public FastqSummaryReport() {
		super();
	}
	
	public FastqSummaryReport(String [] excludes) {
		super();
		this.excludes = excludes;
		if (null != excludes) {
			for (String exclude : excludes) {
				if ("all".equalsIgnoreCase(exclude)) {
					excludeAll = true;
					break;
				}
			}
		}
		logger.debug("Running with excludeAll: " + excludeAll);
	}
	
	@Override
	public void toXml(Element parent) {
		
		Element element = init(parent, ProfileType.FASTQ, null, excludes, null);
		if ( ! excludeAll) {
			
			// header breakdown
			SummaryReportUtils.lengthMapToXml(element, "INSTRUMENTS", instruments);
			SummaryReportUtils.lengthMapToXml(element, "RUN_IDS", runIds);
			SummaryReportUtils.lengthMapToXml(element, "FLOW_CELL_IDS", flowCellIds);
			SummaryReportUtils.lengthMapToXml(element, "FLOW_CELL_LANES", flowCellLanes);
			SummaryReportUtils.lengthMapToXml(element, "TILE_NUMBERS", tileNumbers);
			
			Map<String, AtomicLong> pairs = new HashMap<>();
			pairs.put("1", firstInPair);
			pairs.put("2", secondInPair);
			SummaryReportUtils.lengthMapToXml(element, "PAIR_INFO", pairs);
			
			Map<String, AtomicLong> filtered = new HashMap<>();
			filtered.put("Y", filteredY);
			filtered.put("N", filteredN);
			SummaryReportUtils.lengthMapToXml(element, "FILTER_INFO", filtered);
			SummaryReportUtils.lengthMapToXml(element, "INDEXES", indexes);
			
			Map<String, AtomicLong> qualHeaders = new HashMap<>();
			qualHeaders.put("non +", qualHeaderNotEqualToPlus);
			qualHeaders.put("+", new AtomicLong(getRecordsParsed() - qualHeaderNotEqualToPlus.longValue()));
			SummaryReportUtils.lengthMapToXml(element, "QUAL_HEADERS", qualHeaders);
			
			logger.info("no of kmers: " + kmers.size());
			SummaryReportUtils.lengthMapToXml(element, "KMERS", kmers);
			
			// create the length maps here from the cycles objects
			seqLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(seqByCycle, getRecordsParsed());
			qualLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(qualByCycleInteger, getRecordsParsed());
			
			// SEQ
			Element seqElement = createSubElement(element, "SEQ");
			seqByCycle.toXml(seqElement, "BaseByCycle");
			SummaryReportUtils.lengthMapToXmlTallyItem(seqElement, "LengthTally", seqLineLengths);
			SummaryReportUtils.lengthMapToXml(seqElement, "BadBasesInReads", seqBadReadLineLengths);
			
			// QUAL
			Element qualElement = createSubElement(element, "QUAL");
			qualByCycleInteger.toXml(qualElement, "QualityByCycle");
			SummaryReportUtils.lengthMapToXmlTallyItem(qualElement, "LengthTally", qualLineLengths);
			SummaryReportUtils.lengthMapToXml(qualElement, "BadQualsInReads", qualBadReadLineLengths);
		}
	}
	
	/**
	 * Reads a row from the text file and returns it as a string
	 * 
	 * @return next row in file
	 */
	public void parseRecord(FastqRecord record) {
		if (null != record) {
			
			updateRecordsParsed();
			
			if ( ! excludeAll) {
				
				// SEQ
				byte[] readBases = record.getReadString().getBytes();
				SummaryByCycleUtils.parseCharacterSummary(seqByCycle, readBases, reverseStrand);
				SummaryReportUtils.tallyBadReadsAsString(readBases, seqBadReadLineLengths);
				
				// split read into 6-mers and tally
				int kmerLength = 6;
				for (int i = 0, len = readBases.length - kmerLength ; i < len ; i++) {
					String kmer = new String(Arrays.copyOfRange(readBases, i, i+kmerLength));
					updateMap(kmers, kmer);
				}
				

				// QUAL
				byte[] baseQualities = SAMUtils.fastqToPhred(record.getBaseQualityString());
				SummaryByCycleUtils.parseIntegerSummary(qualByCycleInteger, baseQualities, reverseStrand);
				SummaryReportUtils.tallyQualScores(baseQualities, qualBadReadLineLengths);
				
				// header stuff
				if (record.getReadHeader().contains(":")) {
					String [] headerDetails = TabTokenizer.tokenize(record.getReadHeader(), ':');
					if (null != headerDetails && headerDetails.length > 0) {
						String key = headerDetails[0];
						updateMap(instruments, key);
						
						// run Id
						if (headerDetails.length > 1) {
							key = headerDetails[1];
							updateMap(runIds, key);
							
							// flow cell id
							if (headerDetails.length > 2) {
								key = headerDetails[2];
								updateMap(flowCellIds, key);
								
								// flow cell lanes
								if (headerDetails.length > 3) {
									key = headerDetails[3];
									updateMap(flowCellLanes, key);
									
									// tile numbers within flow cell lane
									if (headerDetails.length > 4) {
										key = headerDetails[4];
										try {
											Integer intKey = Integer.valueOf(key);
											updateMap(tileNumbers, intKey);
										} catch (NumberFormatException nfe) {
											logger.error("Can't convert string to integer: " + key, nfe);
										}
										
										// skip x, y coords for now
										if (headerDetails.length > 6) {
											key = headerDetails[6];
											// this may contain member of pair information
											int index = key.indexOf(" ");
											if (index == -1) {
												index = key.indexOf("/");
											}
											if (index != -1) {
												char c = key.charAt(index + 1);
												if (c == '1') {
													firstInPair.incrementAndGet();
												} else if (c == '2') {
													firstInPair.incrementAndGet();
												} else {
													logger.warn("unexpected value for member of pair: " + c + " from " + key);
												}
											}
											
											// filtered
											if (headerDetails.length > 7) {
												key = headerDetails[7];
												if ("Y".equals(key)) {
													filteredY.incrementAndGet();
												} else if ("N".equals(key)) {
													filteredN.incrementAndGet();
												}
												
												// skip control bit for now
												
												// indexes
												if (headerDetails.length > 9) {
													key = headerDetails[9];
													updateMap(indexes, key);
												}	// thats it!!
											}
										}
									}
								}
							}
						}
					}
				}
				
				String qualHeader = record.getBaseQualityHeader();
				
				// If header just contains "+" then FastqRecord has null for qual header
				if ( ! StringUtils.isNullOrEmpty(qualHeader)) {
					qualHeaderNotEqualToPlus.incrementAndGet();
				}
				
			}
		}
	}

	private <T> void updateMap(ConcurrentMap<T, AtomicLong> map , T key) {
		AtomicLong al = map.get(key);
		if (null == al) {
			al = new AtomicLong();
			AtomicLong existing = map.putIfAbsent(key, al);
			if (null != existing) {
				al = existing;
			}
		}
		al.incrementAndGet();
	}
	
	SummaryByCycleNew2<Character> getFastqBaseByCycle() {
		return seqByCycle;
	}
}
