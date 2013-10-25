package org.qcmg.common.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class QLoggerFactoryTest {
	
	//private static final String USER_DIR = System.getProperty("user.dir");
	private static final String FILE_SEPERATOR = System.getProperty("file.separator");
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@After
	public void resetRootLogger() {
		QLoggerFactory.reset();
	}
	
	@Test
	public void testGetLogger() {
		// null class
		try {
			QLoggerFactory.getLogger(null);
			Assert.fail("should not get here");
		} catch (Exception e) {}
		
		// valid class
		QLogger qlogger = QLoggerFactory.getLogger(QLoggerFactoryTest.class);
		Assert.assertNotNull(qlogger);
	}
	
	@Test
	public void testGetLoggerMultiArg() {
		// null class
		try {
			QLoggerFactory.getLogger(null, null, null);
			Assert.fail("should not get here");
		} catch (Exception e) {}
		
		// valid class - creates a logger with default parent, no file handler appended
		QLogger qlogger = QLoggerFactory.getLogger(QLoggerFactoryTest.class, null,null);
		Assert.assertNotNull(qlogger);
		Assert.assertEquals(1, qlogger.getLogger().getParent().getHandlers().length);
		Assert.assertNull(qlogger.getLogger().getLevel());
		
		// valid output log file, but invalid level
		try {
			QLoggerFactory.getLogger(QLoggerFactoryTest.class, testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "testlog.log", "");
			Assert.fail("Should not get here");
		} catch (Exception e) {}
		
		// valid output log file, and valid level
		QLogger ql = QLoggerFactory.getLogger(QLoggerFactoryTest.class, testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "testlog.log", "INFO");
		Assert.assertNotNull(ql);
		Assert.assertEquals(2, ql.getLogger().getParent().getHandlers().length);
		Assert.assertEquals(Level.INFO, ql.getLogger().getParent().getLevel());		
		// qlogger's parent should also now have 2 handlers
		Assert.assertEquals(2, qlogger.getLogger().getParent().getHandlers().length);
		
		// invalid output file
		try {
			QLoggerFactory.getLogger(QLoggerFactoryTest.class, FILE_SEPERATOR + "this" 
					+ FILE_SEPERATOR + "should" + FILE_SEPERATOR + "not" + FILE_SEPERATOR + "exist.log", "1");
					
			Assert.fail("Should not get here");
		} catch (IllegalArgumentException e) {}
		
	}
	
	@Test
	public void testLoggingLevel() throws Exception {
		File logFile = testFolder.newFile("testLoggingLevel.log");
		File debugLogFile = testFolder.newFile("testLoggingLevelDebug.log");
		boolean correctHandlerFound = false;
		
		//null level - defaults to INFO
		QLogger logger = QLoggerFactory.getLogger(QLoggerFactoryTest.class, logFile.getAbsolutePath(), null);
		Assert.assertNull(logger.getLogger().getLevel());
		Assert.assertEquals(QLevel.INFO, logger.getLogger().getParent().getLevel());
		// ensure that the root logger has a fileHandler with correct level set
		for (Handler h : logger.getLogger().getParent().getHandlers()) {
			if (h instanceof FileHandler && h.getLevel().equals(QLevel.INFO)){
				correctHandlerFound = true;
			}
		}
		Assert.assertTrue(correctHandlerFound);
		
		// debug level
		QLogger debugLogger = QLoggerFactory.getLogger(QLoggerFactoryTest.class, debugLogFile.getAbsolutePath(), "DEBUG");
		Assert.assertNull(debugLogger.getLogger().getLevel());
		// ensure that the root logger has a fileHandler with correct level set
		correctHandlerFound = false;
		for (Handler h : debugLogger.getLogger().getParent().getHandlers()) {
			if (h instanceof FileHandler && h.getLevel().equals(QLevel.DEBUG)){
				correctHandlerFound = true;
			}
		}
		Assert.assertTrue(correctHandlerFound);
	}
	
	@Test
	public void testDebugLevel() throws Exception {
		File debugLogFile = testFolder.newFile("testDebugLevel.log");
		
		QLogger debugLogger = QLoggerFactory.getLogger(QLoggerFactoryTest.class, debugLogFile.getAbsolutePath(), "DEBUG");
		
		for (int i = 0 ; i < 100 ; i++) {
			debugLogger.debug("testing testing 123");
			debugLogger.info("456");
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(debugLogFile)));
		String line = reader.readLine();
		reader.close();
		Assert.assertTrue(line.contains("DEBUG"));
	}
	
}
