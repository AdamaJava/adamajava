/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import org.qcmg.common.util.TabTokenizer;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedFileWriter;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public class DbSnpChrLiftover {
	
	private static char TAB = '\t';
	
	String inputVCF;
	String outputVCF;
	
	
	private final Set<String> uniqueChrNames = new TreeSet<String>();
	
	public DbSnpChrLiftover() {}
	
	private void getUniqueChrNames() throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(inputVCF));
		TabbedFileWriter writer = new TabbedFileWriter(new File(outputVCF));
		try {
			
			TabbedHeader header = reader.getHeader();
			
			// writer out header
			writer.addHeader(header);
			
			for (TabbedRecord record : reader) {
				String [] params = TabTokenizer.tokenize(record.getData()); 
				String chr = params[0];
				uniqueChrNames.add(chr);
				
				// switch the chr
				params[0] = "chr" + chr;
				
				StringBuilder sb = new StringBuilder();
				for (int i = 0, len =  params.length ; i < len ; i ++) {
					sb.append(params[i]);
					if (i < len-1) sb.append(TAB);
				}
				
				record.setData(sb.toString());
				
				writer.add(record);
			}
			
		} finally {
			try {
				writer.close();
			} finally {
				reader.close();
			}
		}
		
		
		for (String chr : uniqueChrNames) {
			System.out.println("chr: " + chr);
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		if (args.length < 2) 
			throw new IllegalArgumentException("USAGE: DbSnpChrLiftover <input_vcf> <output_vcf>");
		
		DbSnpChrLiftover dcl = new DbSnpChrLiftover();
		
		
		dcl.inputVCF = args[0];
		dcl.outputVCF = args[1];
		
		dcl.getUniqueChrNames();
		
	}

}
