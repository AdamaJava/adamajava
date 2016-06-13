/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.common.model;

import java.util.Comparator;
import java.util.List;

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
	
	/**	
	 * Creates a ChrPosition comparator that is based on the chromosome name comparator supplied as an argument.
	 * Allows the the user to be flexible as to how ChrPosition objects are compared
	 * @param chrNameComp
	 * @return
	 */
	public static Comparator<ChrPosition> getComparator(Comparator<String> chrNameComp) {
		
		return Comparator.comparing(ChrPosition::getChromosome, chrNameComp)
				.thenComparingInt(ChrPosition::getStartPosition)
				.thenComparingInt(ChrPosition::getEndPosition);
	}
	
	
	/**
	 * This method is useful if you have a list of contigs whose order you want to preserve.
	 * eg. a sorted bam will in its  header have a list of contigs, and it is possible that you would like to sort chromosome (Strings) based on this order
	 * 
	 * If the list is empty of null, then then @link ReferenceNameComparator comparator will be returned.
	 * 
	 * @param chrNameComp
	 * @return
	 */
	public static Comparator<String> getChrNameComparator(List<String> list) {
		
		return	(null == list || list.isEmpty()) ? COMPARATOR : 
			new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return list.indexOf(o1) - list.indexOf(o2);
			}
		};
		
	}

}
