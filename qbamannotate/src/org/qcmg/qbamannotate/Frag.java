/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamannotate;

import org.qcmg.ma.MADirection;
import org.qcmg.ma.MARecord;

import net.sf.samtools.SAMRecord;

public class Frag extends AnnotatorType {

	@Override
	public boolean annotate(final SAMRecord record) throws Exception {
		return true;
	}

	@Override
	public boolean annotate(SAMRecord record, MARecord maRecord) throws Exception {
		int n = maRecord.getDefLine().getNumberMappings();
		setZMAttribute(record, n);
		return annotate(record);
	}

	public void resetCount() {
	}
	
	public String generateReport() throws Exception {
		return "";
	}

}
