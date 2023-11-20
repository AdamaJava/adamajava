/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup;

import java.io.File;
import java.io.IOException;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.picard.SAMFileReaderFactory;

public class InputBAM {
	
	private final Integer id;
	private final String donor;
	private final File bamFile;
	private final String inputType;
	
	public InputBAM(File bamFile) {
		this(null, null, bamFile, null);
	}

	public InputBAM(Integer id, String donor, File bamFile, String inputType) {
		super();
		this.id = id;
		this.donor = donor;
		this.bamFile = bamFile;
		this.inputType = inputType;
	}
	public Integer getId() {
		return id;
	}

	public File getBamFile() {
		return bamFile;
	}
	public boolean exists() {
		return this.bamFile.exists();
	}

	public SamReader getSAMFileReader() throws IOException {
		return SAMFileReaderFactory.createSAMFileReader(bamFile, null, ValidationStringency.SILENT);
	}
	
	@Override
	public String toString() {
		return switch (inputType) {
			case Options.INPUT_BAM, Options.INPUT_HDF -> "\t\t" + this.bamFile.getAbsolutePath();
			case Options.INPUT_LIST -> this.id + "\t" + this.donor + "\t" + this.bamFile.getAbsolutePath();
			default -> "";
		};
	}
	
	
	public String getAbbreviatedBamFileName() {
		return bamFile.getName().replace(".bam", "");
	}
}

	
