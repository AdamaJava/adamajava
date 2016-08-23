package org.qcmg.sig;

import static org.junit.Assert.*;

import java.util.Optional;
import java.util.OptionalInt;

import org.junit.Assert;
import org.junit.Test;

public class OptionsTest {
	
	@Test
	public void nullOptions() throws Exception {
		Options options = new Options(new String[] {});
		assertEquals(0, options.getMinCoverage());
		assertEquals(0.0f, options.getCutoff(), 0.000);
		assertEquals(0, options.getNoOfThreads());
		assertEquals(null, options.getMinBaseQuality());
		
		try {
			 options.detectBadOptions();
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e){}
		
	}
	
	@Test
	public void minRGCov() throws Exception {
		Options options = new Options(new String[] {});
		assertEquals(Optional.empty(), options.getMinRGCoverage());
	}

}
