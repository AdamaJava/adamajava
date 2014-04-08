/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
//package org.qcmg.qmule;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.TreeMap;
//import java.util.Vector;
//
//import org.qcmg.bed.BEDFileReader;
//import org.qcmg.bed.BEDRecord;
//import org.qcmg.common.log.QLogger;
//import org.qcmg.common.log.QLoggerFactory;
//import org.qcmg.common.model.ReferenceNameComparator;
//import org.qcmg.common.model.VCFRecord;
//import org.qcmg.common.util.FileUtils;
//import org.qcmg.gff3.GFF3FileReader;
//import org.qcmg.gff3.GFF3Record;
//import org.qcmg.maf.MAFRecord;
//import org.qcmg.tab.TabbedFileReader;
//import org.qcmg.tab.TabbedRecord;
//import org.qcmg.vcf.VCFFileReader;
//
//public class SnpToReferenceRegionFilter {
//	
//	private String logFile;
//	private String[] cmdLineInputFiles;
//	private String[] cmdLineOutputFiles;
//	private List<String> chromosomes = new ArrayList<String>();
//	private int exitStatus;
//	private Map<String, List<VCFRecord>> vcfRecords = new HashMap<String, List<VCFRecord>>();
//	private Map<String, List<MAFRecord>> mafRecords = new HashMap<String, List<MAFRecord>>();
//	private Map<String, TreeMap<Integer, GFF3Record>> gffRecords = new HashMap<String, TreeMap<Integer, GFF3Record>>();
//	private Map<String, TreeMap<Integer, BEDRecord>> bedRecords = new HashMap<String,  TreeMap<Integer, BEDRecord>>();	
//	private final static ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
//	private List<MAFRecord> overlappingMafRecords = new ArrayList<MAFRecord>();
//	private List<MAFRecord> notOverlappingMafRecords = new ArrayList<MAFRecord>();
//	private List<VCFRecord> overlappingVcfRecords = new ArrayList<VCFRecord>();
//	private List<VCFRecord> notOverlappingVcfRecords = new ArrayList<VCFRecord>();
//	private int overlapCount = 0;
//	private int notOverlappingCount = 0;
//	private int snpCount;
//	private Vector<String> header = new Vector<String>();
//	private String inputSnpType;
//	
//	private static QLogger logger;
//	
//	public int engage() throws Exception {
//		inputSnpType = null;
//		if (cmdLineInputFiles[0].endsWith("maf")) {
//			logger.info("MAF File: " + cmdLineInputFiles[0]);
//			loadMafFile();
//			inputSnpType = "MAF";
//			if (mafRecords.isEmpty()) throw new IllegalArgumentException("No positions loaded from maf file");
//		}
//		if (cmdLineInputFiles[0].endsWith("vcf")) {
//			logger.info("VCF File: " + cmdLineInputFiles[0]);
//			loadVCFFile();
//			inputSnpType = "VCF";
//			if (vcfRecords.isEmpty()) throw new IllegalArgumentException("No positions loaded from vcf file");
//		}
//		if (cmdLineInputFiles[1].endsWith("bed")) {
//			logger.info("BED File: " + cmdLineInputFiles[1]);
//		} else if (cmdLineInputFiles[1].endsWith("gff3")) {
//			logger.info("GFF3 File: " + cmdLineInputFiles[1]);
//		}
//		logger.info("Output file: " + cmdLineOutputFiles[0]);
//		logger.info("Output file: " + cmdLineOutputFiles[1]);
//		
//		String fileType = null;
//		if (cmdLineInputFiles[1].endsWith("bed")) {
//			loadBedFile();
//			fileType = "bed";
//		} else if (cmdLineInputFiles[1].endsWith("gff3")) {
//			fileType = "gff3";
//			loadGffFile();
//		} else {
//			throw new IllegalArgumentException("File type for reference regions is not recognized. Must be bed or gff3");
//		}		
//		
//		Collections.sort(chromosomes,COMPARATOR);
//		
//		writeHeader();
//		
//		if (fileType.equals("bed")) {
//			if (bedRecords.isEmpty()) throw new IllegalArgumentException("No positions loaded from bed file");
//			for (String c: chromosomes) {
//				if (inputSnpType.equals("MAF")) {
//					
//					compareBedRecordsToMaf(c, bedRecords.get(c));
//				}
//				if (inputSnpType.equals("VCF")) {
//					compareBedRecordsToVcf(c, bedRecords.get(c));
//				}
//			}			
//		}
//		
//		if (fileType.equals("gff3")) {
//			if (gffRecords.isEmpty()) throw new IllegalArgumentException("No positions loaded from gff3 file");
//			for (String c: chromosomes) {
//				logger.info("Chromosome: " + c);
//				if (inputSnpType.equals("MAF")) {
//					compareGFFRecordsToMaf(c, gffRecords.get(c));
//				}
//				if (inputSnpType.equals("VCF")) {
//					compareGFFRecordsToVcf(c, gffRecords.get(c));
//				}
//			}			
//		}
//				
//		logger.info("SUMMARY");
//		logger.info("Total Records in " +inputSnpType+ ": " + snpCount);
//		logger.info("Total Records in supplied reference regions: " + overlapCount);
//		logger.info("Total Records not in supplied reference regions: " + notOverlappingCount);
//		return exitStatus;
//	}
//
//	private void loadVCFFile() throws Exception {
//		VCFFileReader reader = new VCFFileReader(new File(cmdLineInputFiles[0]));
//		try {		
//			header = new Vector<String>();
//			Iterator<VCFRecord> iterator = reader.getRecordIterator();
//			snpCount = 0;
//			if (reader.getHeader() != null) {
//				Iterator<String> iter = reader.getHeader().iterator();
//				while (iter.hasNext()) {
//					header.add(iter.next());
//				}
//			}
//			while (iterator.hasNext()) {
//				
//				VCFRecord vcfRec = iterator.next();
//				
//				snpCount++;
//				if (vcfRecords.containsKey(vcfRec.getChromosome())) {
//					vcfRecords.get(vcfRec.getChromosome()).add(vcfRec);
//				} else {
//					List<VCFRecord> list = new ArrayList<VCFRecord>();
//					list.add(vcfRec);
//					vcfRecords.put(vcfRec.getChromosome(),list);
//				}
//				if (!chromosomes.contains(vcfRec.getChromosome())) {
//					chromosomes.add(vcfRec.getChromosome());
//				}
//			}
//			logger.info("loaded maf file, total records: " + snpCount);
//		} finally {		
//			reader.close();
//		}		
//	}
//
//	private void loadMafFile() throws Exception {
//		TabbedFileReader reader = new TabbedFileReader(new File(cmdLineInputFiles[0]));
//		try {		
//			header = new Vector<String>();
//			Iterator<TabbedRecord> iterator = reader.getRecordIterator();
//			snpCount = 0;
//			if (reader.getHeader() != null) {
//				Iterator<String> iter = reader.getHeader().iterator();
//				while (iter.hasNext()) {
//					header.add(iter.next());
//				}
//			}
//			while (iterator.hasNext()) {
//				
//				TabbedRecord tab = iterator.next();
//				
//				if (tab.getData().startsWith("#") || tab.getData().startsWith("Hugo")) {
//					header.add(tab.getData());
//					continue;
//				}
//				snpCount++;
//				MAFRecord mafRec = convertToMafRecord(tab.getData().split("\t"));
//				mafRec.setData(tab.getData());
//				if (mafRecords.containsKey(mafRec.getChromosome())) {
//					mafRecords.get(mafRec.getChromosome()).add(mafRec);
//				} else {
//					List<MAFRecord> list = new ArrayList<MAFRecord>();
//					list.add(mafRec);
//					mafRecords.put(mafRec.getChromosome(),list);
//				}
//				if (!chromosomes.contains(mafRec.getChromosome())) {
//					chromosomes.add(mafRec.getChromosome());
//				}
//			}
//			logger.info("loaded maf file, total records: " + snpCount);
//		} finally {		
//			reader.close();
//		}
//	}	
//
//	private void loadBedFile() throws IOException {
//		BEDFileReader reader =  new BEDFileReader(new File(cmdLineInputFiles[1]));
//		try {
//			int count = 0;
//			for (BEDRecord record : reader) {
//				count++;
//				String chr = record.getChrom();
//				if (inputSnpType.equals("MAF")) {
//					chr = record.getChrom().replace("chr", "");
//				} 
//				if (bedRecords.containsKey(chr)) {
//					bedRecords.get(chr).put(record.getChromStart(), record);
//				} else {
//					TreeMap<Integer, BEDRecord> map = new TreeMap<Integer, BEDRecord>();
//					map.put(record.getChromStart(), record);
//					bedRecords.put(chr,map);
//				}
//			}
//			logger.info("loaded bed file, total record: " + count);
//		} finally {
//			reader.close();
//		}
//		
//	}
//	
//	private void loadGffFile() throws Exception {
//		GFF3FileReader reader =  new GFF3FileReader(new File(cmdLineInputFiles[1]));
//		try {
//			int count = 0;
//			for (GFF3Record record : reader) {
//				count++;
//				String chr = record.getSeqId();
//				if (inputSnpType.equals("MAF")) {
//					chr = record.getSeqId().replace("chr", "");
//				} 
//				if (gffRecords.containsKey(chr)) {
//					gffRecords.get(chr).put(record.getStart(), record);
//				} else {
//					TreeMap<Integer, GFF3Record> map = new TreeMap<Integer, GFF3Record>();
//					map.put(record.getStart(), record);
//					gffRecords.put(chr,map);
//				}
//			}
//			
//			logger.info("loaded gff3 file, total record: " + count);
//		} finally {
//			reader.close();
//		}
//	}	
//	
//	public void compareBedRecordsToVcf(String chromosome, TreeMap<Integer, BEDRecord> map) throws IOException {
//		List<VCFRecord> vcfList = vcfRecords.get(chromosome);
//		
//		//bed positions are zero based
//		if (map != null) {				
//				
//			for (VCFRecord snp : vcfList) {				
//				Entry<Integer, BEDRecord> floor = map.floorEntry(new Integer(snp.getPosition()));
//				Entry<Integer, BEDRecord> ceiling = map.ceilingEntry(new Integer(snp.getPosition()));
//				
//				if (vcfRecordFallsInBEDRecord(snp, floor) || vcfRecordFallsInBEDRecord(snp, ceiling)) {
//					overlapCount++;
//					overlappingVcfRecords.add(snp);
//				} else {
//					notOverlappingCount++;
//					notOverlappingVcfRecords.add(snp);
//					if (notOverlappingCount % 10000 == 0) {
//						logger.info("Processed records: " + notOverlappingCount);
//					}
//				}
//			}			
//		} else {
//			notOverlappingVcfRecords.addAll(vcfList);
//			notOverlappingCount += vcfList.size();
//		}
//		writeParsedVcfRecords();
//	}
//	
//	public void compareBedRecordsToMaf(String chromosome, TreeMap<Integer, BEDRecord> map) throws IOException {
//		List<MAFRecord> mafList = mafRecords.get(chromosome);
//		
//		//bed positions are zero based
//		if (map != null) {				
//				
//			for (MAFRecord snp : mafList) {
//				
//				Entry<Integer, BEDRecord> floor = map.floorEntry(new Integer(snp.getStartPosition()));
//				Entry<Integer, BEDRecord> ceiling = map.ceilingEntry(new Integer(snp.getStartPosition()));
//				
//				if (mafRecordFallsInBEDRecord(snp, floor) || mafRecordFallsInBEDRecord(snp, ceiling)) {
//					overlapCount++;
//					overlappingMafRecords.add(snp);
//				} else {
//					notOverlappingCount++;
//					notOverlappingMafRecords.add(snp);
//					if (notOverlappingCount % 10000 == 0) {
//						logger.info("Processed records: " + notOverlappingCount);
//					}
//				}
//				
//			}
//		} else {
//			notOverlappingMafRecords.addAll(mafList);
//			notOverlappingCount += mafList.size();
//		}
//		writeParsedMafRecords();
//	}
//	
//	public void compareGFFRecordsToVcf(String chromosome, TreeMap<Integer, GFF3Record> map) throws IOException {
//		List<VCFRecord> vcfList = vcfRecords.get(chromosome);
//		
//		if (map != null) {
//			
//			logger.info("List size: " + vcfList.size());
//			for (VCFRecord snp : vcfList) {
//				Entry<Integer, GFF3Record> floor = map.floorEntry(new Integer(snp.getPosition()));
//				Entry<Integer, GFF3Record> ceiling = map.ceilingEntry(new Integer(snp.getPosition()));
//				
//				if (vcfRecordFallsInGFF3Record(snp, floor) || vcfRecordFallsInGFF3Record(snp, ceiling)) {
//					overlapCount++;
//					overlappingVcfRecords.add(snp);
//				} else {
//					notOverlappingCount++;
//					notOverlappingVcfRecords.add(snp);
//					if (notOverlappingCount % 10000 == 0) {
//						logger.info("Processed records: " + notOverlappingCount);
//					}
//				}
//			}
//		} else {			 
//			notOverlappingVcfRecords.addAll(vcfList);
//			notOverlappingCount += vcfList.size();				
//		}
//		writeParsedVcfRecords();
//	}
//	
//	public void compareGFFRecordsToMaf(String chromosome, TreeMap<Integer, GFF3Record> map) throws IOException {
//		List<MAFRecord> mafList = mafRecords.get(chromosome);		
//		
//		if (map != null) {
//			
//			for (MAFRecord snp : mafList) {
//				
//				Entry<Integer, GFF3Record> floor = map.floorEntry(new Integer(snp.getStartPosition()));
//				Entry<Integer, GFF3Record> ceiling = map.ceilingEntry(new Integer(snp.getStartPosition()));
//				
//				if (mafRecordFallsInGFF3Record(snp, floor) || mafRecordFallsInGFF3Record(snp, ceiling)) {
//					overlapCount++;
//					overlappingMafRecords.add(snp);
//				} else {
//					notOverlappingCount++;
//					notOverlappingMafRecords.add(snp);
//					if (notOverlappingCount % 10000 == 0) {
//						logger.info("Processed records: " + notOverlappingCount);
//					}
//				}
//			}
//		} else {			 
//			notOverlappingMafRecords.addAll(mafList);
//			notOverlappingCount += mafList.size();				
//		}
//		writeParsedMafRecords();
//	}	
//
//
//	private boolean mafRecordFallsInGFF3Record(MAFRecord snp, Entry<Integer, GFF3Record> entry) {
//		if (entry != null) {
//			if (snp.getStartPosition() >= entry.getValue().getStart() && snp.getStartPosition() <= entry.getValue().getEnd() ||
//					snp.getEndPosition() >= entry.getValue().getStart() && snp.getEndPosition() <= entry.getValue().getEnd()) {
//				return true;
//			}		
//		}
//		return false;
//	}
//	
//	private boolean mafRecordFallsInBEDRecord(MAFRecord snp, Entry<Integer, BEDRecord> entry) {
//		if (entry != null) {
//			if (snp.getStartPosition() >= entry.getValue().getChromStart()+1 && snp.getStartPosition() <= entry.getValue().getChromEnd() ||
//					snp.getEndPosition() >= entry.getValue().getChromStart()+1 && snp.getEndPosition() <= entry.getValue().getChromEnd()) {
//				return true;
//			}		
//		}
//		return false;
//	}
//	
//	private boolean vcfRecordFallsInGFF3Record(VCFRecord snp, Entry<Integer, GFF3Record> entry) {
//		if (entry != null) {
//			if (snp.getPosition() >= entry.getValue().getStart() && snp.getPosition() <= entry.getValue().getEnd()) {
//				return true;
//			}
//		}
//		return false;
//	}
//	
//	private boolean vcfRecordFallsInBEDRecord(VCFRecord snp, Entry<Integer, BEDRecord> entry) {
//		if (entry != null) {
//			if (snp.getPosition() >= entry.getValue().getChromStart()+1 && snp.getPosition() <= entry.getValue().getChromEnd()) {
//				return true;
//			}	
//		}
//		return false;
//	}
//
//	public String[] getCmdLineInputFiles() {
//		return cmdLineInputFiles;
//	}
//
//	public void setCmdLineInputFiles(String[] cmdLineInputFiles) {
//		this.cmdLineInputFiles = cmdLineInputFiles;
//	}
//
//	public String[] getCmdLineOutputFiles() {
//		return cmdLineOutputFiles;
//	}
//
//	public void setCmdLineOutputFiles(String[] cmdLineOutputFiles) {
//		this.cmdLineOutputFiles = cmdLineOutputFiles;
//	}
//
//	private void writeParsedMafRecords() throws IOException {
//		writeMafRecordsToFile(cmdLineOutputFiles[0], overlappingMafRecords);
//		writeMafRecordsToFile(cmdLineOutputFiles[1], notOverlappingMafRecords);		
//	}
//
//	private void writeParsedVcfRecords() throws IOException {
//		writeVcfRecordsToFile(cmdLineOutputFiles[0], overlappingVcfRecords);
//		writeVcfRecordsToFile(cmdLineOutputFiles[1], notOverlappingVcfRecords);		
//	}
//	
//	private void writeHeader() throws IOException {
//		writeHeader(cmdLineOutputFiles[0]);
//		writeHeader(cmdLineOutputFiles[1]);
//	}
//	
//	private void writeHeader(String fileName) throws IOException {
//		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName), true));
//		
//		for (String h: header) {
//			writer.write(h + "\n");
//		}
//		writer.close();	
//	}
//
//	private void writeMafRecordsToFile(String fileName,
//			List<MAFRecord> outputRecords) throws IOException {
//		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName), true));
//		
//		for (MAFRecord r: outputRecords) {
//			writer.write(r.getData() + "\n");
//		}
//		
//		writer.close();
//		outputRecords.clear();		
//	}
//	
//	private void writeVcfRecordsToFile(String fileName,
//			List<VCFRecord> outputRecords) throws IOException {
//		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName), true));
//		
//		for (VCFRecord r: outputRecords) {
//			writer.write(r.toString() + "\n");
//		}
//		
//		writer.close();
//		outputRecords.clear();		
//	}
//
//	
//	protected int setup(String args[]) throws Exception{
//		int returnStatus = 1;
//		if (null == args || args.length == 0) {
//			System.err.println(Messages.USAGE);
//			System.exit(1);
//		}
//		Options options = new Options(args);
//
//		if (options.hasHelpOption()) {
//			System.err.println(Messages.USAGE);
//			options.displayHelp();
//			returnStatus = 0;
//		} else if (options.hasVersionOption()) {
//			System.err.println(Messages.getVersionMessage());
//			returnStatus = 0;
//		} else if (options.getInputFileNames().length < 1) {
//			System.err.println(Messages.USAGE);
//		} else if ( ! options.hasLogOption()) {
//			System.err.println(Messages.USAGE);
//		} else {
//			// configure logging
//			logFile = options.getLogFile();
//			logger = QLoggerFactory.getLogger(SnpToReferenceRegionFilter.class, logFile, options.getLogLevel());
//			logger.logInitialExecutionStats("SnpMafBedFileComparison", SnpToReferenceRegionFilter.class.getPackage().getImplementationVersion(), args);
//			
//			// get list of file names
//			cmdLineInputFiles = options.getInputFileNames();
//			if (cmdLineInputFiles.length < 1) {
//				throw new QMuleException("INSUFFICIENT_ARGUMENTS");
//			} else {
//				// loop through supplied files - check they can be read
//				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
//					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
//						throw new QMuleException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
//					}
//				}
//			}
//			
//			// check supplied output files can be written to
//			if (null != options.getOutputFileNames()) {
//				cmdLineOutputFiles = options.getOutputFileNames();
//				for (String outputFile : cmdLineOutputFiles) {
//					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
//						throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", outputFile);
//				}
//			}			
//			return engage();
//		}
//		return returnStatus;
//	}
//	
//	
//	public static void main(String[] args) throws Exception {
//		SnpToReferenceRegionFilter sp = new SnpToReferenceRegionFilter();
//		int exitStatus = sp.setup(args);
//		if (null != logger)
//			logger.logFinalExecutionStats(exitStatus);
//		
//		System.exit(exitStatus);
//	}
//	
//	public static MAFRecord convertToMafRecord(String[] params) {
//		MAFRecord maf = new MAFRecord();
////		System.out.println(params[0]);
////		maf.setHugoSymbol(params[0]);
////		maf.setEntrezGeneId(params[1]);
////		maf.setCenter(params[2]);
////		maf.setNcbiBuild(Integer.parseInt(params[3]));
//		maf.setChromosome(params[0]);
//		maf.setStartPosition(Integer.parseInt(params[1]));
//		maf.setEndPosition(Integer.parseInt(params[1]));
////		maf.setStrand(params[7].charAt(0));
////		maf.setVariantClassification(params[8]);
////		maf.setVariantType(params[9]);
////		maf.setRef(params[10]);
////		maf.setTumourAllele1(params[11]);
////		maf.setTumourAllele2(params[12]);
////		maf.setDbSnpId(params[13]);
////		maf.setDbSnpValStatus(params[14]);
////		maf.setTumourSampleBarcode(params[15]);
////		maf.setNormalSampleBarcode(params[16]);
////		maf.setNormalAllele1(params[17]);
////		maf.setNormalAllele2(params[18]);
////		maf.setTumourValidationAllele1(params[19]);
////		maf.setTumourValidationAllele2(params[20]);		
////		maf.setNormalValidationAllele1(params[21]);		
////		maf.setNormalValidationAllele2(params[22]);		
////		maf.setVerificationStatus(params[23]);		
////		maf.setValidationStatus(params[24]);		
////		maf.setMutationStatus(params[25]);		
////		maf.setSequencingPhase(params[26]);		
////		maf.setSequencingSource(params[27]);		
////		maf.setValidationMethod(params[28]);		
////		maf.setScore(params[29]);		
////		maf.setBamFile(params[30]);		
////		maf.setSequencer(params[31]);
////		// QCMG
////		if (params.length > 32)
////			maf.setFlag(params[32]);
////		if (params.length > 33)
////			maf.setNd(params[33]);
////		if (params.length > 34)
////			maf.setTd(params[34]);
////		if (params.length > 35)
////			maf.setCanonicalTranscriptId(params[35]);
////		if (params.length > 36)
////			maf.setCanonicalAAChange(params[36]);
////		if (params.length > 37)
////			maf.setCanonicalBaseChange(params[37]);
////		if (params.length > 38)
////			maf.setAlternateTranscriptId(params[38]);
////		if (params.length > 39)
////			maf.setAlternateAAChange(params[39]);
////		if (params.length > 40)
////			maf.setAlternateBaseChange(params[40]);
//		
//		return maf;
//	}
//	
//	public List<String> getChromosomes() {
//		return chromosomes;
//	}
//
//	public void setChromosomes(List<String> chromosomes) {
//		this.chromosomes = chromosomes;
//	}
//
//	public Map<String, List<MAFRecord>> getMafRecords() {
//		return mafRecords;
//	}
//
//	public void setMafRecords(Map<String, List<MAFRecord>> mafRecords) {
//		this.mafRecords = mafRecords;
//	}
//
//	public List<MAFRecord> getOverlappingRecords() {
//		return overlappingMafRecords;
//	}
//
//	public void setOverlappingRecords(List<MAFRecord> overlappingRecords) {
//		this.overlappingMafRecords = overlappingRecords;
//	}
//
//	public List<MAFRecord> getNotOverlappingRecords() {
//		return notOverlappingMafRecords;
//	}
//
//	public void setNotOverlappingRecords(List<MAFRecord> notOverlappingRecords) {
//		this.notOverlappingMafRecords = notOverlappingRecords;
//	}
//
//	public int getOverlapCount() {
//		return overlapCount;
//	}
//
//	public void setOverlapCount(int overlapCount) {
//		this.overlapCount = overlapCount;
//	}
//
//	public int getNotOverlappingCount() {
//		return notOverlappingCount;
//	}
//
//	public void setNotOverlappingCount(int notOverlappingCount) {
//		this.notOverlappingCount = notOverlappingCount;
//	}
//
//	public int getMafCount() {
//		return snpCount;
//	}
//
//	public void setMafCount(int mafCount) {
//		this.snpCount = mafCount;
//	}
//
//	
//
//}
