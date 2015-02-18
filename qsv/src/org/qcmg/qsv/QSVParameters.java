/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.annotate.Annotator;
import org.qcmg.qsv.annotate.RunTypeRecord;
import org.qcmg.qsv.util.QSVConstants;


/**
 * Class to hold all of the relevant parameters associated with a bam file.
 */
public class QSVParameters {

	private static QLogger logger = QLoggerFactory.getLogger(QSVParameters.class);
	public static final String FILE_SEPERATOR = System.getProperty("file.separator");

	private final boolean isTumor;    
	private final Integer clusterSize;
	private final Integer compareClusterSize;
	private final Annotator annotator;
	private final String resultsDir;
	private final Date analysisDate;    
	private final String pairingType;
	private final Integer qPrimerThreshold;
	private final String mapper;
	private final boolean runSoftClipAnalysis;
	private final List<String> readGroupIds = new ArrayList<String>();
	private final int repeatCountCutoff;

	private File inputBamFile;   
	private String findType;
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


	/**
	 * Instantiates a new qSV parameters instance
	 *
	 * @param options the options
	 * @param isTumor the is tumor
	 * @param masterDirPath the master dir path
	 * @param matePairFileDir the mate pair file dir
	 * @param analysisDate the analysis date
	 * @param donorName the donor name
	 * @throws Exception the exception
	 */
	public QSVParameters(Options options, boolean isTumor, String masterDirPath, String matePairFileDir,
			Date analysisDate, String donorName) throws Exception {
		this.sampleId = null;
		this.isTumor = isTumor;
		this.clusterSize = options.getClusterSize();
		this.compareClusterSize = options.getFilterSize();
		this.resultsDir = masterDirPath + donorName;
		this.analysisDate = analysisDate;
		this.pairingType = options.getPairingType();

		this.qPrimerThreshold = options.getQPrimerThreshold();
		this.mapper = options.getMapper();
		this.reference = options.getReference();
		this.runSoftClipAnalysis = options.runClipAnalysis(); 

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
			final String fileName = baseName.replace(".bam", ".discordantpair.filtered.bam");
			this.filteredBamFile = new File(options.getTempDirName() + FILE_SEPERATOR + fileName);
		}

		if (options.runClipPreprocess() || options.isSplitRead()) {        	
			final String baseName = this.inputBamFile.getName();         	
			this.clippedBamFile= new File(options.getTempDirName() + FILE_SEPERATOR  +  baseName.replace(".bam", ".softclip.filtered.bam"));
		}

		//get the names of the chromosomes that will be analysed
		getChromosomesToAnalyse(options);

		//get isizes
		getISizesFromIniFile(options);

