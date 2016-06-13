/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.split;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTagUtil;

import org.qcmg.picard.RnameFile;
import org.qcmg.picard.SAMFileReaderFactory;

public class Split {
	static final Pattern colonDelimitedPattern = Pattern.compile("[:]+");
	final HashMap<Integer, String> zcToFileNameMap = new HashMap<Integer, String>();
	final HashMap<String, Integer> fileNameToZcMap = new HashMap<String, Integer>();
	final HashMap<Integer, SAMFileWriter> zcToWriterMap = new HashMap<Integer, SAMFileWriter>();
	final HashMap<Integer, SAMFileHeader> zcToOutputHeaderMap = new HashMap<Integer, SAMFileHeader>();
	final ArrayList<File> outputNames = new ArrayList<File>();
	boolean useFileNames;
	boolean createIndex;
	String outputDirName;
	SplitType type;
	SAMFileHeader header;
	SamReader reader;
	String inputFileName;
	String validation;
	
	private final static SAMTagUtil samTagUtil = SAMTagUtil.getSingleton();
	final static short ZC = samTagUtil.makeBinaryTag("ZC");
	
	// default constructor
	public Split() {}

	/**
	 * Called by Main
	 * 
	 * @param options
	 * @throws Exception
	 */
	public Split(final Options options) throws Exception {
		options.detectBadOptions();
		useFileNames = !options.hasNumberedOption();
		createIndex = options.hasCreateIndexOption();
		type = getType(options);
		outputDirName = options.getDirNames()[0];
		inputFileName = options.getInputFileNames()[0];
		validation = options.getValidation();
		reader = SAMFileReaderFactory.createSAMFileReader(new File(inputFileName), validation);
		try {
			header = reader.getFileHeader();
			extractHeaderReadGroupDetails();
			prepareOutputHeaders();
			openWriters();
			performSplit();
			closeWriters();
		} finally {
			reader.close();
		}
	}

	/**
	 * Used by the test classes
	 * 
	 * @param inputFileName
	 * @param outputDirName
	 * @param useFileNames
	 * @param type
	 * @throws Exception
	 */
	public Split(final String inputFileName, final String outputDirName,
			final boolean useFileNames, final SplitType type) throws Exception {
		this.inputFileName = inputFileName;
		this.type = type;
		this.outputDirName = outputDirName;
		this.useFileNames = useFileNames;
		this.createIndex = false;
		reader = SAMFileReaderFactory.createSAMFileReader(new File(inputFileName));
	
		try {
			header = reader.getFileHeader();
			extractHeaderReadGroupDetails();
			prepareOutputHeaders();
			openWriters();
			performSplit();
			closeWriters();
		} finally {
			reader.close();
		}
	}

	public Set<Integer> getAllZcs() {
		return zcToFileNameMap.keySet();
	}

	public Collection<String> getOriginalFileNames() {
		return zcToFileNameMap.values();
	}

	public String getOriginalFileName(Integer zc) {
		return zcToFileNameMap.get(zc);
	}

	public Integer getZcFromOriginalFileName(String originalFileName) {
		return fileNameToZcMap.get(originalFileName);
	}

