/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import au.edu.qimr.tiledaligner.PositionChrPositionMap.LongRange;
import htsjdk.samtools.*;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.annotate.Annotator;
import org.qcmg.qsv.annotate.RunTypeRecord;
import org.qcmg.qsv.util.QSVConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.Map.Entry;


/**
 * Class to hold all of the relevant parameters associated with a bam file.
 */
public class QSVParameters {

	private static final QLogger logger = QLoggerFactory.getLogger(QSVParameters.class);
	public static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

	private final boolean isTumor;    
	private final Integer clusterSize;
	private final Integer compareClusterSize;
	private final Annotator annotator;
	private final String resultsDir;
    private final String pairingType;
    private final String mapper;
	private final boolean runSoftClipAnalysis;
	private Collection<String> readGroupIds;
	private final int repeatCountCutoff;

	private final File inputBamFile;
	private final String findType;
	private String sampleId;
	private File filteredBamFile;
	private SAMFileHeader header;
	private Integer lowerInsertSize  = -1;
	private Integer upperInsertSize = -1;
	private int averageInsertSize;
	private List<RunTypeRecord> sequencingRuns;
	private Map<String, List<Chromosome>> chromosomes;
	private String reference;
	private File clippedBamFile;
	
	private final Map<ChrPosition, LongRange> refIndexPositionMap;


	/**
	 * Instantiates a new qSV parameters instance
	 *
	 * @param options the options
	 * @param isTumor the is tumor
	 * @param masterDirPath the master dir path
	 * @param donorName the donor name
	 * @throws Exception the exception
	 */
	public QSVParameters(Options options, boolean isTumor, String masterDirPath,
						 String donorName, Map<ChrPosition, LongRange> refIndexPositionMap) throws Exception {
		this.sampleId = null;
		this.isTumor = isTumor;
		this.clusterSize = options.getClusterSize();
		this.compareClusterSize = options.getFilterSize();
		this.resultsDir = masterDirPath + donorName;
        this.pairingType = options.getPairingType();

        this.mapper = options.getMapper();
		this.reference = options.getReference();
		this.runSoftClipAnalysis = options.runClipAnalysis();
		
		this.refIndexPositionMap = refIndexPositionMap;

		if (isTumor) {
			this.inputBamFile = new File(options.getInputFile());            
			this.findType = options.getInputFileAbbreviation(); 
			this.sampleId = options.getInputSampleId();
		} else {
			this.inputBamFile = new File(options.getComparisonFile()); 
			this.findType = options.getComparisonFileAbbreviation();
			this.sampleId = options.getComparisonSampleId();             
		}  

		//set filtered bam file name
		if (options.runPairPreprocess()) {	       
			final String baseName = inputBamFile.getName();
			//clarify sam and bam; because seekablestream can't open sam file even the file extension name is .bam;			 
			this.filteredBamFile = new File(options.getTempDirName() + FILE_SEPARATOR + getUpdatedFileName(baseName, ".discordantpair.filtered"));
		}

		if (options.runClipPreprocess() || options.isSplitRead()) {        	
			final String baseName = this.inputBamFile.getName();  
			//clarify sam and bam; because seekablestream can't open sam file even the file extension name is .bam;			 
			this.clippedBamFile= new File(options.getTempDirName() + FILE_SEPARATOR + getUpdatedFileName(baseName, ".softclip.filtered"));
		}

		//get the names of the chromosomes that will be analysed
		getChromosomesToAnalyse(options);

		//get isizes
		getISizesFromIniFile(options);

		this.repeatCountCutoff = options.getRepeatCountCutoff();
		logger.info("Max repeat count number: " + repeatCountCutoff);
		this.annotator = new Annotator(lowerInsertSize, upperInsertSize, new File(resultsDir + "." + findType + ".pairing_stats.xml"), pairingType, sequencingRuns, pairingType, mapper);	
	}

	public static String getUpdatedFileName(String fileName, String extension) {
		if (fileName.endsWith(".sam")) {
			return fileName.replace(".sam", extension + ".sam");
		} else if (fileName.endsWith(".bam")) {
			return fileName.replace(".bam", extension + ".bam");
		} else if (fileName.endsWith(".cram")) {
			return fileName.replace(".cram", extension + ".cram");
		} else {
			return fileName + extension;
		}
	}

	public File getClippedBamFile() {
		return clippedBamFile;
	}

