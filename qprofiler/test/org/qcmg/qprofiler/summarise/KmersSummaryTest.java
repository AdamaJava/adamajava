package org.qcmg.qprofiler.summarise;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.w3c.dom.Document;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.SequenceUtil;

public class KmersSummaryTest {
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void bothReversedTest() throws IOException {
		long start = System.currentTimeMillis();
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);	
		System.out.println("time taken to instantiate KmersSummary: " + (System.currentTimeMillis() - start));
		
		
		
		File input = testFolder.newFile();
		createTestSamFile(input);
		start = System.currentTimeMillis();
		
		
 		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader(input); ){
 			for (SAMRecord samRecord : reader) 
				summary.parseKmers(samRecord.getReadBases(), true );				
 		}
 		System.out.println("time taken to parse records: " + (System.currentTimeMillis() - start));
 		
		//kmers3
		// CAGNG TTAGG <= GTCNCAATCC <= CCTAACNCTG		    
		// AGNG TTAGG <= TCNCAATCC  <= CCTAACNCT				
 		String[] bases = summary.getPossibleKmerString(3, true); 
		for(int cycle = 0; cycle < 10; cycle ++) 
			for(String base : bases)
				if( cycle == 0  && ( base.equals("CAG")  || base.equals("AGN")) )
					assertTrue(summary.getCount(cycle, base ) == 1);
				else if(cycle == 1 && ( base.equals("AGN") || base.equals("GNG")) )
					assertTrue(summary.getCount(cycle, base ) == 1);
				else if(cycle == 2 && (   base.equals("GNG") || base.equals("NGT" )) )
					assertTrue(summary.getCount(cycle, base ) == 1);
				else if(cycle == 3 && ( base.equals("NGT" )  || base.equals( "GTT")) )
					assertTrue(summary.getCount(cycle, base ) == 1);
				else if(cycle == 4 &&  base.equals("GTT") )
					assertTrue(summary.getCount(cycle, base ) == 1);		
				else
					assertTrue(summary.getCount(cycle, base ) == 0);		
	}
	
	@Test
	public void producer() {
//		assertEquals(",A,T,G,C,N", KmersSummary.producer(1,"",true));
//		assertEquals(",A,T,G,C", KmersSummary.producer(1,"",false));
//		assertEquals(",,AA,AT,AG,AC,,TA,TT,TG,TC,,GA,GT,GG,GC,,CA,CT,CG,CC", KmersSummary.producer(2,"",false));
		assertEquals("A,T,G,C,N", KmersSummary.producer(1,"",true));
		assertEquals("A,T,G,C", KmersSummary.producer(1,"",false));
		assertEquals("AA,AT,AG,AC,TA,TT,TG,TC,GA,GT,GG,GC,CA,CT,CG,CC", KmersSummary.producer(2,"",false));
		
	}
	
	@Test
	public void getPossibleKmerString()  {
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		String [] kmers = summary.getPossibleKmerString(6, true);
		assertEquals((int)Math.pow(5,6), kmers.length);
		kmers = summary.getPossibleKmerString(6, false);
		assertEquals((int)Math.pow(4,6), kmers.length);
	}
	
	@Test
	public void toXml() throws ParserConfigurationException {
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
//		summary.parseKmers( "AAAAAAAAAAAAAACCCCCCCCCCCCCCCGGGGGGGGGGGGGGGGTTTTTTTTTTTTTTTTTTTTTT".getBytes(), false );
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		
		Document doc = builder.getDOMImplementation().createDocument(null, "qProfiler", null);
		org.w3c.dom.Element root = doc.getDocumentElement();
		summary.toXml(root, KmersSummary.maxKmers);
		assertEquals(true, true);	// we made it!
	}
	
	@Test
	public void revComp() {
		SAMRecord rec = new SAMRecord(null);
		rec.setReadString("ATCG");
		byte[] bases = rec.getReadBases();
		bases = Arrays.copyOf(bases, bases.length);
		SequenceUtil.reverseComplement(bases);
		assertArrayEquals(new byte[]{'C','G','A','T'}, bases);
		
		byte[] basesAgain = rec.getReadBases();
		assertArrayEquals(new byte[]{'A','T','C','G'}, basesAgain);
	}
		
	@Test
	public void bothForwardTest() throws IOException {

		KmersSummary summary = new KmersSummary(6);
		File input = testFolder.newFile();
		createTestSamFile(input);
		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader(input);){
			for (SAMRecord samRecord : reader) { 
				summary.parseKmers(samRecord.getReadBases(), false );
			}
		}
		
		 //kmers1
		 // CCTA A CNCTG
		 // CCTA A CNCT
		String[] bases = summary.getPossibleKmerString(1, true); 
		for(int cycle = 0; cycle < 10; cycle ++) 
			for( String base : bases )
				if((cycle == 0 || cycle == 1) && base.equals("C")) 
			 		assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 2 && base.equals("T")) 
			 		assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 3 && base.equals("A")) 
			 		assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 4 && base.equals("A")) 
					//short mers from second reads are discarded
			 		assertTrue(summary.getCount(cycle, base ) == 1);
				else{
					assertTrue(summary.getCount(cycle, base ) == 0);
				}
		
		 //kmers2
		 bases = summary.getPossibleKmerString(2, true); 	
		 for(int cycle = 0; cycle < 10; cycle ++) 
			for(String base : bases)
				if(cycle == 0 && base.equals("CC"))
					assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 1 && base.equals("CT"))
					assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 2 && base.equals("TA"))
					assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 3 && base.equals("AA"))
					assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 4 && base.equals("AC"))
					assertTrue(summary.getCount(cycle, base ) == 1);
				else  
					assertTrue(summary.getCount(cycle, base ) == 0);
	}
	
	private static void createTestSamFile(File f ) {
		List<String> data = new ArrayList<>();
		data.add("@HD	VN:1.0	SO:coordinate");
		data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
		data.add("@SQ	SN:chr1	LN:249250621");
		data.add("@CO	Test SAM file for use by KmersSummaryTest.java");
		
		//reverse
		data.add("642_1887_1862	83	chr1	10167	1	5H10M	=	10176	59	CCTAACNCTG	.(01(\"!\"	RG:Z:1959T");	
 		//forward
		data.add("970_1290_1068	163	chr1	10176	3	9M6H	=	10167	-59	CCTAACNCT	I&&HII%%I	RG:Z:1959T");
		
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)) ) ) {
			for (String line : data)   out.println(line);			 
		} catch (IOException e) {
			Logger.getLogger("KmersSummaryTest").log(
					Level.WARNING, "IOException caught whilst attempting to write to SAM test file: " + f.getAbsolutePath(), e);
		}  
	}
	
}
