package au.edu.qimr.qannotate.nanno;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.qio.record.RecordWriter;
import org.qcmg.qio.record.StringFileReader;
import org.qcmg.qio.vcf.VcfFileReader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import au.edu.qimr.qannotate.Messages;
import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.nanno.AnnotationInputs.AnnotationInput;

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
		AnnotationInputs ais = getInputs(jsonInputs);
		
		/*
		 * create a comparator that will be used to sort the annotation fields for output
		 */
		CUSTOM_COMPARATOR = createComparatorFromList(Arrays.stream(ais.getOutputFieldOrder().split(",")).collect(Collectors.toList()));
		
		/*
		 * check headers that have been supplied in the json inputs file
		 */
		int headersOK = checkHeaders(ais);
		if (headersOK == 1) {
			System.exit(headersOK);
		}
		
		List<AnnotationSource> annotationSources = new ArrayList<>();
		populateAnnotationSources(ais, annotationSources);
		
		String emptyHeaders = ais.getAdditionalEmptyFields();
		String [] emptyHeadersArray =  StringUtils.isNullOrEmpty(emptyHeaders) ? new String[]{} : emptyHeaders.split(",");
		
		String header = "chr\tposition\tref\talt\tGATK_AD\t" + Arrays.stream(ais.getOutputFieldOrder().split(",")).collect(Collectors.joining("\t"));
		if (emptyHeadersArray.length > 0) {
			header += "\t" +  Arrays.stream(emptyHeadersArray).collect(Collectors.joining("\t"));
		}
		
		boolean includeSearchTerm = ais.isIncludeSearchTerm();
		header += (includeSearchTerm ? "\tsearchTerm" : "");
		
		String emptyHeaderValues =  getEmptyHeaderValues(emptyHeadersArray.length);
		
		CountDownLatch consumerLatch = new CountDownLatch(1);
		Queue<ChrPositionAnnotations> queue = new ConcurrentLinkedQueue<>();
		
		ExecutorService executor = Executors.newFixedThreadPool(3);
		executor.execute(new Consumer(queue, outputFile, consumerLatch, header, includeSearchTerm, emptyHeaderValues));
		
		
		try (
			VcfFileReader reader = new VcfFileReader(inputFile);) {
			int vcfCount = 0;
			for (VcfRecord vcf : reader) {
				vcfCount++;
				
				ChrPosition thisVcfsCP = vcf.getChrPositionRefAlt();
				
				String alt = ((ChrPositionRefAlt) thisVcfsCP).getAlt();
				String gatkAD = VcfUtils.getFormatField(vcf.getFormatFields(), "AD", 0);
				
				if (alt.contains(",")) {
					logger.info("alt has comma: " + thisVcfsCP.toString());
					/*
					 * split record, create new ChrPositions for each
					 */
					String [] altArray = alt.split(",");
					Map<String, String> altToADMap = getADForSplitVCfRecords(altArray, gatkAD);
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
					
					List<String> annotations = new ArrayList<>(getAnnotationsForPosition(thisVcfsCP, annotationSources, executor));
					queue.add(new ChrPositionAnnotations(thisVcfsCP, annotations, gatkAD));
					
				}
			}
			
			logger.info("# of vcf records: " + vcfCount);
		}
		
		return exitStatus;
	}
	
	public static AnnotationInputs getInputs(String file) throws IOException {
		//read json file data to String
		byte[] jsonData = Files.readAllBytes(Paths.get(file));
		//create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		//convert json string to object
		AnnotationInputs ai = objectMapper.readValue(jsonData, AnnotationInputs.class);
		
		return ai;
	}
	
	private static List<String> getAnnotationsForPosition(ChrPosition cp, List<AnnotationSource> annotationSources, Executor executor) {
		return annotationSources.stream()
			.map(source -> CompletableFuture.supplyAsync(() -> 
				source.getAnnotation(cp), executor))
			.map(CompletableFuture::join).collect(Collectors.toList());
		
	}
	
	public static Comparator<String[]> createComparatorFromList(final List<String> sortedList) {
		Comparator<String[]> c = new Comparator<String[]>() {
			@Override
			public int compare(String[] o1, String[] o2) {
				final int index1 = sortedList.indexOf(o1[0]);
				if (index1 == -1) return 1;
				final int index2 = sortedList.indexOf(o2[0]);
				if (index2 == -1) return -1;
				return index1 - index2;
			}
		};
		return c;
	}
	
	/**
	 * 
	 * @param altArray
	 * @param gatkAD
	 * @return
	 */
	public static Map<String, String> getADForSplitVCfRecords(String [] altArray, String gatkAD) {
		
		Map<String, String> altToADMap = new HashMap<>(4);
		String [] gatkADArray = gatkAD.split(",");
		/*
		 * should have 1 more in the gatkADArray than the altArray
		 */
		if (altArray.length == gatkADArray.length - 1) {
			for (int i = 0 ; i < altArray.length ; i++) {
				altToADMap.put(altArray[i], gatkADArray[0] + "," + gatkADArray[i + 1]);
			}
		}
		
		return altToADMap;
	}
	
	public static String getEmptyHeaderValues(int count) {
		if (count <= 0) {
			return "";
		}
		return org.apache.commons.lang3.StringUtils.repeat("\t", count);
	}
	
	/**
	 * @param ais
	 * @param annotationSources
	 * @throws IOException
	 */
	public static void populateAnnotationSources(AnnotationInputs ais, List<AnnotationSource> annotationSources) throws IOException {
		for (AnnotationInput ai : ais.getInputs()) {
			String fileName = ai.getFile();
			String fieldNames = ai.getFields();
			
			logger.info("fileName: " + fileName + ", positions: " + ai.getChrIndex() + ", " + ai.getPositionIndex() + ", " + ai.getRefIndex() + ", " + ai.getAltIndex() + ", fieldNames: " + fieldNames);
			
			if (ai.isSnpEffVcf()) {
				annotationSources.add(new AnnotationSourceSnpEffVCF(new StringFileReader(new File(fileName), 1024 * 1024), ai.getChrIndex(), ai.getPositionIndex(), ai.getRefIndex(), ai.getAltIndex(), fieldNames));
			} else if (fileName.contains("vcf")) {
				annotationSources.add(new AnnotationSourceVCF(new StringFileReader(new File(fileName), 1024 * 1024), ai.getChrIndex(), ai.getPositionIndex(), ai.getRefIndex(), ai.getAltIndex(), fieldNames));
			} else {
				annotationSources.add(new AnnotationSourceTSV(new StringFileReader(new File(fileName), 1024 * 1024), ai.getChrIndex(), ai.getPositionIndex(), ai.getRefIndex(), ai.getAltIndex(), fieldNames));
			}
		}
	}
	
	public static int checkHeaders(AnnotationInputs ais) {
		List<String> annotationFields = ais.getInputs().stream().map(ai -> ai.getFields()).collect(Collectors.toList());
		boolean headersValid = isOrderedHeaderListValid(ais.getOutputFieldOrder(), annotationFields.toArray(new String[]{}));
		
		if ( ! headersValid) {
			System.err.println("headers are not valid! OrderedHeader: " + ais.getOutputFieldOrder() + "\nAnnotation fields: " + (ais.getInputs().stream().map(ai -> ai.getFields())).collect(Collectors.joining(",")));
			return 1;
		}
		return 0;
	}
	
	/**
	 * checks to see if the sortedHEader contains all the fields from the various annotation sources
	 * Not sure what to do if we have 2 fields with the same name (presumably from different sources)
	 * 
	 * 
	 * @param ai
	 * @return
	 */
	public static boolean isOrderedHeaderListValid(String sortedHeader, String ... fieldsFromAnnotationSources) {
		if (StringUtils.isNullOrEmpty(sortedHeader)) {
			/*
			 * empty or null sorted header - not valid
			 */
			logger.error("sortedHeader is null or empty");
			return false;
		}
		if (null == fieldsFromAnnotationSources || fieldsFromAnnotationSources.length == 0) {
			/*
			 * empty or null annotation fields - not valid
			 */
			logger.error("fieldsFromAnnotationSources is null or length is 0");
			return false;
		}
		
		Set<String> sortedHeaderSet = Arrays.stream(sortedHeader.split(",")).collect(Collectors.toSet());
		Set<String> fieldsFromAnnotationSourcesSet = Arrays.stream(Arrays.stream(fieldsFromAnnotationSources).collect(Collectors.joining(",")).split(",")).collect(Collectors.toSet());
		
		for (String s : sortedHeaderSet) {
			if ( ! fieldsFromAnnotationSourcesSet.contains(s)) {
				logger.error(s +  " in header but not found in any data source!");
			}
		}
		for (String s : fieldsFromAnnotationSourcesSet) {
			if ( ! sortedHeaderSet.contains(s)) {
				logger.error(s +  " in data source but not found in header!");
			}
		}
		
		return sortedHeaderSet.containsAll(fieldsFromAnnotationSourcesSet) && fieldsFromAnnotationSourcesSet.containsAll(sortedHeaderSet);
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
			List<String> singleAnnotations = convertAnnotations(annotations);
			if (debug) {
				System.out.println("singleAnnotations.size(): " + singleAnnotations.size());
				singleAnnotations.stream().forEach(System.out::println);
			}
			
			String searchTerm = "";
			if (includeSearchTerm) {
				Optional<String> hgvsC = getAnnotationFromList(singleAnnotations, "hgvs.c");
				Optional<String> hgvsP = getAnnotationFromList(singleAnnotations, "hgvs.p");
				searchTerm = getSearchTerm(hgvsC, hgvsP);
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
	
	/**
	 * Create a PubMed search term using the hgvsC and hgvsP values
	 * @param hgvsC
	 * @param hgvsP
	 * @return
	 */
	public static String getSearchTerm(Optional<String> hgvsC, Optional<String> hgvsP) {
		String st = "";
		
		/*
		 * check the optionals - if they are both not present, no need to proceed
		 */
		if (( ! hgvsC.isPresent() && ! hgvsP.isPresent())) {
			return st;
		}
		
		if ( hgvsC.isPresent() && hgvsC.get().length() > 0) {
			
			/*
			 * need to check that the string contains the dot ('.') and the gt sign ('>')
			 */
			int dotIndex = hgvsC.get().indexOf('.');
			int gtIndex = hgvsC.get().indexOf('>');
			if (dotIndex > -1 && gtIndex > -1) {
			
				/*
				 * split value into required parts
				 */
				String firstPart = hgvsC.get().substring(dotIndex + 1, gtIndex);
				String secondPart = hgvsC.get().substring(gtIndex + 1);
				
				st += SEARCH_TERM_VARIETIES.stream().map(s -> "\"" + firstPart + s + secondPart + "\"").collect(Collectors.joining("|"));
			}
		}
		
		if ( hgvsP.isPresent() && hgvsP.get().length() > 0) {
			if (st.length() > 0) {
				/*
				 * we must have hgvs.c data - so add bar
				 */
				st += "|";
			}
			st += "\"" + hgvsP.get().substring(hgvsP.get().indexOf('.') + 1) + "\"";
		}
		
		if (st.length() > 0) {
			return "\"GENE\"+(" + st + ")";
		}
		return st;
	}
	
	/**
	 * get the requiredAnnotation value from the list of annotations
	 * return null if not present
	 * 
	 * @param listOfAnnotations
	 * @param requiredAnnotation
	 * @return
	 */
	public static Optional<String> getAnnotationFromList(List<String> listOfAnnotations, String requiredAnnotation) {
		
		if (null != listOfAnnotations && ! StringUtils.isNullOrEmpty(requiredAnnotation)) {
			for (String anno : listOfAnnotations) {
				if (anno.startsWith(requiredAnnotation)) {
					return Optional.of(anno.substring(requiredAnnotation.length() + 1));		// don't forget the equals sign
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Splits the strings in the supplied list by tab, and flattens them to a single list
	 */
	public static List<String> convertAnnotations(List<String> manyAnnotations) {
		if (null != manyAnnotations) {
			return manyAnnotations.stream().flatMap(s -> java.util.Arrays.stream(TabTokenizer.tokenize(s))).collect(Collectors.toList());
		}
		return Collections.emptyList();
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
