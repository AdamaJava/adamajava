package org.qcmg.qvisualise;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.qvisualise.report.HTMLReportUtils;

public class HTMLReportUtilsTest {
	
	static final String SS = "<style type=\"text/css\">.header {font-family: Verdana, Helvetica, Arial;color: rgb(0,66,174);background: rgb(234,242,255);font-size: 15px;}\n.desc{padding: 5px 10px;font-family: Verdana, Helvetica, Arial; font-size:12px}\n.butt{font-family: Verdana, Helvetica, Arial; font-size:12px}\ntable { font-family: Verdana, Helvetica, Arial; font-size:12px }\n</style>\n";
	
	@Test
	public void getStyleSheet(){
//		System.out.println("ss: " + HTMLReportUtils.createStyleSheet());
		assertEquals(SS, HTMLReportUtils.SS);
	}

}
