/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

import htsjdk.samtools.reference.ReferenceSequenceFileFactory;


/**
 * Class to parse the user supplied options.  
 *
 */
public class Options {
	public static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
	
	private static final String HELP_OPTION = Messages.getMessage("HELP_OPTION");	
	private static final String VERSION_OPTION = Messages.getMessage("VERSION_OPTION");
	private static final String INI_OPTION = Messages.getMessage("INI_OPTION");
	private static final String TEMP_DIR_OPTION = Messages.getMessage("TEMPDIR_OPTION");
	private static final String UUID_OPTION = Messages.getMessage("UUID_OPTION");;
	
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private String log;
	private String loglevel;
	private String comparisonFile;
	private String inputFile;		
	private String outputDirName;
	private String outputDirNameOverride;
	private final String uuid;
	private String sampleName;
	private Integer filterSize;
	private Integer clusterSize;
	private String tempDirName;
	private String preprocessMode;
	private String analysisMode;
	private String comparisonFileAbbreviation;
	private String inputFileAbbreviation;
	private String pairingType;
	private final String iniFile;
	private String query;
	private Integer qPrimerThreshold;
	private String inputSampleId;
	private String comparisonSampleId;
	private List<String> ranges;
	private String reference;
	private String referenceIndex;
	private String mapper;
	private String clipQuery;
	private String tiledAlignerFile;
	private String maxISizeCount;
	private boolean isQCMG = false;
	private boolean isSplitRead = true;
	private boolean singleSided;
	private boolean singleFileMode = false;
	private boolean twoFileMode = true;
	private Integer minInsertSize;
	private Integer consensusLength;
	private Integer clipSize;
	private String platform;
	private String sequencingPlatform;
	private List<String> gffFiles = new ArrayList<>();
	private boolean includeTranslocations;
	private boolean allChromosomes = true;
	private int repeatCountCutoff = 1000;


	/**
	 * Takes arguments from the command line and sets up the options for qsv 
	 */
	public Options(final String[] args) throws QSVException, IOException {
		
		//define the options the parse accepts		
        parser.accepts("ini", INI_OPTION).withRequiredArg().ofType(String.class);
        parser.accepts("output-temporary", TEMP_DIR_OPTION).withRequiredArg().ofType(String.class);
        parser.accepts("uuid", UUID_OPTION).withOptionalArg().ofType(String.class);
		parser.accepts("help", HELP_OPTION);
		parser.acceptsAll(List.of("version"), VERSION_OPTION);
		options = parser.parse(args);	
		//logging
		
		iniFile = (String) options.valueOf("ini");
		tempDirName = (String) options.valueOf("output-temporary");
		
		/*
		 *User can overwrite the output at command line - 
		 */
		uuid = options.has("uuid") ? (String) options.valueOf("uuid") :  QExec.createUUid() ;
		
	}
	
	@Deprecated
	public String getOverrideOutput() {
		return outputDirNameOverride;
	}
	
	//run id will be sub folder name, it will be a new uuid if null
	public String getUuid() {
		return uuid; 
	}

