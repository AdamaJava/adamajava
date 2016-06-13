/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.util.ArrayList;
import java.util.Collections;

import org.qcmg.common.util.Constants;

public class Feature {

	private final String name;
	private final int priority;
	private final ArrayList<Integer> preList;
	private final ArrayList<Integer> postList;
	private int beforeBases = 0;
	private int afterBases = 0;

	public Feature(String featureString, int priority) {
		
		String[] values = featureString.split(",");
		this.name = values[0];
		this.priority = priority;
		this.preList = new ArrayList<Integer>();
		this.postList = new ArrayList<Integer>();
		
		for (int i=1; i<values.length; i++) {
			String current = values[i];
			if (current.startsWith("+")) {
				postList.add(Integer.valueOf(current.substring(1, current.length())));
			} else if (current.startsWith("-")) {
				preList.add(Integer.valueOf(current.substring(1, current.length())));				
			} else {
				Integer currentInt = Integer.valueOf(current);
				preList.add(currentInt);
				postList.add(currentInt);
			}			
		}
		
		for (Integer i: preList) {
			beforeBases += i;
		}
		for (Integer i: postList) {
			afterBases += i;
		}
		
	}
	
	public String getName() {
		return name;
	}

	public int getPriority() {
		return priority;
	}

	public ArrayList<Integer> getPreList() {
		return preList;
	}

	public ArrayList<Integer> getPostList() {
		return postList;
	}

	public int getBeforeBases() {
		return beforeBases;
	}

	public int getAfterBases() {
		return afterBases;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		Collections.reverse(preList);
		for (Integer i: preList) {
			sb.append(i).append(Constants.COMMA);
		}
		//put it back the correct order
//		Collections.reverse(preList);
		sb.append(name);
		for (Integer i: postList) {
			sb.append(Constants.COMMA).append(i);
		}
		return sb.toString();
	}


}
