/*
 * All code copyright The Queensland Centre for Medical Genomics.
 *
 * All rights reserved.
 */
package org.qcmg.vcf;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.qcmg.common.model.VCFRecord;

public final class VCFFileWriter implements Closeable {
	private final File file;
	private final OutputStream outputStream;

	public VCFFileWriter(final File file) throws IOException {
		this(file, false);
	}
	
	public VCFFileWriter(final File file, boolean gzip) throws IOException {
		this.file = file;
		OutputStream stream = gzip ? new GZIPOutputStream(new FileOutputStream(file)) : new FileOutputStream(file); 
		outputStream = stream;
	}
	
	public void addHeader(final String headerString) throws IOException {
		outputStream.write(headerString.getBytes());
		outputStream.flush();
	}

	public void add(final VCFRecord record) throws IOException {
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
