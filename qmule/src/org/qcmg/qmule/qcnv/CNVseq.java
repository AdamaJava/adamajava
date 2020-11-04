/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.qcnv;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import htsjdk.samtools.*;

import java.lang.Math;
import java.io.*;

import org.qcmg.picard.SAMFileReaderFactory;


public class CNVseq {
	
	private static final boolean String = false;
	//in cnv-seq.pl it call below R to get value
	//echo 'options(digits=16);qnorm(1-0.5*0.001)' | R --vanilla --slave (result: 3.290526731491926)	
	public static double bt = 3.290526731491926;
	//$echo 'options(digits=16);qnorm(0.5*0.001)' | R --vanilla --slave (result: -3.290526731491894)
	public static double st = -3.290526731491894;
	
	public static double pvalue = 0.001;
	public static int min_windoe = 4;
	public static double log2 = 0.6;	
	public static double bigger = 1.5;
	//public static int debug = 0;
	//public static String Rexe = "R";
	
	private final Map<String, Integer> refSeq;

	private final long genomeSize ;
	private final long numTest;
	private final long numRef;
	
	private final double biglog2_window;
	private final double smalog2_window;
	private final int windowSize;
	
	private final File ftest;
	private final File fref;
 
	
	/**
	 * it caculate the window size based on genome size, TEST and REF BAM records number 
	 * @param test: File of TEST BAM
	 * @param ref: File of reference BAM
	 * @throws Exception
	 */
	CNVseq(File test, File ref, int window ) throws Exception {	
		//open file
		SamReader rtest = SAMFileReaderFactory.createSAMFileReader(test );//new SAMFileReader(test);
		SamReader rref = SAMFileReaderFactory.createSAMFileReader(ref );//new SAMFileReader(ref);
	
		//check whether index file exist or not 
		if(!rtest.hasIndex()){
			throw new Exception("can't find index for: " + test.getName());
		}
		if(!rref.hasIndex()){
			throw new Exception("can't find index for: " + ref.getName());
		}
		ftest = test;
		fref = ref;
		
		//check SAM header
		SAMFileHeader htest = rtest.getFileHeader();
		SAMFileHeader href = rref.getFileHeader();
		
		//get sequence information from both  inputs 
		Map<String, Integer> seqTest = new HashMap <String, Integer>();
		Map<String, Integer> seqRef = new HashMap <String, Integer>();
		
		List<SAMSequenceRecord> genome = htest.getSequenceDictionary().getSequences();
		for(SAMSequenceRecord re : genome){
			seqTest.put(re.getSequenceName(),re.getSequenceLength());
		}
		
		genome = href.getSequenceDictionary().getSequences();
		for(SAMSequenceRecord re : genome){
			seqRef.put(re.getSequenceName(),re.getSequenceLength());
		}

		// check both @SQ line are same or not
		if(seqRef.size() != seqTest.size()){ 
			throw new Exception("the sequence size are different between two inputs: \n" + ftest.getName() + "\n" + fref.getName() ); 
		}
 
		for (String key : seqTest.keySet()){
			//first convert Integer to int
			int l1 = seqTest.get(key);
			int l2 = seqRef.get(key);		
			if(l1 != l2){
				throw new Exception("the sequence size of " + key + " are different between two inputs : \n" + ftest.getName() + "\n" + fref.getName()  );
			}		
		}
		
		// assign one of the identical reference info into the hash map
		refSeq = seqTest;
		
		//caculate the genome size based on the identail reference
		 long size = 0;
		 for(String key : refSeq.keySet()){	  size += refSeq.get(key);	 }
		 genomeSize = size;
//-debug
//genomeSize = 3253037807L;

		//count mapped record number based on index file
		BAMIndex tIndex = rtest.indexing().getIndex();
		BAMIndex rIndex = rref.indexing().getIndex();
		BAMIndexMetaData meta; 
		int tMapped = 0;
		int rMapped = 0;
		for(int i = 0; i < seqRef.size(); i ++ ){
			meta = tIndex.getMetaData(i);
			tMapped += meta.getAlignedRecordCount();
			meta = rIndex.getMetaData(i);
			rMapped += meta.getAlignedRecordCount();
		}		 
		 numTest = tMapped;
		 numRef = rMapped;
	
		//close files
		rtest.close();
		rref.close();
		
		//caculate window size
		 double brp = Math.pow(2, log2);
		 double srp = 1.0 / brp; 		
		 
		
		 biglog2_window = (numTest * Math.pow(brp, 2)  + numRef) * genomeSize * Math.pow(bt, 2) / (  Math.pow((1- brp),2 ) * numTest * numRef);
		 smalog2_window = (numTest * Math.pow(srp, 2)  + numRef) * genomeSize * Math.pow(st, 2) / (  Math.pow((1- srp),2 ) * numTest * numRef);
		 if(window == 0 ){
			 windowSize =  (int) (Math.max(biglog2_window, smalog2_window) * bigger) ;
		 }else{
			 windowSize = window;
		 }

	}

	/**
	 * it create an Iterator and query on each window; finally it close the iterator
	 * @param f: SAMFileReader
	 * @param chr: genoeme name
	 * @param start: window start postion
	 * @param end: window end position
	 * @return the totoal number of records mapped overlapped on this window region
	 */
	int  exeQuery (SamReader reader, String chr, int start, int end){
  
	 	SAMRecordIterator block_ite = reader.queryOverlapping(chr, start, end);
	 	int num = 0;
	 	while(block_ite.hasNext()){
	 		num ++; 
	 		block_ite.next();
	 	}
	 	
	 	block_ite.close();
		
		return num;
	}
	
	/**
	 * 
	 * @return total SAM records number in Test input file
	 */
	long getTestReadsNumber(){return numTest;}
	
	/**
	 * 
	 * @return total SAM records number in Ref input file
	 */
	long getRefReadsNumber(){return numRef;}
	
	/**
	 * 
	 * @return a hash table list each sequence reference name and length
	 */
	Map<String, Integer> getrefseq(){return refSeq;}
	
	/**
	 * 
	 * @return return the minimum window size for detecting log2>=0.6
	 */
	double getpositivelog2window(){ return biglog2_window;}
	
	/**
	 * 
	 * @return The minimum window size for detecting log2<=-0.6
	 */
	double getnegativelog2window(){return smalog2_window;}
	
	/**
	 * 
	 * @return The window size to use is max(100138.993801, 66550.928197) * 1.500000
	 */
	int getWindowSize(){		 return  windowSize;	}	
	
	/**
	 * 
	 * @return the total length of reference sequence listed on BAM @SQ lines
	 */
	long getGenomeSize( ){ return genomeSize;}
	
	/**
	 * 
	 * @return the Test File with File type
	 */
	File getTestFile(){return ftest;}
	
	/**
	 * 
	 * @return the Ref File with File type
	 */
	File getRefFile(){return fref;}
	 
}
