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
	
	public long getNumber() {
        return num.get();
    }
}
