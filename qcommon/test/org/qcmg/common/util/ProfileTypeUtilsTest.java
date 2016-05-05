package org.qcmg.common.util;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.qcmg.common.model.ProfileType;

public class ProfileTypeUtilsTest {
	
	@Test
	public void fastqs() throws Exception {
		assertEquals(ProfileType.FASTQ, ProfileTypeUtils.getType(new File(".fq.gz")));
		assertEquals(ProfileType.FASTQ, ProfileTypeUtils.getType(new File("blah.fq")));
		assertEquals(ProfileType.FASTQ, ProfileTypeUtils.getType(new File("blah.fastq.gz")));
		assertEquals(ProfileType.FASTQ, ProfileTypeUtils.getType(new File("blah.fastq")));
	}
	@Test
	public void gffs() throws Exception {
		assertEquals(ProfileType.GFF, ProfileTypeUtils.getType(new File("blah.gff")));
		assertEquals(ProfileType.GFF, ProfileTypeUtils.getType(new File("blah.gff3")));
	}

}
