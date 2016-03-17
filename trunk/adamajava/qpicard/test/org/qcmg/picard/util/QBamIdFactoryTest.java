package org.qcmg.picard.util;

import static org.junit.Assert.assertTrue;
import htsjdk.samtools.SAMFileHeader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.qcmg.common.meta.QBamId;
import org.qcmg.picard.SAMFileReaderFactory;

 
public class QBamIdFactoryTest {
	final static String  inputBam = "input.sam";
	final static String  uuid = "ecc3c778-a77e-49b7-a4f5-fe828bcf9b88";
 
	
	
	@Test
	public void getQ3BamIdTest() throws Exception {
		createSam(inputBam);		
		String q3bamuuid = QBamIdFactory.getQ3BamId(inputBam).getUUID();	
		assertTrue(q3bamuuid.equals(uuid));
		
		new File(inputBam).delete();
		
	}
	
	
	public static void createSam(String inputBam) {
        List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:20140717025441134	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:qtest::Test	VN:0.2pre");
        data.add("@SQ	SN:chrY	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        data.add("@CO	create by qcmg.qbamfilter.filter::TestFile");
        data.add("@CO	q3BamUUID:" + uuid);	
        data.add("@CO	q3BamUUID=" + uuid);		        
        try( BufferedWriter out = new BufferedWriter(new FileWriter(inputBam ))) {	           
           for (String line : data)  
                   out.write(line + "\n");	           	            
        }catch(IOException e){ 	        	 
        	assertTrue(false);
        } 
	}    
	        


}
