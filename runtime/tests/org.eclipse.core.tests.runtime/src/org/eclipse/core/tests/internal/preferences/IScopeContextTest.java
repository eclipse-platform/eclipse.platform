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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.jupiter.api.Test;
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.0
 */
public class IScopeContextTest {

	@Test
	public void testGetNode() {
		IScopeContext context = InstanceScope.INSTANCE;

		// null
		assertThrows(IllegalArgumentException.class, () -> context.getNode(null));

		// valid single segment
		String qualifier = Long.toString(System.currentTimeMillis());
		Preferences node = context.getNode(qualifier);
		assertNotNull(node);
		String expected = "/instance/" + qualifier;
		String actual = node.absolutePath();
		assertEquals(expected, actual);

		// path
		qualifier = IPath.fromOSString(Long.toString(System.currentTimeMillis())).append("a").toString();
		node = context.getNode(qualifier);
		assertNotNull(node);
		expected = "/instance/" + qualifier;
		actual = node.absolutePath();
		assertEquals(expected, actual);
	}

	@Test
	public void testBadContext() {
		IScopeContext context = new BadTestScope();
		IPreferencesService service = Platform.getPreferencesService();
		assertThrows(RuntimeException.class, () -> context.getNode("qualifier"));
		assertNull(service.getString("qualifier", "foo", null, new IScopeContext[] { context }));
	}

}
