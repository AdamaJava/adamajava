/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.indel;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupException;


public class IndelPileup {
	
	InputBAM inputBam;
	IndelPosition position;
	private int totalReads = 0;
	private int informativeReads = 0;
	private int supportingReads = 0;
	private int nearbySoftClipCount = 0;
	private int nearbyIndelCount = 0;	
	private int partialIndelCount = 0;		
	private final int homopolymerWindow;
	private final int nearbyIndelWindow;
	private final int nearbySoftClipWindow;	
	private int forwardSupportingReads = 0;
	private int reverseSupportingReads = 0;
	private Set<Integer> novelStarts = new HashSet<Integer>();	
	private boolean isTumour;	
	private static final String SCOLON = ";";
	private Homopolymer homopolymer;
	private boolean highCoverage;
	private Map<String, SAMRecord> records = new HashMap<String, SAMRecord>();
	private final Options options;
	private final File referenceFile;

	public IndelPileup(Options options,InputBAM inputBam, IndelPosition position, File reference, int softClipWindow, int homopolymerWindow, int nearbyIndelWindow, boolean isTumour) throws QBasePileupException {
		this.inputBam = inputBam;
		this.position = position;
		this.nearbySoftClipWindow = softClipWindow;
		this.homopolymerWindow = homopolymerWindow;
		this.nearbyIndelWindow = nearbyIndelWindow;
		this.isTumour = isTumour;
		this.referenceFile = reference;		
		this.options = options;
	}
	
	public void pileupRead(SAMRecord r) {
		int readStart = r.getAlignmentStart();
		int readEnd = r.getAlignmentEnd();
		if (position.getStart() >= readStart && position.getStart() <=readEnd ||
				position.getEnd() >= readStart && position.getEnd() <=readEnd
				) {
			if (totalReads >= 1000) {
				highCoverage = true;
				return;
			}
			totalReads++;
			filterSAMRecord(r);					
		}		
	}
	
	public void pileupReads(QueryExecutor exec, IndexedFastaSequenceFile indexedFasta) throws Exception {
				
	//	setDefaultValidationStringency();
		SamReader reader =   SAMFileReaderFactory.createSAMFileReader(inputBam.getBamFile(),  ValidationStringency.SILENT);   //new SAMFileReader(inputBam.getBamFile());			
		
		SAMRecordIterator iter = reader.queryOverlapping(position.getFullChromosome(), position.getStart(), position.getEnd());		
		boolean passFilter;
		while (iter.hasNext()) {
			SAMRecord r = iter.next();
			//reset soft clip in indel flag
//			if (!r.getReadUnmappedFlag() &&  (!r.getDuplicateReadFlag() || r.getDuplicateReadFlag() && options.includeDuplicates()) && (exec == null || (exec !=null && exec.Execute(r)))) {

			if(exec != null )
				passFilter = exec.Execute(r);
			else
				passFilter = !r.getReadUnmappedFlag() && (!r.getDuplicateReadFlag() || options.includeDuplicates());

			if(! passFilter) continue;
			
			if (totalReads >= 1000) {
				highCoverage = true;
				break;
			}
			totalReads++;
			
			filterSAMRecord(r);						 
		}
		
		iter.close();
		reader.close();
	
		if (isTumour) {
			//only need to count homopolymers if it is the tumour bam
			this.homopolymer = new Homopolymer(position, indexedFasta, homopolymerWindow);			
		}
	}	
	
	public void finish(IndexedFastaSequenceFile indexedFasta) {
		
		if (isTumour) {
			//only need to count homopolymers if it is the tumour bam
			this.homopolymer = new Homopolymer(position, indexedFasta, homopolymerWindow);			
		}
	}

	private void filterSAMRecord(SAMRecord record) {
		
		byte[] maskedReadBases = new byte[record.getReadLength()];		
		int[] referencePositions = new int[record.getReadLength()];		
		
		//check to see if there is any soft clips close to the indel, 
		if (record.getCigarString().contains("S")) {
			//only need to mask if it contains soft clipping reads	
			deconvoluteCigarString(record, maskedReadBases, referencePositions);
			if (inSoftClipWindow(record)) {
				nearbySoftClipCount++;
			}
		} else {
			maskedReadBases = record.getReadBases();
		}
		
		//keep soft clip reads for the moment		
		if (recordSpansIndel(record)) {			
			if (position.isDeletion()) {
				parseDeletionIndel(record);
			} else if (position.isInsertion()){
				parseInsertionIndel(record, maskedReadBases);
			} else {
				parseComplexIndel(record, maskedReadBases);
			}
		}
	}	

