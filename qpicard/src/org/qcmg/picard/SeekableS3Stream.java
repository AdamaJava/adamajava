package org.qcmg.picard;

import java.net.Proxy;
import java.net.URL;

import htsjdk.samtools.seekablestream.SeekableHTTPStream;

/*
 * We can entirely re-use SeekableHTTPStream for s3:// URLS because that class uses
 * URL.openConnection() everywhere to do its thing — explicitly in SeekableHTTPStream.read(...)
 * and indirectly in HttpUtils.getHeaderField(...) — so if we've registered our own s3-aware
 * URLStreamHandlerFactory then all will be well.
 */

public class SeekableS3Stream extends SeekableHTTPStream {

    public SeekableS3Stream(URL url) {
        this(url, null);
    }    
    
    public SeekableS3Stream(URL url, Proxy proxy) {
        super(url, proxy);
    }

}
