/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.motif.util.MotifConstants;
import org.qcmg.motif.util.MotifMode;
import org.qcmg.motif.util.MotifUtils;
import org.qcmg.motif.util.MotifsAndRegexes;
import org.qcmg.motif.util.RegionCounter;

import htsjdk.samtools.SAMRecord;

public class MotifCoverageAlgorithm implements Algorithm {
	
	final MotifMode mode;
	
	 Set<String> stageOneMotifs;
	 Set<String> stageTwoMotifs;

	final int windowSize;
	
	final Pattern stageOneRegex;
	final Pattern stageTwoRegex;
	
	public MotifCoverageAlgorithm(MotifsAndRegexes motifs) {
		this.mode = motifs.getMotifMode();
		
		if (mode.stageOneString()) {
			this.stageOneMotifs = new HashSet<>(motifs.getStageOneMotifs().getMotifs());
		}
		if (mode.stageTwoString()) {
			this.stageTwoMotifs = new HashSet<>(motifs.getStageTwoMotifs().getMotifs());
		}
		
		this.stageOneRegex = motifs.getStageOneRegexPattern();
		this.stageTwoRegex = motifs.getStageTwoRegexPattern();
		
		this.windowSize = motifs.getWindowSize();
	}

	@Override
	public String getName() {
		return "motif coverage";
	}

	@Override
	public boolean applyTo(final SAMRecord read, Map<ChrPosition, RegionCounter> regions) {
		if (null == read) {
			throw new IllegalArgumentException("Null SAMRecord passed to applyTo");
		}
		
		String readString = read.getReadString();
		
		if (stageOneSearch(readString)) {
			int readStart = read.getAlignmentStart();
			String readChr = read.getReferenceName();
			if (read.getReadUnmappedFlag()) {
				if (readStart == 0) {
					// set the start position to be 1 as we are dealing with 1-based chrPos objects
					readStart = 1;
				}
				if ("*".equals(readChr)) {
					readChr = MotifConstants.UNMAPPED;
				}
			}
			ChrPosition cp = new ChrPointPosition(readChr, readStart);
			RegionCounter rc = getCounterFromMap(regions, cp);
			
			// throw exception if we don't have a region for this read
			if (null == rc) {
				throw new IllegalArgumentException("No RegionCounter exists for region: " + cp.toIGVString());
			}
			
			// check that our region type can handle this type of read
			if ( ! rc.getType().acceptRead( ! read.getReadUnmappedFlag())) {
				return false;
			}
			
			rc.updateStage1Coverage();
			
			// get motifs
			String stm = getStageTwoMotifs(readString);
			if ( ! StringUtils.isNullOrEmpty(stm)) {
				
				rc.addMotif(stm, ! read.getReadNegativeStrandFlag(), ! read.getReadUnmappedFlag());
				rc.updateStage2Coverage();
				
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Finds and returns the identified stage two motifs in the given read string.
	 * The search method depends on the processing mode, which could either be
	 * string matching or regex matching.
	 *
	 * @param readString the string to be searched for stage two motifs or patterns
	 * @return a concatenated string of stage two motifs found, or null if no motifs are found
	 */
	String getStageTwoMotifs(String readString) {
		if ( ! StringUtils.isNullOrEmpty(readString)) {
			
			StringBuilder motifs = new StringBuilder();
			boolean firstAppend = true;
			if (mode.stageTwoString()) {
				for (String motif : stageTwoMotifs) {
					if (readString.contains(motif)) {
						if (!firstAppend) {
							motifs.append(MotifUtils.M_D);
						} else {
							firstAppend = false;
						}
						motifs.append(motif);
					}
				}
			} else {
				
				Matcher matcher = stageTwoRegex.matcher(readString);
				while (matcher.find()) {
					if (!firstAppend) {
						motifs.append(MotifUtils.M_D);
					} else {
						firstAppend = false;
					}
					motifs.append(matcher.group());
				}
				
			}
			return motifs.isEmpty() ? null : motifs.toString();
		} else {
			return null;
		}
	}
	
	RegionCounter getCounterFromMap(Map<ChrPosition, RegionCounter> regions, ChrPosition read) {
		if (null == regions) {
			throw new IllegalArgumentException("Null map passed to getCounterFromMap");
		}
		
		for (ChrPosition cp : regions.keySet()) {
			if (ChrPositionUtils.doChrPositionsOverlapPositionOnly(read, cp)) {
				return regions.get(cp);
			}
		}
		return null;
	}

/**
 * Searches for specified motifs or patterns in the provided string during the first stage of processing.
 * Performs a search based on whether the current processing mode uses string matching or regex.
 *
 * @param readString the string to be searched for motifs or patterns
 * @return true if a match is found according to the current processing mode, false otherwise
 */
	boolean stageOneSearch(String readString) {
		if (readString == null || readString.isEmpty()) {
			return false;
		}
		
		if (mode.stageOneString()) {
			for (String motif : stageOneMotifs) {
				if (readString.contains(motif)) {
					return true;
				}
			}
		} else {		// regex
			Matcher matcher = stageOneRegex.matcher(readString);
            return matcher.find();
		}
		return false;
	}
}
