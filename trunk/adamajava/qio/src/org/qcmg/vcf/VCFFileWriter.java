/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.vcf;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;

public final class VCFFileWriter implements Closeable {
	private final File file;
	private final OutputStream outputStream;
	
	private VCFFileWriter(final File file, boolean gzip, boolean append) throws IOException {
		this.file = file;
		OutputStream stream = gzip ? new GZIPOutputStream(new FileOutputStream(file, append)) : new FileOutputStream(file, append); 
		outputStream = stream;
	}
		
	/**
	 * Create a text VCFFileWriter not allowing appending
	 * @param file
	 * @throws IOException
	 */
	public VCFFileWriter(final File file) throws IOException {
		this(file, false,false);
	}
	/**
	 * create a gzip/txt VCFFileWriter not allowing appending
	 * @param file
	 * @param gzip
	 * @throws IOException
	 */
	public VCFFileWriter(final File file, boolean gzip) throws IOException {
		this(file, gzip, false);
	}
	
	/**
	 * Create a text VCFFileWriter. It will append to the file if it exists
	 * @param file
	 * @throws IOException
	 * */
	public static VCFFileWriter CreateAppendVcfWriter(final File file) throws IOException{
		return new VCFFileWriter( file, false, true);
	}
	
	public void addHeader(final String headerString) throws IOException {
		outputStream.write((headerString + Constants.NL).getBytes());
		outputStream.flush();
	}

	public void add(final VcfRecord record) throws IOException {
		String encoded = record.toString();
		outputStream.write(encoded.getBytes());
		outputStream.flush();
	}

	@Override
	public void close() throws IOException {
		outputStream.flush();
		outputStream.close();
	}

	public File getFile() {
		return file;
	}
}
