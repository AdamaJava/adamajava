package org.qcmg.qio.gff3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qio.gff3.Gff3Record;


public class Gff3RecordTest {
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	
	private static final String r1 = "chr1	simple_segmenter.pl[v2940]	fill	1	14166	.	.	.	ID=gnl|fill";
	private static final String r2 = "chr1	simple_segmenter.pl[v2940]	exon_3_100	14167	14266	.	+	.	ID=gnl|exon_3_100";

	@Test
	public void ReaderTest() throws IOException {
		File gff3File = tempFolder.newFile("wigglePileupTest.gff3");
		createGFF3File(gff3File);
		
		Gff3Record records[] = new Gff3Record[2];
		try(Gff3FileReader reader =  new Gff3FileReader(gff3File);) {
			int no = 0;
			for (Gff3Record record : reader) {
				Assert.assertTrue(no < 2);				 
				records[no] = record;
				no ++;
			}			
		} 
			
		Assert.assertEquals(records[0].toString(), r1);
		Assert.assertEquals(records[1].toString(), r2);	
	}
	
	@Test
	public void RecordTest1() {
		
		Gff3Record record = new Gff3Record(r2);
		 
		Assert.assertEquals(record.getRawData(), r2);
		Assert.assertEquals(record.getSeqId() , "chr1");
		Assert.assertEquals(record.getSource(), "simple_segmenter.pl[v2940]");
		Assert.assertEquals(record.getType() , "exon_3_100");
		Assert.assertEquals(record.getStart() , 14167);
		Assert.assertEquals(record.getEnd() , 14266);
		Assert.assertEquals(record.getScore() , ".");
		Assert.assertEquals(record.getStrand() , "+");
		Assert.assertEquals(record.getPhase() , ".");
		Assert.assertEquals(record.getAttributes() , "ID=gnl|exon_3_100");		
	}
	
	@Test
	public void RecordTest2() {
		
		Gff3Record record = new Gff3Record();
		 
		record.setSeqId("chr1");
		record.setSource("simple_segmenter.pl[v2940]");
		record.setType("exon_3_100");
		record.setStart(14167);
		record.setEnd(14266);
		record.setScore(".");
		record.setStrand("+");
		record.setPhase(".");
		record.setAttributes("ID=gnl|exon_3_100");		
		
		Assert.assertEquals(record.getRawData(), null);
		Assert.assertEquals(record.toString(), r2);
		
	}	
	
	//cp from WiggleFromPileupTest
	private void createGFF3File(File pileupFile) throws IOException {
		
		OutputStream os = new FileOutputStream(pileupFile);
		
		PrintStream ps = new PrintStream(os);		
		
		ps.println("##gff-version 3");
		ps.println("# Created by: simple_segmenter.pl[v2940]");
		ps.println("# Created on: Tue May 24 01:48:54 2011");
		ps.println("# Commandline: -v -g -l -i SureSelect_All_Exon_50mb_filtered_exons_1-200_20110524.gff3 -o SureSelect_All_Exon_50mb_filtered_exons_1-200_20110524_shoulders.gff3 -f exon,100,100,100 -f highexon,300 -f lowexon");
		ps.println(r1);
		ps.println(r2);
 		ps.close();
		os.close();
	}	
}
