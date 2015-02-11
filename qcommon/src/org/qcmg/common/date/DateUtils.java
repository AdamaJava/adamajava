/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.date;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
	public static final String LONG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String SHORT_DATE_FORMAT = "dd/mm/yy HH:mm:ss";
	public static final String TIME_FORMAT = "HH:mm:ss.SSS";

	/**
	 * Returns the current date in the <code>yyyy-MM-dd HH:mm:ss</code> format
	 * @return String containing formatted date
	 */
	public static String getCurrentDateAsString() {
		return getCurrentDateAsString(LONG_DATE_FORMAT);
	}
	
	/**
	 * Returns the current date in the specified format
	 * @param format String containing the required date format
	 * @return String containing the current date formatted according to the supplied format
	 */
	public static String getCurrentDateAsString(String format) {
	    	SimpleDateFormat sdf = new SimpleDateFormat(format);
	    return sdf.format(getCurrentDate());
	}
	
	/**
	 * Returns the current date in the specified format
	 * @param FORMAT String containing the required date format
	 * @return String containing the current date formatted according to the supplied format
	 */
	public static String getCurrentTimeAsString() {
		SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
		return sdf.format(getCurrentDate());
	}
	
	/**
	 * Gets the current date
	 * @return Date object relating to the current date
	 */
	public static Date getCurrentDate() {
		Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}
}
