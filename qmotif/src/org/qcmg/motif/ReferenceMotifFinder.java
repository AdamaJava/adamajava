/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;


public class ReferenceMotifFinder {
	
	public static final String TELOMERE = "((TTA|TCA|TTC|GTA|TGA|TTG|TAA|ATA|CTA|TTT|TTAA)GGG){2}|(CCC(TAA|TGA|GAA|TAC|TCA|CAA|TTA|TAT|TAG|AAA|TTAA)){2}";
	
	//(CCC(TTA)|(AAA)|(TGA)|(AAT)|(AGT)|(AAG)|(TAC)|(TCG)|(TCA)|(CAA)|(TAT)|(ATG)|(TTAA)){2}|((TTA)|(TCA)|(TTC)|(GTA)|(TGA)|(TTG)|(TAA)|(ATA)|(CAT)|(TTT)|(TTAA)GGG){2}
	
//	public static final String TELOMERE_RS = "(CCC(TAA|TGA|GAA|TAC|TCA|CAA|TTA|TAT|TAG|AAA|TTAA)){2}";
//	public static final String TELOMERE = "(((TTA)|(TCA)|(TTC)|(GTA)|(TGA)|(TTG)|(TAA)|(ATA)|(CAT)|(TTT)|(TTAA))GGG){2}";
//	public static final String TELOMERE = "([ACT]{3}GGG){2}";
	public final Pattern TELOMERE_PATTERN;
//	public static final Pattern TELOMERE_PATTERN_RS = Pattern.compile(TELOMERE_RS);
	
	private final Motifs motifs;
	private final String refFile;
	byte[] referenceBases;
	int referenceBasesLength;
	QLogger logger;
	String currentChr;
	String currentRefSequence;
	Map<ChrPosition, String> positionsWithMotifs = new HashMap<>(); 
	Map<ChrPosition, String> positionsWithMotifsRegex = new HashMap<>(); 
	
	public Map<ChrPosition, String> getPositionsWithMotifs() {
		return positionsWithMotifsRegex;
//		return positionsWithMotifs;
	}

	private FastaSequenceFile sequenceFile;
	
	public ReferenceMotifFinder(Motifs motifs, String refFile, String regex) {
		this.motifs = motifs;
		this.refFile = refFile;
		logger = QLoggerFactory.getLogger(ReferenceMotifFinder.class);
		if (null != regex) {
			logger.info("Will use the following supplied regex: " + regex);
		}
		TELOMERE_PATTERN = regex == null ? Pattern.compile(TELOMERE) : Pattern.compile(regex);
	}
	
	void performMotifSearch() {
		
		List<String> motifStrings = motifs.getMotifs();
		
		while (true) {
			logger.info("about to load " + currentChr + " into a string"); 
			currentRefSequence = new String(referenceBases);
			logger.info("DONE loading " + currentChr + " into a string");
			
			Matcher matcher = TELOMERE_PATTERN.matcher(currentRefSequence);
			int count = 0;
			while (matcher.find()) {
				count++;
				ChrPosition cp = new ChrPosition(currentChr, matcher.start(), matcher.end());
				positionsWithMotifsRegex.put(cp, matcher.group());
			}
			logger.info("Found " + count + " matches for " + currentChr);
			
//			// and now the reverse strand
//			matcher = TELOMERE_PATTERN_RS.matcher(currentRefSequence);
//			count = 0;
//			while (matcher.find()) {
//				count++;
//				ChrPosition cp = new ChrPosition(currentChr, matcher.start(), matcher.end());
//				positionsWithMotifsRegex.put(cp, matcher.group());
//			}
//			logger.info("Found " + count + " RS matches for " + currentChr);
//			continue;
			
//			int currentIndexPosition = 0;
//			int motifIndex = 0;
//			int[][] motifPositionsInReference = new int[motifStrings.size()][];
			
//			for (String motif : motifStrings) {
//				
//				int j = 0;
//				int[] currentMotifPositions = motifPositionsInReference[motifIndex];
//				if (null == currentMotifPositions) {
//					currentMotifPositions = new int[1000];
//					motifPositionsInReference[motifIndex] = currentMotifPositions;
//				}
//				
//				motifIndex++;
//				
//				while (currentIndexPosition >= 0) {
//					currentIndexPosition = currentRefSequence.indexOf(motif, currentIndexPosition + 1 );
//					if (currentIndexPosition > -1) {
//						currentMotifPositions[j++] = currentIndexPosition;
//						ChrPosition cp = new ChrPosition(currentChr, currentIndexPosition);
//						String listOfMotifs = positionsWithMotifs.get(cp);
//						if (null == listOfMotifs) {
//							listOfMotifs = motif; 
//						} else {
//							listOfMotifs += ":" + motif;
//						}
//						positionsWithMotifs.put(cp, listOfMotifs);
//					}
//				}
//				
//				currentIndexPosition = 0;
//			}
			
//			// println out array
//			StringBuilder sb = new StringBuilder();
//			int m = 0;
//			for (int[] motifPositions : motifPositionsInReference) {
////				if (null == motifPositions) continue;
//				int positionCount = 0;
//				for (int mp : motifPositions) {
//					if (mp >0) {
//						positionCount++;
//						sb.append(mp).append(", ");
//					}
//				}
//				if (positionCount > 0) {
//					logger.info("motif " + ++m + ": " + positionCount + " occurrence(s) at the following positions: " 
//								+ sb.substring(0, sb.length() - 2).toString());
//				}
//				sb.setLength(0);
//			}
			// next chr
			loadNextReferenceSequence();
			if (null == referenceBases) break;
		}
		
		// now attempt to log out the entries in the map
		List<ChrPosition> positions = new ArrayList<>(positionsWithMotifs.keySet());
		Collections.sort(positions);
		
		for (ChrPosition cp : positions) {
			logger.info(cp.toIGVString() + ": " +  positionsWithMotifs.get(cp));
		}
		
		logger.info("No of positions in INDEXOF map: " + positionsWithMotifs.size());
		
		// now attempt to log out the entries in the map
		List<ChrPosition> positionsRegex = new ArrayList<>(positionsWithMotifsRegex.keySet());
		Collections.sort(positionsRegex);
		
		for (ChrPosition cp : positionsRegex) {
			logger.info(cp.toIGVString() + ": " +  positionsWithMotifsRegex.get(cp));
		}
		
		logger.info("No of positions in REGEX map: " + positionsWithMotifsRegex.size());
		
		
		logger.info("DONE!!!");
		
	}
	
	void loadNextReferenceSequence() {
		if (null == sequenceFile) {
			sequenceFile = new FastaSequenceFile(new File(refFile), true);
		}
		referenceBases = null;
		currentChr = null;
		referenceBasesLength = 0;
		ReferenceSequence refSeq = sequenceFile.nextSequence();
		
		// debugging code
//		while ( ! "chr1".equals(refSeq.getName()))
//			refSeq = sequenceFile.nextSequence();
//			 debugging code
		if (null == refSeq) {	// end of the line
			logger.info("No more chromosomes in reference file - shutting down");
			closeReferenceFile();
		} else {
			currentChr = refSeq.getName();
			referenceBases = refSeq.getBases();
			referenceBasesLength = refSeq.length();
			logger.info("Will process records from: " + currentChr + ", length: " + referenceBasesLength);
		}
	}
	
	void closeReferenceFile() {
		if (null != sequenceFile) sequenceFile.close();
	}

}
