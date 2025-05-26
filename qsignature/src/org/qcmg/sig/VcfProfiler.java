/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2021.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.sig;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qio.vcf.VcfFileReader;
import org.qcmg.sig.model.SigVcfMeta;
import org.qcmg.sig.util.SignatureUtil;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Performs some profiling on the qsignature generated vcf file.
 * It is hoped that this will help to characterize the underlying input (BAM files)
 * 
 * @author oliverh
 *
 */
public class VcfProfiler {
	
	private static final int MIN_COVERAGE = 10;
	
	private static QLogger logger;
	private int exitStatus;

    private String outputXml;
	private String inputVcf;
	private String snpPositionsVcf;
	
	private int minimumCoverage = SignatureUtil.MINIMUM_COVERAGE;
	
	private final int coverageDistMaxValue = 1000;
	private final int[] coverageDist = new int[coverageDistMaxValue + 1];
	
	private final Map<String, AtomicInteger> baseDist = new HashMap<>();
	private final Map<String, int[]> geneDist = new HashMap<>();
	private final Map<String, AtomicInteger> snpDist = new HashMap<>();
	
	private final Map<ChrPosition, String> positionGeneMap = new HashMap<>();
	private final Map<String, List<String>> homMap = new HashMap<>();
	
	private final AtomicIntegerArray homLengthDistribution = new AtomicIntegerArray(40000);
	
