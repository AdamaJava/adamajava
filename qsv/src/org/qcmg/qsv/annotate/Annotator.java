/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.annotate;

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

import javax.xml.bind.Marshaller;

import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qsv.QSVException;

/**
 * 
 * Class to annotate discordant pairs
 *
 */
public class Annotator  {

	private final File file;
	private static QLogger logger = QLoggerFactory.getLogger(Annotator.class);
	private final int isizeLowerLimit;
	private final int isizeUpperLimit;
	private final int averageiSize;
	private final ConcurrentHashMap<String, AtomicLong> zpToCount = new ConcurrentHashMap<>();
	private String xmlReport;
	private Double physCoverage;
	private Double baseCoverage;
	private final AtomicLong annotatedCount = new AtomicLong();
	private final AtomicLong unrecognizedCount = new AtomicLong();
	private final AtomicLong singletons = new AtomicLong();
	private final AtomicLong duplicates = new AtomicLong();
//	private final AtomicLong totalCount = new AtomicLong();
	private final String annotatorType;
	private final List<RunTypeRecord> sequencingRuns;
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
		this.sequencingRuns = sequencingRuns;
		
		// put values from sequencingRuns into map keyed on id
		this.sequencingRunsMap = new ConcurrentHashMap<>();
		if ( ! this.sequencingRuns.isEmpty()) {
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
	 * @throws Exception
	 */
	public void annotate(SAMRecord record) throws QSVException {

		//NH is the number of reporte alignments. Only get those that have a single alignment
		//		record.getAttribute("NH");

		//need to set it based on mapper and pairing type
		if (record.getAttribute("NH") == null) {
			if ( ! pairingType.equals("lmp")) {
				setNHAttribute(mapper, record);
			}
		}
		int lower = -1;
		int upper = -1;

		//find the correct upper and lower insert sizes		
		if (record.getReadGroup() != null) {
			RunTypeRecord rtr = sequencingRunsMap.get(record.getReadGroup().getId());
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
//				if (annotatorType.equals("lifescope")) {
//					annotatorType = "lmp";
//				}
				//solid lmp
				if (annotatorType.equals("lmp")) {
					SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, lower, upper);
					lmp.createZPAnnotation();

					countZp(lmp.getZPAnnotation());		           
					lmp = null;
					//paired end
				} else if (annotatorType.equals("pe")){
					PairedEndRecord pe = new PairedEndRecord(record, lower, upper);
					pe.createZPAnnotation();

					countZp(pe.getZPAnnotation());		                 
					pe = null;
					//illumna mate pair    
				} else if (annotatorType.equals("imp")){			    	
					IlluminaLongMatePairRecord imp = new IlluminaLongMatePairRecord(record, lower, upper);
					imp.createZPAnnotation();
					//if (record.getFirstOfPairFlag()) {
					countZp(imp.getZPAnnotation());
					if (imp.isAbbConverted()) {
						abbConvertedCount.incrementAndGet();
					}
					// }       
					imp = null;
				} else {
					throw new QSVException("ANNOTATION_ERROR", annotatorType);
				}			

				//count the annotation			
				annotatedCount.incrementAndGet();			
			} else {	
				//read unpaired
				if (record.getDuplicateReadFlag()) {
					record.setAttribute("ZP", "W**");
					countZp("W**");
				} else {
					record.setAttribute("ZP", "X**");
					countZp("X**");
				}
			}
		} else {
			if (record.getReadPairedFlag()) {
				if (record.getDuplicateReadFlag()) {
					record.setAttribute("ZP", "Y**");					
					countZp("Y**");
				} else {
					record.setAttribute("ZP", "T**");
					countZp("T**");
				}				
			} else {
				if (record.getDuplicateReadFlag()) {
					record.setAttribute("ZP", "W**");
					countZp("W**");
				} else {
					record.setAttribute("ZP", "X**");
					countZp("X**");
				}				
			}			

			unrecognizedCount.incrementAndGet();
		}
	}

	private void countZp(String zpAnnotation) {
		AtomicLong al =zpToCount.get(zpAnnotation); 
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
			physCoverage = Double.valueOf(0d);
		} else { 
			physCoverage = Double.valueOf((aaaPairs.doubleValue() * averageiSize) / genomeBases);           
			logger.info("Physical coverage: " + physCoverage);
		}

		//(Number of non-redundant reads x read length)/ the number of bases in the
		// genome (3137161264)
		baseCoverage = Double.valueOf((singletons.doubleValue() * 50) / genomeBases);
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
		xmlReport = writer.toString();
		return xmlReport;
	}

	private void addReadCount(String type, long l, PairingStats report) {
		PairingStats.ReadCount readCount = new PairingStats.ReadCount();
		readCount.setType(type);
		readCount.setCount(l);  
		report.getReadCount().add(readCount);
	}

	public void writeReport(Marshaller m) throws Exception {
		try (FileWriter fw = new FileWriter(file);
				BufferedWriter writer = new BufferedWriter(fw);) {
			writer.write(generateReport(m));        
		}
	}

	public Double getPhysCoverage() {
		return physCoverage;
	}

	public Double getBaseCoverage() {
		return baseCoverage;
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
	public void setNHAttribute(String mapper, SAMRecord record) throws QSVException {

		//bwa
		if (mapper.equals("bwa")) {

			// annotate the record
			if (record.getReadUnmappedFlag() || record.getNotPrimaryAlignmentFlag()) {
				record.setAttribute("NH", 0);
				//X0 = unqiue
			} else if (record.getAttribute("X0") != null) {
				// X0 == NH
				record.setAttribute("NH", record.getAttribute("X0"));
			} else {
				// X0 is absent, use XT
				if (record.getCharacterAttribute("XT") != null) {
					//mate rescued
					if (record.getCharacterAttribute("XT") == 'M') {
						// include m
						if (record.getAttribute("XA") == null) {
							record.setAttribute("NH", 1);
						} else {
							String xa = (String) record.getAttribute("XA");
							int value = xa.split(";").length + 1;
							record.setAttribute("NH", value);
						}
					} else {
						record.setAttribute("NH", 0);
					}
				} else {
					record.setAttribute("NH", 0);
				}
			}
		} else if (mapper.equals("bwa-mem")) {
			if (record.getReadUnmappedFlag() || record.getNotPrimaryAlignmentFlag()) {
				record.setAttribute("NH", 0);
			} else if (record.getAttribute("SA") != null) {				
				String xa = (String) record.getAttribute("SA");
				int value = xa.split(";").length + 1;
				record.setAttribute("NH", value);
			} else {
				record.setAttribute("NH", 1);
			}
		} else if (mapper.equals("novoalign")) {
			if ((Integer)record.getAttribute("ZN") != null) {
				record.setAttribute("NH", record.getAttribute("ZN"));	
			} else if (record.getNotPrimaryAlignmentFlag()) {
				record.setAttribute("NH", 2);
			} else {
				record.setAttribute("NH", 1);
			}			
		} else {
			throw new QSVException("UNKNOWN_MAPPER");
		}
	}

}
