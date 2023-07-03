/*******************************************************************************
 *  Copyright (c) 2007, 2016 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.help.internal.search.HTMLDocParser;
import org.eclipse.help.internal.webapp.servlet.FilterHTMLHeadAndBodyOutputStream;
import org.eclipse.help.internal.webapp.servlet.FilterHTMLHeadOutputStream;
import org.junit.Test;

/**
 * Test for functions which decode a topic string
 */

public class FilterTest {
	private static final String HTML40 =  "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">";
	private static final String HEAD1 =  "<HEAD>";
	private static final String HEAD2 = "</HEAD>";
	private static final String HEADLC1 =  "<head>";
	private static final String HEADLC2 = "</head>";
	private static final String CONTENT_TYPE_ISO_8859_1 =    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">";
	private static final String CONTENT_TYPE_ISO_8859_1_UC = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />";
	private static final String CONTENT_TYPE_UTF8 = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">";
	private static final String CONTENT_TYPE_UTF8UC = "<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">";
	private static final String BODY1 = "<BODY>";
	private static final String BODY2 = "</BODY></html>";
	private static final String BODYLC1 = "<body>";
	private static final String BODYLC2 = "</body></html>";
	private static final String CSS1 = "<LINK REL=\"STYLESHEET\" HREF=\"book1.css\" TYPE=\"text/css\">";
	private static final String CSS2 = "<LINK REL=\"STYLESHEET\" HREF=\"book1.css\" TYPE=\"text/css\">";
	private static final String CONTENT1 = "<p>Content1</p>";
	private static final String CONTENT2 = "<p>Content2</p>";
	private String CHINESE_CONTENT = "<p>" + (char)24320 + (char)21457 + (char)29932 + "</p>";
	private String CHINESE_ENTITY_CONTENT = "<p>&#24320;&#21457;&#29932;</p>";

