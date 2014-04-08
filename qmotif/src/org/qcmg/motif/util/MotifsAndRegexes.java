/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

import java.util.regex.Pattern;

import org.qcmg.motif.Motifs;

public class MotifsAndRegexes {

	private final Motifs stageOneMotifs;
	private final Motifs stageTwoMotifs;
	private final Pattern stageOneRegexPattern;
	private final Pattern stageTwoRegexPattern;
	private final int windowSize;
	private final MotifMode mode;
	
	public MotifsAndRegexes(Motifs stageOneMotifs, String stageOneRegex, Motifs stageTwoMotifs, String stageTwoRegex, int windowSize) {
		this.stageOneMotifs = stageOneMotifs;
		this.stageTwoMotifs = stageTwoMotifs;
		
		this.windowSize = windowSize > 0 ? windowSize : 10000 ;
		
		stageOneRegexPattern = null != stageOneRegex ? Pattern.compile(stageOneRegex) : null;
		stageTwoRegexPattern = null != stageTwoRegex ? Pattern.compile(stageTwoRegex) : null;
		
		if (null != this.stageOneMotifs) {
			if (null != this.stageTwoMotifs) {
				mode = MotifMode.STRING_STRING;
			} else if (null != stageTwoRegexPattern){
				mode = MotifMode.STRING_REGEX;
			} else {
				// uh oh - no stage two
				throw new IllegalArgumentException("No stage two motifs or regex supplied!:=");
			}
		} else if (null != stageOneRegexPattern) {
			if (null != this.stageTwoMotifs) {
				mode = MotifMode.REGEX_STRING;
			} else if (null != stageTwoRegexPattern){
				mode = MotifMode.REGEX_REGEX;
			} else {
				// uh oh - no stage two
				throw new IllegalArgumentException("No stage two motifs or regex supplied!:=");
			}
		} else {
			// uh oh - no stage one
			throw new IllegalArgumentException("No stage one motifs or regex supplied!:=");
		}
	}
	
	public MotifMode getMotifMode() {
		return mode;
	}

	public Motifs getStageOneMotifs() {
		return stageOneMotifs;
	}
	
	public Motifs getStageTwoMotifs() {
		return stageTwoMotifs;
	}
	
	public Pattern getStageOneRegexPattern() {
		return stageOneRegexPattern;
	}
	
	public Pattern getStageTwoRegexPattern() {
		return stageTwoRegexPattern;
	}

	public int getWindowSize() {
		return windowSize;
	}
}
