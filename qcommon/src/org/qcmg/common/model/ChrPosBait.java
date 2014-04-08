/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import org.qcmg.common.string.StringUtils;

public class ChrPosBait implements Comparable<ChrPosBait> {
	
	private static final ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private final String chr;
	private final int position;
	private String bait;
	
	public ChrPosBait(String chr, int position) {
		this.chr = chr;
		this.position = position;
	}
	public ChrPosBait(ChrPosition chrPos) {
		this.chr = chrPos.getChromosome();
		this.position = chrPos.getPosition();
	}

	public void updateBait(String bait) {
		if ( ! StringUtils.isNullOrEmpty(bait)) {
			if (StringUtils.isNullOrEmpty(this.bait)) {
				this.bait = bait;
			} else {
					this.bait += ";" + bait;
			}
		}
	}
	
	public String getBait() {
		return bait;
	}
	
	public String getChr() {
		return chr;
	}
	public int getPosition() {
		return position;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chr == null) ? 0 : chr.hashCode());
		result = prime * result + position;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChrPosBait other = (ChrPosBait) obj;
		if (chr == null) {
			if (other.chr != null)
				return false;
		} else if (!chr.equals(other.chr))
			return false;
		if (position != other.position)
			return false;
		return true;
	}
	
	@Override
	public int compareTo(ChrPosBait o) {
		int chromosomeDiff = COMPARATOR.compare(chr, o.chr);
		if (chromosomeDiff != 0)
			return chromosomeDiff;
		
		return position - o.position;
	}

}
