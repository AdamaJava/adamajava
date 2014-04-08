/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.io.Closeable;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;

public final class MultiSAMFileReader implements Closeable, Iterable<SAMRecord> {
	private static final Pattern colonDelimitedPattern = Pattern.compile("[:]+");
	private final HashSet<Integer> taken = new HashSet<Integer>();
	private final Vector<SAMFileReader> readers = new Vector<SAMFileReader>();
	private final Vector<SAMFileHeader> headers = new Vector<SAMFileHeader>();
	private final Map<SAMFileReader, File> fileMap = new HashMap<SAMFileReader, File>();
	private final Map<SAMFileReader, HashSet<Integer>> oldZcs = new HashMap<SAMFileReader, HashSet<Integer>>();
	private final HashMap<SAMFileReader, HashMap<Integer, Integer>> replacementZcs = new HashMap<SAMFileReader, HashMap<Integer, Integer>>();
	private final Map<SAMFileReader, Integer> defaultZcs = new HashMap<SAMFileReader, Integer>();
	private MultiSAMFileIterator activeIterator = null;

	
	/**
	 * constructing MultiSAMFileReader based on filesets and ValidationStringency
	 */
	public MultiSAMFileReader(final HashSet<File> files, SAMFileReader.ValidationStringency validation ) throws Exception {
		
		for (final File file : files) {
			
			final SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(file, validation);
			final SAMFileHeader header = reader.getFileHeader();
			if (SAMFileHeader.SortOrder.coordinate != header.getSortOrder()) {
				throw new Exception("Input files must be coordinate sorted");
			}
			final HashSet<Integer> zcs = new HashSet<Integer>();
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
		for (final SAMFileReader reader : readers) {
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
	
	public MultiSAMFileReader(final HashSet<File> files) throws Exception {
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
			final SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(file, validation);
			final SAMFileHeader header = reader.getFileHeader();
			if (SAMFileHeader.SortOrder.coordinate != header.getSortOrder()) {
				throw new Exception("Input files must be coordinate sorted");
			}
			readers.add(reader);
		}
	}
	
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

	public Vector<SAMFileReader> getSAMFileReaders() {
		return readers;
	}
	
	public Vector<SAMFileHeader> getSAMFileHeaders() {
		return headers;
	}
	
	public HashSet<Integer> getOldZcs(SAMFileReader reader) {
		return oldZcs.get(reader);
	}

	public HashMap<Integer, Integer> getReplacementZcs(SAMFileReader reader) {
		return replacementZcs.get(reader);
	}

	public HashSet<Integer> getTakenZcs() {
		return taken;
	}

	public Integer getDefaultZc(SAMFileReader reader) {
		return defaultZcs.get(reader);
	}

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

	public File getFile(SAMFileReader reader) {
		return fileMap.get(reader);
	}

	public synchronized void close() {
		activeIterator = null;
		for (final SAMFileReader reader : readers) {
			reader.close();
		}
	}

	private void evaluateReplacementZcs() throws Exception {
		for (SAMFileReader reader : readers) {
			HashMap<Integer, Integer> replacements = new HashMap<Integer, Integer>();
			replacementZcs.put(reader, replacements);
			HashSet<Integer> oldZcs = getOldZcs(reader);
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
