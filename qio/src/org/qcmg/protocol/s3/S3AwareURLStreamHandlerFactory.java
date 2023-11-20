package org.qcmg.protocol.s3;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegates s3:// URL handling to {@link S3URLStreamHandler} while file://,
 * ftp://, http://, https://, jar:// protocols handled by the default runtime
 * handlers.
 * 
 * @author conradL
 * 
 */
public class S3AwareURLStreamHandlerFactory implements URLStreamHandlerFactory {

    static final Logger logger = LoggerFactory.getLogger(S3AwareURLStreamHandlerFactory.class);

    public URLStreamHandler createURLStreamHandler(String protocol) {

        logger.debug("createURLStreamHandler(\"{}\")", protocol);

        if (protocol.equalsIgnoreCase("s3")) {
            return new S3URLStreamHandler();
        } else {
            // see https://stackoverflow.com/questions/8938260/url-seturlstreamhandlerfactory#9253634
            return null;
        }
    }
}
