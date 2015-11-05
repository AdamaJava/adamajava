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

	public static List<Rule> getRules(Ini ini, String type) {
		if (StringUtils.isNullOrEmpty(type))
			throw new IllegalArgumentException("null or empty rule type passed to getRules");
		
		checkIni(ini);
		
		List<Rule> rules = new ArrayList<Rule>();
		
		Ini.Section section = ini.get("rules");
		if (null != section) {
			for (Map.Entry<String,String> entry : section.entrySet()) {
//				System.out.println("entry key: " + entry.getKey());
				
				if (entry.getKey().startsWith(type)) {
					String[] values = TabTokenizer.tokenize(entry.getValue(), ',');
					final int min = Integer.parseInt(values[0]);
					final int max = StringUtils.isNullOrEmpty(values[1]) ? Integer.MAX_VALUE : Integer.parseInt(values[1]);
					final int value = Integer.parseInt(values[2]);
					rules.add(new Rule(min, max, value));
				}
			}
		}

		return rules;
	}

	/**
	 * Returns the lowest value for the list of rules regardless or whether its a normal or tumour rule.
	 * This is generally used to provide a minimum coverage values when examining pileup
	 *  
	 * @param ini
	 * @return int lowest vlaue specified in rules
	 * @throws SnpException
	 */
	public static int getLowestRuleValue(Ini ini) throws Exception {
		checkIni(ini);

		int minValue = Integer.MAX_VALUE;
		
		Ini.Section section = ini.get("rules");
		if (null != section) {
			for (Map.Entry<String,String> entry : section.entrySet()) {
//				System.out.println("entry key: " + entry.getKey());
				
	//			if (entry.getKey().startsWith(type)) {
					String[] values = TabTokenizer.tokenize(entry.getValue(), ',');
	//				final int min = Integer.parseInt(values[0]);
	//				final int max = Integer.parseInt(values[1]);
					final int value = Integer.parseInt(values[2]);
					minValue = Math.min(minValue, value);
	//			}
			}
		}

//		Set<String> sectionNames = ini.keySet();
//		for (String sectionName : sectionNames) {
//			if (sectionName.contains("rules")) {
//				Ini.Section rule = ini.get(sectionName);
//				int value = Integer.parseInt(rule.get("value"));
//				minValue = Math.min(minValue, value);
//			}
//		}

		if (minValue == Integer.MAX_VALUE)
			throw new Exception("NO_VALID_RULES_IN_INI");
//		throw new SnpException(
//		"Did not retrieve a valid value for any rule");

		return minValue;
	}

	public static int getNumberOfFiles(Ini ini, char type) {
		checkIni(ini);
		return PileupUtils.getNoOfFilesFromPileupFormat(ini.get("parameters", "pileupOrder"), type);
	}
	
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
	
	public static String getPatientId(Ini ini) {
		return getEntry(ini, "patient", "id");
	}
	
	public static String getEntry(Ini ini, String parent, String child) {
		checkIni(ini);
		// use fetch rather than get to resolve any variable references
		return ini.fetch(parent, child);
	}
	
	public static boolean isPatientFemale(Ini ini) {
		String isFemale = getEntry(ini, "patient", "female");
		return Boolean.parseBoolean(isFemale);
	}
	
	private static void checkIni(Ini ini) {
		if (null == ini) throw new IllegalArgumentException("Missing ini file reference");
	}

}
