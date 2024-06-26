package au.edu.qimr.panel;

import au.edu.qimr.panel.model.Contig;
import au.edu.qimr.panel.model.Fragment2;
import au.edu.qimr.panel.model.IntPair;

import au.edu.qimr.panel.model.LongInt;
import au.edu.qimr.panel.model.PositionChrPositionMap;
import au.edu.qimr.panel.model.ReadOneTwoPosition;
import au.edu.qimr.panel.util.ClinVarUtil;
import au.edu.qimr.panel.util.FragmentUtil;
import au.edu.qimr.panel.util.PanelUtil;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.THashMap;
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

import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import org.apache.commons.lang3.StringUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.model.Transcript;
import org.qcmg.common.sw.SmithWatermanGotoh;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.IndelUtils;

import org.qcmg.common.util.Pair;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils.VcfInfoType;
import org.qcmg.qio.vcf.VcfFileReader;
import org.qcmg.qio.record.RecordWriter;
import org.qcmg.qio.record.StringFileReader;

public class Q3Panel {
	
	private static final Comparator<ChrPosition> COMPARATOR = new ChrPositionComparator();
	
	public static QLogger logger;
	private static final int TILE_SIZE = 13;
	private static final char [] AT = new char[]{'A','T'};
	private static final String POISON_STRING = "the end";
	
	private QExec exec;	
	private static List<String> fastqR1Files;
	private static List<String> fastqR2Files;
	private static String version;
	private String logFile;
	private String bedFile;
	private String geneTranscriptsFile;
	private String outputFileNameBase;
	private String refTiledAlignmentFile;
	private String refFileName;
	private final int tiledDiffThreshold = 1;
	private final int tileMatchThreshold = 2;
	private final int maxIndelLength = 5;
	private int fastqRecordCount;
	private int threadCount = 1;
	
	private int dbSnpTotalCount;
	private int cosmicTotalCount;
	
	private int mutationDbSnpRecordCount;
	private int mutationCosmicRecordCount;
	
	private int minBinSize = 10;
	private int minFragmentSize = 3;
	private int minReadPercentage = 2;
	private int contigBoundary = 10;
	private int multiMutationThreshold = 10;
	private int bamFilterDepth = 5;
	private int trimFromEndsOfReads = 0;
	final AtomicInteger outputMutations = new AtomicInteger();
	
	private boolean runExtendedFB = false;	
	private boolean singleSampleMode = false;
	
	private final Map<IntPair, AtomicInteger> readLengthDistribution = new THashMap<>(4);
	private final Map<String, String> frequentlyOccurringRefTiles = new ConcurrentHashMap<>();
	private final Map<String, TLongArrayList> refTilesPositions = new ConcurrentHashMap<>();
	
	private final PositionChrPositionMap positionToActualLocation = new PositionChrPositionMap();
	
	private final Map<String, byte[]> referenceCache = new THashMap<>();
	
	private final Map<String, Fragment2> frags = new ConcurrentHashMap<>(1024 * 1024);
	
	private final Map<ChrPosition, Map<String, List<Fragment2>>> variants = new THashMap<>();
	private final Map<VcfRecord, List<int[]>> vcfFragmentMap = new THashMap<>();
	private final List<VcfRecord> filteredMutations = new ArrayList<>();
	
	private Map<Contig, List<Fragment2>> contigFragmentMap = new THashMap<>();
	
	private final Map<Contig, List<Contig>> bedToAmpliconMap = new THashMap<>();
	private final Map<String, Transcript> transcripts = new THashMap<>();
	
	private final VcfHeader dbSnpHeaderDetails = new VcfHeader();
	private final VcfHeader cosmicHeaderDetails = new VcfHeader();
	private VcfHeader header = new VcfHeader();
	
	private int exitStatus;
	private String cosmicFile;
	private String dbSNPFile;
	
	protected int engage() throws Exception {
		readFastqsCreateFrags();
		
		/*
		 * remove fragments with less than the minFragmentSize number of records
		 */
		removeFragsMatchingLambda((e) -> e.getValue().getRecordCount() < minFragmentSize);
		
		loadTiledAlignerData();
		/*
		 * need to wait for cfLoadTiledAlignerData before digesting tiled data
		 */
		digestTiledData();
		
		/*
		 * Clear up some no longer used resources
		 * The frags collection is what contains the Fragments that we are interested in
		 */
		removeFragsMatchingLambda((e) -> e.getValue().getPosition() == null);
		
		getActualLocationForFrags();
		
		removeFragsMatchingLambda((e) ->  ! e.getValue().isActualPositionSet());
		
		createContigs();
		
		mapBedToAmplicons();
		
		loadTranscripts();
		
		createMutations();
		logger.info("number of mutations created: " + vcfFragmentMap.size());
		filterAndAnnotateMutations();
		logger.info("number of filtered mutations remaining: " + filteredMutations.size());
		
		List<VcfRecord> vcfs = new ArrayList<>();
		PanelUtil.createVcfRecordFromVariantMap(variants, contigFragmentMap, vcfs, minReadPercentage);
		writeMutationsToFileNew(vcfs);
		
		writeMutationsToFile();
		writeXml();
		writeBam();
		
		logger.info("number of fastq records: " + fastqRecordCount);
		return exitStatus;
	}
	
	private void removeFragsMatchingLambda(Predicate<Entry<String,Fragment2>> p) {
		logger.info("in removeFragsMatchingLambda with frags size: " + frags.size());
		
		List<String> toRemove = frags.entrySet().stream().filter(p).map(e -> e.getKey()).collect(Collectors.toList());
		logger.info("about to remove " + toRemove.size() + " from frags");
		
		for (String s : toRemove) {
			frags.remove(s);
		}
		
		logger.info("in removeFragsMatchingLambda - DONE.  frags size: " + frags.size());
	}
	