	private void getISizesFromIniFile(Options options) throws Exception {

		String parent = QSVConstants.CONTROL_SAMPLE;
		if (isTumor) {
			parent = QSVConstants.DISEASE_SAMPLE; 
		}

		final Ini iniFile = new Ini(new File(options.getIniFile()));				

		final Set<Entry<String, Section>> sectionList = iniFile.entrySet();

		//check to see if isizes have been provided
		this.sequencingRuns = new ArrayList<>();
		//		int count = 0;
		for (final Entry<String, Section> s : sectionList) {
			final Section section = s.getValue();

			if (section.getParent() != null) {				
				if (section.getParent().getName().equals(parent)) {	
					if (section.containsKey("lower") && section.containsKey("upper") && section.containsKey("rgid")) {				
						//						count++;
						String name = "";
						if (section.get("name") != null) {
							name = section.get("name");
						}
						final RunTypeRecord runRecord = new RunTypeRecord(section.get("rgid"), Integer.parseInt(section.get("lower")), Integer.parseInt(section.get("upper")), name);
						/*
						 * check to see if we have valid iSize values (ie. lower is < upper)
						 */
						if (runRecord.getLower() > runRecord.getUpper()) {
							throw new QSVException("INVALID_ISIZES_ERROR", "Lower isize value (" + runRecord.getLower() + ") is greater than or equal to upper isize (" + runRecord.getUpper() + ")");
						}
						sequencingRuns.add(runRecord);						
					} else {
						throw new QSVException("MISSING_INI_OPTION", section.getName());
					}
				} 
			}
		}

		//no isizes provided
		if (sequencingRuns.isEmpty() && options.runPairAnalysis()) {
			throw new QSVException("NO_ISIZES_ERROR");
		} else {
			if (options.runClipAnalysis() && ! options.runPairAnalysis()) {
				//still need to get read groups
				try (final SamReader reader = SAMFileReaderFactory.createSAMFileReader(inputBamFile, "silent")) {
					
					int rgCount = reader.getFileHeader().getReadGroups().size();
					readGroupIds = rgCount == 1 ? new ArrayList<>(2) : new HashSet<>(rgCount);
					lowerInsertSize = 0;
					upperInsertSize = 1000;
					
					for (final SAMReadGroupRecord record: reader.getFileHeader().getReadGroups()) {
						readGroupIds.add(record.getId());
					}
				}

			} else {
				int rgCount = sequencingRuns.size();
				readGroupIds = rgCount == 1 ? new ArrayList<>(2) : new HashSet<>(rgCount);
				for (final RunTypeRecord r: sequencingRuns) {
					readGroupIds.add(r.getRgId());
					logger.info("Insert size for sample " + findType + ": " + r);
				}

				//work out the upper and lower insert sizes for clustering
				for (final RunTypeRecord record : sequencingRuns) {
					if (record.getLower() < lowerInsertSize || lowerInsertSize == -1) {
						lowerInsertSize = record.getLower();			
					}
					if (record.getUpper() == -1 || record.getUpper() > upperInsertSize) {
						upperInsertSize = record.getUpper();
					}
				}
				this.averageInsertSize = ((this.upperInsertSize - this.lowerInsertSize)/2) + this.lowerInsertSize;				
			}
		}


	}	

	/*
	 * Determine which chromosomes will be analysed in this run
	 */
	private void getChromosomesToAnalyse(Options options) throws IOException {

		try ( SamReader inputSam = SAMFileReaderFactory.createSAMFileReader(inputBamFile, "silent")) { 
			this.header = inputSam.getFileHeader();        
	
			//get chromosomes to run
			final SAMSequenceDictionary sequenceDictionary = header.getSequenceDictionary();
			final List<SAMSequenceRecord> sequenceRecords = sequenceDictionary.getSequences();
			this.chromosomes = new HashMap<>();
	
			//get all chromosomes
			if (options.allChromosomes() || options.getIncludeTranslocations()) {
				for (final SAMSequenceRecord seq : sequenceRecords) {
					final String key = seq.getSequenceName();				
					final Chromosome c = new Chromosome(key, seq.getSequenceLength());
					chromosomes.put(key, List.of(c));
				}        	        
			} else {
				//get those ranges provided
				for (final String r : options.getRanges()) {
					//subsection of a chromosome
					if (r.contains(":")) {
						String [] rParams = r.split(":");
						final String chrName = rParams[0];
						final String pos = rParams[1];
						String [] posParams = pos.split("-");
						final int start = Integer.parseInt(posParams[0]);
						final int end = Integer.parseInt(posParams[1]);
						for (final SAMSequenceRecord seq : sequenceRecords) {
							if (seq.getSequenceName().equals(chrName)) {
								final Chromosome c = new Chromosome(chrName, seq.getSequenceLength(), start, end);
								chromosomes.computeIfAbsent(chrName, v -> new ArrayList<>()).add(c);
							}
						}									      					
					} else {
						//entire chromosome
                        for (final SAMSequenceRecord seq : sequenceRecords) {
							if (seq.getSequenceName().equals(r)) {
								final Chromosome c= new Chromosome(r, seq.getSequenceLength());
								chromosomes.computeIfAbsent(r, v -> new ArrayList<>()).add(c);
							}
						}					
					}
				}
			}
		}
	}

