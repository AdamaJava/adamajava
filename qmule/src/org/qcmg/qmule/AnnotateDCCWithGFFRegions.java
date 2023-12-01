/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.util.FileUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;

@Deprecated
public class AnnotateDCCWithGFFRegions {

	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private final List<String> chromosomes = new ArrayList<>();
	private final Map<String, TreeMap<ChrPosition, TabbedRecord>> inputRecords = new HashMap<>();
	private final Map<String, TreeMap<ChrPosition, TabbedRecord>> compareRecords = new HashMap<>();
	private int overlapCount = 0;
	private int notOverlappingCount = 0;
	private final Vector<String> inputFileHeader = new Vector<>();
	private String compareFileType;
	private static QLogger logger;
	private static final String MAF = "maf";
	private static final String GFF3 = "gff3";
	private static final String BED = "bed";
	private static final String VCF = "vcf";
	private static final String TAB = "txt";
	private static final String DCC1 = "dcc1";
	private static final String DCCQ = "dccq";
	private BufferedWriter outputFileWriter;
	private File outputFile;
	private String[] features;
	private boolean stranded;
	private final int GFF_STRAND_INDEX = 6;
	private int DCC_STRAND_INDEX = -1;
	private int QCMGFLAG_COLUMN_INDEX = -1;
	private int REFERENCE_ALLELE_INDEX  = -1;
	private int TUMOUR_ALLELE_INDEX = -1;
	private String annotation;
	private int MUTATION_TYPE_INDEX;
	//private static final int PATIENT_MIN = 5;
	
	public int engage() throws Exception {		

		loadGFFFile(cmdLineInputFiles[1], compareRecords);
		if (compareRecords.isEmpty())  {
			logger.info("No positions loaded from gff file");
		}
	
		logger.info("Starting to process DCC records.");	
		
		outputFile = new File(cmdLineOutputFiles[0]);		
		
		outputFileWriter = new BufferedWriter(new FileWriter(outputFile));

		String inputFileType = null;
		inputFileType = getFileType(cmdLineInputFiles[0]);
		int recordCount = loadDCCFile(cmdLineInputFiles[0], inputFileHeader, inputFileType);
		logger.info("Finished processing DCC records.");	
		outputFileWriter.close();
		logger.info("SUMMARY");
		logger.info("Total DCC Records: " + recordCount);
		logger.info("Total Records in supplied reference regions: " + overlapCount);
		logger.info("Total Records not in supplied reference regions: " + notOverlappingCount);
		return 0;
	}	

	private String getFileType(String fileName) throws QMuleException {
		int index = fileName.lastIndexOf(".") + 1;
		String name = fileName.substring(index);

		if (name.equals("dcc")) {
			return "dcc1";
		}
		
		if (!name.equals(DCC1) && !name.equals(DCCQ)) {
			throw new QMuleException("FILE_TYPE_ERROR");
		}
		
		return name;
	}
	