	public void parseComplexIndel(SAMRecord record, byte[] maskedReadBases) {
		informativeReads++;	
		
		boolean indelPresent = false;
		int indexOfIndelStart = SAMUtils.getIndexInReadFromPosition(record, position.getStart());
		int indexOfIndelEnd = SAMUtils.getIndexInReadFromPosition(record, position.getEnd());
		for (int i = indexOfIndelStart; i<=indexOfIndelEnd; i++) {			
			if ((cigarOperatorAtReadPostion(record, CigarOperator.INSERTION, i)) || (cigarOperatorAtReadPostion(record, CigarOperator.DELETION, i))) {
				indelPresent = true;
				break;
			}
		}
		if (indelPresent) {
			partialIndelCount++;
		}
		
		if (nearbyIndel(record)) {
			nearbyIndelCount++;
		}
	}

	public boolean inSoftClipWindow(SAMRecord record) {
		
		int indelStart, indelEnd;		
		if (position.isDeletion() || position.isComplex()) {
			//reference bases that aren't indels
			indelStart = position.getStart()-1;
			indelEnd = position.getEnd()+1;	
		} else {
			indelStart = position.getStart();
			indelEnd = position.getEnd();	
		}		
		
		int windowStart = indelStart-nearbySoftClipWindow+1;
		int windowEnd = indelEnd+nearbySoftClipWindow-1;		
		
		//check left hand clipping		
		if (record.getAlignmentStart() != record.getUnclippedStart()) {
			int clipStartPosition = record.getAlignmentStart()-1;
			if (clipStartPosition >= windowStart && clipStartPosition <= windowEnd) {				
				return true;
			}
		}
		
		//check right hand clipping
		if (record.getAlignmentEnd() != record.getUnclippedEnd()) {
			int clipEndPosition = record.getAlignmentEnd()+1;			
			//clip start position is in the window to the left of the indel			
			if (clipEndPosition >= windowStart && clipEndPosition <= windowEnd) {				
				return true;
			}
		}
		return false;
	}

	public boolean recordSpansIndel(SAMRecord record) {
		//indel is present
		int indelStart = position.getStart();
		int indelEnd = position.getEnd();		
		
		//add one to make sure read extends PAST the indel, unless the deletion is one base only
		if (position.isDeletion()) {
			indelStart--;
			indelEnd++;		
		}
		//make sure that the read bases span the indel
		
		return record.getAlignmentStart() <= indelStart && record.getAlignmentEnd() >= indelEnd;
	}	
	
	public void parseDeletionIndel(SAMRecord record) {
		informativeReads++;		
		
		//check the length of the indel
		int indelLength = position.getLength();
		
		int readIndelLength = getDeletionIndelLength(record);			
		
		if (indelLength == readIndelLength) {
			//check that next position isn't part of a longer deletion	
			int nextPosition = position.getEnd()+1;
			if (nextPosition <= position.getEnd()+1 && SAMUtils.getIndexInReadFromPosition(record, nextPosition) < 0) {
				partialIndelCount++;
			} else {
				annotateIndelPresentInRead(record);
			}
			
		} else if (readIndelLength > 0) {
			//indel partially present			
			partialIndelCount++;			
		} 
		
		if (nearbyIndel(record)) {
			nearbyIndelCount++;
		}
	}

	public int getDeletionIndelLength(SAMRecord record) {
		int readIndelLength = 0;		
		for (int i = position.getStart(); i<=position.getEnd(); i++) {			
			int index = SAMUtils.getIndexInReadFromPosition(record, i);	
			//not in read
			if (index < 0) {				
				readIndelLength++;
			}
			
		}
		return readIndelLength;
	}

	public void parseInsertionIndel(SAMRecord record, byte[] maskedReadBases) {		
		
		//check to see if there is an N within the indel or in reference bases on either side of indel 
		if (!nBasePresentInInsertion(record, maskedReadBases)) {			
			informativeReads++;			
			//confirm there is an indel present
			findInsertionInRecord(record, maskedReadBases);		
			
			if (nearbyIndel(record)) {			
				nearbyIndelCount++;
			}
		}				
	}	
	
