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

import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.SAMUtils;

import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.KmersSummary;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.qcmg.qvisualise.util.SummaryByCycleNew2;
import org.qcmg.qvisualise.util.SummaryByCycleUtils;
import org.w3c.dom.Element;

public class FastqSummaryReport extends SummaryReport {
	
	private static final Character c = Character.MAX_VALUE;
	private static final Integer i = Integer.MAX_VALUE;
	
	//SEQ
	private final SummaryByCycleNew2<Character> seqByCycle = new SummaryByCycleNew2<Character>(c, 512);
	private Map<Integer, AtomicLong> seqLineLengths = null;
	private final QCMGAtomicLongArray seqBadReadLineLengths = new QCMGAtomicLongArray(128);
	private final KmersSummary kmersSummary = new KmersSummary( KmersSummary.MAX_KMERS ); //default use biggest mers length
	
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
			
			Element readNameElement = createSubElement(element, "ReadNameAnalysis");
			
			// header breakdown
			SummaryReportUtils.lengthMapToXml(readNameElement, "INSTRUMENTS", instruments);
			SummaryReportUtils.lengthMapToXml(readNameElement, "RUN_IDS", runIds);
			SummaryReportUtils.lengthMapToXml(readNameElement, "FLOW_CELL_IDS", flowCellIds);
			SummaryReportUtils.lengthMapToXml(readNameElement, "FLOW_CELL_LANES", flowCellLanes);
			SummaryReportUtils.lengthMapToXml(readNameElement, "TILE_NUMBERS", tileNumbers);
			
			Map<String, AtomicLong> pairs = new HashMap<>();
			pairs.put("1", firstInPair);
			pairs.put("2", secondInPair);
			SummaryReportUtils.lengthMapToXml(readNameElement, "PAIR_INFO", pairs);
			
			Map<String, AtomicLong> filtered = new HashMap<>();
			filtered.put("Y", filteredY);
			filtered.put("N", filteredN);
			SummaryReportUtils.lengthMapToXml(readNameElement, "FILTER_INFO", filtered);
			SummaryReportUtils.lengthMapToXml(readNameElement, "INDEXES", indexes);
			
			Map<String, AtomicLong> qualHeaders = new HashMap<>();
			qualHeaders.put("non +", qualHeaderNotEqualToPlus);
			qualHeaders.put("+", new AtomicLong(getRecordsParsed() - qualHeaderNotEqualToPlus.longValue()));
			SummaryReportUtils.lengthMapToXml(readNameElement, "QUAL_HEADERS", qualHeaders);
						
			// create the length maps here from the cycles objects
			seqLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(seqByCycle, getRecordsParsed());
			qualLineLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(qualByCycleInteger, getRecordsParsed());
			
			// SEQ
			Element seqElement = createSubElement(element, "SEQ");
			seqByCycle.toXml(seqElement, "BaseByCycle");
			SummaryReportUtils.lengthMapToXmlTallyItem(seqElement, "LengthTally", seqLineLengths);
			SummaryReportUtils.lengthMapToXml(seqElement, "BadBasesInReads", seqBadReadLineLengths);
			
			kmersSummary.toXml(seqElement,kmersSummary.MAX_KMERS); //debug
			kmersSummary.toXml(seqElement,1); //add 1-mers
			kmersSummary.toXml(seqElement,2); //add 2-mers
			kmersSummary.toXml(seqElement,3); //add 3-mers
			
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
				// QUAL   it also throw exception if fastq reads is invalid
				byte[] baseQualities = SAMUtils.fastqToPhred(record.getBaseQualityString());
				SummaryByCycleUtils.parseIntegerSummary(qualByCycleInteger, baseQualities, reverseStrand);
				SummaryReportUtils.tallyQualScores(baseQualities, qualBadReadLineLengths);
				
				
				// SEQ
				byte[] readBases = record.getReadString().getBytes();
				SummaryByCycleUtils.parseCharacterSummary(seqByCycle, readBases, reverseStrand);
				SummaryReportUtils.tallyBadReadsAsString(readBases, seqBadReadLineLengths);
				kmersSummary.parseKmers( readBases, false ); //fastq base are all orignal forward
				
