package org.qcmg.common.model;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ProfileType;

public class ProfileTypeTest {
	
	@Test
	public void fastqs() throws Exception {
		assertEquals( ProfileType.FASTQ, ProfileType.getType1(".fq.gz"));
		assertEquals( ProfileType.FASTQ, ProfileType.getType1("blah.fq"));
		assertEquals( ProfileType.FASTQ, ProfileType.getType1("blah.fastq.gz"));
		assertEquals( ProfileType.FASTQ, ProfileType.getType1("blah.fastq"));
		try {
			ProfileType.getType1(("blah.faq.gz"));
			Assert.fail("should have barfed");
		} catch (IllegalArgumentException e) {}
	}
	@Test
	public void gffs() throws Exception {
		assertEquals( ProfileType.GFF, ProfileType.getType1(("blah.gff")));
		assertEquals( ProfileType.GFF, ProfileType.getType1(("blah.gff3")));
		try {
			ProfileType.getType1(("blah.gff2"));
			Assert.fail("should have barfed");
		} catch (IllegalArgumentException e) {}
		
		//qprofiler2 don't support gff
		try {
			ProfileType.getType2(("blah.gff"));
			Assert.fail("should have barfed");
		} catch (IllegalArgumentException e) {}
		
	}
	@Test
	public void bams() throws Exception {
		assertEquals( ProfileType.BAM, ProfileType.getType2(("blah.sam")) );
		assertEquals( ProfileType.BAM, ProfileType.getType1(("blah.bam")) );
		try {
			ProfileType.getType1(("blah.bam123"));
			Assert.fail("should have barfed");
		} catch ( IllegalArgumentException e ) {    }
		
		
		
	}
	@Test
	public void vcfs() throws Exception {
		assertEquals( ProfileType.VCF, ProfileType.getType2("vcf.gz"));
		assertEquals( ProfileType.VCF, ProfileType.getType2("ok.vcf"));
		assertEquals( ProfileType.VCF, ProfileType.getType2("blah.vcf.gz"));
		 
		try {
			ProfileType.getType1(("blah.faq.gz"));
			Assert.fail("should have vcf");
		} catch (IllegalArgumentException e) {}
	}
}