	public boolean nearbyIndel(SAMRecord record) {		
		
		final Cigar cigar = record.getCigar();
		if ( ! cigar.toString().contains("I") && ! cigar.toString().contains("D")) {
			return false;
		}
		
		//set up window of reference bases
		int indelStart, indelEnd, windowStart, windowEnd;
		if (position.isDeletion() || position.isComplex()) {
			indelStart = position.getStart()-1;
			indelEnd = position.getEnd()+1;
			windowStart = Math.max(record.getAlignmentStart(), indelStart-nearbyIndelWindow+1);
			windowEnd = Math.min(record.getAlignmentEnd(), indelEnd+ nearbyIndelWindow-1);
		} else {
			indelStart = position.getStart();
			indelEnd = position.getEnd();
			windowStart = Math.max(record.getAlignmentStart(), indelStart-nearbyIndelWindow);
			windowEnd = Math.min(record.getAlignmentEnd(), indelEnd+ nearbyIndelWindow-1);
		}
		
		//SSystem.out.println(windowStart + " " + indelStart + " " + indelEnd + " " + windowEnd);

		int refPos = record.getAlignmentStart();	
//		System.out.println(cigar.toString() + " " + record.getReadName());
//		System.out.println(windowStart + " " + indelStart + " " + indelEnd + " " + windowEnd);
//		System.out.println("s" + record.getAlignmentStart() + " e " + record.getAlignmentEnd());
		for (CigarElement ce : cigar.getCigarElements()) {
			int cigarLength = ce.getLength();			
//			System.out.println("refPos" + refPos + " " + ce.getOperator().toString() + " " + ce.getLength());
			if (CigarOperator.DELETION == ce.getOperator()) {
				int currentRefPos = refPos;
				//need to check is isn't just part of a partial indel for the supplied position
				if (position.isDeletion()) {					
					for (int pos= position.getStart(); pos<= position.getEnd(); pos++) {						
						if (pos >= currentRefPos && pos <= (currentRefPos + cigarLength-1)) {
							return false;
						}
					}
				}				
				for (int i=0; i<cigarLength; i++) {					
					if ((currentRefPos >= windowStart && currentRefPos <= indelStart) ||
							(currentRefPos >= indelEnd && currentRefPos <= windowEnd)) {
						//make sure it isn't same position						
						if (currentRefPos == position.getStart()) {
							if (position.isInsertion()) {
								return true;
							}
						} else {
							return true;
						}
					}
				}
					
			} else if (CigarOperator.INSERTION == ce.getOperator()) {
				int currentRefPos = refPos-1;
				if ((currentRefPos >= windowStart && currentRefPos <= indelStart) ||
						(currentRefPos >= indelEnd && currentRefPos <= windowEnd)) {
					if (position.isInsertion()) {
						if (currentRefPos != indelStart) {
							return true;
						}
					} else {
						return true;
					}
				}
			}			

			if (ce.getOperator().consumesReferenceBases()) {
				refPos += ce.getLength();
			}				
		}
		
		return false;
	}

	public void findInsertionInRecord(SAMRecord record, byte[] maskedReadBases) {		
		
		if (!record.getCigarString().contains("I")) {
			return;
		}
		
		//get index of indel positions
		int indexOfIndelStart = SAMUtils.getIndexInReadFromPosition(record, position.getStart());
		int indexOfIndelEnd = SAMUtils.getIndexInReadFromPosition(record, position.getEnd());
		
		if (indexOfIndelStart == -1 && indexOfIndelEnd == -1) {
			return;
		}
		
		//get reference position at start of index and the next position
		int refPosition = record.getReferencePositionAtReadPosition(indexOfIndelStart+1);
		int nextRefPosition = record.getReferencePositionAtReadPosition(indexOfIndelStart+2);	
		
		//if the referencePosition at start of insert + next reference position is one greater, and the cigar is an insertion 
		if (refPosition != -1 && (nextRefPosition == 0) && cigarOperatorAtReadPostion(record, CigarOperator.INSERTION, indexOfIndelStart+1)) {			
			//check actual inserted bases only indexStart==refbase before insertion and indexEnd==refbase after insertion
			byte[] motif = position.getMotif();
			int motifIndex = 0;
			boolean fullInsertion = true;
			
			int indelLength = (indexOfIndelEnd-1) - (indexOfIndelStart+1) +1;
			if (motif.length != (indelLength)) {
				fullInsertion = false;
			}
			for (int i = indexOfIndelStart+1; i<indexOfIndelEnd; i++) {
				if (motifIndex < motif.length) {					
					if (motif[motifIndex] != maskedReadBases[i]) {
						fullInsertion = false;
						break;
					}	
					motifIndex++;
				}			
			}
			if (fullInsertion) {
				annotateIndelPresentInRead(record);
			} else {				
				partialIndelCount++;
			}
		}
	}
	