	DecimalFormat df = new DecimalFormat("####0.000");
	
	
	private int engage() throws Exception {
		
		populatePosGeneMap(snpPositionsVcf, positionGeneMap, snpDist);
		logger.info("loaded " + positionGeneMap.size() + " entries into positionGeneMap");
		
		/*
		 * get some stats on the genes that have are represented
		 */
		int uniqueGeneCount = getUniqueGeneCountFromMap(positionGeneMap);
		
		/*
		 * get count of positions in qsig positions file from snpDist 
		 */
		AtomicInteger qsigPositionsCount =  snpDist.values().stream().reduce(new AtomicInteger(), (a, b) -> new AtomicInteger(a.get() + b.get()));
		
		int vcfRecordCount = 0;
		int geneHitCount = 0;
		int geneHitCountPassingCoverage = 0;
		int betweenGeneHitCount = 0;
		int betweenGeneHitCountPassingCoverage = 0;
		
		
		int firstHomPosition = 0;
		int lastHomPosition = 0;
		boolean prevPositionHom = false;
		int contiguousHomCounter = 0;
		int homCounter = 0;
		int hetCounter = 0;
		String thisContig;
		String lastContig = "";
		
		/*
		 * load vcf file into memory
		 */
		try (VcfFileReader reader = new VcfFileReader(inputVcf)) {
			for (VcfRecord rec : reader) {
				
				thisContig = rec.getChromosome();
				if (vcfRecordCount == 0) {
					lastContig = rec.getChromosome();
				} else {
					if ( ! thisContig.equals(lastContig)) {
						/*
						 * reset homCounters
						 */
						contiguousHomCounter = 0;
						firstHomPosition = 0;
						lastHomPosition = 0;
						prevPositionHom = false;
						lastContig = thisContig;
					}
				}
				
				vcfRecordCount++;
				/*
				 * info field is what we are after
				 */
				String info = rec.getInfo();
				String ref = rec.getRef();
				int startIndex = info.indexOf("QAF=t:");
				if (startIndex > -1) {
					int endIndex = info.indexOf(",", startIndex);
					String infoSubString = endIndex > -1 ? info.substring(startIndex + 6, endIndex) : info.substring(startIndex + 6);
					
					/*
					 * this should be a string containing counts of As-Cs-Gs-Ts
					 * Convert into an array
					 */
					Optional<int[]> coverageArray = SignatureUtil.decipherCoverageStringBespoke(infoSubString);
					
					int coverage = coverageArray.map(ints -> Arrays.stream(ints).sum()).orElse(0);
					if (coverage >= coverageDistMaxValue) {
						
						coverageDist[coverageDistMaxValue] ++;
					} else {
						coverageDist[coverage] ++;
					}
					
					boolean isWildtype = false;
					boolean isHomAlt = false;
					boolean isHetAlt = false;
					boolean isHetWildtype = false;
					boolean isHom;
					
					if (coverageArray.isPresent()) {
						int [] coverageArrayActual = coverageArray.get();
						/*
						 * get the bases that this represents
						 * use the trusted 90% for hom, 30-70% for het cutoffs
						 */
						
						String basesCovered = getBasesCovered(coverageArrayActual, coverage);
						isHom = basesCovered.length() == 1;
						isHomAlt = isHom && ! basesCovered.equals(ref);
						isWildtype = isHom && basesCovered.equals(ref);
						isHetWildtype = basesCovered.length() > 1 && basesCovered.contains(ref);
						isHetAlt = basesCovered.length() > 1 && ! basesCovered.contains(ref);
						
						if (isHom) {
							homCounter++;
						} else {
							hetCounter++;
						}
						if (isHom) {
							contiguousHomCounter++;
							if ( ! prevPositionHom) {
								firstHomPosition = rec.getPosition();
							}
							lastHomPosition = rec.getPosition();
						} else {
							if (prevPositionHom) {
								if (contiguousHomCounter > 2) {
								
									/*
									 * add details to map
									 */
									homMap.computeIfAbsent(rec.getChromosome(), f -> new ArrayList<>()).add((lastHomPosition - firstHomPosition) + ":" + contiguousHomCounter + ":" + rec.getChromosome() + "_" + firstHomPosition);
									
									/*
									 * update hom length distribution
									 */
									homLengthDistribution.incrementAndGet(contiguousHomCounter);
								}
								
								contiguousHomCounter = 0;
							}
						}
						prevPositionHom = isHomAlt;
						
						baseDist.computeIfAbsent(ref + "-" + basesCovered, f -> new AtomicInteger()).incrementAndGet();
					}
					
					/*
					 * look into gene coverage now
					 */
					String gene = positionGeneMap.get(rec.getChrPosition());
					if ( ! StringUtils.isNullOrEmpty(gene)) {
						int [] dist = geneDist.computeIfAbsent(gene, f -> new int[5]);
						int position = isWildtype ? 0 : isHetWildtype ? 1 : isHetAlt ? 2 : isHomAlt ? 3 : 4;
						dist[position] ++;
						geneHitCount++;
						if (coverage >= MIN_COVERAGE) {
							geneHitCountPassingCoverage++;
						}
					} else {
						betweenGeneHitCount++;
						if (coverage >= MIN_COVERAGE) {
							betweenGeneHitCountPassingCoverage++;
						}
					}
				}
			}
		}
		
		/*
		 * print some stats
		 */
		logger.info("vcfRecordCount: " + vcfRecordCount);
		int countAboveCutoff = 0;
		int cutoff = 10;
		logger.info("Coverage Distribution:");
		for (int i = 0 ; i < coverageDistMaxValue ; i++) {
			if (coverageDist[i] > 0) {
				logger.info("coverage: " + i + ", number of positions with this coverage: " + coverageDist[i]);
				if (i >= cutoff) {
					countAboveCutoff += coverageDist[i];
				}
			}
		}
		logger.info("vcfRecordCount with coverage above " + cutoff + ": " + countAboveCutoff + "(" + 100 * ((double)countAboveCutoff / vcfRecordCount) + "%)");
		logger.info("Base Distribution:");
		int positionsEqualToRef = 0;
		int positionsNoAltCanBeCalled = 0;
		int positionsAltIsHet = 0;
		int positionsAltIsHom = 0;
		
		for (Map.Entry<String, AtomicInteger> entry : baseDist.entrySet()) {
			logger.info(entry.getKey() + ": " + entry.getValue().get());
			
			String alt = entry.getKey().substring(entry.getKey().indexOf("-") + 1);
			if (StringUtils.isNullOrEmpty(alt)) {
				positionsNoAltCanBeCalled += entry.getValue().get();
			} else {
				if (alt.equals( String.valueOf(entry.getKey().charAt(0)))) {
					positionsEqualToRef += entry.getValue().get();
				} else {
					if (alt.length() == 1) {
						positionsAltIsHom += entry.getValue().get();
					} else {
						positionsAltIsHet += entry.getValue().get();
					}
				}
			}
		}
		logger.info("positionsEqualToRef: " + positionsEqualToRef + " (" + df.format(100 * ((double)positionsEqualToRef / vcfRecordCount)) + "%)");
		logger.info("positionsNoAltCanBeCalled: " + positionsNoAltCanBeCalled + " (" + 100 * ((double)positionsNoAltCanBeCalled / vcfRecordCount) + "%)");
		logger.info("positionsAltIsHet: " + positionsAltIsHet + " (" + 100 * ((double)positionsAltIsHet / vcfRecordCount) + "%)");
		logger.info("positionsAltIsHom: " + positionsAltIsHom + " (" + 100 * ((double)positionsAltIsHom / vcfRecordCount) + "%)");
		
		logger.info("Gene Distribution: [wildtype, het wildtype, het alt , hom alt, dunno]");
		int [] geneDistArray = new int[100]; 
		for (Map.Entry<String, int[]> entry : geneDist.entrySet()) {
			logger.info(entry.getKey() + ": " + Arrays.toString(entry.getValue()));
			int totalValue = Arrays.stream(entry.getValue()).sum();
			if (totalValue >= 99) {
				geneDistArray[99] ++;
			} else {
				geneDistArray[totalValue] ++;
			}
		}
		for (int i = 0 ; i < geneDistArray.length ; i++) {
			if (geneDistArray[i] > 0) {
				logger.info("position count: " + i + ", gene count: " + geneDistArray[i]);
			}
		}
		
		logger.info("qsignature positions coverage percentage: " + df.format(100d * vcfRecordCount / qsigPositionsCount.get()) + "% (" + vcfRecordCount + " from a possible " + qsigPositionsCount.get() + ")");
		
		logger.info("geneDist size (number of genes hit by this vcf): " + geneDist.size() + " from a possible " + uniqueGeneCount + ", which is " + df.format(100 * ((double)geneDist.size() / uniqueGeneCount)) + "%");
		logger.info("geneHitCount: " + geneHitCount);
		logger.info("geneHitCountPassingCoverage: " + geneHitCountPassingCoverage);
		logger.info("geneHitCountPassingCoverage percentage: " + df.format(100d * geneHitCountPassingCoverage / geneHitCount) + "%");
		logger.info("betweenGeneHitCount: " + betweenGeneHitCount);
		logger.info("betweenGeneHitCountPassingCoverage: " + betweenGeneHitCountPassingCoverage);
		logger.info("betweenGeneHitCountPassingCoverage percentage: " + df.format(100d * betweenGeneHitCountPassingCoverage / betweenGeneHitCount) + "%");
		logger.info("gene hit percentage: " + df.format(100 * ((double)geneHitCount / (geneHitCount + betweenGeneHitCount))) + "%");
		logger.info("gene hit percentage passing coverage: " + df.format(100 * ((double)geneHitCountPassingCoverage / (geneHitCountPassingCoverage + betweenGeneHitCountPassingCoverage))) + "%");
		logger.info("homCounter: " + homCounter + ", hetCounter: " + hetCounter + ", total: " + (homCounter + hetCounter) + ", hom %: " + df.format(100d * homCounter / (homCounter + hetCounter)));
		
		
		/*
		 * hom stats
		 */
		int numberOfHomEvents = 0;
		for (Entry<String, List<String>> entry : homMap.entrySet()) {
			numberOfHomEvents += entry.getValue().size();
			int totalSpan = 0;
			int totalHomPositions = 0;
			for (String s : entry.getValue()) {
				String [] sArray = s.split(":");
				totalSpan += Integer.parseInt(sArray[0]);
				totalHomPositions += Integer.parseInt(sArray[1]);
			}
			logger.info("total hom span for " + entry.getKey() + " is " + totalSpan + ", number of events: " + entry.getValue().size() + ", number of hom positions: " + totalHomPositions 
					+ ", as a % of positions in that contig: " + df.format(100.0 *  totalHomPositions / snpDist.get(entry.getKey()).get()));
		}
		logger.info("numberOfHomEvents: " + numberOfHomEvents);
		
		logger.info("hom length distribution:");
		Map<Integer, Integer> homLengthDistributionMap = new HashMap<>();
		for (int i = 0 ; i < homLengthDistribution.length() ; i++) {
			if (homLengthDistribution.get(i) > 0) {
				logger.info("hom length: " + i + ", count: " + homLengthDistribution.get(i));
				homLengthDistributionMap.put(i, homLengthDistribution.get(i));
			}
		}
		
		/*
		 * set attributes on object that jackson will serialise into json for us
		 */
		SigVcfMeta svm = new SigVcfMeta();
		svm.setNumberOfPositions(vcfRecordCount);
		svm.setNumberOfPositionsPercentage(100d * vcfRecordCount / qsigPositionsCount.get());
		svm.setUniqueGeneHitCount(uniqueGeneCount);
		svm.setUniqueGeneHitCountPercentage(100d * geneDist.size() / uniqueGeneCount);
		svm.setGeneHitCount(geneHitCount);
		svm.setGeneHitCountPercentage(100d * geneHitCount / (geneHitCount + betweenGeneHitCount));
		svm.setGeneHitCountPassingCoverage(geneHitCountPassingCoverage);
		svm.setGeneHitCountPassingCoveragePercentage(100d * geneHitCountPassingCoverage / geneHitCount);
		svm.setGeneDist(geneDist);
		svm.setHomLengthDistribution(homLengthDistributionMap);
		svm.setNumberOfHomPositions(homCounter);
		svm.setNumberOfHomPositionsPercentage(100d * homCounter / (homCounter + hetCounter));
		svm.setNumberOfHetPositions(hetCounter);
		
		ObjectMapper om = new ObjectMapper();
		om.writeValue(new File(outputXml), svm);
		
		return exitStatus;
	}
	
