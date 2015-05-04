package au.edu.qimr.clinvar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Serializer;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.Pair;
import org.w3c.dom.DOMImplementation;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;

import au.edu.qimr.clinvar.model.FastqProbeMatch;
import au.edu.qimr.clinvar.model.MatchScore;
import au.edu.qimr.clinvar.model.Probe;
import au.edu.qimr.clinvar.util.ClinVarUtil;
import au.edu.qimr.clinvar.util.FastqProbeMatchUtil;

public class Q3ClinVar {
	
private static QLogger logger;
	
	
	private static String[] fastqFiles;
	private static String[] cmdLineInclude;
	private static String[] cmdLineTags;
	private static String[] cmdLineTagsInt;
	private static String[] cmdLineTagsChar;
	private static DOMImplementation domImpl;
//	private static Document doc;
//	private static Element root ;
	private static ExecutorService exec;
	private static String version;
	private String logFile;
	private String xmlFile;
	private String outputFile;
	

	private final Map<Integer, Map<String, Probe>> probeLengthMapR1 = new HashMap<>();
	private final Map<Integer, Map<String, Probe>> probeLengthMapR2 = new HashMap<>();
	
	private final Set<Probe> probeSet = new HashSet<>();
	
//	private final Map<String, Probe> dlsoProbeMap = new HashMap<>();
//	private final Map<String, Probe> dlsoProbeMapSameLength = new HashMap<>();
	private final Map<Probe, AtomicInteger> probeDist = new HashMap<>();
//	private final Map<Pair<Probe, Probe>, AtomicInteger> multiProbeDist = new HashMap<>();
	
	// used to check for uniqueness
	private final Set<String> probeSequences = new HashSet<>();
//	private final Set<String> probeSequencesSameLength = new HashSet<>();
	
	private final Set<FastqProbeMatch> matches = new HashSet<>();
	
	
//	private final Map<Pair<FastqRecord, FastqRecord>, Probe> matchedBothReads = new HashMap<>();
//	private final Map<Pair<FastqRecord, FastqRecord>, Probe> matchedFirstRead = new HashMap<>();
//	private final Map<Pair<FastqRecord, FastqRecord>, Probe> matchedSecondRead = new HashMap<>();
//	private final Map<Pair<FastqRecord, FastqRecord>, Probe> matchedNoRead = new HashMap<>();
	
	private final static Map<Pair<FastqRecord, FastqRecord>, List<Probe>> multiMatchingReads = new HashMap<>();
	
	private int exitStatus;
	public static void listChildren(Element current, int depth) {
		System.out.println(current.getQualifiedName());
		Elements children = current.getChildElements();
		for (int i = 0; i < children.size(); i++) {
			listChildren(children.get(i), depth+1);
		}
		
	}
	