	public void deconvoluteCigarString(SAMRecord record, byte[] maskedReadBases, int[] referencePositions) {
		Cigar cigar = record.getCigar();		
		byte[] readBases = record.getReadBases();
		int referencePosition = record.getAlignmentStart();
		int readBaseIndex = 0;		
		
		for (CigarElement ce : cigar.getCigarElements()) {
			
			CigarOperator co = ce.getOperator();	
           
			if (co.consumesReferenceBases() && co.consumesReadBases()) {				
				// move both offset and referenceOffset forward by length	
				for (int i=0; i<ce.getLength(); i++) {							
					maskedReadBases[readBaseIndex] = readBases[readBaseIndex];
					referencePositions[readBaseIndex] = referencePosition;
					referencePosition++;
					readBaseIndex++;
				}								
			} else if (co.consumesReadBases()){
				// INSERTION, SOFT CLIPPING				
				if (ce.getOperator() == CigarOperator.SOFT_CLIP) {
					for (int i=0; i<ce.getLength(); i++) {							
						maskedReadBases[readBaseIndex] = (byte) 'S';
						referencePositions[readBaseIndex] = -1;
						referencePosition++;
						readBaseIndex++;
					}
				} else {					
					for (int i=0; i<ce.getLength(); i++) {						
						maskedReadBases[readBaseIndex] = readBases[readBaseIndex];
						referencePositions[readBaseIndex] = 0;
						readBaseIndex++;
					}
				}
			} else if (co.consumesReferenceBases()) {
				for (int i=0; i<ce.getLength(); i++) {						
					referencePosition++;
				}
			}
		}
	}

	public boolean cigarOperatorAtReadPostion(SAMRecord record, CigarOperator operator, int readIndexToFind) {
		Cigar cigar = record.getCigar();
		
		if ( ! cigar.toString().contains(operator.toString())) {
			return false;
		}			
		
		int readIndex = 0;		
		
		for (CigarElement ce : cigar.getCigarElements()) {
			int cigarLength = ce.getLength();			
			if (readIndex > readIndexToFind) {
				break;
			}
			if (operator == ce.getOperator()) {				
				if (readIndexToFind >= readIndex && readIndexToFind < (readIndex+cigarLength)) {
					return true;
				}
			}
			if (ce.getOperator().consumesReadBases()) {
				readIndex += cigarLength;
			}
		}
		return false;
	}	

	public boolean nBasePresentInInsertion(SAMRecord record, byte[] maskedReadBases) {
		int startIndex = SAMUtils.getIndexInReadFromPosition(record, position.getStart());
		int endIndex =  SAMUtils.getIndexInReadFromPosition(record, position.getEnd());		
		
		if (startIndex > -1 && endIndex > -1) {
			for (int i = startIndex; i<=endIndex; i++) {							
				char base = (char)maskedReadBases[i];			
				if (base == 'N') {
					return true;
				}			
			}			
		}
		return false;
	}

	public void annotateIndelPresentInRead(SAMRecord record) {
		
		supportingReads++;
		addToNovelStarts(record);	
		String pairFlag = "2";
		if (record.getFirstOfPairFlag()) {
			pairFlag = "1";
		}
		records.put(record.getReadName()+"/" + pairFlag, record);
		
		if (record.getReadNegativeStrandFlag()) {
			reverseSupportingReads++;
		} else {
			forwardSupportingReads++;
		}
	}
	
	public void addToNovelStarts(SAMRecord record) {
		
		if (record.getReadNegativeStrandFlag()) {
			novelStarts.add(record.getAlignmentEnd());
		} else {
			novelStarts.add(record.getAlignmentStart());
		}
		
	}
	
	public int getTotalReads() {
		return totalReads;
	}

	public void setTotalReads(int totalReads) {
		this.totalReads = totalReads;
	}

	public int getInformativeReads() {
		return informativeReads;
	}

	public void setInformativeReads(int informativeReads) {
		this.informativeReads = informativeReads;
	}

	public int getNearbySoftClipCount() {
		return nearbySoftClipCount;
	}

