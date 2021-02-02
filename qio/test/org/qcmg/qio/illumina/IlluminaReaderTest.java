package org.qcmg.qio.illumina;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class IlluminaReaderTest {
	private static String invalidInputString = "chr12	126890980	-	rs1000000	0.8379	C	C	hom	[T/C]";
	
	private static final String rawInputString = "rs10002311	4802094023_R02C01	A	A	" +
			"0.7992			14	93259	0	A	A	T	T	A	A	4	77661528	" +
			"0.7895	0.5494	[T/G]	BOT	TOP		0.109	0.409	0.349	0.060	4887	1511	0.0290	0.1235";
	
	private static final String rawInputString2 = "rs6680706	5760640025_R02C01	A	G	" +
			"0.7956			70	900225	0	T	C	T	C	A	B	1	4231843	" +
			"0.7872	1.0000	[T/C]	BOT	BOT		0.515	1.911	0.933	0.977	12110	13151	0.4745	0.0471";


	
	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();
	
	@Test
	public void testParseIDInvalid() throws Exception {
		
		File illuminaFile = tmpFolder.newFile("illumina");
		IlluminaFileReader reader = new IlluminaFileReader(illuminaFile);
		
		// test empty string
		try {
			reader.getRecord("");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {}
		
		// string that does not start with 'chr' 
		try {
			reader.getRecord("testing testing 123");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {}
		
		// string containing 'chr' but not at the start..
		try {
			reader.getRecord("this is a chr1 test");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {}
		
		// string that is not the right length
		try {
			reader.getRecord(invalidInputString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals(true, e.getMessage().startsWith("Bad Illumina data format"));
		}
	}
	
	
	@Test
	public void testParseRecords() throws Exception {
		
		File illuminaFile = tmpFolder.newFile("illumina");
		IlluminaFileReader reader = new IlluminaFileReader(illuminaFile);
		
		// real record
		// inputString = "chr12	126890980	-	rs1000000	0.8379	C	C	hom	[T/C]	G__C/T";	
		IlluminaRecord record = reader.getRecord(rawInputString);
		Assert.assertNotNull(record);
		Assert.assertEquals("4", record.getChr());	// we now parse chr and position for Illumina records 
		Assert.assertEquals(77661528, record.getStart());
		Assert.assertEquals("rs10002311", record.getSnpId());
		Assert.assertEquals(0.7992f, record.getGCScore(), 0.00000);
		Assert.assertEquals('T', record.getFirstAllele());
		Assert.assertEquals('T', record.getSecondAllele());
		Assert.assertTrue(record.isHom());
		Assert.assertEquals("[T/G]", record.getSnp());
	}
	
	@Test
	public void testNextRecord() throws Exception {
		// create tmp illumina file
		File illuminaFile = tmpFolder.newFile("illumina");
		generateIllumiaFile(illuminaFile);		
		IlluminaFileReader reader = new IlluminaFileReader(illuminaFile);
		
		//only one record
		for(IlluminaRecord rec: reader) {
			Assert.assertNotNull(rec);
			Assert.assertEquals("cnvi0000657", rec.getSnpId());
		}
		 

	}
	
	private void generateIllumiaFile(File file) throws IOException {
		FileWriter writer = new FileWriter(file);
		writer.write("[Header]\n");
		writer.write("GSGT Version    1.8.4\n");
		writer.write("Processing Date 8/12/2011 8:41 PM\n");
		writer.write("Content         HumanOmni1-Quad_v1-0_H.bpm\n");
		writer.write("Num SNPs        1134514\n");
		writer.write("Total SNPs      1134514\n");
		writer.write("Num Samples     259\n");
		writer.write("Total Samples   260\n");
		writer.write("File    77 of 259\n");
		writer.write("[Data]\n");
		writer.write("SNP Name	Sample ID	Allele1 - Top	Allele2 - Top	GC Score	Sample Name	Sample Group	Sample Index	SNP Index	SNP Aux	Allele1 - Forward	Allele2 - Forward	Allele1 - Design	Allele2 - Design	Allele1 - AB	Allele2 - AB	Chr	Position	GT Score	Cluster Sep     SNP	ILMN Strand     Customer	Strand	Top Genomic Sequence	Theta	R	X	Y"
+ "X Raw   Y Raw   B Allele Freq   Log R Ratio\n");
		writer.write("cnvi0000657	5636391030_R02C01	-	-	0.0000			78	127	0	-	-	-	-	-	-	6	160513181	0.0000	0.0000	[A/G]	TOP	TOP\t\t"
             + "0.021	1.675	1.621	0.054	17348	1140	0.0100	-0.1054\n");
		writer.flush();
	}
}
