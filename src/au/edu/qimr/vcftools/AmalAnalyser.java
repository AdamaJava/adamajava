package au.edu.qimr.vcftools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.TabTokenizer;


public class AmalAnalyser {
	
	private static QLogger logger;
	private QExec exec;
	private String[] vcfFiles;
	private String outputFileName;
	private String version;
	private String logFile;
	private int exitStatus;
	private boolean somatic;
	private boolean germline;
	
	private List<String> amalData;
	
	protected int engage() throws IOException {
			
		logger.info("about to load amalgamator data");
		loadAmalgamatorData();
		diceData();
		writeOutput();
		return exitStatus;
	}
	
	private void diceData() {
		/*
		 * assume first 4 columns are GS
		 */
		logger.info(amalData.get(0));
		String[] header = TabTokenizer.tokenize(amalData.get(0));
		List<String> gtHeaders = Arrays.stream(header).filter(s -> s.startsWith("GT")).collect(Collectors.toList());
		logger.info("number of gt columns: " + gtHeaders.size());
		int startPos = StringUtils.getPositionOfStringInArray(header, "GT:1" , false);
		int endPos = StringUtils.getPositionOfStringInArray(header, "GT:" + gtHeaders.size(), false);
		logger.info("startPos: " + startPos + ", endPos: " + endPos);
		
		Map<String, AtomicInteger> gtDist = new HashMap<>();
		
		for (String s : amalData) {
			if ( ! s.startsWith("#")) {
				
				String [] array = TabTokenizer.tokenize(s);
				
				String presentInGTs = whatGTsContainVariant(Arrays.copyOfRange(array, startPos, endPos + 1));
				
				gtDist.computeIfAbsent(presentInGTs, f -> new AtomicInteger()).incrementAndGet();
				
			}
		}
		
		logger.info("ALL:");
		AtomicInteger tally = new AtomicInteger();
		gtDist.entrySet().forEach(e -> {
			tally.addAndGet(e.getValue().get());
			logger.info("included in: " + e.getKey() + ", count: " + e.getValue().get());
		});
		logger.info("tally: " + tally.get());
		logger.info("FALSE POSITIVES:");
		AtomicInteger fpTally = new AtomicInteger();
		gtDist.entrySet().forEach(e -> {
			if ( ! e.getKey().contains("1,2,3,4") ) {
				fpTally.addAndGet(e.getValue().get());
				logger.info("included in: " + e.getKey() + ", count: " + e.getValue().get());
			}
		});
		logger.info("fpTally: " + fpTally.get());
		logger.info("FALSE NEGATIVES:");
		AtomicInteger fnTally = new AtomicInteger();
		gtDist.entrySet().forEach(e -> {
			if ( e.getKey().contains("1,2,3,4") && ! e.getKey().contains("5,6,7,8,9")) {
				fnTally.addAndGet(e.getValue().get());
				logger.info("included in: " + e.getKey() + ", count: " + e.getValue().get());
			}
		});
		logger.info("fnTally: " + fnTally.get());
		
	}
	
	
	private void writeOutput() throws FileNotFoundException {
		logger.info(amalData.get(0));
		String[] header = TabTokenizer.tokenize(amalData.get(0));
		List<String> gtHeaders = Arrays.stream(header).filter(s -> s.startsWith("GT")).collect(Collectors.toList());
		logger.info("number of gt columns: " + gtHeaders.size());
		int startPos = StringUtils.getPositionOfStringInArray(header, "GT:1" , false);
		int endPos = StringUtils.getPositionOfStringInArray(header, "GT:" + gtHeaders.size(), false);
		logger.info("startPos: " + startPos + ", endPos: " + endPos);
		
		try (PrintStream ps1 = new PrintStream(new FileOutputStream(new File(outputFileName + "-1")));
				PrintStream ps2 = new PrintStream(new FileOutputStream(new File(outputFileName + "-2")));
				PrintStream ps3 = new PrintStream(new FileOutputStream(new File(outputFileName + "-3")));
				PrintStream ps4 = new PrintStream(new FileOutputStream(new File(outputFileName + "-4")));
				PrintStream ps5 = new PrintStream(new FileOutputStream(new File(outputFileName + "-5")));) {
			
			
			for (String s : amalData) {
				if ( ! s.startsWith("#")) {
					
					String [] array = TabTokenizer.tokenize(s);
					
					/*
					 * only print out records where we have no gold standard
					 */
					if (StringUtils.isNullOrEmptyOrMissingData(array[startPos]) || array[startPos].equals(Constants.MISSING_GT)) {
					
						for (int i = 0 ; i < gtHeaders.size() - 4 ; i++) {
							if ( ! StringUtils.isNullOrEmptyOrMissingData(array[startPos + 4 + i]) && ! array[startPos + 4 + i].equals(Constants.MISSING_GT)) {
								switch (i) {
									case 0: ps1.println(array[0] + Constants.TAB + array[1]); break;
									case 1: ps2.println(array[0] + Constants.TAB + array[1]);break;
									case 2: ps3.println(array[0] + Constants.TAB + array[1]);break;
									case 3: ps4.println(array[0] + Constants.TAB + array[1]);break;
									case 4: ps5.println(array[0] + Constants.TAB + array[1]);break;
									default: logger.info("oops");
								}
							}
						}
					}
				}
			}
		}
		
		/*
		 * false negatives
		 */

		try (PrintStream ps1 = new PrintStream(new FileOutputStream(new File(outputFileName + "-fn-1")));
				PrintStream ps2 = new PrintStream(new FileOutputStream(new File(outputFileName + "-fn-2")));
				PrintStream ps3 = new PrintStream(new FileOutputStream(new File(outputFileName + "-fn-3")));
				PrintStream ps4 = new PrintStream(new FileOutputStream(new File(outputFileName + "-fn-4")));
				PrintStream ps5 = new PrintStream(new FileOutputStream(new File(outputFileName + "-fn-5")));
				PrintStream ps6 = new PrintStream(new FileOutputStream(new File(outputFileName + "-fn-6")));) {
			
			
			for (String s : amalData) {
				if ( ! s.startsWith("#")) {
					
					String [] array = TabTokenizer.tokenize(s);
					
					/*
					 * only print out records where we have a gold standard and a missing record in one of the others
					 */
					if ( ! StringUtils.isNullOrEmptyOrMissingData(array[startPos+1]) && ! array[startPos+1].equals(Constants.MISSING_GT)
							&& doesArrayContainEmptyGT(Arrays.copyOfRange(array, startPos, endPos+1))) {
					
						for (int i = 0 ; i < gtHeaders.size() - 3 ; i++) {
							if ( ! StringUtils.isNullOrEmptyOrMissingData(array[startPos + 3 + i]) && ! array[startPos + 3 + i].equals(Constants.MISSING_GT)) {
								switch (i) {
									case 0: ps1.println(array[0] + Constants.TAB + array[1]); break;
									case 1: ps2.println(array[0] + Constants.TAB + array[1]);break;
									case 2: ps3.println(array[0] + Constants.TAB + array[1]);break;
									case 3: ps4.println(array[0] + Constants.TAB + array[1]);break;
									case 4: ps5.println(array[0] + Constants.TAB + array[1]);break;
									case 5: ps6.println(array[0] + Constants.TAB + array[1]);break;
									default: logger.info("oops");
								}
							}
						}
					}
				}
			}
		}
		
		/*
		 * unique to GS - look in ALL-ACs columns here to see what else was called
		 */
		try (PrintStream ps1 = new PrintStream(new FileOutputStream(new File(outputFileName + "-GSonly-1")));
				PrintStream ps2 = new PrintStream(new FileOutputStream(new File(outputFileName + "-GSonly-2")));
				PrintStream ps3 = new PrintStream(new FileOutputStream(new File(outputFileName + "-GSonly-3")));
				PrintStream ps4 = new PrintStream(new FileOutputStream(new File(outputFileName + "-GSonly-4")));
				PrintStream ps5 = new PrintStream(new FileOutputStream(new File(outputFileName + "-GSonly-5")));) {
			
			int startAllPos = StringUtils.getPositionOfStringInArray(header, "ALL-AC:1" , false);
			int endAllPos = StringUtils.getPositionOfStringInArray(header, "ALL-AC:" + (gtHeaders.size()-4), false);
			logger.info("startAllPos: " + startAllPos + ", endAllPos: " + endAllPos);
			
			for (String s : amalData) {
				if ( ! s.startsWith("#")) {
					
					String [] array = TabTokenizer.tokenize(s);
					
					/*
					 * only print out records where we have a gold standard and a missing record in one of the others
					 */
					if ( ! StringUtils.isNullOrEmptyOrMissingData(array[startPos+1]) && ! array[startPos+1].equals(Constants.MISSING_GT)
							&& doesArrayContainOnlyEmptyGT(Arrays.copyOfRange(array, startPos+4, endPos+1))) {
						
//						logger.info("in unique to GS with " + Arrays.stream(array).collect(Collectors.joining(",")));
						for (int i = 0 ; i < gtHeaders.size() - 4 ; i++) {
							if ( ! StringUtils.isNullOrEmptyOrMissingData(array[startAllPos + i])) {
								switch (i) {
									case 0: ps1.println(array[0] + Constants.TAB + array[1]); break;
									case 1: ps2.println(array[0] + Constants.TAB + array[1]);break;
									case 2: ps3.println(array[0] + Constants.TAB + array[1]);break;
									case 3: ps4.println(array[0] + Constants.TAB + array[1]);break;
									case 4: ps5.println(array[0] + Constants.TAB + array[1]);break;
									default: logger.info("oops");
								}
							}
						}
					}
				}
			}
		}
		
	}
	
