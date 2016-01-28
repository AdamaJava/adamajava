package au.edu.qimr.vcftools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.vcftools.util.MergeUtils;

public class MergeSameSamples {
	
	private static QLogger logger;
	private QExec exec;
	private String[] vcfFiles;
	private String outputFileName;
	private String version;
	private String logFile;
	private int exitStatus;
	
	private final Map<VcfRecord, VcfRecord> mergedRecords = new HashMap<>(1024 * 1024 * 8);
	// assuming there are only 2 inputs for now...
//	private  List<VcfRecord> input1 = new ArrayList<>();
//	private  List<VcfRecord> input2 = new ArrayList<>();
	
	private VcfHeader [] headers;
	
	protected int engage() throws IOException {
		
		logger.info("about to load vcf files");
		loadVcfs();
		
//		processRecords();
		
		writeOutput();
		
		return exitStatus;
	}
	
	
	private void writeOutput() throws IOException {
		List<VcfRecord> recs = new ArrayList<>(mergedRecords.values());
		Collections.sort(recs);
		
		logger.info("writing output");
		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileName))) {
			for (VcfRecord rec : recs) {
				writer.add(rec);
			}
		}
		logger.info("writing output- DONE");
//		logger.info("About to create map based on merged records");
//		Map<VcfRecord, VcfRecord> outputRecords = mergedRecords.stream()
//				.collect(Collectors.toMap(vcf -> vcf, vcf -> vcf));
//		logger.info("About to create map based on merged records - DONE");
//		/*
//		 * add in records from input2
//		 */
//		logger.info("adding records from input2 to map based on merged records");
//		outputRecords.putAll(input2.stream()
//				.filter(vcf -> ! outputRecords.containsKey(vcf))
//				.collect(Collectors.toMap(vcf -> vcf, vcf -> vcf)));
//		logger.info("adding records from input2 to map based on merged records - DONE");
//		
//		input2.clear();
//		input2 =  null;
//		
//		
//		
//		
//		
//		logger.info("writing output");
//		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileName))) {
//			outputRecords.keySet().stream()
//			.sorted((vcf1, vcf2) -> vcf1.compareTo(vcf2))
//			.forEach(vcf -> {
//				try {
//					writer.add(vcf);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			});
//		}
//		logger.info("writing output- DONE");
		
	}
	
	
//	private void processRecords() {
//		/*
//		 * Create map for input2
//		 */
//		logger.info("about to create temp map with input2 records");
//		Map<VcfRecord, VcfRecord> input2Map = input2.stream()
//				.collect(Collectors.toMap(vcf -> vcf, vcf -> vcf));
//		logger.info("about to create temp map with input2 records - DONE");
//		
//		/*
//		 * walk through input1, looking for matches in input2
//		 */
//		AtomicInteger uniqueToInput1 = new AtomicInteger();
//		AtomicInteger samePosition = new AtomicInteger();
//		
//		logger.info("Running through input1, creating merged records if they exist and adding to mergedRecords collection");
//		input1.stream()
//			.forEach( r ->{
//				VcfRecord input2R = input2Map.get(r);
//				if (null == input2R) {
//					uniqueToInput1.incrementAndGet();
//					mergedRecords.add(r);
//				} else {
//					samePosition.incrementAndGet();
//					mergeVcfRecords(r, input2R);
//				}
//			});
//		logger.info("Running through input1, creating merged records if they exist and adding to mergedRecords collection - DONE");
//		
//		logger.info("clearing elements in intput1 - no longer required");
//		input1.clear();
//		input1 = null;
//		logger.info("clearing elements in intput1 - no longer required - DONE");
//		
//		
//		logger.info("uniqueToInput1: " +uniqueToInput1.get());
//		logger.info("samePosition: " +samePosition.get());
//		logger.info("uniqueToInput2: " + (input2.size() - uniqueToInput1.get()));
//	}
	
	


//	private void mergeVcfRecords(VcfRecord r, VcfRecord r2) {
//		VcfRecord mr = MergeUtils.mergeRecords(null, r, r2);
//		mergedRecords.add(mr);
//		
//	}


	private void loadVcfs() throws IOException {
		headers = new VcfHeader[vcfFiles.length];
		int i = 0;
		int mergedRecordCount = 0;
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[0]))) {
			headers[0] = reader.getHeader();
			for (VcfRecord rec : reader) {
				if (++ i % 1000000 == 0) {
					logger.info("hit " + i + "entries");
				}
				/*
				 * Add in IN=1 to info field
				 */
				rec.appendInfo("IN=1");
				mergedRecords.put(rec, rec);
			}
		}
		logger.info("input1 has " + i + " entries");
		i = 0;
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[1]))) {
			headers[1] = reader.getHeader();
			for (VcfRecord rec : reader) {
				if (i++ % 1000000 == 0) {
					logger.info("hit " + i + "entries");
				}
				/*
				 * Add in IN=1 to info field
				 */
				rec.appendInfo("IN=2");
				
				VcfRecord input1Rec = mergedRecords.get(rec);
				if (null != input1Rec) {
					mergedRecordCount++;
					VcfRecord mr = MergeUtils.mergeRecords(null, input1Rec, rec);
					mergedRecords.put(rec, mr);
				} else {
					mergedRecords.put(rec, rec);
				}
			}
		}
		logger.info("input2 has " + i + " entries");
		
	}


	public static void main(String[] args) throws Exception {
		// loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(MergeSameSamples.class);
		
		MergeSameSamples qp = new MergeSameSamples();
		int exitStatus = qp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		} else {
			System.err.println("Exit status: " + exitStatus);
		}
		
		System.exit(exitStatus);
	}

	private int setup(String[] args) throws Exception {
		int returnStatus = 1;
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getVcfs().length < 1) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			version = MergeSameSamples.class.getPackage().getImplementationVersion();
			if (null == version) {
				version = "local";
			}
			logger = QLoggerFactory.getLogger(MergeSameSamples.class, logFile, options.getLogLevel());
			exec = logger.logInitialExecutionStats("q3clinvar", version, args);
			
			// get list of file names
			vcfFiles = options.getVcfs();
			if (vcfFiles.length < 1) {
				throw new Exception("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < vcfFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(vcfFiles[i])) {
						throw new Exception("INPUT_FILE_ERROR: "  +  vcfFiles[i]);
					}
				}
			}
			
			// set outputfile - if supplied, check that it can be written to
			if (null != options.getOutputFileName()) {
				String optionsOutputFile = options.getOutputFileName();
				if (FileUtils.canFileBeWrittenTo(optionsOutputFile)) {
					outputFileName = optionsOutputFile;
				} else {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
			}
			
			logger.info("vcf input files: " + Arrays.deepToString(vcfFiles));
			logger.info("outputFile: " + outputFileName);
			
			return engage();
		}
		return returnStatus;
	}

}
