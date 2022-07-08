package org.qcmg.qmule;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;

public class FixCutAdapterTest {
	
	
	@ClassRule
	public static TemporaryFolder testFolder = new TemporaryFolder();
	public static File INPUT;
	public static File OUTPUT;
	
	private final static FastqRecord r1 = new FastqRecord("ST-E00129:529:HF5FTALXX:4:2206:13200:50656 1:N:0:TCCGCGAA", "ATGCATGC","", "AAFFFJJJ");
	private final static FastqRecord r2 = new FastqRecord("ST-E00129:529:HF5FTALXX:4:2206:15899:50656", "","", "");
	private final static FastqRecord r3 = new FastqRecord("others", "ATGCATGC","", "");
	
	@BeforeClass
	public static void setup() throws IOException {
		INPUT = testFolder.newFile("input.fastq");		
		OUTPUT = testFolder.newFile("output.fastq");
		
		ArrayList<FastqRecord> data = new ArrayList<>(Arrays.asList( r1,r2,r3 ) );	
	    try( FastqWriter writer = (new FastqWriterFactory()).newWriter(INPUT);) {			 
			 for (FastqRecord re : data)  writer.write(re);      	
	    }  
	}
	
	private int execute() throws Exception {
		String command = " -I " + INPUT.getAbsolutePath() + " -O " + OUTPUT.getAbsolutePath();
		return (new Executor(command, "org.qcmg.qmule.FixCutAdapter")).getErrCode();
	}
		
	@Test
	public void inValidTest() throws Exception{
  		
		//different length bwt seq and qual  
		try(BufferedWriter out = new BufferedWriter(new FileWriter(INPUT))) { out.write("@f1\nATGC\n+\nA");	} 		
		assertEquals(0 , execute());
		
		//missing qual 
		try(BufferedWriter out = new BufferedWriter(new FileWriter(INPUT))) { out.write("@f1\nATGC\n");	} 
		assertEquals(1 , execute());
		
		//two seq line
		try(BufferedWriter out = new BufferedWriter(new FileWriter(INPUT))) { out.write("@f1\n\nATGC\n+\nA");	} 		
		assertEquals(1 , execute());
		
		//two records but one is wrong
		try(BufferedWriter out = new BufferedWriter(new FileWriter(INPUT))) { out.write("@f1\nATGC\n+\nA\n@f2\nATGC\n");	} 
		assertEquals(1 , execute());		
	}
	
	@Test
	public void emptyTest() throws Exception{
		
		assertEquals(0 , execute());
		
		int i = 0;
		try (FastqReader reader =  new FastqReader(OUTPUT)) {
			for (FastqRecord record : reader) {								
				if(record.equals(r1)) assertEquals(0 , i); // first record are same
				else if(record.getReadName().equals(r2.getReadName())) {
					//second record
					assertEquals(1 , i); 
					assertEquals(record.getReadString() , "N");  
                	assertEquals(record.getBaseQualityString(), "!");
				} else {
					//third record
					assertEquals(2 , i); 
					assertEquals(record.toFastQString() , "@others\nN\n+\n!"); 
 				}
				i ++;				
			}			
		}
 	}
}
