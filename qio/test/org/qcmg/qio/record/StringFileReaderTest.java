package org.qcmg.qio.record;


import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

 
public class StringFileReaderTest {

	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();

	public static final String[] vcfStrings = new String[] {"##test=test", VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE};
	public static final String[] parms = {"chrY","2675826",".","TG","CA",".","COVN12;MIUN","SOMATIC;NNS=4;END=2675826","ACCS","TG,5,37,CA,0,2","AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1"};
	
	@Test
	public void getHeaderFromtxtFile1() throws IOException {
		File f = testFolder.newFile();
		createFile(f);
		
		//test StringFileReader(File file)
		try(StringFileReader reader = new StringFileReader(f);) {
			assertEquals( 2, reader.getHeader().size());
			int count = 0;
			for(String rec : reader) { count ++; }
			assertEquals( 1, count);
		}
		
		//test StringFileReader(File file, CharSequence headerPrefix)
		try(StringFileReader reader = new StringFileReader(f, "##");) {
			assertEquals( 1, reader.getHeader().size());
			int count = 0;
			for(String rec : reader) { count ++; }
			assertEquals( 2, count);
		}
		
		//test StringFileReader(File file, int bufferSize)
		try(StringFileReader reader = new StringFileReader(f, 512);) {
			assertEquals( 2, reader.getHeader().size());
			int count = 0;
			for(String rec : reader) { count ++; }
			assertEquals( 1, count);
		}	
		
		//test StringFileReader(final File file, int bufferSize, CharSequence headerPrefix, Charset charset)
		try(StringFileReader reader = new StringFileReader(f, 512, "##", StandardCharsets.UTF_8);) {
			assertEquals( 1, reader.getHeader().size());
			int count = 0;
			for(String rec : reader) { count ++; }
			assertEquals( 2, count);
		}		
		
	}
	
	private void createFile(File f) throws IOException {
		
		
		//append two string into file
		try(RecordWriter<String> writer = new RecordWriter<>(f)) {
			writer.addHeader(Arrays.stream(vcfStrings).collect(Collectors.joining("\n")));
			writer.add(new VcfRecord(parms).toString());
		}  

	}

}
