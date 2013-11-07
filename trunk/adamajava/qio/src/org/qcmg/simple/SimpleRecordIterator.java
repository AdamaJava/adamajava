package org.qcmg.simple;

import java.io.InputStream;

import org.qcmg.record.AbstractRecordIterator;

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
