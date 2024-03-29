/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import htsjdk.samtools.SamReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.qcmg.common.meta.KeyValue;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.FileUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.pileup.metrics.IndelMetric;
import org.qcmg.pileup.metrics.MappingQualityMetric;
import org.qcmg.pileup.metrics.Metric;
import org.qcmg.pileup.metrics.SnpMetric;
import org.qcmg.pileup.metrics.StrandBiasMetric;
import org.qcmg.pileup.metrics.SummaryMetric;
import org.qcmg.pileup.model.StrandEnum;
import org.qcmg.qio.record.StringFileReader;

public final class Options {	
	private final static int DEFAULT_THREAD = 1;
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private String log;
	private String logLevel;
	private String hdfFile;
	private String referenceFile;
	private String outputDir;
	private List<String> bamFiles;
	private String mode;
	private List<String> readRanges;// = new ArrayList<String>();
	private List<StrandEnum> viewElements = new ArrayList<StrandEnum>();
	private ArrayList<StrandEnum> groupElements = new ArrayList<StrandEnum>();
	private String sampleType;
	private String bamType;	
	private String iniFile;
	private List<String> inputHDFs;
	private boolean getForwardElements = true;
	private boolean getReverseElements = true;
	private boolean bamOverride = false;
	private Integer lowReadCount;
	private Integer percentnonref;
	private Integer threadNo;
	private String metricType;

	private int snpPercentNonRef;
	private File comparisonSnpFile;
	private String comparisonSnpFileFormat;
	private File pileupDir;
	private int snpNonRefCount;
	private int snpHighNonRefCount;
	private QExec qexec;
	private String uuid;
	private File dbSNPFile;
	private String comparisonSnpFileAnnotation;
	private File distributionDir;
	private File wiggleDir;
	private SummaryMetric summaryMetric;
	private File germlineDbFile;
	private String tmpDir;
	private String pathToBigWig;
	private String filter;
	private File snpDir;
	
	@SuppressWarnings("unchecked")
	public Options(final String[] args) throws Exception {
		
		parser.accepts("help", Messages.getMessage("HELP_OPTION"));
		parser.accepts("version",Messages.getMessage("VERSION_OPTION"));
		parser.accepts("ini", Messages.getMessage("INI_OPTION")).withOptionalArg().ofType(String.class);

		parser.accepts("view", Messages.getMessage("VIEW_OPTION"));
 		parser.accepts("hdf", Messages.getMessage("HDF_FILE_OPTION")).withOptionalArg().ofType(String.class);
		parser.accepts("hdf-header", Messages.getMessage("HEADER_OPTION"));
		parser.accepts("hdf-version", Messages.getMessage("HDF_VERSION_OPTION"));		
 		parser.accepts("range", Messages.getMessage("READ_RANGE_OPTION")).withOptionalArg().ofType(String.class);
		parser.accepts("element", Messages.getMessage("ELEMENT_OPTION")).withOptionalArg().ofType(String.class);
		parser.accepts("group", Messages.getMessage("GROUP_OPTION")).withOptionalArg().ofType(String.class);		
		
		options = parser.parse(args);	
		iniFile = (String) options.valueOf("ini");
		hdfFile = (String)options.valueOf("hdf");
		
		if (options.has("range")) {
			readRanges = new ArrayList<String>();
			readRanges.add((String) options.valueOf("range"));
		}
		
		if (options.has("group")) {
			String group = (String) options.valueOf("group");
			parseGroupElements(group);
		}
		
		if (options.has("element")) {
			List<String> elements = (List<String>) options.valuesOf("element");			
			parseViewElements(elements);
		}
	}