		this.repeatCountCutoff = options.getREPEAT_COUNT_CUTOFF();
		logger.info("Max repeat count number: " + repeatCountCutoff);
		this.annotator = new Annotator(lowerInsertSize, upperInsertSize, new File(resultsDir + "." + findType + ".pairing_stats.xml"), pairingType, sequencingRuns, pairingType, mapper);	
	}

	public File getClippedBamFile() {
		return clippedBamFile;
	}

	public void setClippedBamFile(File clippedBamFile) {
		this.clippedBamFile = clippedBamFile;
	}

	private void getISizesFromIniFile(Options options) throws Exception {

		String parent = QSVConstants.CONTROL_SAMPLE;
		if (isTumor) {
			parent = QSVConstants.DISEASE_SAMPLE; 
		}

		final Ini iniFile = new Ini(new File(options.getIniFile()));				

		final Set<Entry<String, Section>> sectionList = iniFile.entrySet();

		//check to see if isizes have been provided
		this.sequencingRuns = new ArrayList<RunTypeRecord>();
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
						final RunTypeRecord runRecord = new RunTypeRecord(section.get("rgid"), new Integer(section.get("lower")), new Integer(section.get("upper")), name);
						sequencingRuns.add(runRecord);						
					} else {
						throw new QSVException("MISSING_INI_OPTION", section.getName());
					}
				} 
			}
		}

		//no isizes provided
		if (sequencingRuns.size() == 0 && options.runPairAnalysis()) {

			throw new QSVException("NO_ISIZES_ERROR");
			//			logger.info("Calculating isizes for " + inputBamFile);
			//			SAMRecordCounterMT est = new SAMRecordCounterMT(inputBamFile);
			//			if (est.getExitStatus() > 0) {
			//				throw new QSVException("COUNT_READS_ERROR");
			//			}
			//			this.sequencingRuns = est.getRunRecords();
		} else {
			if (options.runClipAnalysis() && !options.runPairAnalysis()) {
				//still need to getread groups
				final SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(inputBamFile, "silent");
				for (final SAMReadGroupRecord record: reader.getFileHeader().getReadGroups()) {
					readGroupIds.add(record.getId());
					lowerInsertSize = 0;
					upperInsertSize = 1000;
				}

			} else {
				for (final RunTypeRecord r: sequencingRuns) {
					readGroupIds.add(r.getRgId());
					logger.info("Insert size for sample " + findType + ": " + r.toString());					
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
	private void getChromosomesToAnalyse(Options options) throws QSVException {

		final SAMFileReader inputSam = SAMFileReaderFactory.createSAMFileReader(inputBamFile, "silent"); 
		this.header = inputSam.getFileHeader();        

		//get chromosomes to run
		final SAMSequenceDictionary sequenceDictionary = header.getSequenceDictionary();
		final List<SAMSequenceRecord> sequenceRecords = sequenceDictionary.getSequences();
		this.chromosomes = new HashMap<String, List<Chromosome>>();

		//get all chromosomes
		if (options.allChromosomes() || options.getIncludeTranslocations()) {
			for (final SAMSequenceRecord seq : sequenceRecords) {
				final String key = seq.getSequenceName();				
				final Chromosome c = new Chromosome(key, seq.getSequenceLength());
				final List<Chromosome> list = new ArrayList<>(1);
				list.add(c);
				chromosomes.put(key, list);
			}        	        
		} else {
			//get those ranges provided
			for (final String r : options.getRanges()) {
				//subsection of a chromosome
				if (r.contains(":")) {
					final String chrName = r.split(":")[0];
					final String pos = r.split(":")[1];
					final Integer start = new Integer(pos.split("-")[0]);
					final Integer end = new Integer(pos.split("-")[1]);
					for (final SAMSequenceRecord seq : sequenceRecords) {
						if (seq.getSequenceName().equals(chrName)) {
							final Chromosome c = new Chromosome(chrName, seq.getSequenceLength(), start, end);
							if (chromosomes.containsKey(chrName)) {
								chromosomes.get(chrName).add(c);
							} else {
								final List<Chromosome> list = new ArrayList<Chromosome>();
								list.add(c);
								chromosomes.put(chrName, list);
							}			        		
						}
					}									      					
				} else {
					//entire chromosome
					final String chrName = r;
					for (final SAMSequenceRecord seq : sequenceRecords) {
						if (seq.getSequenceName().equals(chrName)) {
							final Chromosome c= new Chromosome(chrName, seq.getSequenceLength());
							if (chromosomes.containsKey(chrName)) {
								chromosomes.get(chrName).add(c);
							} else {
								final List<Chromosome> list = new ArrayList<Chromosome>();
								list.add(c);
								chromosomes.put(chrName, list);
							}
						}
					}					
				}
			}
		}
		inputSam.close();
	}

	/**
	 * Gets the LIMS meta.
	 *
	 * @return the lIMS meta
	 */
	public String getLIMSMeta() {
		final List<String> comments = header.getComments();
		for (final String c: comments) {
			if (c.split("\t").length >= 3) {
				if (c.split("\t")[2].equals("QN:qlimsmeta")) {    				
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

	//	/**
	//	 * Sets the input bam file.
	//	 *
	//	 * @param inputBamFile the new input bam file
	//	 */
	//	public void setInputBamFile(File inputBamFile) {
	//		this.inputBamFile = inputBamFile;
	//	}

	/**
	 * Checks if is the tumor sample.
	 *
	 * @return true, if is tumor
	 */
	public boolean isTumor() {
		return isTumor;
	}

	//	/**
	//	 * Sets boolean to say if it is the tumor sample.
	//	 *
	//	 * @param isTumor the new tumor sample boolean
	//	 */
	//	public void setTumor(boolean isTumor) {
	//		this.isTumor = isTumor;
	//	}

	/**
	 * Gets the find type.
	 *
	 * @return the find type
	 */
	public String getFindType() {
		return findType;
	}

	//	/**
	//	 * Sets the find type.
	//	 *
	//	 * @param findType the new find type
	//	 */
	//	public void setFindType(String findType) {
	//		this.findType = findType;
	//	}

	/**
	 * Gets the cluster size.
	 *
	 * @return the cluster size
	 */
	public Integer getClusterSize() {
		return clusterSize;
	}

	//	/**
	//	 * Sets the cluster size.
	//	 *
	//	 * @param clusterSize the new cluster size
	//	 */
	//	public void setClusterSize(Integer clusterSize) {
	//		this.clusterSize = clusterSize;
	//	}

	/**
	 * Gets the compare cluster size.
	 *
	 * @return the compare cluster size
	 */
	public Integer getCompareClusterSize() {
		return compareClusterSize;
	}

	//	/**
	//	 * Sets the compare cluster size.
	//	 *
	//	 * @param compareClusterSize the new compare cluster size
	//	 */
//		public void setCompareClusterSize(Integer compareClusterSize) {
//			this.compareClusterSize = compareClusterSize;
//		}

	/**
	 * Gets the sample id.
	 *
	 * @return the sample id
	 */
	public String getSampleId() {
		return sampleId;
	}

	//	/**
	//	 * Sets the sample id.
	//	 *
	//	 * @param sampleId the new sample id
	//	 */
	//	public void setSampleId(String sampleId) {
	//		this.sampleId = sampleId;
	//	}

	/**
	 * Gets the filtered bam file.
	 *
	 * @return the filtered bam file
	 */
	public File getFilteredBamFile() {
		return filteredBamFile;
	}

	//	/**
	//	 * Sets the filtered bam file.
	//	 *
	//	 * @param filteredBamFile the new filtered bam file
	//	 */
	//	public void setFilteredBamFile(File filteredBamFile) {
	//		this.filteredBamFile = filteredBamFile;
	//	}

	/**
	 * Gets the header.
	 *
	 * @return the header
	 */
	public SAMFileHeader getHeader() {
		return header;
	}

	//	/**
	//	 * Sets the header.
	//	 *
	//	 * @param header the new header
	//	 */
	//	public void setHeader(SAMFileHeader header) {
	//		this.header = header;
	//	}

	/**
	 * Gets the lower insert size.
	 *
	 * @return the lower insert size
	 */
	public Integer getLowerInsertSize() {
		return lowerInsertSize;
	}

	//	/**
	//	 * Sets the lower insert size.
	//	 *
	//	 * @param lowerInsertSize the new lower insert size
	//	 */
	//	public void setLowerInsertSize(Integer lowerInsertSize) {
	//		this.lowerInsertSize = lowerInsertSize;
	//	}

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

	//	/**
	//	 * Sets the annotator.
	//	 *
	//	 * @param annotator the new annotator
	//	 */
	//	public void setAnnotator(Annotator annotator) {
	//		this.annotator = annotator;
	//	}

	/**
	 * Gets the average insert size.
	 *
	 * @return the average insert size
	 */
	public int getAverageInsertSize() {
		return averageInsertSize;
	}

	//	/**
	//	 * Sets the average insert size.
	//	 *
	//	 * @param averageInsertSize the new average insert size
	//	 */
	//	public void setAverageInsertSize(int averageInsertSize) {
	//		this.averageInsertSize = averageInsertSize;
	//	}

	/**
	 * Gets the results dir.
	 *
	 * @return the results dir
	 */
	public String getResultsDir() {
		return resultsDir;
	}

	//	/**
	//	 * Sets the results dir.
	//	 *
	//	 * @param resultsDir the new results dir
	//	 */
	//	public void setResultsDir(String resultsDir) {
	//		this.resultsDir = resultsDir;
	//	}

	/**
	 * Gets the analysis date.
	 *
	 * @return the analysis date
	 */
	public Date getAnalysisDate() {
		return analysisDate;
	}

	//	/**
	//	 * Sets the analysis date.
	//	 *
	//	 * @param analysisDate the new analysis date
	//	 */
	//	public void setAnalysisDate(Date analysisDate) {
	//		this.analysisDate = analysisDate;
	//	}

	/**
	 * Gets the pairing type.
	 *
	 * @return the pairing type
	 */
	public String getPairingType() {
		return pairingType;
	}

	//	/**
	//	 * Sets the pairing type.
	//	 *
	//	 * @param pairingType the new pairing type
	//	 */
	//	public void setPairingType(String pairingType) {
	//		this.pairingType = pairingType;
	//	}

	/**
	 * Gets the sequencing runs.
	 *
	 * @return the sequencing runs
	 */
	public List<RunTypeRecord> getSequencingRuns() {
		return sequencingRuns;
	}

	//	/**
	//	 * Sets the sequencing runs.
	//	 *
	//	 * @param sequencingRuns the new sequencing runs
	//	 */
	//	public void setSequencingRuns(List<RunTypeRecord> sequencingRuns) {
	//		this.sequencingRuns = sequencingRuns;
	//	}

	/**
	 * Gets the q primer threshold.
	 *
	 * @return the q primer threshold
	 */
	public Integer getqPrimerThreshold() {
		return qPrimerThreshold;
	}

	//	/**
	//	 * Sets the q primer threshold.
	//	 *
	//	 * @param qPrimerThreshold the new q primer threshold
	//	 */
	//	public void setqPrimerThreshold(Integer qPrimerThreshold) {
	//		this.qPrimerThreshold = qPrimerThreshold;
	//	}

	/**
	 * Gets the chromosomes.
	 *
	 * @return the chromosomes
	 */
	public Map<String, List<Chromosome>> getChromosomes() {
		return chromosomes;
	}

	//	/**
	//	 * Sets the chromosomes.
	//	 *
	//	 * @param chromosomes the chromosomes
	//	 */
	//	public void setChromosomes(Map<String, List<Chromosome>> chromosomes) {
	//		this.chromosomes = chromosomes;
	//	}

	/**
	 * Gets the mapper.
	 *
	 * @return the mapper
	 */
	public String getMapper() {
		return mapper;
	}

	//	/**
	//	 * Sets the mapper.
	//	 *
	//	 * @param mapper the new mapper
	//	 */
	//	public void setMapper(String mapper) {
	//		this.mapper = mapper;
	//	}

	/**
	 * Gets the reference.
	 *
	 * @return the reference
	 */
	public String getReference() {
		return reference;
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

	//	/**
	//	 * Sets the run soft clip analysis.
	//	 *
	//	 * @param runSoftClipAnalysis the new run soft clip analysis
	//	 */
	//	public void setRunSoftClipAnalysis(boolean runSoftClipAnalysis) {
	//		this.runSoftClipAnalysis = runSoftClipAnalysis;
	//	}

	/**
	 * Gets the read group ids.
	 *
	 * @return the read group ids
	 */
	public List<String> getReadGroupIds() {
		return readGroupIds;
	}
	
	public Set<String> getReadGroupIdsAsSet() {
		return new HashSet<String>(readGroupIds);
	}

	//	/**
	//	 * Sets the read group ids.
	//	 *
	//	 * @param readGroupIds the new read group ids
	//	 */
	//	public void setReadGroupIds(List<String> readGroupIds) {
	//		this.readGroupIds = readGroupIds;
	//	}

	public int getRepeatCountCutoff() {
		return this.repeatCountCutoff;
	}
}
