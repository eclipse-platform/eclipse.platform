/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XHTMLEntityTest {
	private static final String XHTML1 = """
			<?xml version="1.0" encoding="ISO-8859-1"?>
			<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
			<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
			<head></head><body>""";
	private static final String XHTML2 = "</body></html>";

	public void checkResolution(String text, int expected) throws Exception {
		String xmlSource = XHTML1 + text + XHTML2;
		Document doc = LocalEntityResolver.parse(xmlSource);
		assertEquals(2, doc.getChildNodes().getLength());
		Node bodyNode = doc.getElementsByTagName("body").item(0);
		Node textNode = bodyNode.getChildNodes().item(0);
		assertNotNull(textNode);
		String value = textNode.getNodeValue();
		assertEquals(1, value.length());
		assertEquals(expected, value.charAt(0));
	}

	@Test
	public void testResolveAmpersand() throws Exception {
		checkResolution("&amp;", 38);
	}

	@Test
	public void testResolveNonBlockingSpace() throws Exception {
		checkResolution("&nbsp;", 160);
	}

	@Test
	public void testResolveGt() throws Exception {
		checkResolution("&gt;", 62);
	}

	@Test
	public void testResolveLeftArrow() throws Exception {
		checkResolution("&larr;", 8592);
	}

	@Test
	public void testResolveaacute() throws Exception {
		checkResolution("&aacute;", 225);
	}

	@Test
	public void testResolveCopy() throws Exception {
		checkResolution("&copy;", 169);
	}

	@Test
	public void testResolveTilde() throws Exception {
		checkResolution("&tilde;", 732);
	}

	@Test
	public void testResolveAacute() throws Exception {
		checkResolution("&Aacute;", 193);
	}

	@Test
	public void testResolveAlpha() throws Exception {
		checkResolution("&Alpha;", 913);
	}

}
