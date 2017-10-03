package org.qcmg.vcf;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class VcfWriterReaderTest {
	
	@Test
	public void testValidation()  {
		String[] fnames = new String[] { "test.output.gz", "test.output" };
		String[] vcfStrings = new String[] {"#testing...", VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE};
		for(int i = 0; i < fnames.length; i ++){ 
			File file = new File(fnames[i]);
			//create writer
			try(VCFFileWriter writer = new VCFFileWriter(file) ){
				 writer.addHeader(vcfStrings[i]);				
			} catch (IOException e) { fail(); }
			
			//check output type
			try {
				if( FileUtils.isFileNameGZip(file) )				
					Assert.assertTrue( FileUtils.isInputGZip(file) );
				else
					Assert.assertFalse( FileUtils.isInputGZip(file) );				
			} catch (IOException e) { fail(); }
			
			//read invaid vcf
			try(VCFFileReader reader = new VCFFileReader(file);){
				//file can be deleted here even without close but can't be closed if fail()	
				Assert.assertTrue( file.delete());				
				if(i == 0 ) fail();
				Assert.assertTrue( reader.getHeader() != null);
			
			} catch (IOException e) {				 
				e.printStackTrace();
				if(i == 1 ) fail();
				//delete testing file which is invalid vcf, 
				Assert.assertTrue( file.delete());
			}						
		}			
	}

	@Test
	public void testCreateAppendVcfWriter()  {
		File file = new File("output.vcf");
		String[] vcfStrings = new String[] {"##test=test", VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE};
		
		//create new file 
		try(VCFFileWriter writer = new VCFFileWriter(file) ){
			 writer.addHeader(vcfStrings[0]);				
		} catch (IOException e) { fail(); }
		
		// read throw exception
		try(VCFFileReader reader = new VCFFileReader(file);){
			fail();
		} catch (IOException e) { }
		
		
		//append to file
		try(VCFFileWriter writer = VCFFileWriter.CreateAppendVcfWriter(file)   ){
			 writer.addHeader(vcfStrings[1]);				
		} catch (IOException e) { fail(); }
		
		//now it become a valid vcf file
		try(VCFFileReader reader = new VCFFileReader(file);){ 
			Assert.assertFalse( FileUtils.isInputGZip(file) );	
			Assert.assertTrue( reader.getHeader().getAllMetaRecords().size() == 1);	
			Assert.assertTrue(file.delete());
			
		} catch (IOException e) {fail(); }		
		
	}

}