	@Test
	public void testHeadOutputFilter() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (OutputStream filteredOutput = new FilterHTMLHeadOutputStream(output, CSS2.getBytes())) {
			filteredOutput.write(HTML40.getBytes());
			filteredOutput.write(HEAD1.getBytes());
			filteredOutput.write(CSS1.getBytes());
			filteredOutput.write(HEAD2.getBytes());
			filteredOutput.write(BODY1.getBytes());
			filteredOutput.write(CONTENT1.getBytes());
			filteredOutput.write(BODY2.getBytes());
			filteredOutput.close();
		} catch (IOException e) {
			fail("IO Exception");
		}
		final String expected = HTML40 + HEAD1 + CSS1 + CSS2 + '\n' + HEAD2 + BODY1 + CONTENT1 + BODY2;
		assertEquals(expected, output.toString());
	}

	@Test
	public void testHeadAndBodyOutputFilter() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (FilterHTMLHeadAndBodyOutputStream filteredOutput = new FilterHTMLHeadAndBodyOutputStream(output,
				CSS2.getBytes(), CONTENT2)) {
			filteredOutput.write(HTML40.getBytes());
			filteredOutput.write(HEAD1.getBytes());
			filteredOutput.write(CONTENT_TYPE_ISO_8859_1.getBytes());
			filteredOutput.write(CSS1.getBytes());
			filteredOutput.write(HEAD2.getBytes());
			filteredOutput.write(BODY1.getBytes());
			filteredOutput.write(CONTENT1.getBytes());
			filteredOutput.write(BODY2.getBytes());
			filteredOutput.close();
		} catch (IOException e) {
			fail("IO Exception");
		}
		final String expected = HTML40 + HEAD1 + CONTENT_TYPE_ISO_8859_1 + CSS1 + CSS2 + '\n' + HEAD2 + BODY1 + '\n' + CONTENT2 + '\n' + CONTENT1 + BODY2;
		assertEquals(expected, output.toString());
	}

	@Test
	public void testLowerCaseTags() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (OutputStream filteredOutput = new FilterHTMLHeadAndBodyOutputStream(output, CSS1.getBytes(), CONTENT2)) {
			filteredOutput.write(HTML40.getBytes());
			filteredOutput.write(HEADLC1.getBytes());
			filteredOutput.write(HEADLC2.getBytes());
			filteredOutput.write(BODYLC1.getBytes());
			filteredOutput.write(CONTENT1.getBytes());
			filteredOutput.write(BODYLC2.getBytes());
			filteredOutput.close();
		} catch (IOException e) {
			fail("IO Exception");
		}
		final String expected = HTML40 + HEADLC1 + CSS1 + '\n' + HEADLC2 + BODYLC1 + '\n' + CONTENT2 + '\n' + CONTENT1 + BODYLC2;
		assertEquals(expected, output.toString());
	}

	@Test
	public void testFilterHeadlessDocument() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (OutputStream filteredOutput = new FilterHTMLHeadAndBodyOutputStream(output, CSS1.getBytes(), CONTENT2)) {
			filteredOutput.write(HTML40.getBytes());
			filteredOutput.write(BODY1.getBytes());
			filteredOutput.write(CONTENT1.getBytes());
			filteredOutput.write(BODY2.getBytes());
			filteredOutput.close();
		} catch (IOException e) {
			fail("IO Exception");
		}
		final String expected = HTML40 + BODY1 + '\n' + CONTENT2 + '\n' + CONTENT1 + BODY2;
		assertEquals(expected, output.toString());
	}

	@Test
	public void testInsertChineseUtf8() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (FilterHTMLHeadAndBodyOutputStream filteredOutput = new FilterHTMLHeadAndBodyOutputStream(output, null,
				CHINESE_CONTENT)) {
			filteredOutput.write(HTML40.getBytes());
			filteredOutput.write(HEAD1.getBytes());
			filteredOutput.write(CONTENT_TYPE_UTF8.getBytes());
			filteredOutput.write(HEAD2.getBytes());
			filteredOutput.write(BODY1.getBytes());
			filteredOutput.write(BODY2.getBytes());
			filteredOutput.close();
			final String expected = HTML40 + HEAD1 + CONTENT_TYPE_UTF8 + HEAD2 + BODY1 + '\n' + CHINESE_CONTENT + '\n' + BODY2;
			assertEquals(expected, output.toString("UTF-8"));
		} catch (IOException e) {
			fail("IO Exception");
		}
	}

	@Test
	public void testInsertChineseISO8859() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (FilterHTMLHeadAndBodyOutputStream filteredOutput = new FilterHTMLHeadAndBodyOutputStream(output, null,
				CHINESE_CONTENT)) {
			filteredOutput.write(HTML40.getBytes());
			filteredOutput.write(HEAD1.getBytes());
			filteredOutput.write(CONTENT_TYPE_ISO_8859_1.getBytes());
			filteredOutput.write(HEAD2.getBytes());
			filteredOutput.write(BODY1.getBytes());
			filteredOutput.write(BODY2.getBytes());
			filteredOutput.close();
			final String expected = HTML40 + HEAD1 + CONTENT_TYPE_ISO_8859_1 + HEAD2 + BODY1 + '\n' + CHINESE_ENTITY_CONTENT + '\n' + BODY2;
			assertEquals(expected, output.toString());
		} catch (IOException e) {
			fail("IO Exception");
		}
	}

	@Test
	public void testInsertChineseNoCharsetSpecified() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (FilterHTMLHeadAndBodyOutputStream filteredOutput = new FilterHTMLHeadAndBodyOutputStream(output, null,
				CHINESE_CONTENT)) {
			filteredOutput.write(HTML40.getBytes());
			filteredOutput.write(HEAD1.getBytes());
			filteredOutput.write(HEAD2.getBytes());
			filteredOutput.write(BODY1.getBytes());
			filteredOutput.write(BODY2.getBytes());
			filteredOutput.close();
			final String expected = HTML40 + HEAD1 + HEAD2 + BODY1 + '\n' + CHINESE_ENTITY_CONTENT + '\n' + BODY2;
			assertEquals(expected, output.toString());
		} catch (IOException e) {
			fail("IO Exception");
		}
	}

	@Test
	public void testCharsetUtf8Upper() {
		InputStream is = new ByteArrayInputStream(CONTENT_TYPE_UTF8UC.getBytes());
		assertEquals("UTF-8", HTMLDocParser.getCharsetFromHTML(is));
	}

	@Test
	public void testCharsetISO_8859_UCUpper() {
		InputStream is = new ByteArrayInputStream(CONTENT_TYPE_ISO_8859_1_UC.getBytes());
		assertEquals("ISO-8859-1", HTMLDocParser.getCharsetFromHTML(is));
	}

}
