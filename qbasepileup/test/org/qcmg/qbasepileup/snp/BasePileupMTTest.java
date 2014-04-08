package org.qcmg.qbasepileup.snp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.snp.SnpBasePileupMT;

public class BasePileupMTTest {
	
	final static String FILE_SEPERATOR = System.getProperty("file.separator");
	String log;
	String input;
	String reference;
	String output;
	String snps;
	String samFile;
	String bamFile;
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws IOException {
		log =  testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "test.log";
		samFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.sam";
		bamFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.bam";
    	createBamFile();
    	reference = testFolder.newFile("reference.fa").getAbsolutePath();
		testFolder.newFile("reference.fa.fai").getAbsolutePath();
		snps = testFolder.newFile("snps.dcc").getAbsolutePath();
		createSNPFile();
		createReferenceFile();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "pileup.txt";		
	}



	@Test
	public void testBasePileupMT() throws Exception {
		
        assertFalse(new File(log).exists());
        assertFalse(new File(output).exists());
        String[] args = {"--log", log, "-i", bamFile, "-r", reference, "-o", output, "-s", snps, "-f", "tab"};
        Options options = new Options(args);
        new SnpBasePileupMT(options);
        assertTrue(new File(output).exists());
       
        BufferedReader reader = new BufferedReader(new FileReader(new File(output)));
        String line;
        int count = 0;
        while ((line = reader.readLine()) != null) {
        	count++;
        	 if (count == 1) {
        		 assertEquals("##qbasepileup version 1.0", line);
        	 } else if (count == 2) {
        		 assertTrue(line.startsWith("ID"));
        	 } else if (count ==3) {
        		 assertTrue(line.startsWith(""));
        		 assertEquals(22, line.split("\t").length);
        		 assertEquals("1", line.split("\t")[16]);
        		 assertEquals("1", line.split("\t")[8]);
        		 assertEquals("0", line.split("\t")[9]);
        	 }
        }
        assertEquals(3, count);
        reader.close();
	}
	
	@Test
	public void testBasePileupMTWithFilter() throws Exception {
		
        assertFalse(new File(log).exists());
        assertFalse(new File(output).exists());
        String[] args = {"--log", log, "-i", bamFile, "-r", reference, "-o", output, "-s", snps, "-f", "tab", "-filter", "option_SM > 30"};
        Options options = new Options(args);
        assertEquals("option_SM > 30", options.getFilterQuery());
        new SnpBasePileupMT(options);
        assertTrue(new File(output).exists());
       
        BufferedReader reader = new BufferedReader(new FileReader(new File(output)));
        String line;
        int count = 0;
        while ((line = reader.readLine()) != null) {
        	count++;
        	 if (count == 1) {
        		 assertEquals("##qbasepileup version 1.0", line);
        	 } else if (count == 2) {
        		 assertTrue(line.startsWith("ID"));
        	 } else if (count ==3) {
        		 assertTrue(line.startsWith(""));
        		 assertEquals(22, line.split("\t").length);
        		 assertEquals("0", line.split("\t")[16]);
        		 assertEquals("0", line.split("\t")[8]);
        		 assertEquals("0", line.split("\t")[9]);
        	 }
        }
        assertEquals(3, count);
        reader.close();
	}
	
	
	private void createBamFile() throws IOException {
		
		createSAMFile();
		
        SAMFileReader reader = new SAMFileReader(new File(samFile));
        SAMFileHeader header = reader.getFileHeader();
        
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		factory.setCreateIndex(true);
		SAMFileWriter writer = factory.makeBAMWriter(header, false, new File(testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.bam"));
		
		for (SAMRecord r: reader) {
			writer.addAlignment(r);
		}
		reader.close();
		writer.close(); 

	}

	private void createSAMFile() throws IOException {
		final List<String> data = new ArrayList<String>();
		data.addAll(createSamHeader(SortOrder.coordinate));
		data.addAll(createSamBody());

		BufferedWriter out;
		out = new BufferedWriter(new FileWriter(samFile));
		for (final String line : data) {
			out.write(line + "" + "\n");
		}
		out.close();

	}	
	
	private static Collection<String> createSamHeader(
			SortOrder sort) {
		 final List<String> data = new ArrayList<String>();
	        data.add("@HD	VN:1.0	GO:none	SO:"+ sort.name());	        
	        data.add("@SQ	SN:chr1	LN:135534747");          
	        data.add("@RG	ID:2012060803293054	PL:ILLUMINA	PU:lane_3	LB:Library_20120511_C	SM:Colo-829");
	        data.add("@RG	ID:2012060803293054	PL:ILLUMINA	PU:lane_2	LB:Library_20120511_C	SM:Colo-829");
	        return data;
	}
	
	public static List<String> createSamBody() {
		final List<String> data = new ArrayList<String>();
		 
	     data.add("HWI-ST1240:47:D12NAACXX:2:2315:11796:5777	83	chr1	6	29	94M7S	=	106	100	ACTTCAGATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTA	DCC=EDDDDDEDEEEEEFFFFFHHHFHJJJJIHIHJIIJJJJIIJJJIGIJIJJJIJJIJJJJJJJJJJIIJJJJJJJJJIIJJJJJJHHHHHFFFFFCCC	ZC:i:10	MD:Z:94	RG:Z:2012060803293054	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M");
	     data.add("HWI-ST1240:47:D12NAACXX:2:2315:11796:5778	83	chr1	7	29	94M7S	=	107	100	ACTTCAGATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTA	DCC=EDDDDDEDEEEEEFFFFFHHHFHJJJJIHIHJIIJJJJIIJJJIGIJIJJJIJJIJJJJJJJJJJIIJJJJJJJJJIIJJJJJJHHHHHFFFFFCCC	ZC:i:10	MD:Z:94	RG:Z:2012060803293054	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M");
			
        return data;
    }
	


	private void createSNPFile() throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(snps)));
		
		w.write("test\tchr1\t6\t6\n");
		w.close();
	}
	
	private void createReferenceFile() throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(new File(reference)));
		
		w.write(">chr1");
		w.write("AAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		w.close();
		
		BufferedWriter w2 = new BufferedWriter(new FileWriter(new File(reference + ".fai")));
		
		w2.write("chr1\t95\t6\t95\t96\n");
		w2.close();
	}
	

}
