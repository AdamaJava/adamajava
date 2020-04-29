/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.indel.pileup;

import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;

import java.lang.IllegalStateException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.IndelUtils.SVTYPE;

@Deprecated
public class Homopolymer {
	
	static final String nullValue = "-";
	private final List<String> motifs ; //same chrposition but differnt allel
	private final ChrRangePosition position;
	
	private final SVTYPE indelType; 	
	QLogger logger = QLoggerFactory.getLogger(Homopolymer.class);	
	private byte[] upstreamReference;
	private byte[] downstreamReference;
	private final int homopolymerWindow;
	private final int reportWindow;

	private final List<Integer> maxBase = new ArrayList<>();
	private final List<byte[]> homoString = new ArrayList<>();
	private final byte[] referenceBase;
	
	public Homopolymer(IndelPosition position, final byte[] referenceBase, int homopolymerWindow, int reportWindow) {
		this.position =   position.getChrRangePosition();
		this.indelType = position.getIndelType();
		this.motifs = position.getMotifs();
		
		this.referenceBase = referenceBase; 
		this.homopolymerWindow = homopolymerWindow;
		this.reportWindow = reportWindow; 
		getReferenceBase();
		
		//init
		for( int i = 0 ; i < position.getMotifs().size(); i ++ ){
			homoString.add(null);	
			maxBase.add(0);
		}
		
		for( int i = 0 ; i < position.getMotifs().size(); i ++ )
			findHomopolymer(i);
	}
 
 	
	public String getPolymerSequence(int index){
		try {
			return  homoString.get(index) == null ? null : new String(homoString.get(index), StandardCharsets.UTF_8.name());				 
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("StandardCharsets.UTF_8 should be supported");
		}
	}
		
	private byte[] setSequence(String motif) {	
		
		//at the edge of reference, the report window maybe bigger than nearbybase
		int baseNo1 = Math.min(upstreamReference.length, reportWindow) ;	
		int baseNo2 = Math.min(downstreamReference.length, reportWindow) ;				
		byte[] seq = new byte[baseNo1 + motif.length() + baseNo2]; 
		System.arraycopy(upstreamReference, (upstreamReference.length - baseNo1), seq, 0, baseNo1);  	 		
		System.arraycopy(downstreamReference, 0, seq, baseNo1 + motif.length(), baseNo2); 
		
		if (indelType.equals(SVTYPE.DEL))				 
			for (int i=0; i<motif.length(); i++)
				seq[baseNo1 + i] = '_';
		else  			
			try {
				System.arraycopy(motif.toLowerCase().getBytes(StandardCharsets.UTF_8.name()), 0, seq, baseNo1 , motif.length());  
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException("StandardCharsets.UTF_8 should be supported");
			}

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
		
		//count downstream homopolymer
		nearBase = (char) downstreamReference[0];
		for (int i=1; i<downstreamReference.length; i++) {
			if (nearBase == downstreamReference[i]) {
				downBaseCount++;
			} else {
				break;
			}
		}
		
		int max  = 0;
		//reset up or down stream for deletion reference base
		if(indelType.equals(SVTYPE.DEL)){			
			byte[] mByte;
			try {
				mByte = motifs.get(0).getBytes(StandardCharsets.UTF_8.name()); 	
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException("StandardCharsets.UTF_8 should be supported");
			}
			
			int left = 0;
			nearBase = (char) upstreamReference[finalUpIndex];			
			for(int i = 0; i < mByte.length; i ++ ) 
				if (nearBase == mByte[i])  left ++;
				else  break;				 
			upBaseCount += left; 
						
			int right = 0;
			nearBase = (char) downstreamReference[0];
			for(int i = mByte.length -1; i >=0; i--) 
				if (nearBase == mByte[i]) right++;
				else break;
			downBaseCount += right; 
			
			max = (left == right && left == mByte.length)? 
					(downBaseCount + upBaseCount - mByte.length) : Math.max(downBaseCount, upBaseCount);
						 			
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
	
	public int getCount(int index){return maxBase.get(index); }
	
	public synchronized void getReferenceBase() { 	

		int maxEnd = referenceBase.length;
	
		//eg. INS: 21 T TC or DEL: 21  TCC T
		//both T  position.getPosition() is 21 but should be  referenceBase[20] which is end of upStream
		//INS position.getEndPosition() is 21, downStream should start at referenceBase[21]
		//DEL position.getEndPosition() is 23, downStream should start at referenceBase[23]
		
		int indelStart = position.getStartPosition();
	    int indelEnd = position.getEndPosition(); 
	    
	    	//at least start from position 1 
	    	int wstart = Math.max( 0,indelStart-homopolymerWindow); 	
  	    upstreamReference = new byte[indelStart - wstart ]; 
	    System.arraycopy(referenceBase, wstart, upstreamReference, 0, upstreamReference.length);
		
	    int wend = Math.min(maxEnd, indelEnd + homopolymerWindow);  
     	downstreamReference = new byte[wend - indelEnd ];     	 	
     	System.arraycopy(referenceBase, indelEnd, downstreamReference, 0, downstreamReference.length);    	
	}
	

	public static FastaSequenceIndex getFastaIndex(File reference) {
		File indexFile = new File(reference.getAbsolutePath() + ".fai");	    		
		return new FastaSequenceIndex(indexFile);
	}
	
	public static IndexedFastaSequenceFile getIndexedFastaFile(File reference) {
		FastaSequenceIndex index = getFastaIndex(reference);		
		IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(reference, index);
		
		return indexedFasta;
	}

}

