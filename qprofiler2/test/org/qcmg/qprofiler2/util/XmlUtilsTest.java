package org.qcmg.qprofiler2.util;


import static org.junit.jupiter.api.Assertions.assertAll;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.w3c.dom.Element;

public class XmlUtilsTest {
	
	
	@Test
	public void testReadGroupElement() throws ParserConfigurationException {				
				
		assertAll(  ()-> {
			Element ele = XmlUtils.createReadGroupNode( QprofilerXmlUtils.createRootElement( XmlUtils.readGroupsEle, null) , "id" );
			ele.getAttribute("RGID").equals("id");
			ele.getNodeName().equals("readGroup");
		} );
	}
}
