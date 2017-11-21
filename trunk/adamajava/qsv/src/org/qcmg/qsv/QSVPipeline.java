/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.qsv.annotate.AnnotateFilterMT;
import org.qcmg.qsv.annotate.PairingStats;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.discordantpair.DiscordantPairCluster;
import org.qcmg.qsv.discordantpair.FindDiscordantPairClustersMT;
import org.qcmg.qsv.discordantpair.FindMatePairsMT;
import org.qcmg.qsv.discordantpair.MatePairsReader;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.report.DCCReport;
import org.qcmg.qsv.report.SVCountReport;
import org.qcmg.qsv.report.SummaryReport;
import org.qcmg.qsv.softclip.FindClipClustersMT;
import org.qcmg.qsv.util.CustomThreadPoolExecutor;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

/**
 * 
 * Class to run the pipeline for qsv
 *
 */
public class QSVPipeline {


	private final QLogger logger = QLoggerFactory.getLogger(QSVPipeline.class);
	private static final String FILE_SEPERATOR = System.getProperty("file.separator");
	private QSVParameters normal;
	private QSVParameters tumor;
	private final Options options;
	private final Date analysisDate;
	private final SVCountReport clusterCounts;
	private final String matePairDir;
	private final AtomicInteger exitStatus = new AtomicInteger();
	private final String softclipDir;
	private final SummaryReport summaryReport;
	private final Map<PairGroup, Map<String, List<DiscordantPairCluster>>> tumorRecords;
	private final Map<PairGroup, Map<String, List<DiscordantPairCluster>>> normalRecords;
	private int somaticCounts = 0;
	private int  germlineCounts = 0;
	private int normalGermlineCounts =0;
	private final String analysisId;
	private final String resultsDir;
	private long clipCount =0;

	public QSVPipeline(Options options, String resultsDir, Date analysisDate, String analysisId, QExec exec) throws Exception {

		this.options = options;        
		this.analysisDate = analysisDate;
		this.analysisId = analysisId;
		this.tumorRecords = new TreeMap<>();
		this.normalRecords = new TreeMap<>();
		this.resultsDir = resultsDir;

		//copy ini file to output directory

		Path path = Files.copy(Paths.get(options.getIniFile()), Paths.get(resultsDir + FILE_SEPERATOR + new File(options.getIniFile()).getName()), StandardCopyOption.REPLACE_EXISTING);


		// dir to write the mate pairs to
		matePairDir = options.getTempDirName() + FILE_SEPERATOR +  "matepair" + FILE_SEPERATOR;
		createOutputDirectory(matePairDir);

		//matepairs directories
		for (PairClassification zp : PairClassification.values()) {
			createOutputDirectory(matePairDir + zp.getPairingClassification() + FILE_SEPERATOR);
		}

		//soft clips directory
		this.softclipDir = options.getTempDirName() + FILE_SEPERATOR +  "softclip";
		createOutputDirectory(softclipDir);

		this.summaryReport = new SummaryReport(new File(resultsDir + FILE_SEPERATOR + options.getSampleName() + ".qsv.summary.txt"), analysisDate, analysisId, options.isQCMG());

		if (options.isTwoFileMode()) {
			setQSVParameters(resultsDir); 
		} else {
			setQSVParametersSingleMode(resultsDir); 
		}        

		if (options.isQCMG()) {
			writeDCCHeader(options, analysisDate, analysisId, exec);
		}

		File countFile = new File(resultsDir + FILE_SEPERATOR + options.getSampleName()+ "_sv_counts.txt");
		this.clusterCounts = new SVCountReport(countFile, options.getSampleName());
		if (options.getRanges() != null) {
			for (String s: options.getRanges()) {
				logger.info("Finding SVs for: "+ s);
			}        	
		}

	}

