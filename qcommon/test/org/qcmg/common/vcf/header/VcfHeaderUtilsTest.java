package org.qcmg.common.vcf.header;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class VcfHeaderUtilsTest {
	
	@Test
	public void addQPGEntryNullOrEmpty() {
		
		try {
			VcfHeaderUtils.addQPGLineToHeader(null, null, null, null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		
		VcfHeader header = new VcfHeader();
		
		try {
			VcfHeaderUtils.addQPGLineToHeader(header, null, null, null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		try {
			VcfHeaderUtils.addQPGLineToHeader(header, "", "", "");
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
	}
	
	@Test
	public void addQPGEntry() {
		VcfHeader header = new VcfHeader();
		assertEquals(0, header.getqPGLines().size());
		
		String tool = "qsnp";
		String version = "1.0";
		String cl = "qsnp -i ini_file.ini -log log_file.log";
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(1, header.getqPGLines().size());
		assertEquals(1, header.getqPGLines().get(0).getOrder());
		assertEquals(tool, header.getqPGLines().get(0).getTool());
		assertEquals(version, header.getqPGLines().get(0).getVersion());
		assertEquals(cl, header.getqPGLines().get(0).getCommandLine());
		
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(2, header.getqPGLines().size());
		assertEquals(2, header.getqPGLines().get(0).getOrder());
		assertEquals(1, header.getqPGLines().get(1).getOrder());
		assertEquals(tool, header.getqPGLines().get(0).getTool());
		assertEquals(version, header.getqPGLines().get(0).getVersion());
		assertEquals(cl, header.getqPGLines().get(0).getCommandLine());
		
		
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(3, header.getqPGLines().size());
		assertEquals(3, header.getqPGLines().get(0).getOrder());
	}

}
