/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.util;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;

public class TabbedDataLoader {
	
	public static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	private static final QLogger logger = QLoggerFactory.getLogger(TabbedDataLoader.class);
	
	
	public static void loadTabbedData(String tabbedDataFile, int position, Map<ChrPosition, TabbedRecord> collection) throws Exception {
		if (FileUtils.canFileBeRead(tabbedDataFile)) {
			
			TabbedFileReader reader = new TabbedFileReader(new File(tabbedDataFile));
			try {
				for (TabbedRecord tr : reader) {
					String [] params = tabbedPattern.split(tr.getData());
					String chrPosition = getStringFromArray(params, position);
					
					if (null != chrPosition) {
						ChrPosition chrPos = StringUtils.getChrPositionFromString(chrPosition);
						if (null != chrPos) collection.put(chrPos,tr);
					}
				}
				
				logger.info("Added " + collection.size() + " entries to the tabbed data collection");
				
			} finally {
				reader.close();
			}
		} else {
			throw new IllegalArgumentException("data file: " + tabbedDataFile + " could not be read");
		}
	}
	
	public static String getStringFromArray(String[] params, int index) {
		String result = null;
		if (null != params && params.length > 0) {
			if (index >= 0) {
				result = params[(index > params.length ? params.length : index)];
			} else if (params.length + index >= 0 &  params.length + index < params.length){
				result = params[params.length + index];	// adding a negative number!
			}
		}
		return result;
	}

}
