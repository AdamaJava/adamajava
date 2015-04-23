package au.edu.qimr.clinvar;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class EditDistance {
	
	@Test
	public void editDistanceOfZero() {
		String s1 = "GGGGAGTAGAAGGCAAATAAGGAAAAGGATTAGAAATAATAATAATAATCAACCCGCATAGCAGCTGATTTCTCTAGCCCAACCTGCTAATTAATATATGCTAAAGTGAGAGGGGGCAATACTGGAGGGAGGGGAGAATTGGGGAGACCAC";
		String s2 = "GGGGAGTAGAAGGCAAATAAGGAAAAG";
		
		assertEquals(0, StringUtils.getLevenshteinDistance(s2, s1.subSequence(0, s2.length())));
	}

}
