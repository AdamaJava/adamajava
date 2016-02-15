package org.qcmg.motif.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.ini4j.Ini;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.model.ChrPositionName;

public class IniUtilsTest {
	
//	private static Ini iniFile;
	private  File iniFile;
	private  Ini ini;
	
	 @org.junit.Rule
	 public  TemporaryFolder folder = new TemporaryFolder();
	 
	 @Before
	public  void createIniFile() throws IOException {
		iniFile = folder.newFile("IniFileUtilTest.ini");
		try (BufferedWriter out = new BufferedWriter(new FileWriter(iniFile));) {
	       	out.write("[INCLUDES]\nlpchr1	chr1:100-1000\n");
	       	out.write("chr1q   chr1:249237907-249240620\n");
	       	out.write("chr6_ssto_hap7	chr6_ssto_hap7:1-4928567\n");
	       	out.write("chr4_ctg9_hap1    chr4_ctg9_hap1:1-590426\n");
	       	out.close();
		}
		ini = new Ini(iniFile);
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void nullIni() {
		IniUtils.getEntry(null, null, null);
	}

	@Test
	public void nullAndEmptyParent() {
		assertNull(IniUtils.getEntry(ini, null, null));
		assertNull(IniUtils.getEntry(ini, "", null));
	}
	
	@Test
	public void nullAndEmptyChild() {
		assertNull(IniUtils.getEntry(ini, "test", null));
		assertNull(IniUtils.getEntry(ini, "test", ""));
	}
	
	
	@Test
	public void getIncludes() {
		List<ChrPosition> positions = IniUtils.getPositions(ini, "INCLUDES");
		assertEquals(4, positions.size());
		Collections.sort(positions, new ChrPositionComparator());
		
		assertEquals(100, positions.get(0).getStartPosition());
		assertEquals(1000, positions.get(0).getEndPosition());
		assertEquals("chr1", positions.get(0).getChromosome());
		
		assertEquals(249237907, positions.get(1).getStartPosition());
		assertEquals(249240620, positions.get(1).getEndPosition());
		assertEquals("chr1", positions.get(1).getChromosome());
		
		assertEquals(1, positions.get(2).getStartPosition());
		assertEquals(590426, positions.get(2).getEndPosition());
		assertEquals("chr4_ctg9_hap1", positions.get(2).getChromosome());
		
		assertEquals(1, positions.get(3).getStartPosition());
		assertEquals(4928567, positions.get(3).getEndPosition());
		assertEquals("chr6_ssto_hap7", positions.get(3).getChromosome());
		
	}
	@Test
	public void getExcludes() {
		List<ChrPosition> positions = IniUtils.getPositions(ini, "EXCLUDES");
		assertEquals(0, positions.size());
	}

}
