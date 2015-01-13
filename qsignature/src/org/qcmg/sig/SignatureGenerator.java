/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.ListUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfPositionComparator;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderFilter;
import org.qcmg.common.vcf.header.VcfHeaderInfo;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoNumber;
import org.qcmg.common.vcf.header.VcfHeaderRecord.VcfInfoType;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.illumina.IlluminaFileReader;
import org.qcmg.illumina.IlluminaRecord;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.record.Record;
import org.qcmg.sig.model.BaseStrandPosition;
import org.qcmg.sig.util.SignatureUtil;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileWriter;


public class SignatureGenerator {
	
	private final static ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private final static String QSIG_REPORT = ".txt.qsig.report.txt";
	
	static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String illumiaArraysDesign;
	private int exitStatus;
	
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
	
	private final List<VcfRecord> snps = new ArrayList<VcfRecord>();
	private final Map<VcfRecord, List<BaseStrandPosition>> results = new ConcurrentHashMap<VcfRecord, List<BaseStrandPosition>>();
	private final AbstractQueue<SAMRecord> sams = new ConcurrentLinkedQueue<SAMRecord>();
	private final Map<String, String[]> IlluminaArraysDesign = new ConcurrentHashMap<String, String[]>();
	
	
	public int engage() throws Exception {
		
		bamFiles = FileUtils.findFilesEndingWithFilter(cmdLineInputFiles[1], ".bam");
		if (bamFiles.length == 0) {
			illuminaFiles = FileUtils.findFilesEndingWithFilter(cmdLineInputFiles[1], ".txt");
			logger.info("Found " + illuminaFiles.length + " illumina snp chip files to process");
			Collections.sort(snps, new VcfPositionComparator());
			
			final List<File> results = new LinkedList<File>();
			// remove any files from array that are .report.txt files
			for (final File f : illuminaFiles) {
				if ( ! f.getName().endsWith(QSIG_REPORT)) {
					// add to new list
					results.add(f);
				} else {
					logger.info("Skipping " + f.getName());
				}
			}
			
			illuminaFiles = results.toArray(new File[0]);
			
			if (illuminaFiles.length > 0) {
				
				// load snp file - 1.5 million in here...
				loadRandomSnpPositions(cmdLineInputFiles[0]);
				
				// load in the Illumina arrays design document to get the list of snp ids and whether they should be complemented.
				loadIlluminaArraysDesign();
			}
		}
		
		for (final File illuminaFile : illuminaFiles) {
			
			// load contents of each illumina file into mem
			final Map<ChrPosition, IlluminaRecord> iIlluminaMap = new HashMap<ChrPosition, IlluminaRecord>(1250000);	// not expecting more than 1000000
			
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
			
			 
			final VcfHeader header = getHeaderForQSigIlluminaFile(patient, sample, inputType, 
					illuminaFile.getName(), cmdLineInputFiles[0]);
			
			loadIlluminaData(illuminaFile, iIlluminaMap);
			logger.info("Illumina data loaded: " + iIlluminaMap.size());
			
			updateResultsIllumina(iIlluminaMap);
			logger.info("updateResults - DONE");
			
			logger.info("about to write output");
			writeVCFOutput(illuminaFile, header);
			
			// clean out results, and erase info field from snps
			results.clear();
			for (final VcfRecord vcf : snps) vcf.setInfo(null);
		}		
		
		if (null != bamFiles &&  bamFiles.length > 0) {
			
			for (final File bamFile : bamFiles) {
			
				// set some bam specific values
				arrayPosition = 0;
				vcf = null;
				
				final VcfHeader header = generateVcfHeader(bamFile, cmdLineInputFiles[0]);
				
				// load snp file - 1.5 million in here...
				if (snps.isEmpty()) {		//may have been loaded when dealing with illumina files
					loadRandomSnpPositions(cmdLineInputFiles[0]);
				}
				
				createComparatorFromSAMHeader(bamFile);
				
				runSequentially(bamFile);
				
				updateResults();
				
				// output vcf file
				writeVCFOutput(bamFile, header);
				
				// clean out results, and erase info field from snps
				results.clear();
				for (final VcfRecord vcf : snps) vcf.setInfo(null);
			}
		}
		
		return exitStatus;
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
					IlluminaArraysDesign.put(id, params);
				}
			}
		} else {
			logger.info("Could not read the illumina arrays design file: " + illumiaArraysDesign);
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
						&& Float.NaN != tempRec.getbAlleleFreq()
						&& Float.NaN != tempRec.getLogRRatio()
						&& tempRec.getSnpId().startsWith("rs")) {
					
					// only deal with bi-allelic snps
					final String snp = tempRec.getSnp();
					if (snp.length() == 5 &&  '/' == snp.charAt(2)) {
					
						if ("XY".equals(tempRec.getChr())) {
							// add both X and Y to map
							illuminaMap.put(new ChrPosition("chrX", tempRec.getStart()), tempRec);
							illuminaMap.put(new ChrPosition("chrY", tempRec.getStart()), tempRec);
						} else {
							// 	Illumina record chromosome does not contain "chr", whereas the positionRecordMap does - add
							illuminaMap.put(new ChrPosition("chr" + tempRec.getChr(), tempRec.getStart()), tempRec);
						}
					}
				}
			}
		}
	}
	
	void createComparatorFromSAMHeader(File fileName) {
		if (null == fileName) throw new IllegalArgumentException("null file passed to createComparatorFromSAMHeader");
		
		final List<String> sortedContigs = new ArrayList<String>();
		
		try (SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(fileName)) {
			final SAMFileHeader header = reader.getFileHeader();
			for (final SAMSequenceRecord contig : header.getSequenceDictionary().getSequences()) {
				sortedContigs.add(contig.getSequenceName());
			}
		}
		
		// try and sort according to the ordering of the bam file that is about to be processed
		// otherwise, resort to alphabetic ordering and cross fingers...
		if ( ! sortedContigs.isEmpty()) {
			
			chrComparator = ListUtils.createComparatorFromList(sortedContigs);
			
			Collections.sort(snps, new Comparator<VcfRecord>() {
				@Override
				public int compare(VcfRecord o1, VcfRecord o2) {
					final int diff = chrComparator.compare(o1.getChromosome(), o2.getChromosome());
					if (diff != 0) return diff;
					return o1.getPosition() - o2.getPosition();
				}
			});
			
		} else {
			chrComparator = COMPARATOR;
			Collections.sort(snps, new VcfPositionComparator());
		}
		
		final Set<String> uniqueChrs = new HashSet<String>();
		logger.info("chr order:");
		for (final VcfRecord vcf : snps) {
			if (uniqueChrs.add(vcf.getChromosome())) {
				logger.info(vcf.getChromosome());
			}
		}
	}
	
	private void updateResults() throws Exception {
		// update the snps list with the details from the results map
		
		for (final Entry<VcfRecord, List<BaseStrandPosition>> entry : results.entrySet()) {
			final VcfRecord thisVCF = entry.getKey();
			final List<BaseStrandPosition> bsps = entry.getValue();
			
			if (null == bsps || bsps.isEmpty()) {
				thisVCF.setInfo(SignatureUtil.EMPTY_COVERAGE);
			} else {
				final StringBuilder allBases = new StringBuilder();
				final StringBuilder novelStartBases = new StringBuilder();
				final Set<Integer> forwardStrand = new HashSet<Integer>();
				final Set<Integer> reverseStrand = new HashSet<Integer>();
				
				for (final BaseStrandPosition bsp : bsps) {
					allBases.append(bsp.getBase());
					
					if (bsp.isForwardStrand()) {
						if (forwardStrand.add(bsp.getStartPosition()))
							novelStartBases.append(bsp.getBase());
					} else {
						if (reverseStrand.add(bsp.getStartPosition()))
							novelStartBases.append(bsp.getBase());
					}
				}
				
				final List<PileupElement> pileup = PileupElementUtil.getPileupCounts(allBases.toString());
				final List<PileupElement> novelStartPileup = PileupElementUtil.getPileupCounts(novelStartBases.toString());
				String info = VcfUtils.getPileupElementAsString(pileup, false);
				info += ";";
				info += VcfUtils.getPileupElementAsString(novelStartPileup, true);
				thisVCF.setInfo(info);
			}
		}
	}
	
	private void updateResultsIllumina(Map<ChrPosition, IlluminaRecord> iIlluminaMap) throws Exception {
		
		// update the snps list with the details from the results map
		for (final VcfRecord snp : snps) {
			
			// lookup corresponding snp in illumina map
			final IlluminaRecord illRec = iIlluminaMap.get(new ChrPosition(snp.getChromosome(), snp.getPosition()));
			if (null == illRec) continue;
			
			final String [] params = IlluminaArraysDesign.get(illRec.getSnpId());
			if (null == params) continue;
			
			snp.setInfo(SignatureUtil.getCoverageStringForIlluminaRecord(illRec, params, 20));
			
		}
		
	}
	
	
	private void writeVCFOutput(File bamFile, VcfHeader header) throws Exception {
		// if we have an output folder defined, place the vcf files there, otherwise they will live next to the bams
		File outputVCFFile = null;
		if (null != outputDirectory) {
			outputVCFFile = new File(outputDirectory + FileUtils.FILE_SEPARATOR + bamFile.getName() + ".qsig.vcf.gz");
		} else {
			outputVCFFile = new File(bamFile.getAbsoluteFile() + ".qsig.vcf.gz");
		}
		logger.info("Will write output vcf to file: " + outputVCFFile.getAbsolutePath());
		// standard output format
		// check that can wriite to new file
		if (FileUtils.canFileBeWrittenTo(outputVCFFile)) {
			
			try (VCFFileWriter writer = new VCFFileWriter(outputVCFFile, true);){
				// write header
				for(final VcfHeaderRecord re: header)
					writer.addHeader(re.toString() );
				
				for (final VcfRecord vcf : snps) {
					if (StringUtils.isNullOrEmpty(vcf.getInfo())) vcf.setInfo(SignatureUtil.EMPTY_COVERAGE);
					writer.add(vcf);
				}
				
			}
		} else {
			logger.warn("Can't write to output vcf file: " + outputVCFFile.getAbsolutePath());
		}
	}
	
	
	private VcfHeader getBasicHeaderForQSig(final String bamName, final String snpFile, String ... bamHeaderInfo) throws Exception {
		
		String patient = null;
		String library = null;
		if (null != bamHeaderInfo && bamHeaderInfo.length > 0) {
			patient = bamHeaderInfo[0];
			library = bamHeaderInfo.length > 1 ? bamHeaderInfo[1] : null; 
		}
		
		final VcfHeader header = new VcfHeader();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");
		final String version = SignatureGenerator.class.getPackage().getImplementationVersion();
		final String pg = Messages.getProgramName();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();

		//move input uuid into preuuid
		header.add(new VcfHeaderRecord(VcfHeaderUtils.CURRENT_FILE_VERSION));		
		header.add(new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate ));
		header.add(new VcfHeaderRecord(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid ));
		header.add(new VcfHeaderRecord( "##bam_file=" + bamName ) );
		header.add( new VcfHeaderRecord("##snp_file=" + snpFile  ));
		header.add( new VcfHeaderFilter("BASE_QUALITY", "Base quality < " + minBaseQuality ) );
		header.add( new VcfHeaderFilter("MAPPING_QUALITY", "Mapping quality < " + minMappingQuality ) );
