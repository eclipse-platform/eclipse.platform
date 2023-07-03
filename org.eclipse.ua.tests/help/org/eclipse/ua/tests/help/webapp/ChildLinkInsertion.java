/*******************************************************************************
 *  Copyright (c) 2009, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.help.webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.webapp.servlet.PluginsRootResolvingStream;
import org.junit.Test;

/**
 * Test for text matching when inserting links
 */

public class ChildLinkInsertion {

	private class TestableReplacementStream extends PluginsRootResolvingStream {
		public TestableReplacementStream(OutputStream out, HttpServletRequest req, String prefix) {
			super(out, req, prefix);
		}

		@Override
		protected void insertBasedOnKeyword(int index) throws IOException {
			if (index == 0 ) {
				out.write("<LINKS>".getBytes()); //$NON-NLS-1$
			} else if (index == 1) {
				out.write("<STYLE>".getBytes()); //$NON-NLS-1$
			} else {
				out.write("<UNKNOWN>".getBytes()); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void testEmpty() {
		final String input = "";
		checkFilter(input, input);
	}

	@Test
	public void testNoMatch() {
		final String input = "<HEAD><HEAD/>";
		checkFilter(input, input);
	}

	@Test
	public void testPartialMatch1() {
		final String input = "<A href = \"PLUGINS\"><!--INSTRUCT-->";
		checkFilter(input, input);
	}

	@Test
	public void testPartialMatch2() {
		final String input = "<A href = \"PLUGINS\"><!A -->";
		checkFilter(input, input);
	}

	@Test
	public void testPartialMatch3() {
		final String input = "<A href = \"PLUGINS\"><!-A -->";
		checkFilter(input, input);
	}

	@Test
	public void testPartialMatch4() {
		final String input = "<A href = \"PLUGINS\"><!--A-->";
		checkFilter(input, input);
	}

	@Test
	public void testEndsUnmatched() {
		final String input = "<A><!--INSTR";
		checkFilter(input, input);
	}

	@Test
	public void testNotAtStart() {
		final String input = "<A><!-- INSERT_CHILD_LINKS-->";
		checkFilter(input, input);
	}

	@Test
	public void testSpaceBeforeEnd() {
		final String input = "<A><!-- INSERT_CHILD_LINKS -->";
		checkFilter(input, input);
	}

	@Test
	public void testTooManyCharacters_1() {
		final String input = "<A><!--INSERT_CHILD_LINKSS-->";
		checkFilter(input, input);
	}

	@Test
	public void testTooManyCharacters_2() {
		final String input = "<A><!--INSERT_CHILD_LINKS_STYLES-->";
		checkFilter(input, input);
	}

	@Test
	public void testAtStart() {
		final String input = "<!--INSERT_CHILD_LINKS--><A>";
		final String expected = "<LINKS><A>";
		checkFilter(input, expected);
	}

	@Test
	public void testChildStyle() {
		final String input = "<!--INSERT_CHILD_LINK_STYLE--><A>";
		final String expected = "<STYLE><A>";
		checkFilter(input, expected);
	}

	@Test
	public void testDefaultEncoding() {
		final String input = "";
		checkEncoding(input, null);
	}

	@Test
	public void testEncodingUtf8() {
		final String input =
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">";
		checkEncoding(input, "utf-8");
	}

	@Test
	public void testMetaNoEncoding() {
		final String input =
			"<meta http-equiv=\"Content-Type\" content=\"text/html\">";
		checkEncoding(input, null);
	}

	@Test
	public void testMultiMeta() {
		final String input =
			"<meta name=\"test\" content=\"test\">" +
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
			"<meta name=\"test\" content=\"test\">";
		checkEncoding(input, "utf-8");
	}

	@Test
	public void testMetaAndInsert() {
		final String metaInfo = "<meta name=\"test\" content=\"test\">" +
					"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">";
		final String input =
			metaInfo + "<!--INSERT_CHILD_LINK_STYLE--><A>";
			final String expected = metaInfo + "<STYLE><A>";
		checkFilter(input, expected);
		checkEncoding(input, "utf-8");
	}

	private void checkFilter(final String input, final String expected) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (OutputStream filteredOutput = new TestableReplacementStream(output, null, "../")) {
			filteredOutput.write(input.getBytes());
		} catch (IOException e) {
			fail("IO Exception");
		}
		assertEquals(expected, output.toString());
	}

	private void checkEncoding(String input, String expectedEncoding) {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				TestableReplacementStream filteredOutput = new TestableReplacementStream(output, null, "../")) {
			filteredOutput.write(input.getBytes());
			if (expectedEncoding == null) {
				assertNull(filteredOutput.getCharset());
			} else {
				assertEquals(expectedEncoding, filteredOutput.getCharset());
			}
		} catch (IOException e) {
			fail("IO Exception");
		}
	}

}
