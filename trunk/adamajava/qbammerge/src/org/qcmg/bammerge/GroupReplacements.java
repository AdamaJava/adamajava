/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.bammerge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * The Class GroupReplacements.
 */
public final class GroupReplacements {

	/** The input file names. */
	private final String[] inputFileNames;

	/** The list of replacements. */
	private final List<GroupReplacement> replacements;

	/** The file name mappings. */
	private final Map<String, Map<String, GroupReplacement>> fileNameMappings = new HashMap<String, Map<String, GroupReplacement>>();

	/**
	 * Instantiates a new group replacements.
	 * 
	 * @param groupReplacements
	 *            the group replacements
	 * @param inputFileNameArray
	 *            the input file name array
	 * @throws BamMergeException
	 *             the bam merge exception
	 */
	public GroupReplacements(final List<GroupReplacement> groupReplacements,
			final String[] inputFileNameArray) throws BamMergeException {
		replacements = groupReplacements;
		inputFileNames = inputFileNameArray;
		detectBadReplacementFileNames();
		detectBadReplacementGroups();
		for (int i = 0; i < inputFileNames.length; i++) {
			String fileName = inputFileNames[i];
			Map<String, GroupReplacement> groupMappings = new HashMap<String, GroupReplacement>();
			for (Object obj : replacements) {
				GroupReplacement replacement = (GroupReplacement) obj;
				if (replacement.getFileName().equals(fileName)) {
					String oldGroup = replacement.getOldGroup();
					groupMappings.put(oldGroup, replacement);
				}
			}
			fileNameMappings.put(fileName, groupMappings);
		}
	}

	/**
	 * Instantiates a new group replacements.
	 * 
	 * @param groupReplacements the group replacements
	 * @param inputFileNameArray the input file name array
	 * @throws BamMergeException the bam merge exception
	 */
	public GroupReplacements(final String[] groupReplacements, final String[] inputFileNameArray) throws BamMergeException {
		replacements = new Vector<GroupReplacement>();
		for (String value : groupReplacements) {
			GroupReplacement details = new GroupReplacement(value);
			replacements.add(details);
		}
		inputFileNames = inputFileNameArray;
		detectBadReplacementFileNames();
		detectBadReplacementGroups();
		for (int i = 0; i < inputFileNames.length; i++) {
			String fileName = inputFileNames[i];
			Map<String, GroupReplacement> groupMappings = new HashMap<String, GroupReplacement>();
			for (Object obj : replacements) {
				GroupReplacement replacement = (GroupReplacement) obj;
				if (replacement.getFileName().equals(fileName)) {
					String oldGroup = replacement.getOldGroup();
					groupMappings.put(oldGroup, replacement);
				}
			}
//			if ( ! groupMappings.isEmpty()) {
				fileNameMappings.put(fileName, groupMappings);
//			}
		}
	}

	/**
	 * Gets the group mappings.
	 * 
	 * @param fileName
	 *            the file name
	 * @return the group mappings
	 */
	public Map<String, GroupReplacement> getGroupMappings(String fileName) {
		return fileNameMappings.get(fileName);
	}

	/**
	 * Detect bad replacement file names.
	 * 
	 * @throws BamMergeException
	 *             the bam merge exception
	 */
	private void detectBadReplacementFileNames() throws BamMergeException {
		for (GroupReplacement r : replacements) {
			if ( ! isInputFileName(r.getFileName())) {
				throw new BamMergeException("BAD_GROUP_REPLACEMENT_FILENAME", r
						.getRawForm(), r.getFileName());
			}
		}
	}

	/**
	 * Checks if is input file name.
	 * 
	 * @param value
	 *            the value
	 * @return true, if is input file name
	 */
	private boolean isInputFileName(String value) {
		for (int i = 0; i < inputFileNames.length; i++) {
			if (inputFileNames[i].equals(value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Detect bad replacement groups.
	 * 
	 * @throws BamMergeException
	 *             the bam merge exception
	 */
	private void detectBadReplacementGroups() throws BamMergeException {
		for (GroupReplacement basis : replacements) {
			for (GroupReplacement obj : replacements) {
				detectBadOldGroup(obj, basis);
				detectBadNewGroup(obj, basis);
			}
		}
	}

	/**
	 * Detect bad old group.
	 * 
	 * @param replacement
	 *            the replacement
	 * @param basis
	 *            the basis
	 * @throws BamMergeException
	 *             the bam merge exception
	 */
	private static void detectBadOldGroup(final GroupReplacement replacement,
			final GroupReplacement basis) throws BamMergeException {
		if (basis != replacement) {
			if (replacement.getFileName().equals(basis.getFileName())) {
				if (replacement.getOldGroup().equals(basis.getOldGroup())) {
					throw new BamMergeException(
							"CLASHING_OLD_REPLACEMENT_GROUP", basis
									.getOldGroup(), basis.getRawForm(),
							replacement.getRawForm());
				}
			}
		}
	}

	/**
	 * Detect bad new group.
	 * 
	 * @param replacement
	 *            the r
	 * @param basis
	 *            the basis
	 * @throws BamMergeException
	 *             the bam merge exception
	 */
	private static void detectBadNewGroup(final GroupReplacement replacement,
			final GroupReplacement basis) throws BamMergeException {
		if (basis != replacement) {
			if (replacement.getNewGroup().equals(basis.getNewGroup())) {
				throw new BamMergeException("CLASHING_NEW_REPLACEMENT_GROUP",
						basis.getNewGroup(), basis.getRawForm(), replacement
								.getRawForm());
			}
		}
	}

}
