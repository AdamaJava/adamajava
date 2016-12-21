/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLongArray;

public class QCMGAtomicLongArray {
	
	private static final int MAX_CAPACITY = 2048 * 2048;	// 4 mill 
	
	// atomic boolean used as a lock when resizing the array
	private final AtomicBoolean resizingInProgress = new AtomicBoolean(false);
	
	private AtomicLongArray array;
	private volatile int capacity;
	
	public  QCMGAtomicLongArray(final int initialCapacity) {
		// double capacity 
		capacity = Math.min(initialCapacity * 2, MAX_CAPACITY);
		array = new AtomicLongArray(capacity);
	}
	
	public void increment(final int arrayPosition) {		
		increment(arrayPosition, 1);		
	}

	public void increment(final int arrayPosition, final long value) {
		
		if (arrayPosition >= capacity) {
			resize(arrayPosition);
		}
		
		if (resizingInProgress.get()) {
			// cas until resizing is complete
			while  ( ! resizingInProgress.compareAndSet(false, false) ) {}
		}
		array.addAndGet(arrayPosition, value);
	}
	
	public long get(int arrayPosition) { 	return array.get(arrayPosition); }
	public long length() { 	return array.length(); }
	
	private void resize(final int arrayPosition) {
		// lock
		while ( ! resizingInProgress.compareAndSet(false, true)) {}
		
		try {
			// check capacity as may have already  been resized to fit our arrayPosition
			if (arrayPosition >= capacity) {
							
				// sleep to allow any threads in CAS loop in increment method to complete
				// resizing should not occur often so will not be too great a performance penalty
				// and it SHOULD mean we keep the array in a consistent state 
				Thread.sleep(100);
				
				System.out.println("resizing...");
			
				// double the required capacity
				capacity = Math.min(arrayPosition * 2, MAX_CAPACITY);
				
				AtomicLongArray newArray = new AtomicLongArray(capacity);
				for (int i = 0, length = array.length() ; i < length ; i++) {
					newArray.set(i, array.get(i));
				}
				array = newArray;
				System.out.println("resizing...DONE - new capacity: " + capacity);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			//unlock
			while ( ! resizingInProgress.compareAndSet(true, false)) {}
		}
	}
}
