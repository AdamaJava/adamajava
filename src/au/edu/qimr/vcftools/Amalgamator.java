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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPointPosition;
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


public class Amalgamator {
	
	private static QLogger logger;
	private QExec exec;
	private String[] vcfFiles;
	private String outputFileName;
	private String version;
	private String logFile;
	private String goldStandard;
	private int exitStatus;
	private boolean somatic;
	private boolean germline;
	
	private final Map<ChrPosition, IdRefAlt> positions = new HashMap<>(1024 * 64);
	
	private class IdRefAlt {
		private String id;
		private final String ref;
		private final String alt;
		private final String[] gts;
		private final String[] acs;
		IdRefAlt(String ref, String alt, int fileCount) {
			this.ref = ref;
			this.alt = alt;
			gts = new String[fileCount];
			acs = new String[fileCount];
		}
		void setId(String id) {
			this.id = id;
		}
		public String toString() {
			return (null != id ? "GS:" + id : Constants.MISSING_DATA_STRING) + Constants.TAB 
					+ ref + Constants.TAB + alt + Constants.TAB + 
					getStringFromArray(gts, "./.") + Constants.TAB + getStringFromArray(acs, Constants.MISSING_DATA_STRING);
		}
		
	}
	
	public static String getStringFromArray(String [] arr, String missingData) {
		StringBuilder sb = new StringBuilder();
		for (String s : arr) {
			StringUtils.updateStringBuilder(sb, (null == s ? missingData : s), Constants.TAB);
		}
		return sb.toString();
	}
	
	protected int engage() throws IOException {
			
		logger.info("about to load vcf files");
		loadVcfs();
		addGoldStandard();
		writeOutput();
		return exitStatus;
	}
	
	private void addGoldStandard() throws IOException {
		if (null != goldStandard) {
			Path p = new File(goldStandard).toPath();
			Map<ChrPosition, String> gsMap = Files.lines(p).filter(s -> ! s.startsWith("#"))
					.map(s -> TabTokenizer.tokenize(s))
					.filter(arr -> arr[2].length() == 1 && arr[3].length() == 1)
//					.map(arr -> new ChrPositionName(arr[0], Integer.parseInt(arr[1].replaceAll(",", "")),Integer.parseInt(arr[1].replaceAll(",", "")), arr[3]))
					.collect(Collectors.toMap(arr ->  new ChrPointPosition(arr[0], Integer.parseInt(arr[1].replaceAll(",", ""))), arr -> arr[3]));
			logger.info("number of gold standard positions:" + gsMap.size());
			
			List<ChrPosition> recs = new ArrayList<>(positions.keySet());
			recs.forEach(r -> {
				String goldStandardAlt = gsMap.get(r);
				if (null != goldStandardAlt) {
					/*
					 * update entry in positions map
					 */
					positions.get(r).setId(goldStandardAlt);
				}
			});
			
		}
	}
	
