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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContentServletTest {

	private static final String UA_TESTS = "org.eclipse.ua.tests";
	private int mode;

	@BeforeEach
	public void setUp() throws Exception {
		BaseHelpSystem.ensureWebappRunning();
		mode = BaseHelpSystem.getMode();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
	}

	@AfterEach
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

	@Test
	public void testRemoteContentNotFound() throws Exception {
		assertThrows(IOException.class, () -> RemoteTestUtils.getRemoteContent(UA_TESTS, "/no/such/path.html", "en"));
	}



}
