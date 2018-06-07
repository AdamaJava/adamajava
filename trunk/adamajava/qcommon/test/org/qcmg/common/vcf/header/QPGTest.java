package org.qcmg.common.vcf.header;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.vcf.header.VcfHeaderRecord;

public class QPGTest {
	
	@Test
	public void nullCtor() {
		try {
			new VcfHeaderRecord(null);
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
	}
	@Test
	public void emptyCtor() {
		try {
			new VcfHeaderRecord("");
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
		
	}
	@Test
	public void exampleCtor() {
		
		final String line = "##qPG=<ID=1,Tool=qannotate,Version=0.01,Date=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		
		final VcfHeaderRecord qpg = new VcfHeaderRecord(line);
		assertEquals(1, VcfHeaderUtils.getQPGOrder(qpg));
		assertEquals("qannotate", VcfHeaderUtils.getQPGTool(qpg)  );
		assertEquals("0.01", VcfHeaderUtils.getQPGVersion(qpg)   );
		assertEquals("20140106", VcfHeaderUtils.getQPGDate(qpg)  );		
		assertEquals("\"/opt/local/q3/bin/qannotate mode=snpEff ...\"", VcfHeaderUtils.getQPGCommandLine(qpg) );		
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void ExceptionTest() {

		//no exception for below
		//final String line = "##qPG=<ORDER=1,TOOL=qsnp,TVER=2.0 (439),DATE=2015-01-21 07:59:42,CL=qsnp -i /mnt/seq_results/melanoma/MELA_0188/variants/qSNP/c55237e9_23e8_4825_8610_ec69624d4273/MELA_0188.ini -log /mnt/seq_results/melanoma/MELA_0188/variants/qSNP/c55237e9_23e8_4825_8610_ec69624d4273/qsnp_cs.log>";		

		//exception if no ID
		final String line = "##=<ORDER1=1,TOOL=qsnp,TVER=2.0 (439),DATE=2015-01-21 07:59:42,CL=qsnp -i /mnt/seq_results/melanoma/MELA_0188/variants/qSNP/c55237e9_23e8_4825_8610_ec69624d4273/MELA_0188.ini -log /mnt/seq_results/melanoma/MELA_0188/variants/qSNP/c55237e9_23e8_4825_8610_ec69624d4273/qsnp_cs.log>";				
		final VcfHeaderRecord qpg = new VcfHeaderRecord(line);
		
	}	
	
	@Test
	public void exampleCtorMultiArg() {
		final int order = 12345;
		final String tool = "grep";
		final String ver = "v14.3";

		
		//discussion tomorrow		
		VcfHeader header = new VcfHeader();
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		 
		String cl = "grep -w \"hello???\" my_big_file.txt";			
		VcfHeaderUtils.addQPGLine(header, order, tool, ver, cl, date);
 		
		final VcfHeaderRecord qpg = VcfHeaderUtils.getqPGRecords(header).get(0);
		assertEquals(order, VcfHeaderUtils.getQPGOrder(qpg));
		assertEquals(tool, VcfHeaderUtils.getQPGTool(qpg));
		assertEquals(ver, VcfHeaderUtils.getQPGVersion(qpg));
		assertEquals(date, VcfHeaderUtils.getQPGDate(qpg));
		assertEquals( "\"" + cl + "\"", VcfHeaderUtils.getQPGCommandLine(qpg));
		
		VcfHeaderRecord qpg1 = new VcfHeaderRecord(qpg.toString());
		assertEquals(qpg1.getId(), VcfHeaderUtils.getQPGOrder(qpg)+"");
		assertEquals(qpg1.getSubFieldValue(VcfHeaderUtils.TOOL), VcfHeaderUtils.getQPGTool(qpg));
		assertEquals(qpg1.getSubFieldValue(VcfHeaderUtils.VERSION), VcfHeaderUtils.getQPGVersion(qpg));
		assertEquals(qpg1.getSubFieldValue(VcfHeaderUtils.DATE), VcfHeaderUtils.getQPGDate(qpg));		
		assertEquals(qpg1.getSubFieldValue(VcfHeaderUtils.COMMAND_LINE), VcfHeaderUtils.getQPGCommandLine(qpg));
	}
	
	
	@Test
	public void doesTheComparatorWork() {
		final int order = 12345;
		final String tool = "grep";
		final String ver = "v14.3";
		final String cl = "grep -w \"hello???\" my_big_file.txt";
		
		VcfHeader header = new VcfHeader();
		VcfHeaderUtils.addQPGLine(header,order, tool, ver, cl, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		VcfHeaderUtils.addQPGLine(header,order + 1, tool, ver, cl, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		
		// calling getqPGLines sorts the internal collection
		final VcfHeaderRecord qpg = VcfHeaderUtils.getqPGRecords(header).get(0);	//order + 1
		final VcfHeaderRecord qpg2 =VcfHeaderUtils.getqPGRecords(header).get(1);   //order 
		
		//qpg2.id < qpg.id but ComparTo o.id.comparTo(this.id)
		assertEquals(true, qpg2.compareTo(qpg )> 0);
		
		final List<VcfHeaderRecord> list = new ArrayList<>();
		list.add(qpg);
		list.add(qpg2);
		Collections.sort(list);
		
		assertEquals(qpg2, list.get(1));  //order
		assertEquals(qpg, list.get(0));    //order+1
		
		VcfHeaderUtils.addQPGLine( header, order - 1, tool, ver, cl, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		final VcfHeaderRecord qpg3 =VcfHeaderUtils.getqPGRecords( header).get(2);
		list.add(qpg3);
		Collections.sort(list);
				
		assertEquals(qpg, list.get(0));  //order+1
		assertEquals(qpg2, list.get(1)); //order
		assertEquals(qpg3, list.get(2)); //order-1
	}
	
	@Test
	public void doesToStringWork() {
	    String line = "##qPG=<ID=1,Source=qannotate,Version=0.01,DATE=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		final VcfHeaderRecord qpg = new VcfHeaderRecord(line);
		
		//different order, can't convert Source to Tool
//		line = "##qPG=<ID=1,Tool=null,Version=0.01,Date=20140106,CL=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		assertEquals(line, qpg.toString());
		assertEquals(VcfHeaderUtils.getQPGTool(qpg), null);
	}
	
	@Test
	public void equals() {
		final String line = "##qPG=<ID=1,Source=qannotate,Version=0.01,DATE=20140106,Description=\"/opt/local/q3/bin/qannotate mode=snpEff ...\">";
		final VcfHeaderRecord qpg2 = new VcfHeaderRecord(line);
		final VcfHeaderRecord qpg = new VcfHeaderRecord(line);		
		assertEquals(true, qpg.equals(qpg2));
	}

}
