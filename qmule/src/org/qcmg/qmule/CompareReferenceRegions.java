/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.FileUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;


public class CompareReferenceRegions {	
	
	private static final String MODE_ONEWAY = "oneway";
	private static final String MODE_ANNOTATE = "annotate";
	private static final String MODE_TWOWAY = "twoway";
	private static final String MODE_INTERSECT = "intersect";
	private static final String MODE_UNIQUE = "unique";
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private List<String> chromosomes = new ArrayList<String>();
	private int overlapCount = 0;
	private int notOverlappingCount = 0;
	private int recordCount;
	private String mode;
	private int column;
	private String annotation;
	private static QLogger logger;
	private static final String MAF = "maf";
	private static final String GFF3 = "gff3";
	private static final String GTF = "gtf";
	private static final String BED = "bed";
	private static final String VCF = "vcf";
	private static final String TAB = "txt";
	private static final String DCC1 = "dcc1";

	private void runOnewayComparison(File inputFile, File comparisonFile,
			File outputOverlapFile, File outputNoOverlapFile) throws Exception {
		
		if (mode.equals(MODE_ANNOTATE)) {
			logger.info("If overlapping, will annotate column: " + column+1 +" of file with the annotation " + annotation);
		}
		
		//get a list of the chromosomes
		setUp(inputFile, outputOverlapFile, outputNoOverlapFile);		
		
		logger.info("Input file: " + inputFile.getAbsolutePath());
		logger.info("Comparison file: " + comparisonFile.getAbsolutePath());
		
		logger.info("Chromosomes to analyze: " + chromosomes.size());
		
		for (String c: chromosomes) {
			logger.info("Getting records for chromosome: " + c);
			Map<ChrPosition, TabbedRecord> inputRecords = readRecords(inputFile, c); 
			Map<ChrPosition, TabbedRecord> compareRecords = readRecords(comparisonFile, c);			
			compareRecords(inputRecords, compareRecords, outputOverlapFile, outputNoOverlapFile);			
		}
		logSummary();		
		clear();
	}	
	
	private void logSummary() {
		logger.info("SUMMARY");
		logger.info("Total Records: " + recordCount);
		logger.info("Total Records in supplied reference regions: " + overlapCount);
		logger.info("Total Records not in supplied reference regions: " + notOverlappingCount);		
	}

	private void runAnnotateComparison(File inputFile, File comparisonFile,
			File outputOverlapFile) throws Exception {
		
		//get a list of the chromosomes
		setUp(inputFile, outputOverlapFile, null);		
		
		logger.info("Input file: " + inputFile.getAbsolutePath());
		logger.info("Comparison file: " + comparisonFile.getAbsolutePath());
		
		logger.info("Chromosomes to analyze: " + chromosomes.size());
		
		for (String c: chromosomes) {
			logger.info("Getting records for chromosome: " + c);
			Map<ChrPosition, TabbedRecord> inputRecords = readRecords(inputFile, c); 
			Map<ChrPosition, TabbedRecord> compareRecords = readRecords(comparisonFile, c);			
			compareRecordsAndAnnotate(inputRecords, compareRecords, outputOverlapFile);			
		}
		logSummary();	
		clear();
	}	

	private void runIntersectComparison() throws Exception {
		//Set first input file as primary
		File primaryInputFile = new File(cmdLineInputFiles[0]);
		//Single output file 
		File outputFile = new File(cmdLineOutputFiles[0]);		
		
		int[] counts = new int[cmdLineInputFiles.length];
		counts[0] = 0;
		
		setUp(primaryInputFile, outputFile, null);
		
		//logging
		logger.info("Input file 1: " + primaryInputFile.getAbsolutePath());		
		for (int i=1; i<cmdLineInputFiles.length; i++) {
			File compareFile = new File(cmdLineInputFiles[i]);
			counts[i] = 0;
			int num = i +1;
			logger.info("Input file " + num + ": " + compareFile.getAbsolutePath());
		}
		logger.info("Output file: " + outputFile.getAbsolutePath());	
		
		for (String c: chromosomes) {
			logger.info("Getting records for chromosome: " + c);
			Map<ChrPosition, TabbedRecord> inputRecords = readRecords(primaryInputFile, c); 
			counts[0] += inputRecords.size();
			for (int i=1; i<cmdLineInputFiles.length; i++) {
				File compareFile = new File(cmdLineInputFiles[i]);
				 
				Map<ChrPosition, TabbedRecord> compareRecords = readRecords(compareFile, c);
				counts[i] += compareRecords.size();
				compareOverlapRecords(c, inputRecords, compareRecords, getFileType(primaryInputFile));	
			}		
			overlapCount += inputRecords.size();
			//any input records left at the end are intersecting
			writeRecords(inputRecords, outputFile);			
		}
		for (int i=0; i<cmdLineInputFiles.length; i++) {
			logger.info(counts[i] + " total records for file " +cmdLineInputFiles[i]);
		}
		logger.info("Total intersecting records: " + overlapCount);		
	}
	
