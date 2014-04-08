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

public class BaseCount  implements Count{
	final SAMFileReader rtest;
	final SAMFileReader rref;
	final SAMSequenceRecord chr;	
	final File tmpDir;
	
	
	/**
	 * Initialize 
	 * @param normal: normal BAM file
	 * @param tumor: tumour BAM file
	 * @param chr: reads mapped on this reference will be counted 
	 * @throws Exception
	 */
	
	BaseCount(File normal, File tumor, SAMSequenceRecord chr, File tmpDir) throws Exception {	
		rtest = SAMFileReaderFactory.createSAMFileReader(tumor, SAMFileReader.ValidationStringency.SILENT);  
		rref = SAMFileReaderFactory.createSAMFileReader(normal, SAMFileReader.ValidationStringency.SILENT); 				
		this.chr = chr;	
		this.tmpDir = tmpDir;
//		tmpOutput = File.createTempFile(chr.getSequenceName() + ".", ".count", tmpDir);	
		 
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
	
	/**
	 * 
	 * output counts into tmp files
	 */
	private ReferenceInfo reporting(int[] normal, int[] tumour) throws Exception{
		long allWindow = 0;
		int numWindows = 1; //can't be 0, it cause exception
		int maxWindow = 0;
		int minWindow = chr.getSequenceLength();
	
		int start = 0;
		int end = 0;
		long Ntotal = 0;
		long Ttotal = 0;
		//can't be appended
		File tmpOutput = File.createTempFile(chr.getSequenceName() + ".", ".count", tmpDir);	
		FileWriter writer = new FileWriter(tmpOutput,false); 
		for (int i = 1; i < chr.getSequenceLength();  i ++){	
			//summary total counts for calculation coverage late
			Ttotal += tumour[i];
			Ntotal += normal[i];
			
			if((tumour[i] == tumour[i-1]) && (normal [i] == normal[i-1])){
				end = i;
			}else{					
				end = i-1;	
				writer.write(String.format("%d\t%d\t%d\n", start, normal[i-1], tumour[i-1]));										
				//only summary non zero region
				if(tumour [i-1] > 0 && normal [i-1] > 0){
					numWindows ++;
					allWindow += end - start + 1;
					maxWindow = Math.max(maxWindow, (end-start + 1));
					minWindow = Math.min(minWindow, (end-start + 1));
				}
				start = i;
			}
		} 
		
		//output last region counts
		end = chr.getSequenceLength() - 1;
		writer.write(String.format("%d\t%d\t%d\n", start, normal[end],tumour[end]));			
		if(tumour[end] > 0 && normal[end] > 0){
			numWindows ++;
			allWindow += end - start + 1;
			maxWindow = Math.max(maxWindow, (end-start + 1));
			minWindow = Math.min(minWindow, (end-start + 1));
		}
		writer.close();	
		
		int index = rtest.getFileHeader().getSequenceIndex(chr.getSequenceName());				
		ReferenceInfo refInfo = new ReferenceInfo(chr,numWindows, maxWindow, minWindow,  (int) (allWindow/numWindows) );
		refInfo.setTmpFile(tmpOutput);
		
		return refInfo;		
	}
	
	
	/**
	 * store counts on each base into array. 
	 * It may cause memory if max ref length is over Integer.MAX_VALUE 
	 */
	
	 int[] counting(SAMFileReader reader) throws Exception{
		 
		 //debug
		// System.out.println("###############");
		
		//initialize array to store the counts 
		int[] chrArray = new int[chr.getSequenceLength()];
		for (int i = 0; i < chr.getSequenceLength(); i ++)
			chrArray[i] = 0;				
		
		//count on each base
		SAMRecordIterator ite =	reader.query(chr.getSequenceName(), 1, chr.getSequenceLength(),false);
		int start, end;
		while(ite.hasNext()){
			SAMRecord record = ite.next();
			start = record.getAlignmentStart();
			end = record.getAlignmentEnd();
			for (int j = start; j <= end; j++)
				chrArray[j] ++;
			
			//debug
			//System.out.println("record: " + start + " , " + end);
		}
		
		return chrArray;

	}

}
