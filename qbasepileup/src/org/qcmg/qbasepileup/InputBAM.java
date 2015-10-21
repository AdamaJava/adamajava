/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup;

import java.io.File;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;

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
	public String getDonor() {
		return donor;
	}
	public File getBamFile() {
		return bamFile;
	}
	public boolean exists() {
		return this.bamFile.exists();
	}
	public String getInputType() {
		return inputType;
	}
	
	public SAMFileReader getSAMFileReader() {
		return SAMFileReaderFactory.createSAMFileReader(bamFile, ValidationStringency.SILENT);
	}
	
	@Override
	public String toString() {
		if (inputType.equals(Options.INPUT_BAM)) {
			return "\t\t" + this.bamFile.getAbsolutePath();
		} else if (inputType.equals(Options.INPUT_LIST)){
			return this.id + "\t" + this.donor + "\t" + this.bamFile.getAbsolutePath();
		} else if (inputType.equals(Options.INPUT_HDF)){
			return "\t\t" + this.bamFile.getAbsolutePath();
		} else {
			return new String();
		}		
	}
	
	
	public String getAbbreviatedBamFileName() {
		return bamFile.getName().replace(".bam", "");
	}
}

	