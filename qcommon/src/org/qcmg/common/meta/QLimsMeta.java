package org.qcmg.common.meta;

import org.qcmg.common.string.StringUtils;

public class QLimsMeta {
	
	/*
	 * @CO	CN:QCMG	QN:qlimsmeta	
	 * Aligner=bwa	
	 * Capture Kit=NoCapture	
	 * Donor=ABCD_1234
	 * Failed QC=0	
	 * Library Protocol=Illumina TruSEQ Multiplexed Manual	
	 * Material=1:DNA	Project=cancer	
	 * Reference Genome File=GRCh37_ICGC_standard_v2.fa	
	 * Sample=CANCER_1234	
	 * Sample Code=8:Mouse xenograft derived from tumour	
	 * Sequencing Platform=HiSeq	
	 * Species Reference Genome=Homo sapiens (GRCh37_ICGC_standard_v2)
	 */
	
	public static final String BAM_FILE = "Bam";
	public static final String SAMPLE = "Sample";
	public static final String SAMPLE_CODE = "Sample Code";
	public static final String ASSEMBLY_VERSION = "assembly_version";
	public static final String PLATFORM = "Sequencing Platform";
	public static final String CAPTURE_KIT = "Capture Kit";
	public static final String BASE_CALLING_ALGORITHM = "BaseCallingAlgorithm";
	public static final String ALIGNMENT_ALGORITHM = "Aligner";
	public static final String VARIATION_CALLING_ALGORITHM = "VariationCallingAlgorithm";
	public static final String DONOR = "Donor";
	public static final String FAILED_QC = "Failed QC";
	public static final String LIBRARY_PROTOCOL = "Library Protocol";
	public static final String MATERIAL = "Material";
	public static final String PROJECT = "Project";
	public static final String REFERENCE_FILE = "Reference Genome File";
	public static final String SPECIES_REF_GENOME = "Species Reference Genome";
	
	private final KeyValue bamFile;
	private final KeyValue aligner;
	private final KeyValue captureKit;
	private final KeyValue donor;
	private final KeyValue failedQC;
	private final KeyValue libraryProtocol;
	private final KeyValue material;	
	private final KeyValue project;
	private final KeyValue referenceFile;
	private final KeyValue sample;
	private final KeyValue sampleCode;
	private final KeyValue platform;
	private final KeyValue speciesRefGenome;
	
	private final String type;
	
	public QLimsMeta(String type, String bamFile, String commentHeaderString) {
		this.type = type;
		this.bamFile = new KeyValue(BAM_FILE, bamFile);
		this.aligner = new KeyValue(ALIGNMENT_ALGORITHM, StringUtils.getValueFromKey(commentHeaderString, ALIGNMENT_ALGORITHM));
		this.captureKit = new KeyValue(CAPTURE_KIT, StringUtils.getValueFromKey(commentHeaderString, CAPTURE_KIT));
		this.donor = new KeyValue(DONOR, StringUtils.getValueFromKey(commentHeaderString, DONOR));
		this.failedQC = new KeyValue(FAILED_QC, StringUtils.getValueFromKey(commentHeaderString, FAILED_QC));
		this.libraryProtocol = new KeyValue(LIBRARY_PROTOCOL, StringUtils.getValueFromKey(commentHeaderString, LIBRARY_PROTOCOL));
		this.material = new KeyValue(MATERIAL, StringUtils.getValueFromKey(commentHeaderString, MATERIAL));
		this.project = new KeyValue(PROJECT, StringUtils.getValueFromKey(commentHeaderString, PROJECT));
		this.referenceFile = new KeyValue(REFERENCE_FILE, StringUtils.getValueFromKey(commentHeaderString, REFERENCE_FILE));
		this.sample = new KeyValue(SAMPLE, StringUtils.getValueFromKey(commentHeaderString, SAMPLE));
		this.sampleCode = new KeyValue(SAMPLE_CODE, StringUtils.getValueFromKey(commentHeaderString, SAMPLE_CODE));
		this.platform = new KeyValue(PLATFORM, StringUtils.getValueFromKey(commentHeaderString, PLATFORM));
		this.speciesRefGenome = new KeyValue(SPECIES_REF_GENOME, StringUtils.getValueFromKey(commentHeaderString, SPECIES_REF_GENOME));
	}
	
	public QLimsMeta(String type, String bamFile, String aligner, String captureKit, String donor, String failedQC, 
			String libraryProtocol, String material, String project, String referenceFile, 
			String sample,  String sampleCode ,String platform, String speciesRefGenome) {
		
		this.type = type;
		this.bamFile = new KeyValue(BAM_FILE, bamFile);
		this.aligner = new KeyValue(ALIGNMENT_ALGORITHM, aligner);
		this.captureKit = new KeyValue(CAPTURE_KIT, captureKit);
		this.donor = new KeyValue(DONOR, donor);
		this.failedQC = new KeyValue(FAILED_QC, failedQC);
		this.libraryProtocol = new KeyValue(LIBRARY_PROTOCOL, libraryProtocol);
		this.material = new KeyValue(MATERIAL, material);
		this.project = new KeyValue(PROJECT, project);
		this.referenceFile = new KeyValue(REFERENCE_FILE, referenceFile);
		this.sample = new KeyValue(SAMPLE, sample);
		this.sampleCode = new KeyValue(SAMPLE_CODE, sampleCode);
		this.platform = new KeyValue(PLATFORM, platform);
		this.speciesRefGenome = new KeyValue(SPECIES_REF_GENOME, speciesRefGenome);
	}
	
	public String getDonor() {
		return donor.getValue();
	}
	public String getReferenceFile() {
		return referenceFile.getValue();
	}
	public String getSample() {
		return sample.getValue();
	}
	public String getSampleCode() {
		return sampleCode.getValue();
	}
	public String getPlatform() {
		return platform.getValue();
	}
	public String getAligner() {;
		return aligner.getValue();
	}
	public String getCaptureKit() {;
	return captureKit.getValue();
	}
	
	public String getLimsMetaDataToString() {
		StringBuilder sb = new StringBuilder();
		sb.append(bamFile.toLimsMetaString(type));
		sb.append(aligner.toLimsMetaString(type));
		sb.append(captureKit.toLimsMetaString(type));
		sb.append(donor.toLimsMetaString(type));
		sb.append(failedQC.toLimsMetaString(type));
		sb.append(libraryProtocol.toLimsMetaString(type));
		sb.append(material.toLimsMetaString(type));
		sb.append(project.toLimsMetaString(type));
		sb.append(referenceFile.toLimsMetaString(type));
		sb.append(sample.toLimsMetaString(type));
		sb.append(sampleCode.toLimsMetaString(type));
		sb.append(platform.toLimsMetaString(type));
		sb.append(speciesRefGenome.toLimsMetaString(type));
		return sb.toString();
	}
}
