package org.qcmg.maf.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qcmg.common.model.MafType;

public class MafTypeTest {
	
	@Test
	public void testIsSomatic() {
		assertEquals(true, MafType.SNV_SOMATIC.isSomatic());
		assertEquals(true, MafType.INDEL_SOMATIC.isSomatic());
		assertEquals(false, MafType.SNV_GERMLINE.isSomatic());
		assertEquals(false, MafType.INDEL_GERMLINE.isSomatic());
	}
	
	@Test
	public void testIsGermline() {
		assertEquals(false, MafType.SNV_SOMATIC.isGermline());
		assertEquals(false, MafType.INDEL_SOMATIC.isGermline());
		assertEquals(true, MafType.SNV_GERMLINE.isGermline());
		assertEquals(true, MafType.INDEL_GERMLINE.isGermline());
	}
	
	@Test
	public void testIsIndel() {
		assertEquals(false, MafType.SNV_SOMATIC.isIndel());
		assertEquals(false, MafType.SNV_GERMLINE.isIndel());
		assertEquals(true, MafType.INDEL_SOMATIC.isIndel());
		assertEquals(true, MafType.INDEL_GERMLINE.isIndel());
	}

}
