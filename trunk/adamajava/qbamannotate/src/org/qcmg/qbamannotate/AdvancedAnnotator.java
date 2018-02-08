/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamannotate;

import java.io.File;

public final class AdvancedAnnotator {
	private int unmatchedRecordCount = 0;
	private final AnnotatorType type;

	public AdvancedAnnotator(final String outputFileName,
			final String inputBAMFileName, final String firstInputMAFileName,
			final String secondInputMAFileName, final AnnotatorType type,
			final String programName, final String programVersion,
			final String programCommandLine) throws Exception {
		File outputFile = new File(outputFileName);
		File inputBAMFile = new File(inputBAMFileName);
		File firstMAFile = new File(firstInputMAFileName);
		File secondMAFile = new File(secondInputMAFileName);

		this.type = type;
		String ext = getExtension(outputFile);
		if (null == ext) {
			throw new BamAnnotateException("UNSUITABLE_FILE");
		}
		ext = "." + ext;

		File canonicalFile = outputFile.getCanonicalFile();
		File outputDir = canonicalFile.getParentFile();
		File tempFile = File.createTempFile("tmp", ext, outputDir);
		new Annotator(tempFile, inputBAMFile, firstMAFile, type);
		type.generateReport();
		type.resetCount();
		Annotator secondPass = new Annotator(outputFile, tempFile,
				secondMAFile, type, programName, programVersion,
				programCommandLine);
		unmatchedRecordCount = secondPass.getNumberOfUnmatchedRecords();
		if (tempFile.exists()) {
			tempFile.delete();
		}
	}

	public AdvancedAnnotator(final String outputFileName,
			final String inputBAMFileName, final String firstInputMAFileName,
			final AnnotatorType type, final String programName,
			final String programVersion, final String programCommandLine)
			throws Exception {
		File outputFile = new File(outputFileName);
		File inputBAMFile = new File(inputBAMFileName);
		File firstMAFile = new File(firstInputMAFileName);
		this.type = type;
		String ext = getExtension(outputFile);
		if (null == ext) {
			throw new BamAnnotateException("UNSUITABLE_FILE");
		}
		Annotator pass = new Annotator(outputFile, inputBAMFile, firstMAFile, type);
		type.generateReport();
		type.resetCount();
		unmatchedRecordCount = pass.getNumberOfUnmatchedRecords();
	}

	public AnnotatorType getAnnotatorType() {
		return type;
	}

	public int getNumberOfUnmatchedRecords() {
		return unmatchedRecordCount;
	}

	private static String getExtension(File f) {
		String s = f.getName();
		int i = s.lastIndexOf('.');
		if (i > 0 && i < s.length() - 1 &&  ! f.isDirectory() ) {
			return s.substring(i + 1).toLowerCase();
		}
		return null;
	}

}
