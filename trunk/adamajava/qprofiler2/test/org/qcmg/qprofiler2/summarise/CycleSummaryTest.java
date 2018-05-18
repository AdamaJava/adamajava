package org.qcmg.qprofiler2.summarise;


import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.bam.BamSummarizer2;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.bam.TagSummaryReportTest;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import junit.framework.Assert;

public class CycleSummaryTest {
	private static final String INPUT_FILE = "input.sam";

	
	@Before
	public void setUp() throws Exception{
		createInputFile(INPUT_FILE);
	}
	
	@After
	public void tearDown() { new File(INPUT_FILE).delete();	}	
	
	@Test
	public void getLengthsFromSummaryByCycleTest() throws Exception{  
  		Element root = QprofilerXmlUtils.getChildElement(getSummarizedRoot(),  ProfileType.bam +"Report", 0);
  		for(String parent : new String[]{"SEQ", "QUAL"}){
 			
 			List<Element> elements = QprofilerXmlUtils.getChildElementByTagName( 
 					QprofilerXmlUtils.getChildElement( QprofilerXmlUtils.getChildElement(root,  parent, 0), "LengthTally",0), QprofilerXmlUtils.valueTally );
 			Assert.assertEquals(elements.size(), 2);
 			
 			for( Element element : elements  ){					 
				String source = element.getAttributes().getNamedItem("source").getNodeValue();
				int totalCounts = Integer.parseInt( element.getAttributes().getNamedItem( QprofilerXmlUtils.totalCount).getNodeValue() );
				List<Element> eleTallys =  QprofilerXmlUtils.getChildElementByTagName( element, QprofilerXmlUtils.tallyItem );
							
				int countSum = 0;
				for(Element ele: eleTallys)	{
					int count= Integer.parseInt(  ele.getAttributes().getNamedItem("count").getNodeValue() );
					countSum += count;  
					
					//check percent == count/totalCount
					double percent = Double.parseDouble(ele.getAttributes().getNamedItem("percent").getNodeValue().replace("%", ""));
					double comp = ((double) count / totalCounts) * 100;
					assertTrue(comp == percent);
										
					String value = ele.getAttributes().getNamedItem("value").getNodeValue();						
					if(parent.equals("SEQ") && source.equals("FirstOfPair")) assertTrue( value.equals("141") ||  value.equals("151")  );
					else if(parent.equals("QUAL") && source.equals("FirstOfPair")) assertTrue( value.equals("143") ||  value.equals("151")  );
					else if(source.equals("FirstOfPair")) assertTrue( value.equals("151")  );					
				}
				assertTrue(totalCounts == countSum);					 
 			}
 		}		
	}

	@Test
	public void getBaseByCycleTest() throws Exception{
 		Element root = getSummarizedRoot();		 		 		
 		 
 		root = QprofilerXmlUtils.getChildElement( QprofilerXmlUtils.getChildElement(root,"SEQ",0), "BaseByCycle", 0); 		
 		List<Element> eleList =  QprofilerXmlUtils.getChildElementByTagName(root, "CycleTally");	
  		for(Element ele : eleList){
				String possible = ele.getAttributes().getNamedItem("possibleValues").getNodeValue();
				String pair = ele.getAttributes().getNamedItem("source").getNodeValue();
				//record.getReadBases() only output ACGMNT, convert (a-z) to upcase; special letter to 'N'
				if(pair.equals( "FirstOfPair"))
					assertTrue(possible.equals("A,C,G,T,M,N"));
				else if(pair.equals("SecondOfPair"))
					assertTrue(possible.equals("A,C,G,T,N") );	
								
				if(pair.equals("FirstOfPair")){
					for(int l = 0,   nodeNo = ele.getChildNodes().getLength(); l < nodeNo; l++){					
						  Element cycleE = (Element) ele.getChildNodes().item(l);						  
						  if(cycleE.getAttribute("value").equals("1")   ) 
							  assertTrue( "0,1,0,1,0,0".equals(cycleE.getAttribute("counts"))   ) ;
						  else if(cycleE.getAttribute("value").equals("141")  ) 
							  assertTrue( "0,0,1,0,0,1".equals(cycleE.getAttribute("counts"))   ) ;
						  else if(cycleE.getAttribute("value").equals("142")   ) 
							  assertTrue( "0,0,0,0,0,1".equals(cycleE.getAttribute("counts"))   ) ;
						  else if(cycleE.getAttribute("value").equals("151") ) 
							  assertTrue( "0,0,0,0,1,0".equals(cycleE.getAttribute("counts"))   ) ;
					}					
				}else{
					for(int l = 0,  nodeNo = ele.getChildNodes().getLength(); l < nodeNo; l++){					
						  Element cycleE = (Element) ele.getChildNodes().item(l);
						 // System.out.println(l +"," + cycleE.getAttribute("value") + " : " + cycleE.getAttribute( "counts"));						  
						  if(cycleE.getAttribute("value").equals("2")   ) //reverse '.' convet to 'N'
							  assertTrue( "0,0,0,0,1".equals(cycleE.getAttribute("counts"))   ) ;
						  else if(cycleE.getAttribute("value").equals("4")  ) //reverse 'a' (=A) convet to 'T'
							  assertTrue( "0,0,0,1,0".equals(cycleE.getAttribute("counts"))   ) ;
						  else if(cycleE.getAttribute("value").equals("151") ) //reverse  C convet to 'G'
							  assertTrue( "0,0,1,0,0".equals(cycleE.getAttribute("counts"))   ) ;
					}
					
				}									 
		}		
	}
	
