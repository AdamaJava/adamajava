/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.indel.pileup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.IndelUtils.SVTYPE;

import au.edu.qimr.indel.Q3PileupException;
import net.sf.picard.reference.FastaSequenceIndex;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;

public class Homopolymer {
	
	public enum HOMOTYPE {HOMADJ, HOMCON,HOMEMB,NONE }
	private final List<String> motifs ; //same chrposition but differnt allel
//	private  final int indelStart, indelEnd; 
//	private final String fullChromosome;
	private final ChrPosition position;
	
	private final SVTYPE indelType; 
	
	QLogger logger = QLoggerFactory.getLogger(Homopolymer.class);
//    private IndelPosition position;
	private List<String> sequence ;
	
	private byte[] upstreamReference;
	private byte[] downstreamReference;
	private List<byte[]> indelReferenceBases;
//	private Lbyte[] delReferenceBases;
	private int homopolymerWindow;

	private List<HOMOTYPE> homoType = new ArrayList<HOMOTYPE>();
	private List<Integer> homopolymerCount = new ArrayList<Integer>();
	private List<String> homoString = new ArrayList<String>();


//	private IndexedFastaSequenceFile indexedFasta;
//	private File referenceFile; 
	private byte[] referenceBase;

//	public Homopolymer(IndelPosition position, File reference, int homopolymerWindow) {
//		this.position = position.getChrPosition();
//		this.indelType = position.getIndelType();
//		this.motifs = position.getMotifs();
//		
//		this.referenceFile = reference; 
//		this.homopolymerWindow = homopolymerWindow;
//		getReferenceSequence();
//		
//		for(int i = 0 ; i < position.getMotifs().size(); i ++)
//			findHomopolymer(i);
//	}
	
	
	public Homopolymer(IndelPosition position, final byte[] referenceBase, int homopolymerWindow) {
		this.position = position.getChrPosition();
		this.indelType = position.getIndelType();
		this.motifs = position.getMotifs();
		
		this.referenceBase = referenceBase; 
		this.homopolymerWindow = homopolymerWindow;
		getReferenceBase();
		
		for(int i = 0 ; i < position.getMotifs().size(); i ++)
			findHomopolymer(i);
	}
 
	public HOMOTYPE getHOMOTYPE(int index){
		return homoType.get(index);
	}
	
	public int getBaseCount(int index){
		return homopolymerCount.get(index);
	}
	
	public String getPolymerSequence(int index){
		return homoString.get(index);
	}
	
	
	private void setSequence(int index) {
		if(homoType.get(index).equals(HOMOTYPE.NONE)){
			homoString.add(index, null);
			return;
		}
		
		String motif = motifs.get(index);
		
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<upstreamReference.length; i++) {
			sb.append((Character.toString((char) upstreamReference[i])));
		}
		
		if (indelType.equals(SVTYPE.DEL))				 
			for (int i=0; i<motif.length(); i++)
				sb.append("_");
		else  			
			sb.append(motif.toLowerCase());
			 	 			
 		for (int i=0; i<downstreamReference.length; i++) {
			sb.append((Character.toString((char) downstreamReference[i])));
		}
			
 		homoString.add(index, sb.toString());
	}

 
	
	