	public void setNearbySoftClipCount(int nearbySoftClipCount) {
		this.nearbySoftClipCount = nearbySoftClipCount;
	}

	public int getNovelStartCount() {
		return novelStarts.size();
	}

	public int getNearbyIndelCount() {
		return nearbyIndelCount;
	}

	public void setNearbyIndelCount(int nearbyIndelCount) {
		this.nearbyIndelCount = nearbyIndelCount;
	}

	public int getPartialIndelCount() {
		return partialIndelCount;
	}

	public void setPartialIndelCount(int partialIndelCount) {
		this.partialIndelCount = partialIndelCount;
	}
	
	public Homopolymer getHomopolymer() {
		return this.homopolymer;
	}

	public void setHomopolymer(Homopolymer hp) {
		this.homopolymer = hp;
		
	}	

	public boolean isHighCoverage() {
		return highCoverage;
	}

	public void setHighCoverage(boolean highCoverage) {
		this.highCoverage = highCoverage;
	}

	public void setNovelStarts(Set<Integer> nStarts) {
		this.novelStarts = nStarts;		
	}

	public Set<Integer> getNovelStarts() {
		return this.novelStarts;
	}

	public void setIsTumour(boolean b) {
		this.isTumour = b;		
	}

	public IndelPosition getIndelPosition() {
		return position;		
	}
	
	public int getSupportingReads() {
		return supportingReads;
	}

	public void setSupportingReads(int supportingReads) {
		this.supportingReads = supportingReads;
	}
	
	public Map<String, SAMRecord> getRecords() {
		return records;
	}

	public void setRecords(Map<String, SAMRecord> records) {
		this.records = records;
	}


	
	public String toDCCString() {
		String s = getNovelStartCount() + SCOLON + totalReads  + SCOLON + informativeReads + SCOLON + supportingReads + "[" +  forwardSupportingReads + "|" + reverseSupportingReads + "]" 
				+ SCOLON + partialIndelCount  + SCOLON + nearbyIndelCount + SCOLON + nearbySoftClipCount;
		
//				String s = getNovelStartCount() + SCOLON + totalReads  + SCOLON + informativeReads + SCOLON + indelPresentInRecordCount + "[" +  forwardSupportingReads + "|" + reverseSupportingReads + "]" 
//						+ SCOLON + partialIndelCount  + SCOLON + nearbyIndelCount + SCOLON + nearbySoftClipCount +  SCOLON + forwardSupportingReads 
//						+ SCOLON  + reverseSupportingReads; 
				
		if (isTumour && homopolymer != null) {
			s += SCOLON + homopolymer.toString(); 
		}
		return s;
	}

	public int getForwardSupportingReads() {
		return forwardSupportingReads;
	}

	public void setForwardSupportingReads(int forwardSupportingReads) {
		this.forwardSupportingReads = forwardSupportingReads;
	}

	public int getReverseSupportingReads() {
		return reverseSupportingReads;
	}

	public void setReverseSupportingReads(int reverseSupportingReads) {
		this.reverseSupportingReads = reverseSupportingReads;
	}

	public void setIndelPosition(IndelPosition position) {
		this.position = position;		
	}

	public boolean hasStrandBias() {
		//supporting reads are greater than 3
		if (supportingReads >= 3) {
			//0 reads on one side
			if (forwardSupportingReads == 0 || reverseSupportingReads == 0) {
				return true;
			}
			
			double minPercent, maxPercent;
			if (isTumour) {
				minPercent = 10;
				maxPercent = 90;
			} else {
				minPercent = 5;
				maxPercent = 95;
			}
			double forwardPercent = getPercentForwardSupportingReads();
			double reversePercent = getPercentReverseSupportingReads();
			if (forwardPercent < minPercent || forwardPercent > maxPercent 
					|| reversePercent < minPercent || reversePercent > maxPercent) {
				return true;
			}					
		}

		return false;
	}

	private double getPercentForwardSupportingReads() {
		if (forwardSupportingReads > 0) {
			return ((double) forwardSupportingReads / (double)supportingReads) * 100;
		}
		return 0;
	}
	
	private double getPercentReverseSupportingReads() {
		if (reverseSupportingReads > 0) {
			return ((double) reverseSupportingReads / (double)supportingReads) * 100;
		}
		return 0;		
	}

	public double getPercentPartial() {
		if (partialIndelCount > 0) {
			return ((double) partialIndelCount / (double) totalReads) * 100;
		}
		return 0;
	}
}
