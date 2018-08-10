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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPositionRefAlt;
import org.qcmg.common.string.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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


public class AmalgamatorGS {
	
	private static QLogger logger;
	private QExec exec;
	private String[] vcfFiles;
	private String[] allFiles;
	private String outputFileName;
	private String version;
	private String logFile;
	private String goldStandard;
	private int exitStatus;
	private boolean somatic;
	private boolean germline;
	
	private final Map<ChrPositionRefAlt, Pair<String[], String[]>> positions = new HashMap<>(1024 * 64);
	private final Map<ChrPositionRefAlt, Pair<String[], String[]>> missingPositionsFromAll = new HashMap<>(1024 * 64);
	
	
	public static String getStringFromArray(String [] arr, String missingData) {
		StringBuilder sb = new StringBuilder();
		for (String s : arr) {
			StringUtils.updateStringBuilder(sb, (null == s ? missingData : s), Constants.TAB);
		}
		return sb.toString();
	}
	
	protected int engage() throws IOException {
			
		logger.info("about to load vcf files");
		addGoldStandard();
		loadVcfs();
		if (null != allFiles && allFiles.length > 0) {
			loadAlls();
		}
		writeOutput();
		return exitStatus;
	}
	
	private void addGoldStandard() throws IOException {
		if (null != goldStandard) {
			logger.info("Loading Gold Standard positions from " + goldStandard);
			Path p = new File(goldStandard).toPath();
			int vcfFileCount = vcfFiles.length; 
			
			 Files.lines(p).filter(s -> ! s.startsWith("#"))
				.map(s -> TabTokenizer.tokenize(s))
				.forEach(arr -> {
					/*
					 * only deal with snps
					 */
					if (arr[3].length() ==  arr[4].length() && arr[4].length() == 1) {
						int pos = Integer.parseInt(arr[2].replace(",", ""));
						ChrPositionRefAlt cpra = new ChrPositionRefAlt("chr" + arr[1], pos, pos, arr[3], arr[4]);
						
						
						Pair<String[], String[]> pair = positions.computeIfAbsent(cpra, f -> Pair.of(new String[4 + vcfFileCount], new String[4 + vcfFileCount]));
						String [] gts = pair.getLeft();
						String [] ads = pair.getRight();
						for (int i = 0 ; i < 4 ; i++) {
							gts[i] = arr[13 + i];
							ads[i] = arr[17 + i];
						}
					}
				});
			 logger.info("Finished loading Gold Standard positions - loaded: " + positions.size());
		}
	}
	