	public void parseIniFile() throws Exception {

		Ini ini = new Ini(new File(iniFile));
		
		//[general] common ini options
		Section generalSection = ini.get("general");		
		this.log = generalSection.get("log");
		this.logLevel = generalSection.containsKey("loglevel")?  generalSection.get("loglevel") : "INFO";
		
		this.hdfFile = generalSection.get("hdf");
		this.mode = generalSection.get("mode");
		this.threadNo = generalSection.containsKey("thread_no")? 
				new Integer(generalSection.get("thread_no")) : DEFAULT_THREAD; 
		
		//[general] "bam_override" for add & remove mode
		if ((mode.equals("remove") || mode.equals("add") || mode.equals("merge")) && 
				generalSection.containsKey("bam_override") && generalSection.get("bam_override").equals("true")) {
			this.bamOverride = true;
		} 
		
		//bam_override is for add/remove and merge, but default is false
		if(generalSection.containsKey("bam_override") && generalSection.get("bam_override").equals("true")) {
			bamOverride = true;
		}
		
		readRanges = new ArrayList<String>(); //in case parseIniFile() execute multi time
		if (mode.equals("view") || mode.equals("metrics") || mode.equals("add") || mode.equals("remove")) {
			if(generalSection.containsKey("range")) {
				readRanges = generalSection.getAll("range");
			} else {
				readRanges = Arrays.asList(new String[] {"all"});
			}
			//range will be check in detectBadOptions
		} 
				

		if (mode.equals("merge")) {
			Section merged = ini.get("merge");		
			this.inputHDFs = merged.getAll("input_hdf");			 
		}
		
		if (mode.equals("bootstrap") || mode.equals("merge")) {		
			Section bootstrap = ini.get("bootstrap");	
			if(bootstrap == null) throw new QPileupException("NO_MODE", "bootstrap");
			referenceFile = bootstrap.get("reference");			
			lowReadCount = bootstrap.containsKey("low_read_count")? new Integer(bootstrap.get("low_read_count")) : 10;					
			percentnonref =	bootstrap.containsKey("nonref_percent")? new Integer(bootstrap.get("nonref_percent")) : 20;							
		}	
			
		if (mode.equals("add") || mode.equals("remove")) {
			Section bamSection = ini.get("add_remove");			
			filter = bamSection.get("filter");
			if (bamSection.containsKey("bam_list")) {
				String bamlist = bamSection.get("bam_list");
				if (!new File(bamlist).exists()) {
					throw new QPileupException("NO_BAM_FILE_LIST", bamlist);
				}
				bamFiles = readBamFileList(bamlist);
				
			} else {
				bamFiles = bamSection.getAll("name");
			}			
		}		
		
		if (mode.equals("view")) {
			Section viewSection = ini.get("view");
			String group = viewSection.get("group");			
			parseGroupElements(group); //only allow one group
			
			List<String> elements = viewSection.getAll("element");			
			parseViewElements(elements); //allow multi element
					
//		/////below code only used by deprecated ViewMT2
//			graphHdfs = viewSection.getAll("graph_hdf");		
//		
//			if (viewSection.containsKey("range_file")) {				
//				rangeFile = viewSection.get("range_file");
//				readRanges.clear();				
//				getRangesFromRangeFile(viewSection.get("range_file"), group);			
//			}
//			
//			if (viewSection.containsKey("graph")) {
//				includeGraph = true;
//				if (viewSection.containsKey("stranded")) {
//					String type = (String) viewSection.get("stranded");
//					if (type.equals("true")) {
//						viewGraphStranded = true;
//					}
//				}
//			}
//		/////above code only used by deprecated ViewMT2
			
		}
		
		//[general] output_dir
		if (mode.equals("view") || mode.equals("metrics")) {
			//compulsary for view and metrics, will be checked on detectBadOptions
			outputDir = generalSection.get("output_dir");	
			
			String pileup = "qpileup_" + PileupUtil.getCurrentDateTime();
			this.pileupDir = new File(outputDir + PileupConstants.FILE_SEPARATOR + pileup);
			pileupDir.mkdir();			
			
			if (includeGraph) {
				this.htmlDir = new File(outputDir + PileupConstants.FILE_SEPARATOR + pileup + PileupConstants.FILE_SEPARATOR + "html");
				htmlDir.mkdir();
			}			
			
			if (mode.equals("metrics")) {				
				this.distributionDir = new File(outputDir + PileupConstants.FILE_SEPARATOR + pileup + PileupConstants.FILE_SEPARATOR + "distribution");
				distributionDir.mkdir();
				this.wiggleDir = new File(outputDir + PileupConstants.FILE_SEPARATOR + pileup + PileupConstants.FILE_SEPARATOR + "wiggle");
				wiggleDir.mkdir();
				File summaryDir = new File(outputDir + PileupConstants.FILE_SEPARATOR + pileup + PileupConstants.FILE_SEPARATOR + "summary");
				summaryDir.mkdir();		
				this.snpDir =  new File(outputDir + PileupConstants.FILE_SEPARATOR + pileup + PileupConstants.FILE_SEPARATOR + "snp");
				snpDir.mkdir();
				this.summaryMetric = new SummaryMetric(new File(hdfFile).getName(), pileupDir.getAbsolutePath(), wiggleDir.getAbsolutePath(),distributionDir.getAbsolutePath(), summaryDir.getAbsolutePath(), tmpDir);				
			}
		}
		
		if (mode.equals("metrics")) {
			
			Section metrics = ini.get("metrics");
			Integer minBasesPerPatient = new Integer(metrics.get("min_bases"));
			this.tmpDir = metrics.get("temporary_dir");
			summaryMetric.setMinBasesPerPatient(minBasesPerPatient);
						
			if (metrics.containsKey("bigwig_path") && metrics.containsKey("chrom_sizes")) {
				pathToBigWig = (String) metrics.get("bigwig_path");
				String chromSizes = (String) metrics.get("chrom_sizes");
				summaryMetric.setPathToBigWig(pathToBigWig);
				summaryMetric.setChromSizes(chromSizes);
			}
						
			Set<Entry<String, Section>> sectionList = ini.entrySet();
			for (Entry<String, Section> s : sectionList) {
				Section parentSection = s.getValue();				
									
					if (parentSection.getName().equals("metrics")) {						
						
						String[] children = parentSection.childrenNames();	
							
						for (String child: children) {
							
							Section section = parentSection.getChild(child);							
							String element = section.getSimpleName();
							
							
							if (element.equals(PileupConstants.METRIC_SNP)) { //parse [metrics/snp]	
								if (section.containsKey("nonref_percent")) {
									snpPercentNonRef = new Integer(section.get("nonref_percent"));
								} else {
									snpPercentNonRef = 20;
								}
								
								if (section.containsKey("nonref_count")) {
									snpNonRefCount = new Integer(section.get("nonref_count"));
								} else {
									snpNonRefCount = 10;
								}
								
								if (section.containsKey("high_nonref_count")) {
									snpHighNonRefCount = new Integer(section.get("high_nonref_count"));
								} else {
									snpHighNonRefCount = 0;
								}
								
								if (section.containsKey("snp_file")) {
									comparisonSnpFile = new File(section.get("snp_file"));
									comparisonSnpFileFormat = section.get("snp_file_format");
									comparisonSnpFileAnnotation = section.get("snp_file_annotation");
								}
								
								if (section.containsKey("dbSNP")) {
									dbSNPFile = new File (section.get("dbSNP"));					
								}
								
								if (section.containsKey("germlineDB")) {
									germlineDbFile = new File (section.get("germlineDB"));									
								}

								Integer winCount = new Integer(section.get("window_count"));
								Metric metric = new SnpMetric(new File(hdfFile).getName(), hdfFile, pileupDir.getAbsolutePath(), snpDir.getAbsolutePath(), snpPercentNonRef, snpNonRefCount, snpHighNonRefCount, dbSNPFile, germlineDbFile,
										comparisonSnpFile, comparisonSnpFileFormat, comparisonSnpFileAnnotation, winCount, tmpDir);
								
								summaryMetric.addMetric(PileupConstants.METRIC_SNP, metric);
								
							} else if (element.equals(PileupConstants.METRIC_STRAND_BIAS)) { //parse [metrics/strand_bias]	
								
								Integer minPercentDiff = new Integer(section.get("min_percent_diff"));								
								Integer minNonReferenceBases = new Integer(section.get("min_nonreference_bases"));
								Metric metric = new StrandBiasMetric(new File(hdfFile).getName(), hdfFile, pileupDir.getAbsolutePath(), minPercentDiff, minBasesPerPatient, minNonReferenceBases);
								summaryMetric.addMetric(PileupConstants.METRIC_STRAND_BIAS, metric);
								
							} else {								
									Double posvalue = new Double(section.get("position_value"));	
									Integer winCount = new Integer(section.get("window_count"));
									
									if (element.equals(PileupConstants.METRIC_INDEL)) {								
										Metric metric = new IndelMetric(posvalue, winCount, minBasesPerPatient);										
										summaryMetric.addMetric(PileupConstants.METRIC_INDEL, metric);
										
									}  else if (element.equals(PileupConstants.METRIC_MAPPING)) {									
											MappingQualityMetric metric = new MappingQualityMetric(posvalue, winCount, minBasesPerPatient);
											summaryMetric.addMetric(PileupConstants.METRIC_MAPPING, metric);
									} else {										
										summaryMetric.addMetric(element, posvalue, winCount, minBasesPerPatient);										 
									}
							}
					}
				}
			}			
		}
		
		if (summaryMetric != null) {
			summaryMetric.checkMetrics();
		}

		detectBadOptions();		
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}


