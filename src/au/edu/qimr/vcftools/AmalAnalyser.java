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
		List<String> ftHeaders = Arrays.stream(header).filter(s -> s.startsWith("ALL-FT")).collect(Collectors.toList());
		logger.info("number of gt columns: " + gtHeaders.size());
		logger.info("number of all-ft columns: " + ftHeaders.size());
		int startPos = StringUtils.getPositionOfStringInArray(header, "GT:1" , false);
		int endPos = StringUtils.getPositionOfStringInArray(header, "GT:" + gtHeaders.size(), false);
		int startPosFT = StringUtils.getPositionOfStringInArray(header, "ALL-FT:1" , false);
		int endPosFT = StringUtils.getPositionOfStringInArray(header, "ALL-FT:" + ftHeaders.size(), false);
		logger.info("startPos: " + startPos + ", endPos: " + endPos);
		
		Map<String, AtomicInteger> gtDist = new HashMap<>();
		Map<String, AtomicInteger> ftDist = new HashMap<>();
		
		
		for (String s : amalData) {
			if ( ! s.startsWith("#")) {
				
				String [] array = TabTokenizer.tokenize(s);
				
				String presentInGTs = whatGTsContainVariant(Arrays.copyOfRange(array, startPos, endPos + 1));
				
				gtDist.computeIfAbsent(presentInGTs, f -> new AtomicInteger()).incrementAndGet();
				
				/*
				 * find false negatives
				 */
				if ( presentInGTs.contains("1,2,3,4") && ! presentInGTs.contains("5,6,7,8,9")) {
					String key = Arrays.stream(Arrays.copyOfRange(array, startPosFT, endPosFT+1)).collect(Collectors.joining(Constants.TAB_STRING));
					ftDist.computeIfAbsent(key, f -> new AtomicInteger()).incrementAndGet();
				}
				
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
		AtomicInteger fp5Tally = new AtomicInteger();
		AtomicInteger fp6Tally = new AtomicInteger();
		AtomicInteger fp7Tally = new AtomicInteger();
		AtomicInteger fp8Tally = new AtomicInteger();
		AtomicInteger fp9Tally = new AtomicInteger();
		gtDist.entrySet().forEach(e -> {
			if ( ! e.getKey().contains("1,2,3,4") ) {
				fpTally.addAndGet(e.getValue().get());
				logger.info("included in: " + e.getKey() + ", count: " + e.getValue().get());
				
				/*
				 * find out which pairs these positions belonged in
				 */
				for (String s : TabTokenizer.tokenize(e.getKey(), Constants.COMMA)) {
					switch (s) {
					case "5":fp5Tally.addAndGet(e.getValue().get()); break;
					case "6":fp6Tally.addAndGet(e.getValue().get()); break;
					case "7":fp7Tally.addAndGet(e.getValue().get()); break;
					case "8":fp8Tally.addAndGet(e.getValue().get()); break;
					case "9":fp9Tally.addAndGet(e.getValue().get()); break;
					}
				}
			}
		});
		logger.info("fpTally: " + fpTally.get());
		logger.info("fp in 5 Tally: " + fp5Tally.get());
		logger.info("fp in 6 Tally: " + fp6Tally.get());
		logger.info("fp in 7 Tally: " + fp7Tally.get());
		logger.info("fp in 8 Tally: " + fp8Tally.get());
		logger.info("fp in 9 Tally: " + fp9Tally.get());
		
		
		
		logger.info("FALSE NEGATIVES:");
		AtomicInteger fnTally = new AtomicInteger();
		gtDist.entrySet().forEach(e -> {
			if ( e.getKey().contains("1,2,3,4") && ! e.getKey().contains("5,6,7,8,9")) {
				fnTally.addAndGet(e.getValue().get());
				logger.info("included in: " + e.getKey() + ", count: " + e.getValue().get());
			}
		});
		logger.info("fnTally: " + fnTally.get());

		/*
		 * here we are looking for novel variants for each pair where the variant is not in the gold standard but is in at least 3 pairs, and across both technologies
		 * 5,6,7 - Hiseq, 8,9 - BGI
		 */
		AtomicInteger novel5Tally = new AtomicInteger();
		AtomicInteger novel6Tally = new AtomicInteger();
		AtomicInteger novel7Tally = new AtomicInteger();
		AtomicInteger novel8Tally = new AtomicInteger();
		AtomicInteger novel9Tally = new AtomicInteger();
		AtomicInteger novelTally = new AtomicInteger();
		gtDist.entrySet().forEach(e -> {
			if ( ! e.getKey().contains("1,2,3,4") && e.getKey().length() >= 5 
					&& (e.getKey().contains("5") || e.getKey().contains("6") || e.getKey().contains("7"))
					&& (e.getKey().contains("8") || e.getKey().contains("9"))) {
				
				novelTally.addAndGet(e.getValue().get());
				/*
				 * find out which pairs these positions belonged in
				 */
				for (String s : TabTokenizer.tokenize(e.getKey(), Constants.COMMA)) {
					switch (s) {
					case "5":novel5Tally.addAndGet(e.getValue().get()); break;
					case "6":novel6Tally.addAndGet(e.getValue().get()); break;
					case "7":novel7Tally.addAndGet(e.getValue().get()); break;
					case "8":novel8Tally.addAndGet(e.getValue().get()); break;
					case "9":novel9Tally.addAndGet(e.getValue().get()); break;
					}
				}
				
				
				logger.info("included in: " + e.getKey() + ", count: " + e.getValue().get());
			}
		});
		logger.info("novelTally: " + novelTally.get());
		logger.info("novel to 5 Tally: " + novel5Tally.get());
		logger.info("novel to 6 Tally: " + novel6Tally.get());
		logger.info("novel to 7 Tally: " + novel7Tally.get());
		logger.info("novel to 8 Tally: " + novel8Tally.get());
		logger.info("novel to 9 Tally: " + novel9Tally.get());
		
		
		
		logger.info("FT ALL:");
		AtomicInteger ftTally = new AtomicInteger();
		AtomicInteger ftHOMTally5 = new AtomicInteger();
		AtomicInteger ftHOMTally6 = new AtomicInteger();
		AtomicInteger ftHOMTally7 = new AtomicInteger();
		AtomicInteger ftHOMTally8 = new AtomicInteger();
		AtomicInteger ftHOMTally9 = new AtomicInteger();
		AtomicInteger ftMINTally5 = new AtomicInteger();
		AtomicInteger ftMINTally6 = new AtomicInteger();
		AtomicInteger ftMINTally7 = new AtomicInteger();
		AtomicInteger ftMINTally8 = new AtomicInteger();
		AtomicInteger ftMINTally9 = new AtomicInteger();
		AtomicInteger ftMIUNTally5 = new AtomicInteger();
		AtomicInteger ftMIUNTally6 = new AtomicInteger();
		AtomicInteger ftMIUNTally7 = new AtomicInteger();
		AtomicInteger ftMIUNTally8 = new AtomicInteger();
		AtomicInteger ftMIUNTally9 = new AtomicInteger();
		Map<String, AtomicInteger> firstFileDist = new HashMap<>();
		ftDist.entrySet().forEach(e -> {
			ftTally.addAndGet(e.getValue().get());
			String [] array = TabTokenizer.tokenize(e.getKey(), Constants.TAB);
			
			for (int i = 0, len = array.length ; i < len ; i++) {
				String s = array[i];
				firstFileDist.computeIfAbsent("file" + (i+5) +":"+ s, f -> new AtomicInteger()).addAndGet(e.getValue().get());
				
				if (s.contains("HOM")) {
					
					switch (i) {
					case 0:ftHOMTally5.addAndGet(e.getValue().get()); break;
					case 1:ftHOMTally6.addAndGet(e.getValue().get()); break;
					case 2:ftHOMTally7.addAndGet(e.getValue().get()); break;
					case 3:ftHOMTally8.addAndGet(e.getValue().get()); break;
					case 4:ftHOMTally9.addAndGet(e.getValue().get()); break;
					}
				} else if (s.contains("MIN")) {
					switch (i) {
					case 0:ftMINTally5.addAndGet(e.getValue().get()); break;
					case 1:ftMINTally6.addAndGet(e.getValue().get()); break;
					case 2:ftMINTally7.addAndGet(e.getValue().get()); break;
					case 3:ftMINTally8.addAndGet(e.getValue().get()); break;
					case 4:ftMINTally9.addAndGet(e.getValue().get()); break;
					}
				} else if (s.contains("MIUN")) {
					switch (i) {
					case 0:ftMIUNTally5.addAndGet(e.getValue().get()); break;
					case 1:ftMIUNTally6.addAndGet(e.getValue().get()); break;
					case 2:ftMIUNTally7.addAndGet(e.getValue().get()); break;
					case 3:ftMIUNTally8.addAndGet(e.getValue().get()); break;
					case 4:ftMIUNTally9.addAndGet(e.getValue().get()); break;
					}
				}
				
				
			}
			logger.info("included in: " + e.getKey() + ", count: " + e.getValue().get());
		});
		logger.info("ftTally: " + ftTally.get());
		logger.info("HOM tally 5 : " + ftHOMTally5.get());
		logger.info("HOM tally 6 : " + ftHOMTally6.get());
		logger.info("HOM tally 7 : " + ftHOMTally7.get());
		logger.info("HOM tally 8 : " + ftHOMTally8.get());
		logger.info("HOM tally 9 : " + ftHOMTally9.get());
		logger.info("MIN tally 5 : " + ftMINTally5.get());
		logger.info("MIN tally 6 : " + ftMINTally6.get());
		logger.info("MIN tally 7 : " + ftMINTally7.get());
		logger.info("MIN tally 8 : " + ftMINTally8.get());
		logger.info("MIN tally 9 : " + ftMINTally9.get());
		logger.info("MIUN tally 5 : " + ftMIUNTally5.get());
		logger.info("MIUN tally 6 : " + ftMIUNTally6.get());
		logger.info("MIUN tally 7 : " + ftMIUNTally7.get());
		logger.info("MIUN tally 8 : " + ftMIUNTally8.get());
		logger.info("MIUN tally 9 : " + ftMIUNTally9.get());
		
		logger.info("first file ft dist:");
		firstFileDist.entrySet().forEach(e -> {
			logger.info("included in: " + e.getKey() + ", count: " + e.getValue().get());
		});
		
		for (int i = 0 ; i < 5 ; i++) {
			final int j = i;
			logger.info("file" + (j + 5) + " HOM count: " + firstFileDist.entrySet().stream().filter(e -> e.getKey().contains("file" + (j + 5))).filter(e -> e.getKey().contains("HOM")).mapToInt(e -> e.getValue().get()).sum());
			logger.info("file" + (j + 5) + " MIN count: " + firstFileDist.entrySet().stream().filter(e -> e.getKey().contains("file" + (j + 5))).filter(e -> e.getKey().contains("MIN") && ! e.getKey().contains("HOM")).mapToInt(e -> e.getValue().get()).sum());
			logger.info("file" + (j + 5) + " MIUN count: " + firstFileDist.entrySet().stream().filter(e -> e.getKey().contains("file" + (j + 5))).filter(e -> e.getKey().contains("MIUN") && ! e.getKey().contains("MIN") && ! e.getKey().contains("HOM")).mapToInt(e -> e.getValue().get()).sum());
		}
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