	private void writeDCCHeader(Options options, Date analysisDate, String analysisId, QExec exec) throws IOException {
		new DCCReport(new File(resultsDir + FILE_SEPERATOR + options.getSampleName() + ".somatic.dcc"), analysisDate, analysisId, tumor, normal, options, exec);
		if (options.isTwoFileMode()) {
			new DCCReport(new File(resultsDir + FILE_SEPERATOR + options.getSampleName() + ".germline.dcc"), analysisDate, analysisId, tumor, normal, options, exec);	
			new DCCReport(new File(resultsDir + FILE_SEPERATOR + options.getSampleName() + ".normal-germline.dcc"), analysisDate, analysisId, tumor, normal, options, exec);
		}	
	}

	/*
	 * Set qsv parameters with 2 input files - carry out in two separate threads in case isize needs to be calculated
	 */
	private void setQSVParameters(String resultsDir) throws Exception {
		tumor = new QSVParameters(options, true, resultsDir, matePairDir, analysisDate, options.getSampleName());
		normal = new QSVParameters(options, false, resultsDir, matePairDir, analysisDate, options.getSampleName());
	}

	/*
	 * Set qsv parameters with a single input file 
	 */
	private void setQSVParametersSingleMode(String resultsDir) throws QSVException {
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		CountDownLatch countDownLatch = new CountDownLatch(1);

		Callable<QSVParameters> tumourWorker = new CreateParametersCallable(countDownLatch,options, true, resultsDir, matePairDir, analysisDate, options.getSampleName());
		Future<QSVParameters> tumourFuture = executorService
				.submit(tumourWorker);

		executorService.shutdown();

		try {
			countDownLatch.await();
			tumor = tumourFuture.get();
		} catch (InterruptedException e) {
			logger.debug("Thread interrupted while running qbamfilter");
			exitStatus.set(1);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			logger.debug("Thread interrupted while running qbamfilter");
			exitStatus.set(1);
			Thread.currentThread().interrupt();
		}

		if (exitStatus.intValue() == 1 || ((CreateParametersCallable) tumourWorker).getExitStatus() == 1) {
			throw new QSVException ("QSV_PARAMETER_EXCEPTION");
		}		
	}

	/**
	 * Create output directory
	 * @param dirName name of the output directory
	 * @throws QSVException if the directory could not be created
	 */
	public void createOutputDirectory(String dirName) throws QSVException {
		File dir = new File(dirName);
		if (dir.exists()) {
			QSVUtil.removeDirectory(dir);
		}
		if (!dir.mkdir()) {
			throw new QSVException("DIR_CREATE_ERROR", dirName);   
		}
	}

