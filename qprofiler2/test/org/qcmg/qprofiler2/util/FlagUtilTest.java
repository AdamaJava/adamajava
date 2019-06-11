package org.qcmg.qprofiler2.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.util.FlagUtil;
import org.w3c.dom.Element;

public class FlagUtilTest {
	
	@Test
	public void getValidFlags() {
		assertEquals("000000000000", FlagUtil.getFlagString(0));
		assertEquals("000000000001, p", FlagUtil.getFlagString(1));
		assertEquals("000000000010", FlagUtil.getFlagString(2));			// INVALID
		assertEquals("000000000011, pP", FlagUtil.getFlagString(3));		// paired and in proper pair
		assertEquals("000000000100, u", FlagUtil.getFlagString(4));		//UNMAPPED
		assertEquals("000000000101, pu", FlagUtil.getFlagString(5));		//paired and UNMAPPED
		assertEquals("000000000110, u", FlagUtil.getFlagString(6));			//UNMAPPED
		assertEquals("000000000111, pPu", FlagUtil.getFlagString(7));			//UNMAPPED
		assertEquals("000000001000", FlagUtil.getFlagString(8));			//INVALID
	}
	
	
	/*
	 * Bit Description
0x1 template having multiple segments in sequencing
0x2 each segment properly aligned according to the aligner
0x4 segment unmapped
0x8 next segment in the template unmapped
0x10 SEQ being reverse complemented
0x20 SEQ of the next segment in the template being reversed
0x40 the first segment in the template
0x80 the last segment in the template
0x100 secondary alignment
0x200 not passing quality controls
0x400 PCR or optical duplicate
0x800 supplementary alignment
	 */
	
	
	@Test
	public void readPairedFlag() {
		
		for (int i = 1 ; i < 5000 ; i++) {
			
			if ((i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("1, p"));
			}
			if ((i & 0x2) == 2 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("1, pP"));
			}
			if ((i & 0x4) == 4) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("u"));
			}
			if ((i & 0x8) == 8 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("U"));
			}
			if ((i & 0x10) == 16) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("r"));
			}
			if ((i & 0x20) == 32 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("R"));
			}
			if ((i & 0x40) == 64 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).matches(".*1, p.*1.*"));
			}
			if ((i & 0x80) == 128 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("2"));
			}
			if ((i & 0x100) == 256) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("s"));
			}
			if ((i & 0x200) == 512) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("f"));
			}
			if ((i & 0x400) == 1024) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("d"));
			}
			if ((i & 0x800) == 2048) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("S"));
			}
			
		}
	}
	
	@Test
	public void flagXmlTest() throws ParserConfigurationException {
		Map<String, AtomicLong> flagBinaryCount = new ConcurrentSkipListMap<String, AtomicLong>();
		QCMGAtomicLongArray flagIntegerCount = new QCMGAtomicLongArray( 2048 );
		Element root = XmlElementUtils.createRootElement("root", null);
		
		//parse two flags
		flagIntegerCount.increment(99);		
		flagIntegerCount.increment(147);
		flagIntegerCount.increment(147);
		
		//convert to map
		long length = flagIntegerCount.length();
		for (int i = 0 ; i < length ; i++) 
			if (flagIntegerCount.get(i) > 0) {
				String flagString = FlagUtil.getFlagString(i);
				flagBinaryCount.put(flagString, new AtomicLong(flagIntegerCount.get(i)));
			}				
		XmlUtils.outputTallyGroup( root , "FLAG", flagBinaryCount, true, true);	
		
		//check output
		List<Element> tallys =  XmlElementUtils.getChildElementByTagName(XmlElementUtils.getChildElement( root, XmlUtils.VARIABLE_GROUP,0), XmlUtils.TALLY );
		assertEquals( 2, tallys.size() );
		
		assertEquals( 1, tallys.stream().filter( e -> e.getAttribute(XmlUtils.COUNT).equals("1")  &&  e.getAttribute(XmlUtils.VALUE).equals("000001100011, pPR1")).count() );
		assertEquals( 1, tallys.stream().filter( e -> e.getAttribute(XmlUtils.COUNT).equals("2")  &&  e.getAttribute(XmlUtils.VALUE).equals("000010010011, pPr2")).count() );
	}

}
