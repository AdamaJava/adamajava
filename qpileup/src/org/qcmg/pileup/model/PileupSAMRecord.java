/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.model;

import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.pileup.PileupUtil;
import org.qcmg.pileup.QPileupException;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.SequenceUtil;

public class PileupSAMRecord {
	
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private final SAMRecord record;
	private boolean isReverse = false;
	private final byte[] baseQualities;
	private final int referenceStart;
	private final int referenceEnd;
	private final int readStart;
	private final int readEnd;
	private final List<PileupDataRecord> dsRecords;	
	private final boolean isDuplicate;
	private boolean isMateUnmapped;
	
	public PileupSAMRecord(SAMRecord record) {
		this.record = record;
		this.isReverse = record.getReadNegativeStrandFlag();
		this.baseQualities = record.getBaseQualities();
		this.referenceStart = record.getUnclippedStart();
		this.referenceEnd = record.getUnclippedEnd();
		this.readStart = record.getAlignmentStart();
		this.readEnd = record.getAlignmentEnd();
		this.dsRecords = new ArrayList<PileupDataRecord>();
		this.isDuplicate = record.getDuplicateReadFlag();
		this.isMateUnmapped = false;
		if (record.getReadPairedFlag()) {
			this.isMateUnmapped = record.getMateUnmappedFlag();
		}		
	}
	
	public boolean isReverse() {
		return isReverse;
	}

//	public void setReverse(boolean isReverse) {
//		this.isReverse = isReverse;
//	}

	public String getReference() {
		return record.getReferenceName();		
	}
	
	public int getReferenceStart() {
		return referenceStart;
	}
	
	public int getReferenceEnd() {
		return referenceEnd;
	}
	
	public String toSAMString() {
		return record.getSAMString();
	}

	public List<PileupDataRecord> getPileupDataRecords() {
		return dsRecords;
	}
	
	public SAMRecord getSAMRecord() {
		return this.record;
	}

	public boolean getReadStrandNegativeFlag() {
		return record.getReadNegativeStrandFlag();
	}
	
	public void pileup() throws QPileupException {
		try {
			
			String md = (String)record.getAttribute("MD");
			if (md.contains("M")){				
				String newM = md.replaceAll("M", "N");				
				record.setAttribute("MD", newM);
			} else if (md.contains("R")) {		
				String newM = 	md.replaceAll("R", "N");
				record.setAttribute("MD", newM);
			} else if (md.contains("x")) {
				String newM = 	md.replaceAll("x", "N");
				record.setAttribute("MD", newM);
			}
			
			
			int readIndex = 0;		
			int referencePos = referenceStart;
			int referenceIndex = 0;
			
			//make the reference from the read sequence using picard
			byte[] referenceBytes = SequenceUtil.makeReferenceFromAlignment(record, true);			
			byte[] readBytes = record.getReadBases();		

			//iterate through cigar elements and perform pileup
			for (CigarElement element: record.getCigar().getCigarElements()) {
				CigarOperator operator = element.getOperator();
				
				//not a read base
				if (referencePos < readStart || referencePos > readEnd) {
					
					if (operator == CigarOperator.H || operator == CigarOperator.S) {	
						setCigarRecords(referencePos, element.getLength(), operator, readStart, readEnd);
						referencePos += element.getLength();					
						if (operator == CigarOperator.S) {
							readIndex += element.getLength();
							referenceIndex += element.getLength();
						}
					} else if (operator == CigarOperator.I) {
						setCigarI(referencePos-1);
						referenceIndex += element.getLength();
					} else {
						String error = "ReferencePos: " + referencePos + " ReadStart: " + readStart + " ReadEnd: " + readEnd + " CigarOperator: " + operator.name();
						throw new QPileupException("CIGAR_ERROR", error, record.getSAMString());						
					}
				} else {
					//should be in the read
					if (referencePos >= readStart && referencePos <= readEnd) {
						if (operator == CigarOperator.M) {						
							for (int i=0; i<element.getLength(); i++) {
								char base = (char) readBytes[readIndex];
								char ref = (char) referenceBytes[referenceIndex];
								addReadDataRecord(referencePos, base, ref, baseQualities[readIndex]);
								readIndex++;
								referenceIndex++;
								referencePos++;
							}												
						} else if (operator ==  CigarOperator.D) {
							setCigarRecords(referencePos, element.getLength(), operator, readStart, readEnd);
							referencePos += element.getLength();
							referenceIndex += element.getLength();
						} else if (operator == CigarOperator.I) {
							setCigarI(referencePos-1);						
							readIndex += element.getLength();
							referenceIndex += element.getLength();
						} else if (operator == CigarOperator.N) {
							setCigarRecords(referencePos, element.getLength(), operator, readStart, readEnd);
							referencePos += element.getLength();
							referenceIndex+= element.getLength();											
						} else if (operator == CigarOperator.P) {
							throw new QPileupException("CIGAR_P_ERROR", record.getSAMString());
						} else {
							String error = "ReferencePos: " + referencePos + " ReadStart: " + readStart + " ReadEnd: " + readEnd;
							throw new QPileupException("BASE_ERROR", error, record.getSAMString());
						}						
					} else {
						String error = "ReferencePos: " + referencePos + " ReadStart" + readStart + " ReadEnd: " + readEnd + " CigarOperator: " + operator.name();
						throw new QPileupException("BASE_RANGE_ERROR", ""  + error, record.getSAMString());
					}
				}			
	 		}
			
		} catch (Exception e) {
			logger.warn("Error parsing SAMRecord: " + record.getSAMString());
			logger.warn("Error parsing SAMRecord: " + PileupUtil.getStrackTrace(e));
			throw new QPileupException("PILEUP_ERROR");   
		}	
	}

