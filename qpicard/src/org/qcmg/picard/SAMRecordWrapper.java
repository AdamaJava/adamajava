/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.util.Iterator;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

public final class SAMRecordWrapper {
    private final SAMRecord record;
    private final Iterator<SAMRecord> iterator;
    private final SAMFileReader reader;

    SAMRecordWrapper(final SAMRecord record, final Iterator<SAMRecord> iterator,
                     final SAMFileReader reader) {
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
    
    public SAMFileReader getReader() {
        return reader;
    }
}
