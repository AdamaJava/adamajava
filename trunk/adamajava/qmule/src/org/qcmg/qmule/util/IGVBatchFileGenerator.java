/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.FileUtils;

public class IGVBatchFileGenerator {
	
	public static final String GENOME = "GRCh37_ICGC_standard_v2";
	
	
	public static void generate(final List<ChrPosition> positions, final String outputFile) throws IOException {
		// check that list is not empty
		if (positions == null || positions.isEmpty()) 
			throw new IllegalArgumentException("Null or empty list passed to IGVBatchFileGenerator");
		
		// can we write to the outputFile?
		File output = new File(outputFile);
		if( ! FileUtils.canFileBeWrittenTo(output))
			throw new IllegalArgumentException("Can't write to output file: " + outputFile);
			
		FileWriter writer = new FileWriter(output);
		
		try {
			writer.write(getHeaderInfo(output));
			
			for (ChrPosition position : positions) {
				writer.write(getLocationString(position));
			}
			
		} finally {
			writer.close();
		}
		
	}
	
	private static String getHeaderInfo(File output) {
		String path = output.getParent();
		return "snapshotDirectory " + path + "\n"
		+ "genome " + GENOME + "\n";
	}
	
	private static String getLocationString(ChrPosition chrPos) {
		return "goto " + chrPos.getChromosome() + ":" + chrPos.getPosition() + "-" + chrPos.getPosition() 
		+ "\nsort base\n" +
				"collapse\n" +
				"snapshot " + chrPos.getChromosome() + ":" + chrPos.getPosition() + ".png\n"; 
	}
	
	
	
//	snapshotDirectory C:/IGV_sessions/exonorama/APGI_1992
//	genome GRCh37_ICGC_standard_v2
//	goto chr8:93156526-93156566
//	sort base
//	collapse
//	snapshot APGI_1992_SNP_35325-chr8-93156546-var-CtoT-WITHIN_NON_CODING_GENE-ENSG00000233778.png
//	goto chr12:114377865-114377905
//	sort base
//	collapse
//	snapshot APGI_1992_SNP_50905-chr12-114377885-var-GtoC-SYNONYMOUS_CODING-RBM19.png
//	goto chr1:228481880-228481920
//	sort base
//	collapse
//	snapshot APGI_1992_SNP_6964-chr1-228481900-var-GtoA-NON_SYNONYMOUS_CODING-OBSCN.png


}
