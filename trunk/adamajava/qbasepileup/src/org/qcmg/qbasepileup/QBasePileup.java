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
import org.qcmg.qbasepileup.snp.SnpOption;

public class QBasePileup {
	
	private static QLogger logger;	
	private static String version;
	private Options options;
 
	public static void main(String[] args) throws Exception {
		
		
		
		
		if (args.length == 0) {
			System.err.println(Messages.USAGE);
		} else {
			Options options = new Options(args);
					System.out.println("after option");
			

/*			
			if (options.hasHelpOption()) {
				options.displayHelp();
			} else if (options.hasVersionOption()) {
				System.err.println(Messages.getVersionMessage());			
			} else {						
//				QBasePileup pileup = new QBasePileup();
	//			LoadReferencedClasses.loadClasses(QBasePileup.class);
				
				
				
				int	exitStatus = pileup.runBasePileup(args);
		
				if (null != logger)  
					logger.logFinalExecutionStats(exitStatus);
				 				
//				System.exit(exitStatus);
			}*/
		}
	}	

	public int runBasePileup(String[] args) {
		int exitStatus = 0;

		return exitStatus;
	}
	
	private int runSnpMode( ) throws Exception {
		SnpOption snpOpt = options.getSnpOption();
		logger.info("***INPUT FILES***");
		if (snpOpt.getHdf() != null) {
			logger.info("HDF file " + snpOpt.getHdf());
		}
		for (InputBAM i : snpOpt.getInputBAMs()) {
			logger.info("BAM file/s: " + i.getBamFile());
		}
		logger.info("Positions file: " + snpOpt.getPositionsFile());
		logger.info("Positions file format: " + snpOpt.getFormat());
		
		logger.info("***OUTPUT FILES***");
		logger.info("Log file " + snpOpt.getLog());
		logger.info("Pileup file " + snpOpt.getOutput());
		if (snpOpt.getOutputFormat() == 2) {
			logger.info("Output file format: columns");
		}
		
		logger.info("***FILTERING***");
		logger.info("Pileup profile: " + snpOpt.getProfile());
		logger.info("Base Quality filtering: " + snpOpt.getBaseQuality());
//		logger.info("Mapping Quality filtering: " + snpOpt.getMappingQuality());
		logger.info("Novel starts: " + snpOpt.isNovelstarts());
		logger.info("Coverage by strand: " + snpOpt.isStrandSpecific());
		logger.info("Include introns: " + snpOpt.includeIntron());
		logger.info("Include indels: " + snpOpt.includeIndel());		
		logger.info("Qbamfilter query: " + snpOpt.getFilterQuery());
//		logger.info("Include duplicate " + options.includeDuplicates());
		logger.info("Threads: " + snpOpt.getThreadNo() + " + 3");
		
		int lineNumber = countLines(snpOpt.getPositionsFile());
		
		if (lineNumber > 50000) {
				SnpBasePileupByFileMT mt = new SnpBasePileupByFileMT(options.getMode(),snpOpt);
				return mt.getExitStatus();		
		} else {
			SnpBasePileupMT mt = new SnpBasePileupMT(options.getMode(), snpOpt);
			return mt.getExitStatus();
		}		
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