	/**
	 * Gets the LIMS meta.
	 *
	 * @return the lIMS meta
	 */
	public String getLIMSMeta() {
		final List<String> comments = header.getComments();
		for (final String c: comments) {
			String [] cParams = c.split("\t");
			if (cParams.length >= 3) {
				if (cParams[2].equals("QN:qlimsmeta")) {    				
					return c;
				}
			}
		}
		return null;
	}

	/**
	 * Gets the isize report string.
	 *
	 * @return the isize report string
	 */
	public String getISizeReportString() {
		final StringBuilder sb = new StringBuilder();
		for (int i=0; i< sequencingRuns.size(); i++) {
			final RunTypeRecord r = sequencingRuns.get(i);
			sb.append(r.getReportDataString());
			if (i != sequencingRuns.size()-1) {
				sb.append("/");
			}
		}
		return sb.toString();
	}

	/**
	 * Gets the input bam file.
	 *
	 * @return the input bam file
	 */
	public File getInputBamFile() {
		return inputBamFile;
	}

	/**
	 * Checks if is the tumor sample.
	 *
	 * @return true, if is tumor
	 */
	public boolean isTumor() {
		return isTumor;
	}

	/**
	 * Gets the find type.
	 *
	 * @return the find type
	 */
	public String getFindType() {
		return findType;
	}

	/**
	 * Gets the cluster size.
	 *
	 * @return the cluster size
	 */
	public Integer getClusterSize() {
		return clusterSize;
	}

	/**
	 * Gets the compare cluster size.
	 *
	 * @return the compare cluster size
	 */
	public Integer getCompareClusterSize() {
		return compareClusterSize;
	}
	/**
	 * Gets the sample id.
	 *
	 * @return the sample id
	 */
	public String getSampleId() {
		return sampleId;
	}

	/**
	 * Gets the filtered bam file.
	 *
	 * @return the filtered bam file
	 */
	public File getFilteredBamFile() {
		return filteredBamFile;
	}

	/**
	 * Gets the header.
	 *
	 * @return the header
	 */
	public SAMFileHeader getHeader() {
		return header;
	}

	/**
	 * Gets the lower insert size.
	 *
	 * @return the lower insert size
	 */
	public Integer getLowerInsertSize() {
		return lowerInsertSize;
	}

	/**
	 * Gets the upper insert size.
	 *
	 * @return the upper insert size
	 */
	public Integer getUpperInsertSize() {		
		return upperInsertSize;
	}

		/**
		 * Sets the upper insert size.
		 *
		 * @param upperInsertSize the new upper insert size
		 */
		public void setUpperInsertSize(Integer upperInsertSize) {
			this.upperInsertSize = upperInsertSize;
		}

	/**
	 * Gets the annotator.
	 *
	 * @return the annotator
	 */
	public Annotator getAnnotator() {
		return annotator;
	}

	/**
	 * Gets the average insert size.
	 *
	 * @return the average insert size
	 */
	public int getAverageInsertSize() {
		return averageInsertSize;
	}

	/**
	 * Gets the results dir.
	 *
	 * @return the results dir
	 */
	public String getResultsDir() {
		return resultsDir;
	}

	/**
	 * Gets the pairing type.
	 *
	 * @return the pairing type
	 */
	public String getPairingType() {
		return pairingType;
	}

	/**
	 * Gets the sequencing runs.
	 *
	 * @return the sequencing runs
	 */
	public List<RunTypeRecord> getSequencingRuns() {
		return sequencingRuns;
	}

	/**
	 * Gets the chromosomes.
	 *
	 * @return the chromosomes
	 */
	public Map<String, List<Chromosome>> getChromosomes() {
		return chromosomes;
	}

	/**
	 * Gets the mapper.
	 *
	 * @return the mapper
	 */
	public String getMapper() {
		return mapper;
	}

	/**
	 * Gets the reference.
	 *
	 * @return the reference
	 */
	public String getReference() {
		return reference;
	}
	
	public Map<ChrPosition, LongRange> getRefIndexPositionMap() {
		return refIndexPositionMap;
	}

	/**
	 * Sets the reference.
	 *
	 * @param reference the new reference
	 */
	public void setReference(String reference) {
		this.reference = reference;
	}

	/**
	 * Checks if is run soft clip analysis.
	 *
	 * @return true, if is run soft clip analysis
	 */
	public boolean isRunSoftClipAnalysis() {
		return runSoftClipAnalysis;
	}

	/**
	 * Gets the read group ids.
	 *
	 * @return the read group ids
	 */
	public Collection<String> getReadGroupIds() {
		return readGroupIds;
	}
	
	public Set<String> getReadGroupIdsAsSet() {
		return new HashSet<>(readGroupIds);
	}

	public int getRepeatCountCutoff() {
		return this.repeatCountCutoff;
	}
}
