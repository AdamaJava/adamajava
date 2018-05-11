/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.messages.QMessage;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.ProfileTypeUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.bam.BamSummarizer;
import org.qcmg.qprofiler2.bam.BamSummarizerMT;
import org.qcmg.qprofiler2.cohort.CohortSummarizer;
import org.qcmg.qprofiler2.fa.FaSummarizerMT;
import org.qcmg.qprofiler2.fasta.FastaSummarizer;
import org.qcmg.qprofiler2.fastq.FastqSummarizer;
import org.qcmg.qprofiler2.fastq.FastqSummarizerMT;
import org.qcmg.qprofiler2.gff.GffSummarizer;
import org.qcmg.qprofiler2.ma.MaSummarizer;
import org.qcmg.qprofiler2.qual.QualSummarizer;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.vcf.VcfSummarizer;
import org.qcmg.qvisualise2.QVisualise;
import org.w3c.dom.Element;


public class QProfiler {
	
	private static QLogger logger;
	
	private final static int NO_OF_PROCESORS = Runtime.getRuntime().availableProcessors();
	private final static String USER_DIR = System.getProperty("user.dir");
	private final static String FILE_SEPERATOR = System.getProperty("file.separator");
	
	private static String[] cmdLineFiles;
	private static String[] cmdLineInclude;
	private static String[] cmdLineTags;
	private static String[] cmdLineFormats; //vcf mode
	private static String[] cmdLineTagsInt;
	private static String[] cmdLineTagsChar;

	private static ExecutorService exec;
	private static String version;
	
	private String outputFile = USER_DIR + FILE_SEPERATOR + "qprofiler.xml";
	private int exitStatus;
	private int noOfConsumerThreads;
	private int noOfProducerThreads;
	private int maxRecords;
	private String logFile;
	private String validation;
	private boolean noHtml; 
	

	/*
	 * This is the "main" method for this class. It will be invoked by the
	 * instanceMain() method of the superclass. All logic for running the
	 * application needs to be inside this method. All command line argument
	 * processing will have been completed by the superclass by the time this
	 * method is invoked so we can assume that the class fields are populated
	 * and ready for us to use.
	 */
	protected int engage() throws Exception {
		Element root = QprofilerXmlUtils.createRootElement(null, "qProfiler", null);
		
		// Create new Summary object ready to hold our processing
		QProfilerSummary sol = new QProfilerSummary();
		sol.setStartTime(DateUtils.getCurrentDateAsString());
				
		/*
		 * First thing we should do is load XML report file if one is specified
		 * and if it already exists. The behavior we want for XML is if a file
		 * exists, we rename the old file, load it, add more report nodes to it
		 * if more CSFASTA, GFF, SAM file etc are specified and then we write
		 * the file out again under the same name. This will also let us do
		 * things like load an existing XML and output it in text format if no
		 * additional files are specified for processing
		 */

		final Map<ProfileType, List<File>> sortedFiles = new HashMap<ProfileType, List<File>>();		
		
		for (int i = 0 ; i < cmdLineFiles.length ; i++) {
			ProfileType type = ProfileTypeUtils.getType(cmdLineFiles[i]);			
			
			if ( ! sortedFiles.containsKey(type))  
				sortedFiles.put(type, new ArrayList<File>());			 
			sortedFiles.get(type).add(new File (cmdLineFiles[i]) );
		}
		
		if ( ! sortedFiles.isEmpty()) {			
			/*
			 * If neither XML nor text output files is requested then we should
			 * probably drop out without doing any work. This behavior should be OK
			 * until we get to the stage where we do validation as well as summary
			 * reports.
			 */
			try {
				// Don't forget to try to load the XML file before doing any crazy
				// renaming stuff!
				QprofilerXmlUtils.backupFileByRenaming(outputFile);
			} catch (Exception e) {
				logger.error("Exception caught whilst running StaticMethods.backupFileByRenaming", e);
				throw e;
			}
			
			//do xmlSummary here
			List<File> xmls = sortedFiles.remove(ProfileType.XML);
			if(xmls != null )
				processXmlFiles( xmls, outputFile);
			
			if ( ! sortedFiles.isEmpty())			
				processFiles(sortedFiles, root);	
			else
				//no xml output required if no inputs except xml
				return exitStatus; 
		}
		
		// if we have a failure, exit now rather than creating a skeleton output file
		if (exitStatus == 1) return exitStatus;
		logger.info("generating output xml file: " + outputFile);
		
		//xml reorganise
		root.setAttribute("startTime", sol.getStartTime());
		root.setAttribute("finishTime", sol.getFinishTime());
		root.setAttribute("user", System.getProperty("user.name"));
		root.setAttribute("operatingSystem", System.getProperty("os.name"));
		
		root.setAttribute("version", version);
		sol.setFinishTime(DateUtils.getCurrentDateAsString());		
		QprofilerXmlUtils.asXmlText(root, outputFile);
		
		//output tsv file if inputs are xml			
		if ( ! noHtml) {
			String qVisLogFile = logFile.replace("log", "html.log");
			String [] args = {"-i",outputFile, "-log", qVisLogFile};
			logger.info("about to run qVisualise on qProfiler output");
			try {
				int exitstatus = new QVisualise().setup(args);
				if (exitstatus == 0) logger.info("qVisualise output generated: " + outputFile + ".html");
				else logger.warn("Problem occurred in qVisualise - please refer to log file [" + qVisLogFile + "] for (hopefully) more details"); 
			} catch (Exception e) {
				// just log this and continue
				logger.warn("qVisualise failed for qprofiler output: " + outputFile);
			}
		}
		return exitStatus;
	}
	