	/**
	 * Parses ini file options
	 * @throws QSVException
	 * @throws InvalidFileFormatException
	 * @throws IOException
	 */
	public void parseIniFile() throws QSVException, InvalidFileFormatException, IOException {
    	if (null == iniFile) {
			throw new QSVException("NO_INI");
		}
		if (!iniFile.contains("ini")) {
			throw new QSVException("NO_INI");
		}	
    	Ini ini = new Ini(new File(iniFile));   	
		
    	//General params
		Section generalSection = ini.get("general");
		
		log = generalSection.get("log");
		loglevel = generalSection.get("loglevel");
		sampleName = generalSection.get("sample");
		tiledAlignerFile = generalSection.get("tiled_aligner");
		
		if (log == null) {
			log = sampleName + ".log";
		}
		
		//create output directory, the base name should be run id		
		outputDirName = generalSection.get("output");
		if (outputDirName != null) {
			if ( ! directoryExists(outputDirName)) {
                throw new QSVException("NO_OUTPUT_DIR", outputDirName);
			}
			outputDirName = (outputDirName.endsWith(FILE_SEPARATOR) ? outputDirName : outputDirName + FILE_SEPARATOR) + uuid + FILE_SEPARATOR;
			createResultsDirectory(outputDirName);
		} else {
			throw new QSVException("NO_OUTPUT");
		}	
		
		if (generalSection.get("min_insert_size") != null) {
			minInsertSize = Integer.valueOf(generalSection.get("min_insert_size"));
		} else {
			minInsertSize = QSVConstants.DEFAULT_MIN_INSERT_SIZE;
		}
		
		if (generalSection.get("repeat_cutoff") != null) {
			repeatCountCutoff = Integer.parseInt(generalSection.get("repeat_cutoff"));
		}

		//ranges can be null, means all chromosome
		ranges = generalSection.getAll("range");			
		processRanges();			
	
		
		String splitRead = generalSection.get("split_read");
		if (splitRead != null && splitRead.equals("false")) {
			isSplitRead = false;
		}
		
		reference = generalSection.get("reference");
		referenceIndex = generalSection.get("reference_index");
		/*
		 * if the reference index has not been supplied see if it sits next to the reference file
		 */
		if (null == referenceIndex && null != reference) {
			Path indexPath = ReferenceSequenceFileFactory.getFastaIndexFileName(Paths.get(reference));
			if (Files.isReadable(indexPath)) {
				referenceIndex = indexPath.toString();
			}
		}
		
		if (generalSection.get("sv_analysis") != null) {
			this.preprocessMode = generalSection.get("sv_analysis");
			this.analysisMode = generalSection.get("sv_analysis");
		} else {
			this.preprocessMode = QSVConstants.DEFAULT_SV_ANALYSIS;
			this.analysisMode = QSVConstants.DEFAULT_SV_ANALYSIS;
		}		
		
		if (generalSection.get("platform") != null) {
			this.sequencingPlatform = generalSection.get("platform");
		} else {
			this.sequencingPlatform = QSVConstants.DEFAULT_SEQ_PLATFORM;
		}		
		
		if (this.sequencingPlatform != null) {
			if (sequencingPlatform.toLowerCase().contains("solid")) {
				platform = "solid";
			} else if (sequencingPlatform.toLowerCase().contains("hiseq") || sequencingPlatform.toLowerCase().contains("illumina")) {
				platform = "illumina";
			} else if (sequencingPlatform.toLowerCase().contains("novaseq") || sequencingPlatform.toLowerCase().contains("nextseq")) {
				/*
				 * add novaseq and nextseq to the list of allowed sequencers. Treat them as illumina (for now)
				 */
				platform = "illumina";	
			} else if (sequencingPlatform.toLowerCase().contains("bgiseq") || sequencingPlatform.toLowerCase().contains("mgiseq")) {
				platform = "bgi";
			} else {
				throw new QSVException("PLATFORM_ERROR", sequencingPlatform);			
			}
		} else {
			throw new QSVException("PLATFORM_ERROR", sequencingPlatform);
		}
		
		maxISizeCount = "all";
		
		
		if (generalSection.get("qcmg") != null) {
			if (generalSection.get("qcmg").equals("true")) {
				isQCMG = true;
			}
		}
		
		this.gffFiles = generalSection.getAll("gff");
		
		if (gffFiles == null) {
			gffFiles = new ArrayList<>();
		}				

		//Pairing params
		Section pairSection = ini.get("pair");
		
		if (pairSection != null) {
			pairingType = pairSection.get("pairing_type");
			mapper = pairSection.get("mapper");
			query = pairSection.get("pair_query");
			if (pairSection.get("cluster_size") != null) {
				clusterSize = Integer.valueOf(pairSection.get("cluster_size"));
			}
			if (pairSection.get("filter_size") != null) {
				filterSize = Integer.valueOf(pairSection.get("filter_size"));
			} 
		}
		
		if (pairingType == null) {
			pairingType = QSVConstants.DEFAULT_PAIRING_TYPE;
		}
		
		if (pairingType.equals("lmp") && platform.equals("illumina")) {
			pairingType = "imp";
		}
		
		if (mapper == null) {
			mapper = QSVConstants.DEFAULT_MAPPER;
		}		
				
		if (query == null) {
			query = QSVUtil.getPairQuery(this.pairingType, this.mapper);
		}	
			
		if (clusterSize == null) {
			clusterSize = QSVConstants.DEFAULT_CLUSTER_SIZE;			
		}		
				
		if (filterSize == null) {
			filterSize = QSVConstants.DEFAULT_FILTER_SIZE;		
		}
		
		qPrimerThreshold = 3;
		
		
		//Clipping params
		Section clipSection = ini.get("clip");
		if (clipSection != null) {
			clipQuery = clipSection.get("clip_query");
			if (clipQuery == null) {
				clipQuery = QSVConstants.DEFAULT_CLIP_QUERY;
			} 
			
			if (clipSection.get("clip_size") != null) {
				clipSize = Integer.parseInt(clipSection.get("clip_size"));
			} else {
				clipSize = QSVConstants.DEFAULT_CLIP_SIZE;
			}
			if (clipSection.get("consensus_length") != null) {
				consensusLength = Integer.parseInt(clipSection.get("consensus_length")); 
			} else {
				consensusLength = QSVConstants.DEFAULT_CONSENSUS_LENGTH;
			}
			
			String s = clipSection.get("single_side_clip");			
			if (s != null) {
				if (s.equals("true")) {
					singleSided = true;
				}
			}	
		}
		if (analysisMode.equals("pair")) {
			isSplitRead = false;
		}
		
		Section tumourSection = ini.get(QSVConstants.DISEASE_SAMPLE); 		
		inputFileAbbreviation = tumourSection.get("name");
		inputFile = tumourSection.get("input_file");
		if (tumourSection.get("sample_id") != null) {
			inputSampleId = tumourSection.get("sample_id");
		} else {
			inputSampleId = inputFileAbbreviation;
		}		
		
		Section normalSection = ini.get(QSVConstants.CONTROL_SAMPLE);
		if (normalSection != null) {
			comparisonFileAbbreviation = normalSection.get("name");		
			comparisonFile = normalSection.get("input_file");	
			
			if (normalSection.get("sample_id") != null) {
				comparisonSampleId = normalSection.get("sample_id");
			} else {
				comparisonSampleId = comparisonFileAbbreviation;
			}
		} else {
			singleFileMode = true;
			twoFileMode = false;
		}
		detectBadOptions();
	}

