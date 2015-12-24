package au.edu.qimr.clinvar;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TLongProcedure;
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
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.apache.commons.lang3.StringUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qmule.SmithWatermanGotoh;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.clinvar.model.Amplicon;
import au.edu.qimr.clinvar.model.Fragment;
import au.edu.qimr.clinvar.model.IntPair;
import au.edu.qimr.clinvar.model.PositionChrPositionMap;
import au.edu.qimr.clinvar.model.RawFragment;
import au.edu.qimr.clinvar.util.ClinVarUtil;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;

public class Q3ClinVar2 {
	
	private static QLogger logger;
	private static final int TILE_SIZE = 13;
	private static final char [] AT = new char[]{'A','T'};
	
	private static String[] fastqFiles;
	private static String version;
	private String logFile;
	private String bedFile;
	private String outputFileNameBase;
	private String refTiledAlignmentFile;
	private String refFileName;
	private final int tiledDiffThreshold = 1;
	private final int swDiffThreshold = 2;
	private final int tileMatchThreshold = 2;
	private final int maxIndelLength = 5;
	private int fastqRecordCount;
	
	private int minBinSize = 10;
	private int minFragmentSize = 3;
	private  int minReadPercentage = 2;
	private int ampliconBoundary = 10;
	
	private final Map<String, AtomicInteger> reads = new HashMap<>();
	
	private final Set<String> frequentlyOccurringRefTiles = new HashSet<>();
	private final Map<String, TLongArrayList> refTilesPositions = new HashMap<>();
	
	private final PositionChrPositionMap positionToActualLocation = new PositionChrPositionMap();
	
	private final Map<String, byte[]> referenceCache = new HashMap<>();
	
//	private final Map<ChrPosition, List<String>> positionFragmentsMap = new HashMap<>();
	
	private final Map<String, RawFragment> rawFragments = new HashMap<>();
	private final Map<String, Fragment> frags = new HashMap<>();
	
	private final Map<VcfRecord, List<int[]>> vcfFragmentMap = new HashMap<>();
	
	private Map<Amplicon, List<Fragment>> ampliconFragmentMap = new HashMap<>();
	
	private final Map<Amplicon, List<Amplicon>> bedToAmpliconMap = new HashMap<>();
	
//	private final static Map<Pair<FastqRecord, FastqRecord>, List<Probe>> multiMatchingReads = new HashMap<>();
	
	private int exitStatus;
	private int fragmentId = 1;
	private int rawFragmentId = 1;
	
	protected int engage() throws Exception {
			
		fastqRecordCount = readFastqs();
		
		createFragments();
		
		loadTiledAlignerData();
		
		/*
		 * Clear up some no longer used resources
		 */
		rawFragments.clear();
		reads.clear();
		
		getActualLocationForFrags();
		
		createAmplicons();
		
		writeFragmentAmpliconsToXml();
		
		mapBedToAmplicons();
		
		createMutations();
		logger.info("no of mutations created: " + vcfFragmentMap.size());
		filterAndWriteVcfs();
		writeBam();
		
		logger.info("no of fastq records: " +fastqRecordCount);
		return exitStatus;
	}

	private void createFragments() {
		int perfectOverlap = 0;
		int nonPerfectOverlap = 0;
		int smallOverlap = 0;
		int onlyATs = 0;
		int theSame = 0, different = 0;
		
		TIntIntHashMap overlapDistribution = new TIntIntHashMap();
		TIntIntHashMap nonOverlapDistribution = new TIntIntHashMap();
		TIntIntHashMap overlapLengthDistribution = new TIntIntHashMap();
		
		for (Entry<String, AtomicInteger> entry : reads.entrySet()) {
			int readCount = entry.getValue().intValue();
			String combinedReads = entry.getKey();
			String r1 = combinedReads.substring(0, combinedReads.indexOf(':'));
			String r2 = combinedReads.substring(combinedReads.indexOf(':') + 1);
			String r2RevComp = SequenceUtil.reverseComplement(r2);
			
			boolean readsAreTheSame = r1.equals(r2RevComp);
			if (readsAreTheSame) {
				theSame++;
				overlapDistribution.adjustOrPutValue(readCount, 1, 1);
				overlapLengthDistribution.adjustOrPutValue(r1.length(), 1, 1);
				
				RawFragment f = rawFragments.get(r1);
				if (null == f) {
					f = new RawFragment(fragmentId++, r1, readCount, r1.length());
					rawFragments.put(r1, f);
				} else {
					// update count and overlap
					f.addCount(readCount);
					f.addOverlap( r1.length(), readCount);
				}
				
			} else {
				different++;
			
				SmithWatermanGotoh nm = new SmithWatermanGotoh(r1, r2RevComp, 5, -4, 16, 4);
				String [] newSwDiffs = nm.traceback();
				String overlapMatches = newSwDiffs[1];
				if (overlapMatches.indexOf(" ") > -1 || overlapMatches.indexOf('.') > -1) {
					nonOverlapDistribution.adjustOrPutValue(readCount, 1, 1);
	//				nonOverlapDistribution.incrementAndGet(entry.getValue().intValue());
					nonPerfectOverlap++;
	//				for (String s : newSwDiffs) {
	//					logger.info("non perfect overlap sw: " + s);
	//				}
				} else {
					overlapDistribution.adjustOrPutValue(readCount, 1, 1);
	//				overlapDistribution.incrementAndGet(entry.getValue().intValue());
					String overlap = newSwDiffs[0];
					int overlapLength = overlapMatches.length();
					if (overlapLength < 10) {
						smallOverlap++;
						continue;
					}
					if (StringUtils.containsOnly(newSwDiffs[0], AT)) {
						onlyATs++;
						continue;
					}
					overlapLengthDistribution.adjustOrPutValue(overlapLength, 1, 1);
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
					
					if (fragment.length() > 250) {		// need to get t good number here.....
//						logger.info("Fragment has length greater than 250: " + fragment + "\nr1: " + r1 + "\nr2RevComp: " + r2RevComp);
//						for (String s  : newSwDiffs) {
//							logger.info("s: " + s);
//						}
					} //else {
					
					
					RawFragment f = rawFragments.get(fragment);
					if (null == f) {
						f = new RawFragment(fragmentId++, fragment, readCount, overlapLength);
						rawFragments.put(fragment, f);
					} else {
						// update count and overlap
						f.addCount(readCount);
						f.addOverlap(overlapLength, readCount);
					}
					
//						AtomicInteger ai2 = fragments.get(fragment);
//						if (null == ai2) {
//							fragments.put(fragment, new AtomicInteger(readCount));
//						} else {
//							ai2.addAndGet(entry.getValue().intValue());
//						}
				}
			}
		}
		logger.info("theSame: " + theSame + ", different: " + different);
		logger.info("perfectOverlap: " + perfectOverlap + ", nonPerfectOverlap: " + nonPerfectOverlap + ", small overlap: " + smallOverlap + ", onlyATs: " + onlyATs);
		logger.info("the following distribution is of the number of reads that were not able to make fragments due to differences in r1 and r2 reads.");
		int nonFragmentRecordTally = 0;
		int fragmentRecordTally = 0;
		int [] nonOverlapDistributionKeys = nonOverlapDistribution.keys();
		Arrays.sort(nonOverlapDistributionKeys);
		for (int i : nonOverlapDistributionKeys) {
			long l = nonOverlapDistribution.get(i);
			if (l > 0) {
				nonFragmentRecordTally += (l * i);
				logger.info("no fragment distribution, record count: " + i + ", appeared " + l + " times");
			}
		}
		int [] overlapDistributionKeys = overlapDistribution.keys();
		Arrays.sort(overlapDistributionKeys);
		logger.info("the following distribution is of the number of reads that were able to make fragments.");
		for (int i : overlapDistributionKeys) {
			long l = overlapDistribution.get(i);
			if (l > 0) {
				fragmentRecordTally += (l * i);
				logger.info("fragment distribution, record count: " + i + ", appeared " + l + " times");
			}
		}
		
		logger.info("Percentage of records that failed to make a fragment: " + nonFragmentRecordTally + " : " + ((double)nonFragmentRecordTally / fastqRecordCount) * 100 + "%");
		logger.info("Percentage of records that made a fragment: " + fragmentRecordTally + " : " + ((double)fragmentRecordTally / fastqRecordCount) * 100 + "%");
		
		
		int [] overlapLengthDistributionKeys = overlapLengthDistribution.keys();
		Arrays.sort(overlapLengthDistributionKeys);
		logger.info("the following distribution is of the size of overlap from reads that were able to make fragments.");
		for (int i : overlapLengthDistributionKeys) {
			int l = overlapLengthDistribution.get(i);
			if (l > 0) {
				fragmentRecordTally += (l * i);
				logger.info("overlapLength: " + i + ", count: " +l);
			}
		}
		// cleanup
		overlapLengthDistribution.clear();
		overlapLengthDistribution = null;
		
		logger.info("fragments size: " + rawFragments.size());
	}
	
