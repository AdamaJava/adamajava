/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.softclip;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;

import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.util.QSVUtil;

public class SoftClipStaticMethods {
	
	public static void writeSoftClipRecord(BufferedWriter writer, SAMRecord record, Integer start, Integer end, String chromosome) throws IOException {
//		public static synchronized void writeSoftClipRecord(BufferedWriter writer, SAMRecord record, Integer start, Integer end, String chromosome) throws IOException {

		Clip clipRecord = createSoftClipRecord(record, start, end, chromosome);
		
//		if (record.getReadName().equals("HWI-ST1240:47:D12NAACXX:2:2315:11796:5777")) {
//			System.out.println(record.getSAMString());
//			System.out.println(record.getAlignmentStart());
//		}
		if (clipRecord != null) {			
			writer.write(clipRecord.toString());
		}
	}

	
	public static Clip createSoftClipRecord(SAMRecord record, Integer start, Integer end, String chromosome) {
		if ( ! record.getReadUnmappedFlag()) {
			List<CigarElement> elements = record.getCigar().getCigarElements();
			
			CigarElement first = elements.get(0);
			CigarElement last = elements.get(elements.size()-1);
			
			if (first.getOperator().equals(CigarOperator.S) && last.getOperator().equals(CigarOperator.S)) {
				return null;
			} else if (first.getOperator().equals(CigarOperator.S)) {
				
				String sequenceString = record.getReadString();
				int bpPos = record.getAlignmentStart();
				if (QSVUtil.createRecord(bpPos, start, end)) {
					String sequence = sequenceString.substring(0, first.getLength());
					String alignedSequence = sequenceString.substring(first.getLength(), sequenceString.length());
					return new Clip(record, bpPos, sequence,alignedSequence, "left");
				}			
			} else if (last.getOperator().equals(CigarOperator.S)) {
				
				String sequenceString = record.getReadString();
				int bpPos = record.getAlignmentEnd();
				if (QSVUtil.createRecord(bpPos, start, end)) {
					String sequence = sequenceString.substring(sequenceString.length()-last.getLength(), sequenceString.length());
					String alignedSequence = sequenceString.substring(0, sequenceString.length()-last.getLength());
					return new Clip(record, bpPos, sequence, alignedSequence, "right");				
				}			
			}			
		}		
		return null;
	}

	public static String getSoftClipFile(String chromosome, String type, String softclipDir) {
		return softclipDir + QSVParameters.FILE_SEPERATOR + type  + "."+ chromosome + ".clip";
	}

	public static String getSoftClipBases(SAMRecord record) {
		List<CigarElement> elements = record.getCigar().getCigarElements();
		
		CigarElement first = elements.get(0);
		CigarElement last = elements.get(elements.size()-1);
		String sequenceString = record.getReadString();
		
		if (first.getOperator().equals(CigarOperator.S) && last.getOperator().equals(CigarOperator.S)) {
			return null;
		}
		if (first.getOperator().equals(CigarOperator.S)) {
			String sequence = sequenceString.substring(0, first.getLength());
			return sequence;
		}
		
		if (last.getOperator().equals(CigarOperator.S)) {
			String sequence = sequenceString.substring(sequenceString.length()-last.getLength(), sequenceString.length());
			return sequence;
		}
		return null;
	}
	
//	public static boolean passesAdapterFilter(SAMRecord record, String pairingType) {
//		
//		if (!pairingType.equals("imp")) {
//			return true;
//		}
//		Cigar cigar = record.getCigar();
//		String sequenceString = record.getReadString();			
//		
//		CigarElement startClip = cigar.getCigarElement(0);
//		CigarElement endClip = cigar.getCigarElement(cigar.numCigarElements()-1);
//		
//		if (startClip.getOperator() == CigarOperator.SOFT_CLIP) {
//			int length = startClip.getLength();
//			if (length > 9) {
//				String sequence = sequenceString.substring(length-10, length);
//				for (String adapter: QSVConstants.startAdapterMatches) {
//					if (adapter.equals(sequence)) {
//						//System.out.println(sequence);
//						return false;
//					}					
//				}
//			} 
//			
////			else {
////				String sequence = sequenceString.substring(0, startClip.getLength());
////				for (String adapter: QSVConstants.startAdapterMatches) {
////					if (adapter.contains(sequence)) {
////						System.out.println(sequence);
////						return false;
////					}
////				}
////			}			
//		}
//		
//		if (endClip.getOperator() == CigarOperator.SOFT_CLIP) {
//			int length = endClip.getLength();
//			if (length > 9) {
//				String sequence = sequenceString.substring(sequenceString.length()-length, sequenceString.length()-length+10);
//				for (String adapter: QSVConstants.endAdapterMatches) {
//					if (adapter.equals(sequence)) {
//						return false;
//					}					
//				}
//			} 
////			else {
////				String sequence = sequenceString.substring(sequenceString.length()-endClip.getLength(), sequenceString.length());
////				for (String adapter: QSVConstants.endAdapterMatches) {
////					if (adapter.contains(sequence)) {
////						//System.out.println(sequence);
////						return false;
////					}
////				}
////			}			
//		}
//		return true;
//	}



}
