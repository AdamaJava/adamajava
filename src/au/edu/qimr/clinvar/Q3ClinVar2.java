package au.edu.qimr.clinvar;

import gnu.trove.list.array.TLongArrayList;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.SequenceUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qmule.SmithWatermanGotoh;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

import au.edu.qimr.clinvar.model.PositionChrPositionMap;
import au.edu.qimr.clinvar.util.ClinVarUtil;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;

public class Q3ClinVar2 {
	
private static QLogger logger;
	
	
private static final int TILE_SIZE = 13;
	private static String[] fastqFiles;
	private static String version;
	private String logFile;
	private String xmlFile;
	private String outputFile;
	private String refTiledAlignmentFile;
	private String refFileName;
	private final int binId = 1;
	
	private int minBinSize = 10;
	
	private final Map<String, AtomicInteger> reads = new HashMap<>();
	private final Map<String, AtomicInteger> fragments = new HashMap<>();
	private final Set<String> fragAndRCFrags = new HashSet<>();
	
	private final Set<String> frequentlyOccurringRefTiles = new HashSet<>();
	private final Map<String, TLongArrayList> refTilesPositions = new HashMap<>();
	
	private final PositionChrPositionMap positionToActualLocation = new PositionChrPositionMap();
	
	private final Map<String, byte[]> referenceCache = new HashMap<>();
	
	
	
//	private final static Map<Pair<FastqRecord, FastqRecord>, List<Probe>> multiMatchingReads = new HashMap<>();
	
	private int exitStatus;
	
