/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.qcmg.common.string.StringUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.ValidationStringency;

public class ReferenceInfo {
	
	final SAMSequenceRecord ref;
	final private int windowSize;	
	final private int windowNumber;	
	HashMap<String, int[]> counts = new HashMap<String, int[]>();
//	private ArrayList<Count> counts = new ArrayList<Count>();


	/**
	 * This construction works for window mode only
	 * @param chr: refernce record
	 * @param noWindow: total windows number are divided in this reference
	 * @param sizeFixWindow: the fixed window size
	 */
	public ReferenceInfo(SAMSequenceRecord chr, int sizeFixWindow){
		ref = chr;
		windowSize = sizeFixWindow;
		
		if(chr.getSequenceLength() % windowSize == 0)
			windowNumber = chr.getSequenceLength() / windowSize;
		else
			windowNumber = chr.getSequenceLength() / windowSize + 1;
	}
	
	public void addCounts(String id, String bam, String query) throws Exception { 
		
		//initialize array to store the counts 
		int[] chrArray = new int[ref.getSequenceLength() / windowSize + 1];
		for (int i = 0; i < chrArray.length; i ++)
			chrArray[i] = 0;
		
		QueryExecutor qbamFilter = null; 
		if ( ! StringUtils.isNullOrEmpty(query) )
			qbamFilter = new QueryExecutor(query);
		
		//check each reads start point and counts it into belonging window
		SamReader rbam =   SAMFileReaderFactory.createSAMFileReader(new File(bam), ValidationStringency.SILENT);  
		SAMRecordIterator ite =	rbam.query(ref.getSequenceName(), 0, ref.getSequenceLength(),true);
		while(ite.hasNext()){
			SAMRecord record = ite.next();
			if(qbamFilter != null && !qbamFilter.Execute(record))
				continue; 
				
			int pos = record.getAlignmentStart() / windowSize ;
			chrArray[pos] ++; 
		}		
		rbam.close();	
		
		for( String key :counts.keySet()){
			if(key.contains(id))
				throw new Exception("sample id duplicated:" + id);
			if(counts.get(key).length != chrArray.length)
				throw new Exception("Algorithm error, same reference but different window number");
		}
	   
		counts.put(id, chrArray);
		
	}   

	public int[] getCount(String id){return counts.get(id);}
	public SAMSequenceRecord getReferenceRecord(){ return ref;}
	public String[] getSampleIds(){return counts.keySet().toArray(new String[counts.size()]);}
	public int getWindowNumber(){ return windowNumber; }
	public int getWindowSize(){ return windowSize; }
	
	/* 
   public void setCounts(int[][] array) { readCounts = array;}   
   public int[] getCounts() {return readCounts;}      
   public void setAverage(long n){ averageCounts = n; }   
   public long getAverage(){ return averageCounts; }
   public void setTmpFile(File f){ tmpCountFile = f;}
   public File getTmpFile(){return tmpCountFile;}   
   public String getRefname(){ return referenceName; }
   public String getRefAscii(){return referenceAscii; }
   public int getIndex(){ return referenceIndex;}
   public int getNumberOfWindows(){ return noWindows; }
   public int getSizeOfBigWindows(){ return sizeOfBigWindows; }
   public int getSizeOfSmallWindows(){ return sizeOfSmallWindows; }
   public int getSizeOfEverageWindows(){return sizeOfAverageFragemtns;}
   
   public String getMessage(){
	   if(sizeOfBigWindows == sizeOfAverageFragemtns)
		   return String.format("%s: reported %d windows with size %d, about (normal=%d, tumour=%d ) reads per window !", 
				   referenceName, noWindows, sizeOfAverageFragemtns, averageCounts, averageCounts   );
	   
	   return String.format("%s: reported %d windows with size %d ~ %d, abount (normal=%d, tumour=%d ) reads total!", 
			   referenceName, noWindows, sizeOfSmallWindows,sizeOfBigWindows, averageCounts, averageCounts );
   }
   
   public String getMessage(long[] averageCounts){
	   
	   String message;
	   
	   if(sizeOfBigWindows == sizeOfAverageFragemtns){
		   message = String.format("%s: reported %d windows with size %d, about (", 
			   referenceName, noWindows, sizeOfAverageFragemtns );
		   
		   
	   }
		   return message + 
		   
		   return String.format("%s: reported %d windows with size %d, about (normal=%d, tumour=%d ) reads per window !", 
				   referenceName, noWindows, sizeOfAverageFragemtns, averageCounts, averageCounts   );
	   
	   return String.format("%s: reported %d windows with size %d ~ %d, abount (normal=%d, tumour=%d ) reads total!", 
			   referenceName, noWindows, sizeOfSmallWindows,sizeOfBigWindows, averageCounts, averageCounts );
   }
 */
}
