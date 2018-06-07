package org.qcmg.qprofiler2.summarise;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.SubsitutionEnum;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qprofiler2.summarise.SampleSummary;
import org.w3c.dom.Element;

public class SampleSummaryTest {
	
	private VcfRecord tiRe = new VcfRecord( new String[]{ "chr1", "200",  ".", "A", "G", ".", ".",".", "GT", "0/1" } );  //ti
	private VcfRecord tvRe = new VcfRecord( new String[]{ "chr1", "100",  ".", "A", "T", ".", ".", ".","GT", "1/1" } );  //tv
		
	
	@Test
	/**
	 * check all element in one case
	 * @throws ParserConfigurationException
	 */
	public void toXMTest() throws ParserConfigurationException{
		
		//new  VcfSummarizer(null); cause exception since for(Sting cat: null)
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();			 
		Element root = builder.getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
		SampleSummary summary = new SampleSummary();
		 
		summary.parseRecord(tiRe, 1);	
		for(int i = 0; i < 2; i++ )
			summary.parseRecord(tvRe, 1);
		for(int i = 0; i < 3; i++ )
			summary.parseRecord( new VcfRecord( new String[]{"chr1", "300",  "dbid", "A", "C", ".", ".", ".","GT:AD:DP", "0/1:,10:15"}), 1); //Tv	
		summary.toXML(root);  
		
		//check <Substitution ... >
		for(Element ele : QprofilerXmlUtils.getOffspringElementByTagName( root, SampleSummary.substitution ) ){
			String change = ele.getAttribute("change");
			if(change.equals(SubsitutionEnum.AG.toString()) )
					assertEquals(ele.getAttribute("count"), "1");			
			else if(change.equals(SubsitutionEnum.AT.toString()) )
				assertEquals(ele.getAttribute("count"), "2");
			else if(change.equals(SubsitutionEnum.AC.toString()) )
				assertEquals(ele.getAttribute("count"), "3");
		}
		 
		//check <SNP TiTvRatio= ... >
		Element subE = (Element) ((Element) root.getElementsByTagName(SampleSummary.substitutions).item(0) ).getElementsByTagName(SVTYPE.SNP.name()).item(0);
		assertEquals(subE.getAttribute(SampleSummary.tiTvRatio), "0.20");
		assertEquals(subE.getAttribute(SampleSummary.transitions), "1");
		assertEquals(subE.getAttribute(SampleSummary.transversions), "5");	

		//test <VariantType ...>
		subE = (Element) root.getElementsByTagName(SampleSummary.variantType).item(0);
		assertEquals(subE.getAttribute("count"), "6");
		assertEquals(subE.getAttribute("inDBSNP"), "3");
		assertEquals(subE.getAttribute("type"), SVTYPE.SNP.toVariantType());	
		
		//check genotype
		for(Element ele: QprofilerXmlUtils.getOffspringElementByTagName(subE, SampleSummary.genotype))
			if(ele.getAttribute("type").equals("0/1"))
				assertEquals(ele.getAttribute("count"), "4");
			else if(ele.getAttribute("type").equals("1/1"))
				assertEquals(ele.getAttribute("count"), "2");
	}
	@Test
	public void incrementGTAD() {
		Map<String, QCMGAtomicLongArray> map = new HashMap<>();
		SVTYPE type = IndelUtils.getVariantType("A", "B");
		assertEquals(0, map.size());
		SampleSummary.incrementGTAD(type, "0/1", "10,10", "20", map);
		assertEquals(1, map.size());
		QCMGAtomicLongArray ar = map.get(SVTYPE.SNP.name());
//		for (int i = 0 ; i < ar.length() ; i++) {
//			System.out.println("ar[" + i + "]: " + ar.get(i));
//		}
		assertEquals(204, ar.length());
		assertEquals(1, ar.get(50));
	}
	
	@Test
	/**
	 * check titv elements by different inputs, check the boundary value of titvratio, such as 0, round to half,etc 
	 * @throws ParserConfigurationException
	 */
	public void titvTest() throws ParserConfigurationException{
		
		List<VcfRecord> records = new ArrayList<>();
		checkTiTv(records, null, null, null); //empty
 
		//reset root and summary, add only one ti
		records.add(tiRe);
		checkTiTv(records, "-", "1", "0");	//only one ti
						
		//reset root and summary, add only one tv
		records.clear();
		records.add(tvRe);
		checkTiTv(records, "0.00", "0", "1");
		
		//only one ti one tv
		records.add(tiRe); 
		checkTiTv(records, "1.00", "1", "1");
		
		//one ti three tv
		records.add(tvRe);
		records.add(tvRe);
		checkTiTv(records, "0.33", "1", "3");
						
		//two ti three tv
		records.add(tiRe);
		checkTiTv(records, "0.67", "2", "3");

		//QprofilerXmlUtils.asXmlText(root, output);
	}
	
	private void checkTiTv( List<VcfRecord> records, String ratio, String ti, String tv) throws ParserConfigurationException{
		
		//new  VcfSummarizer(null); cause exception since for(Sting cat: null)
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();			 
		Element root = builder.getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
		SampleSummary summary = new SampleSummary();
		for(VcfRecord re: records)
			summary.parseRecord(re, 1);		
		summary.toXML(root);  

		
		if(records.size() == 0){
			assertTrue( root.getElementsByTagName(SampleSummary.substitutions).getLength() == 0 ); 
			return; 
		}
			
		//get <SNP TiTvRatio="0.00" Transitions="0" Transversions="1">
		Element subE = (Element) ((Element) root.getElementsByTagName(SampleSummary.substitutions).item(0) ).getElementsByTagName(SVTYPE.SNP.name()).item(0);
		assertEquals(subE.getAttribute(SampleSummary.tiTvRatio), ratio);
		assertEquals(subE.getAttribute(SampleSummary.transitions), ti);
		assertEquals(subE.getAttribute(SampleSummary.transversions), tv);		
	}

}
