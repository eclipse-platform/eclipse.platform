/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.help.ui.internal.views.ScopeSet;
import org.eclipse.help.ui.internal.views.ScopeSetManager;
import org.junit.Test;

public class ScopeSetManagerTest {
	@Test
	public void testAddScope() {
		String scopeName = "newScope1";
		ScopeSetManager manager = new ScopeSetManager();
		ScopeSet newScope = new ScopeSet(scopeName);
		manager.add(newScope);
		manager.save();
		manager = new ScopeSetManager();
		ScopeSet set = manager.findSet(scopeName);
		assertNotNull(set);
		assertEquals(scopeName, set.getName());
	}

	@Test
	public void testAddAndRenameScope() {
		String oldScopeName = "newScope2";
		String newScopeName = "newScope3";
		ScopeSetManager manager = new ScopeSetManager();
		ScopeSet newScope = new ScopeSet(oldScopeName);
		manager.add(newScope);
		newScope.setName(newScopeName);
		manager.save();
		manager = new ScopeSetManager();
		ScopeSet set = manager.findSet(newScopeName);
		assertNotNull(set);
		assertEquals(newScopeName, set.getName());
		set = manager.findSet(oldScopeName);
		assertNull(set);
	}

	@Test
	public void testAddAndDeleteScope() {
		String scopeName = "newScope4";
		ScopeSetManager manager = new ScopeSetManager();
		ScopeSet newScope = new ScopeSet(scopeName);
		manager.add(newScope);
		manager.save();

		manager = new ScopeSetManager();
		ScopeSet set = manager.findSet(scopeName);
		assertNotNull(set);
		assertEquals(scopeName, set.getName());
		manager.remove(set);

		manager = new ScopeSetManager();
		set = manager.findSet(scopeName);
		assertNull(set);
	}

}
