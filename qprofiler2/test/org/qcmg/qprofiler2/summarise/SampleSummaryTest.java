package org.qcmg.qprofiler2.summarise;


import javax.xml.parsers.ParserConfigurationException;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qprofiler2.summarise.SampleSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

public class SampleSummaryTest {
	
	private VcfRecord tiRe = new VcfRecord( new String[]{ "chr1", "200",  ".", "A", "G", ".", ".",".", "GT", "0/1" } );  //ti
	private VcfRecord tvRe = new VcfRecord( new String[]{ "chr1", "100",  ".", "A", "T", ".", ".", ".","GT", "1/1" } );  //tv

	@Test
	public void incrementGTAD() {
		Map<String, QCMGAtomicLongArray> map = new HashMap<>();
		SVTYPE type = IndelUtils.getVariantType("A", "B");
		assertEquals(0, map.size());
		SampleSummary.incrementGTAD(type, "0/1", "10,10", "20", map);
		assertEquals(1, map.size());
		QCMGAtomicLongArray ar = map.get(SVTYPE.SNP.name());
		
		//QCMGAtomicLongArray(final int initialCapacity)  double capacity 
		assertEquals(2 * SampleSummary.altBinSize , ar.length());
		assertEquals(1, ar.get( 10 * SampleSummary.altBinSize / 20 -1 ));
	}
	

	@Test
	/**
	 * check titv elements by different inputs, check the boundary value of titvratio, such as 0, round to half,etc 
	 * @throws ParserConfigurationException
	 */
	public void titvTest() throws ParserConfigurationException{
		
		List<VcfRecord> records = new ArrayList<>();
		checkTiTv( records, null, null, null ); //empty
 
		//reset root and summary, add only one ti
		records.add(tiRe);
		checkTiTv( records, "0.00", "1", null );	//only one ti
						
		//reset root and summary, add only one tv
		records.clear();
		records.add(tvRe);
		checkTiTv( records, "0.00", null, "1" );
		
		//only one ti one tv
		records.add(tiRe); 
		checkTiTv( records, "1.00", "1", "1" );
		
		//one ti three tv
		records.add(tvRe);
		records.add(tvRe);
		checkTiTv( records, "0.33", "1", "3" );
						
		//two ti three tv
		records.add(tiRe);
		checkTiTv( records, "0.67", "2", "3" );

	}
	
	private void checkTiTv( List<VcfRecord> records, String ratio, String ti, String tv) throws ParserConfigurationException{
		
		//new  VcfSummarizer(null); cause exception since for(Sting cat: null)
		 
		Element root = XmlElementUtils.createRootElement("root", null) ;
		SampleSummary summary = new SampleSummary();
		for(VcfRecord re: records) summary.parseRecord( re, 1 );
		summary.toXML( root,null, null );  
		if(records.size() == 0){			
			assertEquals( 0, XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.VALUE).size());			 
			return; 
		}
			
			
		//get <SNP TiTvRatio="0.00" Transitions="0" Transversions="1">
		Element subE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.VALUE).stream()
				.filter( e -> e.getAttribute(XmlUtils.NAME).equals(SampleSummary.tiTvRatio)).findFirst().get();								 
		assertEquals( ratio, subE.getTextContent());
		
		
		if(ti != null) {
			subE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.VARIABLE_GROUP_ELE).stream()
					.filter( e -> e.getAttribute(XmlUtils.NAME).equals(SampleSummary.transitions)).findFirst().get();
			assertEquals(ti, XmlElementUtils.getChildElement(subE, XmlUtils.TALLY, 0).getAttribute(XmlUtils.COUNT));
			
		}
		
		if(tv != null) {
			subE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.VARIABLE_GROUP_ELE).stream()
					.filter( e -> e.getAttribute(XmlUtils.NAME).equals(SampleSummary.transversions)).findFirst().get();
			assertEquals(tv,  XmlElementUtils.getChildElement(subE, XmlUtils.TALLY, 0).getAttribute(XmlUtils.COUNT));
			
		}
	
	}

}