	private void runUniqueComparison() throws Exception {
		
		for (int f=0; f<cmdLineInputFiles.length; f++) {
			notOverlappingCount = 0;
			//Set first input file as primary
			File primaryInputFile = new File(cmdLineInputFiles[f]);
			//Single output file 
			File outputFile = new File(cmdLineOutputFiles[f]);		
			
			int[] counts = new int[cmdLineInputFiles.length];
			counts[0] = 0;
			
			setUp(primaryInputFile, outputFile, null);
			
			//logging
			logger.info("File to find unique records: " + primaryInputFile.getAbsolutePath());	
			
			for (int i=0; i<cmdLineInputFiles.length; i++) {
				if (i != f) {
					File compareFile = new File(cmdLineInputFiles[i]);
					counts[i] = 0;
					logger.info("Comparison input files: " + compareFile.getAbsolutePath());
				}
			}
			logger.info("Output file: " + outputFile.getAbsolutePath());			
			
			for (String c: chromosomes) {
				logger.info("Getting records for chromosome: " + c);
				Map<ChrPosition, TabbedRecord> inputRecords = readRecords(primaryInputFile, c); 
				Map<ChrPosition, TabbedRecord> compareRecords = new TreeMap<ChrPosition, TabbedRecord>();
				counts[f] += inputRecords.size();
				for (int i=0; i<cmdLineInputFiles.length; i++) {					
					if (i != f) {						
						File compareFile = new File(cmdLineInputFiles[i]);				 
						Map<ChrPosition, TabbedRecord> currentRecords = readRecords(compareFile, c);
						counts[i] = counts[i] + currentRecords.size();
						compareRecords.putAll(currentRecords);						
					}
				}
				compareOverlapRecords(c, inputRecords, compareRecords, getFileType(primaryInputFile));
				notOverlappingCount += inputRecords.size();
				//any input records left at the end are unique
				writeRecords(inputRecords, outputFile);	
				logger.info(counts[f] + " total records for file " +cmdLineInputFiles[f]);
				for (int i=0; i<cmdLineInputFiles.length; i++) {
					if (i != f) {
						logger.info(counts[i] + " total records for file " +cmdLineInputFiles[i]);
					}
				}
				logger.info("Total unique records: " + notOverlappingCount);
			}				
		}			
	}

	public void compareOverlapRecords(String chromosome, Map<ChrPosition, TabbedRecord> inputRecords, Map<ChrPosition, TabbedRecord> compareRecords, String inputFileType) throws Exception {
		
		Iterator<Entry<ChrPosition, TabbedRecord>> entries = inputRecords.entrySet().iterator();
		while (entries.hasNext()) {
		    Entry<ChrPosition, TabbedRecord> entry = entries.next();
		    			
			boolean isOverlapping = compareRecord(entry, compareRecords, inputFileType);		 
		
			if (mode.equals(MODE_INTERSECT) && !isOverlapping) {
				//remove input record if it isn't overlapping and won't intersect with all records
				entries.remove();				
			}
			if (mode.equals(MODE_UNIQUE) && isOverlapping) {				
				entries.remove();				
			}
		}			
	}
	
	private void compareRecordsAndAnnotate(Map<ChrPosition, TabbedRecord> inputRecords,
			Map<ChrPosition, TabbedRecord> compareRecords,
			File outputOverlapFile) throws Exception {
		BufferedWriter overlapWriter = new BufferedWriter(new FileWriter(outputOverlapFile, true));
		
		try {
			for (Entry<ChrPosition, TabbedRecord> entry : inputRecords.entrySet()) {				
				recordCount++;				
				boolean isOverlapping = compareRecord(entry, compareRecords, null);		 
			
				if (isOverlapping) {					
					overlapCount++;					
				} else {
					notOverlappingCount++;
				}		
				writeRecord(overlapWriter, entry.getValue());
			}
		} finally {
			overlapWriter.close();
		}		
	}

