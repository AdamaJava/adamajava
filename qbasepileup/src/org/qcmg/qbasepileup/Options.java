/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.qcmg.pileup.hdf.PileupHDF;
public class Options {

	private static final String HELP_OPTION = Messages.getMessage("OPTION_HELP");	
	private static final String VERSION_OPTION = Messages.getMessage("OPTION_VERSION");
	
	private final OptionParser parser = new OptionParser();
	private final OptionSet options;
	private File output;
	private final String log;
	private File positionsFile;
	private String profile;
	private String format;
	private File reference;
	private Integer baseQuality;
	private Integer mappingQuality;
	private boolean strand = true;
	private boolean novelstarts = false;
	private boolean intron = true;
	private boolean indel = true;
	private Integer threadNo = 1;
	private final List<InputBAM> inputBAMs = new ArrayList<InputBAM>();
	private File hdf;
	private String inputType;
	public final static String INPUT_BAM = "bam";
	public final static String INPUT_HDF = "hdf";
	public final static String INPUT_LIST = "list";
	public int nearbyIndelWindow = 3;
	public int nearbyHomopolymer = 10;
	public int softClipWindow = 13;
	private InputBAM tumourBam;
	private InputBAM normalBam;
	private String mode= new String("snp");
	private File germlineIndelFile;
	private File somaticOutputFile;
	private File germlineOutputFile;
	private File somaticIndelFile;
	private String filterQuery;
	//private Integer minCoverage;
	private Integer maxCoverage;
	private Map<String, String[]> pindelMutations;
	private Integer outputFormat = 1;

