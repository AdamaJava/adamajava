/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

//import htsjkd.samtools.SAMFileHeader;
import java.io.File;

import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.SAMFileHeader;

import org.qcmg.common.meta.KeyValue;
import org.qcmg.common.meta.QBamId;
import org.qcmg.common.string.StringUtils;
import org.qcmg.picard.SAMFileReaderFactory;

public class QBamIdFactory {
	
	public static final String CN_QCMG = "CN:QCMG	QN:qbamid";
	public static final String Q3BamId = "q3BamUUID";
	
	public static QBamId getBamId(String bamFIleName) throws Exception {
		SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(new File(bamFIleName)).getFileHeader();
		String commentLine = getQCMGCommentLine(header);
		return new QBamId(bamFIleName, commentLine);
	}
	public static QBamId getBamId(String bamFIleName, SAMFileHeader header) throws Exception {
		String commentLine = getQCMGCommentLine(header);
		return new QBamId(bamFIleName, commentLine);
	}
	
	public static String getQCMGCommentLine(SAMFileHeader header) {
		for (String s : header.getComments()) {
			if (s.contains(CN_QCMG)) {
				return s;
			}
		}
		return null;
	}
	
	//@CO	q3BamUUID:299225f0-59fc-4cbd-89a1-e7c2ea23e220
	public static QBamId getQ3BamId(String bamFIleName) throws Exception {
		SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(new File(bamFIleName)).getFileHeader();				
		String commentLine = null;
		for (String s : header.getComments()) 
			if (s.contains(Q3BamId+":")) 
				commentLine = s;
	
		String uuid =  StringUtils.getValueFromKey(commentLine, Q3BamId, ':');				
		return new QBamId(bamFIleName, null, uuid);
	}
}