//		header.add( new VcfHeaderFilter("##filter_q_score=10") );
//		header.add( new VcfHeaderRecord("##filter_match_qual=10"));		
//		header.add( new VcfHeaderRecord(VcfHeaderUtils.FILTER_LOW_QUAL,"REQUIRED: QUAL < 50.0") );
		header.add( new VcfHeaderInfo("FULLCOV", VcfInfoNumber.UNKNOWN, -1, VcfInfoType.String, "all bases at position", null, null) );
		header.add( new VcfHeaderInfo("NOVELCOV", VcfInfoNumber.UNKNOWN, -1, VcfInfoType.String, "bases at position from reads with novel starts", null, null)  );
  		header.add(new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE));
		return  header;
		
/*		
		return "##fileformat=VCFv4.0\n" +
		(patient != null ? 	("##patient_id=" + patient + "\n")  : "") +
		(library != null ? 	("##library=" + library  + "\n")  : "") +
		"##bam=" + bamName + "\n" +
		"##snp_file=" + snpFile + "\n" + 
		"##filter_q_score=10\n" + 
		"##filter_match_qual=10\n" + 
		"##FILTER=<ID=LowQual,Description=\"REQUIRED: QUAL < 50.0\">\n" + 
		"##INFO=<ID=FULLCOV,Number=.,Type=String,Description=\"all bases at position\">\n" + 
		"##INFO=<ID=NOVELCOV,Number=.,Type=String,Description=\"bases at position from reads with novel starts\">\n" + 
		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE+ "\n";*/
	}
	
	private VcfHeader getHeaderForQSigIlluminaFile(final String patientId,  final String sample,
			final String inputType, final String illuminaFileName, final String snpFile) throws Exception {

 		
		final VcfHeader header = new VcfHeader();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");
		final String version = SignatureGenerator.class.getPackage().getImplementationVersion();
		final String pg = Messages.getProgramName();
		final String fileDate = df.format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();

		//move input uuid into preuuid
		header.add( new VcfHeaderRecord(VcfHeaderUtils.CURRENT_FILE_VERSION));		
		header.add( new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate ));
		header.add( new VcfHeaderRecord(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid ));
		header.add( new VcfHeaderRecord(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version) );
		header.add( new VcfHeaderRecord("##patient_id=" + patientId));
		header.add( new VcfHeaderRecord("##input_type=" + inputType ));
		header.add( new VcfHeaderRecord( "##sample=" + sample ) );
		header.add( new VcfHeaderRecord( "##bam_file=" + illuminaFileName ) );		
		header.add( new VcfHeaderRecord("##snp_file=" + snpFile  ));
		header.add( new VcfHeaderRecord("##genome=GRCh37_ICGC_standard_v2.fa\n"));		
		header.add( new VcfHeaderRecord("##filter_q_score=10") );
		header.add( new VcfHeaderRecord("##filter_match_qual=10"));	
		
		header.add( new VcfHeaderFilter(VcfHeaderUtils.FILTER_LOW_QUAL,"REQUIRED: QUAL < 50.0") );
		header.add( new VcfHeaderInfo("FULLCOV", VcfInfoNumber.UNKNOWN, -1, VcfInfoType.String, "all bases at position", null, null) );
		header.add( new VcfHeaderInfo("NOVELCOV", VcfInfoNumber.UNKNOWN, -1, VcfInfoType.String, "bases at position from reads with novel starts", null, null)  );
  		header.add(new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE));
		return  header;
		
		