	public Options(final String[] args) throws Exception {

		parser.accepts("log", Messages.getMessage("OPTION_LOG")).withRequiredArg().ofType(String.class);	
		parser.accepts("loglevel", Messages.getMessage("OPTION_LOGLEVEL")).withRequiredArg().ofType(String.class);
		parser.accepts("f", Messages.getMessage("OPTION_FORMAT")).withOptionalArg().ofType(String.class).describedAs("format_of_snp_file");
		parser.accepts("s", Messages.getMessage("OPTION_SNP")).withRequiredArg().ofType(String.class).describedAs("snps_file");
		parser.accepts("m", Messages.getMessage("OPTION_MODE")).withRequiredArg().ofType(String.class).describedAs("snp_or_indel");	
		parser.accepts("r", Messages.getMessage("OPTION_REFERENCE")).withRequiredArg().ofType(String.class).describedAs("reference.fa");
		parser.accepts("filter", Messages.getMessage("OPTION_FILTER")).withRequiredArg().ofType(String.class).describedAs("qbamfilter");
		parser.accepts("dup", Messages.getMessage("OPTION_DUPS"));
		//snp mode options
		parser.accepts("i", Messages.getMessage("OPTION_INPUT")).withRequiredArg().ofType(String.class).describedAs("input_bam");		
		parser.accepts("b", Messages.getMessage("OPTION_BAMLIST")).withRequiredArg().ofType(String.class).describedAs("bam_list_file");		
		parser.accepts("o", Messages.getMessage("OPTION_OUTPUT")).withRequiredArg().ofType(String.class).describedAs("output");	
		parser.accepts("of", Messages.getMessage("OPTION_OUTPUT_FORMAT")).withRequiredArg().ofType(String.class).describedAs("output_format");	
		parser.accepts("p", Messages.getMessage("OPTION_PROFILE")).withOptionalArg().ofType(String.class).describedAs("pileup_profile");		
		parser.accepts("t", Messages.getMessage("OPTION_THREADS")).withOptionalArg().ofType(Integer.class).describedAs("threadNo");
		parser.accepts("bq", Messages.getMessage("OPTION_QUALITY")).withRequiredArg().ofType(Integer.class).describedAs("base_qual");		
		parser.accepts("mq", Messages.getMessage("OPTION_MAPPING_QUALITY")).withRequiredArg().ofType(Integer.class).describedAs("mapping_qual");		
		parser.accepts("intron", Messages.getMessage("OPTION_INTRON")).withRequiredArg().ofType(String.class).describedAs("include_introns");
		parser.accepts("ind", Messages.getMessage("OPTION_INDEL")).withRequiredArg().ofType(String.class).describedAs("include_indels");
		parser.accepts("strand", Messages.getMessage("OPTION_STRAND")).withRequiredArg().ofType(String.class).describedAs("strand");
		parser.accepts("novelstarts", Messages.getMessage("OPTION_NOVELSTARTS")).withRequiredArg().ofType(String.class).describedAs("novelstarts");
		parser.accepts("hdf", Messages.getMessage("OPTION_HDF")).withRequiredArg().ofType(String.class).describedAs("hdf");

		//indel
		parser.accepts("is", Messages.getMessage("OPTION_SOMATIC_INPUT")).withRequiredArg().ofType(String.class).describedAs("somatic_indel_file");
		parser.accepts("ig", Messages.getMessage("OPTION_GERMLINE_INPUT")).withRequiredArg().ofType(String.class).describedAs("germline_file");
		parser.accepts("o", Messages.getMessage("OPTION_OUTPUT")).withRequiredArg().ofType(String.class).describedAs("pileup_output");
		parser.accepts("os", Messages.getMessage("OPTION_OUTPUT_SOMATIC")).withRequiredArg().ofType(String.class).describedAs("somatic_output");
		parser.accepts("og", Messages.getMessage("OPTION_OUTPUT_GERMLINE")).withRequiredArg().ofType(String.class).describedAs("germline_output");
		parser.accepts("sc", Messages.getMessage("OPTION_SOFTCLIP")).withRequiredArg().ofType(Integer.class).describedAs("soft_clip_window");
		parser.accepts("hp", Messages.getMessage("OPTION_WINDOW")).withRequiredArg().ofType(Integer.class).describedAs("homopolymer_window");
		parser.accepts("n", Messages.getMessage("OPTION_NEAR_INDELS")).withRequiredArg().ofType(Integer.class).describedAs("nearby_indel_bases");
		parser.accepts("pindel", Messages.getMessage("OPTION_PINDEL"));
		parser.accepts("strelka", Messages.getMessage("OPTION_STRELKA"));
		parser.accepts("gatk", Messages.getMessage("OPTION_GATK"));
		parser.accepts("it", Messages.getMessage("OPTION_INPUT_TUMOUR")).withRequiredArg().ofType(String.class).describedAs("input_tumour_bam");
		parser.accepts("in", Messages.getMessage("OPTION_INPUT_NORMAL")).withRequiredArg().ofType(String.class).describedAs("input_normal_bam");
		parser.accepts("pd", Messages.getMessage("OPTION_INPUT_NORMAL")).withRequiredArg().ofType(String.class).describedAs("pindel_deletions");

		//coverage
		parser.accepts("mincov", Messages.getMessage("OPTION_MIN_COV")).withRequiredArg().ofType(Integer.class).describedAs("min_coverage");
		parser.accepts("maxcov", Messages.getMessage("OPTION_MIN_COV")).withRequiredArg().ofType(Integer.class).describedAs("max_coverage");

		parser.acceptsAll(asList("h", "help"), HELP_OPTION);
		parser.acceptsAll(asList("v", "V", "version"), VERSION_OPTION);	
		options = parser.parse(args);	
		log = (String) options.valueOf("log");
		String loglevel = (String) options.valueOf("loglevel");

		if (loglevel == null) {
			loglevel = "INFO";
		}		

		if (options.has("r")) {
			reference = new File((String) options.valueOf("r"));
		}

		if (options.has("m")) {
			mode =  (String) options.valueOf("m");
		}

		if ( ! mode.equals(QBasePileupConstants.SNP_MODE) && ! mode.equals(QBasePileupConstants.SNP_CHECK_MODE) && ! mode.equals(QBasePileupConstants.INDEL_MODE) && ! mode.equals(QBasePileupConstants.COVERAGE_MODE) && ! mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
			throw new QBasePileupException("MODE_ERROR", mode);
		}		

		if (options.has("t")) {
			threadNo = (Integer) options.valueOf("t");
		} else {
			threadNo = 1;
		}	

		if (options.has("filter")) {
			filterQuery = (String) options.valueOf("filter");
		} 	

		if (QBasePileupConstants.SNP_MODE.equals(mode) || mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE) || mode.equals(QBasePileupConstants.SNP_CHECK_MODE)) {

			///default filter for compound snps:			
			if (null == filterQuery && mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
				filterQuery = "and(Flag_DuplicateRead==false , CIGAR_M>34 , MD_mismatch <= 3 , option_SM > 10)";
			}

			String posFile = (String) options.valueOf("s");
			if (posFile != null) {
				positionsFile = new File(posFile);
			}
			if (options.has("o")) {
				output = new File((String) options.valueOf("o"));
			}
			if (options.has("f")) {
				format = (String) options.valueOf("f");
			}

			if (options.has("of")) {
				String of = (String) options.valueOf("of");
				if (of.equals("columns")) {
					outputFormat = 2;
				}
			}	


			if (mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE) && ! format.equals("dcc1")) {
				throw new QBasePileupException("COMP_FILE_FORMAT_ERROR");
			}

