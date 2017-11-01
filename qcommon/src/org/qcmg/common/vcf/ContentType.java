package org.qcmg.common.vcf;

public enum ContentType {
	SINGLE_CALLER_SINGLE_SAMPLE,				// single format column
	SINGLE_CALLER_MULTIPLE_SAMPLES,			// more than 1 sample column, all with different ids
	MULTIPLE_CALLERS_SINGLE_SAMPLE,			// more than 1 column, all with the same sample id (apart from discriminator eg. _n)
	MULTIPLE_CALLERS_MULTIPLE_SAMPLES,;	// more than 1 column, more than 1 sample id (taking into account discriminator eg. _n)
	
	
	public static boolean multipleCallers(ContentType ct) {
		return ct == MULTIPLE_CALLERS_SINGLE_SAMPLE || ct == MULTIPLE_CALLERS_MULTIPLE_SAMPLES;
	}
	public static boolean multipleSamples(ContentType ct) {
		return ct == SINGLE_CALLER_MULTIPLE_SAMPLES || ct == MULTIPLE_CALLERS_MULTIPLE_SAMPLES;
	}
	
}
