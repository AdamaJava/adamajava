package org.qcmg.qprofiler2.summarise;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class KmersSummaryTest {
	private static final String SAM_INPUT_FILE = "testInputFile.sam";
	
	@Before
	public void createFile(){ createTestSamFile(); }
	
	@After
	public void deleteFile(){ new File(SAM_INPUT_FILE).delete(); }
	
	@Test
	public void bothReversedTest() throws IOException {		
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);	
 		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader( new File(SAM_INPUT_FILE)); ){
 			for (SAMRecord record : reader){ 
 				final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;	
				summary.parseKmers(record.getReadBases(), true, order);						
 			}		
 		}
 		
		//kmers3
		// CAGNG TTAGG <= GTCNCAATCC <= CCTAACNCTG		 first   
		// AGNG TTAGG <= TCNCAATCC  <= CCTAACNCT		 second		 		
 		//first of pair
 		String[] bases1 = new String[]{"CAG","AGN","GNG","NGT" ,"GTT"};
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
 		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader( new File(SAM_INPUT_FILE)); ){
 			for (SAMRecord record : reader){ 
 				final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;	
				summary.parseKmers(record.getReadBases(), true, order);						
 			}
 			
 		}
 		
		Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(null, "root", null).getDocumentElement();
		summary.toXml(root, 3);
		
		Element mers3 = (Element) root.getElementsByTagName("mers3").item(0);
		NodeList nodes = mers3.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i++){
			if(!(nodes.item(i) instanceof Element)) continue;
			Element ele = (Element) nodes.item(i);

			if(ele.getAttribute(QprofilerXmlUtils.source ).equals(QprofilerXmlUtils.FirstOfPair)){
				ele.getAttribute("possibleValues").equals("AAA, GTT");	
				int cycleNo = ele.getElementsByTagName("Cycle").getLength();
				for(int cycle  = 0; cycle <  cycleNo-1; cycle ++)
					assertTrue(((Element) ele.getElementsByTagName("Cycle").item(cycle)).getAttribute("counts").equals("0,0"));
				assertTrue(((Element) ele.getElementsByTagName("Cycle").item(cycleNo-1)).getAttribute("counts").equals("0,1")); 		 
			}else{				
				assertTrue( ele.getAttribute( QprofilerXmlUtils.source ).equals(QprofilerXmlUtils.SecondOfPair ));
				ele.getAttribute("possibleValues").equals("AAA");	
				int cycleNo = ele.getElementsByTagName("Cycle").getLength();
				for(int cycle  = 0; cycle <  cycleNo; cycle ++)
					assertTrue(((Element) ele.getElementsByTagName("Cycle").item(cycle)).getAttribute("counts").equals("0"));
			}						
		}
		

 		
		//kmers3
		// CAGNG TTAGG <= GTCNCAATCC <= CCTAACNCTG		 first   
		// AGNG TTAGG <= TCNCAATCC  <= CCTAACNCT		 second		 		
 		//first of pair
 		String[] bases1 = new String[]{"CAG","AGN","GNG","NGT" ,"GTT"};
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
	public void bothForwardTest() throws IOException {

		KmersSummary summary = new KmersSummary(6);	
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