//	public void  getContiguousBasesCount(int index) {
	public void  findHomopolymer( int index) {
		
		HOMOTYPE upType = HOMOTYPE.NONE;
		HOMOTYPE downType = HOMOTYPE.NONE;
		int upBaseCount = 1;
		int downBaseCount = 1;
 		//upstream - start from end since this is the side adjacent to the indel
		//decide if it is contiguous		
		int finalUpIndex = upstreamReference.length-1;	
		
		//count upstream homopolymer bases
		char previousBase = (char) upstreamReference[finalUpIndex];
		for (int i=finalUpIndex-1; i>=0; i--) {
			if (previousBase == upstreamReference[i]) {
				upBaseCount++;
			} else {
				break;
			}
		}
		
		byte[] indelRefBases = indelReferenceBases.get(index);
		
		//if homopolymer run present, work out if it is contiguous
		if (upBaseCount > 1) {
			if (upstreamReference[finalUpIndex] == indelRefBases[0]) {
				upType = HOMOTYPE.HOMCON; // CONTIGUOUS
			} else {
				upType = HOMOTYPE.HOMADJ;  //DISCONTIGUOUS;
			}
		}
		
		//count downstream homopolymer
		char previousDownBase = (char) downstreamReference[0];
		for (int i=1; i<downstreamReference.length; i++) {
			if (previousDownBase == downstreamReference[i]) {
				downBaseCount++;
			} else {
				break;
			}
		}
		
		//if homopolymer run present, work out if it is contiguous
		if (downBaseCount > 1) {
			if (downstreamReference[0] == indelRefBases[indelRefBases.length-1]) {
				downType = HOMOTYPE.HOMCON; //CONTIGUOUS;
			} else {
				downType = HOMOTYPE.HOMADJ; //DISCONTIGUOUS;
			}
		}
		
		//see if it is embedded, set counts
		if (! isEmbeddedHomopolymer(index, finalUpIndex, upType, downType, upBaseCount, downBaseCount)) {
			//if they are both contiguous, see which one is higher, set counts
			checkHomopolymer( index, upType, downType, upBaseCount, downBaseCount);
			
		}		
		
		//set ffs sequence
		setSequence(index);

	 
	}
	
	/**
	 * check and set indel homopolymers type and base counts
	 * @param index
	 * @param upType
	 * @param downType
	 * @param upBaseCount
	 * @param downBaseCount
	 */
	public void checkHomopolymer(int index, HOMOTYPE upType, HOMOTYPE downType, int upBaseCount, int downBaseCount) {		
		if(upType.equals(HOMOTYPE.NONE) && downType.equals(HOMOTYPE.NONE)){
			homoType.add(index, HOMOTYPE.NONE);
			homopolymerCount.add(index, 0);			 
		}else if(upType.equals(HOMOTYPE.HOMCON) && downType.equals(HOMOTYPE.HOMCON)){
			homoType.add(index, HOMOTYPE.HOMCON);
			homopolymerCount.add(index, Math.max(upBaseCount, downBaseCount)); 			 
		}else if(upType.equals(HOMOTYPE.HOMADJ) && downType.equals(HOMOTYPE.HOMADJ)){
			homoType.add(index, HOMOTYPE.HOMADJ);
			homopolymerCount.add(index, Math.max(upBaseCount, downBaseCount)); 			 
		} else if(downType.equals(HOMOTYPE.NONE) || upType.equals(HOMOTYPE.HOMADJ)){
			homoType.add(index, upType);
			homopolymerCount.add(index, upBaseCount); 			 
		} else {     //remaining upType is non or downType is HOMADJ
			homoType.add(index, downType);
			homopolymerCount.add(index, downBaseCount); 			 
		}
					 
	}

	/**
	 * check and set indel homopolymers type and base counts if embeded
	 * @param motifIndex: index number of current motif in current indel position
	 * @param finalUpIndex
	 * @param upType
	 * @param downType
	 * @param upBaseCount
	 * @param downBaseCount
	 * @return true if current indel motif is embeded
	 */
	public boolean isEmbeddedHomopolymer(int motifIndex, int finalUpIndex, HOMOTYPE upType, HOMOTYPE downType, int upBaseCount, int downBaseCount) {
		byte[] indelRefBases = indelReferenceBases.get(motifIndex);
		if (upstreamReference[finalUpIndex] == downstreamReference[0] && upType.equals(HOMOTYPE.HOMCON) && downType.equals(HOMOTYPE.HOMCON)) {
			//embedded
			char base = (char) upstreamReference[finalUpIndex];
			boolean match = true;
			for (int i=0; i< indelRefBases.length; i++) {
				if ((char) indelRefBases[i] != base) {
					match = false;
					break;
				}
			}
			if (match) {
				homoType.add(motifIndex, HOMOTYPE.HOMEMB); // EMBEDDED;				
				homopolymerCount.add(motifIndex,  upBaseCount + downBaseCount);
				return true;
			}
		}
		return false;
	}
	
	public synchronized void getReferenceBase() {	
		final int indelStart = position.getPosition();
		final int  indelEnd = position.getEndPosition(); 
//		final String fullChromosome = position.getChromosome();

		int MaxEnd = referenceBase.length;
		indelReferenceBases = new ArrayList<byte[]>() ;
				
    	if (indelType.equals(SVTYPE.DEL)) {   
    		int wstart = Math.max(1,indelStart-homopolymerWindow); 
    		upstreamReference = new byte[indelStart - wstart];
    		System.arraycopy(referenceBase, wstart, upstreamReference, 0, upstreamReference.length);
    		
    		byte[] base = new byte[indelEnd - indelStart + 1];
    		System.arraycopy(referenceBase, indelStart, base, 0, base.length);
    		indelReferenceBases.add(0, base);
    		
    		int wend = Math.min(MaxEnd, indelEnd+homopolymerWindow);    			
    		downstreamReference = new byte[wend - indelEnd];
    		System.arraycopy(referenceBase, indelEnd + 1, downstreamReference, 0, downstreamReference.length);    		
     			 
    	} else if (indelType.equals(SVTYPE.INS)) {   
    		int wstart = Math.max(1,indelStart-homopolymerWindow + 1);    		
    		upstreamReference = new byte[indelStart - wstart + 1];
    		System.arraycopy(referenceBase, wstart, upstreamReference, 0, upstreamReference.length);
    		   		
    		for(int i = 0; i < motifs.size(); i ++)
    				this.indelReferenceBases.add(i, motifs.get(i).getBytes());
    		    		
    		int wend = Math.min(MaxEnd, indelEnd+homopolymerWindow -1);  
    		downstreamReference = new byte[wend - indelEnd + 1];
    		System.arraycopy(referenceBase, indelEnd, downstreamReference, 0, downstreamReference.length);    		   			
    	}

	}
	
	
	
