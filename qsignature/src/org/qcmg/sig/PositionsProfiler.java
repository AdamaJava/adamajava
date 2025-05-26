/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2021.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.sig;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qio.vcf.VcfFileReader;

/**
 * This class provides some stats on the qsignature positions vcf file.
 * This is the file that contains the positions that are used to query BAMs and snp array files
 * 
 * This class will not be often used.
 * There are no unit tests for this class - please don't assume that what appears in the generated log file is correct
 * See https://github.com/AdamaJava/adamajava/pull/262#issuecomment-839417078 for further details
 * 
 * @author oliverh
 *
 */
public class PositionsProfiler {
	
	private static QLogger logger;
	
	private int exitStatus;
    private String snpPositionsVcf;
	
	private final Map<String, AtomicInteger> snpDistByContig = new HashMap<>();
	private final Map<String, AtomicInteger> snpDistByGeneMap = new HashMap<>();
	
	DecimalFormat df = new DecimalFormat("####0.000");
	
	
	private int engage() throws Exception {
		
		populatePosGeneMap(snpPositionsVcf, snpDistByGeneMap, snpDistByContig);
		logger.info("loaded " + snpDistByGeneMap.size() + " entries into snpDistByGeneMap");
		logger.info("loaded " + snpDistByContig.size() + " entries into snpDistByContig");
		
		/*
		 * get some stats on the genes that are present
		 */
		List<String> sortedGenes = new ArrayList<>(snpDistByGeneMap.keySet());
		sortedGenes.sort(null);
		for (String gene : sortedGenes) {
			logger.info("gene: " + gene + ", positions in gene: " + snpDistByGeneMap.get(gene).get());
		}
		List<String> sortedContigs = new ArrayList<>(snpDistByContig.keySet());
		sortedContigs.sort(null);
		for (String contig : sortedContigs) {
			logger.info("contig: " + contig + ", positions in contig: " + snpDistByContig.get(contig).get());
		}
		
		logger.info("number of genes: " + sortedGenes.size());
		
		
		int acGeneCount = 0;
		int apGeneCount = 0;
		int orfCount = 0;
		int as1Count = 0;
		int rpCount = 0;
		int ctaCount = 0;
		int ctbCount = 0;
		int ctcCount = 0;
		int ctdCount = 0;
		for (String gene : sortedGenes) {
			if (gene.matches("AC[0-9]+\\.[0-9]+")) {
				acGeneCount++;
			} else if (gene.matches("AP[0-9]+\\.[0-9]+")) {
				apGeneCount++;
			} else if (gene.contains("orf")) {
				orfCount++;
			} else if (gene.endsWith("-AS1")) {
				as1Count++;
			} else if (gene.matches("RP[0-9]+-[0-9A-Z]+\\.[0-9]+")) {
				rpCount++;
			} else if (gene.matches("CTA-[0-9A-Z]+\\.[0-9]+")) {
				ctaCount++;
			} else if (gene.matches("CTB-[0-9A-Z]+\\.[0-9]+")) {
				ctbCount++;
			} else if (gene.matches("CTC-[0-9A-Z]+\\.[0-9]+")) {
				ctcCount++;
			} else if (gene.matches("CTD-[0-9A-Z]+\\.[0-9]+")) {
				ctdCount++;
			}
		}
		logger.info("acGeneCount: " + acGeneCount + ", eg. AC002480.2");
		logger.info("apGeneCount: " + apGeneCount + ", eg. AP000525.9");
		logger.info("orfCount: " + orfCount + ", eg. C10orf90");
		logger.info("as1Count: " + as1Count + ", eg. C1RL-AS1");
		logger.info("rpCount: " + rpCount + ", eg. RP11-108K14.4");
		logger.info("ctaCount: " + ctaCount + ", eg. CTA-929C8.6");
		logger.info("ctbCount: " + ctbCount + ", eg. CTB-134H23.2");
		logger.info("ctcCount: " + ctcCount + ", eg. CTC-340A15.2");
		logger.info("ctdCount: " + ctdCount + ", eg. CTD-2021A8.2");
		
		int i = 100;
		logger.info("top " + i + " genes by number of positions");
		
		List<String> topGenes = snpDistByGeneMap.entrySet().stream().sorted((e1, e2) -> Integer.compare(e2.getValue().get(), e1.getValue().get())).map(Entry::getKey).limit(i).toList();
		for (String topGene : topGenes) {
			logger.info("topGene: " + topGene + ", positions in gene: " + snpDistByGeneMap.get(topGene).get());
		}
		
		return exitStatus;
	}
	
