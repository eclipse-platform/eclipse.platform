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

package org.eclipse.ua.tests.help.webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.junit.Test;

/**
 * Test for methods in UrlUtils
 */

public class UrlUtilsTests {
	@Test
	public void testHelpNav() {
		assertTrue(UrlUtil.isNavPath("/help/nav/1_2"));
	}

	@Test
	public void testOtherNav() {
		assertTrue(UrlUtil.isNavPath("/other/nav/1_2"));
	}

	@Test
	public void testHelpHelpNav() {
		assertFalse(UrlUtil.isNavPath("/help/help/nav/1_2"));
	}

	@Test
	public void testNoNav() {
		assertFalse(UrlUtil.isNavPath("/helpcontext"));
	}

	@Test
	public void testNoSlash() {
		assertFalse(UrlUtil.isNavPath("help/nav/1_2"));
	}

	@Test
	public void testNavTopicPath() {
		int[] path = UrlUtil.getTopicPath("/nav/23_4_5", "en_us");
		assertEquals(3, path.length);
		assertEquals(23, path[0]);
		assertEquals(4, path[1]);
		assertEquals(5, path[2]);
	}

	@Test
	public void testRelativePathUnrelated() {
		MockServletRequest req = new MockServletRequest();
		req.setPathInfo("/advanced/index.jsp");
		assertEquals("../basic/index.jsp", UrlUtil.getRelativePath(req, "/basic/index.jsp"));
	}

	@Test
	public void testRelativePathSameStart() {
		MockServletRequest req = new MockServletRequest();
		req.setPathInfo("/advanced/index.jsp");
		assertEquals("test.jsp", UrlUtil.getRelativePath(req, "/advanced/test.jsp"));
	}

	@Test
	public void testRelativePathSameFile() {
		MockServletRequest req = new MockServletRequest();
		req.setPathInfo("/advanced/index.jsp");
		assertEquals("index.jsp", UrlUtil.getRelativePath(req, "/advanced/index.jsp"));
	}

	@Test
	public void testRelativeAlmostMatch1() {
		MockServletRequest req = new MockServletRequest();
		req.setPathInfo("/advanced/index.jsp");
		assertEquals("../advance/index.jsp", UrlUtil.getRelativePath(req, "/advance/index.jsp"));
	}

	@Test
	public void testRelativeAlmostMatch2() {
		MockServletRequest req = new MockServletRequest();
		req.setPathInfo("/advanced/index.jsp");
		assertEquals("../advancedd/index.jsp", UrlUtil.getRelativePath(req, "/advancedd/index.jsp"));
	}

	@Test
	public void testRelativePathMultipleMatchingSegments() {
		MockServletRequest req = new MockServletRequest();
		req.setPathInfo("/a/b/c/index.jsp");
		assertEquals("../d/test.jsp", UrlUtil.getRelativePath(req, "/a/b/d/test.jsp"));
	}

	@Test
	public void testRelativePathSecondSegmentMatch1() {
		MockServletRequest req = new MockServletRequest();
		req.setPathInfo("/a/a/a/index.jsp");
		assertEquals("../../../b/a/c/test.jsp", UrlUtil.getRelativePath(req, "/b/a/c/test.jsp"));
	}

	@Test
	public void testRelativePathSecondSegmentMatch2() {
		MockServletRequest req = new MockServletRequest();
		req.setPathInfo("/b/a/c/index.jsp");
		assertEquals("../../../a/a/a/test.jsp", UrlUtil.getRelativePath(req, "/a/a/a/test.jsp"));
	}

	@Test
	public void testGetHelpUrlNull() {
		assertEquals("about:blank", UrlUtil.getHelpURL(null));
	}

	@Test
	public void testGetHelpUrlHttp() {
		assertEquals("http://www.eclipse.org", UrlUtil.getHelpURL("http://www.eclipse.org"));
	}

	@Test
	public void testGetHelpUrlHttps() {
		assertEquals("https://bugs.eclipse.org/bugs/", UrlUtil.getHelpURL("https://bugs.eclipse.org/bugs/"));
	}

	@Test
	public void testGetHelpUrlFile() {
		assertEquals("../topic/file://etc/about.html", UrlUtil.getHelpURL("file://etc/about.html"));
	}

	@Test
	public void testGetHelpUrlPageInBundle() {
		assertEquals("../topic/bundle/help.html", UrlUtil.getHelpURL("/bundle/help.html"));
	}

	@Test
	public void testGetHelpUrlNullDepthTwo() {
		assertEquals("about:blank", UrlUtil.getHelpURL(null, 2));
	}

	@Test
	public void testGetHelpUrlHttpDepthTwo() {
		assertEquals("http://www.eclipse.org", UrlUtil.getHelpURL("http://www.eclipse.org", 2));
	}

	@Test
	public void testGetHelpUrlHttpDepthTwos() {
		assertEquals("https://bugs.eclipse.org/bugs/", UrlUtil.getHelpURL("https://bugs.eclipse.org/bugs/", 2));
	}

	@Test
	public void testGetHelpUrlFileDepthTwo() {
		assertEquals("../../topic/file://etc/about.html", UrlUtil.getHelpURL("file://etc/about.html", 2));
	}

	@Test
	public void testGetHelpUrlPageInBundleDepthTwo() {
		assertEquals("../../topic/bundle/help.html", UrlUtil.getHelpURL("/bundle/help.html", 2));
	}


}