	@Test
	public void getQualityByCycleTest() throws Exception{
 		Element root = getSummarizedRoot();		 		 		

 		root = QprofilerXmlUtils.getChildElement( QprofilerXmlUtils.getChildElement(root,"QUAL",0), "QualityByCycle", 0); 		
 		List<Element> eleList =  QprofilerXmlUtils.getChildElementByTagName(root, "CycleTally");	
  		for(Element ele : eleList){
				String possible = ele.getAttributes().getNamedItem("possibleValues").getNodeValue();
				String pair = ele.getAttributes().getNamedItem("source").getNodeValue();
				
				//record.getBaseQualities() = qual base letter's ascii code - 33
 				if(pair.equals("FirstOfPair"))
					assertTrue(possible.equals(getPossibleQualString(true))); // -,7,<,A,F,J,
				else if(pair.equals("SecondOfPair"))
					assertTrue(possible.equals(getPossibleQualString(false)));
					
				if(pair.equals("FirstOfPair")){
					for(int l = 0,  nodeNo = ele.getChildNodes().getLength(); l < nodeNo; l++){					
						  Element cycleE = (Element) ele.getChildNodes().item(l);
						  if(cycleE.getAttribute("value").equals("1")   ) //AF							  
							  assertTrue(  new StringBuffer("0,1,1,0,0,0").toString().equals(cycleE.getAttribute("counts"))   ) ; 
						  else if(cycleE.getAttribute("value").equals("143")  ) //-J
							  assertTrue( new StringBuffer("1,0,0,0,0,1").toString().equals(cycleE.getAttribute("counts"))   ) ;
						  else if(cycleE.getAttribute("value").equals("144")   ) //7
							  assertTrue( new StringBuffer("0,0,0,0,1,0").toString().equals(cycleE.getAttribute("counts"))   ) ;
						  else if(cycleE.getAttribute("value").equals("151") ) //A
							  assertTrue( new StringBuffer("0,0,1,0,0,0").toString().equals(cycleE.getAttribute("counts"))   ) ;					  
					}
					
				}else if(pair.equals("SecondOfPair")){
					for(int l = 0,  nodeNo = ele.getChildNodes().getLength(); l < nodeNo; l++){					
						  Element cycleE = (Element) ele.getChildNodes().item(l);
						  if(cycleE.getAttribute("value").equals("1")   ) //reverse A
							  assertTrue( new StringBuffer("0,0,1,0,0,0") .toString().equals(cycleE.getAttribute("counts"))   ) ;
						  else if(cycleE.getAttribute("value").equals("148")  ) //-
							  assertTrue( new StringBuffer("0,0,0,0,0,1").toString().equals(cycleE.getAttribute("counts"))   ) ;
						  else if(cycleE.getAttribute("value").equals("151") ) //7
							  assertTrue( new StringBuffer("0,0,0,0,1,0").toString().equals(cycleE.getAttribute("counts"))   ) ;
					}
					
				}									 
		}		
	}

	
	@Test
	public void getBadBaseQualByCycleTest() throws Exception{
		Element root = getSummarizedRoot();		
 
		String[] parentTags = new String[]{"QUAL", "SEQ"};
		String[] childTags = new String[]{"BadQualsInReads", "BadBasesInReads"};
		
		for(int i = 0; i < 2; i ++ ){
			root = QprofilerXmlUtils.getChildElement( QprofilerXmlUtils.getChildElement(root,parentTags[i],0), childTags[i], 0); 		
			List<Element> eleList =  QprofilerXmlUtils.getChildElementByTagName(root, "ValueTally");	
	  		for(Element ele : eleList){
	  			String pair = ele.getAttribute("source");
	  			if(pair.equals(QprofilerXmlUtils.FirstOfPair)){
	  				assertTrue(ele.getAttribute(QprofilerXmlUtils.totalCount).equals("2") );
	  				List<Element> eleList1 =QprofilerXmlUtils.getChildElementByTagName(ele, QprofilerXmlUtils.tallyItem);	
	  				assertTrue(eleList1.size() == 2);
	  				for(int j = 0; j < 2; j ++){
	  					Element ele1 = eleList1.get(j);
	  					if(j == 0 )
	  						assertTrue(ele1.getAttribute(QprofilerXmlUtils.value).equals("0"));
	  					else if( i == 0) //QUAL FirstOfPari second TallyItem
	  						assertTrue(ele1.getAttribute(QprofilerXmlUtils.value).equals("2"));
	  					else //SEQ second TallyItem
	  						assertTrue(ele1.getAttribute(QprofilerXmlUtils.value).equals("18"));
	  					
		  				assertTrue(ele1.getAttribute(QprofilerXmlUtils.count).equals("1") ); 
		  				assertTrue(ele1.getAttribute(QprofilerXmlUtils.percent).equals("50.0%") );
	  				} 
	  			}else{
	  				assertTrue(ele.getAttribute(QprofilerXmlUtils.totalCount).equals("1") );
	  				List<Element> eleList1 =QprofilerXmlUtils.getChildElementByTagName(ele, QprofilerXmlUtils.tallyItem);	
	  				assertTrue(eleList1.size() == 1);
	  				Element ele1 = eleList1.get(0);
	  				assertTrue(ele1.getAttribute(QprofilerXmlUtils.value).equals("1")   ); 
	  				assertTrue(ele1.getAttribute(QprofilerXmlUtils.count).equals("1") ); 
	  				assertTrue(ele1.getAttribute(QprofilerXmlUtils.percent).equals("100.0%") );
	 				 
	  			}
	  		}			
			
			
		}

 
		
	}
	

