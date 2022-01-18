/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.BAMFileUtils;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.qio.illumina.IlluminaFileReader;
import org.qcmg.qio.illumina.IlluminaRecord;
import org.qcmg.qio.record.RecordReader;
import org.qcmg.qio.record.StringFileReader;
import org.qcmg.sig.positions.GeneModelInMemoryPositionIterator;
import org.qcmg.sig.positions.PositionIterator;
import org.qcmg.sig.positions.VcfInMemoryPositionIterator;
import org.qcmg.sig.positions.VcfStreamPositionIterator;
import org.qcmg.sig.util.SignatureUtil;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

/**
 * 
 * This class creates the newer "bespoke" qsig vcf output file which is more parsimonious with its outputting, and will only output a position if there is coverage.
 *  
 * Coverage data is shown in the following format:
 * QAF=t:0-0-0-2,rg1:0-0-0-2,rg2:0-0-0-0
 * 
 * Read group coverage is displayed allowing the user to run the CompareRG class to determine if the read groups that make up the BAM match each other.
 * 
 * @author oliverh
 * @param <T>
 *
 */
public class Generate {
	
	static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String illumiaArraysDesign;
	private String snpPositions;
	private String genePositions;
	private String reference;
	private int exitStatus;
	private QExec exec;
	
	private ChrPosition cp;
	private final AbstractQueue<ChrPosition> completedCPs = new ConcurrentLinkedQueue<>();
	
	private  File[] bamFiles = new File[] {};
	private  File[] illuminaFiles = new File[] {};
	
	private String outputDirectory;
	private String outputFile;
	private boolean stream;
	
	private int minMappingQuality = 10;
	private int minBaseQuality = 10;
	private static float minGCScore = 0.70000f ;
	private String validationStringency;
	
	Comparator<String> chrComparator;
	
	private final Map<ChrPosition, int[][]> results = new ConcurrentHashMap<>();
	private TObjectIntMap<String> rgIds;
	private final List<StringBuilder> resultsToWrite = new ArrayList<>();
	private final AbstractQueue<SAMRecord> sams = new ConcurrentLinkedQueue<>();
	private final Map<String, String[]> illuminaArraysDesignMap = new ConcurrentHashMap<>();
	private byte[] snpPositionsMD5;
	
	private PositionIterator<ChrPosition> positionsIterator;
	private boolean isSnpPositionsAVcfFile;
	private MessageDigest md;
	private RecordReader<?> positionsReader;
	
	private List<ChrPosition> nextCPs = new java.util.LinkedList<>();
	
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
			 * alternatively, load genePositions gff3 file and create md5sum
			 */
			if (null != snpPositions) {
				setupPositionIterator(snpPositions);
				getFileMd5Sum(snpPositions);
			} else if (null != genePositions) {
				setGenePositionsIterator(genePositions);
				getFileMd5Sum(genePositions);
			}
			
			if (illuminaFiles.length > 0) {
				// load in the Illumina arrays design document to get the list of snp ids and whether they should be complemented.
				loadIlluminaArraysDesign();
				processIlluminaFiles();
			} else {
				logger.info("Did not find any snp chip files to process");
			}
			
			if (bamFiles.length > 0) {
				logger.info("about to hit processBamFiles, positionsIterator.hasNext(): " + positionsIterator.hasNext());
				processBamFiles();
			} else {
				logger.info("Did not find any bam files to process");
			}
			