	private void createAmpliconElement(Element amplicons, Amplicon p, List<Fragment> frags) {
		Element amplicon = new Element("Amplicon");
		amplicons.appendChild(amplicon);
		
		// attributes
		amplicon.addAttribute(new Attribute("id", "" + p.getId()));
		amplicon.addAttribute(new Attribute("position", "" + p.getPosition().toIGVString()));
		amplicon.addAttribute(new Attribute("initial_frag_position", "" + p.getInitialFragmentPosition().toIGVString()));
		amplicon.addAttribute(new Attribute("amplicon_length", "" + p.getPosition().getLength()));
		amplicon.addAttribute(new Attribute("number_of_fragments", "" + frags.size()));

		AtomicInteger readCount = new AtomicInteger();
//		int readCount = 0;
		Element fragments = new Element("Fragments");
		amplicon.appendChild(fragments);
		frags.stream()
			.sorted()
			.forEach(f -> {
				Element fragment = new Element("Fragment");
				fragments.appendChild(fragment);
				readCount.addAndGet(f.getRecordCount());
				fragment.addAttribute(new Attribute("id", "" + f.getId()));
				fragment.addAttribute(new Attribute("record_count", "" + f.getRecordCount()));
				fragment.addAttribute(new Attribute("position", "" + f.getActualPosition().toIGVString()));
				fragment.addAttribute(new Attribute("genomic_length", "" + f.getActualPosition().getLength()));
				fragment.addAttribute(new Attribute("overlap_dist", "" + getOverlapDistributionAsString(f.getOverlapDistribution())));
				fragment.addAttribute(new Attribute("md", "" + ClinVarUtil.getSWDetails(f.getSmithWatermanDiffs())));
				fragment.addAttribute(new Attribute("fragment_length", "" + f.getLength()));
				fragment.addAttribute(new Attribute("seq", "" + f.getSequence()));
			});
		
		amplicon.addAttribute(new Attribute("number_of_reads", "" + readCount.get()));
	}
	
