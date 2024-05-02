/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.qsv.util.QSVUtil;

import au.edu.qimr.tiledaligner.PositionChrPositionMap.LongRange;

public class CreateParametersCallable implements Callable<QSVParameters> {
	
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private final Options options;
	private final boolean isTumor;
	private final String resultsDir;
	private final String matePairDir;
	private final String sampleName;
    private int exitStatus;
	private final CountDownLatch latch;
	private final Map<ChrPosition, LongRange> refIndexPositionMap;

	public CreateParametersCallable(CountDownLatch latch,Options options, boolean b, String resultsDir,
			String matePairDir,String sampleName, Map<ChrPosition, LongRange> refIndexPositionMap) {
		this.options = options;
		this.isTumor = b;
		this.resultsDir = resultsDir;
		this.matePairDir = matePairDir;
		this.sampleName = sampleName;
        this.latch = latch;
		this.refIndexPositionMap = refIndexPositionMap;
	}

	@Override
	public QSVParameters call() {
		QSVParameters p = null;
		try {
			p = new QSVParameters(options, isTumor, resultsDir, sampleName, refIndexPositionMap);
		} catch (Exception e) {
			this.exitStatus = 1;
			logger.info(QSVUtil.getStrackTrace(e));
		}
		latch.countDown();
		return p;
	}
	
	public int getExitStatus() {
		return exitStatus;
	}

	public void setExitStatus(int exitStatus) {
		this.exitStatus = exitStatus;
	}
}
