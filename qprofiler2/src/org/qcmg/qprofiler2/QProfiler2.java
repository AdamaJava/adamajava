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

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.messages.Messages;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.bam.BamSummarizer2;
import org.qcmg.qprofiler2.bam.BamSummarizerMT2;
import org.qcmg.qprofiler2.cohort.CohortSummarizer;
import org.qcmg.qprofiler2.fastq.FastqSummarizer;
import org.qcmg.qprofiler2.fastq.FastqSummarizerMT;
import org.qcmg.qprofiler2.vcf.VcfSummarizer;
import org.w3c.dom.Element;

public class QProfiler2 { 
		
	private static final String msgResource = "org.qcmg.qprofiler2.messages";	
	private static final int NO_OF_PROCESORS = Runtime.getRuntime().availableProcessors();
	private static final String USER_DIR = System.getProperty("user.dir");
	private static final String FILE_SEPERATOR = System.getProperty("file.separator");	
	
	private QLogger logger;	
	private String[] cmdLineFiles;
	private String[] cmdLineIndexFiles;
	private String[] cmdLineFormats;  // vcf mode	
	private ExecutorService exec;
	private String version;
	
	private String outputFile = USER_DIR + FILE_SEPERATOR + "qprofiler.xml";
	private int exitStatus;
	private int noOfConsumerThreads;
	private int noOfProducerThreads;
	private int maxRecords;
	private String logFile;
	private String validation;
	private boolean  isFullBamHeader;

	
	/*
	 * This is the "main" method for this class. It will be invoked by the
	 * instanceMain() method of the superclass. All logic for running the
	 * application needs to be inside this method. All command line argument
	 * processing will have been completed by the superclass by the time this
	 * method is invoked so we can assume that the class fields are populated
	 * and ready for us to use.
	 */
	protected int engage() throws Exception { 
		Element root = XmlElementUtils.createRootElement( "qProfiler", null);
		
		//  Create new Summary object ready to hold our processing
		QProfilerSummary2 sol = new QProfilerSummary2();
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
		
		final Map<ProfileType, List<Pair<String, String>>> sortedFiles = new HashMap<>();
		
		for (int i = 0 ; i < cmdLineFiles.length ; i++) { 
			String f = cmdLineFiles[i];
			 
			//  see if we have a corresponding index
			String index = null;
			if (null != cmdLineIndexFiles && cmdLineIndexFiles.length > i) { 
				index = cmdLineIndexFiles[i];
			}
			ProfileType type = ProfileType.getType2(f);
			sortedFiles.computeIfAbsent(type, v -> new ArrayList<>()).add(Pair.of(f, index));
		}		
				
		if ( ! sortedFiles.isEmpty()) { 						
			// do xmlSummary here
			if (sortedFiles.containsKey(ProfileType.XML )) { 
				List<String> xmls = new ArrayList<>();
				sortedFiles.remove( ProfileType.XML ).forEach(p -> xmls.add(  p.getLeft()));
				processXmlFiles( xmls, outputFile);
			}
			// after removed xml files				
			if ( ! sortedFiles.isEmpty()) {		
				processFiles( sortedFiles, root );	
			} else {
				// no xml output required if no inputs except xml
				return exitStatus; 
			}
		}
		
		//  if we have a failure, exit now rather than creating a skeleton output file
		if (exitStatus == 1) {
			return exitStatus;
		}
		logger.info("generating output xml file: " + outputFile);
		
		
		// xml reorganise
		sol.setFinishTime(DateUtils.getCurrentDateAsString());		
		root.setAttribute( "startTime",  sol.getStartTime() );
		root.setAttribute( "finishTime", sol.getFinishTime() );		
		root.setAttribute( "user", System.getProperty("user.name") );
		root.setAttribute( "operatingSystem", System.getProperty("os.name") );
		root.setAttribute( "version", version );
		// add md5 to identify of schema file
		root.setAttribute( "validationSchema", Messages.getMessage(msgResource, "XSD_FILE") );
		root.setAttribute( "md5OfSchema", Messages.getMessage(msgResource, "XSD_FILE_md5") );
		XmlElementUtils.asXmlText(root, outputFile);		
		 			
		return exitStatus;
	}
	
	private void processXmlFiles(List<String> files, String output)throws Exception { 			 				
		final CohortSummarizer summarizer = new CohortSummarizer();
		for (final String file :files) { 
			 summarizer.summarize(file) ;
		}
		
		summarizer.outputSumamry(  new File( output + ".tsv" ));
	}
	
