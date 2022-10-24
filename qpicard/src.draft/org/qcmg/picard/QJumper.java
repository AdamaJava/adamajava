/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;

import org.qcmg.common.util.FileUtils;


public class QJumper {
	
	private List<SamReader> readers = new ArrayList<SamReader>();

	/**
	 * Returns all SAMRecords that have reads that are at the supplied contig and position
	 * @param contig
	 * @param position
	 * @return List<SAMRecord> records that represent this contig and position
	 * @throws Exception 
	 */
	public List<SAMRecord> getRecordsAtPosition(String contig, int position) throws Exception {
		return getOverlappingRecordsAtPosition(contig, position, position);
	}
	
	/**
	 * Returns all SAMRecords that have reads that are at the supplied contig and position
	 * @param contig
	 * @param position
	 * @return List<SAMRecord> records that represent this contig and position
	 */
	public List<SAMRecord> getOverlappingRecordsAtPosition(String contig, int start, int end) throws Exception {
		List<SAMRecord> records = null;
		if ( ! readers.isEmpty()) {
			records = new ArrayList<SAMRecord>();
			for (SamReader reader : readers) {
				SAMRecordIterator iter = reader.queryOverlapping(contig, start, end);
				while (iter.hasNext()) {
					records.add(iter.next());
				}
				iter.close();
			}
		}
		return records;
	}
	
	public List<SAMRecord> getOverlappingRecordsAtPosition(String contig, int position) throws Exception {
		return getOverlappingRecordsAtPosition(contig, position, position);
	}
	
	public void setupReader(String fileName, String indexFileName) {
		readers.add(SAMFileReaderFactory.createSAMFileReader(new File(fileName), new File(indexFileName)));
	}
	
	public void setupReader(String ...fileNames) {
		if (null != fileNames) {
			File [] files = new File[fileNames.length];
			int i = 0;
			for (String s : fileNames) {
				files[i++] = new File(s);
			}
			setupReader(files);
		}
	}
//	public void setupReader(File file) {
//		if (FileUtils.canFileBeRead(file)) {
//			reader = SAMFileReaderFactory.createSAMFileReader(file);
//			reader.enableIndexCaching(true);
////			reader.enableIndexMemoryMapping(false);
//		} else throw new IllegalArgumentException("File can not be read: " + file.getAbsolutePath());	
//	}
	public void setupReader(File ... files) {
		if (null != files) {
			for (File f : files) {
				if (FileUtils.canFileBeRead(f)) {
					readers.add(SAMFileReaderFactory.createSAMFileReader(f));
				} else throw new IllegalArgumentException("File can not be read: " + f.getAbsolutePath());	
			}
		}
		
	}
	
	public void closeReader() throws IOException {
		if ( ! readers.isEmpty()) {
			for (SamReader smf : readers) smf.close();
		}
	}
	
}
