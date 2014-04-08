/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import net.sf.samtools.SAMFileHeader;

import org.qcmg.common.meta.QLimsMeta;
import org.qcmg.picard.SAMFileReaderFactory;

public class QLimsMetaFactory {
	
	public static final String CN_QCMG = "CN:QCMG	QN:qlimsmeta";
	
	public static QLimsMeta getLimsMeta(String type, String bamFIleName) throws Exception {
		SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(bamFIleName).getFileHeader();
		String commentLine = getQCMGCommentLine(header);
		return new QLimsMeta(type, bamFIleName, commentLine);
	}
	public static QLimsMeta getLimsMeta(String type, String bamFIleName, SAMFileHeader header) throws Exception {
		String commentLine = getQCMGCommentLine(header);
		return new QLimsMeta(type, bamFIleName, commentLine);
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
