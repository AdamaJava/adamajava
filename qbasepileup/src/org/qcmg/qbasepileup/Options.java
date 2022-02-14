package org.qcmg.qbasepileup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.qcmg.pileup.Messages;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.model.StrandEnum;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Options {
	
	public final static String INPUT_BAM = "bam";
	public final static String INPUT_HDF = "hdf";
	public final static String INPUT_LIST = "list";
	public final static String PINDEL = "pindel";
	public final static String STRELKA = "strelka";
	public final static String GATK = "gatk";
	
	public final static String COLUMN = "column";
	public final static String ROW = "row";
			
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private final String log;
	private int threadNo = 1;
	private String filterQuery;
	private  List<InputBAM> inputBAMs;
	
	private String mode;
	private File output;
	private File positionsFile;
	private File reference;
	private String format;

	private Integer baseQuality;
	private Integer mappingQuality;
	private boolean strand;
	private boolean novelstarts;
	private boolean intron;
	private boolean indel;
	private File somaticIndelFile;
	private File germlineIndelFile;

	private InputBAM normalBam;
	private InputBAM tumourBam;
	private File germlineOutputFile;
	private File somaticOutputFile;
	private Integer softClipWindow;
	private Integer nearbyIndelWindow;
	private Integer nearbyHomopolymer;
	private Map<String, String[]> pindelMutations;
	private Integer maxCoverage;
	private boolean dup;
	private String indelType;
	private String outputFormat;

	
	public Options(final String[] args) throws Exception {
			
			parser.accepts("help", Messages.getMessage("HELP_OPTION"));
			parser.accepts("version",Messages.getMessage("VERSION_OPTION"));
			parser.accepts("ini", Messages.getMessage("INI_OPTION")).withOptionalArg().ofType(String.class);
			parser.accepts("log", Messages.getMessage("OPTION_LOG")).withRequiredArg().ofType(String.class);	
			parser.accepts("loglevel", Messages.getMessage("OPTION_LOGLEVEL")).withRequiredArg().ofType(String.class);

		
			options = parser.parse(args);					
			log = (String) options.valueOf("log");
//            logLevel = options.has("loglevel")? (String) options.valueOf("loglevel"): "INFO";
            
            if(options.has("ini")) {
	            String iniFile = (String) options.valueOf("ini");
	            Ini ini = new Ini(new File(iniFile));
	            
	    		//[general] common ini options default mode = snp
	            Section section = ini.get("general");
	    		this.mode = section.containsKey("mode")? section.get("mode") : QBasePileupConstants.SNP_MODE;
	    		
	    		//default threadNo = 1
	    		if(section.containsKey("thread_no")) {
	    			this.threadNo = new Integer(section.get("thread_no")); 
	    		}
	    		
	    		//filter = Opt, a qbamfilter query to filter out BAM records. Def=null.
	    		if(section.containsKey("filter")) {
	    			this.filterQuery =  section.get("filter"); 
	    		} else if(mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
	    			this.filterQuery = "and(Flag_DuplicateRead==false , CIGAR_M>34 , MD_mismatch <= 3 , option_SM > 10)";
	    		}

	    		
	    		//mode from general section	    						
	    		if (QBasePileupConstants.SNP_MODE.equals(mode) || mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE) || mode.equals(QBasePileupConstants.SNP_CHECK_MODE)) {	    			
	    			parseIniSNP( ini.get("snps"));
	    		} else if (QBasePileupConstants.INDEL_MODE.equals(mode)) {
	    			parseIniIndel( ini.get("indel"));
	    		} else if (QBasePileupConstants.COVERAGE_MODE.equals(mode)) {
	    			parseIniCoverage(ini.get("coverage"));
	    		} else {
	    			throw new QBasePileupException("MODE_ERROR", mode);
	    		}
            }
	}
	
	public void parseIniCoverage(Section section ) throws Exception {		
		
		//required option
		this.output = new File( section.get("output")); 		
		this.positionsFile = new File(section.get("input-snp"));
		this.format = section.containsKey("input-snp-format")? section.get("input-snp-foramt") : "dcc1";
	
		
 		//get input BAMs
		inputBAMs = new ArrayList<InputBAM>();
		if(section.containsKey("input-bam")) {
			String input = section.get("input-bam");    		 
			InputBAM i = new InputBAM(null, null, new File(input), INPUT_BAM);
			inputBAMs.add(i);
		}
		
		if(section.containsKey("input-bam-list")) {
			getBamList( section.get("input-bam-list"));
		}
 
		if (options.has("maxcov")) {				
			this.maxCoverage = section.containsKey("max_coverage")? Integer.parseInt( section.get("max_coverage")) : null;		
		}
		
		
	}
	
	
	public void parseIniIndel(Section section ) throws Exception {
		this.reference = new File( section.get("reference"));
		checkReference();
		
		//default is null
		this.somaticIndelFile =  section.containsKey("input-somatic")? new File( section.get("input-somatic")) : null;
		this.germlineIndelFile = section.containsKey("section")? new File( section.get("section")) : null;
		
		//it is required option
		this.tumourBam = new InputBAM(null, null, new File((String) options.valueOf("input-tumour-bam")), INPUT_BAM);
		this.normalBam = new InputBAM(null, null, new File(section.get("input-normal-bam")), INPUT_BAM);	
				
		//default null 
		this.somaticOutputFile = section.containsKey("output-somatic")? new File( section.get("output-somatic")) : null;
		this.germlineOutputFile = section.containsKey("output-germline")? new File( section.get("output-germline")) : null;
				 
		//default value based on old qbasepileup indel mode
		this.softClipWindow = section.containsKey("soft_clip_window")? Integer.parseInt( section.get("soft_clip_window")) : 13;		
		this.nearbyHomopolymer = section.containsKey("homoplymer_window")? Integer.parseInt( section.get("homoplymer_window")) : 10;		
		this.nearbyIndelWindow = section.containsKey("indel_window")? Integer.parseInt( section.get("indel_window")) : 3;		
		this.dup = (section.containsKey("include-duplicate") && section.get("include-duplicate").equalsIgnoreCase("true") )? true : false;

		//indel options			
		if (section.containsKey("input_pindel_deletion") || section.containsKey("input_pindel_insertion")) {			
			pindelMutations = new HashMap<String, String[]>();
			if (section.containsKey("input_pindel_deletion")) {
				readPindelMutationFile(pindelMutations, new File(section.get("input_pindel_deletion")), "DEL") ;
			}
			if (section.containsKey("input_pindel_insertion")) {
				readPindelMutationFile(pindelMutations, new File(section.get("input_pindel_insertion")), "INS");
			}
		}	
		
		
		this.indelType = section.containsKey("indel_type") ? section.get("indel_type") : null; 
		
		if ( !indelType.equals(GATK)   && ! indelType.equals(STRELKA)&& ! indelType.equals(PINDEL)) {
			throw new QBasePileupException("INDEL_FILETYPE_ERROR");
		}
		
	}
	
	public void parseIniSNP(Section section ) throws Exception {
		
		//required option
		this.output = new File( section.get("output"));		
		this.positionsFile = new File(section.get("input-snp"));
		this.reference = new File(section.get("reference"));
		checkReference();

		//default is dcc1
		this.format = section.containsKey("input-snp-format")? section.get("input-snp-foramt") : "dcc1";
		if (!format.equals("dcc1") && !format.equals("maf") && !format.equals("tab") &&
				!format.equals("dccq") && !format.equals("vcf") && !format.equals("hdf") && !format.equals("txt")
				&& !format.equals("columns")) {
			throw new QBasePileupException("UNKNOWN_FILE_FORMAT", format);
		}
		
		
		//??? old code was if (outputFormat == 2) { format = "columns"; }
		//so it means input snp file must be columns then output columns as well	
		//need further test
		//this.outputFormat = format.equals("columns")?   2 : 1;		 
		this.outputFormat = section.containsKey("output-format")? section.get("output-foramt") : ROW;
		this.outputFormat = outputFormat.equalsIgnoreCase(COLUMN) ? COLUMN : ROW; 		
		if(outputFormat.equals(COLUMN) && ! format.equals(COLUMN)) {
			throw new QBasePileupException( COLUMN + " output format can only work with column format input, but current input format is " + format );
		}
		
				 
		
		this.baseQuality = section.containsKey("base-qualtiy")? Integer.parseInt( section.get("base-qulity")) : null;
		this.mappingQuality = section.containsKey("mapping-qualtiy")? Integer.parseInt( section.get("mapping-qulity")) : null;
		this.novelstarts =  (section.containsKey("report-novel-start") && section.get("report-novel-start").equalsIgnoreCase("true") ) ? true : false;
		this.strand =  (section.containsKey("seperate-strand") && section.get("seperate-strand").equalsIgnoreCase("false") ) ? false : true;
		this.intron =  (section.containsKey("include-intron") && section.get("include-intron").equalsIgnoreCase("false") ) ? false : true;
		this.indel =  (section.containsKey("include-indel") && section.get("include-indel").equalsIgnoreCase("false") ) ? false : true;
		
		
		
		if (mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE) && ! format.equals("dcc1")) {
			throw new QBasePileupException("COMP_FILE_FORMAT_ERROR");
		}

		if (mode.equals(QBasePileupConstants.SNP_CHECK_MODE) && ! format.equals("maf")) {
			throw new QBasePileupException("CHECK_FILE_FORMAT_ERROR");
		}
		
		//default filter for compound snps:			
		if (null == filterQuery && mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
			filterQuery = "and(Flag_DuplicateRead==false , CIGAR_M>34 , MD_mismatch <= 3 , option_SM > 10)";
		}
		
 		//get input BAMs
		inputBAMs = new ArrayList<InputBAM>();
		if(section.containsKey("input-bam")) {
			String input = section.get("input-bam");    		 
			InputBAM i = new InputBAM(null, null, new File(input), "bam");
			inputBAMs.add(i);
		}
		
		if(section.containsKey("input-bam-list")) {
			getBamList( section.get("input-bam-list"));
		}
 
		if(section.containsKey("input-hdf")) {
			getHDFBamList( section.get("input-hdf"));			 
		}

		
	}
	
	private void checkReference() throws QBasePileupException {
		if ( ! reference.exists()) {
			throw new QBasePileupException("NO_REF_FILE", reference.getAbsolutePath());
		}	
		File indexFile = new File(reference.getAbsolutePath() + ".fai");		

		if ( ! indexFile.exists()) {
			throw new QBasePileupException("FASTA_INDEX_ERROR", reference.getAbsolutePath());
		}
	}
	
	private void readPindelMutationFile(Map<String, String[]> pindelMutations,
			File file, String type) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(file));) {
			String line;
			List<String> lines = new ArrayList<>();
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("####")) {
					if (lines.size() > 0) {
						String[] infos = lines.get(0).split("\t");
						String chr = infos[3].replace("ChrID ", "");
						Integer start = new Integer(infos[4].replace("BP ", ""));
						Integer end = new Integer(infos[5].replace("BP ", ""));
						if ("DEL".equals(type)) {
							start++;
							end--;
						}
						String key = chr + ":" + start + ":" + end + ":" + type;
						String normal = "";
						String tumour = "";
						int normalCount = 0;
						int tumourCount = 0;
						for (int i=2; i<lines.size(); i++) {
							String[] subLines = lines.get(i).split("\t");
							String bam = subLines[subLines.length-2];
							String name = subLines[subLines.length-1].replace("@", "");
							if (bam.contains("Normal")) {
								normal += name + ";";
								normalCount++;
							} else {
								tumour += name + ";";
								tumourCount++;
							}
						}
						String[] out = {tumourCount + ";" + normalCount, tumour, normal};
						pindelMutations.put(key, out);
					}
					lines.clear();
				} else {
					lines.add(line);
				}
			}
		}
	}
	
	 
			
	private void getBamList(String list) throws IOException, QBasePileupException {
		
		String inputType = "list";		
		File bamList = new File (list);
		if (!bamList.exists()) {
			throw new QBasePileupException("BAMFILELIST_ERROR", list);
		}
		
		try (BufferedReader reader = new BufferedReader(new FileReader(bamList));) {
			String line;
			int count = 0;
			while((line = reader.readLine()) != null) {
				if ( ! line.startsWith("#") && ! line.startsWith("ID")) {
					if (line.split("\t").length >= 3) {
						count++;
						String[] values = line.split("\t");
						try {
							InputBAM i = new InputBAM(Integer.valueOf(values[0]), values[1], new File(values[2]), inputType);
							inputBAMs.add(i);
						} catch (NumberFormatException e) {
							reader.close();
							throw new QBasePileupException("INPUT_FORMAT_ERROR", line);
						}				
					} else {					
						InputBAM i = new InputBAM(count, "", new File(line), inputType);
						inputBAMs.add(i);
					}
				}
			}
		}
	}
			
	 
	
	private void getHDFBamList(String fhdf) throws Exception {
		String inputType = "hdf";
		File hdf = new File(fhdf);

		if (!hdf.exists()) {
			throw new QBasePileupException("NO_HDF", hdf.getAbsolutePath());
		}
		
		PileupHDF pileupHDF = new PileupHDF(hdf.getAbsolutePath(), false, false);
		pileupHDF.open();
		List<String> bamFiles = pileupHDF.getBamFilesInHeader();
		int count = 1;
		for (String bam: bamFiles) {
			InputBAM i = new InputBAM(count, "", new File(bam), inputType );
			inputBAMs.add(i);
			count++;
		}
		pileupHDF.close();
	}
	
	public OptionSet getOptions() {
		return options;
	}

	boolean hasHelpOption() {
		return options.has("h") || options.has("help");
	}

	void displayHelp() throws Exception {
		parser.printHelpOn(System.err);
	}

	boolean hasVersionOption() {
		return options.has("v") || options.has("V") || options.has("version");
	}

	boolean hasLogOption() {		
		return options.has("log");
	}

