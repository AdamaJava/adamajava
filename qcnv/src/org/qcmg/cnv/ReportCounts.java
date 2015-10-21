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
//	String[] sampleids;
	 
	/**	 
	 * @param infoQueue
	 * @param logger
	 * @throws Exception
	 */
	ReportCounts(AbstractQueue<ReferenceInfo> infoQueue ) throws Exception{

		//sort instance of ReferenceInfo
		refArray = new ReferenceInfo[infoQueue.size()];
		ReferenceInfo info;
		while ((info = infoQueue.poll()) != null){
			if( info.getReferenceRecord().getSequenceIndex() > refArray.length )
				throw new Exception("reference index number is greater than total number of reference!");			
			//not sure reference index start from 0 or 1
			refArray[info.getReferenceRecord().getSequenceIndex()] = info;			
		}	

	}

	/*
	 * report the counts into the specified output file. Here we give the counts for each fixed sized window.
	 */
	public void windowCountReport(File output )throws Exception{
		//create output header
		String[] sampleids = refArray[0].getSampleIds();			
		String str = "#CHROM\tID\tSTART\tEND\tFORMAT";
		for(int i = 0; i < sampleids.length; i++)
			str += "\t" + sampleids[i];		
       FileWriter writer = new FileWriter(output);	       
       writer.write(str + "\n");	 
		
       //reporting	
       int windowSize = refArray[0].getWindowSize();
       for(int i = 0; i < refArray.length; i ++){
    	   ReferenceInfo info = refArray[i];   
    //	   String ref = info.getRefname().replaceFirst("(?i)^chr", "");
    	   String ref = info.getReferenceRecord().getSequenceName();
    	   for(int j = 0; j < info.getWindowNumber() -1 ;j++){		  
    		   str = String.format("%s\t%d_%d\t%d\t%d\tDP", ref,j+1, windowSize, windowSize * j + 1, windowSize * (j+1) ); 
    		   for(String sample : sampleids)
    			   str += "\t" +info.getCount(sample)[j];
    		   writer.write(str + "\n");
    	   }
    	   
    	   //add last window  
    	   int j = info.getWindowNumber() - 1;
   		   str = String.format("%s\t%d_%d\t%d\t%d\tDP", ref,j+1, windowSize, windowSize * j + 1, info.getReferenceRecord().getSequenceLength()  ); 
		   for(String sample : sampleids)
			   str += "\t" +info.getCount(sample)[j];
		   writer.write(str + "\n");    	   
       }
       
       writer.close();           
	}

}
