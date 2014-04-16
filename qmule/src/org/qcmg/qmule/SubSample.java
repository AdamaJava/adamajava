/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.util.ArrayList;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.HeaderUtils;
import net.sf.samtools.*;

public class SubSample {
	SAMFileReader reader;
	SAMFileWriter writer;
	double proportion;
	QLogger logger;
	
	SubSample(Options op, QLogger log) throws Exception{	
		

		proportion = op.getPROPORTION();	 
		logger = log;
		
		String[] inputs =op.getInputFileNames();
		String[] outputs =op.getOutputFileNames();
		if(inputs.length == 0 || outputs.length == 0) 
			throw new Exception("please specify input/output");
		 	
		//get initialized logger		
		File input = new File(inputs[0]);
		File output = new File(outputs[0]);	
		if(!input.canRead()) 
			throw new Exception("unreadable input: " + input.getAbsolutePath());	 
			
		reader = new SAMFileReader(input);			
		SAMFileHeader header = reader.getFileHeader();
		if(header.getSortOrder() != SAMFileHeader.SortOrder.queryname){
			throw new Exception("the input BAM is not sorted by queryname");
		}
        SAMFileWriterFactory writeFactory = new SAMFileWriterFactory();   
        HeaderUtils.addProgramRecord(header,  op.getCommandLine(), null );
         
        writer = writeFactory.makeSAMOrBAMWriter(header, false, output );
	}

	void run() throws Exception{
		int numPair = 0;
		int numSingle = 0;
		int numtotal = 0;
		 SAMRecordIterator ie = reader.iterator();
		 ArrayList<SAMRecord> adjacents = new ArrayList<SAMRecord>();
		 adjacents.add(ie.next());		 
		 
		 while(ie.hasNext()){	
			 numtotal ++;
			 SAMRecord  record = ie.next();		
			//select reads
			if(! record.getReadName().equals(adjacents.get(0).getReadName())){			
				//select pairs
				if(adjacents.size() > 1)
					numPair += selectPair( adjacents);
				//select single
				else if(Math.random() < proportion  ){
					writer.addAlignment(adjacents.get(0));
					numSingle ++;
				}				
				//after reporting clear the arraylist
				adjacents.clear();
			} 
			adjacents.add(record);

	 	}
		 
		 //select last records
		 if(adjacents.size() > 1)
				selectPair( adjacents);
		 else if(Math.random() < proportion  )
			writer.addAlignment(adjacents.get(0));
 
		reader.close();
		writer.close();
		
		logger.info("total reads in input is " + numtotal);
		logger.info("select paired reads is " + numPair);
		logger.info("select single reads is " + numSingle);
		logger.info("the rate of selected reads is "+ ((double)(numPair + numSingle)) / numtotal);
 
	}

	private int selectPair(ArrayList<SAMRecord> pairs) {
 
		if(pairs.size() == 0 ){
			logger.error("Program Error: select reads from empty arraylist! ");
			return 0;
		}		 
		if(pairs.size() == 1 ){
			logger.error("program Error: single read in paired arraylist -- " + pairs.get(0).getReadName());
			return 0; 
		}
		
		int num = 0;		
		while(pairs.size() >= 2){		
			//seek pair one by one
			SAMRecord first  = pairs.get(0);
			SAMRecord mate = null;
			pairs.remove(first);
			
			for(int i = 0; i < pairs.size(); i ++){
				if(first.getReadGroup().getId().equals(pairs.get(i).getReadGroup().getId())){
					mate = pairs.get(i);
					pairs.remove(mate);
					break;
				}
			}
			
			if(Math.random() <  proportion ){			
				num ++; //number of selected paired reads
				writer.addAlignment(first);
				if(mate != null){
					num ++;
					writer.addAlignment(mate);
				}else{
					logger.error("paired reads missing mate -- " + pairs.get(0).getReadName());
				}	
			}
		}	

		return num;		
	}
	
	public static void main(String[] args) throws Exception{	
		Options op = new Options(SubSample.class,  args);    
	    if(op.hasHelpOption()){
	    	System.out.println(Messages.getMessage("USAGE_SUBSAMPLE"));
	    	op.displayHelp();
	    	System.exit(0);		
	    }
    
		String version = org.qcmg.qmule.Main.class.getPackage().getImplementationVersion();	
	    QLogger logger = QLoggerFactory.getLogger(SubSample.class, op.getLogFile(), op.getLogLevel());	
	    try{
			logger.logInitialExecutionStats(SubSample.class.toString(), version, args);	
			logger.exec("Porportion " + op.getPROPORTION());
			SubSample mySample = new SubSample(op, logger);	
			mySample.run();
			logger.logFinalExecutionStats(0);
			System.exit(0);		
		}catch(Exception e){
			System.err.println(e.toString());
			logger.logFinalExecutionStats(-1);
			System.exit(-1);
		}
	}

}
