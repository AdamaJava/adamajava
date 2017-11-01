package au.edu.qimr.qannotate.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CCMTest {
	
	@Test
	public void getGermlineSomatic() {
		assertEquals(CCM.TWENTY_FIVE, CCM.getCCM(25));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(25)));
		assertEquals("Somatic", CCM.getTest(CCM.getCCM(25)));
		
		assertEquals(CCM.TWENTY_SIX, CCM.getCCM(26));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(26)));
		assertEquals("Somatic", CCM.getTest(CCM.getCCM(26)));
		
		assertEquals(CCM.THIRTY_FIVE, CCM.getCCM(35));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(35)));
		assertEquals("Somatic", CCM.getTest(CCM.getCCM(35)));
	}
	
	@Test
	public void getGermlineLOH() {
		assertEquals(CCM.TWENTY_TWO, CCM.getCCM(22));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(22)));
		assertEquals("ReferenceNoVariant", CCM.getTest(CCM.getCCM(22)));
		
		assertEquals(CCM.TWENTY_FOUR, CCM.getCCM(24));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(24)));
		assertEquals("GermlineNoReference", CCM.getTest(CCM.getCCM(24)));
		
		assertEquals(CCM.FORTY_FIVE, CCM.getCCM(45));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(45)));
		assertEquals("SomaticLostVariant", CCM.getTest(CCM.getCCM(45)));
		
		assertEquals(CCM.FORTY_SIX, CCM.getCCM(46));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(46)));
		assertEquals("SomaticLostVariant", CCM.getTest(CCM.getCCM(46)));
	}
	
	@Test
	public void getHomozygousLoss() {
		assertEquals(CCM.ELEVEN, CCM.getCCM(11));
		assertEquals("Reference", CCM.getControl(CCM.getCCM(11)));
		assertEquals("HomozygousLoss", CCM.getTest(CCM.getCCM(11)));
		
		assertEquals(CCM.TWENTY_ONE, CCM.getCCM(21));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(21)));
		assertEquals("HomozygousLoss", CCM.getTest(CCM.getCCM(21)));
		
		assertEquals(CCM.THIRTY_ONE, CCM.getCCM(31));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(31)));
		assertEquals("HomozygousLoss", CCM.getTest(CCM.getCCM(31)));
		
		assertEquals(CCM.FORTY_ONE, CCM.getCCM(41));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(41)));
		assertEquals("HomozygousLoss", CCM.getTest(CCM.getCCM(41)));
	}
	@Test
	public void getReference() {
		assertEquals(CCM.TWELVE, CCM.getCCM(12));
		assertEquals("Reference", CCM.getControl(CCM.getCCM(12)));
		assertEquals("Reference", CCM.getTest(CCM.getCCM(12)));
	}
	
	@Test
	public void getReversionToReference() {
		assertEquals(CCM.THIRTY_TWO, CCM.getCCM(32));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(32)));
		assertEquals("ReferenceNoVariant", CCM.getTest(CCM.getCCM(32)));
		
		assertEquals(CCM.THIRTY_THREE, CCM.getCCM(33));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(33)));
		assertEquals("GermlineReversionToReference", CCM.getTest(CCM.getCCM(33)));
		
		assertEquals(CCM.FORTY_TWO, CCM.getCCM(42));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(42)));
		assertEquals("ReferenceNoVariant", CCM.getTest(CCM.getCCM(42)));
		
		assertEquals(CCM.FORTY_THREE, CCM.getCCM(43));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(43)));
		assertEquals("GermlineReversionToReference", CCM.getTest(CCM.getCCM(43)));
		
		assertEquals(CCM.FORTY_FOUR, CCM.getCCM(44));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(44)));
		assertEquals("GermlineReversionToReference", CCM.getTest(CCM.getCCM(44)));
		
	}
	
	@Test
	public void getGermlineGermline() {
		assertEquals(CCM.TWENTY_THREE, CCM.getCCM(23));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(23)));
		assertEquals("Germline", CCM.getTest(CCM.getCCM(23)));
		
		assertEquals(CCM.THIRTY_FOUR, CCM.getCCM(34));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(34)));
		assertEquals("Germline", CCM.getTest(CCM.getCCM(34)));
		
		assertEquals(CCM.FORTY_SEVEN, CCM.getCCM(47));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(47)));
		assertEquals("Germline", CCM.getTest(CCM.getCCM(47)));
	}
	
	@Test
	public void getGermlineDoubleSomatic() {
		assertEquals(CCM.FIFTY_ONE, CCM.getCCM(51));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(51)));
		assertEquals("Somatic", CCM.getTest(CCM.getCCM(51)));
		
		assertEquals(CCM.FIFTY, CCM.getCCM(50));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(50)));
		assertEquals("DoubleSomatic", CCM.getTest(CCM.getCCM(50)));
		
		assertEquals(CCM.FORTY_NINE, CCM.getCCM(49));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(49)));
		assertEquals("DoubleSomatic", CCM.getTest(CCM.getCCM(49)));
		
		assertEquals(CCM.FORTY_EIGHT, CCM.getCCM(48));
		assertEquals("DoubleGermline", CCM.getControl(CCM.getCCM(48)));
		assertEquals("DoubleSomatic", CCM.getTest(CCM.getCCM(48)));
		
		assertEquals(CCM.THIRTY_EIGHT, CCM.getCCM(38));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(38)));
		assertEquals("DoubleSomatic", CCM.getTest(CCM.getCCM(38)));
		
		assertEquals(CCM.THIRTY_SEVEN, CCM.getCCM(37));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(37)));
		assertEquals("DoubleSomatic", CCM.getTest(CCM.getCCM(37)));
		
		assertEquals(CCM.THIRTY_SIX, CCM.getCCM(36));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(36)));
		assertEquals("DoubleSomatic", CCM.getTest(CCM.getCCM(36)));
		
		assertEquals(CCM.TWENTY_SEVEN, CCM.getCCM(27));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(27)));
		assertEquals("DoubleSomatic", CCM.getTest(CCM.getCCM(27)));
		
		assertEquals(CCM.TWENTY_EIGHT, CCM.getCCM(28));
		assertEquals("Germline", CCM.getControl(CCM.getCCM(28)));
		assertEquals("DoubleSomatic", CCM.getTest(CCM.getCCM(28)));
		
		assertEquals(CCM.FOURTEEN, CCM.getCCM(14));
		assertEquals("Reference", CCM.getControl(CCM.getCCM(14)));
		assertEquals("SomaticNoReference", CCM.getTest(CCM.getCCM(14)));
		
		assertEquals(CCM.FIFTEEN, CCM.getCCM(15));
		assertEquals("Reference", CCM.getControl(CCM.getCCM(15)));
		assertEquals("DoubleSomatic", CCM.getTest(CCM.getCCM(15)));
	}
	
}
