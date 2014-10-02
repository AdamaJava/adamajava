/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.qbasepileup.coverage.CoveragePileupMT;
import org.qcmg.qbasepileup.indel.IndelBasePileupByChrMT;
import org.qcmg.qbasepileup.indel.IndelBasePileupMT;
import org.qcmg.qbasepileup.snp.SnpBasePileupByFileMT;
import org.qcmg.qbasepileup.snp.SnpBasePileupMT;


public class QBasePileup {
	
	private static QLogger logger;	
	private static String version;
	private Options options;
	
	public static void main(String[] args) throws Exception {
		QBasePileup pileup = new QBasePileup();
		LoadReferencedClasses.loadClasses(QBasePileup.class);
		
		int exitStatus = pileup.runBasePileup(args);

		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		
		System.exit(exitStatus);
	}	

	public int runBasePileup(String[] args) {
		int exitStatus = 0;
		try {
			//Get options from the command line			
			if (args.length == 0) {
				System.err.println(Messages.USAGE);
			} else {
				this.options = new Options(args);
				if (options.hasHelpOption()) {
					options.displayHelp();
				} else if (options.hasVersionOption()) {
					System.err.println(Messages.getVersionMessage());			
				} else {						
									
					version = QBasePileup.class.getPackage().getImplementationVersion();
					logger = QLoggerFactory.getLogger(QBasePileup.class, options.getLog(), "INFO");
					logger.logInitialExecutionStats("qbasepileup", version, args, QExec.createUUid());
					
					logger.info("Running " + options.getMode() + " mode");
					
					if (options.getMode().equals("snp") || options.getMode().equals(QBasePileupConstants.COMPOUND_SNP_MODE)
							|| options.getMode().equals(QBasePileupConstants.SNP_CHECK_MODE)) {
						return runSnpMode();						
					} 
					if (options.getMode().equals("indel")){
						return runIndelMode();	
					}
					if (options.getMode().equals("coverage")){
						return runCoverageMode();	
					}
				}
			}
			
		} catch (Exception e) {	
		    System.err.println(Messages.USAGE);
			e.printStackTrace();
			exitStatus = 1;
			if (null != logger) {				
				logger.error(e.getMessage());
			} else {
				System.err.print(e.getMessage());
			}
		}
		return exitStatus;
	}


	private int runSnpMode() throws Exception {
		logger.info("***INPUT FILES***");
		if (options.getHdf() != null) {
			logger.info("HDF file " + options.getHdf());
		}
		for (InputBAM i : options.getInputBAMs()) {
			logger.info("BAM file/s: " + i.getBamFile());
		}
		logger.info("Positions file: " + options.getPositionsFile());
		logger.info("Positions file format: " + options.getFormat());
		
		logger.info("***OUTPUT FILES***");
		logger.info("Log file " + options.getLog());
		logger.info("Pileup file " + options.getOutput());
		if (options.getOutputFormat() == 2) {
			logger.info("Output file format: columns");
		}
		
		
		logger.info("***FILTERING***");
		logger.info("Pileup profile: " + options.getProfile());
		logger.info("Base Quality filtering: " + options.getBaseQuality());
		logger.info("Mapping Quality filtering: " + options.getMappingQuality());
		logger.info("Novel starts: " + options.isNovelstarts());
		logger.info("Coverage by strand: " + options.isStrandSpecific());
		logger.info("Include introns: " + options.includeIntron());
		logger.info("Include indels: " + options.includeIndel());		
		logger.info("Qbamfilter query: " + options.getFilterQuery());
		logger.info("Include duplicate " + options.includeDuplicates());
		logger.info("Threads: " + options.getThreadNo() + " + 3");
		
		int lineNumber = countLines(options.getPositionsFile());
		
		if (lineNumber > 50000) {
				SnpBasePileupByFileMT mt = new SnpBasePileupByFileMT(options);
				return mt.getExitStatus();		
		} else {
			SnpBasePileupMT mt = new SnpBasePileupMT(options);
			return mt.getExitStatus();
		}		
	}
	
