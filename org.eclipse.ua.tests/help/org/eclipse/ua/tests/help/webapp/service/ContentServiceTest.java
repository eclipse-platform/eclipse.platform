/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.webapp.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.eclipse.ua.tests.help.remote.ContentServletTest;
import org.junit.Test;

public class ContentServiceTest extends ContentServletTest {

	private static final String UA_TESTS = "org.eclipse.ua.tests";

	@Override
	@Test
	public void testSimpleContent() throws Exception {
		final String path = "/data/help/index/topic1.html";
		String remoteContent = ServicesTestUtils.getRemoteContent(UA_TESTS, path, "en");
		String localContent = ServicesTestUtils.getLocalContent(UA_TESTS, path);
		assertEquals(remoteContent, localContent);
	}

	@Override
	@Test
	public void testFilteredContent() throws Exception {
		final String path = "/data/help/manual/filter.xhtml";
		String remoteContent = ServicesTestUtils.getRemoteContent(UA_TESTS, path, "en");
		String localContent = ServicesTestUtils.getLocalContent(UA_TESTS, path);
		assertEquals(remoteContent, localContent);
	}

	@Override
	@Test
	public void testContentInEnLocale() throws Exception {
		final String path = "/data/help/search/testnl1.xhtml";
		String remoteContent = ServicesTestUtils.getRemoteContent(UA_TESTS, path, "en");
		String localContent = ServicesTestUtils.getLocalContent(UA_TESTS, path);
		assertEquals(remoteContent, localContent);
	}

	@Override
	@Test
	public void testContentInDeLocale() throws Exception {
		final String path = "/data/help/search/testnl1.xhtml";
		String remoteContent = ServicesTestUtils.getRemoteContent(UA_TESTS, path, "de");
		String enLocalContent = ServicesTestUtils.getLocalContent(UA_TESTS, path);
		String deLocalContent = ServicesTestUtils.getLocalContent(UA_TESTS, "/nl/de" + path);
		assertEquals(remoteContent, deLocalContent);
		assertFalse(remoteContent.equals(enLocalContent));
	}

	@Override
	@Test(expected = IOException.class)
	public void testRemoteContentNotFound() throws Exception {
		ServicesTestUtils.getRemoteContent(UA_TESTS, "/no/such/path.html", "en");
	}

}