	private void compareRecords(Map<ChrPosition, TabbedRecord> inputRecords,
			Map<ChrPosition, TabbedRecord> compareRecords,
			File outputOverlapFile, File outputNoOverlapFile) throws Exception {
		BufferedWriter overlapWriter = new BufferedWriter(new FileWriter(outputOverlapFile, true));
		BufferedWriter noOverlapWriter = new BufferedWriter(new FileWriter(outputNoOverlapFile, true));
		
		try {
			for (Entry<ChrPosition, TabbedRecord> entry : inputRecords.entrySet()) {
				
				recordCount++;			
				
				boolean isOverlapping = compareRecord(entry, compareRecords, null);		 
			
				if (isOverlapping) {					
					overlapCount++;
					writeRecord(overlapWriter, entry.getValue());
				} else {
					notOverlappingCount++;
					if (mode.equals(MODE_ANNOTATE)) {
						
					} else {
						writeRecord(noOverlapWriter, entry.getValue());
					}
				}			
			}
		} finally {
			overlapWriter.close();
			noOverlapWriter.close();
		}		
	}
	
	private boolean compareRecord(Entry<ChrPosition, TabbedRecord> entry, Map<ChrPosition, TabbedRecord> compareRecords, String inputFileType) throws Exception {
		ChrPosition inputChrPos = entry.getKey();
		TabbedRecord inputRecord = entry.getValue();
		boolean isOverlapping = false;
		//check to see if it is overlapping with the comparison reference region
		for (Entry<ChrPosition, TabbedRecord> compareEntry : compareRecords.entrySet()) {
			ChrPosition comparePos = compareEntry.getKey();
			if (comparePos.getEndPosition() < inputChrPos.getStartPosition()) {
				continue;
			} else if (comparePos.getStartPosition() > inputChrPos.getEndPosition()) {
				break;
			} else {
				if (tabbedRecordFallsInCompareRecord(inputChrPos, inputRecord, compareEntry)) {								
					isOverlapping = true;							
					if (mode.equals(MODE_ANNOTATE)) {
						String[] values = inputRecord.getDataArray();
						String oldVal = values[column];
						if (oldVal.equals("")) {
							values[column] = annotation;
						} else {
							if (oldVal.endsWith(";")) {
								values[column] = oldVal + annotation;
							} else {
								values[column] = oldVal + ";" + annotation;
							}
						}
						String data = "";
						for (String s: values) {
							data += s + "\t";
						}
						inputRecord.setData(data);
					}
					if (mode.equals(MODE_INTERSECT)) {
						//change the ends??
						int[] indexes = getChrIndex(inputFileType, entry.getValue().getData().split("\t"));
						String[] array = inputRecord.getDataArray();
						
						if (inputChrPos.getStartPosition() > compareEntry.getKey().getStartPosition()) {							
							array[indexes[1]] =  Integer.toString(compareEntry.getKey().getStartPosition());
						}
						if (inputChrPos.getEndPosition() < compareEntry.getKey().getEndPosition()) {
							array[indexes[2]] =  Integer.toString(compareEntry.getKey().getEndPosition());
						}
						String data = "";
						for (String s: array) {
							data += s + "\t";
						}
						inputRecord.setData(data);
						entry.setValue(inputRecord);
					}
				}
			}
		}
		return isOverlapping;
	}
	
	
	private void writeRecords(Map<ChrPosition, TabbedRecord> records, File outputFile) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
		
