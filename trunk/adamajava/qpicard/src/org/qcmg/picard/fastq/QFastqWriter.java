/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.fastq;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import htsjdk.samtools.fastq.FastqConstants;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;

import org.qcmg.common.util.FileUtils;

public class QFastqWriter implements FastqWriter {
	
	public static final char NL = '\n';
	
	private final Writer writer;
	
	public QFastqWriter(final File f) throws IOException {
		if(FileUtils.isFileNameGZip(f)){
			writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)));
		} else {
			writer = new FileWriter(f);
		}
	}

	@Override
	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void write(FastqRecord rec) {
	   	try {
			writer.write(FastqConstants.SEQUENCE_HEADER);
			writer.write(rec.getReadHeader());
			writer.write(NL);
			writer.write(rec.getReadString());
			writer.write(NL);
			writer.write(FastqConstants.QUALITY_HEADER);
			writer.write(rec.getBaseQualityHeader() == null ? "" : rec.getBaseQualityHeader());
			writer.write(NL);
			writer.write(rec.getBaseQualityString());
			writer.write(NL);
		} catch (IOException e) {
			e.printStackTrace();
		}
//	        if (writer.checkError()) {
//	            throw new PicardException("Error in writing file " + file);
//	        }
		
	}

}
