package au.edu.qimr.vcftools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.vcf.ContentType;
import org.qcmg.common.vcf.VcfFileMeta;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
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
	
	private final Map<ChrPosition, VcfRecord> input1 = new HashMap<>(1024 * 1024 * 8, 0.95f);
	private final List< VcfRecord> mergedRecords = new ArrayList<>(1024 * 1024);
	
	
	private VcfHeader [] headers;
	private ContentType [] contentTypes;
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
			
//			addMissingFormatFields();
			
			writeOutput();
		} else {
			logger.error("Headers from supplied vcf files cannot be merged - exiting");
			exitStatus = 1;
		}
		return exitStatus;
	}
	
	private boolean canHeadersBeMerged() throws IOException {
		headers = new VcfHeader[vcfFiles.length];
		contentTypes = new ContentType[vcfFiles.length];
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[0]))) {
			headers[0] = reader.getHeader();
			VcfFileMeta meta = new VcfFileMeta(headers[0]);
			contentTypes[0] = meta.getType();
		}
		try (VCFFileReader reader = new VCFFileReader(new File(vcfFiles[1]))) {
			headers[1] = reader.getHeader();
			VcfFileMeta meta = new VcfFileMeta(headers[1]);
			contentTypes[1] = meta.getType();
		}
		boolean canHeadersBeMerged = MergeUtils.canMergeBePerformed(headers);
		if (canHeadersBeMerged) {
			/*
			 * check to see if we have the same content type across the board
			 */
			canHeadersBeMerged = (contentTypes[0] == contentTypes[1]);
		}
		logger.info("canHeadersBeMerged: " + canHeadersBeMerged);
		return canHeadersBeMerged;
	}

	private void loadVcfHeaders() throws IOException {
		
		Pair<VcfHeader, Rule> pair = MergeUtils.getMergedHeaderAndRules(headers);
		mergedHeader = null != pair ? pair.getLeft() : null;
		if (null == mergedHeader) {
			throw new IllegalArgumentException("Null mergedHeader from MergeUtils.getMergedHeaderAndRules");
		}
		VcfHeaderUtils.addQPGLine(mergedHeader, 1, exec);
		mergedHeader.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);
		
		int i = 1; 
		for (String s : vcfFiles) {
			logger.info("adding header entry for input " + i + " : " + s);
			mergedHeader.addOrReplace(VcfHeaderUtils.BLANK_HEADER_LINE + i + Constants.COLON + "VCFFileToBeMerged=" + s);
			i++;
		}
	}
	
//	private void addMissingFormatFields() {
//		/*
//		 * for input1 records, add 2 format fields to the end of the record
//		 */
//		for (VcfRecord v : input1.values()) {
//			List<String> ff = v.getFormatFields();
//			VcfUtils.addAdditionalSamplesToFormatField(v, Arrays.asList(ff.get(0), Constants.MISSING_DATA_STRING, Constants.MISSING_DATA_STRING));
//		}
//		
//		/*
//		 * for input2 records, add 2 format fields to beginning of format fields
//		 */
//		for (VcfRecord v : input2) {
//			VcfUtils.addMissingDataToFormatFields(v, 1);
//			VcfUtils.addMissingDataToFormatFields(v, 1);
//		}
//	}
	
	
	private void writeOutput() throws IOException {
		List<VcfRecord> recs = new ArrayList<>(mergedRecords);
//		recs.addAll(input1.values());
//		recs.addAll(input2);
		recs.sort(null);
		
		logger.info("writing output [" + recs.size() + " records]");
		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileName))) {
			Iterator<VcfHeaderRecord> iter =mergedHeader.iterator(); 
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
				input1.put(rec.getChrPosition(), rec);
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
				 * Add in IN=2 to info field
				 */
				rec.appendInfo(Constants.VCF_MERGE_INFO + "=2");
				
				VcfRecord input1Rec = input1.remove(rec.getChrPosition());
				if (null != input1Rec) {
					VcfRecord mr = MergeUtils.mergeRecords(null, input1Rec, rec);
					mergedRecords.add(mr);
				} else {
//					input2.add(rec);
					/*
					 * add missing format columns to rec
					 */
					VcfUtils.addMissingDataToFormatFields(rec, 1, ContentType.multipleSamples(contentTypes[0]) ? 2 : 1);
//					VcfUtils.addMissingDataToFormatFields(rec, 1);
					mergedRecords.add(MergeUtils.mergeRecords(null, rec));
				}
			}
		}
		for (VcfRecord rec : input1.values()) {
			/*
			 * add missing sample columns at end
			 */
//			List<String> ff = rec.getFormatFields();
//			VcfUtils.addAdditionalSamplesToFormatField(rec, Arrays.asList(ff.get(0), Constants.MISSING_DATA_STRING, Constants.MISSING_DATA_STRING));
			int position = ContentType.multipleSamples(contentTypes[0]) ? 3 : 2;
			VcfUtils.addMissingDataToFormatFields(rec, position, ContentType.multipleSamples(contentTypes[0]) ? 2 : 1);
			mergedRecords.add(MergeUtils.mergeRecords(null, rec));
		}
		logger.info("input2 has " + i + " entries");
	}


	public static void main(String[] args) throws Exception {
		//loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(MergeSameSamples.class);
		
		MergeSameSamples qp = new MergeSameSamples();
		int exitStatus = qp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		} else {
			System.err.println("Exit status: " + exitStatus);
		}		
		System.exit( exitStatus );
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
			if (null == version) {	version = "local"; }
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
