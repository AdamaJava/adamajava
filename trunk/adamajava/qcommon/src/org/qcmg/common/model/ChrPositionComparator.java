package org.qcmg.common.model;

import java.util.Comparator;

public class ChrPositionComparator implements Comparator<ChrPosition> {

	private static final ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	@Override
	public int compare(ChrPosition o1, ChrPosition o2) {
		int chromosomeDiff = COMPARATOR.compare(o1.getChromosome(), o2.getChromosome());
		if (chromosomeDiff != 0)
			return chromosomeDiff;
		
		int positionDiff = o1.getStartPosition() - o2.getStartPosition();
		if (positionDiff != 0)
			return  positionDiff;
		
		return  o1.getEndPosition() - o2.getEndPosition();
	}
	

}
