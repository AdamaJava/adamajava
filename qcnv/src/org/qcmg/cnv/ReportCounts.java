/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.SAMFileReader.ValidationStringency;

import org.qcmg.common.log.QLogger;
import org.qcmg.picard.SAMFileReaderFactory;

public class ReportCounts {
	
	ReferenceInfo[] refArray;
	QLogger logger;
	 
	/**
	 
	 * @param infoQueue
	 * @param logger
	 * @throws Exception
	 */
	ReportCounts(AbstractQueue<ReferenceInfo> infoQueue, QLogger logger) throws Exception{
				this.logger = logger;
		
		//sort instance of ReferenceInfo
		refArray = new ReferenceInfo[infoQueue.size()];
		ReferenceInfo info;
		while ((info = infoQueue.poll()) != null){
			if( info.getIndex() > refArray.length )
				throw new Exception("reference index number is greater than total number of reference!");
			
			//not sure reference index start from 0 or 1
			refArray[info.getIndex()] = info;			
		}		
	}
	
	/*
	 * report the counts into the specified output file. Here we give the counts for each base rather than counts
	 *  fallen in certain window.
	 */
	
	public void baseCountReport(File output) throws Exception {
	    
		FileWriter writer = new FileWriter(output);	
			
		//create header
	   	ReferenceInfo info; 
		for(int i = 0; i < refArray.length ; i++ ){
			info = refArray[i];
				 				
			writer.write(String.format("##SQ=<ASCII=%s,Name=%s,MaxFragSize=%s,MinFragSize=%s,AverageFragSize=%s,NumberOfFrag=%s>\n",
					info.getRefAscii(),
					info.getRefname(),
					info.getSizeOfBigWindows(),
					info.getSizeOfSmallWindows(),
					info.getSizeOfEverageWindows(),
					info.getNumberOfWindows()));
						
			logger.info(String.format("##SQ=<ASCII=%s,Name=%s,MaxFragSize=%s,MinFragSize=%s,AverageFragSize=%s,NumberOfFrag=%s>\n",
					info.getRefAscii(),
					info.getRefname(),
					info.getSizeOfBigWindows(),
					info.getSizeOfSmallWindows(),
					info.getSizeOfEverageWindows(),
					info.getNumberOfWindows()));	
		}	
		
		//create body
       writer.write("#ref_ascii\tstart\tref\ttest\n");	      
       for(int i = 0; i < refArray.length ; i++ ){
    	   info = refArray[i];
    	   String asc = info.getRefAscii();
    	   File tmp = info.getTmpFile();
    	   BufferedReader input =  new BufferedReader(new FileReader(tmp));
    	   String line = null;
    	   while((line = input.readLine()) != null){
    		   writer.write(asc + "\t" + line + "\n");
    	   }
    	   input.close();
//    	   tmp.deleteOnExit();
    	   if(tmp.delete())
    		   logger.debug("deleted tmp files:" + tmp.getName());
       }	 			
		writer.close();
	}
	  
    
	/*
	 * report the counts into the specified output file. Here we give the counts for each fixed sized window.
	 */
	public void windowCountReport(File output, int windowSize, List<SAMSequenceRecord> genome )throws Exception{
		   
       FileWriter writer = new FileWriter(output);	       
       writer.write("#Chrom\tStart\tEnd\tFormat\tCounts\n");	 
       
       for(int i = 0; i < refArray.length; i ++){
    	   ReferenceInfo info = refArray[i];   
    	   String ref = info.getRefname().replaceFirst("(?i)^chr", "");
    	   for(int j = 0; j < info.getNumberOfWindows() - 1 ;j++){
    		   writer.write(String.format("%s\t%d\t%d\t%s\t%d:%d\n", info.getRefname(), windowSize * j, windowSize * (j+1) -1, "P0:Ref", 
    				   info.getTumourCounts()[j], info.getNormalCounts()[j] ));
    	   }
    	   //add last window  
    	   int last = info.getNumberOfWindows() - 1;
    	   int lref = genome.get(info.getIndex()).getSequenceLength();
    	   writer.write(String.format("%s\t%d\t%d\t%s\t%d:%d\n", info.getRefname(), windowSize *( last), lref,"P0:Ref",
    			   info.getTumourCounts()[last], info.getNormalCounts()[last] ));
       }
       
       writer.close();           
	}
	
	
	/**
	public void windowCountReport(File output, int windowSize, List<SAMSequenceRecord> genome )throws Exception{
		   
       FileWriter writer = new FileWriter(output);	       
       writer.write("#ChromosomeArmID, StartPosition, EndPosition, TotalReadNormal, TotalReadTumor\n");	 
       
       for(int i = 0; i < refArray.length; i ++){
    	   ReferenceInfo info = refArray[i];   
    	   for(int j = 0; j < info.getNumberOfWindows() - 1 ;j++){
    		   writer.write(String.format("%s,%d,%d,%d,%d\n", info.getRefname(), windowSize * j, windowSize * (j+1) -1,
    				  info.getNormalCounts()[j], info.getTumourCounts()[j] ));
    	   }
    	   //add last window  
    	   int last = info.getNumberOfWindows() - 1;
    	   int lref = genome.get(info.getIndex()).getSequenceLength();
    	   writer.write(String.format("%s,%d,%d,%d,%d\n", info.getRefname(), windowSize *( last), lref,
    			   info.getNormalCounts()[last], info.getTumourCounts()[last])   );
       }
       
       writer.close();           
	}
	 */
	

}
