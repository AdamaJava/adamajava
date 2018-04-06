/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import gnu.trove.map.hash.THashMap;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.DatatypeConverter;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.illumina.IlluminaFileReader;
import org.qcmg.illumina.IlluminaRecord;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.BAMFileUtils;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.record.Record;
import org.qcmg.sig.model.BaseReadGroup;
import org.qcmg.sig.util.SignatureUtil;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;


public class SignatureGeneratorBespoke {
	
	private final static ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String illumiaArraysDesign;
	private String snpPositions;
	private int exitStatus;
	private QExec exec;
	
	private VcfRecord vcf;
	
	private  File[] bamFiles = new File[] {};
	private  File[] illuminaFiles = new File[] {};
	
	private int arraySize;
	private int arrayPosition;
	private String outputDirectory;
	
	private int minMappingQuality = 10;
	private int minBaseQuality = 10;
	private String validationStringency;
	
	Comparator<String> chrComparator;
	
	private final List<VcfRecord> snps = new ArrayList<>();
	private final Map<ChrPosition, List<BaseReadGroup>> results = new ConcurrentHashMap<>();
	private final List<StringBuilder> resultsToWrite = new ArrayList<>();
	private final AbstractQueue<SAMRecord> sams = new ConcurrentLinkedQueue<>();
	private final Map<String, String[]> illuminaArraysDesignMap = new ConcurrentHashMap<>();
	private List<String> sortedContigs;
	private byte[] snpPositionsMD5;
	private final StringBuilder stdHeaderDetails = new StringBuilder();
	
	public int engage() throws Exception {
		
		bamFiles = FileUtils.findFilesEndingWithFilter(cmdLineInputFiles[0], ".bam");
		illuminaFiles = FileUtils.findFilesEndingWithFilter(cmdLineInputFiles[0], ".txt");
		
		if (bamFiles.length == 0 && illuminaFiles.length == 0) {
			/*
			 * nothing to process - go home
			 */
			logger.warn("Could not find any bam or txt files in " + cmdLineInputFiles[0]);
		} else {
			/*
			 * load snp positions file, create md5sum
			 */
			
			loadRandomSnpPositions(snpPositions);
			
			assert snpPositionsMD5.length > 0 : "md5sum digest array for snp positions is empty!!!";
			assert ! snps.isEmpty() : "no snps positions loaded!!!";
			
			setupHeader();
			
			if (illuminaFiles.length > 0) {
				// load in the Illumina arrays design document to get the list of snp ids and whether they should be complemented.
				loadIlluminaArraysDesign();
				processIlluminaFiles();
			} else {
				logger.info("Did not find any snp chip files to process");
			}
			
			if (bamFiles.length > 0) {
				processBamFiles();
			} else {
				logger.info("Did not find any bam files to process");
			}
		}
		
		return exitStatus;
	}

	private void setupHeader() {
		LocalDateTime timePoint = LocalDateTime.now();
		
		stdHeaderDetails.append("##fileformat=VCFv4.2").append(Constants.NL);
		stdHeaderDetails.append("##datetime=").append(timePoint.toString()).append(Constants.NL);
		stdHeaderDetails.append("##program=").append(exec.getToolName().getValue()).append(Constants.NL);
		stdHeaderDetails.append("##version=").append(exec.getToolVersion().getValue()).append(Constants.NL);
		stdHeaderDetails.append("##java_version=").append(exec.getJavaVersion().getValue()).append(Constants.NL);
		stdHeaderDetails.append("##run_by_os=").append(exec.getOsName().getValue()).append(Constants.NL);
		stdHeaderDetails.append("##run_by_user=").append(exec.getRunBy().getValue()).append(Constants.NL);
		stdHeaderDetails.append("##positions=").append(snpPositions).append(Constants.NL);
		stdHeaderDetails.append(SignatureUtil.MD_5_SUM).append("=").append(DatatypeConverter.printHexBinary(snpPositionsMD5).toLowerCase()).append(Constants.NL);
		stdHeaderDetails.append(SignatureUtil.POSITIONS_COUNT).append("=").append(snps.size()).append(Constants.NL);
		stdHeaderDetails.append(SignatureUtil.MIN_BASE_QUAL).append("=").append(minBaseQuality).append(Constants.NL);
		stdHeaderDetails.append(SignatureUtil.MIN_MAPPING_QUAL).append("=").append(minMappingQuality).append(Constants.NL);
		stdHeaderDetails.append("##illumina_array_design=").append(illumiaArraysDesign).append(Constants.NL);
		stdHeaderDetails.append("##cmd_line=").append(exec.getCommandLine().getValue()).append(Constants.NL);
		stdHeaderDetails.append("##INFO=<ID=QAF,Number=.,Type=String,Description=\"Lists the counts of As-Cs-Gs-Ts for each read group, along with the total\">").append(Constants.NL);
	}
	
