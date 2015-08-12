package au.edu.qimr.clinvar;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.util.SequenceUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.clinvar.model.Bin;
import au.edu.qimr.clinvar.model.FastqProbeMatch;
import au.edu.qimr.clinvar.model.IntPair;
import au.edu.qimr.clinvar.model.PositionChrPositionMap;
import au.edu.qimr.clinvar.model.Probe;
import au.edu.qimr.clinvar.util.ClinVarUtil;
import au.edu.qimr.clinvar.util.FastqProbeMatchUtil;

public class Q3ClinVar {
	
	private static QLogger logger;
	
	
	private static final int TILE_SIZE = 13;
	private static String[] fastqFiles;
	private static String version;
	private QExec qexec;
	private String logFile;
	private String xmlFile;
	private String outputDir;
	private String outputFileNameBase;
	private String refTiledAlignmentFile;
	private String refFileName;
	private int binId = 1;
	private int minBinSize = 10;
	private int exitStatus;
	
	private final Map<Integer, Map<String, Probe>> probeLengthMapR1 = new HashMap<>();
	private final Map<Integer, Map<String, Probe>> probeLengthMapR2 = new HashMap<>();
	private final Set<Probe> probeSet = new TreeSet<>();
	private final Map<Probe, List<Bin>> probeBinDist = new HashMap<>();
	private final Map<VcfRecord, List<Pair<Probe, Bin>>> mutations = new HashMap<>();
//	private final Map<Probe, IntPair> probeReadCountMap = new HashMap<>();
	
	private final Map<Probe, List<FastqProbeMatch>> probeDist = new HashMap<>();
	private final Map<Probe, Map<String, AtomicInteger>> probeFragments = new HashMap<>();
	private final Map<Probe, Map<IntPair, AtomicInteger>> probeMatchMap = new HashMap<>();
	
	// used to check for uniqueness
	private final Set<String> probeSequences = new HashSet<>();
	private final Set<FastqProbeMatch> matches = new HashSet<>();
	private final Set<String> frequentlyOccurringRefTiles = new HashSet<>();
	private final Map<String, TLongArrayList> refTilesPositions = new HashMap<>();
	private final PositionChrPositionMap positionToActualLocation = new PositionChrPositionMap();
	private final Map<String, byte[]> referenceCache = new HashMap<>();
	
	
	protected int engage() throws Exception {
		
		try {
			parseAmpliconManifestXmlFile();
			
			readFastqFiles();
			
			matchReadsToProbes();
			
			FastqProbeMatchUtil.getStats(matches);
			
			logger.info("Rescue reads using edit distances");
			findNonExactMatches();
			FastqProbeMatchUtil.getStats(matches);
			
			setupFragments();
			writeAmpliconDetails();
			binFragments();
			writeAmpliconDetailsAgain();
			
			/*
			 * Loops through the bins, and populates the mutations collection
			 * This needs to be run before we can output any variants
			 */
			extractMutationsFromBins(false, minBinSize);
			
			writeCsv(false);
			
//			writeDiagnosticOutput(false);
			
			writeBam(false);
			
			loadTiledAlignerData();
			/*
			 * Write output files again, this time filtering out bins if they don't sit close to the amplicon they have been assigned to
			 */
			mutations.clear();
			extractMutationsFromBins(true, minBinSize);
			writeCsv(true);
//			writeDiagnosticOutput(true);
			writeBam(true);
			writeAmpliconPerformanceCsv();
			writeDodgyBinReport();
			
//			writeOutput();
		} finally {
		}
		return exitStatus;
	}

	private void writeDodgyBinReport() throws IOException {
		try (FileWriter writer = new FileWriter(new File(outputFileNameBase +"lost_bins.csv"))) {
			
			/*
			 * Setup the header
			 */
			writer.write("#amplicon_id,amplicon_name,amplicon_position,bin_id,bin_read_count,bin_sequence_length,bin_position,amplicon_sw_score,bin_sw_score,amplicon_sw,bin_sw\n");
			
			for (Entry<Probe, List<Bin>> entry : probeBinDist.entrySet()) {
				Probe probe = entry.getKey();
				
				Set<Bin> binsSet = new TreeSet<>();
				List<Bin> allBins = entry.getValue();
				for (Bin b : allBins) {
					if ( null == b.getBestTiledLocation() || ! ClinVarUtil.doChrPosOverlap(probe.getCp(), b.getBestTiledLocation())) {
						binsSet.add(b);
					}
				}
				
				for (Bin b : binsSet) {
					
					String [] ampliconPositionSWDiffs = b.getSmithWatermanDiffs();
					if (null == ampliconPositionSWDiffs) {
						String sequence = probe.reverseComplementSequence() ? SequenceUtil.reverseComplement(b.getSequence()) : b.getSequence();
						ampliconPositionSWDiffs =  ClinVarUtil.getSwDiffs(probe.getReferenceSequence(), sequence);
					}
					
					StringBuilder tiledLocations = new StringBuilder();
					StringBuilder tiledLocationScores = new StringBuilder();
					StringBuilder tiledLocationDiffs = new StringBuilder();
					if (null != b.getBestTiledLocation()) {
						tiledLocations.append(b.getBestTiledLocation().toIGVString());
						tiledLocationScores.append(ClinVarUtil.getSmithWatermanScore(b.getSmithWatermanDiffs(b.getBestTiledLocation())));
						tiledLocationDiffs.append(b.getSmithWatermanDiffs(b.getBestTiledLocation())[0]);
						tiledLocationDiffs.append(b.getSmithWatermanDiffs(b.getBestTiledLocation())[1]);
						tiledLocationDiffs.append(b.getSmithWatermanDiffs(b.getBestTiledLocation())[2]);
					} else if (null != b.getSmithWatermanDiffsMap() && ! b.getSmithWatermanDiffsMap().isEmpty()) {
						for (Entry<ChrPosition, String []> entry2 : b.getSmithWatermanDiffsMap().entrySet()) {
							if (tiledLocations.length() > 0) {
								tiledLocations.append(Constants.SEMI_COLON);
								tiledLocationScores.append(Constants.SEMI_COLON);
								tiledLocationDiffs.append(Constants.SEMI_COLON);
							}
							tiledLocations.append(entry2.getKey().toIGVString());
							tiledLocationScores.append(ClinVarUtil.getSmithWatermanScore(entry2.getValue()));
							tiledLocationDiffs.append(entry2.getValue()[0]);
							tiledLocationDiffs.append(entry2.getValue()[1]);
							tiledLocationDiffs.append(entry2.getValue()[2]);
						}
					} else {
						tiledLocations.append("-");
						tiledLocationScores.append("-");
						tiledLocationDiffs.append("-");
					}
					
					StringBuilder sb = new StringBuilder();
					sb.append(probe.getId());
					sb.append(Constants.COMMA);
					sb.append(probe.getName());
					sb.append(Constants.COMMA);
					sb.append(probe.getCp().toIGVString());
					sb.append(Constants.COMMA);
					sb.append(b.getId());
					sb.append(Constants.COMMA);
					sb.append(b.getRecordCount());
					sb.append(Constants.COMMA);
					sb.append(b.getLength());
					sb.append(Constants.COMMA);
					sb.append(tiledLocations.toString());
					sb.append(Constants.COMMA);
					sb.append(ClinVarUtil.getSmithWatermanScore(ampliconPositionSWDiffs));
					sb.append(Constants.COMMA);
					sb.append(tiledLocationScores.toString());
					sb.append(Constants.COMMA);
					sb.append(ampliconPositionSWDiffs[0]);
					sb.append(ampliconPositionSWDiffs[1]);
					sb.append(ampliconPositionSWDiffs[2]);
					sb.append(Constants.COMMA);
					sb.append(tiledLocationDiffs.toString());
					sb.append("\n");
					writer.write(sb.toString());
				}
			}
			writer.flush();
		}
	}

