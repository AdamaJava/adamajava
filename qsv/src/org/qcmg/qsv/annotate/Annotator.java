/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.annotate;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTag;
import jakarta.xml.bind.Marshaller;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.Constants;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.util.QSVConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * Class to annotate discordant pairs
 *
 */
public class Annotator  {
	
	public static final String X0 = "X0";
	public static final String XT = "XT";
	public static final short X0_SHORT = SAMTag.makeBinaryTag(X0);
	public static final short XA_SHORT = SAMTag.makeBinaryTag("XA");
	public static final short SA_SHORT = SAMTag.makeBinaryTag("SA");
	public static final short ZN_SHORT = SAMTag.makeBinaryTag("ZN");

	private final File file;
	private static final QLogger logger = QLoggerFactory.getLogger(Annotator.class);
	private final int isizeLowerLimit;
	private final int isizeUpperLimit;
	private final int averageiSize;
	private final ConcurrentHashMap<String, AtomicLong> zpToCount = new ConcurrentHashMap<>();
	private Double physCoverage;
	private Double baseCoverage;
	private final AtomicLong annotatedCount = new AtomicLong();
	private final AtomicLong unrecognizedCount = new AtomicLong();
	private final AtomicLong singletons = new AtomicLong();
	private final AtomicLong duplicates = new AtomicLong();
	private final String annotatorType;
	private final ConcurrentMap <String, RunTypeRecord> sequencingRunsMap;
	private final String pairingType;
	private final String mapper;
	private final AtomicLong abbConvertedCount = new AtomicLong();

	public Annotator(int isizeLowerLimit, int isizeUpperLimit, File file, String type, List<RunTypeRecord> sequencingRuns, String pairingType, String mapper) throws Exception {
		if (isizeLowerLimit >= isizeUpperLimit) {
			throw new QSVException("ANNOTATE_ERROR");					
		}
		this.isizeLowerLimit = isizeLowerLimit;
		this.isizeUpperLimit = isizeUpperLimit;
		this.physCoverage = null;
		this.baseCoverage = null;
		this.averageiSize = ((this.isizeUpperLimit - this.isizeLowerLimit)/2) + this.isizeLowerLimit;
		this.file = file;
		this.annotatorType = type.equals("lifescope") ? "lmp" : type;

		// put values from sequencingRuns into map keyed on id
		this.sequencingRunsMap = new ConcurrentHashMap<>();
		if ( ! sequencingRuns.isEmpty()) {
			for (RunTypeRecord rtr : sequencingRuns) {
				sequencingRunsMap.put(rtr.getRgId(), rtr);
			}
		}
		this.pairingType = pairingType;
		this.mapper = mapper;
	}

	/**
	 * Annotate the current SAMRecord to see if it is discordant
	 * @param record
	 * @throws QSVException
	 */
	public void annotate(SAMRecord record) throws QSVException {
		SAMReadGroupRecord rg = record.getReadGroup();
		annotate(record, null != rg ? rg.getId() : null);
		
	}
	public void annotate(SAMRecord record, String rgId) throws QSVException {

		//NH is the number of reported alignments. Only get those that have a single alignment
		//		record.getAttribute("NH");

		//need to set it based on mapper and pairing type
		if (record.getAttribute(QSVConstants.NH_SHORT) == null) {
			if ( ! pairingType.equals("lmp")) {
				setNHAttribute(mapper, record);
			}
		}
		int lower = -1;
		int upper = -1;

		//find the correct upper and lower insert sizes		
		if (rgId != null) {
			RunTypeRecord rtr = sequencingRunsMap.get(rgId);
			if (null != rtr) {
				lower = rtr.getLower();
				upper = rtr.getUpper();
			}
		}

		//counts of singletons and duplicates
		if ( ! record.getDuplicateReadFlag() && ! record.getReadFailsVendorQualityCheckFlag()) {
			singletons.incrementAndGet();
		} else {
			duplicates.incrementAndGet();
		}

		annotateRecord(record, lower, upper);
	}

	public void annotateByTumorISize(int minInsize, int maxIsize, SAMRecord r) throws QSVException {
		annotateRecord(r, minInsize, maxIsize);
	}

