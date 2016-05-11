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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeader.FormattedRecord;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils.VcfInfoType;
import org.qcmg.qmule.SmithWatermanGotoh;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileReader;
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
	
	private static final Comparator<ChrPosition> COMPARATOR = new ChrPositionComparator();
	
	private static QLogger logger;
	private static final int TILE_SIZE = 13;
	private static final char [] AT = new char[]{'A','T'};
	
	private QExec exec;
	
	private static String[] fastqFiles;
	private static String version;
	private String logFile;
	private String bedFile;
	private String geneTranscriptsFile;
	private String outputFileNameBase;
	private String refTiledAlignmentFile;
	private String refFileName;
	private final int tiledDiffThreshold = 1;
	private final int swDiffThreshold = 2;
	private final int tileMatchThreshold = 2;
	private final int maxIndelLength = 5;
	private int fastqRecordCount;
	
	private int dbSnpTotalCount;
	private int cosmicTotalCount;
	
	private int mutationDbSnpRecordCount;
	private int mutationCosmicRecordCount;
	
	private  int minBinSize = 10;
	private  int minFragmentSize = 3;
	private   int minReadPercentage = 2;
	private  int ampliconBoundary = 10;
	private  int multiMutationThreshold = 10;
	final AtomicInteger outputMutations = new AtomicInteger();

	
	private final Map<String, List<StringBuilder>> reads = new HashMap<>();
	private final Map<IntPair, AtomicInteger> readLengthDistribution = new HashMap<>();
	private final Set<String> frequentlyOccurringRefTiles = new HashSet<>();
	private final Map<String, TLongArrayList> refTilesPositions = new HashMap<>();
	
	private final PositionChrPositionMap positionToActualLocation = new PositionChrPositionMap();
	
	private final Map<String, byte[]> referenceCache = new HashMap<>();
	
	private final Map<String, RawFragment> rawFragments = new HashMap<>();
	private final Map<String, Fragment> frags = new HashMap<>();
	
	private final Map<VcfRecord, List<int[]>> vcfFragmentMap = new HashMap<>();
	private final List<VcfRecord> filteredMutations = new ArrayList<>();
	
	private Map<Amplicon, List<Fragment>> ampliconFragmentMap = new HashMap<>();
	
	private final Map<Amplicon, List<Amplicon>> bedToAmpliconMap = new HashMap<>();
	private final Map<String, Transcript> transcripts = new HashMap<>();
	
	private final VcfHeader dbSnpHeaderDetails = new VcfHeader();
	private final VcfHeader cosmicHeaderDetails = new VcfHeader();
	private VcfHeader header = new VcfHeader();
	
	private int exitStatus;
	private int rawFragmentId = 1;
	private int fragmentId = 1;
	private String cosmicFile;
	private String dbSNPFile;
	
	protected int engage() throws Exception {
			
		fastqRecordCount = readFastqs();
		
		createRawFragments();
		
		loadTiledAlignerData();
		
		/*
		 * Clear up some no longer used resources
		 */
		rawFragments.clear();
		reads.clear();
		
		getActualLocationForFrags();
		
		createAmplicons();
		
		mapBedToAmplicons();
		
		loadTranscripts();
		
		createMutations();
		logger.info("no of mutations created: " + vcfFragmentMap.size());
		filterAndAnnotateMutations();
		logger.info("no of filtered mutations remaining: " + filteredMutations.size());
		writeMutationsToFile();
		writeXml();
		writeBam();
		
		logger.info("no of fastq records: " +fastqRecordCount);
		return exitStatus;
	}

	/**
	 * Only runs when we have both a bed and a transcripts file present
	 * @throws IOException 
	 */
	private void loadTranscripts() throws IOException {
		
		if (StringUtils.isNoneBlank(bedFile, geneTranscriptsFile)) {
			
			Map<String, List<Amplicon>> uniqueChrs = bedToAmpliconMap.keySet().stream()
														.collect(Collectors.groupingBy(a -> a.getInitialFragmentPosition().getChromosome()));
					
			logger.info("Number of unique chromosomes in bed file: " + uniqueChrs.size());
			
			try (TabbedFileReader reader = new TabbedFileReader(new File(geneTranscriptsFile))) {
				String currentTranscriptId = null;
				for (TabbedRecord rec : reader) {
					String contig = rec.getData().substring(0, rec.getData().indexOf(Constants.TAB));
					if (uniqueChrs.containsKey(contig)) {
						String [] params= TabTokenizer.tokenize(rec.getData());
						String [] column8 = params[8].split(";"); 
//						String id = column8[1].replace("transcript_id", "").replace("\"", "").trim();
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
						
//						String id = optionalId.isPresent() ? optionalId.get().replace("transcript_id", "").replace("\"", "").trim() : null;
//						String exonNumber = optionalExonNumber.isPresent() ? optionalExonNumber.get().replace("exon_number", "").replace("\"", "").trim() : null;
						
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
//							String gene = optionalGene.isPresent() ? optionalGene.get().replace("gene_name", "").replace("\"", "").trim() : null;
							
							t = new Transcript(id, params[1], contig, gene);
							transcripts.put(id, t);
						}
						switch (params[2]) {
						case "exon":
							t.addExon(new ChrPositionName(params[0], Integer.parseInt(params[3]), Integer.parseInt(params[4]), exonNumber));
							break;
						case "CDS":
							t.addCDS(new ChrPositionName(params[0], Integer.parseInt(params[3]), Integer.parseInt(params[4]), exonNumber));
							break;
						default:
							logger.debug("Ignoring " + params[2]);
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
				List<Amplicon> amps = uniqueChrs.get(t.getContig());
				if (amps.stream().noneMatch(a -> a.getInitialFragmentPosition().getStartPosition() >= t.getStart() && a.getInitialFragmentPosition().getEndPosition() <= t.getEnd())) {
					transcriptsToRemove.add(t.getId());
				}
			});
			transcriptsToRemove.stream().forEach(id -> transcripts.remove(id));
			logger.info("Number of Transcripts covering bed regions: " + transcripts.size());
		}
	}

	private void createRawFragments() {
		int perfectOverlap = 0;
		int nonPerfectOverlap = 0;
		int smallOverlap = 0;
		int onlyATs = 0;
		int theSame = 0, different = 0;
		
		TIntIntHashMap overlapDistribution = new TIntIntHashMap();
		TIntIntHashMap nonOverlapDistribution = new TIntIntHashMap();
		TIntIntHashMap overlapLengthDistribution = new TIntIntHashMap();
		
		for (Entry<String, List<StringBuilder>> entry : reads.entrySet()) {
//			for (Entry<String, AtomicInteger> entry : reads.entrySet()) {
			int readCount = entry.getValue().size();
//			int readCount = entry.getValue().intValue();
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
					f = new RawFragment(rawFragmentId++, r1, entry.getValue(), r1.length());
					rawFragments.put(r1, f);
				} else {
					// update count and overlap
//					f.addCount(readCount);
					f.addOverlap( r1.length(), entry.getValue());
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
						f = new RawFragment(rawFragmentId++, fragment, entry.getValue(), overlapLength);
						rawFragments.put(fragment, f);
					} else {
						// update count and overlap
//						f.addCount(readCount);
						f.addOverlap(overlapLength, entry.getValue());
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
		q3pElement.addAttribute(new Attribute("bed", bedFile));
		q3pElement.addAttribute(new Attribute("bed_amplicon_count", ""+bedToAmpliconMap.size()));
		q3pElement.addAttribute(new Attribute("vcf", outputFileNameBase + ".vcf"));
		q3pElement.addAttribute(new Attribute("vcf_variant_count", ""+outputMutations.get()));
		q3pElement.addAttribute(new Attribute("fastq_1", fastqFiles[0]));
		q3pElement.addAttribute(new Attribute("fastq_2", fastqFiles[1]));
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
		readLengthDistribution.entrySet().stream()
			.sorted((e1, e2) -> {return e1.getKey().compareTo(e2.getKey());})
			.forEach(entry -> {
				Element rl = new Element("ReadLength");
				rl.addAttribute(new Attribute("r1", ""+entry.getKey().getInt1()));
				rl.addAttribute(new Attribute("r2", ""+entry.getKey().getInt2()));
				rl.addAttribute(new Attribute("count", ""+entry.getValue().intValue()));
				rls.appendChild(rl);
			});
	
		q3pElement.appendChild(rls);
		
		// logging and writing to file
		Element amplicons = new Element("Amplicons");
		q3pElement.appendChild(amplicons);
		ampliconFragmentMap.entrySet().stream()
			.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
			.forEach(entry -> {
				createAmpliconElement(amplicons, entry.getKey(), entry.getValue());
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
				amplicon.addAttribute(new Attribute("number_of_overlapping_fragment_amplicons", "" + entry.getValue().size()));
				
				String idLIst = entry.getValue().stream()
					.sorted((a1, a2) -> COMPARATOR.compare(a1.getPosition(),a2.getPosition()))
					.map(a -> a.getId() + "")
					.collect(Collectors.joining(","));
				
				amplicon.addAttribute(new Attribute("overlapping_fragment_amplicon_ids", "" + idLIst));
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
		transcripts.values().stream()
			.sorted()
			.forEach(v -> {
			
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
		try (OutputStream os = new FileOutputStream(new File(outputFileNameBase + ".q3p.xml"));){
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
					ChrPosition cp = new ChrRangePosition(params[0], Integer.parseInt(params[1]), Integer.parseInt(params[2]));
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
								&& a.getPosition().getStartPosition() < (bed.getPosition().getStartPosition() + 10)
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
				.sorted((f1,f2) -> COMPARATOR.compare(f1.getActualPosition(), f2.getActualPosition()))
				.forEach(f -> {
					int fragId = f.getId();
					recordCount.addAndGet(f.getRecordCount());
					String [] swDiffs = f.getSmithWatermanDiffs();
					
					/*
					 * Get amplicon id that this fragment belongs to
					 */
					AtomicInteger ampliconId = new AtomicInteger();
					ampliconFragmentMap.entrySet().stream()
						.filter(entry -> ChrPositionUtils.isChrPositionContained(entry.getKey().getPosition(), f.getActualPosition()))
						.forEach(entry -> {
							if (entry.getValue().stream()
								.anyMatch(fr -> fr.getId() == fragId)) {
								ampliconId.set(entry.getKey().getId());
							}
								
						});
					/*
					 * assign different mapping quality based on record count within fragment
					 */
					int mappingQuality = f.getRecordCount() > minFragmentSize ? 60 : 20;
					/*
					 * Deal with exact matches first
					 */
					if (ClinVarUtil.isSequenceExactMatch(swDiffs,  f.getSequence())) {
						Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(f.getLength());
						ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, ampliconId.get(), swDiffs[0], f, 0, mappingQuality);
					} else if (ClinVarUtil.doesSWContainSnp(swDiffs) && ! ClinVarUtil.doesSWContainIndel(swDiffs)) {
						/*
						 * Snps only here
						 */
						if (swDiffs[1].length() == f.getLength()) {
							// just snps and same length
							Cigar cigar = ClinVarUtil.getCigarForMatchMisMatchOnly(f.getLength());
//							ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, 1, fragId,  f.getRecordCount(), swDiffs[0], fragCp.getChromosome(), fragCp.getPosition(), 0, fragSeq, mappingQuality);
							ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, ampliconId.get(), swDiffs[0], f, 0, mappingQuality);
						} else {
							logger.info("snps only but length differs, swDiffs[1].length(): " + swDiffs[1].length() + ", f.getLength(): " + f.getLength());
						}
						
					} else if ( ! ClinVarUtil.doesSWContainSnp(swDiffs) && ClinVarUtil.doesSWContainIndel(swDiffs)) {
						// indels only
						String ref = swDiffs[0].replaceAll("-","");
						Cigar cigar = ClinVarUtil.getCigarForIndels(ref,   f.getSequence(), swDiffs,  f.getActualPosition());
						if (cigar.toString().equals("51M53I-52M7I159M")) {
							logger.info("cigar: " + cigar.toString());
							logger.info("ref: " + ref);
							logger.info("f.getActualPosition(): " + f.getActualPosition().toIGVString());
							logger.info("f.getSequence(): " + f.getSequence());
							logger.info("cigar: " + cigar.toString());
							for (String s : swDiffs) {
								logger.info("s: " + s);
							}
						}
//						ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, 1, fragId,  f.getRecordCount(), ref, fragCp.getChromosome(), fragCp.getPosition(), 0, fragSeq, mappingQuality);
						ClinVarUtil.addSAMRecordToWriter(header, writer, cigar, ampliconId.get(), ref, f, 0, mappingQuality);
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
	
	private void filterAndAnnotateMutations() throws IOException {
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
			filteredMutations.add(entry.getKey());
		});
		
		
		Map<ChrPosition, List<VcfRecord>> cpVcfMap = filteredMutations.stream()
				.collect(Collectors.groupingBy(VcfRecord::getChrPosition));
				
		/*
		 * COSMIC
		 */
		if ( ! StringUtils.isBlank(cosmicFile)) {
			AtomicInteger cosmicCount = new AtomicInteger();
			Map<ChrPosition, List<String[]>> cosmicData = new HashMap<>();
			try (Stream<String> lines = Files.lines(Paths.get(cosmicFile), Charset.defaultCharset())) {
				cosmicData = lines.filter(s -> ! s.startsWith("Gene name"))
					.map(s -> TabTokenizer.tokenize(s))
					.filter(p -> StringUtils.isNotEmpty(p[23]) && cosmicCount.incrementAndGet() > -1)
					.filter(p -> cpVcfMap.containsKey(ChrPositionUtils.createCPFromCosmic(p[23])))
					.collect(Collectors.groupingBy(p -> ChrPositionUtils.createCPFromCosmic(p[23])));
				
				cosmicTotalCount = cosmicCount.get();
				mutationCosmicRecordCount = cosmicData.values().stream()
					.collect(Collectors.summingInt(list -> list.size()));
				
				logger.info("no of entries in cosmicData: " + cosmicData.size() + " with total cosmic record count: " + mutationCosmicRecordCount);
				
				cosmicData.entrySet().stream()
					.forEach(entry-> {
						logger.info("k: " + entry.getKey().toIGVString());
						String genes = entry.getValue().stream()
							.map(p -> p[0])
							.distinct()
							.collect(Collectors.joining(";"));
						String cosmicId = entry.getValue().stream()
								.map(p -> p[16])
								.distinct()
								.collect(Collectors.joining(";"));
						String cds = entry.getValue().stream()
								.map(p -> p[17])
								.distinct()
								.collect(Collectors.joining(","));
						logger.info("genes: " + genes + ", cosmicId: " + cosmicId + ", cds: " + cds);
						
						/*
						 * update vcf records with cosmic id and annotations
						 */
						List<VcfRecord> vcfs = cpVcfMap.get(entry.getKey());
						vcfs.stream()
							.forEach(v -> {
								v.appendId(cosmicId);
	//							v.setId(v.getId().equals(".") ? cosmicId : v.getId() + ";" + cosmicId);
								v.appendInfo("CCDS=" + cds + ";CG=" + genes);
							});
					});
				cosmicHeaderDetails.parseHeaderLine("##INFO=<ID=CG,Number=.,Type=String,Description=\"Gene as reported by Cosmic\">");
				cosmicHeaderDetails.parseHeaderLine("##INFO=<ID=CCDS,Number=.,Type=String,Description=\"CDS as reported by Cosmic\">");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/*
		 * dbSNP
		 */
		if ( ! StringUtils.isBlank(dbSNPFile)) {
			try (VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
				//add dbSNP version into header		
				List<Record>  metas = reader.getHeader().getMetaRecords(); 
				for (Record re: metas) {
					if (re.getData().startsWith(VcfHeaderUtils.STANDARD_DBSNP_LINE)) {
						dbSnpHeaderDetails.parseHeaderLine(String.format("##INFO=<ID=%s,Number=0,Type=%s,Description=\"%s\",Source=%s,Version=%s>",
										VcfHeaderUtils.INFO_DB, VcfInfoType.Flag.name(),
										VcfHeaderUtils.DESCRITPION_INFO_DB, dbSNPFile, new VcfHeaderUtils.SplitMetaRecord(re).getValue()  ));
					}
				}
						
				Map<String, FormattedRecord> snpInfoHeader = reader.getHeader().getInfoRecords();
				if (snpInfoHeader.get(VcfHeaderUtils.INFO_CAF) != null ) {
					dbSnpHeaderDetails.parseHeaderLine( String.format("##INFO=<ID=%s,Number=.,Type=String,Description=\"%s\">", VcfHeaderUtils.INFO_VAF, VcfHeaderUtils.DESCRITPION_INFO_VAF  )	);
				}
				if (snpInfoHeader.get(VcfHeaderUtils.INFO_VLD) != null ) {
					dbSnpHeaderDetails.addInfo(snpInfoHeader.get(VcfHeaderUtils.INFO_VLD));		 						 
				}
				dbSnpHeaderDetails.parseHeaderLine("##INFO=<ID=DB_CDS,Number=.,Type=String,Description=\"Reference and Alt alleles as reported by dbSNP\">");
				
				
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
					if (null == mutations || mutations.isEmpty()) {
						continue;
					}
					
					mutationDbSnpRecordCount++;
					
					for (VcfRecord mut : mutations) {
//						logger.info("Found dbsnp record: " + dbSNPVcf.toString() + " for mutation: " + mut.toString());
						mut.appendId(dbSNPVcf.getId());
						mut.appendInfo(VcfHeaderUtils.INFO_DB);
						mut.appendInfo("DB_CDS=" + dbSNPVcf.getRef() + ">" + dbSNPVcf.getAlt());
//						logger.info("updated mut: " + mut.toString());
					}
					
								
	//				//each dbSNP check twice, since indel alleles followed by one reference base, eg. chr1 100 . TT T ...
	//				final int end =  dbSNPVcf.getRef().length() +  (start - 1); 
	//				List<VcfRecord> inputVcfs = positionRecordMap.get(chrPos);
	//				if (null != inputVcfs && inputVcfs.size() != 0){
	//					for(VcfRecord re: inputVcfs)
	//						if(annotateDBsnp(re, dbSNPVcf ))
	//							dbSnpNo ++;						
	//				}
	//				
	//				//check RSPOS for MNV only
	////				String VC = dbSNPVcf.getInfoRecord().getField("VC");
	//				if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=MNV", false))
	//					continue;
	//				
	//				//if RSPOS different to column 2
	//				String rspos = dbSNPVcf.getInfoRecord().getField("RSPOS");
	//				if( !StringUtils.isNullOrEmpty(rspos)){ 		
	//					start = Integer.parseInt(rspos);
	//					if(start == chrPos.getPosition() || start > chrPos.getEndPosition()) continue; 
	//					
	//					chrPos = new ChrPosition(chr, start, end );	
	//					inputVcfs = positionRecordMap.get(chrPos);
	//					if (null != inputVcfs && inputVcfs.size() != 0) {
	//						for(VcfRecord re: inputVcfs)
	//							if(annotateDBsnp(re, dbSNPVcf ))
	//								dbSnpNo ++;					
	//					}
	//				}
				}
			}
		}
		
		
		/*
		 * distance from end of read
		 */
		
		filteredMutations.stream()
			.forEach(vcf -> {
				final AtomicInteger middleOfReadCount = new AtomicInteger();
				final AtomicInteger endOfReadCount = new AtomicInteger();
				vcfFragmentMap.get(vcf).stream()
					.forEach(array -> {
						ampliconFragmentMap.get(new Amplicon(array[0], null)).stream()
							.filter(f -> f.getId() == array[1])
							.forEach(f -> {
								if (vcf.getPosition() - f.getActualPosition().getStartPosition() <= 5
										|| f.getActualPosition().getEndPosition() - vcf.getChrPosition().getEndPosition() <= 5) {
									endOfReadCount.addAndGet(f.getRecordCount());
								} else {
									middleOfReadCount.addAndGet(f.getRecordCount());
								}
							});
						
					});
				logger.info("vcf: " + vcf.toString() + ", middleOfReadCount: " + middleOfReadCount.get() + ", endOfReadCount: " + endOfReadCount.get());
				
				/*
				 * Add filter to vcf if we only have end of reads - may want to be a little more strict than this
				 */
				if (middleOfReadCount.get() == 0 && endOfReadCount.get() > 0) {
					VcfUtils.updateFilter(vcf, SnpUtils.END_OF_READ + endOfReadCount.get());
				}
			
			});
		
		
		// ends of reads check
//		void checkForEndsOfReads(QSnpRecord rec, Accumulator normal, Accumulator tumour, char ref ) {
//			
//			final PileupElementLite pel = Classification.GERMLINE != rec.getClassification() ? (null != tumour? tumour.getLargestVariant(ref) : null) : 
//				(null != normal ? normal.getLargestVariant(ref) : null);
//			
//			if (null != pel && pel.getEndOfReadCount() > 0) {
//				
//				if (pel.getMiddleOfReadCount() >= 5 && pel.isFoundOnBothStrandsMiddleOfRead()) {
//					// all good
//				} else {
//					VcfUtils.updateFilter(rec.getVcfRecord(), SnpUtils.END_OF_READ + pel.getEndOfReadCount());
//				}
//			}
//		}
	}
	
	private void writeMutationsToFile() throws IOException {
		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileNameBase + ".vcf"))) {
			
			/*
			 * Setup the VcfHeader
			 */
			final DateFormat df = new SimpleDateFormat("yyyyMMdd");
			String date = df.format(Calendar.getInstance().getTime());
			
			header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + date);		
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + QExec.createUUid());		
			header.addQPGLine(1, "Q3Panel", exec.getToolVersion().getValue(), exec.getCommandLine().getValue(), date);
			header.addFormatLine("FB", ".","String","Breakdown of Amplicon ids, Fragment ids and read counts supporting this mutation, along with total counts of amplicon, fragment, and reads for all reads at that location in the following format: AmpliconId,FragmentId,readCount;[...] / Sum of amplicons at this position,sum of fragments at this position,sum of read counts at this position");
			header.addFilterLine(SnpUtils.END_OF_READ, "Indicates that the mutation occurred within the first or last 5 bp of all the reads contributing to the mutation");
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT);
			header = VcfHeaderUtils.mergeHeaders(header, dbSnpHeaderDetails, true);
			header = VcfHeaderUtils.mergeHeaders(header, cosmicHeaderDetails, true);
			
			Iterator<Record> iter = header.iterator();
			while (iter.hasNext()) {
				writer.addHeader(iter.next().toString() );
			}
			filteredMutations.stream()
				.sorted((e1, e2) -> {return e1.compareTo(e2);})
				.forEach(entry -> {
					try {
						writer.add(entry);
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
		int actualCPReadCount = 0;
		int noActualCPReadCount = 0;
		
		for (Fragment f : frags.values()) {
			
			/*
			 * Get best tiled location - and get ref bases based on this +/- 100
			 * and then sw
			 */
			ChrPosition bestTiledCP = f.getBestTiledLocation();
			ChrPosition bufferedCP = new ChrRangePosition(bestTiledCP.getChromosome(),  Math.max(1,bestTiledCP.getStartPosition() - 100), bestTiledCP.getStartPosition() + 100 + f.getLength());
//			ChrPosition bufferedCP = new ChrRangePosition(bestTiledCP.getChromosome(), bestTiledCP.getStartPosition() - 100, bestTiledCP.getStartPosition() + 100 + f.getLength());
			String bufferedReference = getRefFromChrPos(bufferedCP);
			
			String [] swDiffs = ClinVarUtil.getSwDiffs(bufferedReference, f.getSequence(), true);
			f.setSWDiffs(swDiffs);
			
			
			if (f.getId() == 6282 || f.getId() == 2765) {
				System.out.println("frag id: " + f.getId());
				for (String s : swDiffs) {
					System.out.println("s: " + s);
				}
			}
			
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
						
						/*
						 * annotate mutations whose SW show many mutations
						 */
						int swScore = ClinVarUtil.getSmithWatermanScore(smithWatermanDiffs);
						
						boolean multipleMutations = (f.getLength() - swScore) >= multiMutationThreshold;
						
//						if (entry.getKey().getId() == 8 || f.getId() == 6282) {
//							logger.info("entry.getKey().getId(): " +entry.getKey().getId() + ", frag: " + f.getId() + ", length: " + f.getLength()  + ", score: " + swScore + " multipleMutations: " + multipleMutations);
//						}
//						
						List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(smithWatermanDiffs);
						if ( ! mutations.isEmpty()) {
							for (Pair<Integer, String> mutation : mutations) {
								int position = mutation.getLeft().intValue();
								String mutString = mutation.getRight();
								int slashIndex = mutString.indexOf('/');
								String ref = mutString.substring(0, slashIndex);
								String alt = mutString.substring(slashIndex + 1);
								if (ref.equals(alt)) {
									logger.warn("ref is equal to alt: " + mutString);
									logger.warn("f: " + Arrays.stream(f.getSmithWatermanDiffs()).collect(Collectors.joining("\n")));
								}
								createMutation(f.getActualPosition(), position , ref, alt, entry.getKey().getId(), f.getId(), f.getRecordCount(), multipleMutations);
							}
						}
					});
			});
	}
	
	private void createMutation(ChrPosition actualCP, int position, String ref, String alt, int ampliconId, int fragmentId, int fragmentRecordCount, boolean multipleMutations) {
		int startPos = actualCP.getStartPosition() + position;
//		int endPos =  startPos + ref.length() -1 ;
		VcfRecord vcf = VcfUtils.createVcfRecord(ChrPointPosition.valueOf(actualCP.getChromosome(),  startPos), "."/*id*/, ref, alt);
		if (multipleMutations) {
			vcf.addFilter("MM");
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
		existingFragmentIds.add(new int[]{ampliconId, fragmentId, fragmentRecordCount});
	}
	
	
	
	private void setActualCP(ChrPosition bufferedCP, int offset, Fragment f, int referenceLength) {
		final int startPosition =  bufferedCP.getStartPosition() + offset + 1;	// we are 1 based
		// location needs to reflect reference bases consumed rather sequence length
		ChrPosition actualCP = new ChrRangePosition(bufferedCP.getChromosome(), startPosition, startPosition + referenceLength -1);
		f.setActualPosition(actualCP);
	}

	private int readFastqs() {
		int fastqCount = 0;
		/*
		 *  read in a fastq file and lets see if we get some matches
		 *  Keep stats on read lengths
		 */
		
		int sameReadLength = 0;
		
		try (FastqReader reader1 = new FastqReader(new File(fastqFiles[0]));
				FastqReader reader2 = new FastqReader(new File(fastqFiles[1]));) {
			
			for (FastqRecord rec : reader1) {
				FastqRecord rec2 = reader2.next();
				
				StringBuilder readHeader = new StringBuilder(rec.getReadHeader());
//				readHeader.append(Constants.DOUBLE_HASH);
//				readHeader.append(rec2.getReadHeader());
				readHeader.trimToSize();
				
				/*
				 * Put entries into map - don't collapse at the moment as we could save some processing if we have multiple identical reads
				 */
				String r1Andr2 = rec.getReadString() + ":" + rec2.getReadString();
				
				List<StringBuilder> readHeaderList = reads.get(r1Andr2);
				if (null == readHeaderList) {
					readHeaderList = new ArrayList<>(2);
					reads.put(r1Andr2, readHeaderList);
				}
				readHeaderList.add(readHeader);
				
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
			.sorted((entry1, entry2) -> {return COMPARATOR.compare(entry1.getValue().getBestTiledLocation(), entry2.getValue().getBestTiledLocation());})
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
		
//		int bestLocationSet = 0, bestLocationNotSet = 0;
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
					List<StringBuilder> rfHeaders = rf.getReadHeaders();
					String forwardStrandFragment = forwardStrand ? fragment : SequenceUtil.reverseComplement(fragment);
//					int currentCount =  rf.getCount();
					positionFoundReadCount += rfHeaders.size();
					
					Fragment f = frags.get(forwardStrandFragment);
					if (null == f) {
						f = new Fragment(fragmentId++, forwardStrandFragment, forwardStrand ? rfHeaders : Collections.emptyList(), forwardStrand ? Collections.emptyList() : rfHeaders, bestTiledCp, rf.getOverlapDistribution());
						frags.put(forwardStrandFragment, f);
					} else {
						// update count
						if (forwardStrand) {
							// check that we don't already have a fs count set!!
							if (f.getFsCount() != 0) {
								logger.warn("already have fs count for this fragment!!!");
							}
							f.setForwardStrandCount(rfHeaders);
						} else {
							if (f.getRsCount() != 0) {
								logger.warn("already have rs count for this fragment!!!");
							}
							f.setReverseStrandCount(rfHeaders);
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
			}
		logger.info("positionFound count: " + positionFound + " which contain " + positionFoundReadCount + " reads,  noPositionFound count: " + noPositionFound + ", which contain "+ noPositionFoundReadCount + " reads");
//		logger.info("bestLocationSet count: " + bestLocationSet + ", not set count: " + bestLocationNotSet);
	}
	
	
	private Map<ChrPosition, String[]> getSWScores(TLongArrayList positionsList, final String binSequence ) throws IOException {
		final Map<ChrPosition, String[]> positionSWDiffMap = new HashMap<>(positionsList.size() * 2);
		final int buffer = 300;
		positionsList.forEach(new TLongProcedure() {
			@Override
			public boolean execute(long position) {
				ChrPosition cp = positionToActualLocation.getChrPositionFromLongPosition(position);
				ChrPosition refCp =  new ChrRangePosition(cp.getChromosome(), Math.max(1, cp.getStartPosition() - buffer), cp.getStartPosition() + binSequence.length() + buffer);
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
		if (cp.getStartPosition() <= 0 || cp.getEndPosition() > ref.length) {
			logger.warn("ChrPosition goes beyond edge of contig: " + cp.toIGVString() + ", ref length: " + ref.length);
		}
		byte [] refPortion = Arrays.copyOfRange(referenceCache.get(chr), cp.getStartPosition(), (cp.getEndPosition() > ref.length ? ref.length : cp.getEndPosition()));
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
			options.getLog().ifPresent((s) -> logFile = s);
			version = Q3ClinVar2.class.getPackage().getImplementationVersion();
			if (null == version) {
				version = "local";
			}
			logger = QLoggerFactory.getLogger(Q3ClinVar2.class, logFile, options.getLogLevel().orElse(null));
			exec = logger.logInitialExecutionStats("q3clinvar", version, args);
			
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
			options.getOutputFileName().ifPresent((s) -> outputFileNameBase = s);
			if (null != outputFileNameBase) {
				if ( ! FileUtils.canFileBeWrittenTo(outputFileNameBase)) {
					throw new Exception("OUTPUT_FILE_WRITE_ERROR");
				}
			} else {
				throw new Exception("OUTPUT_FILE_WRITE_ERROR");
			}
			
			options.getDbSnpFile().ifPresent((s) -> dbSNPFile = s);
			options.getCosmicFile().ifPresent((s) -> cosmicFile = s);
			options.getTiledRefFileName().ifPresent((s) -> refTiledAlignmentFile = s);
			options.getRefFileName().ifPresent((s) -> refFileName = s);
			options.getBedFile().ifPresent((s) -> bedFile = s);
			options.getGeneTranscriptsFile().ifPresent((s) -> geneTranscriptsFile = s);
			
			options.getMultiMutationThreshold().ifPresent((i) -> multiMutationThreshold = i.intValue());
			options.getMinFragmentSize().ifPresent((i) -> minFragmentSize = i.intValue());
			options.getMinReadPercentageSize().ifPresent((i) -> minReadPercentage = i.intValue());
			options.getAmpliconBoundary().ifPresent((i) -> ampliconBoundary = i.intValue());
			options.getMinBinSize().ifPresent((i) -> minBinSize = i.intValue());
			
			logger.info("minBinSize is " + minBinSize);
			logger.info("minFragmentSize is " + minFragmentSize);
			logger.info("minReadPercentage is " + minReadPercentage);
			logger.info("ampliconBoundary is " + ampliconBoundary);
			
			
			return engage();
		}
		return returnStatus;
	}

}
