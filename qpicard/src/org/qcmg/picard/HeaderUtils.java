/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.util.UUID;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMSequenceRecord;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.picard.util.SAMReadGroupRecordUtils;

public final class HeaderUtils {
	public static SAMProgramRecord addProgramRecord(SAMFileHeader header,
			String programName, String programVersion, String commandLine) {
		SAMProgramRecord pr = createProgramRecord(programName, programVersion,
				commandLine);
		header.addProgramRecord(pr);
		return pr;
	}

	public static SAMProgramRecord addProgramRecord(SAMFileHeader header,
			String programName, String programVersion) {
		SAMProgramRecord pr = createProgramRecord(programName, programVersion);
		header.addProgramRecord(pr);
		return pr;
	}

	private static SAMProgramRecord createProgramRecord(String programName,
			String programVersion, String commandLine) {
		SAMProgramRecord pr = createProgramRecord(programName, programVersion);
		pr.setCommandLine(commandLine);
		return pr;
	}

	private static SAMProgramRecord createProgramRecord(String programName,
			String programVersion) {
		String programId = UUID.randomUUID().toString();
		SAMProgramRecord pr = new SAMProgramRecord(programId);
		pr.setProgramName(programName);
		pr.setProgramVersion(programVersion);
		return pr;
	}
	
	public static void addComment(SAMFileHeader header,
			String programName, String programVersion, String comment){
		String str = String.format("PN:%s\tVN:%s\t%s", programName, programVersion,comment);
		header.addComment(str);		 
	}
	public static void addComment(SAMFileHeader header, String comment){
		 
		header.addComment(comment);		 
	}
	
	
	public static String getPGString(SAMProgramRecord rec) {
		if (null == rec) throw new IllegalArgumentException("null SAMProgramRecord passed to getPGString");
		
		StringBuilder sb = new StringBuilder(Constants.PROGRAM_PREFIX);
		if ( ! StringUtils.isNullOrEmpty(rec.getId()))
			sb.append(StringUtils.getTabAndString(SAMProgramRecord.PROGRAM_GROUP_ID_TAG)).append(Constants.COLON).append(rec.getId());
		if ( ! StringUtils.isNullOrEmpty(rec.getProgramName()))
			sb.append(StringUtils.getTabAndString(SAMProgramRecord.PROGRAM_NAME_TAG)).append(Constants.COLON).append(rec.getProgramName());
		if ( ! StringUtils.isNullOrEmpty(rec.getProgramVersion()))
			sb.append(StringUtils.getTabAndString(SAMProgramRecord.PROGRAM_VERSION_TAG)).append(Constants.COLON).append(rec.getProgramVersion());
		if ( ! StringUtils.isNullOrEmpty(rec.getCommandLine()))
			sb.append(StringUtils.getTabAndString(SAMProgramRecord.COMMAND_LINE_TAG)).append(Constants.COLON).append(rec.getCommandLine());
		if (null != (rec.getPreviousProgramGroupId()))
			sb.append(StringUtils.getTabAndString(SAMProgramRecord.PREVIOUS_PROGRAM_GROUP_ID_TAG)).append(Constants.COLON).append(rec.getPreviousProgramGroupId());
		
		return sb.toString();
	}
	
	
	/**
	 * Picard's getTextHeader returns null if there is more than 1MB of blurb in the header.
	 * This method attempts to recreate the header text, and may truncate lines over 2k in length
	 * 
	 * @param header
	 * @return
	 */
	public static String getHeaderStringFromHeader(SAMFileHeader header) {
		String headerText = header.getTextHeader();
		if (null == headerText) {
			StringBuilder sb = new StringBuilder();
			// header line
			sb.append(Constants.HEADER_PREFIX).append(Constants.TAB);
			sb.append(SAMFileHeader.VERSION_TAG).append(Constants.COLON);
			sb.append(header.getVersion()).append(Constants.TAB);
			sb.append(SAMFileHeader.GROUP_ORDER_TAG).append(Constants.COLON);
			sb.append(header.getGroupOrder().toString()).append(Constants.TAB);
			sb.append(SAMFileHeader.SORT_ORDER_TAG).append(Constants.COLON);
			sb.append(header.getSortOrder().toString()).append(Constants.NL);
			
			// sequence next
			for (SAMSequenceRecord ssr :header.getSequenceDictionary().getSequences()) {
				sb.append(Constants.SEQUENCE_PREFIX).append(Constants.TAB);
				sb.append(SAMSequenceRecord.SEQUENCE_NAME_TAG).append(Constants.COLON);
				sb.append(ssr.getSequenceName()).append(Constants.TAB);
				sb.append(SAMSequenceRecord.SEQUENCE_LENGTH_TAG).append(Constants.COLON);
				sb.append(ssr.getSequenceLength()).append(Constants.NL);
			}
			
			// readgroup next
			for (SAMReadGroupRecord srgr : header.getReadGroups()) {
				sb.append(SAMReadGroupRecordUtils.getRGString(srgr)).append(Constants.NL);
			}
			
			// program lines now
			for (SAMProgramRecord spr : header.getProgramRecords()) {
				sb.append(getPGString(spr)).append(Constants.NL);
			}
			
			// and finally comments
			for (String co : header.getComments()) {
				sb.append(co).append(Constants.NL);
//				sb.append(Constants.COMMENT_PREFIX).append(Constants.TAB).append(co).append(Constants.NL);
			}
			
			headerText = sb.toString();
		}
		return headerText;
	}
	 
}
