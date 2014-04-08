/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ini4j.Ini;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;

public class IniUtils {
	
	public static final String chrPos = "[a-zA-Z0-9]{1,}:[0-9]{1,}-[0-9]{1,}";
	public static final Pattern chrPosPattern = Pattern.compile(chrPos);
	

	/**
	 * {@link http://ini4j.sourceforge.net/tutorial/IniTutorial.java.html}
	 * @param ini
	 * @param format
	 * @param type
	 * @return
	 */
	public static String getOutputFile(Ini ini, String type) {
		return getEntry(ini, "outputFiles", type);
	}
	
	public static String getInputFile(Ini ini, String type) {
		return getEntry(ini, "inputFiles", type);
	}
	
	public static String [] getInputFiles(Ini ini, String type) {
		checkIni(ini);
		Ini.Section inputFiles = ini.get("inputFiles");
		return inputFiles.getAll(type, String[].class);
	}
	
	public static String getEntry(Ini ini, String parent, String child) {
		checkIni(ini);
//		// use fetch rather than get to resolve any variable references
		return ini.fetch(parent, child);
	}
	
	private static void checkIni(Ini ini) {
		if (null == ini) throw new IllegalArgumentException("Missing ini file reference");
	}
	
	public static List<ChrPosition> getPositions(Ini ini, String type) {
		if (StringUtils.isNullOrEmpty(type))
			throw new IllegalArgumentException("null or empty type passed to getPositions");
		
		checkIni(ini);
		
		List<ChrPosition> positions = new ArrayList<>();
		
		Ini.Section section = ini.get(type);
		if (null != section) {
			for (Map.Entry<String,String> entry : section.entrySet()) {
//				System.out.println("entry key: " + entry.getKey() + ", entry value: " + entry.getValue());
				
				Matcher m = chrPosPattern.matcher(entry.getKey() + ":" + entry.getValue());
				if (m.find()) {
					String chrPos = m.group();
					positions.add(ChrPositionUtils.getChrPositionFromString(chrPos));
				} else {
					throw new IllegalArgumentException("Invalid chromosome and position format: " 
								+ entry.getKey() + ":" + entry.getValue() + " - should be chr1:12345-678910");
				}
				
			}
		}

		return positions;
	}

}