	private void readFastqsCreateFrags() {
		
		/*
		 * check to see if we are single sample mode - ie. both R1 and R2 are the same
		 */
		for (int i = 0 ; i < fastqR1Files.size() ; i++) {
			String fq1 = fastqR1Files.get(i);
			String fq2 = fastqR2Files.get(i);
			
			if (fq1.equals(fq2)) {
				singleSampleMode = true;
			}
		}
		
		/*
		 * setup an execution service - hard code thread count for now
		 * and a queue
		 */
		
		ExecutorService service = Executors.newFixedThreadPool(threadCount);
		Queue<ReadOneTwoPosition> queue = new ConcurrentLinkedQueue<>();
		AtomicInteger fragmentId = new AtomicInteger();
		
		/*
		 * setup threads to monitor queue and create fragments
		 */
		
		for (int i = 0 ; i < threadCount ; i++) {
			service.execute(() -> {
				int counter = 0;
				int basicMatchCount = 0;
				while (true) {
					
					ReadOneTwoPosition rotp = queue.poll();
					if (null != rotp) {
						String r1 = rotp.getR1();
						String r2 = rotp.getR2();
						if (r1.equals(POISON_STRING) && r2.equals(POISON_STRING)) {
							logger.info("Found poisoned string - exiting");
							break;
						}
						if (++counter % 1000000 == 0) {
							logger.info("hit: " + (counter / 1000000) + "M SW's... basicMatchCount: " + basicMatchCount);
						}
						String r2RevComp = singleSampleMode ? r2 : SequenceUtil.reverseComplement(r2);
						
						boolean readsAreTheSame = r1.equals(r2RevComp);
						if (readsAreTheSame) {
							frags.computeIfAbsent(PanelUtil.trimString(r1, trimFromEndsOfReads), v 
									-> new Fragment2(fragmentId.incrementAndGet(), r1)).addPosition(rotp.getPosition());
						} else {						
							Optional<String> basicFragment = PanelUtil.createBasicFragment(r1, r2RevComp, 10);
							if (basicFragment.isPresent()) {
								basicMatchCount++;
								frags.computeIfAbsent(PanelUtil.trimString(basicFragment.get(), trimFromEndsOfReads), v 
										-> new Fragment2(fragmentId.incrementAndGet(), basicFragment.get())).addPosition(rotp.getPosition());
 							} else {						
								SmithWatermanGotoh nm = new SmithWatermanGotoh(r1, r2RevComp, 5, -4, 16, 4);
								String [] newSwDiffs = nm.traceback();
								if (newSwDiffs[1].indexOf(" ") <= -1 && newSwDiffs[1].indexOf('.') <= -1 	
									&& newSwDiffs[1].length() >= 10 && ! StringUtils.containsOnly(newSwDiffs[0], AT)) {
									/*
									 * Now build fragment
									 * check to see which read starts with the overlap
									 */
									Optional<String> fragment = FragmentUtil.getFragmentString(r1,  r2RevComp, newSwDiffs[0]);
									if ( fragment.isPresent()) {
										frags.computeIfAbsent(PanelUtil.trimString(fragment.get(), trimFromEndsOfReads), v 
												-> new Fragment2(fragmentId.incrementAndGet(), fragment.get())).addPosition(rotp.getPosition());
									}									
								}
							}
						}
					} else {
						try {
							Thread.sleep(5);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
		
		/*
		 * shutdown service
		 */
		service.shutdown();
		
		/*
		 * read fastq files - if strings already exist in the map, don't add to the queue 
		 */
		int fastqCount = 0;
		int sameReadLength = 0;
		/*
		 *  read in a fastq file and lets see if we get some matches
		 *  Keep stats on read lengths
		 */		
		for (int i = 0 ; i < fastqR1Files.size() ; i++) {
			String fq1 = fastqR1Files.get(i);
			String fq2 = fastqR2Files.get(i);
			logger.info("reading records from " + fq1 + " and " + fq2);
		
			try (FastqReader reader1 = new FastqReader(new File(fq1));
					FastqReader reader2 = new FastqReader(new File(fq2));) {
				
				for (FastqRecord rec : reader1) {
					FastqRecord rec2 = reader2.next();
					if (fastqCount++ % 1000000 == 0 ) {
						logger.info("Hit " + (fastqCount / 1000000) + "M fastq records");
					}
					
					queue.add(new ReadOneTwoPosition(rec.getReadString(), rec2.getReadString(), fastqCount));
					
					IntPair ip = new IntPair(rec.getReadString().length(), rec2.getReadString().length());
					if (ip.getInt1() == ip.getInt2()) { 
						sameReadLength++;
					}
					readLengthDistribution.computeIfAbsent(ip, v -> new AtomicInteger()).incrementAndGet();
				}
			}
			logger.info("reading records from " + fq1 + " and " + fq2 + " - DONE, queue size: " + queue.size() + ", frags size: " + frags.size());
		}
		
		logger.info("Finished reading from fastq files. queue size: " + queue.size());
		for (int i = 0 ; i < threadCount ; i++) {
			queue.add(new ReadOneTwoPosition(POISON_STRING, POISON_STRING, -1));
		}
		
		try {
			service.awaitTermination(12, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		logger.info("number of unique fragments: " + frags.size() + ", from " + fastqCount + " fastq records, number with same read length: " + sameReadLength + " : " 
				+ ((double)sameReadLength / fastqCount) * 100 + "%");
		logger.info("read length breakdown:");
		
		readLengthDistribution.entrySet().stream().sorted((e1, e2) -> {
				return e1.getKey().compareTo(e2.getKey());
			}).forEach(entry -> logger.info("r1: " + entry.getKey().getInt1() + ", r2: " + entry.getKey().getInt2() + ", count: " + entry.getValue().intValue()));
		
		fastqRecordCount = fastqCount;
	}

	/**
	 * Only runs when we have both a bed and a transcripts file present.
	 * 
	 * @throws IOException if create file reader error occurs.
	 */
	private void loadTranscripts() throws IOException {
		
		if (StringUtils.isNoneBlank(geneTranscriptsFile)) {
			
			/*
			 * if we don't have a bed file, get contigs from the contigFragmentMap
			 */
			Map<String, List<Contig>> uniqueChrs = bedToAmpliconMap.isEmpty() ? contigFragmentMap.keySet().stream()
					.collect(Collectors.groupingBy(a -> a.getInitialFragmentPosition().getChromosome())) : bedToAmpliconMap.keySet().stream()
					.collect(Collectors.groupingBy(a -> a.getInitialFragmentPosition().getChromosome())) ;
			
					
			logger.info("Number of unique chromosomes in bed file: " + uniqueChrs.size());
			
			try (StringFileReader reader = new StringFileReader(new File(geneTranscriptsFile))) {
				String currentTranscriptId = null;
				for (String rec : reader) {
					String contig = rec.substring(0, rec.indexOf(Constants.TAB));
					if (uniqueChrs.containsKey(contig)) {
						String [] params = TabTokenizer.tokenize(rec);
						String [] column8 = params[8].split(Constants.SEMI_COLON_STRING); 
						Optional<String> optionalId = Arrays.stream(column8).filter(s -> s.trim().startsWith("transcript_id")).findAny();
						Optional<String> optionalExonNumber = Arrays.stream(column8).filter(s -> s.trim().startsWith("exon_number")).findAny();
						
						String id = null;
						if (optionalId.isPresent()) {
							int index1 = optionalId.get().indexOf("transcript_id \"");
							int index2 = optionalId.get().indexOf("\"", index1 + 15);
							id = optionalId.get().substring(index1 + 15, index2);
						}
						String exonNumber = null;
						if (optionalExonNumber.isPresent()) {
							int index1 = optionalExonNumber.get().indexOf("exon_number \"");
							int index2 = optionalExonNumber.get().indexOf("\"", index1 + 13);
							exonNumber = optionalExonNumber.get().substring(index1 + 13, index2);
						}
						
						if (null == currentTranscriptId || ! currentTranscriptId.equals(id)) {							
							if (null != currentTranscriptId) {
								Transcript t = transcripts.get(currentTranscriptId);
								ChrPosition tcp = new ChrRangePosition(params[0], t.getStart(), t.getEnd());
								logger.debug("check to see if any bed regions overlap current transcript : " + tcp.toIGVString());
								if (uniqueChrs.get(contig).stream()	
									.anyMatch(a -> ChrPositionUtils.doChrPositionsOverlapPositionOnly(tcp, a.getInitialFragmentPosition()))) {
									logger.info("transcript overlaps a bed region - keeping: " + tcp.toIGVString());
								} else {
									logger.debug("removing transcript: " + currentTranscriptId + " from map");
									transcripts.remove(currentTranscriptId);
								}
							}
							logger.debug("new transcript: " + id);
							currentTranscriptId = id;
						}
						
						/*
						 * Update existing transcript
						 */
						Transcript t = transcripts.get(id);
						if (null == t) {
							
							Optional<String> optionalGene = Arrays.stream(column8).filter(s -> s.trim().startsWith("gene_name")).findAny();
							
							String gene = null;
							if (optionalGene.isPresent()) {
								int index1 = optionalGene.get().indexOf("gene_name \"");
								int index2 = optionalGene.get().indexOf("\"", index1 + 11);
								gene = optionalGene.get().substring(index1 + 11, index2);
							}
							
							t = new Transcript(id, params[1], contig, gene);
							transcripts.put(id, t);
						}
						switch (params[2]) {
							case "exon": t.addExon(new ChrPositionName(params[0], Integer.parseInt(params[3]), Integer.parseInt(params[4]), exonNumber));
								break;
							case "CDS": t.addCDS(new ChrPositionName(params[0], Integer.parseInt(params[3]), Integer.parseInt(params[4]), exonNumber));
								break;
							default:  logger.debug("Ignoring " + params[2]);
								break;						
						}
					}
				}
			}
			
			/*
			 * remove any elements added to transcripts that are not within our beds
			 */
			List<String> transcriptsToRemove = new ArrayList<>();
			transcripts.values().stream()
			.forEach(t -> {
				List<Contig> amps = uniqueChrs.get(t.getContig());
				if (amps.stream().noneMatch(a -> a.getInitialFragmentPosition().getStartPosition() >= t.getStart() && a.getInitialFragmentPosition().getEndPosition() <= t.getEnd())) {
					transcriptsToRemove.add(t.getId());
				}
			});
			transcriptsToRemove.stream().forEach(id -> transcripts.remove(id));
			logger.info("Number of Transcripts covering bed regions: " + transcripts.size());
		}
	}

	private void createContigElement(Element contigs, Contig p, List<Fragment2> frags) {
		Element contig = new Element("Contig");
		contigs.appendChild(contig);
		
		// attributes
		contig.addAttribute(new Attribute("id", "" + p.getId()));
		contig.addAttribute(new Attribute("position", "" + p.getPosition().toIGVString()));
		contig.addAttribute(new Attribute("initial_frag_position", "" + p.getInitialFragmentPosition().toIGVString()));
		contig.addAttribute(new Attribute("contig_length", "" + p.getPosition().getLength()));
		contig.addAttribute(new Attribute("number_of_fragments", "" + frags.size()));

		AtomicInteger readCount = new AtomicInteger();
		Element fragments = new Element("Fragments");
		contig.appendChild(fragments);
		frags.stream()
			.sorted()
			.forEach(f -> {
				Element fragment = new Element("Fragment");
				fragments.appendChild(fragment);
				readCount.addAndGet(f.getRecordCount());
				fragment.addAttribute(new Attribute("id", "" + f.getId()));
				fragment.addAttribute(new Attribute("record_count", "" + f.getRecordCount()));
				fragment.addAttribute(new Attribute("position", "" + f.getPosition().toIGVString()));
				fragment.addAttribute(new Attribute("genomic_length", "" + f.getPosition().getLength()));
				fragment.addAttribute(new Attribute("overlap_dist", "" + ClinVarUtil.getOverlapDistributionAsString(f.getOverlapDistribution())));
				if (null == f.getSmithWatermanDiffs()) {
					fragment.addAttribute(new Attribute("md", f.getSequence().length() + "="));
				} else { 
					fragment.addAttribute(new Attribute("md", "" + ClinVarUtil.getSWDetails(f.getSmithWatermanDiffs())));
				}
				fragment.addAttribute(new Attribute("fragment_length", "" + f.getLength()));
				fragment.addAttribute(new Attribute("seq", "" + f.getSequence()));
			});
		
		contig.addAttribute(new Attribute("number_of_reads", "" + readCount.get()));
	}
	
	private void writeXml() {
		
		Element q3pElement = new Element("q3panel");
		q3pElement.addAttribute(new Attribute("start_time", exec.getStartTime().getValue()));
		q3pElement.addAttribute(new Attribute("finish_time", exec.getStartTime().getValue()));
		q3pElement.addAttribute(new Attribute("run_by_os", exec.getOsName().getValue()));
		q3pElement.addAttribute(new Attribute("run_by_user", exec.getRunBy().getValue()));
		q3pElement.addAttribute(new Attribute("records_parsed", "" + fastqRecordCount));
		q3pElement.addAttribute(new Attribute("version", exec.getToolVersion().getValue()));
		q3pElement.addAttribute(new Attribute("reference", refFileName));
		q3pElement.addAttribute(new Attribute("tiled_reference", refTiledAlignmentFile));
		q3pElement.addAttribute(new Attribute("bed", null != bedFile ? bedFile : "-"));
		q3pElement.addAttribute(new Attribute("bed_amplicon_count", "" + bedToAmpliconMap.size()));
		q3pElement.addAttribute(new Attribute("vcf", outputFileNameBase + ".vcf"));
		q3pElement.addAttribute(new Attribute("vcf_variant_count", "" + outputMutations.get()));
		for (int i = 0 ; i < fastqR1Files.size() ; i++) {
			String r1 = fastqR1Files.get(i);
			String r2 = fastqR2Files.get(i);
			q3pElement.addAttribute(new Attribute("fastq_1", r1));
			q3pElement.addAttribute(new Attribute("fastq_2", r2));
		}
		q3pElement.addAttribute(new Attribute("dbSnp", dbSNPFile));
		q3pElement.addAttribute(new Attribute("dbSnp_total_count", dbSnpTotalCount + ""));
		q3pElement.addAttribute(new Attribute("dbSnp_mutation_count", mutationDbSnpRecordCount + ""));
		q3pElement.addAttribute(new Attribute("COSMIC", cosmicFile));
		q3pElement.addAttribute(new Attribute("cosmic_total_count", cosmicTotalCount + ""));
		q3pElement.addAttribute(new Attribute("cosmic_mutation_count", mutationCosmicRecordCount + ""));
		q3pElement.addAttribute(new Attribute("gene_transcripts", geneTranscriptsFile));
		q3pElement.addAttribute(new Attribute("gene_transcripts_count", transcripts.size() + ""));
		
		/*
		 * read length dist
		 */
		Element rls = new Element("ReadLengths");
		readLengthDistribution.entrySet().stream().sorted((e1, e2) -> {
				return e1.getKey().compareTo(e2.getKey());
			}).forEach(entry -> {
				Element rl = new Element("ReadLength");
				rl.addAttribute(new Attribute("r1", "" + entry.getKey().getInt1()));
				rl.addAttribute(new Attribute("r2", "" + entry.getKey().getInt2()));
				rl.addAttribute(new Attribute("count", "" + entry.getValue().intValue()));
				rls.appendChild(rl);
			});
	
		q3pElement.appendChild(rls);
		
		// logging and writing to file
		Element contigs = new Element("Contigs");
		q3pElement.appendChild(contigs);
		contigFragmentMap.entrySet().stream()
			.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
			.forEach(entry -> {
				createContigElement(contigs, entry.getKey(), entry.getValue());
			});
		
		Element bedAmplicons = new Element("BedAmplicons");
		q3pElement.appendChild(bedAmplicons);
		bedToAmpliconMap.entrySet().stream()
			.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
			.forEach(entry -> {
				
				Element amplicon = new Element("BedAmplicon");
				bedAmplicons.appendChild(amplicon);
				
				// attributes
				amplicon.addAttribute(new Attribute("id", "" + entry.getKey().getId()));
				amplicon.addAttribute(new Attribute("position", "" + entry.getKey().getPosition().toIGVString()));
				amplicon.addAttribute(new Attribute("amplicon_length", "" + entry.getKey().getPosition().getLength()));
				amplicon.addAttribute(new Attribute("number_of_overlapping_fragment_contigs", "" + entry.getValue().size()));
				
				String idLIst = entry.getValue().stream()
					.sorted((a1, a2) -> COMPARATOR.compare(a1.getPosition(),a2.getPosition()))
					.map(a -> a.getId() + "")
					.collect(Collectors.joining(","));
				
				amplicon.addAttribute(new Attribute("overlapping_fragment_contigs_ids", "" + idLIst));
		});
		
		/*
		 * Vcf records
		 */
		Element vcfs = new Element("VcfRecords");
		q3pElement.appendChild(vcfs);
		filteredMutations.stream()
			.sorted((v1, v2) -> COMPARATOR.compare(v1.getChrPosition(), v2.getChrPosition()))
			.forEach(v -> {
				
				Element vcf = new Element("VcfRecord");
				vcfs.appendChild(vcf);
				vcf.appendChild(v.toString());				
			});
		
		/*
		 * Transcript records
		 */
		Element transcriptsE = new Element("Transcripts");
		q3pElement.appendChild(transcriptsE);
		transcripts.values().stream().sorted().forEach(v -> {
			
			Element transcript = new Element("Transcript");
			transcriptsE.appendChild(transcript);			
			transcript.addAttribute(new Attribute("id", "" + v.getId()));
			transcript.addAttribute(new Attribute("type", "" + v.getType()));
			transcript.addAttribute(new Attribute("gene", "" + v.getGene()));
			transcript.addAttribute(new Attribute("contig", "" + v.getContig()));
			transcript.addAttribute(new Attribute("start", "" + v.getStart()));
			transcript.addAttribute(new Attribute("end", "" + v.getEnd()));
			transcript.addAttribute(new Attribute("exon_count", "" + v.getExons().size()));
			transcript.addAttribute(new Attribute("cds_count", "" + v.getCDSs().size()));
			
			if ( ! v.getExons().isEmpty()) {
				Element exons = new Element("Exons");
				transcript.appendChild(exons);
				
				v.getExons().stream()
					.sorted()
					.forEach(cp -> {
						Element exon = new Element("Exon");
						exons.appendChild(exon);
						exon.addAttribute(new Attribute("position", cp.toIGVString()));
						exon.addAttribute(new Attribute("exon_number", cp.getName()));
					});
			}
			
			if ( ! v.getCDSs().isEmpty()) {
				Element cdss = new Element("CDSs");
				transcript.appendChild(cdss);
				
				v.getCDSs().stream()
				.sorted()
				.forEach(cp -> {
					Element cds = new Element("CDS");
					cdss.appendChild(cds);
					cds.addAttribute(new Attribute("position", cp.toIGVString()));
					cds.addAttribute(new Attribute("exon_number", cp.getName()));
				});
			}
		});
		
		// write output
		Document doc = new Document(q3pElement);
		try (OutputStream os = new GZIPOutputStream(new FileOutputStream(outputFileNameBase + ".q3p.xml.gz"), 1024 * 1024)) {
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
			try (StringFileReader reader = new StringFileReader(new File(bedFile));) {
				for (String rec : reader) {
					String [] params = TabTokenizer.tokenize(rec);
					ChrPosition cp = new ChrRangePosition(params[0], Integer.parseInt(params[1]), Integer.parseInt(params[2]));
					bedToAmpliconMap.put(new Contig(++bedId, cp), new ArrayList<Contig>(1));
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
			 * log distribution of contig lengths
			 */
			Map<Integer, Long> contigLengthDistribution = contigFragmentMap.keySet().stream()
					.collect(Collectors.groupingBy(a -> a.getPosition().getLength(), Collectors.counting()));
			contigLengthDistribution.entrySet().stream()
				.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
				.forEach(e -> logger.info("contig length, and count: " + e.getKey().intValue() + " : " + e.getValue().longValue()));
			
			/*
			 * Assign contigs to bed poisitions
			 */
			contigFragmentMap.keySet().stream().forEach(a -> {
					List<Contig> beds = bedToAmpliconMap.keySet().stream()
						.filter(bed -> ChrPositionUtils.isChrPositionContained(a.getPosition(), bed.getPosition())
								&& a.getPosition().getStartPosition() < (bed.getPosition().getStartPosition() + 10)
								&& a.getPosition().getEndPosition() > (bed.getPosition().getEndPosition() + 10)
								)
						.collect(Collectors.toList());
					
					if (beds.size() > 1) {
						logger.info("Found " + beds.size() + " bed positions that are contained by this contig " + a.getPosition().toIGVString());
						beds.stream().forEach(b -> logger.info("bed: " + b.toString()));
					}
				});		
		}
	}
	
	private void createContigs() {
		
		contigFragmentMap = ClinVarUtil.groupFragments(frags.values(), contigBoundary);
		/*
		 * Get some stats on each Contig
		 * # of fragments, and reads to start with
		 */
		contigFragmentMap.entrySet().stream().forEach(entry -> {
				int recordCount = entry.getValue().stream().mapToInt(Fragment2::getRecordCount).sum();
				logger.info("Contig " + entry.getKey().getId() + " " + entry.getKey().getPosition().toIGVString() + " has " + entry.getValue().size() 
						+ " fragments with a total of " + recordCount + " records (" + ((double) recordCount / fastqRecordCount) * 100 + "%)");
			});
	}
	
	private void writeBam() throws IOException {
		
		SAMFileHeader header = new SAMFileHeader();
		header.setSequenceDictionary(ClinVarUtil.getSequenceDictionaryFromFragments(frags.values()));
		header.setSortOrder(SortOrder.coordinate);
		
		SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		factory.setCreateIndex(true);
		String outputFileName = outputFileNameBase + ".bam";
		File bamFile = new File(outputFileName);
		SAMFileWriter writer = factory.makeSAMOrBAMWriter(header, false, bamFile);
		
		final AtomicLong recordCount = new AtomicLong();
		try {			
			frags.values().stream()
				.filter(f -> f.getPosition() != null)
				.filter(f -> f.getRecordCount() >= bamFilterDepth)
				.sorted((f1,f2) -> COMPARATOR.compare(f1.getPosition(), f2.getPosition()))
				.forEach(f -> {
					int fragId = f.getId();
					recordCount.addAndGet(f.getRecordCount());
					String [] swDiffs = f.getSmithWatermanDiffs();
					
					/*
					 * Get contig id that this fragment belongs to
					 */
					AtomicInteger contigId = new AtomicInteger();
					contigFragmentMap.entrySet().stream()
						.filter(entry -> ChrPositionUtils.isChrPositionContained(entry.getKey().getPosition(), f.getPosition()))
						.forEach(entry -> {
							if (entry.getValue().stream()
								.anyMatch(fr -> fr.getId() == fragId)) {
								contigId.set(entry.getKey().getId());
							}
								
						});
					/*
					 * assign different mapping quality based on record count within fragment
					 */
					int mappingQuality = f.getRecordCount() > minFragmentSize ? 60 : 20;
					/*
					 * Deal with exact matches first
					 */
					boolean containsIndel = ClinVarUtil.doesSWContainIndel(swDiffs);
									
					if (containsIndel) {
						String ref = StringUtils.remove(swDiffs[0], Constants.MINUS);
						Cigar cigar = ClinVarUtil.getCigarForIndels(ref,   f.getSequence(), swDiffs,  f.getPosition());
						if (cigar.toString().equals("51M53I-52M7I159M")) {
							logger.info("cigar: " + cigar.toString());
							logger.info("ref: " + ref);
							logger.info("f.getActualPosition(): " + f.getPosition().toIGVString());
							logger.info("f.getSequence(): " + f.getSequence());
							logger.info("cigar: " + cigar.toString());
							for (String s : swDiffs) {
								logger.info("s: " + s);
							}
						}
						ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, contigId.get(), ref, f, 0, mappingQuality);
					} else {
						
						/*
						 * all other records should be handled the same way as the cigar doesn't distinguish between match and mismatch
						 */
						if (swDiffs[1].length() == f.getLength()) {
							// just snps and same length
							Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(f.getLength());
							ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, contigId.get(), swDiffs[0], f, 0, mappingQuality);
						} else {
							logger.info("snps only but length differs, swDiffs[1].length(): " + swDiffs[1].length() + ", f.getLength(): " + f.getLength());
						}						
					}					
				});			
		} finally {
			writer.close();
		}
		logger.info("No of records written to bam file: " + recordCount.longValue());
	}
	
	private void filterAndAnnotateMutations() throws IOException {
		
		/*
		 * create map of contig -> Fragment for use by getMutationCoveragePercentage 
		 */
		Map<String, List<Fragment2>> fragsByContig = frags.values().stream().filter(f -> f.isActualPositionSet()).collect(Collectors.groupingBy(f -> f.getPosition().getChromosome()));
		
		Map<String, Map<Contig, List<Fragment2>>> contigFragmentMapByContig = new HashMap<>();
		for (Entry<Contig , List<Fragment2>> entry : contigFragmentMap.entrySet()) {
			ChrPosition cp = entry.getKey().getPosition();
			if (null != cp) {
				String contig = cp.getChromosome();
				contigFragmentMapByContig.computeIfAbsent(contig, k -> new HashMap<>()).put(entry.getKey(), entry.getValue());
			}
		}
		
		vcfFragmentMap.entrySet().stream()
		.filter((entry) -> PanelUtil.getRecordCountFromIntPairs(entry.getValue()) >= minBinSize ).sorted((e1, e2) -> {
			return e1.getKey().compareTo(e2.getKey());
		}).forEach(entry -> {		
			int [] altAndTotalCoverage = PanelUtil.getAltAndTotalCoverage(entry.getKey(), entry.getValue(), fragsByContig);
			/*
			 * get percentage of alts from altAndTotalCoverage array and only proceed if we are over the threshold
			 */
			double altPercentage = altAndTotalCoverage[1] > 0 ? ((double)altAndTotalCoverage[0] / altAndTotalCoverage[1]) * 100 : 0.0;
			if (altPercentage >= minReadPercentage) {			
				final StringBuilder fb = new StringBuilder();
				final StringBuilder xFb = new StringBuilder();
				int[] contigIds = new int[entry.getValue().size()];
				int[] fragmentIds = new int[entry.getValue().size()];
				AtomicInteger j = new AtomicInteger();
				AtomicInteger readCount = new AtomicInteger();
				entry.getValue().stream().forEach(i -> {
						if (xFb.length() > 0) {
							xFb.append(Constants.SEMI_COLON);
						}
						readCount.addAndGet(i[2]);
						contigIds[j.get()] = i[0];
						fragmentIds[j.getAndIncrement()] = i[2];
						if (runExtendedFB) {
							xFb.append(i[0]).append(Constants.COMMA).append(i[1]).append(Constants.COMMA).append(i[2]);
						}
					});
				
				fb.append(Arrays.stream(contigIds).distinct().count()).append(",");
				fb.append(Arrays.stream(fragmentIds).distinct().count()).append(",");
				fb.append(readCount.get());
				
				/*
				 * update vcf format fields with FB and GT
				 */				
				ClinVarUtil.getCoverageStatsForVcf(entry.getKey(), contigFragmentMapByContig, fb, xFb, altAndTotalCoverage);
				filteredMutations.add(entry.getKey());
			}
		});
				
		Map<ChrPosition, List<VcfRecord>> cpVcfMap = filteredMutations.stream()
				.collect(Collectors.groupingBy(VcfRecord::getChrPosition));
				
		/*
		 * COSMIC
		 */
		if ( ! StringUtils.isBlank(cosmicFile)) {
			AtomicInteger cosmicCount = new AtomicInteger();
			Map<ChrPosition, List<String[]>> cosmicData = new HashMap<>();
			try (Stream<String> lines = PanelUtil.lines(Paths.get(cosmicFile))) {
				cosmicData = lines.filter(s -> ! s.startsWith("Gene name"))
					.map(s -> TabTokenizer.tokenize(s))
					.filter(p -> StringUtils.isNotEmpty(p[23]) && cosmicCount.incrementAndGet() > -1)
					.filter(p -> cpVcfMap.containsKey(ChrPositionUtils.createCPFromCosmic(p[23])))
					.collect(Collectors.groupingBy(p -> ChrPositionUtils.createCPFromCosmic(p[23])));
				
				cosmicTotalCount = cosmicCount.get();
				mutationCosmicRecordCount = cosmicData.values().stream()
					.collect(Collectors.summingInt(list -> list.size()));
				
				logger.info("no of entries in cosmicData: " + cosmicData.size() + " with total cosmic record count: " + mutationCosmicRecordCount);
				
				cosmicData.entrySet().stream().forEach( entry -> {
						logger.info("k: " + entry.getKey().toIGVString());
						String genes = entry.getValue().stream()
							.map(p -> p[0])
							.distinct()
							.collect(Collectors.joining(Constants.COMMA_STRING));
						String cosmicId = entry.getValue().stream()
								.map(p -> p[16])
								.distinct()
								.collect(Collectors.joining(Constants.SEMI_COLON_STRING));
						String cds = entry.getValue().stream()
								.map(p -> p[17])
								.distinct()
								.collect(Collectors.joining(Constants.COMMA_STRING));
						logger.info("genes: " + genes + ", cosmicId: " + cosmicId + ", cds: " + cds);
						
						/*
						 * update vcf records with cosmic id and annotations
						 */
						List<VcfRecord> vcfs = cpVcfMap.get(entry.getKey());
						vcfs.stream()
							.forEach(v -> {
								v.appendId(cosmicId);
								v.appendInfo("CCDS=" + cds + ";CG=" + genes);
							});
					});
				cosmicHeaderDetails.addOrReplace("##INFO=<ID=CG,Number=.,Type=String,Description=\"Gene as reported by Cosmic\">");
				cosmicHeaderDetails.addOrReplace("##INFO=<ID=CCDS,Number=.,Type=String,Description=\"CDS as reported by Cosmic\">");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/*
		 * dbSNP
		 */
		if ( ! StringUtils.isBlank(dbSNPFile)) {
			logger.info("Reading in dbsnp data from: " + dbSNPFile);
			try ( VcfFileReader reader = new VcfFileReader( dbSNPFile )) {				
				VcfHeaderRecord re = reader.getVcfHeader().firstMatchedRecord(VcfHeaderUtils.STANDARD_DBSNP_LINE);
				if ( re != null ) {
					dbSnpHeaderDetails.addOrReplace(String.format("##INFO=<ID=%s,Number=0,Type=%s,Description=\"%s\",Source=%s,Version=%s>",
							VcfHeaderUtils.INFO_DB, VcfInfoType.Flag.name(),
							VcfHeaderUtils.INFO_DB_DESC, dbSNPFile,re.getMetaValue()  ));
				}
				
				re = reader.getVcfHeader().getInfoRecord(VcfHeaderUtils.INFO_CAF);
				if ( re != null ) dbSnpHeaderDetails.addInfo(VcfHeaderUtils.INFO_VAF, ".", "String", VcfHeaderUtils.INFO_VAF_DESC);
				re = reader.getVcfHeader().getInfoRecord(VcfHeaderUtils.INFO_VLD);
				if (re != null) dbSnpHeaderDetails.addOrReplace(re);				
				
				dbSnpHeaderDetails.addInfo("DB_CDS", ".", "String", "Reference and Alt alleles as reported by dbSNP");
				
				for (final VcfRecord dbSNPVcf : reader) {
					dbSnpTotalCount++;
					/*
					 * snps
					 */
					int start = dbSNPVcf.getPosition();
					final String chr = IndelUtils.getFullChromosome(dbSNPVcf.getChromosome());
					ChrPosition chrPos;
					if (dbSNPVcf.getInfo().contains("VC=SNV")) {
						chrPos = ChrPointPosition.valueOf(chr, start);
					} else {
						
						/*
						 * Taken from ftp://ftp.ncbi.nih.gov/snp/specs/00--VCF_README.txt
						 * 
							A note about the position.
							
							The RSPOS tag is the position of the SNP in dbSNP and the position reported in
							column 2 may differ from the RSPOS tag.  All alleles for an INDEL or multi-byte
							SNP must begin with the same nucleotide and to accomplish this, the preceeding
							base pair is prefixed to each allele and the position of this base pair is 
							reported.
							Also, if all of the alleles consist of the same repeated sequence or a deletion
							the beginning of the repeat is calculated and the preceeding base pair is 
							reported.
							For example, if the variations are AT/ATAT/-, the position in column 2 is
							the location of the first repeat (AT) minus one.
						 */
						
						final int end =  dbSNPVcf.getRef().length() +  (start - 1);
						chrPos = new ChrRangePosition(chr, start, end);
					}
					
					List<VcfRecord> mutations = cpVcfMap.get(chrPos);
					if (null == mutations || mutations.isEmpty()) continue;					 
					
					mutationDbSnpRecordCount++;
					
					for (VcfRecord mut : mutations) {
						mut.appendId(dbSNPVcf.getId());
						mut.appendInfo(VcfHeaderUtils.INFO_DB);
						mut.appendInfo("DB_CDS=" + dbSNPVcf.getRef() + ">" + dbSNPVcf.getAlt());
					}
				}
			}
			logger.info("Reading in dbsnp data from: " + dbSNPFile + " - DONE");
		}
		
		addFormatFieldValues();
	}
	
	private void addFormatFieldValues() {
		/*
		 * distance from end of read
		 */
		
		filteredMutations.stream()
		.forEach(vcf -> {
			final AtomicInteger middleOfReadCount = new AtomicInteger();
			final AtomicInteger endOfReadCount = new AtomicInteger();
			vcfFragmentMap.get(vcf).stream()
			.forEach(array -> {
				contigFragmentMap.get(new Contig(array[0], null)).stream()
				.filter(f -> f.getId() == array[1])
				.forEach(f -> {
					if (vcf.getPosition() - f.getPosition().getStartPosition() <= 5
							|| f.getPosition().getEndPosition() - vcf.getChrPosition().getEndPosition() <= 5) {
						endOfReadCount.addAndGet(f.getRecordCount());
					} else {
						middleOfReadCount.addAndGet(f.getRecordCount());
					}
				});
				
			});
			
			/*
			 * Add filter to vcf if we only have end of reads - may want to be a little more strict than this
			 */
			if (middleOfReadCount.get() == 0 && endOfReadCount.get() > 0) {
				/*
				 * add to format filter rather than filter
				 */
				VcfUtils.addFormatFieldsToVcf(vcf, Arrays.asList("FT", SnpUtils.END_OF_READ + Constants.EQ +  endOfReadCount.get()), true, Constants.COMMA);
			}
		});
		
		/*
		 * OABS
		 */
		
	}

	private void writeMutationsToFile() throws IOException {
		try (RecordWriter<VcfRecord> writer = new RecordWriter<>(new File(outputFileNameBase + ".vcf"))) {
			
			/*
			 * Setup the VcfHeader
			 */
			final DateFormat df = new SimpleDateFormat("yyyyMMdd");
			String date = df.format(Calendar.getInstance().getTime());
			
			header.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);		
			header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + date);		
			header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + QExec.createUUid());		
			VcfHeaderUtils.addQPGLine(header,1, "Q3Panel", exec.getToolVersion().getValue(), exec.getCommandLine().getValue(), date);
			header.addFormat(VcfHeaderUtils.FORMAT_READ_DEPTH, ".","Integer",VcfHeaderUtils.FORMAT_READ_DEPTH_DESCRIPTION);
			header.addFormat("FB", ".","String","Sum of contigs supporting the alt,sum of fragments supporting the alt,sum of read counts supporting the alt");
			header.addFormat("FT", ".","String","Filters that have been applied to the sample");
			header.addFormat(VcfHeaderUtils.FORMAT_MUTANT_READS, ".","Integer",VcfHeaderUtils.FORMAT_MUTANT_READS_DESC);
			header.addFormat(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND, ".","String",VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND_DESC);
			header.addFormat("XFB", ".","String","Breakdown of Contig ids, Fragment ids and read counts supporting this mutation, along with total counts of contig, fragment,"
					+ " and reads for all reads at that location in the following format: ContigId,FragmentId,readCount;[...] / Sum of contigs at this position,"
					+ " sum of fragments at this position,sum of read counts at this position");
			header.addFilter(SnpUtils.END_OF_READ, "Indicates that the mutation occurred within the first or last 5 bp of all the reads contributing to the mutation");
			header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "sample1");
			header = VcfHeaderUtils.mergeHeaders(header, dbSnpHeaderDetails, true);
			header = VcfHeaderUtils.mergeHeaders(header, cosmicHeaderDetails, true);
			
			Iterator<VcfHeaderRecord> iter = header.iterator();
			while (iter.hasNext()) {
				writer.addHeader(iter.next().toString() );
			}
			filteredMutations.stream().sorted((e1, e2) -> {
					return e1.compareTo(e2);
				}).forEach(entry -> {
					try {
						writer.add(entry);
						outputMutations.incrementAndGet();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			logger.info("number of mutations written to file (" + outputFileNameBase + ".vcf): " + outputMutations.intValue());
		}
	}
	
	private void writeMutationsToFileNew(List<VcfRecord> vcfs) throws IOException {
		String filename = outputFileNameBase + "_2.vcf";
		try (RecordWriter<VcfRecord> writer = new RecordWriter<>(new File(filename))) {
			
			/*
			 * Setup the VcfHeader
			 */
			final DateFormat df = new SimpleDateFormat("yyyyMMdd");
			String date = df.format(Calendar.getInstance().getTime());
			
			header.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);		
			header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + date);		
			header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + QExec.createUUid());		
			header.addOrReplace(VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=sample1");		
			VcfHeaderUtils.addQPGLine(header,1, "Q3Panel", exec.getToolVersion().getValue(), exec.getCommandLine().getValue(), date);
			header.addFormat(VcfHeaderUtils.FORMAT_READ_DEPTH, ".","Integer",VcfHeaderUtils.FORMAT_READ_DEPTH_DESCRIPTION);
			header.addFormat(VcfHeaderUtils.FORMAT_GENOTYPE, ".","String",VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS);
			header.addFormat(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS, ".","String",VcfHeaderUtils.FORMAT_AD_DESC);
			header.addFormat(VcfHeaderUtils.FORMAT_READ_DEPTH, ".","String",VcfHeaderUtils.FORMAT_READ_DEPTH_DESCRIPTION);
			header.addFormat("FB", ".","String","Sum of contigs supporting the alt,sum of fragments supporting the alt,sum of read counts supporting the alt");
			header.addFormat("FT", ".","String","Filters that have been applied to the sample");
			header.addFormat(VcfHeaderUtils.FORMAT_MUTANT_READS, ".","Integer",VcfHeaderUtils.FORMAT_MUTANT_READS_DESC);
			header.addFormat(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND, ".","String",VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND_DESC);
			header.addFormat("XFB", ".","String","Breakdown of Contig ids, Fragment ids and read counts supporting this mutation,"
					+ " along with total counts of contig, fragment, and reads for all reads at that location in the following format: ContigId,FragmentId,readCount;[...] "
					+ "/ Sum of contigs at this position,sum of fragments at this position,sum of read counts at this position");
			header.addFilter(SnpUtils.END_OF_READ, "Indicates that the mutation occurred within the first or last 5 bp of all the reads contributing to the mutation");
			header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "sample1");
			header = VcfHeaderUtils.mergeHeaders(header, dbSnpHeaderDetails, true);
			header = VcfHeaderUtils.mergeHeaders(header, cosmicHeaderDetails, true);
						
			Iterator<VcfHeaderRecord> iter = header.iterator();
			while (iter.hasNext()) {
				writer.addHeader(iter.next().toString() );
			}
			vcfs.stream().sorted((e1, e2) -> {
				return e1.compareTo(e2);
			}).forEach(entry -> {
				try {
					writer.add(entry);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			logger.info("number of mutations written to file (" + filename + "): " + vcfs.size());
		}
	}	
	
	private void getActualLocationForFrags() {
		
		/*
		 * setup an execution service - hard code thread count for now
		 * and a queue
		 */
		ExecutorService service = Executors.newFixedThreadPool(threadCount);
		Queue<Fragment2> queue = new ConcurrentLinkedQueue<>(frags.values());
		
		AtomicInteger noOfExactMatches = new AtomicInteger();
		AtomicInteger actualCPReadCount = new AtomicInteger();
		AtomicInteger noActualCPReadCount = new AtomicInteger();
		
		/*
		 * setup threads to monitor queue and create fragments
		 */
		for (int i = 0 ; i < threadCount ; i++) {
			service.execute(() -> {
				int counter = 0;
				while (true) {
					
					Fragment2 f = queue.poll();
					if (null != f) {
						if (++counter % 1000000 == 0) {
							logger.info("hit: " + (counter / 1000000) + "M Frags...");
						}
						
						ChrPosition bestTiledCP = f.getPosition();
						if (null != bestTiledCP) {
							ChrPosition bufferedCP = new ChrRangePosition(bestTiledCP.getChromosome(),  
									Math.max(1,bestTiledCP.getStartPosition() - 100), bestTiledCP.getStartPosition() + 100 + f.getLength());
							String bufferedReference = getRefFromChrPos(bufferedCP);
							
							/*
							 * if  fragment exists in buffered reference, job done, otherwise use SW
							 */
							String fragString = f.isForwardStrand() ? f.getSequence() : SequenceUtil.reverseComplement(f.getSequence());
							int index = bufferedReference.indexOf(fragString);
							if (index > -1) {
								PanelUtil.setActualCP(bufferedCP, index, f, fragString.length());
								actualCPReadCount.addAndGet(f.getRecordCount());
								noOfExactMatches.incrementAndGet();
								/*
								 * create matching swdiffs array
								 */
								String[] swMatches = new String[3];
								swMatches[0] = fragString;
								swMatches[1] = StringUtils.repeat('|',fragString.length());
								swMatches[2] = fragString;
								f.setSWDiffs(swMatches);
							} else {
							
								String [] swDiffs = ClinVarUtil.getSwDiffs(bufferedReference, fragString, true);
								f.setSWDiffs(swDiffs);
								
								
								String swFragmentMinusDeletions = StringUtils.remove(swDiffs[2], Constants.MINUS);
								
								if (fragString.equals(swFragmentMinusDeletions)) {
									/*
									 * Fragment is wholly contained in swdiffs, and so can get actual position based on that
									 */
									String swRefMinusDeletions = StringUtils.remove(swDiffs[0], Constants.MINUS);
									int offset = bufferedReference.indexOf(swRefMinusDeletions);
									int refLength = swRefMinusDeletions.length();
									PanelUtil.setActualCP(bufferedCP, offset, f, refLength);
									actualCPReadCount.addAndGet(f.getRecordCount());
									
								} else {
									logger.debug("fragment length: " + f.getLength() + ", differs from swDiffs: " + swFragmentMinusDeletions.length());
									logger.debug("frag: " + fragString);
									logger.debug("swDiffs[2]: " + swDiffs[2]);
									logger.debug("bufferedRef: " + bufferedReference);
									noActualCPReadCount.addAndGet(f.getRecordCount());
								}
							}
						}
						
					} else {
						/*
						 * if rf is null, queue is empty - call it a day
						 */
						break;
					}
				}
			});
		}
		/*
		 * shutdown service
		 */
		service.shutdown();
		
		try {
			service.awaitTermination(12, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		logger.info("number of reads that have actual cp set: " + actualCPReadCount + " which is " + (actualCPReadCount.doubleValue() / fastqRecordCount) * 100 + "%");
		logger.info("number of reads that DONT have actual cp set: " + noActualCPReadCount + " which is " + (noActualCPReadCount.doubleValue() / fastqRecordCount) * 100 + "%");
		logger.info("Number of fragments that matched exactly to the reference: " + noOfExactMatches);
	}
	
	
	private void createMutations() {
		
		/*
		 * Only call variants on contigs that contain more than 10 reads
		 * and on fragments that have more than twice the specified minimum fragment size
		 */
		contigFragmentMap.entrySet().stream()
			.filter(entry -> entry.getValue().stream().collect(Collectors.summingInt(Fragment2::getRecordCount)) >= 10)
			.forEach(entry -> {
				
				entry.getValue().stream()
					.filter(f -> f.isActualPositionSet()).filter( f ->  f.getRecordCount()  > minFragmentSize * 2)
					.forEach(f -> {
						
						String [] smithWatermanDiffs = f.getSmithWatermanDiffs();
						/*
						 * annotate mutations whose SW show many mutations
						 */
						int swScore = ClinVarUtil.getSmithWatermanScore(smithWatermanDiffs);
						
						boolean multipleMutations = (f.getLength() - swScore) >= multiMutationThreshold;
						
						List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(smithWatermanDiffs);
						if ( ! mutations.isEmpty()) {
							for (Pair<Integer, String> mutation : mutations) {
								int position = mutation.left().intValue();
								String mutString = mutation.right();
								int slashIndex = mutString.indexOf('/');
								String ref = mutString.substring(0, slashIndex);
								String alt = mutString.substring(slashIndex + 1);
								if (ref.equals(alt)) {
									logger.warn("ref is equal to alt: " + mutString);
									logger.warn("f: " + Arrays.stream(f.getSmithWatermanDiffs()).collect(Collectors.joining("\n")));
								}
								createMutation(f.getPosition(), position , ref, alt, entry.getKey().getId(), f.getId(), f.getRecordCount(), multipleMutations);	
								variants.computeIfAbsent(new ChrPositionName(f.getPosition().getChromosome(), f.getPosition().getStartPosition() + position, 
										f.getPosition().getStartPosition() + position + ref.length() - 1, ref), 
										func -> new HashMap<>(4)).computeIfAbsent(alt, func2 -> new ArrayList<>()).add(f);
							}
						}
					});
			});
	}
	
	private void createMutation(ChrPosition actualCP, int position, String ref, String alt, int contigId, int fragmentId, int fragmentRecordCount, boolean multipleMutations) {
		int startPos = actualCP.getStartPosition() + position;
		VcfRecord vcf = VcfUtils.createVcfRecord(ChrPointPosition.valueOf(actualCP.getChromosome(),  startPos), "."/*id*/, ref, alt);
		if (multipleMutations) {
			/*
			 * add to format filter field
			 */
			VcfUtils.addFormatFieldsToVcf(vcf, Arrays.asList("FT", "MM"), true, Constants.COMMA);
		}
		
		List<int[]> existingFragmentIds = vcfFragmentMap.get(vcf);
		if (null == existingFragmentIds) {
			existingFragmentIds = new ArrayList<>();
			vcfFragmentMap.put(vcf, existingFragmentIds);
 		} else {
 			if (multipleMutations) {
 				// need to update with annotation
 				vcfFragmentMap.put(vcf, existingFragmentIds);
 			}
 		}
		existingFragmentIds.add(new int[]{contigId, fragmentId, fragmentRecordCount});
	}
	
	
	
	private void loadTiledAlignerData() {
		logger.info("in loadTiledAlignerData");
		/*
		 * Loop through all our fragments (and reverse strand), split into 13mers and add to contigTiles 
		 */
		Set<String> fragmentTiles = new HashSet<>();
		for (String fragment : frags.keySet()) {
			String revComp = SequenceUtil.reverseComplement(fragment);
			int sLength = fragment.length();
			int noOfTiles = sLength / TILE_SIZE;
			
			for (int i = 0 ; i < noOfTiles ; i++) {
				fragmentTiles.add(fragment.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE));
				fragmentTiles.add(revComp.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE));
			}
		}
		logger.info("finished setting up set of fragment tiles, size: " + fragmentTiles.size());
		
		
		/*
		 * setup an execution service
		 * and a queue
		 */
		
		ExecutorService service = Executors.newFixedThreadPool(threadCount);
		Queue<String> queue = new ConcurrentLinkedQueue<>();
		
		/*
		 * setup threads to monitor queue and populate maps
		 */
		
		for (int i = 0 ; i < threadCount ; i++) {
			service.execute(() -> {
				int counter = 0;
				while (true) {
					
					String s = queue.poll();
					if (null != s) {
						if (s.equals(POISON_STRING)) {
							logger.info("Found poisoned string - exiting");
							break;
						}
						if (++counter % 1000000 == 0) {
							logger.info("hit: " + (counter / 1000000) + "M tiles...");
						}
											
						String tile = s.substring(0, TILE_SIZE);
						if (fragmentTiles.contains(tile)) {
							String countOrPosition = s.substring(s.indexOf(Constants.TAB) + 1);
							if (countOrPosition.charAt(0) == 'C') {
								frequentlyOccurringRefTiles.put(tile, tile);
							} else {
								
								TLongArrayList positionsList = new TLongArrayList();
								if (countOrPosition.indexOf(Constants.COMMA) == -1) {
									positionsList.add(Long.parseLong(countOrPosition));
								} else {
									String [] positions =  TabTokenizer.tokenize(countOrPosition, Constants.COMMA);
									for (String pos : positions) {
										positionsList.add(Long.parseLong(pos));
									}
									positionsList.sort();
								}
								
								// create TLongArrayList from 
								refTilesPositions.put(tile, positionsList);
							}
						}
					} else {
						try {
							Thread.sleep(5);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
		
		
		/*
		 * shutdown service
		 */
		service.shutdown();
		
		
		
		logger.info("Number of fragment tiles: " + fragmentTiles.size());
		
		logger.info("loading genome tiles alignment data");
		
		try (StringFileReader reader = new StringFileReader(new File(refTiledAlignmentFile))) {
			
//			TabbedHeader header = reader.getHeader();
//			List<String> headerList = new ArrayList<>();
//			for (String head : header) {
//				headerList.add(head);
//			}
			
			List<String> headerList = reader.getHeader();
			positionToActualLocation.loadMap(headerList);
			int i = 0;
			for (String rec : reader) {
				if (++i % 1000000 == 0) {
					logger.info("hit " + (i / 1000000) + "M records");
				}
				queue.add(rec);
			}
			/*
			 * kill off threads waiting on queue
			 */
			for (int z = 0 ; z < threadCount ; z++) {
				queue.add(POISON_STRING);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		try {
			service.awaitTermination(12, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		int refTilesCountSize =  frequentlyOccurringRefTiles.size();
		int refTilesPositionsSize =  refTilesPositions.size();
		logger.info("finished reading the genome tiles alignment data");
		logger.info("no of entries in refTilesCount: " + refTilesCountSize);
		logger.info("no of entries in refTilesPositions: " + refTilesPositionsSize);
		logger.info("Unique tiles in contigs: " + (fragmentTiles.size() - (refTilesCountSize + refTilesPositionsSize)));
	}
	
	private long[][] getTiledPositions(String f) {
		
		int noOfTiles = f.length() / TILE_SIZE;
		long[][] tilePositions = new long[noOfTiles][];
		
		for (int i = 0 ; i < noOfTiles ; i++) {
			String bt = f.substring(i * TILE_SIZE, (i + 1) * TILE_SIZE);
			
			if (frequentlyOccurringRefTiles.containsKey(bt)) {
				tilePositions[i] = new long[]{Long.MAX_VALUE};
			} else {
				TLongArrayList refPositions = refTilesPositions.get(bt);
				if (null == refPositions) {
					tilePositions[i] = new long[]{Long.MIN_VALUE};
				} else {
					tilePositions[i] = refPositions.toArray();
				}
			}
		}
		return tilePositions;
	}
	
	private void digestTiledData() throws IOException {
		
		logger.info("in digestTiledData with no of fragments: " + frags.size());
		
		/*
		 * setup an execution service - hard code thread count for now
		 * and a queue
		 */
		
		ExecutorService service = Executors.newFixedThreadPool(threadCount);
		Queue<Fragment2> queue = new ConcurrentLinkedQueue<>(frags.values());
		
		AtomicInteger positionFound = new AtomicInteger();
		AtomicInteger positionFoundReadCount = new AtomicInteger();
		AtomicInteger noPositionFound = new AtomicInteger();
		AtomicInteger noPositionFoundReadCount = new AtomicInteger();
		
		/*
		 * setup threads to monitor queue and create fragments
		 */
		for (int i = 0 ; i < threadCount ; i++) {
			service.execute(() -> {
				int counter = 0;
				while (true) {
					
					Fragment2 rf = queue.poll();
					if (null != rf) {
						if (++counter % 1000000 == 0) {
							logger.info("hit: " + (counter / 1000000) + "M frags...");
						}
						
						String fragment = rf.getSequence();
						long[][] tilePositions = getTiledPositions(fragment);
						/*
						 * And now reverse complement and run again
						 */
						long[][] rcTilePositions =  getTiledPositions(SequenceUtil.reverseComplement(fragment));
						
						/*
						 * Get the best (ie. positions with more than 2 tiles aligned to it) positions for each strand
						 */
						List<LongInt> resultsList = ClinVarUtil.getBestStartPositionLongInt(tilePositions, TILE_SIZE, maxIndelLength, tiledDiffThreshold, tileMatchThreshold);
						List<LongInt> rcResultsList = ClinVarUtil.getBestStartPositionLongInt(rcTilePositions, 
								TILE_SIZE, maxIndelLength, tiledDiffThreshold, tileMatchThreshold);
						
						ChrPosition bestTiledCp = null;
						
						/*
						 * get best tile counts - could be zero if no matches above our threshold of 2...
						 */
						int bestTileCount = resultsList.size() > 0 ? resultsList.get(0).getInt() : 0;
						int rcBestTileCount = rcResultsList.size() > 0 ? rcResultsList.get(0).getInt() : 0;
							
								/*
								 * Only perform sw on positions if the best tile position is not next to the contig position
								 */
						boolean forwardStrand = true;
						if (bestTileCount > rcBestTileCount + tiledDiffThreshold) {
							/*
							 * Only set bestTiledCp if we have a single key in the resultsMap, that only has a single long in its TLongArrayList value
							 */
							if (resultsList.size() == 1 ||  resultsList.get(1).getInt() <  bestTileCount) {
								bestTiledCp = positionToActualLocation.getChrPositionFromLongPosition(resultsList.get(0).getLong());
							}
						} else if (tiledDiffThreshold + bestTileCount < rcBestTileCount) {
							/*
							* Only set bestTiledCp if we have a single key in the resultsMap, that only has a single long in its TLongArrayList value
							*/
							if (rcResultsList.size() == 1 || rcResultsList.get(1).getInt() <  rcBestTileCount) {
								bestTiledCp = positionToActualLocation.getChrPositionFromLongPosition(rcResultsList.get(0).getLong());
								forwardStrand = false;
							}
						}
											
						if (null != bestTiledCp) {
							positionFound.incrementAndGet();
							positionFoundReadCount.addAndGet(rf.getRecordCount());							
							rf.setForwardStrand(forwardStrand);
							/*
							 * Set position - not the final one yet
							 */
							rf.setPosition(bestTiledCp, false);
							
						} else {
							
								if (bestTileCount != 0 || rcBestTileCount != 0) {
									logger.debug("Did NOT get a position!!!");
									logger.debug("bestTileCount: " + bestTileCount + ", rcBestTileCount: " + rcBestTileCount);
									logger.debug("frag: " + fragment);
								}
								noPositionFound.incrementAndGet();
								noPositionFoundReadCount.addAndGet(rf.getRecordCount());
							}
						
					} else {
						/*
						 * if rf is null, queue is empty - call it a day
						 */
						break;
					}
				}
			});
		}
		
		/*
		 * shutdown service
		 */
		service.shutdown();
		
		try {
			service.awaitTermination(12, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.info("positionFound count: " + positionFound.get() + " which contain " + positionFoundReadCount.get() + " reads,  noPositionFound count: " 
				+ noPositionFound.get() + ", which contain " + noPositionFoundReadCount.get() + " reads");
		logger.info("in digestTiledData - DONE");
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
			try (IndexedFastaSequenceFile refFile = new IndexedFastaSequenceFile(new File(refFileName), index);) {
				ReferenceSequence refSeq = refFile.getSequence(chr);
				ref = refSeq.getBases();
				referenceCache.put(chr, ref);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (cp.getStartPosition() <= 0 || cp.getEndPosition() > ref.length) { 
			logger.warn("ChrPosition goes beyond edge of contig: " + cp.toIGVString() + ", ref length: " + ref.length);
		} 
		
		byte [] refPortion = Arrays.copyOfRange(referenceCache.get(chr), cp.getStartPosition(), (cp.getEndPosition() > ref.length ? ref.length : cp.getEndPosition()));
		referenceSeq = new String(refPortion);
		
		return referenceSeq;
	}
	
	public static void main(String[] args) throws Exception {
		
		Q3Panel qp = new Q3Panel();
		int exitStatus = qp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		} else {
			System.err.println("Exit status: " + exitStatus);
		}
		
		System.exit(exitStatus);
	}
	
	protected int setup(String[] args) throws Exception {
		int returnStatus = 1;
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE2);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getFastqsR1().isEmpty()) {
			System.err.println(Messages.USAGE2);
		} else {
			// configure logging
			options.getLog().ifPresent((s) -> logFile = s);
			version = Q3Panel.class.getPackage().getImplementationVersion();
			if (null == version) {
				version = "local";
			}
			logger = QLoggerFactory.getLogger(Q3Panel.class, logFile, options.getLogLevel().orElse(null));
			exec = logger.logInitialExecutionStats("q3panel", version, args);
			
			// get list of file names
			fastqR1Files = options.getFastqsR1();
			fastqR2Files = options.getFastqsR2();
			/*
			 * There should be hte same number of r1 and r2 files, and this number should be greater than zero
			 */
			if (fastqR1Files.isEmpty() || fastqR1Files.size() != fastqR2Files.size()) {
				throw new Exception("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (String f : fastqR1Files) {
					if ( ! FileUtils.canFileBeRead(f)) {
						throw new Exception("INPUT_FILE_ERROR: "  +  f);
					}
				}
			}
			
			// set outputfile - if supplied, check that it can be written to
			options.getOutputFileName().ifPresent((s) -> outputFileNameBase = s);
			if (null != outputFileNameBase) {
				if ( ! FileUtils.canFileBeWrittenTo(outputFileNameBase)) {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
			} else {
				throw new Exception("OUTPUT_FILE_WRITE_ERROR");
			}
			
			runExtendedFB = options.runExtendedFB();
			
			options.getDbSnpFile().ifPresent((s) -> dbSNPFile = s);
			options.getCosmicFile().ifPresent((s) -> cosmicFile = s);
			options.getTiledRefFileName().ifPresent((s) -> refTiledAlignmentFile = s);
			options.getRefFileName().ifPresent((s) -> refFileName = s);
			options.getBedFile().ifPresent((s) -> bedFile = s);
			options.getGeneTranscriptsFile().ifPresent((s) -> geneTranscriptsFile = s);
			
			options.getMultiMutationThreshold().ifPresent((i) -> multiMutationThreshold = i.intValue());
			options.getMinFragmentSize().ifPresent((i) -> minFragmentSize = i.intValue());
			options.getMinReadPercentageSize().ifPresent((i) -> minReadPercentage = i.intValue());
			options.getAmpliconBoundary().ifPresent((i) -> contigBoundary = i.intValue());
			options.getMinBinSize().ifPresent((i) -> minBinSize = i.intValue());
			options.getBamFilterDepth().ifPresent((i) -> bamFilterDepth = i.intValue());
			options.getTrimFromEndsOfReads().ifPresent((i) -> trimFromEndsOfReads = i.intValue());
			options.getNumberOfThreads().ifPresent((i) -> threadCount = i.intValue());
			
			logger.info("runExtendedFB: " + runExtendedFB);
			logger.info("minBinSize is " + minBinSize);
			logger.info("minFragmentSize is " + minFragmentSize);
			logger.info("minReadPercentage is " + minReadPercentage);
			logger.info("contigBoundary is " + contigBoundary);
			logger.info("bamFilterDepth is " + bamFilterDepth);
			logger.info("trimFromEndsOfReads is " + trimFromEndsOfReads);
			logger.info("threadCount is " + threadCount);
						
			return engage();
		}
		return returnStatus;
	}
}
