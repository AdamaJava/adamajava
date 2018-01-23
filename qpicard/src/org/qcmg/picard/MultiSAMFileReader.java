/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;

public final class MultiSAMFileReader implements Closeable, Iterable<SAMRecord> {
	private static final Pattern colonDelimitedPattern = Pattern.compile("[:]+");
	private final Set<Integer> taken = new HashSet<Integer>(8);
	private final Vector<SamReader> readers = new Vector<SamReader>();
	private final Vector<SAMFileHeader> headers = new Vector<SAMFileHeader>();
	private final Map<SamReader, File> fileMap = new HashMap<SamReader, File>(8);
	private final Map<SamReader, Set<Integer>> oldZcs = new HashMap<SamReader, Set<Integer>>(8);
	private final Map<SamReader, Map<Integer, Integer>> replacementZcs = new HashMap<SamReader, Map<Integer, Integer>>(8);
	private final Map<SamReader, Integer> defaultZcs = new HashMap<SamReader, Integer>(8);
	private MultiSAMFileIterator activeIterator = null;

	
	/**
	 * constructing MultiSamReader based on filesets and ValidationStringency
	 */
	public MultiSAMFileReader(final Set<File> files, ValidationStringency validation ) throws Exception {
		
		for (final File file : files) {
			
			final SamReader reader = SAMFileReaderFactory.createSAMFileReader(file, validation);
			final SAMFileHeader header = reader.getFileHeader();
			if (SAMFileHeader.SortOrder.coordinate != header.getSortOrder()) {
				throw new Exception("Input files must be coordinate sorted");
			}
			final Set<Integer> zcs = new HashSet<Integer>(8);
			for (SAMReadGroupRecord record : header.getReadGroups()) {
				final String attribute  = getAttributeZc( record);
				if (null != attribute ) {				 
					final String[] params = colonDelimitedPattern.split(attribute);
					//get the integer, eg. get "1" from "zc:1:test1.sam"
					final Integer zcInt = Integer.parseInt(params[0]);
					zcs.add(zcInt);
					taken.add(zcInt);					
				}
			}
			
			//store current zc value into taken
			for (SAMProgramRecord record : header.getProgramRecords()) {
				final Integer attribute = getAttributeZc( record);
				if ( null != attribute){
					zcs.add(attribute);
					taken.add(attribute);	
				}			 
			}
			oldZcs.put(reader, zcs);
			readers.add(reader);
			headers.add(header);
			fileMap.put(reader, file);
		}
		for (final SamReader reader : readers) {
			final SAMFileHeader header = reader.getFileHeader();
			for (SAMReadGroupRecord record : header.getReadGroups()) {
				final String obj = getAttributeZc(record);
				if (null == obj) {
					Integer defaultZc = getNextAvailableZc();
					taken.add(defaultZc);
					defaultZcs.put(reader, defaultZc);
					break;
				}
			}
			if (null == defaultZcs.get(reader)) {
				Integer defaultZc = getNextAvailableZc();
				taken.add(defaultZc);
				defaultZcs.put(reader, defaultZc);				
			}
		}
		evaluateReplacementZcs();
	}
	
	public MultiSAMFileReader(final Set<File> files) throws Exception {
		this(files, null);
	}
	
	/**
	 * Constructor that just allows the iterator to be setup.
	 * Doesn't do anything with zc attributes
	 * 
	 * @param files
	 * @param iteratorOnly
	 * @throws Exception
	 */
	public MultiSAMFileReader(final Set<File> files, final boolean iteratorOnly) throws Exception {
		this(files, iteratorOnly, null);
	}
	
