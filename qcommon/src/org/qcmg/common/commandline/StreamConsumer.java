/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * 
 */
package org.qcmg.common.commandline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

public class StreamConsumer extends Thread {
	final InputStream is;
	String[] lines;

	public StreamConsumer(InputStream is) {
		this.is = is;
	}

	@Override
	public void run() {
		Vector<String> results = new Vector<String>();
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				results.add(line);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		lines = new String[results.size()];
		results.toArray(lines);
	}

	public String[] getLines() {
		return lines;
	}
}