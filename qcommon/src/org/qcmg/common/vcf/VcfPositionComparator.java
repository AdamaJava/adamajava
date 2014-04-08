/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf;

import java.util.Comparator;

import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.model.VCFRecord;

public class VcfPositionComparator implements Comparator<VCFRecord> {
	
	private static final Comparator<String> CHR_COMP = new ReferenceNameComparator();

	@Override
	public int compare(VCFRecord vcf1, VCFRecord vcf2) {
		// currently just care about chr and position
		int diff = CHR_COMP.compare(vcf1.getChromosome(), vcf2.getChromosome());
//		int diff = vcf1.getChromosome().compareTo(vcf2.getChromosome());
		if (diff != 0) return diff;
		
		return vcf1.getPosition() - vcf2.getPosition();
	}

}