	public static boolean doesArrayContainEmptyGT(String [] array) {
		for (String s : array) {
			if (StringUtils.isNullOrEmptyOrMissingData(s) || Constants.MISSING_GT.equals(s)) {
				return true;
			}
		}
		return false;
	}
	public static boolean doesArrayContainOnlyEmptyGT(String [] array) {
		for (String s : array) {
			if ( ! StringUtils.isNullOrEmptyOrMissingData(s) && ! Constants.MISSING_GT.equals(s)) {
				return false;
			}
		}
		return true;
	}
	
	public static String whatGTsContainVariant(String[] array) {
		StringBuilder sb = new StringBuilder();
		if (null != array) {
			int i = 1;
			for (String s : array) {
				if ( ! StringUtils.isNullOrEmptyOrMissingData(s) && ! Constants.MISSING_GT.equals(s)) {
					StringUtils.updateStringBuilder(sb, ""+i, Constants.COMMA);
				}
				i++;
			}
		}
		
		return sb.toString();
	}
	
	private void loadAmalgamatorData() throws IOException {
		int i = 0;
		int index = 0;
			
		Path p = new File(vcfFiles[0]).toPath();
		amalData = Files.lines(p).filter(l -> ! l.startsWith("##"))
			.collect(Collectors.toList());
		 
		logger.info("input: " + (index+1) + " has " + i + " entries");
		logger.info("data size: " + amalData.size());
		i = 0;
		index++;
	}
	
