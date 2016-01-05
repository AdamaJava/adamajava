/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.indel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ini4j.Ini;
import org.qcmg.common.model.Rule;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.PileupUtils;
import org.qcmg.common.util.TabTokenizer;

public class IniFileUtil {	
	public static String secIOs = "IOs";
	public static String secIDs = "ids";
	public static String secParameter = "parameters";
	public static String secRule = "rules";

 
	
	/**
	 * {@link http://ini4j.sourceforge.net/tutorial/IniTutorial.java.html}
	 * @param ini
	 * @param format
	 * @param type
	 * @return
	 */
	public static String getOutputFile(Ini ini, String type) {
		return getEntry(ini, secIOs, type);
	}
	
	public static String getInputFile(Ini ini, String type) {
		return getEntry(ini, secIOs, type);
	}
	
	public static String [] getInputFiles(Ini ini, String type) {
		checkIni(ini);
		Ini.Section ios = ini.get(secIOs);
		return ios.getAll(type, String[].class);
	}
	
	public static String getDonorId(Ini ini) {
		return getEntry(ini, secIOs, "donorId");
	}
	
	public static String getEntry(Ini ini, String parent, String child) {
		checkIni(ini);
		// use fetch rather than get to resolve any variable references
		return ini.fetch(parent, child);
	}
	
	
	private static void checkIni(Ini ini) {
		if (null == ini) throw new IllegalArgumentException("Missing ini file reference");
	}

}
