package org.qcmg.illumina;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class IlluminaFileTestTest {
	private static String invalidInputString = "chr12	126890980	-	rs1000000	0.8379	C	C	hom	[T/C]";
	
	private static final String rawInputString = "rs10002311	4802094023_R02C01	A	A	" +
			"0.7992			14	93259	0	A	A	T	T	A	A	4	77661528	" +
			"0.7895	0.5494	[T/G]	BOT	TOP		0.109	0.409	0.349	0.060	4887	1511	0.0290	0.1235";
//	private static final String rawInputString2 = "rs10013427      4802094023_R02C01       G       G       " +
//			"0.9080                  14      94874   0       G       G       G       G       B       B       4       169381756       " +
//			"0.8710  1.0000  [A/G]   TOP     TOP             0.940   0.548   0.047   0.501   1145    11978   0.9675  -0.5171";
	
	private static final String rawInputString2 = "rs6680706	5760640025_R02C01	A	G	" +
			"0.7956			70	900225	0	T	C	T	C	A	B	1	4231843	" +
			"0.7872	1.0000	[T/C]	BOT	BOT		0.515	1.911	0.933	0.977	12110	13151	0.4745	0.0471";

	private static File illuminaFile = null;

	@ClassRule
	public static TemporaryFolder testFolder = new TemporaryFolder();
	
	
	@BeforeClass
	public static void setup() throws IOException {
		illuminaFile = testFolder.newFile("illumina");
		generateIllumiaFile(illuminaFile);

	}
	
	@Test
	public void testParseIDInvalid() throws Exception {
		IlluminaFileReader reader = new IlluminaFileReader(illuminaFile);

		try {
			// test empty string
			reader.readRecord("");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {}
		
		// string that does not start with 'chr' 
		try {
			reader.readRecord("testing testing 123");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {}
		
		// string containing 'chr' but not at the start..
		try {
			reader.readRecord("this is a chr1 test");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {}
		
		// string that is not the right length
		try {
			reader.readRecord(invalidInputString);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals(true, e.getMessage().startsWith("Bad Illumina data format"));
		}
		
		reader.close();
	}	
	
	
	@Test
	public void testParseRecords() throws Exception {
		IlluminaFileReader reader = new IlluminaFileReader(illuminaFile);

		// real record
		// inputString = "chr12	126890980	-	rs1000000	0.8379	C	C	hom	[T/C]	G__C/T";
		IlluminaRecord record = reader.readRecord(rawInputString);
		Assert.assertNotNull(record);
		Assert.assertEquals("4", record.getChr());	// we now parse chr and position for Illumina records 
		Assert.assertEquals(77661528, record.getStart());
		
		Assert.assertEquals("rs10002311", record.getSnpId());
		Assert.assertEquals(0.7992f, record.getGCScore(), 0.00000);
		Assert.assertEquals('T', record.getFirstAllele());
		Assert.assertEquals('T', record.getSecondAllele());
		Assert.assertTrue(record.isHom());
		Assert.assertEquals("[T/G]", record.getSnp());
		
		reader.close();
	}
	
	@Test
	public void testNextRecord() throws Exception {
		// create tmp illumina file
		try(IlluminaFileReader reader = new IlluminaFileReader(illuminaFile)){
			int count = 0;
			for(IlluminaRecord re : reader) {
				 
				if(count == 0) {
					//add more
					Assert.assertEquals("cnvi0000657", re.getSnpId());				
					Assert.assertEquals(0.0f, re.getGCScore(), 0.00000);	
					Assert.assertEquals('-', re.getFirstAlleleForward());						
					Assert.assertEquals('-', re.getSecondAlleleForward());					
					Assert.assertEquals('-', re.getFirstAllele());					
					Assert.assertEquals('-', re.getSecondAllele());					
					Assert.assertEquals(true, re.isHom());					
					Assert.assertEquals('-', re.getFirstAlleleCall());					
					Assert.assertEquals('-', re.getSecondAlleleCall());					
					Assert.assertEquals("6", re.getChr());
					Assert.assertEquals(160513181, re.getStart());
					Assert.assertEquals("[A/G]", re.getSnp());
					Assert.assertEquals("TOP", re.getStrand());
					Assert.assertEquals(17348, re.getRawX());
					Assert.assertEquals(1140, re.getRawY());					
					Assert.assertEquals(0.01f, re.getbAlleleFreq(), 0.00000);						
					Assert.assertEquals(-0.1054f, re.getLogRRatio(), 0.00000);					
				} else {
					Assert.assertEquals(4231843, re.getStart());
				}
				
				count ++;
			}			
		}
	}
	
	@Test
	public void testHeader() throws Exception {
		// create tmp illumina file
		try(IlluminaFileReader reader = new IlluminaFileReader(illuminaFile)){
			java.util.List<String> header = reader.getHeader();
			//first header line
			assertTrue(header.get(0).startsWith(IlluminaFileReader.HEADER_LINE));
			//last header line
			assertTrue(header.get(header.size()-1).startsWith("SNP Name"));
			//second last line
			assertTrue(header.get(header.size()-2).startsWith(IlluminaFileReader.DATA_LINE));			
		}
	}
	
	private static void generateIllumiaFile(File file) throws IOException {
		try(FileWriter writer = new FileWriter(file);){
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
			writer.write(rawInputString2);
			writer.flush();
		}
	}
}
