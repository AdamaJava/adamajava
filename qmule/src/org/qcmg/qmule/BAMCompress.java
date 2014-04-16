/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.util.List;


import net.sf.samtools.BAMFileWriter;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;

public class BAMCompress {
		static QLogger logger = QLoggerFactory.getLogger(BAMCompress.class);
		private static File input;
		private static File output;
		private static int level;
		
		BAMCompress(File input, File output, int level) throws Exception{
			this.input = input;
			this.output = output;
			this.level = level;
			
 	 		logger.info("input file: " + input.getAbsolutePath());
	 		logger.info("output file name: " + output.getAbsolutePath());
	 		logger.info("compress level for output BAM: " + level);		
		}

		public void replaceSeq() throws Exception{			
			BAMFileWriter writer = new BAMFileWriter(output, level) ;			
			SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(
					input, SAMFileReader.ValidationStringency.SILENT);
					

			//create header
			writer.setHeader(reader.getFileHeader());						
			for( SAMRecord record : reader){
				//only replace fully mapped reads, that is no clipping, indels and pading
				if( seekFullMppaed(record) && seekMismatch(record) ){					 
						byte[] base = record.getReadBases();
						for(int i = 0; i < base.length; i++)
							base[i] = 'N';
						record.setReadBases(base);		
				} 
				
				if(record.isValid() == null)  // if valid
					writer.addAlignment( record );				 
			}
			
			reader.close();	
			writer.close(); 
			
			logger.info( "input "  + reportFileSize(input)  );
			logger.info( "output " + reportFileSize(output) );
			 
		}
			
		public String reportFileSize(File f){
			
			double bytes_in = f.length();
			double kilobytes = (bytes_in / 1024);
			double megabytes = (kilobytes / 1024);
			double gigabytes = (megabytes / 1024);
			
			return String.format("file size is %.2fG or %.2fK", gigabytes, kilobytes);			
		}
		
		
		private boolean seekMismatch(SAMRecord r) {		
			String attribute = (String)r.getAttribute("MD");	
			if (null != attribute) {			 
				for (int i = 0, size = attribute.length() ; i < size ; ) {
					char c = attribute.charAt(i);
					if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N') {
						return false;
					} else if ( c == '^') {
						//skip the insertion base
						while (++i < size && Character.isLetter(attribute.charAt(i))) {}
					} else i++;	// need to increment this or could end up with infinite loop...
				}
				return true;
			} 			
			return false;
		}	
			
		private boolean seekFullMppaed(SAMRecord r){
			 
			if(r.getReadUnmappedFlag()) 
				return false;
		 
			//reads with clips or indel, skips, pads			 
			List<CigarElement> ele = r.getCigar().getCigarElements();
 			for (CigarElement element : r.getCigar().getCigarElements()){		
 				if( element.getLength() > 0){ 					
 					if(element.getOperator() == CigarOperator.H ||element.getOperator() == CigarOperator.S) {					 	 						
 						return false;
 					}else if (element.getOperator() == CigarOperator.I ||element.getOperator() == CigarOperator.D){	 						 
 						return false;
 					}else if (element.getOperator() == CigarOperator.P ||element.getOperator() == CigarOperator.N){	 						 
 						return false;
 					}
 				}
 			}
 			
 			return true;
		} 
		

		public static void main(String[] args) throws Exception{
			Options op = new Options(BAMCompress.class, args);    
		    if(op.hasHelpOption()){
		    	System.out.println(Messages.getMessage("USAGE_BAMCompress"));
		    	op.displayHelp();
		    	System.exit(0);		
		    }
		 		
	  	    String output = op.getOutputFileNames()[0];
		    String input =  op.getInputFileNames()[0];	  		    
			if(! new File(input).exists() ) 
				throw new Exception("input file not exists: " + args[0]);

			if(op.hasLogOption())
				logger = QLoggerFactory.getLogger(BAMCompress.class, op.getLogFile(), op.getLogLevel());
	 		else
				logger = QLoggerFactory.getLogger(BAMCompress.class, output + ".log", op.getLogLevel());	

			String version = org.qcmg.qmule.Main.class.getPackage().getImplementationVersion();	
		 	logger.logInitialExecutionStats( "qmule " + BAMCompress.class.getName(), version,args);

			int level = op.getcompressLevel(); //default compress level			
			
  			logger.logInitialExecutionStats( "qmule " + BAMCompress.class.getName(), null,args);
 			
 			long startTime = System.currentTimeMillis();
			BAMCompress compress = new BAMCompress(new File(input), new File(output) ,  level  );				
			compress.replaceSeq();

			logger.info( String.format("It took %d hours, %d seconds to perform the compression",
					 (int) (System.currentTimeMillis() - startTime) / (1000*60*60), 
					 (int) ( (System.currentTimeMillis() - startTime) / (1000*60) ) % 60)  );
			logger.logFinalExecutionStats(0);
			
		}
 
		 
}
