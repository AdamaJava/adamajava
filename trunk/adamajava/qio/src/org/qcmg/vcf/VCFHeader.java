/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.vcf;

import java.util.Iterator;
import java.util.Vector;

public class VCFHeader implements Iterable<String>{
	
	private final Vector<String> records = new Vector<String>();

	public VCFHeader(final Vector<String> headerRecords) {
		for (final String record : headerRecords) {
			records.add(record);
	        }
	 }

	@Override
	public Iterator<String> iterator() {
		return records.iterator();
	}

}
