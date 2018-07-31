package au.edu.qimr.vcftools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;


public class Classify {
	
	private static QLogger logger;
	private QExec exec;
	private String[] vcfFiles;
	private String outputDirectory;
	private String version;
	private String logFile;
	private String goldStandard;
	private boolean vcfOutput;
	private int exitStatus;
	private boolean somatic;
	private boolean germline;
	
	private final Map<ChrPosition, ChrPositionRefAlt> roguePositions = new HashMap<>(1024 * 64);
	private final List<VcfRecord> roguePositionsMatches = new ArrayList<>(1024 * 64);
	
	protected int engage() throws IOException {
			
		logger.info("about to load vcf files");
		loadVcfs();
		examineMatches();
//		writeOutput();
		return exitStatus;
	}
	
//	private void outputStats() throws FileNotFoundException {
//	
//		/*
//		 * first of all, the happy positions in all 
//		 */
//		final int numberOfInputFiles = vcfFiles.length + (null != goldStandard ? 1 : 0);
//		/*
//		 * print summary stats
//		 */
//		String[] inputs = new String[numberOfInputFiles];
//		int i = 0;
//		for (String s : vcfFiles) {
//			inputs[i++] = s;
//		}
//		if (null != goldStandard) {
//			inputs[i++] = goldStandard;
//		}
//		long centreVenn = positions.entrySet().stream().filter(e -> e.getValue().size() == numberOfInputFiles).count();
//		logger.info("number of variants in centre of venn: " + centreVenn);
//		
//		
////		Map<String, List<ChrPositionRefAlt>> uniqueVariants = new HashMap<>();
//		Map<String, List<ChrPositionRefAlt>> partiallyUniqueVariants = new HashMap<>();
//		
//		/*
//		 * get variants unique to one of the inputs
//		 */
//		positions.entrySet().stream().filter(e -> e.getValue().size() == 1).forEach(e -> {
//			partiallyUniqueVariants.computeIfAbsent(e.getValue().get(0), v -> new ArrayList<ChrPositionRefAlt>()).add(e.getKey());
//		});
//		
//		
//		/*
//		 * get variants unique to one of the inputs
//		 */
//		positions.entrySet().stream().filter(e -> e.getValue().size() > 1 && e.getValue().size() < numberOfInputFiles).forEach(e -> {
//			partiallyUniqueVariants.computeIfAbsent(e.getValue().stream().collect(Collectors.joining("\t")), v -> new ArrayList<ChrPositionRefAlt>()).add(e.getKey());
//		});
//		
//		
//		logger.info("number of partially unique variants: " + partiallyUniqueVariants.entrySet().stream().mapToInt(e -> e.getValue().size()).sum());
//		partiallyUniqueVariants.entrySet().stream().forEach(e -> logger.info("files: " + e.getKey() + " have " + e.getValue().size() + " partially unique variants"));
//		
//		
//		/*
//		 * assuming we have 3 inputs...
//		 */
//		logger.info("123: " + centreVenn);
//		String oneAndTwoS = inputs[0] + Constants.TAB + inputs[1];
//		int oneAndTwo =  partiallyUniqueVariants.containsKey(oneAndTwoS) ? partiallyUniqueVariants.get(oneAndTwoS).size() : 0;
//		logger.info("12: " + oneAndTwo);
//		int twoAndThree =  partiallyUniqueVariants.containsKey(inputs[1] + Constants.TAB + inputs[2]) ? partiallyUniqueVariants.get(inputs[1] + Constants.TAB + inputs[2]).size() : 0;
//		logger.info("23: " + twoAndThree);
//		int oneAndThree =  partiallyUniqueVariants.containsKey(inputs[0] + Constants.TAB + inputs[2]) ? partiallyUniqueVariants.get(inputs[0] + Constants.TAB + inputs[2]).size() : 0;
//		logger.info("13: " + oneAndThree);
//		logger.info("1: " + (null != partiallyUniqueVariants.get(inputs[0]) ? partiallyUniqueVariants.get(inputs[0]).size() : 0));
//		logger.info("2: " + (null != partiallyUniqueVariants.get(inputs[1]) ? partiallyUniqueVariants.get(inputs[1]).size() : 0));
//		logger.info("3: " + (null != partiallyUniqueVariants.get(inputs[2]) ? partiallyUniqueVariants.get(inputs[2]).size() : 0));
//		logger.info("file 1: " + inputs[0]);
//		logger.info("file 2: " + inputs[1]);
//		logger.info("file 3: " + inputs[2]);
//		
//		
//		/*
//		 * output all variants NOT found in 1, so that they can be put through the classifier application (yet to be written)
//		 */
//		List<ChrPositionRefAlt> notInOne = new ArrayList<>();
//		partiallyUniqueVariants.forEach((s,l) -> {
//			if ( ! s.contains(inputs[0])) {
//				notInOne.addAll(l);
//			}
//		});
//		logger.info("number of variants not in first input file: " + notInOne.size());
//		writeOutput(notInOne, outputDirectory + "/notInOne.vcf");
//		
//	}
	
	
	private void writeOutput(List<ChrPositionRefAlt> recs, String output) throws FileNotFoundException {
		recs.sort(new ChrPositionComparator());
		
		logger.info("writing output");
		
		
		
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(output)))) {
			/*
			 * put input files along with their positions into the file header
			 */
			int j = 1;
			for (String s : vcfFiles) {
				ps.println("##" + j++ + ": vcf file: " + s);
			}
			if (null != goldStandard) {
				ps.println("##: gold standard file: " + goldStandard);
			}
			
			
			String header =  VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE ;
			if (vcfOutput) {
				ps.println(VcfHeaderUtils.CURRENT_FILE_FORMAT);
			}
			ps.println(header);
			for (ChrPositionRefAlt cp : recs) {
//				logger.info("writing out: " + cp.getName());
				ps.println(cp.getChromosome() + Constants.TAB + cp.getStartPosition() + "\t.\t" + cp.getName() + Constants.TAB + cp.getAlt() + "\t.\t.\t.");
			}
		}
		
		logger.info("writing output- DONE");
	}
	
	private void examineMatches() {
		logger.info("Found " + roguePositionsMatches.size() + " in vcf file out of a potential " + roguePositions.size());
		int germlineCount = 0;
		int somaticCount = 0;
		int pass = 0;
		Map<String, List<VcfRecord>> filterDistSom = new HashMap<>();
		Map<String, List<VcfRecord>> filterDistGerm = new HashMap<>();
		for (VcfRecord v : roguePositionsMatches) {
			if (VcfUtils.isRecordSomatic(v)) {
				somaticCount++;
				filterDistSom.computeIfAbsent(getFilter(v), f -> new ArrayList<>()).add(v);
			} else {
				germlineCount++;
				filterDistGerm.computeIfAbsent(getFilter(v), f -> new ArrayList<>()).add(v);
			}
			if (VcfUtils.isRecordAPass(v)) {
				pass++;
			}
		}
		logger.info("pass: " + pass + ", somaticCount: " + somaticCount + ", germlineCount: " + germlineCount);
		Comparator<Entry<String,  List<VcfRecord>>> comp = Comparator.comparingInt(e -> e.getValue().size());
		filterDistSom.entrySet().stream().sorted(comp).forEach(e -> logger.info("somatic, filter: " + e.getKey() + ", count: " + e.getValue().size() + ", v: " + e.getValue().get(0).toSimpleString()));
		filterDistGerm.entrySet().stream().sorted(comp).forEach(e -> logger.info("germline, filter: " + e.getKey() + ", count: " + e.getValue().size() + ", v: " + e.getValue().get(0).toSimpleString()));
	}
	
	public static String getFilter(VcfRecord v) {
		String filter = v.getFilter();
		if (StringUtils.isNullOrEmptyOrMissingData(filter)) {
			/*
			 * look to see if filter is in the format column
			 */
			List<String> formatFields = v.getFormatFields();
			if (null != formatFields && formatFields.size() > 0) {
				if (formatFields.get(0).contains(VcfHeaderUtils.FORMAT_FILTER)) {
					
					/*
					 * get filters for all format values
					 */
					Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(formatFields);
					String [] filterArray = ffMap.get(VcfHeaderUtils.FORMAT_FILTER);
					if (null != filterArray && filterArray.length > 0) {
						filter = Arrays.stream(filterArray).collect(Collectors.joining(Constants.COMMA_STRING));
					}
				}
			}
		}
		return filter;
	}
	
	private void loadVcfs() throws IOException {
		int i = 0;
		int index = 0;
		for (String s : vcfFiles) {
			if (index == 0) {
				try (VCFFileReader reader = new VCFFileReader(new File(s))) {
					for (VcfRecord rec : reader) {
						if (++ i % 1000000 == 0) {
							logger.info("hit " + i + " entries");
						}
						
						processVcfRecord(rec, somatic, germline, roguePositions);
					}
				}
				logger.info("input: " + (index+1) + " has " + i + " entries");
				logger.info("positions size: " + roguePositions.size());
				i = 0;
			} else {
				try (VCFFileReader reader = new VCFFileReader(new File(s))) {
					for (VcfRecord rec : reader) {
						if (++ i % 1000000 == 0) {
							logger.info("hit " + i + " entries");
						}
						
						if (roguePositions.containsKey(rec.getChrPosition())) {
							roguePositionsMatches.add(rec);
						}
					}
				}
			}
			index++;
		}
	}


	/**
	 * @param rec
	 */
	public static void processVcfRecord(VcfRecord rec, boolean somatic, boolean germline, Map<ChrPosition, ChrPositionRefAlt> map) {
		/*
		 * we want HC or PASS
		 */
//		if (Amalgamator.isRecordHighConfOrPass(rec)) {
			
			/*
			 * only process record if it is of the desired type .eg somatic
//			 */
//			boolean recordSomatic = Amalgamator.isRecordSomatic(rec);
//			if ( (recordSomatic && somatic) || ( ! recordSomatic && germline)) {
				
				
				/*
				 * only deal with snps and compounds for now
				 */
				if (VcfUtils.isRecordASnpOrMnp(rec)) {
					
//					String gt = GoldStandardGenerator.getGT(rec, recordSomatic);
					
					String ref = rec.getRef();
					String alt = rec.getAlt();
					
					if (ref.length() > 1) {
						/*
						 * split cs into constituent snps
						 */
						for (int z = 0 ; z < ref.length() ; z++) {
							addToMap(map, rec.getChrPosition().getChromosome(),  rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(z)+"",  alt.charAt(z)+"");
//							addToMap(map, rec.getChrPosition().getChromosome(),  rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(z)+"",  alt.charAt(z) +Constants.TAB_STRING+ gt, input);
						}
					} else {
						addToMap(map, rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt);
//						addToMap(map, rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt+Constants.TAB+gt, input);
					}
				}
//			}
//		}
	}


