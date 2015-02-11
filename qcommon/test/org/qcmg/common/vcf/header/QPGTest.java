package org.qcmg.common.vcf.header;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class QPGTest {
	
	@Test
	public void nullCtor() {
		try {
			new VcfHeader.QPGRecord(null);
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
	}
	@Test
	public void emptyCtor() {
		try {
			new VcfHeader.QPGRecord("");
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
		
	}
	@Test
	public void exampleCtor() {
		
		final String line = "##qPG=<ID=1,Tool=qannotate,Version=0.01,Date=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		
		final VcfHeader.QPGRecord qpg = new VcfHeader.QPGRecord(line);
		assertEquals(1, qpg.getOrder());
		assertEquals("qannotate", qpg.getTool());
		assertEquals("0.01", qpg.getVersion());
		assertEquals("20140106", qpg.getDate());
		assertEquals("\"/opt/local/q3/bin/qannotate mode=snpEff ...\"", qpg.getCommandLine());
		
	}
	
	@Test (expected=NumberFormatException.class)
	public void ExceptionTest() {
		
		final String line = "##qPG=<ORDER=1,TOOL=qsnp,TVER=2.0 (439),DATE=2015-01-21 07:59:42,CL=qsnp -i /mnt/seq_results/melanoma/MELA_0188/variants/qSNP/c55237e9_23e8_4825_8610_ec69624d4273/MELA_0188.ini -log /mnt/seq_results/melanoma/MELA_0188/variants/qSNP/c55237e9_23e8_4825_8610_ec69624d4273/qsnp_cs.log>";
		
		final VcfHeader.QPGRecord qpg = new VcfHeader.QPGRecord(line);
 
		
	}	
	
	@Test
	public void exampleCtorMultiArg() {
		final int order = 12345;
		final String tool = "grep";
		final String ver = "v14.3";
		final String cl = "grep -w \"hello???\" my_big_file.txt";
		
		VcfHeader header = new VcfHeader();
		header.addQPGLine(order, tool, ver, cl, VcfHeaderUtils.DF.format(new Date()));
		
		final VcfHeader.QPGRecord qpg =header.getqPGLines().get(0);
		assertEquals(order, qpg.getOrder());
		assertEquals(tool, qpg.getTool());
		assertEquals(ver, qpg.getVersion());
//		System.out.println("date: " + qpg.getISODate());
		assertEquals("\"" + cl + "\"", qpg.getCommandLine());
	}
	
	@Test
	public void doesTheComparatorWork() {
		final int order = 12345;
		final String tool = "grep";
		final String ver = "v14.3";
		final String cl = "grep -w \"hello???\" my_big_file.txt";
		
		VcfHeader header = new VcfHeader();
		header.addQPGLine(order, tool, ver, cl, VcfHeaderUtils.DF.format(new Date()));
		header.addQPGLine(order + 1, tool, ver, cl, VcfHeaderUtils.DF.format(new Date()));
		
		final VcfHeader.QPGRecord qpg = header.getqPGLines().get(1);	// calling getqPGLines sorts the internal collection
		final VcfHeader.QPGRecord qpg2 =header.getqPGLines().get(0);
		
		assertEquals(true, qpg.compareTo(qpg2) > 0);
		
		final List<VcfHeader.QPGRecord> list = new ArrayList<>();
		list.add(qpg);
		list.add(qpg2);
		Collections.sort(list);
		
		assertEquals(qpg2, list.get(0));
		assertEquals(qpg, list.get(1));
		
		header.addQPGLine(order - 1, tool, ver, cl, VcfHeaderUtils.DF.format(new Date()));
		final VcfHeader.QPGRecord qpg3 =header.getqPGLines().get(2);
		list.add(qpg3);
		Collections.sort(list);
		
		assertEquals(qpg2, list.get(0));
		assertEquals(qpg, list.get(1));
		assertEquals(qpg3, list.get(2));
	}
	
	@Test
	public void doesToStringWork() {
	    String line = "##qPG=<ID=1,Source=qannotate,Version=0.01,DATE=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		final VcfHeader.QPGRecord qpg = new VcfHeader.QPGRecord(line);
		
		//different order, can't convert Source to Tool
//		line = "##qPG=<ID=1,Tool=null,Version=0.01,Date=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		assertEquals(line, qpg.toString());
	}
	
	@Test
	public void equals() {
		final String line = "##qPG=<ID=1,Source=qannotate,Version=0.01,DATE=20140106,Description=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		final VcfHeader.QPGRecord qpg2 = new VcfHeader.QPGRecord(line);
		final VcfHeader.QPGRecord qpg = new VcfHeader.QPGRecord(line);		
		assertEquals(true, qpg.equals(qpg2));
	}

}
