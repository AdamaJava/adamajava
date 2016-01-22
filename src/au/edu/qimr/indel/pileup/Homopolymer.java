/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.indel.pileup;

import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.IndelUtils.SVTYPE;

import au.edu.qimr.indel.Q3IndelException;

public class Homopolymer {
	
	static final String nullValue = "-";
	private final List<String> motifs ; //same chrposition but differnt allel
	private final ChrPosition position;
	
	private final SVTYPE indelType; 	
	QLogger logger = QLoggerFactory.getLogger(Homopolymer.class);	
	private byte[] upstreamReference;
	private byte[] downstreamReference;
//	private List<byte[]> indelReferenceBases = new ArrayList<byte[]>() ;
	private int homopolymerWindow;
	private int reportWindow;

//	private List<String> upBase = new ArrayList<String>();
//	private List<String> downBase = new ArrayList<String>();
	private List<Integer> maxBase = new ArrayList<Integer>();
	private List<byte[]> homoString = new ArrayList<byte[]>();
	private byte[] referenceBase;

	
	public Homopolymer(IndelPosition position, final byte[] referenceBase, int homopolymerWindow, int reportWindow) {
		this.position = position.getChrPosition();
		this.indelType = position.getIndelType();
		this.motifs = position.getMotifs();
		
		this.referenceBase = referenceBase; 
		this.homopolymerWindow = homopolymerWindow;
		this.reportWindow = reportWindow; 
		getReferenceBase();
		
		//init
		for( int i = 0 ; i < position.getMotifs().size(); i ++ ){
//			upBase.add(nullValue);
//			downBase.add(nullValue);
			homoString.add(null);	
			maxBase.add(0);
		}
		
		for( int i = 0 ; i < position.getMotifs().size(); i ++ )
			findHomopolymer(i);
	}
 
 	
	public String getPolymerSequence(int index){
			
		return  homoString.get(index) == null ? null : new String(   homoString.get(index));				 
	}
		
	private byte[] setSequence(String motif) {	
		
//		byte[] seq = new byte[upstreamReference.length + downstreamReference.length + motif.length() ]; 
		
		//at the edge of reference, the report window maybe bigger than nearbybase
		int baseNo1 = Math.min(upstreamReference.length, reportWindow) ;	
		int baseNo2 = Math.min(downstreamReference.length, reportWindow) ;				
		byte[] seq = new byte[baseNo1 + motif.length() + baseNo2]; 
		System.arraycopy(upstreamReference, (upstreamReference.length - baseNo1), seq, 0, baseNo1);  	 		
		System.arraycopy(downstreamReference, 0, seq, baseNo1 + motif.length(), baseNo2); 
		
//		int pos = upstreamReference.length;
//		System.arraycopy(downstreamReference, 0, seq, reportWindow + motif.length(), reportWindow); 
				
		if (indelType.equals(SVTYPE.DEL))				 
			for (int i=0; i<motif.length(); i++)
				seq[baseNo1 + i] = '_';
		else  			
			System.arraycopy(motif.toLowerCase().getBytes(), 0, seq, baseNo1 , motif.length());  

		return seq; 
	}

	public void  findHomopolymer( int index) {
		
		int upBaseCount = 1;
		int downBaseCount = 1;
 		//upstream - start from end since this is the side adjacent to the indel
		//decide if it is contiguous		
		int finalUpIndex = upstreamReference.length-1;	
		
		//count upstream homopolymer bases
		char nearBase = (char) upstreamReference[finalUpIndex];
		for (int i=finalUpIndex-1; i>=0; i--) {
			if (nearBase == upstreamReference[i]) {
				upBaseCount++;
			} else {
				break;
			}
		}
		
//		if(upBaseCount > 1)
//			upBase.set(index, upBaseCount + "" + nearBase );
		
		//count downstream homopolymer
		nearBase = (char) downstreamReference[0];
		for (int i=1; i<downstreamReference.length; i++) {
			if (nearBase == downstreamReference[i]) {
				downBaseCount++;
			} else {
				break;
			}
		}
		
//		if(downBaseCount > 1)
//			downBase.set(index, downBaseCount + "" + nearBase );
		
		int max  = 0;
		//reset up or down stream for deletion reference base
		if(indelType.equals(SVTYPE.DEL)){
			
			byte[] mByte = motifs.get(0).getBytes(); 			
			int left = upBaseCount;
			nearBase = (char) upstreamReference[finalUpIndex];			
			for(int i = 0; i < mByte.length; i ++ ){
				if (nearBase == mByte[i]) {
					left ++;
				} else {
					break;
				}
			}
			
			int right = downBaseCount;
			nearBase = (char) downstreamReference[0];
			for(int i = mByte.length -1; i >=0; i--){
				if (nearBase == mByte[i]) {
					right++;
				} else {
					break;
				}				
			}
			
			max = Math.max(left, right);
			
			//deletion is homopolymer
			if((left - upBaseCount) == (right - downBaseCount)) 
				max = left + right - mByte.length;
			 			
		}else{
		    //INS don't have reference base
			max = (upstreamReference[finalUpIndex] == downstreamReference[0] )? 
					(downBaseCount + upBaseCount) : Math.max(downBaseCount, upBaseCount);
		}
					
		if(max > 1){
			maxBase.set(index, max);
			homoString.set(index, setSequence(motifs.get(index))); 
		}
					
	}
	
//	public String getUpBaseCount(int index){ return upBase.get(index); }
//	public String getDownBaseCount(int index){ return downBase.get(index); }
	public int getCount(int index){return maxBase.get(index); }
	
	public synchronized void getReferenceBase() { 	

		int MaxEnd = referenceBase.length;
//		indelReferenceBases = new ArrayList<byte[]>() ;
	
		//eg. INS: 21 T TC or DEL: 21  TCC T
		//both T  position.getPosition() is 21 but should be  referenceBase[20] which is end of upStream
		//INS position.getEndPosition() is 21, downStream should start at referenceBase[21]
		//DEL position.getEndPosition() is 23, downStream should start at referenceBase[23]
		
		int indelStart = position.getPosition();
	    int indelEnd = position.getEndPosition(); 
	    
    	//at least start from position 1 
    	int wstart = Math.max( 0,indelStart-homopolymerWindow); 	
  	    upstreamReference = new byte[indelStart - wstart ]; 
	    System.arraycopy(referenceBase, wstart, upstreamReference, 0, upstreamReference.length);
		
	    int wend = Math.min(MaxEnd, indelEnd + homopolymerWindow);  
     	downstreamReference = new byte[wend - indelEnd ];     	 	
     	System.arraycopy(referenceBase, indelEnd, downstreamReference, 0, downstreamReference.length);    	
	}
	

	public ChrPosition getChrPosition(){return position; }
		

	public static FastaSequenceIndex getFastaIndex(File reference) {
		File indexFile = new File(reference.getAbsolutePath() + ".fai");	    		
		return new FastaSequenceIndex(indexFile);
	}
	
	public static IndexedFastaSequenceFile getIndexedFastaFile(File reference) {
		FastaSequenceIndex index = getFastaIndex(reference);		
		IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(reference, index);
		
		return indexedFasta;
	}
	
	
	public static String getSequenceString(byte[] sequence) {
		StringBuilder sb = new StringBuilder();
		for (byte b: sequence) {
			sb.append((char) b);
		}
		return sb.toString();
	}

}