//	//???
//	boolean hasLogLevelOption() {
//		return options.has("loglevel");
//	}
	
	public String getLog() {
		return log;
	}
	
	public String getMode() {
		return mode;
	}
	
	
	public List<InputBAM> getInputBAMs() {
		return inputBAMs;
	}
	
	public File getPositionsFile() {
		return positionsFile;
	}
	
	public String getFormat() {
		return format;
	}
	
	public File getOutput() {
		return output;
	}
	
	public String getFilterQuery() {
		return filterQuery;
	}
	
	public Integer getThreadNo() {
		return threadNo;
	}
	
	public Integer getMaxCoverage() {
		return maxCoverage;
	}
	
	public Integer getBaseQuality() {
		return baseQuality;
	}

	public Integer getMappingQuality() {
		return mappingQuality;
	}
	
	public boolean isStrandSpecific() {
		return strand;
	}

	public boolean isNovelstarts() {
		return novelstarts;
	}
	
	public boolean includeIndel() {
		return indel;
	}

	public boolean includeIntron() {
		return intron;
	}

	
	public boolean includeDuplicates() {
		return dup;
	}
	
	public InputBAM getTumourBam() {
		return tumourBam;
	}

	public InputBAM getNormalBam() {
		return normalBam;
	}
	
	public File getGermlineIndelFile() {
		return germlineIndelFile;
	}

	public File getSomaticOutputFile() {
		return somaticOutputFile;
	}

	public File getGermlineOutputFile() {
		return germlineOutputFile;
	}
	
	public File getSomaticIndelFile() {
		return somaticIndelFile;
	}
	
	public int getNearbyIndelWindow() {
		return nearbyIndelWindow;
	}

	public int getNearbyHomopolymerWindow() {
		return nearbyHomopolymer;
	}

	public int getSoftClipWindow() {
		return softClipWindow;
	}
	
	public String getindelFileType() {
		
		return indelType;
	}

	public File getReference() {
		return reference;
	}
	
	public String getOutputFormat() {
		return outputFormat;
	}

	
}
