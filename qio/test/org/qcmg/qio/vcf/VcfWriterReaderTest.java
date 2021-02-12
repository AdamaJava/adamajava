package org.qcmg.qio.vcf;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qio.vcf.VcfFileReader;
import org.qcmg.qio.record.RecordWriter;

public class VcfWriterReaderTest {
	
	public static final String[] vcfStrings = new String[] {"##test=test", VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, };
	private final String[] parms = {"chrY","2675826",".","TG","CA"};

	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void getHeaderFromZippedVcfFile() throws IOException {
		File file =  testFolder.newFile("header.vcf.gz");
		
		try(RecordWriter<VcfRecord> writer = new RecordWriter<>(file) ){
			 writer.addHeader(Arrays.stream(vcfStrings).collect(Collectors.joining("\n")));
		}
		assertEquals(true, FileUtils.isInputGZip(file) );
		
		/*
		 * Should be able to get the header back out
		 */
		VcfHeader header = null;
		int num = 0;
		try(VcfFileReader reader = new VcfFileReader(file) ){
			header = reader.getVcfHeader();
			for(VcfRecord re: reader) {
				num ++;
			}
			
		}
		assertEquals(true, null != header);
		assertEquals(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE, header.getChrom().toString());
		assertEquals(1, header.getAllMetaRecords().size());
		assertEquals("##test=test", header.getAllMetaRecords().get(0).toString());
		assertEquals(0, num); //no record
	}
	
	@Test
	public void getHeaderFromInvalidVcfFile() throws IOException {
		File file =  testFolder.newFile("header.vcf.gz");
		
		try(RecordWriter<VcfRecord> writer = new RecordWriter<>(file) ){
			writer.addHeader(Arrays.stream(vcfStrings).collect(Collectors.joining("\n")));
			//add two valide record
			writer.add(new VcfRecord(parms));
			writer.add(new VcfRecord(parms));
			
			writer.addHeader(Arrays.stream(vcfStrings).collect(Collectors.joining("\n")));
		}
		assertEquals(true, FileUtils.isInputGZip(file) );
		
		
		/*
		 * Should be able to get the header back out
		 */
		VcfHeader header = null;
		try(VcfFileReader reader = new VcfFileReader(file) ){
			header = reader.getVcfHeader();
		}
		assertEquals(true, null != header);
		assertEquals(1, header.getAllMetaRecords().size());
		
		int num = 0;
		try(VcfFileReader reader = new VcfFileReader(file) ){
			for(VcfRecord re: reader) {
				num ++;
				System.out.println(re.toSimpleString());
			}
			fail("expected exception should throw here! ");
		}catch(IllegalArgumentException e) {
			//two valid record but exception happed when check next record before create second vcf record
			assertEquals(1, num); //one valid record			
		}
	}
	
	@Test
	public void testValidation() throws IOException  {
		String[] fnames = new String[] { "test.output.gz", "test.output" };
		String[] vcfStrings = new String[] {"#testing...", VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE};
		for(int i = 0; i < fnames.length; i ++){ 
			File file = testFolder.newFile(fnames[i]);
			//create writer
			try(RecordWriter<VcfRecord> writer = new RecordWriter<>(file) ){
				 writer.addHeader(vcfStrings[i]);				
			} catch (Exception e) { fail(); }
			
			//check output type
			try {
				if( FileUtils.isFileNameGZip(file) )				
					Assert.assertTrue( FileUtils.isInputGZip(file) );
				else
					Assert.assertFalse( FileUtils.isInputGZip(file) );				
			} catch (IOException e) { fail(); }
			
			//read invaid vcf
			try(VcfFileReader reader = new VcfFileReader(file);){
				//file can be deleted here even without close but can't be closed if fail()	
				Assert.assertTrue( file.delete());				
				if(i == 0 ) fail();
				Assert.assertTrue( reader.getVcfHeader() != null);
			
			} catch (Exception e) {				 
				e.printStackTrace();
				if(i == 1 ) fail();
				//delete testing file which is invalid vcf, 
				Assert.assertTrue( file.delete());
			}						
		}			
	}

	@Test
	public void testCreateAppendVcfWriter() throws IOException {
		File file =  testFolder.newFile("output.vcf");
		
		//create new file 
		try(RecordWriter<VcfRecord> writer = new RecordWriter<>(file) ){
			 writer.addHeader(vcfStrings[0]);				
		} catch (Exception e) { fail(); }
		
		// read throw exception
		try(VcfFileReader reader = new VcfFileReader(file);){
			fail();
		} catch (Exception e) { }
		
		
		//append to file
		try(RecordWriter<VcfRecord> writer = new RecordWriter<>(file, true)   ){
			 writer.addHeader(vcfStrings[1]);				
		} catch (Exception e) { fail(); }
		
		//now it become a valid vcf file
		try(VcfFileReader reader = new VcfFileReader(file);){ 
			Assert.assertFalse( FileUtils.isInputGZip(file) );	
			Assert.assertTrue( reader.getVcfHeader().getAllMetaRecords().size() == 1);	
			Assert.assertTrue(file.delete());
			
		} catch (Exception e) {fail(); }		
		
	}

}
