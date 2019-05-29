/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.softclip;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

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
			CigarElement last = elements.get(elements.size() - 1);
			
			if (first.getOperator().equals(CigarOperator.S) && last.getOperator().equals(CigarOperator.S)) {
				return null;
			} else if (first.getOperator().equals(CigarOperator.S)) {
				
				String sequenceString = record.getReadString();
				int bpPos = record.getAlignmentStart();
				if (QSVUtil.createRecord(bpPos, start, end)) {
					int len = first.getLength();
					String sequence = sequenceString.substring(0, len);
					String alignedSequence = sequenceString.substring(len);
					return new Clip(record, bpPos, sequence,alignedSequence, Clip.LEFT);
				}			
			} else if (last.getOperator().equals(CigarOperator.S)) {
				
				String sequenceString = record.getReadString();
				int bpPos = record.getAlignmentEnd();
				if (QSVUtil.createRecord(bpPos, start, end)) {
					int seqLen = sequenceString.length() - last.getLength();
					String sequence = sequenceString.substring(seqLen);
					String alignedSequence = sequenceString.substring(0, seqLen);
					return new Clip(record, bpPos, sequence, alignedSequence, Clip.RIGHT);				
				}			
			}			
		}		
		return null;
	}

	public static String getSoftClipFile(String chromosome, String type, String softclipDir) {
		return softclipDir + QSVParameters.FILE_SEPERATOR + type  + "."+ chromosome + ".clip";
	}

}
