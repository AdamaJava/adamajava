package org.qcmg.snp;

import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class TestVcfPipeline extends Pipeline {
	
	static VcfHeader controlVcfHeader;
	VcfHeader testVcfHeader;
	String controlVcfFile, testVcfFile;
	
	public TestVcfPipeline() {
		this(false);
	}
	public TestVcfPipeline(boolean singleSampleMode) {
		super(new QExec("qSNP","TEST",new String[] {}), singleSampleMode);
	}
	
	public void setTestVcfHeader(VcfHeader h) {
		this.testVcfHeader = h;
	}

	@Override
	VcfHeader getExistingVCFHeaderDetails()  {
		VcfHeader existingHeader = new VcfHeader();
		
		if ( ! singleSampleMode) {
			for (VcfHeaderRecord rec : controlVcfHeader.getInfoRecords()) {
				existingHeader.addOrReplace(rec);
			}
			for (VcfHeaderRecord rec : controlVcfHeader.getFormatRecords()) {
				existingHeader.addOrReplace(rec);
			}
			for (VcfHeaderRecord rec : controlVcfHeader.getFilterRecords()) {
				existingHeader.addOrReplace(rec);
			}
			
			// add in the vcf filename, gatk version and the uuid
			existingHeader.addOrReplace( VcfHeaderUtils.STANDARD_CONTROL_VCF + Constants.EQ + controlVcfFile);
			existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_CONTROL_VCF_UUID + Constants.EQ + VcfHeaderUtils.getUUIDFromHeaderLine(controlVcfHeader.getUUID()));
			existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_CONTROL_VCF_GATK_VER + Constants.EQ + VcfHeaderUtils.getGATKVersionFromHeaderLine(controlVcfHeader));
			
		}
		
		for (VcfHeaderRecord rec : testVcfHeader.getInfoRecords()) {
			existingHeader.addOrReplace(rec);
		}
		for (VcfHeaderRecord rec : testVcfHeader.getFormatRecords()) {
			existingHeader.addOrReplace(rec);
		}
		for (VcfHeaderRecord rec : testVcfHeader.getFilterRecords()) {
			existingHeader.addOrReplace(rec);
		}
		// add in the vcf filename, gatk version and the uuid
		existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_TEST_VCF + Constants.EQ + testVcfFile);
		existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_TEST_VCF_UUID + Constants.EQ + VcfHeaderUtils.getUUIDFromHeaderLine(testVcfHeader.getUUID()));
		existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_TEST_VCF_GATK_VER + Constants.EQ + VcfHeaderUtils.getGATKVersionFromHeaderLine(testVcfHeader));
		
		// override this if dealing with input VCFs and the existing headers are to be kept
		return existingHeader;
	}

}
