/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

public final class MultiSAMFileIterator implements Iterator<SAMRecord> {
	private SAMRecordWrapper nextWrapper;
	private SamReader currentFileReader;
	private final SAMRecordWrapperComparator comparator = new SAMRecordWrapperComparator();
	
	//OJH switched this from a HashSet to a List - don't think Set properties are being used, and is more expensive when adding
//	private final HashSet<SAMRecordWrapper> nextWrappers = new HashSet<>();
//	private final Queue<SAMRecordWrapper> nextWrappers = new PriorityQueue<>(comparator);
	private final List<SAMRecordWrapper> nextWrappers = new ArrayList<>();

	MultiSAMFileIterator(final Vector<SamReader> fileReaders) {
		for (final SamReader reader : fileReaders) {
			Iterator<SAMRecord> i = reader.iterator();
			if (i.hasNext()) {
				SAMRecord record = i.next();
				SAMRecordWrapper wrapper = new SAMRecordWrapper(record, i, reader);
				nextWrappers.add(wrapper);
			}
		}
		march();
	}

	@Override
	public boolean hasNext() {
		return null != nextWrapper;
	}

	@Override
	public SAMRecord next() {
		SAMRecord result = nextWrapper.getRecord();
		currentFileReader = nextWrapper.getReader();
		march();
		return result;
	}

	public SamReader getCurrentSAMFileReader() {
		return currentFileReader;
	}

	private void march() {
//		nextWrapper = nextWrappers.poll();
//		if (null != nextWrapper) {
//			step(nextWrapper);
//		}
		if (nextWrappers.isEmpty()) {
			nextWrapper = null;
		} else {
			nextWrappers.sort(comparator);
			nextWrapper = nextWrappers.remove(0);
			step(nextWrapper);
//			nextWrapper = Collections.min(nextWrappers, comparator);
//			step(nextWrapper);
		}
	}

	private void step(final SAMRecordWrapper wrapper) {
		Iterator<SAMRecord> iter = wrapper.getRecordIterator();
		SamReader reader = wrapper.getReader();
//		nextWrappers.remove(wrapper);
		if (iter.hasNext()) {
//			SAMRecord record = iter.next();
//			SAMRecordWrapper temp = new SAMRecordWrapper(record, iter, reader);
			nextWrappers.add(new SAMRecordWrapper(iter.next(), iter, reader));
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
