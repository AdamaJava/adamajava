/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

public enum RegionType {
	
	INCLUDES (true, true),
	EXCLUDES (false, false),
	UNMAPPED (true, false),
	GENOMIC (true, false);
	
	private boolean keepCounts;
	private boolean includeMapped;
	
	private RegionType(boolean keepCount, boolean includeMapped) {
		this.keepCounts = keepCount;
		this.includeMapped = includeMapped;
	}
	
	public boolean keepCounts() {
		return keepCounts;
	}
	public boolean includeMapped() {
		return includeMapped;
	}
	
	/**
	 * A read is accepted if:
	 *  keepCounds is true
	 *  and
	 *  if the read is unmapped (mapped == false)
	 *  or 
	 *  if the read is mapped and includeMapped is true
	 *  
	 * @param mapped
	 * @return
	 */
	public boolean acceptRead(boolean mapped) {
		return keepCounts && allowMappedStatus(mapped);
//		return keepCounts && ! ( mapped && ! includeMapped);
	}
	
	public boolean allowMappedStatus(boolean mapped) {
		// by default, all unmapped reads are allowed
		
		if ( ! mapped) return true;
		// if we are here, read is mapped, so return includeMapped
		return includeMapped;
	}

}
