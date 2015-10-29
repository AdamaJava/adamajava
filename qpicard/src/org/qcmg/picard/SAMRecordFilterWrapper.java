/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import htsjdk.samtools.SAMRecord;

public class SAMRecordFilterWrapper implements Comparable<SAMRecordFilterWrapper>{
	
	private final SAMRecord record;
	private final long position;
	private boolean passesFilter;
	
	public SAMRecordFilterWrapper(final SAMRecord record, final long position) {
		this.record = record;
		this.position = position;
	}
	
	public void setPassesFilter(boolean passesFilter) {
		this.passesFilter = passesFilter;
	}
	
	public SAMRecord getRecord() {
		return record;
	}
	public long getPosition() {
		return position;
	}
	public boolean getPassesFilter() {
		return passesFilter;
	}

	@Override
	public int compareTo(SAMRecordFilterWrapper o) {
		long diff = this.position - o.position;
		if (diff > 0) return 1;
		else if (diff == 0) return 0;	// should never happen...
		else return -1;
	}

}