	protected int engage() throws Exception {
		
			
		int fastqCount = 0;
		// read in a fastq file and lits see if we get some matches
		try (FastqReader reader1 = new FastqReader(new File(fastqFiles[0]));
				FastqReader reader2 = new FastqReader(new File(fastqFiles[1]));) {
			
			for (FastqRecord rec : reader1) {
				FastqRecord rec2 = reader2.next();
				
				/*
				 * Put entries into map - don't collapse at the moment as we could save some processing if we have multiple identical reads
				 */
				String r1Andr2 = rec.getReadString() + ":" + rec2.getReadString();
				AtomicInteger ai = reads.get(r1Andr2);
				if (null == ai) {
					reads.put(r1Andr2, new AtomicInteger(1));
				} else {
					ai.incrementAndGet();
				}
				fastqCount++;
			}
		}
		
		logger.info("no of unique fragments: " + reads.size() + ", from " + fastqCount + " fastq records");
		int perfectOverlap = 0;
		int nonPerfectOverlap = 0;
		int smallOverlap = 0;
		Map<Integer, AtomicInteger> overlapLengthDistribution = new TreeMap<>();
		for (Entry<String, AtomicInteger> entry : reads.entrySet()) {
			String combinedReads = entry.getKey();
			String r1 = combinedReads.substring(0, combinedReads.indexOf(':'));
			String r2 = combinedReads.substring(combinedReads.indexOf(':') + 1);
			String r2RevComp = SequenceUtil.reverseComplement(r2);
			
			SmithWatermanGotoh nm = new SmithWatermanGotoh(r1, r2RevComp, 5, -4, 16, 4);
			String [] newSwDiffs = nm.traceback();
			String overlapMatches = newSwDiffs[1];
			if (overlapMatches.indexOf(" ") > -1 || overlapMatches.indexOf('.') > -1) {
				nonPerfectOverlap++;
//				for (String s : newSwDiffs) {
//					logger.info("non perfect overlap sw: " + s);
//				}
			} else {
				String overlap = newSwDiffs[0];
				int overlapLength = overlapMatches.length();
				if (overlapLength < 10) {
					smallOverlap++;
					continue;
				}
				if (overlapLength >= 150) {
					logger.info("r1 length: " + r1.length() + ", r2RecVomp length: " + r2RevComp.length());
					for (String s : newSwDiffs) {
						logger.info("overlapLength >= 150 sw: " + s);
					}
				}
				AtomicInteger ai = overlapLengthDistribution.get(overlapLength);
				if (null == ai) {
					overlapLengthDistribution.put(overlapLength, new AtomicInteger(1));
				} else {
					ai.incrementAndGet();
				}
				perfectOverlap++;
				
				/*
				 * Now build fragment
				 * check to see which read starts with the overlap
				 */
				String fragment = null;
				if (r1.indexOf(overlap) < 2 || r2RevComp.endsWith(overlap)) {
					fragment = r2RevComp.substring(0, r2RevComp.indexOf(overlap)) + overlap + r1.substring(r1.indexOf(overlap) + overlap.length());
				} else if (r2RevComp.indexOf(overlap) < 2 || r1.endsWith(overlap)) {
					fragment = r1.substring(0, r1.indexOf(overlap)) + overlap + r2RevComp.substring(r2RevComp.indexOf(overlap) + overlap.length());
				} else {
					logger.warn("neither r1 nor r2RevComp start with the overlap!!!");
					logger.warn("r1: " + r1);
					logger.warn("r2RevComp: " + r2RevComp);
					logger.warn("overlap: " + overlap);
					continue;
				}
				
				
				if (fragment.length() < 150) {
					logger.warn("short fragment made: " + fragment);
					logger.warn("r1: " + r1);
					logger.warn("r2RevComp: " + r2RevComp);
					logger.warn("overlap: " + overlap);
				}
				AtomicInteger ai2 = fragments.get(fragment);
				if (null == ai2) {
					fragments.put(fragment, new AtomicInteger(entry.getValue().intValue()));
				} else {
					ai2.addAndGet(entry.getValue().intValue());
				}
				
			}
			
		}
		logger.info("perfectOverlap: " + perfectOverlap + ", nonPerfectOverlap: " + nonPerfectOverlap + ", small overlap: " + smallOverlap);
		
		for (Entry<Integer, AtomicInteger> entry : overlapLengthDistribution.entrySet()) {
			logger.info("overlapLength: " + entry.getKey().intValue() + ", count: " + entry.getValue().intValue());
		}
		// cleanup
		overlapLengthDistribution.clear();
		overlapLengthDistribution = null;
		
		logger.info("fragments size: " + fragments.size());
		for (Entry<String, AtomicInteger> entry : fragments.entrySet()) {
			if (entry.getValue().intValue() > 100) {
				logger.info("fragment: " + entry.getKey() + ", count: " + entry.getValue().intValue());
			}
			fragAndRCFrags.add(entry.getKey());
			fragAndRCFrags.add(SequenceUtil.reverseComplement(entry.getKey()));
		}
		
		
		logger.info("No of entries in fragAndRCFrags: " + fragAndRCFrags.size());
		
		
		loadTiledAlignerData();
		
//		String r1 = "ATTCTGCTCTAAATAAAAATGGTTTAACCTTTCTACTGTTTTCTTTGTCTGATAATAACTTCCAAAAAAATACCTAGCTCAAGGGTTAATATTTCATAAATAGTTACTTTTTTTTTTCATTTTTAGGAAGTACATCTCAGAATCTTGATTC";
//		String r2 = "GCTTTGATAAAACTTTCGATCACTTTGTAAAAATCAAAGGGTTTTAAATTAAGCACATTCAGAATCCATGGGAAAGACAAATCTGTTCCAGAATCAAGATTCTGAGATGTACTTCCTAAAAATGAAAAAAAAAAGTAACTATTTATGAAAT";
//		String r2RevComp = SequenceUtil.reverseComplement(r2);
//		
//		SmithWatermanGotoh nm = new SmithWatermanGotoh(r1, r2RevComp, 5, -4, 16, 4);
//		String [] newSwDiffs = nm.traceback();
//		for (String s : newSwDiffs) {
//			logger.info("sw: " + s);
//		}
		
		
//		String 
//		String overlap = "ATTTCATAAATAGTTACTTTTTTTTTTCATTTTTAGGAAGTACATCTCAGAATCTTGATTC";
//		ATTTCATAAATAGTTACTTTTTTTTTTCATTTTTAGGAAGTACATCTCAGAATCTTGATTC		
		logger.info("no of fastq records: " +fastqCount);
		return exitStatus;
	}
	
	
	private void loadTiledAlignerData() throws Exception {
		
		/*
		 * Loop through all our fragments, split into 13mers and add to ampliconTiles 
		 */
		Set<String> ampliconTiles = new HashSet<>();
		for (String s : fragAndRCFrags) {
			int sLength = s.length();
			int noOfTiles = sLength / TILE_SIZE;
				
			for (int i = 0 ; i < noOfTiles ; i++) {
				ampliconTiles.add(s.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE));
			}
		}
		logger.info("Number of amplicon tiles: " + ampliconTiles.size());
		logger.info("loading genome tiles alignment data");
		