	/**
	 * Runs the pipeline to identify structural variations. Pipeline begins by annotating
	 * and filtering the bams. If discordant pair mode is chosen discordant pairs are clustered. 
	 * If clip mode is chose, soft clip clusters are found. 
	 * @throws Exception if problems occurs when running the pipeline
	 */
	public void runPipeline() throws Exception  {

		//add qsv parameters to log file
		logger.info("Starting QSV pipeline");
		logQSVParameters();

		logger.debug("Test sample input BAM file size: " + tumor.getInputBamFile().length());

		//annotation and filtering of bams
		annotateAndFilterBams();           

		//log info about annotation and filtering. 
		if (tumor.getFilteredBamFile() != null) {
			logger.debug("Test sample filtered and sorted discordant pair file size: " + tumor.getFilteredBamFile().length());
		}  
		if (options.isTwoFileMode()) {
			if (normal.getFilteredBamFile() != null) {
				logger.debug("Control filtered and sorted BAM discordant pair file size: " + normal.getFilteredBamFile().length());
			}            
		}        

		//Time for annotation and filtering to take place
		Date filterDate = new Date();       
		logger.info(QSVUtil.writeTime("Pre-processing time: ", analysisDate, filterDate));

		//Run pair analysis 
		if (options.runPairAnalysis()) {            	
			extractPairs();
			findPairClusters();            	
		}  
		//Time for pair analysis
		Date pairsDate = new Date();
		if (options.runPairAnalysis()) {
			logger.info(QSVUtil.writeTime("Discordant pair clustering time: ", filterDate, pairsDate));
		}
		
		//load reference file into mem
		QSVUtil.setupReferenceMap(new File(tumor.getReference()));
		logger.info("setup reference map (name : length)");
		for (Entry<String,byte[]> entry : QSVUtil.getReferenceMap().entrySet()) {
			logger.info(entry.getKey() + " : " + entry.getValue().length);
		}

		QSVClusterWriter writer = new QSVClusterWriter(tumor, normal, options.isQCMG(), analysisId, options.singleSided(), options.isTwoFileMode(), options.getMinInsertSize(), options.getPlatform(), options.getGffFiles());                 	        

		//write normal germline records
		if (options.isQCMG() && options.isTwoFileMode()) {
			writer.writeQSVClusterRecords(normalRecords, false);
			this.normalGermlineCounts = writer.getNormalGermlineCount().intValue();
		}

		//if clip mode is chosen, run clip clustering. Somatic and germline Records will be written in the analysis 
		if (options.runClipAnalysis() || options.isSplitRead()) {
			findSoftClips(); 
			logger.info(QSVUtil.writeTime("Soft clip clustering time: ", pairsDate, new Date()));
		} else {
			//otherwise write tumour records
			writer.writeQSVClusterRecords(tumorRecords, true);
		}

		tumorRecords.clear();	            
		normalRecords.clear();

		//write report of SV counts        
		if (options.isQCMG()) {
			clusterCounts.writeReport();
		}

		//Find clusters time
		logger.info(QSVUtil.writeTime("SV Analysis Time: ", pairsDate, new Date())); 

		//Remove tmp directories
		if (!QSVUtil.removeDirectory(new File(matePairDir))) {
			logger.warn("Could not delete files from temporary directory");
		}       
		if (!QSVUtil.removeDirectory(new File(softclipDir))) {
			logger.warn("Could not delete files from temporary directory");
		}   

		String runTime = QSVUtil.writeTime("Total run time: ", analysisDate, new Date());

		if (options.runPairAnalysis()) {
			somaticCounts = clusterCounts.getSomaticCounts();
			germlineCounts = clusterCounts.getGermlineCounts();
			normalGermlineCounts = clusterCounts.getNormalGermlineCounts();        	
		}        

		//write summary report
		summaryReport.summarise(runTime, tumor, normal, options, somaticCounts, germlineCounts, normalGermlineCounts);

		//Run time
		logger.info(runTime); 
	}

	/*
	 * Write relevant qsv parameters to log file 
	 */
	private void logQSVParameters() throws IOException {
		List<String> params = new ArrayList<String>(); 
		params.add("SV analysis type: " + options.getAnalysisMode());
		params.add("Test sample BAM file: " + tumor.getInputBamFile());        
		params.add("Test sample name: " + tumor.getFindType());
		if (options.isQCMG()) {
			params.add("Test sample id: " + tumor.getSampleId());
		}
		params.add("Lower insert size - test sample file: " + tumor.getLowerInsertSize());
		params.add("Upper insert size - test sample file: " + tumor.getUpperInsertSize());

		params.add("Log file name: " + options.getLog());
		params.add("Output directory: " + options.getOutputDirName());
		if (null != options.getOverrideOutput()) {
			params.add("OverrideOutput directory: " + options.getOverrideOutput());
		}
		params.add("Output file name: " + options.getSampleName());
		params.add("Include split reads: " + options.isSplitRead());
		params.add("Minimum SV insert size: " + options.getMinInsertSize()); 
		if (options.isTwoFileMode()) {
			params.add("Control sample BAM file: " + normal.getInputBamFile());
			params.add("Control sample name: " + normal.getFindType());
			if (options.isQCMG()) {
				params.add("Control sample id: " + normal.getSampleId());
			}
			params.add("Lower insert size - control sample file: " + normal.getLowerInsertSize());
			params.add("Upper insert size - control sample file: " + normal.getUpperInsertSize());

		}
		if (options.runPairPreprocess() || options.runPairAnalysis()) {
			params.add("Pairing type: " + options.getPairingType());
			params.add("Mapper: " + options.getMapper());
			params.add("Discordant pair cluster size: " + options.getClusterSize());
			params.add("Discordant pair germline filter exclusion size: " + options.getFilterSize());
			if (options.getPairQuery() != null) {
				params.add("Discordant pair query: " + options.getPairQuery());
			}
		}
		if (options.runClipPreprocess() || options.runClipAnalysis()) {
			params.add("Soft clip reads size: " + options.getClipSize());        	
			params.add("Soft clip consensus length: " + options.getConsensusLength());
			if (options.getClipQuery() != null) {
				params.add("Soft clip query: " + options.getClipQuery());
			}
		}

		for(String param : params) {
			logger.info(param);
		} 
	}

