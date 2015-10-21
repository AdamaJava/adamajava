/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.indel;

import net.sf.picard.reference.IndexedFastaSequenceFile;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qbasepileup.QBasePileupUtil;


public class Homopolymer {
	
	public static final String CONTIGUOUS = "contiguous";
	public static final String DISCONTIGUOUS = "discontiguous";
	public static final String EMBEDDED = "embedded";
	
	QLogger logger = QLoggerFactory.getLogger(Homopolymer.class);
    private IndelPosition position;
	private String type = "";
	private String homopolymerCount = "";
	private String sequence = "";
	private byte[] upstreamReference;
	private byte[] downstreamReference;
	private byte[] indelReferenceBases;
	private int homopolymerWindow;
	private String upType = "";
	private String downType = "";
	private int upBaseCount = 0;
	private int downBaseCount = 0;
	private IndexedFastaSequenceFile indexedFasta;
 
	public Homopolymer() {
		// TODO Auto-generated constructor stub
	}
	
	public Homopolymer(IndelPosition position, IndexedFastaSequenceFile indexedFasta, int homopolymerWindow) {
		super();
		this.position = position;
		this.indexedFasta = indexedFasta;
		this.homopolymerWindow = homopolymerWindow;
		findHomopolymer();
	}

	public void findHomopolymer() {
		
		//don't find homopolymer for complex
		if ( ! position.isComplex()) {	
			
		        boolean retrieved = getReferenceSequence();
			if (retrieved) {
				countContiguousBases();
				if ( ! type.equals("")) {
					getSequence();
				}	
			}				
		}		
	}

	public String getSequence() {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<upstreamReference.length; i++) {
			sb.append((Character.toString((char) upstreamReference[i])));
		}
		for (int i=0; i<indelReferenceBases.length; i++) {
			if (position.isDeletion()) {
				sb.append("_");
			} else {				
				sb.append(Character.toString((char)indelReferenceBases[i]).toLowerCase());
			}
			
		}
		for (int i=0; i<downstreamReference.length; i++) {
			sb.append((Character.toString((char) downstreamReference[i])));
		}
		
		this.sequence = sb.toString();	
		return this.sequence;
	}

	public void countContiguousBases() {
		
		upType = "";
		downType = "";
		upBaseCount = 1;
		downBaseCount = 1;	
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
		
		//if homopolymer run present, work out if it is contiguous
		if (upBaseCount > 1) {
			if (upstreamReference[finalUpIndex] == indelReferenceBases[0]) {
				upType = CONTIGUOUS;
			} else {
				upType = DISCONTIGUOUS;
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
			if (downstreamReference[0] == indelReferenceBases[indelReferenceBases.length-1]) {
				downType = CONTIGUOUS;
			} else {
				downType = DISCONTIGUOUS;
			}
		}
		
		//see if it is embedded
		if (isEmbeddedHomopolymer(finalUpIndex, upType, downType, upBaseCount, downBaseCount)) {
			return;
		}		

		//if they are both contiguous, see which one is higher
		if (!isHomopolymer(CONTIGUOUS, upType, downType, upBaseCount, downBaseCount) &&
				!isHomopolymer(DISCONTIGUOUS, upType, downType, upBaseCount, downBaseCount)) {
			type = "";
			homopolymerCount = "";
		}		
	}

	public String getUpType() {
		return upType;
	}

	public String getDownType() {
		return downType;
	}

	public int getUpBaseCount() {
		return upBaseCount;
	}

	public int getDownBaseCount() {
		return downBaseCount;
	}

	public boolean isEmbeddedHomopolymer(int finalUpIndex, String upType, String downType, int upBaseCount, int downBaseCount) {
		if (upstreamReference[finalUpIndex] == downstreamReference[0] && upType.equals(CONTIGUOUS) && downType.equals(CONTIGUOUS)) {
			//embedded
			char base = (char) upstreamReference[finalUpIndex];
			boolean match = true;
			for (int i=0; i< indelReferenceBases.length; i++) {
				if ((char) indelReferenceBases[i] != base) {
					match = false;
				}
			}
			if (match) {
				type = EMBEDDED;
				homopolymerCount = Integer.toString(upBaseCount + downBaseCount);
				return true;
			}
		}
		return false;
	}

	public boolean isHomopolymer(String typeToCheck, String upType, String downType, int upBaseCount, int downBaseCount) {		
		if ((upType.equals(typeToCheck) || downType.equals(typeToCheck))) {
			type = typeToCheck;
			//set length of the homopolymer
			if (upType.equals(typeToCheck) && downType.equals(typeToCheck)) {				
				if (upBaseCount > downBaseCount) {					
					homopolymerCount = Integer.toString(upBaseCount);
				} else {						
					homopolymerCount = Integer.toString(downBaseCount);
				}				
			} else if (upType.equals(typeToCheck) && upBaseCount > 1) {
				homopolymerCount = Integer.toString(upBaseCount);
			} else if (downType.equals(typeToCheck) && downBaseCount > 1) {
				homopolymerCount = Integer.toString(downBaseCount);
			}	
			return true;
		}
		return false;		
	}

	public IndelPosition getPosition() {
		return position;
	}

	public byte[] getUpstreamReference() {
		return upstreamReference;
	}

	public void setUpstreamReference(byte[] upstreamReference) {
		this.upstreamReference = upstreamReference;
	}

	public byte[] getDownstreamReference() {
		return downstreamReference;
	}

	public void setDownstreamReference(byte[] downstreamReference) {
		this.downstreamReference = downstreamReference;
	}

	public byte[] getIndelReferenceBases() {
		return indelReferenceBases;
	}

	public void setIndelReferenceBases(byte[] indelReferenceBases) {
		this.indelReferenceBases = indelReferenceBases;
	}

	public int getHomopolymerWindow() {
		return homopolymerWindow;
	}

	public synchronized boolean 
	getReferenceSequence() {		
		try {		
   		
    		//where are the start and end really???
    		if (position.isDeletion()) {
    				
    			this.upstreamReference = indexedFasta.getSubsequenceAt(position.getFullChromosome(), position.getStart()-homopolymerWindow, position.getStart()-1).getBases();        		
    			this.indelReferenceBases = indexedFasta.getSubsequenceAt(position.getFullChromosome(), position.getStart(), position.getEnd()).getBases();
    			this.downstreamReference = indexedFasta.getSubsequenceAt(position.getFullChromosome(), position.getEnd()+1, position.getEnd()+homopolymerWindow).getBases();
    		} else {
    			this.upstreamReference = indexedFasta.getSubsequenceAt(position.getFullChromosome(), position.getStart()-homopolymerWindow+1, position.getStart()).getBases();	
    			this.indelReferenceBases = position.getMotif();
    			this.downstreamReference = indexedFasta.getSubsequenceAt(position.getFullChromosome(), position.getEnd(), position.getEnd()+(homopolymerWindow-1)).getBases();    		
    		}
    		
		
		} catch (Exception e) {
			
	        logger.info("Error retrieving reference bases for: " + position.toString());
	        logger.info("Error: " + QBasePileupUtil.getStrackTrace(e));
		    return false;
		}
		return true;		
	}

	@Override
	public String toString() {	
		if (homopolymerCount.equals("") && type.equals("")) {
			return "";
		} else {
			return "\"" + homopolymerCount + " " + type + " " + sequence + "\"";
		}
	}
	
	public String getType() {
		return type;
	}

	public String getHomopolymerCount() {
		return homopolymerCount;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setHomopolymerCount(String homopolymerCount) {
		this.homopolymerCount = homopolymerCount;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
		
	}

}