	private void writeOutput() throws FileNotFoundException {
		List<ChrPositionRefAlt> recs = new ArrayList<>(positions.keySet());
		recs.sort(null);
		
		Map<String, AtomicInteger> scoreDist = new HashMap<>();
		
		logger.info("writing output");
		
		int gsOnlyCount = 0;
		int gsAndAllOnlyCount = 0;
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(outputFileName)))) {
			/*
			 * put input files along with their positions into the file header
			 */
			int j = 1;
			for (String s : new String[]{"Illumina","Pleasance","TGEN","GSC"}) {
				ps.println("##" + j++ + ": gold standard file: " + goldStandard + ", data from " + s);
			}
			for (String s : vcfFiles) {
				ps.println("##" + j++ + ": vcf file: " + s);
			}
			
			
			String header = "#chr\tposition\tref\talt";
			for (int i = 1 ; i <= 4 + vcfFiles.length ; i++) {
				header += "\tGT:" + i;
			}
			for (int i = 1 ; i <= 4 + vcfFiles.length ; i++) {
				header += "\tAC:" + i;
			}
			if (null != allFiles) {
				for (int i = 1 ; i <= allFiles.length ; i++) {
					header += "\tALL-AC:" + i;
				}
			}
			header += "\tScore";
			
			ps.println(header);
			for (ChrPositionRefAlt cp : recs) {
				Pair<String[], String[]> p = positions.get(cp);
				Pair<String[], String[]> allP = missingPositionsFromAll.get(cp);
				String[] missingACs = null != allP ? allP.getRight() : null;
				if (null != allFiles && null == missingACs || missingACs.length == 0) {
					missingACs = new String[allFiles.length];
					Arrays.fill(missingACs, ".");
				}
				int score = getScore(p.getLeft());
				boolean gsOnly = goldStandardOnly(p.getLeft(), 4);
				if (gsOnly) {
					gsOnlyCount++;
					if (null != allP) {
						gsAndAllOnlyCount++;
					}
				}
				String scoreS = score + "/" + (4 + vcfFiles.length);
				scoreDist.computeIfAbsent(scoreS, v -> new AtomicInteger()).incrementAndGet();
				ps.println(cp.getChromosome() + Constants.TAB + cp.getStartPosition() + Constants.TAB +cp.getName() + Constants.TAB 
						+ cp.getAlt() + Constants.TAB + Arrays.stream(p.getLeft()).map(s -> null == s ? "./." : s).collect(Collectors.joining(Constants.TAB_STRING))
						+ Constants.TAB +  Arrays.stream(p.getRight()).map(s -> null == s ? "." : s).collect(Collectors.joining(Constants.TAB_STRING))
						+ Constants.TAB +  Arrays.stream(missingACs).map(s -> null == s ? "." : s).collect(Collectors.joining(Constants.TAB_STRING))
						+ Constants.TAB + scoreS);
			}
		}
		logger.info("writing output- DONE");
		
		scoreDist.entrySet().stream().sorted((e1,e2) -> Integer.compare(e1.getValue().get() , e2.getValue().get()))
		.forEach(e -> logger.info("score: " + e.getKey() + ", count: " + e.getValue().get()));
		logger.info("Number of records that are in the Gold Standard only: " + gsOnlyCount + ", of which " + gsAndAllOnlyCount + " were in the all files");
	}
	
	static int getScore(String [] gts) {
		if (null != gts && gts.length > 0) {
			Map<String, List<String>> map = Arrays.stream(gts).filter(s ->  ! StringUtils.isNullOrEmptyOrMissingData(s) && ! "./.".equals(s)).collect(Collectors.groupingBy(s -> s));
			if (null != map && ! map.isEmpty()) {
				Optional<Map.Entry<String, List<String>>> maxValue = map.entrySet().stream().max((e1, e2) -> Integer.compare(e1.getValue().size() , e2.getValue().size()));
				
				AtomicInteger max = new AtomicInteger();
				maxValue.ifPresent(c -> max.set(c.getValue().size()));
				
				return max.get();
			}
		}
		return 0;
	}
	
	static boolean goldStandardOnly(String [] gts, int numberOfGSEntries) {
		/*
		 * need to check that the first <numberOfGSEntries> of entries in the array are non-zero, and that all the other entries are zero
		 */
		boolean gsOnly = true;
		for (int i = 0, len = gts.length ; i < len ; i++) {
			String s = gts[i];
			if (i < numberOfGSEntries) {
				// non-empty
				if (StringUtils.isNullOrEmptyOrMissingData(s) || "./.".equals(s)) {
					gsOnly = false;
				}
			} else {
				if ( ! StringUtils.isNullOrEmptyOrMissingData(s) && ! "./.".equals(s)) {
					gsOnly = false;
				}
			}
		}
		return gsOnly;
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
										
										final int position = z;
										ChrPositionRefAlt cpn  = new ChrPositionRefAlt(rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(position)+"", alt.charAt(position) + "");
										Pair<String[], String[]> pair = positions.computeIfAbsent(cpn, v -> Pair.of(new String[4 + fileCount], new String[4 + fileCount]));
										
										/*
										 * most likely don't have GT and AC info for cs
										 */
										pair.getLeft()[4 + index] =  "C/S";
										pair.getRight()[4 + index] =  "C,S,0";
									}
								} else {
									ChrPositionRefAlt cpn  = new ChrPositionRefAlt(rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt);
									Pair<String[], String[]> pair = positions.computeIfAbsent(cpn, v -> Pair.of(new String[4 + fileCount], new String[4 + fileCount]));
									updateGTsAndACs(4 + index, rec, recordSomatic, ref, alt, pair);
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
	
	static List<ChrPosition> getMissingPositions(Map<ChrPositionRefAlt, Pair<String[],String[]>> positions, int fileIndex) {
//		List<ChrPosition> missingPositions = new ArrayList<>();
		List<ChrPosition> missingPositions = positions.entrySet().stream()
				.filter(e -> StringUtils.isNullOrEmptyOrMissingData(e.getValue().getLeft()[fileIndex]) || "./.".equals(e.getValue().getLeft()[fileIndex]))
				.map(e -> new ChrPointPosition(e.getKey().getChromosome(), e.getKey().getStartPosition())).collect(Collectors.toList());
		
		if (null != missingPositions) {
			return missingPositions;
		}
		return Collections.emptyList();
	}
	
	private void loadAlls() throws IOException {
		int i = 0;
		int fileCount = allFiles.length;
		int index = 0;
		int somaticCount = 0;
		int foundPosition = 0;
		for (String s : allFiles) {
			
			Set<ChrPosition> missingPositions = new HashSet<>(getMissingPositions(positions, 4 + index));
			logger.info("will try and retireve " + missingPositions.size() + " missing positions from file " + s);
			
			try (VCFFileReader reader = new VCFFileReader(new File(s))) {
				for (VcfRecord rec : reader) {
					if (++ i % 1000000 == 0) {
						logger.info("hit " + i + " entries");
					}
					if (missingPositions.contains(rec.getChrPosition())) {
						foundPosition++;
						boolean recordSomatic = isRecordSomatic(rec);
						
						if (VcfUtils.isRecordASnpOrMnp(rec)) {
							
							String ref = rec.getRef();
							String alt = rec.getAlt();
							
							if (ref.length() > 1) {
								/*
								 * split cs into constituent snps
								 */
								for (int z = 0 ; z < ref.length() ; z++) {
									
									final int position = z;
									ChrPositionRefAlt cpn  = new ChrPositionRefAlt(rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(position)+"", alt.charAt(position) + "");
									Pair<String[], String[]> pair = missingPositionsFromAll.computeIfAbsent(cpn, v -> Pair.of(new String[fileCount], new String[fileCount]));
									
									/*
									 * most likely don't have GT and AC info for cs
									 */
									pair.getLeft()[index] =  "C/S";
									pair.getRight()[index] =  "C,S,0";
								}
							} else {
								ChrPositionRefAlt cpn  = new ChrPositionRefAlt(rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt);
								Pair<String[], String[]> pair = missingPositionsFromAll.computeIfAbsent(cpn, v -> Pair.of(new String[fileCount], new String[fileCount]));
								updateGTsAndACs(index, rec, recordSomatic, ref, alt, pair, true);
							}
						}
					}
					
				}
			}
			logger.info("input: " + (index+1) + " has " + i + " entries");
			logger.info("Found " + foundPosition + " positions out of " + missingPositions.size() + " missing positions");
			i = 0;
			foundPosition = 0;
			index++;
		}
	}
	static void updateGTsAndACs(int index, VcfRecord rec, boolean recordSomatic, String ref, String alt, Pair<String[], String[]> pair) {
		updateGTsAndACs( index,  rec,  recordSomatic,  ref,  alt, pair, false);
	}

	static void updateGTsAndACs(int index, VcfRecord rec, boolean recordSomatic, String ref, String alt, Pair<String[], String[]> pair, boolean showPrefix) {
		List<String> ffList = rec.getFormatFields();
		/*
		 * get position of GT from first entry, and then get second (germline and qsnp)
		 */
		String [] formatHeaders = TabTokenizer.tokenize(ffList.get(0), Constants.COLON);
		int gtPosition = getPositionFromHeader(formatHeaders, VcfHeaderUtils.FORMAT_GENOTYPE);
		int adPosition = getPositionFromHeader(formatHeaders, VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		int oabsPosition = getPositionFromHeader(formatHeaders, VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		int adAllPosition = -1;
		if (oabsPosition == -1) {
			adAllPosition = getPositionFromHeader(formatHeaders, "ADALL");
		}
		
		/*
		 * If list contains 2 elements, set position to 1, otherwise 
		 * if germline, get from second entry in list, for somatic, get third
		 */
		int position = ffList.size() == 2 ? 1 : recordSomatic ? 2 : 1;
		String [] params = TabTokenizer.tokenize(ffList.get(position), Constants.COLON); 
		pair.getLeft()[index] =  getStringFromArray(params, gtPosition);
		String somGerPrefix = recordSomatic ? "S" : "G";
		
		if (adPosition > -1) {
			pair.getRight()[index] = (showPrefix ? somGerPrefix : "") +  getStringFromArray(params, adPosition);
		} else {
		
			/*
			 * get allele dist next
			 */
			if (oabsPosition > -1) {
				String oabs = getStringFromArray(params, oabsPosition);
				Map<String, Integer> alleleDist = VcfUtils.getAllelicCoverageFromAC(oabs);
				pair.getRight()[index] = (showPrefix ? somGerPrefix : "") +  getAllelicDistFromMap(alleleDist, ref, alt);
			} else if (adAllPosition > -1) {
				pair.getRight()[index] = "ADALL:" + getStringFromArray(params, adAllPosition);
			}
		}
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
		//loads all classes in referenced jars into memory to avoid nightly build shenanegans
		LoadReferencedClasses.loadClasses(AmalgamatorGS.class);
		
		AmalgamatorGS qp = new AmalgamatorGS();
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
			version = AmalgamatorGS.class.getPackage().getImplementationVersion();
			if (null == version) {	version = "local"; }
			logger = QLoggerFactory.getLogger(AmalgamatorGS.class, logFile, options.getLogLevel());
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
			allFiles = options.getAlls();
			
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
