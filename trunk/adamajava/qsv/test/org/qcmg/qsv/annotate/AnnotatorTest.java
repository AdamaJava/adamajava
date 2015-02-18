package org.qcmg.qsv.annotate;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.DefaultSAMRecordFactory;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.util.TestUtil;

public class AnnotatorTest {   
   
    private List<SAMRecord> records = new ArrayList<SAMRecord>();
    private List<RunTypeRecord> sequencingRuns;
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	private File tumourBam;
    
    @Before
    public void setUp() throws IOException, Exception {
    	records = new ArrayList<SAMRecord>();
        tumourBam = testFolder.newFile("test.bam");
        TestUtil.createBamFile(tumourBam.getCanonicalPath(), null, SortOrder.coordinate);
        SAMFileReader read = new SAMFileReader(tumourBam);
        
        for (SAMRecord r : read) {
            records.add(r);
        }
        
        read.close();
    	setupSequencingRuns();
    }
    
    private void setupSequencingRuns() {
    	sequencingRuns = new ArrayList<RunTypeRecord>();
		RunTypeRecord r = new RunTypeRecord("20110221052813657", 300, 2700, "seq_mapped");
		sequencingRuns.add(r);
	}
    
    
    @Test
    public void setNHAttributeWhenMappedWithBWAMEM() throws IOException, Exception {
    	
    	//HS2000-1262_116:6:1208:4223:25933	99	chr1	10012	40	97M3S	=	10258	344	CTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACC
    	//CTAACCCTAACCCTAACCCTAACCCCAA	CCCFFFFFHHHHHIJJJJJHIIJJJJJJJJJIJJJIJ?GHIJJGHIJJJGGIJEHCGGHJHAAEEBD>D@>AC;C?=?B95AB?B9?CA??#########	ZC:i:6	MD:Z:97	PG:Z:MarkDuplicates.7
    //			RG:Z:20141216163713333	NM:i:0	AS:i:97	XS:i:98
    	
    		SAMReadGroupRecord rg = new SAMReadGroupRecord("20141216163713333");
    		rg.setDescription("20141216163713333");
    		List<SAMReadGroupRecord> rgs = new ArrayList<SAMReadGroupRecord>();
    		rgs.add(rg);
    		SAMFileHeader header = new SAMFileHeader();
    		header.setReadGroups(rgs);
    		SAMRecord rec = new DefaultSAMRecordFactory().createSAMRecord(header);
    		rec.setReadName("HS2000-1262_116:6:1208:4223:25933");
    		rec.setFlags(99);
    		rec.setReferenceName("chr1");
    		rec.setAlignmentStart(10012);
    		rec.setMappingQuality(40);
    		rec.setCigarString("97M3S");
    		rec.setMateReferenceName("=");
    		rec.setMateAlignmentStart(10258);
    		rec.setInferredInsertSize(344);
    		rec.setReadString("CTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCCAA");
    		rec.setBaseQualityString("CCCFFFFFHHHHHIJJJJJHIIJJJJJJJJJIJJJIJ?GHIJJGHIJJJGGIJEHCGGHJHAAEEBD>D@>AC;C?=?B95AB?B9?CA??#########");
    		rec.setAttribute("ZC", 6);
    		rec.setAttribute("MD", 97);
    		rec.setAttribute("PG", "MarkDuplicates.7");
    		rec.setAttribute("RG", "20141216163713333");
    		rec.setAttribute("NM", 0);
    		rec.setAttribute("AS", 97);
    		rec.setAttribute("XS", 98);
    		
    		
    		assertEquals("HS2000-1262_116:6:1208:4223:25933", rec.getReadName());
    		assertEquals(99, rec.getFlags());
    		assertEquals("chr1", rec.getReferenceName());
    		// only care about the attribures really...
    		assertEquals(6, rec.getAttribute("ZC"));
    		assertEquals(97, rec.getAttribute("MD"));
    		assertEquals("MarkDuplicates.7", rec.getAttribute("PG"));
    		assertEquals("20141216163713333", rec.getAttribute("RG"));
    		assertEquals(0, rec.getAttribute("NM"));
    		assertEquals(97, rec.getAttribute("AS"));
    		assertEquals(98, rec.getAttribute("XS"));
    		
    		
//    	   	sequencingRuns = new ArrayList<RunTypeRecord>();
    		RunTypeRecord r = new RunTypeRecord("20141216163713333", 300, 2700, "seq_mapped");
    		sequencingRuns.add(r);
    		
    		 Annotator annotator = new Annotator(250, 2790, testFolder.newFile("testFile"), "pe", sequencingRuns, "pe", "bwa");
    		 annotator.setNHAttribute("bwa", rec);
    		 assertEquals(0, rec.getAttribute("NH"));
    		 
    		 // try running the whole annotation
    		 rec.setAttribute("NH", null);
    		 annotator.annotate(rec);
    		 assertEquals("AAA", rec.getAttribute("ZP"));
    		 
    		 
    		 //reset the ZP and NH atributes and try again with bwa-mem
    		 rec.setAttribute("ZP", null);
    		 rec.setAttribute("NH", null);
    		 
    		 annotator.setNHAttribute("bwa-mem", rec);
    		 assertEquals(1, rec.getAttribute("NH"));
    		 
    		 rec.setAttribute("NH", null);
    		 // try running the whole annotation
    		 annotator.annotate(rec);
    		 assertEquals("AAA", rec.getAttribute("ZP"));
    		 
    		 //clean up
    		 sequencingRuns.remove(r);
    }
    
