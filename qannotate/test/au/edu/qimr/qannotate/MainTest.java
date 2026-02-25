package au.edu.qimr.qannotate;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class MainTest {

	@Test
	public void rewriteArgsForModeReplacesModeAndIo() {
		String[] args = {"--mode", "dbsnp", "--mode", "germline", "-i", "in.vcf", "-o", "out.vcf", "-d", "db.vcf"};
		String[] rewritten = Main.rewriteArgsForMode(args, Options.MODE.germline, "tmpIn.vcf", "tmpOut.vcf");
		assertArrayEquals(new String[]{"-d", "db.vcf", "--mode", "germline", "-i", "tmpIn.vcf", "-o", "tmpOut.vcf"}, rewritten);
	}
}
