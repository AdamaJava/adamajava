package org.qcmg.illumina;

import java.io.InputStream;

import org.qcmg.record.AbstractRecordIterator;

public class IlluminaRecordIterator extends AbstractRecordIterator {

	public IlluminaRecordIterator(InputStream stream) throws Exception {
		super(stream);
	}

	@Override
	protected void readNext() throws Exception {
		next = IlluminaSerializer.nextRecord(reader);
	}

}
