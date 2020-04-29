package au.edu.qimr.vcftools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Rule {
	
	public enum Type {
		INFO,
		FILTER,
		FORMAT
	}
	
	private final List<Map<String, String>> infoRules;
	private final List<Map<String, String>> filterRules;
	private final List<Map<String, String>> formatRules;
	
	public Rule(int numberOfFiles) {
		infoRules = new ArrayList<>(numberOfFiles);
		filterRules = new ArrayList<>(numberOfFiles);
		formatRules = new ArrayList<>(numberOfFiles);
	}
	
	public Map<String, String> getRules(int filePosition, Type type) {
		switch (type) {
		case INFO:
			return null == infoRules.get(filePosition) ? Collections.emptyMap() : infoRules.get(filePosition);
		case FILTER:
			return null == filterRules.get(filePosition) ? Collections.emptyMap() : filterRules.get(filePosition);
		case FORMAT:
			return null == formatRules.get(filePosition) ? Collections.emptyMap() : formatRules.get(filePosition);
		default:
			return Collections.emptyMap();
		}
	}
	
	public void setRules(int filePosition,  Map<String, String> filterRules, Map<String, String> infoRules, Map<String, String> formatRules) {
		this.filterRules.add(filePosition, filterRules);
		this.infoRules.add(filePosition, infoRules);
		this.formatRules.add(filePosition, formatRules);
	}

}
