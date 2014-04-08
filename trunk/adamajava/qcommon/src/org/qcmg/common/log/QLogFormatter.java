/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.qcmg.common.date.DateUtils;

public class QLogFormatter extends Formatter{
	private static final String LINE_SEP = System.getProperty("line.separator");

	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(DateUtils.getCurrentTimeAsString());
		sb.append(" [");
		sb.append(Thread.currentThread().getName());
		sb.append("] ");
		sb.append(record.getLevel());
		sb.append(" ");
		sb.append(record.getLoggerName());
		sb.append(" - ");
		sb.append(record.getMessage());
		
		Throwable t = record.getThrown();
		if (null != t) {
			sb.append(LINE_SEP);
//			sb.append(" - ");
			sb.append(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				sb.append(LINE_SEP);
				sb.append("\t" + ste.toString());
			}
		}
		
		sb.append(LINE_SEP);
		return sb.toString();
	}

}
