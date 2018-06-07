package org.qcmg.common.util;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TabTokenizerTest {
	
	private final static Pattern tabbedPattern = Pattern.compile("[\\t]");
	
	@Test
	public void testTokenize() {
		String data = "123\t456";
		Assert.assertEquals(2, TabTokenizer.tokenize(data).length);
		Assert.assertEquals("123", TabTokenizer.tokenize(data)[0]);
		Assert.assertEquals("456", TabTokenizer.tokenize(data)[1]);
	}
	@Test
	public void testTokenizeIter() {
		String data = "123\t456";
		char delim = '\t';
		
		Iterable<String> iter = new TabTokenizer.Iter(data, delim);
		
		int i = 0;
		for (String s : iter) {
			if (i == 0) {
				Assert.assertEquals("123", s);
			}
			if (i == 1) {
				Assert.assertEquals("456", s);
			}
			i++;
		}
		Assert.assertEquals(2, i);
	}
	
	@Ignore
	public void testSpeedComparison() {
		String data = "1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12\t13\t14\t15\t16\t17\t18\t19\t20";
		Assert.assertEquals(20, TabTokenizer.tokenize(data).length);
		Assert.assertEquals("1", TabTokenizer.tokenize(data)[0]);
		Assert.assertEquals("4", TabTokenizer.tokenize(data)[3]);
		Assert.assertEquals("20", TabTokenizer.tokenize(data)[19]);
		
		Assert.assertEquals(20, TabTokenizer.tokenizeCharAt(data).length);
		Assert.assertEquals("1", TabTokenizer.tokenizeCharAt(data)[0]);
		Assert.assertEquals("4", TabTokenizer.tokenizeCharAt(data)[3]);
		Assert.assertEquals("20", TabTokenizer.tokenizeCharAt(data)[19]);
				
		int noOfLoops = 10000;
		long counter = 0;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			String[] params = TabTokenizer.tokenizeCharAt(data);
			counter += params.length;
		}
		System.out.println("tt charAt: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
		
		counter = 0;
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			String[] params = tabbedPattern.split(data, -1);
			counter += params.length;
		}
		System.out.println("split: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
		
		counter = 0;
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			String[] params = TabTokenizer.tokenize(data);
			counter += params.length;
		}
		System.out.println("tt: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
	}
	
	@Ignore
	public void testSpeed2Comparison() {
		String data = "1\t2\t\t3\t4\t5\t6\t7\t8\t9\t10\t\t11\t12\t13\t14\t15\t\t16\t17\t18\t19\t20";
		Assert.assertEquals(23, TabTokenizer.tokenize(data).length);
		Assert.assertEquals("1", TabTokenizer.tokenize(data)[0]);
		Assert.assertEquals("4", TabTokenizer.tokenize(data)[4]);
		Assert.assertEquals("20", TabTokenizer.tokenize(data)[22]);
		
		Assert.assertEquals(23, TabTokenizer.tokenizeCharAt(data).length);
		Assert.assertEquals("1", TabTokenizer.tokenizeCharAt(data)[0]);
		Assert.assertEquals("4", TabTokenizer.tokenizeCharAt(data)[4]);
		Assert.assertEquals("20", TabTokenizer.tokenizeCharAt(data)[22]);
		
		
		
		int noOfLoops = 10000;
		long counter = 0;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			String[] params = TabTokenizer.tokenizeCharAt(data);
			counter += params.length;
		}
		System.out.println("tt char at: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
		
		counter = 0;
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			String[] params = tabbedPattern.split(data, -1);
			counter += params.length;
		}
		System.out.println("split: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
	
		counter = 0;
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			String[] params = TabTokenizer.tokenize(data);
			counter += params.length;
		}
		System.out.println("tt: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
	}
	@Test
	public void testRealLifeData() {
		String data = "chr1	63665	T	66	,,,.,,,.,,,,,,,,,..,,,.,,..,,,,,,,,,,,,,,,,,,..,.,,,,,,,,,,,.,,^!,^!,^!,	IIIIIIIHIIIIIIIII%IIIIIIIIDIIIIHIIII@)II!IIIIIII*IEIB>I;IF;III,2%;	" +
				"44	,,,,,,,,,,,,,,,,,,,..,,,,,,,,,,,,,,,,,,,,,,^!,	IIIIIIIIIIII=IIIIIIIIIIIIIIIIIIFII>IIII0DIE4";
		Assert.assertEquals(9, TabTokenizer.tokenize(data).length);
		Assert.assertEquals("chr1", TabTokenizer.tokenize(data)[0]);
		Assert.assertEquals("63665", TabTokenizer.tokenize(data)[1]);
		Assert.assertEquals("T", TabTokenizer.tokenize(data)[2]);
		Assert.assertEquals("66", TabTokenizer.tokenize(data)[3]);
		
		Assert.assertEquals("chr1", TabTokenizer.tokenizeCharAt(data)[0]);
		Assert.assertEquals("63665", TabTokenizer.tokenizeCharAt(data)[1]);
		Assert.assertEquals("T", TabTokenizer.tokenizeCharAt(data)[2]);
		Assert.assertEquals("66", TabTokenizer.tokenizeCharAt(data)[3]);
		
		int noOfLoops = 10000;
		long counter = 0;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			String[] params = TabTokenizer.tokenize(data);
			counter += params.length;
		}
		System.out.println("tt: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
		
		counter = 0;
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			String[] params = tabbedPattern.split(data, -1);
			counter += params.length;
		}
		System.out.println("split: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
		
		counter = 0;
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			String[] params = TabTokenizer.tokenizeCharAt(data);
			counter += params.length;
		}
		System.out.println("tt charAt: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
	}

}
