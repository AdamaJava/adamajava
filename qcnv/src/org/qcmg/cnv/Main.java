/**
 *  Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;

import java.io.File;

import java.io.FileWriter;
import java.util.List;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMSequenceRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.common.date.DateUtils;


public class Main {
	
	 public static void main(final String[] args) throws Exception {
        LoadReferencedClasses.loadClasses(Main.class);
        Options option = new Options(args);
        if(option.hasHelp() || option.hasVersion())
        	 System.exit(0); 

        QLogger logger =  option.getLogger(args);	
        try{     	

			MtCounts cnvThread = new MtCounts(option.getInputNames(), option.getSampleIds(), 
					option.getOutputName(), option.getThreadNumber(), option.getWindowSize(), logger);
			
			cnvThread.callCounts();
        	logger.logFinalExecutionStats(0);
        	System.exit(0);
        }catch(Exception e){ 
        	logger.error(e.toString());
        	logger.logFinalExecutionStats(1);
        	System.err.println(e.toString());       	 
        	System.exit(1);
        }		
	 }
}



 