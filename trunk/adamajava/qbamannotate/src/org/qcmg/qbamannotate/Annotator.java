/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamannotate;

import java.io.File;
import java.util.Iterator;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;

import org.qcmg.ma.MAFileReader;
import org.qcmg.ma.MARecord;
import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;

public final class Annotator {
	private final boolean modifyProgramLine;
	private String pgProgramName;
	private String pgProgramVersion;
	private String pgCommandLine;
	private final File outputFile;
	private final File inputBAMFile;
	private final File inputMAFile;
	private SAMFileWriter fileWriter;
	private SAMFileHeader header;
	private SamReader samFileReader;
	private MAFileReader maFileReader;
	private Iterator<SAMRecord> samIterator;
	private Iterator<MARecord> maIterator;
	private MARecord nextMARecord;
	private SAMRecord nextSAMRecord;
	private AnnotatorType type = new Frag();

	public Annotator(final String outputFileName,
			final String inputBAMFileName, final String inputMAFileName,
			final String programName, final String programVersion,
			final String programCommandLine) throws Exception {
		this(new File(outputFileName), new File(inputBAMFileName), new File(
				inputMAFileName), programName, programVersion,
				programCommandLine);
	}

	public Annotator(final String outputFileName,
			final String inputBAMFileName, final String inputMAFileName,
			final AnnotatorType type, final String programName,
			final String programVersion, final String programCommandLine)
			throws Exception {
		this(new File(outputFileName), new File(inputBAMFileName), new File(
				inputMAFileName), type, programName, programVersion,
				programCommandLine);
	}

	public Annotator(final File outputFile, final File inputBAMFile,
			final File inputMAFile, final String programName,
			final String programVersion, final String programCommandLine)
			throws Exception {
		this.outputFile = outputFile;
		this.inputBAMFile = inputBAMFile;
		this.inputMAFile = inputMAFile;
		modifyProgramLine = true;
		pgProgramName = programName;
		pgProgramVersion = programVersion;
		pgCommandLine = programCommandLine;
		marchAndAnnotate();
	}

	public Annotator(final File outputFile, final File inputBAMFile,
			final File inputMAFile, final AnnotatorType type,
			final String programName, final String programVersion,
			final String programCommandLine) throws Exception {
		this.outputFile = outputFile;
		this.inputBAMFile = inputBAMFile;
		this.inputMAFile = inputMAFile;
		this.type = type;
		modifyProgramLine = true;
		pgProgramName = programName;
		pgProgramVersion = programVersion;
		pgCommandLine = programCommandLine;
		marchAndAnnotate();
	}

	public Annotator(final String outputFileName,
			final String inputBAMFileName, final String inputMAFileName)
			throws Exception {
		this(new File(outputFileName), new File(inputBAMFileName), new File(
				inputMAFileName));
	}

	public Annotator(final File outputFile, final File inputBAMFile,
			final File inputMAFile) throws Exception {
		this.outputFile = outputFile;
		this.inputBAMFile = inputBAMFile;
		this.inputMAFile = inputMAFile;
		modifyProgramLine = false;
		marchAndAnnotate();
	}

	public Annotator(final File outputFile, final File inputBAMFile,
			final File inputMAFile, final AnnotatorType type) throws Exception {
		this.outputFile = outputFile;
		this.inputBAMFile = inputBAMFile;
		this.inputMAFile = inputMAFile;
		this.type = type;
		modifyProgramLine = false;
		marchAndAnnotate();
	}

	public AnnotatorType getAnnotatorType() {
		return type;
	}

	public int getNumberOfUnmatchedRecords() {
		return type.getUnmatchedRecordCount();
	}

	private void nextSAMRecord() throws Exception {
		if (samIterator.hasNext()) {
			nextSAMRecord = samIterator.next();
			type.annotate(nextSAMRecord);
		} else {
			nextSAMRecord = null;
		}
	}

	private void nextMARecord() {
		if (maIterator.hasNext()) {
			nextMARecord = maIterator.next();
		} else {
			nextMARecord = null;
		}
	}

	private void marchAndAnnotate() throws Exception {
		try {
			samFileReader = SAMFileReaderFactory.createSAMFileReader( inputBAMFile) ; //new SAMFileReader(inputBAMFile);
			maFileReader = new MAFileReader(inputMAFile);
			header = samFileReader.getFileHeader();
			header.setSortOrder(SAMFileHeader.SortOrder.unsorted);
			if (modifyProgramLine) {
				HeaderUtils.addProgramRecord(header, pgProgramName,
						pgProgramVersion, pgCommandLine);
			}
			final SAMFileWriterFactory factory = new SAMFileWriterFactory();
			fileWriter = factory.makeSAMOrBAMWriter(header, true, outputFile);
			maIterator = maFileReader.iterator();
			samIterator = samFileReader.iterator();
			nextMARecord();
			nextSAMRecord();
			while (!(null == nextMARecord && null == nextSAMRecord)) {
				if (null == nextMARecord) {
					type.markRecordUnmatched(nextSAMRecord);
					fileWriter.addAlignment(nextSAMRecord);
					nextSAMRecord();
				} else if (null == nextSAMRecord) {
					nextMARecord = null;
				} else {
					if (0 == AnnotatorType.compareTriplet(nextSAMRecord,
							nextMARecord)) {
						type.annotate(nextSAMRecord, nextMARecord);
						fileWriter.addAlignment(nextSAMRecord);
						nextSAMRecord();
					} else if (0 > AnnotatorType.compareTriplet(nextSAMRecord,
							nextMARecord)) {
						type.markRecordUnmatched(nextSAMRecord);
						fileWriter.addAlignment(nextSAMRecord);
						nextSAMRecord();
					} else {
						nextMARecord();
					}
				}
			}
			close();
		} catch (Exception ex) {
			closeQuietly();
			throw ex;
		}
		type.generateReport();
	}

	private void close() throws Exception {
		if (null != fileWriter) {
			fileWriter.close();
		}
		if (null != samFileReader) {
			samFileReader.close();
		}
	}

	private void closeQuietly() {
		try {
			close();
		} catch (Exception inner_ex) {
		}
	}
}
