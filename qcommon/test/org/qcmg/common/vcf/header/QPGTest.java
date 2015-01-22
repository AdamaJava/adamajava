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
			new VcfHeaderQPG(null);
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
	}
	@Test
	public void emptyCtor() {
		try {
			new VcfHeaderQPG("");
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
		
	}
	@Test
	public void exampleCtor() {
		
		final String line = "##qPG=<ID=1,Tool=qannotate,Version=0.01,Date=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		
		final VcfHeaderQPG qpg = new VcfHeaderQPG(line);
		assertEquals(1, qpg.getOrder());
		assertEquals("qannotate", qpg.getTool());
		assertEquals("0.01", qpg.getVersion());
		assertEquals("20140106", qpg.getISODate());
		assertEquals("/opt/local/q3/bin/qannotate mode=snpEff ...", qpg.getCommandLine());
		
	}
	
	@Test
	public void exampleCtorMultiArg() {
		final int order = 12345;
		final String tool = "grep";
		final String ver = "v14.3";
		final String cl = "grep -w \"hello???\" my_big_file.txt";
		
		final VcfHeaderQPG qpg = new VcfHeaderQPG(order, tool, ver, cl);
		assertEquals(order, qpg.getOrder());
		assertEquals(tool, qpg.getTool());
		assertEquals(ver, qpg.getVersion());
		System.out.println("date: " + qpg.getISODate());
		assertEquals(cl, qpg.getCommandLine());
	}
	
	@Test
	public void doesTheComparatorWork() {
		final int order = 12345;
		final String tool = "grep";
		final String ver = "v14.3";
		final String cl = "grep -w \"hello???\" my_big_file.txt";
		
		final VcfHeaderQPG qpg = new VcfHeaderQPG(order, tool, ver, cl);
		final VcfHeaderQPG qpg2 = new VcfHeaderQPG(order + 1, tool, ver, cl);
		
		assertEquals(true, qpg.compareTo(qpg2) > 0);
		
		final List<VcfHeaderQPG> list = new ArrayList<>();
		list.add(qpg);
		list.add(qpg2);
		Collections.sort(list);
		
		assertEquals(qpg2, list.get(0));
		assertEquals(qpg, list.get(1));
		
		final VcfHeaderQPG qpg3 = new VcfHeaderQPG(order - 1, tool, ver, cl);
		list.add(qpg3);
		Collections.sort(list);
		
		assertEquals(qpg2, list.get(0));
		assertEquals(qpg, list.get(1));
		assertEquals(qpg3, list.get(2));
	}
	
	@Test
	public void doesToStringWork() {
	    String line = "##qPG=<ID=1,Source=qannotate,Version=0.01,DATE=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		final VcfHeaderQPG qpg = new VcfHeaderQPG(line);
		
		//different order, can't convert Source to Tool
		line = "##qPG=<ID=1,Tool=null,Version=0.01,Date=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		assertEquals(line, qpg.toString());
	}
	
	@Test
	public void equals() {
		final String line = "##qPG=<ID=1,Source=qannotate,Version=0.01,DATE=20140106,Description=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		final VcfHeaderQPG qpg2 = new VcfHeaderQPG(line);
		final VcfHeaderQPG qpg = new VcfHeaderQPG(line);		
		assertEquals(true, qpg.equals(qpg2));
	}

}
