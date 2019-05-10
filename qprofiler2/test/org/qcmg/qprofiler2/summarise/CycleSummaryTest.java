package org.qcmg.qprofiler2.summarise;


import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.bam.BamSummarizer2;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;


public class CycleSummaryTest {
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	private static File input;
	
	@Before
	public void setUp() throws Exception{
		input = testFolder.newFile("input.sam");
		createInputFile(input); 		
	}
	
	private  void checklength( Element root, String metricName, String pairName, int cycle, String[] values, int[] counts ) throws Exception {
		if(counts.length != values.length)
			throw new Exception("error: values size must be same to counts size");
		
		
		//eg. <sequenceMetrics name="seqBase"><variableGroup name="firstReadInPair">	
		List<Element> elements = XmlElementUtils.getOffspringElementByTagName( root, XmlUtils.variableGroupEle ).stream()
			.filter( e -> e.getAttribute(XmlUtils.Sname).equals(pairName ) &&
					 ((Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals(metricName)).collect(Collectors.toList());
		Assert.assertEquals(elements.size(), 1);
		

		//eg, <baseCycle cycle="1">
		Element ele = XmlElementUtils.getOffspringElementByTagName( elements.get(0), XmlUtils.baseCycleEle ).stream()
			.filter( e -> e.getAttribute(XmlUtils.Scycle).equals(cycle + "" )).findFirst().get();	
		assertEquals(values.length, ele.getChildNodes().getLength());	
		
		
			
		//eg   QprofilerXmlUtils.seqLength + "_"+  QprofilerXmlUtils.FirstOfPair;
		for(int i = 0; i < values.length; i ++) {
			String v =  (metricName.contains("qual"))? ((byte) values[i].toCharArray()[0]-33) + ""   : (String)values[i] ;
			String c = counts[i] + "";
			long count = XmlElementUtils.getChildElementByTagName( ele, XmlUtils.Stally ).stream()
					.filter( e -> e.getAttribute(XmlUtils.Svalue).equals(v) 
							&& e.getAttribute(XmlUtils.Scount).equals(c) ).count();
			assertTrue(count == 1);
		}	 
	}
	
	@Test
	public void getBaseByCycleTest() throws Exception{
 		Element root = getSummarizedRoot();			  
 		checklength( root, XmlUtils.seqBase , XmlUtils.FirstOfPair, 1, new String[] {"C","T"}, new int[] { 1,1 } ) ;
 		checklength( root, XmlUtils.seqBase , XmlUtils.FirstOfPair, 141, new String[] {"G","N"}, new int[] { 1,1 } ) ;
 		checklength( root, XmlUtils.seqBase , XmlUtils.FirstOfPair, 142, new String[] {"N"}, new int[] { 1 } ) ;
 		checklength( root, XmlUtils.seqBase , XmlUtils.FirstOfPair, 151, new String[] {"M" }, new int[] { 1 } ) ; 		
 		checklength( root, XmlUtils.seqBase , XmlUtils.SecondOfPair , 2, new String[] {"N"}, new int[] { 1} ) ;
 		checklength( root, XmlUtils.seqBase , XmlUtils.SecondOfPair , 4, new String[] {"T"}, new int[] { 1} ) ;
 		checklength( root, XmlUtils.seqBase , XmlUtils.SecondOfPair , 151, new String[] {"G"}, new int[] { 1} ) ; 		
 	}
	
	@Test
	public void getQualityByCycleTest() throws Exception{
 		Element root = getSummarizedRoot();		
 		
 		checklength( root,  XmlUtils.qualBase , XmlUtils.FirstOfPair, 1, new String[] {"A","("}, new int[] { 1,1 } ) ;
 		checklength( root,  XmlUtils.qualBase , XmlUtils.FirstOfPair, 143, new String[] {"-","J"}, new int[] {1, 1} ) ;
 		checklength( root,  XmlUtils.qualBase , XmlUtils.FirstOfPair, 144, new String[] {"7"}, new int[] { 1 } ) ;
 		checklength( root,  XmlUtils.qualBase , XmlUtils.FirstOfPair, 151, new String[] {"A"}, new int[] { 1 } ) ;		
  		checklength( root,  XmlUtils.qualBase , XmlUtils.SecondOfPair, 1, new String[] {"A" }, new int[] { 1} ) ;	
		checklength( root,  XmlUtils.qualBase , XmlUtils.SecondOfPair, 148, new String[] {"-" }, new int[] { 1} ) ;
		checklength( root,  XmlUtils.qualBase , XmlUtils.SecondOfPair, 151, new String[] {"7" }, new int[] { 1} ) ;
	}	

	public static Element getSummarizedRoot() throws Exception{				
		Element root = XmlElementUtils.createRootElement( "qProfiler", null);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(input.getAbsolutePath()); 
		sr.toXml(root);
		Assert.assertEquals(sr.getRecordsInputed(), 4);			
		return root; 	
	}
			
	public static void createInputFile(File input) throws IOException{
		List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:20150125163736341	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@RG	ID:20150125163738010	SM:eBeads_20091110_ND	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");

      //first in pair, inward properPair
        data.add("ST-E00129:119:H0F9NALXX:5:1107:21421:3401	99	chr1	10001	0	3S125M23S	=	10001	126	" + 
        		"CCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCAACCCTACCCCTAACCCCNNNNNNNNNNNNNNNNaa..MM	" + 
        		"AFFFAJJJJJ<JJFFJAJJAFJ-F<<<<AJAAJAFA<<J<JJJJJFFJJAJJAA-AAFJJJAFFJFJJF7-FF<F<FAJJJFJ-FFJJAJJFFAJ7F<AJF<A-FJ-FFAJJFJJJF<-A-7F-------77FJJAA---A<-7-A<FF7A	" +
        		"ZC:i:7	MD:Z:115A9	PG:Z:MarkDuplicates.7	RG:Z:20150125163736341	NM:i:1	AS:i:120	XS:i:117");
        //second in pair	properPair	
        data.add("ST-E00129:119:H0F9NALXX:5:1107:21421:3401	147	chr1	10001	0	25S126M	=	10001	-126	" + 
        		"CTACCCCACGCTCTCCCCACCTCCCTAACCCTAACCCTAACCCTAACCCTACCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCAACCCTAACCCTAAaC.T	" + 
        		"7-------A-A7A--------<AAA--<A7-7--FFF7-7AFA---FA-7--JAJ<--JFA<<AFFFAFAAFJFA<FFJ7--FJFA--JJJJFFAJF-<-JJFJFFJFJFFFFFFJJJJJAAAJJFJFFFJJJJ<JJFJFFFFFFJAFA-A	" + 
        		"ZC:i:7	MD:Z:26A99	PG:Z:MarkDuplicates.7	RG:Z:20150125163736341	NM:i:1	AS:i:121	XS:i:117");
        
       //first in pair inward, ProperPair mate duplicate
       data.add("ST-E00129:119:H0F9NALXX:7:1113:19016:29050	99	chr11	81194800	25	115M26S	=	81194989	189	" +
        		"TAGGGTTACGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTAGGGTTAGGGTTAGGGTTAGGGTTAGGGGTTAGGGGTTAGGGTTACGGTTAGGGTTAGGGGTTAGGGTTAGGGTGAGGGTGAGGGTGAGGGTG	" + 
        		"()FFFAFJJFJFJFJJJJFJFJFFFJJJF<AJFFJ<AJJFJ<JJFJJFJFJJ<JFFJJAJAJJA-FJJFA<-JAJJJF7AFAJFA-FJJJ<JJJFJAFJ<<JA<7FFJJ-7JJFJ--7AJJ7FFJFF-AAFJF-JFFJF7J7J	" + 
        		"ZC:i:6	MD:Z:8G84G21	PG:Z:MarkDuplicates.7	RG:Z:20150125163738010	NM:i:2	AS:i:105	XS:i:94");

       //second in pair duplicate
       data.add("ST-E00129:119:H0F9NALXX:7:1113:19016:29050	1169	chr11	81194989	6	96S53M2S	=	81194800	-189	" + 
       		"GGGTTTGGGTGTGGGTGTGGGGTGGGGGGTGGGGTTGGGGTGGGGGGTTGGGGTGGGGTTAGGGTGGGGGTGGGGGTGAGGGTTAGGGTGGGGGTGAGGGTTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGTTAGGGTAG	" + 
       		"-7-7--<7J-<--F7-A--F-7--A7---<-7A7---<F7---FF-----F----FF7-7<JFJF7-J<--<-J7<77-FJA---F-77<--JAFF-F-<<J-FF<AFJJAJJJFFFJJJJJJJJJFJJJJJAJJJJJJJJJJFJAFFFFA	" + 
       		"ZC:i:6	MD:Z:53	PG:Z:MarkDuplicates.7	RG:Z:20150125163738010	NM:i:0	AS:i:53	XS:i:49");	      
              
        try(BufferedWriter out = new BufferedWriter(new FileWriter(input))){	    
			for (String line : data)  out.write(line + "\n");	               
		}		
	}	
	
}
