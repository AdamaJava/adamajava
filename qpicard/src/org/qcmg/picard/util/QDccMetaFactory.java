/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import net.sf.samtools.SAMFileHeader;

import org.qcmg.common.meta.KeyValue;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.meta.QLimsMeta;
import org.qcmg.common.string.StringUtils;

public class QDccMetaFactory {
	
	public static QDccMeta getDccMeta(QExec qexec, SAMFileHeader controlHeader, SAMFileHeader analysedHeader, String tool) throws Exception {
		
		String controlCommentLine = QLimsMetaFactory.getQCMGCommentLine(controlHeader);
		String analysedCommentLine = QLimsMetaFactory.getQCMGCommentLine(analysedHeader);
		
		String controlSampleId = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.SAMPLE);
		String analysisSampleId = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.SAMPLE);
		
		String controlAssemblyVersion = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.ASSEMBLY_VERSION);
		String analysisAssemblyVersion = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.ASSEMBLY_VERSION);
		
		String controlPlatform = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.PLATFORM);
		String analysisPlatform = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.PLATFORM);
		
		String controlProtocol= StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.LIBRARY_PROTOCOL);
		String analysisProtocol = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.LIBRARY_PROTOCOL);
		
		String controlCaptureKit= StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.CAPTURE_KIT);
		String analysisCaptureKit = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.CAPTURE_KIT);
		
		String controlBaseCallingAlgorithm = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.BASE_CALLING_ALGORITHM);
		String analysisBaseCallingAlgorithm = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.BASE_CALLING_ALGORITHM);
		
		String controlAlignmentAlgorithm = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.ALIGNMENT_ALGORITHM);
		String analysisAlignmentAlgorithm = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.ALIGNMENT_ALGORITHM);
		
//		String controlVariationCallingAlgorithm = getValueFromKey(controlCommentLine, VARIATION_CALLING_ALGORITHM);
//		String analysisVariationCallingAlgorithm = getValueFromKey(analysedCommentLine, VARIATION_CALLING_ALGORITHM);
		
		String controlDonorId = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.DONOR);
		String analysisDonorId = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.DONOR);
		
		// QC checks
		if ( ! StringUtils.isNullOrEmpty(analysisDonorId) && ! analysisDonorId.equals(controlDonorId)) {
			throw new Exception("Donors don't match! Control: " + controlDonorId + ", Analysis: " + analysisDonorId);
		}
		
		QDccMeta dccMeta = new QDccMeta(qexec.getUuid().getValue(), analysisSampleId, controlSampleId, analysisAssemblyVersion, 
				analysisPlatform, StringUtils.getJoinedString(analysisProtocol, analysisCaptureKit), analysisBaseCallingAlgorithm, analysisAlignmentAlgorithm, tool, analysisDonorId);
		
		return dccMeta;
	}
	
	public static QDccMeta getDccMeta(String uuid, SAMFileHeader controlHeader, SAMFileHeader analysedHeader, String tool) throws Exception {
		
		String controlCommentLine = QLimsMetaFactory.getQCMGCommentLine(controlHeader);
		String analysedCommentLine = QLimsMetaFactory.getQCMGCommentLine(analysedHeader);
		
		String controlSampleId = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.SAMPLE);
		String analysisSampleId = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.SAMPLE);
		
		String controlAssemblyVersion = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.ASSEMBLY_VERSION);
		String analysisAssemblyVersion = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.ASSEMBLY_VERSION);
		
		String controlPlatform = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.PLATFORM);
		String analysisPlatform = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.PLATFORM);
		
		String controlProtocol= StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.LIBRARY_PROTOCOL);
		String analysisProtocol = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.LIBRARY_PROTOCOL);
		
		String controlCaptureKit= StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.CAPTURE_KIT);
		String analysisCaptureKit = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.CAPTURE_KIT);
		
		String controlBaseCallingAlgorithm = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.BASE_CALLING_ALGORITHM);
		String analysisBaseCallingAlgorithm = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.BASE_CALLING_ALGORITHM);
		
		String controlAlignmentAlgorithm = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.ALIGNMENT_ALGORITHM);
		String analysisAlignmentAlgorithm = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.ALIGNMENT_ALGORITHM);
		
//		String controlVariationCallingAlgorithm = getValueFromKey(controlCommentLine, VARIATION_CALLING_ALGORITHM);
//		String analysisVariationCallingAlgorithm = getValueFromKey(analysedCommentLine, VARIATION_CALLING_ALGORITHM);
		
		String controlDonorId = StringUtils.getValueFromKey(controlCommentLine, QLimsMeta.DONOR);
		String analysisDonorId = StringUtils.getValueFromKey(analysedCommentLine, QLimsMeta.DONOR);
		
		// QC checks
		if ( ! StringUtils.isNullOrEmpty(analysisDonorId) && ! analysisDonorId.equals(controlDonorId)) {
			throw new Exception("Donors don't match! Control: " + controlDonorId + ", Analysis: " + analysisDonorId);
		}		
		
		QDccMeta dccMeta = new QDccMeta(uuid, analysisSampleId, controlSampleId, analysisAssemblyVersion, 
				analysisPlatform, StringUtils.getJoinedString(analysisProtocol, analysisCaptureKit), analysisBaseCallingAlgorithm, analysisAlignmentAlgorithm, tool, analysisDonorId);
		
		return dccMeta;
	}
	
	public static QDccMeta getDccMeta(Iterable<String> iter) throws Exception {
		
		String analysisId = null;
		String analyzedSampleId = null;
		String controlSampleId = null;
		String assemblyVersion = null;
		String platfom = null;
		String experimentalProtocol = null;
		String baseCallingAlgorithm = null;
		String alignmentAlgorithm = null;
		String variationCallingAlgorithm = null;
		String donorId = null;
		
		for (String s : iter) {
			
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.ANALYSIS_ID))
				analysisId = s.substring(s.lastIndexOf("\t") + 1);
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.ANALYZED_SAMPLE_ID))
				analyzedSampleId = s.substring(s.lastIndexOf("\t") + 1);
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.MATCHED_SAMPLE_ID))
				controlSampleId = s.substring(s.lastIndexOf("\t") + 1);
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.ASSEMBLY_VERSION))
				assemblyVersion = s.substring(s.lastIndexOf("\t") + 1);
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.PLATFORM))
				platfom = s.substring(s.lastIndexOf("\t") + 1);
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.EXPERIMENTAL_PROTOCOL))
				experimentalProtocol = s.substring(s.lastIndexOf("\t") + 1);
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.BASE_CALLING_ALGORITHM))
				baseCallingAlgorithm = s.substring(s.lastIndexOf("\t") + 1);
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.ALIGNMENT_ALGORITHM))
				alignmentAlgorithm = s.substring(s.lastIndexOf("\t") + 1);
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.VARIATION_CALLING_ALGORITHM))
				variationCallingAlgorithm = s.substring(s.lastIndexOf("\t") + 1);
			if (s.startsWith(KeyValue.Q_DCC_META + KeyValue.TAB + QDccMeta.DONOR_ID))
				donorId = s.substring(s.lastIndexOf("\t") + 1	);
		}
		
		QDccMeta dccMeta = new QDccMeta(analysisId, analyzedSampleId, controlSampleId, assemblyVersion, 
				platfom, experimentalProtocol, baseCallingAlgorithm, alignmentAlgorithm, variationCallingAlgorithm, donorId);
		
		return dccMeta;
	}
	
}
