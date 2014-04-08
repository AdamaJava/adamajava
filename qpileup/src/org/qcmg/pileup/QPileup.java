/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.pileup.mode.ViewMT;

public final class QPileup {
	
	private static QLogger logger;	
	private static String version;	
	private Options options;	

	public static void main(String[] args) throws Exception {		

		QPileup qpileup = new QPileup();
		long start = System.currentTimeMillis();
		int	exitStatus = qpileup.runPileup(args, start);
		long end = System.currentTimeMillis();		
		
				
		if (null != logger) {
			logger.info("Run time (ms): " + end);
			logger.info("Run time: " + PileupUtil.getRunTime(start, end));
			logger.logFinalExecutionStats(exitStatus);
		}
		
		System.exit(exitStatus);	
	}

	public int runPileup(String[] args, long start)  {
		int exitStatus = 0;
		
			
		try {
			options = new Options(args);
			
			if (args.length == 0) {
				System.err.println(Messages.USAGE);
			} else if (options.hasHelpOption()) {
				options.displayHelp();
			} else if (options.hasVersionOption()) {
				System.err.println(Messages.getVersionMessage());	
			} else if (options.hasDeleteOption()) {
				//exitStatus = options.deleteLocks();
			} else if (options.hasViewOption()) {
				options.detectBadOptions();
				ViewMT view = new ViewMT(options);
				view.execute();
			} else {
				LoadReferencedClasses.loadClasses(QPileup.class);
				//check to make sure there are no bad options
				options.parseIniFile();
				
				// configure logging				
				version = QPileup.class.getPackage().getImplementationVersion();
				logger = QLoggerFactory.getLogger(QPileup.class, options.getLog(), options.getLogLevel());
				QExec exec = logger.logInitialExecutionStats("qpileup", version, args);			
				options.setQExec(exec);	
				//Run the QSV pipeline
				PileupPipeline pipeline = new PileupPipeline(options, start);
				pipeline.runPipeline();
			}
		} catch (Exception e) {	
			
		    System.err.println(Messages.USAGE);
		    e.printStackTrace();
		    
		   //gracefully close hdf file
		   try {
			   if (H5.getOpenIDCount() == 1) {
				   H5.H5Fclose(H5.getOpenID(0));
			   }
			} catch (HDF5LibraryException e1) {
				e1.printStackTrace();
			}

			exitStatus = 1;
			if (null != logger) {				
				logger.error(PileupUtil.getStrackTrace(e));
			} else {
				System.err.print(e.toString());
			}
		}
		
		return exitStatus;
	}
}
