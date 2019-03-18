package org.qcmg.qprofiler2.summarise;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class KmersSummaryTest {
	private static final String SAM_INPUT_FILE = "testInputFile.sam";
	
	@Before
	public void createFile(){ createTestSamFile(); }
	
	@After
	public void deleteFile(){ new File(SAM_INPUT_FILE).delete(); }
	
	@Test
	public void producerTest() {
		assertEquals("A,T,G,C,N", KmersSummary.producer(1,"",true));
		assertEquals("A,T,G,C", KmersSummary.producer(1,"",false));
		assertEquals("AA,AT,AG,AC,TA,TT,TG,TC,GA,GT,GG,GC,CA,CT,CG,CC", KmersSummary.producer(2,"",false));		
	}
	
	@Test
	public void getPossibleKmerStringTest()  {
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		String [] kmers = summary.getPossibleKmerString(6, true);
		assertEquals((int)Math.pow(5,6), kmers.length);
		kmers = summary.getPossibleKmerString(6, false);
		assertEquals((int)Math.pow(4,6), kmers.length);
		
		
		//test speed between split and subString
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  			
		LocalDateTime now = LocalDateTime.now(); 
		System.out.println(dtf.format(now)); 
		
		System.out.println("calling producer with split 101 times "); 
		String[] mers1 = summary.getPossibleKmerString(6, false);
		for (int  i = 0; i < 100; i ++) {
			mers1 = summary.getPossibleKmerString(6, false);
		}
		assertTrue(mers1.length == 4096);
		System.out.println("the finished producer with split 101 times "); 
		
		now = LocalDateTime.now(); 
		System.out.println(dtf.format(now));		
		System.out.println("calling producer with subString 101 times "); 
		
		List<String> mers2 = new ArrayList<>();
		String str = KmersSummary.producer(6, "", false);
		while(str.contains(Constants.COMMA_STRING)) {
			int pos =  str.indexOf(Constants.COMMA_STRING); 
			mers2.add(str.substring(0, pos)  );
			str = str.substring(pos+1);				
		}
		mers2.add(str); //add last mer
		for (int  i = 0; i < 100; i ++) {
			str = KmersSummary.producer(6, "", false);
			mers2 = new ArrayList<>();
			while(str.contains(Constants.COMMA_STRING)) {
				int pos =  str.indexOf(Constants.COMMA_STRING); 
				mers2.add( str.substring(0, pos)  );
				str = str.substring(pos+1);		
			}
			mers2.add(str); //add last mer			
		}
		assertTrue(mers2.size() == 4096);
		now = LocalDateTime.now(); 
		System.out.println(dtf.format(now));
		System.out.println("the finished producer with subString 100 times "); 				
	}
	
	@Test
	public void speedTest() {
		int counter = 10;
		String s = KmersSummary.producer(6, "", true);
		System.out.println("s length: " + s.length());
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
		String[] split = s.split(Constants.COMMA_STRING);
		}
//		System.out.println("split.length: " + split.length);
		System.out.println("time taken split: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			String [] tt = TabTokenizer.tokenize(s, Constants.COMMA);
			}
//		System.out.println("tt.length: " + tt.length);
		System.out.println("time taken tt: " + (System.currentTimeMillis() - start));
		
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
		String[] split = s.split(Constants.COMMA_STRING);
		}
