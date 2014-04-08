package org.qcmg.motif.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qcmg.motif.Motifs;

public class MotifsAndRegexesTest {
	
	@Test(expected=IllegalArgumentException.class)
	public void nullArguments() {
		new MotifsAndRegexes(null, null, null, null, -1);
	}
	@Test(expected=IllegalArgumentException.class)
	public void nullStage1Arguments() {
		new MotifsAndRegexes(null, null, null, "", -1);
	}
	@Test(expected=IllegalArgumentException.class)
	public void nullStage2Arguments() {
		new MotifsAndRegexes(null, "", null, null, -1);
	}
	@Test(expected=IllegalArgumentException.class)
	public void nullStage1MotifArguments() {
		new MotifsAndRegexes(null, null, new Motifs(true, "blah"), null, -1);
	}
	@Test(expected=IllegalArgumentException.class)
	public void nullStage2MotifArguments() {
		new MotifsAndRegexes(new Motifs(true, "blah"), null, null, null, -1);
	}
	@Test
	public void negativeWindowSize() {
		MotifsAndRegexes mAndR = new MotifsAndRegexes(null, "", null, "", -1);
		assertEquals(10000, mAndR.getWindowSize());
	}
	@Test
	public void positiveWindowSize() {
		MotifsAndRegexes mAndR = new MotifsAndRegexes(null, "", null, "", 1);
		assertEquals(1, mAndR.getWindowSize());
	}
	
	@Test
	public void stringStringMode() {
		MotifsAndRegexes mAndR = new MotifsAndRegexes(new Motifs(true, "blah"), null, new Motifs(true, "blah"), null, 10);
		assertEquals(MotifMode.STRING_STRING, mAndR.getMotifMode());
	}
	@Test
	public void regexRegexMode() {
		MotifsAndRegexes mAndR = new MotifsAndRegexes(null, "", null, "",10);
		assertEquals(MotifMode.REGEX_REGEX, mAndR.getMotifMode());
	}
	@Test
	public void regexStringMode() {
		MotifsAndRegexes mAndR = new MotifsAndRegexes(null, "", new Motifs(false, "hello"), null,10);
		assertEquals(MotifMode.REGEX_STRING, mAndR.getMotifMode());
	}
	@Test
	public void stringRegexMode() {
		MotifsAndRegexes mAndR = new MotifsAndRegexes(new Motifs(false, "the cat sat on a mat"), null, null,"",10);
		assertEquals(MotifMode.STRING_REGEX, mAndR.getMotifMode());
	}

}
