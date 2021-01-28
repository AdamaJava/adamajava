package org.qcmg.protocol.s3;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Provides {@code openConnection(url)} for s3:// urls
 * 
 * @author conradL
 *
 */
public class S3URLStreamHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new S3URLConnection(url);
    }

}
