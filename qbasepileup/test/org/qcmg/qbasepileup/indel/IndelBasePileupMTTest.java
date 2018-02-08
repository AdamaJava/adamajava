package org.qcmg.qbasepileup.indel;

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

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbasepileup.Options;


public class IndelBasePileupMTTest {
	
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
	    	reference = this.getClass().getResource("/resources/example.fa").getFile();
	}
	
	@After
	public void after() throws IOException {
		reference = null;
	}

	@Test
	public void testIndelBasePileupMT() throws Exception {
		
		log =  testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "test.log";
		samFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.sam";
		bamFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.bam";
	    	createBamFile();
	    	
	    	snps = testFolder.newFile("indels.dcc1").getAbsolutePath();
		createIndelFile();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "output.dcc1";
		
		
        assertFalse(new File(log).exists());
        assertFalse(new File(output).exists());
        String[] args = {"--log", log, "-it", bamFile, "-in", bamFile, "-r", reference, "-os", output, "-is", snps, "-m", "indel", "--pindel"};
        Options options = new Options(args);
        new IndelBasePileupMT(options.getSomaticIndelFile(), options.getSomaticOutputFile(), null, false, options);
        assertTrue(new File(output).exists());
       
        BufferedReader reader = new BufferedReader(new FileReader(new File(output)));
        String line;
        int count = 0;
        while ((line = reader.readLine()) != null) {        	
	        	count++;
	        	String[] vals = line.split("\t");
	        	if (count == 2) {
	        		
		        	assertTrue(vals[23].startsWith("PASS;NNS;COVN12;HOMADJ"));
		        	assertTrue(vals[24].startsWith("0;1;1;0[0|0];0;1;0"));
		        	assertTrue(vals[25].startsWith("0;1;1;0[0|0];0;1;0;\"4 discontiguous GGTAATAAAAtATTGTAAAAC\""));
		    		assertEquals(28, vals.length);
	        	}
        }
        assertEquals(2, count);
        reader.close();
	}
	
	@Test
	public void testIndelBasePileupMTWithFilter() throws Exception {
		
		log =  testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "test.log";
		samFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.sam";
		bamFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.bam";
	    	createBamFile();
	    	
     	snps = testFolder.newFile("indels.dcc1").getAbsolutePath();
		createIndelFile();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "output.dcc1";
		
        assertFalse(new File(log).exists());
        assertFalse(new File(output).exists());
        String[] args = {"--log", log, "-it", bamFile, "-in", bamFile, "-r", reference, "-os", output, "-is", snps, "-m", "indel", "--pindel", "--filter", "option_SM > 30"};
        Options options = new Options(args);
        assertEquals("option_SM > 30", options.getFilterQuery());
        new IndelBasePileupMT(options.getSomaticIndelFile(), options.getSomaticOutputFile(), null, false, options);
        assertTrue(new File(output).exists());
       
        BufferedReader reader = new BufferedReader(new FileReader(new File(output)));
        String line;
        int count = 0;
        while ((line = reader.readLine()) != null) {        	
	        	count++;
	        	String[] vals = line.split("\t");
	        	if (count == 2) {
		        	assertTrue(vals[24].startsWith("0;0;0;0[0|0];0;0;0"));
		    		assertEquals(28, vals.length);
	        	}
        }
        assertEquals(2, count);
        reader.close();
	}
	
	@Test
	public void testIndelBasePileupByChrMT() throws Exception {
		
		log =  testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "test.log";
		samFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.sam";
		bamFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.bam";
	    	createBamFile();
	    	
     	snps = testFolder.newFile("indels.dcc1").getAbsolutePath();
		createIndelFile();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "output.dcc1";
		
        assertFalse(new File(log).exists());
        assertFalse(new File(output).exists());
        String[] args = {"--log", log, "-it", bamFile, "-in", bamFile, "-r", reference, "-os", output, "-is", snps, "-m", "indel", "--pindel"};
        Options options = new Options(args);
        
        new IndelBasePileupByChrMT(options.getSomaticIndelFile(), options.getSomaticOutputFile(), null, false, options);
        assertTrue(new File(output).exists());
       
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(output)));) {
	        String line;
	        int count = 0;
	        while ((line = reader.readLine()) != null) {        	
		        	count++;
		        	if (count == 2) {
			        	String[] vals = line.split("\t");
			        	assertTrue(vals[23].startsWith("PASS;NNS;COVN12;HOMADJ"));
			        	assertEquals("0;1;1;0[0|0];0;1;0", vals[24]);
			        	assertTrue(vals[25].startsWith("0;1;1;0[0|0];0;1;0;\"4 discontiguous GGTAATAAAAtATTGTAAAAC\""));
			    		assertEquals(28, vals.length);
		        	}
	        }
	        assertEquals(2, count);
        }
	}
	
	@Test
	public void testIndelBasePileupByChrMTWithFilter() throws Exception {
		
		log =  testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "test.log";
		samFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.sam";
		bamFile = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.bam";
	    	createBamFile();
	    	
     	snps = testFolder.newFile("indels.dcc1").getAbsolutePath();
		createIndelFile();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "output.dcc1";
		
        assertFalse(new File(log).exists());
        assertFalse(new File(output).exists());
        String[] args = {"--log", log, "-it", bamFile, "-in", bamFile, "-r", reference, "-os", output, "-is", snps, "-m", "indel", "--pindel", "--filter", "option_SM > 30"};
        Options options = new Options(args);
        assertEquals("option_SM > 30", options.getFilterQuery());
        new IndelBasePileupByChrMT(options.getSomaticIndelFile(), options.getSomaticOutputFile(), null, false, options);
        assertTrue(new File(output).exists());
       
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(output)));) {
	        String line;
	        int count = 0;
	        while ((line = reader.readLine()) != null) {        	
		        	count++;
		        	if (count == 2) {
			        	String[] vals = line.split("\t");
			        	assertEquals("0;0;0;0[0|0];0;0;0", vals[24]);
			    		assertEquals(28, line.split("\t").length);
		        	}
	        }
	        assertEquals(2, count);
        }
	}
	
	
	private void createBamFile() throws IOException {
		
		createSAMFile();
		
		try (SamReader reader =  SAMFileReaderFactory.createSAMFileReader(new File(samFile)) ){
	        SAMFileHeader header = reader.getFileHeader();
	        
			SAMFileWriterFactory factory = new SAMFileWriterFactory();
			factory.setCreateIndex(true);
			SAMFileWriter writer = factory.makeBAMWriter(header, false, new File(testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "input.bam"));
			
			for (SAMRecord r: reader) {
				writer.addAlignment(r);
			}
			 
			writer.close(); 
		}
	}

	private void createSAMFile() throws IOException {
		final List<String> data = new ArrayList<>();
		data.addAll(createSamHeader(SortOrder.coordinate));
		data.addAll(createSamBody());

		try (BufferedWriter out = new BufferedWriter(new FileWriter(samFile));) {
			for (final String line : data) {
				out.write(line + "\n");
			}
		}
	}	
	
	private static Collection<String> createSamHeader(SortOrder sort) {
		List<String> data = new ArrayList<>();
        data.add("@HD	VN:1.0	GO:none	SO:"+ sort.name());	        
        data.add("@SQ	SN:chr1	LN:135534747");          
        data.add("@RG	ID:2012060803293054	PL:ILLUMINA	PU:lane_3	LB:Library_20120511_C	SM:Colo-829");
        data.add("@RG	ID:2012060803293054	PL:ILLUMINA	PU:lane_2	LB:Library_20120511_C	SM:Colo-829");
        return data;
	}
	
	public static List<String> createSamBody() {
		List<String> data = new ArrayList<>();
		data.add("HWI-ST1240:61:D12U4ACXX:5:1111:7824:75112	83	chr1	51	0	34M1I66M	=	451	400	GTTTGGGTAATAAAAATTGTAAAACTTTTTTTTCTTTTTTTTTTGAGACAGAGTCTCCCTCTGTCGCCAGGCTGAAGTGCAGTGGCGCAATCTCGGCTCAC	DDDDDDDEDDDDDDDDDDDDDDC@DDDDDDDDDDDDDDDCEDEEFFFFFFHHHHJJJJJJJJJIJIJJJJJJJIJJJJJJJJJIJJJJHHHHHFFFFFCCC	X0:i:2	X1:i:1	XA:Z:chr19,-103413,34M1I66M,1;chr15,+102468929,57M1I43M,1;	ZC:i:11	MD:Z:13T86	RG:Z:2012060803293054	XG:i:1	AM:i:0	NM:i:2	SM:i:0	XM:i:1	XO:i:1	XT:A:R");
		data.add("HWI-ST1240:61:D12U4ACXX:5:1111:7824:75112	1107	chr1	51	0	34M1I66M	=	451	400	GTTTGGGTAATAAAAATTGTAAAACTTTTTTTTCTTTTTTTTTTGAGACAGAGTCTCCCTCTGTCGCCAGGCTGAAGTGCAGTGGCGCAATCTCGGCTCAC	DDDDDDDEDDDDDDDDDDDDDDC@DDDDDDDDDDDDDDDCEDEEFFFFFFHHHHJJJJJJJJJIJIJJJJJJJIJJJJJJJJJIJJJJHHHHHFFFFFCCC	X0:i:2	X1:i:1	XA:Z:chr19,-103413,34M1I66M,1;chr15,+102468929,57M1I43M,1;	ZC:i:11	MD:Z:13T86	RG:Z:2012060803293054	XG:i:1	AM:i:0	NM:i:2	SM:i:0	XM:i:1	XO:i:1	XT:A:R");
		return data;
    }	

	private void createIndelFile() throws IOException {
		try (BufferedWriter w = new BufferedWriter(new FileWriter(new File(snps)));) {
			w.write("analysis_id\tanalyzed_sample_id\tmutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\tmutation\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\tverification_platform\txref_ensembl_var_id\tnote\tQCMGflag\tND\tTD\tNNS\tFlankSeq\n");
			w.write("id\ttest\ttest_ind1\t2\tchr1\t85\t86\t1\t-999\t-999\t-\t-999\tT\t-/T\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--\n");
		}
	}
}