/*		return "##fileformat=VCFv4.0\n" +
		"##patient_id=" + patientId + "\n" + 
		"##input_type=" + inputType  + "\n" +
		"##sample=" + sample  + "\n" +
		"##bam=" + illuminaFileName + "\n" +
		"##snp_file=" + snpFile + "\n" + 
		"##filter_q_score=10\n" + 
		"##filter_match_qual=10\n" + 
		"##genome=GRCh37_ICGC_standard_v2.fa\n" + 
		"##FILTER=<ID=LowQual,Description=\"REQUIRED: QUAL < 50.0\">\n" + 
		"##INFO=<ID=FULLCOV,Number=.,Type=String,Description=\"all bases at position\">\n" + 
		"##INFO=<ID=NOVELCOV,Number=.,Type=String,Description=\"bases at position from reads with novel starts\">\n" + 
		VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE+ "\n";*/
	}
	
	private VcfHeader generateVcfHeader(File file, String snpChipFile) throws Exception {
		// not hitting the LIMS anymore - get what we can from the bam header
		final String [] bamHeaderInfo = new String[2];
		try (SAMFileReader samReader = SAMFileReaderFactory.createSAMFileReader(file);) {
			
			final SAMFileHeader header = samReader.getFileHeader();
			for (final SAMReadGroupRecord srgr : header.getReadGroups()) {
				if ( ! StringUtils.isNullOrEmpty(srgr.getSample())) {
					bamHeaderInfo[0] = srgr.getSample();
				}
				if ( ! StringUtils.isNullOrEmpty(srgr.getLibrary())) {
					bamHeaderInfo[1] = srgr.getLibrary();
				}
			}
		}
		
		return getBasicHeaderForQSig(file.getAbsolutePath(), snpChipFile, bamHeaderInfo);
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
				if ( ! currentChr.equals(vcf.getChromosome()))
					break;
			}
			
		} else {
			vcf = snps.get(arrayPosition++);
			if (null != rec) {
				final int startPosition = rec.getAlignmentStart();
				final String currentChr = rec.getReferenceName();
				while (arrayPosition < arraySize) {
					if ( ! currentChr.equals(vcf.getChromosome()))
						break;
					if (startPosition <= vcf.getPosition())
						break;
					vcf = snps.get(arrayPosition++);
				}
			}
		}
	}
	
	private boolean match(SAMRecord rec, VcfRecord thisVcf, boolean updatePointer) {
		//logger.info("vcf: " + thisVcf.getChromosome() + ":" + thisVcf.getPosition() + ", rec: " + rec.getReferenceName() + ":" + rec.getAlignmentStart());
		if (null == thisVcf) return false;
		
		final String samChr = rec.getReferenceName().startsWith("chr") ? rec.getReferenceName() : "chr" + rec.getReferenceName();
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
		// get read index
		final int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, vcf.getPosition());
		
		if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
			
			if (sam.getBaseQualities()[indexInRead] < minBaseQuality) return;
			
			final char c = sam.getReadString().charAt(indexInRead);
			final int position = sam.getAlignmentStart();
			final boolean negativeStrand = sam.getReadNegativeStrandFlag();
			
			final BaseStrandPosition bsp = new BaseStrandPosition(c, ! negativeStrand, position);
			
			List<BaseStrandPosition> bsps = results.get(vcf);
			if (null == bsps) {
				bsps = new ArrayList<BaseStrandPosition>();
				results.put(vcf, bsps);
			}
			bsps.add(bsp);
		}
	}
	
	private void loadRandomSnpPositions(String randomSnpsFile) throws Exception {
		int count = 0;
		try (TabbedFileReader reader = new TabbedFileReader(new File(randomSnpsFile));){
			for (final TabbedRecord rec : reader) {
				++count;
				final String[] params = TabTokenizer.tokenize(rec.getData());
				
				String ref = null;
				if (params.length > 4 && null != params[4] && params[4].length() == 1) {
					ref = params[4];
				} else if (params.length > 3 && null != params[3] && params[3].length() == 1){
					// mouse file has ref at position 3 (0-based)
					ref = params[3];
				}
				
				if (params.length < 2) {
					throw new IllegalArgumentException("snp file must have at least 2 tab seperated columns, chr and position");
				}
				
				final String chr = params[0];
				final int position = Integer.parseInt(params[1]);
				final VcfRecord vcf =VcfUtils.createVcfRecord(chr, position, ref);
				
				if (params.length > 2)
					vcf.setId(params[2]);
				if (params.length > 5)
					vcf.setAlt(params[5].replaceAll("/", ","));

				// Lynns new files are 1-based - no need to do any processing on th position
				snps.add(vcf);
			}
			
			arraySize = snps.size();
			logger.info("Loaded " + arraySize + " positions into map (should be equal to: " + count + ")");
		}
	}
	
	public static void main(String[] args) throws Exception {
		final SignatureGenerator sp = new SignatureGenerator();
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
			logger = QLoggerFactory.getLogger(SignatureGenerator.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("SignatureGenerator", SignatureGenerator.class.getPackage().getImplementationVersion(), args);
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 2) {
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
			if (null != options.getMinMappingQuality()) {
				minMappingQuality = options.getMinMappingQuality().intValue();
			}
			if (null != options.getMinBaseQuality()) {
				minBaseQuality = options.getMinBaseQuality().intValue();
			}
			
			if (options.hasIlluminaArraysDesignOption()) {
				illumiaArraysDesign = options.getIlluminaArraysDesign();
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
		private final SAMFileReader reader;
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
							logger.info("Processed " + (recordCount / intervalSize) + "M records so far..");
						}
						
						if (match(sam, vcf, true)) {
//							logger.info("got a match!");
							updateResults(vcf, sam);
							
							// get next cp and see if it matches
							int j = 0;
							if (arrayPosition < arraySize) {
								VcfRecord tmpVCF = snps.get(arrayPosition + j++);
								while (match(sam, tmpVCF, false)) {
									//								logger.info("got a subsequent match!");
									updateResults(tmpVCF, sam);
									if (arrayPosition + j < arraySize)
										tmpVCF = snps.get(arrayPosition + j++);
									else tmpVCF = null;
								}
							}
						}
					}
				}
				logger.info("Processed " + recordCount + " records");
			} catch (final Exception e) {
				logger.error("Exception caught in Consumer thread - interrupting main thread", e);
				mainThread.interrupt();
			} finally {
				cLatch.countDown();
			}
		}
	}
}