		try (TabbedFileReader reader = new TabbedFileReader(new File(refTiledAlignmentFile))) {
//			try (TabbedFileReader reader = new TabbedFileReader(new File(refTiledAlignmentFile));
//					FileWriter writer = new FileWriter(new File(refTiledAlignmentFile + ".condensed"))) {
			
			TabbedHeader header = reader.getHeader();
			List<String> headerList = new ArrayList<>();
			for (String head : header) {
				headerList.add(head);
			}
			positionToActualLocation.loadMap(headerList);
//			for (String head : header) {
//				writer.write(head + "\n");
//			}
			int i = 0;
			for (TabbedRecord rec : reader) {
				if (++i % 1000000 == 0) {
					logger.info("hit " + (i / 1000000) + "M records");
				}
				String tile = rec.getData().substring(0, TILE_SIZE);
				if (ampliconTiles.contains(tile)) {
					String countOrPosition = rec.getData().substring(rec.getData().indexOf('\t') + 1);
					if (countOrPosition.charAt(0) == 'C') {
						frequentlyOccurringRefTiles.add(tile);
					} else {
						
						TLongArrayList positionsList = new TLongArrayList();
						if (countOrPosition.indexOf(',') == -1) {
							positionsList.add(Long.parseLong(countOrPosition));
						} else {
							String [] positions =  TabTokenizer.tokenize(countOrPosition, ',');
							for (String pos : positions) {
								positionsList.add(Long.parseLong(pos));
							}
						}
						
						// create TLongArrayList from 
						refTilesPositions.put(tile, positionsList);
						
					}
//					writer.write(rec.getData() + "\n");
				}
			}
		}
		int refTilesCountSize =  frequentlyOccurringRefTiles.size();
		int refTilesPositionsSize =  refTilesPositions.size();
		int total = refTilesCountSize + refTilesPositionsSize;
		int diff = ampliconTiles.size() - total;
		logger.info("finished reading the genome tiles alignment data");
		logger.info("no of entries in refTilesCount: " + refTilesCountSize);
		logger.info("no of entries in refTilesPositions: " + refTilesPositionsSize);
		logger.info("Unique tiles in amplicons: " + diff);
		
		
		/*
		 * For each fragment, get best tile count
		 * Do same for fragment reverse complement, and see which one gives the better result
		 */
		int singleLocation = 0;
		int perfectMatch = 0;
		int multiLoci = 0;
		int unknown = 0;
		int existingStrandWins = 0;
		int rcStrandWins = 0;
		int bothStrandsWin = 0;
		for (String fragment : fragments.keySet()) {
			int fragLength = fragment.length();
			int noOfTiles = fragLength / TILE_SIZE;
			
//			LinkedHashMap<String, TLongArrayList> binSpecificTiles = new LinkedHashMap<>();
			
			long[][] tilePositions = new long[noOfTiles][];
			
			for (int i = 0 ; i < noOfTiles ; i++) {
				String bt = fragment.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE);
				
				if (frequentlyOccurringRefTiles.contains(bt)) {
					tilePositions[i] = new long[]{Long.MAX_VALUE};
				} else if (refTilesPositions.containsKey(bt)) {
					TLongArrayList refPositions = refTilesPositions.get(bt);
					tilePositions[i] = refPositions.toArray();
					Arrays.sort(tilePositions[i]);
				} else {
					tilePositions[i] = new long[]{Long.MIN_VALUE};
				}
			}
			
			long[][] rcTilePositions = new long[noOfTiles][];
			String rcFragment = SequenceUtil.reverseComplement(fragment);
			for (int i = 0 ; i < noOfTiles ; i++) {
				String bt = rcFragment.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE);
				
