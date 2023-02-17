package au.edu.qimr.qannotate.nanno;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.qio.record.RecordWriter;
import org.qcmg.qio.vcf.VcfFileReader;

import au.edu.qimr.qannotate.Messages;
import au.edu.qimr.qannotate.Options;

public class Annotate {
	
	static final List<String> SEARCH_TERM_VARIETIES = Arrays.asList(">", "->", "-->", "/");
	
	static Comparator<String[]> CUSTOM_COMPARATOR;
	static QLogger logger;
	
	private int exitStatus;
	
	private String logFile;
	private String inputFile;
	private String outputFile;
	private String jsonInputs;
	
	private QExec exec;
	
	public int engage() throws Exception {
		
		/*
		 * parse the json file into an AnnotationINputs object
		 */
		AnnotationInputs ais = AnnotateUtils.getInputs(jsonInputs);
		logger.info("Number of annotation source threads to use: " + ais.getAnnotationSourceThreadCount());
		/*
		 * create a comparator that will be used to sort the annotation fields for output
		 */
		CUSTOM_COMPARATOR = AnnotateUtils.createComparatorFromList(Arrays.stream(ais.getOutputFieldOrder().split(",")).collect(Collectors.toList()));
		logger.info("Custom comparator created");
		/*
		 * check headers that have been supplied in the json inputs file
		 */
		int headersOK = AnnotateUtils.checkHeaders(ais);
		if (headersOK == 1) {
			logger.error("Headers have been checked - not OK!!!");
			System.exit(headersOK);
		}
		logger.info("Headers have been checked - OK");
		
		List<AnnotationSource> annotationSources = new ArrayList<>();
		AnnotateUtils.populateAnnotationSources(ais, annotationSources);
		logger.info("annotationSources have been loaded (aize: " + annotationSources.size() + ")");
		annotationSources.stream().forEach(as -> logger.info(as.toString()));
		
		String emptyHeaders = ais.getAdditionalEmptyFields();
		String [] emptyHeadersArray =  StringUtils.isNullOrEmpty(emptyHeaders) ? new String[]{} : emptyHeaders.split(",");
		
		String header = "chr\tposition\tref\talt\tGATK_AD\t" + Arrays.stream(ais.getOutputFieldOrder().split(",")).collect(Collectors.joining("\t"));
		if (emptyHeadersArray.length > 0) {
			header += "\t" +  Arrays.stream(emptyHeadersArray).collect(Collectors.joining("\t"));
		}
		
		boolean includeSearchTerm = ais.isIncludeSearchTerm();
		header += (includeSearchTerm ? "\tsearchTerm" : "");
		
		String emptyHeaderValues =  AnnotateUtils.getEmptyHeaderValues(emptyHeadersArray.length);
		
		CountDownLatch consumerLatch = new CountDownLatch(1);
		Queue<ChrPositionAnnotations> queue = new ConcurrentLinkedQueue<>();
		
		
		ExecutorService executor = Executors.newFixedThreadPool(ais.getAnnotationSourceThreadCount() + 1);	// need an extra thread for the consumer
		executor.execute(new Consumer(queue, outputFile, consumerLatch, header, includeSearchTerm, emptyHeaderValues));
		logger.info("ExecutorService has been setup");
		
		
		try (
			VcfFileReader reader = new VcfFileReader(inputFile);) {
			logger.info("VcfFileReader has been setup");
			int vcfCount = 0;
			for (VcfRecord vcf : reader) {
				vcfCount++;
				
				ChrPosition thisVcfsCP = vcf.getChrPositionRefAlt();
				logger.info("thisVcfsCP: " + thisVcfsCP.toIGVString());
				
				String alt = ((ChrPositionRefAlt) thisVcfsCP).getAlt();
				String gatkAD = VcfUtils.getFormatField(vcf.getFormatFields(), "AD", 0);
				
				if (alt.contains(",")) {
					logger.info("alt has comma: " + thisVcfsCP.toString());
					/*
					 * split record, create new ChrPositions for each
					 */
					String [] altArray = alt.split(",");
					Map<String, String> altToADMap = AnnotateUtils.getADForSplitVcfRecords(altArray, gatkAD);
					List<VcfRecord> splitVcfs = new ArrayList<>();
					for (String thisAlt : altArray) {
						if (thisAlt.equals("*")) {
							/*
							 * ignore
							 */
						} else {
							VcfRecord newVcf = VcfUtils.cloneWithNewAlt(vcf, thisAlt);
							splitVcfs.add(newVcf);
						}
					}
					if (splitVcfs.size() > 1) {
						/*
						 * sort
						 */
						splitVcfs.sort(null);
					}
					for (VcfRecord splitVcf : splitVcfs) {
						List<String> annotations = new ArrayList<>(getAnnotationsForPosition(splitVcf.getChrPositionRefAlt(), annotationSources, executor));
						queue.add(new ChrPositionAnnotations(splitVcf.getChrPositionRefAlt(), annotations, altToADMap.get(splitVcf.getAlt())));
					}
					
				} else {
					
					logger.debug("about to get annotations for: " + thisVcfsCP.toIGVString());
					List<String> annotations = getAnnotationsForPosition(thisVcfsCP, annotationSources, executor);
					logger.debug("got annotations for: " + thisVcfsCP.toIGVString() + " - adding to queue");
					queue.add(new ChrPositionAnnotations(thisVcfsCP, annotations, gatkAD));
					
				}
			}
			
			logger.info("# of vcf records: " + vcfCount);
		}
		
		executor.shutdown();
		logger.info("ExecutorService has been shutdown");
		return exitStatus;
	}
	