	private String getOverlapDistributionAsString(TIntArrayList dist) {
		if (dist.size() == 1) {
			return "" + dist.get(0) + ":1";
		}
		
		TIntIntHashMap map = new TIntIntHashMap();
		dist.forEach(new TIntProcedure() {
			@Override
			public boolean execute(int i) {
				map.adjustOrPutValue(i, 1, 1);
				return true;
			}});
		
		
		StringBuilder sb = new StringBuilder();
		int[] keys = map.keys();
		Arrays.sort(keys);
		for (int i : keys) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(i).append(":").append(map.get(i));
		}
		return sb.toString();
	}
	
	private void writeFragmentAmpliconsToXml() {
		
		// logging and writing to file
		Element amplicons = new Element("Amplicons");
		amplicons.addAttribute(new Attribute("bed_file", bedFile));
		amplicons.addAttribute(new Attribute("fastq_file_1", fastqFiles[0]));
		amplicons.addAttribute(new Attribute("fastq_file_2", fastqFiles[1]));
		
		ampliconFragmentMap.entrySet().stream()
			.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
			.forEach(entry -> {
				createAmpliconElement(amplicons, entry.getKey(), entry.getValue());
			});
		
//		for (Probe p : probeSet) {
//			
//			// fragments next
//			Map<String, AtomicInteger> frags = probeFragments.get(p);
//			if (null != frags && ! frags.isEmpty()) {
//				
//				TIntArrayList fragMatches = new TIntArrayList();
//				for (Entry<String, AtomicInteger> entry : frags.entrySet()) {
//					if (entry.getKey() != null) {
//						Element fragment = new Element("Fragment");
//						fragments.appendChild(fragment);
//						fragment.addAttribute(new Attribute("fragment_length", "" + (entry.getKey().startsWith("+++") || entry.getKey().startsWith("---") ? entry.getKey().length() - 3 : entry.getKey().length())));
//						fragment.addAttribute(new Attribute("record_count", "" + entry.getValue().get()));
//						fragment.appendChild(entry.getKey());
//						fragMatches.add(entry.getValue().get());
//					}
//				}
//				if (fragMatches.size() > 1) {
//					fragments.addAttribute(new Attribute("fragment_breakdown", "" + ClinVarUtil.breakdownEditDistanceDistribution(fragMatches)));
//				}
//			}
//		}
//			logger.info("no of probes with secondary bin contains > 10% of reads: " + noOfProbesWithLargeSecondaryBin);
		
		// write output
		Document doc = new Document(amplicons);
		try (OutputStream os = new FileOutputStream(new File(outputFileNameBase + ".xml"));){
			 Serializer serializer = new Serializer(os, "ISO-8859-1");
		        serializer.setIndent(4);
		        serializer.setMaxLength(64);
		        serializer.write(doc);  
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void mapBedToAmplicons() throws IOException, Exception {
		/*
		 * Only do this if we have been supplied a bed file
		 */
		if (bedFile != null && new File(bedFile).exists()) {
			int bedId = 0;
			try (TabbedFileReader reader = new TabbedFileReader(new File(bedFile));) {
				for (TabbedRecord rec : reader) {
					String [] params = TabTokenizer.tokenize(rec.getData());
					ChrPosition cp = new ChrPosition(params[0], Integer.parseInt(params[1]), Integer.parseInt(params[2]));
					bedToAmpliconMap.put(new Amplicon(++bedId, cp), new ArrayList<Amplicon>(1));
				}
			}
			logger.info("Loaded " + bedToAmpliconMap.size() + " bed positions into map");
			
			/*
			 * log distribution of bed lengths
			 */
			Map<Integer, Long> bedLengthDistribution = bedToAmpliconMap.keySet().stream()
					.collect(Collectors.groupingBy(bed -> bed.getPosition().getLength(), Collectors.counting()));
			bedLengthDistribution.entrySet().stream()
				.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
				.forEach(e -> logger.info("bed length, and count: " + e.getKey().intValue() + " : " + e.getValue().longValue()));
			/*
			 * log distribution of amplicon lengths
			 */
			Map<Integer, Long> ampliconLengthDistribution = ampliconFragmentMap.keySet().stream()
					.collect(Collectors.groupingBy(a -> a.getPosition().getLength(), Collectors.counting()));
			ampliconLengthDistribution.entrySet().stream()
				.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
				.forEach(e -> logger.info("amplicon length, and count: " + e.getKey().intValue() + " : " + e.getValue().longValue()));
			
			/*
			 * Assign amplicons to bed poisitions
			 */
			ampliconFragmentMap.keySet().stream()
				.forEach(a -> {
					List<Amplicon> beds = bedToAmpliconMap.keySet().stream()
						.filter(bed -> ChrPositionUtils.isChrPositionContained(a.getPosition(), bed.getPosition())
								&& a.getPosition().getPosition() < (bed.getPosition().getPosition() + 10)
								&& a.getPosition().getEndPosition() > (bed.getPosition().getEndPosition() + 10)
								)
						.collect(Collectors.toList());
					
					if (beds.size() > 1) {
						logger.info("Found " + beds.size() + " bed positions that are contained by this amplicon " + a.getPosition().toIGVString());
						beds.stream().forEach(b ->logger.info("bed: " + b.toString()));
					}
				});
			
		}
	}
	
	private void createAmplicons() {
		
//		List<ChrPosition> fragCPs = frags.values().stream()
//				.filter(f -> f.getActualPosition() != null)
//				.map(Fragment::getActualPosition)
//				.collect(Collectors.toList());
//		
//		Map<ChrPosition, Set<ChrPosition>> ampliconFragmentPositionMap = ChrPositionUtils.getAmpliconsFromFragments(fragCPs);
//		
//		logger.info("Found " + ampliconFragmentPositionMap.size() + " amplicons from " + fragCPs.size() + " fragments");
//		
//		/*
//		 * Next, try and rollup amplicons with adjacent start positions
//		 */
//		List<ChrPosition> toRemove = new ArrayList<>();
//		
//		for (Entry<ChrPosition, Set<ChrPosition>> entry : ampliconFragmentPositionMap.entrySet()) {
//			
//			
//			ChrPosition cp = entry.getKey();
//			if ( ! toRemove.contains(cp)) {
//				ampliconFragmentPositionMap.keySet().stream()
//					.filter(cp1 -> ! cp1.equals(cp))
//					.filter(cp1 -> ! toRemove.contains(cp1))
//					.filter(cp1 -> cp1.getChromosome().equals(cp.getChromosome()) 
//							&& Math.abs(cp1.getPosition() - cp.getPosition()) <= ampliconBoundary
//							&& Math.abs(cp1.getEndPosition() - cp.getEndPosition()) <= ampliconBoundary)
//					.forEach(cp1 -> {
//						entry.getValue().addAll(ampliconFragmentPositionMap.get(cp1));
//						toRemove.add(cp1);
//					});
//			}
//		}
//		toRemove.stream()
//			.forEach(a -> ampliconFragmentPositionMap.remove(a));
//		
//		
//		Map<ChrPosition, Set<ChrPosition>> ampliconFragmentPositionMap = ClinVarUtil.getGroupedChrPositionsFromFragments(frags.values(), ampliconBoundary);
//		
//		logger.info("After rollup, found " + ampliconFragmentPositionMap.size() + " amplicons from " + frags.size() + " fragments");
//		
//		/*
//		 * Now need to expand out the amplcon cp to include the largest end position of its constituents
//		 * and keep a list of the fragments associated with it 
//		 */
//		final AtomicInteger ampliconId = new AtomicInteger();
//		ampliconFragmentPositionMap.entrySet().stream()
//			.sorted((e1, e2) -> {return e1.getKey().compareTo(e2.getKey());})
//			.forEach(entry -> {
//				
//				OptionalInt maxEnd = entry.getValue().stream()
//						.mapToInt(ChrPosition::getEndPosition)
//						.max();
//				ChrPosition ampliconCP = new ChrPosition(entry.getKey().getChromosome(), entry.getKey().getPosition(), maxEnd.orElse( entry.getKey().getPosition()));
//				Amplicon amplicon = new Amplicon(ampliconId.incrementAndGet(), ampliconCP);
//				
//				ampliconFragmentMap.put(amplicon,  frags.values().stream()
//						.filter(f -> entry.getValue().contains(f.getActualPosition()))
//						.collect(Collectors.toList()));
//				});
		
		ampliconFragmentMap = ClinVarUtil.groupFragments(frags.values(), ampliconBoundary);
		/*
		 * Get some stats on each Amplicon
		 * # of fragments, and reads to start with
		 */
		ampliconFragmentMap.entrySet().stream()
			.forEach(entry -> {
				int recordCount = entry.getValue().stream().mapToInt(Fragment::getRecordCount).sum();
				logger.info("Amplicon " + entry.getKey().getId() + " " + entry.getKey().getPosition().toIGVString() + " has " + entry.getValue().size() + " fragments with a total of " + recordCount + " records (" + ((double) recordCount / fastqRecordCount) * 100 + "%)");
			});
		
		/*
		 * If amplicon length is over 200, lets list its constituents
		 */
//		ampliconFragmentMap.entrySet().stream()
//			.filter(entry -> entry.getKey().getFragmentPosition().getLength() > 200)
//			.forEach(entry ->{
//				logger.info("Amplicon " + entry.getKey().getId() + " " + entry.getKey().getFragmentPosition().toIGVString() + " has length " + entry.getKey().getFragmentPosition().getLength());
//				entry.getValue().stream()
//					.forEach(value -> {logger.info("fragment: " + value.getActualPosition().toIGVString());});
//			});
	}

	private double getMutationCoveragePercentage(VcfRecord vcf, List<int[]> fragmentsCarryingMutation) {
		/*
		 * Get the total coverage at this position
		 * Use 
		 */
		List<Fragment> overlappingFragments = getOverlappingFragments(vcf.getChrPosition());
		int totalCoverage = overlappingFragments.stream()
				.mapToInt(Fragment::getRecordCount)
				.sum();
		int mutationCoverage = fragmentsCarryingMutation.stream()
				.mapToInt( i -> i[2])
//				.mapToInt(IntPair::getInt2)
				.sum();
		
		double percentage = totalCoverage > 0 ? ((double)mutationCoverage / totalCoverage) * 100 : 0.0;
		return percentage;
	}
	
	private int getRecordCountFormIntPairs(List<int[]> list) {
		return list.stream()
			.mapToInt(i -> i[2])
			.sum();
//		final AtomicInteger tally = new AtomicInteger();
//		list.stream().forEach(ip -> tally.addAndGet(ip.getInt2()));
//		return tally.intValue();
	}
	
	
	private void writeBam() throws IOException {
		
		String outputFileName = outputFileNameBase + ".bam";
		File bamFile = new File(outputFileName);
		SAMFileHeader header = new SAMFileHeader();
		header.setSequenceDictionary(ClinVarUtil.getSequenceDictionaryFromFragments(frags.values()));
		header.setSortOrder(SortOrder.coordinate);
		SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		factory.setCreateIndex(true);
		SAMFileWriter writer = factory.makeSAMOrBAMWriter(header, false, bamFile);
		
		final AtomicLong recordCount = new AtomicLong();
//		int indelCount = 0;
//		int indelSameLength = 0;
//		int indelDiffLength = 0;
//		int sameSize = 0, diffSize = 0, noDiffInFirs20 = 0;
		try {

			
			frags.values().stream()
				.filter(f -> f.getActualPosition() != null)
				.sorted((f1,f2) -> f1.getActualPosition().compareTo(f2.getActualPosition()))
				.forEach(f -> {
					ChrPosition fragCp = f.getActualPosition();
					int fragId = f.getId();
					recordCount.addAndGet(f.getRecordCount());
					String fragSeq = f.getSequence();
					String [] swDiffs = f.getSmithWatermanDiffs();
					
					/*
					 * assign different mapping quality based on record count within fragment
					 */
					int mappingQuality = f.getRecordCount() > minFragmentSize ? 60 : 20;
					/*
					 * Deal with exact matches first
					 */
					if (ClinVarUtil.isSequenceExactMatch(swDiffs, fragSeq)) {
						
//						logger.info("bam record position: " + fragCp.getPosition() + ", seq: " + fragSeq);
						
						Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(f.getLength());
						ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, 1, fragId,  f.getRecordCount(), swDiffs[0], fragCp.getChromosome(), fragCp.getPosition(), 0, fragSeq, mappingQuality);
					} else if (ClinVarUtil.doesSWContainSnp(swDiffs) && ! ClinVarUtil.doesSWContainIndel(swDiffs)) {
						/*
						 * Snps only here
						 */
						if (swDiffs[1].length() == f.getLength()) {
							// just snps and same length
							Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(f.getLength());
							ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, 1, fragId,  f.getRecordCount(), swDiffs[0], fragCp.getChromosome(), fragCp.getPosition(), 0, fragSeq, mappingQuality);
						} else {
							logger.info("snps only but length differs, swDiffs[1].length(): " + swDiffs[1].length() + ", f.getLength(): " + f.getLength());
						}
						
					} else if ( ! ClinVarUtil.doesSWContainSnp(swDiffs) && ClinVarUtil.doesSWContainIndel(swDiffs)) {
						// indels only
						String ref = swDiffs[0].replaceAll("-","");
						Cigar cigar = ClinVarUtil.getCigarForIndels(ref,  fragSeq, swDiffs,  fragCp);
//						if (cigar.toString().equals("75M2I80M")) {
//							logger.info("ref: " + ref);
//							logger.info("fragSeq: " + fragSeq);
//							logger.info("cigar: " + cigar.toString());
//							for (String s : swDiffs) {
//								logger.info("s: " + s);
//							}
//						}
						ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, 1, fragId,  f.getRecordCount(), ref, fragCp.getChromosome(), fragCp.getPosition(), 0, fragSeq, mappingQuality);
					} else {
						// snps and indels
					}
					
					
				});
				
			
			
			
			/*
			 * loop through probes
			 * output bins of size greater than minBinSize 
			 */
//			for (Probe p : coordSortedProbes) {
//				boolean reverseComplementSequence = p.reverseComplementSequence();
//				final String referenceSequence = p.getReferenceSequence();
//				final String bufferedReferenceSequence = p.getBufferedReferenceSequence();
//				List<Bin> bins = probeBinDist.get(p);
//				if (null != bins) {
//					for (Bin b : bins) {
//						
//						if ( ! filter || (null != b.getBestTiledLocation() && ClinVarUtil.doChrPosOverlap(ampliconCP, b.getBestTiledLocation()))) {
//						
//							int binId = b.getId();
//							
//							String binSeq = reverseComplementSequence ?  SequenceUtil.reverseComplement(b.getSequence()) : b.getSequence() ;
//							
//							/*
//							 * Just print ones that match the ref for now - makes ceegar easier..
//							 */
//							int offset = referenceSequence.indexOf(binSeq);
//							int bufferedOffset = -1;
//							if (offset == -1) {
//								/*
//								 *  try running against bufferedRefSeq
//								 */
//								bufferedOffset = bufferedReferenceSequence.indexOf(binSeq);
//								if (bufferedOffset >= 0) {
//									logger.debug("got a match against the buffered reference!!! p: " + p.getId() + ", bin: " + b.getId());
////									if (p.getId() == 121 && b.getId() == 1272) {
////										logger.info("referenceSequence: " + referenceSequence);
////										logger.info("bufferedReferenceSequence: " + bufferedReferenceSequence);
////										logger.info("binSeq: " + binSeq);
////									}
//								}
//							}
//							if (offset >= 0) {
//								/*
//								 * Perfect Match
//								 */
//								Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(b.getLength());
//								ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), referenceSequence, p.getCp().getChromosome(), p.getCp().getPosition(), offset, binSeq);
//								
//							} else if (bufferedOffset >= 0) {
//								/*
//								 * Perfect Match against buffered reference
//								 */
//								Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(b.getLength());
//								ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), bufferedReferenceSequence, p.getCp().getChromosome(), p.getCp().getPosition() - 10, bufferedOffset, binSeq);
//								
//							} else {
//								
//								/*
//								 * bin sequence differs from reference
//								 */
//								String [] swDiffs = b.getSmithWatermanDiffs();
//								
//								if (null != swDiffs) {
//									if ( ! swDiffs[1].contains(" ")) {
//										/*
//										 * Only snps
//										 */
//										if (swDiffs[1].length() == referenceSequence.length()) {
//											// just snps and same length
//											Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(b.getLength());
//											ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), referenceSequence, p.getCp().getChromosome(), p.getCp().getPosition(), 0, binSeq);
//										} else {
//											logger.debug("only snps but diff length to ref. bin: " + b.getId() + ", p: " + p.getId() + ", binSeq: " + binSeq + ", ref: " + referenceSequence);
//											for (String s : swDiffs) {
//												logger.debug("s: " + s);
//											}
//											Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(b.getLength());
//											ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), referenceSequence, p.getCp().getChromosome(), p.getCp().getPosition(), 0, binSeq);
//										}
//									} else {
//										
//										Cigar cigar = ClinVarUtil.getCigarForIndels( referenceSequence,  binSeq, swDiffs,  p,  b);
//										ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, probeId, binId,  b.getRecordCount(), referenceSequence, p.getCp().getChromosome(), p.getCp().getPosition(), 0, binSeq);
//									}
//								} else {
////									logger.warn("Not generating SAMRecord for bin: " + b.getId() + " as no sw diffs. getRecordCount: " + b.getRecordCount());
//								}
//							}
//						}
//					}
//				}
//			}
		} finally {
			writer.close();
		}