				if (frequentlyOccurringRefTiles.contains(bt)) {
					rcTilePositions[i] = new long[]{Long.MAX_VALUE};
				} else if (refTilesPositions.containsKey(bt)) {
					TLongArrayList refPositions = refTilesPositions.get(bt);
					rcTilePositions[i] = refPositions.toArray();
					Arrays.sort(tilePositions[i]);
				} else {
					rcTilePositions[i] = new long[]{Long.MIN_VALUE};
				}
			}
			
			/*
			 * See if we can walk through binSpecificTiles
			 */
			long [] results = ClinVarUtil.getBestStartPosition(tilePositions, TILE_SIZE, 2);
			long [] rcResults = ClinVarUtil.getBestStartPosition(rcTilePositions, TILE_SIZE, 2);
			
			long bestTileCount = results[1];
			long rcBestTileCount = rcResults[1];
			
			if (bestTileCount >= rcBestTileCount) {
				if (bestTileCount == rcBestTileCount) {
					bothStrandsWin++;
					logger.info("tile counts same for both strands, existing: " + Arrays.toString(results) + ", rc: " + Arrays.toString(rcResults) + " for fragment: " + fragment);
					
				} else {
					existingStrandWins++;
				}
				
				if (results.length > 2) {
					multiLoci++;
//					if (results[1] > 1) {		// just show larger ones for now...
//						logger.info("Have more than 1 best start positions: " + Arrays.toString(results) + " for fragment: " + fragment);
//					}
				} else if (results.length == 2) {
					singleLocation++;
					if (results[1] == noOfTiles) {
						perfectMatch++;
					}
				} else {
					unknown++;
				}
			} else {
				rcStrandWins++;
				if (rcResults.length > 2) {
					multiLoci++;
//					if (rcResults[1] > 1) {		// just show larger ones for now...
//						logger.info("Have more than 1 best start positions: " + Arrays.toString(rcResults) + " for fragment: " + fragment);
//					}
				} else if (rcResults.length == 2) {
					singleLocation++;
					if (rcResults[1] == noOfTiles) {
						perfectMatch++;
					}
				} else {
					unknown++;
				}
				
			}
			
			
		}
		logger.info("singleLocation: " + singleLocation + ", perfectMatch: " + perfectMatch + ", multiple Loci: " + multiLoci + ", unknown: " + unknown);
		logger.info("bothStrandsWin: " + bothStrandsWin + ", existingStrandWins: " + existingStrandWins + ", rcStrandWins: " + rcStrandWins);
		
	}
	
	
	
	public static void main(String[] args) throws Exception {
		// loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(Q3ClinVar2.class);
		
		Q3ClinVar2 qp = new Q3ClinVar2();
		int exitStatus = qp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		} else {
			System.err.println("Exit status: " + exitStatus);
		}
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getFastqs().length < 1) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			version = Q3ClinVar2.class.getPackage().getImplementationVersion();
			logger = QLoggerFactory.getLogger(Q3ClinVar2.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("q3clinvar", version, args);
			
			// get list of file names
			fastqFiles = options.getFastqs();
			if (fastqFiles.length < 1) {
				throw new Exception("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < fastqFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(fastqFiles[i])) {
						throw new Exception("INPUT_FILE_ERROR: "  +  fastqFiles[i]);
					}
				}
			}
			
			// set outputfile - if supplied, check that it can be written to
			if (null != options.getOutputFileName()) {
				String optionsOutputFile = options.getOutputFileName();
				if (FileUtils.canFileBeWrittenTo(optionsOutputFile)) {
					outputFile = optionsOutputFile;
				} else {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
			}
			
			refTiledAlignmentFile = options.getTiledRefFileName();
			refFileName = options.getRefFileName();
			
			xmlFile = options.getXml();
			if (options.hasMinBinSizeOption()) {
				this.minBinSize = options.getMinBinSize().intValue();
			}
			logger.info("minBinSize is " + minBinSize);
			
			
				return engage();
			}
		return returnStatus;
	}

}
