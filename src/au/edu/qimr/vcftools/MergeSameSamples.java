package au.edu.qimr.vcftools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.vcf.VCFFileReader;

public class MergeSameSamples {
	
	private static QLogger logger;
	private QExec exec;
	private String[] vcfFiles;
	private String outputFileName;
	private String version;
	private String logFile;
	private int exitStatus;
	
	private final List<VcfRecord> mergedRecords = new ArrayList<>();
	private final List<VcfRecord> input1 = new ArrayList<>();
	// assuming there are only 2 inputs for now...
	private final Map<VcfRecord, VcfRecord> input2 = new HashMap<>();
//	private final TMap<ChrPosition, VcfRecord> input2 = new THashMap<>();
	
	private VcfHeader [] headers;
	
	protected int engage() throws IOException {
		
		logger.info("about to load vcf files");
		loadVcfs();
		
		processRecords();
		
		
		return exitStatus;
	}
	
	
	private void processRecords() {
		/*
		 * walk through input1, looking for matches in input2
		 */
		AtomicInteger uniqueToInput1 = new AtomicInteger();
		AtomicInteger samePosition = new AtomicInteger();
		input1.stream()
			.forEach(r -> {
				VcfRecord input2R = input2.get(r);
				if (null == input2R) {
					uniqueToInput1.incrementAndGet();
				} else {
					samePosition.incrementAndGet();
					mergeVcfRecords(r, input2R);
				}
			});
		
		logger.info("uniqueToInput1: " +uniqueToInput1.get());
		logger.info("samePosition: " +samePosition.get());
		logger.info("uniqueToInput2: " + (input2.size() - uniqueToInput1.get()));
	}
	
	


	private void mergeVcfRecords(VcfRecord r, VcfRecord r2) {
		
		
	}


	private void loadVcfs() throws IOException {
		headers = new VcfHeader[vcfFiles.length];
		int i = 0;
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[0]))) {
			headers[0] = reader.getHeader();
			for (VcfRecord rec : reader) {
				if (i++ % 1000000 == 0) {
					logger.info("hit " + i + "entries");
				}
				input1.add(rec);
			}
		}
		logger.info("input1 has " + input1.size() + " entries");
		i = 0;
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[1]))) {
			headers[1] = reader.getHeader();
			for (VcfRecord rec : reader) {
				if (i++ % 1000000 == 0) {
					logger.info("hit " + i + "entries");
				}
				input2.put(rec, rec);
			}
		}
		logger.info("input2 has " + input2.size() + " entries");
		
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
