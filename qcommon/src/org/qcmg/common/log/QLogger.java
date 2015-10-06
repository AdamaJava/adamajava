/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.log;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.meta.QExec;

/**
 * QCMG Logger that utilises the <CODE>java.util.logging</CODE> package to
 * provide specific levels and messages as required
 * <p>
 * This class is composed of a {@link Logger} instance that handles all the
 * logging requests. It defines a few methods that differ from Java's logging
 * approach (eg. <CODE>debug</CODE>, <CODE>error</CODE>), but these convenience
 * methods are just wrappers around the equivalent Logger methods.
 * <p>
 * 
 * @author oholmes
 * @version 0.1, 21/03/2011
 * @see Logger
 */
public class QLogger {

	// composed of a JUL Logger instance
	private final Logger logger;
	
	private long startTime;

	/**
	 * QCMG Logger that utilises the <CODE>java.util.logging</CODE> package to
	 * provide specific levels and messages as required
	 * 
	 * @param logger
	 *            <CODE>Logger</CODE> instance that this <CODE>QLogger</CODE>
	 *            instance will push its logging requests to
	 * @throws IllegalArgumentException
	 *             if the passed in Logger is null
	 * @see Logger
	 */
	public QLogger(Logger logger) {
		if (null != logger)
			this.logger = logger;
		else
			throw new IllegalArgumentException("Null logger passed to QLogger");
	}

	/**
	 * Log an EXECLOG message.
	 * <p>
	 * If the logger is currently enabled for the INFO message level then the
	 * given message is forwarded to all the registered output Handler objects.
	 * <p>
	 * 
	 * @param msg
	 *            The string message
	 * @see QLevel#EXEC
	 */
	public void exec(String msg) {
		logger.log(QLevel.EXEC, msg);
	}

	/**
	 * Log an TOOLLOG message.
	 * <p>
	 * If the logger is currently enabled for the INFO message level then the
	 * given message is forwarded to all the registered output Handler objects.
	 * <p>
	 * 
	 * @param msg
	 *            The string message
	 * @see QLevel#TOOL
	 */
	public void tool(String msg) {
		logger.log(QLevel.TOOL, msg);
	}

	/**
	 * Convenience method that checks to see if the specified level is enabled
	 * for this particular logger
	 * <p>
	 * 
	 * @param level
	 * @return boolean value indicating if the supplied logging level is enabled
	 * @see java.util.logging.Logger#isLoggable(java.util.Level)
	 */
	public boolean isLevelEnabled(Level level) {
		return logger.isLoggable(level);
	}

	/**
	 * @see java.util.logging.Logger#info(java.lang.String)
	 */
	public void info(String msg) {
		logger.info(msg);
	}

	/**
	 * Logs a <CODE>DEBUG</CODE> message, using the {@link Level#FINE} logging
	 * level
	 * 
	 * @see java.util.logging.Logger#fine(java.lang.String)
	 * @see QLevel#DEBUG
	 */
	public void debug(String msg) {
		logger.log(QLevel.DEBUG, msg);
//		logger.fine(msg);
	}

	/**
	 * Logs a <CODE>WARN</CODE> message, using the {@link Level#WARNING} logging
	 * level
	 * 
	 * @see java.util.logging.Logger#warning(java.lang.String)
	 */
	public void warn(String msg) {
		logger.warning(msg);
	}

	/**
	 * Logs an <CODE>ERROR</CODE> message, using the {@link Level#SEVERE}
	 * logging level
	 * 
	 * @see java.util.logging.Logger#severe(java.lang.String)
	 */
	public void error(String msg) {
		logger.severe(msg);
	}

	/**
	 * Logs an <CODE>ERROR</CODE> message, using the {@link Level#SEVERE}
	 * logging level
	 * 
	 * @see java.util.logging.Logger#log(java.util.logging.Level,
	 *      java.lang.String, java.lang.Throwable)
	 */
	public void error(String msg, Throwable t) {
		logger.log(Level.SEVERE, msg, t);
	}

	/**
	 * @see java.util.logging.Logger#log(java.util.logging.Level,
	 *      java.lang.String)
	 */
	public void log(Level level, String msg) {
		logger.log(level, msg);
	}