				// header stuff
				if (record.getReadName().contains(":")) {
					String [] headerDetails = TabTokenizer.tokenize(record.getReadName(), ':');
					if (null != headerDetails && headerDetails.length > 0) {
						
						//if length is equal to 10, we have the classic Casava 1.8 format
						
						int headerLength = headerDetails.length;
						
					if (headerLength == 5) {
						//@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2
						//@ERR091788 - machine id
						//3104 - read position in file
						// HSQ955 - flowcell
						// 155 - run_id
						// 2 - flowcell lane
						// 1101 - tile
						// 13051 - x
						// 2071 - y
						// 2 - 2nd in pair
						if (record.getReadName().contains(" ")) {
							parseFiveElementHeaderWithSpaces(headerDetails);
						} else {
							parseFiveElementHeaderNoSpaces(headerDetails);
						}
					} else {
						
							String key = headerDetails[0];
							updateMap(instruments, key);
							
							// run Id
							if (headerLength > 1) {
								key = headerDetails[1];
								updateMap(runIds, key);
								
								// flow cell id
								if (headerLength > 2) {
									key = headerDetails[2];
									updateMap(flowCellIds, key);
									
									// flow cell lanes
									if (headerLength > 3) {
										key = headerDetails[3];
										updateMap(flowCellLanes, key);
										
										// tile numbers within flow cell lane
										if (headerLength > 4) {
											key = headerDetails[4];
											try {
												Integer intKey = Integer.valueOf(key);
												updateMap(tileNumbers, intKey);
											} catch (NumberFormatException nfe) {
												logger.error("Can't convert string to integer: " + key, nfe);
												throw nfe;
											}
											
											// skip x, y coords for now
											if (headerLength > 6) {
												key = headerDetails[6];
												// this may contain member of pair information
												getPairInfo(key);
												
												// filtered
												if (headerLength > 7) {
													key = headerDetails[7];
													if ("Y".equals(key)) {
														filteredY.incrementAndGet();
													} else if ("N".equals(key)) {
														filteredN.incrementAndGet();
													}
													
													// skip control bit for now
													
													// indexes
													if (headerLength > 9) {
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
				}
				
				String qualHeader = record.getBaseQualityHeader();
				
				// If header just contains "+" then FastqRecord has null for qual header
				if ( ! StringUtils.isNullOrEmpty(qualHeader)) {
					qualHeaderNotEqualToPlus.incrementAndGet();
				}
				
			}
		}
	}

	private void getPairInfo(String key) {
		int index = key.indexOf(" ");
		if (index == -1) {
			index = key.indexOf("/");
		}
		if (index != -1) {
			char c = key.charAt(index + 1);
			if (c == '1') {
				firstInPair.incrementAndGet();
			} else if (c == '2') {
				secondInPair.incrementAndGet();
			} else {
				logger.warn("unexpected value for member of pair: " + c + " from " + key);
			}
		}
	}
	
	/**
	 * 	//@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2
							//@ERR091788 - machine id
							//3104 - read position in file
							// HSQ955 - flowcell
							// 155 - run_id
							// 2 - flowcell lane
							// 1101 - tile
							// 13051 - x
							// 2071 - y
							// 2 - 2nd in pair
	 */
	void parseFiveElementHeaderWithSpaces(String [] params) {
		// split by space
		String [] firstElementParams = params[0].split(" ");
		if (firstElementParams.length != 2) {
			throw new UnsupportedOperationException("Incorrect header format encountered in parseFiveElementHeader. Expected '@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2' but recieved: " + Arrays.deepToString(params));
		}
		String [] machineAndReadPosition = firstElementParams[0].split("\\.");
		if (machineAndReadPosition.length != 2) {
			throw new UnsupportedOperationException("Incorrect header format encountered in parseFiveElementHeader. Expected '@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2' but recieved: " + Arrays.deepToString(params));
		}
		
		updateMap(instruments, machineAndReadPosition[0]);
		
		String [] flowCellAndRunId = firstElementParams[1].split("_");
		if (flowCellAndRunId.length != 2) {
			throw new UnsupportedOperationException("Incorrect header format encountered in parseFiveElementHeader. Expected '@ERR091788.3104 HSQ955_155:2:1101:13051:2071/2' but recieved: " + Arrays.deepToString(params));
		}
		
		updateMap(flowCellIds, flowCellAndRunId[0]);
		updateMap(runIds, flowCellAndRunId[1]);
		
		updateMap(flowCellLanes, params[1]);
		updateMap(tileNumbers, Integer.parseInt(params[2]));
		// skip x, and y coords for now..
		getPairInfo(params[4]);
	}
	
	/**
	 * @HWUSI-EAS100R:6:73:941:1973#0/1
	 * HWUSI-EAS100R	the unique instrument name
	* 6	flowcell lane
	* 73	tile number within the flowcell lane
	* 941	'x'-coordinate of the cluster within the tile
	* 1973	'y'-coordinate of the cluster within the tile
	* #0	index number for a multiplexed sample (0 for no indexing)
	* /1	the member of a pair, /1 or /2 (paired-end or mate-pair reads only)
	* 
	* 
	* OR
	* 
	* @HS2000-1107_220:6:1115:6793:38143/1
	* HS2000-1107 	the unique instrument name
	* 220	runId 
	* 6	flowcell lane
	* 1115	tile number within the flowcell lane
	* 6793	'x'-coordinate of the cluster within the tile
	* 38143	'y'-coordinate of the cluster within the tile
	* /1	the member of a pair, /1 or /2 (paired-end or mate-pair reads only)
	*
	 * @param params
	 */
	void parseFiveElementHeaderNoSpaces(String [] params) {
		
		/*
		 * If instrument name contains an underscore, split on this and the RHS becomes the run id!
		 * If no underscore, no run_id and the 
		 */
		int underscoreIndex = params[0].indexOf('_');
		if (underscoreIndex > -1) {
			updateMap(runIds,  params[0].substring(underscoreIndex + 1));
			updateMap(instruments, params[0].substring(0, underscoreIndex));
		} else {
			updateMap(instruments, params[0]);
		}
		
		updateMap(flowCellLanes, params[1]);
		updateMap(tileNumbers, Integer.parseInt(params[2]));
		// skip x, and y coords for now..
		getPairInfo(params[4]);
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
