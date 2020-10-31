/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.vcf;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VcfRecord;

public final class VCFFileWriter implements Closeable {
	private final File file;
	private final OutputStream outputStream;
	
	/**
	 * create a gzip/txt VCFFileWriter according file name ending. 
	 * @param file: add ending ( .gz/.gzip ) to file name end if want to create a gzip file
	 * @param append: set to true if allowing appending
	 * @throws IOException
	 */
	public VCFFileWriter(final File file, boolean append) throws IOException {
		this.file = file;
		boolean gzip = FileUtils.isFileNameGZip(file);
		outputStream  = gzip ? new GZIPOutputStream(new FileOutputStream(file, append)) : new FileOutputStream(file, append); 		 
	}
	
	/**
	 * Create a text/gzip VCFFileWriter not allowing appending
	 * @param file
	 * @throws IOException
	 */
	public VCFFileWriter(final File file) throws IOException {
		this(file,false);
	}	
	
	/**
	 * Create a text VCFFileWriter. It will append to the file if it exists
	 * @param file
	 * @throws IOException
	 * */
	public static VCFFileWriter createAppendVcfWriter(final File file) throws IOException{
		return new VCFFileWriter( file, true);
	}	
	
	public void addHeader(final String headerString) throws IOException {
		outputStream.write((headerString + Constants.NL).getBytes());
		outputStream.flush();
	}

	public void add(final VcfRecord record) throws IOException {
		outputStream.write(record.toString().getBytes());
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
