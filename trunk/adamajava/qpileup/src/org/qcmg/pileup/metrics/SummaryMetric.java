/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.commandline.BlockingExecutor;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.hdf.PositionDS;
import org.qcmg.pileup.hdf.StrandDS;
import org.qcmg.pileup.metrics.record.ClipRecord;
import org.qcmg.pileup.metrics.record.IndelRecord;
import org.qcmg.pileup.metrics.record.MetricRecord;
import org.qcmg.pileup.metrics.record.NonReferenceMetricRecord;
import org.qcmg.pileup.metrics.record.ResultSummary;
import org.qcmg.pileup.metrics.record.ResultRecord;

import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;


public class SummaryMetric {
	
	private QLogger logger = QLoggerFactory.getLogger(getClass());
	protected String hdfHeader;
	protected String uuid;
	protected String execHeader;
	protected String hdfName;
	protected String pileupDir;
	protected String wiggleDir;
	protected final static String TAB_DELIMITER = PileupConstants.TAB_DELIMITER;
	protected TreeMap<String, Metric> metrics = new TreeMap<String, Metric>();
	protected ConcurrentSkipListSet<Chromosome> finishedChromosomes = new ConcurrentSkipListSet<Chromosome>();
	protected ConcurrentHashMap<String,TreeMap<Integer,Integer>> totalBasesMap = new ConcurrentHashMap<String,TreeMap<Integer,Integer>>();
	private String distributionDir;	
	private String summaryDir;
	private String[] headerList;
	public final int WINDOW_SIZE;
	public final int STEP_SIZE;
	public final AtomicLong recordCount = new AtomicLong();
	private String trackName;
	private ConcurrentHashMap<String, AtomicLong> countsMap = new ConcurrentHashMap<String, AtomicLong>(); 
	private String tmpDir;
	private String pathToBigWig;
	private String chromSizes;
	private List<Chromosome> chromosomes;
	private Integer minBasesPerPatient;
	//private BaseDistributionRecord totalBaseDistribution;
	private TreeMap<Integer, Integer> totalBaseCountMap;
	private int totalPatients;
	
	public SummaryMetric(String hdfFileName, String pileupDir, String wiggleDir, String baselineDir, String summaryDir, String tmpDir) {
		this.hdfName = hdfFileName;
		this.pileupDir = pileupDir;
		this.wiggleDir = wiggleDir;
		this.distributionDir = baselineDir;
		this.summaryDir = summaryDir;
		this.tmpDir = tmpDir;
		
		if (!new File(wiggleDir).exists()) {
			new File(wiggleDir).mkdir();
		}
		this.trackName = hdfFileName + "_mismapping";
		WINDOW_SIZE = PileupConstants.WINDOW_SIZE;
		STEP_SIZE = PileupConstants.STEP_SIZE;
		countsMap.put("total", new AtomicLong());
		countsMap.put(PileupConstants.LOW_MAPPING_QUAL, new AtomicLong());
		countsMap.put(PileupConstants.IRREGULAR, new AtomicLong());
		countsMap.put(PileupConstants.REGULAR, new AtomicLong());
		countsMap.put(PileupConstants.HCOV, new AtomicLong());
		countsMap.put(PileupConstants.MIXED, new AtomicLong());
		countsMap.put(PileupConstants.MIXED_AND_LOWMAPING, new AtomicLong());
		countsMap.put(PileupConstants.MIXED_AND_IRREGULAR, new AtomicLong());
		countsMap.put(PileupConstants.MIXED_AND_REGULAR, new AtomicLong());
		countsMap.put(PileupConstants.MIXED_AND_HCOV, new AtomicLong());
		this.totalBaseCountMap = new TreeMap<Integer, Integer>();	
	}	
	
	public String getHdfName() {
		return hdfName;
	}

	public void setHdfName(String hdfName) {
		this.hdfName = hdfName;
	}

	public String getPileupDir() {
		return pileupDir;
	}