//		System.out.println("split.length: " + split.length);
		System.out.println("time taken split: " + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void bothReversedTest() throws IOException {		
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);	
 		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(SAM_INPUT_FILE));){
 			for (SAMRecord record : reader){ 
				final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;	
				summary.parseKmers(record.getReadBases(), true, order );				
			} 
		}
 		
		//kmers3
		// CAGNG TTAGG <= GTCNCAATCC <= CCTAACNCTG		 first  set to reverse 
 		String[] bases1 = new String[]{"CAG","AGN","GNG","NGT" ,"GTT"};
 		// AGNG TTAGG <= TCNCAATCC  <= CCTAACNCT		 second	 set to reverse
 		String[] bases2 = new String[]{"AGN","GNG","NGT" ,"GTT"};
 		for(int i = 0; i < bases2.length; i++ ){
 			assertTrue(summary.getCount(i, bases1[i], 1 ) == 1);
 			assertTrue(summary.getCount(i, bases1[i], 2 ) == 0);
 			assertTrue(summary.getCount(i, bases2[i], 2 ) == 1);
 			assertTrue(summary.getCount(i, bases2[i], 1 ) == 0); 			
 		}
 		assertTrue(summary.getCount(4, bases1[4], 1 ) == 1);		 
	}

	@Test
	public void toXmlTest() throws IOException, DOMException, ParserConfigurationException {		
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);	
 		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(SAM_INPUT_FILE));){
 			for (SAMRecord record : reader){ 		
				final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;	
				summary.parseKmers(record.getReadBases(), record.getReadNegativeStrandFlag(), order );				
			} 
		}
 		
		Element root = QprofilerXmlUtils.createRootElement("root", null);
		summary.toXml(root, 3);
		
		//the popular kmers are based on counts on middle, middle of first half, middle of second half
		//in this testing case it look at firt cyle, middle cycle and last cycle
		assertEquals( "GTT,CAG", StringUtils.join(summary.getPopularKmerString(16,  3, false, 1), ",") );
		assertEquals( "TAA,CCT", StringUtils.join(summary.getPopularKmerString(16,  3, false, 2), ",") );
			 
		List<Element> tallysE = QprofilerXmlUtils.getOffspringElementByTagName(root, XmlUtils.Stally);
		assertEquals(4, tallysE.size());
		
		for(Element tE : tallysE) { 
			assertTrue( tE.getAttribute(XmlUtils.Scount).equals("1") );	
			Element parentE = (Element) tE.getParentNode();
			if( tE.getAttribute( XmlUtils.Svalue ).equals("GTT") ) {								
				assertTrue( parentE.getAttribute(XmlUtils.Sname).equals("kmersOnCycle_5") );
				assertTrue( ((Element) parentE.getParentNode()).getAttribute(XmlUtils.Sname).equals("3mers_firstReadInPair") ); 
			}else if(tE.getAttribute( XmlUtils.Svalue ).equals("TAA")){
				assertTrue( parentE.getAttribute(XmlUtils.Sname).equals("kmersOnCycle_3") );
				assertTrue( ((Element) parentE.getParentNode()).getAttribute(XmlUtils.Sname).equals("3mers_secondReadInPair") ); 				
			}else if(tE.getAttribute( XmlUtils.Svalue ).equals("CCT")) 
				assertTrue( parentE.getAttribute(XmlUtils.Sname).equals("kmersOnCycle_1") );
			else
				assertTrue( tE.getAttribute( XmlUtils.Svalue ).equals("CAG"));			 
		}
		
		
		// kmers3
		// CAGNG TTAGG <= GTCNCAATCC <= CCTAACNCTG		 first reversed
 		String[] bases1 = new String[]{"CAG","AGN","GNG","NGT" ,"GTT"};		
 		//  CCTAACNCT		 second	forwarded	 				
 		String[] bases2 = new String[]{"CCT","CTA","TAA" ,"AAC"}; //,"ACN", "CNC", "NCT"};
 
 		for(int i = 0; i < bases2.length; i++ ){
 			assertTrue( summary.getCount(i, bases1[i], 1 ) == 1 );
 			assertTrue( summary.getCount(i, bases1[i], 2 ) == 0 );
 			assertTrue( summary.getCount(i, bases2[i], 2 ) == 1 );
 			assertTrue( summary.getCount(i, bases2[i], 1 ) == 0 ); 			
 		}
 		assertTrue( summary.getCount(4, bases1[4], 1 ) == 1 );		 
	}
	
	
	@Test
	public void bothForwardTest() throws IOException {
		
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);	
 		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(SAM_INPUT_FILE));){
 			for (SAMRecord record : reader){ 
				final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;	
				summary.parseKmers(record.getReadBases(), false, order );				
			} 
		}		
		 //kmers1
		 // CCTA A CNCTG first
		 // CCTA A CNCT  second
		String[] bases = summary.getPossibleKmerString(1, true); 
		for(int cycle = 0; cycle < 10; cycle ++) 
			for( String base : bases )
				//second read
				if((cycle == 0 || cycle == 1) && base.equals("C")) 
			 		assertTrue(summary.getCount(cycle, base, 2 ) == 1);
				else if(cycle == 2 && base.equals("T")) 
			 		assertTrue(summary.getCount(cycle, base, 2 ) == 1);
				else if(cycle == 3 && base.equals("A")) 
			 		assertTrue(summary.getCount(cycle, base, 2) == 1);
				else{ 
					//short mers from second reads are discarded
					assertTrue(summary.getCount(cycle, base, 2 ) == 0);										
				}
		
		 //kmers2
		 bases = summary.getPossibleKmerString(2, true); 	
		 for(int cycle = 0; cycle < 10; cycle ++) 
			for(String base : bases)
				if(cycle == 0 && base.equals("CC"))
					assertTrue(summary.getCount(cycle, base, 1 ) == 1);
				else if(cycle == 1 && base.equals("CT"))
					assertTrue(summary.getCount(cycle, base, 1) == 1);
				else if(cycle == 2 && base.equals("TA"))
					assertTrue(summary.getCount(cycle, base, 1 ) == 1);
				else if(cycle == 3 && base.equals("AA"))
					assertTrue(summary.getCount(cycle, base, 1 ) == 1);
				else if(cycle == 4 && base.equals("AC"))
					assertTrue(summary.getCount(cycle, base, 1 ) == 1);
				else  
					assertTrue(summary.getCount(cycle, base,1 ) == 0);
	}

	@Test
	/**
	 * the accuracy will drop down if the read base is short. it trim the last six base which the value of KmersSummary.maxKmers
	 * here we set the base to more the 15 base
	 * @throws ParserConfigurationException
	 */
	public void bothFirstTest() throws ParserConfigurationException {
		final String base1 = "CAGNGTTAGGTTTTT";
		final String base2 = "CCCCGTTAGGTTTTTT";

		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);	
		summary.parseKmers( base1.getBytes() , false, 1);
		summary.parseKmers( base2.getBytes() , false, 1);
				
		/** 		
		 * int midCycle = cycleNo / 2; 
		 * int bfMidCycle = (midCycle > 20 )? midCycle - 10 : (midCycle < kLength )? 0 : midCycle - kLength; 
		 * int afMidCycle = (midCycle > 20 )? midCycle + 10 : (midCycle < kLength )? cycleNo-1 : midCycle + kLength; 
		 * according above code:
		 * popular kmers string is based on cycle: 
		 * midCycle 5 = ("base2".length() - 6 + 1)/2
		 * 2 = 5 - mersNo  since 5>mersNo
		 * 8 = 5+ mersNo
		 */		
		
		//CAGNG TTAGG TTTTT
		//CCCCG TTAGG TTTTT T
		//the mer with N won't belong to possible mer string
		assertTrue( summary.getCount(2, "GNG", 1 ) == 1 );
		assertTrue( summary.getCount(2, "CCG", 1 ) == 1 );
		//two same bases
		assertTrue( summary.getCount(5, "TTA", 1 ) == 2 );	
		assertTrue( summary.getCount(8, "GGT", 1 ) == 2 );	
		
		//mers are not counted that is zero unless "TTA,GGT,CCG"
		assertEquals( "TTA,GGT,CCG", StringUtils.join( summary.getPopularKmerString(16,  3, false, 1), ",") );
		//nothing on second pair
		assertEquals( "", StringUtils.join(summary.getPopularKmerString(16,  3, false, 2), ",") );						
	}
	
	
	private static void createTestSamFile( ) {
		List<String> data = new ArrayList<String>();
		data.add("@HD	VN:1.0	SO:coordinate");
		data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
		data.add("@SQ	SN:chr1	LN:249250621");
		data.add("@CO	Test SAM file for use by KmersSummaryTest.java");
		
		//reverse first in pair
		data.add("642_1887_1862	83	chr1	10167	1	5H10M	=	10176	59	CCTAACNCTG	.(01(\"!\"	RG:Z:1959T");	
 		//forward second in pair
		data.add("970_1290_1068	163	chr1	10176	3	9M6H	=	10167	-59	CCTAACNCT	I&&HII%%I	RG:Z:1959T");
				
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(SAM_INPUT_FILE)) ) ) {
			for (String line : data)   out.println(line);			 
		} catch (IOException e) {
			Logger.getLogger("KmersSummaryTest").log(
					Level.WARNING, "IOException caught whilst attempting to write to SAM test file: " + SAM_INPUT_FILE, e);
		}  
	}
	
}
