package au.edu.qimr.qannotate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class MainTest {

	@Test
	public void rewriteArgsForModeReplacesModeAndIo() {
		String[] args = {"--mode", "dbsnp", "--mode", "germline", "-i", "in.vcf", "-o", "out.vcf", "-d", "db.vcf"};
		String[] rewritten = Main.rewriteArgsForMode(args, Options.MODE.germline, "tmpIn.vcf", "tmpOut.vcf", Collections.singletonList("db2.vcf"));
		assertArrayEquals(new String[]{"--mode", "germline", "-i", "tmpIn.vcf", "-o", "tmpOut.vcf", "-d", "db2.vcf"}, rewritten);
	}

	@Test
	public void extractDatabaseArgsInOrder() {
		String[] args = {"--mode", "dbsnp", "-d", "db1.vcf", "--database=db2.vcf", "-d=db3.vcf"};
		List<String> dbs = Main.extractDatabaseArgs(args);
		assertEquals(List.of("db1.vcf", "db2.vcf", "db3.vcf"), dbs);
	}
}
