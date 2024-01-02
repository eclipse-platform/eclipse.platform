/*******************************************************************************
 * Copyright (c) 2023 ArSysOp and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Nikifor Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.UserStringMappings;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public final class UserMappingTest {

	private static final String KEY = "key";

	@Test
	public void testParsedCorrectly() {
		UserStringMappings mappings = mappings();
		assertEquals((int) UserStringMappings.TEXT, mappings.getType("*.ext"));
		assertEquals((int) UserStringMappings.BINARY, mappings.getType("*.bn"));
		assertEquals((int) UserStringMappings.UNKNOWN, mappings.getType("*.unkwn"));
		assertEquals((int) UserStringMappings.UNKNOWN, mappings.getType("someunknowntype"));
		assertEquals((int) UserStringMappings.UNKNOWN, mappings.getType(null));
	}

	@Test
	public void testPicksExternalChanges() throws BackingStoreException {
		UserStringMappings mappings = mappings();
		assertEquals((int) UserStringMappings.UNKNOWN, mappings.getType("some"));
		modify("some\n2\n");
		assertEquals((int) UserStringMappings.BINARY, mappings.getType("some"));
	}

	@Test
	public void testAcceptsCorruptedData() throws BackingStoreException {
		UserStringMappings mappings = mappings();
		assertEquals((int) UserStringMappings.TEXT, mappings.getType("*.ext"));
		modify("corrupted");
		assertTrue(mappings.referenceMap().isEmpty());
	}

	private void modify(String value) throws BackingStoreException {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(TeamPlugin.ID);
		node.put(KEY, value);
		node.flush();
	}

	private UserStringMappings mappings() {
		UserStringMappings mappings = new UserStringMappings(KEY);
		mappings.setStringMappings( //
				new String[] { "*.ext", "*.bn", "*.unkwn" }, //
				new int[] { UserStringMappings.TEXT, UserStringMappings.BINARY, UserStringMappings.UNKNOWN });
		return mappings;
	}

}
