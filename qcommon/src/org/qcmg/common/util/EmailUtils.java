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
		isError.read(errorStream);
		
		String errorMessage = new String(errorStream);
		if (logger != null) logger.info("Email sending exit status: " + emalExitStatus + ", msg: " + errorMessage);
		
		return emalExitStatus;
	}

}
