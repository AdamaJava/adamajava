/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamannotate;

import java.io.File;
import java.util.Iterator;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import org.qcmg.picard.HeaderUtils;

public final class SelfAnnotator {
	private final boolean modifyProgramLine;
	private int recordCount = 0;
	private String pgProgramName;
	private String pgProgramVersion;
	private String pgCommandLine;
	private final File outputFile;
	private final File inputFile;
	private File reportedFile;
	private File unreportedFile;
	private SAMFileWriter fileWriter;
	private SAMFileHeader header;
	private SAMFileReader samFileReader;
	private AnnotatorType type = new Frag();
	private final boolean reportBams;

	public SelfAnnotator(final String outputFileName,
			final String inputFileName, final String programName,
			final String programVersion, final String programCommandLine,
			final boolean reportBams) throws Exception {
		this(new File(outputFileName), new File(inputFileName), programName,
				programVersion, programCommandLine, reportBams);
	}

	public SelfAnnotator(final String outputFileName,
			final String inputFileName, final AnnotatorType type,
			final String programName, final String programVersion,
			final String programCommandLine, final boolean reportBams)
			throws Exception {
		this(new File(outputFileName), new File(inputFileName), type,
				programName, programVersion, programCommandLine, reportBams);
	}

	public SelfAnnotator(final File outputFile, final File inputFile,
			final String programName, final String programVersion,
			final String programCommandLine, final boolean reportBams)
			throws Exception {
		if (outputFile.exists()) {
			throw new Exception("Output file of the same name already exists");
		}
		if (!inputFile.exists()) {
			throw new Exception("The specified input file does not exist.");
		}
		this.outputFile = outputFile;
		this.inputFile = inputFile;
		String reportedFileName = "reported." + inputFile.getName();
		this.reportedFile = new File(reportedFileName);
		String unreportedFileName = "unreported." + inputFile.getName();		
		this.unreportedFile = new File(unreportedFileName);
		this.reportBams = reportBams;
		modifyProgramLine = true;
		pgProgramName = programName;
		pgProgramVersion = programVersion;
		pgCommandLine = programCommandLine;
		annotate();
	}

	public SelfAnnotator(final File outputFile, final File inputFile,
			final AnnotatorType type, final String programName,
			final String programVersion, final String programCommandLine,
			final boolean reportBams) throws Exception {
		this.outputFile = outputFile;
		this.inputFile = inputFile;
		String reportedFileName = "reported." + inputFile.getName();
		this.reportedFile = new File(reportedFileName);
		String unreportedFileName = "unreported." + inputFile.getName();		
		this.unreportedFile = new File(unreportedFileName);
		this.type = type;
		this.reportBams = reportBams;
		modifyProgramLine = true;
		pgProgramName = programName;
		pgProgramVersion = programVersion;
		pgCommandLine = programCommandLine;
		annotate();
	}

	public SelfAnnotator(final String outputFileName,
			final String inputFileName, final boolean reportBams)
			throws Exception {
		this(new File(outputFileName), new File(inputFileName), reportBams);
	}

	public SelfAnnotator(final File outputFile, final File inputFile,
			final boolean reportBams) throws Exception {
		this.outputFile = outputFile;
		this.inputFile = inputFile;
		this.reportBams = reportBams;
		String reportedFileName = "reported." + inputFile.getName();
		this.reportedFile = new File(reportedFileName);
		String unreportedFileName = "unreported." + inputFile.getName();		
		this.unreportedFile = new File(unreportedFileName);
		modifyProgramLine = false;
		annotate();
	}

	public SelfAnnotator(final File outputFile, final File inputFile,
			final AnnotatorType type, final boolean reportBams)
			throws Exception {
		this.outputFile = outputFile;
		this.inputFile = inputFile;
		this.type = type;
		this.reportBams = reportBams;
		String reportedFileName = "reported." + inputFile.getName();
		this.reportedFile = new File(reportedFileName);
		String unreportedFileName = "unreported." + inputFile.getName();		
		this.unreportedFile = new File(unreportedFileName);
		modifyProgramLine = false;
		annotate();
	}

	public AnnotatorType getAnnotatorType() {
		return type;
	}

	public int getNumberOfUnmatchedRecords() {
		return type.getUnmatchedRecordCount();
	}

	private void annotate() throws Exception {
		try {
			samFileReader = new SAMFileReader(inputFile);
			header = samFileReader.getFileHeader();
			if (modifyProgramLine) {
				HeaderUtils.addProgramRecord(header, pgProgramName,
						pgProgramVersion, pgCommandLine);
			}
			final SAMFileWriterFactory factory = new SAMFileWriterFactory();
			fileWriter = factory.makeSAMOrBAMWriter(header, true, outputFile);
			SAMFileWriter unreportedFileWriter = factory.makeSAMOrBAMWriter(
					header, true, unreportedFile);
			SAMFileWriter reportedFileWriter = factory.makeSAMOrBAMWriter(
					header, true, reportedFile);
			for (final SAMRecord record : samFileReader) {
				boolean reported = type.annotate(record);
				if (reportBams) {
					if (reported) {
						reportedFileWriter.addAlignment(record);
					} else {
						unreportedFileWriter.addAlignment(record);
					}
				}
				fileWriter.addAlignment(record);
				recordCount++;
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
