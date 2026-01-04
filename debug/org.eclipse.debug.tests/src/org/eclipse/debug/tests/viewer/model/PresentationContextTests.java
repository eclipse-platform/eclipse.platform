/*******************************************************************************
 * Copyright (c) 2008, 2015 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM - moved to debug platform tests from JDT
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.tests.DebugTestExtension;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.XMLMemento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test the serialization of presentation context properties.
 *
 * @since 3.4
 */
@ExtendWith(DebugTestExtension.class)
public class PresentationContextTests {

	/**
	 * Tests saving and restoring presentation context properties.
	 */
	@Test
	public void testSaveRestore () {
		PresentationContext context = new PresentationContext("test"); //$NON-NLS-1$
		context.setProperty("string", "string"); //$NON-NLS-1$ //$NON-NLS-2$
		context.setProperty("integer", Integer.valueOf(1)); //$NON-NLS-1$
		context.setProperty("boolean", Boolean.TRUE); //$NON-NLS-1$
		context.setProperty("persistable", ResourcesPlugin.getWorkspace().getRoot().getAdapter(IPersistableElement.class)); //$NON-NLS-1$

		final XMLMemento memento = XMLMemento.createWriteRoot("TEST"); //$NON-NLS-1$
		context.saveProperites(memento);

		context = new PresentationContext("test"); //$NON-NLS-1$
		context.initProperties(memento);
		assertEquals("string", context.getProperty("string"), "Wrong value restored"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(Integer.valueOf(1), context.getProperty("integer"), "Wrong value restored"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Boolean.TRUE, context.getProperty("boolean"), "Wrong value restored"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(ResourcesPlugin.getWorkspace().getRoot(), context.getProperty("persistable"), "Wrong value restored"); //$NON-NLS-1$ //$NON-NLS-2$
		context.dispose();
	}

}
