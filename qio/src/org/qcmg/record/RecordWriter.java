/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.record;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;
import org.qcmg.common.util.FileUtils;

public final class RecordWriter<T> implements Closeable {
	private final File file;
	private final BufferedWriter bos;

	public RecordWriter(final File file, boolean append) throws IOException {
		this.file = file;
		boolean gzip = FileUtils.isFileNameGZip(file);
		OutputStream outputStream  = gzip ? new GZIPOutputStream(new FileOutputStream(file, append)) : new FileOutputStream(file, append); 	
		bos = new BufferedWriter(new OutputStreamWriter(outputStream)); 
	}
	 
	public RecordWriter(final File file) throws IOException {
		this(file,false);
	}	
	
	public void addHeader(final String header) throws IOException {
		bos.write(header);
		//String NEW_LINE = System.getProperty("line.separator");same to below
		bos.newLine();
	}
	
	public void add(final T record) throws IOException {
		String encoded = record instanceof String? (String) record : record.toString();
		bos.write(encoded);
		bos.newLine();
	}

	@Override
	public void close() throws IOException {
		bos.close();
	}

	public File getFile() {
		return file;
	}
}
