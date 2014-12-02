package org.qcmg.maf.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.qcmg.common.maf.MAFRecord;

public class MafStatsUtilsTest {

	@Test
	public void testGetVariantClassifications() {
		List<MAFRecord> mafs = new ArrayList<>();
		MAFRecord maf = new MAFRecord();
		maf.setVariantClassification("Frame_Shift_Del");
		mafs.add(maf);
		assertEquals(2, MafStatsUtils.getVariantClassifications(mafs).size());	// extra entry for silent/nonSilent ratio
		String ratio = MafStatsUtils.getVariantClassifications(mafs).get(1);
		assertEquals("\u221E", ratio.substring(ratio.lastIndexOf(':') + 1));		// infinity symbol

		// add another Frams_Shift_Del - should not affect size of collection returned
		MAFRecord maf2 = new MAFRecord();
		maf2.setVariantClassification("Frame_Shift_Del");
		mafs.add(maf2);
		assertEquals(2, MafStatsUtils.getVariantClassifications(mafs).size());	// extra entry for silent/nonSilent ratio
		// silent non silent ratio should be NaN
		ratio = MafStatsUtils.getVariantClassifications(mafs).get(1);
		assertEquals("\u221E", ratio.substring(ratio.lastIndexOf(':') + 1));		// infinity symbol
		
		// now for a silent one
		MAFRecord maf3 = new MAFRecord();
		maf3.setVariantClassification("Silent");
		mafs.add(maf3);
		assertEquals(3, MafStatsUtils.getVariantClassifications(mafs).size());	// extra entry for silent/nonSilent ratio
		// silent non silent ratio should be NaN
		ratio = MafStatsUtils.getVariantClassifications(mafs).get(2);
		assertEquals(2.0, Double.parseDouble(ratio.substring(ratio.lastIndexOf(':') + 1)), 0.0001);
		
		// andfinally another non silent one
		MAFRecord maf4 = new MAFRecord();
		maf4.setVariantClassification("Nonstop_Mutation");
		mafs.add(maf4);
		assertEquals(4, MafStatsUtils.getVariantClassifications(mafs).size());	// extra entry for silent/nonSilent ratio
		// silent non silent ratio should be NaN
		ratio = MafStatsUtils.getVariantClassifications(mafs).get(3);
		assertEquals(3.0, Double.parseDouble(ratio.substring(ratio.lastIndexOf(':') + 1)), 0.0001);
		
		// check numbers in list
		List<String> vcs = MafStatsUtils.getVariantClassifications(mafs);
		for (String s : vcs) {
			if (s.contains("Frame_Shift_Del : ")) {
				assertEquals(2, Integer.parseInt(s.substring(s.length() - 1)));
			} else if (s.contains("Silent : ")) {
				assertEquals(1, Integer.parseInt(s.substring(s.length() - 1)));
			} else if (s.contains("Nonstop_Mutation : ")) {
				assertEquals(1, Integer.parseInt(s.substring(s.length() - 1)));
			}
		}
		
	}
	
	@Test
	public void testSomething() {
		
	}
	
}
