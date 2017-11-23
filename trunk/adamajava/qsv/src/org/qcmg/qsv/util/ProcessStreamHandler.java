/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.util;

import java.io.*;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;


public class ProcessStreamHandler extends Thread {
	InputStream is;
	String type;
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	
	
	public ProcessStreamHandler(InputStream is, String type) {
		this.is = is;
		this.type = type;
	}

	@Override
	public void run() {
		try {

			StringBuilder output = new StringBuilder();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			int count = 0;
			while ((line = br.readLine()) != null) {
				count++;
				output.append(line);
				if (count % 10000 == 0) {
					output = new StringBuilder();
				}
			}
			if (type.equals("ERROR")) {
				if (output.toString().length() > 0) {
					logger.info("Error running BLAT: " + output.toString());		
				}
			}
			
			br.close();			
			isr.close();
			is.close();
			output = null;
		} catch (IOException ioe) {
			ioe.printStackTrace();	
			logger.info("Error running BLAT: " + QSVUtil.getStrackTrace(ioe));	
		}
	}
}
