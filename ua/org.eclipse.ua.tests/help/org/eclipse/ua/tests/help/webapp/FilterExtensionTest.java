/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.help.internal.webapp.servlet.ExtraFilters;
import org.eclipse.help.internal.webapp.servlet.PrioritizedFilter;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for the code which supports the extension point org.eclipse.help.webapp.extraFilter
 */
public class FilterExtensionTest {

	@After
	public void tearDown() throws Exception {
		ExtraFilters.setFilters(new PrioritizedFilter[0]);
	}

	@Test
	public void testFilterExtensions() throws IOException {
		PrioritizedFilter[] filters = new PrioritizedFilter[] {
				new PrioritizedFilter(new CommentFilterTwo(), 2),
				new PrioritizedFilter(new CommentFilterThree(), 3),
				new PrioritizedFilter(new CommentFilterOne(), 1) };
		ExtraFilters.setFilters(filters);
		OutputStream out = new ByteArrayOutputStream(1000);
		MockServletRequest req = new MockServletRequest();
		try (OutputStream filteredOutput = new ExtraFilters().filter(req, out)) {
			filteredOutput.write("<html>".getBytes());
		}
		String result = out.toString();
		String expected = "<!-- pre 3 --><!-- pre 2 --><!-- pre 1 --><html>"
				+ "<!-- post 1 --><!-- post 2 --><!-- post 3 -->";
		assertEquals(expected, result);
	}

	@Test
	public void testRepeatedExtensions() throws IOException {
		PrioritizedFilter[] filters = new PrioritizedFilter[] {
				new PrioritizedFilter(new CommentFilterTwo(), 2),
				new PrioritizedFilter(new CommentFilterThree(), 3),
				new PrioritizedFilter(new CommentFilterTwo(), 1) };
		ExtraFilters.setFilters(filters);
		OutputStream out = new ByteArrayOutputStream(1000);
		MockServletRequest req = new MockServletRequest();
		try (OutputStream filteredOutput = new ExtraFilters().filter(req, out)) {
			filteredOutput.write("<html>".getBytes());
		}
		String result = out.toString();
		String expected = "<!-- pre 3 --><!-- pre 2 --><!-- pre 2 --><html>"
				+ "<!-- post 2 --><!-- post 2 --><!-- post 3 -->";
		assertEquals(expected, result);
	}

	@Test
	public void testNoFilters() throws IOException {
		PrioritizedFilter[] filters = new PrioritizedFilter[0];
		ExtraFilters.setFilters(filters);
		OutputStream out = new ByteArrayOutputStream(1000);
		MockServletRequest req = new MockServletRequest();
		try (OutputStream filteredOutput = new ExtraFilters().filter(req, out)) {
			filteredOutput.write("<html>".getBytes());
		}
		String result = out.toString();
		String expected = "<html>";
		assertEquals(expected, result);
	}

}
