/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.vcf;

import java.io.File;
import java.io.IOException;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.qio.record.RecordReader;



/**
 * Contains an `InputStream` so remember to call close() or use in try-with-resources
 */
public final class new1 extends RecordReader<VcfRecord> {
	private static final String HEADER_PREFIX = Constants.HASH_STRING;
   
//    private final InputStream inputStream;
    private VcfHeader header;
    
    public new1(final String file) throws IOException {
    	this(new File(file));
    }

    public new1(final File file) throws IOException {
    	super(file, HEADER_PREFIX);
       
    	header = new VcfHeader(getHeader());
        
//        boolean isGzip = FileUtils.isInputGZip( file);        
//        try(InputStream stream = (isGzip) ? new GZIPInputStream(new FileInputStream(file), 65536) : new FileInputStream(file);) {
//	        	BufferedReader in = new BufferedReader(new InputStreamReader(stream));        
//	        	header = VCFSerializer.readHeader(in);        	
//        }  
//        
//        //get a new stream rather than a closed one
//        inputStream = (isGzip) ? new GZIPInputStream(new FileInputStream(file), 65536) : new FileInputStream(file);         
    }
   
//    /**
//     * Constructor initialized by an InputStream. {@code markSupported()} for the supplied InputStream must be
//     * {@code true}.
//     * <p> 
//     * The value of {@code file} for the returned instance is {@code null}.
//     * 
//     * @param instrm
//     * @param headerMaxBytes
//     * @throws IOException
//     */
//    public VcfFileReader(final InputStream instrm, Integer headerMaxBytes) throws IOException {
//        
//        if ( ! instrm.markSupported()) {
//            throw new IOException("The supplied InputStream does not support marking");
//        }
//        instrm.mark(headerMaxBytes);
//        BufferedInputStream bis = new BufferedInputStream(instrm);
//        BufferedReader br = new BufferedReader(new InputStreamReader(bis));
//        header = VCFSerializer.readHeader(br);
//        instrm.reset();
//        inputStream = instrm;
//        file = null;
//    }
//
//    /**
//     * Uses a default value of 1048576 (1MB) for {@code headerMaxBytes}
//     * 
//     * @param instrm
//     * @throws IOException
//     */
//    public VcfFileReader(final InputStream instrm) throws IOException {
//        this(instrm, 1048576);
//    }
//    
//    /**
//     * returns a stream over the supplied file
//     * If the file is zipped, appropriate action is taken
//     * @param f
//     * @return
//     * @throws IOException 
//     * @throws FileNotFoundException 
//     */
//    public static VcfFileReader createStream(File f) throws FileNotFoundException, IOException {
//    		if (null == f) throw new IllegalArgumentException("Null file passed to VCFFileReader.createStream");
//    		
//	    	InputStream is = FileUtils.isInputGZip(f) ? new BufferedInputStream( new GZIPInputStream(new FileInputStream(f), 1048576)): new BufferedInputStream(new FileInputStream(f));
//	    	return new VcfFileReader(is);
//    }

	public VcfHeader getVcfHeader() {
		return header;
	}

	@Override
	public VcfRecord getRecord(String line) {
		final String[] params = TabTokenizer.tokenize(line);
		final int arrayLength = params.length; 
		if (8 > arrayLength) {
			throw new IllegalArgumentException("Bad VCF format. Insufficient columns: '" + line + "'");
		}
		
		return new VcfRecord(params);
		
	}
	
}
