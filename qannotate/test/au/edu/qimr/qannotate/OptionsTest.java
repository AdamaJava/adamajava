package au.edu.qimr.qannotate;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

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

}
