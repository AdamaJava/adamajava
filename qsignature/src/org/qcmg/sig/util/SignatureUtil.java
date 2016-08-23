/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.util;

import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntShortHashMap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.util.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.ChrPositionCache;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.illumina.IlluminaRecord;
import org.qcmg.sig.model.SigMeta;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public class SignatureUtil {
	
	/*
	 * CONSTANTS
	 */
	public final static String QSIG_REPORT = ".txt.qsig.report.txt";
	public static final String QSIG_VCF_GZ = ".qsig.vcf.gz";
	public static final String QSIG_VCF = ".qsig.vcf";
	public static final String BAM_QSIG_VCF = ".bam.qsig.vcf";
	
	public static final String SHORT_CUT_EMPTY_COVERAGE = "L:0;N";		// from ...TOTAL:0;NOVELCOV=A...
	public static final String EMPTY_COVERAGE = "FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
	public static final String EMPTY_NOVEL_STARTS = ";NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
	public static final String PATIENT_REGEX = "[A-Z]{4}_[0-9]{4}";
	public static final String SAMPLE_REGEX = "[A-Z]{4}_[A-Z]{4}_[0-9]{8}_[0-9]{2}_[A-Z]{2}";
	public static final String TYPE_REGEX = "_[A-Z]{2}_";
	private static final char[] BASES = new char[] {'A', 'C', 'G', 'T', 'N'};
	
	public static final String SNP_ARRAY = "SNP_array";
	public static final String UNKNOWN = "UNKNOWN";
	
	public static final NumberFormat nf = new DecimalFormat("0.####");
	public static final QLogger logger = QLoggerFactory.getLogger(SignatureUtil.class);
	
	
	public static final String MD_5_SUM = "##positions_md5sum";
	public static final String POSITIONS_COUNT = "##positions_count";
	public static final String MIN_BASE_QUAL = "##filter_base_quality";
	public static final String MIN_MAPPING_QUAL = "##filter_mapping_quality";
	public static final String RG_PREFIX = "##rg";
	public static final String NAN = "nan";
	
	/*
	 * METHODS
	 */
	
	public static String getCoverageStringFromCharsAndInts(char c1, char c2, int i1, int i2) {
		StringBuilder sb = new StringBuilder("FULLCOV=");
		
		for (char c : BASES) {
			if (c1 == c && c2 == c) {
				sb.append(c).append(Constants.COLON).append(i1 + i2).append(Constants.COMMA);
			} else if (c1 == c) {
				sb.append(c).append(Constants.COLON).append(i1).append(Constants.COMMA);
			} else if (c2 == c) {
				sb.append(c).append(Constants.COLON).append(i2).append(Constants.COMMA);
			} else {
				sb.append(c).append(":0,");
			}
		}
		
		// add total
		sb.append("TOTAL:").append(i1 + i2);
		
		// and finally novel start count
		sb.append(EMPTY_NOVEL_STARTS);
		
		return sb.toString();
	}
	
	public static String getPatientFromFile(File file) {
		String donor = DonorUtils.getDonorFromFilename(file.getAbsolutePath());
		return null != donor ? donor : UNKNOWN;
	}
	
	public static String getPatternFromString(String pattern, String string) {
		if (null == pattern) throw new IllegalArgumentException("null pattern string passed to getPatternFromString");
		if (null == string) throw new IllegalArgumentException("null string passed to getPatternFromString");
		
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(string);
		if (m.find())
			return m.group();
		return UNKNOWN;
	}

	public static Map<File, Map<ChrPosition, double[]>> getDonorSnpChipData(String donor, List<File> files) throws IOException {
		
		Map<File, Map<ChrPosition, double[]>> map = new HashMap<>();
		
		for (File snpFile : files) {
			if (null != snpFile 
					&& snpFile.exists() 
					&& null != snpFile.getAbsolutePath() 
					&& snpFile.getAbsolutePath().contains(donor)) {
				
				Map<ChrPosition, double[]> snpChipRatios = loadSignatureRatios(snpFile);
				map.put(snpFile, snpChipRatios);
			}
		}
		
		return map;
	}
	
	/**
	 * Loads a qsignature vcf file and returns a map containing positions where there is coverage.
	 * Returns null if the supplied file is null, or can't be read
	 *  
	 * @param file qsignature vcf file to read
	 * @return Map of ChrPositions and double[] that contains all the non-empty positions in the supplied qsignature vcf file
	 * 
	 * @throws Exception
	 */
	public static Map<ChrPosition, double[]> loadSignatureRatios(File file) throws IOException {
		return loadSignatureRatios(file, 10);
	}
	
	public static Map<ChrPosition, double[]> loadSignatureRatios(File file, int minCoverage) throws IOException {
		Map<ChrPosition, double[]> ratios = new HashMap<>();
		
		if (null == file) {
			throw new IllegalArgumentException("Null file object passed to loadSignatureRatios");
		}
		
		try (TabbedFileReader reader = new TabbedFileReader(file)) {
			String line;
			
			for (TabbedRecord vcfRecord : reader) {
				line = vcfRecord.getData();
				if (line.startsWith(Constants.HASH_STRING)) continue;
				//do a brute force search for the empty coverage string before passing to tokenizer
				// only populate ratios with non-zero values
				// attempt to keep memory usage down...
				if (line.indexOf(SHORT_CUT_EMPTY_COVERAGE) > -1) continue;
				// ignore entries that have nan in there
				if (line.indexOf(NAN) > -1) continue;
				
				String[] params = TabTokenizer.tokenize(line);
				String coverage = params[7];
				double[] array = getValuesFromCoverageString(coverage, minCoverage);
				if (null != array) {
					ChrPosition chrPos = ChrPositionCache.getChrPosition(params[0], Integer.parseInt(params[1]));
					ratios.put(chrPos, array);
				}
			}
		}
		return ratios;
	}
	
	public static Map<ChrPosition, float[]> loadSignatureRatiosFloat(File file, int minCoverage) throws IOException {
		Map<ChrPosition, float[]> ratios = new HashMap<>();
		
		if (null == file) {
			throw new IllegalArgumentException("Null file object passed to loadSignatureRatios");
		}
		
		try (TabbedFileReader reader = new TabbedFileReader(file)) {
			String line;
			
			for (TabbedRecord vcfRecord : reader) {
				line = vcfRecord.getData();
				if (line.startsWith(Constants.HASH_STRING)) continue;
				//do a brute force search for the empty coverage string before passing to tokenizer
				// only populate ratios with non-zero values
				// attempt to keep memory usage down...
				if (line.indexOf(SHORT_CUT_EMPTY_COVERAGE) > -1) continue;
				// ignore entries that have nan in there
				if (line.indexOf(NAN) > -1) continue;
				
				String[] params = TabTokenizer.tokenize(line);
				String coverage = params[7];
				Optional<float[]> array = getValuesFromCoverageStringFloat(coverage, minCoverage);
				array.ifPresent(f -> {
					ChrPosition chrPos = ChrPositionCache.getChrPosition(params[0], Integer.parseInt(params[1]));
					ratios.put(chrPos, f);
				});
			}
		}
		return ratios;
	}
	public static TIntShortHashMap loadSignatureRatiosFloatGenotype(File file) throws IOException {
		return loadSignatureRatiosFloatGenotype(file, 10);
	}
	public static Map<ChrPosition, float[]> loadSignatureRatiosFloat(File file) throws IOException {
		return loadSignatureRatiosFloat(file, 10);
	}
	
	public static TIntShortHashMap loadSignatureRatiosFloatGenotype(File file, int minCoverage) throws IOException {
		TIntShortHashMap ratios = new TIntShortHashMap();
		
		if (null == file) {
			throw new IllegalArgumentException("Null file object passed to loadSignatureRatios");
		}
		
		try (TabbedFileReader reader = new TabbedFileReader(file)) {
			String line;
			
			for (TabbedRecord vcfRecord : reader) {
				line = vcfRecord.getData();
				if (line.startsWith(Constants.HASH_STRING)) continue;
				//do a brute force search for the empty coverage string before passing to tokenizer
				// only populate ratios with non-zero values
				// attempt to keep memory usage down...
				if (line.indexOf(SHORT_CUT_EMPTY_COVERAGE) > -1) continue;
				// ignore entries that have nan in there
				if (line.indexOf(NAN) > -1) continue;
				
				String[] params = TabTokenizer.tokenize(line);
				String coverage = params[7];
				Optional<float[]> array = getValuesFromCoverageStringFloat(coverage, minCoverage);
				array.ifPresent(f -> {
					
					short g = getCodedGenotype(f);
					
					if (isCodedGenotypeValid(g)) {
						Integer i = ChrPositionCache.getChrPositionIndex(params[0], Integer.parseInt(params[1]));
						ratios.put(i, g);
					}
				});
			}
		}
		return ratios;
	}
	
	public static  Optional<Pair<SigMeta, Map<String, String>>> getSigMetaAndRGsFromHeader(final TabbedHeader h) {
		if (null == h) {
			return Optional.empty();
		} else {
			
			String md = "";
			int count = 0;
			int bq = 0;
			int mq = 0;
			Map<String, String> rgIds = new THashMap<>();
			for (String s : h) {
				if (s.startsWith(SignatureUtil.MD_5_SUM)) {
					md = s.substring(MD_5_SUM.length() + 1);
				} else if (s.startsWith(POSITIONS_COUNT)) {
					count = Integer.parseInt(s.substring(POSITIONS_COUNT.length() + 1));
				} else if (s.startsWith(MIN_BASE_QUAL)) {
					bq = Integer.parseInt(s.substring(MIN_BASE_QUAL.length() + 1));
				} else if (s.startsWith(MIN_MAPPING_QUAL)) {
					mq = Integer.parseInt(s.substring(MIN_MAPPING_QUAL.length() + 1));
				} else if (s.startsWith(RG_PREFIX)) {
					int ci = s.indexOf(Constants.COLON);
					if (ci > -1) {
						rgIds.put(s.substring(2, ci), s.substring(ci + 1));
					}
				}
			}
			
			return Optional.of(new Pair<>(new SigMeta(md, count, bq, mq), rgIds));
		}
	}
	
	public static Pair<SigMeta, TMap<String, TIntShortHashMap>> loadSignatureRatiosBespokeGenotype(File file, int minCoverage) throws IOException {
		if (null == file) {
			throw new IllegalArgumentException("Null file object passed to loadSignatureRatios");
		}
		
		TIntShortHashMap ratios = new TIntShortHashMap();
		TMap<String, TIntShortHashMap> rgRatios = new THashMap<>();
		
		Map<String, String> rgIds = null;
		SigMeta sm = null;
		
		try (TabbedFileReader reader = new TabbedFileReader(file)) {
			TabbedHeader h = reader.getHeader();
			
			Optional<Pair<SigMeta, Map<String, String>>> metaAndRGsO = getSigMetaAndRGsFromHeader(h);
			if (metaAndRGsO.isPresent()) {
				Pair<SigMeta, Map<String, String>> p = metaAndRGsO.get();
				sm = p.getFirst();
				rgIds = p.getSecond();
			}
			
			int noOfRGs = rgIds.size();
			logger.info("Number of rgs for  " + file.getAbsolutePath() + " is " + noOfRGs);
			
			String line;
			
			AtomicInteger cachePosition = new AtomicInteger();
			
			for (TabbedRecord vcfRecord : reader) {
				line = vcfRecord.getData();
				if (line.startsWith(Constants.HASH_STRING)) {
					continue;
				}
				
				String[] params = TabTokenizer.tokenize(line);
				String coverage = params[7];
				
				/*
				 * This should be in the QAF=t:5-0-0-0,rg4:2-0-0-0,rg1:1-0-0-0,rg2:2-0-0-0 format
				 * Need to tease out the pertinent bits
				 */
				int commaIndex = coverage.indexOf(Constants.COMMA);
				String totalCov = coverage.substring(coverage.indexOf(Constants.COLON) + 1, commaIndex);
				
				
				Optional<float[]> array = getValuesFromCoverageStringBespoke(totalCov, minCoverage);
				array.ifPresent(f -> {
					
					short g = getCodedGenotype(f);
					
					if (isCodedGenotypeValid(g)) {
						cachePosition.set(ChrPositionCache.getChrPositionIndex(params[0], Integer.parseInt(params[1])));
						ratios.put(cachePosition.get(), g);
						/*
						 * Get rg data if we have more than 1 rg
						 */
						if (noOfRGs > 1) {
							
							String [] covParams = coverage.substring(commaIndex + 1).split(Constants.COMMA_STRING);
							for (String s : covParams) {
								/*
								 * strip rg id from string
								 */
								int index = s.indexOf(Constants.COLON_STRING);
								String cov = s.substring(index + 1);
								
								Optional<float[]> arr = getValuesFromCoverageStringBespoke(cov, minCoverage);
								arr.ifPresent(f2-> {
									
									short g2 = getCodedGenotype(f2);
									
									if (isCodedGenotypeValid(g2)) {
										String rg = s.substring(0, index);
										TIntShortHashMap r = rgRatios.get(rg);
										if (r == null) {
											r = new TIntShortHashMap();
											rgRatios.put(rg, r);
										}
										r.put(cachePosition.get(), g2);
									}
								});
							}
						}
					}
				});
				
				rgRatios.put("all", ratios);
			}
		}
		return new Pair<>(sm, rgRatios);
	}
	
	/**
	 * Convert float array containing allele percentages for ACGT bases into a short.
	 * 
	 * If over 90% in A, then will return 2000
	 * If over 90% in C, then will return 200
	 * If over 90% in G, then will return 20
	 * If over 90% in T, then will return 2
	 * If het for AC, will return 1100
	 * If het for AG, will return 1010
	 * If het for AT, will return 1001
	 * If het for CG, will return 0110
	 * If het for CT, will return 0101
	 * If het for GT, will return 0011
	 * 
	 * 
	 * The cutoffs are > 0.90000 for homozygous, and > 0.30000 and < 0.70000 for heterozygous
	 * 
	 * 
	 * @param f
	 * @return
	 */
	public static short getCodedGenotype(float[] f) {
		short g1 = 0;
		for (int i = 0 ; i < 4 ; i++) {
			float d = f[i];
			if (d > 0.90000) {	// homozygous
				g1 += i == 0 ? 2000 : i == 1 ? 200 : i == 2 ? 20 : 2;
				break;
			} else if (d > 0.30000 && d < 0.70000) {	// heterozygous
				g1 += i == 0 ? 1000 : i == 1 ? 100 : i == 2 ? 10 : 1;
			}
		}
		return g1;
	}
	
	/**
	 * A short value outwith the 0-2000 range is invalid, and will return false.
	 * Also, if the short contains only a single 1 within this range (ie. 1000), then this is also invalid, and will return false
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isCodedGenotypeValid(short s) {
		switch (s) {
		case 2000 : 
		case 200 :
		case 20 :
		case 2:
		case 1100:
		case 1010:
		case 1001:
		case 110:
		case 101:
		case 11: return true;
		default: return false;
		}
	}
	
	
	/**
	 * Returns the contents of a file as a list of strings
	 * Assumes that each file identified within this file is on a single line
	 * uses java7 nio package
	 * 
	 * @param excludesFile String relating to a filename that contains a list of filesnames
	 * @return List of Strings representing the filenames in the file
	 * @throws IOException
	 */
	public static List<String> getEntriesFromExcludesFile(String excludesFile) throws IOException {
		List<String> excludes = Collections.emptyList();
		if ( ! StringUtils.isNullOrEmpty(excludesFile)) {
			excludes = Files.readAllLines(Paths.get(excludesFile), StandardCharsets.UTF_8);
		}
		return excludes;
	}
	
	/**
	 * Removes from the list of files the filenames specified in the list of strings
	 * 
	 * @param originalList
	 * @param filesToExclude
	 * @return
	 */
	public static List<File> removeExcludedFilesFromList(List<File> originalList, List<String> filesToExclude) {
		
		if (null == filesToExclude || filesToExclude.isEmpty()) return originalList;
		
		List<File> keepers = new ArrayList<>();
		for (File f : originalList) {
			
			boolean includeFile = true;
			String fName = f.getName();
			for (String s : filesToExclude) {
				if (fName.startsWith(s))  {
					includeFile = false;
					break;
				}
			}
			
			if (includeFile) keepers.add(f);
			else logger.info("ignoring " + fName + " as it is in the excludes file");
			
		}
		return keepers;
	}
	
	public static List<File> removeClosedProjectFilesFromList(List<File> originalList, List<String> closedProjects) {
		
		if (null == closedProjects || closedProjects.isEmpty()) return originalList;
		
		List<File> keepers = new ArrayList<>();
		for (File f : originalList) {
			String donor = getPatientFromFile(f);
			if (closedProjects.contains(donor)) {
				logger.info("ignoring " + f.getName() + " as it belongs to a closed project");
			} else {
				keepers.add(f);
			}
		}
		return keepers;
	}
	
	
	/**
	 * Attempts to find some files in the supplied path that match the snpChipSearchSuffix 
	 * and that aren't in the excludes and failedQC collections, 
	 * and that satisfy the additionalSearchStrings criteria...
	 * 
	 * @param path String
	 * @param snpChipSearchSuffix String
	 * @param excludes List<String>
	 * @param failedQC List<String>
	 * @param additionalSearchStrings String[]
	 * @return List<File>
	 * @throws Exception
	 */
	public static List<File> populateSnpChipFilesList(String path, String snpChipSearchSuffix, 
			List<String> excludes, String ... additionalSearchStrings) throws Exception {
		
		final Set<File> uniqueFiles = new HashSet<File>(FileUtils.findFilesEndingWithFilterNIO(path, snpChipSearchSuffix));
		logger.info("No of unique snp chip files: " + uniqueFiles.size());
		
		List<File> orderedSnpChipFiles = new ArrayList<File>();
		for (File f : uniqueFiles) {
			if (f.exists()) {
				if (null != additionalSearchStrings && additionalSearchStrings.length > 0) {
					boolean passesAllAdditionalSearchCriteria = true;
					for (String s : additionalSearchStrings) {
						if ( ! f.getAbsolutePath().contains(s)) passesAllAdditionalSearchCriteria = false;
					}
					if (passesAllAdditionalSearchCriteria) {
						addFileToCollection(orderedSnpChipFiles, excludes, f);
					}
				} else {
					addFileToCollection(orderedSnpChipFiles, excludes, f);
				}
			}
		}
		
		Collections.sort(orderedSnpChipFiles, FileUtils.FILE_COMPARATOR);
		logger.info("No of unique and filtered snp chip files: " + orderedSnpChipFiles.size());
		return orderedSnpChipFiles;
	}
	
	/**
	 * Adds the supplied file to the supplied collection of files assuming that it does not appear in the excludes list or the failedQC list.
	 * 
	 * No checking is in place to see if the file to be added is already in the collection
	 * 
	 * @param collection
	 * @param excludes
	 * @param failedQC
	 * @param f
	 */
	public static void addFileToCollection(List<File> collection, List<String> excludes, File f) {
		if (null == collection || null == f) return;		// don't proceed if collection or file is null
		
		boolean inExcludes = false;
		if (null != excludes) {
			// failedQC collection doesn't include full names, so need to do a partial search
			for (String exclude : excludes) {
				if (f.getName().startsWith(exclude)) {
					inExcludes = true;
					logger.info("ignoring " + f.getName() + " as it is in the excludes file");
					break;
				}
			}
		}
		if ( ! inExcludes) collection.add(f);
	}
	
	
	public static String getCoverageStringForIlluminaRecord(IlluminaRecord illRec, String [] params, int arbitraryCoverage) {
		
		//# get bases that are probed for at this genomic coordinate
		final char snpChar1 = illRec.getSnp().charAt(1);
		final char snpChar2 = illRec.getSnp().charAt(3);
		
		// # should the array genotype be complemented because, according to dbSNP,
        		// # the SNP is on the - strand; 0 = no complement; 1 = complement;
        		// # undefined = not determined so a decision cannot be made
		
		/*
		 *  ## determine genotype according to array
	                # Unambiguous SNPs:
	                # A/C -> SNP=TOP, A=ALLELE A and C=ALLELE B
	                # A/G -> SNP=TOP, A=ALLELE A and G=ALLELE B
	                # T/C -> SNP=BOT, T=ALLELE A and C=ALLELE B
	                # T/G -> SNP=BOT, T=ALLELE A and G=ALLELE B
		 */
		String arrayGenotypoe = "";
		if (BaseUtils.isAT(snpChar1) && BaseUtils.isCG(snpChar2)) {
			if (illRec.getFirstAlleleCall() == 'A') {
				arrayGenotypoe = ""+snpChar1;
				
				if (illRec.getSecondAlleleCall() == 'A') {
					arrayGenotypoe += snpChar1;
				} else if (illRec.getSecondAlleleCall() == 'B') {
					arrayGenotypoe += snpChar2;
				}
			} else if (illRec.getFirstAlleleCall() == 'B') {
				arrayGenotypoe = ""+ snpChar2 + snpChar2;
			}
		}
		/*
		 *  # Ambiguous SNPs:
	                # if SNP is A/T and SNP=TOP, A=ALLELE A and T=ALLELE B
	                # if SNP is A/T and SNP=BOT, T=ALLELE A and A=ALLELE B
	                # if SNP is C/G and SNP=TOP, C=ALLELE A and G=ALLELE B
	                # if SNP is C/G and SNP=BOT, G=ALLELE A and C=ALLELE B

		 */
		else if ((snpChar1 == 'A' && snpChar2 == 'T') 
				|| (snpChar1 == 'C' && snpChar2 == 'G')
				|| (snpChar1 == 'T' && snpChar2 == 'A')
				|| (snpChar1 == 'G' && snpChar2 == 'C')) {
			
			if (illRec.getFirstAlleleCall() == 'A' && "TOP".equalsIgnoreCase(illRec.getStrand())) {
				arrayGenotypoe = ""+snpChar1;
				
				if (illRec.getSecondAlleleCall() == 'A') {
					arrayGenotypoe += snpChar1;
				} else if  (illRec.getSecondAlleleCall() == 'B') {
					arrayGenotypoe += snpChar2;
				}
			} else if (illRec.getFirstAlleleCall() == 'A' && "BOT".equalsIgnoreCase(illRec.getStrand())) {
				arrayGenotypoe = ""+snpChar2;
				if (illRec.getSecondAlleleCall() == 'A') {
					arrayGenotypoe += snpChar2;
				} else if  (illRec.getSecondAlleleCall() == 'B') {
					arrayGenotypoe += snpChar1;
				}
			} else if (illRec.getFirstAlleleCall() == 'B') {
				arrayGenotypoe = ""+ snpChar2 + snpChar2;
			}
		} else {
			//# shouldn't happen, but here as a warning just in case
			logger.warn("Setting arrayGenotype to unhandled case");
			logger.info("first allele call: " + illRec.getFirstAlleleCall() + ", second allele call: " 
					+ illRec.getSecondAlleleCall() 
					+ ", strand: " + illRec.getStrand() 
					+ ", snp char 1: " + snpChar1 
					+ ", snp char 2: " + snpChar2);
			return null;
		}
		
		// # complement genotype if dbSNP says SNP is on - minus strand
		if ("yes".equalsIgnoreCase(params[7])) {
			String complementArrayGenotype = "";
			for (char c : arrayGenotypoe.toCharArray()) {
				complementArrayGenotype += "" + BaseUtils.getComplement(c);
			}
			arrayGenotypoe = complementArrayGenotype;
		}
		
		char base1 = arrayGenotypoe.charAt(0);
		char base2 = arrayGenotypoe.charAt(1);
		
		/*
		 *   # We'll arbitrarily set nominal coverage as 20 but we'll scale
                # it up or down according to logR (assumes it's a natural log)
                my $arbcov      = 20;
                my $totcov      = floor( $arbcov * exp( $rec->{'Log R Ratio'} ) );
                my $alt_count   = floor( $rec->{'B Allele Freq'} * $totcov );
                my $ref_count   = $totcov - $alt_count;
		 */
		int totalCoverage = (int) Math.floor(arbitraryCoverage * Math.exp(illRec.getLogRRatio())); 
		int altCoverage = (int) Math.floor(illRec.getbAlleleFreq() * totalCoverage);
		int refCoverage = totalCoverage - altCoverage;
		
		return getCoverageStringFromCharsAndInts(base1, base2, refCoverage, altCoverage);
		
	}
	
	/**
	 * * Info field must be in the following format:
	 * "FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
	 * 
	 * @param info
	 * @return
	 */
	public static int[] decipherCoverageString(String info) {
		if (StringUtils.isNullOrEmpty(info))
			throw new IllegalArgumentException("Invalid coverage string passed to decipherCoverageString: " + info);
		// strip out the pertinent bits
		// total
		final int totalIndex = info.indexOf("TOTAL:");
		
		if (totalIndex == -1)
			throw new IllegalArgumentException("Invalid coverage string passed to decipherCoverageString: " + info);
		
		final int total = Integer.parseInt(info.substring(totalIndex + 6, info.indexOf("NOVELCOV") - 1));
		
		int aIndex = info.indexOf("A:");
		int cIndex = info.indexOf("C:");
		int gIndex = info.indexOf("G:");
		int tIndex = info.indexOf("T:");
		int nIndex = info.indexOf("N:");
		if (nIndex < 0) {
			// use totalIndex instead
			nIndex = totalIndex;
		}
		
		// all these indicies should be +ve
		if (aIndex < 0 || cIndex < 0 || gIndex < 0 || tIndex < 0 || nIndex < 0) {
			throw new IllegalArgumentException("Invalid coverage string passed to decipherCoverageString: " + info);
		}
		
		
		// all these indicies should be +ve
		if (aIndex < 0 || cIndex < 0 || gIndex < 0 || tIndex < 0 || nIndex < 0) {
			logger.warn("Invalid coverage string: " + info);
		}
		
		int aCount = Integer.parseInt(info.substring(aIndex + 2, cIndex - 1));
		int cCount = Integer.parseInt(info.substring(cIndex + 2, gIndex - 1));
		int gCount = Integer.parseInt(info.substring(gIndex + 2, tIndex - 1));
		int tCount = Integer.parseInt(info.substring(tIndex + 2, nIndex - 1));
		
		if (total != (aCount + cCount + gCount + tCount)) {
			logger.warn("inconsistency in sums: " + info + " (NOTE doesn't take into account N values)");
		}
		
		return new int[] {aCount, cCount, gCount, tCount, total};
	}
	
	/**
	 * info should be in the following format:
	 * "5-0-0-0" which corresponds to count_of_As-count_of_Cs-count_of_Gs-count_of_Ts
	 * 
	 * @param info
	 * @return
	 */
	public static Optional<int[]> decipherCoverageStringBespoke(String info) {
		if (StringUtils.isNullOrEmpty(info))
			throw new IllegalArgumentException("Invalid coverage string passed to decipherCoverageStringBespoke: " + info);
		
		String [] data = TabTokenizer.tokenize(info, Constants.MINUS);
		if (data.length == 4) {
			int[] counts = new int[4];
			
			for (int i = 0 ; i < 4 ; i++) {
				counts[i] =  Integer.parseInt(data[i]);
			}
			return Optional.of(counts);
		}
		return Optional.empty();
	}
	
	public static double[] getValuesFromCoverageString(final String coverage) {
		return getValuesFromCoverageString(coverage, 10);
	}
	
	public static double[] getValuesFromCoverageString(final String coverage, int minimumCoverage) {
		
		int[] baseCoverages = decipherCoverageString(coverage);
		int total = baseCoverages[4];
		if (total < minimumCoverage) return null;
		
		double aFrac = (double) baseCoverages[0] / total;
		double cFrac = (double) baseCoverages[1] / total;
		double gFrac = (double) baseCoverages[2] / total;
		double tFrac = (double) baseCoverages[3] / total;
		
		final double[] array = new double[] {aFrac, 
				cFrac,
				gFrac, 
				tFrac,
				total};
		
		return array;
	}
	public static Optional<float[]> getValuesFromCoverageStringFloat(final String coverage) {
		return getValuesFromCoverageStringFloat(coverage, 10);
	}
	
	public static Optional<float[]> getValuesFromCoverageStringBespoke(final String coverage, int minimumCoverage) {
		
		Optional<int[]> baseCoverages = decipherCoverageStringBespoke(coverage);
		
		final float[] floats = new float[5];
		
		baseCoverages.ifPresent(i -> {
			int total = i[0] + i[1] + i[2] + i[3];
			if (total >= minimumCoverage) {
				floats[0] = (float) i[0] / total; 
				floats[1] = (float) i[1] / total; 
				floats[2] = (float) i[2] / total; 
				floats[3] = (float) i[3] / total; 
				floats[4] = total; 
			}
		});
		return floats[4] > 0 ? Optional.of(floats) : Optional.empty();
	}
	
	public static Optional<float[]> getValuesFromCoverageStringFloat(final String coverage, int minimumCoverage) {
		
		int[] baseCoverages = decipherCoverageString(coverage);
		int total = baseCoverages[4];
		if (total < minimumCoverage) return Optional.empty();
		
		return Optional.of(new float[] { (float) baseCoverages[0] / total, 
				(float) baseCoverages[1] / total,
				(float) baseCoverages[2] / total, 
				(float) baseCoverages[3] / total,
				total});
	}

}
