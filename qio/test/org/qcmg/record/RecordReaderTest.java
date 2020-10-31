package org.qcmg.record;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.record.StringFileReader;


public class RecordReaderTest {
	@ClassRule
	public  static TemporaryFolder testFolder = new TemporaryFolder();

	private static File VCF_INPUT_FILE;

	@BeforeClass
	public static void setup() throws IOException {
		
		VCF_INPUT_FILE = testFolder.newFile("test.vcf");
		createTestVcfFile(VCF_INPUT_FILE);
	}
	
	@Test
	public void headerFileTest() throws IOException {		
		//'<>' cannot be used with anonymous classes but it is included java 9
		try(StringFileReader reader = new StringFileReader(VCF_INPUT_FILE)) {
			//no header line
			assertEquals(reader.getHeader().size(), 0);			
		} 
			
		try(StringFileReader reader = new StringFileReader(VCF_INPUT_FILE, "#")) {			 
			//header line start with #
			assertEquals(reader.getHeader().size(), 3);	
			//three records
			int count = 0;
			for(String re: reader) { count ++; }
			assertEquals(count, 3);	
		}
	}
	
	public static void createTestVcfFile(File output) throws IOException {
		
		try(RecordWriter<String> writer = new RecordWriter<>(output);){	
			writer.addHeader("#header1");
			writer.addHeader("#header2");
			writer.addHeader("##header	laster");
			writer.add("tab1_1\ttab1_2\ttab1_3");
			writer.add("#tab2_1\ttab2_2\ttab2_3");
			writer.add("tab3_1\ttab3_2\ttab3_3");
		}
	}

}
