/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ini4j.Ini;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;

public class IniUtils {
	
	/**
	 * {@link http://ini4j.sourceforge.net/tutorial/IniTutorial.java.html}
	 * @param ini
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
				
				String params[] = entry.getKey().split("\\s");
				// if there are 2 or more entries, we have a chr and a name, otherwise just have a chr
				String chr = null, name =null;
				if (params.length == 1) {
					chr = params[0];
				} else if (params.length > 1) {
					name = params[0];
					chr = params[params.length - 1];
				}
				positions.add(ChrPositionUtils.getChrPositionNameFromString(chr + ":" + entry.getValue(), name));
			}
		}

		return positions;
	}

}
