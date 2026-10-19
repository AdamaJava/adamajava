package org.qcmg.qio.record;

import java.io.Closeable;
import java.io.File;
import java.util.List;

/** A file-backed record stream with separately accessible header lines. */
public interface RecordSource<T> extends Closeable, Iterable<T> {
    File getFile();

    List<String> getHeader();
}
