package org.qcmg.qprofiler2.fastq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;
import htsjdk.samtools.fastq.FastqRecord;

public class FastqSummaryReportTest { 
			
	 // create fastq reads
	private List<FastqRecord> createFastqRecord() { 
		List<FastqRecord> myList = new ArrayList<>();
		
		 // seq read 1
		String name = "NB551151:83:HWC2VBGX9:4:11401:16365:1025 1:N:0:NGTTAA";
		String base = "AAAGGGG.N";
		String qual = "#AAA/////";	
		FastqRecord record = new FastqRecord(name, base, null, qual);
		myList.add(record);
		
		 // seq read 2
		name = "NB551151:83:HWC2VBGX9:4:11401:16365:1025";
		record = new FastqRecord(name, base, null, null);
		myList.add(record);
				
		 // seq read 3
		record = new FastqRecord(null, "", null, qual);
		myList.add(record);
		
		
		 // 7~14th good qual
		for(int i = 0; i < 8; i++) { 
			record = new FastqRecord("NB551151.1", "", null, "FFFFFFFFFFFFF");
			myList.add(record);
		}
		
		return myList;		
	}
	
	
	@Test
	public void seqTest() throws ParserConfigurationException { 
		FastqSummaryReport report  = new FastqSummaryReport();		
		for(FastqRecord record : createFastqRecord()) { 
			report.parseRecord(record);
		}
		
		Element root =  XmlElementUtils.createRootElement("root", null);
		report.toXml(root);
		
		Element seqEle = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.SEQ).get(0);
		assertEquals( XmlElementUtils.getChildElementByTagName(seqEle, XmlUtils.SEQUENCE_METRICS).size(), 6  );
		for(Element ele : XmlElementUtils.getChildElementByTagName(seqEle, XmlUtils.SEQUENCE_METRICS)) { 
			assertEquals( ele.getAttribute( ReadGroupSummary.READ_COUNT ) ,  "11");
			assertEquals( XmlElementUtils.getChildElementByTagName(ele, XmlUtils.VARIABLE_GROUP).get(0).getAttribute(XmlUtils.NAME), ele.getAttribute(XmlUtils.NAME) );
		}
		
		for(Element ele : XmlElementUtils.getOffspringElementByTagName(seqEle, XmlUtils.TALLY)) { 
			if(ele.getAttribute( XmlUtils.VALUE).equals("0") ) { 
				 // unless bad reads, value==0 means good
				assertEquals( ele.getAttribute( XmlUtils.COUNT ) ,  "9");
			}else { 
				 // only two reads have seq, others are empty
				assertEquals( ele.getAttribute( XmlUtils.COUNT ) ,  "2");
			}
		}
		
		 // value for name attributte are same 
		for( Element ele : XmlElementUtils.getOffspringElementByTagName( seqEle, XmlUtils.SEQUENCE_METRICS ) ) { 
			String vname = XmlElementUtils.getChildElement( ele, XmlUtils.VARIABLE_GROUP, 0 ).getAttribute(XmlUtils.NAME);
			assertEquals( ele.getAttribute(XmlUtils.NAME), vname );
		}
		
	}
	
	@Test
	public void qualTest() throws ParserConfigurationException { 
		FastqSummaryReport report  = new FastqSummaryReport();		
		for(FastqRecord record : createFastqRecord()) { 
			report.parseRecord(record);
		}
		
		Element root =  XmlElementUtils.createRootElement("root", null);
		report.toXml(root);
		
		Element qulEle = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.QUAL).get(0);
		assertEquals( XmlElementUtils.getChildElementByTagName(qulEle, XmlUtils.SEQUENCE_METRICS).size(), 3  );
		for(Element ele : XmlElementUtils.getChildElementByTagName(qulEle, XmlUtils.SEQUENCE_METRICS)) { 
			 // one of qual is null won't count
			assertEquals( ele.getAttribute( ReadGroupSummary.READ_COUNT ) ,  "10");
			assertEquals( XmlElementUtils.getChildElementByTagName(ele, XmlUtils.VARIABLE_GROUP).get(0).getAttribute(XmlUtils.NAME), ele.getAttribute(XmlUtils.NAME) );
		}
		
		 // value for name attributte are same 
		for( Element ele : XmlElementUtils.getOffspringElementByTagName( qulEle, XmlUtils.SEQUENCE_METRICS ) ) { 
			String vname = XmlElementUtils.getChildElement( ele, XmlUtils.VARIABLE_GROUP, 0 ).getAttribute(XmlUtils.NAME);
			assertEquals( ele.getAttribute(XmlUtils.NAME), vname );
		}
		
	}		
	
	@Test
	public void ExceptionTest() { 
		
		FastqSummaryReport report  = new FastqSummaryReport();		 
		
		 // base seq is empty  is ok
	 	report.parseRecord(new FastqRecord("ok", "", null, null));
	 	 	 	
		 // name, qual are null is ok  
	 	report.parseRecord(new FastqRecord(null, "", null, null));
	 	 
	 		 	
		try { 
			 // base seq is null is not allowed
		 	report.parseRecord(new FastqRecord("test", null, null, ""));
		 	fail("not expect to complete parseRecord!");
		}catch(NullPointerException e) { 
			 // expect to have expection			
		}	
	 	
		
	}	

}
