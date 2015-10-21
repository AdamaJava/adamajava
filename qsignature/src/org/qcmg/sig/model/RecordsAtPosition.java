/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.model;

import java.util.List;

import net.sf.samtools.SAMRecord;

import org.qcmg.common.model.ChrPosition;

public class RecordsAtPosition {
	
	private final ChrPosition cp;
	private final List<SAMRecord> records;
	
	public RecordsAtPosition(ChrPosition cp, List<SAMRecord> records) {
		this.cp = cp;
		this.records = records;
	}

	public ChrPosition getCp() {
		return cp;
	}

	public List<SAMRecord> getRecords() {
		return records;
	}
}