	private static List<String> getAnnotationsForPosition(ChrPosition cp, List<AnnotationSource> annotationSources, Executor executor) throws InterruptedException, ExecutionException, TimeoutException {
		
		return annotationSources.stream()
				.map(source -> CompletableFuture.supplyAsync(() -> 
				source.getAnnotation(cp), executor))
				.map(CompletableFuture::join).collect(Collectors.toList());
		
		
		
		
		
		
		
//		logger.info("in getAnnotationsForPosition: " + cp.toIGVString() + ", annotationSources: " + annotationSources.size() + ", executor: " + executor.toString());
//		
//		List<CompletableFuture<String>> cfs = new ArrayList<>();
//		List<String> annos = new ArrayList<>();
//		for (AnnotationSource as : annotationSources) {
//			CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> as.getAnnotation(cp), executor);
//			cfs.add(cf);
//		}
//		logger.info("CompletableFutures have been setup");
//		for (CompletableFuture<String> cf : cfs) {
//			logger.info("about to call join");
//			logger.info("cf.isDone(): " + cf.isDone());
//			logger.info("cf.isCancelled(): " + cf.isCancelled());
//			logger.info("cf.isCompletedExceptionally(): " + cf.isCompletedExceptionally());
//			logger.info("cf.getNumberOfDependents(): " + cf.getNumberOfDependents());
//			String s = cf.get(10, TimeUnit.SECONDS);
//			annos.add(s);
//			logger.info("about to call join - DONE");
//		}
//		
//		
//		logger.info("CompletableFutures have finished!");
//		
//		
//		logger.info("in getAnnotationsForPosition: " + cp.toIGVString() + ", annotationSources: " + annotationSources.size() + ", executor: " + executor.toString());
//		
//		
//		return annos;
//		
	}
	
	public static class ChrPositionAnnotations {
		
		public String getGatkAD() {
			return gatkAD;
		}
		
		public ChrPosition getCp() {
			return cp;
		}
		
		public List<String> getAnnotations() {
			return annotations;
		}
		
		ChrPosition cp;
		List<String> annotations;
		String gatkAD;
		
		public ChrPositionAnnotations(ChrPosition cp, List<String> annotations, String gatkAD) {
			super();
			this.cp = cp;
			this.annotations = annotations;
			this.gatkAD = gatkAD;
		}
	}
	
	public static class Consumer implements Runnable {
		
		private final Queue<ChrPositionAnnotations> queue;
		private final String outputFile;
		private final boolean includeSearchTerm;
		private final CountDownLatch latch;
		private final RecordWriter<String> writer;
		private final String additionalEmptyHeaders;

		public Consumer(Queue<ChrPositionAnnotations> queue, String outputFile, CountDownLatch latch, String header, boolean includeSearchTerm, String additionalEmptyHeaders) throws IOException {	
			this.queue = queue;
			this.outputFile = outputFile;
			this.latch = latch;
			this.includeSearchTerm = includeSearchTerm;
			this.additionalEmptyHeaders = additionalEmptyHeaders;
			writer = new RecordWriter<String>(new File(outputFile));
			writer.addHeader(header);
		}
		