	private void matchReadsToProbes() {
		
		/*
		 * Create lists of probe lengths
		 */
		List<Integer> primerLengthsR1 = new ArrayList<>(probeLengthMapR1.keySet());
		Collections.sort(primerLengthsR1);
		Collections.reverse(primerLengthsR1);
		for (Integer xyz : primerLengthsR1) {
			logger.info("no of probes with primer length " + xyz.intValue() + ": " + probeLengthMapR1.get(xyz).size());
		}
		
		List<Integer> primerLengthsR2 = new ArrayList<>(probeLengthMapR2.keySet());
		Collections.sort(primerLengthsR2);
		Collections.reverse(primerLengthsR2);
		for (Integer xyz : primerLengthsR2) {
			logger.info("no of probes with primer length " + xyz.intValue() + ": " + probeLengthMapR2.get(xyz).size());
		}
		
		int matchCount = 0;
		int mateMatchCount = 0;
		
		for (FastqProbeMatch fpm : matches) {
			
			final String read1 = fpm.getRead1().getReadString();
			final String read2 = fpm.getRead2().getReadString();
				
			boolean secondReadMatchFound = false;
			// loop through the maps searching for a match
			for (Integer pl : primerLengthsR1) {
				String read =read1.substring(0, pl.intValue());
					
				Map<String, Probe> map = probeLengthMapR1.get(pl);
					
					if (map.containsKey(read)) {
						Probe p = map.get(read);
						matchCount++;
						// set read1 probe
						fpm.setRead1Probe(p, 0);
						
						// now check to see if mate starts with the value in the map
						String mate = p.getUlsoSeq();
						if (read2.startsWith(mate)) {
							secondReadMatchFound = true;
							
							// set read2 probe
							fpm.setRead2Probe(p, 0);
							mateMatchCount++;
//							updateMap(p, probeDist);
						}
						// no need to look at other maps
						break;
					}
				}
				
				if ( ! secondReadMatchFound) {
					for (Integer pl : primerLengthsR2) {
						String read = read2.substring(0, pl.intValue());
						Map<String, Probe> map = probeLengthMapR2.get(pl);
						
						if (map.containsKey(read)) {
							Probe p = map.get(read);
							
							// set read2 probe
							fpm.setRead2Probe(p, 0);
							// no need to look at other maps
							break;
						}
					}
				}
		}
		logger.info("no of records in fastq file: " + matches.size() + ", and no of records that started with a probe in our set: " + matchCount + " and no whose mate also matched: " + mateMatchCount);
	}
	

	private void readFastqFiles() {
		int fastqCount = 0;
		// read in a fastq file and lits see if we get some matches
		try (FastqReader reader1 = new FastqReader(new File(fastqFiles[0]));
				FastqReader reader2 = new FastqReader(new File(fastqFiles[1]));) {
			
			for (FastqRecord rec : reader1) {
				FastqRecord rec2 = reader2.next();
				
				FastqProbeMatch fpm = new FastqProbeMatch(fastqCount++, rec, rec2);
				matches.add(fpm);
			}
		}
	}


	private void parseAmpliconManifestXmlFile() throws ParsingException, ValidityException, IOException {
		Builder parser = new Builder();
		Document doc = parser.build(xmlFile);
		Element root = doc.getRootElement();
		
		// get probes from doc
		Element probes = root.getFirstChildElement("Probes");
		Elements probesElements = probes.getChildElements();
		
		int i = 0;
		for ( ; i < probesElements.size() ; i++) {
			Element probe = probesElements.get(i);
			int id = Integer.parseInt(probe.getAttribute("id").getValue());
		
			String dlsoSeq = "";
			String ulsoSeq = "";
			String dlsoSeqRC = "";
			String ulsoSeqRC = "";
			String subseq = "";
			String chr = "";
			String name = "";
			boolean forwardStrand = false;
			int p1Start = -1;
			int p1End = -1;
			int p2Start = -1;
			int p2End = -1;
			int ssStart = -1;
			int ssEnd = -1;
//				int start = -1;
//				int end = -1;
			
			Elements probeElements = probe.getChildElements();
		
			for (int j = 0 ; j < probeElements.size() ; j++) {
				Element probeSubElement = probeElements.get(j);
				if ("DLSO_Sequence".equals(probeSubElement.getQualifiedName())) {
					dlsoSeq = probeSubElement.getValue();
				} else  if ("ULSO_Sequence".equals(probeSubElement.getQualifiedName())) {
					ulsoSeq = probeSubElement.getValue();
				} else if ("dlsoRevCompSeq".equals(probeSubElement.getQualifiedName())) {
					dlsoSeqRC = probeSubElement.getValue();
				} else  if ("ulsoRevCompSeq".equals(probeSubElement.getQualifiedName())) {
					ulsoSeqRC = probeSubElement.getValue();
				} else  if ("primer1Start".equals(probeSubElement.getQualifiedName())) {
					p1Start = Integer.parseInt(probeSubElement.getValue()) + 1;						// add 1 to make it 1-based
				} else  if ("primer1End".equals(probeSubElement.getQualifiedName())) {
					p1End = Integer.parseInt(probeSubElement.getValue()) + 1;						// add 1 to make it 1-based
				} else  if ("primer2Start".equals(probeSubElement.getQualifiedName())) {
					p2Start = Integer.parseInt(probeSubElement.getValue()) + 1;						// add 1 to make it 1-based
				} else  if ("primer2End".equals(probeSubElement.getQualifiedName())) {
					p2End = Integer.parseInt(probeSubElement.getValue()) + 1;						// add 1 to make it 1-based
				} else  if ("subseq".equals(probeSubElement.getQualifiedName())) {
					subseq = probeSubElement.getValue();
				} else  if ("subseqStart".equals(probeSubElement.getQualifiedName())) {
					ssStart =  Integer.parseInt(probeSubElement.getValue()) + 1;						// add 1 to make it 1-based
				} else  if ("subseqEnd".equals(probeSubElement.getQualifiedName())) {
					ssEnd =  Integer.parseInt(probeSubElement.getValue()) + 1;						// add 1 to make it 1-based
				} else  if ("Chromosome".equals(probeSubElement.getQualifiedName())) {
					chr =  probeSubElement.getValue();
				} else  if ("Probe_Strand".equals(probeSubElement.getQualifiedName())) {
					forwardStrand =  "+".equals(probeSubElement.getValue());
				} else  if ("Name_no_spaces".equals(probeSubElement.getQualifiedName())) {
					name =  probeSubElement.getValue();
				} 
// 					if ("Start_Position".equals(probeSubElement.getQualifiedName())) {
// 						start =  Integer.parseInt(probeSubElement.getValue());
// 					}
// 					if ("End_Position".equals(probeSubElement.getQualifiedName())) {
// 						end =  Integer.parseInt(probeSubElement.getValue());
// 					}
			}
			
			Probe p = new Probe(id, dlsoSeq, dlsoSeqRC, ulsoSeq, ulsoSeqRC, p1Start, p1End, p2Start, p2End, subseq, ssStart, ssEnd, chr, forwardStrand, name);
			probeSet.add(p);
			int primerLengthR1 = dlsoSeqRC.length();
			int primerLengthR2 = ulsoSeq.length();
			
			Map<String, Probe> mapR1 = probeLengthMapR1.get(primerLengthR1);
			if (null == mapR1) {
				mapR1 = new HashMap<>();
				probeLengthMapR1.put(primerLengthR1, mapR1);
			}
			mapR1.put(dlsoSeqRC, p);
			
			Map<String, Probe> mapR2 = probeLengthMapR2.get(primerLengthR2);
			if (null == mapR2) {
				mapR2 = new HashMap<>();
				probeLengthMapR2.put(primerLengthR2, mapR2);
			}
			mapR2.put(ulsoSeq, p);
			
			probeSequences.add(dlsoSeq);
			probeSequences.add(dlsoSeqRC);
			probeSequences.add(ulsoSeq);
			probeSequences.add(ulsoSeqRC);
			
		}
		logger.info("Found " + i + " probes in xml doc. No of entries in seq set is: " + probeSequences.size() + " which should be equals to : " + (4 * i));
	}
	
