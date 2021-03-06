/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.filters;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

public class AdjacentIndelFilter implements SamRecordFilter {
	
	private final int snpPosition;
	
	public AdjacentIndelFilter(int snpPosition) {
		this.snpPosition = snpPosition;
	}
	
	@Override
	public boolean filterOut(final SAMRecord sam) {
		
		// if read does not have an indel in its cigar string, keep
		final Cigar cigar = sam.getCigar();
		if ( ! cigar.toString().contains("I") && ! cigar.toString().contains("D"))
			return false;
		
		int readStart = sam.getAlignmentStart();
		int offset = 0; 
		
		for (CigarElement ce : cigar.getCigarElements()) {
			int cigarLength = ce.getLength();
			
			
			if (CigarOperator.DELETION == ce.getOperator()) {
				if (snpPosition == (readStart + offset -1) 
					|| snpPosition == (readStart + offset + cigarLength))
					return true;
			} else if (CigarOperator.INSERTION == ce.getOperator()) {
				if (snpPosition == (readStart + offset) )
						return true;
			}
			
			if (isCigarElementAdvancable(ce))
				offset += cigarLength;
		}
		
		return false;
	}
	
	private boolean isCigarElementAdvancable(CigarElement ce) {
		return CigarOperator.M == ce.getOperator()
				|| CigarOperator.D == ce.getOperator()
				|| CigarOperator.EQ == ce.getOperator()
				|| CigarOperator.X == ce.getOperator();
	}

	@Override
	public boolean filterOut(SAMRecord arg0, SAMRecord arg1) {
		// TODO Auto-generated method stub
		return false;
	}

}
