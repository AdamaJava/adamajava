package org.qcmg.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;



public class IOStreamUtils {
    
    /**
     * Shamelessly ripped off from https://stackoverflow.com/a/41368866/6705037
     * 
     * @param input
     * @return
     * @throws IOException
     */
    public static InputStream decompressIfGzip(InputStream input) throws IOException {
        
        final PushbackInputStream pb = new PushbackInputStream(input, 2);

        int header = pb.read();
        if(header == -1) {
            return pb;
        }

        int b = pb.read();
        if(b == -1) {
            pb.unread(header);
            return pb;
        }

        pb.unread(new byte[]{(byte)header, (byte)b});

        header = (b << 8) | header;

        if(header == GZIPInputStream.GZIP_MAGIC) {
            return new GZIPInputStream(pb);
        } else {
            return pb;
        }
    }

}
