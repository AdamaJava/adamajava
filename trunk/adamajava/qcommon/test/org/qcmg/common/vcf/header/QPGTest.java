package org.qcmg.common.vcf.header;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class QPGTest {
	
	@Test
	public void nullCtor() {
		try {
			new QPG(null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
	}
	@Test
	public void emptyCtor() {
		try {
			new QPG("");
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		
	}
	@Test
	public void exampleCtor() {
		
		String line = "##qPG=<ORDER=1,TOOL=qannotate,TVER=0.01,DATE=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		QPG qpg = new QPG(line);
		assertEquals(1, qpg.getOrder());
		assertEquals("qannotate", qpg.getTool());
		assertEquals("0.01", qpg.getVersion());
		assertEquals("20140106", qpg.getISODate());
		assertEquals("\"/opt/local/q3/bin/qannotate mode=snpEff ...\"", qpg.getCommandLine());
		
	}
	
	@Test
	public void exampleCtorMultiArg() {
		int order = 12345;
		String tool = "grep";
		String ver = "v14.3";
		String cl = "grep -w \"hello???\" my_big_file.txt";
		
		QPG qpg = new QPG(order, tool, ver, cl);
		assertEquals(order, qpg.getOrder());
		assertEquals(tool, qpg.getTool());
		assertEquals(ver, qpg.getVersion());
		System.out.println("date: " + qpg.getISODate());
		assertEquals(cl, qpg.getCommandLine());
	}
	
	@Test
	public void doesTheComparatorWork() {
		int order = 12345;
		String tool = "grep";
		String ver = "v14.3";
		String cl = "grep -w \"hello???\" my_big_file.txt";
		
		QPG qpg = new QPG(order, tool, ver, cl);
		QPG qpg2 = new QPG(order + 1, tool, ver, cl);
		
		assertEquals(true, qpg.compareTo(qpg2) < 1);
		
		List<QPG> list = new ArrayList<>();
		list.add(qpg);
		list.add(qpg2);
		Collections.sort(list);
		
		assertEquals(qpg, list.get(0));
		assertEquals(qpg2, list.get(1));
		
		QPG qpg3 = new QPG(order - 1, tool, ver, cl);
		list.add(qpg3);
		Collections.sort(list);
		
		assertEquals(qpg3, list.get(0));
		assertEquals(qpg, list.get(1));
		assertEquals(qpg2, list.get(2));
	}
	
	@Test
	public void doesToStringWork() {
		String line = "##qPG=<ORDER=1,TOOL=qannotate,TVER=0.01,DATE=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		QPG qpg = new QPG(line);
		
		assertEquals(line, qpg.toString());
	}
	
	@Test
	public void equals() {
		String line = "##qPG=<ORDER=1,TOOL=qannotate,TVER=0.01,DATE=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		QPG qpg = new QPG(line);
		QPG qpg2 = new QPG(line);
		assertEquals(true, qpg.equals(qpg2));
	}

}
