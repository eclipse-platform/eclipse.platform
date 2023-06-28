/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.tests.view.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.memory.IMemoryRenderingBindingsListener;
import org.eclipse.debug.ui.memory.IMemoryRenderingManager;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.junit.Test;

/**
 * Tests memory rendering manager
 */
public class MemoryRenderingTests extends AbstractDebugTest {

	@Test
	public void testRenderingTypes() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryRenderingType[] types = manager.getRenderingTypes();
		assertTrue("Wrong number of rendering types contributed", types.length > 6); //$NON-NLS-1$
		assertTrue("Missing type 1", indexOf(manager.getRenderingType("rendering_type_1"), types) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Missing type 2", indexOf(manager.getRenderingType("rendering_type_2"), types) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Missing type 3", indexOf(manager.getRenderingType("rendering_type_3"), types) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Missing type", indexOf(manager.getRenderingType("org.eclipse.debug.ui.rendering.raw_memory"), types) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Missing type", indexOf(manager.getRenderingType("org.eclipse.debug.ui.rendering.ascii"), types) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Missing type", indexOf(manager.getRenderingType("org.eclipse.debug.ui.rendering.signedint"), types) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Missing type", indexOf(manager.getRenderingType("org.eclipse.debug.ui.rendering.unsignedint"), types) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testRenderingTypeNames() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryRenderingType type = manager.getRenderingType("rendering_type_1"); //$NON-NLS-1$
		assertEquals("Wrong name", "Rendering One", type.getLabel()); //$NON-NLS-1$ //$NON-NLS-2$
		type = manager.getRenderingType("rendering_type_2"); //$NON-NLS-1$
		assertEquals("Wrong name", "Rendering Two", type.getLabel()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSingleBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockOne();
		IMemoryRenderingType[] types = manager.getRenderingTypes(block);
		assertEquals("Wrong number of bindings", 1, types.length); //$NON-NLS-1$
		assertEquals("Wrong binding", "rendering_type_1", types[0].getId()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockTwo();
		IMemoryRenderingType[] types = manager.getRenderingTypes(block);
		assertEquals("Wrong number of bindings", 2, types.length); //$NON-NLS-1$
		assertTrue("Missing binding", indexOf(manager.getRenderingType("rendering_type_1"), types) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Missing binding", indexOf(manager.getRenderingType("rendering_type_2"), types) >= 0); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDefaultBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockOne();
		IMemoryRenderingType[] types = manager.getDefaultRenderingTypes(block);
		assertEquals("Wrong number of bindings", 1, types.length); //$NON-NLS-1$
		assertEquals("Wrong binding", "rendering_type_1", types[0].getId()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testNoDefaultBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockTwo();
		IMemoryRenderingType[] types = manager.getDefaultRenderingTypes(block);
		assertEquals("Wrong number of bindings", 0, types.length); //$NON-NLS-1$
	}

	@Test
	public void testPrimaryBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockOne();
		IMemoryRenderingType type = manager.getPrimaryRenderingType(block);
		assertEquals("Wrong binding", "rendering_type_1", type.getId()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testNoPrimaryBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockTwo();
		IMemoryRenderingType type = manager.getPrimaryRenderingType(block);
		assertNull("Wrong binding", type); //$NON-NLS-1$
	}

	@Test
	public void testDefaultWithoutPrimaryBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockThree();
		IMemoryRenderingType[] types = manager.getDefaultRenderingTypes(block);
		assertEquals("Wrong number of bindings", 1, types.length); //$NON-NLS-1$
		assertEquals("Wrong binding", "rendering_type_3", types[0].getId()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDynamicBinding() {
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
		IMemoryBlock block = new MemoryBlockDynamic();
		IMemoryRenderingType[] types = manager.getRenderingTypes(block);
		assertEquals("Wrong number of bindings", 1, types.length); //$NON-NLS-1$
		assertEquals("Wrong binding", "rendering_type_1", types[0].getId()); //$NON-NLS-1$ //$NON-NLS-2$
		types = manager.getDefaultRenderingTypes(block);
		assertEquals("Wrong number of bindings", 1, types.length); //$NON-NLS-1$
		assertEquals("Wrong binding", "rendering_type_1", types[0].getId()); //$NON-NLS-1$ //$NON-NLS-2$
		IMemoryRenderingType type = manager.getPrimaryRenderingType(block);
		assertEquals("Wrong bindings", manager.getRenderingType("rendering_type_1"), type); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testBindingChangeNotification() {
		final boolean[] changed = new boolean[1];
		IMemoryRenderingBindingsListener listener = () -> changed[0] = true;
		IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();

		// bug 374447 - Need to make sure that DynamicRenderingBindings singleton
		// is initialized
		IMemoryBlock block = new MemoryBlockDynamic();
		IMemoryRenderingType[] types = manager.getRenderingTypes(block);

		try {
			manager.addListener(listener);
			assertFalse("Renderings should not have changed yet", changed[0]); //$NON-NLS-1$
			DynamicRenderingBindings.setBinding("rendering_type_2"); //$NON-NLS-1$
			assertTrue("Renderings should have changed", changed[0]); //$NON-NLS-1$
			types = manager.getRenderingTypes(block);
			assertEquals("Wrong number of bindings", 1, types.length); //$NON-NLS-1$
			assertEquals("Wrong binding", "rendering_type_2", types[0].getId()); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			// restore original bindings
			DynamicRenderingBindings.setBinding("rendering_type_1"); //$NON-NLS-1$
			manager.removeListener(listener);
		}
	}

	protected int indexOf(Object thing, Object[] list) {
		for (int i = 0; i < list.length; i++) {
			Object object2 = list[i];
			if (object2.equals(thing)) {
				return i;
			}
		}
		return -1;
	}
}
