/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;

import java.io.File;
import java.util.Iterator;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;

public class ReferenceInfo {
	final private String referenceName;
	final private int referenceIndex;
	final private int referenceLenght;
	final private String referenceAscii; 
	
	final private int noWindows; 
	final private int sizeOfBigWindows;
	final private int sizeOfSmallWindows;
	final private int sizeOfAverageFragemtns;	
	
//	private long totalCountsNormal = -1;
//	private long totalCountsTumour = -1;	
	
	private long averageCountsNormal = -1;
	private long averageCountsTumour = -1;		
	
	private File tmpCountFile = null;
	private int[] normalCounts = null;
	private int[] tumourCounts = null;
//	private int noOfEverageCounts;
	
	/**
	 * 
	 * @param chr: refernce record
	 * @param noWindow: total windows number are divided in this reference
	 * @param sizeFixWindow: the fixed window size
	 */
	public ReferenceInfo(SAMSequenceRecord chr, int noWindow, int sizeFixWindow){
		this(chr.getSequenceName(), chr.getSequenceIndex(), chr.getSequenceLength(), noWindow, 
				sizeFixWindow, sizeFixWindow, sizeFixWindow );		
	}
	
	/**
	 * 
	 * @param chr: refernce record 
	 * @param noWindow: total windows number are divided in this reference
	 * @param sizeBigWindow: the biggest window size in this reference
	 * @param sizeSmallWindow : the smallest window size in this reference
	 * @param everageWindow: the average window size in this reference
	 */
	
	public ReferenceInfo(SAMSequenceRecord chr, int noWindow, int sizeBigWindow, 
			int sizeSmallWindow, int everageWindow ){
		this(chr.getSequenceName(), chr.getSequenceIndex(), chr.getSequenceLength(), noWindow, 
				sizeBigWindow, sizeSmallWindow, everageWindow );		
	}
	
	
	/**
	 * 
	 * @param sequenceName: reference name
	 * @param sequenceIndex: reference order in BAM header
	 * @param sequenceLength: reference length
	 * @param noWindow: total windows number are divided in this reference
	 * @param sizeBigWindow: the biggest window size in this reference
	 * @param sizeSmallWindow : the smallest window size in this reference
	 * @param everageWindow: the average window size in this reference
	 */
    public ReferenceInfo(String sequenceName, int sequenceIndex, int sequenceLength, int noWindow, 
    		int sizeBigWindow,  int sizeSmallWindow, int everageWindow  ) {
        this.referenceName = sequenceName;
        this.referenceIndex = sequenceIndex;
        this.referenceLenght = sequenceLength;
        //the first ascii char will be ! (33)
        this.referenceAscii = Character.toString((char) ( sequenceIndex + 33));    
        
    	noWindows = noWindow; 
    	sizeOfBigWindows = sizeBigWindow;
    	sizeOfSmallWindows = sizeSmallWindow;
    	sizeOfAverageFragemtns = everageWindow;	 

   }

   public void setNormalCounts(int[] array) { normalCounts = array;} 
   public void setTumourCounts(int[] array) { tumourCounts = array;} 
   public int[] getNormalCounts() {return normalCounts;}
   public int[] getTumourCounts() {return tumourCounts;}   
   
   public void setNormalAverage(long n){ averageCountsNormal = n; }
   public void setTumourAverage(long n){averageCountsTumour = n;}
   
   public void setTmpFile(File f){ tmpCountFile = f;}
   public File getTmpFile(){return tmpCountFile;}
   
   public String getRefname(){ return referenceName; }
   public String getRefAscii(){return referenceAscii; }
   public int getIndex(){ return referenceIndex;}
   public int getNumberOfWindows(){ return noWindows; }
   public int getSizeOfBigWindows(){ return sizeOfBigWindows; }
   public int getSizeOfSmallWindows(){ return sizeOfSmallWindows; }
   public int getSizeOfEverageWindows(){return sizeOfAverageFragemtns;}
   
   public String generalMessage(){
	   if(sizeOfBigWindows == sizeOfAverageFragemtns)
		   return String.format("%s: reported %d windows with size %d, abount (normal=%d, tumour=%d ) reads per window !", 
				   referenceName, noWindows, sizeOfAverageFragemtns, averageCountsNormal, averageCountsTumour   );
	   
	   return String.format("%s: reported %d windows with size %d ~ %d, abount (normal=%d, tumour=%d ) reads total!", 
			   referenceName, noWindows, sizeOfSmallWindows,sizeOfBigWindows, averageCountsNormal, averageCountsTumour );
   }
 
   /*
   public String vcfFormatMessage(){
	   if(sizeOfBigWindows == sizeOfEverageFragemtns)
		   return String.format("%s: reported %d windows with size %d, abount (normal=%d, tumour=%d ) reads per window !", 
				   sequenceName, noWindows, sizeOfEverageFragemtns, totalCountsNormal / noWindows, totalCountsTumour/noWindows  );
	   
	   return String.format("%s: reported %d windows with size %d ~ %d, abount (normal=%d, tumour=%d ) reads total!", 
			   sequenceName, noWindows, sizeOfSmallWindows,sizeOfBigWindows, totalCountsNormal, totalCountsTumour );
	   
   }*/
}
