/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator; 
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;


import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;

public class BamMismatchCounts {
	static QLogger logger = QLoggerFactory.getLogger(BamMismatchCounts.class);
    static long[] mismatch = new long[100];
    
	static HashMap<String, Integer> counts = new HashMap<String, Integer>();	
	static long total = 0;
	static long unmapped = 0;
	static long clipped = 0;
	static long indel = 0;
	static long skipPad = 0;	
	static long fullMapped = 0;
	static long noMDreads = 0;

	/**
	 * count the mismatch base number based on the MD field
	 * @param r: samrecord
	 */
	private static void countMismatch(SAMRecord r) {		
		String attribute = (String)r.getAttribute("MD");	
		if (null != attribute) {
			int count = 0;
			for (int i = 0, size = attribute.length() ; i < size ; ) {
				char c = attribute.charAt(i);
				if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N') {
					count++;
					i++;
				} else if ( c == '^') {
					//skip the insertion base
					while (++i < size && Character.isLetter(attribute.charAt(i))) {}
				} else i++;	// need to increment this or could end up with infinite loop...
			}
			mismatch[count] ++;
			
		}else
			noMDreads ++;
		 
		
	}
    
	/**
	 * 
	 * @param r: sam record
	 * @return true if this read is full length mapped without any indels, skips and pads
	 */
	static private Boolean seekFullMapped(SAMRecord r){
				 
		if(r.getReadUnmappedFlag()){
			unmapped ++;
			return false;
		}
		//reads with clips or indel, skips, pads
		else{
			List<CigarElement> ele = r.getCigar().getCigarElements();
 			for (CigarElement element : r.getCigar().getCigarElements()){		
 				if( element.getLength() > 0){ 					
 					if(element.getOperator() == CigarOperator.H ||element.getOperator() == CigarOperator.S) {					 
 						clipped ++;
 						return false;
 					}else if (element.getOperator() == CigarOperator.I ||element.getOperator() == CigarOperator.D){
 						indel ++;
 						return false;
 					}else if (element.getOperator() == CigarOperator.P ||element.getOperator() == CigarOperator.N){
 						skipPad ++;
 						return false;
 					}
 				}
 			}
 			//count mismatch after the for loop
 			return true;			
		}		
	} 
 
	/**
	 * survey the mismatch stats on full length mapped reads
	 * @param args: SAM/BAM file with full path, log file with full path
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
 		System.out.println("reder closing....");			
		if (args.length == 2) {	
 			logger = QLoggerFactory.getLogger(BamMismatchCounts.class, args[1] + ".log", null);			
 			logger.logInitialExecutionStats( "qmule " + BamMismatchCounts.class.getName(), null,args);
 			for(int i = 0; i < 100; i++) mismatch[i] = 0;
 			
 			//check each reads 						 
 			SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(new File(args[0]),
 					SAMFileReader.ValidationStringency.SILENT);			
			for (SAMRecord r : reader){ 	
				total ++;
				if(seekFullMapped( r)){
					fullMapped ++;  
					countMismatch(r);
				}
			}			
			reader.close();

			//report mismatch
			String S_mismatch = "mismatch matrix for fully mapped reads is below:\nmismatch\treads_number\tratio_to_(fullmapped,total)\n";
			for(int i = 0; i < 100; i++)
				if(mismatch[i] > 0){
					int p1 = Math.round(mismatch[i] * 100 / fullMapped);
					int p2 = Math.round(mismatch[i] * 100 / total);
					S_mismatch += String.format("%d\t%d\t(%d%%,%d%%)\n", i,mismatch[i],p1, p2);
						//	i + "\t" + mismatch[i] + "\t" + mismatch[i]/fullMapped +"\n";			
					
				}
	
			  Files.write(Paths.get(args[1]), S_mismatch.getBytes() );			
			 
			logger.info("total records in file: " + total );				
			logger.info("unmapped records: " + unmapped);
			logger.info("records with clipping (CIGAR S,H): " + clipped);
			logger.info("records with indel (CIGAR I,D) : " + indel);	 
			logger.info("records with skipping or padding (CIGAR N,P) : " + skipPad);   
			logger.info("records mapped full-length: " + fullMapped); 
			logger.info("records mapped full-length but missing MD field: " +  noMDreads);
			logger.info("the mismatch counts matrix is outputed to " + args[1]);
 
//			logger.info(S_mismatch);
			logger.logFinalExecutionStats(0);
			
		} else {
			logger.info("USAGE: qmule " + BamMismatchCounts.class.getName() + " <bam/sam filename> <output filename>");
		} 
		
	}

}