	static int getUniqueGeneCountFromMap(Map<ChrPosition, String> map) {
		logger.info("position gene map size: " + map.size());
		
		Map<String, AtomicInteger> geneCountMap = new HashMap<>();
		
		for (Entry<ChrPosition, String> entry : map.entrySet()) {
			geneCountMap.computeIfAbsent(entry.getValue(), f -> new AtomicInteger()).incrementAndGet();
		}
		
		logger.info("Number of unique genes: " + geneCountMap.size());
		return geneCountMap.size();
		
	}
	
	static void populatePosGeneMap(String file, Map<ChrPosition, String> map, Map<String, AtomicInteger> snpDist) throws IOException {
		try (VcfFileReader reader = new VcfFileReader(file)) {
			for (VcfRecord vcf : reader) {
				
				/*
				 * populate snp distribution map for all entries
				 */
				snpDist.computeIfAbsent(vcf.getChromosome(), f -> new AtomicInteger()).incrementAndGet();
				
				/*
				 * only populate gene map if vcf record has a gene id
				 */
				String info = vcf.getInfo();
				if ( ! StringUtils.isNullOrEmpty(info) && ! info.equals(Constants.MISSING_DATA_STRING)) {
					
					int geneIdIndex = info.indexOf("gene_name=");
					if (geneIdIndex > -1) {
						int colonIndex = info.indexOf(";", geneIdIndex + 10);
						String geneName = colonIndex > -1 ? info.substring(geneIdIndex + 10, colonIndex) :  info.substring(geneIdIndex + 10);
						map.put(vcf.getChrPosition(), geneName);
					}
				}
			}
		}
	}
	
