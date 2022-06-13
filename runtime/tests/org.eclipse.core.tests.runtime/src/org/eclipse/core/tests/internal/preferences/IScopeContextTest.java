/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.core.tests.internal.preferences;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.core.tests.runtime.RuntimeTest;
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.0
 */
public class IScopeContextTest extends RuntimeTest {

	public IScopeContextTest() {
		super("");
	}

	public IScopeContextTest(String name) {
		super(name);
	}

	public void testGetNode() {
		IScopeContext context = InstanceScope.INSTANCE;

		// null
		try {
			context.getNode(null);
			fail("1.0");
		} catch (IllegalArgumentException e) {
			// expected
		}

		// valid single segment
		String qualifier = Long.toString(System.currentTimeMillis());
		Preferences node = context.getNode(qualifier);
		assertNotNull("2.0", node);
		String expected = "/instance/" + qualifier;
		String actual = node.absolutePath();
		assertEquals("2.1", expected, actual);

		// path
		qualifier = new Path(Long.toString(System.currentTimeMillis())).append("a").toString();
		node = context.getNode(qualifier);
		assertNotNull("3.0", node);
		expected = "/instance/" + qualifier;
		actual = node.absolutePath();
		assertEquals("3.1", expected, actual);
	}

	public void testBadContext() {
		IScopeContext context = new BadTestScope();
		IPreferencesService service = Platform.getPreferencesService();
		try {
			context.getNode("qualifier");
			fail("0.5"); // should throw an exception
		} catch (RuntimeException e) {
			// expected
		}
		assertNull("1.0", service.getString("qualifier", "foo", null, new IScopeContext[] {context}));
	}
}