	protected int engage() throws Exception {
		
		try {
			Builder parser = new Builder();
			Document doc = parser.build(xmlFile);
			Element root = doc.getRootElement();
			
//			listChildren(probes, 1);
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
				int p1Start = -1;
				int p1End = -1;
				int p2Start = -1;
				int p2End = -1;
				
				Elements probeElements = probe.getChildElements();
			
				for (int j = 0 ; j < probeElements.size() ; j++) {
					Element probeSubElement = probeElements.get(j);
 					if ("DLSO_Sequence".equals(probeSubElement.getQualifiedName())) {
 						dlsoSeq = probeSubElement.getValue();
 					}
 					if ("ULSO_Sequence".equals(probeSubElement.getQualifiedName())) {
 						ulsoSeq = probeSubElement.getValue();
 					}
 					if ("dlsoRevCompSeq".equals(probeSubElement.getQualifiedName())) {
 						dlsoSeqRC = probeSubElement.getValue();
 					}
 					if ("ulsoRevCompSeq".equals(probeSubElement.getQualifiedName())) {
 						ulsoSeqRC = probeSubElement.getValue();
 					}
 					if ("primer1Start".equals(probeSubElement.getQualifiedName())) {
 						p1Start = Integer.parseInt(probeSubElement.getValue());
 					}
 					if ("primer1End".equals(probeSubElement.getQualifiedName())) {
 						p1End = Integer.parseInt(probeSubElement.getValue());
 					}
 					if ("primer2Start".equals(probeSubElement.getQualifiedName())) {
 						p2Start = Integer.parseInt(probeSubElement.getValue());
 					}
 					if ("primer2End".equals(probeSubElement.getQualifiedName())) {
 						p2End = Integer.parseInt(probeSubElement.getValue());
 					}
				}
				
				Probe p = new Probe(id, dlsoSeq, dlsoSeqRC, ulsoSeq, ulsoSeqRC, p1Start, p1End, p2Start, p2End);
				probeSet.add(p);
//				int primerLengthR1 = p.getDlsoPrimerLength();
//				int primerLengthR2 = p.getUlsoPrimerLength();
				int primerLengthR1 = dlsoSeqRC.length();
				int primerLengthR2 = ulsoSeq.length();
//				if (primerLength != ulsoSeq.length() 
//						|| primerLength != ulsoSeqRC.length()
//						|| primerLength != dlsoSeq.length()) {
//					
//					logger.warn("inconsistent primer lengths for probe: " + p.toString());
//				}
				
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
				
//				logger.info("found probe with id: " + id + " and dlso seq: " + dlsoSeq + ", and ulso seq: " + ulsoSeq);
//				dlsoProbeMap.put(dlsoSeqRC, p);
				
				probeSequences.add(dlsoSeq);
				probeSequences.add(dlsoSeqRC);
				probeSequences.add(ulsoSeq);
				probeSequences.add(ulsoSeqRC);
				
//				probeSequencesMap.put(ulsoSeq, dlsoSeq);
//				probeSequencesMap.put(ulsoSeqRC, dlsoSeqRC);
//				probeSequencesMap.put(dlsoSeq, ulsoSeq);
//				probeSequencesMap.put(dlsoSeqRC, ulsoSeqRC);
//				probeSequencesMap.put(dlsoSeq, ulsoSeqRC);
//				probeSequencesMap.put(dlsoSeqRC, ulsoSeq);
			}
			
			logger.info("Found " + i + " probes in xml doc. No of entries in seq set is: " + probeSequences.size() + " which should be equals to : " + (4 * i));
			
			
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
			
			int matchCount = 0;
			int mateMatchCount = 0;
			
			for (FastqProbeMatch fpm : matches) {
				
				final String read1 = fpm.getRead1().getReadString();
				final String read2 = fpm.getRead2().getReadString();
					
				boolean firstReadMatchFound = false;
				boolean secondReadMatchFound = false;
				// loop through the maps searching for a match
				for (Integer pl : primerLengthsR1) {
					String read =read1.substring(0, pl.intValue());
						
					Map<String, Probe> map = probeLengthMapR1.get(pl);
						
						if (map.containsKey(read)) {
							firstReadMatchFound = true;
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
								updateMap(p, probeDist);
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
								secondReadMatchFound = true;
								Probe p = map.get(read);
								
								// set read2 probe
								fpm.setRead2Probe(p, 0);
//								matchedSecondRead.put(new Pair<FastqRecord, FastqRecord>(rec, rec2), p);
//								matchedSecondRead.put(rec, rec2);
								// no need to look at other maps
								break;
							}
						}
					}
			}
					
					
			logger.info("no of records in fastq file: " + fastqCount + ", and no of records that started with a probe in our set: " + matchCount + " and no whose mate also matched: " + mateMatchCount);
			
			FastqProbeMatchUtil.getStats(matches);
			
			
			//TODO - PUT THIS BACK IN
			logger.info("Rescue reads using edit distances");
			findNonExactMatches();
			FastqProbeMatchUtil.getStats(matches);
			
