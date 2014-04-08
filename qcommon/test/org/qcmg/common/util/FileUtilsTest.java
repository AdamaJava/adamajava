package org.qcmg.common.util;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileUtilsTest {
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	@Test
	public void testOutputFile() throws Exception{
		// null outputFile
//		try {
			Assert.assertFalse(FileUtils.validOutputFile(null));
//			Assert.fail("Shouldn't have got here");
//		} catch (Exception e) {}
		
		// empty string file
		Assert.assertFalse(FileUtils.validOutputFile(""));
		
		// non-existant directory
		Assert.assertFalse(FileUtils.validOutputFile("/extra/special/test1.html"));
		
		// extension does not match file type
		Assert.assertFalse(FileUtils.validOutputFile("/test1.html", "xml"));
		
		File tmpDir = tempFolder.newFolder();
		// non-existant file, but directory is OK
		Assert.assertTrue(FileUtils.validOutputFile(tmpDir.getAbsolutePath() + FileUtils.FILE_SEPARATOR + "test1.html"));
		
		// existing file
		File testFile = File.createTempFile("test", null, tmpDir);
		testFile.deleteOnExit();
		Assert.assertTrue(FileUtils.validOutputFile(testFile.getAbsolutePath()));
	}
	
	
	@Test
	public void testInputFile() throws Exception {
		// null input file
		try {
			FileUtils.validInputFile(null);
			Assert.fail("Shouldn't have got here");
		} catch (Exception e) {}
		
		// empty string file
		Assert.assertFalse(FileUtils.validInputFile(""));
		
		// non-existant directory
		Assert.assertFalse(FileUtils.validInputFile("/extra/special/test1.html"));
		
		// extention does not match file type
		Assert.assertFalse(FileUtils.validInputFile("/test1.xml", "html"));
		
		// existing file
		File testFile = File.createTempFile("test", null, tempFolder.newFolder());
		testFile.deleteOnExit();
		Assert.assertTrue(FileUtils.validInputFile(testFile.getAbsolutePath()));
	}
	
	@Test
	public void testIsFileTypeValid()  {
		// null file, null ext
		try {
			FileUtils.isFileTypeValid((File)null, null);
			Assert.fail("Shouldn't have got here");
		} catch (Exception e) {}
		
		// valid file, null extension
		try {
			FileUtils.isFileTypeValid("/test1.txt", null);
			Assert.fail("Shouldn't have got here");
		} catch (Exception e) {}
		
		// null file, valid extension
		try {
			FileUtils.isFileTypeValid((String)null, "txt");
			Assert.fail("Shouldn't have got here");
		} catch (Exception e) {}
		
		// valid file, incorrect extension
		Assert.assertFalse(FileUtils.isFileTypeValid("test1.html", "txt"));
		// valid file, incorrect extension
		Assert.assertFalse(FileUtils.isFileTypeValid("test1.html", "html5"));
		// valid file, incorrect extension
		Assert.assertFalse(FileUtils.isFileTypeValid("test1.html5", "html"));
		// valid file, incorrect extension
		Assert.assertFalse(FileUtils.isFileTypeValid("test1", "test1"));
		// invalid file, empty extension
		Assert.assertFalse(FileUtils.isFileTypeValid("test1.", ""));
		
		//valid file, correct extension
		Assert.assertTrue(FileUtils.isFileTypeValid("test1.html", "html"));
		//valid file, correct extension
		Assert.assertTrue(FileUtils.isFileTypeValid("test1.test2.xml", "xml"));
		//valid file, correct extension
		Assert.assertTrue(FileUtils.isFileTypeValid("/test1/test2/xml.xml", "xml"));
		//valid file, correct extension
		Assert.assertTrue(FileUtils.isFileTypeValid("/test1/test2/test3.a", "a"));
	}
	
	@Test
	public void testCanFileBeWrittenTo() {
		Assert.assertFalse(FileUtils.canFileBeWrittenTo(""));
	}
	
	@Test
	public void testGetBamIndexFile() throws IOException {
		// null
		File indexFile = FileUtils.getBamIndexFile(null);
		Assert.assertNull(indexFile);
		
		// empty string
		indexFile = FileUtils.getBamIndexFile("");
		Assert.assertNull(indexFile);
		
		// garbage file
		indexFile = FileUtils.getBamIndexFile("asdfasdfasdfsa.AFDASDFASD");
		Assert.assertNull(indexFile);
		
		// actual file
		File bamFile = tempFolder.newFile("testBamFile.bam");
		indexFile = FileUtils.getBamIndexFile(bamFile.getAbsolutePath());
		Assert.assertNull(indexFile);
		
		// now create some index files
		File bamBaiIndexFile = tempFolder.newFile("testBamFile.bam.bai");
		indexFile = FileUtils.getBamIndexFile(bamFile.getAbsolutePath());
		Assert.assertEquals(bamBaiIndexFile, indexFile);
		
		// delete the index file
		bamBaiIndexFile.delete();
		
		// try again with the differently named index file - should still work
		File baiIndexFile = tempFolder.newFile("testBamFile.bai");
		indexFile = FileUtils.getBamIndexFile(bamFile.getAbsolutePath());
		Assert.assertEquals(baiIndexFile, indexFile);
	}
	
	@Test
	public void testIsFileGzip() {
		try {
			Assert.assertFalse(FileUtils.isFileGZip(null));
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		//no
		Assert.assertFalse(FileUtils.isFileGZip(new File("")));
		Assert.assertFalse(FileUtils.isFileGZip(new File("testing")));
		Assert.assertFalse(FileUtils.isFileGZip(new File("testing.testing")));
		Assert.assertFalse(FileUtils.isFileGZip(new File("testing.testing.gzz")));
		//yes
		Assert.assertTrue(FileUtils.isFileGZip(new File("testing.testing.gz")));
		Assert.assertTrue(FileUtils.isFileGZip(new File("testing.testing.gzip")));
		Assert.assertTrue(FileUtils.isFileGZip(new File("testing.testing.gzip.gz")));
	}
	
	@Test
	public void testIsFileEmpty() throws IOException {
		File emptyFile = tempFolder.newFile("empty");
		Assert.assertEquals(true, FileUtils.isFileEmpty(emptyFile));
		Assert.assertEquals(true, FileUtils.isFileEmpty(emptyFile.getAbsolutePath()));
		
		// put stuff in file to create non-empty file and then test
		FileWriter writer = new FileWriter(emptyFile);
		try {
			writer.write("non-empty file");
			writer.flush();
		} finally {
			writer.close();
		}
		Assert.assertEquals(false, FileUtils.isFileEmpty(emptyFile));
		Assert.assertEquals(false, FileUtils.isFileEmpty(emptyFile.getAbsolutePath()));
	}
	
	@Test
	public void testFindFilesEndingWithFilter() throws IOException {
		try {
			FileUtils.findFilesEndingWithFilter(null, null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			FileUtils.findFilesEndingWithFilter("", null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			FileUtils.findFilesEndingWithFilter("", "");
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		File dir = tempFolder.newFolder("testFolder");
		File file = tempFolder.newFile("test");
		File fileInFolder = tempFolder.newFile("testFolder" + FileUtils.FILE_SEPARATOR + "test");
		File nestedDir = tempFolder.newFolder("testFolder" + FileUtils.FILE_SEPARATOR + "innerTestFolder");
		File nestedFileInFolder = tempFolder.newFile("testFolder" + FileUtils.FILE_SEPARATOR + "innerTestFolder" + FileUtils.FILE_SEPARATOR + "test");
		
		try {
			FileUtils.findFilesEndingWithFilter(file.getAbsolutePath(), null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			FileUtils.findFilesEndingWithFilter(file.getAbsolutePath(), "");
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		Assert.assertEquals(0, FileUtils.findFilesEndingWithFilter(file.getAbsolutePath(), "TEST").length);
		Assert.assertEquals(1, FileUtils.findFilesEndingWithFilter(file.getAbsolutePath(), "test").length);
		Assert.assertEquals(file, FileUtils.findFilesEndingWithFilter(file.getAbsolutePath(), "test")[0]);
		
		Assert.assertEquals(2, FileUtils.findFilesEndingWithFilter(dir.getAbsolutePath(), "test", true).length);
		Assert.assertEquals(fileInFolder, FileUtils.findFilesEndingWithFilter(dir.getAbsolutePath(), "test", true)[0]);
		Assert.assertEquals(nestedFileInFolder, FileUtils.findFilesEndingWithFilter(dir.getAbsolutePath(), "test", true)[1]);
	}
	
	@Test
	public void testFindFilesEndingWithFilterNIO() throws IOException {
		try {
			FileUtils.findFilesEndingWithFilterNIO(null, null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			FileUtils.findFilesEndingWithFilterNIO("", null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			FileUtils.findFilesEndingWithFilterNIO("", "");
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		File dir = tempFolder.newFolder("testFolder");
		File file = tempFolder.newFile("test");
		File fileInFolder = tempFolder.newFile("testFolder" + FileUtils.FILE_SEPARATOR + "test");
		File nestedDir = tempFolder.newFolder("testFolder" + FileUtils.FILE_SEPARATOR + "innerTestFolder");
		File nestedFileInFolder = tempFolder.newFile("testFolder" + FileUtils.FILE_SEPARATOR + "innerTestFolder" + FileUtils.FILE_SEPARATOR + "test");
		
		try {
			FileUtils.findFilesEndingWithFilterNIO(file.getAbsolutePath(), null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			FileUtils.findFilesEndingWithFilterNIO(file.getAbsolutePath(), "");
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		Assert.assertEquals(0, FileUtils.findFilesEndingWithFilterNIO(file.getAbsolutePath(), "TEST").size());
		Assert.assertEquals(1, FileUtils.findFilesEndingWithFilterNIO(file.getAbsolutePath(), "test").size());
		Assert.assertEquals(file, FileUtils.findFilesEndingWithFilterNIO(file.getAbsolutePath(), "test").get(0));
		Assert.assertEquals(2, FileUtils.findFilesEndingWithFilterNIO(dir.getAbsolutePath(), "test").size());
		Assert.assertEquals(true, FileUtils.findFilesEndingWithFilterNIO(dir.getAbsolutePath(), "test").contains(fileInFolder));
		Assert.assertEquals(true, FileUtils.findFilesEndingWithFilterNIO(dir.getAbsolutePath(), "test").contains(nestedFileInFolder));
	}
	
	@Test
	public void testFindFiles() throws IOException {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return name.startsWith("PPPP_") && new File(file +  FileUtils.FILE_SEPARATOR + name).isDirectory(); 
			}};
		
		try {
			FileUtils.findFiles(null, null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			FileUtils.findFiles("", null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		File file = tempFolder.newFile("test");
		Assert.assertEquals(1, FileUtils.findFiles(file.getAbsolutePath(), filter).length);
		Assert.assertEquals(file, FileUtils.findFiles(file.getAbsolutePath(), filter)[0]);
		
		File dirXXX = tempFolder.newFolder("XXX");
		File dirPPP = tempFolder.newFolder("PPP");
		File dirPPPP = tempFolder.newFolder("PPPP_");
		
		Assert.assertEquals(1, FileUtils.findFiles(tempFolder.getRoot().getAbsolutePath(), filter).length);
		Assert.assertEquals(dirPPPP, FileUtils.findFiles(tempFolder.getRoot().getAbsolutePath(), filter)[0]);
	}
	
	@Test
	public void testFindDirectories() throws IOException {
		try {
			FileUtils.findDirectories(null, null, false);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			FileUtils.findDirectories("", null, false);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		File file = tempFolder.newFile("test");
		
		Assert.assertEquals(0, FileUtils.findDirectories(tempFolder.getRoot().getAbsolutePath(), "test", false).length);
		
		File dir = tempFolder.newFolder("testing");
		Assert.assertEquals(1, FileUtils.findDirectories(tempFolder.getRoot().getAbsolutePath(), "testing", false).length);
		Assert.assertEquals(dir, FileUtils.findDirectories(tempFolder.getRoot().getAbsolutePath(), "testing", false)[0]);
	}
	
	@Ignore
	public void testNio() throws IOException {
		long start = System.currentTimeMillis();
		String path = "/Users/o.holmes";
		
		final List<File> foundFiles = new ArrayList<File>();
		
		Path startingDir = Paths.get("/Users/test");
		Files.walkFileTree(startingDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (file.toString().endsWith(".java")) {
//					if (file.toString().endsWith(".java")) {
					foundFiles.add(file.toFile());
				}
				return FileVisitResult.CONTINUE;
			}
		});
		
		System.out.println("nio, foundFiles size: " + foundFiles.size() +", time taken: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		File[] oldSchoolFoundFiles = FileUtils.findFilesEndingWithFilter(path, ".java", true);
		System.out.println("old skool, foundFiles size: " + oldSchoolFoundFiles.length +", time taken: " + (System.currentTimeMillis() - start));
		
	}
}
