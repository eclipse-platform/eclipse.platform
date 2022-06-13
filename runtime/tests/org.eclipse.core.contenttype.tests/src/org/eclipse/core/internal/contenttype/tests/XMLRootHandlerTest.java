/*******************************************************************************
 * Copyright (c) 2020, 2021 Alex Blewitt and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alex Blewitt - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.contenttype.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.eclipse.core.internal.content.XMLRootHandler;

public class XMLRootHandlerTest {

	private XMLRootHandler handler;

	@Before
	public void setUp() {
		this.handler = new XMLRootHandler(true);
	}

	@After
	public void tearDown() {
		this.handler = null;
	}

	@Test
	public void testParse() throws IOException, ParserConfigurationException, SAXException {
		InputSource contents = new InputSource(new StringReader("<xml/>")); //$NON-NLS-1$

		assertTrue(handler.parseContents(contents));
	}
}
