/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
/**
 * @author jpearson
 * @version $Id: StaticMethods.java,v 1.6 2009/10/27 22:03:09 jpearson Exp $
 * 
 * This class holds static methods that might be useful across all of SolSuite
 * 
 */

package org.qcmg.qprofiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StaticMethods {
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	private static Pattern fileVersionPattern = Pattern.compile("^(.*)\\.(\\d+)$");

	/**
	 * Returns the current date-time as a string for use in time stamping.
	 * 
	 * @return date-time string in format: yyyy-MM-dd HH:mm:ss
	 * @deprecated use <code>DateUtils.getCurrentDateAsString()</code> instead
	 */
	public static String now() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	/**
	 * Compute an MD5 signature for a file
	 * 
	 * @param file File object for file to be MD5'd.
	 */
	public static String computeMD5(File file) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		InputStream is = new FileInputStream(file);
		byte[] buffer = new byte[8192];
		int read = 0;
		try {
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			return output;
		} catch (IOException e) {
			throw new RuntimeException("Unable to process file for MD5", e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				throw new RuntimeException(
						"Unable to close input stream for MD5 calculation", e);
			}
		}

	}

	/**
	 * Concatenate the elements of an array of strings into a single string with
	 * a given separator string between the elements.
	 * 
	 * @param separator string to be placed between adjacent elements
	 * @param strings array of strings to be concatenated
	 * @return concatenated result
	 */
	public static String join(String separator, String[] strings) {
		if (strings == null) {
			return null;
		}
		if (strings.length == 0) {
			return "";
		}

		StringBuffer sb = new StringBuffer(strings[0]);
		for (int i = 1; i < strings.length; ++i) {
			sb.append(separator);
			sb.append(strings[i]);
		}
		return sb.toString();
	}

	/**
	 * Backup a file by renaming it where the renaming is based on appending (or
	 * incrementing) a version number extension. This could turn into a
	 * recursive process if the new names we come up with already exist in which
	 * case those files need to be renamed etc etc. To do the renaming we will
	 * add a numeric version number to the file and increment as needed.
	 * 
	 * @param filename name of file to be renamed with version number
	 */
	public static void backupFileByRenaming(String filename) throws Exception {
		File origFile = new File(filename);
		
		// check that directory exists and is writable
		// this will throw an IOExcpetion if the file path is incorrect
		// if it returns true, do nowt, otherwise - rename existing file
		if (origFile.createNewFile()) {
			
			// delete the file straight away - don't want empty files lying around
			origFile.delete();
			
		} else {

//		 if file already exists, backup by renaming
//		if (origFile.canRead()) {
			Matcher matcher = fileVersionPattern.matcher(origFile
					.getCanonicalPath());
			boolean matchFound = matcher.find();

			// Determine the name we will use to rename the current file
			String fileStem = null;
			Integer fileVersion = 0;
			if (!matchFound) {
				// Original filename has no version so create new filename by
				// appending ".1"
				fileStem = origFile.getCanonicalPath();
				fileVersion = 1;
			} else {
				// Original filename has version so create new filename by
				// incrementing version
				fileStem = matcher.group(1);
				fileVersion = Integer.parseInt(matcher.group(2)) + 1;
			}

			// If new filename already exists then we need to rename that file
			// also so let's use some recursion
			File newFile = new File(fileStem + "." + fileVersion);
			if (newFile.canRead()) {
				backupFileByRenaming(newFile.getCanonicalPath());
			}

			// Finally we get the rename origFile to newFile!
			if (!origFile.renameTo(newFile)) {
				throw new RuntimeException("Unable to rename file from "
						+ origFile.getName() + " to " + newFile.getName());
			}
		}
	}

}