	private void writeOutput() throws FileNotFoundException {
		List<ChrPosition> recs = new ArrayList<>(positions.keySet());
		recs.sort(null);
		
		logger.info("writing output");
		
		
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
			
			
			String header = "#chr\tposition\tid\tref\talt";
			for (int i = 1 ; i <= vcfFiles.length ; i++) {
				header += "\tGT:" + i;
			}
			for (int i = 1 ; i <= vcfFiles.length ; i++) {
				header += "\tAC:" + i;
			}
			ps.println(header);
			for (ChrPosition cp : recs) {
				IdRefAlt ira = positions.get(cp);
				ps.println(cp.getChromosome() + Constants.TAB + cp.getStartPosition() + Constants.TAB +ira.toString());
			}
		}
		logger.info("writing output- DONE");
	}
	
	private void loadVcfs() throws IOException {
		int i = 0;
		int fileCount = vcfFiles.length;
		int index = 0;
		int somaticCount = 0;
		for (String s : vcfFiles) {
			
			try (VCFFileReader reader = new VCFFileReader(new File(s))) {
				for (VcfRecord rec : reader) {
					if (++ i % 1000000 == 0) {
						logger.info("hit " + i + " entries");
					}
					
					/*
					 * we want HC or PASS
					 */
					if (isRecordHighConfOrPass(rec)) {
						
						/*
						 * only process record if it is of the desired type .eg somatic
						 */
						boolean recordSomatic = isRecordSomatic(rec);
						if ( (recordSomatic && somatic) || ( ! recordSomatic && germline)) {
							/*
							 * only deal with snps and compounds for now
							 */
							if (VcfUtils.isRecordASnpOrMnp(rec)) {
								
								String ref = rec.getRef();
								String alt = rec.getAlt();
								
								if (ref.length() > 1) {
									/*
									 * split cs into constituent snps
									 */
									for (int z = 0 ; z < ref.length() ; z++) {
										
										ChrPosition cpn  = new ChrPointPosition(rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition() + z);
										final int position = z;
										IdRefAlt ira = positions.computeIfAbsent(cpn, v -> new IdRefAlt(ref.charAt(position)+"", alt.charAt(position) + "", fileCount));
										
										/*
										 * most likely don't have GT and AC info for cs
										 */
										ira.gts[index] =  "C/S";
										ira.acs[index] =  "C,S,0";
									}
								} else {
									ChrPosition cpn  = new ChrPointPosition(rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition());
									IdRefAlt ira = positions.computeIfAbsent(cpn, v -> new IdRefAlt(ref, alt, fileCount));
									updateGTsAndACs(index, rec, recordSomatic, ref, alt, ira);
								}
							}
						}
					}
				}
			}
			logger.info("input: " + (index+1) + " has " + i + " entries");
			logger.info("positions size: " + positions.size());
			i = 0;
			index++;
		}
		logger.info("Number of positions to be reported upon: " + positions.size());
	}

	static void updateGTsAndACs(int index, VcfRecord rec, boolean recordSomatic, String ref, String alt, IdRefAlt ira) {
		List<String> ffList = rec.getFormatFields();
		/*
		 * get position of GT from first entry, and then get second (germline and qsnp)
		 */
		String [] formatHeaders = ffList.get(0).split(":");
		int gtPosition = getPositionFromHeader(formatHeaders, VcfHeaderUtils.FORMAT_GENOTYPE);
		int oabsPosition = getPositionFromHeader(formatHeaders, VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		
		/*
		 * if germline, get from second entry in list, for somatic, get third
		 */
		String [] params = ffList.get(recordSomatic ? 2 : 1).split(":"); 
		ira.gts[index] =  getStringFromArray(params, gtPosition);
		/*
		 * get allele dist next
		 */
		String oabs = getStringFromArray(params, oabsPosition);
		Map<String, Integer> alleleDist = VcfUtils.getAllelicCoverageFromAC(oabs);
		ira.acs[index] =  getAllelicDistFromMap(alleleDist, ref, alt);
	}
	
	static String getStringFromArray(String [] array, int position) {
		if (null == array || position < 0 || position >= array.length) {
			throw new IllegalArgumentException("Array is null, or position is beyond end of array");
		}
		String gts = array[position];
		int ampesandIndex = gts.indexOf(Constants.VCF_MERGE_DELIM);
		if (ampesandIndex > -1) {
			gts = gts.substring(0, ampesandIndex);
		}
		return gts;
	}
	
	static int getPositionFromHeader(String [] headers, String header) {
		int j = 0;
		for (String h : headers) {
			if (header.equals(h)) {
				return j;
			}
			j++;
		}
		return -1;
	}
	
	/*
	 * Returns a string representing the coverage counts of ref, alt and rest based on entries in the supplied map
	 */
	static String getAllelicDistFromMap(Map<String, Integer> map, String ref, String alt) {
		int refCount = map.computeIfAbsent(ref, v -> Integer.valueOf(0)).intValue();
		int altCount = map.computeIfAbsent(alt, v -> Integer.valueOf(0)).intValue();
		int restCount = map.entrySet().stream().filter(kv -> ! kv.getKey().equals(ref) && ! kv.getKey().equals(alt)).mapToInt(kv -> kv.getValue().intValue()).sum();
		
		return refCount + Constants.COMMA_STRING + altCount + Constants.COMMA_STRING + restCount; 
	}
	
	public static final boolean isRecordHighConfOrPass(VcfRecord r) {
		if (r.getInfo().contains("CONF=HIGH_1,HIGH_2")) {
			return true;
		}
		if ("PASS".equals(r.getFilter())) {
			return true;
		}
		List<String> l = r.getFormatFields();
		/*
		 * all fields in list ( apart from first) should have PASS
		 */
		long numberOfPasses = l.stream().filter(s -> s.contains("PASS")).count();
		
		return numberOfPasses == l.size() - 1;
	}
	
	public static final boolean isRecordSomatic(VcfRecord r) {
		if (VcfUtils.isRecordSomatic(r)) {
			return true;
		}
		List<String> l = r.getFormatFields();
		/*
		 * all fields in list ( apart from first) should have PASS
		 */
		long numberOfSomatics = l.stream().filter(s -> s.contains("SOMATIC")).count();
		
		return numberOfSomatics == (l.size() - 1) / 2;
	}
	
	public static void main(String[] args) throws Exception {
		//loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(Amalgamator.class);
		
		Amalgamator qp = new Amalgamator();
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
			version = Amalgamator.class.getPackage().getImplementationVersion();
			if (null == version) {	version = "local"; }
			logger = QLoggerFactory.getLogger(Amalgamator.class, logFile, options.getLogLevel());
			exec = logger.logInitialExecutionStats("q3vcftools Amalgamator", version, args);
			
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
			
			options.getGoldStandard().ifPresent(s -> goldStandard = s);
			
			return engage();
		}
		return returnStatus;
	}

}
