package org.qcmg.common.util;

import static org.qcmg.common.util.FileUtils.FILE_SEPARATOR;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.string.StringUtils;

public class DonorUtils {
	
	public static final String PATTERN_STRING = "([A-Z]{2}|[A-Z]{4})_[A-Z0-9]{1}[0-9]{2,3}";
	public static final Pattern DONOR_PATTERN = Pattern.compile(PATTERN_STRING);
	public static final Pattern DONOR_FILE_NAME_PATTERN = Pattern.compile( FILE_SEPARATOR + PATTERN_STRING + FILE_SEPARATOR);
	
	public static boolean doesStringContainDonor(String stringToSearch) {
		if (StringUtils.isNullOrEmpty(stringToSearch)) return false;
			
		Matcher m = DONOR_PATTERN.matcher(stringToSearch);
		return m.find();
	}
	
	public static String getDonorFromString(String stringToSearch) {
		if (StringUtils.isNullOrEmpty(stringToSearch)) return null;
		
		Matcher m = DONOR_PATTERN.matcher(stringToSearch);
		return m.find() ? m.group() : null;
	}
	
	public static String getDonorFromFilename(String stringToSearch) {
		if (StringUtils.isNullOrEmpty(stringToSearch)) return null;
		
		Matcher m = DONOR_FILE_NAME_PATTERN.matcher(stringToSearch);
		return m.find() ? m.group().substring(1, m.group().length() - 1) : null;
	}
}
