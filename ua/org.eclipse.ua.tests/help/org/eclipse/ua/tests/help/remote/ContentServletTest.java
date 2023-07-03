/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContentServletTest {

	private static final String UA_TESTS = "org.eclipse.ua.tests";
	private int mode;

	@Before
	public void setUp() throws Exception {
		BaseHelpSystem.ensureWebappRunning();
		mode = BaseHelpSystem.getMode();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
	}

	@After
	public void tearDown() throws Exception {
		BaseHelpSystem.setMode(mode);
	}

	@Test
	public void testSimpleContent() throws Exception {
		final String path = "/data/help/index/topic1.html";
		String remoteContent = RemoteTestUtils.getRemoteContent(UA_TESTS, path, "en");
		String localContent = RemoteTestUtils.getLocalContent(UA_TESTS, path);
		assertEquals(remoteContent, localContent);
	}

	@Test
	public void testFilteredContent() throws Exception {
		final String path = "/data/help/manual/filter.xhtml";
		String remoteContent = RemoteTestUtils.getRemoteContent(UA_TESTS, path, "en");
		String localContent = RemoteTestUtils.getLocalContent(UA_TESTS, path);
		assertEquals(remoteContent, localContent);
	}

	@Test
	public void testContentInEnLocale() throws Exception {
		final String path = "/data/help/search/testnl1.xhtml";
		String remoteContent = RemoteTestUtils.getRemoteContent(UA_TESTS, path, "en");
		String localContent = RemoteTestUtils.getLocalContent(UA_TESTS, path);
		assertEquals(remoteContent, localContent);
	}

	@Test
	public void testContentInDeLocale() throws Exception {
		final String path = "/data/help/search/testnl1.xhtml";
		String remoteContent = RemoteTestUtils.getRemoteContent(UA_TESTS, path, "de");
		String enLocalContent = RemoteTestUtils.getLocalContent(UA_TESTS, path);
		String deLocalContent = RemoteTestUtils.getLocalContent(UA_TESTS, "/nl/de" + path);
		assertEquals(remoteContent, deLocalContent);
		assertFalse(remoteContent.equals(enLocalContent));
	}

	@Test(expected = IOException.class)
	public void testRemoteContentNotFound() throws Exception {
		RemoteTestUtils.getRemoteContent(UA_TESTS, "/no/such/path.html", "en");
	}



}