	/**
	 * Method to run preprocessing: annotation and filtering of input bam files
	 * @throws Exception
	 */
	public void annotateAndFilterBams() throws Exception {

		logger.info("Starting BAM filter threads");

		ExecutorService executorService = new CustomThreadPoolExecutor(getThreadNo(), exitStatus, logger);
		CountDownLatch countDownLatch = new CountDownLatch(getThreadNo());

		if (options.isTwoFileMode()) {        
			AnnotateFilterMT normalWorker = new AnnotateFilterMT(Thread.currentThread(), countDownLatch, normal, exitStatus, softclipDir, options);        
			executorService.execute(normalWorker);
		}

		AnnotateFilterMT tumorWorker = new AnnotateFilterMT(Thread.currentThread(), countDownLatch, tumor, exitStatus, softclipDir, options);         
		executorService.execute(tumorWorker);

		executorService.shutdown();

		try {
			countDownLatch.await();

		} catch (InterruptedException e) {
			logger.debug("Thread interrupted while running qbamfilter");
			exitStatus.set(1);
			Thread.currentThread().interrupt();
		}

		clipCount = tumorWorker.getClipCount().longValue();

		//write annotation report of counts of each discordant pair type
		if (options.isQCMG()) {        
			writeAnnotationReport();
		}

		if (exitStatus.intValue() == 1) {
			throw new QSVException("FILTER_ERROR");
		}

		logger.info("Finished processing BAM filter threads");
	}

	/*
	 * Write annotation report - counts of each type of discordant pair
	 */
	private void writeAnnotationReport() throws Exception {
		JAXBContext context = JAXBContext.newInstance(PairingStats.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); //pretty print XML
		if (normal != null) {
			normal.getAnnotator().writeReport(m);
		}
		tumor.getAnnotator().writeReport(m);
	}

	/*
	 * Extract read mates and their types for normal and tumor bams
	 */
	private void extractPairs() throws QSVException {
		logger.info("Starting threads to find mate pairs");

		// start the threads: one for normal and one for tumor
		ExecutorService executorService = new CustomThreadPoolExecutor(getThreadNo(), exitStatus, logger);
		CountDownLatch countDownLatch = new CountDownLatch(getThreadNo());

		if (options.isTwoFileMode()) {
			Runnable normalWorker = new FindMatePairsMT(Thread.currentThread(), countDownLatch, normal, exitStatus, options.getSampleName(), matePairDir, options.isQCMG());
			executorService.execute(normalWorker);
		}

		Runnable tumorWorker = new FindMatePairsMT(Thread.currentThread(), countDownLatch, tumor, exitStatus, options.getSampleName(), matePairDir, options.isQCMG());       
		executorService.execute(tumorWorker);
		executorService.shutdown();

		// wait for threads to finish
		try {
			countDownLatch.await();

		} catch (InterruptedException e) {
			logger.debug("Thread interrupted while running find mate pairs thread for the file ");
			exitStatus.set(1);
			Thread.currentThread().interrupt();
		}

		if (exitStatus.intValue() == 1) {
			throw new QSVException("EXTRACTPAIR_ERROR");
		}

		logger.debug("Finished threads to find mate pairs");
	}

