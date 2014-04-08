/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.bammerge;

import java.util.regex.Pattern;

/**
 * The Class GroupReplacement.
 */
public final class GroupReplacement {
	
	/** The Constant colonPattern. */
	private static final Pattern colonPattern = Pattern.compile("[:\\s]+");
	
	/** The raw value. */
	private final String rawValue;
	
	/** The file name. */
	private final String fileName;
	
	/** The old group. */
	private final String oldGroup;
	
	/** The new group. */
	private final String newGroup;

	/**
	 * Instantiates a new group replacement.
	 *
	 * @param value the value
	 * @throws BamMergeException the bam merge exception
	 */
	public GroupReplacement(final String value) throws BamMergeException {
		if (null == value) {
			throw new BamMergeException("NULL_REPLACEMENT_GROUP");
		}
		if (0 == value.length()) {
			throw new BamMergeException("BLANK_REPLACEMENT_GROUP", value);
		}
		rawValue = value;
		String[] params = colonPattern.split(value);
		if (3 != params.length) {
			throw new BamMergeException("BAD_REPLACEMENT_FORMAT", value);
		}
		fileName = params[0].trim();
		oldGroup = params[1].trim();
		newGroup = params[2].trim();
		if (0 == fileName.length()) {
			throw new BamMergeException("BLANK_FILE_NAME_IN_READGROUP", value);
		}
		if (oldGroup.equals(newGroup)) {
			throw new BamMergeException("IDENTICAL_GROUP_FOR_REPLACEMENT",
					value);
		}
	}

	/**
	 * Gets the raw form.
	 *
	 * @return the raw form
	 */
	public String getRawForm() {
		return rawValue;
	}

	/**
	 * Gets the file name.
	 *
	 * @return the file name
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Gets the old group.
	 *
	 * @return the old group
	 */
	public String getOldGroup() {
		return oldGroup;
	}

	/**
	 * Gets the new group.
	 *
	 * @return the new group
	 */
	public String getNewGroup() {
		return newGroup;
	}

}
