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

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.XMLMemento;
import org.junit.Test;

/**
 * Test the serialization of presentation context properties.
 *
 * @since 3.4
 */
public class PresentationContextTests extends AbstractDebugTest {

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
		assertEquals("Wrong value restored", "string", context.getProperty("string")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("Wrong value restored", Integer.valueOf(1), context.getProperty("integer")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Wrong value restored", Boolean.TRUE, context.getProperty("boolean")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Wrong value restored", ResourcesPlugin.getWorkspace().getRoot(), context.getProperty("persistable")); //$NON-NLS-1$ //$NON-NLS-2$
		context.dispose();
	}

}