	public void setPileupDir(String pileupDir) {
		this.pileupDir = pileupDir;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getExecHeader() {
		return execHeader;
	}

	public void setExecHeader(String execHeader) {
		this.execHeader = execHeader;
	}
	
	public String getHdfHeader() {
		return hdfHeader;
	}

	public void setHdfHeader(String hdfHeader) {
		this.hdfHeader = hdfHeader;
	}	

	public void addMetric(String key, Metric metric) {
		metrics.put(key, metric);		
	}

	public void addMetric(String type, Double minPositionCount, Integer minWindowCount, Integer minBasesPerPatient) throws Exception {
		Metric met = null;
		
		if (type.equals(PileupConstants.METRIC_CLIP)) {
			met = new ClipMetric(minPositionCount, minWindowCount, minBasesPerPatient);
		} else if (type.equals(PileupConstants.METRIC_HCOV)) {
			met = new HighCoverageMetric(minPositionCount, minWindowCount, minBasesPerPatient);
		} else if (type.equals(PileupConstants.METRIC_NONREFBASE)) {
			met = new NonReferenceMetric(minPositionCount, minWindowCount, minBasesPerPatient);
		}  else if (type.equals(PileupConstants.METRIC_UNMAPPED_MATE)) {
			met = new MatesUnmappedMetric(minPositionCount, minWindowCount, minBasesPerPatient);
		} else {
			throw new Exception("Type not recognized: " + type);
		}
			
		metrics.put(type, met);						
	}

	public void start(String hdfHeader, String uuid, String exec, List<Chromosome> chromosomes, int totalPatients) throws IOException {
		int metricCount = 0;
		this.hdfHeader = hdfHeader;
		this.uuid = uuid; 
		this.execHeader = exec;
		this.chromosomes = chromosomes;
		this.totalPatients = totalPatients;
		logger.info("Window size: " + WINDOW_SIZE);
		logger.info("Step size: " + STEP_SIZE);
		logger.info("Total patients: " + totalPatients);
		logger.info("Required min number of reads/patient: " + minBasesPerPatient);
		
		for (Entry<String, Metric> entry: metrics.entrySet()) {
			
    		Metric metric = entry.getValue();
    		metric.setTotalPatients(totalPatients);
    		//if (!entry.getKey().equals(PileupConstants.METRIC_TOTALBASE)) {
			metricCount++;
    		logger.info("METRIC: " + metricCount);
    		metric.logOptions();		    		
    		metric.open();
	    		
    		//if wigToBigWig path is supplied, write wiggle to temp folder and create bigWig
    		if (pathToBigWig != null) {
    			metric.writeHeader(hdfHeader, execHeader, uuid, tmpDir);
    		} else {
    			metric.writeHeader(hdfHeader, execHeader, uuid, wiggleDir);	    			
    		}
    		//} 
    	}
	}	
	
	public void finish() throws Exception {
        //close metrics output files and write distribution counts
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(distributionDir + PileupConstants.FILE_SEPARATOR + "total_base_distribution.txt")));
		w.write("TotalPatients\t"+totalPatients+"\n");
		w.write("TotalReads\tNumberPositions\n");
		
		for (Entry<Integer, Integer> entry: totalBaseCountMap.entrySet()) {
			w.write(entry.getKey() + "\t" + entry.getValue() + "\n");
		}
		w.close();		
		
        for (Entry<String, Metric> entry: metrics.entrySet()) {	
	    		Metric metric = entry.getValue();
	    		metric.write(pileupDir, distributionDir);
	   	 		metric.close();
    	}        
           
        String gffName = writeSummaryGFF();                
        
        //write final SNP records and compare with gff
    	if (metrics.containsKey(PileupConstants.METRIC_SNP)) {
    		SnpMetric metric = (SnpMetric)metrics.get(PileupConstants.METRIC_SNP);
    		metric.writeRecords(gffName, StrandEnum.getMetricsElements(), chromosomes);
   	 		metric.close();	    		
    	}
        
