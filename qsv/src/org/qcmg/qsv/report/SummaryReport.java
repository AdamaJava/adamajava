/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.qcmg.qsv.Options;
import org.qcmg.qsv.QSV;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.annotate.RunTypeRecord;

public class SummaryReport extends QSVReport {

	private String runTime;
	private final Date runDate;
	private QSVParameters tumorParam;
	private QSVParameters normalParam;
	private Options options;
	private final boolean isQCMG;
	private int somaticCount;
	private int germlineCount;
	private int normalGermlineCount;
	private final String analysisId;
	private static String NEWLINE = System.getProperty("line.separator");

	public SummaryReport(File file, Date runDate, String analysisId, boolean isQCMG) {
		super(file);
		this.runDate = runDate;
		this.analysisId = analysisId;
		this.isQCMG = isQCMG;
	}

	@Override
	public String getHeader() {
		return "qSV analysis performed with version: " + QSV.class.getPackage().getImplementationVersion() + NEWLINE;
	}

	@Override
	public void writeReport() throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		if (options.isQCMG()) {
			writeQCMGReport(writer);
		} else {
			
			writeRunInfo(writer);
			writer.write("GENERAL PARAMETERS:" + NEWLINE);
			writer.write("Analysis ID: " + analysisId + NEWLINE);			
			writer.write("Sample: " + options.getSampleName() + NEWLINE);			
			writer.write("Preprocessing option: " + options.getPreprocessMode() + NEWLINE);
			writer.write("SV analysis option: " + options.getAnalysisMode() + NEWLINE);
			
			writePairOptions(writer);
			writeClipOptions(writer);

			writeParameters(writer, "TEST", "CONTROL");
						
			
		}
		writer.close();
	}

	private void writeParameters(BufferedWriter writer, String test, String control) throws IOException {
		writer.write(writeSampleParams(tumorParam, test, "input"));
		writer.write(NEWLINE);
		if (normalParam != null) {
			writer.write(writeSampleParams(normalParam, control, "comparison"));
		}
		writer.write(NEWLINE);
		
		if (normalParam != null) {
			writer.write("TOTAL STRUCTURAL VARIANTS" + NEWLINE);
			writer.write("Somatic: " + somaticCount + NEWLINE);
			writer.write("Germline: " + germlineCount + NEWLINE);
			if (this.isQCMG) {
				writer.write("Normal germline: " + normalGermlineCount + NEWLINE);
			}
		} else {
			writer.write("TOTAL STRUCTURAL VARIANTS" + NEWLINE);
			writer.write(somaticCount + NEWLINE);
		}	
		
		
	}

	private void writeQCMGReport(BufferedWriter writer) throws IOException {
		
		writeRunInfo(writer);		
		
		writer.write("GENERAL PARAMETERS:" + NEWLINE);
		writer.write("Sample: " + options.getSampleName() + NEWLINE);
		writer.write("Analysis ID: " + analysisId + NEWLINE);
		writer.write("Preprocessing option: " + options.getPreprocessMode() + NEWLINE);
		writer.write("SV analysis option: " + options.getAnalysisMode() + NEWLINE + NEWLINE);		
		
		writePairOptions(writer);
		writeClipOptions(writer);
		writeParameters(writer, "TUMOUR", "NORMAL");

	}

	private void writeRunInfo(BufferedWriter writer) throws IOException {
		writer.write("RUN INFORMATION:" + NEWLINE);
		writer.write(getHeader());		
		writer.write("Analysis commenced: " + runDate + NEWLINE);
		writer.write("Analysis completed: " + new Date() + NEWLINE);
		writer.write(runTime + NEWLINE + NEWLINE);		
	}

	private void writeClipOptions(BufferedWriter writer) throws IOException {
		if (options.runClipPreprocess() || options.runClipAnalysis()) {
			writer.write("Soft clip filtering query: " + options.getClipQuery() + NEWLINE);
			writer.write(("Soft clip reads size: " + options.getClipSize()) + NEWLINE);        	
			writer.write(("Soft clip consensus length: " + options.getConsensusLength()) + NEWLINE);
			writer.write("Path to blat: " + options.getBlatPath() + NEWLINE);
			writer.write("BLAT Server: " + options.getBlatServer() + NEWLINE);			
			writer.write("BLAT Port: " + options.getBlatPort() + NEWLINE + NEWLINE);
		}
		
	}

	private void writePairOptions(BufferedWriter writer) throws IOException {
		if (options.runPairPreprocess() || options.runPairAnalysis()) {			
			writer.write("Pairing type: " + options.getPairingType() + NEWLINE);
			writer.write("Mapper: " + options.getMapper() + NEWLINE);
			writer.write("Cluster size: " + options.getClusterSize() + NEWLINE);
			writer.write("Filter size: " + options.getFilterSize() + NEWLINE);
			writer.write("Discordant pair filtering query: " + options.getPairQuery() + NEWLINE);			
		}
		
	}

	private String writeSampleParams(QSVParameters params, String titleType, String type) {
		StringBuilder builder = new StringBuilder();
		builder.append(titleType + " PARAMETERS:" + NEWLINE);
		builder.append("Sample abbreviation: " + params.getFindType() + NEWLINE);
		builder.append("Sample ID: " + params.getSampleId() + NEWLINE);
		builder.append("Input BAM file: " + params.getInputBamFile() + NEWLINE);
		builder.append("Discordant pair filtered "+type+" BAM file: " + params.getFilteredBamFile() + NEWLINE);
		builder.append("Min lower insert size: " + params.getLowerInsertSize() + NEWLINE);
		builder.append("Max upper insert size: " + params.getUpperInsertSize() + NEWLINE);	
		
		for (RunTypeRecord r: params.getSequencingRuns()) {
			builder.append("Read group insert size: "  + r.toString() + NEWLINE);
		}
		
		return builder.toString();
	}

	public void summarise(String runTime, QSVParameters tumor, QSVParameters normal, Options options, int somaticCount, int germlineCount, int normalGermlineCount) throws Exception {
		this.runTime = runTime;
		this.tumorParam = tumor;
		this.normalParam = normal;
		this.options = options;
		this.somaticCount = somaticCount;
		this.germlineCount = germlineCount;
		this.normalGermlineCount = normalGermlineCount;
		writeReport();
	}

	@Override
	public void writeHeader() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
