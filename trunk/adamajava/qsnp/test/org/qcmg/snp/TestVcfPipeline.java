package org.qcmg.snp;

import java.util.List;

import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.pileup.QSnpRecord;

public class TestVcfPipeline extends Pipeline {
	
	static VcfHeader controlVcfHeader;
	static VcfHeader testVcfHeader;
	String controlVcfFile, testVcfFile;
	
	public TestVcfPipeline() {
		this(false);
	}
	public TestVcfPipeline(boolean singleSampleMode) {
		super(new QExec("qSNP","TEST",new String[] {}), singleSampleMode);
	}

	@Override
	protected String getFormattedRecord(final QSnpRecord record, final String ensemblChr) {
		throw new UnsupportedOperationException("Test class - do not use");
	}

	@Override
	protected String getOutputHeader(final boolean isSomatic) {
		throw new UnsupportedOperationException("Test class - do not use");
	}
	
	QSnpRecord getQSnpRecord(QSnpRecord normal, QSnpRecord tumour) {
		QSnpRecord qpr = null;
		
		//TODO need to merge the underlying VcfRecords should both exist
		
		if (null != normal && null != tumour) {
			// create new VcfRecord
			VcfRecord mergedVcf = normal.getVcfRecord();
			// add filter and format fields
			mergedVcf.addFilter(tumour.getVcfRecord().getFilter());
			List<String> formatFields = mergedVcf.getFormatFields();
			formatFields.add(tumour.getVcfRecord().getFormatFields().get(1));
			mergedVcf.setFormatFields(formatFields);
			
			// create new QSnpRecord with the mergedVcf details
			qpr = new QSnpRecord(mergedVcf);
			qpr.setNormalNucleotides(normal.getNormalNucleotides());
			qpr.setNormalGenotype(normal.getNormalGenotype());
			qpr.setTumourNucleotides(tumour.getTumourNucleotides());
			qpr.setTumourGenotype(tumour.getTumourGenotype());
			
			
		} else if (null != normal) {
			qpr = normal;
			// need to add a format field entry for the empty tumoursample
			VcfUtils.addMissingDataToFormatFields(qpr.getVcfRecord(), 2);
		} else {
			// tumour only
			qpr = tumour;
			VcfUtils.addMissingDataToFormatFields(qpr.getVcfRecord(), 1);
		}
		
		
		
//		if (null != normal) {
//			qpr.setRef(normal.getRef());
//			qpr.setNormalGenotype(normal.getGenotypeEnum());
//			qpr.setAnnotation(normal.getAnnotation());
			// tumour fields
//			qpr.setTumourGenotype(null == tumour ? null : tumour.getTumourGenotype());
//			qpr.setTumourCount(null == tumour ? 0 :  VcfUtils.getDPFromFormatField(tumour.getVcfRecord().getFormatFields().get(1)));
			
//		} else if (null != tumour) {
//			qpr = new QSnpRecord(tumour.getChromosome(), tumour.getPosition(), tumour.getRef());
//			qpr.setRef(tumour.getRef());
//			qpr.setTumourGenotype(tumour.getGenotypeEnum());
//			qpr.setTumourCount(VcfUtils.getDPFromFormatField(tumour.getGenotype()));
//		}
		
		qpr.setId(++mutationId);
		return qpr;
	}
	
	@Override
	VcfHeader getExistingVCFHeaderDetails()  {
		VcfHeader existingHeader = new VcfHeader();
		
		if ( ! singleSampleMode) {
			for (VcfHeader.Record rec : controlVcfHeader.getInfoRecords().values()) {
				existingHeader.addInfo(rec);
			}
			for (VcfHeader.Record rec : controlVcfHeader.getFormatRecords().values()) {
				existingHeader.addFormat(rec);
			}
			for (VcfHeader.Record rec : controlVcfHeader.getFilterRecords().values()) {
				existingHeader.addFilter(rec);
			}
			
			// add in the vcf filename, gatk version and the uuid
			existingHeader.parseHeaderLine(VcfHeaderUtils.STANDARD_CONTROL_VCF + Constants.EQ + controlVcfFile);
			existingHeader.parseHeaderLine(VcfHeaderUtils.STANDARD_CONTROL_VCF_UUID + Constants.EQ + VcfHeaderUtils.getUUIDFromHeaderLine(controlVcfHeader.getUUID()));
			existingHeader.parseHeaderLine(VcfHeaderUtils.STANDARD_CONTROL_VCF_GATK_VER + Constants.EQ + VcfHeaderUtils.getGATKVersionFromHeaderLine(controlVcfHeader));
			
		}
		
		for (VcfHeader.Record rec : testVcfHeader.getInfoRecords().values()) {
			existingHeader.addInfo(rec);
		}
		for (VcfHeader.Record rec : testVcfHeader.getFormatRecords().values()) {
			existingHeader.addFormat(rec);
		}
		for (VcfHeader.Record rec : testVcfHeader.getFilterRecords().values()) {
			existingHeader.addFilter(rec);
		}
		// add in the vcf filename, gatk version and the uuid
		existingHeader.parseHeaderLine(VcfHeaderUtils.STANDARD_TEST_VCF + Constants.EQ + testVcfFile);
		existingHeader.parseHeaderLine(VcfHeaderUtils.STANDARD_TEST_VCF_UUID + Constants.EQ + VcfHeaderUtils.getUUIDFromHeaderLine(testVcfHeader.getUUID()));
		existingHeader.parseHeaderLine(VcfHeaderUtils.STANDARD_TEST_VCF_GATK_VER + Constants.EQ + VcfHeaderUtils.getGATKVersionFromHeaderLine(testVcfHeader));
		
		// override this if dealing with input VCFs and the existing headers are to be kept
		return existingHeader;
	}

}
