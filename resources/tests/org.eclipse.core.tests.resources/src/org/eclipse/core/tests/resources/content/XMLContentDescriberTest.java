/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.*;
import java.util.Arrays;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.junit.Test;

/**
 * Ensures the XMLContentDescriber is able to handle encodings properly.
 */
public class XMLContentDescriberTest extends ContentTypeTest {
	private final static String ENCODED_TEXT = "\u1000\u20001\u3000\u4000\u5000\u6000\u7000\u8000\u9000\uA000";
	private final static String ENCODING_UTF16 = "UTF-16";
	private final static String ENCODING_UTF8 = "UTF-8";
	private final static String ENCODING_NOTSUPPORTED = "ENCODING_NOTSUPPORTED";
	private final static String ENCODING_INCORRECT = "<?ENCODING?>";
	private final static String ENCODING_EMPTY = "";
	private final static String WHITESPACE_CHARACTERS = " 	\n\r";

	@Test
	public void testEncodedContents1() throws Exception {
		checkEncodedContents(ENCODING_UTF16, ENCODING_UTF16, ENCODING_UTF16);
		checkEncodedContents(ENCODING_UTF8, ENCODING_UTF8, ENCODING_UTF8);

		checkEncodedContents(ENCODING_UTF16, ENCODING_UTF16, ENCODING_UTF8);
		checkEncodedContents(ENCODING_UTF8, ENCODING_UTF8, ENCODING_UTF16);

		checkEncodedContents(ENCODING_NOTSUPPORTED, ENCODING_NOTSUPPORTED, ENCODING_UTF8);
		checkEncodedContents(ENCODING_NOTSUPPORTED, ENCODING_NOTSUPPORTED, ENCODING_UTF16);
	}

	@Test
	public void testEncodedContents2() throws Exception {
		checkEncodedContents2(ENCODING_UTF16, ENCODING_UTF16);
		checkEncodedContents2(ENCODING_UTF8, ENCODING_UTF8);
		checkEncodedContents2(ENCODING_NOTSUPPORTED, ENCODING_NOTSUPPORTED);
	}

	@Test
	public void testEncodedContents3() throws Exception {
		boolean[][] flags = { {true, true, false}, {true, false, false}, {false, true, false}, {false, false, false}, {true, true, true}, {true, false, true}, {false, true, true}, {false, false, true}};

		IContentDescription description = null;
		for (boolean[] flag : flags) {
			description = Platform.getContentTypeManager().getDescriptionFor(getInputStream(ENCODING_INCORRECT, ENCODING_UTF8, flag[0], flag[1], flag[2]), "fake.xml", new QualifiedName[] {IContentDescription.CHARSET});
			assertNull("1.0", description);

			description = Platform.getContentTypeManager().getDescriptionFor(getInputStream(ENCODING_INCORRECT, ENCODING_UTF16, flag[0], flag[1], flag[2]), "fake.xml", new QualifiedName[] {IContentDescription.CHARSET});
			assertNull("2.0", description);

			description = Platform.getContentTypeManager().getDescriptionFor(getReader(ENCODING_INCORRECT, flag[0], flag[1], flag[2]), "fake.xml", new QualifiedName[] {IContentDescription.CHARSET});
			assertNull("3.0", description);
		}

		for (boolean[] flag : flags) {
			description = Platform.getContentTypeManager().getDescriptionFor(getInputStream(ENCODING_EMPTY, ENCODING_UTF8, flag[0], flag[1], flag[2]), "fake.xml", new QualifiedName[] {IContentDescription.CHARSET});
			assertNull("1.0", description);

			description = Platform.getContentTypeManager().getDescriptionFor(getInputStream(ENCODING_EMPTY, ENCODING_UTF16, flag[0], flag[1], flag[2]), "fake.xml", new QualifiedName[] {IContentDescription.CHARSET});
			assertNull("2.0", description);

			description = Platform.getContentTypeManager().getDescriptionFor(getReader(ENCODING_EMPTY, flag[0], flag[1], flag[2]), "fake.xml", new QualifiedName[] {IContentDescription.CHARSET});
			assertNull("3.0", description);
		}
	}

