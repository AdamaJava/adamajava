package org.qcmg.gff;

import java.io.InputStream;

import org.qcmg.record.AbstractRecordIterator;

public class GFFIterator extends AbstractRecordIterator {

	public GFFIterator(InputStream stream) throws Exception {
		super(stream);
	}

	@Override
	protected void readNext() {
//		try {
			next = GFFSerializer.nextRecord(reader);
//		} catch (Exception ex) {
//			next = null;
//		}
	}

}
