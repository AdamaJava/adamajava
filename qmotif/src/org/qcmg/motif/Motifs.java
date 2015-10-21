/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.sf.samtools.util.SequenceUtil;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;

public class Motifs {
	
	static QLogger logger = QLoggerFactory.getLogger(Motifs.class);
	List<String> motifs;
	int motifSize;
	boolean revComp;
	
	public Motifs(boolean revComp, String ...motifStrings) {
		if (null == motifStrings) throw new IllegalArgumentException("Null string array passed to Motifs constructor");
		
		this.revComp = revComp;
		populateMotifs(motifStrings);
	}
	
	private void populateMotifs(String ... motifStrings) {
		Set<String> motifsSet = new LinkedHashSet<>();
		
		for (String s : motifStrings) {
			if (StringUtils.isNullOrEmpty(s)) continue;		// only add actual strings to the collections
			
			logger.debug("adding motif: " + s);
			motifsSet.add(s);
			if (revComp) {
				// also add in the complement
				String comp = SequenceUtil.reverseComplement(s);
				logger.debug("adding motif: " + comp);
				motifsSet.add(comp);
			}
		}
		
		// just keep the unique entries, and they should stay in the order specified by the user at runtime
		motifs = new ArrayList<>(motifsSet);
		motifSize = motifs.size();
	}
	
	public List<String> getMotifs() {
		return motifs;
	}
	public int getMotifsSize() {
		return motifSize;
	}

}