	/**
	 * Annotate current SAMRecord with the given upper and lower isizes
	 * @param record
	 * @param lower
	 * @param upper
	 * @throws QSVException
	 */
	public void annotateRecord(SAMRecord record, int lower, int upper) throws QSVException {
		if (lower != -1 || upper != -1) {

			if (record.getReadPairedFlag()) {
				//solid lmp
				switch (annotatorType) {
					case "lmp" -> {
						SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, lower, upper);
						lmp.createZPAnnotation();
						countZp(lmp.getZPAnnotation());
					}
					//paired end
					case "pe" -> {
						String zpAnnotation = ZPAnnotator.createZPAnnotation(record, lower, upper);
						record.setAttribute(QSVConstants.ZP, zpAnnotation);
						countZp(zpAnnotation);
					}
					//illumna mate pair
					case "imp" -> {
						IlluminaLongMatePairRecord imp = new IlluminaLongMatePairRecord(record, lower, upper);
						imp.createZPAnnotation();
						//if (record.getFirstOfPairFlag()) {
						countZp(imp.getZPAnnotation());
						if (imp.isAbbConverted()) {
							abbConvertedCount.incrementAndGet();
						}
						// }
					}
					default -> throw new QSVException("ANNOTATION_ERROR", annotatorType);
				}

				//count the annotation			
				annotatedCount.incrementAndGet();			
			} else {	
				//read unpaired
				if (record.getDuplicateReadFlag()) {
					record.setAttribute(QSVConstants.ZP, "W**");
					countZp("W**");
				} else {
					record.setAttribute(QSVConstants.ZP, "X**");
					countZp("X**");
				}
			}
		} else {
			if (record.getReadPairedFlag()) {
				if (record.getDuplicateReadFlag()) {
					record.setAttribute(QSVConstants.ZP, "Y**");					
					countZp("Y**");
				} else {
					record.setAttribute(QSVConstants.ZP, "T**");
					countZp("T**");
				}				
			} else {
				if (record.getDuplicateReadFlag()) {
					record.setAttribute(QSVConstants.ZP, "W**");
					countZp("W**");
				} else {
					record.setAttribute(QSVConstants.ZP, "X**");
					countZp("X**");
				}				
			}			

			unrecognizedCount.incrementAndGet();
		}
	}

	private void countZp(String zpAnnotation) {
		AtomicLong al = zpToCount.get(zpAnnotation); 
		if (null == al) {
			AtomicLong newValue = new AtomicLong(1);
			al = zpToCount.putIfAbsent(zpAnnotation, newValue);
			if (null != al) {
				newValue.addAndGet(al.get());
			}
		} else {
			al.incrementAndGet();
		}
	}

	/*
	 * Calculate physical and base coverage
	 */
	private void calculateCoverage() {
		long genomeBases = 3137161264L;
		//Average physical coverage: (The number of unique/non-redundant AAA read pairs x the average window
		//size) / the number of bases in the genome (3137161264)
		AtomicLong aaaPairs = zpToCount.get("AAA");

		if (aaaPairs == null) {
			physCoverage = 0d;
		} else { 
			physCoverage = (aaaPairs.doubleValue() * averageiSize) / genomeBases;
			logger.info("Physical coverage: " + physCoverage);
		}

		//(Number of non-redundant reads x read length)/ the number of bases in the
		// genome (3137161264)
		baseCoverage = (singletons.doubleValue() * 50) / genomeBases;
		logger.info("Base coverage: " + baseCoverage);

	}

	/*
	 * Generate discordant pair counts report
	 */
	private String generateReport(Marshaller m) throws Exception {
		PairingStats report = new PairingStats();
		PairingStats.InsertRange range = new PairingStats.InsertRange();
		range.setLowerLimit(BigInteger.valueOf(this.isizeLowerLimit));
		range.setUpperLimit(BigInteger.valueOf(this.isizeUpperLimit));        
		range.setAverage(BigInteger.valueOf(this.averageiSize));
		report.setInsertRange(range);
		for (Entry<String, AtomicLong> entry : zpToCount.entrySet()) {
			PairingStats.UniquePairing pairing = new PairingStats.UniquePairing();
			pairing.setType(entry.getKey());
			pairing.setCount(BigInteger.valueOf(entry.getValue().longValue()));
			report.getUniquePairing().add(pairing);
		}

		//add abbConverted count
		if (pairingType.equals("imp")) {
			PairingStats.UniquePairing pairing = new PairingStats.UniquePairing();
			pairing.setType("ABBtoAAA");
			pairing.setCount(BigInteger.valueOf(abbConvertedCount.longValue()));
			report.getUniquePairing().add(pairing);
		}

		addReadCount("total reads", duplicates.longValue() + singletons.longValue(), report);
		addReadCount("duplicates", duplicates.longValue(), report);
		addReadCount("singletons", singletons.longValue(), report);

		logger.info("Total reads: " + (duplicates.longValue() + singletons.longValue()) + " Duplicates: " + duplicates.longValue() + " Singletons: " + singletons.longValue() + " Unrecognized: " + unrecognizedCount.longValue());
		//Coverage
		calculateCoverage();
		DecimalFormat df = new DecimalFormat("#.#####");
		PairingStats.Coverage physical = new PairingStats.Coverage();
		physical.setType("average physical coverage");
		physical.setValue(df.format(physCoverage));
		report.getCoverage().add(physical);

		PairingStats.Coverage base = new PairingStats.Coverage();
		base.setType("base coverage");
		base.setValue(df.format(baseCoverage));        
		report.getCoverage().add(base);

		StringWriter writer = new StringWriter();  
		m.marshal(report, writer);
		return writer.toString();
	}

	private void addReadCount(String type, long l, PairingStats report) {
		PairingStats.ReadCount readCount = new PairingStats.ReadCount();
		readCount.setType(type);
		readCount.setCount(l);  
		report.getReadCount().add(readCount);
	}

	public void writeReport(Marshaller m) throws Exception {
		try (FileWriter fw = new FileWriter(file);
				BufferedWriter writer = new BufferedWriter(fw)) {
			writer.write(generateReport(m));        
		}
	}

	public AtomicLong getSingletons() {
		return singletons;
	}

	public AtomicLong getDuplicates() {
		return duplicates;
	}

	public AtomicLong getTotalCount() {
		return new AtomicLong(singletons.longValue() + duplicates.longValue());
	}

	public Map<String, AtomicLong> getZpCount() {
		return zpToCount;
	}

	/**
	 * Set the NH attribute for the SAMRecord
	 * @param mapper
	 * @param record
	 * @throws QSVException
	 */
	public static void setNHAttribute(String mapper, SAMRecord record) throws QSVException {

		//bwa
		switch (mapper) {
			case "bwa" -> {

				// annotate the record
				if (record.getReadUnmappedFlag() || record.isSecondaryAlignment()) {
					record.setAttribute(QSVConstants.NH, 0);
					//X0 = unique
				} else {
					Object xo = record.getAttribute(X0_SHORT);
					if (xo != null) {
						record.setAttribute(QSVConstants.NH, xo);
					} else {

						// X0 is absent, use XT
						Character xt = record.getCharacterAttribute(XT);
						if (xt != null) {
							//mate rescued
							if (xt == 'M') {
								// include m
								String xa = (String) record.getAttribute(XA_SHORT);
								if (xa == null) {
									record.setAttribute(QSVConstants.NH, 1);
								} else {
									int value = xa.split(Constants.SEMI_COLON_STRING).length + 1;
									record.setAttribute(QSVConstants.NH, value);
								}
							} else {
								record.setAttribute(QSVConstants.NH, 0);
							}
						} else {
							record.setAttribute(QSVConstants.NH, 0);
						}
					}
				}
			}
			case "bwa-mem" -> {
				if (record.getReadUnmappedFlag() || record.isSecondaryAlignment()) {
					record.setAttribute(QSVConstants.NH, 0);
				} else {
					String xa = (String) record.getAttribute(SA_SHORT);
					if (xa != null) {
						int value = xa.split(Constants.SEMI_COLON_STRING).length + 1;
						record.setAttribute(QSVConstants.NH, value);
					} else {
						record.setAttribute(QSVConstants.NH, 1);
					}
				}
			}
			case "novoalign" -> {
				Integer zn = (Integer) record.getAttribute(ZN_SHORT);
				if (zn != null) {
					record.setAttribute(QSVConstants.NH, zn);
				} else if (record.isSecondaryAlignment()) {
					record.setAttribute(QSVConstants.NH, 2);
				} else {
					record.setAttribute(QSVConstants.NH, 1);
				}
			}
			default -> throw new QSVException("UNKNOWN_MAPPER");
		}
	}

}
