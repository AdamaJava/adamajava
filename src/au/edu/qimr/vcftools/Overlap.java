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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrPositionRefAlt;
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


public class Overlap {
	
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
	
	private final Map<ChrPositionRefAlt, List<String>> positions = new HashMap<>(1024 * 64);
	
	protected int engage() throws IOException {
			
		logger.info("about to load vcf files");
		loadVcfs();
		addGoldStandard();
		outputStats();
//		writeOutput();
		return exitStatus;
	}
	
	private void addGoldStandard() throws IOException {
		if (null != goldStandard) {
			Path p = new File(goldStandard).toPath();
			Files.lines(p).filter(s -> ! s.startsWith("#"))
					.map(s -> TabTokenizer.tokenize(s))
					.filter(arr -> arr[2].length() == 1 && arr[3].length() == 1)
					.forEach(arr -> {
						addToMap(positions, arr[0],Integer.parseInt(arr[1].replaceAll(",", "")),Integer.parseInt(arr[1].replaceAll(",", "")), arr[2], arr[3], goldStandard);
					});
			
		}
	}
	
	private void outputStats() {
		/*
		 * comparing 2 inputs - could be 2 vcfs, could be 1 vcf and the gold standard
		 */
		
//		final int numberOfInputFiles = vcfFiles.length + (null != goldStandard ? 1 : 0);
		final int totalVariants = positions.size();
		
		Map<String, List<ChrPositionRefAlt>> positionsByInput = new HashMap<>();
		
		positions.forEach((k,v) -> {
			String files = v.stream().collect(Collectors.joining(Constants.TAB_STRING));
			positionsByInput.computeIfAbsent(files, f -> new ArrayList<>()).add(k);
		});
		
		StringBuilder sb = new StringBuilder();
		positionsByInput.forEach((k,v) -> {
			double perc = 100.0 * v.size() / totalVariants;
			if (k.contains(Constants.TAB_STRING)) {
				sb.append("In both: ").append(v.size()).append(" (").append(String.format("%.2f", perc)).append("%)");
			} else {
				int position = Arrays.binarySearch(vcfFiles, k);
				String sPos = "";
				if (position < 0) {
					if (k.equals(goldStandard)) {
						sPos = "gold standard";
					}
				} else {
					sPos = "file " + (position + 1);
				}
				
				sb.append(", In " + sPos + " only: ").append(v.size()).append(" (").append(String.format("%.2f", perc)).append("%)");
			}
			logger.info("files: " + k + " have " + v.size() + " positions (" +String.format("%.2f", perc)+"%)");
			/*
			 * output entries that belong to a single file
			 */
			if ( ! k.contains(Constants.TAB_STRING)) {
				try {
					writeOutput(v, outputDirectory + "/uniqueTo" + new File(k).getName());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
		
		logger.info("summary string: " + sb.toString());
		
		try {
			writeSummaryLineToFile(sb.toString(), outputDirectory + "/summary.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
//		long totalVariants = positions.size();
//		long centreVenn = positions.entrySet().stream().filter(e -> e.getValue().size() == numberOfInputFiles).count();
//		double centreVennPerc = 100.0 * centreVenn / totalVariants;
//		logger.info("total number of variants: " + totalVariants);
//		logger.info("number of variants in centre of venn: " + centreVenn + " ("+String.format("%.2f", centreVennPerc)+"%)");
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
	private void writeSummaryLineToFile(String line, String output) throws FileNotFoundException {
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(output)))) {
			ps.println(line);
		}
	}
	
	private void writeOutput(List<ChrPositionRefAlt> recs, String output) throws FileNotFoundException {
		recs.sort(new ChrPositionComparator());
		
		logger.info("writing output");
		
		
		
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(output)))) {
			/*
			 * put input files along with their positions into the file header
			 */
//			int j = 1;
//			for (String s : vcfFiles) {
//				ps.println("##" + j++ + ": vcf file: " + s);
//			}
//			if (null != goldStandard) {
//				ps.println("##: gold standard file: " + goldStandard);
//			}
			
			
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
	
	private void loadVcfs() throws IOException {
		int i = 0;
		int index = 0;
		for (String s : vcfFiles) {
			
			try (VCFFileReader reader = new VCFFileReader(new File(s))) {
				for (VcfRecord rec : reader) {
					if (++ i % 1000000 == 0) {
						logger.info("hit " + i + " entries");
					}
					
					processVcfRecord(rec, somatic, germline, positions, s);
				}
			}
			logger.info("input: " + (index+1) + " has " + i + " entries");
			logger.info("positions size: " + positions.size());
			i = 0;
			index++;
		}
//		logger.info("Number of positions to be reported upon: " + positions.size());
	}


	/**
	 * @param rec
	 */
	public static void processVcfRecord(VcfRecord rec, boolean somatic, boolean germline, Map<ChrPositionRefAlt, List<String>> map, String input) {
		/*
		 * we want HC or PASS
		 */
		if (Amalgamator.isRecordHighConfOrPass(rec)) {
			
			/*
			 * only process record if it is of the desired type .eg somatic
			 */
			boolean recordSomatic = Amalgamator.isRecordSomatic(rec);
			if ( (recordSomatic && somatic) || ( ! recordSomatic && germline)) {
				
				
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
							addToMap(map, rec.getChrPosition().getChromosome(),  rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(z)+"",  alt.charAt(z)+"", input);
//							addToMap(map, rec.getChrPosition().getChromosome(),  rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(z)+"",  alt.charAt(z) +Constants.TAB_STRING+ gt, input);
						}
					} else {
						addToMap(map, rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt, input);
//						addToMap(map, rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt+Constants.TAB+gt, input);
					}
				}
			}
		}
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
	
	public static void addToMap(Map<ChrPositionRefAlt, List<String>> map, String chr, int start, int end, String ref, String alt, String input) {
		if (null != map && null != chr && start >=0 && end >= start) {
			ChrPositionRefAlt cpn  = new ChrPositionRefAlt(chr, start, end, ref, alt);
			map.computeIfAbsent(cpn, v -> new ArrayList<String>(4)).add(input);
		}
	}
	
	public static void main(String[] args) throws Exception {
		//loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(Overlap.class);
		
		Overlap qp = new Overlap();
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
			version = Overlap.class.getPackage().getImplementationVersion();
			if (null == version) {	version = "local"; }
			logger = QLoggerFactory.getLogger(Overlap.class, logFile, options.getLogLevel());
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