	/**
	 * Constructor that just allows the iterator to be setup.
	 * Doesn't do anything with zc attributes
	 * Sets the validation Stringency directly (can be null)
	 * 
	 * @param files
	 * @param iteratorOnly
	 * @throws Exception
	 */
	public MultiSAMFileReader(final Set<File> files, final boolean iteratorOnly, String validation) throws Exception {
		for (final File file : files) {
			final SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(file.getAbsolutePath(), validation);
			final SAMFileHeader header = reader.getFileHeader();
			if (SAMFileHeader.SortOrder.coordinate != header.getSortOrder()) {
				throw new Exception("Input files must be coordinate sorted");
			}
			readers.add(reader);
		}
	}
//	public MultiSAMFileReader(final Set<File> files, final boolean iteratorOnly, String validation) throws Exception {
//		for (final File file : files) {
//			final SamReader reader = SAMFileReaderFactory.createSAMFileReader(file, validation);
//			final SAMFileHeader header = reader.getFileHeader();
//			if (SAMFileHeader.SortOrder.coordinate != header.getSortOrder()) {
//				throw new Exception("Input files must be coordinate sorted");
//			}
//			readers.add(reader);
//		}
//	}
	
	public String getAttributeZc(SAMReadGroupRecord  record) throws Exception {
		String lowZc = record.getAttribute("zc");
		String upZc = record.getAttribute("ZC");
		
		//Both "zc" and "ZC" tag can't exist at same  RG line
		if(lowZc != null && upZc != null)
			throw new Exception("Bad RG line: contains both upcase and lowcase tag (zc,ZC)");
		//convert old ZC:Z:<int>:<file> ==> zc:<i>:<file>
		else if( lowZc == null && upZc != null ){
			String[] params = colonDelimitedPattern.split(upZc);
			if (3 != params.length) {
				throw new Exception("Bad RG:ZC format: " + upZc);
			}			
			return String.format("%s:%s", params[1],params[2]);
		}
		else if(lowZc != null && upZc == null )
			return lowZc;		
		
		
		return null;
	}
	
	public Integer getAttributeZc(SAMProgramRecord record) throws Exception {
		String lowZc = record.getAttribute("zc");
		String upZc = record.getAttribute("ZC");
		
		Integer value = null;
		
		try{
			if(lowZc != null && upZc != null)
				throw new Exception("Bad PG line: contains both upcase and lowcase tag (zc,ZC)");
			else if(lowZc != null && upZc == null )
				value = Integer.parseInt(lowZc);			
			else if( lowZc == null && upZc != null ){
				String[] params = colonDelimitedPattern.split(upZc);
				value = Integer.parseInt(params[1]);
			}
		}catch(NumberFormatException e ){
			throw new Exception("non integer value assigned on tag PG:zc");
		}

		return value;
	}

	public Vector<SamReader> getSAMFileReaders() {
		return readers;
	}
	
	public Vector<SAMFileHeader> getSAMFileHeaders() {
		return headers;
	}
	
	public Set<Integer> getOldZcs(SamReader reader) {
		return oldZcs.get(reader);
	}

	public Map<Integer, Integer> getReplacementZcs(SamReader reader) {
		return replacementZcs.get(reader);
	}

	public Integer getDefaultZc(SamReader reader) {
		return defaultZcs.get(reader);
	}

	@Override
	public synchronized Iterator<SAMRecord> iterator() throws IllegalStateException {
		return getMultiSAMFileIterator();
	}

	public synchronized MultiSAMFileIterator getMultiSAMFileIterator() throws IllegalStateException {
		if (null != activeIterator) {
			throw new IllegalStateException("Reader already iterating. Close the reader and retry.");
		}
		activeIterator = new MultiSAMFileIterator(readers);
		return activeIterator;
	}

	public File getFile(SamReader reader) {
		return fileMap.get(reader);
	}

	@Override
	public synchronized void close() throws IOException {
		activeIterator = null;
		for (final SamReader reader : readers) {
			reader.close();
		}
	}

	private void evaluateReplacementZcs() throws Exception {
		for (SamReader reader : readers) {
			Map<Integer, Integer> replacements = new HashMap<Integer, Integer>();
			replacementZcs.put(reader, replacements);
			Set<Integer> oldZcs = getOldZcs(reader);
			for (Integer oldZc : oldZcs) {
				if (taken.contains(oldZc)) {
					Integer newZc = getNextAvailableZc();
					taken.add(newZc);
					replacements.put(oldZc, newZc);
				} else {
					taken.add(oldZc);
				}
			}
		}
	}

	public Integer getNextAvailableZc() {
		int result = 0;
		while (taken.contains(result)) {
			result++;
		}
		return result;
	}
}
