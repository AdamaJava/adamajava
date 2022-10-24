package org.qcmg.picard;

/*
 * The MIT License
 *
 * Copyright (c) 2013 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.SeekableByteChannel;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.seekablestream.SeekableFTPStream;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.seekablestream.SeekablePathStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.IOUtil;

/**
 * This implementation copied from
 * htsjdk.samtools.seekablestream.SeekableStreamFactory$DefaultSeekableStreamFactory,
 * adding functionality to detect and handle S3 URLs.
 *
 * @author conradL
 *
 */
public class S3AwareSeekableStreamFactory implements ISeekableStreamFactory {

    static final Logger logger = LoggerFactory.getLogger(S3AwareSeekableStreamFactory.class);

    @Override
    public SeekableStream getStreamFor(final URL url) throws IOException {
        return getStreamFor(url.toExternalForm());
    }

    @Override
    public SeekableStream getStreamFor(final String path) throws IOException {
        return getStreamFor(path, null);
    }

    @Override
    public SeekableStream getStreamFor(final String path, Function<SeekableByteChannel, SeekableByteChannel> wrapper)
            throws IOException {

        logger.debug("getStreamFor(\"{}\", ...)", path);

        if (path.startsWith("http:") || path.startsWith("https:")) {
            final URL url = new URL(path);
            return new SeekableHTTPStream(url);
        } else if (path.startsWith("s3:")) {
            final URL url = new URL(path);
            return new SeekableS3Stream(url);            
        } else if (path.startsWith("ftp:")) {
            return new SeekableFTPStream(new URL(path));
        } else if (path.startsWith("file:")) {
            return new SeekableFileStream(new File(new URL(path).getPath()));
        } else if (IOUtil.hasScheme(path)) {
            return new SeekablePathStream(IOUtil.getPath(path), wrapper);
        } else {
            return new SeekableFileStream(new File(path));
        }
    }

    @Override
    public SeekableStream getBufferedStream(SeekableStream stream) {
        return getBufferedStream(stream, SeekableBufferedStream.DEFAULT_BUFFER_SIZE);
    }

    @Override
    public SeekableStream getBufferedStream(SeekableStream stream, int bufferSize) {
        if (bufferSize == 0)
            return stream;
        else
            return new SeekableBufferedStream(stream, bufferSize);
    }

}