    @Test
    public void annotateRecordWhenMappedWithBWAMEM() throws IOException, Exception {
    	
    	//HS2000-1262_116:5:1101:15754:19035	81	chr4	181878334	60	100M	=	181876623	-1811	CTTGCATGTTACCCAGTTGTGAAAGGAGATGTGATTCCTATTGCATAGGATTGCAG
    	//GGTGCACTCACTACCGCATCTGCAACACACCAGGCACTCAATAA	DCDDDCDDDCADDDDDEEEEEFEDEFFGHGHHHJJJJJJJIHIIJJJJJIIJJJIJJJGGD:HIG?HFDGIGJGHJJGIHFGFGCJJHHHHHFFDFFCCB	ZC:i:7	MD:Z:100
    			//PG:Z:MarkDuplicates.7	RG:Z:20141216163712514	NH:i:1	NM:i:0	ZP:Z:AAC	AS:i:100	XS:i:19
    	
    	SAMReadGroupRecord rg = new SAMReadGroupRecord("20141216163712514");
    	rg.setDescription("20141216163712514");
    	List<SAMReadGroupRecord> rgs = new ArrayList<SAMReadGroupRecord>();
    	rgs.add(rg);
    	SAMFileHeader header = new SAMFileHeader();
    	header.setReadGroups(rgs);
    	SAMRecord rec = new DefaultSAMRecordFactory().createSAMRecord(header);
    	rec.setReadName("HS2000-1262_116:5:1101:15754:19035");
    	rec.setFlags(81);
    	rec.setReferenceName("chr4");
    	rec.setAlignmentStart(181878334);
    	rec.setMappingQuality(60);
    	rec.setCigarString("100M");
    	rec.setMateReferenceName("=");
    	rec.setMateAlignmentStart(181876623);
    	rec.setInferredInsertSize(-1811);
    	rec.setReadString("CTTGCATGTTACCCAGTTGTGAAAGGAGATGTGATTCCTATTGCATAGGATTGCAGGGTGCACTCACTACCGCATCTGCAACACACCAGGCACTCAATAA");
    	rec.setBaseQualityString("DCDDDCDDDCADDDDDEEEEEFEDEFFGHGHHHJJJJJJJIHIIJJJJJIIJJJIJJJGGD:HIG?HFDGIGJGHJJGIHFGFGCJJHHHHHFFDFFCCB");
    	rec.setAttribute("ZC", 7);
    	rec.setAttribute("MD", 100);
    	rec.setAttribute("PG", "MarkDuplicates.7");
    	rec.setAttribute("RG", "20141216163712514");
    	rec.setAttribute("NM", 0);
    	rec.setAttribute("AS", 100);
    	rec.setAttribute("XS", 19);
    	
    	
    	assertEquals("HS2000-1262_116:5:1101:15754:19035", rec.getReadName());
    	assertEquals(81, rec.getFlags());
    	assertEquals("chr4", rec.getReferenceName());
    	// only care about the attribures really...
    	assertEquals(7, rec.getAttribute("ZC"));
    	assertEquals(100, rec.getAttribute("MD"));
    	assertEquals("MarkDuplicates.7", rec.getAttribute("PG"));
    	assertEquals("20141216163712514", rec.getAttribute("RG"));
    	assertEquals(0, rec.getAttribute("NM"));
    	assertEquals(100, rec.getAttribute("AS"));
    	assertEquals(19, rec.getAttribute("XS"));
    	
    	
//    	   	sequencingRuns = new ArrayList<RunTypeRecord>();
    	RunTypeRecord r = new RunTypeRecord("20141216163712514", 300, 1700, "seq_mapped");
    	sequencingRuns.add(r);
    	
    	Annotator annotator = new Annotator(250, 1790, testFolder.newFile("testFile"), "pe", sequencingRuns, "pe", "bwa");
    	
    	annotator.annotate(rec);
    	assertEquals(0, rec.getAttribute("NH"));
    	assertEquals("Z**", rec.getAttribute("ZP"));		// Z** are not considered discordant
    	
    	
    	//reset the ZP and NH atributes and try again with bwa-mem
    	rec.setAttribute("ZP", null);
    	rec.setAttribute("NH", null);
    	
    	annotator = new Annotator(250, 1790, testFolder.newFile("testFile"), "pe", sequencingRuns, "pe", "bwa-mem");
    	annotator.annotate(rec);
    	assertEquals("AAC", rec.getAttribute("ZP"));	// AAC are considered discordant
    	
    	//clean up
    	sequencingRuns.remove(r);
    }
    

