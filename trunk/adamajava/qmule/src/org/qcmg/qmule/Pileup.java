/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.QPileupSimpleRecord;
import org.qcmg.picard.SAMFileReaderFactory;


public class Pileup {
	private static QLogger logger = QLoggerFactory.getLogger(Pileup.class);
	
	Map<ChrPosition, QPileupSimpleRecord> pileup = new TreeMap<ChrPosition, QPileupSimpleRecord>();
//	Map<ChrPosition, PileupRecord> pileup = new HashMap<ChrPosition, PileupRecord>(10000000, 0.99f);
	
	private void engage(String args[]) throws IOException {
		
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(args[0]));
		FileWriter writer = new FileWriter(new File(args[1]));
		
		int counter = 0;
		for (SAMRecord sr : reader) {
			parseRecord(sr);
			if (++counter % 100000 == 0) {
				logger.info("hit " + counter + " reads in bam file, size of pileup map is: " + pileup.size());
				
				// output contents of pileup to file to clear memory
				// get current chromosome and position an write out 
				//all records a couple of hundred bases prior to that position
				writePileup(writer, sr.getReferenceName(), sr.getAlignmentStart() - 500);
			}
		}
		logger.info("Done!! No of reads in file: " + counter + ",  size of pileup map is: " + pileup.size() );
	}
	
	private void writePileup(FileWriter writer, String chromosome, int position) throws IOException {
		ChrPosition chrPos = new ChrPosition(chromosome, position);
		
		Iterator<Entry<ChrPosition, QPileupSimpleRecord>>  iter = pileup.entrySet().iterator();
		
		while (iter.hasNext()) {
			Map.Entry<ChrPosition, QPileupSimpleRecord> entry = iter.next();
			if (0 < chrPos.compareTo(entry.getKey())) {
				
				writer.write(entry.getKey().getChromosome() + "\t" +
						entry.getKey().getPosition() + "\t" +
						entry.getValue().getFormattedString());
				
				iter.remove();
			}
		}
		
	}
	
	private void parseRecord(SAMRecord sr) {
		
		ChrPosition chrPos;
		QPileupSimpleRecord pileupRec;
		int position = 0;
		
		for (byte b : sr.getReadBases()) {
			chrPos = new ChrPosition(sr.getReferenceName(), sr.getAlignmentStart() + position++);
			pileupRec = pileup.get(chrPos);
			if (null == pileupRec) {
				pileupRec = new QPileupSimpleRecord();
				pileup.put(chrPos, pileupRec);
			}
			pileupRec.incrementBase(b);
		}
		
		
	}
	
	
	
	public static void main(String[] args) throws IOException {
		Pileup p = new Pileup();
		p.engage(args);
	}
}
