/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
  * @deprecated since it is no longer used.
 */

package org.qcmg.unused.simple;

import java.io.InputStream;

import org.qcmg.qmule.record.AbstractRecordIterator;

@Deprecated
public class SimpleRecordIterator extends AbstractRecordIterator {

	public SimpleRecordIterator(InputStream stream) throws Exception{
		super(stream);
	}

	@Override
	protected void readNext() throws Exception {
//		try {
			next = SimpleSerializer.nextRecord(reader);
//		} catch (Exception ex) {
//			next = null;
//			throw ex;
//		}
	}

}
