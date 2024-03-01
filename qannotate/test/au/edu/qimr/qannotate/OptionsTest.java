package au.edu.qimr.qannotate;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OptionsTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void getDB() throws IOException {
		File i = testFolder.newFile();
		Options o = new Options(new String[]{"--mode", "make_valid","-i",i.getAbsolutePath()});
		assertEquals(null, o.getDatabaseFileName());
		
		File d = testFolder.newFile();
		o = new Options(new String[]{"--mode", "make_valid","-i",i.getAbsolutePath(), "-d", d.getAbsolutePath()});
		assertEquals(d.getAbsolutePath(), o.getDatabaseFileName());
	}
	
	@Test
	public void outputOption() throws IOException {
		File i = testFolder.newFile();
		File output = testFolder.newFile();
		Options o = new Options(new String[]{"--mode", "make_valid","-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(output.getAbsolutePath(), o.getOutputFileName());
		o = new Options(new String[]{"--mode", "make_valid","-o",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(output.getAbsolutePath(), o.getOutputFileName());
		o = new Options(new String[]{"--mode", "hom","-o",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(output.getAbsolutePath(), o.getOutputFileName());
		o = new Options(new String[]{"--mode", "hom","-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(output.getAbsolutePath(), o.getOutputFileName());
	}

	@Test
	public void homCutoff() throws IOException {
		File i = testFolder.newFile();
		File output = testFolder.newFile();
		Options o = new Options(new String[]{"--mode", "confidence","-homCutoff", "10", "-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(Optional.of(10), o.getHomoplymersCutoff());

		o = new Options(new String[]{"--mode", "confidence","-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(Optional.empty(), o.getHomoplymersCutoff());

		o = new Options(new String[]{"--mode", "trf","-homCutoff", "10", "-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(Optional.of(10), o.getHomoplymersCutoff());
	}

	@Test
	public void homWindow() throws IOException {
		File i = testFolder.newFile();
		File output = testFolder.newFile();
		Options o = new Options(new String[]{"--mode", "hom","-homWindow", "50", "-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(Optional.of(50), o.getHomoplymersWindow());

		o = new Options(new String[]{"--mode", "hom","-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(Optional.empty(), o.getHomoplymersWindow());

		o = new Options(new String[]{"--mode", "cadd","-homWindow", "42", "-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(Optional.of(42), o.getHomoplymersWindow());
	}

	@Test
	public void homReportWindow() throws IOException {
		File i = testFolder.newFile();
		File output = testFolder.newFile();
		Options o = new Options(new String[]{"--mode", "hom","-homReportWindow", "5", "-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(Optional.of(5), o.getHomoplymersReportWindow());

		o = new Options(new String[]{"--mode", "hom","-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(Optional.empty(), o.getHomoplymersReportWindow());

		o = new Options(new String[]{"--mode", "confidence","-homReportWindow", "42", "-output",output.getAbsolutePath(),"-i",i.getAbsolutePath(),});
		assertEquals(Optional.of(42), o.getHomoplymersReportWindow());
	}

}
