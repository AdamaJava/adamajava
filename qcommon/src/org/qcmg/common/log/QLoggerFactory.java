/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.log;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qcmg.common.util.FileUtils;

/**
 * 
 * @author oholmes
 * @version 0.1, 21/03/2011
 */
public class QLoggerFactory {
	
	private static final QLogFormatter QCMG_LOG_FORMATTER = new QLogFormatter();
	public static final Logger ROOT_LOGGER = Logger.getLogger("");
	public static final Level DEFAULT_LEVEL = QLevel.INFO;
	
	// set these up so that the Level class knows about them
	private static final Level DEBUG_LEVEL = QLevel.DEBUG;
	private static final Level TOOL_LEVEL = QLevel.TOOL;
	private static final Level EXEC_LEVEL = QLevel.EXEC;
	
	static {
		//setup root logger
		ROOT_LOGGER.getHandlers()[0].setFormatter(QCMG_LOG_FORMATTER);
	}
	
	/**
	 * 
	 * 
	 * 
	 * @param clazz Class for which a QLogger is required
	 * @param logFile String containing the path to the log file to be used by a 
	 * new {@link java.util.logging.FileHandler} object that will be associated with the parent logger
	 * @param logLevel {@link org.qcmg.common.log.QLevel} logging level that will be enabled (and all levels upwards from this value)
	 * @see java.util.logging.Logger#getLogger(String)
	 * @return {@link QLogger} logger for this class that will output to the specified file, at the specified level
	 */
	public synchronized static QLogger getLogger(Class<?> clazz, String logFile, String logLevel) {
		if (null == clazz) throw new IllegalArgumentException("Null class passed to QLoggerFactory");
		
		Logger logger = Logger.getLogger(clazz.getName());
		logger.setUseParentHandlers(true);
		
		if (null != logFile) setupParentLogger(logFile, logLevel);
		
		return new QLogger(logger);
	}
	
	/**
	 * Calls <CODE>QLoggerFactory.getLogger(Class clazz, String logFile, String logLevel)</CODE> 
	 * with <CODE>null</CODE> values for the <CODE>logFile</CODE> and <CODE>logLevel</CODE> parameters
	 * <p>
	 * @param clazz Clas
	 * @return org.qcmg.common.log.QLogger
	 * @see QLoggerFactory#getLogger(Class, String, String)
	 */
	public synchronized static QLogger getLogger(Class<?> clazz) {
		return getLogger(clazz, null, null);
	}
	
	private static void setupParentLogger(String logFile, String logLevel) {
		if (FileUtils.canFileBeWrittenTo(logFile)) {
		
			FileHandler fh = null;
			try {
				fh = new FileHandler(logFile);
			} catch (SecurityException e) {
				System.err.println("SecurityException caught whilst attempting to setup logging: " + logFile);
				throw new IllegalArgumentException("Invalid logFile parameter passed to QLoggerFactory: " + logFile);
			} catch (IOException e) {
				System.err.println("IOException caught whilst attempting to setup logging: " + logFile);
				throw new IllegalArgumentException("Invalid logFile parameter passed to QLoggerFactory: " + logFile);
			}
			
			fh.setFormatter(QCMG_LOG_FORMATTER);
			Level level = null != logLevel ? QLevel.parse(logLevel) : DEFAULT_LEVEL;
			fh.setLevel(level);
			ROOT_LOGGER.addHandler(fh);
			ROOT_LOGGER.setLevel(level);
			
		} else {
			System.err.println("Can't write to log file: " + logFile);
			throw new IllegalArgumentException("Invalid logFile parameter passed to QLoggerFactory: " + logFile);
		}
	}
	
	static void reset() {
		// used by test classes to reset root logger to initial state
		ROOT_LOGGER.getHandlers()[0].setFormatter(QCMG_LOG_FORMATTER);
		if (ROOT_LOGGER.getHandlers().length > 1) {
			int i = 0;
			for (java.util.logging.Handler h : ROOT_LOGGER.getHandlers()) {
				if (i++ > 0)
					ROOT_LOGGER.removeHandler(h);
			}
		}
	}
}
