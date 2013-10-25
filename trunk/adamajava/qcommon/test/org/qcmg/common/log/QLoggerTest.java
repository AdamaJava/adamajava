package org.qcmg.common.log;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

public class QLoggerTest {
	
	private final static Logger logger = Logger.getLogger(QLoggerTest.class.getName());

	@Test
	public void testQLogger() {
		// empty constructor
		try {
			new QLogger(null);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		
		QLogger ql = new QLogger(logger);
		Assert.assertNotNull(ql);
	}
	
	@Test
	public void testExecLog() {
		QLogger ql = new QLogger(logger);
		ql.exec("testExecLog");
		ql.tool("testToolLog");
	}
	
	@Test
	public void testIsLevelEnabled() {
		logger.setLevel(Level.INFO);
		QLogger ql = new QLogger(logger);
		Assert.assertTrue(ql.isLevelEnabled(Level.INFO));
		Assert.assertTrue(ql.isLevelEnabled(Level.WARNING));
		Assert.assertTrue(ql.isLevelEnabled(Level.SEVERE));
		
		Assert.assertFalse(ql.isLevelEnabled(Level.FINE));
		Assert.assertFalse(ql.isLevelEnabled(Level.FINER));
		Assert.assertFalse(ql.isLevelEnabled(Level.FINEST));
		
		logger.setLevel(Level.OFF);
		Assert.assertFalse(ql.isLevelEnabled(Level.INFO));
		Assert.assertFalse(ql.isLevelEnabled(Level.WARNING));
		Assert.assertFalse(ql.isLevelEnabled(Level.SEVERE));
		Assert.assertFalse(ql.isLevelEnabled(Level.FINE));
		Assert.assertFalse(ql.isLevelEnabled(Level.FINER));
		Assert.assertFalse(ql.isLevelEnabled(Level.FINEST));
		
		logger.setLevel(Level.ALL);
		Assert.assertTrue(ql.isLevelEnabled(Level.INFO));
		Assert.assertTrue(ql.isLevelEnabled(Level.WARNING));
		Assert.assertTrue(ql.isLevelEnabled(Level.SEVERE));
		Assert.assertTrue(ql.isLevelEnabled(Level.FINE));
		Assert.assertTrue(ql.isLevelEnabled(Level.FINER));
		Assert.assertTrue(ql.isLevelEnabled(Level.FINEST));
		
	}
}
