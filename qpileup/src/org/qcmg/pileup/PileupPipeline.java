/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.mode.AddMT;
import org.qcmg.pileup.mode.BootstrapMT;
import org.qcmg.pileup.mode.MergeMT;
import org.qcmg.pileup.mode.MetricsMT;
import org.qcmg.pileup.mode.ViewMT;


public class PileupPipeline {
	
	private QLogger logger = QLoggerFactory.getLogger(getClass());
	private String mode;
	private int exitStatus = 0;
	private Options options;
	private long startTime;
	
	public PileupPipeline(Options options, long start) {		
		this.mode = options.getMode();
		this.startTime = start;
		this.options = options;
	}

	public void runPipeline() throws Exception {		
		
		logger.info("Mode: " + mode);
		logger.info("HDF file: "  + options.getHdfFile());
		logger.info("ThreadNo option: " + options.getThreadNo());
		
		List<String> readRanges = options.getReadRanges();
		
		for (String s: readRanges) {
			logger.info("Chromosome: " + s);
		}
		
		if (mode.equals("bootstrap")) {			
			BootstrapMT bootstrap = new BootstrapMT(options, startTime);			
			exitStatus = bootstrap.execute();
		} else if (mode.equals("merge")) {		
			MergeMT merge = new MergeMT(options, startTime, mode);
			exitStatus = merge.execute();			
		} else if (mode.equals("add") || mode.equals("remove")) {
			logger.info("Starting " + mode + " mode");
			AddMT add = new AddMT(options, startTime, mode);
			exitStatus = add.execute();	
		} else if (mode.equals("view")) {			
			ViewMT read = new ViewMT(options);			
			exitStatus = read.execute();
		} else if (mode.equals("metrics")) {
			MetricsMT metric = new MetricsMT(options);			
			exitStatus = metric.execute();	
		} else {
			throw new QPileupException("UNKNOWN_MODE", mode);
		}
		
		if (exitStatus > 0) {
			throw new QPileupException("RUN_EXCEPTION", mode);
		}
		logger.info(mode.toUpperCase() + " mode complete");
	}		
	
}
