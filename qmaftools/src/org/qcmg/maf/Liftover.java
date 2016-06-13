/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import htsjdk.samtools.liftover.LiftOver;
import htsjdk.samtools.util.Interval;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.maf.util.MafUtils;

public class Liftover {
	
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	private String chainFile;
	
	private final Collection<ChrPosition> positionsOfInterest = new HashSet<ChrPosition>();
	
	private int engage() throws Exception {
		
		logger.info("Loading maf file");
		MafUtils.loadPositionsOfInterest(cmdLineInputFiles[0], positionsOfInterest);
		logger.info("Loading maf file - DONE");
		logger.info("performing liftover");
		liftover();
		logger.info("performing liftover - DONE");
		
		return exitStatus;
	}
	
	private void liftover() {
		// loop through positions of interest, getting new Interval for each
		LiftOver picardLiftover = new LiftOver(new File(chainFile));
		for (ChrPosition cp : positionsOfInterest) {
			Interval oldInt = new Interval(MafUtils.getFullChrFromMafChr(cp.getChromosome()), cp.getStartPosition(), cp.getEndPosition());
			Interval newInt =  picardLiftover.liftOver(oldInt);
			logger.info("oldInt: " + oldInt + ", new Int: " + newInt);
		}
	}

	public static void main(String[] args) throws Exception {
		Liftover sp = new Liftover();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 1;
			if (null != logger)
				logger.error("Exception caught whilst running Liftover:", e);
			else System.err.println("Exception caught whilst running Liftover");
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
//		if (null == args || args.length == 0) {
//			System.err.println(Messages.USAGE);
//			System.exit(1);
//		}
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
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMafException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMafException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMafException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			if ((chainFile = options.getChainFile()) == null)
				throw new QMafException("NO_CHAIN_FILE_ERROR");
			if ( ! FileUtils.canFileBeRead(chainFile)) {
				throw new QMafException("INPUT_FILE_READ_ERROR" , chainFile);
			}
			
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(Liftover.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("Liftover", Liftover.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}
}
