/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.util;

import static java.util.Comparator.comparing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
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
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.math3.util.Pair;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.ChrPositionCache;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.NumberUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.illumina.IlluminaRecord;
import org.qcmg.sig.model.Comparison;
import org.qcmg.sig.model.SigMeta;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntByteHashMap;

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
	private static final char[] BASES_NO_N = new char[] {'A', 'C', 'G', 'T'};
	
	public static final String SNP_ARRAY = "SNP_array";
	public static final String UNKNOWN = "UNKNOWN";
	
	public static final int MINIMUM_COVERAGE = 10;
	public static final int MINIMUM_RG_COVERAGE = 10;
	
	public static final NumberFormat nf = new DecimalFormat("0.####");
	public static final QLogger logger = QLoggerFactory.getLogger(SignatureUtil.class);
	
	/*
	 * byte values representing the available genotypes
	 */
	public static final byte HOM_A = 16;
	public static final byte HOM_C = 32;
	public static final byte HOM_G = 64;
	public static final byte HOM_T = -128;
	public static final byte HET_AC = 3;
	public static final byte HET_AG = 5;
	public static final byte HET_AT = 9;
	public static final byte HET_CG = 6;
	public static final byte HET_CT = 10;
	public static final byte HET_GT = 12;
	
	
	public static final String MD_5_SUM = "##positions_md5sum";
	public static final String POSITIONS_COUNT = "##positions_count";
	public static final String MIN_BASE_QUAL = "##filter_base_quality";
	public static final String MIN_MAPPING_QUAL = "##filter_mapping_quality";
	public static final String MIN_GC_SCORE = "##filter_gc_score";
	public static final String RG_PREFIX = "##rg";
	public static final String NAN = "nan";
	
	public static final float HOM_CUTOFF = 0.90000f;
	public static final float HET_UPPER_CUTOFF = 0.70000f;
	public static final float HET_LOWER_CUTOFF = 0.30000f;
	
	/*
	 * METHODS
	 */
	
	public static String getCoverageStringFromCharsAndInts(char c1, char c2, int i1, int i2, boolean bespoke) {
		StringBuilder sb = new StringBuilder();
		if (bespoke) {
			
			for (char c : BASES_NO_N) {
				if (c1 == c && c2 == c) {
					StringUtils.updateStringBuilder(sb, "" + (i1 + i2), '-');
				} else if (c1 == c) {
					StringUtils.updateStringBuilder(sb, "" + i1, '-');
				} else if (c2 == c) {
					StringUtils.updateStringBuilder(sb, "" + i2, '-');
				} else {
					StringUtils.updateStringBuilder(sb, "0", '-');
				}
			}
			
		} else {
			sb.append("FULLCOV=");
			
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
		}
		return sb.toString();
	}
	public static String getCoverageStringFromCharsAndInts(char c1, char c2, int i1, int i2) {
		return getCoverageStringFromCharsAndInts(c1, c2, i1, i2, false);
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
		if (m.find()) {
			return m.group();
		}
		return UNKNOWN;
	}

	/**
	 * adds chr to the beginning of the elements in the list that start with a digit
	 * @param contigs
	 * @return
	 */
	public static List<String> addChrToContigs(List<String> contigs) {
		return contigs.stream()
				.map(f -> (Character.isDigit(f.charAt(0)) ? "chr" + f : f))
				.collect(Collectors.toList());
	}
	
	/**
	 * Get the unique contig names from the list of vcfs
	 * @param vcfs
	 * @return
	 */
	public static List<String> getUniqueContigsFromListOfVcfRecords(List<VcfRecord> vcfs) {
		return vcfs.stream()
				.map(v -> v.getChrPosition().getChromosome())
				.distinct()
				.collect(Collectors.toList());
	}
	
	/**
	 * returns true if ant of the supplied contigs start with a digit
	 * eg. 
	 * @param contigs
	 * @return
	 */
	public static boolean doContigsStartWithDigit(List<String> contigs) {
		return contigs.stream().anyMatch(p -> Character.isDigit(p.charAt(0)));
	}
	
	
	public static List<VcfRecord> addChrToVcfRecords(List<VcfRecord> vcfs) {
		
		List<VcfRecord> updatedVcfs = new ArrayList<>();
		for (VcfRecord v : vcfs) {
			if (Character.isDigit(v.getChrPosition().getChromosome().charAt(0))) {
				ChrPosition oldCP = v.getChrPosition();
				v = VcfUtils.cloneWithNewChrPos(v, ChrPositionUtils.cloneWithNewChromosomeName(oldCP, "chr" + oldCP.getChromosome()));
			}
			updatedVcfs.add(v);
		}
		return updatedVcfs;
		
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
				if (line.startsWith(Constants.HASH_STRING)) {
					continue;
				}
				//do a brute force search for the empty coverage string before passing to tokenizer
				// only populate ratios with non-zero values
				// attempt to keep memory usage down...
				if (line.indexOf(SHORT_CUT_EMPTY_COVERAGE) > -1) {
					continue;
				}
				// ignore entries that have nan in there
				if (line.indexOf(NAN) > -1) {
					continue;
				}
				
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
				if (line.startsWith(Constants.HASH_STRING)) {
					continue;
				}
				//do a brute force search for the empty coverage string before passing to tokenizer
				// only populate ratios with non-zero values
				// attempt to keep memory usage down...
				if (line.indexOf(SHORT_CUT_EMPTY_COVERAGE) > -1) {
					continue;
				}
				// ignore entries that have nan in there
				if (line.indexOf(NAN) > -1) {
					continue;
				}
				
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
	public static Map<ChrPosition, float[]> loadSignatureRatiosFloat(File file) throws IOException {
		return loadSignatureRatiosFloat(file, 10);
	}
	
	public static TIntByteHashMap loadSignatureRatiosFloatGenotypeNew(File file) throws IOException {
		return loadSignatureRatiosFloatGenotypeNew(file, 10, HOM_CUTOFF, HET_UPPER_CUTOFF, HET_LOWER_CUTOFF);
	}
	public static TIntByteHashMap loadSignatureRatiosFloatGenotypeNew(File file, int minCoverage, float homCutoff, float hetUpperCutoff, float hetLowerCutoff) throws IOException {
		TIntByteHashMap ratios = new TIntByteHashMap();
		
		if (null == file) {
			throw new IllegalArgumentException("Null file object passed to loadSignatureRatios");
		}
		
		try (TabbedFileReader reader = new TabbedFileReader(file)) {
			String line;
			
			for (TabbedRecord vcfRecord : reader) {
				line = vcfRecord.getData();
				if (line.startsWith(Constants.HASH_STRING)) {
					continue;
				}
				//do a brute force search for the empty coverage string before passing to tokenizer
				// only populate ratios with non-zero values
				// attempt to keep memory usage down...
				if (line.indexOf(SHORT_CUT_EMPTY_COVERAGE) > -1) {
					continue;
				}
				// ignore entries that have nan in there
				if (line.indexOf(NAN) > -1) {
					continue;
				}
				
				/*
				 * don't tokenise the line, just get the first 2 columns along with the last
				 * should be quicker than a full tokenise...
				 */
				
				int lastIndex = line.lastIndexOf(Constants.TAB_STRING);
				String coverage = line.substring(lastIndex);
				/*
				 * sometimes we have trailing tabs...
				 */
				if (StringUtils.isNullOrEmpty(coverage)) {
					int nextLastIndex = line.lastIndexOf(Constants.TAB_STRING, lastIndex - 1);
					coverage = line.substring(nextLastIndex + 1, lastIndex);
				}
				
				Optional<float[]> array = getValuesFromCoverageStringFloat(coverage, minCoverage);
				
				if (array.isPresent()) {
					byte g = getCodedGenotypeAsByte(array.get(), homCutoff, hetUpperCutoff, hetLowerCutoff);
					if (isCodedGenotypeValid(g)) {
						int index = line.indexOf(Constants.TAB_STRING);
						index = line.indexOf(Constants.TAB_STRING, index + 1);
						String chrAndPos = line.substring(0, index);
						ratios.put(ChrPositionCache.getStringIndex(chrAndPos), g);
					}
				}
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
			int bq = -1;
			int mq = -1;
			float gc = -1f;
			Map<String, String> rgIds = new THashMap<>();
			for (String s : h) {
				if (s.startsWith(SignatureUtil.MD_5_SUM)) {
					md = s.substring(MD_5_SUM.length() + 1);
				} else if (s.startsWith(POSITIONS_COUNT)) {
					count = Integer.parseInt(s.substring(POSITIONS_COUNT.length() + 1));
				} else if (s.startsWith(MIN_BASE_QUAL)) {
					bq = Integer.parseInt(s.substring(MIN_BASE_QUAL.length() + 1));
				} else if (s.startsWith(MIN_GC_SCORE)) {
					gc = Float.parseFloat(s.substring(MIN_GC_SCORE.length() + 1));
				} else if (s.startsWith(MIN_MAPPING_QUAL)) {
					mq = Integer.parseInt(s.substring(MIN_MAPPING_QUAL.length() + 1));
				} else if (s.startsWith(RG_PREFIX)) {
					int ci = s.indexOf(Constants.EQ);
					if (ci > -1) {
						rgIds.put(s.substring(2, ci), s.substring(ci + 1));
					}
				}
			}
			
			return Optional.of(new Pair<>(new SigMeta(md, count, bq, mq, gc), rgIds));
		}
	}
	
	public static Map<Short, int[]> getVariantAlleleFractionDistribution(File f, int minCoverage) throws IOException {
		
		Map<Short, int[]> dist = new HashMap<>(1000);
		
		/*
		 * load file, get float value of VAF
		 */
		try (VCFFileReader reader = new VCFFileReader(f)) {
			
			for (VcfRecord vcf : reader) {
				String info = vcf.getInfo();
				
				/*
				 * get total, see if it is gte our minCoverage cutoff
				 * expecting format to be FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0 yuck....
				 */
				
				int [] counts = decipherCoverageString(info);
				
				if (counts[4] >= 1) {
					boolean passesCoverage = counts[4] >= minCoverage;
					
					String ref = vcf.getRef();
					short vafAsShort = getFloatAsShort(getVAF(counts, ref));
					dist.computeIfAbsent(Short.valueOf(vafAsShort), v -> new int[2])[passesCoverage ? 0 : 1] += 1;
				}
			}
		}
		
		return dist;
	}
	
	/**
	 * This returns a short version of a float multiplied by 100 so as to get 1 basis point precision
	 * @param f
	 * @return
	 */
	public static short getFloatAsShort(float f) {
		return (short) (f * 100);
	}
	
	public static float getVAF(int[] counts, String ref) {
		float vaf = 0.0f;
		switch (ref) {
		case "A": vaf = (float)(counts[1] + counts[2] + counts[3]) / counts[4]; break;
		case "C": vaf = (float)(counts[0] + counts[2] + counts[3]) / counts[4]; break;
		case "G": vaf = (float)(counts[0] + counts[1] + counts[3]) / counts[4]; break;
		case "T": vaf = (float)(counts[0] + counts[1] + counts[2]) / counts[4]; break;
		}
		return vaf;
	}
	
	public static Pair<SigMeta, TMap<String, TIntByteHashMap>> loadSignatureGenotype(File file) throws IOException {
		return loadSignatureGenotype(file, MINIMUM_COVERAGE, MINIMUM_RG_COVERAGE);
	}
	
	public static Pair<SigMeta, TMap<String, TIntByteHashMap>> loadSignatureGenotype(File file, int minCoverage, int minRGCoverage) throws IOException {
		if (null == file) {
			throw new IllegalArgumentException("Null file object passed to loadSignatureGenotype");
		}
		TIntByteHashMap ratios = new TIntByteHashMap();
		TMap<String, TIntByteHashMap> rgRatios = new THashMap<>();
		
		Map<String, String> rgIds = Collections.emptyMap();
		SigMeta sm = null;
		
		try (TabbedFileReader reader = new TabbedFileReader(file)) {
			TabbedHeader h = reader.getHeader();
			
			Optional<Pair<SigMeta, Map<String, String>>> metaAndRGsO = getSigMetaAndRGsFromHeader(h);
			if (metaAndRGsO.isPresent()) {
				Pair<SigMeta, Map<String, String>> p = metaAndRGsO.get();
				sm = p.getFirst();
				rgIds = p.getSecond();
			}
			
			if (null != sm && sm.isValid()) {
				getDataFromBespolkeLayout(file, minCoverage, minRGCoverage, ratios, rgRatios, rgIds, reader);
			} else {
				rgRatios.put("all", loadSignatureRatiosFloatGenotypeNew(file));
			}
		}
		return new Pair<>(sm, rgRatios);
	}
	
	public static void getDataFromBespolkeLayout(File file, int minCoverage, int minRGCoverage, TIntByteHashMap ratios,
			TMap<String, TIntByteHashMap> rgRatios, Map<String, String> rgIds, TabbedFileReader reader) {
		int noOfRGs = rgIds.size();
		logger.debug("Number of rgs for  " + file.getAbsolutePath() + " is " + noOfRGs);
		
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
			String totalCov = coverage.substring(coverage.indexOf(Constants.COLON) + 1, commaIndex > -1 ? commaIndex : coverage.length());
			
			Optional<float[]> array = getValuesFromCoverageStringBespoke(totalCov, minCoverage);
			array.ifPresent(f -> {
				
				byte genotype1 = getCodedGenotypeAsByte(f);
				
				if (isCodedGenotypeValid(genotype1)) {
					cachePosition.set(ChrPositionCache.getStringIndex(params[0] + Constants.TAB + params[1]));
					ratios.put(cachePosition.get(), genotype1);
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
							
							Optional<float[]> arr = getValuesFromCoverageStringBespoke(cov, minRGCoverage);
							arr.ifPresent(f2-> {
								
								byte genotype2 = getCodedGenotypeAsByte(f2);
								
								if (isCodedGenotypeValid(genotype2)) {
									String rg = s.substring(0, index);
									TIntByteHashMap r = rgRatios.get(rg);
									if (r == null) {
										r = new TIntByteHashMap();
										rgRatios.put(rg, r);
									}
									r.put(cachePosition.get(), genotype2);
								}
							});
						}
					}
				}
			});
			
			rgRatios.put("all", ratios);
		}
	}
	
	/**
	 * Convert float array containing allele percentages for ACGT bases into a byte.
	 * 
	 * If over 90% in A, then will return 1000000 (binary form)		HOM_A
	 * If over 90% in C, then will return 0100000
	 * If over 90% in G, then will return 0010000
	 * If over 90% in T, then will return 0001000
	 * If het for AC, will return 00001100
	 * If het for AG, will return 00001010
	 * If het for AT, will return 00001001
	 * If het for CG, will return 00000110
	 * If het for CT, will return 00000101
	 * If het for GT, will return 00000011
	 * 
	 * 
	 * The default cutoffs are > 0.90000 for homozygous, and > 0.30000 and < 0.70000 for heterozygous
	 * 
	 * 
	 * @param f
	 * @return
	 */
	public static byte getCodedGenotypeAsByte(float[] f,  float homCutoff, float hetUpperCutoff, float hetLowerCutoff) {
		byte g1 = 0;
		for (int i = 0 ; i < 4 ; i++) {
			float d = f[i];
			if (d > homCutoff) {
				g1 = NumberUtils.setBit(g1, i + 4);
				break;
			} else if (d > hetLowerCutoff && d < hetUpperCutoff) {	// heterozygous
				g1 = NumberUtils.setBit(g1, i);
			}
		}
		return g1;
	}
	
	public static byte getCodedGenotypeAsByte(float[] f) {
		return getCodedGenotypeAsByte(f, HOM_CUTOFF,  HET_UPPER_CUTOFF, HET_LOWER_CUTOFF);
	}
	
	/**
	 * the following values are considered to be valid:
	 * 10000000		-128	HOM_A
	 * 01000000		64		HOM_C
	 * 00100000		32		HOM_G
	 * 00010000		16		HOM_T
	 * 00001100		12		HET_GT
	 * 00001010		10		HET_CT
	 * 00001001		9		HET_AT
	 * 00000110		6		HET_CG
	 * 00000101		5		HET_AG
	 * 00000011		3		HET_AC
	 * 
	 * @param b
	 * @return
	 */
	public static boolean isCodedGenotypeValid(byte b) {
		return b == HET_AC
				|| b == HET_AG
				|| b == HET_CG
				|| b == HET_AT
				|| b == HET_CT
				|| b == HET_GT
				|| b == HOM_T
				|| b == HOM_G
				|| b == HOM_C
				|| b == HOM_A;
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
		
		if (null == filesToExclude || filesToExclude.isEmpty()) {
			return originalList;
		}
		
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
			
			if (includeFile) {
				keepers.add(f);
			} else {
				logger.info("ignoring " + fName + " as it is in the excludes file");
			}
			
		}
		return keepers;
	}
	
	public static List<File> removeClosedProjectFilesFromList(List<File> originalList, List<String> closedProjects) {
		
		if (null == closedProjects || closedProjects.isEmpty()) {
			return originalList;
		}
		
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
		
		orderedSnpChipFiles.sort(FileUtils.FILE_COMPARATOR);
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
		if (null == collection || null == f) {
			return;		// don't proceed if collection or file is null
		}
		
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
		if ( ! inExcludes) {
			collection.add(f);
		}
	}
	
	
	public static String getCoverageStringForIlluminaRecord(IlluminaRecord illRec, String [] params, int arbitraryCoverage) {
		return getCoverageStringForIlluminaRecord(illRec, params, arbitraryCoverage, false);
	}
	public static String getCoverageStringForIlluminaRecord(IlluminaRecord illRec, String [] params, int arbitraryCoverage, boolean bespoke) {
		
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
				arrayGenotypoe = "" + snpChar1;
				
				if (illRec.getSecondAlleleCall() == 'A') {
					arrayGenotypoe += snpChar1;
				} else if (illRec.getSecondAlleleCall() == 'B') {
					arrayGenotypoe += snpChar2;
				}
			} else if (illRec.getFirstAlleleCall() == 'B') {
				arrayGenotypoe = "" + snpChar2 + snpChar2;
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
				arrayGenotypoe = "" + snpChar1;
				
				if (illRec.getSecondAlleleCall() == 'A') {
					arrayGenotypoe += snpChar1;
				} else if  (illRec.getSecondAlleleCall() == 'B') {
					arrayGenotypoe += snpChar2;
				}
			} else if (illRec.getFirstAlleleCall() == 'A' && "BOT".equalsIgnoreCase(illRec.getStrand())) {
				arrayGenotypoe = "" + snpChar2;
				if (illRec.getSecondAlleleCall() == 'A') {
					arrayGenotypoe += snpChar2;
				} else if  (illRec.getSecondAlleleCall() == 'B') {
					arrayGenotypoe += snpChar1;
				}
			} else if (illRec.getFirstAlleleCall() == 'B') {
				arrayGenotypoe = "" + snpChar2 + snpChar2;
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
		
		return getCoverageStringFromCharsAndInts(base1, base2, refCoverage, altCoverage, bespoke);
		
	}
	
	/**
	 * * Info field must be in the following format:
	 * "FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0";
	 * 
	 * @param info
	 * @return
	 */
	public static int[] decipherCoverageString(String info) {
		if (StringUtils.isNullOrEmpty(info)) {
			throw new IllegalArgumentException("Invalid coverage string passed to decipherCoverageString: " + info);
		}
		// strip out the pertinent bits
		// total
		final int totalIndex = info.indexOf("TOTAL:");
		
		if (totalIndex == -1) {
			throw new IllegalArgumentException("Invalid coverage string passed to decipherCoverageString: " + info);
		}
		
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
			logger.warn("Invalid coverage string: " + info);
			throw new IllegalArgumentException("Invalid coverage string passed to decipherCoverageString: " + info);
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
		if (StringUtils.isNullOrEmpty(info)) {
			throw new IllegalArgumentException("Invalid coverage string passed to decipherCoverageStringBespoke: " + info);
		}
		
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
		if (total < minimumCoverage) {
			return null;
		}
		
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
		if (total < minimumCoverage) {
			return Optional.empty();
		}
		
		return Optional.of(new float[] { (float) baseCoverages[0] / total, 
				(float) baseCoverages[1] / total,
				(float) baseCoverages[2] / total, 
				(float) baseCoverages[3] / total,
				total});
	}
	
	public static void writeXmlOutput( Map<String, int[]> fileIdsAndCounts, List<Comparison> allComparisons, String outputXml) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("qsignature");
		doc.appendChild(rootElement);
		
		// list files
		Element filesE = doc.createElement("files");
		rootElement.appendChild(filesE);
		
		// write output xml file
		// do it to console first...
		List<String> keys = new ArrayList<>( fileIdsAndCounts.keySet());
		keys.sort(null);
		for (String f  : keys) {
			int[] value = fileIdsAndCounts.get(f);
			
			Element fileE = doc.createElement("file");
			fileE.setAttribute("id", value[0] + "");
			fileE.setAttribute("name", f);
			fileE.setAttribute("coverage", value[1] + "");
			if (value[2] > -1) {
				fileE.setAttribute("average_coverage_at_positions", value[2] + "");
			}
			filesE.appendChild(fileE);
		}
		
		// list files
		Element compsE = doc.createElement("comparisons");
		rootElement.appendChild(compsE);
		
		/*
		 * sort comparisons by file ids
		 */
		allComparisons.sort(comparing((Comparison c) -> fileIdsAndCounts.get(c.getMain())[0])
				.thenComparing((c) ->  fileIdsAndCounts.get(c.getTest())[0]));
		
		for (Comparison comp : allComparisons) {
			int id1 = fileIdsAndCounts.get(comp.getMain())[0];
			int id2 = fileIdsAndCounts.get(comp.getTest())[0];
			
			Element compE = doc.createElement("comparison");
			compE.setAttribute("file1", id1 + "");
			compE.setAttribute("file2", id2 + "");
			compE.setAttribute("score", comp.getScore() + "");
			compE.setAttribute("overlap", comp.getOverlapCoverage() + "");
			compsE.appendChild(compE);
		}
		
		// write it out
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(outputXml));
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(source, result);
	}

}
