package org.qcmg.common.vcf.header;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class VcfHeaderUtilsTest {
	
	//@Test unit test don't have jar file so won't have tool name and version
	@Test
	public void addQPGEntryNullOrEmpty() {
		
		try {
			VcfHeaderUtils.addQPGLineToHeader(null, null, null, null);
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
		
		final VcfHeader header = new VcfHeader();
		
		try {
			VcfHeaderUtils.addQPGLineToHeader(header, null, null, null);
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
		try {
			VcfHeaderUtils.addQPGLineToHeader(header, "", "", "");
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
	}
	
	@Test
	public void addQPGEntry() {
		final VcfHeader header = new VcfHeader();
		assertEquals(0, header.getqPGLines().size());
		
		final String tool = "qsnp";
		final String version = "1.0";
		final String cl = "qsnp -i ini_file.ini -log log_file.log";
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(1, header.getqPGLines().size());
		assertEquals(1, header.getqPGLines().get(0).getOrder());
		assertEquals(tool, header.getqPGLines().get(0).getTool());
		assertEquals(version, header.getqPGLines().get(0).getVersion());
		assertEquals("\"" + cl + "\"", header.getqPGLines().get(0).getCommandLine());
		
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(2, header.getqPGLines().size());
		assertEquals(2, header.getqPGLines().get(0).getOrder());
		assertEquals(1, header.getqPGLines().get(1).getOrder());
		assertEquals(tool, header.getqPGLines().get(0).getTool());
		assertEquals(version, header.getqPGLines().get(0).getVersion());
		assertEquals("\"" + cl + "\"", header.getqPGLines().get(0).getCommandLine());
		
		
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(3, header.getqPGLines().size());
		assertEquals(3, header.getqPGLines().get(0).getOrder());
	}

}
