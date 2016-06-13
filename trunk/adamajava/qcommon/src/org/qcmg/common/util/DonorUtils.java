/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.string.StringUtils;

public class DonorUtils {
	
	public static final String FS = System.getProperty("os.name").toLowerCase().contains("win") ? 
			FileUtils.FILE_SEPARATOR + FileUtils.FILE_SEPARATOR : FileUtils.FILE_SEPARATOR;
	
	public static final String PATTERN_STRING = "([A-Z]{2,5})_[A-Z0-9]{1}[0-9]{2,4}";
	public static final String PPPP_PATTERN_STRING = "([P]{4})_[A-Z0-9]{2,6}";
//	public static final String PATTERN_STRING = "([A-Z]{2}|[A-Z]{4})_[A-Z0-9]{1}[0-9]{2,3}";
	public static final Pattern DONOR_PATTERN = Pattern.compile(PATTERN_STRING);
	public static final Pattern PPPP_DONOR_PATTERN = Pattern.compile(PPPP_PATTERN_STRING);
	public static final Pattern DONOR_FILE_NAME_PATTERN = Pattern.compile( FS + PATTERN_STRING + FS);
	public static final Pattern PPPP_DONOR_FILE_NAME_PATTERN = Pattern.compile( FS + PPPP_PATTERN_STRING + FS);
	
	public static boolean doesStringContainDonor(String stringToSearch) {
		if (StringUtils.isNullOrEmpty(stringToSearch)) return false;
			
		Matcher m = DONOR_PATTERN.matcher(stringToSearch);
		if (m.find()) return true;
		else m = PPPP_DONOR_PATTERN.matcher(stringToSearch);
				
		return m.find();
	}
	
	public static String getDonorFromString(String stringToSearch) {
		if (StringUtils.isNullOrEmpty(stringToSearch)) return null;
		
		Matcher m = DONOR_PATTERN.matcher(stringToSearch);
		if (m.find()) return m.group();
		else m = PPPP_DONOR_PATTERN.matcher(stringToSearch);
		
		return m.find() ? m.group() : null;
	}
	
	public static String getDonorFromFilename(String stringToSearch) {
		if (StringUtils.isNullOrEmpty(stringToSearch)) return null;
		
		Matcher m = DONOR_FILE_NAME_PATTERN.matcher(stringToSearch);
		if (m.find() ) {
			return m.group().substring(1, m.group().length() - 1);
		} else {		// check PPPP regex too
			m = PPPP_DONOR_FILE_NAME_PATTERN.matcher(stringToSearch);
			return m.find() ? m.group().substring(1, m.group().length() - 1) : null;
		}
	}
}
