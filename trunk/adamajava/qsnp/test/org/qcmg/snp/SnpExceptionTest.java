package org.qcmg.snp;

import java.text.MessageFormat;

import org.junit.Test;

public class SnpExceptionTest {
	
	@Test
	public void testSnpException() {
		String message = Messages.getMessage("INPUT_FILE_ERROR");
		System.out.println(message);
		
		System.out.println(MessageFormat.format("{0} World", new Object[] {"Hello"}));
		String message1 = "{0} World2";
		System.out.println(MessageFormat.format(message1, new Object[] {"Hello"}));
		message1 = "{0} World2 {1}";
		System.out.println(MessageFormat.format(message1, new Object[] {"Hello", "there"}));
		String message2 = "Why  won't work now??? {0}";
		System.out.println(MessageFormat.format(message2, new Object[] {"Hello"}));
		
		SnpException exception = new SnpException("INPUT_FILE_ERROR", new Object[] {"Hello"});
		System.out.println("exception: " + exception.getMessage());
		
		MessageFormat form = new MessageFormat("Why doesn't this work {0} ??");
		System.out.println(form.format(new Object[] {"blah"}));
		
	}

}
