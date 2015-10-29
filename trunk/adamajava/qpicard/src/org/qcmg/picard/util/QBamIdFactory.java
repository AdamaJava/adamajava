/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

//import htsjkd.samtools.SAMFileHeader;
import java.io.File;

import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.SAMFileHeader;

import org.qcmg.common.meta.QBamId;
import org.qcmg.picard.SAMFileReaderFactory;

public class QBamIdFactory {
	
	public static final String CN_QCMG = "CN:QCMG	QN:qbamid";
	
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

}