	private int runIndelMode() throws Exception {
		logger.info("***INPUT FILES***");
		logger.info("Tumour bam: " + options.getTumourBam());
		logger.info("Normal bam: " + options.getNormalBam());
		logger.info("Somatic indel file: " + options.getSomaticIndelFile());
		logger.info("Germline indel file " + options.getGermlineIndelFile());	
		if (options.hasPindelOption()) {
			logger.info("File type: pindel");	
		}
		if (options.hasStrelkaOption()) {
			logger.info("File type: strelka");	
		}
		if (options.hasGATKOption()) {
			logger.info("File type: gatk");	
		}
		logger.info("***OUTPUT FILES***");
		logger.info("Log file " + options.getLog());
		logger.info("Somatic pileup file " + options.getSomaticOutputFile());
		logger.info("Germline pileup file " + options.getGermlineOutputFile());
		logger.info("***OPTIONS***");
		logger.info("Homopolymer window: " + options.getNearbyHomopolymerWindow());
		logger.info("Nearby indel window: " + options.getNearbyIndelWindow());
		logger.info("Soft clip window: " + options.getSoftClipWindow());
		logger.info("Include duplicate " + options.includeDuplicates());
		logger.info("Threads: " + options.getThreadNo() + " + 3");
		logger.info("***OTHER OPTIONS***");
		
		if (options.getFilterQuery() != null) {
			logger.info("Qbamfilter query: " + options.getFilterQuery());	
		}
		
		if (options.getSomaticIndelFile() != null) {
			logger.info("Piling up somatic indels");
			IndelBasePileupMT mtSom = new IndelBasePileupMT(options.getSomaticIndelFile(), options.getSomaticOutputFile(), options.getOutput(), false, options);
			if (mtSom.getExitStatus() > 0) {
				return 1;
			}
		}
		
		if (options.getGermlineIndelFile() != null) {
			logger.info("Piling up germline indels");
			//work out number of lines in file
			int lineNumber = countLines(options.getGermlineIndelFile());
			logger.info("Number of lines in file: " + lineNumber);
			if (lineNumber > 50000) {
				IndelBasePileupByChrMT mtGerm = new IndelBasePileupByChrMT(options.getGermlineIndelFile(), options.getGermlineOutputFile(), options.getOutput(), true, options);
				if (mtGerm.getExitStatus() > 0) {
					return 1;
				}
			} else {
				IndelBasePileupMT mtGerm = new IndelBasePileupMT(options.getGermlineIndelFile(), options.getGermlineOutputFile(), options.getOutput(), true, options);
				if (mtGerm.getExitStatus() > 0) {
					return 1;
				}
			}
			
		} 
		return 0;
	}
	
	private int runCoverageMode() throws Exception {
		logger.info("***INPUT FILES***");
		
		for (InputBAM i : options.getInputBAMs()) {
			logger.info("BAM file/s: " + i.getBamFile());
		}
		logger.info("Positions file: " + options.getPositionsFile());
		logger.info("Positions file format: " + options.getFormat());
		
		logger.info("***OUTPUT FILES***");
		logger.info("Log file " + options.getLog());
		logger.info("Pileup file " + options.getOutput());
		
		logger.info("***FILTERING***");	
		logger.info("Qbamfilter query: " + options.getFilterQuery());
		if (options.getMaxCoverage() != null) {
			logger.info("maximum coverage option: " + options.getMaxCoverage());
		}
		logger.info("Qbamfilter query: " + options.getFilterQuery());
		logger.info("Threads: " + options.getThreadNo() + " + 3");
		CoveragePileupMT mt = new CoveragePileupMT(options);
		return mt.getExitStatus();
	}
	
	private int countLines(File file) throws IOException {		
	    InputStream is = new BufferedInputStream(new FileInputStream(file));
	    try {
	        byte[] c = new byte[1024];
	        int count = 0;
	        int readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++count;
	                }
	            }
	        }
	        return (count == 0 && !empty) ? 1 : count;
	    } finally {
	        is.close();
	    }
		
	}
}