	public void setCigarI(int referencePos) {

		PileupDataRecord d = new PileupDataRecord(referencePos, isReverse);
		d.setCigarI(1);
		
		dsRecords.add(d);
	}

	public void addReadDataRecord(int position, char base, char ref, int baseQual) throws QPileupException {
		
		PileupDataRecord d = new PileupDataRecord(position, isReverse);
		if (position == readStart) {						
			d.setStartAll(1);
			if (!isDuplicate) {
				d.setStartNondup(1);
			}
		}
		
		if(isDuplicate){
			d.setDupCount(1);
		}
		
		if (isMateUnmapped) {
			d.setMateUnmapped(1);
		}
		
		if (position == readEnd) {
			d.setStopAll(1);
		}
		
		d.setMapQual(record.getMappingQuality());
		d.checkBase(base, baseQual, ref, record);

		dsRecords.add(d);
	}

	public void setCigarRecords(int referencePos, int length, CigarOperator o, int readStart, int readEnd) {
		for (int i=0; i<length; i++) {
			PileupDataRecord d = new PileupDataRecord(referencePos, isReverse);	
			if (o.equals(CigarOperator.H)) {
				d.setCigarH(1);	
				if (i==0 && referencePos > readEnd) {
					d.setCigarHStart(1);
				}
				if (i==(length-1) && referencePos < readStart) {
					d.setCigarHStart(1);
				}
			} else if (o.equals(CigarOperator.S)) {
				d.setCigarS(1);	
				if (i==0 && referencePos > readEnd) {
					d.setCigarSStart(1);
				}
				if (i==(length-1) && referencePos < readStart) {
					d.setCigarSStart(1);
				}
			} else if (o.equals(CigarOperator.N)) {
				d.setCigarN(1);
				if (i==0) {
					d.setCigarNStart(1);
				}
			} else if (o.equals(CigarOperator.D)) {
				d.setCigarD(1);
				if (i==0) {
					d.setCigarDStart(1);
				}
			}
			dsRecords.add(d);
			referencePos++;
		}
	}
}