	public String getBasesCovered(int [] array, int coverage) {
		int aCount = array[0];
		int cCount = array[1];
		int gCount = array[2];
		int tCount = array[3];
		
		/*
		 * deal with homs first
		 */
		int homCutoff = (int)(0.9 * coverage);
		if (aCount >= homCutoff) {
			return "A";
		} else if (cCount >= homCutoff) {
			return "C";
		} else if (gCount >= homCutoff) {
			return "G";
		} else if (tCount >= homCutoff) {
			return "T";
		}
		
		int hetLower = (int)(0.3 * coverage);
		int hetUpper = (int)(0.7 * coverage);
		String toReturn = "";
		
		if (aCount > hetLower && aCount < hetUpper) {
			toReturn += "A";
		}
		if (cCount > hetLower && cCount < hetUpper) {
			toReturn += "C";
		}
		if (gCount > hetLower && gCount < hetUpper) {
			toReturn += "G";
		}
		if (tCount > hetLower && tCount < hetUpper) {
			toReturn += "T";
		}
		
		return toReturn;
	}
	
	public static void main(String[] args) throws Exception {
		VcfProfiler sp = new VcfProfiler();
		int exitStatus;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running VcfProfiler:", e);
			else {
				System.err.println("Exception caught whilst running VcfProfiler: " + e.getMessage());
				System.err.println(Messages.VCF_PROFILER_USAGE);
			}
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String[] args) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.VCF_PROFILER_USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.VCF_PROFILER_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.VCF_PROFILER_USAGE);
		} else {
			// configure logging
            String logFile = options.getLog();
			logger = QLoggerFactory.getLogger(VcfProfiler.class, logFile, options.getLogLevel());
			
			
			String [] cmdLineOutputFiles = options.getOutputFileNames();
			if (null != cmdLineOutputFiles && cmdLineOutputFiles.length > 0) {
				outputXml = cmdLineOutputFiles[0];
			}
			
			String [] cmdLineInputFiles = options.getInputFileNames();
			if (null != cmdLineInputFiles && cmdLineInputFiles.length > 0) {
				inputVcf = cmdLineInputFiles[0];
			}
			options.getSnpPositions().ifPresent(s -> snpPositionsVcf = s);
			if (snpPositionsVcf == null) {
				System.err.println(Messages.VCF_PROFILER_USAGE);
				return 1;
			}
			
			options.getMinCoverage().ifPresent(i -> minimumCoverage = i);
			logger.tool("Setting minimum coverage to: " + minimumCoverage);
			logger.tool("snpPositionsVcf: " + snpPositionsVcf);
			
			logger.logInitialExecutionStats("VcfProfiler", VcfProfiler.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
