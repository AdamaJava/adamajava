/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QToolMeta {
	
	private String toolName;
	private List<KeyValue> toolMetaList;
	
	public QToolMeta(String toolName, KeyValue ... toolSpecificMetaInformation) {
		this.toolName = toolName;
		
		if (toolSpecificMetaInformation.length > 0) {
			
			toolMetaList = new ArrayList<KeyValue>();
			
			for (KeyValue kv : toolSpecificMetaInformation) {
				toolMetaList.add(kv);
			}
		} else {
			toolMetaList = Collections.emptyList();
		}
	}
	
	public List<KeyValue> getToolMetaData() {
		return toolMetaList;
	}
	
	public String getToolMetaDataToString() {
		StringBuilder sb = new StringBuilder();
		
		for (KeyValue kv : toolMetaList) {
			sb.append(kv.toToolString(toolName));
		}
		return sb.toString();
	}

}
