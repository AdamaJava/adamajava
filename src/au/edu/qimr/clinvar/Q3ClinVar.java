package au.edu.qimr.clinvar;

import java.io.File;
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
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.commons.lang3.StringUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.common.util.Pair;
import org.w3c.dom.DOMImplementation;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;

import au.edu.qimr.clinvar.model.FastqProbeMatch;
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
	private final Map<Pair<Probe, Probe>, AtomicInteger> multiProbeDist = new HashMap<>();
	
	// used to check for uniqueness
	private final Set<String> probeSequences = new HashSet<>();
	private final Set<String> probeSequencesSameLength = new HashSet<>();
	
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
				}
				
				Probe p = new Probe(id, dlsoSeq, dlsoSeqRC, ulsoSeq, ulsoSeqRC);
				probeSet.add(p);
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
							fpm.setRead1Probe(p);
							
							// now check to see if mate starts with the value in the map
							String mate = p.getUlsoSeq();
							if (read2.startsWith(mate)) {
								secondReadMatchFound = true;
								
								// set read2 probe
								fpm.setRead2Probe(p);
								mateMatchCount++;
								updateProbeDist(p, probeDist);
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
								fpm.setRead2Probe(p);
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
			
			logger.info("Rescue reads using edit distances");
			
			int maxSingleEditDistance = 2;
			int maxDoubleEditDistance = 3;
			for (FastqProbeMatch fpm : matches) {
				if (FastqProbeMatchUtil.neitherReadsHaveAMatch(fpm)) {
					// both reads missing probes
					String read1 = fpm.getRead1().getReadString();
					String read2 = fpm.getRead2().getReadString();
					
					for (Probe p : probeSet) {
						String dlsoRC = p.getDlsoSeqRC();
						String ulso = p.getUlsoSeq();
						
						
						int [] editDistances = ClinVarUtil.getDoubleEditDistance(read1, read2, dlsoRC, ulso, maxSingleEditDistance);
						if (editDistances[0] < maxSingleEditDistance 
								&& editDistances[1] < maxSingleEditDistance) {
							
							// it could be the case that we have already found a matching probe, so make sure this match is better!!!
							
							if (FastqProbeMatchUtil.neitherReadsHaveAMatch(fpm)) {
								fpm.setRead1Probe(p);
								fpm.setRead2Probe(p);
								fpm.setRead1EditDistance(editDistances[0]);
								fpm.setRead2EditDistance(editDistances[1]);
							} else {
								// already have some probes set...
								logger.warn("have to determine which match is best...");
							}
						}
						
						
					}
					
				} else if ( ! FastqProbeMatchUtil.bothReadsHaveAMatch(fpm)) {
					// just 1 is missing a probe
				}
			}
			
			FastqProbeMatchUtil.getStats(matches);
			
//			logger.info("matched both reads: " + matchedBothReads.size() + " (" + (100 * matchedBothReads.size() / fastqCount) + "%)");
//			logger.info("matched first reads: " + matchedFirstRead.size() + " (" + (100 * matchedFirstRead.size() / fastqCount) + "%)");
//			logger.info("matched second reads: " + matchedSecondRead.size() + " (" + (100 * matchedSecondRead.size() / fastqCount) + "%)");
//			logger.info("matched no reads: " + matchedNoRead.size() + " (" + (100 * matchedNoRead.size() / fastqCount) + "%)");
				
			
//			// rescue the reads that had no matches first
//			int editDistance = 3;
//			logger.info("Will try and rescue reads where neither pair matched a probe" );
//			rescueReads(matchedNoRead, true, true, matchedBothReads, editDistance, probeSet);
//			
//			logger.info("after rescuing reads that had no matches... with edit distance of " + editDistance);
//			logger.info("matched both reads: " + matchedBothReads.size() + " (" + (100 * matchedBothReads.size() / fastqCount) + "%)");
//			logger.info("matched first reads: " + matchedFirstRead.size() + " (" + (100 * matchedFirstRead.size() / fastqCount) + "%)");
//			logger.info("matched second reads: " + matchedSecondRead.size() + " (" + (100 * matchedSecondRead.size() / fastqCount) + "%)");
//			logger.info("matched no reads: " + matchedNoRead.size() + " (" + (100 * matchedNoRead.size() / fastqCount) + "%)");
//			logger.info("matched different probes: " + multiMatchingReads.size() + " (" + (100 * multiMatchingReads.size() / fastqCount) + "%)");
//			
//			// next, rescue the reads that had matches on the first read, but not the second
//			editDistance = 2;
//			logger.info("Will try and rescue reads where only the first read matched a probe" );
//			rescueReads(matchedFirstRead, false, true, matchedBothReads, editDistance, probeSet);
//			
//			logger.info("after rescuing reads that had matches on the first read, but not the second... with edit distance of " + editDistance);
//			logger.info("matched both reads: " + matchedBothReads.size() + " (" + (100 * matchedBothReads.size() / fastqCount) + "%)");
//			logger.info("matched first reads: " + matchedFirstRead.size() + " (" + (100 * matchedFirstRead.size() / fastqCount) + "%)");
//			logger.info("matched second reads: " + matchedSecondRead.size() + " (" + (100 * matchedSecondRead.size() / fastqCount) + "%)");
//			logger.info("matched no reads: " + matchedNoRead.size() + " (" + (100 * matchedNoRead.size() / fastqCount) + "%)");
//			logger.info("matched different probes: " + multiMatchingReads.size() + " (" + (100 * multiMatchingReads.size() / fastqCount) + "%)");
//			
//			
//			// finally, rescue the reads that had matches on the second read, but not the first
//			logger.info("Will try and rescue reads where only the second read matched a probe" );
//			rescueReads(matchedSecondRead, true, false, matchedBothReads, editDistance, probeSet);
//			
//			logger.info("after rescuing reads that had matches on the second read, but not the first... with edit distance of " + editDistance);
//			logger.info("matched both reads: " + matchedBothReads.size() + " (" + (100 * matchedBothReads.size() / fastqCount) + "%)");
//			logger.info("matched first reads: " + matchedFirstRead.size() + " (" + (100 * matchedFirstRead.size() / fastqCount) + "%)");
//			logger.info("matched second reads: " + matchedSecondRead.size() + " (" + (100 * matchedSecondRead.size() / fastqCount) + "%)");
//			logger.info("matched no reads: " + matchedNoRead.size() + " (" + (100 * matchedNoRead.size() / fastqCount) + "%)");
//			logger.info("matched different probes: " + multiMatchingReads.size() + " (" + (100 * multiMatchingReads.size() / fastqCount) + "%)");
			
			
		} finally {
		}
		return exitStatus;
	}
	
	
	public static final void rescueReads(Map<Pair<FastqRecord, FastqRecord>, Probe> map, boolean rescueFirst, boolean rescueSecond, Map<Pair<FastqRecord, FastqRecord>, Probe> rescuedReads, int allowedEditDistance, Set<Probe> probeSet) {
		Set<Pair<FastqRecord,FastqRecord>> toRemove = new HashSet<>();
		int [] noMatchEditDistanceDist = new int[100];	// should not be more than 50ish ( 2 x primer length)
		
		for (Entry<Pair<FastqRecord, FastqRecord>, Probe> entry : map.entrySet()) {
			String r1 = entry.getKey().getLeft().getReadString();
			String r2 = entry.getKey().getRight().getReadString();
			final Probe mateProbe = entry.getValue();
			
			Probe closestMatch = null;
			int lowestEditDistance = Integer.MAX_VALUE; 
			int noOfHitsWithinAllowedEditDistance = 0;
			for (Probe p : probeSet) {
				String dlsoRC = p.getDlsoSeqRC();
				String ulso = p.getUlsoSeq();
				
				int editDistance = 0;
				if (rescueFirst) {
					String r1SubString = r1.substring(0, dlsoRC.length());
					editDistance = StringUtils.getLevenshteinDistance(dlsoRC, r1SubString);
				}
				
				if (rescueSecond && editDistance <= allowedEditDistance) {
					String r2SubString = r2.substring(0, ulso.length());
					editDistance += StringUtils.getLevenshteinDistance(ulso, r2SubString);
				}
//				if (editDistance == 0) {
//					if (rescueFirst) {
//						logger.warn("Found reads with edit distance of 0, r1: " + r1 + ", primer: " + dlsoRC);
//					} else {
//						logger.warn("Found reads with edit distance of 0, r2: " + r2 + ", primer: " + ulso);
//					}
//				}
				
				if (editDistance < lowestEditDistance) {
					closestMatch = p;
					lowestEditDistance = editDistance;
				}
				
				if (editDistance <= allowedEditDistance) {
					noOfHitsWithinAllowedEditDistance++;
//					logger.info("got editDistance of 1 for each read");
				}
			}
//			if (noOfHitsWithinAllowedEditDistance > 1) {
//				logger.warn("Got more than 1 probe matching with edit distance of 2: " + noOfHitsWithinAllowedEditDistance);
//			}
			
			// if edit distance is low enough, rescue this read
			if (lowestEditDistance <= allowedEditDistance) {
				
				if (null != mateProbe && closestMatch.equals(mateProbe)) {
					rescuedReads.put(entry.getKey(), closestMatch);
				} else if (null != mateProbe) {
					// 2 different probes!!!
					List<Probe> list = new ArrayList<>();
					list.add(mateProbe);
					list.add(closestMatch);
					multiMatchingReads.put(entry.getKey(),list);
				} else {
					// mate probe is null so set
					rescuedReads.put(entry.getKey(), closestMatch);
				}
				toRemove.add(entry.getKey());
				
			}
			
//			logger.info("lowest edit distance: " + lowestEditDistance);
			if (lowestEditDistance < noMatchEditDistanceDist.length) {
				// don't want to update if we still have lowestEditDistance set to Integer.MAX_VALUE
				noMatchEditDistanceDist[lowestEditDistance]++;
			}
		}
		
		for (int k = 0 ; k < noMatchEditDistanceDist.length ; k++) {
			if (noMatchEditDistanceDist[k] > 0) {
				logger.info("editDistance: " + k + ", no of read pairs: " + noMatchEditDistanceDist[k]);
			}
		}
		
		// remove
		for (Pair<FastqRecord,FastqRecord> rec : toRemove) {
			map.remove(rec);
		}
		
	}
	
	
	public static final void updateProbeDist(Probe p, Map<Probe, AtomicInteger> map) {
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
