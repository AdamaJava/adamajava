/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.split;

public class AffineSplitType extends SplitType {
	private final String fileExtension;
	
	AffineSplitType(final String fileExtension) {
		this.fileExtension = fileExtension;
	}
	@Override
	public String getFileExtension() {
		return fileExtension;
	}

}
