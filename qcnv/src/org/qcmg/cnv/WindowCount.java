/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
//import java.util.ArrayList;
import java.util.*;


import org.qcmg.common.log.QLogger;
import org.qcmg.picard.SAMFileReaderFactory;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;

public class WindowCount implements Count{
	SAMFileReader rtest;
	SAMFileReader rref;
	SAMSequenceRecord chr;
	int windowSize;
	
	/**
	 * Initialize 
	 * @param normal: normal BAM file
	 * @param tumor: tumour BAM file
	 * @param chr: reads mapped on this reference will be counted 
	 * @throws Exception
	 */
	WindowCount(File normal, File tumor, SAMSequenceRecord chr, int windowSize) throws Exception {		 
		rtest = SAMFileReaderFactory.createSAMFileReader(tumor, SAMFileReader.ValidationStringency.SILENT);  
		rref = SAMFileReaderFactory.createSAMFileReader(normal, SAMFileReader.ValidationStringency.SILENT); 				
		this.chr = chr;		
		this.windowSize = windowSize;
	}
	
	/**
	 * 
	 * it count the reads which mapped on each window region, and store related information into SingleRefInfo 
	 * @return the counts information on this reference (SinglerefInfo)
	 * @throws Exception
	 */
	public ReferenceInfo execute() throws Exception{

			int[] normalCount = counting(rref );
			int[] tumorCount = counting(rtest );
			if(tumorCount.length != normalCount.length)
				throw new Exception("the reference length are difference in two inputs: " + chr.getSequenceName());
		
			 ReferenceInfo refInfo = reporting(normalCount,tumorCount );	 
			 
			 return refInfo;				 
	}
	
	ReferenceInfo reporting(int[] normal, int[] tumour){
		
		ReferenceInfo refInfo = new ReferenceInfo(chr,normal.length,windowSize);
		refInfo.setNormalCounts(normal);
		refInfo.setTumourCounts(tumour);
		
		long total = 0;
		for(int i = 0; i < normal.length;i++) total += normal[i];
		refInfo.setNormalAverage(total/normal.length );
		
		total = 0;
		for(int i = 0; i < tumour.length;i++) total += tumour[i];
		refInfo.setTumourAverage(total/tumour.length);	
		
		return refInfo;		
	}
	
	int[] counting(SAMFileReader reader) throws Exception{
		 
		//initialize array to store the counts 
		int[] chrArray = new int[chr.getSequenceLength() / windowSize + 1];
		for (int i = 0; i < chrArray.length; i ++)
			chrArray[i] = 0;
		
		//check each reads start point and counts it into belonging window
		SAMRecordIterator ite =	reader.query(chr.getSequenceName(), 0, chr.getSequenceLength(),true);
		while(ite.hasNext()){
			SAMRecord record = ite.next();
			int pos = record.getAlignmentStart() / windowSize ;
			chrArray[pos] ++; 
		}		
		
    	return chrArray;
	}

}