	/**
	 * Convenience method that logs (at <CODE>EXEC</CODE> level) some key
	 * standard values
	 * <p>
	 * This method would ideally be called when the application is started, so
	 * that the values are logged before any potential problems arise, and so
	 * that the start time can be used to give an accurate indication of the
	 * total run time of the application
	 * <p>
	 * 
	 * @param programName
	 *            String representation of the QCMG module that has been invoked
	 * @param programVersion
	 *            String indicates the version of the program
	 */
	public void logInitialExecutionStats(final String programName,
			final String programVersion) {
		logInitialExecutionStats(programName, programVersion, null);
	}

	/**
	 * Convenience method that logs (at <CODE>EXEC</CODE> level) some key
	 * standard values
	 * <p>
	 * This method would ideally be called when the application is started, so
	 * that the values are logged before any potential problems arise, and so
	 * that the start time can be used to give an accurate indication of the
	 * total run time of the application
	 * <p>
	 * 
	 * @param programName
	 *            String representation of the QCMG module that has been invoked
	 * @param programVersion
	 *            String indicates the version of the program
	 */
	public QExec logInitialExecutionStats(final String programName,
			final String programVersion, final String[] args) {
		return logInitialExecutionStats(new QExec(programName, programVersion, args));
	}	

	public QExec logInitialExecutionStats(final String programName,
			final String programVersion, final String[] args, String uuid) {
		return logInitialExecutionStats(new QExec(programName, programVersion, args, uuid));
	}
	
	public QExec logInitialExecutionStats(QExec ex) {
		// set this so that we can track how long a process takes
		startTime = System.currentTimeMillis();
		
		exec(ex.getUuid().toLogString());
		exec(ex.getStartTime().toLogString());
		exec(ex.getOsName().toLogString());
		exec(ex.getOsArch().toLogString());
		exec(ex.getOsVersion().toLogString());
		exec(ex.getRunBy().toLogString());
		exec(ex.getToolName().toLogString());
		exec(ex.getToolVersion().toLogString());
		exec(ex.getCommandLine().toLogString());
		exec(ex.getJavaHome().toLogString());
		exec(ex.getJavaVendor().toLogString());
		exec(ex.getJavaVersion().toLogString());
		exec(ex.getHost().toLogString());
		return ex;
	}


	/**
	 * Convenience method that logs the stop time, and exit status of the
	 * application
	 * <p>
	 * This method would ideally be called at the end of the applications life,
	 * so that the outputted values can be used in a meaningful way
	 * 
	 * @param exitStatus
	 *            int representing the exit status of the application
	 */
	public void logFinalExecutionStats(final int exitStatus) {
		logFinalExecutionStats(exitStatus, this.startTime);
	}
	
	/**
	 * Returns the time between 2 longs as a string in hh:mm:ss form
	 * @param start long indicating start time
	 * @param end long indicating end time
	 * @return String representation of time elapsed between the two supplied longs (hh:mm:ss)
	 */
	public static String getRunTime(long start, long end) {
		
		long runTimeSec = (end - start) / 1000;
		
		int seconds = (int) (runTimeSec % 60);
        int minutes = (int) ((runTimeSec / 60) % 60);
        int hours = (int) ((runTimeSec / 3600));
        String secondsStr = (seconds < 10 ? "0" : "") + seconds;
        String minutesStr = (minutes < 10 ? "0" : "") + minutes;
        String hoursStr = (hours < 10 ? "0" : "") + hours;
        String time =  hoursStr + ":" + minutesStr + ":" + secondsStr;
        return time;
	}
	
	public static String getUsedMemory(){
		final long MEGABYTE = 1024L * 1024L;

		Runtime runtime = Runtime.getRuntime();
		runtime.gc();	
		long MTotal =  runtime.totalMemory() / MEGABYTE;
//		long MUsed = MTotal - runtime.freeMemory() / MEGABYTE;
		long MMax = runtime.maxMemory() / MEGABYTE;		 
		
		return   String.format("ResourcesUsed: mem=%dM; vmem=%dM.", MTotal,MMax);
		 
	}
	
	
	public void logFinalExecutionStats(final int exitStatus, long startTime) {
		
		exec("StopTime " + DateUtils.getCurrentDateAsString());
		if (startTime > 0) exec("TimeTaken " + getRunTime(startTime, System.currentTimeMillis()));
		exec("ExitStatus " + exitStatus);
	}

	public static String reconstructCommandLine(final String programName,
			final String[] args) {
		String result = programName;
		for (final String arg : args) {
			result += " " + arg;
		}
		return result;
	}

	// ///////////////////////////////////////
	// used for testing
	// //////////////////////////////////////
	Logger getLogger() {
		return logger;
	}

}