	private List<String> readBamFileList(String bamlist) throws IOException {
		List<String> files = new ArrayList<String>();
		
		BufferedReader reader = new BufferedReader(new FileReader(new File(bamlist)));
		String line;
		while ((line = reader.readLine()) != null) {
			files.add(line);
		}
		
		reader.close();
		return files;		
	}

	private void parseViewElements(List<String> elements) throws QPileupException {
		StrandEnum[] enums = StrandEnum.values();
		
		
		if (elements != null) {				
			for (String element : elements) {	
				StrandEnum currentEnum = null;
				for (StrandEnum e : enums) {						
					if (e.getStrandEnum().equals(element)) {
						currentEnum = e;
					}						
				}
				if (currentEnum != null) {
					viewElements.add(currentEnum);
				} else {
					throw new QPileupException("BAD_ELEMENT", element);
				}
			}
		}
	}

	private void parseGroupElements(String group) throws QPileupException {
		if (group == null) return;
				 			
		if (group.equals("forward")) {
			getReverseElements = false;
			groupElements.addAll(Arrays.asList(StrandEnum.values()));
		} else if (group.equals("reverse")) {
			getForwardElements = false;
			groupElements.addAll(Arrays.asList(StrandEnum.values()));
		} else if (group.equals("bases")) {
			groupElements.addAll(StrandEnum.getBases());
		} else if (group.equals("quals")) {
			groupElements.addAll(StrandEnum.getQuals());
		} else if (group.equals("cigars")) {
			groupElements.addAll(StrandEnum.getCigars());
		} else if (group.equals("readStats")) {
			groupElements.addAll(StrandEnum.getReadStats());
		} else if (group.equals("metrics")) {
			groupElements.addAll(StrandEnum.getMetrics());
		} else {
			throw new QPileupException("GROUP_ERROR", group);
		}	
	}
	
	
	public File getPileupDir() {
		return pileupDir;
	}

