/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;

import java.text.SimpleDateFormat;
import java.util.UUID;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;

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
	 
}