	private Element getSummarizedRoot() throws Exception{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();		
		Document doc = domImpl.createDocument(null,"qProfiler",null);
		Element root = doc.getDocumentElement();					 

		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(INPUT_FILE); 
		sr.toXml(root);
		Assert.assertEquals(sr.getRecordsParsed(), 4);		
		
		return root; 	
	}
		
	private String getPossibleQualString(boolean isFirstOfPair){
		 	
		// iterate over the SAMRecord objects returned, passing them to the summariser
		HashSet<Byte> bases = new HashSet<Byte>();	
		try(SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(INPUT_FILE));) {
			for (SAMRecord record : reader) {
				if(record.getFirstOfPairFlag() != isFirstOfPair) continue;
				for(byte base : record.getBaseQualities())
					bases.add(base);	
			}			
		} catch (IOException e) {
			fail("unexpected to have IO problem");
		}
		
		List<Byte> sorted =  bases.stream().sorted( (i,j ) -> j-i ).collect(Collectors.toList());
		
//		TreeSet<Byte> sorted = new TreeSet<Byte>( bases);		
		StringBuilder sb = new StringBuilder();
		for(byte b : sorted) sb.append(b).append(QprofilerXmlUtils.COMMA);
		
		if(sb.length() > 0)
			return sb.substring(0, sb.length()-QprofilerXmlUtils.COMMA.length());
		
