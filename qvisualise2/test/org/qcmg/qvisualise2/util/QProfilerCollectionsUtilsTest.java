package org.qcmg.qvisualise2.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.junit.Test;
import org.qcmg.qvisualise2.util.QProfilerCollectionsUtils;


public class QProfilerCollectionsUtilsTest {
//	@Ignore
//	public void realTest() {
//		Document doc = XmlReportReader.createDocumentFromFile(new File("/home/oholmes/development/data/S0449_20100809_2_LMP_F3-R3-Paired.bam_all.xml"));
//		Element el =  (Element) doc.getElementsByTagName("BAMReport").item(0);
//		NodeList seqNL = el.getElementsByTagName("SEQ");
//		Element seqElement = (Element) seqNL.item(0);	
//		SummaryByCycle<Character> summary = QProfilerCollectionsUtils.generateSummaryByCycleFromElement(
//				(Element) seqElement.getElementsByTagName("BaseByCycle").item(0));
//		
//		Map<Integer, AtomicLong> origLengths = SummaryByCycleUtils.getLengthsFromSummaryByCycle(summary, 535504340);
//		
//		for (Entry<Integer, AtomicLong> entry : origLengths.entrySet()) 
//			System.out.println("origLengths entry: " + entry.getKey().intValue() + ", " + entry.getValue().get());
//								
//	}
	
	
	@Test
	public void doBothSecondaryAndSupplementaryElementsAppear() {
		Map<String, AtomicLong> flagMap = new HashMap<>();
		flagMap.put("100010000001, pS2", new AtomicLong(100));
		flagMap.put("100010010001, p1s", new AtomicLong(200));
		flagMap.put("100010010001, p", new AtomicLong(20500));
		
		Map<String, String> discMap = new HashMap<>();
		discMap.put("S", "Supplementary");
		discMap.put("s", "secondary");
		
		Map<String, AtomicLong> results = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flagMap, discMap, null);
		assertEquals(3, results.size());
		
		discMap.clear();
		discMap.put("S", "Supplementary");
		results = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flagMap, discMap, null);
		assertEquals(2, results.size());
		
		discMap.clear();
		results = QProfilerCollectionsUtils.splitFlagTallyByDistinguisher(flagMap, discMap, null);
		assertEquals(1, results.size());
	}
	
	
	@Test
	public void xuTest(){
		
		SimpleRegression simpleRegression = new SimpleRegression(true);

        // passing data to the model
        // model will be fitted automatically by the class 
        simpleRegression.addData(new double[][] {
                {1, 2},
                {2, 3},
                {3, 4},
                {4, 5},
                {5, 6}
        });

        // querying for model parameters
        System.out.println("slope = " + simpleRegression.getSlope());
        System.out.println("intercept = " + simpleRegression.getIntercept());

        // trying to run model for unknown data
        System.out.println("prediction for 1.5 = " + simpleRegression.predict(1.5));
		
		
	}
}