	private void loadGFFFile(String file, Map<String, TreeMap<ChrPosition, TabbedRecord>> records) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(file));
		int recordCount = 0;
		try {		
			
			Iterator<TabbedRecord> iterator = reader.getRecordIterator();

			while (iterator.hasNext()) {
				
				TabbedRecord tab = iterator.next();
				
				if (tab.getData().startsWith("#")) {					
					continue;
				}				
				recordCount++;
				ChrPosition chrPos = getChrPosition(GFF3, tab, Integer.toString(recordCount));
				String key = chrPos.getChromosome().replace("chr", "");				
				if (records.containsKey(key)) {
					records.get(key).put(chrPos, tab);
				} else {
					TreeMap<ChrPosition, TabbedRecord> map = new TreeMap<>();
					map.put(chrPos, tab);
					records.put(key,map);
				}
				if (!chromosomes.contains(key)) {
					chromosomes.add(key);
				}
			}
		} finally {		
			reader.close();
		}		
				
		logger.info("loaded gff file, total records: " + recordCount);
	}
	
	private int loadDCCFile(String file, Vector<String> header, String fileType) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(file));
		
		int recordCount = 0;
		try {		
			 
			Iterator<TabbedRecord> iterator = reader.getRecordIterator();
			
			if (reader.getHeader() != null) {
				for (String s : reader.getHeader()) {
					header.add(s);
				}
			}
			while (iterator.hasNext()) {
				
				TabbedRecord inputRecord = iterator.next();
				if (inputRecord.getData().startsWith("#") || inputRecord.getData().startsWith("Hugo") || inputRecord.getData().startsWith("analysis") ||
						inputRecord.getData().startsWith("mutation")) {					
					header.add(inputRecord.getData());
					continue;
				}
				
				if (header.size() > 0) {
					parseDCCHeader(header, fileType);
					logger.info("Column of DCC file to annotate: " + QCMGFLAG_COLUMN_INDEX);
					writeHeader(header);
					header.clear();
				}
				
				recordCount++;
				ChrPosition chrPos = getChrPosition(fileType, inputRecord, null);
				String key = chrPos.getChromosome().replace("chr", "");
				TreeMap<ChrPosition, TabbedRecord> compareMap = compareRecords.get(key);
				boolean isOverlapping = false;
				if (compareMap != null) {					
					//check to see if it is overlapping with the comparison reference region
					for (Entry<ChrPosition, TabbedRecord> compareEntry : compareMap.entrySet()) {
						ChrPosition comparePos = compareEntry.getKey();
						if (comparePos.getEndPosition() < chrPos.getStartPosition()) {
						} else if (comparePos.getStartPosition() > chrPos.getEndPosition()) {
							break;
						} else {
							String[] vals = inputRecord.getDataArray();	
							
							if (annotation != null) {
								String oldInfo = vals[QCMGFLAG_COLUMN_INDEX];
								if (!oldInfo.contains("GERM") && tabbedRecordMatchesCompareRecord(chrPos, inputRecord, compareEntry)) {	
									if (annotation != null && !oldInfo.contains("GERM")) {											
										if (annotateWithGermline(vals, compareEntry.getValue().getDataArray())) {
											isOverlapping = true;	
											if (!oldInfo.equals("") && !oldInfo.endsWith(";")) {
												oldInfo += ";";
											}
											oldInfo += annotation;
											inputRecord = buildOutputString(inputRecord, vals, oldInfo);	
										}
									}	
								}
							} else {
								if (tabbedRecordFallsInCompareRecord(chrPos, inputRecord, compareEntry)) {								
									isOverlapping = true;											
									String oldInfo = vals[QCMGFLAG_COLUMN_INDEX];
									//annotate with gff feature
									String feature = getFeatures(compareEntry.getValue());								
									if (!oldInfo.equals("") && !oldInfo.endsWith(";") && !feature.equals("")) {
										oldInfo += ";";
									}
									oldInfo += feature;
									inputRecord = buildOutputString(inputRecord, vals, oldInfo);									
								}
							}
							
						}
					}				
				} 
				
				if (isOverlapping) {					
					overlapCount++;
				} else {
					notOverlappingCount++;
				}
				
				writeRecord(inputRecord);	
				
				if (recordCount % 50000 == 0) {						
					logger.info("Processed records: " + recordCount);	
				}
			} 
		} finally {
			reader.close();
		}
		return recordCount;
	}

	private TabbedRecord buildOutputString(TabbedRecord inputRecord, String[] vals,
			String oldInfo) {
		vals[QCMGFLAG_COLUMN_INDEX] = oldInfo;
		StringBuilder data= new StringBuilder();
		for (String s: vals) {
			data.append(s).append("\t");
		}
		inputRecord.setData(data.toString());
		return inputRecord;
	}

	private boolean annotateWithGermline(String[] inputValues, String[] gffValues) throws QMuleException {	
		String[] attribs = gffValues[getFeatureIndex("attribs")].split(";");
		String gffMotif = getGFF3Motif(attribs);
		//int patientCount = getPatientCount(attribs);
		if (gffMotif == null) {
			String position = gffValues[0] + ":" + gffValues[3] + "-" + gffValues[4];
			throw new QMuleException("NULL_GFF_MOTIF", position);
		}
		String dccMotif = getDCCMotif(inputValues);
		return dccMotif == null || gffMotif.equals(dccMotif);
	}

	private String getGFF3Motif(String[] attribs) {
		
		String referenceAllele = null;
		String tumourAllele = null;
		for (String s: attribs) {
			if (s.startsWith("ReferenceAllele")) {
				referenceAllele = s.split("=")[1];
			}
			if (s.startsWith("TumourAllele")) {
				tumourAllele = s.split("=")[1];
			}
		}

		assert referenceAllele != null;
		if (referenceAllele.contains("-")) {
			assert tumourAllele != null;
			if (!tumourAllele.contains("-")) {
				return tumourAllele;
			}
		}
		if (!referenceAllele.contains("-")) {
			assert tumourAllele != null;
			if (tumourAllele.contains("-")) {
				return referenceAllele;
			}
		}
		return null;
	}
	
	private String getDCCMotif(String[] inputValues) {
		String mutationType = inputValues[MUTATION_TYPE_INDEX];
		String refAllele = inputValues[REFERENCE_ALLELE_INDEX];
		String tumourAllele = inputValues[TUMOUR_ALLELE_INDEX];
		
		if (mutationType.equals("2")) {
			return tumourAllele;
		} else if (mutationType.equals("3")) {
			return refAllele;
		} 
		return null;
	}

	public void parseDCCHeader(List<String> headers, String inputFileType) throws QMuleException {
		
		for (String header: headers) {			
			String[] values = header.split("\t");
			if (values.length == 28 && inputFileType.equals(DCC1)
					|| values.length == 39 && inputFileType.equals(DCCQ)) {
				//check dcc header
				for (int i=0; i<values.length; i++) {
					if (values[i].toLowerCase().contains("mutation_type")) {
						MUTATION_TYPE_INDEX = i;
					}
					if (values[i].toLowerCase().contains("qcmgflag")) {
						QCMGFLAG_COLUMN_INDEX = i;
					}
					if (values[i].equalsIgnoreCase("reference_genome_allele")) {
						REFERENCE_ALLELE_INDEX = i;
					}
					if (values[i].equalsIgnoreCase("tumour_genotype")) {
						TUMOUR_ALLELE_INDEX = i;
					}
					if (values[i].toLowerCase().contains("chromosome_strand")) {						
						DCC_STRAND_INDEX = i;
					}
				}
			}
		}
		checkForColumnParseError(MUTATION_TYPE_INDEX, "Mutation type");		
		checkForColumnParseError(QCMGFLAG_COLUMN_INDEX, "QCMGFlag");		
		checkForColumnParseError(DCC_STRAND_INDEX, "Chromosome strand");		
		checkForColumnParseError(REFERENCE_ALLELE_INDEX, "Reference allele");		
		checkForColumnParseError(TUMOUR_ALLELE_INDEX, "Tumour allele");
		
	}

	private void checkForColumnParseError(int columnIndex,
			String headerName) throws QMuleException {
		if (columnIndex == -1) {			
			throw new QMuleException("DCC_PARSE_ERROR", headerName);
		}		
	}

	public int getDCC_STRAND_INDEX() {
		return DCC_STRAND_INDEX;
	}

	public void setDCC_STRAND_INDEX(int dCC_STRAND_INDEX) {
		DCC_STRAND_INDEX = dCC_STRAND_INDEX;
	}

	public int getQCMGFLAG_COLUMN_INDEX() {
		return QCMGFLAG_COLUMN_INDEX;
	}

	public String getFeatures(TabbedRecord record) {
		StringBuilder sb = new StringBuilder();	
		String[] vals = record.getDataArray();
		if (features == null && annotation == null) {			
			sb.append(vals[getFeatureIndex("feature")]).append(";");
		} else if (features != null){
			for (String s: features) {
				if (s.equals("attribs")) {
					String[] attribs = vals[getFeatureIndex(s)].split(";");
					StringBuilder attrs = new StringBuilder();
					for (int i=0; i<attribs.length; i++) {						
						attrs.append(attribs[i]);
						if (i != attribs.length -1) {
							attrs.append("::");
						}
					}
					sb.append(attrs).append(";");
				} else {
					sb.append(vals[getFeatureIndex(s)]).append(";");
				}
			}
		}
		String outString = sb.toString();
		
		if (outString.endsWith(";")) {
			return outString.substring(0, outString.length() - 1);
		} else {
			return outString;
		}		
	}
	
	private int getFeatureIndex(String feature) {
		return switch (feature) {
			case "seqname", "seqid" -> 0;
			case "source" -> 1;
			case "feature" -> 2;
			case "start" -> 3;
			case "end" -> 4;
			case "score" -> 5;
			case "strand" -> 6;
			case "frame", "phase" -> 7;
			case "attribs", "attributes" -> 8;
			default -> -1;
		};
	}

	private void writeRecord(TabbedRecord record) throws IOException {
		if (!record.getData().endsWith("\n")) {
			record.setData(record.getData() + "\n");
		}
		outputFileWriter.write(record.getData());		
	}

	private ChrPosition getChrPosition(String inputFileType, TabbedRecord tab, String name) throws Exception {
		
		String[] values = tab.getData().split("\t");
		ChrPosition chr;
		int chrIndex = 0;
		int startIndex;
		int endIndex;

		switch (inputFileType) {
			case DCC1 -> {
				chrIndex = 4;
				startIndex = 5;
				endIndex = 6;
			}
			case DCCQ -> {
				chrIndex = 2;
				startIndex = 3;
				endIndex = 4;
			}
			case GFF3 -> {
				startIndex = 3;
				endIndex = 4;
			}
			default -> throw new Exception("Input file type is not recognized");
		}
		
		String chromosome = values[chrIndex];
		if (!chromosome.contains("GL") && !chromosome.startsWith("chr")) {
			chromosome = "chr" + chromosome;
		}
		if (chromosome.equals("chrM")) {
			chromosome = "chrMT";
		}
		chr = new ChrPositionName(chromosome, Integer.parseInt(values[startIndex]), Integer.parseInt(values[endIndex]), name);

		return chr;
	}	
	
	private boolean tabbedRecordMatchesCompareRecord(ChrPosition inputChrPos,
			TabbedRecord inputRecord,
			Entry<ChrPosition, TabbedRecord> compareEntry) {
		if (compareEntry != null) {
			ChrPosition compareChrPos = compareEntry.getKey();
			if ((inputChrPos.getStartPosition() == compareChrPos.getStartPosition() 
					&& inputChrPos.getEndPosition() == compareChrPos.getEndPosition())) {
				//check strand if this option is provided
				if (stranded) {
					String inputStrand = inputRecord.getDataArray()[DCC_STRAND_INDEX];
					String compareStrand = compareEntry.getValue().getDataArray()[GFF_STRAND_INDEX];
					return inputStrand.equals(compareStrand);
				} else {				
					return true;
				}
			}		
		}
		return false;
	}
	
	private boolean tabbedRecordFallsInCompareRecord(ChrPosition inputChrPos, TabbedRecord inputRecord, Entry<ChrPosition, TabbedRecord> entry) {
		if (entry != null) {
			ChrPosition compareChrPos = entry.getKey();
			if ((inputChrPos.getStartPosition() >= compareChrPos.getStartPosition() && inputChrPos.getStartPosition() <= compareChrPos.getEndPosition()) ||
					(inputChrPos.getEndPosition() >= compareChrPos.getStartPosition() && inputChrPos.getEndPosition() <= compareChrPos.getEndPosition()) 
					|| (inputChrPos.getStartPosition() <= compareChrPos.getStartPosition() && inputChrPos.getEndPosition() >= compareChrPos.getEndPosition())) {
				//check strand if this option is provided
				if (stranded) {
					String inputStrand = inputRecord.getDataArray()[DCC_STRAND_INDEX];
					String compareStrand = entry.getValue().getDataArray()[GFF_STRAND_INDEX];
					return inputStrand.equals(compareStrand);
				} else {				
					return true;
				}
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


	private void writeHeader(Vector<String> header) throws IOException {
		
		for (String h: header) {			
			outputFileWriter.write(h + "\n");
		}
	}


	public int getOverlapCount() {
		return overlapCount;
	}

	public int getNotOverlappingCount() {
		return notOverlappingCount;
	}

	protected int setup(String [] args) throws Exception{
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
			String logFile = options.getLogFile();
			logger = QLoggerFactory.getLogger(AnnotateDCCWithGFFRegions.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("AnnotateDCCWithGFFRegions", AnnotateDCCWithGFFRegions.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 2) {
				throw new QMuleException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (String cmdLineInputFile : cmdLineInputFiles) {
					if (!FileUtils.canFileBeRead(cmdLineInputFile)) {
						throw new QMuleException("INPUT_FILE_READ_ERROR", cmdLineInputFile);
					}
				}
			}		
			cmdLineOutputFiles = options.getOutputFileNames();
			if ( ! FileUtils.canFileBeWrittenTo(cmdLineOutputFiles[0])) {
				throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", cmdLineOutputFiles[0]);
			}
			
			for (String file :  cmdLineOutputFiles) {
				if (new File(file).exists() && !new File(file).isDirectory()) {
					throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", file);
				}
			}
			features = options.getFeature();
			annotation = options.getAnnotation();	
			if (features == null && annotation == null) {
				logger.info("Features to annotate: " + "feature");
			} else if (features != null){
				StringBuilder featureString = new StringBuilder();
				for (String f : features) {
					featureString.append(f);
				}
				logger.info("Features to annotate: " + featureString);
			}
			logger.info("Annotation is : " + annotation);
			stranded = options.hasStrandedOption();			
			if (options.getColumn() != null) {
				this.QCMGFLAG_COLUMN_INDEX = Integer.parseInt(options.getColumn()) - 1;
			}

			logger.info("Require matching strand: " + stranded);
			logger.info("DCC file: " + cmdLineInputFiles[0]);
			logger.info("GFF file: " + cmdLineInputFiles[1]);
		}

		return returnStatus;
	}

	public static void main(String[] args) throws Exception {
		AnnotateDCCWithGFFRegions sp = new AnnotateDCCWithGFFRegions();
		sp.setup(args);
		int exitStatus = sp.engage();
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	public String[] getCmdLineOutputFiles() {
		return cmdLineOutputFiles;
	}

	public void setCmdLineOutputFiles(String[] cmdLineOutputFiles) {
		this.cmdLineOutputFiles = cmdLineOutputFiles;
	}

	public Map<String, TreeMap<ChrPosition, TabbedRecord>> getInputRecords() {
		return inputRecords;
	}

	public File getOutputFile() {
		return outputFile;
	}

	public int getREFERENCE_ALLELE_INDEX() {
		return REFERENCE_ALLELE_INDEX;
	}

	public int getTUMOUR_ALLELE_INDEX() {
		return TUMOUR_ALLELE_INDEX;
	}

	public int getMUTATION_TYPE_INDEX() {
		return MUTATION_TYPE_INDEX;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	public String getAnnotation() {
		return this.annotation;
	}

}