			assert snpPositionsMD5.length > 0 : "md5sum digest array for snp positions is empty!!!";
		}
		if (null != positionsReader) {
			positionsReader.close();
		}
		
		return exitStatus;
	}

	private StringBuilder getHeader(boolean bam) {
		LocalDateTime timePoint = LocalDateTime.now();
		StringBuilder sb = new StringBuilder();
		sb.append("##fileformat=VCFv4.2").append(Constants.NL);
		sb.append("##datetime=").append(timePoint.toString()).append(Constants.NL);
		sb.append("##program=").append(exec.getToolName().getValue()).append(Constants.NL);
		sb.append("##version=").append(exec.getToolVersion().getValue()).append(Constants.NL);
		sb.append("##java_version=").append(exec.getJavaVersion().getValue()).append(Constants.NL);
		sb.append("##run_by_os=").append(exec.getOsName().getValue()).append(Constants.NL);
		sb.append("##run_by_user=").append(exec.getRunBy().getValue()).append(Constants.NL);
		sb.append("##snp_positions=").append(snpPositions).append(Constants.NL);
		sb.append("##gene_positions=").append(genePositions).append(Constants.NL);
		sb.append("##reference=").append(reference).append(Constants.NL);
		sb.append(SignatureUtil.MD_5_SUM).append("=").append(DatatypeConverter.printHexBinary(snpPositionsMD5).toLowerCase()).append(Constants.NL);
		if (bam) {
			sb.append(SignatureUtil.MIN_BASE_QUAL).append("=").append(minBaseQuality).append(Constants.NL);
			sb.append(SignatureUtil.MIN_MAPPING_QUAL).append("=").append(minMappingQuality).append(Constants.NL);
		} else {
			sb.append(SignatureUtil.MIN_GC_SCORE).append("=").append(minGCScore).append(Constants.NL);
		}
		sb.append("##illumina_array_design=").append(illumiaArraysDesign).append(Constants.NL);
		sb.append("##cmd_line=").append(exec.getCommandLine().getValue()).append(Constants.NL);
		sb.append("##INFO=<ID=QAF,Number=.,Type=String,Description=\"Lists the counts of As-Cs-Gs-Ts for each read group, along with the total\">").append(Constants.NL);
		return sb;
	}
	
	private void processBamFiles() throws IOException {
		for (final File bamFile : bamFiles) {
			logger.info("Processing data from " + bamFile.getAbsolutePath());
			// set some bam specific values
			cp = null;
			
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
			positionsIterator.sort(bamContigs);
			
			/*
			 * Get readgroups from bam header and populate map with values and ids - will need to display these in header
			 */
			rgIds = getReadGroupsAsMap(header);
			
			try {
				runSequentially(bamFile);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	public static TObjectIntMap<String> getReadGroupsAsMap(SAMFileHeader header) {
		/*
		 * Get readgroups from bam header and populate map with values and ids - will need to display these in header
		 */
		TObjectIntMap<String> map = new TObjectIntHashMap<>();
		int id = 0;
		/*
		 * Add a null entry to the map for records that don't have their RG set
		 * This is conveniently set as the first entry as TObjectIntMap will return 0 when the key is not in the map
		 */
		map.putIfAbsent(null, id++);
		
		for (SAMReadGroupRecord srgr : header.getReadGroups()) {
			map.putIfAbsent(srgr.getId(), id++);
		}
		
		return map;
	}
	
	private void processIlluminaFiles() throws IOException {
		for (final File illuminaFile : illuminaFiles) {
			
			// load contents of each illumina file into mem
			final Map<ChrPosition, IlluminaRecord> iIlluminaMap = new HashMap<>(1250000);	// not expecting more than 1000000
			
			// set some bam specific values
			cp = null;
			
			final String patient = SignatureUtil.getPatientFromFile(illuminaFile);
			final String sample = SignatureUtil.getPatternFromString(SignatureUtil.SAMPLE_REGEX, illuminaFile.getName());
			String inputType = SignatureUtil.getPatternFromString(SignatureUtil.TYPE_REGEX, illuminaFile.getName());
			logger.info("got following details from illumina file:" + illuminaFile.getName());
			logger.info("patient: " + patient + ", sample: " + sample + ", inputType: " + inputType);
					
			if (null != inputType && inputType.length() == 4) {
				inputType = inputType.substring(1, 3);
			}
			
			/*
			 * load data from snp chip file into map
			 */
			logger.info("about to load data from snp chip file " + illuminaFile);
			loadIlluminaData(illuminaFile, iIlluminaMap);
			logger.info("loaded " + iIlluminaMap.size() + " positions from snp chip file");
			
			 
			updateResultsIllumina(iIlluminaMap);
			logger.info("updateResults - DONE");
			
			logger.info("about to write output");
			try {
				writeOutput(illuminaFile, null, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// clean out results, and erase info field from snps
			results.clear();
			resultsToWrite.clear();
		}		
	}
	
	private void loadIlluminaArraysDesign() throws IOException {

		logger.info("in loadIlluminaArraysDesign with illumiaArraysDesign: " + illumiaArraysDesign);
		// check that we can read the file
		if (null != illumiaArraysDesign && FileUtils.canFileBeRead(illumiaArraysDesign)) {
			try (StringFileReader reader=  new StringFileReader(new File(illumiaArraysDesign));) {
				for (final String rec : reader) {
					final String [] params = TabTokenizer.tokenize(rec);
					final String id = params[0];
					illuminaArraysDesignMap.put(id, params);
				}
			}
		} else {
			logger.info("could not read the illumina arrays design file: " + illumiaArraysDesign);
		}
		logger.info("in loadIlluminaArraysDesign - illuminaArraysDesignMap size: " + illuminaArraysDesignMap.size());
	}

	static void loadIlluminaData(File illuminaFile, Map<ChrPosition, IlluminaRecord> illuminaMap) throws IOException {
		;
		try (IlluminaFileReader reader = new IlluminaFileReader(illuminaFile);) {
			for (final IlluminaRecord tempRec : reader) {
				
				// only interested in illumina data if it has a gc score above 0.7, and a valid chromosome
				// ignore chromosome 0, and for XY, create 2 records, one for each!
				// skip if the B allele ratio or Log R ratios are NaN
				// skip if non-dbSnp position
				
				if (tempRec.getGCScore() >= minGCScore 
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
	
	/**
	 * rgBases is a 2D array. Top level is readgroup, and each readgroup contains a sub-array of length 4. Once for each of ACGT.
	 * The elements in the sub-array correspond to the number of times that particular base was seen
	 * Returns null if the 2D array is null, or of zero length
	 * Will only tally the sub-arrays if they are of length 4.
	 * 
	 * @param rgBases
	 * @return
	 */
	public static String getTotalDist(int [][] rgBases) {
		if (null == rgBases || rgBases.length == 0) {
			return null;
		}
		int as = 0, cs = 0, gs = 0, ts = 0;
		for (int [] innerArray : rgBases) {
			if (innerArray.length == 4) {
				as += innerArray[0];
				cs += innerArray[1];
				gs += innerArray[2];
				ts += innerArray[3];
			}
		}
		return "" + as + Constants.MINUS + cs + Constants.MINUS + gs + Constants.MINUS + ts;
	}
	
	public static String getDist(int [][] rgBases, int position) {
		if (null == rgBases || rgBases.length < position || rgBases[position].length != 4) {
			return null;
		}
		if (rgBases[position][0] + rgBases[position][1] + rgBases[position][2] + rgBases[position][3] == 0) {
			return Constants.EMPTY_STRING;
		}
		return "" + rgBases[position][0] + Constants.MINUS + rgBases[position][1] + Constants.MINUS + rgBases[position][2] + Constants.MINUS + rgBases[position][3];
	}
	
	private void updateResultsIllumina(Map<ChrPosition, IlluminaRecord> iIlluminaMap) {
		
		// update the snps list with the details from the results map
		ChrPosition vcf = getNextPositionRecord("updateResultsIllumina-1");
		while (null != vcf) {
			/*
			 * create ChrPointPOsition from vcf (?!?) so that map lookup works
			 */
			ChrPointPosition cpForMap = ChrPointPosition.valueOf(vcf.getChromosome(), vcf.getStartPosition());
			// lookup corresponding snp in illumina map
			final IlluminaRecord illRec = iIlluminaMap.get(cpForMap);
			if (null != illRec) {
				final String [] params = illuminaArraysDesignMap.get(illRec.getSnpId());
				if (null == params) {
					logger.debug("didn't find entry in illuminaArraysDesignMap for snp id: " + illRec.getSnpId());
					continue;
				}
				
				String [] idAndRef = vcf.getName().split("\t");
				/*
				 * add to resultsToWrite
				 */
				final StringBuilder sb = new StringBuilder(vcf.getChromosome());
				sb.append(Constants.TAB);
				sb.append(vcf.getStartPosition());
				sb.append(Constants.TAB);
				sb.append(idAndRef[0]).append(Constants.TAB);			// id
				sb.append(idAndRef[1]).append(Constants.TAB);			// ref allele
				sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// alt allele
				sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// qual
				sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// filter
				sb.append("QAF=t:");										// info
				String coverageString = SignatureUtil.getCoverageStringForIlluminaRecord(illRec, params, 20, true);
				sb.append(coverageString);
				
				resultsToWrite.add(sb);
			}
			
			 vcf = getNextPositionRecord("updateResultsIllumina-2");
		}
	}
	
	private void writeOutput(File f, TObjectIntMap<String> rgIds, boolean bam) throws IOException {
		/*
		 * If outputFile is defined, and is not a directory, use that
		 * If outputFile is defined and is a directory, use directory and input filename
		 * If outputFile is not defined, look at outputDirectory
		 * If outputDirectory is defined, use directory and input filename
		 * If outputDirectory is not defined, use input directory and input filename.
		 * 
		 */
		File outputVCFFile = null;
		if (null != outputFile) {
			if (new File(outputFile).isDirectory()) {
				outputVCFFile = new File(outputFile + FileUtils.FILE_SEPARATOR + f.getName() + SignatureUtil.QSIG_VCF_GZ);
			} else {
				outputVCFFile = new File(outputFile);
			}
		} else if (null != outputDirectory) {
			outputVCFFile = new File(outputDirectory + FileUtils.FILE_SEPARATOR + f.getName() + SignatureUtil.QSIG_VCF_GZ);
		} else {
			outputVCFFile = new File(f.getAbsoluteFile() + SignatureUtil.QSIG_VCF_GZ);
		}
		logger.info("will write output vcf to file: " + outputVCFFile.getAbsolutePath());
		// standard output format
		// check that can write to new file
		if (FileUtils.canFileBeWrittenTo(outputVCFFile)) {
			final StringBuilder sbRgIds = new StringBuilder();
			
			/*
			 * convert TObjectIntMap to HashMap to use streaming etc
			 */
			Map<String, Integer> convertedMap = new HashMap<>();
			if (null != rgIds) {
				rgIds.forEachEntry((String k, int v) -> {
					convertedMap.putIfAbsent(k, Integer.valueOf(v));
					return true;
				} );
			}
			
				
			convertedMap.entrySet().stream()
				.sorted(Comparator.comparing(Entry::getValue))
				.forEach(e -> sbRgIds.append("##rg").append(e.getValue()).append(Constants.EQ).append(e.getKey()).append(Constants.NL));
			
			try (OutputStream os = new GZIPOutputStream(new FileOutputStream(outputVCFFile), 1024 * 1024)) {
				/*
				 * Header contains some standard bumf, some file specific bumf, and finally the column header
				 */
				os.write(getHeader(bam).toString().getBytes());
				os.write(("##input=" + f.getAbsolutePath() + Constants.NL).getBytes());
				if (null != rgIds) {
					os.write((sbRgIds.toString()).getBytes());
				}
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
		if (nextChromosome) {
			final String currentChr = cp.getChromosome();
			while (null != cp) {
				cp = getNextPositionRecord("advanceVCFAndPosition-1");
				if (null != cp && ! currentChr.equals(cp.getChromosome())) {
					break;
				}
			}
		} else {
			cp = getNextPositionRecord("advanceVCFAndPosition-2");
			if (null != rec) {
				while (null != cp) {
					if ( ! rec.getReferenceName().equals(cp.getChromosome())) {
						break;
					}
					if (rec.getAlignmentStart() <= cp.getStartPosition()) {
						break;
					}
					cp = getNextPositionRecord("advanceVCFAndPosition-3");
				}
			}
		}
	}
	
	private boolean match(SAMRecord rec, ChrPosition thisCP, boolean updatePointer) {
		if (null == thisCP) {
			return false;
		}
		
		String samChr = rec.getReferenceName();
		if (samChr.equals(thisCP.getChromosome())) {
			
			if (rec.getAlignmentEnd() < thisCP.getStartPosition()) {
				return false;
			}
			if (rec.getAlignmentStart() <= thisCP.getStartPosition()) {
				return true;
			}
			
			// finished with this cp - update results and get a new cp
			if (updatePointer) {
				advanceVCFAndPosition(false, rec);
				return match(rec, cp, true);
			} else {
				return false;
			}
			
			
		} else if (chrComparator.compare(samChr, thisCP.getChromosome()) < 1){
			// keep iterating through bam file 
			return false;
		} else {
			if (updatePointer) {
				// need to get next ChrPos
				advanceVCFAndPosition(true, rec);
				return match(rec, cp, true);
			} else {
				return false;
			}
		}
	}
	
	private void runSequentially(File bamFile) throws Exception {
		
		// setup count down latches
		final CountDownLatch pLatch = new CountDownLatch(1);
		final CountDownLatch cLatch = new CountDownLatch(1);
		final CountDownLatch wLatch = new CountDownLatch(1);
		
		// create executor service for producer and consumer
		
		final ExecutorService producerEx = Executors.newSingleThreadExecutor();
		final ExecutorService consumerEx = Executors.newSingleThreadExecutor();
		final ExecutorService writerEx = Executors.newSingleThreadExecutor();
		
		// set up producer
		producerEx.execute(new Producer(bamFile, pLatch, Thread.currentThread()));
		producerEx.shutdown();
		
		// setup consumer
		consumerEx.execute(new Consumer(pLatch, cLatch, Thread.currentThread()));
		consumerEx.shutdown();
		
		// setup writer
		/*
		 * If outputFile is defined, and is not a directory, use that
		 * If outputFile is defined and is a directory, use directory and input filename
		 * If outputFile is not defined, look at outputDirectory
		 * If outputDirectory is defined, use directory and input filename
		 * If outputDirectory is not defined, use input directory and input filename.
		 * 
		 */
		File outputVCFFile = null;
		if (null != outputFile) {
			if (new File(outputFile).isDirectory()) {
				outputVCFFile = new File(outputFile + FileUtils.FILE_SEPARATOR + bamFile.getName() + SignatureUtil.QSIG_VCF_GZ);
			} else {
				outputVCFFile = new File(outputFile);
			}
		} else if (null != outputDirectory) {
			outputVCFFile = new File(outputDirectory + FileUtils.FILE_SEPARATOR + bamFile.getName() + SignatureUtil.QSIG_VCF_GZ);
		} else {
			outputVCFFile = new File(bamFile.getAbsoluteFile() + SignatureUtil.QSIG_VCF_GZ);
		}
		logger.info("will write output vcf to file: " + outputVCFFile.getAbsolutePath());
		writerEx.execute(new Writer(wLatch, cLatch, outputVCFFile, Thread.currentThread(), bamFile, true));
		writerEx.shutdown();
		
		// wait till the producer count down latch has hit zero
		try {
			pLatch.await(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			throw new Exception("Exception caught in Producer thread");
		}
		
		// and now the consumer latch
		try {
			cLatch.await(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			throw new Exception("Exception caught in Consumer thread");
		}
		
		// and finally the writer latch
		try {
			wLatch.await(Constants.EXECUTOR_SERVICE_AWAIT_TERMINATION, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			throw new Exception("Exception caught in Consumer thread");
		}
	}
	
	private void updateResults(ChrPosition vcf, SAMRecord sam) {
		if (null != sam && null != vcf) {
			// get read index
			final int indexInRead = sam.getReadPositionAtReferencePosition(vcf.getStartPosition()) - 1;		// picard's method is 1-based
			if (indexInRead > -1) {
				final byte[] readBases = sam.getReadBases();
				if (indexInRead < readBases.length) {
				
					if (sam.getBaseQualities()[indexInRead] < minBaseQuality) {
						return;
					}
					
					final char c = (char) readBases[indexInRead];
					SAMReadGroupRecord srgr = sam.getReadGroup();
					String rgId = null != srgr ? srgr.getId() : null;
					
					int rgPosition = rgIds.get(rgId);
					int innerArrayPosition = c == 'A' ? 0 : (c == 'C' ? 1 : (c == 'G' ? 2 : (c == 'T' ? 3 : -1)));
					if (innerArrayPosition > -1) {
						results.computeIfAbsent(vcf, f -> new int[rgIds.size()][4])[rgPosition][innerArrayPosition]++;
					}
				}
			}
		}
	}
	
	/**
	 * Examines the positions file to see if it a vcf file.
	 * Does this in 2 ways - checks the name, and if it contains vcf, all good.
	 * If not, checks the file header to see if it contains the standard vcf header.
	 * 
	 * Returns a PositionInterator instance. If stream option has been set, uses that, otherwise uses an in memory version.
	 * 
	 * 
	 * @param randomSnpsFile
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private void setupPositionIterator(String randomSnpsFile) throws IOException, NoSuchAlgorithmException {
		
		isSnpPositionsAVcfFile = randomSnpsFile.contains("vcf");
		/*
		 * get header from file and examine that to see if it looks like a vcf
		 * set the positionsIterator depending on the type of file (vcf or string)
		 */
		if ( ! isSnpPositionsAVcfFile) {
			try (StringFileReader reader = new StringFileReader(new File(randomSnpsFile), "#")) {
				List<String> header = reader.getHeader();
				for (String h : header) {
					if (h.startsWith(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE)) {
						isSnpPositionsAVcfFile = true;
					}
				}
			}
		}
		
		positionsIterator = stream ? new VcfStreamPositionIterator(new File(randomSnpsFile), isSnpPositionsAVcfFile ? 3 : 4) : new VcfInMemoryPositionIterator(new File(randomSnpsFile), isSnpPositionsAVcfFile ? 3 : 4);
		
		logger.info("isSnpPositionsAVcfFile: " + isSnpPositionsAVcfFile);
	}
	
	private void getFileMd5Sum(String file) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
		md = MessageDigest.getInstance("MD5");
		/*
		 * get md5sum for snp positions file
		 */
		byte [] bytes = new byte[131072];
		try (InputStream in = new DigestInputStream(new FileInputStream(file), md)) {
			for (int n; (n = in.read(bytes)) != -1;) { }
		}
		snpPositionsMD5 = md.digest();
		logger.info("md5sum: " + DatatypeConverter.printHexBinary(snpPositionsMD5).toLowerCase());
	}
	
	
	private ChrPosition getNextPositionRecord(String from) {
		return getNextPositionRecord(false, -1, from);
	}
	private ChrPosition getNextPositionRecord(boolean populateCache, int lookForwardPosition, String from) {
		int cacheSize = nextCPs.size();
		ChrPosition thisCP = null;
		if (cacheSize == 0 || lookForwardPosition >= cacheSize) {
			/*
			 * get from iterator
			 */
			thisCP = positionsIterator.next();
			if (populateCache) {
				nextCPs.add(thisCP);
			} else {
				if (null != cp) {
					completedCPs.add(cp);
				}
			}
		} else {
			/*
			 * retrieving record from cache - don't re-insert
			 * if we are looking forward (ie. not consuming), then we do a get rather than a remove
			 */
			thisCP = lookForwardPosition >= 0 ? nextCPs.get(lookForwardPosition) : nextCPs.remove(0);
			if (lookForwardPosition == -1) {
				if (null != cp) {
					completedCPs.add(cp);
				}
			}
		}
		return thisCP;
	}
	
	/**
	 * A gff3 file containing a number of genes is what is expected in the randomGeneFile
	 * 
	 * 
	 * @param randomGeneFile
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private void setGenePositionsIterator(String randomGeneFile) throws IOException, NoSuchAlgorithmException {
		positionsIterator = new GeneModelInMemoryPositionIterator(new File(randomGeneFile), reference);
	}
	
	public static void main(String[] args) throws Exception {
		final Generate sp = new Generate();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (final Exception e) {
			exitStatus = 1;
			if (null != logger) {
				logger.error("Exception caught whilst running Generate:", e);
			} else {
				System.err.println("Exception caught whilst running Generate");
			}
			e.printStackTrace();
		}
		
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception {
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.GENERATOR_USAGE);
			System.exit(1);
		}
		final Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.GENERATOR_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.GENERATOR_USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.GENERATOR_USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(Generate.class, logFile, options.getLogLevel());
			exec = logger.logInitialExecutionStats("Generate", Generate.class.getPackage().getImplementationVersion(), args);
			
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
			
			if (null != options.getOutputFileNames() && options.getOutputFileNames().length > 0) {
				outputFile = options.getOutputFileNames()[0];
			}
			if (null != options.getDirNames() && options.getDirNames().length > 0) {
				outputDirectory = options.getDirNames()[0];
			}
			options.getMinMappingQuality().ifPresent(i -> minMappingQuality = i.intValue());
			options.getMinBaseQuality().ifPresent(i -> minBaseQuality = i.intValue());
			
			options.getIlluminaArraysDesign().ifPresent(i -> illumiaArraysDesign = i);
			options.getSnpPositions().ifPresent(s -> snpPositions = s);
			options.getGenePositions().ifPresent(g -> genePositions = g);
			options.getReference().ifPresent(r -> reference = r);
			options.getStream().ifPresent(s -> stream = s);
			
			if (stream) {
				logger.warn("Please ensure that the input file is in the same chromosome order as the positions file! IF this order is different, then any output generated will be incomplete.");
			}
			
			/*
			 * want either genePositions or snpPOsitions
			 */
			if ((null == snpPositions || ! FileUtils.canFileBeRead(snpPositions)) && (null == genePositions || ! FileUtils.canFileBeRead(genePositions))) {
				throw new QSignatureException("INSUFFICIENT_INPUT_FILES");
			}
			
			/*
			 * if we are dealing with a genePositions file, need to make sure we also have the reference file
			 */
			if (null != genePositions && (null == reference || ! FileUtils.canFileBeRead(reference))) {
				throw new QSignatureException("INCORRECT_GENE_INPUT_FILES");
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
				int topLevelCounter = 0;
				int bottomLevelCounter = 0;
				for (final SAMRecord rec : reader)  {
					// quality checks
					if (SAMUtils.isSAMRecordValidForVariantCalling(rec) 
							&& rec.getMappingQuality() >= minMappingQuality) {
						
						if (bottomLevelCounter++ > 1000000) {
							topLevelCounter++;
							bottomLevelCounter = 0;
							
							/*
							 * check queue - if we have more than a million entries - sleep
							 */
							int queueSize = sams.size();
							int loopCounter = 0;
							while (queueSize > 1000000) {
								if (loopCounter > 1000) {
									logger.warn("queue size still over 1m despite multiple sleeps! loopCounter: " + loopCounter);
								}
								Thread.sleep(50);
								loopCounter++;
								queueSize = sams.size();
							}
						}
						
						sams.add(rec);
					}
				}
				logger.info("Producer has processed " + ((topLevelCounter * 1000000) + topLevelCounter) + " records from input file");
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
	
	public class Writer implements Runnable {
		private final CountDownLatch wLatch;
		private final CountDownLatch cLatch;
		private final Thread mainThread;
		private final File outputVCFFile;
		private OutputStream os;
		private boolean isBam;
		private File inputFile;
		
		public Writer(CountDownLatch wLatch,  CountDownLatch cLatch, File outputVCFFile, Thread mainThread, File inputFile, boolean isBam) {
			
			this.wLatch = wLatch;
			this.cLatch = cLatch;
			this.mainThread = mainThread;
			this.isBam = isBam;
			this.inputFile = inputFile;
			this.outputVCFFile = outputVCFFile;
		}
		
		@Override
		public void run() {
			try {
				
				final StringBuilder sbRgIds = new StringBuilder();
				
				/*
				 * convert TObjectIntMap to HashMap to use streaming etc
				 */
				Map<String, Integer> convertedMap = new HashMap<>();
				if (null != rgIds) {
					rgIds.forEachEntry((String k, int v) -> {
						convertedMap.putIfAbsent(k, Integer.valueOf(v));
						return true;
					} );
				}
					
				convertedMap.entrySet().stream()
					.sorted(Comparator.comparing(Entry::getValue))
					.forEach(e -> sbRgIds.append("##rg").append(e.getValue()).append(Constants.EQ).append(e.getKey()).append(Constants.NL));
				
				os = new GZIPOutputStream(new FileOutputStream(outputVCFFile), 1024 * 1024);
				os.write(getHeader(isBam).toString().getBytes());
				os.write(("##input=" + inputFile.getAbsolutePath() + Constants.NL).getBytes());
				if (null != rgIds) {
					os.write((sbRgIds.toString()).getBytes());
				}
				os.write(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE.getBytes());
				os.write(Constants.NL);
				
				
				/*
				 * now pick up entries from queue
				 */
				ChrPosition cp = completedCPs.poll();
				while (true) {
					if (null == cp) {
						/*
						 * check to see if the consumer latch is zero
						 * If it is and we have no more elements in queue, then our work is done
						 */
						if (cLatch.getCount() == 0) {
							if (results.size() > 0) {
								/*
								 * add remaining cps to completedCPs queue (sort them first)
								 */
								completedCPs.addAll(results.keySet().stream().sorted(ChrPositionComparator.getComparator(chrComparator)).collect(Collectors.toList()));
								logger.info("added " + results.size() + " entries to completedCPs. completedCPs size: " + completedCPs.size());
							} else {
								break;
							}
						} else {
							/*
							 * sleep whilst waiting for element to arrive in queue and/or the latch to countdown
							 */
							Thread.sleep(50);
						}
					} else {
						
						final int [][] bsps = results.remove(cp);
						
						if (null != bsps && bsps.length > 0) {
							final StringBuilder sb = new StringBuilder(cp.getChromosome());
							int tabIndex = cp.getName().indexOf("\t");
							String id = cp.getName().substring(0, tabIndex);
							String ref = cp.getName().substring(tabIndex + 1);
							sb.append(Constants.TAB);
							sb.append(cp.getStartPosition());
							sb.append(Constants.TAB);
							sb.append(id).append(Constants.TAB);	// id
							sb.append(ref).append(Constants.TAB);						// ref allele
							sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// alt allele
							sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// qual
							sb.append(Constants.MISSING_DATA).append(Constants.TAB);	// filter
							sb.append("QAF=t:");										// info
			 				sb.append(getTotalDist(bsps));
							
							/*
							 * now again for the readgroups that we have
							 */
							
							for (int i = 0 ; i < bsps.length ; i++) {
								String readGroupSpecificDist = getDist(bsps, i);
								if ( ! readGroupSpecificDist.isEmpty()) { 
									sb.append(Constants.COMMA).append("rg" + i).append(Constants.COLON).append(readGroupSpecificDist);
								}
							}
						
							sb.append(Constants.NL);
							os.write(sb.toString().getBytes());
						}
					}
					cp = completedCPs.poll();
				}
			} catch (final Exception e) {
				logger.error("Exception caught in Producer thread - interrupting main thread", e);
				mainThread.interrupt();
			} finally {
				try {
					os.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					wLatch.countDown();
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
		
		private String getSamDeets(SAMRecord rec) {
			return rec.getReferenceName() + ":" + rec.getAlignmentStart() + "-" + rec.getAlignmentEnd();
		}
		
		@Override
		public void run() {
			final int intervalSize = 1000000;
			try {
				// reset some key values
				cp = null;
				// load first VCFRecord
				advanceVCFAndPosition(false, null);
				long recordCount = 0;
				// take items off the queue and process
				
				while (true) {
					final SAMRecord samFromQueue = sams.poll();
					if (null == samFromQueue) {
						// if latch is zero, producer is done, and so are we
						if (pLatch.getCount() == 0) {
							break;
						}
						try {
							Thread.sleep(10);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						
						if (++recordCount % intervalSize == 0) {
							logger.info("processed " + (recordCount / intervalSize) + "M records so far... nextCPs size: " + nextCPs.size() + ", results.size: " + results.size() + ", SAM recs in queue: " + sams.size() + ", sam: " + getSamDeets(samFromQueue) + ", cp: " + (null != cp ? cp.toIGVString() : "null"));
						}
						
						if (match(samFromQueue, cp, true)) {
							updateResults(cp, samFromQueue);
							
							// get next cp and see if it matches
							int j = 0;
							/*
							 * need to see if the next CP will also be covered by this record
							 * The next CP will be stored in a collection for retrieval at a later time (Iterator doesn't have a peek())
							 */
							ChrPosition nextCP = getNextPositionRecord(true, j++, "Consumer-1");
							if (null != nextCP) {
								while (match(samFromQueue, nextCP, false)) {
									updateResults(nextCP, samFromQueue);
									nextCP = getNextPositionRecord(true, j++, "Consumer-2");
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