			if (mode.equals(QBasePileupConstants.SNP_CHECK_MODE) && ! format.equals("maf")) {
				throw new QBasePileupException("CHECK_FILE_FORMAT_ERROR");
			}

			if (format == null) {
				format = "dcc1";
			}
			if (outputFormat == 2) {
				format = "columns";
			}
			String input = (String) options.valueOf("i");

			if (input != null) {
				inputType = INPUT_BAM;
				InputBAM i = new InputBAM(null, null, new File(input), inputType);
				inputBAMs.add(i);
			}

			String bamList = (String) options.valueOf("b");

			if (bamList != null) {
				File bamFileList = new File(bamList);
				if (!bamFileList.exists()) {
					throw new QBasePileupException("BAMFILELIST_ERROR", bamList);
				}
				inputType = INPUT_LIST;
				getBamList(bamFileList);			
			}

			if (options.has("hdf")) {
				hdf = new File((String) options.valueOf("hdf"));

				if (!hdf.exists()) {
					throw new QBasePileupException("NO_HDF", hdf.getAbsolutePath());
				}
				inputType = INPUT_HDF;
				getHDFBamList();

			}		

			if (options.has("p")) {
				profile = (String) options.valueOf("p");
			}			
			if (profile == null) {
				profile = "standard";
			}	


			//set filtering options			
			if (profile.equals("torrent")) {
				baseQuality = 0;
				mappingQuality = 1;
				indel = true;
				intron = true;
				novelstarts = false;
				strand=false;
			} else if (profile.equals("RNA")) {
				baseQuality = 7;
				mappingQuality = 10;
				indel = true;
				intron = true;
				novelstarts = true;
				strand=false;
			} else if (profile.equals("DNA")) {
				baseQuality = 10;
				mappingQuality = 10;
				indel = true;
				intron = false;
				novelstarts = false;
				strand=false;
			} else if (profile.equals("indel")) {

			}		    

			if (options.has("bq")) {
				baseQuality =  (Integer) options.valueOf("bq");
			}

			if (options.has("mq")) {
				mappingQuality =  (Integer) options.valueOf("mq");
			}			