	private void processXmlFiles(List<File> files, String output)throws Exception{
			 				
			final CohortSummarizer summarizer = new CohortSummarizer();
			//List<XmlSummaryReport> reports = new ArrayList<>();
			for (final File file :files) 
				 summarizer.summarize(file) ;
				 
			summarizer.outputSumamry(  new File( output+".tsv" ));
	}
	
	/**
	 * Process each input file listed on the command line
	 * 
	 * @param gffFiles names of GFF files to be processed
	 * @return SummaryReport objects for each file processed
	 */
	//private void processFiles(Map<ProfileType, List<File>> files, Element root) throws RuntimeException{
	private void processFiles(Map<ProfileType, List<File>> files, Element root) throws Exception{		
		for (Map.Entry<ProfileType, List<File>> entry : files.entrySet()) {					
			//for all summary except xmlSummary
			for (final File file : entry.getValue()) {
				logger.info("processing file " + file);
				final Summarizer summarizer = getSummarizer(entry.getKey());
				if (null != summarizer) {
					Runnable task = new Runnable() {
						@Override
						public void run() {
							logger.info("running " + summarizer.getClass().getSimpleName());						 
							try {								
								SummaryReport sr = summarizer.summarize(file);							 
								sr.toXml(root);
							} catch (Exception e) {
								logger.error( "Exception caught whilst running summarizer for file: " + file.getName(), e );
								// set the exit status to failure if any of the tasks throw an exception
								exitStatus = 1;
								//throw new RuntimeException(e);
							}							
							logger.debug("done with " + summarizer.getClass());
						}
					};
					exec.execute(task);
				}
			}
		}
		
		// don't allow any new tasks to be executed
		exec.shutdown();
		try {
			exec.awaitTermination(20, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// restore interrupted status
			Thread.currentThread().interrupt();
		}
	}
	
	private Summarizer getSummarizer(ProfileType key) {
		Summarizer summarizer = null;
		if (null != key) {
			switch (key) {
			case VCF: summarizer = new VcfSummarizer( cmdLineFormats );
				break;
			case gff:
				summarizer = new GffSummarizer();
				break;
			case FASTA:
				summarizer = new FastaSummarizer(cmdLineInclude);
				break;
			case FASTQ:
				if (noOfConsumerThreads > 0) {
					summarizer = new FastqSummarizerMT(noOfConsumerThreads);
				} else {
					//summarizer = new FastqSummarizer(cmdLineInclude);
					summarizer = new FastqSummarizer();					
				}
				break;
			case bam:
				if (noOfConsumerThreads > 0) {
					summarizer = new BamSummarizerMT(noOfProducerThreads, noOfConsumerThreads, cmdLineInclude, maxRecords, cmdLineTags, cmdLineTagsInt, cmdLineTagsChar, validation);
				} else {
					summarizer = new BamSummarizer(cmdLineInclude, maxRecords, cmdLineTags, cmdLineTagsInt, cmdLineTagsChar, validation);
				}
				break;
			case QUAL:
				summarizer = new QualSummarizer(cmdLineInclude);
				break;
			case MA:
				summarizer = new MaSummarizer();
				break;
			case FA:
				summarizer = new FaSummarizerMT(noOfConsumerThreads);
				break;
			case XML:
				summarizer = new CohortSummarizer();
				break;
				
			default:
				logger.warn("Summarizer for type " + key + " does not yet exist");
			}
		}
		return summarizer;
	}

	public static void main(String args[]) throws Exception{
		// loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(QProfiler.class);
		
		QProfiler qp = new QProfiler();
		int exitStatus = qp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		} else {
			System.err.println("Exit status: " + exitStatus);
		}
		
		System.exit(exitStatus);
	}
	
