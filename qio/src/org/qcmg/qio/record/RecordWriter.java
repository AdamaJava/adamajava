/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.qio.record;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;

public class RecordWriter<T> implements Closeable {
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
	
	/**
	 * it appends header line to writer and also automatically append an newline mark If there is no one on the record string.
	 * @param header
	 * @throws IOException
	 */
	public void addHeader(final String header) throws IOException {
		String line = header.endsWith(Constants.NL_STRING) ? header : header + Constants.NL;
		bos.write(line);
	}
	
	public void addHeader(List<String> header) throws IOException {
		for (String str : header) {			
			addHeader(str);
		}
	}
	
	/**
	 * it will convert the record to string and append to writer. It will automatically append an newline mark If there is no newLine on the record string.
	 * @param record is not allowed to be null
	 * @throws IOException
	 */
	public void add(final T record) throws IOException {		
		String encoded = record instanceof String ? (String) record : record.toString();
		String line = encoded.endsWith(Constants.NL_STRING) ? encoded : encoded + Constants.NL;
		bos.write(line);
	}

	@Override
	public void close() throws IOException {
		bos.close();
	}

	public File getFile() {
		return file;
	}
}
