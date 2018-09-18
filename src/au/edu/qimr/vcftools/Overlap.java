package au.edu.qimr.vcftools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
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


public class Overlap {
	
	
	public static final String DOUBLE_ZERO = "0/0";
	
	private static QLogger logger;
	private QExec exec;
	private String[] vcfFiles;
	private String[] allFiles;
	private String outputDirectory;
	private String summaryFile;
	private String version;
	private String logFile;
	private String goldStandard;
	private boolean vcfOutput;
	private int exitStatus;
	private boolean somatic;
	private boolean germline;
	
	private final Map<ChrPositionRefAlt, Pair<float[], int[]>> positions = new HashMap<>(1024 * 64);
//	private final Map<ChrPositionRefAlt, float[]> positions = new HashMap<>(1024 * 64);
//	private final Map<ChrPositionRefAlt, List<String>> positions = new HashMap<>(1024 * 64);
	
//	public static class NameAlleleDist {
//		List<String> files;
//		float[] alleleDists;
//		public NameAlleleDist(int numberOfFiles) {
//			files = new ArrayList<>(numberOfFiles);
//			alleleDists = new float[numberOfFiles];
//		}
//		public void addFile(String fileName, int index) {
//			files.add(fileName);
//		}
//		public void addAlleleDist(float ad, int index) {
//			alleleDists[index] = ad;
//		}
// 	}
	
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
			int fc = getFileCount();
			Files.lines(p, Charset.defaultCharset()).filter(s -> ! s.startsWith("#"))
					.map(s -> TabTokenizer.tokenize(s))
					.filter(arr -> arr[2].length() == 1 && arr[3].length() == 1)
					.forEach(arr -> {
						addToMap(positions, arr[0],Integer.parseInt(arr[1].replaceAll(",", "")),Integer.parseInt(arr[1].replaceAll(",", "")), arr[2], arr[3], fc, fc, Float.MAX_VALUE, Integer.MAX_VALUE);
					});
			
		}
	}
	
	private int getFileCount() {
		return vcfFiles.length + (null != goldStandard ? 1 : 0);
	}
	
	public static String getFilesFromFloatArray(float[] array, String[] fileNames) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (float f : array) {
			if (f > 0) {
				StringUtils.updateStringBuilder(sb, fileNames[i], Constants.TAB);
			}
			i++;
		}
		
		return sb.toString();
	}
	
	private void outputStats() {
		/*
		 * comparing 2 inputs - could be 2 vcfs, could be 1 vcf and the gold standard
		 */
		
		final int totalVariants = positions.size();
		
		Map<String, List<ChrPositionRefAlt>> positionsByInput = new HashMap<>();
		String[] inputFiles = new String[getFileCount()];
		int i = 0;
		for (String s : vcfFiles) {
			inputFiles[i++] = s;
		}
		if (null != goldStandard) {
			inputFiles[i++] = goldStandard;
		}
		
		positions.forEach((k,v) -> {
			String files = getFilesFromFloatArray(v.getLeft(), inputFiles);
			positionsByInput.computeIfAbsent(files, f -> new ArrayList<>()).add(k);
		});
		
		logger.info("positionsByInput.size(): " + positionsByInput.size());
		positionsByInput.keySet().forEach(s -> logger.info("s: " + s));
		
		List<ChrPositionRefAlt> emptyStringList = positionsByInput.get("");
		if (null != emptyStringList) {
			logger.info("number of entries in emptyStringList: " + emptyStringList.size());
		}
//		emptyStringList.forEach(s -> logger.info("emptyStringList: " + s));
		
		StringBuilder filesBeingCompared = new StringBuilder();
		
		StringBuilder sb = new StringBuilder();
		/*
		 * add inputs to sb
		 */
		sb.append(Arrays.stream(vcfFiles).collect(Collectors.joining(" and ")));
		if ( null != goldStandard) {
			sb.append(" and ").append(goldStandard);
		}
		
		Comparator<Entry<String,  List<ChrPositionRefAlt>>> comp =Comparator.comparingInt(e -> e.getValue().size());
		positionsByInput.entrySet().stream().sorted(Collections.reverseOrder(comp)).forEach((e) -> {
			int size = e.getValue().size();
			String files = e.getKey();
			float [] aveAlleleDists = getAverageFloatValue(e.getValue(), positions, inputFiles.length);
			String ccmString = getCCMDistAsString(getCCMDistribution(e.getValue(), positions, inputFiles.length));
			for (float f : aveAlleleDists) {
				logger.info("ave allele dist: " + f);
			}
			double perc = 100.0 * size / totalVariants;
			if (files.contains(Constants.TAB_STRING)) {
				filesBeingCompared.append(files);
				sb.append(". In both: ").append(size).append(" (").append(String.format("%.2f", perc)).append("%), average allele dist (file1): ").append(aveAlleleDists[0]).append(", average allele dist (file2): ").append(aveAlleleDists[1]).append(", ").append(ccmString);
			} else {
				int position = StringUtils.getPositionOfStringInArray(inputFiles, files, false);
				if (position > -1) {
					String sPos = "";
//					if (position < 0) {
//						if (files.equals(goldStandard)) {
//							sPos = "gold standard";
//						}
//					} else {
					sPos = "file " + (position + 1);
//					}
					int count = 0;
					if (position > 0 && allFiles != null && allFiles.length > 0) {
						try {
							count = getCountInAllFile(e.getValue(), allFiles[0]);
							logger.info("found " + count + " in all file");
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					
					sb.append(". In " + sPos + " only: ").append(size).append(" (").append(String.format("%.2f", perc)).append("%), (in all file: " + count).append("), average allele dist: ").append(aveAlleleDists[position]).append(", ").append(ccmString);
				}
			}
			logger.info("files: " + files + " have " +size + " positions (" +String.format("%.2f", perc)+"%)");
			/*
			 * output entries that belong to a single file
			 */
			if ( ! files.contains(Constants.TAB_STRING)) {
				try {
					String name =  new File(files).getName();
					if (null != outputDirectory) {
						writeOutput(e.getValue(), outputDirectory + "/" + UUID.randomUUID().toString() + ".vcf", "##key=Unique to " + name + " in " + filesBeingCompared.toString().replace("\t", "  vs  ") + " comparison");
					}
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		logger.info("summary string: " + sb.toString());
		if ( ! StringUtils.isNullOrEmpty(summaryFile)) {
			try {
				writeSummaryLineToFile(sb.toString(), summaryFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	public static int getCountInAllFile(List<ChrPositionRefAlt> positions, String allFile) throws IOException {
		if (null != allFile && null != positions && 0 != positions.size()) {
			
			Set<ChrPositionRefAlt> set = new HashSet<>(positions);
			int i = 0;
			int counter = 0;
			try (VCFFileReader reader = new VCFFileReader(new File(allFile))) {
				for (VcfRecord rec : reader) {
					if (++ i % 1000000 == 0) {
						logger.info("hit " + i + " entries");
					}
					
					ChrPositionRefAlt cpra = new ChrPositionRefAlt(rec.getChromosome(), rec.getPosition(),rec.getPosition(), rec.getRef(), rec.getAlt());
					if (set.contains(cpra)) {
						counter++;
					}
				}
			}
			return counter;
		}
		return -1;
	}
	
	public static float [] getAverageFloatValue(List<ChrPositionRefAlt> list,  Map<ChrPositionRefAlt, Pair<float[], int[]>> map, int fileCount) {
		
		float [][] arrays = new float[fileCount][];
		for (ChrPositionRefAlt cpra : list) {
			Pair<float[], int[]> p = map.get(cpra);
			if (null != p) {
				float [] allelDists = p.getLeft();
				if (null != allelDists) {
					
					int i = 0;
					for (float f : allelDists) {
						if (null == arrays[i]) {
							arrays[i] = new float[2];
						}
						if (f > 0 && f < Float.MAX_VALUE) {
							arrays[i][0] += f;
							arrays[i][1] += 1;
						}
						i++;
					}
				}
			}
		}

		float[] results = new float[fileCount];
		int k = 0;
		for (float[] array : arrays) {
			results[k++] = array[0] / array[1];
		}
		
		return results;
	}
	
	public static int [][] getCCMDistribution(List<ChrPositionRefAlt> list,  Map<ChrPositionRefAlt, Pair<float[], int[]>> map, int fileCount) {
		
		int [][] arrays = new int[fileCount][];
		for (ChrPositionRefAlt cpra : list) {
			Pair<float[], int[]> p = map.get(cpra);
			if (null != p) {
				int [] ccms = p.getRight();
				if (null != ccms) {
					
					int i = 0;
					for (int s : ccms) {
						if (null == arrays[i]) {
							arrays[i] = new int[100];
						}
						if (s >= 0 && s < Integer.MAX_VALUE) {
							arrays[i][s] += 1;
						}
						i++;
					}
				}
			}
		}

		return arrays;
	}
	
	public static String getCCMDistAsString(int[][] ccms) {
		if (null != ccms) {
			StringBuilder sb = new StringBuilder();
			int i = 1;
			for (int[] array : ccms) {
				StringUtils.updateStringBuilder(sb, "ccm dist for file " + i++ + " [", Constants.COMMA);
				boolean firstUpdate = true;
				for (int j = 0, len = array.length ; j < len ; j++) {
					if (array[j] > 0) {
						if ( ! firstUpdate) {
							sb.append(Constants.COMMA);
						} else {
							firstUpdate = false;
						}
						sb.append(j).append(Constants.COLON).append(array[j]);
					}
				}
				sb.append(']');
			}
			if (sb.length() > 0) {
				return sb.toString();
			}
		}
		return null;
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
	
	private void writeOutput(List<ChrPositionRefAlt> recs, String output, String extraHeaderInfo) throws FileNotFoundException {
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
			if (null != extraHeaderInfo) {
				ps.println(extraHeaderInfo);
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
		int fileCount = vcfFiles.length + (null != goldStandard ? 1 : 0);
		for (String s : vcfFiles) {
			
			try (VCFFileReader reader = new VCFFileReader(new File(s))) {
				for (VcfRecord rec : reader) {
					if (++ i % 1000000 == 0) {
						logger.info("hit " + i + " entries");
					}
					
					processVcfRecord(rec, somatic, germline, positions, index, fileCount);
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
	public static void processVcfRecord(VcfRecord rec, boolean somatic, boolean germline, Map<ChrPositionRefAlt, Pair<float[], int[]>> map, int input, int fileCount) {
//		public static void processVcfRecord(VcfRecord rec, boolean somatic, boolean germline, Map<ChrPositionRefAlt, float[]> map, int input, int fileCount) {
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
				 * ccm of 13 is when we have a somatic
				 */
//				int ccm = getCCM(rec);
//				if (ccm == 13 && germline) {
//					return;
//				}
				
				
				/*
				 * only deal with snps and compounds for now
				 */
				if (VcfUtils.isRecordASnpOrMnp(rec)) {
					
					
					
//					String gt = GoldStandardGenerator.getGT(rec, recordSomatic);
					
					String ref = rec.getRef();
					String alt = rec.getAlt();
					int ccm = getCCM(rec);
					
					if (ref.length() > 1) {
						/*
						 * split cs into constituent snps
						 */
						for (int z = 0 ; z < ref.length() ; z++) {
							addToMap(map, rec.getChrPosition().getChromosome(),  rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(z)+"",  alt.charAt(z)+"", input,fileCount, Float.MAX_VALUE, ccm);
//							addToMap(map, rec.getChrPosition().getChromosome(),  rec.getChrPosition().getStartPosition() + z, rec.getChrPosition().getStartPosition() + z, ref.charAt(z)+"",  alt.charAt(z) +Constants.TAB_STRING+ gt, input);
						}
					} else {
						addToMap(map, rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt, input,fileCount, getAlleleRatio(rec), ccm);
//						addToMap(map, rec.getChrPosition().getChromosome(), rec.getChrPosition().getStartPosition(), rec.getChrPosition().getStartPosition(), ref, alt+Constants.TAB+gt, input);
					}
				}
			}
		}
	}
	
	/**
	 * Returns Float.MAX_VALUE if there is no OABS
	 * Otherwise, returns the number of times the alt allesles were seen divided by the sum of the number of occurrences of the ale alleles and the ref allele
	 * 
	 * If somatic, looks at the array position 1, 0 otherwise.
	 *
	 */
	public static float getAlleleRatio(VcfRecord v) {
		Map<String, String[]> ffMap = v.getFormatFieldsAsMap();
		if (null != ffMap && ! ffMap.isEmpty()) {
			String[] oabsArray = ffMap.get(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND);
			boolean recordSomatic = Amalgamator.isRecordSomatic(v);
			int position = recordSomatic ? 1 : 0;
			if (null != oabsArray && oabsArray.length > 0) {
				return getAlleleRatioFromOABSorAC(oabsArray, position, v.getRef(), v.getAlt(), (s -> VcfUtils.getAllelicCoverage(s)));
			} else {
				/*
				 * need to deal with vcf records that don't have an OABS - instead look for AC
				 */
				String[] acArray = ffMap.get(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
				if (null != acArray && acArray.length > 0) {
					return getAlleleRatioFromOABSorAC(acArray, position, v.getRef(), v.getAlt(), (s -> VcfUtils.getAllelicCoverageFromAC(s)));
				} else {
					System.out.println("Coulnd't get allele dist from vcf record - no OABS or AC in format field");
				}
			}
		}
		return Float.MAX_VALUE;
	}
	
	public static int getCCM(VcfRecord v) {
		Map<String, String[]> ffMap = v.getFormatFieldsAsMap();
		if (null != ffMap && ! ffMap.isEmpty()) {
			String[] ccmArray = ffMap.get(VcfHeaderUtils.FORMAT_CCM);
			boolean recordSomatic = Amalgamator.isRecordSomatic(v);
			int position = recordSomatic ? 1 : 0;
			if (null != ccmArray && ccmArray.length > 0) {
				String ccm = getFirstStringIfDelimiterPresent(ccmArray[position], Constants.VCF_MERGE_DELIM);
//				if ("13".equals(ccm)) {
//					System.out.println("germline??? " + v.toString());
//				}
				return Integer.parseInt(ccm);
			} else {
				/*
				 * old vcf files will not have this - can get it from the CCM mode of qannotate
				 * just need the control and test GT (minus any delimiters naturally...)
				 */
				String[] gtArray = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
				if (null != gtArray && gtArray.length >= 2) {
					String gtControl = getFirstStringIfDelimiterPresent(gtArray[0], Constants.VCF_MERGE_DELIM);
					String gtTest = getFirstStringIfDelimiterPresent(gtArray[1], Constants.VCF_MERGE_DELIM);
					return getCCM(gtControl, gtTest);
				}
			}
		}
		return -1;
	}
	
	public static String getFirstStringIfDelimiterPresent(String data, char delim) {
		int index = data.indexOf(delim);
		if (index > -1) {
			/*
			 * pick first one
			 */
			return data.substring(0, index);
		}
		return data;
	}
	
	public static float getAlleleRatioFromOABSorAC(String [] oabsOrAcArray, int position, String ref, String alt, Function<String, Map<String, Integer>> f) {
		
		if (position < oabsOrAcArray.length) {
			String ac = oabsOrAcArray[position];
			int delimiterIndex = ac.indexOf(Constants.VCF_MERGE_DELIM);
			if (delimiterIndex > 0) {
				ac = ac.substring(0, delimiterIndex);
			}
			
			Map<String, Integer> map = f.apply(ac);
			return getAlleleRatioFromMap(map, ref, alt);
		}
		return 0f;
	}
	
	public static float getAlleleRatioFromMap(Map<String, Integer> map, String ref, String altString) {
		String [] alts = TabTokenizer.tokenize(altString, Constants.COMMA);
		
		int altValue = 0;
		for (String alt : alts) {
			Integer i = map.get(alt);
			if (null != i) {
				altValue += i;
			}
		}
//		System.out.println("returning allele ratio of: " + (float) altValue / (altValue + refValue));
		return (float) altValue / (altValue + map.getOrDefault(ref, 0));
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
	
	public static final int getCCM(String cGT, String tGT) {
		/*
		 * deal with instances where we have single dot rather than double dot
		 */
		if (Constants.MISSING_DATA_STRING.equals(cGT)) {
			cGT = Constants.MISSING_GT;
		}
		if (Constants.MISSING_DATA_STRING.equals(tGT)) {
			tGT = Constants.MISSING_GT;
		}
		
		if (Constants.MISSING_GT.equals(cGT)) {
			if (Constants.MISSING_GT.equals(tGT)) 						return 1;
			if (DOUBLE_ZERO.equals(tGT)) 						return 2;
			int[] testAlleles = getAlleles(tGT);
			if (testAlleles[0] == 0 || testAlleles[1] == 0)	return 3;
			if (testAlleles[0] == testAlleles[1] ) 				return 4;
			return 5;
		}
			
		if (DOUBLE_ZERO.equals(cGT)) {
			if (Constants.MISSING_GT.equals(tGT)) 						return 11;
			if (DOUBLE_ZERO.equals(tGT)) 						return 12;
			int[] testAlleles = getAlleles(tGT);
			if (testAlleles[0] == 0 || testAlleles[1] == 0)	return 13;
			if (testAlleles[0] == testAlleles[1] ) 				return 14;
			return 15;
		}
		
		int[] controlAlleles = getAlleles(cGT);
		if (controlAlleles[0] == 0 || controlAlleles[1] == 0) {
			if (Constants.MISSING_GT.equals(tGT)) 						return 21;
			if (DOUBLE_ZERO.equals(tGT)) 						return 22;
			int[] testAlleles = getAlleles(tGT);
			if (testAlleles[0] == 0 || testAlleles[1] == 0) {
				if (testAlleles[0] != 0 && isIntInArray(testAlleles[0], controlAlleles)
						|| testAlleles[1] != 0 && isIntInArray(testAlleles[1], controlAlleles)) return 23;
				return 26;
			}
			if (testAlleles[0] == testAlleles[1]) {
				if (isIntInArray(testAlleles[0], controlAlleles)) return 24;
				return 27;
			} else {
				if (isIntInArray(testAlleles[0], controlAlleles) 
						|| isIntInArray(testAlleles[1], controlAlleles)) 		return 25;
				return 28;
			}
		}
		
		if (controlAlleles[0] == controlAlleles[1]) {
			if (Constants.MISSING_GT.equals(tGT)) 						return 31;
			if (DOUBLE_ZERO.equals(tGT)) 						return 32;
			int[] testAlleles = getAlleles(tGT);
			if (testAlleles[0] == 0 || testAlleles[1] == 0) {
				if (testAlleles[0] != 0 && isIntInArray(testAlleles[0], controlAlleles)
						|| testAlleles[1] != 0 && isIntInArray(testAlleles[1], controlAlleles)) return 33;
				return 38;
			}
			if (testAlleles[0] == testAlleles[1]) {
				if (testAlleles[0] == controlAlleles[0])	return 34;
				return 37;
			}
			if (isIntInArray(testAlleles[0], controlAlleles) 
					|| isIntInArray(testAlleles[1], controlAlleles)) return 35;
			return 36;
		}
		
		/*
		 * control is x/y by this point
		 */
		if (Constants.MISSING_GT.equals(tGT)) 						return 41;
		if (DOUBLE_ZERO.equals(tGT)) 						return 42;
		int[] testAlleles = getAlleles(tGT);
		if (testAlleles[0] == 0 || testAlleles[1] == 0) {
			if ((testAlleles[0] != 0 && testAlleles[0] == controlAlleles[0])
					|| (testAlleles[1] != 0 && testAlleles[1] == controlAlleles[0])) return 43;
			if ((testAlleles[0] != 0 && testAlleles[0] == controlAlleles[1])
					|| (testAlleles[1] != 0 && testAlleles[1] == controlAlleles[1])) return 44;
			return 49;
		}
		if (testAlleles[0] == testAlleles[1]) {
			if (testAlleles[0] == controlAlleles[0])	return 45;
			if (testAlleles[0] == controlAlleles[1])	return 46;
			return 50;		// update with correct number once known
		}
		if (cGT.equals(tGT)) return 47;
		if ( ! isIntInArray(testAlleles[0], controlAlleles) &&  ! isIntInArray(testAlleles[1], controlAlleles)) return 48;
		if ((isIntInArray(testAlleles[0], controlAlleles) &&  ! isIntInArray(testAlleles[1], controlAlleles))
				|| ( ! isIntInArray(testAlleles[0], controlAlleles) &&  isIntInArray(testAlleles[1], controlAlleles))) return 51;
		
		logger.warn("Found a 99er! cGT: " + cGT + ", tGT: " + tGT);
		return 99;
			
	}
	public static boolean isIntInArray(int i, int[] array) {
		for (int j : array) {
			if (i == j) return true;
		}
		return false;
	}
	public static int[] getAlleles(String gt) {
		if (StringUtils.isNullOrEmpty(gt)) {
			throw new IllegalArgumentException("Null or empty gt passed to CCMMode.getAlleles()");
		}
		
		int i = gt.indexOf(Constants.SLASH);
		if (i == -1) {
			throw new IllegalArgumentException("Null or empty gt passed to CCMMode.getAlleles()");
		}
		int a1 = Integer.parseInt(gt.substring(0, i));
		int a2 = Integer.parseInt(gt.substring( i + 1));
		return new int[]{a1,a2};
	}
	
	public static void addToMap(Map<ChrPositionRefAlt, Pair<float[], int[]>> map, String chr, int start, int end, String ref, String alt, int input, int fileCount, float alleleDist, int ccm) {
		if (null != map && null != chr && start >=0 && end >= start) {
			ChrPositionRefAlt cpn  = new ChrPositionRefAlt(chr, start, end, ref, alt);
			Pair<float[], int[]> p = map.computeIfAbsent(cpn, v -> Pair.of(new float[fileCount], new int[fileCount]));
			p.getLeft()[input] = alleleDist;
			p.getRight()[input] = ccm;
		}
	}
//	public static void addToMap(Map<ChrPositionRefAlt, float[]> map, String chr, int start, int end, String ref, String alt, int input, int fileCount, float alleleDist) {
//		if (null != map && null != chr && start >=0 && end >= start) {
//			ChrPositionRefAlt cpn  = new ChrPositionRefAlt(chr, start, end, ref, alt);
//			map.computeIfAbsent(cpn, v -> new float[fileCount])[input] = alleleDist;
//		}
//	}
//	public static void addToMap(Map<ChrPositionRefAlt, List<String>> map, String chr, int start, int end, String ref, String alt, String input) {
//		if (null != map && null != chr && start >=0 && end >= start) {
//			ChrPositionRefAlt cpn  = new ChrPositionRefAlt(chr, start, end, ref, alt);
//			map.computeIfAbsent(cpn, v -> new ArrayList<String>(4)).add(input);
//		}
//	}
	
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
			allFiles = options.getAlls();
			
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
			options.getSummaryFile().ifPresent(s -> summaryFile = s);
			
			return engage();
		}
		return returnStatus;
	}

}
