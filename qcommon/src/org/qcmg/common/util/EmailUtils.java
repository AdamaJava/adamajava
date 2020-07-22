/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.io.IOException;
import java.util.Arrays;

import org.qcmg.common.log.QLogger;

public class EmailUtils {
	
	public static int sendEmail(String subject, String body, String recipients, QLogger logger) throws IOException, InterruptedException {
		
		String[] cmd = { "/bin/bash", "-c", "echo \"" + body + "\" | mail -s " + subject + " " + recipients};
		if (logger != null) logger.debug(Arrays.deepToString(cmd));
		
		Process p = new ProcessBuilder(cmd).start();
		int emalExitStatus = p.waitFor();
		
		byte[] errorStream = new byte[1024];
		java.io.InputStream isError = p.getErrorStream();
		int size = isError.read(errorStream);
		
		String errorMessage = null; 
		if(size > 0) {
			errorMessage = new String(errorStream, "UTF-8");
		}
		if (logger != null) logger.info("email sending exit status: " + emalExitStatus + ", msg: " + errorMessage);
		
		return emalExitStatus;
	}

}
