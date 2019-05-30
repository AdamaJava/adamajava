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
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.util.Constants;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.util.QSVUtil;

public class SoftClipStaticMethods {
	
	public static void writeSoftClipRecord(BufferedWriter writer, SAMRecord record, Integer start, Integer end, String chromosome) throws IOException {
		SAMReadGroupRecord rg = record.getReadGroup();
		writeSoftClipRecord( writer, record, (null != rg ? rg.getId() : Constants.EMPTY_STRING), start, end, chromosome);
	}
	public static void writeSoftClipRecord(BufferedWriter writer, SAMRecord record, String rgId, Integer start, Integer end, String chromosome) throws IOException {

		String clipRecordString = createSoftClipRecordString(record, rgId, start, end, chromosome);
		
		if (clipRecordString != null) {			
			writer.write(clipRecordString);
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
				
				int bpPos = record.getAlignmentStart();
				if (QSVUtil.createRecord(bpPos, start, end)) {
					String sequenceString = record.getReadString();
					int len = first.getLength();
					String sequence = sequenceString.substring(0, len);
					String alignedSequence = sequenceString.substring(len);
					return new Clip(record, bpPos, sequence,alignedSequence, Clip.LEFT);
				}			
			} else if (last.getOperator().equals(CigarOperator.S)) {
				
				int bpPos = record.getAlignmentEnd();
				if (QSVUtil.createRecord(bpPos, start, end)) {
					String sequenceString = record.getReadString();
					int seqLen = sequenceString.length() - last.getLength();
					String sequence = sequenceString.substring(seqLen);
					String alignedSequence = sequenceString.substring(0, seqLen);
					return new Clip(record, bpPos, sequence, alignedSequence, Clip.RIGHT);				
				}			
			}			
		}		
		return null;
	}
	public static String createSoftClipRecordString(SAMRecord record, String rgId, Integer start, Integer end, String chromosome) {
		if ( ! record.getReadUnmappedFlag()) {
			List<CigarElement> elements = record.getCigar().getCigarElements();
			
			CigarElement first = elements.get(0);
			CigarElement last = elements.get(elements.size()-1);
			
			if (first.getOperator().equals(CigarOperator.S) && last.getOperator().equals(CigarOperator.S)) {
				return null;
			} else if (first.getOperator().equals(CigarOperator.S)) {
				
				int bpPos = record.getAlignmentStart();
				if (QSVUtil.createRecord(bpPos, start, end)) {
					int len = first.getLength();
					return createSoftClipRecordString(record, rgId, bpPos, len, true);
				}			
			} else if (last.getOperator().equals(CigarOperator.S)) {
				
				String sequenceString = record.getReadString();
				int bpPos = record.getAlignmentEnd();
				if (QSVUtil.createRecord(bpPos, start, end)) {
					int cutoff = sequenceString.length() - last.getLength();
					return createSoftClipRecordString(record,rgId,  bpPos, cutoff, false);				
				}			
			}			
		}		
		return null;
	}
	
	public static String createSoftClipRecordString(SAMRecord record, String rgId, int bpPos, int cutoff, boolean isLeft) {
		StringBuilder sb = new StringBuilder(record.getReadName());
		sb.append(Constants.COLON);
		sb.append(rgId).append(Constants.COMMA);
		sb.append(record.getReferenceName()).append(Constants.COMMA);
		sb.append(bpPos).append(Constants.COMMA);
		sb.append(record.getReadNegativeStrandFlag() ? QSVUtil.MINUS : QSVUtil.PLUS).append(Constants.COMMA);
		sb.append(isLeft ? Clip.LEFT : Clip.RIGHT).append(Constants.COMMA);
		String read = record.getReadString();
		sb.append(read).append(Constants.COMMA);
		sb.append(isLeft ? read.substring(0,  cutoff) : read.substring(cutoff)).append(Constants.COMMA);
		sb.append(isLeft ? read.substring(cutoff) : read.substring(0, cutoff)).append(QSVUtil.NEW_LINE);
		
		return sb.toString();
	}

	public static String getSoftClipFile(String chromosome, String type, String softclipDir) {
		return softclipDir + QSVParameters.FILE_SEPERATOR + type  + "."+ chromosome + ".clip";
	}

}