	/*
	 * Find clusters for each discordant pair type
	 */
	private void findPairClusters() throws QSVException {
		try {
			// find clusters
			for (PairGroup zp : PairGroup.values()) {
				if (options.isQCMG()) {
					logger.info("Starting to find clusters for ZP: " + zp.getPairGroup());
				}

				MatePairsReader normalReader = null;
				//create normal file reader
				if (options.isTwoFileMode()) {
					normalReader = new MatePairsReader(zp, matePairDir, options.getSampleName(), QSVConstants.CONTROL_SAMPLE);
				}
				// create tumor file reader
				MatePairsReader tumorReader = new MatePairsReader(zp, matePairDir, options.getSampleName(), QSVConstants.DISEASE_SAMPLE);

				findCluster(zp, tumorReader, normalReader);

				if (exitStatus.intValue() == 1) {
					throw new QSVException("CLUSTER_ERROR");
				}

				logger.info("Finished finding clusters for ZP: " + zp.getPairGroup());
			}                

		} catch (Exception e) {
			exitStatus.set(1);
			e.printStackTrace();
			logger.error("Exception when finding clusters", e);            
		}

		if (exitStatus.intValue() == 1) {
			throw new QSVException("CLUSTER_ERROR");
		}
	}

	/*
	 *Find soft clip clusters 
	 */
	private void findSoftClips() throws Exception {

		BLAT blat = new BLAT(options.getBlatServer(), options.getBlatPort(), options.getBlatPath());

		FindClipClustersMT worker = new FindClipClustersMT(tumor, normal, softclipDir, blat, tumorRecords, options, analysisId, clipCount);
		worker.execute();

		this.somaticCounts = worker.getQSVRecordWriter().getSomaticCount().intValue();
		this.germlineCounts = worker.getQSVRecordWriter().getGermlineCount().intValue();

		// log some blat stats
		logger.info("BLAT server was accessed " + blat.getExecuteCount() + " times");
		
		if (worker.getExitStatus().intValue() >= 1) {
			throw new QSVException("CLIP_CLUSTER_EXCEPTION");
		}          
	}

	private int getThreadNo() {
		if (options.isSingleFileMode()) {
			return 1;
		}
		return 2;
	}

	private void findCluster(PairGroup zp,
			MatePairsReader tumorReader,
			MatePairsReader normalReader) throws Exception {
		
		ExecutorService executorService = Executors.newFixedThreadPool(getThreadNo());
		CountDownLatch countDownLatch = new CountDownLatch(getThreadNo());
		Future<Map<String, List<DiscordantPairCluster>>> normalFuture = null;
		
		if (options.isTwoFileMode()) {
			Callable<Map<String, List<DiscordantPairCluster>>> normalWorker = new FindDiscordantPairClustersMT(zp, countDownLatch, normalReader, tumorReader, normal, tumor, clusterCounts, options.getPairQuery(), options.isQCMG());
			normalFuture = executorService.submit(normalWorker);
		}
		Callable<Map<String, List<DiscordantPairCluster>>> tumorWorker = new FindDiscordantPairClustersMT(zp, countDownLatch, tumorReader, normalReader, tumor, normal, clusterCounts, options.getPairQuery(), options.isQCMG());       
		Future<Map<String, List<DiscordantPairCluster>>> tumorFuture = executorService.submit(tumorWorker);
		executorService.shutdown();

		try {
			countDownLatch.await();

			//add clusters   
			if (options.isTwoFileMode()) {
				Map<String,List<DiscordantPairCluster>> nClusters = normalFuture.get();
				normalRecords.put(zp, nClusters);
			}

			Map<String,List<DiscordantPairCluster>> tClusters = tumorFuture.get();                    
			tumorRecords.put(zp, tClusters);       

		} catch (InterruptedException e) {
			logger.debug("Thread interrupted when finding cluster" + zp);
			exitStatus.intValue();
			Thread.currentThread().interrupt();
		}
	}
}
