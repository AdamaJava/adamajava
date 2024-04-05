package org.qcmg.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class DonorUtilsTest {
	
	public static final String FS = FileUtils.FILE_SEPARATOR;
	
	@Test
	public void testDoesStringContainDonor() {
		System.out.println("System.getProperty(os.name) " + System.getProperty("os.name"));
        assertFalse(DonorUtils.doesStringContainDonor(null));
        assertFalse(DonorUtils.doesStringContainDonor(""));
        assertFalse(DonorUtils.doesStringContainDonor("Hello"));
        assertFalse(DonorUtils.doesStringContainDonor("APGI_19"));
        assertTrue(DonorUtils.doesStringContainDonor("GI_199"));
        assertTrue(DonorUtils.doesStringContainDonor("PGI_199"));
        assertTrue(DonorUtils.doesStringContainDonor("ME_0001"));
        assertTrue(DonorUtils.doesStringContainDonor("AAAAPGI_1992222"));
        assertFalse(DonorUtils.doesStringContainDonor("APGI__00001"));
	}
	
	@Test
	public void testDoesStringContainDonorOvarian() {
        assertFalse(DonorUtils.doesStringContainDonor("AOCS_00"));
        assertTrue(DonorUtils.doesStringContainDonor("OCS_000"));
        assertTrue(DonorUtils.doesStringContainDonor("CS_000"));
        assertFalse(DonorUtils.doesStringContainDonor("C_000"));
	}
	
	@Test
	public void testGetDonorFromString() {
        assertNull(DonorUtils.getDonorFromString(null));
        assertNull(DonorUtils.getDonorFromString(""));
        assertNull(DonorUtils.getDonorFromString("Hello"));
        assertNull(DonorUtils.getDonorFromString("123456"));
        assertNull(DonorUtils.getDonorFromString("Hello_12345"));
		assertEquals("HELLO_123", DonorUtils.getDonorFromString("HELLO_123"));
		assertEquals("HELLO_1234", DonorUtils.getDonorFromString("SAYHELLO_1234"));
		assertEquals("HELLO_12345", DonorUtils.getDonorFromString("SAYHELLO_12345999"));
	
	}
	@Test
	public void testGetDonorFromFilename() {
        assertNull(DonorUtils.getDonorFromFilename(null));
        assertNull(DonorUtils.getDonorFromFilename(""));
        assertNull(DonorUtils.getDonorFromFilename("Hello"));
        assertNull(DonorUtils.getDonorFromFilename("123456"));
        assertNull(DonorUtils.getDonorFromFilename("Hello_12345"));
		assertEquals("ELLO_123", DonorUtils.getDonorFromFilename("H" + FS + "ELLO_123" + FS));
		assertEquals("ELLO_1234", DonorUtils.getDonorFromFilename("H" + FS + "ELLO_1234" + FS));
		assertEquals("ELLO_1234", DonorUtils.getDonorFromFilename("H" + FS + "ELLO_1234" + FS + "5"));
	}
	
	@Test
	public void testRealLifeData() {
        assertTrue(DonorUtils.doesStringContainDonor("/test/test_results/testresults/ALGP_0000/SNP_array/test.txt.qsig.vcf"));
		assertEquals("ALGP_0000", DonorUtils.getDonorFromString("/test/test_results/testresults/ALGP_0000/SNP_array/test.txt.qsig.vcf"));
        assertTrue(DonorUtils.doesStringContainDonor("/test/results/test_results/testresults/ALGP_0001/SNP_array/testsample.txt.qsig.vcf"));
		assertEquals("ALGP_0001", DonorUtils.getDonorFromString("/test/results/test_results/testresults/ALGP_0001/SNP_array/testsample.txt.qsig.vcf"));
        assertNull(DonorUtils.getDonorFromFilename("/test/results/test_results/testresults/ABC_2547_Banc_10_05/SNP_array/testsample.txt.qsig.vcf"));
	}
	
	@Test
	public void testBrainIds() {
        assertTrue(DonorUtils.doesStringContainDonor("test/test_results/testresults/SLLL_1234/test.qsig.vcf"));
		assertEquals("SLLL_1234", DonorUtils.getDonorFromFilename("test/test_results/testresults/SLLL_1234/test.qsig.vcf".replace("/", FS)));
	}
	
	@Test
	public void testNzNet() {
        assertTrue(DonorUtils.doesStringContainDonor("/test/test_results/testresults/HELLO_12345/test.qsig.vcf"));
		assertEquals("HELLO_12345", DonorUtils.getDonorFromFilename("/test/test_results/testresults/HELLO_12345/test.qsig.vcf".replace("/", FS)));
	}
	@Test
	public void testItNet() {
        assertTrue(DonorUtils.doesStringContainDonor("/test/test_results/testresults/WORLD_1234/test.qsig.vcf"));
		assertEquals("WORLD_1234", DonorUtils.getDonorFromFilename("/test/test_results/testresults/WORLD_1234/test.qsig.vcf".replace("/", FS)));
	}
	@Test
	public void testEmt() {
        assertTrue(DonorUtils.doesStringContainDonor("/test/test_results/testresults/EMT_0000/test.qsig.vcf"));
		assertEquals("EMT_0000", DonorUtils.getDonorFromFilename("/test/test_results/testresults/EMT_0000/test.qsig.vcf".replace("/", FS)));
	}
	@Test
	public void testEndo() {
        assertTrue(DonorUtils.doesStringContainDonor("/test/test_results/testresults/PPPP_AN3CA/test.qsig.vcf"));
		assertEquals("PPPP_AN3CA", DonorUtils.getDonorFromFilename("/test/test_results/testresults/PPPP_AN3CA/test.qsig.vcf".replace("/", FS)));

        assertTrue(DonorUtils.doesStringContainDonor("/test/test_results/testresults/PPPP_0000X/test.qsig.vcf"));
		assertEquals("PPPP_0000X", DonorUtils.getDonorFromFilename("/test/test_results/testresults/PPPP_0000X/test.qsig.vcf".replace("/", FS)));

        assertTrue(DonorUtils.doesStringContainDonor("/test/test_results/testresults/PPPP_0000X/test.qsig.vcf"));
		assertEquals("PPPP_0000X", DonorUtils.getDonorFromFilename("/test/test_results/testresults/PPPP_0000X/test.qsig.vcf".replace("/", FS)));
	}

}
