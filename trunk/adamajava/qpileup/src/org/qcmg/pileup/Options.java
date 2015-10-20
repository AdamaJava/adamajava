/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;

import static java.util.Arrays.asList;
import htsjdk.samtools.SamReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;

public final class Options {	
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private String log;
	private String logLevel;
	private String hdfFile;
	private String referenceFile;
	private String outputDir;
	private List<String> bamFiles;
	private String mode;
	private List<String> readRanges = new ArrayList<String>();
	private List<StrandEnum> viewElements = new ArrayList<StrandEnum>();
	private ArrayList<StrandEnum> groupElements = new ArrayList<StrandEnum>();
	private String sampleType;
	private String bamType;
	private String techType;
	private String iniFile;
	private List<String> inputHDFs;
	private boolean getForwardElements = true;
	private boolean getReverseElements = true;
	private boolean bamOverride = false;
	private Integer lowReadCount;
	private Integer percentnonref;
	private static final String VERSION_OPTION = Messages.getMessage("VERSION_OPTION");
	private static final String HELP_OPTION = Messages.getMessage("HELP_OPTION");
	private Integer threadNo;
	private String metricType;
	private String normalFile;
	private String tumourFile;
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
	private File htmlDir;
	private File distributionDir;
	private File wiggleDir;
	private SummaryMetric summaryMetric;
	private File germlineDbFile;
	private String tmpDir;
	private String pathToBigWig;
	private List<String> viewHDFs;
	private String rangeFile;
	private boolean includeGraph;
	private List<String> graphHdfs;
	private Map<String, String> graphRangeInfoMap = new HashMap<String, String>();
	private File snpDir;
	private Map<String, TreeMap<Integer, String>> positionMap;
	private boolean viewGraphStranded;
	private String filter;

	@SuppressWarnings("unchecked")
	public Options(final String[] args) throws Exception {
		
		parser.accepts("ini", Messages.getMessage("INI_OPTION")).withOptionalArg().ofType(String.class);
		parser.acceptsAll(asList("h", "help"), HELP_OPTION);
		parser.acceptsAll(asList("v", "version"), VERSION_OPTION);
		parser.accepts("view", Messages.getMessage("VIEW_OPTION"));
		parser.accepts("H", Messages.getMessage("HEADER_OPTION"));
		parser.accepts("G", Messages.getMessage("GRAPH_OPTION"));
		parser.accepts("V", Messages.getMessage("VERSION_OPTION"));
		parser.accepts("hdf", Messages.getMessage("HDF_FILE_OPTION")).withOptionalArg().ofType(String.class);
		parser.accepts("tmp", Messages.getMessage("TMPDIR_OPTION")).withOptionalArg().ofType(String.class);
		parser.accepts("range", Messages.getMessage("READ_RANGE_OPTION")).withOptionalArg().ofType(String.class);
		parser.accepts("element", Messages.getMessage("ELEMENT_OPTION")).withOptionalArg().ofType(String.class);
		parser.accepts("group", Messages.getMessage("GROUP_OPTION")).withOptionalArg().ofType(String.class);		
		parser.accepts("delete", Messages.getMessage("GROUP_OPTION")).withOptionalArg().ofType(String.class);
		
		options = parser.parse(args);	
		iniFile = (String) options.valueOf("ini");
		tmpDir = (String) options.valueOf("tmp");
		hdfFile = (String)options.valueOf("hdf");
		
		if (hasGraphOption()) {
			viewHDFs = (List<String>) options.valuesOf("hdf");
		}		
		
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
	
	private void getRangesFromRangeFile(String rangeFile, String viewGroup) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(rangeFile));
	