	void extractHeaderReadGroupDetails() throws Exception {
		for (SAMReadGroupRecord record : header.getReadGroups()) {
			String zc = getAttributeZc(record);
			if (null == zc) {
				throw new Exception(
						"Input file header has RG fields lacking zx attributes");
			}
		
			String[] params = colonDelimitedPattern.split(zc);
			Integer zcInt = Integer.parseInt(params[0]);
			String fileName = params[1];
			String previous = zcToFileNameMap.put(zcInt, fileName);
			if (null != previous && !previous.equals(fileName)) {
				throw new Exception(
						"Input file header contains conflicting output file details for ZC value "
								+ zcInt);
			}
			Integer previousZc = fileNameToZcMap.put(fileName, zcInt);
			if (null != previousZc && previousZc != zcInt) {
				throw new Exception(
						"Malformed merged BAM file. Multiple ZC-to-originating-file mappings.");
			}
		}
	}
	static String getAttributeZc(SAMReadGroupRecord  record) throws Exception {
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
	
	static Integer getAttributeZc(SAMProgramRecord record) throws Exception {
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
	
	void prepareOutputHeaders() throws Exception {
		for (final SAMReadGroupRecord record : header.getReadGroups()) {
			String zc = getAttributeZc(record);
			String[] params = colonDelimitedPattern.split(zc);
			Integer zcInt = Integer.parseInt(params[0]);
			SAMFileHeader outputHeader = header.clone();
			preserveZCReadGroup(outputHeader, zcInt);
			conserveProgramRecords(outputHeader, zcInt);
			zcToOutputHeaderMap.put(zcInt, outputHeader);
		}
		stripZCsFromOutputHeader();
	}

	void stripZCsFromOutputHeader() {
		for (SAMFileHeader outputHeader : zcToOutputHeaderMap.values()) {
			for (final SAMReadGroupRecord record : outputHeader.getReadGroups()) {
				record.setAttribute("zc", null);
			}
			for (final SAMProgramRecord record : outputHeader
					.getProgramRecords()) {
				record.setAttribute("zc", null);
			}
		}
	}

	static void conserveProgramRecords(SAMFileHeader outputHeader,
			Integer zcInt) throws Exception {
		Vector<SAMProgramRecord> keepers = new Vector<SAMProgramRecord>();
		for (final SAMProgramRecord programRecord : outputHeader
				.getProgramRecords()) {
			Integer zc = getAttributeZc( programRecord);
			if (null == zc) {
				keepers.add(programRecord);
			} else {
				if (zcInt == zc) {
					keepers.add(programRecord);
				}
			}
		}
		outputHeader.setProgramRecords(keepers);
	}

	void preserveZCReadGroup(SAMFileHeader outputHeader, Integer zcInt) throws Exception{
		Vector<SAMReadGroupRecord> keepers = new Vector<SAMReadGroupRecord>();
		for (final SAMReadGroupRecord readGroupRecord : outputHeader
				.getReadGroups()) {
			String zc =  getAttributeZc( readGroupRecord );
			 
			String[] params = colonDelimitedPattern.split(zc);
			Integer otherZcInt = Integer.parseInt(params[0]);
			if (otherZcInt == zcInt) {
				keepers.add(readGroupRecord);
			}
		}
		outputHeader.setReadGroups(keepers);
	}

	void openWriters() throws Exception {
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		factory.setCreateIndex(createIndex);
		for (Integer zc : zcToFileNameMap.keySet()) {
			String fileName;
			File outputFile;
			if (useFileNames) {
				String absoluteFileName = zcToFileNameMap.get(zc);
				File file = new File(absoluteFileName);
				fileName = file.getName();
				outputFile = new File(outputDirName, fileName);
			} else {
				fileName = zc.toString() + type.getFileExtension();
				outputFile = new File(outputDirName, fileName);
			}
			if (!outputFile.exists()) {
				SAMFileHeader outputHeader = zcToOutputHeaderMap.get(zc);
				SAMFileWriter outputWriter = factory.makeSAMOrBAMWriter(
						outputHeader, true, outputFile);
				outputNames.add(outputFile);
				zcToWriterMap.put(zc, outputWriter);
			} else {
				closeWriters();
				throw new Exception(
						"Output file in the specified output directory alread exists: "
								+ fileName);
			}
		}
	}

	void performSplit() throws Exception {
		for (SAMRecord record : reader) {
			Object zc = record.getAttribute(ZC);
			// OJH - cut down on the number of lookups to the map
			if (null == zc) {
				closeWriters();
				throw new Exception("Input file contains records lacking ZC integer attribute");
			}
			
			SAMFileWriter writer = zcToWriterMap.get(zc);
			
			if (null == writer) {
				closeWriters();
				throw new Exception("Input file contains records lacking ZC integer attribute");
			}
			
			record.setAttribute("ZC", null);
			writer.addAlignment(record);
		}
	}

	void closeWriters() {
		for (SAMFileWriter writer : zcToWriterMap.values()) {
			writer.close();			
		}
		
		//rename the index file; we don't want to exception happen if rename failed
		if(createIndex){
			for(File out: outputNames){
				try{
					RnameFile.renameIndex(out);
				}catch(Exception e){
					 System.out.println(e.toString());
				}
			}
		}
	}

	SplitType getType(Options options) throws Exception {
		SplitType result = null;
		if (options.hasBamOption()) {
			result = new BamSplitType();
		} else if (options.hasSamOption()) {
			result = new SamSplitType();
		} else {
			result = new SamSplitType();
		}
		return result;
	}
}
