package org.qcmg.picard;


import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qio.fasta.FastaRecord;
import org.qcmg.qio.record.RecordWriter;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.FastaSequenceIndexCreator;
import htsjdk.samtools.SAMFileHeader.SortOrder;

public class SAMWriterFactoryTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
			
	@Test
	public void cramFileTest() throws IOException {
		File bamFile = testFolder.newFile("testValidHeaderValidBody.cram");	
		
		//exception since missing reference file
		try {
			SAMFileReaderFactoryTest.getBamFile(bamFile, true, true, true);	
			fail("Should have thrown an exception");
		} catch(IllegalArgumentException e) {}
		
		//exception since missing reference file
		try {
			makeCramFile(bamFile, null);
			fail("Should have thrown an exception");
		} catch(IllegalArgumentException e) {}
			
		File ref = testFolder.newFile("testcram.fa");	
		ref = new File("testcram.fa");
		createReference(ref);
		//exception since missing reference file
		//debug
		bamFile = new File("testValidHeaderValidBody.cram");
		makeCramFile(bamFile, ref);		
	}
	
	static void makeCramFile(File bamFile, File ref) {
		SAMFileHeader header = createHeader();
		SAMWriterFactory factory = new SAMWriterFactory(header, false, bamFile, null, 0, true, false, -1, ref);		
		try( SAMFileWriter writer = factory.getWriter();){	
			for (SAMRecord s : getRecords(header)) {				
				writer.addAlignment(s);						
			}
		} 
	}
	
	static List<SAMRecord> getRecords(SAMFileHeader header) {
		List<SAMRecord> records = new ArrayList<SAMRecord>();
		
		//HS2000-152_756:1:1316:11602:65130	0	chrA	1	0	40M	*	0	0	AATAAAAATAAAAAAATACAGACAAGGCGGAAGAAATGAA	B@??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?
		SAMRecord sam = new SAMRecord(header);
		sam.setAlignmentStart(1);
		sam.setReferenceName("chrA");
		sam.setReadName("HS2000-152_756:1:1316:11602:65130");
		sam.setReadString("AATAAAAATAAAAAAATACAGACAAGGCGGAAGAAATGAA");
		sam.setBaseQualityString("B@??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?");
		sam.setCigarString("40M");
		records.add(sam);
		
		//HS2000-152_756:1:1316:11602:65138	0	chrA	3	0	38M	*	0	0	TAAAAATAAAAAAATACAGACAAGGCGGAAGAAATGAA	??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?
		sam = new SAMRecord(header);
		sam.setAlignmentStart(3);
		sam.setReferenceName("chrA");
		sam.setReadName("HS2000-152_756:1:1316:11602:65138");
		sam.setReadString("TAAAAATAAAAAAATACAGACAAGGCGGAAGAAATGAA");
		sam.setBaseQualityString("??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?");
		sam.setCigarString("38M");
		records.add(sam);		
		return records;
	}	
	
	static SAMFileHeader createHeader() {
		SAMFileHeader header = new SAMFileHeader();
		header.setSortOrder(SortOrder.coordinate);
		
		SAMProgramRecord bwaPG = new SAMProgramRecord("bwa");
		bwaPG.setProgramName("bwa");
		bwaPG.setProgramVersion("0.6.1-r104");
		header.addProgramRecord(bwaPG);
//		"@PG	ID:bwa	PN:bwa	zc:6	VN:0.6.1-r104"+
							
		SAMReadGroupRecord rgRec = new SAMReadGroupRecord("ID");
		rgRec.setAttribute("PG", "tmap");
		header.addReadGroup(rgRec);
		
		// looks like we need this to be specifically defined
		SAMSequenceDictionary seqDict = new SAMSequenceDictionary();
		SAMSequenceRecord seqRec1 = new SAMSequenceRecord("chrA", 160);
		SAMSequenceRecord seqRec2 = new SAMSequenceRecord("chrB", 660);
		seqDict.addSequence(seqRec1);
		seqDict.addSequence(seqRec2);
		header.setSequenceDictionary(seqDict);
		
		return header;
	}	
		
	private void createReference(File reference) throws IOException {		
		//string 40x4=160 bases	
		final String chrA = "AATAAAAATAAAAAAATACAGACAAGGCGGAAGAAATGAA" +
				   "AAAGGGCCACAGAGAAGTAGCCACATCTTGAAGGAATAAA" +
				   "CAACCAAGAAGGAATAACAACAGCCTCAGTCTAGGCAAAA" +
				   "ACCGACTCACTCAATCCTGAGATAGCAACGAGGAGGTAAA";
		try(RecordWriter<FastaRecord> writer = new RecordWriter<>(reference);){	 			
			FastaRecord record = new FastaRecord(">chrA", chrA);			
			writer.add(record);
		} 	
		
		//index file (aln.fa.fai) is compulsory for reference file (aln.fa)
		FastaSequenceIndexCreator.create(reference.toPath(), true);
	}			
}
