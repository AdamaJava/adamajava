package org.qcmg.common.model;

import org.junit.Assert;
import org.junit.Test;

public class QCMGIntArrayTest {
	
	@Test
	public void testConstructor() {
		QCMGIntArray array = new QCMGIntArray(10);
		
		for (int i = 0 ; i < 20 ; i++) {
			array.increment(i);
		}
		Assert.assertEquals(20, array.length());
		
		array.increment(30);
		Assert.assertEquals(60, array.length());
		
		for (int i = 0 ; i < array.length() ; i++) {
			if (i < 20 || i == 30)
				Assert.assertEquals(1, array.get(i));
			else
				Assert.assertEquals(0, array.get(i));
		}
	}
	
}
