/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import org.qcmg.picard.SAMFileReaderFactory;

public class TestSort {
	private final File input;
	private final File output;
	private final int maxRecordsInRam;
	private SAMFileHeader.SortOrder sort = SAMFileHeader.SortOrder.unsorted;
	
	TestSort(final String[] args) throws Exception{
		input = new File(args[0]);
		output = new File(args[1]);
		maxRecordsInRam = Integer.parseInt(args[2]);		 
		 
    	String sortOrder = args[3];
    	if(sortOrder.equalsIgnoreCase("coordinate"))
    		sort = SAMFileHeader.SortOrder.coordinate;
    	else if(sortOrder.equalsIgnoreCase("queryname"))
    		sort = SAMFileHeader.SortOrder.queryname;
    	else if(! sortOrder.equalsIgnoreCase("unsorted"))
    		throw new Exception( sortOrder +  " isn't valid SAMFileHeader sort order!");    	
    	
    	System.out.println(getTime() + " host: " + InetAddress.getLocalHost().getHostName());
    	System.out.println(getTime() + " input: " + input.getAbsolutePath());
    	System.out.println(getTime() + " output: " + output.getAbsolutePath());
    	System.out.println(getTime() + " sort order: " + sortOrder);
    	System.out.println(getTime() + " max Records In RAM: " + maxRecordsInRam);
	}
	
	public void Sorting() throws Exception{
		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(input);
		SAMFileHeader header = reader.getFileHeader();
		
		SAMFileWriterFactory writeFactory = new SAMFileWriterFactory();                         
	    net.sf.samtools.SAMFileWriterImpl.setDefaultMaxRecordsInRam(maxRecordsInRam );  	
	    header.setSortOrder(sort);
	    if(sort.equals(SAMFileHeader.SortOrder.coordinate))
	    	 writeFactory.setCreateIndex(true);                                
	    final SAMFileWriter writer = writeFactory.makeSAMOrBAMWriter(header, false, output);
		
	    int num = 0;
		for (SAMRecord record : reader) {
			if(num % maxRecordsInRam == 0)
				printRunInfo(num);
			
			writer.addAlignment(record);	
			num ++;
		}
		
//		System.out.println(getTime() + " Merging tmp into output BAM, tmp location are " +  net.sf.samtools.util.IOUtil.getDefaultTmpDir());
		reader.close();
		writer.close();
		
		System.out.println(getTime() + " created output: " + output.getAbsolutePath());		
	}
	
	private void printRunInfo(int number) throws IOException{
		Runtime runtime = Runtime.getRuntime();
		int mb = 1024 * 1024;
		long totalRAM = runtime.totalMemory() / mb;
		long usedRAM = (runtime.totalMemory() - runtime.freeMemory()) / mb;
 
		String dateNow = getTime();
		
		String info = String.format("%s read %d record. Total memeory: %dM, used memory: %dM",
				dateNow, number, totalRAM, usedRAM);
		
		System.out.println(info);
	}
	
	private String getTime(){
		Calendar currentDate = Calendar.getInstance();
		SimpleDateFormat formatter=  new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss");
		return "[" + formatter.format(currentDate.getTime()) + "]";		
	}
	
	public static void main(final String[] args) {
		try{
			TestSort mysort = new TestSort(args); 
			mysort.Sorting();
			System.exit(0);
		}catch(Exception e){	
			System.err.println("usage:qmule.TestSort <input> <output> <maxRecordInRAM> [queryname/coordinate/unsorted]");
			System.err.println(e.toString());
			System.exit(1);			
		}
		
		
	}
}