	public int setup(String args[]) throws Exception{
		int returnStatus = 1;
		Options options = new Options(args);
		QMessage messages = options.getMessage();
		if (options.hasHelpOption()) {
			System.err.println(messages.getUsage()  );
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getFileNames().length < 1) {
			System.err.println(messages.getUsage() );
		} else if ( ! options.hasLogOption()) {
			System.err.println(messages.getUsage() );
		} else {
			// configure logging
			logFile = options.getLog();
			version = QProfiler.class.getPackage().getImplementationVersion();
			logger = QLoggerFactory.getLogger(QProfiler.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("qprofiler", version, args);
			
			// get list of file names
			cmdLineFiles = options.getFileNames();
			if (cmdLineFiles.length < 1) {
				throw new Exception( messages.getMessage("INSUFFICIENT_ARGUMENTS"));
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineFiles[i])) {
						throw new Exception( messages.getMessage("INPUT_FILE_ERROR" , cmdLineFiles[i]));
					}
				}
			}
			
			// set outputfile - if supplied, check that it can be written to
			if (null != options.getOutputFileName()) {
				String optionsOutputFile = options.getOutputFileName();
				if (FileUtils.canFileBeWrittenTo(optionsOutputFile)) {
					outputFile = optionsOutputFile;
				} else {
					throw new Exception( messages.getMessage("OUTPUT_FILE_WRITE_ERROR"));
				}
			}
			
			cmdLineInclude = options.getBamIncludes();
			cmdLineTags = options.getTags();
			cmdLineFormats = options.getFormats(); // vcf mode 
			cmdLineTagsInt = options.getTagsInt();
			cmdLineTagsChar = options.getTagsChar();
			validation = options.getValidation();
			
			noHtml = options.hasNoHtmlOption();
			
			// get no of threads
			noOfConsumerThreads = options.getNoOfConsumerThreads();
			noOfProducerThreads = Math.max(1, options.getNoOfProducerThreads());
			if (noOfConsumerThreads > 0) {
				logger.tool("Running in multi-threaded mode (BAM files only). No of available processors: " + NO_OF_PROCESORS + 
						", no of requested consumer threads: " + noOfConsumerThreads + ", producer threads: " + noOfProducerThreads);
			} else {
				logger.tool("Running in single-threaded mode");
			}
			
			// get max Record count
			maxRecords = options.getMaxRecords();
			if (maxRecords > 0) {
				logger.tool("Running in maxRecord mode (BAM files only). Will stop profiling after " + maxRecords + " records");
			}
			
			// setup the ExecutorService thread pool
			// the size of the pool is the smaller of the no of files, and the NO_OF_PROCESSORS variable
			if (Math.min(cmdLineFiles.length, NO_OF_PROCESORS) < 1) {
				throw new Exception("Not enough available processors to perform this task");	
			} else {
				exec = Executors.newFixedThreadPool(Math.min(cmdLineFiles.length, NO_OF_PROCESORS));
				return engage();
			}
		}
		return returnStatus;
	}
}
