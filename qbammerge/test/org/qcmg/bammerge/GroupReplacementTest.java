package org.qcmg.bammerge;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class GroupReplacementTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public final void before() {
	}

	@After
	public final void after() {
	}

	@Test
	public final void constructWithValidArguments() throws Exception {
            ExpectedException.none();

            String cmdOptionValue = "someFileName:OLDG:NEWG";
            GroupReplacement gr = new GroupReplacement(cmdOptionValue);

            assertTrue(0 == gr.getRawForm().compareTo(cmdOptionValue));
            assertTrue(0 == gr.getOldGroup().compareTo("OLDG"));
            assertTrue(0 == gr.getNewGroup().compareTo("NEWG"));
            assertTrue(0 == gr.getFileName().compareTo("someFileName"));
	}

	@Test
	public final void constructWithIncompleteValue1() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("someFileName:OLDG: does not follow pattern file:oldgroup:newgroup");

            String cmdOptionValue = "someFileName:OLDG:";
            new GroupReplacement(cmdOptionValue);
	}

	@Test
	public final void constructWithIncompleteValue2() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("someFileName::NEWG does not follow pattern file:oldgroup:newgroup");

            String cmdOptionValue = "someFileName::NEWG";
            new GroupReplacement(cmdOptionValue);
	}

	@Test
	public final void constructWithIncompleteValue3() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("someFileName:: does not follow pattern file:oldgroup:newgroup");

            String cmdOptionValue = "someFileName::";
            new GroupReplacement(cmdOptionValue);
	}

	@Test
	public final void constructWithIncompleteValue4() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("someFileName: does not follow pattern file:oldgroup:newgroup");

            String cmdOptionValue = "someFileName:";
            new GroupReplacement(cmdOptionValue);
	}

	@Test
	public final void constructWithIncompleteValue5() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("someFileName does not follow pattern file:oldgroup:newgroup");

            String cmdOptionValue = "someFileName";
            new GroupReplacement(cmdOptionValue);
	}

	@Test
	public final void constructWithIncompleteValue6() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("someFileName::: does not follow pattern file:oldgroup:newgroup");

            String cmdOptionValue = "someFileName:::";
            new GroupReplacement(cmdOptionValue);
	}

	@Test
	public final void constructWithIncompleteValue7() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("someFileName:::: does not follow pattern file:oldgroup:newgroup");

            String cmdOptionValue = "someFileName::::";
            new GroupReplacement(cmdOptionValue);
	}

	@Test
	public final void constructWithIncompleteValue8() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("Blank file name in replacement :OLDG:NEWG");

            String cmdOptionValue = ":OLDG:NEWG";
            new GroupReplacement(cmdOptionValue);
	}

	@Test
	public final void constructWithEmptyValue() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("Cannot use an empty value for a group replacement");

            String cmdOptionValue = "";
            new GroupReplacement(cmdOptionValue);
	}

	@Test
	public final void constructWithNullValue() throws Exception {
            thrown.expect(Exception.class);
            thrown.expectMessage("Cannot use a null value for a group replacement");

            String cmdOptionValue = null;
            new GroupReplacement(cmdOptionValue);
	}
}
