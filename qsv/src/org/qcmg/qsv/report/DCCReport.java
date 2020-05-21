/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.KeyValue;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.meta.QLimsMeta;
import org.qcmg.common.meta.QToolMeta;
import org.qcmg.qsv.Options;
import org.qcmg.qsv.QSVCluster;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

public class DCCReport extends QSVReport {	
	
	private Options options;
	private QSVParameters tumour;
	private QSVParameters normal;
	private String analysisId;
	private List<QSVCluster> qsvRecords;
	private boolean isSingleSided;
	private String tumourFindType;
	private String normalFindType;
	private String platform;
	private QExec exec;
	private QLogger logger =  QLoggerFactory.getLogger(DCCReport.class);
	
	public DCCReport(File file, Date runDate, String analysisId, QSVParameters tumor, QSVParameters normal, Options options, QExec exec) throws IOException {
		super(file);
		this.analysisId = analysisId;
		this.options = options;
		this.tumour = tumor;
		this.normal = normal;
		this.exec = exec;
		writeHeader();
	}

	public DCCReport(File file, List<QSVCluster> records, String tumourFindType, String normalFindType, boolean isSingleSided, String platform) throws IOException {
		super(file);
		this.qsvRecords = records;
		this.isSingleSided = isSingleSided;
		this.tumourFindType = tumourFindType;
		this.normalFindType = normalFindType;
		this.platform = platform;
		writeReport();
	}

	@Override
	public void writeHeader() throws IOException {
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file));) {	
			writeExec(writer);
			writeDCCMeta(writer);
			writeLIMSMeta(writer);
			writeToolSpecific(writer);		
			writer.write(getHeader());
		}
	}

	private void writeLIMSMeta(BufferedWriter writer) throws IOException {
		
		writer.write(new QLimsMeta("TEST", tumour.getInputBamFile().getAbsolutePath(), tumour.getLIMSMeta()).getLimsMetaDataToString());
		if (normal != null) {
			writer.write(new QLimsMeta("CONTROL", normal.getInputBamFile().getAbsolutePath(), normal.getLIMSMeta()).getLimsMetaDataToString());
		}
	}

	private void writeDCCMeta(BufferedWriter writer) throws IOException {
		QDccMeta meta = new QDccMeta(analysisId, options.getInputSampleId(), options.getComparisonSampleId(), options.translateReference(), options.translatePlatform(),
				options.getPairingType(), options.getSequencingPlatform(), options.getMapper(), "qSV", options.getSampleName());
		writer.write(meta.getDCCMetaDataToString());
	}

	private void writeExec(BufferedWriter writer) throws IOException {
		writer.write(exec.getExecMetaDataToString());
	}
	
	private void writeToolSpecific(BufferedWriter writer) throws IOException {
		KeyValue[] array = new KeyValue[6];
		array[0] = new KeyValue("analyzed_sample_isize", tumour.getISizeReportString());
		if (normal != null) {
			array[1] = new KeyValue("matched_sample_isize", normal.getISizeReportString());
		} else {
			array[1] = new KeyValue("matched_sample_isize", "");
		}
		array[2] = new KeyValue("analyzed_sample_file", tumour.getInputBamFile().getAbsolutePath());
		if (normal != null) {
			array[3] = new KeyValue("matched_sample_file", normal.getInputBamFile().getAbsolutePath());
		} else {
			array[3] = new KeyValue("matched_sample_file", "");
		}
		array[4] = new KeyValue("analyzed_sample_code", tumour.getFindType());
		if (normal != null) {
			array[5] = new KeyValue("matched_sample_code", normal.getFindType());
		} else {
			array[5] = new KeyValue("matched_sample_code", "");
		}		
		
		QToolMeta meta = new QToolMeta("qSV", array);
		writer.write(meta.getToolMetaDataToString());
	}

	@Override
	public void writeReport() throws IOException {
		try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
		    	 for (QSVCluster r: qsvRecords) {
		    		 if (r.printRecord(isSingleSided)) {
		    			 String str = r.getDataString("dcc", tumourFindType, normalFindType, true, getValidationPlatform(platform)); 				 
		    			 writer.write(str +  QSVUtil.getNewLine());    			 
		    		 } else {
		    			 logger.info("Not writing cluster to dcc file, cluster confidence: " + r.getConfidenceLevel() + ", " + r.getHeader(""));
		    		 }
		    	 }
		}
	}

	private String getValidationPlatform(String platform) {
		if (platform.equals("solid")) {
			return "4";
		} else if (QSVConstants.DEFAULT_SEQ_PLATFORM.equals(platform)) {
			return"60";
		} else {
			return "-999";
		}
	}

	@Override
	public String getHeader() {
		
		return "analysis_id\tanalyzed_sample_id\tsv_id\tplacement\tannotation\tinterpreted_annotation\tvariant_type\t"
				+ "chr_from\tchr_from_bkpt\tchr_from_strand\tchr_from_range\tchr_from_flanking_seq\tchr_to\tchr_to_bkpt\t"
				+ "chr_to_strand\tchr_to_range\tchr_to_flanking_seq\tmicrohomology_sequence\tnon_templated_sequence\tevidence\t"
				+ "quality_score\tprobability\tzygosity\tvalidation_status\tvalidation_platform\tdb_xref\tnote\tnumber_of_reads\t"
                + "number_of_normal_reads\tnumber_of_low_qual_normal_reads\tnumber_of_tumour_clips_pos1\tnumber_of_tumour_clips_pos2\t"
                + "number_of_normal_clips_pos1\tnumber_of_normal_clips_pos2\tcategory\torientation_category\t"
                + "pairing_category\tevent_notes\tcontig\n";
	}

}
