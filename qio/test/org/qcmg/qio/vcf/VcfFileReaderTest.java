package org.qcmg.qio.vcf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.vcf.header.VcfHeaderUtils;


public class VcfFileReaderTest {
	
	
	
	
	private void createVcf() throws IOException {
		//normal BAM with one novel start, gematic.soi = 3% < 0.05
		 List<String> data = new ArrayList<String>();
	 
	   data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1");
	   data.add("chr11	2672739	.	ATT	A	123.86	.	.	GT	0/1"); 
	   try( BufferedWriter out = new BufferedWriter(new FileWriter("test.vcf" ))) {
			for (String line : data) {
	           out.write(line + "\n");
			}
			for (String line : data) {
				out.write(line + "\n");
			}
	    }   

	}
}
