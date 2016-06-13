/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.meta;

import org.qcmg.common.string.StringUtils;

public class QBamId {
	
	/*
		@CO	CN:QCMG	QN:qbamid	
		uuid=4dc35557-efbc-4fe2-93ec-f09f9c9a6437	
		epoch=1413174467	
		bamname=SmgresLakhaniffpe_PSAR0001_1DNA_7PrimaryTumour_EXTERNPSAR20140514061_Unknown0_NoCapture_Bwa_MiSeq.qxu.bam
	 */
	
	public static final String BAM_NAME = "bamname";
	public static final String EPOCH = "epoch";
	public static final String UUID = "uuid";
	
	private final KeyValue bamName;
	private final KeyValue epoch;
	private final KeyValue uuid;
	
	public QBamId(String bamFile, String commentHeaderString) {
		this.bamName = new KeyValue(BAM_NAME, bamFile);
		this.epoch = new KeyValue(EPOCH, StringUtils.getValueFromKey(commentHeaderString, EPOCH));
		this.uuid = new KeyValue(UUID, StringUtils.getValueFromKey(commentHeaderString, UUID));
	}
	
	public QBamId(String bamFile, String epoch, String uuid) {
		this.bamName = new KeyValue(BAM_NAME, bamFile);
		this.epoch = new KeyValue(EPOCH, epoch);
		this.uuid = new KeyValue(UUID, uuid);
	}
	
	public String getBamName() {
		return bamName.getValue();
	}
	public String getEpoch() {;
		return epoch.getValue();
	}
	public String getUUID() {;
	return uuid.getValue();
	}
	
	public String getBamIdDataToString() {
		StringBuilder sb = new StringBuilder();
		sb.append(bamName.toBamIdString());
//		sb.append(epoch.toBamIdString());
		sb.append(uuid.toBamIdString());
		return sb.toString();
	}
}
