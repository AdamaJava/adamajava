package org.qcmg.snp;

import org.qcmg.common.meta.QExec;
import org.qcmg.pileup.QSnpRecord;

public class TestPipeline extends Pipeline {
	
	public TestPipeline() {
		super(new QExec("qSNP","TEST",new String[] {}), false);
	}

	@Override
	protected String getFormattedRecord(final QSnpRecord record, final String ensemblChr) {
		throw new UnsupportedOperationException("Test class - do not use");
	}

	@Override
	protected String getOutputHeader(final boolean isSomatic) {
		throw new UnsupportedOperationException("Test class - do not use");
	}

}
