/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import org.qcmg.common.string.StringUtils;


public class BAM2CS {
	File inBAM;
	File outDir;
	HashMap<Integer, PrintWriter> outFast = new HashMap<Integer, PrintWriter>();  
	HashMap<Integer, PrintWriter> outQual = new HashMap<Integer, PrintWriter>();
	 
	
	BAM2CS(final String[] args) throws Exception{
		inBAM = new File(args[0]);
		outDir = new File(args[1]);
		printHeader(null);   	 
	}
	
	/**
	 * retrive the CS and CQ value from BAM record to output csfasta or qual file
	 * @throws Exception 
	 */
	void CreateCSfile() throws Exception{	
		
		SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault();	
		SamReader reader = samReaderFactory.open(inBAM);
		int num = 0;
		for (SAMRecord record : reader) {
			String id = ">" + record.getReadName();
			Add2Fasta(id, record.getAttribute("CS").toString());
			add2Qual(id,  record.getAttribute("CQ").toString());
			num ++;			
		}
		
		reader.close();
		closeWriters();
		
		System.out.println(getTime() + " total output records " + num);
		System.exit(0);
	}
	
	/**
	 * Add header information to Writer. If Writer is null, print to STD
	 * @param Writer
	 * @throws Exception
	 */
	private void printHeader(PrintWriter Writer) throws Exception{
		if(Writer == null){
			System.out.println(getTime() + " tool name: qmule org.qcmg.qmule.BAM2CS");
			System.out.println(getTime() + " host: " + InetAddress.getLocalHost().getHostName());
	    	System.out.println(getTime() + " input: " + inBAM.getAbsolutePath());
	    	System.out.println(getTime() + " output directory: " + outDir.getAbsolutePath());		
		}else{
			Writer.println("#" + getTime() + " tool name: qmule org.qcmg.qmule.BAM2CS");
			Writer.println("#" + getTime() + " host: " + InetAddress.getLocalHost().getHostName());
	    	Writer.println("#" + getTime() + " input: " + inBAM.getAbsolutePath());			
		}
	}
	
	private void closeWriters(){
		//close all csfasta files
	    Iterator<PrintWriter> itr =	outFast.values().iterator();    
	    while(itr.hasNext()){
	    	PrintWriter Writer =   itr.next();
	    	Writer.close();	    	
	    }
	    
	    //close all qual files
	    itr =	outQual.values().iterator();    
	    while(itr.hasNext()){
	    	PrintWriter Writer =   itr.next();
	    	Writer.close();	    	
	    }		
	}
	
	/**
	 * Add raw color sequence into output csfasta; If the output file isn't exist, create a new one with header lines
	 * @param id
	 * @param seq
	 * @throws Exception
	 */
	private void Add2Fasta(String id, String seq) throws Exception{
		//sequence length should -1 since it start with 'T' or 'G'
		int len = seq.length() - 1;	
		PrintWriter Writer;		
		
		//get writer or create an new one
		if(outFast.containsKey(len)){
			Writer = outFast.get(len);
		}else{
			String fname = inBAM.getName();
			int index = fname.lastIndexOf('.');
			fname = fname.substring(0,index) + "." + len + ".csfasta";
			File csFile = new File(outDir, fname);
			Writer = new PrintWriter(new FileWriter(csFile));
			outFast.put(len, Writer);	
			printHeader(Writer);
			System.out.println(getTime() + " creating output: " + csFile.getAbsolutePath() );
		}
		
		Writer.println(id);
		Writer.println(seq);	
	}
	/**
	 * cover CQ value into raw qual sequence and addto output qual;
	 * If the output file isn't exist, create a new one with header lines.
	 * @param id
	 * @param seq
	 * @throws Exception
	 */
	void add2Qual(String id, String seq) throws Exception{
		int len = seq.length();		
		PrintWriter writer;	
		
		//get writer or create an new one
		if(outQual.containsKey(len)){
			writer = outQual.get(len);
		}else{
			String fname = inBAM.getName();
			int index = fname.lastIndexOf('.');
			fname = fname.substring(0,index) + "." + len + ".qual";
			File csFile = new File(outDir, fname);
			writer = new PrintWriter(new FileWriter(csFile));
			outQual.put(len, writer);	
			printHeader(writer);
			System.out.println(getTime() + " creating output: " + csFile.getAbsolutePath() );
		}
		
		//convert ascii to int
		String qual = "";
		for(int i = 0; i < len; i ++){
			char c = seq.charAt(i);
			int j = c;
			
			if(StringUtils.isNullOrEmpty(qual)){
				qual += j;
			} else	 {
				qual += " " + j;
			}
		}		
		
		writer.println(id);
		writer.println(qual);

	}
	
	private String getTime(){
		Calendar currentDate = Calendar.getInstance();
		SimpleDateFormat formatter=  new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss");
		return "[" + formatter.format(currentDate.getTime()) + "]";		
	}
	public static void main(final String[] args) throws IOException, InterruptedException {
  
    	try{
    		BAM2CS myCS = new BAM2CS(args);
    		myCS.CreateCSfile();
    		System.exit(0);
    	}catch(Exception e){
    		System.err.println(e.toString());
    		Thread.sleep(1);
    		System.out.println("usage: qmule org.qcmg.qmule.BAM2CS <input BAM> <output Dir>");
    		System.exit(1);
    	}
		
	}
}
