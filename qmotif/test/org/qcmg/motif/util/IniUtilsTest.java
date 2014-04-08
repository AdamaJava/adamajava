package org.qcmg.motif.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.ini4j.Ini;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.model.ChrPosition;

public class IniUtilsTest {
	
	private static Ini iniFile;
	
	@BeforeClass
	public static  void setup() {
		iniFile = new Ini();
		iniFile.add("test", "my_name_is", "snoop");
		iniFile.add("INCLUDES", "lp	chr1", "100-1000");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nullIni() {
		IniUtils.getEntry(null, null, null);
	}

	@Test
	public void nullAndEmptyParent() {
		assertNull(IniUtils.getEntry(iniFile, null, null));
		assertNull(IniUtils.getEntry(iniFile, "", null));
	}
	
	@Test
	public void nullAndEmptyChild() {
		assertNull(IniUtils.getEntry(iniFile, "test", null));
		assertNull(IniUtils.getEntry(iniFile, "test", ""));
	}
	
	@Test
	public void validRequest() {
		assertEquals("snoop", IniUtils.getEntry(iniFile, "test", "my_name_is"));
	}
	
	@Test
	public void getIncludes() {
		List<ChrPosition> positions = IniUtils.getPositions(iniFile, "INCLUDES");
		assertEquals(1, positions.size());
		assertEquals(100, positions.get(0).getPosition());
		assertEquals(1000, positions.get(0).getEndPosition());
		assertEquals("chr1", positions.get(0).getChromosome());
	}
	@Test
	public void getExcludes() {
		List<ChrPosition> positions = IniUtils.getPositions(iniFile, "EXCLUDES");
		assertEquals(0, positions.size());
	}

}
