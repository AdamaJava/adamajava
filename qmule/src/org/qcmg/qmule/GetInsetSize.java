/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;

import org.qcmg.picard.SAMFileReaderFactory;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
public class GetInsetSize {
	public static void main(String[] args) throws Exception{
		 
		File input = new File(args[0]);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(input); //new SAMFileReader(input);
		int min =3000;
		int max = 0;
		String aaa = "AAA";
		for( SAMRecord record : reader){
			
			 if(record.getAttribute("XC").equals(aaa)){
				 int size = Math.abs( record.getInferredInsertSize());
				 if(size > max) max = size;
				 if(size < min) min = size;
			 }
		}
		reader.close();
		System.out.println(String.format("Insert range %d-%d\n", min, max));		 
	}

}
