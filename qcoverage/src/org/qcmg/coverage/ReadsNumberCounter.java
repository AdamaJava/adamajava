/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.util.concurrent.atomic.AtomicLong;

public class ReadsNumberCounter {
	final AtomicLong num = new AtomicLong(); 

	public void increment(){
		num.incrementAndGet();
	}

	/**
	 * Adds the given number to the counter.
	 * <p>
	 * Used to add the counts gathered whilst processing a job to the total, without
	 * having to update the shared counter for every record read.
	 *
	 * @param number number of records to add
	 */
	public void add(final long number) {
		num.addAndGet(number);
	}
	
	public long getNumber() {
        return num.get();
    }
}