	static void populatePosGeneMap(String file, Map<String, AtomicInteger> snpDistByGene, Map<String, AtomicInteger> snpDistByContig) throws IOException {
		
		Map<String, AtomicInteger> geneBioTypePositionCountMap = new HashMap<>();
		int recordCount = 0;
		try (VcfFileReader reader = new VcfFileReader(file)) {
			for (VcfRecord vcf : reader) {
				recordCount++;
				/*
				 * populate snp distribution map for all entries
				 */
				snpDistByContig.computeIfAbsent(vcf.getChromosome(), f -> new AtomicInteger()).incrementAndGet();
				
				/*
				 * only populate gene map if vcf record has a gene id
				 */
				String info = vcf.getInfo();
				if ( ! StringUtils.isNullOrEmpty(info) && ! info.equals(Constants.MISSING_DATA_STRING)) {
					
					int geneIdIndex = info.indexOf("gene_name=");
					if (geneIdIndex > -1) {
						
						int bioTypeIndex = info.indexOf("gene_biotype=");
						if (bioTypeIndex > -1) {
							int bioTypeEndIndex = info.indexOf(";", bioTypeIndex);
							String bioType = info.substring(bioTypeIndex + 13, bioTypeEndIndex);
							geneBioTypePositionCountMap.computeIfAbsent(bioType, f -> new AtomicInteger()).incrementAndGet();
						}
						
						int colonIndex = info.indexOf(";", geneIdIndex + 10);
//						logger.info("colonIndex: " + colonIndex + ", info: " + info + ", geneIdIndex + 10: " + (geneIdIndex + 10) + ", gene: " + info.substring(geneIdIndex, colonIndex));
						String geneName = colonIndex > -1 ? info.substring(geneIdIndex + 10, colonIndex) :  info.substring(geneIdIndex + 10);
						snpDistByGene.computeIfAbsent(geneName, f -> new AtomicInteger()).incrementAndGet();
					}
				}
			}
		}
		
		for (Entry<String, AtomicInteger> entry : geneBioTypePositionCountMap.entrySet()) {
			logger.info("gene bio type: " + entry.getKey() + ", number of positions: " + entry.getValue().get());
		}
		
		logger.info("recordCount: " + recordCount);
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
		PositionsProfiler sp = new PositionsProfiler();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running PositionsProfiler:", e);
			else {
				System.err.println("Exception caught whilst running PositionsProfiler: " + e.getMessage());
				System.err.println(Messages.POSITIONS_PROFILER_USAGE);
			}
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String[] args) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.POSITIONS_PROFILER_USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.POSITIONS_PROFILER_USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.POSITIONS_PROFILER_USAGE);
		} else {
			// configure logging
            String logFile = options.getLog();
			logger = QLoggerFactory.getLogger(PositionsProfiler.class, logFile, options.getLogLevel());
			
			options.getSnpPositions().ifPresent(s -> snpPositionsVcf = s);
			logger.tool("snpPositionsVcf: " + snpPositionsVcf);
			
			logger.logInitialExecutionStats("PositionsProfiler", PositionsProfiler.class.getPackage().getImplementationVersion(), args);
			
			return engage();
		}
		return returnStatus;
	}

}
