/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.qcmg.common.string.StringUtils;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;


/**
 * Class to parse the user supplied options.  
 *
 */
public class Options {
	
	private static final String HELP_OPTION = Messages.getMessage("HELP_OPTION");	
	private static final String VERSION_OPTION = Messages.getMessage("VERSION_OPTION");
	private static final String LOG_OPTION = Messages.getMessage("LOG_OPTION");
	private static final String LOG_LEVEL_OPTION = Messages.getMessage("LOG_LEVEL_OPTION");
	private static final String INI_OPTION = Messages.getMessage("INI_OPTION");
	private static final String TEMPDIR_OPTION = Messages.getMessage("TEMPDIR_OPTION");
	private final String RANGE_OPTION = Messages.getMessage("RANGE_OPTION");;
	private final String OUTPUT_OPTION = Messages.getMessage("OUTPUT_OPTION");;
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private String log;
	private String loglevel;
	private String comparisonFile;
	private String inputFile;		
	private String outputDirName;
	private String outputDirNameOverride;
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
	private String blatServer;
	private String blatPort;
	private String bitFile;
	private String blatPath;
	private String mapper;
	private String clipQuery;		
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
	private List<String> gffFiles = new ArrayList<String>();
	private boolean includeTranslocations;
	private boolean allChromosomes = true;
	private int REPEAT_COUNT_CUTOFF = 1000;


	/**
	 * Takes arguments from the command line and sets up the options for qsv 
	 * @param args
	 * @throws QSVException
	 * @throws IOException 
	 * @throws InvalidFileFormatException 
	 */
	public Options(final String[] args) throws QSVException, InvalidFileFormatException, IOException {
		
		//define the options the parse accepts		
		parser.accepts("log", LOG_OPTION).withRequiredArg().ofType(String.class);	
		parser.accepts("loglevel", LOG_LEVEL_OPTION).withRequiredArg().ofType(String.class); 
        parser.accepts("ini", INI_OPTION).withRequiredArg().ofType(String.class).describedAs("ini");
        parser.accepts("tmp", TEMPDIR_OPTION).withRequiredArg().ofType(String.class).describedAs("tmp");  
        parser.accepts("range", RANGE_OPTION).withOptionalArg().ofType(String.class).describedAs("range");    
        parser.accepts("overrideOutput", OUTPUT_OPTION).withOptionalArg().ofType(String.class).describedAs("overrideOutput");
		parser.acceptsAll(asList("h", "help"), HELP_OPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_OPTION);		
		options = parser.parse(args);	
		//logging
		
		iniFile = (String) options.valueOf("ini");
		tempDirName = (String) options.valueOf("tmp");
		
		/*
		 *User can overwrite the output at command line - 
		 */
		if (options.has("overrideOutput")) {
			outputDirNameOverride = (String) options.valueOf("overrideOutput");
		}
		
		if (options.has("range")) {
	    		ranges = new ArrayList<String>();
	    		ranges.add((String) options.valueOf("range"));
	    		processRanges();
	    	}
	}
	
