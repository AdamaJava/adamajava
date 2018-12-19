package org.qcmg.qvisualise2;

import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.w3c.dom.Element;

public class CreateInputXML {
	
	@Test
	public void xuTest() throws ParserConfigurationException{
		
		Element root = createMDElemet();
//		asXmlText( root, "test.xml" );
	}
	
	/**
	 * 
	 * @return Element: an empty root 
	 * @throws ParserConfigurationException
	 */
	private static Element createRootElement() throws ParserConfigurationException{
		Element root = QprofilerXmlUtils.createRootElement( "qProfiler", null);
		root.setAttribute("annotation", "testing xml for qvisulise");		
		return root;
	}
	
	
	/**
	 * 
	 * @return simulated Element start from qprfiler MD tag
	 * @throws ParserConfigurationException
	 */
	public static Element createMDElemet() throws ParserConfigurationException{
		Element root = createRootElement();
		
		Element tagMDElement = QprofilerXmlUtils.createSubElement(root, "MD");	
		Element allReadElement = QprofilerXmlUtils.createSubElement(tagMDElement, "AllReads");	
		Element mismatchElement = QprofilerXmlUtils.createSubElement(tagMDElement, "MismatchByCycle");
		String[] sources = new String[]{"UnPaired", "FirstOfPair", "SecondOfPair"};
		for(int i = 1; i <= sources.length; i ++ ){
			//for AllReads
			Element tallyElement = QprofilerXmlUtils.createSubElement(allReadElement, "ValueTally");
			tallyElement.setAttribute("source", sources[i-1]);
			for(int j = 1; j <= 2; j ++){
				Element element = QprofilerXmlUtils.createSubElement(tallyElement, "TallyItem");
				element.setAttribute("count", (100 * i *j ) + "");
				element.setAttribute("percen", (33 * j)+"%");
				element.setAttribute("value", (10*(i+j)) + "");
			}
			
			//for MismatchByCycle			
			tallyElement = QprofilerXmlUtils.createSubElement(mismatchElement, "CycleTally");
			tallyElement.setAttribute("source", sources[i-1]);
			tallyElement.setAttribute("possibleValue", "A,C,G,T");		
			for(int j = 1; j <=10*(i+2); j ++){				
					Element element = QprofilerXmlUtils.createSubElement(tallyElement, "cycle");
					element.setAttribute("counts", "1,2,3,4");
					element.setAttribute("percen", "1%");
					element.setAttribute("value",j + "");				
			}			
		}
				
		return root; 		
	}
		


		
	

}