			setupFragments();
			
//			writeOutput();
		} finally {
		}
		return exitStatus;
	}

	private void writeOutput() {
		Element amplicons = new Element("Amplicons");
		
		List<Probe> orderedProbes = new ArrayList<>(probeSet);
		Collections.sort(orderedProbes);
		
		for (Probe p : orderedProbes) {
			Element amplicon = new Element("Amplicon");
			amplicons.appendChild(amplicon);
			
			// attributes
			amplicon.addAttribute(new Attribute("probe_id", "" + p.getId()));
			amplicon.addAttribute(new Attribute("fragment_length", "" + p.getExpectedFragmentLength()));
			
			// fragments
			int perfectMatch = 0;
			int totalCount = 0;
			int fragmentExists = 0;
			Map<String, AtomicInteger> fragmentMap = new HashMap<>();
			for (FastqProbeMatch fpm : matches) {
				if (FastqProbeMatchUtil.doesFPMMatchProbe(fpm, p)) {
					totalCount++;
					if (FastqProbeMatchUtil.isProperlyMatched(fpm)) {
						perfectMatch++;
					}
					
					String frag = fpm.getFragment();
					if (null != frag) {
						fragmentExists++;
						updateMap(frag, fragmentMap);
					}
				}
			}
			
			Element fragments = new Element("Fragments");
			amplicon.appendChild(fragments);
			
			// attributes for the fragments element
			fragments.addAttribute(new Attribute("total_count", "" + totalCount));
			fragments.addAttribute(new Attribute("perfect_probe_match", "" + perfectMatch));
			fragments.addAttribute(new Attribute("fragment_count", "" + fragmentExists));
			
			for (Entry<String, AtomicInteger> entry : fragmentMap.entrySet()) {
				Element fragment = new Element("Fragment");
				fragments.appendChild(fragment);
				
				fragment.addAttribute(new Attribute("fragment_length", "" + entry.getKey().length()));
				fragment.addAttribute(new Attribute("record_count", "" + entry.getValue().get()));
				fragment.appendChild(entry.getKey());
			}
		}
		
		Document doc = new Document(amplicons);
		try (OutputStream os = new FileOutputStream(new File(outputFile));){
			 Serializer serializer = new Serializer(os, "ISO-8859-1");
		        serializer.setIndent(4);
		        serializer.setMaxLength(64);
		        serializer.write(doc);  
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void findNonExactMatches() {
		int maxSingleEditDistance = 2;
//			int maxDoubleEditDistance = 3;
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
				// just 1 is missing a probe - find out which one, and try and
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
				int editDistance = ClinVarUtil.getEditDistance(read,  existingProbe.getDlsoSeqRC());
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
						editDistance = ClinVarUtil.getEditDistance(read, primer);
						
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
		
		int noOfProbesWithLargeSecondaryBin = 0;
		
		Map<Probe, List<FastqProbeMatch>> probeDist = new HashMap<>();
		Map<Probe, Map<String, AtomicInteger>> probeFragments = new HashMap<>();
		Map<Probe, Map<MatchScore, AtomicInteger>> probeMatchMap = new HashMap<>();
		
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
				Map<MatchScore, AtomicInteger> matchMap = probeMatchMap.get(p);
				if (null == matchMap) {
					matchMap = new HashMap<>();
					probeMatchMap.put(p, matchMap);
				}
				updateMap(fpm.getScore(), matchMap);
			}
		}
		
		
		// logging and writing to file
		Element amplicons = new Element("Amplicons");
		amplicons.addAttribute(new Attribute("amplicon_file", xmlFile));
		amplicons.addAttribute(new Attribute("fastq_file_1", fastqFiles[0]));
		amplicons.addAttribute(new Attribute("fastq_file_2", fastqFiles[1]));
		
		List<Probe> sortedProbes = new ArrayList<>(probeDist.keySet());
		Collections.sort(sortedProbes);		// sorts on id of probe
		
		for (Probe p : sortedProbes) {
			
			Element amplicon = new Element("Amplicon");
			amplicons.appendChild(amplicon);
			
			// attributes
			amplicon.addAttribute(new Attribute("probe_id", "" + p.getId()));
			amplicon.addAttribute(new Attribute("fragment_length", "" + p.getExpectedFragmentLength()));
			
			List<FastqProbeMatch> fpms = probeDist.get(p);
			
			Element fragments = new Element("Fragments");
			if (null != fpms && ! fpms.isEmpty()) {
				amplicon.appendChild(fragments);
				
				int perfectMatch = 0;
				int fragmentExists = 0;
				for (FastqProbeMatch fpm : fpms) {
					if (FastqProbeMatchUtil.doesFPMMatchProbe(fpm, p)) {
						if (FastqProbeMatchUtil.isProperlyMatched(fpm)) {
							perfectMatch++;
						}
						
						String frag = fpm.getFragment();
						if (null != frag) {
							fragmentExists++;
						}
					}
				}
				// add some attributtes
				fragments.addAttribute(new Attribute("total_number_of_reads", "" + fpms.size()));
				fragments.addAttribute(new Attribute("reads_containing_fragments", "" + fragmentExists));
//				fragments.addAttribute(new Attribute("perfect_probe_match", "" + perfectMatch));
				Map<MatchScore, AtomicInteger> matchMap = probeMatchMap.get(p);
				if (null != matchMap) {
					StringBuilder sb = new StringBuilder();
					for (Entry<MatchScore, AtomicInteger> entry : matchMap.entrySet()) {
						sb.append(entry.getKey().getRead1EditDistance()).append("/").append(entry.getKey().getRead2EditDistance());
						sb.append(":").append(entry.getValue().get()).append(',');
					}
					fragments.addAttribute(new Attribute("probe_match_breakdown", sb.toString()));
				}
				
//				Map<Integer, List<Integer>> overlapEditDist = new HashMap<>();
//				logger.info("probe: " + p.getId() + ", no of hits: " + fpms.size());
//				for (FastqProbeMatch fpm : fpms) {
//					List<Integer> editDistances = overlapEditDist.get(fpm.getExpectedReadOverlapLength());
//					if (null == editDistances) {
//						editDistances = new ArrayList<>();
//						overlapEditDist.put(fpm.getExpectedReadOverlapLength(), editDistances);
//					}
//					editDistances.add(fpm.getOverlapBasicEditDistance());
//				}
////				logger.info("distribution breakdown:");
////				for (Entry<Integer, List<Integer>> entry : overlapEditDist.entrySet()) {
////					logger.info("overlap: " + entry.getKey() + ", edit distances: " + ClinVarUtil.breakdownEditDistanceDistribution(entry.getValue()));
////				}
			}
			
			// fragments next
			Map<String, AtomicInteger> frags = probeFragments.get(p);
			if (null != frags && ! frags.isEmpty()) {
				
				
				List<Integer> fragMatches = new ArrayList<>();
				for (Entry<String, AtomicInteger> entry : frags.entrySet()) {
					if (entry.getKey() != null) {
						Element fragment = new Element("Fragment");
						fragments.appendChild(fragment);
						fragment.addAttribute(new Attribute("fragment_length", "" + (entry.getKey().startsWith("+++") || entry.getKey().startsWith("---") ? entry.getKey().length() - 3 : entry.getKey().length())));
						fragment.addAttribute(new Attribute("record_count", "" + entry.getValue().get()));
						fragment.appendChild(entry.getKey());
//						logger.info("frag: " + entry.getKey() + ", count: " + entry.getValue().get());
						fragMatches.add(entry.getValue().get());
					}
				}
				if (fragMatches.size() > 1) {
					Collections.sort(fragMatches);
					Collections.reverse(fragMatches);
					
					fragments.addAttribute(new Attribute("fragment_breakdown", "" + ClinVarUtil.breakdownEditDistanceDistribution(fragMatches)));
					
					
					int total = 0;
					for (Integer i : fragMatches) {
						total += i;
					}
					int secondLargestCount = fragMatches.get(1);
					if (secondLargestCount > 1) {
						int secondLargestPercentage = (100 * secondLargestCount) / total;
						if (secondLargestPercentage > 10) {
							
							for (Entry<String, AtomicInteger> entry : frags.entrySet()) {
								if (entry.getKey() != null) {
									logger.info("frag: " + entry.getKey() + ", count: " + entry.getValue().get());
//									fragMatches.add(entry.getValue().get());
								}
							}
							
							noOfProbesWithLargeSecondaryBin++;
							logger.info("secondLargestPercentage is greater than 10%!!!: " + secondLargestPercentage);
							logger.info("fragMatches: " + ClinVarUtil.breakdownEditDistanceDistribution(fragMatches));
						}
					}
				}
			}
			
		}
		logger.info("no of probes with secondary bin contains > 10% of reads: " + noOfProbesWithLargeSecondaryBin);
		
		
		// write output
		Document doc = new Document(amplicons);
		try (OutputStream os = new FileOutputStream(new File(outputFile));){
			 Serializer serializer = new Serializer(os, "ISO-8859-1");
		        serializer.setIndent(4);
		        serializer.setMaxLength(64);
		        serializer.write(doc);  
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			version = Q3ClinVar.class.getPackage().getImplementationVersion();
			logger = QLoggerFactory.getLogger(Q3ClinVar.class, logFile, options.getLogLevel());
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
			
			xmlFile = options.getXml();
			
			
				return engage();
			}
		return returnStatus;
	}

}
