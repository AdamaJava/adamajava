/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import org.qcmg.common.string.StringUtils;

public class ChrPosBait implements ChrPosition , Comparable<ChrPosBait> {
	
	private ChrPointPosition cpp;
	private String bait;
	
	public ChrPosBait(String chr, int position) {
		this.cpp = ChrPointPosition.valueOf(chr, position);
	}
	public ChrPosBait(ChrPosition chrPos) {
		this.cpp = ChrPointPosition.valueOf(chrPos.getChromosome(), chrPos.getStartPosition());
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
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((bait == null) ? 0 : bait.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChrPosBait other = (ChrPosBait) obj;
		if (bait == null) {
			if (other.bait != null)
				return false;
		} else if (!bait.equals(other.bait))
			return false;
		return true;
	}
	@Override
	public int compareTo(ChrPosBait o) {
		return cpp.compareTo(o.cpp);
	}
	@Override
	public String getChromosome() {
		return cpp.getChromosome();
	}
	@Override
	public int getStartPosition() {
		return cpp.getStartPosition();
	}
	

}