//		logger.info("indelDiffLength: " + indelDiffLength + ", indelSameLength: " + indelSameLength);
		logger.info("No of records written to bam file: " + recordCount.longValue());
//		logger.info("No of records written to bam file: " + recordCount + ", no of bins with same seq size as ref: " + sameSize +", and diff size: " + diffSize + ", indelCount: " + indelCount + ", noDiffInFirs20: " + noDiffInFirs20);
	}
	
	
	private void filterAndWriteVcfs() throws IOException {
		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileNameBase + ".vcf"))) {
			
			final AtomicInteger outputMutations = new AtomicInteger();
			/*
			 * Setup the VcfHeader
			 */
			final DateFormat df = new SimpleDateFormat("yyyyMMdd");
			VcfHeader header = new VcfHeader();
			header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + df.format(Calendar.getInstance().getTime()));		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + QExec.createUUid());		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=q3ClinVar");		
			header.addFormatLine("FB", ".","String","Breakdown of Amplicon ids, Fragment ids and read counts supporting this mutation, along with total counts of amplicon, fragment, and reads for all reads at that location in the following format: AmpliconId,FragmentId,readCount;[...] / Sum of amplicons at this position,sum of fragments at this position,sum of read counts at this position");
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT);
			
			Iterator<Record> iter = header.iterator();
			while (iter.hasNext()) {
				writer.addHeader(iter.next().toString() );
			}
			vcfFragmentMap.entrySet().stream()
				.filter((entry) -> getRecordCountFormIntPairs(entry.getValue()) >= minBinSize )
				.filter((entry) -> getMutationCoveragePercentage(entry.getKey(), entry.getValue()) >= minReadPercentage )
				.sorted((e1, e2) -> {return e1.getKey().compareTo(e2.getKey());})
				.forEach(entry -> {
				
					final StringBuilder mutationFragmentsDetails = new StringBuilder();
					entry.getValue().stream()
						.forEach(i -> {
							if (mutationFragmentsDetails.length() > 0) {
								mutationFragmentsDetails.append(';');
							}
							mutationFragmentsDetails.append(i[0]).append(',').append(i[1]).append(',').append(i[2]);
						});
					List<String> ff = new ArrayList<>(3);
					ff.add("FB");
					ff.add(mutationFragmentsDetails.toString() + "/" + ClinVarUtil.getCoverageStringAtPosition(entry.getKey().getChrPosition(), ampliconFragmentMap));
					entry.getKey().setFormatFields(ff);
					
					try {
						writer.add(entry.getKey());
						outputMutations.incrementAndGet();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			logger.info("no of mutations written to file: " + outputMutations.intValue());
		}
	}
	
	private List<Fragment> getOverlappingFragments(ChrPosition cp) {
		return frags.values().stream()
				.filter(frag -> null != frag.getActualPosition())
				.filter(frag -> ChrPositionUtils.doChrPositionsOverlap(cp, frag.getActualPosition()))
				.collect(Collectors.toList());
	}
	
	
	private void getActualLocationForFrags() {
		
//		int perfectMatchCount = 0;
//		int mutationCount = 0;
//		int diff20Count = 0;
//		int diffOtherCount = 0;
//		int snpsOnlyCorrectLength = 0;
//		int snpsOnlyWrongLength = 0;
//		int indelsOnly = 0;
//		int snpsAndindels = 0;
		
		int actualCPReadCount = 0;
		int noActualCPReadCount = 0;
		
		for (Fragment f : frags.values()) {
			
			/*
			 * Get best tiled location - and get ref bases based on this +/- 100
			 * and then sw
			 */
			ChrPosition bestTiledCP = f.getBestTiledLocation();
			ChrPosition bufferedCP = new ChrPosition(bestTiledCP.getChromosome(), bestTiledCP.getPosition() - 100, bestTiledCP.getPosition() + 100 + f.getLength());
			String bufferedReference = getRefFromChrPos(bufferedCP);
			
			String [] swDiffs = ClinVarUtil.getSwDiffs(bufferedReference, f.getSequence(), true);
			f.setSWDiffs(swDiffs);
			
			String swFragmentMinusDeletions = swDiffs[2].replaceAll("-", "");
			
			if (f.getSequence().equals(swFragmentMinusDeletions)) {
				/*
				 * Fragment is wholly contained in swdiffs, and so can get actual position based on that
				 */
				String swRefMinusDeletions = swDiffs[0].replaceAll("-", "");
				int offset = bufferedReference.indexOf(swRefMinusDeletions);
				int refLength = swRefMinusDeletions.length();
				setActualCP(bufferedCP, offset, f, refLength);
				actualCPReadCount += f.getRecordCount();
				
//				/*
//				 * If we don't have an insertion, then can get start position accurately from reference in swDiffs
//				 */
//				if ( ! swDiffs[0].contains("-")) {
//				} else {
//					
//					/*
//					 * If we can uniquely identify the start position, proceed
//					 */
//					
//					int firstInsertionIndex =  swDiffs[0].indexOf("-");
//					String refPortion = swDiffs[0].substring(0, firstInsertionIndex);
//					int firstReferenceLocation = bufferedReference.indexOf(refPortion);
//					if (bufferedReference.indexOf(refPortion, firstReferenceLocation + refPortion.length()) == -1) {
//						setActualCP(bufferedCP, firstReferenceLocation, f);
//					} else {
//						logger.info(refPortion + " is not unique in reference");
//						for (String s : swDiffs) {
//							logger.info("insertion s: " + s);
//						}
//					}
//				}
			} else {
				logger.info("fragment length: " + f.getLength() + ", differs from swDiffs: " + swFragmentMinusDeletions.length());
				logger.info("frag: " + f.getSequence());
				logger.info("swDiffs[2]: " + swDiffs[2]);
				logger.info("bufferedRef: " + bufferedReference);
				noActualCPReadCount += f.getRecordCount();
			}
			
				
			
//			if (swDiffs[1].contains(".") || swDiffs[1].contains(" ")) {
//				// snp or indel
//				mutationCount++;
//				if ( ! swDiffs[1].contains(" ")) {
//					// snps only
//					int diff = f.getLength() - swDiffs[0].length();
//					if (diff == 0) {
//						snpsOnlyCorrectLength++;
//						int offset = bufferedReference.indexOf(swDiffs[0]);
//						setActualCP(bufferedCP, offset, f);
//					} else {
//						snpsOnlyWrongLength++;
//					}
//				} else if ( ! ClinVarUtil.doesSWContainSnp(swDiffs)) {
//					// indels only
//					indelsOnly++;
//					/*
//					 * If we only have deletions, then can get start position accurately
//					 */
//					if ( ! swDiffs[0].contains("-")) {
//						int offset = bufferedReference.indexOf(swDiffs[0]);
//						setActualCP(bufferedCP, offset, f);
//					} else {
//						int firstIndelPosition = swDiffs[1].indexOf(' ');
//						if (firstIndelPosition > 10) {
//							int offset = bufferedReference.indexOf(swDiffs[0].substring(0, firstIndelPosition));
//							setActualCP(bufferedCP, offset, f);
//						} else {
//							logger.info("first indel is too close to beginning of read");
//							for (String s : swDiffs) {
//								logger.info("indel s: " + s);
//							}
//						}
//						
//					}
//				} else {
//					// snps and indels
//					snpsAndindels++;
//				}
//				
//			} else {
//				int diff = f.getLength() - swDiffs[0].length();
//				if (diff == 0) {
//					// perfect match - update actual location on fragment
//					int offset = bufferedReference.indexOf(swDiffs[0]);
//					setActualCP(bufferedCP, offset, f);
//					perfectMatchCount++;
//				} else {
//					if (diff == 20) {
//						diff20Count++;
//					} else {
//						diffOtherCount++;
//					}
//					logger.info("swdiff length: " + swDiffs[0].length() + ", frag length: " + f.getLength() + ", frag fs count: " + f.getFsCount() + ", frag rs count: " + f.getRsCount());
//					logger.info("frag: " + f.getSequence());
//					for (String s : swDiffs) {
//						logger.info("s: " + s);
//					}
//				}
//			}
		}
//		logger.info("no of perfect matches: " + perfectMatchCount + ", no with mutation: " + mutationCount + ", diff20Count: " + diff20Count + ", diffOtherCount: " + diffOtherCount + ", snpsOnlyCorrectLength: " + snpsOnlyCorrectLength + ", snpsOnlyWrongLength: " + snpsOnlyWrongLength + ", indelsOnly: " + indelsOnly + ", snpsAndindels: " + snpsAndindels);
		logger.info("number of reads that have actual cp set: " + actualCPReadCount + " which is " + ((double)actualCPReadCount / fastqRecordCount) * 100 + "%");
		logger.info("number of reads that DONT have actual cp set: " + noActualCPReadCount + " which is " + ((double)noActualCPReadCount / fastqRecordCount) * 100 + "%");
	}
	
	
	private void createMutations() {
		
		/*
		 * Only call variants on amplicons that contain more than 10 reads
		 * and on fragments that have more than twice the specified minimum fragment size
		 */
		ampliconFragmentMap.entrySet().stream()
			.filter(entry -> entry.getValue().stream().collect(Collectors.summingInt(Fragment::getRecordCount)) >= 10)
			.forEach(entry -> {
				
				entry.getValue().stream()
					.filter(f -> f.getActualPosition() != null &&  f.getRecordCount()  > minFragmentSize * 2)
					.forEach(f -> {
						
						String [] smithWatermanDiffs = f.getSmithWatermanDiffs();
						List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(smithWatermanDiffs);
						if ( ! mutations.isEmpty()) {
							for (Pair<Integer, String> mutation : mutations) {
								int position = mutation.getLeft().intValue();
								String mutString = mutation.getRight();
								int slashIndex = mutString.indexOf('/');
								String ref = mutString.substring(0, slashIndex);
								String alt = mutString.substring(slashIndex + 1);
								createMutation(f.getActualPosition(), position , ref, alt, entry.getKey().getId(), f.getId(), f.getRecordCount());
							}
						}
					});
			});
					
		
//		List<Fragment> fragmentsToCallVariantsOn = new ArrayList<>();
//		
//		/*
//		 * Only call variants on amplicons that contain more than 10 reads
//		 */
//		ampliconFragmentMap.values().stream()
//		.filter(list -> list.stream().collect(Collectors.summingInt(Fragment::getRecordCount)) >= 10)
//		.forEach(list -> fragmentsToCallVariantsOn.addAll(list));
//		
//		logger.info("number of fragments we will call mutations on: " + fragmentsToCallVariantsOn.size());
//		
//		
//		/*
//		 * Fragments must contain a certain number of reads (minFragmentSize)
//		 */
//		fragmentsToCallVariantsOn.stream()
//		.filter(f -> f.getActualPosition() != null &&  f.getRecordCount()  > minFragmentSize * 2)
//		.forEach(f -> {
//			
//			String [] smithWatermanDiffs = f.getSmithWatermanDiffs();
//			List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(smithWatermanDiffs);
//			if ( ! mutations.isEmpty()) {
//				for (Pair<Integer, String> mutation : mutations) {
//					int position = mutation.getLeft().intValue();
//					String mutString = mutation.getRight();
//					int slashIndex = mutString.indexOf('/');
//					String ref = mutString.substring(0, slashIndex);
//					String alt = mutString.substring(slashIndex + 1);
//					createMutation(f.getActualPosition(), position , ref, alt, f.getId(), f.getRecordCount());
//				}
//			}
//		});
	}
	
	private void createMutation(ChrPosition actualCP, int position, String ref, String alt, int ampliconId, int fragmentId, int fragmentRecordCount) {
		int startPos = actualCP.getPosition() + position;
		int endPos =  startPos + ref.length() -1 ;
		VcfRecord vcf = VcfUtils.createVcfRecord(new ChrPosition(actualCP.getChromosome(),  startPos, endPos), "."/*id*/, ref, alt);
		
		
		List<int[]> existingFragmentIds = vcfFragmentMap.get(vcf);
//		List<IntPair> existingFragmentIds = vcfFragmentMap.get(vcf);
		if (null == existingFragmentIds) {
			existingFragmentIds = new ArrayList<>();
			vcfFragmentMap.put(vcf, existingFragmentIds);
		}
		existingFragmentIds.add(new int[]{ampliconId, fragmentId, fragmentRecordCount});
//		existingFragmentIds.add(new IntPair(fragmentId, fragmentRecordCount));
	}
	
	
	
	private void setActualCP(ChrPosition bufferedCP, int offset, Fragment f, int referenceLength) {
		final int startPosition =  bufferedCP.getPosition() + offset + 1;	// we are 1 based
		// location needs to reflect reference bases consumed rather sequence length
		ChrPosition actualCP = new ChrPosition(bufferedCP.getChromosome(), startPosition, startPosition + referenceLength -1);
		f.setActualPosition(actualCP);
	}

	private int readFastqs() {
		int fastqCount = 0;
		/*
		 *  read in a fastq file and lets see if we get some matches
		 *  Keep stats on read lengths
		 */
		Map<IntPair, AtomicInteger> readLengthDistribution = new HashMap<>();
		int sameReadLength = 0;
		
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
				IntPair ip = new IntPair(rec.getReadString().length(), rec2.getReadString().length());
				if (ip.getInt1() == ip.getInt2()) {
					sameReadLength++;
				}
				AtomicInteger atomicI = readLengthDistribution.get(ip);
				if (null == atomicI) {
					atomicI = new AtomicInteger();
					readLengthDistribution.put(ip,  atomicI);
				}
				atomicI.incrementAndGet();
				fastqCount++;
			}
		}
		
		logger.info("no of unique fragments: " + reads.size() + ", from " + fastqCount + " fastq records, number with same read length: " + sameReadLength + " : " + ((double)sameReadLength / fastqCount) * 100 + "%");
		logger.info("read length breakdown:");
		
		readLengthDistribution.entrySet().stream()
			.sorted((e1, e2) -> {return e1.getKey().compareTo(e2.getKey());})
			.forEach(entry -> logger.info("r1: " + entry.getKey().getInt1() + ", r2: " + entry.getKey().getInt2() + ", count: " +entry.getValue().intValue()));
		
		
		return fastqCount;
	}
	
	private void logPositionAndFragmentCounts() {
		frags.entrySet().stream()
			.sorted((entry1, entry2) -> {return entry1.getValue().getBestTiledLocation().compareTo(entry2.getValue().getBestTiledLocation());})
			.forEach((entry) -> {
				logger.info("cp: " + entry.getValue().getBestTiledLocation().toIGVString() + "frag: " + entry.getKey() + ", no of fragments: " + entry.getValue().getRecordCount());
			});
	}
	
	private void loadTiledAlignerData() throws Exception {
		/*
		 * Loop through all our fragments (and reverse strand), split into 13mers and add to ampliconTiles 
		 */
		Set<String> fragmentTiles = new HashSet<>();
		for (String fragment : rawFragments.keySet()) {
			String revComp = SequenceUtil.reverseComplement(fragment);
			int sLength = fragment.length();
			int noOfTiles = sLength / TILE_SIZE;
			
			for (int i = 0 ; i < noOfTiles ; i++) {
				fragmentTiles.add(fragment.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE));
				fragmentTiles.add(revComp.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE));
			}
		}
		logger.info("Number of fragment tiles: " + fragmentTiles.size());
		
		logger.info("loading genome tiles alignment data");
		
		try (TabbedFileReader reader = new TabbedFileReader(new File(refTiledAlignmentFile))) {
//			try (TabbedFileReader reader = new TabbedFileReader(new File(refTiledAlignmentFile));
//					FileWriter writer = new FileWriter(new File(refTiledAlignmentFile + ".qiagen.condensed"))) {
			
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
				if (fragmentTiles.contains(tile)) {
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
		int diff = fragmentTiles.size() - total;
		logger.info("finished reading the genome tiles alignment data");
		logger.info("no of entries in refTilesCount: " + refTilesCountSize);
		logger.info("no of entries in refTilesPositions: " + refTilesPositionsSize);
		logger.info("Unique tiles in amplicons: " + diff);
		
		int bestLocationSet = 0, bestLocationNotSet = 0;
		int positionFound = 0, positionFoundReadCount = 0;
		int noPositionFound = 0, noPositionFoundReadCount = 0;
		for (String fragment : rawFragments.keySet()) {
			int fragLength = fragment.length();
			int noOfTiles = fragLength / TILE_SIZE;
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
						Arrays.sort(rcTilePositions[i]);
					} else {
						rcTilePositions[i] = new long[]{Long.MIN_VALUE};
					}
				}
				
				/*
				 * Get the best (ie. positions with more than 2 tiles aligned to it) positions for each strand
				 */
				TIntObjectHashMap<TLongArrayList> resultsMap = ClinVarUtil.getBestStartPosition(tilePositions, TILE_SIZE, maxIndelLength, tiledDiffThreshold, tileMatchThreshold);
				TIntObjectHashMap<TLongArrayList> rcResultsMap = ClinVarUtil.getBestStartPosition(rcTilePositions, TILE_SIZE, maxIndelLength, tiledDiffThreshold, tileMatchThreshold);
				
				ChrPosition bestTiledCp = null;
				int [] results = resultsMap.keys();
				if (results.length > 1) {
					Arrays.sort(results);
				}
				int [] rcResults = rcResultsMap.keys();
				if (rcResults.length > 1) {
					Arrays.sort(rcResults);
				}
				
				/*
				 * get best tile counts - could be zero if no matches above our threshold of 2...
				 */
				int bestTileCount = results.length > 0 ? results[results.length -1] : 0;
				int rcBestTileCount = rcResults.length > 0 ? rcResults[rcResults.length -1] : 0;
				/*
				 * If all the best positions overlap the amplicon position - great
				 */
//				if (ClinVarUtil.areAllPositionsClose(resultsMap.valueCollection(), rcResultsMap.valueCollection(), ampliconStartLongPosition, 200)) {
////					logger.info("all positions are close");
//					bestTiledCp = ampliconCP;
//				} else {
				
					/*
					 * Only perform sw on positions if the best tile position is not next to the amplicon position
					 */
				boolean forwardStrand = true;
					if (bestTileCount > rcBestTileCount + tiledDiffThreshold) {
						/*
						 * Only set bestTiledCp if we have a single key in the resultsMap, that only has a single long in its TLongArrayList value
						 */
						if (results.length == 1 && resultsMap.get(bestTileCount).size() == 1) {
							bestTiledCp = positionToActualLocation.getChrPositionFromLongPosition(resultsMap.get(bestTileCount).get(0));
						} else {
							logger.info("results.length: " + results.length + ", resultsMap.get(bestTileCount).size(): " + resultsMap.get(bestTileCount).size());
//							logger.info("(results.length != 1 &&/|| resultsMap.get(bestTileCount).size() != 1");
//							if (ClinVarUtil.areAllPositionsClose(resultsMap.valueCollection(), null, ampliconStartLongPosition, 200)) {
//								logger.info("all positions on +ve strand are close");
//								bestTiledCp = ampliconCP;
//							}
						}
					} else if (tiledDiffThreshold + bestTileCount < rcBestTileCount) {
						/*
						* Only set bestTiledCp if we have a single key in the resultsMap, that only has a single long in its TLongArrayList value
						*/
						if (rcResults.length == 1 && rcResultsMap.get(rcBestTileCount).size() == 1) {
							bestTiledCp = positionToActualLocation.getChrPositionFromLongPosition(rcResultsMap.get(rcBestTileCount).get(0));
							forwardStrand = false;
						} else {
							logger.info("rcResults.length: " + rcResults.length + ", rcResultsMap.get(rcBestTileCount).size(): " + rcResultsMap.get(rcBestTileCount).size());
//							if (ClinVarUtil.areAllPositionsClose(rcResultsMap.valueCollection(), null, ampliconStartLongPosition, 200)) {
//								logger.info("all positions on -ve strand are close");
//								bestTiledCp = ampliconCP;
//							}
						}
					}
//				}
				
				if (null != bestTiledCp) {
					positionFound++;
					
					RawFragment rf = rawFragments.get(fragment);
					
					String forwardStrandFragment = forwardStrand ? fragment : SequenceUtil.reverseComplement(fragment);
					int currentCount =  rf.getCount();
					positionFoundReadCount += currentCount;
					
					Fragment f = frags.get(forwardStrandFragment);
					if (null == f) {
						f = new Fragment(rawFragmentId++, forwardStrandFragment, forwardStrand ? currentCount : 0, forwardStrand ? 0 : currentCount, bestTiledCp, rf.getOverlapDistribution());
						frags.put(forwardStrandFragment, f);
					} else {
						// update count
						if (forwardStrand) {
							// check that we don't already have a fs count set!!
							if (f.getFsCount() != 0) {
								logger.warn("already have fs count for this fragment!!!");
							}
							f.setForwardStrandCount(currentCount);
						} else {
							if (f.getRsCount() != 0) {
								logger.warn("already have rs count for this fragment!!!");
							}
							f.setReverseStrandCount(currentCount);
						}
						f.addOverlapDistribution(rf.getOverlapDistribution());
					}
					
//					List<String> frags = positionFragmentsMap.get(bestTiledCp);
//					if (null == frags) {
//						frags = new ArrayList<String>();
//						positionFragmentsMap.put(bestTiledCp, frags);
//					}
//					frags.add(fragment);
					
//					logger.info("Got a position!!!:  " + bestTiledCp.toIGVString());
//					b.setBestTiledLocation(bestTiledCp);
//					updateMap(bestTiledCp, binLocationDistribution);
				} else {
				
//					logger.info("Did NOT get a position!!!");
//					logger.info("bestTileCount: " + bestTileCount + ", rcBestTileCount: " + rcBestTileCount);
					if (bestTileCount != 0 || rcBestTileCount != 0) {
						logger.info("Did NOT get a position!!!");
						logger.info("bestTileCount: " + bestTileCount + ", rcBestTileCount: " + rcBestTileCount);
						logger.info("frag: " + fragment);
					}
					noPositionFound++;
					noPositionFoundReadCount += rawFragments.get(fragment).getCount();
					/*
					 * Haven't got a best tiled location, or the location is not near the amplicon, so lets generate some SW diffs, and choose the best location based on those
					 */
//					logger.info("about to run a bunch of sw");
					
					if (bestTileCount > 1) {
						TLongArrayList list = ClinVarUtil.getSingleArray(resultsMap);
						logger.info("list size: " + list.size() + ", bestTileCount: " + bestTileCount);
						Map<ChrPosition, String[]> scores = getSWScores(list, fragment);
						logger.info("no of possible scores: " + scores.size());
//						b.addPossiblePositions(scores);
					}
					if (rcBestTileCount > 1) {
						TLongArrayList rclist = ClinVarUtil.getSingleArray(rcResultsMap);
						logger.info("rclist size: " + rclist.size() + ", rcBestTileCount: " + rcBestTileCount);
						Map<ChrPosition, String[]> scores = getSWScores(rclist, SequenceUtil.reverseComplement(fragment));
						logger.info("no of possible scores (rc): " + scores.size());
//						b.addPossiblePositions(scores);
					}
//					bestTiledCp = ClinVarUtil.getPositionWithBestScore(b.getSmithWatermanDiffsMap(), swDiffThreshold);
//					if (null != bestTiledCp) {
//						b.setBestTiledLocation(bestTiledCp);
//						updateMap(bestTiledCp, binLocationDistribution);
//					} else {
////						logger.info("not able to set best tiled location for bin: " + b.getId() + ", no in b.getSmithWatermanDiffsMap(): " + b.getSmithWatermanDiffsMap().size());
//					}
				}
				/*
				 * has best location been set?
				 */
//				if (null != b.getBestTiledLocation()) {
//					bestLocationSet++;
//				} else {
//					bestLocationNotSet++;
//				}
			}
		logger.info("positionFound count: " + positionFound + " which contain " + positionFoundReadCount + " reads,  noPositionFound count: " + noPositionFound + ", which contain "+ noPositionFoundReadCount + " reads");
		logger.info("bestLocationSet count: " + bestLocationSet + ", not set count: " + bestLocationNotSet);
	}
	
	
	private Map<ChrPosition, String[]> getSWScores(TLongArrayList positionsList, final String binSequence ) throws IOException {
		final Map<ChrPosition, String[]> positionSWDiffMap = new HashMap<>(positionsList.size() * 2);
		final int buffer = 300;
		positionsList.forEach(new TLongProcedure() {
			@Override
			public boolean execute(long position) {
				ChrPosition cp = positionToActualLocation.getChrPositionFromLongPosition(position);
				ChrPosition refCp =  new ChrPosition(cp.getChromosome(), Math.max(1, cp.getPosition() - buffer), cp.getPosition() + binSequence.length() + buffer);
//				ChrPosition refCp = positionToActualLocation.getBufferedChrPositionFromLongPosition(position, binSequence.length(), 200);
				String ref = getRefFromChrPos(refCp);
				positionSWDiffMap.put(cp, ClinVarUtil.getSwDiffs(ref, binSequence));
				return true;
			}
		});
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
					outputFileNameBase = optionsOutputFile;
				} else {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
			}
			
			refTiledAlignmentFile = options.getTiledRefFileName();
			refFileName = options.getRefFileName();
			bedFile = options.getBedFile();
			
			if (options.hasMinBinSizeOption()) {
				this.minBinSize = options.getMinBinSize().intValue();
			}
			if (options.hasMinFragmentSizeOption()) {
				this.minFragmentSize = options.getMinFragmentSize().intValue();
			}
			if (options.hasMinReadPercentageSizeOption()) {
				this.minReadPercentage = options.getMinReadPercentageSize().intValue();
			}
			if (options.hasAmpliconBoundaryOption()) {
				this.ampliconBoundary = options.getAmpliconBoundary().intValue();
			}
			logger.info("minBinSize is " + minBinSize);
			logger.info("minFragmentSize is " + minFragmentSize);
			logger.info("minReadPercentage is " + minReadPercentage);
			logger.info("ampliconBoundary is " + ampliconBoundary);
			
			
				return engage();
			}
		return returnStatus;
	}

}