			if (options.has("strand")) {
				String s = (String)options.valueOf("strand");
				if (s.equalsIgnoreCase("y")) {
					strand = true;
				}
				if (s.equalsIgnoreCase("n")) {
					strand = false;
				}
			}
			if (options.has("novelstarts")) {
				String s = (String)options.valueOf("novelstarts");				
				if (s.equalsIgnoreCase("y")) {
					novelstarts = true;
				}
				if (s.equalsIgnoreCase("n")) {
					novelstarts = false;
				}
			}
			if (options.has("intron")) {
				String s = (String)options.valueOf("intron");

				if (s.equalsIgnoreCase("y")) {
					intron = true;
				}
				if (s.equalsIgnoreCase("n")) {
					intron = false;
				}

			}
			if (options.has("ind")) {
				String s = (String)options.valueOf("ind");				
				if (s.equalsIgnoreCase("y")) {
					indel = true;
				}
				if (s.equalsIgnoreCase("n")) {
					indel = false;
				}
			}
		}

		if (QBasePileupConstants.INDEL_MODE.equals(mode)) {

			String somFile = (String) options.valueOf("is");
			if (somFile != null) {
				somaticIndelFile = new File(somFile);
			}

			String germFile = (String) options.valueOf("ig");
			if (germFile != null) {
				germlineIndelFile = new File(germFile);
			}

			String germOutFile = (String) options.valueOf("og");
			if (germOutFile != null) {
				germlineOutputFile = new File(germOutFile);
				//germlineOutputFile.delete();
			}

			String somOutFile = (String) options.valueOf("os");
			if (somOutFile != null) {
				somaticOutputFile = new File(somOutFile);
			}			

			//indel options
			if (options.has("sc")) {
				softClipWindow = (Integer)options.valueOf("sc");
			}
			if (options.has("hp")) {
				nearbyHomopolymer = (Integer)options.valueOf("hp");
			}
			if (options.has("n")) {
				nearbyIndelWindow = (Integer)options.valueOf("n");
			}		
			inputType = INPUT_BAM;
			tumourBam = new InputBAM(null, null, new File((String) options.valueOf("it")), inputType);
			normalBam = new InputBAM(null, null, new File((String) options.valueOf("in")), inputType);			

			if (options.has("pd") || options.has("pi")) {
				this.pindelMutations = getPindelMutations(options);
			}		

		}	

		if (QBasePileupConstants.COVERAGE_MODE.equals(mode)) {
			String posFile = (String) options.valueOf("s");
			if (posFile != null) {
				positionsFile = new File(posFile);
			}
			if (options.has("o")) {
				output = new File((String) options.valueOf("o"));
			}
			if (options.has("f")) {
				format = (String) options.valueOf("f");
			}			
			String input = (String) options.valueOf("i");
			if (input != null) {
				inputType = INPUT_BAM;
				InputBAM i = new InputBAM(null, null, new File(input), inputType);
				inputBAMs.add(i);
			}
			String bamList = (String) options.valueOf("b");

			if (bamList != null) {
				File bamFileList = new File(bamList);
				if (!bamFileList.exists()) {
					throw new QBasePileupException("BAMFILELIST_ERROR", bamList);
				}
				inputType = INPUT_LIST;
				getBamList(bamFileList);			
			}
			if (options.has("maxcov")) {				
				maxCoverage = (Integer) options.valueOf("maxcov");
			}
		}

		detectBadOptions();		
	}	

	public boolean includeDuplicates() {
		if (options.has("dup")) {
			return true;
		}
		return false;
	}

	public Integer getOutputFormat() {
		return outputFormat;
	}

	public Map<String, String[]> getPindelMutations() {
		return pindelMutations;
	}

	private Map<String, String[]> getPindelMutations(OptionSet options) throws IOException {
		Map<String, String[]> pindelMutations = new HashMap<String, String[]>();

		if (options.has("pd")) {
			File file = new File((String)options.valueOf("pd"));			
			readPindelMutationFile(pindelMutations, file, "DEL");
		}
		if (options.has("pi")) {
			File file = new File((String)options.valueOf("pi"));
			readPindelMutationFile(pindelMutations, file, "INS");
		}
		return pindelMutations;
	}

	private void readPindelMutationFile(Map<String, String[]> pindelMutations,
			File file, String type) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		List<String> lines = new ArrayList<String>();
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("####")) {
				if (lines.size() > 0) {
					String[] infos = lines.get(0).split("\t");
					String chr = infos[3].replace("ChrID ", "");
					Integer start = new Integer(infos[4].replace("BP ", ""));
					Integer end = new Integer(infos[5].replace("BP ", ""));
					if (type == "DEL") {
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
		reader.close();
	}

	public Integer getMaxCoverage() {
		return maxCoverage;
	}

	public String getFilterQuery() {
		return filterQuery;
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

	public boolean hasPindelOption() {
		return options.has("pindel");
	}

	public boolean hasStrelkaOption() {
		return options.has("strelka");
	}

	public String getInputType() {
		return inputType;
	}

	private void getHDFBamList() throws Exception {
		PileupHDF pileupHDF = new PileupHDF(hdf.getAbsolutePath(), false, false);
		pileupHDF.open();
		List<String> bamFiles = pileupHDF.getBamFilesInHeader();
		int count = 1;
		for (String bam: bamFiles) {
			InputBAM i = new InputBAM(count, "", new File(bam), inputType);
			inputBAMs.add(i);
			count++;
		}
		pileupHDF.close();
	}

	private void getBamList(File bamList) throws IOException, QBasePileupException {
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

	public List<InputBAM> getInputBAMs() {
		return inputBAMs;
	}

	public String getProfile() {
		return profile;
	}

	public String getFormat() {
		return format;
	}

	public Integer getThreadNo() {
		return threadNo;
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

	public File getPositionsFile() {
		return positionsFile;
	}

	public String getLog() {
		return log;
	}

	public boolean includeIndel() {
		return indel;
	}

	public boolean includeIntron() {
		return intron;
	}

	public File getHdf() {
		return hdf;
	}

	public OptionParser getParser() {
		return parser;
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

	boolean hasLogLevelOption() {
		return options.has("loglevel");
	}

	public void detectBadOptions() throws QBasePileupException {
		if ( ! hasHelpOption() && ! hasVersionOption()) {

			if (QBasePileupConstants.SNP_MODE.equals(mode) || mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE) 
					|| mode.equals(QBasePileupConstants.SNP_CHECK_MODE)) {

				if (null == output ) {
					throw new QBasePileupException("NO_OUTPUT_FILE");
				} else if (output.exists()) { 
					throw new QBasePileupException("OUTPUT_EXISTS", output.getAbsolutePath());
				}

				if (!format.equals("dcc1") && !format.equals("maf") && !format.equals("tab") &&
						!format.equals("dccq") && !format.equals("vcf") && !format.equals("hdf") && !format.equals("txt")
						&& !format.equals("columns")) {
					throw new QBasePileupException("UNKNOWN_FILE_FORMAT", format);
				}				

				if (!positionsFile.exists()) {
					throw new QBasePileupException("NO_POS_FILE", positionsFile.getAbsolutePath());
				}			


				if (inputBAMs.size() == 0 ) {
					throw new QBasePileupException("NO_FILE");
				} 

				for (InputBAM i: inputBAMs) {
					if (!i.exists()) {
						throw new QBasePileupException("FILE_EXISTS_ERROR", i.getBamFile().getAbsolutePath());
					}
				}

				if (mode.equals(QBasePileupConstants.SNP_MODE) || mode.equals(QBasePileupConstants.COMPOUND_SNP_MODE)) {
					checkReference();
					if ( ! profile.equals("standard") && ! profile.equals("torrent") && ! profile.equals("RNA") && ! profile.equals("DNA")) {
						throw new QBasePileupException("UNKNOWN_PROFILE", profile);
					}
				}

			}

			if (QBasePileupConstants.INDEL_MODE.equals(mode)) {
				checkReference();
				if ( ! tumourBam.exists()) {
					throw new QBasePileupException("FILE_EXISTS_ERROR", tumourBam.getBamFile().getAbsolutePath());
				}
				if ( ! normalBam.exists()) {
					throw new QBasePileupException("FILE_EXISTS_ERROR", normalBam.getBamFile().getAbsolutePath());
				}
				if (somaticIndelFile != null && ! somaticIndelFile.exists()) {
					throw new QBasePileupException("FILE_EXISTS_ERROR", somaticIndelFile.getAbsolutePath());
				}
				if (germlineIndelFile != null && ! germlineIndelFile.exists()) {
					throw new QBasePileupException("FILE_EXISTS_ERROR", germlineIndelFile.getAbsolutePath());
				}
				if (somaticOutputFile != null && somaticOutputFile.exists()) {
					throw new QBasePileupException("OUTPUT_EXISTS", somaticOutputFile.getAbsolutePath());
				}
				if (germlineOutputFile != null && germlineOutputFile.exists()) {
					throw new QBasePileupException("OUTPUT_EXISTS", germlineOutputFile.getAbsolutePath());
				}
				if ( ! options.has("pindel") && ! options.has("strelka") && ! options.has("gatk")) {
					throw new QBasePileupException("INDEL_FILETYPE_ERROR");
				}
			}
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

	public File getOutput() {
		return output;
	}

	public File getReference() {
		return reference;
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

	public InputBAM getTumourBam() {
		return tumourBam;
	}

	public InputBAM getNormalBam() {
		return normalBam;
	}

	public String getMode() {
		return mode;
	}

	public boolean hasGATKOption() {
		return options.has("gatk");
	}	
}