	public String getOverrideOutput() {
		return outputDirNameOverride;
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
		
		if (log == null) {
			log = sampleName + ".log";
		}
		
		if (loglevel == null) {
			loglevel = QSVConstants.DEFAULT_LOGLEVEL;
		}
		
		outputDirName = generalSection.get("output");
		
		if (generalSection.get("min_insert_size") != null) {
			minInsertSize =  new Integer(generalSection.get("min_insert_size"));
		} else {
			minInsertSize = QSVConstants.DEFAULT_MIN_INSERT_SIZE;
		}
		
		if (generalSection.get("repeat_cutoff") != null) {
			REPEAT_COUNT_CUTOFF =  new Integer(generalSection.get("repeat_cutoff"));
		}

		if (ranges == null) {
			ranges = generalSection.getAll("range");			
			processRanges();			
		}
		
		String splitRead = generalSection.get("split_read");
		if (splitRead != null && splitRead.equals("false")) {
			isSplitRead = false;
		}
		
		reference = generalSection.get("reference");
		
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
			} else {
				throw new QSVException("PLATFORM_ERROR", sequencingPlatform);			
			}
		} else {
			throw new QSVException("PLATFORM_ERROR", sequencingPlatform);
		}
		
		maxISizeCount = "all";
		
		if (options.has("qcmg")) {
			isQCMG = true;
		}
		
		if (generalSection.get("qcmg") != null) {
			if (generalSection.get("qcmg").equals("true")) {
				isQCMG = true;
			}
		}
		
		this.gffFiles = generalSection.getAll("gff");
		
		if (gffFiles == null) {
			gffFiles = new ArrayList<String>();
		}				

		//Pairing params
		Section pairSection = ini.get("pair");
		
		if (pairSection != null) {
			pairingType = pairSection.get("pairing_type");
			mapper = pairSection.get("mapper");
			query = pairSection.get("pair_query");
			if (pairSection.get("cluster_size") != null) {
				clusterSize = new Integer(pairSection.get("cluster_size"));					
			}
			if (pairSection.get("filter_size") != null) {
				filterSize = new Integer(pairSection.get("filter_size"));
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
		
		
		//Clippping params
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
			
			if (clipSection.get("blatserver") != null) {
				blatServer = clipSection.get("blatserver");
			}
			if (clipSection.get("blatport") != null) {
				blatPort = clipSection.get("blatport");
			}
			if (clipSection.get("blatpath") != null) {
				blatPath = clipSection.get("blatpath");
			}
		}
		if (analysisMode.equals("pair")) {
			isSplitRead = false;
		}
		
		if (analysisMode.equals("clip")) {
			if (blatServer == null || blatPort == null || blatPath == null) {
				throw new QSVException("NO_BLAT");
			}
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

	public int getREPEAT_COUNT_CUTOFF() {
		return REPEAT_COUNT_CUTOFF;
	}

	public void setREPEAT_COUNT_CUTOFF(int rEPEAT_COUNT_CUTOFF) {
		REPEAT_COUNT_CUTOFF = rEPEAT_COUNT_CUTOFF;
	}

	private void processRanges() throws QSVException {
		includeTranslocations = false;
		allChromosomes = true;
		if (ranges != null && ranges.size() > 0) {
			allChromosomes = false;
			for (String s: ranges) {
				if (s.equals("inter")) {
					includeTranslocations = true;
				}
			}			
			if (ranges.size() > 1 && includeTranslocations) {
				throw new QSVException("RANGE_ERROR");
			}
		}		
	}

	/**
	 * Detects any problems with the supplied options
	 * @throws QSVException
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
			if (!new File(reference).exists()) {
				throw new QSVException("NO_REFERENCE_FILE", reference);	
			}
		} else {
			if (reference == null && isQCMG) {
				throw new QSVException("NULL_REFERENCE_FILE");
			}
		}
		
		if (null == inputFile) {
			throw new QSVException("NULL_INPUT_FILE");				
		} else if ( ! new File(inputFile).exists()) {
			throw new QSVException("NO_INPUT_FILE", inputFile);					
		//check output file directory
		}
		
		if (null == outputDirNameOverride) {
			if (null ==  outputDirName) {  
				throw new QSVException("NULL_OUTPUT_DIR");
			} else if (!directoryExists(outputDirName)) {
				throw new QSVException("NO_OUTPUT_DIR", outputDirName);
			//check output file directory
			}
		}
		
		if (null ==  tempDirName) {
			throw new QSVException("NULL_TEMP_DIR");
		} else if (!directoryExists(tempDirName)) {
			throw new QSVException("NO_TEMP_DIR");		
		//check for name of output files
		} else if (StringUtils.isNullOrEmpty(sampleName)) {
			throw new QSVException("NO_DONOR");
		//check cluster size
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
	 * @throws Exception
	 */
    public void displayHelp() throws Exception {
		parser.printHelpOn(System.err);
	}

	public void setRanges(List<String> ranges) {
		this.ranges = ranges;
	}

	public List<String> getGffFiles() {
		return gffFiles;
	}

	public void setGffFiles(List<String> gffFiles) {
		this.gffFiles = gffFiles;
	}

	public Integer getMinInsertSize() {
		return minInsertSize;
	}

	public void setMinInsertSize(Integer minInsertSize) {
		this.minInsertSize = minInsertSize;
	}

	public boolean isSplitRead() {
		return isSplitRead;
	}

	public void setSplitRead(boolean isSplitRead) {
		this.isSplitRead = isSplitRead;
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

    public String getLoglevel() {
        return loglevel;
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
	
	public String getLog() {
		return log;
	}	

	public Integer getClipSize() {
		return clipSize;
	}

	public void setClipSize(Integer clipSize) {
		this.clipSize = clipSize;
	}

	public Integer getConsensusLength() {
		return consensusLength;
	}

	public void setConsensusLength(Integer consensusLength) {
		this.consensusLength = consensusLength;
	}

	public String getBlatServer() {
		return blatServer;
	}

	public String getBlatPort() {
		return blatPort;
	}

	public String getBitFile() {
		return bitFile;
	}

	public String getBlatPath() {
		return blatPath;
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

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public boolean directoryExists(String directoryName) {
		File file = new File(directoryName);
		if (file.exists() && file.isDirectory()) {
			return true;
		} 
		return false;
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
	
	public void setSingleFileMode(boolean singleFileMode) {
		this.singleFileMode = singleFileMode;
	}

	public boolean isTwoFileMode() {
		return twoFileMode;
	}

	public void setTwoFileMode(boolean twoFileMode) {
		this.twoFileMode = twoFileMode;
	}

	public boolean runClipAnalysis() {
		if (analysisMode.equals("both") || analysisMode.equals("clip")) {
			return true;
		}
		return false;
	}	
	
	public boolean runPairAnalysis() {
		if (analysisMode.equals("both") || analysisMode.equals("pair")) {
			return true;
		}
		return false;
	}
	
	public boolean runPairPreprocess() {
		if (preprocessMode.equals("both") || preprocessMode.equals("pair")) {
			return true;
		}
		return false;
	}
	
	public boolean runClipPreprocess() {
		if (preprocessMode.equals("both") || preprocessMode.equals("clip")) {
			return true;
		}
		return false;
	}

	public String translatePlatform() {
		
		if (platform.equals("solid")) {
			return "4";
		} else if (platform.equals("illumina")) {
			return "60";
		} else {
			return "";
		}
	}
	
	public String getSequencingPlatform() {
		return sequencingPlatform;
	}

	public void setSequencingPlatform(String sequencingPlatform) {
		this.sequencingPlatform = sequencingPlatform;
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


}
