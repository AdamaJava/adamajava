package org.qcmg.cnv;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.*;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.log.QLogger;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;

public class MtCountsTest {
	
	public static final String[] INPUTs = { "input.normal.bam","input.tumor.bam"};
	 

	@After
	public void deleteIO(){
		for (String f : INPUTs)
			new File(f).delete();
	 
		
	}
	@Before
	public void createIO(){
		TestFiles.CreateBAM(INPUTs[0], INPUTs[1]);
	}
	
	@Test
	public void main() throws Exception{
		String output = "output.counts";
		String[] ids = {"tumor" ,"normal"};
		String[] args = {"-i", INPUTs[0],"-i", INPUTs[1], "--id", ids[0] , "--id", ids[1], "-o" , output};
		Main.main(args);
		
	}

}