    	List<String> finalCounts = getFinalCountsStrings();
        writeCountsToLog(finalCounts);
        writeToMetricsSummaryFile(finalCounts);
	}	
	
	public String toHeaderString() {
		StringBuilder sb = new StringBuilder();
		
		this.headerList = new String[getHeaderSize()];
		int count = 0;
		
		for (String name : metrics.keySet()) {			
			//use average in heading
			if (name.equals(PileupConstants.METRIC_MAPPING) || name.equals(PileupConstants.METRIC_HCOV)) {
				headerList[count] = name;
				sb.append("no_of_" + name + "_positions\ttotal_"+ name + "_average\t" );
			} else {
				//use counts	
				if (name.equals(PileupConstants.METRIC_INDEL) || name.equals(PileupConstants.METRIC_CLIP) || name.equals(PileupConstants.METRIC_NONREFBASE)) {
					sb.append("no_of_" + name + "_positions\ttotal_"+ name + "_counts\t" );
					headerList[count] = name;
				}
			}
			
			count++;	
						
		}

		return sb.toString();
	}

	private int getHeaderSize() {
		int count = 0;
		for (Entry<String, Metric> s: metrics.entrySet()) {
			if (s.getKey().equals(PileupConstants.METRIC_MAPPING) || s.getKey().equals(PileupConstants.METRIC_HCOV) ||  s.getKey().equals(PileupConstants.METRIC_INDEL) || 
					s.getKey().equals(PileupConstants.METRIC_NONREFBASE) || s.getKey().equals(PileupConstants.METRIC_CLIP)) {
				count++;
			}				
		}
		return count;
	}

	private List<String> getFinalCountsStrings() {
		List<String> finalCounts = new ArrayList<String>();
		finalCounts.add("Total positions: " + countsMap.get("total").longValue());       
		finalCounts.add("Total low quality mapping positions alone: " + countsMap.get(PileupConstants.LOW_MAPPING_QUAL).longValue());
		finalCounts.add("Total low quality mapping positions including mixed: " + countsMap.get(PileupConstants.MIXED_AND_LOWMAPING).longValue());
		finalCounts.add("Total irregular mapping positions alone: " + countsMap.get(PileupConstants.IRREGULAR).longValue());
		finalCounts.add("Total irregular mapping positions including mixed: " + countsMap.get(PileupConstants.MIXED_AND_IRREGULAR).longValue());
		finalCounts.add("Total regular mapping positions alone: " + countsMap.get(PileupConstants.REGULAR).longValue());
		finalCounts.add("Total regular mapping positions including mixed: " + countsMap.get(PileupConstants.MIXED_AND_REGULAR).longValue());       
		finalCounts.add("Total high coverage positions alone: " + countsMap.get(PileupConstants.HCOV).longValue());
		finalCounts.add("Total high coverage positions mixed: " + countsMap.get(PileupConstants.MIXED_AND_HCOV).longValue());       
		finalCounts.add("Total mixed positions: " + countsMap.get(PileupConstants.MIXED).longValue());
		finalCounts.add("Percent low mapping quality positions alone: " + getCountsMapPercent(PileupConstants.LOW_MAPPING_QUAL));
		finalCounts.add("Percent low mapping quality positions including mixed: " + getCountsMapPercent(PileupConstants.MIXED_AND_LOWMAPING));
		finalCounts.add("Percent irregular mapping positions alone: " + getCountsMapPercent(PileupConstants.IRREGULAR));
		finalCounts.add("Percent irregular mapping positions including mixed: " + getCountsMapPercent(PileupConstants.MIXED_AND_IRREGULAR));
		finalCounts.add("Percent regular mapping positions alone: " + getCountsMapPercent(PileupConstants.REGULAR));
		finalCounts.add("Percent regular mapping positions  including mixed: " + getCountsMapPercent(PileupConstants.MIXED_AND_REGULAR));
		finalCounts.add("Percent high coverage positions alone: " + getCountsMapPercent(PileupConstants.HCOV));
		finalCounts.add("Percent high coverage positions  including mixed: " + getCountsMapPercent(PileupConstants.MIXED_AND_HCOV));
		finalCounts.add("Percent mixed positions: " + getCountsMapPercent(PileupConstants.MIXED));
		return finalCounts;				
	}

	private void writeToMetricsSummaryFile(List<String> finalCounts) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File( pileupDir+ PileupConstants.FILE_SEPARATOR + "options.summary.txt")));
		writer.write("===SUMMARY===\n");
		
		writer.write("HDF: " + hdfName + "\n\n");
		for (Entry<String, Metric> entry : metrics.entrySet()) {			
			for (String s:entry.getValue().getOptionsSummary()) {
				writer.write(s + "\n");
			}
			writer.write("\n");
		}
		
		for (String s: finalCounts) {
			writer.write(s + "\n");
		}
		
		writer.close();
	}

	private void writeCountsToLog(List<String> lines) {		
		for (String line: lines) {
			logger.info(line);
		}	
	}
	
	private String getCountsMapPercent(String key) {
		DecimalFormat df = new DecimalFormat("#.####");
		
		double percent = ((double) countsMap.get(key).longValue()/(double)countsMap.get("total").longValue()) * 100;
        return df.format(percent);
	}
	
	public synchronized void writeFinalRecords(Chromosome chromosome, PileupHDF hdf) throws Exception {
		
		if (metrics.containsKey(PileupConstants.METRIC_STRAND_BIAS)) {			
			StrandBiasMetric strandBias = (StrandBiasMetric) metrics.get(PileupConstants.METRIC_STRAND_BIAS);
			strandBias.writeRecords(chromosome);
		}
		for (Entry<String, Metric> entry: metrics.entrySet()) {			
			entry.getValue().clear(chromosome);
		}	
		finishedChromosomes.add(chromosome);
	}	
	
	public synchronized void writeRegularRecords(Chromosome chromosome, int startPos, int endPos) throws Exception {
		List<MetricRecord> records = new ArrayList<MetricRecord>();
		if (metrics.containsKey(PileupConstants.METRIC_INDEL) || metrics.containsKey(PileupConstants.METRIC_NONREFBASE) ||
				metrics.containsKey(PileupConstants.METRIC_CLIP) 
				) {
			
			for (int i = startPos; i <= endPos; i++){
				if (metrics.containsKey(PileupConstants.METRIC_CLIP)) {
					ClipMetric clipMetric = (ClipMetric) metrics.get(PileupConstants.METRIC_CLIP);					
					ClipRecord clip = (ClipRecord) clipMetric.getRegularRecord(chromosome.getName(), i);
					if (clip != null) {
						records.add(clip);						
					}
				}
				if (metrics.containsKey(PileupConstants.METRIC_INDEL)) {
					IndelMetric indel = (IndelMetric) metrics.get(PileupConstants.METRIC_INDEL);
					
					IndelRecord ins = indel.getRegularInsRecord(chromosome.getName(), i);
					IndelRecord del = indel.getRegularDelRecord(chromosome.getName(), i);
					if (ins != null) {
						records.add(ins);
					}
					if (del != null) {
						records.add(del);
					}
				}
				if (metrics.containsKey(PileupConstants.METRIC_NONREFBASE)) {
				
					NonReferenceMetric nonReferenceQPileupMetric = (NonReferenceMetric) metrics.get(PileupConstants.METRIC_NONREFBASE);
					MetricRecord nonref = nonReferenceQPileupMetric.getRegularRecord(chromosome.getName(), i);
					
					if (nonref != null) {
						records.add(nonref);						
					}
				}
	    	}
		}
		//write results to temporary summary file
		writeTmpRegularityFile(chromosome, records);
	}

	public synchronized void writeRecords(Chromosome chromosome, int startPos, int endPos) throws Exception {
		
		//for the specific region of the current chromosome, get window values for each metric, then clear from map
		int winNum = (endPos - startPos + 1) / WINDOW_SIZE + 1;
		TreeMap<Integer, ResultSummary> baseResults = new TreeMap<Integer, ResultSummary>();
		TreeMap<Integer, ResultSummary> results = new TreeMap<Integer, ResultSummary>();
		for (int i = 0; i < winNum; i++){			
			int start = i * WINDOW_SIZE + startPos;
		  	int end = (i + 1 ) * WINDOW_SIZE + startPos -1;	  	
		  	if (start > endPos) {
		  		break;
		  	}
		  	
		  	for (int j=start; j<=end; j+=STEP_SIZE) {
		  		int currentStart = j;
		  		int currentEnd = j + WINDOW_SIZE -1;
		  		if (currentEnd > chromosome.getEndPos()) {
		  			currentEnd = chromosome.getEndPos();
		  		}
		  		if (currentStart >= currentEnd) {
		  			break;
		  		}
		  		long totalBases = getTotalBasesByWindowStart(chromosome.getName(), currentStart, currentEnd);
		  		getWindow(baseResults, results, chromosome.getName(), currentStart, currentEnd, totalBases);		  		
		  	}		  	
    	}
		//write results to temporary summary file
		writeTmpSummaryFile(chromosome, results);
		
		//clear map
		for (Entry<String, Metric> entry: metrics.entrySet()) {	
			//for snp file, write the snps to a temporary file for later processing
			if (metrics.containsKey(PileupConstants.METRIC_SNP)) {
				SnpMetric snp = (SnpMetric) metrics.get(PileupConstants.METRIC_SNP);
				snp.writeTmpRecords(chromosome);
			}			
			if (!entry.getKey().equals(PileupConstants.METRIC_STRAND_BIAS)) {
				entry.getValue().clear(chromosome);			
			}		
		}	
	}

	public int getTotalBasesByWindowStart(String chr, int start, int end) {
		int count = 0;
		if (totalBasesMap.get(chr) != null) {
			TreeMap<Integer, Integer> chrMap = totalBasesMap.get(chr);	
			
			NavigableMap<Integer, Integer> subMap = chrMap.subMap(start, true, end, true);
			if (subMap != null) {
				for (Entry<Integer, Integer> entry: subMap.entrySet()) {
					count += entry.getValue();
				}
			}
		}
		return count;
	}

	private synchronized void getWindow(TreeMap<Integer, ResultSummary> baseResults, TreeMap<Integer, ResultSummary> results, String name, int start, int end, long total) throws IOException {
		ResultSummary summary = new ResultSummary(name, start, end, total, false);
		for (Entry<String, Metric> entry: metrics.entrySet()) {
			if (entry.getKey().equals(PileupConstants.METRIC_MAPPING) || entry.getKey().equals(PileupConstants.METRIC_CLIP) || 
					entry.getKey().equals(PileupConstants.METRIC_HCOV) ||
					entry.getKey().equals(PileupConstants.METRIC_INDEL) || entry.getKey().equals(PileupConstants.METRIC_NONREFBASE)
					) {				
				ResultRecord rr = entry.getValue().getWindow(name, start, end, total); 
				summary.addRecord(entry.getKey(), rr);
			}
		}
		//only write if it has some values greater than 0
		if (summary.isGreaterThanZero()) {
			results.put(summary.getStart(), summary);
		}
	}
	
	private void writeTmpRegularityFile(Chromosome chromosome, List<MetricRecord> records) throws IOException {			

		BufferedWriter writer = new BufferedWriter(new FileWriter(getTmpRegularityFile(chromosome), true));
		for (MetricRecord r: records) {	
			writer.write(r.toTmpString());	
		}
		writer.close();
	}

	private File getTmpRegularityFile(Chromosome chromosome) {
		return new File(summaryDir + PileupConstants.FILE_SEPARATOR + chromosome.getName() + ".regular.metric.summary.txt");
	}

	private void writeTmpSummaryFile(Chromosome chromosome, Map<Integer, ResultSummary> results) throws IOException {		
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(getTmpSummaryFile(chromosome), true));
		for (Map.Entry<Integer, ResultSummary> entry: results.entrySet()) {
			if (entry.getValue().isGreaterThanZero()) {				
				writer.write(entry.getValue().toTmpString(headerList));				
			}			
		} 
		writer.close();
	}

	public void addChromosome(Chromosome chromosome) throws IOException {
		
		//total base map
		if (!totalBasesMap.containsKey(chromosome.getName())) {
			TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
			totalBasesMap.put(chromosome.getName(), map);
		}
		
		for (Entry<String, Metric> entry: metrics.entrySet()) { 	    			
			entry.getValue().addChromosome(chromosome.getName());    		
		}
		
		countsMap.get("total").addAndGet(chromosome.getSectionLength());
		
		File summaryFile = getSummaryFile(chromosome); 
		if (!summaryFile.exists()) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(summaryFile, true));			
			//header
			writer.write("#chr\tstartPos\tendPos\t" + toHeaderString() + "total_bases_per_window\n");
			
			writer.close();			
		}
	}

	private File getSummaryFile(Chromosome chromosome) {
		return new File(summaryDir + PileupConstants.FILE_SEPARATOR + chromosome.getName() + ".metric.summary.txt");
	}

	private File getTmpSummaryFile(Chromosome chromosome) {
		return new File(tmpDir + PileupConstants.FILE_SEPARATOR + chromosome.getName() + ".metric.summary.txt");
	}

	public void processRecords(Chromosome chromosome, PositionDS position,
			StrandDS forward, StrandDS reverse, int startPos, int endPos) throws Exception {

		for (int i=0; i<position.getDatasetLength(); i++) {
			QPileupRecord qRecord = new QPileupRecord(position.getPositionElement(i), forward.getStrandElementMap(i, StrandEnum.getMetricsElements()), reverse.getStrandElementMap(i, StrandEnum.getMetricsElements()));	
			if (qRecord.inRequiredRegion(startPos, endPos)) {
				int totalRecords = qRecord.getTotalReads(true, true);
				addToTotalCountsMap(totalRecords);
				addToTotalBasesMap(qRecord.getChromosome(), (int)qRecord.getBasePosition(), totalRecords);
				for (Entry<String, Metric> entry: metrics.entrySet()) {
					Metric metric = entry.getValue();				
					metric.processRecord(qRecord, totalRecords);				
				}
			}
		}
//		logger.info("**Sizes: ");
//		for (Entry<String, Metric> entry: metrics.entrySet()) {
//			Metric metric = entry.getValue();				
//			logger.info(entry.getKey() + " " + metric.size(chromosome));				
//		}
//		logger.info("bfore: " + totalBasesMap.get(chromosome.getName()).size());
		writeRegularRecords(chromosome, startPos, endPos);
		writeRecords(chromosome, startPos, endPos);
		
		//clear total base map
		totalBasesMap.get(chromosome.getName()).clear();
//		logger.info("**Sizes after: ");
//		for (Entry<String, Metric> entry: metrics.entrySet()) {
//			Metric metric = entry.getValue();				
//			logger.info(entry.getKey() + " " + metric.size(chromosome));				
//		}
//		logger.info("after: " + totalBasesMap.get(chromosome.getName()).size());
	}

	private synchronized void addToTotalCountsMap(Integer totalRecords) {
		
		if (totalBaseCountMap.containsKey(totalRecords)) {
			Integer counts = totalBaseCountMap.get(totalRecords) + 1;
			totalBaseCountMap.put(totalRecords, counts);
		} else {
			totalBaseCountMap.put(totalRecords, 1);
		}
		
	}

	private void addToTotalBasesMap(String chr, int basePosition, int totalRecords) {
		
		totalBasesMap.get(chr).put(basePosition, totalRecords);		
	}

	public String writeSummaryGFF() throws Exception {		
		
		//write header for gff file
		String gffName = pileupDir + PileupConstants.FILE_SEPARATOR + hdfName + ".metrics.summary.gff3";
		BufferedWriter gffWriter = new BufferedWriter(new FileWriter(new File(gffName), true));		
		gffWriter.write("##gff-version 3" + PileupConstants.NEWLINE);
		gffWriter.write("#track name="+trackName+" graphType=bar" + PileupConstants.NEWLINE);
		logger.info("Creating summary gff3: " + gffName);
		
		//write header for gff regular file
		String regularGFF = pileupDir + PileupConstants.FILE_SEPARATOR + hdfName + ".metrics.regularity.summary.gff3";
		BufferedWriter regularWriter = new BufferedWriter(new FileWriter(new File(regularGFF), true));		
		regularWriter.write("##gff-version 3" + PileupConstants.NEWLINE);
		regularWriter.write("#track name="+trackName+"_regularity graphType=bar" + PileupConstants.NEWLINE);
		logger.info("Creating regular summary gff3 file: " + regularGFF);		
		regularWriter.close();
		gffWriter.close();
		
		for (Chromosome chromosome: finishedChromosomes) {
			logger.info("Processing: " + chromosome.toString());
			File metricFile = getTmpSummaryFile(chromosome);
			File regularFile = getTmpRegularityFile(chromosome);
			
			BufferedReader reader = new BufferedReader(new FileReader(metricFile));
			String line;
			TreeMap<Integer, ResultSummary> summaryMap = new TreeMap<Integer, ResultSummary>();			
			while ((line = reader.readLine()) != null) {			
				if (!line.startsWith("#")) {					
					ResultSummary rs = new ResultSummary(headerList, line);						
					summaryMap.put(rs.getStart(), rs);
				}
			}
			reader.close();						
			metricFile.delete();			
			writeToFinalSummaryFile(chromosome, summaryMap);	
			TreeMap<Integer, ResultSummary> slideLengthSummaryMap = writeChromosomeToWiggle(chromosome, summaryMap);
			summaryMap = null;
			writeChromosomeToSummaryGFF(chromosome, slideLengthSummaryMap, gffName);	
			slideLengthSummaryMap = null;
			TreeMap<Integer, MetricRecord> regularityMap = getRegularityMap(regularFile);
			writeChromosomeToRegularitySummaryGFF(chromosome, regularityMap, regularGFF);
			regularityMap = null;
		}
		
		//bedWriter.close();
		logger.info("Finished creating summary gff3 file: " + gffName);		
		
		createBigWigs();		
		writeCombinedSummaryGFF(gffName, regularGFF);		
		return gffName;
	}
	
	private void createBigWigs() throws Exception {
		//if pathToBigWig is not null, create bigWig file
		for (Entry<String, Metric> entry : metrics.entrySet()) {
			Metric metric = entry.getValue();
			
			//if is isn't the total base metric
			if (writeToWiggle(metric.type)) {				
				File wiggleFile = metric.getWiggleFile();			
				if (pathToBigWig != null) {
					createBigWigFile(metric.type, wiggleFile, metric.getBigWigFile(wiggleDir));					
				}
			}
		}		
	}

	private TreeMap<Integer, MetricRecord> getRegularityMap(File regularFile) throws NumberFormatException, IOException {
		BufferedReader regReader = new BufferedReader(new FileReader(regularFile));
		String regLine;
		TreeMap<Integer, MetricRecord> regularityMap = new TreeMap<Integer, MetricRecord>();
		
		while ((regLine = regReader.readLine()) != null) {
			
			String[] values = regLine.split("\t");
			String type = values[3];
			if (type.equals(PileupConstants.METRIC_INDEL)) {				
				//strand bias
				if (values[9].equals("true")) {
					//regular record
					IndelRecord indel = new IndelRecord(values[0], new Integer(values[1]), new Integer(values[2]), 'N', values[6]+ "-SBIAS", new Integer(values[4]), new Integer(values[7]), new Integer(values[8]), new Integer(values[5]));
					regularityMap.put(indel.getPosition(), indel);
				} else {
					//regular record
					MetricRecord indel = new IndelRecord(values[0], new Integer(values[1]), new Integer(values[2]), 'N', values[6], new Integer(values[4]), new Integer(values[5]));
					regularityMap.put(indel.getPosition(), indel);
				}
				
			} else if (type.equals(PileupConstants.METRIC_NONREFBASE)) {
				
					MetricRecord metric = new NonReferenceMetricRecord(values[0], new Integer(values[1]), new Integer(values[4]), new Integer(values[6]), new Integer(values[7]), new Integer(values[5]));
					regularityMap.put(metric.getPosition(), metric);
				
			} else {					
				ClipRecord metric = new ClipRecord(PileupConstants.METRIC_CLIP, values[0], new Integer(values[1]), new Long(values[4]), new Integer(values[6]));
				metric.setStartCount(new Integer(values[5]));
				regularityMap.put(metric.getPosition(), metric);					
			}
		}
		
		regReader.close();
		return regularityMap;
	}

	private String writeCombinedSummaryGFF(String gffName, String regularGFF) throws IOException {				
		Map<String, Map<ChrPosition, String>> mismap = getSummaryMap(gffName);
		Map<String, Map<ChrPosition, String>> regular = getSummaryMap(regularGFF);
		
		//merge the records into the mismap map
		for (Entry<String, Map<ChrPosition, String>> entry: regular.entrySet()) {			
			if (mismap.containsKey(entry.getKey())) {
				
				for (Entry<ChrPosition, String> chrEntry: entry.getValue().entrySet()) {
					ChrPosition chrPos = chrEntry.getKey();
					boolean overlap = false;
					for (Entry<ChrPosition, String> compareEntry : mismap.get(entry.getKey()).entrySet()) {
						ChrPosition comparePos = compareEntry.getKey();
						if (comparePos.getEndPosition() < chrPos.getPosition()) {
							continue;
						} else if (comparePos.getPosition() > chrPos.getEndPosition()) {
							break;
						} else {
							if (chrPositionsOverlap(chrPos, comparePos)) {								
								overlap = true;
							}
						}
					}
					if (!overlap) {
						mismap.get(entry.getKey()).put(chrPos, chrEntry.getValue());
					}
				}

			} else {
				mismap.put(entry.getKey(), entry.getValue());
			}			
		}
		String mergeGFFName = gffName.replace(".gff3", ".merged.gff3");
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(mergeGFFName)));
		writer.write("##gff-version 3" + PileupConstants.NEWLINE);
		writer.write("#track name="+trackName+"_merged_mismap graphType=bar" + PileupConstants.NEWLINE);
		for (Entry<String, Map<ChrPosition, String>> entry: mismap.entrySet()) {
			for (Entry<ChrPosition, String> currEntry: entry.getValue().entrySet()) {
				writer.write(currEntry.getValue()+ "\n");
			}
		}
		writer.close();
		return mergeGFFName;
	}

	private boolean chrPositionsOverlap(ChrPosition chrPos,	ChrPosition comparePos) {
		if ((chrPos.getPosition() >= comparePos.getPosition() && chrPos.getPosition() <= comparePos.getEndPosition()) ||
				(chrPos.getEndPosition() >= comparePos.getPosition() && chrPos.getEndPosition() <= comparePos.getEndPosition())) {
			return true;
		}
		return false;	
	}

	private Map<String, Map<ChrPosition, String>> getSummaryMap(String gffName) throws IOException {
		Map<String, Map<ChrPosition, String>> map = new HashMap<String, Map<ChrPosition, String>>();
		BufferedReader reader = new BufferedReader(new FileReader(new File(gffName)));
		
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.startsWith("#")) {
				String[] values = line.split("\t");				
				
				if (map.containsKey(values[0])) {
					map.get(values[0]).put(new ChrPosition(values[0], new Integer(values[3]), new Integer(values[4])), line);
				} else {
					TreeMap<ChrPosition, String> treemap = new TreeMap<ChrPosition, String>();
					treemap.put(new ChrPosition(values[0], new Integer(values[3]), new Integer(values[4])), line);
					map.put(values[0], treemap);
				}				
			}
		}
		reader.close();
		return map;
	}

	private void writeToFinalSummaryFile(Chromosome chromosome,
			TreeMap<Integer, ResultSummary> summaryMap) throws IOException {
		File file = getSummaryFile(chromosome);
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		
		for (Map.Entry<Integer, ResultSummary> entry: summaryMap.entrySet()) {
			writer.write(entry.getValue().toString(headerList));
		}
		
		writer.close();	
		
	}

	private TreeMap<Integer, ResultSummary> writeChromosomeToWiggle(Chromosome chromosome,
			TreeMap<Integer, ResultSummary> summaryMap) throws Exception {

		TreeMap<Integer, ResultSummary> slideLengthSummaryMap = new TreeMap<Integer, ResultSummary>();
		
		//write header for each metric
		for (Entry<String, Metric> entry : metrics.entrySet()) {
			Metric metric = entry.getValue();
			
			//if is isn't the total base metric
			if (writeToWiggle(metric.type)) {
				//write wiggle header
				metric.openWiggleWriter();
				metric.writeWiggleHeader(chromosome);
			}
		}
		
		int startPos = chromosome.getStartPos();
		int endPos = chromosome.getEndPos();
		
		int winNum = (endPos - startPos  + 1) / STEP_SIZE + 1;
		//wiggle window (ie 10 bases)
		for (int i = 0; i < winNum; i++) {			
			int start = i * STEP_SIZE + startPos;
		  	int end = (i + 1 ) * STEP_SIZE + startPos -1;	  	
		  	if (start > endPos) {
		  		break;
		  	}
		  	
		  	if (end < chromosome.getEndPos()) {	
		  		int mapStart = start - WINDOW_SIZE;
		  		if (mapStart < 1) {
		  			mapStart = 1;
		  		}
		  		int mapEnd = end+WINDOW_SIZE;
		  		//get summary windows that fall within the 10bp window
			  	NavigableMap<Integer, ResultSummary> subMap = summaryMap.subMap(mapStart, true, mapEnd, true);
			  	
			  	if (subMap != null) {
			  		for (Entry<String, Metric> entry : metrics.entrySet()) {
			  			
						Metric metric = entry.getValue();
						if (writeToWiggle(metric.type)) {
							int count = 0;
					  		double regularityScore = 0;
					  		int total = 0;
					  		int num = 0;
	
					  		//go through map and for summary windows that fall within the 10bp wiggle window, add scores,counts
					  		for (Entry<Integer, ResultSummary> subEntry: subMap.entrySet()) {
					  			ResultSummary rs = subEntry.getValue();
					  			
					  			if (rs != null && ((start >= rs.getStart() && start <= rs.getEnd()) || (end >= rs.getStart() && end <= rs.getEnd()))) {					  				
					  				num++;	
					  				//add
					  				if (rs.getRecord(metric.type) != null) {
					  					count += rs.getRecord(metric.type).getNumberPositions();
					  					total += rs.getRecord(metric.type).getTotalCounts();
					  					regularityScore += rs.getRecord(metric.type).getTotalRegularityScore();
					  				}					  				
					  			}
					  		}
					  		
					  		if (count > 0) {
					  			//get average scores for wiggle range and add to map to calculate summary gff regions
					  			double average = (double)count /(double) num;
					  			double avgTotal = (double)total /(double) num;
					  			double avgRegularityScore = regularityScore /(double) num;
					  			ResultRecord r = new ResultRecord(metric.getType(), (long) average, (long)avgTotal, avgRegularityScore);					  			
					  			if (average > 0) {					  				
						  			if (slideLengthSummaryMap.containsKey(start)) {
						  				slideLengthSummaryMap.get(start).addRecord(metric.getType(), r);
						  			} else {
						  				ResultSummary s = new ResultSummary(chromosome.getName(), start, end, 0, false);						  				
						  				s.addRecord(metric.getType(), r);
						  				slideLengthSummaryMap.put(start, s);
						  			}
						  			String output = start + " " + average + PileupConstants.NEWLINE;
						  			metric.writeToWiggle(output);
					  			}
					  		}						
						}
			  		}
			  	}
			  	
			  	//delete
			  	int deleteKey = (Math.max(1, mapStart-WINDOW_SIZE));
			  	Set<Integer> deletionSet = summaryMap.headMap(deleteKey).keySet();		  	
			  	
			  	for (Integer key: deletionSet) {
			  		summaryMap.remove(key);
			  	}			  	
		  	}		  	
		}
		//close wiggle writers
		for (Entry<String, Metric> entry : metrics.entrySet()) {
			Metric metric = entry.getValue();
			if (writeToWiggle(metric.type)) {
				//write wiggle header
				metric.closeWiggleWriter();
			}
		}
		return slideLengthSummaryMap;
	}

	private boolean writeToWiggle(String type) {
		if (!type.equals(PileupConstants.METRIC_STRAND_BIAS) &&  !type.equals(PileupConstants.METRIC_SNP)) {
			return true;	
		}
		return false;
	}

	private void createBigWigFile(String metric, File wiggleFile, File bigWigFile) throws Exception {
		String cmd = pathToBigWig + " " + wiggleFile + " " + chromSizes + " " + bigWigFile;
		
		BlockingExecutor executor = new BlockingExecutor(cmd);
		logger.info("Creating big wig file: " + bigWigFile + " for metric " + metric);
		if (executor.isFailure()) {
			logger.info("Creation of big wig file: " + bigWigFile + " failed");
			String[] lines = executor.getErrorStreamConsumer().getLines();
			for (String l : lines) {
				logger.info("Error is: " + l);
			}
		} 
		if (executor.isSuccessful()) {
			logger.info("Creation of big wig file: " + bigWigFile + " was successful");
			wiggleFile.delete();			
		}		
	}
	
	private void writeChromosomeToSummaryGFF(Chromosome chromosome, Map<Integer, ResultSummary> summaryMap, String gffName) throws IOException {
		    BufferedWriter gffWriter = new BufferedWriter(new FileWriter(new File(gffName), true));	
			ResultSummary summaryRecord = null;
			for (Entry<Integer, ResultSummary> entry: summaryMap.entrySet()) {			
				ResultSummary rs = entry.getValue();	
				//System.out.println(rs.toCheckString(headerList));
				//is the 10bp window a potential mismapper or low quality mapper
				if (rs.isErrorRegion()) {
					if (summaryRecord == null) {
						//instantiate
						summaryRecord = new ResultSummary(rs, recordCount.longValue()); 	
					} else if (rs.getStart() <= summaryRecord.getEnd()+1) {
						//does current window overlap with previous window that is a mismapper
						summaryRecord.addSummaryRecord(rs);					
					} else {														
						if (summaryRecord.isWritableErrorRegion()) {
							incrementCountMap(summaryRecord);						
							gffWriter.write(summaryRecord.toGFFString() + PileupConstants.NEWLINE);
						}
						summaryRecord = new ResultSummary(rs, recordCount.longValue()); 	
					}			 
				}	
				recordCount.incrementAndGet();
			}
			if (summaryRecord != null && summaryRecord.isWritableErrorRegion()) {
				incrementCountMap(summaryRecord);						
				gffWriter.write(summaryRecord.toGFFString() + PileupConstants.NEWLINE);
			}
			gffWriter.close();
	}
	
	private void writeChromosomeToRegularitySummaryGFF(Chromosome chromosome, TreeMap<Integer, MetricRecord> regularityMap, String regularGFF) throws IOException {		
		 BufferedWriter gffWriter = new BufferedWriter(new FileWriter(new File(regularGFF), true));	
		for (Entry<Integer, MetricRecord> entry: regularityMap.entrySet()) {
			gffWriter.write(entry.getValue().toGFFString());
		}
		gffWriter.close();
	}


	private void incrementCountMap(ResultSummary summaryRecord) {
		
		//counts for each category alone
		int bases = summaryRecord.getEnd() - summaryRecord.getStart() + 1;
		countsMap.get(summaryRecord.getName()).addAndGet(bases);
		
		if (summaryRecord.getName().contains(PileupConstants.LOW_MAPPING_QUAL)) {
			countsMap.get(PileupConstants.MIXED_AND_LOWMAPING).addAndGet(bases);
		}
		if (summaryRecord.getName().contains(PileupConstants.IRREGULAR)) {
			countsMap.get(PileupConstants.MIXED_AND_IRREGULAR).addAndGet(bases);
		}
		if (summaryRecord.getName().contains(PileupConstants.REGULAR)) {
			countsMap.get(PileupConstants.MIXED_AND_REGULAR).addAndGet(bases);
		}
		if (summaryRecord.getName().contains(PileupConstants.HCOV)) {
			countsMap.get(PileupConstants.MIXED_AND_HCOV).addAndGet(bases);
		}
		//extra counts for mixed category
		if (summaryRecord.getName().equals(PileupConstants.MIXED)) {
			
			String cats = summaryRecord.getMixedCategories();
			
			if (cats.contains(PileupConstants.LOW_MAPPING_QUAL)) {
				countsMap.get(PileupConstants.MIXED_AND_LOWMAPING).addAndGet(bases);
			}
			if (cats.contains(PileupConstants.IRREGULAR)) {
				countsMap.get(PileupConstants.MIXED_AND_IRREGULAR).addAndGet(bases);
			}
			if (cats.contains(PileupConstants.REGULAR)) {
				countsMap.get(PileupConstants.MIXED_AND_REGULAR).addAndGet(bases);
			}
			if (summaryRecord.getName().contains(PileupConstants.HCOV)) {
				countsMap.get(PileupConstants.MIXED_AND_HCOV).addAndGet(bases);
			}
		}
	}

	public void setPathToBigWig(String pathToBigWig) {
		if (pathToBigWig.endsWith("wigToBigWig")) {
			this.pathToBigWig= pathToBigWig;
		} else {
			this.pathToBigWig= pathToBigWig + PileupConstants.FILE_SEPARATOR + "wigToBigWig";			
		}				
	}
	
	public void setChromSizes(String chromSizes) {
		this.chromSizes = chromSizes;
	}

	public void checkMetrics() {
		if (metrics.containsKey(PileupConstants.METRIC_SNP) && metrics.containsKey(PileupConstants.METRIC_STRAND_BIAS)) {
			SnpMetric s = (SnpMetric)metrics.get(PileupConstants.METRIC_SNP);
			StrandBiasMetric sb = (StrandBiasMetric)metrics.get(PileupConstants.METRIC_STRAND_BIAS);
			s.setMinStrandBiasDifference(sb.getMinPercentDifference());
			metrics.put(PileupConstants.METRIC_SNP, s);
		}		
	}

	public void setMinBasesPerPatient(Integer minBasesPerPatient) {
		this.minBasesPerPatient = minBasesPerPatient;		
	}

	public TreeMap<String, Metric> getMetrics() {
		return this.metrics;
	}
}
