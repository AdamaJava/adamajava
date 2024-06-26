/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.meta;

public class QDccMeta {
	
	public static final String ANALYSIS_ID = "analysisId";
	public static final String ANALYZED_SAMPLE_ID = "analyzed_sample_id";
	public static final String MATCHED_SAMPLE_ID = "matched_sample_id";
	public static final String ASSEMBLY_VERSION = "assembly_version";
	public static final String PLATFORM = "platform";
	public static final String EXPERIMENTAL_PROTOCOL = "experimental_protocol";
	public static final String BASE_CALLING_ALGORITHM = "base_calling_algorithm";
	public static final String ALIGNMENT_ALGORITHM = "alignment_algorithm";
	public static final String VARIATION_CALLING_ALGORITHM = "variation_calling_algorithm";
	public static final String DONOR_ID = "donor_id";
	
	private final KeyValue uuid;
	private final KeyValue analysisId;
	private final KeyValue analyzedSampleId;
	private final KeyValue matchedSampleId;
	private final KeyValue assemblyVersion;
	private final KeyValue platform;
	private final KeyValue experimentalProtocol;
	private final KeyValue baseCallingAlgorithm;
	private final KeyValue alignmentAlgorithm;
	private final KeyValue variationCallingAlgorithm;
	private final KeyValue donorId;
	
	public QDccMeta(String uuid, String analyzed_sample_id, String matched_sample_id, 
			String assembly_version, String platform, String experimental_protocol, String base_calling_algorithm,
			String alignment_algorithm, String variation_calling_algorithm, String donor_id) {
		
		this.uuid = new KeyValue("Uuid", uuid);
		this.analysisId = new KeyValue(ANALYSIS_ID, uuid);
		this.analyzedSampleId = new KeyValue(ANALYZED_SAMPLE_ID, analyzed_sample_id);
		this.matchedSampleId = new KeyValue(MATCHED_SAMPLE_ID, matched_sample_id);
		this.assemblyVersion = new KeyValue(ASSEMBLY_VERSION, assembly_version);
		this.platform = new KeyValue(PLATFORM, platform );
		this.experimentalProtocol = new KeyValue(EXPERIMENTAL_PROTOCOL, experimental_protocol);
		this.baseCallingAlgorithm = new KeyValue(BASE_CALLING_ALGORITHM, base_calling_algorithm);
		this.alignmentAlgorithm = new KeyValue(ALIGNMENT_ALGORITHM, alignment_algorithm);
		this.variationCallingAlgorithm = new KeyValue(VARIATION_CALLING_ALGORITHM, variation_calling_algorithm);
		this.donorId = new KeyValue(DONOR_ID, donor_id);
	}
	
	public KeyValue getUuid() {
		return uuid;
	}

	public KeyValue getAnalysisId() {
		return analysisId;
	}

	public KeyValue getAnalyzedSampleId() {
		return analyzedSampleId;
	}

	public KeyValue getMatchedSampleId() {
		return matchedSampleId;
	}

	public KeyValue getPlatform() {
		return platform;
	}

	public KeyValue getExperimentalProtocol() {
		return experimentalProtocol;
	}

	public KeyValue getAlignmentAlgorithm() {
		return alignmentAlgorithm;
	}

	public KeyValue getDonorId() {
		return donorId;
	}
	
	public String getDCCMetaDataToString() {
        return analysisId.toDCCMetaString() +
                analyzedSampleId.toDCCMetaString() +
                matchedSampleId.toDCCMetaString() +
                assemblyVersion.toDCCMetaString() +
                platform.toDCCMetaString() +
                experimentalProtocol.toDCCMetaString() +
                baseCallingAlgorithm.toDCCMetaString() +
                alignmentAlgorithm.toDCCMetaString() +
                variationCallingAlgorithm.toDCCMetaString() +
                donorId.toDCCMetaString();
	}
}
