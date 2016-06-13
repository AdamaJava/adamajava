/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.Arrays;


/**
 * NOT THREAD SAFE</p>
 * 
 * Class that wraps an int array, allowing it to be resized should an element attempt to be added that would otherwise be out of bounds
 * 
 * @author oholmes
 *
 */
public class QCMGIntArray {
	
	private static final int MAX_CAPACITY = 2048 * 2048 * 2;	// 8 mill
	
	private int[] array;
	private volatile int capacity;
	
	public  QCMGIntArray(int initialCapacity) {
		capacity = Math.min(initialCapacity, MAX_CAPACITY);
		array = new int[capacity];
	}
	
	public void increment(int arrayPosition) {
		
		if (arrayPosition >= capacity) {
			resize(arrayPosition);
		}
		
		array[arrayPosition] += 1;
	}
	
	public void set(int arrayPosition, int value) {
		
		if (arrayPosition >= capacity) {
			resize(arrayPosition);
		}
		
		array[arrayPosition] = value;
	}
	
	public int get(int arrayPosition) {
		return array[arrayPosition];
	}
	public int length() {
		return array.length;
	}
	
	private void resize(int arrayPosition) {
		capacity = Math.min(arrayPosition * 2 , MAX_CAPACITY);
		array = Arrays.copyOf(array, capacity);
	}
}
