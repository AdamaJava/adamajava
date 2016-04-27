package au.edu.qimr.vcftools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
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
	
	private final Map<VcfRecord, VcfRecord> mergedRecords = new HashMap<>(1024 * 1024 * 8, 0.95f);
	
	private VcfHeader [] headers;
	private VcfHeader mergedHeader;
	
	protected int engage() throws IOException {
		
		if (canHeadersBeMerged()) {
			logger.info("about to load vcf headers");
			loadVcfHeaders();
			if (null == mergedHeader) {
				logger.error("Merged header is null - please check that the vcf files being merged contain the same samples");
				return 1;
			}
			
			logger.info("about to load vcf files");
			loadVcfs();
			
			writeOutput();
		} else {
			logger.error("Headers from supplied vcf files cannot be merged - exiting");
			exitStatus = 1;
		}
		return exitStatus;
	}
	
	private boolean canHeadersBeMerged() throws IOException {
		headers = new VcfHeader[vcfFiles.length];
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[0]))) {
			headers[0] = reader.getHeader();
		}
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[1]))) {
			headers[1] = reader.getHeader();
		}
		boolean canHeadersBeMerged = MergeUtils.canMergeBePerformed(headers);
		logger.info("canHeadersBeMerged: " + canHeadersBeMerged);
		return canHeadersBeMerged;
		
	}

	private void loadVcfHeaders() throws IOException {
		
		Pair<VcfHeader, Rule> pair = MergeUtils.getMergedHeaderAndRules(headers);
		mergedHeader = null != pair ? pair.getLeft() : null;
		mergedHeader.addQPGLine(1, exec);
		mergedHeader.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);
		
		int i = 1; 
		for (String s : vcfFiles) {
			logger.info("adding header entry for input " + i + " : " + s);
			mergedHeader.parseHeaderLine(VcfHeaderUtils.BLANK_HEADER_LINE + "INPUT=" + i + ",FILE=" + s);
			i++;
		}
	}
	
	
	private void writeOutput() throws IOException {
		List<VcfRecord> recs = new ArrayList<>(mergedRecords.values());
		Collections.sort(recs);
		
		logger.info("writing output");
		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileName))) {
			Iterator<Record> iter =mergedHeader.iterator(); 
			while (iter.hasNext()) {
				writer.addHeader(iter.next().toString());
			}
			for (VcfRecord rec : recs) {
				writer.add(rec);
			}
		}
		logger.info("writing output- DONE");
		
	}
	
	private void loadVcfs() throws IOException {
		int i = 0;
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[0]))) {
			for (VcfRecord rec : reader) {
				if (++ i % 1000000 == 0) {
					
					logger.info("hit " + i + " entries");
//					break;
				}
				/*
				 * Add in IN=1 to info field
				 */
				rec.appendInfo(Constants.VCF_MERGE_INFO + "=1");
				mergedRecords.put(rec, rec);
			}
		}
		logger.info("input1 has " + i + " entries");
		i = 0;
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[1]))) {
			for (VcfRecord rec : reader) {
				if (++i % 1000000 == 0) {
					logger.info("hit " + i + " entries");
//					break;
				}
				/*
				 * Add in IN=1 to info field
				 */
				rec.appendInfo(Constants.VCF_MERGE_INFO + "=2");
				
				VcfRecord input1Rec = mergedRecords.get(rec);
				if (null != input1Rec) {
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
			exec = logger.logInitialExecutionStats("q3vcftools MergeSameSample", version, args);
			
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
