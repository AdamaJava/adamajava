/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import net.sf.samtools.*;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecordIterator;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;


public class queryChrMT {
		 
	public static void main(final String[] args) throws IOException, InterruptedException {
  
    	try{
    		
    		File inBAM = new File(args[0]);
    		String outputName = inBAM.getName().replace(".bam", ".chrMT.primary.bam");
    		File output = new File(args[1], outputName);
    		
	 		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(inBAM,ValidationStringency.SILENT);
	 		SAMFileHeader he = reader.getFileHeader().clone();
			SAMOrBAMWriterFactory writeFactory = new SAMOrBAMWriterFactory(he , true, output);
			SAMRecordIterator ite = reader.query("chrMT",0,  16569, false);
			
			SAMRecord record;
			while(ite.hasNext()){
				record = ite.next();
				if(!record.getNotPrimaryAlignmentFlag())
					writeFactory.getWriter().addAlignment(record );	 
				
			}
    		writeFactory.closeWriter();
    		reader.close();
    		 
    		System.exit(0);
    	}catch(Exception e){
    		System.err.println(e.toString());
    		Thread.sleep(1);
    		System.out.println("usage: qmule org.qcmg.qmule.queryChrMT <input BAM> <output Dir>");
    		System.exit(1);
    	}
		
	}
	

}
