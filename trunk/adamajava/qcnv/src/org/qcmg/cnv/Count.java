/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.cnv;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
//import java.util.ArrayList;
import java.util.*;


import org.qcmg.common.log.QLogger;
import org.qcmg.picard.SAMFileReaderFactory;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;

public interface Count {
 /**
  * 
  * @return an instance of SingleRefInfo which store the counts in arrays or tmp files
  */
	public ReferenceInfo execute() throws Exception;

}
