package org.qcmg.qmule.util;

import junit.framework.Assert;

import org.junit.Test;

public class TabbedDataLoaderTest {

	@Test
	public void testGetStringFromArray() {
		Assert.assertNull(TabbedDataLoader.getStringFromArray(null, -1));
		Assert.assertNull(TabbedDataLoader.getStringFromArray(new String[] {}, -1));
		Assert.assertNull(TabbedDataLoader.getStringFromArray(new String[] {}, 0));
		Assert.assertEquals("Hello", TabbedDataLoader.getStringFromArray(new String[] {"Hello"}, 0));
		Assert.assertEquals("Hello", TabbedDataLoader.getStringFromArray(new String[] {"Hello"}, -1));
		Assert.assertNull(TabbedDataLoader.getStringFromArray(new String[] {"Hello"}, -10));
		Assert.assertEquals("there", TabbedDataLoader.getStringFromArray(new String[] {"Hello", "there"}, -1));
		Assert.assertEquals("there", TabbedDataLoader.getStringFromArray(new String[] {"Hello", "1", "2", "3", "there"}, -1));
		Assert.assertEquals("1", TabbedDataLoader.getStringFromArray(new String[] {"Hello", "1", "2", "3", "there"}, 1));
	}
}
