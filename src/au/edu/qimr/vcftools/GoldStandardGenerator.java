package au.edu.qimr.vcftools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;


public class GoldStandardGenerator {
	
	private static QLogger logger;
	private QExec exec;
	private String[] vcfFiles;
	private String outputFileName;
	private String version;
	private String logFile;
	private String goldStandard;
	private boolean vcfOutput;
	private int exitStatus;
	private boolean somatic;
	private boolean germline;
	
	private final Map<ChrPositionRefAlt, AtomicInteger> positions = new HashMap<>(1024 * 64);
	
	protected int engage() throws IOException {
			
		logger.info("about to load vcf files");
		loadVcfs();
		writeOutput();
		return exitStatus;
	}
	
	
	private void writeOutput() throws FileNotFoundException {
		List<ChrPositionRefAlt> recs = positions.entrySet().stream().filter(e -> e.getValue().get() == vcfFiles.length).map(e -> e.getKey()).collect(Collectors.toList());
		recs.sort(null);
		
		logger.info("writing output");
		
		/*
		 * get some stats
		 */
		Map<String, AtomicInteger> mutationCounts = new HashMap<>();
		recs.forEach(cpra -> {
			String mutation = cpra.getName() + "->" + cpra.getAlt();
			mutationCounts.computeIfAbsent(mutation, f -> new AtomicInteger()).incrementAndGet();
		});
		mutationCounts.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue().get())).forEach(e -> logger.info("mutation: " + e.getKey() + ", counts: " + e.getValue().get()));
		
		
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(outputFileName)))) {
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
			
			
			String header = vcfOutput ? VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE : "#chr\tposition\tref\talt\tGT";
			ps.println(header);
			for (ChrPositionRefAlt cp : recs) {
				if (vcfOutput) {
					ps.println(cp.getChromosome() + Constants.TAB + cp.getStartPosition() + "\t.\t" + cp.getName() + cp.getAlt());
				} else {
					ps.println(cp.toTabSeperatedString());
				}
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
					
					processVcfRecord(rec, somatic, germline, positions);
				}
			}
			logger.info("input: " + (index+1) + " has " + i + " entries");
			logger.info("positions size: " + positions.size());
			i = 0;
			index++;
		}
		logger.info("Number of positions to be reported upon: " + positions.size());
	}


	/**
	 * @param rec
	 */
	public static void processVcfRecord(VcfRecord rec, boolean somatic, boolean germline, Map<ChrPositionRefAlt, AtomicInteger> map) {
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
					
					String gt = getGT(rec, recordSomatic);
					
					String ref = rec.getRef();
					String alt = rec.getAlt();
					
					if (ref.length() > 1) {
						/*
						 * split cs into constituent snps
						 */
						for (int z = 0 ; z < ref.length() ; z++) {
							addToMap(map, rec.getChrPosition().getChromosome(),  rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(z)+"",  alt.charAt(z) +Constants.TAB_STRING+ gt);
						}
					} else {
						addToMap(map, rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt+Constants.TAB+gt);
					}
				}
			}
		}
	}


	/**
	 * @param rec
	 * @param recordSomatic
	 * @return ./. if rec is null, or if it doesn't have a GT field in the format column
	 */
	public static String getGT(VcfRecord rec, boolean recordSomatic) {
		/*
		 * get GT field as this needs to be the same across samples
		 */
		String gt = "./.";
		if (null != rec) {
			List<String> ffList = rec.getFormatFields();
			if (ffList.size() > 1) {
				if (ffList.get(0).startsWith(VcfHeaderUtils.FORMAT_GENOTYPE)) {
					int position = ffList.size() == 2 ? 1 : recordSomatic ? 2 : 1;
					String stringOfInterest = ffList.get(position);
					int colonIndex = stringOfInterest.indexOf(Constants.COLON);
					gt = stringOfInterest.substring(0, colonIndex > -1 ? colonIndex : stringOfInterest.length());
					int ampIndex = gt.indexOf(Constants.VCF_MERGE_DELIM);
					if (ampIndex > -1) {
						gt = gt.substring(0, ampIndex);
					}
				}
			}
		}
		return gt;
	}
	
	public static void addToMap(Map<ChrPositionRefAlt, AtomicInteger> map, String chr, int start, int end, String ref, String alt) {
		if (null != map && null != chr && start >=0 && end >= start) {
			ChrPositionRefAlt cpn  = new ChrPositionRefAlt(chr, start, end, ref, alt);
			map.computeIfAbsent(cpn, v -> new AtomicInteger()).incrementAndGet();
		}
	}
	
	public static void main(String[] args) throws Exception {
		//loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(GoldStandardGenerator.class);
		
		GoldStandardGenerator qp = new GoldStandardGenerator();
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
			version = GoldStandardGenerator.class.getPackage().getImplementationVersion();
			if (null == version) {	version = "local"; }
			logger = QLoggerFactory.getLogger(GoldStandardGenerator.class, logFile, options.getLogLevel());
			exec = logger.logInitialExecutionStats("q3vcftools GoldStandardGenerator", version, args);
			
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
					outputFileName = optionsOutputFile;
				} else {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
				
				/*
				 * if output is a vcf file, set vcfOutput flag
				 */
				if (outputFileName.endsWith(".vcf") || outputFileName.endsWith(".vcf.gz")) {
					vcfOutput = true;
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
			if (vcfOutput)
				logger.info("Will output gold standard as a vcf file");
			
			options.getGoldStandard().ifPresent(s -> goldStandard = s);
			
			return engage();
		}
		return returnStatus;
	}

}
