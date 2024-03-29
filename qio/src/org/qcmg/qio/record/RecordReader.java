/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.qio.record;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.qcmg.common.util.FileUtils;


public abstract class RecordReader<T> implements Closeable, Iterable<T> {
    public static final int DEFAULT_BUFFER_SIZE = 65536;
    public static final String DEFAULT_HEADER_PREFIX = null; //no header line
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    protected final File file;
    //only allow create once
    protected final BufferedReader bin;
    protected T next;

    protected List<String> headerLines = new ArrayList<>();

    public RecordReader(final File file) throws IOException {
        this(file, DEFAULT_BUFFER_SIZE);
    }

    public RecordReader(final File file, int bufferSize) throws IOException {
        this(file, bufferSize, DEFAULT_HEADER_PREFIX, DEFAULT_CHARSET);
    }

    public RecordReader(final File file, CharSequence headerPrefix) throws IOException {
        this(file, DEFAULT_BUFFER_SIZE, headerPrefix, DEFAULT_CHARSET);
    }

    public RecordReader(final File file, int bufferSize, CharSequence headerPrefix, Charset charset) throws IOException {

        this.file = file;
        boolean isGzip = FileUtils.isInputGZip(file);
        InputStream inputStream = (isGzip) ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file);
        InputStreamReader streamReader = new InputStreamReader(inputStream, charset);
        bin = new BufferedReader(streamReader, bufferSize);

        String nextLine = readHeaderAndReturnFirstNonHeaderLine(headerPrefix);
        //get first record, set to null for empty file
        next = nextLine == null ? null : getRecord(nextLine);

    }

    /**
     * this method is overridable in subclass, eg illumina file have different header pattern
     *
     * @param headerPrefix
     * @return the first line just after header
     * @throws IOException
     */
    public String readHeaderAndReturnFirstNonHeaderLine(CharSequence headerPrefix) throws IOException {

        String nextLine = bin.readLine();

        //keep empty header and return first nonHeaderline
        if (headerPrefix == null) return nextLine;

        //reader header, hence file pointer to first line after header
        while (nextLine != null && nextLine.startsWith(headerPrefix + "")) {
            headerLines.add(nextLine);
            //reset current read line
            nextLine = bin.readLine();
        }

        return nextLine;
    }

    /**
     * This reader can maximum take Integer.MAX lines of file header. Please make other header if bigger than this.
     *
     * @return a list of header lines
     */
    public List<String> getHeader() {
        return headerLines;
    }

    @Override
    /**
     * Here, BufferedReader.close() calls InputStreamReader.close(), which API told us that it Closes the stream and releases any system resources associated with it.
     */ public void close() throws IOException {
        bin.close();
    }

    public File getFile() {
        return file;
    }

    @Override
    public Iterator<T> iterator() {

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return null != next;
            }

            @Override
            //return the stored record (next), even it is null
            public T next() {
                T rec = next;
                next = null; //in case exception happen, same line repeatedly

                try {
                    //get next record, it may read  multi lines
                    String line = bin.readLine();
                    if (line != null) {
                        next = getRecord(line);
                    }

                    return rec;
                } catch (IOException e) {
                    //here we only catch IO exception
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    //some record cross multi lines, eg id\nseq\n, this method may call bin.readLine() inside
    public abstract T getRecord(String line);


}