	@Test
	public void testBug258208() throws Exception {
		IContentDescription description = Platform.getContentTypeManager().getDescriptionFor(getInputStream(ENCODING_EMPTY, ENCODING_UTF8, false, true, false), "fake.xml", new QualifiedName[] {IContentDescription.CHARSET});
		assertNull("1.0", description);

		// empty charset should not disable the xml content type
		checkEncodedContents(ENCODING_UTF16, ENCODING_UTF16, ENCODING_UTF16);
	}

	private void checkEncodedContents(String expectedEncoding, String encodingInContent, String encoding) throws Exception {
		boolean[][] flags = { {true, true, false}, {true, false, false}, {false, true, false}, {false, false, false}, {true, true, true}, {true, false, true}, {false, true, true}, {false, false, true}};

		IContentDescription description = null;
		for (boolean[] flag : flags) {
			description = Platform.getContentTypeManager().getDescriptionFor(getInputStream(encodingInContent, encoding, flag[0], flag[1], flag[2]), "fake.xml", new QualifiedName[] {IContentDescription.CHARSET});
			assertNotNull("1.0: " + flag[0] + " " + flag[1] + " " + flag[2], description);
			assertEquals("1.1: " + flag[0] + " " + flag[1] + " " + flag[2], Platform.PI_RUNTIME + ".xml", description.getContentType().getId());
			assertEquals("1.2: " + flag[0] + " " + flag[1] + " " + flag[2], expectedEncoding, description.getProperty(IContentDescription.CHARSET));
		}
	}

	private void checkEncodedContents2(String expectedEncoding, String encodingInContent) throws Exception {
		boolean[][] flags = { {true, true, false}, {true, false, false}, {false, true, false}, {false, false, false}, {true, true, true}, {true, false, true}, {false, true, true}, {false, false, true}};

		IContentDescription description = null;
		for (boolean[] flag : flags) {
			description = Platform.getContentTypeManager().getDescriptionFor(getReader(encodingInContent, flag[0], flag[1], flag[2]), "fake.xml", new QualifiedName[] {IContentDescription.CHARSET});
			assertNotNull("1.0: " + flag[0] + " " + flag[1] + " " + flag[2], description);
			assertEquals("1.1: " + Arrays.toString(flag), Platform.PI_RUNTIME + ".xml", description.getContentType().getId());
			assertEquals("1.2: " + Arrays.toString(flag), expectedEncoding, description.getProperty(IContentDescription.CHARSET));
		}
	}

	private InputStream getInputStream(String encodingInContent, String encoding, boolean encInNewLine, boolean encClosed, boolean whitespace) throws UnsupportedEncodingException {
		String content = "<?xml version=\"1.0\"" + (encInNewLine ? "\n" : "") + "encoding" + (whitespace ? WHITESPACE_CHARACTERS : "") + "=" + (whitespace ? WHITESPACE_CHARACTERS : "") + "\"" + encodingInContent + (encClosed ? "\"" : "") + "?><root attribute=\"" + ENCODED_TEXT + "\">";
		return new ByteArrayInputStream(content.getBytes(encoding));
	}

	private Reader getReader(String encodingInContent, boolean encInNewLine, boolean encClosed, boolean whitespace) {
		String content = "<?xml version=\"1.0\"" + (encInNewLine ? "\n" : "") + "encoding" + (whitespace ? WHITESPACE_CHARACTERS : "") + "=" + (whitespace ? WHITESPACE_CHARACTERS : "") + "\"" + encodingInContent + (encClosed ? "\"" : "") + "?><root attribute=\"" + ENCODED_TEXT + "\">";
		return new InputStreamReader(new ByteArrayInputStream(content.getBytes()));
	}
}