		for (Entry<ChrPosition, TabbedRecord> entry: records.entrySet()) {
			writeRecord(writer, entry.getValue());
		}
		writer.close();
	}

	private void writeRecord(BufferedWriter writer, TabbedRecord record) throws IOException {
		if (!record.getData().endsWith("\n")) {
			record.setData(record.getData() + "\n");
		}
		writer.write(record.getData());		
	}

	private TreeMap<ChrPosition, TabbedRecord> readRecords(File inputFile, String chromosome) throws Exception {
		
		TabbedFileReader reader = new TabbedFileReader(inputFile);
		TreeMap<ChrPosition, TabbedRecord> records = new TreeMap<ChrPosition, TabbedRecord>();
		String fileType = getFileType(inputFile);
			try {		
				
				Iterator<TabbedRecord> iterator = reader.getRecordIterator();

				while (iterator.hasNext()) {
					
					TabbedRecord tab = iterator.next();					
					if (tab.getData().startsWith("#") || tab.getData().startsWith("Hugo") || tab.getData().startsWith("analysis") || tab.getData().startsWith("Chr") ) {					
						continue;
					}
					ChrPosition chrPos = getChrPosition(fileType, tab);								
					if (chrPos.getChromosome().equals(chromosome)) {
						records.put(chrPos, tab);				
					}
				}
				
			} finally {		
				reader.close();
			}		
			
		return records;
	}
		
	private String getFileType(File inputFile) {
		int index = inputFile.getName().lastIndexOf(".") + 1;
		String name = inputFile.getName().substring(index, inputFile.getName().length());
		
		if (name.equals("dcc")) {
			return "dcc1";
		}
		
		return name;
	}

	private void setUp(File file, File outputFileOne, File outputFileTwo) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(file);
		Iterator<TabbedRecord> iterator = reader.getRecordIterator();
		
		String fileType = getFileType(file);
		List<String> header = new ArrayList<String>();
		if (reader.getHeader() != null) {
			Iterator<String> iter = reader.getHeader().iterator();
			while (iter.hasNext()) {					
				header.add(iter.next());
			}
		}
		
		while (iterator.hasNext()) {
			
			TabbedRecord tab = iterator.next();	
			
			if (tab.getData().startsWith("#") || tab.getData().startsWith("Hugo") || tab.getData().startsWith("analysis") || tab.getData().startsWith("Chr") ) {					
				header.add(tab.getData());
				continue;
			}
		
			ChrPosition	chrPos = getChrPosition(fileType, tab);										
			
			if (!chromosomes.contains(chrPos.getChromosome())) {
				chromosomes.add(chrPos.getChromosome());
			}
		}
		reader.close();
		
		if (outputFileOne != null) {
			writeHeader(header, outputFileOne);
		} 
		if (outputFileTwo != null) {
			writeHeader(header, outputFileTwo);
		} 
		
	}

	private int[] getChrIndex(String inputFileType, String[] values) throws Exception {		
		
		int chrIndex = 0;
		int startIndex = 0;
		int endIndex = 0;
		
		if (inputFileType.equals(MAF)) {
			chrIndex = 4;
			startIndex = 5;
			endIndex = 6;
		} else if (inputFileType.equals(DCC1)) {
			chrIndex = 4;
			startIndex = 5;
			endIndex = 6;
		} else if (inputFileType.equals(BED)) {
			chrIndex = 0;
			startIndex = 1;
			endIndex = 2;
		} else if (inputFileType.equals(GFF3) || inputFileType.equals(GTF)) {
			chrIndex = 0;
			startIndex = 3;
			endIndex = 4;
		} else if (inputFileType.equals(VCF)) {
			chrIndex = 0;
			startIndex = 1;
			endIndex = 1;
			if (values.length >= 8) {
				String[] infos = values[7].split("\t");
				
				for (String info : infos) {
					String[] params = info.split("=");
					if (params.length == 2) {
						if (params[0].equals("END")) {
							endIndex = 2;
							values[2] = params[1];
						}
					}
				}
			}
			//NEED TO CHANGE FOR INDELS
		} else if (inputFileType.equals(TAB)) {
			chrIndex = 0;
			startIndex = 1;
			endIndex = 2;
		} else {
			throw new Exception("Input file type is not recognized");
		}
		int[] arr = {chrIndex, startIndex, endIndex};
		return arr;
	}
	
	private ChrPosition getChrPosition(String inputFileType, TabbedRecord tab) throws Exception {
		String[] values = tab.getData().split("\t");
		ChrPosition chr = null;
		
		int[] indexes = getChrIndex(inputFileType, values);
		int chrIndex = indexes[0];
		int startIndex = indexes[1];
		int endIndex = indexes[2];
		
		if (inputFileType.equals(BED)) {
			chr = new ChrRangePosition(values[chrIndex], new Integer(values[startIndex])+1, new Integer(values[endIndex])+1);
		} else {
			String chromosome = values[chrIndex];
			if (!chromosome.contains("GL") && !chromosome.startsWith("chr")) {
				chromosome = "chr" + chromosome;
			}
			if (chromosome.equals("chrM")) {
				chromosome = "chrMT";
			}
			if (inputFileType.equals(MAF)) {
				chr = new ChrPositionName(chromosome, new Integer(values[startIndex]), new Integer(values[endIndex]), values[0]);	
			} else {
				chr = new ChrRangePosition(chromosome, new Integer(values[startIndex]), new Integer(values[endIndex]));
			}
		}
		return chr;
	}

	private boolean tabbedRecordFallsInCompareRecord(ChrPosition inputChrPos, TabbedRecord inputRecord, Entry<ChrPosition, TabbedRecord> entry) {
		if (entry != null) {
			ChrPosition compareChrPos = entry.getKey();
			if ((inputChrPos.getStartPosition() >= compareChrPos.getStartPosition() && inputChrPos.getStartPosition() <= compareChrPos.getEndPosition()) ||
					(inputChrPos.getEndPosition() >= compareChrPos.getStartPosition() && inputChrPos.getEndPosition() <= compareChrPos.getEndPosition()) 
					|| (inputChrPos.getStartPosition() <= compareChrPos.getStartPosition() && inputChrPos.getEndPosition() >= compareChrPos.getEndPosition())) {				
				return true;				
			}		
		}
		return false;
	}		

	public String[] getCmdLineInputFiles() {
		return cmdLineInputFiles;
	}

	public void setCmdLineInputFiles(String[] cmdLineInputFiles) {
		this.cmdLineInputFiles = cmdLineInputFiles;
	}


	private void writeHeader(List<String> header, File outputOverlapFile) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputOverlapFile, true));
		
		for (String h: header) {
			
			writer.write(h + "\n");
		}
		writer.close();	
	}
	
	public List<String> getChromosomes() {
		return chromosomes;
	}

	public void setChromosomes(List<String> chromosomes) {
		this.chromosomes = chromosomes;
	}


	public int getOverlapCount() {
		return overlapCount;
	}

	public void setOverlapCount(int overlapCount) {
		this.overlapCount = overlapCount;
	}

	public int getNotOverlappingCount() {
		return notOverlappingCount;
	}

	public void setNotOverlappingCount(int notOverlappingCount) {
		this.notOverlappingCount = notOverlappingCount;
	}

	public int getMafCount() {
		return recordCount;
	}

	public void setMafCount(int mafCount) {
		this.recordCount = mafCount;
	}

	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLogFile();
			logger = QLoggerFactory.getLogger(CompareReferenceRegions.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("CompareReferenceRegions", CompareReferenceRegions.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMuleException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMuleException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			//output files
			cmdLineOutputFiles = options.getOutputFileNames();
			
			if (cmdLineOutputFiles.length >= 1) {
				if ( ! FileUtils.canFileBeWrittenTo(cmdLineOutputFiles[0])) {
					throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", cmdLineOutputFiles[0]);
				}
				
				for (String file :  cmdLineOutputFiles) {
					if (new File(file).exists() && !new File(file).isDirectory()) {
						throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", file);
					}
				}
			}
			mode = options.getMode();	
			if (mode == null) {
				mode = MODE_ONEWAY;
			}
			logger.info("Mode: " + mode);
			
			if (mode.equals(MODE_ANNOTATE)) {
				//take away 1 to get index of column rather than column number
				column = new Integer(options.getColumn()) -1;
				annotation = options.getAnnotation();
			}
			
			return engage();
		}

		return returnStatus;
	}


	private int engage() throws Exception {		
		
		if (mode.equals(MODE_ONEWAY) || mode.equals(MODE_TWOWAY)) {
				runOnewayComparison(new File(cmdLineInputFiles[0]), new File(cmdLineInputFiles[1]), new File(cmdLineOutputFiles[0]), new File(cmdLineOutputFiles[1]));
			if (mode.equals(MODE_TWOWAY)) {			
				runOnewayComparison(new File(cmdLineInputFiles[1]), new File(cmdLineInputFiles[0]), new File(cmdLineOutputFiles[2]), new File(cmdLineOutputFiles[3]));
			}
		} else if (mode.equals(MODE_ANNOTATE)) {
			runAnnotateComparison(new File(cmdLineInputFiles[0]), new File(cmdLineInputFiles[1]), new File(cmdLineOutputFiles[0]));
		} else if (mode.equals(MODE_INTERSECT)) {
			runIntersectComparison();
		} else if (mode.equals(MODE_UNIQUE)) {
			runUniqueComparison();
		} else {
			throw new QMuleException("MODE_ERROR", mode);
		}
		return 0;
	}


	private void clear() {
		recordCount = 0;
		overlapCount = 0;
		notOverlappingCount = 0;	
	}

	public static void main(String[] args) throws Exception {
		CompareReferenceRegions sp = new CompareReferenceRegions();
		int exitStatus = sp.setup(args);
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}

}
