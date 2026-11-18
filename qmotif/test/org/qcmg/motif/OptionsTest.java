package org.qcmg.motif;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class OptionsTest {

	@Test
	public void inputBaiIsRead() {
		Options options = new Options(new String[] {"--input-bam", "a.bam", "--input-bai", "a.bam.bai"});
		assertArrayEquals(new String[] {"a.bam"}, options.getBAMFileNames());
		assertArrayEquals(new String[] {"a.bam.bai"}, options.getBAIFileNames());
	}

	@Test
	public void noInputBai() {
		Options options = new Options(new String[] {"--input-bam", "a.bam"});
		assertArrayEquals(new String[0], options.getBAIFileNames());
	}
}