	@Test
    public void testLMPAnnotate() throws Exception {
		//QSVParameters p = TestUtil.getQSVParameters(testFolder, testFolder.newFile("normal.bam").getAbsolutePath(), tumourBam.getAbsolutePath(), true, "none");
        Annotator lmp = new Annotator(250, 2790, testFolder.newFile("testFile"), "lmp", sequencingRuns, "lmp", "bioscope");
        //duplicate, passes vendor check
        SAMRecord r1 = records.get(0);
        r1.setFlags(1025);
        
        //singleton, passes vendor check
        SAMRecord r2 = records.get(1);
        
        SAMRecord r3 = records.get(3); 
        //singleton, fails vendor check
        SAMRecord r4 = records.get(4);
        r4.setFlags(512);
        
        //duplicate, fails vendor check
        SAMRecord r5 = records.get(5);
        r5.setFlags(1536);
        
        SAMRecord r6 = records.get(6); 
        r6.setFlags(129);        
    
        lmp.annotate(r1);
        assertEquals("V**", r1.getAttribute("ZP"));
        lmp.annotate(r2);
        assertEquals("AAC", r2.getAttribute("ZP"));
        lmp.annotate(r3);
        assertEquals("ABC", r3.getAttribute("ZP"));
        lmp.annotate(r4);
        assertEquals("X**", r4.getAttribute("ZP"));
        lmp.annotate(r5);
        assertEquals("W**", r5.getAttribute("ZP"));
        lmp.annotate(r6);
        assertEquals("ABC", r6.getAttribute("ZP"));
        
//        System.out.println(r1.getSAMString());
//        System.out.println(r2.getSAMString());
//        System.out.println(r3.getSAMString());
//        System.out.println(r4.getSAMString());
//        System.out.println(r5.getSAMString());
//        System.out.println(r6.getSAMString());
        
        assertEquals(3, lmp.getDuplicates().intValue());
        assertEquals(3, lmp.getSingletons().intValue());
        assertEquals(6, lmp.getTotalCount().intValue());
        assertEquals(2, lmp.getZpCount().get("ABC").intValue());
        assertEquals(1, lmp.getZpCount().get("AAC").intValue());
    }
    
    
    @Test
    public void testPEAnnotate() throws Exception {
    	//QSVParameters p = TestUtil.getQSVParameters(testFolder, testFolder.newFile("normal.bam").getAbsolutePath(), tumourBam.getAbsolutePath(), true, "none");
        Annotator pe = new Annotator(250, 2790, testFolder.newFile("testFile"), "pe", sequencingRuns,  "lmp", "bioscope");
        //duplicate, passes vendor check
        SAMRecord r1 = records.get(0);
        r1.setFlags(1025);
        
        //singleton, passes vendor check
        SAMRecord r2 = records.get(1);
        r2.setFlags(65);
        
        SAMRecord r3 = records.get(3); 
        //singleton, fails vendor check
        SAMRecord r4 = records.get(4);
        r4.setFlags(512);
        
        //duplicate, fails vendor check
        SAMRecord r5 = records.get(5);
        r5.setFlags(1536);
        
        SAMRecord r6 = records.get(6); 
        r6.setFlags(129);
        
        pe.annotate(r1);        
        pe.annotate(r2);
        pe.annotate(r3);
        pe.annotate(r4);
        pe.annotate(r5);
        pe.annotate(r6);
        
        assertEquals(3, pe.getDuplicates().intValue());
        assertEquals(3, pe.getSingletons().intValue());
        assertEquals(6, pe.getTotalCount().intValue());
        assertEquals(3, pe.getZpCount().get("BAC").intValue());
    }

}
