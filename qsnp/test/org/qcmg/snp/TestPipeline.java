package org.qcmg.snp;

import org.qcmg.common.meta.QExec;

public class TestPipeline extends Pipeline {
	
	public TestPipeline() {
		super(new QExec("qSNP","TEST",new String[] {}), false);
	}


}
