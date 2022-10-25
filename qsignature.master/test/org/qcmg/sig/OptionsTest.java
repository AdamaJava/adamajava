package org.qcmg.sig;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class OptionsTest {
	
	@Test
	public void nullOptions() throws Exception {
		Options options = new Options(new String[] {});
		assertEquals(Optional.empty(), options.getMinCoverage());
		assertEquals(0.0f, options.getCutoff(), 0.000);
		assertEquals(Optional.empty(), options.getNoOfThreads());
		assertEquals(Optional.empty(), options.getMinBaseQuality());
		
		try {
			 options.detectBadOptions();
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e){}
		
	}
	
	@Test
	public void minRGCov() throws Exception {
		Options options = new Options(new String[] {});
		assertEquals(Optional.empty(), options.getMinRGCoverage());
		options = new Options(new String[] {"-minRGCoverage", "10"});
		assertEquals(Optional.of(Integer.valueOf(10)), options.getMinRGCoverage());
		options = new Options(new String[] {"-minRGCoverage", "12345"});
		assertEquals(Optional.of(Integer.valueOf(12345)), options.getMinRGCoverage());
	}
	
	@Test
	public void minCov() throws Exception {
		Options options = new Options(new String[] {});
		assertEquals(Optional.empty(), options.getMinCoverage());
		options = new Options(new String[] {"-minCoverage", "10"});
		assertEquals(Optional.of(Integer.valueOf(10)), options.getMinCoverage());
		options = new Options(new String[] {"-minCoverage", "12345"});
		assertEquals(Optional.of(Integer.valueOf(12345)), options.getMinCoverage());
	}
	@Test
	public void minBQ() throws Exception {
		Options options = new Options(new String[] {});
		assertEquals(Optional.empty(), options.getMinBaseQuality());
		options = new Options(new String[] {"-minBaseQuality", "10"});
		assertEquals(Optional.of(Integer.valueOf(10)), options.getMinBaseQuality());
		options = new Options(new String[] {"-minBaseQuality", "12345"});
		assertEquals(Optional.of(Integer.valueOf(12345)), options.getMinBaseQuality());
	}
	@Test
	public void minMQ() throws Exception {
		Options options = new Options(new String[] {});
		assertEquals(Optional.empty(), options.getMinMappingQuality());
		options = new Options(new String[] {"-minMappingQuality", "10"});
		assertEquals(Optional.of(Integer.valueOf(10)), options.getMinMappingQuality());
		options = new Options(new String[] {"-minMappingQuality", "12345"});
		assertEquals(Optional.of(Integer.valueOf(12345)), options.getMinMappingQuality());
	}

}