	private void loadTiledAlignerData() throws Exception {
		/*
		 * Loop through all our amplicons, split into 13mers and add to ampliconTiles 
		 */
		Set<String> ampliconTiles = new HashSet<>();
		for (Entry<Probe, List<Bin>> entry : probeBinDist.entrySet()) {
			List<Bin> bins = entry.getValue();
			for (Bin b : bins) {
				String s = b.getSequence();
				int sLength = s.length();
				int noOfTiles = sLength / TILE_SIZE;
				
				for (int i = 0 ; i < noOfTiles ; i++) {
					ampliconTiles.add(s.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE));
				}
				/*
				 * Now get the rev comp tiles
				 */
				s = SequenceUtil.reverseComplement(b.getSequence());
				for (int i = 0 ; i < noOfTiles ; i++) {
					ampliconTiles.add(s.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE));
				}
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
		
		int singleLocation = 0;
		int perfectMatch = 0;
		int multiLoci = 0;
		int unknown = 0;
		int existingStrandWins = 0;
		int rcStrandWins = 0;
		int bothStrandsWin = 0;
		for (Entry<Probe,List<Bin>> entry : probeBinDist.entrySet()) {
			Map<ChrPosition, AtomicInteger> binLocationDistribution = new HashMap<>();
			Probe p = entry.getKey();
			ChrPosition ampliconCP = p.getCp();
//			logger.info("Looking at probe: " + p.getId() +", which has " +  entry.getValue().size() + " bins");
			
			for (Bin b : entry.getValue()) {
				String fragment = b.getSequence();
				int sLength = fragment.length();
				int noOfTiles = sLength / TILE_SIZE;
				long[][] tilePositions = new long[noOfTiles][];
				
				/*
				 * Break fragment up into tiles, and see if we have seen these tiles in the tiled genome file
				 */
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
				
				/*
				 * And now reverse complement and run again
				 */
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
				long [] results = ClinVarUtil.getBestStartPosition(tilePositions, TILE_SIZE, 5);
				long [] rcResults = ClinVarUtil.getBestStartPosition(rcTilePositions, TILE_SIZE, 5);
				
				long bestTileCount = results[1];
				long rcBestTileCount = rcResults[1];
				
				/*
				 * Only perform sw on positions if the best tile position is not next to the amplicon position
				 */
				ChrPosition bestTiledCp = null;
				if (bestTileCount > rcBestTileCount) {
					if (results.length == 2) {
						bestTiledCp = positionToActualLocation.getChrPositionFromLongPosition(results[0]);
					}
				} else if (bestTileCount < rcBestTileCount) {
					if (rcResults.length == 2) {
						bestTiledCp = positionToActualLocation.getChrPositionFromLongPosition(rcResults[0]);
					}
				}
				
				if (null != bestTiledCp && ClinVarUtil.doChrPosOverlap(ampliconCP, bestTiledCp)) {
					b.setBestTiledLocation(bestTiledCp);
					updateMap(bestTiledCp, binLocationDistribution);
				} else {
				
					/*
					 * Haven't got a best tiled location, or the location is nor near the amplicon, so lets generate some SW diffs, and choose the best location based on those
					 */
					if (bestTileCount > 1) {
						Map<ChrPosition, String[]> scores = getSWScores(results, b.getSequence());
						b.addPossiblePositions(scores);
					}
					if (rcBestTileCount > 1) {
						Map<ChrPosition, String[]> scores = getSWScores(rcResults, SequenceUtil.reverseComplement(b.getSequence()));
						b.addPossiblePositions(scores);
					}
					bestTiledCp = ClinVarUtil.getPositionWithBestScore(b.getSmithWatermanDiffsMap());
					if (null != bestTiledCp) {
						b.setBestTiledLocation(bestTiledCp);
						updateMap(bestTiledCp, binLocationDistribution);
					}
				}
			}
			/*
			 * Check to see if the bins sit close to the amplicon
			 */
			boolean logProbeDetails = false;
			for (Entry<ChrPosition, AtomicInteger> entry4 : binLocationDistribution.entrySet()) {
				if ( ! ClinVarUtil.doChrPosOverlap(ampliconCP, entry4.getKey())) {
					logProbeDetails = true;
					break;
				}
			}
			if (logProbeDetails) {
				logger.info("probe: " + p.getId() + " : " + p.getCp().toIGVString() + " has following distribution: ");
				for (Entry<ChrPosition, AtomicInteger> entry4 : binLocationDistribution.entrySet()) {
					logger.info("cp: " + entry4.getKey().toIGVString() + " : " + entry4.getValue().intValue());
				}
			}
		}
		logger.info("singleLocation: " + singleLocation + ", perfectMatch: " + perfectMatch + ", multiple Loci: " + multiLoci + ", unknown: " + unknown);
		logger.info("bothStrandsWin: " + bothStrandsWin + ", existingStrandWins: " + existingStrandWins + ", rcStrandWins: " + rcStrandWins);
	}
	
	private Map<ChrPosition, String[]> getSWScores(long [] positionCountArray, String binSequence ) throws IOException {
		Map<ChrPosition, String[]> positionSWDiffMap = new HashMap<>(positionCountArray.length);
		int noOfCompetingPositions = positionCountArray.length / 2;
		for (int i = 0 ; i < noOfCompetingPositions ; i++) {
			long position = positionCountArray[i * 2];
			ChrPosition cp = positionToActualLocation.getChrPositionFromLongPosition(position);
			ChrPosition refCp = positionToActualLocation.getBufferedChrPositionFromLongPosition(position, binSequence.length(), 200);
			
			String ref = getRefFromChrPos(refCp);
			positionSWDiffMap.put(cp, ClinVarUtil.getSwDiffs(ref, binSequence));
		}
		return positionSWDiffMap;
	}

