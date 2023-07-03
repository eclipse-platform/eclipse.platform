package org.eclipse.ua.tests.help.webapp;

import static org.junit.Assert.assertEquals;

import org.eclipse.help.internal.webapp.utils.JSonHelper;
import org.junit.Test;

public class JsonHelperTests {
	@Test
	public void testQuoted() {
		assertEquals("\"test\"", JSonHelper.getQuotes("test"));
		assertEquals("\"test test\"", JSonHelper.getQuotes("test test"));
		assertEquals("\"test\\\"test\"", JSonHelper.getQuotes("test\"test"));
		assertEquals("\"\\\"test test\\\"\"", JSonHelper.getQuotes("\"test test\""));
		assertEquals("\"\\\\\"test test\\\\\"\"", JSonHelper.getQuotes("\\\"test test\\\""));
	}
}