	/**
	 * Process each input file listed on the command line
	 * 
	 * @param gffFiles names of GFF files to be processed
	 * @return SummaryReport objects for each file processed
	 */	
	private void processFiles(Map<ProfileType, List<Pair<String, String>>> files, Element root) throws RuntimeException { 		
		for (Map.Entry<ProfileType, List<Pair<String, String>>> entry : files.entrySet()) { 			
			for (final Pair<String, String> pair : entry.getValue()) { 
				logger.info("processing file " + pair.getLeft());
				final Summarizer summarizer = getSummarizer(entry.getKey());
				if (null != summarizer) { 
					Runnable task = new Runnable() { 
						@Override
						public void run() { 
							logger.info("running " + summarizer.getClass().getSimpleName());
							SummaryReport sr = null;
							try { 
								sr = summarizer.summarize(pair.getLeft(), pair.getRight());
								sr.toXml(root);
							} catch (Exception e) { 
								logger.error( "Exception caught whilst running summarizer for file: " + pair.getLeft(), e );
								exitStatus = 1;
								// throw new RuntimeException(e);
							}
							logger.debug("done with " + summarizer.getClass());
						}
					};
					exec.execute( task );
				}
			}
		}
		
		//  don't allow any new tasks to be executed
		exec.shutdown();
		try { 
			exec.awaitTermination(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);
		} catch (InterruptedException e) { 
			//  restore interrupted status
			Thread.currentThread().interrupt();
		}
	}
	
	private Summarizer getSummarizer(ProfileType key) { 
		Summarizer summarizer = null;
		if (null != key) { 
			switch (key) { 
			case VCF: summarizer = new VcfSummarizer( cmdLineFormats );
				break;
			case FASTQ:
				if (noOfConsumerThreads > 0) { 
					summarizer = new FastqSummarizerMT(noOfConsumerThreads);
				} else { 
					// summarizer = new FastqSummarizer(cmdLineInclude);
					summarizer = new FastqSummarizer();					
				}
				break;
			case BAM:
				if (noOfConsumerThreads > 0) { 
					summarizer = new BamSummarizerMT2(noOfProducerThreads, noOfConsumerThreads, maxRecords,  validation,  isFullBamHeader);
				} else { 
					summarizer = new BamSummarizer2( maxRecords, validation, isFullBamHeader);
				}
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

	public static void main(String args[]) throws Exception { 
		//  loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(QProfiler2.class);
		
		QProfiler2 qp = new QProfiler2();
		int exitStatus = qp.setup(args);
		if (null != qp.logger) { 
			qp.logger.logFinalExecutionStats(exitStatus);
		} else { 
			System.err.println("Exit status: " + exitStatus);
		}
		
		System.exit(exitStatus);
	}
	
	public int setup(String args[]) throws Exception { 
		int returnStatus = 1;
		Options2 options = new Options2(args);
		String usage = Messages.getMessage(msgResource, "USAGE");
		
		if (options.hasHelpOption()) { 
			System.err.println( usage );
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) { 
			System.err.println(options.getVersion());
			returnStatus = 0;
		} else if (options.getFileNames().length < 1) { 
			System.err.println(usage );
		} else if ( ! options.hasLogOption()) { 
			System.err.println(usage );
		} else { 
			//  configure logging
			logFile = options.getLog();
			version = QProfiler2.class.getPackage().getImplementationVersion();
			logger = QLoggerFactory.getLogger(QProfiler2.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("qprofiler", version, args);
			
			//  get list of file names
			cmdLineFiles = options.getFileNames();
			if (cmdLineFiles.length < 1) { 
				throw new Exception( Messages.getMessage(msgResource, "INSUFFICIENT_ARGUMENTS"));
			} else { 
				//  loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineFiles.length ; i ++ ) { 
					if ( ! FileUtils.canFileBeRead(cmdLineFiles[i])) { 
						throw new Exception( Messages.getMessage(msgResource, "INPUT_FILE_ERROR" , cmdLineFiles[i]));
					}
				}
			}
			
			//  set outputfile - if supplied, check that it can be written to
			if (null != options.getOutputFileName()) { 
				String optionsOutputFile = options.getOutputFileName();
				if (FileUtils.canFileBeWrittenTo(optionsOutputFile)) { 
					outputFile = optionsOutputFile;
				} else { 
					throw new Exception( Messages.getMessage(msgResource, "OUTPUT_FILE_WRITE_ERROR"));
				}
			}
			
			cmdLineIndexFiles = options.getIndexFileNames();
			validation = options.getValidation();
			cmdLineFormats = options.getFormats(); //vcf mode 			
			
			//  get no of threads
			noOfConsumerThreads = options.getNoOfConsumerThreads();
			noOfProducerThreads = Math.max(1, options.getNoOfProducerThreads());
			if (noOfConsumerThreads > 0) { 
				logger.tool("Running in multi-threaded mode (BAM files only). No of available processors: " + NO_OF_PROCESORS 
						+ ", no of requested consumer threads: " + noOfConsumerThreads + ", producer threads: " + noOfProducerThreads 
						+ ", no of md5 checksum thread: 1");
			} else { 
				logger.tool("Running in single-threaded mode");
			}
			
			//  get max Record count
			maxRecords = options.getMaxRecords();
			if (maxRecords > 0) { 
				logger.tool("Running in maxRecord mode (BAM files only). Will stop profiling after " + maxRecords + " records");
			}
			
			this.isFullBamHeader = options.hasFullBamHeaderOption();
			
			//  setup the ExecutorService thread pool
			//  the size of the pool is the smaller of the no of files, and the NO_OF_PROCESSORS variable
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