	private String getRefFromChrPos(ChrPosition cp) {
		String referenceSeq = null;
		String chr = cp.getChromosome();
		byte[] ref = referenceCache.get(chr);
		if ( null == ref) {
			/*
			 * Load from file
			 */
			FastaSequenceIndex index = new FastaSequenceIndex(new File(refFileName + ".fai"));
			try (IndexedFastaSequenceFile refFile = new IndexedFastaSequenceFile(new File(refFileName), index);) {;
				ReferenceSequence refSeq = refFile.getSequence(chr);
				ref = refSeq.getBases();
				referenceCache.put(chr, ref);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (cp.getPosition() <= 0 || cp.getEndPosition() > ref.length) {
			logger.warn("ChrPosition goes beyond edge of contig: " + cp.toIGVString() + ", ref length: " + ref.length);
		}
		byte [] refPortion = Arrays.copyOfRange(referenceCache.get(chr), cp.getPosition(), cp.getEndPosition());
		referenceSeq = new String(refPortion);
		
		return referenceSeq;
	}

	private void writeDiagnosticOutput(boolean filter) throws IOException {
		
		List<FastqProbeMatch> multiMatched = new ArrayList<>();
		List<FastqProbeMatch> oneEndMatched = new ArrayList<>();
		List<FastqProbeMatch> noEndsMatched = new ArrayList<>();
		List<FastqProbeMatch> noFragment = new ArrayList<>();
		
		for (FastqProbeMatch fpm : matches) {
			
			if (FastqProbeMatchUtil.isMultiMatched(fpm)) {
				multiMatched.add(fpm);
			} else if (FastqProbeMatchUtil.neitherReadsHaveAMatch(fpm)) {
				noEndsMatched.add(fpm);
			} else if (FastqProbeMatchUtil.onlyOneReadHasAMatch(fpm)) {
				oneEndMatched.add(fpm);
			} else if (StringUtils.isNullOrEmpty(fpm.getFragment())) {
				noFragment.add(fpm);
			}
		}
		
		logger.info("multiMatched: " + multiMatched.size());
		logger.info("oneEndMatched: " + oneEndMatched.size());
		logger.info("noEndsMatched: " + noEndsMatched.size());
		logger.info("noFragment: " + noFragment.size());
		
		String outputFileName = filter ? outputDir+".diagnostic.multi.matched.filtered.csv" : outputDir+".diagnostic.multi.matched.csv";
		
		if ( ! multiMatched.isEmpty()) {
			try (FileWriter writer = new FileWriter(new File(outputFileName))){;
				writer.write("r1_probe_id,r2_probe_id,r1,r2" );
				writer.write("\n");
				
				for (FastqProbeMatch fpm : multiMatched) {
					writer.write(fpm.getRead1Probe().getId() + "," + fpm.getRead2Probe().getId() + "," + fpm.getRead1().getReadString() + "," + fpm.getRead2().getReadString());
					writer.write("\n");
				}
				writer.flush();
			}
		}
		
		outputFileName = filter ? outputDir+".diagnostic.no.fragment.filtered.csv" : outputDir+".diagnostic.no.fragment.csv";
		if ( ! noFragment.isEmpty()) {
			try (FileWriter writer = new FileWriter(new File(outputFileName))){;
			writer.write("r1_probe_id,r2_probe_id,r1,r2" );
			writer.write("\n");
			
			for (FastqProbeMatch fpm : noFragment) {
				writer.write(fpm.getRead1Probe().getId() + "," + fpm.getRead2Probe().getId() + "," + fpm.getRead1().getReadString() + "," + fpm.getRead2().getReadString());
				writer.write("\n");
			}
			writer.flush();
			}
		}
		
		outputFileName = filter ? outputDir+".diagnostic.one.end.matched.filtered.csv" : outputDir+".diagnostic.one.end.matched.csv";
		if ( ! oneEndMatched.isEmpty()) {
			try (FileWriter writer = new FileWriter(new File(outputFileName))){;
			writer.write("r1_probe_id,r2_probe_id,r1,r2" );
			writer.write("\n");
			
			for (FastqProbeMatch fpm : oneEndMatched) {
				String p1 = null != fpm.getRead1Probe() ? fpm.getRead1Probe().getId() + "" : "-";
				String p2 = null != fpm.getRead2Probe() ? fpm.getRead2Probe().getId() + "" : "-";
				writer.write(p1 + "," + p2 + "," + fpm.getRead1().getReadString() + "," + fpm.getRead2().getReadString());
				writer.write("\n");
			}
			writer.flush();
			}
		}
		
		outputFileName = filter ? outputDir+".diagnostic.no.end.matched.filtered.csv" : outputDir+".diagnostic.no.end.matched.csv";
		if ( ! noEndsMatched.isEmpty()) {
			try (FileWriter writer = new FileWriter(new File(outputFileName))){;
			writer.write("r1,r2" );
			writer.write("\n");
			
			for (FastqProbeMatch fpm : noEndsMatched) {
				writer.write(fpm.getRead1().getReadString() + "," + fpm.getRead2().getReadString());
				writer.write("\n");
			}
			writer.flush();
			}
		}
	}
	
	private void writeAmpliconPerformanceCsv() throws IOException {
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		List<Probe> coordSortedProbes = new ArrayList<>(probeSet);
		Collections.sort(coordSortedProbes, new Comparator<Probe>() {
			@Override
			public int compare(Probe o1, Probe o2) {
				return o1.getCp().compareTo(o2.getCp());
			}
		});
		
		final int fastqRecordCount = matches.size();
		
		try (FileWriter writer = new FileWriter(new File(outputFileNameBase + "amplicon_performance.csv" ))) {
			writer.write("#amplicon manifest file: " + xmlFile + Constants.NEW_LINE);
			writer.write("#fastq r1 file: " + fastqFiles[0] + Constants.NEW_LINE);
			writer.write("#fastq r2 file: " + fastqFiles[1] + Constants.NEW_LINE);
			writer.write("#Number of fastq records: " + fastqRecordCount + Constants.NEW_LINE);
			writer.write("#Number of amplicons: " + probeSet.size() + Constants.NEW_LINE);
			writer.write("#\n");
			writer.write("amplicon_id,amplicon_name,amplicon_position,number_of_reads,numbe_of_reads_percentage,number_of_bins,bins_on_target,bins_on_target_percentage,reads_on_target,reads_on_target_percentage\n" );
			
			for (Probe p : coordSortedProbes) {
				ChrPosition ampliconCP = p.getCp();
				List<Bin> bins = probeBinDist.get(p);
				int recordCount = 0;
				int binsOnTarget = 0;
				int readsOnTarget = 0;
				int binCount = 0;
				if (null != bins) {
					binCount = bins.size();
					for (Bin b : bins) {
						recordCount += b.getRecordCount();
						if ( null != b.getBestTiledLocation() && ClinVarUtil.doChrPosOverlap(ampliconCP,b.getBestTiledLocation())) {
							binsOnTarget++;
							readsOnTarget += b.getRecordCount();
						}
					}
				}
				double recordCountPercentage = fastqRecordCount > 0 ? ((double)recordCount / fastqRecordCount) * 100 : 0;
				double binsOnTargetPercentage = binCount > 0 ? ((double)binsOnTarget / binCount) * 100 : 0;
				double readsOnTargetPercentage = recordCount > 0 ? ((double)readsOnTarget / recordCount) * 100 : 0;
				
				writer.write(p.getId() + "," + p.getName() + "," + p.getCp().toIGVString() + "," + recordCount + "," +df.format(recordCountPercentage) + "," 
				+ binCount + "," + binsOnTarget + "," +df.format(binsOnTargetPercentage) + "," + readsOnTarget + "," + df.format(readsOnTargetPercentage) + "\n");
				
			}
			writer.flush();
		}
	}
	
	private void writeBam(boolean filter) throws IOException {
		
		List<Probe> coordSortedProbes = new ArrayList<>(probeSet);
		Collections.sort(coordSortedProbes, new Comparator<Probe>() {
			@Override
			public int compare(Probe o1, Probe o2) {
				return o1.getCp().compareTo(o2.getCp());
			}
		});
		
		String outputFileName = filter ? outputFileNameBase + "bam" : outputFileNameBase + "diag.unbinned.bam";
		File bamFile = new File(outputFileName);
		SAMFileHeader header = new SAMFileHeader();
		header.setSequenceDictionary(ClinVarUtil.getSequenceDictionaryFromProbes(coordSortedProbes));
		header.setSortOrder(SortOrder.coordinate);
		SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		factory.setCreateIndex(true);
		SAMFileWriter writer = factory.makeSAMOrBAMWriter(header, false, bamFile);
		
		long recordCount = 0;
		int indelCount = 0;
		int indelSameLength = 0;
		int indelDiffLength = 0;
		int sameSize = 0, diffSize = 0, noDiffInFirs20 = 0;
		try {

			/*
			 * loop through probes
			 * output bins of size greater than minBinSize 
			 */
			for (Probe p : coordSortedProbes) {
				ChrPosition ampliconCP = p.getCp();
				int probeId = p.getId();
				boolean reverseComplementSequence = p.reverseComplementSequence();
				final String referenceSequence = p.getReferenceSequence();
				final String bufferedReferenceSequence = p.getBufferedReferenceSequence();
				List<Bin> bins = probeBinDist.get(p);
				if (null != bins) {
					for (Bin b : bins) {
						
						if ( ! filter || (null != b.getBestTiledLocation() && ClinVarUtil.doChrPosOverlap(ampliconCP, b.getBestTiledLocation()))) {
						
							int binId = b.getId();
							recordCount += b.getRecordCount();
							
							String binSeq = reverseComplementSequence ?  SequenceUtil.reverseComplement(b.getSequence()) : b.getSequence() ;
							
							/*
							 * Just print ones that match the ref for now - makes ceegar easier..
							 */
							int offset = referenceSequence.indexOf(binSeq);
							int bufferedOffset = -1;
							if (offset == -1) {
								/*
								 *  try running against bufferedRefSeq
								 */
								bufferedOffset = bufferedReferenceSequence.indexOf(binSeq);
								if (bufferedOffset >= 0) {
									logger.info("got a match against the buffered reference!!! p: " + p.getId() + ", bin: " + b.getId());
//									if (p.getId() == 121 && b.getId() == 1272) {
//										logger.info("referenceSequence: " + referenceSequence);
//										logger.info("bufferedReferenceSequence: " + bufferedReferenceSequence);
//										logger.info("binSeq: " + binSeq);
//									}
								}
							}
							if (offset >= 0) {
								/*
								 * Perfect Match
								 */
								Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(b.getLength());
								ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), referenceSequence, p.getCp().getChromosome(), p.getCp().getPosition(), offset, binSeq);
								
							} else if (bufferedOffset >= 0) {
								/*
								 * Perfect Match against buffered reference
								 */
								Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(b.getLength());
								ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), bufferedReferenceSequence, p.getCp().getChromosome(), p.getCp().getPosition() - 10, bufferedOffset, binSeq);
								
							} else {
								
								/*
								 * bin sequence differs from reference
								 */
								String [] swDiffs = b.getSmithWatermanDiffs();
								
								if (null != swDiffs) {
									if ( ! swDiffs[1].contains(" ")) {
										/*
										 * Only snps
										 */
										if (swDiffs[1].length() == referenceSequence.length()) {
											// just snps and same length
											Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(b.getLength());
											ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), referenceSequence, p.getCp().getChromosome(), p.getCp().getPosition(), 0, binSeq);
										} else {
											logger.info("only snps but diff length to ref. bin: " + b.getId() + ", p: " + p.getId() + ", binSeq: " + binSeq + ", ref: " + referenceSequence);
											for (String s : swDiffs) {
												logger.info("s: " + s);
											}
											Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(b.getLength());
											ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), referenceSequence, p.getCp().getChromosome(), p.getCp().getPosition(), 0, binSeq);
										}
									} else {
										
										Cigar cigar = ClinVarUtil.getCigarForIndels( referenceSequence,  binSeq, swDiffs,  p,  b);
										ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), referenceSequence, p.getCp().getChromosome(), p.getCp().getPosition(), 0, binSeq);
									}
								} else {
//									logger.warn("Not generating SAMRecord for bin: " + b.getId() + " as no sw diffs. getRecordCount: " + b.getRecordCount());
								}
							}
						}
					}
				}
			}
			
		} finally {
			writer.close();
		}
		logger.info("indelDiffLength: " + indelDiffLength + ", indelSameLength: " + indelSameLength);
		logger.info("No of records written to bam file: " + recordCount + ", no of bins with same seq size as ref: " + sameSize +", and diff size: " + diffSize + ", indelCount: " + indelCount + ", noDiffInFirs20: " + noDiffInFirs20);
	}
	
	
	private void extractMutationsFromBins(boolean onlyUseBinsThatMatchAmplicons, int binRecordCount) {
		
		for (Entry<Probe, List<Bin>> entry : probeBinDist.entrySet()) {
			Probe p = entry.getKey();
			ChrPosition ampliconCP = p.getCp();
			List<Bin> bins = entry.getValue();
			String bufferedRef = p.getBufferedReferenceSequence();
			boolean reverseComplementSequence = p.reverseComplementSequence();
			
			// get largest bin
			if (null != bins && ! bins.isEmpty()) {
				
				for (Bin b : bins) {
					
					// only care about bins that have more than 10 reads
					if (b.getRecordCount() >= binRecordCount) {
						
						if ( ! onlyUseBinsThatMatchAmplicons || (null != b.getBestTiledLocation() && ClinVarUtil.doChrPosOverlap(ampliconCP, b.getBestTiledLocation()))) {
							
							String binSeq = reverseComplementSequence ? SequenceUtil.reverseComplement(b.getSequence()) :  b.getSequence();
							
							// check that ref length is equal to bin length
							// if shorter, get a longer ref
							int lengthDiff = binSeq.length() - bufferedRef.length();
							if (lengthDiff > 0) {
								logger.warn("bufferef ref length is less than bin length!!!");
								bufferedRef = p.getBufferedReferenceSequence(lengthDiff);
							}
							
							
							String [] diffs =  ClinVarUtil.getSwDiffs(bufferedRef, binSeq, true);
							
							b.setSWDiffs(diffs);
							
//							if (p.getId() == 42) {
//								logger.info("probe: " + p.getId() + ", forward strand: " + p.isOnForwardStrand() + ", size: " + b.getRecordCount() + ", ref: " + ref + ", binSeq: " + binSeq);
//								for (String s : b.getSmithWatermanDiffs()) {
//									logger.info(s);
//								}
//							}
							if ( ! bufferedRef.contains(binSeq)) {
								createMutations(p, b);
							}
//							if ( ! binSeq.equals(bufferedRef)) {
//								createMutations(p, b);
//							}
						}
					}
				}
			}
		}
	}

	private void writeCsv(boolean filter) throws IOException {
		
		/*
		 * Write haplotype file before rolling up mutations
		 */
		if (filter) {
			Map<Probe,Map<Bin,List<VcfRecord>>> mutationsByBin = new HashMap<>();
			for (Entry<VcfRecord, List<Pair<Probe,Bin>>> entry : mutations.entrySet()) {
				List<Pair<Probe,Bin>> list = entry.getValue();
				
				if (null == list) {
					logger.info("Found null list for vcf: " + entry.getKey().toString());
					continue;
				}
				
				for (Pair<Probe,Bin> pair : list) {
					Map<Bin, List<VcfRecord>> binVcfs = mutationsByBin.get(pair.getLeft());
					if (null == binVcfs) {
						binVcfs = new HashMap<>();
						mutationsByBin.put(pair.getLeft(), binVcfs);
					}
					List<VcfRecord> vcfs = binVcfs.get(pair.getRight());
					if (null == vcfs) {
						vcfs = new ArrayList<>();
						binVcfs.put(pair.getRight(), vcfs);
					}
					vcfs.add(entry.getKey());
				}
			}
			
			List<Probe> orderedProbes = new ArrayList<>(mutationsByBin.keySet());
			Collections.sort(orderedProbes);
		
			try (FileWriter writer = new FileWriter(new File(outputFileNameBase +"haplotypes.csv"))) {
				
				/*
				 * Setup the header
				 */
				writer.write("#amplicon_id,amplicon_name,bin_id,bin_read_count,mutations\n");
				
				for (Probe probe : orderedProbes) {
					
					Set<Bin> binsSet = new TreeSet<>(mutationsByBin.get(probe).keySet());
					binsSet.addAll( getEligibleBins(probe));
					
					for (Bin b : binsSet) {
						
						StringBuilder sb = new StringBuilder();
						sb.append(probe.getId());
						sb.append(Constants.COMMA);
						sb.append(probe.getName());
						sb.append(Constants.COMMA);
						sb.append(b.getId());
						sb.append(Constants.COMMA);
						sb.append(b.getRecordCount());
						
						List<VcfRecord> vcfs = mutationsByBin.get(probe).get(b);
						if (null != vcfs &&  ! vcfs.isEmpty()) {
							sb.append(Constants.COMMA);
							Collections.sort(vcfs);
							for (VcfRecord vcf : vcfs) {
								sb.append(vcf.getChromosome()).append(Constants.COLON).append(vcf.getPosition()).append(vcf.getRef()).append(">").append(vcf.getAlt()).append(Constants.SEMI_COLON);
							}
							sb.deleteCharAt(sb.length() -1);
						}
						sb.append("\n");
						writer.write(sb.toString());
					}
				}
				writer.flush();
			}
		}
		
		List<VcfRecord> sortedPositions = new ArrayList<>(mutations.keySet());
		Collections.sort(sortedPositions);
		
//		String outputFileName = filter ?  outputDir+".mutations.filtered.csv" : outputDir+".mutations.csv";
//		try (FileWriter writer = new FileWriter(new File(outputFileName))) {
//			writer.write("chr,position,ref,alt,probe_id,probe_total_reads,probe_total_frags,bin_id,bin_record_count" );
//			writer.write("\n");
//			
//			
//			for (VcfRecord vcf : sortedPositions) {
//				List<Pair<Probe, Bin>> list = mutations.get(vcf);
//				for (Pair<Probe, Bin> pair : list) {
//					Probe p = pair.getLeft();
//					Bin b = pair.getRight();
//					// get some probe stats
//					
//					//loop through fmps and for this probe, get number that have frags, and number that don't
//					IntPair ip = probeReadCountMap.get(p);
//					int totalReads = ip.getInt1();
//					int totalFrags = ip.getInt2();
//					writer.write(vcf.getChromosome() + "," + vcf.getPosition() + "," + vcf.getRef() + "," + vcf.getAlt() + "," + p.getId() + "," + totalReads + "," + totalFrags + "," + b.getId() + "," + b.getRecordCount());
//					writer.write("\n");
//				}
//			}
//			writer.flush();
//		}
		
		
		logger.info("pre-rollup no of mutataions: " + mutations.size());
		
		rollupMutations();
		
		logger.info("post-rollup no of mutataions: " + mutations.size());
		
		sortedPositions = new ArrayList<>(mutations.keySet());
		Collections.sort(sortedPositions);
		logger.info("no of vcf positions for vcf file: " + sortedPositions.size());
		
		String outputFileName = filter ?  outputFileNameBase+"vcf" : outputFileNameBase+"diag.unfiltered.vcf";
		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileName))) {
			
			/*
			 * Setup the VcfHeader
			 */
			final DateFormat df = new SimpleDateFormat("yyyyMMdd");
			VcfHeader header = new VcfHeader();
			header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + df.format(Calendar.getInstance().getTime()));		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + QExec.createUUid());		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=q3ClinVar");		
			header.addFormatLine("BB", ".","String","Breakdown of Bins containing more than 1 read at this position in the following format: Base,NumberOfReadsSupportingBase,NumberOfAmplicons/NumberOfBins.... "
					+ "NOTE that only bins with number of reads greater than " + minBinSize + " will be shown here");
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT);
			
			Iterator<Record> iter = header.iterator();
			while (iter.hasNext()) {
				writer.addHeader(iter.next().toString() );
			}
			
			for (VcfRecord vcf : sortedPositions) {
				/*
				 * get amplicons that overlap this position
				 */
				List<Probe> overlappingProbes = ClinVarUtil.getAmpliconsOverlappingPosition(vcf.getChrPosition(), probeSet);
				
				if (overlappingProbes.isEmpty()) {
					logger.warn("Found no amplicons overlapping position: " + vcf.getChrPosition());
				}
				String format = ClinVarUtil.getAmpliconDistribution(vcf, overlappingProbes, probeBinDist, minBinSize, false, filter);
				List<String> ff = new ArrayList<>(4);
				ff.add("BB");
				ff.add(ClinVarUtil.getSortedBBString(format, vcf.getRef()));
				vcf.setFormatFields(ff);
				
				writer.add(vcf);
			}
		}
		
		outputFileName = filter ?  outputFileNameBase+"diag.detailed.vcf" : outputFileNameBase+"diag.unfiltered_detailed.vcf";
		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileName))) {
			
			/*
			 * Setup the VcfHeader
			 */
			final DateFormat df = new SimpleDateFormat("yyyyMMdd");
			VcfHeader header = new VcfHeader();
			header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + df.format(Calendar.getInstance().getTime()));		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + QExec.createUUid());		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=q3ClinVar");		
			header.addFormatLine("BB", ".","String","Breakdown of Bins containing more than 1 read at this position in the following format: Base,NumberOfReadsSupportingBase,AmpliconID/BinID(# of reads in bin),AmpliconID/BinID(# of reads in bin).... "
					+ "NOTE that only bins with number of reads greater than " + minBinSize + " will be shown here");
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT);
			
			Iterator<Record> iter = header.iterator();
			while (iter.hasNext()) {
				writer.addHeader(iter.next().toString() );
			}
			
			for (VcfRecord vcf : sortedPositions) {
				/*
				 * get amplicons that overlap this position
				 */
				List<Probe> overlappingProbes = ClinVarUtil.getAmpliconsOverlappingPosition(vcf.getChrPosition(), probeSet);
				
				if (overlappingProbes.isEmpty()) {
					logger.warn("Found no amplicons overlapping position: " + vcf.getChrPosition());
				}
				String format = ClinVarUtil.getAmpliconDistribution(vcf, overlappingProbes, probeBinDist, minBinSize, true, filter);
				List<String> ff = new ArrayList<>(4);
				ff.add("BB");
				ff.add(ClinVarUtil.getSortedBBString(format, vcf.getRef()));
				vcf.setFormatFields(ff);
				
				writer.add(vcf);
			}
		}
	}
	
	private List<Bin> getEligibleBins(Probe p) {
		List<Bin> bins = new ArrayList<>();
		List<Bin> allBins = probeBinDist.get(p);
		for (Bin b : allBins) {
			if (b.getRecordCount() >= 10 && null != b.getBestTiledLocation() && ClinVarUtil.doChrPosOverlap(p.getCp(), b.getBestTiledLocation())) {
				bins.add(b);
			}
		}
		return bins;
	}

	private void rollupMutations() {
		
		Map<ChrPosition, Set<VcfRecord>> potentialRollups = new HashMap<>();
		for (VcfRecord vcf : mutations.keySet()) {
			/*
			 * check to see if there are other mutations in this collection that are at the same position
			 */
			for (VcfRecord vcf2 : mutations.keySet()) {
				if (vcf != vcf2) {
					if (vcf.getChrPosition().getChromosome().equals(vcf2.getChrPosition().getChromosome()) && vcf.getChrPosition().getPosition() == vcf2.getChrPosition().getPosition()) {
						
						ChrPosition cp = new ChrPosition(vcf.getChrPosition().getChromosome(), vcf.getChrPosition().getPosition());
						Set<VcfRecord> records = potentialRollups.get(cp);
						if (null == records) {
							records = new HashSet<>();
							potentialRollups.put(cp, records);
						}
						records.add(vcf);
						records.add(vcf2);
					}
				}
			}
		}
		logger.info("no of positions in potentialRollups: " + potentialRollups.size());
		
		
//		for (Entry<ChrPosition, Set<VcfRecord>> entry : potentialRollups.entrySet()) {
//			for (VcfRecord vcf: entry.getValue()) {
//				logger.info("vcf: " + vcf.toString());
//			}
//		}
		
		/*
		 * Rollup
		 */
		for (Entry<ChrPosition, Set<VcfRecord>> entry : potentialRollups.entrySet()) {
			VcfRecord mergedRecord = VcfUtils.mergeVcfRecords(entry.getValue());
			
			for (VcfRecord vcf: entry.getValue()) {
				mutations.remove(vcf);
			}
			mutations.put(mergedRecord,null);
		}
		
	}

	private void createMutations(Probe p, Bin b) {
		
		String [] smithWatermanDiffs = b.getSmithWatermanDiffs();
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(smithWatermanDiffs);
		
		if ( ! mutations.isEmpty()) {
		
//			String probeRef = p.getReferenceSequence();
			// remove any indel characters - only checking
			String swRef = smithWatermanDiffs[0].replace("-", "");
			int startPositionOfSWDiffs = p.getSubReferencePosition(swRef);
			
			if ( startPositionOfSWDiffs == -1) {
				logger.warn("p.getSubReferencePosition(swRef) == -1!!! probe (id:bin.id: " + p.getId() + ":" + b.getId() + ") , swRef: " + swRef);
			}
//			int offset = probeRef.indexOf(swRef);
//			
//			if ( offset == -1) {
//				logger.warn("probeRef.indexOf(swRef) == -1!!! probe (id:bin.id: " + p.getId() + ":" + b.getId() + ") , probe ref: " + probeRef + ", swRef: " + swRef);
//			}
			
			for (Pair<Integer, String> mutation : mutations) {
				int position = mutation.getLeft().intValue();
				String mutString = mutation.getRight();
				int slashIndex = mutString.indexOf('/');
				String ref = mutString.substring(0, slashIndex);
				String alt = mutString.substring(slashIndex + 1);
	//			if (p.getId() == 542 && b.getId() == 87424 ) {
	//				logger.info("position: " + position + ", mutString: " + mutString);
	//			}
				createMutation(p, b, position + startPositionOfSWDiffs, ref, alt);
			}
		}
	}
	
	private void createMutation(Probe p, Bin b, int position, String ref, String alt) {
//		int startPos = p.getCp().getPosition() + position;
//		int endPos = ref.length() > 1 ? startPos + (ref.length() -1 ) : startPos;
		VcfRecord vcf = VcfUtils.createVcfRecord(new ChrPosition(p.getCp().getChromosome(),  position, (position + ref.length() -1)), "."/*id*/, ref, alt);
		List<Pair<Probe, Bin>> existingBins = mutations.get(vcf);
		if (null == existingBins) {
			existingBins = new ArrayList<>();
			mutations.put(vcf, existingBins);
		}
		existingBins.add(new Pair<Probe,Bin>(p,b));
	}
	
	private void binFragments() {
		for (Entry<Probe, Map<String, AtomicInteger>> entry : probeFragments.entrySet()) {
			 Map<String, AtomicInteger> frags = entry.getValue();
			 if (null != frags && ! frags.isEmpty()) {
			
				 List<Bin> bins = convertFragmentsToBins(frags);
				 Collections.sort(bins);
				 // keep this for vcf building
				 probeBinDist.put(entry.getKey(), bins);
			 }
		}
	}
		
	private void writeAmpliconDetailsAgain() {
		
		// logging and writing to file
		Element amplicons = new Element("Amplicons");
		amplicons.addAttribute(new Attribute("amplicon_file", xmlFile));
		amplicons.addAttribute(new Attribute("fastq_file_1", fastqFiles[0]));
		amplicons.addAttribute(new Attribute("fastq_file_2", fastqFiles[1]));
		
		for (Probe p : probeSet) {
			
			Element fragments = createAmpliconElement(amplicons, p);
			
			// fragments next
			Map<String, AtomicInteger> frags = probeFragments.get(p);
			if (null != frags && ! frags.isEmpty()) {
				
				// I want the old fragment breakdown so that a comparison can be made
				TIntArrayList fragMatches = new TIntArrayList();
				for (Entry<String, AtomicInteger> entry : frags.entrySet()) {
					if (entry.getKey() != null) {
						fragMatches.add(entry.getValue().get());
					}
				}
				if (fragMatches.size() > 1) {
					fragments.addAttribute(new Attribute("pre-rescue_fragment_breakdown", "" + ClinVarUtil.breakdownEditDistanceDistribution(fragMatches)));
				}
				
				List<Bin> bins = probeBinDist.get(p);
				
				//reset
				fragMatches.clear();
				for (Bin b : bins) {
						Element fragment = new Element("Fragment");
						fragments.appendChild(fragment);
						fragment.addAttribute(new Attribute("fragment_length", "" + b.getLength()));
						fragment.addAttribute(new Attribute("record_count", "" + b.getRecordCount()));
						fragment.addAttribute(new Attribute("diffs", "" + b.getDifferences()));
						fragment.addAttribute(new Attribute("id", "" + b.getId()));
						fragment.appendChild(b.getSequence());
						fragMatches.add(b.getRecordCount());
				}
				if (fragMatches.size() > 1) {
					fragments.addAttribute(new Attribute("fragment_breakdown", "" + ClinVarUtil.breakdownEditDistanceDistribution(fragMatches)));
				}
			}
		}
		
		// write output
		Document doc = new Document(amplicons);
		File binnedOutput = new File(outputFileNameBase + "amplicons.xml");
		try (OutputStream os = new FileOutputStream(binnedOutput);){
			 Serializer serializer = new Serializer(os, "ISO-8859-1");
		        serializer.setIndent(4);
		        serializer.setMaxLength(64);
		        serializer.write(doc);  
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private List<Bin> convertFragmentsToBins(Map<String, AtomicInteger> frags) {
		List<Bin> singleBins = new ArrayList<>();
		List<Bin> multipleBins = new ArrayList<>();
		
		for (Entry<String, AtomicInteger> entry : frags.entrySet()) {
			int tally = entry.getValue().get();
			String sequence = entry.getKey();
			if (null != sequence) {
				Bin b = new Bin(binId++, sequence, tally);
				if (tally > 1) {
					multipleBins.add(b);
				} else {
					singleBins.add(b);
				}
			}
		}
		
		// sort multiple bins so that the largest is first
		Collections.sort(multipleBins);
		
		// loop through single bins and see if we can assign to a bigger bin
		Iterator<Bin> iter = singleBins.iterator();
		while(iter.hasNext()) {
			Bin sb = iter.next();
			// loop through multi bins
			for (Bin mb : multipleBins) {
				// need to have the same length
				if (sb.getLength() == mb.getLength()) {
					// need to have a BED of 1
					int basicEditDistance = ClinVarUtil.getBasicEditDistance(sb.getSequence(), mb.getSequence());
					if (basicEditDistance == 1) {
						
						// roll sb into mb
						mb.addSequence(sb.getSequence());
						
						// remove this single bin from singleBins
						iter.remove();
						
						// no need to look at other multi bins
						break;
					}
				}
			}
		}
		
		//combine and return
		multipleBins.addAll(singleBins);
		return multipleBins;
	}

	private void findNonExactMatches() {
		int maxSingleEditDistance = 2;
		for (FastqProbeMatch fpm : matches) {
			if (FastqProbeMatchUtil.neitherReadsHaveAMatch(fpm)) {
				// both reads missing probes
				String read1 = fpm.getRead1().getReadString();
				String read2 = fpm.getRead2().getReadString();
				
				for (Probe p : probeSet) {
					String dlsoRC = p.getDlsoSeqRC();
					String ulso = p.getUlsoSeq();
					
					
					int [] editDistances = ClinVarUtil.getDoubleEditDistance(read1, read2, dlsoRC, ulso, maxSingleEditDistance);
					if (editDistances[0] <= maxSingleEditDistance 
							&& editDistances[1] <= maxSingleEditDistance) {
						
						// it could be the case that we have already found a matching probe, so make sure this match is better!!!
						
						if (FastqProbeMatchUtil.neitherReadsHaveAMatch(fpm)) {
							fpm.setRead1Probe(p, editDistances[0]);
							fpm.setRead2Probe(p, editDistances[1]);
						} else {
							// already have some probes set...
							logger.warn("have to determine which match is best...");
						}
					} else if (editDistances[0] <= maxSingleEditDistance) {
						// it could be the case that we have already found a matching probe, so make sure this match is better!!!
						
						if (FastqProbeMatchUtil.neitherReadsHaveAMatch(fpm)) {
							fpm.setRead1Probe(p, editDistances[0]);
						} else {
							// already have some probes set...
							logger.warn("have to determine which match is best...");
						}
						
					} else if (editDistances[1] <= maxSingleEditDistance) {
						// it could be the case that we have already found a matching probe, so make sure this match is better!!!
						
						if (FastqProbeMatchUtil.neitherReadsHaveAMatch(fpm)) {
							fpm.setRead2Probe(p, editDistances[1]);
						} else {
							// already have some probes set...
							logger.warn("have to determine which match is best...");
						}
						
					}
				}
				
			} else if ( ! FastqProbeMatchUtil.bothReadsHaveAMatch(fpm)) {
				// just 1 is missing a probe - find out which one, and try and find a match
				String read = null;
				Probe existingProbe = null;
				boolean updateFirstReadProbe = null == fpm.getRead1Probe();
				
				if (updateFirstReadProbe) {
					read = fpm.getRead1().getReadString();
					existingProbe = fpm.getRead2Probe();
				} else {
					read = fpm.getRead2().getReadString();
					existingProbe = fpm.getRead1Probe();
				}
				
				// lets see if we are close to the existing probe
				int editDistance = ClinVarUtil.getEditDistance(read,  existingProbe.getDlsoSeqRC(), maxSingleEditDistance + 1);
				if (editDistance <= maxSingleEditDistance) {
					if (updateFirstReadProbe) {
						fpm.setRead1Probe(existingProbe, editDistance);
					} else {
						fpm.setRead2Probe(existingProbe, editDistance);
					}
				} else {
					// loop through the probes
					for (Probe p : probeSet) {
						String primer = updateFirstReadProbe ? p.getDlsoSeqRC() :  p.getUlsoSeq();
						editDistance = ClinVarUtil.getEditDistance(read, primer, maxSingleEditDistance + 1);
						
						if (editDistance <= maxSingleEditDistance) {
							// it could be the case that we have already found a matching probe, so make sure this match is better!!!
							
							if (updateFirstReadProbe && null == fpm.getRead1Probe()) {
								fpm.setRead1Probe(p, editDistance);
							} else if ( ! updateFirstReadProbe && null == fpm.getRead2Probe()) {
								fpm.setRead2Probe(p, editDistance);
							} else {
								// already have some probes set...
								logger.warn("have to determine which match is best...");
							}
						}
					}						
				}
			}
		}
	}
	
	public void setupFragments() {
		
		for (FastqProbeMatch fpm : matches) {
			if (FastqProbeMatchUtil.isProperlyMatched(fpm)) {
				
				Probe p = fpm.getRead1Probe();
				
				FastqProbeMatchUtil.createFragment(fpm);
				List<FastqProbeMatch> fpms = probeDist.get(p);
				if (null == fpms) {
					fpms = new ArrayList<>();
					probeDist.put(p, fpms);
				}
				fpms.add(fpm);
				
				Map<String, AtomicInteger> probeFragment = probeFragments.get(p);
				if (null == probeFragment) {
					probeFragment = new HashMap<>();
					probeFragments.put(p, probeFragment);
				}
				updateMap(fpm.getFragment(), probeFragment);
				
				// get match scores
				Map<IntPair, AtomicInteger> matchMap = probeMatchMap.get(p);
				if (null == matchMap) {
					matchMap = new HashMap<>();
					probeMatchMap.put(p, matchMap);
				}
				updateMap(fpm.getScore(), matchMap);
			}
		}
	}

	private void writeAmpliconDetails() {
		// logging and writing to file
		Element amplicons = new Element("Amplicons");
		amplicons.addAttribute(new Attribute("amplicon_file", xmlFile));
		amplicons.addAttribute(new Attribute("fastq_file_1", fastqFiles[0]));
		amplicons.addAttribute(new Attribute("fastq_file_2", fastqFiles[1]));
		
		for (Probe p : probeSet) {
			Element fragments = createAmpliconElement(amplicons, p);
			
			// fragments next
			Map<String, AtomicInteger> frags = probeFragments.get(p);
			if (null != frags && ! frags.isEmpty()) {
				
				TIntArrayList fragMatches = new TIntArrayList();
				for (Entry<String, AtomicInteger> entry : frags.entrySet()) {
					if (entry.getKey() != null) {
						Element fragment = new Element("Fragment");
						fragments.appendChild(fragment);
						fragment.addAttribute(new Attribute("fragment_length", "" + (entry.getKey().startsWith("+++") || entry.getKey().startsWith("---") ? entry.getKey().length() - 3 : entry.getKey().length())));
						fragment.addAttribute(new Attribute("record_count", "" + entry.getValue().get()));
						fragment.appendChild(entry.getKey());
						fragMatches.add(entry.getValue().get());
					}
				}
				if (fragMatches.size() > 1) {
					fragments.addAttribute(new Attribute("fragment_breakdown", "" + ClinVarUtil.breakdownEditDistanceDistribution(fragMatches)));
				}
			}
		}
//		logger.info("no of probes with secondary bin contains > 10% of reads: " + noOfProbesWithLargeSecondaryBin);
		
		// write output
		Document doc = new Document(amplicons);
		try (OutputStream os = new FileOutputStream(new File(outputFileNameBase + "diag.unbinned_amplicons.xml"));){
			 Serializer serializer = new Serializer(os, "ISO-8859-1");
		        serializer.setIndent(4);
		        serializer.setMaxLength(64);
		        serializer.write(doc);  
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Element createAmpliconElement(Element amplicons, Probe p) {
		Element amplicon = new Element("Amplicon");
		amplicons.appendChild(amplicon);
		
		// attributes
		amplicon.addAttribute(new Attribute("probe_id", "" + p.getId()));
		amplicon.addAttribute(new Attribute("expected_fragment_length", "" + p.getExpectedFragmentLength()));
		
		List<FastqProbeMatch> fpms = probeDist.get(p);
		
		Element fragments = new Element("Fragments");
		if (null != fpms && ! fpms.isEmpty()) {
			amplicon.appendChild(fragments);
			
			int fragmentExists = 0;
			for (FastqProbeMatch fpm : fpms) {
				if (FastqProbeMatchUtil.doesFPMMatchProbe(fpm, p)) {
					
					String frag = fpm.getFragment();
					if (null != frag) {
						fragmentExists++;
					}
				}
			}
			
//			probeReadCountMap.put(p, new IntPair(fpms.size(), fragmentExists));
			// add some attributtes
			fragments.addAttribute(new Attribute("total_number_of_reads", "" + fpms.size()));
			fragments.addAttribute(new Attribute("reads_containing_fragments", "" + fragmentExists));
			Map<IntPair, AtomicInteger> matchMap = probeMatchMap.get(p);
			if (null != matchMap) {
				StringBuilder sb = new StringBuilder();
				for (Entry<IntPair, AtomicInteger> entry : matchMap.entrySet()) {
					sb.append(entry.getKey().getInt1()).append("/").append(entry.getKey().getInt2());
					sb.append(":").append(entry.getValue().get()).append(',');
				}
				fragments.addAttribute(new Attribute("probe_match_breakdown", sb.toString()));
			}
		}
		return fragments;
	}
	
	public static final <P> void updateMap(P p, Map<P, AtomicInteger> map) {
		AtomicInteger ai = map.get(p);
		if (null == ai) {
			ai = new AtomicInteger(1);
			map.put(p,  ai);
		} else {
			ai.incrementAndGet();
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		// loads all classes in referenced jars into memory to avoid nightly build sheninegans
		LoadReferencedClasses.loadClasses(Q3ClinVar.class);
		
		Q3ClinVar qp = new Q3ClinVar();
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
			version = Q3ClinVar.class.getPackage().getImplementationVersion();
			logger = QLoggerFactory.getLogger(Q3ClinVar.class, logFile, options.getLogLevel());
			qexec = logger.logInitialExecutionStats("q3clinvar", version, args);
			
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
					outputDir = optionsOutputFile;
				} else {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
			}
			// setup output file name base - use UUID
			//TODO switch to UUID from qexec when ready to roll
//			outputFileNameBase = outputDir + Constants.FILE_SEPARATOR + qexec.getUuid() + ".q3cv.";
			outputFileNameBase = outputDir + Constants.FILE_SEPARATOR +  "UUID.q3cv.";
			
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