	public static void main(String[] args) throws Exception {
		//loads all classes in referenced jars into memory to avoid nightly build shenanegans
		LoadReferencedClasses.loadClasses(AmalAnalyser.class);
		
		AmalAnalyser qp = new AmalAnalyser();
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
			System.err.println(Messages.AMALGAMATOR_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getVcfs().length < 1) {
			System.err.println(Messages.AMALGAMATOR_USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			version = AmalAnalyser.class.getPackage().getImplementationVersion();
			if (null == version) {	version = "local"; }
			logger = QLoggerFactory.getLogger(AmalAnalyser.class, logFile, options.getLogLevel());
			exec = logger.logInitialExecutionStats("q3vcftools AmalAnalyser", version, args);
			
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
			
			// set outputfile - if supplied, check that it can be written to
			if (null != options.getOutputFileName()) {
				String optionsOutputFile = options.getOutputFileName();
				if (FileUtils.canFileBeWrittenTo(optionsOutputFile)) {
					outputFileName = optionsOutputFile;
				} else {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
			}
			
			logger.info("vcf input files: " + Arrays.deepToString(vcfFiles));
			logger.info("outputFile: " + outputFileName);
			
			somatic = options.hasSomaticOption();
			germline = options.hasGermlineOption();
			
			if ( ! somatic && ! germline) {
				somatic = true; germline = true;
			}
			if (somatic)
				logger.info("Will process somatic records");
			if (germline)
				logger.info("Will process germline records");
			
			
			return engage();
		}
		return returnStatus;
	}

}