	public int getRepeatCountCutoff() {
		return repeatCountCutoff;
	}

	private void processRanges() throws QSVException {
		includeTranslocations = false;
		allChromosomes = true;
		if (ranges != null && ! ranges.isEmpty()) {
			allChromosomes = false;
			for (String s: ranges) {
                if (s.equals("inter")) {
                    includeTranslocations = true;
                    break;
                }
			}			
			if (ranges.size() > 1 && includeTranslocations) {
				throw new QSVException("RANGE_ERROR");
			}
		}		
	}

	/**
	 * Detects any problems with the supplied options
     */
	public void detectBadOptions() throws QSVException {
		if (log == null) {
			throw new QSVException("MISSING_LOG_FILE");
		} else
		
		if (preprocessMode.equals("clip") && analysisMode.equals("pair") || preprocessMode.equals("pair") && analysisMode.equals("clip")
				|| preprocessMode.equals("pair") && analysisMode.equals("both") || preprocessMode.equals("clip") && analysisMode.equals("both")
				) {
			throw new QSVException("INCOMPATABLE_MODES", preprocessMode, analysisMode);
		} 
		if (!analysisMode.equals("pair") && !analysisMode.equals("clip") && !analysisMode.equals("both")) {
			throw new QSVException("MODE_ERROR");
		}		
		
		if (null == comparisonFile && !singleFileMode) {
		    throw new QSVException("NULL_COMPARISON_FILE");     
		} else if  (comparisonFile != null) {
			if (!new File(comparisonFile).exists()) {
				throw new QSVException("NO_COMPARISON_FILE", comparisonFile);
			}
		} 
		if (reference != null) {
			if ( ! new File(reference).exists()) {
				throw new QSVException("NO_REFERENCE_FILE", reference);	
			}
		} else {
			if (isQCMG) {
				throw new QSVException("NULL_REFERENCE_FILE");
			}
		}
		if (referenceIndex != null) {
			if ( ! new File(referenceIndex).exists()) {
				throw new QSVException("NO_REFERENCE_INDEX_FILE", referenceIndex);	
			}
		} else {
			if (isQCMG) {
				throw new QSVException("NULL_REFERENCE_INDEX_FILE");
			}
		}
		
		if (tiledAlignerFile == null || ! new File(tiledAlignerFile).canRead()) {
			throw new QSVException("NO_TILED_ALIGNER_FILE", tiledAlignerFile);	
		}
		
		if (null == inputFile) {
			throw new QSVException("NULL_INPUT_FILE");				
		} else if ( ! new File(inputFile).exists()) {
			throw new QSVException("NO_INPUT_FILE", inputFile);					
		}
		
 		//it is compulsory to specify output directory
		if (null ==  outputDirName) {  
			throw new QSVException("NULL_OUTPUT_DIR");
		} else if ( ! directoryExists(outputDirName)) {
			throw new QSVException("NO_OUTPUT_DIR", outputDirName);
		}
		
		if (null ==  tempDirName) {
			throw new QSVException("NULL_TEMP_DIR");
		} else if ( ! directoryExists(tempDirName)) {
			throw new QSVException("NO_TEMP_DIR");		
		} else if (StringUtils.isNullOrEmpty(sampleName)) {
			throw new QSVException("NO_DONOR");
		} else if (null == filterSize || filterSize.equals(0)) {
			throw new QSVException("NO_CLUSTER_NORMAL");
		} else if (null == clusterSize || clusterSize.equals(0)) {
			throw new QSVException("NO_CLUSTER_TUMOR");		
		} else if (null == sampleName) {
		    throw new QSVException("NO_SAMPLE_NAME");
		} else if (null == iniFile || !iniFile.contains("ini")) {
			throw new QSVException("NO_INI");
		} 
		
	    //check if the run type exists			
	    if (pairingType == null) {
	        throw new QSVException("NO_RUN_TYPE");
	    }	
	    if ((!pairingType.equals("lmp") && !pairingType.equals("pe") && !pairingType.equals("lifescope") && !pairingType.equals("imp"))) {
	    	throw new QSVException("NO_RUN_TYPE");
	    }
			
		if (pairingType.equals("pe") && mapper == null) {
			throw new QSVException("NO_MAPPER");
		}
		
		if (pairingType.equals("pe") && !mapper.equals("bwa") && !mapper.equals("bioscope") && !mapper.equals("novoalign") && !mapper.equals("bwa-mem")) {
			throw new QSVException("NO_MAPPER");
		}
	}
	