//	/**
//	 * @param rec
//	 * @param recordSomatic
//	 * @return ./. if rec is null, or if it doesn't have a GT field in the format column
//	 */
//	public static String getGT(VcfRecord rec, boolean recordSomatic) {
//		/*
//		 * get GT field as this needs to be the same across samples
//		 */
//		String gt = "./.";
//		if (null != rec) {
//			List<String> ffList = rec.getFormatFields();
//			if (ffList.size() > 1) {
//				if (ffList.get(0).startsWith(VcfHeaderUtils.FORMAT_GENOTYPE)) {
//					int position = ffList.size() == 2 ? 1 : recordSomatic ? 2 : 1;
//					String stringOfInterest = ffList.get(position);
//					int colonIndex = stringOfInterest.indexOf(Constants.COLON);
//					gt = stringOfInterest.substring(0, colonIndex > -1 ? colonIndex : stringOfInterest.length());
//					int ampIndex = gt.indexOf(Constants.VCF_MERGE_DELIM);
//					if (ampIndex > -1) {
//						gt = gt.substring(0, ampIndex);
//					}
//				}
//			}
//		}
//		return gt;
//	}
	
	public static void addToMap(Map<ChrPosition, ChrPositionRefAlt> map, String chr, int start, int end, String ref, String alt) {
		if (null != map && null != chr && start >=0 && end >= start) {
			ChrPosition cp = new ChrPointPosition(chr, start);
			ChrPositionRefAlt cpn  = new ChrPositionRefAlt(chr, start, end, ref, alt);
			map.computeIfAbsent(cp, v -> cpn);
		}
	}
	
	public static void main(String[] args) throws Exception {
		//loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(Classify.class);
		
		Classify qp = new Classify();
		int exitStatus = qp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		} else {
			System.err.println("Exit status: " + exitStatus);
		}		
		System.exit( exitStatus );
	}

	private int setup(String[] args) throws Exception {
		int returnStatus = 1;
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.GOLD_STANDARD_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getVcfs().length < 1) {
			System.err.println(Messages.GOLD_STANDARD_USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			version = Classify.class.getPackage().getImplementationVersion();
			if (null == version) {	version = "local"; }
			logger = QLoggerFactory.getLogger(Classify.class, logFile, options.getLogLevel());
			exec = logger.logInitialExecutionStats("q3vcftools Overlap", version, args);
			
			// get list of file names
			vcfFiles = options.getVcfs();
			if (vcfFiles.length < 1) {
				throw new Exception("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < vcfFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(vcfFiles[i])) {
						throw new Exception("INPUT_FILE_ERROR: "  +  vcfFiles[i]);
					}
				}
			}
			vcfOutput = options.hasVcfOutputOption();
			
			// set outputfile - if supplied, check that it can be written to
			if (null != options.getOutputFileName()) {
				String optionsOutputFile = options.getOutputFileName();
				if (FileUtils.canFileBeWrittenTo(optionsOutputFile)) {
					outputDirectory = optionsOutputFile;
				} else {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
				
			}
			
			logger.info("vcf input files: " + Arrays.deepToString(vcfFiles));
			logger.info("outputDirectory: " + outputDirectory);
			
			somatic = options.hasSomaticOption();
			germline = options.hasGermlineOption();
			
			if ( ! somatic && ! germline) {
				somatic = true; germline = true;
			}
			if (somatic)
				logger.info("Will process somatic records");
			if (germline)
				logger.info("Will process germline records");
			if (vcfOutput)
				logger.info("Will output gold standard as a vcf file");
			
			options.getGoldStandard().ifPresent(s -> goldStandard = s);
			
			return engage();
		}
		return returnStatus;
	}

}