		Iterator<TabbedRecord> iterator = reader.getRecordIterator();
		positionMap = new HashMap<String, TreeMap<Integer, String>>();
		while (iterator.hasNext()) {
			
			TabbedRecord tab = iterator.next();
			
			if (tab.getData().startsWith("#") || tab.getData().startsWith("Hugo") || tab.getData().startsWith("analysis")) {					
				continue;
			}
			String[] data = tab.getData().split("\t");
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
				//graphRangeInfoMap.put(range, "");	
				
			}
		}
		
		for (Entry<String, TreeMap<Integer, String>> e: positionMap.entrySet()) {
			TreeMap<Integer, String> map = e.getValue();
			readRanges.add(e.getKey() + ":" + (map.firstKey()-10) + "-" + (map.lastKey()+10));
		}
		
		reader.close();
	}

	public Map<String, String> getGraphRangeInfoMap() {
		return graphRangeInfoMap;
	}

	public void setGraphRangeInfoMap(Map<String, String> graphRangeInfoMap) {
		this.graphRangeInfoMap = graphRangeInfoMap;
	}

	public void parseIniFile() throws Exception {

		Ini ini = new Ini(new File(iniFile));
		
		Section generalSection = ini.get("general");
		
		log = generalSection.get("log");
		logLevel = generalSection.get("loglevel");
		hdfFile = generalSection.get("hdf");
		String hdfFileName = new File(hdfFile).getName();
		mode = generalSection.get("mode");
		outputDir = generalSection.get("output_dir");		
		
		
		
		if (mode.equals("view") || mode.equals("metrics") || mode.equals("add")) {
			readRanges = generalSection.getAll("range");			
		}
		
		if (readRanges == null || (readRanges != null && readRanges.size() == 0)) {
			readRanges = new ArrayList<String>();
			readRanges.add("all");
		}
		
		if (generalSection.containsKey("thread_no")) {
			threadNo = new Integer(generalSection.get("thread_no"));
		} else {
			threadNo = new Integer(1);
		}
		
		String override =  generalSection.get("bam_override");
		
		if (override != null && override.equals("true")) {
			bamOverride = true;
		}
		
		if (mode.equals("merge")) {
			Section merged = ini.get("merge");		
			this.inputHDFs = merged.getAll("input_hdf");			 
		}
		
		if (mode.equals("bootstrap") || mode.equals("merge")) {		
			Section bootstrap = ini.get("bootstrap");		
			referenceFile = bootstrap.get("reference");
			lowReadCount = new Integer(bootstrap.get("low_read_count"));
			percentnonref = new Integer(bootstrap.get("nonref_percent"));
			
			if (lowReadCount == null) {
				lowReadCount = 10;
			}
			if (percentnonref == null) {
				percentnonref = 20;
			}
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
			parseGroupElements(group);
			
			List<String> elements = viewSection.getAll("element");			
			parseViewElements(elements);
			
			graphHdfs = viewSection.getAll("graph_hdf");		
			
			if (viewSection.containsKey("range_file")) {				
				rangeFile = viewSection.get("range_file");
				readRanges.clear();				
				getRangesFromRangeFile(viewSection.get("range_file"), group);			
			}
			if (viewSection.containsKey("graph")) {
				includeGraph = true;
				if (viewSection.containsKey("stranded")) {
					String type = (String) viewSection.get("stranded");
					if (type.equals("true")) {
						viewGraphStranded = true;
					}
				}
			}			
		}
		
		if (mode.equals("view") || mode.equals("metrics")) {
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
				this.summaryMetric = new SummaryMetric(hdfFileName, pileupDir.getAbsolutePath(), wiggleDir.getAbsolutePath(),distributionDir.getAbsolutePath(), summaryDir.getAbsolutePath(), tmpDir);				
			}
		}
		
		if (mode.equals("metrics")) {
			
			Section metrics = ini.get("metrics");
			Integer minBasesPerPatient = new Integer(metrics.get("min_bases"));
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
							
							if (element.equals(PileupConstants.METRIC_SNP)) {
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
								Metric metric = new SnpMetric(hdfFileName, hdfFile, pileupDir.getAbsolutePath(), snpDir.getAbsolutePath(), snpPercentNonRef, snpNonRefCount, snpHighNonRefCount, dbSNPFile, germlineDbFile,
										comparisonSnpFile, comparisonSnpFileFormat, comparisonSnpFileAnnotation, winCount, tmpDir);
								
								summaryMetric.addMetric(PileupConstants.METRIC_SNP, metric);
								
								
							} else if (element.equals(PileupConstants.METRIC_STRAND_BIAS)) {	
								
								Integer minPercentDiff = new Integer(section.get("min_percent_diff"));								
								Integer minNonReferenceBases = new Integer(section.get("min_nonreference_bases"));
								Metric metric = new StrandBiasMetric(hdfFileName, hdfFile, pileupDir.getAbsolutePath(), minPercentDiff, minBasesPerPatient, minNonReferenceBases);
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

	public boolean isViewGraphStranded() {
		return viewGraphStranded;
	}

	public void setViewGraphStranded(boolean viewGraphStranded) {
		this.viewGraphStranded = viewGraphStranded;
	}

	public boolean hasGraphOption() {
		return options.has("G");
	}

	public File getHtmlDir() {
		return htmlDir;
	}

	public void setHtmlDir(File htmlDir) {
		this.htmlDir = htmlDir;
	}

	public File getDbSNPFile() {
		return dbSNPFile;
	}

	public void setDbSNPFile(File dbSNPFile) {
		this.dbSNPFile = dbSNPFile;
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
		if (group != null) {				
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
		
	}
	
	
	public File getPileupDir() {
		return pileupDir;
	}

	public void setPileupDir(File pileupDir) {
		this.pileupDir = pileupDir;
		pileupDir.mkdir();
	}
	
	public boolean hasDeleteOption() {
		return options.has("delete");
	}

	public boolean hasViewOption() {
		return options.has("view");
	}
	
	public boolean hasHeaderOption() {
		return options.has("H");
	}
	
	public boolean hasHDFVersionOption() {
		return options.has("V");
	}

	public String getLog() {
		return log;
	}

	public String getLogLevel() {
		return logLevel;
	}

	boolean hasVersionOption() {
		return options.has("v") || options.has("version");
	}

	boolean hasLogOption() {
		return options.has("log");
	}

	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}

	void displayHelp() throws Exception {
		parser.printHelpOn(System.err);
	}
	
	boolean hasHelpOption() {
		return options.has("h") || options.has("help");
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
		if (hasViewOption()) {
			if (hdfFile != null) {
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
				
			} else {
				throw new QPileupException("NO_HDF", hdfFile);	
			}
		} else  {
			
			if (log == null) {		
				throw new QPileupException("NO_OPTION", "log");
			} else if (logLevel == null) {
				logLevel = "INFO";			
			} else if (threadNo == null) {
				
			} else if (threadNo > 12) {
				throw new QPileupException("BAD_THREADS");
			}
			
			if (!mode.equals("add") && !mode.equals("bootstrap") && !mode.equals("view") && !mode.equals("remove") && !mode.equals("merge") && !mode.equals("metrics")) {		
				throw new QPileupException("NO_MODE", mode);
			} else if (mode.equals("merge")) {
				if (new File(hdfFile).exists()) {
					//new File(hdfFile).delete();
					throw new QPileupException("EXISTING_HDF", hdfFile);
				}
				if (inputHDFs != null) {
					if (inputHDFs.size() < 1) {
						throw new QPileupException("TOOFEW_HDF", "" + inputHDFs.size());
					} else {
						for (String f : inputHDFs) {							
							if (!new File(f).exists()) {
								throw new QPileupException("NO_HDF", f);
							}
						}
					}					
				} else {
					throw new QPileupException("TOOFEW_HDF", "" + 0);
				}
			} else {	
				if (!mode.equals("bootstrap")) {
					if (!new File(hdfFile).exists()) {
						throw new QPileupException("NO_HDF", hdfFile);
					}
				} else {
					if (new File(hdfFile).exists()) {
						
						throw new QPileupException("EXISTING_HDF", hdfFile);
					}
					if (!new File(referenceFile).exists()) {
						throw new QPileupException("REFERENCE_FILE_ERROR", referenceFile);
					} else {
						if (!new File(referenceFile + ".fai").exists()) {
							throw new QPileupException("FASTA_INDEX_ERROR", referenceFile);
						}
					}
				}
			} 
			
			if (mode.equals("add") || mode.equals("remove")) {
				if (bamFiles == null) {
					throw new QPileupException("NO_OPTION", "bam");
				} else {
					checkBams();					
				}
				
				checkReadRanges();
			} 

			if (mode.equals("view") || mode.equals("metrics")) {
				if (outputDir == null) {
					throw new QPileupException("NO_OPTION", "output");
				} else if (!new File(outputDir).exists()) {
					throw new QPileupException("NO_FILE", outputDir);
				} else {
					checkReadRanges();	
				}
			}			
				
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
	}

	public String getTmpDir() {
		return tmpDir;
	}

	public void setTmpDir(String tmpDir) {
		this.tmpDir = tmpDir;
	}

	private void checkBams() throws QPileupException, IOException {
		for (String bam : bamFiles) {
			//check if the bam exists
			File bamFile = new File(bam);
			if (!bamFile.exists()) {
				throw new QPileupException("NO_FILE", bam);
			//} else if ((System.currentTimeMillis() - bamFile.lastModified()) < 86400000) {
				//file is less than 24 hours since last mod - may be scheduled for post mapping 
				//throw new QPileupException("BAM_NEW", bam);
			} else {
				try {
					String bamCanPath = bamFile.getCanonicalPath();
					File bamLock = new File(bamCanPath + ".lck");
					File bamIndex = new File(bamCanPath + ".bai");					
					
					if (bamLock.exists() && !bamLock.getAbsolutePath().contains("resources/test.bam")) {
						//bam is locked						
						throw new QPileupException("BAM_LOCK", bam);
					} else {
						
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
						
					}				
					
				} catch (Exception e) {
					throw new QPileupException("BAM_OPTIONS_READ_ERROR", bam, PileupUtil.getStrackTrace(e));
				}

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

	public String getTumourFile() {
		return this.tumourFile;
	}
	
	public String getNormalFile() {
		return this.normalFile;
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

	public void setQexec(QExec qexec) {
		this.qexec = qexec;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}	

	public String getComparisonSnpFileAnnotation() {
		return comparisonSnpFileAnnotation;
	}

	public void setComparisonSnpFileAnnotation(String comparisonSnpFileAnnotation) {
		this.comparisonSnpFileAnnotation = comparisonSnpFileAnnotation;
	}

	public SummaryMetric getSummaryMetric() {
		return this.summaryMetric;
	}

	public List<String> getViewHDFs() {
		return viewHDFs;
	}

	public String getRangeFile() {
		return this.rangeFile;
	}

	public List<String> getGraphHDFs() {
		return this.graphHdfs;
	}
	
	public boolean includeViewGraph() {
		return this.includeGraph;
	}

	public Map<String, TreeMap<Integer, String>> getPositionMap() {
		return positionMap;
	}



}
