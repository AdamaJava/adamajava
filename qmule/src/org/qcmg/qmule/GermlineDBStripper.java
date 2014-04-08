/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.IOException;

import org.qcmg.germlinedb.GermlineDBFileReader;
import org.qcmg.germlinedb.GermlineDBFileWriter;
import org.qcmg.germlinedb.GermlineDBRecord;

public class GermlineDBStripper {
	
	
	public static void main(String[] args) throws IOException {
		
		String germlineDB = args[0];
		String germlineDBClassA = args[1];
		String header = "analysis_id\tcontrol_sample_id\tvariation_id\tvariation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\tquality_score\tprobability\tread_count\tis_annotated\tvalidation_status\tvalidation_platform\txref_ensembl_var_id\tnote\tflag";
		
		GermlineDBFileReader reader = new GermlineDBFileReader(new File(germlineDB));
		GermlineDBFileWriter writer = new GermlineDBFileWriter(new File(germlineDBClassA));
		
		try {
			writer.add(header+"\n");
			
			// strip out all non-classA entities from Germline_DB
			int totalCount = 0, classACount = 0;
			for (GermlineDBRecord record : reader) {
				++totalCount;
				if ("--".equals(record.getFlag())) {
					++classACount;
					writer.add(record.getData() + "\n");
				}
			}
			System.out.println("total count: " + totalCount + ", classA count: " + classACount);
			
		} finally {
			try {
				reader.close();
			} finally {
				writer.close();
			}
		}
	}
}
