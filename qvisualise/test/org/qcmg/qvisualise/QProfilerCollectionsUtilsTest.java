package org.qcmg.qvisualise;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;
import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qvisualise.report.XmlReportReader;
import org.qcmg.qvisualise.util.QProfilerCollectionsUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class QProfilerCollectionsUtilsTest {
	@Ignore
	public void realTest() {
		Document doc = XmlReportReader.createDocumentFromFile(new File("/home/oholmes/development/data/S0449_20100809_2_LMP_F3-R3-Paired.bam_all.xml"));
		Element el =  (Element) doc.getElementsByTagName("BAMReport").item(0);
		NodeList seqNL = el.getElementsByTagName("SEQ");
		Element seqElement = (Element) seqNL.item(0);
		SummaryByCycle<Character> summary = QProfilerCollectionsUtils.generateSummaryByCycleFromElement(seqElement, "BaseByCycle");
	
		
		
		Map<Integer, AtomicLong> origLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(summary, 535504340);
		
		for (Entry<Integer, AtomicLong> entry : origLengths.entrySet()) {
			System.out.println("origLengths entry: " + entry.getKey().intValue() + ", " + entry.getValue().get());
		}
		
//		Map<Integer, AtomicLong> newLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle2(summary);
//		
//		for (Entry<Integer, AtomicLong> entry : newLengths.entrySet()) {
//			System.out.println("newLengths entry: " + entry.getKey().intValue() + ", " + entry.getValue().get());
//		}
		
		
	}
}
