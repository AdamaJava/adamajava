package org.qcmg.common.util;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ProfileType;

public class ProfileTypeUtilsTest {
	
	@Test
	public void fastqs() throws Exception {
		assertEquals( ProfileType.FASTQ, ProfileTypeUtils.getType(".fq.gz"));
		assertEquals( ProfileType.FASTQ, ProfileTypeUtils.getType("blah.fq"));
		assertEquals( ProfileType.FASTQ, ProfileTypeUtils.getType("blah.fastq.gz"));
		assertEquals( ProfileType.FASTQ, ProfileTypeUtils.getType("blah.fastq"));
		try {
			ProfileTypeUtils.getType(("blah.faq.gz"));
			Assert.fail("should have barfed");
		} catch (IllegalArgumentException e) {}
	}
	@Test
	public void gffs() throws Exception {
		assertEquals( ProfileType.gff, ProfileTypeUtils.getType(("blah.gff")));
		assertEquals( ProfileType.gff, ProfileTypeUtils.getType(("blah.gff3")));
		try {
			ProfileTypeUtils.getType(("blah.gff2"));
			Assert.fail("should have barfed");
		} catch (IllegalArgumentException e) {}
	}
	@Test
	public void bams() throws Exception {
		assertEquals( ProfileType.bam, ProfileTypeUtils.getType(("blah.sam")) );
		assertEquals( ProfileType.bam, ProfileTypeUtils.getType(("blah.bam")) );
		try {
			ProfileTypeUtils.getType(("blah.bam123"));
			Assert.fail("should have barfed");
		} catch ( IllegalArgumentException e ) {    }
	}

}
