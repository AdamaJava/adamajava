/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import htsjdk.samtools.SAMRecord;

import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.motif.util.MotifConstants;
import org.qcmg.motif.util.MotifMode;
import org.qcmg.motif.util.MotifUtils;
import org.qcmg.motif.util.MotifsAndRegexes;
import org.qcmg.motif.util.RegionCounter;

public class MotifCoverageAlgorithm implements Algorithm {
	
	final MotifMode mode;
	
	 List<String> stageOneMotifs;
	 int stageOneMotifsSize;
	 List<String> stageTwoMotifs;
	 int stageTwoMotifsSize;
	
	final int windowSize;
	
	final Pattern stageOneRegex;
	final Pattern stageTwoRegex;
	
	public MotifCoverageAlgorithm(MotifsAndRegexes motifs) {
		this.mode = motifs.getMotifMode();
		
		if (mode.stageOneString()) {
			this.stageOneMotifs = motifs.getStageOneMotifs().getMotifs();
			this.stageOneMotifsSize = stageOneMotifs.size();
		}
		if (mode.stageTwoString()) {
			this.stageTwoMotifs = motifs.getStageTwoMotifs().getMotifs();
			this.stageTwoMotifsSize = stageTwoMotifs.size();
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
		if (null == read) throw new IllegalArgumentException("Null SAMRecord passed to applyTo");
		
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
			if (null == rc) throw new IllegalArgumentException("No RegionCounter exists for region: " + cp.toIGVString());
			
			// check that our region type can handle this type of read
			if ( ! rc.getType().acceptRead( ! read.getReadUnmappedFlag())) return false;
			
			rc.updateStage1Coverage();
			
			// get motifs
//			getStageTwoMotifs(readString,  ! read.getReadNegativeStrandFlag(), ! read.getReadUnmappedFlag(), rc);
			String stm = getStageTwoMotifs(readString);
			if ( ! StringUtils.isNullOrEmpty(stm)) {
				
				rc.addMotif(stm, ! read.getReadNegativeStrandFlag(), ! read.getReadUnmappedFlag());
				rc.updateStage2Coverage();
				
			}
			return true;
		}
		return false;
	}
	
//	void getStageTwoMotifs(String readString, boolean forwardStrand, boolean mapped, RegionCounter rc) {
//		if ( ! StringUtils.isNullOrEmpty(readString)) {
//			
//			boolean updateCoverage = false;
//		
//			if (mode.stageTwoString()) {
//				
//				for (int i = 0 ; i < stageTwoMotifsSize ; i++) {
//					String motif = stageTwoMotifs.get(i);
//					int index = readString.indexOf(motif);
//					if (index >= 0) {
//						updateCoverage = true;
//						rc.addMotif(motif, forwardStrand, mapped);
//					}
//				}
//			} else {
//				
//				Matcher matcher = stageTwoRegex.matcher(readString);
//				while (matcher.find()) {
//					updateCoverage = true;
//					rc.addMotif(matcher.group(), forwardStrand, mapped);
//				}
//			}
//			
//			if (updateCoverage) {
//				rc.updateStage2Coverage();
//			}
//		}
//	}
	String getStageTwoMotifs(String readString) {
		if ( ! StringUtils.isNullOrEmpty(readString)) {
			
			StringBuilder motifs = new StringBuilder();
			if (mode.stageTwoString()) {
				
				for (int i = 0 ; i < stageTwoMotifsSize ; i++) {
					String motif = stageTwoMotifs.get(i);
					int index = readString.indexOf(motif);
					if (index >= 0) {
						
						if (motifs.length() > 0) {
							motifs.append(MotifUtils.M_D);
						}
						motifs.append(motif);
					}
				}
				
			} else {
				
				Matcher matcher = stageTwoRegex.matcher(readString);
				while (matcher.find()) {
					if (motifs.length() > 0) {
						motifs.append(MotifUtils.M_D);
					}
					motifs.append(matcher.group());
//					if (motifs.length() > 0) motifs += MotifUtils.M_D;
//					motifs +=matcher.group();
				}
				
			}
			return motifs.length() == 0 ? null : motifs.toString();
		} else {
			return null;
		}
	}
	
	RegionCounter getCounterFromMap(Map<ChrPosition, RegionCounter> regions, ChrPosition read) {
		if (null == regions) throw new IllegalArgumentException("Null map passed to getCounterFromMap");
		
		for (ChrPosition cp : regions.keySet()) {
			if (ChrPositionUtils.doChrPositionsOverlapPositionOnly(read, cp)) {
				return regions.get(cp);
			}
		}
		return null;
	}

	/**
	 * For the stage one pass, we don't care about what motif was found, its purely a yes/no, and it yes, we'll do a more thorough search in stage two
	 * 
	 * @param mode
	 * @param read
	 * @return
	 */
	boolean stageOneSearch(String readString) {
		if (StringUtils.isNullOrEmpty(readString)) return false;
		
		boolean result = false;
		
		if (mode.stageOneString()) {
			
			for (int i = 0 ; i < stageOneMotifsSize ; i++) {
				String motif = stageOneMotifs.get(i);
				int index = readString.indexOf(motif);
				if (index >= 0) {
					result = true;
					break;
				}
			}
			
		} else {		// regex
			Matcher matcher = stageOneRegex.matcher(readString);
			if (matcher.find()) {
				result = true;
			}
		}
		
		return result;
	}

	
}
