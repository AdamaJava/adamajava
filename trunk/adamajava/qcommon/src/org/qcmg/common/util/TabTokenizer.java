/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class TabTokenizer {
	
	private static final char DELIM = '\t';
	private static final String[] stringArrayType = new String[] {};
	
	public static String[] tokenize(final String data) {
		return tokenize(data, DELIM);
	}
	public static String[] tokenize(final String data, int requiredEntries) {
		return tokenize(data, DELIM, requiredEntries);
	}
	
	public static String[] tokenize(final String data, final char delim) {
		int nextIndex = data.indexOf(delim);
		if (nextIndex < 0) throw new IllegalArgumentException("no delimiters '" + delim + "' found in string: " + data);
		
		int currentIndex = 0;
		final List<String> resultList = new ArrayList<String>();
		
		resultList.add(data.substring(currentIndex, nextIndex));
		currentIndex = nextIndex + 1;
		
		nextIndex = data.indexOf(delim, currentIndex);
		while (nextIndex != -1) {
			resultList.add(data.substring(currentIndex, nextIndex));
			currentIndex = nextIndex + 1;
			nextIndex = data.indexOf(delim, currentIndex);
		}
		// get last string
		resultList.add(data.substring(currentIndex));
		
		return resultList.toArray(stringArrayType);
	}
	
	public static String[] tokenize(final String data, final char delim, final int requiredEntries) {
		int noOfEntries = 0;
		int nextIndex = data.indexOf(delim);
		if (nextIndex < 0) throw new IllegalArgumentException("no delimiters '" + delim + "' found in string: " + data);
		
		int currentIndex = 0;
		final List<String> resultList = new ArrayList<String>();
		
		resultList.add(data.substring(currentIndex, nextIndex));
		noOfEntries++;
		currentIndex = nextIndex + 1;
		
		nextIndex = data.indexOf(delim, currentIndex);
		while ((noOfEntries <= requiredEntries) && nextIndex != -1) {
			resultList.add(data.substring(currentIndex, nextIndex));
			noOfEntries++;
			currentIndex = nextIndex + 1;
			nextIndex = data.indexOf(delim, currentIndex);
		}
		// get last string
		resultList.add(data.substring(currentIndex));
		
		return resultList.toArray(stringArrayType);
	}
	
	public static String[] tokenizeCharAt(final String data) {
		return tokenizeCharAt(data, DELIM);
	}
	
	public static String[] tokenizeCharAt(final String data, final char delim) {
		final List<String> resultList = new ArrayList<String>();
		
		 int i=0;
		 int length = data.length();
	        while (i<=length) {
	                int start = i;
	                while (i<length && data.charAt(i)!=delim) {
	                        i++;
	                }
	                resultList.add(data.substring(start,i));
	                 // do something with the string here
	                i++;
	        }
		return resultList.toArray(stringArrayType);
	}
	
	static class Iter implements Iterable<String>, Iterator<String> {
		private final String data;
		private final char delim;
		private int nextIndex;
		private int currentIndex = 0;
		private String next;
		private boolean lastRecord = false;
		
		public Iter(String data, char delim) {
			this.data = data;
			this.delim = delim;
			readNext();
		}

		@Override
		public Iterator<String> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			return null != next;
		}

		@Override
		public String next() {
			if ( ! hasNext())
				throw new NoSuchElementException();
			
			String s = next;
			readNext();
			return s;
		}
		
		private void readNext() {
			if (lastRecord) {
				next = null;
				return;
			}
			nextIndex = data.indexOf(delim, currentIndex);
			if (nextIndex < 0) {
				lastRecord = true;
				next = data.substring(currentIndex);
			} else {
				next = data.substring(currentIndex, nextIndex);
				currentIndex = nextIndex + 1;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}

}
