/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.picard.SAMFileReaderFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

public class BAMFileUtils {
	
	public static boolean bamFileIsLocked(File bamFile) {
		if (bamFile != null) {
			File bamlock = new File (bamFile.getAbsolutePath() + ".lck");
			
			if (bamlock.exists()) {
				return true;
			}
			
			File indexLock = new File (bamFile.getAbsolutePath() + ".bai.lck");
			
			if (indexLock.exists()) {
				return true;
			}
		}
		return false;
	}
	
	public static List<String> getContigsFromHeader(File bamFile) throws IOException {
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile)) {
			
			return reader.getFileHeader().getSequenceDictionary().getSequences()
					.stream()
					.map(SAMSequenceRecord::getSequenceName)
					.collect(Collectors.toList());
		}
	}

}