//	@Deprecated
//	public synchronized boolean getReferenceSequence() {	
//		final int indelStart = position.getPosition();
//		final int  indelEnd = position.getEndPosition(); 
//		final String fullChromosome = position.getChromosome();
//
//		
//		try(IndexedFastaSequenceFile indexedFasta = getIndexedFastaFile(referenceFile)) {		  		
//			int MaxEnd = indexedFasta.getSequence(fullChromosome).length();
//			indelReferenceBases = new ArrayList<byte[]>() ;
//    		if (indelType.equals(SVTYPE.DEL)) {    				
//    			this.upstreamReference = indexedFasta.getSubsequenceAt(fullChromosome, 
//    					Math.max(1,indelStart-homopolymerWindow), indelStart-1).getBases();        		
//    			this.indelReferenceBases.add(
//    					indexedFasta.getSubsequenceAt(fullChromosome, indelStart, indelEnd).getBases());
//    			this.downstreamReference = indexedFasta.getSubsequenceAt(fullChromosome, indelEnd+1, 
//    					Math.min(MaxEnd, indelEnd+homopolymerWindow)).getBases();
//    		} else {
//    			this.upstreamReference = indexedFasta.getSubsequenceAt(fullChromosome, 
//    					Math.max(1,indelStart-homopolymerWindow+1), indelStart).getBases();	
// //   			this.indelReferenceBases = position.getMotif().getBytes();
//    			this.downstreamReference = indexedFasta.getSubsequenceAt(fullChromosome, indelEnd, 
//    					Math.min(MaxEnd, indelEnd+(homopolymerWindow-1)) ).getBases(); 
//    			
//    			for(String motif : motifs)
//    				this.indelReferenceBases.add(motif.getBytes());
//    		}
//		
//		} catch (Exception e) {
//			
//	        logger.info("Error retrieving reference bases for: " + fullChromosome + " [" + indelStart + "," + indelEnd + "]");
//	        logger.info("Error: " + Q3PileupException.getStrackTrace(e));
//		    return false;
//		}
//		
//		return true;		
//	}
	
	public HOMOTYPE getType(int index) {
		return homoType.get(index);
	}

	public int getHomopolymerCount(int index) {
		return homopolymerCount.get(index);
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

