/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.util.Iterator;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

public final class SAMRecordWrapper {
    private final SAMRecord record;
    private final Iterator<SAMRecord> iterator;
    private final SamReader reader;

    SAMRecordWrapper(final SAMRecord record, final Iterator<SAMRecord> iterator,
                     final SamReader reader) {
        this.record = record;
        this.iterator = iterator;
        this.reader = reader;
    }

    public SAMRecord getRecord() {
        return record;
    }

    Iterator<SAMRecord> getRecordIterator() {
        return iterator;
    }
    
    public SamReader getReader() {
        return reader;
    }
}
