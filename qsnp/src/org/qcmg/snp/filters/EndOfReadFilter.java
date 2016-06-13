/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.filters;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;

public class EndOfReadFilter implements SamRecordFilter {
	
	private final int offset;
	private final int snpPosition;
	
	public EndOfReadFilter(int offset, int snpPosition) {
		this.offset = offset;
		this.snpPosition = snpPosition;
	}
	
	@Override
	public boolean filterOut(SAMRecord sam) {
		int readEnd = sam.getAlignmentEnd();
		return snpPosition > (readEnd - offset);
	}

	@Override
	public boolean filterOut(SAMRecord arg0, SAMRecord arg1) {
		// TODO Auto-generated method stub
		return false;
	}

}
