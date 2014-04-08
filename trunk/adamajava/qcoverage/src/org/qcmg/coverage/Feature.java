/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Feature {

	private String name;
	private int priority;
	private ArrayList<Integer> preList;
	private ArrayList<Integer> postList;
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
				postList.add(new Integer(current.substring(1, current.length())));
			} else if (current.startsWith("-")) {
				preList.add(new Integer(current.substring(1, current.length())));				
			} else {
				Integer currentInt = new Integer(current);
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

	public void setName(String name) {
		this.name = name;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public ArrayList<Integer> getPreList() {
		return preList;
	}

	public void setPreList(ArrayList<Integer> preList) {
		this.preList = preList;
	}

	public ArrayList<Integer> getPostList() {
		return postList;
	}

	public void setPostList(ArrayList<Integer> postList) {
		this.postList = postList;
	}

	public int getBeforeBases() {
		return beforeBases;
	}

	public void setBeforeBases(int beforeBases) {
		this.beforeBases = beforeBases;
	}

	public int getAfterBases() {
		return afterBases;
	}

	public void setAfterBases(int afterBases) {
		this.afterBases = afterBases;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		Collections.reverse(preList);
		for (Integer i: preList) {
			sb.append(i + ",");
		}
		//put it back the correct order
		Collections.reverse(preList);
		sb.append(name + ",");
		for (Integer i: postList) {
			sb.append(i + ",");
		}
		String result = sb.toString();
		return result.substring(0, result.length()-1);
	}


}