	public void setPileupDir(File pileupDir) {
		this.pileupDir = pileupDir;
		pileupDir.mkdir();
	}
	


	public boolean hasViewOption() {
		return options.has("view");
	}
	
	public boolean hasHeaderOption() {
		return options.has("hdf-header");
	}
	
	public boolean hasHDFVersionOption() {
		return options.has("hdf-version");
	}

	public String getLog() {
		return log;
	}

	public String getLogLevel() {
		return logLevel;
	}

	boolean hasVersionOption() {
		return options.has("version");
	}

	boolean hasLogOption() {
		return options.has("log");
	}

	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}

	void displayHelp() throws Exception {
		parser.formatHelpWith(new BuiltinHelpFormatter(160, 2));
        parser.printHelpOn(System.err);
	}
	
	boolean hasHelpOption() {
		return  options.has("help");
	}
	
	public boolean hasNonOptions() {
		return 0 != options.nonOptionArguments().size();
	}
	
	public int getSnpPercentNonRef() {
		return snpPercentNonRef;
	}

	public void setSnpPercentNonRef(int snpPercentNonRef) {
		this.snpPercentNonRef = snpPercentNonRef;
	}
	
	public int getSnpNonRefCount() {
		return snpNonRefCount;
	}

	public void setSnpNonRefCount(int snpNonRefCount) {
		this.snpNonRefCount = snpNonRefCount;
	}

	public int getSnpHighNonRefCount() {
		return snpHighNonRefCount;
	}

	public void setSnpHighNonRefCount(int snpHighNonRefCount) {
		this.snpHighNonRefCount = snpHighNonRefCount;
	}

	public void detectBadOptions() throws Exception {
		
		//usage2: qpileup --view 
		if (hasViewOption()) {
			if (iniFile != null) throw new QPileupException("INI_VIEW_OPTIONE_ERROR", "view");
				
			if (hdfFile == null) throw new QPileupException("NO_HDF", hdfFile);	
					 
			if (!new File(hdfFile).exists()) {
				throw new QPileupException("NO_HDF", hdfFile);	
			}								 
			if (readRanges != null && readRanges.size() != 0) {
				if ((readRanges.size() > 1)) {
					throw new QPileupException("MT_READ_EXCEPTION");
				}	
				if (readRanges.get(0).equals("all")) {
					throw new QPileupException("MT_READ_EXCEPTION");
				}
			}
							
			return;
		}
				
		//usage1: qpileup --ini	
		
		//not allow other options with --ini together
		for (String opt : new String[] {"hdf", "range", "group", "element"}) {
			if (options.has(opt)) {
				throw new QPileupException("INI_VIEW_OPTIONE_ERROR", opt);
			}
		}
		
		if (log == null) {		
			throw new QPileupException("NO_OPTION", "log");
		} 
				
//		if (threadNo > 12) {
//			throw new QPileupException("BAD_THREADS");
//		}
			
		//check mode				
		if (!mode.equals("add") && !mode.equals("bootstrap") && !mode.equals("view") && !mode.equals("remove") && !mode.equals("merge") && !mode.equals("metrics")) {		
			throw new QPileupException("UNKNOWN_MODE", mode);
		} 
		
		//check hdf
		if (mode.equals("merge") || mode.equals("bootstrap")) {
			//hdf is the output, can't be pre-exsiting
			 if (new File(hdfFile).exists()) throw new QPileupException("EXISTING_HDF", hdfFile) ;			 			
		} else {
			//hdf is the input must pre-exsiting
			if (!new File(hdfFile).exists()) throw new QPileupException("NO_HDF", hdfFile);			 
		}	
		
		//check input_hdf for [merge]
		if (mode.equals("merge")) {		
			if (inputHDFs == null) throw new QPileupException("TOOFEW_HDF", "" + 0);	
			if (inputHDFs.size() < 1) throw new QPileupException("TOOFEW_HDF", "" + inputHDFs.size());
			  						 
			for (String f : inputHDFs) {							
				if (!new File(f).exists()) throw new QPileupException("NO_HDF", f);				
			}
		} 	
		
		//check reference for [bootstrap]		 
		if ( mode.equals("bootstrap")) {						
			if (!new File(referenceFile).exists()) {
				throw new QPileupException("REFERENCE_FILE_ERROR", referenceFile);
			} 
			 
			if (!new File(referenceFile + ".fai").exists()) {
				throw new QPileupException("FASTA_INDEX_ERROR", referenceFile);
			}				 
		} 
		
		//check name or bamlist for [add_remove]
		if (mode.equals("add") || mode.equals("remove")) {
			if (bamFiles == null)  throw new QPileupException("NO_OPTION", "bam");
			checkBams();			 
		} 

		//check output_dir under [general] for view and metrics
		if (mode.equals("view") || mode.equals("metrics")) {	
			if (outputDir == null) throw new QPileupException("NO_OPTION", "output_dir");
			  
			if (!new File(outputDir).exists()) throw new QPileupException("NO_FILE", outputDir);						 
		}	
		
		//check range under [general] for add, remove, view and metrics
		if (mode.equals("view") || mode.equals("metrics") || mode.equals("add") || mode.equals("remove")) {
			checkReadRanges();		
		}
		
		//check 
			if (mode.equals("metrics")) {
				if (tmpDir == null) {
					throw new QPileupException("NO_TMPDIR");
				} 
				if (!FileUtils.canFileBeRead(tmpDir)) {
					throw new QPileupException("NO_READ_TMP", tmpDir);
				}
				if (dbSNPFile != null) {
					if (!dbSNPFile.exists()) {
						throw new QPileupException("NO_DBSNP_FILE", dbSNPFile.getAbsolutePath());
					}
					if (!dbSNPFile.getName().endsWith(".vcf")) {
						throw new QPileupException("NO_DBSNP_VCF", dbSNPFile.getAbsolutePath());
					}
				}
				if (comparisonSnpFile != null) {
					if (!comparisonSnpFile.exists()) {
						throw new QPileupException("NO_SNP_FILE", comparisonSnpFile.getAbsolutePath());
					}
				}
			}
	}

	private void checkBams() throws QPileupException, IOException {
		for (String bam : bamFiles) {
			//check if the bam exists
			File bamFile = new File(bam);
			if (!bamFile.exists()) {
				throw new QPileupException("NO_FILE", bam);
			}  
			try {
				String bamCanPath = bamFile.getCanonicalPath();
				File bamLock = new File(bamCanPath + ".lck");
				File bamIndex = new File(bamCanPath + ".bai");					
				
				if (bamLock.exists() && !bamLock.getAbsolutePath().contains("resources/test.bam")) {
					//bam is locked						
					throw new QPileupException("BAM_LOCK", bam);
				}  
					
				SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile, "silent");			
				File indexLock = new File(bam + ".bai.lck");
				//does bam have index
				if (!reader.hasIndex()) {
					reader.close();
					throw new QPileupException("NO_INDEX", bam);
				} 
				
				if (bamIndex.lastModified() < bamFile.lastModified()) {
					reader.close();
					throw new QPileupException("INDEX_OLD", bam);
				}
				
				//is index locked						
				if (indexLock.exists() && !indexLock.getAbsolutePath().contains("resources/test.bam.bai")) {
					reader.close();
					throw new QPileupException("INDEX_LOCK", bam);
				}
							
				reader.close();
				
				//the file is free to use, so create a lock file for it. 								
				
			} catch (Exception e) {
				throw new QPileupException("BAM_OPTIONS_READ_ERROR", bam, PileupUtil.getStrackTrace(e));
			}			
		}
		
	}

	private void checkReadRanges() throws QPileupException {
		for (String readRange : readRanges) {
			Pattern subPattern = Pattern.compile("[\\w|.]+:[0-9]+-[0-9]+");
			Pattern pattern = Pattern.compile("[\\w|.]+");
			Matcher matcher1 = subPattern.matcher(readRange);
			Matcher matcher2 = pattern.matcher(readRange);
			if (!readRange.equals("all")) {
				if (!matcher1.matches() && !matcher2.matches()) {
					throw new QPileupException("NO_READ_RANGE", readRange);
				}
			}
		}		
	}
	
	public String getTmpDir() {
		return tmpDir;
	}

	public void setTmpDir(String tmpDir) {
		this.tmpDir = tmpDir;
	}

	public String getHdfFile() {
		return hdfFile;
	}

	public List<String> getInputHDFFiles() {
		return this.inputHDFs;
	}
	
	public String getReferenceFile() {
		return referenceFile;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public List<String> getBamFiles() {
		return bamFiles;
	}

	public String getMode() {
		return mode;
	}

	public List<String> getReadRanges() {
		return readRanges;
	}

	public String getBamType() {
		return this.bamType;
	}

	public String getSampleType() {
		return sampleType;
	}
	
	public String getTechType() {
		return techType;
	}

	public List<StrandEnum> getViewElements() {
		return this.viewElements;
	}

	public List<StrandEnum> getGroupElements() {
		return this.groupElements;
	}

	public boolean getReverseElements() {
		return this.getReverseElements;
	}

	public boolean getForwardElements() {
		return this.getForwardElements;
	}	
	
	public boolean isBamOverride() {
		return bamOverride;
	}

	public Integer getPercentNonRef() {
		return this.percentnonref;
	}

	public Integer getLowReadCount() {
		return this.lowReadCount;
	}

	public int getThreadNo() {
		return this.threadNo;
	}
	
	public File getComparisonSnpFile() {
		return comparisonSnpFile;
	}

	public void setComparisonSnpFile(File comparisonSnpFile) {
		this.comparisonSnpFile = comparisonSnpFile;
	}

	public String getComparisonSnpFileFormat() {
		return comparisonSnpFileFormat;
	}

	public void setComparisonSnpFileFormat(String comparisonSnpFileFormat) {
		this.comparisonSnpFileFormat = comparisonSnpFileFormat;
	}

	public String getMetricType() {
		return this.metricType;
	}

	public void setQExec(QExec exec) {
		this.qexec = exec;
		setUuid(qexec.getUuid());
	}

	private void setUuid(KeyValue uuid) {
		this.uuid = uuid.getValue();		
	}

	public QExec getQexec() {
		return qexec;
	}

	public String getUuid() {
		return uuid;
	}

	public SummaryMetric getSummaryMetric() {
		return this.summaryMetric;
	}

	
	@Deprecated private String techType;
	@Deprecated private String tumourFile;
	@Deprecated private String normalFile;	
	@Deprecated private List<String> viewHDFs;
	@Deprecated private Map<String, String> graphRangeInfoMap = new HashMap<String, String>();	
	@Deprecated private boolean viewGraphStranded;	
	@Deprecated private File htmlDir;	
	@Deprecated private boolean includeGraph;
	@Deprecated private List<String> graphHdfs;
	@Deprecated private String rangeFile;
	@Deprecated private Map<String, TreeMap<Integer, String>> positionMap;

	
	
	@Deprecated //not used anywhere
	public String getTumourFile() {
		return this.tumourFile;
	}
	
	@Deprecated //not used anywhere	
	public String getNormalFile() {
		return this.normalFile;
	}
	
	@Deprecated //not used anywhere
	public void setQexec(QExec qexec) {
		this.qexec = qexec;
	}
	@Deprecated //not used anywhere
	public String getComparisonSnpFileAnnotation() {
		return comparisonSnpFileAnnotation;
	}
	
	@Deprecated //not used anywhere
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	@Deprecated //not used anywhere
	public void setComparisonSnpFileAnnotation(String comparisonSnpFileAnnotation) {
		this.comparisonSnpFileAnnotation = comparisonSnpFileAnnotation;
	}

	@Deprecated //not used anywhere
	public File getDbSNPFile() {
		return dbSNPFile;
	}
	
	@Deprecated //not used anywhere
	public void setDbSNPFile(File dbSNPFile) {
		this.dbSNPFile = dbSNPFile;
	}
	
	@Deprecated //not used anywhere
	public List<String> getViewHDFs() {
		return viewHDFs;
	}
	
	@Deprecated //not used anywhere
	public String getRangeFile() {
		return this.rangeFile;
	}

	@Deprecated //used by viewMT2
	public List<String> getGraphHDFs() {
		return this.graphHdfs;
	}
	
	@Deprecated //used by viewMT2
	public boolean includeViewGraph() {
		return this.includeGraph;
	}
	
	@Deprecated //used by viewMT2
	public boolean isViewGraphStranded() {
		return viewGraphStranded;
	}
	@Deprecated //used by viewMT2
	public void setViewGraphStranded(boolean viewGraphStranded) {
		this.viewGraphStranded = viewGraphStranded;
	}

	@Deprecated //not used anywhere
	public boolean hasGraphOption() {
		return options.has("G");
	}
	@Deprecated //used by viewMT2
	public File getHtmlDir() {
		return htmlDir;
	}
	@Deprecated //used by viewMT2
	public void setHtmlDir(File htmlDir) {
		this.htmlDir = htmlDir;
	}
	
	@Deprecated //used by viewMT2
	public Map<String, String> getGraphRangeInfoMap() {
		return graphRangeInfoMap;
	}

	@Deprecated //used by viewMT2
	public void setGraphRangeInfoMap(Map<String, String> graphRangeInfoMap) {
		this.graphRangeInfoMap = graphRangeInfoMap;
	}	

	@Deprecated
	public boolean hasDeleteOption() {
		return options.has("delete");
	}
	
	@Deprecated //used by viewMT2
	public Map<String, TreeMap<Integer, String>> getPositionMap() {
		return positionMap;
	}
	
	
	@Deprecated //work for positionMap which used by viewMT2
	private void getRangesFromRangeFile(String rangeFile, String viewGroup) throws Exception {
		
		try (StringFileReader reader = new StringFileReader(new File(rangeFile));) {
			positionMap = new HashMap<String, TreeMap<Integer, String>>();
		
			for (final String tab : reader) {			
				if (tab.startsWith("#") || tab.startsWith("Hugo") || tab.startsWith("analysis")) {					
					continue;
				}
				String[] data = tab.split("\t");
				if (rangeFile.endsWith("gff3")) {				
					String range = data[0] + ":" + data[3] + "-" + data[4];
					
					graphRangeInfoMap.put(range, data[8]);			
					readRanges.add(range);
				} else {
					String range = data[0] + "\t" + data[1] + "\t" + data[1];
					if (data[0].equals("chrXY")) {
						range = "chrX" + ":" + data[1] + "-" + data[1];
					}
					int pos = new Integer(data[1]);
					if (positionMap.containsKey(data[0])) {
						positionMap.get(data[0]).put(pos, range);
					} else {
						TreeMap<Integer, String> treemap = new TreeMap<Integer, String>();
						treemap.put(pos, range);
						positionMap.put(data[0], treemap);
						
					}					
				}
			}
		}
		for (Entry<String, TreeMap<Integer, String>> e: positionMap.entrySet()) {
			TreeMap<Integer, String> map = e.getValue();
			readRanges.add(e.getKey() + ":" + (map.firstKey()-10) + "-" + (map.lastKey()+10));
		}
	}	
	

	//do not use it, testing only
	 String getIniFile() {
		return iniFile;
	}

}
