/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.vcf;

import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;

 

public class VcfRecordFilterWrapper implements Comparable<VcfRecordFilterWrapper>{
	
	private VcfRecord record;
	private final long position;
	private boolean passesFilter;
		 
	
	public VcfRecordFilterWrapper(final VcfRecord record, final long position) { 	
		this.record = record; 	 	
		this.position = position;		
 	}	
	
	public void setPassesFilter(boolean passesFilter) {
		this.passesFilter = passesFilter;
	}	
	
	public void setVcfRecord(VcfRecord vcf){
		this.record = vcf; 
	}

 
	public VcfRecord getRecord() {
		return record;
	}
	public long getPosition() {
		return position;
	}
	public boolean getPassesFilter() {
		return passesFilter;
	}
	
	@Override
	public int compareTo(VcfRecordFilterWrapper o) {
		long diff = this.position - o.position;
		if (diff > 0) return 1;
		else if (diff == 0) return 0;	// same position but maybe differenct allel
		else return -1;
	}


}