	/**
	 * Checks for version option
	 * @return true if successful
	 */
	public boolean hasVersionOption() {
		return options.has("v") || options.has("V") || options.has("version");
	}
	
	/**
	 * Checks for help option
	 * @return true if successful
	 */
	public boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}
	
	/**
	 * Display help information
	 */
    public void displayHelp() throws Exception {
    	parser.formatHelpWith(new BuiltinHelpFormatter(150, 2));
		parser.printHelpOn(System.err);
	}

	public List<String> getGffFiles() {
		return gffFiles;
	}

	public Integer getMinInsertSize() {
		return minInsertSize;
	}

	public boolean isSplitRead() {
		return isSplitRead;
	}

	public String getPreprocessMode() {
		return preprocessMode;
	}

	public void setPreprocessMode(String preprocessMode) {
		this.preprocessMode = preprocessMode;
	}

	public String getAnalysisMode() {
		return analysisMode;
	}

	public void setAnalysisMode(String analysisMode) {
		this.analysisMode = analysisMode;
	}

    public String getComparisonFile() {
        return comparisonFile;
    }

    public String getInputFile() {
        return inputFile;
    }

    public String getOutputDirName() {
        return outputDirName;
    }

    public String getSampleName() {
        return sampleName;
    }

    public Integer getFilterSize() {
        return filterSize;
    }

    public Integer getClusterSize() {
        return clusterSize;
    }

    public String getTempDirName() {
        return tempDirName;
    }

    public String getComparisonFileAbbreviation() {
        return comparisonFileAbbreviation;
    }

    public String getInputFileAbbreviation() {
        return inputFileAbbreviation;
    }

	public String getReference() {
		return reference;
	}
	
	public String getReferenceIndex() {
		return referenceIndex;
	}
	
	public String getLog() {
		return log;
	}	

	public Integer getClipSize() {
		return clipSize;
	}

	public Integer getConsensusLength() {
		return consensusLength;
	}

	public String getMapper() {
		return mapper;
	}

	public void setMapper(String mapper) {
		this.mapper = mapper;
	}

	public String getPairingType() {
		return pairingType;
	}
	
    public String getPlatform() {
		return platform;
	}

	public boolean directoryExists(String directoryName) {
		File file = new File(directoryName);
		return file.exists() && file.isDirectory();
	}

    public String getLogLevel() {
       return loglevel;
    }

	public String getIniFile() {
		return this.iniFile;
	}

	public String getPairQuery() {
		return this.query;
	}

	public Integer getQPrimerThreshold() {
		return this.qPrimerThreshold;
	}

	public String getInputSampleId() {
		return this.inputSampleId;
	}

	public String getComparisonSampleId() {
		return this.comparisonSampleId;
	}

	public List<String> getRanges() {
		return this.ranges;
	}

	public String getClipQuery() {
		return this.clipQuery;
	}

	public String getMaxISizeCount() {
		return this.maxISizeCount;
	}

	public boolean isQCMG() {
		return isQCMG;
	}
	
	public boolean singleSided() {
		return singleSided;
	}	

	public void setLogFile(String log) {
		this.log = log;		
	}

	public void setTmpDir(String tmp) {
		this.tempDirName = tmp;		
	}

	public void setPairingType(String pairingType) {
		this.pairingType = pairingType;		
	}

	public boolean isSingleFileMode() {
		return singleFileMode;
	}

	public boolean isTwoFileMode() {
		return twoFileMode;
	}

	public boolean runClipAnalysis() {
		return (analysisMode.equals("both") || analysisMode.equals("clip")) ;
	}	
	
	public boolean runPairAnalysis() {
		return (analysisMode.equals("both") || analysisMode.equals("pair"));
	}
	
	public boolean runPairPreprocess() {
		return  (preprocessMode.equals("both") || preprocessMode.equals("pair"));
	}
	
	public boolean runClipPreprocess() {
		return  (preprocessMode.equals("both") || preprocessMode.equals("clip")) ;
	}

	public String getSequencingPlatform() {
		return sequencingPlatform;
	}
	public String getTiledAligner() {
		return tiledAlignerFile;
	}

	public String translateReference() {
		File ref = new File(reference);
		if (ref.exists()) {
			if (ref.getName().equals("GRCh37_ICGC_standard_v2.fa")) {
				return "GRCh37.p2";
			}
		}
		return reference;
	}

	public boolean getIncludeTranslocations() {
		return this.includeTranslocations;
	}

	public boolean allChromosomes() {
		return allChromosomes;
	}

	/**
	 * Create the results directory for qsv
	 * @throws QSVException if the directory already exists
	 */
	public static void createResultsDirectory(String directoryToCreate) throws QSVException {
	    File resultsDir = new File(directoryToCreate);
	    	/*
	    	 * No longer check to see if directory already exists
	    	 */
	    resultsDir.mkdir();
	     if ( ! resultsDir.exists()) {
		    throw new QSVException("DIR_CREATE_ERROR", directoryToCreate);   
	     }
	}
}
