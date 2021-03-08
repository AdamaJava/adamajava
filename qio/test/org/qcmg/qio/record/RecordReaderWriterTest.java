package org.qcmg.qio.record;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qio.record.StringFileReader;
import org.qcmg.qio.record.RecordWriter;
import org.qcmg.common.util.Constants;

public class RecordReaderWriterTest {
	
	public static final String[] vcfStrings = new String[] {"##test=test", VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE};
	public static final String[] parms = {"chrY","2675826",".","TG","CA",".","COVN12;MIUN","SOMATIC;NNS=4;END=2675826","ACCS","TG,5,37,CA,0,2","AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1"};
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void getHeaderFromZippedVcfFile1() throws IOException {
		File file =  testFolder.newFile("header.vcf.gz");
		
		try(RecordWriter<VcfRecord> writer = new RecordWriter<>(file) ){
			 writer.addHeader(Arrays.stream(vcfStrings).collect(Collectors.joining("\n")));
		}
		assertEquals(true, FileUtils.isInputGZip(file) );
		
		/*
		 * Should be able to get the header back out
		 */
		List<String> header = null;
		try(StringFileReader reader = new StringFileReader(file)){
			header = reader.getHeader();
		}
		assertEquals(2, header.size());
		assertEquals(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, header.get(1));
		assertEquals("##test=test", header.get(0));
	}
	
	@Test
	public void VcfRecordTest() throws IOException {
		File file =  testFolder.newFile("output.vcf");
		
		//create new String file with two vcf record
		try(RecordWriter<VcfRecord> writer = new RecordWriter<>(file)   ){
			 //add record twice
			 writer.add(new VcfRecord(parms));
			 writer.add(new VcfRecord(parms));
		} catch (Exception e) { fail(); }
		
		//append two string into file
		try(RecordWriter<String> writer = new RecordWriter<>(file,true)   ){
			 writer.add("");
			 writer.add("after empty");
			 writer.add(Constants.NL_STRING);
			 writer.add("after new line");

			 //add record twice
			 writer.add(null);
				fail(); 
		} catch (Exception e) { 
			//null is not allowed
		}
			
		//now it become a valid vcf file
		try(StringFileReader reader = new StringFileReader(file);){ 
			int count = 0;
			for(String rec : reader) {
				count++;
				if(count == 4) assertEquals("after empty", rec);
				if(count == 5) assertEquals("", rec);
				if(count == 6) assertEquals("after new line", rec);
			}
			assertEquals( 6, count );		
		} catch (Exception e) {fail(); }	
	}

	@Test
	public void testCreateAppendVcfWriter() throws IOException {
		File file =  testFolder.newFile("output.vcf");
		
		//create new file 
		try(RecordWriter<VcfRecord> writer = new RecordWriter<>(file) ){
			 writer.addHeader(vcfStrings[0]);				
		} catch (Exception e) { fail(); }
		
		// read throw exception
		try(StringFileReader reader = new StringFileReader(file);){
			Assert.assertTrue( reader.getHeader().size() == 1);	
			int count = 0;
			for(String rec : reader) count++;
			assertEquals( 0, count );				
		} catch (Exception e) { fail();}		

		//append to file
		try(RecordWriter<VcfRecord> writer = new RecordWriter<>(file,true)   ){
			 writer.addHeader(vcfStrings[1]);	
			 //add record twice
			 writer.add(new VcfRecord(parms));
			 writer.add(new VcfRecord(parms));
		} catch (Exception e) { fail(); }
		
		//now it become a valid vcf file
		try(StringFileReader reader = new StringFileReader(file);){ 
			Assert.assertFalse( FileUtils.isInputGZip(file) );	
			assertEquals( 2, reader.getHeader().size());	
			int count = 0;
			for(String rec : reader) {
				assertEquals(rec, String.join(Constants.TAB_STRING, parms));	
				count++;
			}
			assertEquals(2,  count );		
		} catch (Exception e) {fail(); }			
	}
}
