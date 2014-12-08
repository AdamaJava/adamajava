/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;

public class SnpFileDetails {
	
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private int exitStatus;
	private final List<VcfRecord> snps = new ArrayList<VcfRecord>();
	
	private int engage() throws Exception {
		
		loadRandomSnpPositions(cmdLineInputFiles[0]);
		
		return exitStatus;
	}
	
	private void loadRandomSnpPositions(String randomSnpsFile) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(randomSnpsFile));
		try {
			int count = 0, emptyRefCount = 0, dashRef = 0;
			for (TabbedRecord rec : reader) {
				++count;
				String[] params = TabTokenizer.tokenize(rec.getData());
				
				String ref = null;
				if (params.length > 4 && ! StringUtils.isNullOrEmpty(params[4])) {
					ref = params[4];
					if ("-".equals(ref) || ".".equals(ref)) {
						dashRef++;
						logger.info("dash ref: " + rec.getData());
					}
				} else {
					emptyRefCount++;
					logger.info("empty ref: " + rec.getData());
				}
				
				String chr = params[0];
				int position = Integer.parseInt(params[1]);
				VcfRecord vcf =VcfUtils.createVcfRecord(chr, position, ref);
				vcf.setId(params[2]);
				if (params.length > 5) {
					vcf.setAlt(params[5].replaceAll("/", ","));
				}

				// Lynns new files are 1-based - no need to do any processing on th position
				snps.add(vcf);
			}
			
			// sort - this step is now done after retrieving the sequences from the bam header
//			Collections.sort(snps, new VcfPositionComparator());
			
			
			logger.info("Loaded " + snps.size() + " positions into map (should be equal to: " + count + ") empty refs: " + emptyRefCount
					+ ", dash refs: " + dashRef);
		} finally {
			reader.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		SnpFileDetails sp = new SnpFileDetails();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running QSignatureSequential:", e);
			else System.err.println("Exception caught whilst running QSignatureSequential");
			e.printStackTrace();
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(SnpFileDetails.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("SnpFileDetails", SnpFileDetails.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QSignatureException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QSignatureException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			return engage();
		}
		return returnStatus;
	}

}