		return null; 
	}
	
	public static void createInputFile(String fname) throws IOException{
		List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:20150125163736341	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@RG	ID:20150125163738010	SM:eBeads_20091110_ND	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");

      //first in pair, inward
        data.add("ST-E00129:119:H0F9NALXX:5:1107:21421:3401	99	chr1	10001	0	3S125M23S	=	10001	126	" + 
        		"CCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCAACCCTACCCCTAACCCCNNNNNNNNNNNNNNNNaa..MM	" + 
        		"AFFFAJJJJJ<JJFFJAJJAFJ-F<<<<AJAAJAFA<<J<JJJJJFFJJAJJAA-AAFJJJAFFJFJJF7-FF<F<FAJJJFJ-FFJJAJJFFAJ7F<AJF<A-FJ-FFAJJFJJJF<-A-7F-------77FJJAA---A<-7-A<FF7A	" +
        		"ZC:i:7	MD:Z:115A9	PG:Z:MarkDuplicates.7	RG:Z:20150125163736341	NM:i:1	AS:i:120	XS:i:117");
        //second in pair		
        data.add("ST-E00129:119:H0F9NALXX:5:1107:21421:3401	147	chr1	10001	0	25S126M	=	10001	-126	" + 
        		"CTACCCCACGCTCTCCCCACCTCCCTAACCCTAACCCTAACCCTAACCCTACCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCAACCCTAACCCTAAaC.T	" + 
        		"7-------A-A7A--------<AAA--<A7-7--FFF7-7AFA---FA-7--JAJ<--JFA<<AFFFAFAAFJFA<FFJ7--FJFA--JJJJFFAJF-<-JJFJFFJFJFFFFFFJJJJJAAAJJFJFFFJJJJ<JJFJFFFFFFJAFA-A	" + 
        		"ZC:i:7	MD:Z:26A99	PG:Z:MarkDuplicates.7	RG:Z:20150125163736341	NM:i:1	AS:i:121	XS:i:117");
        
      //first in pair inward, mate duplicate
       data.add("ST-E00129:119:H0F9NALXX:7:1113:19016:29050	97	chr1	81194800	25	115M26S	=	81194989	189	" +
        		"TAGGGTTACGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTAGGGTTAGGGTTAGGGTTAGGGTTAGGGGTTAGGGGTTAGGGTTACGGTTAGGGTTAGGGGTTAGGGTTAGGGTGAGGGTGAGGGTGAGGGTG	" + 
        		"()FFFAFJJFJFJFJJJJFJFJFFFJJJF<AJFFJ<AJJFJ<JJFJJFJFJJ<JFFJJAJAJJA-FJJFA<-JAJJJF7AFAJFA-FJJJ<JJJFJAFJ<<JA<7FFJJ-7JJFJ--7AJJ7FFJFF-AAFJF-JFFJF7J7J	" + 
        		"ZC:i:6	MD:Z:8G84G21	PG:Z:MarkDuplicates.7	RG:Z:20150125163738010	NM:i:2	AS:i:105	XS:i:94");

//       data.add("ST-E00129:119:H0F9NALXX:7:1113:19016:29050	97	chr1	81194800	25	115M26S	=	81194989	189	" +
//       		"TAGGGTTACGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTAGGGTTAGGGTTAGGGTTAGGGTTAGGGGTTAGGGGTTAGGGTTACGGTTAGGGTTAGGGGTTAGGGTTAGGGTGAGGGTGAGGGTGAGGGTG	" + 
//       		"FFFFFAFJJFJFJFJJJJFJFJFFFJJJF<AJFFJ<AJJFJ<JJFJJFJFJJ<JFFJJAJAJJA-FJJFA<-JAJJJF7AFAJFA-FJJJ<JJJFJAFJ<<JA<7FFJJ-7JJFJ--7AJJ7FFJFF-AAFJF-JFFJF7J7J	" + 
//       		"ZC:i:6	MD:Z:8G84G21	PG:Z:MarkDuplicates.7	RG:Z:20150125163738010	NM:i:2	AS:i:105	XS:i:94");

       
       
       //second in pair 
       data.add("ST-E00129:119:H0F9NALXX:7:1113:19016:29050	1169	chr1	81194989	6	96S53M2S	=	81194800	-189	" + 
       		"GGGTTTGGGTGTGGGTGTGGGGTGGGGGGTGGGGTTGGGGTGGGGGGTTGGGGTGGGGTTAGGGTGGGGGTGGGGGTGAGGGTTAGGGTGGGGGTGAGGGTTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGTTAGGGTAG	" + 
       		"-7-7--<7J-<--F7-A--F-7--A7---<-7A7---<F7---FF-----F----FF7-7<JFJF7-J<--<-J7<77-FJA---F-77<--JAFF-F-<<J-FF<AFJJAJJJFFFJJJJJJJJJFJJJJJAJJJJJJJJJJFJAFFFFA	" + 
       		"ZC:i:6	MD:Z:53	PG:Z:MarkDuplicates.7	RG:Z:20150125163738010	NM:i:0	AS:i:53	XS:i:49");	      
       
       
        try(BufferedWriter out = new BufferedWriter(new FileWriter(fname))){	    
			for (String line : data)  out.write(line + "\n");	               
		}
		
	}
	
	
}