		@Override
		public void run() {
			System.out.println("Consumer thread is a go!");
			try {
				int count = 0;
				
				while (true) {
					
					final ChrPositionAnnotations rec = queue.poll();
					if (null != rec) {
						
						processRecord(rec);
						
					} else {
						if (latch.getCount() == 0) {
							break;
						}
						// sleep and try again
						try {
							Thread.sleep(50);
						} catch (final InterruptedException e) {
							logger.error("InterruptedException caught in Consumer sleep: " +  e.getLocalizedMessage());
							throw e;
						}
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
				logger.error("Exception caught in Consumer class: " + e.getCause().getMessage());
			} finally {
				logger.info("Consumer: shutting down");
				/*
				 * close writer
				 */
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		public void processRecord(final ChrPositionAnnotations recAndAnnotations) throws IOException {
			ChrPosition cp = recAndAnnotations.getCp();
			boolean debug = false;
			if (cp.getStartPosition() == 953279) {
				debug = true;
			}
			
			
			List<String> annotations = recAndAnnotations.getAnnotations();
			if (debug) {
				System.out.println("annotations.size(): " + annotations.size());
				annotations.stream().forEach(System.out::println);
			}
			
			/*
			 * collect entries in annotations lists into map
			 */
			List<String> singleAnnotations = AnnotateUtils.convertAnnotations(annotations);
			if (debug) {
				System.out.println("singleAnnotations.size(): " + singleAnnotations.size());
				singleAnnotations.stream().forEach(System.out::println);
			}
			
			String searchTerm = "";
			if (includeSearchTerm) {
				Optional<String> hgvsC = AnnotateUtils.getAnnotationFromList(singleAnnotations, "hgvs.c");
				Optional<String> hgvsP = AnnotateUtils.getAnnotationFromList(singleAnnotations, "hgvs.p");
				searchTerm = AnnotateUtils.getSearchTerm(hgvsC, hgvsP);
			}
			/*
			 * sort and write out to file
			 */
			String annotationString = singleAnnotations.stream().map(s -> s.split("=", 2)).sorted(CUSTOM_COMPARATOR).map(a -> a[1]).collect(Collectors.joining("\t"));
			
			if (debug) {
				System.out.println("annotationString: " + annotationString);
			}
			
			writer.add(((ChrPositionRefAlt)cp).toTabSeperatedString() + "\t" + recAndAnnotations.getGatkAD() + "\t" + annotationString + additionalEmptyHeaders + (includeSearchTerm ? "\t" + searchTerm : ""));
		}
	}
	
	public static void main(String[] args) throws Exception {
		final Annotate sp = new Annotate();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (final Exception e) {
			exitStatus = 1;
			if (null != logger) {
				logger.error("Exception caught whilst running Annotate:", e);
			} else {
				System.err.println("Exception caught whilst running Annotate");
			}
			e.printStackTrace();
		}
		
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception {
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.getMessage("NANNO_USAGE"));
			System.exit(1);
		}
		final Options options = new Options(args);

		System.out.println("options.getInputFileName: " + options.getInputFileName());
		System.out.println("options.getOutputFileName: " + options.getOutputFileName());
		System.out.println("options.getOutputFileName: " + options.getOutputFileName());
		System.out.println("options.getConfigFileName: " + options.getConfigFileName());
		if ( null == options.getInputFileName()) {
			System.err.println(Messages.getMessage("NANNO_USAGE"));
		} else if ( null == options.getOutputFileName()) {
			System.err.println(Messages.getMessage("NANNO_USAGE"));
		} else if ( null == options.getLogFileName()) {
			System.err.println(Messages.getMessage("NANNO_USAGE"));
		} else if ( null == options.getConfigFileName()) {
			System.err.println(Messages.getMessage("NANNO_USAGE"));
		} else {
			// configure logging
			logFile = options.getLogFileName();
			logger = QLoggerFactory.getLogger(Annotate.class, logFile, options.getLogLevel());
			exec = logger.logInitialExecutionStats("Annotate", Annotate.class.getPackage().getImplementationVersion(), args);
			outputFile = options.getOutputFileName();
			inputFile = options.getInputFileName();
			jsonInputs = options.getConfigFileName();
			
			return engage();
		}
		
		return returnStatus;
	}

}
