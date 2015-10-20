/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;

public class BamRecordCounter {
	
	private static final QLogger logger = QLoggerFactory.getLogger(BamRecordCounter.class);

	public static void main(String args[]) {
		
		if (null != args && args.length > 0) {
			for (String filename : args) {
				SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(filename));
				long count = 0;
				long duplicates = 0;
				long startTime = System.currentTimeMillis();
				for (SAMRecord r : reader) {
					count++;
					if (r.getDuplicateReadFlag())
						duplicates++;
				}
				logger.info("no of records in file [" + filename + "] is: " + count);
				logger.info("no of duplicate records: " + duplicates);
				logger.info("It took " + (System.currentTimeMillis() - startTime) + "ms to perform the count.");
			}
		} else {
			logger.info("USAGE: qmule " + BamRecordCounter.class.getName() + " <bam/sam filename>");
		}
	}

}
