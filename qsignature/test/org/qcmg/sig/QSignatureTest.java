package org.qcmg.sig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Test;

public class QSignatureTest {

	@Test
	public void everyToolHasAMainMethod() throws Exception {
		for (String tool : QSignature.TOOLS) {
			Method main = QSignature.getMainMethod(tool);
			assertEquals("org.qcmg.sig." + tool, main.getDeclaringClass().getName());
			assertTrue(tool, Modifier.isStatic(main.getModifiers()));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void unknownToolIsRejected() throws Exception {
		QSignature.getMainMethod("NotATool");
	}

	@Test(expected = IllegalArgumentException.class)
	public void helperClassesAreNotTools() throws Exception {
		QSignature.getMainMethod("Messages");
	}
}
