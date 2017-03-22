/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.split;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.RnameFile;
import org.qcmg.picard.SAMFileReaderFactory;

public class SingleLevelSplit extends Split {
	private static final String QBM = "qbammerge";
	
	Map<Integer, String> singleSplitMap = new HashMap<Integer, String>();
	private String commandLine;
	
	private final QLogger logger;
	
	/**
	 * Called by Main
	 * 
	 * @param options
	 * @throws Exception
	 */
	public SingleLevelSplit(final Options options) throws Exception {
		options.detectBadOptions();
		useFileNames = !options.hasNumberedOption();
		createIndex = options.hasCreateIndexOption();
		type = getType(options);
		outputDirName = options.getDirNames()[0];
		inputFileName = options.getInputFileNames()[0];
		reader = SAMFileReaderFactory.createSAMFileReader(new File(inputFileName));
		logger = QLoggerFactory.getLogger(SingleLevelSplit.class);
		commandLine = options.getCommandLine();
		try {
			header = reader.getFileHeader();
			extractHeaderReadGroupDetails();
			
			// single level stuff
			if (null != options.getSplit1String()) {
				setupSingleSplitFromOptions(options);
			} else {
				setupSingleSplit();
			}
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
	public SingleLevelSplit(final String inputFileName, final String outputDirName,
			final boolean useFileNames, final SplitType type) throws Exception {
		this.inputFileName = inputFileName;
		this.type = type;
		this.outputDirName = outputDirName;
		this.useFileNames = useFileNames;
		this.createIndex = false;
		reader = SAMFileReaderFactory.createSAMFileReader(new File(inputFileName));
		logger = QLoggerFactory.getLogger(SingleLevelSplit.class);
		try {
			header = reader.getFileHeader();
			extractHeaderReadGroupDetails();
			prepareOutputHeaders();
			
			// single level stuff
			setupSingleSplit();
			
		} finally {
			reader.close();
		}
	}
	
	@Override
	void closeWriters() {
		Set<SAMFileWriter> writers = new HashSet<SAMFileWriter>(zcToWriterMap.values());
		logger.info("Number of writers to close: " + writers.size());
		for (SAMFileWriter writer : writers) {
			writer.close();
		}
		
		//rename the index file; we don't want to exception happen if rename failed
		if(createIndex){
			for(File out: outputNames){
				try{
					RnameFile.renameIndex(out);
					logger.info("rename index to " + out.getPath() + ".bai");
				}catch(Exception e){
					logger.error(e.toString());
				}
			}
		}
	}
	
	private void setupSingleSplitFromOptions(Options options) throws SplitException, IOException {
		
		for (Integer i : options.getSplit1ZCs()) {
			singleSplitMap.put(i, options.getSplit1String());
		}
		for (Integer i : options.getSplit2ZCs()) {
			singleSplitMap.put(i, options.getSplit2String());
		}
		
		checkSingleSplitMap();
		
	}
	private void setupSingleSplit() throws SplitException, IOException {
		
		List<SAMProgramRecord> pgs = header.getProgramRecords();
		SAMProgramRecord latestMerge = getLatestMerge(pgs);
		
		logger.info("latestMerge: " + latestMerge.getCommandLine());
		logger.info("group id: " + latestMerge.getProgramGroupId());
		logger.info("zc: " + latestMerge.getAttribute("zc"));
		
		List<String> inputs = getProgramRecordInputs(latestMerge);
		determineSingleSplitOutputs(inputs, singleSplitMap, pgs, null);
		
		checkSingleSplitMap(inputs);
		
	}
	
	private void checkSingleSplitMap() throws SplitException {
		// sanity check to ensure that all zc's have been accounted for
		StringBuilder sb = new StringBuilder();
		for (Integer zc : zcToFileNameMap.keySet()) {
			if ( ! singleSplitMap.containsKey(zc)) {
				if (sb.length() > 0) sb.append('\n');
				sb.append("no entry in singleSplitMap for zc: " + zc);
			}
		}
		if (sb.length() != 0) {
			logger.error(sb.toString());
			throw new SplitException(sb.toString());
		}
	}
	
	private void checkSingleSplitMap(List<String> inputs) throws SplitException {
		for (String input : inputs) {
			StringBuilder sb = new StringBuilder();
			for (Entry<Integer, String> entry : singleSplitMap.entrySet()) {
				if (entry.getValue().equals(input)) {
					if (sb.length() > 0) sb.append('\n');
					
					sb.append(zcToFileNameMap.get(entry.getKey()));
				}
			}
			if (sb.length() == 0) {
				logger.error("Could not find zc's for input: " + input);
				throw new SplitException("Could not find zc's for input: " + input);
			} else {
				logger.info("");
				logger.info("output file: " + input + " will contain data from the following bams: \n" + sb.toString());
			}
		}
	}

	private void determineSingleSplitOutputs(List<String> inputs, Map<Integer, String> singleSplitMap, List<SAMProgramRecord> pgs, String parentInput) throws IOException {
		for (String s : inputs) {
			boolean matchFound = false;
			
			// if the input is in the zcToFileMap, we are done - add to singleSplitMap and move on
			//here fileNameToZcMap store bams lists on RG
			for (Entry<String,Integer> entry : fileNameToZcMap.entrySet()) {
				String linkedFile = getLinkedFilename(s);
				String linkedFileName = new File(linkedFile).getName();
				if (entry.getKey().equals(s) || entry.getKey().equals(linkedFile) || entry.getKey().endsWith(linkedFileName)) {
					// we've got a match
					if (null != parentInput) {
						singleSplitMap.put(entry.getValue(), parentInput);
					} else {
						singleSplitMap.put(entry.getValue(), s);
					}
					matchFound = true;
					break;
				}
			}
			 
			if (matchFound) continue;
			
			// need to find program record for input
			
			SAMProgramRecord linkedMerge = getPGforInput(s, pgs);
			if (null != linkedMerge) {
				logger.info("linkedMerge: " + ((null != linkedMerge) ? linkedMerge.getCommandLine() : "null"));
				determineSingleSplitOutputs(getProgramRecordInputs(linkedMerge), singleSplitMap, pgs, null != parentInput ? parentInput : s);
			} else {
				logger.info("no PG line found for input: " + s);
			}
		}
	}

	static String getLinkedFilename(String filename) throws IOException {
		filename = filename.replace("/panfs/seq_results", "/mnt/seq_results");
		Path path = Paths.get(filename, "");
		
		// if the file exists, return its full (real) path, otherwise, just return the filename
		if (path.toFile().exists())
			return path.toRealPath().toString();
		else
			return filename;
	}
	
	static SAMProgramRecord getPGforInput(String input, List<SAMProgramRecord> pgs) {
		File inputFile = new File(input);
		input = inputFile.getName();
		System.out.println("input: " + input);
		
		//do perfect output name match first
		for (SAMProgramRecord pg : pgs) 
			if (QBM.equals(pg.getProgramName())) {
				String output = getProgramRecordOutputs(pg);
				
				// need to use the filenames as the paths are non-conformant
				File outputFile = new File(output);
				output = outputFile.getName();
				
				if (output.equals(input)) 
					return pg;
				
		}
		
		//if no perfect output match then seek ambiguous match		
		for (SAMProgramRecord pg : pgs) {
			if (QBM.equals(pg.getProgramName())) {
				String output = getProgramRecordOutputs(pg);
				
				// need to use the filenames as the paths are non-conformant
				File outputFile = new File(output);
				output = outputFile.getName();
				if(input.contains("19x.dedup") &&
					(output.length() - 3) >= input.substring(0, input.indexOf("19x.dedup")).length() ){
					// need to handle the 90ND_10CD.19x.dedup cases as these names have changed
					// since the file was merged
					return pg;
				} else if( input.contains(".dedup.bam") ){
					if(output.substring(0, output.indexOf(".bam")).equalsIgnoreCase(input.substring(0, input.indexOf(".dedup.bam"))))  
						return pg;			
				} else if (input.contains("100ND_0CD") && output.endsWith(".ND.bam")) {
					return pg;
				} else if (output.contains(input.substring(0, input.indexOf(".bam")))) {
					return pg;
				} else if (input.contains("COLO_829.HiSeq.genome.TD.bam") && output.contains("COLO_829.genome.TD.illumina.bam")) {
					return pg;
				} else if (input.contains("COLO_829.HiSeq.genome.ND.bam") && output.contains("COLO_829.genome.ND.illumina.bam")) {
					return pg;
				} else if (input.contains("COLO_829.solid_exome_tumour.qxu.bam") && output.contains("COLO_829.exome.TD.bam")) {
					return pg;
				} else if (input.contains("COLO_829.solid_exome_normal.qxu.bam") && output.contains("COLO_829.exome.ND.bam")) {
					return pg;
				} else if (input.contains("hiseq_exome_0CD_100ND") && output.contains("hiseq_exome_0CD_100ND")) {
					return pg;
				} else if (input.contains("hiseq_exome_20CD_80ND") && output.contains("hiseq_exome_20CD_80ND")) {
					return pg;
				} else if (input.contains("hiseq_exome_40CD_60ND") && output.contains("hiseq_exome_40CD_60ND")) {
					return pg;
				} else if (input.contains("hiseq_exome_60CD_40ND") && output.contains("hiseq_exome_60CD_40ND")) {
					return pg;
				} else if (input.contains("hiseq_exome_80CD_20ND") && output.contains("hiseq_exome_80CD_20ND")) {
					return pg;
				} else if (input.contains("hiseq_exome_100CD_0ND") && output.contains("hiseq_exome_100CD_0ND")) {
					return pg;
				}
			}
		}
		return null;
	}
	
	static List<String> getProgramRecordInputs(SAMProgramRecord pg) throws IOException {
		List<String> inputs = new ArrayList<String>();
		
		String cl = pg.getCommandLine();
		String [] params = TabTokenizer.tokenize(cl, ' ');
		
		// loop through array - when we get a "-i", the next param is an input
		boolean get = false;
		for (String p : params) {
			// if any of the inputs are symbolic links, substitute the real files in their place
			if (get) inputs.add(getLinkedFilename(p));
			get = "-i".equals(p) || "--input".equals(p);
		}
		return inputs;
	}
	
	static String getProgramRecordOutputs(SAMProgramRecord pg) {
		String output = null;
		String cl = pg.getCommandLine();
		String [] params = TabTokenizer.tokenize(cl, ' ');
		
		// loop through array - when we get a "-i", the next param is an input
		boolean get = false;
		for (String p : params) {
			if (get) output = p;
			get = "-o".equals(p) || "--output".endsWith(p);
		}
		return output;
	}
	
	
	static SAMProgramRecord getLatestMerge(List<SAMProgramRecord> pgs) {
		// we are looking for the qbammerge program record that has the highest zc attribute
		SAMProgramRecord latestMerge = null;
		
		for (SAMProgramRecord pg : pgs) {
			if (QBM.equals(pg.getProgramName())) {
				if (null == latestMerge) {
					latestMerge = pg;
				} else if (Integer.parseInt(pg.getAttribute("zc")) > Integer.parseInt(latestMerge.getAttribute("zc"))) {
					latestMerge = pg;
				}
			}
		}
		return latestMerge;
	}
	
	
	static boolean doMultipleMergesExist(List<SAMProgramRecord> pgs) {
		int counter = 0;
		
		for (SAMProgramRecord pg : pgs)
			if (QBM.equalsIgnoreCase(pg.getProgramName()))
				counter++;
		
		return counter > 1;
	}

	/**
	 * Overridden method as GATK inserts RG records without the qcmg specific zc tag
	 * and we need to be able to handle that
	 */
	@Override
	void extractHeaderReadGroupDetails() throws Exception {
		for (SAMReadGroupRecord record : header.getReadGroups()) {
			String zc = getAttributeZc(record);
			if (null == zc) {
//						"Input file header has RG fields lacking zx attributes");
				// skip this RG line as we only care about qbammerge RGs here 
				continue;
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
			if (null != previousZc && ! previousZc.equals(zcInt)) {
				throw new Exception(
						"Malformed merged BAM file. Multiple ZC-to-originating-file mappings.");
			}
		}
	}
	
	/**
	 *Overridden method that doesn not remove all entries in the header that have
	 *a zc tag (qcmg specific) in them.
	 *Also, add PG to header line with split command line
	 */
	@Override
	void prepareOutputHeaders() throws Exception {
		for (final SAMReadGroupRecord record : header.getReadGroups()) {
			String zc = getAttributeZc(record);
			String[] params = colonDelimitedPattern.split(zc);
			Integer zcInt = Integer.parseInt(params[0]);
			SAMFileHeader outputHeader = header.clone();
			preserveZCReadGroup(outputHeader, zcInt);
			HeaderUtils.addProgramRecord(outputHeader, "qsplit", Messages.getProgramVersion(), commandLine);
			zcToOutputHeaderMap.put(zcInt, outputHeader);
		}
	}
	
	/**
	 * Overridden method that keeps RG lines for this zc value, and others contributing to the output file
	 */
	@Override
	void preserveZCReadGroup(SAMFileHeader outputHeader, Integer zcInt) throws Exception{
		Vector<SAMReadGroupRecord> keepers = new Vector<SAMReadGroupRecord>();
		List<Integer> linkedZCs = retrieveOtherZCKeepers(zcInt, singleSplitMap);
		for (final SAMReadGroupRecord readGroupRecord : outputHeader.getReadGroups()) {
			String zc =  getAttributeZc( readGroupRecord );
			 
			String[] params = colonDelimitedPattern.split(zc);
			Integer otherZcInt = Integer.parseInt(params[0]);
			if (linkedZCs.contains(otherZcInt)) {
				keepers.add(readGroupRecord);
			}
		}
		outputHeader.setReadGroups(keepers);
	}
	
	private List<Integer> retrieveOtherZCKeepers(int zc, Map<Integer, String> map) {
		String value = map.get(zc);
		List<Integer> linkedZCs = new ArrayList<Integer>();
		if (null != value) {
			for (Entry<Integer, String> entry : map.entrySet()) {
				if (value.equals(entry.getValue())) {
					linkedZCs.add(entry.getKey());
				}
			}
		}
		return linkedZCs;
	}

	/**
	 * Overridden method that uses a different map to the parent method.
	 * This map could contain multiple keys mapping to the same output.
	 */
	@Override
	void openWriters() throws Exception {
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		factory.setCreateIndex(createIndex);
		
		// create map of file to writer objects
		Map<String, SAMFileWriter> splitWriters = new HashMap<String, SAMFileWriter>();
		
		for (Entry<Integer, String> entry : singleSplitMap.entrySet()) {
			Integer zc = entry.getKey();
			String fileName = new File(entry.getValue()).getName();
			
			SAMFileWriter outputWriter = splitWriters.get(fileName);
			if (null == outputWriter) {
				File outputFile = new File(outputDirName, fileName);
				
				if (!outputFile.exists()) {
					
					SAMFileHeader outputHeader = zcToOutputHeaderMap.get(zc);
					outputWriter = factory.makeSAMOrBAMWriter(outputHeader, true, outputFile);
					splitWriters.put(fileName, outputWriter);
					
				} else {
					closeWriters();
					throw new Exception(
							"Output file in the specified output directory alread exists: "
							+ fileName);
				}
				outputNames.add(outputFile);
			} 
			zcToWriterMap.put(zc, outputWriter);
			
		}
	}

	/**
	 * Overridden method that doesn't strip the ZC tag from the SAMRecord
	 * as it is essential if further splits are required
	 */
	@Override
	void performSplit() throws Exception {
		for (SAMRecord record : reader) {
			Object zc = record.getAttribute(ZC);
			if (null == zc) {
				closeWriters();
				throw new Exception("Input file contains records lacking ZC integer attribute");
			}
			
			SAMFileWriter writer = zcToWriterMap.get(zc);

			if (null == writer) {
				closeWriters();
				for (Entry<Integer,SAMFileWriter> entry : zcToWriterMap.entrySet()) {
					System.err.println("entry: " + entry.getKey() + ":" + entry.getValue().toString());
				}
				System.err.println("zc: " + zc);
				System.err.println("record: " + record.getSAMString());
				throw new Exception("Input file contains records lacking ZC integer attribute");
			}
			
			// may need to do a further split, so keep hold of the ZC record attribute
			writer.addAlignment(record);
		}
	}

}
