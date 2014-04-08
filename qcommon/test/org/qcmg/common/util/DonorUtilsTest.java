package org.qcmg.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DonorUtilsTest {
	
	public static final String FS = FileUtils.FILE_SEPARATOR;
	
	@Test
	public void testDoesStringContainDonor() {
		System.out.println("System.getProperty(os.name) " + System.getProperty("os.name"));
		assertEquals(false, DonorUtils.doesStringContainDonor(null));
		assertEquals(false, DonorUtils.doesStringContainDonor(""));
		assertEquals(false, DonorUtils.doesStringContainDonor("Hello"));
		assertEquals(false, DonorUtils.doesStringContainDonor("APGI_19"));
		assertEquals(true, DonorUtils.doesStringContainDonor("GI_199"));
		assertEquals(true, DonorUtils.doesStringContainDonor("PGI_199"));
		assertEquals(true, DonorUtils.doesStringContainDonor("ME_0001"));
		assertEquals(true, DonorUtils.doesStringContainDonor("AAAAPGI_1992222"));
		assertEquals(false, DonorUtils.doesStringContainDonor("APGI__00001"));
	}
	
	@Test
	public void testDoesStringContainDonorOvarian() {
		assertEquals(false, DonorUtils.doesStringContainDonor("AOCS_00"));
		assertEquals(true, DonorUtils.doesStringContainDonor("OCS_000"));
		assertEquals(true, DonorUtils.doesStringContainDonor("CS_000"));
		assertEquals(false, DonorUtils.doesStringContainDonor("C_000"));
	}
	
	@Test
	public void testGetDonorFromString() {
		assertEquals(null, DonorUtils.getDonorFromString(null));
		assertEquals(null, DonorUtils.getDonorFromString(""));
		assertEquals(null, DonorUtils.getDonorFromString("Hello"));
		assertEquals(null, DonorUtils.getDonorFromString("123456"));
		assertEquals(null, DonorUtils.getDonorFromString("Hello_12345"));
		assertEquals("HELLO_123", DonorUtils.getDonorFromString("HELLO_123"));
		assertEquals("HELLO_1234", DonorUtils.getDonorFromString("SAYHELLO_1234"));
		assertEquals("HELLO_12345", DonorUtils.getDonorFromString("SAYHELLO_12345999"));
	
	}
	@Test
	public void testGetDonorFromFilename() {
		assertEquals(null, DonorUtils.getDonorFromFilename(null));
		assertEquals(null, DonorUtils.getDonorFromFilename(""));
		assertEquals(null, DonorUtils.getDonorFromFilename("Hello"));
		assertEquals(null, DonorUtils.getDonorFromFilename("123456"));
		assertEquals(null, DonorUtils.getDonorFromFilename("Hello_12345"));
		assertEquals("ELLO_123", DonorUtils.getDonorFromFilename("H" + FS + "ELLO_123" + FS));
		assertEquals("ELLO_1234", DonorUtils.getDonorFromFilename("H" + FS + "ELLO_1234" + FS));
		assertEquals("ELLO_1234", DonorUtils.getDonorFromFilename("H" + FS + "ELLO_1234" + FS + "5"));
	}
	
	@Test
	public void testRealLifeData() {
		assertEquals(true, DonorUtils.doesStringContainDonor("/test/test_results/testresults/ALGP_0000/SNP_array/test.txt.qsig.vcf"));
		assertEquals("ALGP_0000", DonorUtils.getDonorFromString("/test/test_results/testresults/ALGP_0000/SNP_array/test.txt.qsig.vcf"));
		assertEquals(true, DonorUtils.doesStringContainDonor("/test/results/test_results/testresults/ALGP_0001/SNP_array/testsample.txt.qsig.vcf"));
		assertEquals("ALGP_0001", DonorUtils.getDonorFromString("/test/results/test_results/testresults/ALGP_0001/SNP_array/testsample.txt.qsig.vcf"));
		assertEquals(null, DonorUtils.getDonorFromFilename("/test/results/test_results/testresults/ABC_2547_Banc_10_05/SNP_array/testsample.txt.qsig.vcf"));
	}
	
	@Test
	public void testBrainIds() {		
		assertEquals(true, DonorUtils.doesStringContainDonor("test/test_results/testresults/SLLL_1234/test.qsig.vcf"));
		assertEquals("SLLL_1234", DonorUtils.getDonorFromFilename("test/test_results/testresults/SLLL_1234/test.qsig.vcf".replace("/", FS)));
	}
	
	@Test
	public void testNzNet() {		
		assertEquals(true, DonorUtils.doesStringContainDonor("/test/test_results/testresults/HELLO_12345/test.qsig.vcf"));
		assertEquals("HELLO_12345", DonorUtils.getDonorFromFilename("/test/test_results/testresults/HELLO_12345/test.qsig.vcf".replace("/", FS)));
	}
	@Test
	public void testItNet() {		
		assertEquals(true, DonorUtils.doesStringContainDonor("/test/test_results/testresults/WORLD_1234/test.qsig.vcf"));
		assertEquals("WORLD_1234", DonorUtils.getDonorFromFilename("/test/test_results/testresults/WORLD_1234/test.qsig.vcf".replace("/", FS)));
	}
	@Test
	public void testEmt() {		
		assertEquals(true, DonorUtils.doesStringContainDonor("/test/test_results/testresults/EMT_0000/test.qsig.vcf"));
		assertEquals("EMT_0000", DonorUtils.getDonorFromFilename("/test/test_results/testresults/EMT_0000/test.qsig.vcf".replace("/", FS)));
	}
	@Test
	public void testEndo() {		
		assertEquals(true, DonorUtils.doesStringContainDonor("/test/test_results/testresults/PPPP_AN3CA/test.qsig.vcf"));
		assertEquals("PPPP_AN3CA", DonorUtils.getDonorFromFilename("/test/test_results/testresults/PPPP_AN3CA/test.qsig.vcf".replace("/", FS)));
		
		assertEquals(true, DonorUtils.doesStringContainDonor("/test/test_results/testresults/PPPP_0000X/test.qsig.vcf"));
		assertEquals("PPPP_0000X", DonorUtils.getDonorFromFilename("/test/test_results/testresults/PPPP_0000X/test.qsig.vcf".replace("/", FS)));
		
		assertEquals(true, DonorUtils.doesStringContainDonor("/test/test_results/testresults/PPPP_0000X/test.qsig.vcf"));
		assertEquals("PPPP_0000X", DonorUtils.getDonorFromFilename("/test/test_results/testresults/PPPP_0000X/test.qsig.vcf".replace("/", FS)));
	}

}