	private void processBamFiles() throws IOException {
		for (final File bamFile : bamFiles) {
			logger.info("Processing data from " + bamFile.getAbsolutePath());
			// set some bam specific values
			arrayPosition = 0;
			vcf = null;
			
			SAMFileHeader header;
			try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile)) {
				header = reader.getFileHeader();
			}
			
			List<String> bamContigs = BAMFileUtils.getContigsFromHeader(header);
			if (SignatureUtil.doContigsStartWithDigit(bamContigs)) {
				/*
				 * add 'chr' to numerical contigs in bamContigs to see if that gives us a match
				 * If not, throw a wobbly
				 */
				bamContigs = SignatureUtil.addChrToContigs(bamContigs);
			}
			
			/*
			 * Set chrComparator and
			 * order snps based on bam contig order
			 */
			chrComparator = ChrPositionComparator.getChrNameComparator(bamContigs);
			snps.sort(ChrPositionComparator.getVcfRecordComparator(bamContigs));
			
			
			try {
				runSequentially(bamFile);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			/*
			 * Get readgroups from bam header and populate map with values and ids - will need to display these in header
			 */
			Map<String, String> rgIds = new HashMap<>();
			int id = 1;
			for (SAMReadGroupRecord srgr : header.getReadGroups()) {
				rgIds.put(srgr.getId(), "rg" + id++);
			}
			
			
			updateResults(rgIds);
			
			// output vcf file
			writeOutput(bamFile, rgIds);
			
			// clean out results, and erase info field from snps
			results.clear();
			resultsToWrite.clear();
		}
	}
	
	private void processIlluminaFiles() throws IOException {
		for (final File illuminaFile : illuminaFiles) {
			
			// load contents of each illumina file into mem
			final Map<ChrPosition, IlluminaRecord> iIlluminaMap = new HashMap<>(1250000);	// not expecting more than 1000000
			
			// set some bam specific values
			arrayPosition = 0;
			vcf = null;
			
			final String patient = SignatureUtil.getPatientFromFile(illuminaFile);
			final String sample = SignatureUtil.getPatternFromString(SignatureUtil.SAMPLE_REGEX, illuminaFile.getName());
			String inputType = SignatureUtil.getPatternFromString(SignatureUtil.TYPE_REGEX, illuminaFile.getName());
			logger.info("got following details from illumina file:" + illuminaFile.getName());
			logger.info("patient: " + patient + ", sample: " + sample + ", inputType: " + inputType);
					
			if (null != inputType && inputType.length() == 4)
				inputType = inputType.substring(1, 3);
			
			
			/*
			 * load data from snp chip fileinto map
			 */
			logger.info("about to load data from snp chip file " + illuminaFile);
			loadIlluminaData(illuminaFile, iIlluminaMap);
			logger.info("loaded " + iIlluminaMap.size() + " positions from snp chip file");
			
			 
			updateResultsIllumina(iIlluminaMap);
			logger.info("updateResults - DONE");
			
			logger.info("about to write output");
			try {
				writeOutput(illuminaFile, null);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// clean out results, and erase info field from snps
			results.clear();
			resultsToWrite.clear();
		}		
	}
	
	private void loadIlluminaArraysDesign() throws Exception {

		// set to file specified by user if applicable
		if (cmdLineInputFiles.length == 3) {
			illumiaArraysDesign = cmdLineInputFiles[2];
		}
		
		// check that we can read the file
		if (null != illumiaArraysDesign && FileUtils.canFileBeRead(illumiaArraysDesign)) {
			try (TabbedFileReader reader=  new TabbedFileReader(new File(illumiaArraysDesign));) {
				for (final TabbedRecord rec : reader) {
					final String [] params = TabTokenizer.tokenize(rec.getData());
					final String id = params[0];
					illuminaArraysDesignMap.put(id, params);
				}
			}
		} else {
			logger.info("could not read the illumina arrays design file: " + illumiaArraysDesign);
		}
	}

	static void loadIlluminaData(File illuminaFile, Map<ChrPosition, IlluminaRecord> illuminaMap) throws IOException {
		IlluminaRecord tempRec;
		try (IlluminaFileReader reader = new IlluminaFileReader(illuminaFile);){
			for (final Record rec : reader) {
				tempRec = (IlluminaRecord) rec;
				
				// only interested in illumina data if it has a gc score above 0.7, and a valid chromosome
				// ignore chromosome 0, and for XY, create 2 records, one for each!
				// skip if the B allele ratio or Log R ratios are NaN
				// skip if non-dbSnp position
				
				
				
				if (tempRec.getGCScore() >= 0.70000f 
						&& null != tempRec.getChr()
						&& ! "0".equals(tempRec.getChr())
						&& ! Float.isNaN(tempRec.getbAlleleFreq())
						&& ! Float.isNaN(tempRec.getLogRRatio())
						&& tempRec.getSnpId().startsWith("rs")) {
					
					// only deal with bi-allelic snps
					final String snp = tempRec.getSnp();
					if (snp.length() == 5 &&  '/' == snp.charAt(2)) {
					
						if ("XY".equals(tempRec.getChr())) {
							// add both X and Y to map
							illuminaMap.put(ChrPointPosition.valueOf("chrX", tempRec.getStart()), tempRec);
							illuminaMap.put(ChrPointPosition.valueOf("chrY", tempRec.getStart()), tempRec);
						} else {
							// 	Illumina record chromosome does not contain "chr", whereas the positionRecordMap does - add
							illuminaMap.put(ChrPointPosition.valueOf("chr" + tempRec.getChr(), tempRec.getStart()), tempRec);
						}
					}
				}
			}
		}
	}
	
//	void createComparatorFromSAMHeader(SAMFileHeader header) throws IOException {
//		if (null == header) throw new IllegalArgumentException("null file passed to createComparatorFromSAMHeader");
//		
//		/*
//		 * get contig names from bam header
//		 */
//		sortedContigs = header.getSequenceDictionary().getSequences().stream().map(SAMSequenceRecord::getSequenceName).collect(Collectors.toList());
//		
//		// try and sort according to the ordering of the bam file that is about to be processed
//		// otherwise, resort to alphabetic ordering and cross fingers...
//		if ( ! sortedContigs.isEmpty()) {
//			
//			chrComparator = ListUtils.createComparatorFromList(sortedContigs);
//			snps.sort(Comparator.comparing(VcfRecord::getChromosome, chrComparator).thenComparingInt(VcfRecord::getPosition));
//			
//		} else {
//			chrComparator = COMPARATOR;
//			snps.sort(new VcfPositionComparator());
//		}
//		
//		final Set<String> uniqueChrs = new HashSet<>();
//		logger.info("chr order:");
//		for (final VcfRecord vcf : snps) {
//			if (uniqueChrs.add(vcf.getChromosome())) {
//				logger.info(vcf.getChromosome());
//			}
//		}
//	}
	
	private void updateResults(Map<String, String> rgIds) {
		
		
		/*
		 * create map from snps list - need ref allele
		 */
		Map<ChrPosition, String> snpMap = new THashMap<>(snps.size() * 2);
		for (VcfRecord v : snps) {
			snpMap.put(v.getChrPosition(), v.getRef());
		}
		
		// update the snps list with the details from the results map
		
		/*
		 * go through results in an ordered fashion
		 */
		List<ChrPosition> keys = new ArrayList<>(results.keySet());
		keys.sort(ChrPositionComparator.getComparator(ChrPositionComparator.getChrNameComparator(sortedContigs)));
		
		for (ChrPosition cp : keys) {
		
			String ref = snpMap.get(cp);
			if ("-".equals(ref)|| "+".equals(ref)) {
				ref = "n";
			}
			final List<BaseReadGroup> bsps = results.get(cp);
			
			if (null == bsps || bsps.isEmpty()) {
			} else {
				final StringBuilder sb = new StringBuilder(cp.getChromosome());
				sb.append(Constants.TAB);
				sb.append(cp.getStartPosition());
				sb.append(Constants.TAB);
				sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// id
				sb.append(ref).append(Constants.TAB);										// ref allele
				sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// alt allele
				sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// qual
				sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// filter
				sb.append("QAF=t:");																	// info
 				sb.append(getEncodedDist(bsps));
				
				/*
				 * now again for the readgroups that we have
				 */
				Map<String, List<BaseReadGroup>> mapOfRgToBases = bsps.stream().collect(Collectors.groupingBy(BaseReadGroup::getReadGroup));
				mapOfRgToBases.forEach((s,l) -> sb.append(Constants.COMMA).append(rgIds.get(s)).append(Constants.COLON).append(getEncodedDist(l)));
				
				resultsToWrite.add(sb);
			}
		}
	}

	public static String getEncodedDist(final List<BaseReadGroup> bsps) {
		if (null == bsps || bsps.isEmpty()) {
			return null;
		}
		int as = 0, cs = 0, gs = 0, ts = 0;
		for (final BaseReadGroup bsp : bsps) {
			switch (bsp.getBase()) {
			case  'A' : as++;break;
			case  'C' : cs++;break;
			case  'G' : gs++;break;
			case  'T' : ts++;break;
			}
		}
		return "" + as + Constants.MINUS + cs + Constants.MINUS + gs + Constants.MINUS + ts;
	}
	
	private void updateResultsIllumina(Map<ChrPosition, IlluminaRecord> iIlluminaMap) {
		
		// update the snps list with the details from the results map
		for (final VcfRecord snp : snps) {
			
			// lookup corresponding snp in illumina map
			final IlluminaRecord illRec = iIlluminaMap.get(ChrPointPosition.valueOf(snp.getChromosome(), snp.getPosition()));
			if (null == illRec) continue;
			
			final String [] params = illuminaArraysDesignMap.get(illRec.getSnpId());
			if (null == params) continue;
			
			snp.setInfo(SignatureUtil.getCoverageStringForIlluminaRecord(illRec, params, 20));
			
		}
	}
	
	
	private void writeOutput(File f, Map<String, String> rgIds) throws IOException {
		// if we have an output folder defined, place the vcf files there, otherwise they will live next to the input file
		File outputVCFFile = null;
		if (null != outputDirectory) {
			outputVCFFile = new File(outputDirectory + FileUtils.FILE_SEPARATOR + f.getName() + SignatureUtil.QSIG_VCF_GZ);
		} else {
			outputVCFFile = new File(f.getAbsoluteFile() + SignatureUtil.QSIG_VCF_GZ);
		}
		logger.info("will write output vcf to file: " + outputVCFFile.getAbsolutePath());
		// standard output format
		// check that can write to new file
		if (FileUtils.canFileBeWrittenTo(outputVCFFile)) {
			final StringBuilder sbRgIds = new StringBuilder("##id:readgroup\n");
			if (null != rgIds) {
				rgIds.entrySet().stream()
					.sorted(Comparator.comparing(Entry::getValue))
					.forEach(e -> sbRgIds.append("##").append(e.getValue()).append(Constants.COLON).append(e.getKey()).append(Constants.NL));
			}
			
			try (OutputStream os = new GZIPOutputStream(new FileOutputStream(outputVCFFile), 1024 * 1024)) {
				/*
				 * Header contains some standard bumf, some file specific bumf, and finally the column header
				 */
				os.write(stdHeaderDetails.toString().getBytes());
				os.write(("##input=" + f.getAbsolutePath() + Constants.NL).getBytes());
				os.write((sbRgIds.toString()).getBytes());
				os.write(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE.getBytes());
				os.write(Constants.NL);
				
				
				for (StringBuilder sb : resultsToWrite) {
					sb.append(Constants.NL);
					os.write(sb.toString().getBytes());
				}
			}
			
		} else {
			logger.warn("Can't write to output vcf file: " + outputVCFFile.getAbsolutePath());
		}
	}
	
	
	private void advanceVCFAndPosition(boolean nextChromosome, SAMRecord rec) {
		if (arrayPosition >= arraySize) {
			// reached the end of the line
			vcf = null;
			return;
		}
		if (nextChromosome) {
			final String currentChr = vcf.getChromosome();
			while (arrayPosition < arraySize) {
				vcf = snps.get(arrayPosition++);
				if ( !  currentChr.equals(vcf.getChromosome()))
					break;
			}
			
		} else {
			vcf = snps.get(arrayPosition++);
			if (null != rec) {
				while (arrayPosition < arraySize) {
					if ( ! rec.getReferenceName().equals(vcf.getChromosome()))
						break;
					if (rec.getAlignmentStart() <= vcf.getPosition())
						break;
					vcf = snps.get(arrayPosition++);
				}
			}
		}
	}
	
	private boolean match(SAMRecord rec, VcfRecord thisVcf, boolean updatePointer) {
		if (null == thisVcf) return false;
		
		String samChr = rec.getReferenceName().startsWith(Constants.CHR) ? rec.getReferenceName() : Constants.CHR + rec.getReferenceName();
		if (samChr.equals(thisVcf.getChromosome())) {
			
			if (rec.getAlignmentEnd() < thisVcf.getPosition())
				return false;
			
			if (rec.getAlignmentStart() <= thisVcf.getPosition()) {
				return true;
			}
			
			// finished with this cp - update results and get a new cp
			if (updatePointer) {
				advanceVCFAndPosition(false, rec);
				return match(rec, vcf, true);
			} else {
				return false;
			}
			
			
		} else if (chrComparator.compare(samChr, thisVcf.getChromosome()) < 1){
			// keep iterating through bam file 
			return false;
		} else {
			if (updatePointer) {
				// need to get next ChrPos
				advanceVCFAndPosition(true, rec);
				return match(rec, vcf, true);
			} else {
				return false;
			}
		}
	}
	
	private void runSequentially(File bamFile) throws Exception {
		
		// setup count down latches
		final CountDownLatch pLatch = new CountDownLatch(1);
		final CountDownLatch cLatch = new CountDownLatch(1);
		
		// create executor service for producer and consumer
		
		final ExecutorService producerEx = Executors.newSingleThreadExecutor();
		final ExecutorService consumerEx = Executors.newSingleThreadExecutor();
		
		// set up producer
		producerEx.execute(new Producer(bamFile, pLatch, Thread.currentThread()));
		producerEx.shutdown();
		
		// setup consumer
		consumerEx.execute(new Consumer(pLatch, cLatch, Thread.currentThread()));
		consumerEx.shutdown();
		
		// wait till the producer count down latch has hit zero
		try {
			pLatch.await(10, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			throw new Exception("Exception caught in Producer thread");
		}
		
		// and now the consumer latch
		try {
			cLatch.await(1, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			throw new Exception("Exception caught in Consumer thread");
		}
	}
	
	private void updateResults(VcfRecord vcf, SAMRecord sam) {
		if (null != sam && null != vcf) {
			// get read index
			final int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, vcf.getPosition());
			
			if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
				
				if (sam.getBaseQualities()[indexInRead] < minBaseQuality) return;
				
				final char c = sam.getReadString().charAt(indexInRead);
				String rgId = null != sam.getReadGroup() ? sam.getReadGroup().getId() : "null";
				
				results.computeIfAbsent(vcf.getChrPosition(), f -> new ArrayList<>())
					.add(new BaseReadGroup(c, rgId));
			}
		}
	}
	
	private void loadRandomSnpPositions(String randomSnpsFile) throws Exception {
		int count = 0;
		MessageDigest md = MessageDigest.getInstance("MD5");
		try (BufferedReader in = new BufferedReader(new FileReader(randomSnpsFile));) {
			
			String line = null;
			while ((line = in.readLine()) != null) {
				++count;
				final String[] params = TabTokenizer.tokenize(line);
				String ref = null;
				if (params.length > 4 && null != params[4]) {
					ref = params[4];
				} else if (params.length > 3 && null != params[3]){
					// mouse file has ref at position 3 (0-based)
					ref = params[3];
				}
				
				if (params.length < 2) {
					throw new IllegalArgumentException("snp file must have at least 2 tab seperated columns, chr and position");
				}
				
				String id = params.length > 2 ? params[2] : null; 
				String alt = params.length > 5 ? params[5].replaceAll("/", ",") : null;
				
				// Lynns new files are 1-based - no need to do any processing on th position
				snps.add( new VcfRecord.Builder(params[0], Integer.parseInt(params[1]), ref).allele(alt).id(id).build());
				
			}
			arraySize = snps.size();
			logger.info("loaded " + arraySize + " positions into map (should be equal to: " + count + ")");
		}
		md.update(Files.readAllBytes(Paths.get(randomSnpsFile)));
		snpPositionsMD5 = md.digest();
		
	}
	
	public static void main(String[] args) throws Exception {
		final SignatureGeneratorBespoke sp = new SignatureGeneratorBespoke();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (final Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running SignatureGenerator:", e);
			else System.err.println("Exception caught whilst running SignatureGenerator");
			e.printStackTrace();
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit(1);
		}
		final Options options = new Options(args);

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
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(SignatureGeneratorBespoke.class, logFile, options.getLogLevel());
			exec = logger.logInitialExecutionStats("SignatureGeneratorBespoke", SignatureGeneratorBespoke.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QSignatureException("INSUFFICIENT_INPUT_FILES");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QSignatureException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			if (null != options.getDirNames() && options.getDirNames().length > 0) {
				outputDirectory = options.getDirNames()[0];
			}
			options.getMinMappingQuality().ifPresent(i -> minMappingQuality = i.intValue());
			options.getMinBaseQuality().ifPresent(i -> minBaseQuality = i.intValue());
			
			if (options.hasIlluminaArraysDesignOption()) {
				illumiaArraysDesign = options.getIlluminaArraysDesign();
			}
			if (options.hasSnpPositionsOption()) {
				snpPositions = options.getSnpPositions();
			}
			if (null == snpPositions || ! FileUtils.canFileBeRead(snpPositions)) {
				throw new QSignatureException("INSUFFICIENT_INPUT_FILES");
			}
			
			validationStringency = options.getValidation();
			
			return engage();
		}
		return returnStatus;
	}
	
	/******************
	* INNER CLASSES
	*******************/
	public class Producer implements Runnable {
		private final SamReader reader;
		private final CountDownLatch pLatch;
		private final Thread mainThread;
		
		public Producer(File bamFile, CountDownLatch pLatch, Thread mainThread) {
			reader = SAMFileReaderFactory.createSAMFileReader(bamFile, validationStringency);
			this.pLatch = pLatch;
			this.mainThread = mainThread;
		}
		
		@Override
		public void run() {
			try {
				for (final SAMRecord rec : reader)  {
					// quality checks
					if (SAMUtils.isSAMRecordValidForVariantCalling(rec) 
							&& rec.getMappingQuality() >= minMappingQuality) {
						sams.add(rec);
					}
				}
			} catch (final Exception e) {
				logger.error("Exception caught in Producer thread - interrupting main thread", e);
				mainThread.interrupt();
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					pLatch.countDown();
				}
			}
		}
	}
	
	public class Consumer implements Runnable {
		
		private final CountDownLatch pLatch;
		private final CountDownLatch cLatch;
		private final Thread mainThread;
		
		public Consumer(final CountDownLatch pLatch, final CountDownLatch cLatch, Thread mainThread) {
			this.pLatch = pLatch;
			this.cLatch = cLatch;
			this.mainThread = mainThread;
		}
		
		@Override
		public void run() {
			final int intervalSize = 1000000;
			try {
				// reset some key values
				arrayPosition = 0;
				vcf = null;
				// load first VCFRecord
				advanceVCFAndPosition(false, null);
				long recordCount = 0;
				// take items off the queue and process
				
				while (true) {
					final SAMRecord sam = sams.poll();
					if (null == sam) {
						// if latch is zero, producer is done, and so are we
						if (pLatch.getCount() == 0) break;
						try {
							Thread.sleep(10);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						
						if (++recordCount % intervalSize == 0) {
							logger.info("processed " + (recordCount / intervalSize) + "M records so far...");
						}
						
						if (match(sam, vcf, true)) {
							updateResults(vcf, sam);
							
							// get next cp and see if it matches
							int j = 0;
							if (arrayPosition < arraySize) {
								VcfRecord tmpVCF = snps.get(arrayPosition + j++);
								while (match(sam, tmpVCF, false)) {
									updateResults(tmpVCF, sam);
									if (arrayPosition + j < arraySize)
										tmpVCF = snps.get(arrayPosition + j++);
									else tmpVCF = null;
								}
							}
						}
					}
				}
				logger.info("processed " + recordCount + " records");
			} catch (final Exception e) {
				logger.error("Exception caught in Consumer thread - interrupting main thread", e);
				mainThread.interrupt();
			} finally {
				cLatch.countDown();
			}
		}
	}
}
